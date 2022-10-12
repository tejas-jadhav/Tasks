package com.example.tasks.ui.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.lifecycle.lifecycleScope
import com.example.tasks.R
import com.example.tasks.databinding.FormAddTaskBinding
import com.example.tasks.databinding.FragmentAddTaskBinding
import com.example.tasks.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import java.util.*

class EditTaskFragment : BaseFragment(R.layout.fragment_add_task) {
    private val TAG = "AddTaskFragment"
    private var _binding: FragmentAddTaskBinding? = null
    private val binding: FragmentAddTaskBinding get() = _binding!!
    private var _formBinding: FormAddTaskBinding? = null
    private val formBinding: FormAddTaskBinding get() = _formBinding!!

    private lateinit var discardChangesDialog: MaterialAlertDialogBuilder
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var timePickerDialog: TimePickerDialog
    private var priority: Int = Constants.PRIORITY_LOW


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddTaskBinding.bind(view)
        _formBinding = FormAddTaskBinding.bind(binding.formAddTask.scrollView)

        setUpDatePickerDialog()
        setUpTimePickerDialog()
        initializeFormFields()
        setFormOnClickListeners()
        setUpDiscardChangesDialog()
        setUpMenuClickHandler()

        lifecycleScope.launchWhenCreated {
            tasksViewModel.taskOperationStatus.collectLatest { operationResult ->
                when (operationResult) {
                    is Resource.Success -> {
                        showSnackBarWithMessage("Task Updated")
                        mainActivity.navController.popBackStack()
                    }
                    is Resource.Error -> showSnackBarWithMessage("Some Error occurred")
                    is Resource.Loading -> return@collectLatest
                }
            }
        }
    }

    private fun setUpDatePickerDialog() {
        val datePickerListener =
            DatePickerDialog.OnDateSetListener { _, year, month, date ->
                val selectedDate = getStringFromDate(year, month, date)
                formBinding.etDate.setText(selectedDate)
            }
        val date = Date(tasksViewModel.currentTaskItem.task.dateTime)
        datePickerDialog = DatePickerDialog(
            requireContext(),
            datePickerListener,
            getYearFromDate(date), getMonthFromDate(date), getDayFromDate(date)
        )
    }

    private fun setUpTimePickerDialog() {
        val onTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
            val selectedTime = getTimeFromDate(hours, minutes)
            formBinding.etTime.setText(selectedTime)
        }
        val date = Date(tasksViewModel.currentTaskItem.task.dateTime)
        timePickerDialog = TimePickerDialog(
            requireContext(),
            onTimeSetListener,
            getHourOfDayFromDate(date), getMinuteFromDate(date), false
        )
    }

    private fun initializeFormFields() {
        val date = Date(tasksViewModel.currentTaskItem.task.dateTime)
        val currentDate = getStringFromDate(date)
        val currentTime = getTimeFromDate(date)
        val currentTask = tasksViewModel.currentTaskItem.task

        formBinding.etDate.setText(currentDate)
        formBinding.etTime.setText(currentTime)

        formBinding.etTaskTitle.setText(currentTask.title)
        formBinding.etDescription.setText(currentTask.description)
        val save = "Save"
        formBinding.btnCreateTask.text = save


        tasksViewModel.getAllTaskList().observe(viewLifecycleOwner) { taskList ->
            val autoCompleteTextView =
                (formBinding.textLayoutList.editText as? AutoCompleteTextView)
            if (taskList.isNotEmpty()) {
                val found = taskList.find { it.id == tasksViewModel.currentTaskItem.task.listId }
                autoCompleteTextView?.setText(found.toString())
            }
            val dropDownAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                taskList
            )
            autoCompleteTextView?.setAdapter(dropDownAdapter)

        }
        priority = tasksViewModel.currentTaskItem.task.priorityId
        val checkedButtonId = when (priority) {
            Constants.PRIORITY_HIGH -> R.id.btnPriorityHigh
            Constants.PRIORITY_MID -> R.id.btnPriorityMedium
            else -> R.id.btnPriorityLow
        }
        formBinding.btnGroupPriority.check(checkedButtonId)
    }

    private fun setFormOnClickListeners() {
        val showDatePickerDialog: (View) -> Unit = {
            datePickerDialog.show()
        }
        formBinding.ibCalendar.setOnClickListener(showDatePickerDialog)
        formBinding.etDateClickProxy.setOnClickListener(showDatePickerDialog)

        val showTimePickerDialog: (View) -> Unit = {
            timePickerDialog.show()
        }

        formBinding.ibTime.setOnClickListener(showTimePickerDialog)
        formBinding.etTimeClickProxy.setOnClickListener(showTimePickerDialog)

        formBinding.btnGroupPriority.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            priority = when (checkedId) {
                R.id.btnPriorityLow -> Constants.PRIORITY_LOW
                R.id.btnPriorityMedium -> Constants.PRIORITY_MID
                R.id.btnPriorityHigh -> Constants.PRIORITY_HIGH
                else -> Constants.PRIORITY_LOW
            }
        }

        formBinding.btnCreateTask.setOnClickListener {
            submitCreateTask()
        }
    }

    private fun setUpDiscardChangesDialog() {
        discardChangesDialog = MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle("Discard Changes ?")
            setPositiveButton("Yes") { dialogInterface, _ ->
                dialogInterface.dismiss()
                mainActivity.navController.popBackStack()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
            }
        }
        mainActivity.setBackCallback {
            discardChangesDialog.show()
        }

    }

    private fun showSnackBarWithMessage(msg: String) {
        Log.e(TAG, "showSnackBarWithMessage: Showing updated snackbar")
        Snackbar.make(mainView, msg, Snackbar.LENGTH_SHORT).show()
    }

    private fun validateForm(): Boolean {
        hideKeyboard()
        if (formBinding.etTaskTitle.text.isEmpty() && formBinding.etTaskTitle.text.isBlank()) {
            showSnackBarWithMessage("Title cannot be empty")
            return false
        }

        return true
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(formBinding.root.windowToken, 0)
    }

    private fun setUpMenuClickHandler() {
        mainActivity.setHandleMenuOptions { menuItem ->
            when (menuItem.itemId) {
                R.id.miAccept -> submitCreateTask()
                else -> Log.e(TAG, "setUpMenuClickHandler: invalid : ${menuItem.itemId}")
            }
        }
    }

    private val submitCreateTask = {
        if (validateForm()) {
            updateTask()
        }
    }

    private fun updateTask() {
        val title = formBinding.etTaskTitle.text.toString()
        val priority = this.priority
        val date = formBinding.etDate.text.toString()
        val time = formBinding.etTime.text.toString()
        val list = formBinding.textLayoutList.editText?.text.toString() ?: ""
        val description = formBinding.textLayoutDescription.editText?.text.toString() ?: ""

        tasksViewModel.updateTask(
            tasksViewModel.currentTaskItem.task.id,
            title,
            priority,
            date,
            time,
            list,
            description,
            tasksViewModel.currentTaskItem.task.progress
        )


    }
}