package com.blink.monitor

const val ControlUp = 4
const val ControlDown = 5
const val ControlLeft = 6
const val ControlRight = 7
const val ControlStart = 8
const val ControlStop = 9
const val ControlPhoto = 16
const val ControlDot = 17
const val ScoreBoard = 18
const val ZoomInOut = 19


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