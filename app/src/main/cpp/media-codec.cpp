//
// Created by smkj-A on 2024/12/26.
//
#include <jni.h>
#include <string>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>
#include <media/NdkMediaExtractor.h>
#include <android/log.h>
#include <cstring>
#include <fstream>
#include <sstream>
#include <vector>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <fcntl.h>
#include <unistd.h>
#define LOG_TAG "H264Encoder"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

AMediaCodec *codec = nullptr;
std::ofstream outputFile;
bool isInitialized = false;
AMediaFormat *format;
// 输入缓冲区索引
ssize_t inputBufferIndex = -1;
AMediaCodecBufferInfo bufferInfo;

std::string getCurrentTimeFileName() {
    // 获取当前时间
    std::time_t t = std::time(nullptr);
    std::tm *tmPtr = std::localtime(&t);

    // 使用 std::ostringstream 构造文件名
    std::ostringstream fileName;
    fileName << std::put_time(tmPtr, "%m%d%H%M%S") << ".h264";  // 格式：MMDDHHMMSS.h264

    return fileName.str();
}

// JNI 方法来初始化编码器
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_CameraActivity_initCodec(JNIEnv *env, jobject thiz, jint width,
                                                         jint height,jstring path) {
    if (isInitialized) return;

// 创建编码格式
    format = AMediaFormat_new();
    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME,"video/avc");
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, width);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, height);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_BIT_RATE,2000000);  // 降低比特率
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_FRAME_RATE,30);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL,1);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT,21);

    codec = AMediaCodec_createEncoderByType("video/avc");
    if (!codec) {
        LOGE("Failed to create MediaCodec encoder");
        return;
    }

// 打印调试信息
    if (AMediaCodec_configure(codec, format,nullptr, nullptr, AMEDIACODEC_CONFIGURE_FLAG_ENCODE) != AMEDIA_OK) {
        LOGE("Failed to configure MediaCodec encoder");
        return;
    }

// 尝试启动编码器
    int32_t result = AMediaCodec_start(codec);
    if (result != AMEDIA_OK) {
//error code 1100
        LOGE("Failed to start MediaCodec encoder. Error code: %d", result);
        return;
    }

    outputFile.open(env->GetStringUTFChars(path, nullptr), std::ios::binary);
// 打开输出文件流
//    outputFile.open("/data/data/com.blink.monitor/files/output.h264", std::ios::binary);
    if (!outputFile.is_open()) {
        LOGE("Failed to open output file");
        return;
    }

    isInitialized = true;
}
long lastPts = 0;

void processDecodedData(uint8_t *buffer, size_t size);

const long long frameIntervalNs = 1000000000 / 30; // 30fps的时间间隔

// JNI 方法来处理图像并进行编码
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_CameraActivity_processImageForEncoding(JNIEnv *env, jobject thiz,
                                                                       jbyteArray yuvData,
                                                                       jint yuvSize
) {
    if (!isInitialized) {
        LOGE("Codec not initialized!");
        return;
    }

    jbyte *yuvBytes = env->GetByteArrayElements(yuvData, nullptr);
// 获取当前帧的时间戳
    long long currentPts = lastPts + frameIntervalNs;
    lastPts = currentPts;  // 更新上一帧的时间戳
// 获取输入缓冲区
    inputBufferIndex = AMediaCodec_dequeueInputBuffer(codec, 1000);
    if (inputBufferIndex >= 0) {
        size_t inputBufferSize;
        uint8_t *inputBuffer = AMediaCodec_getInputBuffer(codec, inputBufferIndex,&inputBufferSize);

        if (inputBuffer) {
// 将YUV数据复制到输入缓冲区
            std::memcpy(inputBuffer, yuvBytes, yuvSize);
// 提交输入缓冲区
            AMediaCodec_queueInputBuffer(codec, inputBufferIndex,0, yuvSize, currentPts, 0);}
    }

// 释放YUV数据
    env->ReleaseByteArrayElements(yuvData, yuvBytes, JNI_ABORT);

// 获取编码输出数据
    ssize_t outputBufferIndex = AMediaCodec_dequeueOutputBuffer(codec, &bufferInfo, 0);
    if (outputBufferIndex >= 0) {
        size_t outputBufferSize;
        uint8_t *outputBuffer = AMediaCodec_getOutputBuffer(codec, outputBufferIndex,
                                                            &outputBufferSize);
        if (outputBuffer) {
// 将编码后的数据写入文件
            outputFile.write(reinterpret_cast<char *>(outputBuffer), bufferInfo.size);
// 释放输出缓冲区
            AMediaCodec_releaseOutputBuffer(codec, outputBufferIndex,false);
        }
    }
}

// JNI 方法来释放资源
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_CameraActivity_releaseCodec(JNIEnv *env, jobject thiz) {
    if (codec) {
        AMediaCodec_stop(codec);
        AMediaCodec_delete(codec);
        codec = nullptr;
    }
    if (format) {
        AMediaFormat_delete(format);
        format = nullptr;
    }
    if (outputFile.is_open()) {
        outputFile.close();
    }
    isInitialized = false;
    LOGE("Resources cleaned up successfully");
}
#define NALU_START_CODE 0x000001  // H.264 NALU start code

// Function to find NALU start code
bool findNALUStartCode(const uint8_t* data, size_t size, size_t& offset) {
    for (size_t i = 0; i < size - 3; ++i) {
        if (data[i] == 0x00 && data[i + 1] == 0x00 && data[i + 2] == 0x01) {
            offset = i + 3;
            return true;
        }
    }
    return false;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_VideoPlayActivity_decodeAndPlay(JNIEnv *env, jobject obj/* this */, jstring filePath, jobject surface) {
    const char *file_path = env->GetStringUTFChars(filePath, NULL);

    // Step 1: Open .h264 file
    std::ifstream file(file_path, std::ios::binary);
    if (!file.is_open()) {
        LOGE("Failed to open file: %s", file_path);
        return;
    }

    // Step 2: Initialize AMediaCodec for H.264 decoding
    const char* mime = "video/avc";  // MIME type for H.264
    AMediaCodec* codec = AMediaCodec_createDecoderByType(mime);

    // Step 2.1: Create AMediaFormat and configure decoder
    AMediaFormat* format = AMediaFormat_new();
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, 1280);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, 720);
    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME, mime);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 39);
    ANativeWindow* nativeWindow = ANativeWindow_fromSurface(env, surface);
    // Configure the codec with the format and the surface for output
    if (AMediaCodec_configure(codec, format, nativeWindow, nullptr, 0) != AMEDIA_OK) {
        LOGE("Failed to configure codec");
        return;
    }
    AMediaCodec_start(codec);

    // Step 3: Read H.264 stream and feed to codec NALU by NALU
    uint8_t buffer[8192]; // Buffer to read raw data
    ssize_t inputBufferIndex;
    AMediaCodecBufferInfo info;

    std::vector<uint8_t> naluBuffer;

    while (file.read(reinterpret_cast<char*>(buffer), sizeof(buffer))) {
        size_t dataSize = file.gcount();
        size_t offset = 0;

        while (offset < dataSize) {
            size_t startOffset = offset;
            if (findNALUStartCode(&buffer[startOffset], dataSize - startOffset, offset)) {
                // Found the start of the next NALU, copy the previous NALU
                if (naluBuffer.size() > 0) {
                    // Queue the previous NALU to codec
                    inputBufferIndex = AMediaCodec_dequeueInputBuffer(codec, 1000);
                    if (inputBufferIndex >= 0) {
                        uint8_t* inputData;
                        size_t inputDataSize;
                        inputData = AMediaCodec_getInputBuffer(codec, inputBufferIndex, &inputDataSize);

                        // Copy the full NALU to input buffer
                        memcpy(inputData, &naluBuffer[0], naluBuffer.size());

                        // Queue the input buffer with timestamp 0
                        AMediaCodec_queueInputBuffer(codec, inputBufferIndex, 0, naluBuffer.size(), 0, 0);
                    }
                    naluBuffer.clear(); // Clear the buffer for next NALU
                }

                // Add new NALU data to naluBuffer
                naluBuffer.insert(naluBuffer.end(), &buffer[offset], &buffer[startOffset + 3]);

                // Move to next NALU
                offset += 3;
            } else {
                // Add data to naluBuffer
                naluBuffer.insert(naluBuffer.end(), &buffer[offset], &buffer[dataSize]);
                break;
            }
        }

        // Dequeue and process output buffers
        ssize_t outputBufferIndex = AMediaCodec_dequeueOutputBuffer(codec, &info, 1000);
        if (outputBufferIndex >= 0) {
            uint8_t* outputData;
            size_t outputDataSize;
            outputData = AMediaCodec_getOutputBuffer(codec, outputBufferIndex, &outputDataSize);

            // Process decoded frame here, e.g., render to Surface or store it
            LOGE("Decoded frame with size: %zu", outputDataSize);

            AMediaCodec_releaseOutputBuffer(codec, outputBufferIndex, false);
        }
    }

    // Step 4: Handle end of stream
    inputBufferIndex = AMediaCodec_dequeueInputBuffer(codec, 1000);
    if (inputBufferIndex >= 0) {
        AMediaCodec_queueInputBuffer(codec, inputBufferIndex, 0, 0, 0, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
    }

    // Step 5: Cleanup
    AMediaCodec_stop(codec);
    AMediaCodec_delete(codec);

    // Close the file
    file.close();

    env->ReleaseStringUTFChars(filePath, file_path);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_blink_monitor_VideoPlayActivity_decodeH264(JNIEnv *env, jobject obj/* this */, jstring filePath) {
    const char *outputFilePath = env->GetStringUTFChars(filePath, nullptr);

    // 打开 H.264 文件
    int fd = open(outputFilePath, O_RDONLY);
    if (fd == -1) {
        LOGE("Failed to open file: %s, errno: %d", outputFilePath, errno);
        env->ReleaseStringUTFChars(filePath, outputFilePath);
        return;
    }

    // 创建解码器
    AMediaCodec *codec = AMediaCodec_createDecoderByType("video/avc");
    if (codec == nullptr) {
        LOGE("Failed to create AMediaCodec decoder");
        close(fd);
        env->ReleaseStringUTFChars(filePath, outputFilePath);
        return;
    }

    // 配置解码器
    AMediaFormat *format = AMediaFormat_new();
    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME, "video/avc");
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, 1280);  // 假设宽度为 1920
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, 720); // 假设高度为 1080
    AMediaCodec_configure(codec, format, nullptr, nullptr, 0);
    AMediaCodec_start(codec);

    uint8_t buffer[1024 * 1024];  // 假设缓冲区大小为 1MB
    ssize_t bytesRead;

    // 获取handleDecodedData方法ID
    jmethodID handleDecodedDataMethod = env->GetMethodID(
            env->GetObjectClass(obj),
            "handleDecodedData",
            "([B)V"  // 方法签名，表示接收 byte[] 并返回 void
    );

    while (true) {
        // 1. 输入缓冲区（读取文件数据并填充到解码器）
        ssize_t inputBufferIndex = AMediaCodec_dequeueInputBuffer(codec, 0);  // 0 是非阻塞，-1 是阻塞模式
        if (inputBufferIndex >= 0) {
            size_t inputBufferSize;
            uint8_t *inputBuffer = AMediaCodec_getInputBuffer(codec, inputBufferIndex, &inputBufferSize);
            if ((bytesRead = read(fd, buffer, sizeof(buffer))) > 0) {
                memcpy(inputBuffer, buffer, bytesRead);
                AMediaCodec_queueInputBuffer(codec, inputBufferIndex, 0, bytesRead, 0, 0);
            } else if (bytesRead == 0) {
                AMediaCodec_queueInputBuffer(codec, inputBufferIndex, 0, 0, AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM, 0);
                break; // 文件读取完毕，发送 End-of-stream flag
            }
        } else if (inputBufferIndex == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            LOGE("Input buffer unavailable, try again later");
        }

        // 2. 输出缓冲区（获取解码后的数据）
        AMediaCodecBufferInfo bufferInfo;
        ssize_t outputBufferIndex = AMediaCodec_dequeueOutputBuffer(codec, &bufferInfo, 10000);  // 等待最大 10ms
        if (outputBufferIndex >= 0) {
            size_t outputBufferSize;
            uint8_t *outputBuffer = AMediaCodec_getOutputBuffer(codec, outputBufferIndex, &outputBufferSize);

            // 创建 byte[] 数组
            jbyteArray decodedDataArray = env->NewByteArray(outputBufferSize);
            env->SetByteArrayRegion(decodedDataArray, 0, outputBufferSize, reinterpret_cast<jbyte *>(outputBuffer));

            // 调用 Kotlin 层的方法处理解码后的数据
            env->CallVoidMethod(obj, handleDecodedDataMethod, decodedDataArray);

            // 释放解码后的输出缓冲区
            AMediaCodec_releaseOutputBuffer(codec, outputBufferIndex, false);  // 不需要渲染到 Surface

            // 释放 local ref
            env->DeleteLocalRef(decodedDataArray);
        } else if (outputBufferIndex == AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
            LOGE("Output buffers changed");
        } else if (outputBufferIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            LOGE("Output format changed");
        } else if (outputBufferIndex == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            LOGE("No output buffer available, try again later");
        } else {
            LOGE("Unknown error while dequeuing output buffer: %zd", outputBufferIndex);
            break;
        }

        // 检查是否解码结束
        if (bufferInfo.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
            break;
        }
    }

    // 停止并释放解码器
    AMediaCodec_stop(codec);
    AMediaCodec_delete(codec);
    close(fd);

    // 释放 JNI 字符串资源
    env->ReleaseStringUTFChars(filePath, outputFilePath);
}

void decodeH264Data(AMediaCodec* codec, uint8_t* nalData, size_t nalDataSize) {
    size_t dataOffset = 0;  // 当前NAL数据的处理位置
    ssize_t inputIndex = -1; // 解码器的输入缓冲区索引
    ssize_t outputIndex = -1; // 解码器的输出缓冲区索引

    AMediaCodecBufferInfo info;
    while (dataOffset < nalDataSize) {
        // 1. 获取解码器的输入缓冲区（非阻塞）
        inputIndex = AMediaCodec_dequeueInputBuffer(codec, 1000);  // 1000ms超时
        if (inputIndex < 0) {
            // 如果没有可用的输入缓冲区，稍后再试
            continue;
        }

        // 获取输入缓冲区指针和大小
        size_t inputBufferSize = 0;
        uint8_t* inputBuffer = AMediaCodec_getInputBuffer(codec, inputIndex, &inputBufferSize);

        if (inputBuffer == nullptr) {
            // 如果未能获取输入缓冲区，跳过
            continue;
        }

        // 2. 判断NAL数据是否能放入输入缓冲区
        size_t remainingData = nalDataSize - dataOffset;
        size_t toCopy = std::min(remainingData, inputBufferSize);

        // 3. 将NAL数据的一部分复制到输入缓冲区
        memcpy(inputBuffer, nalData + dataOffset, toCopy);

        // 4. 更新数据偏移量
        dataOffset += toCopy;

        // 5. 将数据提交到解码器进行解码
        AMediaCodec_queueInputBuffer(codec, inputIndex, 0, toCopy, 0, 0);

        // 6. 获取解码器的输出缓冲区
        outputIndex = AMediaCodec_dequeueOutputBuffer(codec, &info, 1000);  // 1000ms超时
        while (outputIndex >= 0) {
            // 获取输出数据的指针和大小
            size_t outputBufferSize = 0;
            uint8_t* outputBuffer = AMediaCodec_getOutputBuffer(codec, outputIndex, &outputBufferSize);

            if (outputBuffer != nullptr) {
                // 在这里你可以处理解码后的数据
                processDecodedData(outputBuffer, outputBufferSize);
            }

            // 标记该输出缓冲区已经处理完
            AMediaCodec_releaseOutputBuffer(codec, outputIndex, false);

            // 获取下一个输出缓冲区
            outputIndex = AMediaCodec_dequeueOutputBuffer(codec, &info, 1000);
        }

        // 如果解码器已结束处理，可能会收到输出缓冲区结束标志
        if (outputIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            // 处理格式变化的情况（例如分辨率变动等）
            AMediaFormat* format = AMediaCodec_getOutputFormat(codec);
            if (format != nullptr) {
                // 你可以在这里获取视频分辨率等信息
            }
        }
    }

    // 处理完NAL数据后，如果有未释放的输出缓冲区，需要清理
    while (outputIndex >= 0) {
        size_t outputBufferSize = 0;
        uint8_t* outputBuffer = AMediaCodec_getOutputBuffer(codec, outputIndex, &outputBufferSize);
        if (outputBuffer != nullptr) {
            processDecodedData(outputBuffer, outputBufferSize);
        }
        AMediaCodec_releaseOutputBuffer(codec, outputIndex, false);
        outputIndex = AMediaCodec_dequeueOutputBuffer(codec, &info, 1000);
    }
}

// 处理解码后的数据（可以是渲染到屏幕或保存到文件）
void processDecodedData(uint8_t* data, size_t size) {
    // 这里你可以处理解码后的帧，比如渲染到SurfaceView、保存到文件等
    printf("Processed decoded data, size: %zu\n", size);
}



