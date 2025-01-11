package com.blink.monitor

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
            override fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: String) {
                onConnectListener?.onPeerAddress(ipAddress, deviceName, deviceType)
            }

            override fun onPeerConnectStatus(ipAddress: String, status: Int) {
                connectedIp = if (status == 1) ipAddress else null
                onConnectListener?.onPeerConnectStatus(ipAddress, status)
            }

            override fun onDecodedFrame(pixelBuffer: ByteArray?) {
                onMessageListener?.onDecodedFrame(pixelBuffer)
            }

            override fun onPeerMessage(javaMap: Map<String, Any>) {
                onMessageListener?.onPeerMessage(javaMap)
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

    external fun sendMarkingMessage(
        clientIp: String? = this.connectedIp,
        nativeHandle: Long = this.nativeHandle
    ): Int

    external fun sendRecordSwitchMessage(
        isRecording: Int,
        clientIp: String? = this.connectedIp,
        nativeHandle: Long = this.nativeHandle
    ): Int

    external fun sendMuteSwitchMessage(
        muted: Int,
        clientIp: String? = this.connectedIp,
        nativeHandle: Long = this.nativeHandle
    ): Int


    external fun sendSynchronizeSwitchMessage(
        synchronize: Boolean,
        clientIp: String? = this.connectedIp,
        nativeHandle: Long = this.nativeHandle
    ): Int

    external fun sendCapturedSwitchMessage(
        isCaptured: Int,
        clientIp: String? = this.connectedIp,
        nativeHandle: Long = this.nativeHandle
    ): Int

    external fun sendRemoteCtrlMessage(
        direction: Int,
        operation: Int,
        clientIp: String? = this.connectedIp,
        nativeHandle: Long = this.nativeHandle
    ): Int

    external fun sendScoreboardMessage(
        title: String,
        hide: Int,
        section: Int,
        homeName: String,
        homeColor: Int,
        homeScore: Int,
        awayName: String,
        awayColor: Int,
        awayScore: Int,
        clientIp: String? = this.connectedIp,
        nativeHandle: Long = this.nativeHandle
    ): Int

    // 添加 addListener 方法
    private external fun addListener(
        listener: BLRTCServerSessionListener?, nativeHandle: Long = this.nativeHandle
    )

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(nativeHandle: Long = this.nativeHandle)

}

interface OnConnectListener {
    fun onPeerAddress(ipAddress: String, deviceName: String?, deviceType: String)
    fun onPeerConnectStatus(ipAddress: String, status: Int)
}

interface OnMessageListener {
    fun onDecodedFrame(pixelBuffer: ByteArray?)
    fun onPeerMessage(javaMap: Map<String, Any>)
}

// 定义监听器接口
interface BLRTCServerSessionListener : OnConnectListener, OnMessageListener
