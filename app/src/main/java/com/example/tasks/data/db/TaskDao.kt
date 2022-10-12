package com.example.tasks.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tasks.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("select * from tasks where id = :id")
    suspend fun getTaskById(id: Int): Task

    @Query("delete from tasks where isCompleted = 1")
    suspend fun deleteAllCompletedTasks()

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dateTime, priorityId")
    suspend fun getAllTasksToDo(): List<Task>

    @Query("SELECT * FROM tasks ORDER BY dateTime, priorityId")
    fun getAllTasksAsFlow(): Flow<List<Task>>

    @Query("select * from tasks where isCompleted = 1 order by dateTime, priorityId")
    suspend fun getAllCompletedTasks(): List<Task>

    @Query("update tasks set isCompleted = 1, progress = 100 where id = :id")
    suspend fun markAsComplete(id: Int)

    @Query("update tasks set isCompleted = 0, progress = null where id = :id")
    suspend fun markAsIncomplete(id: Int)

    @Query("update tasks set priorityId = :newPriority where id = :id")
    suspend fun getTaskByIdAndChangePriority(id: Int, newPriority: Int)

    @Query("update tasks set progress = :progress where id = :id")
    suspend fun getTaskByIdAndSetProgress(id: Int, progress: Int)


    @Query("update tasks set dateTime = :millis where id = :id")
    suspend fun getTaskByIdAndSetDatetime(id: Int, millis: Long)

    @Query("SELECT COUNT(*) FROM tasks")
    fun getTotalTaskCount(): LiveData<Int>

    @Query("select count(*) from tasks where isCompleted = 1")
    fun getTotalCompletedTaskCount(): LiveData<Int>


    @Query("SELECT listId FROM tasks WHERE id = :id")
    suspend fun getListIdByTaskId(id: Int): Int

    @Query("select * from tasks where listId = :listId")
    suspend fun getAllTasksInListId(listId: Int): List<Task>

    @Query("select * from tasks where listId = :listId and isCompleted = 0")
    suspend fun getIncompleteTasksOfListId(listId: Int): List<Task>


}