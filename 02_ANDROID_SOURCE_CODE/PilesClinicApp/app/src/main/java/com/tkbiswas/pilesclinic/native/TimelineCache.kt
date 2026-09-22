package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * TK-REQUESTED (2026-07-27): "ভিউ অল চাপলে যেন সেই পেশেন্টের সমস্ত ডিটেলস
 * ওপেন হয়, এবং দ্রুত — তার জন্য যেন কোনো লেট না হয় বা লোডিংয়ের জন্য অপেক্ষা
 * করতে না হয়।"
 *
 * WHAT THIS IS
 * The last time a patient's Details screen was opened successfully, what was
 * on it is kept on the phone. Next time that patient is opened, the screen is
 * drawn from that copy INSTANTLY -- no waiting, no "Loading..." -- and the
 * fresh copy from the cloud quietly replaces it a moment later. This is the
 * same "show what the phone already has, then refresh" pattern the Chamber,
 * Payment, Draft and Follow-up screens already use.
 *
 * THE ONE SAFETY RULE THAT MATTERS
 * A saved row is for LOOKING AT ONLY. Every id that would let a row be edited
 * (the payment id, the enquiry row id, the follow-up history id) is
 * deliberately NOT saved, so a 3-tap on an old row cannot open an editor and
 * therefore cannot change the wrong record. The moment the fresh copy lands,
 * the real rows -- with their real ids -- take over and editing works exactly
 * as it always has.
 *
 * Nothing here ever writes to the cloud, and every call is wrapped so a
 * problem in the cache can never stop the screen from opening.
 */
object TimelineCache {

    private const val PREFS = "patient_timeline_cache"
    /** A saved copy older than this is ignored, so the staff can never be
     *  shown something genuinely out of date. */
    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * 🔴🔴🔒 V522 (২২.০৮.২০২৬, TK-নির্দেশ "Report Card ধরুন") — **দুই রোগীর
     * জমানো তথ্য আর মিশবে না।**
     *
     * **সমস্যা যেটা ছিল (কোডে প্রমাণিত):** চাবি ছিল **শুধু মোবাইল নম্বর**।
     * V516-এর পরে এক নম্বরে স্বামী ও স্ত্রী দুজন আলাদা রোগী থাকতে পারেন —
     * তখন স্বামীর Timeline দেখার পরে স্ত্রীরটা খুললে **প্রথম মুহূর্তে
     * স্বামীর জমানো তথ্যই আঁকা হত** (cache-first, V216 §10)। আসল পড়া শেষ
     * হলে ঠিক হয়ে যেত, কিন্তু ওই কয়েক সেকেন্ড ভুল রিপোর্ট দেখা যেত।
     *
     * **এখন:** ডাকার জায়গা যে রোগীটা চেয়েছে (`rowId`), সেটাও চাবির অংশ।
     *
     * ⛔ **পুরোনো জমানো তথ্য নষ্ট হয় না** — `rowId` ফাঁকা রাখলে চাবিটা
     *    **অক্ষরে অক্ষরে আগের মতোই** (`tl_<১০ সংখ্যা>`), তাই আগের সব
     *    ডাক ও ফোনে আগে থেকে জমা থাকা তথ্য অবিকল আগের মতোই চলে।
     * ⛔ চাওয়া-আইডি দিয়েই চাবি হয় (পাওয়া-আইডি দিয়ে নয়) — তাই জমানো ও
     *    খোঁজা সবসময় একই চাবিতে মেলে, কোনো সারি "হারায়" না।
     * ⛔ কোনো cloud-read নেই · কোনো তথ্য বদলায় না — শুধু ফোনের জমানো কপির নাম।
     */
    private fun key(mobile: String, rowId: String = ""): String {
        val base = "tl_" + mobile.filter { it.isDigit() }.takeLast(10)
        val r = rowId.trim()
        return if (r.isEmpty()) base else base + "_" + r
    }

    fun save(ctx: Context, mobile: String, data: TimelineData, rowId: String = "") {
        try {
            val arr = JSONArray()
            for (e in data.entries) {
                arr.put(
                    JSONObject()
                        .put("icon", e.icon)
                        .put("colorHex", e.colorHex)
                        .put("title", e.title)
                        .put("date", e.date)
                        .put("by", e.by)
                        .put("note", e.note)
                        .put("paymentBranch", e.paymentBranch)
                        .put("paymentAmount", e.paymentAmount)
                        .put("paymentMode", e.paymentMode)
                        .put("paymentCashAmount", e.paymentCashAmount)
                        .put("paymentOnlineAmount", e.paymentOnlineAmount)
                        .put("paymentEventCount", e.paymentEventCount)
                        .put("payType", e.payType)
                        .put("visitNo", e.visitNo)
                        .put("runningPaid", e.runningPaid)
                        .put("runningDue", e.runningDue)
                        .put("sortKey", e.sortKey)
                        .put("callTime", e.callTime)
                        // 🔴 V217 self-audit fix (31.07.2026): `paidEffect` নতুন ফিল্ড
                        // (§B216, refund-এর সাইন)। এটা cache-এ না থাকলে পরের বার
                        // cache-first paint-এ প্রতিটা রোগীর PAID বাক্স ভুল করে ₹0
                        // দেখাত (ReportCardActivity-এর নতুন `sumOf { it.paidEffect }`
                        // default 0.0 পেত) — শুধু refund থাকা রোগীর নয়, **সবার**।
                        // এখন সেভ করা হয়, তাই cache থেকে পড়লেও হিসাব ঠিক থাকে।
                        .put("paidEffect", e.paidEffect)
                        // \ud83d\udfe2\ud83d\udd12 V1090 (\u09e6\u09eb.\u09e6\u09ef.\u09e8\u09e6\u09e8\u09ec) \u2014 \u099a\u09bf\u09b9\u09cd\u09a8\u099f\u09be cache-\u098f \u09a8\u09be \u09b0\u09be\u0996\u09b2\u09c7
                        // \u09aa\u09b0\u09c7\u09b0 \u09ac\u09be\u09b0 cache \u09a5\u09c7\u0995\u09c7 \u0986\u0981\u0995\u09be\u09b0 \u09b8\u09ae\u09af\u09bc \u099a\u09bf\u0995\u09bf\u09ce\u09b8\u09be\u09b0 \u09a8\u09cb\u099f \u0986\u09ac\u09be\u09b0 "Called By"
                        // \u09b8\u09c7\u099c\u09c7 \u0995\u09b2\u09c7 \u0997\u09cb\u09a8\u09be \u09b9\u09a4 \u2014 \u09a4\u09be\u0987 \u098f\u0996\u09be\u09a8\u09c7\u0993 \u09b0\u09be\u0996\u09be \u09b9\u09b2\u09cb\u0964
                        .put("isTreatmentNote", e.isTreatmentNote)
                    // paymentId / enquiryRowId / followUpHistoryId are NEVER
                    // saved -- see the safety rule at the top of this file.
                )
            }
            val root = JSONObject()
                .put("savedAt", System.currentTimeMillis())
                .put("name", data.name)
                .put("patientId", data.patientId)
                .put("mobile", data.mobile)
                .put("branch", data.branch)
                .put("disease", data.disease)
                .put("age", data.age)
                .put("sex", data.sex)
                .put("address", data.address)
                .put("billTotal", data.billTotal)
                .put("billBeforeDiscount", data.billBeforeDiscount)   // 🏷️ V1508
                .put("discount", data.discount)
                .put("followupStage", data.followupStage)
                .put("refDoctorDisplay", data.refDoctorDisplay)
                /* 🔵🔒 V521 (২২.০৮.২০২৬): Timing চিপটা (⏰ UNEXPECTED TIME) যেন
                   cache থেকে আঁকার সময়েও **সঙ্গে সঙ্গে** দেখা যায়, পরে হঠাৎ
                   এসে না পড়ে। ⛔ পুরোনো cache-এ ঘরটা নেই → ফাঁকা → চিপ দেখায় না,
                   ঠিক আগের মতোই; আসল পড়া শেষ হলে চিপ বসে যায়। */
                .put("timeType", data.timeType)
                .put("entries", arr)
            prefs(ctx).edit().putString(key(mobile, rowId), root.toString()).apply()
        } catch (_: Throwable) {
            // Saving is a convenience only; failing to save must never matter.
        }
    }

    /** The saved copy, or null when there is none / it is too old / anything
     *  at all goes wrong. Callers treat null as "nothing to show yet" and
     *  behave exactly as they do today. */
    fun load(ctx: Context, mobile: String, rowId: String = ""): TimelineData? {
        return try {
            val raw = prefs(ctx).getString(key(mobile, rowId), null) ?: return null
            val root = JSONObject(raw)
            val savedAt = root.optLong("savedAt", 0L)
            if (savedAt <= 0L || System.currentTimeMillis() - savedAt > MAX_AGE_MS) return null
            val arr = root.optJSONArray("entries") ?: JSONArray()
            val list = ArrayList<TimelineEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    TimelineEntry(
                        icon = o.optString("icon", ""),
                        colorHex = o.optString("colorHex", "#334155"),
                        title = o.optString("title", ""),
                        date = o.optString("date", ""),
                        by = o.optString("by", ""),
                        note = o.optString("note", ""),
                        paymentId = null,
                        paymentBranch = o.optString("paymentBranch", ""),
                        paymentAmount = o.optDouble("paymentAmount", 0.0),
                        paymentMode = o.optString("paymentMode", "CASH"),
                        paymentCashAmount = o.optDouble("paymentCashAmount", if (o.optString("paymentMode", "CASH").equals("CASH", true)) o.optDouble("paymentAmount", 0.0) else 0.0),
                        paymentOnlineAmount = o.optDouble("paymentOnlineAmount", if (!o.optString("paymentMode", "CASH").equals("CASH", true)) o.optDouble("paymentAmount", 0.0) else 0.0),
                        paymentEventCount = o.optInt("paymentEventCount", 1).coerceAtLeast(1),
                        payType = o.optString("payType", ""),
                        visitNo = o.optInt("visitNo", 0),
                        runningPaid = o.optDouble("runningPaid", 0.0),
                        runningDue = o.optDouble("runningDue", 0.0),
                        sortKey = o.optString("sortKey", ""),
                        // 🔴 V217 self-audit fix: save()-এর সঙ্গে মিলিয়ে paidEffect
                        // ফিরিয়ে আনা হচ্ছে (না থাকলে পুরনো cache-এ 0.0, স্বাভাবিক)।
                        paidEffect = o.optDouble("paidEffect", 0.0),
                        enquiryRowId = null,
                        followUpHistoryId = null,
                        followUpHistoryIndex = -1,
                        callTime = o.optString("callTime", ""),
                        // \ud83d\udfe2\ud83d\udd12 V1090 \u2014 \u09aa\u09c1\u09b0\u09a8\u09cb cache-\u098f \u0998\u09b0\u099f\u09be \u09a8\u09c7\u0987 \u21d2 false \u21d2 \u09b9\u09c1\u09ac\u09b9\u09c1 \u0986\u0997\u09c7\u09b0 \u0986\u099a\u09b0\u09a3\u0964
                        isTreatmentNote = o.optBoolean("isTreatmentNote", false)
                    )
                )
            }
            if (list.isEmpty()) return null
            TimelineData(
                name = root.optString("name", ""),
                patientId = root.optString("patientId", ""),
                mobile = root.optString("mobile", ""),
                branch = root.optString("branch", ""),
                disease = root.optString("disease", ""),
                photo = "",
                entries = list,
                billTotal = root.optDouble("billTotal", 0.0),
                billBeforeDiscount = root.optDouble("billBeforeDiscount", 0.0),   // 🏷️ V1508
                discount = root.optDouble("discount", 0.0),
                age = root.optString("age", ""),
                sex = root.optString("sex", ""),
                address = root.optString("address", ""),
                followupStage = root.optString("followupStage", ""),
                refDoctorDisplay = root.optString("refDoctorDisplay", ""),
                timeType = root.optString("timeType", "")   // 🔵 V521
            )
        } catch (_: Throwable) {
            null
        }
    }
}
