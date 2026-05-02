package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrinterProfile
import java.io.File
import org.json.JSONObject

internal class OctoKlipperConnectionClient(
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
        val failures = mutableListOf<String>()
        for (candidate in printerHostCandidates(baseUrl)) {
            val octoPrint = requestText(
                "$candidate/api/version",
                "GET",
                profile.authHeaders(PrinterProtocol.OctoPrint)
            )
            if (octoPrint.isSuccess) {
                return PrinterConnectionResult(true, "Connection successful", "OctoPrint API responded at ${safeDisplayUrl(candidate)}.")
            }
            failures += "OctoPrint ${safeDisplayUrl(candidate)}: ${octoPrint.errorMessage ?: "no response"}"

            val moonraker = requestText(
                "$candidate/server/info",
                "GET",
                profile.authHeaders(PrinterProtocol.Moonraker)
            )
            if (moonraker.isSuccess) {
                return PrinterConnectionResult(true, "Connection successful", "Moonraker/Klipper API responded at ${safeDisplayUrl(candidate)}.")
            }
            failures += "Moonraker ${safeDisplayUrl(candidate)}: ${moonraker.errorMessage ?: "no response"}"
        }

        return PrinterConnectionResult(false, "Connection failed", failures.joinToString("\n").ifBlank { "Printer did not respond." })
    }

    suspend fun uploadGcode(
        profile: PrinterProfile,
        baseUrl: String,
        file: File,
        remoteFileName: String,
        action: PrinterUploadAction,
        onProgress: (Int) -> Unit
    ): PrinterConnectionResult {
        val failures = mutableListOf<String>()
        for (candidate in printerHostCandidates(baseUrl)) {
            val octoProbe = requestText(
                "$candidate/api/version",
                "GET",
                profile.authHeaders(PrinterProtocol.OctoPrint)
            )
            if (octoProbe.isSuccess) {
                val result = uploadMultipart(
                    "$candidate/api/files/local",
                    profile.authHeaders(PrinterProtocol.OctoPrint),
                    mapOf("print" to (action == PrinterUploadAction.UploadAndStart).toString()),
                    file,
                    "file",
                    remoteFileName,
                    onProgress
                )
                if (result.isSuccess) {
                    return successfulUpload(action, remoteFileName)
                }
                failures += "OctoPrint upload ${safeDisplayUrl(candidate)}: ${result.errorMessage ?: "failed"}"
                continue
            }
            failures += "OctoPrint probe ${safeDisplayUrl(candidate)}: ${octoProbe.errorMessage ?: "no response"}"

            val moonrakerProbe = requestText(
                "$candidate/server/info",
                "GET",
                profile.authHeaders(PrinterProtocol.Moonraker)
            )
            if (!moonrakerProbe.isSuccess) {
                failures += "Moonraker probe ${safeDisplayUrl(candidate)}: ${moonrakerProbe.errorMessage ?: "no response"}"
                continue
            }
            val result = uploadMultipart(
                "$candidate/server/files/upload",
                profile.authHeaders(PrinterProtocol.Moonraker),
                mapOf(
                    "root" to "gcodes",
                    "print" to "false"
                ),
                file,
                "file",
                remoteFileName,
                onProgress
            )
            if (!result.isSuccess) {
                failures += "Moonraker upload ${safeDisplayUrl(candidate)}: ${result.errorMessage ?: "failed"}"
                continue
            }
            if (action == PrinterUploadAction.UploadAndStart) {
                val startResult = sendMoonrakerGcode(
                    baseUrl = candidate,
                    headers = profile.authHeaders(PrinterProtocol.Moonraker),
                    gcode = "SDCARD_PRINT_FILE FILENAME=${remoteFileName.moonrakerFilenameArgument()}"
                )
                if (!startResult.isSuccess) {
                    return PrinterConnectionResult(false, "Start failed", startResult.errorMessage ?: "File uploaded, but print did not start.")
                }
            }
            return successfulUpload(action, remoteFileName)
        }

        return PrinterConnectionResult(false, "Send failed", failures.joinToString("\n").ifBlank { "Printer rejected the upload." })
    }

    fun fetchStatus(profile: PrinterProfile, baseUrl: String): PrinterConnectionResult {
        val failures = mutableListOf<String>()
        for (candidate in printerHostCandidates(baseUrl)) {
            val moonraker = requestTextBody(
                "$candidate/printer/objects/query?print_stats&heater_bed&extruder&virtual_sdcard",
                "GET",
                profile.authHeaders(PrinterProtocol.Moonraker)
            )
            val moonrakerBody = moonraker.body
            if (moonraker.isSuccess && moonrakerBody != null) {
                return moonrakerStatusResult(moonrakerBody)
            }
            failures += "Moonraker ${safeDisplayUrl(candidate)}: ${moonraker.errorMessage ?: "no response"}"

            val octoJob = requestTextBody(
                "$candidate/api/job",
                "GET",
                profile.authHeaders(PrinterProtocol.OctoPrint)
            )
            val octoJobBody = octoJob.body
            if (octoJob.isSuccess && octoJobBody != null) {
                return octoPrintStatusResult(octoJobBody)
            }
            failures += "OctoPrint ${safeDisplayUrl(candidate)}: ${octoJob.errorMessage ?: "no response"}"
        }

        return PrinterConnectionResult(false, "Status unavailable", failures.joinToString("\n").ifBlank { "Printer did not respond." })
    }

    private fun sendMoonrakerGcode(baseUrl: String, headers: Map<String, String>, gcode: String): NetworkResult {
        val fields = mapOf("script" to gcode).toQueryString()
        return requestText(
            "${baseUrl.trimEnd('/')}/printer/gcode/script?$fields",
            "POST",
            headers
        )
    }

    private fun String.moonrakerFilenameArgument(): String {
        val escaped = replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }

    private fun moonrakerStatusResult(body: String): PrinterConnectionResult {
        val status = JSONObject(body)
            .optJSONObject("result")
            ?.optJSONObject("status")
            ?: return PrinterConnectionResult(
                success = true,
                title = "Printer status",
                detail = "Moonraker responded.",
                runtimeStatus = PrinterRuntimeStatus(
                    reachable = true,
                    state = PrinterState.Online,
                    message = "Moonraker responded.",
                    hostType = "moonraker"
                )
            )
        val printStats = status.optJSONObject("print_stats")
        val virtualSdcard = status.optJSONObject("virtual_sdcard")
        val bed = status.optJSONObject("heater_bed")
        val extruder = status.optJSONObject("extruder")
        val rawState = printStats?.optString("state", "unknown") ?: "unknown"
        val progress = virtualSdcard?.optDouble("progress", Double.NaN)
            ?.takeIf { !it.isNaN() }
            ?.let { (it.coerceIn(0.0, 1.0) * 100.0).toInt() }
        val currentFile = printStats?.optString("filename", "")?.takeIf { it.isNotBlank() }
        val temperatures = buildList {
            extruder?.toPrinterTemperature("Nozzle")?.let { add(it) }
            bed?.toPrinterTemperature("Bed")?.let { add(it) }
        }
        val runtimeStatus = PrinterRuntimeStatus(
            reachable = true,
            state = rawState.toPrinterState(),
            message = rawState,
            progressPercent = progress,
            temperatures = temperatures,
            currentFile = currentFile,
            hostType = "moonraker"
        )
        return PrinterConnectionResult(
            success = true,
            title = "Printer status",
            detail = runtimeStatus.toStatusLine(rawState),
            runtimeStatus = runtimeStatus
        )
    }

    private fun octoPrintStatusResult(body: String): PrinterConnectionResult {
        val json = JSONObject(body)
        val rawState = json.optString("state", "unknown")
        val fileName = json.optJSONObject("job")?.optJSONObject("file")?.optString("name", "").orEmpty()
        val progress = json.optJSONObject("progress")
            ?.optDouble("completion", Double.NaN)
            ?.takeIf { !it.isNaN() }
            ?.let { it.coerceIn(0.0, 100.0).toInt() }
        val runtimeStatus = PrinterRuntimeStatus(
            reachable = true,
            state = rawState.toPrinterState(),
            message = rawState,
            progressPercent = progress,
            currentFile = fileName.ifBlank { null },
            hostType = "octoprint"
        )
        return PrinterConnectionResult(
            success = true,
            title = "Printer status",
            detail = runtimeStatus.toStatusLine(rawState),
            runtimeStatus = runtimeStatus
        )
    }

    private fun JSONObject.toPrinterTemperature(label: String): PrinterTemperature? {
        val current = optDouble("temperature", Double.NaN).takeIf { !it.isNaN() } ?: return null
        val target = optDouble("target", Double.NaN).takeIf { !it.isNaN() }
        return PrinterTemperature(label = label, currentCelsius = current, targetCelsius = target)
    }

    private fun PrinterRuntimeStatus.toStatusLine(rawState: String): String =
        buildList {
            add("State: ${rawState.ifBlank { "unknown" }}")
            currentFile?.takeIf { it.isNotBlank() }?.let { add("File: $it") }
            progressPercent?.let { add("Progress: ${it.coerceIn(0, 100)}%") }
            temperatures.forEach { temperature ->
                add("${temperature.label}: ${formatTemperature(temperature.currentCelsius)}")
            }
        }.joinToString(" • ")

    private fun String.toPrinterState(): PrinterState {
        val lowered = lowercase()
        return when {
            "printing" in lowered || lowered == "printing" || lowered == "print" -> PrinterState.Printing
            "paused" in lowered || lowered == "pause" -> PrinterState.Paused
            "standby" in lowered || "idle" in lowered || "operational" in lowered || "ready" in lowered -> PrinterState.Idle
            "offline" in lowered || "error" in lowered || "shutdown" in lowered -> PrinterState.Offline
            "online" in lowered || "connected" in lowered -> PrinterState.Online
            else -> PrinterState.Unknown
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
