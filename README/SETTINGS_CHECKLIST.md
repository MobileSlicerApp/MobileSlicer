# Settings Checklist

This file is the accountability checklist for Mobile Slicer's Orca-backed
settings coverage.

This file is the single source of truth for:

* settings coverage
* app-facing settings status
* future advanced-visibility planning

Goal:

* add full `Printer` / `Filament` / `Process` settings coverage before broader
  UI feature work resumes
* keep one tracked list showing what Orca exposes, what Mobile Slicer exposes,
  and what still remains missing

Printer connection status:

* `Printer > Connection` is the profile-owned UI for printer host settings.
* Runtime printer networking, `Test`, `Browse`, upload, post-upload action,
  device status, secure credential storage, and per-host hardware-validation
  gaps are tracked in `README/PRINTER_CONNECTION.md`.
* The print-host controls remain visible without `Advanced slicer controls`
  even though Orca marks several backing keys as `comAdvanced`, because the app
  treats connecting to a printer as a normal printer workflow.

App-facing status rule for this checklist:

* `Config only`
  * the setting is surfaced in the app and reaches the config path, but the app
    should not claim accepted real-device proof yet
* `Config only - Waydroid`
  * the setting is surfaced in the app and reached the config path under
    exploratory Waydroid validation only
  * this is still not accepted real-device proof and must not be collapsed into
    `Device tested`
* `Device tested`
  * the setting has accepted real-device evidence on the Android path
* `Missing`
  * the setting is not yet surfaced in the app
* `Parent surfaced, subset only`
  * the app exposes the parent setting, but only some Orca mode values are
    currently surfaced

Future app visibility rule:

* `Settings > Advanced slicer controls` is a visibility toggle only
  * it must never be treated as a page/category replacement
  * an Orca-advanced setting stays in the same page and option group Orca uses;
    the toggle only decides whether that setting is visible by default
* for any surfaced Orca-backed setting, advanced visibility is determined by
  Orca's own config metadata in
  `vendor/orcaslicer/src/libslic3r/PrintConfig.cpp`
  * `ConfigOptionDef::mode = comAdvanced` => hide by default behind the app's
    advanced toggle
  * `ConfigOptionDef::mode = comSimple` => show by default
* when raw config mode metadata disagrees with Orca's actual surfaced desktop
  UI layout for a current setting, use Orca GUI layout as the final app-facing
  visibility authority
  * source of truth for that override:
    `vendor/orcaslicer/src/slic3r/GUI/Tab.cpp`
  * example: infill controls such as `sparse_infill_density`,
    `fill_multiline`, and `sparse_infill_pattern` appear in Orca's main
    `Strength > Infill` group and must stay visible by default in the app;
    in the current vendored Orca tree these controls do not carry
    `comAdvanced`
* special-case rule for surfaced controls without a normal live Orca mode entry
  in this vendored tree:
  * if the option is commented out, absent from the live config-def path, or
    otherwise not represented as a normal `PrintConfig.cpp` mode-tagged field,
    do not assume `DEFAULT` means simple
  * for those cases, the checklist must mark the intended app behavior
    explicitly and the UI should follow that documented exception
* checklist `[Advanced]` tags are the accountability mirror of Orca mode
  metadata, not a separate source of truth
  * if a surfaced setting's checklist tag and Orca mode disagree, Orca mode
    wins for UI behavior and the checklist must be corrected in the same or
    next touched run
  * if a surfaced setting is a documented special-case exception, the checklist
    tag and the UI should mirror that exception explicitly
* Orca-advanced settings should stay hidden by default
* they should only be shown when `Advanced slicer controls` is enabled
* this checklist marks those settings with:
  * `Advanced`
* generated enforcement lives in:
  * `scripts/generate_orca_setting_metadata.py`
  * `android-app/app/src/main/java/com/mobileslicer/profiles/GeneratedOrcaSettingMetadata.kt`
  * `android-app/app/src/main/java/com/mobileslicer/profiles/ProfileSettingVisibility.kt`
  * `android-app/app/src/test/java/com/mobileslicer/ProfileModelsTest.kt`
    for editor tab/group order and structured dependency-selection regression
    coverage
* process editor implementation guardrail:
  * keep expanded settings out of giant local Compose state/capture lists
  * use `ProcessProfileEditorDraft`, `ProcessProfileEditorOptions`, and
    `ProcessProfileEditorDraft.toProcessProfile(...)` when adding future
    Process settings
  * Printer and Filament now follow the same editor isolation pattern:
    `PrinterProfileEditorDialog.kt`, `PrinterProfileEditorDraft`,
    `PrinterProfileEditorDraft.toPrinterProfile(...)`,
    `FilamentProfileEditorDialog.kt`, `FilamentProfileEditorDraft`, and
    `FilamentProfileEditorDraft.toFilamentProfile(...)`
  * future large setting clusters should add fields to those draft/save-helper
    surfaces instead of reintroducing large local state blocks in composables
* Gradle regenerates Orca metadata before Android builds, and unit tests fail
  when a surfaced profile setting lacks Orca metadata or an explicit documented
  override
* Process profile model guardrail:
  * `ProcessProfile` must not be a giant Kotlin `data class`; Android ART can
    reject the generated default constructor / `copy$default` bytecode once the
    process setting surface grows too large.
  * Process import/storage uses explicit legacy-index-to-field mapping in
    `ProfileModels.kt`. Do not rely on the physical order of the backing value
    list when adding settings.
  * Any Orca `FloatOrPercent` setting that can be omitted by a preset must have
    a valid Orca default in the app model. Example: `internal_bridge_speed`
    defaults to Orca's documented `150%`, not an empty string.
  * Some Orca `FloatOrPercent` defaults are valid as desktop defaults but should
    not be sent as explicit Android native overrides. Example: overhang speed
    buckets default to `0` in Orca, so the Android wrapper omits explicit zero
    buckets and lets Orca keep its own default while still accepting non-zero
    user/imported overrides.
  * Orca flow-ratio settings defined in `PrintConfig.cpp` default to `1` and
    the app model defaults must match that Orca behavior. This includes
    first-layer, wall, top/bottom surface, bridge/internal bridge, sparse/internal
    solid infill, gap fill, support/interface, print, and scarf flow ratios.
    Native export sends the actual stored/imported value; it must not silently
    rewrite user values except for documented Android-wrapper boundary cases.
* Native slice config precedence guardrail:
  * resolved Orca printer, filament, and process JSON is the immutable preset
    baseline and must remain available for unmodeled Orca keys
  * profile editor changes are stored as explicit `orca*OverridesJson` layers
  * after any resolved-Orca parity restoration, single-material boundary
    handling, and config normalization, the explicit override layers must be
    applied again as the final authority
  * this final override pass is what allows every surfaced printer, filament,
    and process edit to win without copying entire resolved presets into custom
    profiles
* Native emission guardrail:
  * when a setting is persisted in `PrinterProfile`, `FilamentProfile`, or
    `ProcessProfile`, it must either be emitted by the relevant native config
    writer or be documented as metadata/app-only
  * current audited writers are `NativeSlicePrinterConfiguration.kt`,
    `NativeSliceFilamentConfiguration.kt`, `NativeSliceFilamentOverrides.kt`,
    and `NativeSliceProcessConfiguration.kt`
  * recent audit gaps that are now covered: printer thumbnail settings and
    process `inner_wall_line_width`, `dont_slow_down_outer_wall`,
    `internal_bridge_fan_speed`, and `support_material_interface_fan_speed`

## Orca UI Parity Contract

This contract is mandatory for every future profile setting run.

Mobile Slicer's `Printer`, `Filament`, and `Process` profile editors must mirror
the vendored Orca UI structure from `vendor/orcaslicer/src/slic3r/GUI/Tab.cpp`.
Do not reorganize settings by implementation convenience, backend ownership, or
whether the app's advanced toggle hides them.

For each newly surfaced setting:

* first find its Orca page in `Tab.cpp`
  * examples: `TabFilament::build()`, `TabFilament::add_filament_overrides_page()`,
    `TabPrinter::build()`, and the process `TabPrint` build section
* then place the Android control in the same top-level page/tab
* then place it in the same high-level option-group order within that page
* then preserve the relative order of surfaced controls inside that option group
* then apply visibility from `PrintConfig.cpp` mode metadata through
  `ProfileEditorSetting.isVisible(...)`
* then update this checklist in the same run

If Mobile Slicer has not implemented a whole Orca page or option group yet,
keep an honest placeholder for the page instead of moving fields to a different
page. A page named `Advanced` in Orca is a real page. It is not the same thing
as the app's `Advanced slicer controls` toggle.

Current Orca-backed top-level editor pages:

* Printer mirrors Orca printer pages currently represented in the app:
  `Basic information`, `Machine G-code`, `Multimaterial`, `Extruder`,
  `Motion ability`, `Notes`
* Printer also has a Mobile Slicer `Connection` tab. That tab is not a
  `TabPrinter::build()` printer-preset page; it mirrors Orca's Physical Printer
  dialog so print-host setup remains reachable as a normal app workflow.
* Filament mirrors `TabFilament::build()`:
  `Filament`, `Cooling`, `Setting Overrides`, `Advanced`, `Multimaterial`,
  `Dependencies`, `Notes`
* Process mirrors Orca process pages currently represented in the app:
  `Quality`, `Strength`, `Speed`, `Support`, `Multimaterial`, `Others`

Current Filament page/group mapping from Orca:

* `Filament`
  * `Basic information`
  * `Flow ratio and Pressure Advance`
  * `Print chamber temperature`
  * `Print temperature`
  * `Bed temperature`
  * `Volumetric speed limitation`
* `Cooling`
  * `Cooling for specific layer`
  * `Part cooling fan`
  * `Auxiliary part cooling fan`
  * `Exhaust fan`
* `Setting Overrides`
  * `Retraction`
  * `Ironing`
* `Advanced`
  * `Filament start G-code`
  * `Filament end G-code`
* `Multimaterial`
  * `Wipe tower parameters`
  * `Multi Filament`
  * `Tool change parameters with single extruder MM printers`
  * `Tool change parameters with multi extruder MM printers`
* `Dependencies`
  * `Compatible printers`
  * `Compatible process profiles`
* `Notes`

Current Printer page/group mapping from Orca plus the app connection extension:

* `Basic information`
  * `Printable space`
  * `Advanced`
  * `Cooling Fan`
  * `Extruder Clearance`
  * `Adaptive bed mesh`
  * `Accessory`
* `Connection`
  * `Print host`
  * Mobile Slicer extension backed by Orca's Physical Printer dialog
* `Machine G-code`
  * `File header`
  * `Machine start G-code`
  * `Machine end G-code`
  * `Printing by object G-code`
  * `Before layer change G-code`
  * `Layer change G-code`
  * `Timelapse G-code`
  * `Clumping Detection G-code`
  * `Change filament G-code`
  * `Change extrusion role G-code`
  * `Pause G-code`
  * `Template Custom G-code`
* `Multimaterial`
  * `Single extruder multi-material setup`
  * `Wipe tower`
  * `Single extruder multi-material parameters`
  * `Advanced`
* `Extruder`
  * `Basic information`
  * `Layer height limits`
  * `Position`
  * `Retraction`
  * `Z-Hop`
  * `Retraction when switching material`
* `Motion ability`
  * `Advanced`
  * `Resonance Avoidance`
  * `Speed limitation`
  * `Acceleration limitation`
  * `Jerk limitation`
* `Notes`
  * `Notes`

When the app surfaces only part of a group, it must still keep that part in the
group's Orca position. Example: `filament_flow_ratio`,
`enable_pressure_advance`, and `pressure_advance` are advanced-visible fields,
but Orca places them in `Filament > Flow ratio and Pressure Advance`, not in
`Filament > Advanced`.

Current Process page/group mapping from Orca:

* `Quality`
  * `Layer height`
    * Surfaced order follows Orca `Tab.cpp`: `layer_height`,
      `initial_layer_print_height`.
  * `Line width`
    * Verified against `vendor/orcaslicer/src/slic3r/GUI/Tab.cpp`:
      `line_width`, `initial_layer_line_width`, `outer_wall_line_width`,
      `inner_wall_line_width`, `top_surface_line_width`,
      `sparse_infill_line_width`, `internal_solid_infill_line_width`,
      `support_line_width`.
    * These are advanced-visible Orca `FloatOrPercent` settings whose `0`
      value means slicer auto/default width over the active nozzle diameter.
      Android imports the resolved Orca values and fallback/default profiles
      now use `0` instead of fixed `0.42`/`0.45`/`0.5` widths.
  * `Seam`
    * Surfaced scarf controls follow Orca `Tab.cpp` order; `scarf_joint_flow_ratio`
      stays after `seam_slope_steps`.
    * `has_scarf_joint_seam` remains a config/import/export value but is not
      shown in the process editor because it is not surfaced in the vendored
      Orca process `Tab.cpp`.
  * `Precision`
    * Surfaced order follows Orca `Tab.cpp` for implemented controls:
      `resolution`, `enable_arc_fitting`, X-Y compensation, elephant-foot
      compensation, `elefant_foot_compensation_layers`, `precise_outer_wall`,
      `precise_z_height`, then polyhole controls.
    * `adaptive_layer_height` and `apply_top_surface_compensation` remain
      config/import/export values but are not shown in the process editor
      because they are not surfaced in the vendored Orca process `Tab.cpp`.
  * `Ironing`
  * `Wall generator`
  * `Walls and surfaces`
    * Surfaced advanced flow-ratio controls are kept before the one-wall
      controls, matching Orca's `wall_sequence`, surface flow ratios,
      one-wall controls, threshold, and avoid-crossing-walls order.
    * Newly first-class wired controls in this group:
      `is_infill_first`, `wall_direction`, `print_flow_ratio`,
      `set_other_flow_ratios`, `internal_solid_infill_flow_ratio`, and
      `small_area_infill_flow_compensation`.
  * `Bridging`
    * Surfaced order follows Orca `Tab.cpp`: `bridge_flow`,
      `internal_bridge_flow`, bridge densities, thick bridges, internal bridge
      filtering, counterbore hole bridging.
    * Newly first-class wired controls in this group:
      `thick_internal_bridges` and `enable_extra_bridge_layer`.
  * `Overhangs`
    * Newly first-class wired controls in this group:
      `make_overhang_printable`, `make_overhang_printable_angle`,
      `make_overhang_printable_hole_size`, `overhang_reverse`,
      `overhang_reverse_internal_only`, and `overhang_reverse_threshold`.
* `Strength`
  * `Walls`
    * Surfaced order follows Orca `Tab.cpp`: `wall_loops`,
      `alternate_extra_wall`, then advanced-visible `detect_thin_wall`.
  * `Top/bottom shells`
  * `Infill`
    * Surfaced order follows Orca `Tab.cpp`: sparse infill density, fill
      multiline, sparse infill pattern, advanced sparse direction/rotate,
      skin/skeleton density/depth/line-width controls, symmetric Y axis,
      infill shift, lateral lattice angles, infill overhang angle, anchors,
      internal solid infill pattern, solid direction/rotate, gap fill target,
      tiny gap filter, and infill wall overlap.
    * Newly first-class wired advanced controls in this group:
      `skin_infill_density`, `skeleton_infill_density`, `infill_lock_depth`,
      `skin_infill_depth`, `skin_infill_line_width`,
      `skeleton_infill_line_width`, `symmetric_infill_y_axis`,
      `infill_shift_step`, `infill_overhang_angle`, `gap_fill_target`, and
      `filter_out_gap_fill`.
  * `Advanced`
    * Surfaced order follows Orca `Tab.cpp`: align infill direction to model,
      extra solid infills, bridge/internal bridge angle, minimum sparse infill
      area, infill combination, max layer height, detect narrow internal solid
      infill, ensure vertical shell thickness.
* `Speed`
  * `First layer speed`
  * `Other layers speed`
    * Surfaced order follows Orca `Tab.cpp`; gap infill appears before ironing.
  * `Overhang speed`
  * `Travel speed`
  * `Acceleration`
  * `Jerk(XY)`
  * `Advanced`
* `Support`
  * `Support`
  * `Raft`
  * `Support filament`
  * `Support ironing`
  * `Advanced`
  * `Tree supports`
    * Newly first-class wired controls in this group follow Orca
      `vendor/orcaslicer/src/slic3r/GUI/Tab.cpp` order:
      `tree_support_tip_diameter`, `tree_support_branch_distance`,
      `tree_support_branch_distance_organic`, `tree_support_top_rate`,
      `tree_support_branch_diameter`,
      `tree_support_branch_diameter_organic`,
      `tree_support_branch_diameter_angle`, `tree_support_branch_angle`,
      `tree_support_branch_angle_organic`, `tree_support_angle_slow`,
      `tree_support_auto_brim`, and `tree_support_brim_width`.
* `Multimaterial`
  * `Prime tower`
  * `Filament for Features`
  * `Ooze prevention`
  * `Flush options`
  * `Advanced`
* `Others`
  * `Skirt`
  * `Brim`
  * `Special mode`
  * `Fuzzy Skin`
  * `G-code output`
  * `Post-processing Scripts`
  * `Notes`

The current Android Process editor keeps surfaced controls inside these Orca
groups and in this order. Empty Orca groups are documented here instead of
rendered as blank mobile sections.

Filament `Dependencies` note:

* Orca renders `compatible_printers` and `compatible_prints` through an
  `All` checkbox plus `Set...` picker widget, not as raw preset-list text.
* Mobile Slicer now mirrors that shape with an `All` toggle plus per-profile
  picker rows built from known printer/process presets, while preserving
  imported raw selections for round-trip compatibility.
* `compatible_printers_condition` and `compatible_prints_condition` remain
  visible under their Orca groups because Orca exposes them as text conditions.

Current visibility verification:

* verified on 2026-04-27 against the vendored Orca checkout used by this repo
  and the supplied Orca Process screenshots:
  `quality1.png`, `quality 2.png`, `quality 3.png`, `strength 1.png`,
  `strength 2.png`, `speed 1.png`, `speed 2.png`, `support 1.png`,
  `support 2.png`, `multimaterial.png`, `other 1.png`, and `other 2.png`
* all current profile-editor advanced gates in
  `android-app/app/src/main/java/com/mobileslicer/profiles/ProfilesScreen.kt`
  route through `ProfileEditorSetting.isVisible(...)`
* filament settings follow Orca's top-level filament pages:
  `Filament`, `Cooling`, `Setting Overrides`, `Advanced`, `Multimaterial`,
  `Dependencies`, and `Notes`
  * currently surfaced flow / Pressure Advance, temperature, and volumetric
    speed controls stay on Orca's `Filament` page because that is where
    `TabFilament::build()` places those optgroups
  * Orca's `Advanced` filament page is retained as its own UI category for
    filament start/end G-code; those fields are surfaced there and remain
    hidden until `Advanced slicer controls` is enabled
* `android-app/app/src/test/java/com/mobileslicer/ProfileModelsTest.kt`
  verifies:
  * bed temperature, cooling baseline, and core infill controls stay visible
    when `Advanced slicer controls` is off
  * printer bed dimensions, nozzle diameter, max volumetric speed,
    speed/acceleration detail controls, process ironing controls, and fuzzy-skin detail controls are
    hidden until `Advanced slicer controls` is on
  * every surfaced profile visibility declaration has generated Orca metadata
    or an explicit documented override
* verification commands:
  * `./gradlew :app:testDebugUnitTest`
  * `./gradlew :app:assembleDebug`
* current verification note:
  * `./gradlew :app:testDebugUnitTest` currently fails in
    `AutomationConfigResolverTest` because
    `ProfileStoreRepository.defaultPrinterProfiles()` returns an empty list
    while the tests still expect a built-in default printer fixture
  * `./gradlew :app:assembleDebug` succeeds for the APK hash below
  * after profile-editor changes, run `scripts/verify_android.sh profile-ui`
    to build, install, launch, check the Android crash buffer, and verify the
    app process remains alive
* latest local debug APK:
  * `android-app/app/build/outputs/apk/debug/app-debug.apk`
  * SHA-256:
    `a55a45d3ee27a0a3909e18e28c30be1d05147073cf078cc1deb1a45386fefa01`
  * on-device launch proof:
    `scripts/verify_android.sh profile-ui RFCYA01ANVE` installed and
    cold-launched the app, crash log buffer clean, `pidof com.mobileslicer`
    returned running process `21541`
* APK files are intentionally ignored by `.gitignore`; keep binary release
  artifacts out of source unless a dedicated release-artifact workflow is added

Repo-internal proof docs may still keep finer language such as
`Start-sequence only`, `Fan-command only`, `Stronger-fixture proven`, or
`Config only - Waydroid`, but the app-facing accountability surface should use
only the tracked statuses defined above.

Checklist line format:

* `setting_key` -> `status`
* `setting_key [Advanced]` -> `status`
* for mode-dependent selectors such as infill/surface patterns, include the
  adjacent Orca sub-controls in this same checklist instead of treating the
  selector as the whole feature
* when Orca exposes enumerated mode values, track the parent key and the
  individual surfaced/missing mode values separately

Source basis for this checklist:

* current Mobile Slicer app profile/config surface in
  `android-app/app/src/main/java/com/mobileslicer/ProfileModels.kt`
* Orca process template:
  * `vendor/orcaslicer/resources/profiles_template/Template/process/process template.json`
* Orca filament template:
  * `vendor/orcaslicer/resources/profiles_template/Template/filament/filament_pla_template.json`
* Orca printer/machine setting superset derived from:
  * `vendor/orcaslicer/src/libslic3r/PrintConfig.hpp`
  * `vendor/orcaslicer/resources/profiles/*/machine/*.json`

Excluded from this checklist:

* preset metadata only:
  * `name`
  * `type`
  * `instantiation`
  * `inherits`
  * `from`
  * `settings_id`
  * `printer_settings_id`
  * `print_settings_id`
  * `filament_settings_id`
  * `desciption`
  * `model_id`
  * `renamed_from`
  * `setting_id`
  * `default_filament_profile`
  * `default_materials`
  * `default_nozzle_volume_type` unless paired with surfaced
    `nozzle_volume_type` compatibility export
  * `default_print_profile`
  * `best_object_pos`

Audit note:

* this checklist covers current known Orca-derived slicer settings and machine
  controls relevant to Mobile Slicer product coverage
* it intentionally excludes preset inheritance/metadata keys and similar preset
  bookkeeping values
* it has been audited against the current vendored Orca config surface used by
  this repo, and advanced visibility is sourced from Orca's
  `PrintConfig.cpp` mode metadata while this checklist mirrors that state for
  app accountability
* if a missing Orca key is later found to be a real user-facing slicer control,
  add it here instead of tracking it informally
* future Orca profile import/export should be implemented only after much
  broader native settings coverage exists; until then, keep new native settings
  documented in a translation-friendly way so each field's Orca key or lack of
  Orca equivalent stays explicit
* when a native control intentionally collapses multiple Orca fields or stores
  a mobile-specific derived form, document that collapse rule alongside the
  setting instead of leaving it implicit for future mapper work

## Current App Coverage

### Printer

* [x] `printable_area [Advanced]` / `printable_height` via app `bed width` / `bed depth` / `max height` -> `Device tested`
* [x] `nozzle_diameter [Advanced]` -> `Device tested`
* [x] `file_start_gcode [Advanced]` -> `Config only`
* [x] `machine_start_gcode [Advanced]` -> `Config only`
* [x] `machine_end_gcode [Advanced]` -> `Config only`
* [x] `printing_by_object_gcode [Advanced]` -> `Config only`
* [x] `before_layer_change_gcode [Advanced]` -> `Config only`
* [x] `layer_change_gcode [Advanced]` -> `Config only`
* [x] `time_lapse_gcode [Advanced]` -> `Config only`
* [x] `wrapping_detection_gcode [Advanced]` -> `Config only`
* [x] `change_filament_gcode [Advanced]` -> `Config only`
* [x] `change_extrusion_role_gcode [Advanced]` -> `Config only`
* [x] `machine_pause_gcode [Advanced]` -> `Config only`
* [x] `template_custom_gcode [Advanced]` -> `Config only`

### Filament

* [x] `filament_type` -> `Config only`
* [x] `filament_diameter` moved to Orca `Filament > Basic information` path -> `Config only`
* [x] `nozzle_temperature_initial_layer` -> `Device tested`
* [x] `nozzle_temperature` -> `Device tested`
* [x] `hot_plate_temp_initial_layer` / `bed_temperature_initial_layer` app compatibility path -> `Device tested`
* [x] `hot_plate_temp` / `bed_temperature` app compatibility path -> `Device tested`
* [x] `fan_min_speed` / `fan_max_speed` through app `cooling baseline` -> `Device tested`
* [x] `close_fan_the_first_x_layers` -> `Device tested`
* [x] `filament_flow_ratio [Advanced]` -> `Config only - Waydroid`
* [x] `filament_retraction_length [Advanced]` -> `Config only - Waydroid`
* [x] `filament_retraction_speed [Advanced]` -> `Config only - Waydroid`
* [x] `filament_deretraction_speed [Advanced]` -> `Config only - Waydroid`
* [x] `enable_pressure_advance [Advanced]` -> `Config only - Waydroid`
* [x] `pressure_advance [Advanced]` -> `Config only - Waydroid`
* [x] `filament_max_volumetric_speed [Advanced]` -> `Device tested`
* [x] `filament_adaptive_volumetric_speed [Advanced]` -> `Config only - Waydroid`
* [x] `volumetric_speed_coefficients [Advanced]` -> `Config only - Waydroid`
* [x] `filament_start_gcode [Advanced]` -> `Config only - Waydroid; UI preserves raw text and native wrapper maps empty value to Orca's single-space default`
* [x] `filament_end_gcode [Advanced]` -> `Config only - Waydroid; UI preserves raw text and native wrapper maps empty value to Orca's single-space default`

### Process

* [x] `initial_layer_print_height` / `first_layer_height` -> `Device tested`
* [x] `layer_height` -> `Device tested`
* [x] `initial_layer_speed [Advanced]` / `first_layer_print_speed` -> `Device tested`
* [x] `initial_layer_infill_speed [Advanced]` -> `Device tested`
* [x] `initial_layer_travel_speed [Advanced]` -> `Device tested`
* [x] `slow_down_layers [Advanced]` -> `Device tested`
* [x] `outer_wall_speed [Advanced]` -> `Device tested`
* [x] `inner_wall_speed [Advanced]` -> `Device tested`
* [x] `top_surface_speed [Advanced]` -> `Device tested`
* [x] `travel_speed [Advanced]` -> `Device tested`
* [x] `outer_wall_acceleration [Advanced]` -> `Device tested`
* [x] `inner_wall_acceleration [Advanced]` -> `Device tested`
* [x] `top_surface_acceleration [Advanced]` -> `Device tested`
* [x] `sparse_infill_acceleration [Advanced]` -> `Device tested`
* [x] `bridge_speed [Advanced]` -> `Device tested`
* [x] `small_perimeter_speed [Advanced]` -> `Device tested`
* [x] `small_perimeter_threshold [Advanced]` -> `Device tested`
* [x] `top_shell_layers` -> `Device tested`
* [x] `bottom_shell_layers` -> `Device tested`
* [x] `seam_position` -> `Device tested`
* [x] `seam_gap [Advanced]` -> `Config only`
* [x] `seam_slope_type [Advanced]` -> `Config only`
* [x] `seam_slope_conditional [Advanced]` -> `Config only`
* [x] `scarf_angle_threshold [Advanced]` -> `Config only`
* [x] `scarf_overhang_threshold [Advanced]` -> `Config only`
* [x] `scarf_joint_speed [Advanced]` -> `Config only`
* [x] `scarf_joint_flow_ratio [Advanced]` -> `Config only`
* [x] `seam_slope_start_height [Advanced]` -> `Config only`
* [x] `seam_slope_entire_loop [Advanced]` -> `Config only`
* [x] `seam_slope_min_length [Advanced]` -> `Config only`
* [x] `seam_slope_steps [Advanced]` -> `Config only`
* [x] `seam_slope_inner_walls [Advanced]` -> `Config only`
* [x] `has_scarf_joint_seam [Advanced]` -> `Config only`
* [x] `counterbore_hole_bridging [Advanced]` -> `Config only`
* [x] `precise_outer_wall` -> `Device tested`
* [x] `only_one_wall_first_layer` Orca `Quality > Walls and surfaces` -> `Config only`
* [x] `only_one_wall_top` -> `Device tested`
* [x] `wall_sequence [Advanced]` -> `Config only`
* [x] `enable_arc_fitting [Advanced]` -> `Config only`
* [x] `reduce_crossing_wall [Advanced]` -> `Config only`
* [x] `max_travel_detour_distance [Advanced]` -> `Config only`
* [x] `hole_to_polyhole [Advanced]` -> `Config only`
* [x] `hole_to_polyhole_threshold [Advanced]` -> `Config only`
* [x] `hole_to_polyhole_twisted [Advanced]` -> `Config only`
* [x] `top_surface_pattern` -> `Device tested`
* [x] `wall_loops` -> `Device tested`
* [x] `alternate_extra_wall [Advanced]` -> `Config only`
* [x] `extra_solid_infills [Advanced]` -> `Config only`
* [x] `sparse_infill_density` -> `Device tested`
* [x] `sparse_infill_pattern` -> `Device tested`
* [x] `skirt_loops` -> `Device tested`
* [x] `skirt_type [Advanced]` -> `Config only`
* [x] `skirt_distance [Advanced]` -> `Config only`
* [x] `skirt_height` -> `Config only`
* [x] `brim_width` -> `Device tested`
* [x] `brim_type` -> `Config only`
* [x] `brim_object_gap [Advanced]` -> `Config only`
* [x] `brim_ears [Advanced]` -> `Config only`
* [x] `brim_ears_detection_length [Advanced]` -> `Config only`
* [x] `brim_ears_max_angle [Advanced]` -> `Config only`
* [x] `draft_shield [Advanced]` -> `Config only`
* [x] `print_sequence` -> `Config only`
* [x] `spiral_mode` -> `Config only`
* [x] `reduce_infill_retraction [Advanced]` -> `Config only`
* [x] `filename_format [Advanced]` -> `Config only`
* [x] `support_filament` -> `Config only`
* [x] `support_interface_filament` -> `Config only`
* [x] `raft_layers [Advanced]` -> `Config only`
* [x] `raft_contact_distance [Advanced]` -> `Config only`
* [x] `raft_expansion [Advanced]` -> `Config only`
* [x] `raft_first_layer_density [Advanced]` -> `Config only`
* [x] `raft_first_layer_expansion [Advanced]` -> `Config only`
* [x] `support_ironing_pattern [Advanced]` -> `Config only`
* [x] `independent_support_layer_height [Advanced]` -> `Config only`
* [x] `tree_support_branch_angle [Advanced]` -> `Config only`
* [x] `tree_support_branch_diameter [Advanced]` -> `Config only`
* [x] `tree_support_wall_count [Advanced]` -> `Config only`
* [x] `tree_support_tip_diameter [Advanced]` Orca `Support > Tree supports`, `comAdvanced` -> `Config only`
* [x] `tree_support_branch_distance [Advanced]` Orca `Support > Tree supports`, `comAdvanced` -> `Config only`
* [x] `tree_support_branch_distance_organic [Advanced]` Orca `Support > Tree supports`, `comAdvanced` -> `Config only`
* [x] `tree_support_top_rate [Advanced]` Orca `Support > Tree supports` branch density, `comAdvanced` -> `Config only`
* [x] `tree_support_branch_diameter_organic [Advanced]` Orca `Support > Tree supports`, `comAdvanced` -> `Config only`
* [x] `tree_support_branch_diameter_angle [Advanced]` Orca `Support > Tree supports`, `comAdvanced` -> `Config only`
* [x] `tree_support_branch_angle_organic [Advanced]` Orca `Support > Tree supports`, `comAdvanced` -> `Config only`
* [x] `tree_support_angle_slow [Advanced]` Orca `Support > Tree supports` preferred branch angle, `comAdvanced` -> `Config only`
* [x] `tree_support_auto_brim [Advanced]` Orca `Support > Tree supports` auto brim width -> `Config only`
* [x] `tree_support_brim_width [Advanced]` Orca `Support > Tree supports` -> `Config only`
* [x] `fuzzy_skin` -> `Config only`
* [x] `fuzzy_skin_thickness` -> `Config only`
* [x] `fuzzy_skin_point_distance` -> `Config only`
* [x] `fuzzy_skin_first_layer` -> `Config only`
* [x] `fuzzy_skin_mode` -> `Config only`
* [x] `fuzzy_skin_noise_type` -> `Config only`
* [x] `fuzzy_skin_scale [Advanced]` -> `Config only`
* [x] `fuzzy_skin_octaves [Advanced]` -> `Config only`
* [x] `fuzzy_skin_persistence [Advanced]` -> `Config only`
* [x] `ironing_type [Advanced]` -> `Config only`
* [x] `ironing_pattern [Advanced]` -> `Config only`
* [x] `ironing_flow [Advanced]` -> `Config only`
* [x] `ironing_spacing [Advanced]` -> `Config only`
* [x] `ironing_inset [Advanced]` -> `Config only`
* [x] `ironing_angle [Advanced]` -> `Config only`
* [x] `ironing_angle_fixed [Advanced]` -> `Config only`
* [x] `ironing_speed [Advanced]` -> `Config only`

## Printer Checklist

### Build Volume / Bed / Geometry

* [x] `printable_area [Advanced]` -> `Device tested`
* [x] `printable_height` -> `Device tested`
* [x] `extruder_printable_area [Advanced]` -> `Config only`
* [x] `extruder_printable_height [Advanced]` -> `Config only`
* [x] `bed_exclude_area [Advanced]` -> `Config only`
* [x] `best_object_pos [Advanced]` -> `Config only`
* [x] `bed_custom_model [Advanced]` Orca `Printer > Basic information > Printable space` bed-shape dialog custom model path -> `Config only`
* [x] `bed_custom_texture [Advanced]` Orca `Printer > Basic information > Printable space` bed-shape dialog custom texture path -> `Config only`
* [x] `head_wrap_detect_zone [Advanced]` Orca `comDevelop` machine compatibility area used by `GCode.cpp` to populate `in_head_wrap_detect_zone`; preserved in import/export but hidden from normal mobile Printer UI because current Orca screenshots do not show the developer row -> `Config only`
* [x] `bed_model [Advanced]` Orca printer-model metadata (`Preset.hpp` key `BBL_JSON_KEY_BED_MODEL`, loaded by `PresetBundle.cpp`, used by bed rendering/G-code preview through `PresetUtils::system_printer_bed_model`) -> `Config only`
* [x] `bed_shape [Advanced]` Orca/vendor profile metadata that triggers bed-shape refresh in `Plater.cpp`; app preserves it with printable-space metadata while `printable_area` remains the live geometry field -> `Config only`
* [x] `bed_texture [Advanced]` Orca printer-model metadata (`Preset.hpp` key `BBL_JSON_KEY_BED_TEXTURE`, loaded by `PresetBundle.cpp`, used by bed rendering/G-code preview through `PresetUtils::system_printer_bed_texture`) -> `Config only`
* [x] `bed_texture_area [Advanced]` Orca vendor machine-profile metadata seen in Elegoo machine JSON; app preserves it with bed visual metadata -> `Config only`
* [x] `default_bed_type [Advanced]` Orca machine-profile default; `PrintConfig.cpp` says this option is not shown in UI, and `Preset::get_default_bed_type()` reads the numeric string bed type; preserved in import/export as config compatibility -> `Config only`
* [x] `bottom_texture_rect [Advanced]` Orca printer-model metadata (`Preset.hpp`/`PresetBundle.cpp`) read by `Plater::get_bed_texture_maps()` and parsed by `PartPlate.cpp` for split bed texture placement -> `Config only`
* [x] `bottom_texture_end_name [Advanced]` Orca printer-model metadata (`Preset.hpp`/`PresetBundle.cpp`) read by `Plater::get_bed_texture_maps()` and used by `PartPlate.cpp` to select bottom bed texture assets -> `Config only`
* [x] `image_bed_type [Advanced]` Orca printer-model metadata (`Preset.hpp`/`PresetBundle.cpp`) used by `Sidebar::reset_bed_type_combox_choices()` for bed-image variant state -> `Config only`
* [x] `use_double_extruder_default_texture [Advanced]` Orca printer-model metadata (`Preset.hpp`/`PresetBundle.cpp`) read by `Plater::get_bed_texture_maps()` and used by `PartPlate.cpp` to choose double-extruder bed texture assets -> `Config only`
* [x] `use_rect_grid [Advanced]` Orca vendor machine-profile metadata seen in Bambu machine JSON; app preserves it with bed visual metadata -> `Config only`
* [x] `preferred_orientation [Advanced]` -> `Config only`
* [x] `z_offset [Advanced]` -> `Config only`

### Nozzle / Extruder / Tooling

* [x] `nozzle_diameter [Advanced]` -> `Device tested`
* [x] `nozzle_type [Advanced]` -> `Config only`
* [x] `nozzle_volume [Advanced]` -> `Config only`
* [x] `nozzle_volume_type [Advanced]` current Orca Extruder UI comments this normal row out in `Tab.cpp`; Orca still defines `Standard` / `High Flow` in `PrintConfig.cpp` and updates it through printer/extruder variant flows; preserved in import/export as compatibility metadata and exported through matching `default_nozzle_volume_type` -> `Config only`
* nozzle-volume-type modes:
  * [x] `nozzle_volume_type=Standard` -> `Config only`
  * [x] `nozzle_volume_type=High Flow` -> `Config only`
* [x] `nozzle_hrc` Orca `comDevelop`; preserved in import/export but hidden from normal mobile Printer UI because current Orca screenshots do not show the developer row -> `Config only`
* [x] `nozzle_height [Advanced]` Orca `comDevelop`, PrintConfig-defined hotend geometry compatibility field; preserved in import/export but hidden from normal mobile Printer UI -> `Config only`
* [x] `extruders_count [Advanced]` Orca `Printer > Multimaterial > Single extruder multi-material setup` `Extruders` row -> `Config only`
* [x] `extruder_type [Advanced]` Orca `comAdvanced`, PrintConfig-defined extruder calibration compatibility selector; current vendored `Tab.cpp` does not surface a normal row, preserved in import/export only -> `Config only`
* extruder-type modes:
  * [x] `extruder_type=Direct Drive` -> `Config only`
  * [x] `extruder_type=Bowden` -> `Config only`
* [x] `extruder_variant_list [Advanced]` Orca per-extruder variant metadata used by `extend_extruder_variant()`; not a normal live row, preserved in import/export only -> `Config only`
* [x] `printer_extruder_id [Advanced]` Orca per-extruder variant ID metadata updated from `extruder_variant_list`; not a normal live row, preserved in import/export only -> `Config only`
* [x] `printer_extruder_variant [Advanced]` Orca per-extruder variant metadata updated from `extruder_variant_list`; not a normal live row, preserved in import/export only -> `Config only`
* [x] `master_extruder_id [Advanced]` Orca default extruder metadata used by filament grouping/tool ordering; not a normal live row, preserved in import/export only -> `Config only`
* [x] `physical_extruder_map [Advanced]` Orca `comDevelop` logical-to-physical extruder map; not a normal live row, preserved in import/export only -> `Config only`
* [x] `extruder_ams_count [Advanced]` Orca per-extruder AMS count metadata parsed by `get_extruder_ams_count()`; not a normal live row, preserved in import/export only -> `Config only`
* [x] `extruder_colour [Advanced]` Orca `comAdvanced`, PrintConfig-defined visual-help color field; current vendored `Tab.cpp` keeps the preview row disabled under `#if 0`, preserved in import/export only -> `Config only`
* [x] `extruder_offset [Advanced]` -> `Config only`
* [x] `extruder_max_nozzle_count [Advanced]` Orca vendor machine-profile metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `extruder_clearance_height_to_rod [Advanced]` -> `Config only`
* [x] `extruder_clearance_height_to_lid [Advanced]` -> `Config only`
* [x] `extruder_clearance_radius [Advanced]` -> `Config only`
* [x] `extruder_clearance_dist_to_rod [Advanced]` Orca vendor machine-profile metadata seen in Qidi/BBL machine JSON; preserved in import/export but hidden from normal mobile Printer UI because current Orca screenshots do not show the row -> `Config only`
* [x] `extruder_clearance_max_radius [Advanced]` legacy/import alias remapped by Orca `PrintConfig.cpp` to `extruder_clearance_radius`; app exports/imports it through the existing `Printer > Basic information > Extruder Clearance` radius control -> `Config only`
* [x] `single_extruder_multi_material [Advanced]` -> `Config only`
* [x] `purge_in_prime_tower [Advanced]` -> `Config only`

### Machine Motion / Firmware / Limits

* [x] `gcode_flavor [Advanced]` -> `Config only`
* gcode flavor modes:
  * [x] `gcode_flavor=marlin` -> `Config only`
  * [x] `gcode_flavor=marlin2` -> `Config only`
  * [x] `gcode_flavor=reprapfirmware` -> `Config only`
  * [x] `gcode_flavor=klipper` -> `Config only`
  * [x] `gcode_flavor=reprap` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
  * [x] `gcode_flavor=repetier` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
  * [x] `gcode_flavor=teacup` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
  * [x] `gcode_flavor=makerware` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
  * [x] `gcode_flavor=sailfish` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
  * [x] `gcode_flavor=smoothie` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
  * [x] `gcode_flavor=mach3` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
  * [x] `gcode_flavor=machinekit` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
* [x] `gcode_flavor=no-extrusion` verified unsupported: legacy/commented-out Orca enum value in current `PrintConfig.cpp`; app does not expose invalid UI choice -> `Config only`
* [x] `bed_temperature_formula [Advanced]` -> `Config only`
* bed-temperature-formula modes:
  * [x] `bed_temperature_formula=by_first_filament` -> `Config only`
  * [x] `bed_temperature_formula=by_highest_temp` -> `Config only`
* [x] `emit_machine_limits_to_gcode [Advanced]` -> `Config only`
* [x] `fan_kickstart [Advanced]` -> `Config only`
* [x] `fan_speedup_overhangs [Advanced]` -> `Config only`
* [x] `fan_speedup_time [Advanced]` -> `Config only`
* [x] `machine_max_speed_x` -> `Config only`
* [x] `machine_max_speed_y` -> `Config only`
* [x] `machine_max_speed_z` -> `Config only`
* [x] `machine_max_speed_e` -> `Config only`
* [x] `machine_max_acceleration_x` -> `Config only`
* [x] `machine_max_acceleration_y` -> `Config only`
* [x] `machine_max_acceleration_z` -> `Config only`
* [x] `machine_max_acceleration_e` -> `Config only`
* [x] `machine_max_acceleration_extruding` -> `Config only`
* [x] `machine_max_acceleration_retracting` -> `Config only`
* [x] `machine_max_acceleration_travel [Advanced]` -> `Config only`
* [x] `machine_max_jerk_x` -> `Config only`
* [x] `machine_max_jerk_y` -> `Config only`
* [x] `machine_max_jerk_z` -> `Config only`
* [x] `machine_max_jerk_e` -> `Config only`
* [x] `machine_max_junction_deviation [Advanced]` -> `Config only`
* [x] `machine_min_travel_rate [Advanced]` Orca `comDevelop`, PrintConfig-defined M205 T minimum-feedrate field; current vendored `Tab.cpp` comments the Motion ability row out, preserved in import/export only -> `Config only`
* [x] `machine_min_extruding_rate [Advanced]` Orca `comDevelop`, PrintConfig-defined M205 S minimum-feedrate field; current vendored `Tab.cpp` comments the Motion ability row out, preserved in import/export only -> `Config only`
* [x] `resonance_avoidance [Advanced]` -> `Config only`
* [x] `min_resonance_avoidance_speed [Advanced]` -> `Config only`
* [x] `max_resonance_avoidance_speed [Advanced]` -> `Config only`
* [x] `silent_mode [Advanced]` Orca `comDevelop`, PrintConfig-defined "Supports silent mode" flag; Orca uses it in `Tab.cpp` to expose dual normal/silent Motion ability machine-limit columns, preserved in import/export only while Android edits the normal machine-limit column -> `Config only`
* [x] `use_relative_e_distances [Advanced]` -> `Config only`
* [x] `use_firmware_retraction [Advanced]` -> `Config only`

### Retraction / Z-Hop / Travel Behavior

* [x] `retraction_length` -> `Config only`
* [x] `retraction_speed [Advanced]` -> `Config only`
* [x] `deretraction_speed [Advanced]` -> `Config only`
* [x] `retraction_minimum_travel [Advanced]` -> `Config only`
* [x] `retract_before_wipe [Advanced]` -> `Config only`
* [x] `retract_restart_extra [Advanced]` -> `Config only`
* [x] `retract_when_changing_layer [Advanced]` -> `Config only`
* [x] `retract_length_toolchange [Advanced]` -> `Config only`
* [x] `retract_restart_extra_toolchange [Advanced]` -> `Config only`
* [x] `enable_long_retraction_when_cut [Advanced]` Orca `comDevelop` machine gate for long-retraction-when-cut beta controls; preserved in import/export only because current Orca screenshots do not show the developer row -> `Config only`
* [x] `retraction_distances_when_cut` Orca `comDevelop`; preserved in import/export only because current Orca screenshots do not show the developer row -> `Config only`
* [x] `long_retractions_when_cut` Orca `comDevelop`; preserved in import/export only because current Orca screenshots do not show the developer row -> `Config only`
* [x] `retract_on_top_layer [Advanced]` legacy/import alias absent from current live Orca printer UI; preserved in import/export only -> `Config only`
* [x] `z_hop` -> `Config only`
* [x] `z_hop_types [Advanced]` -> `Config only`
* z-hop-type modes:
  * [x] `z_hop_types=Auto Lift` -> `Config only`
  * [x] `z_hop_types=Normal Lift` -> `Config only`
  * [x] `z_hop_types=Slope Lift` -> `Config only`
  * [x] `z_hop_types=Spiral Lift` -> `Config only`
* [x] `z_hop_when_prime [Advanced]` legacy/import alias absent from current live Orca printer UI; preserved in import/export only -> `Config only`
* [x] `travel_slope [Advanced]` -> `Config only`
* [x] `retract_lift_above [Advanced]` -> `Config only`
* [x] `retract_lift_below [Advanced]` -> `Config only`
* [x] `retract_lift_enforce [Advanced]` -> `Config only`
* [x] `wipe [Advanced]` -> `Config only`
* [x] `wipe_distance [Advanced]` -> `Config only`

### Machine / Printer G-code

* [x] `file_start_gcode [Advanced]` -> `Config only`
* [x] `machine_start_gcode [Advanced]` -> `Config only`
* [x] `machine_end_gcode [Advanced]` -> `Config only`
* [x] `printing_by_object_gcode [Advanced]` -> `Config only`
* [x] `before_layer_change_gcode [Advanced]` -> `Config only`
* [x] `layer_change_gcode [Advanced]` -> `Config only`
* [x] `time_lapse_gcode [Advanced]` -> `Config only`
* [x] `wrapping_detection_gcode [Advanced]` -> `Config only`
* [x] `change_filament_gcode [Advanced]` -> `Config only`
* [x] `change_extrusion_role_gcode [Advanced]` -> `Config only`
* [x] `machine_pause_gcode [Advanced]` -> `Config only`
* [x] `template_custom_gcode [Advanced]` -> `Config only`
* [x] `pause_gcode [Advanced]` legacy profile/import alias; current live Orca key is `machine_pause_gcode`, and app imports/exports it through the existing Pause G-code control -> `Config only`
* [x] `toolchange_gcode [Advanced]` legacy profile/import alias; current live Orca Machine G-code UI uses `change_filament_gcode` / `change_extrusion_role_gcode`, and app imports/exports it through the existing Change filament G-code control -> `Config only`

### Printer Output / Thumbnail / Metadata / Host

* [x] `thumbnails [Advanced]` -> `Config only`
* [x] `thumbnail_size [Advanced]` legacy/import alias remapped by Orca `PrintConfig.cpp` to `thumbnails`; app exports/imports it through the existing `Printer > Basic information > Advanced` G-code thumbnails control -> `Config only`
* [x] `thumbnails_format [Advanced]` Orca `comAdvanced`, separate `Tab.cpp` row is commented out and Orca syncs this from `thumbnails`; app derives/exports the enum from the existing G-code thumbnails value and imports direct values for config compatibility -> `Config only`
* [x] `thumbnails_internal [Advanced]` legacy/output metadata absent from current live Orca printer UI; preserved in import/export only -> `Config only`
* [x] `thumbnails_internal_switch [Advanced]` legacy/output metadata absent from current live Orca printer UI; preserved in import/export only -> `Config only`
* [x] `remaining_times [Advanced]` legacy/output metadata absent from current live Orca printer UI; preserved in import/export only -> `Config only`
* [x] `disable_m73 [Advanced]` -> `Config only`
* [x] `print_host [Advanced]` Orca physical-printer dialog `Hostname, IP or URL` -> `Connection UI + runtime Octo/Klipper`
* [x] `print_host_webui [Advanced]` Orca physical-printer dialog `Device UI` -> `Connection UI + in-app printer browser`
* [x] `host_type [Advanced]` Orca physical-printer dialog `Host Type` -> `Connection UI; runtime Octo/Klipper only`
* host-type modes:
  * [x] `host_type=prusalink` -> `Config only`
  * [x] `host_type=prusaconnect` -> `Config only`
  * [x] `host_type=octoprint` -> `Test/upload/browser runtime for OctoPrint and Moonraker/Klipper`
  * [x] `host_type=crealityprint` -> `Config only`
  * [x] `host_type=duet` -> `Config only`
  * [x] `host_type=flashair` -> `Config only`
  * [x] `host_type=astrobox` -> `Config only`
  * [x] `host_type=repetier` -> `Config only`
  * [x] `host_type=mks` -> `Config only`
  * [x] `host_type=esp3d` -> `Config only`
  * [x] `host_type=obico` -> `Config only`
  * [x] `host_type=flashforge` -> `Config only`
  * [x] `host_type=simplyprint` -> `Config only`
  * [x] `host_type=elegoolink` -> `Config only`
* [x] `printhost_apikey [Advanced]` Orca physical-printer dialog `API Key / Password` -> `Connection UI + runtime auth; Android Keystore-backed encrypted storage`
* [x] `printhost_authorization_type [Advanced]` Orca physical-printer dialog `Authorization Type` -> `Connection UI + runtime auth`
* printhost-authorization modes:
  * [x] `printhost_authorization_type=key` -> `Config only`
  * [x] `printhost_authorization_type=user` -> `Config only`
* [x] `printhost_cafile [Advanced]` Orca physical-printer dialog `HTTPS CA File` -> `Config only`
* [x] `printhost_password [Advanced]` Orca physical-printer dialog `Password` -> `Connection UI + runtime auth; Android Keystore-backed encrypted storage`
* [x] `printhost_port [Advanced]` Orca physical-printer dialog `Printer` -> `Config only`
* [x] `printhost_ssl_ignore_revoke [Advanced]` Orca physical-printer dialog Windows-only row, surfaced in Mobile Slicer's `Connection` tab for config compatibility -> `Config only`
* [x] `printhost_user [Advanced]` Orca physical-printer dialog `User` -> `Connection UI + runtime auth`

### Printer Vendor / Integration / Platform Keys

* [x] `active_feeder_motor_name [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `adaptive_bed_mesh_margin [Advanced]` -> `Config only`
* [x] `auto_disable_filter_on_overheat [Advanced]` Orca vendor machine-profile metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `auto_toolchange_command [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `auxiliary_fan [Advanced]` -> `Config only`
* [x] `bbl_use_printhost [Advanced]` -> `Config only`
* [x] `bed_mesh_max [Advanced]` -> `Config only`
* [x] `bed_mesh_min [Advanced]` -> `Config only`
* [x] `bed_mesh_probe_distance [Advanced]` -> `Config only`
* [x] `box_id [Advanced]` Orca vendor machine-profile metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `cooling_filter_enabled [Advanced]` Orca vendor machine-profile metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `cooling_tube_length [Advanced]` -> `Config only`
* [x] `cooling_tube_retraction [Advanced]` -> `Config only`
* [x] `creality_flush_time [Advanced]` Orca vendor machine-profile metadata seen in Creality machine JSON; preserved in import/export only -> `Config only`
* [x] `enable_power_loss_recovery [Advanced]` -> `Config only`
* power-loss-recovery modes:
  * [x] `enable_power_loss_recovery=printer_configuration` -> `Config only`
  * [x] `enable_power_loss_recovery=enable` -> `Config only`
  * [x] `enable_power_loss_recovery=disable` -> `Config only`
* [x] `enable_filament_ramming [Advanced]` -> `Config only`
* [x] `enable_pre_heating [Advanced]` Orca vendor machine-profile metadata seen in Bambu machine JSON; preserved in import/export only -> `Config only`
* [x] `extra_loading_move [Advanced]` -> `Config only`
* [x] `fan_direction [Advanced]` Orca vendor machine-profile metadata seen in Bambu/Qidi nozzle JSON; preserved in import/export only -> `Config only`
* [x] `family [Advanced]` Orca printer-model metadata (`Preset.hpp` key `BBL_JSON_KEY_FAMILY`, loaded by `PresetBundle.cpp`, used by config wizard family grouping); preserved in import/export only -> `Config only`
* [x] `grab_length [Advanced]` Orca `comDevelop`, PrintConfig-defined extruder grab-length compatibility field; preserved in import/export only -> `Config only`
* [x] `group_algo_with_time [Advanced]` Orca vendor machine-profile metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `high_current_on_filament_swap [Advanced]` -> `Config only`
* [x] `hotend_cooling_rate [Advanced]` Orca vendor machine-profile metadata seen in Bambu nozzle JSON; preserved in import/export only -> `Config only`
* [x] `hotend_heating_rate [Advanced]` Orca vendor machine-profile metadata seen in Bambu nozzle JSON; preserved in import/export only -> `Config only`
* [x] `hotend_model [Advanced]` Orca printer-model metadata (`Preset.hpp` key `BBL_JSON_KEY_HOTEND_MODEL`, loaded by `PresetBundle.cpp`, used by `PresetUtils::system_printer_hotend_model()` / `GCodeViewer.cpp`); preserved in import/export only -> `Config only`
* [x] `is_artillery [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `is_support_3mf [Advanced]` Orca vendor machine-profile capability metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `is_support_air_condition [Advanced]` Orca vendor machine-profile capability metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `is_support_mqtt [Advanced]` Orca vendor machine-profile capability metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `is_support_multi_box [Advanced]` Orca vendor machine-profile capability metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `is_support_timelapse [Advanced]` Orca vendor machine-profile capability metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `machine_LED_light_exist [Advanced]` Orca vendor machine-profile metadata seen in Creality machine JSON; preserved in import/export only -> `Config only`
* [x] `machine_hotend_change_time [Advanced]` Orca vendor machine-profile metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `machine_load_filament_time [Advanced]` -> `Config only`
* [x] `machine_platform_motion_enable [Advanced]` Orca vendor machine-profile metadata seen in Creality machine JSON; preserved in import/export only -> `Config only`
* [x] `machine_prepare_compensation_time [Advanced]` Orca vendor/profile metadata seen in Bambu machine and Qidi process JSON; preserved in import/export only -> `Config only`
* [x] `machine_switch_extruder_time [Advanced]` legacy/import alias remapped by Orca `PrintConfig.cpp` to `machine_tool_change_time`; app exports/imports it through the existing `Printer > Multimaterial` tool-change-time control -> `Config only`
* [x] `machine_tech [Advanced]` Orca printer-model metadata (`Preset.hpp` key `BBL_JSON_KEY_PRINTER_TECH`, loaded by `PresetBundle.cpp` into printer technology); preserved in import/export only -> `Config only`
* [x] `machine_tool_change_time [Advanced]` -> `Config only`
* [x] `machine_unload_filament_time [Advanced]` -> `Config only`
* [x] `manual_filament_change [Advanced]` -> `Config only`
* [x] `multi_zone [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `multi_zone_number [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `nozzle_flush_dataset [Advanced]` Orca vendor machine-profile opaque dataset seen in Bambu machine JSON; preserved in import/export only -> `Config only`
* [x] `parking_pos_retraction [Advanced]` -> `Config only`
* [x] `pellet_modded_printer` Orca `comSimple`, shown in `Printer > Basic information > Advanced` optgroup -> `Config only`
* [x] `printer_agent [Advanced]` Orca physical-printer dialog network agent selector; stored as `coString` -> `Config only`
* [x] `printer_model [Advanced]` Orca `PrintConfig.cpp` printer identity key `Printer type`; current vendored `TabPrinter::build()` does not surface it as a normal live row, preserved in import/export only -> `Config only`
* [x] `printer_notes [Advanced]` Orca `Printer > Notes`, `comAdvanced` -> `Config only`
* [x] `printer_structure` Orca `comDevelop`; preserved in import/export only because current Orca screenshots do not show the developer row -> `Config only`
* [x] `printer_technology [Advanced]` Orca common printer identity enum with `FFF` / `SLA`; current app remains FFF-first but preserves/exports the profile key -> `Config only`
* printer-technology modes:
  * [x] `printer_technology=FFF` -> `Config only`
  * [x] `printer_technology=SLA` -> `Config only`
* [x] `printer_variant [Advanced]` Orca `PrintConfig.cpp` printer identity key, typically nozzle/variant text; preserved in import/export only -> `Config only`
* [x] `ramming_pressure_advance_value [Advanced]` Orca vendor machine/profile metadata; preserved in import/export only -> `Config only`
* [x] `right_icon_offset_bed [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `scan_first_layer [Advanced]` -> `Config only`
* [x] `scan_folder [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `support_air_filtration` Orca `comDevelop`; preserved in import/export but hidden from normal mobile Printer UI because current Orca screenshots do not show the developer row -> `Config only`
* [x] `support_box_temp_control [Advanced]` Orca vendor machine-profile capability metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `support_chamber_temp_control` Orca `comDevelop`; preserved in import/export but hidden from normal mobile Printer UI because current Orca screenshots do not show the developer row -> `Config only`
* [x] `support_cooling_filter [Advanced]` Orca vendor machine-profile capability metadata seen in Qidi machine JSON; preserved in import/export only -> `Config only`
* [x] `support_multi_bed_types` -> `Config only`
* [x] `support_multi_filament [Advanced]` Orca vendor machine-profile capability metadata; preserved in import/export only -> `Config only`
* [x] `support_object_skip_flush [Advanced]` Orca vendor machine-profile capability metadata seen in Bambu machine JSON; preserved in import/export only -> `Config only`
* [x] `support_wan_network [Advanced]` Orca vendor machine-profile capability metadata; preserved in import/export only -> `Config only`
* [x] `time_cost [Advanced]` -> `Config only`
* [x] `tool_change_temprature_wait [Advanced]` Orca vendor machine-profile metadata, preserving Orca's misspelled key name; preserved in import/export only -> `Config only`
* [x] `upward_compatible_machine [Advanced]` Orca vendor machine-profile compatibility metadata seen in Bambu machine JSON; preserved in import/export only -> `Config only`
* [x] `url [Advanced]` Orca vendor profile update/model URL metadata seen in Bambu machine JSON; preserved in import/export only -> `Config only`
* [x] `use_active_pellet_feeding [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `use_extruder_rotation_volume [Advanced]` Orca vendor machine-profile metadata; preserved in import/export only -> `Config only`
* [x] `wipe_tower_type [Advanced]` -> `Config only`
* wipe-tower-type modes:
  * [x] `wipe_tower_type=type1` -> `Config only`
  * [x] `wipe_tower_type=type2` -> `Config only`
* [x] `wrapping_exclude_area [Advanced]` Orca `comAdvanced` clumping-detection probing area; current Orca Basic information UI comments this row out in `Tab.cpp`, preserved in import/export only -> `Config only`
* [x] `z_lift_type [Advanced]` legacy profile/import alias; current Orca uses `z_hop_types`, preserved in import/export only -> `Config only`

## Filament Checklist

### Material Identity / Physical Material Data

* [x] `filament_type` -> `Config only`
* `filament_type` parent coverage -> `All current Orca MaterialType::all enum values surfaced in Android dropdown`
* filament-type modes:
  * [x] `filament_type=PLA` -> `Config only`
  * [x] `filament_type=PLA+` legacy app/built-in preset value; not in current Orca `MaterialType::all()` enum -> `Config only`
  * [x] `filament_type=PETG` -> `Config only`
  * [x] `filament_type=ABS` -> `Config only`
  * [x] `filament_type=ASA` -> `Config only`
  * [x] `filament_type=TPU` -> `Config only`
  * [x] `filament_type=PA` -> `Config only`
  * [x] `filament_type=PC` -> `Config only`
  * [x] `filament_type=PET` -> `Config only`
  * [x] `filament_type=HIPS` -> `Config only`
  * [x] `filament_type=PVA` -> `Config only`
  * [x] `filament_type=ABS/ASA` verified unsupported: not in current Orca `MaterialType::all()` enum; app exposes separate `ABS` and `ASA` values instead -> `Config only`
  * [x] `filament_type=Support` verified unsupported: not in current Orca `MaterialType::all()` enum; app uses `filament_is_support` for support-material behavior -> `Config only`
* [x] `filament_vendor [Advanced]` -> `Config only`
* [x] `filament_map_mode [Advanced]` Orca `comAdvanced` plate/project filament mapping mode; not a normal live Process `Tab.cpp` row, preserved in import/export only -> `Config only`
* [x] `print_extruder_id [Advanced]` Orca per-process extruder ID metadata used by preset variant synchronization; not a normal live row, preserved in import/export only -> `Config only`
* [x] `print_extruder_variant [Advanced]` Orca per-process extruder variant metadata used by preset variant synchronization; not a normal live row, preserved in import/export only -> `Config only`
* [x] `allow_mix_temp [Advanced]` Orca CLI/project compatibility flag used by `OrcaSlicer.cpp` to allow high/low-temperature filaments together; not a normal Process `Tab.cpp` row, preserved in import/export only -> `Config only`
* [x] `allow_multicolor_oneplate [Advanced]` Orca CLI/project arrange flag used by `OrcaSlicer.cpp`; not a normal Process `Tab.cpp` row, preserved in import/export only -> `Config only`
* [x] `filament_soluble [Advanced]` -> `Config only`
* [x] `filament_is_support [Advanced]` -> `Config only`
* [x] `filament_change_length [Advanced]` -> `Config only`
* [x] `required_nozzle_HRC` Orca `comDevelop` filament metadata; preserved in import/export but hidden from normal mobile Filament UI because supplied Orca UI screenshots do not show developer rows -> `Config only`
* [x] `default_filament_colour [Advanced]` -> `Config only`
* [x] `filament_diameter` moved to Orca `Filament > Basic information` path -> `Config only`
* [x] `filament_adhesiveness_category` Orca `comDevelop` filament metadata; preserved in import/export but hidden from normal mobile Filament UI because supplied Orca UI screenshots do not show developer rows -> `Config only`
* [x] `filament_density [Advanced]` -> `Config only`
* [x] `filament_shrink [Advanced]` -> `Config only`
* [x] `filament_shrinkage_compensation_z [Advanced]` -> `Config only`
* [x] `filament_cost [Advanced]` -> `Config only`
* [x] `filament_extruder_variant [Advanced]` Orca per-filament extruder variant metadata used by preset variant synchronization; not a normal live row, preserved in import/export but not rendered as a Filament page control -> `Config only`
* [x] `filament_self_index [Advanced]` Orca per-filament index metadata; not a normal live row, preserved in import/export but not rendered as a Filament page control -> `Config only`
* [x] `temperature_vitrification` -> `Config only`
* [x] `idle_temperature` -> `Config only`
* [x] `pellet_flow_coefficient [Advanced]` Orca pellet-printer-only flow metadata; preserved in import/export but hidden from normal mobile Filament UI until printer-aware pellet mode exists -> `Config only`
* [x] `filament_flow_ratio [Advanced]` -> `Config only - Waydroid`
* [x] `enable_pressure_advance [Advanced]` -> `Config only - Waydroid`
* [x] `pressure_advance [Advanced]` -> `Config only - Waydroid`
* [x] `adaptive_pressure_advance [Advanced]` -> `Config only`
* [x] `adaptive_pressure_advance_overhangs [Advanced]` -> `Config only`
* [x] `adaptive_pressure_advance_bridges [Advanced]` -> `Config only`
* [x] `adaptive_pressure_advance_model [Advanced]` -> `Config only`
* [x] `filament_max_volumetric_speed [Advanced]` -> `Device tested`
* [x] `filament_adaptive_volumetric_speed [Advanced]` -> `Config only - Waydroid`
* [x] `volumetric_speed_coefficients [Advanced]` -> `Config only - Waydroid`
* [x] `min_layer_height [Advanced]` -> `Config only`
* [x] `max_layer_height [Advanced]` -> `Config only`

### Temperature

* [x] `nozzle_temperature_initial_layer` -> `Device tested`
* [x] `nozzle_temperature` -> `Device tested`
* [x] `chamber_temperature` / `chamber_temperatures` import alias -> `Config only`
* [x] `activate_chamber_temp_control` -> `Config only`
* [x] `supertack_plate_temp_initial_layer` -> `Config only`
* [x] `supertack_plate_temp` -> `Config only`
* [x] `hot_plate_temp_initial_layer` -> `Device tested`
* [x] `hot_plate_temp` -> `Device tested`
* [x] `cool_plate_temp_initial_layer` -> `Config only`
* [x] `cool_plate_temp` -> `Config only`
* [x] `textured_cool_plate_temp_initial_layer` -> `Config only`
* [x] `textured_cool_plate_temp` -> `Config only`
* [x] `textured_plate_temp_initial_layer` -> `Config only`
* [x] `textured_plate_temp` -> `Config only`
* [x] `eng_plate_temp_initial_layer` -> `Config only`
* [x] `eng_plate_temp` -> `Config only`
* [x] `nozzle_temperature_range_low` -> `Config only`
* [x] `nozzle_temperature_range_high` -> `Config only`

### Cooling

* [x] `fan_min_speed` -> `Device tested`
* [x] `fan_max_speed` -> `Device tested`
* [x] `close_fan_the_first_x_layers` -> `Device tested`
* [x] `full_fan_speed_layer [Advanced]` -> `Config only`
* [x] `fan_cooling_layer_time` -> `Config only`
* [x] `slow_down_layer_time` -> `Config only`
* [x] `reduce_fan_stop_start_freq` -> `Config only`
* [x] `slow_down_for_layer_cooling` -> `Config only`
* [x] `dont_slow_down_outer_wall` moved to Orca `Filament > Cooling > Part cooling fan` -> `Config only`
* [x] `slow_down_min_speed [Advanced]` -> `Config only`
* [x] `enable_overhang_bridge_fan` -> `Config only`
* [x] `overhang_fan_threshold [Advanced]` -> `Config only`
* overhang-fan-threshold modes:
  * [x] `overhang_fan_threshold=0%` -> `Config only`
  * [x] `overhang_fan_threshold=10%` -> `Config only`
  * [x] `overhang_fan_threshold=25%` -> `Config only`
  * [x] `overhang_fan_threshold=50%` -> `Config only`
  * [x] `overhang_fan_threshold=75%` -> `Config only`
  * [x] `overhang_fan_threshold=95%` -> `Config only`
* [x] `overhang_fan_speed [Advanced]` -> `Config only`
* [x] `internal_bridge_fan_speed [Advanced]` moved to Orca `Filament > Cooling > Part cooling fan` -> `Config only`
* [x] `support_material_interface_fan_speed [Advanced]` moved to Orca `Filament > Cooling > Part cooling fan` -> `Config only`
* [x] `ironing_fan_speed [Advanced]` -> `Config only`
* [x] `additional_cooling_fan_speed` -> `Config only`
* [x] `activate_air_filtration` -> `Config only`
* [x] `during_print_exhaust_fan_speed` -> `Config only`
* [x] `complete_print_exhaust_fan_speed` -> `Config only`

### Retraction / Z-Hop Overrides

* [x] `filament_retraction_length [Advanced]` -> `Config only - Waydroid`
* [x] `filament_z_hop [Advanced]` -> `Config only`
* [x] `filament_z_hop_types [Advanced]` -> `Config only`
* [x] `filament_retract_lift_above [Advanced]` -> `Config only`
* [x] `filament_retract_lift_below [Advanced]` -> `Config only`
* [x] `filament_retract_lift_enforce [Advanced]` -> `Config only`
* [x] `filament_retraction_speed [Advanced]` -> `Config only - Waydroid`
* [x] `filament_deretraction_speed [Advanced]` -> `Config only - Waydroid`
* [x] `filament_retract_restart_extra [Advanced]` -> `Config only`
* [x] `filament_retraction_minimum_travel [Advanced]` -> `Config only`
* [x] `filament_retract_when_changing_layer [Advanced]` -> `Config only`
* [x] `filament_wipe [Advanced]` -> `Config only`
* [x] `filament_wipe_distance [Advanced]` -> `Config only`
* [x] `filament_retract_before_wipe [Advanced]` -> `Config only`
* [x] `filament_long_retractions_when_cut [Advanced]` -> `Config only`
* [x] `filament_retraction_distances_when_cut [Advanced]` -> `Config only`
* [x] `filament_ironing_flow [Advanced]` -> `Config only`
* [x] `filament_ironing_spacing [Advanced]` -> `Config only`
* [x] `filament_ironing_inset [Advanced]` -> `Config only`
* [x] `filament_ironing_speed [Advanced]` -> `Config only`

### Wipe Tower / Multi-Material / G-code

* [x] `filament_minimal_purge_on_wipe_tower [Advanced]` -> `Config only`
* [x] `filament_tower_interface_pre_extrusion_dist [Advanced]` -> `Config only`
* [x] `filament_tower_interface_pre_extrusion_length [Advanced]` -> `Config only`
* [x] `filament_tower_ironing_area [Advanced]` -> `Config only`
* [x] `filament_tower_interface_purge_volume [Advanced]` -> `Config only`
* [x] `filament_tower_interface_print_temp [Advanced]` -> `Config only`
* [x] `long_retractions_when_ec [Advanced]` -> `Config only`
* [x] `retraction_distances_when_ec [Advanced]` -> `Config only`
* [x] `filament_loading_speed_start [Advanced]` -> `Config only`
* [x] `filament_loading_speed [Advanced]` -> `Config only`
* [x] `filament_unloading_speed_start [Advanced]` -> `Config only`
* [x] `filament_unloading_speed [Advanced]` -> `Config only`
* [x] `filament_toolchange_delay [Advanced]` -> `Config only`
* [x] `filament_cooling_moves [Advanced]` -> `Config only`
* [x] `filament_cooling_initial_speed [Advanced]` -> `Config only`
* [x] `filament_cooling_final_speed [Advanced]` -> `Config only`
* [x] `filament_stamping_loading_speed [Advanced]` -> `Config only`
* [x] `filament_stamping_distance [Advanced]` -> `Config only`
* [x] `filament_ramming_parameters [Advanced]` -> `Config only`
* [x] `filament_multitool_ramming [Advanced]` -> `Config only`
* [x] `filament_multitool_ramming_volume [Advanced]` -> `Config only`
* [x] `filament_multitool_ramming_flow [Advanced]` -> `Config only`
* [x] `filament_start_gcode [Advanced]` -> `Config only - Waydroid; Filament > Advanced tab, hidden until advanced profile settings are enabled`
* [x] `filament_end_gcode [Advanced]` -> `Config only - Waydroid; Filament > Advanced tab, hidden until advanced profile settings are enabled`

### Dependencies / Notes

* [x] `compatible_printers [Advanced]` -> `Config only`
* [x] `compatible_printers_condition [Advanced]` -> `Config only`
* [x] `compatible_prints [Advanced]` -> `Config only`
* [x] `compatible_prints_condition [Advanced]` -> `Config only`
* [x] `filament_notes [Advanced]` -> `Config only`

## Process Checklist

### Known Missing Live Orca Process Rows

These rows are visible in the vendored Orca process UI in
`vendor/orcaslicer/src/slic3r/GUI/Tab.cpp` but are not all first-class Android
profile controls yet. Keep this list current until each row is implemented,
verified as config-only, and moved into the checked sections below.

* `Multimaterial`: `prime_tower_enable_framework`, `wall_filament`,
  `solid_infill_filament`, `wipe_tower_filament`, `ooze_prevention`,
  `preheat_time`, `preheat_steps`, `interlocking_beam`,
  `mmu_segmented_region_max_width`,
  `mmu_segmented_region_interlocking_depth`, `interlocking_beam_width`,
  `interlocking_orientation`, `interlocking_beam_layer_count`,
  `interlocking_depth`, `interlocking_boundary_avoidance`

### Quality / Layers / Surface Basics

* [x] `initial_layer_print_height` -> `Device tested`
* [x] `layer_height` -> `Device tested`
* [x] `adaptive_layer_height [Advanced]` -> `Config only`
* [x] `top_shell_layers` -> `Device tested`
* [x] `bottom_shell_layers` -> `Device tested`
* [x] `top_shell_thickness` -> `Config only`
* [x] `bottom_shell_thickness` -> `Config only`
* [x] `top_surface_pattern` -> `Device tested`
* `top_surface_pattern` parent coverage -> `Parent surfaced, subset only`
* [x] `top_surface_density` -> `Config only`
* top-surface pattern modes:
  * [x] `top_surface_pattern=monotonicline` -> `Device tested`
  * [x] `top_surface_pattern=monotonic` -> `Config only`
  * [x] `top_surface_pattern=rectilinear` -> `Config only`
  * [x] `top_surface_pattern=concentric` -> `Device tested`
  * [x] `top_surface_pattern=alignedrectilinear` -> `Config only`
  * [x] `top_surface_pattern=hilbertcurve` -> `Config only`
  * [x] `top_surface_pattern=archimedeanchords` -> `Config only`
  * [x] `top_surface_pattern=octagramspiral` -> `Config only`
* [x] `bottom_surface_pattern` -> `Config only`
* `bottom_surface_pattern` parent coverage -> `Parent surfaced, subset only`
* [x] `bottom_surface_density` -> `Config only`
* bottom-surface pattern modes:
  * [x] `bottom_surface_pattern=monotonic` -> `Config only`
  * [x] `bottom_surface_pattern=monotonicline` -> `Config only`
  * [x] `bottom_surface_pattern=rectilinear` -> `Config only`
  * [x] `bottom_surface_pattern=alignedrectilinear` -> `Config only`
  * [x] `bottom_surface_pattern=concentric` -> `Config only`
  * [x] `bottom_surface_pattern=hilbertcurve` -> `Config only`
  * [x] `bottom_surface_pattern=archimedeanchords` -> `Config only`
  * [x] `bottom_surface_pattern=octagramspiral` -> `Config only`
* [x] `internal_solid_infill_pattern` -> `Config only`
* `internal_solid_infill_pattern` parent coverage -> `Parent surfaced, subset only`
* internal-solid pattern modes:
  * [x] `internal_solid_infill_pattern=monotonic` -> `Config only`
  * [x] `internal_solid_infill_pattern=monotonicline` -> `Config only`
  * [x] `internal_solid_infill_pattern=rectilinear` -> `Config only`
  * [x] `internal_solid_infill_pattern=alignedrectilinear` -> `Config only`
  * [x] `internal_solid_infill_pattern=zigzag` -> `Config only`
  * [x] `internal_solid_infill_pattern=concentric` -> `Config only`
* [x] `resolution [Advanced]` -> `Config only`
* [x] `slice_closing_radius [Advanced]` Orca `Quality > Precision`, `comAdvanced` -> `Config only`
* [x] `interface_shells [Advanced]` -> `Config only`
* [x] `detect_thin_wall [Advanced]` -> `Config only`
* [x] `detect_overhang_wall [Advanced]` -> `Config only`
* [x] `detect_narrow_internal_solid_infill [Advanced]` -> `Config only`
* [x] `elefant_foot_compensation [Advanced]` -> `Config only`
* [x] `apply_top_surface_compensation [Advanced]` -> `Config only`
* [x] `ensure_vertical_shell_thickness [Advanced]` -> `Config only`

### Walls / Perimeters / Surface Tuning

* [x] `wall_loops` -> `Device tested`
* [x] `precise_outer_wall` -> `Device tested`
* [x] `only_one_wall_first_layer` Orca `Quality > Walls and surfaces` -> `Config only`
* [x] `only_one_wall_top` -> `Device tested`
* [x] `wall_generator [Advanced]` -> `Config only`
* wall-generator modes:
  * [x] `wall_generator=classic` -> `Config only`
  * [x] `wall_generator=arachne` -> `Config only`
* [x] `wall_transition_length [Advanced]` Orca `Quality > Wall generator`, `comAdvanced` -> `Config only`
* [x] `wall_transition_filter_deviation [Advanced]` Orca `Quality > Wall generator`, `comAdvanced` -> `Config only`
* [x] `wall_transition_angle [Advanced]` Orca `Quality > Wall generator`, `comAdvanced` -> `Config only`
* [x] `wall_distribution_count [Advanced]` Orca `Quality > Wall generator`, `comAdvanced` -> `Config only`
* [x] `min_bead_width [Advanced]` Orca `Quality > Wall generator`, `comAdvanced` -> `Config only`
* [x] `min_feature_size [Advanced]` Orca `Quality > Wall generator`, `comAdvanced` -> `Config only`
* [x] `min_length_factor [Advanced]` Orca `Quality > Wall generator`, `comAdvanced` -> `Config only`
* [x] `wall_infill_order` -> `Config only`
* wall-infill-order modes:
  * [x] `wall_infill_order=inner wall/outer wall/infill` -> `Config only`
  * [x] `wall_infill_order=outer wall/inner wall/infill` -> `Config only`
  * [x] `wall_infill_order=inner-outer-inner wall/infill` -> `Config only`
  * [x] `wall_infill_order=infill/inner wall/outer wall` -> `Config only`
  * [x] `wall_infill_order=infill/outer wall/inner wall` -> `Config only`
* [x] `extra_perimeters_on_overhangs [Advanced]` -> `Config only`
* [x] `inner_wall_line_width [Advanced]` -> `Config only`
* [x] `outer_wall_line_width [Advanced]` -> `Config only`
* [x] `top_surface_line_width [Advanced]` -> `Config only`
* [x] `min_width_top_surface [Advanced]` -> `Config only`
* [x] `line_width [Advanced]` -> `Config only`
* [x] `xy_hole_compensation [Advanced]` -> `Config only`
* [x] `xy_contour_compensation [Advanced]` -> `Config only`

### Infill

* [x] `sparse_infill_density` -> `Device tested`
* [x] `sparse_infill_pattern` -> `Device tested`
* `sparse_infill_pattern` parent coverage -> `Parent surfaced, full app enum device-tested`
* sparse-infill pattern modes:
  * [x] `sparse_infill_pattern=grid` -> `Device tested`
  * [x] `sparse_infill_pattern=gyroid` -> `Device tested`
  * [x] `sparse_infill_pattern=cubic` -> `Device tested`
  * [x] `sparse_infill_pattern=rectilinear` -> `Device tested`
  * [x] `sparse_infill_pattern=alignedrectilinear` -> `Device tested`
  * [x] `sparse_infill_pattern=zigzag` -> `Device tested`
  * [x] `sparse_infill_pattern=crosszag` -> `Device tested`
  * [x] `sparse_infill_pattern=lockedzag` -> `Device tested`
  * [x] `sparse_infill_pattern=line` -> `Device tested`
  * [x] `sparse_infill_pattern=triangles` -> `Device tested`
  * [x] `sparse_infill_pattern=tri-hexagon` -> `Device tested`
  * [x] `sparse_infill_pattern=adaptivecubic` -> `Device tested`
  * [x] `sparse_infill_pattern=quartercubic` -> `Device tested`
  * [x] `sparse_infill_pattern=supportcubic` -> `Device tested`
  * [x] `sparse_infill_pattern=lightning` -> `Device tested`
  * [x] `sparse_infill_pattern=honeycomb` -> `Device tested`
  * [x] `sparse_infill_pattern=3dhoneycomb` -> `Device tested`
  * [x] `sparse_infill_pattern=lateral-honeycomb` -> `Device tested`
  * [x] `sparse_infill_pattern=lateral-lattice` -> `Device tested`
  * [x] `sparse_infill_pattern=crosshatch` -> `Device tested`
  * [x] `sparse_infill_pattern=tpmsd` -> `Device tested`
  * [x] `sparse_infill_pattern=tpmsfk` -> `Device tested`
  * [x] `sparse_infill_pattern=concentric` -> `Device tested`
  * [x] `sparse_infill_pattern=hilbertcurve` -> `Device tested`
  * [x] `sparse_infill_pattern=archimedeanchords` -> `Device tested`
  * [x] `sparse_infill_pattern=octagramspiral` -> `Device tested`
* [x] `sparse_infill_line_width [Advanced]` -> `Config only`
* [x] `sparse_infill_speed [Advanced]` -> `Device tested`
* [x] `sparse_infill_rotate_template [Advanced]` -> `Config only`
* [x] `sparse_infill_filament [Advanced]` -> `Config only`
* [x] `sparse_infill_flow_ratio [Advanced]` -> `Config only`
* [x] `minimum_sparse_infill_area [Advanced]` -> `Config only`
* [x] `infill_direction [Advanced]` -> `Config only`
* [x] `solid_infill_direction [Advanced]` -> `Config only`
* [x] `solid_infill_rotate_template [Advanced]` -> `Config only`
* [x] `align_infill_direction_to_model [Advanced]` -> `Config only`
* [x] `infill_combination [Advanced]` -> `Config only`
* [x] `infill_combination_max_layer_height [Advanced]` -> `Config only`
* [x] `infill_anchor [Advanced]` -> `Config only`
* [x] `infill_anchor_max [Advanced]` -> `Config only`
* [x] `infill_wall_overlap [Advanced]` -> `Config only`
* [x] `top_bottom_infill_wall_overlap [Advanced]` -> `Config only`
* [x] `lateral_lattice_angle_1 [Advanced]` -> `Config only`
* [x] `lateral_lattice_angle_2 [Advanced]` -> `Config only`
* [x] `fill_multiline` -> `Config only`
* [x] `gap_infill_speed [Advanced]` -> `Config only`
* [x] `gap_fill_flow_ratio [Advanced]` -> `Config only`
* [x] `internal_solid_infill_line_width [Advanced]` -> `Config only`
* [x] `internal_solid_infill_speed [Advanced]` -> `Device tested`
* infill / internal-solid / gap-infill speed subset coverage -> `Parent surfaced, subset only`
* [x] `internal_bridge_support_thickness [Advanced]` -> `Config only`
* [x] `dont_filter_internal_bridges [Advanced]` -> `Config only`

### Speed / Acceleration

* [x] `initial_layer_speed [Advanced]` -> `Device tested`
* [x] `initial_layer_infill_speed [Advanced]` -> `Device tested`
* [x] `initial_layer_line_width [Advanced]` -> `Config only`
* [x] `initial_layer_min_bead_width [Advanced]` -> `Config only`
* [x] `initial_layer_acceleration [Advanced]` -> `Config only`
* [x] `initial_layer_jerk [Advanced]` -> `Config only`
* [x] `first_layer_flow_ratio [Advanced]` -> `Config only`
* [x] `initial_layer_travel_speed [Advanced]` -> `Device tested`
* [x] `slow_down_layers [Advanced]` -> `Device tested`
* per-feature speed cluster parent coverage -> `Parent surfaced, subset only`
* [x] `travel_speed [Advanced]` -> `Device tested`
* [x] `outer_wall_speed [Advanced]` -> `Device tested`
* [x] `inner_wall_speed [Advanced]` -> `Device tested`
* [x] `top_surface_speed [Advanced]` -> `Device tested`
* [x] `bridge_speed [Advanced]` -> `Device tested`
* [x] `small_perimeter_speed [Advanced]` -> `Device tested`
* [x] `small_perimeter_threshold [Advanced]` -> `Device tested`
* [x] `bridge_acceleration [Advanced]` -> `Config only`
* [x] `internal_solid_infill_acceleration [Advanced]` -> `Config only - Orca Speed > Acceleration row`
* [x] `travel_acceleration [Advanced]` -> `Config only - Orca Speed > Acceleration row`
* [x] `accel_to_decel_enable [Advanced]` -> `Config only - Orca Speed > Acceleration row`
* [x] `accel_to_decel_factor [Advanced]` -> `Config only - Orca Speed > Acceleration row`
* [x] `bridge_angle [Advanced]` -> `Config only`
* [x] `bridge_density [Advanced]` -> `Config only`
* [x] `internal_bridge_angle [Advanced]` -> `Config only`
* [x] `internal_bridge_density [Advanced]` -> `Config only`
* [x] `internal_bridge_flow [Advanced]` -> `Config only`
* [x] `internal_bridge_speed [Advanced]` -> `Config only`
* [x] `thick_bridges [Advanced]` -> `Config only`
* [x] `default_acceleration [Advanced]` -> `Config only`
* [x] `default_junction_deviation [Advanced]` -> `Config only - Orca Speed > Jerk(XY) row`
* [x] `default_jerk [Advanced]` -> `Config only - Orca Speed > Jerk(XY) row`
* [x] `inner_wall_acceleration [Advanced]` -> `Device tested`
* [x] `inner_wall_jerk [Advanced]` -> `Config only`
* [x] `infill_jerk [Advanced]` -> `Config only - Orca Speed > Jerk(XY) row`
* [x] `top_surface_jerk [Advanced]` -> `Config only - Orca Speed > Jerk(XY) row`
* [x] `travel_jerk [Advanced]` -> `Config only - Orca Speed > Jerk(XY) row`
* [x] `inner_wall_flow_ratio [Advanced]` -> `Config only`
* [x] `outer_wall_acceleration [Advanced]` -> `Device tested`
* [x] `outer_wall_jerk [Advanced]` -> `Config only`
* [x] `outer_wall_flow_ratio [Advanced]` -> `Config only`
* [x] `top_surface_acceleration [Advanced]` -> `Device tested`
* [x] `sparse_infill_acceleration [Advanced]` -> `Device tested`
* [x] `top_solid_infill_flow_ratio [Advanced]` -> `Config only`
* [x] `bottom_solid_infill_flow_ratio [Advanced]` -> `Config only`
* [x] `overhang_1_4_speed [Advanced]` -> `Config only`
* [x] `overhang_2_4_speed [Advanced]` -> `Config only`
* [x] `overhang_3_4_speed [Advanced]` -> `Config only`
* [x] `overhang_4_4_speed [Advanced]` -> `Config only`
* [x] `enable_overhang_speed [Advanced]` -> `Config only - Orca Speed > Overhang speed row`
* [x] `slowdown_for_curled_perimeters [Advanced]` -> `Config only - Orca Speed > Overhang speed row`
* [x] `overhang_flow_ratio [Advanced]` -> `Config only`
* [x] `max_volumetric_extrusion_rate_slope [Advanced]` -> `Config only - Orca Speed > Advanced row`
* [x] `max_volumetric_extrusion_rate_slope_segment_length [Advanced]` -> `Config only - Orca Speed > Advanced row`
* [x] `extrusion_rate_smoothing_external_perimeter_only [Advanced]` -> `Config only - Orca Speed > Advanced row`
### Seam / Adhesion / Prime / Startup

* [x] `seam_position` -> `Device tested`
* seam-position modes:
  * [x] `seam_position=nearest` -> `Device tested`
  * [x] `seam_position=aligned` -> `Device tested`
  * [x] `seam_position=aligned_back` -> `Device tested`
  * [x] `seam_position=back` -> `Device tested`
  * [x] `seam_position=random` -> `Device tested`
* [x] `seam_gap [Advanced]` -> `Config only`
* [x] `staggered_inner_seams [Advanced]` Orca `Quality > Seam`, `comAdvanced` -> `Config only`
* [x] `seam_slope_type [Advanced]` -> `Config only`
* seam-slope modes:
  * [x] `seam_slope_type=none` -> `Config only`
  * [x] `seam_slope_type=external` -> `Config only`
  * [x] `seam_slope_type=all` -> `Config only`
* [x] `seam_slope_conditional [Advanced]` -> `Config only`
* [x] `scarf_angle_threshold [Advanced]` -> `Config only`
* [x] `scarf_overhang_threshold [Advanced]` -> `Config only`
* [x] `scarf_joint_speed [Advanced]` -> `Config only`
* [x] `scarf_joint_flow_ratio [Advanced]` -> `Config only`
* [x] `seam_slope_start_height [Advanced]` -> `Config only`
* [x] `seam_slope_entire_loop [Advanced]` -> `Config only`
* [x] `seam_slope_min_length [Advanced]` -> `Config only`
* [x] `seam_slope_steps [Advanced]` -> `Config only`
* [x] `seam_slope_inner_walls [Advanced]` -> `Config only`
* [x] `role_based_wipe_speed [Advanced]` Orca `Quality > Seam`, `comAdvanced` -> `Config only`
* [x] `wipe_speed [Advanced]` Orca `Quality > Seam`, `comAdvanced` -> `Config only`
* [x] `wipe_on_loops [Advanced]` Orca `Quality > Seam`, `comAdvanced` -> `Config only`
* [x] `wipe_before_external_loop [Advanced]` Orca `Quality > Seam`, `comAdvanced` -> `Config only`
* [x] `has_scarf_joint_seam [Advanced]` -> `Config only`
* [x] `counterbore_hole_bridging [Advanced]` -> `Config only`
* [x] `skirt_loops` -> `Device tested`
* [x] `skirt_type [Advanced]` -> `Config only`
* skirt-type modes:
  * [x] `skirt_type=combined` -> `Config only`
  * [x] `skirt_type=perobject` -> `Config only`
* [x] `min_skirt_length [Advanced]` -> `Config only - Orca Others > Skirt row`
* [x] `skirt_distance [Advanced]` -> `Config only`
* [x] `skirt_start_angle [Advanced]` -> `Config only - Orca Others > Skirt row`
* [x] `skirt_speed [Advanced]` -> `Config only - Orca Others > Skirt row`
* [x] `skirt_height` -> `Config only`
* [x] `brim_width` -> `Device tested`
* [x] `brim_type` -> `Config only`
* brim-type modes:
  * [x] `brim_type=auto_brim` -> `Config only`
  * [x] `brim_type=brim_ears` -> `Config only`
  * [x] `brim_type=painted` -> `Config only`
  * [x] `brim_type=outer_only` -> `Config only`
  * [x] `brim_type=inner_only` -> `Config only`
  * [x] `brim_type=outer_and_inner` -> `Config only`
  * [x] `brim_type=no_brim` -> `Config only`
* [x] `brim_object_gap [Advanced]` -> `Config only`
* [x] `brim_use_efc_outline [Advanced]` -> `Config only - Orca Others > Brim row`
* [x] `combine_brims [Advanced]` -> `Config only - Orca Others > Brim row`
* [x] `brim_ears [Advanced]` -> `Config only`
* [x] `brim_ears_detection_length [Advanced]` -> `Config only`
* [x] `brim_ears_max_angle [Advanced]` -> `Config only`
* [x] `slicing_mode [Advanced]` -> `Config only - Orca Others > Special mode row`
* [x] `print_order [Advanced]` -> `Config only - Orca Others > Special mode row`

### Multimaterial / Prime Tower

* [x] `enable_prime_tower` -> `Config only`
* [x] `enable_tower_interface_features [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `enable_tower_interface_cooldown_during_tower [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `prime_tower_width` -> `Config only`
* [x] `prime_tower_skip_points [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `prime_volume` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `prime_tower_brim_width [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `prime_tower_infill_gap [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_rotation_angle [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_bridging [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_extra_spacing [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_extra_flow [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_max_purge_speed [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_wall_type [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_cone_angle [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_extra_rib_length [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_rib_width [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_fillet_wall [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `wipe_tower_no_sparse_layers [Advanced]` -> `Config only`
* [x] `single_extruder_multi_material_priming [Advanced]` Orca `Process > Multimaterial > Prime tower` -> `Config only`
* [x] `standby_temperature_delta [Advanced]` -> `Config only`
* [x] `flush_into_infill` Orca `Process > Multimaterial > Flush options` -> `Config only`
* [x] `flush_into_objects` Orca `Process > Multimaterial > Flush options` -> `Config only`
* [x] `flush_into_support` Orca `Process > Multimaterial > Flush options` -> `Config only`

### Support

* [x] `enable_support` -> `Device tested`
* [x] `support_type` -> `Config only`
* support-type modes:
  * [x] `support_type=normal(auto)` -> `Device tested`
  * [x] `support_type=tree(auto)` -> `Device tested`
  * [x] `support_type=normal(manual)` -> `Config only`
  * [x] `support_type=tree(manual)` -> `Config only`
* [x] `support_style [Advanced]` -> `Config only`
* support-style modes:
  * [x] `support_style=default` -> `Device tested`
  * [x] `support_style=grid` -> `Device tested`
  * [x] `support_style=snug` -> `Config only`
  * [x] `support_style=organic` -> `Config only`
  * [x] `support_style=tree_slim` -> `Config only`
  * [x] `support_style=tree_strong` -> `Config only`
  * [x] `support_style=tree_hybrid` -> `Config only`
* [x] `support_threshold_angle` -> `Config only`
* [x] `support_threshold_overlap` -> `Config only - Orca Support > Support simple row`
* [x] `support_buildplate_only` -> `Config only`
* [x] `support_critical_regions_only [Advanced]` -> `Config only - Orca Support > Support advanced row`
* [x] `support_remove_small_overhang [Advanced]` -> `Config only - Orca Support > Support advanced row`
* [x] `independent_support_layer_height [Advanced]` -> `Config only`
* [x] `support_filament` -> `Config only`
* [x] `support_interface_filament` -> `Config only`
* [x] `support_interface_not_for_body` -> `Config only - Orca Support > Support filament simple row`
* [x] `support_line_width [Advanced]` -> `Config only`
* [x] `support_top_z_distance [Advanced]` -> `Config only`
* [x] `support_bottom_z_distance [Advanced]` -> `Config only`
* [x] `support_interface_top_layers [Advanced]` -> `Config only`
* [x] `support_interface_bottom_layers [Advanced]` -> `Config only`
* [x] `support_interface_spacing [Advanced]` -> `Config only`
* [x] `support_bottom_interface_spacing [Advanced]` -> `Config only`
* [x] `support_interface_speed [Advanced]` -> `Config only`
* [x] `support_interface_flow_ratio [Advanced]` -> `Config only`
* [x] `support_interface_pattern [Advanced]` -> `Config only`
* support-interface-pattern modes:
  * [x] `support_interface_pattern=auto` -> `Config only`
  * [x] `support_interface_pattern=rectilinear` -> `Config only`
  * [x] `support_interface_pattern=concentric` -> `Config only`
  * [x] `support_interface_pattern=rectilinear_interlaced` -> `Config only`
  * [x] `support_interface_pattern=grid` -> `Config only`
* [x] `support_interface_loop_pattern [Advanced]` -> `Config only`
* [x] `support_base_pattern [Advanced]` -> `Config only`
* support-base-pattern modes:
  * [x] `support_base_pattern=default` -> `Config only`
  * [x] `support_base_pattern=rectilinear` -> `Config only`
  * [x] `support_base_pattern=rectilinear-grid` -> `Config only`
  * [x] `support_base_pattern=honeycomb` -> `Config only`
  * [x] `support_base_pattern=lightning` -> `Config only`
  * [x] `support_base_pattern=hollow` -> `Config only`
* [x] `support_base_pattern_spacing [Advanced]` -> `Config only`
* [x] `support_angle [Advanced]` -> `Config only - Orca Support > Advanced pattern angle row`
* [x] `support_speed [Advanced]` -> `Config only`
* [x] `support_flow_ratio [Advanced]` -> `Config only`
* [x] `support_object_xy_distance [Advanced]` -> `Config only`
* [x] `support_object_first_layer_gap [Advanced]` -> `Config only - Orca Support > Advanced row`
* [x] `support_object_elevation [Advanced]` -> `Config only`
* [x] `support_max_bridge_length [Advanced]` -> `Config only`
* [x] `support_ironing [Advanced]` -> `Config only`
* [x] `support_ironing_flow [Advanced]` -> `Config only`
* [x] `support_ironing_spacing [Advanced]` -> `Config only`
* [x] `support_expansion [Advanced]` -> `Config only`
* [x] `bridge_no_support [Advanced]` -> `Config only`
* [x] `draft_shield [Advanced]` -> `Config only`
* draft-shield modes:
  * [x] `draft_shield=disabled` -> `Config only`
  * [x] `draft_shield=enabled` -> `Config only`
* [x] `tree_support_branch_angle [Advanced]` -> `Config only`
* [x] `tree_support_branch_diameter [Advanced]` -> `Config only`
* [x] `tree_support_wall_count [Advanced]` -> `Config only`

### Special Modes / Output / Other Process Controls

* [x] `enable_arc_fitting [Advanced]` -> `Config only`
* [x] `spiral_mode` -> `Config only`
* [x] `raft_layers [Advanced]` -> `Config only`
* [x] `raft_contact_distance [Advanced]` -> `Config only`
* [x] `raft_expansion [Advanced]` -> `Config only`
* [x] `raft_first_layer_density [Advanced]` -> `Config only`
* [x] `raft_first_layer_expansion [Advanced]` -> `Config only`
* [x] `reduce_crossing_wall [Advanced]` -> `Config only`
* [x] `reduce_infill_retraction [Advanced]` -> `Config only`
* [x] `max_travel_detour_distance [Advanced]` -> `Config only`
* [x] `alternate_extra_wall [Advanced]` -> `Config only`
* [x] `extra_solid_infills [Advanced]` -> `Config only`
* [x] `hole_to_polyhole [Advanced]` -> `Config only`
* [x] `hole_to_polyhole_threshold [Advanced]` -> `Config only`
* [x] `hole_to_polyhole_twisted [Advanced]` -> `Config only`
* [x] `seam_gap [Advanced]` -> `Config only`
* [x] `print_sequence` -> `Config only`
* print-sequence modes:
  * [x] `print_sequence=by layer` -> `Config only`
  * [x] `print_sequence=by object` -> `Config only`
* [x] `wall_sequence [Advanced]` -> `Config only`
* [x] `spiral_mode_smooth` Orca `Process > Others > Special mode` -> `Config only`
* [x] `spiral_mode_max_xy_smoothing [Advanced]` Orca `Process > Others > Special mode` -> `Config only`
* [x] `spiral_starting_flow_ratio [Advanced]` Orca `Process > Others > Special mode` -> `Config only`
* [x] `spiral_finishing_flow_ratio [Advanced]` Orca `Process > Others > Special mode` -> `Config only`
* [x] `timelapse_type` Orca `Process > Others > Special mode` -> `Config only`
* [x] `enable_wrapping_detection [Advanced]` Orca `Process > Others > Special mode` -> `Config only`
* [x] `gcode_add_line_number [Advanced/develop]` Orca `Process > Others > G-code output` -> `Config only`
* [x] `gcode_comments [Advanced]` Orca `Process > Others > G-code output` -> `Config only`
* [x] `gcode_label_objects [Advanced]` Orca `Process > Others > G-code output` -> `Config only`
* [x] `exclude_object [Advanced]` Orca `Process > Others > G-code output` -> `Config only`
* wall-sequence modes:
  * [x] `wall_sequence=inner wall/outer wall` -> `Config only`
  * [x] `wall_sequence=outer wall/inner wall` -> `Config only`
  * [x] `wall_sequence=inner-outer-inner wall` -> `Config only`
* [x] `filename_format [Advanced]` -> `Config only`
* [x] `post_process [Advanced]` Orca `Process > Others > Post-processing Scripts` -> `Config only`
* [x] `notes [Advanced]` Orca `Process > Others > Notes` configuration notes -> `Config only`
* [x] `bridge_flow [Advanced]` -> `Config only`
* [x] `ironing_type [Advanced]` -> `Config only`
* ironing-type modes:
  * [x] `ironing_type=no ironing` -> `Config only`
  * [x] `ironing_type=top` -> `Config only`
  * [x] `ironing_type=topmost` -> `Config only`
  * [x] `ironing_type=solid` -> `Config only`
* [x] `ironing_pattern [Advanced]` -> `Config only`
* ironing-pattern modes:
  * [x] `ironing_pattern=rectilinear` -> `Config only`
  * [x] `ironing_pattern=concentric` -> `Config only`
* [x] `support_ironing_pattern [Advanced]` -> `Config only`
* support-ironing-pattern modes:
  * [x] `support_ironing_pattern=rectilinear` -> `Config only`
  * [x] `support_ironing_pattern=concentric` -> `Config only`
* [x] `ironing_flow [Advanced]` -> `Config only`
* [x] `ironing_spacing [Advanced]` -> `Config only`
* [x] `ironing_inset [Advanced]` -> `Config only`
* [x] `ironing_angle [Advanced]` -> `Config only`
* [x] `ironing_angle_fixed [Advanced]` -> `Config only`
* [x] `ironing_speed [Advanced]` -> `Config only`
* [x] `fuzzy_skin` -> `Config only`
* fuzzy-skin modes:
  * [x] `fuzzy_skin=none` -> `Config only`
  * [x] `fuzzy_skin=external` -> `Config only`
  * [x] `fuzzy_skin=all` -> `Config only`
  * [x] `fuzzy_skin=allwalls` -> `Config only`
  * [x] `fuzzy_skin=disabled_fuzzy` -> `Config only`
* [x] `fuzzy_skin_thickness` -> `Config only`
* [x] `fuzzy_skin_point_distance` -> `Config only`
* [x] `fuzzy_skin_first_layer` -> `Config only`
* [x] `fuzzy_skin_mode` -> `Config only`
* fuzzy-skin-mode modes:
  * [x] `fuzzy_skin_mode=displacement` -> `Config only`
  * [x] `fuzzy_skin_mode=extrusion` -> `Config only`
  * [x] `fuzzy_skin_mode=combined` -> `Config only`
* [x] `fuzzy_skin_noise_type` -> `Config only`
* fuzzy-skin-noise modes:
  * [x] `fuzzy_skin_noise_type=classic` -> `Config only`
  * [x] `fuzzy_skin_noise_type=perlin` -> `Config only`
  * [x] `fuzzy_skin_noise_type=billow` -> `Config only`
  * [x] `fuzzy_skin_noise_type=ridgedmulti` -> `Config only`
  * [x] `fuzzy_skin_noise_type=voronoi` -> `Config only`
* [x] `fuzzy_skin_scale [Advanced]` -> `Config only`
* [x] `fuzzy_skin_octaves [Advanced]` -> `Config only`
* [x] `fuzzy_skin_persistence [Advanced]` -> `Config only`
* current process checklist is actively checked against vendored Orca `Tab.cpp`;
  tree-support parity was refreshed after finding the prior no-remaining note was
  stale.
* 2026-04-27 Prusa process preset import audit:
  * generated Orca printer assets now include `**/process/*.json` as Gradle
    inputs so printer/process compatibility bundles do not go stale
  * Prusa process compatibility supports both explicit `compatible_printers`
    lists and Orca's newer process-name target suffixes such as
    `@CORE One 0.4`, `@CORE One HF 0.4`, and `@MK4S HF0.4`
  * verified regenerated Prusa bundles are non-empty for CORE One, CORE One
    HF/L, MINI/MINI IS, MK3.5, MK3S, MK4, MK4S, MK4S HF, and XL/XL 5T
  * a follow-up whole-manifest audit found one remaining empty process bundle,
    `Wanhao France / D12 500 PRO M2 DIRECT`; Orca's nozzle-machine JSON names
    the M2 DIRECT printer but carries a mismatched `printer_model`, so generated
    assets now also match nozzle-machine `name` after removing the nozzle suffix
  * verified the regenerated 335-printer manifest has zero empty process bundles
* moved out of Process during the 2026-04-26 audit:
  * `enable_power_loss_recovery` belongs to
    `Printer > Basic information > Advanced`
  * `host_type` and `printhost_authorization_type` belong to Orca's physical
    printer / print-host dialog, not the Process editor; they are now surfaced
    under the dedicated Printer `Connection` tab and intentionally remain
    visible without the advanced toggle because printer connection is a core
    app workflow
  * `extruder_type`, `nozzle_volume_type`, and `z_hop_types` are
    printer/project extruder controls, not Process editor controls
  * `z_lift_type` is retained only as a legacy profile/import alias; Orca's
    live config key is `z_hop_types`

## Advanced Controls Rule

Future app behavior:

* add `Settings > Advanced slicer controls`
* unchecked:
  * hide every checklist item marked `Advanced`
  * keep Printer `Connection` visible; its print-host controls are source-backed
    by Orca physical-printer settings but are classified as normal app workflow
    controls
* checked:
  * show both normal and advanced settings
* keep the setting names, tabs, and subgroup labels aligned with Orca where
  practical; when a setting is metadata rather than a live Orca row, document
  the exception beside the checklist item

## Delivery Rule

Before the project resumes broader UI/features, this checklist should be used as
the accountability surface for settings coverage.

Expected workflow:

1. pick one bounded settings cluster
2. surface it in the correct `Printer` / `Filament` / `Process` section
3. classify it in the app as `Config only` or `Device tested`
4. record the finer proof detail in the repo truth docs when needed
5. only then move to the next cluster

Advanced visibility auditing is intentionally tracked only in this file.
