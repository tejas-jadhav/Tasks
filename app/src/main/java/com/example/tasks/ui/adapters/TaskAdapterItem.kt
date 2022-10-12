package com.example.tasks.ui.adapters

import com.example.tasks.data.model.Task
import com.example.tasks.data.model.TaskList

sealed class TaskAdapterItem {
    class TaskSeparatorHeading(val heading: String) : TaskAdapterItem()
    class TaskItem(
        val task: Task,
        val list: TaskList,
        val datesDifferenceFromToday: Long,
        val priorityColorResourceId: Int,
        val groupHeading: String
    ) : TaskAdapterItem()
}
