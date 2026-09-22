package com.tkbiswas.pilesclinic.native

/**
 * 🆕 B337 (03.08.2026, TK-নির্দেশ) — "বিগত তারিখের পেমেন্ট করলে Master-এর
 * পারমিশন চাইছে... নির্দিষ্ট স্টাফকে নির্দিষ্ট একটা দিনের অথবা সময়ের জন্য
 * পেমেন্ট তোলা/Edit/ডিলিট করার অনুমতি দেওয়া যাবে?"
 *
 * এই ব্যবস্থা `payment_backdate_requests`-এর (স্টাফ প্রতিটা পেমেন্টে আলাদা
 * অনুরোধ পাঠায়, Master প্রতিটা আলাদা করে অনুমোদন দেন) **পাশাপাশি** চলে —
 * সেই পুরনো প্যাটার্ন এক অক্ষরও বদলায়নি। এটা একটা **বাড়তি শর্টকাট**:
 * Master একবার একটা তারিখ-সীমা দিয়ে অনুমতি দিলে, ওই সীমার মধ্যে ওই স্টাফের
 * প্রতিটা ব্যাকডেট পেমেন্টে আর আলাদা করে অনুমোদনের অপেক্ষা করতে হয় না।
 *
 * ⛔ শুধু PAYMENT-এর (তোলা/Edit/ডিলিট) জন্য — Enquiry/Registration ইত্যাদির
 *    সাধারণ ডিলিট-অনুমতি (`DeletePermission.canDeleteEntryNow`) এই ফাংশন
 *    ছোঁয় না, তাই সেখানে কোনো প্রভাব পড়ে না।
 * ⚠️ ক্লাউড ছুঁতে পারে — `Dispatchers.IO`-তে ডাকতে হবে।
 * ⚠️ TK-এর জন্য মনে রাখার কথা: সমস্ত পুরনো পেশেন্টের ডিটেইলস অ্যাপে
 *    উঠে গেলে, Briefing/ঘন্টা স্ক্রিন থেকে সক্রিয় সব অনুমতি Revoke করে
 *    দিলে আবার সবার জন্য প্রতিবার Master-এর অনুমোদন বাধ্যতামূলক হয়ে যাবে —
 *    কোড বদলানোর দরকার নেই।
 */
object BackdatePaymentGrant {

    private fun todayIso(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())

    /**
     * 🔴 V444 (19.08.2026, TK live report): Grant-এর startDate/endDate হলো
     * **অনুমতি চালু থাকার সময়**, backdated payment-এর আসল তারিখের সীমা নয়।
     * উদাহরণ: 07–31 Aug 2026 পর্যন্ত permission active থাকলে 19 Aug-এ Staff
     * 2024/2025-এর পুরনো payment-ও তুলতে/Edit/Delete করতে পারবে।
     *
     * `forDate` রাখা হয়েছে caller-contract/validation-এর জন্য; permission আছে
     * কিনা বিচার হয় **আজকের তারিখ** startDate..endDate-এর মধ্যে কিনা দেখে।
     * Branch/payment ownership guard আলাদা জায়গায় আগের মতোই বাধ্যতামূলক।
     */
    fun isGrantedNow(staffMobile: String, forDate: String): Boolean {
        return try {
            val digits = staffMobile.filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10 || forDate.isBlank()) return false
            val permissionDay = todayIso()
            // এই টেবিলে "updatedAt" নেই, তাই grantedAt দিয়ে order।
            val rows = SupabaseClient.fetchList(
                "backdate_payment_grants",
                "staffMobile=eq.$digits&active=eq.true&startDate=lte.$permissionDay&endDate=gte.$permissionDay",
                1, order = "grantedAt.desc"
            )
            if (rows.length() > 0) return true

            // Fail-safe fallback: text/date formatting-এর কারণে server-side range
            // filter না মিললে active grants এনে ফোনে **আজকের দিন** দিয়ে মিলাই।
            val all = SupabaseClient.fetchList(
                "backdate_payment_grants",
                "staffMobile=eq.$digits&active=eq.true",
                50, order = "grantedAt.desc"
            )
            for (i in 0 until all.length()) {
                val g = all.optJSONObject(i) ?: continue
                val s = g.optString("startDate").take(10)
                val e = g.optString("endDate").take(10)
                if (s.isNotBlank() && e.isNotBlank() && permissionDay >= s && permissionDay <= e) return true
            }
            false
        } catch (_: Throwable) { false }
    }

    /** Master নতুন অনুমতি তৈরি করেন। */
    fun grant(
        staffMobile: String, staffName: String, branch: String,
        startDate: String, endDate: String, note: String,
        masterMobile: String, masterName: String
    ): Boolean {
        return try {
            val row = org.json.JSONObject()
                .put("id", "bpg_" + System.currentTimeMillis() + "_" + (0..999).random())
                .put("staffMobile", staffMobile.filter { it.isDigit() }.takeLast(10))
                .put("staffName", staffName)
                .put("branch", branch)
                .put("startDate", startDate.take(10))
                .put("endDate", endDate.take(10))
                .put("note", note)
                .put("active", true)
                .put("grantedBy", masterMobile)
                .put("grantedByName", masterName)
                .put("grantedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
            SupabaseClient.upsert("backdate_payment_grants", row)
        } catch (_: Throwable) { false }
    }

    /** Master আগেভাগে বন্ধ করে দেন (মেয়াদ শেষ হওয়ার আগেই)। */
    fun revoke(id: String, masterMobile: String): Boolean {
        return try {
            val fields = org.json.JSONObject()
                .put("active", false)
                .put("revokedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
                .put("revokedBy", masterMobile)
            SupabaseClient.updateById("backdate_payment_grants", id, fields)
        } catch (_: Throwable) { false }
    }

    /** Master-এর ঘন্টা স্ক্রিনে দেখানোর জন্য — এই মুহূর্তে সক্রিয় (মেয়াদ
     *  শেষ হয়নি) সব অনুমতি, নতুন আগে। */
    fun listActive(): List<org.json.JSONObject> {
        return try {
            val today = todayIso()
            val rows = SupabaseClient.fetchList(
                "backdate_payment_grants",
                "active=eq.true&endDate=gte.$today",
                100, order = "grantedAt.desc"
            )
            (0 until rows.length()).map { rows.getJSONObject(it) }
        } catch (_: Throwable) { emptyList() }
    }
}
