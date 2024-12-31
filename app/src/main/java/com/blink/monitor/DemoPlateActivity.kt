package com.blink.monitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blink.monitor.databinding.ActivityDemoPlateBinding

class DemoPlateActivity: AppCompatActivity() {

    private lateinit var binding: ActivityDemoPlateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDemoPlateBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}