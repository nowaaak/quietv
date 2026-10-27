import pytest

from quietv import cli, devicelist, guard, pair, privacy, report, state
from quietv.backup import HOME_QUERY

TV_INPUT = (
    "  com.mediatek.tvinput/.hdmi.HDMIInputService/HW5: info: TvInputInfo{id=com.mediatek.tvinput/.hdmi."
    "HDMIInputService/HW5, pkg=com.mediatek.tvinput, service=x}; state: 1\n"
    "  de.sky.online/com.peacock.RichTvInputService: info: TvInputInfo{id=de.sky.online/com.peacock."
    "RichTvInputService, pkg=de.sky.online, service=y}; state: 0\n"
)
CHIQ = {
    "getprop ro.product.model": "AI PONT\n",
    "getprop ro.serialno": "ABC123\n",
    "dumpsys tv_input": TV_INPUT,
    "settings get secure default_input_method": "com.google.android.inputmethod.latin/com.android.Latin\n",
    HOME_QUERY: "io.github.nowaaak.quietv/.HomeEntry\n",
}


@pytest.fixture
def connected(monkeypatch, fake_tv):
    def use(responses):
        tv = fake_tv({**CHIQ, **responses})
        monkeypatch.setattr(cli, "connect", lambda args: tv)
        return tv

    return use


def test_static_and_suffix_protection():
    assert guard.reason("com.android.tv.settings", {}) is not None
    assert "crash-loop" in guard.reason("com.tcl.partnercustomizer", {})
    assert guard.reason("com.example.ads", {}) is None


def test_dynamic_protection_finds_hardware_inputs_keyboard_and_home(fake_tv):
    found = guard.dynamic(fake_tv({**CHIQ, HOME_QUERY: "com.google.android.tvlauncher/.MainActivity\n"}))
    assert "com.mediatek.tvinput" in found
    assert "de.sky.online" not in found
    assert "com.google.android.inputmethod.latin" in found
    assert "com.google.android.tvlauncher" in found


def test_no_device_list_disables_a_protected_package(repo_root):
    for path in sorted((repo_root / "devices").glob("*.toml")):
        for package in devicelist.load(str(path)).packages:
            assert guard.reason(package.id, {}) is None, (path.name, package.id)


def test_state_is_keyed_by_model_and_serial_not_address(fake_tv, tmp_path):
    tv = fake_tv(CHIQ)
    state.record(tv, added=["com.a.b", "com.c.d"], root=tmp_path)
    state.record(tv, removed=["com.a.b"], root=tmp_path)
    assert state.disabled(tv, root=tmp_path) == {"com.c.d"}
    other = fake_tv({**CHIQ, "getprop ro.serialno": "XYZ\n"})
    assert state.disabled(other, root=tmp_path) == set()
    assert "ABC123" not in state.path(tv, tmp_path).name


def test_debloat_keeps_protected_packages_and_records_the_rest(connected, monkeypatch, capsys):
    tv = connected(
        {
            "pm list packages -e": "package:com.mitv.tvhome\npackage:com.mediatek.tvinput\n",
            "pm disable-user": "Package com.mitv.tvhome new state: disabled-user\n",
        }
    )

    class List:
        name, os, keep, source = "Test TV", "Android TV", [], "x"

        @staticmethod
        def select(tier):
            return [
                devicelist.Package("com.mitv.tvhome", "ads", "recommended"),
                devicelist.Package("com.mediatek.tvinput", "vendor", "recommended"),
            ]

    monkeypatch.setattr(cli.devicelist, "load", lambda name: List)
    assert cli.main(["debloat", "x", "--yes", "--no-backup"]) == 0
    out = capsys.readouterr().out
    assert "keep com.mediatek.tvinput: protected" in out
    assert "pm disable-user --user 0 com.mitv.tvhome" in tv.commands
    assert not any("com.mediatek.tvinput" in c and "disable" in c for c in tv.commands)
    assert state.disabled(tv) == {"com.mitv.tvhome"}


def test_check_reports_and_fixes_packages_that_came_back(connected, capsys):
    tv = connected(
        {
            "pm list packages -e": "package:com.a.ads\npackage:com.b.ok\n",
            "pm list packages -u": "package:com.a.ads\npackage:com.b.ok\npackage:com.c.off\n",
            "pm disable-user": "new state: disabled-user\n",
        }
    )
    state.record(tv, added=["com.a.ads", "com.c.off", "com.gone.app"])
    assert cli.main(["check"]) == 0
    out = capsys.readouterr().out
    assert "1 packages quietv disabled are enabled again" in out
    assert "com.a.ads" in out
    assert not any(c.startswith("pm disable-user") for c in tv.commands)
    assert state.disabled(tv) == {"com.a.ads", "com.c.off"}

    assert cli.main(["check", "--fix", "--yes", "--no-backup"]) == 0
    assert "pm disable-user --user 0 com.a.ads" in tv.commands


def test_check_without_a_record(connected, capsys):
    connected({})
    assert cli.main(["check"]) == 0
    assert "No record" in capsys.readouterr().out


def test_report_lists_device_home_and_packages(fake_tv):
    tv = fake_tv(
        {
            **CHIQ,
            "getprop ro.product.manufacturer": "CHiQ\n",
            "cmd package query-activities": (
                "  io.github.nowaaak.quietv/.HomeEntry\n  com.android.tv.settings/.FallbackHome\n"
            ),
            "dumpsys package io.github.nowaaak.quietv": "    versionName=0.3.0\n",
            "pm list packages -e": "package:com.a.b\n",
            "pm list packages -d": "package:com.c.d\n",
        }
    )
    text = report.build(tv)
    assert "| Model | `AI PONT` |" in text
    assert "`io.github.nowaaak.quietv/.HomeEntry`" in text
    assert "Launcher: 0.3.0" in text
    assert "`com.mediatek.tvinput/.hdmi.HDMIInputService/HW5`" in text
    assert "Enabled packages (1)" in text and "com.c.d" in text
    assert "ABC123" not in text


def test_pair_runs_pair_connect_and_tcpip():
    calls = []
    replies = iter(["Successfully paired to 10.0.0.5:37000", "connected to 10.0.0.5:41000", "restarting in TCP mode"])

    def run(adb, *args):
        calls.append(args)
        return next(replies)

    pair.pair("adb", "10.0.0.5", 37000, "123456", 41000, run=run)
    assert calls == [
        ("pair", "10.0.0.5:37000", "123456"),
        ("connect", "10.0.0.5:41000"),
        ("-s", "10.0.0.5:41000", "tcpip", "5555"),
    ]


def test_pair_reports_a_wrong_code():
    with pytest.raises(pair.PairError, match="pairing failed"):
        pair.pair("adb", "10.0.0.5", 37000, "000000", 41000, run=lambda *a: "Failed: Wrong password")


def test_pair_needs_six_digits(capsys):
    with pytest.raises(SystemExit):
        cli.main(["--host", "10.0.0.5", "pair", "37000", "12ab", "41000"])
    assert "six digits" in capsys.readouterr().err


def test_screensaver_on_and_off(connected, capsys):
    tv = connected({"settings get secure screensaver_components": "com.gathin.tvhome/.ClockDream\n"})
    assert cli.main(["screensaver", "off"]) == 0
    assert "Nothing changed" in capsys.readouterr().out
    assert cli.main(["screensaver", "on"]) == 0
    assert "settings put secure screensaver_components io.github.nowaaak.quietv/.dream.ClockDream" in tv.commands
    assert "settings put secure screensaver_enabled 1" in tv.commands

    tv = connected({"settings get secure screensaver_components": "io.github.nowaaak.quietv/.dream.ClockDream\n"})
    assert cli.main(["screensaver", "off"]) == 0
    assert "settings delete secure screensaver_components" in tv.commands


DUMP = """Packages:
  Package [com.google.android.katniss] (8f5788c):
    runtime permissions:
        android.permission.ACCESS_FINE_LOCATION: granted=true, flags=[ GRANTED_BY_DEFAULT]
        android.permission.RECORD_AUDIO: granted=true, flags=[ GRANTED_BY_DEFAULT]
  Package [com.android.vending] (bb73595):
        android.permission.RECORD_AUDIO: granted=true, flags=[ USER_SET|USER_SENSITIVE_WHEN_GRANTED]
  Package [com.old.disabled] (1):
        android.permission.CAMERA: granted=true
  Package [com.vendor.camera] (2):
        android.permission.CAMERA: granted=false
"""


def test_privacy_report_lists_enabled_holders_and_origins(fake_tv, repo_root):
    tv = fake_tv(
        {
            "pm list packages -e": (
                "package:com.google.android.katniss\npackage:com.android.vending\npackage:com.vendor.camera\n"
                "package:com.miui.tv.analytics\npackage:tv.anoki.acr.controller\n"
            ),
            "dumpsys package packages": DUMP,
            "settings get secure enabled_accessibility_services": "io.github.nowaaak.quietv/x.Service\n",
            "settings get secure enabled_notification_listeners": "null\n",
        }
    )
    text = privacy.build(tv, devicelist.load(str(repo_root / "devices" / "chiq-ai-pont.toml")))
    microphone = text.split("## Microphone")[1].split("##")[0]
    assert "`com.google.android.katniss` (granted by the manufacturer)" in microphone
    assert "`com.android.vending` (granted by you or an app)" in microphone
    assert "## Camera\n\nNo enabled app has it." in text
    assert "`io.github.nowaaak.quietv/x.Service`" in text
    assert "## Notification listeners\n\nNone." in text
    assert "`tv.anoki.acr.controller` (ads" in text
    assert "`com.miui.tv.analytics`" in text.split("a guess from the package name only")[1]
