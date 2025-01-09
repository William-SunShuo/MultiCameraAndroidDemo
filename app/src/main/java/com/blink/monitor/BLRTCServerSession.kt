package com.blink.monitor
import android.util.Log
import android.view.Surface

object BLRTCServerSession {

    private var nativeHandle: Long = 0
    var onConnectListener: OnConnectListener? = null
    var onMessageListener: OnMessageListener? = null
    private var connectedIp: String? = null

    init {
        System.loadLibrary("server")
    }

    fun createSession(surface: Surface) {
        if (nativeHandle != 0L) {
            return
        }
        nativeHandle = nativeCreate(surface)
        addListener(object : BLRTCServerSessionListener {
            override fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: String) {
                onConnectListener?.onPeerAddress(ipAddress, deviceName, deviceType)
                connectedIp = ipAddress
            }

            override fun onPeerConnectStatus(ipAddress: String, status: Int) {
//                connectedIp = if (status == 1) ipAddress else null
                onConnectListener?.onPeerConnectStatus(ipAddress, status)
            }

            override fun onDecodedFrame(pixelBuffer: ByteArray?) {
                onMessageListener?.onDecodedFrame(pixelBuffer)
            }

            override fun onPeerMessage(client: String?, topic: String, msg: ByteArray?) {
//                Log.d("Native", "msgType: ${printCommand(msgType)}")
                onMessageListener?.onPeerMessage(client, topic, msg)
            }
        })
        startSession()
    }

    fun destroySession() {
        if (nativeHandle != 0L) {
            stopSession()
            nativeDestroy()
            nativeHandle = 0
        }
    }

    private external fun startSession(nativeHandle: Long = this.nativeHandle)
    external fun connectPeerSession(peerIp: String?, nativeHandle: Long = this.nativeHandle)
    private external fun stopSession(nativeHandle: Long = this.nativeHandle)
    external fun sendMessage(
        clientIp: String? = this.connectedIp,
        topic: String,
        payloadLen: Int,
        payload: ByteArray,
        nativeHandle: Long = this.nativeHandle
    )

    // 添加 addListener 方法
    private external fun addListener(
        listener: BLRTCServerSessionListener?, nativeHandle: Long = this.nativeHandle
    )

    private external fun nativeCreate(surface: Surface): Long
    private external fun nativeDestroy(nativeHandle: Long = this.nativeHandle)

}

interface OnConnectListener {
    fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: String)
    fun onPeerConnectStatus(ipAddress: String, status: Int)
}

interface OnMessageListener {
    fun onDecodedFrame(pixelBuffer: ByteArray?)
    fun onPeerMessage(ipAddress: String?, topic: String, msg: ByteArray?)
}

// 定义监听器接口
interface BLRTCServerSessionListener : OnConnectListener, OnMessageListener
