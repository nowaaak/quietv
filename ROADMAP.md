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

- [x] Home redirect for TVs whose stock home cannot be replaced, Google TV first
- [x] Redirect that survives reboots and firmware quirks
- [x] Safer home handover and debloat guards for packages that break a TV
- [x] `quietv check`: find packages a firmware update re-enabled and offer to disable them again
- [x] `quietv report`: collect what a device report needs in one go
- [x] `quietv pair`: wireless debugging pairing for TVs without a debugging switch for the network
- [ ] Google TV redirect confirmed on real hardware

## 0.4: Everyday comfort

- [x] Screensaver with clock and date (DreamService)
- [x] App search with the remote
- [x] Remote key shortcuts: a colour or number key opens an app
- [x] Inputs menu that remembers the last used HDMI port, optional input or app on start

## 0.5: Device lists

- [x] Lists for the TVs people ask about most: TCL, Sony, Chromecast with Google TV, Google TV Streamer,
  onn, Xiaomi, Philips and Hisense
- [ ] Each imported list checked with quietv on real hardware

## 0.6: More languages

- [x] Nine more languages (first drafts)
- [ ] Translations through a hosted translation platform (needs the public repository)
- [x] Right-to-left layouts in code
- [ ] Right-to-left layouts checked on a TV set to Arabic or Hebrew

## 0.7: Distribution

- [x] Store metadata for F-Droid and IzzyOnDroid
- [ ] Submit to F-Droid and IzzyOnDroid (needs the public repository)
- [x] Reproducible builds
- [x] `quietv update` to update the launcher in place

## Later

- [x] Privacy report: which apps can use the microphone, camera and location, which known ad and
  telemetry packages still run
- [ ] Privacy report: which apps talk to known tracking hosts (needs network monitoring, not possible
  over ADB alone)
- [x] A guide for TVs quietv cannot debloat (Samsung, LG, Roku, Vizio)
