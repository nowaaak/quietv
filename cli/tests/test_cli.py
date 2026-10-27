import pytest

from quietv import cli, release


@pytest.fixture
def connected(monkeypatch, fake_tv):
    def use(responses):
        tv = fake_tv(responses)
        monkeypatch.setattr(cli, "connect", lambda args: tv)
        return tv

    return use


def test_help_lists_all_commands(capsys):
    with pytest.raises(SystemExit):
        cli.main(["--help"])
    out = capsys.readouterr().out
    for command in ("probe", "backup", "debloat", "restore", "install", "config", "firetv", "screenshot"):
        assert command in out


def test_shell_passes_options_through(connected, capsys):
    tv = connected({"pm list packages -e": "package:com.a\n"})
    assert cli.main(["shell", "pm", "list", "packages", "-e"]) == 0
    assert tv.commands == ["pm list packages -e"]
    assert "package:com.a" in capsys.readouterr().out


def test_shell_without_command_is_rejected():
    with pytest.raises(SystemExit):
        cli.main(["shell"])


def test_missing_host_is_a_clean_error(monkeypatch, capsys):
    monkeypatch.delenv("QUIETV_HOST", raising=False)
    assert cli.main(["probe"]) == 1
    assert "--host" in capsys.readouterr().err


def test_debloat_dry_run_changes_nothing(connected, repo_root, capsys):
    tv = connected({"pm list packages -e": "package:com.miui.tv.analytics\npackage:com.xiaomi.mitv.updateservice\n"})
    path = str(repo_root / "devices" / "xiaomi-mi-tv-4a.toml")
    assert cli.main(["debloat", path, "--dry-run"]) == 0
    out = capsys.readouterr().out
    assert "disable com.miui.tv.analytics" in out
    assert "updateservice" not in out
    assert not any(c.startswith("pm disable") for c in tv.commands)


def test_debloat_dry_run_works_without_a_tv(monkeypatch, repo_root, capsys):
    monkeypatch.delenv("QUIETV_HOST", raising=False)
    path = str(repo_root / "devices" / "chiq-ai-pont.toml")
    assert cli.main(["debloat", path, "--dry-run", "--tier", "advanced"]) == 0
    out = capsys.readouterr().out
    assert "disable com.google.android.katniss [assistant, advanced]" in out
    assert "without a TV" in out


def test_debloat_exclude_skips_a_category(connected, repo_root, capsys):
    connected({"pm list packages -e": "package:com.disney.disneyplus\npackage:com.amazon.tv.acr\n"})
    path = str(repo_root / "devices" / "toshiba-fire-tv-aftha004.toml")
    assert cli.main(["debloat", path, "--tier", "advanced", "--exclude", "streaming", "--dry-run"]) == 0
    out = capsys.readouterr().out
    assert "com.amazon.tv.acr" in out
    assert "disneyplus" not in out


def test_debloat_disables_after_backup(connected, repo_root, monkeypatch, tmp_path, capsys):
    monkeypatch.setenv("QUIETV_HOME", str(tmp_path))
    tv = connected(
        {
            "pm list packages -e": "package:com.miui.tv.analytics\n",
            "pm disable-user": "Package com.miui.tv.analytics new state: disabled-user\n",
        }
    )
    path = str(repo_root / "devices" / "xiaomi-mi-tv-4a.toml")
    assert cli.main(["debloat", path, "--yes"]) == 0
    assert "pm disable-user --user 0 com.miui.tv.analytics" in tv.commands
    assert any((tmp_path / "backups").iterdir())
    assert "Disabled 1 of 1" in capsys.readouterr().out


def test_debloat_without_tty_needs_yes(connected, repo_root, monkeypatch, capsys):
    monkeypatch.setattr("sys.stdin.isatty", lambda: False)
    connected({"pm list packages -e": "package:com.miui.tv.analytics\n"})
    path = str(repo_root / "devices" / "xiaomi-mi-tv-4a.toml")
    assert cli.main(["debloat", path]) == 1
    assert "--yes" in capsys.readouterr().err


def test_install_on_fire_os_turns_on_the_redirect(connected, tmp_path):
    apk = tmp_path / "quietv.apk"
    apk.write_bytes(b"apk")
    tv = connected(
        {
            "getprop ro.product.manufacturer": "Amazon\n",
            "pm install": "Success\n",
            "settings get secure enabled_accessibility_services": "null\n",
        }
    )
    assert cli.main(["install", "--apk", str(apk), "--yes"]) == 0
    assert any("enabled_accessibility_services io.github.nowaaak.quietv/" in c for c in tv.commands)
    assert not any(c.startswith("pm disable-user") for c in tv.commands)
    assert not any("set-home-activity" in c for c in tv.commands)


def test_install_on_android_tv_disables_the_stock_launcher(connected, tmp_path):
    apk = tmp_path / "quietv.apk"
    apk.write_bytes(b"apk")
    tv = connected(
        {
            "getprop ro.product.manufacturer": "CHiQ\n",
            "cmd package resolve-activity": "com.google.android.tvlauncher/.MainActivity\n",
            "pm install": "Success\n",
        }
    )
    assert cli.main(["install", "--apk", str(apk), "--yes"]) == 0
    assert "cmd package set-home-activity --user 0 io.github.nowaaak.quietv/.HomeEntry" in tv.commands
    assert "pm disable-user --user 0 com.google.android.tvlauncher" in tv.commands


def test_parse_sums():
    digest = "a" * 64
    assert release.parse_sums(f"{digest}  quietv-0.1.0.apk\n{digest} *other.apk\nnoise\n") == {
        "quietv-0.1.0.apk": digest,
        "other.apk": digest,
    }
