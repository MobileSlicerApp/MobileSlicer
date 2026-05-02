package com.mobileslicer.profiles

import org.json.JSONObject

internal enum class GcodeFlavor(
    val configValue: String,
    val displayLabel: String
) {
    MarlinLegacy("marlin", "Marlin legacy"),
    Klipper("klipper", "Klipper"),
    RepRapFirmware("reprapfirmware", "RepRapFirmware"),
    Marlin2("marlin2", "Marlin 2");

    companion object {
        fun fromConfigValue(value: String?): GcodeFlavor =
            entries.firstOrNull { it.configValue == value } ?: MarlinLegacy
    }
}

internal enum class PrinterTechnology(
    val configValue: String,
    val displayLabel: String
) {
    Fff("FFF", "FFF"),
    Sla("SLA", "SLA");

    companion object {
        fun fromConfigValue(value: String?): PrinterTechnology =
            entries.firstOrNull { it.configValue == value } ?: Fff
    }
}

internal enum class PowerLossRecoveryMode(
    val configValue: String,
    val displayLabel: String
) {
    PrinterConfiguration("printer_configuration", "Printer configuration"),
    Enable("enable", "Enable"),
    Disable("disable", "Disable");

    companion object {
        fun fromConfigValue(value: String?): PowerLossRecoveryMode =
            entries.firstOrNull { it.configValue == value } ?: PrinterConfiguration
    }
}

internal enum class PrintHostType(
    val configValue: String,
    val displayLabel: String
) {
    PrusaLink("prusalink", "PrusaLink"),
    PrusaConnect("prusaconnect", "PrusaConnect"),
    OctoPrint("octoprint", "Octo/Klipper"),
    Duet("duet", "Duet"),
    FlashAir("flashair", "FlashAir"),
    AstroBox("astrobox", "AstroBox"),
    Repetier("repetier", "Repetier"),
    Mks("mks", "MKS"),
    Esp3d("esp3d", "ESP3D"),
    CrealityPrint("crealityprint", "CrealityPrint"),
    Obico("obico", "Obico"),
    Flashforge("flashforge", "Flashforge"),
    SimplyPrint("simplyprint", "SimplyPrint"),
    ElegooLink("elegoolink", "Elegoo Link"),
    BambuLan("bambulan", "Bambu LAN");

    companion object {
        fun fromConfigValue(value: String?): PrintHostType =
            entries.firstOrNull { it.configValue == value } ?: OctoPrint
    }
}

internal enum class PrintHostAuthorizationType(
    val configValue: String,
    val displayLabel: String
) {
    Key("key", "API key"),
    User("user", "HTTP digest");

    companion object {
        fun fromConfigValue(value: String?): PrintHostAuthorizationType =
            entries.firstOrNull { it.configValue == value } ?: Key
    }
}


internal enum class NozzleType(
    val configValue: String,
    val displayLabel: String
) {
    Undefined("undefine", "Undefine"),
    HardenedSteel("hardened_steel", "Hardened steel"),
    StainlessSteel("stainless_steel", "Stainless steel"),
    TungstenCarbide("tungsten_carbide", "Tungsten carbide"),
    Brass("brass", "Brass");

    companion object {
        fun fromConfigValue(value: String?): NozzleType =
            entries.firstOrNull { it.configValue == value } ?: Undefined
    }
}

internal enum class NozzleVolumeType(
    val configValue: String,
    val displayLabel: String
) {
    Standard("Standard", "Standard"),
    HighFlow("High Flow", "High Flow");

    companion object {
        fun fromConfigValue(value: String?): NozzleVolumeType =
            entries.firstOrNull { it.configValue == value?.substringBefore(';')?.substringBefore(',')?.trim() } ?: Standard
    }
}

internal enum class PrinterStructure(
    val configValue: String,
    val displayLabel: String
) {
    Undefined("undefine", "Undefine"),
    CoreXY("corexy", "CoreXY"),
    I3("i3", "I3"),
    HBot("hbot", "Hbot"),
    Delta("delta", "Delta");

    companion object {
        fun fromConfigValue(value: String?): PrinterStructure =
            entries.firstOrNull { it.configValue == value } ?: Undefined
    }
}

internal enum class DefaultBedType(
    val configValue: String,
    val displayLabel: String
) {
    NotSet("", "Not set"),
    CoolPlate("1", "Cool Plate"),
    EngineeringPlate("2", "Engineering Plate"),
    HighTempPlate("3", "High Temp Plate"),
    TexturedPeiPlate("4", "Textured PEI Plate"),
    TexturedCoolPlate("5", "Textured Cool Plate"),
    SuperTackPlate("6", "Cool Plate (SuperTack)");

    companion object {
        fun fromConfigValue(value: String?): DefaultBedType {
            val normalized = value?.trim().orEmpty()
            return entries.firstOrNull { it.configValue == normalized } ?: when (normalized) {
                "Cool Plate" -> CoolPlate
                "Engineering Plate" -> EngineeringPlate
                "High Temp Plate" -> HighTempPlate
                "Textured PEI Plate" -> TexturedPeiPlate
                "Textured Cool Plate" -> TexturedCoolPlate
                "SuperTack Plate" -> SuperTackPlate
                else -> NotSet
            }
        }
    }
}

internal val DefaultBedType.orcaBedTypeName: String
    get() = when (this) {
        DefaultBedType.NotSet -> ""
        DefaultBedType.CoolPlate -> "Cool Plate"
        DefaultBedType.EngineeringPlate -> "Engineering Plate"
        DefaultBedType.HighTempPlate -> "High Temp Plate"
        DefaultBedType.TexturedPeiPlate -> "Textured PEI Plate"
        DefaultBedType.TexturedCoolPlate -> "Textured Cool Plate"
        DefaultBedType.SuperTackPlate -> "SuperTack Plate"
    }

internal val DefaultBedType.userVisibleLabel: String
    get() = if (this == DefaultBedType.NotSet) {
        DefaultBedType.TexturedPeiPlate.displayLabel
    } else {
        displayLabel
    }

internal fun PrinterProfile.effectiveOrcaBedType(): DefaultBedType {
    if (defaultBedType != DefaultBedType.NotSet) return defaultBedType
    orcaConfiguredDefaultBedType().takeIf { it != DefaultBedType.NotSet }?.let { return it }

    return if (orcaModelIds().any { it in orcaCoolPlateDefaultModelIds }) {
        DefaultBedType.CoolPlate
    } else {
        DefaultBedType.HighTempPlate
    }
}

internal fun PrinterProfile.effectiveOrcaBedTypeName(): String =
    effectiveOrcaBedType().orcaBedTypeName

private val orcaCoolPlateDefaultModelIds = setOf("BL-P001", "BL-P002", "C13")

private fun PrinterProfile.orcaConfiguredDefaultBedType(): DefaultBedType =
    sequenceOf(
        jsonStringValue(orcaMachineModelJson, "default_bed_type"),
        jsonStringValue(orcaResolvedMachineJson, "default_bed_type"),
        jsonStringValue(orcaMachineOverridesJson, "default_bed_type")
    )
        .map { DefaultBedType.fromConfigValue(it) }
        .firstOrNull { it != DefaultBedType.NotSet }
        ?: DefaultBedType.NotSet

private fun PrinterProfile.orcaModelIds(): Set<String> = buildSet {
    addIfNotBlank(printerModel)
    addIfNotBlank(printerVariant)
    addIfNotBlank(jsonStringValue(orcaMachineModelJson, "model_id"))
    addIfNotBlank(jsonStringValue(orcaResolvedMachineJson, "model_id"))
    addIfNotBlank(jsonStringValue(orcaMachineOverridesJson, "model_id"))
}

private fun MutableSet<String>.addIfNotBlank(value: String?) {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isNotBlank()) add(trimmed)
}

private fun jsonStringValue(json: String, key: String): String =
    runCatching { JSONObject(json).optString(key) }.getOrDefault("")

internal enum class BedTemperatureFormula(
    val configValue: String,
    val displayLabel: String
) {
    ByFirstFilament("by_first_filament", "By First filament"),
    ByHighestTemp("by_highest_temp", "By Highest Temp");

    companion object {
        fun fromConfigValue(value: String?): BedTemperatureFormula =
            entries.firstOrNull { it.configValue == value } ?: ByHighestTemp
    }
}

internal enum class WipeTowerType(
    val configValue: String,
    val displayLabel: String
) {
    Type1("type1", "Type 1"),
    Type2("type2", "Type 2");

    companion object {
        fun fromConfigValue(value: String?): WipeTowerType =
            entries.firstOrNull { it.configValue == value } ?: Type2
    }
}

internal enum class ZHopType(
    val configValue: String,
    val displayLabel: String
) {
    Auto("Auto Lift", "Auto"),
    Normal("Normal Lift", "Normal"),
    Slope("Slope Lift", "Slope"),
    Spiral("Spiral Lift", "Spiral");

    companion object {
        fun fromConfigValue(value: String?): ZHopType =
            entries.firstOrNull { it.configValue == value } ?: Slope
    }
}

internal enum class RetractLiftEnforce(
    val configValue: String,
    val displayLabel: String
) {
    AllSurfaces("All Surfaces", "All surfaces"),
    TopOnly("Top Only", "Top only"),
    BottomOnly("Bottom Only", "Bottom only"),
    TopAndBottom("Top and Bottom", "Top and bottom");

    companion object {
        fun fromConfigValue(value: String?): RetractLiftEnforce =
            entries.firstOrNull { it.configValue == value } ?: AllSurfaces
    }
}

internal enum class LongRetractionWhenCutMode(
    val configValue: Int,
    val displayLabel: String
) {
    Disabled(0, "Disabled"),
    EnableMachine(1, "Enable machine"),
    EnableFilament(2, "Enable filament");

    companion object {
        fun fromConfigValue(value: Int): LongRetractionWhenCutMode =
            entries.firstOrNull { it.configValue == value } ?: Disabled
    }
}

internal enum class ExtruderType(
    val configValue: String,
    val displayLabel: String
) {
    DirectDrive("Direct Drive", "Direct drive"),
    Bowden("Bowden", "Bowden");

    companion object {
        fun fromConfigValue(value: String?): ExtruderType =
            entries.firstOrNull { it.configValue == value } ?: DirectDrive
    }
}
