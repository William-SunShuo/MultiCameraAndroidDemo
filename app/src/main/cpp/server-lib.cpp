#include <jni.h>
#include "BLRTCServerSession.hpp"
#include "BLNSPServer.hpp"
#include "LogcatBuffer.hpp"
#include <android/log.h>
#define  LOG_TAG    "Native"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#include <android/native_window_jni.h>
#include <android/native_window.h>

using namespace rtcsdk;
// 全局函数用于将 Java 的 long 转换为 C++ 的指针
inline BLRTCServerSession* getNativeHandle(jlong handle) {
    return reinterpret_cast<BLRTCServerSession*>(handle);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCServerSession_startSession(JNIEnv *env, jobject thiz,jlong handle) {
    auto* session = getNativeHandle(handle);
    if (session) {
        session->startSession();
//        ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
//        session->setSurface(nativeWindow);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCServerSession_connectPeerSession(JNIEnv *env, jobject thiz,
                                                             jstring peerIp, jlong handle) {
    auto* session = getNativeHandle(handle);
    if (session) {
        const char* peerIpStr = env->GetStringUTFChars(peerIp, nullptr);
        std::string peerIpCpp(peerIpStr);
        session->connectPeerSession(peerIpCpp);
        env->ReleaseStringUTFChars(peerIp, peerIpStr);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCServerSession_stopSession(JNIEnv *env, jobject thiz, jlong handle) {
    auto* session = getNativeHandle(handle);
    if (session) {
        session->stopSession();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCServerSession_sendMessage(JNIEnv *env, jobject thiz, jstring clientIp,jstring topic, jint payloadlen,jbyteArray payload, jlong handle) {
    auto *session = getNativeHandle(handle);
    if (session) {
        if (clientIp == nullptr) {
            return;
        }
        const char *clientIpStr = env->GetStringUTFChars(clientIp, nullptr);
        std::string client(clientIpStr);
        env->ReleaseStringUTFChars(clientIp, clientIpStr);  // 释放内存
        // 获取 topic 字符串
        const char *topic_cstr = env->GetStringUTFChars(topic, nullptr);
        // 获取 payload 字节数组
        jbyte *payload_bytes = env->GetByteArrayElements(payload, nullptr);
        // 调用 C++ 的 sendPeerMessage 方法
        session->sendPeerMessage(client, topic_cstr, payloadlen, payload_bytes);
        // 释放相关内存
        env->ReleaseStringUTFChars(topic, topic_cstr);
        env->ReleaseByteArrayElements(payload, payload_bytes, 0); // 释放并可选设置 0 表示没有修改 payload
    }
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_blink_monitor_BLRTCServerSession_nativeCreate(JNIEnv *env, jobject thiz,jobject surface) {
    std::cout.rdbuf(new AndroidBuf);
    LOGI("nativeCreate");
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
    auto* session = new BLRTCServerSession(nativeWindow);
    return reinterpret_cast<jlong>(session);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCServerSession_nativeDestroy(JNIEnv *env, jobject thiz,
                                                        jlong handle) {
    auto* session = getNativeHandle(handle);
    delete session;
}

// C++ 实现一个 Java Listener 的适配器
class JavaBLRTCServerSessionListener : public BLRTCServerSessionListener {
public:
    JavaBLRTCServerSessionListener(JNIEnv* env, jobject listenerObj)
            : listenerGlobalRef(env->NewGlobalRef(listenerObj)) {
        jclass cls = env->GetObjectClass(listenerObj);
        onPeerAddressMethod = env->GetMethodID(cls, "onPeerAddress", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        onPeerConnectStatusMethod = env->GetMethodID(cls, "onPeerConnectStatus", "(Ljava/lang/String;I)V");
        onDecodedFrameMethod = env->GetMethodID(cls, "onDecodedFrame", "([B)V");
        onPeerMessageMethod = env->GetMethodID(cls, "onPeerMessage", "(Ljava/lang/String;Ljava/lang/String;[B)V");
    }

    ~JavaBLRTCServerSessionListener() {
        JNIEnv* env = getJNIEnv();
        if (listenerGlobalRef) {
            env->DeleteGlobalRef(listenerGlobalRef);
        }
    }

    void onPeerDevicesRefresh(BLNSPClient &client) override {
        JNIEnv *env = getJNIEnv();
        jstring jIpAddress = env->NewStringUTF(client.ip.c_str());
        jstring jDeviceName = env->NewStringUTF(client.name.c_str());
        jstring jDeviceType;
        if (!client.device_type.empty()) {
            jDeviceType = env->NewStringUTF(client.device_type.c_str());
        } else {
            jDeviceType = env->NewStringUTF("0");
        }
        env->CallVoidMethod(listenerGlobalRef, onPeerAddressMethod, jIpAddress, jDeviceName, jDeviceType);
        env->DeleteLocalRef(jIpAddress);
        env->DeleteLocalRef(jDeviceName);
        env->DeleteLocalRef(jDeviceType);
    }

    void onPeerConnectStatus(BLNSPClient &client, int status) override {
        JNIEnv *env = getJNIEnv();
        jstring jIpAddress = env->NewStringUTF(client.ip.c_str());
        env->CallVoidMethod(listenerGlobalRef, onPeerConnectStatusMethod, jIpAddress, status);
        env->DeleteLocalRef(jIpAddress);
    }

    void onDecodedFrame(const std::vector<uint8_t>& pixelBuffer) override {
        JNIEnv* env = getJNIEnv();
        jbyteArray jPixelBuffer = env->NewByteArray(pixelBuffer.size());
        env->SetByteArrayRegion(jPixelBuffer, 0, pixelBuffer.size(), reinterpret_cast<const jbyte*>(pixelBuffer.data()));
        env->CallVoidMethod(listenerGlobalRef, onDecodedFrameMethod, jPixelBuffer);
        env->DeleteLocalRef(jPixelBuffer);
    }

    void onPeerMessage(BLNSPClient& client,const MQTTMessage& message) override {
        JNIEnv* env = getJNIEnv();
        jstring jIpAddress = env->NewStringUTF(client.ip.c_str());

        // 将 C++ 中的 topic 转换为 jstring
        jstring topic = env->NewStringUTF(message.topic);  // 将 C++ 的字符串转换为 Java 字符串

        // 将 C++ 中的 payload 转换为 jbyteArray
        jbyteArray payload = env->NewByteArray(message.payloadlen);  // 创建一个 Java 字节数组
        env->SetByteArrayRegion(payload, 0, message.payloadlen, reinterpret_cast<const jbyte*>(message.payload));  // 设置数据

        // 调用 Java 中的回调方法
        env->CallVoidMethod(listenerGlobalRef, onPeerMessageMethod, jIpAddress, topic, payload);
        // 释放局部引用
        env->DeleteLocalRef(jIpAddress);
        env->DeleteLocalRef(topic);
        env->DeleteLocalRef(payload);
    }
    static JavaVM* javaVM; // 全局 JavaVM 对象
private:
    jobject listenerGlobalRef;
    jmethodID onPeerAddressMethod;
    jmethodID onPeerConnectStatusMethod;
    jmethodID onDecodedFrameMethod;
    jmethodID onPeerMessageMethod;

    JNIEnv* getJNIEnv() {
        // 使用全局 JavaVM
        JNIEnv* env = nullptr;
        if (javaVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
            javaVM->AttachCurrentThread(&env, nullptr);
        }
        return env;
    }
};


extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_BLRTCServerSession_addListener(JNIEnv *env, jobject thiz, jobject listener, jlong handle) {
    auto* session = reinterpret_cast<BLRTCServerSession*>(handle);
    LOGI("addListener");
    if (session && listener) {
        auto* nativeListener = new JavaBLRTCServerSessionListener(env, listener);
        session->addListener(nativeListener);
    }
}

// 全局 JavaVM
JavaVM* JavaBLRTCServerSessionListener::javaVM = nullptr;

extern "C" jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JavaBLRTCServerSessionListener::javaVM = vm;
    return JNI_VERSION_1_6;
}