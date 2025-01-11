package com.blink.monitor

object BLRTCSession {

    init {
        System.loadLibrary("push")
        initSession()
    }

    // Native methods
    private external fun initSession()
    external fun addListener(listener: RTCSessionListener)
    external fun startPeerConnection(deviceName: String, deviceType: String)  // 1:iphone,2:ipad,3:android,4:android-pad

    external fun startLive(url: String?): Int
    external fun stopLive()
    external fun pushVideo(pixelBuffer: ByteArray?)
    external fun pushAudio(audioBuffer: ByteArray?)

    external fun sendPhonePowerMessage(powerPercentage: Int): Int
    external fun sendRemoteInfoMessage(isConnected: Int, powerPercentage: Int): Int
    external fun sendCapturedSwitchMessage(isCaptured: Int): Int
}

interface RTCSessionListener {
    fun onPeerConnection(status: Int) // 1=Success, 0=Fail
    fun onPeerMessage(javaMap: Map<String, Any>) // Received message from peer
}