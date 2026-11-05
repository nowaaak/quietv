import re
from collections.abc import Iterable

from quietv import PACKAGE
from quietv.backup import HOME_QUERY
from quietv.packages import is_package, parse_home_activity, require_package
from quietv.tv import Shell

PROTECTED = {
    "android": "the Android system",
    "com.android.systemui": "the system interface",
    "com.android.shell": "the ADB shell quietv works through",
    "com.android.providers.settings": "stores every system setting",
    "com.android.phone": "the telephony service, some TVs restart in a loop without it",
    "com.android.settings": "system settings",
    "com.android.tv.settings": "TV settings, the way back on the TV itself",
    "com.android.packageinstaller": "installs and updates apps",
    "com.google.android.packageinstaller": "installs and updates apps",
    "com.android.permissioncontroller": "grants app permissions, Android does not start without it",
    "com.google.android.permissioncontroller": "grants app permissions, Android does not start without it",
    "com.android.inputdevices": "remote and keyboard layouts",
    "com.android.location.fused": "location provider, a TCL TV boot-looped into recovery without it",
    "com.android.sdksandbox": "Privacy Sandbox runtime, Google TV 14 stays on the boot logo without it",
    "android.ext.services": "core system services",
    "android.ext.shared": "core shared library",
    "com.android.ext.services": "core system services",
    "com.google.android.ext.services": "core system services",
    "com.android.ext.shared": "core shared library",
    "com.google.android.ext.shared": "core shared library",
    "com.android.modulemetadata": "system module metadata",
    "com.google.android.modulemetadata": "system module metadata",
    "com.google.android.gms": "Google Play services, most apps need it",
    "com.google.android.gsf": "Google services framework",
    "com.mediatek.tv.service": "the MediaTek TV service: inputs, picture and sound",
    "com.mediatek.tis": "MediaTek TV input service",
    "com.mediatek.tv.agent": "MediaTek TV agent, part of the TV service",
    "com.cltv.mal": "TV middleware, a Haier TV restarted in a loop without it",
    "com.cvte.tv.launcher.space": "the CVTE stock home, these TVs do not boot without it",
    "com.google.android.tungsten.setupwraith": "Google TV setup, holds Wi-Fi and Cast credentials on some devices",
    PACKAGE: "quietv itself",
}
PROTECTED_SUFFIXES = {
    ".partnercustomizer": "launcher partner settings, without them the TV service can crash-loop",
}
PROTECTED_PREFIXES = {
    "com.android.providers.": "a system content provider",
    "com.google.android.overlay.modules": "a system module overlay",
}
UNPROTECTED_PROVIDERS = {
    "com.android.providers.calendar",
    "com.android.providers.contacts",
    "com.android.providers.userdictionary",
    "com.android.providers.blockednumber",
}
HARDWARE_INPUT_RE = re.compile(r"TvInputInfo\{id=([A-Za-z][A-Za-z0-9_.]*)/[^,\s]*/(?:HW|HDMI)\d+")
COMPONENT_LINE_RE = re.compile(r"^\s*([A-Za-z][A-Za-z0-9_.]*)/[A-Za-z0-9_.$]+\s*$")
INPUT_QUERY = (
    "cmd package query-activities --brief -a android.intent.action.VIEW "
    "-d content://android.media.tv/passthrough/x -t vnd.android.cursor.item/channel"
)
SETTINGS_QUERY = "cmd package query-activities --brief -a android.settings.SETTINGS"
HARDWARE_PERMISSION = "android.permission.TV_INPUT_HARDWARE"
REQUESTED_BLOCK = "/requested permissions:/,/:[ ]*$/p"


def reason(package: str, dynamic: dict[str, str]) -> str | None:
    if package in PROTECTED:
        return PROTECTED[package]
    for suffix, why in PROTECTED_SUFFIXES.items():
        if package.endswith(suffix):
            return why
    if package not in UNPROTECTED_PROVIDERS:
        for prefix, why in PROTECTED_PREFIXES.items():
            if package.startswith(prefix):
                return why
    return dynamic.get(package)


def answering_packages(output: str) -> set[str]:
    found = set()
    for line in output.splitlines():
        match = COMPONENT_LINE_RE.match(line)
        if match and is_package(match.group(1)):
            found.add(match.group(1))
    return found


def hardware_users(tv: Shell, candidates: Iterable[str]) -> set[str]:
    names = sorted({require_package(p) for p in candidates})
    if not names:
        return set()
    script = (
        f"for p in {' '.join(names)}; do dumpsys package $p | sed -n '{REQUESTED_BLOCK}' "
        f"| grep -q {HARDWARE_PERMISSION} && echo $p; done"
    )
    return {line.strip() for line in tv.shell(script, timeout=600.0).splitlines() if line.strip() in names}


def dynamic(tv: Shell, candidates: Iterable[str] = ()) -> dict[str, str]:
    found: dict[str, str] = {}
    for package in hardware_users(tv, candidates):
        found[package] = "uses the TV's input hardware (HDMI, tuner)"
    for package in answering_packages(tv.shell(SETTINGS_QUERY)):
        found[package] = "opens the TV settings, the way back to ADB"
    for package in answering_packages(tv.shell(INPUT_QUERY)):
        found[package] = "opens the HDMI and tuner inputs"
    for package in HARDWARE_INPUT_RE.findall(tv.shell("dumpsys tv_input")):
        found[package] = "provides the TV's HDMI or tuner inputs"
    ime = tv.shell("settings get secure default_input_method").strip()
    if "/" in ime and is_package(ime.split("/", 1)[0]):
        found[ime.split("/", 1)[0]] = "the keyboard in use"
    home = parse_home_activity(tv.shell(HOME_QUERY))
    if home:
        package = home.split("/", 1)[0]
        if package not in ("android", PACKAGE):
            found[package] = "the current home screen, quietv install takes care of it"
    return found
