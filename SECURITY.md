# Security policy

quietv runs with high privileges on people's TVs: it changes system settings over ADB and holds an
accessibility service on Fire TV. Please report anything that could be abused.

## Reporting

Report vulnerabilities privately through
[GitHub Security Advisories](https://github.com/nowaaak/quietv/security/advisories/new). Do not open a
public issue. You will get an answer within a week.

Please include the quietv version, your TV model and OS version, and steps to reproduce.

## Scope

In scope: the launcher APK, the CLI, the release workflow and the device lists (for example a list that
disables a package needed for security updates).

Out of scope: ADB itself being open on the network. That is a property of the TV's developer options.
Switch network debugging off when you are done.

## Supported versions

Only the latest release gets fixes.

## Verifying a release

Every release lists the SHA-256 checksum of the APK in `SHA256SUMS` and the fingerprint of the signing
certificate in its notes. Compare the fingerprint with `apksigner verify --print-certs quietv.apk`.
