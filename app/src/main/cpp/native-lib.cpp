#include <jni.h>
#include <string>
#include "BLRTCSession.hpp"
#include "LogcatBuffer.hpp"
#define  LOG_TAG    "Native"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace rtcpushsdk;
static std::shared_ptr<BLRTCSession> rtcSession = nullptr;
static jobject globalListener = nullptr; // Global reference to the Java listener object

// Helper to attach a thread to the JVM
static JavaVM* javaVM = nullptr;

JNIEnv* getJNIEnv() {
    JNIEnv* env = nullptr;
    if (javaVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        javaVM->AttachCurrentThread(&env, nullptr);
    }
    return env;
}

// Implementation of BLRTCSessionListener
class JNIListener : public BLRTCSessionListener {
public:
    void onPeerConnection(int code) override {
        JNIEnv* env = getJNIEnv();
        if (!env || !globalListener) return;

        jclass listenerClass = env->GetObjectClass(globalListener);
        if (!listenerClass) return;

        jmethodID onPeerConnectionMethod = env->GetMethodID(listenerClass, "onPeerConnection", "(I)V");
        if (onPeerConnectionMethod) {
            env->CallVoidMethod(globalListener, onPeerConnectionMethod, static_cast<jint>(code));
        }
        env->DeleteLocalRef(listenerClass);
    }

    void onPeerMessage(MQTTMessage &message) override {
        JNIEnv *env = getJNIEnv();
        if (!env || !globalListener) return;
        jclass listenerClass = env->GetObjectClass(globalListener);
        if (!listenerClass) return;

        jmethodID onPeerMessageMethod = env->GetMethodID(listenerClass, "onPeerMessage",
                                                         "(Ljava/lang/String;[B)V");
        if (!onPeerMessageMethod) return;
        // 将 C++ 中的 topic 转换为 jstring
        jstring topic = env->NewStringUTF(message.topic);
        auto payloadlen = static_cast<jsize>(message.payloadlen);
        // 将 C++ 中的 payload 转换为 jbyteArray
        jbyteArray payload = env->NewByteArray(payloadlen);  // 创建一个 Java 字节数组
        env->SetByteArrayRegion(payload, 0, payloadlen, reinterpret_cast<const jbyte *>(message.payload));  // 设置数据

        // 调用 Java 中的回调方法
        env->CallVoidMethod(globalListener, onPeerMessageMethod, topic, payload);
        // 释放局部引用
        env->DeleteLocalRef(topic);
        env->DeleteLocalRef(payload);
    }
};

extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_initSession(JNIEnv* env, jobject thiz) {
    std::cout.rdbuf(new AndroidBuf);
//    delete std::cout.rdbuf(0);
    rtcSession = std::make_shared<BLRTCSession>();
    LOGD("RTC Session initialized.");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_addListener(JNIEnv* env, jobject thiz, jobject listener) {
    if (globalListener) {
        env->DeleteGlobalRef(globalListener);
    }
    globalListener = env->NewGlobalRef(listener);

    if (rtcSession) {
        rtcSession->addListener(new JNIListener());
        LOGD("Listener added to RTC Session.");
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_startPeerConnection(JNIEnv* env, jobject thiz, jstring deviceName,jstring deviceType) {
    if (!rtcSession) {
        LOGD("RTC Session is not initialized.");
        return;
    }

    const char* deviceNameCStr = env->GetStringUTFChars(deviceName, nullptr);
    const char* deviceTypeCStr = env->GetStringUTFChars(deviceType, nullptr);
    rtcSession->startSession(std::string(deviceNameCStr), std::string(deviceTypeCStr));
    env->ReleaseStringUTFChars(deviceName, deviceNameCStr);
    env->ReleaseStringUTFChars(deviceType, deviceTypeCStr);
    LOGD("Started peer connection with device: %s", deviceNameCStr);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_sendPeerMessage(JNIEnv *env, jobject thiz, jstring topic, jint payloadlen,jbyteArray payload) {
    if (!rtcSession) {
        LOGD("RTC Session is not initialized.");
        return;
    }
    // 获取 topic 字符串
    const char *topic_cstr = env->GetStringUTFChars(topic, nullptr);
    // 获取 payload 字节数组
    jbyte *payload_bytes = env->GetByteArrayElements(payload, nullptr);
    // 调用 C++ 的 sendPeerMessage 方法
    rtcSession->sendPeerMessage(topic_cstr, payloadlen, payload_bytes);
    // 释放相关内存
    env->ReleaseStringUTFChars(topic, topic_cstr);
    env->ReleaseByteArrayElements(payload, payload_bytes, 0); // 释放并可选设置 0 表示没有修改 payload

}

extern "C"
JNIEXPORT jint JNICALL
Java_com_blink_monitor_BLRTCSession_startLive(JNIEnv *env, jobject thiz, jstring url) {
    const char* nativeUrl = env->GetStringUTFChars(url, nullptr);
    int result = rtcSession->startLive(std::string(nativeUrl));
    env->ReleaseStringUTFChars(url, nativeUrl);
    return result;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_stopLive(JNIEnv *env, jobject thiz) {
    return rtcSession->stopLive();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_pushVideo(JNIEnv *env, jobject thiz, jbyteArray pixelBuffer) {
    jsize length = env->GetArrayLength(pixelBuffer);
    jbyte* nativeBuffer = env->GetByteArrayElements(pixelBuffer, nullptr);
    rtcSession->pushVideo(std::vector<uint8_t>(nativeBuffer, nativeBuffer + length));
    env->ReleaseByteArrayElements(pixelBuffer, nativeBuffer, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_pushAudio(JNIEnv *env, jobject thiz, jbyteArray audioBuffer) {
    jsize length = env->GetArrayLength(audioBuffer);
    jbyte* nativeBuffer = env->GetByteArrayElements(audioBuffer, nullptr);
    rtcSession->pushAudio(std::vector<uint8_t>(nativeBuffer, nativeBuffer + length));
    env->ReleaseByteArrayElements(audioBuffer, nativeBuffer, 0);
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    javaVM = vm;
    return JNI_VERSION_1_6;
}
