package com.example.tasks.utils

object Constants {
    const val BOTTOM_NAVIGATION_ANIMATION_DURATION = 100L
    const val FAB_ANIMATION_DURATION = 350L

    const val SEEKBAR_DELAY = 250L

    const val PRIORITY_HIGH = 1
    const val PRIORITY_MID = 2
    const val PRIORITY_LOW = 3

    const val PENDING = "Pending"
    const val TODAY = "Today"
    const val TOMORROW = "Tomorrow"
    const val UPCOMING = "Upcoming"

//    notification
    const val NOTIFICATION_CHANNEL_NAME = "Task Reminders"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Receive reminders about pending tasks"
    const val REQUEST_CODE_CONTENT_INTENT = 10





    fun getRandomTaskTitle(): String = listOf(
        "Feed tommy",
        "Add icons to navbar",
        "4 Laps of garden",
        "Laze around",
        "Help Sam with his Dinosaur Project",
        "Learn cartwheel",
        "Finish Chapter 3 of Calculus",
        "Learn Kotlin Coroutines",
        "To be or not to be",
        "Sanitize data in users collection",
        "Date with Jamie"
    ).random()


}