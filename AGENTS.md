# Instructions for AI coding agents

This file serves two purposes. Part 1 tells you how to set up a user's TV with quietv. Part 2 tells you
how to work on this repository. Read the part that matches the user's request.

## Part 1: Set up a TV

The user wants their Android TV, Google TV or Fire TV debloated and the quietv launcher installed. Work
on the real device, one step at a time, and verify each step. Ask before anything that depends on the
user's preferences. Never guess which apps they use.

### Connect

1. Install the CLI from this checkout: `uv tool install ./cli` (or `pipx install ./cli`).
2. Ask for the TV's IP address if you cannot find it. Set `QUIETV_HOST`.
3. Run `quietv probe`. Tell the user to accept "Allow USB debugging?" on the TV when it appears.
   If the connection is refused, ADB debugging is off or the TV is asleep. Explain how to turn it on
   (README, Quick start) and wait.

### Back up

Run `quietv backup` before changing anything and tell the user where the backup is.

### Debloat

1. If `devices/` has a file for this model, run `quietv debloat <file> --dry-run --tier advanced` and
   show the user the list, grouped by category.
2. Otherwise build a list. Run `quietv report` and `quietv shell pm list packages -e`, sort the packages into the categories
   from [devices/README.md](devices/README.md) and present them. Look up packages you do not recognise
   before suggesting them. When unsure, leave a package alone.
3. Ask with a checklist which of the `advanced` categories to apply: casting, voice assistant, vendor
   updates, preloaded streaming apps. Disable only what the user ticks.
4. Apply with `quietv debloat`. Use `--exclude` for categories the user wants to keep. For a new list,
   write the TOML file first and use it.
5. Never uninstall packages. Never disable anything the device list marks as `keep`.

### Launcher

1. Ask which apps go in the favourites row and in which order.
2. `quietv install`. On Android TV the CLI asks before disabling the stock launcher. On Fire TV it
   switches on the Home redirect instead.
3. Write the favourites into a config file (see [docs/configuration.md](docs/configuration.md)) and run
   `quietv config push <file>`.

### Verify

After each step run `quietv screenshot step.png` and look at the image. Inject remote keys with
`quietv shell input keyevent KEYCODE_DPAD_DOWN` (or `KEYCODE_HOME`, `KEYCODE_DPAD_CENTER`, `KEYCODE_BACK`).
Fix what you see before moving on. HDMI and Live TV come out black in screenshots. That is expected.

### Finish

Tell the user how to undo everything (`quietv restore`), how to re-enable one app
(`quietv shell cmd package install-existing <package>` then `pm enable <package>`) and how to change
favourites on the TV (hold OK on a tile). If you built a new device list, offer to open a pull request
with it.

## Part 2: Work on this repository

- `cli/`: Python 3.11+, standard library plus `adb-shell`. Run `uv run ruff check`, `uv run ruff format`
  and `uv run pytest` inside `cli/` before you finish.
- `launcher/`: Kotlin with Android Views and RecyclerView. No Compose, no new dependencies without an
  issue. Run `./gradlew lint testDebugUnitTest assembleDebug` inside `launcher/`.
- `devices/`: TOML device lists. The CLI tests validate every file.
- Keep the launcher light: no blur, no video, no polling, no background services beyond the opt-in ones.
- Everything a user might change belongs in the runtime config, not in code or resources.
- The config format is implemented twice: `launcher/.../config/LauncherConfig.kt` and `cli/src/quietv/config.py`.
  Change both together and update [docs/configuration.md](docs/configuration.md).
- Commit messages follow Conventional Commits: `feat: ...`, `fix: ...`, `docs: ...`.
