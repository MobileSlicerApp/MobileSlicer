package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import org.json.JSONObject

internal class MksConnectionClient(
    private val printerHostName: (String) -> String?,
    private val safeDisplayUrl: (String) -> String,
    private val urlEncode: (String) -> String,
    private val sendTcpConsoleCommands: (
        host: String,
        port: Int,
        commands: List<String>,
        delayBeforeMs: Long
    ) -> NetworkResult,
    private val uploadRawFileBody: suspend (
        url: String,
        method: String,
        headers: Map<String, String>,
        file: File,
        onProgress: (Int) -> Unit,
        successCodes: IntRange
    ) -> TextNetworkResult
) {
    fun testConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val host = printerHostName(baseUrl)
            ?: return PrinterConnectionResult(false, "Connection failed", "MKS host could not be parsed.")
        val port = profile.mksConsolePort()
        val result = sendTcpConsoleCommands(host, port, listOf("M105"), 0L)
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Connection successful", "MKS TCP console responded at $host:$port.")
        } else {
            PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "MKS console did not respond.")
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
        if (!test.success) return test.copy(title = "Send failed")

        val upload = uploadRawFileBody(
            "${baseUrl.trimEnd('/')}/upload?X-Filename=${urlEncode(remoteFileName)}",
            "POST",
            emptyMap(),
            file,
            onProgress,
            200..299
        )
        if (!upload.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "MKS rejected the upload.")
        }

        val errCode = runCatching { JSONObject(upload.body.orEmpty()).optInt("err", 0) }.getOrDefault(0)
        if (errCode != 0) {
            return PrinterConnectionResult(false, "Send failed", "MKS upload returned error code $errCode.")
        }

        if (action == PrinterUploadAction.UploadAndStart) {
            val host = printerHostName(baseUrl)
                ?: return PrinterConnectionResult(false, "Start failed", "MKS host could not be parsed.")
            val start = sendTcpConsoleCommands(
                host,
                profile.mksConsolePort(),
                listOf("M23 $remoteFileName", "M24"),
                1_500L
            )
            if (!start.isSuccess) {
                return PrinterConnectionResult(false, "Start failed", start.errorMessage ?: "MKS uploaded the file, but print start failed.")
            }
        }

        return successfulUpload(action, remoteFileName)
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val test = testConnection(profile, baseUrl)
        return if (test.success) {
            PrinterConnectionResult(true, "Printer status", "State: online • Host: ${safeDisplayUrl(baseUrl)} • Console: ${profile.mksConsolePort()}")
        } else {
            test.copy(title = "Status unavailable")
        }
    }

    private fun successfulUpload(action: PrinterUploadAction, remoteFileName: String): PrinterConnectionResult {
        val title = when (action) {
            PrinterUploadAction.UploadOnly -> "Upload complete"
            PrinterUploadAction.UploadAndStart -> "Print started"
            PrinterUploadAction.Queue -> "Upload queued"
        }
        val detail = when (action) {
            PrinterUploadAction.UploadOnly -> remoteFileName
            PrinterUploadAction.UploadAndStart -> "$remoteFileName\nThe printer accepted the file and start command."
            PrinterUploadAction.Queue -> "$remoteFileName\nThe file was added to the printer queue."
        }
        return PrinterConnectionResult(true, title, detail)
    }

    private fun PrinterProfile.mksConsolePort(): Int =
        printHostPort.trim().toIntOrNull() ?: 8080
}
