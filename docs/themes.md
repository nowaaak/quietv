# Themes

quietv comes with three themes. Pick one on the TV (Settings, Theme) or from your computer:

```bash
quietv theme list
quietv theme apply glass
```

| Theme | Look |
|---|---|
| `graphite` | Dark grey, the default |
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
shows the home screen with the focused colour. Custom colour has a field for the hex code
(`#RRGGBB`, OK opens the keyboard) and sliders for red, green and blue from 0 to 255: left and
right change the value, holding the key speeds it up. Field, sliders and preview follow each other.
If a colour is hard to see on your theme, the picker says so, but lets you keep it. From your
computer, set `accent` in the [config](configuration.md).

## Your own theme

A theme file is a small JSON file. Start from one of the [examples](examples/themes/):

```json
{
  "name": "Ocean",
  "theme": "graphite",
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
setting. On Glass this only applies where text is, behind the clock and the tiles: in the
middle of the screen, where there is only the picture, `--dim` alone decides, so it always shows.

## Background

On the TV, Settings, Background opens a list of the theme's colour, four built-in landscapes and
your own picture, once you have set one: Mountains (dusk), Mountain lake (night), Dunes (sunset) and
Forest (morning mist). Next to the list a picture of the home screen shows the focused background,
and the slider below sets the dimming from 0 to 80 percent, with the picture following along. OK
keeps both, Back leaves everything as it was. The landscapes are drawn by the launcher, no
download, no photo. In the config they are `wallpaper.scene`: `"mountains"`, `"lake"`, `"dunes"`
or `"forest"`, with `wallpaper.enabled` set to `true`. An empty scene shows your own picture.

## Tile corners

Settings, Tile corners opens a slider from 0 (square) to 24 dp with a picture of the home screen
above it that changes as you move it. OK keeps the value, Back leaves it as it was.

## Glass

`glass` looks like Apple TV rather than like the other themes. The focused tile lifts with a soft
shadow instead of the accent ring, app names only appear under the focused tile, the large title
above the favourites is left out so the background shows, and Settings groups its rows on cards.
Tiles, cards, menus and the search field share a fine light edge, brighter at the top left. The top
buttons have no frame. Focused, they turn into a white circle with a glow in your accent colour.
Nothing is blurred and every gradient is drawn once into a picture, so switching between apps is
as smooth as with the other themes, even on a Fire TV stick. On a landscape
or your own picture the darkening is a gradient: strong behind the clock and the tiles, light in
the middle.

The colours are neutral: a near-black graphite background with a faint gradient, a soft white light
at the top left and grey surfaces. Only your accent colour adds a tint.
