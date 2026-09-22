package com.tkbiswas.pilesclinic.clinical

/**
 * 🔵🔒 V557 (২২.০৮.২০২৬) — TK-এর কাগজ "Patient History Sheet"-এর **ভাগ ৫**।
 *
 * TK-এর সিদ্ধান্ত:
 *   • **সম্ভাব্য কি রোগ** — ডাক্তার বদলালে **সিস্টেমেও রোগের নাম বদলে যাবে**;
 *   • **কতদিন সময় চাওয়া হল** — সংখ্যা + Days/Months/Years (রেজিস্ট্রেশনের মতোই);
 *   • **আনুমানিক খরচ** — Save চাপলে **সেই ব্রাঞ্চের সব স্টাফের কাছে 🔔 নোটিফিকেশন**;
 *   • **"জমা" এখানে থাকবে না** (টাকা জমা Payment পর্দাতেই)।
 *
 * ⛔ রোগের তালিকা **রেজিস্ট্রেশনের হুবহু একই** (`RegistrationActivity.diseaseOptions`)
 *    — নতুন কোনো রোগের নাম বানানো হয়নি।
 * ⛔ নোটিফিকেশন **নতুন করে বানানো হয়নি** — এই পর্দারই প্রমাণিত পথ
 *    (`BriefingRepository().post(..., "branch", ...)`, যেটা Patient Decision-এ চলছে)।
 * ⛔ এই ফাইলে Android-এর কিছুই নেই, তাই সিদ্ধান্তগুলো **চালিয়ে যাচাই করা যায়** —
 *    বিশেষ করে "রোগ সত্যিই বদলেছে কি না" আর "নোটিফিকেশন পাঠানো উচিত কি না",
 *    কারণ এ দুটোই **সত্যিকারের ডেটা ও স্টাফের ঘণ্টা** ছোঁয়।
 */
object CounselModel {

    /** রেজিস্ট্রেশনের হুবহু একই তালিকা। প্রথম ঘরটা "বাছুন" — কিছু না বাছলে কিছুই বদলায় না। */
    const val PICK_NONE = "বাছুন"
    val DISEASES = listOf(PICK_NONE, "Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog", "Other")

    /** "কতদিন সময় চাওয়া হল" — রেজিস্ট্রেশনের সেই একই তিনটে একক। */
    val UNITS = listOf("Days", "Months", "Years")

    private fun norm(v: String) = v.trim()

    fun cleanAmount(v: String): String = v.filter { it.isDigit() }

    fun cleanUnit(v: String): String = UNITS.firstOrNull { it.equals(norm(v), true) } ?: "Days"

    /** "15 Days" — রেজিস্ট্রেশনের `durationNote`-এর হুবহু একই ধরন। */
    fun timeAsked(amount: String, unit: String): String {
        val a = cleanAmount(amount)
        return if (a.isBlank()) "" else "$a ${cleanUnit(unit)}"
    }

    fun splitTimeAsked(saved: String): Pair<String, String> {
        val bits = norm(saved).split(" ").filter { it.isNotBlank() }
        return Pair(cleanAmount(bits.getOrElse(0) { "" }), cleanUnit(bits.getOrElse(1) { "" }))
    }

    /**
     * 🔴 রোগীর সারিতে হাত দেওয়ার একমাত্র শর্ত।
     * ⛔ ডাক্তার কিছু না বাছলে (`PICK_NONE`/ফাঁকা) **কখনো নয়**।
     * ⛔ যা আছে সেটাই আবার বাছলে **কখনো নয়** (শুধু ছোট-বড় হরফের ফারাক হলেও নয়)।
     */
    fun diseaseChanged(current: String, picked: String): Boolean {
        val p = norm(picked)
        if (p.isEmpty() || p == PICK_NONE) return false
        return !p.equals(norm(current), true)
    }

    /**
     * 🔔 নোটিফিকেশন পাঠানোর একমাত্র শর্ত — যাতে প্রতিবার Save-এ স্টাফের ঘণ্টা না বাজে।
     * ⛔ খরচ ফাঁকা/শূন্য হলে নয়; আগের বারের সমান হলে নয়; বদলালে বা নতুন বসলে হ্যাঁ।
     */
    fun shouldNotifyCost(previousCost: String, currentCost: String): Boolean {
        val now = cleanAmount(currentCost)
        if (now.isBlank() || now.toLongOrNull() == null || now.toLong() <= 0L) return false
        return now != cleanAmount(previousCost)
    }

    const val COST_TITLE = "Estimated Cost Told"

    /** স্টাফ যা পড়বেন। ⛔ "জমা" নেই (TK-এর নির্দেশ)। */
    fun costMessage(
        patientName: String, displayId: String, branch: String,
        cost: String, disease: String, timeAsked: String, byName: String
    ): String {
        val bits = ArrayList<String>()
        bits.add(listOf(patientName.trim(), displayId.trim()).filter { it.isNotBlank() }.joinToString(" · "))
        if (branch.isNotBlank()) bits.add(branch.trim())
        bits.add("আনুমানিক খরচ ₹" + cleanAmount(cost))
        if (disease.isNotBlank() && disease != PICK_NONE) bits.add("রোগ: " + disease.trim())
        if (timeAsked.isNotBlank()) bits.add("সময়: " + timeAsked.trim())
        if (byName.isNotBlank()) bits.add("বলেছেন: " + byName.trim())
        return bits.filter { it.isNotBlank() }.joinToString(" · ")
    }
}
