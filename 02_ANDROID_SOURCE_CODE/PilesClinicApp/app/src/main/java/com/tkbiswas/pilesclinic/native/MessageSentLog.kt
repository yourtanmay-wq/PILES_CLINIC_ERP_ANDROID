package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * 🔴🟢 খাতার সারি B433 (TK-নির্দেশ, 05.08.2026 — "দুইজন স্টাফ একই ব্যক্তিকে
 * একই বার্তা পাঠিয়েছেন, রোগী বিভ্রান্ত হয়ে গেছেন")।
 *
 * TK-এর স্পষ্ট নির্দেশ (গুরুত্বপূর্ণ, স্মরণে রাখা): *"পেশেন্ট বললাম মানে
 * এই না যে RMP/ডাক্তারদের ক্ষেত্রে আলাদা নিয়ম হবে — নিয়ম একই হবে সমস্ত
 * ব্রাঞ্চ ও সমস্ত প্রজেক্টের ক্ষেত্রে।"* তাই এই একই, একটামাত্র ফাইল/টেবিল
 * রোগীর ১১ ধরনের বার্তা (`PatientMessage.kt`) ও RMP/ডাক্তারের ৪ ধরনের
 * বার্তা (`DoctorMessage.kt` / `DoctorVisitActivity.sendDoctorMessage()`)
 * — দুটোতেই ব্যবহার হয়। সব ব্রাঞ্চ, সব স্টাফ, সব ফোন — একই নিয়ম।
 *
 * **যা করে:**
 *  - `checkPrior()` — ওই নম্বরে ওই একই ধরনের বার্তা আগে কেউ পাঠিয়ে থাকলে
 *    (কে, কবে) ফেরত দেয়; না থাকলে/ক্লাউড না পাওয়া গেলে `null` — তখন
 *    পপ-আপ স্বাভাবিকভাবেই কোনো সতর্কতা ছাড়া খোলে (কখনো আটকায় না)।
 *  - `record()` — WhatsApp/SMS-এ সত্যিই "Send" চাপার **পরে** একটা নতুন
 *    সারি জমা করে (fire-and-forget, ব্যর্থ হলেও বার্তা পাঠানো আটকায় না)।
 *
 * ⛔ **"Later" চাপলে/পপ-আপ বাতিল করলে কিছুই জমা হয় না** — শুধু সত্যিই
 * পাঠানো হলেই (WhatsApp/SMS) রেকর্ড হয়, তাই ভবিষ্যতে ভুল সতর্কতা আসবে না।
 * ⛔ সতর্কতা শুধু **মনে করিয়ে দেয়, আটকায় না** — TK-এর নির্দেশ অনুযায়ী
 *    স্টাফ চাইলে জেনে-শুনেই আবার পাঠাতে পারবেন।
 *
 * **নতুন SQL টেবিল লাগবে (TK নিজে Supabase-এ চালাবেন):**
 * ```sql
 * create table if not exists message_log (
 *   id text primary key,
 *   mobile text not null,
 *   kind text not null,
 *   recipient_type text not null default 'patient',
 *   branch text,
 *   name text,
 *   sent_by text,
 *   sent_by_name text,
 *   channel text,
 *   sent_at timestamptz not null default now()
 * );
 * create index if not exists idx_message_log_mobile_kind on message_log(mobile, kind);
 * ```
 */
object MessageSentLog {

    data class PriorSend(val staffName: String, val whenText: String, val channel: String)

    /**
     * ক্লাউডে জিজ্ঞাসা করে — এই নম্বরে এই ধরনের বার্তা আগে পাঠানো হয়েছে
     * কিনা। নেট/টেবিল-সমস্যা হলে চুপচাপ `null` (নতুন ফিচার কখনো পুরনো
     * পাঠানোর কাজ আটকাবে না)।
     */
    fun checkPrior(mobile: String, kind: String, recipientType: String = "patient"): PriorSend? {
        return try {
            val digits = mobile.filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10) return null
            val filter = "mobile=eq.$digits&kind=eq.$kind&recipient_type=eq.$recipientType"
            val rows = SupabaseClient.fetchListOrNull(
                "message_log", filter, 1, "sent_at.desc", "sent_by_name,sent_at,channel"
            ) ?: return null
            if (rows.length() == 0) return null
            val row = rows.getJSONObject(0)
            val staffName = row.s("sent_by_name").ifBlank { "অন্য স্টাফ" }   // 🔴 V819 — `optString` SQL NULL-এ আক্ষরিক "null" ফেরায় (V696/V812-এর ফাঁদ); `s()` সেটা ফাঁকা ধরে
            val whenRaw = row.optString("sent_at", "")
            val whenText = try { DateUtil.displayWithTime(whenRaw) } catch (_: Throwable) { whenRaw }
            val channel = row.optString("channel", "")
            PriorSend(staffName, whenText, channel)
        } catch (_: Throwable) { null }
    }

    /** সত্যিই পাঠানোর পরে (WhatsApp/SMS বোতাম চাপার পরে) ডাকা হয় — ব্যর্থ
     *  হলেও নিঃশব্দে বাদ, বার্তা পাঠানোর কাজ কখনো আটকায় না। */
    fun record(
        mobile: String, kind: String, branch: String, name: String,
        staffMobile: String, staffName: String, channel: String,
        recipientType: String = "patient"
    ) {
        try {
            val digits = mobile.filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10) return
            val row = JSONObject()
                .put("id", "mlog_" + System.currentTimeMillis() + "_" + digits)
                .put("mobile", digits)
                .put("kind", kind)
                .put("recipient_type", recipientType)
                .put("branch", branch)
                .put("name", name)
                .put("sent_by", staffMobile)
                .put("sent_by_name", staffName)
                .put("channel", channel)
            SupabaseClient.upsert("message_log", row)
        } catch (_: Throwable) { }
    }

    /** পপ-আপে বসানোর জন্য তৈরি বাংলা সতর্কতা-লাইন। */
    fun warningText(p: PriorSend): String {
        val whenPart = if (p.whenText.isNotBlank()) " — " + p.whenText else ""
        return "⚠️ এই বার্তা আগে " + p.staffName + whenPart + "-এ পাঠিয়েছেন।"
    }
}
