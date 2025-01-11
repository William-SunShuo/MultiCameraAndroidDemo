#include <jni.h>
#include <string>
#include "BLRTCSession.hpp"
#include "LogcatBuffer.hpp"
#include "topic.hpp"
extern "C" {
  #include "monitor-mqtt-message.h"
}
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

        jmethodID onPeerMessageMethod = env->GetMethodID(listenerClass, "onPeerMessage","(Ljava/util/Map;)V");
        if (!onPeerMessageMethod) return;

        std::map<std::string, std::shared_ptr<Value>> data;
        unpackMQTTMessage(message, data);
        jclass mapClass = env->FindClass("java/util/HashMap");
        jmethodID initMethod = env->GetMethodID(mapClass, "<init>", "()V");
        jmethodID putMethod = env->GetMethodID(mapClass, "put",
                                               "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        jobject javaMap = env->NewObject(mapClass, initMethod);
        for (const auto &pair: data) {
            jstring key = env->NewStringUTF(pair.first.c_str());
            jobject value = nullptr;
            if (pair.second->getType() == "string") {
//                value = env->NewStringUTF(pair.second->getStringValue().c_str());
                std::string strValue = pair.second->getStringValue();
                if (!strValue.empty()) {
                    value = env->NewStringUTF(strValue.c_str());
                } else {
                    LOGD("Empty string value for key: %s", pair.first.c_str());
                    value = env->NewStringUTF("");
                }
            } else if (pair.second->getType() == "int") {
                jint intValue = pair.second->getIntValue();
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
        env->CallVoidMethod(globalListener, onPeerMessageMethod, javaMap);
        env->DeleteLocalRef(javaMap);
        env->DeleteLocalRef(mapClass);
        env->DeleteLocalRef(listenerClass);
    }
private:
// 定义一个辅助方法，根据 topic 判断 payload 的类型并解包
    static int unpackMQTTMessage(const MQTTMessage &message, std::map<std::string, std::shared_ptr<Value>>& data) {
        if (!message.payload || message.payloadlen == 0 || !message.topic) {
            std::cerr << "Invalid MQTTMessage input." << std::endl;
            return -1; // 输入无效
        }

        std::string topic(message.topic);
        data["topic"] = std::make_shared<StringValue>(topic); // Add topic to the map

        // 根据 topic 确定消息类型
        if (topic == TOPIC_MARKING) { //打点
            MarkingInfo info{};
            if (MarkingInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                return 0;
            }
        } else if (topic == TOPIC_RECORD_SWITCH) {  //录制开关
            RecordSwitch info{};
            if (RecordSwitchInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["isRecording"] = std::make_shared<IntValue>(info.u8RecordSwitch);
                return 0;
            }
        } else if (topic == TOPIC_MUTE_SWITCH) { //静音开关
            MuteSwitch info{};
            if (MuteSwitchInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["isMuted"] = std::make_shared<IntValue>(info.u8MuteSwitch);
                return 0;
            }
        } else if (topic == TOPIC_SYNCHRONIZE_SWITCH) { //同步开关
            SynchronizeSwitch info{};
            if (SynchronizeSwitchInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["Syn"] = std::make_shared<IntValue>(info.u8SynchronizeSwitch);
                return 0;
            }
        } else if (topic == TOPIC_CAPTURED_SWITCH) { //拍摄端是否已经进入到拍摄页面
            CapturedSwitch info{};
            if (CapturedSwitchInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["isCaptured"] = std::make_shared<IntValue>(info.u8CapturedSwitch);
                return 0;
            }
        } else if (topic == TOPIC_REMOTE_CTRL_STATE) { //遥控器控制信息
            RemoteCtrlState info{};
            if (RemoteCtrlStateUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["direction"] = std::make_shared<IntValue>(info.u8RemoteCtrlDirectionState);
                data["operation"] = std::make_shared<IntValue>(info.u8RemoteCtrlOperationState);
                return 0;
            }
        } else if (topic == TOPIC_SCOREBOARD_INFO) { //计分板信息
            ScoreboardInfo info{};
            if (ScoreboardInfoUnPack(&info, static_cast<const uint8_t *>(message.payload), message.payloadlen) == 0) {
                data["title"] = std::make_shared<StringValue>(reinterpret_cast<char *>(info.title));
                data["homename"] = std::make_shared<StringValue>(reinterpret_cast<char *>(info.homename));
                data["awayname"] = std::make_shared<StringValue>(reinterpret_cast<char *>(info.awayname));
                data["section"] = std::make_shared<IntValue>(info.section);
                data["hide"] = std::make_shared<IntValue>(info.hiden);
                data["homescore"] = std::make_shared<IntValue>(info.homescore);
                data["awayscore"] = std::make_shared<IntValue>(info.awayscore);
                data["homecolor"] = std::make_shared<IntValue>(info.homecolor);
                data["awaycolor"] = std::make_shared<IntValue>(info.awaycolor);
                return 0;
            }
        }
        std::cerr << "Unrecognized topic or unpacking failed: " << topic << std::endl;
        return -1;
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
extern "C" {
// Function to send PhonePower
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCSession_sendPhonePowerMessage(
        JNIEnv *env, jobject obj, jint powerPercentage) {
    if (powerPercentage < 0 || powerPercentage > 100) {
        return -1; // Invalid value
    }

    PhonePower phonePower{};
    phonePower.u8PhonePower = static_cast<uint8_t>(powerPercentage);

    int length = PhonePowerInfoLength();
    uint8_t infoArray[length];

    if (PhonePowerInfoPack(&phonePower, infoArray) != 0) {
        return -1; // Packing failed
    }
    std::cout << "TOPIC_PHONE_POWER topic: " << TOPIC_PHONE_POWER << std::endl;
    return rtcSession->sendPeerMessage(TOPIC_PHONE_POWER, length, infoArray);
}


// Function to send RemoteInfoState
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCSession_sendRemoteInfoMessage(
        JNIEnv *env, jobject obj, jint isConnected,
        jint powerPercentage) {
    if (powerPercentage < 0 || powerPercentage > 100) {
        return -1; // Invalid value
    }

    RemoteInfoState remoteInfo{};
    remoteInfo.u8RemoteConnectInfoState = isConnected;
    remoteInfo.u8RemotePowerInfoState = static_cast<uint8_t>(powerPercentage);
    int length = RemoteInfoStateLength();
    uint8_t infoArray[length];

    if (RemoteInfoStatePack(&remoteInfo, infoArray) != 0) {
        return -1; // Packing failed
    }
    std::cout << "TOPIC_REMOTE_INFO_STATE topic: " << TOPIC_REMOTE_INFO_STATE << std::endl;
    return rtcSession->sendPeerMessage(TOPIC_REMOTE_INFO_STATE, length, infoArray);
}

// Function to send CapturedSwitch
JNIEXPORT jint JNICALL Java_com_blink_monitor_BLRTCSession_sendCapturedSwitchMessage(
        JNIEnv *env, jobject obj, jint isCaptured) {
    CapturedSwitch capturedSwitch{};
    capturedSwitch.u8CapturedSwitch = isCaptured;

    int length = CapturedSwitchInfoLength();
    uint8_t infoArray[length];

    if (CapturedSwitchInfoPack(&capturedSwitch, infoArray) != 0) {
        return -1; // Packing failed
    }
    std::cout << "TOPIC_CAPTURED_SWITCH topic: " << TOPIC_CAPTURED_SWITCH << std::endl;
    return rtcSession->sendPeerMessage(TOPIC_CAPTURED_SWITCH, length, infoArray);
}
}


