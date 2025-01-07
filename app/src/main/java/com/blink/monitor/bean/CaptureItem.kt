package com.blink.monitor.bean

data class CaptureItem (val name: String = "",
                        val address: String = ""
) {
    //ui状态
    var state: Boolean = false
}




data class PeerDeviceItem(
    val ipAddress: String = "",
    val deviceName: String? = "",
    //1:iphone,2:ipad,3:android,4:android-pad
    val deviceType: String = "",
    val peerTime: Long = 0L
) {
    //设备连接状态
    var connectedState: Boolean = false
}