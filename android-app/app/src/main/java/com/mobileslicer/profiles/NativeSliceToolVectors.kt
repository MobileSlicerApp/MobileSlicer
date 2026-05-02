package com.mobileslicer.profiles

import org.json.JSONArray
import org.json.JSONObject

internal val nativeToolVectorKeys = setOf(
    "nozzle_diameter",
    "min_layer_height",
    "max_layer_height",
    "extruder_offset",
    "extruder_type",
    "extruder_colour",
    "extruder_printable_height",
    "extruder_printable_area",
    "extruder_variant_list",
    "physical_extruder_map",
    "printer_extruder_id",
    "printer_extruder_variant",
    "print_extruder_id",
    "print_extruder_variant",
    "nozzle_type",
    "nozzle_height",
    "nozzle_volume",
    "nozzle_volume_type",
    "default_nozzle_volume_type",
    "grab_length",
    "hotend_heating_rate",
    "hotend_cooling_rate",
    "retraction_length",
    "retract_restart_extra",
    "retraction_speed",
    "deretraction_speed",
    "retraction_minimum_travel",
    "retract_when_changing_layer",
    "retract_on_top_layer",
    "wipe",
    "wipe_distance",
    "retract_before_wipe",
    "retract_lift_enforce",
    "z_hop_types",
    "z_hop_when_prime",
    "z_lift_type",
    "z_hop",
    "travel_slope",
    "retract_lift_above",
    "retract_lift_below",
    "retract_length_toolchange",
    "retract_restart_extra_toolchange",
    "long_retractions_when_cut",
    "retraction_distances_when_cut",
    "filament_diameter",
    "filament_type",
    "filament_vendor",
    "filament_soluble",
    "filament_is_support",
    "filament_extruder_variant",
    "filament_self_index",
    "filament_settings_id",
    "filament_ids",
    "filament_colour",
    "filament_multi_colour",
    "filament_map",
    "filament_change_length",
    "required_nozzle_HRC",
    "default_filament_colour",
    "filament_adhesiveness_category",
    "filament_density",
    "filament_shrink",
    "filament_shrinkage_compensation_z",
    "filament_cost",
    "temperature_vitrification",
    "idle_temperature",
    "nozzle_temperature_range_low",
    "nozzle_temperature_range_high",
    "filament_flow_ratio",
    "enable_pressure_advance",
    "pressure_advance",
    "pellet_flow_coefficient",
    "adaptive_pressure_advance",
    "adaptive_pressure_advance_overhangs",
    "adaptive_pressure_advance_bridges",
    "filament_max_volumetric_speed",
    "filament_adaptive_volumetric_speed",
    "volumetric_speed_coefficients",
    "nozzle_temperature_initial_layer",
    "nozzle_temperature",
    "filament_flush_volumetric_speed",
    "filament_flush_temp",
    "filament_ironing_flow",
    "filament_ironing_spacing",
    "filament_ironing_inset",
    "filament_ironing_speed",
    "filament_long_retractions_when_cut",
    "filament_retraction_distances_when_cut",
    "long_retractions_when_ec",
    "retraction_distances_when_ec",
    "chamber_temperature",
    "activate_chamber_temp_control",
    "supertack_plate_temp_initial_layer",
    "supertack_plate_temp",
    "cool_plate_temp_initial_layer",
    "cool_plate_temp",
    "textured_cool_plate_temp_initial_layer",
    "textured_cool_plate_temp",
    "eng_plate_temp_initial_layer",
    "eng_plate_temp",
    "bed_temperature_initial_layer",
    "bed_temperature",
    "hot_plate_temp_initial_layer",
    "hot_plate_temp",
    "textured_plate_temp_initial_layer",
    "textured_plate_temp",
    "fan_min_speed",
    "fan_max_speed",
    "cooling_baseline",
    "close_fan_the_first_x_layers",
    "full_fan_speed_layer",
    "fan_cooling_layer_time",
    "slow_down_layer_time",
    "reduce_fan_stop_start_freq",
    "slow_down_for_layer_cooling",
    "dont_slow_down_outer_wall",
    "slow_down_min_speed",
    "enable_overhang_bridge_fan",
    "overhang_fan_threshold",
    "overhang_fan_speed",
    "internal_bridge_fan_speed",
    "support_material_interface_fan_speed",
    "ironing_fan_speed",
    "additional_cooling_fan_speed",
    "activate_air_filtration",
    "during_print_exhaust_fan_speed",
    "complete_print_exhaust_fan_speed",
    "filament_retraction_length",
    "filament_z_hop",
    "filament_z_hop_types",
    "filament_retraction_speed",
    "filament_deretraction_speed",
    "filament_retraction_minimum_travel",
    "filament_retract_when_changing_layer",
    "filament_wipe",
    "filament_wipe_distance",
    "filament_retract_before_wipe",
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
    "machine_max_jerk_e",
    "machine_min_extruding_rate",
    "machine_min_travel_rate"
)

internal fun JSONObject.nativeToolCount(): Int =
    sequenceOf(
        optNativeArrayLength(NativeConfigKeys.Printer.NozzleDiameter),
        optNativeArrayLength(NativeConfigKeys.Printer.ExtruderOffset),
        optNativeArrayLength(NativeConfigKeys.Printer.ExtruderColor),
        optNativeArrayLength(NativeConfigKeys.Filament.Type),
        optString(NativeConfigKeys.Printer.ExtrudersCount).toIntOrNull()
    )
        .filterNotNull()
        .filter { it in 2..16 }
        .maxOrNull()
        ?: 1

internal fun JSONObject.expandActiveMaterialAcrossNativeToolVectors(toolCount: Int) {
    if (toolCount <= 1) return
    nativeToolVectorKeys.forEach { key ->
        if (!has(key)) return@forEach
        val value = opt(key) ?: return@forEach
        val length = nativeArrayLength(value)
        if (length >= toolCount) return@forEach
        val repeated = JSONArray()
        if (key == NativeConfigKeys.Filament.SelfIndex || key == NativeConfigKeys.Filament.Map) {
            repeat(toolCount) { index ->
                repeated.put(index + 1)
            }
        } else if (key == NativeConfigKeys.Printer.PhysicalExtruderMap) {
            repeat(toolCount) { index ->
                repeated.put(index)
            }
        } else {
            val scalar = nativeExpansionScalar(value)
            repeat(toolCount) {
                repeated.put(scalar)
            }
        }
        put(key, repeated)
    }
}

private fun JSONObject.optNativeArrayLength(key: String): Int =
    nativeArrayLength(opt(key))

internal fun nativeArrayLength(value: Any?): Int =
    when (value) {
        is JSONArray -> value.length()
        is String -> runCatching {
            val trimmed = value.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                JSONArray(trimmed).length()
            } else {
                0
            }
        }.getOrDefault(0)
        else -> 0
    }

internal fun nativeExpansionScalar(value: Any): Any =
    when (value) {
        is JSONArray -> value.opt(0) ?: ""
        is String -> {
            val trimmed = value.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                runCatching { JSONArray(trimmed).opt(0) ?: value }.getOrDefault(value)
            } else {
                value
            }
        }
        else -> value
    }
