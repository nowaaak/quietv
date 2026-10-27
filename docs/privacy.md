# Privacy report

```bash
quietv privacy
quietv privacy --list tcl-g09-google-tv --out privacy.md
```

The report shows, for every enabled app:

- who may use the **microphone**, the **camera** and the **location**, and whether the manufacturer
  granted it at the factory or you (or an app) did later,
- which **accessibility services** and **notification listeners** are on. Both can see a lot of what
  happens on the TV,
- which **ad and telemetry packages** from a device list are still enabled, with `--list`,
- package names that look like analytics or tracking. That part is only a guess from the name.

It does not show which servers an app talks to. That needs a look at the network traffic, which ADB
cannot provide.

quietv itself has location access on Android 10 and newer. Android only reveals the Wi-Fi network name
to apps with location access, and the launcher shows it in the top bar. quietv never reads your
position.
