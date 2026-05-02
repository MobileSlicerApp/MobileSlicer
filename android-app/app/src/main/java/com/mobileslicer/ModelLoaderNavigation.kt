package com.mobileslicer

import com.mobileslicer.profiles.PrintHostType
import com.mobileslicer.profiles.PrinterProfile
import com.mobileslicer.profiles.ProfileStore
import com.mobileslicer.workspace.AppScreen

internal fun appScreenFromName(name: String, fallback: AppScreen): AppScreen =
    runCatching { AppScreen.valueOf(name) }.getOrDefault(fallback)

internal fun ProfileStore.hasCompleteProfileSelection(): Boolean =
    printers.any { it.id == selectedPrinterId } &&
        filaments.any {
            it.id == selectedFilamentId &&
                it.printerProfileId == selectedPrinterId
        } &&
        processes.any {
            it.id == selectedProcessId &&
                it.printerProfileId == selectedPrinterId
        }

internal fun printerWebUiUrl(printerProfile: PrinterProfile): String? {
    val rawHost = printerProfile.printHostWebUi
        .ifBlank { printerProfile.printHost }
        .ifBlank {
            if (printerProfile.printHostType == PrintHostType.SimplyPrint) {
                "https://simplyprint.io/panel"
            } else {
                ""
            }
        }
        .trim()
    if (rawHost.isBlank()) return null
    return normalizePrinterWebUiUrl(rawHost)
}
