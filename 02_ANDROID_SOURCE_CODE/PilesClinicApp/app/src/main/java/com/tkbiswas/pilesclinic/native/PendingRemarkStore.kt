package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🔒 TK-এর নির্দেশ (28.07.2026 রাত · খাতার সারি B51)
 *
 * TK-এর কথা: *"কল বাটনে চাপার পরে অ্যাপ্লিকেশনে যখন সেই স্টাফ প্রথম ঢুকবে
 * তখন তাকে মনে করিয়ে দেবে — এই ব্যক্তির রিমার্ক লেখা বাকি আছে। আর যদি
 * অ্যাপ্লিকেশনে না ফেরে, তাহলে Dashboard-এ যে ঘন্টা আছে সেখানে অবশ্যই
 * নোটিফিকেশন আসতে হবে।"*
 *
 * **আগে কী ছিল:** কল করার পরে "রিমার্ক লিখবেন?" মনে করানোটা রাখা হত
 * `FollowUpActivity`-র ভিতরের একটা চলতি স্মৃতিতে (`pendingCallItem`)। কল লম্বা
 * হলে Android অ্যাপটা পিছন থেকে মেরে দেয় — তখন ওই স্মৃতি মুছে যেত, আর
 * মনে করানোটা আর কোনোদিন আসত না। Follow-up পর্দায় না ফিরে ড্যাশবোর্ডে ঢুকলেও
 * কিছুই আসত না।
 *
 * **এখন:** কল-বোতামে চাপ দেওয়ামাত্র নামটা এখানে **ফোনের নিজের ঘরে জমা** হয়,
 * তাই অ্যাপ বন্ধ হলে বা মরে গেলেও বাকির তালিকা থেকে যায়। রিমার্ক লেখা হলে
 * (বা এন্ট্রিটা Reject/Cancel হলে) নামটা নিজে থেকেই উঠে যায়।
 *
 * ⛔ **ক্লাউডে কিছুই লেখা হয় না** — পুরোটাই এই ফোনে, তাই Supabase-এর খরচ এক
 *    পয়সাও বাড়ে না এবং অন্য কারো তালিকা বদলায় না।
 * ⛔ **টাকার বা কল-গোনার কোনো হিসাবে হাত পড়ে না** — দাগ (call signal) আগের
 *    মতোই কেবল রিমার্ক সেভ করলে বাড়ে; এটা শুধু মনে করিয়ে দেওয়ার তালিকা।
 * ⛔ **এক মোবাইলে একটাই সারি** — একই লোককে দশ বার কল করলেও তালিকা বড় হয় না।
 */
data class PendingRemark(
    val mobile: String,
    val name: String,
    val branch: String,
    val stage: String,
    val followUpId: String,
    val staffMobile: String,
    val at: Long,
    // 🔒 খাতার সারি B57 (TK, 29.07.2026): কল করার সময় ওই এন্ট্রির কত কল হয়ে
    // গিয়েছিল তা সঙ্গে রাখা হয়। তালিকা না এলে সরাসরি রিমার্কের বাক্সে গেলেও
    // যেন **৫ কলের সিদ্ধান্ত-পপ-আপটা বাদ না পড়ে**। পুরনো এন্ট্রিতে এটা না
    // থাকলে ০ ধরা হয় (আগের মতোই)।
    val callCount: Int = 0,
    // 🆕 (03.08.2026, TK-নির্দেশ, B360) — "Not now" চাপলে কিছুক্ষণ (১ ঘণ্টা)
    // আর পপ-আপ দেখাবে না, আর সর্বমোট ৩ বার দেখানোর পরে পপ-আপ একদম বন্ধ
    // (শুধু ঘন্টার সংখ্যায় থেকে যাবে, BellCounter আগে থেকেই এই তালিকা
    // গোনে — নতুন কিছু করতে হয়নি ওখানে)। পুরনো এন্ট্রিতে এই দুটো না থাকলে
    // ০ ধরা হয় (আগের মতোই, প্রথমবার হিসেবে গণ্য)।
    val shownCount: Int = 0,
    val snoozeUntil: Long = 0,
    // V1652 — কোথা থেকে Call-পেন্ডিং তৈরি হয়েছে। পুরনো রেকর্ডে ফাঁকা।
    val source: String = ""
)

object PendingRemarkStore {

    private const val PREFS = "piles_clinic_pending_remark"
    private const val KEY = "rows"

    /** ৩০ দিনের পুরনো সারি নিজে থেকেই উঠে যায় — তালিকা যেন চিরকাল জমে না থাকে। */
    private const val MAX_AGE_MS = 30L * 24L * 60L * 60L * 1000L

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** অ্যাপের সব জায়গার মতোই — শেষ ১০টা সংখ্যা (নম্বর `+91` সহ জমা থাকে)। */
    private fun key(mobile: String): String = mobile.filter { it.isDigit() }.takeLast(10)

    private fun readArray(ctx: Context): JSONArray = try {
        JSONArray(prefs(ctx).getString(KEY, "[]") ?: "[]")
    } catch (e: Exception) {
        JSONArray()
    }

    private fun writeArray(ctx: Context, arr: JSONArray) {
        try {
            prefs(ctx).edit().putString(KEY, arr.toString()).commit()
        } catch (e: Exception) {
        }
    }

    /** কল-বোতামে চাপ দেওয়ার সঙ্গে সঙ্গে — "এর রিমার্ক লেখা বাকি"। */
    fun add(ctx: Context, item: FollowUpItem, staffMobile: String, source: String = "") {
        val k = key(item.mobile)
        if (k.length != 10) return
        val arr = readArray(ctx)
        val out = JSONArray()
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            if (key(row.optString("mobile")) == k) continue
            out.put(row)
        }
        out.put(
            JSONObject()
                .put("mobile", item.mobile)
                .put("name", item.name)
                .put("branch", item.branch)
                .put("stage", item.stage)
                .put("followUpId", item.id)
                .put("staffMobile", staffMobile)
                .put("at", System.currentTimeMillis())
                .put("callCount", item.callCount)   // 🔒 খাতার সারি B57
                .put("source", source)              // V1652: My Enquiry post-call popup isolation
        )
        writeArray(ctx, out)
    }

    /** রিমার্ক লেখা হয়ে গেছে (বা এন্ট্রিটা বাতিল) — তালিকা থেকে বাদ। */
    fun remove(ctx: Context, mobile: String) {
        val k = key(mobile)
        if (k.isEmpty()) return
        val arr = readArray(ctx)
        val out = JSONArray()
        var changed = false
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            if (key(row.optString("mobile")) == k) {
                changed = true
                continue
            }
            out.put(row)
        }
        if (changed) writeArray(ctx, out)
    }

    /** এই স্টাফের বাকি তালিকা (নতুনটা আগে)। */
    fun list(ctx: Context, staffMobile: String): List<PendingRemark> {
        val me = key(staffMobile)
        val arr = readArray(ctx)
        val now = System.currentTimeMillis()
        val keep = JSONArray()
        val mine = ArrayList<PendingRemark>()
        var dropped = false
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            val at = row.optLong("at", 0L)
            if (at > 0L && now - at > MAX_AGE_MS) {
                dropped = true
                continue
            }
            keep.put(row)
            if (key(row.optString("staffMobile")) != me) continue
            mine.add(
                PendingRemark(
                    mobile = row.optString("mobile"),
                    name = row.optString("name"),
                    branch = row.optString("branch"),
                    stage = row.optString("stage").ifBlank { "Inquiry" },
                    followUpId = row.optString("followUpId"),
                    staffMobile = row.optString("staffMobile"),
                    at = at,
                    callCount = row.optInt("callCount", 0),   // 🔒 খাতার সারি B57
                    shownCount = row.optInt("shownCount", 0),   // 🆕 B360
                    snoozeUntil = row.optLong("snoozeUntil", 0L),   // 🆕 B360
                    source = row.optString("source")                 // V1652
                )
            )
        }
        if (dropped) writeArray(ctx, keep)
        mine.sortByDescending { it.at }
        return mine
    }

    fun count(ctx: Context, staffMobile: String): Int = list(ctx, staffMobile).size

    // 🆕 (03.08.2026, TK-নির্দেশ, B360) — "Not now" চাপলে ১ ঘণ্টা snooze, আর
    // সর্বমোট ৩ বার পপ-আপ দেখানোর পরে চিরকালের জন্য পপ-আপ বন্ধ (তালিকা থেকে
    // মোছা হয় না, শুধু আর দেখানো হয় না — BellCounter-এর সংখ্যায় থেকেই যায়,
    // Master/স্টাফ ঘন্টায় দেখে বুঝতে পারবেন কাজ এখনো বাকি)।
    private const val SNOOZE_MS = 60L * 60L * 1000L   // ১ ঘণ্টা

    /** পপ-আপ দেখানোর মুহূর্তে ডাকা হয় — shownCount ১ বাড়ে। */
    fun markShown(ctx: Context, mobile: String) {
        val k = key(mobile)
        if (k.isEmpty()) return
        val arr = readArray(ctx)
        var changed = false
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            if (key(row.optString("mobile")) == k) {
                row.put("shownCount", row.optInt("shownCount", 0) + 1)
                changed = true
            }
        }
        if (changed) writeArray(ctx, arr)
    }

    /** "Not now" চাপলে ডাকা হয় — ১ ঘণ্টার জন্য snooze। */
    fun snooze(ctx: Context, mobile: String) {
        val k = key(mobile)
        if (k.isEmpty()) return
        val arr = readArray(ctx)
        var changed = false
        for (i in 0 until arr.length()) {
            val row = arr.optJSONObject(i) ?: continue
            if (key(row.optString("mobile")) == k) {
                row.put("snoozeUntil", System.currentTimeMillis() + SNOOZE_MS)
                changed = true
            }
        }
        if (changed) writeArray(ctx, arr)
    }
}
