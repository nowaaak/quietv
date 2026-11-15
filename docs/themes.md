# Themes

quietv comes with seven themes. Pick one on the TV (Settings, Theme) or from your computer:

```bash
quietv theme list
quietv theme apply midnight
```

| Theme | Look |
|---|---|
| `graphite` | Dark grey, the default |
| `paper` | Light, warm white |
| `midnight` | Pure black, kind to OLED screens |
| `slate` | Dark blue grey |
| `forest` | Dark green |
| `sand` | Light, beige |
| `glass` | Dark, in the style of Apple TV, see [Glass](#glass) |

On the TV, Settings, Theme shows every theme as a small picture of the home screen, in its own
colours and with your accent colour and tile corners. The dot marks the theme in use. OK picks the
focused theme, Back leaves everything as it was.

Switching themes keeps your accent colour. Between a dark and a light theme it picks the matching
shade of the same colour.

## Accent colour

Settings, Accent colour offers twelve colours. Each has a shade for dark and one for light themes,
and both are easy to read on every bundled theme. While you move between them, the picture above
shows the home screen with the focused colour. Custom colour opens sliders for hue, saturation and
brightness: left and right change the value, holding the key speeds it up. If a colour is hard to
see on your theme, the picker says so, but lets you keep it. From your computer, set `accent` in the
[config](configuration.md).

## Your own theme

A theme file is a small JSON file. Start from one of the [examples](examples/themes/):

```json
{
  "name": "Ocean",
  "theme": "slate",
  "accent": "#5EEAD4",
  "colors": {
    "background": "#071A2B",
    "surface": "#0E2A40",
    "text": "#EAF4FB",
    "textMuted": "#9DB8CC",
    "line": "#1F4560"
  },
  "tileRadius": 22
}
```

```bash
quietv theme apply ocean.json
```

| Key | Meaning |
|---|---|
| `name` | Optional, for you. |
| `theme` | The bundled theme your colours start from. |
| `accent` | Focus ring and highlights, `#RRGGBB`. Leave it out to keep the current accent. |
| `colors` | Any of `background`, `surface`, `text`, `textMuted`, `line`. Missing ones come from `theme`. |
| `tileRadius` | Tile corners in dp, 0 to 24. |
| `wallpaper` | `{"image": "photo.jpg", "dim": 30}` to set a wallpaper, `false` to switch it off. The path is relative to the theme file. |

`quietv theme apply` warns when text would be hard to read on your colours. It checks the same
contrast ratio as the bundled themes (4.5:1, WCAG AA).

## Wallpaper

```bash
quietv theme wallpaper photo.jpg --dim 30
quietv theme wallpaper --off
```

JPEG, PNG or WebP up to 8 MB. A 1920 by 1080 picture is plenty. Larger images are scaled down on
the TV. The picture is copied to the launcher's private storage. It never leaves your network.

quietv lays a layer of the theme's background colour over the picture. `--dim` sets how strong it
is. If the picture is too bright behind the clock and the labels, quietv makes the layer stronger
until all text is readable. A few bright spots do not count, so a mostly dark photo keeps your
setting.

## Background

On the TV, Settings, Background switches between the theme's colour, four built-in landscapes and
your own picture, once you have set one: Mountains (dusk), Mountain lake (night), Dunes (sunset) and
Forest (morning mist). The landscapes are drawn by the launcher, no download, no photo. A second
row sets the dimming. In the config they are `wallpaper.scene`: `"mountains"`, `"lake"`, `"dunes"`
or `"forest"`, with `wallpaper.enabled` set to `true`. An empty scene shows your own picture.

## Glass

`glass` looks like Apple TV rather than like the other themes. The focused tile lifts with a soft
shadow instead of the accent ring, app names only appear under the focused tile, the large title
above the favourites is left out so the background shows, and Settings groups its rows on cards.
Tiles, cards, menus and the search field share a fine light edge, brighter at the top left. The top
buttons have no frame. Focused, they turn into a white circle with a glow in your accent colour.
Nothing is blurred,
everything is drawn once, so the launcher stays as light as with the other themes. On a landscape
or your own picture the darkening is a gradient: strong behind the clock and the tiles, light in
the middle.
