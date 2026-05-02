package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

internal class ObicoConnectionClient(
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
    fun testConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        if (profile.printHostApiKey.trim().isBlank()) {
            return PrinterConnectionResult(false, "Connection failed", "Enter an Obico API token.")
        }
        val result = requestText(
            "${baseUrl.trimEnd('/')}/api/v1/version/",
            "GET",
            profile.obicoHeaders()
        )
        return if (result.isSuccess) {
            PrinterConnectionResult(true, "Connection successful", "Obico API responded at ${safeDisplayUrl(baseUrl)}.")
        } else {
            PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "Obico did not respond.")
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
        val printerId = profile.printHostPort.trim()
        if (printerId.isBlank()) {
            return PrinterConnectionResult(
                false,
                "Send failed",
                "Enter the Obico printer id in Profiles > Printer > Connection > Printer path or port."
            )
        }
        val test = testConnection(profile, baseUrl)
        if (!test.success) {
            return test.copy(title = "Send failed")
        }
        val fileName = remoteFileName.substringAfterLast('/').substringAfterLast('\\')
        val upload = uploadMultipart(
            "${baseUrl.trimEnd('/')}/api/v1/g_code_files/",
            profile.obicoHeaders(),
            mapOf(
                "print" to (action == PrinterUploadAction.UploadAndStart).toString(),
                "path" to remoteFileName.substringBeforeLast('/', ""),
                "printer_id" to printerId,
                "filename" to fileName
            ),
            file,
            "file",
            fileName,
            onProgress
        )
        return if (upload.isSuccess) {
            successfulUpload(action, fileName)
        } else {
            PrinterConnectionResult(false, "Send failed", upload.errorMessage ?: "Obico rejected the upload.")
        }
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        if (profile.printHostApiKey.trim().isBlank()) {
            return PrinterConnectionResult(false, "Status unavailable", "Enter an Obico API token.")
        }
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/api/v1/printers/",
            "GET",
            profile.obicoHeaders()
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            val test = testConnection(profile, baseUrl)
            return if (test.success) {
                PrinterConnectionResult(true, "Printer status", "State: connected • Host: ${safeDisplayUrl(baseUrl)}")
            } else {
                PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "Obico did not return printer status.")
            }
        }
        return PrinterConnectionResult(true, "Printer status", obicoStatusLine(body, profile.printHostPort.trim(), baseUrl))
    }

    fun browsePrinters(profile: PrinterProfile, baseUrl: String): PrinterConnectionChoicesResult {
        if (profile.printHostApiKey.trim().isBlank()) {
            return PrinterConnectionChoicesResult(false, "Picker unavailable", "Enter an Obico API token.")
        }
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/api/v1/printers/",
            "GET",
            profile.obicoHeaders()
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            return PrinterConnectionChoicesResult(false, "Picker unavailable", result.errorMessage ?: "Obico did not return printers.")
        }
        val choices = obicoPrinterChoices(body)
        return if (choices.isNotEmpty()) {
            PrinterConnectionChoicesResult(true, "Select Obico printer", "Choose the printer id to store in Printer path or port.", choices)
        } else {
            PrinterConnectionChoicesResult(false, "Picker unavailable", "Obico returned no printers.")
        }
    }

    private fun PrinterProfile.obicoHeaders(): Map<String, String> {
        val key = printHostApiKey.trim()
        return if (key.isBlank()) emptyMap() else mapOf("Authorization" to "Bearer $key")
    }

    private fun obicoPrinterChoices(body: String): List<PrinterConnectionChoice> {
        val rootObject = runCatching { JSONObject(body) }.getOrNull()
        val printers = runCatching { JSONArray(body) }.getOrNull()
            ?: rootObject?.optJSONArray("results")
            ?: rootObject?.optJSONArray("printers")
            ?: rootObject?.let { JSONArray().put(it) }
            ?: return emptyList()
        return (0 until printers.length())
            .asSequence()
            .mapNotNull { printers.optJSONObject(it) }
            .mapNotNull { printer ->
                val id = printer.optString("id", "")
                    .ifBlank { printer.optString("pk", "") }
                    .ifBlank { printer.optString("printer_id", "") }
                    .ifBlank { printer.optString("uuid", "") }
                    .trim()
                if (id.isBlank()) return@mapNotNull null
                val name = printer.optString("name", "")
                    .ifBlank { printer.optString("display_name", "") }
                    .ifBlank { printer.optString("printer_name", "") }
                    .ifBlank { id }
                val detail = printer.optString("status", "")
                    .ifBlank { printer.optString("state", "") }
                    .ifBlank { printer.optString("status_text", "") }
                    .takeIf { it.isNotBlank() }
                    ?.let { "State: $it" }
                    .orEmpty()
                PrinterConnectionChoice(label = name, value = id, detail = detail)
            }
            .toList()
    }

    private fun obicoStatusLine(body: String, printerId: String, baseUrl: String): String {
        val printers = runCatching { JSONArray(body) }.getOrNull()
            ?: runCatching { JSONObject(body).optJSONArray("results") }.getOrNull()
            ?: runCatching { JSONObject(body).optJSONArray("printers") }.getOrNull()
        if (printers == null) {
            val error = runCatching { JSONObject(body).optString("error", "") }.getOrDefault("")
            return error.takeIf { it.isNotBlank() } ?: "State: connected • Host: ${safeDisplayUrl(baseUrl)}"
        }

        val matchedPrinter = (0 until printers.length())
            .asSequence()
            .mapNotNull { printers.optJSONObject(it) }
            .firstOrNull { printer ->
                printerId.isNotBlank() && printer.optString("id", "") == printerId
            }
        val printer = matchedPrinter ?: printers.optJSONObject(0)

        return buildList {
            val state = printer?.optString("status", "")
                ?.ifBlank { printer.optString("state", "") }
                ?.ifBlank { printer.optString("status_text", "") }
                ?: ""
            add("State: ${state.ifBlank { "connected" }}")
            printer?.optString("name", "")?.takeIf { it.isNotBlank() }?.let { add("Printer: $it") }
            printer?.optString("id", "")?.takeIf { it.isNotBlank() }?.let { add("ID: $it") }
            val progress = printer?.optDouble("progress", Double.NaN) ?: Double.NaN
            if (!progress.isNaN()) {
                add("Progress: ${formatPercent(if (progress > 1.0) progress / 100.0 else progress)}")
            }
            if (printerId.isBlank() && printers.length() > 0) {
                add("Enter printer id to send")
            } else if (printerId.isNotBlank() && matchedPrinter == null) {
                add("Configured printer id not found")
            }
        }.joinToString(" • ")
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
