package com.blink.monitor.viewmodel
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blink.monitor.BLRTCServerSession
import com.blink.monitor.CAPTURE_YES
import com.blink.monitor.KEY_IS_CAPTURED
import com.blink.monitor.KEY_PHONE_POWER
import com.blink.monitor.KEY_REMOTE_CONNECT
import com.blink.monitor.KEY_REMOTE_POWER
import com.blink.monitor.KEY_TOPIC
import com.blink.monitor.OnMessageListener
import com.blink.monitor.TOPIC_CAPTURED_SWITCH
import com.blink.monitor.TOPIC_PHONE_POWER
import com.blink.monitor.TOPIC_REMOTE_INFO_STATE
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class MonitorViewModel : ViewModel() {

    private val _batteryLivaData = MutableLiveData<Int>()
    var batteryLiveData = _batteryLivaData

    private val _remoteConnectLivaData = MutableLiveData<Int>()
    var remoteConnectLivaData = _remoteConnectLivaData

    private val _remotePowerLivaData = MutableLiveData<Int>()
    var remotePowerLivaData = _remotePowerLivaData

    private val _elapsedTime = MutableStateFlow("00:00") // 初始时间
    val elapsedTime: StateFlow<String> = _elapsedTime

    private var startTime: Long = 0L
    private var isRunning = false // 计时标志

    private val _isShowJoystick = MutableLiveData(false)
    val isShowJoystick = _isShowJoystick

//    var isShowScorePanel = MutableLiveData(false)

    var homeTeamName = MutableLiveData<String>()

    var awayTeamName = MutableLiveData<String>()

    var gameNameOrEvent = MutableLiveData<String>()

    var colorOfHomeTeam = MutableLiveData(0)

    var colorOfAwayTeam = MutableLiveData(0)

    init {
        BLRTCServerSession.run {
            onMessageListener = object : OnMessageListener {

                override fun onDecodedFrame(pixelBuffer: ByteArray?) { //收到视频帧数据
                    TODO("Not yet implemented")
                }

                override fun onPeerMessage(
                    javaMap: Map<String, Any>
                ) { //收到push端发送的消息，比如电量信息
                    when (javaMap[KEY_TOPIC]) {
                        TOPIC_PHONE_POWER -> {
                            batteryLiveData.postValue(javaMap[KEY_PHONE_POWER] as Int?)
                        }

                        TOPIC_REMOTE_INFO_STATE -> {
                            _remoteConnectLivaData.postValue(javaMap[KEY_REMOTE_CONNECT] as Int?)
                            remotePowerLivaData.postValue(javaMap[KEY_REMOTE_POWER] as Int?)
                        }

                        TOPIC_CAPTURED_SWITCH -> {
                            Log.d("Native", "在拍摄页面：${if (javaMap[KEY_IS_CAPTURED] == CAPTURE_YES) "是" else "否"}")
                        }
                    }
                }
            }
            createSession()
        }
    }

    fun toggleJoystick(boolean: Boolean) {
        _isShowJoystick.value = boolean
    }

//    fun showScorePanel(state: Boolean) {
//        isShowScorePanel.value = state
//    }

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

    override fun onCleared() {
        super.onCleared()
        BLRTCServerSession.run {
            onMessageListener = null
            destroySession()
        }
    }
}