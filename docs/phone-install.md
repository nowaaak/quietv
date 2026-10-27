# Install from an Android phone

No computer at hand? An Android phone on the same network can install quietv with
[Termux](https://termux.dev).

```bash
pkg install -y android-tools curl
curl -fsSLO https://raw.githubusercontent.com/nowaaak/quietv/main/scripts/termux-install.sh
bash termux-install.sh 192.168.1.50
```

The script downloads the latest release, checks its SHA-256 checksum, installs it over ADB and does the
same setup as `quietv install`: it makes quietv the home screen on Android TV, or switches on the Home
redirect on Fire TV. Accept "Allow USB debugging?" on the TV when it appears.

On Android TV the script does not disable the stock launcher. Do that by hand once you are happy:
`adb -s 192.168.1.50:5555 shell pm disable-user --user 0 com.google.android.tvlauncher`
(the package differs by brand, `adb shell cmd package resolve-activity -c android.intent.category.HOME -a android.intent.action.MAIN` shows it).

Debloating needs the CLI, which does not run well on Termux. Use a computer for that.
