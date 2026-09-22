package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🔒 TK-APPROVED (28.07.2026, ফটো-প্রুফে লক · খাতার সারি B36).
 *
 * TK-এর প্রশ্ন: *"কোন স্টাফ যদি চেম্বার বন্ধ করতে ভুলে যায় তাহলে কী হবে?"*
 *
 * আগে যা ছিল: রাত ৭টা থেকে ১২টা পর্যন্ত ওই ব্রাঞ্চের **স্টাফকে** তাগাদা দেওয়া
 * হত। কিন্তু রাত ১২টার পরে আর কিছু হত না, আর **মাস্টার (TK) কখনো জানতেই
 * পারতেন না** কোন দিনের চেম্বার বন্ধ হয়নি।
 *
 * এটা সেই ফাঁকটা বন্ধ করে: বিগত কয়েক দিনের মধ্যে **কোন কোন দিনের চেম্বার বন্ধ
 * করা হয়নি** সেটা বের করে দেয়, যাতে মাস্টারের ড্যাশবোর্ডে দেখানো যায়।
 *
 * ⚡ **নেটের খরচ ইচ্ছে করে সবচেয়ে কম রাখা হয়েছে — মাত্র দুটো অনুরোধ:**
 *  ১. `chamber_close` — ওই ক'দিনে কোন কোন দিন বন্ধ করা হয়েছে
 *  ২. `payments` — ওই ক'দিনের টাকার সারি (তারিখ ও ব্রাঞ্চ ধরে)
 * (ব্রাঞ্চ-ধরে-দিন-ধরে আলাদা করে চেম্বারের বোর্ড আনা হয় না — তাতে ৩৫ বার
 *  ক্লাউডে যেতে হত, TK-এর লাইনে সেটা কখনোই করা যাবে না।)
 *
 * ⛔ কিছুই লেখা হয় না — শুধু পড়া। কোনো হিসাব বা নিয়ম বদলায় না।
 */
object ChamberUnclosedRepository {

    /** একটা দিন, যার চেম্বার বন্ধ করা হয়নি। */
    data class UnclosedDay(
        val date: String,
        val branch: String,
        val arrived: Int,
        val money: Double
    )

    private fun isoDaysAgo(n: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -n)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
    }

    /**
     * [branchFilter] ফাঁকা বা "All" হলে সব ব্রাঞ্চ; নইলে শুধু ওই ব্রাঞ্চ।
     * আজকের দিন ইচ্ছে করে বাদ — আজ তো এখনো চলছে, বন্ধ না থাকাই স্বাভাবিক।
     */
    // 🔵 TK-ORDER (07.08.2026): পড়া ব্যর্থ হলে এখন **null** ফেরে (আগে emptyList)।
    // আচরণ একই থাকে (findUnclosedCached ব্যর্থে ভুল সতর্কতা দেয় না) — কিন্তু ব্যর্থতা
    // আর ২ মিনিট "কিছু বাকি নেই" হিসেবে cache হয় না। ⛔ একই দুটো cloud-read।
    // 🔔🔒 V1437 (১৩.০৯.২০২৬, TK-নির্দেশ) — [serverBranchFilter] শুধু **নতুন
    // স্টাফ-তাগাদার কাজটার** জন্য: একটা নির্দিষ্ট ব্রাঞ্চ চাওয়া হলে টাকার
    // সারিগুলো **সার্ভারেই** ওই ব্রাঞ্চে ছেঁকে আনা হয় (প্রজেক্টের প্রমাণিত
    // একই ধাঁচ — PaymentRepository:223 · ChamberAttendanceRepository:251)।
    // নিচের হিসাবে এক অক্ষরও বদলায় না — ওই সারিগুলো এমনিতেও এখানে বাদ পড়ত;
    // শুধু পাঁচ ব্রাঞ্চের বদলে এক ব্রাঞ্চের সারি নামে, তাই স্টাফের ফোনে
    // রোজকার খরচ অনেক কম।
    // ⛔ default `false` — মাস্টারের পর্দা ও মেনুর সংখ্যা (পুরনো দুই ডাক)
    //    হুবহু আগের মতোই চলে, একটুও ছোঁয়া হয়নি।
    fun findUnclosed(
        context: Context?,
        branchFilter: String?,
        days: Int = 7,
        serverBranchFilter: Boolean = false
    ): List<UnclosedDay>? {
        return try {
            val from = isoDaysAgo(days)
            val to = isoDaysAgo(1)          // গতকাল পর্যন্ত
            if (from > to) return emptyList()

            // ---- ১) কোন কোন দিন বন্ধ করা হয়েছে ----
            val closed = HashSet<String>()
            try {
                val rows = SupabaseClient.fetchListSlim(
                    "chamber_close", "date=gte.$from&date=lte.$to", 500, "id,branch,date"
                )
                for (i in 0 until rows.length()) {
                    val r = rows.optJSONObject(i) ?: continue
                    val br = r.s("branch")
                    val dt = r.optString("date")
                    if (br.isNotBlank() && dt.isNotBlank()) closed.add(ChamberCloseRepository.idOf(br, dt))
                    val rid = r.optString("id")
                    if (rid.isNotBlank()) closed.add(rid)
                }
            } catch (_: Throwable) { return null }   // পড়া ব্যর্থ — null (আগে emptyList; ভুল সতর্কতা নয়, cache-বিষও নয়)

            // ---- ২) ওই ক'দিনে কোন দিনে কোন ব্রাঞ্চে কাজ হয়েছে ----
            val only = branchFilter?.trim()?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }

            // 🔔 V1437 — একটা ব্রাঞ্চ চাওয়া হলে (আর ডাকার জায়গা চাইলে) সার্ভারেই ছাঁকা।
            val branchPart = if (serverBranchFilter && only != null)
                "&branch=eq.${java.net.URLEncoder.encode(only, "UTF-8")}" else ""
            val pays = try {
                SupabaseClient.fetchListSlim(
                    "payments", "date=gte.$from&date=lte.$to$branchPart", 5000,
                    "id,date,branch,mobile,amount,payType,refundApprovalStatus,updatedAt"
                )
            } catch (_: Throwable) { return null }   // পড়া ব্যর্থ — null
            val people = HashMap<String, HashSet<String>>()
            val money = HashMap<String, Double>()
            val label = HashMap<String, Pair<String, String>>()   // key -> (date, branch)

            for (i in 0 until pays.length()) {
                val p = pays.optJSONObject(i) ?: continue
                val dt = p.optString("date").take(10)
                val br = p.s("branch")
                if (dt.isBlank() || br.isBlank()) continue
                if (only != null && !br.equals(only, ignoreCase = true)) continue
                // ⛔ "আসার কথা" শুধু আশা — আসা নয়। চেম্বারের বোর্ডও ঠিক এই
                // সারিটাকে "এসেছেন" হিসেবে গোনে না, তাই এখানেও গোনা হয় না।
                if (p.s("payType") == "chamber_expected") continue
                val key = ChamberCloseRepository.idOf(br, dt)
                label[key] = dt to br
                val m = p.optString("mobile").filter { it.isDigit() }.takeLast(10)
                if (m.isNotBlank()) people.getOrPut(key) { HashSet() }.add(m)
                // 🔴 TK-অডিট-অনুরোধ (01.08.2026): Refund সারিও প্লেইন যোগ হচ্ছিল।
                val moneyEffect = when {
                    PaymentModel.isApprovedRefund(p) -> -p.optDouble("amount", 0.0)
                    PaymentModel.isRefundRow(p) -> 0.0
                    else -> p.optDouble("amount", 0.0)
                }
                money[key] = (money[key] ?: 0.0) + moneyEffect
            }

            // ---- ৩) যেদিন কাজ হয়েছে অথচ বন্ধ করা হয়নি ----
            val out = ArrayList<UnclosedDay>()
            for ((key, dtBr) in label) {
                if (closed.contains(key)) continue
                // এই ফোন থেকেই মাত্র বন্ধ করা হয়েছে অথচ ক্লাউডের উত্তর এখনো
                // পুরনো — তখনও যেন তালিকায় ফিরে না আসে। (নেট লাগে না।)
                if (ChamberCloseRepository.isClosedLocally(context, dtBr.second, dtBr.first)) continue
                val arrived = people[key]?.size ?: 0
                val amt = money[key] ?: 0.0
                // TK-এর নিয়ম (স্টাফের তাগাদাতেও একই): কেউ না এলে ও টাকা না
                // নিলে ওই দিন নিয়ে কিছুই বলা হবে না।
                if (arrived <= 0 && amt <= 0.0) continue
                out.add(UnclosedDay(dtBr.first, dtBr.second, arrived, amt))
            }
            out.sortedByDescending { it.date }
        } catch (_: Throwable) {
            null   // পড়া/প্রসেস ব্যর্থ — null (আগে emptyList)
        }
    }

    // ---------------------------------------------------------------------
    // 🔒 নতুন (28.07.2026 · খাতার সারি B46) — মেনুর সংখ্যা আর "চেম্বার বন্ধ
    // করুন" পর্দা, দুটোই একই তথ্য চায়। দু'বার ক্লাউডে যাওয়া চলবে না, তাই
    // দু'মিনিটের একটা ছোট স্মৃতি রাখা হলো। ⛔ কিছুই লেখা হয় না।
    // ---------------------------------------------------------------------
    private var cacheKey: String? = null
    private var cacheAt: Long = 0L
    private var cacheVal: List<UnclosedDay> = emptyList()

    fun findUnclosedCached(context: Context?, branchFilter: String?, days: Int = 30): List<UnclosedDay> {
        val key = (branchFilter ?: "") + "|" + days
        val now = System.currentTimeMillis()
        if (cacheKey == key && now - cacheAt < 120_000L) return cacheVal
        // 🔵 TK-ORDER (07.08.2026): পড়া ব্যর্থ (null) হলে ব্যর্থতা cache করব না —
        // নইলে ২ মিনিট "কিছু বাকি নেই" আটকে থাকত। ব্যর্থে শেষ-জানা তালিকাই ফেরে।
        val out = findUnclosed(context, branchFilter, days) ?: return cacheVal
        cacheKey = key
        cacheAt = now
        cacheVal = out
        return out
    }

    /** 🟢🔒 V604 (২৪.০৮.২০২৬, TK-রিপোর্ট) — "পুরনো ডেটা থাকলেও প্রতিবার
     *  Loading দেখায় কেন?" এই ফাংশনটা নেট না ছুঁয়ে **শুধু মেমরির উপরের
     *  ক্যাশটাই** দেখে — এখনো তাজা (২ মিনিটের কম) থাকলে তালিকাটা তৎক্ষণাৎ
     *  ফেরায়, নইলে null। ⛔ `findUnclosedCached` এক অক্ষরও বদলায়নি। */
    fun peekCached(branchFilter: String?, days: Int = 30): List<UnclosedDay>? {
        val key = (branchFilter ?: "") + "|" + days
        val now = System.currentTimeMillis()
        return if (cacheKey == key && now - cacheAt < 120_000L) cacheVal else null
    }

    /** একটা দিন বন্ধ করার পরে স্মৃতিটা ফেলে দিতে হয়, নইলে পুরনো তালিকা দেখাবে। */
    fun clearCache() {
        cacheKey = null
        cacheAt = 0L
        cacheVal = emptyList()
    }
}
