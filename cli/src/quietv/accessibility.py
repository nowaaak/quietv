def _services(current: str | None) -> list[str]:
    if current is None:
        return []
    value = current.strip()
    if value in ("", "null"):
        return []
    return [s for s in value.split(":") if s]


def add_service(current: str | None, service: str) -> str:
    services = _services(current)
    if service not in services:
        services.append(service)
    return ":".join(services)


def remove_service(current: str | None, service: str) -> str | None:
    services = [s for s in _services(current) if s != service]
    return ":".join(services) or None


def normalize(current: str | None) -> str | None:
    return ":".join(_services(current)) or None
