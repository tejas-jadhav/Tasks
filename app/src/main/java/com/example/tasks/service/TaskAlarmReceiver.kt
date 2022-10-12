package com.example.tasks.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tasks.data.model.Task

class TaskAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "TaskAlarmReceiver"
        const val TASK_KEY = "hey-this-is-the-task-key"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getSerializableExtra(TASK_KEY) as? Task ?: return

        Log.e(TAG, "onReceive: Received task ${task.title}")

        TaskNotificationManager(context).showNotification(task)
    }
}