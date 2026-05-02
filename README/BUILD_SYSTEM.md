# Build System

Historical notes in this file may still mention paths under
`_quarantine/root-dependency-dump*`. Those paths are no longer tracked repo
content. For fresh-clone reruns, use checked-in fixtures under
`proof-fixtures/` or `mobileslicer_test_cube.stl` unless a section explicitly
states that it is preserving old evidence.

Use this file in two modes:

* live build/runtime boundary:
  * read from the top through `## Device Testing Workflow`
* preserved build/probe history:
  * treat `## Historical Appendix` and everything below it as archived evidence,
    not default rerun instructions

## Native

* Android NDK
* CMake
* Output: liborca_engine.so
* Android Orca subset output: liborca_core_android.so
* Experimental real Orca Android path: `engine-wrapper/orca-android-libslic3r/`

## Android Subset Stub Inventory

The Android Orca subset still uses a deliberate stub layer under
`engine-wrapper/orca-android-libslic3r/`.

Treat those files by category, not as one undifferentiated cleanup target:

* `android_libslic3r_stubs.cpp`
  * current role: small compatibility shims for utility/helpers that are not a
    current product boundary
  * examples:
    * `header_slic3r_generated()`
    * file-extension helpers such as `is_gcode_file(...)` and `is_json_file(...)`
    * thumbnail-error fallback text for the experimental path
  * current classification:
    * acceptable compatibility shims for now
    * not a known product blocker today
* `android_libslic3r_model_stubs.cpp`
  * current role: explicit feature-gate stubs for import/CAD/model-editing
    surfaces that the Android app does not currently support
  * examples:
    * `3MF`, `OBJ`, `AMF`, `SVG`, and `STEP` import paths
    * mesh-boolean and face-selection helpers
    * backup/save helpers outside the accepted Android model-load boundary
  * current classification:
    * intentional unsupported-feature gates
    * blockers for broader model/import claims
    * `temporary_dir()` is no longer part of this blocker class because runtime
      path configuration now sets it from Android app-private storage
* `android_libslic3r_print_stubs.cpp`
  * current role: mixed print-path compatibility layer
  * acceptable compatibility shims still here:
    * SVG/debug draw helpers
    * timelapse helpers
    * some thumbnail/logging helpers
  * calibration parity note:
    * Pressure Advance helpers are no longer stubbed here; the Android print
      target now links Orca's real `vendor/orcaslicer/src/libslic3r/calib.cpp`
      so `CalibPressureAdvance::find_optimal_PA_speed`,
      `CalibPressureAdvanceLine`, and `CalibPressureAdvancePattern` use the
      upstream implementation.
  * known remaining product blocker here:
    * `WipeTower` / `WipeTower2` are still stubbed, so prime-tower / wipe-tower
      multimaterial behavior is currently blocked on Android even though the app
      now surfaces and transports some multimaterial settings

Stub review rule:

* do not delete stubs blindly
* when a surfaced feature depends on a stubbed subsystem, either:
  * link the real Orca implementation, or
  * keep the feature/documentation honestly classified as blocked or `Config only`
* keep `engine-wrapper/orca-android-libslic3r/stub_inventory.json` aligned with
  the active stub files when adding, removing, or reclassifying Android Orca
  subset stubs
* every Android-linked `*stubs.cpp` file must be represented in the stub
  inventory, and inventory entries must remain referenced by the Android Orca
  CMake target
* run `scripts/verify_android.sh stubs` or `scripts/verify_stub_inventory.py`
  after changing the Android Orca subset stub layer

Current viewer/performance build truth:

* the live tree currently builds a working STL `Prepare` path
* the live tree currently builds and ships the accepted native `libvgcode`
  G-code `Preview` renderer
* the Android wrapper exposes real native G-code viewer entry points backed by
  `libvgcode`
* the accepted Preview interaction/build boundary keeps one active native
  viewer handle, updates visible layer range live during slider drag, and
  performs the expensive filtered reload only once on slider release
* oversized G-code Preview starts with a native count-only layer planning pass;
  Android enables `libvgcode` loading only after it has selected an exact
  layer range under the conservative phone preview planning budget
* Android plans exact G-code Preview windows and native `libvgcode` loading with
  the selected GCODE Preview Performance budget: Low end 400k, Mid range 750k,
  High end 1m. The native hard ceiling remains 1m.
* If a selected Preview range is still rejected as too large, the render thread
  returns native-planned subranges to the workspace so the first safe exact
  range is selected automatically.
* the 2026-04-29 user-confirmed device behavior is that large G-code Preview
  calculates the safe range first and does not surface an oversized full-range
  failure before chunking
* single-nozzle multicolor tower handling now has two safeguards before G-code
  export: Android auto-arrange reserves a conservative tower keepout, and the
  native wrapper relocates or rejects the tower against transformed object
  bounds before slicing
* the rejected full native preview mesh experiment should not be treated as
  established build truth just because it compiled once
* the debug APK intentionally optimizes the shipping native slicer/viewer path:
  * `orca_android_libslic3r_port_config` applies C++ `-O3` and
    `-fomit-frame-pointer`
  * shipping `orca_engine` applies C++ `-O3` and `-fomit-frame-pointer`
  * native `libvgcode` applies C++ `-O3` and `-fomit-frame-pointer`
  * this is required because current on-device performance testing uses the
    debug APK on `RFCYA01ANVE`

## Android App

* Gradle
* Kotlin
* Jetpack Compose
* Path: `android-app/`
* Debug build: `./gradlew :app:assembleDebug`
* Perf debug build: `./gradlew :app:assemblePerfDebug`
  * output: `android-app/app/build/outputs/apk/perfDebug/app-perfDebug.apk`
  * intended for phone-side performance checks with debug signing and JNI debugging disabled
* Generated Android sources:
  * Orca setting metadata is generated by
    `scripts/generate_orca_setting_metadata.py`
  * Gradle writes it under
    `android-app/app/build/generated/source/orcaSettingMetadata/`
  * do not check in `GeneratedOrcaSettingMetadata.kt` under `src/main`
* Project-local Android SDK copy: `.android-sdk/`
* Project-local adb entrypoint: `tools/adb`

## Target

* `arm64-v8a` for the production Android release target
* `x86_64` only for explicit local desktop Android validation when real x86_64
  libslic3r dependencies are staged

## Validation Environment

* Local development/runtime validation on this PC is still performed in installed Waydroid (`x86_64`) for isolated probing
* Shipping-stack runtime boundaries are validated on `RFCYA01ANVE` (`arm64-v8a`) for milestones that are proven, with fresh rebuilds still required at each boundary
* Waydroid remains for narrow iterative checks only and does not replace shipping ARM proof for runtime milestones
* Current large-model slice-time honesty boundary:
  * accepted Benchy timing truth still comes only from the phone path on `RFCYA01ANVE`
  * older Waydroid `x86_64` runs were classification/smoke only while they
    produced the known non-comparable tiny artifact:
    * `29215 bytes`
    * `1691 lines`
    * `11-13 ms`
    * no `orca_slice:` wrapper timing lines
    * no native timing breakdown lines
* Future coders should use the in-project `.android-sdk` copy for builds and Android tooling rather than any external or Trash-backed SDK path
* Runtime-affecting milestone changes should be tested at the boundary they target:
  * local `x86_64` probe boundary → Waydroid
  * shipping `arm64-v8a` boundary → `RFCYA01ANVE`
* Gradle now passes `ORCA_SHIPPING_REQUIRE_REAL_LIBSLIC3R=ON` and
  `ORCA_SHIPPING_ALLOW_REDUCED_WRAPPER=OFF` into CMake:
  * Android app builds fail fast if the real libslic3r wrapper cannot be configured
  * `x86_64` is not packaged by default; use
    `-PmobileSlicer.includeX86_64Abi=true` only with staged real x86_64
    dependencies
  * reduced-wrapper builds require an explicit CMake opt-in and are
    non-shipping probes only
* The current staged-boundary evidence now extends beyond link closure and the first runtime crash boundary: the shipping app launches on `RFCYA01ANVE`, loads models, completes slice, reaches export, emits `.gcode` artifacts, those artifacts are content-proven to be real-wrapper / Orca-style output rather than reduced-backend synthetic output, and the current matrix now distinguishes legitimate default-path slicer rejection(s) from successful accepted-fixture runs.

## Device Testing Workflow

Use this split unless a task explicitly says otherwise:

* coder:
  * builds the app
  * may use `tools/adb` only to install/push the updated app to the target device when the task requires device delivery
  * should use `scripts/verify_android.sh install [serial]` or `scripts/verify_android.sh device [serial]` for delivery-only device verification
  * must not use `tools/adb` for UI automation, taps, dumps, log pulls, runtime probing, or other device-side interaction unless the user explicitly changes that rule
  * must report honestly what was verified directly without device automation
* user:
  * performs hands-on app functionality testing on-device when requested
* handoff rule:
  * coder must clearly separate:
    * build/install proof
    * local non-device verification
    * user-required functional validation still pending
* milestone proof rule for app viewer/workspace work:
  * source review, compile success, and APK install are not enough to claim viewer/runtime success
  * if the user reports that the viewer still fails or the workspace still does not feel full-screen on device, that user report is the source of truth and the milestone remains open
* bounded verification-tooling rule:
  * the current debug app also contains a bounded automation action for one-variable slice-proof workflows on `RFCYA01ANVE`
  * that path exists to make exported G-code comparisons repeatable
  * it must be treated as opt-in per run, not as a standing override of the default delivery-only `tools/adb` rule
  * `scripts/verify_android.sh profile-ui`, `scripts/verify_android.sh benchy`, and `scripts/verify_android.sh device-automation` require `MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1`

## Slice-Time Audit Boundary

Use the existing Android automation path plus wrapper stage logs for bounded slice-time checks:

2026-04-24 superseding performance note:

* the old Benchy `59931 ms` / `62261 ms` audit numbers below were captured
  before the optimized native debug build
* the current debug APK now applies `-O3 -fomit-frame-pointer` to the shipping
  native slicer path
* generated ARM compile commands were checked after rebuild and include those
  flags for `orca_engine`, `orca_wrapper.cpp`, and
  `orca_android_libslic3r_print_gcode_lib`
* the follow-up Preview optimization now applies the same flags to native
  `libvgcode`; generated ARM compile commands show them on `GCodeInputData.cpp`,
  `Layers.cpp`, and `ViewerImpl.cpp`
* user-confirmed current result on `RFCYA01ANVE`:
  * Benchy-class slice path dropped from about `59s` to about `4s`
  * STL render also became much faster
* app-visible timing split later showed the Benchy-class pipeline was no longer
  dominated by native slicing:
  * before summary cleanup: `Slice 1687 ms`, `G-code 6 ms`, `Summary 4520 ms`,
    `Total 6221 ms`
  * after summary cleanup: `Slice 1814 ms`, `G-code 7 ms`, `Summary 682 ms`,
    `Total 2513 ms`
  * estimated time was then restored with a manual G-code word scanner instead
    of the old regex movement parser
* future slice-time work must collect fresh post-optimization wrapper stage
  logs before optimizing against the old unoptimized-debug baseline

* app hot path:
  * `sliceCurrentModel(...)`
  * `nativeSetConfigJson`
  * `nativeSlice`
  * `nativeWriteGcodeToFile`
  * `nativeGetGcodeSummary`
* native wrapper hot path:
  * `config`
  * `prepare_model`
  * `apply_validate`
  * `process`
  * `export_gcode`
  * `read_cleanup`
* accepted 2026-04-22 audit baseline on `RFCYA01ANVE`:
  * small `mobileslicer_test_cube.stl`:
    * `config 39 ms`
    * `prepare_model 1 ms`
    * `apply_validate 7 ms`
    * `process 97 ms`
    * `export_gcode 515 ms`
    * `read_cleanup 20 ms`
    * automation elapsed `707 ms`
    * dominant stage: `export_gcode`
  * medium `stage2_small_perimeter_array_fixture.stl`:
    * `config 39 ms`
    * `prepare_model 1 ms`
    * `apply_validate 7 ms`
    * `process 385 ms`
    * `export_gcode 2143 ms`
    * `read_cleanup 64 ms`
    * automation elapsed `2653 ms`
    * dominant stage: `export_gcode`
  * larger `3DBenchy.stl`:
    * `config 31 ms`
    * `prepare_model 18 ms`
    * `apply_validate 926 ms`
    * `process 32893 ms`
    * `export_gcode 25785 ms`
    * `read_cleanup 249 ms`
    * automation elapsed `59931 ms`
    * dominant stage: `process`, with `export_gcode` still materially large
* exact audit conclusion:
  * wrapper JSON/config setup is too small to justify a first-pass optimization claim
  * a bounded export/readback reduction attempt did not move total slice time meaningfully on the same fixtures and was reverted
  * the accepted Benchy-native probe now pushes that conclusion one step deeper:
    * exact fixture path: `/data/local/tmp/3DBenchy.stl`
    * exact app-owned config source is the automation-echoed baseline JSON on `RFCYA01ANVE`
    * exact deeper rerun timings: `33 / 20 / 952 / 33821 / 27151 / 255 ms`, automation elapsed `62261 ms`
    * exact dominant internal `process` bucket: `infill 26386 ms`
    * exact largest measured subphases inside that bucket: `process_external_surfaces 10665 ms`, `bridge_over_infill 8367 ms`, `make_fills 5331 ms`
    * exact next large-model slice-time lane should stay inside those measured native subphases, with the heavy Orca export path still the secondary measured lane
  * the narrower follow-up on the same accepted Benchy path stayed measurement-only and did not keep code:
    * exact rerun wrapper timings: `31 / 18 / 931 / 32841 / 26814 / 247 ms`, automation elapsed `60917 ms`
    * exact rerun `process_external_surfaces` timing: `10459 ms`
    * exact narrowed hotspot inside that subphase:
      * `layer=1`
      * `printZ=0.4`
      * `bridge=8`
      * `bridgesMs=10348`
      * total per-call `10373 ms`
    * honest conclusion from that finer probe:
      * the cost is not generic empty-layer overhead
      * no small safe optimization was justified enough to keep from this run alone
  * exact current classification follow-up after exhausting the rejected lower-hull pre-check and the redundant SliceBeam-inspired `clip_fill_surfaces()` guard:
    * this run stayed source-review / classification only
    * no native behavior change was attempted
    * exact Orca-only Arachne ordering difference now carried forward:
      * SliceBeam/Prusa uses a simpler `Arachne::PerimeterOrder::ordered_perimeter_extrusions(...)` path before traversal
      * Orca adds a common custom scheduler in `vendor/orcaslicer/src/libslic3r/PerimeterGenerator.cpp` that:
        * flattens `all_extrusions`
        * builds dependency maps
        * calls `Arachne::WallToolPaths::getRegionOrder(...)`
        * repeatedly rebuilds and sorts `available_candidates`
        * scans candidates against `current_position` to choose the next start
    * exact adjacent Orca-only wall-sequence path:
      * `findAllTouchingPerimeters(...)`
      * `reorderPerimetersByProximity(...)`
      * `bringContoursToFront(...)`
      * this branch is guarded by `wall_sequence == InnerOuterInner`
    * exact classification decision:
      * the next bounded suspect is the common custom ordering scheduler, not the `InnerOuterInner`-only proximity helpers
      * this is the safer first target because it is materially absent or simpler in SliceBeam and is more likely to run on the accepted default Benchy path
  * exact bounded common-ordering instrumentation follow-up:
    * this run added temporary probes only inside the common ordering block in `vendor/orcaslicer/src/libslic3r/PerimeterGenerator.cpp`
    * exact intended sub-breakdown:
      * `getRegionOrder(...)`
      * dependency construction / constraint application
      * `available_candidates` rebuild and sort
      * nearest-candidate scan against `current_position`
    * exact exploratory validation path used:
      * built `:app:assembleDebug`
      * installed the debug APK to connected Waydroid `x86_64`
      * verified the Waydroid copy of `/data/local/tmp/3DBenchy.stl` still matched accepted hash `6ab57f1c3f8e86bc3cbd302c6fa6270acf06277c6335454e922419c25d42e97e`
      * pushed the accepted config JSON to `/data/local/tmp/benchy_accepted_config.json`
      * launched the existing automation action with `CONFIG=$(cat /data/local/tmp/benchy_accepted_config.json)`
    * exact exploratory result:
      * status artifact echoed the accepted config JSON intact
      * output stayed the same non-comparable tiny artifact:
        * `29215 bytes`
        * `1691 lines`
        * `13 ms`
      * even direct Android-native probe logging did not surface usable ordering sub-breakdown lines on this Waydroid path
    * exact honest conclusion:
      * this run did not produce a usable runtime timing split for the common ordering block
      * the only accepted timing truth still available for this lane is the prior phone-side `orderingMs 21` on the dominant first-layer surface, versus `wallPathsMs 517`
      * if this lane continues, the best bounded suspect remains the nearest-candidate scan, but that is still source-structured suspicion rather than accepted measured proof
  * the next bounded follow-up on that same Benchy path did land a kept `bridge_over_infill` win:
    * exact hotspot classification:
      * `gather_candidates` dominated at `7898 ms` out of `8198 ms`
      * deeper gather measurement showed the real cost was lower-layer unsupported-area preparation, not candidate filtering:
        * `lowerLayerPrepMs 9485 ms`
        * `candidateFilterMs 192 ms`
    * exact kept optimization:
      * skip the expensive lower-layer unsupported-area preparation when the current layer has no `stInternalSolid`
      * also skip it when the lower layer contains no bridgeable sparse/void infill
    * exact before baseline remains:
      * `31 / 18 / 926 / 32893 / 25785 / 249 ms`, automation elapsed `59931 ms`
      * `bridge_over_infill 8367 ms`
    * exact kept-code measured run with bridge probes:
      * `33 / 19 / 957 / 26229 / 27474 / 261 ms`, automation elapsed `55011 ms`
      * `bridge_over_infill 515 ms`
    * exact final clean verification rerun without temporary bridge probes:
      * `config 31 ms`
      * `prepare_model 19 ms`
      * `apply_validate 948 ms`
      * `process 25692 ms`
      * final `export_gcode + read_cleanup` combined tail `27065 ms`
      * automation elapsed `53755 ms`
      * `bridge_over_infill 505 ms`
      * neighboring rerun subphases:
        * `process_external_surfaces 10648 ms`
        * `make_fills 5370 ms`
        * `infill 18435 ms`
        * `make_perimeters 6754 ms`
    * output-drift truth:
      * the earlier kept-code comparison against the accepted Benchy baseline proved stripped executable G-code equality on the same optimization path
      * the final clean verification rerun kept the same output byte count `3479325`
  * exact bounded `make_fills` follow-up on the same accepted Benchy path:
    * exact hotspot classification from the accepted measurement-only reruns:
      * one early layer still dominates `make_fills`
      * exact dominant layer summary:
        * `layer=1`
        * `printZ=0.4`
        * `group_fills 1332 ms`
        * dominant `fill_surface` call: `pattern=ipMonotonic`, `fillMs 3062 ms`, `gapMs 0 ms`
        * inside `group_fills`, the remaining cost is mainly `narrowSplitMs 858 ms` plus `clipMs 467 ms`
    * exact measured probe reruns:
      * wrapper `31 / 17 / 933 / 25316 / 26553 / 221 ms`, automation elapsed `53102 ms`, `make_fills 5260 ms`
      * wrapper `32 / 18 / 937 / 26518 / 26778 / 235 ms`, automation elapsed `54548 ms`, `make_fills 5421 ms`
    * honest outcome:
      * the remaining cost is real core geometry work, not cheap setup or gap-fill waste
      * no small safe optimization was directly justified enough to keep
      * no code was landed from this run
  * exact bounded device-resource-utilization audit on the same accepted Benchy path:
    * exact rerun wrapper timings:
      * `31 / 18 / 929 / 25546 / 26135 / 246 ms`
      * automation elapsed `52939 ms`
    * exact capture method:
      * reused the accepted cold-start automation path on `/data/local/tmp/3DBenchy.stl`
      * captured wrapper/native stage timing from `MobileSlicerNative` logcat plus the automation status artifact
      * sampled device resources externally during the run from:
        * `/proc/stat`
        * `/proc/<pid>/status`
        * `/proc/<pid>/task`
        * `top -H -b -n 1 -p <pid>`
        * `/sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq`
        * `dumpsys thermalservice`
        * post-run `dumpsys meminfo com.mobileslicer`
    * exact resource findings:
      * active thread count grew from `23` to `29`, then settled around `28`
      * heavy snapshots showed `7-8` hot `DefaultDispatch` worker threads at roughly `92-103%` each
      * sampled total CPU reached about `727-781%` out of `800%`
      * sampled mid-run per-core deltas showed one interval with effectively no additional idle ticks across all `8` CPUs
      * hot intervals also held near-max frequencies:
        * little/mid cluster up to `3532800`
        * prime cluster up to `4473600`
      * thermal status stayed `0` throughout the audited run
      * app memory stayed moderate:
        * `VmRSS` rose from about `201868 kB` to about `288700 kB`
        * post-run `TOTAL RSS` was `263336 kB`
        * post-run `TOTAL PSS` was `166147 kB`
        * post-run `TOTAL SWAP PSS` was only `83 kB`
    * honest outcome:
      * the accepted Benchy slice path is not obviously underusing the phone
      * there is no strong evidence that CPU parallelism, thermal throttling, or app memory pressure is currently the primary limiter
      * the next bounded native optimization target from the measured list should stay `make_perimeters`
  * exact bounded `make_perimeters` follow-up on the same accepted Benchy path:
    * same fixture path and same automation-echoed baseline JSON were reused
    * exact probe rerun wrapper timings:
      * `31 / 18 / 928 / 25529 / 28340 / 235 ms`
      * automation elapsed `55082 ms`
    * exact hotspot classification:
      * all measured perimeter work on this path used `algorithm=arachne`
      * the dominant perimeter layer was the first layer:
        * `layer=0`
        * `printZ=0.2`
        * total `673 ms`
      * the dominant measured sub-hotspot inside that layer was one first-layer Arachne surface:
        * `surface=0`
        * `wallPathsMs 517`
        * `orderingMs 21`
        * `traverseMs 5`
        * `fillBoundaryMs 83`
        * total `653 ms`
        * `loops=2`
        * `extrusions=13`
      * a secondary heavy band appeared around hull/body layers near `49-59`, but those layers stayed much smaller at roughly `302-360 ms`
    * honest outcome:
      * the remaining `make_perimeters` cost is dominated by Arachne wall-path generation, not `process_no_bridge`, ordering, or infill-boundary cleanup
      * no small safe optimization was directly justified enough to keep
      * the temporary perimeter probes were reverted and no code was landed from this run
    * next bounded recommendation:
      * if the slice-time lane continues, stay inside `make_perimeters` and narrow first into the first-layer `Arachne::WallToolPaths` hotspot instead of broadening back out to wrapper cleanup
  * exact bounded first-layer `Arachne::WallToolPaths` optimization on 2026-04-23:
    * exact accepted config source used for the keep/revert decision:
      * the automation-echoed baseline JSON on `RFCYA01ANVE`
    * exact measured hotspot summary from the probe reruns:
      * the dominant first-layer surface paid the same-outline Arachne wall-path setup twice because `only_one_wall_top` first tried a one-wall probe and then fell back to the full-wall run
      * exact before probe split on `layer=0 surface=0`:
        * one-wall probe `223 ms`
        * fallback full-wall run `230 ms`
      * exact narrow kept change:
        * skip that one-wall probe when the current island is already fully covered by `upper_slices`, because it cannot produce any top-surface area and only repeats the expensive wall-path setup
      * exact after probe split on the same accepted Benchy path:
        * the redundant one-wall probe disappeared
        * the remaining full-wall run stayed `230 ms`
    * exact before/after total timing comparison:
      * accepted current-build anchor before the change:
        * wrapper `34 / 20 / 943 / 25825 / 26336 / 235 ms`
        * automation elapsed `53440 ms`
      * optimized probe rerun:
        * wrapper `32 / 18 / 927 / 25306 / 26373 / 215 ms`
        * automation elapsed `52903 ms`
      * final clean kept-code rerun without probe logs:
        * wrapper `31 / 19 / 940 / 25192 / 25696 / 246 ms`
        * automation elapsed `52160 ms`
    * exact neighboring native timing changes:
      * accepted earlier `make_perimeters 6737 ms`
      * optimized probe rerun `make_perimeters 6308 ms`
      * final clean rerun `make_perimeters 6365 ms`
      * final clean neighboring rerun:
        * `process_external_surfaces 10616 ms`
        * `bridge_over_infill 507 ms`
        * `make_fills 5338 ms`
        * `process 25192 ms`
    * exact output-drift result:
      * raw full-file bytes changed only in early comment bytes while byte count stayed `3479325`
      * stripped executable-body SHA stayed exactly `a88313f832523c94f62d691c5464940da3aaad9e9b6ac6ea44dfae2b81d1e8ed`
    * exact honest conclusion:
      * this optimization produced a real total-time Benchy win on the accepted path
      * the change was kept
  * exact bounded `49-59` `make_perimeters` follow-up on the same accepted Benchy path:
    * exact accepted config source used:
      * the automation-echoed baseline JSON on `RFCYA01ANVE`
    * exact probe-only rerun timings:
      * wrapper `31 / 20 / 944 / 25447 / 26913 / 236 ms`
      * automation elapsed `53621 ms`
      * native `make_perimeters 6392 ms`
    * exact hotspot summary from the temporary band probes:
      * per-layer totals across the band stayed:
        * `49=335 ms`
        * `50=341 ms`
        * `51=327 ms`
        * `52=372 ms`
        * `53=337 ms`
        * `54=316 ms`
        * `55=302 ms`
        * `56=297 ms`
        * `57=292 ms`
        * `58=325 ms`
        * `59=309 ms`
      * lower hull `49-51` still pays repeated same-outline fallback work on the dominant `surface=0` path:
        * `layer=49`: `226 ms` total, `95 ms` one-wall probe, `97 ms` fallback full-wall rerun, `topExpolygonsEmpty=1`
        * `layer=50`: `229 ms` total, `95 ms` one-wall probe, `97 ms` fallback full-wall rerun, `topExpolygonsEmpty=1`
        * `layer=51`: `227 ms` total, `95 ms` one-wall probe, `99 ms` fallback full-wall rerun, `topExpolygonsEmpty=1`
      * middle / upper hull-body layers are mostly different work, not the same-outline rerun:
        * `layer=52 surface=0`: `260 ms` total, `96 ms` primary one-wall pass, `105 ms` inner-wall pass on different `not_top` polygons
        * comparable `ranInnerWall=1` splits continue on `53` and `56-59`
      * a smaller secondary fallback remained on `surface=2` for `54-59`, but only at about `50-56 ms`
    * exact clean reverted-source verification after removing the probes:
      * wrapper `31 / 18 / 934 / 24959 / 26304 / 242 ms`
      * automation elapsed `52522 ms`
      * native `make_perimeters 6301 ms`
      * neighboring `infill 18166 ms`
    * exact output-drift result for the probe build and the clean reverted rerun:
      * both outputs stayed `3479325` bytes
      * raw full-file bytes drifted
      * stripped executable-body compare still returned equal
    * exact honest conclusion:
      * this run identified a real duplicate-work sub-band, but only for lower hull `49-51`
      * no safe small optimization was justified enough to keep
      * the temporary band probes were reverted and no code was landed
  * exact bounded lower-hull `49-51` keep-or-revert trial on the same accepted Benchy path:
    * exact current-source baseline before the trial:
      * wrapper `31 / 17 / 929 / 24921 / 26519 / 238 ms`
      * wrapper total `52658 ms`
      * automation elapsed `52699 ms`
      * native `make_perimeters 6209 ms`
    * exact cheap pre-check tested:
      * skip the one-wall-top probe when a virtual second perimeter from `offset_ex(last, -double(ext_perimeter_spacing))` is already fully covered by `upper_slices_clipped`
    * exact lower-hull after timings from the temporary validation run:
      * `layer=49 surface=0`: `candidateSecondPerimeterCovered=1`, `primaryWallMs=98`, `fallbackWallMs=0`
      * `layer=50 surface=0`: `candidateSecondPerimeterCovered=1`, `primaryWallMs=100`, `fallbackWallMs=0`
      * `layer=51 surface=0`: `candidateSecondPerimeterCovered=1`, `primaryWallMs=111`, `fallbackWallMs=0`
    * exact trial rerun timings:
      * wrapper `31 / 18 / 940 / 24697 / 26622 / 248 ms`
      * wrapper total `52558 ms`
      * automation elapsed `52587 ms`
      * native `make_perimeters 5920 ms`
    * exact output result:
      * bytes changed from `3479325` to `3479283`
      * stripped executable-body compare failed
      * `cmp -s` returned `1`
    * exact honest conclusion:
      * the candidate did remove the targeted fallback and lowered the hotspot
      * it changed executable G-code behavior, so it was reverted and not kept
    * exact clean reverted-source verification:
      * wrapper `31 / 18 / 950 / 25666 / 26707 / 237 ms`
      * wrapper total `53612 ms`
      * automation elapsed `53640 ms`
      * native `make_perimeters 6499 ms`
      * stripped executable-body compare returned equal again after revert
  * exact bounded SliceBeam-informed prep-chain follow-up:
    * exact chosen hypothesis:
      * skip the `clip_fill_surfaces()` call site when `PrintObject::infill_only_where_needed` is false
    * exact reason this was the safest SliceBeam-informed suspect:
      * SliceBeam disables `clip_fill_surfaces()` outright
      * Orca already documents that pass as conditional
      * this was a local pruning check, not a reorder of `discover_horizontal_shells()` and `process_external_surfaces()`
    * exact source-review result:
      * `PrintObject::clip_fill_surfaces()` already starts with `if (! PrintObject::infill_only_where_needed) return;`
      * the accepted Benchy config leaves that mode off
      * no honest code change was justified because the proposed guard duplicated existing behavior
    * exact Waydroid verification method:
      * started local Waydroid session
      * connected local adb transport to `192.168.240.112:5555`
      * installed `android-app/app/build/outputs/apk/debug/app-debug.apk`
      * pushed `/home/peanut/Downloads/3DBenchy.stl` to `/data/local/tmp/3DBenchy.stl`
      * verified Waydroid fixture SHA:
        * `6ab57f1c3f8e86bc3cbd302c6fa6270acf06277c6335454e922419c25d42e97e`
      * pushed `/tmp/benchy_accepted_config.json` and launched the existing automation action with `CONFIG=$(cat ...)`
    * exact Waydroid benchmark result:
      * output bytes `29215`
      * output lines `1691`
      * automation elapsed `11 ms`
      * artifact head showed only a tiny rectangle-like path, not a full Benchy toolpath
      * no `orca_slice:` or `process_breakdown` timing lines were emitted
    * exact honest conclusion:
      * current Waydroid `x86_64` runtime does not reproduce the accepted large-model MobileSlicer Benchy slice path honestly enough for timing comparison
      * no code was attempted and no timing claim was accepted from this run
  * exact bounded 2026-04-23 comparative audit against SliceBeam:
    * external comparison target reviewed:
      * GitHub repo: `https://github.com/utkabobr/SliceBeam`
      * Play listing: `https://play.google.com/store/apps/details?id=ru.ytkab0bp.slicebeam`
    * exact visible external build/core differences:
      * SliceBeam describes itself publicly as PrusaSlicer-core and Prusa-profile based
      * SliceBeam's `app/CMakeLists.txt` explicitly enables `-fopenmp -static-openmp`, requires `OpenMP`, and also links `TBB::tbb`
      * SliceBeam also sets `NDEBUG` in that Android native build
    * exact current local MobileSlicer comparison truth:
      * MobileSlicer's current Orca Android path already uses oneTBB and `NDEBUG`
      * the reviewed local Android app/native build path does not show OpenMP enabled
      * MobileSlicer still exports G-code to a file and then reads it back in `engine-wrapper/orca_wrapper.cpp`
    * honest comparison conclusion:
      * the largest likely speed gap is not a small app-wrapper trick
      * the strongest plausible external levers are:
        * different slicer core/config surface
        * explicit Android OpenMP parallel build
        * different preset/default workload
      * Java-versus-Kotlin app code is weak speculation compared with the already measured native `process` costs
    * next bounded build-system recommendation from that audit:
      * before going deeper into Arachne-core micro-probes, take one narrow current-Orca Android native build lane:
        * measure whether enabling OpenMP safely on MobileSlicer's existing Android native path yields a real Benchy win
      * keep that lane bounded to build/runtime configuration on the existing Orca path, not a core swap or architecture rewrite
  * exact bounded 2026-04-23 OpenMP comparison attempt:
    * current reviewed shipping Android path already links oneTBB and defines `NDEBUG`, but did not visibly enable OpenMP before the attempted comparison
    * the smallest reversible comparison change was to pass a build toggle from `android-app/app/build.gradle.kts` into `engine-wrapper/orca-android-libslic3r/CMakeLists.txt` and link `OpenMP::OpenMP_CXX` only on the shipping Orca libslic3r target path
    * exact local build proof from that attempt:
      * direct arm64 CMake configure on the real shipping build tree succeeded with `OpenMP 5.0`
      * generated arm64 compile commands and ninja rules showed:
        * `-fopenmp=libomp`
        * `ORCA_ANDROID_OPENMP_ENABLED=1`
      * direct arm64 ninja build completed and produced an OpenMP-linked `liborca_engine.so`
    * exact blocker:
      * this sandboxed host stopped allowing the socket operations required by both Gradle daemon startup and `adb`
      * exact failures included Gradle `java.net.SocketException: Operation not permitted` and `adb` `could not install *smartsocket* listener: Operation not permitted`
    * honest result:
      * local compile proof exists
      * no accepted device-side Benchy comparison exists from this run
      * the temporary build-toggle change was reverted
  * exact completed OpenMP comparison on 2026-04-23:
    * exact current-build rerun on `RFCYA01ANVE`:
      * wrapper `34 / 20 / 943 / 25825 / 26336 / 235 ms`
      * automation elapsed `53440 ms`
    * exact OpenMP-enabled rerun on the same accepted Benchy config:
      * wrapper `34 / 20 / 968 / 25633 / 26706 / 236 ms`
      * automation elapsed `53652 ms`
    * exact comparison build change used:
      * the same bounded Gradle-to-CMake OpenMP toggle path proved earlier
      * the successful comparison artifact came from a normal Gradle `:app:assembleDebug` build with OpenMP forced on in the local comparison tree so Android handled stripping and native-lib packaging correctly
    * exact output-drift result:
      * raw full-file SHA changed
      * stripped executable-body SHA using `grep -v '^;'` matched exactly
      * byte count stayed exactly `3479325`
    * exact honest conclusion:
      * OpenMP did not produce a real total-time Benchy win on the accepted Orca path
      * the temporary build change was reverted

## Dirty Tree Guidance

The repo may contain a large amount of generated build dirt under:

* `engine-wrapper/orca-android-libslic3r/android-deps-build/openssl-src`

Use this rule when resuming or reviewing:

* treat that generated OpenSSL tree as background build noise unless the current task directly targets dependency staging or OpenSSL build behavior
* do not let that generated dirt obscure the real active boundary in Android app, wrapper, or supervisor docs
* local repo-hygiene cleanup has now started:
  * the generated dependency-build tree is being removed from Git tracking
  * `.gitignore` now covers `engine-wrapper/orca-android-libslic3r/android-deps-build/`
  * until that cleanup is committed, expect one large staged delete block in `git status`
  * that staged delete block is intentional cleanup-in-progress, not a new runtime/build blocker

## Current Runtime Summary

Use this section for the short build/runtime environment truth. Keep detailed run history in `README/CHANGELOG.md`.

* Current successful classified shipping ARM fixtures:
  * `aa_matrix_tetra_20mm.stl`
  * `ac_matrix_cube.stl`
  * `est.stl`
  * `obj_1_Tassen Regal lang.stl`
  * `ad_pig_real.stl`
  * `ae_sphere_real.stl`
  * `af_goliath.stl`
  * `ag_vz330_bed.stl`
* Current repeatable exact-fixture cases:
  * `aa_matrix_tetra_20mm.stl`
  * `est.stl`
  * `ae_sphere_real.stl`
  * `obj_1_Tassen Regal lang.stl`
  * `af_goliath.stl`
  * `ag_vz330_bed.stl`
* Current legitimate default-path rejection:
  * `ab_matrix_tetra_small.stl`
* Immediate next runtime boundary:
  * keep speculative upstream-core probing paused
  * keep `ad_pig_real.stl` classified honestly as succeeds/emits output but not repeatable for this exact fixture
* the current compact Stage 2 classification on `RFCYA01ANVE` is:
  * Printer:
    * `bed dimensions` -> `Error-state only`
    * `nozzle_diameter` -> `Device tested`
    * `filament_diameter` -> `Device tested`
  * Filament:
    * `filament_type` -> `Config only`
    * `filament_max_volumetric_speed` -> `Device tested`
    * `first_layer_nozzle_temperature` -> `Start-sequence only`
    * `other_layers_nozzle_temperature` -> `Layer-change command only`
    * `first_layer_bed_temperature` -> `Start-sequence only`
    * `other_layers_bed_temperature` -> `Layer-change command only`
    * `cooling_baseline` -> `Fan-command only`
    * `no_cooling_for_first_x_layers` -> `Fan-command only`
  * Process:
    * `layer_height` -> `Device tested`
    * `first_layer_height` -> `Device tested`
    * `first_layer_print_speed` -> `Device tested`
    * `top_shell_layers` -> `Device tested`
    * `bottom_shell_layers` -> `Device tested`
    * `seam_position` -> `Device tested`
    * `precise_outer_wall` -> `Device tested`
    * `top_surface_pattern` -> `Stronger-fixture proven`
    * `wall_loops` -> `Stronger-fixture proven`
    * `skirts` -> `Device tested`
    * `brim_width` -> `Device tested`
    * `sparse_infill_density` -> `Device tested`
    * `sparse_infill_pattern` -> `Device tested`
* current special-case reminders:
  * `bed dimensions` stay `Error-state only`: they reach Orca as centered `printable_area` / `printable_height`, and the fresh typed-extra rerun in this review on `RFCYA01ANVE` used `/data/local/tmp/mobileslicer_test_cube.stl` staged to `/data/user/0/com.mobileslicer/cache/selected-model-mobileslicer_test_cube.stl`; baseline exported `/sdcard/Download/stage2_beddims_baseline.gcode`, while the `10 x 10 x 10 mm` variant failed before export with `nativeError=printable volume exceeded via fallback check areaExceeded=1 heightExceeded=1 offendingLine=43 gcode="G1 X17.14 Y2.86 E.8648 ; perimeter"`
  * `filament_type` remains `Config only` as Orca `filament_type`; the current path does not load a type-specific preset bundle from that field alone
  * `first_layer_print_speed` is now `Device tested`; the current path already wrote Orca `initial_layer_speed`, and the accepted `mobileslicer_test_cube.stl` `100 -> 10 mm/s` proof on `RFCYA01ANVE` changes emitted first-layer feedrates while preserving the same geometry after feedrate stripping
  * `initial_layer_infill_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `75 -> 15 mm/s` proof on `RFCYA01ANVE` changes first-layer `Bottom surface` feedrate from `G1 F4500` to `G1 F900` while feedrate-stripped motion stays equal
  * `initial_layer_travel_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `50% -> 100%` proof on `RFCYA01ANVE` changes first-layer travel feedrate from `G1 ... F3600` to `G1 ... F7200` while feedrate-stripped motion stays equal
  * `slow_down_layers` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `0 -> 3` proof on `RFCYA01ANVE` changes emitted early-layer feedrates above layer 1, including second-layer `Inner wall` / `Outer wall` `G1 F6000 -> F3200`
  * the bounded per-feature speed subset is now `Device tested` on `RFCYA01ANVE`:
    * `outer_wall_speed 100 -> 30` changes emitted `;TYPE:Outer wall` feedrates from `G1 F6000` to `G1 F1800` while normalized motion stays equal
    * `inner_wall_speed 100 -> 30` changes emitted `;TYPE:Inner wall` feedrates on the same cube fixture while normalized motion stays equal after removing feedrate-only lines
    * `top_surface_speed 100 -> 20` changes emitted `;TYPE:Top surface` feedrates from `G1 F6000` to `G1 F1200` while feedrate-stripped motion stays equal
    * `travel_speed 120 -> 240` changes emitted non-first-layer travel feedrates from `G1 ... F7200` to `G1 ... F14400` while feedrate-stripped motion stays equal
  * bounded bridge / small-perimeter lane after the stronger small-feature reruns:
    * `bridge_speed` is now `Device tested`; the accepted `stage2_bridge_speed_fixture.stl` `10 -> 40 mm/s` rerun on `RFCYA01ANVE` changes emitted `;TYPE:Overhang wall` and `;TYPE:Bridge` feedrates from `G1 F600` to `G1 F1807` while feedrate-stripped motion stays equal
    * `small_perimeter_threshold` is now `Device tested`; the accepted `stage2_small_perimeter_array_fixture.stl` `0 -> 20 mm` rerun on `RFCYA01ANVE` activates emitted small-feature perimeter handling and changes `Inner wall` / `Outer wall` feedrates from `G1 F6000` to `G1 F600`
    * `small_perimeter_speed` is now `Device tested`; with the threshold path active on that same stronger fixture, the accepted `10 -> 50 mm/s` rerun changes emitted small-feature perimeter feedrates from `G1 F600` to `G1 F3000`
  * bounded infill / internal-solid / gap-infill lane after the accepted reruns:
    * `sparse_infill_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `20 -> 80 mm/s` rerun on `RFCYA01ANVE` changes emitted `;TYPE:Sparse infill` feedrates from `G1 F1200` to `G1 F3822` while feedrate-stripped motion stays equal
    * `internal_solid_infill_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `20 -> 80 mm/s` rerun on `RFCYA01ANVE` changes emitted `;TYPE:Internal solid infill` feedrates from `G1 F1200` to `G1 F4800` while feedrate-stripped motion stays equal
    * `gap_infill_speed` remains `Config-labeling-only effect`; the dedicated `stage2_gap_infill_strip_fixture.stl` `10 -> 60 mm/s` rerun changes only `; gap_infill_speed = ...`, and the narrower `stage2_gap_infill_narrow_strip_fixture.stl` probe still does not emit a gap-infill block
  * bounded wall / top-surface / sparse-infill acceleration lane after the accepted reruns:
    * `outer_wall_acceleration` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `500 -> 1500 mm/s²` rerun on `RFCYA01ANVE` removes the emitted `M204 S500` reset before `;TYPE:Outer wall` blocks while acceleration-stripped motion stays equal
    * `inner_wall_acceleration` is now `Device tested`; the accepted stronger `stage2_inner_wall_acceleration_tall_box_fixture.stl` `1000 -> 500 mm/s²` rerun on `RFCYA01ANVE` changes emitted `M204` immediately before `;TYPE:Inner wall` from `M204 S1000` to `M204 S500` while acceleration-stripped motion stays equal
    * `top_surface_acceleration` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `500 -> 2000 mm/s²` rerun changes emitted `;TYPE:Top surface` acceleration handling from `M204 S500` to `M204 S1500` while acceleration-stripped motion stays equal
    * `sparse_infill_acceleration` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `500 -> 1500 mm/s²` rerun changes emitted `;TYPE:Sparse infill` acceleration handling from `M204 S500` to `M204 S1500` while acceleration-stripped motion stays equal
  * `filament_max_volumetric_speed` is now `Device tested`; the accepted gyroid `mobileslicer_test_cube.stl` rerun on `RFCYA01ANVE` with fixed high-speed Process overrides changes emitted non-first-layer feedrates from capped `G1 F8843.491` to `G1 F15000` while feedrate-stripped motion stays equal
  * `filament_flow_ratio` is now explicit app-owned filament wiring:
    * Android stores it in `FilamentProfile.flowRatio`
    * active slice JSON now emits `filament_flow_ratio`
    * `engine-wrapper/orca_wrapper.cpp` now forwards that float with `config.set_deserialize_strict("filament_flow_ratio", ...)`
    * formal status is `Config only - Waydroid`
  * the smallest honest Orca pressure-advance lane is now explicit app-owned wiring:
    * Android stores it as:
      * `FilamentProfile.pressureAdvanceEnabled`
      * `FilamentProfile.pressureAdvance`
    * active slice JSON now emits:
      * `enable_pressure_advance`
      * `pressure_advance`
    * `engine-wrapper/orca_wrapper.cpp` now forwards them with:
      * `config.set_deserialize_strict("enable_pressure_advance", "1" or "0")`
      * `config.set_deserialize_strict("pressure_advance", ...)`
    * formal status for both fields is `Config only - Waydroid`
  * `filament_adaptive_volumetric_speed` is now explicit app-owned adaptive-lane wiring:
    * Android stores it in `FilamentProfile.adaptiveVolumetricSpeedEnabled`
    * active slice JSON now emits `filament_adaptive_volumetric_speed`
    * `engine-wrapper/orca_wrapper.cpp` now forwards that bool with `config.set_deserialize_strict("filament_adaptive_volumetric_speed", "1" or "0")`
    * formal status is `Config only - Waydroid`
  * `volumetric_speed_coefficients` is now explicit app-owned prerequisite wiring:
    * Android stores it in `FilamentProfile.volumetricSpeedCoefficients`
    * active slice JSON now emits `volumetric_speed_coefficients`
    * `engine-wrapper/orca_wrapper.cpp` now forwards that string with `config.set_deserialize_strict("volumetric_speed_coefficients", ...)`
    * formal status is `Config only - Waydroid`
  * the smallest honest Orca filament retraction lane is now explicit app-owned wiring as a nullable subset:
    * Android stores it as:
      * `FilamentProfile.retractionLengthMm`
      * `FilamentProfile.retractionSpeedMmPerSec`
      * `FilamentProfile.deretractionSpeedMmPerSec`
    * active slice JSON now emits:
      * `filament_retraction_length`
      * `filament_retraction_speed`
      * `filament_deretraction_speed`
    * emission stays conditional on explicit app values so blank still inherits printer / extruder defaults
    * `engine-wrapper/orca_wrapper.cpp` now forwards them with:
      * `config.set_deserialize_strict("filament_retraction_length", ...)`
      * `config.set_deserialize_strict("filament_retraction_speed", ...)`
      * `config.set_deserialize_strict("filament_deretraction_speed", ...)`
    * explicit `0` for `filament_deretraction_speed` intentionally preserves Orca's same-as-retraction-speed meaning
    * formal status for all three fields is `Config only - Waydroid`
  * exact exploratory Waydroid validation from the completed owned filament subset:
    * local build succeeded with `./gradlew :app:assembleDebug`
    * the debug APK was installed to Waydroid `192.168.240.112:5555`
    * the accepted Benchy fixture hash in Waydroid still matched `6ab57f1c3f8e86bc3cbd302c6fa6270acf06277c6335454e922419c25d42e97e`
    * the pushed compact config JSON included:
      * `filament_flow_ratio = 0.95`
      * `enable_pressure_advance = true`
      * `pressure_advance = 0.03`
      * `volumetric_speed_coefficients = "1 2 3 4 5 6"`
      * `filament_adaptive_volumetric_speed = true`
      * `filament_retraction_length = 0.8`
      * `filament_retraction_speed = 30`
      * `filament_deretraction_speed = 0`
    * the automation status artifact echoed that compact JSON back intact
    * the emitted Waydroid artifact still stayed the known non-comparable tiny output:
      * `29215 bytes`
      * `1691 lines`
      * `14 ms`
    * honest boundary:
      * this confirms the owned filament flow, pressure-advance, adaptive, and retraction subset survives app -> JSON -> wrapper transport only
      * this does not upgrade any of those fields to `Device tested`
  * `no_cooling_for_first_x_layers` is now `Fan-command only`; the bounded `mobileslicer_test_cube.stl` `1 -> 3` proof on `RFCYA01ANVE` keeps motion equal but delays the first emitted `M106 S255 ; enable fan`
  * `wall_loops` remain `Stronger-fixture proven`; the exact reproduced missing-wall cube on `RFCYA01ANVE` is now fixed on the real export path, but that blocker came from wrapper default `wall_generator = arachne`, not missing `wall_loops` transport. The one-variable `wall_loops` proof itself still comes from stronger fixtures such as `Box_v2.stl`
  * `precise_outer_wall` is now `Device tested`; the cached `selected-model-wall_smoke_box_20mm.stl` `true -> false` proof on `RFCYA01ANVE` changed byte count, line count, `;TYPE:` sequence, stripped executable output, and motion-only body while keeping setup/start commands equal
  * `top_surface_pattern` remains `Stronger-fixture proven`, but the accepted proof is now the flatter `mobileslicer_test_cube.stl` rerun on `RFCYA01ANVE`, not the older overclaimed `selected-model-100ml_cube.stl` pair; `monotonicline -> concentric` still changes stripped executable output and motion-only body while setup/start commands stay equal, and the narrower missing-final-`Top surface` bug on that concentric fixture is now fixed by linking the real Android concentric/Arachne wall-toolpath stack; remaining drift is now inside the restored final top-surface block rather than a missing block
  * `only_one_wall_top` is now explicit app-owned Stage 2 `Process` coverage rather than a hidden wrapper default:
    * app mapping path:
      * `ProcessProfile.onlyOneWallTopSurfaces`
      * `automation_only_one_wall_top`
      * config JSON `only_one_wall_top`
      * wrapper `apply_json_overrides(...) -> config.set_deserialize_strict("only_one_wall_top", "1" or "0")`
    * wrapper cleanup:
      * the old unconditional `config.set_deserialize_strict("only_one_wall_top", "1")` force was removed from the generic path
      * built-in process defaults stay aligned with the previously accepted tuned behavior by defaulting the new app field to `true`
    * accepted real-device proof on `RFCYA01ANVE` with `mobileslicer_test_cube.stl`:
      * source model path: `/data/local/tmp/mobileslicer_test_cube.stl`
      * staged model path: `/data/user/0/com.mobileslicer/cache/selected-model-mobileslicer_test_cube.stl`
      * device artifacts:
        * `/sdcard/Download/stage2_only_one_wall_top_true_20260422.gcode`
        * `/sdcard/Download/stage2_only_one_wall_top_false_20260422.gcode`
      * host artifacts:
        * `/tmp/mobileslicer-proof/top-surface-onewall/20260422/stage2_only_one_wall_top_true_android.gcode`
        * `/tmp/mobileslicer-proof/top-surface-onewall/20260422/stage2_only_one_wall_top_false_android.gcode`
      * byte/line counts:
        * `true`: `232103` bytes, `7469` lines
        * `false`: `231937` bytes, `7462` lines
      * stripped executable-body counts:
        * `true`: `183944` bytes, `5769` lines
        * `false`: `183761` bytes, `5761` lines
      * motion-only counts:
        * `true`: `162380` bytes, `5108` lines
        * `false`: `162362` bytes, `5105` lines
      * final `;TYPE:` tail comparison:
        * `true`: `Outer wall -> Top surface -> Custom`
        * `false`: `Inner wall -> Outer wall -> Top surface -> Custom`
      * concrete roof-region diff example:
        * `true` goes straight from the last outer wall into `;TYPE:Top surface`
        * `false` inserts a final `;TYPE:Inner wall` ring first, beginning `G1 X.654 Y19.346 E.63273 ; perimeter`
    * classification:
      * `only_one_wall_top` is now `Stronger-fixture proven`
      * this is accepted app-owned roof-picture tuning on the bounded flat-roof Android fixture, not a same-config Orca parity claim
  * the isolated accepted Stage 2 settings/proof tranche now has a clean local build boundary again:
    * restored local ARM dependency staging:
      * `/tmp/orca-deps-install/arm64-boost-cgal/lib/libboost_filesystem.a`
      * `/tmp/orca-deps-install/arm64-boost-headers/include/boost/...`
      * `/tmp/orca-deps-install/arm64-tbb-static2/lib/libtbb.a`
      * `/tmp/orca-deps-install/arm64-tbb-static2/lib/libtbbmalloc.a`
      * `/tmp/orca-deps-install/arm64-gmp/lib/libgmp.a`
      * `/tmp/orca-deps-install/arm64-gmp/lib/libgmpxx.a`
      * `/tmp/orca-deps-install/arm64-mpfr/lib/libmpfr.a`
      * `/tmp/orca-deps-install/arm64-cgal/include/CGAL/...`
      * `/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a`
      * `/tmp/orca-deps-install/arm64-openssl/lib/libssl.a`
      * `/tmp/orca-deps-src/boost-1.84.0`
    * exact rebuild command:
      * `cd /home/peanut/Development/MobileSlicer/android-app && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk GRADLE_USER_HOME=/tmp/gradle-cache ./gradlew :app:assembleDebug`
    * exact rebuild result:
      * `:app:compileDebugKotlin` stayed green
      * arm64 native compile/link/archive completed after dependency staging was restored
      * x86_64 native build also completed
      * full debug APK build succeeded at `:app:assembleDebug`
    * honesty boundary:
      * this is build-only closure for the accepted Stage 2 tranche, not fresh runtime or device proof
  * current wall-fix reproducibility caveat: the successful exact-cube rerun used the app-cached STL path under `/data/user/0/com.mobileslicer/cache/...`; direct external `/sdcard/Download/...` absolute-path loading was not the reliable rerun path on that pass because `nativeLoadModel` rejected it
  * current proof-harness caveat from this review: raw `automation_config_json` shell-intent injection still does not count unless the device status artifact echoes the full JSON back intact; the accepted fresh top-surface rerun used shell-escaped compact JSON and verified that echoed config before counting the proof
  * `top_shell_layers` and `bottom_shell_layers` are now `Device tested`; bounded `mobileslicer_test_cube.stl` `4 -> 6` and `3 -> 5` proofs on `RFCYA01ANVE` changed stripped executable output and motion-only body while keeping setup and non-motion layer-change comparisons equal
  * `seam_position` is now `Device tested`; the latest user-confirmed on-device result on `RFCYA01ANVE` is the accepted authority for current Stage 2 runtime truth
  * `skirts` are now `Device tested`; current `mobileslicer_test_cube.stl` `0 -> 3` proof on `RFCYA01ANVE` adds emitted skirt toolpaths because the wrapper now maps the app field to Orca `skirt_loops`
  * `brim_width` is now `Device tested`; app mapping to Orca `brim_width` was already correct, the bounded wrapper fix now switches `brim_type` between `no_brim` and `outer_only`, and the Android print subset now links real Orca `Brim.cpp` instead of the stubbed `make_brim(...)`
  * accepted rebuilt Android proof on `RFCYA01ANVE` over `/data/local/tmp/mobileslicer_test_cube.stl` staged to `/data/user/0/com.mobileslicer/cache/selected-model-mobileslicer_test_cube.stl`:
    * baseline config JSON kept `brim_width:0`
    * variant config JSON kept `brim_width:4`
    * device artifacts:
      * `/sdcard/Download/stage2_brim_fix_width_0_20260422.gcode`
      * `/sdcard/Download/stage2_brim_fix_width_4_20260422.gcode`
    * host artifacts:
      * `/tmp/mobileslicer-proof/brim-width/20260422/stage2_brim_fix_width_0_android.gcode`
      * `/tmp/mobileslicer-proof/brim-width/20260422/stage2_brim_fix_width_4_android.gcode`
    * full outputs: `230934` bytes / `7425` lines vs `240018` bytes / `7714` lines
    * stripped executable bodies: `182601` bytes / `5727` lines vs `191859` bytes / `6014` lines
    * motion-only bodies: `161037` bytes / `5066` lines vs `170295` bytes / `5353` lines
    * first-layer `;TYPE:` sequence changes from `Custom -> Inner wall -> Outer wall -> Bottom surface` to `Custom -> Brim -> Inner wall -> Outer wall -> Bottom surface`
  * `sparse_infill_pattern` is now `Device tested`; the accepted fixed `mobileslicer_test_cube.stl` matrix on `RFCYA01ANVE` emits real sparse infill for `Grid`, `Gyroid`, and `Cubic` after the Android print subset was switched from the no-op `FillGyroid` stub to the real `FillGyroid.cpp`
  * sparse infill pattern coverage stays on the same app-side broader Orca enum list without changing the existing JSON or wrapper path:
    * `ProcessProfile.sparseInfillPattern` still serializes the Orca config value directly
    * active slice JSON still emits `sparse_infill_pattern`
    * `engine-wrapper/orca_wrapper.cpp` still forwards that string unchanged
    * all app-surfaced sparse infill pattern modes are now `Device tested`:
      * `rectilinear`
      * `alignedrectilinear`
      * `zigzag`
      * `crosszag`
      * `lockedzag`
      * `line`
      * `grid`
      * `triangles`
      * `tri-hexagon`
      * `cubic`
      * `adaptivecubic`
      * `quartercubic`
      * `supportcubic`
      * `lightning`
      * `honeycomb`
      * `3dhoneycomb`
      * `lateral-honeycomb`
      * `lateral-lattice`
      * `crosshatch`
      * `tpmsd`
      * `tpmsfk`
      * `gyroid`
      * `concentric`
      * `hilbertcurve`
      * `archimedeanchords`
      * `octagramspiral`
  * bounded Android sparse-infill-pattern fix for the previously failing mode families:
    * exact root cause:
      * the app already surfaced and forwarded the sparse infill pattern strings correctly
      * the Android libslic3r print subset still linked no-op stub implementations for multiple fill families instead of the real Orca sources
    * exact implementation:
      * added the real Orca Android-subset sources for:
        * `Fill3DHoneycomb.cpp`
        * `FillAdaptive.cpp`
        * `FillHoneycomb.cpp`
        * `FillLightning.cpp`
        * `FillLine.cpp`
        * `FillPlanePath.cpp`
        * `FillTpmsD.cpp`
        * `FillTpmsFK.cpp`
        * `Fill/Lightning/DistanceField.cpp`
        * `Fill/Lightning/Generator.cpp`
        * `Fill/Lightning/Layer.cpp`
        * `Fill/Lightning/TreeNode.cpp`
      * removed the overlapping no-op fill-family bodies from `android_libslic3r_print_stubs.cpp`
    * exact local verification:
      * `cd android-app && ./gradlew :app:assembleDebug`
      * `BUILD SUCCESSFUL`
      * fresh debug APK installed to `RFCYA01ANVE`
    * current accepted closure:
      * the local Android-subset fix is now backed by fresh user-confirmed phone validation for the remaining formerly failing modes
      * the broader sparse infill pattern enum is no longer split between proven and config-only buckets
  * accepted bounded `alignedrectilinear` phone proof on `RFCYA01ANVE` over `/data/local/tmp/mobileslicer_test_cube.stl` staged to `/data/user/0/com.mobileslicer/cache/selected-model-mobileslicer_test_cube.stl`:
    * exact method:
      * rebuilt `:app:assembleDebug`
      * installed the fresh debug APK to `RFCYA01ANVE`
      * used cold-start `ACTION_AUTOMATE_SLICE` twice with duplicated active printer / filament / process profiles
      * kept `sparse_infill_density = 15` fixed and changed only `automation_sparse_infill_pattern` from `rectilinear` to `alignedrectilinear`
    * exact device artifacts:
      * `/sdcard/Download/stage2_sparse_infill_pattern_rectilinear_20260424.gcode`
      * `/sdcard/Download/stage2_sparse_infill_pattern_alignedrectilinear_20260424.gcode`
    * exact host artifacts:
      * `/tmp/mobileslicer-proof/sparse-infill-pattern-alignedrectilinear/20260424/stage2_sparse_infill_pattern_rectilinear_android.gcode`
      * `/tmp/mobileslicer-proof/sparse-infill-pattern-alignedrectilinear/20260424/stage2_sparse_infill_pattern_alignedrectilinear_android.gcode`
      * `/tmp/mobileslicer-proof/sparse-infill-pattern-alignedrectilinear/20260424/stage2_sparse_infill_pattern_rectilinear_android.status.txt`
      * `/tmp/mobileslicer-proof/sparse-infill-pattern-alignedrectilinear/20260424/stage2_sparse_infill_pattern_alignedrectilinear_android.status.txt`
    * exact emitted-config confirmation:
      * status artifacts echoed `sparse_infill_density = 15` in both runs
      * G-code comments emitted `; sparse_infill_pattern = rectilinear` vs `; sparse_infill_pattern = alignedrectilinear`
    * exact output comparison:
      * full outputs: `216244 bytes / 6858 lines` vs `218614 bytes / 6901 lines`
      * stripped executable bodies: `102226 bytes / 5152 lines` vs `103345 bytes / 5195 lines`
      * motion-only bodies: `94993 bytes / 4497 lines` vs `96112 bytes / 4540 lines`
      * feedrate-stripped motion bodies: `89866 bytes / 4497 lines` vs `90991 bytes / 4540 lines`
      * concrete diff example:
        * `rectilinear`: `G1 X19 Y16.351 E.13557 ; infill`
        * `alignedrectilinear`: `G1 X16.351 Y1 E.13557 ; infill`
  * bounded Orca-parity sanity check:
    * current tiny matrix over `mobileslicer_test_cube.stl` now shows `baseline`, `skirts`, and `layer_height` matching the reference Orca side for `;TYPE:` sequence, stripped executable body, motion-only body, setup-command body, and end-command body
    * source review confirmed the earlier bounded setup/end drifts came from the wrapper seeding `machine_start_gcode` and `machine_end_gcode` out of `DynamicPrintConfig::full_print_config()` while the reference probe blanked those fields
    * the shipping wrapper now blanks both seeded fields before applying JSON overrides, which removes those bounded custom-block mismatches on this tiny matrix only
* current delivered Profiles-UI truth boundary on `RFCYA01ANVE`:
  * the debug APK installed from current local source visibly shows those same compact per-setting truth labels inside the `Printer`, `Filament`, and `Process` sections
  * the current `Filament` editor also shows `No cooling for first X layers` in `Cooling`, and the current `Process` editor shows `Initial layer height`, `Top shell layers`, and `Bottom shell layers` in `Quality`
* current workspace/result-surface truth boundary on `RFCYA01ANVE`:
  * the workspace still shows the imported STL mesh only
  * it does not preview emitted wall/shell toolpaths yet, and the current result copy now says that explicitly

## Current Nondeterminism Boundary

* Strongest verified source-level cause boundary:
  * `vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * bridge-over-infill candidate surfaces are gathered in parallel via `tbb::concurrent_vector<CandidateSurface>`
  * that unstable insertion order then feeds later order-sensitive bridge expansion / reuse logic
* Minimal local fix now applied:
  * deterministic lexical sort of gathered `CandidateSurface` entries before `surfaces_by_layer` is populated
* Exact verification status:
  * patch is now runtime-verified on the shipping debug real-wrapper path
  * restored minimum ARM staging includes:
    * `/tmp/orca-deps-install/arm64-boost-cgal/lib/libboost_filesystem.a`
    * `/tmp/orca-deps-install/arm64-boost-headers/include/boost/...`
    * `/tmp/orca-deps-install/arm64-tbb-static2/lib/libtbb.a`
    * `/tmp/orca-deps-install/arm64-gmp/lib/libgmp.a`
    * `/tmp/orca-deps-install/arm64-mpfr/lib/libmpfr.a`
    * `/tmp/orca-deps-install/arm64-cgal/include/CGAL/...`
    * `/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a`
    * `/tmp/orca-deps-src/boost-1.84.0`
  * restored debug rebuild command:
    * `cd /home/peanut/Development/MobileSlicer/android-app && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk GRADLE_USER_HOME=/tmp/gradle-cache ./gradlew :app:assembleDebug --no-daemon --stacktrace`
  * restored debug install command:
    * `./tools/adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`
  * patched rerun artifact:
    * `/tmp/mobileslicer-ag-vz330-patch/selected-model-ag_vz330_bed.gcode`
    * `sha256=339ff16ff88fa070ac5fbac2c4ed6565d92d31cbac61f7d056a665ead3f4da97`
    * `51476131` bytes
    * `1389963` lines
* Current honest claim:
  * `ag_vz330_bed.stl` historical preserved runs still include pre-fix executable-body drift
  * the local deterministic-order patch is now verified on the proven shipping debug real-wrapper path for this exact fixture
  * patched rerun versus preserved `run2` differs only by the generated-at header line
  * comment-stripped patched rerun versus preserved `run2` matches exactly at `sha256=ea932dd7b15091eb755c7695e71b50d5c8d2aaf09dba3fbd9dd51611f306e97f`
  * `ag_vz330_bed.stl` is now honestly repeatable for this exact fixture only

## Current ad_pig Boundary

* Latest preserved-evidence investigation and one deeper minimal rerun probe stayed on the shipping ARM path only:
  * target device: `RFCYA01ANVE`
  * target fixture: `ad_pig_real.stl`
* Exact evidence reused first:
  * `/tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode`
  * `/tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.gcode`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-*.xml`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-logcat-app.txt`
* Exact minimal probes now tested:
  * `vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * later per-layer candidate ordering in `PrintObject::bridge_over_infill()` now falls back to `candidate_surface_less`
  * overlap-based bridge-angle reuse in `PrintObject::bridge_over_infill()` now chooses the reused prior surface deterministically instead of taking the first overlap encountered
  * post-`diff()` `expansion_area` mutation in `PrintObject::bridge_over_infill()` now sorts the resulting polygons deterministically before later candidates consume that mutated state
  * `limiting_area = union_(area_to_be_bridge, expansion_area)` in `PrintObject::bridge_over_infill()` now sorts the resulting polygons deterministically before anchor-boundary construction and later clipping consume that geometry
  * clipped `bridging_area = intersection(bridging_area, limiting_area)` in `PrintObject::bridge_over_infill()` now sorts the resulting polygons deterministically before later clipping and subtraction consume that geometry
  * rebuilt shipping debug app:
    * `cd /home/peanut/Development/MobileSlicer/android-app && ./gradlew :app:assembleDebug --no-daemon --stacktrace`
  * reinstalled shipping debug app:
    * `./tools/adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`
  * exact rebuild blocker repairs needed before that build succeeded:
    * reinstalled arm64 GMP headers into `/tmp/orca-deps-install/arm64-gmp`
    * rebuilt arm64 MPFR into `/tmp/orca-deps-install/arm64-mpfr`
* Exact current classification:
  * succeeds
  * emits output
  * not repeatable for this exact fixture
  * executable-body drift
  * likely upstream-core involved
  * exact root cause unknown
  * probing paused
* Exact drift shape verified from preserved artifacts:
  * full-file `diff -u` hunk count: `106`
  * stripped `diff -u` hunk count: `99`
  * no changed perimeter lines were found
  * drift is concentrated in `Sparse infill`, `Bridge`, `Internal Bridge`, `Internal solid infill`, and some downstream `Top surface` sections
  * sorted executable-body comparison still differs, so the drift is not just final output ordering
* Current source-level status:
  * `vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * `PrintObject::bridge_over_infill()` was the strongest reviewed local suspect branch
  * that branch is now exhausted
  * no replacement bounded source-level probe is honestly justified right now
  * current evidence supports likely upstream-core involvement, but does not identify an exact root cause or a known upstream fix
* Exact deeper probed rerun result:
  * artifact:
    * `/tmp/mobileslicer-ad-pig-probe3/selected-model-ad_pig_real.gcode`
    * device save path: `/sdcard/selected-model-ad_pig_real.gcode`
    * `sha256=2f895c917c6a775f041624c28a24ab718c367df33e734efe6eba47b252b5ddcd`
    * `7120468` bytes
    * `218135` lines
  * timings:
    * `sliceCurrentModel:end ... elapsedMs=26235 sliced=true`
  * exact comparison:
    * baseline versus deeper probe full-file hunk count: `88`
    * baseline versus deeper probe stripped hunk count: `80`
    * preserved rerun versus deeper probe full-file hunk count: `67`
    * preserved rerun versus deeper probe stripped hunk count: `63`
    * prior tie-break probe versus deeper probe full-file hunk count: `99`
    * prior tie-break probe versus deeper probe stripped hunk count: `93`
    * sorted stripped hashes still differ across baseline, preserved rerun, tie-break probe, and deeper probe
* Exact `expansion_area` probed rerun result:
  * artifact:
    * `/tmp/mobileslicer-ad-pig-probe4/selected-model-ad_pig_real.gcode`
    * device save path: `/sdcard/selected-model-ad_pig_real.gcode`
    * `sha256=9cfc2a4c3fc6deb20c49fbd3f53158526f51ab49a8edbb8a3eee7d5f54e2604f`
    * `7120344` bytes
    * `218129` lines
  * timings:
    * `sliceCurrentModel:end ... elapsedMs=28146 sliced=true`
  * exact comparison:
    * baseline versus `expansion_area` probe full-file hunk count: `98`
    * baseline versus `expansion_area` probe stripped hunk count: `1`
    * preserved rerun versus `expansion_area` probe full-file hunk count: `47`
    * preserved rerun versus `expansion_area` probe stripped hunk count: `42`
    * tie-break probe versus `expansion_area` probe full-file hunk count: `93`
    * tie-break probe versus `expansion_area` probe stripped hunk count: `87`
    * deeper probe versus `expansion_area` probe full-file hunk count: `96`
    * deeper probe versus `expansion_area` probe stripped hunk count: `91`
    * sorted stripped hashes still differ across baseline, preserved rerun, tie-break probe, deeper probe, and `expansion_area` probe
  * exact observed save location:
    * export dialog stayed at device root `Niko's S25 Ultra`
    * emitted artifact path remained `/sdcard/selected-model-ad_pig_real.gcode`
* Exact `limiting_area` probed rerun result:
  * artifact:
    * `/tmp/mobileslicer-ad-pig-probe5/selected-model-ad_pig_real.gcode`
    * device save path: `/sdcard/selected-model-ad_pig_real.gcode`
    * `sha256=b8f496a422ad6563ae55f99b6b341ef156826514cb6acfa805d479da8765afed`
    * `7121900` bytes
    * `218175` lines
  * timings:
    * `sliceCurrentModel:end ... elapsedMs=27303 sliced=true`
  * exact comparison:
    * baseline versus `limiting_area` probe full-file hunk count: `54`
    * baseline versus `limiting_area` probe stripped hunk count: `51`
    * preserved rerun versus `limiting_area` probe full-file hunk count: `141`
    * preserved rerun versus `limiting_area` probe stripped hunk count: `135`
    * tie-break probe versus `limiting_area` probe full-file hunk count: `148`
    * tie-break probe versus `limiting_area` probe stripped hunk count: `142`
    * deeper probe versus `limiting_area` probe full-file hunk count: `120`
    * deeper probe versus `limiting_area` probe stripped hunk count: `111`
    * `expansion_area` probe versus `limiting_area` probe full-file hunk count: `138`
    * `expansion_area` probe versus `limiting_area` probe stripped hunk count: `128`
    * sorted stripped hashes still differ across baseline, preserved rerun, tie-break probe, deeper probe, `expansion_area` probe, and `limiting_area` probe
  * exact observed save location:
    * export dialog stayed at device root `Niko's S25 Ultra`
    * emitted artifact path remained `/sdcard/selected-model-ad_pig_real.gcode`
* Exact clipped `bridging_area` probed rerun result:
  * artifact:
    * `/tmp/mobileslicer-ad-pig-probe6/selected-model-ad_pig_real.gcode`
    * device save path: `/sdcard/selected-model-ad_pig_real.gcode`
    * `sha256=201310b112887ad31227b7725db61b074d76d34ad9bbf59d8e4b90b71d4e681d`
    * stripped `sha256=dd48b3969851a336741cc34013139e61286344246ada025596faaad9bd1fa949`
    * sorted stripped `sha256=f12f34800525826e8bc39442caeea0a29c3c9df70ea19e2c02ec8de6175a70a4`
    * `7121892` bytes
    * `218176` lines
  * timings:
    * `sliceCurrentModel:end ... elapsedMs=28551 sliced=true`
  * exact comparison:
    * baseline versus clipped `bridging_area` probe full-file hunk count: `40`
    * baseline versus clipped `bridging_area` probe stripped hunk count: `38`
    * preserved rerun versus clipped `bridging_area` probe full-file hunk count: `127`
    * preserved rerun versus clipped `bridging_area` probe stripped hunk count: `120`
    * tie-break probe versus clipped `bridging_area` probe full-file hunk count: `124`
    * tie-break probe versus clipped `bridging_area` probe stripped hunk count: `119`
    * deeper probe versus clipped `bridging_area` probe full-file hunk count: `107`
    * deeper probe versus clipped `bridging_area` probe stripped hunk count: `99`
    * `expansion_area` probe versus clipped `bridging_area` probe full-file hunk count: `126`
    * `expansion_area` probe versus clipped `bridging_area` probe stripped hunk count: `117`
    * `limiting_area` probe versus clipped `bridging_area` probe full-file hunk count: `33`
    * `limiting_area` probe versus clipped `bridging_area` probe stripped hunk count: `30`
    * sorted stripped comparison still differs across all seven preserved outputs
  * exact observed save location:
    * export dialog stayed at device root `Niko's S25 Ultra`
    * emitted artifact path remained `/sdcard/selected-model-ad_pig_real.gcode`
* Honest next step:
  * if a patch is attempted next, keep it to one minimal follow-up probe around the subtraction / clipping behavior itself after the now-tested ordering normalizations and the two now-tested `union_safety_offset(bridging_area)` canonicalizations, prove it first on the clean-head non-interactive control path, and only promote if the control result materially improves rather than regresses

## Historical Appendix

Everything below this heading is preserved build/probe history. Keep it for
auditability and past evidence, but do not treat it as the primary current
build boundary ahead of the live sections above and `CURRENT_STATUS.md`.

## Current Upstream Control Boundary

* Exact clean-head upstream control path used:
  * detached clean-head worktree: `/tmp/mobileslicer-upstream-control`
  * commit: `343772d6`
  * source path: `engine-wrapper/orca-android-libslic3r/`
  * exact target: `orca_android_libslic3r_print_gcode_probe`
* Exact fixture used:
  * `_quarantine/root-dependency-dump/data/meshes/pig.stl`
  * `sha256=584a6e2684053f4112865544115b60a8b3efb66917312db6608d9a152cf30406`
* Exact nearest-equivalent config used:
  * `/tmp/mobileslicer-upstream-control-artifacts/ad_pig_upstream_control_config.json`
  * `layer_height=0.2`
  * `first_layer_height=0.2`
  * `nozzle_diameter=0.4`
  * `filament_diameter=1.75`
  * `bed_temperature=45`
  * `print_temperature=200`
  * `travel_speed=120`
  * `perimeters=2`
* Exact unavoidable mismatch from the shipping MobileSlicer run:
  * shipping real-wrapper `ad_pig_real.stl` uses empty JSON overrides on top of `DynamicPrintConfig::full_print_config()`
  * the experimental print probe requires explicit keys and seeds a fuller Orca config state than the shipping wrapper can prove it used
  * this is therefore a nearest-equivalent config control, not exact same config
* Exact configure/build commands used:
  * `git worktree add --detach /tmp/mobileslicer-upstream-control 343772d6`
  * `cmake -S /tmp/mobileslicer-upstream-control/engine-wrapper/orca-android-libslic3r -B /tmp/orca-android-libslic3r-build-print-arm64-control -DCMAKE_TOOLCHAIN_FILE=/home/peanut/Development/MobileSlicer/.android-sdk/ndk/26.3.11579264/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DORCA_ANDROID_LIBSLIC3R_ENABLE=ON -DORCA_ANDROID_BOOST_PREFIX=/tmp/orca-deps-install/arm64-boost-cgal -DORCA_ANDROID_BOOST_HEADERS_DIR=/tmp/orca-deps-install/arm64-boost-headers/include -DORCA_ANDROID_TBB_PREFIX=/tmp/orca-deps-install/arm64-tbb-static2 -DORCA_ANDROID_GMP_PREFIX=/tmp/orca-deps-install/arm64-gmp -DORCA_ANDROID_MPFR_PREFIX=/tmp/orca-deps-install/arm64-mpfr -DORCA_ANDROID_CGAL_PREFIX=/tmp/orca-deps-install/arm64-cgal -DORCA_ANDROID_OPENSSL_INCLUDE_DIR=/tmp/orca-deps-install/arm64-openssl/include -DORCA_ANDROID_OPENSSL_CRYPTO_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a -DORCA_ANDROID_OPENSSL_SSL_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libssl.a -DORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0`
  * `cmake --build /tmp/orca-android-libslic3r-build-print-arm64-control --target orca_android_libslic3r_print_gcode_probe -j8`
* Exact device/runtime commands used:
  * `./tools/adb -s RFCYA01ANVE push /tmp/orca-android-libslic3r-build-print-arm64-control/orca_android_libslic3r_print_gcode_probe /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
  * `./tools/adb -s RFCYA01ANVE push _quarantine/root-dependency-dump/data/meshes/pig.stl /data/local/tmp/ad_pig_real_control.stl`
  * `./tools/adb -s RFCYA01ANVE push /tmp/mobileslicer-upstream-control-artifacts/ad_pig_upstream_control_config.json /data/local/tmp/ad_pig_upstream_control_config.json`
  * `./tools/adb -s RFCYA01ANVE shell chmod 755 /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
  * repeated three times:
    * `./tools/adb -s RFCYA01ANVE shell rm -f /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
    * `./tools/adb -s RFCYA01ANVE shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/ad_pig_real_control.stl /data/local/tmp/ad_pig_upstream_control_config.json`
    * `./tools/adb -s RFCYA01ANVE pull /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode /tmp/mobileslicer-upstream-control-runs/runN/ad_pig_upstream_control.gcode`
* Exact preserved control outputs:
  * run1: `/tmp/mobileslicer-upstream-control-runs/run1/ad_pig_upstream_control.gcode`
    * full `sha256=ff0ea99c1759bcc3ec95f5df39c9a90b802d59d25de502a42c85cacb88076a1d`
    * stripped `sha256=f5690dc388e69fd45d16a194c438694eec4ef3a05dc3f0f288e0e1e732a44858`
    * sorted stripped `sha256=9fd2dec1506474d63d15dd36ca39a540ce1e33c3d88fdc45335a8e41319e1134`
    * `4634704` bytes
    * `218120` lines
  * run2: `/tmp/mobileslicer-upstream-control-runs/run2/ad_pig_upstream_control.gcode`
    * full `sha256=f1e23d819e596dda25673d89f93c5fb49dc837b7c9a73f0957b2a26d2826fe64`
    * stripped `sha256=58a5151a9e753ee56d5136f0cf19ed326b4b7f5431f03039800cbda46a1c3294`
    * sorted stripped `sha256=00c12af5c57e28c5b66a8496ad1c370ecc8a95a4c137d76391163e2fc7181b85`
    * `4634583` bytes
    * `218122` lines
  * run3: `/tmp/mobileslicer-upstream-control-runs/run3/ad_pig_upstream_control.gcode`
    * full `sha256=7dd1b6737c7f6bdaff6635bc434e16572ec71f9429303dbf0f32f168ebdb5c86`
    * stripped `sha256=f2096fd84a668feaad2e32333e050f9ce848ce1c5a8e46b0ffb04b33a0ddac53`
    * sorted stripped `sha256=e20fd6eb042f709969ec29aca4c506a1997c8e3163b245ceb4917a72efb05e7a`
    * `4635366` bytes
    * `218157` lines
* Exact repeated-run comparison:
  * run1 versus run2 full `diff -u` hunks: `87`
  * run1 versus run2 stripped `diff -u` hunks: `82`
  * run1 versus run3 full `diff -u` hunks: `127`
  * run1 versus run3 stripped `diff -u` hunks: `117`
  * run2 versus run3 full `diff -u` hunks: `131`
  * run2 versus run3 stripped `diff -u` hunks: `120`
  * sorted stripped hashes still differ across all three control outputs
* Exact control classification:
  * upstream control test
  * same or nearest-equivalent config: nearest-equivalent
  * upstream also drifts
  * executable-body drift, not header-only drift
  * similar drift shape to shipping MobileSlicer run:
    * `Bridge`
    * `Sparse infill`
    * `Internal Bridge`
    * `Internal solid infill`
    * downstream `Top surface`
  * no perimeter drift found
  * likely MobileSlicer-specific only: not supported by this control result
* Review conclusion after the completed clean-head control series:
  * there is no next bounded nearby upstream-core source probe with stronger support than the exhausted `PrintObject::bridge_over_infill()` branch
  * further local source probes are now speculative and low-value
  * the next project step should be non-probe boundary framing rather than another clean-head patch
* Exact follow-up subtraction/clipping probe now tested on the same clean-head control path:
  * exact file changed:
    * `/tmp/mobileslicer-upstream-control/vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact probe:
    * `bridging_area = union_safety_offset(bridging_area);`
    * inserted immediately after `bridging_area = intersection(bridging_area, limiting_area);`
  * exact rebuild command:
    * `cmake --build /tmp/orca-android-libslic3r-build-print-arm64-control --target orca_android_libslic3r_print_gcode_probe -j8`
  * exact rerun commands:
    * `./tools/adb -s RFCYA01ANVE push /tmp/orca-android-libslic3r-build-print-arm64-control/orca_android_libslic3r_print_gcode_probe /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
    * `./tools/adb -s RFCYA01ANVE shell chmod 755 /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
    * repeated three times:
      * `./tools/adb -s RFCYA01ANVE shell rm -f /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
      * `./tools/adb -s RFCYA01ANVE shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/ad_pig_real_control.stl /data/local/tmp/ad_pig_upstream_control_config.json`
      * `./tools/adb -s RFCYA01ANVE pull /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode /tmp/mobileslicer-upstream-control-runs/probe1/runN/ad_pig_upstream_control_probe1.gcode`
  * exact probe outputs:
    * run1 `/tmp/mobileslicer-upstream-control-runs/probe1/run1/ad_pig_upstream_control_probe1.gcode`
      * full `sha256=4b764e048ff2a85f9b41fee4d743a318759dccb85e1d3e8b1d33187c13231c1c`
      * stripped `sha256=1ee0595f772f6cbc6b45e25c9b506334cc792f88194bad3f44de41d0c2acf4d3`
      * sorted stripped `sha256=cd0dd730815d44d3eca5e2ec16902fa6b8d5b42622e3854b07c0a7d9fb2e9c3a`
      * `4635508` bytes
      * `218161` lines
    * run2 `/tmp/mobileslicer-upstream-control-runs/probe1/run2/ad_pig_upstream_control_probe1.gcode`
      * full `sha256=e353496235138189366d29d721ff2898de4ab670872ee3f3930cc6c01ab0074a`
      * stripped `sha256=94032bcfdcb4b5cd7ecd823b627d9da21c8118d2ab85c496aa9a50f8cb3ef6a8`
      * sorted stripped `sha256=c7d7397f76bbeab1328bef6ab136c2ca23ed3aaa87f46d48badc1090f9929f0c`
      * `4635499` bytes
      * `218160` lines
    * run3 `/tmp/mobileslicer-upstream-control-runs/probe1/run3/ad_pig_upstream_control_probe1.gcode`
      * full `sha256=9fbf0ec9bebd201205b3dd7af2658937f414c822ad63860fa7b17fedaf22f2eb`
      * stripped `sha256=10c450cf45db9b13320192bb050fc9d3550174e0a2faeb08add681853117bb19`
      * sorted stripped `sha256=53737672294a5eb71ae83bf31e9353305e10513577edeab4db750fea7913a256`
      * `4635720` bytes
      * `218172` lines
  * exact repeated-run comparison:
    * run1 versus run2 full `diff -u` hunks: `40`
    * run1 versus run2 stripped `diff -u` hunks: `38`
    * run1 versus run3 full `diff -u` hunks: `58`
    * run1 versus run3 stripped `diff -u` hunks: `56`
    * run2 versus run3 full `diff -u` hunks: `53`
    * run2 versus run3 stripped `diff -u` hunks: `51`
  * exact comparison versus the prior clean-head control:
    * previous stripped hunk counts were `82`, `117`, and `120`
    * the probe reduced those to `38`, `56`, and `51`
    * the new probe outputs still differ from all prior clean-head control outputs after comment stripping
  * exact classification:
    * subtraction/clipping probe
    * drift changed but remains
    * executable-body drift remains
    * no perimeter drift found
    * likely upstream-core involved
    * not proven exact same root cause as the shipping MobileSlicer run
* Exact second different subtraction/clipping follow-up now also tested on the same clean-head control path:
  * exact file changed:
    * `/tmp/mobileslicer-upstream-control/vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact probe:
    * `bridging_area = intersection(bridging_area, limiting_area, ApplySafetyOffset::Yes);`
  * exact rebuild command:
    * `cmake --build /tmp/orca-android-libslic3r-build-print-arm64-control --target orca_android_libslic3r_print_gcode_probe -j8`
  * exact rerun commands:
    * `./tools/adb -s RFCYA01ANVE push /tmp/orca-android-libslic3r-build-print-arm64-control/orca_android_libslic3r_print_gcode_probe /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
    * `./tools/adb -s RFCYA01ANVE shell chmod 755 /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
    * repeated three times:
      * `./tools/adb -s RFCYA01ANVE shell rm -f /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
      * `./tools/adb -s RFCYA01ANVE shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/ad_pig_real_control.stl /data/local/tmp/ad_pig_upstream_control_config.json`
      * `./tools/adb -s RFCYA01ANVE pull /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode /tmp/mobileslicer-upstream-control-runs/probe3/runN/ad_pig_upstream_control_probe3.gcode`
  * exact probe outputs:
    * run1 `/tmp/mobileslicer-upstream-control-runs/probe3/run1/ad_pig_upstream_control_probe3.gcode`
      * full `sha256=7c141cb92bf3291e6fdc7ff8ea1df638f10cd44db3656426011cac6c93d1aed9`
      * stripped `sha256=5b8137561a15da64c09aa5e70d346870c453ecb9326c2d941218263061b3e76e`
      * sorted stripped `sha256=109448e4835ca97e08b9983170d189cbfb357464aaeb4001c0888e1f374e2ae7`
      * `4634418` bytes
      * `218108` lines
    * run2 `/tmp/mobileslicer-upstream-control-runs/probe3/run2/ad_pig_upstream_control_probe3.gcode`
      * full `sha256=b12bd43e663dfe31a3f15961799d648c68b6e746492b5111c4724f1703568a40`
      * stripped `sha256=e0f257911b3a425839ad478fecd5f5f3f3fad8a5c1e0dabbee6b9d09c3129ce6`
      * sorted stripped `sha256=80fb5bfe7c49a9dbe27e86583f7be20c7fdcb3fcdbe6ee9e8f45088d26ebf3bd`
      * `4635243` bytes
      * `218144` lines
    * run3 `/tmp/mobileslicer-upstream-control-runs/probe3/run3/ad_pig_upstream_control_probe3.gcode`
      * full `sha256=a2939b3def384f409cfecf8a48904291afabe4cf9c1ebf6d8ceb5a284b1f4dd1`
      * stripped `sha256=1d1d29d1119a880ecb1877637c7f8af0e50b2484413d22831b6e58bc7d314db3`
      * sorted stripped `sha256=7ce7b858abf4a255e2e9aee2988e627d220485c9dfb2b31ee4468961fd84165e`
      * `4635770` bytes
      * `218174` lines
  * exact repeated-run comparison:
    * run1 versus run2 full `diff -u` hunks: `69`
    * run1 versus run2 stripped `diff -u` hunks: `63`
    * run1 versus run3 full `diff -u` hunks: `143`
    * run1 versus run3 stripped `diff -u` hunks: `128`
    * run2 versus run3 full `diff -u` hunks: `129`
    * run2 versus run3 stripped `diff -u` hunks: `119`
    * sorted stripped hashes still differ across all three probe outputs
  * exact comparison versus preserved control evidence:
    * baseline versus probe3 stripped `diff -u` hunks:
      * run1 `77`
      * run2 `84`
      * run3 `75`
    * probe1 versus probe3 stripped `diff -u` hunks:
      * run1 `127`
      * run2 `100`
      * run3 `81`
    * probe2 versus probe3 stripped `diff -u` hunks:
      * run1 `118`
      * run2 `111`
      * run3 `190`
  * exact classification:
    * subtraction/clipping probe
    * drift changed but remains
    * executable-body drift remains
    * no perimeter drift found
    * likely upstream-core involved
    * not proven exact same root cause
    * not cause-supported enough to promote next to the shipping ARM app path
* Exact subtraction-side follow-up now also tested on the same clean-head control path:
  * exact file changed:
    * `/tmp/mobileslicer-upstream-control/vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact probe:
    * `expansion_area = diff(expansion_area, bridging_area, ApplySafetyOffset::Yes);`
  * exact rebuild command:
    * `cmake --build /tmp/orca-android-libslic3r-build-print-arm64-control --target orca_android_libslic3r_print_gcode_probe -j8`
  * exact rerun commands:
    * `./tools/adb -s RFCYA01ANVE push /tmp/orca-android-libslic3r-build-print-arm64-control/orca_android_libslic3r_print_gcode_probe /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
    * `./tools/adb -s RFCYA01ANVE shell chmod 755 /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
    * repeated three times:
      * `./tools/adb -s RFCYA01ANVE shell rm -f /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
      * `./tools/adb -s RFCYA01ANVE shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/ad_pig_real_control.stl /data/local/tmp/ad_pig_upstream_control_config.json`
      * `./tools/adb -s RFCYA01ANVE pull /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode /tmp/mobileslicer-upstream-control-runs/probe4/runN/ad_pig_upstream_control_probe4.gcode`
  * exact probe outputs:
    * run1 `/tmp/mobileslicer-upstream-control-runs/probe4/run1/ad_pig_upstream_control_probe4.gcode`
      * full `sha256=c90142024dc25cd46e99eb6c4762b4be33aae5634c39ca0afdfe614de53a1cb7`
      * stripped `sha256=1821a5db08659fbd37dbf7e3336969c47991944657c59ccc1cc3bdf610784847`
      * sorted stripped `sha256=d9b0f5e0d1e5132875068be5931230ef76857afd1558e992a86c0a6fb3ac3fc5`
      * `4634935` bytes
      * `218133` lines
    * run2 `/tmp/mobileslicer-upstream-control-runs/probe4/run2/ad_pig_upstream_control_probe4.gcode`
      * full `sha256=fdcca04b35d34505760a959d6007cfb4afcf0fd7f183d2e4737725e0c22a0e9b`
      * stripped `sha256=24fc49f2eb56c0d8735d83dd305add3177eb48cf0efe50662b67b2b4fc3a8b87`
      * sorted stripped `sha256=efdf64d622a0b0145db6596795b48b2cd368d4c6fd1fe6dc287727a24583fd00`
      * `4635850` bytes
      * `218178` lines
    * run3 `/tmp/mobileslicer-upstream-control-runs/probe4/run3/ad_pig_upstream_control_probe4.gcode`
      * full `sha256=64c1eac7a13ceb4d1479f126d26edb49f00af38d2b97594b32632bf355e739ed`
      * stripped `sha256=a6ba734b3d88bc7b8b133e82dbe0dc25f5f0b93e8e40312e24ee7532fb14b76e`
      * sorted stripped `sha256=899abc94da601e9df002392c9a1daef64296b35e33012824a2e8c070261d3192`
      * `4634537` bytes
      * `218116` lines
  * exact repeated-run comparison:
    * run1 versus run2 full `diff -u` hunks: `135`
    * run1 versus run2 stripped `diff -u` hunks: `128`
    * run1 versus run3 full `diff -u` hunks: `109`
    * run1 versus run3 stripped `diff -u` hunks: `104`
    * run2 versus run3 full `diff -u` hunks: `146`
    * run2 versus run3 stripped `diff -u` hunks: `136`
    * sorted stripped hashes still differ across all three probe outputs
  * exact comparison versus preserved control evidence:
    * baseline versus probe4 stripped `diff -u` hunks:
      * run1 `91`
      * run2 `116`
      * run3 `130`
    * probe1 versus probe4 stripped `diff -u` hunks:
      * run1 `139`
      * run2 `75`
      * run3 `151`
    * probe2 versus probe4 stripped `diff -u` hunks:
      * run1 `155`
      * run2 `173`
      * run3 `108`
    * probe3 versus probe4 stripped `diff -u` hunks:
      * run1 `60`
      * run2 `125`
      * run3 `131`
  * exact classification:
    * subtraction/clipping probe
    * drift changed but remains
    * executable-body drift remains
    * no perimeter drift found
    * likely upstream-core involved
    * not proven exact same root cause
    * not cause-supported enough to promote next to the shipping ARM app path

## Current Slice Responsiveness Model

Current verified shipping `Slice Model` behavior on `RFCYA01ANVE`:

* app entry:
  * `android-app/app/src/main/java/com/mobileslicer/MainActivity.kt`
  * `ModelLoaderScreen` launches `onSliceRequested()` from a coroutine on `Dispatchers.Default`
* JNI entry:
  * `android-app/app/src/main/cpp/jni_bridge.cpp`
  * `Java_com_mobileslicer_nativebridge_NativeEngineBridge_nativeSlice(...)`
* wrapper entry:
  * `engine-wrapper/orca_wrapper.cpp`
  * `orca_slice(...)`

Latest verified larger-fixture fixed runs for `/sdcard/3dPrinting/obj_1_Tassen Regal lang.stl`:

* exact build/deploy commands:
  * `cd /home/peanut/Development/MobileSlicer/android-app && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk GRADLE_USER_HOME=/tmp/gradle-cache ./gradlew :app:assembleDebug --no-daemon --stacktrace`
  * `./tools/adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`
* exact runtime result:
  * load: pass
  * slice: pass
  * export: pass
  * emitted artifact:
    * `/sdcard/3dPrinting/selected-model-obj_1_Tassen_Regal_lang.gcode`
* exact timing evidence from the fixed shipping run:
  * `/tmp/mobileslicer-obj1-anr/obj1-logcat-app-fixed.txt`
  * `sliceCurrentModel:start thread=DefaultDispatcher-worker-1 isMain=false`
  * `process stageMs=14256 totalMs=14300`
  * `export_gcode stageMs=38443 totalMs=52744`
  * `sliceCurrentModel:end ... elapsedMs=52935 sliced=true`
* exact ANR status for that rerun:
  * `/tmp/mobileslicer-obj1-anr/obj1-logcat-anr-fixed.txt`
  * no Mobile Slicer ANR lines emitted
* exact timing evidence from the second fixed run:
  * `/tmp/mobileslicer-obj1-repeat/obj1-logcat-app-repeat.txt`
  * `sliceCurrentModel:start thread=DefaultDispatcher-worker-2 isMain=false`
  * `process stageMs=14329 totalMs=14380`
  * `export_gcode stageMs=38459 totalMs=52840`
  * `sliceCurrentModel:end ... elapsedMs=53032 sliced=true`
* exact ANR status for the second fixed run:
  * `/tmp/mobileslicer-obj1-repeat/obj1-logcat-anr-repeat.txt`
  * no Mobile Slicer ANR lines emitted
* exact repeatability classification for this fixture:
  * first fixed run artifact:
    * `sha256=62564d73014d2f37ed37d37cb1d08ccc950b20e6e0d71e8a545474e9ee5450df`
    * `24754690` bytes
    * `702556` lines
  * second fixed run artifact:
    * `sha256=4709defa31952d630d6d16f285e793d4b56c85fb80081be8060af2d5475a8e89`
    * `24754690` bytes
    * `702556` lines
  * `diff -u` shows only the generated-at timestamp header line changing
  * comment-stripped executable G-code matches exactly:
    * `sha256=3a5fb853584bac9c5cbdce6a44ac74011ec7958f8032b7a2d491fe8a6383a704`
  * current honest claim:
    * the no-ANR larger-fixture result is repeatable for this exact fixture
    * this does not yet prove general larger-fixture stability

## Current Latest Repeatability Boundary

Latest cleared-state repeat run on `RFCYA01ANVE`:

* chosen fixture:
  * `/sdcard/3dPrinting/ad_pig_real.stl`
  * staged from `_quarantine/root-dependency-dump/data/meshes/pig.stl`
* exact preserved comparison baseline:
  * `/tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode`
  * `sha256=9074f126e0c0074373e8106c7a9d5a91a09723d86cd7dcc3ac2f9333f17dda67`
  * `7121341` bytes
  * `218158` lines
* exact commands/interactions used:
  * `mkdir -p /tmp/mobileslicer-ad-pig-baseline /tmp/mobileslicer-ad-pig-repeat`
  * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-ad_pig_real.gcode /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode`
  * `./tools/adb -s RFCYA01ANVE shell sha256sum /sdcard/3dPrinting/selected-model-ad_pig_real.gcode`
  * `sha256sum /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode`
  * `wc -lc /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode`
  * `sha256sum _quarantine/root-dependency-dump/data/meshes/pig.stl`
  * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-ad_pig_real.gcode /sdcard/3dPrinting/ad_pig_real.gcode`
  * `./tools/adb -s RFCYA01ANVE logcat -c`
  * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
  * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
  * `./tools/adb -s RFCYA01ANVE push _quarantine/root-dependency-dump/data/meshes/pig.stl /sdcard/3dPrinting/ad_pig_real.stl`
  * `./tools/adb -s RFCYA01ANVE shell sha256sum /sdcard/3dPrinting/ad_pig_real.stl`
  * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-repeat-picker.xml`
  * verified picker foreground:
    * `topResumedActivity=ActivityRecord{146655242 u0 com.google.android.documentsui/com.android.documentsui.picker.PickActivity t10969}`
  * verified breadcrumb path `3dPrinting`
  * verified safer list-view state because the toolbar exposes `Grid view`
  * verified exact row `ad_pig_real.stl` with title bounds `[252,1642][667,1729]`
  * exact selection tap:
    * row-center `(460,1685)`
  * loaded-state dump:
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-repeat-post.xml`
  * continued after load proof:
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
    * `sleep 8`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-repeat-slice1.xml`
    * `sleep 30`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-repeat-slice2.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-repeat-export-dialog.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
    * `sleep 3`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-repeat-exported.xml`
    * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-ad_pig_real.gcode /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.gcode`
    * `./tools/adb -s RFCYA01ANVE logcat -d -v threadtime | rg 'sliceCurrentModel:start|sliceCurrentModel:end|config stageMs|prepare_model stageMs|apply_validate stageMs|process stageMs|export_gcode stageMs|read_cleanup stageMs|elapsedMs=|sliced='`
    * `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime`
    * `sha256sum /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.gcode`
    * `wc -lc /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.gcode`
    * `diff -u /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.gcode`
    * `sed -E '/^;/d; s/[[:space:]]*;.*$//; /^[[:space:]]*$/d' /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.gcode > /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.stripped.gcode`
    * `sed -E '/^;/d; s/[[:space:]]*;.*$//; /^[[:space:]]*$/d' /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.gcode > /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.stripped.gcode`
    * `sha256sum /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.stripped.gcode /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.stripped.gcode`
    * `wc -lc /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.stripped.gcode /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.stripped.gcode`
    * `diff -u /tmp/mobileslicer-ad-pig-baseline/selected-model-ad_pig_real.stripped.gcode /tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.stripped.gcode`
* exact picker evidence:
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-picker.xml`
    * picker package: `com.google.android.documentsui`
    * breadcrumb path: `3dPrinting`
    * exact result title: `ad_pig_real.stl`
    * title bounds: `[252,1642][667,1729]`
    * preview icon bounds: `[1188,1595][1440,1847]`
    * selected tap point: `(460,1685)`
* exact activity boundary:
  * before selection:
    * `topResumedActivity=ActivityRecord{146655242 u0 com.google.android.documentsui/com.android.documentsui.picker.PickActivity t10969}`
  * after selection:
    * `topResumedActivity=ActivityRecord{80741532 u0 com.mobileslicer/.MainActivity t10969}`
* exact runtime result:
  * load: pass
  * slice: pass
  * export: pass
  * emitted artifact:
    * `/sdcard/3dPrinting/selected-model-ad_pig_real.gcode`
    * `/tmp/mobileslicer-ad-pig-repeat/selected-model-ad_pig_real.gcode`
    * `sha256=5f095b28cdfb2fbd04d9f1ea23159fbf5b1da9570f7675a55dc45de737c1a665`
    * `7120973` bytes
    * `218146` lines
* exact UI and log evidence:
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-post.xml`
    * contains `Model loaded successfully`
    * contains `selected-model-ad_pig_real.stl`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-slice1.xml`
    * contains `Slicing...`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-slice2.xml`
    * contains `Slice successful`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-export-dialog.xml`
    * contains `selected-model-ad_pig_real.gcode`
    * contains `SAVE`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-exported.xml`
    * contains `Export successful`
    * contains `selected-model-ad_pig_real.gcode`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-logcat-app.txt`
    * `process stageMs=16173 totalMs=16287`
    * `export_gcode stageMs=10575 totalMs=26863`
    * `read_cleanup stageMs=39 totalMs=26903`
    * `sliceCurrentModel:end ... elapsedMs=26923 sliced=true`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-logcat-anr.txt`
    * contains `No Mobile Slicer ANR lines were emitted in the ad_pig_real repeat run.`
  * `/tmp/mobileslicer-ad-pig-repeat/ad-pig-repeat-logcat-crash.txt`
    * empty
* exact comparison result:
  * full-file hashes differ:
    * baseline `9074f126e0c0074373e8106c7a9d5a91a09723d86cd7dcc3ac2f9333f17dda67`
    * repeat `5f095b28cdfb2fbd04d9f1ea23159fbf5b1da9570f7675a55dc45de737c1a665`
  * full-file sizes differ:
    * baseline `7121341` bytes
    * repeat `7120973` bytes
  * full-file line counts differ:
    * baseline `218158`
    * repeat `218146`
  * full-file `diff -u`:
    * `106` hunks
    * includes generated-at header drift plus executable-body drift in infill and bridge sections
  * comment-stripped executable G-code differs:
    * baseline `sha256=a81d2f489b966971590c8cc6d12acf426bd902cbc34e4db2b815d8349353eed8`
    * repeat `sha256=f736f79ead0b70edcc03f9c39c36f817ea673370af39f74821f7574c4c37650f`
    * baseline `4534977` bytes
    * repeat `4534773` bytes
    * baseline `213755` lines
    * repeat `213738` lines
    * stripped `diff -u` hunks: `99`
* exact classification:
  * success
  * not a concrete picker/path blocker
  * not a legitimate slicer rejection
  * not repeatable for this fixture at the current boundary
  * exact blocker to repeatability is executable-body drift
  * this run reaches load, slice, export, and emitted output in the shipping ARM path
  * this does not prove general all-fixtures stability

## Historical Pre-Fix Drift Classification

Latest preserved-comparison run on `RFCYA01ANVE`:

* chosen fixture:
  * `/sdcard/3dPrinting/ag_vz330_bed.stl`
  * staged from `vendor/orcaslicer/resources/profiles/Vzbot/Vz330SlicerBedModel-cnc.stl`
* exact staging validation:
  * local source STL:
    * `sha256=a036657131e9a414f4e11a18182a0aaa6386ffec5a891dfa61fd2c264b1edadb`
  * device staged STL before selection:
    * `sha256=a036657131e9a414f4e11a18182a0aaa6386ffec5a891dfa61fd2c264b1edadb`
* exact commands/interactions used:
  * `./tools/adb -s RFCYA01ANVE shell rm -f '/sdcard/3dPrinting/selected-model-ag_vz330_bed.gcode' '/sdcard/3dPrinting/ag_vz330_bed.gcode'`
  * `./tools/adb -s RFCYA01ANVE logcat -c`
  * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
  * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
  * `./tools/adb -s RFCYA01ANVE push 'vendor/orcaslicer/resources/profiles/Vzbot/Vz330SlicerBedModel-cnc.stl' '/sdcard/3dPrinting/ag_vz330_bed.stl'`
  * `./tools/adb -s RFCYA01ANVE shell sha256sum '/sdcard/3dPrinting/ag_vz330_bed.stl'`
  * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-run3-picker.xml`
  * picker reopened already in list view
  * exact list-row selection path:
    * title bounds: `[252,2410][758,2497]`
    * metadata bounds: `[252,2511][680,2568]`
    * preview icon bounds: `[1188,2363][1440,2615]`
    * title-center tap at `(505,2453)` returned to `Mobile Slicer`
    * loaded-state dump: `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-run3-post.xml`
  * continued after load proof:
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
    * `sleep 8`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-run3-slice1.xml`
    * `sleep 10`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-run3-slice2.xml`
    * `sleep 20`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-run3-slice3.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-run3-export-dialog.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
    * `sleep 3`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-run3-exported.xml`
    * `./tools/adb -s RFCYA01ANVE pull '/sdcard/3dPrinting/selected-model-ag_vz330_bed.gcode' /tmp/mobileslicer-ag-vz330-run3/selected-model-ag_vz330_bed.gcode`
* exact picker evidence:
  * `/tmp/mobileslicer-ag-vz330-run3/ag-vz330-run3-picker.xml`
    * picker package: `com.google.android.documentsui`
    * list view is active because the toolbar exposes `Grid view`
    * breadcrumb path: `3dPrinting`
    * `ag_vz330_bed.stl` title bounds: `[252,2410][758,2497]`
    * metadata bounds: `[252,2511][680,2568]`
    * preview icon bounds: `[1188,2363][1440,2615]`
    * selected tap point: `(505,2453)`
* exact activity boundary:
  * before selection:
    * `topResumedActivity=ActivityRecord{207963246 u0 com.google.android.documentsui/com.android.documentsui.picker.PickActivity t10926}`
  * after selection:
    * `topResumedActivity=ActivityRecord{248344532 u0 com.mobileslicer/.MainActivity t10926}`
* exact runtime result:
  * load: pass
  * slice: pass
  * export: pass
  * emitted artifact:
    * `/sdcard/3dPrinting/selected-model-ag_vz330_bed.gcode`
    * `/tmp/mobileslicer-ag-vz330-run3/selected-model-ag_vz330_bed.gcode`
    * `sha256=807febaf4d186c7e27dbb35fc21062316deea43ee53d76e4e823143dd21d8cab`
    * `51478957` bytes
    * `1390049` lines
* exact UI and log evidence:
  * `/tmp/mobileslicer-ag-vz330-run3/ag-vz330-run3-post.xml`
    * contains `Model loaded successfully`
    * contains `selected-model-ag_vz330_bed.stl`
  * `/tmp/mobileslicer-ag-vz330-run3/ag-vz330-run3-slice1.xml`
    * contains `Slicing...`
  * `/tmp/mobileslicer-ag-vz330-run3/ag-vz330-run3-slice2.xml`
    * contains `Slicing...`
  * `/tmp/mobileslicer-ag-vz330-run3/ag-vz330-run3-slice3.xml`
    * contains `Slice successful`
  * `/tmp/mobileslicer-ag-vz330-run3/ag-vz330-run3-export-dialog.xml`
    * contains `selected-model-ag_vz330_bed.gcode`
  * `/tmp/mobileslicer-ag-vz330-run3/ag-vz330-run3-exported.xml`
    * contains `Export successful`
    * contains `selected-model-ag_vz330_bed.gcode`
  * `./tools/adb -s RFCYA01ANVE logcat -d -v threadtime | rg "sliceCurrentModel:start|sliceCurrentModel:end|config stageMs|prepare_model stageMs|apply_validate stageMs|process stageMs|export_gcode stageMs|elapsedMs=|sliced="`
    * `process stageMs=40446 totalMs=40504`
    * `export_gcode stageMs=20136 totalMs=60640`
    * `sliceCurrentModel:end ... elapsedMs=61017 sliced=true`
  * crash buffer:
    * `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime`
    * no output
* exact artifact comparison against `/tmp/mobileslicer-ag-vz330-repeat/selected-model-ag_vz330_bed.gcode`:
  * full-file hash differs:
    * run2 `b8f048012807d3789ae62c4340e5e39ab9d529435fc235ee2e04f443978d6143`
    * run3 `807febaf4d186c7e27dbb35fc21062316deea43ee53d76e4e823143dd21d8cab`
  * full-file size differs:
    * run2 `51476131` bytes
    * run3 `51478957` bytes
  * full-file line count differs:
    * run2 `1389963`
    * run3 `1390049`
  * full-file `diff -u` classification:
    * first hunk is the generated-at header line
    * total hunks: `222`
    * later hunks occur deep in executable output, beginning for example at `@@ -177745,2576 +177745,66 @@`
    * classification: executable-body drift, not header-only
  * comment-stripped comparison:
    * strip command used:
      * `sed -E '/^;/d; s/[[:space:]]*;.*$//; /^[[:space:]]*$/d' ... > ...stripped.gcode`
    * run2 stripped artifact:
      * `/tmp/mobileslicer-ag-vz330-repeat/selected-model-ag_vz330_bed.stripped.gcode`
      * `sha256=ea932dd7b15091eb755c7695e71b50d5c8d2aaf09dba3fbd9dd51611f306e97f`
      * `37854340` bytes
      * `1386185` lines
    * run3 stripped artifact:
      * `/tmp/mobileslicer-ag-vz330-run3/selected-model-ag_vz330_bed.stripped.gcode`
      * `sha256=c79dc0af3c3c3c5f64fa25841f11f9afc1c56e0611fae0e3f99e0f88f7f5e43c`
      * `37855744` bytes
      * `1386271` lines
    * stripped `diff -u` total hunks: `114`
    * stripped comparison still differs in executable commands, beginning for example at `@@ -176102,2576 +176102,66 @@`
    * classification: not comment-only; executable-body drift remains after stripping comments
* exact classification:
  * success
  * emits output
  * output differs
  * executable-body drift
  * narrowest honest cause supported by evidence: real output nondeterminism
  * not inconsistent staged input / selection mismatch
  * not export/path artifact mismatch
* current honest claim at that historical pre-fix boundary:
  * `ag_vz330_bed.stl` was still not yet proven repeatable for this exact fixture
  * evidence at that time showed real executable-body nondeterminism across preserved successful runs
  * this did not yet prove general all-fixtures stability

## Current Latest Vendor Fixture Boundary

Latest newly classified local vendor STL run on `RFCYA01ANVE`:

* chosen fixture:
  * `/sdcard/3dPrinting/ag_vz330_bed.stl`
  * staged from `vendor/orcaslicer/resources/profiles/Vzbot/Vz330SlicerBedModel-cnc.stl`
* exact commands/interactions used:
  * `./tools/adb -s RFCYA01ANVE shell rm -f '/sdcard/3dPrinting/selected-model-ag_vz330_bed.gcode' '/sdcard/3dPrinting/ag_vz330_bed.gcode'`
  * `./tools/adb -s RFCYA01ANVE logcat -c`
  * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
  * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
  * `./tools/adb -s RFCYA01ANVE push 'vendor/orcaslicer/resources/profiles/Vzbot/Vz330SlicerBedModel-cnc.stl' '/sdcard/3dPrinting/ag_vz330_bed.stl'`
  * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-picker2.xml`
  * picker reopened already in list view
  * exact list-row selection path:
    * title bounds: `[252,2410][758,2497]`
    * metadata bounds: `[252,2511][680,2568]`
    * preview icon bounds: `[1188,2363][1440,2615]`
    * title-center tap at `(505,2453)` returned to `Mobile Slicer`
    * loaded-state dump: `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-post.xml`
  * continued after load proof:
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
    * `sleep 8`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-slice1.xml`
    * `sleep 10`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-slice2.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-export-dialog.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
    * `sleep 3`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ag-vz330-exported.xml`
    * `./tools/adb -s RFCYA01ANVE pull '/sdcard/3dPrinting/selected-model-ag_vz330_bed.gcode' /tmp/mobileslicer-ag-vz330/selected-model-ag_vz330_bed.gcode`
* exact picker evidence:
  * `/tmp/mobileslicer-ag-vz330/ag-vz330-picker2.xml`
    * picker package: `com.google.android.documentsui`
    * breadcrumb path: `3dPrinting`
    * list view is active because the toolbar exposes `Grid view`
    * `ag_vz330_bed.stl` title bounds: `[252,2410][758,2497]`
    * metadata bounds: `[252,2511][680,2568]`
    * preview icon bounds: `[1188,2363][1440,2615]`
    * selected tap point: `(505,2453)`
* exact runtime result:
  * load: pass
  * slice: pass
  * export: pass
  * emitted artifact:
    * `/sdcard/3dPrinting/selected-model-ag_vz330_bed.gcode`
    * `/tmp/mobileslicer-ag-vz330/selected-model-ag_vz330_bed.gcode`
    * `sha256=c40e43fbb062607805abd2f830d5850ccdd02655c93ad79b8fdf580292ad5581`
    * `51478957` bytes
    * `1390049` lines
* exact UI and log evidence:
  * `/tmp/mobileslicer-ag-vz330/ag-vz330-post.xml`
    * contains `Model loaded successfully`
    * contains `selected-model-ag_vz330_bed.stl`
  * `/tmp/mobileslicer-ag-vz330/ag-vz330-slice1.xml`
    * contains `Slicing...`
  * `/tmp/mobileslicer-ag-vz330/ag-vz330-export-dialog.xml`
    * contains `selected-model-ag_vz330_bed.gcode`
  * `/tmp/mobileslicer-ag-vz330/ag-vz330-exported.xml`
    * contains `Export successful`
    * contains `selected-model-ag_vz330_bed.gcode`
  * `./tools/adb -s RFCYA01ANVE logcat -d -v threadtime | rg 'sliceCurrentModel:start|sliceCurrentModel:end|config stageMs|prepare_model stageMs|apply_validate stageMs|process stageMs|export_gcode stageMs|elapsedMs=|sliced='`
    * `process stageMs=42915 totalMs=43003`
    * `export_gcode stageMs=20168 totalMs=63172`
    * `sliceCurrentModel:end ... elapsedMs=63554 sliced=true`
  * crash buffer:
    * `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime`
    * no output
* exact classification:
  * success
  * not a legitimate slicer rejection
  * not a concrete shipping bug
  * this run reaches load, slice, export, and emitted output in the shipping ARM path

## Current Shipping ARM Stability Matrix

This section tracks the current shipping app runtime confidence boundary only.

Constraints honored in the latest run:

* target device: `RFCYA01ANVE`
* scope: shipping ARM real-wrapper path only
* no timestamp normalization work
* stop at the first concrete runtime/config/export blocker
* no config/UI contract churn

What the current shipping path can honestly vary:

* fixture geometry

What it cannot currently vary without contract churn:

* meaningful config variants from the shipping UI

Exact staged fixtures for the current matrix:

* `engine-wrapper/orca-android-libslic3r/testdata/tetrahedron_20mm_ascii.stl` -> `/sdcard/3dPrinting/aa_matrix_tetra_20mm.stl`
* `engine-wrapper/orca-android-libslic3r/testdata/tetrahedron_ascii.stl` -> `/sdcard/3dPrinting/ab_matrix_tetra_small.stl`
* `mobileslicer_test_cube.stl` -> `/sdcard/3dPrinting/ac_matrix_cube.stl`
* additional staged real fixture -> `/sdcard/3dPrinting/est.stl`
* larger default-path staged asset -> `/sdcard/3dPrinting/obj_1_Tassen Regal lang.stl`
* additional staged real asset -> `/sdcard/3dPrinting/ad_pig_real.stl`
* additional staged real asset -> `/sdcard/3dPrinting/ae_sphere_real.stl`
* additional staged vendor asset -> `/sdcard/3dPrinting/af_goliath.stl`

Exact shipping flow exercised for the two successful reruns of the proven case:

* `./tools/adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`
* `./tools/adb -s RFCYA01ANVE logcat -c`
* `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-aa_matrix_tetra_20mm.gcode`
* `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
* `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
* `sleep 1`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
* picker reopened directly in `/sdcard/3dPrinting`
* select `aa_matrix_tetra_20mm.stl`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
* `sleep 6`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input tap 1235 2840`
* `sleep 3`

Exact successful artifacts:

* run 1:
  * device path: `/sdcard/3dPrinting/selected-model-aa_matrix_tetra_20mm.gcode`
  * host path: `/tmp/mobileslicer-matrix/run1/selected-model-aa_matrix_tetra_20mm.gcode`
  * `sha256=80b6c77cc80507019346357f772d6640b3a93a527c935a1b52557c66e255f1fb`
  * `370659` bytes
  * `13168` lines
* run 2:
  * device path: `/sdcard/3dPrinting/selected-model-aa_matrix_tetra_20mm.gcode`
  * host path: `/tmp/mobileslicer-matrix/run2/selected-model-aa_matrix_tetra_20mm.gcode`
  * `sha256=14d8bc67d034d15bc3084538a2140fe52841dadbb8dfcd32f2e57dcfc7d7e636`
  * `370659` bytes
  * `13168` lines
* exact repeatability result for that proven case:
  * full-file hash differs
  * byte size matches
  * line count matches
  * `diff -u` shows timestamp-line drift only
  * comment-stripped artifacts match exactly:
    * `/tmp/mobileslicer-matrix/run1/aa.nocomments.gcode`
    * `/tmp/mobileslicer-matrix/run2/aa.nocomments.gcode`
    * `sha256=90a0c6af40fe34a3ea0388c981425ddfee81aba9f15d9c73082ea17f759425e8`

Exact first blocker that stopped the original matrix:

* fixture:
  * `/sdcard/3dPrinting/ab_matrix_tetra_small.stl`
* exact corrected interaction used:
  * `./tools/adb -s RFCYA01ANVE logcat -c`
  * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-ab_matrix_tetra_small.gcode`
  * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
  * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/run3-picker.xml`
  * `./tools/adb -s RFCYA01ANVE shell input tap 1111 1524`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/run3-load.xml`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/run3-slice.xml`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/run3-export.xml`
  * `./tools/adb -s RFCYA01ANVE shell ls -lt /sdcard/3dPrinting`
  * `./tools/adb -s RFCYA01ANVE logcat -d -b crash`
* exact observed result:
  * load: pass
  * slice: fail
  * export: not reached
  * emitted artifact: none
  * exact status text: `Slice failed`
  * expected output path missing: `/sdcard/3dPrinting/selected-model-ab_matrix_tetra_small.gcode`
  * `logcat -d -b crash`: no entries
* honest current classification:
  * blocker class: `slice`
  * current scope: general until proven ARM-specific
  * current interpretation: legitimate slicer rejection under current defaults
* source/config evidence supporting that interpretation:
  * `vendor/orcaslicer/src/libslic3r/GCode.cpp:1749` throws the same empty-first-layer message seen in runtime logs
  * `engine-wrapper/orca_wrapper.cpp` applies default full print config and does not enable supports or cut-bottom handling
  * `android-app/app/src/main/java/com/mobileslicer/MainActivity.kt` exposes no existing no-churn support or cut-bottom control
  * `engine-wrapper/orca-android-libslic3r/testdata/tetrahedron_ascii.stl` is a 1 mm tetrahedron source fixture

## Refined Second-Fixture Slice Classification

The first stopped second-fixture blocker has now been rerun with minimal native logging in the same shipping ARM path.

Exact code change used only to expose the blocker:

* `engine-wrapper/orca_wrapper.cpp`
  * added Android-native error logging for wrapper exceptions
  * added explicit logging of contained `Slic3r::SlicingErrors` entries instead of only logging the aggregate `Errors` wrapper text

Exact rebuild and deploy commands used:

* `cd /home/peanut/Development/MobileSlicer/android-app && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk GRADLE_USER_HOME=/tmp/gradle-cache ./gradlew :app:assembleDebug --no-daemon --stacktrace`
* `./tools/adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`

Exact focused rerun used after the diagnostic change:

* `./tools/adb -s RFCYA01ANVE logcat -c`
* `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-ab_matrix_tetra_small.gcode`
* `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
* `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input tap 1111 1524`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
* `sleep 8`
* `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ab-repro-postslice3.xml`
* `./tools/adb -s RFCYA01ANVE pull /sdcard/ab-repro-postslice3.xml /tmp/mobileslicer-ab-debug/ab-repro-postslice3.xml`
* `./tools/adb -s RFCYA01ANVE logcat -d -s MobileSlicerNative:V MobileSlicer:V AndroidRuntime:E libc:E DEBUG:E > /tmp/mobileslicer-ab-debug/ab-repro-focused.txt`
* `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime > /tmp/mobileslicer-ab-debug/ab-repro-crash.txt`
* `./tools/adb -s RFCYA01ANVE shell ls -l /sdcard/3dPrinting/selected-model-ab_matrix_tetra_small.gcode`

Exact refined blocker evidence:

* UI evidence:
  * `/tmp/mobileslicer-ab-debug/ab-repro-postslice3.xml`
  * app still shows `Slice failed`
  * `Export G-code` and `Share G-code` remain disabled
* focused native log:
  * `/tmp/mobileslicer-ab-debug/ab-repro-focused.txt`
  * exact line:
    * `E MobileSlicerNative: orca_slice: One object has an empty first layer and can't be printed. Please Cut the bottom or enable supports.`
* crash buffer:
  * `/tmp/mobileslicer-ab-debug/ab-repro-crash.txt`
  * empty
* output artifact:
  * `/sdcard/3dPrinting/selected-model-ab_matrix_tetra_small.gcode`
  * not present

Honest refined classification:

* blocker class: `slice`
* blocker scope: general in current evidence, not ARM-specific
* cause type: fixture/default-config slicer rejection
* matrix status after this rerun:
  * repeated single-case stable: still yes
  * small-matrix stable: still no
* `/sdcard/3dPrinting/ac_matrix_cube.stl` is now attempted and successful in the same shipping ARM path

## Continued Matrix Case

Exact continued shipping flow for the next staged fixture:

* `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/ac_matrix_cube.gcode /sdcard/3dPrinting/selected-model-ac_matrix_cube.gcode`
* `./tools/adb -s RFCYA01ANVE logcat -c`
* `./tools/adb -s RFCYA01ANVE shell am force-stop com.mobileslicer`
* `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input swipe 720 1400 720 2600 300`
* `sleep 1`
* `./tools/adb -s RFCYA01ANVE shell input tap 395 2070`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
* `sleep 5`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
* `sleep 3`

Exact result for `/sdcard/3dPrinting/ac_matrix_cube.stl`:

* load: pass
* slice: pass
* export: pass
* emitted artifact:
  * device path: `/sdcard/3dPrinting/selected-model-ac_matrix_cube.gcode`
  * host path: `/tmp/mobileslicer-ac-matrix/selected-model-ac_matrix_cube.gcode`
  * `sha256=ecf93b99b79a402fcd036aaefc701b4fed4670f2f67567685fa4d6fe25b099bf`
  * `417425` bytes
  * `12370` lines

Honest matrix classification after this continuation:

* repeated single-case stable: yes
* small-matrix stable: no as an all-fixture-succeeds boundary
* stable with known limitations: yes
  * `aa_matrix_tetra_20mm.stl` reruns remain stable
  * `ab_matrix_tetra_small.stl` is legitimately rejected by the slicer under current defaults
  * `ac_matrix_cube.stl` slices and exports successfully
* the next larger default-path confidence case now stops at a concrete runtime blocker rather than a slicer rejection
  * chosen fixture:
    * `/sdcard/3dPrinting/obj_1_Tassen Regal lang.stl`
  * exact result:
    * load: pass
    * slice: fail via Android ANR dialog
    * export: not reached
    * emitted artifact: none
  * exact blocker classification:
    * class: `slice`
    * status: concrete shipping bug
    * current scope: not yet evidenced as ARM-specific
  * exact evidence:
    * UI:
      * `/tmp/mobileslicer-obj1-matrix/obj1-anr-ui.xml`
      * title `Mobile Slicer isn't responding`
    * logcat:
      * `ANR in com.mobileslicer (com.mobileslicer/.MainActivity)`
      * `Reason: Input dispatching timed out ... Waited 10000ms for MotionEvent`
      * `spent 52486ms processing MotionEvent`
    * output path:
      * `/sdcard/3dPrinting/selected-model-obj_1_Tassen_Regal_lang.gcode`
      * not present

## Additional Real Fixture Boundary

Exact chosen fixture:

* `/sdcard/3dPrinting/est.stl`
* staged size:
  * about `2.13 MB`

Exact commands/interactions used:

* `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-est.gcode /sdcard/3dPrinting/est.gcode`
* `./tools/adb -s RFCYA01ANVE logcat -c`
* `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
* `./tools/adb -s RFCYA01ANVE shell am force-stop com.mobileslicer`
* `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell input tap 1040 2380`
* `sleep 3`
* `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-loaded.xml`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
* `sleep 8`
* `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-postslice.xml`
* `sleep 35`
* `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-postslice2.xml`
* `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
* `sleep 2`
* `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-export-dialog.xml`
* `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
* `sleep 3`
* `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-exported.xml`
* `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-est.gcode /tmp/mobileslicer-est/selected-model-est.gcode`

Exact result for `/sdcard/3dPrinting/est.stl`:

* load: pass
* slice: pass
* export: pass
* emitted artifact:
  * `/sdcard/3dPrinting/selected-model-est.gcode`
  * `/tmp/mobileslicer-est/selected-model-est.gcode`
  * `sha256=a6a53ce1f4158bb5708e0203a270cd85da38d8e715ea87fe3ff6c26430f9a43e`
  * `15690430` bytes
  * `523489` lines

Exact evidence:

* load UI:
  * `/tmp/mobileslicer-est/est-loaded.xml`
  * `sha256=d3541731f3da77ad207487f85ce0d5eb384c5e8e1e09ae44e8c774d783155dd4`
  * `11021` bytes
  * contains `Model loaded successfully`
* first post-slice snapshot:
  * `/tmp/mobileslicer-est/est-postslice.xml`
  * `sha256=bc9cfe05dcd01d21ba06bec68ec32041bdfa917ad6dd75d58b57502156197a94`
  * `10988` bytes
  * contains `Slicing...`
* successful post-slice snapshot:
  * `/tmp/mobileslicer-est/est-postslice2.xml`
  * `sha256=a60a51292a1f7bcd026d05e442405f1b81205f6342ee50343261efa29eb8653f`
  * `10981` bytes
  * contains `Slice successful`
* post-export snapshot:
  * `/tmp/mobileslicer-est/est-exported.xml`
  * `sha256=fb2c559eed4b8a6870c4aa7f184c260a70ab20646308601b2e0dc66905f17c29`
  * `11011` bytes
  * contains `Export successful` and `selected-model-est.gcode`
* app/native timing log:
  * `/tmp/mobileslicer-est/est-logcat-app.txt`
  * `sha256=9acb6b27dfe5da17bd08ca0de9360896f998e2c682972334078d0bebe05e9e3e`
  * `947` bytes
  * `9` lines
  * exact timing:
    * `config stageMs=2`
    * `prepare_model stageMs=1`
    * `apply_validate stageMs=54`
    * `process stageMs=19716`
    * `export_gcode stageMs=26168`
    * `read_cleanup stageMs=101`
    * `sliceCurrentModel:end ... elapsedMs=46095 sliced=true`
* ANR status:
  * `/tmp/mobileslicer-est/est-logcat-anr.txt`
  * `sha256=d6332eea49cedcfb07236de3f1a0c537c961b23e282daaa04190372a562cba83`
  * `62` bytes
  * `1` line
  * contains `No Mobile Slicer ANR lines were emitted in the est.stl run.`

Honest current matrix update:

* successful fixtures now include:
  * `aa_matrix_tetra_20mm.stl`
  * `ac_matrix_cube.stl`
  * `est.stl`
  * `obj_1_Tassen Regal lang.stl`
* legitimate rejection under current defaults remains:
  * `ab_matrix_tetra_small.stl`
* current honest claim:
  * shipping ARM confidence is broader across the currently classified fixture set
  * it is still not a broad all-fixtures stability claim

## Requirements

* Disable GUI components
* Strip unused modules
* Ensure reproducible builds

## Android Orca Core

* Path: `engine-wrapper/orca-android-core/`
* Built as a separate CMake target from the full vendor Orca tree
* Uses vendored Orca STL import sources plus local Android shims
* Scope limited to STL parsing, repair, simple config capture, and controlled G-code generation
* Android app CMake links `liborca_engine.so` against `liborca_core_android.so`

## Current Reduced Backend Audit

The reduced Android-native core still exists in-tree, but it is no longer the current proven shipping ARM output path. It is a narrow compatibility layer around `admesh` plus wrapper-owned control flow.

What is currently real:

* STL file access and validation are exercised from Android through JNI and the wrapper
* STL parsing comes from vendored Orca dependency sources under `vendor/orcaslicer/deps_src/admesh`
* Import repair logic mirrors the mesh-repair portion of Orca import flow
* The wrapper contract for `load_model`, `set_config_json`, `slice`, and `get_gcode` is stable

What is currently reduced:

* `engine-wrapper/orca-android-core/orca_android_core.cpp` stores an `admesh::stl_file`, not a `Slic3r::Model`
* Config handling is regex-based extraction from a tiny JSON subset, not `DynamicPrintConfig` / `PrintConfig`
* `slice()` validates the repaired mesh and emits a synthetic perimeter loop over the STL bounding box
* G-code output is generated by local string building, not by `Slic3r::Print` plus `Slic3r::GCode`

What is missing from full Orca `libslic3r` slicing:

* `Slic3r::Model` ownership and import pipeline
* Real print preset application through `PrintConfig` / `DynamicPrintConfig`
* `Slic3r::Print` / `PrintObject` / `Layer` slicing stages
* Infill, perimeters, supports, skirts, brims, wipe tower, and conflict logic
* Real G-code export through `Slic3r::GCode`
* Estimates and other structured slice results exposed from the real engine

## Upstream Build Gap

The reduced Android core deliberately avoids the upstream `src/libslic3r/CMakeLists.txt` dependency graph.

The upstream `libslic3r` target in this repo currently requires, directly in CMake:

* `CGAL`
* `OpenCV`
* `OpenCASCADE`
* `JPEG`
* `draco`
* `TBB`
* `OpenSSL`
* `PNG`
* `EXPAT`
* optional `OpenVDB`

It also links vendored internal libraries such as:

* `admesh`
* `libigl`
* `libnest2d`
* `clipper`
* `Clipper2`
* `mcut`
* `qhull`
* `qoi`
* `semver`
* `noise::noise`

This means fallback Android build states can still exercise only a small subset. The current proven shipping ARM boundary on `RFCYA01ANVE`, however, does exercise the real-wrapper build graph and emits real-wrapper / Orca-style output.

## Dedicated Android Orca Build Path

Phase 2 kept the reduced backend in place while the real port was proven separately.

The new isolated path is:

* `engine-wrapper/orca-android-libslic3r/`

Rules for that path:

* Headless only
* Android NDK compatible
* No Android app dependency until the experimental target can compile and load cleanly
* No direct JNI or Kotlin coupling
* Wrapper contract remains the only future swap point

Current status:

* The path is active for experimental work
* It began as an isolated non-shipping path and is now proven through the shipping ARM app build when the real-wrapper staging gate is satisfied
* It exists to hold Android-specific `libslic3r` CMake logic and future shims without destabilizing `liborca_core_android.so`
* The experimental target now builds successfully with the Android NDK for `x86_64` as `liborca_android_libslic3r_dependency_subset.a`
* That target consumes real Android-built Boost, TBB, GMP, MPFR, and CGAL artifacts while remaining outside the shipping app build
* Dependency consumption is currently explicit in CMake through imported targets instead of relying on desktop-style package discovery
* OpenSSL 3.1.7 is now staged as a static Android dependency for the experimental path
* The Boost header tree is now assembled from the full Boost 1.84.0 source to provide complete header-only module coverage
* `arm64-v8a` experimental-path configure proof has progressed to dependency-validated configure and successful compile/link completion for `orca_android_libslic3r_print_gcode_probe`; ARM runtime proof is now complete on `RFCYA01ANVE`.

## Current Metadata Determinism Classification

The current shipping ARM real-wrapper path emits two distinct metadata sources:

* Header timestamp:
  * emission site: `vendor/orcaslicer/src/libslic3r/GCode.cpp:2530`
  * time source: `vendor/orcaslicer/src/libslic3r/Time.hpp:35-36`
  * generator label source in the current Android experimental/real-wrapper build:
    * `engine-wrapper/orca-android-libslic3r/android_libslic3r_stubs.cpp:43-45`
  * current behavior:
    * expected wall-clock metadata
    * emitted whenever the header block is written
    * only suppressed if the header block is skipped by the `BTT_TFT` thumbnail branch

* `printing object` / `stop printing object` comments:
  * emission sites:
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:5218-5221`
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:5355-5359`
  * printed numeric `id:` source:
    * `PrintObject::get_id()` from `vendor/orcaslicer/src/libslic3r/Print.hpp:468-469`
    * backed by raw field `PrintObject::m_id` at `vendor/orcaslicer/src/libslic3r/Print.hpp:580`
  * deterministic assignment path:
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:7804-7842`
    * `GCode::set_object_info(Print *print)` assigns `object->set_id(object_id++)`
  * previous shipping path issue:
    * `set_object_info()` is only called when `m_enable_exclude_object` is true at `vendor/orcaslicer/src/libslic3r/GCode.cpp:2657-2658`
    * `m_enable_exclude_object` is sourced from `exclude_object` at `vendor/orcaslicer/src/libslic3r/GCode.cpp:5443`
    * Orca defaults currently make `gcode_label_objects=true` but `exclude_object=false` at `vendor/orcaslicer/src/libslic3r/PrintConfig.cpp:3651-3663`
    * result: the comment path previously read `PrintObject::m_id` without executing the deterministic assignment path first
  * fix now applied:
    * `vendor/orcaslicer/src/libslic3r/PrintObject.cpp:78-86`
    * constructor seeds `m_id` from `model_object->id().id`
    * the later exclude-object path can still overwrite `m_id` with export-time sequential IDs when enabled

Honest current classification:

* timestamp drift: expected metadata-only behavior
* object-id comment drift: fixed in the current source path
* current shipping reruns now show deterministic toolpath plus deterministic object labels, with only the header timestamp left as intentional metadata drift

### arm64-v8a Shipping App Real-Wrapper Runtime Classification (Current State)

* Fresh restaging used before the rebuild:
  * Boost source root:
    * `/tmp/orca-deps-src/boost-1.84.0`
  * Boost filesystem + headers:
    * `/tmp/orca-deps-install/arm64-boost-cgal/lib/libboost_filesystem.a`
    * `/tmp/orca-deps-install/arm64-boost-headers/include/boost/...`
  * TBB:
    * `/tmp/orca-deps-install/arm64-tbb-static2/lib/libtbb.a`
    * `/tmp/orca-deps-install/arm64-tbb-static2/lib/libtbbmalloc.a`
  * OpenSSL:
    * `/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a`
    * `/tmp/orca-deps-install/arm64-openssl/lib/libssl.a`
  * CGAL:
    * `/tmp/orca-deps-install/arm64-cgal/include/CGAL`
* Build artifact confirmation:
  * `/home/peanut/Development/MobileSlicer/android-app/app/build/outputs/apk/debug/app-debug.apk`
  * `/home/peanut/Development/MobileSlicer/android-app/app/build/intermediates/cxx/Debug/35l5eg2l/obj/arm64-v8a/liborca_engine.so`
  * `/home/peanut/Development/MobileSlicer/android-app/app/build/intermediates/cxx/Debug/35l5eg2l/obj/arm64-v8a/liborca_android_libslic3r_config_impl_subset.so`
  * `/home/peanut/Development/MobileSlicer/android-app/app/build/intermediates/stripped_native_libs/debug/stripDebugDebugSymbols/out/lib/arm64-v8a/liborca_engine.so`
* Configure + selection evidence:
  * `android-app/app/.cxx/Debug/35l5eg2l/arm64-v8a/CMakeCache.txt`
    * `ORCA_SHIPPING_USE_REAL_LIBSLIC3R:BOOL=ON`
    * `ORCA_ANDROID_LIBSLIC3R_ENABLE:BOOL=ON`
    * `ORCA_ANDROID_BOOST_PREFIX:PATH=/tmp/orca-deps-install/arm64-boost-cgal`
    * `ORCA_ANDROID_BOOST_HEADERS_DIR:PATH=/tmp/orca-deps-install/arm64-boost-headers/include`
  * `rg -n "falling back to reduced backend wrapper" android-app/app/build/intermediates/cxx android-app/app/.cxx -S`
    * no matches in the current generated tree
  * `android-app/app/build/intermediates/cxx/Debug/35l5eg2l/logs/arm64-v8a/build_command_targets`
    * target build command includes `orca_android_libslic3r_config_impl_subset`
  * `android-app/app/.cxx/Debug/35l5eg2l/arm64-v8a/build.ninja`
    * `build .../liborca_engine.so ... | ... orca-android-libslic3r/liborca_android_libslic3r_print_gcode_lib.a ...`
    * `TARGET_FILE = orca-android-libslic3r/liborca_android_libslic3r_print_gcode_lib.a`
  * `android-app/app/build/intermediates/cxx/Debug/35l5eg2l/logs/arm64-v8a/build_stdout_targets.txt`
    * current generated log still contains `Linking CXX shared library .../liborca_android_libslic3r_config_impl_subset.so`
* Historical exact blocker (interrupted boundary, preserved):
  * `/tmp/orca-rebuild-logs/shipping_after_gmp_mpfr_stage_run.log:17786-17840`
  * exact failing command context: `/tmp/gradle-cache/daemon/8.9/daemon-216527.out.log:35802` (ninja shared-link of `.../liborca_android_libslic3r_config_impl_subset.so`)
  * unresolved symbols included:
    * `Clipper2Lib::ClipperBase::AddPaths(...)`
    * `Clipper2Lib::ClipperBase::ExecuteInternal(...)`
    * `Clipper2Lib::Clipper64::BuildPaths64(...)`
    * `Clipper2Lib::ClipperBase::~ClipperBase()`
    * `Clipper2Lib::ClipperOffset::AddPaths(...)`
    * `Clipper2Lib::ClipperOffset::Execute(...)`
    * `Clipper2Lib::ClipperBase`/`ClipperOffset` vtable/typeinfo references
    * `Slic3r::chain_clipper_polynodes(...)`
  * exact boundary class: link-time after target generation (Clipper2 source/object/dep closure)
* Exact shipping wrapper-selection gate in `android-app/app/src/main/cpp/CMakeLists.txt`:
  * `EXISTS "${ORCA_ANDROID_BOOST_PREFIX}/lib/libboost_filesystem.a"`
  * `EXISTS "${ORCA_ANDROID_TBB_PREFIX}/lib/libtbb.a"`
  * `EXISTS "${ORCA_ANDROID_OPENSSL_CRYPTO_LIBRARY}"`
  * `EXISTS "${ORCA_ANDROID_OPENSSL_SSL_LIBRARY}"`
  * `EXISTS "${ORCA_ANDROID_CGAL_PREFIX}/include"`
  * `EXISTS "${ORCA_ANDROID_BOOST_SRC_ROOT}"`
  * then `TARGET orca_android_libslic3r_print_gcode_lib` must exist after `add_subdirectory(...)`
* Exact runtime crash causes isolated and fixed in this boundary:
  * config-owned crash before export setup completed:
    * `vendor/orcaslicer/src/libslic3r/Config.cpp:1805-1808`
      * `StaticConfig::set_defaults()` calls `opt->set(def->default_value.get())`
    * `vendor/orcaslicer/src/libslic3r/Config.hpp:2121-2127`
      * `ConfigOptionEnumsGenericTempl<...>::set(...)` now copies both `values` and `keys_map`
    * exact invalid state:
      * `ConfigOptionEnumsGenericTempl<false>::keys_map == nullptr`
    * exact surfaced key during diagnosis:
      * `extruder_type`
    * failure timing:
      * during export setup, on full-config serialization reached from `GCode::append_full_config(...)`
  * wrapper-owned crash during export setup:
    * `engine-wrapper/orca_wrapper.cpp:205-207`
      * wrapper now passes a real `Slic3r::GCodeProcessorResult gcode_result`
    * exact invalid state before fix:
      * `GCodeProcessorResult *result == nullptr`
    * failure timing:
      * during `Print::export_gcode(...)`, before slice/export flow could complete
* Rebuild command used after the minimum fix set:
  * `cd /home/peanut/Development/MobileSlicer/android-app && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk GRADLE_USER_HOME=/tmp/gradle-cache ./gradlew :app:assembleDebug --no-daemon --stacktrace`
* Deploy + runtime commands used for the successful rerun:
  * `./tools/adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`
  * `./tools/adb -s RFCYA01ANVE logcat -c`
  * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
  * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
  * `./tools/adb -s RFCYA01ANVE shell input tap 400 2800`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-after-load-final.xml`
  * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-after-load-final.xml`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-after-slice-final.xml`
  * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-after-slice-final.xml`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
  * `./tools/adb -s RFCYA01ANVE shell input tap 1235 2840`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-after-export-final.xml`
  * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-after-export-final.xml`
  * `./tools/adb -s RFCYA01ANVE shell ls -l /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `./tools/adb -s RFCYA01ANVE logcat -d -b crash`
* Runtime classifier for the current boundary:
  * pre-`add_subdirectory` wrapper gate: pass
  * configure/generation: pass
  * linker closure: pass through `orca_android_libslic3r_config_impl_subset`, `orca_android_libslic3r_print_gcode_lib`, and `orca_engine`
  * install: pass
  * launch: pass
  * model load: pass
  * slice: pass
  * export: pass
  * output emitted: pass
  * rerun repeatability: semantically stable but byte-different
* Exact successful runtime evidence:
  * `am start -W` returned `Status: ok`, `LaunchState: COLD`, `Activity: com.mobileslicer/.MainActivity`
  * `mobileslicer-after-load-final.xml` shows `Model loaded successfully` and `selected-model-tetrahedron_20mm_ascii_fresh.stl`
  * `mobileslicer-after-slice-final.xml` shows `Slice successful`, `Export G-code`, and `Share G-code`
  * `mobileslicer-after-export-final.xml` shows `Export successful` and `selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * emitted output artifact:
    * `/sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
    * size `375843`
  * `adb logcat -d -b crash` is empty after the successful rerun
* Exact artifact-validation rerun commands used after the runtime proof:
  * `./tools/adb devices -l`
  * `./tools/adb -s RFCYA01ANVE shell getprop ro.product.cpu.abi`
  * `./tools/adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`
  * `./tools/adb -s RFCYA01ANVE logcat -c`
  * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
  * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
  * `./tools/adb -s RFCYA01ANVE shell input tap 400 2800`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-after-load-verify.xml`
  * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-after-load-verify.xml`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-after-slice-verify.xml`
  * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-after-slice-verify.xml`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
  * `./tools/adb -s RFCYA01ANVE shell input tap 1235 2840`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-after-export-verify.xml`
  * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-after-export-verify.xml`
  * `./tools/adb -s RFCYA01ANVE shell ls -l /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode /tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `sha256sum /tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `wc -l -c /tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `sed -n '1,160p' /tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `tail -n 120 /tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `rg -n "generated by orca_core_android|source=orcaslicer_android_wrapper|^;LAYER:" /tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode -N`
  * `rg -n "^;TYPE:|^;LAYER_CHANGE$|^; CONFIG_BLOCK_START$|^; CONFIG_BLOCK_END$|^; EXECUTABLE_BLOCK_START$|^; EXECUTABLE_BLOCK_END$|^; filament used \\[mm\\] =|^; total layers count =|^; printing object " /tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
* Exact artifact-validation result:
  * on-device artifact remains:
    * `/sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
    * size `375843`
    * timestamp `2026-04-18 12:24`
  * pulled host artifact:
    * `/tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
    * `sha256=5f79db9fe5d4c7c576e187b26cda0a0452c29216700cc1a1110eafca987abc14`
    * `13168` lines / `375843` bytes
* Deterministic signatures proving real-wrapper output:
  * header and block structure:
    * `; HEADER_BLOCK_START`
    * `; EXECUTABLE_BLOCK_START`
    * `; EXECUTABLE_BLOCK_END`
    * `; CONFIG_BLOCK_START`
    * `; CONFIG_BLOCK_END`
  * toolpath structure:
    * `;LAYER_CHANGE`
    * `;TYPE:Skirt`
    * `;TYPE:Bottom surface`
    * `;TYPE:Sparse infill`
    * `;TYPE:Internal Bridge`
    * `; printing object selected-model-tetrahedron_20mm_ascii_fresh.stl id:11484594880478881415 copy 0`
  * export metadata:
    * `; filament used [mm] = 213.53`
    * `; total layers count = 100`
    * `; machine_start_gcode = G28 ; home all axes\nG1 Z5 F5000 ; lift nozzle\n`
    * `; thumbnails = 48x48/PNG,300x300/PNG`
    * config lines including `extruder_type = Direct Drive`, `filament_type = PLA`, `sparse_infill_pattern = crosshatch`, `wall_generator = arachne`
* Exact contradiction against the reduced backend:
  * `engine-wrapper/orca-android-core/orca_android_core.cpp:117-140` shows the reduced backend can only emit:
    * `; generated by orca_core_android`
    * `; source=orcaslicer_android_wrapper`
    * `; facets=...`
    * `; model_height_mm=...`
    * `; layer_height_mm=...`
    * repeated `;LAYER:<n>` bounding-box moves
    * final `M400` / `M84`
  * none of those reduced-only signatures appear in the shipping artifact
  * the shipping artifact includes config serialization, feature-typed sections, object-id comments, and filament totals that the reduced generator does not implement
* Current exact conclusion:
  * the shipping app artifact on `RFCYA01ANVE` is honestly proven real-wrapper output
  * the output-validation boundary is closed; the next open question is repeatability across repeated fresh reruns
* Exact repeatability rerun commands used after the output-validation proof:
  * `./tools/adb -s RFCYA01ANVE shell svc power stayon true`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-unlocked-confirm.xml`
  * `./tools/adb -s RFCYA01ANVE shell logcat -c`
  * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
  * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell input tap 400 2800`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
  * `sleep 4`
  * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell input tap 1235 2840`
  * `sleep 2`
  * `./tools/adb -s RFCYA01ANVE shell ls -l /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-repeat-run1-export.xml`
  * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-repeat-run1-export.xml`
  * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode /tmp/mobileslicer-repeatability/run1.gcode`
  * rerun the same cleared-state sequence again, then:
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/mobileslicer-repeat-run2-export.xml`
    * `./tools/adb -s RFCYA01ANVE shell cat /sdcard/mobileslicer-repeat-run2-export.xml`
    * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode /tmp/mobileslicer-repeatability/run2.gcode`
  * `sha256sum /tmp/mobileslicer-repeatability/run1.gcode /tmp/mobileslicer-repeatability/run2.gcode`
  * `wc -l -c /tmp/mobileslicer-repeatability/run1.gcode /tmp/mobileslicer-repeatability/run2.gcode`
  * `diff -u /tmp/mobileslicer-repeatability/run1.gcode /tmp/mobileslicer-repeatability/run2.gcode`
  * `rg -n "^; generated by MobileSlicer|^; printing object |^; stop printing object |^; filament used \\[mm\\] =|^; total layers count =" /tmp/mobileslicer-repeatability/run1.gcode /tmp/mobileslicer-repeatability/run2.gcode`
  * `rg -v '^;' /tmp/mobileslicer-repeatability/run1.gcode > /tmp/mobileslicer-repeatability/run1.nocomments.gcode`
  * `rg -v '^;' /tmp/mobileslicer-repeatability/run2.gcode > /tmp/mobileslicer-repeatability/run2.nocomments.gcode`
  * `sha256sum /tmp/mobileslicer-repeatability/run1.nocomments.gcode /tmp/mobileslicer-repeatability/run2.nocomments.gcode`
  * `wc -l -c /tmp/mobileslicer-repeatability/run1.nocomments.gcode /tmp/mobileslicer-repeatability/run2.nocomments.gcode`
  * `diff -u /tmp/mobileslicer-repeatability/run1.nocomments.gcode /tmp/mobileslicer-repeatability/run2.nocomments.gcode`
* Exact repeatability evidence from the current boundary:
  * run 1:
    * device path: `/sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
    * host path: `/tmp/mobileslicer-repeatability/run1.gcode`
    * `sha256=d82daea5b499cbf5a61ece0b6627ccc2610eba678260aff60caafccfbfd4177c`
    * size `375843`
    * line count `13168`
  * run 2:
    * device path: `/sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
    * host path: `/tmp/mobileslicer-repeatability/run2.gcode`
    * `sha256=c6bd67b41aca2903146583d3603eec9c201b06e1579231477058b05a9289a173`
    * size `375459`
    * line count `13168`
  * both reruns retain the same real-wrapper signatures:
    * `; HEADER_BLOCK_START`
    * `; EXECUTABLE_BLOCK_START`
    * `; CONFIG_BLOCK_START`
    * `;TYPE:Skirt`
    * `;TYPE:Bottom surface`
    * `;TYPE:Sparse infill`
    * `;TYPE:Internal Bridge`
    * `; filament used [mm] = 213.53`
    * `; total layers count = 100`
  * exact drift source:
    * header timestamp comment differs
    * `; printing object ... id:...` / `; stop printing object ... id:...` comment metadata differs
    * there are `192` object-id comment lines and the run-1 object id is `2` characters longer, which exactly explains the `384`-byte file-size delta
    * after removing comment lines, both reruns become identical:
      * `sha256=90a0c6af40fe34a3ea0388c981425ddfee81aba9f15d9c73082ea17f759425e8`
      * `326741` bytes
      * `11682` lines
  * honest classification:
    * semantically stable but byte-different

### arm64-v8a Experimental ABI Proof Attempt

* Configure command used:
  * `cmake -S /home/peanut/Development/MobileSlicer/engine-wrapper/orca-android-libslic3r -B /tmp/orca-android-libslic3r-build-print-arm64 -DCMAKE_TOOLCHAIN_FILE=/home/peanut/Development/MobileSlicer/.android-sdk/ndk/26.3.11579264/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DORCA_ANDROID_LIBSLIC3R_ENABLE=ON -DORCA_ANDROID_BOOST_PREFIX=/tmp/orca-deps-install/arm64-boost-cgal -DORCA_ANDROID_BOOST_HEADERS_DIR=/tmp/orca-deps-install/arm64-boost-headers/include -DORCA_ANDROID_TBB_PREFIX=/tmp/orca-deps-install/arm64-tbb-static2 -DORCA_ANDROID_GMP_PREFIX=/tmp/orca-deps-install/arm64-gmp -DORCA_ANDROID_MPFR_PREFIX=/tmp/orca-deps-install/arm64-mpfr -DORCA_ANDROID_CGAL_PREFIX=/tmp/orca-deps-install/arm64-cgal -DORCA_ANDROID_OPENSSL_INCLUDE_DIR=/tmp/orca-deps-install/arm64-openssl/include -DORCA_ANDROID_OPENSSL_CRYPTO_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a -DORCA_ANDROID_OPENSSL_SSL_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libssl.a`
  * `-DORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0`
* Configure result:
  * succeeded with all dependency checks passing after ARM dependency staging including OpenSSL under `/tmp/orca-deps-install/arm64-openssl`
* Build command used:
  * `cmake --build /tmp/orca-android-libslic3r-build-print-arm64 --target orca_android_libslic3r_print_gcode_probe -j$(nproc)`
* Build result:
  * resolved the `BuildVolume.hpp` enum typing by explicit `signed char` underlying type
  * resolved the missing `boost::nowide::setenv` boundary with the same Boost source-equivalent path used on x86_64 (`ORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0`)
* `build result` at this boundary: configure succeeds, dependency checks pass, compile succeeds, and `orca_android_libslic3r_print_gcode_probe` now links and builds successfully
* runtime validation result:
  * confirmed ARM target and runtime proof:
    * `./tools/adb devices -l`
    * `./tools/adb -s RFCYA01ANVE shell getprop ro.product.cpu.abi`
    * `./tools/adb -s RFCYA01ANVE shell getprop ro.product.cpu.abilist`
    * `./tools/adb -s RFCYA01ANVE shell getprop ro.product.cpu.abilist64`
  * target output:
    * `RFCYA01ANVE` `device product:SM_S938U1`
    * `ro.product.cpu.abi`=`arm64-v8a`
    * `ro.product.cpu.abilist`=`arm64-v8a`
    * `ro.product.cpu.abilist64`=`arm64-v8a`
  * deployment + run sequence:
    * `./tools/adb -s RFCYA01ANVE push /tmp/orca-android-libslic3r-build-print-arm64/orca_android_libslic3r_print_gcode_probe /data/local/tmp/orca_android_libslic3r_print_gcode_probe`
    * `./tools/adb -s RFCYA01ANVE push /home/peanut/Development/MobileSlicer/engine-wrapper/orca-android-libslic3r/testdata/tetrahedron_20mm_ascii.stl /data/local/tmp/tetrahedron_20mm_ascii.stl`
    * `./tools/adb -s RFCYA01ANVE push /home/peanut/Development/MobileSlicer/engine-wrapper/orca-android-libslic3r/testdata/config_probe_valid.json /data/local/tmp/config_probe_valid.json`
    * `./tools/adb -s RFCYA01ANVE shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/tetrahedron_20mm_ascii.stl /data/local/tmp/config_probe_valid.json`
  * success signal:
    * `print.process() completed`
    * `_do_export` completion with `gcode_bytes=241256`
    * output written to `/data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
  * pulled host artifact:
    * `./tools/adb -s RFCYA01ANVE pull /data/local/tmp/orca_android_libslic3r_print_probe_output.gcode /tmp/orca_android_libslic3r_print_probe_output-arm64.gcode`
    * verified artifact size `241256` bytes
  * next exact blocker:
    * shipping integration boundary (outside isolated experimental target)
    * app-level ARM `orca_engine` link blocker is now cleared:
      * build command: `cd /home/peanut/Development/MobileSlicer/android-app && cmake --build app/.cxx/Debug/35l5eg2l/arm64-v8a --target orca_engine -j 6`
      * result: link success (no `getrandom` symbol error)
      * fix applied: include `android-app/app/src/main/cpp/getrandom_compat.cpp` in `orca_engine` target sources for `ORCA_SHIPPING_REAL_WRAPPER=ON`
      * failure point now removed: final shipping `orca_engine` link in `arm64-v8a`
    * next exact blocker: ARM runtime proof on a confirmed ARM `arm64-v8a` session after this link success

## Experimental Dependency Bring-Up

The first Phase 2 dependency milestone is now partially proven for `x86_64` Android.

### cereal

* Build status: integrated successfully for Android as a header-only dependency
* Proven Android strategy:
* use the exact upstream version already referenced by Orca build assets: `cereal` `v1.3.0`
* keep it isolated under `engine-wrapper/orca-android-libslic3r/third_party/cereal-1.3.0/`
* consume it as headers only from the experimental CMake path
* Required validation:
* `ORCA_ANDROID_CEREAL_PREFIX` must point at a root containing `include/cereal/cereal.hpp`
* the experimental CMake path fails early if either the include directory or the core header is missing
* Current result:
* the experimental Android target now compiles real `libslic3r` config headers that require `cereal`

### Boost

* Build status: builds successfully for Android as static libraries plus headers
* Proven Android strategy:
* Build with NDK CMake toolchain
* Keep `BUILD_SHARED_LIBS=OFF`
* Keep `BUILD_TESTING=OFF`
* Build the compiled subset needed by Orca-adjacent code: `filesystem`, `thread`, `iostreams`, `locale`, `regex`, `log`, `date_time`, `atomic`, `chrono`, `format`
* Keep additional header-only modules available for CGAL / `libslic3r`, including at minimum `math`, `polygon`, and `multiprecision`
* Required flags:
* `-DBUILD_SHARED_LIBS=OFF`
* `-DBUILD_TESTING=OFF`
* `-DBOOST_LOCALE_ENABLE_ICU=OFF`
* `-DBOOST_IOSTREAMS_ENABLE_BZIP2=OFF`
* `-DBOOST_IOSTREAMS_ENABLE_ZSTD=OFF`
* Current blocker / caveat:
* A component-filtered Boost install did not stage enough header-only modules for CGAL and `libslic3r`
* `libslic3r`-oriented Android builds therefore need the full relevant Boost header tree, not only the headers implied by a few compiled archives
* The proven Android strategy for headers is to assemble a unified tree from the Boost 1.84.0 source by copying all `libs/*/include/boost` directories (including nested modules like `libs/numeric/conversion/include/boost`)
* This provides all header-only modules including Beast, numeric/conversion, nowide headers, and any future transitive requirements
* `Boost.System` did not produce a standalone `libboost_system.a` in this Android build and is currently treated as header-only in the experimental path
* `Boost.Nowide` does not require a separate staged `libboost_nowide.a` for this path: the equivalent `libs/nowide/src/cstdlib.cpp` source is compiled directly from `ORCA_ANDROID_BOOST_SRC_ROOT` and linked into ARM/ x86_64 probe targets

### TBB

* Build status: builds successfully for Android as static libraries
* Proven Android strategy:
* Build oneTBB statically and link it as a normal native dependency
* Required flags:
* `-DBUILD_SHARED_LIBS=OFF`
* `-DTBB_TEST=OFF`
* Current blocker / caveat:
* Shared `tbbmalloc` failed on Android because linker version-script expectations did not match the produced symbols
* Setting `TBB_STRICT=OFF` broke Android compiler flag handling in oneTBB and produced invalid `=format-security` command-line fragments
* Static `libtbb.a` and `libtbbmalloc.a` are the currently proven Android-safe path

### GMP

* Build status: builds successfully for Android as static libraries
* Proven Android strategy:
* Cross-compile with the NDK clang toolchain through autotools
* Build static-only with PIC
* Enable `gmpxx`
* Required flags:
* `--host=x86_64-linux-android`
* `--enable-shared=no`
* `--enable-static=yes`
* `--enable-cxx=yes`
* `CFLAGS/CXXFLAGS` include `-fPIC -DPIC`

### MPFR

* Build status: builds successfully for Android as a static library
* Proven Android strategy:
* Cross-compile with the NDK clang toolchain through autotools
* Build static-only with PIC
* Point MPFR at the Android-built GMP prefix
* Required flags:
* `--host=x86_64-linux-android`
* `--enable-shared=no`
* `--enable-static=yes`
* `--with-gmp=<android-gmp-prefix>`
* `CFLAGS/CXXFLAGS` include `-fPIC -DPIC`

### CGAL

* Build status: header-only Android install succeeds and is consumable by the experimental target
* Proven Android strategy:
* Configure CGAL as header-only under the Android NDK toolchain
* Pair it with Android-built GMP / MPFR and a real Boost header tree
* Required flags:
* `-DCGAL_HEADER_ONLY=ON`
* `-DBUILD_TESTING=OFF`
* Current blocker / caveat:
* CGAL package-config consumption is less reliable than direct include-and-link wiring in this environment because its CMake path still expects desktop-style Boost discovery behavior
* The experimental Android path currently consumes CGAL headers directly and links GMP / MPFR explicitly instead of depending on CGAL’s package metadata to solve the full graph

### OpenSSL

* Build status: builds successfully for Android as static libraries
* Proven Android strategy:
  * Cross-compile OpenSSL 3.1.7 with the NDK clang toolchain through OpenSSL's own `Configure` script
  * Build static-only, no shared libraries, no tests
  * Keep it isolated under `ORCA_ANDROID_OPENSSL_INCLUDE_DIR` / `ORCA_ANDROID_OPENSSL_CRYPTO_LIBRARY`
* Required flags:
  * `./Configure android-x86_64 -D__ANDROID_API__=29 --prefix=<install> no-shared no-tests no-ui-console no-engine`
  * `ANDROID_NDK_ROOT` environment variable pointing at the NDK root
  * NDK toolchain `bin/` directory on `PATH`
* Current result:
  * `libcrypto.a` and `libssl.a` are available as static Android dependencies
  * `openssl/md5.h` and all required headers are staged
  * `Config.cpp` now compiles successfully against these headers for both x86_64 and arm64 staging paths

## First Real `libslic3r`-Oriented Target

The previous probe target has been replaced by a more-real target:

* `orca_android_libslic3r_dependency_subset`

What it proves today:

## Config Implementation Bring-Up

The config type milestone and config implementation linkage are both complete.

The experimental Android path now contains two implementation-backed targets:

* `orca_android_libslic3r_printconfig_impl_subset` — static library with `PrintConfig.cpp`
* `orca_android_libslic3r_config_impl_subset` — shared library with `Config.cpp`, `PrintConfig.cpp`, and all required support units

What these targets prove:

* construction of implementation-backed config objects
* representative calls such as `DynamicPrintConfig::full_print_config()`
* representative default-key config creation through `DynamicPrintConfig::new_from_defaults_keys(...)`
* helper calls from `Config.cpp` such as `escape_string_cstyle(...)`

Current result for Android `x86_64`:

* `Config.cpp` compiles successfully
* `PrintConfig.cpp` compiles successfully
* `printconfig_impl_subset` static library archives cleanly
* `config_impl_subset` shared library links successfully as `liborca_android_libslic3r_config_impl_subset.so`

### Included `libslic3r` Source Units

The config implementation target compiles and links the following `libslic3r` source files:

**Config core:**
* `Config.cpp` — config option definitions, serialization, environment variable application
* `PrintConfig.cpp` — all print/printer/filament config option definitions
* `libslic3r.cpp` — `SCALING_FACTOR` global
* `LocalesUtils.cpp` (shim) — locale-safe numeric formatting
* `MaterialType.cpp` — filament material type database

**Geometry (required by config bed-shape and polygon operations):**
* `Polygon.cpp`, `Polyline.cpp`, `ExPolygon.cpp`, `MultiPoint.cpp`
* `Line.cpp`, `Point.cpp`, `BoundingBox.cpp`
* `ClipperUtils.cpp` — polygon boolean operations
* `clipper.cpp` — vendored Clipper library with Slic3r namespace wrapping
* `Geometry.cpp` — geometric utilities
* `Circle.cpp` — arc segment operations
* `ArcFitter.cpp` — arc fitting for polylines

**Boost.Nowide:**
* `cstdlib.cpp` — compiled from Boost source for `setenv` support

**Stubs (linker-only, not exercised in config path):**
* `android_libslic3r_stubs.cpp` — provides `chain_clipper_polynodes`, `MedialAxis`, `GCodeThumbnails`, and `utils.cpp` functions

### Stubbed Subsystems

The following subsystems are stubbed rather than compiled to avoid pulling in large dependency trees:

* **ShortestPath** → `Print.hpp` → full slicing pipeline
* **MedialAxis / Voronoi** → `Voronoi.cpp` → Arachne wall generation → MultiMaterialSegmentation
* **GCodeThumbnails** → `Thumbnails.cpp` → jpeglib, qoi (image format libraries not staged)
* **utils.cpp** → boost::log, boost::locale, boost::filesystem (heavy compiled Boost modules)

### OpenSSL

* Build status: builds successfully for Android `x86_64` as static libraries
* Proven Android strategy:
  * Cross-compile OpenSSL 3.1.7 using the NDK clang toolchain through the OpenSSL `Configure` script
  * Build static-only with no tests, no UI console, no engine module
  * Target: `android-x86_64` with `__ANDROID_API__=29`
* Required flags:
  * `./Configure android-x86_64 -D__ANDROID_API__=29 --prefix=<install> no-shared no-tests no-ui-console no-engine`
  * `ANDROID_NDK_ROOT` must be set and NDK toolchain bin must be on `PATH`
* Current result:
  * `libcrypto.a` and `libssl.a` build and install cleanly for Android `x86_64`
  * `openssl/md5.h` and all required OpenSSL headers are available from the install prefix
  * `Config.cpp` now compiles with the staged Android OpenSSL headers

### `Config.cpp`

* Build status: compiles successfully
* Previous blocker: `openssl/md5.h` not found
* Resolution: staged Android-built OpenSSL 3.1.7 headers and static libraries for the experimental path
* Current result: `Config.cpp` compiles to an object file with no errors

### `PrintConfig.cpp`

* Build status: compiles successfully
* Previous blocker: `boost/beast/core/detail/base64.hpp` not found
* Resolution: replaced the component-filtered Boost install header tree with a unified header tree assembled from the full Boost 1.84.0 source tree (`libs/*/include/boost`)
* Current result: `PrintConfig.cpp` compiles to an object file with no errors; the `orca_android_libslic3r_printconfig_impl_subset` static library target builds and archives successfully

### Config Implementation Link Status

Both `Config.cpp` and `PrintConfig.cpp` compile and the `config_impl_subset` shared library links successfully on Android `x86_64`.

The linking target includes 15 real `libslic3r` source files plus 1 Boost.Nowide source and 1 stub file.
All previously reported undefined symbols are now resolved through either real implementations or controlled stubs.

### Current Honest Milestone Boundary

What is proven:

* Android dependency-backed `libslic3r` compilation works in the experimental path
* `PrintConfig`, `FullPrintConfig`, and `DynamicPrintConfig` types compile on Android
* `Config.cpp` and `PrintConfig.cpp` compile and link on Android
* the `config_impl_subset` shared library links successfully with all required support units
* OpenSSL 3.1.7 is proven buildable and consumable for Android `x86_64`
* the full Boost header tree (assembled from source) provides all header-only modules needed by config implementation units
* Boost.Nowide `setenv` is available through direct source compilation
* the geometry subsystem (Polygon, Clipper, BoundingBox, etc.) compiles for Android
* representative config behavior (`full_print_config()`, `new_from_defaults_keys()`, `escape_string_cstyle()`) is available in native code

What is not proven yet:

* full preset application
* slicing (`Slic3r::Print`)
* G-code generation (`Slic3r::GCode`)
* wrapper / JNI / app integration of the real config path

## Model Load Bring-Up

The experimental path now also includes:

* `orca_android_libslic3r_model_stl_probe`

What it proves today:

* real `Slic3r::Model` construction and STL loading compile and link on Android `x86_64`
* the Android executable runs inside Waydroid and loads a real STL through `Slic3r::load_stl(...)`
* runtime proof is real, not wrapper-backed or reduced-backend validation
* the current probe fixture loads successfully with:
  * `objects=1`
  * `volumes=1`
  * `instances=0`
  * `facets=4`

Additional real implementation units required beyond the config implementation milestone:

* `ObjectID.cpp` — `ObjectBase::s_last_id` and related object-id support used by `Model`
* `Geometry/ConvexHull.cpp` — 2D convex hull helpers used by `Model` and `TriangleMesh`
* `TriangleMesh.cpp` — real mesh container and STL-backed mesh operations
* `Model.cpp` — real `Slic3r::Model` implementation
* `Format/STL.cpp` — real STL import entrypoint
* `deps_src/semver/semver.c` — C semver helpers referenced by `Semver.hpp`
* `deps_src/qhull/...` — real 3D convex-hull support required by `TriangleMesh::its_convex_hull(...)`
* `deps_src/admesh/connect.cpp`, `normals.cpp`, `shared.cpp`, `stl_io.cpp`, `stlinit.cpp`, `util.cpp` — real STL parsing support used by `TriangleMesh::ReadSTLFile`

Model-load-specific isolation helpers:

* `android_libslic3r_model_stubs.cpp` keeps non-STL `Model.cpp` branches out of scope for this milestone:
  * OBJ / SVG / DRC / AMF / 3MF import entrypoints
  * STEP import class methods
  * backup helpers from `bbs_3mf` / `utils.cpp`
  * mesh boolean, face detection, build-volume, and triangle-selector helpers only referenced by broader model-editing code
* `shims/orca_android_libslic3r_model_preinclude.hpp` plus local shim headers short-circuit vendor `STEP.hpp` and `3mf.hpp` so STL-first Android bring-up does not require OpenCASCADE or Expat

What it does not prove yet:

* OBJ / AMF / 3MF / STEP import
* preset application onto a loaded model
* `Slic3r::Print`
* `Slic3r::GCode`
* connection to wrapper, JNI, or shipping app targets

## Real Config Application

The experimental path now also includes:

* `orca_android_libslic3r_config_json_probe`

What it proves today:

* flat wrapper-style JSON can be parsed on Android and translated into a real `DynamicPrintConfig`
* the Android executable links and runs inside Waydroid with no reduced-backend validator path involved
* representative config values round-trip through real Orca config storage and serialization
* missing required keys fail honestly before validation
* invalid values fail honestly through real `DynamicPrintConfig::validate(false)`

Representative wrapper keys proven:

* `layer_height` -> `layer_height`
* `first_layer_height` -> `initial_layer_print_height`
* `nozzle_diameter` -> `nozzle_diameter`
* `filament_diameter` -> `filament_diameter`
* `bed_temperature` -> `hot_plate_temp` and `hot_plate_temp_initial_layer`
* `print_temperature` -> `nozzle_temperature` and `nozzle_temperature_initial_layer`
* `travel_speed` -> `travel_speed`
* `perimeters` -> `wall_loops`

Android runtime proof with the checked-in valid fixture:

* `layer_height=0.2`
* `initial_layer_print_height=0.24`
* `nozzle_diameter=0.4`
* `filament_diameter=1.75`
* `hot_plate_temp=60`
* `hot_plate_temp_initial_layer=60`
* `nozzle_temperature=220`
* `nozzle_temperature_initial_layer=220`
* `travel_speed=120`
* `wall_loops=3`

No additional real `libslic3r` implementation units were required beyond the completed config implementation linkage milestone.
The existing `Config.cpp` / `PrintConfig.cpp` closure was sufficient; this milestone added an isolated Android probe plus JSON fixtures only.

Important implementation note:

* `DynamicPrintConfig::full_print_config()` seeds only the print-region subset in this Orca tree
* the probe therefore uses `DynamicPrintConfig::new_from_defaults_keys(...)` with the exact real stored option keys it needs
* wrapper-style aliases such as `first_layer_height` and `bed_temperature` are translated onto the actual stored Orca keys before validation and readback

What it does not prove yet:

* full preset coverage
* preset-file loading
* app / JNI / wrapper integration
* any slicing pipeline behavior

Next honest step after config application:

* prove one end-to-end native `Print` plus `GCode` path in the experimental Android `libslic3r` build
* replace current stubs only when that real print-path milestone requires them

## First Print + GCode Bring-Up

The experimental path now also includes:

* `orca_android_libslic3r_print_gcode_probe`

What is proven so far:

* the first isolated Android `Print` + `GCode` probe compiles successfully on Android `x86_64`
* the probe is fully outside the shipping app, wrapper API, JNI, and UI
* it reuses the real STL model-load path and the real wrapper-style JSON config translation path
* the probe attempts the real desktop-shaped sequence:
  * STL -> `Slic3r::Model`
  * wrapper-style JSON -> real `DynamicPrintConfig`
  * `Print::apply(...)`
  * `Print::process()`
  * `Print::export_gcode(...)`

Additional real implementation units added while expanding toward the first print path:

* `PrintBase.cpp`
* `PrintApply.cpp`
* `Print.cpp`
* `PrintObject.cpp`
* `PrintObjectSlice.cpp`
* `PrintRegion.cpp`
* `PlaceholderParser.cpp`
* `Layer.cpp`
* `LayerRegion.cpp`
* `Surface.cpp`
* `SurfaceCollection.cpp`
* `Slicing.cpp`
* `Flow.cpp`
* `Support/SupportMaterial.cpp`
* `GCode.cpp`
* `GCodeWriter.cpp`
* `Extruder.cpp`
* `ExtrusionEntityCollection.cpp`

Additional real implementation units added for the next slicing-core closure layer:

* `PerimeterGenerator.cpp` — clears the perimeter-generation entrypoints (`process_classic()` / `process_arachne()`)
* `ExtrusionEntity.cpp` — clears extrusion-entity RTTI and path simplification methods
* `SlicingAdaptive.cpp` — clears adaptive slicing helpers used by `Slicing.cpp`
* `PrincipalComponents2D.cpp` — clears geometry-analysis helpers used by bridge detection
* `Algorithm/RegionExpansion.cpp` — clears region-expansion entrypoints used by `LayerRegion.cpp`
* `Feature/Interlocking/InterlockingGenerator.cpp` — clears the interlocking entrypoint referenced by `PrintObjectSlice.cpp`
* `GCode/AvoidCrossingPerimeters.cpp` — clears the travel-planning entrypoint referenced by `GCode.cpp`

Additional real implementation units added for the essential runtime follow-up layer:

* `deps_src/clipper/clipper_z.cpp` — the correct Z-enabled Clipper runtime source for `ClipperLib_Z::*`
* `EdgeGrid.cpp` — real travel-planning support grid used by `AvoidCrossingPerimeters`
* `BridgeDetector.cpp` — real bridge-analysis helper used by `PerimeterGenerator`
* `VariableWidth.cpp` — real variable-width perimeter helper used by `PerimeterGenerator`
* `GCode/ToolOrdering.cpp` — real tool-order planning required directly by both `Print::process()` and `GCode::_do_export()`
* `GCode/ToolOrderUtils.cpp` — helper algorithms used by real `ToolOrdering.cpp`
* `FilamentGroup.cpp` — real filament-group helper logic now required by `ToolOrdering.cpp`
* `FilamentGroupUtils.cpp` — utility layer used by `FilamentGroup.cpp`
* `ParameterUtils.cpp` — parameter helper layer used by `ToolOrdering.cpp`
* `CustomGCode.cpp` — real custom-toolchange helper used by `ToolOrdering.cpp`
* `GCode/CoolingBuffer.cpp` — real cooling-state logic used by `GCode.cpp`
* `GCode/PrintExtents.cpp` — real print-extents accounting used by `GCode.cpp`

Additional experimental-only isolation helpers added for this milestone attempt:

* `android_libslic3r_print_stubs.cpp`
  * `resources_dir()`, `log_memory_info()`, `check_layer_id_pattern()`
  * path-ordering stubs for `chain_points(...)`, `chain_expolygons(...)`, `chain_and_reorder_extrusion_entities(...)`
  * single-probe ordering / print helpers for `chain_print_object_instances(...)`, `name_tbb_thread_pool_threads_set_locale()`, `make_brim(...)`, and `ConflictChecker::find_inter_of_lines_in_diff_objs(...)`
  * support-generation stubs from `Support/SupportCommon.hpp`
  * SVG debug-export stubs
  * narrow `WipeTower` compatibility stubs for single-extruder / no-prime-tower probe configs
  * narrow `WipeTower2` compatibility stubs for prime-tower codepaths that are not exercised by the single-extruder STL probe
  * `GCodeProcessor` compatibility static storage plus narrow helper / post-export stubs for `get_last_z_from_gcode(...)`, `get_last_position_from_gcode(...)`, `initialize(...)`, `check_multi_extruder_gcode_valid(...)`, and `finalize(...)`
  * a narrow `GCodeProcessor::process_file(...)` helper stub for the `export_gcode_from_previous_file(...)` branch
  * narrow `Utils` compatibility stubs for `rename_file(...)`, `get_utf8_sequence_length(...)`, and `Utils::get_current_time_utc()`
  * optional-branch stubs for `FlushPredict::calc_color_distance(...)`, Arachne path-parameter setup, and extra `SVG` / `png` debug overloads
  * non-runtime feature stubs for paint-based multi-material segmentation and fuzzy-skin grouping
  * interlocking entrypoint stub restored so the single-material STL probe does not chase voxel internals
* local experimental shims:
  * `shims/boost/log/trivial.hpp` extended to absorb stream manipulators such as `std::endl`
  * `shims/boost/nowide/cstdio.hpp` extended with `boost::nowide::remove`
* additional include roots:
  * `deps_src/libigl`
  * `deps_src/miniz`
* additional local shim headers:
  * `shims/png.h` keeps `EdgeGrid.cpp` buildable without staging libpng for this milestone

Current status:

* compile status: SUCCESS
* link status: SUCCESS
* runtime status: CLEAN COMPLETION
* G-code output status: CLEAN REAL OUTPUT

Current honest linker boundary after the next G-code runtime layer:

* the corrected Clipper-Z runtime source (`clipper_z.cpp`) now resolves the previous `ClipperLib_Z::*` blocker
* `EdgeGrid.cpp` now compiles in the experimental path under the local `png.h` shim
* `BridgeDetector.cpp` and `VariableWidth.cpp` are now included as real perimeter-runtime helpers
* `ToolOrdering.cpp` is now proven essential and included as real code:
  * `Print::process()` constructs `ToolOrdering` directly for both non-sequential and by-object flows
  * `GCode::_do_export()` also constructs and uses `ToolOrdering` directly for real export sequencing
* the small `utils.cpp` helper blockers (`rename_file(...)`, `get_utf8_sequence_length(...)`, `Utils::get_current_time_utc()`) are now resolved only by narrow experimental compatibility stubs, not by linking the full vendor `utils.cpp`
* the `GCodeProcessor` export bookkeeping / validation blockers (`initialize(...)`, `check_multi_extruder_gcode_valid(...)`, `finalize(...)`) are now resolved only by narrow experimental compatibility stubs, not by linking the full vendor `GCode/GCodeProcessor.cpp`
* the requested real G-code runtime layer is now included and the previous blocker set is cleared:
  * `CustomGCode::custom_tool_changes(...)`
  * `CoolingBuffer::reset(...)`
  * `get_print_extrusions_extents(...)`
  * `get_print_object_extrusions_extents(...)`
  * `get_wipe_tower_extrusions_extents(...)`
* that historical blocker layer has now been cleared
* the current state has moved beyond link-time closure:
  * the probe links and executes on Android `x86_64`
* runtime slicing/output is now cleanly completing to final export
  * still-likely optional/helper for the narrow single-extruder STL-only probe:
    * `TreeSupport::*`
      * source files: `vendor/orcaslicer/src/libslic3r/Support/TreeSupport.cpp`, `Support/TreeSupport3D.cpp`
      * role: tree-support generation; the probe does not enable support
    * `SupportSpotsGenerator::estimate_malformations(...)`
      * source file: `vendor/orcaslicer/src/libslic3r/SupportSpotsGenerator.cpp`
      * role: support/curl estimation helper
    * `FillAdaptive::*`
      * source file: `vendor/orcaslicer/src/libslic3r/Fill/FillAdaptive.cpp`
      * role: adaptive infill branch; the probe does not target adaptive infill
    * `FillLightning::*`
      * source file: `vendor/orcaslicer/src/libslic3r/Fill/FillLightning.cpp` and lightning generator units
      * role: lightning infill branch; not required for the narrow fixture
    * `WipeTower::*` and `WipeTower2::*`
      * source files: `vendor/orcaslicer/src/libslic3r/GCode/WipeTower.cpp`, `GCode/WipeTower2.cpp`
      * role: prime-tower / multi-material branches; not exercised by the single-extruder probe config
    * `Arachne::*`, `WallToolPaths::*`, `ExtrusionLine::is_contour()`, `extrusion_paths_append(...)`
      * source files:
        * `vendor/orcaslicer/src/libslic3r/Arachne/WallToolPaths.cpp`
        * `vendor/orcaslicer/src/libslic3r/Arachne/utils/ExtrusionLine.cpp`
      * role: nonessential if the narrow probe stays on the classic perimeter path
    * `TimelapsePosPicker::init(...)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/TimelapsePosPicker.cpp`
      * role: optional timelapse branch
    * `GCodeThumbnails::make_and_check_thumbnail_list(const ConfigBase&)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/Thumbnails.cpp`
      * role: optional thumbnail branch
    * `Slic3r::png::write_rgb_to_file_scaled(...)` plus multiple `SVG::draw*` overloads referenced only by `EdgeGrid` debug/export helpers
      * source files:
        * `vendor/orcaslicer/src/libslic3r/PNGReadWrite.cpp`
        * `vendor/orcaslicer/src/libslic3r/SVG.cpp`
      * role: nonessential debug/export helper behavior
    * `FlushPredict::calc_color_distance(...)`
      * source file: `vendor/orcaslicer/src/libslic3r/FlushVolPredictor.cpp`
      * role: filament grouping quality helper
    * `GCodeProcessor::get_gcode_last_filament(...)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/GCodeProcessor.cpp`
      * role: machine-start G-code helper
    * `FanMover::process_gcode(...)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/FanMover.cpp`
      * role: post-processing helper behavior
    * `SeamPlacer::init(...)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/SeamPlacer.cpp`
      * role: seam-placement feature branch
    * `CalibPressureAdvance::*`
      * source file: `vendor/orcaslicer/src/libslic3r/calib.cpp`
      * role: calibration branch
  * the constructor-side `GCodeProcessor` blocker is now cleared:
    * `CommandProcessor::{CommandProcessor,register_command,process_comand}`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/GCodeProcessor.cpp` support machinery
      * role: trie support for the partially stubbed `GCodeProcessor` path
      * current result: resolved in the experimental print stub layer and no longer present in the latest unresolved set
  * the next still-open essential runtime-support blockers exposed after that rebuild are:
    * `GCodeProcessor::reset()`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/GCodeProcessor.cpp`
      * role: essential runtime export initialization
      * proposed next action: add a narrow compatible stub or partially-real export-state implementation
    * `GCodeProcessor::apply_config(const PrintConfig&)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/GCodeProcessor.cpp`
      * role: essential runtime export initialization
      * proposed next action: add a narrow compatible stub or selectively lift the real config-application logic
    * `GCodeProcessor::enable_stealth_time_estimator(bool)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/GCodeProcessor.cpp`
      * role: essential runtime export initialization
      * proposed next action: add a narrow compatible stub
    * `PressureEqualizer::PressureEqualizer(const GCodeConfig&)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/PressureEqualizer.cpp`
      * role: essential runtime export helper once `GCode.cpp` is linked
      * proposed next action: link the real source file
    * `SmallAreaInfillFlowCompensator::SmallAreaInfillFlowCompensator(const GCodeConfig&)`
      * source file: `vendor/orcaslicer/src/libslic3r/GCode/SmallAreaInfillFlowCompensator.cpp`
      * role: essential runtime export helper once `GCode.cpp` is linked
      * proposed next action: link the real source file

Unresolved groups cleared by this layer:

* `PerimeterGenerator::process_classic()` / `process_arachne()`
* extrusion-entity RTTI and `ExtrusionPath::*simplify*`
* `SlicingAdaptive::*`
* `compute_principal_components(...)`
* `Algorithm::wave_seeds(...)`, `propagate_waves(...)`, `propagate_waves_ex(...)`, `merge_expansions_into_expolygons(...)`, `RegionExpansionParameters::build(...)`
* `InterlockingGenerator::generate_interlocking_structure(...)`
* `AvoidCrossingPerimeters::travel_to(...)`
* `ClipperLib_Z::*`
* `EdgeGrid::Grid::create(...)`
* `variable_width(...)`
* `BridgeDetector::*`
* `CustomGCode::custom_tool_changes(...)`
* `CoolingBuffer::reset(...)`
* `get_print_extrusions_extents(...)`
* `get_print_object_extrusions_extents(...)`
* `get_wipe_tower_extrusions_extents(...)`
* `CommandProcessor::{CommandProcessor,register_command,process_comand}`
* `PressureEqualizer::PressureEqualizer(const GCodeConfig&)`
* `SmallAreaInfillFlowCompensator::SmallAreaInfillFlowCompensator(const GCodeConfig&)`

`GCodeProcessor` status:

* full `GCode/GCodeProcessor.cpp` is still NOT linked
* the probe currently uses narrow compatibility stubs for:
  * `Reserved_Tags`
  * `Reserved_Tags_compatible`
  * `s_IsBBLPrinter`
  * `reset()`
  * `apply_config(const PrintConfig&)`
  * `enable_stealth_time_estimator(...)`
  * `get_last_z_from_gcode(...)`
  * `get_last_position_from_gcode(...)`
  * `initialize(...)`
  * `check_multi_extruder_gcode_valid(...)`
  * `finalize(...)`
  * `process_file(...)`
  * `get_gcode_last_filament(...)`
* the probe does NOT yet provide the deeper helper layer beneath those stubs:
  * `CachedPosition::reset()`
  * `CpColor::reset()`
  * `TimeProcessor::reset()`
  * `UsedFilaments::reset()`
  * `GCodeProcessorResult::reset()`
  * `process_buffer(...)`
  * `contains_reserved_tags(...)`
* this remains intentional for the current milestone because these are export bookkeeping / validation helpers around the emitted file, not the core path that computes slices or writes raw G-code moves

Proposed next action:

* keep interlocking isolated for the current milestone; this is honest because the probe is STL-only and single-material, and no evidence from runtime requirements points to voxel-based multi-material interlocking
* keep `SVG` / `PNGReadWrite` helper branches stubbed; they are only referenced by debug/export helpers
* keep Arachne-specific perimeter branches isolated if the narrow config stays on the classic perimeter path
* keep support, adaptive-infill, lightning-infill, and wipe-tower branches isolated unless the probe config proves they are exercised
* the latest rebuild clears the requested bookkeeping/config/time layer with real code where feasible:
  * real vendor units now linked: `GCodeReader.cpp`, `Time.cpp`
  * real lifted `GCodeProcessor` bookkeeping now present in `android_libslic3r_print_stubs.cpp`:
    * `CachedPosition::reset()`
    * `CpColor::reset()`
    * `TimeMachine::{State::reset(),CustomGCodeTime::reset(),reset()}`
    * `TimeProcessor::reset()`
    * `UsedFilaments::reset()`
    * `GCodeProcessorResult::reset()`
    * `contains_reserved_tags(...)`
  * real lifted `GCodeProcessor::reset()` and `GCodeProcessor::apply_config(const PrintConfig&)` are also now used instead of the earlier reduced bookkeeping stubs
* the next verified layer-feature bring-up now adds real source units for:
  * `ElephantFootCompensation.cpp`
  * `Fill/Fill.cpp`
  * `GCode/AdaptivePAProcessor.cpp`
  * `GCode/AdaptivePAInterpolator.cpp`
  * `GCode/SpiralVase.cpp`
  * `Arachne/utils/ExtrusionLine.cpp`
* that rebuild clears these runtime symbols from the unresolved set:
  * `elephant_foot_compensation(...)`
  * `AdaptivePAProcessor::{AdaptivePAProcessor,process_layer}`
  * `AdaptivePAInterpolator::{parseAndSetData,operator()}`
  * `SpiralVase::process_layer(...)`
  * `Arachne::ExtrusionLine::getLength() const`
* the verified fill-base closure is now substantially more real:
  * newly added real source units in the isolated print probe:
    * `vendor/orcaslicer/src/libslic3r/Fill/FillBase.cpp`
    * `vendor/orcaslicer/src/libslic3r/Fill/FillRectilinear.cpp`
    * `vendor/orcaslicer/src/libslic3r/Fill/FillCrossHatch.cpp`
    * `vendor/orcaslicer/src/libslic3r/ShortestPath.cpp`
    * `vendor/orcaslicer/deps_src/clipper2/Clipper2Lib/src/clipper.engine.cpp`
    * `vendor/orcaslicer/deps_src/clipper2/Clipper2Lib/src/clipper.offset.cpp`
    * `vendor/orcaslicer/deps_src/clipper2/Clipper2Lib/src/clipper.rectclip.cpp`
    * `vendor/orcaslicer/deps_src/clipper2/Clipper2Lib/src/clipper2_z.cpp`
  * real active-path fill defaults are now backed by Orca source:
    * `ipRectilinear`
    * `ipMonotonic`
    * `ipMonotonicLine`
    * `ipCrossHatch`
  * optional fill families surfaced only by the factory boundary remain isolated through experimental no-op key-function stubs:
    * concentric / concentric-internal
    * honeycomb / 3d-honeycomb
    * gyroid
    * TPMS D / TPMS FK
    * line / plane-path families
    * adaptive cubic / lightning fillers
* the first honest link is now complete
* additional real source units added to reach link:
  * `vendor/orcaslicer/src/libslic3r/GCode/SeamPlacer.cpp`
  * `vendor/orcaslicer/src/libslic3r/GCode/RetractWhenCrossingPerimeters.cpp`
  * `vendor/orcaslicer/src/libslic3r/ShortEdgeCollapse.cpp`
  * `vendor/orcaslicer/src/libslic3r/TriangleSetSampling.cpp`
  * `vendor/orcaslicer/src/libslic3r/NormalUtils.cpp`
* `SeamPlacer::place_seam(...)` and `RetractWhenCrossingPerimeters::travel_inside_internal_regions(...)` are now confirmed real runtime path, not optional helper branches
* optional or feature-scoped branches now isolated by narrow experimental stubs:
  * `TreeSupportData::{TreeSupportData,clear_nodes}`
  * `smooth_outward(...)`
  * `slice_mesh_slabs(...)`
  * `triangulate_expolygon_3d(...)`
* verified Android runtime result:
  * target built: `orca_android_libslic3r_print_gcode_probe`
  * runtime command:
    * `tools/adb shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/tetrahedron_ascii.stl /data/local/tmp/config_probe_valid.json`
  * repeated with larger fixture:
    * `tools/adb shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/tetrahedron_20mm_ascii.stl /data/local/tmp/config_probe_valid.json`
  * root cause of the previous `No layers were detected` failure:
    * the print probe was still linking the experimental `slice_mesh_ex(...)` stub from `android_libslic3r_model_stubs.cpp`
    * that stub returned an empty slice vector for every Z plane
  * minimum real runtime closure added for this run:
    * `vendor/orcaslicer/src/libslic3r/TriangleMeshSlicer.cpp`
  * conflicting slicer stubs removed from the executed path:
    * `slice_mesh_ex(...)`
    * `slice_mesh_slabs(...)`
    * `project_mesh(...)`
  * 20 mm fixture runtime result now:
    * `print.process()` completes on Android `x86_64`
    * `_do_export()` completes and closes the output stream
    * final real output is emitted at `/data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
    * execution logs show `gcode_bytes=241256` for `tetrahedron_20mm_ascii.stl`
    * representative first lines:
      * `; HEADER_BLOCK_START`
      * `M201 X1000 Y1000 Z500 E5000`
  * current status after this run:
    * no remaining export failure was observed for the verified command
    * final output now exists at the target `.gcode` path

What is not proven yet:

* shipping-app integration of the experimental path
* final ARM (production) Android ABI proof of this closed experimental pipeline
* successful `arm64-v8a` configure/build proof for the isolated experimental path has now been reached (`orca_android_libslic3r_print_gcode_probe` links successfully); the next boundary is shipping integration link completion on ARM `app` target (`orca_engine`)

## Debug Build Flow

* `cd android-app`
* `./gradlew :app:assembleDebug`
* APK includes:
* `lib/arm64-v8a/liborca_engine.so`
* `lib/arm64-v8a/liborca_core_android.so`
* `lib/x86_64/liborca_engine.so`
* `lib/x86_64/liborca_core_android.so`

## Strict SliceBeam Comparison Boundary

Use this rule for third-party apples-to-apples Benchy comparisons:

* accepted exact MobileSlicer fixture identity is:
  * `/data/local/tmp/3DBenchy.stl`
  * same device copy `/sdcard/Download/3DBenchy.stl`
  * `sha256 6ab57f1c3f8e86bc3cbd302c6fa6270acf06277c6335454e922419c25d42e97e`
* do not substitute a third-party app's bundled calibration Benchy unless its hash matches exactly
  * current reviewed SliceBeam bundled asset does not match:
    * `app/src/main/assets/models/3dbenchy.stl`
    * `sha256 a0afa505090b6f16cb6bdcfad3b843ec5b3b540b8357c306d447d0b324b051bc`
* current reviewed SliceBeam import mechanics from source are:
  * in-app `Intent.ACTION_OPEN_DOCUMENT`
  * `Intent.ACTION_VIEW` only when Android grants a readable `Uri`
* exact host-automation limit seen on 2026-04-23:
  * `adb shell am start ... ACTION_VIEW content://com.android.externalstorage.documents/...`
    failed with Android `SecurityException` because shell UID `2000` did not own the document grant
* workflow consequence:
  * exact same-model SliceBeam comparison must use either:
    * a real picker-granted import path inside SliceBeam
    * or a user-assisted/manual on-device import step that records the exact source file and hash
* completed 2026-04-23 proof outcome:
  * the real picker-granted SliceBeam flow was completed on `RFCYA01ANVE` for the exact accepted file:
    * imported `3DBenchy.stl`
    * sliced it in `5.100 s`
    * exported `/sdcard/Download/3DBenchy.gcode`
  * exact exported-output comparison versus the accepted MobileSlicer artifact:
    * SliceBeam:
      * `3707781` bytes
      * `146211` lines
      * `240` layer markers
      * `4284.00 mm` filament
    * accepted MobileSlicer:
      * `3479325` bytes
      * `106669` lines
      * `240` layer markers
      * `3430.20 mm` filament
  * exact visible/default mismatch captured from the proof:
    * SliceBeam used `perimeters = 3`, `fill_density = 20%`, `fill_pattern = gyroid`, `skirts = 1`
    * accepted MobileSlicer used `wall_loops = 2`, `sparse_infill_density = 15%`, `sparse_infill_pattern = grid`, `skirts = 2`
  * decision consequence:
    * the completed same-file proof does not support the theory that SliceBeam is faster mainly because it emits a materially cheaper exported workload
    * return to native runtime hotspot work instead of spending more time on third-party default-alignment comparison

## Phase 2 Build Milestones

1. The experimental Android `libslic3r` path configures with NDK CMake without touching the shipping app targets.
2. A headless Android-native target can compile against generated Orca version headers and selected upstream include roots.
3. Boost, TBB, GMP, MPFR, and CGAL are either buildable for Android or documented with an explicit blocker.
4. A dependency-backed native target can compile real `libslic3r` execution-policy code on Android.
5. `PrintConfig` / `DynamicPrintConfig` headers compile once `cereal` and the required transitive includes are staged.
6. Config implementation units compile and link in the experimental path.
7. Real `Slic3r::Model` loading works in the experimental path for STL import.
8. Wrapper-owned JSON is translated into real `DynamicPrintConfig` in the experimental path.
9. One end-to-end native `Print` plus `GCode` path works in the experimental path.
10. The wrapper backend switches from `orca-android-core` to the real Android `libslic3r` path with no JNI or UI contract change.
11. Shipping integration boundary on `arm64-v8a` app/native link is cleared.
