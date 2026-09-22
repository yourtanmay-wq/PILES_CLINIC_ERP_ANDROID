package com.tkbiswas.pilesclinic.clinical

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 🩺🔒 V839 (২৯.০৮.২০২৬) — **NEXT VISIT PLAN · পরের বার এই রোগীর কী হবে**
 *
 * TK-নির্দেশ (দীর্ঘ আলোচনা ও ফটো-প্রুফের পরে অনুমোদিত):
 * *"একজন পেশেন্ট প্রথম সপ্তাহে এক ডাক্তার দেখল, পরের সপ্তাহে অন্য ডাক্তার।
 * তখন যেন সে বুঝতে পারে গত সপ্তাহে কী আলোচনা হয়েছিল, কী ট্রিটমেন্ট করতে
 * হবে… ফিউচার প্ল্যান ছিল ক্ষারসূত্র পরিবর্তন করা অথবা অপারেশন করা।"*
 *
 * ### কোথায় জমা হয়
 * `patients.nextVisitPlan` — একটা **jsonb তালিকা**। প্রতিবারের প্ল্যান
 * তালিকার শেষে যোগ হয়; **পুরনো সারি কখনো মোছা হয় না**।
 *
 * ### ⛔ কেন এই আলাদা ঘরে (`medical` টেবিলে নয়)
 * `medical`-এ রাখলে `ClinicalCloudRepository.loadMedical()` · Patient
 * Timeline · Print Center — তিন জায়গায় **সব ধরনের সারি একসাথে** পড়া হয়,
 * তাই ভুল করে রোগীর কাগজে/পর্দায় দেখানোর ঝুঁকি থাকত।
 * এই ঘরটা প্রজেক্টের **আর কোথাও পড়া হয় না**, তাই TK-এর নির্দেশ
 * (*"শুধুমাত্র সিস্টেমে থাকবে"*) যন্ত্রগতভাবেই নিশ্চিত।
 *
 * ### ⛔ ছাপার কাগজে কখনো যাবে না
 * ছাপার লেখা আসে শুধু `PrescriptionOptionsStore.printLinesForSlip()` থেকে
 * (DISEASE · SYMPTOMS · DURATION · CHIEF COMPLAINT)। এই ফাইলের একটা
 * ফাংশনও সেখানে ডাকা হয় না।
 */
object NextVisitPlan {

    /** ঘরের নাম — ওয়েবের `app.js`-এর সাথে **হুবহু** এক রাখতে হবে। */
    const val FIELD = "nextVisitPlan"

    /** ৯টা বাছাই — TK নিজে লিখে দিয়েছেন (২৯.০৮.২০২৬)।
     *  `key` কখনো বদলানো যাবে না (জমা তথ্য ওই নামেই বসে);
     *  `label` শুধু পর্দায় দেখানোর লেখা। */
    data class Option(val key: String, val label: String)

    val OPTIONS: List<Option> = listOf(
        Option("followUp",      "Follow up / ফলো আপ"),
        Option("dressing",      "Dressing / ড্রেসিং"),
        Option("cautery",       "Cautery Machine / মেশিনের কাজ"),
        Option("threadInside",  "Inside Thread Tie / ভেতরে সুতো বাঁধা"),
        Option("threadOutside", "Outside Thread Tie / বাইরে সুতো বাঁধা"),
        Option("threadChange",  "Thread Change / সুতো চেঞ্জ"),
        Option("threadTighten", "Thread Tighten / সুতো টানা"),
        Option("threadNew",     "New Thread / নতুন সুতো পরানো"),
        Option("medicine",      "Medicine / ঔষধ দিতে হবে")
    )

    /** ঔষধ বাছলে "কোন ঔষধ" ঘরটা খোলে — এই একটাই চাবি। */
    const val KEY_MEDICINE = "medicine"

    /** ছোট নাম (কার্ডের ট্যাগ ও নোটিফিকেশনে জায়গা কম বলে)। */
    private val SHORT: Map<String, String> = mapOf(
        "followUp" to "Follow up",
        "dressing" to "Dressing",
        "cautery" to "Cautery",
        "threadInside" to "ভেতরে সুতো",
        "threadOutside" to "বাইরে সুতো",
        "threadChange" to "সুতো চেঞ্জ",
        "threadTighten" to "সুতো টানা",
        "threadNew" to "নতুন সুতো",
        "medicine" to "ঔষধ"
    )

    /** একটা প্ল্যান-সারি। কোনো ঘর না থাকলে ফাঁকা — কখনো ক্র্যাশ নয়। */
    data class Entry(
        val id: String = "",
        val date: String = "",          // ডাক্তার যে তারিখ ভেবেছেন (yyyy-MM-dd)
        val items: List<String> = emptyList(),
        val medicine: String = "",
        val note: String = "",
        val byName: String = "",
        val byMobile: String = "",
        val at: String = ""             // কবে লেখা হলো (ISO)
    ) {
        val isEmpty: Boolean get() = items.isEmpty() && note.isBlank() && medicine.isBlank()

        /** কার্ড ও নোটিফিকেশনের এক লাইনের লেখা — "Dressing · সুতো চেঞ্জ · ঔষধ" */
        fun shortLine(): String {
            val parts = items.mapNotNull { SHORT[it] }
            return if (parts.isEmpty()) note.take(40) else parts.joinToString(" · ")
        }
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    /** সারির ঘরটা তালিকা হিসেবে পড়া। ফাঁকা/অচেনা হলে খালি তালিকা (নীরবে)।
     *  ⛔ নাম `entriesOf` — `listOf` নয়, কারণ ওটা Kotlin-এর নিজের নামের সাথে
     *     সংঘর্ষ করত; এখন কাজ করলেও ভবিষ্যতে লুকানো ভুলের ফাঁদ হত।
     *  ⛔ ঘরটা কখনো লেখা (string) হয়ে এলেও সামলানো হয় — পুরনো ওয়েব-সারি
     *     এভাবেই আসতে পারে (প্রজেক্টের `doctorFullNote`-এ একই ব্যবস্থা আছে)। */
    fun entriesOf(row: JSONObject?): List<Entry> {
        if (row == null) return emptyList()
        val arr: JSONArray = row.optJSONArray(FIELD) ?: try {
            val raw = row.optString(FIELD, "")
            if (raw.trimStart().startsWith("[")) JSONArray(raw) else return emptyList()
        } catch (_: Throwable) { return emptyList() }
        val out = ArrayList<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val items = ArrayList<String>()
            val ia = o.optJSONArray("items")
            if (ia != null) for (j in 0 until ia.length()) {
                val k = ia.optString(j, "").trim()
                if (k.isNotBlank()) items.add(k)
            }
            out.add(
                Entry(
                    id = o.optString("id", ""),
                    date = o.optString("date", ""),
                    items = items,
                    medicine = o.optString("medicine", ""),
                    note = o.optString("note", ""),
                    byName = o.optString("byName", ""),
                    byMobile = o.optString("byMobile", ""),
                    at = o.optString("at", "")
                )
            )
        }
        return out
    }

    /** সবচেয়ে নতুন প্ল্যান — না থাকলে `null`.
     *  ⛔ তালিকার **শেষেরটাই** নতুন (নতুন সারি শেষে যোগ হয়)। */
    fun latest(row: JSONObject?): Entry? = entriesOf(row).lastOrNull { !it.isEmpty }

    /** নতুন সারি **যোগ** করা — পুরনো তালিকা হুবহু রেখে।
     *  ⛔ কখনো পুরনো সারি মোছে না বা বদলায় না। */
    fun appended(row: JSONObject?, entry: Entry): JSONArray {
        val arr = JSONArray()
        val old = row?.optJSONArray(FIELD) ?: try {
            val raw = row?.optString(FIELD, "").orEmpty()
            if (raw.trimStart().startsWith("[")) JSONArray(raw) else JSONArray()
        } catch (_: Throwable) { JSONArray() }
        for (i in 0 until old.length()) arr.put(old.get(i))
        arr.put(toJson(entry))
        return arr
    }

    fun toJson(e: Entry): JSONObject {
        val items = JSONArray()
        for (k in e.items) items.put(k)
        return JSONObject()
            .put("id", e.id.ifBlank { "nvp_" + System.currentTimeMillis() + "_" + (0..999).random() })
            .put("date", e.date)
            .put("items", items)
            .put("medicine", e.medicine)
            .put("note", e.note)
            .put("byName", e.byName)
            .put("byMobile", e.byMobile)
            .put("at", e.at.ifBlank { isoNow() })
    }

    /** 🟢🔒 OLD নাকি NEW (TK-নির্দেশ: *"এই কথা যেন মেনশন থাকে"*)।
     *  নিয়ম: রেজিস্ট্রেশনের তারিখ **আজ** হলে NEW, নইলে OLD।
     *  ⛔ কেন এই ঘরটাই: `registrationDate` কখনো বদলায় না — রোগী যতবারই
     *     আসুন, প্রথম দিনের তারিখটাই থেকে যায় (`PatientModel.kt:264`)।
     *  ⛔ তারিখ জানা না থাকলে **কিছুই দেখানো হয় না** (ফাঁকা) — আন্দাজে
     *     "NEW"/"OLD" বসানো হয় না। */
    fun oldOrNew(registrationDate: String?): String {
        val d = registrationDate.orEmpty().take(10)
        if (d.length != 10) return ""
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return if (d == today) "NEW" else "OLD"
    }
}
