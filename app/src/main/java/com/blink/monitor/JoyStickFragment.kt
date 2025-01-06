package com.blink.monitor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.blink.monitor.composeable.JoystickView
import com.blink.monitor.extention.JoystickDirection
import com.blink.monitor.viewmodel.MonitorViewModel

class JoyStickFragment : Fragment() {

    private val viewModel: MonitorViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                JoystickView(180f) {
                    Log.d("JoyStick", "${it.name}")
                    when (it) {
                        JoystickDirection.Up -> {
                            viewModel.sendControlMsg(ControlUp)
                        }

                        JoystickDirection.Down -> {
                            viewModel.sendControlMsg(ControlDown)
                        }

                        JoystickDirection.Left -> {
                            viewModel.sendControlMsg(ControlLeft)
                        }

                        JoystickDirection.Right -> {
                            viewModel.sendControlMsg(ControlRight)
                        }

                        JoystickDirection.Release -> {}
                        JoystickDirection.UpTap -> {}
                        JoystickDirection.DownTap -> {}
                        JoystickDirection.LeftTap -> {}
                        JoystickDirection.RightTap -> {}
                    }
                }
            }
        }
    }
}