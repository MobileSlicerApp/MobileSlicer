# Native Slice Performance Notes

## 2026-04-29 final status

This performance thread is closed for now.

The regression was not in Orca's core slicer after the latest profile work. It
was in Android/profile preparation and repeated profile/config work before the
native slice call. That path is now cached and measured.

Verified device result after Android profile/config caching:

```text
slice_config_prepare elapsedMs=0
native_cache=hit
plate_cache=hit
repair_cache=hit
profile_override_counts=printer:0,filament:0,process:0
```

When process fields were changed, the first changed-profile slice still stayed
small:

```text
slice_config_prepare elapsedMs=13
native_cache=hit
plate_cache=miss
repair_cache=miss
profile_override_counts=printer:0,filament:0,process:3
```

Repeating that changed-profile slice returned to cache hits and `0 ms` config
prep. This means the button-press lag from profile/config preparation is fixed.

Latest measured native breakdown from device after instrumentation:

```text
slice_config_prepare elapsedMs=12
config_detail fullMs=0 seedMs=0 overridesMs=41 runtimeBaselineMs=0 boundsMs=0 jsonBytes=31498
prepare_model_detail objectOverridesMs=3 instanceMs=0 bedAssignMs=0 addedInstances=0 objects=1
apply_validate_detail applyMs=2 calibrationExtractMs=3 calibrationSetMs=0 validateMs=2 statusMs=0
process stageMs=1122 totalMs=1181
export_gcode stageMs=513 totalMs=1694
export_summary_detail exportCallMs=513 fileStatMs=0 summaryParseMs=22 summaryReplaceMs=0 bytes=2959407 processorSeconds=1738
sliceCurrentModel:end elapsedMs=1753
```

Conclusion:

* Android/profile config preparation is no longer the bottleneck.
* Remaining time is dominated by Orca `print.process()` and
  `print.export_gcode(...)`.
* `apply_json_overrides(...)` is measurable at about `41 ms`, but not large
  enough to justify risky work right now.
* Do not optimize Orca `print.process()` or `print.export_gcode(...)` without a
  stronger fixture-based parity harness. That work is intentionally deferred.

## 2026-04-29 Android profile/config prep changes

Changed behavior:

* `ActiveSlicerConfiguration.toNativeSliceConfigBuildResult(...)` now exposes
  cache hit/miss and timing detail while preserving
  `toNativeSliceConfigJson(...)` as the simple API.
* Native slice config cache keys use exact profile content hashes instead of
  serializing complete profiles to JSON before cache lookup.
* The selected profile config is warmed from Compose when printer, filament, or
  process content changes, moving most resolved-profile work off the `Slice`
  button path.
* Plate filament slot native config output is cached by base config, active
  slots, object slot assignment, filament profile content, and flush volumes.
* Orca filament asset repair is cached by printer content, input config, and
  filament override content.
* `slice_config_prepare` logs now include native config cache, plate cache,
  repair cache, per-stage timing, and printer/filament/process override counts.

Guardrail tests:

* `NativeSliceConfigTest.nativeSliceConfigBuildCacheHitsButInvalidatesOnChangedProcessOverride`
  proves unchanged config hits the cache and a changed process override misses
  and changes only the expected key.
* Existing profile parity tests verify unchanged Orca baselines remain intact
  and edited UI fields become override JSON instead of mutating resolved Orca
  JSON.

Revert scope:

* `android-app/app/src/main/java/com/mobileslicer/profiles/NativeSliceConfiguration.kt`
* `android-app/app/src/main/java/com/mobileslicer/profiles/NativeSliceConfigurationHelpers.kt`
* `android-app/app/src/main/java/com/mobileslicer/profiles/ProfileModels.kt`
* `android-app/app/src/main/java/com/mobileslicer/ModelLoaderScreen.kt`
* `android-app/app/src/test/java/com/mobileslicer/NativeSliceConfigTest.kt`

If this needs to be backed out, remove the build-result/timing APIs, remove the
plate/repair caches, return the slice path to `toNativeSliceConfigJson(...)`,
and remove the cache invalidation test. Do not revert the profile override
architecture unless the goal is to abandon Orca profile parity.

## 2026-04-29 instrumentation pass

Goal: identify the remaining native slice/export hot spots after Android profile config preparation was reduced to cache hits.

Changed files:

* `engine-wrapper/orca_wrapper.cpp`
  * Added `elapsed_ms_since(...)`.
  * Added non-semantic `orca_slice` timing logs:
    * `config_detail`
    * `prepare_model_detail`
    * `apply_validate_detail`
    * `export_gcode attempt=1 tempPathMs=...`
    * `export_summary_detail`
  * No toolpath, config, profile, model, or G-code writer logic was intentionally changed.

Revert scope:

* Remove the timing logs and `elapsed_ms_since(...)` helper from `engine-wrapper/orca_wrapper.cpp`.
* Delete this note if the instrumentation is no longer useful.

Verification expected after this pass:

* Unit tests still pass.
* Debug APK builds and installs.
* Re-slicing the same STL/profile should produce identical G-code except generated timestamp metadata.
* Logs should identify whether remaining time is dominated by `print.process()`, `print.export_gcode(...)`, or Android summary parsing.

Actual verification:

* `NativeSliceConfigTest` and `ProfileModelsTest` passed after the native
  instrumentation and bed-type parity fix.
* Debug APK built and installed to `192.168.1.156:40697`.
* `git diff --check` passed.
* Device logs showed remaining time dominated by `print.process()` and
  `print.export_gcode(...)`, so deeper native optimization was closed as not
  worth the risk for now.

## 2026-04-29 quiet timing-log default

Changed behavior:

* Android `slice_config_prepare` detail logging is now guarded by
  `VerboseSliceConfigTimingLogs = false` in `ModelLoaderScreen.kt`.
* Native wrapper detail logging is now guarded by
  `kVerboseNativeTimingLogs = false` in `engine-wrapper/orca_wrapper.cpp`.
* The guarded native logs are diagnostic-only timing/cache lines:
  `config_detail`, `prepare_model_detail`, `apply_validate_detail`,
  `export_summary_detail`, `gcode_preview_parser`, `gcode_preview_cache`, and
  `gcode_viewer_load_latest_slice`.

Re-enable only while profiling a regression, then turn the flags back off before
normal device testing so logcat volume does not become part of the slice-button
cost.

## Slice-path cleanup follow-up

Changed behavior:

* Native diagnostic `orca_slice` detail logs are guarded by
  `kVerboseNativeTimingLogs` so normal slices do not build or emit detailed
  config/extruder/support/timing strings.
* Android no longer clears the prepared viewer mesh cache or forces
  `Runtime.gc()` before every normal native slice. Memory cleanup remains on the
  native retry path after the known transient `vector` failure.

These changes do not alter model loading, config JSON, Orca `print.process()`, or
G-code export behavior.
