# Device lists

One TOML file per TV model. `quietv debloat <file>` reads it, or `quietv debloat <name>` fetches it from
this folder on GitHub. The CLI tests validate every file in here.

## Format

```toml
schema = 1

[device]
name = "CHiQ AI PONT"
brand = "CHiQ"
model = "DVB93CQ (samseong)"
platform = "androidtv"
os = "Android TV 11"
firmware = "RTO2.250508.001"
stock_launcher = "com.google.android.tvlauncher"
notes = """
Anything a user of this TV should know.
"""

[[package]]
id = "tv.anoki.acr.controller"
category = "ads"
tier = "recommended"
note = "automatic content recognition (ACR)"

[[keep]]
id = "com.google.android.leanbacklauncher.partnercustomizer"
reason = "the Input key stops working without it"
```

| Field | Required | Meaning |
|---|---|---|
| `device.name` | yes | Marketing name |
| `device.brand` | yes | Manufacturer |
| `device.model` | yes | Model number and codename, as `quietv probe` prints them |
| `device.platform` | yes | `androidtv`, `googletv` or `fireos` |
| `device.os` | yes | OS and version as the user would recognise it |
| `device.firmware` | no | `ro.build.display.id` from `quietv probe` |
| `device.stock_launcher` | no | Package of the stock home screen. Do not list it as a package, `quietv install` handles it. |
| `device.notes` | no | Quirks, things that broke, things to know |
| `package.note` | no | What the package does, or what stops working without it |
| `keep` | no | Packages that look like bloat but must stay, with the reason |

## Categories

| Category | What goes in |
|---|---|
| `ads` | Advertising, ad IDs, automatic content recognition, shopping |
| `telemetry` | Analytics, metrics, logging, crash and usage reporting |
| `content` | Recommendation rows, promoted content, screensavers with promotions |
| `vendor` | Manufacturer app stores, help apps, cleaners, auto-installers |
| `preloaded` | Apps installed at the factory that most people do not use |
| `assistant` | Voice assistants and their bridges |
| `cast` | Chromecast, Miracast, AirPlay, vendor screen sharing |
| `streaming` | Preinstalled streaming apps |
| `updates` | App or firmware updaters |
| `launcher` | Launcher add-ons other than the stock launcher itself |

## Tiers

| Tier | Use it for |
|---|---|
| `recommended` | Safe to disable for everyone. Nothing a user would miss. |
| `advanced` | A feature some people use. Say which one in `note`. |
| `expert` | Might break something. Explain what in `note`. |

When in doubt, choose the higher tier or leave the package out.
