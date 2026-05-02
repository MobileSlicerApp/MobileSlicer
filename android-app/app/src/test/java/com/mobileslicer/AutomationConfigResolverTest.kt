package com.mobileslicer

import com.mobileslicer.automation.AutomationConfigInput
import com.mobileslicer.automation.AutomationConfigResolver
import com.mobileslicer.profiles.ProfileStore
import com.mobileslicer.profiles.ProfileStoreRepository
import com.mobileslicer.profiles.SparseInfillPattern
import com.mobileslicer.profiles.SupportStyle
import com.mobileslicer.profiles.SupportType
import com.mobileslicer.profiles.activeConfiguration
import com.mobileslicer.profiles.newProcessProfileUnchecked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationConfigResolverTest {
    @Test
    fun returnsExplicitConfigJsonUnchanged() {
        val explicit = """{"layer_height":0.32}"""
        val resolved = resolver().resolve(
            mapInput("automation_config_json" to explicit)
        )

        assertEquals(explicit, resolved)
    }

    @Test
    fun appliesPrinterOverridesToNativeConfig() {
        val store = resolver().resolveProfileStore(
            mapInput(
                "automation_bed_width_mm" to 180f,
                "automation_bed_depth_mm" to 190f,
                "automation_max_height_mm" to 200f,
                "automation_nozzle_diameter_mm" to 0.6f
            )
        )
        val printer = store.activeConfiguration().printer

        assertEquals(180f, printer.bedWidthMm)
        assertEquals(190f, printer.bedDepthMm)
        assertEquals(200f, printer.maxHeightMm)
        assertEquals(0.6f, printer.nozzleDiameterMm)
        assertFalse(printer.builtIn)
    }

    @Test
    fun appliesProcessAndBridgeOverridesToNativeConfig() {
        val store = resolver().resolveProfileStore(
            mapInput(
                "automation_layer_height_mm" to 0.28f,
                "automation_wall_count" to 4,
                "automation_infill_percent" to 35,
                "automation_sparse_infill_pattern" to "gyroid",
                "automation_bridge_speed_mm_per_sec" to 18f,
                "automation_bridge_no_support" to true
            )
        )
        val process = store.activeConfiguration().process

        assertEquals(0.28f, process.layerHeightMm)
        assertEquals(4, process.wallCount)
        assertEquals(35, process.infillPercent)
        assertEquals(SparseInfillPattern.Gyroid, process.sparseInfillPattern)
        assertEquals(18f, process.bridgeSpeedMmPerSec)
        assertTrue(process.bridgeNoSupport)
        assertFalse(process.builtIn)
    }

    @Test
    fun appliesSupportOverridesToNativeConfig() {
        val store = resolver().resolveProfileStore(
            mapInput(
                "automation_enable_support" to true,
                "automation_support_type" to "tree(manual)",
                "automation_support_style" to "tree_hybrid",
                "automation_support_threshold_angle" to 42,
                "automation_support_buildplate_only" to true
            )
        )
        val process = store.activeConfiguration().process

        assertTrue(process.enableSupport)
        assertEquals(SupportType.TreeManual, process.supportType)
        assertEquals(SupportStyle.TreeHybrid, process.supportStyle)
        assertEquals(42, process.supportThresholdAngleDegrees)
        assertTrue(process.supportBuildplateOnly)
        assertFalse(process.builtIn)
    }

    @Test
    fun keepsBaseConfigWhenNoOverridesArePresent() {
        val config = resolver().resolveProfileStore(mapInput()).activeConfiguration()

        assertEquals(220f, config.printer.bedWidthMm)
        assertEquals(0.20f, config.process.layerHeightMm)
        assertFalse(config.process.bridgeNoSupport)
    }

    private fun resolver(): AutomationConfigResolver =
        AutomationConfigResolver(
            loadProfileStore = { defaultStore() },
            timestampMillis = { 123L }
        )

    private fun mapInput(vararg extras: Pair<String, Any>): AutomationConfigInput =
        MapAutomationConfigInput(extras.toMap())

    private fun defaultStore(): ProfileStore {
        val printers = listOf(ProfileStoreRepository.fallbackPrinterProfile())
        val filaments = ProfileStoreRepository.defaultFilamentProfiles()
        val processes = listOf(
            newProcessProfileUnchecked(
                0 to "process_fixture",
                1 to "Fixture Process",
                3 to false,
                5 to 0.20f,
                259 to printers.first().id,
                261 to printers.first().nozzleDiameterMm
            )
        )
        return ProfileStore(
            printers = printers,
            filaments = filaments,
            processes = processes,
            selectedPrinterId = printers.first().id,
            selectedFilamentId = filaments.first().id,
            selectedProcessId = processes.first().id
        )
    }

    private class MapAutomationConfigInput(
        private val extras: Map<String, Any>
    ) : AutomationConfigInput {
        override fun getStringExtra(name: String): String? = extras[name] as? String

        override fun getBooleanExtra(name: String, defaultValue: Boolean): Boolean =
            extras[name] as? Boolean ?: defaultValue

        override fun getFloatExtra(name: String, defaultValue: Float): Float =
            extras[name] as? Float ?: defaultValue

        override fun getIntExtra(name: String, defaultValue: Int): Int =
            extras[name] as? Int ?: defaultValue

        override fun hasExtra(name: String): Boolean = extras.containsKey(name)
    }
}
