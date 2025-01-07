package com.blink.monitor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.blink.monitor.databinding.FragmentScoreCountPageBinding
import com.blink.monitor.viewmodel.MonitorViewModel

class ScoreCountFragment: BaseViewBindingFragment<FragmentScoreCountPageBinding>() {

    private val viewModel: MonitorViewModel by lazy {
        ViewModelProvider(this)[MonitorViewModel::class.java]
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        binding.scoreView.getHomeTeam().text =  viewModel.awayTeamName.value
        binding.scoreView.getAwayTeam().text = viewModel.awayTeamName.value

    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentScoreCountPageBinding {
        return FragmentScoreCountPageBinding.inflate(inflater)
    }

}