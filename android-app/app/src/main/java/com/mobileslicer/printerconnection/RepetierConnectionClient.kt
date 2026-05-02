package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

internal class RepetierConnectionClient(
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
            "${baseUrl.trimEnd('/')}/printer/info",
            "GET",
            profile.repetierHeaders()
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            return PrinterConnectionResult(false, "Connection failed", result.errorMessage ?: "Repetier did not respond.")
        }
        return try {
            val json = JSONObject(body)
            val name = json.optString("name", "")
            val software = json.optString("software", "")
            val valid = when {
                software.isNotBlank() -> software == "Repetier-Server"
                name.isNotBlank() -> name.startsWith("Repetier", ignoreCase = true)
                else -> true
            }
            if (valid) {
                PrinterConnectionResult(true, "Connection successful", "Repetier API responded at ${safeDisplayUrl(baseUrl)}.")
            } else {
                PrinterConnectionResult(false, "Connection failed", "Host responded as ${software.ifBlank { name }}, not Repetier.")
            }
        } catch (_: Exception) {
            PrinterConnectionResult(false, "Connection failed", "Could not parse Repetier server response.")
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
        val printerSlug = profile.printHostPort.trim()
        if (printerSlug.isBlank()) {
            return PrinterConnectionResult(
                false,
                "Send failed",
                "Enter the Repetier printer slug in Profiles > Printer > Connection > Printer path or port."
            )
        }
        val test = testConnection(profile, baseUrl)
        if (!test.success) {
            return test.copy(title = "Send failed")
        }
        val encodedSlug = urlEncode(printerSlug)
        val start = action == PrinterUploadAction.UploadAndStart
        val result = uploadMultipart(
            "${baseUrl.trimEnd('/')}/printer/${if (start) "job" else "model"}/$encodedSlug",
            profile.repetierHeaders(),
            buildMap {
                put("a", "upload")
                if (start) {
                    put("name", remoteFileName)
                    put("autostart", "true")
                } else {
                    profile.printHostGroup.trim()
                        .takeIf { it.isNotBlank() && !it.equals("Default", ignoreCase = true) }
                        ?.let { put("group", it) }
                }
            },
            file,
            "filename",
            remoteFileName,
            onProgress
        )
        return if (result.isSuccess) {
            successfulUpload(action, remoteFileName)
        } else {
            PrinterConnectionResult(false, "Send failed", result.errorMessage ?: "Repetier rejected the upload.")
        }
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val printerSlug = profile.printHostPort.trim()
        if (printerSlug.isBlank()) {
            return PrinterConnectionResult(
                false,
                "Status unavailable",
                "Enter the Repetier printer slug in Profiles > Printer > Connection > Printer path or port."
            )
        }
        val listResult = requestTextBody(
            "${baseUrl.trimEnd('/')}/printer/list",
            "GET",
            profile.repetierHeaders()
        )
        val listBody = listResult.body
        if (!listResult.isSuccess || listBody == null) {
            return PrinterConnectionResult(false, "Status unavailable", listResult.errorMessage ?: "Repetier did not return printer status.")
        }
        val stateResult = requestTextBody(
            "${baseUrl.trimEnd('/')}/printer/api/${urlEncode(printerSlug)}?a=stateList&includeHistory=false",
            "GET",
            profile.repetierHeaders()
        )
        val detail = repetierStatusLine(
            listBody = listBody,
            stateBody = stateResult.body,
            printerSlug = printerSlug
        )
        return PrinterConnectionResult(true, "Printer status", detail)
    }

    fun browsePrinters(profile: PrinterProfile, baseUrl: String): PrinterConnectionChoicesResult {
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/printer/list",
            "GET",
            profile.repetierHeaders()
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            return PrinterConnectionChoicesResult(false, "Picker unavailable", result.errorMessage ?: "Repetier did not return printers.")
        }
        val choices = repetierPrinterChoices(body)
        return if (choices.isNotEmpty()) {
            PrinterConnectionChoicesResult(true, "Select Repetier printer", "Choose the printer slug to store in Printer path or port.", choices)
        } else {
            PrinterConnectionChoicesResult(false, "Picker unavailable", "Repetier returned no printers.")
        }
    }

    fun browseGroups(profile: PrinterProfile, baseUrl: String): PrinterConnectionChoicesResult {
        val printerSlug = profile.printHostPort.trim()
        if (printerSlug.isBlank()) {
            return PrinterConnectionChoicesResult(false, "Picker unavailable", "Select or enter a Repetier printer slug first.")
        }
        val result = requestTextBody(
            "${baseUrl.trimEnd('/')}/printer/api/${urlEncode(printerSlug)}?a=listModelGroups",
            "GET",
            profile.repetierHeaders()
        )
        val body = result.body
        if (!result.isSuccess || body == null) {
            return PrinterConnectionChoicesResult(false, "Picker unavailable", result.errorMessage ?: "Repetier did not return model groups.")
        }
        val choices = repetierGroupChoices(body)
        return if (choices.isNotEmpty()) {
            PrinterConnectionChoicesResult(true, "Select Repetier group", "Choose the model group used for upload-only sends.", choices)
        } else {
            PrinterConnectionChoicesResult(false, "Picker unavailable", "Repetier returned no model groups.")
        }
    }

    private fun PrinterProfile.repetierHeaders(): Map<String, String> {
        val key = printHostApiKey.trim()
        return if (key.isBlank()) emptyMap() else mapOf("X-Api-Key" to key)
    }

    private fun repetierPrinterChoices(body: String): List<PrinterConnectionChoice> {
        val rootObject = runCatching { JSONObject(body) }.getOrNull()
        val printers = rootObject?.optJSONArray("data")
            ?: runCatching { JSONArray(body) }.getOrNull()
            ?: return emptyList()
        return (0 until printers.length())
            .asSequence()
            .mapNotNull { printers.optJSONObject(it) }
            .mapNotNull { printer ->
                val slug = printer.optString("slug", "")
                    .ifBlank { printer.optString("id", "") }
                    .ifBlank { printer.optString("name", "") }
                    .trim()
                if (slug.isBlank()) return@mapNotNull null
                val name = printer.optString("name", "").ifBlank { slug }
                val detail = buildList {
                    printer.optString("online", "").takeIf { it.isNotBlank() }?.let { add("Online: $it") }
                    printer.optString("job", "").takeIf { it.isNotBlank() && it != "none" }?.let { add("Job: $it") }
                }.joinToString(" • ")
                PrinterConnectionChoice(label = name, value = slug, detail = detail)
            }
            .toList()
    }

    private fun repetierGroupChoices(body: String): List<PrinterConnectionChoice> {
        val rootObject = runCatching { JSONObject(body) }.getOrNull() ?: return emptyList()
        val groups = rootObject.optJSONObject("groupNames")
            ?: rootObject.optJSONArray("groupNames")?.let { array ->
                return (0 until array.length()).mapNotNull { index ->
                    val raw = array.optString(index, "").ifBlank { return@mapNotNull null }
                    val value = if (raw == "#") "Default" else raw
                    PrinterConnectionChoice(label = value, value = value)
                }
            }
            ?: return emptyList()
        return groups.keys().asSequence()
            .mapNotNull { key ->
                val raw = groups.optString(key, "").ifBlank { key }
                val value = if (raw == "#") "Default" else raw
                PrinterConnectionChoice(label = value, value = value)
            }
            .toList()
    }

    private fun repetierStatusLine(listBody: String, stateBody: String?, printerSlug: String): String {
        val printer = runCatching {
            val printers = JSONArray(listBody)
            (0 until printers.length())
                .asSequence()
                .mapNotNull { printers.optJSONObject(it) }
                .firstOrNull { it.optString("slug") == printerSlug }
        }.getOrNull()
        val state = runCatching {
            val json = JSONObject(stateBody.orEmpty())
            json.optJSONObject(printerSlug)
                ?: json.optJSONObject("state")
                ?: json
        }.getOrNull()
        return buildList {
            val name = printer?.optString("name", "")?.takeIf { it.isNotBlank() }
            val online = printer?.optInt("online", -1) ?: -1
            val active = printer?.optBoolean("active", false) ?: false
            val job = printer?.optString("job", "")?.takeIf { it.isNotBlank() && it != "none" }
            add(
                "State: " + when {
                    job != null -> "printing"
                    online > 0 -> "online"
                    active -> "active"
                    online == 0 -> "offline"
                    else -> "unknown"
                }
            )
            name?.let { add("Printer: $it") }
            job?.let { add("Job: $it") }
            state?.optJSONArray("extruder")?.optJSONObject(0)?.let {
                add("Nozzle: ${formatTemperature(it.optDouble("tempRead", Double.NaN))}")
            }
            state?.optJSONObject("heatedBed")?.let {
                add("Bed: ${formatTemperature(it.optDouble("tempRead", Double.NaN))}")
            }
            state?.optInt("layer", -1)?.takeIf { it >= 0 }?.let { add("Layer: $it") }
        }.joinToString(" • ").ifBlank { "Repetier responded." }
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
