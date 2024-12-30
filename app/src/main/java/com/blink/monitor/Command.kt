package com.blink.monitor

object Command {
    val ControlUp = 4
    val ControlDown = 5
    val ControlLeft = 6
    val ControlRight = 7
    val ControlStart = 8
    val ControlStop = 9
    val ControlPhoto = 16
    val ControlDot = 17
    val ScoreBoard = 18
    val ZoomInOut = 19


    fun printCommand(command: Int): String {
        return when (command) {
            ControlUp -> "ControlUp"
            ControlDown -> "ControlDown"
            ControlLeft -> "ControlLeft"
            ControlRight -> "ControlRight"
            ControlStart -> "ControlStart"
            ControlStop -> "ControlStop"
            ControlPhoto -> "ControlPhoto"
            ControlDot -> "ControlDot"
            ScoreBoard -> "ScoreBoard"
            ZoomInOut -> "ZoomInOut"
            else -> "Unknown command"
        }
    }
}