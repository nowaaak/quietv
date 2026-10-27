import json
import re
from datetime import datetime
from pathlib import Path

from quietv.accessibility import normalize
from quietv.packages import is_package, parse_home_activity, parse_package_list
from quietv.tv import Shell, getprop

HOME_QUERY = "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
FILES = {
    "packages_enabled.txt": "pm list packages -e",
    "packages_disabled.txt": "pm list packages -d",
    "packages_all.txt": "pm list packages -u",
    "home_activity.txt": HOME_QUERY,
    "getprop.txt": "getprop",
    "settings_global.txt": "settings list global",
    "settings_secure.txt": "settings list secure",
    "settings_system.txt": "settings list system",
}
SAFE_NAME = re.compile(r"[^A-Za-z0-9_.-]+")


def create(tv: Shell, root: Path) -> Path:
    model = getprop(tv, "ro.product.model") or "tv"
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    folder = root / f"{SAFE_NAME.sub('-', model).strip('-')}-{stamp}"
    folder.mkdir(parents=True, exist_ok=False)
    for name, command in FILES.items():
        (folder / name).write_text(tv.shell(command), encoding="utf-8")
    meta = {
        "model": model,
        "manufacturer": getprop(tv, "ro.product.manufacturer"),
        "android": getprop(tv, "ro.build.version.release"),
        "sdk": getprop(tv, "ro.build.version.sdk"),
        "build": getprop(tv, "ro.build.display.id"),
        "created": datetime.now().isoformat(timespec="seconds"),
    }
    (folder / "meta.json").write_text(json.dumps(meta, indent=2), encoding="utf-8")
    return folder


def latest(root: Path) -> Path | None:
    if not root.is_dir():
        return None
    folders = [p for p in root.iterdir() if (p / "packages_enabled.txt").is_file()]
    return max(folders, key=lambda p: p.stat().st_mtime, default=None)


def read_setting(settings_text: str, key: str) -> str | None:
    prefix = key + "="
    for line in settings_text.splitlines():
        if line.startswith(prefix):
            return line[len(prefix) :].strip()
    return None


def enable_commands(packages: list[str] | set[str]) -> list[str]:
    commands = []
    for name in sorted(packages):
        if is_package(name):
            commands.append(f"cmd package install-existing {name}")
            commands.append(f"pm enable {name}")
    return commands


def restore_plan(folder: Path, enabled_now: set[str]) -> list[str]:
    enabled_then = parse_package_list((folder / "packages_enabled.txt").read_text(encoding="utf-8"))
    commands = enable_commands(enabled_then - enabled_now)

    home = parse_home_activity((folder / "home_activity.txt").read_text(encoding="utf-8"))
    if home:
        commands.append(f"cmd package set-home-activity --user 0 {home}")

    secure = (folder / "settings_secure.txt").read_text(encoding="utf-8")
    ime = read_setting(secure, "default_input_method")
    if ime and re.match(r"^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$", ime):
        commands.append(f"ime set {ime}")

    services = normalize(read_setting(secure, "enabled_accessibility_services"))
    if services is None:
        commands.append("settings delete secure enabled_accessibility_services")
    elif re.match(r"^[A-Za-z0-9_.$/:]+$", services):
        commands.append(f"settings put secure enabled_accessibility_services {services}")
    return commands
