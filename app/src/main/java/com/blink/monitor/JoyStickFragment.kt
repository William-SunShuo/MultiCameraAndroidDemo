package com.blink.monitor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.blink.monitor.composeable.JoystickView
import com.blink.monitor.extention.JoystickDirection

class JoyStickFragment : Fragment() {

    private var lastDirectionType = JoystickDirection.None

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                JoystickView(180f) {
                    when (it) {
                        JoystickDirection.Up -> {
                            if (lastDirectionType != JoystickDirection.Up) {
                                lastDirectionType = JoystickDirection.Up
                                BLRTCServerSession.sendRemoteCtrlMessage(
                                    CONTROL_UP, OPERATION_LONG_CLICK
                                )
                            }
                        }

                        JoystickDirection.Down -> {
                            if (lastDirectionType != JoystickDirection.Down) {
                                lastDirectionType = JoystickDirection.Down
                                BLRTCServerSession.sendRemoteCtrlMessage(
                                    CONTROL_DOWN, OPERATION_LONG_CLICK
                                )
                            }
                        }

                        JoystickDirection.Left -> {
                            if (lastDirectionType != JoystickDirection.Left) {
                                lastDirectionType = JoystickDirection.Left
                                BLRTCServerSession.sendRemoteCtrlMessage(
                                    CONTROL_LEFT, OPERATION_LONG_CLICK
                                )
                            }
                        }

                        JoystickDirection.Right -> {
                            if (lastDirectionType != JoystickDirection.Right) {
                                lastDirectionType = JoystickDirection.Right
                                BLRTCServerSession.sendRemoteCtrlMessage(
                                    CONTROL_RIGHT, OPERATION_LONG_CLICK
                                )
                            }
                        }

                        JoystickDirection.Release -> {
                            lastDirectionType = JoystickDirection.None
                            BLRTCServerSession.sendRemoteCtrlMessage(
                                CONTROL_INVALID, OPERATION__RELEASE
                            )
                        }

                        JoystickDirection.UpTap -> {
                            BLRTCServerSession.sendRemoteCtrlMessage(
                                CONTROL_UP, OPERATION_TAP
                            )
                        }

                        JoystickDirection.DownTap -> {
                            BLRTCServerSession.sendRemoteCtrlMessage(
                                CONTROL_DOWN, OPERATION_TAP
                            )
                        }

                        JoystickDirection.LeftTap -> {
                            BLRTCServerSession.sendRemoteCtrlMessage(
                                CONTROL_LEFT, OPERATION_TAP
                            )
                        }

                        JoystickDirection.RightTap -> {
                            BLRTCServerSession.sendRemoteCtrlMessage(
                                CONTROL_RIGHT, OPERATION_TAP
                            )
                        }
                        JoystickDirection.None -> {}
                    }
                }
            }
        }
    }
}