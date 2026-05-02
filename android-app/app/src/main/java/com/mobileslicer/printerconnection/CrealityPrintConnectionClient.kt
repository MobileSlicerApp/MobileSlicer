package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File

internal class CrealityPrintConnectionClient(
    private val requestTextBody: (url: String, method: String, headers: Map<String, String>) -> TextNetworkResult,
    private val uploadMultipart: suspend (
        url: String,
        headers: Map<String, String>,
        fields: Map<String, String>,
        file: File,
        fileFieldName: String,
        remoteFileName: String,
        onProgress: (Int) -> Unit
    ) -> NetworkResult,
    private val startPrint: (baseUrl: String, fileName: String) -> NetworkResult
) {
    fun testConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/info",
            "GET",
            profile.crealityPrintHeaders()
        )
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Connection successful", "CrealityPrint API responded at ${safeDisplayUrl(baseUrl)}.")
        } else {
            PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "CrealityPrint did not respond.")
        }
    }

    suspend fun uploadGcode(
        profile: PrinterProfile,
        baseUrl: String,
        file: File,
        remoteFileName: String,
        action: PrinterUploadAction,
        onProgress: (Int) -> Unit
    ): PrinterConnectionResult {
        val test = testConnection(profile, baseUrl)
        if (!test.success) {
            return test.copy(title = "Send failed")
        }
        val uploadFileName = remoteFileName.substringAfterLast('/').substringAfterLast('\\')
        val safeFileName = crealityPrintSafeFilename(uploadFileName)
        val parentPath = remoteFileName
            .replace('\\', '/')
            .substringBeforeLast('/', missingDelimiterValue = "")
        val upload = uploadMultipart(
            "${baseUrl.trimEnd('/')}/upload/${urlEncode(safeFileName)}",
            profile.crealityPrintHeaders(),
            mapOf("path" to parentPath),
            file,
            "file",
            uploadFileName,
            onProgress
        )
        if (!upload.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "CrealityPrint rejected the upload.")
        }
        if (action == PrinterUploadAction.UploadAndStart) {
            val start = startPrint(baseUrl, safeFileName)
            if (!start.isSuccess) {
                return PrinterConnectionResult(false, "Start failed", start.errorMessage ?: "CrealityPrint uploaded the file, but WebSocket start failed.")
            }
        }
        return successfulUpload(action, safeFileName)
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/info",
            "GET",
            profile.crealityPrintHeaders()
        )
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Printer status", "CrealityPrint API responded at ${safeDisplayUrl(baseUrl)}.")
        } else {
            PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "CrealityPrint did not respond.")
        }
    }

    private fun PrinterProfile.crealityPrintHeaders(): Map<String, String> {
        val key = printHostApiKey.trim()
        return if (key.isBlank()) emptyMap() else mapOf("Authorization" to "Bearer $key")
    }

    private fun crealityPrintSafeFilename(fileName: String): String =
        fileName.replace(' ', '_')

    private fun successfulUpload(action: PrinterUploadAction, remoteFileName: String): PrinterConnectionResult {
        val title = if (action == PrinterUploadAction.UploadAndStart) "Print started" else "Upload complete"
        val detail = if (action == PrinterUploadAction.UploadAndStart) {
            "Uploaded and started $remoteFileName."
        } else {
            "Uploaded $remoteFileName."
        }
        return PrinterConnectionResult(true, title, detail)
    }
}
