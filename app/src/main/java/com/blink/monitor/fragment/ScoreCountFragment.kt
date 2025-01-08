package com.blink.monitor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.blink.monitor.databinding.FragmentScoreCountPageBinding
import com.blink.monitor.viewmodel.MonitorViewModel

class ScoreCountFragment: BaseViewBindingFragment<FragmentScoreCountPageBinding>() {

    private val viewModel: MonitorViewModel by activityViewModels()


    override fun initView(view: View, savedInstanceState: Bundle?) {
        viewModel.awayTeamName.observe(this) {
            binding.scoreView.getAwayTeam().text = it
        }

        viewModel.homeTeamName.observe(this) {
            binding.scoreView.getHomeTeam().text = it
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentScoreCountPageBinding {
        return FragmentScoreCountPageBinding.inflate(inflater)
    }


}