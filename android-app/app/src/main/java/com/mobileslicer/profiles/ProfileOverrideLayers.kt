package com.mobileslicer.profiles

import kotlin.math.abs
import org.json.JSONObject

internal fun PrinterProfile.withChangedNativePrinterOverridesFrom(initial: PrinterProfile): PrinterProfile {
    val overrides = changedNativeOverrides(
        baselineNative = JSONObject().putNativePrinterConfiguration(initial),
        updatedNative = JSONObject().putNativePrinterConfiguration(this),
        resolvedJson = initial.orcaResolvedMachineJson,
        existingOverridesJson = initial.orcaMachineOverridesJson
    )
    return copy(orcaMachineOverridesJson = overrides)
}

internal fun FilamentProfile.withChangedNativeFilamentOverridesFrom(initial: FilamentProfile): FilamentProfile {
    val overrides = changedNativeOverrides(
        baselineNative = JSONObject()
            .putNativeFilamentConfiguration(initial)
            .putNativeFilamentOverrideConfiguration(initial),
        updatedNative = JSONObject()
            .putNativeFilamentConfiguration(this)
            .putNativeFilamentOverrideConfiguration(this),
        resolvedJson = initial.orcaResolvedFilamentJson,
        existingOverridesJson = initial.orcaFilamentOverridesJson
    )
    return copy(orcaFilamentOverridesJson = overrides)
}

internal fun ProcessProfile.withChangedNativeProcessOverridesFrom(initial: ProcessProfile): ProcessProfile {
    val overrides = changedNativeOverrides(
        baselineNative = JSONObject().putNativeProcessConfiguration(initial),
        updatedNative = JSONObject().putNativeProcessConfiguration(this),
        resolvedJson = initial.orcaResolvedProcessJson,
        existingOverridesJson = initial.orcaProcessOverridesJson
    )
    return withValues("orcaProcessOverridesJson" to overrides)
}

private fun changedNativeOverrides(
    baselineNative: JSONObject,
    updatedNative: JSONObject,
    resolvedJson: String,
    existingOverridesJson: String
): String {
    val resolved = jsonObjectOrNull(resolvedJson)
    val overrides = jsonObjectOrNull(existingOverridesJson) ?: JSONObject()
    val keys = updatedNative.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val oldValue = baselineNative.opt(key)
        val newValue = updatedNative.opt(key)
        if (!jsonValuesEquivalent(oldValue, newValue)) {
            if (resolved != null && resolved.has(key) && jsonValuesEquivalent(resolved.opt(key), newValue)) {
                overrides.remove(key)
            } else {
                overrides.put(key, newValue)
            }
        }
    }
    return if (overrides.length() == 0) "" else overrides.toString()
}

internal fun jsonObjectOrNull(json: String): JSONObject? =
    runCatching { JSONObject(json) }.getOrNull()

internal fun jsonValuesEquivalent(left: Any?, right: Any?): Boolean {
    if (left === right) return true
    if (left == null || left == JSONObject.NULL) return right == null || right == JSONObject.NULL
    if (right == null || right == JSONObject.NULL) return false
    if (left is Number && right is Number) {
        return abs(left.toDouble() - right.toDouble()) < 0.0001
    }
    return left.toString() == right.toString()
}

internal fun String.jsonObjectLengthOrZero(): Int =
    jsonObjectOrNull(this)?.length() ?: 0
