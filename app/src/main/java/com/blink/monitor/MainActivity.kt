package com.blink.monitor
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blink.monitor.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var ip: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recordButton.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        binding.btn.setOnClickListener {
            BLRTCSession.apply {
                addListener(object : RTCSessionListener {
                    override fun onPeerConnection(status: Int) {
                        Log.d("Native", "onPeerConnection: $status")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.tvPushStatus.text = "Push Status: $status"
                        }
                    }

                    override fun onPeerMessage(msgType: Int, data: ByteArray?) {
                        val msg = data?.let { String(it) }
                        Log.d("Native", "onPeerMessage,msgType: ${printCommand(msgType)}, msg: $msg")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.tvMessage.text = "msgType: ${printCommand(msgType)}, msg: $msg"
                        }
                    }
                })
                startPeerConnection(Build.MANUFACTURER)
            }
        }

        binding.startMonitor.setOnClickListener{
            BLRTCServerSession.createSession()
            BLRTCServerSession.apply {
                onConnectListener = object:OnConnectListener{
                    override fun onPeerAddress(ipAddress: String?, deviceName: String?) {
                        Log.d("Native", "Peer Address: $ipAddress, Device: $deviceName, thread: ${Thread.currentThread().name}")
                        ip = ipAddress
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.ipAddress.text = "Ip Address: $ipAddress"
                        }
                    }

                    override fun onPeerConnectStatus(client: String?, status: Int) {
                        Log.d("Native", "Client: $client, Status: $status, thread: ${Thread.currentThread().name}")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.status.text = "status: $status"
                        }
                    }

                }
                onMessageListener = object : OnMessageListener {
                    override fun onDecodedFrame(pixelBuffer: ByteArray?) {
                        Log.d("Native", "pixelBuffer size:${pixelBuffer?.size}")
                        if (pixelBuffer != null) {
//                            handleDecodedData(pixelBuffer)
                        }
                    }

                    override fun onPeerMessage(client: String?, msgType: Int, msg: ByteArray?) {
//                TODO("Not yet implemented")
                    }

                }
                createSession()
                setSurface(binding.surfaceView.holder.surface)
            }
        }

        binding.connect.setOnClickListener {
            ip?.let {
                BLRTCServerSession.connectPeerSession(it)
            } ?: Toast.makeText(
                this@MainActivity, "no ip address", Toast.LENGTH_SHORT
            ).show()
//            session?.run {
//
//            } ?: Toast.makeText(
//                this, "Please start monitor first", Toast.LENGTH_SHORT
//            ).show()
        }

        binding.sendMessage.setOnClickListener {
//            session?.run {
//                val message = "Hello from Android"
//                ip?.let {
//                    sendMessage(message.toByteArray(), Command.ControlPhoto, it)
//                }
//            } ?: Toast.makeText(
//                this, "Please start monitor first", Toast.LENGTH_SHORT
//            ).show()
        }
    }

//    fun handleDecodedData(decodedData: ByteArray) {
//        lifecycleScope.launch {
//            // 直接使用解码后的数据，避免额外的内存复制
//            val yuvImage = YuvImage(decodedData, ImageFormat.NV21, 1280, 720, null) // 根据你的 YUV 格式调整
//            val byteArrayOutputStream = ByteArrayOutputStream()
//            yuvImage.compressToJpeg(Rect(0, 0, 1280, 720), 100, byteArrayOutputStream)
//            val imageBytes = byteArrayOutputStream.toByteArray()
//
//            // 转换为 Bitmap
//            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//            binding.imageView.setImageBitmap(bitmap)
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        BLRTCServerSession.onConnectListener = null
    }
}