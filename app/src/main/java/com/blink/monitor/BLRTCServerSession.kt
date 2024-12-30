package com.blink.monitor

class BLRTCServerSession {
    private var nativeHandle: Long = 0



    companion object {
        // Load native library
        init {
            System.loadLibrary("server")
        }
    }

    init {
        nativeHandle = nativeCreate()
    }

    fun destroy() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }

    external fun startSession(nativeHandle: Long = this.nativeHandle)
    external fun connectPeerSession(peerIp: String?, nativeHandle: Long = this.nativeHandle)
    external fun stopSession(nativeHandle: Long = this.nativeHandle)
    external fun sendMessage(
        msg: ByteArray?, msgType: Int, client: String?, nativeHandle: Long = this.nativeHandle
    )

    // 添加 addListener 方法
    external fun addListener(
        listener: BLRTCServerSessionListener?, nativeHandle: Long = this.nativeHandle
    )

    private external fun nativeCreate(): Long
    private external fun nativeDestroy(nativeHandle: Long)

}

// 定义监听器接口
interface BLRTCServerSessionListener {
    fun onPeerAddress(ipAddress: String?, deviceName: String?)
    fun onPeerConnectStatus(client: String?, status: Int)
    fun onDecodedFrame(pixelBuffer: ByteArray?)
}
