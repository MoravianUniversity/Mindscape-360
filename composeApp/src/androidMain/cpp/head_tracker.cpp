/**
 * JNI bindings for the Cardboard HeadTracker class.
 */

#include <jni.h>
#include "cardboard.h"
#include "general.hpp"

JNI_CARDBOARD_FUNC(jlong, AndroidHeadTracker_create)(JNIEnv *env, jclass clazz) {
    CardboardHeadTracker* head_tracker = CardboardHeadTracker_create();
    return reinterpret_cast<jlong>(head_tracker);
}

JNI_CARDBOARD_FUNC(void, AndroidHeadTracker_destroy)(JNIEnv *env, jclass clazz, jlong head_tracker_ptr) {
    auto head_tracker = reinterpret_cast<CardboardHeadTracker*>(head_tracker_ptr);
    CardboardHeadTracker_destroy(head_tracker);
}

JNI_CARDBOARD_FUNC(void, AndroidHeadTracker_pause)(JNIEnv *env, jclass clazz, jlong head_tracker_ptr) {
    auto head_tracker = reinterpret_cast<CardboardHeadTracker*>(head_tracker_ptr);
    CardboardHeadTracker_pause(head_tracker);
}

JNI_CARDBOARD_FUNC(void, AndroidHeadTracker_resume)(JNIEnv *env, jclass clazz, jlong head_tracker_ptr) {
    auto head_tracker = reinterpret_cast<CardboardHeadTracker*>(head_tracker_ptr);
    CardboardHeadTracker_resume(head_tracker);
}

JNI_CARDBOARD_FUNC(void, AndroidHeadTracker_getPose)(JNIEnv *env, jclass clazz,
                                                     jlong head_tracker_ptr,
                                                     jlong timestamp_ns,
                                                     jint viewport_orientation,
                                                     jfloatArray position,
                                                     jfloatArray orientation) {
    auto head_tracker = reinterpret_cast<CardboardHeadTracker*>(head_tracker_ptr);
    jfloat* pos = env->GetFloatArrayElements(position, nullptr);
    jfloat* orient = env->GetFloatArrayElements(orientation, nullptr);
    CardboardHeadTracker_getPose(
        head_tracker, timestamp_ns,
        static_cast<CardboardViewportOrientation>(viewport_orientation),
        pos,orient
    );
    env->ReleaseFloatArrayElements(position, pos, 0);
    env->ReleaseFloatArrayElements(orientation, orient, 0);
}

JNI_CARDBOARD_FUNC(void, AndroidHeadTracker_recenter)(JNIEnv *env, jclass clazz, jlong head_tracker_ptr) {
    auto head_tracker = reinterpret_cast<CardboardHeadTracker*>(head_tracker_ptr);
    CardboardHeadTracker_recenter(head_tracker);
}

JNI_CARDBOARD_FUNC(void, AndroidHeadTracker_setLowPassFilter)(JNIEnv *env, jclass clazz, jlong head_tracker_ptr, jint cutoff_frequency) {
    auto head_tracker = reinterpret_cast<CardboardHeadTracker*>(head_tracker_ptr);
    CardboardHeadTracker_setLowPassFilter(head_tracker, cutoff_frequency);
}
