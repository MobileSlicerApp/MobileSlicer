package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

internal const val SIMPLYPRINT_PANEL_URL = "https://simplyprint.io/panel"

private const val SIMPLYPRINT_TOKEN_INFO_URL = "https://api.simplyprint.io/oauth2/TokenInfo"
private const val SIMPLYPRINT_TEMP_UPLOAD_URL = "https://simplyprint.io/api/files/TempUpload"
private const val SIMPLYPRINT_MAX_SINGLE_UPLOAD_BYTES = 100_000_000L
private const val SIMPLYPRINT_CHUNK_UPLOAD_BUFFER_BYTES = 99_000_000L
private const val SIMPLYPRINT_CHUNK_RECEIVE_URL = "https://api.simplyprint.io/0/files/ChunkReceive"

internal class SimplyPrintConnectionClient(
    private val requestTextBody: (url: String, method: String, headers: Map<String, String>) -> TextNetworkResult,
    private val requestFormBody: (url: String, fields: Map<String, String>, headers: Map<String, String>) -> TextNetworkResult,
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
    ) -> TextNetworkResult,
    private val uploadMultipartFieldsBody: (
        url: String,
        headers: Map<String, String>,
        fields: Map<String, String>
    ) -> TextNetworkResult
) {
    fun testConnection(profile: PrinterProfile): PrinterConnectionResult {
        val accessToken = resolveAccessToken(profile)
            ?: return PrinterConnectionResult(false, "Connection failed", "Log in to SimplyPrint or enter a current access token.")
        val result = requestTextBody(
            SIMPLYPRINT_TOKEN_INFO_URL,
            "GET",
            simplyPrintHeaders(accessToken)
        )
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Connection successful", "SimplyPrint API accepted the configured access token.")
        } else {
            PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "SimplyPrint did not accept the configured access token.")
        }
    }

    suspend fun uploadGcode(
        profile: PrinterProfile,
        file: File,
        remoteFileName: String,
        action: PrinterUploadAction,
        onProgress: (Int) -> Unit
    ): PrinterConnectionResult {
        if (action == PrinterUploadAction.UploadAndStart) {
            return PrinterConnectionResult(false, "Send failed", "SimplyPrint supports upload to queue/import only from Mobile Slicer.")
        }
        val test = testConnection(profile)
        if (!test.success) {
            return test.copy(title = "Send failed")
        }
        val accessToken = resolveAccessToken(profile)
            ?: return PrinterConnectionResult(false, "Send failed", "Log in to SimplyPrint or enter a current access token.")
        val fileName = remoteFileName.substringAfterLast('/').substringAfterLast('\\')
        val uuid = if (file.length() > SIMPLYPRINT_MAX_SINGLE_UPLOAD_BYTES) {
            uploadChunks(accessToken, file, fileName, onProgress)
                ?: return PrinterConnectionResult(false, "Send failed", "SimplyPrint chunked upload failed.")
        } else {
            uploadTempFile(accessToken, file, fileName, onProgress)
                ?: return PrinterConnectionResult(false, "Send failed", "SimplyPrint temp upload failed.")
        }
        val importUrl = "$SIMPLYPRINT_PANEL_URL?import=${formEncode("tmp:$uuid")}&filename=${formEncode(fileName)}"
        return when (action) {
            PrinterUploadAction.Queue -> PrinterConnectionResult(
                true,
                "Upload queued",
                "$fileName\nOpening SimplyPrint import panel: $importUrl",
                openUrl = importUrl
            )
            PrinterUploadAction.UploadOnly -> PrinterConnectionResult(
                true,
                "Upload complete",
                "$fileName\nOpening SimplyPrint import panel: $importUrl",
                openUrl = importUrl
            )
            PrinterUploadAction.UploadAndStart -> PrinterConnectionResult(false, "Send failed", "SimplyPrint supports upload to queue/import only from Mobile Slicer.")
        }
    }

    fun fetchStatus(profile: PrinterProfile): PrinterConnectionResult {
        val accessToken = resolveAccessToken(profile)
            ?: return PrinterConnectionResult(false, "Status unavailable", "Log in to SimplyPrint or enter a current access token.")
        val result = requestTextBody(
            SIMPLYPRINT_TOKEN_INFO_URL,
            "GET",
            simplyPrintHeaders(accessToken)
        )
        val body = result.body
        return if (result.isSuccess && body != null) {
            PrinterConnectionResult(true, "Printer status", statusLine(body))
        } else {
            PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "SimplyPrint did not return token status.")
        }
    }

    private suspend fun uploadTempFile(
        accessToken: String,
        file: File,
        fileName: String,
        onProgress: (Int) -> Unit
    ): String? {
        val upload = uploadMultipartBody(
            SIMPLYPRINT_TEMP_UPLOAD_URL,
            simplyPrintHeaders(accessToken),
            emptyMap(),
            file,
            "file",
            fileName,
            0L,
            file.length(),
            file.length().coerceAtLeast(1L),
            0L,
            onProgress
        )
        if (!upload.isSuccess) return null
        return runCatching { JSONObject(upload.body.orEmpty()).optString("uuid", "") }
            .getOrDefault("")
            .takeIf { it.isNotBlank() }
    }

    private suspend fun uploadChunks(
        accessToken: String,
        file: File,
        fileName: String,
        onProgress: (Int) -> Unit
    ): String? {
        val fileSize = file.length()
        val chunkSize = SIMPLYPRINT_CHUNK_UPLOAD_BUFFER_BYTES
        val chunkCount = ((fileSize + chunkSize - 1L) / chunkSize).toInt().coerceAtLeast(1)
        var chunkId = ""
        var deleteToken = ""
        val headers = simplyPrintHeaders(accessToken)
        for (index in 0 until chunkCount) {
            val offset = index * chunkSize
            val length = if (index == chunkCount - 1) fileSize - offset else chunkSize
            val query = buildMap {
                put("i", index.toString())
                put("temp", "true")
                if (index == 0) {
                    put("filename", fileName)
                    put("chunks", chunkCount.toString())
                    put("totalsize", fileSize.toString())
                } else {
                    put("id", chunkId)
                }
            }.toQueryString()
            val upload = uploadMultipartBody(
                "$SIMPLYPRINT_CHUNK_RECEIVE_URL?$query",
                headers,
                emptyMap(),
                file,
                "file",
                fileName,
                offset,
                length,
                fileSize,
                offset,
                onProgress
            )
            if (!upload.isSuccess) {
                cleanupChunk(accessToken, chunkId, deleteToken)
                return null
            }
            if (index == 0) {
                val json = runCatching { JSONObject(upload.body.orEmpty()) }.getOrNull() ?: return null
                chunkId = json.optString("id", "")
                deleteToken = json.optString("delete_token", "")
                if (chunkId.isBlank() || deleteToken.isBlank()) return null
            }
        }
        val complete = uploadMultipartFieldsBody(
            SIMPLYPRINT_TEMP_UPLOAD_URL,
            headers,
            mapOf("chunkId" to chunkId)
        )
        if (!complete.isSuccess) {
            cleanupChunk(accessToken, chunkId, deleteToken)
            return null
        }
        return runCatching { JSONObject(complete.body.orEmpty()).optString("uuid", "") }
            .getOrDefault("")
            .takeIf { it.isNotBlank() }
    }

    private fun cleanupChunk(accessToken: String, chunkId: String, deleteToken: String) {
        if (chunkId.isBlank() || deleteToken.isBlank()) return
        requestTextBody(
            "$SIMPLYPRINT_CHUNK_RECEIVE_URL?" + mapOf(
                "id" to chunkId,
                "temp" to "true",
                "delete" to deleteToken
            ).toQueryString(),
            "GET",
            simplyPrintHeaders(accessToken)
        )
    }

    private fun resolveAccessToken(profile: PrinterProfile): String? {
        val accessToken = profile.printHostApiKey.trim()
        if (accessToken.isNotBlank()) {
            val probe = requestTextBody(
                SIMPLYPRINT_TOKEN_INFO_URL,
                "GET",
                simplyPrintHeaders(accessToken)
            )
            if (probe.isSuccess) return accessToken
        }
        val refreshToken = profile.printHostPassword.trim()
        if (refreshToken.isBlank()) return null
        return refreshAccessToken(refreshToken)
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        val result = requestFormBody(
            SIMPLYPRINT_OAUTH_TOKEN_URL,
            mapOf(
                "grant_type" to "refresh_token",
                "client_id" to SIMPLYPRINT_OAUTH_CLIENT_ID,
                "refresh_token" to refreshToken
            ),
            mapOf("Accept" to "application/json")
        )
        if (!result.isSuccess) return null
        return runCatching { JSONObject(result.body.orEmpty()).optString("access_token", "") }
            .getOrDefault("")
            .takeIf { it.isNotBlank() }
    }

    private fun simplyPrintHeaders(accessToken: String): Map<String, String> = buildMap {
        put("Authorization", "Bearer $accessToken")
        put("User-Agent", "SimplyPrint Orca Plugin")
        put("Accept", "application/json")
    }

    private fun statusLine(body: String): String {
        val json = runCatching { JSONObject(body) }.getOrNull()
            ?: return "State: connected"
        return buildList {
            add("State: connected")
            json.optString("scope", "").takeIf { it.isNotBlank() }?.let { add("Scope: $it") }
            json.optJSONObject("user")?.let { user ->
                user.optString("name", "").takeIf { it.isNotBlank() }?.let { add("User: $it") }
                user.optString("email", "").takeIf { it.isNotBlank() }?.let { add("Email: $it") }
            }
            json.optString("user", "").takeIf { it.isNotBlank() }?.let { add("User: $it") }
            json.optString("expires_in", "").takeIf { it.isNotBlank() }?.let { add("Expires: ${it}s") }
        }.joinToString(" • ").ifBlank { "State: connected" }
    }

    private fun Map<String, String>.toQueryString(): String =
        entries.joinToString("&") { (key, value) -> "${formEncode(key)}=${formEncode(value)}" }

    private fun formEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}
