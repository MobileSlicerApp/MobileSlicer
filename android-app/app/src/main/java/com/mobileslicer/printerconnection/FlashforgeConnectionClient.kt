package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.GcodeFlavor
import com.mobileslicer.profiles.PrinterProfile
import java.io.File

internal class FlashforgeConnectionClient(
    private val sendTcpConsoleCommands: (
        host: String,
        port: Int,
        commands: List<String>,
        delayBeforeMs: Long,
        delimiter: String
    ) -> NetworkResult,
    private val uploadFlashforgeFile: suspend (
        host: String,
        port: Int,
        file: File,
        remoteFileName: String,
        onProgress: (Int) -> Unit
    ) -> NetworkResult
) {
    fun testConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val host = printerHostName(baseUrl)
            ?: return PrinterConnectionResult(false, "Connection failed", "Flashforge host could not be parsed.")
        val port = profile.flashforgeConsolePort()
        val result = sendTcpConsoleCommands(host, port, listOf("~M601 S1"), 0L, "\r\n")
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Connection successful", "Flashforge TCP console responded at $host:$port.")
        } else {
            PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "Flashforge console did not respond.")
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
        val host = printerHostName(baseUrl)
            ?: return PrinterConnectionResult(false, "Send failed", "Flashforge host could not be parsed.")
        val port = profile.flashforgeConsolePort()
        val fileName = remoteFileName.substringAfterLast('/').substringAfterLast('\\')

        val connect = sendTcpConsoleCommands(host, port, profile.flashforgeConnectCommands(), 0L, "\r\n")
        if (!connect.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", connect.errorMessage ?: "Flashforge connection setup failed.")
        }

        val upload = uploadFlashforgeFile(host, port, file, fileName, onProgress)
        if (!upload.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "Flashforge rejected the upload.")
        }

        val save = sendTcpConsoleCommands(host, port, listOf("~M29"), 3_000L, "\r\n")
        if (!save.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", save.errorMessage ?: "Flashforge uploaded the file, but save failed.")
        }

        if (action == PrinterUploadAction.UploadAndStart) {
            val start = sendTcpConsoleCommands(host, port, listOf("~M23 0:/user/$fileName"), 0L, "\r\n")
            if (!start.isSuccess) {
                return PrinterConnectionResult(false, "Start failed", start.errorMessage ?: "Flashforge uploaded the file, but print start failed.")
            }
        }

        return successfulUpload(action, fileName)
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val host = printerHostName(baseUrl)
            ?: return PrinterConnectionResult(false, "Status unavailable", "Flashforge host could not be parsed.")
        val port = profile.flashforgeConsolePort()
        val result = sendTcpConsoleCommands(host, port, listOf("~M601 S1", "~M115", "~M119"), 0L, "\r\n")
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Printer status", "State: online • Host: ${safeDisplayUrl(baseUrl)} • Console: $port")
        } else {
            PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "Flashforge console did not respond.")
        }
    }

    private fun PrinterProfile.flashforgeConsolePort(): Int =
        printHostPort.trim().toIntOrNull()?.takeIf { it in 1..65535 } ?: 8899

    private fun PrinterProfile.flashforgeConnectCommands(): List<String> =
        listOf(
            "~M601 S1",
            "~M115",
            if (gcodeFlavor == GcodeFlavor.Klipper) "~M640" else "~M650",
            "~M119"
        )

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
