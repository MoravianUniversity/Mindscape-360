/**
 * JNI bindings for the Cardboard QrCode class.
 */

#include <jni.h>
#include "cardboard.h"
#include "general.hpp"


JNI_CARDBOARD_FUNC(jbyteArray, QrCode_nativeGetSavedDeviceParams)(JNIEnv *env, jclass clazz) {
    uint8_t* encoded_device_params = nullptr;
    int size = 0;
    CardboardQrCode_getSavedDeviceParams(&encoded_device_params, &size);
    if (!encoded_device_params) { return nullptr; }
    jbyteArray result = makeByteArray(env, encoded_device_params, size);
    CardboardQrCode_destroy(encoded_device_params);
    return result;
}


JNI_CARDBOARD_FUNC(jboolean, QrCode_nativeHasSavedDeviceParams)(JNIEnv *env, jclass clazz) {
    uint8_t* encoded_device_params = nullptr;
    int size = 0;
    CardboardQrCode_getSavedDeviceParams(&encoded_device_params, &size);
    if (!encoded_device_params) { return false; }
    CardboardQrCode_destroy(encoded_device_params);
    return size > 0;
}

JNI_CARDBOARD_FUNC(void, QrCode_nativeSaveDeviceParams)(JNIEnv *env, jclass clazz, jstring uri) {
    if (uri == nullptr) { return; }

    const char* uri_str = env->GetStringUTFChars(uri, nullptr);
    if (uri_str == nullptr) { return; }

    int size = env->GetStringUTFLength(uri);
    CardboardQrCode_saveDeviceParams(reinterpret_cast<const uint8_t*>(uri_str), size);
    env->ReleaseStringUTFChars(uri, uri_str);
}

JNI_CARDBOARD_FUNC(void, QrCode_nativeScanQrCodeAndSaveDeviceParams)(JNIEnv *env, jclass clazz) {
    CardboardQrCode_scanQrCodeAndSaveDeviceParams();
}

JNI_CARDBOARD_FUNC(jint, QrCode_nativeGetDeviceParamsChangedCount)(JNIEnv *env, jclass clazz) {
    return CardboardQrCode_getDeviceParamsChangedCount();
}

JNI_CARDBOARD_FUNC(jbyteArray, QrCode_nativeGetCardboardV1DeviceParams)(JNIEnv *env, jclass clazz) {
    uint8_t* encoded_device_params = nullptr;
    int size = 0;
    CardboardQrCode_getCardboardV1DeviceParams(&encoded_device_params, &size);
    return makeByteArray(env, encoded_device_params, size);
}
