#include <jni.h>
#include <string>

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jstring JNICALL
Java_xyz_hiroshifuu_speechapp_activity_SpeechActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

#ifdef __cplusplus
}