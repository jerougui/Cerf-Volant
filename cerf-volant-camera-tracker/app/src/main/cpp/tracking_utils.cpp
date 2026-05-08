#include <jni.h>
#include <cmath>

extern "C" JNIEXPORT jfloat JNICALL
Java_com_kilotracker_kinematics_Kinematics_nativeComputeSpeed(JNIEnv *env, jclass thiz,
                                                              jfloat dx, jfloat dy, jfloat dz,
                                                              jfloat dt) {
    if (dt <= 0.001f) return 0.0f;
    float speed = sqrtf(dx*dx + dy*dy + dz*dz) / dt;
    return speed;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_kilotracker_kinematics_Kinematics_nativeComputeAzimuth(JNIEnv *env, jclass thiz,
                                                                jfloat dx, jfloat dz) {
    return atan2f(dx, dz);
}

// EMA filter for velocity smoothing
extern "C" JNIEXPORT jfloat JNICALL
Java_com_kilotracker_kinematics_Kinematics_nativeEmaFilter(JNIEnv *env, jclass thiz,
                                                            jfloat current, jfloat prevFiltered,
                                                            jfloat alpha) {
    return alpha * current + (1.0f - alpha) * prevFiltered;
}
