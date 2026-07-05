package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateString(): String {
        return sdf.format(Date())
    }

    fun isTodayOrPast(dateStr: String): Boolean {
        return try {
            val date = sdf.parse(dateStr) ?: return false
            val today = sdf.parse(getTodayDateString()) ?: return false
            !date.after(today)
        } catch (e: Exception) {
            false
        }
    }

    fun isTodayOrFuture(dateStr: String): Boolean {
        return try {
            val date = sdf.parse(dateStr) ?: return false
            val today = sdf.parse(getTodayDateString()) ?: return false
            !date.before(today)
        } catch (e: Exception) {
            false
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = sdf.parse(dateStr) ?: return dateStr
            val displaySdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            displaySdf.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
