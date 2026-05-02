package com.mobileslicer

import com.mobileslicer.workspace.ModelImportTiming
import com.mobileslicer.workspace.PlateObject
import com.mobileslicer.workspace.WorkspacePreparationResult
import com.mobileslicer.workspace.formatDurationMs
import com.mobileslicer.workspace.modelLoadStatusMessage
import java.io.File

internal fun workspacePreparationTargetKey(
    selectedObject: PlateObject?,
    currentModelFilePath: String?
): String? =
    selectedObject?.let { "${it.id}:${it.filePath}" } ?: currentModelFilePath

internal fun shouldPrepareWorkspaceMesh(
    selectedObject: PlateObject?,
    currentPreparedMeshPresent: Boolean,
    currentViewerPreparationError: String?,
    inProgressTargetKey: String?,
    targetKey: String
): Boolean {
    if (selectedObject != null) {
        if (selectedObject.mesh != null || !selectedObject.viewerPreparationError.isNullOrBlank()) return false
    } else if (currentPreparedMeshPresent || !currentViewerPreparationError.isNullOrBlank()) {
        return false
    }
    return inProgressTargetKey != targetKey
}

internal fun workspaceMeshPreparingStatus(
    modelFilePath: String,
    importTiming: ModelImportTiming?
): String =
    buildString {
        append(
            modelLoadStatusMessage(
                loaded = true,
                fileName = File(modelFilePath).name,
                timing = importTiming
            )
        )
        append('\n')
        append("Preparing workspace mesh in the background...")
    }

internal fun workspaceMeshPreparedStatus(
    modelFilePath: String,
    importTiming: ModelImportTiming?,
    result: WorkspacePreparationResult
): String =
    buildString {
        append(
            modelLoadStatusMessage(
                loaded = true,
                fileName = File(modelFilePath).name,
                timing = importTiming
            )
        )
        result.timing?.let {
            append('\n')
            append(if (it.cacheHit) "Viewer mesh prep cache hit: " else "Viewer mesh prep: ")
            append(formatDurationMs(it.viewerMeshPrepMs))
            if (it.reducedForDisplay && it.sourceTriangleCount != null && it.displayTriangleCount != null) {
                append(" • display LOD ")
                append(it.displayTriangleCount)
                append("/")
                append(it.sourceTriangleCount)
                append(" triangles")
            }
        }
        if (!result.viewerPreparationError.isNullOrBlank()) {
            append('\n')
            append("Workspace mesh preparation failed: ")
            append(result.viewerPreparationError)
        } else {
            append('\n')
            append("Workspace mesh prepared. Waiting for first visible frame.")
        }
    }
