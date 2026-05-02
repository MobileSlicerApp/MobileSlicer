package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrintHostAuthorizationType
import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONArray
import org.json.JSONObject

internal class PrusaConnectionClient(
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
    private val uploadRawFile: suspend (
        url: String,
        method: String,
        headers: Map<String, String>,
        file: File,
        onProgress: (Int) -> Unit,
        successCodes: IntRange,
        contentType: String
    ) -> NetworkResult,
    private val fetchDigestChallenge: (targetUrl: String) -> Map<String, String>?
) {
    fun testConnectConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val probe = probePrusaLink(profile, baseUrl)
        return if (probe.success) {
            val method = if (probe.usePut) "PUT upload" else "multipart upload"
            PrinterConnectionResult(true, "Connection successful", "Prusa Connect API responded at ${safeDisplayUrl(baseUrl)}. Using $method.")
        } else {
            PrinterConnectionResult(false, "Connection failed", probe.error ?: "Prusa Connect did not respond.")
        }
    }

    suspend fun uploadConnectGcode(
        profile: PrinterProfile,
        baseUrl: String,
        file: File,
        remoteFileName: String,
        action: PrinterUploadAction,
        onProgress: (Int) -> Unit
    ): PrinterConnectionResult =
        uploadPrusaFamilyGcode(
            profile = profile,
            baseUrl = baseUrl,
            file = file,
            remoteFileName = remoteFileName,
            action = action,
            onProgress = onProgress,
            connectMultipartFields = true
        )

    fun fetchConnectStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val probe = probePrusaLink(profile, baseUrl)
        return if (probe.success) {
            PrinterConnectionResult(true, "Printer status", "State: connected • Host: ${safeDisplayUrl(baseUrl)}")
        } else {
            PrinterConnectionResult(false, "Status unavailable", probe.error ?: "Prusa Connect did not respond.")
        }
    }

    fun testLinkConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val probe = probePrusaLink(profile, baseUrl)
        return if (probe.success) {
            val method = if (probe.usePut) "PUT upload" else "multipart upload"
            PrinterConnectionResult(true, "Connection successful", "PrusaLink API responded at ${safeDisplayUrl(baseUrl)}. Using $method.")
        } else {
            PrinterConnectionResult(false, "Connection failed", probe.error ?: "PrusaLink did not respond.")
        }
    }

    suspend fun uploadLinkGcode(
        profile: PrinterProfile,
        baseUrl: String,
        file: File,
        remoteFileName: String,
        action: PrinterUploadAction,
        onProgress: (Int) -> Unit
    ): PrinterConnectionResult =
        uploadPrusaFamilyGcode(
            profile = profile,
            baseUrl = baseUrl,
            file = file,
            remoteFileName = remoteFileName,
            action = action,
            onProgress = onProgress,
            connectMultipartFields = false
        )

    fun fetchLinkStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val statusUrl = "${baseUrl.trimEnd('/')}/api/v1/status"
        val result = requestTextBody(
            statusUrl,
            "GET",
            profile.prusaLinkHeaders("GET", statusUrl)
        )
        if (!result.isSuccess) {
            val probe = probePrusaLink(profile, baseUrl)
            return if (probe.success) {
                PrinterConnectionResult(true, "Printer status", "State: online • Host: ${safeDisplayUrl(baseUrl)}")
            } else {
                PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "PrusaLink did not respond.")
            }
        }
        return PrinterConnectionResult(true, "Printer status", prusaLinkStatusLine(result.body.orEmpty(), baseUrl))
    }

    fun browseLinkStorage(profile: PrinterProfile, baseUrl: String): PrinterConnectionChoicesResult {
        val storageUrl = "${baseUrl.trimEnd('/')}/api/v1/storage"
        val result = requestTextBody(
            storageUrl,
            "GET",
            profile.prusaLinkHeaders("GET", storageUrl)
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            return PrinterConnectionChoicesResult(false, "Picker unavailable", result.errorMessage ?: "PrusaLink did not return storage.")
        }
        val choices = prusaLinkStorageChoices(body)
        return if (choices.isNotEmpty()) {
            PrinterConnectionChoicesResult(true, "Select PrusaLink storage", "Choose the writable storage path to store in Printer path or port.", choices)
        } else {
            PrinterConnectionChoicesResult(false, "Picker unavailable", "PrusaLink returned no writable storage with free space.")
        }
    }

    private suspend fun uploadPrusaFamilyGcode(
        profile: PrinterProfile,
        baseUrl: String,
        file: File,
        remoteFileName: String,
        action: PrinterUploadAction,
        onProgress: (Int) -> Unit,
        connectMultipartFields: Boolean
    ): PrinterConnectionResult {
        val probe = probePrusaLink(profile, baseUrl)
        if (!probe.success) {
            val name = if (connectMultipartFields) "Prusa Connect" else "PrusaLink"
            return PrinterConnectionResult(false, "Send failed", probe.error ?: "$name did not respond.")
        }

        val storage = profile.prusaLinkStoragePath()
        val fileName = remoteFileName.substringAfterLast('/').substringAfterLast('\\')
        val usePut = probe.usePut && !(connectMultipartFields && action == PrinterUploadAction.Queue)
        val upload = if (usePut) {
            val uploadUrl = "${baseUrl.trimEnd('/')}/api/v1/files$storage/${escapePathElements(fileName)}"
            val headers = profile.prusaLinkHeaders("PUT", uploadUrl).toMutableMap().apply {
                put("Overwrite", "?1")
                if (action == PrinterUploadAction.UploadAndStart) put("Print-After-Upload", "?1")
            }
            uploadRawFile(
                uploadUrl,
                "PUT",
                headers,
                file,
                onProgress,
                200..299,
                "text/x.gcode"
            )
        } else {
            val fields = if (connectMultipartFields) {
                buildMap {
                    put("path", "")
                    when (action) {
                        PrinterUploadAction.UploadAndStart -> put("to_print", "True")
                        PrinterUploadAction.Queue -> put("to_queue", "True")
                        PrinterUploadAction.UploadOnly -> Unit
                    }
                }
            } else {
                mapOf(
                    "path" to "",
                    "print" to (action == PrinterUploadAction.UploadAndStart).toString()
                )
            }
            val uploadUrl = "${baseUrl.trimEnd('/')}/api/files$storage"
            uploadMultipart(
                uploadUrl,
                profile.prusaLinkHeaders("POST", uploadUrl),
                fields,
                file,
                "file",
                fileName,
                onProgress
            )
        }
        if (!upload.isSuccess) {
            return PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "PrusaLink rejected the upload.")
        }
        return successfulUpload(action, fileName)
    }

    private fun probePrusaLink(profile: PrinterProfile, baseUrl: String): PrusaLinkProbe {
        val versionUrl = "${baseUrl.trimEnd('/')}/api/version"
        val result = requestTextBody(
            versionUrl,
            "GET",
            profile.prusaLinkHeaders("GET", versionUrl)
        )
        if (!result.isSuccess) return PrusaLinkProbe(false, error = result.errorMessage)
        return try {
            val json = JSONObject(result.body.orEmpty())
            if (!json.has("api")) {
                return PrusaLinkProbe(false, error = "Host did not return a PrusaLink API version response.")
            }
            val text = json.optString("text", "")
            val matches = text.startsWith("PrusaLink") || text.startsWith("OctoPrint")
            if (!matches) {
                return PrusaLinkProbe(false, error = "Host responded as ${text.ifBlank { "unknown" }}, not PrusaLink.")
            }
            val usePut = json.optJSONObject("capabilities")?.optBoolean("upload-by-put", false) ?: false
            PrusaLinkProbe(true, usePut = usePut)
        } catch (_: Exception) {
            PrusaLinkProbe(false, error = "Could not parse PrusaLink server response.")
        }
    }

    private fun PrinterProfile.prusaLinkHeaders(method: String, url: String): Map<String, String> {
        return when (printHostAuthorizationType) {
            PrintHostAuthorizationType.Key -> {
                val key = printHostApiKey.trim()
                if (key.isBlank()) emptyMap() else mapOf("X-Api-Key" to key)
            }
            PrintHostAuthorizationType.User -> {
                val user = printHostUser.trim()
                if (user.isBlank() && printHostPassword.isBlank()) {
                    emptyMap()
                } else {
                    prusaLinkDigestAuthorization(user, printHostPassword, method, url)
                        ?.let { mapOf("Authorization" to it) }
                        ?: emptyMap()
                }
            }
        }
    }

    private fun prusaLinkDigestAuthorization(
        username: String,
        password: String,
        method: String,
        url: String
    ): String? {
        val challenge = fetchDigestChallenge(url)
            ?: return null
        val uri = runCatching { URI(url) }.getOrNull()
            ?: return null
        val digestUri = buildString {
            append(uri.rawPath.ifBlank { "/" })
            if (!uri.rawQuery.isNullOrBlank()) {
                append('?')
                append(uri.rawQuery)
            }
        }
        val realm = challenge["realm"] ?: return null
        val nonce = challenge["nonce"] ?: return null
        val qop = challenge["qop"]
            ?.split(',')
            ?.map { it.trim().trim('"') }
            ?.firstOrNull { it.equals("auth", ignoreCase = true) }
        val algorithm = challenge["algorithm"]?.trim('"')?.ifBlank { "MD5" } ?: "MD5"
        if (!algorithm.equals("MD5", ignoreCase = true)) return null
        val nc = "00000001"
        val cnonce = randomHex(16)
        val ha1 = md5Hex("$username:$realm:$password")
        val ha2 = md5Hex("${method.uppercase()}:$digestUri")
        val response = if (qop != null) {
            md5Hex("$ha1:$nonce:$nc:$cnonce:$qop:$ha2")
        } else {
            md5Hex("$ha1:$nonce:$ha2")
        }
        return buildString {
            append("Digest ")
            appendDigestPair("username", username)
            append(", ")
            appendDigestPair("realm", realm)
            append(", ")
            appendDigestPair("nonce", nonce)
            append(", ")
            appendDigestPair("uri", digestUri)
            append(", ")
            appendDigestPair("response", response)
            append(", algorithm=MD5")
            if (challenge["opaque"] != null) {
                append(", ")
                appendDigestPair("opaque", challenge.getValue("opaque"))
            }
            if (qop != null) {
                append(", qop=$qop, nc=$nc, ")
                appendDigestPair("cnonce", cnonce)
            }
        }
    }

    private fun PrinterProfile.prusaLinkStoragePath(): String {
        val configured = printHostPort.trim().trimEnd('/')
        return when {
            configured.isBlank() -> "/local"
            configured.startsWith("/") -> configured
            else -> "/$configured"
        }
    }

    private fun prusaLinkStorageChoices(body: String): List<PrinterConnectionChoice> {
        val rootObject = runCatching { JSONObject(body) }.getOrNull()
        val storage = rootObject?.optJSONArray("storage_list")
            ?: rootObject?.optJSONArray("storage")
            ?: runCatching { JSONArray(body) }.getOrNull()
            ?: return emptyList()
        return (0 until storage.length())
            .asSequence()
            .mapNotNull { storage.optJSONObject(it) }
            .mapNotNull { item ->
                val path = item.optString("path", "").trim()
                if (path.isBlank()) return@mapNotNull null
                val available = !item.has("available") || item.optBoolean("available", true)
                val readOnly = item.optBoolean("read_only", item.optBoolean("ro", false))
                val freeSpace = item.optLong("free_space", 1L)
                if (!available || readOnly || freeSpace <= 0L) return@mapNotNull null
                val name = item.optString("name", "").ifBlank { path }
                val detail = if (freeSpace > 1L) "Free: ${formatBytes(freeSpace)}" else ""
                PrinterConnectionChoice(label = name, value = path, detail = detail)
            }
            .toList()
    }

    private fun prusaLinkStatusLine(body: String, baseUrl: String): String {
        val json = runCatching { JSONObject(body) }.getOrNull()
            ?: return "State: online • Host: ${safeDisplayUrl(baseUrl)}"
        return buildList {
            val state = json.optJSONObject("printer")?.optJSONObject("state")?.optString("text", "")
                ?: json.optJSONObject("state")?.optString("text", "")
                ?: json.optString("state", "")
            add("State: ${state.ifBlank { "online" }}")
            json.optJSONObject("job")?.let { job ->
                job.optString("file", "").ifBlank { job.optString("name", "") }
                    .takeIf { it.isNotBlank() }
                    ?.let { add("File: ${it.substringAfterLast('/')}") }
                val progress = job.optDouble("progress", Double.NaN)
                if (!progress.isNaN()) {
                    add("Progress: ${formatPercent(if (progress > 1.0) progress / 100.0 else progress)}")
                }
            }
            json.optJSONObject("telemetry")?.let { telemetry ->
                telemetry.optDouble("temp-nozzle", Double.NaN).takeIf { !it.isNaN() }?.let {
                    add("Nozzle: ${formatTemperature(it)}")
                }
                telemetry.optDouble("temp-bed", Double.NaN).takeIf { !it.isNaN() }?.let {
                    add("Bed: ${formatTemperature(it)}")
                }
            }
        }.joinToString(" • ").ifBlank { "State: online • Host: ${safeDisplayUrl(baseUrl)}" }
    }

    private fun escapePathElements(value: String): String =
        value.split('/').joinToString("/") { urlEncode(it) }

    private fun md5Hex(value: String): String =
        MessageDigest.getInstance("MD5")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private fun randomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount).also { SecureRandom().nextBytes(it) }
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun StringBuilder.appendDigestPair(name: String, value: String) {
        append(name)
        append("=\"")
        append(value.replace("\\", "\\\\").replace("\"", "\\\""))
        append('"')
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

    private data class PrusaLinkProbe(
        val success: Boolean,
        val usePut: Boolean = false,
        val error: String? = null
    )
}
