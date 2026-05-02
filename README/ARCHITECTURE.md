# Architecture

This project enforces a strict 3-layer architecture.

## 1. Vendor Layer

`/vendor/orcaslicer`

* Contains upstream OrcaSlicer
* No modifications unless unavoidable

## 2. Engine Wrapper

`/engine-wrapper`

* C++ shared library
* Provides stable C API
* Handles all interaction with Orca

## 3. Android App

`/android-app`

* Kotlin + Jetpack Compose
* Uses JNI to call wrapper
* Loads `liborca_engine.so`
* Current Kotlin file ownership after the large non-networking split is
  documented in `/README/KOTLIN_REFACTOR_MAP.md`

## Rule

Android must NEVER directly depend on Orca internals.

All communication goes through the wrapper.

## Current Workspace / Preview Boundary

Current app-side ownership is intentionally split:

* `Prepare`
  * Android-owned STL workspace viewer
  * Android-owned bed / camera / interaction shell
  * Android-owned multi-object plate session
  * selected-object transform UI for move, rotate, scale, auto-orient, and
    auto-arrange entry points
* `Preview`
  * native Orca `libvgcode` owns G-code toolpath interpretation and rendering
  * Android owns the workspace shell, bed, layer controls, and draw submission

## Current Workspace Session Boundary

Current rotation/lifecycle ownership is also intentionally split:

* retained Android-side workspace session state survives configuration change
* recreated activity/view/render objects reconnect to that retained session
* native engine and render surfaces are still recreated normally

Current retained session scope includes the practical workspace session facts:

* selected staged STL path / current model identity
* plate object list, stable object IDs, and per-object transforms
* loaded-model/session labels
* prepared STL mesh state
* current `Prepare / Preview` mode
* Preview summary/filter session state

Explicit non-goal of this retained boundary:

* do not retain raw `SurfaceView` instances
* do not retain raw EGL / GL objects
* do not treat rotation as a reason to discard the staged STL

Current slice-after-rotation rule:

* the recreated native engine must reload the retained staged STL before
  slicing
* a visually restored workspace without a reloaded engine is not acceptable

## Multi-Object Plate Boundary

The current plate model deliberately separates object state from camera/viewer
state:

* each imported STL on the plate has its own stable object ID
* `PlateObject.transform` is the single source of truth for per-object
  placement
* the legacy single-model transform field mirrors the selected object only for
  compatibility with existing UI/status paths
* selecting an object changes selection state only; it must not rebuild mesh
  uploads or open transform controls by itself
* tapping the `Prepare` viewer returns an ordered list of object candidates,
  not only the front-most object; repeated taps in the same small screen area
  cycle overlapping candidates so stacked or nearby objects remain selectable
* an empty viewer tap clears only the selected object ID; it must not clear the
  loaded plate, staged model path, or legacy model-loaded state
* multi-object plates expose an `Objects` fallback selector in the compact
  bottom status panel instead of adding more persistent top-level controls
* selected-object feedback is rendered as a lightweight app-owned footprint
  outline in the STL viewer; it is UI state and must not affect slicing or
  exported transforms
* adding, deleting, or cloning plate objects is a plate-content update, not a
  new viewer scene; it must not show a fresh `Preparing workspace` state or
  refit/reset the user's current camera
* the STL viewer may auto-fit the camera when the first plate scene is created
  or when the active bed changes, but normal object membership edits preserve
  orbit, pan, and zoom
* moving, rotating, or scaling an object updates its model matrix in the viewer
  without reparsing STL or re-uploading every object mesh
* slicing a plate uses all plate objects and their transforms through
  `nativeLoadPlateModels(...)`
* the legacy single-model transform initializer must stay disabled whenever
  plate objects exist, otherwise it can reset edited object transforms during
  add/import flows
* repeated copies of the same STL may share prepared Android mesh state and are
  path-cached during one native `orca_load_plate_models(...)` call before being
  cloned into the combined plate model

## Rejected Viewer Boundary

The old Android-owned Preview geometry path is not the accepted long-term
architecture.

Specifically:

* Android should not remain the owner of Preview extrusion geometry generation
* Android-side Preview tessellation is temporary/scaffolding only
* the accepted long-term direction is:
  * native Orca-side Preview geometry generation
  * Android-side buffer upload and drawing only

The recent full native preview mesh export experiment also does not count as the
final answer yet because, in its current form, it was too memory-heavy for the
phone path.
