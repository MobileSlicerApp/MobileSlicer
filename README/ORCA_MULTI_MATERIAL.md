# Orca Multi-Material / Multi-Nozzle Boundary

This document replaces the old MobileSlicer-specific "AMS" implementation
model.

MobileSlicer should keep OrcaSlicer's slicer semantics:

* project material slots are logical Orca filaments/tools
* object assignment is stored as Orca `extruder`, using Orca's 1-based ids
* filament/profile vectors such as `filament_colour`, `filament_type`,
  `filament_settings_id`, temperatures, cooling, flow, and density describe
  logical material slots
* physical printer capability stays in printer/nozzle vectors such as
  `nozzle_diameter`, `extruder_offset`, `physical_extruder_map`,
  `printer_extruder_id`, and `master_extruder_id`
* `filament_map` maps logical material slots to physical nozzles/extruders
* `flush_volumes_matrix` size is:
  `material_slot_count * material_slot_count * physical_nozzle_count`

Do not build a separate app-owned AMS slicer subsystem. If an Orca printer
profile contains AMS-related metadata such as `extruder_ams_count`, preserve it
as Orca printer metadata and pass it through the normal profile/config pipeline.

## Current Runtime Contract

The existing slot strip UI is still the mobile presentation. Internally it is a
plate/project material-slot surface:

* `PlateObject.filamentSlotIndex` is the selected logical material/tool id.
* `PlateFilamentSlot` stores the selected filament profile, display label,
  material type, color, and optional 1-based physical nozzle binding for a
  logical slot.
* `PlateFlushVolumes` stores Orca flush data for the active plate.
* the selected profile-menu filament is only a seed for a brand-new empty
  printer slot state. Once plate/printer slots exist, the slot's filament
  profile, material, color, temperatures, flow, and Orca ids are the slicer
  source of truth.
* populated slots are resynchronized from their current `FilamentProfile` ids
  when profile data changes, so editing a filament profile updates the slot's
  slicer material identity instead of leaving stale ABS/PLA/profile values in
  the plate state. Slot color is preserved as a plate/printer-slot override
  during this sync, because the slot strip color can intentionally differ from
  the filament profile's default color and must survive app restart.
* printer-default slot persistence writes to `printer_material_slots`, keyed by
  the printer profile instance id, not the printer name or model.
* legacy app-owned AMS preference migration has been removed; do not read or
  write `printer_ams_slots`.

When a printer profile is deleted, MobileSlicer must also delete that printer
profile instance's saved material-slot preference. Re-importing the same Orca
printer preset creates a new profile instance when an existing profile with the
same deterministic Orca id is already present, so two printers of the same kind
do not share saved material slots.

Do not let the active profile-menu filament override a populated slot strip.
That would diverge from Orca: Orca's process/profile selection can provide
defaults, but the plate's logical filament slots are what determine
`filament_type`, `filament_settings_id`, colors, temperatures, flow, and the
object `extruder` assignments that reach slicing.

Native plate loading must carry object material assignment. The JNI and C API
therefore pass one 1-based `extruderId` per loaded plate object:

```text
nativeLoadPlateModels(handle, paths, transforms, extruderIds)
orca_load_plate_models(engine, paths, transforms, extruder_ids, count)
```

The wrapper sets `ModelObject::config["extruder"]` on each combined object.
Volume-level `extruder` should only be introduced when MobileSlicer supports
explicit per-volume material assignment or 3MF color/painting import.

## Mode Rules

For one physical nozzle with multiple logical material slots:

* `single_extruder_multi_material = true`
* `filament_map = [1, 1, ...]`
* flush matrix has one nozzle plane
* prime/wipe tower behavior may be enabled by the active process/profile

For multiple physical nozzles:

* `single_extruder_multi_material = false`
* preserve Orca physical nozzle vectors
* default to `filament_map_mode = Auto For Flush`, matching Orca's unconnected
  desktop behavior, so Orca can recommend the logical-slot-to-physical-nozzle
  map during tool ordering
* reserve `filament_map_mode = Auto For Match` for a future connected-printer
  state where MobileSlicer knows the live loaded-material/nozzle state, matching
  Orca's GUI fallback behavior
* use `filament_map_mode = Manual` only when MobileSlicer exposes and emits an
  explicit user-selected slot-to-nozzle map. Internally this is represented as
  `PlateFilamentSlot.physicalNozzleIndex`; current UI does not expose it, so
  normal slices stay Orca-auto unless restored project data or future UI sets a
  physical nozzle binding.
* flush matrix has one plane per physical nozzle
* prime tower is still controlled by Orca's `enable_prime_tower` semantics, not
  by physical nozzle count. H2D-style printers can use multiple physical nozzles
  and still generate a prime tower when a multi-material plate needs one.
* per-nozzle printable-area vectors are normalized at the Orca `Print` boundary:
  if `extruder_printable_area` is missing, shorter than `nozzle_diameter`, or
  contains an empty group, that physical nozzle falls back to the full bed
  printable polygon. This protects every multi-nozzle caller, including brim,
  skirt, conflict checks, and timelapse/safe-position logic, while preserving
  explicit valid per-nozzle areas from the printer profile.

For Bambu/AMS-like printers, the same rule applies: represent the print as Orca
logical filaments plus Orca printer metadata. Do not add an app-specific AMS
translation layer.

## Orca Multi-Nozzle Source Contract

The relevant Orca source behavior is:

* `Extruder::extruder_id()` maps a logical filament/tool id through
  `filament_map` and subtracts one, so `filament_map` is serialized as 1-based
  physical nozzle ids and consumed internally as 0-based ids
  (`vendor/orcaslicer/src/libslic3r/Extruder.cpp`).
* wipe tower integration builds per-logical-filament extruder offsets from
  `extruder_offset[filament_map[idx] - 1]`, so physical nozzle offsets must
  remain intact (`vendor/orcaslicer/src/libslic3r/GCode.hpp`).
* `nozzle_diameter.values.size()` is used as the physical nozzle/extruder count
  when Orca builds per-nozzle flush matrices and filament grouping
  (`vendor/orcaslicer/src/libslic3r/GCode/ToolOrdering.cpp`).
* Orca slices `flush_volumes_matrix` into one matrix plane per physical nozzle
  with `get_flush_volumes_matrix(..., nozzle_id, nozzle_nums)`
  (`vendor/orcaslicer/src/libslic3r/PrintConfig.hpp`).
* Orca's `filament_map_mode` supports `Auto For Flush`, `Auto For Match`, and
  `Manual` (`vendor/orcaslicer/src/libslic3r/PrintConfig.hpp`). In auto modes,
  Orca may update the effective map while applying the print. MobileSlicer should
  not force a round-robin physical-nozzle assignment unless the user explicitly
  selected a manual map.

This is the target, not an app-owned AMS model. MobileSlicer material slots are
logical Orca filaments; the selected printer profile defines physical nozzles;
`filament_map` is the boundary between them.

## Completed Slot/Profile Boundary

Implemented on 2026-04-29:

* saved printer material slots are scoped to the printer profile instance id via
  `printer_material_slots`; deleting a printer profile clears that instance's
  slot defaults, and two profiles with the same display name do not share slot
  state.
* saved plate projects persist the optional `physicalNozzleIndex` on each
  `PlateFilamentSlot`, matching Orca's 1-based `filament_map` convention.
* the slot strip wins over the profile-menu filament once slots exist. The
  selected profile filament seeds an empty strip, but active slicing resolves
  filament vectors from each slot's filament profile id.
* the Orca filament repair pass now uses the first active slot filament's
  overrides for a single-slot slice, instead of blindly applying the currently
  selected profile-menu filament overrides.
* multi-nozzle config emission preserves Orca's physical printer vectors,
  writes `single_extruder_multi_material = false`, sizes
  `flush_volumes_matrix` as `slot_count * slot_count * physical_nozzle_count`,
  and emits `filament_map` from explicit slot nozzle bindings when present.
* the workspace slot sheet exposes manual nozzle binding only when the selected
  printer resolves to more than one physical nozzle. Single-nozzle multi-filament
  printers do not show this control. The UI choices are `Auto`, `N1`, `N2`, ...
  where `Auto` leaves Orca in `Auto For Flush` mode and explicit `N#` choices
  serialize a manual 1-based physical nozzle id into `filament_map`.

This closes the old AMS persistence and profile-precedence bugs without adding a
MobileSlicer-owned AMS translation layer.

## Verification Targets

Required proof before claiming full support:

* two STL objects assigned to two slots on a one-nozzle printer produce real
  Orca material/tool-change behavior in emitted G-code. This is verified; keep
  it as a regression target.
* one STL object assigned to a non-first slot on a one-nozzle printer emits
  G-code that uses only that selected logical slot. This is verified; do not
  regress it while changing the multi-material pipeline.
* two STL objects assigned to two slots on a two-nozzle printer use the expected
  physical tools
* changing a plate object's slot invalidates the native plate-load signature and
  reloads the model
* `flush_volumes_matrix` length matches Orca's expected
  `slot_count * slot_count * nozzle_count`

## Verified Single-Nozzle Multicolor

Verified on device over wireless ADB on 2026-04-29 with two Benchy plate
objects, one Qidi Q-series physical nozzle, and two ABS material slots.

The generated G-code now matches Orca's single-nozzle multi-material semantics:

* object 1 loads as Orca `extruder = 1`
* object 2 loads as Orca `extruder = 2`
* Orca tool ordering reports two logical tools: `[0, 1]`
* `filament_map = 1,1`
* `single_extruder_multi_material = 1`
* `nozzle_diameter = 0.4`
* `wipe_tower_type = type1`
* `wipe_tower_wall_type = rectangle`
* `flush_volumes_matrix = 0,104,97,0`

Device proof from `cache/latest-slice-plate_2_objects_ABS.gcode`:

* slice completed with `sliced=true`
* Orca planned `240` wipe/toolchange layers
* G-code contains `121` `T0` commands and `120` `T1` commands
* G-code contains `241` `TOOL_CHANGE_START` markers
* G-code contains `480` `CP TOOLCHANGE` markers
* filament usage is nonzero for both logical materials:
  `12215.30, 12398.65 mm`

The first Android-specific fix was that the Android libslic3r boundary had
stubbed `WipeTower::plan_toolchange()` and `WipeTower::generate_new()` so Orca
planned logical tool changes but exported no wipe/toolchange records. Android
now builds Orca's real `GCode/WipeTower.cpp` Type1 implementation and the real
`Triangulation.cpp` dependency. The remaining Android stubs no longer replace
Type1 `WipeTower` generation.

Skirt/brim integration must preserve Orca's fake wipe tower coordinate
contract. Orca's brim code normally translates fake tower outlines by
`fake_wipe_tower.pos`, because desktop-generated fake tower walls are local to
the tower. Android's real Type1 tower generator may provide an already
positioned outer wall. The mobile build detects outlines already inside the
expected `pos + width/depth + brim` tower footprint and skips the second
translation; otherwise the skirt can be emitted hundreds of millimeters outside
the bed even though the prime tower itself was clamped onto the printable area.

The same normalization is required for `Print::first_layer_wipe_tower_corners()`.
That function feeds `_make_skirt()`; if it adds `wipe_tower_x/y` to an already
positioned Android Type1 tower bounding box, the combined skirt hull wraps a
phantom tower at roughly double the intended tower coordinates.

Verified on device on 2026-04-29 with the Qidi Q2 profile, six Benchy objects,
one physical nozzle, and three logical material slots. Before the coordinate
normalization, Orca exported valid logical tool changes but the combined skirt
included a phantom tower at roughly double coordinates and failed printable
volume validation with emitted extrusion around `X395/Y422` on a `270 x 270`
bed. After normalizing both brim/outer-wall and skirt-corner inputs, the same
plate slices successfully, the prime tower stays inside the Qidi printable area,
and G-code preview shows the expected material colors and tower.

Verified on device again on 2026-04-29 with the Bambu Lab P1S profile and two
PLA slots:

* Orca planned `240` Type1 wipe/toolchange layers
* real Orca `WipeTower::generate_new()` reported depth `12.289` and brim
  `3.428`
* MobileSlicer clamped the profile's default tower position from `[165,250]` to
  `[165,210]` so the real tower fits the `256 x 256` P1S printable area
* slice completed with `sliced=true`
* G-code contains `481` `WIPE_TOWER_START` markers and `481`
  `WIPE_TOWER_END` markers
* G-code contains `240` `CP TOOLCHANGE WIPE` markers and `240`
  `CP TOOLCHANGE START` markers
* filament usage is nonzero for both logical materials:
  `12302.02, 11357.53 mm`

Verified on device again on 2026-04-29 with a Bambu-style Type2 tower path,
two Benchy objects, one physical nozzle, and three logical PLA slots with the
objects assigned to slots 1 and 3:

* Orca config emitted three logical filament vectors and `filament_map = 1,1,1`
* object 1 loaded as Orca `extruder = 1`
* object 2 loaded as Orca `extruder = 3`
* Orca tool ordering reported logical tools `[0, 2]`
* `wipe_tower_type = type2`
* slice completed with `sliced=true`
* G-code contains `240` `WIPE_TOWER_START` markers and `240`
  `WIPE_TOWER_END` markers
* G-code contains `240` `CP TOOLCHANGE WIPE` markers and `241`
  `CP TOOLCHANGE START` markers
* G-code contains `243` `T` commands with real alternating `T0` / `T2`
  tool changes
* filament usage is nonzero for the assigned logical materials:
  `13992.15, 0.00, 14241.88 mm`
* Bambu `M73` starts at `M73 P0 R459` and ends at `M73 P100 R0`
* estimated print time is sane: `7h 39m 16s`

The second Android-specific fix was that the Android libslic3r boundary still
stubbed `WipeTower2`. Android now builds Orca's real `GCode/WipeTower2.cpp`, so
Type2 tower planning, tower G-code insertion, wipe/toolchange records, and
per-slot usage accounting come from Orca instead of a MobileSlicer placeholder.

The third fix was removing the incorrect assumption that multi-nozzle printers
never need a prime tower. Orca's `Print::has_wipe_tower()` checks
`enable_prime_tower`, spiral mode, and logical filament count; it does not
disable the tower just because `nozzle_diameter` has more than one entry. For
MobileSlicer, any plate with more than one logical material slot now emits
`enable_prime_tower = 1` and `purge_in_prime_tower = 1` while preserving
`single_extruder_multi_material = 0` for multi-nozzle profiles. This keeps H2D
and other multi-nozzle printers on Orca's real tower/toolchange path instead of
silently dropping the tower.

MobileSlicer also disables Orca's `exclude_object` and `gcode_label_objects` at
the final plate configuration boundary. Orca's Bambu G-code path emits `M624`
label commands from desktop instance label ids. MobileSlicer combines and
reloads plate objects through the Android wrapper, so that metadata is not yet
authoritative enough for label-object export. Leaving it enabled can fail after
successful tower planning with `Unknown label object id!`. The Android Orca
source now respects `exclude_object = 0` before emitting Bambu layer/object
label commands; material assignment and tower generation remain unchanged.

H2D timelapse custom G-code must stay enabled. The H2D Orca profile depends on
`has_timelapse_safe_pos`, `timelapse_pos_x`, and `timelapse_pos_y` in addition to
the common `layer_num`, `layer_z`, `max_layer_z`,
`most_used_physical_extruder_id`, and `curr_physical_extruder_id` placeholders.
MobileSlicer therefore keeps the original profile `time_lapse_gcode` and exposes
the same timelapse-safe-position placeholders through Orca's custom G-code
placeholder definitions instead of clearing or rewriting the profile G-code.

## Filament Usage Accounting

MobileSlicer displays Orca's generated material summary for G-code previews.
For multicolor single-nozzle prints, that summary must include prime/wipe tower
purge material instead of reporting only model extrusion.

Verified on device on 2026-04-29 with a Bambu Lab A1 profile, one physical
nozzle, two logical PLA slots, and 800 Orca tool changes:

* G-code contains `single_extruder_multi_material = 1`
* G-code contains `purge_in_prime_tower = 1`
* G-code contains `prime_tower_width = 35` and `prime_volume = 45`
* G-code contains repeated `;TYPE:Prime tower` sections
* Orca footer reports:
  * `filament used [mm] = 18523.73, 18433.27`
  * `filament used [g] = 55.25, 54.98`
  * `total filament used [g] = 110.23`
  * `total filament change = 800`
* MobileSlicer displays `Filament: 110.23 g`, matching Orca's
  `total filament used [g]`

Parser contract:

* prefer Orca's `total filament used [g]` whenever it exists
* otherwise use Orca's per-tool `filament used [g]`
* if Orca emits separate `filament used for wipe tower [...]` lines without a
  total line, add the wipe-tower value to the per-tool/model value
* only fall back to raw positive-E estimation when Orca does not emit usable
  filament summary comments

This keeps accounting on Orca semantics. Raw positive-E scanning is not a
replacement for Orca's summary because retraction recoveries and toolchange
sequences can inflate simple positive-E totals.

## Prime Tower Placement And Preview

MobileSlicer now treats the prime/wipe tower as Orca print geometry with a real
bed keepout, not as a loose visual accessory.

Current behavior:

* the Prepare auto-arrange path reserves a conservative prime-tower footprint
  for single-nozzle multi-material plates
* the native wrapper clamps `wipe_tower_x` / `wipe_tower_y` to the printable
  area, then checks the estimated tower footprint against transformed model
  instance bounds before `print.apply(...)`
* when the configured tower spot overlaps objects, the wrapper searches the bed
  for a clear tower keepout and rewrites `wipe_tower_x` / `wipe_tower_y` before
  slicing
* if no clear tower keepout fits, slicing fails before G-code export with an
  explicit prime-tower-fit message instead of emitting overlapping tower G-code
* multicolor G-code Preview defaults to Orca `ColorPrint` view mode, so tower
  preview color follows filament/tool color semantics instead of the generic
  `FeatureType` wipe-tower role color
* G-code preview classifies `;TYPE:Prime tower`, `;FEATURE:Prime tower`, and
  wipe-tower markers as Orca/libvgcode `WipeTower` role
* the phone preview suppresses only long, near-zero-flow wipe-tower transition
  strands. These are real Orca travel-to-tower dribble moves, but drawing them
  as thick extrusion creates fan-shaped geometry between the model and tower.
  The emitted G-code remains unchanged; only the mobile preview mesh drops
  `WipeTower` segments longer than 25 mm with less than `0.005 mm3/mm` flow.
  The same rule is used by preview range planning and preview mesh loading so
  automatic exact-preview chunking remains consistent with the selected GCODE
  Preview Performance budget.

This mirrors the Orca model we rely on locally: tower placement is config-driven
through `wipe_tower_x`, `wipe_tower_y`, `prime_tower_width`, brim, and computed
purge depth; Orca includes the tower footprint in print extents and collision
checks through `first_layer_wipe_tower_corners()` / `FakeWipeTower`.

## Verified Single-Object Slot Selection

Verified from MobileSlicer-generated G-code files on 2026-04-29. These files
prove that the current app can slice a single STL object assigned to a
non-first logical filament slot on a one-nozzle printer.

`/home/peanut/Downloads/3DBenchy_50m24s_PETG.gcode`:

* generated by MobileSlicer at `2026-04-29 17:33:56`
* config contains `filament_self_index = 1,2`
* config contains `filament_settings_id = PLA;PETG`
* config contains `filament_type = PLA;PETG`
* config contains `filament_map = 1,1`
* config contains `single_extruder_multi_material = 1`
* G-code selects `T1`, Orca's zero-based tool for logical slot 2
* filament usage is only in the second slot:
  `0.00, 3626.28 mm` and `0.00, 11.08 g`

`/home/peanut/Downloads/3DBenchy_3h55m_PLA.gcode`:

* generated by MobileSlicer at `2026-04-29 17:35:25`
* config contains `filament_self_index = 1,2,3,4`
* config contains `filament_type = PLA;PLA;PLA;PLA`
* config contains `filament_map = 1,1,1,1`
* config contains `single_extruder_multi_material = 1`
* G-code contains `MANUAL_TOOL_CHANGE T3`, Orca's zero-based tool for logical
  slot 4
* filament usage is only in the fourth slot:
  `0.00, 0.00, 0.00, 4072.82 mm` and
  `0.00, 0.00, 0.00, 12.15 g`
* the fourth slot color is `#FFEB3B`; slots 1-3 are configured but unused

Do not replace this path with a new app-owned AMS abstraction. The correct
contract is already visible in emitted G-code: MobileSlicer assigns Orca logical
filament slots, Orca emits zero-based tool ids, and usage accounting lands in
the selected slot.

## Verified Bambu P1S Time Summary

Verified on device over wireless ADB on 2026-04-29 with two Benchy plate
objects, a Bambu Lab P1S profile, and two PLA material slots.

The earlier bad summary came from impossible Orca/Bambu time metadata:
`print_statistics` reported `64563605504` seconds and the generated G-code
contained `M73 P0 R1076060032` plus an estimated time comment of
`747263d 22h 51m 44s`. MobileSlicer now treats those values as untrusted
metadata and rejects any impossible print time before it reaches the UI.

Fresh device proof from `cache/latest-slice-plate_2_objects_PLA.gcode`:

* slice completed with `sliced=true`
* Orca reported sane processor time: `23713` seconds
* header time is `model printing time: 6h 28m 57s; total estimated time:
  6h 35m 13s`
* Bambu `M73` starts at `M73 P0 R395` and ends at `M73 P100 R0`
* filament usage is nonzero for both logical materials: `8998.09, 8966.69 mm`

## Next Step

The next proof target is multiple physical nozzles:

* select/import a printer profile whose `nozzle_diameter` vector has more than
  one physical nozzle/head
* keep `single_extruder_multi_material = false`
* preserve the physical nozzle vectors from the Orca printer profile, including
  H2D-style vector keys such as `nozzle_volume`, `extruder_printable_area`,
  `nozzle_height`, `grab_length`, and hotend/retraction cut vectors
* default to `filament_map_mode = Auto For Flush` with a valid initial
  `filament_map`; add manual slot-to-nozzle mapping only when the UI exposes it
* verify emitted G-code routes each object's extrusion through the expected
  physical tool
* verify `flush_volumes_matrix` is sized as
  `slot_count * slot_count * physical_nozzle_count`

An H2D Pro multi-filament repro on 2026-04-29 surfaced `std::bad_alloc` before
MobileSlicer captured a new G-code artifact. The current cache still contained
the previous successful single-nozzle Bambu run (`nozzle_diameter = 0.4`,
`single_extruder_multi_material = 1`, `filament_map = 1,1`), so the H2D failure
must be treated as the next native multi-nozzle repro to capture after clearing
logcat and slicing again. The native wrapper now logs
`orca_config_vectors_pre_apply` immediately before `print.apply(...)` so a future
H2D failure captures the nozzle vectors, map mode, map, and flush matrix that
Orca received before allocation-heavy print application begins.

## H2D Timelapse Custom G-code Parity

On 2026-04-29 an H2D Pro slice reached G-code export and then failed inside
Orca custom G-code parsing:

`timelapse_gcode Parsing error ... Not a variable name`

The failing vendor template referenced Orca's `timelapse_type` inside
`time_lapse_gcode`, including H2D-style guards such as:

`{if !spiral_mode && !(has_timelapse_safe_pos && timelapse_type == 0) }`

This is not an AMS or material-slot issue. It is custom G-code placeholder
context. MobileSlicer must keep the profile G-code intact and provide the same
runtime placeholders Orca expects, instead of deleting the timelapse template.

Implementation rule:

* `layer_change_gcode`, `timelapse_gcode`, and `machine_end_gcode` receive
  `timelapse_type`
* `timelapse_gcode` also receives `spiral_mode`,
  `has_timelapse_safe_pos`, `timelapse_pos_x`, and `timelapse_pos_y`
* `max_layer_z` must be populated before processing `layer_change_gcode`
* parser tests cover `!variable`, `!(...)`, and the H2D timelapse guard form

This keeps H2D/Bambu/Qidi custom G-code behavior aligned with Orca profile
semantics while preserving exact printer G-code.

## Prusa XL Placeholder Parser Parity

On 2026-04-29 a Prusa XL 5T slice reached export with the correct
multi-nozzle contract:

* five physical nozzles: `nozzle_diameter_size=5`
* five logical filaments mapped one-to-one: `filament_map=1,2,3,4,5`
* multi-nozzle mode: `single_extruder_multi_material=0`
* object/volume extruders set as 1-based Orca logical extruders

The failure was not material routing. It was Prusa XL vendor
`filament_start_gcode`, which uses inline nested placeholders such as:

`M572 S{if nozzle_diameter[0]==0.4}...{elsif ...}...{else}0{endif}`

MobileSlicer's Orca fork now preserves this vendor G-code and fixes the
placeholder parser so `{elsif}`, `{else}`, and `{endif}` terminate an inline
text branch instead of being parsed as ordinary macros. A parser regression test
covers the Prusa XL pressure-advance line. This keeps pressure advance,
heatbreak, and other filament-start commands Orca-authored instead of replacing
them with app-side shortcuts.

Follow-up on the same Prusa XL path found a separate config-boundary bug:
MobileSlicer was still letting `filament_start_gcode` and
`filament_end_gcode` cross the Android wrapper as scalar strings. That is not
Orca parity. Orca stores these settings as `ConfigOptionStrings`, one complete
custom G-code script per logical filament. Scalar/semicolon deserialization can
split vendor scripts at comments or leave a dangling `{endif}` on a later
logical slot.

Implementation rule:

* multi-slot plates emit `filament_start_gcode` and `filament_end_gcode` as
  JSON arrays, one entry per active logical material slot
* each entry comes from the slot filament's resolved Orca filament JSON, falling
  back to the saved `FilamentProfile` custom G-code
* the native wrapper sets those options directly as `ConfigOptionStrings`
  instead of serializing them through semicolon-delimited text
* blank entries are preserved as a single space, matching the existing native
  config convention while keeping vector length aligned with logical slots

This preserves exact Prusa XL, H2D, Bambu, Qidi, and other vendor filament
custom G-code without an app-side rewrite.

## Verified H2D Multi-Nozzle Export

Verified on device over wireless ADB on 2026-04-29 after the timelapse
placeholder fix.

Single-color H2D Pro proof:

* `nozzle_diameter_size=2`, `single_extruder_multi_material=0`
* one logical filament mapped to one physical nozzle: `filament_map=1`
* `prime_tower=0`, as expected for no filament change
* G-code export completed with `print_export_gcode: do_export_done`
* time summary came from Orca's G-code processor: `seconds=6984`

Multi-material H2D Pro proof:

* Orca received two physical nozzles: `nozzle_diameter_size=2`
* MobileSlicer kept logical material slots separate from physical nozzles:
  `filament_map_mode=Manual`, `filament_map=1,2,1`
* flush volumes matched Orca's required shape:
  `flush_volumes_matrix_size=18` for `3 slots * 3 slots * 2 nozzles`
* multi-nozzle mode stayed physical-nozzle native:
  `single_extruder_multi_material=0`
* Orca saw object assignments as 1-based logical extruders:
  `object0=3 volumes=[3] object1=1 volumes=[1]`

## Multi-Nozzle Printable Area Normalization

On 2026-04-29, an Elegoo Neptune 2D dual-nozzle slice failed with
`std::bad_alloc` immediately after Orca logged `before_skirt_brim`. The logged
config showed the real root cause:

* two physical nozzles: `nozzle_diameter_size=2`
* two logical slots mapped to physical nozzles: `filament_map=1,2`
* multi-nozzle mode: `single_extruder_multi_material=0`
* incomplete per-nozzle printable area: `extruder_printable_area_size=1` with
  an empty serialized value

The material/nozzle mapping was correct. The failure was a profile-vector shape
mismatch: the brim/skirt path indexed printable-area data by physical nozzle,
but the printable-area vector did not contain one valid polygon per nozzle.

MobileSlicer now normalizes `Print::get_extruder_printable_polygons()` and
`Print::get_extruder_unprintable_polygons()` so every physical nozzle has a
valid area entry. Missing or empty entries fall back to the full bed polygon,
matching Orca's intended behavior when per-extruder printable area is not
specified. Brim also clamps the resolved physical nozzle index before indexing
the normalized vector.

This is a broad multi-nozzle fix, not an Elegoo-specific special case.
* Orca converted those to zero-based print tools:
  `orca_print_extruders all=[0,2]`
* Orca generated the prime tower: `prime_tower=1`
* wipe tower ordering and toolchange G-code were real:
  layers alternated `tools=[0,2]` / `tools=[2,0]`, emitted `e0` and `e2`
  extrusion groups, and logged `orca_gcode_wipe_toolchange`
* G-code export completed with `print_export_gcode: do_export_done`
* time summary came from Orca's G-code processor: `seconds=24595` and
  `seconds=69048` across the checked H2D runs

This is the intended parity point: MobileSlicer supplies Orca with logical
filament slots, object extruder assignments, physical nozzle vectors,
`filament_map`, and correctly-sized flush matrices. Orca then owns physical
tool routing, wipe ordering, prime tower generation, custom printer G-code, and
G-code summary accounting. Do not reintroduce an app-level AMS subsystem for
this path.
