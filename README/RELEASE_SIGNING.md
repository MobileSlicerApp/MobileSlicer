# Android Release Signing

Current state: `:app:assembleRelease` requires an explicit release signing
configuration. Debug APKs are signed automatically by the Android debug keystore
and are suitable for local ADB installs only.

## Local Debug Install

Use this for development device pushes:

```bash
cd android-app
./gradlew :app:assembleDebug
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Release Signing Inputs

Provide signing values through environment variables:

```bash
export MOBILE_SLICER_RELEASE_STORE_FILE=/absolute/path/to/release.jks
export MOBILE_SLICER_RELEASE_STORE_PASSWORD=...
export MOBILE_SLICER_RELEASE_KEY_ALIAS=...
export MOBILE_SLICER_RELEASE_KEY_PASSWORD=...
```

Alternatively, create ignored local file `android-app/release-signing.properties`:

```properties
mobileSlicer.release.storeFile=/absolute/path/to/release.jks
mobileSlicer.release.storePassword=...
mobileSlicer.release.keyAlias=...
mobileSlicer.release.keyPassword=...
```

## Release Version Inputs

Default local release metadata is:

```text
versionName=0.1.0
versionCode=1
```

Override it in CI or local release builds with environment variables:

```bash
export MOBILE_SLICER_VERSION_NAME=0.1.1
export MOBILE_SLICER_VERSION_CODE=2
```

Or add matching Gradle properties in the ignored local
`android-app/release-signing.properties` file:

```properties
mobileSlicer.versionName=0.1.1
mobileSlicer.versionCode=2
```

`versionCode` must be a positive integer. If it is absent or invalid, the build
falls back to `1`; keep CI responsible for monotonically increasing release
codes before uploading to a store.

Then build:

```bash
cd android-app
./gradlew :app:assembleRelease
adb -s <serial> install -r app/build/outputs/apk/release/app-release.apk
```

If signing inputs are absent, Gradle fails the release task instead of producing
an unsigned release artifact.

## Production Signing Guardrails

* Keep production keystores and passwords outside this repository.
* Prefer CI or local environment variables for `storeFile`, `storePassword`,
  `keyAlias`, and `keyPassword`.
* Do not commit generated `.apk`, `.aab`, `.jks`, `.keystore`, or signing
  property files.
* Set `MOBILE_SLICER_VERSION_NAME` and `MOBILE_SLICER_VERSION_CODE` explicitly
  for every release candidate.
* Before a public release candidate, run `scripts/verify_android.sh local`,
  `scripts/verify_android.sh release`, and
  `MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1 scripts/verify_android.sh slice-regression <serial>`,
  then install the signed artifact on a device and smoke-test model import,
  workspace rendering, slicing, export/share, and printer upload.
