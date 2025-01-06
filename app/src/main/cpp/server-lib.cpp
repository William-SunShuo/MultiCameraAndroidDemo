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
Java_com_blink_monitor_BLRTCServerSession_startSession(JNIEnv *env, jobject thiz,jobject surface, jlong handle) {
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
Java_com_blink_monitor_BLRTCServerSession_sendMessage(JNIEnv *env, jobject thiz, jbyteArray msg,jint msgType,
                                                      jstring clientIp, jlong handle) {
    auto* session = getNativeHandle(handle);
    if (session) {
        if (clientIp == nullptr) {
            return;
        }
        const char *clientIpStr = env->GetStringUTFChars(clientIp, nullptr);
        std::string client(clientIpStr);
        char buffer[1024];
//        NSPMessage nspMessage;
//        nspMessage.msgType = static_cast<NSPMessageType>(msgType);
//        // 获取 jbyteArray 的长度
//        jsize length = env->GetArrayLength(msg);
//        // 分配本地缓冲区
//        char *msgBuffer = new char[length + 1]; // +1 是为了容纳 '\0'
//        // 将 jbyteArray 内容拷贝到本地缓冲区
//        env->GetByteArrayRegion(msg, 0, length, reinterpret_cast<jbyte *>(msgBuffer));
//        // 确保字符串以 '\0' 结尾
//        msgBuffer[length] = '\0';
//        std::strcpy(nspMessage.data, msgBuffer);
//        nspMessage.setLength();
//        nspMessage.packet(buffer);
//        session->sendMessage(&nspMessage, client);
//        env->ReleaseStringUTFChars(clientIp, clientIpStr);
//        delete[] msgBuffer;
    }
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_blink_monitor_BLRTCServerSession_nativeCreate(JNIEnv *env, jobject thiz) {
    std::cout.rdbuf(new AndroidBuf);
    LOGI("nativeCreate");
    auto* session = new BLRTCServerSession();
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
        onPeerAddressMethod = env->GetMethodID(cls, "onPeerAddress", "(Ljava/lang/String;Ljava/lang/String;I)V");
        onPeerConnectStatusMethod = env->GetMethodID(cls, "onPeerConnectStatus", "(Ljava/lang/String;I)V");
        onDecodedFrameMethod = env->GetMethodID(cls, "onDecodedFrame", "([B)V");
        onPeerMessageMethod = env->GetMethodID(cls, "onPeerMessage", "(Ljava/lang/String;I[B)V");
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
        jint jDeviceType = client.device_type;
        env->CallVoidMethod(listenerGlobalRef, onPeerAddressMethod, jIpAddress, jDeviceName);
        env->DeleteLocalRef(jIpAddress);
        env->DeleteLocalRef(jDeviceName);
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
        // 提取 msgType
//        jint msgType = static_cast<jint>(message.msgType);
//        // 创建 byte[] (jbyteArray) 并填充数据
//        jbyteArray byteArray = env->NewByteArray(message.length);
//        if (byteArray) {
//            env->SetByteArrayRegion(byteArray, 0, message.length, reinterpret_cast<jbyte*>(message.data));
//        }
        // 调用 Java 回调方法
//        env->CallVoidMethod(listenerGlobalRef, onPeerMessageMethod, msgType, byteArray);
//        // 释放本地引用
//        if (byteArray) env->DeleteLocalRef(byteArray);
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