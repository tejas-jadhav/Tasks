package com.example.tasks.ui.fragments

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasks.R
import com.example.tasks.TasksApplication
import com.example.tasks.data.model.Task
import com.example.tasks.databinding.FragmentTaskBinding
import com.example.tasks.ui.adapters.TaskAdapter
import com.example.tasks.ui.adapters.TaskAdapterItem
import com.example.tasks.ui.adapters.TaskViewHolder
import com.example.tasks.utils.Constants
import com.example.tasks.utils.Resource
import com.google.android.material.snackbar.Snackbar
import com.maltaisn.icondialog.pack.IconPack
import kotlinx.coroutines.delay

class TaskFragment : BaseFragment(R.layout.fragment_task), TaskAdapter.OnTaskClickListener {
    private val TAG = "TaskFragment"
    private var _binding: FragmentTaskBinding? = null
    private val binding: FragmentTaskBinding get() = _binding!!
    private lateinit var taskAdapter: TaskAdapter

    private val iconDialogIconPack: IconPack
        get() = (requireActivity().application as TasksApplication).iconPack!!

    private lateinit var deleteDialog: AlertDialog
    private lateinit var deleteAllCompletedDialog: AlertDialog


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskBinding.bind(view)

        setFabOnClickToAddFragment()
        showFab()
        setUpDeleteDialog()
        setUpMenuClickHandler()
        setUpRecyclerView()
        observeTasks()
        observeTaskListCount()
        observeCompletedTasks()
        setUpOnClickListeners()

    }

    private fun observeTaskListCount() {
        tasksViewModel.getTaskListCount().observe(viewLifecycleOwner) {
            if (it == 0) {
                setFabOnClickToDisplaySnackbar("Create a list first")
            } else {
                setFabOnClickToAddFragment()
            }
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
    }

    private fun deleteAllCompleted() {
        tasksViewModel.deleteAllCompletedTasks()
        Snackbar.make(
            binding.root,
            "Cleared completed tasks",
            Snackbar.LENGTH_SHORT
        ).setAnchorView(R.id.bottomNavigationView).show()
    }

    private fun observeCompletedTasks() {
        tasksViewModel.getTotalCompletedTaskCount().observe(viewLifecycleOwner) {
            if (it == 0) {
                binding.tvCompletedCount.visibility = View.GONE
            } else {
                val text = "Completed($it)"
                binding.tvCompletedCount.text = text
                binding.tvCompletedCount.visibility = View.VISIBLE
            }
        }
    }

    private fun setUpOnClickListeners() {
        binding.btnSort.setOnClickListener {
            if (mainActivity.isActionModeEnabled) {
                return@setOnClickListener
            }
            Log.e(TAG, "setUpOnClickListeners: sort button clicked")
        }

        binding.ibViewAs.setOnClickListener {
            if (mainActivity.isActionModeEnabled) {
                return@setOnClickListener
            }
            Log.e(TAG, "setUpOnClickListeners: viewAsbutton clicked")
        }

        binding.tvCompletedCount.setOnClickListener {
            if (mainActivity.isActionModeEnabled) {
                return@setOnClickListener
            }
            navigateToCompletedTasks()
        }

    }

    private fun navigateToCompletedTasks() {
        mainActivity.navController.navigate(
            R.id.action_taskFragment_to_completedTaskFragment
        )
    }

    private fun setUpRecyclerView() {
        taskAdapter = TaskAdapter(iconDialogIconPack, this)
        binding.rvMainTasks.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeTasks() {
        tasksViewModel.taskItemToDoList.observe(viewLifecycleOwner) { taskAdapterItemsResource ->
            when (taskAdapterItemsResource) {
                is Resource.Success -> {
                    taskAdapterItemsResource.data?.let { taskAdapterItems ->
                        taskAdapter.updateList(taskAdapterItems)
                        if (taskAdapterItems.isNotEmpty()) {
                            binding.lottieAnimationView.visibility = View.GONE
                            binding.tvAddNotesHelper.visibility = View.GONE
                        } else {
                            binding.lottieAnimationView.visibility = View.VISIBLE
                            binding.tvAddNotesHelper.setText(R.string.add_tasks)
                            binding.tvAddNotesHelper.visibility = View.VISIBLE
                        }
                    }
                }
                is Resource.Loading -> {
                    binding.lottieAnimationView.visibility = View.INVISIBLE
                    binding.tvAddNotesHelper.setText(R.string.loading)
                }
                is Resource.Error -> {
                    binding.lottieAnimationView.visibility = View.VISIBLE
                    binding.tvAddNotesHelper.setText(R.string.error)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        showFab()
    }

    override fun onPause() {
        hideFab()
        super.onPause()
    }

    private fun navigateToAddTaskFragment() {
        mainActivity.navController.navigate(
            R.id.action_taskFragment_to_addTaskFragment
        )
    }

    private fun navigateToTaskListFragment() {
        mainActivity.navController.navigate(
            R.id.action_taskFragment_to_taskListFragment
        )
    }


    private fun setUpMenuClickHandler() {
        mainActivity.setHandleMenuOptions { menuItem ->
            when (menuItem.itemId) {
                R.id.miAddNewTask -> navigateToAddTaskFragment()
                R.id.miClearCompleted -> deleteAllCompletedDialog.show()
                R.id.miShowCompleted -> navigateToCompletedTasks()
                else -> Log.e(TAG, "setUpMenuClickHandler: Invalid Option ${menuItem.title}")
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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
                R.id.action_taskFragment_to_individualTaskFragment
            )
        }
    }

    override fun onTaskLayoutLongClickListener(task: Task, holder: TaskViewHolder.Item) {
        if (!mainActivity.isActionModeEnabled) {
            mainActivity.openSupportActionMode(actionModeCallback)
            hideFab()
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
            showFab()
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
            binding.root,
            message,
            Toast.LENGTH_SHORT
        ).setAnchorView(R.id.bottomNavigationView)
            .setAction("Undo") {
                tasksViewModel.insertListOfTask(selectedTasks)
            }.show()
    }

    //    fab stuff
    private fun setFabOnClickToAddFragment() {
        val navigateToAddTask = {
            navigateToAddTaskFragment()
        }
        binding.fabTasks.setOnClickListener {
            fabRevealAnimationForwards {
                navigateToAddTask()
            }
        }
    }

    private fun setFabOnClickToDisplaySnackbar(message: String) {
        binding.fabTasks.setOnClickListener {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.bottomNavigationView).show()
        }
    }

    private fun showFab() {
        lifecycleScope.launchWhenCreated {
            delay(100)
            binding.fabTasks.show()
        }
    }

    private fun hideFab() {
        binding.fabTasks.hide()
    }

    private fun fabRevealAnimationForwards(onEndListener: () -> Unit) {
        val fabDummy = binding.fabAnimationDummy
        val fab = binding.fabTasks
        val container = binding.clContent

        fabDummy.isVisible = true

        mainActivity.hideBottomNavigationView()

        val hideFab = ObjectAnimator.ofFloat(fab, View.ALPHA, 0f)
        val hideContainer = ObjectAnimator.ofFloat(container, View.ALPHA, 1f, 0f)
        val hideFabDummy = ObjectAnimator.ofFloat(fabDummy, View.ALPHA, 1f, 0f)
        val scaleFabDummy = ObjectAnimator.ofPropertyValuesHolder(
            fabDummy,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 40f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 40f)
        )

        AnimatorSet().apply {
            duration = Constants.FAB_ANIMATION_DURATION
            interpolator = AccelerateInterpolator()
            playTogether(
                hideFab,
                hideContainer,
                scaleFabDummy,
                hideFabDummy
            )

            addListener(onEnd = {
                binding.fabTasks.isVisible = true
                onEndListener()
            })
        }.start()

    }

}