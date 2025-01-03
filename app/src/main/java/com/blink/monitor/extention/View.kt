package com.blink.monitor.extention

import android.os.SystemClock
import android.view.View
import com.blink.monitor.R

/**
 * 防快速连点
 */
fun View.onClick(unDebounce: Boolean = true, block: (View) -> Unit) {
    if (unDebounce) {
        setOnClickListener(block)
    } else {
        setOnClickListener {
            it.takeIf { v -> !v.isFastClick() }?.also(block)
        }
    }
}

fun View.isFastClick(): Boolean = SystemClock.elapsedRealtime()
    .let {
        val valid = it - this.triggerTime > 500L
        this.triggerTime = it
        return@let !valid
    }


var <T: View> T.triggerTime: Long
    get() = getTag(R.id.view_click_trigger) as? Long ?: 0L
    set(value) = setTag(R.id.view_click_trigger, value)


fun View.gone() {
    when(visibility) {
        View.VISIBLE, View.INVISIBLE -> visibility = View.GONE
        else -> {}
    }
}

fun View.visible() {
    when(visibility) {
        View.VISIBLE -> {}
        else -> visibility = View.VISIBLE
    }
}