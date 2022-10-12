package com.example.tasks.ui.fragments

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.SeekBar
import androidx.lifecycle.lifecycleScope
import com.example.tasks.R
import com.example.tasks.TasksApplication
import com.example.tasks.databinding.FragmentIndividualTaskBinding
import com.example.tasks.utils.Constants
import com.example.tasks.utils.Resource
import com.example.tasks.utils.getTimeFromDate
import com.google.android.material.snackbar.Snackbar
import com.maltaisn.icondialog.pack.IconPack
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

class IndividualTaskFragment : BaseFragment(R.layout.fragment_individual_task) {
    private val TAG = "IndividualTaskFragment"
    private var _binding: FragmentIndividualTaskBinding? = null
    private val binding: FragmentIndividualTaskBinding get() = _binding!!

    private lateinit var confirmDeleteDialog: AlertDialog
    private lateinit var priorityDialog: AlertDialog

    private val iconDialogIconPack: IconPack
        get() = (requireActivity().application as TasksApplication).iconPack!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentIndividualTaskBinding.bind(view)

        setUpDialog()
        bindData()
        setUpMenuClickHandler()
        observeOperationStatus()
        setUpOnClickListeners()
    }


    override fun onResume() {
        super.onResume()
        bindData()
    }

    private fun observeOperationStatus() {
        lifecycleScope.launchWhenCreated {
            tasksViewModel.taskOperationStatus.collectLatest { operationStatus ->
                when (operationStatus) {
                    is Resource.Success -> Snackbar.make(
                        mainView,
                        "Task deleted successfully",
                        Snackbar.LENGTH_SHORT
                    ).setAction("Undo") {
                        operationStatus.data?.let {
                            tasksViewModel.insertTask(it)
                        }
                    }.show()
                    is Resource.Error -> Snackbar.make(
                        mainView,
                        "Some Error Occurred",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    is Resource.Loading -> return@collectLatest
                }

            }
        }
    }

    private fun setUpDialog() {
        confirmDeleteDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.confirm_delete)
            setMessage(R.string.delete_pending_message)
            setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteCurrentTask()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
        }.create()
        val task = tasksViewModel.currentTaskItem.task
        val priorities = arrayOf("High", "Medium", "Low")
        val priorityMap = mapOf(
            "High" to Constants.PRIORITY_HIGH,
            "Medium" to Constants.PRIORITY_MID,
            "Low" to Constants.PRIORITY_LOW
        )
        val checked = when (task.priorityId) {
            Constants.PRIORITY_HIGH -> 0
            Constants.PRIORITY_MID -> 1
            else -> 2
        }
        priorityDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle("Set Priority")
//            setMessage(task.title)
            setSingleChoiceItems(priorities, checked) { _, index ->
                priorityMap[priorities[index]]?.let {
                    tasksViewModel.getTaskByIdAndChangePriority(
                        task.id,
                        it
                    ) {
                        bindData()
                    }
                }
            }
            setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
            }
            setNegativeButton("Back") { dialog, _ ->
                dialog.cancel()
            }

        }.create()
    }

    private fun handleDelete() {
        if (tasksViewModel.currentTaskItem.task.isCompleted) {
            deleteCurrentTask()
        } else {
            confirmDeleteDialog.show()
        }
    }

    private fun setUpMenuClickHandler() {
        mainActivity.setHandleMenuOptions { menuItem ->
            when (menuItem.itemId) {
                R.id.miEdit -> navigateToEdit()
                R.id.miDelete -> handleDelete()
                else -> Log.e(TAG, "setUpMenuClickHandler: invalid : ${menuItem.itemId}")
            }
        }
    }

    private fun navigateToEdit() {
        mainActivity.navController.navigate(
            R.id.action_individualTaskFragment_to_editTaskFragment
        )
    }

    private fun deleteCurrentTask() {
        tasksViewModel.deleteTask(tasksViewModel.currentTaskItem.task)
        tasksViewModel.clearCurrentTask()
        mainActivity.navController.popBackStack()
    }

    private fun bindData() {
        val currentTask = tasksViewModel.currentTaskItem.task
        val taskList = tasksViewModel.currentTaskItem.list
        val dateDifference = tasksViewModel.currentTaskItem.datesDifferenceFromToday
        val type = tasksViewModel.currentTaskItem.groupHeading

        binding.apply {
            val priorityImageId = when (currentTask.priorityId) {
                Constants.PRIORITY_HIGH -> R.drawable.red_img
                Constants.PRIORITY_MID -> R.drawable.yellow_img
                else -> R.drawable.green_img
            }
            val bgContainerColorList = when (currentTask.priorityId) {
                Constants.PRIORITY_HIGH -> root.context.getColorStateList(R.color.bg_high_priority)
                Constants.PRIORITY_MID -> root.context.getColorStateList(R.color.bg_mid_priority)
                else -> root.context.getColorStateList(R.color.bg_low_priority)
            }
            placeholderBgView.backgroundTintList = bgContainerColorList
            tvTaskDescription.backgroundTintList = bgContainerColorList

            ivPriority.setImageResource(priorityImageId)

            tvTaskTitle.text = currentTask.title
            cbTaskCompleted.isChecked = currentTask.isCompleted

            ivListIcon.setImageDrawable(iconDialogIconPack.getIcon(taskList.iconId)!!.drawable)
            tvTaskListName.text = taskList.name
            tvDueDate.text = when {
                dateDifference > 0 && type == Constants.PENDING -> {
                    tvDueDate.setTextColor(root.context.getColor(R.color.high_priority))
                    "Past Due"
                }
                dateDifference == 0L -> "Today"
                else -> "In ${dateDifference.absoluteValue} days"
            }
            tvTaskTime.text = getTimeFromDate(Date(currentTask.dateTime))

            Log.e(TAG, "bindData: currentTask progress = ${currentTask.progress}")

            tvTaskDescription.text = currentTask.description.ifBlank {
                tvTaskDescription.setTextColor(
                    root.context.getColor(R.color.dark_gray)
                )
                "No Description"
            }

            val progress = currentTask.progress ?: 0
            ObjectAnimator.ofInt(seekBar, "progress", progress).apply {
                duration = 200
                interpolator = DecelerateInterpolator()
                start()
            }
            val progressString = "$progress %"
            tvProgressText.text = progressString
            seekBar.isEnabled = !currentTask.isCompleted
        }
    }

    private fun setUpOnClickListeners() {
        val changePriorityBtn = binding.btnChangePriority

        binding.cbTaskCompleted.setOnClickListener {
            val task = tasksViewModel.currentTaskItem.task
            task.isCompleted = !task.isCompleted
            binding.cbTaskCompleted.isChecked = task.isCompleted
            tasksViewModel.toggleMarkAsComplete(task) {
                bindData()
            }
        }

        changePriorityBtn.setOnClickListener {
            priorityDialog.show()
        }
        var updateProgressJob: Job? = null

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, value: Int, p2: Boolean) {
                val text = "$value %"
                binding.tvProgressText.text = text
                updateProgressJob?.cancel()
                updateProgressJob = MainScope().launch {
                    delay(Constants.SEEKBAR_DELAY)
                    tasksViewModel.changeTaskProgress(
                        tasksViewModel.currentTaskItem.task.id, value
                    ) {
                        bindData()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

    }

}