package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🟢🔒 V616 (২৪.০৮.২০২৬, TK-নির্দেশ) — "জলপাইগুড়ি থেকে ভুল করে রেজিস্টার
 * হওয়া রোগীর সব তথ্য কিষাণগঞ্জে ট্রান্সফার করা যাক, আর ভবিষ্যতেও Master
 * যেন এটা করতে পারেন।"
 *
 * **প্রেক্ষাপট (সৎভাবে লেখা):** Field Officer/Master রেজিস্ট্রেশনের সময়
 * নিজের ব্রাঞ্চ বাছতে বাধ্য (৩-চাপ লক শুধু Staff/Doctor-এর জন্য, ইচ্ছাকৃত —
 * তাঁরা একাধিক ব্রাঞ্চে কাজ করেন)। ভুল বেছে ফেললে এতদিন সংশোধনের কোনো
 * পথ ছিল না — এই ফাইলটাই সেই প্রথম পথ।
 *
 * 🔴🔒 V1630 (২০.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — "Patient ID তো চেঞ্জ হতে
 * হবে") — V616-এর তখনকার সিদ্ধান্ত ছিল patientId অক্ষুণ্ণ রাখা (পুরনো
 * ছাপা কাগজের সাথে মিল রাখতে), TK-কে সেই পুরনো কারণ ও তার উল্টো ঝুঁকি
 * (পুরনো ছাপা কাগজ আর নতুন ID-র সাথে মিলবে না) সরাসরি দেখিয়ে জিজ্ঞেস
 * করা হলো — TK "হ্যাঁ, নতুন ব্রাঞ্চ অনুযায়ী ID বানান" বলে সিদ্ধান্ত
 * পাল্টালেন। এখন থেকে ট্রান্সফারে patientId-ও নতুন ব্রাঞ্চের কোডে নতুন
 * করে বানানো হয় (আজকের তারিখ ধরে, `PatientIdGenerator.generate()`-এর
 * প্রমাণিত ডুপ্লিকেট-এড়ানো নিয়মেই) — একই মোবাইলে একাধিক আলাদা রোগী
 * (আলাদা আলাদা পুরনো patientId) থাকলে প্রত্যেকে নিজের আলাদা নতুন ID পান।
 *
 * **কী করে, কী করে না (স্পষ্ট, ঝুঁকি না লুকিয়ে):**
 *  ✅ `patients`, `followups`, `payments` — এই তিনটে টেবিলে ওই মোবাইলের
 *     **সব সারির `branch`** নতুন ব্রাঞ্চে বদলে দেয়।
 *  ✅ `patients`/`payments`-এর `patientId` নতুন ব্রাঞ্চ অনুযায়ী নতুন করে
 *     বানানো হয় (V1630) — `followups`-এ `patientId` ঘরই নেই (ওটা `refId`
 *     দিয়ে রোগীর `id`-কে ধরে, `id` কখনো বদলায় না, তাই `refId` ছোঁয়া হয়নি)।
 *  ⛔ **পুরনো ছাপা কাগজ/প্রেসক্রিপশন/রসিদে যে ID ছাপা ছিল, সেটা আর নতুন
 *     ID-র সাথে মিলবে না** — TK-কে এই ঝুঁকি সরাসরি জানিয়ে তবেই তিনি
 *     সিদ্ধান্ত বদলেছেন।
 *  ✅ প্রতিটা সারি **একটা একটা করে**, প্রমাণিত `updateById()` দিয়ে (৬২টা
 *     জায়গায় আগে থেকে ব্যবহৃত, নির্ভরযোগ্য) — নতুন কোনো bulk-write পথ
 *     বানানো হয়নি, তাই আচমকা ভুল হওয়ার নতুন কোনো সুযোগ তৈরি হয়নি।
 *  ✅ Master ছাড়া কেউ ডাকতে পারবেন না (কল করার আগে UI-তেই role-চেক)।
 */
object BranchTransferRepository {

    data class TransferPreview(
        val patientRows: List<JSONObject>,
        val followupRows: List<JSONObject>,
        val paymentRows: List<JSONObject>
    ) {
        val totalCount: Int get() = patientRows.size + followupRows.size + paymentRows.size
        val currentBranches: Set<String> get() =
            (patientRows + followupRows + paymentRows).map { it.optString("branch", "") }.filter { it.isNotBlank() }.toSet()
    }

    /** ট্রান্সফারের আগে — কতগুলো সারি, কোন ব্রাঞ্চে আছে, তা দেখানোর জন্য। */
    fun preview(mobile: String): TransferPreview? {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return null
        return try {
            val patients = SupabaseClient.findByMobile("patients", digits, "id,branch,patientId,name", 10)
            val followups = SupabaseClient.findByMobile("followups", digits, "id,branch,refId,name", 10)
            val payments = SupabaseClient.findByMobile("payments", digits, "id,branch,patientId,amount,payType", 200)
            TransferPreview(
                (0 until patients.length()).map { patients.getJSONObject(it) },
                (0 until followups.length()).map { followups.getJSONObject(it) },
                (0 until payments.length()).map { payments.getJSONObject(it) }
            )
        } catch (_: Throwable) { null }
    }

    data class TransferResult(val moved: Int, val failed: Int, val newPatientIds: List<String> = emptyList())

    /** আসল কাজ — প্রতিটা সারির `branch` ঘর নতুন ব্রাঞ্চে বদলে দেয়, আর
     *  (V1630) patients/payments-এর patientId নতুন ব্রাঞ্চ অনুযায়ী নতুন
     *  করে বানিয়ে দেয়। একই মোবাইলে একাধিক পুরনো patientId থাকলে (আলাদা
     *  আলাদা রোগী) প্রত্যেকে নিজের আলাদা নতুন ID পান — একটাও দুজনে ভাগ
     *  করে নেন না। */
    fun transfer(preview: TransferPreview, newBranch: String, context: Context? = null): TransferResult {
        var moved = 0
        var failed = 0
        val today = PatientIdGenerator.todayIso()
        // পুরনো patientId -> নতুন patientId — একটা পুরনো ID-র জন্য একটাই নতুন ID।
        val idMap = HashMap<String, String>()
        for (row in preview.patientRows) {
            val oldId = row.optString("patientId").trim()
            if (oldId.isBlank() || idMap.containsKey(oldId)) continue
            idMap[oldId] = PatientIdGenerator.generate(newBranch, today, context)
        }
        fun moveRows(table: String, rows: List<JSONObject>, hasPatientId: Boolean) {
            for (row in rows) {
                val id = row.optString("id")
                if (id.isBlank()) { failed++; continue }
                val fields = JSONObject().put("branch", newBranch)
                if (hasPatientId) {
                    val oldId = row.optString("patientId").trim()
                    val newId = idMap[oldId]
                    if (newId != null) fields.put("patientId", newId)
                }
                val ok = try {
                    SupabaseClient.updateById(table, id, fields)
                } catch (_: Throwable) { false }
                if (ok) moved++ else failed++
            }
        }
        moveRows("patients", preview.patientRows, hasPatientId = true)
        moveRows("followups", preview.followupRows, hasPatientId = false)   // ⛔ এই টেবিলে patientId ঘরই নেই — refId (রোগীর id) অপরিবর্তিত
        moveRows("payments", preview.paymentRows, hasPatientId = true)
        return TransferResult(moved, failed, idMap.values.toList())
    }
}
