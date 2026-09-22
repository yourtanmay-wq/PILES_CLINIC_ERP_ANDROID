package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.modules.ModuleAuth
import org.json.JSONArray
import org.json.JSONObject

/* 🎤🔒 V1415 (১৩.০৯.২০২৬, TK-নির্দেশ "কাজ শুরু করে দিন", তালিকা ৫২০) —
 * Search-এর ভয়েস/টাইপ-প্রশ্নের জবাবের জন্য সার্ভারের `reports` স্কিমার ছোট্ট
 * ফাংশনগুলো (কোনো বাল্ক-ডাউনলোড নয়, প্রতিটা ডাক থেকে মাত্র একটা সংখ্যা/অল্প
 * কয়েকটা সারি আসে — ফ্রি প্ল্যান-নিরাপদ)।
 * ⛔ নামটা ইচ্ছে করে `ReportsRepository` নয় — সেই নামে আগে থেকেই একটা ক্লাস
 * আছে (Master-এর পুরনো Reports পর্দার জন্য, `ReportsRepository.kt`); প্রথমবার
 * ভুল করে সেটার উপরেই লিখে ফেলেছিলাম, git থেকে ফিরিয়ে এই আলাদা নামে আনা হলো। */
object VoiceReportRepository {
    data class RepoResult<T>(val ok: Boolean, val value: T? = null, val message: String = "")

    /* 🌐 V1423 (১৩.০৯.২০২৬, TK: "হ্যাঁ, সব ব্রাঞ্চ মিলিয়ে মোট দেখান") — প্রশ্নে ব্রাঞ্চের নাম
     * না থাকলে p_branch = "ALL": সার্ভারের একই ছোট্ট ফাংশন পাঁচ ব্রাঞ্চের জন্য পাঁচবার ডাকা হয়
     * (প্রতিবার শুধু সংখ্যা/অল্প সারি আসে — ফ্রি প্ল্যান-নিরাপদ) আর ফল জোড়া হয়:
     *   `_list` ফাংশন → সারিগুলো একসাথে; সংখ্যা-ফেরত ফাংশন → যোগ; এক-সারির summary → প্রতিটা
     *   সংখ্যার ঘর যোগ (সব ঘরই গোনা/যোগফল, গড় কোথাও নেই — তাই যোগ করা সঠিক)।
     * ⛔ SQL-এ কিছু বদলায়নি — ব্রাঞ্চ-পাহারা যেমন ছিল তেমনই। */
    const val ALL = "ALL"
    private val ALL_BRANCHES = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")

    private fun reportsRpc(fn: String, args: JSONObject): ModuleAuth.RpcResult {
        if (args.optString("p_branch") != ALL) return ModuleAuth.rpc("reports", fn, args)
        val bodies = ArrayList<String>()
        for (b in ALL_BRANCHES) {
            val r = ModuleAuth.rpc("reports", fn, JSONObject(args.toString()).put("p_branch", b))
            if (!r.ok) return r
            bodies.add(r.body.trim())
        }
        return try { ModuleAuth.RpcResult(true, mergeBodies(fn, bodies), "") } catch (_: Exception) { ModuleAuth.RpcResult(false, "", "Invalid response") }
    }

    private fun numOf(v: Any?): Double? = when (v) {
        is Number -> v.toDouble()
        is String -> v.trim().toDoubleOrNull()
        else -> null
    }

    private fun mergeBodies(fn: String, bodies: List<String>): String {
        if (fn.endsWith("_list")) {
            val out = JSONArray()
            for (b in bodies) { val arr = JSONArray(b); for (i in 0 until arr.length()) out.put(arr.get(i)) }
            return out.toString()
        }
        if (bodies.all { it == "null" || it.toDoubleOrNull() != null }) {
            if (bodies.any { it == "null" }) return "null"
            return if (bodies.all { it.toIntOrNull() != null }) bodies.sumOf { it.toInt() }.toString() else bodies.sumOf { it.toDouble() }.toString()
        }
        val merged = JSONObject()
        for (b in bodies) {
            val arr = JSONArray(b)
            if (arr.length() == 0) continue
            val row = arr.getJSONObject(0)
            val keys = row.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val n = numOf(row.opt(k))
                if (n != null) merged.put(k, merged.optDouble(k, 0.0) + n) else if (!merged.has(k)) merged.put(k, row.opt(k))
            }
        }
        return JSONArray().put(merged).toString()
    }

    data class RegisteredPatient(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val registrationDate: String, val branch: String = "")
    /** 🎤 V1503 — "কতজন পেশেন্ট এসেছিল": নতুন রেজিস্ট্রেশন + পুরনো রোগীর সেদিনের ভিজিট-প্রমাণ, দুটো মিলিয়ে। */
    data class VisitedPatient(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val visitDate: String)
    data class CollectionSummary(val total: Double, val patientCount: Int, val paymentCount: Int)
    data class CollectionRow(val paymentId: String, val patientRowId: String, val name: String, val mobile: String, val amount: Double, val mode: String, val payType: String, val paidOn: String, val branch: String = "")
    data class ProductSaleSummary(val total: Double, val saleCount: Int)
    data class ProductSaleRow(val productRowId: String, val customer: String, val mobile: String, val product: String, val bill: Double, val deposit: Double, val due: Double, val mode: String, val soldOn: String, val branch: String = "")
    data class EnquiryRow(val enquiryRowId: String, val name: String, val mobile: String, val disease: String, val enquiryDate: String, val branch: String = "")
    data class RefundSummary(val total: Double, val refundCount: Int)
    data class RefundRow(val paymentId: String, val patientRowId: String, val name: String, val mobile: String, val amount: Double, val mode: String, val refundedOn: String, val branch: String = "")
    data class HandoverSummary(val total: Double, val dayCount: Int)
    data class HandoverRow(val handoverDate: String, val cash: Double, val receiverName: String, val receivedAt: String, val branch: String = "")
    data class RmpDueSummary(val totalDue: Double, val rmpCount: Int)
    data class RmpDueRow(val rmpId: String, val rmpName: String, val rmpMobile: String, val due: Double, val branch: String = "")
    data class ProductDueSummary(val total: Double, val rowCount: Int)
    data class ProductDueRow(val productRowId: String, val customer: String, val mobile: String, val product: String, val due: Double, val soldOn: String, val branch: String = "")
    data class CallRow(val callRowId: String, val staffCode: String, val targetMobileMask: String, val callDate: String, val branch: String = "")
    data class TrashRow(val trashRowId: String, val tableName: String, val deletedAt: String, val deletedBy: String, val branch: String = "")
    data class RmpAdvanceSummary(val total: Double, val advanceCount: Int)
    data class RmpAdvanceRow(val advanceId: String, val rmpName: String, val amount: Double, val mode: String, val paidOn: String, val branch: String = "")
    // V1420
    data class AppointmentRow(val enquiryRowId: String, val name: String, val mobile: String, val disease: String, val appointmentDate: String, val registered: Boolean, val branch: String = "")
    data class ExpectedRow(val markId: String, val patientRowId: String, val name: String, val mobile: String, val expectedOn: String, val branch: String = "")
    data class HandoverPendingSummary(val total: Double, val dayCount: Int)
    data class HandoverPendingRow(val handoverDate: String, val cash: Double, val status: String, val branch: String = "")
    data class PaymentRequestsSummary(val backdate: Int, val edit: Int, val refund: Int, val total: Int)
    data class PaymentRequestRow(val requestId: String, val requestType: String, val name: String, val mobile: String, val amount: Double, val requestedOn: String, val branch: String = "")
    data class ReferralRequestsSummary(val total: Int, val deleteCount: Int)
    data class ReferralRequestRow(val requestId: String, val requestType: String, val newAmount: Double, val requestedOn: String, val branch: String = "")
    data class LeaveSummary(val total: Int, val confirmed: Int, val pending: Int, val rejected: Int)
    data class LeaveRow(val staffCode: String, val leaveDate: String, val status: String, val appliedOn: String, val branch: String = "")
    data class DoctorReminderSummary(val total: Int, val notAccepted: Int)
    data class DoctorReminderRow(val reminderId: String, val remindDate: String, val createdOn: String, val accepted: Boolean, val cancelled: Boolean, val branch: String = "")
    data class StaffReminderRow(val reminderId: String, val toName: String, val toCode: String, val reminderType: String, val remindOn: String, val status: String, val branch: String = "")
    data class FeeReturnSummary(val total: Double, val patientCount: Int)
    data class FeeReturnRow(val paymentId: String, val patientRowId: String, val name: String, val mobile: String, val amount: Double, val returnedOn: String, val branch: String = "")
    // V1421
    data class UnclosedRow(val chamberDate: String, val arrived: Int, val money: Double, val branch: String = "")
    data class NoShowSummary(val noShow: Int, val arrived: Int, val expectedTotal: Int)
    data class NoShowRow(val name: String, val mobile: String, val expectedOn: String, val arrived: Boolean, val branch: String = "")
    data class OutMissingSummary(val total: Int, val staffCount: Int)
    data class OutMissingRow(val staffCode: String, val workDate: String, val checkIn: String, val branch: String = "")
    data class WfhSummary(val total: Int, val approved: Int, val pending: Int, val rejected: Int)
    data class WfhRow(val staffName: String, val staffCode: String, val workDate: String, val status: String, val requestedOn: String, val branch: String = "")
    data class DuplicateSummary(val mobileGroups: Int, val nameGroups: Int, val paymentGroups: Int)
    data class DuplicateRow(val mobile: String, val rowCount: Int, val names: String, val branch: String = "")
    // 🎤🔒 V1504 — "ডুপ্লিকেট পেমেন্ট" (RIMPA ROY/KHAGEN BHAGAT-এর মতো) — ডুপ্লিকেট রোগী থেকে আলাদা।
    data class DuplicatePaymentSummary(val extraCount: Int, val patientCount: Int)
    data class DuplicatePaymentRow(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val payDate: String, val payLabel: String, val amount: Double, val mode: String, val extraCount: Int, val extraAmount: Double, val branch: String = "")
    data class FeeUnpaidRow(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val registrationDate: String, val branch: String = "")
    data class CallPendingRow(val followupId: String, val name: String, val mobile: String, val stage: String, val nextFollow: String, val branch: String = "")
    data class MessagesSummary(val total: Int, val whatsapp: Int, val sms: Int)
    data class MessageRow(val name: String, val mobile: String, val kind: String, val channel: String, val sentOn: String, val branch: String = "")
    // V1422
    data class NewPatientRow(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val registrationDate: String, val branch: String = "")
    data class FuCallsDoneSummary(val total: Int, val patientCount: Int)
    data class FuCallDoneRow(val followupId: String, val name: String, val mobile: String, val callDay: String, val remarks: Int, val branch: String = "")
    data class DiseaseSummary(val total: Int, val allPatients: Int)
    data class DiseaseRow(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val disease: String, val registrationDate: String, val branch: String = "")
    data class RmpCallRow(val rmpId: String, val name: String, val mobile: String, val lastCallDate: String, val nextCallDate: String, val branch: String = "")
    data class FieldVisitSummary(val visits: Int, val km: Double, val staffCount: Int)
    data class FieldVisitRow(val staffCode: String, val workDate: String, val visits: Int, val km: Double, val branch: String = "")
    data class StaffHoursSummary(val totalHours: Double, val staffCount: Int)
    data class StaffHoursRow(val staffCode: String, val hours: Double, val days: Int, val leaveDays: Int, val outMissingDays: Int, val branch: String = "")
    // 🎤 V1428 — RMP-কে দেওয়া কমিশন · IN-বাদ · ব্রাঞ্চ-তুলনা
    data class RmpPaidSummary(val total: Double, val rmpCount: Int, val paymentCount: Int)
    data class RmpPaidRow(val paymentId: String, val rmpId: String, val rmpName: String, val paidOn: String, val amount: Double, val kind: String, val patientName: String, val mode: String, val branch: String = "")
    data class InMissingSummary(val total: Int, val staffCount: Int)
    data class InMissingRow(val staffCode: String, val staffName: String, val workDate: String, val checkOut: String, val branch: String = "")
    data class BranchRank(val branch: String, val value: Double, val patients: Int)
    data class StaffPresentSummary(val total: Int, val staffCount: Int)
    data class StaffPresentRow(val staffCode: String, val workDate: String, val checkIn: String, val checkOut: String, val branch: String = "")
    // 🎤🔒 V1578 (১৭.০৯.২০২৬, আইটেম ৩৩ — অসময়ের এনকোয়ারির ইনসেন্টিভ)
    data class IncentiveSummary(val total: Double, val entryCount: Int, val staffCount: Int)
    data class IncentiveRow(val personCode: String, val patientRowId: String, val patientCode: String, val amount: Double, val reason: String, val branch: String = "")
    // 🎤🔒 V1533 (১৮.০৯.২০২৬, TK-নির্দেশ) — "গত সপ্তাহে কতজন পেশেন্ট RMP পাঠিয়েছে"
    data class RmpReferredRow(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val refDoctor: String, val registrationDate: String, val branch: String = "")
    // 🎤🔒 V1534 (১৮.০৯.২০২৬, TK-নির্দেশ) — "শুধু ডিসকাউন্ট কত" · "কতজন রোগীর ছবি"
    data class DiscountSummary(val total: Double, val patientCount: Int)
    data class DiscountRow(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val amount: Double, val registrationDate: String, val branch: String = "")
    data class PhotoRow(val patientRowId: String, val patientCode: String, val name: String, val mobile: String, val registrationDate: String, val branch: String = "")

    private fun args(branch: String, from: String, to: String): JSONObject = JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to)
    private fun args(branch: String): JSONObject = JSONObject().put("p_branch", branch)

    /** সার্ভারের ফাংশন যখন একটাই সারি দেয় (summary) — ফাঁকা এলে "Not allowed"। */
    private fun firstRow(fn: String, a: JSONObject): RepoResult<JSONObject> {
        val rpc = reportsRpc(fn, a)
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) RepoResult(false, message = "Not allowed for this branch") else RepoResult(true, arr.getJSONObject(0))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    private fun rowList(fn: String, a: JSONObject): RepoResult<List<JSONObject>> {
        val rpc = reportsRpc(fn, a)
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<JSONObject>(arr.length())
            for (i in 0 until arr.length()) out.add(arr.getJSONObject(i))
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    private fun scalarInt(fn: String, a: JSONObject): RepoResult<Int> {
        val rpc = reportsRpc(fn, a)
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val body = rpc.body.trim()
        if (body == "null") return RepoResult(false, message = "Not allowed for this branch")
        val n = body.toIntOrNull() ?: return RepoResult(false, message = "Invalid response")
        return RepoResult(true, n)
    }

    private fun <T> RepoResult<List<JSONObject>>.mapRows(f: (JSONObject) -> T): RepoResult<List<T>> =
        if (!ok) RepoResult(false, message = message) else RepoResult(true, (value ?: emptyList()).map(f))

    private fun <T> RepoResult<JSONObject>.mapRow(f: (JSONObject) -> T): RepoResult<T> =
        if (!ok || value == null) RepoResult(false, message = message) else RepoResult(true, f(value))

    fun appointmentCount(b: String, f: String, t: String): RepoResult<Int> = scalarInt("appointment_count", args(b, f, t))
    fun appointmentList(b: String, f: String, t: String): RepoResult<List<AppointmentRow>> = rowList("appointment_list", args(b, f, t)).mapRows {
        AppointmentRow(it.optString("enquiry_row_id"), it.optString("name"), it.optString("mobile"), it.optString("disease"), it.optString("appointment_date"), it.optBoolean("registered", false), it.optString("branch")) }

    fun expectedCount(b: String, f: String, t: String): RepoResult<Int> = scalarInt("expected_count", args(b, f, t))
    fun expectedList(b: String, f: String, t: String): RepoResult<List<ExpectedRow>> = rowList("expected_list", args(b, f, t)).mapRows {
        ExpectedRow(it.optString("mark_id"), it.optString("patient_row_id"), it.optString("name"), it.optString("mobile"), it.optString("expected_on"), it.optString("branch")) }

    fun handoverPendingSummary(b: String): RepoResult<HandoverPendingSummary> = firstRow("handover_pending_summary", args(b)).mapRow {
        HandoverPendingSummary(it.optDouble("total", 0.0), it.optInt("day_count", 0)) }
    fun handoverPendingList(b: String): RepoResult<List<HandoverPendingRow>> = rowList("handover_pending_list", args(b)).mapRows {
        HandoverPendingRow(it.optString("handover_date"), it.optDouble("cash", 0.0), it.optString("status"), it.optString("branch")) }

    fun paymentRequestsSummary(b: String): RepoResult<PaymentRequestsSummary> = firstRow("payment_requests_summary", args(b)).mapRow {
        PaymentRequestsSummary(it.optInt("backdate_count", 0), it.optInt("edit_count", 0), it.optInt("refund_count", 0), it.optInt("total", 0)) }
    fun paymentRequestsList(b: String): RepoResult<List<PaymentRequestRow>> = rowList("payment_requests_list", args(b)).mapRows {
        PaymentRequestRow(it.optString("request_id"), it.optString("request_type"), it.optString("name"), it.optString("mobile"), it.optDouble("amount", 0.0), it.optString("requested_on"), it.optString("branch")) }

    fun referralRequestsSummary(b: String): RepoResult<ReferralRequestsSummary> = firstRow("referral_requests_summary", args(b)).mapRow {
        ReferralRequestsSummary(it.optInt("total", 0), it.optInt("delete_count", 0)) }
    fun referralRequestsList(b: String): RepoResult<List<ReferralRequestRow>> = rowList("referral_requests_list", args(b)).mapRows {
        ReferralRequestRow(it.optString("request_id"), it.optString("request_type"), it.optDouble("new_amount", 0.0), it.optString("requested_on"), it.optString("branch")) }

    fun leaveSummary(b: String, f: String, t: String): RepoResult<LeaveSummary> = firstRow("leave_summary", args(b, f, t)).mapRow {
        LeaveSummary(it.optInt("total", 0), it.optInt("confirmed", 0), it.optInt("pending", 0), it.optInt("rejected", 0)) }
    fun leaveList(b: String, f: String, t: String): RepoResult<List<LeaveRow>> = rowList("leave_list", args(b, f, t)).mapRows {
        LeaveRow(it.optString("staff_code"), it.optString("leave_date"), it.optString("status"), it.optString("applied_on"), it.optString("branch")) }

    fun doctorReminderSummary(b: String, f: String, t: String): RepoResult<DoctorReminderSummary> = firstRow("doctor_reminder_summary", args(b, f, t)).mapRow {
        DoctorReminderSummary(it.optInt("total", 0), it.optInt("not_accepted", 0)) }
    fun doctorReminderList(b: String, f: String, t: String): RepoResult<List<DoctorReminderRow>> = rowList("doctor_reminder_list", args(b, f, t)).mapRows {
        DoctorReminderRow(it.optString("reminder_id"), it.optString("remind_date"), it.optString("created_on"), it.optBoolean("accepted", false), it.optBoolean("cancelled", false), it.optString("branch")) }

    fun staffReminderOpenSummary(b: String): RepoResult<Int> = firstRow("staff_reminder_open_summary", args(b)).mapRow { it.optInt("total", 0) }
    fun staffReminderOpenList(b: String): RepoResult<List<StaffReminderRow>> = rowList("staff_reminder_open_list", args(b)).mapRows {
        StaffReminderRow(it.optString("reminder_id"), it.optString("to_name"), it.optString("to_code"), it.optString("reminder_type"), it.optString("remind_on"), it.optString("status"), it.optString("branch")) }

    fun feeReturnSummary(b: String, f: String, t: String): RepoResult<FeeReturnSummary> = firstRow("fee_return_summary", args(b, f, t)).mapRow {
        FeeReturnSummary(it.optDouble("total", 0.0), it.optInt("patient_count", 0)) }
    fun feeReturnList(b: String, f: String, t: String): RepoResult<List<FeeReturnRow>> = rowList("fee_return_list", args(b, f, t)).mapRows {
        FeeReturnRow(it.optString("payment_id"), it.optString("patient_row_id"), it.optString("name"), it.optString("mobile"), it.optDouble("amount", 0.0), it.optString("returned_on"), it.optString("branch")) }

    // ── V1421 ──
    fun chamberUnclosedSummary(b: String, f: String, t: String): RepoResult<Int> = firstRow("chamber_unclosed_summary", args(b, f, t)).mapRow { it.optInt("day_count", 0) }
    fun chamberUnclosedList(b: String, f: String, t: String): RepoResult<List<UnclosedRow>> = rowList("chamber_unclosed_list", args(b, f, t)).mapRows {
        UnclosedRow(it.optString("chamber_date"), it.optInt("arrived", 0), it.optDouble("money", 0.0), it.optString("branch")) }

    fun noShowSummary(b: String, f: String, t: String): RepoResult<NoShowSummary> = firstRow("no_show_summary", args(b, f, t)).mapRow {
        NoShowSummary(it.optInt("no_show", 0), it.optInt("arrived", 0), it.optInt("expected_total", 0)) }
    fun noShowList(b: String, f: String, t: String): RepoResult<List<NoShowRow>> = rowList("no_show_list", args(b, f, t)).mapRows {
        NoShowRow(it.optString("name"), it.optString("mobile"), it.optString("expected_on"), it.optBoolean("arrived", false), it.optString("branch")) }

    fun outMissingSummary(b: String, f: String, t: String): RepoResult<OutMissingSummary> = firstRow("out_missing_summary", args(b, f, t)).mapRow {
        OutMissingSummary(it.optInt("total", 0), it.optInt("staff_count", 0)) }
    fun outMissingList(b: String, f: String, t: String): RepoResult<List<OutMissingRow>> = rowList("out_missing_list", args(b, f, t)).mapRows {
        OutMissingRow(it.optString("staff_code"), it.optString("work_date"), it.optString("check_in"), it.optString("branch")) }

    fun wfhSummary(b: String, f: String, t: String): RepoResult<WfhSummary> = firstRow("wfh_summary", args(b, f, t)).mapRow {
        WfhSummary(it.optInt("total", 0), it.optInt("approved", 0), it.optInt("pending", 0), it.optInt("rejected", 0)) }
    fun wfhList(b: String, f: String, t: String): RepoResult<List<WfhRow>> = rowList("wfh_list", args(b, f, t)).mapRows {
        WfhRow(it.optString("staff_name"), it.optString("staff_code"), it.optString("work_date"), it.optString("status"), it.optString("requested_on"), it.optString("branch")) }

    fun duplicateSummary(b: String): RepoResult<DuplicateSummary> = firstRow("duplicate_summary", args(b)).mapRow {
        DuplicateSummary(it.optInt("mobile_groups", 0), it.optInt("name_groups", 0), it.optInt("payment_groups", 0)) }
    fun duplicateList(b: String): RepoResult<List<DuplicateRow>> = rowList("duplicate_list", args(b)).mapRows {
        DuplicateRow(it.optString("mobile"), it.optInt("row_count", 0), it.optString("names"), it.optString("branch")) }

    // 🎤🔒 V1504 — একই রোগী+দিন+লেবেল+অঙ্ক+ধরন, ১৫ মিনিটের মধ্যে তৈরি হলে তবেই "ডুপ্লিকেট"।
    fun duplicatePaymentsSummary(b: String): RepoResult<DuplicatePaymentSummary> = firstRow("duplicate_payments_summary", args(b)).mapRow {
        DuplicatePaymentSummary(it.optInt("total", 0), it.optInt("patient_count", 0)) }
    fun duplicatePaymentsList(b: String): RepoResult<List<DuplicatePaymentRow>> = rowList("duplicate_payments_list", args(b)).mapRows {
        DuplicatePaymentRow(it.optString("patient_row_id"), it.optString("patient_code"), it.optString("name"), it.optString("mobile"),
            it.optString("pay_date"), it.optString("pay_label"), it.optDouble("amount", 0.0), it.optString("mode"),
            it.optInt("extra_count", 0), it.optDouble("extra_amount", 0.0), it.optString("branch")) }

    fun feeUnpaidSummary(b: String): RepoResult<Int> = firstRow("fee_unpaid_summary", args(b)).mapRow { it.optInt("total", 0) }
    fun feeUnpaidList(b: String): RepoResult<List<FeeUnpaidRow>> = rowList("fee_unpaid_list", args(b)).mapRows {
        FeeUnpaidRow(it.optString("patient_row_id"), it.optString("patient_code"), it.optString("name"), it.optString("mobile"), it.optString("registration_date"), it.optString("branch")) }

    fun callsPendingSummary(b: String): RepoResult<Int> = firstRow("calls_pending_summary", args(b)).mapRow { it.optInt("total", 0) }
    fun callsPendingList(b: String): RepoResult<List<CallPendingRow>> = rowList("calls_pending_list", args(b)).mapRows {
        CallPendingRow(it.optString("followup_id"), it.optString("name"), it.optString("mobile"), it.optString("stage"), it.optString("next_follow"), it.optString("branch")) }

    fun messagesSummary(b: String, f: String, t: String): RepoResult<MessagesSummary> = firstRow("messages_summary", args(b, f, t)).mapRow {
        MessagesSummary(it.optInt("total", 0), it.optInt("whatsapp", 0), it.optInt("sms", 0)) }
    fun messagesList(b: String, f: String, t: String): RepoResult<List<MessageRow>> = rowList("messages_list", args(b, f, t)).mapRows {
        MessageRow(it.optString("name"), it.optString("mobile"), it.optString("kind"), it.optString("channel"), it.optString("sent_on"), it.optString("branch")) }

    // ── V1422 ──
    fun newPatientsCount(b: String, f: String, t: String): RepoResult<Int> = firstRow("new_patients_count", args(b, f, t)).mapRow { it.optInt("total", 0) }
    fun newPatientsList(b: String, f: String, t: String): RepoResult<List<NewPatientRow>> = rowList("new_patients_list", args(b, f, t)).mapRows {
        NewPatientRow(it.optString("patient_row_id"), it.optString("patient_code"), it.optString("name"), it.optString("mobile"), it.optString("registration_date"), it.optString("branch")) }

    fun fuCallsDoneSummary(b: String, f: String, t: String): RepoResult<FuCallsDoneSummary> = firstRow("followup_calls_done_summary", args(b, f, t)).mapRow {
        FuCallsDoneSummary(it.optInt("total", 0), it.optInt("patient_count", 0)) }
    fun fuCallsDoneList(b: String, f: String, t: String): RepoResult<List<FuCallDoneRow>> = rowList("followup_calls_done_list", args(b, f, t)).mapRows {
        FuCallDoneRow(it.optString("followup_id"), it.optString("name"), it.optString("mobile"), it.optString("call_day"), it.optInt("remarks", 0), it.optString("branch")) }

    fun diseaseCount(b: String, f: String, t: String, disease: String): RepoResult<DiseaseSummary> = firstRow("disease_count", args(b, f, t).put("p_disease", disease)).mapRow {
        DiseaseSummary(it.optInt("total", 0), it.optInt("all_patients", 0)) }
    fun diseaseList(b: String, f: String, t: String, disease: String): RepoResult<List<DiseaseRow>> = rowList("disease_list", args(b, f, t).put("p_disease", disease)).mapRows {
        DiseaseRow(it.optString("patient_row_id"), it.optString("patient_code"), it.optString("name"), it.optString("mobile"), it.optString("disease"), it.optString("registration_date"), it.optString("branch")) }

    fun rmpCalledCount(b: String, f: String, t: String): RepoResult<Int> = firstRow("rmp_called_count", args(b, f, t)).mapRow { it.optInt("total", 0) }
    fun rmpCalledList(b: String, f: String, t: String): RepoResult<List<RmpCallRow>> = rowList("rmp_called_list", args(b, f, t)).mapRows {
        RmpCallRow(it.optString("rmp_id"), it.optString("name"), it.optString("mobile"), it.optString("last_call_date"), it.optString("next_call_date"), it.optString("branch")) }
    fun rmpCallDueCount(b: String, f: String, t: String): RepoResult<Int> = firstRow("rmp_call_due_count", args(b, f, t)).mapRow { it.optInt("total", 0) }
    fun rmpCallDueList(b: String, f: String, t: String): RepoResult<List<RmpCallRow>> = rowList("rmp_call_due_list", args(b, f, t)).mapRows {
        RmpCallRow(it.optString("rmp_id"), it.optString("name"), it.optString("mobile"), it.optString("last_call_date"), it.optString("next_call_date"), it.optString("branch")) }

    fun fieldVisitSummary(b: String, f: String, t: String): RepoResult<FieldVisitSummary> = firstRow("field_visit_summary", args(b, f, t)).mapRow {
        FieldVisitSummary(it.optInt("visits", 0), it.optDouble("km", 0.0), it.optInt("staff_count", 0)) }
    fun fieldVisitList(b: String, f: String, t: String): RepoResult<List<FieldVisitRow>> = rowList("field_visit_list", args(b, f, t)).mapRows {
        FieldVisitRow(it.optString("staff_code"), it.optString("work_date"), it.optInt("visits", 0), it.optDouble("km", 0.0), it.optString("branch")) }

    fun staffHoursSummary(b: String, f: String, t: String): RepoResult<StaffHoursSummary> = firstRow("staff_hours_summary", args(b, f, t)).mapRow {
        StaffHoursSummary(it.optDouble("total_hours", 0.0), it.optInt("staff_count", 0)) }
    fun staffHoursList(b: String, f: String, t: String): RepoResult<List<StaffHoursRow>> = rowList("staff_hours_list", args(b, f, t)).mapRows {
        StaffHoursRow(it.optString("staff_code"), it.optDouble("hours", 0.0), it.optInt("days", 0), it.optInt("leave_days", 0), it.optInt("out_missing_days", 0), it.optString("branch")) }

    fun staffPresentSummary(b: String, f: String, t: String): RepoResult<StaffPresentSummary> = firstRow("staff_present_summary", args(b, f, t)).mapRow {
        StaffPresentSummary(it.optInt("total", 0), it.optInt("staff_count", 0)) }
    fun staffPresentList(b: String, f: String, t: String): RepoResult<List<StaffPresentRow>> = rowList("staff_present_list", args(b, f, t)).mapRows {
        StaffPresentRow(it.optString("staff_code"), it.optString("work_date"), it.optString("check_in"), it.optString("check_out"), it.optString("branch")) }

    fun rmpPaidSummary(b: String, f: String, t: String): RepoResult<RmpPaidSummary> = firstRow("rmp_paid_summary", args(b, f, t)).mapRow {
        RmpPaidSummary(it.optDouble("total", 0.0), it.optInt("rmp_count", 0), it.optInt("payment_count", 0)) }
    fun rmpPaidList(b: String, f: String, t: String): RepoResult<List<RmpPaidRow>> = rowList("rmp_paid_list", args(b, f, t)).mapRows {
        RmpPaidRow(it.optString("payment_id"), it.optString("rmp_id"), it.optString("rmp_name"), it.optString("paid_on"), it.optDouble("amount", 0.0), it.optString("kind"), it.optString("patient_name"), it.optString("mode"), it.optString("branch")) }
    fun inMissingSummary(b: String, f: String, t: String): RepoResult<InMissingSummary> = firstRow("in_missing_summary", args(b, f, t)).mapRow {
        InMissingSummary(it.optInt("total", 0), it.optInt("staff_count", 0)) }
    fun inMissingList(b: String, f: String, t: String): RepoResult<List<InMissingRow>> = rowList("in_missing_list", args(b, f, t)).mapRows {
        InMissingRow(it.optString("staff_code"), it.optString("staff_name"), it.optString("work_date"), it.optString("check_out"), it.optString("branch")) }

    // 🎤🔒 V1578 (আইটেম ৩৩) — TK-র দুই সিদ্ধান্ত: মাস = রেজিস্ট্রেশনের দিন
    // (reports.incentive_* এতেই বাঁধা, নতুন কিছু বসাতে হয়নি), ব্রাঞ্চ = রোগীর
    // ব্রাঞ্চ (reports.can_access_branch-এর একই গার্ড, বাকি সব প্রশ্নের মতো)।
    fun incentiveSummary(b: String, f: String, t: String): RepoResult<IncentiveSummary> = firstRow("incentive_summary", args(b, f, t)).mapRow {
        IncentiveSummary(it.optDouble("total", 0.0), it.optInt("entry_count", 0), it.optInt("staff_count", 0)) }
    fun incentiveList(b: String, f: String, t: String): RepoResult<List<IncentiveRow>> = rowList("incentive_list", args(b, f, t)).mapRows {
        IncentiveRow(it.optString("person_code"), it.optString("patient_row_id"), it.optString("patient_code"), it.optDouble("amount", 0.0), it.optString("reason"), it.optString("branch")) }

    // 🎤🔒 V1533 (১৮.০৯.২০২৬, TK-নির্দেশ) — "গত সপ্তাহে/গত মাসে কতজন পেশেন্ট RMP পাঠিয়েছে"
    fun rmpReferredCount(b: String, f: String, t: String): RepoResult<Int> = firstRow("rmp_referred_count", args(b, f, t)).mapRow { it.optInt("total", 0) }
    fun rmpReferredList(b: String, f: String, t: String): RepoResult<List<RmpReferredRow>> = rowList("rmp_referred_list", args(b, f, t)).mapRows {
        RmpReferredRow(it.optString("patient_row_id"), it.optString("patient_code"), it.optString("name"), it.optString("mobile"), it.optString("ref_doctor"), it.optString("reg_date"), it.optString("branch")) }

    // 🎤🔒 V1534 (১৮.০৯.২০২৬, TK-নির্দেশ) — "শুধু ডিসকাউন্ট কত হয়েছে"
    fun discountSummary(b: String, f: String, t: String): RepoResult<DiscountSummary> = firstRow("discount_summary", args(b, f, t)).mapRow {
        DiscountSummary(it.optDouble("total", 0.0), it.optInt("patient_count", 0)) }
    fun discountList(b: String, f: String, t: String): RepoResult<List<DiscountRow>> = rowList("discount_list", args(b, f, t)).mapRows {
        DiscountRow(it.optString("patient_row_id"), it.optString("patient_code"), it.optString("name"), it.optString("mobile"), it.optDouble("amount", 0.0), it.optString("reg_date"), it.optString("branch")) }

    // 🎤🔒 V1534 — "কতজন রোগীর ছবি তোলা হয়েছে"
    fun photoCount(b: String, f: String, t: String): RepoResult<Int> = firstRow("patient_photo_count", args(b, f, t)).mapRow { it.optInt("total", 0) }
    fun photoList(b: String, f: String, t: String): RepoResult<List<PhotoRow>> = rowList("patient_photo_list", args(b, f, t)).mapRows {
        PhotoRow(it.optString("patient_row_id"), it.optString("patient_code"), it.optString("name"), it.optString("mobile"), it.optString("reg_date"), it.optString("branch")) }

    /* 🎤 V1428 (আইটেম ২০) — "কোন ব্রাঞ্চে সবচেয়ে বেশি/কম": পাঁচ ব্রাঞ্চের **একই** ফাংশন পাঁচবার
       (নতুন SQL নেই), ফল সাজিয়ে ফেরত — বেশি→কম, "min" হলে কম→বেশি। একটা ব্রাঞ্চে ভুল হলে পুরোটা ভুল। */
    fun branchRankCollection(f: String, t: String, lowestFirst: Boolean): RepoResult<List<BranchRank>> {
        val out = ArrayList<BranchRank>()
        for (b in ALL_BRANCHES) {
            val r = collectionSummary(b, f, t)
            val v = r.value ?: return RepoResult(false, message = r.message)
            out.add(BranchRank(b, v.total, v.patientCount))
        }
        return RepoResult(true, if (lowestFirst) out.sortedBy { it.value } else out.sortedByDescending { it.value })
    }
    fun branchRankPatients(f: String, t: String, lowestFirst: Boolean): RepoResult<List<BranchRank>> {
        val out = ArrayList<BranchRank>()
        for (b in ALL_BRANCHES) {
            val r = patientsRegisteredCount(b, f, t)
            val v = r.value ?: return RepoResult(false, message = r.message)
            out.add(BranchRank(b, v.toDouble(), v))
        }
        return RepoResult(true, if (lowestFirst) out.sortedBy { it.value } else out.sortedByDescending { it.value })
    }

    fun patientsRegisteredCount(branch: String, from: String, to: String): RepoResult<Int> {
        val rpc = reportsRpc("patients_registered_count", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val body = rpc.body.trim()
        if (body == "null") return RepoResult(false, message = "Not allowed for this branch")
        val n = body.toIntOrNull() ?: return RepoResult(false, message = "Invalid response")
        return RepoResult(true, n)
    }

    fun patientsRegisteredList(branch: String, from: String, to: String): RepoResult<List<RegisteredPatient>> {
        val rpc = reportsRpc("patients_registered_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<RegisteredPatient>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(RegisteredPatient(x.optString("patient_row_id"), x.optString("patient_code"), x.optString("name"), x.optString("mobile"), x.optString("registration_date"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    /** 🎤 V1503 (১৫.০৯.২০২৬, TK-নির্দেশ) — "কতজন পেশেন্ট এসেছিল" এখন সব রোগী
     *  ধরে (নতুন রেজিস্ট্রেশন + পুরনো রোগীর ভিজিট-প্রমাণ) — patientsRegisteredCount
     *  এর বদলে (ওটা এখনো অন্য জায়গায় "শুধু রেজিস্ট্রেশন" অর্থে ব্যবহৃত, অক্ষত)। */
    fun patientsVisitedCount(branch: String, from: String, to: String): RepoResult<Int> {
        val rpc = reportsRpc("patients_visited_count", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val body = rpc.body.trim()
        if (body == "null") return RepoResult(false, message = "Not allowed for this branch")
        val n = body.toIntOrNull() ?: return RepoResult(false, message = "Invalid response")
        return RepoResult(true, n)
    }

    fun patientsVisitedList(branch: String, from: String, to: String): RepoResult<List<VisitedPatient>> {
        val rpc = reportsRpc("patients_visited_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<VisitedPatient>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(VisitedPatient(x.optString("patient_row_id"), x.optString("patient_code"), x.optString("name"), x.optString("mobile"), x.optString("visit_date")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun collectionSummary(branch: String, from: String, to: String): RepoResult<CollectionSummary> {
        val rpc = reportsRpc("collection_summary", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            val x = arr.getJSONObject(0)
            RepoResult(true, CollectionSummary(x.optDouble("total", 0.0), x.optInt("patient_count", 0), x.optInt("payment_count", 0)))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun collectionList(branch: String, from: String, to: String): RepoResult<List<CollectionRow>> {
        val rpc = reportsRpc("collection_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<CollectionRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(CollectionRow(x.optString("payment_id"), x.optString("patient_row_id"), x.optString("name"), x.optString("mobile"), x.optDouble("amount", 0.0), x.optString("mode"), x.optString("pay_type"), x.optString("paid_on"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    /* 🎤 V1416 (১৩.০৯.২০২৬, TK-নির্দেশ "শুরু করে দিন", তালিকা ৫২২) — মেডিসিন/
     * স্যালাইন বিক্রি (VOICE_QUERY_PLAN আইটেম ৪ ও ২১) — একই `products` টেবিল,
     * শুধু kind আলাদা, তাই একটাই ফাংশন-জোড়া দুই kind দিয়েই ডাকা হয়। */
    fun productSaleSummary(branch: String, from: String, to: String, kind: String): RepoResult<ProductSaleSummary> {
        val rpc = reportsRpc("product_sale_summary", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to).put("p_kind", kind))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            val x = arr.getJSONObject(0)
            RepoResult(true, ProductSaleSummary(x.optDouble("total", 0.0), x.optInt("sale_count", 0)))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun productSaleList(branch: String, from: String, to: String, kind: String): RepoResult<List<ProductSaleRow>> {
        val rpc = reportsRpc("product_sale_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to).put("p_kind", kind))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<ProductSaleRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(ProductSaleRow(x.optString("product_row_id"), x.optString("customer"), x.optString("mobile"), x.optString("product"), x.optDouble("bill", 0.0), x.optDouble("deposit", 0.0), x.optDouble("due", 0.0), x.optString("mode"), x.optString("sold_on"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    /* 🎤 V1417 (১৩.০৯.২০২৬, TK-নির্দেশ "চালিয়ে যান", তালিকা ৫২৩) — এনকোয়ারি-
     * সংখ্যা ও Approved রিফান্ডের টাকা (VOICE_QUERY_PLAN আইটেম ১১ ও ১৪-র রিফান্ড
     * অংশ — ডিসকাউন্ট আলাদা জায়গা থেকে আসে বলে এখানে বসানো হয়নি)। */
    fun enquiryCount(branch: String, from: String, to: String): RepoResult<Int> {
        val rpc = reportsRpc("enquiry_count", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val body = rpc.body.trim()
        if (body == "null") return RepoResult(false, message = "Not allowed for this branch")
        val n = body.toIntOrNull() ?: return RepoResult(false, message = "Invalid response")
        return RepoResult(true, n)
    }

    fun enquiryList(branch: String, from: String, to: String): RepoResult<List<EnquiryRow>> {
        val rpc = reportsRpc("enquiry_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<EnquiryRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(EnquiryRow(x.optString("enquiry_row_id"), x.optString("name"), x.optString("mobile"), x.optString("disease"), x.optString("enquiry_date"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun refundSummary(branch: String, from: String, to: String): RepoResult<RefundSummary> {
        val rpc = reportsRpc("refund_summary", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            val x = arr.getJSONObject(0)
            RepoResult(true, RefundSummary(x.optDouble("total", 0.0), x.optInt("refund_count", 0)))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun refundList(branch: String, from: String, to: String): RepoResult<List<RefundRow>> {
        val rpc = reportsRpc("refund_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<RefundRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(RefundRow(x.optString("payment_id"), x.optString("patient_row_id"), x.optString("name"), x.optString("mobile"), x.optDouble("amount", 0.0), x.optString("mode"), x.optString("refunded_on"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    /* 🎤 V1418 (১৩.০৯.২০২৬, TK-নির্দেশ "চালিয়ে যান", তালিকা ৫২৪) — ক্যাশ
     * হ্যান্ডওভার (VOICE_QUERY_PLAN আইটেম ১৫) ও RMP-দের ব্রাঞ্চ-বাকি (আইটেম ২৫)।
     * ⛔ RMP-বাকি এখানেও কোনো নতুন হিসাব করে না — শুধু আজই বানানো
     * `fin.rmp_branch_due` ডাকা হয় (CLAUDE.md ৭গ-র "একটাই সার্ভার-নিয়ম")। */
    fun cashHandoverSummary(branch: String, from: String, to: String): RepoResult<HandoverSummary> {
        val rpc = reportsRpc("cash_handover_summary", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            val x = arr.getJSONObject(0)
            RepoResult(true, HandoverSummary(x.optDouble("total", 0.0), x.optInt("day_count", 0)))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun cashHandoverList(branch: String, from: String, to: String): RepoResult<List<HandoverRow>> {
        val rpc = reportsRpc("cash_handover_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<HandoverRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(HandoverRow(x.optString("handover_date"), x.optDouble("cash", 0.0), x.optString("receiver_name"), x.optString("received_at"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun rmpDueSummary(branch: String): RepoResult<RmpDueSummary> {
        val rpc = reportsRpc("rmp_due_summary", JSONObject().put("p_branch", branch))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            val x = arr.getJSONObject(0)
            RepoResult(true, RmpDueSummary(x.optDouble("total_due", 0.0), x.optInt("rmp_count", 0)))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun rmpDueList(branch: String): RepoResult<List<RmpDueRow>> {
        val rpc = reportsRpc("rmp_due_list", JSONObject().put("p_branch", branch))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<RmpDueRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(RmpDueRow(x.optString("rmp_id"), x.optString("rmp_name"), x.optString("rmp_mobile"), x.optDouble("due", 0.0), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    /* 🎤 V1419 (১৩.০৯.২০২৬, TK-নির্দেশ "একসাথে যতগুলো সম্ভব") — মেডিসিন/স্যালাইনের
     * বর্তমান বাকি (settlement-সারি বাদ দিয়ে সার্ভারেই আসল হিসাব), অ্যাপ-কল সংখ্যা,
     * ট্র্যাশে-যাওয়া রেকর্ড, RMP-অগ্রিম — সবই reports.* ছোট্ট ফাংশন, বাল্ক নয়। */
    fun productDueSummary(branch: String): RepoResult<ProductDueSummary> {
        val rpc = reportsRpc("product_due_summary", JSONObject().put("p_branch", branch))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            val x = arr.getJSONObject(0)
            RepoResult(true, ProductDueSummary(x.optDouble("total", 0.0), x.optInt("row_count", 0)))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun productDueList(branch: String): RepoResult<List<ProductDueRow>> {
        val rpc = reportsRpc("product_due_list", JSONObject().put("p_branch", branch))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<ProductDueRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(ProductDueRow(x.optString("product_row_id"), x.optString("customer"), x.optString("mobile"), x.optString("product"), x.optDouble("due", 0.0), x.optString("sold_on"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun callCount(branch: String, from: String, to: String): RepoResult<Int> {
        val rpc = reportsRpc("call_count", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        val body = rpc.body.trim()
        if (body == "null") return RepoResult(false, message = "Not allowed for this branch")
        val n = body.toIntOrNull() ?: return RepoResult(false, message = "Invalid response")
        return RepoResult(true, n)
    }

    fun callList(branch: String, from: String, to: String): RepoResult<List<CallRow>> {
        val rpc = reportsRpc("call_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<CallRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(CallRow(x.optString("call_row_id"), x.optString("staff_code"), x.optString("target_mobile_mask"), x.optString("call_date"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun trashSummary(branch: String, from: String, to: String): RepoResult<Int> {
        val rpc = reportsRpc("trash_summary", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            RepoResult(true, arr.getJSONObject(0).optInt("total", 0))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun trashList(branch: String, from: String, to: String): RepoResult<List<TrashRow>> {
        val rpc = reportsRpc("trash_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<TrashRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(TrashRow(x.optString("trash_row_id"), x.optString("table_name"), x.optString("deleted_at"), x.optString("deleted_by"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun rmpAdvanceSummary(branch: String, from: String, to: String): RepoResult<RmpAdvanceSummary> {
        val rpc = reportsRpc("rmp_advance_summary", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) return RepoResult(false, message = "Not allowed for this branch")
            val x = arr.getJSONObject(0)
            RepoResult(true, RmpAdvanceSummary(x.optDouble("total", 0.0), x.optInt("advance_count", 0)))
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }

    fun rmpAdvanceList(branch: String, from: String, to: String): RepoResult<List<RmpAdvanceRow>> {
        val rpc = reportsRpc("rmp_advance_list", JSONObject().put("p_branch", branch).put("p_from", from).put("p_to", to))
        if (!rpc.ok) return RepoResult(false, message = rpc.message)
        return try {
            val arr = JSONArray(rpc.body)
            val out = ArrayList<RmpAdvanceRow>(arr.length())
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                out.add(RmpAdvanceRow(x.optString("advance_id"), x.optString("rmp_name"), x.optDouble("amount", 0.0), x.optString("mode"), x.optString("paid_on"), x.optString("branch")))
            }
            RepoResult(true, out)
        } catch (_: Exception) { RepoResult(false, message = "Invalid response") }
    }
}
