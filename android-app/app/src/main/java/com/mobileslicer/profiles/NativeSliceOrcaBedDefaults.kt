package com.mobileslicer.profiles

import org.json.JSONObject

internal val knownOrcaBedTypes = setOf(
    "Cool Plate",
    "High Temp Plate",
    "Engineering Plate",
    "Textured PEI Plate",
    "Textured Cool Plate",
    "SuperTack Plate"
)

internal fun normalizedOrcaBedTypeName(value: String): String {
    val trimmed = value.trim().trim('"')
    if (trimmed.isBlank()) return ""
    if (trimmed in knownOrcaBedTypes) return trimmed
    return DefaultBedType.fromConfigValue(trimmed).orcaBedTypeName
}

internal fun JSONObject.applyOrcaCurrentBedTypeDefault(printer: PrinterProfile? = null) {
    val current = scalarString(NativeConfigKeys.Bed.CurrentType)
    val defaultBedType = normalizedOrcaBedTypeName(scalarString(NativeConfigKeys.Bed.DefaultType))
    if (defaultBedType in knownOrcaBedTypes && current.isBlank()) {
        put(NativeConfigKeys.Bed.CurrentType, defaultBedType)
        return
    }
    val profileDefault = printer?.effectiveOrcaBedTypeName().orEmpty()
    if ((current.isBlank() || current == "Cool Plate") && profileDefault in knownOrcaBedTypes) {
        put(NativeConfigKeys.Bed.CurrentType, profileDefault)
        return
    }
    val initialBed = optDouble(NativeConfigKeys.Temperature.BedInitialLayer, Double.NaN)
        .takeIf { it.isFinite() }
        ?: optDouble(NativeConfigKeys.Temperature.Bed, Double.NaN).takeIf { it.isFinite() }
        ?: return
    val inferred = sequenceOf(
        NativeConfigKeys.Temperature.HotPlateInitialLayer to "High Temp Plate",
        NativeConfigKeys.Temperature.HotPlate to "High Temp Plate",
        NativeConfigKeys.Temperature.EngineeringPlateInitialLayer to "Engineering Plate",
        NativeConfigKeys.Temperature.EngineeringPlate to "Engineering Plate",
        NativeConfigKeys.Temperature.TexturedPlateInitialLayer to "Textured PEI Plate",
        NativeConfigKeys.Temperature.TexturedPlate to "Textured PEI Plate",
        NativeConfigKeys.Temperature.SupertackPlateInitialLayer to "SuperTack Plate",
        NativeConfigKeys.Temperature.SupertackPlate to "SuperTack Plate",
        NativeConfigKeys.Temperature.TexturedCoolPlateInitialLayer to "Textured Cool Plate",
        NativeConfigKeys.Temperature.TexturedCoolPlate to "Textured Cool Plate",
        NativeConfigKeys.Temperature.CoolPlateInitialLayer to "Cool Plate",
        NativeConfigKeys.Temperature.CoolPlate to "Cool Plate"
    ).firstOrNull { (key, _) ->
        optDouble(key, Double.NaN).takeIf { it.isFinite() } == initialBed
    }?.second
    if (!inferred.isNullOrBlank() && (current.isBlank() || current == "Cool Plate")) {
        put(NativeConfigKeys.Bed.CurrentType, inferred)
    }
}
