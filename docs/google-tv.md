# Google TV

Google TV's home screen, `com.google.android.apps.tv.launcherx`, does more than show rows. It runs the
account and profile setup, parts of the Google Home panel and the handler that lets other apps open
an app by name. People who disabled it have reported reboot loops, a dead Home key and broken remote
apps (see cleanyourtv issues #62, #90, #1059 and #1653).

So on Google TV quietv does what it does on Fire TV: it leaves the stock home installed and switches
to quietv the moment the stock home comes to the front.

## Set up

```bash
quietv backup
quietv install
```

`quietv install` sees that the current home is `launcherx` and picks the redirect:

- it installs quietv and starts it,
- it writes `homeRedirect.stockHomePackage` into the launcher config,
- it grants `WRITE_SECURE_SETTINGS` and adds quietv's accessibility service to the enabled ones.

`launcherx` stays enabled and stays Android's home. Pressing Home shows it for a moment, then quietv.

To force one way or the other:

```bash
quietv install --mode redirect --stock-launcher com.google.android.apps.tv.launcherx
quietv install --mode replace
```

`--mode replace` sets quietv as the home activity first and checks that Android agrees. Only then does
it disable the stock launcher. If Android keeps the stock home, nothing is disabled.

## Check and switch off

```bash
quietv redirect status
quietv redirect off
quietv redirect on
```

On the TV, quietv's Settings shows the redirect under Home button.

## The Assistant

Google TV shows some Assistant answers inside the stock home. For eight seconds after the Assistant
was on screen, quietv does not redirect, so those answers stay visible. Press Home again to come back
to quietv.

## After a reboot

Some TVs come up with the accessibility service listed but not running. quietv checks this a few
seconds after boot and, if needed, switches its service off and on again. That needs
`WRITE_SECURE_SETTINGS`, which `quietv install` grants.

## Status

The redirect is the same code that runs on Fire TV every day. It has not been tested on a Google TV
device yet. If you have one, a report with `quietv probe` and what you saw helps a lot.
