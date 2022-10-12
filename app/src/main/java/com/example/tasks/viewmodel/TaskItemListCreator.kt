package com.example.tasks.viewmodel

import android.util.Log
import com.example.tasks.R
import com.example.tasks.data.model.Task
import com.example.tasks.data.repository.TasksRepository
import com.example.tasks.ui.adapters.TaskAdapterItem
import com.example.tasks.utils.Constants
import com.example.tasks.utils.getDateDifferenceInDays
import com.example.tasks.utils.isSameDay
import java.util.*

class TaskItemListCreator {
    private var isPendingAdded = false
    private var isTodayAdded = false
    private var isTomorrowAdded = false
    private var isUpcomingAdded = false
    private var heading = ""

    private val taskAdapterList = mutableListOf<TaskAdapterItem>()
    private var today = Date()
    private var tomorrow = getTomorrow()

    companion object {
        private var INSTANCE: TaskItemListCreator? = null
        const val TAG = "TaskItemListCreator"

        fun getInstance(): TaskItemListCreator = INSTANCE ?: synchronized(this) {
            INSTANCE ?: TaskItemListCreator().also { INSTANCE = it }
        }
    }

    private fun getTomorrow(): Date {
        return Calendar.getInstance().apply {
            time = today
            add(Calendar.DATE, 1)
        }.time
    }


    private fun resetValues() {
        isPendingAdded = false
        isTodayAdded = false
        isTomorrowAdded = false
        isUpcomingAdded = false
        heading = ""

        today = Date()
        tomorrow = getTomorrow()
        taskAdapterList.clear()
    }

    private fun isPending(taskDate: Date) = getDateDifferenceInDays(
        taskDate,
        today
    ) > 0 && !isPendingAdded

    private fun handlePendingTask() {
        Log.d(TAG, "getTaskAdapterItemList: Pending Tasks")
        taskAdapterList.add(
            TaskAdapterItem.TaskSeparatorHeading(Constants.PENDING)
        )
        heading = Constants.PENDING
        isPendingAdded = true
    }

    private fun isToday(taskDate: Date) = isSameDay(today, taskDate) && !isTodayAdded

    private fun handleTodayTask() {
        Log.d(TAG, "getTaskAdapterItemList: This tasks are Today")
        taskAdapterList.add(
            TaskAdapterItem.TaskSeparatorHeading(Constants.TODAY)
        )
        heading = Constants.TODAY
        isTodayAdded = true
    }

    private fun isTomorrow(taskDate: Date) = isSameDay(tomorrow, taskDate) && !isTomorrowAdded

    private fun handleTomorrowTask() {
        Log.d(TAG, "getTaskAdapterItemList: This Tasks are tomorrow")
        taskAdapterList.add(
            TaskAdapterItem.TaskSeparatorHeading(Constants.TOMORROW)
        )
        heading = Constants.TOMORROW
        isTomorrowAdded = true
    }

    private fun isUpcoming(taskDate: Date) = taskDate.after(tomorrow) && !isUpcomingAdded

    private fun handleUpcomingTask() {
        Log.d(TAG, "getTaskAdapterItemList: This are upcoming tasks")
        taskAdapterList.add(
            TaskAdapterItem.TaskSeparatorHeading(Constants.UPCOMING)
        )
        heading = Constants.UPCOMING
        isUpcomingAdded = true
    }

    private fun getPriorityColor(priority: Int) = when (priority) {
        Constants.PRIORITY_HIGH -> R.color.high_priority
        Constants.PRIORITY_MID -> R.color.mid_priority
        Constants.PRIORITY_LOW -> R.color.low_priority
        else -> R.color.low_priority
    }

    private fun calculateDateDifferenceFromNow(date: Date) = getDateDifferenceInDays(
        date,
        Date()
    )


    suspend fun getTaskAdapterItemList(
        tasks: List<Task>,
        repository: TasksRepository
    ): List<TaskAdapterItem> {
        resetValues()

        for (task in tasks) {
            val taskAdapterItem = convert(task, repository, false)
            taskAdapterList.add(
                taskAdapterItem
            )
        }

        return taskAdapterList.toList()
    }

    suspend fun convert(
        task: Task,
        repository: TasksRepository,
        resetValues: Boolean = true
    ): TaskAdapterItem.TaskItem {
        if (resetValues) resetValues()

        val taskDate = Date(task.dateTime)
        when {
            isPending(taskDate) -> handlePendingTask()
            isToday(taskDate) -> handleTodayTask()
            isTomorrow(taskDate) -> handleTomorrowTask()
            isUpcoming(taskDate) -> handleUpcomingTask()
        }
        val taskList = repository.getTaskListById(task.listId)
        val priorityColorResource = getPriorityColor(task.priorityId)

        val datesDiff = calculateDateDifferenceFromNow(taskDate)

        return TaskAdapterItem.TaskItem(task, taskList, datesDiff, priorityColorResource, heading)
    }


}