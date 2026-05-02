# Mobile Slicer Verification

Use `scripts/verify_android.sh` for repeatable local, APK, device, and Benchy checks.

## Quick Commands

```bash
scripts/verify_android.sh unit
scripts/verify_android.sh lint
scripts/verify_android.sh stubs
scripts/verify_android.sh script-tests
scripts/verify_android.sh apk
scripts/verify_android.sh local
```

After large Kotlin file-boundary refactors, run at least:

```bash
scripts/verify_android.sh unit
scripts/verify_android.sh apk
```

## Device Smoke

The script defaults to `$ANDROID_SERIAL`, then `RFCYA01ANVE`.

```bash
scripts/verify_android.sh device
scripts/verify_android.sh device RFCYA01ANVE
MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1 scripts/verify_android.sh device-automation RFCYA01ANVE
MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1 scripts/verify_android.sh slice-regression RFCYA01ANVE
```

`device` builds and installs the debug APK only. `device-automation` builds, installs,
cold-launches the app, verifies a clean crash buffer, slices `mobileslicer_test_cube.stl`
through `com.mobileslicer.action.AUTOMATE_SLICE`, and fails if the status is not
`success:` or the emitted G-code is unexpectedly small. `slice-regression` runs a
physical-device matrix for brim, wall count, infill, support, temperature, and speed
changes, pulls emitted G-code, and checks that the expected G-code geometry or command
signals changed.

If device automation fails after a slice starts, the script captures context,
logcat, crash logcat, status text, and G-code head/tail snippets under
`artifacts/device-automation/`.

## Benchy Automation

```bash
scripts/verify_android.sh benchy /home/peanut/Documents/3DBenchy.stl
scripts/verify_android.sh benchy /home/peanut/Documents/3DBenchy.stl RFCYA01ANVE
```

This builds and installs the debug APK, stages the STL into app-private storage with `run-as`, starts the existing `com.mobileslicer.action.AUTOMATE_SLICE` path, prints the status file, and lists the app-private G-code output.

Expected successful status shape:

```text
success: model=... staged=... output=... bytes=... placementMs=... elapsedMs=... config=...
```

## Full Local Plus Device Smoke

```bash
scripts/verify_android.sh all
```

This runs JVM tests, builds the APK, installs it on the device, and launches the app.

## Notes

- `unit` maps to `cd android-app && ./gradlew testDebugUnitTest`.
- `lint` maps to `cd android-app && ./gradlew lintDebug`.
- `stubs` validates `engine-wrapper/orca-android-libslic3r/stub_inventory.json`
  against the active Android Orca subset stub files and the Android Orca CMake
  target references.
- `script-tests` runs shell syntax checks for verification/build scripts.
- `apk` maps to `cd android-app && ./gradlew assembleDebug`.
- `local` runs `script-tests`, `stubs`, `asset-tests`, `lint`, `unit`, and `apk`
  in that order.
- `release` runs release Kotlin compilation, `lintVitalRelease`, and R8 when
  signing is not configured; with release signing configured it builds the signed
  release APK and reports the artifact path. Set `MOBILE_SLICER_VERSION_NAME` and
  `MOBILE_SLICER_VERSION_CODE` for release-candidate builds.
- Device modes use `tools/adb` when present, falling back to `adb` on `PATH`.
- Benchy mode avoids shared-storage raw reads by copying the STL into the app-private `files/automation` directory before starting automation.
- Use the Samsung device for final runtime truth when Waydroid and physical-device results differ.
