# G-code Viewer

This document defines the target architecture, performance model, and visual
quality bar for the Mobile Slicer G-code viewer.

It is intentionally precise. The goal is not to "have some preview." The goal
is a fast, stable, phone-appropriate toolpath viewer that feels deliberate and
trustworthy, with the strongest useful parts of the OrcaSlicer viewing
experience translated to Android.

This document is a design and implementation contract and the current source
of truth for the working Android G-code preview path.

For phone-side exact range loading, cache-hit behavior, selected-vertex caps,
and rejected Lite/striding behavior, `README/GCODE_PREVIEW.md` is the newest
and most specific contract. Keep this document aligned with that file when
editing Preview behavior.

## Current Reality

Current live-tree truth:

* `Preview` exists in the product shell
* `Preview` now uses Orca `libvgcode` through the native wrapper for the live
  G-code toolpath render path
* current debug APK builds the shipping native viewer/slicer code with C++
  `-O3` and `-fomit-frame-pointer`
* generated ARM debug compile commands now show those flags on `libvgcode`
  sources including `GCodeInputData.cpp`, `Layers.cpp`, and `ViewerImpl.cpp`
* the old phone-side `59s` Benchy slice number was an unoptimized-debug build
  baseline; user-confirmed current result after the native build correction is
  about `4s`
* user also confirmed the STL render path became much faster after the same
  native build correction
* current app-visible timing now separates native slice from post-slice work:
  * `Summary 4520 ms` was reduced to `682 ms` on user-provided Benchy
    screenshots
  * estimated print time remains visible through a manual fallback parser
    instead of the previous regex movement parser
* Android no longer owns G-code preview geometry reconstruction
* the old Kotlin/Android Preview geometry path is not the accepted boundary and
  must not be revived
* the attempted native full-mesh export path also does not qualify as the final
  answer because it was too memory-heavy on phone
* current workspace/session lifecycle truth now also includes:
  * `Prepare / Preview` session state survives configuration-change recreation
  * the staged STL is retained across rotation
  * native engine/view objects are still recreated normally
  * slice-after-rotation must work by reloading the retained staged STL into
    the recreated native engine before slice execution

The working implementation follows the same practical boundary SliceBeam uses:
feed emitted G-code into Orca's native viewer stack and let native preview code
own toolpath interpretation and render data.

## Working Android `libvgcode` Path - 2026-04-24

This is the exact working path that fixed the on-device Preview renderer.

### Native Build Integration

`android-app/app/src/main/cpp/CMakeLists.txt` links Orca `libvgcode` into the
Android native library:

* `SLIC3R_OPENGL_ES` is enabled for the Android build.
* `vendor/orcaslicer/src/libvgcode` is added with `add_subdirectory(...)`.
* `libvgcode` receives `GLAD_GLES2_USE_SYSTEM_EGL`.
* the Android native bridge links `libvgcode`, `GLESv3`, `EGL`, and `log`.
* `MOBILE_SLICER_ENABLE_VGCODE` gates the real viewer path.

`vendor/orcaslicer/src/libvgcode/src/ViewerImpl.cpp` has one Android GLES fix:
under `ENABLE_OPENGL_ES`, the local `Vec4` storage used by the affected texture
path is treated as `Vec3` with 3-float initializers. The GLES path is RGB-like;
keeping the desktop RGBA/buffer-texture shape caused Android build/runtime
friction.

### Native Wrapper Surface

`engine-wrapper/orca_wrapper.h` exposes an opaque `OrcaGcodeViewer` and this C
API:

* `orca_gcode_viewer_create`
* `orca_gcode_viewer_destroy`
* `orca_gcode_viewer_init`
* `orca_gcode_viewer_shutdown`
* `orca_gcode_viewer_load_latest_slice`
* `orca_gcode_viewer_load_gcode`
* `orca_gcode_viewer_render`
* `orca_gcode_viewer_get_layers_count`
* `orca_gcode_viewer_set_layers_view_range`
* `orca_gcode_viewer_get_last_error`

`android-app/app/src/main/cpp/jni_bridge.cpp` and
`android-app/app/src/main/java/com/mobileslicer/nativebridge/NativeEngineBridge.kt`
mirror those calls for the Kotlin viewer.

### Live Android Flow

`MainActivity.kt` keeps the emitted slice G-code as a native-owned latest-slice
artifact and a retained app-cache file path for export/share. Kotlin no longer
retains the full emitted G-code text for the normal interactive path.

`TouchModelViewerView.kt` owns the native G-code viewer for Preview mode:

* `setGcodePreviewSource(engineHandle, previewKey)` stages the native engine
  handle and current slice key.
* the GL thread calls `nativeCreateGcodeViewer`,
  `nativeLoadLatestSliceIntoGcodeViewer`, and `nativeRenderGcodeViewer`.
* the app-owned bed/grid still renders first, then native `libvgcode` renders
  the toolpaths with the same view/projection matrices.
* `setGcodeLayerRange(...)` stores the selected visible range and applies it
  through native latest-slice Preview loading/range updates.
* shutdown and destroy happen on the GL/render thread because the viewer owns GL
  resources.

The EGL path uses a GLES 3 context (`EGL_RENDERABLE_TYPE = 0x40`, client version
3), while the existing app viewer shell keeps touch orbit, pan, zoom, and the
shared bed.

### Camera And Gestures

The workspace camera is app-owned in `TouchModelViewerView` / `ViewerCamera`
and is shared by STL Prepare and G-code Preview.

Accepted camera behavior:

* pinch zoom scales the whole camera distance; there is no fixed close-range
  distance term that flattens zoom response
* small pinch scale changes are applied instead of being dropped by a zoom
  deadband
* the zoom range is intentionally wide enough for both zoomed-out plate context
  and close toolpath inspection; current camera bounds are `0.28x..24x`
* the default and reset camera uses a front-on bed view: yaw `-90`, pitch `42`,
  zoom `0.55x`
* one-finger drag orbits the camera
* two-finger gestures use a dead zone and then lock to either centroid pan or
  pinch zoom for the rest of that gesture; pan and zoom are not applied
  together after the lock
* two-finger double-tap resets camera orbit, pan, and zoom without changing
  model transforms or plate-object state
* camera zoom never changes G-code Preview LOD, reloads native preview buffers,
  or mutates extrusion dimensions

This avoids the previously observed pan/zoom clash where normal finger drift
during pinch also moved the camera target.

### Preview Layer Controls

The Preview dock exposes layer controls only in Preview:

* `Range`: two-sided range slider; sends the selected inclusive layer span.
* `Single`: one slider; sends one selected layer.

`Range` is the default mode and sits on the left. `Single` sits on the right.
The removed `All` behavior is represented by the default full-span `Range`
selection.

`Range` scrubbing is a live preview contract:

* every drag tick updates the visible layer window immediately
* dragging top-to-bottom and bottom-to-top must both update smoothly
* the app must not wait until finger release to update the model
* finger release records the final range only; it must not force a native
  G-code preview reload
* native reloads are reserved for a new preview source or an explicit reload
  token change

Kotlin UI labels are 1-based for users. Native viewer calls are 0-based, so the
UI subtracts one before calling `setGcodeLayerRange(...)`.

The same Preview control area shows a compact metrics row:

* selected layer span
* estimated time
* filament grams

### Preview Info Sheet

Preview also exposes an `Info` action beside `Prepare` when the latest slice
has G-code available. The sheet is labeled `G-code stats`. This is the Android
translation of Orca's G-code viewer legend and estimate panel, not a separate
Android estimate model.

Source of truth:

* slice completion stores the bottom-panel summary and enriches filament/time
  totals directly from Orca's in-memory `GCodeProcessorResult`; this avoids a
  second G-code parse while preserving model/support/flushed/prime-tower
  attribution
* opening `G-code stats` asks native code for any still-missing preview fields,
  but it must not force an unbounded full-preview parse
* native derives line-type rows from the same libvgcode role/option ids used by
  the viewer visibility toggles
* Kotlin only parses and displays those fields; it does not recalculate
  line-type visibility or usage from UI state

The sheet is mobile-first and grouped into tabs:

* `Line Type`: Orca/libvgcode role and option rows, time, percent, meters,
  grams, and visibility control.
* `Filament`: per-filament model/support/flushed/prime-tower/total usage and
  cost.
* `Time`: total/model/prepare timing, layer count, filament changes, extruder
  changes, and largest time contributors.
* `Cost`: total cost plus per-filament cost and purge/prime-tower context.

Line-type visibility is real preview state. Tapping `Hide` / `Show` calls
through `TouchModelViewerView` -> `WorkspaceRenderThread` ->
`NativeGcodeViewerCalls.setPathVisibility(...)` -> libvgcode
`toggle_extrusion_role_visibility(...)` or `toggle_option_visibility(...)`.
The app stores the target visibility by role/option id and reapplies it after a
preview reload or `SurfaceView` recreation. Default visibility mirrors
libvgcode: extrusion roles are visible, seams are visible, and
travel/wipe/retract/tool-change style option rows start hidden. This matches
Orca's viewer semantics while keeping Android UI state outside the native
slicer model.

Seams require real `libvgcode::EMoveType::Seam` vertices, not only a visible
row in the stats sheet. Exact-cacheable previews are fed from Orca's processed
move stream (`Slic3r::GCodeProcessorResult::moves`) so seam, travel, wipe,
retract, unretract, and tool-change rows have matching libvgcode move ids.
Oversized previews intentionally fall back to the lightweight G-code text parser
and exact layer chunking, because building a full processed-move cache for those
jobs would put too much work and memory pressure on the slice/preview boundary.
Per-filament `Tower` also falls back to Orca `Print::wipe_tower_data()` when the
processor map is empty, and `Filament changes` / `Extruder changes` fall back to
`Print::print_statistics().total_toolchanges`.

The native viewer surface is recreated on portrait/landscape size changes so
the OpenGL viewport is not stretched. Before disposal, Android snapshots the
current camera orbit, pan, and zoom; after the new scene and G-code buffers are
uploaded, the snapshot is restored and any explicit line-type visibility
overrides are replayed.

The same teardown path runs on Android lifecycle pause. Backgrounding the app
releases the render thread, EGL context, GL uploads, and native G-code viewer
handles instead of keeping the cached preview resident while the app is not
visible. Returning to the app recreates the renderer from the retained source
key, layer range, visibility state, and camera snapshot.

Settings > Advanced exposes `GCODE Preview Performance` for phone-class
tuning. `Low end` plans and loads exact chunks around 400k vertices, `Mid range`
uses 750k and is the default, and `High end` uses the 1m vertex ceiling.
The setting affects G-code Preview only and never switches to approximate or
decimated geometry.

The app scans the whole emitted G-code for Orca-style metrics instead of
stopping at the first partial footer line. Filament display precedence is:

* `; total filament used [g] = ...`
* per-tool `; filament used [g] = ...`
* `; filament used [cm3] = ...` converted with filament density
* `; filament used [mm] = ...` converted with filament diameter and density
* positive extrusion distance from `G0` / `G1` `E` or `A` words

### Preview Fidelity

Current behavior:

* exported G-code is never changed
* slicing output is never changed
* layer filtering still happens before `libvgcode` upload
* the hard selected-preview phone cap is `1M` vertices
* selected toolpath ranges are parsed exactly until the hard vertex cap is hit
* if an exact selected range exceeds the hard vertex cap, Preview reports an
  explicit limit error and asks the user to narrow the layer range
* Preview must not silently degrade large previews with Lite/balanced-Lite
  striding in the normal user path
* there is no emergency shell mode, automatic layer-window substitution, or
  extrusion-width scaling in the normal Preview path
* there is no retry that replaces a failed selected range with a single layer
* native preview load logs include `fidelity=exact`

Preview interaction optimizations:

* The native wrapper lazily builds a per-slice parsed Preview cache on first
  Preview load, then cache hits load selected ranges directly through
  `libvgcode::Viewer::load_layer_range(...)`.
* If the parsed cache would exceed the conservative phone cache budget, Preview
  falls back to exact selected-range parsing instead of changing rendered
  toolpath fidelity.
* Single/Range sliders update the visible native layer range while dragging
  without recreating the G-code viewer.
* Filtered Preview loads track the global layer offset of the loaded window so
  live layer-range updates map correctly into `libvgcode`'s local layer IDs.
* The render thread owns one active native G-code viewer handle for the current
  loaded range.
* Live native reloads while dragging were tested on device and backed out
  because they made Preview feel less smooth.
* The small two-entry native viewer cache was tested on device and backed out
  because it also made Preview feel less smooth.
* A display-only Z-rebase experiment for large filtered Preview was tested and
  rejected because it made device rendering worse.
* The later 96-layer large-window clamp, large-drag throttled reload path, and
  loaded-bounds camera-framing experiment were also removed after device review.
  Large Preview should be rebuilt from the normal Preview behavior.

This is intentionally an exact viewer path. It does not trade toolpath truth for
large-model speed in the normal user experience.

Near-zero gram values are ignored so a rounded `0.00` footer cannot mask a
valid mm/cm3/extrusion fallback. Time uses emitted estimate comments first, then
falls back to feedrate/distance accumulation when needed.

Filament density is part of the app filament profile and is forwarded into the
native Orca config as `filament_density`. That keeps Orca's emitted
`total filament used [g]` footer nonzero and lets the Preview summary use the
same material density as the slicer. If an older or malformed emitted file still
reports non-positive density, the summary treats it as missing and falls back to
the app's generic material-density table.

Verified on device after this fix: the Android Preview metric row shows
nonzero filament grams from regenerated G-code, including the `25 mm` cube
case that previously showed `0.00 g` / `--`.

The Preview summary is now sourced from the emitted G-code that the app already
keeps for the active slice. The old duplicate `last-preview-debug.gcode` cache
copy has been removed from the normal slice path.

### Zoomed-Out Rendering Artifact

At wide zoom, dense Preview toolpaths can look noisy or striped even when the
same G-code looks correct up close. The current renderer draws many real
world-width extrusion prisms through `libvgcode`. When hundreds of close,
sub-pixel paths project into the same screen pixels, the result is mostly
aliasing, depth conflict, and overdraw. It is a rendering level-of-detail issue,
not a slicer or parser geometry issue.

Rejected fixes:

* Solid STL proxy overview mode was rejected on device because Preview should
  stay a G-code/layer preview, not a solid-model substitute.
* Top-layer-only wide-zoom LOD was rejected because tall models visually
  disappeared when zoomed out.
* 4x MSAA EGL preference was backed out because the interaction path felt less
  smooth on the test device.
* Runtime zoom-aware extrusion width/height scaling was backed out because it
  changed native viewer buffers during camera interaction and made pinch/zoom
  feel worse.
* The April 25 shell/window experiment was backed out because it degraded both
  small and large G-code previews. The default whole-print view became visually
  noisy, the range slider behavior felt broken, and small Benchy previews were
  affected even though the intended target was large G-code.
* Span-based extrusion-width scaling was also backed out. It preserved G-code
  coordinates but changed the rendered extrusion dimensions enough to make the
  preview look less trustworthy on device.
* Emergency shell/window large-model Preview LOD was deleted from the normal
  path because it made large G-code previews visually untrustworthy.

Current active behavior:

* Preview keeps full selected-range native G-code rendering at every zoom level.
* Preview renders exact selected G-code ranges up to the `1M` selected-vertex
  phone cap, then fails clearly instead of silently degrading geometry.
* `TouchModelViewerView` uses the original non-MSAA GLES 3 window config.
* The camera keeps the smooth non-scaling render path but allows a wider
  inspection range: `0.45x` to `12x`.
* The remaining likely improvement path is a native viewer/shader-side
  anti-aliasing or screen-space treatment that does not rebuild native
  extrusion buffers while the user is interacting.

Guardrails for future work:

* Do not alter emitted G-code for preview performance.
* Do not replace selected ranges with a shell, sampled model, or synthetic
  overview in the normal user path.
* Do not auto-clamp the existing `Range` slider to a fixed window unless the UI
  explicitly exposes that as a separate mode.
* Do not change extrusion width/height globally to hide overdraw.
* Any future large-preview optimization must be built behind an explicit
  opt-in/debug path and device-validated on at least one small Benchy slice and
  one larger/high-layer-count G-code before becoming the default.

### G-code Text Fallback Parser

The native wrapper includes
`to_vgcode_input_data_from_gcode_text(...)` in
`engine-wrapper/orca_wrapper.cpp`. This is intentionally narrow and exists to
feed Android-emitted G-code into `libvgcode` without rebuilding the old Android
geometry path.

It reads:

* `G0` / `G1` motion
* `G90` / `G91`
* `M82` / `M83`
* `G92`
* `;TYPE:...`
* `; FEATURE: ...`
* `HEIGHT`, `WIDTH`, and layer-change comments

It emits only `EMoveType::Extrude` vertices into the renderable input data.
Travel, retract, and unretract moves are intentionally skipped so Preview does
not draw non-printing paths by default.

`kMaxPreviewVertices` is currently `1000000`, aligned with
`kMaxCachedPreviewVertices` and Android's
`GcodePreviewPerformanceMode.HARD_VERTEX_CEILING`. Keep those caps aligned so
ranges above the safe exact-preview limit are chunked before libvgcode/GL upload
instead of landing in a crash-prone middle band.

### Required Parser Invariants

Do not regress these details. They were the difference between crash/fake paths
and a working Preview.

`libvgcode::Layers::update(...)` expects compact layer IDs. Layer IDs must start
at `0` and increment by exactly one only when the first extrusion vertex for a
new layer is emitted. Do not increment immediately on every `;LAYER_CHANGE`.
Instead, set a pending layer-change flag, then increment only when the next
extrusion vertex is appended and the vertex list is nonempty.

Path breaks require a separator vertex. `libvgcode` considers a segment drawable
when `vertices[i + 1].position != vertices[i].position`,
`vertices[i + 1].type == vertices[i].type`, and the type is not `Seam`. If a
new extrusion island begins right after another island with the same move type,
the renderer draws a diagonal connector. The fix is:

* when `append_path_vertex(...)` starts a new path and vertices already exist,
  push a zero-length separator copied from the previous vertex
* set the separator `type` to `EMoveType::Noop`
* set the separator `role` to `EGCodeExtrusionRole::None`
* set the separator `times` to `{0.0f, 0.0f}`
* then push the actual start vertex and end vertex

That makes old-end to separator invalid by move-type difference, separator to
new-start invalid by move-type difference, and new-start to new-end valid.

Top-surface color normalization is also intentional. Some Orca/Bambu output
labels top faces with a mix of solid/bridge/internal roles on the same visual
surface, which made the top look like red/blue/purple stripes. The wrapper runs
`normalize_top_surface_layers(data)`:

* detect layers that contain `TopSolidInfill`
* on those same layers, fold `SolidInfill`, `InternalInfill`, `BridgeInfill`,
  `InternalBridgeInfill`, and `BottomSurface` vertices to `TopSolidInfill`
* keep other layer-type colors intact

The Android Preview uses `libvgcode` `EViewType::FeatureType` so Orca-style
role colors remain visible. Do not switch this to one-color `Summary` mode just
to hide bad lines. Fake diagonal lines are handled at the parser boundary by
forcing path breaks after skipped travel/retract/non-extrusion moves.

### Role Mapping

`role_from_type_marker(...)` maps common Orca, Bambu, and Prusa-style markers
onto `libvgcode` extrusion roles:

* `Skirt` / `Brim`
* `Inner wall` -> `Perimeter`
* `Outer wall` -> `ExternalPerimeter`
* `Overhang wall` -> `OverhangPerimeter`
* `Sparse infill` / `Internal infill` -> `InternalInfill`
* `Internal solid infill` / `Solid infill` -> `SolidInfill`
* `Top surface` / `Top solid infill` -> `TopSolidInfill`
* `Bottom surface` -> `BottomSurface`
* `Bridge` / `Internal bridge` -> `InternalBridgeInfill`
* support, ironing, and custom markers where available

### Verification

The working path was built with:

```bash
./gradlew :app:assembleDebug
```

When the debug APK package became corrupt during iteration, the fix was:

```bash
rm -f app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:packageDebug
unzip -t app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Final install on `RFCYA01ANVE` succeeded, and on-device Preview was visually
confirmed with sliced cube and Benchy G-code.

## Goal

Maintain and extend a real Android G-code viewer that:

* stays responsive on phones
* scales to realistically large G-code jobs
* separates parsing, model construction, and rendering cleanly
* supports Orca-style inspection workflows
* does not fake toolpath truth using STL mesh rendering
* does not entangle preview logic with slicer-core logic

## Non-Goals

This viewer should not regress into:

* a Compose `Canvas` prototype
* a bed/STL renderer with lines layered on top as an afterthought
* a one-file parser that mutates UI state directly
* a desktop clone in Android clothing
* a requirement to match Orca's implementation details one-to-one

This viewer also does not require:

* live preview during slicing in v1
* editable toolpath geometry in v1
* perfect desktop parity in v1
* support for every printer metadata block before toolpath viewing is useful

## Product Bar

The viewer should feel:

* immediate
* legible
* stable under large files
* visually intentional
* useful for real print inspection, not just screenshots

If a user can orbit and admire it but cannot quickly answer:

* where are travels?
* what layer is this?
* what role is this path?
* where are bridges?
* what changed when I altered a setting?

then the viewer is not good enough.

## Core Principles

### 1. Parse Once, Render Many

Parsing must be separate from rendering. G-code should be parsed into an
internal normalized toolpath model once, cached, and then rendered repeatedly
without reparsing.

### 2. GPU Viewer, Not UI Drawing

The viewer must use a real GPU rendering path with batched geometry. Do not
build the core around Compose drawing primitives or Android `Path` rendering.

### 3. Layered Architecture

The viewer should be split into:

* parser
* normalized toolpath model
* renderer
* viewer UI shell

Each layer should be testable and replaceable without rewriting the others.

### 3a. Ownership Boundary

The accepted long-term ownership boundary is:

* Orca/native side owns Preview geometry generation
* Android owns:
  * `Prepare / Preview` shell
  * layer/filter UI
  * GPU upload
  * drawing

Android-owned Preview geometry generation is the wrong boundary and should not
be treated as the permanent architecture.

### 4. Toolpath Truth Over Visual Approximation

The viewer must render emitted G-code toolpaths, not inferred geometry from STL
or pseudo-preview heuristics.

### 5. Phone Constraints Are Primary

The viewer is for a phone-first product. Memory, thermal cost, upload size,
interaction latency, and clarity on small screens matter more than copying a
desktop preview literally.

## Required Architecture

### Layer 1: G-code Parser

The parser is responsible for:

* reading emitted G-code
* tokenizing movement and metadata lines
* tracking machine state through the file
* reconstructing toolpath segments from sequential motion commands
* assigning segments to layers and roles where possible
* recording feedrate, extrusion, and travel state

The parser should extract at minimum:

* move type:
  * extrusion move
  * travel move
  * retraction / deretraction signal when inferable
* position:
  * X
  * Y
  * Z
* feedrate
* extrusion delta
* segment length
* layer index
* role / `;TYPE:` classification
* file bounds
* toolpath bounds
* estimated print extents

The parser should also capture useful metadata where available:

* layer count
* elapsed print time comments
* thumbnail block presence
* object / preset comments
* material / printer comments

### Layer 2: Normalized Toolpath Model

The parser should not feed raw strings directly into the renderer. It should
build a normalized model with stable semantics.

Suggested model shape:

* file metadata
* ordered layer table
* ordered segment buffers
* role table
* chunk table
* bounds table

Suggested normalized entities:

* `ToolpathFile`
* `ToolpathLayer`
* `ToolpathChunk`
* `ToolpathSegment`
* `ToolpathRole`

The normalized model should support:

* fast layer-range queries
* fast role filtering
* fast visibility toggles for travel versus extrusion
* compact serialization / caching
* future diff/compare mode without reparsing logic

### Layer 3: Renderer

The renderer is responsible for:

* GPU buffer construction
* chunk upload
* camera transforms
* role coloring
* line thickness policy
* visible-range drawing
* layer fading / highlighting

The renderer should accept a prepared toolpath model, not parse text itself.

Recommended rendering strategy:

* pre-bucket by:
  * layer
  * role
  * segment class
* build batched vertex buffers per chunk
* render only currently visible chunks
* avoid per-segment draw calls
* avoid per-frame geometry rebuilds
* reuse uploaded buffers until visibility state changes materially

### Layer 4: Viewer UI Shell

The Android UI should control:

* loading state
* empty/error state
* layer scrubber
* role toggles
* travel visibility toggle
* color scheme selection
* camera actions
* compare mode entry points later

The UI shell should not own parsing or geometry generation.

## Performance Requirements

The viewer must be designed for stable phone performance.

### Hard Rules

Do:

* parse off the UI thread
* build immutable or near-immutable viewer state
* batch segments into GPU-friendly buffers
* render only visible layer windows
* use compact numeric storage
* cache parsed / normalized results
* keep per-frame allocations near zero

Do not:

* rebuild all geometry on every camera movement
* keep one heavyweight Kotlin object per G-code line in the hot path
* issue one draw call per segment
* parse on every layer scrub
* colorize by rebuilding geometry every frame when attributes or uniforms can do it

### Memory Strategy

Phone performance will usually fail on memory discipline before it fails on raw
GPU capability.

The model should therefore support:

* chunked storage by layer windows
* compact role ids instead of repeated strings
* packed float/int vertex formats
* lazy loading for giant files
* optional discard of unused raw text after normalization

### Large-File Strategy

For large jobs, the viewer should use:

* visible layer windows
* background chunk preparation
* optional low-detail mode
* progressive refinement rather than all-at-once uploads

The user should still be able to:

* open the file
* scrub to a layer
* orbit and inspect

without the app freezing.

## Visual Quality Bar

The viewer should borrow the strongest inspection ideas from OrcaSlicer, while
still feeling native on Android.

### Must-Have Visual Features

* role-based color coding
* clear differentiation between extrusion and travel
* highlighted active layer or layer window
* ghosted neighboring layers
* smooth orbit / pan / zoom camera
* bed-aware framing
* clean dark-on-light or dark-on-dark palette options that keep line contrast high

### Orca-Like Inspection Features

The future viewer should support:

* layer slider / scrubber
* current-layer only mode
* layer range window mode
* travel show/hide
* role filters:
  * outer wall
  * inner wall
  * top surface
  * bottom surface
  * infill
  * support
  * brim
  * skirt
  * bridge
  * travel
* feedrate-aware highlighting later
* optional color-by-role versus color-by-speed schemes later

### Camera Behavior

Camera interactions should feel deliberate:

* one-finger orbit
* two-finger pan
* pinch zoom
* reset camera
* fit-to-bed
* fit-to-layer selection later

Do not ship a viewer with awkward or unstable camera controls. A mediocre
camera makes the whole viewer feel broken.

### Line Rendering Quality

Toolpaths should look crisp, not muddy.

Requirements:

* consistent line visibility across zoom levels
* anti-aliased or otherwise visually stable line output
* role colors with enough contrast to read quickly
* travel lines visibly distinct but not overpowering
* active selection emphasis without washing out the scene

If necessary, prefer a simple but clean line style over a more ambitious style
with unstable performance.

## Feature Staging

### Stage 1: Foundation

Deliver:

* parser
* normalized toolpath model
* basic GPU renderer
* camera
* visible layer range rendering
* role coloring
* travel toggle

This is the first acceptable viewer milestone.

### Stage 2: Inspection UX

Deliver:

* layer scrubber
* role filters
* active layer emphasis
* ghosted neighboring layers
* fit/reset actions
* better error/loading states

### Stage 3: Advanced Analysis

Deliver:

* speed coloring
* flow/extrusion coloring
* bridge highlighting
* support highlighting
* compare mode for two G-code outputs
* segment metadata inspection

### Stage 4: Workflow Integration

Deliver:

* exported-job thumbnail support
* slice result summary integration
* printer-send pipeline hooks later
* saved viewer state for reopened files later

## Comparison Mode

A future top-tier feature is direct toolpath comparison.

This should allow:

* baseline versus variant G-code
* same camera / same layer window
* highlight changed roles or changed feedrate regions
* support the existing proof workflow used elsewhere in the repo

This is not required in v1, but the normalized model should not block it.

## Integration Boundaries

The viewer must respect repository architecture.

### Android App Layer

Responsible for:

* viewer screen
* user interactions
* state persistence
* loading / error UX

### Engine Wrapper

Should not become the G-code viewer renderer.

It may provide:

* file access helpers
* exported file references
* metadata handoff if useful

But the viewer must not require the wrapper to become a rendering subsystem.

### Vendor Orca Layer

Do not make viewer quality depend on patching Orca desktop preview internals
into the app unless there is a later very strong reason.

This project should prefer:

* emitted G-code parsing
* app-owned viewer model
* app-owned renderer

over brittle coupling to desktop preview code.

## Data Contracts

The viewer should define explicit contracts for:

* parsed file metadata
* layer indexing
* role classification
* segment geometry
* chunk boundaries
* cache versioning

If the parser or renderer changes shape, cache invalidation must be explicit.

## Error Handling

The viewer must fail clearly, not silently.

Examples:

* unsupported G-code flavor
* malformed file
* missing layer comments requiring fallback reconstruction
* out-of-memory pressure
* incomplete parser classification

Good failure behavior:

* preserve the file where possible
* surface the limitation clearly
* keep the rest of the app stable

## Testing Requirements

The viewer should be verified at three levels.

### Parser Tests

Verify:

* line parsing
* layer reconstruction
* role classification
* travel versus extrusion detection
* feedrate extraction
* edge-case comment handling

### Model Tests

Verify:

* bounds
* layer counts
* chunk generation
* cache serialization
* role filtering correctness

### Device Runtime Tests

Verify:

* open performance
* layer scrub latency
* orbit / pan / zoom stability
* large-file behavior
* memory pressure on realistic phones

The viewer should not be called "done" from emulator-only confidence.

## UX Requirements

The screen should make inspection obvious.

Minimum controls:

* layer slider
* current layer label
* total layer count
* role filter sheet / panel
* travel toggle
* reset view

Nice-to-have later:

* color scheme picker
* statistics panel
* compare toggle
* line thickness mode

## What "Top Notch" Means Here

For this project, "top notch" means:

* it opens quickly
* it stays smooth on a phone
* it tells the truth about emitted toolpaths
* it makes role/layer inspection easy
* it looks intentional and clean
* it does not collapse under larger files

It does not mean:

* maximum shader complexity
* a desktop UI transplanted onto Android
* every advanced feature on day one

## Delivery Rule

Do not start implementation by chasing polish first.

Build in this order:

1. parser correctness
2. normalized toolpath model
3. stable GPU rendering
4. inspection controls
5. visual polish

If those are done out of order, the viewer will look impressive early and feel
bad later.

## Final Standard

The viewer is acceptable when:

* it renders real emitted G-code, not inferred geometry
* it remains responsive on a realistic phone
* its architecture is still clean
* layer and role inspection are genuinely useful
* the result feels worthy of a slicer, not a demo
