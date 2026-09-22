package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.modules.ModuleAuth

/**
 * 🏖️🔒 V740 (২৭.০৮.২০২৬, TK-নির্দেশ) — **ছুটির জন্য চেম্বারের ডেট**।
 *
 * TK-এর কথা: *"চেম্বারের তারিখ প্রতি চেম্বারে সপ্তাহে দুদিন করে তারিখ থাকে।
 * এই তারিখের দিন অন্তত কেউ ছুটি পাবে না।"*
 *
 * ─────────────────────────────────────────────────────────────────────
 * ⚠️ **সবচেয়ে জরুরি কথা — এটা `ChamberDays.kt` নয়।**
 *   · `ChamberDays.kt` = **রোগী কবে আসতে পারে** (কিশানগঞ্জে রবিবার ছাড়া রোজ)
 *   · এই ফাইল       = **ডাক্তার কবে বসেন** (কিশানগঞ্জে বুধ + শনি)
 *   TK নিজে ২৭.০৮.২০২৬-এ এটা পরিষ্কার করেছেন। দুটো গুলিয়ে ফেললে ছুটির নিয়ম
 *   ভুল হবে, তাই `ChamberDays.kt` **এক অক্ষরও ছোঁয়া হয়নি**।
 *
 * ─────────────────────────────────────────────────────────────────────
 * **তালিকাটা কোথা থেকে আসে**
 *  ১. আগে **মেঘ থেকে** (`hr.branch_chamber_days`) — TK চাইলে ওখানে বদলে
 *     দিলেই হবে, **নতুন APK লাগবে না**।
 *  ২. মেঘে পৌঁছতে না পারলে **নিচের বাঁধা তালিকা** (TK নিজে ২৭.০৮.২০২৬-এ
 *     মিলিয়ে দিয়েছেন — *"সব ঠিক আছে"*)।
 *  ⇒ তাই নেট না থাকলেও নিয়মটা কাজ করে, চুপচাপ ফাঁক তৈরি হয় না।
 *
 * ⛔ ব্রাঞ্চের নাম চেনা না গেলে **কোনো দিনই আটকানো হয় না** — অচেনা কারণে
 *    কারও ছুটি আটকে যাওয়ার চেয়ে সেটা নিরাপদ (মাস্টার তো দেখছেনই)।
 * ⚡ একবার পড়ে **মনে রাখা হয়** (একই পর্দায় বারবার মেঘে যায় না) — Egress বাঁচে।
 */
object LeaveChamberDays {

    // Calendar: SUN=1 · MON=2 · TUE=3 · WED=4 · THU=5 · FRI=6 · SAT=7
    // 🔒 TK-মিলিয়ে-দেওয়া তালিকা (২৭.০৮.২০২৬) — অনুমতি ছাড়া বদলাবেন না।
    private val FALLBACK: Map<String, Set<Int>> = mapOf(
        "jalpaiguri" to setOf(java.util.Calendar.SATURDAY, java.util.Calendar.TUESDAY),
        "cooch_behar" to setOf(java.util.Calendar.MONDAY, java.util.Calendar.FRIDAY),
        "birpara" to setOf(java.util.Calendar.WEDNESDAY, java.util.Calendar.SUNDAY),
        "falakata" to setOf(
            java.util.Calendar.TUESDAY, java.util.Calendar.THURSDAY, java.util.Calendar.SATURDAY
        ),
        "kishanganj" to setOf(java.util.Calendar.WEDNESDAY, java.util.Calendar.SATURDAY)
    )

    /** মেঘ থেকে একবার পড়ার পর এখানে জমা থাকে। */
    private var cloud: Map<String, Set<Int>>? = null

    /** ⚠️ একবার চেষ্টা হয়ে গেছে কিনা। **ব্যর্থ হলেও** সত্যি হয়, তাই বারবার
     *  মেঘে যাওয়ার চেষ্টা হয় না (নইলে প্রতিবার নেট-কল = ক্র্যাশের ঝুঁকি ও
     *  অকারণ Egress)। */
    @Volatile private var tried = false

    /** সার্ভারে রবি=0 … শনি=6; Calendar-এ রবি=1 … শনি=7। */
    private fun toCalendarDow(pgDow: Int): Int = pgDow + 1

    /** ⛔ কখনো ব্যতিক্রম ছুড়ে না — ব্যর্থ হলে চুপচাপ বাঁধা তালিকাই চলে। */
    private fun loadCloudOnce() {
        if (tried) return
        tried = true
        val out = HashMap<String, Set<Int>>()
        try {
            val rows = ModuleAuth.getRows("hr", "branch_chamber_days", "select=branch,weekdays")
            for (i in 0 until rows.length()) {
                val o = rows.optJSONObject(i) ?: continue
                val branch = o.optString("branch", "")
                if (branch.isBlank()) continue
                val arr = o.optJSONArray("weekdays") ?: continue
                val set = HashSet<Int>()
                for (j in 0 until arr.length()) set.add(toCalendarDow(arr.optInt(j, -1)))
                set.remove(0)
                val id = branchId(branch)
                if (set.isNotEmpty() && id.isNotBlank()) out[id] = set
            }
        } catch (_: Throwable) { /* মেঘে পৌঁছানো গেল না — বাঁধা তালিকাই চলবে */ }
        if (out.isNotEmpty()) cloud = out
    }

    /**
     * ব্রাঞ্চের নাম → চেনা আইডি। **চেনা না গেলে খালি লেখা।**
     *
     * ⚠️ এখানে `BranchCatalog.byName()` **ইচ্ছে করেই ব্যবহার করা হয়নি** —
     *    ওটা চেনা না গেলে **কিশানগঞ্জ ধরে নেয়** (যাচাই করে দেখা)। ছুটির
     *    ক্ষেত্রে সেটা মারাত্মক হত: ব্রাঞ্চের নাম ফাঁকা বা অন্যরকম থাকলে
     *    কিশানগঞ্জের বুধ+শনি নিয়মটা ভুল করে অন্য কারও উপর বসে যেত।
     *    ⇒ তাই এখানে নিজেই মিলিয়ে নেওয়া হয়; না মিললে কিছুই আটকানো হয় না।
     */
    private fun branchId(branch: String?): String {
        val n = (branch ?: "").trim().lowercase().replace("-", " ").replace("_", " ")
        if (n.isBlank()) return ""
        return when {
            n.contains("kishanganj") -> "kishanganj"
            n.contains("jalpaiguri") -> "jalpaiguri"
            n.contains("cooch") -> "cooch_behar"
            n.contains("falakata") -> "falakata"
            n.contains("birpara") -> "birpara"
            else -> ""
        }
    }

    /**
     * এই ব্রাঞ্চের চেম্বার-দিনগুলো (Calendar-এর সংখ্যায়)। চেনা না গেলে `null`।
     *
     * ⚠️ **এটা নেট-কল করতে পারে — মূল থ্রেডে (পর্দা আঁকার সময়) কখনো ডাকবেন না**,
     *    নইলে Android অ্যাপ থামিয়ে দেয় (NetworkOnMainThreadException)।
     *    পর্দায় দেখাতে হলে আগে `preload()` (আলাদা থ্রেডে), তারপর
     *    `isChamberDateNoNet()` — ওটা কখনো নেটে যায় না।
     */
    fun weekdaysFor(branch: String?): Set<Int>? {
        val id = branchId(branch)
        if (id.isBlank()) return null
        loadCloudOnce()
        return cloud?.get(id) ?: FALLBACK[id]
    }

    /** 🧵 আলাদা থ্রেড থেকে ডাকুন — তালিকাটা একবার এনে রাখে। */
    fun preload(branch: String?) {
        try { weekdaysFor(branch) } catch (_: Throwable) { }
    }

    /** ⛔ **কখনো নেটে যায় না** — যা আনা আছে (বা বাঁধা তালিকা) তাই দিয়ে বলে।
     *  পর্দায় "এটা চেম্বারের দিন" দেখানোর জন্য এটাই ব্যবহার করুন। */
    fun isChamberDateNoNet(branch: String?, dateIso: String): Boolean {
        val id = branchId(branch)
        if (id.isBlank()) return false
        val set = cloud?.get(id) ?: FALLBACK[id] ?: return false
        return dowOf(dateIso)?.let { set.contains(it) } ?: false
    }

    /** তারিখ (yyyy-MM-dd) → সপ্তাহের দিন। না পারলে `null`।
     *  ⛔ block body — expression body-তে `return` চলে না (কম্পাইল-পাহারায় ধরা পড়েছিল)। */
    private fun dowOf(dateIso: String): Int? {
        return try {
            val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            f.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            val parsed = f.parse(dateIso)
            if (parsed == null) null else {
                val c = java.util.Calendar.getInstance(
                    java.util.TimeZone.getTimeZone("Asia/Kolkata"))
                c.time = parsed
                c.get(java.util.Calendar.DAY_OF_WEEK)
            }
        } catch (_: Throwable) { null }
    }

    /**
     * এই তারিখটা (yyyy-MM-dd) ওই ব্রাঞ্চের চেম্বারের দিন কিনা।
     * ⛔ ব্রাঞ্চ বা তারিখ চেনা না গেলে **false** — অচেনা কারণে কারও ছুটি
     *    আটকে দেওয়ার চেয়ে সেটাই নিরাপদ।
     */
    fun isChamberDate(branch: String?, dateIso: String): Boolean {
        val set = weekdaysFor(branch) ?: return false
        return dowOf(dateIso)?.let { set.contains(it) } ?: false
    }

    /** "Wed, Sat" — পর্দায় দেখানোর জন্য (সংখ্যা নেই, তাই ভাষার ঝামেলাও নেই)। */
    fun labelFor(branch: String?): String {
        val set = weekdaysFor(branch) ?: return ""
        val names = linkedMapOf(
            java.util.Calendar.SUNDAY to "Sun", java.util.Calendar.MONDAY to "Mon",
            java.util.Calendar.TUESDAY to "Tue", java.util.Calendar.WEDNESDAY to "Wed",
            java.util.Calendar.THURSDAY to "Thu", java.util.Calendar.FRIDAY to "Fri",
            java.util.Calendar.SATURDAY to "Sat"
        )
        return names.filterKeys { set.contains(it) }.values.joinToString(", ")
    }
}
