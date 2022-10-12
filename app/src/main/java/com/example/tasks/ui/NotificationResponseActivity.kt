package com.example.tasks.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tasks.R
import com.example.tasks.TasksApplication
import com.example.tasks.data.db.TasksDatabase
import com.example.tasks.data.model.Task
import com.example.tasks.data.repository.TasksRepository
import com.example.tasks.databinding.ActivityNotificationResponseBinding
import com.example.tasks.databinding.FragmentIndividualTaskBinding
import com.example.tasks.ui.adapters.TaskAdapterItem
import com.example.tasks.utils.Constants
import com.example.tasks.utils.getTimeFromDate
import com.example.tasks.viewmodel.TaskItemListCreator
import com.maltaisn.icondialog.pack.IconPack
import java.util.*
import kotlin.math.absoluteValue

class NotificationResponseActivity : AppCompatActivity() {

    private val TAG = "NotificationResponseActivity"

    private var _binding: FragmentIndividualTaskBinding? = null
    private val binding: FragmentIndividualTaskBinding get() = _binding!!
    private var _activityBinding: ActivityNotificationResponseBinding? = null
    private lateinit var repository: TasksRepository

    private val iconDialogIconPack: IconPack
        get() = (application as TasksApplication).iconPack!!

    private lateinit var task: Task

    companion object {
        const val TASK_KEY = "task"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _activityBinding = ActivityNotificationResponseBinding.inflate(layoutInflater)
        _binding = _activityBinding?.contentOfActivity!!
        setContentView(_activityBinding!!.root)

        task = intent.getSerializableExtra(TASK_KEY) as Task
        repository = TasksRepository(TasksDatabase.getDatabase(this))
        lifecycleScope.launchWhenCreated {
            val item = TaskItemListCreator.getInstance().convert(task, repository)
            bindData(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        return true
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun bindData(taskItem: TaskAdapterItem.TaskItem) {
        val currentTask = taskItem.task
        val taskList = taskItem.list
        val dateDifference = taskItem.datesDifferenceFromToday
        val type = taskItem.groupHeading

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

}