import re

from quietv import PACKAGE, __version__
from quietv.backup import HOME_QUERY
from quietv.install import redirect_enabled
from quietv.packages import COMPONENT_RE, parse_home_activity, parse_package_list
from quietv.tv import Shell, getprop, is_fire_os

PROPS = (
    ("Manufacturer", "ro.product.manufacturer"),
    ("Model", "ro.product.model"),
    ("Device", "ro.product.device"),
    ("Android", "ro.build.version.release"),
    ("SDK", "ro.build.version.sdk"),
    ("Firmware", "ro.build.display.id"),
)
HOME_CANDIDATES = "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
INPUT_RE = re.compile(r"TvInputInfo\{id=([^,\s]+), pkg=")
VERSION_RE = re.compile(r"versionName=(\S+)")


def build(tv: Shell) -> str:
    lines = ["## Device", "", "| Property | Value |", "|---|---|"]
    for label, prop in PROPS:
        lines.append(f"| {label} | `{getprop(tv, prop) or '-'}` |")
    lines.append(f"| Fire OS | {'yes' if is_fire_os(tv) else 'no'} |")

    home = parse_home_activity(tv.shell(HOME_QUERY)) or "-"
    candidates = [c for c in (s.strip() for s in tv.shell(HOME_CANDIDATES).splitlines()) if COMPONENT_RE.match(c)]
    lines += ["", "## Home", "", f"Current home: `{home}`", "", "Home candidates:", ""]
    lines += [f"- `{c}`" for c in candidates] or ["- none found"]

    version = VERSION_RE.search(tv.shell(f"dumpsys package {PACKAGE} | grep versionName"))
    lines += [
        "",
        "## quietv",
        "",
        f"- CLI: {__version__}",
        f"- Launcher: {version.group(1) if version else 'not installed'}",
        f"- Home redirect: {'on' if version and redirect_enabled(tv) else 'off'}",
    ]

    inputs = sorted(set(INPUT_RE.findall(tv.shell("dumpsys tv_input"))))
    lines += ["", "## TV inputs", ""] + ([f"- `{i}`" for i in inputs] or ["- none"])

    for title, command in (("Enabled packages", "pm list packages -e"), ("Disabled packages", "pm list packages -d")):
        packages = sorted(parse_package_list(tv.shell(command)))
        lines += ["", f"<details><summary>{title} ({len(packages)})</summary>", "", "```"]
        lines += packages
        lines += ["```", "", "</details>"]
    return "\n".join(lines) + "\n"
