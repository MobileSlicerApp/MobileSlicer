package com.mobileslicer.profiles

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal data class OrcaPrinterPreset(
    val name: String,
    val family: String,
    val searchText: String,
    val nozzleDiameters: String,
    val profilePath: String,
    val coverAssetPath: String,
    val importBundleAssetPath: String,
    val bedModelAssetPath: String,
    val bedTextureAssetPath: String,
    val bedWidthMm: Float,
    val bedDepthMm: Float,
    val maxHeightMm: Float,
    val activeNozzleDiameterMm: Float,
    val nozzleMachinePaths: List<String>,
    val resolvedSourceChains: List<String>
)

internal data class OrcaPrinterImportBundle(
    val machineModelJson: String,
    val resolvedMachineJson: String,
    val nozzleMachineJsons: List<String>,
    val resolvedMachineJsons: List<String>,
    val processPresets: List<OrcaProcessPresetBundle>
)

internal data class OrcaProcessPresetBundle(
    val machineName: String,
    val nozzleDiameterMm: Float,
    val name: String,
    val rawName: String,
    val profilePath: String,
    val rawProcessJson: String,
    val resolvedProcessJson: String,
    val resolvedSourceChain: List<String>
)

internal data class OrcaFilamentPreset(
    val name: String,
    val rawName: String,
    val family: String,
    val materialType: String,
    val vendor: String,
    val defaultFilamentColor: String,
    val profilePath: String,
    val importBundleAssetPath: String,
    val compatiblePrinters: List<String>,
    val compatiblePrinterKeys: List<String> = emptyList(),
    val pickerDuplicateKey: String = "",
    val searchText: String
)

internal data class OrcaFilamentImportBundle(
    val rawFilamentJson: String,
    val resolvedFilamentJson: String,
    val resolvedSourceChain: List<String>
)

private val orcaProfileAssetCacheLock = Any()
private var cachedOrcaPrinterPresets: List<OrcaPrinterPreset>? = null
private var cachedOrcaFilamentPresets: List<OrcaFilamentPreset>? = null
private val cachedOrcaPrinterImportBundles = object : LinkedHashMap<String, OrcaPrinterImportBundle>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, OrcaPrinterImportBundle>?): Boolean =
        size > 12
}
private val cachedOrcaFilamentImportBundles = object : LinkedHashMap<String, OrcaFilamentImportBundle>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, OrcaFilamentImportBundle>?): Boolean =
        size > 16
}

internal fun loadOrcaPrinterPresets(context: Context): List<OrcaPrinterPreset> {
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaPrinterPresets?.let { return it }
    }
    val parsed = runCatching {
        val manifest = context.assets.open("orca-printers/printer_presets.json").use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
        val array = JSONArray(manifest)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            OrcaPrinterPreset(
                name = item.optString("name"),
                family = item.optString("family"),
                searchText = item.optString("searchText").ifBlank {
                    "${item.optString("name")} ${item.optString("family")}".lowercase(Locale.US)
                },
                nozzleDiameters = item.optString("nozzleDiameters"),
                profilePath = item.optString("profilePath"),
                coverAssetPath = item.optString("coverAssetPath"),
                importBundleAssetPath = item.optString("importBundleAssetPath"),
                bedModelAssetPath = item.optString("bedModelAssetPath"),
                bedTextureAssetPath = item.optString("bedTextureAssetPath"),
                bedWidthMm = item.optDouble("bedWidthMm", 220.0).toFloat(),
                bedDepthMm = item.optDouble("bedDepthMm", 220.0).toFloat(),
                maxHeightMm = item.optDouble("maxHeightMm", 220.0).toFloat(),
                activeNozzleDiameterMm = item.optDouble("activeNozzleDiameterMm", 0.4).toFloat(),
                nozzleMachinePaths = item.optJSONArray("nozzleMachinePaths").toOrcaStringList(),
                resolvedSourceChains = item.optJSONArray("resolvedSourceChains").toOrcaStringList()
            )
        }
    }.getOrDefault(emptyList())
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaPrinterPresets?.let { return it }
        cachedOrcaPrinterPresets = parsed
    }
    return parsed
}

internal fun loadOrcaPrinterImportBundle(
    context: Context,
    preset: OrcaPrinterPreset
): OrcaPrinterImportBundle {
    if (preset.importBundleAssetPath.isBlank()) {
        return OrcaPrinterImportBundle(
            machineModelJson = "",
            resolvedMachineJson = "",
            nozzleMachineJsons = emptyList(),
            resolvedMachineJsons = emptyList(),
            processPresets = emptyList()
        )
    }
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaPrinterImportBundles[preset.importBundleAssetPath]?.let { return it }
    }
    val bundleText = context.assets.open(preset.importBundleAssetPath).use { input ->
        input.readBytes().toString(StandardCharsets.UTF_8)
    }
    val bundle = JSONObject(bundleText)
    val parsed = OrcaPrinterImportBundle(
        machineModelJson = bundle.optString("machineModelJson"),
        resolvedMachineJson = bundle.optString("resolvedMachineJson"),
        nozzleMachineJsons = bundle.optJSONArray("nozzleMachineJsons").toOrcaStringList(),
        resolvedMachineJsons = bundle.optJSONArray("resolvedMachineJsons").toOrcaStringList(),
        processPresets = bundle.optJSONArray("processPresets").toOrcaProcessPresetBundles()
    )
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaPrinterImportBundles[preset.importBundleAssetPath] = parsed
    }
    return parsed
}

internal fun loadOrcaFilamentPresets(context: Context): List<OrcaFilamentPreset> {
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaFilamentPresets?.let { return it }
    }
    val parsed = runCatching {
        val manifest = context.assets.open("orca-filaments/filament_presets.json").use { input ->
            input.readBytes().toString(StandardCharsets.UTF_8)
        }
        val array = JSONArray(manifest)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            OrcaFilamentPreset(
                name = item.optString("name"),
                rawName = item.optString("rawName"),
                family = item.optString("family"),
                materialType = item.optString("materialType"),
                vendor = item.optString("vendor"),
                defaultFilamentColor = item.optString("defaultFilamentColor"),
                profilePath = item.optString("profilePath"),
                importBundleAssetPath = item.optString("importBundleAssetPath"),
                compatiblePrinters = item.optJSONArray("compatiblePrinters").toOrcaStringList(),
                compatiblePrinterKeys = item.optJSONArray("compatiblePrinterKeys").toOrcaStringList(),
                pickerDuplicateKey = item.optString("pickerDuplicateKey"),
                searchText = item.optString("searchText").ifBlank {
                    "${item.optString("name")} ${item.optString("materialType")}".lowercase(Locale.US)
                }
            )
        }
    }.getOrDefault(emptyList())
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaFilamentPresets?.let { return it }
        cachedOrcaFilamentPresets = parsed
    }
    return parsed
}

internal fun loadOrcaFilamentImportBundle(
    context: Context,
    preset: OrcaFilamentPreset
): OrcaFilamentImportBundle {
    if (preset.importBundleAssetPath.isBlank()) {
        return OrcaFilamentImportBundle(
            rawFilamentJson = "",
            resolvedFilamentJson = "",
            resolvedSourceChain = emptyList()
        )
    }
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaFilamentImportBundles[preset.importBundleAssetPath]?.let { return it }
    }
    val bundleText = context.assets.open(preset.importBundleAssetPath).use { input ->
        input.readBytes().toString(StandardCharsets.UTF_8)
    }
    val bundle = JSONObject(bundleText)
    val parsed = OrcaFilamentImportBundle(
        rawFilamentJson = bundle.optString("rawFilamentJson"),
        resolvedFilamentJson = bundle.optString("resolvedFilamentJson"),
        resolvedSourceChain = bundle.optJSONArray("resolvedSourceChain").toOrcaStringList()
    )
    synchronized(orcaProfileAssetCacheLock) {
        cachedOrcaFilamentImportBundles[preset.importBundleAssetPath] = parsed
    }
    return parsed
}

internal fun OrcaPrinterPreset.nozzleDiametersList(): List<Float> =
    nozzleDiameters.split(';', ',')
        .mapNotNull { it.trim().toFloatOrNull() }
        .filter { it > 0f }
        .distinct()
        .sorted()

internal fun formatNozzle(nozzle: Float): String =
    String.format(Locale.US, "%.2f", nozzle).trimEnd('0').trimEnd('.')

private fun JSONArray?.toOrcaStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val value = opt(index)
        if (value is JSONArray) {
            List(value.length()) { nestedIndex -> value.optString(nestedIndex) }.joinToString(" > ")
        } else {
            optString(index)
        }
    }.filter { it.isNotBlank() }
}

private fun JSONArray?.toOrcaProcessPresetBundles(): List<OrcaProcessPresetBundle> {
    if (this == null) return emptyList()
    return List(length()) { index ->
        val item = getJSONObject(index)
        OrcaProcessPresetBundle(
            machineName = item.optString("machineName"),
            nozzleDiameterMm = item.optDouble("nozzleDiameterMm", 0.4).toFloat(),
            name = item.optString("name"),
            rawName = item.optString("rawName"),
            profilePath = item.optString("profilePath"),
            rawProcessJson = item.optString("rawProcessJson"),
            resolvedProcessJson = item.optString("resolvedProcessJson"),
            resolvedSourceChain = item.optJSONArray("resolvedSourceChain").toOrcaStringList()
        )
    }
}
