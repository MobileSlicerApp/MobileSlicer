package com.mobileslicer.printerconnection

internal enum class PrinterUploadAction {
    UploadOnly,
    UploadAndStart,
    Queue
}

internal enum class PrinterState {
    Online,
    Idle,
    Printing,
    Paused,
    Offline,
    Unknown
}

internal data class PrinterTemperature(
    val label: String,
    val currentCelsius: Double,
    val targetCelsius: Double? = null
)

internal data class PrinterRuntimeStatus(
    val reachable: Boolean,
    val state: PrinterState = PrinterState.Unknown,
    val message: String = "",
    val progressPercent: Int? = null,
    val temperatures: List<PrinterTemperature> = emptyList(),
    val currentFile: String? = null,
    val hostType: String = ""
)

internal data class PrinterConnectionResult(
    val success: Boolean,
    val title: String,
    val detail: String,
    val openUrl: String? = null,
    val runtimeStatus: PrinterRuntimeStatus? = null
) {
    fun userMessage(): String = if (detail.isBlank()) title else "$title\n$detail"
}

internal enum class PrinterBrowseTargetType {
    Host,
    StoragePath,
    PrinterTarget,
    Group
}

internal data class PrinterConnectionChoice(
    val label: String,
    val value: String,
    val detail: String = "",
    val targetType: PrinterBrowseTargetType = PrinterBrowseTargetType.PrinterTarget
)

internal data class PrinterConnectionChoicesResult(
    val success: Boolean,
    val title: String,
    val detail: String,
    val choices: List<PrinterConnectionChoice> = emptyList()
) {
    fun userMessage(): String = if (detail.isBlank()) title else "$title\n$detail"
}

internal fun PrinterConnectionChoicesResult.withTargetType(targetType: PrinterBrowseTargetType): PrinterConnectionChoicesResult =
    copy(choices = choices.map { choice -> choice.copy(targetType = targetType) })
