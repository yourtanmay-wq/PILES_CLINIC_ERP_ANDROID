package com.tkbiswas.pilesclinic.native

/**
 * 🏠🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু):
 * *"তাছাড়া সে যদি Work from Home করতে চায় তার ব্যাবস্থা যেন থাকে এবং
 *   মাস্টারের অনুমতি নেওয়া জরুরী Work From Home এর ক্ষেত্রে"*
 *
 * **নিয়ম (TK-এর কথা অনুযায়ী):**
 *  ১) স্টাফ ক্লিনিকের বাইরে থেকে IN TIME চাপলে "🏠 Work From Home" চাওয়ার
 *     সুযোগ পান — অনুরোধ সোজা মাস্টারের কাছে যায়।
 *  ২) **মাস্টার Approve না করা পর্যন্ত IN TIME হয় না** — অনুমতি বাধ্যতামূলক।
 *  ৩) অনুমতি **শুধু ওই একটা দিনের** (`workDate`) — পরের দিন আবার চাইতে হবে।
 *
 * ⛔ হাজিরার আর কোনো নিয়ম ছোঁয়া হয়নি — আঙুলের ছাপ · সার্ভারে সেভ · বেতনের
 *    ঘণ্টা-হিসাব সবই আগের মতোই। Work From Home দিনেও IN/OUT TIME স্টাফ নিজেই
 *    দেন, তাই ঘণ্টা **সত্যিকারের কাজের সময় ধরেই** গোনা হয় — ছুটি (is_leave)
 *    হিসেবে বসে না।
 * ⛔ `backdate_payment_grants`-এর হুবহু একই কনভেনশন (public schema, text id) —
 *    নতুন কোনো ধরন বা পথ তৈরি করা হয়নি।
 * ⚠️ ক্লাউড ছুঁতে পারে — `Dispatchers.IO`/`Thread`-এ ডাকতে হবে।
 */
object WfhRequests {

    const val TABLE = "wfh_requests"

    const val KIND_WFH = "wfh"        // 🏠 Work From Home — মাস্টারের অনুমতি লাগে
    const val KIND_BRANCH = "branch"  // 🚌 অন্য ব্রাঞ্চে ডিউটি — শুধু জানানো (V1200)

    const val STATUS_PENDING = "pending"
    const val STATUS_APPROVED = "approved"
    const val STATUS_REJECTED = "rejected"

    private fun nowIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())

    fun todayIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
            .format(java.util.Date())

    private fun digits(mobile: String): String = mobile.filter { it.isDigit() }.takeLast(10)

    /** এই স্টাফের ওই দিনের সবচেয়ে নতুন সারি (না থাকলে `null`)। */
    fun latestFor(staffMobile: String, workDate: String): org.json.JSONObject? {
        return try {
            val m = digits(staffMobile)
            if (m.length != 10 || workDate.isBlank()) return null
            val rows = SupabaseClient.fetchList(
                TABLE,
                "staffMobile=eq.$m&workDate=eq.$workDate",
                5, order = "requestedAt.desc"
            )
            if (rows.length() == 0) null else rows.optJSONObject(0)
        } catch (_: Throwable) { null }
    }

    /** মাস্টার আজকের দিনের জন্য অনুমতি দিয়েছেন কিনা — IN TIME-এর GPS-ধাপ
     *  এড়ানোর একমাত্র শর্ত। */
    fun approvedToday(staffMobile: String): Boolean =
        latestFor(staffMobile, todayIso())?.optString("status", "") == STATUS_APPROVED

    /** স্টাফ অনুরোধ পাঠান। একই দিনে আগের অনুরোধ থাকলে **নতুন সারি বসে না** —
     *  সেটার অবস্থাই ফেরত যায় (ডুপ্লিকেট নোটিশ আটকাতে, V1176-এর মতোই)। */
    /* 📌🔒 V1200 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু):
         *"work from home / work on another branch — স্টাফ এরকম যেন আগে থেকে
          বিবেচনা করে নিতে পারে"* · অন্য ব্রাঞ্চের ক্ষেত্রে *"শুধু জানিয়ে রাখলেই হবে"*।
       ⇒ দুটো নতুন ঘর: `kind` ("wfh" / "branch") ও `toBranch`; আর তারিখটা এখন
         বাইরে থেকে দেওয়া যায় (আজ **বা আগামী** যেকোনো দিন)।
       ⛔ পুরনো ডাকটা (আজকের Work From Home) হুবহু আগের মতোই চলে — নতুন ঘরগুলোর
          ডিফল্ট মান পুরনো আচরণই দেয়। */
    fun request(
        context: android.content.Context?,
        staffMobile: String, staffCode: String, staffName: String,
        branch: String, reason: String,
        workDate: String = "", kind: String = KIND_WFH, toBranch: String = ""
    ): String {
        return try {
            val m = digits(staffMobile)
            if (m.length != 10) return "Could not identify you. Please inform the Master."
            val today = workDate.ifBlank { todayIso() }
            val existing = latestFor(m, today)
            if (existing != null) {
                return when (existing.optString("status", "")) {
                    STATUS_APPROVED ->
                        if (kind == KIND_BRANCH) "Already planned for this day."
                        else "Master has already approved. Press IN TIME again."
                    STATUS_REJECTED -> "Master did not approve this day's Work From Home."
                    else -> "Request already sent — waiting for the Master."
                }
            }
            val row = org.json.JSONObject()
                .put("id", "wfh_" + System.currentTimeMillis() + "_" + (0..999).random())
                .put("staffMobile", m)
                .put("staffCode", staffCode)
                .put("staffName", staffName)
                .put("branch", branch)
                .put("workDate", today)
                .put("reason", reason)
                /* 🚌 V1200 — অন্য ব্রাঞ্চের ডিউটিতে অনুমতি লাগে না (TK-সিদ্ধান্ত),
                   তাই সারিটা সঙ্গে সঙ্গেই approved; মাস্টার শুধু খবর পান। */
                .put("status", if (kind == KIND_BRANCH) STATUS_APPROVED else STATUS_PENDING)
                .put("kind", kind)
                .put("toBranch", toBranch)
                .put("requestedAt", nowIso())
            val ok = SupabaseClient.upsert(TABLE, row)
            /* 🔔 মাস্টারের ঘন্টা বাজার জন্য নোটিশও যায় — শুধু টেবিলে বসলে
               মাস্টার Briefing না খোলা পর্যন্ত জানতেই পারতেন না। কম্পিউটারেও
               এই নোটিশ ধরেই Approve/Reject-এর বোতাম আসে (app.js)। */
            if (ok && context != null) {
                val msg = StringBuilder()
                    .append("Staff : ").append(staffCode.ifBlank { staffName }).append("\n")
                    .append("Branch : ").append(branch).append("\n")
                    .append("Date : ").append(today).append("\n")
                    .append("Reason : ").append(reason).append("\n")
                    .append("Request ID : ").append(row.optString("id", ""))
                    .toString()
                try {
                    BriefingRepository().post(
                        context,
                        if (kind == KIND_BRANCH)
                            "🚌 Duty at another branch — " + staffName.ifBlank { staffCode }
                        else "🏠 Work From Home request — " + staffName.ifBlank { staffCode },
                        msg + (if (kind == KIND_BRANCH) "\nTo branch : " + toBranch else ""),
                        "role", branch, "master", m
                    )
                } catch (_: Throwable) { }
            }
            if (ok) (if (kind == KIND_BRANCH) "Planned — the Master has been informed"
                     else "Request sent to Master")
            else "Failed - check your network"
        } catch (_: Throwable) { "Failed - check your network" }
    }

    /** 📌 V1200 — ওই দিনের প্ল্যান (Work From Home বা অন্য ব্রাঞ্চ), না থাকলে null। */
    fun planFor(staffMobile: String, workDate: String): org.json.JSONObject? =
        latestFor(staffMobile, workDate)

    /** মাস্টারের পর্দায় দেখানোর জন্য — **আজকের** অপেক্ষমাণ অনুরোধগুলো। */
    fun pendingToday(): List<org.json.JSONObject> {
        return try {
            val rows = SupabaseClient.fetchList(
                TABLE,
                "status=eq.$STATUS_PENDING&workDate=eq.${todayIso()}",
                100, order = "requestedAt.desc"
            )
            (0 until rows.length()).map { rows.getJSONObject(it) }
        } catch (_: Throwable) { emptyList() }
    }

    /** মাস্টার সিদ্ধান্ত দেন। স্টাফের ফোনে নোটিশও যায় (Briefing)। */
    fun decide(
        context: android.content.Context?, row: org.json.JSONObject,
        approve: Boolean, masterMobile: String, masterName: String
    ): Boolean {
        return try {
            val fields = org.json.JSONObject()
                .put("status", if (approve) STATUS_APPROVED else STATUS_REJECTED)
                .put("decidedBy", masterMobile)
                .put("decidedByName", masterName)
                .put("decidedAt", nowIso())
            val ok = SupabaseClient.updateById(TABLE, row.optString("id", ""), fields)
            if (ok && context != null) {
                val who = row.optString("staffName", "").ifBlank {
                    row.optString("staffCode", "").ifBlank { row.optString("staffMobile", "") }
                }
                val msg = StringBuilder()
                    .append("Staff : ").append(who).append("\n")
                    .append("Branch : ").append(row.optString("branch", "")).append("\n")
                    .append("Date : ").append(row.optString("workDate", "")).append("\n")
                    .append(if (approve) "Work From Home approved by Master."
                            else "Work From Home not approved.")
                    .toString()
                try {
                    BriefingRepository().post(
                        context,
                        (if (approve) "🏠 Work From Home approved — " else "🏠 Work From Home rejected — ") + who,
                        msg,
                        "individual",
                        row.optString("branch", ""),
                        "",
                        masterMobile,
                        row.optString("staffMobile", "")
                    )
                } catch (_: Throwable) { }
            }
            ok
        } catch (_: Throwable) { false }
    }
}
