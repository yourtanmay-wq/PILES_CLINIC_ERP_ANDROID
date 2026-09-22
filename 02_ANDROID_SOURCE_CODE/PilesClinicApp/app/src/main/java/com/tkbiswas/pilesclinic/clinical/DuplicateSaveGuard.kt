package com.tkbiswas.pilesclinic.clinical

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.tkbiswas.pilesclinic.native.BackgroundWork
import com.tkbiswas.pilesclinic.native.LocalWorkflowStore
import com.tkbiswas.pilesclinic.native.NoBengali
import com.tkbiswas.pilesclinic.native.PremiumAlert
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🟡🔒 V708 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত) —
 * **একই জিনিস দুবার চুপচাপ জমা হয়ে যাবে না।**
 *
 * TK-এর ছবি (Patient Timeline): ১৫.০৭.২০২৬-এ ৩.১৭ – ৩.২১-এর মধ্যে **৫টা
 * হুবহু এক Investigation**, আর ৩.০০/৩.০১-এ **২টা হুবহু এক Prescription**।
 * TK: *"এত ডুপ্লিকেট কেন? আটকায় না কেন?"* → *"Warning দেবে আটকাতে হবে"*
 * → *"cancel & Ok ২ টা ই রাখতে হবে"*।
 *
 * ✅ **আসল কারণ (কোড ধরে যাচাই, আন্দাজ নয়):** `ClinicalCloudRepository.saveMedical()`
 *    প্রতিবার একটা **নতুন `med_...` আইডি** বানিয়ে নতুন সারি লেখে — আগেরটার
 *    সঙ্গে হুবহু মিলে গেলেও কোনো যাচাই ছিল না। তার উপর Investigation পর্দায়
 *    `Save` ও `Save & Print` — **দুটো বোতামই** সেভ করে, তাই দুটোতে চাপলেই
 *    দুটো সারি। কম্পিউটারেও একই (`saveMedicalRecord`)।
 *
 * TK-এর সিদ্ধান্ত (আমার সুপারিশ, TK-অনুমোদিত): অ্যাপ **ভুল ঠেকাবে, কিন্তু
 * মালিকের হাত বাঁধবে না** —
 *   • **Cancel** → কিছুই সেভ হয় না (ডিফল্ট; অসাবধানে দুবার চাপলে এটাই বাঁচায়)
 *   • **OK** → জেনেশুনে তবুও সেভ হয় (বিরল, কিন্তু পথ খোলা থাকে)
 *
 * ⚠️ **নেটের খরচ শূন্য** — মিল খোঁজা হয় শুধু **এই ফোনে জমা থাকা** তালিকা
 *    (`LocalWorkflowStore.medicalForPatient`) থেকে; একটাও নতুন Supabase
 *    অনুরোধ যায় না (TK: *"আমি ফ্রি প্লানে চালাতে চাই"*)।
 * ⛔ এই ফাইল **কিছুই সেভ করে না** — শুধু জিজ্ঞাসা করে, আর হ্যাঁ হলে
 *    ডাকনেওয়ালার নিজের পুরোনো সেভ-কোডটাই চালায়। তাই সেভের নিয়ম,
 *    অফলাইন-তালিকা, ক্লাউড — কিছুই বদলায়নি।
 */
object DuplicateSaveGuard {

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun norm(s: String?): String =
        (s ?: "").trim().replace(Regex("\\s+"), " ").lowercase(Locale.US)

    /**
     * আজকের দিনে এই রোগীর **হুবহু একই** সারি আগে থেকে জমা আছে কিনা।
     * চারটেই মিলতে হবে: রোগী · দিন · ধরন · লেখা (selected + details)।
     * ⛔ ফাঁকা লেখায় কখনো মিল ধরা হয় না — নইলে দুটো ফাঁকা সারি এক গণ্য হত।
     */
    fun findTodaysDuplicate(
        context: android.content.Context,
        patientId: String,
        type: String,
        selected: String,
        details: String
    ): JSONObject? {
        if (patientId.isBlank()) return null
        if (norm(selected).isBlank() && norm(details).isBlank()) return null
        return try {
            val rows = LocalWorkflowStore(context).medicalForPatient(patientId)
            var found: JSONObject? = null
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                if (r.optString("date").take(10) != today()) continue
                if (!r.optString("type").equals(type, ignoreCase = true)) continue
                if (norm(r.optString("selected")) != norm(selected)) continue
                if (norm(r.optString("details")) != norm(details)) continue
                found = r    // একাধিক থাকলে শেষেরটাই দেখানো হয়
            }
            found
        } catch (_: Throwable) { null }
    }

    /**
     * মিল না থাকলে সঙ্গে সঙ্গে [proceed]; থাকলে Warning দেখিয়ে TK-কে বেছে নিতে দেয়।
     * খোঁজাটা পিছনের থ্রেডে হয় (ফাইল পড়া), তাই পর্দা এক মুহূর্তও আটকায় না।
     */
    fun run(
        activity: Activity,
        patientId: String,
        type: String,
        selected: String,
        details: String,
        proceed: () -> Unit
    ) {
        BackgroundWork.run {
            val dup = findTodaysDuplicate(activity, patientId, type, selected, details)
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                if (dup == null) proceed() else showWarning(activity, type, dup, proceed)
            }
        }
    }

    /** সময়টা মানুষের পড়ার মতো ("3.17 PM"); না পড়া গেলে ফাঁকা। */
    private fun timeOf(row: JSONObject): String = try {
        val raw = row.optString("createdAt", "")
        if (raw.length < 16) "" else {
            val hh = raw.substring(11, 13).toInt()
            val mm = raw.substring(14, 16)
            val ap = if (hh >= 12) "PM" else "AM"
            val h12 = when { hh == 0 -> 12; hh > 12 -> hh - 12; else -> hh }
            "$h12.$mm $ap"
        }
    } catch (_: Throwable) { "" }

    /* ⛔ লেখাগুলো ইচ্ছে করেই **ইংরেজি** — V691-এর একই-দিনের Remark Warning-ও
       ইংরেজিতে, আর বাংলা-বন্ধ স্টাফের ফোনেও এটা হুবহু একই দেখাবে। */
    private fun showWarning(activity: Activity, type: String, dup: JSONObject, proceed: () -> Unit) {
        val d = activity.resources.displayMetrics.density
        val pad = (18 * d).toInt()
        val body = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, (14 * d).toInt(), pad, 0)
        }
        val whenWho = listOf(timeOf(dup), dup.optString("createdBy", ""))
            .filter { it.isNotBlank() }.joinToString(" · ")
        body.addView(android.widget.TextView(activity).apply {
            text = if (whenWho.isBlank()) "ALREADY SAVED" else "ALREADY SAVED · $whenWho"
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#8A5A00"))
        })
        val shown = listOf(dup.optString("selected", ""), dup.optString("details", ""))
            .filter { it.isNotBlank() }.joinToString(" — ")
        body.addView(android.widget.TextView(activity).apply {
            text = shown
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#3A4A63"))
            setPadding((11 * d).toInt(), (10 * d).toInt(), (11 * d).toInt(), (10 * d).toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 10 * d
                setColor(android.graphics.Color.parseColor("#FFFBF0"))
                setStroke((1 * d).toInt(), android.graphics.Color.parseColor("#EBD9A8"))
            }
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = (6 * d).toInt()
            layoutParams = lp
        })
        body.addView(android.widget.TextView(activity).apply {
            text = "The same $type is already saved for this patient today.\n\n" +
                "Cancel  -  do not save again (recommended)\n" +
                "OK  -  save it anyway"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#10223A"))
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = (14 * d).toInt()
            layoutParams = lp
        })
        AlertDialog.Builder(activity)
            /* ⛔ শিরোনামে "⚠" — প্রজেক্টের লক করা নিয়মে ওটাই হলুদ (সতর্কতা)
                  হেডার আনে (PremiumAlert.severityOf)। */
            .setCustomTitle(PremiumAlert.header(activity, "⚠ Already saved today"))
            .setView(body)
            .setPositiveButton("OK") { _, _ -> proceed() }
            .setNegativeButton("Cancel", null)
            .show().also { dlg ->
                PremiumAlert.paint(dlg)
                try { NoBengali.installDialog(dlg) } catch (_: Throwable) { }
            }
    }
}
