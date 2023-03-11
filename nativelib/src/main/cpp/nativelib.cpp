#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativelib_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "9670b9417980911d";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_nativelib_NativeLib_stringFromJNI1(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI1()
    std::string hello = "a28418e11a42964f";
    return env->NewStringUTF(hello.c_str());
}