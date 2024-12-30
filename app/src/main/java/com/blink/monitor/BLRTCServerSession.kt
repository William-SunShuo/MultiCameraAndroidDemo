package com.blink.monitor
import android.util.Log

object BLRTCServerSession {

    private var nativeHandle: Long = 0
    var onConnectListener: OnConnectListener? = null
    var onMessageListener: OnMessageListener? = null

    init {
        System.loadLibrary("server")
    }

    fun createSession() {
        if (nativeHandle != 0L) {
            return
        }
        nativeHandle = nativeCreate()
        addListener(object : BLRTCServerSessionListener {
            override fun onPeerAddress(ipAddress: String?, deviceName: String?) {
                Log.d(
                    "Native",
                    "Peer Address: $ipAddress, Device: $deviceName, thread: ${Thread.currentThread().name}"
                )
                onConnectListener?.onPeerAddress(ipAddress, deviceName)
            }

            override fun onPeerConnectStatus(client: String?, status: Int) {
                Log.d(
                    "Native",
                    "Client: $client, Status: $status, thread: ${Thread.currentThread().name}"
                )
                onConnectListener?.onPeerConnectStatus(client, status)
            }

            override fun onDecodedFrame(pixelBuffer: ByteArray?) {
                Log.d("Native", "Decoded Frame Size: " + pixelBuffer!!.size)
                onMessageListener?.onDecodedFrame(pixelBuffer)
            }

            override fun onPeerMessage(client: String?, msgType: Int, msg: ByteArray?) {
                Log.d("Native", "msgType: ${printCommand(msgType)}")
                onMessageListener?.onPeerMessage(client, msgType, msg)
            }
        })
        startSession()
    }

    fun destroySession() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }

    private external fun startSession(nativeHandle: Long = this.nativeHandle)
    external fun connectPeerSession(peerIp: String?, nativeHandle: Long = this.nativeHandle)
    external fun stopSession(nativeHandle: Long = this.nativeHandle)
    external fun sendMessage(
        msg: ByteArray? = null, msgType: Int, client: String? = null, nativeHandle: Long = this.nativeHandle
    )

    // 添加 addListener 方法
    private external fun addListener(
        listener: BLRTCServerSessionListener?, nativeHandle: Long = this.nativeHandle
    )

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(nativeHandle: Long)

}

interface OnConnectListener {
    fun onPeerAddress(ipAddress: String?, deviceName: String?)
    fun onPeerConnectStatus(client: String?, status: Int)
}

interface OnMessageListener {
    fun onDecodedFrame(pixelBuffer: ByteArray?)
    fun onPeerMessage(client: String?, msgType: Int, msg: ByteArray?)
}

// 定义监听器接口
interface BLRTCServerSessionListener : OnConnectListener, OnMessageListener
