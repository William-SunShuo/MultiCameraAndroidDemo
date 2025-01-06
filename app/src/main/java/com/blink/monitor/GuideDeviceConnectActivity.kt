package com.blink.monitor

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View

import com.blink.monitor.databinding.ActivityGuideConnectBinding
import com.blink.monitor.extention.onClick
import com.blink.monitor.utils.NetworkUtils.openWirelessSettings

class GuideDeviceConnectActivity: BaseBindingActivity<ActivityGuideConnectBinding>() {

    override fun getViewBinding(): ActivityGuideConnectBinding {
        return ActivityGuideConnectBinding.inflate(layoutInflater)
    }

    override fun initView() {

        val spannableString = SpannableString(getString(R.string.jump_network_settings)).apply {
            setSpan(UnderlineSpan(), 0, this.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(Color.parseColor("#198BFF")),
                0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openWirelessSettings()
                }

            },  0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.netTv.text = spannableString
        binding.netTv.movementMethod = LinkMovementMethod.getInstance()

        binding.navigationBack.onClick(false) {
            finish()
        }

    }

}