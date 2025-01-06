package com.blink.monitor.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings



object NetworkUtils {

    fun Context.openWirelessSettings() {
        startActivity(
            Intent(Settings.ACTION_WIFI_SETTINGS)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}