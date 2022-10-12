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
import com.example.tasks.R
import com.example.tasks.databinding.FormAddTaskBinding
import com.example.tasks.databinding.FragmentAddTaskBinding
import com.example.tasks.service.TaskNotificationManager
import com.example.tasks.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.*

class AddTaskFragment : BaseFragment(R.layout.fragment_add_task) {
    private val TAG = "AddTaskFragment"
    private var _binding: FragmentAddTaskBinding? = null
    private val binding: FragmentAddTaskBinding get() = _binding!!
    private var _formBinding: FormAddTaskBinding? = null
    private val formBinding: FormAddTaskBinding get() = _formBinding!!

    private lateinit var discardChangesDialog: MaterialAlertDialogBuilder
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var timePickerDialog: TimePickerDialog

    private var dateOrTimeChanged = false

    private var priority = Constants.PRIORITY_LOW
    private val submitCreateTask = {
        if (validateForm()) {
            createTask()
        }
    }

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
    }

    private fun setUpDatePickerDialog() {
        datePickerDialog = DatePickerDialog(requireContext())
        datePickerDialog.setOnDateSetListener { _, year, month, date ->
            dateOrTimeChanged = true
            val selectedDate = getStringFromDate(year, month, date)
            formBinding.etDate.setText(selectedDate)
        }
    }

    private fun setUpTimePickerDialog() {
        val onTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hours, minutes ->
            dateOrTimeChanged = true
            val selectedTime = getTimeFromDate(hours, minutes)
            formBinding.etTime.setText(selectedTime)
        }
        timePickerDialog = TimePickerDialog(
            requireContext(),
            onTimeSetListener,
            getHourOfDayNow(), getMinutesNow(), false
        )
    }

    private fun initializeFormFields() {
        val currentDate = getStringFromDate(Date())
        val currentTime = getTimeFromDate(Date())


        formBinding.etDate.setText(currentDate)
        formBinding.etTime.setText(currentTime)

        formBinding.etTaskTitle.hint = Constants.getRandomTaskTitle()

        tasksViewModel.getAllTaskList().observe(viewLifecycleOwner) { taskList ->
            val autoCompleteTextView =
                (formBinding.textLayoutList.editText as? AutoCompleteTextView)
            if (taskList.isNotEmpty()) {
                val selected =
                    if (tasksViewModel.currentTaskListExists()) tasksViewModel.currentTaskList else taskList[0]
                autoCompleteTextView?.setText(selected.toString())
            }
            val dropDownAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                taskList
            )
            autoCompleteTextView?.setAdapter(dropDownAdapter)
        }
        formBinding.btnGroupPriority.check(R.id.btnPriorityLow)
        priority = Constants.PRIORITY_LOW

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
            if (formBinding.etTaskTitle.text.toString().isNotBlank() || dateOrTimeChanged) {
                discardChangesDialog.show()
            } else {
                mainActivity.navController.popBackStack()
            }
        }

    }

    private fun setUpMenuClickHandler() {
        mainActivity.setHandleMenuOptions { menuItem ->
            when (menuItem.itemId) {
                R.id.miAccept -> submitCreateTask()
                else -> Log.e(TAG, "setUpMenuClickHandler: invalid : ${menuItem.itemId}")
            }
        }
    }

    private fun validateForm(): Boolean {
        hideKeyboard()
        if (formBinding.etTaskTitle.text.isEmpty() && formBinding.etTaskTitle.text.isBlank()) {
            showSnackBarWithMessage("Title cannot be empty")
            return false
        }

        return true
    }

    private fun showSnackBarWithMessage(msg: String) {
        Snackbar.make(formBinding.root, msg, Snackbar.LENGTH_SHORT).show()
    }


    private fun createTask() {
        val title = formBinding.etTaskTitle.text.toString()
        val priority = this.priority
        val date = formBinding.etDate.text.toString()
        val time = formBinding.etTime.text.toString()
        val list = formBinding.textLayoutList.editText?.text.toString() ?: ""
        val description = formBinding.textLayoutDescription.editText?.text.toString() ?: ""

        tasksViewModel.insertTask(title, priority, date, time, list, description)
//        { newTask ->
//            val service = TaskNotificationManager(requireContext())
//            Log.e(TAG, "createTask: newTask.id = ${newTask.id}")
//            service.showNotification(newTask)
//        }

        showSnackBarWithMessage("Task Created")
        mainActivity.navController.popBackStack()
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            mainActivity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(formBinding.root.windowToken, 0)
    }

}