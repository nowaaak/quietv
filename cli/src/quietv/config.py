import base64
import json
import re
from typing import Any

from quietv import PACKAGE
from quietv.packages import is_package
from quietv.tv import Shell

RECEIVER = f"{PACKAGE}/.config.ConfigReceiver"
ACTION_SET = f"{PACKAGE}.action.SET_CONFIG"
ACTION_GET = f"{PACKAGE}.action.GET_CONFIG"
RESULT_OK = -1
RESULT_RE = re.compile(r'Broadcast completed: result=(-?\d+)(?:, data="(.*)")?')
COLOR_RE = re.compile(r"^#[0-9A-Fa-f]{6}$")
TOKEN_RE = re.compile(r"^[A-Za-z0-9_-]{16,}$")
HOST_RE = re.compile(r"^[A-Za-z0-9.:-]+$")
TOP_KEYS = {
    "version",
    "favorites",
    "hidden",
    "extraApps",
    "columns",
    "accent",
    "theme",
    "colors",
    "tileRadius",
    "wallpaper",
    "liveTvPackage",
    "homeRedirect",
    "fireTv",
    "shortcuts",
    "startup",
    "companion",
}
REDIRECT_KEYS = {"enabled", "stockHomePackage", "classHint", "showStockHomeTile"}
LEGACY_REDIRECT_KEYS = {
    "redirectHome": "enabled",
    "stockHomePackage": "stockHomePackage",
    "stockHomeClassHint": "classHint",
    "showStockHomeTile": "showStockHomeTile",
}
COMPANION_KEYS = {"mode", "host", "port", "token"}
THEMES = ("graphite", "paper", "midnight", "slate", "forest", "sand")
LEGACY_THEMES = ("dark", "light")
COLOR_KEYS = ("background", "surface", "text", "textMuted", "line")
WALLPAPER_KEYS = {"enabled", "dim"}
MAX_TILE_RADIUS = 24
SHORTCUT_KEYS = tuple(str(n) for n in range(10)) + ("red", "green", "yellow", "blue")
STARTUP_KEYS = {"app", "input"}
INPUT_ID_RE = re.compile(r"^[A-Za-z0-9_.$/-]+$")
MAX_DIM = 80


class ConfigError(Exception):
    pass


def validate(config: Any) -> list[str]:
    if not isinstance(config, dict):
        return ["the config must be a JSON object"]
    problems = [f"unknown key {k!r}" for k in sorted(set(config) - TOP_KEYS)]
    if config.get("version", 1) != 1:
        problems.append("version must be 1")
    for key in ("favorites", "hidden", "extraApps"):
        value = config.get(key, [])
        if not isinstance(value, list) or not all(isinstance(p, str) and is_package(p) for p in value):
            problems.append(f"{key} must be a list of package names")
    columns = config.get("columns", 5)
    if not isinstance(columns, int) or isinstance(columns, bool) or not 3 <= columns <= 8:
        problems.append("columns must be a whole number from 3 to 8")
    if not COLOR_RE.match(str(config.get("accent", "#6EE7B7"))):
        problems.append("accent must look like #RRGGBB")
    if config.get("theme", "graphite") not in THEMES + LEGACY_THEMES:
        problems.append(f"theme must be one of {', '.join(THEMES)}")
    colors = config.get("colors", {})
    if not isinstance(colors, dict):
        problems.append("colors must be an object")
    else:
        problems += [f"colors: unknown key {k!r}" for k in sorted(set(colors) - set(COLOR_KEYS))]
        for key in COLOR_KEYS:
            if key in colors and not COLOR_RE.match(str(colors[key])):
                problems.append(f"colors.{key} must look like #RRGGBB")
    if not _whole(config.get("tileRadius", 14), 0, MAX_TILE_RADIUS):
        problems.append(f"tileRadius must be a whole number from 0 to {MAX_TILE_RADIUS}")
    wallpaper = config.get("wallpaper", {})
    if not isinstance(wallpaper, dict):
        problems.append("wallpaper must be an object")
    else:
        problems += [f"wallpaper: unknown key {k!r}" for k in sorted(set(wallpaper) - WALLPAPER_KEYS)]
        if not isinstance(wallpaper.get("enabled", False), bool):
            problems.append("wallpaper.enabled must be true or false")
        if not _whole(wallpaper.get("dim", 30), 0, MAX_DIM):
            problems.append(f"wallpaper.dim must be a whole number from 0 to {MAX_DIM}")
    live = config.get("liveTvPackage", "")
    if live and not is_package(str(live)):
        problems.append("liveTvPackage must be a package name or empty")

    problems += _redirect_problems(config)
    problems += _shortcut_problems(config)

    companion = config.get("companion", {})
    if not isinstance(companion, dict):
        problems.append("companion must be an object")
    else:
        problems += [f"companion: unknown key {k!r}" for k in sorted(set(companion) - COMPANION_KEYS)]
        mode = companion.get("mode", "off")
        if mode not in ("off", "send", "receive"):
            problems.append("companion.mode must be 'off', 'send' or 'receive'")
        port = companion.get("port", 47201)
        if not isinstance(port, int) or isinstance(port, bool) or not 1024 <= port <= 65535:
            problems.append("companion.port must be from 1024 to 65535")
        token = companion.get("token", "")
        if mode != "off" and not (isinstance(token, str) and TOKEN_RE.match(token)):
            problems.append("companion.token needs at least 16 letters, digits, - or _ when the companion switch is on")
        if mode == "send" and not (isinstance(companion.get("host"), str) and HOST_RE.match(companion["host"])):
            problems.append("companion.host is required when companion.mode is 'send'")
    return problems


def _shortcut_problems(config: dict) -> list[str]:
    problems = []
    shortcuts = config.get("shortcuts", {})
    if not isinstance(shortcuts, dict):
        problems.append("shortcuts must be an object")
    else:
        for key, package in shortcuts.items():
            if key not in SHORTCUT_KEYS:
                problems.append(f"shortcuts: {key!r} is not a number or colour key")
            elif not isinstance(package, str) or not is_package(package):
                problems.append(f"shortcuts.{key} must be a package name")
    startup = config.get("startup", {})
    if not isinstance(startup, dict):
        problems.append("startup must be an object")
        return problems
    problems += [f"startup: unknown key {k!r}" for k in sorted(set(startup) - STARTUP_KEYS)]
    app = startup.get("app", "")
    if app and not (isinstance(app, str) and is_package(app)):
        problems.append("startup.app must be a package name or empty")
    target = startup.get("input", "")
    if target and not (isinstance(target, str) and INPUT_ID_RE.match(target)):
        problems.append("startup.input must be 'last' or a TV input id")
    if app and target:
        problems.append("startup takes either app or input, not both")
    return problems


def home_redirect(config: dict) -> dict:
    if "fireTv" in config and isinstance(config["fireTv"], dict):
        return {LEGACY_REDIRECT_KEYS.get(k, k): v for k, v in config["fireTv"].items()}
    value = config.get("homeRedirect", {})
    return value if isinstance(value, dict) else {}


def _redirect_problems(config: dict) -> list[str]:
    if "fireTv" in config and "homeRedirect" in config:
        return ["use homeRedirect, not the old fireTv key"]
    key = "fireTv" if "fireTv" in config else "homeRedirect"
    raw = config.get(key, {})
    if not isinstance(raw, dict):
        return [f"{key} must be an object"]
    allowed = set(LEGACY_REDIRECT_KEYS) if key == "fireTv" else REDIRECT_KEYS
    problems = [f"{key}: unknown key {k!r}" for k in sorted(set(raw) - allowed)]
    redirect = home_redirect(config)
    for flag in ("enabled", "showStockHomeTile"):
        if not isinstance(redirect.get(flag, False), bool):
            problems.append(f"homeRedirect.{flag} must be true or false")
    stock = redirect.get("stockHomePackage", "")
    if stock and not is_package(str(stock)):
        problems.append("homeRedirect.stockHomePackage must be a package name or empty")
    if not isinstance(redirect.get("classHint", ""), str):
        problems.append("homeRedirect.classHint must be text")
    return problems


def _whole(value: Any, low: int, high: int) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and low <= value <= high


def parse_broadcast(output: str) -> tuple[int, str]:
    match = RESULT_RE.search(output)
    if not match:
        raise ConfigError(f"unexpected reply from the launcher: {output.strip() or 'nothing'}")
    return int(match.group(1)), match.group(2) or ""


def push(tv: Shell, config: dict) -> None:
    problems = validate(config)
    if problems:
        raise ConfigError("invalid config:\n  " + "\n  ".join(problems))
    payload = base64.b64encode(json.dumps(config, separators=(",", ":")).encode("utf-8")).decode()
    out = tv.shell(f"am broadcast -n {RECEIVER} -a {ACTION_SET} --es config '{payload}'")
    code, data = parse_broadcast(out)
    if code != RESULT_OK:
        raise ConfigError(f"the launcher rejected the config: {data or 'is quietv installed?'}")


def pull(tv: Shell) -> dict:
    code, data = parse_broadcast(tv.shell(f"am broadcast -n {RECEIVER} -a {ACTION_GET}"))
    if code != RESULT_OK or not data:
        raise ConfigError("the launcher did not return its config. Is quietv installed?")
    return json.loads(base64.b64decode(data).decode("utf-8"))
