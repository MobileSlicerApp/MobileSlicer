package com.mobileslicer

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelLoaderWorkspacePreparationTest {
    @Test
    fun firstVisibleWorkspaceFrameStatusAppendsFormattedTiming() {
        assertEquals(
            "Loaded cube.stl\nFirst visible workspace frame: 1234 ms",
            firstVisibleWorkspaceFrameStatus("Loaded cube.stl", firstFrameMs = 1234L)
        )
    }
}
