package com.blink.monitor


import android.content.Intent
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blink.monitor.adapter.CaptureDeviceListAdapter
import com.blink.monitor.bean.PeerDeviceItem
import com.blink.monitor.databinding.ActivityMonitorCaptureDevicesBinding
import com.blink.monitor.extention.onClick
import com.blink.monitor.manager.PeerDeviceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MonitorCaptureDevicesActivity: BaseBindingActivity<ActivityMonitorCaptureDevicesBinding>() {

    override fun getViewBinding(): ActivityMonitorCaptureDevicesBinding {
        return ActivityMonitorCaptureDevicesBinding.inflate(layoutInflater)
    }

    private var mAdapter: CaptureDeviceListAdapter? = null

    override fun initView() {

        binding.navigationBack.onClick {
            finish()
        }

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(context)
            mAdapter = CaptureDeviceListAdapter().apply {
                connectAction = {item ,position ->
                    item.connectedState = true
                    notifyItemChanged(position)
                    BLRTCServerSession.connectPeerSession(item.ipAddress)
                }
            }
            adapter = mAdapter
        }

        BLRTCServerSession.run {
            onConnectListener = object : OnConnectListener {
                override fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: String) {
                    Log.d(
                        "Native",
                        "Peer Address: $ipAddress, Device: $deviceName, thread: ${Thread.currentThread().name}"
                    )
                    PeerDeviceManager.addPeerDevices(ipAddress, PeerDeviceItem(ipAddress, deviceName, deviceType = deviceType, peerTime = System.currentTimeMillis()))
                    lifecycleScope.launch(Dispatchers.Main) {
                    }
                }

                override fun onPeerConnectStatus(ipAddress: String, status: Int) {
                    Log.d(
                        "Native",
                        "Client: $ipAddress, Status: $status, thread: ${Thread.currentThread().name}"
                    )
                    lifecycleScope.launch(Dispatchers.Main) {
                        startActivity(Intent(this@MonitorCaptureDevicesActivity, MonitorActivity::class.java))
                        finish()
                    }
                }
            }
            createSession()
            PeerDeviceManager.startPeerDevices()
            PeerDeviceManager.peerDeviceListener = { list ->
                runOnUiThread {
                    mAdapter?.submitList(list)
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        BLRTCServerSession.run {
            onConnectListener = null
            PeerDeviceManager.stopPeerDevices()
        }
    }
}