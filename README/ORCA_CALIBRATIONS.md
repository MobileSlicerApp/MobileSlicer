# Orca Calibrations

This file is the source of truth for Mobile Slicer's Orca calibration parity work.

## Current Status

Mobile Slicer has a working calibration flow, but it is not complete Orca parity yet.

Implemented:

* Home shows a `Printer Calibrations` card below model import.
* Calibration entry is blocked unless a printer, filament, and process are selected.
* The calibration picker lists the current Orca-style calibration set:
  * Pressure Advance
  * Flow Rate
  * Temperature Tower
  * Max Volumetric Speed
  * VFA
  * Retraction
  * Input Shaping Frequency
  * Input Shaping Damping
  * Cornering
* Each calibration opens an options dialog before starting.
* Confirming a calibration copies a bundled calibration STL asset onto the build
  plate and opens Workspace.
* Calibration slicing applies temporary override JSON through
  `CalibrationJob.applyTemporaryOverrides(...)`.
* Saved printer, filament, and process profiles are not mutated by calibration
  override settings.
* The native wrapper reads Mobile Slicer calibration metadata and calls
  `Print::set_calib_params(...)`.
* Android links Orca's real `libslic3r/calib.cpp`, so Pressure Advance line and
  pattern helpers are no longer Android stubs.
* Pressure Advance now mirrors Orca's desktop PA dialog defaults for method and
  extruder changes:
  * DDE PA Tower: `0 -> 0.1`, step `0.002`, numbers off
  * DDE PA Line: `0 -> 0.1`, step `0.002`, numbers user-toggleable
  * DDE PA Pattern: `0 -> 0.08`, step `0.005`, numbers forced on
  * Bowden PA Tower / PA Line: `0 -> 1.0`, step `0.02`
  * Bowden PA Pattern: `0 -> 1.0`, step `0.05`
  * PA Pattern exposes Orca's comma-separated acceleration and speed override
    inputs and forwards them into native `Calib_Params`.
* Flow Rate now mirrors Orca's manual calibration stage choice:
  * `Complete Calibration` loads the pass 1 flow-rate asset.
  * `Fine Calibration based on flow ratio` loads the pass 2 flow-rate asset and
    exposes Orca's single base flow-ratio input.
  * Fine flow-ratio input is constrained to Orca's valid range
    `0.0 < flow ratio < 2.0` before temporary override metadata is written.
  * Flow Rate pass 1 and pass 2 now use split coupon STL assets named after the
    original Orca 3MF objects, preserving names like `flowrate_m20` and
    `flowrate_20` so native slicing can apply per-object flow modifiers.

Not complete yet:

* Orca option dialog parity has not been fully audited field-by-field against the
  vendored Orca GUI.
* STL conversion loses any 3MF per-object config/modifier metadata that Orca may
  depend on for exact calibration behavior.
* Calibration result save/apply flows are not mapped back into editable printer,
  filament, or process profiles yet.
* Each calibration's generated G-code still needs device-side comparison against
  Orca output for the same printer, filament, process, nozzle, and options.

## Pressure Advance Notes

Current PA reference points:

* Orca desktop PA dialog fields are in
  `vendor/orcaslicer/src/slic3r/GUI/calib_dlg.cpp`.
* Orca PA method defaults are set in `PA_Calibration_Dlg::reset_params()`.
* Orca manual PA wizard line/pattern behavior is in
  `vendor/orcaslicer/src/slic3r/GUI/CalibrationWizard.cpp` and
  `vendor/orcaslicer/src/slic3r/GUI/CalibrationWizardPresetPage.cpp`.
* Orca adds line/pattern/tower geometry in `Plater::calib_pa(...)`.
* Orca native line/pattern generation is in `vendor/orcaslicer/src/libslic3r/calib.cpp`.

Implemented PA parity so far:

* The Android dialog presents `DDE` / `Bowden` and `PA Tower` / `PA Line` /
  `PA Pattern`.
* Changing method or extruder type resets the same defaults used by Orca's
  desktop PA dialog.
* PA Pattern exposes acceleration and speed list inputs; empty lists keep Orca's
  native default behavior.
* The native wrapper forwards start, end, step, print-numbers, acceleration
  list, and speed list into `Slic3r::Calib_Params`.

Still not proven for complete PA parity:

* Device G-code comparison against Orca output for PA Tower, PA Line, and PA
  Pattern.
* Whether the STL-converted PA Pattern asset preserves every 3MF object modifier
  Orca uses before slicing.
* Save/apply of measured PA result back into filament profile pressure-advance
  fields.

## Flow Rate Notes

Current Flow Rate reference points:

* Orca's manual flow calibration stage UI is in
  `vendor/orcaslicer/src/slic3r/GUI/CalibrationWizardPresetPage.cpp`.
* Orca validates fine flow-ratio input through
  `CalibUtils::validate_input_flow_ratio(...)`.
* Orca loads the flow-rate pass assets in
  `vendor/orcaslicer/src/slic3r/Utils/CalibUtils.cpp`.
* Orca desktop/plater flow calibration object and print overrides are in
  `vendor/orcaslicer/src/slic3r/GUI/Plater.cpp` in
  `adjust_settings_for_flowrate_calib(...)`.

Implemented Flow Rate parity so far:

* Android presents Orca's manual stage choices: `Complete Calibration` and
  `Fine Calibration based on flow ratio`.
* Complete calibration uses
  `calib_stl/filament_flow/flowrate-test-pass1-objects/*.stl`.
* Fine calibration uses
  `calib_stl/filament_flow/flowrate-test-pass2-objects/*.stl`.
* Fine calibration exposes one base `Flow ratio` value instead of the previous
  generic start/end/step range.
* The split asset generator preserves Orca's flow coupon object names from the
  source 3MF.
* The native wrapper reads those `flowrate_*` object names and applies Orca's
  non-linear flow modifier formula as object-level `print_flow_ratio`.
* Split coupon groups are translated as a group to the selected build plate
  center while preserving Orca's original relative coupon layout.
* Flow Rate export suggests `Filament Flow Rate Calibration.gcode`.
* Temporary overrides apply the Orca-style flow test shell setup: 1 wall loop,
  top-only one-wall behavior, 35% rectilinear sparse infill, top/internal solid
  line width at `1.2 * nozzle`, archimedean top surface pattern, disabled
  ironing, disabled resonance avoidance, and reduced crossing wall behavior.

Still not proven for complete Flow Rate parity:

* Generated G-code must confirm that each `flowrate_*` coupon is emitted as a
  separate object and that object-level `print_flow_ratio` differs according to
  the Orca modifier encoded in the name.
* Linear flow calibration assets (`Orca-LinearFlow*.stl`) exist in the Android
  asset pack but are not exposed in the Android Flow Rate dialog yet.
* Generated G-code comparison against Orca pass 1 and pass 2 output is still
  required.
* Save/apply of measured flow result back into the selected filament profile's
  flow-ratio field is not implemented yet.

## Source References

Use the vendored Orca source as the behavioral reference:

* `vendor/orcaslicer/src/slic3r/GUI/Calibration.cpp`
* `vendor/orcaslicer/src/slic3r/GUI/Calibration.hpp`
* `vendor/orcaslicer/src/slic3r/GUI/CalibrationPanel.cpp`
* `vendor/orcaslicer/src/slic3r/GUI/CalibrationPanel.hpp`
* `vendor/orcaslicer/src/slic3r/GUI/ExtrusionCalibration.cpp`
* `vendor/orcaslicer/src/slic3r/GUI/ExtrusionCalibration.hpp`
* `vendor/orcaslicer/src/libslic3r/calib.cpp`
* `vendor/orcaslicer/resources/calib/`

Mobile Slicer implementation entry points:

* `android-app/app/src/main/java/com/mobileslicer/calibration/CalibrationsScreen.kt`
* `android-app/app/src/main/java/com/mobileslicer/ModelLoaderScreen.kt`
* `engine-wrapper/orca_wrapper.cpp`
* `engine-wrapper/orca-android-libslic3r/CMakeLists.txt`
* `scripts/generate_orca_calibration_stls.py`
* `android-app/app/src/main/assets/calib_stl/`

## Calibration Asset Inventory

The current Android asset pack is generated from Orca's bundled calibration
resources by `scripts/generate_orca_calibration_stls.py`.

| Calibration | Android asset |
| --- | --- |
| Pressure Advance line | `calib_stl/pressure_advance/pressure_advance_test.stl` |
| Pressure Advance pattern | `calib_stl/pressure_advance/pa_pattern.stl` |
| Pressure Advance tower | `calib_stl/pressure_advance/tower_with_seam.stl` |
| Auto PA line single | `calib_stl/pressure_advance/auto_pa_line_single.stl` |
| Auto PA line dual | `calib_stl/pressure_advance/auto_pa_line_dual.stl` |
| Flow Rate pass 1 | `calib_stl/filament_flow/flowrate-test-pass1.stl` |
| Flow Rate pass 2 | `calib_stl/filament_flow/flowrate-test-pass2.stl` |
| Flow Rate pass 1 split coupons | `calib_stl/filament_flow/flowrate-test-pass1-objects/*.stl` |
| Flow Rate pass 2 split coupons | `calib_stl/filament_flow/flowrate-test-pass2-objects/*.stl` |
| Linear Flow | `calib_stl/filament_flow/Orca-LinearFlow.stl` |
| Linear Flow fine | `calib_stl/filament_flow/Orca-LinearFlow_fine.stl` |
| Temperature Tower | `calib_stl/temperature_tower/temperature_tower.stl` |
| Max Volumetric Speed | `calib_stl/volumetric_speed/SpeedTestStructure.stl` |
| VFA | `calib_stl/vfa/vfa.stl` |
| Retraction | `calib_stl/retraction/retraction_tower.stl` |
| Input Shaping Frequency | `calib_stl/input_shaping/ringing_tower.stl` |
| Input Shaping Damping | `calib_stl/input_shaping/fast_tower_test.stl` |
| Cornering | `calib_stl/cornering/SCV-V2.stl` |

## Parity Contract

For a calibration to be marked Orca-parity complete, all of the following must be
true:

* The visible Android options match Orca's current dialog options for that
  calibration.
* Defaults match Orca for the selected printer, filament, process, and nozzle.
* The same bundled Orca geometry is used, including object-level metadata when
  Orca relies on it.
* Calibration-specific process, filament, temperature, speed, acceleration,
  extrusion, spiral, brim, cooling, and support overrides match Orca's setup.
* The override layer is temporary and cannot mutate saved user profiles unless
  the user explicitly saves calibration results.
* The native slice config passes Orca's expected `Calib_Params` values for the
  selected calibration.
* Generated G-code is compared against Orca for the same inputs and documented.
* Result-save behavior maps back into the correct profile field:
  * Pressure Advance -> filament pressure advance fields
  * Flow Rate -> filament flow ratio fields
  * Temperature Tower -> filament temperature fields
  * Max Volumetric Speed -> filament max volumetric speed fields
  * Retraction -> filament/printer retraction fields as Orca defines them
  * Input Shaping / Cornering -> printer firmware/profile fields only where Orca
    supports saving them

## Next Work

The next bounded implementation pass should audit one calibration at a time
against Orca source and screenshots. Do not mark a calibration complete just
because the picker, dialog, asset copy, and temporary override path exist.

Recommended order:

1. Finish Pressure Advance G-code comparison and result-save mapping.
2. Finish Flow Rate object-metadata verification, Orca G-code comparison, and
   result-save mapping.
3. Temperature Tower, because temperature override behavior must be verified in
   generated G-code.
4. Max Volumetric Speed.
5. Retraction.
6. VFA.
7. Input Shaping Frequency and Damping.
8. Cornering.

Each pass should update this file with:

* Orca source functions used as reference.
* Android UI fields implemented.
* Asset and object metadata requirements.
* Temporary config keys applied.
* Native `Calib_Params` fields used.
* Verification commands and device result.
