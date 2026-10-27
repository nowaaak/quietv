import base64
import hashlib
from collections.abc import Callable
from pathlib import Path
from typing import Protocol

from quietv.paths import config_dir

DEFAULT_PORT = 5555
CHUNK = 48 * 1024


class TvError(Exception):
    pass


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
        except (OSError, AdbConnectionError, DeviceAuthError) as e:
            raise TvError(
                f"cannot connect to {self.host}:{self.port} ({e}). Is ADB debugging on, is the TV awake "
                "and did you accept the 'Allow USB debugging?' prompt?"
            ) from e
        self._device = device

    def close(self) -> None:
        if self._device is not None:
            self._device.close()
            self._device = None

    def shell(self, command: str, timeout: float = 120.0) -> str:
        if self._device is None:
            raise TvError("not connected")
        return self._device.shell(command, read_timeout_s=timeout)

    def pull(self, remote: str, local: Path) -> None:
        if self._device is None:
            raise TvError("not connected")
        self._device.pull(remote, str(local))


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
