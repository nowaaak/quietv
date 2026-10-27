import os
import shutil
import subprocess
import sys
from collections.abc import Callable
from pathlib import Path

ADB_NAME = "adb.exe" if sys.platform == "win32" else "adb"
LEGACY_PORT = 5555


class PairError(Exception):
    pass


def find_adb() -> str | None:
    found = shutil.which("adb")
    if found:
        return found
    roots = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    if sys.platform == "win32":
        roots.append(str(Path(os.environ.get("LOCALAPPDATA", "")) / "Android" / "Sdk"))
    elif sys.platform == "darwin":
        roots.append(str(Path.home() / "Library" / "Android" / "sdk"))
    else:
        roots.append(str(Path.home() / "Android" / "Sdk"))
    for root in filter(None, roots):
        candidate = Path(root) / "platform-tools" / ADB_NAME
        if candidate.is_file():
            return str(candidate)
    return None


def run_adb(adb: str, *args: str) -> str:
    result = subprocess.run([adb, *args], capture_output=True, text=True, timeout=60)
    return (result.stdout + result.stderr).strip()


def pair(
    adb: str,
    host: str,
    pair_port: int,
    code: str,
    connect_port: int,
    run: Callable[..., str] = run_adb,
) -> None:
    out = run(adb, "pair", f"{host}:{pair_port}", code)
    if "Successfully paired" not in out:
        raise PairError(f"pairing failed: {out or 'no answer'}")
    target = f"{host}:{connect_port}"
    out = run(adb, "connect", target)
    if "connected to" not in out:
        raise PairError(f"connect failed: {out or 'no answer'}")
    out = run(adb, "-s", target, "tcpip", str(LEGACY_PORT))
    if "restarting in TCP mode" not in out:
        raise PairError(f"could not switch the TV to port {LEGACY_PORT}: {out or 'no answer'}")
