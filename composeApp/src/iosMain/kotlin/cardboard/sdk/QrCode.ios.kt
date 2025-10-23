@file:OptIn(ExperimentalForeignApi::class)

package cardboard.sdk

import cardboard.native.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual object QrCode {
    actual val savedDeviceParams get() = memScoped {
        val deviceParams = allocPointerTo<UByteVar>()
        val sizePtr = alloc<IntVar>()
        CardboardQrCode_getSavedDeviceParams(deviceParams.ptr, sizePtr.ptr)
        deviceParams.toArray(sizePtr) { ptr -> CardboardQrCode_destroy(ptr.reinterpret()) }
    }
    actual val hasSavedDeviceParams get() = memScoped {
        val deviceParams = allocPointerTo<UByteVar>()
        val sizePtr = alloc<IntVar>()
        CardboardQrCode_getSavedDeviceParams(deviceParams.ptr, sizePtr.ptr)
        if (deviceParams.value != null) { CardboardQrCode_destroy(deviceParams.value) }
        sizePtr.value > 0 && deviceParams.value != null
    }
    actual fun saveDeviceParams(uri: String) {
        val uriBytes = uri.encodeToByteArray()
        uriBytes.usePinned { pinned ->
            CardboardQrCode_saveDeviceParams(
                pinned.addressOf(0).reinterpret(),
                uriBytes.size
            )
        }
    }
    actual fun scanQrCodeAndSaveDeviceParams() { CardboardQrCode_scanQrCodeAndSaveDeviceParams() }
    actual val deviceParamsChangedCount get() = CardboardQrCode_getDeviceParamsChangedCount()
    actual val cardboardV1DeviceParams get() = memScoped {
        val deviceParams = allocPointerTo<UByteVar>()
        val sizePtr = alloc<IntVar>()
        CardboardQrCode_getCardboardV1DeviceParams(deviceParams.ptr, sizePtr.ptr)
        deviceParams.toArray(sizePtr)
    }
}
