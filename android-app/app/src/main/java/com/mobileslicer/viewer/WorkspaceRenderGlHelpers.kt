package com.mobileslicer.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewConfiguration
import com.mobileslicer.nativebridge.NativeEngineCallResult
import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.max

internal fun uploadTriangleData(vertices: FloatArray, normals: FloatArray): TriangleUpload {
    require(vertices.size == normals.size) { "Vertex and normal buffers must have matching sizes." }
    require(vertices.size % 3 == 0) { "Triangle vertex buffer is malformed." }
    val vertexBuffer = IntArray(1)
    val normalBuffer = IntArray(1)
    GLES20.glGenBuffers(1, vertexBuffer, 0)
    GLES20.glGenBuffers(1, normalBuffer, 0)

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer[0])
    GLES20.glBufferData(
        GLES20.GL_ARRAY_BUFFER,
        vertices.size * Float.SIZE_BYTES,
        floatBufferOf(vertices),
        GLES20.GL_STATIC_DRAW
    )

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalBuffer[0])
    GLES20.glBufferData(
        GLES20.GL_ARRAY_BUFFER,
        normals.size * Float.SIZE_BYTES,
        floatBufferOf(normals),
        GLES20.GL_STATIC_DRAW
    )

    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    return TriangleUpload(
        vertexBufferId = vertexBuffer[0],
        normalBufferId = normalBuffer[0],
        vertexCount = vertices.size / 3
    )
}

internal fun deleteTriangleUpload(upload: TriangleUpload) {
    GLES20.glDeleteBuffers(1, intArrayOf(upload.vertexBufferId), 0)
    GLES20.glDeleteBuffers(1, intArrayOf(upload.normalBufferId), 0)
}

internal fun uploadTriangleDataIfNotEmpty(geometry: TriangleGeometry): TriangleUpload? =
    if (geometry.vertices.isEmpty()) {
        null
    } else {
        uploadTriangleData(vertices = geometry.vertices, normals = geometry.normals)
    }

internal fun uploadTextureQuad(vertices: FloatArray, uvs: FloatArray, bitmap: Bitmap): TextureUpload {
    require(vertices.size % 3 == 0) { "Texture vertex buffer is malformed." }
    require(vertices.size / 3 * 2 == uvs.size) { "Texture UV buffer is malformed." }
    val vertexBuffer = IntArray(1)
    val uvBuffer = IntArray(1)
    val texture = IntArray(1)

    GLES20.glGenBuffers(1, vertexBuffer, 0)
    GLES20.glGenBuffers(1, uvBuffer, 0)
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBuffer[0])
    GLES20.glBufferData(
        GLES20.GL_ARRAY_BUFFER,
        vertices.size * Float.SIZE_BYTES,
        floatBufferOf(vertices),
        GLES20.GL_STATIC_DRAW
    )
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, uvBuffer[0])
    GLES20.glBufferData(
        GLES20.GL_ARRAY_BUFFER,
        uvs.size * Float.SIZE_BYTES,
        floatBufferOf(uvs),
        GLES20.GL_STATIC_DRAW
    )

    GLES20.glGenTextures(1, texture, 0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

    return TextureUpload(
        vertexBufferId = vertexBuffer[0],
        uvBufferId = uvBuffer[0],
        textureId = texture[0],
        vertexCount = vertices.size / 3
    )
}

internal fun deleteTextureUpload(upload: TextureUpload) {
    GLES20.glDeleteBuffers(1, intArrayOf(upload.vertexBufferId), 0)
    GLES20.glDeleteBuffers(1, intArrayOf(upload.uvBufferId), 0)
    GLES20.glDeleteTextures(1, intArrayOf(upload.textureId), 0)
}

internal fun chooseConfig(display: EGLDisplay): EGLConfig {
    val eglOpenGlEs3BitKhr = 0x00000040
    val attributes = intArrayOf(
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_ALPHA_SIZE, 8,
        EGL14.EGL_DEPTH_SIZE, 16,
        EGL14.EGL_RENDERABLE_TYPE, eglOpenGlEs3BitKhr,
        EGL14.EGL_NONE
    )
    val configs = arrayOfNulls<EGLConfig>(1)
    val count = IntArray(1)
    require(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)) {
        "Unable to choose EGL config."
    }
    require(count[0] > 0 && configs[0] != null) {
        "No compatible EGL config found."
    }
    return requireNotNull(configs[0]) { "No compatible EGL config found." }
}

internal fun createContext(display: EGLDisplay, config: EGLConfig): EGLContext {
    val attrs = intArrayOf(
        EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL14.EGL_NONE
    )
    return EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attrs, 0).also { context ->
        require(context != EGL14.EGL_NO_CONTEXT) { "Unable to create EGL context." }
    }
}

internal fun floatBufferOf(values: FloatArray): FloatBuffer {
    return ByteBuffer.allocateDirect(values.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(values)
            position(0)
        }
}
