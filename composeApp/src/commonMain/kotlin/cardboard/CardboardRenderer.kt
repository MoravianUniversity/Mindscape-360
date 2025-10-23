package cardboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import cardboard.sdk.DUMMY_PARAMS_URI
import cardboard.sdk.DistortionRenderer
import cardboard.sdk.Eye
import cardboard.sdk.EyeTextureDescription
import cardboard.sdk.HeadTracker
import cardboard.sdk.LensDistortion
import cardboard.sdk.QrCode
import cardboard.sdk.ViewportOrientation
import cardboard.sdk.getBootTimeNano
import cardboard.util.Matrix
import cardboard.util.Quat
import kotlin.jvm.JvmField

data class CardboardRendererParams(
    @JvmField val headTracker: HeadTrackerParams = HeadTrackerParams(),
)

data class HeadTrackerParams(
    @JvmField val velocityFilterCutoffFreq: Int = 6,  // 6 Hz cutoff frequency for the velocity filter of the head tracker
    @JvmField val predictionTimeWithoutVsyncNanos: Long = 50000000,  // 50 ms prediction time without vsync
)

open class CardboardRenderer(
    val view: NativeRenderView,
    private val shaders: ShaderCollection,
    val params: CardboardRendererParams = CardboardRendererParams(),
    protected val native: NativeRenderer = createDefaultNativeRenderer(),
    protected val zNear: Float = 0.1f,  // near clip plane
    protected val zFar: Float = 100f,   // far clip plane
): RenderViewListener {
    private var headTracker: HeadTracker? = null
    private var lensDistortion: LensDistortion? = null
    private var distortionRenderer: DistortionRenderer? = null

    private var paramsChanged = false
    var width = -1; private set
    var height = -1; private set
    private var deviceParamId = -1

    protected val headView = Matrix()
    protected val projMatrices = Array(2) { Matrix() }
    protected val eyeMatrices = Array(2) { Matrix() }
    protected val textureDesc = Array(2) { EyeTextureDescription() }

    // temporary variables to reduce allocations
    private val orientation = FloatArray(4)
    private val position = FloatArray(3)
    private val quat = Quat()
    private val mat = Matrix()

    @Composable
    fun View(modifier: Modifier = Modifier) {
        view.RenderView(modifier)
        DisposableEffect(null) {
            view.addListener(this@CardboardRenderer)
            view.immersive()
            onDispose {
                view.resetImmersive()
                view.removeListener(this@CardboardRenderer)
            }
        }
    }

    override fun onInit() {
        native.init(this, shaders.findMatch(native.engine))
        if (headTracker == null) {
            headTracker = HeadTracker().apply { setLowPassFilter(params.headTracker.velocityFilterCutoffFreq) }
        }
    }
    override fun onDestroy() {
        native.destroy(this)
        distortionRenderer?.destroy()
        distortionRenderer = null
        lensDistortion?.destroy()
        lensDistortion = null
        headTracker?.destroy()
        headTracker = null
    }
    override fun onResize(width: Int, height: Int) {
        this.width = width
        this.height = height
        paramsChanged = true
    }
    override fun onPause() { headTracker?.pause() }
    override fun onResume() {
        headTracker?.resume()
        paramsChanged = true

        // Check for device parameters existence in external storage. If they're
        // missing, we must scan a Cardboard QR code and save the obtained parameters.
        if (!QrCode.hasSavedDeviceParams) {
            QrCode.scanQrCodeAndSaveDeviceParams()
            //QrCode.saveDeviceParams(DUMMY_PARAMS_URI)
        }
    }
    override fun onDraw() {
        if (!updateDeviceParams()) { return }

        // Update head pose
        getPose(headView)

        // Start drawing
        native.prepareToDraw()
        onDrawStart()

        // Draw eyes views
        native.prepareToDraw(Eye.LEFT)
        onDraw(Eye.LEFT)
        native.prepareToDraw(Eye.RIGHT)
        onDraw(Eye.RIGHT)

        // Render
        val target = native.prepareToDrawFinal()
        distortionRenderer?.renderEyeToDisplay(
            target, 0, 0, width, height,
            textureDesc[0], textureDesc[1]
        )

        // End drawing
        native.endDraw()
        onDrawEnd()
    }

    private fun getPose(m: Matrix) {
        headTracker?.getPose(getBootTimeNano() + params.headTracker.predictionTimeWithoutVsyncNanos,
            ViewportOrientation.LANDSCAPE_LEFT, position, orientation)
        m.setTranslation(position)
        m *= quat.set(orientation).toMatrix(mat)
    }

    open fun onDrawStart() { }
    open fun onDraw(eye: Eye) { }
    open fun onDrawEnd() { }

    private fun updateDeviceParams(): Boolean {
        // Checks if screen or device parameters changed
        if (!paramsChanged && deviceParamId == QrCode.deviceParamsChangedCount) { return true }

        // Get saved device parameters
        val buffer = QrCode.savedDeviceParams
        if (buffer === null || buffer.isEmpty()) { return false }  // no params saved yet, return false

        lensDistortion?.destroy()
        val ld = LensDistortion(buffer, width, height).also { lensDistortion = it }

        distortionRenderer?.destroy()
        val dr = native.setupGraphics(this).also { distortionRenderer = it }

        dr.setMeshes(ld)

        // Get eye matrices
        ld.getEyeFromHeadMatrix(Eye.LEFT, eyeMatrices[0].m)
        ld.getEyeFromHeadMatrix(Eye.RIGHT, eyeMatrices[1].m)
        ld.getProjectionMatrix(Eye.LEFT, zNear, zFar, projMatrices[0].m)
        ld.getProjectionMatrix(Eye.RIGHT, zNear, zFar, projMatrices[1].m)

        paramsChanged = false
        deviceParamId = QrCode.deviceParamsChangedCount
        return true
    }

    /**
     * Sets a single texture for both eyes. The left eye will use the left half
     * of the texture and the right eye will use the right half.
     *
     * The texture is either GL texture ID or a native texture handle.
     */
    fun setTexture(texture: Long) {
        textureDesc[0] = EyeTextureDescription(texture, 0f, 0.5f)
        textureDesc[1] = EyeTextureDescription(texture, 0.5f, 1f)
    }

    /**
     * Sets textures for both eyes. The left eye will use the left texture and
     * the right eye will use the right texture.
     *
     * The texture is either GL texture ID or a native texture handle.
     */
    fun setTexture(left: Long, right: Long) {
        textureDesc[0] = EyeTextureDescription(left)
        textureDesc[1] = EyeTextureDescription(right)
    }
}
