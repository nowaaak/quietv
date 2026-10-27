import base64
import hashlib
import re
from pathlib import Path

import pytest

REPO_ROOT = Path(__file__).resolve().parents[2]


class FakeTv:
    def __init__(self, responses: dict[str, str] | None = None):
        self.responses = responses or {}
        self.commands: list[str] = []
        self.files: dict[str, bytes] = {}

    def shell(self, command: str, timeout: float = 120.0) -> str:
        self.commands.append(command)
        append = re.fullmatch(r"echo '([^']*)' >> '([^']+)' && echo (ok\d+)", command)
        if append:
            self.files[append.group(2)] = self.files.get(append.group(2), b"") + append.group(1).encode() + b"\n"
            return append.group(3) + "\n"
        decode = re.fullmatch(r"base64 -d '([^']+)' > '([^']+)' && rm '[^']+' && md5sum '[^']+'", command)
        if decode:
            chunks = self.files.pop(decode.group(1)).decode().split()
            data = b"".join(base64.b64decode(c) for c in chunks)
            self.files[decode.group(2)] = data
            return f"{hashlib.md5(data).hexdigest()}  {decode.group(2)}\n"
        for prefix, reply in self.responses.items():
            if command.startswith(prefix):
                return reply
        return ""

    def pull(self, remote, local) -> None:
        Path(local).write_bytes(self.files.get(remote, b""))

    def close(self) -> None:
        pass

    def __enter__(self):
        return self

    def __exit__(self, *exc) -> None:
        pass


@pytest.fixture
def fake_tv():
    return FakeTv


@pytest.fixture
def repo_root() -> Path:
    return REPO_ROOT
