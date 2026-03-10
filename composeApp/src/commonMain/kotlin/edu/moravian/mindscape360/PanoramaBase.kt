package org.example.cardboard_demo

import edu.moravian.cardboard.CardboardRenderer
import edu.moravian.cardboard.CardboardRendererParams
import edu.moravian.cardboard.Mesh
import edu.moravian.cardboard.NativeMesh
import edu.moravian.cardboard.NativeRenderView
import edu.moravian.cardboard.NativeRenderer
import edu.moravian.cardboard.NativeTexture
import edu.moravian.cardboard.ShaderCollection
import edu.moravian.cardboard.createDefaultNativeRenderer
import edu.moravian.cardboard.sdk.Eye
import edu.moravian.cardboard.util.Matrix
import edu.moravian.mindscape360.dome

open class PanoramaBase(
    view: NativeRenderView,
    shaders: ShaderCollection,
    val mesh: Mesh = dome(5.0f),
    val floorHeight: Float = -1.7f,  // approximate height of the floor in meters
    params: CardboardRendererParams = CardboardRendererParams(),
    native: NativeRenderer = createDefaultNativeRenderer(),
) : CardboardRenderer(view, shaders, params, native) {
    protected var dome: NativeMesh? = null
    protected var texture: NativeTexture? = null

    private val mvpDome = Matrix()
    private val mat = Matrix()  // temporary variable to reduce allocations

    override fun onInit() {
        super.onInit()
        dome = native.loadMesh(mesh)
    }
    override fun onDrawStart() {
        // Incorporate the floor height into the head_view
        headView *= mat.setTranslation(0.0f, floorHeight, 0.0f)
    }
    override fun onDraw(eye: Eye) {
        mvpDome.fromMultiply(
            projMatrices[eye.value],
            mat.fromMultiply(eyeMatrices[eye.value], headView) // mat == eyeView
        )
        native.setModelViewMatrix(mvpDome)
        if (texture != null) {
            texture?.bind()
            dome?.draw()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        dome?.destroy()
        dome = null
        texture?.destroy()
        texture = null
    }
}
