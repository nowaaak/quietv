import re

PACKAGE_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")
COMPONENT_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_.]*/[A-Za-z0-9_.$]+$")


def is_package(name: str) -> bool:
    return bool(PACKAGE_RE.match(name))


def require_package(name: str) -> str:
    if not is_package(name):
        raise ValueError(f"not a valid package name: {name!r}")
    return name


def parse_package_list(output: str) -> set[str]:
    found = set()
    for line in output.splitlines():
        line = line.strip()
        if line.startswith("package:"):
            name = line[len("package:") :].strip()
            if is_package(name):
                found.add(name)
    return found


def parse_home_activity(output: str) -> str | None:
    for line in reversed(output.splitlines()):
        line = line.strip()
        if COMPONENT_RE.match(line):
            return line
    return None
