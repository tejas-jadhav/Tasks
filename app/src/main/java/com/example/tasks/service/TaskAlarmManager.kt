package com.example.tasks.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tasks.data.model.Task
import com.example.tasks.utils.getTimeFromDate
import java.util.*

class TaskAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val taskNotification = TaskNotificationManager(context)

    companion object {
        private const val TAG = "TaskAlarmManager"

    }

    private fun createPendingIntent(task: Task): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        intent.putExtra(TaskAlarmReceiver.TASK_KEY, task)

        return PendingIntent.getBroadcast(
            context,
            Int.MIN_VALUE + task.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun setAlarm(task: Task) {
        if (task.dateTime < System.currentTimeMillis()) {
            taskNotification.showNotification(task)
            return
        }

        cancelExistingAlarm(task)

        val scheduleNotification = createPendingIntent(task)


        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            task.dateTime,
            scheduleNotification
        )

        Log.e(TAG, "setAlarm: Alarm set for ${getTimeFromDate(Date(task.dateTime))}")
    }

    fun cancelExistingAlarm(task: Task) {
        val oldPendingIntent = createPendingIntent(task)
        alarmManager.cancel(oldPendingIntent)
        Log.e(TAG, "cancelExistingAlarm: Alarm cancelled for ${getTimeFromDate(Date(task.dateTime))}")
    }

    fun setAlarmForTasks(tasks: List<Task>) {
        tasks.forEach { setAlarm(it) }
    }

    fun cancelAlarmForTasks(tasks: List<Task>) {
        tasks.forEach { cancelExistingAlarm(it) }
    }



}