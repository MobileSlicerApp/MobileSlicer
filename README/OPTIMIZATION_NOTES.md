# Mobile Slicer Optimization Notes

Last updated: 2026-04-25

## Current State

- Debug APK was built before the latest lifecycle cleanup with:
  - `cd android-app && ./gradlew assembleDebug`
- The main UI has been split out of `MainActivity`:
  - `ModelLoaderScreen.kt`: app shell, home/import flow, screen routing, export launcher.
  - `WorkspaceScreen.kt`: workspace viewer, transform controls, preview layer controls.
  - `ProfilesScreen.kt`: profile landing, profile lists, profile editor dialogs.
  - `SettingsScreen.kt`: theme/accent/about settings.
  - `AppUi.kt`: shared app UI colors/helpers.
- `MainActivity.kt` is now focused on Android lifecycle, native engine setup/destruction, model staging/loading, native slicing, export/share, preferences, and automation entry.
- Automation was split into:
  - `AutomationSliceRunner.kt`: headless slice execution.
  - `AutomationConfigResolver.kt`: automation config/profile override resolution.
- G-code summary parsing lives in `GcodeSummaryParser.kt` with focused unit coverage.
- Shared workspace/result models live in `WorkspaceModels.kt`.

## Latest Cleanup

- Started the slice-output memory cleanup:
  - Added `orca_write_gcode_to_file(...)` and `nativeWriteGcodeToFile(...)`.
  - Interactive slicing now writes the latest G-code to an app-cache file and stores only that file path in retained workspace state.
  - Export/share now stream/copy from the cached G-code file instead of retaining and rewriting a Kotlin `String`.
  - Automation slicing now writes the requested output through the native file-write bridge instead of pulling the full G-code text through JNI.
  - Kotlin summary display now uses the native summary path; the previous full-G-code parser remains available for focused tests/fallback work but is no longer on the normal interactive slice path.
- Rejected the sampled Prepare-view mesh experiment:
  - Device review showed visible holes/distortion on a detailed STL.
  - The STL viewer must preserve source geometry; future performance work should reduce copies, cache data, or move parsing/upload ownership without dropping triangles.
- Rejected the smoothed display-normal experiment:
  - Device review showed worse performance and unacceptable visual results.
  - Keep STL viewer normals on the direct parsed STL path until a better full-fidelity rendering approach is proven on device.
- Replaced deprecated workspace lifecycle owner import with `androidx.lifecycle.compose.LocalLifecycleOwner`.
- Added `androidx.lifecycle:lifecycle-runtime-compose` through the Gradle version catalog.
- Optimized single-model viewer updates:
  - `TouchModelViewerView` now treats STL mesh changes and model transform changes separately.
  - Vertex/normal GL buffers are rebuilt only when the active mesh changes.
  - Placement, matrix, camera focus, and scene span are refreshed for transform-only updates without re-uploading geometry.
- Audited the existing multi-object and G-code preview paths:
  - Plate-object geometry, transform, and selection versions are already split so transform/selection updates avoid object-buffer rebuilds.
  - G-code preview source/reload changes and layer-range changes are already split so layer scrubbing stays a cheap range update.
- Added viewer update-decision regression coverage:
  - `ViewerUpdateDecisions.kt` centralizes pure comparison logic for plate-object uploads and G-code preview/layer updates.
  - `ViewerUpdateDecisionsTest.kt` covers transform-only, selection-only, geometry identity, object order, preview source normalization, layer-range, range-commit, and reload-token decisions.
  - G-code Preview range scrubbing is intentionally live: slider drag ticks update the existing native viewer's visible layer window, while release records the final range without forcing a native preview reload.
- Stabilized pinch zoom after on-device review showed a sticky zoom transition:
  - `ViewerCamera` now scales the whole camera distance by zoom instead of mixing a zoomed span term with a fixed close-range distance term.
  - Small pinch scale changes are no longer dropped by a zoom deadband.
  - `TouchModelViewerView` keeps two-finger centroid pan active during two-finger gestures, so pan is preserved while pinch zoom remains smooth.
  - Max zoom is widened for close STL/toolpath inspection.
  - `ViewerCameraTest.kt` covers full-distance zoom scaling and small pinch scale changes.
- Validated the debug APK on a connected Samsung `SM-S938U1`:
  - Installed with `adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk`.
  - Cold-launched `com.mobileslicer/.MainActivity` in `421ms`.
  - Confirmed the landing UI hierarchy is present and there were no fatal startup logs.
  - Ran automation slicing from an app-private STL smoke fixture; native slicing succeeded in `73ms` and wrote a `246K` G-code file.
- Ran 3DBenchy validation on the Samsung device using `/home/peanut/Documents/3DBenchy.stl`:
  - Source STL is binary, `225,706` triangles, `11M` on disk.
  - A transform-only viewer update for this model now avoids rebuilding/resending about `15.5 MiB` of vertex/normal GL buffer data.
- Optimized automation placement:
  - `AutomationSliceRunner` now applies the same bed-centered native STL transform used by the UI slice path.
  - `StlMeshParser.parseBounds` adds a lightweight binary STL bounds scan so automation placement does not allocate full viewer vertex/normal arrays.
  - Raw `/home/peanut/Documents/3DBenchy.stl` now slices successfully without a temporary pre-centered copy.
  - Samsung result for raw Benchy after the change: `placementMs=53`, `elapsedMs=1.751s`, output G-code `2.9M`.
  - Added JVM coverage in `NativeModelPlacementTest.kt` and `StlMeshParserBoundsTest.kt`.
- Reverted the large G-code preview shell/window experiment:
  - Device review showed it degraded both small and large G-code preview readability.
  - Keep the existing `Range`/`Single` behavior until the preview path can be fixed behind a safer implementation.
- Reverted the follow-up G-code preview extrusion-width scaling experiment:
  - Device review showed span-based render thinning made full-range previews look worse, especially on the small Benchy preview.
  - Current APK is back to the prior native `libvgcode` preview behavior: selected G-code ranges are rendered directly, without shell reconstruction, window replacement, or width scaling.
  - Record this as a guardrail: do not optimize G-code preview by mutating the selected range, replacing it with a shell, or changing extrusion dimensions unless the change is isolated behind an opt-in/debug path and validated on both small and large G-code.
- Deleted the unsafe large-model G-code preview LOD path from normal Preview:
  - Removed emergency shell/top/support preview and automatic loaded-layer window shrinking.
  - Preview now renders exact selected G-code ranges up to the `1.2M` selected-vertex phone cap.
  - The older Lite / balanced Lite fallback path is no longer accepted for the
    normal user path because silent degradation made Preview less trustworthy.
  - If the selected exact range is too large, Preview reports an explicit limit
    error and asks the user to narrow the layer range.
  - Removed the render-thread retry that silently replaced a failed selected range with a single-layer preview.
- Extended bounds-first placement to the regular UI path:
  - `ModelLoadResult`, `PlateObject`, and `WorkspaceSessionViewModel` now retain STL bounds separately from the full viewer mesh.
  - `loadModelFromUri` parses lightweight bounds during import after staging the original STL.
  - `sliceCurrentModel` can use full mesh bounds when available, or retained import bounds while viewer mesh parsing is still in progress.
  - The Slice button can become available from bounds metadata; STL transform controls and rendering still require the full-fidelity `StlMesh`.
  - This does not alter STL viewer geometry, G-code viewer rendering, or native G-code generation; bounds only drive native placement.
  - Samsung raw Benchy verification after this change: `placementMs=50`, `elapsedMs=1.910s`, output G-code `2.9M`.
- Added prepared viewer mesh caching:
  - `PreparedViewerMeshCache` is a small LRU cache keyed by canonical path, file size, and modified time.
  - `prepareWorkspaceMesh` reuses cached full-fidelity `StlMesh` data for unchanged staged files and records cache hits in `WorkspacePreparationTiming`.
  - Cache entries are successful full mesh parses only; the cache is cleared when the activity is permanently destroyed.
  - This does not change STL geometry, native slicing, G-code output, or G-code preview rendering.
  - Added JVM coverage in `PreparedViewerMeshCacheTest.kt`.
  - Samsung raw Benchy guardrail after this cache change: `placementMs=54`, `elapsedMs=1.646s`, output G-code `2.9M`.
- Started splitting `TouchModelViewerView.kt` into focused viewer modules:
  - `ViewerModels.kt`: viewer-facing state/data types such as failures, transforms, plate objects, appearance, and STL placement points.
  - `ViewerGlModels.kt`: passive GL upload/program/object-upload structs.
  - `ViewerPlacement.kt`: model placement, transformed bounds, point transforms, and bed-centered default transform helpers.
  - `ViewerBedGeometry.kt`: pure bed plate, grid, border, wall, and selected-footprint geometry builders.
  - `ViewerPicking.kt`: ray/projection picking and pickable object bounds helpers.
  - `ViewerCamera.kt`: orbit, pan, zoom, focus, scene-span, and camera matrix helpers.
  - `ViewerAppearanceColors.kt`: viewer color derivation plus future-ready per-color override hooks for customizable viewer colors.
  - Settings now separates app accent from 3D world-space color: app accent still controls the model, while world color controls the viewer clear/background space.
  - Cleaned up touch selection rules so a pinch/pan gesture no longer falls through into a tap selection on pointer release.
  - `TouchModelViewerView.kt` dropped from about `2073` lines to about `1309` lines without changing render behavior.
  - JVM tests pass after the split with `./gradlew testDebugUnitTest`.
  - Debug APK builds after the split with `./gradlew assembleDebug`.
- Added a repeatable Android verification workflow:
  - `scripts/verify_android.sh` wraps unit tests, debug APK builds, device install/launch smoke, and app-private automation slicing.
  - `README/VERIFICATION.md` documents local, device, and Benchy verification commands.
  - Benchy mode stages STL files into app-private storage before calling `com.mobileslicer.action.AUTOMATE_SLICE`, avoiding scoped-storage raw-read failures.
- Added the advanced STL feature architecture note:
  - `README/ADVANCED_STL_FEATURES.md` maps cut, seam painting, fuzzy-skin painting, and multicolor support to Orca's native `ModelObject` / `ModelVolume` / facet-annotation model.
  - The first implemented slice is profile-level fuzzy-skin process config through Kotlin profiles and the native wrapper.
  - Per-facet painting and cut UI remain blocked on durable project/session state.
- Continued the STL/viewer optimization pass:
  - Tested one interleaved position/normal GL buffer for STL/bed triangle
    uploads, but backed it out after Prepare felt slower on device. The current
    parser still produces separate arrays, so interleaving added an extra CPU
    pass before upload.
  - `PreparedViewerMeshCache` now has a retained-byte budget in addition to the
    entry-count LRU, skips meshes larger than the budget, and exposes retained
    bytes for cache-hit/miss logging.
  - Interactive slicing now treats a missing or incomplete native G-code summary
    as a post-slice error instead of fabricating a byte-count-only placeholder.
  - Triangle shader/program compilation and handle lookup moved out of
    `TouchModelViewerView.kt` into a focused `ViewerTriangleProgram` helper;
    EGL/render-thread lifecycle remains unchanged.
- Added native G-code Preview measurement logs:
  - `gcode_preview_cache` now records cache build vertices, layers, source
    bytes, completeness, vertex-limit state, and parse time.
  - `gcode_viewer_load_latest_slice` now records cache mode, cache hit/build
    state, cached vertices/layers, selected exact vertices, selected-range
    parse time, `libvgcode` load/upload time, total native load time, and
    `fidelity=exact`.
  - This is measurement-only; it does not change Preview geometry, layer-range
    behavior, emitted G-code, or exact-preview limits.

## Verification Commands

Run these after each optimization/refactor:

```bash
scripts/verify_android.sh local
```

If only an APK build is needed:

```bash
scripts/verify_android.sh apk
```

See `README/VERIFICATION.md` for device and Benchy automation modes.

## Known Repository State Notes

- `engine-wrapper/orca-android-libslic3r/android-deps-build` and `engine-wrapper/orca-android-libslic3r/android-deps-install` were removed from git tracking with `git rm -r --cached`; files remain on disk and the paths are ignored.
- The worktree had many unrelated pre-existing modified files before the cleanup pass. Do not revert unrelated files unless explicitly requested.

## Recommended Optimization Roadmap

Priority order from the April 25 repo audit:

1. Stop retaining full emitted G-code in Kotlin.
   - Status: completed for the normal slice/export/share path.
   - Normal app and automation flows now use a native file-write bridge and retained state stores a file path.
   - Native wrapper now keeps the exported temp G-code file as the source artifact and only lazy-loads text for legacy callers or preview loading.
2. Make native summary data authoritative enough that Kotlin does not parse full G-code after slicing.
   - Status: completed for the normal interactive path.
   - Normal app summary display asks `nativeGetGcodeSummary(...)`.
   - Missing or incomplete native summary data now surfaces a post-slice error
     instead of falling back to a fabricated byte-count-only summary.
3. Reduce STL viewer memory during GL upload.
   - Status: pending after sampled-mesh rejection, prepared-mesh cache addition,
     and backed-out interleaved-upload attempt.
   - Do not interleave on the render thread while the parser still returns
     separate `FloatArray`s; that can trade memory for slower Prepare startup.
   - Follow-up: consider preparing upload-ready direct/interleaved memory during
     parsing if device memory traces still show avoidable transient pressure.
4. Add a bounded Prepare-view mesh LOD path.
   - Status: rejected for STL surface meshes.
   - Do not decimate/drop STL triangles in Prepare; any future level-of-detail work must be opt-in or use a representation that does not create holes in the visible model.
5. Cache prepared viewer meshes by staged file identity.
   - Status: started.
   - Keyed by canonical path plus size/mtime with a small LRU.
   - Cache now also has a retained-byte budget and logs retained MB on
     cache-hit/miss paths.
   - Follow-up: manually verify cache-hit status through duplicate object/workspace recovery flows.
6. Split `TouchModelViewerView.kt` into smaller rendering components.
   - Status: started.
   - Completed extractions so far: viewer models, GL data structs, placement math, bed geometry builders, selected-footprint geometry, picking math, camera math, and appearance color derivation.
   - GL shader/program ownership is now split into `ViewerTriangleProgram`.
   - Next safe extraction: triangle upload/buffer ownership, leaving EGL/thread
     lifecycle in place until native G-code bridge work stabilizes.
7. Keep Preview layer filtering cheap.
   - Status: covered by regression tests and native timing logs.
   - Preserve the current split between source/reload changes and layer-range updates; add device measurements around preview reload tokens.
8. Keep generated native dependency outputs out of source control.
   - Status: completed.
   - Continue treating `engine-wrapper/orca-android-libslic3r/android-deps-build` and `engine-wrapper/orca-android-libslic3r/android-deps-install` as ignored/generated state.
9. Keep large G-code preview readable without touching G-code output.
   - Status: reset after large-preview optimization rejection.
   - Rejected follow-up: span-based extrusion-width scaling made full-range previews look worse on device.
   - Rejected follow-up: role-aware/emergency native LOD made large previews visually untrustworthy and was removed from the normal path.
   - Current accepted boundary: render exact selected native `libvgcode` ranges only; fail clearly if the range is too large for the phone vertex limit.
   - Avoid Android-side preview geometry rewrites, STL-like shell proxies, layer-window substitution, automatic extrusion-width/height scaling, and role-based segment dropping in the normal user path.
   - Future large-preview optimizations must be opt-in/debug-only first and need device validation on both small and large G-code before becoming the default.

## Recommended Next Optimization

Continue runtime validation on device:

1. Load a larger real STL manually on the Samsung device and verify transform controls update smoothly without viewer reset artifacts.
2. Load multiple plate objects and verify selection/transform changes do not trigger loading overlays.
3. Scrub G-code preview layers and confirm the preview updates without a full reload unless the preview source changes.
4. Validate export/share from the new cached G-code file path.
5. Measure memory before and after a slice using `dumpsys meminfo` to quantify the Kotlin string-retention removal.
6. Manually import raw 3DBenchy through the UI and tap Slice before the first STL frame finishes preparing to validate the bounds-first UI path under real interaction.
7. If device behavior matches the JVM coverage, move to the native-side temp-file retention follow-up from roadmap item 1.

## Samsung Smoke-Test Recipe

Use USB serial `RFCYA01ANVE` when both USB and TCP adb entries are present:

```bash
adb -s RFCYA01ANVE install -r android-app/app/build/outputs/apk/debug/app-debug.apk
adb -s RFCYA01ANVE shell am force-stop com.mobileslicer
adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity
adb -s RFCYA01ANVE shell dumpsys gfxinfo com.mobileslicer
adb -s RFCYA01ANVE shell dumpsys meminfo com.mobileslicer
```

For automation smoke tests, put the model under the app-private files directory with `run-as` and pass absolute app-private paths to `AUTOMATE_SLICE`. Shared storage paths such as `/sdcard/Download` may be blocked by scoped storage for raw file reads.

Secondary cleanup after that:

- Prune unused imports created by mechanical file splits.
- Consider moving native geometry placement helpers out of `MainActivity` if they become shared.
