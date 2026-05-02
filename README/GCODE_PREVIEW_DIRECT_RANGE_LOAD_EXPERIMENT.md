# G-code Preview Direct Range Load Experiment

Date: 2026-04-25

Status: validated on device. The direct range-load path plus exact-preview cap
increase made large G-code preview substantially better in manual testing.

## Goal

Reduce G-code preview layer-range memory spikes by avoiding the wrapper-side
temporary filtered `GCodeInputData` allocation when a full cached preview is
available.

Follow-up: remove the old lite/balanced-lite degradation path. Preview ranges
above the old 400k vertex threshold now stay exact up to the exact preview cap.
If a selected range is still too large, the viewer reports an exact-preview
limit error instead of silently rendering a simplified preview.

Important behavior to preserve: do not reintroduce automatic striding for large
preview ranges. The old degraded mode hid missing geometry behind a successful
preview. The current behavior deliberately prefers exact output: render exactly
up to `kMaxPreviewVertices`, then show an explicit exact-preview limit error and
ask the user to narrow the layer range.

Before this experiment, the cache-hit preview path was:

1. Keep full parsed preview data in `OrcaEngineImpl::cached_preview_input`.
2. Build a filtered `viewer->input_data` with `build_gcode_input_layer_range(...)`.
3. Move that filtered data into `libvgcode::Viewer::load(...)`.

After this experiment, the cache-hit preview path is:

1. Keep full parsed preview data in `OrcaEngineImpl::cached_preview_input`.
2. Call `libvgcode::Viewer::load_layer_range(...)`.
3. Let libvgcode count the selected vertices, reject too-large ranges so the
   app can show an explicit exact-preview limit error, and build its internal
   `m_vertices` directly from the cached data.

STL preview code is not part of this experiment.

## Exact Large Preview Change

This follow-up is the part that made the large-model G-code viewer visibly
better:

- `engine-wrapper/orca_wrapper.cpp`
  - `kMaxPreviewVertices` changed from the old degraded-preview threshold to the
    current exact mobile budget of `1000000`.
  - `kMaxCachedPreviewVertices` is also `1000000`.
  - `orca_gcode_viewer_load_latest_slice(...)` no longer retries with
    lite/balanced-lite stride values when the exact selected range reaches the
    old cap.
  - Oversized selected ranges now fail with an exact-preview limit message
    instead of silently rendering simplified geometry.
  - Preview load logs now report `fidelity=exact` only.

- `to_vgcode_input_data_from_gcode_text(...)`
  - Removed the `lite_stride` and `balanced_lite_stride` parameters.
  - Removed role-based segment skipping.
  - The parser now either keeps exact extrusion vertices or stops at the exact
    preview limit.

This means a G-code preview with more than the old degraded-preview threshold is
no longer degraded. It remains accurate up to 1M selected preview vertices. For
models above that, the user should narrow the visible layer range rather than
accepting inaccurate geometry.

## Files Touched

- `engine-wrapper/orca_wrapper.cpp`
- `vendor/orcaslicer/src/libvgcode/include/Viewer.hpp`
- `vendor/orcaslicer/src/libvgcode/src/Viewer.cpp`
- `vendor/orcaslicer/src/libvgcode/src/ViewerImpl.hpp`
- `vendor/orcaslicer/src/libvgcode/src/ViewerImpl.cpp`

## Revert To Previous Conservative Implementation

Use these steps to revert this experiment while keeping the previous lower-risk
allocation improvement.

1. In `vendor/orcaslicer/src/libvgcode/include/Viewer.hpp`, remove:
   - `#include <cstddef>` if no longer needed
   - `size_t load_layer_range(const GCodeInputData& gcode_data, long min_layer, long max_layer, size_t max_vertices);`

2. In `vendor/orcaslicer/src/libvgcode/src/Viewer.cpp`, remove:
   - `Viewer::load_layer_range(...)`

3. In `vendor/orcaslicer/src/libvgcode/src/ViewerImpl.hpp`, remove:
   - `#include <cstddef>` if no longer needed
   - `size_t load_layer_range(...)`
   - `void load_vertices(...)`

4. In `vendor/orcaslicer/src/libvgcode/src/ViewerImpl.cpp`, remove:
   - `ViewerImpl::load_layer_range(...)`
   - `ViewerImpl::load_vertices(...)`
   - the shortened `ViewerImpl::load(...)`

   Restore `ViewerImpl::load(GCodeInputData&& gcode_data)` to contain the
   original load body directly:
   - check initialized
   - check `gcode_data.vertices.empty()`
   - `reset()`
   - move `gcode_data.vertices`, `tools_colors`, and `color_print_colors` into
     the member fields
   - resize `m_vertices_colors`
   - set `m_settings.spiral_vase_mode`
   - run the existing per-vertex setup, GPU upload setup, and color/range update
     code that currently lives in `load_vertices(...)`

5. In `engine-wrapper/orca_wrapper.cpp`, restore the helper named
   `build_gcode_input_layer_range(...)` just below `gcode_input_layer_count(...)`.
   The previous conservative helper:
   - copied `source.spiral_vase_mode`, `tools_colors`, and `color_print_colors`
   - counted selected vertices first
   - reserved exactly the selected count, capped at `kMaxPreviewVertices`
   - copied selected `PathVertex` values
   - renumbered `vertex.layer_id` by subtracting `min_layer` when filtering

6. In `orca_gcode_viewer_load_latest_slice(...)`, restore the cache-hit branch to:
   - assign `viewer->input_data = build_gcode_input_layer_range(...)`
   - use `viewer->input_data.vertices.size()` for empty/limit checks and logging
   - always call `viewer->viewer.load(std::move(viewer->input_data))` before
     setting view type and time mode

7. Re-run:

```bash
cd android-app
./gradlew testDebugUnitTest
./gradlew :app:externalNativeBuildPerfDebug
```

## Validation Expected

The preview should remain visually accurate because selected `PathVertex` values
and layer renumbering are unchanged. The intended improvement is lower peak RAM
and fewer temporary allocations when loading a selected range from the cached
full preview.

The wrapper no longer uses lite/balanced-lite striding for oversized G-code
preview ranges. Accuracy is preferred over degraded fallback rendering.

## Validation Completed

Commands run after the direct range-load and exact large-preview changes:

```bash
cd android-app
./gradlew testDebugUnitTest
./gradlew :app:externalNativeBuildPerfDebug
./gradlew :app:installPerfDebug -Pandroid.injected.device.serial=RFCYA01ANVE
```

Results:

- Unit tests passed.
- Native build passed for `arm64-v8a` and `x86_64`.
- Perf debug APK installed to the connected `SM-S938U1`.
- Manual device check confirmed the large G-code preview is accurate and much
  better than the previous degraded view.
