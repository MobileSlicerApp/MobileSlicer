package com.mobileslicer.viewer

import kotlin.math.max

internal class WorkspaceObjectUploadManager {
    var uploads: List<ModelObjectUpload> = emptyList()
        private set

    var selectedFootprintUpload: TriangleUpload? = null
        private set

    var plateSceneCameraInitialized = false

    fun uploadObjects(
        objects: List<ViewerPlateObject>,
        bed: PrinterBedSpec,
        camera: ViewerCamera
    ) {
        clearObjectUploads()
        if (objects.isEmpty()) {
            clearSelectedFootprint()
            plateSceneCameraInitialized = false
            return
        }

        val nextUploads = mutableListOf<ModelObjectUpload>()
        var maxSize = 40f
        for (plateObject in objects) {
            val mesh = plateObject.mesh
            if (mesh.triangleCount <= 0) continue
            val placement = buildModelPlacement(mesh, plateObject.transform, bed)
            maxSize = max(maxSize, max(max(placement.sizeX, placement.sizeY), placement.sizeZ))
            nextUploads.add(
                ModelObjectUpload(
                    id = plateObject.id,
                    mesh = mesh,
                    upload = uploadTriangleData(vertices = mesh.vertices, normals = mesh.normals),
                    modelMatrix = placement.matrix,
                    centerX = placement.centerX,
                    centerY = placement.centerY,
                    centerZ = placement.centerZ,
                    radius = modelRadius(placement),
                    sizeX = placement.sizeX,
                    sizeY = placement.sizeY,
                    sizeZ = placement.sizeZ,
                    colorInt = plateObject.colorInt,
                    selected = plateObject.selected
                )
            )
        }
        uploads = nextUploads
        uploadSelectedFootprint(nextUploads.firstOrNull { it.selected })

        if (plateSceneCameraInitialized) {
            camera.updatePlateObjectsSceneKeepingView(bed, maxSize)
        } else {
            camera.setPlateObjectsScene(bed, maxSize)
            plateSceneCameraInitialized = true
        }
    }

    fun updateTransforms(objects: List<ViewerPlateObject>, bed: PrinterBedSpec) {
        val objectsById = objects.associateBy { it.id }
        val updatedUploads = uploads.map { upload ->
            val plateObject = objectsById[upload.id] ?: return@map upload
            val placement = buildModelPlacement(plateObject.mesh, plateObject.transform, bed)
            upload.copy(
                mesh = plateObject.mesh,
                modelMatrix = placement.matrix,
                centerX = placement.centerX,
                centerY = placement.centerY,
                centerZ = placement.centerZ,
                radius = modelRadius(placement),
                sizeX = placement.sizeX,
                sizeY = placement.sizeY,
                sizeZ = placement.sizeZ,
                colorInt = plateObject.colorInt,
                selected = plateObject.selected
            )
        }
        uploads = updatedUploads
        uploadSelectedFootprint(updatedUploads.firstOrNull { it.selected })
    }

    fun clear() {
        clearObjectUploads()
        clearSelectedFootprint()
        plateSceneCameraInitialized = false
    }

    private fun clearObjectUploads() {
        uploads.forEach { deleteTriangleUpload(it.upload) }
        uploads = emptyList()
    }

    private fun uploadSelectedFootprint(selected: ModelObjectUpload?) {
        clearSelectedFootprint()
        val geometry = selected?.let(::buildSelectedFootprintGeometry) ?: return
        selectedFootprintUpload = uploadTriangleData(vertices = geometry.vertices, normals = geometry.normals)
    }

    private fun clearSelectedFootprint() {
        selectedFootprintUpload?.let(::deleteTriangleUpload)
        selectedFootprintUpload = null
    }
}
