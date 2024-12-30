package com.blink.monitor
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blink.monitor.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var ip: String? = null
    private var session: BLRTCServerSession? = null

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
                        Log.d("Native", "onPeerMessage,msgType: ${Command.printCommand(msgType)}, msg: $msg")
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.tvMessage.text = "msgType: ${Command.printCommand(msgType)}, msg: $msg"
                        }
                    }
                })
                startPeerConnection(Build.MANUFACTURER)
            }
        }

        binding.startMonitor.setOnClickListener{
            session = BLRTCServerSession().apply {
                addListener(object : BLRTCServerSessionListener {
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

                    override fun onDecodedFrame(pixelBuffer: ByteArray?) {
                        Log.d("Native", "Decoded Frame Size: " + pixelBuffer!!.size)
                    }
                })
                startSession()
            }
        }

        binding.connect.setOnClickListener {
            session?.run {
                ip?.let {
                    connectPeerSession(it)
                } ?: Toast.makeText(
                    this@MainActivity, "no ip address", Toast.LENGTH_SHORT
                ).show()
            } ?: Toast.makeText(
                this, "Please start monitor first", Toast.LENGTH_SHORT
            ).show()
        }

        binding.sendMessage.setOnClickListener {
            session?.run {
                val message = "Hello from Android"
                ip?.let {
                    sendMessage(message.toByteArray(), Command.ControlPhoto, it)
                }
            } ?: Toast.makeText(
                this, "Please start monitor first", Toast.LENGTH_SHORT
            ).show()
        }
    }
}