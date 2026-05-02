# Workspace Viewer Improvement Plan

Date: 2026-05-01

This tracks the next viewer/workspace improvements selected from the OrcaSlicer
comparison: mesh-aware picking, large STL handling, 3MF/project graph support,
and richer G-code preview inspection.

## Guardrails

* Do not run Preview Info enrichment or display-mode work while a slice is in
  progress. Slicing stays the highest-priority operation.
* Keep permanent viewport controls sparse. Detailed preview inspection belongs
  in the existing Preview Info bottom sheet.
* Keep slicing inputs exact. Viewer decimation or display-only mesh changes must
  never replace the original model used by Orca/libslic3r slicing.
* Reuse Orca/libvgcode data where available instead of duplicating slicer
  semantics in Kotlin.

## G-code Preview Inspection

Current status:

* The Preview Info sheet already owns line-type, filament, time, and cost
  inspection.
* Line-type visibility already flows from the sheet to libvgcode through
  `nativeSetGcodeViewerPathVisibility`.

Target:

* Add a Display tab to the Preview Info sheet.
* Let the user switch libvgcode display mode between Auto, feature type,
  filament/color print, speed, temperature, fan, volumetric flow, and layer
  time.
* Keep the sheet hidden and enrichment disabled while `sliceInProgress` is true.
* Future pass: expose color-range legends for scalar display modes.

## Mesh/Face-Aware Picking

Current status:

* Viewer taps now use transformed object bounds and projected bounds only as a
  broad phase.
* Final selection requires a ray/triangle hit against the displayed viewer mesh,
  returning `ViewerPickHit` with object id, triangle index, hit point, normal,
  and distance.
* The pick-hit callback is exposed through the Android viewer surface so
  face-aware tools can use the same hit data without adding another picking
  path.

Target:

* Add a BVH or spatial index before using face picking for repeated pointer
  moves or brush strokes.
* Use face hits for lay-face-on-bed, precise selection, seam/fuzzy/color
  painting, and future support blockers/enforcers.

## Large STL Handling

Current status:

* STL import parses bounds before the Workspace opens, so the viewer can place
  the model without waiting for full render mesh preparation.
* Workspace render preparation uses `PreparedViewerMesh`, cached by file
  identity, with separate source-triangle and display-triangle counts.
* Large STL files are capped to a deterministic display-only triangle budget for
  the Android Prepare viewer. The prepared mesh keeps full-source bounds, and
  slicing still uses the original staged STL path.
* When display LOD is active, Workspace status/performance text shows the
  display-triangle count against the source-triangle count.
* Binary and ASCII STL bounds parsing are allocation-light and do not build full
  vertex/normal render arrays.

Target:

* Add an optional native/Orca-backed mesh simplifier behind the existing
  `PreparedViewerMesh` contract if we need higher visual quality than the
  deterministic display sampler.
* Keep display LOD out of slice-time work. The slice path must not wait on
  viewer mesh preparation.

## 3MF And Project Graph

Current status:

* Workspace viewing is STL-only.

Target:

* Load 3MF through native Orca model graph APIs.
* Expose a lightweight Android scene graph with object ids, volume ids,
  transforms, material assignments, and render meshes.
* Extend saved projects with object/volume ids, per-object/per-volume settings,
  and future facet annotation payloads.
* Keep STL as the simple one-object path over the same plate-object model.
