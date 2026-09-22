package com.tkbiswas.pilesclinic.native

import org.json.JSONObject

/**
 * 🟢🔒 V621 (২৪.০৮.২০২৬, TK-নির্দেশ) — "Visit Card থেকে Fees Return করলে
 * Chamber Date থেকে অটোমেটিক সরে যাবে, আর সেটা Draft-এর নতুন 'Return
 * Visit' তালিকায় যাবে।"
 *
 * **কারিগরি সত্য (যাচাই করে নিশ্চিত হওয়া, আন্দাজ নয়):** Chamber Date
 * বোর্ড `followups`-এর status দেখে না — ওই দিনের `payments` টেবিলে
 * (registration/visit_fee/treatment ইত্যাদি) কোনো সারি থাকলেই রোগীকে
 * "Arrived" দেখায় (`ChamberAttendanceRepository.kt`)। তাই শুধু status
 * বদলালে বা আলাদা Refund-সারি যোগ করলে Chamber Date থেকে সরবে না —
 * **আসল Fees payment-সারিটাই** সরাতে হবে।
 *
 * তাই এই ফাংশনটা:
 *  ১) সেই দিনের Fees (registration/visit_fee) payment সারি(গুলো) — প্রমাণিত
 *     `TrashHelper.moveToTrash()` দিয়ে সরানো হয় (reversible, Restore করা
 *     যায়) — এই একই কারণে Chamber Date বোর্ড আর ওই রোগীকে দেখাবে না।
 *  ২) `followups`-এর সংশ্লিষ্ট সারিতে status="Returned" বসানো হয়
 *     (প্রমাণিত `FollowUpRepository.updateStatus()` দিয়ে) — এটাই
 *     `DraftRepository`-এর নতুন "Return Visit" তালিকা চেনার চাবি।
 *
 * ⛔ Registration-এর বাকি সব তথ্য (নাম/ঠিকানা/রোগ/ছবি) অক্ষত থাকে —
 *    শুধু Fees-এর টাকা ও "arrived today" অবস্থা সরে যায়।
 */
object ReturnVisitRepository {

    data class FeePaymentsPreview(val rows: List<JSONObject>, val totalAmount: Double)

    /** আজকের Fees (registration/visit_fee) পেমেন্ট সারি(গুলো) খুঁজে দেখায় —
     *  Return করার আগে "কত টাকা ফেরত যাবে" নিশ্চিত করে দেখানোর জন্য। */
    fun previewTodayFees(mobile: String): FeePaymentsPreview? {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) return null
        return try {
            val today = PaymentModel.today()
            val rows = SupabaseClient.findByMobile("payments", digits, "*", 50)
            val feeRows = (0 until rows.length()).map { rows.getJSONObject(it) }.filter {
                val t = it.optString("payType", "").lowercase()
                (t == "registration" || t == "visitfee" || t == "visit_fee") &&
                    it.optString("date", "").take(10) == today
            }
            FeePaymentsPreview(feeRows, feeRows.sumOf { it.optDouble("amount", 0.0) })
        } catch (_: Throwable) { null }
    }

    data class ReturnResult(val movedPayments: Int, val statusUpdated: Boolean)

    /** আসল কাজ — Fees payment সারি(গুলো) Trash-এ সরানো + followups status
     *  "Returned" বসানো। ⛔ `followUpId` ফাঁকা হলে শুধু টাকাই সরে, status
     *  বদলায় না (patients টেবিল/registration নিজে অক্ষত থাকে সবসময়)। */
    fun returnFees(
        preview: FeePaymentsPreview, followUpId: String,
        byMobile: String, byName: String, context: android.content.Context?
    ): ReturnResult {
        var moved = 0
        for (row in preview.rows) {
            val ok = try { TrashHelper.moveToTrash("payments", row, byMobile) } catch (_: Throwable) { false }
            if (ok) moved++
        }
        var statusOk = false
        if (followUpId.isNotBlank()) {
            statusOk = try {
                FollowUpRepository(context).updateStatus(
                    followUpId, "Returned",
                    remark = "Fees returned — ₹" + "%,.0f".format(preview.totalAmount),
                    staffName = byName.ifBlank { byMobile }
                )
            } catch (_: Throwable) { false }
        }
        return ReturnResult(moved, statusOk)
    }
}
