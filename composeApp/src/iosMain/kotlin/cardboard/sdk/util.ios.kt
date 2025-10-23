@file:OptIn(ExperimentalForeignApi::class)

package cardboard.sdk

import cardboard.native.CardboardEyeTextureDescription
import cardboard.native.CardboardUv
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.posix.CLOCK_UPTIME_RAW
import platform.posix.clock_gettime_nsec_np
import platform.posix.memcpy

actual fun getBootTimeNano(): Long {
    return clock_gettime_nsec_np(CLOCK_UPTIME_RAW.toUInt()).toLong()
}

fun CPointer<IntVar>.toArray(size: Int) = IntArray(size).also { it.usePinned {
    pinned -> memcpy(pinned.addressOf(0), this, (size * Int.SIZE_BYTES).toULong())
} }

fun CPointer<FloatVar>.toArray(size: Int) = FloatArray(size).also { it.usePinned { pinned ->
    memcpy(pinned.addressOf(0), this, (size * Float.SIZE_BYTES).toULong())
} }

inline fun CPointerVar<UByteVar>.toArray(size: IntVar, destroy: (CPointer<*>) -> Unit = {}) = toArray(size.value, destroy)
inline fun CPointerVar<UByteVar>.toArray(size: Int, destroy: (CPointer<*>) -> Unit = {}): ByteArray? {
    if (size < 0) return null
    val arr = this.value ?: return null
    return ByteArray(size).also {
        it.usePinned { pinned -> memcpy(pinned.addressOf(0), arr, (size * Byte.SIZE_BYTES).toULong()) }
        destroy(arr)
    }
}

fun MemScope.toNative(eyeTexture: EyeTextureDescription): CardboardEyeTextureDescription {
    val nativeEyeTexture = alloc<CardboardEyeTextureDescription>()
    nativeEyeTexture.texture = eyeTexture.texture.toULong()
    nativeEyeTexture.left_u = eyeTexture.leftU
    nativeEyeTexture.right_u = eyeTexture.rightU
    nativeEyeTexture.top_v = eyeTexture.topV
    nativeEyeTexture.bottom_v = eyeTexture.bottomV
    return nativeEyeTexture
}

fun MemScope.toNative(uv: UV): CardboardUv {
    val native = alloc<CardboardUv>()
    native.u = uv.u
    native.v = uv.v
    return native
}

fun CValue<CardboardUv>.fromNative(): UV {
    this.useContents { return UV(u, v) }
}
