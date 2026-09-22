package com.tkbiswas.pilesclinic.modules

import org.json.JSONObject

/**
 * 📋🔒 V1435 (১৩.০৯.২০২৬, তালিকা ৫৫৪, TK "পাশ") — Briefing-এর "View" থেকে
 * Daily/Monthly Report দেখানোর লেখা।
 *
 * · নতুন রিপোর্টে (V1435-এর পরে) পাঠানো লেখাটা হুবহু `wn.work_reports.report_text`-এ
 *   জমা থাকে — সেটাই দেখানো হয়।
 * · পুরনো রিপোর্টে ওই ঘর ফাঁকা — তখন এখানে সংখ্যা (auto_stats) + সেদিনের খাতা
 *   (wn.notebook_days: IN/OUT · ছুটি · Notes) থেকে **একই ধাঁচে** লেখাটা বানানো হয়।
 *   যেসব ঘর জমা ছিল না (যেমন Today Patient — স্টাফ হাতে লিখত, খাতায় থাকে না)
 *   সেখানে সৎভাবে "-" থাকে, আন্দাজ নয়।
 * ⛔ এটা শুধু দেখানোর; কোনো হিসাব/সেভ ছোঁয় না। WorkNotebookActivity-র নিজের
 *    রিপোর্ট-লেখার নিয়ম এক অক্ষরও বদলায়নি।
 */
object ReportTextBuilder {

    private fun s(o: JSONObject?, key: String): String {
        if (o == null || o.isNull(key)) return ""
        val v = o.optString(key, "").trim()
        return if (v.equals("null", true)) "" else v
    }

    private fun num(o: JSONObject?, key: String): String {
        if (o == null || o.isNull(key)) return "-"
        return try { o.getInt(key).toString() } catch (_: Throwable) { s(o, key).ifBlank { "-" } }
    }

    fun dotDate(iso: String): String {
        val p = iso.split("-")
        return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
    }

    /** "HH:mm" বা "HH:mm:ss" → "9.30 AM" (WorkNotebook-এর displayTime12-এর হুবহু নিয়ম)। */
    fun time12(hhmm: String): String {
        if (hhmm.isBlank()) return ""
        val p = hhmm.split(":"); if (p.size != 2 && p.size != 3) return hhmm
        val h24 = p[0].toIntOrNull() ?: return hhmm
        val ampm = if (h24 < 12) "AM" else "PM"
        val h12 = when { h24 == 0 -> 12; h24 > 12 -> h24 - 12; else -> h24 }
        return "$h12.${p[1]} $ampm"
    }

    /** পুরনো দৈনিক রিপোর্ট — stats + notebook_days সারি (null হতে পারে) থেকে। */
    fun daily(staffCode: String, dateIso: String, stats: JSONObject?, day: JSONObject?, manualSummary: String): String {
        val t = StringBuilder()
        t.append("Daily Report ").append(dotDate(dateIso)).append("\nStaff: ").append(staffCode).append("\n")
        if (day != null && day.optBoolean("is_leave", false)) {
            t.append("🏖️ On Leave: ").append(s(day, "leave_reason")).append("\n")
        } else {
            t.append("IN TIME- ").append(time12(s(day, "check_in")).ifBlank { "-" }).append("\n")
            t.append("OUT TIME ").append(time12(s(day, "check_out")).ifBlank { "-" }).append("\n")
        }
        t.append("\nNew Enquiry: ").append(num(stats, "enquiries"))
            .append("\nRegistration: ").append(num(stats, "registrations"))
            .append("\nToday Patient: ").append(num(stats, "patients"))
            .append("\nApp Calls: ").append(num(stats, "appCalls"))
            .append("\nOutside Calls: ").append(
                if (stats != null && !stats.isNull("outsideCalls")) num(stats, "outsideCalls")
                else if (day != null && !day.isNull("outside_calls_manual")) num(day, "outside_calls_manual") else "-")
            .append("\nTotal call : ").append(num(stats, "totalCalls"))
        val notes = s(day, "day_note").ifBlank { manualSummary.trim() }
        if (notes.isNotBlank()) t.append("\n\nNotes: \n").append(notes)
        return t.toString()
    }

    /** পুরনো মাসিক রিপোর্ট — stats থেকে (WorkNotebook-এর "monthly" ধাঁচ)। */
    fun monthly(staffCode: String, ym: String, stats: JSONObject?): String {
        return "Monthly Report " + ym + "\nStaff: " + staffCode + "\n\n" +
            "New Enquiry: " + num(stats, "enquiries") + "\nRegistration: " + num(stats, "registrations") +
            "\nApp Calls: " + num(stats, "appCalls") + " | Outside Calls: " + num(stats, "outsideCalls") +
            " | Total: " + num(stats, "totalCalls") + "\nLeave Days: " + num(stats, "leaveDays")
    }
}
