# Contributing

Thanks for helping more TVs get quiet. Issues and pull requests are welcome. For anything bigger than
a fix, open an issue first so we can agree on the approach before you spend time on it.

## Add your TV

The most useful contribution. Every brand ships different bloat.

1. Run `quietv probe` and copy the output.
2. Create `devices/<brand>-<model>.toml`, lowercase with dashes. Copy the structure from an existing file
   and read [devices/README.md](devices/README.md) for the categories and tiers.
3. Only list packages you have disabled yourself and lived with for a few days. Put what broke and what
   you re-enabled in `notes` or as a `[[keep]]` entry.
4. Run the tests (see below). They validate your file.
5. Add a row to the device table in the [README](README.md#device-lists).

Never list packages to uninstall. quietv only disables, so everything can be undone from the TV's own
settings even without the tools.

## Improve the launcher

The launcher in `launcher/` is Kotlin with Android Views and RecyclerView. It targets TVs with 1 GB of RAM:

- No Compose, no blur, no video backgrounds, no polling, no background services beyond the opt-in ones.
- No new dependencies without an issue first.
- Anything a user might want to change belongs in the runtime config, not in code or resources. The
  format lives in `LauncherConfig.kt` and in `cli/src/quietv/config.py`. Change both and update
  [docs/configuration.md](docs/configuration.md).
- D-pad navigation must stay deterministic. Test Up, Down, Left and Right from every edge, and with
  3 to 8 tiles per row.
- Test on a real TV, not only the emulator. `quietv screenshot` and `quietv shell input keyevent ...`
  make that quick. Add a screenshot to your pull request.

```bash
cd launcher
./gradlew lint testDebugUnitTest assembleDebug
quietv install --apk app/build/outputs/apk/debug/app-debug.apk
```

You need JDK 17 and the Android SDK with platform 36 and build tools 36.

Ideas that fit: themes, a screensaver, app search, translations, remote key shortcuts.
Ideas that do not fit: recommendation rows, content feeds, ads, telemetry.

## Improve the CLI

The CLI in `cli/` is Python 3.11+ with `adb-shell` as its only dependency.

```bash
cd cli
uv sync
uv run ruff check && uv run ruff format --check && uv run pytest
```

Every command that changes the TV must ask first (or take `--yes`) and must be reversible.

## Translations

Strings live in `launcher/app/src/main/res/values/strings.xml`. Add a `values-<language>/strings.xml`
with the same names. Keep them short: they have to fit on a TV tile.

## Pull requests

- One topic per pull request. Small is fine.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org): `feat: ...`,
  `fix: ...`, `docs: ...`, `devices: ...`.
- Say which TV you tested on.
- Be kind in reviews.

By contributing you agree that your work is released under the [MIT licence](LICENSE).
