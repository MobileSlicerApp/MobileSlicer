# Orca Profile Import / Export

## Goal

Define a practical, honest, and maintainable way for MobileSlicer to:

* import OrcaSlicer printer / filament / process profiles into the app's native
  profile model
* export MobileSlicer native profiles back into Orca-compatible preset JSON

This document assumes the intended architecture is:

* MobileSlicer remains the owner of its own persisted profile store
* vendored Orca printer presets are the normal source for non-custom printers
* imported Orca printer data replaces the old MobileSlicer built-in printer list
* only user-created manual printers are treated as `Custom`
* imported Orca data is translated into native app display/cache fields while
  preserving the raw/resolved Orca data needed for slicing
* exported Orca data is generated from native app profiles
* Android runtime slicing must use resolved Orca-equivalent settings wherever
  the app claims an imported printer is usable

That matches the current repo direction:

* app-owned profile storage in
  `android-app/app/src/main/java/com/mobileslicer/profiles/ProfileModels.kt`
* app-owned config flattening in `ActiveSlicerConfiguration.toNativeSliceConfigJson()`
* staged expansion of Orca-style settings coverage tracked in
  `README/SETTINGS_CHECKLIST.md`

## Non-Goals

This document does not propose:

* copying the full desktop Orca preset manager into the app
* claiming full Orca parity before the app actually supports all mapped fields
* hiding partial coverage behind vague "compatible" language

It also does not allow returning to a curated built-in printer list. The
accepted product direction is:

* the visible Printer Profiles list starts empty on a fresh install
* `Select Printer` imports Orca printer presets into that list
* `New Custom Printer` creates the only manually-authored/custom printer type
* imported Orca printers should not be labeled `Built-in`

## Current Constraints

## App Model

Today the app owns three native profile types:

* `PrinterProfile`
* `FilamentProfile`
* `ProcessProfile`

Those are persisted locally and selected independently.

The app currently stores a bounded subset of Orca-style settings, not the full
preset universe. That means import/export must be coverage-aware.

## Android Runtime

The reduced Android wrapper/core still exists in-tree for explicit non-shipping
development probes:

* `engine-wrapper/orca_wrapper_android.cpp`
* `engine-wrapper/orca-android-core/orca_android_core.cpp`

The Android app build now refuses to package that reduced wrapper unless a
developer explicitly disables the real-wrapper requirement and opts into the
probe path at CMake level. The current proven shipping ARM output path is the
wrapper-integrated Orca-style slice/export path documented in
`CURRENT_STATUS.md` and `README/BUILD_SYSTEM.md`. Import/export still needs to
stay coverage-aware because MobileSlicer only surfaces and proves a bounded
subset of Orca's preset
universe at a time.

The immediate value of import/export is:

* profile portability
* profile authoring convenience
* future-proofing for wider settings coverage

It is not a claim of full desktop Orca preset parity on Android.

The normal mobile slice path currently has one active filament/material. For
that reason `ActiveSlicerConfiguration.toNativeSliceConfigJson()` preserves the
saved Orca profile values, then applies a runtime-only single-material native
export rule before calling the Android slicer: prime tower, purge-in-prime-tower,
single-extruder multi-material priming, multicolor-one-plate, and flush-into
targets are disabled in the temporary native JSON. This avoids invoking desktop
multi-material tower paths for single-filament Android slices while keeping the
profile UI/import data unchanged for future full multi-material work.

## Upstream Orca Presets

Vendored Orca profiles under `vendor/orcaslicer/resources/profiles` are not
simple flat records. They may include:

* `inherits`
* `from`
* type and metadata keys
* compatibility filters
* printer/nozzle/material specializations
* keys the app currently does not support

That is why import needs a resolver layer instead of direct one-shot mapping.

## Current Product Contract For Filament Import

`Select Filament` is a library picker, not a hard printer-compatibility gate.
All generated Orca filament presets should remain available to import for the
selected printer. The printer-specific part is the imported/saved
`FilamentProfile` copy, which is bound to the active `PrinterProfile` so native
slicing still receives a coherent printer/filament pair.

The picker presents Orca compatibility as organization and warning metadata:

* `Recommended` is the first/default category when a printer is selected.
* `All` remains available and searchable for cross-printer or experimental
  material choices.
* both category controls show counts for the current search result set.
* non-recommended rows are still selectable, but are labeled as not
  Orca-recommended for the selected printer only where that warning is useful;
  the `All` list hides non-recommended warning text to reduce visual noise.
* visible duplicate rows are collapsed at picker time. Orca often ships many
  files for one displayed filament name, such as printer/nozzle-specific
  variants of the same material. When those collapse to one visible row, the
  picker prefers a variant compatible with the selected printer.
* generic material-name rows such as `PLA / <vendor>` also collapse to one row.
  The selected-printer compatible row wins first; if none is compatible, the
  `Generic` vendor row is used as the representative.

The generated manifest and import bundles are still preserved underneath the
picker. De-duplication is a UI/catalog decision; import should continue to keep
the raw and resolved Orca filament JSON plus its source chain.

For performance, the app prebuilds the picker-facing filament row index in the
background for the selected printer/nozzle and caches it by printer compatibility
key. That row index already contains the chosen visible representative, merged
search text, and selected-printer compatibility flag. The picker should filter
that compact row index for search, `Recommended`, and `All`; it should not redo
raw manifest parsing, duplicate collapse, or compatibility scoring while rows
are being rendered.

## Current Product Contract For Printer Import

`Select Printer` is the entry point for importing Orca printer presets. The
selection catalog should be built from Orca `machine_model` entries so the user
chooses printer models, not every individual nozzle JSON as a separate printer.

When a printer is imported, the initial import must store enough data to make
the printer immediately meaningful in the app:

* printer display name
* printer family/vendor
* printer thumbnail/cover image for the Printer Profiles card
* Orca machine model path and raw JSON
* all matching nozzle-specific machine preset paths
* available nozzle diameters
* bed width and bed depth parsed from the resolved Orca `printable_area`
* max printable height parsed from resolved Orca `printable_height`
* generated Android asset paths for Orca `bed_model` and `bed_texture` when
  the source profile declares them
* the resolved inheritance source chain used to derive those fields

Bed dimensions are not optional. The imported printer must be able to drive the
workspace bed size as soon as it appears in Printer Profiles.

Orca bed assets are also part of the import contract. When present, the
workspace uses the generated `bed_model` and `bed_texture` assets to render the
same printer-specific build plate family OrcaSlicer uses, including stickers,
labels, and Orca-style grid behavior. Custom or missing-asset profiles fall
back to the older procedural Android bed.

The initial import may stage deeper settings as raw/resolved Orca payloads
before every field is surfaced in the editor, but it must not discard them. The
goal is exact usable printer behavior over time, not a lossy name/nozzle import.

## Generated Printer Thumbnail Assets

Printer thumbnails shown in the picker and imported printer cards are generated
APK assets, but they are product-required assets, not disposable noise.

Source cover files live under:

```text
vendor/orcaslicer/resources/profiles/<family>/<printer>_cover.png
```

The Android build generates:

```text
android-app/app/build/generated/orcaPrinterAssets/orca-printers/covers/
android-app/app/build/generated/orcaPrinterAssets/orca-printers/printer_presets.json
```

The generated manifest should contain cover paths for every generated printer
model that has a source cover. A healthy generation currently produces hundreds
of covers, not a few dozen. If a Gradle run fails while deleting or regenerating
`orcaPrinterAssets`, do not assume the partial directory is acceptable just
because it is under `build/generated`. Force regeneration before installing:

```bash
cd android-app
./gradlew :app:generateOrcaPrinterAssets --rerun-tasks
```

Then verify the manifest and cover count before packaging or installing. A
partial generated output can make printer thumbnails disappear in the app even
though the source Orca cover files are still present.

## Recommended Architecture

Build one translation subsystem with two directions:

* `Orca preset JSON -> Native profile model`
* `Native profile model -> Orca preset JSON`

That subsystem should live conceptually between:

* raw file parsing
* native app profile persistence

Do not bury this logic inside Compose UI or inside JNI.

Recommended layers:

1. `OrcaPresetSource`
2. `OrcaPresetResolver`
3. `OrcaPrinterCatalog`
4. `OrcaPrinterImporter`
5. `OrcaToNativeMapper`
6. `NativeToOrcaMapper`
7. `ImportExportReport`

Recommended location:

* Android-side Kotlin, near the existing profile model layer
* keep UI as a caller, not the owner of parsing/mapping rules

Possible package:

* `com.mobileslicer.profiles.orca`

## Core Data Shapes

## Raw Orca Preset

Represent the loaded raw preset before mapping:

```kotlin
data class OrcaRawPreset(
    val presetId: String,
    val fileName: String,
    val presetType: OrcaPresetType,
    val json: JSONObject
)

enum class OrcaPresetType {
    Printer,
    Filament,
    Process,
    Unknown
}
```

`presetId` should be a resolver-facing identity, not just display name. It
should include enough path/type context to avoid collisions.

## Resolved Orca Preset

After inheritance resolution:

```kotlin
data class OrcaResolvedPreset(
    val presetId: String,
    val presetType: OrcaPresetType,
    val sourceChain: List<String>,
    val flattenedValues: Map<String, Any?>,
    val metadata: Map<String, Any?>,
    val unresolvedKeys: List<String> = emptyList()
)
```

Important split:

* `flattenedValues` is for real user-facing configuration
* `metadata` is for preset bookkeeping

Do not mix them.

## Native Import Result

```kotlin
data class OrcaImportResult<T>(
    val profile: T?,
    val warnings: List<String>,
    val ignoredKeys: List<String>,
    val unsupportedKeys: List<String>,
    val sourcePresetId: String
)
```

This is critical. Import must always be able to explain:

* what was mapped
* what was ignored
* what was unsupported
* whether the import is partial

## Export Result

```kotlin
data class OrcaExportResult(
    val fileName: String,
    val presetType: OrcaPresetType,
    val json: JSONObject,
    val warnings: List<String>
)
```

## Import Design

## 1. File Discovery

Support two import modes:

### A. Single File Import

User picks one Orca preset JSON file.

Use this for:

* importing one printer preset
* importing one filament preset
* importing one process preset

### B. Bundle / Folder Import

User picks a directory or archive containing multiple Orca presets.

Use this for:

* resolving `inherits` across multiple files
* importing a coherent printer + filament + process set

Bundle import is the more complete design. Single-file import should still
exist, but it will often have weaker inheritance resolution.

## 2. Preset Type Detection

Determine type from a combination of:

* directory path
* filename conventions
* explicit JSON keys when available

Recommended detection order:

1. explicit type field if present
2. parent directory name:
   * `machine` -> `Printer`
   * `filament` -> `Filament`
   * `process` -> `Process`
3. fallback heuristics based on keys:
   * printer-like keys:
     * `printable_area`
     * `printable_height`
     * `nozzle_diameter`
   * filament-like keys:
     * `filament_type`
     * `nozzle_temperature`
     * `bed_temperature`
   * process-like keys:
     * `layer_height`
     * `wall_loops`
     * `sparse_infill_density`

If detection is ambiguous, fail with an explicit error instead of guessing.

## 3. Metadata vs Config Separation

When reading raw Orca JSON, split keys into:

* preset metadata
* config values

Metadata includes keys listed as excluded in `README/SETTINGS_CHECKLIST.md`,
for example:

* `name`
* `inherits`
* `from`
* `settings_id`
* `compatible_printers`
* `default_print_profile`
* `default_filament_profile`

These should not be treated as app-editable slicer settings.

Config values are the actual printer / filament / process controls.

## 4. Inheritance Resolution

This is the most important import step.

Orca profiles frequently rely on `inherits`, so a naive one-file parse will be
lossy.

Resolver rules:

1. Load the selected preset.
2. If `inherits` exists, locate the parent preset.
3. Repeat until there is no parent.
4. Merge ancestors from oldest to newest.
5. Child value overrides parent value.
6. Track the source chain for diagnostics.

Resolver must detect:

* missing parent preset
* inheritance loops
* type mismatch across inheritance chain

Example merge order:

```text
base filament -> vendor filament -> printer-specific filament -> nozzle-specific filament
```

Final flattened map is what the native mapper consumes.

### Parent Lookup Strategy

Resolver should search parents in this order:

1. already loaded presets in the current import bundle
2. vendored Orca profiles shipped in the repo
3. optional app-bundled known base templates

This lets the app import custom presets that inherit from standard Orca bases
without requiring every base file to be present in the user-selected set.

### Merge Rules

For practical MobileSlicer import/export, merge like Orca preset resolution
normally works for scalar fields:

* absent child key -> inherit parent value
* present child scalar -> replace parent scalar
* present child array -> replace parent array
* present child object -> replace parent object unless a specific key requires
  structured merge

Do not over-engineer structured merging unless a real Orca preset key demands
it. Most preset keys are scalar or scalar-list oriented.

## 5. Mapping to Native Profiles

After flattening, map only supported keys into the app's native model.

This is not a generic reflection problem. Use explicit mapping tables.

Recommended approach:

```kotlin
data class OrcaFieldMapping<T>(
    val orcaKey: String,
    val apply: (MutableNativeDraft<T>, Any?) -> Unit
)
```

Use per-domain mapping registries:

* `PrinterImportMappings`
* `FilamentImportMappings`
* `ProcessImportMappings`

## Printer Mapping

Initial supported import set should match current native profile coverage:

* `printable_area` -> app `bedWidthMm`, `bedDepthMm`
* `printable_height` -> app `maxHeightMm`
* `nozzle_diameter` -> app `nozzleDiameterMm`
* `filament_diameter` -> app `filamentDiameterMm`

### Parsing `printable_area`

Orca printable area is typically polygon-like coordinate data, not direct width
and depth fields.

Import rule:

1. Parse all XY points.
2. Compute min/max X and min/max Y.
3. Derive width and depth from bounding box.
4. Reject obviously malformed areas.

This loses shape fidelity for irregular beds, but it matches the current native
app profile model, which stores width/depth only.

Record a warning when:

* the bed is not rectangular
* the current native display/cache field uses a rectangular approximation

Example warning:

```text
Imported printable_area as rectangular width/depth bounds for the native bed cache; original polygon is preserved in the Orca payload.
```

## Filament Mapping

Map current supported fields:

* `filament_type` -> `materialType`
* `nozzle_temperature_initial_layer` -> `nozzleTemperatureInitialLayerC`
* `nozzle_temperature` -> `nozzleTemperatureC`
* `bed_temperature_initial_layer` -> `bedTemperatureInitialLayerC`
* `bed_temperature` -> `bedTemperatureC`
* `fan_min_speed` / `fan_max_speed` -> current app `coolingPercent`
* `close_fan_the_first_x_layers` -> `noCoolingFirstLayers`

### Cooling Import Rule

The app currently stores one cooling baseline percentage, while Orca may carry:

* `fan_min_speed`
* `fan_max_speed`

Recommended behavior:

* if min == max, import directly
* if min != max, choose a documented rule:
  * preferred: use `fan_max_speed`
* emit a warning that range collapsed to one app field

## Process Mapping

Map current supported fields from the resolved Orca preset to:

* `first_layer_height`
* `layer_height`
* `initial_layer_speed` or `first_layer_print_speed`
* `initial_layer_infill_speed`
* `initial_layer_travel_speed`
* `slow_down_layers`
* `outer_wall_speed`
* `inner_wall_speed`
* `top_surface_speed`
* `travel_speed`
* `outer_wall_acceleration`
* `inner_wall_acceleration`
* `top_surface_acceleration`
* `sparse_infill_acceleration`
* `bridge_speed`
* `small_perimeter_speed`
* `small_perimeter_threshold`
* `sparse_infill_speed`
* `internal_solid_infill_speed`
* `gap_infill_speed`
* `top_shell_layers`
* `bottom_shell_layers`
* `seam_position`
* `precise_outer_wall`
* `only_one_wall_top`
* `top_surface_pattern`
* `wall_loops`
* `sparse_infill_density`
* `sparse_infill_pattern`
* `skirts` or `skirt_loops`
* `brim_width`

### Percent and Enum Normalization

Several Orca values need normalization:

* `sparse_infill_density` may be stored as `"15%"` or numeric text
* enum strings must match app enum values:
  * seam position
  * top surface pattern
  * sparse infill pattern

Use explicit normalizers:

* strip `%`
* trim whitespace
* lowercase only where safe
* map aliases to canonical app enum entries

If an enum value exists in Orca but not in the app, do not silently coerce it
to a random neighboring mode. Instead:

* use a safe default
* emit an unsupported-value warning

## 6. Handling Unsupported Fields

This is where honesty matters.

For each imported preset:

* mapped supported fields become native profile values
* metadata stays as metadata
* unsupported real settings are collected into `unsupportedKeys`
* ignored bookkeeping keys are collected into `ignoredKeys`

Example categories:

### Ignored By Design

* `inherits`
* `from`
* `compatible_printers`
* `default_print_profile`

### Unsupported Today

* `gcode_flavor`
* `machine_max_speed_x`
* `retraction_length`
* `bed_custom_texture`

### Partially Collapsed

* `fan_min_speed` + `fan_max_speed` -> one app `coolingPercent`
* non-rectangular `printable_area` -> rectangular width/depth approximation for
  the current native bed cache, while preserving the original Orca polygon in
  the imported payload

Import UI should surface this summary.

## 7. Imported Profile Identity

When creating native profiles from Orca imports:

* generate a new native app `id`
* preserve the Orca source name as user-visible name where possible
* store source provenance in metadata

Recommended extra metadata:

```kotlin
data class ImportedProfileProvenance(
    val sourceFormat: String = "orca",
    val sourcePresetName: String,
    val sourcePresetId: String,
    val importTimestampUtc: String,
    val wasPartialImport: Boolean
)
```

If native profile model should stay clean, store provenance in a parallel local
metadata table rather than inside the current data classes.

## 8. Validation Rules

Before accepting import, validate native values:

### Printer

* width > 0
* depth > 0
* height > 0
* nozzle diameter > 0
* filament diameter > 0

### Filament

* temperatures within sane range
* cooling 0-100
* no-cooling layers >= 0

### Process

* layer heights > 0
* speeds > 0 where required
* shell counts >= 0
* infill percent 0-100
* brim width >= 0

If invalid:

* reject import for hard-invalid core values
* clamp only when policy is explicit and documented

Prefer rejection for malformed source over silent mutation.

## 9. Persistence Strategy

Once mapped and validated, persist via the existing native profile repository:

* append to `ProfileStore`
* select optionally, not automatically by default
* mark as custom profile

This reuses existing app ownership and avoids adding a second runtime profile
store.

## Export Design

## 1. Export Philosophy

Export should generate Orca-compatible preset JSON from native app profiles.

Use export for:

* sharing MobileSlicer-tuned profiles with Orca users
* carrying app-native profiles into desktop workflows
* backup/interchange

Do not try to recreate the full richness of upstream Orca preset ecosystems on
day one.

Start with flattened valid presets, then add smarter inheritance later if
needed.

## 2. Export Modes

Support two modes.

### A. Flattened Export

Emit one standalone preset JSON file containing all mapped values directly.

Advantages:

* simplest to implement
* easiest to debug
* no dependency on parent presets being present

Disadvantages:

* more verbose
* less aligned with how Orca often structures its built-in presets

This should be the first implementation.

### B. Inheritance-Based Export

Emit a preset with `inherits` plus only overridden values.

Advantages:

* smaller files
* closer to upstream Orca style

Disadvantages:

* requires choosing a base preset safely
* more fragile if base preset does not exist on target Orca install

This should be a later optimization, not the initial path.

## 3. Orca Export Shapes

Export three file types separately:

* printer preset JSON
* filament preset JSON
* process preset JSON

Do not merge them into one app-specific bundle if the target is Orca.

Possible output naming:

* `MobileSlicer <name> printer.json`
* `MobileSlicer <name> filament.json`
* `MobileSlicer <name> process.json`

Or, if following Orca directory conventions:

* `machine/<name>.json`
* `filament/<name>.json`
* `process/<name>.json`

## 4. Export Metadata

Each Orca JSON needs enough metadata for Orca to treat it as a preset-like
object.

Exact required metadata should be verified against real Orca examples in
`vendor/orcaslicer/resources/profiles`, but export should at minimum include:

* `name`
* `from`
* `instantiation`
* type-specific identity fields where Orca expects them

Recommended conservative strategy:

* mirror the minimum metadata keys seen in a simple real built-in preset of the
  same type
* keep metadata generation explicit and versioned

Do not invent large amounts of fake metadata.

## 5. Native -> Orca Field Mapping

Build explicit export mapping tables parallel to import tables.

### Printer Export

From native printer profile:

* `bedWidthMm` + `bedDepthMm` -> `printable_area`
* `maxHeightMm` -> `printable_height`
* `nozzleDiameterMm` -> `nozzle_diameter`
* `filamentDiameterMm` -> `filament_diameter`

### `printable_area` Reconstruction

Because the native model is rectangular, export rectangular coordinates:

```text
(-width/2, -depth/2)
( width/2, -depth/2)
( width/2,  depth/2)
(-width/2,  depth/2)
```

This is deterministic and reversible against the app's current width/depth
model.

### Filament Export

From native filament profile:

* `materialType` -> `filament_type`
* `nozzleTemperatureInitialLayerC` -> `nozzle_temperature_initial_layer`
* `nozzleTemperatureC` -> `nozzle_temperature`
* `bedTemperatureInitialLayerC` -> `bed_temperature_initial_layer`
* `bedTemperatureC` -> `bed_temperature`
* `coolingPercent` -> both `fan_min_speed` and `fan_max_speed`
* `noCoolingFirstLayers` -> `close_fan_the_first_x_layers`

### Process Export

From native process profile:

* `firstLayerHeightMm` -> `first_layer_height`
* `layerHeightMm` -> `layer_height`
* `firstLayerPrintSpeedMmPerSec` -> `initial_layer_speed`
* `firstLayerInfillSpeedMmPerSec` -> `initial_layer_infill_speed`
* `firstLayerTravelSpeedPercent` -> `initial_layer_travel_speed`
* `slowDownLayers` -> `slow_down_layers`
* `outerWallSpeedMmPerSec` -> `outer_wall_speed`
* `innerWallSpeedMmPerSec` -> `inner_wall_speed`
* `topSurfaceSpeedMmPerSec` -> `top_surface_speed`
* `travelSpeedMmPerSec` -> `travel_speed`
* `outerWallAccelerationMmPerSec2` -> `outer_wall_acceleration`
* `innerWallAccelerationMmPerSec2` -> `inner_wall_acceleration`
* `topSurfaceAccelerationMmPerSec2` -> `top_surface_acceleration`
* `sparseInfillAccelerationMmPerSec2` -> `sparse_infill_acceleration`
* `bridgeSpeedMmPerSec` -> `bridge_speed`
* `smallPerimeterSpeedMmPerSec` -> `small_perimeter_speed`
* `smallPerimeterThresholdMm` -> `small_perimeter_threshold`
* `sparseInfillSpeedMmPerSec` -> `sparse_infill_speed`
* `internalSolidInfillSpeedMmPerSec` -> `internal_solid_infill_speed`
* `gapInfillSpeedMmPerSec` -> `gap_infill_speed`
* `topShellLayers` -> `top_shell_layers`
* `bottomShellLayers` -> `bottom_shell_layers`
* `seamPosition` -> `seam_position`
* `preciseOuterWall` -> `precise_outer_wall`
* `onlyOneWallTopSurfaces` -> `only_one_wall_top`
* `topSurfacePattern` -> `top_surface_pattern`
* `wallCount` -> `wall_loops`
* `infillPercent` -> `sparse_infill_density`
* `sparseInfillPattern` -> `sparse_infill_pattern`
* `skirts` -> `skirt_loops`
* `brimWidthMm` -> `brim_width`

## 6. Export Validation

Before writing Orca JSON:

* validate all native values
* ensure all exported enums are valid Orca enum values
* omit unsupported synthetic app-only fields

If a native field has no Orca equivalent:

* do not fabricate one
* add an export warning

## 7. Optional Base-Preset Export

Later, export may support selecting a known Orca base preset and emitting only
overrides.

Example:

* export a custom PLA filament profile as inheriting from Orca `Generic PLA`
* export a custom 0.20 process profile as inheriting from an Orca standard
  process preset

This should only be allowed if:

* the chosen base preset is explicit
* the target environment is expected to have that base preset

Otherwise flattened export is safer.

## Import / Export Reports

Every import and export action should produce a human-readable report.

Recommended report fields:

* source file(s)
* detected preset type
* inheritance chain used
* mapped fields count
* unsupported fields count
* ignored metadata keys count
* warnings
* whether result is partial

Example import summary:

```text
Imported Orca process preset "Standard @ Printer X"
Resolved 3-level inheritance chain
Mapped 24 supported fields
Ignored 8 metadata keys
Skipped 17 unsupported Orca fields
Result: partial import
```

This report should be available both:

* in UI
* in logs for debugging

## Round-Trip Expectations

Do not promise perfect round-trip behavior.

Expected truths:

* native -> Orca -> native should be stable for currently supported fields
* Orca -> native -> Orca will be lossy whenever the source used unsupported
  Orca keys or richer preset metadata/inheritance than the app stores

That is acceptable as long as it is explicit.

## Versioning Strategy

The mapping layer should be versioned against the app's supported settings
surface.

Recommended constants:

```kotlin
const val ORCA_IMPORT_EXPORT_SCHEMA_VERSION = 1
```

Use versioned tests and fixtures so future settings expansion can:

* add new mapped fields
* reduce unsupported-key counts
* preserve previous behavior intentionally

## Testing Strategy

## Unit Tests

Add pure tests for:

* preset type detection
* inheritance resolution
* printable area parsing
* enum normalization
* percent parsing
* import mapping for printer/filament/process
* export mapping for printer/filament/process

## Fixture Tests

Add real fixture files from vendored Orca resources, copied into a narrow test
fixture set.

Test cases:

* simple standalone preset
* preset with one parent
* preset with multiple inheritance levels
* filament with nozzle-specific specialization
* malformed preset
* preset with unsupported keys only

## Round-Trip Tests

For supported fields:

1. build native profile
2. export to Orca JSON
3. import exported JSON
4. compare normalized native values

This should be exact for the supported field subset.

## UI / Workflow Recommendation

## Import UX

Suggested user flow:

1. user taps `Import Orca Profile`
2. user selects file or folder
3. app parses and resolves preset
4. app shows preview:
   * name
   * detected type
   * mapped fields
   * unsupported fields count
   * warnings
5. user confirms import
6. app stores resulting native profile

Do not auto-apply imported profile without confirmation.

## Export UX

Suggested user flow:

1. user opens a native printer / filament / process profile
2. user taps `Export to Orca`
3. app chooses flattened export by default
4. app generates JSON file
5. app shows export summary and share/save action

Optional later toggle:

* `Use inherits base preset`

Default should stay off.

## Implementation Plan

Recommended implementation order:

1. Add internal Kotlin parser models for raw/resolved Orca presets.
2. Add inheritance resolver.
3. Add explicit import mapping tables for the current supported field subset.
4. Add import summary reporting.
5. Add flattened export mapping tables for the same field subset.
6. Add round-trip tests for supported fields.
7. Add UI entry points for import/export.
8. Expand mappings as `README/SETTINGS_CHECKLIST.md` coverage expands.

## Practical Recommendation

The best fit for this codebase is:

* import Orca presets by resolving and flattening them, then rewriting them
  into native app profiles
* export native app profiles as flattened Orca preset JSON first
* treat inheritance-based export as an optional later refinement
* keep unsupported fields visible in reports instead of hiding them

That approach is:

* aligned with the repo's app-owned profile architecture
* compatible with the current reduced Android runtime
* easier to test
* honest about partial coverage
* progressively better as native Orca-style settings coverage expands

## Generated Printer Process Bundles

MobileSlicer builds its Orca printer picker from generated assets under
`orca-printers/`. The generator must treat Orca `process/*.json` files as
first-class inputs, not just machine JSON, because a printer import also seeds
the compatible process presets for that exact model/nozzle.

Prusa profiles require two compatibility paths:

* Older profiles may use `compatible_printers`, for example `Prusa MK4 0.4
  nozzle`.
* Newer Prusa CORE One / MK4S profiles often omit `compatible_printers` and
  encode the target in the process name, for example
  `0.20mm SPEED @CORE One 0.4` or `0.20mm SPEED @MK4S HF0.4`.

The asset generator therefore parses the process-name suffix after `@`, matches
it against the resolved machine `printer_model` / `name`, and requires the
suffix nozzle to match the machine nozzle. This mirrors Orca's shipped profile
layout without inventing app-only process presets. As of the 2026-04-27 audit,
all generated Prusa printer bundles have at least one compatible process preset;
CORE One, CORE One HF/L, MK4S, and MK4S HF no longer generate empty process
lists.

The same audit also found one non-Prusa empty bundle:
`Wanhao France / D12 500 PRO M2 DIRECT`. Orca's nozzle machine file is named
for `D12 500 PRO M2 DIRECT 0.4 nozzle`, but its `printer_model` points at
`D12 500 PRO SMARTPAD DIRECT`. The generator now accepts nozzle-machine files
whose own `name` matches the machine model after removing the nozzle suffix, so
that Orca profile inconsistency still imports the correct process presets. After
that fallback, the generated printer manifest has zero empty process bundles.

## Summary

Import is harder than export because Orca source presets can be arbitrary,
inherited, and richer than the current app model.

Export is easier because MobileSlicer already controls its native schema.

The correct long-term architecture is not "run Orca presets directly inside the
app." It is:

* build a robust translator
* keep MobileSlicer's native profile model as the source of truth
* widen the mapping as app-native Orca-style settings coverage grows

## Recommended Timing

This should not be treated as an immediate implementation priority while native
settings coverage is still materially incomplete.

Recommended sequencing:

1. continue expanding native `Printer` / `Filament` / `Process` coverage first
2. keep `README/SETTINGS_CHECKLIST.md` accurate as the source of truth for what
   the app really supports
3. wait until native coverage is broad enough that Orca import/export would be
   mostly translational instead of heavily lossy
4. implement Orca import/export against that broader native surface

Reason:

* import becomes less lossy
* export needs fewer caveats
* fewer Orca keys remain unsupported
* native -> Orca -> native round-tripping becomes more predictable

Until that broader settings milestone is reached, this document should be
treated as architectural guidance and implementation preparation, not as a
signal to start building the feature immediately.
