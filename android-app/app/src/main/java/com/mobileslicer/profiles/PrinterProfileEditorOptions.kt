package com.mobileslicer.profiles

import com.mobileslicer.AppSettingOption

internal class PrinterProfileEditorOptions {
    val boolEnabledDisabledOptions =
            listOf(
                AppSettingOption(true, "Enabled", ""),
                AppSettingOption(false, "Disabled", "")
            )
        val gcodeFlavorOptions =
            GcodeFlavor.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca gcode_flavor = ${option.configValue}.")
            }
        val printerStructureOptions =
            PrinterStructure.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca printer_structure = ${option.configValue}.")
            }
        val printerTechnologyOptions =
            PrinterTechnology.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca printer_technology = ${option.configValue}.")
            }
        val powerLossRecoveryOptions =
            PowerLossRecoveryMode.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca enable_power_loss_recovery = ${option.configValue}.")
            }
        val printHostTypeOptions =
            PrintHostType.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca host_type = ${option.configValue}.")
            }
        val printHostAuthorizationOptions =
            PrintHostAuthorizationType.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca printhost_authorization_type = ${option.configValue}.")
            }
        val nozzleTypeOptions =
            NozzleType.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca nozzle_type = ${option.configValue}.")
            }
        val bedTemperatureFormulaOptions =
            BedTemperatureFormula.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca bed_temperature_formula = ${option.configValue}.")
            }
        val defaultBedTypeOptions =
            bedTypeOptions(supportMultiBedTypes = true)
        val wipeTowerTypeOptions =
            WipeTowerType.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca wipe_tower_type = ${option.configValue}.")
            }
        val zHopTypeOptions =
            ZHopType.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca z_hop_types = ${option.configValue}.")
            }
        val retractLiftEnforceOptions =
            RetractLiftEnforce.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca retract_lift_enforce = ${option.configValue}.")
            }
        val longRetractionWhenCutOptions =
            LongRetractionWhenCutMode.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca enable_long_retraction_when_cut = ${option.configValue}.")
            }
        val extruderTypeOptions =
            ExtruderType.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca extruder_type = ${option.configValue}.")
            }
        val nozzleVolumeTypeOptions =
            NozzleVolumeType.entries.map { option ->
                AppSettingOption(option, option.displayLabel, "Maps directly to Orca nozzle_volume_type = ${option.configValue}.")
            }

    fun bedTypeOptions(supportMultiBedTypes: Boolean): List<AppSettingOption<DefaultBedType>> {
        val bedTypes = if (supportMultiBedTypes) {
            DefaultBedType.entries.filter { it != DefaultBedType.NotSet }
        } else {
            emptyList()
        }
        return bedTypes.map { option ->
            AppSettingOption(
                option,
                option.displayLabel,
                "Maps directly to Orca default_bed_type = ${option.configValue.ifBlank { "<empty>" }}."
            )
        }
    }
}
