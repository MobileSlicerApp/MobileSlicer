package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File

internal class ElegooLinkConnectionClient(
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
    ) -> NetworkResult,
    private val uploadElegooLinkChunks: suspend (
        profile: PrinterProfile,
        baseUrl: String,
        file: File,
        fileName: String,
        onProgress: (Int) -> Unit
    ) -> NetworkResult,
    private val startElegooLinkPrint: (baseUrl: String, fileName: String) -> NetworkResult,
    private val octoPrintHeaders: (profile: PrinterProfile) -> Map<String, String>
) {
    fun testConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val octoPrint = requestText(
            "${baseUrl.trimEnd('/')}/api/version",
            "GET",
            octoPrintHeaders(profile)
        )
        if (octoPrint.isSuccess) {
            return PrinterConnectionResult(true, "Connection successful", "OctoPrint-compatible Elegoo host responded at ${safeDisplayUrl(baseUrl)}.")
        }
        val result = requestTextBody(
            baseUrl.trimEnd('/'),
            "GET",
            octoPrintHeaders(profile)
        )
        val body = result.body.orEmpty()
        return if (result.isSuccess && body.contains("ELEGOO", ignoreCase = true)) {
            PrinterConnectionResult(true, "Connection successful", "Elegoo Link API responded at ${safeDisplayUrl(baseUrl)}.")
        } else {
            PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "Elegoo Link was not detected.")
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
        val octoPrint = requestText(
            "${baseUrl.trimEnd('/')}/api/version",
            "GET",
            octoPrintHeaders(profile)
        )
        if (octoPrint.isSuccess) {
            val upload = uploadMultipart(
                "${baseUrl.trimEnd('/')}/api/files/local",
                octoPrintHeaders(profile),
                mapOf("print" to (action == PrinterUploadAction.UploadAndStart).toString()),
                file,
                "file",
                remoteFileName,
                onProgress
            )
            return if (upload.isSuccess) {
                successfulUpload(action, remoteFileName)
            } else {
                PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "OctoPrint-compatible Elegoo host rejected the upload.")
            }
        }
        val nativeTest = testConnection(profile, baseUrl)
        if (!nativeTest.success) {
            return nativeTest.copy(title = "Send failed")
        }
        val fileName = remoteFileName.substringAfterLast('/').substringAfterLast('\\')
        val upload = uploadElegooLinkChunks(profile, baseUrl, file, fileName, onProgress)
        if (!upload.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "Elegoo Link rejected the upload.")
        }
        if (action == PrinterUploadAction.UploadAndStart) {
            val start = startElegooLinkPrint(baseUrl, fileName)
            if (!start.isSuccess) {
                return PrinterConnectionResult(false, "Start failed", start.errorMessage ?: "Elegoo Link uploaded the file, but start failed.")
            }
        }
        return successfulUpload(action, fileName)
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val test = testConnection(profile, baseUrl)
        return if (test.success) {
            PrinterConnectionResult(true, "Printer status", "Elegoo Link responded at ${safeDisplayUrl(baseUrl)}.")
        } else {
            test.copy(title = "Status unavailable")
        }
    }

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
