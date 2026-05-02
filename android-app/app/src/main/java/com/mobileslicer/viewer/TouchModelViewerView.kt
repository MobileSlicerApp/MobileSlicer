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
import android.opengl.Matrix
import android.os.SystemClock
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

internal class TouchModelViewerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                renderThread?.zoomBy(detector.scaleFactor)
                return true
            }
        }
    )

    private var renderThread: WorkspaceRenderThread? = null
    private var onViewerFailure: ((ViewerFailure?) -> Unit)? = null
    private var onRenderReady: ((Boolean) -> Unit)? = null
    private var onObjectSelected: ((Long?) -> Unit)? = null
    private var currentMesh: StlMesh? = null
    private var currentPlateObjects: List<ViewerPlateObject> = emptyList()
    private var currentPlateObjectsSignature = ViewerUpdateDecisions.plateObjectsSignature(currentPlateObjects)
    private var currentGcodePreviewEngineHandle = 0L
    private var currentGcodePreviewKey = 0L
    private var currentGcodePreviewVertexBudget = GcodePreviewPerformanceMode.HARD_VERTEX_CEILING
    private var currentGcodeLayerMin = 0L
    private var currentGcodeLayerMax = Long.MAX_VALUE
    private var currentGcodeLayerReloadToken = 0L
    private var currentGcodePathVisibility: Map<Pair<Int, Int>, Boolean> = emptyMap()
    private var currentGcodeDisplayMode = GcodePreviewDisplayMode.Auto
    private var pendingCameraState: ViewerCameraState? = null
    private var currentBed = PrinterBedSpec(widthMm = 220f, depthMm = 220f, maxHeightMm = 220f)
    private var currentModelTransform: ViewerModelTransform? = null
    private var currentAppearance = ViewerAppearance(
        darkTheme = true,
        accentColor = Color.rgb(143, 193, 255)
    )
    private var downTouchX = 0f
    private var downTouchY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastCentroidX = 0f
    private var lastCentroidY = 0f
    private var dragging = false
    private var suppressTapSelection = false
    private var resumed = true
    private var lastPickX = Float.NaN
    private var lastPickY = Float.NaN
    private var lastPickAtMs = 0L
    private var lastPickCandidates: List<Long> = emptyList()
    private var lastPickIndex = -1

    init {
        holder.addCallback(this)
        isFocusable = true
        isClickable = true
    }

    internal fun setMesh(mesh: StlMesh?) {
        if (currentMesh === mesh) return
        currentMesh = mesh
        onRenderReady?.invoke(false)
        renderThread?.setMesh(mesh)
    }

    internal fun setGcodePreviewSource(
        engineHandle: Long,
        previewKey: Long,
        vertexBudget: Long = currentGcodePreviewVertexBudget
    ) {
        val source = ViewerUpdateDecisions.normalizeGcodePreviewSource(engineHandle, previewKey)
        val safeEngineHandle = source.engineHandle
        val safePreviewKey = source.previewKey
        val safeVertexBudget = vertexBudget.coerceIn(1L, GcodePreviewPerformanceMode.HARD_VERTEX_CEILING)
        if (
            currentGcodePreviewEngineHandle == safeEngineHandle &&
            currentGcodePreviewKey == safePreviewKey &&
            currentGcodePreviewVertexBudget == safeVertexBudget
        ) return
        currentGcodePreviewEngineHandle = safeEngineHandle
        currentGcodePreviewKey = safePreviewKey
        currentGcodePreviewVertexBudget = safeVertexBudget
        onRenderReady?.invoke(false)
        renderThread?.setGcodePreviewSource(safeEngineHandle, safePreviewKey, safeVertexBudget)
    }

    internal fun setGcodePreviewSourceAndLayerRange(
        engineHandle: Long,
        previewKey: Long,
        vertexBudget: Long,
        minLayer: Long,
        maxLayer: Long,
        reloadToken: Long
    ) {
        val source = ViewerUpdateDecisions.normalizeGcodePreviewSource(engineHandle, previewKey)
        val safeEngineHandle = source.engineHandle
        val safePreviewKey = source.previewKey
        val safeVertexBudget = vertexBudget.coerceIn(1L, GcodePreviewPerformanceMode.HARD_VERTEX_CEILING)
        val changed =
            currentGcodePreviewEngineHandle != safeEngineHandle ||
                currentGcodePreviewKey != safePreviewKey ||
                currentGcodePreviewVertexBudget != safeVertexBudget ||
                currentGcodeLayerMin != minLayer ||
                currentGcodeLayerMax != maxLayer ||
                currentGcodeLayerReloadToken != reloadToken
        if (!changed) return
        val sourceChanged =
            currentGcodePreviewEngineHandle != safeEngineHandle ||
                currentGcodePreviewKey != safePreviewKey ||
                currentGcodePreviewVertexBudget != safeVertexBudget
        val reloadChanged = currentGcodeLayerReloadToken != reloadToken
        currentGcodePreviewEngineHandle = safeEngineHandle
        currentGcodePreviewKey = safePreviewKey
        currentGcodePreviewVertexBudget = safeVertexBudget
        currentGcodeLayerMin = minLayer
        currentGcodeLayerMax = maxLayer
        currentGcodeLayerReloadToken = reloadToken
        if (sourceChanged || reloadChanged) {
            onRenderReady?.invoke(false)
        }
        renderThread?.setGcodePreviewSourceAndLayerRange(
            safeEngineHandle,
            safePreviewKey,
            safeVertexBudget,
            minLayer,
            maxLayer,
            reloadToken
        )
    }

    internal fun setGcodeLayerRange(minLayer: Long, maxLayer: Long, reloadToken: Long = currentGcodeLayerReloadToken) {
        if (
            currentGcodeLayerMin == minLayer &&
            currentGcodeLayerMax == maxLayer &&
            currentGcodeLayerReloadToken == reloadToken
        ) return
        currentGcodeLayerMin = minLayer
        currentGcodeLayerMax = maxLayer
        currentGcodeLayerReloadToken = reloadToken
        renderThread?.setGcodeLayerRange(minLayer, maxLayer, reloadToken)
    }

    internal fun setGcodePathVisibility(kind: Int, id: Int, visible: Boolean) {
        currentGcodePathVisibility = currentGcodePathVisibility.toMutableMap().apply {
            put(kind to id, visible)
        }
        renderThread?.setGcodePathVisibility(kind, id, visible)
    }

    internal fun setGcodeDisplayMode(mode: GcodePreviewDisplayMode) {
        if (currentGcodeDisplayMode == mode) return
        currentGcodeDisplayMode = mode
        renderThread?.setGcodeDisplayMode(mode)
    }

    internal fun captureCameraState(): ViewerCameraState? =
        renderThread?.cameraState() ?: pendingCameraState

    internal fun restoreCameraState(state: ViewerCameraState) {
        pendingCameraState = state
        renderThread?.restoreCameraState(state)
    }

    internal fun setPrinterBed(bed: PrinterBedSpec) {
        if (currentBed == bed) return
        currentBed = bed
        renderThread?.setPrinterBed(bed)
    }

    internal fun setModelTransform(transform: ViewerModelTransform?) {
        if (currentModelTransform == transform) return
        currentModelTransform = transform
        renderThread?.setModelTransform(transform)
    }

    internal fun setPlateObjects(objects: List<ViewerPlateObject>) {
        val nextSignature = ViewerUpdateDecisions.plateObjectsSignature(objects)
        if (currentPlateObjectsSignature == nextSignature) return
        val uploadSetChanged = !ViewerUpdateDecisions.samePlateObjectUploadSet(currentPlateObjects, objects)
        currentPlateObjects = objects.toList()
        currentPlateObjectsSignature = nextSignature
        if (uploadSetChanged) {
            onRenderReady?.invoke(false)
        }
        renderThread?.setPlateObjects(currentPlateObjects)
    }

    internal fun setFailureListener(listener: (ViewerFailure?) -> Unit) {
        onViewerFailure = listener
        renderThread?.currentFailure()?.let(listener)
    }

    internal fun setRenderReadyListener(listener: (Boolean) -> Unit) {
        onRenderReady = listener
    }

    internal fun setObjectSelectionListener(listener: (Long?) -> Unit) {
        onObjectSelected = listener
    }

    internal fun setViewerAppearance(darkTheme: Boolean, accentColor: Int, worldColor: Int? = null) {
        if (
            currentAppearance.darkTheme == darkTheme &&
            currentAppearance.accentColor == accentColor &&
            currentAppearance.worldColor == worldColor
        ) return
        currentAppearance = ViewerAppearance(darkTheme = darkTheme, accentColor = accentColor, worldColor = worldColor)
        renderThread?.setViewerAppearance(darkTheme = darkTheme, accentColor = accentColor, worldColor = worldColor)
    }

    internal fun captureCurrentFrame(onResult: (Bitmap?) -> Unit) {
        val surface = holder.surface
        if (!surface.isValid || width <= 0 || height <= 0) {
            onResult(null)
            return
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(this, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                onResult(bitmap)
            } else {
                bitmap.recycle()
                onResult(null)
            }
        }, Handler(Looper.getMainLooper()))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureRenderThread()
    }

    override fun onDetachedFromWindow() {
        teardownRenderThread()
        super.onDetachedFromWindow()
    }

    internal fun onResume() {
        resumed = true
        ensureRenderThread()
        renderThread?.setPaused(false)
    }

    internal fun onPause() {
        resumed = false
        pendingCameraState = renderThread?.cameraState() ?: pendingCameraState
        teardownRenderThread()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        ensureRenderThread()
        renderThread?.bindSurface(holder.surface, width.coerceAtLeast(1), height.coerceAtLeast(1))
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        ensureRenderThread()
        renderThread?.bindSurface(holder.surface, width.coerceAtLeast(1), height.coerceAtLeast(1))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderThread?.unbindSurface()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downTouchX = event.x
                downTouchY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                dragging = false
                suppressTapSelection = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    lastCentroidX = centroidX(event)
                    lastCentroidY = centroidY(event)
                    dragging = true
                    suppressTapSelection = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val centroidX = centroidX(event)
                    val centroidY = centroidY(event)
                    renderThread?.panBy(centroidX - lastCentroidX, centroidY - lastCentroidY)
                    lastCentroidX = centroidX
                    lastCentroidY = centroidY
                } else if (!scaleDetector.isInProgress) {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    val totalDragX = event.x - downTouchX
                    val totalDragY = event.y - downTouchY
                    if (dragging || abs(totalDragX) > touchSlop || abs(totalDragY) > touchSlop) {
                        dragging = true
                        renderThread?.orbitBy(deltaX, deltaY)
                    }
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount - 1 < 2) {
                    val survivorIndex = if (event.actionIndex == 0) 1 else 0
                    if (survivorIndex in 0 until event.pointerCount) {
                        downTouchX = event.getX(survivorIndex)
                        downTouchY = event.getY(survivorIndex)
                        lastTouchX = event.getX(survivorIndex)
                        lastTouchY = event.getY(survivorIndex)
                        dragging = false
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                if (event.actionMasked == MotionEvent.ACTION_UP && !dragging && !suppressTapSelection) {
                    performClick()
                    val tapX = event.x
                    val tapY = event.y
                    renderThread?.pickObjectHits(tapX, tapY) { hits ->
                        val candidates = hits.map { it.objectId }.distinct()
                        val objectId = chooseObjectFromCandidates(tapX, tapY, candidates)
                        post {
                            onObjectSelected?.invoke(objectId)
                        }
                    }
                }
                dragging = false
                suppressTapSelection = false
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun ensureRenderThread() {
        if (!isAttachedToWindow) return
        if (renderThread != null) return
        val thread = WorkspaceRenderThread(
            context = context.applicationContext,
            onFailure = { failure -> post { onViewerFailure?.invoke(failure) } },
            onRenderReady = { ready -> post { onRenderReady?.invoke(ready) } }
        )
        renderThread = thread
        thread.start()
        thread.setPaused(!resumed)
        thread.setPrinterBed(currentBed)
        thread.setViewerAppearance(
            darkTheme = currentAppearance.darkTheme,
            accentColor = currentAppearance.accentColor,
            worldColor = currentAppearance.worldColor
        )
        thread.setMesh(currentMesh)
        thread.setPlateObjects(currentPlateObjects)
        thread.setModelTransform(currentModelTransform)
        thread.setGcodePreviewSourceAndLayerRange(
            currentGcodePreviewEngineHandle,
            currentGcodePreviewKey,
            currentGcodePreviewVertexBudget,
            currentGcodeLayerMin,
            currentGcodeLayerMax,
            currentGcodeLayerReloadToken
        )
        currentGcodePathVisibility.forEach { (key, visible) ->
            thread.setGcodePathVisibility(kind = key.first, id = key.second, visible = visible)
        }
        thread.setGcodeDisplayMode(currentGcodeDisplayMode)
        pendingCameraState?.let(thread::restoreCameraState)
        if (holder.surface?.isValid == true) {
            thread.bindSurface(holder.surface, width.coerceAtLeast(1), height.coerceAtLeast(1))
        }
    }

    private fun teardownRenderThread() {
        renderThread?.requestExitAndWait()
        renderThread = null
    }

    private fun centroidX(event: MotionEvent): Float {
        var total = 0f
        repeat(event.pointerCount) { total += event.getX(it) }
        return total / event.pointerCount.toFloat()
    }

    private fun centroidY(event: MotionEvent): Float {
        var total = 0f
        repeat(event.pointerCount) { total += event.getY(it) }
        return total / event.pointerCount.toFloat()
    }

    private fun chooseObjectFromCandidates(screenX: Float, screenY: Float, candidates: List<Long>): Long? {
        if (candidates.isEmpty()) {
            lastPickCandidates = emptyList()
            lastPickIndex = -1
            return null
        }
        val now = SystemClock.uptimeMillis()
        val sameTapArea =
            abs(screenX - lastPickX) <= CYCLE_TAP_RADIUS_PX &&
                abs(screenY - lastPickY) <= CYCLE_TAP_RADIUS_PX
        val sameCandidates = candidates == lastPickCandidates
        val shouldCycle =
            candidates.size > 1 &&
                sameTapArea &&
                sameCandidates &&
                now - lastPickAtMs <= CYCLE_TAP_TIMEOUT_MS
        val nextIndex = if (shouldCycle) {
            (lastPickIndex + 1).floorMod(candidates.size)
        } else {
            0
        }
        lastPickX = screenX
        lastPickY = screenY
        lastPickAtMs = now
        lastPickCandidates = candidates
        lastPickIndex = nextIndex
        return candidates[nextIndex]
    }

    private fun Int.floorMod(modulus: Int): Int =
        ((this % modulus) + modulus) % modulus

    private companion object {
        private const val CYCLE_TAP_RADIUS_PX = 34f
        private const val CYCLE_TAP_TIMEOUT_MS = 1_000L
    }
}
