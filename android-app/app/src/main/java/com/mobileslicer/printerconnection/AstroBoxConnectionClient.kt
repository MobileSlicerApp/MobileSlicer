package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import org.json.JSONObject

internal class AstroBoxConnectionClient(
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
    fun testConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/api/version",
            "GET",
            profile.astroBoxHeaders()
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            return PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "AstroBox did not respond.")
        }
        return try {
            val json = JSONObject(body)
            val hasApi = json.has("api")
            val text = json.optString("text", "")
            val valid = hasApi && (text.isBlank() || text.startsWith("AstroBox", ignoreCase = true))
            if (valid) {
                PrinterConnectionResult(true, "Connection successful", "AstroBox API responded at ${safeDisplayUrl(baseUrl)}.")
            } else {
                PrinterConnectionResult(false, "Connection failed", "Host did not return an AstroBox API version response.")
            }
        } catch (_: Exception) {
            PrinterConnectionResult(false, "Connection failed", "Could not parse AstroBox server response.")
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
        val result = uploadMultipart(
            "${baseUrl.trimEnd('/')}/api/files/local",
            profile.astroBoxHeaders(),
            mapOf(
                "print" to (action == PrinterUploadAction.UploadAndStart).toString(),
                "path" to profile.printHostPort.trim()
            ),
            file,
            "file",
            remoteFileName,
            onProgress
        )
        return if (result.isSuccess) {
            successfulUpload(action, remoteFileName)
        } else {
            PrinterConnectionResult(false, "Send failed", result.errorMessage ?: "AstroBox rejected the upload.")
        }
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/api/version",
            "GET",
            profile.astroBoxHeaders()
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            return PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "AstroBox did not respond.")
        }
        val detail = runCatching {
            val json = JSONObject(body)
            buildList {
                add("State: online")
                json.optString("text", "").takeIf { it.isNotBlank() }?.let { add("Host: $it") }
                json.optString("api", "").takeIf { it.isNotBlank() }?.let { add("API: $it") }
            }.joinToString(" • ")
        }.getOrDefault("AstroBox responded.")
        return PrinterConnectionResult(true, "Printer status", detail)
    }

    private fun PrinterProfile.astroBoxHeaders(): Map<String, String> {
        val key = printHostApiKey.trim()
        return if (key.isBlank()) emptyMap() else mapOf("X-Api-Key" to key)
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
