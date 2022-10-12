package com.example.tasks.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tasks.data.db.TaskDao
import com.example.tasks.data.db.TasksDatabase
import com.example.tasks.data.model.Task
import com.example.tasks.data.repository.TasksRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class TaskNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "TaskNotificationReceiver"
        const val TASK_ID = "message"
        const val ACTION = "thisisanactionkey"
        const val BUNDLE = "thisisthebundlekey"
    }


    sealed class Action : java.io.Serializable {
        object MarkAsCompleted : Action()
        object Delay : Action()
    }


    override fun onReceive(context: Context, intent: Intent) {

        val id = intent.getIntExtra(TASK_ID, -1)

        val repository = TasksRepository(TasksDatabase.getDatabase(context))
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val cancelNotification = {
            notificationManager.cancel(id)
        }

        when (intent.getSerializableExtra(ACTION) as? Action) {
            is Action.Delay -> delayTask(id, repository, cancelNotification)
            is Action.MarkAsCompleted -> markAsCompleted(id, repository, cancelNotification)
            else -> Log.e(TAG, "onReceive: Oopsie null action")
        }
    }

    private fun markAsCompleted(taskId: Int, repository: TasksRepository, onComplete: () -> Unit = {}) {
        Log.e(TAG, "markAsCompleted: id of task is $taskId")
        CoroutineScope(Dispatchers.IO).launch {
            repository.markAsComplete(taskId)
            onComplete()
        }
    }

    private fun delayTask(taskId: Int, repository: TasksRepository, onComplete: () -> Unit = {}) {
        Log.e(TAG, "delayTask: id of task is $taskId")
        val delay = 10 * 60 * 1000
        CoroutineScope(Dispatchers.IO).launch {
            repository.getTaskByIdAndSetDatetime(taskId, System.currentTimeMillis() + delay)
            onComplete()
        }
    }

}