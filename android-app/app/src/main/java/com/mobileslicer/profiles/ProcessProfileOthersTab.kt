package com.mobileslicer.profiles

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.mobileslicer.AppSettingOption

@Composable
internal fun ProcessOthersTabContent(
    showAdvancedProfileSettings: Boolean,
    boolEnabledDisabledOptions: List<AppSettingOption<Boolean>>,
    skirtTypeOptions: List<AppSettingOption<SkirtType>>,
    draftShieldOptions: List<AppSettingOption<DraftShield>>,
    brimTypeOptions: List<AppSettingOption<BrimType>>,
    slicingModeOptions: List<AppSettingOption<SlicingMode>>,
    printSequenceOptions: List<AppSettingOption<PrintSequence>>,
    printOrderOptions: List<AppSettingOption<PrintOrder>>,
    timelapseTypeOptions: List<AppSettingOption<TimelapseType>>,
    fuzzySkinOptions: List<AppSettingOption<FuzzySkinType>>,
    fuzzySkinModeOptions: List<AppSettingOption<FuzzySkinMode>>,
    fuzzySkinNoiseOptions: List<AppSettingOption<FuzzySkinNoiseType>>,
    skirtType: SkirtType,
    onSkirtTypeChange: (SkirtType) -> Unit,
    skirts: String,
    onSkirtsChange: (String) -> Unit,
    minSkirtLength: String,
    onMinSkirtLengthChange: (String) -> Unit,
    skirtHeight: String,
    onSkirtHeightChange: (String) -> Unit,
    skirtDistance: String,
    onSkirtDistanceChange: (String) -> Unit,
    skirtStartAngle: String,
    onSkirtStartAngleChange: (String) -> Unit,
    skirtSpeed: String,
    onSkirtSpeedChange: (String) -> Unit,
    draftShield: DraftShield,
    onDraftShieldChange: (DraftShield) -> Unit,
    singleLoopDraftShield: Boolean,
    onSingleLoopDraftShieldChange: (Boolean) -> Unit,
    brimType: BrimType,
    onBrimTypeChange: (BrimType) -> Unit,
    brimWidth: String,
    onBrimWidthChange: (String) -> Unit,
    brimObjectGap: String,
    onBrimObjectGapChange: (String) -> Unit,
    brimUseEfcOutline: Boolean,
    onBrimUseEfcOutlineChange: (Boolean) -> Unit,
    combineBrims: Boolean,
    onCombineBrimsChange: (Boolean) -> Unit,
    brimEars: Boolean,
    onBrimEarsChange: (Boolean) -> Unit,
    brimEarsDetectionLength: String,
    onBrimEarsDetectionLengthChange: (String) -> Unit,
    brimEarsMaxAngle: String,
    onBrimEarsMaxAngleChange: (String) -> Unit,
    slicingMode: SlicingMode,
    onSlicingModeChange: (SlicingMode) -> Unit,
    printSequence: PrintSequence,
    onPrintSequenceChange: (PrintSequence) -> Unit,
    printOrder: PrintOrder,
    onPrintOrderChange: (PrintOrder) -> Unit,
    spiralMode: Boolean,
    onSpiralModeChange: (Boolean) -> Unit,
    specialModeDetails: SpecialModeDetailsDraft,
    onSpecialModeDetailsChange: (SpecialModeDetailsDraft) -> Unit,
    fuzzySkin: FuzzySkinType,
    onFuzzySkinChange: (FuzzySkinType) -> Unit,
    fuzzySkinThickness: String,
    onFuzzySkinThicknessChange: (String) -> Unit,
    fuzzySkinPointDistance: String,
    onFuzzySkinPointDistanceChange: (String) -> Unit,
    fuzzySkinFirstLayer: Boolean,
    onFuzzySkinFirstLayerChange: (Boolean) -> Unit,
    fuzzySkinMode: FuzzySkinMode,
    onFuzzySkinModeChange: (FuzzySkinMode) -> Unit,
    fuzzySkinNoiseType: FuzzySkinNoiseType,
    onFuzzySkinNoiseTypeChange: (FuzzySkinNoiseType) -> Unit,
    fuzzySkinScale: String,
    onFuzzySkinScaleChange: (String) -> Unit,
    fuzzySkinOctaves: String,
    onFuzzySkinOctavesChange: (String) -> Unit,
    fuzzySkinPersistence: String,
    onFuzzySkinPersistenceChange: (String) -> Unit,
    reduceInfillRetraction: Boolean,
    onReduceInfillRetractionChange: (Boolean) -> Unit,
    gcodeOutputDetails: GcodeOutputDetailsDraft,
    onGcodeOutputDetailsChange: (GcodeOutputDetailsDraft) -> Unit,
    filenameFormat: String,
    onFilenameFormatChange: (String) -> Unit,
    postProcessScripts: String,
    onPostProcessScriptsChange: (String) -> Unit,
    processNotes: String,
    onProcessNotesChange: (String) -> Unit,
) {
    ProfileEditorSection("Others", "Skirt, brim, special modes, fuzzy skin, and output metadata.") {
        if (ProfileEditorSetting.ProcessAdhesionAndFuzzySkinCore.isVisible(showAdvancedProfileSettings)) {
            ProfileGroupHeader("Skirt")
            ProfileTextField(skirts, onSkirtsChange, "Skirt loops", KeyboardType.Number)
            ProfileTextField(skirtHeight, onSkirtHeightChange, "Skirt height", KeyboardType.Number)

            ProfileGroupHeader("Brim")
            ProfileDropdownField(
                label = "Brim type",
                selectedLabel = brimType.displayLabel,
                options = brimTypeOptions,
                onSelected = onBrimTypeChange
            )
            ProfileTextField(brimWidth, onBrimWidthChange, "Brim width (mm)", KeyboardType.Decimal)

            ProfileGroupHeader("Special mode")
            ProfileDropdownField(
                label = "Print sequence",
                selectedLabel = printSequence.displayLabel,
                options = printSequenceOptions,
                onSelected = onPrintSequenceChange
            )
            ProfileDropdownField(
                label = "Spiral vase",
                selectedLabel = if (spiralMode) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = onSpiralModeChange
            )
            ProfileDropdownField(
                label = "Spiral mode smooth",
                selectedLabel = if (specialModeDetails.spiralModeSmooth) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = { onSpecialModeDetailsChange(specialModeDetails.copy(spiralModeSmooth = it)) }
            )
            ProfileDropdownField(
                label = "Timelapse",
                selectedLabel = specialModeDetails.timelapseType.displayLabel,
                options = timelapseTypeOptions,
                onSelected = { onSpecialModeDetailsChange(specialModeDetails.copy(timelapseType = it)) }
            )

            ProfileGroupHeader("Fuzzy Skin")
            ProfileDropdownField(
                label = "Fuzzy skin",
                selectedLabel = fuzzySkin.displayLabel,
                options = fuzzySkinOptions,
                onSelected = onFuzzySkinChange
            )
            ProfileTextField(fuzzySkinThickness, onFuzzySkinThicknessChange, "Fuzzy skin thickness (mm)", KeyboardType.Decimal)
            ProfileTextField(fuzzySkinPointDistance, onFuzzySkinPointDistanceChange, "Fuzzy skin point distance (mm)", KeyboardType.Decimal)
            ProfileDropdownField(
                label = "Fuzzy skin on first layer",
                selectedLabel = if (fuzzySkinFirstLayer) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = onFuzzySkinFirstLayerChange
            )
            ProfileDropdownField(
                label = "Fuzzy skin mode",
                selectedLabel = fuzzySkinMode.displayLabel,
                options = fuzzySkinModeOptions,
                onSelected = onFuzzySkinModeChange
            )
            ProfileDropdownField(
                label = "Fuzzy skin noise type",
                selectedLabel = fuzzySkinNoiseType.displayLabel,
                options = fuzzySkinNoiseOptions,
                onSelected = onFuzzySkinNoiseTypeChange
            )
        }

        if (ProfileEditorSetting.ProcessOthersAdvanced.isVisible(showAdvancedProfileSettings)) {
            ProfileGroupHeader("Skirt")
            ProfileDropdownField(
                label = "Skirt type",
                selectedLabel = skirtType.displayLabel,
                options = skirtTypeOptions,
                onSelected = onSkirtTypeChange
            )
            ProfileTextField(minSkirtLength, onMinSkirtLengthChange, "Skirt minimum extrusion length (mm)", KeyboardType.Decimal)
            ProfileTextField(skirtDistance, onSkirtDistanceChange, "Skirt distance (mm)", KeyboardType.Decimal)
            ProfileTextField(skirtStartAngle, onSkirtStartAngleChange, "Skirt start point (degrees)", KeyboardType.Decimal)
            ProfileTextField(skirtSpeed, onSkirtSpeedChange, "Skirt speed (mm/s)", KeyboardType.Decimal)
            ProfileDropdownField(
                label = "Draft shield",
                selectedLabel = draftShield.displayLabel,
                options = draftShieldOptions,
                onSelected = onDraftShieldChange
            )
            ProfileDropdownField(
                label = "Single loop draft shield",
                selectedLabel = if (singleLoopDraftShield) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = onSingleLoopDraftShieldChange
            )

            ProfileGroupHeader("Brim")
            ProfileTextField(brimObjectGap, onBrimObjectGapChange, "Brim-object gap (mm)", KeyboardType.Decimal)
            ProfileDropdownField(
                label = "Brim follows compensated outline",
                selectedLabel = if (brimUseEfcOutline) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = onBrimUseEfcOutlineChange
            )
            ProfileDropdownField(
                label = "Combine brims",
                selectedLabel = if (combineBrims) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = onCombineBrimsChange
            )
            ProfileDropdownField(
                label = "Brim ears",
                selectedLabel = if (brimEars) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = onBrimEarsChange
            )
            ProfileTextField(brimEarsDetectionLength, onBrimEarsDetectionLengthChange, "Brim ears detection length", KeyboardType.Decimal)
            ProfileTextField(brimEarsMaxAngle, onBrimEarsMaxAngleChange, "Brim ears max angle", KeyboardType.Decimal)

            ProfileGroupHeader("Special mode")
            ProfileDropdownField(
                label = "Slicing mode",
                selectedLabel = slicingMode.displayLabel,
                options = slicingModeOptions,
                onSelected = onSlicingModeChange
            )
            ProfileDropdownField(
                label = "Intra-layer order",
                selectedLabel = printOrder.displayLabel,
                options = printOrderOptions,
                onSelected = onPrintOrderChange
            )
            ProfileTextField(
                specialModeDetails.spiralModeMaxXySmoothing,
                { onSpecialModeDetailsChange(specialModeDetails.copy(spiralModeMaxXySmoothing = it)) },
                "Spiral mode max XY smoothing"
            )
            ProfileTextField(
                specialModeDetails.spiralStartingFlowRatio,
                { onSpecialModeDetailsChange(specialModeDetails.copy(spiralStartingFlowRatio = it)) },
                "Spiral starting flow ratio",
                KeyboardType.Decimal
            )
            ProfileTextField(
                specialModeDetails.spiralFinishingFlowRatio,
                { onSpecialModeDetailsChange(specialModeDetails.copy(spiralFinishingFlowRatio = it)) },
                "Spiral finishing flow ratio",
                KeyboardType.Decimal
            )
            ProfileDropdownField(
                label = "Enable wrapping detection",
                selectedLabel = if (specialModeDetails.enableWrappingDetection) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = { onSpecialModeDetailsChange(specialModeDetails.copy(enableWrappingDetection = it)) }
            )
            ProfileDropdownField(
                label = "Reduce infill retraction",
                selectedLabel = if (reduceInfillRetraction) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = onReduceInfillRetractionChange
            )

            ProfileGroupHeader("G-code output")
            ProfileDropdownField(
                label = "G-code add line number",
                selectedLabel = if (gcodeOutputDetails.gcodeAddLineNumber) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = { onGcodeOutputDetailsChange(gcodeOutputDetails.copy(gcodeAddLineNumber = it)) }
            )
            ProfileDropdownField(
                label = "G-code comments",
                selectedLabel = if (gcodeOutputDetails.gcodeComments) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = { onGcodeOutputDetailsChange(gcodeOutputDetails.copy(gcodeComments = it)) }
            )
            ProfileDropdownField(
                label = "G-code label objects",
                selectedLabel = if (gcodeOutputDetails.gcodeLabelObjects) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = { onGcodeOutputDetailsChange(gcodeOutputDetails.copy(gcodeLabelObjects = it)) }
            )
            ProfileDropdownField(
                label = "Exclude object",
                selectedLabel = if (gcodeOutputDetails.excludeObject) "Enabled" else "Disabled",
                options = boolEnabledDisabledOptions,
                onSelected = { onGcodeOutputDetailsChange(gcodeOutputDetails.copy(excludeObject = it)) }
            )
            ProfileTextField(filenameFormat, onFilenameFormatChange, "Filename format")

            ProfileGroupHeader("Post-processing Scripts")
            ProfileTextField(postProcessScripts, onPostProcessScriptsChange, "Post-processing scripts")

            ProfileGroupHeader("Notes")
            ProfileMultilineTextField(processNotes, onProcessNotesChange, "Notes")
        }

        if (ProfileEditorSetting.ProcessFuzzySkinDetail.isVisible(showAdvancedProfileSettings)) {
            ProfileGroupHeader("Fuzzy Skin Advanced")
            ProfileTextField(fuzzySkinScale, onFuzzySkinScaleChange, "Fuzzy skin scale (mm)", KeyboardType.Decimal)
            ProfileTextField(fuzzySkinOctaves, onFuzzySkinOctavesChange, "Fuzzy skin octaves", KeyboardType.Number)
            ProfileTextField(fuzzySkinPersistence, onFuzzySkinPersistenceChange, "Fuzzy skin persistence", KeyboardType.Decimal)
        }
    }
}
