package com.blink.monitor
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blink.monitor.databinding.ActivityCaptureDevicesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CaptureDevicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaptureDevicesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BLRTCServerSession.run {
            onConnectListener = object : OnConnectListener {
                override fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: String) {
                    Log.d(
                        "Native",
                        "Peer Address: $ipAddress, Device: $deviceName, thread: ${Thread.currentThread().name}"
                    )
                    lifecycleScope.launch(Dispatchers.Main) {
//                        binding.ipAddress.text = "Ip Address: $ipAddress"
                    }
                }

                override fun onPeerConnectStatus(ipAddress: String, status: Int) {
                    Log.d(
                        "Native",
                        "Client: $ipAddress, Status: $status, thread: ${Thread.currentThread().name}"
                    )
                    lifecycleScope.launch(Dispatchers.Main) {
//                        binding.status.text = "status: $status"
                    }
                }

            }
            createSession()
        }

        binding.goMonitor.setOnClickListener {
            startActivity(Intent(this, MonitorActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BLRTCServerSession.run {
            onConnectListener = null
            destroySession()
        }
    }

}