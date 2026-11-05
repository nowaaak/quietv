import base64
import errno
import hashlib
import os
from collections.abc import Callable
from pathlib import Path
from typing import Protocol

from quietv.paths import config_dir

DEFAULT_PORT = 5555
CHUNK = 48 * 1024


class TvError(Exception):
    pass


def _adb_errors() -> tuple[type[Exception], ...]:
    from adb_shell import exceptions as e

    return (
        OSError,
        e.AdbCommandFailureException,
        e.AdbConnectionError,
        e.AdbTimeoutError,
        e.DevicePathInvalidError,
        e.InvalidChecksumError,
        e.InvalidCommandError,
        e.InvalidResponseError,
        e.TcpTimeoutException,
    )


def _brief(command: str) -> str:
    return command if len(command) <= 60 else command[:57] + "..."


def connect_hint(error: BaseException) -> str:
    from adb_shell.exceptions import DeviceAuthError, TcpTimeoutException

    if isinstance(error, DeviceAuthError):
        return "The TV did not accept quietv's key. Accept the 'Allow USB debugging?' prompt on the TV."
    if isinstance(error, ConnectionRefusedError):
        return (
            "The TV refused the connection: ADB debugging is off or the TV is asleep. Wake the TV, switch "
            "debugging off and on again, or restart the TV."
        )
    if getattr(error, "winerror", None) == 10013:
        return "Windows blocked the connection. Check the firewall, VPN or antivirus on this PC."
    if isinstance(error, OSError) and error.errno in (errno.EHOSTUNREACH, errno.ENETUNREACH):
        return (
            "No route to the TV. On macOS, allow your terminal app under System Settings, Privacy & Security, "
            "Local Network, then quit and reopen it. Otherwise check that the PC and the TV are on the same network."
        )
    if isinstance(error, (TimeoutError, TcpTimeoutException)):
        return (
            "No answer from the TV. Check the IP address and that the PC and the TV are on the same network. "
            "Guest networks and access point isolation block it."
        )
    return "Is ADB debugging on, is the TV awake and did you accept the 'Allow USB debugging?' prompt?"


class Shell(Protocol):
    def shell(self, command: str, timeout: float = 120.0) -> str: ...


class Tv:
    def __init__(self, host: str, port: int = DEFAULT_PORT, timeout: float = 120.0):
        self.host = host
        self.port = port
        self.timeout = timeout
        self._device = None

    def __enter__(self) -> "Tv":
        self.connect()
        return self

    def __exit__(self, *exc) -> None:
        self.close()

    def connect(self) -> None:
        from adb_shell.adb_device import AdbDeviceTcp
        from adb_shell.exceptions import AdbConnectionError, DeviceAuthError

        device = AdbDeviceTcp(self.host, self.port, default_transport_timeout_s=self.timeout)
        try:
            device.connect(rsa_keys=[_signer()], transport_timeout_s=self.timeout, auth_timeout_s=self.timeout)
        except (*_adb_errors(), AdbConnectionError, DeviceAuthError) as e:
            detail = str(e) or type(e).__name__
            raise TvError(f"cannot connect to {self.host}:{self.port} ({detail}). {connect_hint(e)}") from e
        self._device = device

    def close(self) -> None:
        if self._device is not None:
            self._device.close()
            self._device = None

    def shell(self, command: str, timeout: float = 120.0) -> str:
        if self._device is None:
            raise TvError("not connected")
        try:
            return self._device.shell(command, read_timeout_s=timeout)
        except _adb_errors() as e:
            raise TvError(
                f"lost the connection to the TV while running '{_brief(command)}' ({str(e) or type(e).__name__}). "
                "The TV may or may not have run it."
            ) from e

    def pull(self, remote: str, local: Path) -> None:
        if self._device is None:
            raise TvError("not connected")
        local = Path(local)
        part = local.with_name(f".{local.name}.{os.getpid()}.part")
        try:
            self._device.pull(remote, str(part))
            os.replace(part, local)
        except _adb_errors() as e:
            raise TvError(f"could not copy {remote} from the TV ({str(e) or type(e).__name__})") from e
        finally:
            part.unlink(missing_ok=True)


def _signer():
    from adb_shell.auth.keygen import keygen
    from adb_shell.auth.sign_pythonrsa import PythonRSASigner

    key = config_dir() / "adbkey"
    key.parent.mkdir(parents=True, exist_ok=True)
    if not key.exists():
        keygen(str(key))
    return PythonRSASigner(Path(str(key) + ".pub").read_text(), key.read_text())


def push_bytes(tv: Shell, data: bytes, remote: str, progress: Callable[[int, int], None] | None = None) -> None:
    want = hashlib.md5(data).hexdigest()
    tmp = remote + ".b64"
    tv.shell(f"rm -f '{remote}' '{tmp}'")
    count = max(1, (len(data) + CHUNK - 1) // CHUNK)
    for i in range(count):
        part = base64.b64encode(data[i * CHUNK : (i + 1) * CHUNK]).decode()
        out = tv.shell(f"echo '{part}' >> '{tmp}' && echo ok{i}", timeout=60.0)
        if f"ok{i}" not in out:
            raise TvError(f"upload failed at chunk {i + 1}/{count}: {out.strip()}")
        if progress:
            progress(i + 1, count)
    out = tv.shell(f"base64 -d '{tmp}' > '{remote}' && rm '{tmp}' && md5sum '{remote}'")
    got = out.split()[0] if out.strip() else ""
    if got != want:
        raise TvError(f"upload checksum mismatch for {remote}: expected {want}, got {got or 'nothing'}")


def getprop(tv: Shell, name: str) -> str:
    return tv.shell(f"getprop {name}").strip()


def is_fire_os(tv: Shell) -> bool:
    if getprop(tv, "ro.product.manufacturer").lower() == "amazon":
        return True
    return "amazon.hardware.fire_tv" in tv.shell("pm list features")
