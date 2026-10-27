import re

from quietv.devicelist import DeviceList
from quietv.packages import parse_package_list
from quietv.tv import Shell

WATCHED = {
    "android.permission.RECORD_AUDIO": "Microphone",
    "android.permission.CAMERA": "Camera",
    "android.permission.ACCESS_FINE_LOCATION": "Precise location",
    "android.permission.ACCESS_COARSE_LOCATION": "Approximate location",
    "android.permission.ACCESS_BACKGROUND_LOCATION": "Location in the background",
}
PACKAGE_RE = re.compile(r"^  Package \[([A-Za-z0-9_.]+)\]")
GRANT_RE = re.compile(r"^\s+(android\.permission\.[A-Z_]+): granted=true(?:, flags=\[([^\]]*)\])?")
SUSPICIOUS_RE = re.compile(
    r"(analytic|telemetr|tracking|tracker|statistic|logkit|crashreport|\.acr\b|\.acr\.|adservice)"
)


def granted(dump: str) -> dict[str, dict[str, bool]]:
    result: dict[str, dict[str, bool]] = {permission: {} for permission in WATCHED}
    package = None
    for line in dump.splitlines():
        start = PACKAGE_RE.match(line)
        if start:
            package = start.group(1)
            continue
        grant = GRANT_RE.match(line)
        if package and grant and grant.group(1) in WATCHED:
            result[grant.group(1)][package] = "GRANTED_BY_DEFAULT" in (grant.group(2) or "")
    return result


def services(value: str) -> list[str]:
    value = value.strip()
    return [] if value in ("", "null") else [s for s in value.split(":") if s]


def build(tv: Shell, device_list: DeviceList | None = None) -> str:
    enabled = parse_package_list(tv.shell("pm list packages -e"))
    grants = granted(tv.shell("dumpsys package packages", timeout=180.0))
    lines = ["# Privacy report", ""]
    for permission, title in WATCHED.items():
        holders = sorted(p for p in grants[permission] if p in enabled)
        lines += [f"## {title}", ""]
        if not holders:
            lines += ["No enabled app has it.", ""]
            continue
        for package in holders:
            origin = "granted by the manufacturer" if grants[permission][package] else "granted by you or an app"
            lines.append(f"- `{package}` ({origin})")
        lines.append("")

    for title, setting in (
        ("Accessibility services", "enabled_accessibility_services"),
        ("Notification listeners", "enabled_notification_listeners"),
    ):
        active = services(tv.shell(f"settings get secure {setting}"))
        lines += [f"## {title}", ""] + ([f"- `{s}`" for s in active] or ["None."]) + [""]

    lines += ["## Ads and telemetry still enabled", ""]
    if device_list is not None:
        known = [p for p in device_list.packages if p.category in ("ads", "telemetry") and p.id in enabled]
        lines += [f"From the device list {device_list.name}:", ""]
        lines += [f"- `{p.id}` ({p.category}{', ' + p.note if p.note else ''})" for p in known] or ["- none"]
        lines.append("")
    guesses = sorted(p for p in enabled if SUSPICIOUS_RE.search(p))
    lines += ["Names that look like analytics or tracking (a guess from the package name only):", ""]
    lines += [f"- `{p}`" for p in guesses] or ["- none"]
    lines += [
        "",
        "Disable what you do not need with `quietv debloat` or `quietv shell pm disable-user --user 0 <package>`.",
        "",
    ]
    return "\n".join(lines)
