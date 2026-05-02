# Build Hardening

Mobile Slicer intentionally keeps Android build inputs pinned and reviewed.

## Pinned Toolchain

- Gradle wrapper: `android-app/gradle/wrapper/gradle-wrapper.properties`
- Android Gradle Plugin, Kotlin, Compose BOM, and app dependencies: `android-app/gradle/libs.versions.toml`
- CMake version: `android-app/app/build.gradle.kts`
- Android NDK: resolved by the checked-in Android project/SDK environment; release builds must use the same staged Orca native dependency layout as local verification.

Do not update these opportunistically. Toolchain upgrades should be their own change with:

- `./scripts/verify_android.sh lint`
- `./scripts/verify_android.sh unit`
- `./scripts/verify_android.sh local`
- `./scripts/verify_android.sh release`
- Device launch smoke when hardware is available: `MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1 ./scripts/verify_android.sh device-automation`

## Native Warning Policy

Native release builds suppress known third-party Eigen/admesh/libslic3r warning noise through the named CMake option:

- `MOBILE_SLICER_SUPPRESS_VENDOR_WARNING_NOISE`

This keeps release logs readable without relaxing the real-wrapper requirement. New warnings from Mobile Slicer wrapper sources should be fixed rather than broadly suppressed.

## Dependency Policy

- Prefer version catalog updates over inline Gradle versions.
- Avoid automatic dependency upgrade bots until native/release/device smoke is consistently automated.
- Review Android SDK, AGP, Kotlin, Compose, and NDK upgrades together because they can affect Compose compilation, lint behavior, R8, and C++ warning output.
- Keep `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` enabled so dependency sources remain centralized.

## Release Gate

Use `./scripts/verify_android.sh release` before cutting release candidates. Without signing credentials, it runs release Kotlin compilation, release lint vital checks, and R8. With release signing configured, it also packages the signed release APK.
