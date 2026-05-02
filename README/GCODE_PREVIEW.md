# G-code Preview Architecture

Date: 2026-04-25

This document describes the current phone G-code preview path. Keep this behavior
unless a measured regression proves otherwise. The current path was validated on
a Samsung `SM-S938U1` and made large-model preview smooth, responsive, and
visually accurate.

## What Must Stay

- Keep preview exact by default.
- Do not run Preview Info enrichment or preview-display-mode updates while a
  slice is in progress. The info sheet is an inspection surface for completed
  preview data and must not compete with active slicing.
- Do not silently degrade large previews with lite/balanced-lite striding.
- Keep `kMaxPreviewVertices`, `kMaxCachedPreviewVertices`, and Android's
  `GcodePreviewPerformanceMode.HARD_VERTEX_CEILING` aligned at `1000000`.
  Device testing showed the old 1.2M native cap could crash in the band above
  Android's 1M planner budget instead of forcing exact layer chunks.
- Keep the user-facing GCODE Preview Performance setting bounded by that hard
  ceiling. Low end uses 400k vertices, Mid range uses 750k vertices, and High
  end uses 1m vertices for exact preview loading and automatic layer chunk
  planning.
- If a selected layer range is too large, automatically offer exact preview
  chunks sized around the current exact vertex budget instead of falling back to
  approximate geometry.
- The first exact chunk should load automatically after an oversized full-range
  Preview load fails.
- Keep in-panel previous/next exact-range arrows for chunk navigation.
- Keep the normal two-handle range slider visible in chunk mode, but scope its
  slider domain to the active exact chunk only. The full slider track should
  represent the current chunk's layer span, not the full print.
- Keep the cache-hit path loading directly from `cached_preview_input` through
  `libvgcode::Viewer::load_layer_range(...)`.
- Do not restore the old wrapper-side `build_gcode_input_layer_range(...)`
  temporary object for cache hits.
- Do not change STL preview behavior while tuning G-code preview. STL preview is
  a separate path.

The old degraded behavior felt worse because it crossed the 400k vertex cap,
then retried by skipping geometry. That produced inaccurate previews and extra
native work. The current behavior lets the phone render exact data up to 1M
selected preview vertices.

For models above that exact range, the accepted mobile UX is chunked exact
preview. The app should guide the user through exact ranges without making them
manually guess layer bounds, while still preserving the fast direct range-load
path that keeps G-code performance in a good place.

## Oversized Exact-Preview Chunks

When the requested G-code layer range exceeds the exact preview vertex budget,
native code estimates per-layer preview weight from the cached parsed preview
data when available, otherwise from the emitted G-code text, and
returns sequential `PreviewRangeSuggestion` chunks. The target is about
the selected GCODE Preview Performance budget per chunk.

Accepted behavior:

- Before native Preview loading is enabled for a large new slice, Workspace asks
  the native wrapper for a count-only exact-range plan. If the full print is
  above budget, Workspace applies the first planned exact range before the
  render thread loads libvgcode. Normal-size G-code skips this preflight so
  Preview keeps the older fast direct-load path.
- Low end and Mid range intentionally preflight with lower budgets, so automatic
  layer chunks are smaller on slower phones. High end keeps the 1m hard-ceiling
  behavior.
- Full-range Preview may still fail with `G-code preview range too large` only
  when planning is unavailable or a single layer is itself above the exact cap.
- The bottom Preview panel shows `Exact range N/M - Lstart-end` with previous
  and next arrow buttons using the same button treatment as the existing app
  controls.
- Pressing an arrow applies that chunk's global layer range and increments the
  preview reload token.
- The range slider remains a range slider. In chunk mode, its `valueRange` is
  the active chunk's global layer bounds, so dragging the handles edits only the
  visible chunk.
- Do not use a floating overlay card for chunk navigation.
- Do not convert this into a single-layer slider; users still need to inspect
  sub-ranges inside the active exact chunk.
- Dragging the range slider updates the already-loaded libvgcode viewer range.
  It must not mark the workspace as unready, show `Preparing workspace`, or
  force a native Preview reload unless the source changed or an explicit reload
  token changed.

Important implementation detail: keep requested G-code layer bounds as global
print layers in the render-thread state. The native preview file internally
renumbers selected layers for libvgcode, but Kotlin must not overwrite the
active global range with that local `0..N` range after a successful load. If it
does, arrow navigation will load only a partial/local range until the user moves
the slider and re-applies the correct global bounds.

## High-Level Flow

After slicing, the native engine owns the emitted G-code text. Preview is loaded
on the GL render thread only when Preview mode or layer selection requires it.

1. Kotlin calls `NativeEngineBridge.nativeLoadLatestSliceIntoGcodeViewer(...)`.
2. JNI forwards to `orca_gcode_viewer_load_latest_slice(...)`.
3. Native code ensures a parsed preview cache exists with
   `ensure_gcode_preview_cache(...)`.
4. Cache hits call `libvgcode::Viewer::load_layer_range(...)` directly.
5. libvgcode builds its internal vertex data and GPU upload buffers.
6. Each frame calls `NativeEngineBridge.nativeRenderGcodeViewer(...)`, which
   forwards view/projection matrices to `orca_gcode_viewer_render(...)`.

## Kotlin Entry Points

- `android-app/app/src/main/java/com/mobileslicer/viewer/TouchModelViewerView.kt`
  - `setGcodePreviewSource(...)`
    - Sets the native engine handle and preview key.
    - Changing this bumps `gcodePreviewVersion`.
  - `setGcodeLayerRange(...)`
    - Sets the requested preview layer window.
    - Changing range bumps `gcodeLayerRangeVersion`.
    - Changing reload token can force a preview reload.
  - `WorkspaceRenderThread.syncSceneState()`
    - Detects preview source/range changes.
    - Creates a native G-code viewer handle with
      `NativeEngineBridge.nativeCreateGcodeViewer()`.
    - Loads preview data with
      `NativeEngineBridge.nativeLoadLatestSliceIntoGcodeViewer(...)`.
    - On oversized exact-range failure, preserves native range suggestions in
      `ViewerFailure.previewRangeSuggestions`.
    - On successful range load, preserves the global requested layer bounds
      instead of replacing them with libvgcode-local bounds.
  - `renderFrame(...)`
    - Calls `NativeEngineBridge.nativeRenderGcodeViewer(...)` only when an active
      G-code viewer exists.

This render thread is event-driven. It waits when nothing is dirty, which is a
large part of why interaction stays efficient on phone.

## JNI Bridge

- `android-app/app/src/main/cpp/jni_bridge.cpp`
  - `nativeLoadLatestSliceIntoGcodeViewer(...)`
    - Passes viewer handle, engine handle, min layer, max layer, and `lodHint`
      to native wrapper code.
  - `nativeRenderGcodeViewer(...)`
    - Validates the two 16-float matrices.
    - Passes them to `orca_gcode_viewer_render(...)`.
  - `nativeSuggestGcodePreviewRanges(...)`
    - Returns semicolon-separated exact range suggestions from the native
      wrapper for oversized previews.

`lodHint` is currently not used for automatic simplification. Preserve that
unless approximate preview becomes an explicit user choice.

## Native Wrapper

- `engine-wrapper/orca_wrapper.cpp`
  - `kMaxPreviewVertices = 1000000`
    - Exact selected preview vertex cap.
  - `kMaxCachedPreviewVertices = 1000000`
    - Full parsed preview cache cap.
  - `to_vgcode_input_data_from_gcode_text(...)`
    - Parses emitted G-code into libvgcode `GCodeInputData`.
    - Keeps exact extrusion vertices.
    - When emitted G-code contains layer markers, keeps extrusion before the
      first marker in preview layer 1 so purge/prime moves remain visible
      without creating an extra fake layer before the first model layer.
    - Marker-less G-code still falls back to Z-height layer detection.
    - Stops at `max_vertices` and reports `vertex_limit_reached`.
    - Does not skip roles or stride segments.
  - `ensure_gcode_preview_cache(...)`
    - Builds `engine->impl.cached_preview_input` once per generated G-code.
    - Reuses the cache when source size matches.
    - Stores per-layer vertex ranges so exact range loads can avoid scanning the
      full cached vertex list.
    - Logs vertex count, layer count, source bytes, completeness, vertex-limit
      state, and parse ms when the cache is built.
  - `orca_gcode_viewer_load_latest_slice(...)`
    - Initializes libvgcode viewer if needed.
    - Uses `ensure_gcode_preview_cache(...)`.
    - On cache hit, calls `viewer->viewer.load_layer_range(...)`.
    - On cache miss/incomplete cache, parses the requested range exactly with
      `to_vgcode_input_data_from_gcode_text(...)`.
    - If selected vertices reach the exact cap, returns an explicit error:
      use exact range suggestions or narrow the layer range to keep preview
      accurate.
    - Logs `fidelity=exact`.
  - `orca_gcode_preview_suggest_layer_ranges(...)`
    - Estimates preview vertices by G-code layer from the cached parsed preview
      input when it is complete, falling back to a count-only G-code text pass.
    - Emits sequential exact chunks first, then contextual ranges if available.
    - Keeps the suggestion target below the hard exact cap so normal chunk loads
      avoid the limit failure path.
  - `orca_gcode_viewer_render(...)`
    - Copies view/projection matrices into libvgcode matrices and renders.

The important performance win is that cache hits do not materialize a
wrapper-side filtered `GCodeInputData`. The previous flow was:

```text
cached full G-code preview
  -> temporary filtered GCodeInputData
  -> libvgcode internal m_vertices
  -> GPU upload buffers
```

The current flow is:

```text
cached full G-code preview
  -> libvgcode load_layer_range(...)
  -> GPU upload buffers
```

The selected vertices are still copied once into libvgcode because libvgcode owns
its render state. The removed copy was the extra wrapper-side temporary object.

## libvgcode Changes

- `vendor/orcaslicer/src/libvgcode/include/Viewer.hpp`
  - `size_t load_layer_range(const GCodeInputData&, long, long, size_t)`
- `vendor/orcaslicer/src/libvgcode/src/Viewer.cpp`
  - Forwards `Viewer::load_layer_range(...)` to `ViewerImpl`.
- `vendor/orcaslicer/src/libvgcode/src/ViewerImpl.hpp`
  - Declares `load_layer_range(...)`.
  - Declares `load_vertices(...)`.
- `vendor/orcaslicer/src/libvgcode/src/ViewerImpl.cpp`
  - `ViewerImpl::load(...)`
    - Still supports ownership-transfer loads from `GCodeInputData&&`.
    - Delegates common setup to `load_vertices(...)`.
  - `ViewerImpl::load_layer_range(...)`
    - Uses cached per-layer vertex ranges when present to count and copy only the
      requested layer span.
    - Falls back to the full scan path if indexed ranges are unavailable.
    - Counts selected vertices without mutating the current viewer.
    - If count is zero or reaches `max_vertices`, returns without loading.
    - Builds exact selected `PathVertex` values.
    - Renumbers layer IDs by subtracting `min_layer`, preserving previous
      selected-range behavior.
    - Calls `load_vertices(...)`.
  - `ViewerImpl::load_vertices(...)`
    - Contains the shared original libvgcode load setup:
      - `reset()`
      - move/copy vertices and palettes into viewer state
      - rebuild layers, roles, options, color caches, bitsets, and GPU buffers
      - update full view range, enabled entities, and colors

## Why It Is Fast

- The full G-code preview is parsed once and cached.
- Changing preview ranges uses already-parsed `PathVertex` data.
- Cache hits avoid a large temporary filtered `GCodeInputData`.
- The old repeated exact-then-lite parse attempts were removed.
- The viewer stays exact up to 1M selected vertices, avoiding the old
  simplification path and its extra work.
- The render thread sleeps when no state is dirty, so the app is not rendering a
  constant 60fps loop unnecessarily.
- Layer-range loading rejects too-large ranges before replacing the current
  viewer contents, which keeps failure behavior controlled.

## Measurement Logs

The native preview path now emits two focused log lines under the
`MobileSlicerNative` Android log tag:

- `gcode_preview_cache`
  - emitted when the full parsed preview cache is rebuilt
  - fields: `vertices`, `layers`, `sourceBytes`, `complete`,
    `vertexLimitReached`, and `parseMs`
- `gcode_viewer_load_latest_slice`
  - emitted for every Preview source/range load
  - fields: selected `vertices`, requested and loaded layer bounds,
    `budget`, `cache` mode, `cacheBuilt`, `cacheValid`, `cacheComplete`,
    `cachedVertices`, `cachedLayers`, `selectedParseMs`,
    `libvgcodeLoadMs`, `totalMs`, and `fidelity=exact`

Interpretation:

- `cache=complete-hit` means the selected range loaded from the full parsed
  preview cache through `libvgcode::Viewer::load_layer_range(...)`.
- `cache=range` means the selected range loaded from the existing parsed
  preview cache through `libvgcode::Viewer::load_layer_range(...)`.
- `cache=fallback` means a cache exists, but the selected range had to fall
  back to selected-range parsing.
- `cache=miss` means the full cache was missing or invalid, so the selected
  layer range was parsed exactly from emitted G-code text.
- `cache=selected-parse` means the full cache was missing, invalid, or
  incomplete, so the selected layer range was parsed exactly from the emitted
  G-code text.
- `selectedParseMs` should normally be `0` on complete cache hits.
- `libvgcodeLoadMs` is the time spent handing the selected exact vertices to
  `libvgcode` and rebuilding its render state/GPU buffers.
- `totalMs` is the whole native load path, including cache creation when the
  cache had to be built during that Preview load.

Use these logs before changing Preview behavior. If Preview feels slow, first
classify the bottleneck as cache parse, selected-range parse, libvgcode
load/upload, or render-time interaction. Do not optimize by silently changing
toolpath fidelity.

## Accuracy Rules

The preview should represent emitted G-code toolpaths, not an STL mesh and not a
downsampled approximation. Preserve these rules:

- Exact extrusion vertices only.
- Preserve source vertex order.
- Preserve feature roles from `;TYPE:` / `;FEATURE:` markers.
- Preserve layer range renumbering for selected ranges.
- No hidden role skipping.
- No hidden stride sampling.
- Any future approximate mode must be explicit in UI and logs.

## Regression Checks

Run these after touching the preview path:

```bash
cd android-app
./gradlew testDebugUnitTest
./gradlew :app:externalNativeBuildPerfDebug
./gradlew :app:installPerfDebug -Pandroid.injected.device.serial=RFCYA01ANVE
```

Manual checks:

- Slice a small model, enter Preview, verify preview appears.
- Slice a large model over the old 400k preview vertex threshold.
- Verify the preview is exact, not visibly missing infill/support/surface paths.
- Scrub single-layer and range modes.
- Large exact previews must auto-plan chunk ranges before attempting an
  over-budget full-range load. Scrubbing inside a chunk should update the
  existing viewer state; it must not rebuild the native preview on every drag
  frame.
- For a model whose full preview exceeds the exact cap, verify the first exact
  range auto-loads and the previous/next arrows load whole chunks immediately.
- In exact chunk mode, verify the range slider still has two handles and only
  spans the active chunk's layer range.
- Confirm logs include `gcode_viewer_load_latest_slice ... fidelity=exact`.
- Confirm oversized ranges show an exact-preview limit error instead of degraded
  geometry when suggestions are unavailable.

## Revert Reference

See `README/GCODE_PREVIEW_DIRECT_RANGE_LOAD_EXPERIMENT.md` for the detailed
revert notes from the experiment. Reverting should be a deliberate decision,
because the current behavior was manually validated as much smoother and more
accurate on large models.

## Current Mobile Preview Rules

* The exact preview vertex ceiling is `1,000,000`; do not lower it to hide a
  crash or simplify range planning.
* Slice completion may enrich the summary from Orca's existing
  `GCodeProcessorResult`, including filament model/support/flushed/tower totals
  and tool-change counts. It must not add a second full G-code parse or build an
  oversized full-preview cache.
* The Settings > Advanced `GCODE Preview Performance` option controls the exact
  preview range budget: `Low end` = 400k vertices, `Mid range` = 750k vertices,
  and `High end` = 1m vertices. This changes chunk size only; preview geometry
  remains exact.
* `G-code stats` can lazily fill remaining preview-derived fields when the
  `Info` sheet is opened, while keeping the selected exact-preview budget and
  range planner intact.
* Viewer range changes should preserve camera state and should not show the
  workspace preparation overlay for normal range scrubbing.
* Lifecycle pause tears down the viewer renderer and native G-code viewer so a
  cached/backgrounded app does not retain large GL/native preview allocations.
* The preview top bar must stay action-only. Printer runtime state such as
  `Printing 0%` or `Standby 0%` belongs in the Preview bottom panel, where it
  remains visible without crowding `Send`, `UI`, `Info`, and `Prepare`.
