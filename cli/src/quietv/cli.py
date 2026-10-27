import argparse
import json
import os
import sys
import tempfile
from pathlib import Path

from quietv import (
    PACKAGE,
    __version__,
    backup,
    config,
    devicelist,
    guard,
    install,
    pair,
    release,
    report,
    state,
    themes,
    wallpaper,
)
from quietv.devicelist import CATEGORIES, TIERS, DeviceListError
from quietv.packages import parse_package_list
from quietv.paths import backups_dir
from quietv.release import ReleaseError
from quietv.tv import DEFAULT_PORT, Tv, TvError, getprop, is_fire_os

PROBE_PROPS = (
    "ro.product.manufacturer",
    "ro.product.model",
    "ro.product.device",
    "ro.build.version.release",
    "ro.build.version.sdk",
    "ro.build.display.id",
)


class Abort(Exception):
    pass


def confirm(question: str, assume_yes: bool) -> None:
    if assume_yes:
        return
    if not sys.stdin.isatty():
        raise Abort(f"{question} Pass --yes to confirm when not running interactively.")
    answer = input(f"{question} [y/N] ").strip().lower()
    if answer not in ("y", "yes"):
        raise Abort("cancelled")


def format_note(note: str) -> str:
    return f"  ({note})" if note else ""


def progress(done: int, total: int) -> None:
    if sys.stdout.isatty():
        print(f"\r  upload {done}/{total}", end="" if done < total else "\n", flush=True)
    elif done == total:
        print(f"  uploaded {total} chunks")


def connect(args) -> Tv:
    host = args.host or os.environ.get("QUIETV_HOST")
    if not host:
        raise Abort("set the TV's IP address with --host or the QUIETV_HOST environment variable")
    print(f"Connecting to {host}:{args.port} (accept the debugging prompt on the TV if it appears)")
    tv = Tv(host, args.port)
    tv.connect()
    return tv


def cmd_probe(args) -> None:
    with connect(args) as tv:
        for prop in PROBE_PROPS:
            print(f"{prop} = {getprop(tv, prop)}")
        print(f"fire_os = {is_fire_os(tv)}")
        print(f"home = {install.current_home_package(tv) or PACKAGE}")


def cmd_shell(args) -> None:
    with connect(args) as tv:
        sys.stdout.write(tv.shell(" ".join(args.command_line)))


def cmd_screenshot(args) -> None:
    remote = "/sdcard/quietv-screenshot.png"
    with connect(args) as tv:
        tv.shell(f"screencap -p {remote}")
        tv.pull(remote, Path(args.output))
        tv.shell(f"rm -f {remote}")
    print(f"Saved {args.output}")


def cmd_backup(args) -> None:
    with connect(args) as tv:
        folder = backup.create(tv, Path(args.out) if args.out else backups_dir())
    print(f"Backup written to {folder}")


def cmd_debloat(args) -> None:
    device_list = devicelist.load(args.list)
    excluded = set(args.exclude or [])
    selected = [p for p in device_list.select(args.tier) if p.category not in excluded]
    print(f"{device_list.name} ({device_list.os}), tier '{args.tier}': {len(selected)} packages")
    for keep in device_list.keep:
        print(f"  keep {keep.id}: {keep.reason}")

    if args.dry_run and not (args.host or os.environ.get("QUIETV_HOST")):
        for p in selected:
            print(f"  disable {p.id} [{p.category}, {p.tier}]{format_note(p.note)}")
        print("Dry run without a TV, nothing changed. Pass --host to check what is installed.")
        return

    with connect(args) as tv:
        enabled = parse_package_list(tv.shell("pm list packages -e"))
        installed = [p for p in selected if p.id in enabled]
        skipped = len(selected) - len(installed)
        protected = guard.dynamic(tv)
        todo = []
        for p in installed:
            why = guard.reason(p.id, protected)
            if why:
                print(f"  keep {p.id}: protected, {why}")
            else:
                todo.append(p)
        for p in todo:
            print(f"  disable {p.id} [{p.category}, {p.tier}]{format_note(p.note)}")
        if skipped:
            print(f"  {skipped} listed packages are not installed or already disabled")
        if not todo:
            print("Nothing to do.")
            return
        if args.dry_run:
            print("Dry run, nothing changed.")
            return
        confirm(f"Disable {len(todo)} packages?", args.yes)
        if not args.no_backup:
            print(f"Backup written to {backup.create(tv, backups_dir())}")
        failed = []
        for p in todo:
            out = install.disable_package(tv, p.id)
            if "disabled" not in out:
                failed.append((p.id, out))
        failed_ids = {package for package, _ in failed}
        state.record(tv, added=[p.id for p in todo if p.id not in failed_ids])
        print(f"Disabled {len(todo) - len(failed)} of {len(todo)} packages.")
        for package, out in failed:
            print(f"  failed {package}: {out}")
        print("Undo with: quietv restore --backup <folder>   or   quietv restore --list " + args.list)


def cmd_restore(args) -> None:
    with connect(args) as tv:
        if args.list:
            device_list = devicelist.load(args.list)
            packages = {p.id for p in device_list.packages}
            if device_list.stock_launcher:
                packages.add(device_list.stock_launcher)
            commands = backup.enable_commands(packages)
            if device_list.stock_launcher and device_list.platform != "fireos":
                commands.append(f"cmd package set-home-activity --user 0 {device_list.stock_launcher}")
        else:
            folder = Path(args.backup) if args.backup else backup.latest(backups_dir())
            if not folder or not (folder / "packages_enabled.txt").is_file():
                raise Abort("no backup found, pass --backup <folder>")
            print(f"Restoring from {folder}")
            commands = backup.restore_plan(folder, parse_package_list(tv.shell("pm list packages -e")))
        if not commands:
            print("Nothing to restore.")
            return
        for command in commands:
            print(f"  {command}")
        confirm(f"Run {len(commands)} commands?", args.yes)
        for command in commands:
            tv.shell(command)
        state.record(tv, removed=parse_package_list(tv.shell("pm list packages -e")))
        print("Restored. Press Home on the remote.")


def cmd_check(args) -> None:
    with connect(args) as tv:
        recorded = state.disabled(tv)
        if not recorded:
            print("No record of packages quietv disabled on this TV. Run quietv debloat first.")
            return
        enabled = parse_package_list(tv.shell("pm list packages -e"))
        back = sorted(recorded & enabled)
        gone = recorded - parse_package_list(tv.shell("pm list packages -u"))
        if gone:
            state.record(tv, removed=gone)
        if not back:
            print(f"All {len(recorded - gone)} packages quietv disabled are still disabled.")
            return
        print(f"{len(back)} packages quietv disabled are enabled again, probably by a firmware update:")
        for package in back:
            print(f"  {package}")
        if not args.fix:
            print("Run quietv check --fix to disable them again.")
            return
        confirm(f"Disable {len(back)} packages again?", args.yes)
        if not args.no_backup:
            print(f"Backup written to {backup.create(tv, backups_dir())}")
        protected = guard.dynamic(tv)
        for package in back:
            why = guard.reason(package, protected)
            if why:
                print(f"  keep {package}: protected, {why}")
                state.record(tv, removed=[package])
                continue
            print(f"  {package}: {install.disable_package(tv, package)}")


def cmd_report(args) -> None:
    with connect(args) as tv:
        model = getprop(tv, "ro.product.model") or "tv"
        text = report.build(tv)
    out = Path(args.out or f"quietv-report-{backup.SAFE_NAME.sub('-', model).strip('-')}.md")
    out.write_text(text, encoding="utf-8")
    print(f"Report written to {out}")
    print("It lists the installed packages. Attach it to a device report on GitHub if you are happy to share that.")


def cmd_pair(args) -> None:
    host = args.host or os.environ.get("QUIETV_HOST")
    if not host:
        raise Abort("set the TV's IP address with --host or the QUIETV_HOST environment variable")
    adb = pair.find_adb()
    if not adb:
        raise Abort(
            "pairing needs Android's adb tool. Install the platform tools from "
            "https://developer.android.com/tools/releases/platform-tools and try again."
        )
    print(f"Pairing with {host}:{args.pair_port} using {adb}")
    pair.pair(adb, host, args.pair_port, args.code, args.connect_port)
    print(f"Done. The TV now accepts ADB on port {pair.LEGACY_PORT} until it restarts.")
    print("Run quietv probe and accept the prompt on the TV.")


def cmd_install(args) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        if args.apk:
            apk = Path(args.apk)
            if not apk.is_file():
                raise Abort(f"no such file: {apk}")
        else:
            print("Downloading the latest release from GitHub")
            apk, tag = release.download_latest(Path(tmp))
            print(f"  {apk.name} ({tag}), checksum verified")

        with connect(args) as tv:
            fire = is_fire_os(tv)
            stock = args.stock_launcher or install.current_home_package(tv)
            mode = args.mode
            if mode == "auto":
                mode = "redirect" if fire or stock in install.REDIRECT_HOMES else "replace"
            if mode == "redirect" and not fire and not stock:
                raise Abort("the redirect needs the stock home package, pass --stock-launcher")
            print(f"Installing {apk.name}")
            install.install_apk(tv, apk, progress)
            install.start_launcher(tv)
            if mode == "redirect":
                if fire:
                    print("Fire OS: switching on the Home redirect (accessibility service)")
                else:
                    print(f"Redirecting {stock} to quietv (accessibility service). {stock} stays enabled.")
                    config.push(tv, install.with_redirect(config.pull(tv), stock))
                install.redirect_on(tv)
            else:
                install.make_default_home(tv)
                if not install.home_is_quietv(tv):
                    raise Abort(
                        "Android did not accept quietv as Home, so nothing was disabled. "
                        "Try: quietv install --mode redirect"
                    )
                if stock and not args.keep_stock_launcher:
                    confirm(
                        f"Disable the stock launcher {stock}? Android TV keeps it as Home otherwise. "
                        "quietv restore brings it back.",
                        args.yes,
                    )
                    print(f"  {install.disable_package(tv, stock)}")
            install.start_launcher(tv)
    print("Installed. Press Home on the remote.")


def cmd_config(args) -> None:
    if args.action == "check":
        problems = config.validate(json.loads(Path(args.file).read_text(encoding="utf-8")))
        if problems:
            raise Abort("invalid config:\n  " + "\n  ".join(problems))
        print("Config is valid.")
        return
    with connect(args) as tv:
        if args.action == "push":
            config.push(tv, json.loads(Path(args.file).read_text(encoding="utf-8")))
            install.start_launcher(tv)
            print("Config applied.")
        else:
            text = json.dumps(config.pull(tv), indent=2) + "\n"
            if args.file:
                Path(args.file).write_text(text, encoding="utf-8")
                print(f"Saved {args.file}")
            else:
                sys.stdout.write(text)


def cmd_theme(args) -> None:
    if args.action == "list":
        for name, preset in themes.PRESETS.items():
            print(f"{name:10} {'light' if preset.light else 'dark'}")
        return
    if args.action == "apply":
        theme = themes.load(args.source)
        image = wallpaper.read_image(theme.image) if theme.image else None
        with connect(args) as tv:
            updated = themes.merge(config.pull(tv), theme)
            for warning in themes.contrast_warnings(updated):
                print(f"  warning: {warning}")
            if image is not None:
                print("Uploading the wallpaper")
                wallpaper.upload(tv, image, progress)
            config.push(tv, updated)
            install.start_launcher(tv)
        print("Theme applied.")
        return
    image = None if args.off else wallpaper.read_image(Path(args.source))
    with connect(args) as tv:
        current = config.pull(tv)
        settings = current.get("wallpaper", {})
        if image is None:
            wallpaper.clear(tv)
            current["wallpaper"] = {**settings, "enabled": False}
        else:
            print("Uploading the wallpaper")
            wallpaper.upload(tv, image, progress)
            current["wallpaper"] = {"enabled": True, "dim": settings.get("dim", 30) if args.dim is None else args.dim}
        config.push(tv, current)
        install.start_launcher(tv)
    print("Wallpaper removed." if image is None else "Wallpaper set.")


def cmd_redirect(args) -> None:
    with connect(args) as tv:
        if args.action == "on":
            install.redirect_on(tv)
            print("Home redirect on.")
        elif args.action == "off":
            install.redirect_off(tv)
            print("Home redirect off. Home opens the stock home screen again.")
        else:
            print("Home redirect is " + ("on" if install.redirect_enabled(tv) else "off"))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="quietv",
        description="Debloat Android TV, Google TV and Fire TV over ADB and install the quietv launcher.",
    )
    parser.add_argument("--version", action="version", version=f"quietv {__version__}")
    parser.add_argument("--host", help="TV IP address (or set QUIETV_HOST)")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT, help="ADB port (default 5555)")
    sub = parser.add_subparsers(dest="command", required=True, metavar="command")

    p = sub.add_parser("probe", help="show model, Android version, firmware and current home")
    p.set_defaults(func=cmd_probe)

    p = sub.add_parser("shell", help="run one shell command on the TV")
    p.add_argument("command_line", nargs=argparse.REMAINDER, metavar="command")
    p.set_defaults(func=cmd_shell)

    p = sub.add_parser("screenshot", help="save a screenshot of the TV as PNG")
    p.add_argument("output")
    p.set_defaults(func=cmd_screenshot)

    p = sub.add_parser("backup", help="save packages, settings and home activity")
    p.add_argument("--out", help="parent folder (default: the quietv config folder)")
    p.set_defaults(func=cmd_backup)

    p = sub.add_parser("debloat", help="disable the packages of a device list")
    p.add_argument("list", help="path to a devices/*.toml file, or its name to fetch it from GitHub")
    p.add_argument("--tier", choices=TIERS, default="recommended", help="how far to go (default recommended)")
    p.add_argument(
        "--exclude",
        action="append",
        choices=CATEGORIES,
        metavar="CATEGORY",
        help="skip a category, e.g. streaming or cast (repeatable)",
    )
    p.add_argument("--dry-run", action="store_true", help="only show what would be disabled")
    p.add_argument("--no-backup", action="store_true", help="skip the automatic backup")
    p.add_argument("--yes", "-y", action="store_true", help="do not ask for confirmation")
    p.set_defaults(func=cmd_debloat)

    p = sub.add_parser("restore", help="undo a debloat from a backup or a device list")
    group = p.add_mutually_exclusive_group()
    group.add_argument("--backup", help="backup folder (default: the newest one)")
    group.add_argument("--list", help="re-enable every package of a device list")
    p.add_argument("--yes", "-y", action="store_true", help="do not ask for confirmation")
    p.set_defaults(func=cmd_restore)

    p = sub.add_parser("check", help="find packages a firmware update enabled again")
    p.add_argument("--fix", action="store_true", help="disable them again")
    p.add_argument("--no-backup", action="store_true", help="skip the automatic backup before --fix")
    p.add_argument("--yes", "-y", action="store_true", help="do not ask for confirmation")
    p.set_defaults(func=cmd_check)

    p = sub.add_parser("report", help="write a device report for a GitHub issue")
    p.add_argument("--out", help="file name (default: quietv-report-<model>.md)")
    p.set_defaults(func=cmd_report)

    p = sub.add_parser("pair", help="pair with wireless debugging (Android 11+) and open port 5555")
    p.add_argument("pair_port", type=int, help="port shown under 'Pair device with pairing code'")
    p.add_argument("code", help="six-digit pairing code")
    p.add_argument("connect_port", type=int, help="port shown under 'IP address and port'")
    p.set_defaults(func=cmd_pair)

    p = sub.add_parser("install", help="install the launcher and make it the home screen")
    p.add_argument("--apk", help="local APK instead of the latest GitHub release")
    p.add_argument("--stock-launcher", help="package of the stock launcher (default: the current home)")
    p.add_argument("--keep-stock-launcher", action="store_true", help="do not disable the stock launcher")
    p.add_argument(
        "--mode",
        choices=("auto", "replace", "redirect"),
        default="auto",
        help="replace the stock home, or keep it and redirect Home to quietv (default: auto, redirect on "
        "Fire TV and Google TV)",
    )
    p.add_argument("--yes", "-y", action="store_true", help="do not ask for confirmation")
    p.set_defaults(func=cmd_install)

    p = sub.add_parser("config", help="push, pull or check the launcher config")
    p.add_argument("action", choices=("push", "pull", "check"))
    p.add_argument("file", nargs="?", help="JSON file (required for push and check)")
    p.set_defaults(func=cmd_config)

    p = sub.add_parser("theme", help="list or apply themes and set a wallpaper")
    p.add_argument("action", choices=("list", "apply", "wallpaper"))
    p.add_argument("source", nargs="?", help="theme name or theme file for apply, image file for wallpaper")
    p.add_argument("--dim", type=int, help="wallpaper dimming in percent, 0 to 80 (default 30)")
    p.add_argument("--off", action="store_true", help="remove the wallpaper")
    p.set_defaults(func=cmd_theme)

    for name, text in (
        ("redirect", "switch the Home redirect on or off (Fire TV, Google TV)"),
        ("firetv", "same as redirect"),
    ):
        p = sub.add_parser(name, help=text)
        p.add_argument("action", choices=("on", "off", "status"))
        p.set_defaults(func=cmd_redirect)
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    if args.command == "config" and args.action in ("push", "check") and not args.file:
        parser.error(f"config {args.action} needs a file")
    if args.command == "shell" and not args.command_line:
        parser.error("shell needs a command")
    if args.command == "pair" and not (args.code.isdigit() and len(args.code) == 6):
        parser.error("the pairing code has six digits")
    if args.command == "theme":
        if args.action == "apply" and not args.source:
            parser.error("theme apply needs a theme name or file")
        if args.action == "wallpaper" and bool(args.source) == args.off:
            parser.error("theme wallpaper needs an image file or --off")
        if args.dim is not None and not 0 <= args.dim <= config.MAX_DIM:
            parser.error(f"--dim must be from 0 to {config.MAX_DIM}")
    try:
        args.func(args)
    except (
        Abort,
        TvError,
        DeviceListError,
        ReleaseError,
        config.ConfigError,
        pair.PairError,
        ValueError,
        OSError,
    ) as e:
        print(f"quietv: {e}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("\nquietv: interrupted", file=sys.stderr)
        return 130
    return 0
