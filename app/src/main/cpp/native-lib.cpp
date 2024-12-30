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
    void onPeerConnection(ConnectStatus code) override {
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

    void onPeerMessage(NSPMessage& message) override {
        JNIEnv* env = getJNIEnv();
        if (!env || !globalListener) return;

        jclass listenerClass = env->GetObjectClass(globalListener);
        if (!listenerClass) return;
        // 获取 onPeerMessage 方法的 ID
        jmethodID onPeerMessageMethod = env->GetMethodID(listenerClass, "onPeerMessage", "(I[B)V");
        if (!onPeerMessageMethod) {
            env->DeleteLocalRef(listenerClass);
            return;
        }
        // 提取 msgType
        jint msgType = static_cast<jint>(message.msgType);

        // 创建 byte[] (jbyteArray) 并填充数据
        jbyteArray byteArray = env->NewByteArray(message.length);
        if (byteArray) {
            env->SetByteArrayRegion(byteArray, 0, message.length, reinterpret_cast<jbyte*>(message.data));
        }
        // 调用 Java 回调方法
        env->CallVoidMethod(globalListener, onPeerMessageMethod, msgType, byteArray);
        // 释放本地引用
        if (byteArray) env->DeleteLocalRef(byteArray);
        env->DeleteLocalRef(listenerClass);
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
Java_com_blink_monitor_BLRTCSession_startPeerConnection(JNIEnv* env, jobject thiz, jstring deviceName) {
    if (!rtcSession) {
        LOGD("RTC Session is not initialized.");
        return;
    }

    const char* deviceNameCStr = env->GetStringUTFChars(deviceName, nullptr);
    rtcSession->startPeerConnection(std::string(deviceNameCStr));
    env->ReleaseStringUTFChars(deviceName, deviceNameCStr);
    LOGD("Started peer connection with device: %s", deviceNameCStr);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCSession_sendPeerMessage(JNIEnv *env, jobject thiz, jint msgType,
                                                    jbyteArray msg) {
    if (!rtcSession) {
        LOGD("RTC Session is not initialized.");
        return;
    }
    char buffer[1024];
    NSPMessage nspMessage;
    nspMessage.msgType = static_cast<NSPMessageType>(msgType);
    // 获取 jbyteArray 的长度
    jsize length = env->GetArrayLength(msg);
    // 分配本地缓冲区
    char *msgBuffer = new char[length + 1]; // +1 是为了容纳 '\0'
    // 将 jbyteArray 内容拷贝到本地缓冲区
    env->GetByteArrayRegion(msg, 0, length, reinterpret_cast<jbyte *>(msgBuffer));
    // 确保字符串以 '\0' 结尾
    msgBuffer[length] = '\0';
    std::strcpy(nspMessage.data, msgBuffer);
    nspMessage.setLength();
    nspMessage.packet(buffer);
    rtcSession->sendPeerMessage(nspMessage);
    delete[] msgBuffer;

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
JNIEXPORT jint JNICALL
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
