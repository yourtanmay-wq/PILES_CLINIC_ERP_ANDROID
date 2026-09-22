package com.tkbiswas.pilesclinic.print

/**
 * Branch-specific print identity. These values are copied from the website's
 * config.js (03_NETLIFY_READY/config.js) and are the only branch-dependent
 * parts of the MASTER PRINT DESIGN. (logoAssetPath still points at the real
 * bundled images under assets/www/assets/ -- only the unused JS/HTML/CSS
 * duplicate there was removed, B271, 02.08.2026.)
 */
data class BranchInfo(
    val id: String,
    val displayName: String,
    val clinicName: String,
    val addressLine: String,
    val phoneLine: String,
    val logoAssetPath: String,
    // 🔒 V1599 (১৯.০৯.২০২৬, TK-নির্দেশ): "যে ব্রাঞ্চের পুরনো ইতিহাস নেই সেই ব্রাঞ্চে
    // পুরনো বছর দেখানো বিভ্রান্তিকর" — Year-ফিল্টার মেনুতে এই বছরের আগের কোনো
    // বছর দেখানো হবে না। প্রতিটা ব্রাঞ্চ যে বছর চালু হয়েছিল।
    val startYear: Int
)

object BranchCatalog {
    /**
     * ☎️🔒 V833 (২৯.০৮.২০২৬, TK-নির্দেশ) — *"+919429690640 — এটা সর্বজনীন
     * নম্বর, অর্থাৎ প্রতিটা ব্রাঞ্চের ক্ষেত্রেই এটা হেল্পলাইন নাম্বার।
     * প্রতিটা প্রিন্ট আউটে ক্লিনিকের নাম্বারের পাশে যেন এই নম্বরটা থাকে।"*
     *
     * ⛔ **এক জায়গায় লেখা** — বদলাতে হলে শুধু এখানেই বদলাবে, সব কাগজে
     *    নিজে থেকেই বদলে যাবে। কোনো ব্রাঞ্চের নিজের নম্বর ছোঁয়া হয়নি।
     * ⛔ শুধু **ছাপার কাগজে** — WhatsApp/SMS-এর লেখায় হাত দেওয়া হয়নি
     *    (TK বলেছেন "প্রিন্ট আউটে")।
     */
    const val HELPLINE = "9429690640"

    private const val MAA = "MAA AYURVED PILES CLINIC"
    private const val KISHANGANJ_NAME = "TK BISWAS PILES CLINIC"
    private const val KISH_LOGO = "www/assets/kishanganj-final-logo.jpg"
    private const val MAA_LOGO = "www/assets/maa-ayurved-final-logo.jpg"

    val KISHANGANJ = BranchInfo(
        id = "kishanganj", displayName = "Kishanganj", clinicName = KISHANGANJ_NAME,
        addressLine = "Caltex Chowk, Modi Gola, Kishanganj", phoneLine = "8676002200",
        logoAssetPath = KISH_LOGO, startYear = 2017
    )
    val JALPAIGURI = BranchInfo(
        id = "jalpaiguri", displayName = "Jalpaiguri", clinicName = MAA,
        addressLine = "Raikatpara, Opp. Sports Complex, Jalpaiguri", phoneLine = "8436002200",
        logoAssetPath = MAA_LOGO, startYear = 2023
    )
    val COOCH_BEHAR = BranchInfo(
        id = "cooch_behar", displayName = "Cooch Behar", clinicName = MAA,
        addressLine = "Opp. Mini Bus Stand, Sengupta Complex 1st Floor, Cooch Behar", phoneLine = "8514002200",   // 🔒 খাতার সারি B33: আগে ভুল করে ফালাকাটার নম্বর বসানো ছিল; StaffDirectory-র COB-BRANCH অনুযায়ী এটাই সঠিক
        logoAssetPath = MAA_LOGO, startYear = 2025
    )
    val FALAKATA = BranchInfo(
        id = "falakata", displayName = "Falakata", clinicName = MAA,
        addressLine = "BDO Office Road, near Hotel Nandonik, Falakata", phoneLine = "8514001100",
        logoAssetPath = MAA_LOGO, startYear = 2026
    )
    val BIRPARA = BranchInfo(
        id = "birpara", displayName = "Birpara", clinicName = MAA,
        // 🔒 খাতার সারি B86 (TK, 29.07.2026 দুপুর ১.৩০): বিরপাড়ার নম্বর বদল
        // — পুরনো নম্বর TK-এর নির্দেশে চিরতরে বাদ, নতুন 8538002200।
        // ⛔ কমেন্ট আলাদা লাইনে (খাতার সারি B44-এর শিক্ষা: লাইনের শেষে
        //    কমেন্ট বসালে কোড গিলে ফেলার ঝুঁকি থাকে)।
        addressLine = "MG Road, near Axis Bank, Birpara", phoneLine = "8538002200",
        logoAssetPath = MAA_LOGO, startYear = 2026
    )

    val all = listOf(KISHANGANJ, JALPAIGURI, COOCH_BEHAR, FALAKATA, BIRPARA)

    fun byName(branch: String?): BranchInfo {
        val normalized = (branch ?: "").trim().lowercase()
            .replace("-", " ").replace("_", " ")
        return all.firstOrNull {
            normalized == it.displayName.lowercase() ||
                normalized.contains(it.displayName.lowercase())
        } ?: KISHANGANJ
    }

    /**
     * Year-ফিল্টার মেনুর সবচেয়ে পুরনো বছর কী হবে — নির্দিষ্ট একটা ব্রাঞ্চ বাছা
     * থাকলে সেই ব্রাঞ্চের startYear, "All"/ফাঁকা থাকলে সবচেয়ে পুরনো ব্রাঞ্চের
     * (Kishanganj, 2017) বছর — যাতে "All" বাছলে কোনো ব্রাঞ্চের ইতিহাসই বাদ না যায়।
     */
    fun minYearFor(branch: String?): Int {
        val b = (branch ?: "").trim()
        if (b.isBlank() || b.equals("All", ignoreCase = true)) return all.minOf { it.startYear }
        return byName(b).startYear
    }
}

object BranchSession {
    var current: BranchInfo = BranchCatalog.KISHANGANJ
}
