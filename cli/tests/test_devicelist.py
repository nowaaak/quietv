import pytest

from quietv import devicelist
from quietv.devicelist import DeviceListError

VALID = """
schema = 1
[device]
name = "Test TV"
brand = "Test"
model = "T1"
platform = "androidtv"
os = "Android TV 11"
stock_launcher = "com.google.android.tvlauncher"

[[package]]
id = "com.vendor.ads"
category = "ads"
tier = "recommended"

[[package]]
id = "com.vendor.cast"
category = "cast"
tier = "advanced"

[[package]]
id = "com.vendor.risky"
category = "vendor"
tier = "expert"

[[keep]]
id = "com.vendor.keys"
reason = "remote keys"
"""


def test_every_device_list_in_the_repo_is_valid(repo_root):
    files = sorted((repo_root / "devices").glob("*.toml"))
    assert files
    for path in files:
        device_list = devicelist.load(str(path))
        assert device_list.packages, path.name


def test_device_list_file_names_are_lowercase_slugs(repo_root):
    for path in (repo_root / "devices").glob("*.toml"):
        assert devicelist.NAME_RE.match(path.stem), path.name


def test_select_includes_lower_tiers():
    device_list = devicelist.parse(VALID, "test")
    assert [p.id for p in device_list.select("recommended")] == ["com.vendor.ads"]
    assert [p.id for p in device_list.select("advanced")] == ["com.vendor.ads", "com.vendor.cast"]
    assert len(device_list.select("expert")) == 3


def test_keep_entries_are_parsed():
    device_list = devicelist.parse(VALID, "test")
    assert device_list.keep[0].id == "com.vendor.keys"


@pytest.mark.parametrize(
    ("change", "message"),
    [
        (('tier = "advanced"', 'tier = "sometimes"'), "tier must be one of"),
        (('category = "ads"', 'category = "misc"'), "category must be one of"),
        (('id = "com.vendor.ads"', 'id = "com.vendor.ads; reboot"'), "invalid id"),
        (('id = "com.vendor.cast"', 'id = "com.vendor.ads"'), "listed twice"),
        (('id = "com.vendor.keys"', 'id = "com.vendor.risky"'), "also listed as a package"),
        (('platform = "androidtv"', 'platform = "tizen"'), "device.platform"),
        (("schema = 1", "schema = 2"), "schema must be 1"),
        (('id = "com.vendor.risky"', 'id = "com.google.android.tvlauncher"'), "handled by 'quietv install'"),
    ],
)
def test_invalid_lists_are_rejected(change, message):
    with pytest.raises(DeviceListError, match=message):
        devicelist.parse(VALID.replace(*change, 1), "test")


def test_unknown_reference_is_rejected():
    with pytest.raises(DeviceListError, match="not a device list name"):
        devicelist.load("../etc/passwd")
