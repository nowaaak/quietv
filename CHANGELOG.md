# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com)
and the project uses [Semantic Versioning](https://semver.org).

## [Unreleased]

### Removed

- Themes Paper, Midnight, Slate and Forest. Configs and theme files that name them keep working:
  `paper` (and `light`) become Sand, `midnight`, `slate` and `forest` become Graphite.

### Changed

- Theme `glass` is now a neutral graphite: near black with a soft white light at the top left, grey
  surfaces and lines, instead of indigo and violet.
- Settings, Appearance, Theme opens a picker with a small picture of the home screen for every
  theme, instead of switching to the next theme on each press. You see what you get before you
  choose.
- Settings, Appearance, Accent colour opens a picker with twelve colours (six more than before) and
  a live preview of the home screen. Custom colour offers a hex field (`#RRGGBB`) and sliders for
  red, green and blue from 0 to 255, and the picker warns when a colour is hard to see on the theme.
- Settings, Appearance, Tile corners opens a slider from 0 to 24 dp with a live picture of the home
  screen, instead of stepping through four fixed shapes.
- Settings, Appearance, Background opens a list with a live picture of the home screen and the
  dimming slider below it, in 5 % steps up to 80 %, instead of stepping through both settings blind.
- Glass switches between apps as smoothly as the other themes. Its background gradient is drawn
  once into a picture, and on every theme the wallpaper dimming is drawn into the wallpaper itself,
  so the TV draws one full-screen layer less per frame.
- Menus and pickers on Glass are opaque, so nothing of the screen behind them shows through.
- Settings has a new layout: categories (Appearance, Apps, Remote, Home button, System) on the left,
  the settings of the focused category on the right, in cards on every theme. Moving through the
  categories shows their settings right away, Right or OK goes in, Left or Back comes back.
  Appearance shows a live picture of the home screen. The list keeps room above and below, the
  focused row keeps a margin to the edge of the screen, long names wrap, and hints are plain text.
- On Glass the wallpaper dimming always shows. The darkening that keeps text readable now only
  applies behind the clock and the tiles, the middle of the screen follows the dimming setting.
  Before, a bright picture like Dunes needed more darkening than the setting allowed, so it changed
  nothing.

### Fixed

- Search: the name of the focused app was white on the white highlight and could not be read.

## [0.10.1] - 2026-11-16

### Fixed

- The network button shows a LAN icon on a wired connection instead of the Wi-Fi icon.

## [0.10.0] - 2026-11-15

### Added

- Theme `glass` in the style of Apple TV: the focused tile lifts with a shadow instead of the accent ring,
  names only under the focused tile, no title block, Settings grouped on cards, a fine light edge on tiles,
  cards, menus and the search field, and borderless top buttons that turn into a white circle with an
  accent glow when focused. No blur, no animation beyond the focus lift. Pick it in Settings or with
  `quietv theme apply glass`.
- The top buttons are smaller in every theme.
- Background setting for every theme: the theme's colour, four built-in landscapes (mountains, mountain
  lake, dunes, forest) or your own picture. New config key `wallpaper.scene`.

## [0.9.1] - 2026-11-12

### Fixed

- `quietv restore` always enables launcherx before any other package. On Google TV, the setup wizard
  with launcherx still disabled can leave the TV stuck at the boot animation (cleanyourtv #3578).
- The clock uses digits of equal width, so the date next to it no longer shifts every minute.

### Changed

- Device lists: notes from more units for the Hisense FFM, Philips TPM191E, Sony BRAVIA VU31, TCL G08 and
  TCL G10 lists, and `com.homwee.howtocast` for Changhong AI PONT sets. Thanks to the cleanyourtv community.
- Troubleshooting: Private DNS and the launcherx order on Google TV.

## [0.9.0] - 2026-11-12

### Added

- Companion switch: on the streaming stick, the Inputs button lists the TV's inputs, so you can switch
  the TV to another HDMI port, for example a game console, without the TV remote.

## [0.8.0] - 2026-11-05

### Added

- `inputsKey` in the config: the remote's Input key opens the quietv Inputs menu, for TVs where it does
  nothing once the stock launcher is disabled.
- `quietv probe` warns when the TV accepts ADB from any computer without asking.

### Changed

- Debloat refuses more packages that are known to stop a TV from booting or to break it, among them
  `com.android.sdksandbox`, `com.android.location.fused`, the permission controller, system content
  providers and module overlays. It also keeps the apps that open the TV settings and the inputs, and
  packages that use the TV's input hardware.
- Device lists: new keep entries for the Fire TV Stick 4K Max, the CHiQ AI PONT and the Xiaomi Mi TV 4A,
  ZEE5 and the TCL art mode moved to `advanced`. Thanks to the cleanyourtv community for the reports.
- `quietv install` warns when the TV has more than one user and never disables a stock launcher the
  TV cannot boot without.
- Package lists are read for the main user only, so a second profile no longer hides what is enabled.

### Fixed

- Home redirect: a firmware that unbinds accessibility services while it shuts down turned the redirect
  off for good. A redirect switched off with the global accessibility switch is now also noticed and
  switched back on.
- Google TV: the redirect no longer covers screens the stock home opens right after itself, such as the
  profile chooser. Not yet checked on a Google TV device.
- HDMI entries in the Inputs menu open in the Live TV app first, so no "Open with" choice appears.
- A connection that drops during a command ends with a message instead of a traceback. An interrupted
  debloat still records what it disabled, so `quietv check` knows it.
- A failed screenshot no longer empties an existing file, and the screenshot is removed from the TV.
- Connection errors name the likely cause: debugging off, network isolation, a blocking firewall on
  Windows or the Local Network permission on macOS.
- Docs: the macOS Local Network entry was wrong, quietv is affected too. Menu paths for LG, Samsung,
  Roku and Vizio follow what users reported, and Hisense VIDAA has a section.

## [0.7.0] - 2026-10-27

### Added

- Inputs menu: a device connected over HDMI-CEC shows under its port with its name ("HDMI 1 · Fire TV")
  and its standby state, instead of as a second row.
- Google TV: after the Assistant shows an answer, the Home redirect waits eight seconds, so answers
  that open in the stock home stay on screen.

### Fixed

- "Force stop" on quietv removed the Home redirect for good on Fire TV and Google TV. quietv now adds it
  back the next time it starts or the TV restarts, unless you switched it off yourself in the
  accessibility settings.

## [0.6.0] - 2026-10-27

### Added

- French, Spanish, Italian, Dutch, Polish, Portuguese, Russian, Turkish and Arabic. They are a first
  draft, corrections from native speakers are welcome.

### Fixed

- Right-to-left languages: the tile menu opens on the correct side, the edges of the top bar stop the
  focus on the correct side, and moving a tile left or right follows the screen.

## [0.5.1] - 2026-10-27

### Fixed

- Fire TV: after an update of the launcher, Home kept showing Amazon's home. Fire OS lists the redirect
  service as bound after an app update but no longer sends it anything. quietv now switches its
  service off and on again right after each update, and `quietv update` opens quietv when it is done.
- After a reboot or an update, quietv checks that its redirect service is really connected, not only
  listed, and binds it again if not.

## [0.5.0] - 2026-10-27

### Added

- 12 device lists imported from cleanyourtv: Chromecast with Google TV, Google TV Streamer, onn 4K,
  TCL G08, G09 and G10, Sony BRAVIA VH21 and VU31, Philips TPM191E, Hisense FFM, Xiaomi Mi Box S and
  NVIDIA SHIELD TV Pro. They are marked as not yet checked with quietv.
- `quietv update` updates the launcher in place and keeps its settings.
- `quietv privacy` lists apps that can use the microphone, camera or location, active accessibility
  services and ads or telemetry that are still enabled.
- Release builds are reproducible. `scripts/compare-apks.py` checks a release against your own build.
- Store descriptions and screenshots for F-Droid and IzzyOnDroid in `fastlane/`.
- A guide for TVs quietv cannot debloat: Samsung, LG, Roku and Vizio.

## [0.4.0] - 2026-10-27

### Added

- Search your apps from the home screen (the magnifier at the top, or the remote's search key).
- Number and colour keys open apps on the home screen. Assign them on the TV (hold OK on a tile) or with
  `shortcuts` in the config.
- The Inputs menu remembers the last input and starts on it.
- `startup` opens an app or the last input once after a restart.
- A clock screensaver that drifts to avoid burn-in. `quietv screensaver on|off|status` sets it.

## [0.3.0] - 2026-10-27

### Added

- Google TV: `quietv install` keeps `launcherx` and redirects Home to quietv instead of disabling it.
- `quietv install --mode auto|replace|redirect`. Replace mode disables the stock home only after Android
  has accepted quietv as Home.
- After a reboot quietv checks that its Home redirect is running and binds it again if not.
- `quietv redirect on|off|status`. `quietv firetv` still works.
- `quietv debloat` never disables packages a TV needs: TV settings, Google Play services, the keyboard in
  use, the packages behind the HDMI and tuner inputs, the current home and launcher partner customisations.
- `quietv check` finds packages a firmware update enabled again, `--fix` disables them again.
- `quietv report` writes a device report for a GitHub issue.
- `quietv pair` pairs with Wireless debugging and opens port 5555 for TVs without a network debugging switch.

### Changed

- The config key `fireTv` is now `homeRedirect` (`enabled`, `stockHomePackage`, `classHint`,
  `showStockHomeTile`). Old configs keep working.

## [0.2.0] - 2026-10-27

### Added

- Four more themes: Midnight, Slate, Forest and Sand. Pick them on the TV or with `quietv theme apply`.
- Your own colours (`colors`), tile corners (`tileRadius`) and theme files for `quietv theme apply`.
- Wallpaper with `quietv theme wallpaper`. quietv dims it until all text is readable.
- Settings on the TV for tile corners and the wallpaper.

### Fixed

- Paper theme: the names under the favourites now meet the 4.5:1 contrast ratio.
- Light themes: the accent colours are a shade darker, so the focus ring and "Selected" stay readable.

## [0.1.2] - 2026-10-27

### Fixed

- Moving a tile between the favourites row and the app grid: the focus ring follows the tile, the
  "Selected" name above the row disappears, and names and spacing match the new row.
- Down on the last row no longer pushes a moving tile sideways.

## [0.1.1] - 2026-10-27

### Changed

- Android Gradle Plugin 9.4.1.
- org.json 20260814 for the unit tests.

## [0.1.0] - 2026-10-27

### Added

- Launcher rewritten in Kotlin, package `io.github.nowaaak.quietv`.
- Runtime configuration: one APK for every TV, set up on the TV or with `quietv config push`.
- Fire OS detected at runtime. The Home redirect no longer needs a separate build.
- Hold OK on a tile to move it, hide it or open its app info.
- Settings screen with Graphite and Paper themes, accent colours, tiles per row and hidden apps.
- Companion switch between a TV and a streaming stick, now protected by a shared token.
- `quietv` CLI for Windows, macOS and Linux, replacing the shell scripts.
- Device lists in TOML with categories, tiers and packages to keep.
- CI with lint and tests, signed releases with SHA-256 checksums.
