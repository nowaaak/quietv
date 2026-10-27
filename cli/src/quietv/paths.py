import os
import sys
from pathlib import Path


def config_dir() -> Path:
    override = os.environ.get("QUIETV_HOME")
    if override:
        return Path(override)
    if sys.platform == "win32":
        base = Path(os.environ.get("APPDATA") or Path.home() / "AppData" / "Roaming")
    elif sys.platform == "darwin":
        base = Path.home() / "Library" / "Application Support"
    else:
        base = Path(os.environ.get("XDG_CONFIG_HOME") or Path.home() / ".config")
    return base / "quietv"


def backups_dir() -> Path:
    return config_dir() / "backups"


def state_dir() -> Path:
    return config_dir() / "state"
