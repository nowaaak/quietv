import sys
import zipfile

SIGNATURE_FILES = (".SF", ".RSA", ".EC", ".DSA", "MANIFEST.MF")


def contents(path: str) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as apk:
        return {
            info.filename: apk.read(info.filename)
            for info in apk.infolist()
            if not (info.filename.startswith("META-INF/") and info.filename.endswith(SIGNATURE_FILES))
        }


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: compare-apks.py <your-build.apk> <release.apk>", file=sys.stderr)
        return 2
    mine, theirs = contents(sys.argv[1]), contents(sys.argv[2])
    problems = [f"only in {sys.argv[1]}: {name}" for name in sorted(set(mine) - set(theirs))]
    problems += [f"only in {sys.argv[2]}: {name}" for name in sorted(set(theirs) - set(mine))]
    problems += [f"differs: {name}" for name in sorted(set(mine) & set(theirs)) if mine[name] != theirs[name]]
    for line in problems:
        print(line)
    if problems:
        return 1
    print(f"Identical apart from the signature: {len(mine)} entries.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
