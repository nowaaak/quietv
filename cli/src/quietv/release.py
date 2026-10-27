import hashlib
import json
import urllib.request
from pathlib import Path

from quietv import REPO

API = f"https://api.github.com/repos/{REPO}/releases/latest"


class ReleaseError(Exception):
    pass


def _get(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "quietv-cli", "Accept": "application/octet-stream"})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return response.read()
    except OSError as e:
        raise ReleaseError(f"download failed: {url}: {e}") from e


def parse_sums(text: str) -> dict[str, str]:
    sums = {}
    for line in text.splitlines():
        parts = line.split()
        if len(parts) == 2 and len(parts[0]) == 64:
            sums[parts[1].lstrip("*")] = parts[0].lower()
    return sums


def download_latest(dest: Path) -> tuple[Path, str]:
    request = urllib.request.Request(API, headers={"User-Agent": "quietv-cli", "Accept": "application/vnd.github+json"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            release = json.load(response)
    except OSError as e:
        raise ReleaseError(f"cannot read the latest release of {REPO}: {e}") from e

    assets = {a["name"]: a["browser_download_url"] for a in release.get("assets", [])}
    apks = [name for name in assets if name.endswith(".apk")]
    if len(apks) != 1 or "SHA256SUMS" not in assets:
        raise ReleaseError(f"release {release.get('tag_name')} does not have exactly one APK and a SHA256SUMS file")

    name = apks[0]
    expected = parse_sums(_get(assets["SHA256SUMS"]).decode("utf-8")).get(name)
    if not expected:
        raise ReleaseError(f"SHA256SUMS has no entry for {name}")
    data = _get(assets[name])
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise ReleaseError(f"checksum mismatch for {name}: expected {expected}, got {actual}")

    dest.mkdir(parents=True, exist_ok=True)
    path = dest / name
    path.write_bytes(data)
    return path, release.get("tag_name", "")
