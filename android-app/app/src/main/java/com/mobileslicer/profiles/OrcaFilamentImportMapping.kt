package com.mobileslicer.profiles

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private val resolvedFilamentJsonCacheLock = Any()
private val resolvedFilamentJsonCache = object : LinkedHashMap<String, JSONObject>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, JSONObject>?): Boolean =
        size > 24
}

internal fun OrcaFilamentPreset.toImportedFilamentProfile(
    bundle: OrcaFilamentImportBundle,
    printer: PrinterProfile? = null
): FilamentProfile {
    val resolved = resolvedFilamentJsonObject(bundle.resolvedFilamentJson)
    val material = resolved.filamentConfigString("filament_type", materialType.ifBlank { name })
    return FilamentProfile(
        id = "orca_filament_${"$profilePath@${printer?.id.orEmpty()}".hashCode().toUInt().toString(16)}",
        name = name,
        subtitle = "",
        builtIn = false,
        materialType = material,
        vendor = resolved.filamentConfigString("filament_vendor", vendor),
        soluble = resolved.filamentConfigBoolean("filament_soluble", false),
        supportMaterial = resolved.filamentConfigBoolean("filament_is_support", false),
        filamentExtruderVariant = resolved.filamentConfigString("filament_extruder_variant", "Direct Drive Standard"),
        filamentSelfIndex = resolved.filamentConfigString("filament_self_index", "1"),
        filamentChangeLengthMm = resolved.filamentConfigFloat("filament_change_length", 10f),
        requiredNozzleHrc = resolved.filamentConfigInt("required_nozzle_HRC", 0),
        defaultFilamentColor = resolved.filamentConfigString("default_filament_colour", defaultFilamentColor),
        diameterMm = resolved.filamentConfigFloat("filament_diameter", 1.75f),
        adhesivenessCategory = resolved.filamentConfigInt("filament_adhesiveness_category", 0),
        densityGPerCm3 = resolved.filamentConfigFloat("filament_density", defaultFilamentDensityForMaterial(material)),
        shrinkageXyPercent = resolved.filamentConfigFloat("filament_shrink", 100f),
        shrinkageZPercent = resolved.filamentConfigFloat("filament_shrinkage_compensation_z", 100f),
        costPerKg = resolved.filamentConfigFloat("filament_cost", 0f),
        softeningTemperatureC = resolved.filamentConfigInt("temperature_vitrification", 100),
        idleTemperatureC = resolved.filamentConfigInt("idle_temperature", 0),
        nozzleTemperatureRangeLowC = resolved.filamentConfigInt("nozzle_temperature_range_low", 190),
        nozzleTemperatureRangeHighC = resolved.filamentConfigInt("nozzle_temperature_range_high", 240),
        flowRatio = resolved.filamentConfigFloat("filament_flow_ratio", 1f),
        retractionLengthMm = resolved.filamentConfigOptionalFloat("filament_retraction_length"),
        zHopMm = resolved.filamentConfigOptionalFloat("filament_z_hop"),
        zHopType = resolved.filamentConfigOptionalString("filament_z_hop_types")?.let { ZHopType.fromConfigValue(it) },
        retractLiftAboveMm = resolved.filamentConfigOptionalFloat("filament_retract_lift_above"),
        retractLiftBelowMm = resolved.filamentConfigOptionalFloat("filament_retract_lift_below"),
        retractLiftEnforce = resolved.filamentConfigOptionalString("filament_retract_lift_enforce")?.let { RetractLiftEnforce.fromConfigValue(it) },
        retractionSpeedMmPerSec = resolved.filamentConfigOptionalFloat("filament_retraction_speed"),
        deretractionSpeedMmPerSec = resolved.filamentConfigOptionalFloat("filament_deretraction_speed"),
        retractRestartExtraMm = resolved.filamentConfigOptionalFloat("filament_retract_restart_extra"),
        retractionMinimumTravelMm = resolved.filamentConfigOptionalFloat("filament_retraction_minimum_travel"),
        retractWhenChangingLayer = resolved.filamentConfigOptionalBoolean("filament_retract_when_changing_layer"),
        wipe = resolved.filamentConfigOptionalBoolean("filament_wipe"),
        wipeDistanceMm = resolved.filamentConfigOptionalFloat("filament_wipe_distance"),
        retractBeforeWipePercent = resolved.filamentConfigOptionalInt("filament_retract_before_wipe"),
        longRetractionsWhenCut = resolved.filamentConfigOptionalBoolean("filament_long_retractions_when_cut"),
        retractionDistancesWhenCut = resolved.filamentConfigString("filament_retraction_distances_when_cut"),
        ironingFlowPercent = resolved.filamentConfigOptionalFloat("filament_ironing_flow"),
        ironingSpacingMm = resolved.filamentConfigOptionalFloat("filament_ironing_spacing"),
        ironingInsetMm = resolved.filamentConfigOptionalFloat("filament_ironing_inset"),
        ironingSpeedMmPerSec = resolved.filamentConfigOptionalFloat("filament_ironing_speed"),
        pressureAdvanceEnabled = resolved.filamentConfigBoolean("enable_pressure_advance", false),
        pressureAdvance = resolved.filamentConfigFloat("pressure_advance", 0f),
        pelletFlowCoefficient = resolved.filamentConfigFloat("pellet_flow_coefficient", 0.4157f),
        adaptivePressureAdvanceEnabled = resolved.filamentConfigBoolean("adaptive_pressure_advance", false),
        adaptivePressureAdvanceOverhangsEnabled = resolved.filamentConfigBoolean("adaptive_pressure_advance_overhangs", false),
        adaptivePressureAdvanceBridges = resolved.filamentConfigFloat("adaptive_pressure_advance_bridges", 0f),
        adaptivePressureAdvanceModel = resolved.filamentConfigString("adaptive_pressure_advance_model", "0,0,0\n0,0,0"),
        maxVolumetricSpeedMm3PerSec = resolved.filamentConfigFloat("filament_max_volumetric_speed", defaultFilamentMaxVolumetricSpeedForMaterial(material)),
        adaptiveVolumetricSpeedEnabled = resolved.filamentConfigBoolean("filament_adaptive_volumetric_speed", false),
        volumetricSpeedCoefficients = resolved.filamentConfigString("volumetric_speed_coefficients"),
        nozzleTemperatureInitialLayerC = resolved.filamentConfigInt("nozzle_temperature_initial_layer", resolved.filamentConfigInt("nozzle_temperature", 210)),
        nozzleTemperatureC = resolved.filamentConfigInt("nozzle_temperature", 210),
        chamberTemperatureC = resolved.filamentConfigInt("chamber_temperature", 0),
        activateChamberTemperatureControl = resolved.filamentConfigBoolean("activate_chamber_temp_control", false),
        supertackPlateTemperatureInitialLayerC = resolved.filamentConfigInt("supertack_plate_temp_initial_layer", 35),
        supertackPlateTemperatureC = resolved.filamentConfigInt("supertack_plate_temp", 35),
        coolPlateTemperatureInitialLayerC = resolved.filamentConfigInt("cool_plate_temp_initial_layer", 35),
        coolPlateTemperatureC = resolved.filamentConfigInt("cool_plate_temp", 35),
        texturedCoolPlateTemperatureInitialLayerC = resolved.filamentConfigInt("textured_cool_plate_temp_initial_layer", resolved.filamentConfigInt("textured_plate_temp_initial_layer", 40)),
        texturedCoolPlateTemperatureC = resolved.filamentConfigInt("textured_cool_plate_temp", resolved.filamentConfigInt("textured_plate_temp", 40)),
        engineeringPlateTemperatureInitialLayerC = resolved.filamentConfigInt("eng_plate_temp_initial_layer", 45),
        engineeringPlateTemperatureC = resolved.filamentConfigInt("eng_plate_temp", 45),
        bedTemperatureInitialLayerC = resolved.filamentConfigInt("hot_plate_temp_initial_layer", resolved.filamentConfigInt("bed_temperature_initial_layer", 60)),
        bedTemperatureC = resolved.filamentConfigInt("hot_plate_temp", resolved.filamentConfigInt("bed_temperature", 60)),
        texturedPlateTemperatureInitialLayerC = resolved.filamentConfigInt("textured_plate_temp_initial_layer", 45),
        texturedPlateTemperatureC = resolved.filamentConfigInt("textured_plate_temp", 45),
        minFanSpeedPercent = resolved.filamentConfigInt("fan_min_speed", 30),
        coolingPercent = resolved.filamentConfigInt("fan_max_speed", 100),
        noCoolingFirstLayers = resolved.filamentConfigInt("close_fan_the_first_x_layers", 1),
        fullFanSpeedLayer = resolved.filamentConfigInt("full_fan_speed_layer", 0),
        fanCoolingLayerTimeSeconds = resolved.filamentConfigFloat("fan_cooling_layer_time", 60f),
        slowDownLayerTimeSeconds = resolved.filamentConfigFloat("slow_down_layer_time", 5f),
        reduceFanStopStartFrequency = resolved.filamentConfigBoolean("reduce_fan_stop_start_freq", false),
        slowDownForLayerCooling = resolved.filamentConfigBoolean("slow_down_for_layer_cooling", true),
        dontSlowDownOuterWall = resolved.filamentConfigBoolean("dont_slow_down_outer_wall", false),
        slowDownMinSpeedMmPerSec = resolved.filamentConfigFloat("slow_down_min_speed", 10f),
        enableOverhangBridgeFan = resolved.filamentConfigBoolean("enable_overhang_bridge_fan", true),
        overhangFanThreshold = resolved.filamentConfigString("overhang_fan_threshold", "95%"),
        overhangFanSpeedPercent = resolved.filamentConfigInt("overhang_fan_speed", 100),
        internalBridgeFanSpeedPercent = resolved.filamentConfigInt("internal_bridge_fan_speed", -1),
        supportMaterialInterfaceFanSpeedPercent = resolved.filamentConfigInt("support_material_interface_fan_speed", -1),
        ironingFanSpeedPercent = resolved.filamentConfigInt("ironing_fan_speed", -1),
        additionalCoolingFanSpeedPercent = resolved.filamentConfigInt("additional_cooling_fan_speed", 0),
        activateAirFiltration = resolved.filamentConfigBoolean("activate_air_filtration", false),
        duringPrintExhaustFanSpeedPercent = resolved.filamentConfigInt("during_print_exhaust_fan_speed", 60),
        completePrintExhaustFanSpeedPercent = resolved.filamentConfigInt("complete_print_exhaust_fan_speed", 80),
        minimalPurgeOnWipeTowerMm3 = resolved.filamentConfigFloat("filament_minimal_purge_on_wipe_tower", 15f),
        towerInterfacePreExtrusionDistanceMm = resolved.filamentConfigFloat("filament_tower_interface_pre_extrusion_dist", 10f),
        towerInterfacePreExtrusionLengthMm = resolved.filamentConfigFloat("filament_tower_interface_pre_extrusion_length", 0f),
        towerIroningAreaMm2 = resolved.filamentConfigFloat("filament_tower_ironing_area", 4f),
        towerInterfacePurgeVolumeMm = resolved.filamentConfigFloat("filament_tower_interface_purge_volume", 20f),
        towerInterfacePrintTemperatureC = resolved.filamentConfigInt("filament_tower_interface_print_temp", -1),
        longRetractionsWhenExtruderChange = resolved.filamentConfigOptionalBoolean("long_retractions_when_ec"),
        retractionDistanceWhenExtruderChangeMm = resolved.filamentConfigOptionalFloat("retraction_distances_when_ec"),
        loadingSpeedStartMmPerSec = resolved.filamentConfigFloat("filament_loading_speed_start", 3f),
        loadingSpeedMmPerSec = resolved.filamentConfigFloat("filament_loading_speed", 28f),
        unloadingSpeedStartMmPerSec = resolved.filamentConfigFloat("filament_unloading_speed_start", 100f),
        unloadingSpeedMmPerSec = resolved.filamentConfigFloat("filament_unloading_speed", 90f),
        toolchangeDelaySeconds = resolved.filamentConfigFloat("filament_toolchange_delay", 0f),
        coolingMoves = resolved.filamentConfigInt("filament_cooling_moves", 4),
        coolingInitialSpeedMmPerSec = resolved.filamentConfigFloat("filament_cooling_initial_speed", 2.2f),
        coolingFinalSpeedMmPerSec = resolved.filamentConfigFloat("filament_cooling_final_speed", 3.4f),
        stampingLoadingSpeedMmPerSec = resolved.filamentConfigFloat("filament_stamping_loading_speed", 0f),
        stampingDistanceMm = resolved.filamentConfigFloat("filament_stamping_distance", 0f),
        rammingParameters = resolved.filamentConfigString("filament_ramming_parameters", "120 100 6.6 6.8 7.2 7.6 7.9 8.2 8.7 9.4 9.9 10.0| 0.05 6.6 0.45 6.8 0.95 7.8 1.45 8.3 1.95 9.7 2.45 10 2.95 7.6 3.45 7.6 3.95 7.6 4.45 7.6 4.95 7.6"),
        multitoolRamming = resolved.filamentConfigBoolean("filament_multitool_ramming", false),
        multitoolRammingVolumeMm3 = resolved.filamentConfigFloat("filament_multitool_ramming_volume", 10f),
        multitoolRammingFlowMm3PerSec = resolved.filamentConfigFloat("filament_multitool_ramming_flow", 10f),
        compatiblePrinters = resolved.filamentConfigString("compatible_printers"),
        compatiblePrintersCondition = resolved.filamentConfigString("compatible_printers_condition"),
        compatiblePrints = resolved.filamentConfigString("compatible_prints"),
        compatiblePrintsCondition = resolved.filamentConfigString("compatible_prints_condition"),
        filamentNotes = resolved.filamentConfigString("filament_notes"),
        filamentStartGcode = resolved.filamentConfigString("filament_start_gcode"),
        filamentEndGcode = resolved.filamentConfigString("filament_end_gcode"),
        printerProfileId = printer?.id.orEmpty(),
        profileSource = "orca",
        orcaFamily = family,
        orcaFilamentPath = profilePath,
        orcaRawFilamentJson = bundle.rawFilamentJson,
        orcaResolvedFilamentJson = bundle.resolvedFilamentJson,
        orcaResolvedSourceChain = bundle.resolvedSourceChain
    )
}

private fun resolvedFilamentJsonObject(text: String): JSONObject {
    synchronized(resolvedFilamentJsonCacheLock) {
        resolvedFilamentJsonCache[text]?.let { return it }
    }
    val parsed = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
    synchronized(resolvedFilamentJsonCacheLock) {
        resolvedFilamentJsonCache[text] = parsed
    }
    return parsed
}

private fun JSONObject.filamentConfigString(key: String, defaultValue: String = ""): String {
    if (!has(key) || isNull(key)) return defaultValue
    val value = opt(key)
    return when (value) {
        is JSONArray -> if (value.length() == 1) value.optString(0, defaultValue) else value.toString()
        else -> value?.toString().orEmpty().ifBlank { defaultValue }
    }
}

private fun JSONObject.filamentConfigFloat(key: String, defaultValue: Float): Float =
    filamentConfigScalar(key)?.toString()?.trim()?.toFloatOrNull() ?: defaultValue

private fun JSONObject.filamentConfigInt(key: String, defaultValue: Int): Int =
    filamentConfigScalar(key)?.toString()?.trim()?.toFloatOrNull()?.toInt() ?: defaultValue

private fun JSONObject.filamentConfigOptionalString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return filamentConfigString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.filamentConfigOptionalFloat(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    return filamentConfigScalar(key)?.toString()?.trim()?.toFloatOrNull()
}

private fun JSONObject.filamentConfigOptionalInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return filamentConfigScalar(key)?.toString()?.trim()?.toFloatOrNull()?.toInt()
}

private fun JSONObject.filamentConfigOptionalBoolean(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    val raw = filamentConfigScalar(key)?.toString()?.trim()?.lowercase(Locale.US) ?: return null
    return when (raw) {
        "1", "true", "yes", "on" -> true
        "0", "false", "no", "off" -> false
        else -> null
    }
}

private fun JSONObject.filamentConfigBoolean(key: String, defaultValue: Boolean = false): Boolean {
    val raw = filamentConfigScalar(key)?.toString()?.trim()?.lowercase(Locale.US) ?: return defaultValue
    return when (raw) {
        "1", "true", "yes", "on" -> true
        "0", "false", "no", "off" -> false
        else -> defaultValue
    }
}

private fun JSONObject.filamentConfigScalar(key: String): Any? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key)
    return if (value is JSONArray) {
        if (value.length() == 0) null else value.opt(0)
    } else {
        value
    }
}

private fun defaultFilamentDensityForMaterial(materialType: String): Float =
    when (materialType.uppercase(Locale.US)) {
        "ABS", "ASA" -> 1.04f
        "PETG", "PCTG" -> 1.27f
        "TPU", "TPE" -> 1.20f
        "PA", "PA-CF" -> 1.14f
        "PC" -> 1.20f
        else -> 1.24f
    }

private fun defaultFilamentMaxVolumetricSpeedForMaterial(materialType: String): Float =
    when (materialType.uppercase(Locale.US)) {
        "ABS", "ASA" -> 11f
        "PETG", "PCTG" -> 10f
        "TPU", "TPE" -> 3.5f
        "PA", "PA-CF", "PC" -> 8f
        else -> 12f
    }
