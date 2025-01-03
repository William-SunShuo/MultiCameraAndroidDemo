package com.blink.monitor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blink.monitor.databinding.FragmentScoreCountPageBinding

class ScoreCountFragment: BaseViewBindingFragment<FragmentScoreCountPageBinding>() {

    override fun initView(view: View, savedInstanceState: Bundle?) {

    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentScoreCountPageBinding {
        return FragmentScoreCountPageBinding.inflate(inflater)
    }

}