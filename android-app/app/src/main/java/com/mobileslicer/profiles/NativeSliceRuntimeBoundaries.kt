package com.mobileslicer.profiles

import org.json.JSONArray
import org.json.JSONObject

internal fun JSONObject.applySingleMaterialNativeSliceDefaults(preserveOrcaResolvedValues: Boolean = false) {
    put(NativeConfigKeys.Mobile.SingleMaterialNativeSlice, true)
    applySingleMaterialNativeRuntimeBoundary(
        preserveSingleExtruderMultiMaterial = preserveOrcaResolvedValues
    )
    if (preserveOrcaResolvedValues) return
}

internal fun JSONObject.applySingleFilamentSlotNativeRuntimeBoundary(
    preserveSingleExtruderMultiMaterial: Boolean = true
) {
    put(NativeConfigKeys.Mobile.ActiveFilamentSlotCount, 1)
    applySingleMaterialNativeRuntimeBoundary(
        preserveSingleExtruderMultiMaterial = preserveSingleExtruderMultiMaterial
    )
    put(NativeConfigKeys.Filament.Map, JSONArray().put(1))
    put(NativeConfigKeys.Filament.SelfIndex, JSONArray().put(1))
}

internal fun JSONObject.applyBambuNativeSliceSafetyDefaults(
    printer: PrinterProfile,
    preserveOrcaResolvedValues: Boolean = false
) {
    if (!printer.hasBambuContext() && !hasBambuContext()) return
    if (preserveOrcaResolvedValues) return
    applySingleMaterialNativeRuntimeBoundary()
    collapseBambuNativeSliceToActiveFilament()
}

private fun JSONObject.applySingleMaterialNativeRuntimeBoundary(
    preserveSingleExtruderMultiMaterial: Boolean = false
) {
    put(NativeConfigKeys.PrimeTower.Enable, false)
    put(NativeConfigKeys.PrimeTower.Purge, false)
    if (!preserveSingleExtruderMultiMaterial) {
        put(NativeConfigKeys.PrimeTower.SingleExtruderMultiMaterial, false)
    }
    if (!preserveSingleExtruderMultiMaterial) {
        put(NativeConfigKeys.PrimeTower.SingleExtruderMultiMaterialPriming, false)
        put(NativeConfigKeys.PrimeTower.AllowMulticolorOnePlate, false)
        put(NativeConfigKeys.PrimeTower.FlushIntoInfill, false)
        put(NativeConfigKeys.PrimeTower.FlushIntoObjects, false)
        put(NativeConfigKeys.PrimeTower.FlushIntoSupport, false)
    }
}

private fun JSONObject.collapseBambuNativeSliceToActiveFilament() {
    put(NativeConfigKeys.Printer.ExtrudersCount, 1)
    put(NativeConfigKeys.Filament.Map, JSONArray().put(1))
    put(NativeConfigKeys.Filament.MapMode, "Auto For Flush")
    put(NativeConfigKeys.Filament.SelfIndex, JSONArray().put(1))

    val activeVariant = firstNativeScalar(NativeConfigKeys.Filament.ExtruderVariant)
        ?: firstNativeScalar(NativeConfigKeys.Printer.ExtruderVariant)
        ?: firstNativeScalar(NativeConfigKeys.Printer.ExtruderVariantList)
        ?: "Direct Drive Standard"
    put(NativeConfigKeys.Filament.ExtruderVariant, JSONArray().put(activeVariant))
    put(NativeConfigKeys.Printer.ExtruderId, JSONArray().put(1))
    put(NativeConfigKeys.Printer.ExtruderVariant, JSONArray().put(activeVariant))
    put(NativeConfigKeys.Process.ExtruderId, JSONArray().put(1))
    put(NativeConfigKeys.Process.ExtruderVariant, JSONArray().put(activeVariant))
    put(NativeConfigKeys.Printer.ExtruderVariantList, JSONArray().put(activeVariant))

    val singleToolKeys = nativeToolVectorKeys + setOf(
        "extruder_type",
        "nozzle_volume_type",
        "default_nozzle_volume_type",
        NativeConfigKeys.Printer.ExtruderId,
        NativeConfigKeys.Printer.ExtruderVariant,
        NativeConfigKeys.Process.ExtruderId,
        NativeConfigKeys.Process.ExtruderVariant,
        NativeConfigKeys.Printer.ExtruderVariantList
    )
    singleToolKeys.forEach { key ->
        val scalar = firstNativeScalarValue(key) ?: return@forEach
        put(key, JSONArray().put(scalar))
    }

    put(NativeConfigKeys.Filament.Map, JSONArray().put(1))
    put(NativeConfigKeys.Filament.SelfIndex, JSONArray().put(1))
    put(NativeConfigKeys.Filament.ExtruderVariant, JSONArray().put(activeVariant))
    collapseToActiveScalarString(NativeConfigKeys.Filament.SettingsId)
    collapseToActiveScalarString(NativeConfigKeys.Filament.Ids)
    collapseToActiveScalarString(NativeConfigKeys.Filament.Color)
    collapseToActiveScalarString(NativeConfigKeys.Filament.MultiColor)
}

private fun JSONObject.firstNativeScalar(key: String): String? =
    firstNativeScalarValue(key)?.toString()?.trim()?.trim('"')?.takeIf { it.isNotBlank() }

private fun JSONObject.collapseToActiveScalarString(key: String) {
    firstNativeScalar(key)?.let { put(key, it) }
}

private fun JSONObject.firstNativeScalarValue(key: String): Any? {
    val value = opt(key) ?: return null
    return when (value) {
        is JSONArray -> {
            for (index in 0 until value.length()) {
                val item = value.opt(index) ?: continue
                if (item.toString().isNotBlank()) return item
            }
            null
        }
        is String -> {
            val trimmed = value.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val parsed = runCatching { JSONArray(trimmed) }.getOrNull()
                if (parsed != null) {
                    for (index in 0 until parsed.length()) {
                        val item = parsed.opt(index) ?: continue
                        if (item.toString().isNotBlank()) return item
                    }
                }
            }
            trimmed.takeIf { it.isNotBlank() }
        }
        else -> value
    }
}

internal fun PrinterProfile.hasBambuContext(): Boolean =
    printerAgent.contains("bambu", ignoreCase = true) ||
        printerModel.contains("bambu", ignoreCase = true) ||
        name.contains("bambu", ignoreCase = true) ||
        orcaFamily.contains("bambu", ignoreCase = true) ||
        orcaMachineModelPath.contains("bambu", ignoreCase = true) ||
        orcaMachineModelPath.contains("bbl", ignoreCase = true)

internal fun JSONObject.hasBambuContext(): Boolean =
    sequenceOf(
        NativeConfigKeys.Printer.SettingsId,
        NativeConfigKeys.Printer.Model,
        NativeConfigKeys.Printer.Variant,
        NativeConfigKeys.Printer.Inherits,
        NativeConfigKeys.Printer.Name,
        NativeConfigKeys.Printer.Notes,
        NativeConfigKeys.Printer.DefaultPrintProfile,
        NativeConfigKeys.Printer.DefaultFilamentProfile
    ).any { key ->
        val value = opt(key) ?: return@any false
        value.toString().contains("bambu", ignoreCase = true) ||
            value.toString().contains("@BBL", ignoreCase = true) ||
            value.toString().contains("BBL ", ignoreCase = true)
    }
