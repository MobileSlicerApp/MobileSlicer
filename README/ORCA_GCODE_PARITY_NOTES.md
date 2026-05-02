# Orca G-code Parity Notes

This note tracks the first deep parity pass against OrcaSlicer using Benchy and the Bambu Lab A1/Generic PLA profile set. The goal is Orca-compatible G-code output from the Android slicer path, not just similar visual slicing.

## 2026-04-29 Profile Override Parity Pass

Goal:

* Preserve the full resolved Orca printer, filament, and process profiles as
  the baseline.
* Store only user-edited UI fields as override JSON.
* Apply those overrides on top of the resolved Orca baseline at slice time.
* Avoid changing unrelated profile fields when one UI field changes.

Implementation boundary:

* Printer profiles now carry `orcaMachineOverridesJson`.
* Filament profiles now carry `orcaFilamentOverridesJson`.
* Process profiles now carry `orcaProcessOverridesJson`.
* Printer, filament, and process editors compare edited native config output
  against the initial profile and write only changed native keys into the
  override JSON.
* Custom profile copies preserve Orca baseline/provenance plus override JSON.
* Native slice config assembly uses:
  * resolved Orca printer/model JSON
  * resolved Orca filament JSON or repaired asset-backed filament JSON
  * resolved Orca process JSON
  * then the corresponding override JSON layer
* The resolved Orca JSON is not mutated when the user edits one field.

Regression coverage:

* Process editor changes only edited Orca process keys.
* Printer editor changes only edited Orca machine keys.
* Filament editor changes only edited Orca filament keys.
* Native config override layers win over resolved Orca baselines.
* Native config cache invalidates when override content changes.
* Old unresolved custom process copies can be hydrated from resolved Orca
  process fallback while preserving edits as overrides.

Device/log proof:

* No-change ABS Benchy re-slice:
  * `/home/peanut/Downloads/3DBenchy_2h18m_abs.gcode`
  * `/home/peanut/Downloads/3DBenchy_2h18m_abs (1).gcode`
  * line count matched: `205892`
  * layer count matched: `320`
  * filament matched: `9.72 g`
  * time matched: `2h18m16s`
  * config differences: `0`
  * command count differences: none
  * feature count differences: none
  * byte difference was only generated timestamp metadata
  * log showed `profile_override_counts=printer:0,filament:0,process:0`
* Changed wall-loop ABS Benchy comparison:
  * `/home/peanut/Downloads/3DBenchy_2h18m_abs.gcode`
  * `/home/peanut/Downloads/3DBenchy_2h32m_abs.gcode`
  * config differences: exactly one key, `wall_loops`
  * Orca/baseline file had `wall_loops = 3`
  * changed mobile file had `wall_loops = 6`
  * time changed from `2h18m16s` to `2h32m16s`
  * filament changed from `9.72 g` to `11.35 g`
  * this confirmed changed UI fields affect G-code and preview, while
    unrelated profile fields stay stable.

Bed-type parity fix found during this pass:

* Resolved Orca `default_bed_type` could be preserved while
  `curr_bed_type` stayed on the MobileSlicer profile default, e.g.
  `High Temp Plate`.
* The final config now carries the resolved Orca default bed type into
  `curr_bed_type` when the resolved Orca profile does not specify a separate
  current bed type.
* Regression test:
  `resolvedOrcaPrinterModelDefaultBedTypeWinsOverFilamentPlateTemps`.

Printable-area coordinate-frame fix found during Flashforge AD5M proof:

* Symptom:
  * Flashforge Adventurer 5M slices failed with Orca
    `ORCA_PLATE_PRINTABLE_AREA_ERROR` even when the Benchy was visually on the
    bed.
  * Native diagnostics showed `printableBounds=[-10,210]x[-10,210]`, while the
    emitted Flashforge profile purge line intentionally extruded at `Y=-110`.
* Root cause:
  * The Android wrapper applied `bed_width_mm` / `bed_depth_mm` before applying
    the resolved Orca `bed_shape` / `printable_area`.
  * For printers with centered or negative-origin printable areas, the seeded
    Orca default area could survive long enough for the width/depth override to
    preserve the wrong center.
  * This was not a Flashforge-specific model-placement bug; it was a generic
    profile coordinate-frame bug.
* Fix:
  * `engine-wrapper/orca_wrapper.cpp` now applies explicit `bed_shape` or
    `printable_area` into `printable_area` before width/depth normalization.
  * `apply_printable_volume_override` still resizes from the configured
    printable-area center, so non-zero-origin and centered printer profiles keep
    their Orca coordinate frame.
* Device proof:
  * The same Flashforge Adventurer 5M Benchy that failed before the fix sliced
    successfully after install.
  * This keeps the fix general for all Orca profiles with custom printable
    coordinates instead of forcing a model-center workaround for one printer.

Random Flashforge PLA parity check after the printable-area fix:

* Files:
  * Mobile: `/home/peanut/Downloads/3DBenchy_36m06s_pla.gcode`
  * Orca: `/home/peanut/Downloads/3DBenchy_PLA_37m35s.gcode`
* Strong parity signals:
  * layers: `240` vs `240`
  * filament length: `3769.52 mm` vs `3769.45 mm`
  * filament volume: `9.07 cm3` vs `9.07 cm3`
  * filament mass: `11.24 g` vs `11.24 g`
  * max Z: `48.00`
  * Flashforge start purge coordinates now match and no longer fail printable
    area validation.
* Remaining known setup mismatch:
  * Mobile used `Generic PLA @System` / `OGFL99`.
  * Orca used `Flashforge Generic PLA` / `FFG01`.
  * The files also differed in `curr_bed_type` (`Textured PEI Plate` vs
    `High Temp Plate`) and thumbnail generation.
* Resulting drift:
  * estimated time: `36m 06s` Mobile vs `37m 35s` Orca
  * config differences were mostly filament/bed/profile identity fields, fan
    and temperature defaults, pressure advance, max volumetric speed, and
    thumbnail settings.
  * command count differences were mainly extra `G1` segmentation and one fewer
    `M73` in Mobile.
* Interpretation:
  * This is a good post-fix smoke result, but not a strict parity fixture
    because the filament and bed-type selections are not identical.

Current status:

* Profile override parity is considered working for the tested no-change and
  single-field-change flows.
* Remaining performance work is not profile related; see
  `README/NATIVE_SLICE_PERF_NOTES.md`.

## Baseline Files

- Orca reference: `/home/peanut/Downloads/3DBenchy_PLA_53m14s.gcode`
- Mobile outputs inspected during the pass included:
  - `/home/peanut/Downloads/3DBenchy_31m17s_pla.gcode`
  - `/home/peanut/Downloads/3DBenchy_47m12s_totalestimatedtime_53m21s_pla.gcode`

## Final Benchy/A1 State

After linking the real Orca post-processor, the latest checked mobile output was close to Orca on the major print-critical summary values:

- Orca time: `model printing time: 47m 5s; total estimated time: 53m 14s`
- Mobile time: `model printing time: 47m 12s; total estimated time: 53m 21s`
- Orca filament: `3759.08 mm`, `9.04 cm3`, `11.21 g`
- Mobile filament: `3760.85 mm`, `9.05 cm3`, `11.22 g`
- Layers: both `240`
- XY bounds: matched
- `M73` command count no longer appeared in command diffs after the real `GCodeProcessor` path was enabled.

Remaining differences in that run were mostly geometry/post-processing counts and a test setup issue where the mobile UI had two PLA filament slots active while the Orca reference was single-filament.

## Root Causes Found

### Stubbed GCodeProcessor

The Android print target was still using `GCodeProcessor` stubs from `engine-wrapper/orca-android-libslic3r/android_libslic3r_print_stubs.cpp`.

Impact:

- Native Orca time estimation and placeholder replacement were not running.
- `M73` progress generation was incomplete.
- UI and file names showed invalid estimates such as `31m17s` or earlier extreme fallback values.

Fix:

- Link `vendor/orcaslicer/src/libslic3r/GCode/GCodeProcessor.cpp` into the Android print G-code target.
- Guard the Android `GCodeProcessor` / `CommandProcessor` stub block with `ORCA_ANDROID_REAL_GCODE_PROCESSOR`.
- Update `stub_inventory.json` so `GCodeProcessor` is marked as non-print fallback only.

### Missing Real Gap/Internal Infill Dependencies

The print target previously relied on compatibility stubs for geometry and infill behavior that Orca uses in normal desktop slicing.

Impact:

- Gap/internal infill behavior drifted from Orca.
- Filament usage and feature counts were far from the reference before these were restored.

Fixes:

- Link real `Geometry/MedialAxis.cpp`.
- Link real `Geometry/VoronoiOffset.cpp`.
- Link real `Fill/FillConcentricInternal.cpp`.
- Guard the matching stubs with `ORCA_ANDROID_REAL_MEDIAL_AXIS` and `ORCA_ANDROID_REAL_CONCENTRIC_INTERNAL`.

### Bambu Printer Flag Not Set

The Android wrapper did not force the Orca `Print` object into Bambu mode after applying config.

Impact:

- Bambu-specific custom G-code/timelapse blocks drifted.
- Counts for commands like `M624`, `M625`, `M1004`, `M622`, `M623`, `M971`, and `M991` were wrong before the fix.

Fix:

- In `engine-wrapper/orca_wrapper.cpp`, set `print.is_BBL_printer()` when `printer_model` starts with `Bambu Lab`.

### Orca Profile Values Were Being Dropped

The mobile profile adapter was flattening and defaulting too aggressively instead of preserving the resolved Orca profile JSON.

Impact:

- Header/config values diverged from Orca.
- Skin/skeleton, machine limit, wall direction, and other less-visible settings could silently drift.

Fixes:

- Preserve and re-merge resolved Orca printer/filament/process JSON.
- Treat Orca printer/process context as sufficient to apply Orca process template defaults.
- Pass through first-class values for skin/skeleton settings, machine limit vectors, and `wall_direction`.
- Add `WallDirection::Auto` support in the vendored Orca config and execute it like Orca's effective CCW behavior.

### UI Time Summary Parsing

Orca writes both model time and total time in one comment:

```gcode
; model printing time: 47m 12s; total estimated time: 53m 21s
```

The Android UI summary code was taking everything after the first colon and showing:

```text
Time: 47m 12s; total estimated time: 53m 21s
```

Impact:

- The compact workspace status row was overcrowded.
- The displayed time was not the total time users expect.

Fixes:

- Native C++ summary generation now prefers the `total estimated time` segment and emits only that value in `time=...`.
- Kotlin fallback/native summary parsing also normalizes stale combined values.
- UI remains `Time: <value>` and uses total estimated time when available.
- Filament grams parsing sums multi-value Orca lines such as `11.22, 0.00` and displays a single total.

## Verification Commands Used

Build and install:

```bash
cd /home/peanut/Development/MobileSlicer/android-app
./gradlew :app:assembleDebug
cd /home/peanut/Development/MobileSlicer
adb -s 192.168.1.156:40697 install -r android-app/app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.1.156:40697 shell am force-stop com.mobileslicer
```

Focused tests:

```bash
cd /home/peanut/Development/MobileSlicer/android-app
./gradlew :app:testDebugUnitTest --tests com.mobileslicer.NativeSliceConfigTest
./gradlew :app:testDebugUnitTest --tests com.mobileslicer.GcodeSummaryParserTest
```

Static check:

```bash
cd /home/peanut/Development/MobileSlicer
git diff --check
```

Parity comparison:

```bash
python3 scripts/compare_orca_gcode.py \
  /home/peanut/Downloads/3DBenchy_PLA_53m14s.gcode \
  /home/peanut/Downloads/3DBenchy_47m12s_totalestimatedtime_53m21s_pla.gcode
```

## Remaining Parity Risks

- Keep random-printer tests to the same model, same filament count, same process preset, same plate setup, and same printer preset on both sides. The last Benchy run had two mobile PLA slots active while the Orca reference had one, causing avoidable config drift and prime-tower-related diffs.
- Color/profile metadata still had mobile defaults such as `#8FC1FF` where the Orca reference used `#26A69A`.
- Some machine metadata still differed, including `bed_exclude_area`, `head_wrap_detect_zone`, `flush_volumes_*`, `wipe_tower_x`, and `wipe_tower_y`.
- Feature and command diffs remain for `G1`, `G2`, `G3`, `M106`, `M204`, and several feature counts. These should be investigated with a random-printer parity matrix after eliminating setup/config drift.

## Creality K2 0.2 mm Random-Printer Pass

Compared:

- Orca reference: `/home/peanut/Downloads/3DBenchy_PLA_2h11m.gcode`
- Mobile output: `/home/peanut/Downloads/3DBenchy_2h12m55s_pla.gcode`

Good signals:

- Time was close: Orca `2h 11m 22s`, mobile `2h 12m 55s`.
- Layers matched: both `600`.
- XY bounds were effectively matched.
- `M73` count matched.

Root issue found:

- Orca reference used `wall_generator = classic`.
- Mobile emitted `wall_generator = arachne`.
- On this 0.2 mm super-detail profile, that changed perimeter generation enough that Orca had `2062` gap infill features while mobile had `0`.
- Mobile filament was low: Orca `11.13 g`, mobile `10.78 g`, consistent with missing classic gap fill and related perimeter/infill redistribution.
- The same resolved-process override path also changed `only_one_wall_top` from Orca's `1` to mobile's `0`.

Underlying fix:

- Non-Bambu Orca template defaults no longer replace `wall_generator` when the resolved Orca process explicitly provides it.
- Non-Bambu Orca template defaults no longer replace `only_one_wall_top` when the resolved Orca process explicitly provides it.
- A regression test now covers a non-Bambu Orca profile with `wall_generator = classic`, `only_one_wall_top = true`, and `gap_infill_speed = 50`.

## Prusa CORE One HF 0.4 mm Random-Printer Pass

Compared:

- Orca reference: `/home/peanut/Downloads/3DBenchy_0.4n_0.15mm_PLA_Prusa CORE One HF_44m54s.gcode`
- Mobile output: `/home/peanut/Downloads/3DBenchy_56m18s_pla.gcode`

Good signals:

- Layers matched: both `320`.
- XY bounds matched within rounding.
- Major feature counts were close.

Root issue found:

- Orca reference used `filament_settings_id = "Generic PLA @System"`.
- Mobile used `filament_settings_id = "Prusa Generic PLA Silk @CORE One"`.
- That changed print-critical filament values:
  - Flow ratio: Orca `0.98`, mobile `1`.
  - Max volumetric speed: Orca `12`, mobile `7`.
  - Nozzle temperature: Orca `220`, mobile `225`.
  - Initial nozzle temperature: Orca `220`, mobile `230`.
  - Fan and cooling times also differed.
- Result: mobile time was much slower (`56m18s` vs `44m54s`) even though model/process geometry was broadly aligned.

Underlying fix:

- Generic Orca filament fallback now only considers plain generic material presets, such as `Generic PLA @System` or true family-generic variants like `Generic PLA @BBL P2S`.
- Specialty variants such as `PLA Silk` are no longer eligible as the generic fallback just because they are compatible with the selected printer and have vendor `Generic`.
- A regression test now covers Prusa CORE One HF choosing System Generic PLA over a compatible `Prusa Generic PLA Silk @CORE One` specialty preset for generic PLA fallback.
- Explicit user selection of Silk remains valid; this fix prevents automatic generic fallback/recommendation from substituting Silk when parity expects generic PLA.

## Next Random Printer Parity Test Checklist

1. Export the Orca reference G-code from desktop with the exact printer, filament, and process preset intended for mobile.
2. In mobile, load only the same number of filament slots used by the Orca project.
3. Slice and export from the freshly installed APK.
4. Run `scripts/compare_orca_gcode.py` on the two files.
5. Triage in this order:
   - Header/profile/config diffs.
   - Filament mass/volume/length.
   - Layer count and bounds.
   - Bambu or printer-specific command blocks.
   - Movement command counts and feature counts.
   - UI summary display.
