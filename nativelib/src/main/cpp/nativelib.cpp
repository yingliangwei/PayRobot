#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nativelib_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "efg59b69324c8abc";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_nativelib_NativeLib_stringFromJNI1(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI1()
    std::string hello = "abc7fe7d74f4dert";
    return env->NewStringUTF(hello.c_str());
}