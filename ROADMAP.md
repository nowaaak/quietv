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
- [x] First signed release with checksums
- [x] Verified on Android TV (CHiQ AI PONT) and Fire TV (Fire TV Stick 4K Max) hardware
- [x] Screenshots from real TVs in the README
- [ ] Tested on a Google TV device

## 0.2: Themes

- [x] Theme files (colours, wallpaper image, tile shape) pushed with `quietv theme apply`
- [x] A small set of bundled themes
- [x] Optional wallpaper with automatic text contrast

## 0.3: Google TV and reliability

- Home redirect for TVs whose stock home cannot be replaced, Google TV first
- Redirect that survives reboots and firmware quirks
- Safer home handover and debloat guards for packages that break a TV
- `quietv check`: find packages a firmware update re-enabled and offer to disable them again
- `quietv report`: collect what a device report needs in one go
- `quietv pair`: wireless debugging pairing for TVs without a debugging switch for the network

## 0.4: Everyday comfort

- Screensaver with clock and date (DreamService)
- App search with the remote
- Remote key shortcuts: long press a colour or number key to open an app
- Inputs menu that remembers the last used HDMI port, optional input or app on start

## 0.5: Device lists

- Lists for the TVs people ask about most: TCL, Sony, Chromecast with Google TV, Google TV Streamer,
  onn, Xiaomi, Philips and Hisense

## 0.6: More languages

- Translations through a hosted translation platform
- Right-to-left layouts

## 0.7: Distribution

- F-Droid and IzzyOnDroid
- Reproducible builds
- `quietv update` to update the launcher in place

## Later

- Privacy report: which apps can use the microphone, which talk to known tracking hosts
- A guide for TVs quietv cannot debloat (Samsung, LG, Roku)
