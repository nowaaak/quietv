import base64
import json

import pytest

from quietv import config
from quietv.config import ConfigError

EXAMPLE = {
    "version": 1,
    "favorites": ["com.netflix.ninja", "com.google.android.youtube.tv"],
    "hidden": ["com.mediatek.wwtv.tvcenter"],
    "columns": 5,
    "accent": "#6EE7B7",
    "theme": "dark",
    "liveTvPackage": "com.mediatek.wwtv.tvcenter",
    "fireTv": {"redirectHome": True, "stockHomePackage": "com.amazon.tv.launcher"},
    "companion": {"mode": "off"},
}


def test_examples_in_the_repo_are_valid(repo_root):
    files = sorted((repo_root / "docs" / "examples").glob("*.json"))
    assert files
    for path in files:
        assert config.validate(json.loads(path.read_text(encoding="utf-8"))) == [], path.name


def test_valid_config_has_no_problems():
    assert config.validate(EXAMPLE) == []
    assert config.validate({}) == []
    appearance = {
        "theme": "midnight",
        "colors": {"background": "#000000", "textMuted": "#A1A1A8"},
        "tileRadius": 0,
        "wallpaper": {"enabled": True, "dim": 80},
    }
    assert config.validate({**EXAMPLE, **appearance}) == []


@pytest.mark.parametrize(
    ("patch", "message"),
    [
        ({"columns": 12}, "columns"),
        ({"columns": True}, "columns"),
        ({"accent": "green"}, "accent"),
        ({"theme": "blue"}, "theme"),
        ({"favorites": ["not a package"]}, "favorites"),
        ({"dock": []}, "unknown key 'dock'"),
        ({"companion": {"mode": "receive", "token": "short"}}, "token"),
        ({"companion": {"mode": "send", "token": "x" * 16}}, "companion.host"),
        ({"fireTv": {"redirectHome": "yes"}}, "redirectHome"),
        ({"colors": {"text": "white"}}, "colors.text"),
        ({"colors": {"glow": "#FFFFFF"}}, "colors: unknown key 'glow'"),
        ({"colors": []}, "colors must be an object"),
        ({"tileRadius": 30}, "tileRadius"),
        ({"tileRadius": False}, "tileRadius"),
        ({"wallpaper": {"enabled": "yes"}}, "wallpaper.enabled"),
        ({"wallpaper": {"dim": 90}}, "wallpaper.dim"),
        ({"wallpaper": {"blur": 3}}, "wallpaper: unknown key 'blur'"),
    ],
)
def test_invalid_values_are_reported(patch, message):
    problems = config.validate({**EXAMPLE, **patch})
    assert any(message in p for p in problems), problems


def test_push_sends_base64_json_to_the_receiver(fake_tv):
    tv = fake_tv({"am broadcast": "Broadcasting: Intent { }\nBroadcast completed: result=-1\n"})
    config.push(tv, EXAMPLE)
    command = tv.commands[0]
    assert f"-n {config.RECEIVER}" in command
    payload = command.split("--es config ")[1].strip("'")
    assert json.loads(base64.b64decode(payload)) == EXAMPLE


def test_push_reports_rejection(fake_tv):
    tv = fake_tv({"am broadcast": 'Broadcast completed: result=0, data="bad accent"\n'})
    with pytest.raises(ConfigError, match="bad accent"):
        config.push(tv, EXAMPLE)


def test_push_refuses_invalid_config_without_touching_the_tv(fake_tv):
    tv = fake_tv()
    with pytest.raises(ConfigError, match="columns"):
        config.push(tv, {"columns": 99})
    assert tv.commands == []


def test_pull_decodes_the_reply(fake_tv):
    payload = base64.b64encode(json.dumps(EXAMPLE).encode()).decode()
    tv = fake_tv({"am broadcast": f'Broadcast completed: result=-1, data="{payload}"\n'})
    assert config.pull(tv) == EXAMPLE


def test_pull_without_launcher_fails(fake_tv):
    tv = fake_tv({"am broadcast": "Broadcast completed: result=0\n"})
    with pytest.raises(ConfigError, match="installed"):
        config.pull(tv)
