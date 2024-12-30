package com.blink.monitor.viewmodel
import androidx.lifecycle.ViewModel
import com.blink.monitor.BLRTCServerSession
import com.blink.monitor.OnMessageListener

class MonitorViewModel : ViewModel() {

    init {
        BLRTCServerSession.run {
            onMessageListener = object : OnMessageListener {

                override fun onDecodedFrame(pixelBuffer: ByteArray?) { //收到视频帧数据
                    TODO("Not yet implemented")
                }

                override fun onPeerMessage(
                    client: String?, msgType: Int, msg: ByteArray?
                ) { //收到push端发送的消息，比如电量信息
                    when (msgType) {
                        1 -> { //电量信息
                            val battery = String(msg!!)
                        }
                    }
                }
            }
            createSession()
        }
    }

    fun sendControlMsg(msgType: Int) {
        BLRTCServerSession.sendMessage(msgType = msgType)
    }

    override fun onCleared() {
        super.onCleared()
        BLRTCServerSession.onMessageListener = null
    }
}