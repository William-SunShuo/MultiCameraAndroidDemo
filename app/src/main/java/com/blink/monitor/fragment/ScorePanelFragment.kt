package com.blink.monitor.fragment

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.blink.monitor.R
import com.blink.monitor.databinding.WindowScoreBoardBinding
import com.blink.monitor.extention.onClick
import com.blink.monitor.viewmodel.MonitorViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator

class ScorePanelFragment: BaseViewBindingFragment<WindowScoreBoardBinding>() {


    private val viewModel: MonitorViewModel by lazy {
        ViewModelProvider(this)[MonitorViewModel::class.java]
    }


    override fun initView(view: View, savedInstanceState: Bundle?) {

        activity?.also {
            binding.vpTeam.adapter = TeamPageAdapter(it)
            TabLayoutMediator(binding.tabTeam, binding.vpTeam) { tab, position ->
                tab.setCustomView(getTabCustomView(context, position))
            }.attach()
        }

        binding.tabTeam.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.also {
                    if(it.customView != null) {
                        val teamTv = it.customView?.findViewById<TextView>(R.id.tv_content)
                        teamTv?.typeface = Typeface.DEFAULT_BOLD
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.also {
                    if(it.customView != null) {
                        val teamTv = it.customView?.findViewById<TextView>(R.id.tv_content)
                        teamTv?.typeface = Typeface.DEFAULT
                    }
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        binding.ivGame.onClick {
            viewModel.gameNameOrEvent.value = binding.tvEventGame.text.toString()
        }

        binding.colorPlate.apply {
            colorAction = { color ->
                when(binding.vpTeam.currentItem) {
                    0 -> viewModel.colorOfHomeTeam.value = color
                    1 -> viewModel.colorOfAwayTeam.value = color
                }
            }
        }

//        viewModel

        binding.checkScore.setOnClickListener {

        }

    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): WindowScoreBoardBinding {
        return WindowScoreBoardBinding.inflate(inflater)
    }


    private fun getTabCustomView(context: Context?, position: Int): View {
        val tabView = LayoutInflater.from(context).inflate(R.layout.tab_custom_view, null);
        //设置相关显示 tabView
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        tabView.layoutParams = layoutParams
        val tvContent = tabView.findViewById<TextView>(R.id.tv_content)
        tvContent.text = when (position) {
            0 -> "Home Team"
            else -> "Away Team"
        }
        return tabView

    }
}