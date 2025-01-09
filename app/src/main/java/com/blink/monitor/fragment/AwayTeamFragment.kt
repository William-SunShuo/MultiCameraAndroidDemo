package com.blink.monitor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.blink.monitor.R
import com.blink.monitor.extention.onClick
import com.blink.monitor.extention.onClickSpring
import com.blink.monitor.viewmodel.MonitorViewModel

class AwayTeamFragment: Fragment() {

    private val viewModel: MonitorViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = LayoutInflater.from(context).inflate(R.layout.fragment_team_container, container, false)
        initView(view)
        return view
    }

    private fun initView(view: View) {

        val etContent = view.findViewById<TextView>(R.id.et_event)
        val ivDone = view.findViewById<View>(R.id.iv_event)

        ivDone.onClickSpring {
            viewModel.awayTeamName.value = etContent.text.toString()
        }
    }
}