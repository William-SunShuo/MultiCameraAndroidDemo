package com.blink.monitor.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blink.monitor.BLRTCServerSession
import com.blink.monitor.OnMessageListener
import com.blink.monitor.printCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class MonitorViewModel : ViewModel() {

    private val _batteryLivaData = MutableLiveData<Int>()
    var batteryLiveData = _batteryLivaData

    private val _elapsedTime = MutableStateFlow("00:00") // 初始时间
    val elapsedTime: StateFlow<String> = _elapsedTime

    private var startTime: Long = 0L
    private var isRunning = false // 计时标志

    private val _isShowJoystick = MutableLiveData(false)
    val isShowJoystick = _isShowJoystick

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
                            _batteryLivaData.postValue(battery.toInt())
                        }
                    }
                }
            }
//            createSession()
        }
    }

    fun toggleJoystick(boolean: Boolean) {
        _isShowJoystick.value = boolean
    }

    fun startTimer() {
        if (isRunning) return // 如果已经在计时，则不重复启动
        isRunning = true
        startTime = System.currentTimeMillis() // 设置开始时间
        // 启动协程更新时间
        viewModelScope.launch {
            while (isRunning) {
                val elapsed = System.currentTimeMillis() - startTime
                _elapsedTime.value = formatElapsedTime(elapsed)
                delay(1000) // 每秒更新
            }
        }
    }

    fun stopTimer() {
        isRunning = false // 设置标志位为 false
        _elapsedTime.value = "00:00"
    }

    private fun formatElapsedTime(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60

        return when {
            hours > 0 -> String.format(
                Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds
            ) // HH:mm:ss
            else -> String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds) // mm:ss
        }
    }

    fun sendControlMsg(msgType: Int, msg: ByteArray? = null) {
        Log.d("MonitorViewModel", "sendControlMsg: ${printCommand(msgType)}, $msg")
        BLRTCServerSession.sendMessage(msg, msgType)
    }

    override fun onCleared() {
        super.onCleared()
        BLRTCServerSession.onMessageListener = null
    }
}