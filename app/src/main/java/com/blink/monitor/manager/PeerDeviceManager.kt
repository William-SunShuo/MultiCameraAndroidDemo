package com.blink.monitor.manager

import android.util.Log
import com.blink.monitor.bean.PeerDeviceItem
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理已经配对的设备;
 * 管理在同一个网络内能配对的设备列表;
 *
 * 1. 可配对的设备列表获取
 * 2. 正在配对的设备获取
 *
 */
object PeerDeviceManager {

    //需要一个队列去管理对应的配对容器;
    private val peerDevices = ConcurrentHashMap<String, PeerDeviceItem>()

    private var timer: Timer? = null

    private const val EXPIRATION_TIME = 3000L

    var peerDeviceListener: ((List<PeerDeviceItem>) -> Unit)? = null

    //主动检测设备的状态;
    fun startPeerDevices() {
        timer?.cancel()
        timer = Timer()
        timer?.schedule(object :TimerTask() {
            override fun run() {
                checkDevicePeerState()
            }
        }, 3000L, EXPIRATION_TIME)
    }

    /**
     * 检测设备是否持续接收到对方的信号，3s内有更新表示有效设备;
     */
    private fun checkDevicePeerState() {
        val iterator = peerDevices.iterator()
        Log.d("PeerDeviceManager", "before-check....size:${peerDevices.size}" )
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if(System.currentTimeMillis() - entry.value.peerTime > EXPIRATION_TIME) {
                iterator.remove()
            }
        }
        Log.d("PeerDeviceManager", "after-check....size:${peerDevices.size}" )
        peerDeviceListener?.invoke(ArrayList(peerDevices.values))
    }

    fun addPeerDevices(idAddress: String, deviceItem: PeerDeviceItem) {
        peerDevices[idAddress] = deviceItem
    }

    fun getConnectedDevice(): PeerDeviceItem? {
        return peerDevices.map {
            it.value
        }.firstOrNull {
            it.connectedState
        }
    }


    fun setConnectedDevice(deviceItem: PeerDeviceItem) {
        peerDevices[deviceItem.ipAddress] = deviceItem
        peerDevices[deviceItem.ipAddress]?.connectedState = true
    }

    fun getPeerDevices(): List<PeerDeviceItem> {
        return ArrayList(peerDevices.values)
    }


    fun stopPeerDevices() {
        timer?.cancel()
    }

}