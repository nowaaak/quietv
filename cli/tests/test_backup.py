from quietv import backup


def write_backup(folder, enabled, home, secure):
    folder.mkdir()
    (folder / "packages_enabled.txt").write_text("".join(f"package:{p}\n" for p in enabled))
    (folder / "home_activity.txt").write_text(home)
    (folder / "settings_secure.txt").write_text(secure)


def test_restore_plan_only_re_enables_what_was_enabled(tmp_path):
    folder = tmp_path / "b"
    write_backup(
        folder,
        ["com.a", "com.b", "com.c"],
        "priority=0 preferredOrder=0\ncom.google.android.tvlauncher/.MainActivity\n",
        "default_input_method=com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME\n"
        "enabled_accessibility_services=null\n",
    )
    plan = backup.restore_plan(folder, enabled_now={"com.a", "com.extra"})
    assert plan == [
        "cmd package install-existing com.b",
        "pm enable com.b",
        "cmd package install-existing com.c",
        "pm enable com.c",
        "cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity",
        "ime set com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME",
        "settings delete secure enabled_accessibility_services",
    ]


def test_restore_plan_puts_back_accessibility_services(tmp_path):
    folder = tmp_path / "b"
    write_backup(folder, [], "", "enabled_accessibility_services=com.x/com.x.Svc\n")
    assert backup.restore_plan(folder, set()) == ["settings put secure enabled_accessibility_services com.x/com.x.Svc"]


def test_restore_plan_ignores_unsafe_values(tmp_path):
    folder = tmp_path / "b"
    write_backup(
        folder,
        ["com.ok", "bad;reboot"],
        "",
        "default_input_method=x; reboot\nenabled_accessibility_services=a/b;reboot\n",
    )
    assert backup.restore_plan(folder, set()) == ["cmd package install-existing com.ok", "pm enable com.ok"]


def test_create_writes_all_files(tmp_path, fake_tv):
    tv = fake_tv({"getprop ro.product.model": "Mi TV/4A\n", "pm list packages -e": "package:com.a\n"})
    folder = backup.create(tv, tmp_path)
    assert folder.name.startswith("Mi-TV-4A-")
    assert (folder / "packages_enabled.txt").read_text() == "package:com.a\n"
    assert (folder / "meta.json").is_file()
    assert backup.latest(tmp_path) == folder
