package com.mobileslicer.profiles

import org.json.JSONArray
import org.json.JSONObject

internal fun ProfileStore.toJsonObject(): JSONObject = JSONObject()
    .put("printers", JSONArray().apply {
        printers.forEach { put(it.toJson()) }
    })
    .put("filaments", JSONArray().apply {
        filaments.forEach { put(it.toJson()) }
    })
    .put("processes", JSONArray().apply {
        processes.forEach { put(it.toJson()) }
    })
    .put("selectedPrinterId", selectedPrinterId)
    .put("selectedFilamentId", selectedFilamentId)
    .put("selectedProcessId", selectedProcessId)
    .put("selectedProcessIdsByPrinterId", JSONObject().apply {
        selectedProcessIdsByPrinterId.entries.sortedBy { it.key }.forEach { (printerId, processId) ->
            put(printerId, processId)
        }
    })

internal fun JSONObject.toProfileStoreOrDefault(): ProfileStore {
    val defaultPrinters = ProfileStoreRepository.defaultPrinterProfiles()
    val defaultFilaments = emptyList<FilamentProfile>()
    val defaultProcesses = emptyList<ProcessProfile>()
    val defaultStore = ProfileStore(
        printers = defaultPrinters,
        filaments = defaultFilaments,
        processes = defaultProcesses,
        selectedPrinterId = defaultPrinters.firstOrNull()?.id.orEmpty(),
        selectedFilamentId = defaultFilaments.firstOrNull()?.id.orEmpty(),
        selectedProcessId = defaultProcesses.firstOrNull()?.id.orEmpty()
    )
    return runCatching {
        val printers = optJSONArray("printers")?.let { array ->
            List(array.length()) { index -> array.getJSONObject(index).toPrinterProfile() }
        }.orEmpty().filterNot { it.builtIn }
        val filaments = optJSONArray("filaments")?.let { array ->
            List(array.length()) { index -> array.getJSONObject(index).toFilamentProfile() }
        }.orEmpty().filterNot { it.builtIn }
        val processes = optJSONArray("processes")?.let { array ->
            List(array.length()) { index -> array.getJSONObject(index).toProcessProfile() }
        }.orEmpty().filterNot { it.builtIn }
        val selectedPrinterId = optString("selectedPrinterId")
            .takeIf { id -> printers.any { it.id == id } }
            ?: printers.firstOrNull()?.id.orEmpty()
        val visibleProcesses = processes.filter { it.printerProfileId == selectedPrinterId }
        val selectedProcessIdsByPrinterId = optJSONObject("selectedProcessIdsByPrinterId")?.let { json ->
            buildMap {
                json.keys().forEach { printerId ->
                    val processId = json.optString(printerId)
                    if (processId.isNotBlank() &&
                        printers.any { it.id == printerId } &&
                        processes.any { process -> process.id == processId && process.printerProfileId == printerId }
                    ) {
                        put(printerId, processId)
                    }
                }
            }
        }.orEmpty()
        val selectedProcessId = selectedProcessIdsByPrinterId[selectedPrinterId]
            ?.takeIf { id -> visibleProcesses.any { it.id == id } }
            ?: optString("selectedProcessId")
                .takeIf { id -> visibleProcesses.any { it.id == id } }
            ?: visibleProcesses.standardProcessForPrinter(selectedPrinterId)?.id
            ?: visibleProcesses.firstOrNull()?.id.orEmpty()
        ProfileStore(
            printers = printers,
            filaments = filaments,
            processes = processes,
            selectedPrinterId = selectedPrinterId,
            selectedFilamentId = optString("selectedFilamentId")
                .takeIf { id -> filaments.any { it.id == id } }
                ?: filaments.firstOrNull()?.id.orEmpty(),
            selectedProcessId = selectedProcessId,
            selectedProcessIdsByPrinterId = selectedProcessIdsByPrinterId + (selectedPrinterId to selectedProcessId)
        )
    }.getOrDefault(defaultStore)
}
