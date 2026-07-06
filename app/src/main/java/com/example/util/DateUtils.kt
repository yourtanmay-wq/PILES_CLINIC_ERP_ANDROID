package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sdfDDMMYYYY = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

    fun getTodayDateString(): String {
        return sdf.format(Date())
    }

    fun getTodayDateStringDDMMYYYY(): String {
        return sdfDDMMYYYY.format(Date())
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

    fun isTodayOrPastDDMMYYYY(dateStr: String): Boolean {
        return try {
            val date = sdfDDMMYYYY.parse(dateStr) ?: return false
            val today = sdfDDMMYYYY.parse(getTodayDateStringDDMMYYYY()) ?: return false
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

    fun isTodayOrFutureDDMMYYYY(dateStr: String): Boolean {
        return try {
            val date = sdfDDMMYYYY.parse(dateStr) ?: return false
            val today = sdfDDMMYYYY.parse(getTodayDateStringDDMMYYYY()) ?: return false
            !date.before(today)
        } catch (e: Exception) {
            false
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val date = try {
                sdfDDMMYYYY.parse(dateStr)
            } catch (e: Exception) {
                sdf.parse(dateStr)
            } ?: return dateStr
            val displaySdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            displaySdf.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getFullBranchName(branch: String): String {
        return when (branch.trim().uppercase()) {
            "KNE" -> "Kishanganj"
            "JPE" -> "Jalpaiguri"
            "COB" -> "Cooch Behar"
            "FLK" -> "Falakata"
            "BIR" -> "Birpara"
            else -> branch
        }
    }
}
