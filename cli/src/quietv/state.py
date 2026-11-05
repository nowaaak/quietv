import hashlib
import json
import re
from datetime import datetime
from pathlib import Path

from quietv.packages import is_package
from quietv.paths import state_dir
from quietv.tv import Shell, getprop

SAFE_NAME = re.compile(r"[^A-Za-z0-9_.-]+")


def tv_key(tv: Shell) -> str:
    model = getprop(tv, "ro.product.model") or "tv"
    serial = getprop(tv, "ro.serialno") or getprop(tv, "ro.boot.serialno") or getprop(tv, "ro.build.fingerprint")
    digest = hashlib.sha256(f"{model}:{serial}".encode()).hexdigest()[:16]
    return f"{SAFE_NAME.sub('-', model).strip('-')}-{digest}"


def file_for(key: str, root: Path | None = None) -> Path:
    return (root or state_dir()) / f"{key}.json"


def path(tv: Shell, root: Path | None = None) -> Path:
    return file_for(tv_key(tv), root)


def _read(file: Path) -> set[str]:
    if not file.is_file():
        return set()
    data = json.loads(file.read_text(encoding="utf-8"))
    return {p for p in data.get("disabled", []) if isinstance(p, str) and is_package(p)}


def disabled(tv: Shell, root: Path | None = None) -> set[str]:
    return _read(path(tv, root))


def record(tv: Shell, added=(), removed=(), root: Path | None = None) -> None:
    save(tv_key(tv), getprop(tv, "ro.product.model"), added, removed, root)


def save(key: str, model: str, added=(), removed=(), root: Path | None = None) -> None:
    file = file_for(key, root)
    current = (_read(file) | set(added)) - set(removed)
    file.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "model": model,
        "disabled": sorted(current),
        "updated": datetime.now().isoformat(timespec="seconds"),
    }
    file.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
