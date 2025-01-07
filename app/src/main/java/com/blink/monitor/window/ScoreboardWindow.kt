package com.blink.monitor.window

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.blink.monitor.R
import com.blink.monitor.databinding.WindowScoreBoardBinding
import com.blink.monitor.fragment.TeamPageAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayoutMediator


/**
 * 比分弹窗页，单独控制管理；
 * //todo,后续考虑替换成fragment.
 */
@SuppressLint("InflateParams", "ClickableViewAccessibility")
class ScoreboardWindow(context: Context) : PopupWindow(
    LayoutInflater.from(context).inflate(R.layout.window_score_board, null),
    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
) {

    private var binding: WindowScoreBoardBinding

    init {
//        isOutsideTouchable = true
        isFocusable = true
        isOutsideTouchable = false

        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        animationStyle = R.style.Popup_Anim
        binding = WindowScoreBoardBinding.bind(contentView)

        val activity = context as? FragmentActivity
        activity?.also {
            binding.vpTeam.adapter = TeamPageAdapter(it)
        }

        binding.vpTeam.isUserInputEnabled = false

        TabLayoutMediator(binding.tabTeam, binding.vpTeam) { tab, position ->
            tab.setCustomView(getTabCustomView(context, position))
        }.attach()


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
    }


    private fun getTabCustomView(context: Context, position: Int): View {
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

