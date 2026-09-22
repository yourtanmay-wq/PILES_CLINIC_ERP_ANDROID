package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * TK-APPROVED (2026-07-26, photo proof): "সতর্কবাতি".
 *
 * WHY THIS EXISTS
 * Every save in this app is local first, then pushed to Supabase with a
 * retry queue behind it (BottomNav.wire flushes them on every screen open).
 * That is good . but when a push kept failing, the screen still showed
 * everything green, so a record could sit on one phone for days with nobody
 * knowing. This object only COUNTS what is still waiting and offers a manual
 * retry. It reads the very same SharedPreferences queues the repositories
 * already use and writes nothing itself, so it cannot break any flow.
 */
object PendingSyncStatus {

    /** prefs file name to the Bengali word shown to TK. */
    private val QUEUES = listOf(
        "piles_clinic_registration_pending" to "রেজিস্ট্রেশন",
        "piles_clinic_payment_pending" to "পেমেন্ট",
        "piles_clinic_enquiry_pending" to "এনকোয়ারি",
        "piles_clinic_followup_pending" to "ফলো-আপ",
        "piles_clinic_followup_heal_pending" to "ফলো-আপ",
        "piles_clinic_chamber_pending" to "চেম্বার",
        "piles_clinic_medical_pending" to "প্রেসক্রিপশন",
        "piles_clinic_briefing_pending" to "ব্রিফিং",
        "piles_clinic_generic_update_pending" to "সংশোধন"
    )

    data class Summary(val total: Int, val detail: String)

    /** Counts every row still waiting in the offline queues. Read only. */
    fun summary(context: Context): Summary {
        var total = 0
        val perLabel = LinkedHashMap<String, Int>()
        // AUDIT FIX (2026-07-26): the Registration prefs file holds a SECOND
        // list under the key "closeQueue" (close the source Enquiry after a
        // registration). It was not being counted, so the screen could say
        // "Synced" while those were still stuck.
        try {
            val raw = context.getSharedPreferences("piles_clinic_registration_pending", Context.MODE_PRIVATE)
                .getString("closeQueue", "[]") ?: "[]"
            val n = org.json.JSONArray(raw).length()
            if (n > 0) { total += n; perLabel["এনকোয়ারি বন্ধ"] = n }
        } catch (_: Throwable) { }
        // 🚨🚨 খাতার সারি B145 (TK, 30.07.2026): **কেন্দ্রীয় তালিকাটা (`CloudWriteQueue`)
        // এখানে গোনাই হত না** — তাই ওষুধ বিক্রি · বিল সংশোধন · অনুমোদন · Trash
        // restore · ডাক্তার এন্ট্রি · পাসওয়ার্ড — এগুলোর কোনোটা আটকে থাকলে পর্দা
        // সবুজই দেখাত। TK-এর কথা: *"Synced দেখালেও কিছু কাজ বাকি থাকতে পারে।"*
        try {
            val n = CloudWriteQueue.pendingCount(context)
            if (n > 0) { total += n; perLabel["অন্য কাজ"] = n }
        } catch (_: Throwable) { }
        // যেগুলো বারবার চেষ্টা করেও যায়নি — আগে নীরবে বাদ পড়ত, এখন এখানে দেখা যায়।
        try {
            val n = CloudWriteQueue.failedCount(context)
            if (n > 0) { total += n; perLabel["পাঠানো যায়নি"] = n }
        } catch (_: Throwable) { }
        // 🚨🚨 খাতার সারি B170 (TK, 30.07.2026): Chamber Close-এর নিজের ছোট
        // তালিকাটা (কেন্দ্রীয় `CloudWriteQueue`-এর বাইরে, কারণ PATCH না-থাকা
        // সারিতে চুপচাপ সফল দেখায় — উপরের ফাইলের কমেন্টেই লেখা আছে কেন) আগে
        // এখানে গোনাই হত না। TK: *"App সবুজ Synced দেখাতে পারে, অথচ ভিতরে একটি
        // কাজ আটকে থাকতে পারে।"*
        try {
            val n = ChamberCloseRepository.pendingCount(context)
            if (n > 0) { total += n; perLabel["চেম্বার ক্লোজ"] = n }
        } catch (_: Throwable) { }
        for ((prefsName, label) in QUEUES) {
            val n = try {
                val raw = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    .getString("queue", "[]") ?: "[]"
                org.json.JSONArray(raw).length()
            } catch (_: Throwable) { 0 }
            if (n > 0) {
                total += n
                perLabel[label] = (perLabel[label] ?: 0) + n
            }
        }
        val detail = perLabel.entries.joinToString(", ") { "${it.value} ${it.key}" }
        // 🔒🔒 খাতার সারি B194 (TK, 30.07.2026 রাত — "fast Wifi-তেও কেন এমন
        // হচ্ছে?"): এখন সবচেয়ে সাম্প্রতিক আসল ব্যর্থতার কারণটাও (থাকলে)
        // Dashboard-এর সতর্কবার্তায় জুড়ে দেওয়া হয় — যাতে TK শুধু "নেটওয়ার্ক"
        // না ভেবে আসল কারণটা (যেমন HTTP 401, বা Timeout) দেখতে পান।
        // ⛔ কোনো কারণ পাওয়া না গেলে আগের মতোই শুধু সংখ্যাগুলোই দেখা যাবে।
        val lastError = try { CloudWriteQueue.peekLastError(context) } catch (_: Throwable) { "" }
        val detailWithReason = if (lastError.isNotBlank()) {
            if (detail.isBlank()) "কারণ: $lastError" else "$detail — কারণ: $lastError"
        } else detail
        // 🔒 V219 (§4, 31.07.2026): শুধু সংখ্যা/একটা কারণ নয় — কোন Table-এর কোন
        // Record আটকে আছে ও কেন, তার একটা ছোট তালিকাও (সর্বোচ্চ ৩টা) একই লেখায়
        // জুড়ে দেওয়া হয়, যাতে TK ঠিক কোনটা আটকেছে দেখে ব্যবস্থা নিতে পারেন।
        // ⛔ কোনো নতুন design/বোতাম নয় — বিদ্যমান সতর্কবার্তার লেখাতেই যোগ। কোনো
        //    stuck-item না থাকলে আগের মতোই।
        val stuck = try { CloudWriteQueue.stuckDetail(context, 3) } catch (_: Throwable) { "" }
        val detailFull = if (stuck.isNotBlank()) "$detailWithReason · আটকে: $stuck" else detailWithReason
        return Summary(total, detailFull)
    }

    /* ═══════════════════════════════════════════════════════════════════
       🟢🔒 V706 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত) — TK-এর প্রশ্ন:
       *"কোন পেশেন্ট এর পেমেন্ট আটকে রয়েছে সেটাই বা আমি জানবো কি করে"*।
       এতদিন লাল বাক্সে শুধু **সংখ্যা** দেখা যেত, কার টাকা আটকে তা জানার
       কোনো উপায় ছিল না।

       ⛔ এই অংশটা **শুধু পড়ে** — ঠিক সেই একই SharedPreferences তালিকা থেকে,
          যেগুলো `summary()` আগে থেকেই গোনে। একটাও লেখা/মোছা/ক্লাউড-কল নেই,
          তাই টাকার হিসাবে এর কোনো প্রভাব পড়তে পারে না।
       ⛔ TK-নির্দেশ: *"বাংলা হবে না, শুধুমাত্র ইংরেজিতে করুন"* ⇒ নিচের
          প্রতিটা দেখানোর লেখা ইংরেজি; সংখ্যাও ইংরেজি (Locale.US)।
       ═══════════════════════════════════════════════════════════════════ */
    data class Item(
        val kind: String,
        val name: String,
        val code: String,
        val amount: String,
        val date: String,
        /** কেন আটকে আছে — শেষ চেষ্টায় যা পাওয়া গেছে (ইংরেজি)। জানা না থাকলে ফাঁকা। */
        val why: String
    )

    /** prefs file name → the English word shown in the list. */
    private val DETAIL_QUEUES = listOf(
        "piles_clinic_payment_pending" to "Payment",
        "piles_clinic_registration_pending" to "Registration",
        "piles_clinic_enquiry_pending" to "Enquiry",
        "piles_clinic_followup_pending" to "Follow-up",
        "piles_clinic_followup_heal_pending" to "Follow-up",
        "piles_clinic_chamber_pending" to "Chamber",
        "piles_clinic_medical_pending" to "Prescription",
        "piles_clinic_briefing_pending" to "Briefing",
        "piles_clinic_generic_update_pending" to "Correction"
    )

    /** First non-blank value among these keys, looking inside "paymentRow"
     *  too (the Payment queue keeps the real row nested there). */
    private fun pick(entry: org.json.JSONObject, vararg keys: String): String {
        val nested = entry.optJSONObject("paymentRow")
        for (k in keys) {
            val v = entry.optString(k, "")
            if (v.isNotBlank() && v != "null") return v
            val n = nested?.optString(k, "") ?: ""
            if (n.isNotBlank() && n != "null") return n
        }
        return ""
    }

    private fun money(entry: org.json.JSONObject): String {
        val nested = entry.optJSONObject("paymentRow")
        val a = when {
            nested != null && nested.has("amount") -> nested.optDouble("amount", 0.0)
            entry.has("amount") -> entry.optDouble("amount", 0.0)
            else -> 0.0
        }
        if (a <= 0.0) return ""
        // ⛔ Locale.US — সংখ্যা সবসময় ইংরেজিতে (প্রজেক্টের নিয়ম ৯.১১)।
        return "Rs " + java.text.DecimalFormat("#,##0", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(a)
    }

    /**
     * Every row still waiting, in a form a person can read. Read only.
     * Payment rows come first, because that is the money.
     */
    fun details(context: Context, max: Int = 200): List<Item> {
        val out = ArrayList<Item>()
        for ((prefsName, kind) in DETAIL_QUEUES) {
            val arr = try {
                val raw = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    .getString("queue", "[]") ?: "[]"
                org.json.JSONArray(raw)
            } catch (_: Throwable) { org.json.JSONArray() }
            for (i in 0 until arr.length()) {
                if (out.size >= max) return out
                val e = arr.optJSONObject(i) ?: continue
                out.add(
                    Item(
                        kind = kind,
                        name = pick(e, "patientName", "name").ifBlank { "(no name)" },
                        code = pick(e, "patientCode", "patientId"),
                        amount = money(e),
                        date = pick(e, "date", "createdAt", "updatedAt"),
                        // 🟢🔒 V706 — `PaymentRepository.flushPending()` শেষ ব্যর্থ
                        //    চেষ্টায় এই ঘরটা লিখে রাখে। না থাকলে ফাঁকা।
                        why = e.optString("lastWhy", "")
                    )
                )
            }
        }
        return out
    }

    /**
     * Runs the SAME flush functions BottomNav.wire already runs on every
     * screen open . nothing new, just triggered on demand by the banner.
     * Must be called off the main thread.
     */
    fun retryAll(context: Context) {
        // 🔒 খাতার সারি B169 (TK-এর ৬ নম্বর সন্দেহ): এটাও এখন সেই একটাই দরজা
        // দিয়ে ঢোকে। ⛔ কিন্তু এখানে **চুপচাপ ফিরে যাওয়া চলবে না** — TK নিজে
        // বোতাম চেপেছেন, তিনি ভাববেন বোতামটা কাজ করেনি। তাই দরজা খালি হওয়ার
        // জন্য **১০ সেকেন্ড পর্যন্ত অপেক্ষা** করা হয়; তার মধ্যে খালি না হলে
        // বোঝা যায় অন্য একটা দফা এই কাজগুলোই করছে, তখন ফিরে গেলে কিছুই হারায়
        // না (পর্দার সংখ্যাটা তার পরেই আবার গোনা হয়)।
        SyncGate.runWaiting(10_000L) { retryAllNow(context) }
    }

    private fun retryAllNow(context: Context) {
        // 🚨🚨 খাতার সারি B145 (TK, 30.07.2026): **"পাঠান" চাপলেও কেন্দ্রীয় তালিকাটা
        // পাঠানো হত না** — TK হাতে চাপলেও ওই কাজগুলো আটকেই থাকত। এখন সবার আগে
        // ওটাই, আর "যায়নি" ঘরের কাজগুলোকেও আবার একটা সুযোগ দেওয়া হয়।
        try { CloudWriteQueue.attach(context) } catch (_: Throwable) { }
        try { CloudWriteQueue.retryFailed(context) } catch (_: Throwable) { }
        try { CloudWriteQueue.flush(context) } catch (_: Throwable) { }
        try { EnquiryRepository(context).flushPending() } catch (_: Throwable) { }
        try { RegistrationRepository(context).flushPending() } catch (_: Throwable) { }
        // 🔴🔒 V715 — মালিক/স্টাফ নিজে বোতাম চেপেছেন, তাই `force = true`:
        //    পরপর ব্যর্থ হওয়া সারির অপেক্ষা এখানে মানা হয় না, সঙ্গে সঙ্গে চেষ্টা।
        try { PaymentRepository(context).flushPending(force = true) } catch (_: Throwable) { }
        try { FollowUpRepository(context).flushPending() } catch (_: Throwable) { }
        try { ChamberAttendanceRepository.flushPending(context) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.clinical.ClinicalCloudRepository.flushPending(context) } catch (_: Throwable) { }
        try { BriefingRepository().flushPending(context) } catch (_: Throwable) { }
        try { GenericUpdateQueue.flushPending(context) } catch (_: Throwable) { }
        // 🚨🚨 খাতার সারি B170 (TK-এর ৭ নম্বর সন্দেহ): "পাঠান" চাপলেও এটা
        // পাঠানো হত না — এখন সবার সঙ্গে এটাও চেষ্টা হয়।
        try { ChamberCloseRepository.flushPending(context) } catch (_: Throwable) { }
    }
}
