# quietv

**A calm home screen for your TV. No ads, no rows of things you never asked for.**

quietv strips the ads, telemetry and preloaded clutter from Android TV, Google TV and Fire TV over ADB,
and replaces the stock home screen with a small, fast launcher: your apps, your order, nothing else.

- **One APK for every TV.** Download it from Releases, no Android SDK needed. Settings live on the TV.
- **Fire TV and Google TV too.** Where the stock home cannot be replaced safely, quietv keeps it and opens
  itself instead.
- **Reversible.** Packages are disabled, never uninstalled. Every debloat starts with a backup.
- **Light.** Built for sets with 1 GB of RAM. No tracking, no background work, no network traffic
  unless you switch on the optional companion feature.
- **One CLI for Windows, macOS and Linux**, or let an AI coding agent do the setup for you.

![quietv home screen with the favourites row](docs/screenshots/home.png)

| All apps | Hold OK on a tile | Inputs |
|---|---|---|
| ![App grid](docs/screenshots/apps.png) | ![Tile menu](docs/screenshots/tile-menu.png) | ![Inputs menu](docs/screenshots/inputs.png) |

| Settings | Paper theme |
|---|---|
| ![Settings](docs/screenshots/settings.png) | ![Paper theme](docs/screenshots/paper.png) |

Screenshots from a CHiQ AI PONT (Android TV 11), taken with `quietv screenshot`.

## Quick start

You need the TV's IP address and ADB debugging switched on:
Settings → Device Preferences → About → press OK on **Build** seven times, then
Developer options → **USB debugging** (and **Network debugging** if your TV has it).
On Fire TV: Settings → My Fire TV → Developer options → **ADB debugging**.

### With the CLI

```bash
uv tool install "git+https://github.com/nowaaak/quietv#subdirectory=cli"   # or: pipx install ...
export QUIETV_HOST=192.168.1.50                                           # PowerShell: $env:QUIETV_HOST="..."

quietv probe                                  # model, Android version, current home
quietv backup                                 # packages, settings, home app, keyboard
quietv debloat chiq-ai-pont --dry-run         # a list from devices/, by name or path
quietv debloat chiq-ai-pont                   # disables, after an automatic backup
quietv install                                # latest release, checksum verified
```

Accept the "Allow USB debugging?" prompt on the TV the first time. `quietv restore` undoes the debloat
from the newest backup. `quietv --help` lists everything.

Some newer TVs only offer **Wireless debugging** with a pairing code. Open it, choose "Pair device with
pairing code" and run `quietv pair <pairing port> <code> <port>` once. This needs Android's `adb` tool,
everything after it does not.

After a firmware update, `quietv check` finds packages the update switched back on and
`quietv check --fix` disables them again. quietv never disables packages a TV needs to work, such as
TV settings, the keyboard in use or the services behind the HDMI inputs, even if a device list says so.

### With an AI coding agent

Clone this repo and ask Claude Code, Codex, Gemini CLI or a similar agent to set up your TV.
[AGENTS.md](AGENTS.md) tells it how: back up first, sort the packages into categories, ask before
touching apps you might use, verify every step with a screenshot from the TV.

### Just the launcher

Download `quietv.apk` from [Releases](https://github.com/nowaaak/quietv/releases), check it against
`SHA256SUMS` and install it with any ADB tool. On Fire TV, follow [docs/fire-tv.md](docs/fire-tv.md).
No computer? See [docs/phone-install.md](docs/phone-install.md).

## The launcher

- Your favourites in a row at the bottom, the rest of your apps in a grid below. Press Down to see them.
- One focus ring that springs from tile to tile. The focused favourite's name stands above the row.
- Hold OK on a tile to move it, hide it or open its app info.
- Inputs menu with HDMI ports, AV and Live TV through the TV input framework. It remembers the last one.
- Search your apps with the remote. Number and colour keys open your favourite apps.
- A clock screensaver that moves a little every minute: `quietv screensaver on`.
- Six themes, your own colours, tile corners and an optional wallpaper that stays readable.
  [Themes](docs/themes.md).
- Settings on the TV: theme, accent colour, tile corners, tiles per row, wallpaper, hidden apps.
- Clock, date and greeting in [Geist](https://vercel.com/font). English and German.

Everything is configurable on the TV. To set up many TVs the same way, keep a JSON file and push it:

```bash
quietv config pull my-tv.json
quietv config push my-tv.json
```

See [docs/configuration.md](docs/configuration.md) for every option.

## Device lists

[`devices/`](devices/) holds one TOML file per tested TV. Each package has a category (ads,
telemetry, cast, assistant, ...) and a tier:

| Tier | Meaning |
|---|---|
| `recommended` | Ads, telemetry and clutter. Safe for everyone. |
| `advanced` | Features some people use: casting, voice, vendor updates, preloaded streaming apps. |
| `expert` | Can break something. Read the note first. |

`quietv debloat` applies `recommended` by default. Use `--tier advanced` to go further and
`--exclude cast` to skip a category.

| TV | OS | Checked with quietv | File |
|---|---|---|---|
| Fire TV Stick 4K Max (2nd gen) | Fire OS 8.1.8.5 (Android 11) | yes | [amazon-fire-tv-stick-4k-max-2.toml](devices/amazon-fire-tv-stick-4k-max-2.toml) |
| CHiQ AI PONT | Android TV 11 | yes | [chiq-ai-pont.toml](devices/chiq-ai-pont.toml) |
| Chromecast with Google TV (4K) | Google TV, Android 14 | no, imported | [google-chromecast-google-tv-4k.toml](devices/google-chromecast-google-tv-4k.toml) |
| Google TV Streamer 4K | Google TV, Android 14 | no, imported | [google-tv-streamer-4k.toml](devices/google-tv-streamer-4k.toml) |
| Hisense SmartTV 4K FFM (Android 11) | Google TV 11 | no, imported | [hisense-smarttv-4k-ffm-android11.toml](devices/hisense-smarttv-4k-ffm-android11.toml) |
| NVIDIA SHIELD TV Pro (2019) | Android TV 11 | no, imported | [nvidia-shield-tv-pro-2019.toml](devices/nvidia-shield-tv-pro-2019.toml) |
| onn 4K Streaming Box (2023) | Google TV, Android 14 | no, imported | [onn-4k-streaming-box-2023.toml](devices/onn-4k-streaming-box-2023.toml) |
| Philips TPM191E (50PUS8505, 55OLED854, 65OLED804, 50PUS7354) | Android TV 12 | no, imported | [philips-tpm191e.toml](devices/philips-tpm191e.toml) |
| Sony BRAVIA 4K VH21 (EU) | Google TV 12 | no, imported | [sony-bravia-vh21-eu.toml](devices/sony-bravia-vh21-eu.toml) |
| Sony BRAVIA VU31 4K | Google TV 14 | no, imported | [sony-bravia-vu31.toml](devices/sony-bravia-vu31.toml) |
| TCL Smart TV Pro (G08, EU firmware) | Google TV (Android 12) | no, imported | [tcl-g08-eu-android12.toml](devices/tcl-g08-eu-android12.toml) |
| TCL Smart TV Pro (G09) | Google TV (Android 12) | no, imported | [tcl-g09-google-tv.toml](devices/tcl-g09-google-tv.toml) |
| TCL 4K Google TV (G10) | Google TV (Android 12) | no, imported | [tcl-g10-google-tv.toml](devices/tcl-g10-google-tv.toml) |
| Toshiba Fire TV | Fire OS (Android 9) | by the project quietv grew out of | [toshiba-fire-tv-aftha004.toml](devices/toshiba-fire-tv-aftha004.toml) |
| Xiaomi Mi Box S 2nd gen | Google TV 11 | no, imported | [xiaomi-mi-box-s-2nd-gen.toml](devices/xiaomi-mi-box-s-2nd-gen.toml) |
| Xiaomi Mi TV 4A | Android TV 10 | by the project quietv grew out of | [xiaomi-mi-tv-4a.toml](devices/xiaomi-mi-tv-4a.toml) |

Lists marked "imported" come from [cleanyourtv](https://github.com/Gathin23/cleanyourtv)'s device reports,
sorted into quietv's categories and tiers. Nobody has run them with quietv yet. Start with
`--dry-run`, and please report back how it went.

Your TV is missing? [Add it](CONTRIBUTING.md#add-your-tv). It is the most useful contribution there is.
`quietv report` writes everything the device report needs into one file.

## Supported TVs

Android TV and Google TV from any brand (Sony, TCL, Hisense, Xiaomi, Philips, CHiQ, Nvidia Shield and
others) and Amazon Fire TV. Not Samsung Tizen or LG webOS. Android 8.0 or newer.

## Documentation

- [Configuration](docs/configuration.md)
- [Themes](docs/themes.md)
- [Fire TV](docs/fire-tv.md)
- [Privacy report](docs/privacy.md)
- [TVs without Android](docs/other-tvs.md)
- [Reproducible builds](docs/reproducible-builds.md)
- [Google TV](docs/google-tv.md)
- [Troubleshooting](docs/troubleshooting.md)
- [Install from a phone](docs/phone-install.md)
- [Roadmap](ROADMAP.md)

## Contributing

Device lists, launcher features, translations and fixes are welcome. Read
[CONTRIBUTING.md](CONTRIBUTING.md) first.

## Credits and licence

quietv started as a fork of [cleanyourtv](https://github.com/Gathin23/cleanyourtv) by Gathin, with Fire TV
support by Umang Chauhan. See [NOTICE.md](NOTICE.md).

MIT, see [LICENSE](LICENSE). The Geist typeface is under the SIL Open Font License, see
[launcher/FONT-LICENSE-OFL.txt](launcher/FONT-LICENSE-OFL.txt).
