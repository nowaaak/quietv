import re
import tomllib
import urllib.request
from dataclasses import dataclass
from pathlib import Path

from quietv import REPO
from quietv.packages import is_package

TIERS = ("recommended", "advanced", "expert")
CATEGORIES = (
    "ads",
    "telemetry",
    "content",
    "vendor",
    "preloaded",
    "assistant",
    "cast",
    "streaming",
    "updates",
    "launcher",
)
PLATFORMS = ("androidtv", "googletv", "fireos")
NAME_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
PACKAGE_KEYS = {"id", "category", "tier", "note"}


class DeviceListError(Exception):
    pass


@dataclass(frozen=True)
class Package:
    id: str
    category: str
    tier: str
    note: str = ""


@dataclass(frozen=True)
class Keep:
    id: str
    reason: str


@dataclass(frozen=True)
class DeviceList:
    source: str
    name: str
    brand: str
    model: str
    platform: str
    os: str
    firmware: str
    stock_launcher: str
    notes: str
    packages: tuple[Package, ...]
    keep: tuple[Keep, ...]

    def select(self, tier: str) -> list[Package]:
        limit = TIERS.index(tier)
        return [p for p in self.packages if TIERS.index(p.tier) <= limit]


def load(ref: str) -> DeviceList:
    path = Path(ref)
    if path.is_file():
        return parse(path.read_text(encoding="utf-8"), str(path))
    if NAME_RE.match(ref):
        url = f"https://raw.githubusercontent.com/{REPO}/main/devices/{ref}.toml"
        request = urllib.request.Request(url, headers={"User-Agent": "quietv-cli"})
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return parse(response.read().decode("utf-8"), url)
        except OSError as e:
            raise DeviceListError(f"cannot download device list {ref!r} from {url}: {e}") from e
    raise DeviceListError(f"no such file and not a device list name: {ref}")


def parse(text: str, source: str) -> DeviceList:
    try:
        data = tomllib.loads(text)
    except tomllib.TOMLDecodeError as e:
        raise DeviceListError(f"{source}: invalid TOML: {e}") from e

    problems: list[str] = []
    if data.get("schema") != 1:
        problems.append("schema must be 1")
    device = data.get("device")
    if not isinstance(device, dict):
        raise DeviceListError(f"{source}: missing [device] table")

    for key in ("name", "brand", "model", "platform", "os"):
        if not isinstance(device.get(key), str) or not device[key].strip():
            problems.append(f"device.{key} is required")
    if device.get("platform") not in PLATFORMS:
        problems.append(f"device.platform must be one of {', '.join(PLATFORMS)}")
    stock = device.get("stock_launcher", "")
    if stock and not is_package(stock):
        problems.append(f"device.stock_launcher is not a package name: {stock}")

    packages: list[Package] = []
    seen: set[str] = set()
    for i, raw in enumerate(data.get("package", [])):
        pid = raw.get("id", "")
        if not is_package(pid):
            problems.append(f"package #{i + 1}: invalid id {pid!r}")
            continue
        where = f"package {pid}"
        if pid in seen:
            problems.append(f"{where}: listed twice")
        seen.add(pid)
        if raw.get("category") not in CATEGORIES:
            problems.append(f"{where}: category must be one of {', '.join(CATEGORIES)}")
        if raw.get("tier") not in TIERS:
            problems.append(f"{where}: tier must be one of {', '.join(TIERS)}")
        unknown = set(raw) - PACKAGE_KEYS
        if unknown:
            problems.append(f"{where}: unknown keys {', '.join(sorted(unknown))}")
        packages.append(Package(pid, raw.get("category", ""), raw.get("tier", ""), raw.get("note", "")))

    keep: list[Keep] = []
    for raw in data.get("keep", []):
        kid = raw.get("id", "")
        if not is_package(kid):
            problems.append(f"keep: invalid id {kid!r}")
            continue
        if kid in seen:
            problems.append(f"keep {kid}: also listed as a package to disable")
        if not raw.get("reason"):
            problems.append(f"keep {kid}: reason is required")
        keep.append(Keep(kid, raw.get("reason", "")))

    if stock and stock in seen:
        problems.append(f"{stock}: the stock launcher is handled by 'quietv install', do not list it as a package")
    if problems:
        raise DeviceListError(f"{source}:\n  " + "\n  ".join(problems))

    return DeviceList(
        source=source,
        name=device["name"],
        brand=device["brand"],
        model=device["model"],
        platform=device["platform"],
        os=device["os"],
        firmware=device.get("firmware", ""),
        stock_launcher=stock,
        notes=device.get("notes", ""),
        packages=tuple(packages),
        keep=tuple(keep),
    )
