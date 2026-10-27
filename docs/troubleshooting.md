# Troubleshooting

## Connecting

**"Cannot connect" although the TV is on.** Check that ADB or network debugging is on in Developer
options, that the TV and your computer are on the same network and that the TV is awake. Some TVs only
open port 5555 after a reboot with USB debugging on.

**No "Allow USB debugging?" prompt.** The TV may have remembered an older decision. Revoke USB debugging
authorisations in Developer options and connect again.

**The prompt appears every time.** quietv keeps its ADB key in your user config folder
(`%APPDATA%\quietv` on Windows, `~/Library/Application Support/quietv` on macOS, `~/.config/quietv` on
Linux). Tick "Always allow from this computer" on the TV.

**macOS says "No route to host" with the `adb` binary.** macOS Local Network privacy blocks it. The
quietv CLI does not use the binary and is not affected.

**There is no "Wireless debugging" switch.** Not every TV has one. USB debugging alone usually opens
network ADB on port 5555.

**The TV only has "Wireless debugging" with a pairing code.** Pair once with
`quietv pair <pairing port> <code> <port>`. The two ports and the code are on the TV's Wireless
debugging screen. It uses Android's `adb` tool to pair and then opens port 5555 for quietv until the
TV restarts.

## After debloating

**No on-screen keyboard.** Seen on Xiaomi: the default input method points at an IME that is not
installed. Fix it with
`quietv shell ime set com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME`.

**Something stopped working.** `quietv restore` re-enables everything that was enabled in the newest
backup. To bring back a single package:
`quietv shell cmd package install-existing <package>` and then `quietv shell pm enable <package>`.

**The Input key or HDMI-CEC broke.** Some vendors hide system services in packages that look like bloat.
Restore, then disable in smaller steps. Please report it so the device list can mark the package as `keep`.

**"keep ...: protected" during a debloat.** quietv refuses to disable packages that break TVs when they
are gone: TV settings, Google Play services, the keyboard in use, the packages behind the HDMI and tuner
inputs, the current home screen and launcher partner customisations. Disable one by hand with
`quietv shell pm disable-user --user 0 <package>` if you are sure.

**Apps came back after a firmware update.** `quietv check` lists packages quietv disabled that are enabled
again. `quietv check --fix` disables them again after a backup.

## The launcher

**Home still opens the stock launcher (Android TV).** The stock launcher has a higher priority than any
third-party home. `quietv install` disables it. If you kept it, disable it with
`quietv shell pm disable-user --user 0 <stock launcher package>`.

**The Wi-Fi button shows "Wi-Fi" instead of the network name.** Android 10 and newer only reveal the
network name with location access. `quietv install` grants it. On some TVs, Location must also be on in
the TV settings.

**The quietv clock does not appear as screensaver on Fire TV.** Fire OS runs Amazon's own screensaver and
ignores other ones. `quietv screensaver on` works on Android TV and Google TV.

**A number key does nothing.** Shortcuts work on quietv's home screen. Inside other apps the keys belong to
the app. Check the assignments in quietv's Settings under Remote.

**HDMI and Live TV screenshots are black.** The video layer is protected. That is expected.

**An app is missing from the grid.** Apps without a TV launcher entry do not show up by default. Add
the package to `extraApps` (see [configuration.md](configuration.md)). Also check Settings for hidden apps.
