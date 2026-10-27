import re
from collections.abc import Callable
from pathlib import Path

from quietv import PACKAGE
from quietv.accessibility import add_service, remove_service
from quietv.backup import HOME_QUERY
from quietv.packages import parse_home_activity, require_package
from quietv.tv import Shell, TvError, push_bytes

REMOTE_APK = "/data/local/tmp/quietv.apk"
MAIN_ACTIVITY = f"{PACKAGE}/.MainActivity"
HOME_ENTRY = f"{PACKAGE}/.HomeEntry"
REDIRECT_SERVICE = f"{PACKAGE}/{PACKAGE}.firetv.HomeRedirectService"
SERVICES_RE = re.compile(r"^[A-Za-z0-9_.$/:]+$")


def current_home_package(tv: Shell) -> str | None:
    home = parse_home_activity(tv.shell(HOME_QUERY))
    if not home:
        return None
    package = home.split("/", 1)[0]
    return None if package in ("android", PACKAGE) else package


def install_apk(tv: Shell, apk: Path, progress: Callable[[int, int], None] | None = None) -> None:
    push_bytes(tv, apk.read_bytes(), REMOTE_APK, progress)
    out = tv.shell(f"pm install -r -g {REMOTE_APK}", timeout=300.0)
    tv.shell(f"rm -f {REMOTE_APK}")
    if "Success" not in out:
        raise TvError(f"pm install failed: {out.strip()}")
    tv.shell(f"appops set {PACKAGE} SYSTEM_ALERT_WINDOW allow")


def make_default_home(tv: Shell) -> None:
    tv.shell(f"cmd package set-home-activity --user 0 {HOME_ENTRY}")


def disable_package(tv: Shell, package: str) -> str:
    return tv.shell(f"pm disable-user --user 0 {require_package(package)}").strip()


def start_launcher(tv: Shell) -> None:
    tv.shell(f"am start -n {MAIN_ACTIVITY}")


def accessibility_services(tv: Shell) -> str:
    return tv.shell("settings get secure enabled_accessibility_services").strip()


def _write_services(tv: Shell, services: str | None) -> None:
    tv.shell("settings delete secure enabled_accessibility_services")
    if services is None:
        tv.shell("settings put secure accessibility_enabled 0")
        return
    if not SERVICES_RE.match(services):
        raise TvError(f"refusing to write unexpected accessibility value: {services!r}")
    tv.shell(f"settings put secure enabled_accessibility_services {services}")
    tv.shell("settings put secure accessibility_enabled 1")


def redirect_on(tv: Shell) -> str:
    tv.shell(f"pm grant {PACKAGE} android.permission.WRITE_SECURE_SETTINGS")
    services = add_service(accessibility_services(tv), REDIRECT_SERVICE)
    _write_services(tv, services)
    return services


def redirect_off(tv: Shell) -> str | None:
    services = remove_service(accessibility_services(tv), REDIRECT_SERVICE)
    _write_services(tv, services)
    return services


def redirect_enabled(tv: Shell) -> bool:
    return REDIRECT_SERVICE in accessibility_services(tv).split(":")
