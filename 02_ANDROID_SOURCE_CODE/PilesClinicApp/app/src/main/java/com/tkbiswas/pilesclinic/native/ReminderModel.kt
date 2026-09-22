package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * 🟢🔒 V1144 (০৬.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ: *"হ্যাঁ পাশ, বসিয়ে দিন, সাবধানে"*)
 *
 * TK-এর কথা: ডাক্তারবাবুর হোম পেজেই রিমাইন্ডার থাকবে; ভিতরে ঢুকে তিনি নতুন
 * রিমাইন্ডার যোগ করতে পারবেন ও রোগীর হিস্ট্রি দেখতে পাবেন; **যেকোনো ব্রাঞ্চের**
 * স্টাফও তাঁকে পাঠাতে পারবে (যেমন *"জলপাইগুড়ির অমুক রোগীর জন্য ওষুধ আনতে হবে"*)।
 * TK-এর প্রশ্নে যোগ হলো: পাঠানো স্টাফ যেন অবস্থা দেখতে পায় —
 * **Sent → Seen → Accepted → Done**, আর **২৪ ঘণ্টায়** Accept না হলে লাল সতর্কতা।
 *
 * ⛔ এটা শুধু একটা সারির ছাঁচ — কোনো নেটওয়ার্ক/পর্দার কাজ এখানে নেই।
 */
data class ReminderItem(
    val id: String,
    val branch: String,
    val patientName: String,
    val patientMobile: String,
    val disease: String,
    val type: String,          // Medicine · Treatment · Other
    val details: String,
    val remindOn: String,      // yyyy-MM-dd
    val toCode: String,
    val toName: String,
    val fromCode: String,
    val fromName: String,
    val status: String,        // sent · seen · accepted · done
    val sentAt: String,
    val seenAt: String,
    val acceptedAt: String,
    val doneAt: String
)

object ReminderModel {

    const val TABLE = "reminders"

    const val TYPE_MEDICINE = "Medicine"
    const val TYPE_TREATMENT = "Treatment"
    const val TYPE_OTHER = "Other"

    const val ST_SENT = "sent"
    const val ST_SEEN = "seen"
    const val ST_ACCEPTED = "accepted"
    const val ST_DONE = "done"

    /** TK-নির্দেশ (০৬.০৯.২০২৬): ২৪ ঘণ্টা পেরিয়ে গেলে পাঠানো স্টাফ লাল সতর্কতা দেখবেন। */
    const val ACCEPT_WINDOW_MS = 24L * 60L * 60L * 1000L

    val COLS = "id,branch,patientName,patientMobile,disease,type,details,remindOn," +
        "toCode,toName,fromCode,fromName,status,sentAt,seenAt,acceptedAt,doneAt,updatedAt"

    fun from(o: JSONObject): ReminderItem = ReminderItem(
        id = o.optString("id", ""),
        branch = o.optString("branch", ""),
        patientName = o.optString("patientName", ""),
        patientMobile = o.optString("patientMobile", ""),
        disease = o.optString("disease", ""),
        type = o.optString("type", TYPE_OTHER),
        details = o.optString("details", ""),
        remindOn = o.optString("remindOn", "").take(10),
        toCode = o.optString("toCode", ""),
        toName = o.optString("toName", ""),
        fromCode = o.optString("fromCode", ""),
        fromName = o.optString("fromName", ""),
        status = o.optString("status", ST_SENT).lowercase(),
        sentAt = o.optString("sentAt", ""),
        seenAt = o.optString("seenAt", ""),
        acceptedAt = o.optString("acceptedAt", ""),
        doneAt = o.optString("doneAt", "")
    )

    /** ঘরের রঙ — ওষুধ হলুদ · চিকিৎসা বেগুনি · অন্য সবুজ (ফটো-প্রুফের হুবহু)। */
    fun tint(type: String): String = when (type) {
        TYPE_MEDICINE -> "#FDE8C8"
        TYPE_TREATMENT -> "#E4DBF7"
        else -> "#DCEFE4"
    }

    fun icon(type: String): String = when (type) {
        TYPE_MEDICINE -> "💊"     // 💊
        TYPE_TREATMENT -> "🩺"    // 🩺
        else -> "📋"              // 📋
    }

    /** কাজ শেষ হয়ে গেছে কিনা — শেষ হলে "EARLIER" ঘরে ফিকে হয়ে বসে। */
    fun isDone(r: ReminderItem): Boolean = r.status == ST_DONE

    /**
     * পাঠানো স্টাফের পর্দায় লাল "Not accepted yet" দেখাবে কিনা।
     * ⛔ Accept হয়ে গেলে (বা Done) কখনো দেখায় না; সময় বোঝা না গেলেও দেখায় না —
     *    আন্দাজে সতর্কতা তোলা হয় না।
     */
    fun overdueForAccept(r: ReminderItem, nowMs: Long): Boolean {
        if (r.status == ST_ACCEPTED || r.status == ST_DONE) return false
        val sent = parseMs(r.sentAt)
        if (sent <= 0L) return false
        return nowMs - sent >= ACCEPT_WINDOW_MS
    }

    /** ISO সময় থেকে মিলিসেকেন্ড — না বুঝলে 0 (কিছু ভাঙে না)। */
    fun parseMs(iso: String): Long {
        val t = iso.trim()
        if (t.length < 10) return 0L
        val patterns = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"
        )
        for (p in patterns) {
            try {
                val f = java.text.SimpleDateFormat(p, java.util.Locale.US)
                if (p.endsWith("'Z'")) f.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return f.parse(t)?.time ?: 0L
            } catch (_: Exception) { }
        }
        return 0L
    }

    /** "06.09 · 3.10 Pm" — প্রজেক্টের চেনা ধাঁচ; না বুঝলে ড্যাশ। */
    fun stamp(iso: String): String {
        val ms = parseMs(iso)
        if (ms <= 0L) return "—"
        return try {
            java.text.SimpleDateFormat("dd/MM · h.mm a", java.util.Locale.US)   // 🔴 V1158
                .format(java.util.Date(ms))
        } catch (_: Exception) { "—" }
    }
}
