package com.blink.monitor.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blink.monitor.R

class HomeTeamFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = LayoutInflater.from(context).inflate(R.layout.fragment_team_container, container, false)
        return view
    }
}