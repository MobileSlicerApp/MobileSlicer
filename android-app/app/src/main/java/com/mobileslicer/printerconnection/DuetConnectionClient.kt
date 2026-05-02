package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONObject

internal class DuetConnectionClient(
    private val requestText: (url: String, method: String, headers: Map<String, String>) -> NetworkResult,
    private val requestTextBody: (url: String, method: String, headers: Map<String, String>) -> TextNetworkResult,
    private val sendRawBody: (url: String, method: String, headers: Map<String, String>, body: String) -> NetworkResult,
    private val uploadRawFile: suspend (
        url: String,
        method: String,
        headers: Map<String, String>,
        file: File,
        onProgress: (Int) -> Unit,
        successCodes: IntRange,
        contentType: String
    ) -> NetworkResult
) {
    fun testConnection(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val connection = connectDuet(profile, baseUrl)
        return if (connection.type != DuetConnectionType.Error) {
            if (connection.type == DuetConnectionType.Rrf) disconnectDuet(baseUrl)
            PrinterConnectionResult(true, "Connection successful", "Duet ${connection.type.displayLabel} API responded at ${safeDisplayUrl(baseUrl)}.")
        } else {
            PrinterConnectionResult(false, "Connection failed", connection.error ?: "Could not connect to Duet.")
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
        val connection = connectDuet(profile, baseUrl)
        if (connection.type == DuetConnectionType.Error) {
            return PrinterConnectionResult(false, "Send failed", connection.error ?: "Could not connect to Duet.")
        }
        val encodedName = urlEncode(remoteFileName)
        val result = when (connection.type) {
            DuetConnectionType.Dsf -> uploadRawFile(
                "${baseUrl.trimEnd('/')}/machine/file/gcodes/$encodedName",
                "PUT",
                emptyMap(),
                file,
                onProgress,
                200..299,
                "application/octet-stream"
            )
            DuetConnectionType.Rrf -> uploadRawFile(
                "${baseUrl.trimEnd('/')}/rr_upload?name=0:/gcodes/$encodedName&${duetTimestamp()}",
                "POST",
                emptyMap(),
                file,
                onProgress,
                200..299,
                "application/octet-stream"
            )
            DuetConnectionType.Error -> NetworkResult.Failure(connection.error ?: "Could not connect to Duet.")
        }
        if (!result.isSuccess) {
            if (connection.type == DuetConnectionType.Rrf) disconnectDuet(baseUrl)
            return PrinterConnectionResult(false, "Send failed", result.errorMessage ?: "Duet rejected the upload.")
        }
        if (action == PrinterUploadAction.UploadAndStart) {
            val startResult = startDuetPrint(baseUrl, remoteFileName, connection.type)
            if (connection.type == DuetConnectionType.Rrf) disconnectDuet(baseUrl)
            if (!startResult.isSuccess) {
                return PrinterConnectionResult(false, "Start failed", startResult.errorMessage ?: "File uploaded, but print did not start.")
            }
        } else if (connection.type == DuetConnectionType.Rrf) {
            disconnectDuet(baseUrl)
        }
        return successfulUpload(action, remoteFileName)
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val connection = connectDuet(profile, baseUrl)
        if (connection.type == DuetConnectionType.Error) {
            return PrinterConnectionResult(false, "Status unavailable", connection.error ?: "Could not connect to Duet.")
        }
        val result = when (connection.type) {
            DuetConnectionType.Dsf -> requestTextBody(
                "${baseUrl.trimEnd('/')}/machine/status",
                "GET",
                emptyMap()
            )
            DuetConnectionType.Rrf -> requestTextBody(
                "${baseUrl.trimEnd('/')}/rr_status?type=2&${duetTimestamp()}",
                "GET",
                emptyMap()
            )
            DuetConnectionType.Error -> TextNetworkResult.Failure(connection.error ?: "Could not connect to Duet.")
        }
        if (connection.type == DuetConnectionType.Rrf) disconnectDuet(baseUrl)
        val body = result.body
        return if (result.isSuccess && body != null) {
            PrinterConnectionResult(true, "Printer status", duetStatusLine(body, connection.type))
        } else {
            PrinterConnectionResult(false, "Status unavailable", result.errorMessage ?: "Duet did not return status.")
        }
    }

    private fun connectDuet(profile: PrinterProfile, baseUrl: String): DuetConnection {
        val password = profile.printHostApiKey.ifBlank { "reprap" }
        val rrf = requestTextBody(
            "${baseUrl.trimEnd('/')}/rr_connect?password=${urlEncode(password)}&${duetTimestamp()}",
            "GET",
            emptyMap()
        )
        val rrfBody = rrf.body
        if (rrf.isSuccess && rrfBody != null) {
            val errorCode = runCatching { JSONObject(rrfBody).optInt("err", 0) }.getOrDefault(0)
            return when (errorCode) {
                0 -> DuetConnection(DuetConnectionType.Rrf)
                1 -> DuetConnection(DuetConnectionType.Error, "Wrong Duet password/API key.")
                2 -> DuetConnection(DuetConnectionType.Error, "Duet could not allocate a connection slot.")
                else -> DuetConnection(DuetConnectionType.Error, "Duet returned error code $errorCode.")
            }
        }
        val dsf = requestText(
            "${baseUrl.trimEnd('/')}/machine/status",
            "GET",
            emptyMap()
        )
        if (dsf.isSuccess) {
            return DuetConnection(DuetConnectionType.Dsf)
        }
        return DuetConnection(
            DuetConnectionType.Error,
            "RepRapFirmware ${safeDisplayUrl(baseUrl)}: ${rrf.errorMessage ?: "no response"}\nDSF ${safeDisplayUrl(baseUrl)}: ${dsf.errorMessage ?: "no response"}"
        )
    }

    private fun disconnectDuet(baseUrl: String) {
        requestText(
            "${baseUrl.trimEnd('/')}/rr_disconnect",
            "GET",
            emptyMap()
        )
    }

    private fun startDuetPrint(baseUrl: String, remoteFileName: String, connectionType: DuetConnectionType): NetworkResult {
        val encodedName = urlEncode(remoteFileName)
        return when (connectionType) {
            DuetConnectionType.Dsf -> sendRawBody(
                "${baseUrl.trimEnd('/')}/machine/code",
                "POST",
                emptyMap(),
                "M32 \"0:/gcodes/$remoteFileName\""
            )
            DuetConnectionType.Rrf -> requestText(
                "${baseUrl.trimEnd('/')}/rr_gcode?gcode=M32%20%220:/gcodes/$encodedName%22",
                "GET",
                emptyMap()
            )
            DuetConnectionType.Error -> NetworkResult.Failure("Could not connect to Duet.")
        }
    }

    private fun duetTimestamp(): String =
        "time=${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}"

    private fun duetStatusLine(body: String, connectionType: DuetConnectionType): String {
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return "Duet responded."
        return when (connectionType) {
            DuetConnectionType.Dsf -> duetDsfStatusLine(json)
            DuetConnectionType.Rrf -> duetRrfStatusLine(json)
            DuetConnectionType.Error -> "Duet status unavailable."
        }
    }

    private fun duetDsfStatusLine(json: JSONObject): String = buildList {
        val state = json.optString("state", json.optJSONObject("status")?.optString("state", "unknown") ?: "unknown")
        add("State: $state")
        json.optJSONObject("job")?.let { job ->
            job.optString("file", "").ifBlank { job.optString("fileName", "") }
                .takeIf { it.isNotBlank() }
                ?.let { add("File: ${it.substringAfterLast('/')}") }
            val progress = job.optDouble("filePosition", Double.NaN)
                .takeIf { !it.isNaN() }
                ?.let { filePosition ->
                    val fileSize = job.optDouble("fileSize", Double.NaN)
                    if (!fileSize.isNaN() && fileSize > 0.0) filePosition / fileSize else Double.NaN
                }
                ?: job.optDouble("lastFileProgress", Double.NaN)
            if (!progress.isNaN()) add("Progress: ${formatPercent(progress)}")
        }
        json.optJSONObject("sensors")?.optJSONObject("analog")?.optJSONArray("sensors")?.let { sensors ->
            for (index in 0 until sensors.length()) {
                val sensor = sensors.optJSONObject(index) ?: continue
                val name = sensor.optString("name").lowercase()
                val temp = sensor.optDouble("lastReading", Double.NaN)
                if (temp.isNaN()) continue
                when {
                    "bed" in name -> add("Bed: ${formatTemperature(temp)}")
                    "nozzle" in name || "tool" in name || "hotend" in name -> add("Nozzle: ${formatTemperature(temp)}")
                }
            }
        }
    }.joinToString(" • ")

    private fun duetRrfStatusLine(json: JSONObject): String = buildList {
        add("State: ${duetRrfStatusLabel(json.optString("status", "unknown"))}")
        json.optString("fileName", "").takeIf { it.isNotBlank() }?.let { add("File: ${it.substringAfterLast('/')}") }
        val fraction = json.optDouble("fraction_printed", -1.0)
        if (!fraction.isNaN()) {
            add("Progress: ${formatPercent(if (fraction > 1.0) fraction / 100.0 else fraction)}")
        }
        json.optJSONObject("temps")?.let { temps ->
            temps.optJSONArray("current")?.let { current ->
                current.optDouble(0, Double.NaN).takeIf { !it.isNaN() }?.let { add("Bed: ${formatTemperature(it)}") }
                current.optDouble(1, Double.NaN).takeIf { !it.isNaN() }?.let { add("Nozzle: ${formatTemperature(it)}") }
            }
        }
    }.joinToString(" • ")

    private fun duetRrfStatusLabel(status: String): String = when (status.lowercase()) {
        "i" -> "idle"
        "p" -> "printing"
        "s" -> "paused"
        "b" -> "busy"
        "c" -> "changing tool"
        "a" -> "paused"
        "d" -> "pausing"
        "r" -> "resuming"
        else -> status.ifBlank { "unknown" }
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

    private enum class DuetConnectionType(val displayLabel: String) {
        Rrf("RepRapFirmware"),
        Dsf("DSF"),
        Error("unknown")
    }

    private data class DuetConnection(
        val type: DuetConnectionType,
        val error: String? = null
    )
}
