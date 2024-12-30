package com.blink.monitor
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blink.monitor.databinding.ActivityCameraBinding
import jp.co.yumemi.android.code_check.utils.YUVUtil


class CameraActivity : AppCompatActivity() {

    private val TAG = "CameraActivity"

    private lateinit var binding: ActivityCameraBinding

    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private lateinit var imageReader:ImageReader
    private var previewSurface: Surface? = null
    private var isRecording = false

    private val requestCameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // 权限已授予
                startCameraPreview()
            } else {
                // 权限被拒绝
                Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraId: String by lazy {
        // 默认使用后置摄像头
        cameraManager.cameraIdList.firstOrNull() ?: throw IllegalStateException("No camera found")
    }

    companion object {
        init {
            System.loadLibrary("mediacodec")
        }
    }
    // 记录时间戳和帧计数
    private var lastFrameTimestamp: Long = 0
    private var frameCount = 0
    private var totalFrameTime = 0L // 总时间（纳秒）

    private val targetFrameCount = 30 // 每 30 帧计算一次帧率
//    // Native方法
    external fun initCodec(width: Int, height: Int, outputPath: String)
    external fun processImageForEncoding(yuvData: ByteArray, yuvSize: Int)
    external fun releaseCodec()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化 SurfaceView 和 SurfaceHolder
        surfaceHolder = binding.surfaceView.holder
        binding.recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                initCodec(
                    1280, 720, "${externalCacheDir?.absolutePath}/output.h264"
                )  // Set the appropriate resolution
                startRecording()
            }
        }
        binding.playButton.setOnClickListener {
            startActivity(Intent(this, VideoPlayActivity::class.java))
        }

        // 初始化 CameraManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // Initialize the camera executor

        val imageThread = HandlerThread("$TAG imageThread")
        imageThread.start()
        // Initialize ImageReader (for YUV format)
        val imageReaderWidth = 1280  // You can use your desired width
        val imageReaderHeight = 720  // You can use your desired height
        imageReader = ImageReader.newInstance(imageReaderWidth, imageReaderHeight, ImageFormat.YUV_420_888, 1)
        // Set the listener for ImageReader
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            val currentTimestamp = System.nanoTime() // 获取当前时间戳

            // 如果是第一帧，跳过时间计算
            if (lastFrameTimestamp != 0L) {
                // 计算时间间隔（单位：纳秒）
                val timeInterval = currentTimestamp - lastFrameTimestamp
                totalFrameTime += timeInterval
                frameCount++

                // 每 30 帧计算一次帧率
                if (frameCount >= targetFrameCount) {
                    // 计算帧率
                    val frameRate = (targetFrameCount * 1_000_000_000.0) / totalFrameTime
                    Log.d(TAG, "Frame Rate: $frameRate fps,thread: ${Thread.currentThread().name}")

                    // 重置计数器和时间
                    frameCount = 0
                    totalFrameTime = 0L
                }
            }

            // 更新最后一帧的时间戳
            lastFrameTimestamp = currentTimestamp
            if (image != null) {
//                Log.d(TAG, "Image received")
                if (isRecording) {
                    // Convert image to YUV format and send to C++ for encoding
                    // 获取开始时间
                    val startTime = System.nanoTime()
                    // 创建 byte[] 缓冲区存储 YUV420Planar 格式的数据
                    val yuv420Planar = YUVUtil.yuv420888toNV21(image)
                    BLRTCSession.pushVideo(yuv420Planar)
                    // 立即关闭 Image 释放资源
                    image.close()
                    val midTime = System.nanoTime()

// 执行处理方法
//                    yuv420Planar?.let {
//                        processImageForEncoding(it, it.size)
//                    }

// 获取结束时间
                    val endTime = System.nanoTime()

// 计算耗时（单位：毫秒）
                    val elapsedTimeMillis = (endTime - midTime) / 1_000_000.0 // 转换为毫秒
                    val elapsedTimeMillis2 = (midTime - startTime) / 1_000_000.0 // 转换为毫秒

// 打印耗时
                    // println("processImageForEncoding took $elapsedTimeMillis ms,yuv420toNV21 took $elapsedTimeMillis2")
                    // 接下来处理数据（例如传给编码器）

                } else {
                    // Process image or pass to another component
                    image.close()  // Don't forget to close the image
                }

            }
        }, Handler(imageThread.looper))

        // 设置 SurfaceHolder 的回调
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                previewSurface = holder.surface
                // 启动相机预览
                startCameraPreview()
                Log.e("Camera2", "surfaceCreated")

            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // Surface 改变时的处理（如果需要）
                Log.e("Camera2", "surfaceChanged")

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // 释放相机资源
                closeCamera()
                Log.e("Camera2", "surfaceDestroyed")

            }
        })
    }
    private fun startRecording() {
        isRecording = true
        binding.recordButton.text = "Stop Recording"
        // Initialize codec and other resources for recording
        startCodecForRecording()
    }

    private fun stopRecording() {
        isRecording = false
        binding.recordButton.text = "Start Recording"
        // Release resources and save the encoded file
        stopCodecAndSaveFile()
    }
    private fun startCodecForRecording() {
        // Start the codec for encoding and output file saving
        // Call the C++ function to initialize the codec and start encoding

    }

    private fun stopCodecAndSaveFile() {
        // Stop the codec and save the encoded video file
        releaseCodec()
    }
    private fun startCameraPreview() {
        try {
            // 获取相机权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

                // 打开相机
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        // 相机成功打开
                        cameraDevice = camera
                        createCameraPreviewSession()
                        Log.e("Camera2", "Camera onOpened")
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        cameraDevice?.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        // 错误处理
                        Log.e("Camera2", "Camera error: $error")
                    }
                }, null)
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createCameraPreviewSession() {
        try {
            // 创建 CameraCaptureRequest，用于捕获图像数据
            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(previewSurface!!)  // Surface for preview

            // 将 ImageReader 的 Surface 添加到列表中，用于捕获图像数据
            captureRequestBuilder?.addTarget(imageReader.surface)

            // 创建一个 CameraCaptureSession，用于预览
            cameraDevice?.createCaptureSession(listOf(previewSurface!!, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return

                    cameraCaptureSession = session

                    // 设置相机预览模式
                    captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

                    // 开始相机预览
                    session.setRepeatingRequest(captureRequestBuilder?.build()!!, null, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Toast.makeText(applicationContext, "Camera configuration failed", Toast.LENGTH_SHORT).show()
                }
            }, Handler(Looper.getMainLooper())) // 使用主线程的 Handler
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun closeCamera() {
        // 关闭相机设备和相关资源
        cameraCaptureSession?.close()
        cameraDevice?.close()
    }


    override fun onPause() {
        super.onPause()
        // 在 Activity 暂停时，关闭相机

//        imageReader.close()
//        closeCamera()
    }


}