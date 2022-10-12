package com.example.tasks

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.tasks.service.TaskNotificationManager
import com.example.tasks.utils.Constants
import com.maltaisn.icondialog.pack.IconPack
import com.maltaisn.icondialog.pack.IconPackLoader
import com.maltaisn.iconpack.defaultpack.createDefaultIconPack

class TasksApplication : Application() {
    var iconPack: IconPack? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        loadIconPack()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            TaskNotificationManager.CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = Constants.NOTIFICATION_CHANNEL_DESCRIPTION

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun loadIconPack() {
        val loader = IconPackLoader(this)

        val createdIconPack = createDefaultIconPack(loader)
        createdIconPack.loadDrawables(loader.drawableLoader)

        iconPack = createdIconPack
    }


    override fun onTerminate() {
        super.onTerminate()
        iconPack = null
    }
}