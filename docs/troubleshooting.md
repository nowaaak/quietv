# Troubleshooting

## Connecting

**"Cannot connect" although the TV is on.** Check that ADB or network debugging is on in Developer
options, that the TV and your computer are on the same network and that the TV is awake. Some TVs only
open port 5555 after a reboot with USB debugging on. quietv names the likely cause:

- *refused*: ADB is not listening. Switch debugging off and on again, or restart the TV.
- *no answer*: wrong address, or the network keeps devices apart. Guest networks and access point
  isolation do that. A TV on Ethernet and a computer on Wi-Fi of the same router are fine.
- *Windows blocked the connection* (WinError 10013): a firewall, VPN or antivirus on the computer.

**No "Allow USB debugging?" prompt.** The TV may have remembered an older decision. Revoke USB debugging
authorisations in Developer options and connect again.

**The prompt appears every time.** quietv keeps its ADB key in your user config folder
(`%APPDATA%\quietv` on Windows, `~/Library/Application Support/quietv` on macOS, `~/.config/quietv` on
Linux). Tick "Always allow from this computer" on the TV.

**macOS says "No route to host".** macOS Local Network privacy blocks the connection. The permission
belongs to the terminal app, so quietv and `adb` are both affected. Allow your terminal under System
Settings → Privacy & Security → Local Network, then quit and reopen it. If `quietv pair` still fails,
run `adb kill-server` once: the background `adb` server can keep the old state. A sign of this problem:
the router answers a ping, every other device on the network does not.

**There is no "Wireless debugging" switch.** Not every TV has one. USB debugging alone usually opens
network ADB on port 5555.

**The TV only has "Wireless debugging" with a pairing code.** Pair once with
`quietv pair <pairing port> <code> <port>`. The two ports and the code are on the TV's Wireless
debugging screen. It uses Android's `adb` tool to pair and then opens port 5555 for quietv until the
TV restarts.

**Windows on ARM: the install fails while building `cryptography`.** adb-shell needs it, and there are
no prebuilt packages of current versions for Windows on ARM. Install with x64 Python, which runs under
emulation: `uv tool install --python cpython-3.12-windows-x86_64-none "git+https://github.com/nowaaak/quietv#subdirectory=cli"`.

**When you are done.** ADB on the network lets anyone in it control the TV once the TV trusts their key.
`quietv probe` warns if the TV trusts every computer without asking (`ro.adb.secure=0`). Switch USB or
network debugging off in Developer options when you no longer need it. On TVs with Wireless debugging,
switch that off separately. quietv itself needs ADB only while you run a command.

**Windows: "Filename too long" while installing the CLI.** uv checks out the whole repository, and
Windows limits paths to 260 characters by default. Run `git config --global core.longpaths true` once
and install again.

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
are gone: core Android packages and system providers, TV settings and whatever app opens them, Google
Play services, the keyboard in use, the packages behind the HDMI and tuner inputs or that use the input
hardware, the app that opens an input, the current home screen and launcher partner customisations.
Checking the input hardware runs `dumpsys` once per package and can take a minute on large lists. Disable one by hand with
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
