import errno
import subprocess

import pytest
from adb_shell.exceptions import DeviceAuthError, TcpTimeoutException

from quietv import cli, devicelist, pair, state
from quietv.tv import Tv, TvError, connect_hint


class BrokenDevice:
    def __init__(self, error):
        self.error = error

    def shell(self, command, read_timeout_s=None):
        raise self.error

    def pull(self, remote, local):
        with open(local, "wb") as stream:
            stream.write(b"partial")
        raise self.error


def broken_tv(error) -> Tv:
    tv = Tv("10.0.0.5")
    tv._device = BrokenDevice(error)
    return tv


def test_adb_errors_become_tv_errors():
    with pytest.raises(TvError, match="lost the connection.*TcpTimeoutException"):
        broken_tv(TcpTimeoutException("")).shell("pm list packages")
    with pytest.raises(TvError, match="ConnectionResetError"):
        broken_tv(ConnectionResetError()).shell("pm list packages")


def test_failed_pull_keeps_the_existing_file(tmp_path):
    target = tmp_path / "shot.png"
    target.write_bytes(b"old picture")
    with pytest.raises(TvError, match="could not copy"):
        broken_tv(ConnectionResetError()).pull("/sdcard/x.png", target)
    assert target.read_bytes() == b"old picture"
    assert [p.name for p in tmp_path.iterdir()] == ["shot.png"]


@pytest.mark.parametrize(
    ("error", "hint"),
    [
        (ConnectionRefusedError(), "refused"),
        (TimeoutError(), "No answer"),
        (TcpTimeoutException("x"), "No answer"),
        (OSError(errno.EHOSTUNREACH, "No route to host"), "Local Network"),
        (DeviceAuthError("x"), "did not accept"),
    ],
)
def test_connect_hint_names_the_likely_cause(error, hint):
    assert hint in connect_hint(error)


def test_interrupted_debloat_records_what_was_done(monkeypatch, fake_tv, capsys):
    class DroppingTv(fake_tv):
        def shell(self, command, timeout=120.0):
            if command == "pm disable-user --user 0 com.b.ads":
                raise TvError("lost the connection to the TV")
            return super().shell(command, timeout)

    tv = DroppingTv(
        {
            "getprop ro.product.model": "AI PONT\n",
            "getprop ro.serialno": "ABC123\n",
            "pm list packages -e": "package:com.a.ads\npackage:com.b.ads\n",
            "pm disable-user --user 0 com.a.ads": "Package com.a.ads new state: disabled-user\n",
        }
    )
    monkeypatch.setattr(cli, "connect", lambda args: tv)

    class List:
        name, os, keep, source = "Test TV", "Android TV", [], "x"

        @staticmethod
        def select(tier):
            return [
                devicelist.Package("com.a.ads", "ads", "recommended"),
                devicelist.Package("com.b.ads", "ads", "recommended"),
            ]

    monkeypatch.setattr(cli.devicelist, "load", lambda name: List)
    assert cli.main(["debloat", "x", "--yes", "--no-backup"]) == 1
    assert "1 of 2 packages were disabled before that" in capsys.readouterr().err
    assert state.disabled(tv) == {"com.a.ads"}


def test_screenshot_refuses_a_folder_before_connecting(monkeypatch, tmp_path, capsys):
    monkeypatch.setattr(cli, "connect", lambda args: pytest.fail("should not connect"))
    assert cli.main(["--host", "10.0.0.5", "screenshot", str(tmp_path)]) == 1
    assert cli.main(["--host", "10.0.0.5", "screenshot", str(tmp_path / "missing" / "x.png")]) == 1
    assert "does not exist" in capsys.readouterr().err


def test_screenshot_removes_the_file_on_the_tv(monkeypatch, fake_tv, tmp_path):
    tv = fake_tv()
    tv.files["/sdcard/quietv-screenshot.png"] = b"png"
    monkeypatch.setattr(cli, "connect", lambda args: tv)
    out = tmp_path / "shot.png"
    assert cli.main(["--host", "10.0.0.5", "screenshot", str(out)]) == 0
    assert out.read_bytes() == b"png"
    assert tv.commands[-1] == "rm -f /sdcard/quietv-screenshot.png"


def test_pair_timeout_is_a_pair_error(monkeypatch):
    def slow(*args, **kwargs):
        raise subprocess.TimeoutExpired(args[0], 60)

    monkeypatch.setattr(pair.subprocess, "run", slow)
    with pytest.raises(pair.PairError, match="no answer within a minute"):
        pair.run_adb("adb", "pair", "10.0.0.5:37000", "123456")


def test_pair_explains_no_route_to_host():
    with pytest.raises(pair.PairError, match="Local Network"):
        pair.pair("adb", "10.0.0.5", 37000, "123456", 41000, run=lambda *a: "failed: No route to host")
