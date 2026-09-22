package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.databinding.ActivityVoiceReportDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/* 🎤 V1415 (১৩.০৯.২০২৬, TK-নির্দেশ) — Search-এর ভয়েস/টাইপ-প্রশ্নের উত্তরে
 * চাপ দিলে এই পর্দা খোলে — আসল রোগী/পেমেন্টের তালিকা, রোগীর নামে চাপলে
 * চেনা Patient Timeline-এই যায় (TK: "চাপ দিলে যেন সেই পেজে রিডাইরেক্ট হয়")।
 * ⛔ এখানে কোনো টাকা/হিসাব নতুন করে গোনা হয় না — সার্ভারের ফাংশনই যা দেয় তাই। */
class VoiceReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceReportDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val metric = intent.getStringExtra("metric").orEmpty()
        val branch = intent.getStringExtra("branch").orEmpty()
        val from = intent.getStringExtra("from").orEmpty()
        val to = intent.getStringExtra("to").orEmpty()
        val title = intent.getStringExtra("title").orEmpty()
        val extra = intent.getStringExtra("extra").orEmpty()   // V1422 — যেমন রোগের নাম
        binding.toolbar.title = title.ifBlank { "Report" }
        binding.progressLoad.visibility = android.view.View.VISIBLE
        binding.tvSummary.text = "Loading…"

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun row(name: String, sub: String, right: String, rightColor: String, onTap: (() -> Unit)?): LinearLayout {
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(4), dp(10), dp(4), dp(10))
                if (onTap != null) { isClickable = true; isFocusable = true; setOnClickListener { onTap() } }
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE); setStroke(dp(1), android.graphics.Color.parseColor("#E2E8F0"))
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.bottomMargin = dp(6); layoutParams = lp
            }
            val left = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dp(8), 0, dp(8), 0)
            }
            left.addView(TextView(this).apply {
                text = name.ifBlank { "—" }; textSize = 13.5f
                setTextColor(android.graphics.Color.parseColor(if (onTap != null) "#1457B8" else "#10223A"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            if (sub.isNotBlank()) left.addView(TextView(this).apply {
                text = sub; textSize = 11f; setTextColor(android.graphics.Color.parseColor("#64748B"))
            })
            r.addView(left)
            r.addView(TextView(this).apply {
                text = right; textSize = 13.5f; setTextColor(android.graphics.Color.parseColor(rightColor))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(8), 0, dp(8), 0)
            })
            return r
        }

        lifecycleScope.launch {
            when (metric) {
                "REGISTRATION_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.patientsRegisteredList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} patients"
                    if (rows.isEmpty()) empty("No patients found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.mobile} · ${FollowUpModel.displayDate(p.registrationDate)}", "›", "#94A3B8", onTap))
                    }
                }
                // 🎤 V1503 (১৫.০৯.২০২৬, TK-নির্দেশ) — "কতজন পেশেন্ট এসেছিল": নতুন রেজিস্ট্রেশন + পুরনো রোগীর ভিজিট-প্রমাণ, দুটো মিলিয়ে।
                "PATIENTS_VISITED" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.patientsVisitedList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} patients"
                    if (rows.isEmpty()) empty("No patients found for this period.")
                    rows.forEach { p ->
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "${p.mobile} · ${FollowUpModel.displayDate(p.visitDate)}", "›", "#94A3B8", onTap))
                    }
                }
                // 🎤🔒 V1533 (১৮.০৯.২০২৬, TK-নির্দেশ) — RMP-রেফার করা রোগীর তালিকা
                "RMP_REFERRED_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpReferredList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} patients"
                    if (rows.isEmpty()) empty("No RMP-referred patients found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.refDoctor} · ${FollowUpModel.displayDate(p.registrationDate)}", "›", "#94A3B8", onTap))
                    }
                }
                // 🎤🔒 V1534 (১৮.০৯.২০২৬, TK-নির্দেশ) — শুধু ডিসকাউন্ট · রোগীর ছবির তালিকা
                "DISCOUNT_SUMMARY" -> {
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.discountSummary(branch, from, to) to VoiceReportRepository.discountList(branch, from, to)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total: ₹${"%,.0f".format(s.total)} · ${s.patientCount} patients" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No discounts found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${FollowUpModel.displayDate(p.registrationDate)}", "₹${"%,.0f".format(p.amount)}", "#B3820C", onTap))
                    }
                }
                "PHOTO_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.photoList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} patients"
                    if (rows.isEmpty()) empty("No patients with photo found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${FollowUpModel.displayDate(p.registrationDate)}", "›", "#94A3B8", onTap))
                    }
                }
                "COLLECTION" -> {
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.collectionSummary(branch, from, to) to VoiceReportRepository.collectionList(branch, from, to)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total: ₹${"%,.0f".format(s.total)} · ${s.patientCount} patients · ${s.paymentCount} payments" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No payments found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.payType} · ${p.mode} · ${FollowUpModel.displayDate(p.paidOn)}", "₹${"%,.0f".format(p.amount)}", if (p.payType.equals("refund", true)) "#B42318" else "#0C8F3A", onTap))
                    }
                }
                "MEDICINE_SALE", "SALINE_SALE" -> {
                    val kind = if (metric == "MEDICINE_SALE") "medicinePayment" else "salinePayment"
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.productSaleSummary(branch, from, to, kind) to VoiceReportRepository.productSaleList(branch, from, to, kind)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total: ₹${"%,.0f".format(s.total)} · ${s.saleCount} sales" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No sales found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.customer.ifBlank { p.mobile }, "$branchTag${p.product} · ${p.mode} · ${FollowUpModel.displayDate(p.soldOn)}", "₹${"%,.0f".format(p.bill)}", "#0C8F3A", onTap))
                    }
                }
                "ENQUIRY_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.enquiryList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} enquiries"
                    if (rows.isEmpty()) empty("No enquiries found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.disease} · ${FollowUpModel.displayDate(p.enquiryDate)}", "›", "#94A3B8", onTap))
                    }
                }
                "REFUND" -> {
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.refundSummary(branch, from, to) to VoiceReportRepository.refundList(branch, from, to)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total: ₹${"%,.0f".format(s.total)} · ${s.refundCount} refunds" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No refunds found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${FollowUpModel.displayDate(p.refundedOn)}", "₹${"%,.0f".format(p.amount)}", "#B42318", onTap))
                    }
                }
                "CASH_HANDOVER" -> {
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.cashHandoverSummary(branch, from, to) to VoiceReportRepository.cashHandoverList(branch, from, to)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total: ₹${"%,.0f".format(s.total)} · ${s.dayCount} days" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No handover found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(FollowUpModel.displayDate(p.handoverDate), "${branchTag}Received by ${p.receiverName.ifBlank { "—" }}", "₹${"%,.0f".format(p.cash)}", "#0C8F3A", null))
                    }
                }
                "RMP_DUE" -> {
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.rmpDueSummary(branch) to VoiceReportRepository.rmpDueList(branch)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total due: ₹${"%,.0f".format(s.totalDue)} · ${s.rmpCount} RMP" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No due commission found.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.rmpName.ifBlank { p.rmpMobile }, "$branchTag${p.rmpMobile}", "₹${"%,.0f".format(p.due)}", "#B42318", null))
                    }
                }
                "MEDICINE_DUE" -> {
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.productDueSummary(branch) to VoiceReportRepository.productDueList(branch)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total due: ₹${"%,.0f".format(s.total)} · ${s.rowCount} bills" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No medicine/saline due found.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.customer.ifBlank { p.mobile }, "$branchTag${p.product} · ${FollowUpModel.displayDate(p.soldOn)}", "₹${"%,.0f".format(p.due)}", "#B42318", onTap))
                    }
                }
                "CALL_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.callList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} calls from app"
                    if (rows.isEmpty()) empty("No app calls found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffCode, "$branchTag${p.targetMobileMask} · ${FollowUpModel.displayDate(p.callDate)}", "›", "#94A3B8", null))
                    }
                }
                "TRASH_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.trashList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} records in Trash"
                    if (rows.isEmpty()) empty("No deleted records found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.tableName.ifBlank { "record" }, "$branchTag${FollowUpModel.displayDate(p.deletedAt.take(10))} · by ${p.deletedBy.ifBlank { "—" }}", "›", "#94A3B8", null))
                    }
                }
                "RMP_ADVANCE" -> {
                    val (sum, list) = withContext(Dispatchers.IO) {
                        VoiceReportRepository.rmpAdvanceSummary(branch, from, to) to VoiceReportRepository.rmpAdvanceList(branch, from, to)
                    }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total: ₹${"%,.0f".format(s.total)} · ${s.advanceCount} advance payments" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No RMP advance found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.rmpName, "$branchTag${p.mode} · ${FollowUpModel.displayDate(p.paidOn)}", "₹${"%,.0f".format(p.amount)}", "#0C8F3A", null))
                    }
                }
                // ── V1420 ──
                "APPOINTMENT_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.appointmentList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} appointments"
                    if (rows.isEmpty()) empty("No appointments found for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.disease} · ${FollowUpModel.displayDate(p.appointmentDate)}${if (p.registered) " · already registered" else ""}", "›", "#94A3B8", onTap))
                    }
                }
                "EXPECTED_COUNT" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.expectedList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Total: ${rows.size} marked expected"
                    if (rows.isEmpty()) empty("Nobody marked expected for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.mobile} · ${FollowUpModel.displayDate(p.expectedOn)}", "›", "#94A3B8", onTap))
                    }
                }
                "HANDOVER_PENDING" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.handoverPendingSummary(branch) to VoiceReportRepository.handoverPendingList(branch) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Not handed over: ₹${"%,.0f".format(s.total)} · ${s.dayCount} days" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No pending handover.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(FollowUpModel.displayDate(p.handoverDate), "$branchTag${p.status}", "₹${"%,.0f".format(p.cash)}", "#B42318", null))
                    }
                }
                "PAYMENT_REQUESTS" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.paymentRequestsSummary(branch) to VoiceReportRepository.paymentRequestsList(branch) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Pending: ${s.total} (${s.backdate} backdate · ${s.edit} edit · ${s.refund} refund)" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No pending payment requests.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.requestType} · ${FollowUpModel.displayDate(p.requestedOn)}", "₹${"%,.0f".format(p.amount)}", "#B45309", onTap))
                    }
                }
                "REFERRAL_REQUESTS" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.referralRequestsSummary(branch) to VoiceReportRepository.referralRequestsList(branch) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Pending: ${s.total} (${s.deleteCount} delete)" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No pending referral requests.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.requestType, "$branchTag${FollowUpModel.displayDate(p.requestedOn)}", if (p.requestType == "Delete") "—" else "₹${"%,.0f".format(p.newAmount)}", "#B45309", null))
                    }
                }
                "LEAVE_COUNT" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.leaveSummary(branch, from, to) to VoiceReportRepository.leaveList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Leave-days applied: ${s.total} · ${s.confirmed} confirmed · ${s.pending} pending · ${s.rejected} rejected" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No leave applications for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffCode, "${branchTag}Leave ${FollowUpModel.displayDate(p.leaveDate)} · applied ${FollowUpModel.displayDate(p.appliedOn)}", p.status, if (p.status == "confirmed") "#0C8F3A" else if (p.status == "rejected") "#B42318" else "#B45309", null))
                    }
                }
                "DOCTOR_REMINDER" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.doctorReminderSummary(branch, from, to) to VoiceReportRepository.doctorReminderList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Sent: ${s.total} · ${s.notAccepted} not accepted yet" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No doctor reminders for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row("Remind on ${FollowUpModel.displayDate(p.remindDate)}", "${branchTag}sent ${FollowUpModel.displayDate(p.createdOn)}", if (p.cancelled) "cancelled" else if (p.accepted) "accepted" else "open", if (p.cancelled) "#94A3B8" else if (p.accepted) "#0C8F3A" else "#B45309", null))
                    }
                }
                "STAFF_REMINDER_OPEN" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.staffReminderOpenSummary(branch) to VoiceReportRepository.staffReminderOpenList(branch) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    binding.tvSummary.text = "Open: ${sum.value ?: 0}"
                    val rows = (if (list.ok) list.value ?: emptyList() else emptyList()).filter { VoiceReportModel.nameMatch(extra, it.toName, it.toCode) }   // V1428/V1429 — নাম ধরে, ছেঁকে তবেই ফাঁকা-যাচাই
                    if (extra.isNotBlank()) binding.tvSummary.text = "Open for $extra: ${rows.size}"
                    if (rows.isEmpty()) empty("No open staff reminders.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.toName.ifBlank { p.toCode }, "$branchTag${p.reminderType} · ${FollowUpModel.displayDate(p.remindOn)}", p.status, "#B45309", null))
                    }
                }
                "FEE_RETURN" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.feeReturnSummary(branch, from, to) to VoiceReportRepository.feeReturnList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Visit fee returned: ₹${"%,.0f".format(s.total)} · ${s.patientCount} patients" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No visit fee returned in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${FollowUpModel.displayDate(p.returnedOn)}", "₹${"%,.0f".format(p.amount)}", "#B42318", onTap))
                    }
                }
                // ── V1421 ──
                "CHAMBER_UNCLOSED" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.chamberUnclosedList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Not closed: ${rows.size} days (today not counted)"
                    if (rows.isEmpty()) empty("Every active day was closed.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(FollowUpModel.displayDate(p.chamberDate), "$branchTag${p.arrived} patients had activity", "₹${"%,.0f".format(p.money)}", "#B45309", null))
                    }
                }
                "NO_SHOW" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.noShowSummary(branch, from, to) to VoiceReportRepository.noShowList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Did not come: ${s.noShow} · came: ${s.arrived} · expected: ${s.expectedTotal}" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("Nobody was marked expected for this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.mobile} · expected ${FollowUpModel.displayDate(p.expectedOn)}", if (p.arrived) "came" else "no-show", if (p.arrived) "#0C8F3A" else "#B42318", onTap))
                    }
                }
                "OUT_MISSING" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.outMissingSummary(branch, from, to) to VoiceReportRepository.outMissingList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "OUT time not given: ${s.total} days · ${s.staffCount} staff" else "Total: —"
                    val rows = (if (list.ok) list.value ?: emptyList() else emptyList()).filter { VoiceReportModel.nameMatch(extra, it.staffCode) }   // V1428/V1429
                    if (extra.isNotBlank()) binding.tvSummary.text = "OUT time not given · $extra: ${rows.size} days"
                    if (rows.isEmpty()) empty("No missing OUT time in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffCode, "${branchTag}IN ${p.checkIn} · ${FollowUpModel.displayDate(p.workDate)}", "OUT —", "#B42318", null))
                    }
                }
                "WFH_COUNT" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.wfhSummary(branch, from, to) to VoiceReportRepository.wfhList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "WFH applications: ${s.total} · ${s.approved} approved · ${s.pending} pending · ${s.rejected} rejected" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No WFH applications in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffName.ifBlank { p.staffCode }, "${branchTag}WFH ${FollowUpModel.displayDate(p.workDate)} · applied ${FollowUpModel.displayDate(p.requestedOn)}", p.status, if (p.status == "approved") "#0C8F3A" else if (p.status == "rejected") "#B42318" else "#B45309", null))
                    }
                }
                "DUPLICATE_PATIENTS" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.duplicateSummary(branch) to VoiceReportRepository.duplicateList(branch) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "${s.mobileGroups} same mobile · ${s.nameGroups} same name · ${s.paymentGroups} same payment (list shows same-mobile groups)" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No same-mobile duplicate groups.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: () -> Unit = { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) }
                        binding.rowsHost.addView(row(p.mobile, "$branchTag${p.names}", "${p.rowCount} records", "#B45309", onTap))
                    }
                }
                // 🎤🔒 V1504 (TK-নির্দেশ: "পাহারা ব্যবস্থা") — RIMPA ROY/KHAGEN BHAGAT-এর
                // মতো ডুপ্লিকেট পেমেন্ট: একই রোগী+দিন+লেবেল+অঙ্ক+ধরন, ১৫ মিনিটের মধ্যে।
                "DUPLICATE_PAYMENTS" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.duplicatePaymentsList(branch) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    val totalExtra = rows.sumOf { it.extraAmount }
                    binding.tvSummary.text = if (rows.isEmpty()) "No duplicate payments found" else "${rows.size} groups · ₹${"%,.0f".format(totalExtra)} extra"
                    if (rows.isEmpty()) empty("No duplicate payments found — nothing to check.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.payLabel} · ₹${"%,.0f".format(p.amount)} · ${p.mode} · ${FollowUpModel.displayDate(p.payDate)} · ${p.extraCount} extra", "₹${"%,.0f".format(p.extraAmount)}", "#B42318", onTap))
                    }
                }
                "FEE_UNPAID" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.feeUnpaidSummary(branch) to VoiceReportRepository.feeUnpaidList(branch) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    binding.tvSummary.text = "Visit fee not received: ${sum.value ?: 0} patients (registered from 05/09/2026)"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("Everyone's visit fee is recorded.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.patientCode} · ${FollowUpModel.displayDate(p.registrationDate)}", "›", "#94A3B8", onTap))
                    }
                }
                "CALLS_PENDING" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.callsPendingSummary(branch) to VoiceReportRepository.callsPendingList(branch) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    binding.tvSummary.text = "Pending follow-up calls today: ${sum.value ?: 0}"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No pending follow-up calls today.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.stage} · due ${FollowUpModel.displayDate(p.nextFollow)}", "›", "#94A3B8", onTap))
                    }
                }
                "MESSAGES_SENT" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.messagesSummary(branch, from, to) to VoiceReportRepository.messagesList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Messages opened to send: ${s.total} · ${s.whatsapp} WhatsApp · ${s.sms} SMS" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No messages in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.kind} · ${FollowUpModel.displayDate(p.sentOn)}", p.channel, "#0C8F3A", onTap))
                    }
                }
                // ── V1422 ──
                "NEW_PATIENTS" -> {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.newPatientsList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = "Registered, treatment not started: ${rows.size}"
                    if (rows.isEmpty()) empty("Everyone registered in this period has started treatment.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.patientCode} · ${FollowUpModel.displayDate(p.registrationDate)}", "›", "#94A3B8", onTap))
                    }
                }
                "FOLLOWUP_CALLS_DONE" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.fuCallsDoneSummary(branch, from, to) to VoiceReportRepository.fuCallsDoneList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Follow-up calls noted: ${s.total} (one per patient per day) · ${s.patientCount} patients" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No follow-up calls noted in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${FollowUpModel.displayDate(p.callDay)} · ${p.remarks} remark(s)", "›", "#94A3B8", onTap))
                    }
                }
                "DISEASE_COUNT" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.diseaseCount(branch, from, to, extra) to VoiceReportRepository.diseaseList(branch, from, to, extra) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "$extra: ${s.total} of ${s.allPatients} registered" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No $extra patients registered in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        val onTap: (() -> Unit)? = if (p.mobile.filter { it.isDigit() }.takeLast(10).length == 10) { { startActivity(android.content.Intent(this@VoiceReportDetailActivity, PatientTimelineActivity::class.java).putExtra("mobile", p.mobile)) } } else null
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.disease} · ${FollowUpModel.displayDate(p.registrationDate)}", "›", "#94A3B8", onTap))
                    }
                }
                "RMP_CALLED", "RMP_CALL_DUE" -> {
                    val due = metric == "RMP_CALL_DUE"
                    val got = withContext(Dispatchers.IO) { if (due) VoiceReportRepository.rmpCallDueList(branch, from, to) else VoiceReportRepository.rmpCalledList(branch, from, to) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    binding.tvSummary.text = if (due) "RMP doctors due for a call: ${rows.size}" else "RMP doctors called: ${rows.size}"
                    if (rows.isEmpty()) empty(if (due) "No RMP call due in this period." else "No RMP called in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.name.ifBlank { p.mobile }, "$branchTag${p.mobile} · last call ${FollowUpModel.displayDate(p.lastCallDate)} · next ${FollowUpModel.displayDate(p.nextCallDate)}", "›", "#94A3B8", null))
                    }
                }
                "FIELD_VISIT" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.fieldVisitSummary(branch, from, to) to VoiceReportRepository.fieldVisitList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Visits marked: ${s.visits} · ${"%.1f".format(s.km)} km · ${s.staffCount} field staff" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No field visit in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffCode, "$branchTag${FollowUpModel.displayDate(p.workDate)}", "${p.visits} visits · ${"%.1f".format(p.km)} km", "#0C8F3A", null))
                    }
                }
                // 🎤🔒 V1578 (আইটেম ৩৩) — অসময়ের এনকোয়ারির ইনসেন্টিভ।
                "INCENTIVE" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.incentiveSummary(branch, from, to) to VoiceReportRepository.incentiveList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total: ₹${"%,.0f".format(s.total)} · ${s.entryCount} entries · ${s.staffCount} staff" else "Total: —"
                    val rows = if (list.ok) list.value ?: emptyList() else emptyList()
                    if (rows.isEmpty()) empty("No incentive entries in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.personCode, "$branchTag${p.reason}", "₹${"%,.0f".format(p.amount)}", "#0C8F3A", null))
                    }
                }
                "STAFF_HOURS" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.staffHoursSummary(branch, from, to) to VoiceReportRepository.staffHoursList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Total ${"%.1f".format(s.totalHours)} h · ${s.staffCount} staff (leave/WFH/other-branch = 7 h, OUT missing = 7 h)" else "Total: —"
                    val rows = (if (list.ok) list.value ?: emptyList() else emptyList()).filter { VoiceReportModel.nameMatch(extra, it.staffCode) }   // V1428/V1429
                    if (extra.isNotBlank()) binding.tvSummary.text = "Hours · $extra: ${"%.1f".format(rows.sumOf { r -> r.hours })} h"
                    if (rows.isEmpty()) empty("No attendance in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffCode, "$branchTag${p.days} days · ${p.leaveDays} leave · ${p.outMissingDays} OUT missing", "${"%.1f".format(p.hours)} h", "#0C8F3A", null))
                    }
                }
                "STAFF_PRESENT" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.staffPresentSummary(branch, from, to) to VoiceReportRepository.staffPresentList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    binding.tvSummary.text = if (s != null) "Present: ${s.staffCount} staff · ${s.total} attendance days" else "Total: —"
                    val rows = (if (list.ok) list.value ?: emptyList() else emptyList()).filter { VoiceReportModel.nameMatch(extra, it.staffCode) }   // V1428/V1429
                    if (extra.isNotBlank()) binding.tvSummary.text = "Present · $extra: ${rows.size} attendance days"
                    if (rows.isEmpty()) empty("Nobody marked IN in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffCode, "$branchTag${FollowUpModel.displayDate(p.workDate)}", "IN ${p.checkIn}${if (p.checkOut.isNotBlank()) " · OUT ${p.checkOut}" else ""}", "#0C8F3A", null))
                    }
                }
                // 🎤 V1428 (তালিকা ৫৩৮) — RMP-কে দেওয়া কমিশন · IN-বাদ · কোন ব্রাঞ্চে সবচেয়ে বেশি/কম
                "RMP_PAID" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.rmpPaidSummary(branch, from, to) to VoiceReportRepository.rmpPaidList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    val rows = (if (list.ok) list.value ?: emptyList() else emptyList()).filter { VoiceReportModel.nameMatch(extra, it.rmpName) }
                    binding.tvSummary.text = if (extra.isNotBlank()) "Paid to $extra: ₹${"%,.0f".format(rows.sumOf { it.amount })} · ${rows.size} payments"
                        else if (s != null) "Paid: ₹${"%,.0f".format(s.total)} · ${s.rmpCount} RMPs · ${s.paymentCount} payments (same as RMP Commission Sheet)" else "Total: —"
                    if (rows.isEmpty()) empty("No RMP commission paid in this period.")
                    rows.forEach { p ->
                        // ⛔ সারিতে RMP-র মোবাইল নেই, তাই চাপলে কোথাও যায় না (আন্দাজে পাতা খোলা নিষেধ)
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.rmpName.ifBlank { p.rmpId }, "$branchTag${if (p.kind == "advance") "Advance" else "For ${p.patientName.ifBlank { "patient" }}"} · ${p.mode} · ${FollowUpModel.displayDate(p.paidOn)}", "₹${"%,.0f".format(p.amount)}", "#0C8F3A", null))
                    }
                }
                "IN_MISSING" -> {
                    val (sum, list) = withContext(Dispatchers.IO) { VoiceReportRepository.inMissingSummary(branch, from, to) to VoiceReportRepository.inMissingList(branch, from, to) }
                    if (!sum.ok) { fail(sum.message); return@launch }
                    val s = sum.value
                    val rows = (if (list.ok) list.value ?: emptyList() else emptyList()).filter { VoiceReportModel.nameMatch(extra, it.staffCode, it.staffName) }
                    binding.tvSummary.text = if (extra.isNotBlank()) "IN time not given · $extra: ${rows.size} days"
                        else if (s != null) "IN time not given: ${s.total} days · ${s.staffCount} staff (only days the notebook was opened)" else "Total: —"
                    if (rows.isEmpty()) empty("No missing IN time in this period.")
                    rows.forEach { p ->
                        val branchTag = if (branch == "ALL") "${p.branch} · " else ""
                        binding.rowsHost.addView(row(p.staffName.ifBlank { p.staffCode }, "$branchTag${p.staffCode} · ${FollowUpModel.displayDate(p.workDate)}${if (p.checkOut.isNotBlank()) " · OUT ${p.checkOut}" else ""}", "IN —", "#B42318", null))
                    }
                }
                "BRANCH_TOP_COLLECTION", "BRANCH_TOP_PATIENTS" -> {
                    val money = metric == "BRANCH_TOP_COLLECTION"
                    val lowest = extra == "min"
                    val got = withContext(Dispatchers.IO) { if (money) VoiceReportRepository.branchRankCollection(from, to, lowest) else VoiceReportRepository.branchRankPatients(from, to, lowest) }
                    if (!got.ok) { fail(got.message); return@launch }
                    val rows = got.value ?: emptyList()
                    val grand = if (money) "₹${"%,.0f".format(rows.sumOf { it.value })}" else "${rows.sumOf { it.patients }} patients"
                    binding.tvSummary.text = "All branches: $grand · ${if (lowest) "lowest" else "highest"} first · tap a branch for its list"
                    rows.forEachIndexed { i, r ->
                        val onTap: () -> Unit = {
                            startActivity(android.content.Intent(this@VoiceReportDetailActivity, VoiceReportDetailActivity::class.java)
                                .putExtra("metric", if (money) "COLLECTION" else "REGISTRATION_COUNT").putExtra("branch", r.branch)
                                .putExtra("from", from).putExtra("to", to).putExtra("title", title.replaceFirst("All branches", r.branch)))
                        }
                        binding.rowsHost.addView(row("${i + 1}. ${r.branch}", if (money) "${r.patients} patients" else "", if (money) "₹${"%,.0f".format(r.value)}" else "${r.patients}", if (i == 0) "#0C8F3A" else "#334155", onTap))
                    }
                }
                else -> fail("Unknown report")
            }
            binding.progressLoad.visibility = android.view.View.GONE
        }
    }

    private fun fail(message: String) {
        binding.progressLoad.visibility = android.view.View.GONE
        binding.tvSummary.text = "Could not load this report."
        Toast.makeText(this, message.ifBlank { "Could not load this report" }, Toast.LENGTH_LONG).show()
    }

    private fun empty(text: String) {
        binding.rowsHost.addView(TextView(this).apply {
            this.text = text; textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#8A97A8"))
            setPadding(16, 32, 16, 32)
        })
    }
}
