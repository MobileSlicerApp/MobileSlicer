package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import java.time.LocalDateTime

internal class FlashAirConnectionClient(
    private val requestTextBody: (url: String, method: String, headers: Map<String, String>) -> TextNetworkResult,
    private val uploadMultipartBody: suspend (
        url: String,
        headers: Map<String, String>,
        fields: Map<String, String>,
        file: File,
        fileFieldName: String,
        remoteFileName: String,
        offset: Long,
        length: Long,
        totalProgressBytes: Long,
        progressOffsetBytes: Long,
        onProgress: (Int) -> Unit
    ) -> TextNetworkResult
) {
    fun testConnection(baseUrl: String): PrinterConnectionResult {
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/command.cgi?op=118",
            "GET",
            emptyMap()
        )
        val body = result.body?.trim().orEmpty()
        return when {
            !result.isSuccess -> PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "FlashAir did not respond.")
            body.startsWith("1") -> PrinterConnectionResult(true, "Connection successful", "FlashAir upload is enabled at ${safeDisplayUrl(baseUrl)}.")
            else -> PrinterConnectionResult(false, "Connection failed", "FlashAir responded, but upload is not enabled. Set UPLOAD=1 in the card CONFIG.")
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
        val test = testConnection(baseUrl)
        if (!test.success) {
            return test.copy(title = "Send failed")
        }
        val uploadDir = profile.printHostPort.trim().ifBlank { "/" }.let {
            if (it.startsWith("/")) it else "/$it"
        }
        val prepare = requestTextBody(
            "${baseUrl.trimEnd('/')}/upload.cgi?WRITEPROTECT=ON&FTIME=${flashAirTimestamp()}",
            "GET",
            emptyMap()
        )
        if (!prepare.isSuccess || !prepare.body.orEmpty().contains("SUCCESS", ignoreCase = true)) {
            return PrinterConnectionResult(false, "Send failed", prepare.errorMessage ?: "FlashAir did not accept upload preparation.")
        }
        val setDir = requestTextBody(
            "${baseUrl.trimEnd('/')}/upload.cgi?UPDIR=${urlEncode(uploadDir)}",
            "GET",
            emptyMap()
        )
        if (!setDir.isSuccess || !setDir.body.orEmpty().contains("SUCCESS", ignoreCase = true)) {
            return PrinterConnectionResult(false, "Send failed", setDir.errorMessage ?: "FlashAir did not accept upload directory $uploadDir.")
        }
        val upload = uploadMultipartBody(
            "${baseUrl.trimEnd('/')}/upload.cgi",
            emptyMap(),
            emptyMap(),
            file,
            "file",
            remoteFileName,
            0L,
            file.length(),
            file.length().coerceAtLeast(1L),
            0L,
            onProgress
        )
        val body = upload.body.orEmpty()
        if (!upload.isSuccess || !body.contains("SUCCESS", ignoreCase = true)) {
            return PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "FlashAir rejected the upload.")
        }
        return if (action == PrinterUploadAction.UploadAndStart) {
            PrinterConnectionResult(true, "Upload complete", "$remoteFileName\nFlashAir upload does not support remote start.")
        } else {
            successfulUpload(action, remoteFileName)
        }
    }

    fun fetchStatus(baseUrl: String): PrinterConnectionResult {
        val test = testConnection(baseUrl)
        return if (test.success) {
            PrinterConnectionResult(true, "Printer status", "FlashAir upload enabled. Remote printer state is not available for this host type.")
        } else {
            test.copy(title = "Status unavailable")
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

    private fun flashAirTimestamp(): String {
        val now = LocalDateTime.now()
        val fatTime = ((now.year - 1980).coerceAtLeast(0) shl 25) or
            (now.monthValue shl 21) or
            (now.dayOfMonth shl 16) or
            (now.hour shl 11) or
            (now.minute shl 5) or
            (now.second / 2)
        return "0x${fatTime.toUInt().toString(16)}"
    }
}
