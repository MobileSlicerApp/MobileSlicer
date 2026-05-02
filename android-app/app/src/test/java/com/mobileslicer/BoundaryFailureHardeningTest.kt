package com.mobileslicer

import com.mobileslicer.profiles.ProfileStoreRepository
import com.mobileslicer.viewer.MeshBounds
import com.mobileslicer.viewer.ViewerModelTransform
import com.mobileslicer.workspace.AppScreen
import com.mobileslicer.workspace.ImportedModelFormat
import com.mobileslicer.workspace.ModelLoadResult
import com.mobileslicer.workspace.PlateObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BoundaryFailureHardeningTest {
    @Test
    fun invalidNativeSliceConfigJsonReturnsOriginalConfigInsteadOfCrashing() {
        val rawConfig = "{not-json"
        val result = applyPlateFilamentSlotsToNativeConfigResult(
            configJson = rawConfig,
            slots = listOf(ProfileStoreRepository.defaultFilamentProfiles().first().toPlateFilamentSlot(index = 1)),
            plateObjects = listOf(plateObject()),
            filaments = ProfileStoreRepository.defaultFilamentProfiles(),
            flushVolumes = null
        )

        assertEquals(rawConfig, result.json)
        assertFalse(result.cacheHit)
    }

    @Test
    fun failedModelLoadDoesNotOpenWorkspaceOrAdvanceObjectId() {
        val application = planModelImportApplication(
            result = ModelLoadResult(
                message = "Failed to load model\nbad.stl",
                loaded = false,
                stagedFilePath = null,
                format = ImportedModelFormat.Stl
            ),
            currentScreen = AppScreen.Home,
            existingPlateObjects = listOf(plateObject()),
            appendRequested = false,
            nextPlateObjectId = 9L,
            defaultTransform = { ViewerModelTransform(centerXmm = 0f, centerYmm = 0f) }
        )

        assertNull(application.importedPlateObject)
        assertEquals(9L, application.nextPlateObjectId)
        assertFalse(application.shouldOpenWorkspace)
    }

    @Test
    fun workspacePreparationSkipsNonStlModels() {
        assertNull(
            resolveWorkspacePreparationRequest(
                currentScreen = AppScreen.Workspace,
                modelLoaded = true,
                currentModelFilePath = "/tmp/model.3mf",
                currentModelFormatName = ImportedModelFormat.ThreeMf.name,
                currentImportTiming = null,
                selectedObject = null,
                currentPreparedMeshPresent = false,
                currentViewerPreparationError = null,
                inProgressTargetKey = null
            )
        )
    }

    private fun plateObject(): PlateObject =
        PlateObject(
            id = 1L,
            label = "model",
            filePath = "/tmp/model.stl",
            format = ImportedModelFormat.Stl,
            importTiming = null,
            bounds = MeshBounds(0f, 0f, 0f, 10f, 10f, 10f),
            transform = ViewerModelTransform(centerXmm = 5f, centerYmm = 5f)
        )
}
