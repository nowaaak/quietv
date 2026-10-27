#!/usr/bin/env bash
set -euo pipefail

TV="${1:?usage: termux-install.sh <tv-ip>}"
BASE="https://github.com/nowaaak/quietv/releases/latest/download"
PKG="io.github.nowaaak.quietv"
SERVICE="$PKG/$PKG.firetv.HomeRedirectService"
DEV="$TV:5555"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

curl -fsSL -o "$WORK/quietv.apk" "$BASE/quietv.apk"
curl -fsSL -o "$WORK/SHA256SUMS" "$BASE/SHA256SUMS"
(cd "$WORK" && grep ' quietv.apk$' SHA256SUMS | sha256sum -c -)

adb connect "$DEV"
adb -s "$DEV" wait-for-device
adb -s "$DEV" install -r -g "$WORK/quietv.apk"

tv() { adb -s "$DEV" shell "$@" | tr -d '\r'; }

tv appops set "$PKG" SYSTEM_ALERT_WINDOW allow
if [ "$(tv getprop ro.product.manufacturer)" = "Amazon" ]; then
    tv pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS
    CURRENT="$(tv settings get secure enabled_accessibility_services)"
    case "$CURRENT" in
        null | "") SERVICES="$SERVICE" ;;
        *"$SERVICE"*) SERVICES="$CURRENT" ;;
        *) SERVICES="$CURRENT:$SERVICE" ;;
    esac
    tv settings delete secure enabled_accessibility_services
    tv settings put secure enabled_accessibility_services "$SERVICES"
    tv settings put secure accessibility_enabled 1
    echo "Fire TV: Home redirect on."
else
    tv cmd package set-home-activity --user 0 "$PKG/.HomeEntry"
fi
tv am start -n "$PKG/.MainActivity" >/dev/null
echo "Installed. Press Home on the remote."
