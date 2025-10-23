package org.example.cardboard_demo

import cardboard.CardboardRenderer
import cardboard.CardboardRendererParams
import cardboard.Mesh
import cardboard.NativeMesh
import cardboard.NativeRenderView
import cardboard.NativeRenderer
import cardboard.NativeTexture
import cardboard.ShaderCollection
import cardboard.createDefaultNativeRenderer
import cardboard.sdk.Eye
import cardboard.util.Matrix
import vr.app.dome

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
        texture?.bind()
        dome?.draw()
    }
    override fun onDestroy() {
        super.onDestroy()
        dome?.destroy()
        dome = null
        texture?.destroy()
        texture = null
    }
}