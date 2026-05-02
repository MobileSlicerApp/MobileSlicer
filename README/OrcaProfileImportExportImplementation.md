# Orca Profile Import / Export Implementation Blueprint

## Purpose

This document turns `README/OrcaProfileImportExport.md` into a concrete,
code-shaped implementation plan for MobileSlicer.

It answers:

* what Kotlin files to add
* what classes and interfaces they should contain
* how Orca preset parsing and inheritance resolution should work
* how mapping should plug into the existing native profile model
* how import/export should integrate with current app persistence

This is still a design document, but it is intentionally close to code.

## Design Principles

The implementation should follow these rules:

* MobileSlicer profile persistence remains the source of truth for user state
* imported Orca printers replace the old curated built-in printer list
* only manually-created printers are `Custom`
* Orca presets are an interchange format, not the app's runtime model
* imported Orca printer profiles must preserve raw and resolved Orca data so
  slicing can become exact instead of lossy
* parsing, resolving, mapping, and UI should be separate layers
* import/export must produce diagnostics for partial coverage
* explicit field maps are preferred over reflection or magic string plumbing

## Recommended Package Layout

Recommended new package:

* `com.mobileslicer.profiles.orca`

Recommended files:

* `OrcaPresetTypes.kt`
* `OrcaPresetParser.kt`
* `OrcaPresetResolver.kt`
* `OrcaPresetLookup.kt`
* `OrcaPrinterCatalog.kt`
* `OrcaPrinterImporter.kt`
* `OrcaImportReport.kt`
* `OrcaExportReport.kt`
* `OrcaImportMappings.kt`
* `OrcaExportMappings.kt`
* `OrcaProfileImporter.kt`
* `OrcaProfileExporter.kt`
* `OrcaPrintableArea.kt`
* `OrcaEnumNormalizers.kt`
* `OrcaProfileIntegration.kt`

If the package grows too large, split by domain:

* `com.mobileslicer.profiles.orca.model`
* `com.mobileslicer.profiles.orca.parse`
* `com.mobileslicer.profiles.orca.resolve`
* `com.mobileslicer.profiles.orca.map`
* `com.mobileslicer.profiles.orca.io`

## Existing Integration Points

Current profile ownership lives in:

* `android-app/app/src/main/java/com/mobileslicer/profiles/ProfileModels.kt`

Current persistence lives in:

* `ProfileStoreRepository`

Current selected configuration flattening lives in:

* `ActiveSlicerConfiguration.toNativeSliceConfigJson()`

Import/export should integrate with those, not replace them.

The current `Select Printer` page reads a generated Orca printer catalog from
vendored `machine_model` entries and cover PNGs. Selecting a catalog row now
imports the requested printer/nozzle variant into `ProfileStore` and imports
the compatible Orca process presets for that variant.

## Accepted Printer Import Behavior

Fresh install behavior:

* the visible Printer Profiles list should start with no imported printers
* no QIDI/Prusa/Voron-style curated built-ins should appear as defaults
* `Select Printer` is how normal Orca printers enter the list
* `New Custom Printer` is the only path for a manual custom printer

Imported printer card behavior:

* use the Orca cover image where the current letter tile appears
* show the printer name
* show available nozzle diameters
* do not show a `Built-in` badge
* do not show an `Orca` source badge on the added printer card

Initial import must include:

* `name`
* `family` / vendor
* thumbnail asset or persisted image path
* Orca `machine_model` path
* raw `machine_model` JSON
* all matching nozzle variant machine preset paths and raw JSON
* resolved inheritance chains for the machine model and nozzle presets
* available nozzle diameter list
* bed width/depth derived from resolved `printable_area`
* max height derived from resolved `printable_height`
* every key from the selected nozzle's fully resolved Orca machine preset
  carried into the native slice config

The bed-size import is mandatory because the selected printer already drives
the workspace bed. If `printable_area` is a polygon, the initial native display
cache may derive rectangular bounds, but the original polygon must remain in
the preserved Orca payload.

## Active Printer Mapping Contract

The current Android implementation uses a non-lossy base layer for printer
data:

* `scripts/generate_orca_printer_assets.py` builds one lightweight catalog row
  per Orca `machine_model`.
* Each catalog row points at a per-printer import bundle.
* The import bundle preserves:
  * raw `machine_model` JSON
  * raw nozzle-specific `machine` JSON files
  * fully resolved nozzle-specific machine JSON files
  * resolved source chains
* When the user imports a printer, MobileSlicer maps known Orca printer keys
  into first-class `PrinterProfile` fields for UI, persistence, bed display,
  and editing.
* `ActiveSlicerConfiguration.toNativeSliceConfigJson()` starts from the full
  resolved Orca machine JSON before writing MobileSlicer first-class values
  over it. This means printer keys that do not yet have dedicated Android UI
  fields still travel into the slicer config instead of being dropped.

For printer data, "mapped" therefore has two levels:

1. First-class mapping: the Orca key has an explicit `PrinterProfile` field and
   can be shown/edited by MobileSlicer.
2. Full config mapping: the Orca key remains present in the resolved printer
   config passed to slicing, even if MobileSlicer has no dedicated control yet.

This is the required behavior for Orca printer imports. A future refactor may
move the import code into `com.mobileslicer.profiles.orca`, but it must preserve
the same contract: no resolved Orca printer key may be discarded.

Printer/nozzle/process relationship:

* A visible imported printer profile represents one Orca printer/nozzle variant,
  not only the abstract printer model.
* If an Orca printer has multiple nozzle sizes, `Select Printer` prompts for the
  nozzle before importing.
* The import bundle includes Orca process presets compatible with each
  nozzle-specific machine name from `compatible_printers`.
* Importing a printer/nozzle imports that variant's process presets immediately;
  the Process tab shows those presets directly, with no local `Built-in` badge
  and no separate hidden picker step.
* Changing the selected printer/nozzle changes the visible Process list to the
  processes owned by that imported printer profile.
* The `Process Presets` button opens an immutable generated-asset library for
  the selected printer/nozzle. Tapping a preset imports a separate editable user
  copy; user edits and renames do not mutate the library entry.
* The active-selection summary only shows process text when the selected process
  belongs to the selected printer. If no valid printer is selected, stale
  process text is suppressed.
* Deleting the selected printer clears or rebinds the selected process to a
  valid remaining printer/process pair.
* Printer, filament, and process profile cards use `Duplicate` for the copy
  action label in the current mobile card layout.

Imported process preservation mirrors printer and filament preservation:

* raw process JSON
* fully resolved process JSON
* resolved source chain
* source family
* selected printer profile id
* selected nozzle diameter

`ActiveSlicerConfiguration.toNativeSliceConfigJson()` merges resolved Orca
process JSON after printer and filament JSON, then applies first-class
MobileSlicer fields. This keeps all Orca process data available to slicing even
before every process key has a dedicated Android control.

## Active Filament Mapping Contract

The current filament picker is intentionally narrower than the full Orca
filament library:

* `Select Filament` exposes only generic Orca filament material presets for now.
* Display names remove the `Generic` prefix and show material names such as
  `ABS`, `PLA`, `PETG`, `TPU`, and `PA-CF`.
* Vendor-specific and printer/nozzle-specific filament variants are not shown in
  the first picker pass.
* The picker is flat; it does not show Orca source-family group headings.
* Imported filament cards do not show a `Built-in` badge or source badge.
* Fresh profile state does not show local default filament cards; users must
  import a filament or create a custom filament.
* Each import bundle preserves:
  * raw filament JSON
  * fully resolved filament JSON
  * resolved source chain
* When the user imports a filament, MobileSlicer maps known Orca filament keys
  into first-class `FilamentProfile` fields.
* `ActiveSlicerConfiguration.toNativeSliceConfigJson()` merges the resolved
  Orca filament JSON into the native slice config before applying first-class
  MobileSlicer filament values. Orca filament keys without dedicated Android UI
  fields therefore remain available to slicing instead of being dropped.

The generic-only scope is a product choice, not a data limitation. If the app
later exposes vendor-specific filament imports, the same raw/resolved
preservation rule applies.

## Profile-Required Model Import Guard

Model import is blocked until the required profile domains exist in visible
profile state:

* no printer and no filament:
  * `No printer or filament selected, please go to profiles and select a printer and filament.`
* no printer only:
  * `No printer selected, please go to profiles and select a printer.`
* no filament only:
  * `No filament selected, please go to profiles and select a filament.`
* no process for the selected printer/nozzle:
  * `No process selected, please go to profiles and select a process.`

This guard applies before launching the Android document picker from Home and
before adding another model from Workspace.

## File-Level Blueprint

## 1. `OrcaPresetTypes.kt`

Purpose:

* define the internal data types used by parsing/resolution/mapping

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

import org.json.JSONObject

internal enum class OrcaPresetType {
    Printer,
    Filament,
    Process,
    Unknown
}

internal data class OrcaRawPreset(
    val presetId: String,
    val fileName: String,
    val displayName: String?,
    val presetType: OrcaPresetType,
    val json: JSONObject
)

internal data class OrcaResolvedPreset(
    val presetId: String,
    val fileName: String,
    val displayName: String?,
    val presetType: OrcaPresetType,
    val sourceChain: List<String>,
    val flattenedValues: Map<String, Any?>,
    val metadata: Map<String, Any?>,
    val unresolvedParents: List<String> = emptyList()
)
```

Notes:

* `presetId` should be stable inside one import session
* `displayName` is user-facing
* `sourceChain` should preserve inheritance ancestry in resolved order

Add printer-import-specific metadata types when implementation starts:

```kotlin
internal data class OrcaPrinterCatalogEntry(
    val name: String,
    val family: String,
    val nozzleDiameters: List<Float>,
    val machineModelPath: String,
    val coverAssetPath: String?
)

internal data class OrcaImportedPrinterBundle(
    val catalogEntry: OrcaPrinterCatalogEntry,
    val machineModel: OrcaRawPreset,
    val nozzleMachines: List<OrcaRawPreset>,
    val resolvedNozzleMachines: List<OrcaResolvedPreset>,
    val bedWidthMm: Float,
    val bedDepthMm: Float,
    val maxHeightMm: Float
)
```

These types should feed the persisted `PrinterProfile` extension for imported
Orca printers.

## 2. `OrcaPresetLookup.kt`

Purpose:

* abstract where presets come from
* let the resolver query vendored Orca presets, imported bundles, and parent
  presets referenced through `inherits`

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

internal interface OrcaPresetLookup {
    fun findByReference(reference: String, expectedType: OrcaPresetType? = null): OrcaRawPreset?
}

internal class CompositeOrcaPresetLookup(
    private val delegates: List<OrcaPresetLookup>
) : OrcaPresetLookup {
    override fun findByReference(reference: String, expectedType: OrcaPresetType?): OrcaRawPreset? =
        delegates.firstNotNullOfOrNull { it.findByReference(reference, expectedType) }
}
```

Implementation variants:

* `BundlePresetLookup`
* `VendoredPresetLookup`
* `StaticTemplatePresetLookup`

## 3. `OrcaPresetParser.kt`

Purpose:

* parse file contents into `OrcaRawPreset`
* detect preset type
* split metadata from config later in resolver

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

import org.json.JSONObject

internal interface OrcaPresetParser {
    fun parse(fileName: String, body: String): OrcaRawPreset
}
```

Suggested concrete class:

```kotlin
internal class JsonOrcaPresetParser : OrcaPresetParser {
    override fun parse(fileName: String, body: String): OrcaRawPreset {
        val json = JSONObject(body)
        val type = detectPresetType(fileName, json)
        val displayName = json.optString("name").ifBlank { null }
        return OrcaRawPreset(
            presetId = buildPresetId(fileName, type, displayName),
            fileName = fileName,
            displayName = displayName,
            presetType = type,
            json = json
        )
    }
}
```

Helper functions in this file:

* `detectPresetType(fileName, json)`
* `buildPresetId(fileName, type, displayName)`

Detection logic should follow the design doc:

* explicit type if present
* path heuristics
* key heuristics

## 4. `OrcaPresetResolver.kt`

Purpose:

* resolve `inherits`
* flatten parent chains
* separate metadata keys from config keys

Suggested interface:

```kotlin
package com.mobileslicer.profiles.orca

internal interface OrcaPresetResolver {
    fun resolve(
        root: OrcaRawPreset,
        lookup: OrcaPresetLookup
    ): OrcaResolvedPreset
}
```

Suggested concrete class:

```kotlin
internal class DefaultOrcaPresetResolver : OrcaPresetResolver {
    override fun resolve(root: OrcaRawPreset, lookup: OrcaPresetLookup): OrcaResolvedPreset {
        val visited = linkedSetOf<String>()
        val chain = mutableListOf<OrcaRawPreset>()
        var current: OrcaRawPreset? = root
        val unresolvedParents = mutableListOf<String>()

        while (current != null) {
            if (!visited.add(current.presetId)) {
                error("Orca preset inheritance loop detected for ${current.presetId}")
            }
            chain += current

            val parentRef = current.json.optString("inherits").ifBlank { null }
            current = if (parentRef == null) {
                null
            } else {
                lookup.findByReference(parentRef, current.presetType).also {
                    if (it == null) unresolvedParents += parentRef
                }
            }

            if (parentRef != null && current == null) {
                break
            }
        }

        val flattenedMetadata = linkedMapOf<String, Any?>()
        val flattenedValues = linkedMapOf<String, Any?>()

        chain.asReversed().forEach { raw ->
            raw.json.keys().forEach { key ->
                val value = raw.json.opt(key)
                if (isMetadataKey(key)) {
                    flattenedMetadata[key] = value
                } else {
                    flattenedValues[key] = value
                }
            }
        }

        return OrcaResolvedPreset(
            presetId = root.presetId,
            fileName = root.fileName,
            displayName = root.displayName,
            presetType = root.presetType,
            sourceChain = chain.map { it.presetId }.asReversed(),
            flattenedValues = flattenedValues,
            metadata = flattenedMetadata,
            unresolvedParents = unresolvedParents
        )
    }
}
```

Required helper:

* `isMetadataKey(key: String): Boolean`

That key set should mirror the exclusions already called out in
`README/SETTINGS_CHECKLIST.md`.

## 5. `OrcaImportReport.kt`

Purpose:

* standardize import diagnostics

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

internal data class OrcaImportReport(
    val sourcePresetId: String,
    val presetType: OrcaPresetType,
    val sourceChain: List<String>,
    val mappedKeys: List<String>,
    val ignoredMetadataKeys: List<String>,
    val unsupportedKeys: List<String>,
    val warnings: List<String>,
    val partial: Boolean
)

internal data class OrcaImportResult<T>(
    val profile: T?,
    val report: OrcaImportReport
)
```

This is more useful than a bare list of warnings because it is UI- and test-
friendly.

## 6. `OrcaExportReport.kt`

Purpose:

* standardize export diagnostics

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

import org.json.JSONObject

internal data class OrcaExportReport(
    val presetType: OrcaPresetType,
    val mappedKeys: List<String>,
    val skippedKeys: List<String>,
    val warnings: List<String>
)

internal data class OrcaExportResult(
    val fileName: String,
    val presetType: OrcaPresetType,
    val json: JSONObject,
    val report: OrcaExportReport
)
```

## 7. `OrcaPrintableArea.kt`

Purpose:

* isolate printable-area parsing and reconstruction logic

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

internal data class OrcaBedBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float
) {
    val widthMm: Float get() = maxX - minX
    val depthMm: Float get() = maxY - minY
}

internal interface OrcaPrintableAreaParser {
    fun parse(value: Any?): OrcaBedBounds?
}

internal interface OrcaPrintableAreaFormatter {
    fun formatRectangular(widthMm: Float, depthMm: Float): String
}
```

Suggested parser behavior:

* accept Orca-style polygon string
* parse XY pairs
* compute bounds
* optionally expose whether the bed was non-rectangular

Suggested formatter output:

```text
-110x-110,110x-110,110x110,-110x110
```

with numbers derived from width/depth.

## 8. `OrcaEnumNormalizers.kt`

Purpose:

* normalize Orca enum strings into current app enums and back

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

import com.mobileslicer.ProcessSeamPosition
import com.mobileslicer.SparseInfillPattern
import com.mobileslicer.TopSurfacePattern

internal object OrcaEnumNormalizers {
    fun toSeamPosition(value: String?): ProcessSeamPosition?
    fun toTopSurfacePattern(value: String?): TopSurfacePattern?
    fun toSparseInfillPattern(value: String?): SparseInfillPattern?

    fun fromSeamPosition(value: ProcessSeamPosition): String = value.configValue
    fun fromTopSurfacePattern(value: TopSurfacePattern): String = value.configValue
    fun fromSparseInfillPattern(value: SparseInfillPattern): String = value.configValue
}
```

Rules:

* trim whitespace
* normalize case
* support aliases only when explicitly known
* return `null` for unsupported modes

Do not silently choose a random fallback in the normalizer itself. Let the
mapping layer decide whether to default and warn.

## 9. `OrcaImportMappings.kt`

Purpose:

* hold explicit key-by-key import logic from resolved Orca values into app
  profile drafts

Recommended pattern:

```kotlin
package com.mobileslicer.profiles.orca

internal data class NativePrinterDraft(
    var name: String = "Imported printer",
    var subtitle: String = "Imported from Orca",
    var bedWidthMm: Float = 220f,
    var bedDepthMm: Float = 220f,
    var maxHeightMm: Float = 220f,
    var nozzleDiameterMm: Float = 0.4f,
    var filamentDiameterMm: Float = 1.75f
)
```

Equivalent drafts:

* `NativeFilamentDraft`
* `NativeProcessDraft`

Suggested mapping abstraction:

```kotlin
internal data class ImportMappingContext(
    val preset: OrcaResolvedPreset,
    val mappedKeys: MutableList<String>,
    val warnings: MutableList<String>,
    val unsupportedKeys: MutableSet<String>
)

internal data class ImportFieldMapping<T>(
    val key: String,
    val apply: (T, Any?, ImportMappingContext) -> Unit
)
```

Suggested registry shape:

```kotlin
internal object OrcaImportMappings {
    val printerMappings: List<ImportFieldMapping<NativePrinterDraft>>
    val filamentMappings: List<ImportFieldMapping<NativeFilamentDraft>>
    val processMappings: List<ImportFieldMapping<NativeProcessDraft>>
}
```

Example printer mapping:

```kotlin
ImportFieldMapping<NativePrinterDraft>("printable_height") { draft, value, ctx ->
    val height = value.asFloatOrNull()
    if (height != null && height > 0f) {
        draft.maxHeightMm = height
        ctx.mappedKeys += "printable_height"
    } else {
        ctx.warnings += "Ignored invalid printable_height value."
    }
}
```

Special multi-key mapping like `printable_area` should not be forced into a
pure single-key abstraction. It is acceptable to have a dedicated import
function for compound fields.

## 10. `OrcaExportMappings.kt`

Purpose:

* hold explicit mapping from native app profiles to Orca JSON

Suggested abstraction:

```kotlin
package com.mobileslicer.profiles.orca

import org.json.JSONObject

internal data class ExportMappingContext(
    val mappedKeys: MutableList<String>,
    val skippedKeys: MutableList<String>,
    val warnings: MutableList<String>
)

internal data class ExportFieldMapping<T>(
    val apply: (T, JSONObject, ExportMappingContext) -> Unit
)
```

Suggested registry shape:

```kotlin
internal object OrcaExportMappings {
    val printerMappings: List<ExportFieldMapping<com.mobileslicer.PrinterProfile>>
    val filamentMappings: List<ExportFieldMapping<com.mobileslicer.FilamentProfile>>
    val processMappings: List<ExportFieldMapping<com.mobileslicer.ProcessProfile>>
}
```

Example export mapping:

```kotlin
ExportFieldMapping<com.mobileslicer.FilamentProfile> { profile, json, ctx ->
    json.put("filament_type", profile.materialType)
    ctx.mappedKeys += "filament_type"
}
```

## 11. `OrcaProfileImporter.kt`

Purpose:

* turn resolved Orca presets into native app profiles

Suggested interface:

```kotlin
package com.mobileslicer.profiles.orca

import com.mobileslicer.FilamentProfile
import com.mobileslicer.PrinterProfile
import com.mobileslicer.ProcessProfile

internal interface OrcaProfileImporter {
    fun importPrinter(preset: OrcaResolvedPreset): OrcaImportResult<PrinterProfile>
    fun importFilament(preset: OrcaResolvedPreset): OrcaImportResult<FilamentProfile>
    fun importProcess(preset: OrcaResolvedPreset): OrcaImportResult<ProcessProfile>
}
```

Suggested implementation flow:

1. validate `presetType`
2. initialize native draft
3. apply compound mappings first
4. apply per-field mappings
5. compute unsupported keys as:
   * all flattened config keys
   * minus mapped keys
6. build native profile object
7. validate native values
8. return `OrcaImportResult`

Suggested concrete class:

```kotlin
internal class DefaultOrcaProfileImporter(
    private val printableAreaParser: OrcaPrintableAreaParser
) : OrcaProfileImporter
```

### Example: Printer Import Shape

```kotlin
override fun importPrinter(preset: OrcaResolvedPreset): OrcaImportResult<PrinterProfile> {
    require(preset.presetType == OrcaPresetType.Printer)

    val mappedKeys = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val ignoredMetadataKeys = preset.metadata.keys.sorted()
    val unsupported = preset.flattenedValues.keys.toMutableSet()

    val draft = NativePrinterDraft(
        name = preset.displayName ?: "Imported printer",
        subtitle = "Imported from Orca"
    )

    preset.flattenedValues["printable_area"]?.let { rawArea ->
        val bounds = printableAreaParser.parse(rawArea)
        if (bounds != null) {
            draft.bedWidthMm = bounds.widthMm
            draft.bedDepthMm = bounds.depthMm
            mappedKeys += "printable_area"
            unsupported.remove("printable_area")
        } else {
            warnings += "Unable to parse printable_area."
        }
    }

    val ctx = ImportMappingContext(
        preset = preset,
        mappedKeys = mappedKeys,
        warnings = warnings,
        unsupportedKeys = unsupported
    )

    OrcaImportMappings.printerMappings.forEach { mapping ->
        val value = preset.flattenedValues[mapping.key]
        if (value != null) {
            mapping.apply(draft, value, ctx)
            unsupported.remove(mapping.key)
        }
    }

    val profile = PrinterProfile(
        id = "printer_${java.util.UUID.randomUUID()}",
        name = draft.name,
        subtitle = draft.subtitle,
        builtIn = false,
        bedWidthMm = draft.bedWidthMm,
        bedDepthMm = draft.bedDepthMm,
        maxHeightMm = draft.maxHeightMm,
        nozzleDiameterMm = draft.nozzleDiameterMm,
        filamentDiameterMm = draft.filamentDiameterMm
    )

    validatePrinterProfile(profile)

    return OrcaImportResult(
        profile = profile,
        report = OrcaImportReport(
            sourcePresetId = preset.presetId,
            presetType = preset.presetType,
            sourceChain = preset.sourceChain,
            mappedKeys = mappedKeys.sorted(),
            ignoredMetadataKeys = ignoredMetadataKeys,
            unsupportedKeys = unsupported.sorted(),
            warnings = warnings,
            partial = unsupported.isNotEmpty() || preset.unresolvedParents.isNotEmpty()
        )
    )
}
```

The filament and process versions should mirror this pattern.

## 12. `OrcaProfileExporter.kt`

Purpose:

* convert native app profiles into Orca-style preset JSON

Suggested interface:

```kotlin
package com.mobileslicer.profiles.orca

import com.mobileslicer.FilamentProfile
import com.mobileslicer.PrinterProfile
import com.mobileslicer.ProcessProfile

internal interface OrcaProfileExporter {
    fun exportPrinter(profile: PrinterProfile): OrcaExportResult
    fun exportFilament(profile: FilamentProfile): OrcaExportResult
    fun exportProcess(profile: ProcessProfile): OrcaExportResult
}
```

Suggested implementation:

```kotlin
internal class DefaultOrcaProfileExporter(
    private val printableAreaFormatter: OrcaPrintableAreaFormatter
) : OrcaProfileExporter
```

Suggested export flow:

1. create empty `JSONObject`
2. write minimal metadata
3. apply mapping registry
4. validate final JSON
5. choose output file name
6. return report

### Metadata Helper

Add internal helpers:

```kotlin
internal fun buildPrinterExportMetadata(profile: com.mobileslicer.PrinterProfile): JSONObject
internal fun buildFilamentExportMetadata(profile: com.mobileslicer.FilamentProfile): JSONObject
internal fun buildProcessExportMetadata(profile: com.mobileslicer.ProcessProfile): JSONObject
```

These should be conservative and based on real Orca examples, not guess-heavy.

## 13. `OrcaProfileIntegration.kt`

Purpose:

* provide app-facing orchestration helpers so UI code does not know about
  parser/resolver details

Suggested contents:

```kotlin
package com.mobileslicer.profiles.orca

import com.mobileslicer.ProfileStore

internal data class OrcaImportBundleResult(
    val importedPrinters: List<OrcaImportResult<com.mobileslicer.PrinterProfile>>,
    val importedFilaments: List<OrcaImportResult<com.mobileslicer.FilamentProfile>>,
    val importedProcesses: List<OrcaImportResult<com.mobileslicer.ProcessProfile>>
)

internal object OrcaProfileIntegration {
    fun addImportedProfiles(
        store: ProfileStore,
        result: OrcaImportBundleResult
    ): ProfileStore
}
```

Suggested behavior:

* append imported Orca printers to the user's visible printer list
* do not recreate the old curated built-in printer set
* auto-select the first imported printer only when the UI import flow asks for it
* keep manual custom printers distinct from imported Orca printers

## Helper Extensions

These can live in one of the above files or in a small utility file.

Useful parsing helpers:

```kotlin
internal fun Any?.asFloatOrNull(): Float?
internal fun Any?.asIntOrNull(): Int?
internal fun Any?.asBooleanOrNull(): Boolean?
internal fun Any?.asTrimmedStringOrNull(): String?
internal fun Any?.asPercentIntOrNull(): Int?
```

These should accept:

* `Number`
* numeric `String`
* percent strings like `"15%"`

Do not make them permissive to the point of hiding bad source data.

## Validation Functions

Recommended internal validators:

```kotlin
internal fun validatePrinterProfile(profile: com.mobileslicer.PrinterProfile)
internal fun validateFilamentProfile(profile: com.mobileslicer.FilamentProfile)
internal fun validateProcessProfile(profile: com.mobileslicer.ProcessProfile)
```

These should throw a domain-specific exception on invalid output:

```kotlin
internal class OrcaProfileValidationException(message: String) : IllegalArgumentException(message)
```

This is better than returning broken native profiles.

## Proposed Control Flow

## Single File Import Flow

```text
read file
-> parse into OrcaRawPreset
-> resolve inheritance using bundle + vendored lookups
-> import to native profile
-> validate
-> show report
-> persist on confirmation
```

## Bundle Import Flow

```text
read many files
-> parse into OrcaRawPreset list
-> build BundlePresetLookup
-> resolve each preset
-> import each preset by type
-> gather reports
-> show summary
-> persist on confirmation
```

## Export Flow

```text
pick native profile
-> export to OrcaExportResult
-> serialize JSONObject with indentation
-> offer save/share
-> show report
```

## Mapping Policy Recommendations

## Use Explicit Mappings, Not Generic Reflection

Recommended:

* one explicit mapping per supported field

Not recommended:

* auto-map by same-name strings
* reflection over data-class properties

Reason:

* too many fields require normalization, transformation, or warning generation

## Keep Compound Transformations Special

Examples:

* `printable_area`
* `fan_min_speed` + `fan_max_speed`
* `sparse_infill_density`

These should use dedicated functions rather than pretending they are plain
one-key copies.

## Unknown Keys Must Stay Visible

For import:

* unknown or unsupported config keys should remain in the report

For export:

* native fields without Orca equivalents should remain in the report

This is important for long-term trust.

## Where To Plug Into Current App Code

Recommended integration points in the existing codebase:

* keep `ProfileModels.kt` as the native model owner
* do not add Orca logic directly into `ProfileStoreRepository`
* do not add Orca parsing to `MainActivity.kt`

Instead:

* UI layer calls an import/export coordinator in the new Orca package
* the coordinator returns imported native profiles or exported JSON
* current app store persists the native profiles as usual

That keeps the architecture clean.

## Example UI-Facing Entry Points

Suggested thin orchestration functions:

```kotlin
internal class OrcaImportUseCase(
    private val parser: OrcaPresetParser,
    private val resolver: OrcaPresetResolver,
    private val importer: OrcaProfileImporter,
    private val vendoredLookup: OrcaPresetLookup
) {
    fun importFiles(files: List<Pair<String, String>>): OrcaImportBundleResult
}
```

```kotlin
internal class OrcaExportUseCase(
    private val exporter: OrcaProfileExporter
) {
    fun exportPrinter(profile: com.mobileslicer.PrinterProfile): OrcaExportResult
    fun exportFilament(profile: com.mobileslicer.FilamentProfile): OrcaExportResult
    fun exportProcess(profile: com.mobileslicer.ProcessProfile): OrcaExportResult
}
```

The UI can then stay simple:

* pick file
* call use case
* show report
* persist on approval

## Testing Blueprint

## Unit Tests

Test files to add later:

* `OrcaPresetParserTest.kt`
* `OrcaPresetResolverTest.kt`
* `OrcaPrintableAreaParserTest.kt`
* `OrcaEnumNormalizersTest.kt`
* `OrcaProfileImporterTest.kt`
* `OrcaProfileExporterTest.kt`
* `OrcaRoundTripTest.kt`

## Specific Test Cases

### Parser

* detects printer from machine path
* detects filament from keys
* rejects ambiguous file

### Resolver

* single parent chain
* multi-level chain
* missing parent
* loop detection
* child overrides parent

### Importer

* imports rectangular printable area
* imports process enums correctly
* collapses fan min/max with warning
* reports unsupported keys

### Exporter

* writes rectangular printable area
* writes current supported filament fields
* writes current supported process fields

### Round-Trip

* native printer -> Orca -> native
* native filament -> Orca -> native
* native process -> Orca -> native

For supported fields, values should match exactly after normalization.

## Recommended First Slice of Implementation

If implementing this in bounded steps, start with:

1. `OrcaPresetTypes.kt`
2. `OrcaPresetParser.kt`
3. `OrcaPresetResolver.kt`
4. `OrcaPrintableArea.kt`
5. `OrcaImportReport.kt`
6. `OrcaImportMappings.kt`
7. `OrcaProfileImporter.kt`

That is enough to support read-only import parsing and native profile creation.

Second slice:

1. `OrcaExportReport.kt`
2. `OrcaExportMappings.kt`
3. `OrcaProfileExporter.kt`

Third slice:

1. integration/use-case layer
2. UI entry points
3. persistence wiring

## Final Recommendation

The recommended implementation shape is:

* parser
* resolver
* explicit mapping registries
* importer/exporter services
* thin app integration layer

This keeps the current app architecture intact while making Orca import/export
realistic, testable, and expandable as native settings coverage grows.

## Recommended Timing

This implementation blueprint is intentionally for later use, not for immediate
execution while the app still lacks large parts of the intended Orca-style
native settings surface.

Recommended sequencing:

1. finish broad native settings expansion first
2. keep the native profile model and `README/SETTINGS_CHECKLIST.md` aligned
3. revisit this blueprint once import/export would be mapping a mostly complete
   native surface instead of a narrow partial subset

Why defer:

* the import mapper will otherwise be excessively lossy
* export would need too many temporary omissions and warnings
* round-trip expectations would be weak until the native model covers much more
  of Orca's real preset surface

So this document should be read as an implementation-ready reference for the
future broader-settings phase, not as a recommendation to begin coding the
feature now.
