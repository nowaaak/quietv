import base64
import hashlib
import re

import pytest

from quietv import wallpaper
from quietv.config import ConfigError

OK = "Broadcasting: Intent { }\nBroadcast completed: result=-1\n"


def test_image_kinds():
    assert wallpaper.image_kind(b"\xff\xd8\xff\xe0rest") == "JPEG"
    assert wallpaper.image_kind(b"\x89PNG\r\n\x1a\nrest") == "PNG"
    assert wallpaper.image_kind(b"RIFF\x00\x00\x00\x00WEBPVP8 ") == "WebP"
    assert wallpaper.image_kind(b"GIF89a") is None


def test_read_image_rejects_other_files(tmp_path):
    path = tmp_path / "notes.txt"
    path.write_text("hello", encoding="utf-8")
    with pytest.raises(ConfigError, match="not a JPEG"):
        wallpaper.read_image(path)


def test_read_image_rejects_huge_files(tmp_path):
    path = tmp_path / "big.png"
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + b"0" * wallpaper.MAX_BYTES)
    with pytest.raises(ConfigError, match="larger than"):
        wallpaper.read_image(path)


def test_upload_sends_ordered_chunks_with_a_checksum(fake_tv):
    data = bytes(range(256)) * 500
    tv = fake_tv({"am broadcast": OK})
    progress = []
    wallpaper.upload(tv, data, lambda done, total: progress.append((done, total)))

    assert len(tv.commands) == 3
    received = b""
    for i, command in enumerate(tv.commands):
        assert f"-a {wallpaper.ACTION_CHUNK}" in command
        assert f"--el offset {len(received)}" in command
        received += base64.b64decode(re.search(r"--es data '([^']+)'", command).group(1))
        assert ("--ez last true" in command) == (i == 2)
    assert received == data
    assert tv.commands[-1].endswith(f"--es sha256 {hashlib.sha256(data).hexdigest()}")
    assert progress[-1] == (3, 3)


def test_upload_stops_when_the_launcher_refuses(fake_tv):
    tv = fake_tv({"am broadcast": 'Broadcast completed: result=0, data="checksum mismatch"\n'})
    with pytest.raises(ConfigError, match="checksum mismatch"):
        wallpaper.upload(tv, b"x" * 10)
