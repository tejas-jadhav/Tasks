package com.example.tasks.utils

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

const val DATE_FORMAT = "dd-MM-yyyy"
const val TIME_FORMAT = "hh:mm aa"

fun getStringFromDate(date: Date): String =
    SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(date)

fun getStringFromDate(year: Int, month: Int, date: Int): String {
    val dateObj = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DATE, date)
    }.time

    return getStringFromDate(dateObj)
}

fun getDayFromDate(date: Date): Int {
    return Calendar.getInstance().apply {
        time = date
    }.get(Calendar.DATE)
}

fun getMonthFromDate(date: Date): Int {
    return Calendar.getInstance().apply {
        time = date
    }.get(Calendar.MONTH)
}
fun getYearFromDate(date: Date): Int {
    return Calendar.getInstance().apply {
        time = date
    }.get(Calendar.YEAR)
}

fun getHourOfDayFromDate(date: Date): Int {
    return Calendar.getInstance().apply {
        time = date
    }.get(Calendar.HOUR_OF_DAY)
}

fun getMinuteFromDate(date: Date): Int {
    return Calendar.getInstance().apply {
        time = date
    }.get(Calendar.MINUTE)
}

fun getHourOfDayNow(): Int {
    return getHourOfDayFromDate(Date())
}
fun getMinutesNow(): Int {
    return getMinuteFromDate(Date())
}

fun getDateFromDateString(dateString: String): Date? {
    return SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).parse(dateString)
}

fun isSameDay(date1: Date, date2: Date): Boolean {
    val localDate1 = date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val localDate2 = date2.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    return localDate1.isEqual(localDate2)
}

fun getDateDifferenceInDays(date1: Date, date2: Date): Long {
    val diff = date2.time - date1.time
    return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
}

fun mergeDateTimeStringToDate(dateString: String, timeString: String): Date {
    val date = getDateFromDateString(dateString)!!
    val time = getDateFromTimeString(timeString)!!
    val calendarTime = Calendar.getInstance()
    calendarTime.time = time


    return Calendar.getInstance().apply {
        val hour = calendarTime.get(Calendar.HOUR_OF_DAY)
        val minutes = calendarTime.get(Calendar.MINUTE)
        setTime(date)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minutes)
    }.time

}


fun getDateFromTimeString(timeString: String): Date? {
    return SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).parse(timeString)
}

fun getTimeFromDate(date: Date): String =
    SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(date)

fun getTimeFromDate(hours: Int, minutes: Int): String {
    val dateObj = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hours)
        set(Calendar.MINUTE, minutes)
    }.time

    return getTimeFromDate(dateObj)
}


fun getTimeDifferenceInString(millis: Long): String {
    return "In 2 hrs"
}
