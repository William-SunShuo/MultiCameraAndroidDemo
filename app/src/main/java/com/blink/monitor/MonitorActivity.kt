package com.blink.monitor
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.blink.monitor.databinding.ActivityMonitorBinding
import com.blink.monitor.viewmodel.MonitorViewModel

class MonitorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitorBinding

    private val viewModel: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}