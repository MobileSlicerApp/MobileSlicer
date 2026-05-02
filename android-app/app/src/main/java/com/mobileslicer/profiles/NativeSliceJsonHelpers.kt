package com.mobileslicer.profiles

import org.json.JSONArray
import org.json.JSONObject

internal val nativeScalarListStringKeys = setOf(
    "bridge_acceleration",
    "hole_to_polyhole_threshold",
    "infill_anchor",
    "infill_anchor_max",
    "infill_combination_max_layer_height",
    "initial_layer_line_width",
    "inner_wall_line_width",
    "internal_bridge_fan_speed",
    "internal_bridge_speed",
    "internal_bridge_support_thickness",
    "internal_solid_infill_line_width",
    "line_width",
    "max_travel_detour_distance",
    "min_width_top_surface",
    "outer_wall_line_width",
    "overhang_1_4_speed",
    "overhang_2_4_speed",
    "overhang_3_4_speed",
    "overhang_4_4_speed",
    "overhang_fan_threshold",
    "scarf_joint_speed",
    "seam_gap",
    "sparse_infill_line_width",
    "support_line_width",
    "support_material_interface_fan_speed",
    "top_surface_line_width",
    "wipe_speed"
)

internal fun JSONObject.normalizeNativeScalarListStrings() {
    nativeScalarListStringKeys.forEach { key ->
        val value = opt(key) ?: return@forEach
        val scalar = when (value) {
            is JSONArray -> firstScalarString(value)
            is String -> value.firstScalarFromJsonListString() ?: value
            else -> null
        }
        if (!scalar.isNullOrBlank()) {
            put(key, scalar)
        }
    }
}

private val nativePointStringKeys = setOf(
    NativeConfigKeys.Bed.MeshMin,
    NativeConfigKeys.Bed.MeshMax,
    NativeConfigKeys.Bed.MeshProbeDistance
)

internal fun JSONObject.normalizeNativePointStrings() {
    nativePointStringKeys.forEach { key ->
        opt(key).toNativePointString()?.let { put(key, it) }
    }
}

internal fun firstScalarString(array: JSONArray): String? {
    for (index in 0 until array.length()) {
        val value = array.opt(index)?.toString()?.trim().orEmpty()
        if (value.isNotBlank()) return value
    }
    return null
}

internal fun String.firstScalarFromJsonListString(): String? {
    val trimmed = trim()
    if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return null
    val parsed = runCatching { JSONArray(trimmed) }.getOrNull() ?: return null
    return firstScalarString(parsed)
}

private fun Any?.toNativePointString(): String? {
    val values = when (this) {
        is JSONArray -> List(length()) { index -> opt(index)?.toString().orEmpty().trim().trim('"') }
        is String -> {
            val trimmed = trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val parsed = runCatching { JSONArray(trimmed) }.getOrNull() ?: return trimmed
                List(parsed.length()) { index -> parsed.opt(index)?.toString().orEmpty().trim().trim('"') }
            } else {
                return trimmed
            }
        }
        else -> return null
    }.filter { it.isNotBlank() && it != "nil" }
    if (values.size < 2) return null
    return "${values[0]},${values[1]}"
}

internal fun JSONObject.scalarString(key: String): String =
    when (val value = opt(key)) {
        is JSONArray -> firstScalarString(value).orEmpty()
        null -> ""
        else -> value.toString()
    }.trim().trim('"')

internal fun Any?.toNativeConfigListString(): String? =
    when (this) {
        is JSONArray -> {
            val values = List(length()) { index -> opt(index)?.toString().orEmpty().trim() }
                .filter { it.isNotBlank() }
            values.takeIf { it.isNotEmpty() }?.joinToString(",")
        }
        is String -> takeIf { it.isNotBlank() }
        else -> null
    }

internal fun JSONObject.mergeJsonObject(source: JSONObject?) {
    if (source == null) return
    val keys = source.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        put(key, source.opt(key))
    }
}

internal fun JSONObject.copyJsonObject(): JSONObject =
    JSONObject().also { copy ->
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            copy.put(key, copyJsonValue(opt(key)))
        }
    }

private fun copyJsonValue(value: Any?): Any? =
    when (value) {
        is JSONObject -> value.copyJsonObject()
        is JSONArray -> JSONArray().apply {
            for (index in 0 until value.length()) {
                put(copyJsonValue(value.opt(index)))
            }
        }
        else -> value
    }

internal fun JSONObject.canonicalConfigSignatureHash(): Int =
    canonicalJsonValue(this).toString().hashCode()

private fun canonicalJsonValue(value: Any?): Any? =
    when (value) {
        null, JSONObject.NULL -> JSONObject.NULL
        is JSONObject -> canonicalJsonObject(value)
        is JSONArray -> JSONArray().apply {
            for (index in 0 until value.length()) {
                put(canonicalJsonValue(value.opt(index)))
            }
        }
        is String -> value.canonicalNestedJsonStringOrSelf()
        else -> value
    }

private fun canonicalJsonObject(source: JSONObject): JSONObject =
    JSONObject().apply {
        val keys = source.keys().asSequence().toList().sorted()
        keys.forEach { key ->
            put(key, canonicalJsonValue(source.opt(key)))
        }
    }

private fun String.canonicalNestedJsonStringOrSelf(): String {
    val trimmed = trim()
    if (trimmed.length < 2) return this
    return when {
        trimmed.startsWith("{") && trimmed.endsWith("}") ->
            runCatching { canonicalJsonValue(JSONObject(trimmed)).toString() }.getOrDefault(this)
        trimmed.startsWith("[") && trimmed.endsWith("]") ->
            runCatching { canonicalJsonValue(JSONArray(trimmed)).toString() }.getOrDefault(this)
        else -> this
    }
}
