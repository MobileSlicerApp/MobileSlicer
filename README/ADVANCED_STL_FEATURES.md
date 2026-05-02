# Advanced STL Feature Plan

This document records the OrcaSlicer-aligned implementation plan for cut,
seam painting, fuzzy-skin painting, and multicolor support.

## Orca Reference Shape

Orca keeps simple process controls in `DynamicPrintConfig`, but advanced
model-specific edits live on the model graph:

- `ModelObject` owns one or more `ModelVolume` entries.
- `ModelVolume::config` carries volume-specific config overrides such as
  `extruder`.
- `ModelVolume::seam_facets` stores seam enforcer/blocker painting.
- `ModelVolume::fuzzy_skin_facets` stores fuzzy-skin painting.
- `ModelVolume::mmu_segmentation_facets` stores multicolor/MMU painting.
- `Slic3r::Cut` creates cut result objects/parts from a source
  `ModelObject`, cut matrix, and cut attributes.

Relevant Orca source references:

- `vendor/orcaslicer/src/libslic3r/Model.hpp`
- `vendor/orcaslicer/src/libslic3r/Model.cpp`
- `vendor/orcaslicer/src/libslic3r/TriangleSelector.hpp`
- `vendor/orcaslicer/src/libslic3r/CutUtils.hpp`
- `vendor/orcaslicer/src/libslic3r/GCode/SeamPlacer.cpp`
- `vendor/orcaslicer/src/libslic3r/MultiMaterialSegmentation.cpp`
- `vendor/orcaslicer/src/slic3r/GUI/Gizmos/GLGizmoSeam.*`
- `vendor/orcaslicer/src/slic3r/GUI/Gizmos/GLGizmoFuzzySkin.*`
- `vendor/orcaslicer/src/slic3r/GUI/Gizmos/GLGizmoMmuSegmentation.*`

## Current Implemented Slice

The app now exposes global Orca fuzzy-skin process controls through the
existing profile/config/native-wrapper path:

- `fuzzy_skin`
- `fuzzy_skin_thickness`
- `fuzzy_skin_point_distance`
- `fuzzy_skin_first_layer`
- `fuzzy_skin_mode`
- `fuzzy_skin_noise_type`
- `fuzzy_skin_scale`
- `fuzzy_skin_octaves`
- `fuzzy_skin_persistence`

These are profile-level process controls. They are not painted per-facet
regions yet.

## Required Architecture Before Painting

Painting and cut state must be durable. The app now has a first saved-project
snapshot path for plate objects, copied STL sources, transforms, bounds, and
the full current `ProfileStore` snapshot, plus app-facing project names, save
timestamps, and viewer-captured thumbnails. That is the correct foundation, but
it is not yet a full Orca-style project document because it does not store
per-object config, per-volume config, stable volume ids, or facet annotation
payloads.

Recommended project document fields:

- schema version
- source model references/cache ids
- plate objects and transforms
- object config overrides
- volume config overrides
- serialized `seam_facets`, `fuzzy_skin_facets`, and
  `mmu_segmentation_facets`
- filament/extruder slots and colors
- cut history or cut-result object references

## Native API Target

Add native/JNI calls around the active Orca model graph instead of trying to
encode advanced state into the one-shot process JSON:

- set object extruder
- set volume extruder
- set/clear serialized facet annotation by object, volume, and annotation type
- query object/volume topology and stable ids
- cut object with plane
- cut object with contour
- cut object with groove/dowels

The slice path should continue to call `Print::apply(model, config)` after the
model graph already contains these object/volume/facet edits.

## Implementation Order

1. Profile-level fuzzy-skin process settings.
2. Object-level extruder assignment.
3. Volume-level extruder assignment after object/volume topology is exposed.
4. MobileSlicer project document and project save/load.
5. Extend saved projects with object/volume ids, object overrides, volume
   overrides, and facet annotation payloads.
6. Fuzzy-skin painting via `fuzzy_skin_facets`.
7. Seam painting via `seam_facets`.
8. Plane cut via `Slic3r::Cut`.
9. Advanced cut options: keep upper/lower, flip, place-on-cut, contour cut,
   groove, and dowels.
10. MMU/multicolor painting via `mmu_segmentation_facets`.
11. MMU interlocking process controls and preview colors.

## Guardrails

- Do not treat seam/fuzzy/MMU painting as plain settings. They are facet
  annotations on `ModelVolume`.
- Do not add desktop-Orca GUI code to Android. Reuse the data model and core
  algorithms, then build Android-native interaction on top.
- Keep STL geometry edits native-side. Android should provide the edit intent,
  selection/raycast data, and preview coloring.
- Every new setting must use the current checklist status vocabulary:
  Missing, Parent surfaced subset only, Config only, Config only - Waydroid, or
  Device tested. Add narrower proof qualifiers such as Stronger-fixture proven
  only when the basic status is not specific enough.
