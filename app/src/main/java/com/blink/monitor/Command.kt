package com.blink.monitor

const val CONTROL_UP = 0
const val CONTROL_DOWN = 1
const val CONTROL_LEFT = 2
const val CONTROL_RIGHT = 3
const val CONTROL_RETURN = 4
const val CONTROL_INVALID = 100

const val OPERATION_TAP = 0
const val OPERATION_LONG_CLICK = 1
const val OPERATION__RELEASE = 2

const val STOP_RECORD = 0
const val START_RECORD = 1

const val MUTE_YES = 0
const val MUTE_NO = 1

const val CAPTURE_NO = 0
const val CAPTURE_YES = 1

const val CONNECT_REMOTE_NO = 0
const val CONNECT_REMOTE_YES = 1

const val HIDE_SCORE_BOARD_NO = 0
const val HIDE_SCORE_BOARD_YES = 1

const val TOPIC_MARKING = "MA"
const val TOPIC_RECORD_SWITCH = "MB"
const val TOPIC_MUTE_SWITCH = "MC"
const val TOPIC_PHONE_POWER = "MD"
const val TOPIC_REMOTE_INFO_STATE = "ME"
const val TOPIC_REMOTE_CTRL_STATE = "MF"
const val TOPIC_SYNCHRONIZE_SWITCH = "MG"
const val TOPIC_CAPTURED_SWITCH = "MH"
const val TOPIC_SCOREBOARD_INFO = "MI"

const val KEY_TOPIC = "topic"
const val KEY_PHONE_POWER = "phonePower"
const val KEY_REMOTE_CONNECT = "remoteConnect"
const val KEY_REMOTE_POWER = "remotePower"
const val KEY_IS_CAPTURED = "isCaptured"
const val KEY_IS_MUTED = "isMuted"
const val KEY_IS_RECORDING = "isRecording"
const val KEY_OPERATION = "operation"
const val KEY_DIRECTION = "direction"
const val KEY_SYNC = "sync"
const val KEY_TITLE = "title"
const val KEY_HOME_NAME = "homeName"
const val KEY_AWAY_NAME = "awayName"
const val KEY_SECTION = "section"
const val KEY_HIDE_SCORE_BOARD = "hideScoreBoard"
const val KEY_HOME_SCORE = "homeScore"
const val KEY_AWAY_SCORE = "awayScore"
const val KEY_HOME_COLOR = "homeColor"
const val KEY_AWAY_COLOR = "awayColor"