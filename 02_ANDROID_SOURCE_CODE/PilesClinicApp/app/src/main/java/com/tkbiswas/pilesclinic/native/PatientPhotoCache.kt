package com.tkbiswas.pilesclinic.native

import android.content.Context
/* 🔴🔒 V807 (২৮.০৮.২০২৬) — TK-এর Android Studio-তে বিল্ড ভেঙেছিল:
 * `Unresolved reference: PilesClinicApplication` (PatientPhotoCache.kt:41)।
 * কারণ: এই ফাইলটা `…pilesclinic.native` প্যাকেজে, আর ক্লাসটা তার **উপরের**
 * প্যাকেজে (`…pilesclinic`)। Kotlin-এ উপরের প্যাকেজের নাম **নিজে থেকে আসে না**,
 * import লিখতেই হয়। V794-এ ফাইলটা লেখার সময় সেটা বাদ পড়েছিল। */
import com.tkbiswas.pilesclinic.PilesClinicApplication

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * 🔴🔴🔒 V794 (২৮.০৮.২০২৬, TK-নির্দেশে পূর্ণ Egress-যাচাইয়ের পরে) —
 * **রোগীর ছবি একবারই নামবে, বারবার নয়।**
 *
 * TK: *"Supabase egress এর ঝুঁকি আর কোথায় কোথায় আছে … সবগুলি করুন, সততার
 * সাথে সঠিকভাবে যেন ভবিষ্যতে সেটা কার্যকারী হয়।"*
 *
 * ─── সমস্যা (কোড ধরে প্রমাণিত) ───────────────────────────────────────────
 * `patients.photo` ঘরে রোগীর ছবি **base64** হিসেবে থাকে
 * (`PhotoUtils.encodeResized` — ৬০০ পিক্সেল, JPEG ৮৫ ⇒ ≈ ৬০–১২০ KB)।
 * Check-up খোলার সময় ও Report Card ছাপার সময় গোটা সারিটা `select=*` দিয়ে
 * পড়া হত — অর্থাৎ **প্রতিবার** ছবিটা আবার নামত, যদিও ছবিটা বদলায়নি।
 *
 * ─── সমাধান ─────────────────────────────────────────────────────────────
 * ছবিটা ফোনে জমা থাকে, সঙ্গে ওই সারির `updatedAt`।
 *   · `updatedAt` মিললে ⇒ **একটাও বাইট নামে না**, জমা ছবিটাই চলে
 *   · না মিললে (বা প্রথমবার) ⇒ শুধু `id,photo` নিয়ে **এক সারি** পড়া হয়
 * ⇒ প্রথমবার খরচ আগের মতোই, তারপর থেকে শূন্য। **একটাও ছবি হারায় না।**
 *
 * ─── 🔒 নিরাপত্তা ────────────────────────────────────────────────────────
 *  • সর্বোচ্চ ৪০ জন রোগীর ছবি জমা থাকে (সবচেয়ে পুরোনোটা বাদ যায়) — ফোনের
 *    জায়গা ভরে যাওয়ার পথ নেই।
 *  • ছবি বদলালে `updatedAt` বদলায়, তাই পুরোনো ছবি কখনো আটকে থাকে না।
 *  • যেকোনো গোলমালে চুপচাপ সরে দাঁড়ায় (try/catch) — তখন আগের মতোই
 *    ক্লাউড থেকেই পড়া হয়, কিছু ভাঙে না।
 *  • কোনো নতুন কলাম/টেবিল/SQL লাগেনি।
 * ═══════════════════════════════════════════════════════════════════════════
 */
object PatientPhotoCache {

    private const val PREFS = "piles_clinic_patient_photo_cache"
    private const val KEY_ORDER = "__order"
    private const val MAX = 40

    private fun prefs(ctx: Context?) = try {
        (ctx ?: PilesClinicApplication.appContext)
            ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    } catch (_: Throwable) { null }

    private fun k(id: String) = "p_$id"
    private fun kStamp(id: String) = "s_$id"

    /** জমা ছবিটা — শুধু তখনই, যখন সারির `updatedAt` হুবহু মেলে। */
    fun get(ctx: Context?, id: String, updatedAt: String): String? = try {
        if (id.isBlank()) null else {
            val p = prefs(ctx)
            val st = p?.getString(kStamp(id), "").orEmpty()
            if (st.isNotBlank() && st == updatedAt) p?.getString(k(id), "")?.ifBlank { null } else null
        }
    } catch (_: Throwable) { null }

    /** ছবিটা জমা রাখা। পুরোনো হয়ে গেলে সবচেয়ে আগেরটা বাদ। */
    fun put(ctx: Context?, id: String, updatedAt: String, photo: String) {
        try {
            if (id.isBlank() || photo.isBlank()) return
            val p = prefs(ctx) ?: return
            val order = ArrayList(p.getString(KEY_ORDER, "").orEmpty()
                .split("|").filter { it.isNotBlank() && it != id })
            order.add(id)
            val e = p.edit().putString(k(id), photo).putString(kStamp(id), updatedAt)
            while (order.size > MAX) {
                val old = order.removeAt(0)
                e.remove(k(old)).remove(kStamp(old))
            }
            e.putString(KEY_ORDER, order.joinToString("|")).apply()
        } catch (_: Throwable) { }
    }

    /**
     * ছবিটা এনে দেওয়া — **জমা থাকলে বিনা খরচে**, নইলে ওই এক সারির
     * `id,photo` পড়ে (কোনো বাড়তি ঘর নয়)।
     * ⛔ পড়া ব্যর্থ হলে ফাঁকা ফেরে; ডাকার জায়গা আগের মতোই সামলায়।
     */
    fun photoFor(ctx: Context?, rowId: String, updatedAt: String): String {
        if (rowId.isBlank()) return ""
        get(ctx, rowId, updatedAt)?.let { return it }
        return try {
            val enc = java.net.URLEncoder.encode(rowId, "UTF-8")
            val arr = SupabaseClient.fetchListSlimOrNull(
                "patients", "id=eq.$enc", 1, "id,photo") ?: return ""
            val photo = if (arr.length() > 0) arr.getJSONObject(0).optString("photo", "") else ""
            if (photo.isNotBlank()) put(ctx, rowId, updatedAt, photo)
            photo
        } catch (_: Throwable) { "" }
    }
}
