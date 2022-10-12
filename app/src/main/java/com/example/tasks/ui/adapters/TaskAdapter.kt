package com.example.tasks.ui.adapters

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.content.ContextCompat
import androidx.core.util.keyIterator
import androidx.core.util.remove
import androidx.core.util.size
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.tasks.R
import com.example.tasks.data.model.Task
import com.example.tasks.databinding.ItemTaskBinding
import com.example.tasks.databinding.ItemTaskGroupHeadingBinding
import com.example.tasks.ui.MainActivity
import com.example.tasks.utils.Constants
import com.example.tasks.utils.getDateDifferenceInDays
import com.example.tasks.utils.getStringFromDate
import com.example.tasks.utils.getTimeFromDate
import com.google.android.material.color.MaterialColors
import com.maltaisn.icondialog.pack.IconPack
import java.util.*
import kotlin.math.absoluteValue

class TaskAdapter(
    private val iconPack: IconPack,
    private val onTaskClickListener: OnTaskClickListener,
) :
    RecyclerView.Adapter<TaskViewHolder>() {
    private val TAG = "TaskAdapter"


    private val differCallback = object : DiffUtil.ItemCallback<TaskAdapterItem>() {
        override fun areItemsTheSame(oldItem: TaskAdapterItem, newItem: TaskAdapterItem): Boolean {
            return when (oldItem) {
                is TaskAdapterItem.TaskItem -> when (newItem) {
                    is TaskAdapterItem.TaskItem -> oldItem.task.id == newItem.task.id
                    is TaskAdapterItem.TaskSeparatorHeading -> false
                }
                is TaskAdapterItem.TaskSeparatorHeading -> when (newItem) {
                    is TaskAdapterItem.TaskItem -> false
                    is TaskAdapterItem.TaskSeparatorHeading -> oldItem.heading == newItem.heading
                }
            }
        }

        override fun areContentsTheSame(
            oldItem: TaskAdapterItem,
            newItem: TaskAdapterItem
        ): Boolean {
            return when (oldItem) {
                is TaskAdapterItem.TaskItem -> when (newItem) {
                    is TaskAdapterItem.TaskItem -> oldItem == newItem
                    is TaskAdapterItem.TaskSeparatorHeading -> false
                }
                is TaskAdapterItem.TaskSeparatorHeading -> when (newItem) {
                    is TaskAdapterItem.TaskItem -> false
                    is TaskAdapterItem.TaskSeparatorHeading -> oldItem == newItem
                }
            }
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)
    private val proxyList = mutableListOf<TaskAdapterItem>()
    private val selectedList = SparseBooleanArray()


    fun updateList(list: List<TaskAdapterItem>) {
        proxyList.clear()
        proxyList.addAll(list)
        differ.submitList(proxyList.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return when (viewType) {
            R.layout.item_task -> TaskViewHolder.Item(
                ItemTaskBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            R.layout.item_task_group_heading -> TaskViewHolder.Separator(
                ItemTaskGroupHeadingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw IllegalArgumentException("Unexpected View type")
        }
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentItem = differ.currentList[position]

        when (holder) {
            is TaskViewHolder.Item -> {
                holder.bind(
                    currentItem as TaskAdapterItem.TaskItem,
                    onTaskClickListener,
                    iconPack,
                    holder
                )
                if (selectedList[position]) {
                    holder.setBackgroundColor(R.color.light_gray)
                } else {
                    holder.setBackgroundColor(R.color.white)
                }

            }
            is TaskViewHolder.Separator -> {
                val heading = (currentItem as TaskAdapterItem.TaskSeparatorHeading).heading
                holder.binding.tvTaskGroupHeading.text = heading

                if (position == 0) {
                    holder.binding.tvTaskGroupHeading.setPadding(0, 2, 0, 4)
                }
            }
        }

    }


    override fun getItemCount(): Int {
        return getListSize()
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is TaskAdapterItem.TaskItem -> R.layout.item_task
            is TaskAdapterItem.TaskSeparatorHeading -> R.layout.item_task_group_heading
        }
    }

    private fun getListSize(): Int {
        return differ.currentList.size
    }

    fun toggleSelectTask(position: Int) {
        if (selectedList.get(position, false)) {
            selectedList.delete(position)
        } else {
            selectedList.put(position, true)
        }
        notifyItemChanged(position)
    }

    fun clearSelection() {
        val keys = mutableListOf<Int>()
        for (key in selectedList.keyIterator()) {
            keys.add(key)
        }
        for (key in keys) {
            selectedList.delete(key)
            notifyItemChanged(key)
        }
    }

    fun getSelectedItemCount() = selectedList.size

    fun getSelectedTasks(): List<Task> {
        val tasks = mutableListOf<Task>()
        for (position in selectedList.keyIterator()) {
            val selectedTask =
                (differ.currentList[position] as TaskAdapterItem.TaskItem).task
            tasks.add(selectedTask)
        }
        return tasks.toList()
    }

    interface OnTaskClickListener {
        fun onTaskCheckBoxClickListener(checkBox: CheckBox, task: Task)
        fun onTaskLayoutClickListener(taskItem: TaskAdapterItem.TaskItem, holder: TaskViewHolder.Item)
        fun onTaskLayoutLongClickListener(task: Task, holder: TaskViewHolder.Item)
    }
}


sealed class TaskViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
    class Separator(val binding: ItemTaskGroupHeadingBinding) : TaskViewHolder(binding)

    class Item(val binding: ItemTaskBinding) : TaskViewHolder(binding) {

        fun setBackgroundColor(colorId: Int) {
            binding.root.apply {
                setCardBackgroundColor(
                    context.getColor(
                        colorId,
                    )
                )
            }
        }


        fun bind(
            item: TaskAdapterItem.TaskItem,
            onTaskClickListener: TaskAdapter.OnTaskClickListener,
            iconPack: IconPack,
            holder: TaskViewHolder
        ) {
            val task = item.task
            val list = item.list
            val priorityColorResource = item.priorityColorResourceId
            val datesDifferenceFromToday = item.datesDifferenceFromToday
            val heading = item.groupHeading
            val taskDateTime = Date(task.dateTime)
            val taskTime = getTimeFromDate(taskDateTime)
            val taskDate = getStringFromDate(taskDateTime)

            binding.apply {
                tvTaskTitle.text = task.title
                tvTaskDescription.text = task.description
                cbTaskCompleted.isChecked = task.isCompleted


                priorityBand.backgroundTintList =
                    root.context.getColorStateList(priorityColorResource)

                tvClock.text = taskTime

                tvTaskListName.text = list.name
                ivTaskListIcon.setImageDrawable((iconPack.getIcon(list.iconId)?.drawable))


                if (heading == Constants.PENDING) {
                    val text = when (datesDifferenceFromToday) {
                        0L -> Constants.TODAY
                        1L -> "Yesterday"
                        else -> "$datesDifferenceFromToday days ago"
                    }
                    tvCalendar.text = text
                }

                if (heading == Constants.UPCOMING) {
                    val text = when (datesDifferenceFromToday.absoluteValue) {
                        0L -> Constants.TODAY
                        1L -> Constants.TOMORROW
                        else -> "In ${datesDifferenceFromToday.absoluteValue} days"
                    }
                    tvCalendar.text = text
                }

                cbTaskCompleted.setOnClickListener {
                    onTaskClickListener.onTaskCheckBoxClickListener(cbTaskCompleted, task)
                }
                root.setOnClickListener {
                    onTaskClickListener.onTaskLayoutClickListener(item, holder as Item)
                }
                root.setOnLongClickListener {
                    onTaskClickListener.onTaskLayoutLongClickListener(task, holder as Item)
                    true
                }

                if (heading == Constants.TODAY || heading == Constants.TOMORROW) {
                    tvCalendar.visibility = View.GONE
                    ivCalendarIcon.visibility = View.GONE
                } else {
                    tvCalendar.visibility = View.VISIBLE
                    ivCalendarIcon.visibility = View.VISIBLE
                }

                if (task.description.isBlank()) {
                    tvTaskDescription.visibility = View.GONE
                } else {
                    tvTaskDescription.visibility = View.VISIBLE
                }

            }
        }
    }
}