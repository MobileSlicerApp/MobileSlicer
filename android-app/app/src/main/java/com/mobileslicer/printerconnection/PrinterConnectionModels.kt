package com.mobileslicer.printerconnection

internal enum class PrinterUploadAction {
    UploadOnly,
    UploadAndStart,
    Queue
}

internal data class PrinterConnectionResult(
    val success: Boolean,
    val title: String,
    val detail: String,
    val openUrl: String? = null
) {
    fun userMessage(): String = if (detail.isBlank()) title else "$title\n$detail"
}

internal data class PrinterConnectionChoice(
    val label: String,
    val value: String,
    val detail: String = ""
)

internal data class PrinterConnectionChoicesResult(
    val success: Boolean,
    val title: String,
    val detail: String,
    val choices: List<PrinterConnectionChoice> = emptyList()
) {
    fun userMessage(): String = if (detail.isBlank()) title else "$title\n$detail"
}
