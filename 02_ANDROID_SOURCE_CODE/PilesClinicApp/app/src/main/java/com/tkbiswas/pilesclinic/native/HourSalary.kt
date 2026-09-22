package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * ⏱️🔒 V1166 (০৭.০৯.২০২৬) — **ঘণ্টা হিসাবে বেতন।**
 *
 * TK-এর সঙ্গে পুরো আলোচনা করে ঠিক হওয়া নিয়ম (খাতায় সারি ২৭০-এ লেখা):
 *  · দিনের সময় **১০টা – ৫টা = ৭ ঘণ্টা**
 *  · মাসের ঘণ্টা = **ওই মাসের আসল দিন × ৭** (২৮ → ১৯৬ · ৩০ → ২১০ · ৩১ → ২১৭)
 *    TK: *"ওই মাসের আসল দিনই ধরুন"* — তাহলে পুরো মাস কাজ করলে ঠিক সেট করা
 *    বেতনটাই পাওয়া যায়; ৩০ দিন ধরলে ৩১ দিনের মাসে বেশি, ফেব্রুয়ারিতে কম হত।
 *  · ঘণ্টার দর = **যার যা বেতন সেট আছে ÷ ওই মাসের ঘণ্টা**
 *  · কাজের ঘণ্টা = প্রতিদিন **OUT TIME − IN TIME**-এর যোগ
 *  · বেতন = মোট ঘণ্টা × দর ⇒ **কম কাজ = কম, বেশি কাজ = বেশি**
 *  · **মঞ্জুর হওয়া ছুটির দিন ৭ ঘণ্টা** ধরা হয় (বেতন কমে না)
 *  · IN বা OUT — যেকোনো একটা না চাপলে ওই দিন **০ ঘণ্টা**; মাস্টার
 *    **Fix Attendance** দিয়ে ঠিক করে দিলে ফিরে পাওয়া যায় (TK-র বাছা পথ "খ")
 *
 * ⛔ **এই ফাইল কোনো টাকা লেখে না, কোনো সিদ্ধান্ত নেয় না** — শুধু হিসাব করে
 *    ফেরত দেয়। TK-এর নির্দেশ: *"প্রথমে শুধু দেখানো, টাকা কাটা নয়"*।
 * ⛔ **আগামী মাস থেকে** (TK: *"বিগত দিনের হিসাব ধরবেন না"*) — কোন মাসে দেখানো
 *    হবে সেটা ডাকার জায়গা ঠিক করে (`startsFrom`)।
 * ⛔ সময় দুই ধাঁচেই জমা থাকে — `HH:mm` (রোজকার) ও `HH:mm:ss` (Fix Attendance),
 *    তাই দুটোই পড়া হয়। চেনা না গেলে ওই দিন গোনায় ঢোকে না।
 */
object HourSalary {

    /** TK-নির্দেশ: ১০টা – ৫টা। */
    const val DAY_HOURS = 7.0
    private const val DAY_MINUTES = (DAY_HOURS * 60).toInt()

    /**
     * যে মাস থেকে এই নিয়ম চালু।
     *
     * 📅🔒 V1178 (০৭.০৯.২০২৬, TK-নির্দেশ) — TK: *"হ্যাঁ, সেপ্টেম্বর থেকেই
     * চালু হবে"* · *"স্যালারি হবে 01/09/2026 থেকে"*।
     *
     * আগে এটা **আসছে মাস** হিসাব করত (V1166-এ TK বলেছিলেন *"বিগত দিনের হিসাব
     * ধরবেন না, আগামী মাস থেকে হবে"*)। TK নিজে সেপ্টেম্বরের সংখ্যাটা মিলিয়ে
     * দেখে (COB-UTTAMA — ৪৪ঘ ৫২মি · ₹১,৪৯৬, হাতে গুনে হুবহু মিলেছে) সিদ্ধান্ত
     * বদলেছেন। তাই এখন **বাঁধা `2026-09`**।
     *
     * ⛔ বাঁধা লেখা রাখা হয়েছে ইচ্ছে করে — "আসছে মাস" রাখলে অক্টোবরে গিয়ে
     *    নিয়মটা আবার পিছিয়ে যেত, আর সেপ্টেম্বরের হিসাব হারিয়ে যেত।
     */
    const val STARTS_FROM = "2026-09"

    fun startsFrom(): String = STARTS_FROM

    /** "2026-10" → ওই মাসে কত দিন। চেনা না গেলে ০। */
    fun daysInMonth(ym: String): Int = try {
        val p = ym.trim().split("-")
        val y = p[0].toInt(); val m = p[1].toInt()
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        c.set(y, m - 1, 1)
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    } catch (_: Throwable) { 0 }

    /** ওই মাসের মোট ঘণ্টা = আসল দিন × ৭। */
    fun monthHours(ym: String): Double = daysInMonth(ym) * DAY_HOURS

    /** `"09:15"` বা `"09:15:00"` → মধ্যরাত থেকে কত মিনিট। চেনা না গেলে `null`। */
    fun minutesOf(raw: String?): Int? {
        val t = (raw ?: "").trim()
        if (t.isBlank()) return null
        val p = t.split(":")
        if (p.size != 2 && p.size != 3) return null
        val h = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        if (h < 0 || h > 23 || m < 0 || m > 59) return null
        return h * 60 + m
    }

    /* 🐞🔒 V1455 (১৪.০৯.২০২৬) — নিচের compute()-এর "IN আছে, OUT নেই ⇒ ৭ ঘণ্টা"
       নিয়মটা TK যা বলেছিলেন তার জন্যই ("সেই দিন যদি আউট টাইম চাপতে ভুলে যায়") —
       অর্থাৎ দিন **শেষ হয়ে যাওয়ার পরেও ভুলে যাওয়া**, আজকের এখনো-চলতে-থাকা দিনের
       জন্য নয়। আজকের তারিখেই এখনো OUT না থাকলে সেটা যোগ হয় Calendar.YEAR
       ইত্যাদি দিয়ে যাচাই করে (নিচে)। */
    private fun todayIso(): String {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return String.format(Locale.US, "%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }
    private fun nowMinutesIst(): Int {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /**
     * এক মাসের হিসাব।
     * @param days ওই স্টাফের ওই মাসের `wn.notebook_days` সারিগুলো
     * @param salaryAmount ওই স্টাফের সেট করা মাসিক বেতন
     */
    /* ⛔ নামটা ইচ্ছে করে `HourPay` — `Result` রাখলে প্রকল্পের অন্য ফাইলের
       (যেমন SyncWorker-এর androidx.work `Result`) সঙ্গে নাম গুলিয়ে যেত;
       পাহারা সঙ্গে সঙ্গে ধরিয়ে দিয়েছে। */
    data class HourPay(
        val ym: String,
        val monthHours: Double,
        val ratePerHour: Double,
        val workedMinutes: Int,
        val leaveDays: Int,
        val missingDays: Int,
        val payable: Double
    ) {
        val workedHours: Double get() = workedMinutes / 60.0
    }

    fun compute(days: JSONArray?, salaryAmount: Double, ym: String): HourPay {
        val mh = monthHours(ym)
        val rate = if (mh > 0.0) salaryAmount / mh else 0.0
        var worked = 0
        var leaves = 0
        var missing = 0
        if (days != null) {
            for (i in 0 until days.length()) {
                val d = days.optJSONObject(i) ?: continue
                /* মঞ্জুর হওয়া ছুটি ⇒ পুরো দিনের ৭ ঘণ্টা (TK-নির্দেশ)।
                   ⛔ `is_leave` বসেই তখন, যখন ছুটিটা মঞ্জুর হয়েছে
                      (LeaveRepository.approve · অ্যাপের নিজে-মঞ্জুর) — তাই
                      নামঞ্জুর ছুটি এখানে কখনো ঢোকে না। */
                if (d.optBoolean("is_leave", false)) { leaves++; worked += DAY_MINUTES; continue }
                /* 🏠🔒 V1180 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"Work from Home…
                   মাস্টার অনুমতি দিলে সেটা 7 ঘন্টাই হিসাব করা হবে, কম বেশি হিসাবে
                   সেদিনের জন্য হবে না"* — অর্থাৎ সকাল ১০টা–বিকেল ৫টা ধরাই হবে।
                   ⇒ মাস্টারের অনুমোদিত Work From Home দিনে **সবসময় ৭ ঘণ্টা**,
                     ওই দিনের IN/OUT TIME যা-ই থাক।
                   ⛔ `is_wfh` কেবল মাস্টার Approve করলেই বসে (WfhRequests →
                      WorkNotebookActivity), তাই না-মঞ্জুর অনুরোধ এখানে ঢোকে না।
                   ⛔ ছুটির (is_leave) নিয়ম উপরে অক্ষত; বাকি দিনগুলোর হিসাবও
                      এক অক্ষরও বদলায়নি। */
                if (d.optBoolean("is_wfh", false)) { worked += DAY_MINUTES; continue }
                /* 🚌🔒 V1200 (০৮.০৯.২০২৬, TK-সিদ্ধান্ত): *"অন্য ব্রাঞ্চে গিয়ে ডিউটি
                   করলে পুরো ৭ ঘন্টায় ধরা হবে"* — স্টাফ আগে থেকে জানিয়ে রাখলে
                   (`is_other_branch`) ওই দিন যাতায়াতের সময়ের জন্য বেতন কমে না।
                   ⛔ ঘরটা বসে **কেবল আগাম জানানো থাকলে** (Plan My Day), তাই
                      এমনি অন্য ব্রাঞ্চে গেলে আগের নিয়মই (আসল IN/OUT)। */
                if (d.optBoolean("is_other_branch", false)) { worked += DAY_MINUTES; continue }
                val a = minutesOf(d.optString("check_in", ""))
                val b = minutesOf(d.optString("check_out", ""))
                /* ⏰🔒 V1200 (০৮.০৯.২০২৬, TK-সিদ্ধান্ত, হুবহু): *"চেম্বারে আসার পরে
                   in time চাপবে, কিন্তু সেই দিন যদি আউট টাইম চাপতে ভুলে যায়
                   তাহলে সেই ক্ষেত্রেও ৭ ঘন্টার বেশি ধার্য করা চলবে না"*।
                   ⇒ **IN আছে অথচ OUT নেই** ⇒ ওই দিন ঠিক **৭ ঘণ্টা** (আগে ০ ছিল —
                     TK আজ নিজে নিয়মটা বদলেছেন)।
                   ⛔ IN-ই না থাকলে আগের মতোই ০ — নইলে না এসেও ঘণ্টা পাওয়া যেত।
                   ⛔ OUT যদি IN-এর আগে হয় (ভুল বসানো) সেটাও গোনায় ঢোকে না। */
                /* 🐞🔒 V1455 (১৪.০৯.২০২৬, TK-রিপোর্ট — "আজকের তারিখে এখনও staff
                   চেম্বারে আছে, আর আপনি বানিয়েছেন Missing 7hr এটা তো ঠিক না") —
                   আজকের এখনো-চলতে-থাকা দিনে "ভুলে যাওয়া"-র প্রশ্নই নেই (দিন শেষই
                   হয়নি), তাই এই দিনে ফ্ল্যাট ৭ ঘণ্টা বা "missing" ধরা হয় না —
                   এখন পর্যন্ত IN থেকে সত্যিকারের যত সময় কেটেছে সেটাই যোগ হয়।
                   আগের যেকোনো দিনে (সত্যিই OUT ভুলে যাওয়া) নিয়ম অটুট। */
                if (a != null && b == null && d.optString("work_date", "").take(10) == todayIso()) {
                    val now = nowMinutesIst()
                    if (now > a) worked += (now - a)
                    continue
                }
                if (a != null && b == null) { missing++; worked += DAY_MINUTES; continue }
                if (a == null || b == null || b <= a) { missing++; continue }
                worked += (b - a)
            }
        }
        return HourPay(ym, mh, rate, worked, leaves, missing, (worked / 60.0) * rate)
    }

    /** "৬ঘ ৩০মি" নয় — স্টাফের পর্দার লেখা ইংরেজিতে: `"148h 30m"`। */
    fun hoursText(minutes: Int): String =
        String.format(Locale.US, "%dh %02dm", minutes / 60, minutes % 60)
}
