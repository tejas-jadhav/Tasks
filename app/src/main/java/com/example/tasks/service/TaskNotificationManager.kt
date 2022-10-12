package com.example.tasks.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.tasks.R
import com.example.tasks.data.model.Task
import com.example.tasks.ui.NotificationResponseActivity

class TaskNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    private fun getActivityPendingIntent(task: Task): PendingIntent {
        val intent = Intent(context, NotificationResponseActivity::class.java)
        intent.putExtra(NotificationResponseActivity.TASK_KEY, task)

        return PendingIntent.getActivity(
            context,
            Int.MAX_VALUE - task.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun taskActionPendingIntent(
        id: Int,
        notificationAction: TaskNotificationReceiver.Action
    ): PendingIntent {
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        intent.putExtra(TaskNotificationReceiver.TASK_ID, id)
        intent.putExtra(TaskNotificationReceiver.ACTION, notificationAction)

        val requestCode = when (notificationAction) {
            TaskNotificationReceiver.Action.Delay -> -id
            TaskNotificationReceiver.Action.MarkAsCompleted -> id
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }


    fun showNotification(
        task: Task,
        title: String = task.title,
        message: String = task.description
    ) {
        val openActivity = getActivityPendingIntent(task)
        val id = task.id
        val markAsComplete = taskActionPendingIntent(id, TaskNotificationReceiver.Action.MarkAsCompleted)
        val delayBy10Minutes = taskActionPendingIntent(id, TaskNotificationReceiver.Action.Delay)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_check_circle_24)
            .setContentTitle(title)
            .setContentText(message.ifBlank { "Complete your task !" })
            .setContentIntent(openActivity)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_baseline_check_24,
                "Mark as completed",
                markAsComplete
            )
            .addAction(
                R.drawable.ic_baseline_alarm_24,
                "Delay by 10 minutes",
                delayBy10Minutes
            )
            .build()


        Log.e("TaskNotification", "showNotification: task.id = ${task.id}")

        notificationManager.notify(task.id, notification)
    }


    companion object {
        const val CHANNEL_ID = "task-notification-channel"
    }
}