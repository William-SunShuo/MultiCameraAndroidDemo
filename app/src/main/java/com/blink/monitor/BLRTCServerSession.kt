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

    fun createSession() {
        if (nativeHandle != 0L) {
            return
        }
        nativeHandle = nativeCreate()
        addListener(object : BLRTCServerSessionListener {
            override fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: Int) {
                onConnectListener?.onPeerAddress(ipAddress, deviceName, deviceType)
            }

            override fun onPeerConnectStatus(ipAddress: String, status: Int) {
                connectedIp = if (status == 1) ipAddress else null
                onConnectListener?.onPeerConnectStatus(ipAddress, status)
            }

            override fun onDecodedFrame(pixelBuffer: ByteArray?) {
                onMessageListener?.onDecodedFrame(pixelBuffer)
            }

            override fun onPeerMessage(client: String?, msgType: Int, msg: ByteArray?) {
                Log.d("Native", "msgType: ${printCommand(msgType)}")
                onMessageListener?.onPeerMessage(client, msgType, msg)
            }
        })
        startSession(null)
    }

    fun destroySession() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }

    private external fun startSession(surfaceView: Surface?, nativeHandle: Long = this.nativeHandle)
    external fun connectPeerSession(peerIp: String?, nativeHandle: Long = this.nativeHandle)
    external fun stopSession(nativeHandle: Long = this.nativeHandle)
    external fun sendMessage(
        msg: ByteArray? = null, msgType: Int, clientIp: String? = this.connectedIp, nativeHandle: Long = this.nativeHandle
    )

    // 添加 addListener 方法
    private external fun addListener(
        listener: BLRTCServerSessionListener?, nativeHandle: Long = this.nativeHandle
    )

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(nativeHandle: Long)

}

interface OnConnectListener {
    fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: Int)
    fun onPeerConnectStatus(ipAddress: String, status: Int)
}

interface OnMessageListener {
    fun onDecodedFrame(pixelBuffer: ByteArray?)
    fun onPeerMessage(ipAddress: String?, msgType: Int, msg: ByteArray?)
}

// 定义监听器接口
interface BLRTCServerSessionListener : OnConnectListener, OnMessageListener
