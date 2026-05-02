package com.mobileslicer.printerconnection

import java.io.File

internal class Esp3dConnectionClient(
    private val requestText: (url: String, method: String, headers: Map<String, String>) -> NetworkResult,
    private val requestTextBody: (url: String, method: String, headers: Map<String, String>) -> TextNetworkResult,
    private val uploadMultipart: suspend (
        url: String,
        headers: Map<String, String>,
        fields: Map<String, String>,
        file: File,
        fileFieldName: String,
        remoteFileName: String,
        onProgress: (Int) -> Unit
    ) -> NetworkResult
) {
    fun testConnection(baseUrl: String): PrinterConnectionResult {
        val result = requestTextBody(esp3dCommandUrl(baseUrl, "M105"), "GET", emptyMap())
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Connection successful", "ESP3D command API responded at ${safeDisplayUrl(baseUrl)}.")
        } else {
            PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "ESP3D did not respond.")
        }
    }

    suspend fun uploadGcode(
        baseUrl: String,
        file: File,
        remoteFileName: String,
        action: PrinterUploadAction,
        onProgress: (Int) -> Unit
    ): PrinterConnectionResult {
        val test = testConnection(baseUrl)
        if (!test.success) {
            return test.copy(title = "Send failed")
        }
        val shortName = esp3dShortName(remoteFileName)
        val upload = uploadMultipart(
            "${baseUrl.trimEnd('/')}/upload_serial",
            mapOf("Connection" to "keep-alive"),
            emptyMap(),
            file,
            "file",
            shortName,
            onProgress
        )
        if (!upload.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "ESP3D rejected the upload.")
        }
        if (action == PrinterUploadAction.UploadAndStart) {
            Thread.sleep(1_500)
            val select = requestText(esp3dCommandUrl(baseUrl, "M23 $shortName"), "GET", emptyMap())
            if (!select.isSuccess) {
                return PrinterConnectionResult(false, "Start failed", select.errorMessage ?: "ESP3D uploaded the file, but M23 failed.")
            }
            val start = requestText(esp3dCommandUrl(baseUrl, "M24"), "GET", emptyMap())
            if (!start.isSuccess) {
                return PrinterConnectionResult(false, "Start failed", start.errorMessage ?: "ESP3D uploaded the file, but M24 failed.")
            }
        }
        return successfulUpload(action, shortName)
    }

    fun fetchStatus(baseUrl: String): PrinterConnectionResult {
        val result = requestTextBody(esp3dCommandUrl(baseUrl, "M105"), "GET", emptyMap())
        val body = result.body
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Printer status", esp3dStatusLine(body.orEmpty()))
        } else {
            PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "ESP3D did not respond.")
        }
    }

    private fun successfulUpload(action: PrinterUploadAction, remoteFileName: String): PrinterConnectionResult {
        val title = when (action) {
            PrinterUploadAction.UploadAndStart -> "Upload and start sent"
            PrinterUploadAction.Queue -> "Upload queued"
            PrinterUploadAction.UploadOnly -> "Upload complete"
        }
        return PrinterConnectionResult(true, title, remoteFileName)
    }

    private fun esp3dCommandUrl(baseUrl: String, command: String): String =
        "${baseUrl.trimEnd('/')}/command?plain=${urlEncode(command)}"

    private fun esp3dShortName(remoteFileName: String): String {
        val name = remoteFileName.substringAfterLast('/').substringAfterLast('\\')
        val stem = name.substringBeforeLast('.', name)
            .filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            .ifBlank { "upload" }
            .take(8)
        val ext = name.substringAfterLast('.', "")
            .filter { it.isLetterOrDigit() }
            .ifBlank { "gco" }
            .take(3)
        return "$stem.$ext"
    }

    private fun esp3dStatusLine(body: String): String {
        val nozzle = Regex("""(?:T|T0):\s*([0-9]+(?:\.[0-9]+)?)""")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
        val bed = Regex("""B:\s*([0-9]+(?:\.[0-9]+)?)""")
            .find(body)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()
        return buildList {
            add("State: online")
            nozzle?.let { add("Nozzle: ${formatTemperature(it)}") }
            bed?.let { add("Bed: ${formatTemperature(it)}") }
        }.joinToString(" • ")
    }
}
