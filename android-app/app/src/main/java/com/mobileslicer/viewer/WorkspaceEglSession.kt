package com.mobileslicer.viewer

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.view.Surface

internal class WorkspaceEglSession {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var config: EGLConfig? = null
    private var surface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var attachedSurface: Surface? = null

    val hasDisplay: Boolean
        get() = display != EGL14.EGL_NO_DISPLAY

    val hasWindowSurface: Boolean
        get() = surface != EGL14.EGL_NO_SURFACE

    fun ensureReady(targetSurface: Surface, width: Int, height: Int, onSurfaceCreated: () -> Unit) {
        if (display == EGL14.EGL_NO_DISPLAY) {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            require(display != EGL14.EGL_NO_DISPLAY) { "Unable to access EGL display." }
            val version = IntArray(2)
            require(EGL14.eglInitialize(display, version, 0, version, 1)) {
                "Unable to initialize EGL."
            }
            val chosenConfig = chooseConfig(display)
            config = chosenConfig
            context = createContext(display, chosenConfig)
        }

        if (surface == EGL14.EGL_NO_SURFACE || attachedSurface !== targetSurface) {
            destroyWindowSurface()
            val attrs = intArrayOf(EGL14.EGL_NONE)
            surface = EGL14.eglCreateWindowSurface(display, config, targetSurface, attrs, 0)
            require(surface != EGL14.EGL_NO_SURFACE) { "Unable to create EGL window surface." }
            require(EGL14.eglMakeCurrent(display, surface, surface, context)) {
                "Unable to bind EGL context."
            }
            attachedSurface = targetSurface
            onSurfaceCreated()
        } else {
            require(EGL14.eglMakeCurrent(display, surface, surface, context)) {
                "Unable to bind EGL context."
            }
        }

        GLES20.glViewport(0, 0, width, height)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_DITHER)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun swapBuffers() {
        require(EGL14.eglSwapBuffers(display, surface)) { "Unable to present workspace frame." }
    }

    fun destroyWindowSurface() {
        if (display != EGL14.EGL_NO_DISPLAY && surface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(
                display,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(display, surface)
        }
        surface = EGL14.EGL_NO_SURFACE
        attachedSurface = null
    }

    fun destroyContext() {
        destroyWindowSurface()
        if (context != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(display, context)
        }
        if (display != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(display)
        }
        display = EGL14.EGL_NO_DISPLAY
        context = EGL14.EGL_NO_CONTEXT
        config = null
    }
}
