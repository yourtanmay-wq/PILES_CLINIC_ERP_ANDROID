package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/**
 * 💊🔒 V985 (০২.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) —
 * **একজন রোগীর মেডিসিন/স্যালাইনের বাকি টাকা কত।**
 *
 * TK-এর কথা: *"এখানে সার্চ করার পরে মেডিসিন বা স্যালাইনের টাকা বাকি থাকলে তো
 * দেখার কোনো উপায় নেই?"* — গভীরে দেখে প্রমাণ হলো কথাটা সত্যি: `products`
 * টেবিলটা এতদিন শুধু Medicine পর্দা ও টাকার কালেকশনই পড়ত, Search · Full
 * Journey · রোগীর কার্ড কোথাও ছোঁয়াই হত না।
 *
 * ⛔ হিসাবের নিয়ম **হুবহু** `MedicinePaymentActivity`-র মতোই:
 *      বাকি = বিল − ওই দিনের জমা − পরে বাকি-জমা নেওয়া টাকা
 *    আর `due_…` সারিগুলো নিজে কখনো বাকি তৈরি করে না।
 * ⛔ খরচ কম রাখতে **একটাই অনুরোধ** যায় (খোঁজার সব নম্বর একসাথে), মাত্র
 *    পাঁচটা ঘর — ছবি বা বড় লেখা কিছুই নামে না (খাতার B625-এর নিয়ম)।
 */
object MedicineDue {

    private const val TABLE = "products"
    private const val COLS = "id,mobile,bill,total,deposit"

    /* 🔒 বাকি-জমার সারি চেনার নিয়ম — `MedicinePaymentActivity`-র হুবহু একই
       ("due_<আসল id>_<সময়>")। ওই পর্দার প্রমাণিত কোডে হাত না দিয়ে এখানে
       দুটো ছোট লাইনেই একই নিয়ম রাখা হলো, তাই দুই জায়গায় কখনো দুরকম হিসাব
       হতে পারে না। */
    private const val DUE_ID_PREFIX = "due_"
    private fun isSettlement(id: String) = id.startsWith(DUE_ID_PREFIX)
    private fun settledSaleIdOf(id: String): String {
        if (!isSettlement(id)) return ""
        val body = id.substring(DUE_ID_PREFIX.length)
        val cut = body.lastIndexOf('_')
        return if (cut > 0) body.substring(0, cut) else ""
    }

    /** মোবাইল (শেষ ১০ অঙ্ক) → এখনো কত বাকি। বাকি না থাকলে ঘরটাই থাকে না। */
    fun dueByMobile(mobiles: List<String>): Map<String, Double> {
        val wanted = mobiles.map { it.filter { c -> c.isDigit() }.takeLast(10) }
            .filter { it.length == 10 }.distinct()
        if (wanted.isEmpty()) return emptyMap()
        val rows = fetchFor(wanted) ?: return emptyMap()

        // ── আগে: কোন বিক্রির বিপরীতে পরে কত টাকা জমা পড়েছে ──
        val settled = HashMap<String, Double>()
        for (i in 0 until rows.length()) {
            val r = rows.optJSONObject(i) ?: continue
            val orig = settledSaleIdOf(r.s("id"))
            if (orig.isBlank()) continue
            settled[orig] = (settled[orig] ?: 0.0) + r.optDouble("deposit", 0.0)
        }

        val out = HashMap<String, Double>()
        for (i in 0 until rows.length()) {
            val r = rows.optJSONObject(i) ?: continue
            val id = r.s("id")
            if (isSettlement(id)) continue
            val key = r.s("mobile").filter { c -> c.isDigit() }.takeLast(10)
            if (key.length != 10) continue
            val bill = r.optDouble("bill", r.optDouble("total", 0.0))
            val paid = r.optDouble("deposit", 0.0) + (settled[id] ?: 0.0)
            val due = max(0.0, bill - paid)
            if (due > 0.0) out[key] = (out[key] ?: 0.0) + due
        }
        return out
    }

    /** একজনের বাকি — Full Journey-র মতো একক জায়গার জন্য। */
    fun dueFor(mobile: String): Double = dueByMobile(listOf(mobile)).values.firstOrNull() ?: 0.0

    /**
     * একটাই অনুরোধে সব নম্বরের সারি।
     * ⛔ নম্বর অনেক হলে PostgREST-এর ঠিকানা যাতে বড় না হয়ে যায়, তাই ২৫টা
     *    করে ভাগে যায় — তবু প্রতি পাতায় একটাই অনুরোধ।
     */
    private fun fetchFor(mobiles: List<String>): JSONArray? {
        val all = JSONArray()
        var got = false
        for (part in mobiles.chunked(25)) {
            val list = part.joinToString(",")
            val filter = "mobile=in.($list)"
            val rows = try { SupabaseClient.fetchListSlimOrNull(TABLE, filter, 500, COLS) }
                       catch (_: Throwable) { null } ?: continue
            got = true
            for (i in 0 until rows.length()) all.put(rows.optJSONObject(i) ?: continue)
        }
        return if (got) all else null
    }
}
