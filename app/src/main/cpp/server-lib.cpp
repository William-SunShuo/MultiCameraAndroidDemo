#include <jni.h>
#include "BLRTCServerSession.hpp"
#include "BLNSPServer.hpp"
#include "LogcatBuffer.hpp"
#include <android/log.h>

#include "topic.hpp"
extern "C" {
  #include "monitor-mqtt-message.h"
}
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
Java_com_blink_monitor_BLRTCServerSession_addSurface(JNIEnv *env, jobject thiz, jobject surface,
                                                     jlong handle) {
    auto *session = getNativeHandle(handle);
    if (session) {
        LOGI("addSurface");
        ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
        session->addSurface(nativeWindow);
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
        onPeerAddressMethod = env->GetMethodID(cls, "onPeerAddress", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        onPeerConnectStatusMethod = env->GetMethodID(cls, "onPeerConnectStatus", "(Ljava/lang/String;I)V");
        onDecodedFrameMethod = env->GetMethodID(cls, "onDecodedFrame", "([B)V");
        onPeerMessageMethod = env->GetMethodID(cls, "onPeerMessage", "(Ljava/util/Map;)V");
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
        std::map<std::string, std::shared_ptr<Value>> data;
        unpackMQTTMessage(message, data);
        jclass mapClass = env->FindClass("java/util/HashMap");
        jmethodID initMethod = env->GetMethodID(mapClass, "<init>", "()V");
        jmethodID putMethod = env->GetMethodID(mapClass, "put",
                                               "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        jobject javaMap = env->NewObject(mapClass, initMethod);
        for (const auto &pair: data) {
            std::cout << "Key: " << pair.first  << std::endl;
            jstring key = env->NewStringUTF(pair.first.c_str());
            jobject value = nullptr;
            if (pair.second->getType() == "string") {
                std::string strValue = pair.second->getStringValue();
                std::cout << "Value: " << strValue << std::endl;
                if (!strValue.empty()) {
                    value = env->NewStringUTF(strValue.c_str());
                } else {
                    value = env->NewStringUTF("");
                }
            } else if (pair.second->getType() == "int") {
                jint intValue = pair.second->getIntValue();
                std::cout << "Value: " << intValue << std::endl;
                jclass integerClass = env->FindClass("java/lang/Integer");
                jmethodID intInitMethod = env->GetMethodID(integerClass, "<init>", "(I)V");
                value = env->NewObject(integerClass, intInitMethod, intValue);
                env->DeleteLocalRef(integerClass);
            }
            env->CallObjectMethod(javaMap, putMethod, key, value);
            env->DeleteLocalRef(key);
            env->DeleteLocalRef(value);
        }
        // 调用 Java 中的回调方法
        env->CallVoidMethod(listenerGlobalRef, onPeerMessageMethod, javaMap);
        // 释放局部引用
        env->DeleteLocalRef(mapClass);
        env->DeleteLocalRef(javaMap);

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
    // 定义一个辅助方法，根据 topic 判断 payload 的类型并解包
    static int unpackMQTTMessage(const MQTTMessage &message, std::map<std::string, std::shared_ptr<Value>>& data) {
        if (!message.payload || message.payloadlen == 0 || !message.topic) {
            std::cerr << "Invalid MQTTMessage input." << std::endl;
            return -1; // 输入无效
        }
        std::cout << "message.topic: " << message.topic << std::endl;
        std::string topic(message.topic);
        data["topic"] = std::make_shared<StringValue>(topic); // Add topic to the map
        std::cout << "Topic: " << data["topic"]->getStringValue() << std::endl;
        // 根据 topic 确定消息类型
       if (topic == TOPIC_PHONE_POWER) { // 拍摄端手机电量
            PhonePower info{};
            if (PhonePowerInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["phonePower"] = std::make_shared<IntValue>(info.u8PhonePower);
                std::cout << "Phone power: " << data["phonePower"]->getIntValue() << std::endl;
                return 0;
            }
        } else if (topic == TOPIC_CAPTURED_SWITCH) { // 拍摄端是否已经进入到拍摄页面
            CapturedSwitch info{};
            if (CapturedSwitchInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["isCaptured"] = std::make_shared<IntValue>(info.u8CapturedSwitch);
                std::cout << "Is captured: " << data["isCaptured"]->getIntValue() << std::endl;
                return 0;
            }
        } else if (topic == TOPIC_REMOTE_INFO_STATE) { // 遥控器的连接状态和电量信息
            RemoteInfoState info{};
           if (RemoteInfoStateUnPack(&info, static_cast<const uint8_t *>(message.payload),
                                     message.payloadlen) == 0) {
               data["remoteConnect"] = std::make_shared<IntValue>(info.u8RemoteConnectInfoState);
               data["remotePower"] = std::make_shared<IntValue>(info.u8RemotePowerInfoState);
               std::cout << "Remote connect: " << data["remoteConnect"]->getIntValue() << ", remote power: "
                          << data["remotePower"]->getIntValue() << std::endl;
               return 0;
           }
        }
        std::cerr << "Unrecognized topic or unpacking failed: " << topic << std::endl;
        return -1;
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

// Helper function to send the data (simulate sending over a network or MQTT)
int sendMessage(JNIEnv *env, jstring clientIp, const char *topic, const uint8_t *data, int length, jlong handle) {
    auto *session = getNativeHandle(handle);
    if (!env) {
        LOGI("sendMessage: env is NULL");
        return -1;
    }
    if (!session) {
        LOGI("sendMessage: session is NULL");
        return -1;
    }
    LOGI("sendMessage: topic=%s, length=%d", topic, length);
    if (session) {
        if (clientIp == nullptr) {
            return -1;
        }
        const char *clientIpStr = env->GetStringUTFChars(clientIp, nullptr);
        std::string client(clientIpStr);
        env->ReleaseStringUTFChars(clientIp, clientIpStr);  // 释放内存
        // 调用 C++ 的 sendPeerMessage 方法
        return session->sendPeerMessage(client, topic, length, data);
    }
    return -1;
}

extern "C" {

// Function to send MarkingInfo
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCServerSession_sendMarkingMessage(
        JNIEnv *env, jobject obj, jstring clientIp, jlong handle) {
    MarkingInfo markingInfo{};
    markingInfo.u8MarkingInfo = 1;

    int length = MarkingInfoLength();
    uint8_t infoArray[length];

    if (MarkingInfoPack(&markingInfo, infoArray) != 0) {
        return -1; // Packing failed
    }
    return sendMessage(env, clientIp, TOPIC_MARKING, infoArray, length, handle);
}

// Function to send RecordSwitch
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCServerSession_sendRecordSwitchMessage(
        JNIEnv *env, jobject obj, jint isRecording, jstring clientIp,
        jlong handle) {
    RecordSwitch recordSwitch{};
    recordSwitch.u8RecordSwitch = isRecording;

    int length = RecordSwitchInfoLength();
    uint8_t infoArray[length];

    if (RecordSwitchInfoPack(&recordSwitch, infoArray) != 0) {
        return -1; // Packing failed
    }

    return sendMessage(env, clientIp, TOPIC_RECORD_SWITCH, infoArray, length, handle);
}

// Function to send MuteSwitch
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCServerSession_sendMuteSwitchMessage(
        JNIEnv *env, jobject obj, jint muted, jstring clientIp, jlong handle) {
    MuteSwitch muteSwitch{};
    muteSwitch.u8MuteSwitch = muted;
    int length = MuteSwitchInfoLength();
    uint8_t infoArray[length];

    if (MuteSwitchInfoPack(&muteSwitch, infoArray) != 0) {
        return -1; // Packing failed
    }

    return sendMessage(env, clientIp, TOPIC_MUTE_SWITCH, infoArray, length, handle);
}

// Function to send SynchronizeSwitch
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCServerSession_sendSynchronizeSwitchMessage(
        JNIEnv *env, jobject obj, jboolean synchronize, jstring clientIp, jlong handle) {
    SynchronizeSwitch syncSwitch{};
    syncSwitch.u8SynchronizeSwitch = synchronize ? 1 : 0;

    int length = SynchronizeSwitchInfoLength();
    uint8_t infoArray[length];

    if (SynchronizeSwitchInfoPack(&syncSwitch, infoArray) != 0) {
        return -1; // Packing failed
    }

    return sendMessage(env, clientIp, TOPIC_SYNCHRONIZE_SWITCH, infoArray, length, handle);
}

// Function to send CapturedSwitch
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCServerSession_sendCapturedSwitchMessage(
        JNIEnv *env, jobject obj, jint isCaptured, jstring clientIp,
        jlong handle) {
    CapturedSwitch capturedSwitch{};
    capturedSwitch.u8CapturedSwitch = isCaptured;

    int length = CapturedSwitchInfoLength();
    uint8_t infoArray[length];

    if (CapturedSwitchInfoPack(&capturedSwitch, infoArray) != 0) {
        return -1; // Packing failed
    }

    return sendMessage(env, clientIp, TOPIC_CAPTURED_SWITCH, infoArray, length, handle);
}


// Function to send RemoteCtrlState
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCServerSession_sendRemoteCtrlMessage(
        JNIEnv *env, jobject obj, jint direction, jint operation, jstring clientIp,
        jlong handle) {
    RemoteCtrlState remoteCtrl{};
    remoteCtrl.u8RemoteCtrlDirectionState = static_cast<uint8_t>(direction);
    remoteCtrl.u8RemoteCtrlOperationState = static_cast<uint8_t>(operation);

    int length = RemoteCtrlStateLength();
    uint8_t infoArray[length];

    if (RemoteCtrlStatePack(&remoteCtrl, infoArray) != 0) {
        return -1; // Packing failed
    }
    return sendMessage(env, clientIp, TOPIC_REMOTE_CTRL_STATE, infoArray, length, handle);
}

// Function to send ScoreboardInfo
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCServerSession_sendScoreboardMessage(
        JNIEnv *env, jobject obj, jstring title, jint hide, jint section,
        jstring homeName, jint homeColor, jint homeScore,
        jstring awayName, jint awayColor, jint awayScore, jstring clientIp, jlong handle) {

    ScoreboardInfo scoreboard{};

    const char *nativeTitle = env->GetStringUTFChars(title, nullptr);
    const char *nativeHomeName = env->GetStringUTFChars(homeName, nullptr);
    const char *nativeAwayName = env->GetStringUTFChars(awayName, nullptr);

    strncpy(reinterpret_cast<char *>(scoreboard.title), nativeTitle, sizeof(scoreboard.title));
    strncpy(reinterpret_cast<char *>(scoreboard.homename), nativeHomeName,
            sizeof(scoreboard.homename));
    strncpy(reinterpret_cast<char *>(scoreboard.awayname), nativeAwayName,
            sizeof(scoreboard.awayname));

    scoreboard.hiden = hide;
    scoreboard.section = static_cast<uint8_t>(section);
    scoreboard.homecolor = static_cast<uint32_t>(homeColor);
    scoreboard.homescore = static_cast<uint32_t>(homeScore);
    scoreboard.awaycolor = static_cast<uint32_t>(awayColor);
    scoreboard.awayscore = static_cast<uint32_t>(awayScore);

    env->ReleaseStringUTFChars(title, nativeTitle);
    env->ReleaseStringUTFChars(homeName, nativeHomeName);
    env->ReleaseStringUTFChars(awayName, nativeAwayName);

    int length = ScoreboardInfoLength();
    uint8_t infoArray[length];

    if (ScoreboardInfoPack(&scoreboard, infoArray) != 0) {
        return -1; // Packing failed
    }

    return sendMessage(env, clientIp, TOPIC_SCOREBOARD_INFO, infoArray, length, handle);
}

}

