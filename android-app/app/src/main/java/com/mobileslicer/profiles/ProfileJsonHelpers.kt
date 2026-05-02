package com.mobileslicer.profiles

import org.json.JSONArray

internal fun JSONArray?.toProfileJsonStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}

internal fun JSONArray?.toProfileJsonFloatList(): List<Float> {
    if (this == null) return emptyList()
    return List(length()) { index -> optDouble(index, Double.NaN).toFloat() }
        .filter { it.isFinite() && it > 0f }
}
