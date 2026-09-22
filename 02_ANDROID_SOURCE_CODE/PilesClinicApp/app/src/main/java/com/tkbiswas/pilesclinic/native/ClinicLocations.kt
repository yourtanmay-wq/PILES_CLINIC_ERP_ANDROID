package com.tkbiswas.pilesclinic.native

/**
 * 📍🔒 V496 (২১.০৮.২০২৬, TK-এর চূড়ান্ত নির্দেশ §৫) — **পাঁচ ব্রাঞ্চের অবস্থান।**
 *
 * ════════════════════════════════════════════════════════════════════════════
 *  ⚠️⚠️  এই একটাই ফাইলে সংখ্যা বসাতে হবে। আর কোথাও নয়।  ⚠️⚠️
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ─── ✅ সংখ্যাগুলো কোথা থেকে এল (আন্দাজ নয়) ────────────────────────────────
 * প্রকল্পের পাঁচটা Google Maps লিংক (`PatientMessage.kt:110-134`) খুলে নাম ও
 * ঠিকানা মিলিয়ে দেখা হয়েছিল, কিন্তু **কোঅর্ডিনেট পাওয়া যায়নি** (Google Maps
 * JavaScript চায়; সংখ্যা-বহনকারী ঠিকানাগুলো `robots.txt`-এ বন্ধ)।
 *
 * ⇒ তাই **TK BISWAS নিজে** ২১.০৮.২০২৬ সকাল ৯:১৪–৯:২০-এ ফোনের Google Maps-এ
 *   প্রতিটা চেম্বারে পিন বসিয়ে সংখ্যাগুলো পাঠিয়েছেন (ফটো-প্রুফ সহ)।
 *   নিচে প্রতিটার পাশে কোনটা কখন নিশ্চিত হয়েছে লেখা আছে।
 *
 * ⛔ একটাও সংখ্যা আন্দাজে বসানো হয়নি (TK-এর §৫ ও §১৩)।
 *    কখনো খালি হলে অ্যাপ IN TIME **নেবে না** এবং পরিষ্কার বার্তা দেবে —
 *    নীরবে গ্রহণ করবে না।
 *
 * ─── ঠিকানা মেলানো (প্রমাণসহ) ─────────────────────────────────────────────
 *   ✅ Kishanganj  — Maps লিংক ও TK-এর পিন, দুটোই "Biswas Piles Clinic"
 *   ✅ Jalpaiguri  — দুটোই "Pr. TK BISWAS · মা আয়ুর্বেদ", Raikatpara
 *   ✅ Cooch Behar — TK-এর পিন "Maa Ayurved Piles Clinic"-এর গায়ে
 *   ⚠️ Falakata    — Maps লিংক দেখাত "Kunjanagar Rd, near Railgate", কিন্তু
 *                     TK-এর পিন "Hotel Nandanik-এর কাছে" — অর্থাৎ **প্রকল্পের
 *                     নিজের ঠিকানার সঙ্গেই মেলে**। TK-এর পিনই ধরা হয়েছে।
 *   ✅ Birpara     — TK-এর পিন "MAA AYURVED PILES CLINIC"-এর গায়ে, M.G. Road
 *
 * ─── কীভাবে সংখ্যা বসাবেন (একবারের কাজ) ───────────────────────────────────
 *  ১. ফোনে Google Maps খুলুন।
 *  ২. ওই চেম্বারের ঠিক জায়গাটায় **আঙুল চেপে ধরুন** (long-press)।
 *  ৩. নিচে `26.1234, 87.5678` এরকম দুটো সংখ্যা দেখাবে — কপি করুন।
 *  ৪. নিচের `lat` ও `lng`-তে বসিয়ে দিন। `radiusMeters` দরকার হলে বদলান।
 *  ⛔ সংখ্যা বসানোর পরে **নতুন করে বিল্ড** করতে হবে।
 */
object ClinicLocations {

    /**
     * @param lat / [lng]  `null` মানে **এখনো বসানো হয়নি** — তখন ওই ব্রাঞ্চে
     *                     IN TIME নেওয়া হবে না (নীরবে গ্রহণ নয়)।
     * @param radiusMeters চেম্বার থেকে কত মিটার পর্যন্ত "ক্লিনিকে আছেন" ধরা হবে।
     */
    data class ClinicPoint(
        val branchId: String,
        val displayName: String,
        val lat: Double?,
        val lng: Double?,
        val radiusMeters: Int,
        val verifiedPlace: String,
        val mapLink: String
    ) {
        val isConfigured: Boolean get() = lat != null && lng != null
    }

    /** TK না বললে বদলাবে না। ঘরের ভিতরে GPS-এর স্বাভাবিক ভুল ধরার মতো যথেষ্ট। */
    private const val DEFAULT_RADIUS_M = 150

    // ════════════════════════════════════════════════════════════════════════
    //  ⬇⬇⬇  শুধু এখানে সংখ্যা বসান  ⬇⬇⬇
    // ════════════════════════════════════════════════════════════════════════
    val ALL: List<ClinicPoint> = listOf(

        ClinicPoint(
            branchId = "kishanganj",
            displayName = "Kishanganj",
            lat = 26.101650,   // ✅ TK নিজে Google Maps-এ পিন বসিয়ে নিশ্চিত করেছেন (২১.০৮.২০২৬, ৯:১৪) — "Biswas Piles Clinic"-এর নিজের পিন
            lng = 87.957309,
            radiusMeters = DEFAULT_RADIUS_M,
            verifiedPlace = "Biswas Piles Clinic, CALTEX CHOWK, Kishanganj, Bihar 855107",
            mapLink = "https://maps.app.goo.gl/3WoQv658CdzMhtRA6"
        ),

        ClinicPoint(
            branchId = "jalpaiguri",
            displayName = "Jalpaiguri",
            lat = 26.536368,   // ✅ TK নিজে নিশ্চিত করেছেন (৯:১৫) — "Pr. TK BISWAS · মা আয়ুর্বেদ", Raikatpara
            lng = 88.720920,
            radiusMeters = DEFAULT_RADIUS_M,
            verifiedPlace = "Pr. TK BISWAS, Netaji Subhash Chandra Bose Rd, Raikatpara, Jalpaiguri, West Bengal 735101",
            mapLink = "https://maps.app.goo.gl/mWQPDUJfepYnXhgy8"
        ),

        // ✅ TK-এর পিন ক্লিনিকের গায়েই — ঠিকানার আগের অমিল মিটে গেছে।
        ClinicPoint(
            branchId = "cooch_behar",
            displayName = "Cooch Behar",
            lat = 26.327655,   // ✅ TK নিজে নিশ্চিত করেছেন (৯:১৬) — "Maa Ayurved Piles Clinic"-এর গায়ে
            lng = 89.442545,
            radiusMeters = DEFAULT_RADIUS_M,
            verifiedPlace = "Maa Ayurved Piles Clinic, Cooch Behar Palace, Rajbari Park Rd, Cooch Behar, West Bengal 736101",
            mapLink = "https://maps.app.goo.gl/mnVFJJ436Rwx1Pff6"
        ),

        // ⚠️ Maps লিংকের ঠিকানা আলাদা ছিল; TK-এর পিন প্রকল্পের ঠিকানার সঙ্গে মেলে।
        ClinicPoint(
            branchId = "falakata",
            displayName = "Falakata",
            lat = 26.522931,   // ✅ TK নিজে পিন বসিয়েছেন (৯:১৯) — "Near Hotel Nandanik Palace, Vidya Sagar Pally" ⇒ প্রকল্পের ঠিকানার সঙ্গেই মেলে, Maps লিংকের Railgate ঠিকানা নয়
            lng = 89.201584,
            radiusMeters = DEFAULT_RADIUS_M,
            verifiedPlace = "Maa Ayurved Piles Clinic Falakata, Subash Pally, Kunjanagar Rd, near Railgate, opposite Gas Office, Falakata, Chuakhola, West Bengal 735211",
            mapLink = "https://maps.app.goo.gl/FdwYxUukwK9kTMUcA"
        ),

        // ✅ TK-এর পিন ক্লিনিকের গায়েই (M.G. Road, Debighar Colony)।
        ClinicPoint(
            branchId = "birpara",
            displayName = "Birpara",
            lat = 26.708834,   // ✅ TK নিজে পিন বসিয়েছেন (৯:২০) — "MAA AYURVED PILES CLINIC"-এর গায়ে, M.G. Road, Debighar Colony
            lng = 89.140122,
            radiusMeters = DEFAULT_RADIUS_M,
            verifiedPlace = "BIRPARA MAA AYURVED PILES CLINIC, M.G.Road, near Punjab National Bank, Debighar Colony, Birpara, Birpara Tea Garden, West Bengal 735204",
            mapLink = "https://maps.app.goo.gl/euEW22kdnE21Fove6"
        )
    )
    // ════════════════════════════════════════════════════════════════════════
    //  ⬆⬆⬆  শুধু এখানে সংখ্যা বসান  ⬆⬆⬆
    // ════════════════════════════════════════════════════════════════════════

    /** ব্রাঞ্চের নাম (যেমন "Cooch Behar") থেকে বিন্দু বের করা।
     *  `BranchCatalog`-এর মতোই নামের ছোট-বড় ও ফাঁক উপেক্ষা করা হয়। */
    fun forBranchName(branchName: String?): ClinicPoint? {
        val key = (branchName ?: "").trim().lowercase().replace(" ", "_")
        if (key.isEmpty()) return null
        return ALL.firstOrNull {
            it.branchId == key || it.displayName.trim().lowercase().replace(" ", "_") == key
        }
    }

    /** এখনো যে ব্রাঞ্চগুলোতে সংখ্যা বসানো হয়নি (পাহারাদার ও রিপোর্টের জন্য)। */
    fun unconfigured(): List<ClinicPoint> = ALL.filter { !it.isConfigured }

    /** একটাও বসানো হয়েছে কিনা। */
    fun anyConfigured(): Boolean = ALL.any { it.isConfigured }
}
