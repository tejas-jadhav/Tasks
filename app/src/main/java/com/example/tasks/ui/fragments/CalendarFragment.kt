package com.example.tasks.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.tasks.R
import com.example.tasks.databinding.FragmentCalendarBinding
import com.example.tasks.databinding.FragmentTaskBinding

class CalendarFragment: BaseFragment(R.layout.fragment_calendar){
    private lateinit var mBinding: FragmentCalendarBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding = FragmentCalendarBinding.bind(view)
    }

}