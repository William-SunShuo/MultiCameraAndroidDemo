package com.blink.monitor
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.blink.monitor.databinding.ActivityMainBinding
import com.blink.monitor.extention.onClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var ip: String? = null
    var blrtc: BLRTCSession? = null

    private val requestBluetoothPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // 权限已授予
                blrtc?.startPeerConnection(BluetoothAdapter.getDefaultAdapter().name ?: "", "3")
            } else {
                // 权限被拒绝
                Toast.makeText(this, "蓝牙权限拒绝", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recordButton.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        binding.btCapDevice.onClick {
            startActivity(Intent(this, MonitorCaptureDevicesActivity::class.java))
        }

        binding.btn.setOnClickListener {

            blrtc = BLRTCSession.apply {
                addListener(object : RTCSessionListener {
                    override fun onPeerConnection(status: Int) {
                        Log.d("Native", "onPeerConnection: $status")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.tvPushStatus.text = "Push Status: $status"
                        }
                    }

                    override fun onPeerMessage(javaMap: Map<String, Any>) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (javaMap["topic"]) {
                                TOPIC_MARKING -> {
                                    binding.tvMessage.text = "打点"
                                }

                                TOPIC_MUTE_SWITCH -> {
                                    if (javaMap["isMuted"] == MUTE_YES) {
                                        binding.tvMessage.text = "静音"
                                    } else {
                                        binding.tvMessage.text = "取消静音"
                                    }
                                }
                                TOPIC_RECORD_SWITCH ->{
                                    if (javaMap["isRecording"] == START_RECORD) {
                                        binding.tvMessage.text = "开始抓拍"
                                    } else {
                                        binding.tvMessage.text = "结束抓拍"
                                    }
                                }
                                TOPIC_CAPTURED_SWITCH ->{
                                    if (javaMap["isCaptured"] == CAPTURE_YES) {
                                        binding.tvMessage.text = "在拍摄页面"
                                    } else {
                                        binding.tvMessage.text = "不在拍摄页面"
                                    }
                                }
                                TOPIC_REMOTE_CTRL_STATE -> {
                                    val operation = javaMap["operation"]
                                    var operationStr = ""
                                    when (operation) {
                                        OPERATION_TAP -> {
                                            operationStr = "短按"
                                        }
                                        OPERATION_LONG_CLICK -> {
                                            operationStr = "长按"
                                        }
                                        OPERATION__RELEASE -> {
                                            operationStr = "松手"
                                        }
                                    }
                                    when (javaMap["direction"]) {
                                        CONTROL_UP -> {
                                            binding.tvMessage.text = "$operationStr：上"
                                        }

                                        CONTROL_DOWN -> {
                                            binding.tvMessage.text = "$operationStr：下"
                                        }

                                        CONTROL_LEFT -> {
                                            binding.tvMessage.text = "$operationStr：左"
                                        }

                                        CONTROL_RIGHT -> {
                                            binding.tvMessage.text = "$operationStr：右"
                                        }
                                    }
                                }
                            }
                        }
                    }
                })
            }
            if(Build.VERSION.SDK_INT > 31) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val adapter = BluetoothAdapter.getDefaultAdapter()

                    blrtc?.startPeerConnection(adapter.name, "3")

                } else {
                    requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                }

            } else {
                val name = Settings.Secure.getString(contentResolver, "bluetooth_name")
                blrtc?.startPeerConnection(name, "3")
            }


        }

        binding.startMonitor.setOnClickListener{
            BLRTCServerSession.apply {
                onConnectListener = object:OnConnectListener{
                    override fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: String) {
                        Log.d("Native", "Peer Address: $ipAddress, Device: $deviceName,deviceType: $deviceType, thread: ${Thread.currentThread().name}")
//                        if (ipAddress != "192.168.20.230") {
//                            return
//                        }
                        ip = ipAddress
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.ipAddress.text = "Ip Address: $ipAddress"
                        }
                    }

                    override fun onPeerConnectStatus(ipAddress: String, status: Int) {
                        Log.d("Native", "Client: $ipAddress, Status: $status, thread: ${Thread.currentThread().name}")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.status.text = "status: $status"
                        }
                    }

                }
                onMessageListener = object : OnMessageListener {
                    override fun onDecodedFrame(pixelBuffer: ByteArray?) {
                        Log.d("Native", "pixelBuffer size:${pixelBuffer?.size}")
                        if (pixelBuffer != null) {
                            handleDecodedData(pixelBuffer)
                        }
                    }

                    override fun onPeerMessage(javaMap: Map<String, Any>) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            when (javaMap["topic"]) {
                                TOPIC_PHONE_POWER -> {
                                    binding.tvMessage.text = "电量：${javaMap["phonePower"]}"
                                }

                                TOPIC_REMOTE_INFO_STATE -> {
                                    binding.tvMessage.text = "连接状态：${javaMap["remoteConnect"]},遥控器电量：${javaMap["remotePower"]}"
                                }

                                TOPIC_CAPTURED_SWITCH -> {
                                    if (javaMap["isCaptured"] == CAPTURE_YES) {
                                        binding.tvMessage.text = "在拍摄页面"
                                    } else {
                                        binding.tvMessage.text = "不在拍摄页面"
                                    }
                                }
                            }
                        }
                    }
                }
                createSession()
//                binding.surfaceView.surfaceTexture?.let {
//                    val surface = Surface(it)
////                    setSurface(surface)
//                    createSession()
//                }
            }
        }

        binding.connect.setOnClickListener {
            ip?.let {
                BLRTCServerSession.connectPeerSession(it)
            } ?: Toast.makeText(
                this@MainActivity, "no ip address", Toast.LENGTH_SHORT
            ).show()
        }
        binding.sendMessage.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Main) {
//                delay(1000)
                BLRTCSession.sendRemoteInfoMessage(CONNECT_REMOTE_YES,60)
//                delay(1000)
//                BLRTCSession.sendPhonePowerMessage(89)
//                delay(1000)
//                BLRTCSession.sendCapturedSwitchMessage(CAPTURE_YES)
            }
        }
    }

    fun handleDecodedData(decodedData: ByteArray) {
        lifecycleScope.launch {
            // 直接使用解码后的数据，避免额外的内存复制
            val yuvImage = YuvImage(decodedData, ImageFormat.NV21, 1280, 720, null) // 根据你的 YUV 格式调整
            val byteArrayOutputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, 1280, 720), 100, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()

            // 转换为 Bitmap
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            binding.imageView.setImageBitmap(bitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BLRTCServerSession.onConnectListener = null
    }
}