package com.blink.monitor
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.blink.monitor.databinding.ActivityMainBinding
import com.blink.monitor.extention.onClick
import kotlinx.coroutines.Dispatchers
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

    private var waterView: View? = null


    fun getBitmapFromView(view: View): Bitmap {
        // Define a bitmap with the same dimensions as the view
        Log.d("getBitmapFromView", "width:${view.measuredWidth}, height:${view.measuredHeight}")
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        waterView = LayoutInflater.from(this).inflate(
            R.layout.score_board_regular,
            window.decorView as ViewGroup, false
        )
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        waterView?.measure(widthSpec, heightSpec)

        binding.imageView.post {

            binding.bitmapView.setImageBitmap(getBitmapFromView(binding.scoreBoard.root))
        }


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

                    override fun onPeerMessage(topic: String, data: ByteArray?) {
                        val msg = data?.let { String(it) }
                        Log.d("Native", "onPeerMessage, msg: $msg")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.tvMessage.text = "topic: $topic, msg: $msg"
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

                    override fun onPeerMessage(client: String?, topic: String, msg: ByteArray?) {
                        Log.d("Native", "topic:$topic,msg:${msg?.let { String(it) }}")
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
//            session?.run {
//
//            } ?: Toast.makeText(
//                this, "Please start monitor first", Toast.LENGTH_SHORT
//            ).show()
        }

        binding.sendMessage.setOnClickListener {
            ip?.let {
                BLRTCServerSession.sendMessage(it, "MB", 5, "hello".toByteArray())
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