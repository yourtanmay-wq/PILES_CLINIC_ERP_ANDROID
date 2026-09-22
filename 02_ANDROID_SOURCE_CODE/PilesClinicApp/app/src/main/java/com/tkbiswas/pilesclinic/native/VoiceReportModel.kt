package com.tkbiswas.pilesclinic.native

import java.time.LocalDate
import java.time.ZoneId

/* ═══════════════════════════════════════════════════════════════════════════
   🎤🔒 V1415 (১৩.০৯.২০২৬, TK-নির্দেশ "কাজ শুরু করে দিন", তালিকা ৫২০) —
   Search-এর ঘরে ভয়েসে/টাইপ করে প্রশ্ন করলে (যেমন "গতকাল কোচবিহারে কতজন
   পেশেন্ট এসেছিল") রিপোর্ট-উত্তর দেখানোর প্রথম ধাপ — এখন দুটো বিষয়:
   পেশেন্ট-রেজিস্ট্রেশন সংখ্যা ও টাকার কালেকশন। ৪৮টা সম্ভাব্য প্রশ্নের
   পুরো তালিকা `10_FUTURE_PLANS/VOICE_QUERY_PLAN_2026-09-13.md`-এ লেখা আছে —
   বাকিগুলো TK পরে বেছে দিলে এই একই ছাঁচে যোগ হবে।

   ⛔ এটা AI নয় — নির্দিষ্ট কয়েকটা শব্দ-ছাঁচ চেনে। ব্রাঞ্চ + সময় + বিষয়
   তিনটেই না মিললে `parse()` null দেয়; তখন "বুঝিনি" দেখানো হয়, ভুল সংখ্যা
   কখনো বানানো হয় না (TK-র সততার নিয়ম, ৫)।
   ═══════════════════════════════════════════════════════════════════════ */
object VoiceReportModel {

    enum class Metric {
        REGISTRATION_COUNT, COLLECTION, MEDICINE_SALE, SALINE_SALE, ENQUIRY_COUNT, REFUND, CASH_HANDOVER, RMP_DUE,
        MEDICINE_DUE, CALL_COUNT, TRASH_COUNT, RMP_ADVANCE,
        APPOINTMENT_COUNT, EXPECTED_COUNT, HANDOVER_PENDING, PAYMENT_REQUESTS, REFERRAL_REQUESTS, LEAVE_COUNT,
        DOCTOR_REMINDER, STAFF_REMINDER_OPEN, FEE_RETURN,
        CHAMBER_UNCLOSED, NO_SHOW, OUT_MISSING, WFH_COUNT, DUPLICATE_PATIENTS, FEE_UNPAID, CALLS_PENDING, MESSAGES_SENT,
        NEW_PATIENTS, FOLLOWUP_CALLS_DONE, DISEASE_COUNT, RMP_CALLED, RMP_CALL_DUE, FIELD_VISIT, STAFF_HOURS, STAFF_PRESENT,
        // 🎤 V1428 — কোন ব্রাঞ্চে সবচেয়ে বেশি/কম (কালেকশন · রোগী) · RMP-কে দেওয়া কমিশন · IN-বাদ
        BRANCH_TOP_COLLECTION, BRANCH_TOP_PATIENTS, RMP_PAID, IN_MISSING,
        MONTH_COMPARE_PATIENTS, MONTH_COMPARE_COLLECTION,
        // 🎤 V1503 (১৫.০৯.২০২৬, TK-নির্দেশ) — "কতজন পেশেন্ট এসেছিল" এখন সব
        // রোগী ধরে (নতুন রেজিস্ট্রেশন + পুরনো রোগীর সেদিনের ভিজিট-প্রমাণ)।
        // পুরনো REGISTRATION_COUNT অক্ষত — অন্য জায়গায় (Yearly Registration)
        // "শুধু রেজিস্ট্রেশন" অর্থেই ব্যবহৃত হয়, তাই আলাদা রাখা হলো।
        PATIENTS_VISITED,
        // 🎤🔒 V1504 (১৫.০৯.২০২৬, TK-নির্দেশ: "ভবিষ্যতে যেন এরকম সমস্যা না
        // হয়, তার পাহারা ব্যবস্থা রাখতে হবে") — RIMPA ROY/KHAGEN BHAGAT-এর
        // মতো ডুপ্লিকেট পেমেন্ট (একই রোগী+দিন+লেবেল+অঙ্ক+ধরন, কাছাকাছি
        // সময়ে) TK নিজে যেকোনো সময় জিজ্ঞেস করে ধরতে পারবেন। পুরনো
        // DUPLICATE_PATIENTS (ডুপ্লিকেট রোগী) থেকে সম্পূর্ণ আলাদা —
        // "পেমেন্ট"/"টাকা" শব্দ থাকলে তবেই এটা, নইলে আগের মতোই রোগী।
        DUPLICATE_PAYMENTS,
        // 🎤🔒 V1578 (১৭.০৯.২০২৬, আইটেম ৩৩) — অসময়ের এনকোয়ারির ইনসেন্টিভ।
        // TK: মাস = রেজিস্ট্রেশনের দিন, ব্রাঞ্চ = রোগীর ব্রাঞ্চ (প্রশ্ন করে নেওয়া)।
        INCENTIVE,
        // 🎤🔒 V1531 (১৮.০৯.২০২৬, TK-নির্দেশ) — "কতজনের কাছ থেকে টাকা এসেছে"
        // (কত জন, টাকার অঙ্ক নয়) — "কত টাকা কালেকশন" থেকে আলাদা প্রশ্ন।
        // ⛔ নতুন কোনো SQL/RPC লাগেনি — reports.collection_summary আগে থেকেই
        // patient_count ফেরত দেয় (CollectionSummary.patientCount, GlobalSearchActivity-র
        // COLLECTION-উত্তরে এখনো সাবটাইটেলে দেখা যায়) — এখানে শুধু সেটাই
        // প্রধান সংখ্যা হিসেবে দেখানো হয় যখন প্রশ্নটা টাকার বদলে মানুষ-সংখ্যা
        // নিয়ে করা।
        PAYING_PATIENTS_COUNT,
        // 🎤🔒 V1533 (১৮.০৯.২০২৬, TK-নির্দেশ) — "গত সপ্তাহে/গত মাসে কতজন পেশেন্ট
        // RMP পাঠিয়েছে" — নতুন SQL: reports.rmp_referred_count/_list।
        RMP_REFERRED_COUNT,
        // 🎤🔒 V1534 (১৮.০৯.২০২৬, TK-নির্দেশ) — "শুধু ডিসকাউন্ট কত হয়েছে" ·
        // "কতজন রোগীর ছবি তোলা হয়েছে" — নতুন SQL: reports.discount_summary/_list,
        // reports.patient_photo_count/_list।
        DISCOUNT_SUMMARY, PHOTO_COUNT
    }

    // 🩺 V1422 — বলা রোগের নাম → ডেটাবেসে যে বানানে জমা থাকে (RegistrationActivity-র ৬টা নাম)
    private val DISEASE_MAP = listOf(
        "পাইলস" to "Piles", "অর্শ" to "Piles", "piles" to "Piles",
        "ফিশার" to "Fissure", "ফিসার" to "Fissure", "fissure" to "Fissure",
        "ফিস্টুলা" to "Fistula", "ভগন্দর" to "Fistula", "fistula" to "Fistula",
        "হাইড্রোসিল" to "Hydrocele", "একশিরা" to "Hydrocele", "hydrocele" to "Hydrocele",
        "গুপ্ত" to "Gupt Rog", "gupt" to "Gupt Rog"
    )
    private fun findDisease(q: String): String? {
        val lower = q.lowercase()
        for ((k, v) in DISEASE_MAP) if (lower.contains(k)) return v
        return null
    }

    /** যে প্রশ্নগুলো "এখন পর্যন্ত মোট" — কোনো তারিখ/সময়-সীমা লাগে না (V1418–V1421)। */
    private val SNAPSHOT = setOf(Metric.RMP_DUE, Metric.MEDICINE_DUE, Metric.HANDOVER_PENDING,
        Metric.PAYMENT_REQUESTS, Metric.REFERRAL_REQUESTS, Metric.STAFF_REMINDER_OPEN,
        Metric.DUPLICATE_PATIENTS, Metric.FEE_UNPAID, Metric.CALLS_PENDING,
        Metric.DUPLICATE_PAYMENTS)   // 🎤🔒 V1504

    data class Parsed(
        val metric: Metric,
        val branch: String,
        val from: String,   // yyyy-MM-dd
        val to: String,     // yyyy-MM-dd
        val branchLabel: String,
        val periodLabel: String,
        val extra: String = ""   // V1422 — যেমন রোগের নাম (DISEASE_COUNT)
    )

    // ব্রাঞ্চের বাংলা/ইংরেজি বানান — ডেটাবেসে যে বানানে আছে সেটাই ডানদিকে।
    private val BRANCH_MAP = linkedMapOf(
        "কিশানগঞ্জ" to "Kishanganj", "kishanganj" to "Kishanganj",
        "জলপাইগুড়ি" to "Jalpaiguri", "jalpaiguri" to "Jalpaiguri",
        "কোচবিহার" to "Cooch Behar", "কুচবিহার" to "Cooch Behar",
        "cooch behar" to "Cooch Behar", "coochbehar" to "Cooch Behar",
        "ফালাকাটা" to "Falakata", "falakata" to "Falakata",
        "বীরপাড়া" to "Birpara", "birpara" to "Birpara"
    )

    private fun today(): LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata"))
    private fun iso(d: LocalDate): String = d.toString()

    /* 🎤 V1428 (তালিকা ৫৩৮) — **নাম ধরে** প্রশ্ন (আইটেম ১৬ · ১৭ · ৪৮): প্রশ্নে ইংরেজি অক্ষরের
       শব্দ (যেমন "JPE-CRP", "JAKIR HOSSAIN") থাকলে সেগুলো ছাঁকনি — তালিকার যে সারির নাম/কোডে
       **সবগুলো** শব্দ আছে শুধু সেটাই। ⚠️ সৎ সীমা: নামটা অ্যাপে যেমন ইংরেজিতে জমা, তেমন
       ইংরেজি অক্ষরেই বলতে/লিখতে হবে — বাংলায় বলা নাম ("জাকির হোসেন") ইংরেজি নামের সাথে
       মেলানোর নির্ভরযোগ্য উপায় নেই (আন্দাজে মেলানো নিষেধ, নিয়ম ৫)। প্রশ্ন-চেনার ইংরেজি
       শব্দগুলো (present, hour, reminder…) ও ব্রাঞ্চের নাম ছাঁকনিতে যায় না। */
    private val NAME_STOP = setOf(
        "RMP", "PRESENT", "REMINDER", "REMINDERS", "HOUR", "HOURS", "OUT", "WFH", "FIELD", "KM", "IN", "TIME",
        "PAID", "COMPARE", "REQUEST", "REQUESTS", "REFERRAL", "APPOINTMENT", "APPOINTMENTS", "LEAVE", "DOCTOR",
        "WHATSAPP", "SMS", "DUPLICATE", "NOSHOW", "NO", "SHOW", "ADVANCE", "CLOSE", "MOST", "WHICH", "BRANCH",
        "YESTERDAY", "TODAY", "TOMORROW", "LAST", "DAYS", "DAY", "WEEK", "MONTH", "THIS", "DIN", "MASH", "OPEN",
        "STAFF", "COMMISSION", "HOW", "MANY", "MUCH", "WAS", "WERE", "THE", "FOR", "AND", "GIVEN", "CALL", "CALLS",
        "WORK", "FROM", "HOME", "ATTENDANCE", "STILL", "NOT", "MARKED", "PATIENT", "PATIENTS", "CAME", "COLLECTION",
        "PILES", "FISSURE", "FISTULA", "HYDROCELE", "GUPT", "ROG", "OTHER", "LIST", "TOTAL", "WHO", "WHOM", "TO", "IS", "ARE", "MISSING",
        "KISHANGANJ", "JALPAIGURI", "COOCH", "BEHAR", "COOCHBEHAR", "FALAKATA", "BIRPARA",
        // 🎤🔒 V1530 — মাসের ইংরেজি নাম যেন ভুল করে কারো "নাম" হিসেবে ছাঁকনিতে না ঢোকে
        "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST",
        "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    )
    val NAME_METRICS = setOf(Metric.RMP_PAID, Metric.STAFF_PRESENT, Metric.STAFF_HOURS, Metric.STAFF_REMINDER_OPEN, Metric.OUT_MISSING, Metric.IN_MISSING)

    fun findNameTokens(q: String): String =
        Regex("[A-Za-z][A-Za-z\\-]{2,}").findAll(q).map { it.value.uppercase().trimEnd('-') }
            .filter { it.length >= 3 && it !in NAME_STOP }.distinct().joinToString(" ")

    /** সারিটা নাম-ছাঁকনিতে মেলে কিনা — `extra`-র প্রতিটা শব্দ নাম/কোডের ভিতরে থাকতে হবে। */
    fun nameMatch(extra: String, vararg fields: String): Boolean {
        if (extra.isBlank()) return true
        val hay = fields.joinToString(" ").uppercase()
        return extra.split(" ").filter { it.isNotBlank() }.all { hay.contains(it) }
    }

    private fun findBranch(q: String): Pair<String, String>? {
        val lower = q.lowercase()
        for ((k, v) in BRANCH_MAP) if (lower.contains(k.lowercase())) return v to v
        return null
    }

    private data class Range(val from: String, val to: String, val label: String)

    /* 🎤🔒 V1530 (১৮.০৯.২০২৬, TK-নির্দেশ, ছবিসহ) — নির্দিষ্ট মাসের নাম ধরে
       প্রশ্ন ("আগস্ট মাসে...")। আগে শুধু আপেক্ষিক সময় (আজ/গতকাল/এই মাস/গত
       এক মাস) চেনা হত — নির্দিষ্ট মাসের নাম একেবারেই চেনা যেত না।
       ⛔ "মে"-র মতো ছোট শব্দ আলাদা শব্দ হিসেবে (স্পেস/শুরু/শেষ ঘেরা) মেলে,
          নইলে "সময়ে"/"নামে"-এর মতো শব্দের ভিতরে ভুল করে মিলে যেত।
       বছর ধরে নেওয়ার নিয়ম: চলতি বছরের ওই মাস ভবিষ্যতে পড়লে (এখনো আসেইনি),
       গত বছরের ওই মাস ধরা হয় — ভবিষ্যতের ডেটা চাওয়া অর্থহীন। */
    private val MONTH_MAP = linkedMapOf(
        "জানুয়ারি" to 1, "january" to 1,
        "ফেব্রুয়ারি" to 2, "ফেব্রুয়ারী" to 2, "february" to 2,
        "মার্চ" to 3, "march" to 3,
        "এপ্রিল" to 4, "april" to 4,
        "মে" to 5, "may" to 5,
        "জুন" to 6, "june" to 6,
        "জুলাই" to 7, "july" to 7,
        "আগস্ট" to 8, "অগাস্ট" to 8, "august" to 8,
        "সেপ্টেম্বর" to 9, "september" to 9,
        "অক্টোবর" to 10, "october" to 10,
        "নভেম্বর" to 11, "november" to 11,
        "ডিসেম্বর" to 12, "december" to 12
    )
    private val MONTH_LABEL_EN = arrayOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    private fun containsWord(q: String, word: String): Boolean =
        Regex("(^|\\s)" + Regex.escape(word) + "(\\s|$)", RegexOption.IGNORE_CASE).containsMatchIn(q)

    private fun findNamedMonth(q: String): Range? {
        val lower = q.lowercase()
        for ((word, monthNum) in MONTH_MAP) {
            if (!containsWord(lower, word.lowercase())) continue
            val t = today()
            var year = t.year
            var start = LocalDate.of(year, monthNum, 1)
            if (start.isAfter(t)) { year -= 1; start = LocalDate.of(year, monthNum, 1) }
            val monthEnd = start.withDayOfMonth(start.lengthOfMonth())
            val end = if (monthEnd.isAfter(t)) t else monthEnd
            return Range(iso(start), iso(end), "${MONTH_LABEL_EN[monthNum]} $year")
        }
        return null
    }

    /* 🇬🇧 নিয়ম ৯ (স্টাফের পর্দায় সব লেখা ইংরেজি): এখানে .contains() শর্তে বাংলা
       শব্দ থাকে (ব্যবহারকারীর বাংলায় লেখা/বলা প্রশ্ন চেনার জন্য — অপরিহার্য),
       কিন্তু ফেরত-আসা `label`-টা সবসময় **ইংরেজি**, কারণ সেটাই পর্দায় দেখানো
       হয়। এই সব বাংলা মিলানো-শব্দ `NoBengali.kt`-এর MAP-এও যোগ করা আছে
       (পাহারা ৯.১৪ ধরে, তাই বাদ দেওয়ার সুযোগ নেই)। */
    private fun findDateRange(q: String): Range? {
        val t = today()
        return when {
            // V1420 — "আগামীকাল" (আসার কথা / অ্যাপয়েন্টমেন্ট) — "গতকাল"-এর আগে দেখা হয়, শব্দ দুটো আলাদা
            q.contains("আগামীকাল") || q.contains("tomorrow") -> Range(iso(t.plusDays(1)), iso(t.plusDays(1)), "Tomorrow")
            q.contains("গতকাল") -> Range(iso(t.minusDays(1)), iso(t.minusDays(1)), "Yesterday")
            q.contains("আজ") -> Range(iso(t), iso(t), "Today")
            q.contains("সাত দিন") || q.contains("7 din") || q.contains("last 7") || q.contains("সপ্তাহ") || q.contains("week") ->
                Range(iso(t.minusDays(6)), iso(t), "Last 7 days")
            // 🎤🔒 V1533 (TK-রিপোর্ট, ছবিসহ) — "গত মাসে" ("এক মাস" না বলেও) একই
            // অর্থে ব্যবহার হয় — আগে এটা চেনা যেত না, "Not understood" দেখাত।
            q.contains("এক মাস") || q.contains("গত মাস") || q.contains("1 mash") ->
                Range(iso(t.minusMonths(1).plusDays(1)), iso(t), "Last 1 month")
            // V1419 — "এই মাসে" = চলতি মাসের ১ তারিখ থেকে আজ পর্যন্ত
            q.contains("এই মাস") || q.contains("this month") ->
                Range(iso(t.withDayOfMonth(1)), iso(t), "This month")
            // 🎤🔒 V1530 — উপরের কোনোটাই না মিললে, নির্দিষ্ট মাসের নাম আছে কিনা দেখা হয়
            else -> findNamedMonth(q)
        }
    }

    /** স্ক্রিনে দেখানোর জন্য বাংলা তারিখ-লেবেল (দুটো ভিন্ন হলে "থেকে" দিয়ে)। */
    fun displayPeriod(from: String, to: String, label: String): String = when {
        from.isBlank() -> label
        from == to -> "${FollowUpModel.displayDate(from)} ($label)"
        else -> "${FollowUpModel.displayDate(from)} – ${FollowUpModel.displayDate(to)} ($label)"
    }

    private fun findMetric(qRaw: String): Metric? {
        // 🎤🔒 V1530 (১৮.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ) — স্টাফ/TK ফোনে টাইপ করার
        // সময় "পেশেন্ট"-এর বদলে প্রায়ই "পেসেন্ট" (স দিয়ে) লেখেন — উচ্চারণ
        // একই, বানান আলাদা। আগে শুধু "পেশেন্ট" চেনা হত, তাই এই বানানে লিখলে
        // "Not understood" দেখাত। এখন দুটো বানানই একই ধরে নেওয়া হয়।
        val q = qRaw.replace("পেসেন্ট", "পেশেন্ট")
        val hasSale = q.contains("বিক্রি")
        val hasMedicine = q.contains("মেডিসিন") || q.contains("ওষুধ")
        val hasSaline = q.contains("স্যালাইন")
        val hasEnquiry = q.contains("এনকোয়ারি")
        val hasRefund = q.contains("রিফান্ড")
        // 🎤🔒 V1534 (১৮.০৯.২০২৬, TK-নির্দেশ) — "শুধু ডিসকাউন্ট কত হয়েছে" (রিফান্ড থেকে আলাদা)
        val hasDiscount = q.contains("ডিসকাউন্ট") || q.lowercase().contains("discount")
        // 🎤🔒 V1534 — "কতজন রোগীর ছবি তোলা হয়েছে"
        val hasPhotoCount = q.contains("ছবি") && (q.contains("রোগী") || q.contains("পেশেন্ট")) && q.contains("কতজন")
        val hasHandover = q.contains("হ্যান্ডওভার")
        val hasDueWord = q.contains("বাকি") || q.contains("বাকী")
        val hasRmpDue = q.contains("কমিশন") && hasDueWord
        // 🎤 V1419 — মেডিসিন-বাকি · অ্যাপ-কল · ট্র্যাশ · RMP-অগ্রিম
        val hasProductDue = (hasMedicine || hasSaline) && hasDueWord && !hasSale
        val hasCall = q.contains("কল") && (q.contains("অ্যাপ") || q.contains("হয়েছে")) && !q.contains("ফলো")
        val hasTrash = q.contains("ডিলিট") || q.contains("ট্র্যাশ") || q.contains("মোছা") || q.contains("মুছে")
        val hasAdvance = q.contains("অগ্রিম") || q.contains("অ্যাডভান্স") || q.lowercase().contains("advance")
        // 🎤 V1420 — অ্যাপয়েন্টমেন্ট · আসার কথা · হ্যান্ডওভার-বাকি · অনুরোধ · ছুটি · রিমাইন্ডার · ফি ফেরত
        val lower = q.lowercase()
        val hasFeeReturn = q.contains("ফেরত") && (q.contains("ভিজিট") || q.contains("ফি"))
        val hasPayReq = q.contains("অনুরোধ") || q.contains("রিকোয়েস্ট") || lower.contains("request")
        val hasReferralReq = hasPayReq && (q.contains("রেফারেল") || lower.contains("referral"))
        val hasHandoverPending = hasHandover && (q.contains("হয়নি") || hasDueWord)
        val hasAppointment = q.contains("অ্যাপয়েন্টমেন্ট") || lower.contains("appointment")
        val hasExpected = q.contains("আসার কথা") || q.contains("আসবে")
        val hasLeave = q.contains("ছুটি") || lower.contains("leave")
        val hasReminder = q.contains("রিমাইন্ডার") || lower.contains("reminder")
        val hasDoctorReminder = hasReminder && (q.contains("ডাক্তার") || lower.contains("doctor"))
        // 🎤 V1421 — চেম্বার-বন্ধ-হয়নি · no-show · OUT-বাদ · WFH · ডুপ্লিকেট · ফি-জমা-পড়েনি · ফলো-আপ-কল-বাকি · বার্তা
        val hasNoShow = q.contains("আসেননি") || q.contains("আসেনি") || lower.contains("no show") || lower.contains("no-show") || lower.contains("noshow")
        val hasChamberUnclosed = q.contains("চেম্বার") && (q.contains("বন্ধ") || lower.contains("close"))
        val hasOutMissing = (lower.contains("out") || q.contains("আউট")) && (q.contains("হয়নি") || q.contains("দেয়নি"))
        val hasWfh = lower.contains("wfh") || lower.contains("work from home") || q.contains("বাড়ি থেকে")
        val hasDuplicate = q.contains("ডুপ্লিকেট") || lower.contains("duplicate")
        // 🎤🔒 V1504 — "ডুপ্লিকেট পেমেন্ট/টাকা" আলাদা, নইলে পুরনো "ডুপ্লিকেট রোগী"-ই বসে যেত।
        val hasDuplicatePayment = hasDuplicate && (q.contains("পেমেন্ট") || q.contains("টাকা") || lower.contains("payment"))
        val hasCallsPending = q.contains("ফলো") && q.contains("কল") && (hasDueWord || q.contains("হয়নি"))
        val hasMessages = q.contains("বার্তা") || q.contains("মেসেজ") || q.contains("হোয়াটসঅ্যাপ") || lower.contains("whatsapp") || lower.contains("sms")
        val hasFeeUnpaid = (q.contains("ভিজিট") || q.contains("ফি")) &&
            (q.contains("জমা পড়েনি") || q.contains("জমা হয়নি") || q.contains("দেয়নি") || q.contains("দেননি") || hasDueWord)
        // 🎤 V1422 — নতুন রোগী · ফলো-আপ কল করা · রোগ-ভিত্তিক · RMP কল/কল-করার-কথা · ফিল্ড · ঘণ্টা · হাজির
        val hasNewPatients = q.contains("শুরু হয়নি") || (q.contains("নতুন") && (q.contains("রোগী") || q.contains("পেশেন্ট")))
        val hasFuCallsDone = q.contains("ফলো") && q.contains("কল") && !hasDueWord && !q.contains("হয়নি")
        val hasRmpWord = lower.contains("rmp") || q.contains("আরএমপি") || q.contains("ডাক্তার")
        val hasRmpCallDue = hasRmpWord && q.contains("কল") && (q.contains("কথা") || q.contains("করতে হবে") || hasDueWord)
        val hasRmpCalled = hasRmpWord && q.contains("কল") && !hasRmpCallDue && !hasReminder
        // 🎤🔒 V1533 (১৮.০৯.২০২৬, TK-নির্দেশ) — "কতজন পেশেন্ট RMP পাঠিয়েছে/পাঠানো"
        // — কমিশন/কল নয়, শুধু রেফারেল-সংখ্যা (patients.refBy='Dr. Visit')।
        val hasRmpReferred = hasRmpWord && (q.contains("পাঠিয়েছে") || q.contains("পাঠানো") || q.contains("রেফার") || lower.contains("refer"))
        val hasFieldVisit = q.contains("ফিল্ড") || lower.contains("field") || q.contains("কিমি") || lower.contains(" km") || q.contains("ঘুরে")
        val hasStaffHours = q.contains("ঘণ্টা") || q.contains("ঘন্টা") || lower.contains("hour")
        val hasStaffPresent = q.contains("হাজির") || q.contains("উপস্থিত") || lower.contains("present")
        val hasDisease = findDisease(q) != null && (q.contains("রোগী") || q.contains("পেশেন্ট") || q.contains("কতজন"))
        val hasMoney = q.contains("কালেকশন") || q.contains("জমা") || (q.contains("টাকা") && !q.contains("পেশেন্ট"))
        // 🎤🔒 V1531 (১৮.০৯.২০২৬, TK-নির্দেশ) — "কতজনের কাছ থেকে টাকা এসেছে"
        // (কতজন মানুষ, টাকার অঙ্ক নয়) — সাধারণ "কত টাকা কালেকশন" থেকে আলাদা।
        val hasPayingPatientCount = hasMoney && (q.contains("কতজন") || q.contains("কয়জন"))
        val hasPatientCount = (q.contains("পেশেন্ট") || q.contains("রোগী")) &&
            (q.contains("কতজন") || q.contains("এসেছিল") || q.contains("এসেছে"))
        // 📊 V1426 (TK: "হ্যাঁ") — "এই মাসে গত মাসের তুলনায়/চেয়ে কত বেশি/কম" — দুই মাসের তুলনা
        val hasCompare = q.contains("তুলনা") || q.contains("চেয়ে") || lower.contains("compare")
        // 🎤 V1428 — কোন ব্রাঞ্চে সবচেয়ে বেশি/কম · RMP-কে কমিশন দেওয়া · IN-বাদ
        val hasBranchTop = q.contains("কোন ব্রাঞ্চ") || q.contains("কোন শাখা") || q.contains("সবচেয়ে") || lower.contains("which branch") || lower.contains("most ")
        val hasRmpPaid = q.contains("কমিশন") && !hasDueWord &&
            (q.contains("দেওয়া") || q.contains("দেয়া") || q.contains("দিয়েছি") || q.contains("পেয়েছে") || q.contains("পেল") || lower.contains("paid"))
        val hasInMissing = !hasOutMissing && (q.contains("ইন টাইম") || q.contains("ইন-টাইম") || lower.contains("in time") || Regex("(^|[^a-z])in([^a-z]|$)").containsMatchIn(lower)) &&
            (q.contains("হয়নি") || q.contains("দেয়নি") || q.contains("দেননি") || lower.contains("missing") || lower.contains("not given"))
        // 🎤🔒 V1578 (আইটেম ৩৩) — "ইনসেন্টিভ"/"incentive" শব্দ — "কমিশন" থেকে
        // আলাদা শব্দ, তাই RMP_DUE/RMP_PAID-এর সাথে কখনো মেলে না।
        val hasIncentive = q.contains("ইনসেন্টিভ") || lower.contains("incentive")
        // ⛔ ক্রমটা ওয়েবের wlv1VoiceParse-এর সাথে হুবহু এক রাখতে হবে (নিয়ম ৮)
        return when {
            hasIncentive -> Metric.INCENTIVE
            // 🔴 V1429 (যাচাইকারীর ধরা) — শুধু কালেকশন বা রোগী-সংখ্যার তুলনা; অন্য বিষয়ে
            // "কোন ব্রাঞ্চে সবচেয়ে…" বললে ভুল সংখ্যা না দিয়ে "Not understood"।
            hasBranchTop && hasMoney -> Metric.BRANCH_TOP_COLLECTION
            hasBranchTop && (q.contains("রোগী") || q.contains("পেশেন্ট") || lower.contains("patient")) -> Metric.BRANCH_TOP_PATIENTS
            hasBranchTop -> null
            hasInMissing -> Metric.IN_MISSING
            hasCompare && hasMoney -> Metric.MONTH_COMPARE_COLLECTION
            hasCompare -> Metric.MONTH_COMPARE_PATIENTS
            hasSale && hasMedicine -> Metric.MEDICINE_SALE
            hasSale && hasSaline -> Metric.SALINE_SALE
            hasNoShow -> Metric.NO_SHOW
            hasChamberUnclosed -> Metric.CHAMBER_UNCLOSED
            hasOutMissing -> Metric.OUT_MISSING
            hasWfh -> Metric.WFH_COUNT
            hasDuplicatePayment -> Metric.DUPLICATE_PAYMENTS   // 🎤🔒 V1504 — আগে চেক, নইলে নিচেরটাই জিতে যেত
            hasDuplicate -> Metric.DUPLICATE_PATIENTS
            hasCallsPending -> Metric.CALLS_PENDING
            hasFuCallsDone -> Metric.FOLLOWUP_CALLS_DONE
            hasMessages -> Metric.MESSAGES_SENT
            hasFeeUnpaid -> Metric.FEE_UNPAID
            hasFeeReturn -> Metric.FEE_RETURN
            hasReferralReq -> Metric.REFERRAL_REQUESTS
            hasPayReq -> Metric.PAYMENT_REQUESTS
            hasProductDue -> Metric.MEDICINE_DUE
            hasHandoverPending -> Metric.HANDOVER_PENDING
            hasHandover -> Metric.CASH_HANDOVER
            hasAppointment -> Metric.APPOINTMENT_COUNT
            hasExpected -> Metric.EXPECTED_COUNT
            hasLeave -> Metric.LEAVE_COUNT
            hasDoctorReminder -> Metric.DOCTOR_REMINDER
            hasReminder -> Metric.STAFF_REMINDER_OPEN
            hasFieldVisit -> Metric.FIELD_VISIT
            hasStaffHours -> Metric.STAFF_HOURS
            hasStaffPresent -> Metric.STAFF_PRESENT
            hasRmpCallDue -> Metric.RMP_CALL_DUE
            hasRmpReferred -> Metric.RMP_REFERRED_COUNT
            hasRmpCalled -> Metric.RMP_CALLED
            hasNewPatients -> Metric.NEW_PATIENTS
            hasDisease -> Metric.DISEASE_COUNT
            hasRmpPaid -> Metric.RMP_PAID
            hasAdvance -> Metric.RMP_ADVANCE
            hasRmpDue -> Metric.RMP_DUE
            hasTrash -> Metric.TRASH_COUNT
            hasCall -> Metric.CALL_COUNT
            hasDiscount -> Metric.DISCOUNT_SUMMARY
            hasPhotoCount -> Metric.PHOTO_COUNT
            hasRefund -> Metric.REFUND
            hasEnquiry -> Metric.ENQUIRY_COUNT
            // 🎤🔒 V1531 — plain hasMoney (Metric.COLLECTION)-এর আগে চেক করতে হবে,
            // নইলে "কতজনের কাছ থেকে টাকা এসেছে"ও COLLECTION-এই চলে যেত।
            hasPayingPatientCount -> Metric.PAYING_PATIENTS_COUNT
            hasMoney -> Metric.COLLECTION
            hasPatientCount -> Metric.PATIENTS_VISITED
            else -> null
        }
    }

    /** এই লেখাটা প্রশ্নের মতো মনে হচ্ছে কিনা — এটাই দেখা হয় সাধারণ নাম/নম্বর
     *  খোঁজার (ভারী ক্লাউড-পড়া) আগে, যাতে সাধারণ Search কখনো আটকে না যায়। */
    fun isQuestionLike(q: String): Boolean =
        q.contains("কত") || q.contains("কয়টা") || q.contains("কয়জন") ||   // V1447 (TK-রিপোর্ট: "কয়টা" ধরা পড়ছিল না)
            q.contains("কালেকশন") || q.contains("বিক্রি") || q.contains("হাজির") ||
            q.contains("সবচেয়ে") || q.contains("কোন ব্রাঞ্চ") || q.contains("কোন শাখা") || q.contains("কমিশন") ||   // V1428
            q.contains("ডুপ্লিকেট") || q.lowercase().contains("duplicate") ||   // 🎤🔒 V1504 — "আছে কিনা"-জাতীয় প্রশ্নেও যেন ধরা পড়ে
            q.contains("ইনসেন্টিভ") || q.lowercase().contains("incentive")   // 🎤🔒 V1578

    fun parse(q: String): Parsed? {
        // 🌐 V1423 (TK: "সব ব্রাঞ্চ মিলিয়ে মোট দেখান") — ব্রাঞ্চের নাম না বললে সব ব্রাঞ্চ মিলিয়ে
        val (branch, branchLabel) = findBranch(q) ?: (VoiceReportRepository.ALL to "All branches")
        val metric = findMetric(q) ?: return null
        val extra = when {
            metric == Metric.DISEASE_COUNT -> findDisease(q) ?: ""   // V1422 — রোগের নাম
            metric in NAME_METRICS -> findNameTokens(q)              // V1428 — নাম-ছাঁকনি (ফাঁকা = সবাই)
            metric == Metric.BRANCH_TOP_COLLECTION || metric == Metric.BRANCH_TOP_PATIENTS ->
                if (q.contains("সবচেয়ে কম") || q.lowercase().contains("least") || q.lowercase().contains("lowest")) "min" else ""   // V1428
            else -> ""
        }
        // 📊 V1426 — মাস-তুলনা: এই মাসের ১ থেকে আজ, বনাম গত মাসের ১ থেকে একই তারিখ (ন্যায্য তুলনা);
        // from/to = এই মাস, extra = "গতমাস-শুরু|গতমাস-শেষ" (তারিখ-শব্দ লাগে না)
        if (metric == Metric.MONTH_COMPARE_PATIENTS || metric == Metric.MONTH_COMPARE_COLLECTION) {
            val t = today(); val thisFrom = t.withDayOfMonth(1); val lastEnd = thisFrom.minusDays(1)
            val lastFrom = lastEnd.withDayOfMonth(1)
            val lastTo = lastEnd.withDayOfMonth(minOf(t.dayOfMonth, lastEnd.lengthOfMonth()))
            return Parsed(metric, branch, iso(thisFrom), iso(t), branchLabel, "This month vs last month, days 1–${t.dayOfMonth}", iso(lastFrom) + "|" + iso(lastTo))
        }
        // 🔒 V1418 (১৩.০৯.২০২৬) — RMP-বাকি কোনো সময়-সীমার প্রশ্ন নয় (fin.rmp_branch_due
        // "এখন পর্যন্ত মোট বাকি" দেখায়, তারিখ নেয় না), তাই এখানেই একমাত্র ব্যতিক্রম —
        // "গতকাল/আজ/সাত দিন/এক মাস" কিছু না বললেও চলবে।
        // V1419/V1420 — SNAPSHOT-এর প্রশ্নগুলো "এখন পর্যন্ত মোট", তারিখ লাগে না।
        if (metric in SNAPSHOT) return Parsed(metric, branch, "", "", branchLabel, "Right now", extra)
        val range = findDateRange(q) ?: return null
        return Parsed(metric, branch, range.from, range.to, branchLabel, range.label, extra)
    }
}
