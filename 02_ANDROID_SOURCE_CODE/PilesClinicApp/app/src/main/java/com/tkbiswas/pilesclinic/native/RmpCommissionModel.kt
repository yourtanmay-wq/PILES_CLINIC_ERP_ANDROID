package com.tkbiswas.pilesclinic.native

import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * V325 — the one RMP commission rule used by every Android screen.
 * Pure calculation only: it never reads/writes the database or changes UI.
 */
object RmpCommissionModel {
    enum class Mode { PERCENT, AMOUNT }

    data class Summary(
        val finalBill: Double,
        val netTreatmentPaid: Double,
        val earned: Double,
        val paid: Double,
        val due: Double,
        val overpaid: Double
    )

    private fun money(value: Double): Double = round(max(0.0, value) * 100.0) / 100.0

    /** Registration Fee, Visit Fee and Medicine are excluded by the existing,
     * shared PaymentModel rule. An approved general patient refund reduces the
     * base; when a refund has an original-payment link, that original must be
     * a treatment payment. */
    fun netTreatmentPaid(rows: List<JSONObject>): Double {
        val byId = rows.associateBy { it.optString("id", "") }
        var treatment = 0.0
        var refund = 0.0
        rows.forEach { row ->
            if (PaymentModel.isOrdinalTreatmentPayment(row.optString("payType"), row.optString("remarks"))) {
                treatment += row.optDouble("amount", 0.0)
            } else if (PaymentModel.isApprovedRefund(row)) {
                val originalId = row.optString("refundOfPaymentId", "")
                val original = byId[originalId]
                if (originalId.isBlank() || (original != null && PaymentModel.isOrdinalTreatmentPayment(
                        original.optString("payType"), original.optString("remarks")))) {
                    refund += row.optDouble("amount", 0.0)
                }
            }
        }
        return money(treatment - refund)
    }

    fun calculate(finalBill: Double, netTreatmentPaid: Double, mode: Mode,
                  commissionValue: Double, totalCommissionPaid: Double): Summary {
        val bill = money(finalBill)
        val collection = money(netTreatmentPaid)
        val eligible = min(collection, bill)
        val earned = if (bill <= 0.0 || commissionValue <= 0.0) 0.0 else when (mode) {
            Mode.PERCENT -> eligible * commissionValue / 100.0
            Mode.AMOUNT -> commissionValue * eligible / bill
        }
        val earnedMoney = money(earned)
        val given = money(totalCommissionPaid)
        return Summary(bill, collection, earnedMoney, given,
            money(earnedMoney - given), money(given - earnedMoney))
    }
}
