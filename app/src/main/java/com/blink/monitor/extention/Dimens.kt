package com.blink.monitor.extention

import android.content.res.Resources
import android.util.TypedValue

/**
 * dp转换工具扩展
 */
val Float.dp: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics)

val Int.dp: Float
    get() = this.toFloat().dp