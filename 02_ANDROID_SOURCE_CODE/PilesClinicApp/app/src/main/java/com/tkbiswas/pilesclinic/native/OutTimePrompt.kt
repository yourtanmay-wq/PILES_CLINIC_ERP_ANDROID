package com.tkbiswas.pilesclinic.native

import android.app.Activity
import android.content.Context
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * ⏰🔒 V1166 (০৭.০৯.২০২৬) — **OUT TIME না চাপলে জোর করে জিজ্ঞাসা।**
 *
 * TK-এর নির্দেশ (খাতার সারি ২৭০-এ পুরো আলোচনা):
 *  · সন্ধ্যা **৭.৩০ PM**-এ OUT TIME না চাপলে জোর করে পপ-আপ
 *  · সেখানে **আর কতক্ষণ থাকবে (সময়)** ও **কারণ** — দুটোই লিখতেই হবে
 *  · বসানো সময় **পেরিয়ে গেলে আবার একই পপ-আপ**, যতক্ষণ না OUT TIME চাপে
 *
 * ⛔ পপ-আপটা **বাতিল করা যায় না** (বাইরে চাপলে বা Back চাপলে বন্ধ হয় না) —
 *    TK: *"জোর করে নোটিফিকেশন দেবে"*।
 * ⛔ হাজিরার কোনো ঘর এখান থেকে লেখা হয় না — IN/OUT TIME-এর নিয়ম অটুট।
 *    শুধু **কতক্ষণ থাকবে** সময়টা ফোনে মনে রাখা হয় (আবার কখন জিজ্ঞাসা করবে
 *    সেটা ঠিক করতে), আর কারণটা মাস্টারের নোটিশ-বোর্ডে পাঠানো হয়।
 * ⛔ শুধু তখনই ওঠে যখন স্টাফ **আজ IN TIME দিয়েছেন কিন্তু OUT TIME দেননি** —
 *    ছুটির দিনে বা না-আসা দিনে কখনো ওঠে না।
 */
object OutTimePrompt {

    private const val PREF = "piles_out_time_prompt"
    /** TK-নির্দেশ: সন্ধ্যা ৭.৩০ PM। */
    private const val ASK_HOUR = 19
    private const val ASK_MINUTE = 30

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun today(): String = FollowUpModel.today()

    private fun nowMinutes(): Int {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /** স্টাফ যে সময় পর্যন্ত থাকবেন বলেছেন (মিনিটে); বলা না থাকলে `-1`। */
    private fun stayUntil(context: Context): Int {
        val p = prefs(context)
        if (p.getString("date", "") != today()) return -1
        return p.getInt("until", -1)
    }

    /**
     * এখন জিজ্ঞাসা করা দরকার কি না।
     * @param hasIn আজ IN TIME দেওয়া আছে কি
     * @param hasOut আজ OUT TIME দেওয়া আছে কি
     */
    fun shouldAsk(context: Context, hasIn: Boolean, hasOut: Boolean): Boolean {
        if (!hasIn || hasOut) return false
        /* 🏍️🔒 V1244 (০৮.০৯.২০২৬, TK-নির্দেশ হুবহু): *"সন্ধ্যা সাড়ে সাতটা
           বাজলে স্টাফকে জোর করে বাড়ি পাঠানো হবে — কিন্তু রূপমকে ছাড়া। কারণ সে
           গ্রামে গ্রামে গিয়ে RMP ডাক্তারদের কাছে ভিজিট করে; কোনদিন সন্ধ্যাতে,
           কোনদিন রাত্রেও বাড়িতে আসতে পারে।"*
           ⇒ মাঠে ঘোরা স্টাফের (এখন রূপম) কাছে এই পপ-আপটা আর আসে না।
           ⛔ চেনা হয় প্রকল্পের **আগে থেকে প্রমাণিত** `FieldVisit.isFieldStaff()`
              দিয়েই — নতুন কোনো তালিকা বা নম্বর বসানো হয়নি।
           ⛔ বাকি সবার জন্য ৭.৩০-এর নিয়ম **এক অক্ষরও বদলায়নি**। */
        try {
            if (FieldVisit.isFieldStaff(context)) return false
        } catch (_: Throwable) { }
        val now = nowMinutes()
        if (now < ASK_HOUR * 60 + ASK_MINUTE) return false
        val until = stayUntil(context)
        // এখনো কিছু বলেননি ⇒ জিজ্ঞাসা। বলা সময় পেরিয়ে গেছে ⇒ আবার জিজ্ঞাসা।
        return until < 0 || now >= until
    }

    /** "20:30" রূপে দেখানোর জন্য নয় — TK-র লক করা চেহারা `8.30 PM`। */
    private fun clock(minutes: Int): String {
        val h = minutes / 60; val m = minutes % 60
        val ap = if (h < 12) "AM" else "PM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        return String.format(Locale.US, "%d.%02d %s", h12, m, ap)
    }

    /**
     * পপ-আপটা দেখায়। সময় ও কারণ দুটোই না লিখলে বন্ধ হয় না।
     * @param onMarkOut "OUT TIME now" চাপলে যা চলবে (হাজিরার পুরনো পথ)।
     */
    fun show(activity: Activity, staffCode: String, branch: String, byMobile: String, onMarkOut: () -> Unit) {
        if (activity.isFinishing) return
        val d = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val col = android.widget.LinearLayout(activity).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(18), dp(6), dp(18), dp(2))
        }
        col.addView(android.widget.TextView(activity).apply {
            text = "OUT TIME is not marked yet."
            textSize = 13.5f
            setTextColor(android.graphics.Color.parseColor("#33404F"))
        })
        // সময় — ঘড়ি থেকে বাছা, টাইপ করতে হয় না
        var picked = -1
        val timeBtn = android.widget.TextView(activity).apply {
            text = "Tap to pick — how long will you stay?"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0E6E8C"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
        }
        col.addView(timeBtn, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })
        val reason = android.widget.EditText(activity).apply {
            hint = "Why are you still here?"
            textSize = 14f
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            minLines = 2
            gravity = android.view.Gravity.TOP
            /* ⛔ পাহারা [৯.১৭] — নম্বরের কি-বোর্ড এখানে নয়, এটা সাধারণ লেখা। */
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        col.addView(reason, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) })

        val dlg = androidx.appcompat.app.AlertDialog.Builder(activity)
            .setCustomTitle(PremiumAlert.header(activity, "Still at the chamber?"))
            .setView(col)
            .setCancelable(false)                       // ⛔ TK: জোর করে
            .setPositiveButton("Save", null)            // নিচে নিজে বসানো হয়
            .setNegativeButton("OUT TIME now", null)
            .create()
        dlg.setCanceledOnTouchOutside(false)
        timeBtn.setOnClickListener {
            val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            android.app.TimePickerDialog(activity, { _, h, m ->
                picked = h * 60 + m
                timeBtn.text = "Staying until  " + clock(picked)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
        }
        dlg.show()
        try { PremiumAlert.paint(dlg) } catch (_: Throwable) { }
        try { NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }
        /* ⛔ বোতামের কাজটা `show()`-এর পরে বসানো — নইলে AlertDialog নিজেই
           পপ-আপ বন্ধ করে দিত, আর সময়/কারণ ছাড়াই বেরিয়ে যাওয়া যেত।
           প্রকল্পের প্রমাণিত কৌশল (PaymentActivity-র delete-confirm-এ একই)। */
        dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val why = reason.text?.toString()?.trim().orEmpty()
            if (picked < 0) {
                android.widget.Toast.makeText(activity, "Pick the time first", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (why.isBlank()) {
                android.widget.Toast.makeText(activity, "Write the reason", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs(activity).edit()
                .putString("date", today()).putInt("until", picked).apply()
            /* মাস্টার যেন জানতে পারেন — প্রকল্পের আগে থেকেই থাকা নোটিশ-বোর্ডে।
               ⛔ ব্যর্থ হলে নিঃশব্দে বাদ; স্টাফের কাজ এর জন্য আটকায় না। */
            val ctx = activity.applicationContext
            BackgroundWork.run {
                try {
                    BriefingRepository().post(
                        ctx, "Staff still at chamber",
                        "👤 Staff : " + staffCode + "\n🏥 Branch : " + branch +
                            "\n⏰ Staying until : " + clock(picked) + "\nReason : " + why,
                        /* ⛔ "Staff IN TIME" নোটিশের হুবহু একই পথ ও একই ক্রম
                           (WorkNotebookActivity:1431) — নতুন কিছু বানানো হয়নি। */
                        "role", branch, "master", byMobile
                    )
                } catch (_: Throwable) { }
            }
            try { dlg.dismiss() } catch (_: Throwable) { }
            android.widget.Toast.makeText(activity,
                "OK — we will ask again at " + clock(picked), android.widget.Toast.LENGTH_LONG).show()
        }
        dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener {
            try { dlg.dismiss() } catch (_: Throwable) { }
            onMarkOut()
        }
    }
}
