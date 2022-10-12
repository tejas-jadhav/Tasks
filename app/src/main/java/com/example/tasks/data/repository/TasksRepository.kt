package com.example.tasks.data.repository

import com.example.tasks.data.db.TasksDatabase
import com.example.tasks.data.model.Task
import com.example.tasks.data.model.TaskList

class TasksRepository(private val database: TasksDatabase) {
    /**
     * Task
     */

    suspend fun insertTask(task: Task) = database.getTaskDao().insertTask(task.apply {
        if (createdAt == null) {
            createdAt = System.currentTimeMillis()
        }
        modifiedAt = System.currentTimeMillis()
    })

    suspend fun updateTask(task: Task) = database.getTaskDao().updateTask(task)

    suspend fun deleteTask(task: Task) = database.getTaskDao().deleteTask(task)

    suspend fun getTaskById(id: Int) = database.getTaskDao().getTaskById(id)

    suspend fun getAllTasksTodo() = database.getTaskDao().getAllTasksToDo()

    suspend fun getAllCompletedTasks() = database.getTaskDao().getAllCompletedTasks()

    fun getTotalTaskCount() = database.getTaskDao().getTotalTaskCount()
    fun getTotalCompletedTaskCount() = database.getTaskDao().getTotalCompletedTaskCount()

    suspend fun markAsComplete(id: Int) = database.getTaskDao().markAsComplete(id)
    suspend fun markAsIncomplete(id: Int) = database.getTaskDao().markAsIncomplete(id)

    suspend fun getListIdByTaskId(id: Int) = database.getTaskDao().getListIdByTaskId(id)

    suspend fun deleteAllCompletedTasks() = database.getTaskDao().deleteAllCompletedTasks()

    suspend fun getAllTasksInListId(id: Int) = database.getTaskDao().getAllTasksInListId(id)

    suspend fun getTaskByIdAndChangePriority(id: Int, newPriority: Int) =
        database.getTaskDao().getTaskByIdAndChangePriority(id, newPriority)

    suspend fun getTaskByIdAndSetProgress(id: Int, progress: Int) =
        database.getTaskDao().getTaskByIdAndSetProgress(id, progress)

    suspend fun getTaskByIdAndSetDatetime(taskId: Int, millis: Long) =
        database.getTaskDao().getTaskByIdAndSetDatetime(taskId, millis)

    fun getAllTasksAsFlow() = database.getTaskDao().getAllTasksAsFlow()

    suspend fun getIncompleteTasksOfListId(listId: Int) = database.getTaskDao().getIncompleteTasksOfListId(listId)


    /**
     * Task List
     */
    fun getAllTaskList() = database.getTaskListDao().getAllTaskList()

    suspend fun getTaskListById(id: Int) = database.getTaskListDao().getTaskListById(id)

    suspend fun getTaskListIdByName(name: String) =
        database.getTaskListDao().getTaskListIdByName(name)

    suspend fun insertTaskList(taskList: TaskList) =
        database.getTaskListDao().insertTaskList(taskList.apply {
            if (createdAt == null) {
                createdAt = System.currentTimeMillis()
            }
            modifiedAt = System.currentTimeMillis()
        })

    suspend fun deleteTaskList(taskList: TaskList) =
        database.getTaskListDao().deleteTaskList(taskList)

    suspend fun findListByIdAndIncrementTotal(id: Int, count: Int = 1) =
        database.getTaskListDao().findListByIdAndIncrementTotal(id, count)

    suspend fun findListByIdAndDecrementTotal(id: Int, count: Int = 1) =
        database.getTaskListDao().findListByIdAndDecrementTotal(id, count)

    fun getTotalTaskListCount() = database.getTaskListDao().getTaskListCount()

}