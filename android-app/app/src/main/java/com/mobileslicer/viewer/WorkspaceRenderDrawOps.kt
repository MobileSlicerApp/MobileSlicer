package com.mobileslicer.viewer

import android.opengl.GLES20

internal val ORCA_GRID_THIN_COLOR = floatArrayOf(0.431f, 0.431f, 0.463f, 0.58f)
internal val ORCA_GRID_BOLD_COLOR = floatArrayOf(0.431f, 0.431f, 0.463f, 0.78f)
internal val ORCA_BED_UNDERSIDE_COLOR = floatArrayOf(0.045f, 0.052f, 0.060f, 0.22f)

internal fun viewerObjectColor(colorInt: Int): FloatArray =
    floatArrayOf(
        android.graphics.Color.red(colorInt) / 255f,
        android.graphics.Color.green(colorInt) / 255f,
        android.graphics.Color.blue(colorInt) / 255f,
        1f
    )

internal fun drawWorkspaceTexturedBed(
    textureProgram: Int,
    textureHandles: TextureProgramHandles?,
    upload: TextureUpload,
    viewProjectionMatrix: FloatArray,
    alpha: Float
) {
    val handles = textureHandles ?: return
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_BACK)
    try {
        drawTextureUpload(
            programId = textureProgram,
            handles = handles,
            upload = upload,
            viewProjectionMatrix = viewProjectionMatrix,
            alpha = alpha
        )
    } finally {
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }
}

internal fun drawWorkspaceTransparentPlateSurface(
    upload: TriangleUpload,
    drawTriangles: (TriangleUpload, FloatArray, Boolean, Boolean, FloatArray?) -> Unit,
    viewerColors: ViewerColors
) {
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glDepthMask(false)
    try {
        GLES20.glCullFace(GLES20.GL_FRONT)
        drawTriangles(upload, viewerColors.plateBackFaceColor(), false, false, null)
        GLES20.glCullFace(GLES20.GL_BACK)
        drawTriangles(upload, viewerColors.plateFrontFaceColor(), false, false, null)
    } finally {
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }
}

internal fun drawWorkspaceTransparentBedUnderside(
    upload: TriangleUpload,
    drawTriangles: (TriangleUpload, FloatArray, Boolean, Boolean, FloatArray?) -> Unit
) {
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_BACK)
    GLES20.glDepthMask(false)
    try {
        drawTriangles(upload, ORCA_BED_UNDERSIDE_COLOR, false, false, null)
    } finally {
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }
}

internal fun drawWorkspaceCulledTriangles(
    upload: TriangleUpload,
    color: FloatArray,
    stabilizeSurface: Boolean = false,
    drawTriangles: (TriangleUpload, FloatArray, Boolean, Boolean, FloatArray?) -> Unit
) {
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_BACK)
    try {
        drawTriangles(upload, color, false, stabilizeSurface, null)
    } finally {
        GLES20.glDisable(GLES20.GL_CULL_FACE)
    }
}

internal fun drawWorkspaceBottomOnlyGrid(
    upload: TriangleUpload,
    color: FloatArray,
    drawTriangles: (TriangleUpload, FloatArray, Boolean, Boolean, FloatArray?) -> Unit
) {
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_FRONT)
    try {
        drawTriangles(upload, color, false, true, null)
    } finally {
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
    }
}
