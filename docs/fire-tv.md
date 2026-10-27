# Fire TV

Fire OS can be debloated like any Android TV: `pm disable-user` works on most of Amazon's ad, shopping
and telemetry packages. Replacing the home screen is another matter:

- `cmd package set-home-activity` reports success and is ignored. Amazon's launcher has priority 950.
- `com.amazon.tv.launcher` is a protected package. Disabling or force-stopping it fails.
- Fire OS handles the Home key before any accessibility key filter sees it.

So quietv cannot *be* the home screen on Fire TV, but it can take over the moment Amazon's home appears.
An accessibility service watches for the stock home activity coming to the front and starts quietv
instead. Amazon's home is visible for a moment, typically 0.5 to 0.9 seconds on a Fire TV Stick 4K Max
(Fire OS 8).

## Set up

```bash
quietv debloat amazon-fire-tv-stick-4k-max-2
quietv install
```

`quietv install` detects Fire OS, grants the app `WRITE_SECURE_SETTINGS`, allows it to start from the
background (`SYSTEM_ALERT_WINDOW`) and adds the Home redirect to the enabled accessibility services. Other
accessibility services you use stay on.

Turn it off and on again with:

```bash
quietv firetv off
quietv firetv on
quietv firetv status
```

`off` removes only quietv's service. On the TV, Settings in quietv shows whether the redirect is on.

## What still works

- **Long press on Home** opens Fire OS's quick menu. quietv never touches the key itself.
- **TV settings.** The Settings screen in quietv has a TV settings entry. Fire OS routes the settings
  intent through Amazon's home, so quietv lets that one pass.
- **Amazon's home**, if you want it. Set `fireTv.showStockHomeTile` to `true` (see
  [configuration.md](configuration.md)) and a tile opens it once. The next Home press comes back to quietv.

## Caveats

- Amazon can change this behaviour with any Fire OS update. Other launchers that used the same approach
  have been broken by updates before. If Home stops opening quietv, run `quietv firetv status` and open an
  issue with your Fire OS version.
- Fire OS may stop the service under memory pressure. It comes back the next time the system binds
  accessibility services.
- Tested on Fire TV Stick 4K Max (2nd gen, Fire OS 8) and Toshiba Fire TV (AFTHA004, Fire OS 7).

## Undo

```bash
quietv firetv off
quietv restore
```
