import base64
import hashlib
from collections.abc import Callable
from pathlib import Path

from quietv import PACKAGE
from quietv.config import RECEIVER, RESULT_OK, ConfigError, parse_broadcast
from quietv.tv import Shell

ACTION_CHUNK = f"{PACKAGE}.action.WALLPAPER_CHUNK"
ACTION_CLEAR = f"{PACKAGE}.action.CLEAR_WALLPAPER"
MAX_BYTES = 8 * 1024 * 1024
CHUNK = 48 * 1024
SIGNATURES = {
    b"\xff\xd8\xff": "JPEG",
    b"\x89PNG\r\n\x1a\n": "PNG",
}


def image_kind(data: bytes) -> str | None:
    for signature, kind in SIGNATURES.items():
        if data.startswith(signature):
            return kind
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "WebP"
    return None


def read_image(path: Path) -> bytes:
    data = path.read_bytes()
    if image_kind(data) is None:
        raise ConfigError(f"{path} is not a JPEG, PNG or WebP image")
    if len(data) > MAX_BYTES:
        raise ConfigError(f"{path} is larger than {MAX_BYTES // 1024 // 1024} MB")
    return data


def upload(tv: Shell, data: bytes, progress: Callable[[int, int], None] | None = None) -> None:
    digest = hashlib.sha256(data).hexdigest()
    count = max(1, (len(data) + CHUNK - 1) // CHUNK)
    for i in range(count):
        offset = i * CHUNK
        part = base64.b64encode(data[offset : offset + CHUNK]).decode()
        last = i == count - 1
        command = (
            f"am broadcast -n {RECEIVER} -a {ACTION_CHUNK} --el offset {offset} --es data '{part}' "
            f"--ez last {'true' if last else 'false'}"
        )
        if last:
            command += f" --es sha256 {digest}"
        code, message = parse_broadcast(tv.shell(command, timeout=60.0))
        if code != RESULT_OK:
            raise ConfigError(f"the launcher rejected the wallpaper: {message or 'is quietv 0.2 or newer installed?'}")
        if progress:
            progress(i + 1, count)


def clear(tv: Shell) -> None:
    code, message = parse_broadcast(tv.shell(f"am broadcast -n {RECEIVER} -a {ACTION_CLEAR}"))
    if code != RESULT_OK:
        raise ConfigError(f"the launcher did not clear the wallpaper: {message or 'is quietv installed?'}")
