#pragma once
#include <jni.h>

#define JNI_FUNC(return_type, package, name) \
    extern "C" JNIEXPORT return_type JNICALL Java_##package##_##name
#define JNI_CARDBOARD_FUNC(return_type, name) \
    JNI_FUNC(return_type, cardboard_sdk, name)  // uses _ as a separator

#define PACKAGE_PATH "cardboard/sdk"  // uses / as a separator

static jbyteArray makeByteArray(JNIEnv* env, const uint8_t* data, int length) {
    if (data == nullptr || length < 0) { return nullptr; }
    jbyteArray arr = env->NewByteArray(length);
    if (arr) { env->SetByteArrayRegion(arr, 0, length, reinterpret_cast<const jbyte *>(data)); }
    return arr;
}
