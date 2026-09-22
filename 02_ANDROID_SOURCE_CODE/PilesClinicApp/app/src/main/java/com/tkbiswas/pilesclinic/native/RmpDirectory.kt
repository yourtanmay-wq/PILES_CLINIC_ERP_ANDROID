package com.tkbiswas.pilesclinic.native

import android.content.Context
import org.json.JSONArray

/**
 * 🟢🔒 V802 (২৮.০৮.২০২৬) — **"Saved RMP list not available" কেন আসত, আর তার সমাধান।**
 *
 * TK-এর রিপোর্ট (হুবহু): *"এটা Kishanganj এর ডাক্তার Amit এর নম্বর দিয়ে Login করে
 * রেজিস্ট্রেশন করা হচ্ছে, কিন্তু কাজ হচ্ছে না কেন — RMP পাঠিয়েছে তাহলে এখন কেন
 * দেখাচ্ছে না আরএমপি লিস্ট?"*
 *
 * ─── আসল কারণ (কোড ধরে প্রমাণিত, আন্দাজ নয়) ─────────────────────────────────
 * RMP বাছার তালিকাটা পড়ত **শুধু** `doctor_visit_cache` থেকে, আর ওই ঘরটা ভরে
 * **একমাত্র Doctor Visit পর্দা খুললে** (`DoctorVisitActivity` নিজে জমা করে)।
 * অর্থাৎ যে ফোনে কেউ কখনো Doctor Visit পর্দা খোলেনি, সেখানে ঘরটা **ফাঁকা** —
 * তাই তালিকাও ফাঁকা। আর পুরনো লেখাতেই স্পষ্ট ছিল: *"No cloud search was made"*
 * — egress বাঁচাতে ইচ্ছে করে ক্লাউডে খোঁজা হত না।
 * ⇒ ফল: রিসেপশনে নতুন ফোনে RMP-র নাম কখনোই আসত না।
 *
 * ─── সমাধান ─────────────────────────────────────────────────────────────────
 * তালিকা ফাঁকা থাকলে **একবারই** ক্লাউডে খোঁজা হয় — কিন্তু শুধু ওই ৮টা হালকা ঘর
 * (`id,name,mobile,altMobiles,area,branch,status,updatedAt`)। ভারী `callHistory`
 * ও `referralPayments` **আসেই না**, তাই খরচ কয়েক KB-র বেশি নয়।
 * তারপর সেটা ফোনে জমা থাকে, ⇒ পরের বার **এক বাইটও** খরচ নেই।
 *
 * ⛔ **সবচেয়ে জরুরি সুরক্ষা:** এই হালকা সারিগুলো `doctor_visit_cache`-এ
 *    **লেখা হয় না**, আলাদা ঘরে (`rmp_picker_cache`) থাকে। কারণ Doctor Visit
 *    পর্দা ওই পুরনো ঘরটাই পড়ে, আর সেখানে হালকা সারি ঢুকিয়ে দিলে কার্ডে
 *    কল-সংখ্যা ০ আর Paid ₹0 দেখাত — অর্থাৎ V543-এ সারানো বাগটাই ফিরে আসত।
 * ⛔ Doctor Visit পর্দার কোনো কোডে হাত পড়েনি।
 */
object RmpDirectory {

    private const val PICKER_PREFS = "rmp_picker_cache"
    private const val DV_PREFS = "doctor_visit_cache"

    /** যে ঘরগুলো বাছার তালিকাটা সত্যিই পড়ে — এর বাইরে একটাও নামে না। */
    const val PICKER_COLS = "id,name,mobile,altMobiles,area,branch,status,updatedAt"

    /** একই সেকেন্ডে বারবার চাপলে যেন বারবার না নামে। */
    @Volatile private var lastTryAt = 0L
    private const val RETRY_GAP_MS = 60_000L

    private fun key(branch: String) = "rmp_" + branch.ifBlank { "All" }

    /** ফোনে জমানো হালকা তালিকা (এই ফাইলের নিজের ঘর)। */
    fun cachedRows(ctx: Context, branch: String): JSONArray {
        return try {
            val raw = ctx.getSharedPreferences(PICKER_PREFS, Context.MODE_PRIVATE)
                .getString(key(branch), null) ?: return JSONArray()
            JSONArray(raw)
        } catch (_: Throwable) { JSONArray() }
    }

    /** Doctor Visit পর্দার জমানো ঘরে কিছু আছে কিনা — থাকলে ক্লাউডে যাওয়ার দরকারই নেই। */
    fun hasDoctorVisitCache(ctx: Context, branch: String): Boolean {
        return try {
            val p = ctx.getSharedPreferences(DV_PREFS, Context.MODE_PRIVATE)
            val keys = if (branch.equals("All", ignoreCase = true))
                listOf("All", "Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
            else listOf(branch)
            keys.any { !p.getString("cache_" + it.ifBlank { "All" }, null).isNullOrBlank() }
        } catch (_: Throwable) { false }
    }

    /**
     * একবারের হালকা ক্লাউড-পড়া। **শুধু ব্যাকগ্রাউন্ড থ্রেড থেকে ডাকবেন।**
     * সফল হলে ফোনে জমা করে `true` ফেরায়; নেট না থাকলে `false` — তখন পুরনো
     * আচরণই থাকে (হাতে নাম-নম্বর লেখা যায়), কিছুই ভাঙে না।
     */
    fun refreshFromCloud(ctx: Context, branch: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastTryAt < RETRY_GAP_MS) return false
        lastTryAt = now
        return try {
            val filter = StringBuilder("or=(status.eq.Active,status.is.null)")
            if (branch.isNotBlank() && !branch.equals("All", ignoreCase = true)) {
                filter.append("&branch=eq.").append(java.net.URLEncoder.encode(branch, "UTF-8"))
            }
            val rows = SupabaseClient.fetchListSlimOrNull(
                "doctor_visits", filter.toString(), 2000, PICKER_COLS
            ) ?: return false
            ctx.getSharedPreferences(PICKER_PREFS, Context.MODE_PRIVATE)
                .edit().putString(key(branch), rows.toString()).apply()
            rows.length() > 0
        } catch (_: Throwable) { false }
    }
}
