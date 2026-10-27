# Roadmap

quietv aims to be the one tool you need to make any TV calm: a clean debloat, a launcher you like
looking at, and nothing that phones home. Items move as people contribute. Open an issue to discuss
anything here.

## 0.1: Foundation

- [x] Kotlin launcher with runtime configuration, one APK for every TV
- [x] Fire TV Home redirect detected at runtime
- [x] Tile menu: move, hide, app info
- [x] Settings on the TV: theme, accent, tiles per row, hidden apps
- [x] Cross-platform CLI: probe, backup, debloat, restore, install, config, screenshot
- [x] Structured device lists with categories and tiers
- [x] CI with lint and tests
- [ ] First signed release with checksums
- [x] Verified on Android TV (CHiQ AI PONT) and Fire TV (Fire TV Stick 4K Max) hardware
- [x] Screenshots from real TVs in the README
- [ ] Tested on a Google TV device

## 0.2: Themes

- Theme files (colours, wallpaper image, tile shape) pushed with `quietv theme apply`
- A small set of bundled themes
- Optional wallpaper with automatic text contrast

## 0.3: Everyday comfort

- Screensaver with clock and date (DreamService)
- App search with the remote
- Remote key shortcuts: long press a colour or number key to open an app
- Inputs menu that remembers the last used HDMI port

## 0.4: More languages

- Translations through a hosted translation platform
- Right-to-left layouts

## 0.5: Distribution

- F-Droid and IzzyOnDroid
- Reproducible builds
- `quietv update` to update the launcher in place

## Later

- `quietv check`: find packages a firmware update re-enabled and offer to disable them again
- Privacy report: which apps can use the microphone, which talk to known tracking hosts
- More device lists, especially Google TV, Sony, TCL, Hisense and Philips
