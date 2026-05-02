package com.mobileslicer.profiles

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal object ProfileStoreRepository {
    private const val KEY_PRINTERS_JSON = "profiles_printers_json"
    private const val KEY_FILAMENTS_JSON = "profiles_filaments_json"
    private const val KEY_PROCESSES_JSON = "profiles_processes_json"
    private const val KEY_SELECTED_PRINTER = "selected_printer_id"
    private const val KEY_SELECTED_FILAMENT = "selected_filament_id"
    private const val KEY_SELECTED_PROCESS = "selected_process_id"
    private const val KEY_SELECTED_PROCESSES_BY_PRINTER = "selected_process_ids_by_printer_json"

    fun load(preferences: SharedPreferences): ProfileStore {
        val printers = loadPrinters(preferences)
        val filaments = loadFilaments(preferences)
        val processes = loadProcesses(preferences)
        val selectedPrinterId = preferences.getString(KEY_SELECTED_PRINTER, printers.firstOrNull()?.id.orEmpty())
            ?.takeIf { id -> printers.any { it.id == id } }
            ?: printers.firstOrNull()?.id.orEmpty()
        val visibleProcesses = processes.filter { it.printerProfileId == selectedPrinterId }
        val visibleFilaments = filaments.filter { it.printerProfileId == selectedPrinterId }
        val storedProcessSelections = loadSelectedProcessIdsByPrinter(preferences)
        val legacySelectedProcessId = preferences.getString(KEY_SELECTED_PROCESS, "").orEmpty()
        val selectedProcessId = storedProcessSelections[selectedPrinterId]
            ?.takeIf { id -> visibleProcesses.any { it.id == id } }
            ?: legacySelectedProcessId.takeIf { id -> visibleProcesses.any { it.id == id } }
            ?: visibleProcesses.standardProcessForPrinter(selectedPrinterId)?.id
            ?: visibleProcesses.firstOrNull()?.id.orEmpty()
        return ProfileStore(
            printers = printers,
            filaments = filaments,
            processes = processes,
            selectedPrinterId = selectedPrinterId,
            selectedFilamentId = preferences.getString(KEY_SELECTED_FILAMENT, visibleFilaments.firstOrNull()?.id.orEmpty())
                ?.takeIf { id -> visibleFilaments.any { it.id == id } }
                ?: visibleFilaments.firstOrNull()?.id.orEmpty(),
            selectedProcessId = selectedProcessId,
            selectedProcessIdsByPrinterId = sanitizeSelectedProcessIdsByPrinter(
                printers = printers,
                processes = processes,
                selections = storedProcessSelections + (selectedPrinterId to selectedProcessId)
            )
        )
    }

    fun persist(preferences: SharedPreferences, store: ProfileStore) {
        val selectedProcessIdsByPrinter = sanitizeSelectedProcessIdsByPrinter(
            printers = store.printers,
            processes = store.processes,
            selections = store.selectedProcessIdsByPrinterId + (store.selectedPrinterId to store.selectedProcessId)
        )
        preferences.edit()
            .putString(KEY_PRINTERS_JSON, JSONArray().apply {
                store.printers.forEach { put(it.toJson()) }
            }.toString())
            .putString(KEY_FILAMENTS_JSON, JSONArray().apply {
                store.filaments.forEach { put(it.toJson()) }
            }.toString())
            .putString(KEY_PROCESSES_JSON, JSONArray().apply {
                store.processes.forEach { put(it.toJson()) }
            }.toString())
            .putString(KEY_SELECTED_PRINTER, store.selectedPrinterId)
            .putString(KEY_SELECTED_FILAMENT, store.selectedFilamentId)
            .putString(KEY_SELECTED_PROCESS, store.selectedProcessId)
            .putString(KEY_SELECTED_PROCESSES_BY_PRINTER, selectedProcessIdsByPrinter.toJsonObject().toString())
            .apply()
    }

    fun newCustomId(tab: ProfileTab): String = "${tab.name.lowercase()}_${UUID.randomUUID()}"

    private fun loadPrinters(preferences: SharedPreferences): List<PrinterProfile> {
        val stored = preferences.getString(KEY_PRINTERS_JSON, null) ?: return defaultPrinterProfiles()
        return runCatching {
            JSONArray(stored).let { array ->
                List(array.length()) { index -> array.getJSONObject(index).toPrinterProfile() }
            }.filterNot { it.builtIn }
        }.getOrElse { defaultPrinterProfiles() }
    }

    private fun loadFilaments(preferences: SharedPreferences): List<FilamentProfile> {
        val stored = preferences.getString(KEY_FILAMENTS_JSON, null) ?: return emptyList()
        return runCatching {
            JSONArray(stored).let { array ->
                List(array.length()) { index -> array.getJSONObject(index).toFilamentProfile() }
            }.filterNot { it.builtIn || it.profileSource == "default" }
        }.getOrElse { emptyList() }
    }

    private fun loadProcesses(preferences: SharedPreferences): List<ProcessProfile> {
        val stored = preferences.getString(KEY_PROCESSES_JSON, null) ?: return emptyList()
        return runCatching {
            JSONArray(stored).let { array ->
                List(array.length()) { index -> array.getJSONObject(index).toProcessProfile() }
            }.filterNot { it.builtIn }
        }.getOrElse { emptyList() }
    }

    private fun loadSelectedProcessIdsByPrinter(preferences: SharedPreferences): Map<String, String> {
        val stored = preferences.getString(KEY_SELECTED_PROCESSES_BY_PRINTER, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(stored)
            buildMap {
                json.keys().forEach { printerId ->
                    val processId = json.optString(printerId)
                    if (printerId.isNotBlank() && processId.isNotBlank()) {
                        put(printerId, processId)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun sanitizeSelectedProcessIdsByPrinter(
        printers: List<PrinterProfile>,
        processes: List<ProcessProfile>,
        selections: Map<String, String>
    ): Map<String, String> {
        val printerIds = printers.map { it.id }.toSet()
        return selections.filter { (printerId, processId) ->
            printerId in printerIds &&
                processes.any { process ->
                    process.id == processId &&
                        (process.printerProfileId == printerId ||
                            (process.printerProfileId.isBlank() && process.profileSource == "custom"))
                }
        }
    }

    private fun Map<String, String>.toJsonObject(): JSONObject =
        JSONObject().apply {
            entries.sortedBy { it.key }.forEach { (printerId, processId) ->
                put(printerId, processId)
            }
        }

    internal fun defaultPrinterProfiles(): List<PrinterProfile> = profileStoreDefaultPrinterProfiles()

    internal fun fallbackPrinterProfile(): PrinterProfile = profileStoreFallbackPrinterProfile()

    internal fun fallbackFilamentProfile(): FilamentProfile = profileStoreFallbackFilamentProfile()

    internal fun defaultFilamentProfiles(): List<FilamentProfile> = profileStoreDefaultFilamentProfiles()

    internal fun defaultProcessProfiles(): List<ProcessProfile> = profileStoreDefaultProcessProfiles()

    internal fun fallbackProcessProfile(printerProfileId: String = "", nozzleDiameterMm: Float = 0.4f): ProcessProfile =
        profileStoreFallbackProcessProfile(printerProfileId, nozzleDiameterMm)
}
