@file:OptIn(ExperimentalForeignApi::class)

package edu.moravian.cardboard.sdk

import cardboard.native.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned

//private typealias LensDistortionPtr = CPointer<cnames.structs.CardboardLensDistortion>?
private typealias LensDistortionPtr = CValuesRef<cnames.structs.CardboardLensDistortion>?

@OptIn(ExperimentalForeignApi::class)
actual fun LensDistortion(
    encodedDeviceParams: ByteArray,
    displayWidth: Int,
    displayHeight: Int
): LensDistortion = IosLensDistortion(encodedDeviceParams, displayWidth, displayHeight)

@OptIn(ExperimentalForeignApi::class)
class IosLensDistortion(
    internal var nativePointer: LensDistortionPtr?
) : LensDistortion {
    constructor(
        encodedDeviceParams: ByteArray,
        displayWidth: Int,
        displayHeight: Int
    ): this(create(encodedDeviceParams, displayWidth, displayHeight))

    init {
        require(nativePointer != null) { "Failed to create LensDistortion" }
    }
    companion object {
        fun create(
            encodedDeviceParams: ByteArray,
            displayWidth: Int,
            displayHeight: Int
        ): LensDistortionPtr? {
            encodedDeviceParams.usePinned { params ->
                return CardboardLensDistortion_create(
                    params.addressOf(0).reinterpret(),
                    encodedDeviceParams.size,
                    displayWidth,
                    displayHeight
                )
            }
        }
    }

    override fun getEyeFromHeadMatrix(eye: Eye, matrix: FloatArray) {
        check(nativePointer != null) { "LensDistortion is disposed." }
        require(matrix.size >= 16) { "Matrix array must have at least 16 elements." }
        matrix.usePinned { matrix ->
            CardboardLensDistortion_getEyeFromHeadMatrix(
                nativePointer,
                eye.value.toUInt().convert(),
                matrix.addressOf(0)
            )
        }
    }

    override fun destroy() {
        if (nativePointer != null) {
            CardboardLensDistortion_destroy(nativePointer)
            nativePointer = null
        }
    }

    override fun getProjectionMatrix(eye: Eye, zNear: Float, zFar: Float, matrix: FloatArray) {
        check(nativePointer != null) { "LensDistortion is disposed." }
        require(matrix.size >= 16) { "Matrix array must have at least 16 elements." }
        matrix.usePinned { matrix ->
            CardboardLensDistortion_getProjectionMatrix(
                nativePointer, eye.value.toUInt(), zNear, zFar,
                matrix.addressOf(0)
            )
        }
    }

    override fun getFieldOfView(eye: Eye, fov: FloatArray) {
        check(nativePointer != null) { "CardboardLensDistortion is disposed." }
        require(fov.size >= 4) { "FOV array must have at least 4 elements." }
        fov.usePinned { matrix ->
            CardboardLensDistortion_getFieldOfView(
                nativePointer, eye.value.toUInt(),
                matrix.addressOf(0)
            )
        }
    }

    override fun getDistortionMesh(eye: Eye): Mesh {
        check(nativePointer != null) { "CardboardLensDistortion is disposed." }
        memScoped {
            val nativeMesh = alloc<CardboardMesh>()
            // TODO: causes a compile error, however, this function is not currently used anywhere
//            CardboardLensDistortion_getDistortionMesh(
//                nativePointer,
//                eye.value.toUInt(),
//                nativeMesh.ptr
//            )
            val indices = nativeMesh.indices!!.toArray(nativeMesh.n_indices)
            val vertices = nativeMesh.vertices!!.toArray(nativeMesh.n_vertices * 2)
            val uvs = nativeMesh.uvs!!.toArray(nativeMesh.n_vertices * 2)
            return Mesh(
                indices = indices,
                vertices = vertices,
                uvs = uvs,
            )
        }
    }

    override fun undistortedUvForDistortedUv(distortedUv: UV, eye: Eye): UV {
        check(nativePointer != null) { "CardboardLensDistortion is disposed." }
        memScoped {
            val cDistortedUv: CValue<CardboardUv> = toNative(distortedUv).reinterpret()
            return CardboardLensDistortion_undistortedUvForDistortedUv(
                nativePointer,
                cDistortedUv.ptr,
                eye.value.toUInt()
            ).fromNative()
        }
    }

    override fun distortedUvForUndistortedUv(undistortedUv: UV, eye: Eye): UV {
        check(nativePointer != null) { "CardboardLensDistortion is disposed." }
        memScoped {
            val cUndistortedUv: CValue<CardboardUv> = toNative(undistortedUv).reinterpret()
            return CardboardLensDistortion_distortedUvForUndistortedUv(
                nativePointer,
                cUndistortedUv.ptr,
                eye.value.toUInt()
            ).fromNative()
        }
    }
}