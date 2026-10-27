# Reproducible builds

Every release APK can be rebuilt from its tag. Apart from the signature, your build and the one on the
release page are byte for byte the same. This was checked for 0.4.0 between a Windows build and the
Linux build on GitHub Actions.

## Check a release

You need JDK 17 and the Android SDK with platform 36 and build-tools 36.0.0.

```bash
git clone https://github.com/nowaaak/quietv
cd quietv
git checkout v0.4.0
cd launcher
./gradlew assembleRelease
cd ..
curl -LO https://github.com/nowaaak/quietv/releases/download/v0.4.0/quietv.apk
python scripts/compare-apks.py launcher/app/build/outputs/apk/release/app-release-unsigned.apk quietv.apk
```

Without a keystore the build is unsigned. `compare-apks.py` compares every file in both APKs and
ignores only the signature files. It prints `Identical apart from the signature` when they match.

## What keeps it reproducible

- Release builds do not embed a dependency info block (`dependenciesInfo` in `app/build.gradle.kts`).
- The Gradle wrapper, the Android Gradle Plugin and every dependency are pinned to exact versions.
- The app has no build timestamps, no generated version codes and no native code.

If you find a difference, please open an issue with the output of `compare-apks.py` and the versions
of your JDK and Android SDK.
