package cardboard.sdk

actual object QrCode {
    @JvmStatic
    private external fun nativeGetSavedDeviceParams(): ByteArray?

    @JvmStatic
    private external fun nativeHasSavedDeviceParams(): Boolean

    @JvmStatic
    private external fun nativeSaveDeviceParams(uri: String)

    @JvmStatic
    private external fun nativeScanQrCodeAndSaveDeviceParams()

    @JvmStatic
    private external fun nativeGetDeviceParamsChangedCount(): Int

    @JvmStatic
    private external fun nativeGetCardboardV1DeviceParams(): ByteArray?

    actual val savedDeviceParams get() = nativeGetSavedDeviceParams()
    actual val hasSavedDeviceParams get() = nativeHasSavedDeviceParams()
    actual fun saveDeviceParams(uri: String) { nativeSaveDeviceParams(uri) }
    actual fun scanQrCodeAndSaveDeviceParams() { nativeScanQrCodeAndSaveDeviceParams() }
    actual val deviceParamsChangedCount get() = nativeGetDeviceParamsChangedCount()
    actual val cardboardV1DeviceParams get() = nativeGetCardboardV1DeviceParams()
}
