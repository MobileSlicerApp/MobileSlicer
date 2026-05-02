package com.mobileslicer.profiles

internal fun ProfileStore.activeConfiguration(): ActiveSlicerConfiguration = ActiveSlicerConfiguration(
    printer = printers.firstOrNull { it.id == selectedPrinterId }
        ?: printers.firstOrNull()
        ?: ProfileStoreRepository.fallbackPrinterProfile(),
    filament = filaments.firstOrNull { it.id == selectedFilamentId && it.printerProfileId == selectedPrinterId }
        ?: filaments.firstOrNull { it.printerProfileId == selectedPrinterId }
        ?: ProfileStoreRepository.fallbackFilamentProfile(),
    process = processes.firstOrNull { it.id == selectedProcessId && it.printerProfileId == selectedPrinterId }
        ?: processes.standardProcessForPrinter(selectedPrinterId)
        ?: processes.firstOrNull { it.printerProfileId == selectedPrinterId }
        ?: ProfileStoreRepository.fallbackProcessProfile(selectedPrinterId)
)
