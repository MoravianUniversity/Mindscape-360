/**
 * JNI bindings for the Cardboard HeadTracker class.
 */

#include <jni.h>
#include "cardboard.h"
#include "general.hpp"


JNI_CARDBOARD_FUNC(jlong, AndroidGLDistortionRenderer_createOpenGLES3)(JNIEnv *env, jclass clazz, jint texture_type) {
    CardboardOpenGlEsDistortionRendererConfig config;
    config.texture_type = static_cast<CardboardSupportedOpenGlEsTextureType>(texture_type);
    CardboardDistortionRenderer* renderer = CardboardOpenGlEs2DistortionRenderer_create(&config);
    return reinterpret_cast<jlong>(renderer);
}

JNI_CARDBOARD_FUNC(jlong, AndroidGLDistortionRenderer_createOpenGLES2)(JNIEnv *env, jclass clazz, jint texture_type) {
    CardboardOpenGlEsDistortionRendererConfig config;
    config.texture_type = static_cast<CardboardSupportedOpenGlEsTextureType>(texture_type);
    CardboardDistortionRenderer* renderer = CardboardOpenGlEs3DistortionRenderer_create(&config);
    return reinterpret_cast<jlong>(renderer);
}

JNI_CARDBOARD_FUNC(void, AndroidGLDistortionRenderer_destroy)(JNIEnv *env, jclass clazz, jlong renderer_ptr) {
    auto renderer = reinterpret_cast<CardboardDistortionRenderer*>(renderer_ptr);
    CardboardDistortionRenderer_destroy(renderer);
}

JNI_CARDBOARD_FUNC(void, AndroidGLDistortionRenderer_setMesh)(JNIEnv *env, jclass clazz, jlong renderer_ptr, jobject mesh_obj, jint eye) {
    auto renderer = reinterpret_cast<CardboardDistortionRenderer*>(renderer_ptr);

    static jclass mesh_class = env->GetObjectClass(mesh_obj);
    static jfieldID indices_field = env->GetFieldID(mesh_class, "indices", "[I");
    static jfieldID vertices_field = env->GetFieldID(mesh_class, "vertices", "[F");
    static jfieldID uvs_field = env->GetFieldID(mesh_class, "uvs", "[F");
    if (!indices_field || !vertices_field || !uvs_field) return;

    auto indices_array = reinterpret_cast<jintArray>(env->GetObjectField(mesh_obj, indices_field));
    auto vertices_array = reinterpret_cast<jfloatArray>(env->GetObjectField(mesh_obj, vertices_field));
    auto uvs_array = reinterpret_cast<jfloatArray>(env->GetObjectField(mesh_obj, uvs_field));
    if (!indices_array || !vertices_array || !uvs_array) return;

    CardboardMesh mesh;
    mesh.n_indices = env->GetArrayLength(indices_array);
    mesh.n_vertices = env->GetArrayLength(vertices_array) / 3; // 3 floats per vertex (x, y, z)
    mesh.indices = env->GetIntArrayElements(indices_array, nullptr);
    mesh.vertices = env->GetFloatArrayElements(vertices_array, nullptr);
    mesh.uvs = env->GetFloatArrayElements(uvs_array, nullptr);

    if (!mesh.indices || !mesh.vertices || !mesh.uvs) {
        if (mesh.indices) env->ReleaseIntArrayElements(indices_array, mesh.indices, 0);
        if (mesh.vertices) env->ReleaseFloatArrayElements(vertices_array, mesh.vertices, 0);
        if (mesh.uvs) env->ReleaseFloatArrayElements(uvs_array, mesh.uvs, 0);
        return;
    }

    CardboardDistortionRenderer_setMesh(renderer, &mesh, static_cast<CardboardEye>(eye));

    env->ReleaseIntArrayElements(indices_array, mesh.indices, 0);
    env->ReleaseFloatArrayElements(vertices_array, mesh.vertices, 0);
    env->ReleaseFloatArrayElements(uvs_array, mesh.uvs, 0);
}

JNI_CARDBOARD_FUNC(void, AndroidGLDistortionRenderer_setMeshes)(JNIEnv *env, jclass clazz, jlong renderer_ptr, jlong lens_distortion_ptr) {
    auto renderer = reinterpret_cast<CardboardDistortionRenderer*>(renderer_ptr);
    auto lens_distortion = reinterpret_cast<CardboardLensDistortion*>(lens_distortion_ptr);

    CardboardMesh mesh;
    CardboardLensDistortion_getDistortionMesh(lens_distortion, kLeft, &mesh);
    CardboardDistortionRenderer_setMesh(renderer, &mesh, kLeft);
    CardboardLensDistortion_getDistortionMesh(lens_distortion, kRight, &mesh);
    CardboardDistortionRenderer_setMesh(renderer, &mesh, kRight);
}

JNI_CARDBOARD_FUNC(void, AndroidGLDistortionRenderer_renderEyeToDisplay)(JNIEnv *env, jclass clazz, jlong renderer_ptr, jlong target, jint x, jint y, jint width, jint height, jobject left_eye_obj, jobject right_eye_obj) {
    auto renderer = reinterpret_cast<CardboardDistortionRenderer*>(renderer_ptr);

    // Extract eye textures
    static jclass eye_texture_class = env->GetObjectClass(left_eye_obj);
    static jfieldID texture_field = env->GetFieldID(eye_texture_class, "texture", "J");
    static jfieldID left_u_field = env->GetFieldID(eye_texture_class, "leftU", "F");
    static jfieldID right_u_field = env->GetFieldID(eye_texture_class, "rightU", "F");
    static jfieldID top_v_field = env->GetFieldID(eye_texture_class, "topV", "F");
    static jfieldID bottom_v_field = env->GetFieldID(eye_texture_class, "bottomV", "F");
    if (!left_u_field || !right_u_field || !top_v_field || !bottom_v_field) return;

    CardboardEyeTextureDescription left_eye = {
            .texture = (uint64_t)env->GetLongField(left_eye_obj, texture_field),
            .left_u = env->GetFloatField(left_eye_obj, left_u_field),
            .right_u = env->GetFloatField(left_eye_obj, right_u_field),
            .top_v = env->GetFloatField(left_eye_obj, top_v_field),
            .bottom_v = env->GetFloatField(left_eye_obj, bottom_v_field),
    };
    CardboardEyeTextureDescription right_eye = {
            .texture = (uint64_t)env->GetLongField(right_eye_obj, texture_field),
            .left_u = env->GetFloatField(right_eye_obj, left_u_field),
            .right_u = env->GetFloatField(right_eye_obj, right_u_field),
            .top_v = env->GetFloatField(right_eye_obj, top_v_field),
            .bottom_v = env->GetFloatField(right_eye_obj, bottom_v_field),
    };

    CardboardDistortionRenderer_renderEyeToDisplay(renderer, (uint64_t)target, x, y, width, height,
                                                   &left_eye, &right_eye);
}
