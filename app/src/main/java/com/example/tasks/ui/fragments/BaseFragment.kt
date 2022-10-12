package com.example.tasks.ui.fragments

import android.app.AlarmManager
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.example.tasks.ui.MainActivity
import com.example.tasks.viewmodel.TasksViewModel

open class BaseFragment(id: Int) : Fragment(id) {
    private var _tasksViewModel: TasksViewModel? = null
    protected val tasksViewModel: TasksViewModel get() = _tasksViewModel!!
    protected val mainActivity: MainActivity get() = requireActivity() as MainActivity
    protected val mainView get() = (requireActivity() as MainActivity).getMainView()
    protected val alarmManager get() = run {
        val alarm = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _tasksViewModel = (activity as MainActivity).tasksViewModel
    }
}