package com.mobileslicer.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GcodePreviewRendererTest {
    @Test
    fun previewPerformanceDefaultsToMidRange() {
        assertEquals(GcodePreviewPerformanceMode.MidRange, GcodePreviewPerformanceMode.Default)
        assertEquals(GcodePreviewPerformanceMode.MidRange, GcodePreviewPerformanceMode.fromStoredName(null))
        assertEquals(750_000L, GcodePreviewPerformanceMode.Default.vertexBudget)
    }

    @Test
    fun startsInactive() {
        val renderer = GcodePreviewRenderer()

        assertFalse(renderer.isActive)
    }

    @Test
    fun currentLoadStateMapsRequestedWindowToLocalRange() {
        val renderer = GcodePreviewRenderer()

        val state = renderer.currentLoadState(requestedLayerMin = 12L, requestedLayerMax = 20L)

        assertEquals(12L, state.loadedLayerStart)
        assertEquals(20L, state.loadedLayerEnd)
        assertEquals(0L, state.localLayerMin)
        assertEquals(8L, state.localLayerMax)
    }

    @Test
    fun parsePreviewRangePlanConvertsNativeZeroBasedRangesToUiLayers() {
        val ranges = parsePreviewRangePlan("0-357;358-719")

        assertEquals(2, ranges.size)
        assertEquals(1, ranges[0].startLayer)
        assertEquals(358, ranges[0].endLayer)
        assertEquals("Range 1", ranges[0].label)
        assertEquals(359, ranges[1].startLayer)
        assertEquals(720, ranges[1].endLayer)
        assertEquals("Range 2", ranges[1].label)
    }

    @Test
    fun parsePreviewRangePlanIgnoresMalformedRanges() {
        val ranges = parsePreviewRangePlan("bad;9-4;4-10")

        assertEquals(1, ranges.size)
        assertEquals(5, ranges[0].startLayer)
        assertEquals(11, ranges[0].endLayer)
    }

    @Test
    fun parsePreviewRangePlanReturnsEmptyForBlankPlans() {
        assertEquals(emptyList<PreviewRangeSuggestion>(), parsePreviewRangePlan(null))
        assertEquals(emptyList<PreviewRangeSuggestion>(), parsePreviewRangePlan(""))
        assertEquals(emptyList<PreviewRangeSuggestion>(), parsePreviewRangePlan("   "))
    }

    @Test
    fun parsePreviewRangePlanClampsLayerNumbersToUiRange() {
        val ranges = parsePreviewRangePlan("2147483646-9223372036854775806")

        assertEquals(1, ranges.size)
        assertEquals(Int.MAX_VALUE, ranges[0].startLayer)
        assertEquals(Int.MAX_VALUE, ranges[0].endLayer)
    }
}
