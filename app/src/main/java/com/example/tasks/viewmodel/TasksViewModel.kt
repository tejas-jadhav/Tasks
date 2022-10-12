package com.example.tasks.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.*
import com.example.tasks.TasksApplication
import com.example.tasks.data.model.Task
import com.example.tasks.data.model.TaskList
import com.example.tasks.data.repository.TasksRepository
import com.example.tasks.service.TaskAlarmManager
import com.example.tasks.ui.adapters.TaskAdapterItem
import com.example.tasks.ui.adapters.TaskListAdapter
import com.example.tasks.utils.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class TasksViewModel(private val repository: TasksRepository, app: Application) :
    AndroidViewModel(app) {

    private val TAG = "TasksViewModel"
    private val taskAlarmManager = TaskAlarmManager(getApplication())
    private val _initialLoading =  MutableStateFlow(true)
    val initialLoading = _initialLoading.asStateFlow()


    private var onSuccessOfUpdate = suspend {

    }


    /**
     * Functions related to tasks
     */

    private val _taskOperationStatus = MutableSharedFlow<Resource<Task>>()
    val taskOperationStatus = _taskOperationStatus.asSharedFlow()
    private var _currentTaskItem: TaskAdapterItem.TaskItem? = null
    val currentTaskItem get() = _currentTaskItem!!

    fun getTotalTaskCount() = repository.getTotalTaskCount()
    fun getTotalCompletedTaskCount() = repository.getTotalCompletedTaskCount()

    val taskItemToDoList = MutableLiveData<Resource<List<TaskAdapterItem>>>()
    val taskItemCompletedList = MutableLiveData<Resource<List<TaskAdapterItem>>>()


    init {
        setOnUpdate()
        viewModelScope.launch {
            repository.getAllTasksAsFlow().collectLatest {
                updateTaskItemTodoList()
                updateTaskItemCompletedList()
                updateTasksInList()
                onSuccessOfUpdate()
                onSuccessOfUpdate = {}
                if (_initialLoading.value) {
                    _initialLoading.value = false
                }
            }
        }
    }

    fun setCurrentTask(task: TaskAdapterItem.TaskItem) {
        _currentTaskItem = task
    }

    fun clearCurrentTask() {
        _currentTaskItem = null
    }

    fun insertTask(task: Task) = viewModelScope.launch {
        val id = repository.insertTask(task)
        repository.findListByIdAndIncrementTotal(task.listId)
        setOnUpdate()
        val insertedTask = repository.getTaskById(id.toString().toInt())
        taskAlarmManager.setAlarm(insertedTask)
    }

    fun insertTask(
        title: String,
        priority: Int,
        date: String,
        time: String,
        list: String,
        description: String,
        onSuccess: (Task) -> Unit = {}
    ) = viewModelScope.launch {
        try {
            _taskOperationStatus.emit(Resource.Loading())
            val listId = repository.getTaskListIdByName(list)
            val dateTime = mergeDateTimeStringToDate(date, time).time
            val newTask = Task(
                0,
                title,
                priority,
                dateTime,
                listId,
                description
            )
            val id = repository.insertTask(newTask)
            Log.e(TAG, "insertTask: Id of inserted : $id ")
            newTask.id = id.toString().toInt()
            Log.e(TAG, "insertTask: newTask.id = ${newTask.id}")
            repository.findListByIdAndIncrementTotal(listId)

            _taskOperationStatus.emit(Resource.Success(newTask))
            setOnUpdate()
            onSuccess(newTask)
            taskAlarmManager.setAlarm(newTask)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "insertTask: Some exception occurred")
            _taskOperationStatus.emit(Resource.Error("Some error occurred"))
            e.printStackTrace()
        }
    }

    fun updateTask(
        id: Int,
        title: String,
        priority: Int,
        date: String,
        time: String,
        list: String,
        description: String,
        progress: Int? = null
    ) = viewModelScope.launch {
        try {
            _taskOperationStatus.emit(Resource.Loading())
            val listId = repository.getTaskListIdByName(list)
            val dateTime = mergeDateTimeStringToDate(date, time).time

            Log.e(TAG, "updateTask: Current task id : $id")
            val oldTaskId = repository.getListIdByTaskId(id)
            val oldTask = repository.getTaskById(id)
            taskAlarmManager.cancelExistingAlarm(oldTask)

            val updatedTask = Task(
                id,
                title,
                priority,
                dateTime,
                listId,
                description,
                progress = progress
            )
            repository.updateTask(updatedTask)

            if (oldTaskId != listId) {
                repository.findListByIdAndDecrementTotal(oldTaskId)
                repository.findListByIdAndIncrementTotal(listId)
            }
            taskAlarmManager.setAlarm(updatedTask)

            val newTaskItem = TaskItemListCreator.getInstance().convert(updatedTask, repository)
            _currentTaskItem = newTaskItem
            _taskOperationStatus.emit(Resource.Success(updatedTask))
            setOnUpdate()
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "insertTask: Some exception occurred")
            _taskOperationStatus.emit(Resource.Error("Some error occurred"))
            e.printStackTrace()
        }

    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        _taskOperationStatus.emit(Resource.Loading())
        repository.deleteTask(task)
        repository.findListByIdAndDecrementTotal(task.listId)
        _taskOperationStatus.emit(Resource.Success(task))
        setOnUpdate()
        taskAlarmManager.cancelExistingAlarm(task)
    }

    fun deleteListOfTask(tasks: List<Task>) = viewModelScope.launch {
        val map = mutableMapOf<Int, Int>()

        for (task in tasks) {
            val count = map.getOrDefault(task.listId, 0) + 1
            map[task.listId] = count
        }

        for (task in tasks) {
            repository.deleteTask(task)
            taskAlarmManager.cancelExistingAlarm(task)
        }

        for (listId in map.keys) {
            val count = map[listId] ?: 0
            if (count != 0) {
                repository.findListByIdAndDecrementTotal(listId, count)
            }
        }

    }

    fun deleteAllCompletedTasks() = viewModelScope.launch {
        val completedTasks = repository.getAllCompletedTasks()

        val map = mutableMapOf<Int, Int>()
        for (task in completedTasks) {
            val count = map.getOrDefault(task.listId, 0) + 1
            map[task.listId] = count
        }

        repository.deleteAllCompletedTasks()

        taskAlarmManager.cancelAlarmForTasks(completedTasks)

        for (listId in map.keys) {
            val count = map[listId] ?: 0
            if (count != 0) {
                repository.findListByIdAndDecrementTotal(listId, count)
            }
        }

        setOnUpdate()
    }

    fun insertListOfTask(selectedTasks: List<Task>) = viewModelScope.launch {
        val map = mutableMapOf<Int, Int>()

        for (task in selectedTasks) {
            repository.insertTask(task)
            map[task.listId] = map.getOrDefault(task.listId, 0) + 1
        }


        for (listId in map.keys) {
            repository.findListByIdAndIncrementTotal(listId, map[listId] ?: 0)
        }
        taskAlarmManager.setAlarmForTasks(selectedTasks)
        setOnUpdate()
    }

    fun toggleMarkAsComplete(task: Task, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        if (task.isCompleted) {
            repository.markAsComplete(task.id)
            taskAlarmManager.cancelExistingAlarm(task)
        } else {
            repository.markAsIncomplete(task.id)
            taskAlarmManager.setAlarm(task)
        }
        setOnUpdate() {
            val updatedTask = repository.getTaskById(task.id)
            _currentTaskItem = TaskItemListCreator.getInstance().convert(updatedTask, repository)
            onSuccess()
        }
    }

    private suspend fun updateTaskItemTodoList() {
        taskItemToDoList.postValue(Resource.Loading())
        val tasks = repository.getAllTasksTodo()
        val taskAdapterList = TaskItemListCreator.getInstance().getTaskAdapterItemList(
            tasks, repository
        )
        taskItemToDoList.postValue(Resource.Success(taskAdapterList))
    }

    private suspend fun updateTaskItemCompletedList() {
        taskItemCompletedList.postValue(Resource.Loading())
        val tasks = repository.getAllCompletedTasks()
        val taskAdapterList = TaskItemListCreator.getInstance().getTaskAdapterItemList(
            tasks, repository
        )
        taskItemCompletedList.postValue(Resource.Success(taskAdapterList))
    }

    private fun setOnUpdate(onSuccess: suspend () -> Unit = {}) {
        onSuccessOfUpdate = onSuccess
    }

    val tasksInList = MutableLiveData<Resource<List<TaskAdapterItem>>>()

    suspend fun updateTasksInList() {
        if (currentTaskListExists()) {
            tasksInList.postValue(Resource.Loading())
            val list = repository.getAllTasksInListId(currentTaskList.id)
            val items = TaskItemListCreator.getInstance().getTaskAdapterItemList(list, repository)
            tasksInList.postValue(Resource.Success(items))
        }
    }

    fun getAllTasksInListId(id: Int) = viewModelScope.launch {
        tasksInList.postValue(Resource.Loading())
        val tasks = repository.getAllTasksInListId(id)
        val items = TaskItemListCreator.getInstance().getTaskAdapterItemList(tasks, repository)
        tasksInList.postValue(Resource.Success(items))
    }

    fun getTaskByIdAndChangePriority(id: Int, newPriority: Int, onSuccess: () -> Unit = {}) =
        viewModelScope.launch {
            repository.getTaskByIdAndChangePriority(id, newPriority)
            val task = repository.getTaskById(id)
            _currentTaskItem = TaskItemListCreator.getInstance().convert(task, repository)

            onSuccess()
        }

    fun changeTaskProgress(taskId: Int, newProgress: Int, onSuccess: () -> Unit = {}) =
        viewModelScope.launch {
            repository.getTaskByIdAndSetProgress(taskId, newProgress)
            val task = repository.getTaskById(taskId)
            _currentTaskItem = TaskItemListCreator.getInstance().convert(task, repository)
            onSuccess()
        }

    /**
     * Functions related to task lists
     *
     **/

    private val _taskListOperationStatus = MutableSharedFlow<Resource<TaskList>>()
    val taskListOperationStatus = _taskListOperationStatus.asSharedFlow()


    fun getAllTaskList() = repository.getAllTaskList()
    fun getTaskListCount() = repository.getTotalTaskListCount()

    fun getTaskListIdByName(name: String) = viewModelScope.launch {
        val id = repository.getTaskListIdByName(name)
        Log.e(TAG, "getTaskListIdByName: id: $id")
    }

    fun insertTaskList(taskList: TaskList) {
        viewModelScope.launch {
            try {
                _taskListOperationStatus.emit(Resource.Loading())
                repository.insertTaskList(taskList)
                _taskListOperationStatus.emit(Resource.Success(taskList))
            } catch (e: SQLiteConstraintException) {
                Log.e(TAG, "insertTaskList: Trying to insert duplicate value")
                _taskListOperationStatus.emit(Resource.Error("The list ${taskList.name} already exists"))
                e.printStackTrace()
            }
        }
    }

    fun deleteTaskList(taskList: TaskList) {
        viewModelScope.launch {
            repository.deleteTaskList(taskList)
        }
    }

    fun deleteListOfTaskList(taskListAdapter: TaskListAdapter) =
        viewModelScope.launch {
            val selectedItemsIndex = taskListAdapter.getListOfSelectedIndices().sortedDescending()
            for (index in selectedItemsIndex) {
                val item = taskListAdapter[index]
                val incompleteTasks = repository.getIncompleteTasksOfListId(item.id)
                taskAlarmManager.cancelAlarmForTasks(incompleteTasks)
                repository.deleteTaskList(item)
            }
        }

    private var _currentTaskList: TaskList? = null
    val currentTaskList get() = _currentTaskList!!

    fun currentTaskListExists(): Boolean {
        return _currentTaskList != null
    }

    fun setCurrentTaskList(taskList: TaskList) {
        _currentTaskList = taskList
        getAllTasksInListId(taskList.id)
    }

    fun clearCurrentTaskList() {
        _currentTaskList = null
    }
}


class TasksViewModelFactory(
    private val repository: TasksRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TasksViewModel(repository, application) as T
    }
}