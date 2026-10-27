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
    "liveTvPackage",
    "fireTv",
    "companion",
}
FIRE_TV_KEYS = {"redirectHome", "stockHomePackage", "stockHomeClassHint", "showStockHomeTile"}
COMPANION_KEYS = {"mode", "host", "port", "token"}


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
    if config.get("theme", "dark") not in ("dark", "light"):
        problems.append("theme must be 'dark' or 'light'")
    live = config.get("liveTvPackage", "")
    if live and not is_package(str(live)):
        problems.append("liveTvPackage must be a package name or empty")

    fire = config.get("fireTv", {})
    if not isinstance(fire, dict):
        problems.append("fireTv must be an object")
    else:
        problems += [f"fireTv: unknown key {k!r}" for k in sorted(set(fire) - FIRE_TV_KEYS)]
        for flag in ("redirectHome", "showStockHomeTile"):
            if not isinstance(fire.get(flag, False), bool):
                problems.append(f"fireTv.{flag} must be true or false")
        stock = fire.get("stockHomePackage", "")
        if stock and not is_package(str(stock)):
            problems.append("fireTv.stockHomePackage must be a package name or empty")

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
