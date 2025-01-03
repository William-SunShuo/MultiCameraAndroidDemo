package com.blink.monitor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.blink.monitor.composeable.JoystickView
import com.blink.monitor.extention.JoystickDirection

class JoyStickFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                JoystickView(180f) {
                    Log.d("JoyStick", "${it.name}")
                    when (it) {
                        JoystickDirection.Up -> {}
                        JoystickDirection.Down -> {}
                        JoystickDirection.Left -> {}
                        JoystickDirection.Right -> {}
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