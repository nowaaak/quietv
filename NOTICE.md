# Notice

## Origin

quietv started in October 2026 as a fork of [cleanyourtv](https://github.com/Gathin23/cleanyourtv)
(then called tv-vibehack) by Gathin, released under the MIT licence. It was rewritten and renamed
since. The original copyright notice is kept in [LICENSE](LICENSE).

The following came from that project and its contributors:

- The idea and the first version of the agent prompt, now [AGENTS.md](AGENTS.md).
- The pure-Python ADB approach and the base64 upload, now in `cli/`.
- The Xiaomi Mi TV 4A device list (Gathin).
- The Fire TV Home redirect through an accessibility service and the Toshiba Fire TV device list
  (Umang Chauhan).
- The launcher's first layout, since ported to Kotlin and redesigned.

## Device lists from cleanyourtv

These lists were converted from cleanyourtv's device reports (MIT licence, copyright Gathin and the
contributors of that project). quietv sorted the packages into its categories and tiers, left out
packages whose function or safety was unclear and rewrote the notes.

| quietv list | cleanyourtv source |
|---|---|
| [google-chromecast-google-tv-4k.toml](devices/google-chromecast-google-tv-4k.toml) | `examples/google-chromecast-gtv-4k` |
| [google-tv-streamer-4k.toml](devices/google-tv-streamer-4k.toml) | `examples/google-tv-streamer-4k-kirkwood` |
| [hisense-smarttv-4k-ffm-android11.toml](devices/hisense-smarttv-4k-ffm-android11.toml) | `examples/hisense-smarttv-4k-ffm-android11` |
| [nvidia-shield-tv-pro-2019.toml](devices/nvidia-shield-tv-pro-2019.toml) | `examples/nvidia-shield-tv-pro-2019` |
| [onn-4k-streaming-box-2023.toml](devices/onn-4k-streaming-box-2023.toml) | `examples/onn-4k-streaming-box-2023` |
| [philips-tpm191e.toml](devices/philips-tpm191e.toml) | `examples/philips-tpm191e` |
| [sony-bravia-vh21-eu.toml](devices/sony-bravia-vh21-eu.toml) | `examples/sony-bravia-4k-vh21-eu` |
| [sony-bravia-vu31.toml](devices/sony-bravia-vu31.toml) | `examples/sony-bravia-vu31` |
| [tcl-g08-eu-android12.toml](devices/tcl-g08-eu-android12.toml) | `examples/tcl-g08-eu-android12` |
| [tcl-g09-google-tv.toml](devices/tcl-g09-google-tv.toml) | `examples/tcl-g09-google-tv` |
| [tcl-g10-google-tv.toml](devices/tcl-g10-google-tv.toml) | `examples/tcl-g10-google-tv` |
| [xiaomi-mi-box-s-2nd-gen.toml](devices/xiaomi-mi-box-s-2nd-gen.toml) | `examples/xiaomi-mi-box-s-2nd-gen` |

## Third-party material

| What | Where | Licence |
|---|---|---|
| Geist typeface, by Vercel and basement.studio | `launcher/app/src/main/res/font/geist.ttf` | SIL Open Font License 1.1, see `launcher/FONT-LICENSE-OFL.txt` |
| Material icons (Wi-Fi, settings, input, AV, live TV, search, dialpad), by Google | `launcher/app/src/main/res/drawable/ic_*.xml` | Apache License 2.0 |
| adb-shell, by Jeff Irion | dependency of `cli/` | Apache License 2.0 |
| AndroidX RecyclerView and DynamicAnimation, by Google | dependencies of the launcher | Apache License 2.0 |
