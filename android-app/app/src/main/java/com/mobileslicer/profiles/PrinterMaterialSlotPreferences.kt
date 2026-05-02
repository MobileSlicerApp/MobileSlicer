package com.mobileslicer.profiles

import android.content.Context

private const val PrinterMaterialSlotsPreferences = "printer_material_slots"

internal fun clearPrinterMaterialSlotPreferences(
    context: Context,
    printerId: String,
    legacyPrinterName: String = ""
) {
    val editor = context
        .getSharedPreferences(PrinterMaterialSlotsPreferences, Context.MODE_PRIVATE)
        .edit()
    if (printerId.isNotBlank()) {
        editor.remove("slots_$printerId")
    }
    if (legacyPrinterName.isNotBlank()) {
        editor.remove("slots_$legacyPrinterName")
    }
    editor.apply()
}
