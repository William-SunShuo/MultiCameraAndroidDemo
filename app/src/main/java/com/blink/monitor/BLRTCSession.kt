package com.blink.monitor

object BLRTCSession {

    init {
        System.loadLibrary("push")
        initSession()
    }

    // Native methods
    external fun initSession()
    external fun addListener(listener: RTCSessionListener)
    external fun startPeerConnection(deviceName: String)
    external fun sendPeerMessage(msgType: Int, data: ByteArray?)
    external fun startLive(url: String?): Int
    external fun stopLive(): Int
    external fun pushVideo(pixelBuffer: ByteArray?)
    external fun pushAudio(audioBuffer: ByteArray?)
}

interface RTCSessionListener {
    fun onPeerConnection(status: Int) // 1=Success, 0=Fail
    fun onPeerMessage(msgType: Int, data: ByteArray?) // Received message from peer
}