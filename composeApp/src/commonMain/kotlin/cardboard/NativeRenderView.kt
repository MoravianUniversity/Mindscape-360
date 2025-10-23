package cardboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface RenderViewListener {
    /** Called when the render view is first initialized and ready to use. Runs on graphics thread. */
    fun onInit() { }
    /** Called when the render view is resized (always before drawing and after init). Runs on graphics thread. */
    fun onResize(width: Int, height: Int) { }
    /** Called when the render view is about to draw a frame. Runs on graphics thread. */
    fun onDraw() { }
    /** Called when the render view is destroyed or removed from the window. Runs on an arbitrary thread. */
    fun onDestroy() { }
    /** Called when the render view will be paused (app wide). Runs on main thread. */
    fun onPause() { }
    /** Called when the render view is resumed (app wide). Runs on main thread. */
    fun onResume() { }
    /** Called when the user clicks the "trigger" button. Runs on main thread. */
    fun onTrigger() { }
}

@Composable
expect fun rememberNativeRenderView(): NativeRenderView

expect abstract class NativeContext

private enum class ViewState { NONE, INIT, RESIZE, RESUME, PAUSE, DRAW, DESTROY }

abstract class NativeRenderView {
    abstract val context: NativeContext?

    @Composable
    abstract fun RenderView(modifier: Modifier = Modifier)

    val listeners: Set<RenderViewListener> get() = _listeners
    private val _listeners = mutableSetOf<RenderViewListener>()
    fun addListener(listener: RenderViewListener) { _listeners.add(listener) }
    fun removeListener(listener: RenderViewListener) { _listeners.remove(listener) }

    // Fullscreen and similar utilities
    abstract fun setFullscreen()
    abstract fun resetFullscreen()
    abstract fun setScreenBrightnessFull()
    abstract fun resetBrightness()
    abstract fun forceLandscape()
    abstract fun resetOrientation()
    fun immersive() {
        setFullscreen()
        setScreenBrightnessFull()
        forceLandscape()
    }
    fun resetImmersive() {
        resetFullscreen()
        resetBrightness()
        resetOrientation()
    }

    // Event lifecycle management - make sure we only call the right methods at the right time
    private var lastState = ViewState.NONE
    protected var lastWidth = -1
    protected var lastHeight = -1
    private var lastSetWidth = -1
    private var lastSetHeight = -1
    private var rendering = false
    protected fun init() {
        if (lastState != ViewState.NONE && lastState != ViewState.DESTROY && rendering) { pause() }
        lastState = ViewState.INIT
        onInit()
    }
    protected open fun onInit() { listeners.forEach { it.onInit() } }
    protected fun resize(width: Int, height: Int) {
        if (lastState == ViewState.NONE) { return }
        lastHeight = height
        lastWidth = width
        if (lastState == ViewState.INIT || lastState == ViewState.PAUSE) {
            // defer these until resume() is called
        } else if (width != lastSetWidth || height != lastSetHeight) {
            //assert(lastEvent != ViewState.DESTROY)
            resizeForce()
        }
    }
    private fun resizeForce() {
        lastSetWidth = lastWidth
        lastSetHeight = lastHeight
        lastState = ViewState.RESIZE
        onResize(lastSetWidth, lastSetHeight)
    }
    protected open fun onResize(width: Int, height: Int) { listeners.forEach { it.onResize(width, height) } }
    protected fun draw() {
        if (lastState == ViewState.INIT) { resume() }
        if (!rendering || lastState == ViewState.PAUSE) { return } // ignore requests to render before we are ready
        lastState = ViewState.DRAW
        onDraw()
    }
    protected open fun onDraw() { listeners.forEach { it.onDraw() } }
    protected fun destroy() {
        if (lastState == ViewState.DESTROY) { return }
        if (rendering) { pause() }
        lastState = ViewState.DESTROY
        onDestroy()
    }
    protected open fun onDestroy() { listeners.forEach { it.onDestroy() } }

    open val isPaused: Boolean get() = !rendering

    fun resume() {
        if (lastState == ViewState.NONE || rendering) { return }
        if (lastState == ViewState.INIT || (lastState == ViewState.PAUSE && (lastWidth != lastSetWidth || lastHeight != lastSetHeight))) {
            resizeForce()
        }
        rendering = true
        lastState = ViewState.RESUME
        onResume()
    }
    protected open fun onResume() { listeners.forEach { it.onResume() } }
    fun pause() {
        if (!rendering) { return }
        rendering = false
        lastState = ViewState.PAUSE
        onPause()
    }
    protected open fun onPause() { listeners.forEach { it.onPause() } }

    fun trigger() { onTrigger() }
    protected open fun onTrigger() { listeners.forEach { it.onTrigger() } }
}
