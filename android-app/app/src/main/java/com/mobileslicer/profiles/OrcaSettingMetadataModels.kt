package com.mobileslicer.profiles

internal data class OrcaGuiPlacement(
    val page: String,
    val group: String,
    val tabCppLine: Int
)

internal data class OrcaSettingMetadata(
    val key: String,
    val configType: String,
    val mode: String,
    val label: String,
    val category: String,
    val defaultValue: String,
    val enumValues: List<String>,
    val printConfigLine: Int,
    val guiPlacements: List<OrcaGuiPlacement>
) {
    val isDefinedInPrintConfig: Boolean
        get() = printConfigLine > 0

    val appearsInOrcaGui: Boolean
        get() = guiPlacements.isNotEmpty()
}
