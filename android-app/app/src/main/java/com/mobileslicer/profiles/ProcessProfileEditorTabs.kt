package com.mobileslicer.profiles

internal val processEditorRenderedGroupLabelsForParityTest = mapOf(
    "Quality" to listOf("Layer height", "Line width", "Seam", "Precision", "Ironing", "Wall generator", "Walls and surfaces", "Bridging", "Overhangs"),
    "Strength" to listOf("Walls", "Top/bottom shells", "Infill", "Advanced"),
    "Speed" to listOf("First layer speed", "Other layers speed", "Overhang speed", "Travel speed", "Acceleration", "Jerk(XY)"),
    "Support" to listOf("Support", "Raft", "Support filament", "Support ironing", "Advanced", "Tree supports"),
    "Multimaterial" to listOf("Prime tower", "Advanced", "Flush options"),
    "Others" to listOf("Skirt", "Brim", "Special mode", "Fuzzy Skin", "Skirt", "Brim", "Special mode", "G-code output", "Post-processing Scripts", "Notes", "Fuzzy Skin Advanced")
)
