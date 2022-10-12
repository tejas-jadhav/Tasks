package com.example.tasks.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tasks.R
import com.example.tasks.TasksApplication
import com.example.tasks.data.model.TaskList
import com.example.tasks.databinding.FormAddListBinding
import com.example.tasks.databinding.FragmentTaskListBinding
import com.example.tasks.ui.adapters.TaskListAdapter
import com.example.tasks.ui.adapters.TaskListViewHolder
import com.example.tasks.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.maltaisn.icondialog.IconDialog
import com.maltaisn.icondialog.IconDialogSettings
import com.maltaisn.icondialog.data.Icon
import com.maltaisn.icondialog.pack.IconPack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TaskListFragment : BaseFragment(R.layout.fragment_task_list), IconDialog.Callback,
    TaskListAdapter.OnAddTaskListItemClick {

    private val TAG = "TaskListFragment"

    private var _binding: FragmentTaskListBinding? = null
    private val binding: FragmentTaskListBinding get() = _binding!!
    private lateinit var createListDialog: MaterialAlertDialogBuilder
    private lateinit var iconDialog: IconDialog
    private var _dialogBinding: FormAddListBinding? = null
    private val dialogBinding: FormAddListBinding get() = _dialogBinding!!

    private lateinit var deleteAlertDialog: AlertDialog

    private lateinit var taskListAdapter: TaskListAdapter

    private var selectedIconId: Int? = null

    companion object {
        private const val ICON_DIALOG = "icon-dialog"
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTaskListBinding.bind(view)
        _dialogBinding = FormAddListBinding.bind(
            layoutInflater.inflate(
                R.layout.form_add_list,
                null,
                false
            )
        )


        setUpMenuClickHandler()
        setUpIconDialog()
        setUpCreateListDialog()
        setUpCreateListDialogOnClickListeners()
        setUpRecyclerView()
        buildDeleteAlertDialog()
        observeAllTaskList()
        observeTaskListOperationStatus()
        observeTotalTaskCount()


    }

    private fun observeTotalTaskCount() {
        tasksViewModel.getTotalTaskCount().observe(viewLifecycleOwner) {
            taskListAdapter.totalTasks = it
            taskListAdapter.notifyItemChanged(0)
        }
    }

    private fun buildDeleteAlertDialog() {
        deleteAlertDialog = AlertDialog.Builder(requireContext())
            .apply {
                setTitle("Confirm Delete List")
                setIcon(R.drawable.ic_baseline_delete_24)
                setMessage("Deleting entire list will also delete all the individual tasks within that list. Are you sure you want to proceed with this action ?")
                setPositiveButton("Yes") { dialog, _ ->
                    deleteSelectedItems()
                    mainActivity.closeSupportActionMode()
                    dialog.dismiss()
                }
                setNegativeButton("No") { dialog, _ ->
                    mainActivity.closeSupportActionMode()
                    dialog.cancel()
                }
            }
            .create()
    }

    private fun setUpMenuClickHandler() {
        mainActivity.setHandleMenuOptions { menuItem ->
            when (menuItem.itemId) {
                R.id.miAddNewTask -> navigateToAddTaskFragment()
                R.id.miAddNewList -> openCreateNewListDialog()
                R.id.miAddNewListButton -> openCreateNewListDialog()
                else -> Log.e(TAG, "setUpMenuClickHandler: Invalid input ${menuItem.itemId}")
            }
        }
    }

    override val iconDialogIconPack: IconPack?
        get() = (requireActivity().application as TasksApplication).iconPack

    private fun setUpIconDialog() {
        iconDialog =
            childFragmentManager.findFragmentByTag(ICON_DIALOG) as IconDialog?
                ?: IconDialog.newInstance(IconDialogSettings())
    }

    private fun setUpCreateListDialog() {
        createListDialog = MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("Create List")

            setPositiveButton("Add") { dialog, _ ->
                val text = dialogBinding.textLayoutAddList.editText?.text.toString()
                if (text.isEmpty() || text.isBlank()) {
                    Toast.makeText(requireContext(), "Enter Title", Toast.LENGTH_SHORT).show()
                } else {
                    createTaskList(text, selectedIconId!!)
                    dialog.dismiss()
                    dialogBinding.textLayoutAddList.editText?.text?.clear()
                }
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        }
    }

    private fun setUpCreateListDialogOnClickListeners() {
        dialogBinding.btnChooseListIcon.setOnClickListener {
            showIconDialog()
        }
    }

    private fun setUpRecyclerView() {
        taskListAdapter = TaskListAdapter(this, iconDialogIconPack!!)
        binding.rvAddTasks.apply {
            adapter = taskListAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            CoroutineScope(Dispatchers.Main).launch {
                delay(200)
                smoothScrollToPosition(0)
            }
        }
    }

    private fun observeAllTaskList() {
        tasksViewModel.getAllTaskList().observe(viewLifecycleOwner) {

            Log.e(TAG, "observeAllTaskList: items of task list = ${it.size}")
            taskListAdapter.updateList(it.toList())

        }
    }

    private fun observeTaskListOperationStatus() {
        lifecycleScope.launchWhenCreated {
            tasksViewModel.taskListOperationStatus.collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> showSnackBarWithMessage("${resource.data?.name} List Created")
                    is Resource.Loading -> return@collectLatest
                    is Resource.Error -> showSnackBarWithMessage(
                        resource.message ?: "Some Error Occurred"
                    )
                }
            }
        }
    }


    private fun createTaskList(listName: String, iconId: Int) {
        val newTaskList = TaskList(0, listName, iconId)
        tasksViewModel.insertTaskList(newTaskList)


    }


    private fun showIconDialog() {
        iconDialog.show(childFragmentManager, ICON_DIALOG)
    }

    override fun onIconDialogIconsSelected(dialog: IconDialog, icons: List<Icon>) {
        if (icons.isNotEmpty()) {
            val icon = icons[0]
            selectedIconId = icon.id
            dialogBinding.ivListIcon.setImageDrawable(icon.drawable)
        }
        dialog.dismiss()
    }

    private fun navigateToAddTaskFragment() {
        mainActivity.navController.navigate(
            R.id.action_taskListFragment_to_addTaskFragment
        )
    }

    private fun openCreateNewListDialog() {
        if (dialogBinding.root.parent != null) {
            (dialogBinding.root.parent as ViewGroup).removeView(dialogBinding.root)
        }
        val randomIconDrawable = iconDialogIconPack?.getIcon(getRandomIconId())?.drawable

        randomIconDrawable?.let {
            dialogBinding.ivListIcon.setImageDrawable(it)
        }
        createListDialog.setView(dialogBinding.root)
        createListDialog.show()
    }

    private fun getRandomIconId(): Int {
        val possibleIcons = listOf(1000, 24, 72, 523, 116, 579, 534)
        val randomId = possibleIcons[possibleIcons.indices.random()]
        selectedIconId = randomId
        return randomId
    }

    override fun onAllTaskClickListener() {
        if (!mainActivity.isActionModeEnabled) {
            mainActivity.navController.navigate(R.id.action_taskListFragment_to_taskFragment)
        }
    }

    override fun onAddListClickListener() {
        if (!mainActivity.isActionModeEnabled) {
            openCreateNewListDialog()
        }
    }

    override fun onTaskListItemClickListener(
        taskList: TaskList,
        holder: TaskListViewHolder.TaskListItemViewHolder
    ) {
        if (mainActivity.isActionModeEnabled) {
            selectItem(holder.adapterPosition)
        } else {
            tasksViewModel.setCurrentTaskList(taskList)
            mainActivity.navController.navigate(
                R.id.action_taskListFragment_to_viewTaskListFragment
            )
        }
    }

    override fun onTaskListItemLongClickListener(
        taskList: TaskList,
        holder: TaskListViewHolder.TaskListItemViewHolder
    ) {
        if (!mainActivity.isActionModeEnabled) {
            mainActivity.openSupportActionMode(actionModeCallback)
            selectItem(holder.adapterPosition)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.action_menu, menu)
            mainActivity._isActionModeEnabled = true
            mainActivity.hideBottomNavigationView()
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.miDelete -> {
                    deleteAlertDialog.show()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mainActivity._isActionModeEnabled = false
            mainActivity.actionMode = null
            taskListAdapter.clearSelection()
            mainActivity.showBottomNavigationView()
        }
    }

    private fun deleteSelectedItems() {
        val selectedCount = taskListAdapter.getSelectedItemCount()
        if (selectedCount <= 0) {
            return
        }
        tasksViewModel.deleteListOfTaskList(taskListAdapter)
    }

    private fun selectItem(position: Int) {
        taskListAdapter.toggleSelection(position)
        val selectedItemCount = taskListAdapter.getSelectedItemCount()
        if (selectedItemCount == 0) {
            taskListAdapter.clearSelection()
            mainActivity.closeSupportActionMode()
        } else {
            val text = when (selectedItemCount) {
                1 -> "1 list selected"
                else -> "$selectedItemCount lists selected"
            }
            mainActivity.setSupportActionModeTitle(text)
        }
    }


    private fun showSnackBarWithMessage(msg: String) {
        Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}