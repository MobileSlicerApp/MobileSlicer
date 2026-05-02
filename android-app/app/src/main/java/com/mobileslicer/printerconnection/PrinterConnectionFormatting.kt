package com.mobileslicer.printerconnection

internal fun formatTemperature(value: Double): String =
    if (value.isNaN()) "unknown" else "${value.toInt()}C"

internal fun formatPercent(progress: Double): String =
    "${(progress.coerceIn(0.0, 1.0) * 100.0).toInt()}%"

internal fun formatBytes(bytes: Long): String =
    when {
        bytes >= 1_073_741_824L -> "${bytes / 1_073_741_824L} GB"
        bytes >= 1_048_576L -> "${bytes / 1_048_576L} MB"
        bytes >= 1_024L -> "${bytes / 1_024L} KB"
        else -> "$bytes bytes"
    }

internal fun String.toBambuPackageFileName(): String {
    val cleaned = trim()
        .replace(Regex("""[\\/:*?"<>|]+"""), "_")
        .replace(Regex("""\s+"""), " ")
        .ifBlank { "mobile_slicer_output.gcode.3mf" }
    return when {
        cleaned.endsWith(".gcode.3mf", ignoreCase = true) -> cleaned
        cleaned.endsWith(".3mf", ignoreCase = true) -> cleaned.substringBeforeLast(".3mf") + ".gcode.3mf"
        cleaned.endsWith(".gcode", ignoreCase = true) -> cleaned.substringBeforeLast(".gcode") + ".gcode.3mf"
        else -> "$cleaned.gcode.3mf"
    }
}
