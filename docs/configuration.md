# Configuration

quietv keeps its settings on the TV. You can change most of them in the launcher's Settings screen and
by holding OK on a tile. For everything else, or to set up several TVs the same way, use a JSON file:

```bash
quietv config pull my-tv.json     # save the current settings
quietv config check my-tv.json    # validate without touching the TV
quietv config push my-tv.json     # apply and bring the launcher to the front
```

`push` replaces the whole config. Keys you leave out fall back to their defaults. Tile order is stored
separately and survives a push. Reset it in Settings.

## Example

```json
{
  "version": 1,
  "favorites": ["com.netflix.ninja", "com.google.android.youtube.tv", "com.spotify.tv.android"],
  "hidden": ["com.mediatek.wwtv.tvcenter"],
  "columns": 5,
  "accent": "#6EE7B7",
  "theme": "graphite",
  "liveTvPackage": "com.mediatek.wwtv.tvcenter"
}
```

More in [examples/](examples/).

## Keys

| Key | Default | Meaning |
|---|---|---|
| `version` | `1` | Format version. Always 1 for now. |
| `favorites` | `[]` | Package names for the favourites row, in order. Missing apps are skipped. The row has as many slots as `columns`. |
| `hidden` | `[]` | Packages never shown as tiles. |
| `extraApps` | `[]` | Apps without a TV launcher entry to show anyway, such as Silk on Fire TV. Favourites are added automatically. |
| `columns` | `5` | Tiles per row, 3 to 8. The tiles shrink to fit. |
| `accent` | `"#6EE7B7"` | Focus ring, status dots and the "Selected" label, as `#RRGGBB`. |
| `theme` | `"graphite"` | A bundled theme: `"graphite"`, `"paper"`, `"midnight"`, `"slate"`, `"forest"` or `"sand"`. `"dark"` and `"light"` still work and mean Graphite and Paper. |
| `colors` | `{}` | Your own colours on top of the theme, each as `#RRGGBB`: `background`, `surface` (tiles, menus), `text`, `textMuted` (clock date, hints, labels) and `line`. Leave a key out to keep the theme's colour. See [themes.md](themes.md). |
| `tileRadius` | `14` | Tile corner radius in dp, 0 (square) to 24. |
| `wallpaper.enabled` | `false` | Show the wallpaper image. Set one with `quietv theme wallpaper`. Without an image this does nothing. |
| `wallpaper.dim` | `30` | How much the wallpaper is darkened (or lightened on a light theme), 0 to 80 percent. quietv dims more if text would be hard to read. |
| `liveTvPackage` | `""` | The TV's tuner app, offered at the end of the Inputs menu. Empty hides the entry. |
| `homeRedirect.enabled` | `true` | Open quietv when the stock home appears. Only acts if the accessibility service is on, see [fire-tv.md](fire-tv.md) and [google-tv.md](google-tv.md). |
| `homeRedirect.stockHomePackage` | `""` | The home app to redirect. Empty means `com.amazon.tv.launcher` on Fire OS and nothing elsewhere. `quietv install` fills it in on Google TV. |
| `homeRedirect.classHint` | `"HomeActivity"` | Part of the stock home activity's class name. Other activities of the same app, such as its settings, are left alone. |
| `homeRedirect.showStockHomeTile` | `false` | Add a tile that opens the stock home once. |

Configs from quietv 0.2 and older call `homeRedirect` `fireTv`, with the keys `redirectHome`,
`stockHomePackage`, `stockHomeClassHint` and `showStockHomeTile`. They still work. `quietv config pull`
writes the new name.
| `shortcuts` | `{}` | Keys that open an app on the home screen: `"0"` to `"9"`, `"red"`, `"green"`, `"yellow"`, `"blue"`, each mapped to a package name. Set them on the TV too: hold OK on a tile, Assign to key. |
| `startup.app` | `""` | App to open once after the TV restarts. |
| `startup.input` | `""` | TV input to open once after a restart instead: `"last"` for the input you used last, or an input id from `quietv report`. Use `app` or `input`, not both. |
| `companion.mode` | `"off"` | `"send"`, `"receive"` or `"off"`. See below. |
| `companion.host` | `""` | Address of the receiving device, for `"send"`. |
| `companion.port` | `47201` | TCP port, 1024 to 65535. |
| `companion.token` | `""` | Shared secret, at least 16 letters, digits, `-` or `_`. Required unless the mode is `"off"`. |

Package names: `quietv shell pm list packages` lists all of them. In the launcher, hold OK on a tile
and choose App info.

## After a restart

`startup` runs once per restart of the TV, and only within five minutes of it. Many TVs do not restart
when you switch them off with the remote, they only go to standby. On those, `startup` runs after a
power cut or a restart from the settings.

## Companion switch

For a TV with a streaming stick on one of its HDMI ports. On the stick, the Inputs button becomes a
"Switch to TV" button. Pressing it asks the TV, over the local network, to bring its own quietv home
screen to the front.

On the TV (`receive`), quietv runs a small foreground service that listens on `companion.port` and
accepts a request only from a private network address with the right token. On the stick (`send`), set
the TV's address as `companion.host`. Use the same token on both. Generate one with
`python -c "import secrets; print(secrets.token_urlsafe(24))"`.

The traffic is plain HTTP inside your home network. The token keeps other devices on the network from
triggering the switch, but anyone who can read your network traffic can read it too.
