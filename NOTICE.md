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

## Third-party material

| What | Where | Licence |
|---|---|---|
| Geist typeface, by Vercel and basement.studio | `launcher/app/src/main/res/font/geist.ttf` | SIL Open Font License 1.1, see `launcher/FONT-LICENSE-OFL.txt` |
| Material icons (Wi-Fi, settings, input, AV, live TV), by Google | `launcher/app/src/main/res/drawable/ic_*.xml` | Apache License 2.0 |
| adb-shell, by Jeff Irion | dependency of `cli/` | Apache License 2.0 |
| AndroidX RecyclerView and DynamicAnimation, by Google | dependencies of the launcher | Apache License 2.0 |
