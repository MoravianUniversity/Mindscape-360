package cardboard.sdk

expect object QrCode {
    /**
     * Gets currently saved device parameters.
     * @return A byte array containing the serialized device parameters or null/empty
     *         if no parameters are saved.
     */
    val savedDeviceParams: ByteArray?

    /**
     * Checks if there are saved device parameters. Convenience property to avoid
     * allocating arrays or null checks.
     */
    val hasSavedDeviceParams: Boolean

    /**
     * Saves the encoded device parameters provided by a URI.
     *
     * @param uri UTF-8 URI string containing device parameters.
     */
    fun saveDeviceParams(uri: String)

    /**
     * Scans a QR code and saves the encoded device parameters.
     */
    fun scanQrCodeAndSaveDeviceParams()

    /**
     * Gets the count of successful device parameters read and save operations.
     *
     * @return The count of successful operations.
     */
    val deviceParamsChangedCount: Int

    /**
     * Gets Cardboard V1 device parameters.
     *
     * @return Reference to the device parameters, or an empty byte array if unavailable.
     */
    val cardboardV1DeviceParams: ByteArray?
}

internal val DUMMY_PARAMS_URI = "https://google.com/cardboard/cfg?p=CgZHb29nbGUSEkNhcmRib2FyZCBJL08gMjAxNR0rGBU9JQHegj0qEAAASEIAAEhCAABIQgAASEJYADUpXA89OggeZnc-Ej6aPlAAYAM"
