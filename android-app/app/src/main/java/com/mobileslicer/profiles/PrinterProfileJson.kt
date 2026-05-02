package com.mobileslicer.profiles

import org.json.JSONArray
import org.json.JSONObject

internal fun PrinterProfile.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("subtitle", subtitle)
    .put("builtIn", builtIn)
    .put("bedWidthMm", bedWidthMm.toDouble())
    .put("bedDepthMm", bedDepthMm.toDouble())
    .put("maxHeightMm", maxHeightMm.toDouble())
    .put("bedExcludeArea", bedExcludeArea)
    .put("wrappingExcludeArea", wrappingExcludeArea)
    .put("headWrapDetectZone", headWrapDetectZone)
    .put("bedCustomTexture", bedCustomTexture)
    .put("bedCustomModel", bedCustomModel)
    .put("bedModel", bedModel)
    .put("bedModelAssetPath", bedModelAssetPath)
    .put("bedShape", bedShape)
    .put("bedTexture", bedTexture)
    .put("bedTextureAssetPath", bedTextureAssetPath)
    .put("bedTextureArea", bedTextureArea)
    .put("bottomTextureRect", bottomTextureRect)
    .put("bottomTextureEndName", bottomTextureEndName)
    .put("imageBedType", imageBedType)
    .put("useDoubleExtruderDefaultTexture", useDoubleExtruderDefaultTexture)
    .put("useRectGrid", useRectGrid)
    .put("supportMultiBedTypes", supportMultiBedTypes)
    .put("defaultBedType", defaultBedType.configValue)
    .put("bestObjectPosition", bestObjectPosition)
    .put("zOffsetMm", zOffsetMm.toDouble())
    .put("preferredOrientationDegrees", preferredOrientationDegrees.toDouble())
    .put("bedMeshMin", bedMeshMin)
    .put("bedMeshMax", bedMeshMax)
    .put("bedMeshProbeDistance", bedMeshProbeDistance)
    .put("adaptiveBedMeshMarginMm", adaptiveBedMeshMarginMm.toDouble())
    .put("nozzleDiameterMm", nozzleDiameterMm.toDouble())
    .put("filamentDiameterMm", filamentDiameterMm.toDouble())
    .put("nozzleVolumeMm3", nozzleVolumeMm3.toDouble())
    .put("nozzleVolumeType", nozzleVolumeType.configValue)
    .put("nozzleHeightMm", nozzleHeightMm.toDouble())
    .put("grabLengthMm", grabLengthMm.toDouble())
    .put("extruderVariantList", extruderVariantList)
    .put("printerExtruderId", printerExtruderId)
    .put("printerExtruderVariant", printerExtruderVariant)
    .put("masterExtruderId", masterExtruderId)
    .put("physicalExtruderMap", physicalExtruderMap)
    .put("extrudersCount", extrudersCount)
    .put("extruderAmsCount", extruderAmsCount)
    .put("extruderMaxNozzleCount", extruderMaxNozzleCount)
    .put("extruderType", extruderType.configValue)
    .put("extruderColor", extruderColor)
    .put("extruderPrintableHeightMm", extruderPrintableHeightMm.toDouble())
    .put("extruderPrintableArea", extruderPrintableArea)
    .put("minLayerHeightMm", minLayerHeightMm.toDouble())
    .put("maxLayerHeightMm", maxLayerHeightMm.toDouble())
    .put("extruderOffset", extruderOffset)
    .put("printerModel", printerModel)
    .put("machineTech", machineTech)
    .put("machineFamily", machineFamily)
    .put("printerTechnology", printerTechnology.configValue)
    .put("printerVariant", printerVariant)
    .put("hotendModel", hotendModel)
    .put("boxId", boxId)
    .put("enablePreHeating", enablePreHeating)
    .put("fanDirection", fanDirection)
    .put("hotendCoolingRate", hotendCoolingRate)
    .put("hotendHeatingRate", hotendHeatingRate)
    .put("activeFeederMotorName", activeFeederMotorName)
    .put("autoDisableFilterOnOverheat", autoDisableFilterOnOverheat)
    .put("autoToolchangeCommand", autoToolchangeCommand)
    .put("coolingFilterEnabled", coolingFilterEnabled)
    .put("crealityFlushTime", crealityFlushTime)
    .put("groupAlgoWithTime", groupAlgoWithTime)
    .put("isArtillery", isArtillery)
    .put("isSupport3mf", isSupport3mf)
    .put("isSupportAirCondition", isSupportAirCondition)
    .put("isSupportMqtt", isSupportMqtt)
    .put("isSupportMultiBox", isSupportMultiBox)
    .put("isSupportTimelapse", isSupportTimelapse)
    .put("machineLedLightExist", machineLedLightExist)
    .put("machineHotendChangeTime", machineHotendChangeTime)
    .put("machinePlatformMotionEnable", machinePlatformMotionEnable)
    .put("machinePrepareCompensationTime", machinePrepareCompensationTime)
    .put("multiZone", multiZone)
    .put("multiZoneNumber", multiZoneNumber)
    .put("nozzleFlushDataset", nozzleFlushDataset)
    .put("rammingPressureAdvanceValue", rammingPressureAdvanceValue)
    .put("rightIconOffsetBed", rightIconOffsetBed)
    .put("scanFolder", scanFolder)
    .put("supportBoxTempControl", supportBoxTempControl)
    .put("supportCoolingFilter", supportCoolingFilter)
    .put("supportMultiFilament", supportMultiFilament)
    .put("supportObjectSkipFlush", supportObjectSkipFlush)
    .put("supportWanNetwork", supportWanNetwork)
    .put("toolChangeTemperatureWait", toolChangeTemperatureWait)
    .put("upwardCompatibleMachine", upwardCompatibleMachine)
    .put("vendorUrl", vendorUrl)
    .put("useActivePelletFeeding", useActivePelletFeeding)
    .put("useExtruderRotationVolume", useExtruderRotationVolume)
    .put("printerStructure", printerStructure.configValue)
    .put("gcodeFlavor", gcodeFlavor.configValue)
    .put("pelletModdedPrinter", pelletModdedPrinter)
    .put("useThirdPartyPrintHost", useThirdPartyPrintHost)
    .put("scanFirstLayer", scanFirstLayer)
    .put("useRelativeEDistances", useRelativeEDistances)
    .put("useFirmwareRetraction", useFirmwareRetraction)
    .put("powerLossRecoveryMode", powerLossRecoveryMode.configValue)
    .put("disableM73", disableM73)
    .put("thumbnails", thumbnails)
    .put("thumbnailsInternal", thumbnailsInternal)
    .put("thumbnailsInternalSwitch", thumbnailsInternalSwitch)
    .put("remainingTimes", remainingTimes)
    .put("printHostType", printHostType.configValue)
    .put("printerAgent", printerAgent)
    .put("printHost", printHost)
    .put("printHostWebUi", printHostWebUi)
    .put("printHostAuthorizationType", printHostAuthorizationType.configValue)
    .put("printHostApiKey", printHostApiKey)
    .put("printHostPort", printHostPort)
    .put("printHostGroup", printHostGroup)
    .put("printHostCaFile", printHostCaFile)
    .put("printHostUser", printHostUser)
    .put("printHostPassword", printHostPassword)
    .put("printHostSslIgnoreRevoke", printHostSslIgnoreRevoke)
    .put("timeCost", timeCost.toDouble())
    .put("fanSpeedupTimeSeconds", fanSpeedupTimeSeconds.toDouble())
    .put("fanSpeedupOverhangsOnly", fanSpeedupOverhangsOnly)
    .put("fanKickstartTimeSeconds", fanKickstartTimeSeconds.toDouble())
    .put("extruderClearanceRadiusMm", extruderClearanceRadiusMm.toDouble())
    .put("extruderClearanceHeightToRodMm", extruderClearanceHeightToRodMm.toDouble())
    .put("extruderClearanceHeightToLidMm", extruderClearanceHeightToLidMm.toDouble())
    .put("extruderClearanceDistToRodMm", extruderClearanceDistToRodMm.toDouble())
    .put("nozzleType", nozzleType.configValue)
    .put("nozzleHrc", nozzleHrc)
    .put("auxiliaryFan", auxiliaryFan)
    .put("supportChamberTempControl", supportChamberTempControl)
    .put("supportAirFiltration", supportAirFiltration)
    .put("singleExtruderMultiMaterial", singleExtruderMultiMaterial)
    .put("manualFilamentChange", manualFilamentChange)
    .put("bedTemperatureFormula", bedTemperatureFormula.configValue)
    .put("wipeTowerType", wipeTowerType.configValue)
    .put("purgeInPrimeTower", purgeInPrimeTower)
    .put("enableFilamentRamming", enableFilamentRamming)
    .put("coolingTubeRetractionMm", coolingTubeRetractionMm.toDouble())
    .put("coolingTubeLengthMm", coolingTubeLengthMm.toDouble())
    .put("parkingPositionRetractionMm", parkingPositionRetractionMm.toDouble())
    .put("extraLoadingMoveMm", extraLoadingMoveMm.toDouble())
    .put("highCurrentOnFilamentSwap", highCurrentOnFilamentSwap)
    .put("machineLoadFilamentTimeSeconds", machineLoadFilamentTimeSeconds.toDouble())
    .put("machineUnloadFilamentTimeSeconds", machineUnloadFilamentTimeSeconds.toDouble())
    .put("machineToolChangeTimeSeconds", machineToolChangeTimeSeconds.toDouble())
    .put("fileStartGcode", fileStartGcode)
    .put("machineStartGcode", machineStartGcode)
    .put("machineEndGcode", machineEndGcode)
    .put("printingByObjectGcode", printingByObjectGcode)
    .put("beforeLayerChangeGcode", beforeLayerChangeGcode)
    .put("layerChangeGcode", layerChangeGcode)
    .put("timeLapseGcode", timeLapseGcode)
    .put("wrappingDetectionGcode", wrappingDetectionGcode)
    .put("changeFilamentGcode", changeFilamentGcode)
    .put("changeExtrusionRoleGcode", changeExtrusionRoleGcode)
    .put("machinePauseGcode", machinePauseGcode)
    .put("templateCustomGcode", templateCustomGcode)
    .put("emitMachineLimitsToGcode", emitMachineLimitsToGcode)
    .put("resonanceAvoidance", resonanceAvoidance)
    .put("silentMode", silentMode)
    .put("minResonanceAvoidanceSpeedMmPerSec", minResonanceAvoidanceSpeedMmPerSec.toDouble())
    .put("maxResonanceAvoidanceSpeedMmPerSec", maxResonanceAvoidanceSpeedMmPerSec.toDouble())
    .put("machineMaxSpeedX", machineMaxSpeedX.toDouble())
    .put("machineMaxSpeedY", machineMaxSpeedY.toDouble())
    .put("machineMaxSpeedZ", machineMaxSpeedZ.toDouble())
    .put("machineMaxSpeedE", machineMaxSpeedE.toDouble())
    .put("machineMaxAccelerationX", machineMaxAccelerationX.toDouble())
    .put("machineMaxAccelerationY", machineMaxAccelerationY.toDouble())
    .put("machineMaxAccelerationZ", machineMaxAccelerationZ.toDouble())
    .put("machineMaxAccelerationE", machineMaxAccelerationE.toDouble())
    .put("machineMaxAccelerationExtruding", machineMaxAccelerationExtruding.toDouble())
    .put("machineMaxAccelerationRetracting", machineMaxAccelerationRetracting.toDouble())
    .put("machineMaxAccelerationTravel", machineMaxAccelerationTravel.toDouble())
    .put("machineMaxJerkX", machineMaxJerkX.toDouble())
    .put("machineMaxJerkY", machineMaxJerkY.toDouble())
    .put("machineMaxJerkZ", machineMaxJerkZ.toDouble())
    .put("machineMaxJerkE", machineMaxJerkE.toDouble())
    .put("machineMaxJunctionDeviation", machineMaxJunctionDeviation.toDouble())
    .put("machineMinExtrudingRateMmPerSec", machineMinExtrudingRateMmPerSec.toDouble())
    .put("machineMinTravelRateMmPerSec", machineMinTravelRateMmPerSec.toDouble())
    .put("retractionLengthMm", retractionLengthMm.toDouble())
    .put("retractRestartExtraMm", retractRestartExtraMm.toDouble())
    .put("retractionSpeedMmPerSec", retractionSpeedMmPerSec.toDouble())
    .put("deretractionSpeedMmPerSec", deretractionSpeedMmPerSec.toDouble())
    .put("retractionMinimumTravelMm", retractionMinimumTravelMm.toDouble())
    .put("retractWhenChangingLayer", retractWhenChangingLayer)
    .put("retractOnTopLayer", retractOnTopLayer)
    .put("wipe", wipe)
    .put("wipeDistanceMm", wipeDistanceMm.toDouble())
    .put("retractBeforeWipePercent", retractBeforeWipePercent)
    .put("retractLiftEnforce", retractLiftEnforce.configValue)
    .put("zHopType", zHopType.configValue)
    .put("zHopWhenPrime", zHopWhenPrime)
    .put("zLiftType", zLiftType)
    .put("zHopMm", zHopMm.toDouble())
    .put("travelSlopeDegrees", travelSlopeDegrees.toDouble())
    .put("retractLiftAboveMm", retractLiftAboveMm.toDouble())
    .put("retractLiftBelowMm", retractLiftBelowMm.toDouble())
    .put("retractLengthToolchangeMm", retractLengthToolchangeMm.toDouble())
    .put("retractRestartExtraToolchangeMm", retractRestartExtraToolchangeMm.toDouble())
    .put("enableLongRetractionWhenCut", enableLongRetractionWhenCut.configValue)
    .put("longRetractionsWhenCut", longRetractionsWhenCut)
    .put("retractionDistanceWhenCutMm", retractionDistanceWhenCutMm.toDouble())
    .put("printerNotes", printerNotes)
    .put("profileSource", profileSource)
    .put("thumbnailAssetPath", thumbnailAssetPath)
    .put("orcaFamily", orcaFamily)
	    .put("orcaMachineModelPath", orcaMachineModelPath)
	    .put("orcaMachineModelJson", orcaMachineModelJson)
	    .put("orcaResolvedMachineJson", orcaResolvedMachineJson)
	    .put("orcaMachineOverridesJson", orcaMachineOverridesJson)
	    .put("orcaNozzleMachinePaths", JSONArray().apply { orcaNozzleMachinePaths.forEach { put(it) } })
    .put("orcaNozzleMachineJsons", JSONArray().apply { orcaNozzleMachineJsons.forEach { put(it) } })
    .put("orcaResolvedMachineJsons", JSONArray().apply { orcaResolvedMachineJsons.forEach { put(it) } })
    .put("orcaResolvedSourceChains", JSONArray().apply { orcaResolvedSourceChains.forEach { put(it) } })
    .put("availableNozzleDiameters", JSONArray().apply { availableNozzleDiameters.forEach { put(it.toDouble()) } })

internal fun JSONObject.toPrinterProfile(): PrinterProfile = PrinterProfile(
    id = getString("id"),
    name = getString("name"),
    subtitle = optString("subtitle", ""),
    builtIn = optBoolean("builtIn", false),
    bedWidthMm = optDouble("bedWidthMm", 220.0).toFloat(),
    bedDepthMm = optDouble("bedDepthMm", 220.0).toFloat(),
    maxHeightMm = optDouble("maxHeightMm", 220.0).toFloat(),
    bedExcludeArea = optString("bedExcludeArea", ""),
    wrappingExcludeArea = optString("wrappingExcludeArea", ""),
    headWrapDetectZone = optString("headWrapDetectZone", ""),
    bedCustomTexture = optString("bedCustomTexture", ""),
    bedCustomModel = optString("bedCustomModel", ""),
    bedModel = optString("bedModel", ""),
    bedModelAssetPath = optString("bedModelAssetPath", ""),
    bedShape = optString("bedShape", ""),
    bedTexture = optString("bedTexture", ""),
    bedTextureAssetPath = optString("bedTextureAssetPath", ""),
    bedTextureArea = optString("bedTextureArea", ""),
    bottomTextureRect = optString("bottomTextureRect", ""),
    bottomTextureEndName = optString("bottomTextureEndName", ""),
    imageBedType = optString("imageBedType", ""),
    useDoubleExtruderDefaultTexture = optString("useDoubleExtruderDefaultTexture", ""),
    useRectGrid = optBoolean("useRectGrid", false),
    supportMultiBedTypes = optBoolean("supportMultiBedTypes", false),
    defaultBedType = DefaultBedType.fromConfigValue(optString("defaultBedType", "")),
    bestObjectPosition = optString("bestObjectPosition", "0.5x0.5"),
    zOffsetMm = optDouble("zOffsetMm", 0.0).toFloat(),
    preferredOrientationDegrees = optDouble("preferredOrientationDegrees", 0.0).toFloat(),
    bedMeshMin = optString("bedMeshMin", "-99999x-99999"),
    bedMeshMax = optString("bedMeshMax", "99999x99999"),
    bedMeshProbeDistance = optString("bedMeshProbeDistance", "50x50"),
    adaptiveBedMeshMarginMm = optDouble("adaptiveBedMeshMarginMm", 0.0).toFloat(),
    nozzleDiameterMm = optDouble("nozzleDiameterMm", 0.4).toFloat(),
    filamentDiameterMm = optDouble("filamentDiameterMm", 1.75).toFloat(),
    nozzleVolumeMm3 = optDouble("nozzleVolumeMm3", 0.0).toFloat(),
    nozzleVolumeType = NozzleVolumeType.fromConfigValue(optString("nozzleVolumeType", "Standard")),
    nozzleHeightMm = optDouble("nozzleHeightMm", 2.5).toFloat(),
    grabLengthMm = optDouble("grabLengthMm", 0.0).toFloat(),
    extruderVariantList = optString("extruderVariantList", "Direct Drive Standard"),
    printerExtruderId = optString("printerExtruderId", "1"),
    printerExtruderVariant = optString("printerExtruderVariant", "Direct Drive Standard"),
    masterExtruderId = optInt("masterExtruderId", 1),
    physicalExtruderMap = optString("physicalExtruderMap", "0"),
    extrudersCount = optString("extrudersCount", ""),
    extruderAmsCount = optString("extruderAmsCount", ""),
    extruderMaxNozzleCount = optString("extruderMaxNozzleCount", ""),
    extruderType = ExtruderType.fromConfigValue(optString("extruderType", "Direct Drive")),
    extruderColor = optString("extruderColor", ""),
    extruderPrintableHeightMm = optDouble("extruderPrintableHeightMm", 0.0).toFloat(),
    extruderPrintableArea = optString("extruderPrintableArea", ""),
    minLayerHeightMm = optDouble("minLayerHeightMm", 0.07).toFloat(),
    maxLayerHeightMm = optDouble("maxLayerHeightMm", 0.0).toFloat(),
    extruderOffset = optString("extruderOffset", "0x0"),
    printerModel = optString("printerModel", ""),
    machineTech = optString("machineTech", ""),
    machineFamily = optString("machineFamily", ""),
    printerTechnology = PrinterTechnology.fromConfigValue(optString("printerTechnology", "FFF")),
    printerVariant = optString("printerVariant", ""),
    hotendModel = optString("hotendModel", ""),
    boxId = optString("boxId", ""),
    enablePreHeating = optBoolean("enablePreHeating", false),
    fanDirection = optString("fanDirection", ""),
    hotendCoolingRate = optString("hotendCoolingRate", ""),
    hotendHeatingRate = optString("hotendHeatingRate", ""),
    activeFeederMotorName = optString("activeFeederMotorName", ""),
    autoDisableFilterOnOverheat = optString("autoDisableFilterOnOverheat", ""),
    autoToolchangeCommand = optString("autoToolchangeCommand", ""),
    coolingFilterEnabled = optString("coolingFilterEnabled", ""),
    crealityFlushTime = optString("crealityFlushTime", ""),
    groupAlgoWithTime = optString("groupAlgoWithTime", ""),
    isArtillery = optString("isArtillery", ""),
    isSupport3mf = optString("isSupport3mf", ""),
    isSupportAirCondition = optString("isSupportAirCondition", ""),
    isSupportMqtt = optString("isSupportMqtt", ""),
    isSupportMultiBox = optString("isSupportMultiBox", ""),
    isSupportTimelapse = optString("isSupportTimelapse", ""),
    machineLedLightExist = optString("machineLedLightExist", ""),
    machineHotendChangeTime = optString("machineHotendChangeTime", ""),
    machinePlatformMotionEnable = optString("machinePlatformMotionEnable", ""),
    machinePrepareCompensationTime = optString("machinePrepareCompensationTime", ""),
    multiZone = optString("multiZone", ""),
    multiZoneNumber = optString("multiZoneNumber", ""),
    nozzleFlushDataset = optString("nozzleFlushDataset", ""),
    rammingPressureAdvanceValue = optString("rammingPressureAdvanceValue", ""),
    rightIconOffsetBed = optString("rightIconOffsetBed", ""),
    scanFolder = optString("scanFolder", ""),
    supportBoxTempControl = optString("supportBoxTempControl", ""),
    supportCoolingFilter = optString("supportCoolingFilter", ""),
    supportMultiFilament = optString("supportMultiFilament", ""),
    supportObjectSkipFlush = optString("supportObjectSkipFlush", ""),
    supportWanNetwork = optString("supportWanNetwork", ""),
    toolChangeTemperatureWait = optString("toolChangeTemperatureWait", ""),
    upwardCompatibleMachine = optString("upwardCompatibleMachine", ""),
    vendorUrl = optString("vendorUrl", ""),
    useActivePelletFeeding = optString("useActivePelletFeeding", ""),
    useExtruderRotationVolume = optString("useExtruderRotationVolume", ""),
    printerStructure = PrinterStructure.fromConfigValue(optString("printerStructure", "undefine")),
    gcodeFlavor = GcodeFlavor.fromConfigValue(optString("gcodeFlavor", "marlin")),
    pelletModdedPrinter = optBoolean("pelletModdedPrinter", false),
    useThirdPartyPrintHost = optBoolean("useThirdPartyPrintHost", false),
    scanFirstLayer = optBoolean("scanFirstLayer", false),
    printerNotes = optString("printerNotes", ""),
    useRelativeEDistances = optBoolean("useRelativeEDistances", true),
    useFirmwareRetraction = optBoolean("useFirmwareRetraction", false),
    powerLossRecoveryMode = PowerLossRecoveryMode.fromConfigValue(optString("powerLossRecoveryMode", "printer_configuration")),
    disableM73 = optBoolean("disableM73", false),
    thumbnails = optString("thumbnails", "48x48/PNG,300x300/PNG"),
    thumbnailsInternal = optString("thumbnailsInternal", ""),
    thumbnailsInternalSwitch = optString("thumbnailsInternalSwitch", ""),
    remainingTimes = optString("remainingTimes", ""),
    printHostType = PrintHostType.fromConfigValue(optString("printHostType", "octoprint")),
    printerAgent = optString("printerAgent", ""),
    printHost = optString("printHost", ""),
    printHostWebUi = optString("printHostWebUi", ""),
    printHostAuthorizationType = PrintHostAuthorizationType.fromConfigValue(optString("printHostAuthorizationType", "key")),
    printHostApiKey = optString("printHostApiKey", ""),
    printHostPort = optString("printHostPort", ""),
    printHostGroup = optString("printHostGroup", ""),
    printHostCaFile = optString("printHostCaFile", ""),
    printHostUser = optString("printHostUser", ""),
    printHostPassword = optString("printHostPassword", ""),
    printHostSslIgnoreRevoke = optBoolean("printHostSslIgnoreRevoke", false),
    timeCost = optDouble("timeCost", 0.0).toFloat(),
    fanSpeedupTimeSeconds = optDouble("fanSpeedupTimeSeconds", 0.0).toFloat(),
    fanSpeedupOverhangsOnly = optBoolean("fanSpeedupOverhangsOnly", true),
    fanKickstartTimeSeconds = optDouble("fanKickstartTimeSeconds", 0.0).toFloat(),
    extruderClearanceRadiusMm = optDouble("extruderClearanceRadiusMm", 40.0).toFloat(),
    extruderClearanceHeightToRodMm = optDouble("extruderClearanceHeightToRodMm", 40.0).toFloat(),
    extruderClearanceHeightToLidMm = optDouble("extruderClearanceHeightToLidMm", 120.0).toFloat(),
    extruderClearanceDistToRodMm = optDouble("extruderClearanceDistToRodMm", 0.0).toFloat(),
    nozzleType = NozzleType.fromConfigValue(optString("nozzleType", "undefine")),
    nozzleHrc = optInt("nozzleHrc", 0).coerceIn(0, 500),
    auxiliaryFan = optBoolean("auxiliaryFan", false),
    supportChamberTempControl = optBoolean("supportChamberTempControl", true),
    supportAirFiltration = optBoolean("supportAirFiltration", true),
    singleExtruderMultiMaterial = optBoolean("singleExtruderMultiMaterial", true),
    manualFilamentChange = optBoolean("manualFilamentChange", false),
    bedTemperatureFormula = BedTemperatureFormula.fromConfigValue(optString("bedTemperatureFormula", "by_highest_temp")),
    wipeTowerType = WipeTowerType.fromConfigValue(optString("wipeTowerType", "type2")),
    purgeInPrimeTower = optBoolean("purgeInPrimeTower", true),
    enableFilamentRamming = optBoolean("enableFilamentRamming", true),
    coolingTubeRetractionMm = optDouble("coolingTubeRetractionMm", 91.5).toFloat(),
    coolingTubeLengthMm = optDouble("coolingTubeLengthMm", 5.0).toFloat(),
    parkingPositionRetractionMm = optDouble("parkingPositionRetractionMm", 92.0).toFloat(),
    extraLoadingMoveMm = optDouble("extraLoadingMoveMm", -2.0).toFloat(),
    highCurrentOnFilamentSwap = optBoolean("highCurrentOnFilamentSwap", false),
    machineLoadFilamentTimeSeconds = optDouble("machineLoadFilamentTimeSeconds", 0.0).toFloat(),
    machineUnloadFilamentTimeSeconds = optDouble("machineUnloadFilamentTimeSeconds", 0.0).toFloat(),
    machineToolChangeTimeSeconds = optDouble("machineToolChangeTimeSeconds", 0.0).toFloat(),
    fileStartGcode = optString("fileStartGcode", ""),
    machineStartGcode = optString("machineStartGcode", ""),
    machineEndGcode = optString("machineEndGcode", ""),
    printingByObjectGcode = optString("printingByObjectGcode", ""),
    beforeLayerChangeGcode = optString("beforeLayerChangeGcode", ""),
    layerChangeGcode = optString("layerChangeGcode", ""),
    timeLapseGcode = optString("timeLapseGcode", ""),
    wrappingDetectionGcode = optString("wrappingDetectionGcode", ""),
    changeFilamentGcode = optString("changeFilamentGcode", ""),
    changeExtrusionRoleGcode = optString("changeExtrusionRoleGcode", ""),
    machinePauseGcode = optString("machinePauseGcode", ""),
    templateCustomGcode = optString("templateCustomGcode", ""),
    emitMachineLimitsToGcode = optBoolean("emitMachineLimitsToGcode", true),
    resonanceAvoidance = optBoolean("resonanceAvoidance", false),
    silentMode = optBoolean("silentMode", false),
    minResonanceAvoidanceSpeedMmPerSec = optDouble("minResonanceAvoidanceSpeedMmPerSec", 70.0).toFloat(),
    maxResonanceAvoidanceSpeedMmPerSec = optDouble("maxResonanceAvoidanceSpeedMmPerSec", 120.0).toFloat(),
    machineMaxSpeedX = optDouble("machineMaxSpeedX", 500.0).toFloat(),
    machineMaxSpeedY = optDouble("machineMaxSpeedY", 500.0).toFloat(),
    machineMaxSpeedZ = optDouble("machineMaxSpeedZ", 12.0).toFloat(),
    machineMaxSpeedE = optDouble("machineMaxSpeedE", 120.0).toFloat(),
    machineMaxAccelerationX = optDouble("machineMaxAccelerationX", 1000.0).toFloat(),
    machineMaxAccelerationY = optDouble("machineMaxAccelerationY", 1000.0).toFloat(),
    machineMaxAccelerationZ = optDouble("machineMaxAccelerationZ", 500.0).toFloat(),
    machineMaxAccelerationE = optDouble("machineMaxAccelerationE", 5000.0).toFloat(),
    machineMaxAccelerationExtruding = optDouble("machineMaxAccelerationExtruding", 1500.0).toFloat(),
    machineMaxAccelerationRetracting = optDouble("machineMaxAccelerationRetracting", 1500.0).toFloat(),
    machineMaxAccelerationTravel = optDouble("machineMaxAccelerationTravel", 0.0).toFloat(),
    machineMaxJerkX = optDouble("machineMaxJerkX", 10.0).toFloat(),
    machineMaxJerkY = optDouble("machineMaxJerkY", 10.0).toFloat(),
    machineMaxJerkZ = optDouble("machineMaxJerkZ", 0.2).toFloat(),
    machineMaxJerkE = optDouble("machineMaxJerkE", 2.5).toFloat(),
    machineMaxJunctionDeviation = optDouble("machineMaxJunctionDeviation", 0.01).toFloat(),
    machineMinExtrudingRateMmPerSec = optDouble("machineMinExtrudingRateMmPerSec", 0.0).toFloat(),
    machineMinTravelRateMmPerSec = optDouble("machineMinTravelRateMmPerSec", 0.0).toFloat(),
    retractionLengthMm = optDouble("retractionLengthMm", 0.8).toFloat(),
    retractRestartExtraMm = optDouble("retractRestartExtraMm", 0.0).toFloat(),
    retractionSpeedMmPerSec = optDouble("retractionSpeedMmPerSec", 30.0).toFloat(),
    deretractionSpeedMmPerSec = optDouble("deretractionSpeedMmPerSec", 0.0).toFloat(),
    retractionMinimumTravelMm = optDouble("retractionMinimumTravelMm", 2.0).toFloat(),
    retractWhenChangingLayer = optBoolean("retractWhenChangingLayer", false),
    retractOnTopLayer = optString("retractOnTopLayer", ""),
    wipe = optBoolean("wipe", false),
    wipeDistanceMm = optDouble("wipeDistanceMm", 1.0).toFloat(),
    retractBeforeWipePercent = optInt("retractBeforeWipePercent", 100),
    retractLiftEnforce = RetractLiftEnforce.fromConfigValue(optString("retractLiftEnforce", "All Surfaces")),
    zHopType = ZHopType.fromConfigValue(optString("zHopType", "Slope Lift")),
    zHopWhenPrime = optString("zHopWhenPrime", ""),
    zLiftType = optString("zLiftType", ""),
    zHopMm = optDouble("zHopMm", 0.4).toFloat(),
    travelSlopeDegrees = optDouble("travelSlopeDegrees", 3.0).toFloat(),
    retractLiftAboveMm = optDouble("retractLiftAboveMm", 0.0).toFloat(),
    retractLiftBelowMm = optDouble("retractLiftBelowMm", 0.0).toFloat(),
    retractLengthToolchangeMm = optDouble("retractLengthToolchangeMm", 10.0).toFloat(),
    retractRestartExtraToolchangeMm = optDouble("retractRestartExtraToolchangeMm", 0.0).toFloat(),
    enableLongRetractionWhenCut = LongRetractionWhenCutMode.fromConfigValue(optInt("enableLongRetractionWhenCut", 0)),
    longRetractionsWhenCut = optBoolean("longRetractionsWhenCut", false),
    retractionDistanceWhenCutMm = optDouble("retractionDistanceWhenCutMm", 18.0).toFloat(),
    profileSource = optString("profileSource", if (optBoolean("builtIn", false)) "builtin" else "custom"),
    thumbnailAssetPath = optString("thumbnailAssetPath", ""),
    orcaFamily = optString("orcaFamily", ""),
	    orcaMachineModelPath = optString("orcaMachineModelPath", ""),
	    orcaMachineModelJson = optString("orcaMachineModelJson", ""),
	    orcaResolvedMachineJson = optString("orcaResolvedMachineJson", ""),
	    orcaMachineOverridesJson = optString("orcaMachineOverridesJson", ""),
	    orcaNozzleMachinePaths = optJSONArray("orcaNozzleMachinePaths").toProfileJsonStringList(),
    orcaNozzleMachineJsons = optJSONArray("orcaNozzleMachineJsons").toProfileJsonStringList(),
    orcaResolvedMachineJsons = optJSONArray("orcaResolvedMachineJsons").toProfileJsonStringList(),
    orcaResolvedSourceChains = optJSONArray("orcaResolvedSourceChains").toProfileJsonStringList(),
    availableNozzleDiameters = optJSONArray("availableNozzleDiameters").toProfileJsonFloatList()
)
