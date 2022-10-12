package com.example.tasks.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tasks.data.model.Priority
import com.example.tasks.data.model.Task
import com.example.tasks.data.model.TaskList
import kotlinx.coroutines.*

@Database(
    entities = [Priority::class, TaskList::class, Task::class],
    version = 1,
    exportSchema = false
)
abstract class TasksDatabase : RoomDatabase() {

    abstract fun getTaskListDao(): TaskListDao
    abstract fun getTaskDao(): TaskDao


    private fun populateInitialData() {
        CoroutineScope(Dispatchers.IO).launch {
            runInTransaction {
                CoroutineScope(Dispatchers.IO).launch {
                    val initialLists = TaskList.getInitialList()
                    if (getTaskListDao().getTaskListCountSuspend() == 0) {
                        for (taskList in initialLists) {
                            getTaskListDao().insertTaskList(taskList)
                        }
                    }
                }
            }
        }
    }

    companion object {

        private var INSTANCE: TasksDatabase? = null

        fun getDatabase(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context,
                TasksDatabase::class.java,
                "task_db.db"
            ).build().also {
                INSTANCE = it
                it.populateInitialData()
            }
        }


    }

}