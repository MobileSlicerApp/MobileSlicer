package com.mobileslicer.profiles

import org.json.JSONObject

internal fun JSONObject.putNativeFilamentOverrideConfiguration(filament: FilamentProfile): JSONObject = apply {
    val resolvedFilamentJson = filament.orcaResolvedFilamentJson.asNativeConfigJsonObjectOrNull()
    putResolvedOptionalDoubleOrProfile(resolvedFilamentJson, "filament_retraction_length", filament.retractionLengthMm)
    filament.zHopMm?.let { put("filament_z_hop", it.toDouble()) }
    putResolvedOptionalStringOrProfile(resolvedFilamentJson, "filament_z_hop_types", filament.zHopType?.configValue)
    filament.retractLiftAboveMm?.let { put("filament_retract_lift_above", it.toDouble()) }
    filament.retractLiftBelowMm?.let { put("filament_retract_lift_below", it.toDouble()) }
    filament.retractLiftEnforce?.let { put("filament_retract_lift_enforce", it.configValue) }
    filament.retractionSpeedMmPerSec?.let { put("filament_retraction_speed", it.toDouble()) }
    filament.deretractionSpeedMmPerSec?.let { put("filament_deretraction_speed", it.toDouble()) }
    filament.retractRestartExtraMm?.let { put("filament_retract_restart_extra", it.toDouble()) }
    filament.retractionMinimumTravelMm?.let { put("filament_retraction_minimum_travel", it.toDouble()) }
    filament.retractWhenChangingLayer?.let { put("filament_retract_when_changing_layer", it) }
    putResolvedOptionalBooleanOrProfile(resolvedFilamentJson, "filament_wipe", filament.wipe)
    putResolvedOptionalDoubleOrProfile(resolvedFilamentJson, "filament_wipe_distance", filament.wipeDistanceMm)
    filament.retractBeforeWipePercent?.let { put("filament_retract_before_wipe", it) }
    putResolvedOptionalBooleanOrProfile(resolvedFilamentJson, "filament_long_retractions_when_cut", filament.longRetractionsWhenCut)
    filament.longRetractionsWhenExtruderChange?.let { put("long_retractions_when_ec", it) }
    filament.retractionDistanceWhenExtruderChangeMm?.let { put("retraction_distances_when_ec", it.toDouble()) }
    putResolvedOptionalStringOrProfile(resolvedFilamentJson, "filament_retraction_distances_when_cut", filament.retractionDistancesWhenCut)
    filament.ironingFlowPercent?.let { put("filament_ironing_flow", it.toDouble()) }
    filament.ironingSpacingMm?.let { put("filament_ironing_spacing", it.toDouble()) }
    filament.ironingInsetMm?.let { put("filament_ironing_inset", it.toDouble()) }
    filament.ironingSpeedMmPerSec?.let { put("filament_ironing_speed", it.toDouble()) }
}

private fun JSONObject.putResolvedOptionalDoubleOrProfile(
    resolved: JSONObject?,
    key: String,
    profileValue: Float?
) {
    profileValue?.let { put(key, it.toDouble()) }
        ?: if (resolved?.has(key) == true) {
            resolved.nativeResolvedDouble(key)?.let { put(key, it) } ?: remove(key)
        } else {
            Unit
        }
}

private fun JSONObject.putResolvedOptionalBooleanOrProfile(
    resolved: JSONObject?,
    key: String,
    profileValue: Boolean?
) {
    profileValue?.let { put(key, it) }
        ?: if (resolved?.has(key) == true) {
            resolved.nativeResolvedBoolean(key)?.let { put(key, it) } ?: remove(key)
        } else {
            Unit
        }
}

private fun JSONObject.putResolvedOptionalStringOrProfile(
    resolved: JSONObject?,
    key: String,
    profileValue: String?
) {
    if (!profileValue.isNullOrBlank()) {
        put(key, profileValue)
        return
    }
    if (resolved?.has(key) == true) {
        val resolvedValue = resolved.nativeResolvedString(key)
        if (resolvedValue.isBlank()) remove(key) else put(key, resolvedValue)
        return
    }
}
