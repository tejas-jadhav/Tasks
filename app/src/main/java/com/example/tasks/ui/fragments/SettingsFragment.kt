package com.example.tasks.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.tasks.R
import com.example.tasks.databinding.FragmentSettingsBinding
import com.example.tasks.databinding.FragmentTaskBinding

class SettingsFragment: BaseFragment(R.layout.fragment_settings){
    private lateinit var mBinding: FragmentSettingsBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding = FragmentSettingsBinding.bind(view)
    }

}