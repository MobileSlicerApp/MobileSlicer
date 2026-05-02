package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrintHostType

internal fun PrinterConnectionResult.withInferredRuntimeStatus(hostType: PrintHostType): PrinterConnectionResult {
    if (runtimeStatus != null) return this
    if (title != "Printer status") return this
    return copy(
        runtimeStatus = PrinterRuntimeStatus(
            reachable = success,
            state = inferPrinterState(detail),
            message = detail,
            progressPercent = inferProgressPercent(detail),
            currentFile = inferCurrentFile(detail),
            hostType = hostType.configValue
        )
    )
}

private fun inferPrinterState(detail: String): PrinterState {
    val lowered = detail.lowercase()
    return when {
        !("state:" in lowered || "online" in lowered || "connected" in lowered || "idle" in lowered || "printing" in lowered) -> PrinterState.Unknown
        "printing" in lowered -> PrinterState.Printing
        "paused" in lowered -> PrinterState.Paused
        "idle" in lowered -> PrinterState.Idle
        "offline" in lowered -> PrinterState.Offline
        "online" in lowered || "connected" in lowered -> PrinterState.Online
        else -> PrinterState.Unknown
    }
}

private fun inferProgressPercent(detail: String): Int? {
    val match = Regex("""Progress:\s*(\d{1,3})%""", RegexOption.IGNORE_CASE).find(detail) ?: return null
    return match.groupValues.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 100)
}

private fun inferCurrentFile(detail: String): String? {
    val match = Regex("""File:\s*([^•\n]+)""", RegexOption.IGNORE_CASE).find(detail) ?: return null
    return match.groupValues.getOrNull(1)?.trim()?.ifBlank { null }
}
