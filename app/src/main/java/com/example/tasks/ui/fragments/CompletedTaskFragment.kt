package com.example.tasks.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import androidx.annotation.StringRes
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasks.R
import com.example.tasks.TasksApplication
import com.example.tasks.data.model.Task
import com.example.tasks.databinding.FragmentTaskBinding
import com.example.tasks.ui.adapters.TaskAdapter
import com.example.tasks.ui.adapters.TaskAdapterItem
import com.example.tasks.ui.adapters.TaskViewHolder
import com.example.tasks.utils.Resource
import com.google.android.material.snackbar.Snackbar
import com.maltaisn.icondialog.pack.IconPack

class CompletedTaskFragment : BaseFragment(R.layout.fragment_task),
    TaskAdapter.OnTaskClickListener {

    private var _binding: FragmentTaskBinding? = null
    private val binding: FragmentTaskBinding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private val iconDialogIconPack: IconPack
        get() = (requireActivity().application as TasksApplication).iconPack!!


    private lateinit var deleteAllCompletedDialog: AlertDialog

    private lateinit var deleteSelectedDialog: AlertDialog

    //    entry point
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskBinding.bind(view)

        initializeLayout()
        initializeDialogs()
        observeCompletedTasks()
        setOnClickListeners()

        mainActivity.setHandleMenuOptions {
            if (it.itemId == R.id.miDelete) {
                if (taskAdapter.itemCount == 0) {
                    getSnackbarWithMessage("You're all clear !").show()
                    return@setHandleMenuOptions
                }
                deleteAllCompletedDialog.show()
            }
        }
    }

    private fun getSnackbarWithMessage(s: String): Snackbar =
        Snackbar.make(binding.root, s, Snackbar.LENGTH_SHORT)


    private fun initializeLayout() {
        binding.root.setBackgroundResource(R.color.white)
        binding.fabTasks.visibility = View.GONE
        binding.fabAnimationDummy.visibility = View.GONE
        binding.tvCompletedCount.visibility = View.GONE

        binding.lottieAnimationView.setAnimation(R.raw.no_tasks)
        binding.tvAddNotesHelper.setText(R.string.completed_task_helper_msg)

        taskAdapter = TaskAdapter(iconDialogIconPack, this)
        binding.rvMainTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun initializeDialogs() {
        deleteAllCompletedDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle("Confirm Action")
            setMessage("All completed tasks will be deleted")
            setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteAllCompleted()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
        }.create()

        deleteSelectedDialog = AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete Tasks ?")
            setMessage("All selected tasks will be deleted. Proceed Action ?")
            setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteSelectedTasks()
                mainActivity.closeSupportActionMode()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
        }.create()

    }

    private fun observeCompletedTasks() =
        tasksViewModel.taskItemCompletedList.observe(viewLifecycleOwner) { completedTasksResource ->
            when (completedTasksResource) {
                is Resource.Success -> {
                    val tasks = completedTasksResource.data!!
                    if (tasks.isEmpty()) showPlaceholderView(true)
                    else showPlaceholderView(false)

                    taskAdapter.updateList(tasks)
                }
                is Resource.Error -> {
                    showPlaceholderView(true)
                    setHelperTextMessage(R.string.error)
                }
                is Resource.Loading -> {
                    showPlaceholderView(false)
                    setHelperTextMessage(R.string.loading)
                }
            }
        }

    private fun showPlaceholderView(show: Boolean) {
        if (!show) {
            binding.lottieAnimationView.visibility = View.GONE
            binding.tvAddNotesHelper.visibility = View.GONE
        } else {
            binding.lottieAnimationView.visibility = View.VISIBLE
            binding.tvAddNotesHelper.visibility = View.VISIBLE
            binding.tvAddNotesHelper.setText(R.string.completed_task_helper_msg)
        }
    }

    private fun setHelperTextMessage(@StringRes stringId: Int) {
        binding.tvAddNotesHelper.setText(stringId)
    }

    private fun setHelperTextMessage(text: String) {
        binding.tvAddNotesHelper.text = text
    }

    //    task item events
    override fun onTaskCheckBoxClickListener(checkBox: CheckBox, task: Task) {
        if (mainActivity.isActionModeEnabled) {
            checkBox.isChecked = !checkBox.isChecked
            return
        }
        task.isCompleted = !task.isCompleted
        checkBox.isChecked = task.isCompleted
        tasksViewModel.toggleMarkAsComplete(task)
    }


    override fun onTaskLayoutClickListener(
        taskItem: TaskAdapterItem.TaskItem,
        holder: TaskViewHolder.Item
    ) {
        if (mainActivity.isActionModeEnabled) {
            toggleSelection(holder.adapterPosition)
            return
        }

        tasksViewModel.setCurrentTask(taskItem)
        mainActivity.navController.navigate(
            R.id.action_completedTaskFragment_to_individualTaskFragment
        )
    }

    override fun onTaskLayoutLongClickListener(task: Task, holder: TaskViewHolder.Item) {
        if (!mainActivity.isActionModeEnabled) {
            mainActivity.openSupportActionMode(actionModeCallback)
            toggleSelection(holder.adapterPosition)
        }
    }

    //    action mode callback
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mainActivity._isActionModeEnabled = true
            mode?.menuInflater?.inflate(R.menu.action_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean =
            when (item?.itemId) {
                R.id.miDelete -> {
                    if (mainActivity.isActionModeEnabled) {
                        deleteSelectedDialog.show()
                    }
                    true
                }

                else -> false
            }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mainActivity._isActionModeEnabled = false
            taskAdapter.clearSelection()

        }
    }

    //    task view and sort event
    private fun setOnClickListeners() {
//        TODO("Not yet implemented")
    }

    //    task actions
    private fun deleteSelectedTasks() {
        val selectedTasks = taskAdapter.getSelectedTasks()
        if (selectedTasks.isEmpty()) {
            return
        }
        tasksViewModel.deleteListOfTask(selectedTasks)
        val message = when (selectedTasks.size) {
            1 -> "Deleted Task"
            else -> "Deleted ${selectedTasks.size} tasks"
        }
        getSnackbarWithMessage(message).apply {
            setAction("Undo") {
                tasksViewModel.insertListOfTask(selectedTasks)
            }
        }.show()
    }

    private fun deleteAllCompleted() {
        tasksViewModel.deleteAllCompletedTasks()
    }

    private fun toggleSelection(position: Int) {
        taskAdapter.toggleSelectTask(position)

        val selectedItemCount = taskAdapter.getSelectedItemCount()

        if (selectedItemCount == 0) {
            mainActivity.closeSupportActionMode()
        } else {
            val text = when (selectedItemCount) {
                1 -> "1 task selected"
                else -> "$selectedItemCount tasks selected"
            }
            mainActivity.setSupportActionModeTitle(text)
        }

    }


}