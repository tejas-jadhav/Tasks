package com.example.tasks.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.tasks.data.model.TaskList

@Dao
abstract class TaskListDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertTaskList(taskList: TaskList): Long

    @Query("SELECT * FROM task_list")
    abstract fun getAllTaskList(): LiveData<List<TaskList>>

    @Query("SELECT id FROM task_list WHERE name=:name")
    abstract suspend fun getTaskListIdByName(name: String): Int

    @Query("SELECT COUNT(*) FROM task_list")
    abstract fun getTaskListCount(): LiveData<Int>

    @Query("select count(*) from task_list")
    abstract suspend fun getTaskListCountSuspend(): Int


    @Query("SELECT * FROM task_list WHERE id=:id")
    abstract suspend fun getTaskListById(id: Int): TaskList

    @Delete
    abstract suspend fun deleteTaskList(taskList: TaskList)

    @Query("UPDATE task_list set items = items + :count WHERE id=:id")
    abstract suspend fun findListByIdAndIncrementTotal(id: Int, count: Int = 1)

    @Query("UPDATE task_list set items = items - :count where id=:id")
    abstract suspend fun findListByIdAndDecrementTotal(id: Int, count: Int = 1)



}