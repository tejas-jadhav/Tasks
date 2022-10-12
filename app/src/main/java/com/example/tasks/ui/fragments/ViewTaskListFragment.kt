package com.example.tasks.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.view.isVisible
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

class ViewTaskListFragment : BaseFragment(R.layout.fragment_task), TaskAdapter.OnTaskClickListener {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private val iconDialogIconPack: IconPack?
        get() = (requireActivity().application as TasksApplication).iconPack

    private lateinit var deleteDialog: AlertDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskBinding.bind(view)

        initializeLayout()
        setUpDeleteDialog()
        subscribeToObservables()
    }

    private fun subscribeToObservables() {

        tasksViewModel.tasksInList.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val list = resource.data!!
                    Log.e("ViewTaskListFragment", "subscribeToObservables: $list")
                    if (list.isEmpty()) {
                        binding.lottieAnimationView.isVisible = true
                        binding.tvAddNotesHelper.setText(R.string.completed_task_helper_msg)
                        taskAdapter.updateList(listOf())
                    } else {
                        binding.lottieAnimationView.isVisible = false
                        binding.tvAddNotesHelper.isVisible = false
                        taskAdapter.updateList(list)
                    }
                }
                is Resource.Loading -> {
                    binding.lottieAnimationView.isVisible = false
                    binding.tvAddNotesHelper.isVisible = true
                    binding.tvAddNotesHelper.setText(R.string.loading)
                }
                is Resource.Error -> {
                    binding.tvAddNotesHelper.setText(R.string.error)
                }
            }
        }
    }

    private fun initializeLayout() {
        val taskList = tasksViewModel.currentTaskList
        mainActivity.setActionBarTitle(taskList.name)
        binding.root.setBackgroundResource(R.color.white)
        initializeRecyclerView()

        mainActivity.setBackCallback {
            tasksViewModel.clearCurrentTaskList()
            it.popBackStack()
        }
        mainActivity.setHandleMenuOptions {
            when (it.itemId) {
                R.id.miAddNewTask -> handleCreateNewTaskClick()
            }
        }
    }

    private fun handleCreateNewTaskClick() {
        mainActivity.navController.navigate(
            R.id.action_viewTaskListFragment_to_addTaskFragment
        )
    }

    private fun initializeRecyclerView() {
        taskAdapter = TaskAdapter(iconDialogIconPack!!, this)
        binding.rvMainTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

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
            toggleSelectTask(holder.adapterPosition)
        } else {
            tasksViewModel.setCurrentTask(taskItem)
            mainActivity.navController.navigate(
                R.id.action_viewTaskListFragment_to_individualTaskFragment
            )
        }
    }

    override fun onTaskLayoutLongClickListener(task: Task, holder: TaskViewHolder.Item) {
        if (!mainActivity.isActionModeEnabled) {
            mainActivity.openSupportActionMode(actionModeCallback)
            toggleSelectTask(holder.adapterPosition)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.action_menu, menu)
            mainActivity._isActionModeEnabled = true
//            binding.btnSort.visibility = View.GONE
//            binding.ibViewAs.visibility = View.GONE
            mainActivity.hideBottomNavigationView()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.miDelete -> {
                    deleteDialog.show()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mainActivity._isActionModeEnabled = false
            mainActivity.actionMode = null
            taskAdapter.clearSelection()
//            binding.btnSort.visibility = View.VISIBLE
//            binding.ibViewAs.visibility = View.VISIBLE
            mainActivity.showBottomNavigationView()
        }
    }

    private fun toggleSelectTask(position: Int) {
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

    private fun setUpDeleteDialog() {
        deleteDialog = AlertDialog.Builder(requireContext()).apply {
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

    private fun deleteSelectedTasks() {
        val selectedTasksCount = taskAdapter.getSelectedItemCount()
        if (selectedTasksCount <= 0) {
            return
        }
        val selectedTasks = taskAdapter.getSelectedTasks()
        tasksViewModel.deleteListOfTask(selectedTasks)
        mainActivity.closeSupportActionMode()

        val message = when (selectedTasksCount) {
            1 -> "1 Task deleted"
            else -> "$selectedTasksCount tasks deleted"
        }
        Snackbar.make(
            mainView,
            message,
            Toast.LENGTH_SHORT
        ).setAction("Undo") {
            tasksViewModel.insertListOfTask(selectedTasks)
        }.show()
    }


}