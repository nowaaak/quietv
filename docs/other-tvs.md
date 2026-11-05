# TVs without Android

quietv needs ADB, so it cannot debloat Samsung (Tizen), LG (webOS), Roku or Vizio TVs. Most of their
tracking can still be switched off in the settings. Menu names move around between model years, so
look for the words below if a path does not match your TV exactly.

The most important switch on every brand is **automatic content recognition (ACR)**. It identifies
what is on screen, including HDMI devices, and reports it for advertising.

## Samsung (Tizen)

- Settings → Support → Terms & Privacy. On some 2022 to 2024 models Terms & Privacy sits directly under
  Settings or under General. 2021 models show Privacy centre and Security centre inside it.
- Turn off **Viewing Information Services** (ACR) and **Interest-Based Advertisement**. Many models only
  show these two. Turn off the others where they exist: **Voice Recognition Services** (missing on some
  2023 models), Bixby, and a separate PSID entry.
- 2020 models ask for consent as a list of "I agree" ticks. Leave the optional ones unticked and press
  Confirm.
- 2016 and older (not confirmed by enough reports): Smart Hub → Terms & Conditions, Privacy Policy, then
  the consent status for each service.
- Settings → General → Start Page / Smart Features: turn off **Autorun Smart Hub** and **Autorun Last App**
  if you want the TV to start on your input.
- A DNS server set on the TV can fall back to the default after a Wi-Fi reconnect. Filter at the router
  instead (see below).

## LG (webOS)

- 2021 and newer: Settings → All Settings → General → System → Additional Settings. Turn off **Live Plus**
  (ACR), **Home Promotion** and **Content Recommendation**. Where an **Advertisement** entry exists here,
  turn off its options.
- User Agreements: Settings → All Settings → Support → Privacy & Terms (on some models General → System).
  Turn off every optional agreement, they end in "Agreement": Viewing Information, Voice Information,
  Interest-Based Recommendation, **Interest-based & Cross-device advertising** and Marketing
  Communications. On models without an Advertisement entry, this agreement is the ad switch.
- 2020 budget models have no Live Plus. Turn off **Advertising Cookies** instead.
- 2019 and older have no System level: General → Additional Settings → Live Plus, Advertisement and
  Home Settings → Home Promotion. User Agreements are under General → About This TV. Also turn off
  AI Service → AI Recommendation.
- Live Promotion greys out once Live Plus is off. That is expected.
- LG TVs try other DNS servers when the configured one blocks them. A DNS filter only works if the
  router also blocks DNS traffic from the TV to other servers.

## Roku

- Settings → Privacy → Smart TV Experience: turn off **Use info from TV inputs** (ACR, Roku TVs only).
  Some Roku TVs have no Smart TV Experience entry, some no Privacy menu at all. Then use
  my.roku.com → Privacy Settings.
- Settings → Privacy → Advertising: turn off **Personalized ads** (older firmware: turn on
  **Limit ad tracking**).
- Settings → Privacy → Voice: turn off **Speech recognition** and set **App microphone access** to
  never allow (older firmware: Privacy → Microphone).

## Vizio (SmartCast)

- Settings → Admin & Privacy: turn off **Viewing Data** (ACR) and **Advertising**.
- Before 2020: Settings → System → Reset & Admin.

## Hisense (VIDAA)

- Settings → System → Advanced Settings → **Enhanced Viewing Settings**: turn off viewing data (ACR).
  The entry sits elsewhere on some VIDAA versions, search the settings for "Viewing".
- Decline the optional agreements under Settings → Support or System → Privacy where the TV offers them.

## Any brand

- Plug a streaming stick into HDMI and do not connect the TV itself to the internet. Without a network
  connection the TV cannot send viewing data or show ads.
- If the TV must stay online, a DNS filter in your network (Pi-hole, AdGuard Home or your router's
  filter) blocks much of the ad traffic. Filters can break single features: AdGuard DNS blocks
  `sp.pluto.tv`, after which Pluto TV forgets where you stopped watching. Allow such a host if you miss
  the feature.
