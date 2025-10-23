/**
 * JNI bindings for the Cardboard LensDistortion class.
 */

#include <jni.h>
#include "cardboard.h"
#include "general.hpp"

JNI_CARDBOARD_FUNC(jlong, AndroidLensDistortion_create)(JNIEnv *env, jclass clazz, jbyteArray encoded_device_params, jint display_width, jint display_height) {
    jbyte* params = env->GetByteArrayElements(encoded_device_params, nullptr);
    CardboardLensDistortion* lens_distortion = CardboardLensDistortion_create(
            reinterpret_cast<const uint8_t*>(params),
            env->GetArrayLength(encoded_device_params), display_width, display_height);
    env->ReleaseByteArrayElements(encoded_device_params, params, 0);
    return reinterpret_cast<jlong>(lens_distortion);
}

JNI_CARDBOARD_FUNC(void, AndroidLensDistortion_destroy)(JNIEnv *env, jclass clazz, jlong lens_distortion_ptr) {
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);
    CardboardLensDistortion_destroy(lens_distortion);
}

JNI_CARDBOARD_FUNC(void, AndroidLensDistortion_getEyeFromHeadMatrix)(JNIEnv *env, jclass clazz, jlong lens_distortion_ptr, jint eye, jfloatArray eye_from_head_matrix) {
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);
    auto matrix = static_cast<jfloat *>(env->GetPrimitiveArrayCritical(eye_from_head_matrix, nullptr));
    CardboardLensDistortion_getEyeFromHeadMatrix(lens_distortion, static_cast<CardboardEye>(eye), matrix);
    env->ReleasePrimitiveArrayCritical(eye_from_head_matrix, matrix, 0);
}

JNI_CARDBOARD_FUNC(void, AndroidLensDistortion_getProjectionMatrix)(JNIEnv *env, jclass clazz, jlong lens_distortion_ptr, jint eye, jfloat zNear, jfloat zFar, jfloatArray projection_matrix) {
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);
    auto matrix = static_cast<jfloat *>(env->GetPrimitiveArrayCritical(projection_matrix, nullptr));
    CardboardLensDistortion_getProjectionMatrix(lens_distortion, static_cast<CardboardEye>(eye), zNear, zFar, matrix);
    env->ReleasePrimitiveArrayCritical(projection_matrix, matrix, 0);
}

JNI_CARDBOARD_FUNC(void, AndroidLensDistortion_getFieldOfView)(JNIEnv *env, jclass clazz, jlong lens_distortion_ptr, jint eye, jfloatArray field_of_view) {
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);
    auto fov = static_cast<jfloat *>(env->GetPrimitiveArrayCritical(field_of_view, nullptr));
    CardboardLensDistortion_getFieldOfView(lens_distortion, static_cast<CardboardEye>(eye), fov);
    env->ReleasePrimitiveArrayCritical(field_of_view, fov, 0);
}

static jobject meshFromNative(JNIEnv *env, const CardboardMesh &native) {
    jintArray indices = env->NewIntArray(native.n_indices);
    if (!indices) { return nullptr; }
    jfloatArray vertices = env->NewFloatArray(native.n_vertices * 3);
    if (!vertices) { return nullptr; }
    jfloatArray uvs = env->NewFloatArray(native.n_vertices * 2);
    if (!uvs) { return nullptr; }
    env->SetIntArrayRegion(indices, 0, native.n_indices, native.indices);
    env->SetFloatArrayRegion(vertices, 0, native.n_vertices * 3, native.vertices);
    env->SetFloatArrayRegion(uvs, 0, native.n_vertices * 2, native.uvs);

    static jclass mesh_class = env->FindClass(PACKAGE_PATH "/Mesh");
    static jmethodID constructor = env->GetMethodID(mesh_class, "<init>", "([I[F[F)V");
    return env->NewObject(mesh_class, constructor, indices, vertices, uvs);
}

JNI_CARDBOARD_FUNC(jobject, AndroidLensDistortion_getDistortionMesh)(JNIEnv *env, jclass clazz, jlong lens_distortion_ptr, jint eye) {
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);
    CardboardMesh mesh;
    CardboardLensDistortion_getDistortionMesh(lens_distortion, static_cast<CardboardEye>(eye), &mesh);
    return meshFromNative(env, mesh);
}


static jclass uv_class;
static jfieldID u_field;
static jfieldID v_field;

bool uvToNative(JNIEnv *env, jobject uv, CardboardUv &native_uv) {
    if (!uv_class) {
        uv_class = env->GetObjectClass(uv);
        u_field = env->GetFieldID(uv_class, "u", "F");
        v_field = env->GetFieldID(uv_class, "v", "F");
    }
    if (!u_field || !v_field) { return false; }
    native_uv.u = env->GetFloatField(uv, u_field);
    native_uv.v = env->GetFloatField(uv, v_field);
    return !env->ExceptionCheck();
}

jobject uvFromNative(JNIEnv *env, const CardboardUv &native_uv) {
    if (!uv_class || !u_field || !v_field) { return nullptr; } // assumes uvToNative has been called at least once
    jobject uv = env->AllocObject(uv_class);
    if (uv != nullptr) {
        env->SetFloatField(uv, u_field, native_uv.u);
        env->SetFloatField(uv, v_field, native_uv.v);
    }
    return uv;
}

JNI_CARDBOARD_FUNC(jobject, AndroidLensDistortion_undistortedUvForDistortedUv)(JNIEnv *env, jclass clazz, jlong lens_distortion_ptr, jobject distorted_uv, jint eye) {
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);
    CardboardUv distorted_uv_struct;
    if (!uvToNative(env, distorted_uv, distorted_uv_struct))  { return nullptr; }
    CardboardUv undistorted_uv = CardboardLensDistortion_undistortedUvForDistortedUv(lens_distortion, &distorted_uv_struct, static_cast<CardboardEye>(eye));
    return uvFromNative(env, undistorted_uv);
}

JNI_CARDBOARD_FUNC(jobject, AndroidLensDistortion_distortedUvForUndistortedUv)(JNIEnv *env, jclass clazz, jlong lens_distortion_ptr, jobject undistorted_uv, jint eye) {
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);
    CardboardUv undistorted_uv_struct;
    if (!uvToNative(env, undistorted_uv, undistorted_uv_struct)) { return nullptr; }
    CardboardUv distorted_uv = CardboardLensDistortion_distortedUvForUndistortedUv(lens_distortion, &undistorted_uv_struct, static_cast<CardboardEye>(eye));
    return uvFromNative(env, distorted_uv);
}
