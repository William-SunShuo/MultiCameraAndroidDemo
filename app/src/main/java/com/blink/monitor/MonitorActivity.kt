package com.blink.monitor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blink.monitor.databinding.ActivityMonitorBinding
import com.blink.monitor.extention.gone
import com.blink.monitor.extention.visible
import com.blink.monitor.fragment.ScoreCountFragment
import com.blink.monitor.fragment.ScorePanelFragment
import com.blink.monitor.viewmodel.MonitorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MonitorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitorBinding

    private val viewModel: MonitorViewModel by viewModels()
    private val hideViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // 设置全屏
        setFullScreen()
//        binding.cameraView.post{
//            binding.cameraView.apply {
//                BLRTCServerSession.addSurface(holder.surface)
//                Log.d("size", "width:${this.width}, height:${height}")
//            }
//        }

        binding.cameraView.holder.addCallback(object : SurfaceHolder.Callback { // 监听surface创建
            override fun surfaceCreated(holder: SurfaceHolder) {
                BLRTCServerSession.addSurface(holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })
        hideViews.run {
            add(binding.btHome)
            add(binding.btDirection)
            add(binding.btScoreboard)
            add(binding.btMute)
        }
        listOf(
            binding.btHide,
            binding.btHome,
            binding.btScoreboard,
            binding.btDirection,
            binding.btMute,
            binding.btDotMark,
            binding.recordButton
        ).forEach {
            it.setOnClickListener { view ->
                val button = view as Button
                if(it.id != R.id.bt_scoreboard) {
                    button.isSelected = !button.isSelected
                }
                when (it.id) {
                    R.id.bt_home -> finish()
                    R.id.bt_scoreboard -> toggleScoreBoard(it)
                    R.id.bt_direction -> toggleDirection(it.isSelected)
                    R.id.bt_mute -> toggleMute(it.isSelected)
                    R.id.bt_hide -> toggleViews(it.isSelected)
                    R.id.record_button -> toggleRecord(it.isSelected)
                    R.id.bt_dot_mark -> dotMark()
                }
            }
        }

        viewModel.batteryLiveData.observe(this) {
            changePhoneBattery(it)
            binding.tvBattery.text = it.toString()
        }

        viewModel.isShowJoystick.observe(this) {
            binding.composeJoystickContainer.visibility = if (it) View.VISIBLE else View.GONE
        }

        // 监听时间更新
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.elapsedTime.collectLatest { formattedTime ->
                    binding.tvRecordTime.text = formattedTime
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.compose_joystick_container, JoyStickFragment())
            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_score_panel, ScorePanelFragment())
            .commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_score_count, ScoreCountFragment()).commit()
    }

    private fun setFullScreen() {
        window.decorView.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11 (API 30) 及以上
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                // Android 10 (API 29) 及以下
                @Suppress("DEPRECATION")
                systemUiVisibility =
                    (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }

    }

    private fun toggleScoreBoard(view: View) {
        viewModel.colorOfHomeTeam.value?.let {
            BLRTCServerSession.sendScoreboardMessage(
                title = "Scoreboard",
                hide = if (view.isSelected) HIDE_SCORE_BOARD_YES else HIDE_SCORE_BOARD_NO, section = 1, homeName = "江苏苏州", homeColor = 0xcccccc, homeScore = 3, awayName = "上海圣诞节", awayColor = 0xdddddd, awayScore = 77
            )
        }
        with(binding) {
            when(fragmentScoreCount.visibility) {
                View.VISIBLE -> {
                    fragmentScorePanel.visible()
                    fragmentScoreCount.gone()
                }
                else -> {
                    if(fragmentScorePanel.visibility == View.GONE) {
                        fragmentScorePanel.visible()
                        fragmentScoreCount.gone()
                    } else {
                        if(viewModel.awayTeamName.value.isNullOrBlank()
                            && viewModel.homeTeamName.value.isNullOrBlank()
                            && viewModel.gameNameOrEvent.value.isNullOrBlank()
                            && viewModel.colorOfAwayTeam.value == 0
                            && viewModel.colorOfHomeTeam.value == 0) {
                            fragmentScorePanel.gone()
                            fragmentScoreCount.gone()
                        } else {
                            fragmentScoreCount.visible()
                            fragmentScorePanel.gone()
                        }
                    }
                }
            }

            view.isSelected = fragmentScoreCount.visibility == View.VISIBLE ||
                    fragmentScorePanel.visibility == View.VISIBLE
        }
    }

    private fun toggleDirection(showDirection: Boolean) {
        viewModel.toggleJoystick(showDirection)
    }

    private fun toggleMute(mute: Boolean) {
        BLRTCServerSession.sendMuteSwitchMessage(if (mute) MUTE_YES else MUTE_NO)
    }

    private fun toggleViews(hide: Boolean) {
        if (hide) {
            // 隐藏所有视图
            hideViews.forEach { it.visibility = View.INVISIBLE }
        } else {
            // 显示所有视图
            hideViews.forEach { it.visibility = View.VISIBLE }
        }
    }

    private fun dotMark() {
        BLRTCServerSession.sendMarkingMessage()
    }

    private fun toggleRecord(record: Boolean) {
        if (record) {
            // 开始录制
            viewModel.startTimer() // 启动计时器
        } else {
            // 停止录制
            viewModel.stopTimer() // 停止计时
        }
        BLRTCServerSession.sendRecordSwitchMessage(if (record) START_RECORD else STOP_RECORD)
    }

    private fun changePhoneBattery(batteryLevel: Int) {
        val batteryDrawable = if (batteryLevel > 80) {
            R.drawable.ic_battery_100
        } else if (batteryLevel > 60) {
            R.drawable.ic_battery_80
        } else if (batteryLevel > 40) {
            R.drawable.ic_battery_60
        } else if (batteryLevel > 20) {
            R.drawable.ic_battery_40
        } else if (batteryLevel > 5) {
            R.drawable.ic_battery_20
        } else {
            R.drawable.ic_battery_0
        }
        binding.ivBattery.setImageResource(batteryDrawable)
    }

}