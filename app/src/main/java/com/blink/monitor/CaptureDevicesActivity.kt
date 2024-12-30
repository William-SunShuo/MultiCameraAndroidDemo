package com.blink.monitor
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CaptureDevicesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BLRTCServerSession.run {
            onConnectListener = object : OnConnectListener {
                override fun onPeerAddress(ipAddress: String?, deviceName: String?) {
                }

                override fun onPeerConnectStatus(client: String?, status: Int) {
                }

            }
            createSession()
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