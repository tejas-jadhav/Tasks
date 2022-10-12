package com.example.tasks.ui.adapters

import android.graphics.drawable.Drawable
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.util.keyIterator
import androidx.core.util.size
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.tasks.R
import com.example.tasks.data.model.TaskList
import com.example.tasks.databinding.ItemAddListActionBinding
import com.example.tasks.databinding.ItemAddListItemBinding
import com.maltaisn.icondialog.pack.IconPack
import java.lang.IllegalArgumentException

class TaskListAdapter(
    private val onClickListener: OnAddTaskListItemClick,
    private val iconPack: IconPack
) :
    RecyclerView.Adapter<TaskListViewHolder>() {
    private val TAG = "TaskListAdapter"
    var totalTasks = 0

    private val differCallback = object : DiffUtil.ItemCallback<TaskList>() {
        override fun areItemsTheSame(oldItem: TaskList, newItem: TaskList): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskList, newItem: TaskList): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, differCallback)
    private val selectedItems = SparseBooleanArray()




    fun updateList(list: List<TaskList>) {
        val tempList = mutableListOf<TaskList>()
        val dummyTaskList = TaskList(-1, "dummy", 1, totalTasks)
        tempList.add(dummyTaskList)
        tempList.addAll(list)
        tempList.add(dummyTaskList)
        Log.e(TAG, "updateList: submitting list of size ${tempList.size} (includes front and back)")

        differ.submitList(tempList.toList())
    }




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        return when (viewType) {
            R.layout.item_add_list_action -> TaskListViewHolder.TaskListActionViewHolder(
                ItemAddListActionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
            R.layout.item_add_list_item -> TaskListViewHolder.TaskListItemViewHolder(
                ItemAddListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw IllegalArgumentException("Unexpected View type")
        }
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        when (holder) {
            is TaskListViewHolder.TaskListActionViewHolder -> {
                if (position == 0) {
                    val itemCountString = when (totalTasks) {
                        0 -> "No tasks"
                        1 -> "1 task"
                        else -> "$totalTasks tasks"
                    }
                    holder.bindActionAllTask(itemCountString) {
                        onClickListener.onAllTaskClickListener()
                    }
                } else if (position == differ.currentList.size - 1) {
                    holder.bindAddTask { onClickListener.onAddListClickListener() }
                }
            }
            is TaskListViewHolder.TaskListItemViewHolder -> {
                val currentItem = differ.currentList[position]
                val icon = iconPack.getIcon(currentItem.iconId)?.drawable
                val itemCount = when (currentItem.items) {
                    0 -> "No tasks"
                    1 -> "1 task"
                    else -> "${currentItem.items} tasks"
                }

                val onClick = {
                    onClickListener.onTaskListItemClickListener(currentItem, holder)
                }
                val onLongClick = {
                    onClickListener.onTaskListItemLongClickListener(currentItem, holder)
                }

                holder.bind(currentItem, itemCount, icon, onClick, onLongClick)

                if (selectedItems[position]) {
                    holder.setBackgroundColor(R.color.light_gray)
                } else {
                    holder.setBackgroundColor(R.color.white)
                }

            }
        }
    }


    override fun getItemCount(): Int {
        return differ.currentList.size
    }
    operator fun get(index: Int): TaskList {
        return differ.currentList[index]
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0, differ.currentList.size - 1 -> R.layout.item_add_list_action
            else -> R.layout.item_add_list_item
        }
    }

    //    selection logic
    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }

    fun getSelectedItemCount() = selectedItems.size

    fun getSelectedTaskList(): List<TaskList> {
        val list = mutableListOf<TaskList>()
        for (position in selectedItems.keyIterator()) {
            list.add(
                differ.currentList[position]
            )
        }

        return list.toList()
    }
    fun getListOfSelectedIndices(): List<Int> {
        val list = mutableListOf<Int>()
        for (key in selectedItems.keyIterator()) {
            list.add(key)
        }
        return list.toList()
    }

    fun clearSelection() {
        val selectedPosition = mutableListOf<Int>()
        for (position in selectedItems.keyIterator()) {
            selectedPosition.add(position)
        }
        for (position in selectedPosition) {
            selectedItems.delete(position)
            notifyItemChanged(position)
        }
    }



    interface OnAddTaskListItemClick {
        fun onAllTaskClickListener()
        fun onAddListClickListener()
        fun onTaskListItemClickListener(
            taskList: TaskList,
            holder: TaskListViewHolder.TaskListItemViewHolder
        )

        fun onTaskListItemLongClickListener(
            taskList: TaskList,
            holder: TaskListViewHolder.TaskListItemViewHolder
        )
    }
}

sealed class TaskListViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {
    class TaskListItemViewHolder(val binding: ItemAddListItemBinding) :
        TaskListViewHolder(binding) {

        fun bind(
            currentItem: TaskList,
            subtitle: String,
            icon: Drawable?,
            onClick: () -> Unit,
            onLongClick: () -> Unit
        ) {
            binding.tvAddTaskTitle.text = currentItem.name
            binding.tvAddTaskSubTitle.text = subtitle
            icon?.let {
                binding.ivListIconItem.setImageDrawable(it)
            }
                ?: binding.ivListIconItem.setImageResource(R.drawable.ic_baseline_checklist_24)
            binding.root.setOnClickListener {
                onClick()
            }
            binding.root.setOnLongClickListener {
                onLongClick()
                true
            }
        }

        fun setBackgroundColor(@ColorRes colorId: Int) {
            binding.root.apply {
                setCardBackgroundColor(
                    context.getColor(
                        colorId,
                    )
                )
            }
        }

    }

    class TaskListActionViewHolder(val binding: ItemAddListActionBinding) :
        TaskListViewHolder(binding) {
        fun bindActionAllTask(
            subtitle: String,
            onClick: () -> Unit
        ) {
            binding.tvAddTaskTitle.setText(R.string.all_tasks)
            binding.tvAddTaskSubTitle.text = subtitle
            binding.ivListIconAction.setImageResource(R.drawable.ic_baseline_check_24)
            binding.root.setOnClickListener {
                onClick()
            }
        }

        fun bindAddTask(
            onClick: () -> Unit
        ) {
            binding.tvAddTaskTitle.setText(R.string.add_list)
            binding.tvAddTaskSubTitle.setText(R.string.create_new_list)
            binding.ivListIconAction.setImageResource(R.drawable.ic_baseline_add_24)
            binding.root.setOnClickListener {
                onClick()
            }
        }
    }
}

