package com.blink.monitor.bean

data class CaptureItem (val name: String = "",
                        val address: String = ""
) {
    //ui状态
    var state: Boolean = false
}