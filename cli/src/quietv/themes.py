import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from quietv.config import COLOR_RE, LEGACY_THEMES, MAX_DIM, ConfigError, validate


@dataclass(frozen=True)
class Preset:
    name: str
    light: bool
    background: str
    surface: str
    text: str
    text_muted: str
    text_dim: str
    line: str


PRESETS = {
    p.name: p
    for p in (
        Preset("graphite", False, "#0E1014", "#171A20", "#F3F4F6", "#9AA1AD", "#8B93A0", "#3A404B"),
        Preset("paper", True, "#F4F4F1", "#FFFFFF", "#15171C", "#5F6673", "#676E7B", "#D4D7DD"),
        Preset("midnight", False, "#000000", "#121316", "#F5F5F5", "#A1A1A8", "#8F8F96", "#2E2F33"),
        Preset("slate", False, "#0F172A", "#1E293B", "#F1F5F9", "#A3B1C6", "#94A3B8", "#334155"),
        Preset("forest", False, "#0B1510", "#15231C", "#EBF3EE", "#9DB2A5", "#8CA295", "#2B3D33"),
        Preset("sand", True, "#F2ECE2", "#FFFCF5", "#2A231B", "#685C4E", "#6F6355", "#DAD0C0"),
    )
}
LEGACY = dict(zip(LEGACY_THEMES, ("graphite", "paper"), strict=True))
DARK_ACCENTS = ("#6EE7B7", "#7DD3FC", "#FCD34D", "#FCA5A5", "#C4B5FD", "#F3F4F6")
LIGHT_ACCENTS = ("#047857", "#1D4ED8", "#92400E", "#B91C1C", "#6D28D9", "#15171C")
THEME_KEYS = {"name", "theme", "accent", "colors", "tileRadius", "wallpaper"}
WALLPAPER_KEYS = {"image", "dim"}
TARGET = 4.5
LIGHT_THRESHOLD = 0.4


@dataclass
class Theme:
    settings: dict[str, Any] = field(default_factory=dict)
    image: Path | None = None


def preset_name(theme: str) -> str:
    return LEGACY.get(theme, theme)


def load(source: str) -> Theme:
    if preset_name(source) in PRESETS:
        return Theme({"theme": preset_name(source), "colors": {}})
    path = Path(source)
    if not path.is_file():
        raise ConfigError(f"{source!r} is neither a bundled theme ({', '.join(PRESETS)}) nor a theme file")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        raise ConfigError(f"{path} is not valid JSON: {e}") from e
    problems = check(data)
    if problems:
        raise ConfigError(f"invalid theme {path}:\n  " + "\n  ".join(problems))

    settings: dict[str, Any] = {"theme": preset_name(data.get("theme", "graphite")), "colors": data.get("colors", {})}
    for key in ("accent", "tileRadius"):
        if key in data:
            settings[key] = data[key]
    image = None
    wallpaper = data.get("wallpaper")
    if wallpaper is False:
        settings["wallpaper"] = {"enabled": False}
    elif isinstance(wallpaper, dict):
        image = (path.parent / wallpaper["image"]).resolve()
        settings["wallpaper"] = {"enabled": True, "dim": wallpaper.get("dim", 30)}
    return Theme(settings, image)


def check(data: Any) -> list[str]:
    if not isinstance(data, dict):
        return ["a theme must be a JSON object"]
    problems = [f"unknown key {k!r}" for k in sorted(set(data) - THEME_KEYS)]
    if "name" in data and not isinstance(data["name"], str):
        problems.append("name must be text")
    problems += validate({k: v for k, v in data.items() if k in ("theme", "accent", "colors", "tileRadius")})
    wallpaper = data.get("wallpaper", False)
    if wallpaper is not False:
        if not isinstance(wallpaper, dict):
            problems.append("wallpaper must be an object with an image, or false")
        else:
            problems += [f"wallpaper: unknown key {k!r}" for k in sorted(set(wallpaper) - WALLPAPER_KEYS)]
            if not isinstance(wallpaper.get("image"), str) or not wallpaper["image"]:
                problems.append("wallpaper.image must be the path of an image file")
            dim = wallpaper.get("dim", 30)
            if not isinstance(dim, int) or isinstance(dim, bool) or not 0 <= dim <= MAX_DIM:
                problems.append(f"wallpaper.dim must be a whole number from 0 to {MAX_DIM}")
    return problems


def merge(current: dict[str, Any], theme: Theme) -> dict[str, Any]:
    result = dict(current)
    was_light = palette(current)["light"]
    for key, value in theme.settings.items():
        if key == "wallpaper":
            result["wallpaper"] = {**current.get("wallpaper", {}), **value}
        else:
            result[key] = value
    if "accent" not in theme.settings:
        result["accent"] = map_accent(current.get("accent", DARK_ACCENTS[0]), was_light, palette(result)["light"])
    return result


def map_accent(accent: str, from_light: bool, to_light: bool) -> str:
    source = LIGHT_ACCENTS if from_light else DARK_ACCENTS
    target = LIGHT_ACCENTS if to_light else DARK_ACCENTS
    upper = accent.upper()
    return target[source.index(upper)] if upper in source else accent


def palette(config: dict[str, Any]) -> dict[str, Any]:
    preset = PRESETS.get(preset_name(config.get("theme", "graphite")), PRESETS["graphite"])
    colors = config.get("colors", {})
    background = colors.get("background", preset.background)
    muted = colors.get("textMuted")
    return {
        "light": luminance(background) > LIGHT_THRESHOLD if "background" in colors else preset.light,
        "background": background,
        "surface": colors.get("surface", preset.surface),
        "text": colors.get("text", preset.text),
        "textMuted": muted or preset.text_muted,
        "textDim": muted or preset.text_dim,
        "line": colors.get("line", preset.line),
    }


def contrast_warnings(config: dict[str, Any]) -> list[str]:
    p = palette(config)
    warnings = []
    for role in ("text", "textMuted"):
        for under in ("background", "surface"):
            value = ratio(p[role], p[under])
            if value < TARGET:
                warnings.append(f"{role} on {under} has a contrast of {value:.1f}:1, below {TARGET}:1")
    accent = config.get("accent", DARK_ACCENTS[0])
    value = ratio(accent, p["background"])
    if value < TARGET:
        warnings.append(f"accent on background has a contrast of {value:.1f}:1, below {TARGET}:1")
    return warnings


def luminance(color: str) -> float:
    if not COLOR_RE.match(color):
        raise ValueError(f"not a colour: {color}")
    channels = [int(color[i : i + 2], 16) / 255 for i in (1, 3, 5)]
    r, g, b = (c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def ratio(a: str, b: str) -> float:
    la, lb = luminance(a), luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
