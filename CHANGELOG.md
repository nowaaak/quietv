# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com)
and the project uses [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added

- Launcher rewritten in Kotlin, package `io.github.nowaaak.quietv`.
- Runtime configuration: one APK for every TV, set up on the TV or with `quietv config push`.
- Fire OS detected at runtime. The Home redirect no longer needs a separate build.
- Hold OK on a tile to move it, hide it or open its app info.
- Settings screen with Graphite and Paper themes, accent colours, tiles per row and hidden apps.
- Companion switch between a TV and a streaming stick, now protected by a shared token.
- `quietv` CLI for Windows, macOS and Linux, replacing the shell scripts.
- Device lists in TOML with categories, tiers and packages to keep.
- CI with lint and tests, signed releases with SHA-256 checksums.
