import pytest

from quietv import install
from quietv.accessibility import add_service, remove_service
from quietv.tv import TvError, push_bytes

SERVICE = install.REDIRECT_SERVICE


def test_add_service_keeps_existing_services():
    assert add_service("null", SERVICE) == SERVICE
    assert add_service("", SERVICE) == SERVICE
    assert add_service("com.x/.A", SERVICE) == f"com.x/.A:{SERVICE}"
    assert add_service(f"com.x/.A:{SERVICE}", SERVICE) == f"com.x/.A:{SERVICE}"


def test_remove_service_leaves_other_services():
    assert remove_service(f"com.x/.A:{SERVICE}", SERVICE) == "com.x/.A"
    assert remove_service(SERVICE, SERVICE) is None
    assert remove_service("null", SERVICE) is None


def test_redirect_off_restores_other_services(fake_tv):
    tv = fake_tv({"settings get secure enabled_accessibility_services": f"com.x/.A:{SERVICE}\n"})
    install.redirect_off(tv)
    assert tv.commands[-2:] == [
        "settings put secure enabled_accessibility_services com.x/.A",
        "settings put secure accessibility_enabled 1",
    ]


def test_redirect_off_with_nothing_left_switches_accessibility_off(fake_tv):
    tv = fake_tv({"settings get secure enabled_accessibility_services": f"{SERVICE}\n"})
    install.redirect_off(tv)
    assert tv.commands[-1] == "settings put secure accessibility_enabled 0"


def test_redirect_on_grants_permission_and_rebinds(fake_tv):
    tv = fake_tv({"settings get secure enabled_accessibility_services": "null\n"})
    install.redirect_on(tv)
    assert tv.commands[0].startswith("pm grant io.github.nowaaak.quietv android.permission.WRITE_SECURE_SETTINGS")
    assert "settings delete secure enabled_accessibility_services" in tv.commands
    assert f"settings put secure enabled_accessibility_services {SERVICE}" in tv.commands


def test_current_home_package_ignores_resolver_and_quietv(fake_tv):
    query = install.HOME_QUERY
    assert install.current_home_package(fake_tv({query: "com.amazon.tv.launcher/.ui.HomeActivity\n"})) == (
        "com.amazon.tv.launcher"
    )
    assert install.current_home_package(fake_tv({query: "android/com.android.internal.app.ResolverActivity"})) is None
    assert install.current_home_package(fake_tv({query: "No activity found\n"})) is None


def test_push_bytes_round_trip(fake_tv):
    tv = fake_tv()
    data = bytes(range(256)) * 700
    push_bytes(tv, data, "/data/local/tmp/x.apk")
    assert tv.files["/data/local/tmp/x.apk"] == data


def test_push_bytes_detects_corruption(fake_tv):
    class Corrupting(fake_tv):
        def shell(self, command, timeout=120.0):
            if command.startswith("base64 -d"):
                return "0" * 32 + "  x\n"
            return super().shell(command, timeout)

    with pytest.raises(TvError, match="checksum mismatch"):
        push_bytes(Corrupting(), b"abc", "/data/local/tmp/x.apk")


def test_install_apk_reports_pm_failure(tmp_path, fake_tv):
    apk = tmp_path / "q.apk"
    apk.write_bytes(b"apk")
    tv = fake_tv({"pm install": "Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]\n"})
    with pytest.raises(TvError, match="INSTALL_FAILED_UPDATE_INCOMPATIBLE"):
        install.install_apk(tv, apk)
