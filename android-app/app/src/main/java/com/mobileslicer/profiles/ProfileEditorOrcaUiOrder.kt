package com.mobileslicer.profiles

internal object ProfileEditorOrcaUiOrder {
    val processTabs = listOf("Quality", "Strength", "Speed", "Support", "Multimaterial", "Others")
    val filamentTabs = listOf("Filament", "Cooling", "Setting Overrides", "Advanced", "Multimaterial", "Dependencies", "Notes")
    val printerTabs = listOf("Basic information", "Connection", "Machine G-code", "Multimaterial", "Extruder", "Motion ability", "Notes")
    val printerGroups = mapOf(
        "Basic information" to listOf("Printable space", "Advanced", "Cooling Fan", "Extruder Clearance", "Adaptive bed mesh", "Accessory"),
        "Connection" to listOf("Print host"),
        "Machine G-code" to listOf(
            "File header",
            "Machine start G-code",
            "Machine end G-code",
            "Printing by object G-code",
            "Before layer change G-code",
            "Layer change G-code",
            "Timelapse G-code",
            "Clumping Detection G-code",
            "Change filament G-code",
            "Change extrusion role G-code",
            "Pause G-code",
            "Template Custom G-code"
        ),
        "Multimaterial" to listOf("Single extruder multi-material setup", "Wipe tower", "Single extruder multi-material parameters", "Advanced"),
        "Extruder" to listOf("Basic information", "Layer height limits", "Position", "Retraction", "Z-Hop", "Retraction when switching material"),
        "Motion ability" to listOf("Advanced", "Resonance Avoidance", "Speed limitation", "Acceleration limitation", "Jerk limitation"),
        "Notes" to listOf("Notes")
    )
    val filamentGroups = mapOf(
        "Filament" to listOf(
            "Basic information",
            "Flow ratio and Pressure Advance",
            "Print chamber temperature",
            "Print temperature",
            "Bed temperature",
            "Volumetric speed limitation"
        ),
        "Cooling" to listOf("Cooling for specific layer", "Part cooling fan", "Auxiliary part cooling fan", "Exhaust fan"),
        "Setting Overrides" to listOf("Retraction", "Ironing"),
        "Advanced" to listOf("Filament start G-code", "Filament end G-code"),
        "Multimaterial" to listOf(
            "Wipe tower parameters",
            "Multi Filament",
            "Tool change parameters with single extruder MM printers",
            "Tool change parameters with multi extruder MM printers"
        ),
        "Dependencies" to listOf("Compatible printers", "Compatible process profiles"),
        "Notes" to listOf("Notes")
    )
    val processGroups = mapOf(
        "Quality" to listOf("Layer height", "Line width", "Seam", "Precision", "Ironing", "Wall generator", "Walls and surfaces", "Bridging", "Overhangs"),
        "Strength" to listOf("Walls", "Top/bottom shells", "Infill", "Advanced"),
        "Speed" to listOf("First layer speed", "Other layers speed", "Overhang speed", "Travel speed", "Acceleration", "Jerk(XY)"),
        "Support" to listOf("Support", "Raft", "Support filament", "Support ironing", "Advanced", "Tree supports"),
        "Multimaterial" to listOf("Prime tower", "Advanced", "Flush options"),
        "Others" to listOf("Skirt", "Brim", "Special mode", "Fuzzy Skin", "Skirt", "Brim", "Special mode", "G-code output", "Post-processing Scripts", "Notes", "Fuzzy Skin Advanced")
    )
}

internal fun processEditorGroupLabelsForParityTest(): Map<String, List<String>> = processEditorRenderedGroupLabelsForParityTest
internal fun filamentEditorGroupLabelsForParityTest(): Map<String, List<String>> = ProfileEditorOrcaUiOrder.filamentGroups
internal fun printerEditorGroupLabelsForParityTest(): Map<String, List<String>> = ProfileEditorOrcaUiOrder.printerGroups
