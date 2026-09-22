package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * TK-REPORTED (2026-07-26): "মোবাইল বদলালে Payment/Prescription যেন না হারায়"।
 *
 * WHAT WAS WRONG
 * When a patient's mobile number was corrected (Follow-up "Edit Record" or the
 * Patient Card's 3-tap edit), the app moved the number over in followups /
 * patients / enquiries only. The "payments" and "doctor_visits" rows kept the
 * OLD number, so every screen that finds a patient BY MOBILE (Timeline, Draft,
 * Global Search, Report Card) stopped seeing that money.
 * Prescriptions/Investigation/Diet ("medical" table) are stored against the
 * Patient ID, not the mobile, so they were never affected and are not touched
 * here.
 *
 * SAFETY RULE (families often share one number in this clinic)
 * If the old number carries rows belonging to MORE THAN ONE Patient ID, only
 * the rows holding THIS patient's own Patient ID are moved; everything else is
 * left exactly as it was. Nothing is ever deleted . only the "mobile" field is
 * rewritten, amounts/dates/modes are untouched.
 */
object MobileChangeSync {

    /**
     * Tables that store a PATIENT mobile and are looked up by mobile.
     * AUDIT FIX (2026-07-26): "doctor_visits" was in this list by mistake .
     * that table is the Doctor/RMP directory, its "mobile" is the DOCTOR's
     * number and it has no patientId column at all. Rewriting it could have
     * changed a doctor's phone number. Removed.
     */
    private val TABLES = listOf("payments")

    /**
     * @param oldMobile the number the records are currently filed under
     * @param newMobile the corrected number
     * @param patientId this patient's own Patient ID (may be blank)
     * @return how many rows were moved (0 = nothing needed / nothing found)
     */
    /**
     * TK-REPORTED (2026-07-27): a payment that quietly stays under the OLD
     * number is money the staff can no longer see. Until now, if one of these
     * writes failed (weak line), it was simply lost -- nothing retried it, and
     * nobody was told. A context can now be passed in; when it is, every
     * failed write is put in the app's normal retry queue and goes out on the
     * next screen open, exactly like every other save. Callers that pass no
     * context behave exactly as before.
     */
    fun sync(oldMobile: String, newMobile: String, patientId: String, context: android.content.Context? = null): Int {
        val oldDigits = oldMobile.filter { it.isDigit() }.takeLast(10)
        val newDigits = newMobile.filter { it.isDigit() }.takeLast(10)
        if (oldDigits.length != 10 || newDigits.length != 10 || oldDigits == newDigits) return 0

        var moved = 0
        var lookupFailed = false
        for (table in TABLES) {
            try {
                // 🔴🔴🔒 V457 (TK-নির্দেশ ১৮.০৮.২০২৬: "নেট স্লো — এই অজুহাত আর নয়,
                // প্রজেক্টে কোথাও এই ধরনের সন্দেহ থাকলে খুঁজে বের করে ঠিক করুন")।
                // **আসল ঝুঁকি:** এখানে ambiguous `findByMobile` ব্যবহার হত — লুকআপ
                // ব্যর্থ হলে খালি লিস্ট ফেরাত, তাই `targets` খালি থেকে যেত আর
                // এই টেবিলটা চুপচাপ বাদ পড়ে যেত — payment পুরনো নম্বরেই আটকে
                // থাকত, চিরকাল, কোনো রিট্রাই/সতর্কতা ছাড়াই।
                // **সমাধান:** `findByMobileOrNull` — ব্যর্থ হলে (`null`) নিচে
                // পুরো (oldMobile→newMobile) কাজটাই রিট্রাই-লাইনে জমা থাকে।
                val rows = SupabaseClient.findByMobileOrNull(table, oldDigits, "id,patientId", 500)
                if (rows == null) { lookupFailed = true; continue }
                val distinctPatients = HashSet<String>()
                for (i in 0 until rows.length()) {
                    val pid = rows.getJSONObject(i).optString("patientId", "")
                    if (pid.isNotBlank()) distinctPatients.add(pid)
                }
                val sharedNumber = distinctPatients.size > 1
                // TK-REPORTED (2026-07-27, "slow internet" list item S5): the
                // decision of WHICH rows to move is unchanged, line for line --
                // it is only separated out first so the actual cloud writes can
                // then be sent together instead of one-after-another. On a slow
                // line each write could take many seconds, so a patient with a
                // dozen payment rows made "Saved" appear only after all dozen
                // finished in sequence. Same rows, same field, same safety rule
                // for shared family numbers -- only the waiting is shorter.
                val targets = ArrayList<String>()
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val rowId = row.optString("id", "")
                    if (rowId.isBlank()) continue
                    val pid = row.optString("patientId", "")
                    // shared number: move only this patient's own rows
                    if (sharedNumber && (patientId.isBlank() || pid != patientId)) continue
                    targets.add(rowId)
                }
                if (targets.isEmpty()) continue
                moved += ParallelCloud.runAll(targets) { rowId ->
                    val fields = JSONObject().put("mobile", newDigits)
                    val ok = SupabaseClient.updateById(table, rowId, fields)
                    if (!ok && context != null) {
                        try { GenericUpdateQueue.queue(context, table, rowId, fields) } catch (_: Throwable) { }
                    }
                    ok
                }
            } catch (_: Exception) {
                lookupFailed = true
                // best effort . a failure here must never break the main save
            }
        }
        if (lookupFailed && context != null) {
            try { queuePending(context, oldDigits, newDigits, patientId) } catch (_: Throwable) { }
        }
        return moved
    }

    // 🔴🔴🔒 V457 — লুকআপ নিজেই ব্যর্থ হলে গোটা mobile-change কাজটা এখানে জমা
    // থাকে, যাতে টাকার সারি চিরকাল পুরনো নম্বরে আটকে না যায়। ⛔ শুধু নিরাপত্তা-
    // জাল — সফল পথে (বেশিরভাগ সময়) এক অক্ষরও বদলায়নি।
    private fun prefs(context: android.content.Context) =
        context.getSharedPreferences("piles_clinic_mobilesync_pending", android.content.Context.MODE_PRIVATE)
    private fun queuePending(context: android.content.Context, oldDigits: String, newDigits: String, patientId: String) {
        try {
            val p = prefs(context)
            val arr = try { org.json.JSONArray(p.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
            arr.put(JSONObject().put("old", oldDigits).put("new", newDigits).put("pid", patientId))
            p.edit().putString("queue", arr.toString()).apply()
        } catch (_: Throwable) { }
    }
    /** BackgroundRefreshWorker/BottomNav-এর প্রমাণিত flush-চেইনে ডাকা হয়। */
    fun flushPending(context: android.content.Context) {
        try {
            val p = prefs(context)
            val arr = try { org.json.JSONArray(p.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
            if (arr.length() == 0) return
            val stillPending = org.json.JSONArray()
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val old = e.optString("old"); val new = e.optString("new"); val pid = e.optString("pid")
                if (old.isBlank() || new.isBlank()) continue
                try { sync(old, new, pid, context) } catch (_: Throwable) { stillPending.put(e) }
            }
            p.edit().putString("queue", stillPending.toString()).apply()
        } catch (_: Throwable) { }
    }
}
