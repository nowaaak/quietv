import json
import re

import pytest

from quietv import config, themes
from quietv.config import ConfigError

KOTLIN = "launcher/app/src/main/java/io/github/nowaaak/quietv"


def test_presets_match_the_launcher(repo_root):
    source = (repo_root / KOTLIN / "config" / "ThemePreset.kt").read_text(encoding="utf-8")
    entries = re.findall(r'\n    [A-Z]+\(\s*"([a-z]+)", (true|false),((?:\s*0xFF[0-9A-F]{6}\.toInt\(\),?){6})', source)
    assert [name for name, _, _ in entries] == list(themes.PRESETS)
    for name, light, values in entries:
        preset = themes.PRESETS[name]
        colors = ["#" + c for c in re.findall(r"0xFF([0-9A-F]{6})", values)]
        assert preset.light == (light == "true"), name
        expected = [preset.background, preset.surface, preset.text, preset.text_muted, preset.text_dim, preset.line]
        assert colors == expected, name
    assert tuple(themes.PRESETS) == config.THEMES


def test_accents_match_the_launcher(repo_root):
    source = (repo_root / KOTLIN / "ui" / "ThemeColors.kt").read_text(encoding="utf-8")
    light = re.search(r"LIGHT_ACCENTS = intArrayOf\((.*?)\)\n", source, re.S).group(1)
    dark = re.search(r"DARK_ACCENTS = intArrayOf\((.*?)\)\n", source, re.S).group(1)
    assert tuple("#" + c for c in re.findall(r"0xFF([0-9A-F]{6})", light)) == themes.LIGHT_ACCENTS
    assert tuple("#" + c for c in re.findall(r"0xFF([0-9A-F]{6})", dark)) == themes.DARK_ACCENTS[1:]
    assert themes.DARK_ACCENTS[0] == "#6EE7B7"


def test_bundled_themes_are_readable_with_every_accent():
    for name, preset in themes.PRESETS.items():
        assert themes.contrast_warnings(themes.merge({}, themes.load(name))) == [], name
        for accent in themes.LIGHT_ACCENTS if preset.light else themes.DARK_ACCENTS:
            assert themes.contrast_warnings({"theme": name, "accent": accent}) == [], (name, accent)


def test_bundled_name_resets_custom_colours():
    theme = themes.load("midnight")
    merged = themes.merge({"theme": "graphite", "colors": {"text": "#FFFFFF"}, "accent": "#7DD3FC"}, theme)
    assert merged["theme"] == "midnight"
    assert merged["colors"] == {}
    assert merged["accent"] == "#7DD3FC"


def test_switching_to_a_light_theme_maps_the_accent():
    merged = themes.merge({"theme": "dark", "accent": "#FCD34D"}, themes.load("sand"))
    assert merged["accent"] == "#92400E"
    merged = themes.merge({"theme": "graphite", "accent": "#123456"}, themes.load("paper"))
    assert merged["accent"] == "#123456"


def test_theme_file_with_wallpaper(tmp_path):
    (tmp_path / "sea.jpg").write_bytes(b"\xff\xd8\xff\xe0 fake")
    path = tmp_path / "ocean.json"
    path.write_text(
        json.dumps(
            {
                "name": "Ocean",
                "theme": "slate",
                "colors": {"background": "#04223A"},
                "tileRadius": 22,
                "wallpaper": {"image": "sea.jpg", "dim": 40},
            }
        ),
        encoding="utf-8",
    )
    theme = themes.load(str(path))
    assert theme.image == (tmp_path / "sea.jpg").resolve()
    merged = themes.merge({"theme": "graphite", "wallpaper": {"enabled": False, "dim": 10}}, theme)
    assert merged["colors"] == {"background": "#04223A"}
    assert merged["tileRadius"] == 22
    assert merged["wallpaper"] == {"enabled": True, "dim": 40}
    assert config.validate(merged) == []


def test_invalid_theme_files_are_reported(tmp_path):
    path = tmp_path / "bad.json"
    path.write_text(
        json.dumps({"theme": "neon", "colors": {"glow": "#FFFFFF"}, "wallpaper": {"dim": 99}, "font": "x"}),
        encoding="utf-8",
    )
    with pytest.raises(ConfigError) as error:
        themes.load(str(path))
    message = str(error.value)
    for part in ("theme must be", "glow", "wallpaper.image", "wallpaper.dim", "unknown key 'font'"):
        assert part in message


def test_unknown_source_names_the_bundled_themes():
    with pytest.raises(ConfigError, match="graphite, paper"):
        themes.load("does-not-exist")


def test_low_contrast_custom_colours_are_flagged():
    warnings = themes.contrast_warnings({"theme": "graphite", "colors": {"textMuted": "#30343C"}})
    assert any(w.startswith("textMuted on background") for w in warnings)


def test_example_themes_are_valid(repo_root):
    files = sorted((repo_root / "docs" / "examples" / "themes").glob("*.json"))
    assert files
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8"))
        assert themes.check(data) == [], path.name
        assert themes.contrast_warnings(themes.merge({}, themes.load(str(path)))) == [], path.name
