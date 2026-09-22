package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject

/**
 * ⏰🔒 V990 (০৩.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) —
 * **"আমার অসময়ের এনকোয়ারিগুলো এখন কোন ধাপে?"**
 *
 * TK-এর কথা: *"তারা যদি নাই জানতে পারে যে সেই পেশেন্টটা ট্রিটমেন্ট চালু
 * করেছে কিনা, তাহলে তারা হিসাবটা পাবে কি করে"* ·
 * *"ট্রিটমেন্ট শুরু করালে ৪০০ টাকা… শুধুমাত্র ভিজিট দিলে ১০০ টাকা…
 * দুটোই হলে ৫০০ টাকা"*।
 *
 * ⛔ **টাকার নিয়ম নতুন করে বানানো হয়নি** — ডেটাবেসের চালু নিয়মটাই
 *    (`V418_INCENTIVE_AUTO`) এখানে শুধু **দেখানো** হয়:
 *      অসময়ের এনকোয়ারি → ভিজিট ফি জমা = ₹১০০ → প্রথম চিকিৎসার টাকা = আরও ₹৪০০।
 * ⛔ একটাও সারি লেখা হয় না, শুধু পড়া। টাকার অঙ্ক এক পয়সাও এখান থেকে বদলায় না।
 * ⛔ খরচ কম রাখতে সব মিলিয়ে **দুটো** সরু পড়া — এনকোয়ারি ও পেমেন্ট।
 */
object UnexpectedIncentive {

    const val PAY_REGISTERED = 100.0
    const val PAY_TREATMENT = 400.0

    /* 💰🔒 V1184 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"Unexpected time হলে 100/-
       সেই staff ই পাবে [যে Enquiry Form ভরেছে], যে রেজিষ্ট্রেশন করেছে সে কিছুই
       পাবে না। তবে … ট্রিটমেন্ট শুরু করেন তাহলে যে All Branch Enquiry From
       Fillup করেছিল সে পাবে 350, আর যে Registration করেছে সে পাবে 50।
       08/09/2026 থেকে এই নীয়ম চালু হবে।"*

       ⇒ **এই পর্দাটা এনকোয়ারি-ফর্ম যিনি ভরেছেন তাঁরই** — তাই নতুন নিয়মে
         তাঁর প্রাপ্য: ভিজিট ফি জমা পড়লে ₹১০০, ট্রিটমেন্ট শুরু হলে আরও ₹৩৫০।
       ⛔ ০৮.০৯.২০২৬-এর আগের রেজিস্ট্রেশনে **পুরনো নিয়মই** (V418-এর সমান-ভাগ)
          দেখানো হয়, তাই আগের কোনো অঙ্ক পর্দায় বদলে যায় না।
       ⛔ টাকার আসল হিসাব ডেটাবেসেই (`hr.incentive_wanted()`); এখানে শুধু
          **সেই একই নিয়মটা দেখানো** হয়, যাতে দুই জায়গায় দুরকম না বলে। */
    const val NEW_RULE_FROM = "2026-09-08"
    const val NEW_PAY_ENQUIRY_REG = 100.0
    const val NEW_PAY_ENQUIRY_TRT = 350.0
    const val NEW_PAY_REGISTRAR_TRT = 50.0

    /** `stage`: "none" = এখনো আসেননি · "visit" = ভিজিট দিয়েছেন · "treatment" = চিকিৎসা শুরু। */
    data class Row(
        val name: String,
        val mobile: String,
        val branch: String,
        val callAt: String,      // এনকোয়ারির তারিখ ও সময় (এটাই টাকার শর্ত)
        val stage: String,
        val stageAt: String,     // ওই ধাপের তারিখ ও সময়
        val earned: Double
    )

    /* ⛔ যে ধরনের সারিগুলো "চিকিৎসার টাকা" নয় — Chamber ও Payment পর্দার
       হুবহু একই তালিকা, তাই কোথাও দুরকম হিসাব হতে পারে না। */
    private val NOT_TREATMENT = setOf(
        "visit_fee", "visitfee", "registration",
        "attendance_mark", "bill_edit", "chamber_expected", "refund"
    )

    /* 💰 V1029 — রেজিস্ট্রেশনের ফি যে যে নামে জমা হতে পারে, হুবহু
       `hr.incentive_wanted()`-এর তালিকা। দুই জায়গায় এক নিয়ম, তাই বেতনের
       অঙ্কের সঙ্গে এই পর্দা আর কখনো আলাদা কথা বলবে না। */
    private val FEE_TYPES = setOf("visit_fee", "visitfee", "registration")

    private fun digits(v: String) = v.filter { it.isDigit() }.takeLast(10)

    /** স্যালারি পর্দা person-code দিয়ে চলে; সেখান থেকে মোবাইল বের করা।
     *  ⛔ না মিললে ফাঁকা — তখন পর্দা নিজের (লগ-ইন করা) নম্বরই ধরে। */
    fun mobileForCode(code: String): String {
        val want = code.trim()
        if (want.isBlank()) return ""
        for (a in StaffDirectory.allAccounts()) {
            if (a.name.trim().equals(want, ignoreCase = true)) return digits(a.mobile)
        }
        return ""
    }

    /** একজন স্টাফের অসময়ে নেওয়া সব এনকোয়ারি, প্রতিটার এখনকার ধাপ সহ। */
    fun forStaff(staffMobile: String): List<Row> {
        val me = digits(staffMobile)
        if (me.length != 10) return emptyList()

        val enc = java.net.URLEncoder.encode("Unexpected Time", "UTF-8")
        val enq = try {
            SupabaseClient.fetchListSlimOrNull(
                "enquiries", "receivedBy=eq.$me&timeType=eq.$enc", 500,
                "id,name,mobile,branch,date,timeType,receivedBy,createdAt",
                order = "createdAt.desc.nullslast"
            )
        } catch (_: Throwable) { null } ?: return emptyList()
        if (enq.length() == 0) return emptyList()

        val mobiles = ArrayList<String>()
        for (i in 0 until enq.length()) {
            val m = digits(enq.optJSONObject(i)?.s("mobile").orEmpty())
            if (m.length == 10 && m !in mobiles) mobiles.add(m)
        }
        val pays = payFor(mobiles)
        /* 💰🔒 V1184 — কে রেজিস্ট্রেশন করেছেন ও কোন তারিখে, সেটা জানা না
           থাকলে প্রাপ্য অঙ্কটা ঠিকভাবে বলা যায় না (নতুন নিয়ম তারিখ ধরে চলে)।
           ⛔ একটাই সরু ব্যাচ-পড়া, শুধু তিনটে ঘর — খরচ নগণ্য।
           ⛔ ব্যর্থ হলে কিছুই ভাঙে না; তখন আগের মতোই দেখানো হয়। */
        val regBy = HashMap<String, String>()
        val regOn = HashMap<String, String>()
        for (part in mobiles.chunked(25)) {
            val list = part.joinToString(",")
            val rows = try {
                SupabaseClient.fetchListSlimOrNull(
                    "patients", "mobile=in.($list)", 500,
                    "id,mobile,registeredBy,registrationDate,date", order = "id.asc"
                )
            } catch (_: Throwable) { null } ?: continue
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val m = digits(r.s("mobile"))
                if (m.length != 10) continue
                regBy[m] = digits(r.s("registeredBy"))
                regOn[m] = r.s("registrationDate").ifBlank { r.s("date") }.take(10)
            }
        }

        // মোবাইল ধরে — প্রথম ভিজিট ফি, আর প্রথম চিকিৎসার টাকা
        val firstVisit = HashMap<String, String>()
        val firstTreat = HashMap<String, String>()
        for (i in 0 until pays.length()) {
            val p = pays.optJSONObject(i) ?: continue
            val m = digits(p.s("mobile"))
            if (m.length != 10) continue
            val t = p.s("payType").lowercase()
            val amt = p.optDouble("amount", 0.0)
            val at = p.s("createdAt").ifBlank { p.s("date") }
            if (at.isBlank()) continue
            /* 🐞🔒 V1029 (০৩.০৯.২০২৬, TK-রিপোর্ট: বেতনে ₹৮০০ বাকি, অথচ এই
               পর্দায় সবার ₹০ ও "Not come to the branch yet")। কারণ (যাচাই করা):
               টাকা যে নিয়মে দেওয়া হয় (`hr.incentive_wanted()`), সেখানে
               রেজিস্ট্রেশনের ফি ধরা হয় **তিন রকম নামে** —
               `visit_fee` · `visitfee` · `registration`। এই পর্দা শুধু প্রথমটাই
               দেখত, তাই `registration` নামে টাকা নেওয়া রোগীরা "আসেনি" দেখাত।
               ⇒ এখন তিনটেই ধরা হয়, ঠিক ওই নিয়মেরই মতো। */
            if (t in FEE_TYPES) {
                val old = firstVisit[m]
                if (old == null || at < old) firstVisit[m] = at
            } else if (t !in NOT_TREATMENT && amt > 0.0) {
                val old = firstTreat[m]
                if (old == null || at < old) firstTreat[m] = at
            }
        }

        val out = ArrayList<Row>()
        val seen = HashSet<String>()
        for (i in 0 until enq.length()) {
            val e = enq.optJSONObject(i) ?: continue
            val m = digits(e.s("mobile"))
            if (m.length != 10 || !seen.add(m)) continue      // এক নম্বর = এক কার্ড
            val treat = firstTreat[m]
            val visit = firstVisit[m]
            val stage = when {
                treat != null -> "treatment"
                visit != null -> "visit"
                else -> "none"
            }
            /* 💰🔒 V1184 (TK-নির্দেশ ০৭.০৯.২০২৬) — প্রাপ্য অঙ্কটা ঠিক
               ডেটাবেসের নিয়ম (`hr.incentive_wanted()`) ধরেই বলা হয়, যাতে
               এই পর্দা আর বেতনের পর্দা কখনো দুরকম কথা না বলে (নিয়ম ৭ক-এর ২)।
                 • রেজিস্ট্রেশন ০৮.০৯.২০২৬ বা তার পরে (নতুন নিয়ম) ⇒
                   এনকোয়ারি-ফর্মকারী পান ₹১০০, ট্রিটমেন্ট শুরু হলে আরও ₹৩৫০।
                 • তার আগের রেজিস্ট্রেশনে (পুরনো নিয়ম) ⇒ ₹১০০ ও ₹৪০০ **সমান
                   ভাগ** হয় এনকোয়ারি-ফর্মকারী ও রেজিস্ট্রেশন-কারীর মধ্যে;
                   দুজন একই লোক হলে (বা রেজিস্ট্রেশন-কারী স্টাফ-তালিকায় না
                   থাকলে) পুরোটাই একজনের।
               ⛔ তারিখটা জানা না গেলে **পুরনো নিয়মই** ধরা হয় — আন্দাজে নতুন
                  নিয়মের বেশি টাকা দেখানো হয় না। */
            val rBy = regBy[m].orEmpty()
            val rOn = regOn[m].orEmpty()
            val newEra = rOn.length == 10 && rOn >= NEW_RULE_FROM
            val otherRegistrar =
                rBy.length == 10 && rBy != me && StaffDirectory.findAccount(rBy) != null
            val earned = if (newEra) {
                when (stage) {
                    "treatment" -> NEW_PAY_ENQUIRY_REG + NEW_PAY_ENQUIRY_TRT
                    "visit" -> NEW_PAY_ENQUIRY_REG
                    else -> 0.0
                }
            } else {
                val share = if (otherRegistrar) 2.0 else 1.0
                when (stage) {
                    "treatment" -> (PAY_REGISTERED + PAY_TREATMENT) / share
                    "visit" -> PAY_REGISTERED / share
                    else -> 0.0
                }
            }
            out.add(
                Row(
                    name = e.s("name").ifBlank { "(no name)" },
                    mobile = m,
                    branch = e.s("branch"),
                    callAt = e.s("createdAt").ifBlank { e.s("date") },
                    stage = stage,
                    stageAt = treat ?: visit.orEmpty(),
                    earned = earned
                )
            )
        }
        return out
    }

    private fun payFor(mobiles: List<String>): JSONArray {
        val all = JSONArray()
        for (part in mobiles.chunked(25)) {
            val list = part.joinToString(",")
            val rows = try {
                SupabaseClient.fetchListSlimOrNull(
                    "payments", "mobile=in.($list)", 2000,
                    "id,mobile,amount,payType,date,createdAt", order = "createdAt.asc.nullslast"
                )
            } catch (_: Throwable) { null } ?: continue
            for (i in 0 until rows.length()) all.put(rows.optJSONObject(i) ?: continue)
        }
        return all
    }

    /** "02.09.2026 · 11.40 PM" — তারিখ আর সময় দুটোই (TK-নির্দেশ)। */
    fun dateTime(raw: String): String {
        val t = raw.trim()
        if (t.length < 10) return ""
        val d = try {
            val p = t.substring(0, 10).split("-")
            p[2] + "/" + p[1] + "/" + p[0]
        } catch (_: Throwable) { return "" }
        if (t.length < 16) return d
        return try {
            val hh = t.substring(11, 13).toInt()
            val mm = t.substring(14, 16)
            val ap = if (hh >= 12) "PM" else "AM"
            val h12 = when { hh == 0 -> 12; hh > 12 -> hh - 12; else -> hh }
            "$d  ·  $h12.$mm $ap"
        } catch (_: Throwable) { d }
    }

    fun money(v: Double): String = "₹" + "%,.0f".format(v)

    /** চলতি মাসে এখন পর্যন্ত কত পাওনা হয়েছে। */
    fun thisMonthTotal(rows: List<Row>): Double {
        val ym = try {
            java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US).format(java.util.Date())
        } catch (_: Throwable) { "" }
        return rows.filter { it.stageAt.length >= 7 && it.stageAt.substring(0, 7) == ym }
            .sumOf { it.earned }
    }
}
