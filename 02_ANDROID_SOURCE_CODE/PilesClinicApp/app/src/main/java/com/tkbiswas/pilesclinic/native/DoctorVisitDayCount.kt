package com.tkbiswas.pilesclinic.native

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 👨‍⚕️🔒 V1032 (০৪.০৯.২০২৬, TK-নির্দেশ) — **আজ কতজন ডাক্তারের কাছে যাওয়া হলো,
 * নিজে থেকে গোনা।**
 *
 * TK: *"কতজন ডাক্তারের কাছে ভিজিট করেছে তাকে ম্যানুয়ালি এন্ট্রি করতে হয়েছে"*।
 * সত্যিই তাই — দিনের রিপোর্টে এতদিন এটার কোনো স্বয়ংক্রিয় হিসাব ছিল না।
 *
 * ─── নিয়ম (নতুন কিছু বানানো হয়নি) ───────────────────────────────────────
 * প্রতিটা RMP ডাক্তারের সারিতে `callHistory` আছে; কল/ভিজিট লেখা হলেই সেখানে
 * **তারিখ** ও **কে করেছে** বসে (`DoctorVisitModel.buildCallUpdateFields`),
 * আর `lastCallDate`-এ আজকের তারিখ বসে। অ্যাপের "Doctor Call Summary" মাসের
 * হিসাব ঠিক এই ঘরদুটো দিয়েই গোনে — এখানে সেই একই নিয়মে **আজকেরটা** গোনা হয়,
 * তাই দুই জায়গায় কখনো দুরকম উত্তর হতে পারে না।
 *
 * ⚡ **Free plan-এ খরচ নগণ্য:** ছাঁকনিটা সার্ভারেই বসে (`lastCallDate=eq.আজ`),
 *    তাই আজ যাদের কাছে যাওয়া হয়েছে শুধু সেই কয়েকটা সারিই নামে — গোটা তালিকা
 *    নয়। মাসের হিসাবটা ৫০০০ সারি নামায়, এটা সাধারণত ০–২০টা।
 * ⛔ কোথাও কিছু লেখা হয় না — শুধু পড়া। ব্যর্থ হলে `-1` ফেরে, আর তখন রিপোর্টে
 *    লাইনটাই ওঠে না (পুরনো রিপোর্ট হুবহু আগের মতোই থাকে)।
 */
object DoctorVisitDayCount {

    /* 🔒 তারিখটা **যে নিয়মে লেখা হয় ঠিক সেই নিয়মেই** পড়া হয়
       (`DoctorVisitModel.today()` — ফোনের নিজের অঞ্চল, আলাদা কোনো TimeZone
       বসানো নেই)। দুই জায়গায় দুরকম নিয়ম হলে ফোনের ঘড়ি একটু এদিক-ওদিক
       থাকলেই গোনা শূন্য দেখাত। */
    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

    private fun tenDigits(v: String?): String =
        v.orEmpty().filter { it.isDigit() }.takeLast(10)

    /**
     * ওই স্টাফ আজ **কতজন আলাদা ডাক্তারের** কাছে গেছেন / কল করেছেন।
     * @return গোনা, অথবা পড়া না গেলে `-1`.
     */
    fun todayCount(staffMobile: String?): Int {
        val me = tenDigits(staffMobile)
        if (me.length != 10) return -1
        val today = todayIso()
        val rows = try {
            SupabaseClient.fetchListSlimOrNull(
                "doctor_visits", "lastCallDate=eq.$today", 500, "id,callHistory"
            )
        } catch (_: Throwable) { null } ?: return -1
        val seen = HashSet<String>()
        for (i in 0 until rows.length()) {
            val d = rows.optJSONObject(i) ?: continue
            val id = d.optString("id", "").trim()
            if (id.isBlank()) continue
            val hist = d.optJSONArray("callHistory") ?: continue
            for (j in 0 until hist.length()) {
                val h = hist.optJSONObject(j) ?: continue
                if (h.optString("date", "").take(10) != today) continue
                if (tenDigits(h.optString("by", "")) != me) continue
                seen.add(id)
                break
            }
        }
        return seen.size
    }
}
