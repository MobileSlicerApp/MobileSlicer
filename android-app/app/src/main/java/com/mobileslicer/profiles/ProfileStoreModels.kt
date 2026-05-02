package com.mobileslicer.profiles

internal data class ProfileStore(
    val printers: List<PrinterProfile>,
    val filaments: List<FilamentProfile>,
    val processes: List<ProcessProfile>,
    val selectedPrinterId: String,
    val selectedFilamentId: String,
    val selectedProcessId: String,
    val selectedProcessIdsByPrinterId: Map<String, String> = emptyMap()
)

internal data class SettingTruthRow(
    val label: String,
    val status: String,
    val detail: String? = null
)
