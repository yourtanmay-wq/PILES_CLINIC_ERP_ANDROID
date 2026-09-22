package com.tkbiswas.pilesclinic.native

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RmpCommissionModelTest {
    @Test fun percentUsesTreatmentOnlyAndCapsAtFinalBill() {
        val s = RmpCommissionModel.calculate(25_000.0, 27_000.0,
            RmpCommissionModel.Mode.PERCENT, 10.0, 0.0)
        assertEquals(2_500.0, s.earned, 0.0)
    }

    @Test fun fixedAmountGrowsInFinalBillProportion() {
        val s = RmpCommissionModel.calculate(30_000.0, 5_000.0,
            RmpCommissionModel.Mode.AMOUNT, 3_000.0, 0.0)
        assertEquals(500.0, s.earned, 0.0)
    }

    @Test fun refundReducesFixedAmountProportionally() {
        val s = RmpCommissionModel.calculate(10_000.0, 8_000.0,
            RmpCommissionModel.Mode.AMOUNT, 3_000.0, 0.0)
        assertEquals(2_400.0, s.earned, 0.0)
    }

    @Test fun registrationVisitAndMedicineNeverCount() {
        val rows = listOf(
            JSONObject().put("id","t1").put("payType","treatment").put("amount",5_000),
            JSONObject().put("id","r1").put("payType","registration").put("amount",500),
            JSONObject().put("id","v1").put("payType","visitfee").put("amount",300),
            JSONObject().put("id","m1").put("payType","medicine").put("amount",1_000)
        )
        assertEquals(5_000.0, RmpCommissionModel.netTreatmentPaid(rows), 0.0)
    }

    @Test fun overpaymentIsSeparateAndDueNeverNegative() {
        val s = RmpCommissionModel.calculate(10_000.0, 5_000.0,
            RmpCommissionModel.Mode.PERCENT, 10.0, 900.0)
        assertEquals(0.0, s.due, 0.0)
        assertEquals(400.0, s.overpaid, 0.0)
    }

    @Test fun approvedGeneralPatientRefundReducesTreatmentEvenWithoutOldLinkId() {
        val rows = listOf(
            JSONObject().put("id","t1").put("payType","treatment").put("amount",10_000),
            JSONObject().put("id","rf1").put("payType","refund").put("refundApprovalStatus","approved").put("amount",2_000)
        )
        assertEquals(8_000.0, RmpCommissionModel.netTreatmentPaid(rows), 0.0)
    }
}
