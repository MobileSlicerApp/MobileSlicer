# Orca Profile Mapping Matrix

## Purpose

This file is the future working matrix for MobileSlicer's Orca import/export
translation layer.

Use it for the active Orca printer-import work and for later
Printer / Filament / Process import/export expansion.

This file should answer, for each native field:

* what Orca key it maps to
* whether import is supported
* whether export is supported
* whether the mapping is lossy
* what normalization or collapse rules apply
* what proof/support status the field currently has in the app

This is intended to sit between:

* `README/SETTINGS_CHECKLIST.md`
* `README/OrcaProfileImportExport.md`
* `README/OrcaProfileImportExportImplementation.md`

## Status

This file is now the working ledger for the active Orca printer-import path.

Do not treat blanks here as implementation promises.

Until full import/export becomes active work:

* keep this file lightweight
* add rows gradually as native settings land
* use it to reduce future ambiguity, not to imply current support

The printer-import subset is active now. Rows marked `Initial Import` are the
minimum fields that must be imported when a user adds an Orca printer from
`Select Printer`.

Active printer-import rule:

* `Select Printer` prompts for a nozzle when a printer has multiple Orca nozzle
  variants
* every selected nozzle's fully resolved Orca machine key is retained in
  `PrinterProfile.orcaResolvedMachineJson`
* the native slice config starts from that resolved Orca JSON, then overlays
  MobileSlicer first-class printer fields
* matrix rows below describe first-class Android fields; absence from the
  matrix does not mean an Orca printer key is dropped

Active filament-import rule:

* `Select Filament` currently exposes generic Orca filament material presets
  only
* the picker is a flat material list and does not show source-family categories
* every imported filament retains raw JSON, resolved JSON, and source chain
* native slice config merges the resolved Orca filament JSON before overlaying
  first-class `FilamentProfile` fields
* matrix rows below describe first-class Android fields; absence from the
  matrix does not mean an Orca filament key is dropped

Active process-import rule:

* process presets are imported with the selected Orca printer/nozzle, using
  Orca `compatible_printers` machine-name compatibility
* Process no longer shows local visible built-ins for imported printer/nozzle
  workflows
* every imported process retains raw JSON, resolved JSON, and source chain
* native slice config merges resolved Orca process JSON before overlaying
  first-class `ProcessProfile` fields
* matrix rows below describe first-class Android fields; absence from the
  matrix does not mean an Orca process key is dropped

## How To Use

For each row:

* `Native Field`
  * the MobileSlicer field or concept
* `Native Location`
  * where the field currently lives in code
* `Orca Key(s)`
  * the corresponding Orca key or keys
* `Domain`
  * `Printer`, `Filament`, or `Process`
* `Import`
  * one of:
    * `No`
    * `Planned`
    * `Yes`
* `Export`
  * one of:
    * `No`
    * `Planned`
    * `Yes`
* `Lossy`
  * one of:
    * `No`
    * `Yes`
    * `Partial`
* `Import Rule`
  * how Orca data is translated into native storage
* `Export Rule`
  * how native storage is written back to Orca
* `Normalization / Notes`
  * percent stripping, enum mapping, range collapse, rectangularization, etc.
* `Proof / Support Status`
  * reference the honest current state from `README/SETTINGS_CHECKLIST.md`

Recommended conventions:

* if one native field collapses multiple Orca keys, list all relevant Orca keys
* if one Orca key expands into multiple native fields, use multiple rows and
  explain the split
* if the field is app-only with no Orca equivalent, write `None`
* if mapping is not yet known, write `TBD`, not guesses

## Recommended Workflow

When a new native setting lands:

1. update `README/SETTINGS_CHECKLIST.md`
2. update this matrix row or add a new one
3. note whether mapping is expected to be exact or lossy
4. note whether import/export should eventually support it

When Orca import/export implementation begins:

1. audit this matrix against the real native profile schema
2. convert `Planned` rows into concrete implementation targets
3. implement mappings in bounded clusters
4. keep rows honest as behavior is verified

## Matrix

| Domain | Native Field | Native Location | Orca Key(s) | Import | Export | Lossy | Import Rule | Export Rule | Normalization / Notes | Proof / Support Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Printer | Import source | `ProfileModels.kt` future `PrinterProfile.source` | Orca machine model path / generated catalog entry | Initial Import | No | No | Store imported printer as Orca-backed, not built-in and not custom | N/A | Replaces old curated built-in printer list | UI only |
| Printer | Full resolved machine config | `PrinterProfile.orcaResolvedMachineJson`; `ActiveSlicerConfiguration.toNativeSliceConfigJson()` | all keys in resolved nozzle `machine` preset | Yes | Planned | No | Preserve the complete resolved Orca machine JSON and use it as the base native slice config | Export from preserved resolved config plus first-class overrides | This is the non-lossy coverage path for every Orca printer key without a dedicated Android field yet | Config wired |
| Printer | Cover image | `ProfileModels.kt` future thumbnail field | `{name}_cover.png` beside Orca profile family | Initial Import | No | No | Store generated asset path or copied persisted image path | N/A | Used where letter tile currently appears | UI only |
| Printer | Bed width / depth | `ProfileModels.kt` `PrinterProfile.bedWidthMm` / `bedDepthMm` | resolved `printable_area` | Initial Import | Planned | Partial | Parse resolved Orca printable-area polygon and derive rectangular bounds for current workspace bed cache | Emit rectangular Orca printable-area polygon from width/depth | Preserve original polygon in raw/resolved Orca payload; non-rectangular beds collapse only for current Android bed display | Device tested |
| Printer | Max height | `ProfileModels.kt` `PrinterProfile.maxHeightMm` | resolved `printable_height` | Initial Import | Planned | No | Parse numeric height from resolved nozzle machine preset | Emit numeric height | Validate `> 0`; required for workspace out-of-bounds checks | Device tested |
| Printer | Available nozzles | `ProfileModels.kt` future nozzle list field | machine model `nozzle_diameter`; nozzle machine presets | Initial Import | Planned | No | Store all nozzle diameters and associated machine preset paths | Emit matching nozzle variant presets | Do not import one row per nozzle as separate printers | UI only |
| Printer | Active nozzle diameter | `ProfileModels.kt` `PrinterProfile.nozzleDiameterMm` | resolved `nozzle_diameter` | Initial Import | Planned | No | Default to Orca's preferred/common nozzle if present, otherwise first sorted nozzle, with future UI selection | Emit numeric nozzle diameter | Validate `> 0`; do not discard other nozzle variants | Device tested |
| Printer | Filament diameter | `ProfileModels.kt` `PrinterProfile.filamentDiameterMm` | `filament_diameter` | Planned | Planned | No | Parse numeric filament diameter | Emit numeric filament diameter | Validate `> 0` | Device tested |
| Filament | Full resolved filament config | `FilamentProfile.orcaResolvedFilamentJson`; `ActiveSlicerConfiguration.toNativeSliceConfigJson()` | all keys in resolved filament preset | Yes | Planned | No | Preserve the complete resolved Orca filament JSON and merge it into native slice config | Export from preserved resolved config plus first-class overrides | Non-lossy coverage path for generic filament keys without dedicated Android fields yet | Config wired |
| Filament | Material type | `ProfileModels.kt` `FilamentProfile.materialType` | `filament_type` | Planned | Planned | Partial | Import Orca material string directly when supported | Emit native material string directly | Unsupported Orca material names may require fallback or warning | Config only |
| Filament | First-layer nozzle temp | `ProfileModels.kt` `FilamentProfile.nozzleTemperatureInitialLayerC` | `nozzle_temperature_initial_layer` | Planned | Planned | No | Parse numeric temperature | Emit numeric temperature | Validate sane range | Device tested |
| Filament | Other-layers nozzle temp | `ProfileModels.kt` `FilamentProfile.nozzleTemperatureC` | `nozzle_temperature` | Planned | Planned | No | Parse numeric temperature | Emit numeric temperature | Validate sane range | Device tested |
| Filament | First-layer bed temp | `ProfileModels.kt` `FilamentProfile.bedTemperatureInitialLayerC` | `bed_temperature_initial_layer` | Planned | Planned | No | Parse numeric temperature | Emit numeric temperature | Validate sane range | Device tested |
| Filament | Other-layers bed temp | `ProfileModels.kt` `FilamentProfile.bedTemperatureC` | `bed_temperature` | Planned | Planned | No | Parse numeric temperature | Emit numeric temperature | Validate sane range | Device tested |
| Filament | Cooling baseline | `ProfileModels.kt` `FilamentProfile.coolingPercent` | `fan_min_speed`, `fan_max_speed` | Planned | Planned | Yes | Collapse Orca fan min/max into one app baseline | Export one app baseline into both Orca fan min/max keys | If min != max, import requires warning and documented collapse rule | Device tested |
| Filament | No cooling first layers | `ProfileModels.kt` `FilamentProfile.noCoolingFirstLayers` | `close_fan_the_first_x_layers` | Planned | Planned | No | Parse integer layer count | Emit integer layer count | Validate `>= 0` | Device tested |
| Process | First layer height | `ProfileModels.kt` `ProcessProfile.firstLayerHeightMm` | `first_layer_height` | Planned | Planned | No | Parse numeric height | Emit numeric height | Validate `> 0` | Device tested |
| Process | Layer height | `ProfileModels.kt` `ProcessProfile.layerHeightMm` | `layer_height` | Planned | Planned | No | Parse numeric height | Emit numeric height | Validate `> 0` | Device tested |
| Process | First-layer print speed | `ProfileModels.kt` `ProcessProfile.firstLayerPrintSpeedMmPerSec` | `initial_layer_speed`, `first_layer_print_speed` | Planned | Planned | Partial | Prefer direct Orca first-layer speed key | Export as canonical Orca first-layer speed key | Keep alias handling documented | Device tested |
| Process | First-layer infill speed | `ProfileModels.kt` `ProcessProfile.firstLayerInfillSpeedMmPerSec` | `initial_layer_infill_speed`, `first_layer_infill_speed` | Planned | Planned | Partial | Prefer direct Orca first-layer infill key | Export as canonical Orca first-layer infill key | Keep alias handling documented | Device tested |
| Process | First-layer travel speed | `ProfileModels.kt` `ProcessProfile.firstLayerTravelSpeedPercent` | `initial_layer_travel_speed` | Planned | Planned | Partial | Import Orca travel speed representation into native percent form | Export native percent to Orca-compatible value | Representation may be percent-based rather than absolute | Device tested |
| Process | Slow down layers | `ProfileModels.kt` `ProcessProfile.slowDownLayers` | `slow_down_layers` | Planned | Planned | No | Parse integer count | Emit integer count | Validate `>= 0` | Device tested |
| Process | Outer wall speed | `ProfileModels.kt` `ProcessProfile.outerWallSpeedMmPerSec` | `outer_wall_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `> 0` | Device tested |
| Process | Inner wall speed | `ProfileModels.kt` `ProcessProfile.innerWallSpeedMmPerSec` | `inner_wall_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `> 0` | Device tested |
| Process | Top surface speed | `ProfileModels.kt` `ProcessProfile.topSurfaceSpeedMmPerSec` | `top_surface_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `> 0` | Device tested |
| Process | Travel speed | `ProfileModels.kt` `ProcessProfile.travelSpeedMmPerSec` | `travel_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `> 0` | Device tested |
| Process | Outer wall acceleration | `ProfileModels.kt` `ProcessProfile.outerWallAccelerationMmPerSec2` | `outer_wall_acceleration` | Planned | Planned | No | Parse numeric acceleration | Emit numeric acceleration | Validate `>= 0` | Device tested |
| Process | Inner wall acceleration | `ProfileModels.kt` `ProcessProfile.innerWallAccelerationMmPerSec2` | `inner_wall_acceleration` | Planned | Planned | No | Parse numeric acceleration | Emit numeric acceleration | Validate `>= 0` | Config only |
| Process | Top surface acceleration | `ProfileModels.kt` `ProcessProfile.topSurfaceAccelerationMmPerSec2` | `top_surface_acceleration` | Planned | Planned | No | Parse numeric acceleration | Emit numeric acceleration | Validate `>= 0` | Device tested |
| Process | Sparse infill acceleration | `ProfileModels.kt` `ProcessProfile.sparseInfillAccelerationMmPerSec2` | `sparse_infill_acceleration` | Planned | Planned | No | Parse numeric acceleration | Emit numeric acceleration | Validate `>= 0` | Device tested |
| Process | Bridge speed | `ProfileModels.kt` `ProcessProfile.bridgeSpeedMmPerSec` | `bridge_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `> 0` | Device tested |
| Process | Small perimeter speed | `ProfileModels.kt` `ProcessProfile.smallPerimeterSpeedMmPerSec` | `small_perimeter_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `>= 0` | Device tested |
| Process | Small perimeter threshold | `ProfileModels.kt` `ProcessProfile.smallPerimeterThresholdMm` | `small_perimeter_threshold` | Planned | Planned | No | Parse numeric threshold | Emit numeric threshold | Validate `>= 0` | Device tested |
| Process | Sparse infill speed | `ProfileModels.kt` `ProcessProfile.sparseInfillSpeedMmPerSec` | `sparse_infill_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `> 0` | Device tested |
| Process | Internal solid infill speed | `ProfileModels.kt` `ProcessProfile.internalSolidInfillSpeedMmPerSec` | `internal_solid_infill_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `> 0` | Device tested |
| Process | Gap infill speed | `ProfileModels.kt` `ProcessProfile.gapInfillSpeedMmPerSec` | `gap_infill_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Validate `>= 0` | Config only |
| Process | Slow down for overhang | `ProfileModels.kt` `ProcessProfile.enableOverhangSpeed` | `enable_overhang_speed` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Speed > Overhang speed`, advanced row | Config only |
| Process | Slow down for curled perimeters | `ProfileModels.kt` `ProcessProfile.slowdownForCurledPerimeters` | `slowdown_for_curled_perimeters` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Speed > Overhang speed`, advanced row | Config only |
| Process | Internal solid infill acceleration | `ProfileModels.kt` `ProcessProfile.internalSolidInfillAcceleration` | `internal_solid_infill_acceleration` | Planned | Planned | No | Parse float-or-percent string | Emit float-or-percent string | Orca `Speed > Acceleration`, advanced row | Config only |
| Process | Travel acceleration | `ProfileModels.kt` `ProcessProfile.travelAccelerationMmPerSec2` | `travel_acceleration` | Planned | Planned | No | Parse numeric acceleration | Emit numeric acceleration | Orca `Speed > Acceleration`, advanced row | Config only |
| Process | Enable accel_to_decel | `ProfileModels.kt` `ProcessProfile.accelToDecelEnable` | `accel_to_decel_enable` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Speed > Acceleration`, advanced row | Config only |
| Process | accel_to_decel | `ProfileModels.kt` `ProcessProfile.accelToDecelFactorPercent` | `accel_to_decel_factor` | Planned | Planned | No | Parse percent integer | Emit percent integer | Orca `Speed > Acceleration`, advanced row | Config only |
| Process | Junction deviation | `ProfileModels.kt` `ProcessProfile.defaultJunctionDeviationMm` | `default_junction_deviation` | Planned | Planned | No | Parse numeric distance | Emit numeric distance | Orca `Speed > Jerk(XY)`, advanced row | Config only |
| Process | Default jerk | `ProfileModels.kt` `ProcessProfile.defaultJerkMmPerSec` | `default_jerk` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Orca `Speed > Jerk(XY)`, advanced row | Config only |
| Process | Infill jerk | `ProfileModels.kt` `ProcessProfile.infillJerkMmPerSec` | `infill_jerk` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Orca `Speed > Jerk(XY)`, advanced row | Config only |
| Process | Top surface jerk | `ProfileModels.kt` `ProcessProfile.topSurfaceJerkMmPerSec` | `top_surface_jerk` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Orca `Speed > Jerk(XY)`, advanced row | Config only |
| Process | Travel jerk | `ProfileModels.kt` `ProcessProfile.travelJerkMmPerSec` | `travel_jerk` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Orca `Speed > Jerk(XY)`, advanced row | Config only |
| Process | Extrusion rate smoothing | `ProfileModels.kt` `ProcessProfile.maxVolumetricExtrusionRateSlope` | `max_volumetric_extrusion_rate_slope` | Planned | Planned | No | Parse numeric slope | Emit numeric slope | Orca `Speed > Advanced`, advanced row | Config only |
| Process | Smoothing segment length | `ProfileModels.kt` `ProcessProfile.maxVolumetricExtrusionRateSlopeSegmentLengthMm` | `max_volumetric_extrusion_rate_slope_segment_length` | Planned | Planned | No | Parse numeric length | Emit numeric length | Orca `Speed > Advanced`, advanced row | Config only |
| Process | Apply smoothing only on external features | `ProfileModels.kt` `ProcessProfile.extrusionRateSmoothingExternalPerimeterOnly` | `extrusion_rate_smoothing_external_perimeter_only` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Speed > Advanced`, advanced row | Config only |
| Process | Top shell layers | `ProfileModels.kt` `ProcessProfile.topShellLayers` | `top_shell_layers` | Planned | Planned | No | Parse integer shell count | Emit integer shell count | Validate `>= 0` | Device tested |
| Process | Bottom shell layers | `ProfileModels.kt` `ProcessProfile.bottomShellLayers` | `bottom_shell_layers` | Planned | Planned | No | Parse integer shell count | Emit integer shell count | Validate `>= 0` | Device tested |
| Process | Seam position | `ProfileModels.kt` `ProcessProfile.seamPosition` | `seam_position` | Planned | Planned | Partial | Normalize Orca seam enum to app enum | Emit app enum as Orca canonical enum string | Unsupported Orca enum modes must warn | Device tested |
| Process | Staggered inner seams | `ProfileModels.kt` `ProcessProfile.staggeredInnerSeams` | `staggered_inner_seams` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Quality > Seam`, advanced row | Config only |
| Process | Role based wipe speed | `ProfileModels.kt` `ProcessProfile.roleBasedWipeSpeed` | `role_based_wipe_speed` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Quality > Seam`, advanced row | Config only |
| Process | Wipe speed | `ProfileModels.kt` `ProcessProfile.wipeSpeed` | `wipe_speed` | Planned | Planned | No | Parse float-or-percent string | Emit float-or-percent string | Orca `Quality > Seam`, advanced row | Config only |
| Process | Wipe on loops | `ProfileModels.kt` `ProcessProfile.wipeOnLoops` | `wipe_on_loops` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Quality > Seam`, advanced row | Config only |
| Process | Wipe before external loop | `ProfileModels.kt` `ProcessProfile.wipeBeforeExternalLoop` | `wipe_before_external_loop` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Quality > Seam`, advanced row | Config only |
| Process | Precise outer wall | `ProfileModels.kt` `ProcessProfile.preciseOuterWall` | `precise_outer_wall` | Planned | Planned | No | Parse boolean | Emit boolean | Validate boolean parse | Device tested |
| Process | Slice gap closing radius | `ProfileModels.kt` `ProcessProfile.sliceClosingRadiusMm` | `slice_closing_radius` | Planned | Planned | No | Parse numeric radius | Emit numeric radius | Orca `Quality > Precision`, advanced row | Config only |
| Process | Only one wall top surfaces | `ProfileModels.kt` `ProcessProfile.onlyOneWallTopSurfaces` | `only_one_wall_top` | Planned | Planned | No | Parse boolean | Emit boolean | Validate boolean parse | Device tested |
| Process | Top surface pattern | `ProfileModels.kt` `ProcessProfile.topSurfacePattern` | `top_surface_pattern` | Planned | Planned | Partial | Normalize Orca top-surface enum to app enum | Emit app enum as Orca canonical enum string | Unsupported Orca enum modes must warn | Device tested |
| Process | Wall transition length | `ProfileModels.kt` `ProcessProfile.wallTransitionLengthPercent` | `wall_transition_length` | Planned | Planned | No | Parse percent integer | Emit percent integer | Orca `Quality > Wall generator`, advanced row | Config only |
| Process | Wall transition filter deviation | `ProfileModels.kt` `ProcessProfile.wallTransitionFilterDeviationPercent` | `wall_transition_filter_deviation` | Planned | Planned | No | Parse percent integer | Emit percent integer | Orca `Quality > Wall generator`, advanced row | Config only |
| Process | Wall transition angle | `ProfileModels.kt` `ProcessProfile.wallTransitionAngleDegrees` | `wall_transition_angle` | Planned | Planned | No | Parse numeric angle | Emit numeric angle | Orca `Quality > Wall generator`, advanced row | Config only |
| Process | Wall distribution count | `ProfileModels.kt` `ProcessProfile.wallDistributionCount` | `wall_distribution_count` | Planned | Planned | No | Parse integer count | Emit integer count | Orca `Quality > Wall generator`, advanced row | Config only |
| Process | Minimum wall width | `ProfileModels.kt` `ProcessProfile.minBeadWidthPercent` | `min_bead_width` | Planned | Planned | No | Parse percent integer | Emit percent integer | Orca `Quality > Wall generator`, advanced row | Config only |
| Process | Minimum feature size | `ProfileModels.kt` `ProcessProfile.minFeatureSizePercent` | `min_feature_size` | Planned | Planned | No | Parse percent integer | Emit percent integer | Orca `Quality > Wall generator`, advanced row | Config only |
| Process | Minimum wall length | `ProfileModels.kt` `ProcessProfile.minLengthFactorMm` | `min_length_factor` | Planned | Planned | No | Parse numeric length | Emit numeric length | Orca `Quality > Wall generator`, advanced row | Config only |
| Process | Wall count | `ProfileModels.kt` `ProcessProfile.wallCount` | `wall_loops` | Planned | Planned | No | Parse integer wall count | Emit integer wall count | Validate `>= 0` | Device tested |
| Process | Infill percent | `ProfileModels.kt` `ProcessProfile.infillPercent` | `sparse_infill_density` | Planned | Planned | Partial | Parse numeric or percent-string density | Emit percent-compatible Orca density form | Strip `%` on import; define export format consistently | Device tested |
| Process | Sparse infill pattern | `ProfileModels.kt` `ProcessProfile.sparseInfillPattern` | `sparse_infill_pattern` | Planned | Planned | Partial | Normalize Orca infill enum to app enum | Emit app enum as Orca canonical enum string | Unsupported Orca enum modes must warn | Device tested |
| Process | Skirts | `ProfileModels.kt` `ProcessProfile.skirts` | `skirts`, `skirt_loops` | Planned | Planned | Partial | Accept either Orca alias | Export as canonical Orca skirt-loop key | Keep alias handling documented | Device tested |
| Process | Skirt minimum extrusion length | `ProfileModels.kt` `ProcessProfile.minSkirtLengthMm` | `min_skirt_length` | Planned | Planned | No | Parse numeric length | Emit numeric length | Orca `Others > Skirt`, advanced row | Config only |
| Process | Skirt start point | `ProfileModels.kt` `ProcessProfile.skirtStartAngleDegrees` | `skirt_start_angle` | Planned | Planned | No | Parse numeric angle | Emit numeric angle | Orca `Others > Skirt`, advanced row | Config only |
| Process | Skirt speed | `ProfileModels.kt` `ProcessProfile.skirtSpeedMmPerSec` | `skirt_speed` | Planned | Planned | No | Parse numeric speed | Emit numeric speed | Orca `Others > Skirt`, advanced row | Config only |
| Process | Brim width | `ProfileModels.kt` `ProcessProfile.brimWidthMm` | `brim_width` | Planned | Planned | Partial | Parse numeric brim width | Emit brim width and any required companion brim-mode rule | Orca may require explicit brim mode semantics beyond width alone | Device tested |
| Process | Brim follows compensated outline | `ProfileModels.kt` `ProcessProfile.brimUseEfcOutline` | `brim_use_efc_outline` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Others > Brim`, advanced row | Config only |
| Process | Combine brims | `ProfileModels.kt` `ProcessProfile.combineBrims` | `combine_brims` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Others > Brim`, advanced row | Config only |
| Process | Slicing mode | `ProfileModels.kt` `ProcessProfile.slicingMode` | `slicing_mode` | Planned | Planned | No | Normalize Orca enum to app enum | Emit app enum as Orca canonical enum string | Orca `Others > Special mode`, advanced row | Config only |
| Process | Intra-layer order | `ProfileModels.kt` `ProcessProfile.printOrder` | `print_order` | Planned | Planned | No | Normalize Orca enum to app enum | Emit app enum as Orca canonical enum string | Orca `Others > Special mode`, advanced row | Config only |
| Process | Support threshold overlap | `ProfileModels.kt` `ProcessProfile.supportThresholdOverlap` | `support_threshold_overlap` | Planned | Planned | No | Parse float-or-percent string | Emit float-or-percent string | Orca `Support > Support`, simple row | Config only |
| Process | Support critical regions only | `ProfileModels.kt` `ProcessProfile.supportCriticalRegionsOnly` | `support_critical_regions_only` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Support > Support`, advanced row | Config only |
| Process | Ignore small overhangs | `ProfileModels.kt` `ProcessProfile.supportRemoveSmallOverhang` | `support_remove_small_overhang` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Support > Support`, advanced row | Config only |
| Process | Avoid interface filament for base | `ProfileModels.kt` `ProcessProfile.supportInterfaceNotForBody` | `support_interface_not_for_body` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Support > Support filament`, simple row | Config only |
| Process | Support pattern angle | `ProfileModels.kt` `ProcessProfile.supportAngleDegrees` | `support_angle` | Planned | Planned | No | Parse numeric angle | Emit numeric angle | Orca `Support > Advanced`, advanced row | Config only |
| Process | Support object first layer gap | `ProfileModels.kt` `ProcessProfile.supportObjectFirstLayerGapMm` | `support_object_first_layer_gap` | Planned | Planned | No | Parse numeric gap | Emit numeric gap | Orca `Support > Advanced`, advanced row | Config only |
| Process | Tree support tip diameter | `ProfileModels.kt` `ProcessProfile.treeSupportTipDiameterMm` | `tree_support_tip_diameter` | Planned | Planned | No | Parse numeric diameter | Emit numeric diameter | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support branch distance | `ProfileModels.kt` `ProcessProfile.treeSupportBranchDistanceMm` | `tree_support_branch_distance` | Planned | Planned | No | Parse numeric distance | Emit numeric distance | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support organic branch distance | `ProfileModels.kt` `ProcessProfile.treeSupportBranchDistanceOrganicMm` | `tree_support_branch_distance_organic` | Planned | Planned | No | Parse numeric distance | Emit numeric distance | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support branch density | `ProfileModels.kt` `ProcessProfile.treeSupportTopRatePercent` | `tree_support_top_rate` | Planned | Planned | No | Parse percent integer | Emit percent integer | Orca labels this row `Branch Density` | Config only |
| Process | Tree support branch diameter | `ProfileModels.kt` `ProcessProfile.treeSupportBranchDiameterMm` | `tree_support_branch_diameter` | Planned | Planned | No | Parse numeric diameter | Emit numeric diameter | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support organic branch diameter | `ProfileModels.kt` `ProcessProfile.treeSupportBranchDiameterOrganicMm` | `tree_support_branch_diameter_organic` | Planned | Planned | No | Parse numeric diameter | Emit numeric diameter | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support branch diameter angle | `ProfileModels.kt` `ProcessProfile.treeSupportBranchDiameterAngleDegrees` | `tree_support_branch_diameter_angle` | Planned | Planned | No | Parse numeric angle | Emit numeric angle | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support branch angle | `ProfileModels.kt` `ProcessProfile.treeSupportBranchAngleDegrees` | `tree_support_branch_angle` | Planned | Planned | No | Parse numeric angle | Emit numeric angle | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support organic branch angle | `ProfileModels.kt` `ProcessProfile.treeSupportBranchAngleOrganicDegrees` | `tree_support_branch_angle_organic` | Planned | Planned | No | Parse numeric angle | Emit numeric angle | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support preferred branch angle | `ProfileModels.kt` `ProcessProfile.treeSupportPreferredBranchAngleDegrees` | `tree_support_angle_slow` | Planned | Planned | No | Parse numeric angle | Emit numeric angle | Orca labels this row `Preferred Branch Angle` | Config only |
| Process | Tree support auto brim | `ProfileModels.kt` `ProcessProfile.treeSupportAutoBrim` | `tree_support_auto_brim` | Planned | Planned | No | Parse boolean | Emit boolean | Orca `Support > Tree supports`, advanced row | Config only |
| Process | Tree support brim width | `ProfileModels.kt` `ProcessProfile.treeSupportBrimWidthMm` | `tree_support_brim_width` | Planned | Planned | No | Parse numeric width | Emit numeric width | Orca `Support > Tree supports`, advanced row | Config only |

## Placeholder Rows For Future Settings

Add future settings below as they land.

| Domain | Native Field | Native Location | Orca Key(s) | Import | Export | Lossy | Import Rule | Export Rule | Normalization / Notes | Proof / Support Status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Printer | TBD | TBD | TBD | No | No | TBD | TBD | TBD | TBD | TBD |
| Filament | TBD | TBD | TBD | No | No | TBD | TBD | TBD | TBD | TBD |
| Process | TBD | TBD | TBD | No | No | TBD | TBD | TBD | TBD | TBD |

## Notes

When Orca import/export becomes active work:

* this file should become the main per-field translation ledger
* rows should be updated only when behavior is real and understood
* temporary guesses should be removed in favor of exact rules

Until then, this file should remain a disciplined planning matrix rather than a
marketing surface.
