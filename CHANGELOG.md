# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com)
and the project uses [Semantic Versioning](https://semver.org).

## [Unreleased]

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
