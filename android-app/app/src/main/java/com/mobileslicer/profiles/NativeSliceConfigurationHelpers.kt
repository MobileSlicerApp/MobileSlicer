package com.mobileslicer.profiles

import org.json.JSONObject
import org.json.JSONArray

internal fun JSONObject.restoreResolvedOrcaParityValues(
    printerJson: JSONObject?,
    filamentJson: JSONObject?,
    processJson: JSONObject?
) {
    copyResolvedValues(printerJson, resolvedPrinterParityKeys)
    copyResolvedValues(filamentJson, resolvedFilamentParityKeys)
    copyResolvedValues(processJson, resolvedProcessParityKeys)
    processJson?.opt(NativeConfigKeys.Compatibility.CompatiblePrinters)?.let { compatiblePrinters ->
        put(NativeConfigKeys.Compatibility.PrintCompatiblePrinters, compatiblePrinters)
    }
    filamentJson?.scalarString(NativeConfigKeys.Filament.Id)
        ?.takeIf { it.isNotBlank() && !hasNonBlankNativeScalar(NativeConfigKeys.Filament.Ids) }
        ?.let { filamentId ->
        put(NativeConfigKeys.Filament.Ids, filamentId)
    }
    if (optString(NativeConfigKeys.Bed.Shape).isBlank()) {
        printerJson?.opt(NativeConfigKeys.Bed.PrintableArea).toNativeConfigListString()?.let {
            put(NativeConfigKeys.Bed.Shape, it)
        }
    }
    val resolvedDefaultBedType = normalizedOrcaBedTypeName(printerJson?.scalarString(NativeConfigKeys.Bed.DefaultType).orEmpty())
    if (resolvedDefaultBedType in knownOrcaBedTypes &&
        printerJson?.hasNonBlankNativeScalar(NativeConfigKeys.Bed.CurrentType) != true &&
        scalarString(NativeConfigKeys.Bed.CurrentType).isBlank()
    ) {
        put(NativeConfigKeys.Bed.CurrentType, resolvedDefaultBedType)
    }
}

private fun JSONObject.copyResolvedValues(source: JSONObject?, keys: Set<String>) {
    if (source == null) return
    keys.forEach { key ->
        if (source.has(key) && !source.isNull(key)) {
            if (source.nativeResolvedString(key).isBlank()) {
                if (key in resolvedProfileIdentityKeys) {
                    return@forEach
                }
                remove(key)
                return@forEach
            }
            put(key, source.opt(key))
        }
    }
}

private fun JSONObject.hasNonBlankNativeScalar(key: String): Boolean =
    when (val value = opt(key)) {
        is JSONArray -> firstScalarString(value)?.isNotBlank() == true
        null -> false
        else -> value.toString().trim().trim('"').isNotBlank()
    }

internal fun JSONObject.applyOrcaProfileIdentityDefaults(
    printer: PrinterProfile,
    filament: FilamentProfile,
    process: ProcessProfile
) {
    val hasOrcaPrinterContext = printer.hasOrcaContext()
    val hasOrcaFilamentContext = filament.hasOrcaContext() || hasOrcaFilamentIdentity()
    val hasOrcaProcessContext = process.hasOrcaContext()
    if (hasOrcaPrinterContext && shouldUseExportedPrinterSettingsId(printer)) {
        put(NativeConfigKeys.Printer.SettingsId, resolvedPrinterSettingsIdForExport(printer))
    }
    if (hasOrcaFilamentContext && scalarString(NativeConfigKeys.Filament.SettingsId).isBlank()) {
        put(NativeConfigKeys.Filament.SettingsId, filament.orcaFilamentPath.orcaProfileNameFromPath().ifBlank { filament.name })
    }
    if (hasOrcaFilamentContext && !hasNonBlankNativeScalar(NativeConfigKeys.Filament.Ids)) {
        scalarString(NativeConfigKeys.Filament.Id).takeIf { it.isNotBlank() }?.let { put(NativeConfigKeys.Filament.Ids, it) }
    }
    if (hasOrcaFilamentContext) {
        applyOrcaFilamentIdentityDefaults(filament)
        val currentColor = scalarString(NativeConfigKeys.Filament.Color)
            .ifBlank { scalarString(NativeConfigKeys.Filament.DefaultColor) }
        val fallbackColor = filament.orcaDefaultColorFallback()
        val color = if (currentColor.isBlank() || currentColor.equals("#8FC1FF", ignoreCase = true)) {
            fallbackColor.ifBlank { filament.defaultFilamentColor }.ifBlank { currentColor }
        } else {
            currentColor
        }
        if (color.isNotBlank()) {
            put(NativeConfigKeys.Filament.Color, color)
            put(NativeConfigKeys.Filament.MultiColor, color)
            if (scalarString(NativeConfigKeys.Filament.ColorType).isBlank()) {
                put(NativeConfigKeys.Filament.ColorType, 1)
            }
        }
    }
    if (hasOrcaProcessContext && optString(NativeConfigKeys.Process.SettingsId).isBlank()) {
        put(NativeConfigKeys.Process.SettingsId, process.orcaProcessPath.orcaProfileNameFromPath().ifBlank { process.name })
    }
    if (hasOrcaFilamentContext) {
        putIfBlankOrZero("pressure_advance", "0.02")
        applyOrcaFilamentTemplateDefaults(filament)
        applyOrcaCurrentBedTypeDefault(printer)
    }
    if (hasOrcaProcessContext || hasOrcaPrinterContext) {
        applyOrcaProcessTemplateDefaults(printer, process)
    }
    if (hasOrcaPrinterContext) {
        applyOrcaPrinterHardwareDefaults(printer)
        expandOrcaMachineLimitVectors()
    }
}

internal fun JSONObject.applyFinalOrcaParityNormalization(
    printer: PrinterProfile,
    process: ProcessProfile
) {
    if (printer.hasOrcaContext() || process.hasOrcaContext() || hasBambuContext()) {
        applyOrcaProcessTemplateDefaults(printer, process)
    }
    if (printer.hasOrcaContext()) {
        applyOrcaPrinterHardwareDefaults(printer)
        expandOrcaMachineLimitVectors()
        applyOrcaCurrentBedTypeDefault(printer)
    }
}

private fun PrinterProfile.hasOrcaContext(): Boolean =
    profileSource == "orca" || orcaMachineModelPath.isNotBlank() || orcaFamily.isNotBlank() ||
        orcaResolvedMachineJson.isNotBlank() || orcaMachineOverridesJson.isNotBlank()

private fun FilamentProfile.hasOrcaContext(): Boolean =
    profileSource == "orca" || orcaFilamentPath.isNotBlank() || orcaFamily.isNotBlank() ||
        orcaResolvedFilamentJson.isNotBlank() || orcaFilamentOverridesJson.isNotBlank()

private fun ProcessProfile.hasOrcaContext(): Boolean =
    profileSource == "orca" || orcaProcessPath.isNotBlank() || orcaFamily.isNotBlank() ||
        orcaResolvedProcessJson.isNotBlank() || orcaProcessOverridesJson.isNotBlank()

private fun JSONObject.applyOrcaPrinterHardwareDefaults(printer: PrinterProfile) {
    if (printer.singleExtruderMultiMaterial && scalarString(NativeConfigKeys.Printer.ExtruderAmsCount).isBlank()) {
        put(NativeConfigKeys.Printer.ExtruderAmsCount, "1#0|4#0;1#0|4#0")
    }
    if (printer.hasQidiQ2Context()) {
        if (scalarString(NativeConfigKeys.Printer.ExtruderAmsCount).isBlank() ||
            scalarString(NativeConfigKeys.Printer.ExtruderAmsCount) == "1#0|4#0;"
        ) {
            put(NativeConfigKeys.Printer.ExtruderAmsCount, "1#0|4#0;1#0|4#0")
        }
        putIfBlankOrValue(NativeConfigKeys.Process.WipeTowerType, "type1", "type2")
        putIfBlankOrValue(NativeConfigKeys.Process.WipeTowerX, "165", "15")
        putIfBlankOrValue(NativeConfigKeys.Process.WipeTowerX, "165", "221.5")
        putIfBlankOrValue(NativeConfigKeys.Process.WipeTowerX, "165", "221.500")
        putIfBlankOrValue(NativeConfigKeys.Process.WipeTowerY, "250", "220")
        putIfBlankOrValue(NativeConfigKeys.Process.WipeTowerY, "250", "13.5")
        putIfBlankOrValue(NativeConfigKeys.Process.WipeTowerY, "250", "13.500")
    }
    putIfBlankOrValue(NativeConfigKeys.Printer.MachineMaxJunctionDeviation, "0.01", "0.013")
    putIfBlankOrValue(NativeConfigKeys.Printer.MachineMaxJunctionDeviation, "0.01", "0.013000000268220901")
}

private fun JSONObject.shouldUseExportedPrinterSettingsId(printer: PrinterProfile): Boolean {
    val current = scalarString(NativeConfigKeys.Printer.SettingsId)
    return current.isBlank() ||
        current == "Qidi" ||
        current == printer.orcaFamily ||
        current == printer.name ||
        current == printer.printerModel ||
        (current.contains("MyToolChanger", ignoreCase = true) && !printer.orcaFamily.equals("Custom", ignoreCase = true))
}

private fun JSONObject.resolvedPrinterSettingsIdForExport(printer: PrinterProfile): String =
    printer.resolvedOrcaMachineName().ifBlank { printerSettingsIdForExport(printer) }

private fun PrinterProfile.resolvedOrcaMachineName(): String =
    runCatching { JSONObject(orcaResolvedMachineJson).optString("name") }
        .getOrNull()
        .orEmpty()
        .trim()

private fun JSONObject.applyOrcaFilamentTemplateDefaults(filament: FilamentProfile) {
    val material = resolvedOrcaFilamentMaterial(filament)
    if (material.equals("ABS", ignoreCase = true) || material.equals("ASA", ignoreCase = true)) {
        putIfBlankOrValue("fan_min_speed", "10", "30")
        putIfBlankOrValue("fan_min_speed", "10", "80")
    }
    if (material.equals("ABS", ignoreCase = true) ||
        material.equals("ASA", ignoreCase = true) ||
        material.equals("PLA", ignoreCase = true)
    ) {
        putIfBlankOrValue(NativeConfigKeys.Temperature.TexturedCoolPlate, "40", scalarString(NativeConfigKeys.Temperature.HotPlate))
        putIfBlankOrValue(NativeConfigKeys.Temperature.TexturedCoolPlateInitialLayer, "40", scalarString(NativeConfigKeys.Temperature.HotPlateInitialLayer))
    }
    putIfBlank(NativeConfigKeys.Filament.DefaultColor, "\"\"")
    putIfBlank("volumetric_speed_coefficients", "\"\"")
}

private fun JSONObject.applyOrcaFilamentIdentityDefaults(filament: FilamentProfile) {
    val material = resolvedOrcaFilamentMaterial(filament)
    if (material.isNotBlank()) {
        val currentType = scalarString(NativeConfigKeys.Filament.Type)
        if (currentType.isBlank() ||
            currentType == "0" ||
            currentType.equals("PLA", ignoreCase = true) && !material.equals("PLA", ignoreCase = true)
        ) {
            put(NativeConfigKeys.Filament.Type, material)
        }
    }
    val density = material.defaultFilamentDensity()
    if (density.isNotBlank()) {
        putIfBlankOrZero(NativeConfigKeys.Filament.Density, density)
    }
    val volumetricSpeed = material.defaultOrcaVolumetricSpeed()
    if (volumetricSpeed.isNotBlank()) {
        val currentSpeed = scalarString(NativeConfigKeys.Filament.MaxVolumetricSpeed).toDoubleOrNull()
        if (currentSpeed == null || currentSpeed <= 0.0 || currentSpeed == 2.0 && !material.equals("PLA", ignoreCase = true)) {
            put(NativeConfigKeys.Filament.MaxVolumetricSpeed, volumetricSpeed)
        }
    }
}

private fun JSONObject.hasOrcaFilamentIdentity(): Boolean =
    scalarString(NativeConfigKeys.Filament.SettingsId).isNotBlank() ||
        scalarString(NativeConfigKeys.Filament.Ids).isNotBlank() ||
        scalarString(NativeConfigKeys.Filament.Id).isNotBlank()

private fun JSONObject.resolvedOrcaFilamentMaterial(filament: FilamentProfile): String =
    sequenceOf(
        scalarString(NativeConfigKeys.Filament.SettingsId),
        scalarString(NativeConfigKeys.Filament.Ids),
        optString("name"),
        optString("inherits"),
        filament.orcaFilamentPath,
        filament.name,
        filament.materialType,
        scalarString(NativeConfigKeys.Filament.Type)
    )
        .mapNotNull { it.detectFilamentMaterial() }
        .firstOrNull()
        .orEmpty()

private fun String.defaultFilamentDensity(): String =
    when (uppercase(java.util.Locale.US)) {
        "PLA", "PLA+" -> "1.24"
        "PETG" -> "1.27"
        "ABS" -> "1.04"
        "ASA" -> "1.07"
        "TPU" -> "1.20"
        "PA", "PA-CF" -> "1.12"
        "PC" -> "1.20"
        "PLA-CF" -> "1.30"
        "PETG-CF" -> "1.30"
        else -> ""
    }

private fun String.defaultOrcaVolumetricSpeed(): String =
    when (uppercase(java.util.Locale.US)) {
        "ABS", "ASA" -> "17"
        "PETG", "PETG-CF" -> "12"
        "PLA", "PLA+" -> "12"
        "PLA-CF" -> "12"
        "TPU" -> "3.2"
        "PA", "PA-CF", "PC" -> "12"
        else -> ""
    }

internal fun JSONObject.removeUnresolvedMobileProcessDefaults(processJson: JSONObject?) {
    removeIfUnresolvedDefault(processJson, "brim_ears", "0", "false")
    removeIfUnresolvedDefault(processJson, "combine_brims", "0", "false")
    removeIfUnresolvedDefault(processJson, "support_object_elevation", "0", "0.0")
}

private fun JSONObject.applyOrcaProcessTemplateDefaults(printer: PrinterProfile, process: ProcessProfile) {
    putIfBlankOrValue("accel_to_decel_enable", "0", "1")
    putIfBlankOrValue("accel_to_decel_enable", "0", "true")
    putIfBlankOrValue("wall_direction", "auto", "ccw")
    putIfBlankOrValue("wall_direction", "auto", "clockwise")
    putIfBlankOrValue("wall_direction", "auto", "counter-clockwise")
    putIfBlankOrZero("bridge_density", "100%")
    putIfBlankOrZero("bridge_speed", "50")
    putIfBlankOrValue("filter_out_gap_fill", "0.5", "0")
    putIfBlankOrValue("filter_out_gap_fill", "0.5", "0.0")
    putIfBlankOrValue("gcode_label_objects", "0", "1")
    putIfBlankOrValue("gcode_label_objects", "0", "true")
    if (printer.printerAgent.equals("qidi", ignoreCase = true) || printer.orcaFamily.equals("Qidi", ignoreCase = true)) {
        putIfBlankOrZero("filter_out_gap_fill", "2")
        putIfBlankOrValue("filter_out_gap_fill", "2", "0.5")
    }
    if (printer.hasFlashforgeContext() || process.hasFlashforgeContext()) {
        putIfBlankOrValue("internal_solid_infill_acceleration", "7000", "100%")
        putIfBlankOrValue("wipe_speed", "200", "80%")
    }
    putIfBlankOrZero("initial_layer_min_bead_width", "85%")
    putIfBlankOrZero("internal_bridge_density", "100%")
    putIfBlankOrZero("internal_bridge_speed", "50")
    if (!process.resolvedOrcaProcessHasKey("max_bridge_length")) {
        putIfBlankOrZero("max_bridge_length", "10")
    }
    putIfBlankOrValue("exclude_object", "1", "0")
    putIfBlankOrValue("exclude_object", "1", "false")
    putIfBlankOrValue("ooze_prevention", "1", "0")
    putIfBlankOrValue("ooze_prevention", "1", "false")
    putIfBlankOrValue("prime_volume", "15", "45")
    putIfBlankOrValue("prime_volume", "15", "45.0")
    putIfBlankOrValue("wipe_tower_wall_type", "cone", "rectangle")
    putIfBlankOrValue("wipe_tower_cone_angle", "15", "30")
    putIfBlankOrValue("wipe_tower_cone_angle", "15", "30.0")
    putIfBlankOrValue("wipe_tower_x", "165", "15")
    putIfBlankOrValue("wipe_tower_x", "165", "15.0")
    putIfBlankOrValue("wipe_tower_y", "250", "220")
    putIfBlankOrValue("wipe_tower_y", "250", "220.0")
    putIfBlank("initial_layer_travel_speed", "100%")
    putIfBlankOrValue("internal_solid_infill_pattern", "monotonic", "monotonicline")
    putIfBlankOrZero("resolution", "0.001")
    putIfBlankOrZero("small_perimeter_speed", "50%")
    putIfBlankOrValue("small_perimeter_speed", "50%", "15")
    putIfBlankOrValue("small_perimeter_speed", "50%", "15.0")
    if (printer.printerAgent.equals("qidi", ignoreCase = true) || printer.orcaFamily.equals("Qidi", ignoreCase = true)) {
        putIfBlankOrZero("small_perimeter_threshold", "4")
    }
    putIfBlankOrZero("solid_infill_direction", "45")
    putIfBlankOrValue("sparse_infill_acceleration", "100%", "500")
    putIfBlankOrValue("sparse_infill_acceleration", "100%", "500.0")
    putIfBlankOrZero("support_bottom_interface_spacing", "0.5")
    putIfBlankOrZero("support_bottom_z_distance", "0.2")
    putIfBlankOrValue("support_interface_speed", "80", "100")
    putIfBlankOrValue("support_interface_speed", "80", "100.0")
    putIfBlankOrZero("support_ironing_flow", "10%")
    putIfBlankOrZero("support_ironing_spacing", "0.1")
    putIfBlankOrValue("support_object_xy_distance", "0.5", "50")
    putIfBlankOrValue("support_object_xy_distance", "0.5", "50%")
    putIfBlankOrZero("top_bottom_infill_wall_overlap", "25%")
    if (!process.resolvedOrcaProcessHasKey("detect_narrow_internal_solid_infill")) {
        putIfBlankOrValue("detect_narrow_internal_solid_infill", "1", "0")
        putIfBlankOrValue("detect_narrow_internal_solid_infill", "1", "false")
    }
    if (!process.resolvedOrcaProcessHasKey("ensure_vertical_shell_thickness")) {
        putIfBlankOrValue("ensure_vertical_shell_thickness", "ensure_all", "none")
    }
    if (printer.hasBambuContext() || hasBambuContext()) {
        applyBambuProcessInfillSkinDefaults(process)
    }
    if (!printer.hasBambuContext() && !hasBambuContext() && !process.resolvedOrcaProcessHasKey("only_one_wall_top")) {
        putIfBlankOrValue("only_one_wall_top", "0", "1")
        putIfBlankOrValue("only_one_wall_top", "0", "true")
    }
    if (process.resolvedOrcaProcessHasKey("default_jerk")) {
        putIfBlankOrValue("default_jerk", "9", "0")
        putIfBlankOrValue("default_jerk", "9", "0.0")
    }
    if (process.resolvedOrcaProcessHasKey("top_surface_jerk")) {
        putIfBlankOrValue("top_surface_jerk", "7", "9")
        putIfBlankOrValue("top_surface_jerk", "7", "9.0")
    }
    if (process.resolvedOrcaProcessHasKey("travel_jerk")) {
        putIfBlankOrValue("travel_jerk", "9", "12")
        putIfBlankOrValue("travel_jerk", "9", "12.0")
    }
    putIfBlankOrValue("wall_direction", "auto", "ccw")
    putIfBlankOrValue("wall_direction", "auto", "counterclockwise")
    if (!printer.hasBambuContext() && !hasBambuContext() && !process.resolvedOrcaProcessHasKey("wall_generator")) {
        putIfBlankOrValue("wall_generator", "arachne", "classic")
    }
}

private fun PrinterProfile.hasFlashforgeContext(): Boolean =
    printerAgent.equals("flashforge", ignoreCase = true) ||
        printerModel.contains("flashforge", ignoreCase = true) ||
        name.contains("flashforge", ignoreCase = true) ||
        orcaFamily.equals("Flashforge", ignoreCase = true) ||
        orcaMachineModelPath.contains("Flashforge", ignoreCase = true)

private fun PrinterProfile.hasQidiQ2Context(): Boolean =
    printerAgent.equals("qidi", ignoreCase = true) &&
        printerModel.equals("Qidi Q2", ignoreCase = true) ||
        orcaFamily.equals("Qidi", ignoreCase = true) &&
        (
            printerModel.equals("Qidi Q2", ignoreCase = true) ||
                name.contains("Qidi Q2", ignoreCase = true) ||
                orcaMachineModelPath.contains("Qidi Q2", ignoreCase = true) ||
                orcaResolvedMachineJson.contains("Qidi Q2", ignoreCase = true)
            )

private fun ProcessProfile.hasFlashforgeContext(): Boolean =
    name.contains("Flashforge", ignoreCase = true) ||
        printerVariantKey.contains("Flashforge", ignoreCase = true) ||
        orcaFamily.equals("Flashforge", ignoreCase = true) ||
        orcaProcessPath.contains("Flashforge", ignoreCase = true) ||
        orcaResolvedSourceChain.any { it.contains("Flashforge", ignoreCase = true) }

private fun JSONObject.applyBambuProcessInfillSkinDefaults(process: ProcessProfile) {
    val printSettingsId = scalarString(NativeConfigKeys.Process.SettingsId).ifBlank { process.name }
    val preserveStrengthSkin = printSettingsId.contains("Strength", ignoreCase = true)
    if (!preserveStrengthSkin || !process.resolvedOrcaProcessHasKey("skin_infill_density")) {
        putIfBlankOrValue("skin_infill_density", "15%", "25")
        putIfBlankOrValue("skin_infill_density", "15%", "25%")
    }
    if (!preserveStrengthSkin || !process.resolvedOrcaProcessHasKey("skeleton_infill_density")) {
        putIfBlankOrValue("skeleton_infill_density", "15%", "25")
        putIfBlankOrValue("skeleton_infill_density", "15%", "25%")
    }
    val defaultLineWidth = bambuDefaultInfillSkinLineWidth()
    if (!process.resolvedOrcaProcessHasKey("skin_infill_line_width") ||
        scalarString("skin_infill_line_width") == "100%"
    ) {
        putIfBlankOrValue("skin_infill_line_width", defaultLineWidth, "100%")
    }
    if (!process.resolvedOrcaProcessHasKey("skeleton_infill_line_width") ||
        scalarString("skeleton_infill_line_width") == "100%"
    ) {
        putIfBlankOrValue("skeleton_infill_line_width", defaultLineWidth, "100%")
    }
}

private fun JSONObject.bambuDefaultInfillSkinLineWidth(): String {
    val nozzleDiameter = scalarString("nozzle_diameter").toDoubleOrNull() ?: return "0.45"
    return when {
        nozzleDiameter <= 0.25 -> "0.22"
        nozzleDiameter <= 0.45 -> "0.45"
        nozzleDiameter <= 0.65 -> "0.62"
        else -> "0.82"
    }
}

private fun ProcessProfile.resolvedOrcaProcessHasKey(key: String): Boolean =
    runCatching { JSONObject(orcaProcessOverridesJson).has(key) }.getDefaultOrFalse() ||
        runCatching { JSONObject(orcaResolvedProcessJson).has(key) }.getDefaultOrFalse()

private fun Result<Boolean>.getDefaultOrFalse(): Boolean = getOrDefault(false)

private fun JSONObject.expandOrcaMachineLimitVectors() {
    val keys = setOf(
        "machine_max_speed_x",
        "machine_max_speed_y",
        "machine_max_speed_z",
        "machine_max_speed_e",
        "machine_max_acceleration_x",
        "machine_max_acceleration_y",
        "machine_max_acceleration_z",
        "machine_max_acceleration_e",
        "machine_max_acceleration_extruding",
        "machine_max_acceleration_retracting",
        "machine_max_acceleration_travel",
        "machine_max_jerk_x",
        "machine_max_jerk_y",
        "machine_max_jerk_z",
        "machine_max_jerk_e"
    )
    keys.forEach { key ->
        val value = opt(key) ?: return@forEach
        if (nativeArrayLength(value) >= 2) return@forEach
        val scalar = nativeExpansionScalar(value)
        put(key, JSONArray().put(scalar).put(scalar))
    }
}

private fun JSONObject.putIfBlankOrZero(key: String, value: String) {
    val current = scalarString(key).trim()
    if (current.isBlank() || current == "0" || current == "0%" || current == "0.0") {
        put(key, value)
    }
}

private fun JSONObject.putIfBlank(key: String, value: String) {
    if (scalarString(key).isBlank()) {
        put(key, value)
    }
}

private fun JSONObject.putIfBlankOrValue(key: String, value: String, currentValue: String) {
    val current = scalarString(key)
    if (current.isBlank() || current == currentValue) {
        put(key, value)
    }
}

private fun JSONObject.removeIfUnresolvedDefault(
    source: JSONObject?,
    key: String,
    vararg defaultValues: String
) {
    if (source?.has(key) == true) return
    removeIfDefaultValue(key, *defaultValues)
}

private fun JSONObject.removeIfDefaultValue(
    key: String,
    vararg defaultValues: String
) {
    val current = scalarString(key)
    if (current in defaultValues) {
        remove(key)
    }
}

private fun String.orcaProfileNameFromPath(): String =
    substringAfterLast('/')
        .removeSuffix(".json")
        .trim()

private fun FilamentProfile.orcaDefaultColorFallback(): String =
    if (hasOrcaContext()) {
        "#26A69A"
    } else {
        ""
    }
