package com.blink.monitor

import android.graphics.Paint.Cap
import androidx.recyclerview.widget.LinearLayoutManager
import com.blink.monitor.adapter.CaptureDeviceAdapter
import com.blink.monitor.bean.CaptureItem
import com.blink.monitor.databinding.ActivityMonitorCaptureDevicesBinding


class MonitorCaptureDevicesActivity: BaseBindingActivity<ActivityMonitorCaptureDevicesBinding>() {


    override fun getViewBinding(): ActivityMonitorCaptureDevicesBinding {
        return ActivityMonitorCaptureDevicesBinding.inflate(layoutInflater)
    }



    override fun initView() {

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = CaptureDeviceAdapter().apply {
                setData(listOf(CaptureItem("iPHone11", "fjdjkfjkajf"), CaptureItem("小米15", "dfdjfkadjf")))
            }
        }

    }
}