#include <jni.h>
#include "cardboard.h"
#include "general.hpp"


JNI_CARDBOARD_FUNC(void, Cardboard_jniInit)(JNIEnv* env, jclass clazz, jobject context) {
    JavaVM* vm = nullptr;
    env->GetJavaVM(&vm);
    Cardboard_initializeAndroid(vm, context);
}
