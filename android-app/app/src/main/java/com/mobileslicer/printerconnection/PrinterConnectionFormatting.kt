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
