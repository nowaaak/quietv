# TVs without Android

quietv needs ADB, so it cannot debloat Samsung (Tizen), LG (webOS), Roku or Vizio TVs. Most of their
tracking can still be switched off in the settings. Menu names move around between model years, so
look for the words below if a path does not match your TV exactly.

The most important switch on every brand is **automatic content recognition (ACR)**. It identifies
what is on screen, including HDMI devices, and reports it for advertising.

## Samsung (Tizen)

- Settings → Support → Terms & Privacy (older models: Settings → General → Terms & Privacy).
- Turn off **Viewing Information Services** (ACR) and **Interest-Based Advertisement**.
- Turn off **Voice Recognition Services** if you do not use voice control.
- Under Privacy Choices, decline the optional agreements you do not want.
- Settings → General → Start Page / Smart Features: turn off **Autorun Smart Hub** and **Autorun Last App**
  if you want the TV to start on your input.

## LG (webOS)

- Settings → All Settings → General → System → Additional Settings (older models: General → About This TV
  → User Agreements).
- Turn off **Live Plus** (ACR) and, under User Agreements, **Viewing Information**,
  **Voice Information** and **Interest-Based Recommendation**.
- Turn off **Home Promotion** and **Content Recommendation** in the Home settings.
- Settings → General → Location: set **Limit Ad Tracking** on.

## Roku

- Settings → Privacy → Smart TV Experience: turn off **Use info from TV inputs** (ACR, Roku TVs only).
- Settings → Privacy → Advertising: turn on **Limit ad tracking**.
- Settings → Privacy → Microphone: set channel microphone access to **Never allow**.

## Vizio (SmartCast)

- Settings → Admin & Privacy: turn off **Viewing Data** (ACR) and **Advertising**.

## Any brand

- Plug a streaming stick into HDMI and do not connect the TV itself to the internet. Without a network
  connection the TV cannot send viewing data or show ads.
- If the TV must stay online, a DNS filter in your network (Pi-hole, AdGuard Home or your router's
  filter) blocks much of the ad traffic.
