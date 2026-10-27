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
  "theme": "dark",
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
| `theme` | `"dark"` | `"dark"` (Graphite) or `"light"` (Paper). |
| `liveTvPackage` | `""` | The TV's tuner app, offered at the end of the Inputs menu. Empty hides the entry. |
| `fireTv.redirectHome` | `true` | Open quietv when the stock home appears. Only acts if the accessibility service is on, see [fire-tv.md](fire-tv.md). |
| `fireTv.stockHomePackage` | `""` | The home app to redirect. Empty means `com.amazon.tv.launcher` on Fire OS and nothing elsewhere. |
| `fireTv.stockHomeClassHint` | `"HomeActivity"` | Part of the stock home activity's class name. Other activities of the same app, such as its settings, are left alone. |
| `fireTv.showStockHomeTile` | `false` | Add a tile that opens the stock home once. |
| `companion.mode` | `"off"` | `"send"`, `"receive"` or `"off"`. See below. |
| `companion.host` | `""` | Address of the receiving device, for `"send"`. |
| `companion.port` | `47201` | TCP port, 1024 to 65535. |
| `companion.token` | `""` | Shared secret, at least 16 letters, digits, `-` or `_`. Required unless the mode is `"off"`. |

Package names: `quietv shell pm list packages` lists all of them. In the launcher, hold OK on a tile
and choose App info.

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
