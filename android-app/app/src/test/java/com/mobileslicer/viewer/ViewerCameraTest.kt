package com.mobileslicer.viewer

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerCameraTest {
    @Test
    fun zoomScalesEntireCameraDistanceWithoutFixedCloseRangeFloor() {
        val camera = ViewerCamera(PrinterBedSpec(widthMm = 270f, depthMm = 270f, maxHeightMm = 256f))
        camera.setModelScene(
            bed = PrinterBedSpec(widthMm = 270f, depthMm = 270f, maxHeightMm = 256f),
            focusX = 0f,
            focusY = 0f,
            sizeX = 25f,
            sizeY = 25f,
            sizeZ = 25f
        )
        val initialDistance = camera.cameraDistanceForTesting()

        camera.zoomBy(2f)

        assertEquals(initialDistance / 2f, camera.cameraDistanceForTesting(), 0.001f)
    }

    @Test
    fun smallPinchScaleChangesAreNotDropped() {
        val camera = ViewerCamera(PrinterBedSpec(widthMm = 270f, depthMm = 270f, maxHeightMm = 256f))
        val initialDistance = camera.cameraDistanceForTesting()

        camera.zoomBy(1.005f)

        assertEquals(initialDistance / 1.005f, camera.cameraDistanceForTesting(), 0.001f)
    }

    @Test
    fun resetViewRestoresDefaultZoomAndClearsPan() {
        val camera = ViewerCamera(PrinterBedSpec(widthMm = 270f, depthMm = 270f, maxHeightMm = 256f))
        camera.zoomBy(2f)
        camera.panBy(20f, 30f, 1000)

        camera.resetView()
        val state = camera.snapshotState()

        assertEquals(0.55f, state.zoom, 0.0001f)
        assertEquals(0f, state.panX, 0.0001f)
        assertEquals(0f, state.panY, 0.0001f)
    }

    @Test
    fun zoomAllowsWiderRangeForInspection() {
        val camera = ViewerCamera(PrinterBedSpec(widthMm = 270f, depthMm = 270f, maxHeightMm = 256f))
        camera.zoomBy(100f)

        assertEquals(24f, camera.snapshotState().zoom, 0.0001f)

        camera.zoomBy(0.001f)

        assertEquals(0.28f, camera.snapshotState().zoom, 0.0001f)
    }
}
