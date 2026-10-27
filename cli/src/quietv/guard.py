import re

from quietv import PACKAGE
from quietv.backup import HOME_QUERY
from quietv.packages import is_package, parse_home_activity
from quietv.tv import Shell

PROTECTED = {
    "android": "the Android system",
    "com.android.systemui": "the system interface",
    "com.android.shell": "the ADB shell quietv works through",
    "com.android.providers.settings": "stores every system setting",
    "com.android.settings": "system settings",
    "com.android.tv.settings": "TV settings, the way back on the TV itself",
    "com.android.packageinstaller": "installs and updates apps",
    "com.google.android.packageinstaller": "installs and updates apps",
    "com.android.inputdevices": "remote and keyboard layouts",
    "com.google.android.gms": "Google Play services, most apps need it",
    "com.google.android.gsf": "Google services framework",
    "com.mediatek.tv.service": "the MediaTek TV service: inputs, picture and sound",
    "com.google.android.tungsten.setupwraith": "Google TV setup, holds Wi-Fi and Cast credentials on some devices",
    PACKAGE: "quietv itself",
}
PROTECTED_SUFFIXES = {
    ".partnercustomizer": "launcher partner settings, without them the TV service can crash-loop",
}
HARDWARE_INPUT_RE = re.compile(r"TvInputInfo\{id=([A-Za-z][A-Za-z0-9_.]*)/[^,\s]*/(?:HW|HDMI)\d+")


def reason(package: str, dynamic: dict[str, str]) -> str | None:
    if package in PROTECTED:
        return PROTECTED[package]
    for suffix, why in PROTECTED_SUFFIXES.items():
        if package.endswith(suffix):
            return why
    return dynamic.get(package)


def dynamic(tv: Shell) -> dict[str, str]:
    found: dict[str, str] = {}
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
