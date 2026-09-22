package com.tkbiswas.pilesclinic.native

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Native rebuild -- Reports (Master only).
 *
 * Computes the key numbers the WebView's reports() shows: totals and a
 * this-month vs last-month comparison for Enquiry, Patient and Collection.
 * Reads the live Supabase tables (enquiries / patients / payments) with a high
 * row limit so monthly totals are accurate.
 *
 * SCOPED LIMITATION (honest): the WebView also has per-staff entry counts,
 * conversion rate and branch-wise breakdown. This screen covers the headline
 * numbers most owners look at daily; the deeper breakdowns can be added next.
 */
data class BranchStat(val branch: String, val enq: Int, val pat: Int, val collection: Double)

data class StaffStat(val name: String, val enq: Int, val pat: Int)

data class ReportSummary(
    val totalEnquiries: Int,
    val totalPatients: Int,
    val totalCollection: Double,
    val enqThisMonth: Int, val enqLastMonth: Int,
    val patThisMonth: Int, val patLastMonth: Int,
    val collThisMonth: Double, val collLastMonth: Double,
    val branchRows: List<BranchStat> = emptyList(),
    val conversionRate: Int = 0,
    val staffRows: List<StaffStat> = emptyList(),
    val cashTotal: Double = 0.0,
    val upiTotal: Double = 0.0,
    val todayCollection: Double = 0.0,
    val totalDue: Double = 0.0,
    val referralPaidTotal: Double = 0.0,
    val referralDueTotal: Double = 0.0
)

class ReportsRepository {

    companion object {
        /** Every column this file reads from each table -- nothing else is
         *  asked for. Checked line by line against the code below; if a figure
         *  here is ever made to read a new column, it MUST be added here too. */
        private const val REPORT_ENQUIRY_COLS = "branch,date,createdAt,receivedBy,createdBy"
        private const val REPORT_PATIENT_COLS = "branch,registrationDate,date,bill,mobile,createdBy"
        private const val REPORT_PAYMENT_COLS = "branch,amount,mobile,payType,mode,cashAmount,onlineAmount,date,refundApprovalStatus"
        private const val REPORT_DOCTOR_COLS = "referralPaid,referralDue"
    }

    private fun ym(offsetMonths: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offsetMonths)
        return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
    }

    /** 🔒 V226 (item 86, 01.08.2026): Current-month গণনা কখনো ভুল করে ০ দেখাতে
     *  পারত যদি কোনো টেবিলে তারিখ `dd.MM.yyyy` / `dd/MM/yyyy` ধাঁচে জমা থাকত —
     *  কারণ আগের `raw.take(7)` শুধু `yyyy-MM-...` ধাঁচকে ঠিকভাবে ধরত (`01.08.2026`
     *  থেকে পেত "01.08.2", যা কোনো মাসের সঙ্গে মেলে না)। এখন তিন ধাঁচই সঠিক
     *  `yyyy-MM` বানায়; অজানা ধাঁচে আগের আচরণ (take(7)) অটুট।
     *  ⛔ `yyyy-MM-dd`/ISO সারির ফল হুবহু অপরিবর্তিত — শুধু ভুলভাবে বাদ পড়া
     *  `dd.MM.yyyy` সারি এখন সঠিক মাসে গোনা হয়। কোনো টাকা/সংখ্যার নিয়ম বদলায়নি,
     *  শুধু তারিখ-পড়া দৃঢ় করা হলো। (Owner: live data-তে যাচাই করবেন।) */
    private fun monthOf(raw: String): String {
        val s = raw.trim()
        // yyyy-MM-dd বা ISO (yyyy-MM-ddTHH:...) → আগের মতোই (ফল অপরিবর্তিত)
        if (s.length >= 7 && s[4] == '-' &&
            s[0].isDigit() && s[1].isDigit() && s[2].isDigit() && s[3].isDigit() &&
            s[5].isDigit() && s[6].isDigit()) {
            return s.take(7)
        }
        // dd.MM.yyyy বা dd/MM/yyyy → yyyy-MM
        if (s.length >= 10 && (s[2] == '.' || s[2] == '/') && (s[5] == '.' || s[5] == '/') &&
            s[0].isDigit() && s[1].isDigit() && s[3].isDigit() && s[4].isDigit() &&
            s[6].isDigit() && s[7].isDigit() && s[8].isDigit() && s[9].isDigit()) {
            val year = s.substring(6, 10)
            val month = s.substring(3, 5)
            return "$year-$month"
        }
        return s.take(7)
    }

    /** TK-ORDER (2026-07-25, branch-leak sweep): a staff must see numbers for
     *  THEIR OWN BRANCH only. Master (branchFilter = null or "All") still sees
     *  every branch exactly as before, including the branch-wise breakdown. */
    fun load(branchFilter: String? = null): ReportSummary {
        val thisYM = ym(0)
        val lastYM = ym(-1)

        val onlyBranch = branchFilter?.trim()?.takeIf { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
        fun scoped(arr: org.json.JSONArray): org.json.JSONArray {
            if (onlyBranch == null) return arr
            val out = org.json.JSONArray()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                if (r.s("branch").equals(onlyBranch, ignoreCase = true)) out.put(r)
            }
            return out
        }
        // PERFORMANCE FIX (2026-07-25): these three reads are independent, so
        // they run together instead of one after the other.
        // 🚨 TK'S ORDER (2026-07-28, khata row B26): this screen used to ask for
        // EVERY COLUMN of up to 5,000 enquiries, 5,000 patients and 5,000
        // payments -- every patient photo, every doctor's note, every history
        // log -- purely to add up counts and amounts. Below is the exact list
        // of columns this whole file reads, checked line by line:
        //   enquiries -> branch, date, createdAt, receivedBy, createdBy
        //   patients  -> branch, registrationDate, date, bill, mobile, createdBy
        //   payments  -> branch, amount, mobile, payType, mode, date
        // ⛔ NOT ONE FIGURE CHANGES: same table, same rows, same order, same
        // arithmetic below -- only the unused columns are no longer sent. And
        // if a narrowed read ever fails, fetchListSlim asks for every column
        // again by itself (see SupabaseClient), so the worst case is exactly
        // the old behaviour.
        // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): a branch staff
        // used to download EVERY branch's rows and then throw four fifths of
        // them away in scoped() above. The cloud is now asked for that one
        // branch instead.
        // ⛔ NOT ONE FIGURE CHANGES:
        //   * "ilike" is a case-insensitive exact match -- exactly the rule
        //     scoped() already applies (equals(..., ignoreCase = true)), so the
        //     very same rows come back.
        //   * scoped() itself is left in place word for word, so even an
        //     unexpected extra row would still be dropped as before.
        //   * Master (branchFilter null / "All") asks for nothing extra and is
        //     completely untouched, branch-wise breakdown included.
        val branchPart = onlyBranch?.let { "branch=ilike." + java.net.URLEncoder.encode(it, "UTF-8") }
        val preReports = runBlocking {
            val a = async(Dispatchers.IO) { SupabaseClient.fetchListSlim("enquiries", branchPart, 5000, REPORT_ENQUIRY_COLS) }
            val b = async(Dispatchers.IO) { SupabaseClient.fetchListSlim("patients", branchPart, 5000, REPORT_PATIENT_COLS) }
            val c2 = async(Dispatchers.IO) { SupabaseClient.fetchListSlim("payments", branchPart, 5000, REPORT_PAYMENT_COLS) }
            // V328: the RMP summary is independent of the three reports above.
            // Start its existing read at the same time instead of only after all
            // three finish. Same query/data/calculation; no additional request.
            val d = async(Dispatchers.IO) { SupabaseClient.fetchListSlim("doctor_visits", null, 5000, REPORT_DOCTOR_COLS) }
            listOf(a.await(), b.await(), c2.await(), d.await())
        }
        val enq = scoped(preReports[0])
        val pat = scoped(preReports[1])
        val pay = scoped(preReports[2])

        var enqT = 0; var enqL = 0
        for (i in 0 until enq.length()) {
            val d = monthOf(enq.getJSONObject(i).optString("date", enq.getJSONObject(i).optString("createdAt", "")))
            if (d == thisYM) enqT++ else if (d == lastYM) enqL++
        }

        var patT = 0; var patL = 0
        for (i in 0 until pat.length()) {
            val row = pat.getJSONObject(i)
            val d = monthOf(row.optString("registrationDate", row.optString("date", "")))
            if (d == thisYM) patT++ else if (d == lastYM) patL++
        }

        var collTotal = 0.0; var collT = 0.0; var collL = 0.0
        var cashTotal = 0.0; var upiTotal = 0.0; var todayColl = 0.0
        val paidByMobile = HashMap<String, Double>()
        // TK-REQUESTED FIX (2026-07-18): Visit Fee is a separate, one-time
        // registration fee and must never reduce the Treatment bill's Due
        // total. paidByMobile above (used for Collection totals) legitimately
        // includes every payment type -- this second tracker is ONLY for the
        // Treatment-bill-vs-paid comparison below.
        val treatmentPaidByMobile = HashMap<String, Double>()
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        // 🔴🔴🔴 TK-অডিট-অনুরোধ (01.08.2026, Follow-up-এর Due-ভুল ধরার পরে
        // সম্পূর্ণ প্রজেক্ট যাচাই): এতদিন একটা Refund সারিও প্লেইন পজিটিভ
        // Collection হিসেবে যোগ হচ্ছিল — Total/Month/Today Collection, Cash/UPI
        // Total, প্রতিটাই ভুলভাবে বেশি দেখাত, আর নিচের totalDue-ও ভুলভাবে কম।
        // Chamber Board-এ আগেই (B250/B251) লক করা নিয়ম — approved refund
        // বিয়োগ হয়, refund নিজে "Arrived"/Collection গণনা করে না — এখানেও
        // একই নিয়ম বসানো হলো।
        for (i in 0 until pay.length()) {
            val row = pay.getJSONObject(i)
            val paidEffect = when {
                PaymentModel.isApprovedRefund(row) -> -row.optDouble("amount", 0.0)
                PaymentModel.isRefundRow(row) -> 0.0
                else -> row.optDouble("amount", 0.0)
            }
            collTotal += paidEffect
            val pm = row.optString("mobile", "").filter { it.isDigit() }.takeLast(10)
            if (pm.isNotBlank()) paidByMobile[pm] = (paidByMobile[pm] ?: 0.0) + paidEffect
            val payType = row.optString("payType", "")
            if (pm.isNotBlank() && payType != "visit_fee" && payType != "attendance_mark" && !PaymentModel.isMarkerOnlyRow(payType)) {   /* 🔴🔒 V1301 (তালিকা ৪১৩): চিহ্ন-সারি (bill_edit · chamber_expected · attendance_mark) কখনো টাকা নয় */
                treatmentPaidByMobile[pm] = (treatmentPaidByMobile[pm] ?: 0.0) + paidEffect
            }
            // V452: a daily Treatment row may be MIXED. Never assign the
            // whole day to one mode; preserve Cash/Online separately.
            if (PaymentModel.isApprovedRefund(row)) {
                val refundAmt = row.optDouble("amount", 0.0)
                if (PaymentModel.normalizeMode(row.s("mode").ifBlank { "CASH" }) == "ONLINE") upiTotal -= refundAmt else cashTotal -= refundAmt
            } else if (!PaymentModel.isRefundRow(row)) {
                val split = PaymentModel.paymentSplit(row)
                cashTotal += split.first
                upiTotal += split.second
            }
            val d = monthOf(row.optString("date", ""))
            if (d == thisYM) collT += paidEffect else if (d == lastYM) collL += paidEffect
            if (row.optString("date", "").take(10) == todayStr) todayColl += paidEffect
        }

        // Total outstanding Due across all patients (bill − collected, floored at 0).
        var totalDue = 0.0
        for (i in 0 until pat.length()) {
            val row = pat.getJSONObject(i)
            val bill = row.optDouble("bill", 0.0)
            if (bill <= 0) continue
            val pm = row.optString("mobile", "").filter { it.isDigit() }.takeLast(10)
            val paid = treatmentPaidByMobile[pm] ?: 0.0
            totalDue += (bill - paid).coerceAtLeast(0.0)
        }

        // Referral income totals across all doctors (RMP commission).
        var referralPaidTotal = 0.0; var referralDueTotal = 0.0
        val docs = preReports[3]
        for (i in 0 until docs.length()) {
            val d = docs.getJSONObject(i)
            referralPaidTotal += d.optDouble("referralPaid", 0.0)
            referralDueTotal += d.optDouble("referralDue", 0.0)
        }

        val branchNames = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
        val branchRows = branchNames.map { bn ->
            var be = 0; var bp = 0; var bpay = 0.0
            for (i in 0 until enq.length()) if (enq.getJSONObject(i).s("branch") == bn) be++
            for (i in 0 until pat.length()) if (pat.getJSONObject(i).s("branch") == bn) bp++
            for (i in 0 until pay.length()) {
                val row = pay.getJSONObject(i)
                if (row.s("branch") != bn) continue
                bpay += when {
                    PaymentModel.isApprovedRefund(row) -> -row.optDouble("amount", 0.0)
                    PaymentModel.isRefundRow(row) -> 0.0
                    else -> row.optDouble("amount", 0.0)
                }
            }
            BranchStat(bn, be, bp, bpay)
        }

        // TK item 87 (2026-08-01): a conversion rate is by definition ≤100% —
        // it was showing 116% because lifetime patient rows can exceed lifetime
        // enquiry rows (direct walk-in registrations that never had an enquiry
        // row, plus pre-app migrated patients). Clamping to 0..100 removes the
        // impossible figure. NOTE: a precise "same-window / eligible-enquiry"
        // conversion needs the owner's exact definition of eligible + live data
        // (see MASTER WORK NOTE) — not guessed here.
        val conversionRate = if (enq.length() > 0)
            Math.round(pat.length() * 100.0 / enq.length()).toInt().coerceIn(0, 100) else 0

        fun last10(s: String): String = s.filter { it.isDigit() }.takeLast(10)
        val staffRows = StaffDirectory.allAccounts()
            .filter { it.role == "master" || it.role == "staff" }
            .map { acc ->
                val m = last10(acc.mobile)
                var se = 0; var sp = 0
                for (i in 0 until enq.length()) {
                    val r = enq.getJSONObject(i)
                    val who = last10(r.s("receivedBy").ifBlank { r.s("createdBy") })   // 🔴 V819 — `optString` SQL NULL-এ আক্ষরিক "null" ফেরায় (V696/V812-এর ফাঁদ); `s()` সেটা ফাঁকা ধরে
                    if (who == m && m.isNotBlank()) se++
                }
                for (i in 0 until pat.length()) {
                    val who = last10(pat.getJSONObject(i).optString("createdBy", ""))
                    if (who == m && m.isNotBlank()) sp++
                }
                StaffStat(acc.name, se, sp)
            }
            .filter { it.enq > 0 || it.pat > 0 }
            .sortedByDescending { it.enq + it.pat }

        return ReportSummary(
            totalEnquiries = enq.length(),
            totalPatients = pat.length(),
            totalCollection = collTotal,
            enqThisMonth = enqT, enqLastMonth = enqL,
            patThisMonth = patT, patLastMonth = patL,
            collThisMonth = collT, collLastMonth = collL,
            branchRows = branchRows,
            conversionRate = conversionRate,
            staffRows = staffRows,
            cashTotal = cashTotal,
            upiTotal = upiTotal,
            todayCollection = todayColl,
            totalDue = totalDue,
            referralPaidTotal = referralPaidTotal,
            referralDueTotal = referralDueTotal
        )
    }
}
