#include "exr_reader.h"
#include <jni.h>

extern "C" {

JNIEXPORT jboolean JNICALL
Java_me_anno_image_exr_OpenEXRJava_readFloatImageImpl(
    JNIEnv* env, jclass,
    jint width, jint height, jint channels,
    jfloatArray dstArray,
    jbyteArray dataArray
) {
    jfloat* dst = env->GetFloatArrayElements(dstArray, nullptr);
    jbyte* data = env->GetByteArrayElements(dataArray, nullptr);
    jsize size = env->GetArrayLength(dataArray);

    bool ok = readImageImpl<float>(
        width, height, channels,
        dst,
        (const char*)data, size,
        Imf::FLOAT
    );

    env->ReleaseFloatArrayElements(dstArray, dst, 0);
    env->ReleaseByteArrayElements(dataArray, data, JNI_ABORT);

    return ok;
}

JNIEXPORT jboolean JNICALL
Java_me_anno_image_exr_OpenEXRJava_readHalfImageImpl(
    JNIEnv* env, jclass,
    jint width, jint height, jint channels,
    jshortArray dstArray,
    jbyteArray dataArray
) {
    jshort* dst = env->GetShortArrayElements(dstArray, nullptr);
    jbyte* data = env->GetByteArrayElements(dataArray, nullptr);
    jsize size = env->GetArrayLength(dataArray);

    bool ok = readImageImpl<unsigned short>(
        width, height, channels,
        (unsigned short*)dst,
        (const char*)data, size,
        Imf::HALF
    );

    env->ReleaseShortArrayElements(dstArray, dst, 0);
    env->ReleaseByteArrayElements(dataArray, data, JNI_ABORT);

    return ok;
}

}