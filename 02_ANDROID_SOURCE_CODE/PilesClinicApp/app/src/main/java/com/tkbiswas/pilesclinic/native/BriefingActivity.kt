package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.databinding.ActivityBriefingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native rebuild -- Briefing / Notice Board.
 *
 * Master posts notices to staff (All Staff / a Branch / a Role); staff see the
 * notices that target them, mark Seen, and Reply. Mirrors the WebView's
 * briefings workflow, reading/writing the live Supabase "briefings" table.
 *
 * SCOPED LIMITATIONS (honest disclosure):
 * - Posting targets here are All Staff / a Branch / a Role (the common cases).
 *   Per-individual mobile targeting (the WebView's "individual" picker) is not
 *   offered on this screen yet; it can be added once the common flow is verified.
 * - Deleting a briefing / deleting a single reply is not exposed here yet
 *   (view + seen + reply only), so nothing is ever destructively removed from
 *   this screen.
 */
class BriefingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBriefingBinding
    private lateinit var repository: BriefingRepository
    private lateinit var adapter: BriefingAdapter
    private lateinit var user: NativeUser

    private val branches = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
    private val roles = listOf("staff", "doctor", "field")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBriefingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        // 🔴🆕 V439 — উপরের খোপগুলোর ScrollView-এর উচ্চতা নিজে থেকেই ঠিক রাখে
        //    (দরকারমতো, তবে পর্দার ৬২%-এর বেশি নয়)। খোপ খোলা/বন্ধ হলেই চলে।
        installPanelsScrollClamp()
        repository = BriefingRepository()

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        adapter = BriefingAdapter(
            this, emptyList(),
            onSeen = { markSeen(it) },
            onReply = { showChatThread(it) },
            onDelete = { confirmDelete(it) },
            // 🔒 খাতার সারি B100 (TK, 29.07.2026 রাত ১১.১০): মাস্টার এখান থেকেই
            // এক চাপে স্টাফের ডিলিট-অনুরোধ অনুমোদন করতে পারবেন।
            onApproveDelete = { confirmApproveDelete(it) },
            onReject = { confirmReject(it) },   // 🟢 V1134 — TK: "আমি অনুমতি না দিলে কোনটা চাপব?"
            // 🔴 B281 (02.08.2026, TK-রিপোর্ট): Refund request নোটিশ থেকেই
            // সরাসরি Approve/Reject করা যাবে, আলাদা ড্রপডাউন খুঁজতে হবে না।
            onApproveRefund = { confirmApproveRefund(it) },
            // 🆕 B419 (04.08.2026, TK-নির্দেশ): Chamber Reopen-এর অনুরোধ এখান
            // থেকেই Approve/Reject করা যাবে।
            onApproveReopen = { confirmApproveReopen(it) },
            onCallNumber = { digits -> handleBriefingNumberTap(digits) },
            // 🔴🔒 V501 (TK-নির্দেশ ২১.০৮.২০২৬): নোটিশের "View" — ওই নম্বরের
            //    রোগীর পাতা সরাসরি খোলে। ⛔ নতুন পথ বানানো হয়নি: ফোন করার
            //    পরে যে পথে রেকর্ড খোলা হয় (`checkNumberAfterCall`), হুবহু সেটাই।
            onViewRecord = { item, digits -> openRecordForNumber(item, digits) },
            // 🟢🔒 V692 (২৬.০৮.২০২৬, TK-নির্দেশ ছবিসহ) — ⚠️ Overdue Follow-up
            //    Alert-এ Reply-র বদলে View, আর View চাপলে ওই ব্রাঞ্চের
            //    ৩+ দিন দেরি হওয়া কলগুলোই খোলে।
            onViewOverdue = { item -> openOverdueAlert(item) },
            // 📋🔒 V1435 (তালিকা ৫৫৪, TK "পাশ") — "Daily/Monthly Report submitted"-এর View।
            onViewReport = { item -> openReportNotice(item) },
            isMaster = session.role == "master",
            // 🆕 (07.08.2026) — কার্ডে টিক পড়লে/উঠলে নিচের "একসাথে অনুমোদন" বার হালনাগাদ।
            onSelectChanged = { refreshBulkBar() }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        attachSwipeToClear()
        // 🆕 (07.08.2026) — "একসাথে অনুমোদন" বারের দুটো বোতাম।
        binding.btnBulkCancel.setOnClickListener { adapter.clearSelection(); refreshBulkBar() }
        binding.btnBulkApprove.setOnClickListener { confirmBulkApprove() }
        // TK-REQUESTED (2026-07-27): pull the list down to refresh -- the same
        // gesture as Follow-up, on every section. It runs the screen's OWN
        // existing load, so no new query and no rule changes; the little circle
        // stops on its own when that load has had time to finish.
        binding.swipeRefresh.setColorSchemeColors(
            android.graphics.Color.parseColor("#0EA25F"),
            android.graphics.Color.parseColor("#1167D8")
        )
        binding.swipeRefresh.setOnRefreshListener {
            loadList()
            binding.swipeRefresh.postDelayed({
                try { binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
            }, 2500L)
        }

        binding.btnBack.setOnClickListener { finish() }
        // 🔴🆕🔒 B467 (05.08.2026, TK-নির্দেশ — "ব্রিফিং অথবা মেসেজিং, সেখানে
        // চাইলে প্রতিটা স্টাফ কেউ পাঠাতে পারে তার ব্যবস্থা রাখবেন") — আগে
        // শুধু Master নোটিশ পাঠাতে পারতেন (WebView-এর সাথে মিলিয়ে)। এখন
        // **সবাই** পারবেন — কম্পোজার (`showPostDialog()`) আগে থেকেই
        // "All Staff/A Branch/A Role/An Individual" টার্গেট সাপোর্ট করে
        // (নতুন কিছু বানাতে হয়নি), তাই এক ব্রাঞ্চের স্টাফ আরেক ব্রাঞ্চে
        // বা নির্দিষ্ট একজনকে সরাসরি নোটিশ (যেমন এনকোয়ারির নম্বর) পাঠাতে
        // পারবেন। ⛔ Delete/Approve-Refund/Approve-Reopen ইত্যাদি এখনো
        // Master-only-ই (`isMaster` চেক ছোঁয়া হয়নি) — শুধু "নতুন নোটিশ
        // পাঠানো"-র অনুমতি সবার জন্য খুলল।
        binding.btnAdd.setOnClickListener { showPostDialog() }

        loadList()
        // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow --
        // Master-only pending-approval section, shown above the existing
        // Briefing list. No-op (stays GONE) for any non-Master role.
        loadBackdateRequests()
        // 🆕 B337 (03.08.2026, TK-নির্দেশ) — "Backdate Payment Permissions"
        // (স্টাফকে সাময়িক অনুমতি দেওয়া/Revoke) — Master-only, no-op অন্য role-এ।
        loadBackdateGrants()
        // TK-REQUESTED ADDITION (2026-07-25): "Edit Payment Amount"
        // workflow -- same reasoning as Backdate just above.
        loadEditRequests()
        loadSalaryDue()   // 🟢 B629: Master ও Doctor-এর Salary Due মনে করানো
        // V216 (§13): Refund request approval — Master-only, no-op অন্য role-এ।
        loadRefundRequests()
        loadLeaveRequests()   // 🔵 B618: master/ব্রাঞ্চ-ডাক্তারের ছুটি-অনুমোদন
        loadWfhRequests()     // 🏠 V1179: Work From Home-এর অনুমোদন
        // TK-REQUESTED ADDITION (2026-07-24): "Visit Fee Missing"
        // visibility -- Master-only, read-only, no-op for any non-Master
        // role.
        loadMissingVisitFees()
        // 🔒 খাতার সারি B51 (TK, 28.07.2026): ঘন্টায় আসা "রিমার্ক বাকি" তালিকা।
        loadPendingRemarks()
        // 🆕 B467 (05.08.2026, TK-নির্দেশ) — BriefingReminderWorker-এর
        // নোটিফিকেশন থেকে এলে (IN/OUT TIME-এর quick_mark-এর হুবহু একই
        // প্যাটার্ন) — ছোট প্রশ্ন: এখনই দেখবেন, নাকি পরে মনে করাবে।
        maybeShowReminderPrompt()
        // 🆕🔒 B486 (06.08.2026) — NotificationsActivity-এর ➕ থেকে এলে
        // (extra "openCompose"=true), সরাসরি Post Notice ডায়ালগ খোলে।
        if (intent.getBooleanExtra("openCompose", false)) showPostDialog()
    }

    // 🆕 B467 — "এখনই দেখব" চাপলে শুধু ডায়ালগ বন্ধ (পুরো পাতা এমনিতেই
    // খোলা আছে, তালিকা এমনিতেই লোড হয়ে গেছে)। "⏰ পরে মনে করাবেন" চাপলে
    // TimePickerDialog (WorkNotebookActivity.kt-এর askPostponeTime()-এর
    // হুবহু একই প্যাটার্ন) — বাছা সময়েই আবার নোটিফিকেশন আসবে।
    private fun maybeShowReminderPrompt() {
        if (!intent.getBooleanExtra("quick_reminder", false)) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06): colored header strip (same PremiumAlert
            // header every other styled popup uses) instead of the plain title —
            // was half-styled (rounded card via paint() but no header band).
            .setCustomTitle(PremiumAlert.header(this, "🔔 Unread Notice"))
            .setMessage("You have unread Briefing/Notice messages.")
            .setPositiveButton("👀 I'll check now", null)
            .setNegativeButton("⏰ Remind me later") { _, _ ->
                val cal = java.util.Calendar.getInstance()
                android.app.TimePickerDialog(this, { _, hour, minute ->
                    try { BriefingReminderScheduler.scheduleExactTime(this, hour, minute) } catch (_: Throwable) { }
                    // 🔴 V1158 — দেখানোর ঘড়ি সবসময় "2.30 PM" ধাঁচে (২৪-ঘণ্টা নয়)।
                    val ap12 = if (hour < 12) "AM" else "PM"
                    val h12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
                    val label = String.format(java.util.Locale.US, "%d.%02d %s", h12, minute, ap12)
                    Toast.makeText(this, "OK, will remind you again at $label", Toast.LENGTH_LONG).show()
                }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), false).show()
            }
            .setCancelable(true)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

        private var firstResume = true
    private var pendingCallCheckNumber = ""
    override fun onResume() {
        super.onResume()
        if (firstResume) { firstResume = false; return }
        loadList()
        loadBackdateRequests()
        loadBackdateGrants()
        loadEditRequests()
        loadSalaryDue()   // 🟢 B629: Master ও Doctor-এর Salary Due মনে করানো
        loadRefundRequests()   // V216 (§13)
        loadLeaveRequests()    // 🔵 B618
        loadWfhRequests()      // 🏠 V1179
        loadMissingVisitFees()
        loadPendingRemarks()   // 🔒 খাতার সারি B51
        // 🆕 B467 (05.08.2026, TK-নির্দেশ — "কলিং অপশন খুলে যাওয়ার পরে যখন
        // অ্যাপ্লিকেশনে ফেরত আসবে... অ্যাকশন আসবে রিমার্কস লেখার") — কল
        // করে ফিরে এলেই (এই onResume ঠিক তখনই চলে) নম্বরটা মেলানো হয়।
        if (pendingCallCheckNumber.isNotBlank()) {
            val n = pendingCallCheckNumber
            pendingCallCheckNumber = ""
            checkNumberAfterCall(n)
        }
    }

    // 🆕 B467 — Briefing-এ পাঠানো নম্বরে ট্যাপ করলে ডাকা হয় (BriefingAdapter-এর
    // onCallNumber কলব্যাক)। কল করার পরে অ্যাপে ফিরলে (onResume) নম্বরটা
    // `followups`-এ (Enquiry/Visit/Patient — যেকোনো ব্রাঞ্চের, DialerRepository-র
    // হুবহু একই খোঁজা) মিললে সরাসরি সেই রোগীর Timeline খোলে (Take Action →
    // Remark ওখান থেকেই করা যাবে); না মিললে নতুন Enquiry ফর্ম ভরার সাজেশন।
    private fun handleBriefingNumberTap(digits: String) {
        pendingCallCheckNumber = digits
        CallChooser.open(this, digits)
    }

    /**
     * 🟢🔒 V692 (২৬.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) — ⚠️ **Overdue Follow-up
     * Alert-এর "View"।**
     *
     * TK-এর কথা (হুবহু): *"Overdue Call Alert এ Reply কেন আসবে, সেখানে
     * View থাকতে হবে, আর View তে চাপলে যেন দেখা যায়।"*
     * TK-এর বাছা: **শুধু ওই ৩+ দিন দেরি হওয়া লোকগুলোই** (নোটিশে যত জন
     * লেখা, ঠিক তত জন) — ব্রাঞ্চের সব Overdue নয়।
     *
     * নোটিশের লেখা ঠিক এই ধাঁচের (DashboardActivity যেভাবে বানায়):
     *   Jalpaiguri \u2014 9 calls overdue 3+ days
     *   Cooch Behar \u2014 4 calls overdue 3+ days
     * তাই প্রতি লাইনের "\u2014"-এর আগের অংশটাই ব্রাঞ্চের নাম।
     * একটা ব্রাঞ্চ থাকলে সোজা খোলে; একাধিক থাকলে কোনটা দেখবেন জিজ্ঞাসা করা
     * হয় (নিজে থেকে একটা বেছে নেওয়া হয় না)।
     */
    private fun openOverdueAlert(item: Briefing) {
        val dash = "\u2014"
        val branches = item.message.lines()
            .mapNotNull { line ->
                if (!line.contains(dash)) null
                else line.substringBefore(dash).trim().takeIf { it.isNotBlank() && it != dash }
            }
            .distinct()
        when {
            branches.isEmpty() -> openOverdue3Plus("")
            branches.size == 1 -> openOverdue3Plus(branches[0])
            else -> androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "\u23F0 Overdue 3+ Days"))
                .setItems(branches.toTypedArray()) { _, i -> openOverdue3Plus(branches[i]) }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    /**
     * 📋🔒 V1435 (১৩.০৯.২০২৬, তালিকা ৫৫৪, TK "পাশ", ডেমো পাশ) — **রিপোর্ট-নোটিশের View।**
     *
     * TK: *"Daily Report Submit করেছে বেশ ভালো কথা, তাহলে এখানে সেই Report View কেন
     * করা যাবে না"* — এতদিন রিপোর্ট দেখার কোনো পর্দাই ছিল না (আমার ফাঁক)।
     *
     * নোটিশের লেখা (WorkNotebookActivity.submit / notebook.js nbSubmit): "নাম · কোড · ব্রাঞ্চ · key"
     * → কোড = দ্বিতীয় অংশ, key (2026-09-13 / 2026-09) = শেষ অংশ, ধরন = শিরোনাম।
     * সার্ভার থেকে ওই স্টাফের ওই দিনের/মাসের **সবচেয়ে নতুন সংস্করণ** পড়া হয়
     * (RLS: মাস্টার সব পড়তে পারেন, স্টাফ শুধু নিজেরটা)।
     *  · report_text থাকলে হুবহু সেটাই (V1435-এর পরের রিপোর্ট)।
     *  · না থাকলে (পুরনো রিপোর্ট) সংখ্যা + সেদিনের খাতা থেকে একই ধাঁচে বানানো
     *    (ReportTextBuilder) — জমা না-থাকা ঘরে "-"।
     * বোতাম: ✔ Seen (মাস্টার, একবার — seen_at/seen_by লেখে) · Share · Close।
     * ⛔ রিপোর্টের কোনো সংখ্যা বদলায় না; শুধু পড়া + Seen-চিহ্ন।
     */
    private fun openReportNotice(item: Briefing) {
        val parts = item.message.split("\u00b7").map { it.trim() }.filter { it.isNotBlank() }
        val staffCode = parts.getOrNull(1).orEmpty()
        val key = parts.lastOrNull().orEmpty()
        val type = if (item.title.contains("Monthly", ignoreCase = true)) "monthly" else "daily"
        if (staffCode.isBlank() || key.isBlank()) {
            Toast.makeText(this, "Report details missing in this notice", Toast.LENGTH_SHORT).show(); return
        }
        val session = NativeSession.current(this)
        val isMaster = session?.role == "master"
        val loading = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "\uD83D\uDCCB Loading report\u2026"))
            .setMessage("$staffCode \u00b7 $key")
            .setCancelable(true)
            .show().also { PremiumAlert.paint(it) }
        Thread {
            fun enc(v: String) = try { java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20") } catch (_: Throwable) { v }
            val rows = com.tkbiswas.pilesclinic.modules.ModuleAuth.getRows(
                "wn", "work_reports",
                "select=id,branch,auto_stats,manual_summary,report_text,submitted_at,seen_at,version" +
                    "&staff_code=eq." + enc(staffCode) + "&period_type=eq." + type + "&period_key=eq." + enc(key) +
                    "&order=version.desc&limit=1"
            )
            val r = if (rows.length() > 0) rows.optJSONObject(0) else null
            fun ns(o: org.json.JSONObject?, k: String): String {
                if (o == null || o.isNull(k)) return ""
                val v = o.optString(k, "").trim(); return if (v.equals("null", true)) "" else v
            }
            var text = ns(r, "report_text")
            if (r != null && text.isBlank()) {
                val stats = r.optJSONObject("auto_stats")
                text = if (type == "daily") {
                    val dayRows = com.tkbiswas.pilesclinic.modules.ModuleAuth.getRows(
                        "wn", "notebook_days",
                        "select=check_in,check_out,is_leave,leave_reason,day_note,outside_calls_manual" +
                            "&staff_code=eq." + enc(staffCode) + "&work_date=eq." + enc(key) + "&limit=1"
                    )
                    com.tkbiswas.pilesclinic.modules.ReportTextBuilder.daily(
                        staffCode, key, stats, if (dayRows.length() > 0) dayRows.optJSONObject(0) else null, ns(r, "manual_summary"))
                } else com.tkbiswas.pilesclinic.modules.ReportTextBuilder.monthly(staffCode, key, stats)
            }
            val branch = ns(r, "branch").ifBlank { item.branch }
            val sentMs = BranchSimHelper.isoToMillis(ns(r, "submitted_at"))
            val sentTxt = if (sentMs > 0L) BranchSimHelper.whenText(sentMs) else ""
            val reportId = ns(r, "id")
            val alreadySeen = ns(r, "seen_at").isNotBlank()
            runOnUiThread {
                try { loading.dismiss() } catch (_: Throwable) { }
                if (r == null) {
                    Toast.makeText(this, "Report not found on server ($staffCode \u00b7 $key)", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val d = resources.displayMetrics.density
                fun dp(v: Int) = (v * d).toInt()
                val titleTxt = (if (type == "daily") "\uD83D\uDCCB Daily Report \u00b7 " + com.tkbiswas.pilesclinic.modules.ReportTextBuilder.dotDate(key)
                                else "\uD83D\uDCCA Monthly Report \u00b7 " + key)
                val sub = listOf(staffCode, branch, if (sentTxt.isNotBlank()) "sent $sentTxt" else "").filter { it.isNotBlank() }.joinToString(" \u00b7 ")
                val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(4)) }
                col.addView(TextView(this).apply {
                    this.text = sub; textSize = 12.5f
                    setTextColor(android.graphics.Color.parseColor("#5B7089")); setPadding(0, 0, 0, dp(8))
                })
                col.addView(TextView(this).apply {
                    this.text = boldLabels(text)
                    textSize = 14.5f; setLineSpacing(0f, 1.25f)
                    setTextColor(android.graphics.Color.parseColor("#0F2438"))
                    setTextIsSelectable(true)
                })
                val sv = ScrollView(this).apply { addView(col) }
                val b = AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, titleTxt))
                    .setView(sv)
                    .setNegativeButton("Close", null)
                    .setNeutralButton("Share") { _, _ ->
                        try { WhatsAppMessageChooser.sendGeneric(this, text) } catch (_: Throwable) { }
                    }
                if (isMaster && reportId.isNotBlank()) {
                    b.setPositiveButton(if (alreadySeen) "\u2714 Seen" else "\u2714 Mark Seen") { _, _ ->
                        if (alreadySeen) return@setPositiveButton
                        Thread {
                            val patch = org.json.JSONObject()
                                .put("seen_at", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()))
                                .put("seen_by", session?.name?.ifBlank { "MASTER" } ?: "MASTER")
                            val ok = com.tkbiswas.pilesclinic.modules.ModuleAuth.update("wn", "work_reports", "id=eq." + enc(reportId), patch)
                            runOnUiThread { Toast.makeText(this, if (ok) "Marked as seen" else "Could not mark seen \u2014 try again", Toast.LENGTH_SHORT).show() }
                        }.start()
                    }
                }
                val dlg = b.show()
                PremiumAlert.paint(dlg)
            }
        }.start()
    }

    /** 📋 V1435 — রিপোর্টের "লেবেল:" অংশগুলো (IN TIME- · New Enquiry: · Notes:) মোটা ও নীল। */
    private fun boldLabels(text: String): CharSequence {
        val sp = android.text.SpannableStringBuilder(text)
        val re = Regex("(?m)^([A-Za-z][A-Za-z ]{1,24}[:\\-])")
        for (mt in re.findAll(text)) {
            sp.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), mt.range.first, mt.range.last + 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#0B3D91")), mt.range.first, mt.range.last + 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sp
    }

    /** 🟢🔒 V692 — ব্রাঞ্চের ছাঁকনি বসিয়ে Follow-up পর্দা খোলা, ৩+ দিন
     *  দেরি হওয়া মোডে। ⛔ নতুন কোনো পর্দা বানানো হয়নি — ড্যাশবোর্ডের
     *  "N calls pending today" যে পথে যায়, হুবহু সেই পথ ও সেই পর্দা;
     *  শুধু ছাঁকনিটা "আজ"-এর বদলে "৩+ দিন দেরি"। */
    private fun openOverdue3Plus(branch: String) {
        if (branch.isNotBlank()) {
            try { BranchFilterStore.set(this, branch) } catch (_: Throwable) { }
        }
        startActivity(
            android.content.Intent(this, FollowUpActivity::class.java)
                .putExtra("overdue3Plus", true)
        )
    }

    /**
     * 🔴🔒 V501 — নোটিশের "View" বোতাম চাপলে।
     *
     * নম্বরটা কোনো Enquiry/Visit/রোগীর রেকর্ডে থাকলে সরাসরি তাঁর
     * Patient Timeline খোলে; না থাকলে সৎভাবে জানিয়ে নতুন Enquiry ভরার
     * প্রস্তাব দেয় (ফোন করার পরের যাচাইয়ের হুবহু একই আচরণ)।
     * ⛔ কোনো তথ্য লেখা/বদলানো হয় না — শুধু পড়া ও পর্দা খোলা।
     */
    /**
     * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-নির্দেশ ছবিসহ) — **View চাপলে ঠিক জায়গায় যাবে।**
     *
     * TK-এর কথা (হুবহু): *"এক কথায় ভিউতে চাপ দিলে রিডাইরেক্ট করবে নাম্বারটা
     * যেখানে থাকতে হবে। অর্থাৎ যদি এনকোয়ারি থেকে এসে থাকে তাহলে রিডাইরেক্ট হবে
     * ফলোআপের ইনকয়ারি সেকশনে। যদি ভিজিট অর্থাৎ রেজিস্ট্রেশন হয়ে থাকে, সেই
     * ক্ষেত্রে ভিউতে ক্লিক করলে রিডাইরেক্ট হবে ফলোআপের ভিজিট সেকশনে... যদি
     * অ্যাডভান্স হয়ে থাকে তাহলে ভিউতে চাপ দিলে রিডাইরেক্ট হবে সে কত টাকা
     * এডভান্স করল।"*
     *
     * ─── কীভাবে (নতুন কিছু বানানো হয়নি) ──────────────────────────────────
     *  · **New Enquiry / New Registration** → `FollowUpActivity`-তে
     *    `focusCardMobile` দিয়ে পাঠানো হয়। এই ব্যবস্থাটা **আগে থেকেই আছে ও
     *    কাজ করে** (১০.০৮.২০২৬, "কাল আসার কথা" কার্ড থেকে) — নম্বরটা
     *    Enquiry · Visit · Patient — যে সেকশনে আছে, **নিজে থেকেই** সেই ট্যাবে
     *    গিয়ে কার্ডটা হাইলাইট করে। তাই এনকোয়ারি হলে Enquiry-তে, রেজিস্ট্রেশন
     *    হলে Visit/Patient-এ — TK যা চেয়েছেন হুবহু তাই।
     *  · **Advance Received** → `PaymentActivity`-তে ঐ নম্বর নিয়ে (কত টাকা
     *    জমা পড়ল, বাকি কত — সবই ওখানে)।
     *  · **বাকি সব নোটিশে** আগের আচরণ (রোগীর Timeline) — এক অক্ষরও বদলায়নি।
     *
     * ─── ব্রাঞ্চ (TK-অনুমোদিত, ২১.০৮.২০২৬) ───────────────────────────────
     * নোটিশটা Cooch Behar-এর, কিন্তু মাস্টারের ব্রাঞ্চ-ছাঁকনি তখন Jalpaiguri-তে
     * থাকলে Follow-up-এ নম্বরটা পাওয়াই যেত না। TK বলেছেন *"হ্যাঁ দিন"* —
     * তাই View চাপলে ছাঁকনিটা **নোটিশের ব্রাঞ্চে বদলে দেওয়া হয়**, আর উপরে
     * ছোট করে জানিয়ে দেওয়া হয় (চুপচাপ নয়)।
     * ⛔ শুধু **মাস্টারের** ক্ষেত্রে — স্টাফের ব্রাঞ্চ নিজের, সেটা ছোঁয়া হয় না।
     * ⛔ ইতিমধ্যেই ঠিক ব্রাঞ্চে থাকলে (বা "All" থাকলে) কিছুই বদলায় না।
     */
    private fun openRecordForNumber(item: Briefing, digits: String) {
        val auto = item.title.trim()
        val isEnquiry = auto.equals("New Enquiry", ignoreCase = true)
        val isReg = auto.equals("New Registration", ignoreCase = true)
        val isAdvance = auto.equals("Advance Received", ignoreCase = true)
        if (!isEnquiry && !isReg && !isAdvance) {
            checkNumberAfterCall(digits)   // ⛔ পুরনো আচরণ, অক্ষত
            return
        }
        switchBranchForNotice(item)
        val next = if (isAdvance)
            android.content.Intent(this, PaymentActivity::class.java)
                .putExtra("mobile", digits)
        else
            android.content.Intent(this, FollowUpActivity::class.java)
                .putExtra("focusCardMobile", digits)
                /* 🏷️🔒 V1254 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, খাতার সারি ৩৭৫) —
                   TK: *"কোন নম্বরটা Enquiry-তে নতুন এসেছে বুঝতে পারি না"*।
                   কার্ডটা চেনানোর ছোট ট্যাগের লেখাটা এখান থেকেই যায় (৬ সেকেন্ড
                   থাকে — TK-র নিজের সিদ্ধান্ত)। ⛔ অন্য পথ থেকে এলে ট্যাগ যায় না,
                   তখন আগের মতোই শুধু হাইলাইট — বানানো লেখা কখনো বসে না। */
                .putExtra("focusCardTag", if (isEnquiry) "NEW ENQUIRY" else "NEW REGISTRATION")
        try { startActivity(next) } catch (_: Throwable) { }
    }

    /** 🔴 V511 — উপরের নোটের "ব্রাঞ্চ" অংশ। ⛔ শুধু মাস্টার, শুধু দরকার হলে। */
    private fun switchBranchForNotice(item: Briefing) {
        try {
            if (user.role != "master") return
            val want = item.branch.trim()
            if (want.isBlank() || want.equals("All", ignoreCase = true)) return
            val now = BranchFilterStore.get(this).trim()
            if (now.equals(want, ignoreCase = true) || now.equals("All", ignoreCase = true)) return
            BranchFilterStore.set(this, want)
            Toast.makeText(this, "Branch switched to $want", Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) { }
    }

    private fun checkNumberAfterCall(digits: String) {
        lifecycleScope.launch {
            val matchedId = withContext(Dispatchers.IO) {
                try {
                    val rows = SupabaseClient.findByMobile("followups", digits, "id,stage,branch", 5)
                    if (rows.length() > 0) rows.getJSONObject(0).optString("id") else ""
                } catch (_: Throwable) { "" }
            }
            if (matchedId.isNotBlank()) {
                startActivity(android.content.Intent(this@BriefingActivity, PatientTimelineActivity::class.java).putExtra("mobile", digits))
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this@BriefingActivity)
                    .setCustomTitle(PremiumAlert.header(this@BriefingActivity, "Not found"))
                    .setMessage("This number isn't in any Enquiry, Visit, or Patient record yet. Fill a new Enquiry form for it?")
                    .setPositiveButton("Yes, fill Enquiry") { _, _ ->
                        startActivity(android.content.Intent(this@BriefingActivity, EnquiryActivity::class.java).putExtra("prefillMobile", digits))
                    }
                    .setNegativeButton("No", null)
                    .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
            }
        }
    }

    /**
     * 🔒 খাতার সারি B51 (TK, 28.07.2026 রাত)
     *
     * TK-এর কথা: *"যদি অ্যাপ্লিকেশনে না ফেরে তাহলে Dashboard-এ যে ঘন্টা আছে
     * সেখানে অবশ্যই নোটিফিকেশন আসতে হবে।"* ঘন্টায় চাপ দিলে এই পর্দাটাই খোলে,
     * তাই বাকির নামগুলো এখানে দেখায়। নামে চাপ দিলে Follow-up খুলে **ওই
     * ব্যক্তির রিমার্কের বাক্সটাই** সরাসরি চলে আসে।
     *
     * ⛔ ক্লাউডে কোনো অনুরোধ নেই — তালিকাটা এই ফোনের নিজের ঘরে (`PendingRemarkStore`)।
     * ⛔ কিছু বাকি না থাকলে পুরো অংশটাই লুকানো — পর্দার আগের চেহারা অক্ষত।
     * ⛔ "Visit Fee Missing" অংশের সঙ্গে হুবহু একই ধাঁচ — নতুন কোনো ডিজাইন নয়।
     */
    private fun loadPendingRemarks() {
        val box = binding.pendingRemarkContainer
        box.removeAllViews()
        val pending = try { PendingRemarkStore.list(this, user.mobile) } catch (_: Throwable) { emptyList() }
        if (pending.isEmpty()) {
            box.visibility = View.GONE
            return
        }
        box.visibility = View.VISIBLE
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        box.addView(TextView(this).apply {
            text = "REMARK PENDING (${pending.size})"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#B42318"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
        })
        for (p in pending) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(android.graphics.Color.WHITE)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                isClickable = true
                isFocusable = true
                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                foreground = androidx.core.content.ContextCompat.getDrawable(this@BriefingActivity, outValue.resourceId)
                setOnClickListener {
                    startActivity(
                        android.content.Intent(this@BriefingActivity, FollowUpActivity::class.java)
                            .putExtra("remarkMobile", p.mobile)
                    )
                }
            }
            row.addView(TextView(this).apply {
                // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে নিচের মোবাইল-লাইনের সাথে মিলে দুইবার দেখাত।
                text = p.name.ifBlank { "UNKNOWN" }
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#10223A"))
            })
            row.addView(TextView(this).apply {
                text = "${p.mobile} · ${p.branch} · Remark not written after the call"
                textSize = 11.5f
                setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                setPadding(0, dp(4), 0, 0)
            })
            box.addView(row)
            box.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            })
        }
    }

    private fun loadList() {
        binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        // TK-REQUESTED (2026-07-24): real cache-first -- raw rows saved
        // (SharedPreferences, same pattern as Trash/Payment/Doctor Visit)
        // after every successful fetch, shown instantly next time before
        // the fresh fetch below finishes.
        val cachePrefs = getSharedPreferences("piles_clinic_briefing_cache", MODE_PRIVATE)
        var hasCache = false
        /* 🟢🔒 V997 — জমা কাঁচা সারিগুলো নিচের স্মার্ট পড়াতেও লাগে
           (মিলে গেলে একটা সারিও নামে না), তাই এখানে ধরে রাখা হলো।
           ⛔ পাওয়া না গেলে `null` — তখন আগের মতোই পুরো পড়া। */
        var cachedRaw: org.json.JSONArray? = null
        try {
            val json = cachePrefs.getString("rows", null)
            if (!json.isNullOrBlank()) {
                cachedRaw = try { org.json.JSONArray(json) } catch (_: Throwable) { null }
                val cachedItems = repository.parseForUser(org.json.JSONArray(json), user)
                if (cachedItems.isNotEmpty()) {
                    hasCache = true
                    adapter.updateItems(cachedItems); refreshBulkBar()
                    binding.recyclerView.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
            }
        } catch (_: Throwable) { }
        if (!hasCache) {
            binding.tvEmpty.text = "Loading..."
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
        lifecycleScope.launch {
            val rawRows = try {
                withContext(Dispatchers.IO) { repository.fetchRawSmart(cachedRaw) }
            } catch (_: Throwable) { null }
            binding.progressLoad.visibility = View.GONE
            if (rawRows == null) {
                if (!hasCache) {
                    binding.tvEmpty.text = "No briefing / notice yet"
                    binding.tvEmpty.visibility = View.VISIBLE
                }
                return@launch
            }
            try { cachePrefs.edit().putString("rows", rawRows.toString()).apply() } catch (_: Throwable) { }
            // 🔵 TK-ORDER (07.08.2026): Master-এর জন্য **অ্যাকশন লাগে না এমন**
            // নোটিশ (Staff IN TIME/OUT TIME/সাধারণ নোটিশ) একবার দেখানোর পর নিজে
            // থেকেই সরে যায় — পরের বার Briefing খুললে আর দেখাবে না, Seen/Delete
            // চাপা লাগবে না। **অনুমতি লাগে এমন** নোটিশ (Refund/Delete/Reopen
            // request) থেকে যায় — যতক্ষণ না Master অনুমতি/অ্যাকশন দেন।
            // ⛔ শুধু Master role-এ, শুধু তাঁকে target-করা নোটিশে — Master-এর নিজের
            //    পাঠানো নোটিশ ছোঁয়া হয় না।
            // 📌 স্পষ্ট করে লিখে রাখা হলো (TK-এর অনুরোধে, স্পিড-টেস্ট ছবি সহ:
            //    ২৭.৯ Mbps ডাউনলোড / ৫৯.৩ Mbps আপলোড — **TK-এর ইন্টারনেট দ্রুত**):
            //    নোটিশ থেকে যাওয়ার আসল কারণ **নেট নয়, কোড** — আগে Seen-এ ডিলিট
            //    সফল হলো কিনা যাচাই হত না। এই অটো-ক্লিয়ারে প্রতিটা hide আলাদা করে
            //    cloud-এ লেখা হয়; ব্যর্থ হলে CloudWriteQueue (updateById) নিজে থেকে
            //    পরে সম্পন্ন করে। কখনো TK-এর ইন্টারনেটকে দোষ দেওয়া যাবে না।
            // 🔴 (07.08.2026) — এখন সব ব্যবহারকারীর জন্যই (Seen বোতাম তুলে দেওয়ায়)।
            autoClearActionlessForMaster(rawRows)
            // 🔴🔒 V1621 (২০.০৯.২০২৬, TK-নির্দেশ, স্পষ্ট) — "রাত ১২টা পার হলে
            // ক্লিয়ার হয়ে যাবে": Chamber Reopen-অনুরোধ (শুধু এই একটা ধরনই,
            // Refund/Delete/Leave নয়) যেদিন পাঠানো হয়েছিল সেদিনই যদি Master
            // কোনো সিদ্ধান্ত না নেন, পরের দিন Notice Board খুললেই সেটা নিজে
            // থেকে বন্ধ হয়ে যায় — চিরকাল "URGENT" হয়ে পড়ে থাকে না। প্রয়োজন
            // হলে স্টাফ আবার নতুন করে অনুরোধ পাঠাতে পারবেন।
            // ⛔ Refund/Delete/Leave request এই নিয়মে ছোঁয়া হয়নি — TK শুধু
            //    Reopen-অনুরোধের কথা বলেছেন।
            val staleReopenIds = expireStaleReopenRequests(rawRows)
            val effectiveRows = if (staleReopenIds.isEmpty()) rawRows else {
                val kept = org.json.JSONArray()
                for (i in 0 until rawRows.length()) {
                    val row = rawRows.optJSONObject(i) ?: continue
                    if (row.optString("id") !in staleReopenIds) kept.put(row)
                }
                kept
            }
            val items = repository.parseForUser(effectiveRows, user)
            if (items.isEmpty()) {
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.text = "No briefing / notice yet"
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.progressLoad.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.updateItems(items)
                binding.tvEmpty.visibility = View.GONE
            }
            // 🔵 (07.08.2026, নিজের অডিটে ধরা) — updateItems() আর-না-থাকা কার্ডের
            // বাছাই বাদ দেয়, তাই বারটাও সাথে সাথে হালনাগাদ করতে হয়; নইলে refresh-এর
            // পরে "৩ selected" লেখা বার ঝুলে থাকত, চাপলে কিছুই হতো না।
            refreshBulkBar()
        }
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Backdate Payment" workflow --
    // Master-only. Fetches pending requests and builds the UI directly
    // into backdateRequestsContainer (dynamic, same pattern already used
    // in PatientTimelineActivity.buildEnquiryHistoryTable). Container stays
    // GONE for any non-Master role or when there are zero pending requests
    // -- the existing Briefing list below is completely untouched either way.
    private fun loadBackdateRequests() {
        if (user.role != "master") {
            binding.backdateRequestsContainer.visibility = View.GONE
            return
        }
        // TK-REPORTED (2026-07-24): show a plain "Loading..." line right
        // away so this section is never silently blank on slow network --
        // cleared/replaced the instant fetch finishes (data or nothing).
        binding.backdateRequestsContainer.removeAllViews()
        binding.backdateRequestsContainer.visibility = View.VISIBLE
        binding.backdateRequestsContainer.addView(TextView(this).apply {
            text = "Loading..."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#8A93A6"))
            setPadding((16 * resources.displayMetrics.density).toInt(), (10 * resources.displayMetrics.density).toInt(), 0, 0)
        })
        lifecycleScope.launch {
            val requests = withContext(Dispatchers.IO) {
                try { PaymentRepository(this@BriefingActivity).fetchPendingBackdateRequests() } catch (_: Exception) { emptyList() }
            }
            val box = binding.backdateRequestsContainer
            box.removeAllViews()
            if (requests.isEmpty()) {
                box.visibility = View.GONE
                return@launch
            }
            box.visibility = View.VISIBLE
            val d = resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()
            val backdateTitle = "PENDING BACKDATE PAYMENTS (${requests.size})"
            val backdateRowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            box.addView(TextView(this@BriefingActivity).apply {
                text = "$backdateTitle  ▼"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#B42318"))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { val opening = backdateRowsBox.visibility != View.VISIBLE; backdateRowsBox.visibility = if (opening) View.VISIBLE else View.GONE; text = "$backdateTitle  ${if (opening) "▲" else "▼"}" }
            })
            for (req in requests) {
                val row = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
                // 🔴🔒 V1442 (১৩.০৯.২০২৬, TK-নির্দেশ) — payType=="refund" হলে
                // স্পষ্ট "💸 REFUND" ট্যাগ, নইলে Master ভুল করে ভাবতে পারেন
                // এটা টাকা-তোলার অনুরোধ (Approve চাপলে উল্টো — টাকা বেরোবে)।
                val isRefundReq = req.payType == "refund"
                row.addView(TextView(this@BriefingActivity).apply {
                    text = (if (isRefundReq) "💸 REFUND — " else "") +
                        "${req.name.ifBlank { req.mobile }} — ₹${"%,.0f".format(req.amount)} (${req.mode})"
                    textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(if (isRefundReq) android.graphics.Color.parseColor("#B42318") else android.graphics.Color.parseColor("#10223A"))
                })
                val requesterName = StaffDirectory.findAccount(req.requestedBy)?.name ?: req.requestedByName.ifBlank { req.requestedBy }
                row.addView(TextView(this@BriefingActivity).apply {
                    text = NoBengali.s("${if (isRefundReq) "ফেরতের প্রকৃত তারিখ" else "প্রকৃত জমা"}: ${DateUtil.display(req.requestedDate)} · অনুরোধ: $requesterName" + (if (isRefundReq && req.remarks.isNotBlank()) " · কারণ: ${req.remarks}" else ""))
                    textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                    setPadding(0, dp(4), 0, 0)
                })
                val btnRow = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(10), 0, 0)
                }
                val approveBtn = TextView(this@BriefingActivity).apply {
                    text = "✅ Approve"
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                    setOnClickListener {
                        /* 🔴🔒 V1443 (১৩.০৯.২০২৬, TK-নির্দেশ — তালিকা ৫৬৭) —
                           Refund-এর অনুরোধ Approve করাটাই টাকা বেরোনোর আসল
                           মুহূর্ত, তাই Master এখানেই সতর্কবার্তা পাবেন যদি এই
                           রোগীর আগে (যেকোনো দিনের) কোনো Refund থাকে।
                           ⛔ Treatment-অনুরোধে কিছু বদলায়নি। */
                        fun doApprove() {
                            lifecycleScope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    PaymentRepository(this@BriefingActivity).approveBackdateRequest(req, user.mobile)
                                }
                                Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Approved" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                                if (ok) loadBackdateRequests()
                            }
                        }
                        if (!isRefundReq) { doApprove(); return@setOnClickListener }
                        lifecycleScope.launch {
                            val digitsOnly = req.mobile.filter { it.isDigit() }.takeLast(10)
                            val prior = withContext(Dispatchers.IO) {
                                try {
                                    val repo = PaymentRepository(this@BriefingActivity)
                                    val p = repo.findPatientByMobile(digitsOnly)
                                    if (p != null) repo.priorRefundAnyDate(p, req.amount) else null
                                } catch (_: Throwable) { null }
                            }
                            if (prior == null) { doApprove(); return@launch }
                            val prevAmt = prior.optDouble("amount", 0.0)
                            val prevDate = prior.s("date")
                            AlertDialog.Builder(this@BriefingActivity)
                                .setCustomTitle(PremiumAlert.header(this@BriefingActivity, "⚠️ Already refunded before"))
                                .setMessage(
                                    "${req.name.ifBlank { req.mobile }} already has a refund of ₹${"%,.0f".format(prevAmt)} on ${DateUtil.display(prevDate)}." +
                                    "\n\nApproving this will take another ₹${"%,.0f".format(req.amount)} out of the collection.\n\n" +
                                    "Cancel = do nothing.  OK = approve anyway."
                                )
                                .setPositiveButton("OK, approve anyway") { _, _ -> doApprove() }
                                .setNegativeButton("Cancel", null)
                                .show().also { PremiumAlert.paint(it) }
                        }
                    }
                }
                val rejectBtn = TextView(this@BriefingActivity).apply {
                    text = "❌ Reject"
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.marginStart = dp(12)
                    layoutParams = lp
                    setOnClickListener {
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                PaymentRepository(this@BriefingActivity).rejectBackdateRequest(req.id, user.mobile)
                            }
                            Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Rejected" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                            if (ok) loadBackdateRequests()
                        }
                    }
                }
                btnRow.addView(approveBtn)
                btnRow.addView(rejectBtn)
                row.addView(btnRow)
                backdateRowsBox.addView(row)
                backdateRowsBox.addView(View(this@BriefingActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                })
            }
            box.addView(backdateRowsBox)
        }
    }

    // 🆕 B337 (03.08.2026, TK-নির্দেশ) — "Backdate Payment Permissions":
    // Master নির্দিষ্ট স্টাফের মোবাইল নম্বর + শুরু/শেষ তারিখ দিয়ে সাময়িক
    // অনুমতি দিতে পারেন — সেই সময়ের মধ্যে ওই স্টাফের প্রতিটা ব্যাকডেট
    // পেমেন্টে (তোলা/Edit/ডিলিট) আর আলাদা করে Master-এর অনুমোদনের অপেক্ষা
    // করতে হয় না (`BackdatePaymentGrant.kt`)। ⛔ পুরনো "Pending Backdate
    // Payment Requests" (উপরে) প্রতিটা-পেমেন্টে-আলাদা-অনুরোধ পথ এক অক্ষরও
    // বদলায়নি — এটা তার পাশাপাশি একটা বাড়তি, দ্রুত শর্টকাট।
    // loadBackdateRequests()-এর হুবহু একই দৃশ্য/আচরণ (collapsible, Master-only)।
    private fun loadBackdateGrants() {
        if (user.role != "master") {
            binding.backdateGrantsContainer.visibility = View.GONE
            return
        }
        binding.backdateGrantsContainer.removeAllViews()
        binding.backdateGrantsContainer.visibility = View.VISIBLE
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val box = binding.backdateGrantsContainer
        val grantsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        box.addView(TextView(this).apply {
            text = "BACKDATE PERMISSIONS  ▼"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            // 🎨🔒 V1141 (TK-অনুমোদিত ফটো-প্রুফ) — TK: *"ব্যাকডেট পারমিশনের কালার
            //    বেগুনি করুন"*। ⛔ শুধু রঙ; ভিতরের কিছুই বদলায়নি।
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#6A1B9A"))
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener {
                val opening = grantsBox.visibility != View.VISIBLE
                grantsBox.visibility = if (opening) View.VISIBLE else View.GONE
                text = "BACKDATE PERMISSIONS  ${if (opening) "▲" else "▼"}"
            }
        })

        // ── নতুন অনুমতি দেওয়ার ছোট ফর্ম ──
        val formCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        /* 📄🔒 V1177 (TK-অনুমোদিত ফটো-প্রুফ) — TK: *"Grant New
           Permission উপরে লেখার দরকার নেই"* (উপরের বেগুনি পট্টিতেই
           "BACKDATE PERMISSIONS" লেখা আছে)। ⛔ শুধু লেখাটা বাদ, ফর্ম অটুট। */
        fun field(hint: String): EditText = EditText(this).apply {
            this.hint = hint; textSize = 13f
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            // ⌨️🔒 V756 — এই ঘরগুলো পপ-আপ খোলার সময় বানানো হয়, তাই পর্দার
            //    পাহারা পৌঁছাত না। এখানেই বন্ধ করা হলো।
            try { NoAutofill.harden(this) } catch (_: Throwable) { }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(8)
            layoutParams = lp
        }
        // 🔴🎨🔒 TK-নির্দেশ (05.08.2026 — "স্টাফের লিস্ট আসতে হতো, মোবাইল
        // নাম্বার কেন টাইপ করব") — এই ঘরে আর টাইপ করা যাবে না, চাপলে
        // স্টাফের তালিকা (নাম · ব্রাঞ্চ · রোল) থেকে বাছা যায়, প্রজেক্টের
        // নিজস্ব প্রিমিয়াম পপ-আপ দিয়ে (`PremiumAlert`, নতুন কোনো ডিজাইন
        // বানানো হয়নি)। ⛔ Master নিজেকে বাদে বাকি সবাই (Staff/Doctor/
        // Field Officer) তালিকায় — মাস্টার নিজেকে ব্যাকডেট-অনুমতি দেওয়ার
        // দরকার নেই বলে বাদ। বাছলে ঘরে "নাম · মোবাইল" বসে — নিচের সেভ-
        // কোড আগের মতোই ঘরের লেখা থেকে **শেষ ১০টা অঙ্ক** বার করে নেয়
        // (`.filter{it.isDigit()}.takeLast(10)`), তাই এক অক্ষরও বদলাতে
        // হয়নি ওখানে।
        val staffMobileInput = field("Staff mobile number").apply {
            isFocusable = false
            isClickable = true
            inputType = android.text.InputType.TYPE_NULL
            setOnClickListener {
                // 🔴 TK-নির্দেশ (05.08.2026 রাত — "স্টাফের সাথে ডাক্তাররা কি করছে
                // ওখানে") — এখন শুধু "staff" রোলই এই তালিকায়, Doctor/Field
                // Officer/Master বাদ। ব্যাকডেট-পেমেন্ট পারমিশন শুধু স্টাফের জন্যই।
                val targetField = this
                /* 📋 V755 (২৭.০৮.২০২৬) — **অ্যাপ থেকে যোগ করা স্টাফও এই তালিকায়**।
                   ⚠️ V746-এ অ্যাপ থেকে স্টাফ যোগ করার সুবিধা এল, কিন্তু এই তালিকা
                      শুধু বাঁধা তালিকা দেখত ⇒ নতুন স্টাফ এখানে আসতই না।
                   ⛔ `cachedAccounts` কখনো নেটে যায় না (শুধু ফোনে জমানোটা)।
                   ⛔ একই মোবাইল দুবার এলে একবারই থাকে। */
                val builtIn = StaffDirectory.allAccounts().filter { it.role == "staff" }
                val fromCloud = try {
                    CloudStaffDirectory.cachedAccounts(applicationContext)
                        .filter { it.role == "staff" }
                } catch (_: Throwable) { emptyList() }
                val accounts = (builtIn + fromCloud)
                    .distinctBy { StaffDirectory.normalizeMobile(it.mobile) }
                    .sortedBy { it.name.uppercase() }
                if (accounts.isEmpty()) {
                    Toast.makeText(this@BriefingActivity, "No staff found", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                /* 🎨🔒 V755 (TK-অনুমোদিত ডিজাইন **C**, ডেমো ফটো দেখে বাছা) —
                   *"এটা এখনো কেন সাদা মিঠা, প্রফেশনাল বানাতে হবে"*
                   আগে সাধারণ `setItems()` ছিল — শুধু সাদা পটভূমিতে এক লাইন লেখা।
                   এখন: উপরে **খোঁজার ঘর**, তারপর প্রতি সারিতে সবুজ বিন্দু · নাম
                   (মোটা) · ডানে ব্রাঞ্চ · নিচে মোবাইল — মাঝে হালকা দাগ।
                   ⛔ ডিজাইনটা প্রজেক্টের **নিজের প্রমাণিত ধাঁচেই** (Saved RMP
                      পর্দার মতো: EditText + ListView + BaseAdapter), নতুন কোনো
                      লাইব্রেরি বা XML লাগেনি।
                   ⛔ বাছার পরে কী বসে (`নাম · মোবাইল`) — **এক অক্ষরও বদলায়নি**,
                      তাই সেভের পুরনো কোড (শেষ ১০ অঙ্ক) আগের মতোই চলে। */
                val d = resources.displayMetrics.density
                fun px(v: Int) = (v * d).toInt()
                val box = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(px(14), px(8), px(14), 0)
                    setBackgroundColor(android.graphics.Color.WHITE)
                }
                val search = EditText(this@BriefingActivity).apply {
                    hint = "Search staff or branch"
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    maxLines = 1
                    textSize = 14f
                    setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
                    setPadding(px(12), px(10), px(12), px(10))
                    try { NoAutofill.harden(this) } catch (_: Throwable) { }
                }
                val listView = android.widget.ListView(this@BriefingActivity).apply {
                    divider = android.graphics.drawable.ColorDrawable(
                        android.graphics.Color.parseColor("#EDF1F4"))
                    dividerHeight = 1
                    setBackgroundColor(android.graphics.Color.WHITE)
                    isFastScrollEnabled = accounts.size > 30
                }
                box.addView(search, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                box.addView(listView, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, px(360)).apply { topMargin = px(10) })

                var shown = accounts
                fun draw(query: String) {
                    val q = query.trim().lowercase(java.util.Locale.US)
                    shown = if (q.isBlank()) accounts else accounts.filter {
                        (it.name + " " + it.branch + " " + it.mobile).lowercase(java.util.Locale.US).contains(q)
                    }
                    listView.adapter = object : android.widget.BaseAdapter() {
                        override fun getCount(): Int = shown.size
                        override fun getItem(position: Int): Any = shown[position]
                        override fun getItemId(position: Int): Long = position.toLong()
                        override fun getView(
                            position: Int, convertView: View?, parent: android.view.ViewGroup?
                        ): View {
                            val acc = shown[position]
                            return LinearLayout(this@BriefingActivity).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(px(6), px(13), px(6), px(13))
                                addView(LinearLayout(this@BriefingActivity).apply {
                                    orientation = LinearLayout.HORIZONTAL
                                    gravity = android.view.Gravity.CENTER_VERTICAL
                                    addView(View(this@BriefingActivity).apply {
                                        background = android.graphics.drawable.GradientDrawable().apply {
                                            shape = android.graphics.drawable.GradientDrawable.OVAL
                                            setColor(android.graphics.Color.parseColor("#118452"))
                                        }
                                    }, LinearLayout.LayoutParams(px(8), px(8)).apply { marginEnd = px(9) })
                                    addView(TextView(this@BriefingActivity).apply {
                                        text = acc.name.trim().uppercase()
                                        textSize = 16f
                                        setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        setTextColor(android.graphics.Color.parseColor("#17312A"))
                                        maxLines = 1
                                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                                    addView(TextView(this@BriefingActivity).apply {
                                        text = acc.branch
                                        textSize = 12.5f
                                        setTextColor(android.graphics.Color.parseColor("#7A8A82"))
                                    })
                                })
                                addView(TextView(this@BriefingActivity).apply {
                                    text = acc.mobile
                                    textSize = 13.5f
                                    setTextColor(android.graphics.Color.parseColor("#5C6B64"))
                                    setPadding(px(17), px(3), 0, 0)
                                })
                            }
                        }
                    }
                }
                draw("")
                search.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: android.text.Editable) { draw(s.toString()) }
                })

                val dlg = AlertDialog.Builder(this@BriefingActivity)
                    .setCustomTitle(PremiumAlert.header(this@BriefingActivity, "SELECT STAFF"))
                    .setView(box)
                    .setNegativeButton("Close", null)
                    .create()
                listView.setOnItemClickListener { _, _, position, _ ->
                    val acc = shown.getOrNull(position) ?: return@setOnItemClickListener
                    // ⛔ যা বসে তা আগের মতোই — "নাম · মোবাইল"।
                    targetField.setText("${acc.name}  ·  ${acc.mobile}")
                    dlg.dismiss()
                }
                dlg.show()
                PremiumAlert.paint(dlg)
            }
        }
        /* 📄 V1177 — পাশাপাশি বসায় লেখা ছোট: "Start date" · "End date"। */
        val startDateInput = field("Start date").apply { isFocusable = false }
        val endDateInput = field("End date").apply { isFocusable = false }
        /* 📄🔒 V1177 (TK-নির্দেশ) — *"Reason এর দরকার নেই"*।
           ⛔ ঘরটা মোছা হয়নি (প্রকল্প-নিয়ম: নিজে থেকে কোড মোছা হয় না) — শুধু
              **দেখানো বন্ধ**, আর সেভে ফাঁকা লেখা যায়। নিচের `grantBackdate(...)`
              কল এক অক্ষরও বদলায়নি, তাই ভবিষ্যতে ফেরাতে চাইলে এক লাইনেই ফেরে। */
        val noteInput = field("Reason / note (optional)").apply { visibility = View.GONE }
        fun pickDateInto(target: EditText) {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, y, m, dd ->
                target.setText(String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, dd))
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
        startDateInput.setOnClickListener { pickDateInto(startDateInput) }
        endDateInput.setOnClickListener { pickDateInto(endDateInput) }
        formCard.addView(staffMobileInput)
        /* 📄🔒 V1177 (TK-নির্দেশ) — *"start date & End Date পাশাপাশি
           রাখুন"*: দুটো ঘর এক সারিতে, সমান চওড়া। ⛔ ঘরের id · ক্যালেন্ডার ·
           সেভ — কিচ্ছু বদলায়নি, শুধু বসার জায়গা। */
        formCard.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            addView(startDateInput, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(endDateInput, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(8) })
        })
        formCard.addView(noteInput)
        val grantBtn = TextView(this).apply {
            text = "✅ Grant Permission"
            textSize = 12.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
            setPadding(dp(18), dp(10), dp(18), dp(10))
            gravity = android.view.Gravity.CENTER
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener {
                val mobile = staffMobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
                val start = startDateInput.text.toString().trim()
                val end = endDateInput.text.toString().trim()
                if (mobile.length != 10) { Toast.makeText(this@BriefingActivity, "Enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (start.isBlank() || end.isBlank()) { Toast.makeText(this@BriefingActivity, "Pick start and end date", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (end < start) { Toast.makeText(this@BriefingActivity, "End date cannot be before start date", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val account = StaffDirectory.findAccount(mobile)
                if (account == null) { Toast.makeText(this@BriefingActivity, "No staff found with this mobile number", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                // 🔴🔒 V458 (20.08.2026, TK-নির্দেশ — "একবার অনুমতি দিলে দ্বিতীয়বার
                // আটকাবে না কেন, অন্তত সতর্কতা কেন দেবে না") — আগে এই বোতাম কখনোই
                // চেক করত না ওই স্টাফের ইতিমধ্যে সক্রিয় অনুমতি আছে কিনা, তাই বারবার
                // চাপলে নিঃশব্দে ডুপ্লিকেট জমত। এখন আগে যাচাই হয়, থাকলে TK-কে
                // জিজ্ঞাসা করে (Yes/Cancel) তবেই এগোয় — TK ইচ্ছা করলে (যেমন তারিখ
                // বাড়াতে) তবু নতুন করে দিতে পারবেন, ভুলবশত আবার চাপায় সতর্ক হবেন।
                fun doGrant() {
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            BackdatePaymentGrant.grant(
                                mobile, account.name, account.branch, start, end, noteInput.text.toString().trim(),
                                user.mobile, StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
                            )
                        }
                        Toast.makeText(this@BriefingActivity, if (ok) "Permission granted to ${account.name}" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                        if (ok) { staffMobileInput.setText(""); startDateInput.setText(""); endDateInput.setText(""); noteInput.setText(""); loadBackdateGrants() }
                    }
                }
                lifecycleScope.launch {
                    val existing = withContext(Dispatchers.IO) {
                        try {
                            BackdatePaymentGrant.listActive().filter {
                                it.optString("staffMobile").filter { c -> c.isDigit() }.takeLast(10) == mobile
                            }
                        } catch (_: Throwable) { emptyList() }
                    }
                    if (existing.isEmpty()) {
                        doGrant()
                    } else {
                        val g = existing.first()
                        val range = "${DateUtil.display(g.s("startDate"))} — ${DateUtil.display(g.s("endDate"))}"
                        AlertDialog.Builder(this@BriefingActivity)
                            .setCustomTitle(PremiumAlert.header(this@BriefingActivity, "ALREADY HAS PERMISSION"))
                            .setMessage("${account.name} already has an active permission ($range). Grant another one anyway?")
                            .setPositiveButton("Yes, Grant Anyway") { d, _ -> d.dismiss(); doGrant() }
                            .setNegativeButton("Cancel", null)
                            .show().also { PremiumAlert.paint(it) }
                    }
                }
            }
        }
        formCard.addView(grantBtn)
        grantsBox.addView(formCard)
        grantsBox.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        })

        // ── এই মুহূর্তে সক্রিয় অনুমতিগুলোর তালিকা ──
        val activeListBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        grantsBox.addView(activeListBox)
        activeListBox.addView(TextView(this).apply {
            text = "Loading active permissions..."
            textSize = 12f; setTextColor(android.graphics.Color.parseColor("#8A93A6"))
            setPadding(dp(16), dp(10), dp(16), dp(10))
        })
        lifecycleScope.launch {
            val grants = withContext(Dispatchers.IO) { BackdatePaymentGrant.listActive() }
            activeListBox.removeAllViews()
            if (grants.isEmpty()) {
                activeListBox.addView(TextView(this@BriefingActivity).apply {
                    text = "No active permissions right now."
                    textSize = 12f; setTextColor(android.graphics.Color.parseColor("#8A93A6"))
                    setPadding(dp(16), dp(10), dp(16), dp(10))
                })
            }
            for (g in grants) {
                val row = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.parseColor("#F7F5FC"))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                }
                row.addView(TextView(this@BriefingActivity).apply {
                    // 🔴🔒 V460 (20.08.2026, TK-অনুমোদিত null-টেক্সট অডিট, ধাপ ২):
                    // সরাসরি স্ক্রিনে দেখানো — `.s()` (JsonExt.kt) দিয়ে নিরাপদ করা হলো।
                    text = "${g.s("staffName").ifBlank { g.s("staffMobile") }} · ${g.s("branch")}"
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                })
                row.addView(TextView(this@BriefingActivity).apply {
                    text = "${DateUtil.display(g.s("startDate"))} — ${DateUtil.display(g.s("endDate"))} · granted by ${g.s("grantedByName").ifBlank { g.s("grantedBy") }}"
                    textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                    setPadding(0, dp(3), 0, 0)
                })
                if (g.s("note").isNotBlank()) {
                    row.addView(TextView(this@BriefingActivity).apply {
                        text = "Note: ${g.s("note")}"
                        textSize = 11.5f
                        setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                        setPadding(0, dp(2), 0, 0)
                    })
                }
                val revokeBtn = TextView(this@BriefingActivity).apply {
                    text = "🛑 Revoke"
                    textSize = 12f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                    setPadding(dp(16), dp(8), dp(16), dp(8))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = dp(8)
                    layoutParams = lp
                    setOnClickListener {
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) { BackdatePaymentGrant.revoke(g.optString("id"), user.mobile) }
                            Toast.makeText(this@BriefingActivity, if (ok) "Revoked" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                            if (ok) loadBackdateGrants()
                        }
                    }
                }
                row.addView(revokeBtn)
                activeListBox.addView(row)
                activeListBox.addView(View(this@BriefingActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                })
            }
        }
        box.addView(grantsBox)
    }

    /* 🔴🆕🔒 V439 (TK-রিপোর্ট ১৮.০৮.২০২৬, ছবিসহ — *"এখানে স্ক্রোল ও হয় না
       কোন কাজ ও হয় না"*)।
       **আসল কারণ:** এই পর্দার (`activity_briefing.xml`) আটটা খোপ একটা সাধারণ
       খাড়া LinearLayout-এ বসত, **কোনো ScrollView ছাড়াই**। কোনো খোপ খুললে
       ভিতরের ফর্ম + তালিকা মিলে পর্দার চেয়ে লম্বা হয়ে যেত, তাই **নিচের
       সারিগুলো পর্দার বাইরে চলে যেত** — আর স্ক্রোলও করা যেত না, ফলে ওদের
       বোতামে **আঙুলই পৌঁছাত না**। (TK-এর ছবিতে তৃতীয় সারির Revoke বোতামটা
       নিচে কাটা পড়েছে।)
       **সমাধান:** আটটা খোপ এখন একটাই ScrollView-এর ভিতরে (`panelsScroll`),
       আর তার উচ্চতা দরকারমতো — কিন্তু পর্দার ৬২%-এর বেশি নয়। তাই যত সারিই
       থাকুক, ভিতরে স্ক্রোল করে সবকটাতেই পৌঁছানো যায়, আর নিচের নোটিশ-তালিকাও
       আগের মতোই জায়গা পায়।
       ⛔ কোনো খোপের id/নিয়ম/চেহারা বদলায়নি — শুধু ওদের বাইরে একটা মোড়ক। */
    private fun clampPanelsHeight() {
        val sv = try { binding.panelsScroll } catch (_: Throwable) { return }
        sv.post {
            try {
                if (sv.childCount == 0) return@post
                val child = sv.getChildAt(0)
                val w = if (sv.width > 0) sv.width else resources.displayMetrics.widthPixels
                child.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                val want = child.measuredHeight
                val cap = (resources.displayMetrics.heightPixels * 0.62f).toInt()
                /* 🔴🔴🔒 V705 (২৬.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — কিষানগঞ্জের ফোনে
                   নোটিশ বোর্ডের উপরে **পর্দার প্রায় ২/৩ জুড়ে সাদা ফাঁকা**)।
                   **আসল কারণ (কোড ধরে যাচাই):** স্টাফের ফোনে উপরের আটটা খোপই
                   GONE (সবগুলোই Master-only বা pending-only), তাই `want` = ০।
                   কিন্তু পুরোনো লাইনটা ছিল `if (want in 1 until cap) want else cap` —
                   ০ ওই সীমার বাইরে পড়ায় `cap`-ই বসত, অর্থাৎ **ফাঁকা খোপগুলোকে
                   জোর করে পর্দার ৬২% উঁচু** করে দেওয়া হত। Master-এর ফোনে খোপ
                   থাকে বলে (want > 0) সেখানে চোখে পড়ত না — তাই এতদিন ধরা পড়েনি।
                   ⇒ কিছুই না থাকলে এখন আগের স্বাভাবিক `wrap_content`-এ ফেরত
                     (উচ্চতা ০), তাই তালিকা একদম উপর থেকেই শুরু হয়।
                   ⛔ `wrap_content` বসানো হচ্ছে, স্থির ০ নয় — নইলে ভিতরে নতুন
                      খোপ এলে ছেলেটার layout আর হত না, শোনার কাজটাও (
                      `installPanelsScrollClamp`) চালু হত না, খোপ চিরতরে
                      লুকিয়ে যেত।
                   ⛔ ভরা অবস্থার নিয়ম (দরকারমতো উচ্চতা, তবে ৬২%-এর বেশি নয়)
                      এক অক্ষরও বদলায়নি। */
                val target = when {
                    want <= 0 -> android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    want < cap -> want
                    else -> cap
                }
                val lp = sv.layoutParams
                // ⛔ একই মান আবার বসালে layout-এর অসীম চক্র তৈরি হতে পারত,
                //    তাই সত্যিই বদলালে তবেই বসানো হয়।
                if (lp.height != target) { lp.height = target; sv.layoutParams = lp }
            } catch (_: Throwable) { }
        }
    }

    /** খোপ খোলা/বন্ধ হলে বা নতুন সারি এলে উচ্চতাটা নিজে থেকেই ঠিক হয়ে যায় —
     *  তাই প্রতিটা খোপে আলাদা করে কিছু বসাতে হয়নি (কোনো পুরনো কোড ছোঁয়া হয়নি)। */
    private fun installPanelsScrollClamp() {
        try {
            val sv = binding.panelsScroll
            if (sv.childCount == 0) return
            sv.getChildAt(0).addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                if (bottom - top != oldBottom - oldTop) clampPanelsHeight()
            }
            clampPanelsHeight()
        } catch (_: Throwable) { }
    }

    // V216 (§13, 2026-07-31): "Refund / টাকা ফেরত" — Master-only pending-approval
    // section, loadBackdateRequests()-এর হুবহু একই দৃশ্য/আচরণ। refund request
    // একটা "payments" row (payType=refund, refundApprovalStatus=pending)। Approve
    // করলে সেই row-এর status=approved হয় (তখনই collection/paid থেকে কমে), Reject
    // করলে status=rejected (হিসাবে প্রভাব নেই, history-তে থাকে)।
    private fun loadRefundRequests() {
        if (user.role != "master") {
            binding.refundRequestsContainer.visibility = View.GONE
            return
        }
        binding.refundRequestsContainer.removeAllViews()
        binding.refundRequestsContainer.visibility = View.VISIBLE
        binding.refundRequestsContainer.addView(TextView(this).apply {
            text = "Loading..."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#8A93A6"))
            setPadding((16 * resources.displayMetrics.density).toInt(), (10 * resources.displayMetrics.density).toInt(), 0, 0)
        })
        lifecycleScope.launch {
            val requests = withContext(Dispatchers.IO) {
                try { PaymentRepository(this@BriefingActivity).fetchPendingRefundRequests() } catch (_: Exception) { emptyList() }
            }
            val box = binding.refundRequestsContainer
            box.removeAllViews()
            if (requests.isEmpty()) { box.visibility = View.GONE; return@launch }
            box.visibility = View.VISIBLE
            val d = resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()
            val refundTitle = "PENDING REFUND REQUESTS (${requests.size})"
            val refundRowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            box.addView(TextView(this@BriefingActivity).apply {
                text = "$refundTitle  ▼"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#B42318"))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { val opening = refundRowsBox.visibility != View.VISIBLE; refundRowsBox.visibility = if (opening) View.VISIBLE else View.GONE; text = "$refundTitle  ${if (opening) "▲" else "▼"}" }
            })
            for (req in requests) {
                val refundId = req.s("id")
                val amount = req.optDouble("amount", 0.0)
                val mode = req.s("mode").ifBlank { "CASH" }
                val name = req.s("name").ifBlank { req.s("mobile") }
                val reason = req.s("refundReason").ifBlank { req.s("remarks") }
                val requestedBy = req.s("refundRequestedBy").ifBlank { req.s("createdBy") }
                val row = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
                row.addView(TextView(this@BriefingActivity).apply {
                    text = "$name — ₹${"%,.0f".format(amount)} ($mode)"
                    textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                })
                val requesterName = StaffDirectory.findAccount(requestedBy)?.name ?: requestedBy
                row.addView(TextView(this@BriefingActivity).apply {
                    text = NoBengali.s("কারণ: ${reason.ifBlank { "—" }} · অনুরোধ: $requesterName")
                    textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                    setPadding(0, dp(4), 0, 0)
                })
                val btnRow = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(10), 0, 0)
                }
                val approveBtn = TextView(this@BriefingActivity).apply {
                    text = "✅ Approve"
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                }
                val rejectBtn = TextView(this@BriefingActivity).apply {
                    text = "❌ Reject"
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.marginStart = dp(12)
                    layoutParams = lp
                }
                // 🔴 B414 (04.08.2026) — এই পুরনো dropdown-ভিত্তিক Approve/Reject
                // পথও (B281-এর আগে থেকে ছিল) একই ডাবল-চাপ ঝুঁকিতে ছিল — এখন
                // চাপার সাথে সাথে দুটোই বন্ধ হয়ে যায়।
                approveBtn.setOnClickListener {
                    approveBtn.isEnabled = false; rejectBtn.isEnabled = false
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            PaymentRepository(this@BriefingActivity).approveRefund(refundId, user.mobile)
                        }
                        Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Refund Approved" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                        if (ok) loadRefundRequests() else { approveBtn.isEnabled = true; rejectBtn.isEnabled = true }
                    }
                }
                rejectBtn.setOnClickListener {
                    approveBtn.isEnabled = false; rejectBtn.isEnabled = false
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            PaymentRepository(this@BriefingActivity).rejectRefund(refundId, user.mobile)
                        }
                        Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Refund Rejected" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                        if (ok) loadRefundRequests() else { approveBtn.isEnabled = true; rejectBtn.isEnabled = true }
                    }
                }
                btnRow.addView(approveBtn)
                btnRow.addView(rejectBtn)
                row.addView(btnRow)
                refundRowsBox.addView(row)
                refundRowsBox.addView(View(this@BriefingActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                })
            }
            box.addView(refundRowsBox)
        }
    }

    // 🔵🔒 B618 (11.08.2026, TK-নির্দেশ): master/ব্রাঞ্চ-ডাক্তারের অপেক্ষমাণ
    // ছুটি-অনুরোধ — Refund তালিকার হুবহু ধরন (loadRefundRequests-এর নকল)।
    // ⛔ wn.leave_requests পড়তে/লিখতে module-লগইন লাগে, তাই ensureSignedIn-এর
    // ভিতরে। অন্য কোনো সেকশন/আচরণ ছোঁয়া হয়নি — নতুন container-এ আঁকা।
    /**
     * 🏠🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"সে যদি Work from Home
     * করতে চায় তার ব্যাবস্থা যেন থাকে এবং মাস্টারের অনুমতি নেওয়া জরুরী"*।
     *
     * `loadLeaveRequests()`-এর হুবহু একই ধরন (নতুন কোনো ছাঁদ তৈরি করা হয়নি) —
     * শুধু নিজের container-এ আঁকা, তাই ছুটির সেকশনটা এক অক্ষরও বদলায়নি।
     * ⛔ শুধু **আজকের** অপেক্ষমাণ অনুরোধ দেখায় — Work From Home একদিনের অনুমতি।
     */
    private fun loadWfhRequests() {
        val canApprove = user.role == "master" || user.displayRole == "doctor"
        if (!canApprove) { binding.wfhRequestsContainer.visibility = View.GONE; return }
        lifecycleScope.launch {
            val requests = withContext(Dispatchers.IO) {
                try { com.tkbiswas.pilesclinic.native.WfhRequests.pendingToday() }
                catch (_: Exception) { emptyList<org.json.JSONObject>() }
            }
            val box = binding.wfhRequestsContainer
            box.removeAllViews()
            if (requests.isEmpty()) { box.visibility = View.GONE; return@launch }
            box.visibility = View.VISIBLE
            val d = resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()
            val wfhTitle = "PENDING WORK FROM HOME REQUESTS (${requests.size})"
            val wfhRowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            box.addView(TextView(this@BriefingActivity).apply {
                text = "$wfhTitle  ▼"; textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#B42318"))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { val opening = wfhRowsBox.visibility != View.VISIBLE; wfhRowsBox.visibility = if (opening) View.VISIBLE else View.GONE; text = "$wfhTitle  ${if (opening) "▲" else "▼"}" }
            })
            for (req in requests) {
                val staffName = req.optString("staffName", "").ifBlank {
                    req.optString("staffCode", "").ifBlank { req.optString("staffMobile", "") }
                }
                val branch = req.optString("branch", "")
                val date = req.optString("workDate", "")
                val reason = req.optString("reason", "").ifBlank { "—" }
                val dotted = try { val p = date.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (_: Throwable) { date }
                val row = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
                row.addView(TextView(this@BriefingActivity).apply {
                    text = "$staffName · $branch"; textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                })
                row.addView(TextView(this@BriefingActivity).apply {
                    text = "Date: $dotted · Reason: $reason"; textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                    setPadding(0, dp(4), 0, 0)
                })
                val btnRow = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, 0) }
                val approveBtn = TextView(this@BriefingActivity).apply {
                    text = "✅ Approve"; textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                }
                val rejectBtn = TextView(this@BriefingActivity).apply {
                    text = "❌ Reject"; textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.marginStart = dp(12); layoutParams = lp
                }
                fun decide(approve: Boolean) {
                    approveBtn.isEnabled = false; rejectBtn.isEnabled = false
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            com.tkbiswas.pilesclinic.native.WfhRequests.decide(
                                this@BriefingActivity, req, approve, user.mobile, user.name.ifBlank { user.mobile }
                            )
                        }
                        Toast.makeText(this@BriefingActivity,
                            if (ok) (if (approve) "Work From Home Approved" else "Work From Home Rejected")
                            else "Failed — check net", Toast.LENGTH_SHORT).show()
                        if (ok) loadWfhRequests() else { approveBtn.isEnabled = true; rejectBtn.isEnabled = true }
                    }
                }
                approveBtn.setOnClickListener { decide(true) }
                rejectBtn.setOnClickListener { decide(false) }
                btnRow.addView(approveBtn); btnRow.addView(rejectBtn)
                row.addView(btnRow)
                wfhRowsBox.addView(row)
                wfhRowsBox.addView(View(this@BriefingActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                })
            }
            box.addView(wfhRowsBox)
        }
    }

    private fun loadLeaveRequests() {
        val canApprove = user.role == "master" || user.displayRole == "doctor"
        if (!canApprove) { binding.leaveRequestsContainer.visibility = View.GONE; return }
        com.tkbiswas.pilesclinic.modules.ModuleUi.ensureSignedIn(this, user.name.ifBlank { user.mobile }) {
            binding.leaveRequestsContainer.removeAllViews()
            binding.leaveRequestsContainer.visibility = View.VISIBLE
            val dd = resources.displayMetrics.density
            binding.leaveRequestsContainer.addView(TextView(this).apply {
                text = "Loading..."; textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#8A93A6"))
                setPadding((16 * dd).toInt(), (10 * dd).toInt(), 0, 0)
            })
            lifecycleScope.launch {
                val requests = withContext(Dispatchers.IO) {
                    try { LeaveRepository.fetchPending(user) } catch (_: Exception) { org.json.JSONArray() }
                }
                val box = binding.leaveRequestsContainer
                box.removeAllViews()
                if (requests.length() == 0) { box.visibility = View.GONE; return@launch }
                box.visibility = View.VISIBLE
                val d = resources.displayMetrics.density
                fun dp(v: Int) = (v * d).toInt()
                val leaveTitle = "PENDING LEAVE REQUESTS (${requests.length()})"
                val leaveRowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
                box.addView(TextView(this@BriefingActivity).apply {
                    text = "$leaveTitle  ▼"; textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#B42318"))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    setOnClickListener { val opening = leaveRowsBox.visibility != View.VISIBLE; leaveRowsBox.visibility = if (opening) View.VISIBLE else View.GONE; text = "$leaveTitle  ${if (opening) "▲" else "▼"}" }
                })
                for (k in 0 until requests.length()) {
                    val req = requests.getJSONObject(k)
                    val staffName = req.s("staff_name").ifBlank { req.s("staff_code").ifBlank { req.s("staff_mobile") } }
                    val branch = req.s("branch")
                    val date = req.s("leave_date")
                    val reason = req.s("reason").ifBlank { "—" }
                    /* 🏖️🔒 V740 (২৭.০৮.২০২৬) — কারণটা মাস্টারের পড়ার মতো ইংরেজিতে।
                       আগে সাংকেতিক লেখা যেত ("5th+conflict")। ⛔ ডেটাবেসে যা লেখা
                       আছে তা এক অক্ষরও বদলায়নি — শুধু দেখানোর ভাষা। */
                    val needRaw = req.s("need_reason")
                    val need = if (needRaw.isBlank()) "-" else buildList {
                        if (needRaw.contains("chamber")) add("Chamber day")
                        if (needRaw.contains("conflict")) add("Colleague on leave")
                        if (needRaw.contains("5th")) add("5th day this month")
                    }.joinToString(" + ").ifBlank { needRaw }
                    val dotted = try { val p = date.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (_: Throwable) { date }
                    /* 🕒🔒 V1510 (TK-রিপোর্ট, ১৬.০৯.২০২৬) — কখন আবেদন করা হয়েছে
                       (created_at) সেটাও দেখানো হচ্ছে, শুধু ছুটির দিনটা (leave_date)
                       নয় — TK-র চেম্বার-টাইমের ভিতরে না বাইরে এসেছে সেটা যাচাই করে
                       তবেই Approve/Reject করার জন্য এটা জরুরি। RmpCommissionSheetActivity/
                       DoctorVisitActivity-র প্রমাণিত timeIst() নিয়ম — UTC অফসেট ধরে
                       পার্স করে Asia/Kolkata-তে দেখানো হয়। */
                    val appliedTxt = try {
                        val raw = req.s("created_at").trim()
                        val m = Regex("^(\\d{4}-\\d{2}-\\d{2})[T ](\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$").find(raw)
                        if (m == null) "" else {
                            val f = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", java.util.Locale.US)
                            val off = m.groupValues[3].let { if (it.isBlank() || it == "Z") "+0000" else it.replace(":", "") }
                            val d = f.parse(m.groupValues[1] + " " + m.groupValues[2] + off)
                            val outDate = java.text.SimpleDateFormat("d/M", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
                            val outTime = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }
                            if (d == null) "" else "Applied: ${outDate.format(d)}, ${outTime.format(d)}"
                        }
                    } catch (_: Throwable) { "" }
                    val row = LinearLayout(this@BriefingActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(android.graphics.Color.WHITE)
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                    }
                    row.addView(TextView(this@BriefingActivity).apply {
                        text = "$staffName · $branch"; textSize = 13.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#10223A"))
                    })
                    row.addView(TextView(this@BriefingActivity).apply {
                        text = "Date: $dotted · Reason: $reason · ($need)"; textSize = 11.5f
                        setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                        setPadding(0, dp(4), 0, 0)
                    })
                    if (appliedTxt.isNotBlank()) row.addView(TextView(this@BriefingActivity).apply {
                        text = appliedTxt; textSize = 11f
                        setTextColor(android.graphics.Color.parseColor("#8A93A6"))
                        setPadding(0, dp(2), 0, 0)
                    })
                    val btnRow = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, 0) }
                    val approveBtn = TextView(this@BriefingActivity).apply {
                        text = "✅ Approve"; textSize = 12.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                    }
                    val rejectBtn = TextView(this@BriefingActivity).apply {
                        text = "❌ Reject"; textSize = 12.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.marginStart = dp(12); layoutParams = lp
                    }
                    approveBtn.setOnClickListener {
                        approveBtn.isEnabled = false; rejectBtn.isEnabled = false
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) { LeaveRepository.approve(this@BriefingActivity, req, user.mobile) }
                            Toast.makeText(this@BriefingActivity, if (ok) "Leave Approved" else "Failed — check net", Toast.LENGTH_SHORT).show()
                            if (ok) loadLeaveRequests() else { approveBtn.isEnabled = true; rejectBtn.isEnabled = true }
                        }
                    }
                    rejectBtn.setOnClickListener {
                        approveBtn.isEnabled = false; rejectBtn.isEnabled = false
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) { LeaveRepository.reject(this@BriefingActivity, req, user.mobile) }
                            Toast.makeText(this@BriefingActivity, if (ok) "Leave Rejected" else "Failed — check net", Toast.LENGTH_SHORT).show()
                            if (ok) loadLeaveRequests() else { approveBtn.isEnabled = true; rejectBtn.isEnabled = true }
                        }
                    }
                    btnRow.addView(approveBtn); btnRow.addView(rejectBtn)
                    row.addView(btnRow)
                    leaveRowsBox.addView(row)
                    leaveRowsBox.addView(View(this@BriefingActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                        setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    })
                }
                box.addView(leaveRowsBox)
            }
        }
    }

    // TK-REQUESTED ADDITION (2026-07-25): "Edit Payment Amount" workflow --
    /** 🟢 B629 (11.08.2026, TK-নির্দেশ): Master ও Doctor-এর "Salary Due" মনে করানো।
     *  বিদ্যমান salary_config+salary_payments পড়ে যাদের এ মাসের স্যালারি এখনো বাকি
     *  (তারিখ পেরিয়ে গেছে) তাদের দেখায়। Master চাপলে সরাসরি সেই স্টাফের Salary পর্দা
     *  খোলে (Pay করতে); Doctor শুধু দেখতে পায়। ⛔ নতুন টেবিল নেই; পুরনো কিছু ভাঙে না। */
    private fun loadSalaryDue() {
        val isMaster = user.role == "master"
        val isDoctor = user.displayRole == "doctor"
        if (!isMaster && !isDoctor) { binding.salaryDueContainer.visibility = View.GONE; return }
        binding.salaryDueContainer.removeAllViews()
        binding.salaryDueContainer.visibility = View.VISIBLE
        binding.salaryDueContainer.addView(TextView(this).apply {
            text = "Loading..."; textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#8A93A6"))
            setPadding((16 * resources.displayMetrics.density).toInt(), (10 * resources.displayMetrics.density).toInt(), 0, 0)
        })
        lifecycleScope.launch {
            val due = withContext(Dispatchers.IO) {
                try { com.tkbiswas.pilesclinic.modules.SalaryReminder.dueList(this@BriefingActivity) } catch (_: Exception) { emptyList() }
            }
            val box = binding.salaryDueContainer
            box.removeAllViews()
            if (due.isEmpty()) { box.visibility = View.GONE; return@launch }
            /* 🔔🔒 V1164 (০৭.০৯.২০২৬, TK-নির্দেশ: *"Salary Due সব সময় কেন থাকবে?
               দিনে একবার দেখালো ঠিক আছে চলবে, কিন্তু সারা জীবন কেন থেকে যায়"*) —
               পট্টিটা এখন **দিনে একবারই** ওঠে। একবার দেখানো হয়ে গেলে ওই দিনের
               মতো আর ওঠে না; পরদিন আবার ওঠে।
               ⛔ **নতুন কেউ বাকির তালিকায় ঢুকলে সঙ্গে সঙ্গেই আবার ওঠে** — সংখ্যাটাও
                  মনে রাখা হয়, তাই নতুন বাকি কখনো চাপা পড়ে না।
               ⛔ টাকার কোনো হিসাব · তালিকা · Pay Salary-র পথ কিচ্ছু বদলায়নি —
                  শুধু কবে দেখানো হবে সেটুকু।
               ⛔ চিহ্নটা ফোনের ভিতরেই থাকে (SharedPreferences), ক্লাউডে কিছু যায় না। */
            run {
                val today = FollowUpModel.today()
                val pref = getSharedPreferences("piles_salary_due_banner", MODE_PRIVATE)
                val seenDate = pref.getString("date", "").orEmpty()
                val seenCount = pref.getInt("count", 0)
                // আজ দেখানো হয়ে গেছে **আর** নতুন কেউ যোগ হয়নি ⇒ আজ আর নয়।
                if (seenDate == today && due.size <= seenCount) { box.visibility = View.GONE; return@launch }
                pref.edit().putString("date", today).putInt("count", due.size).apply()
            }
            box.visibility = View.VISIBLE
            val d = resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()
            val title = "SALARY DUE (${due.size})"
            val rowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            box.addView(TextView(this@BriefingActivity).apply {
                text = "$title  ▼"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                // 🎨🔒 V1141 (০৬.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — TK: *"Salary Due-তে
                //    গ্রীন কালার সরিয়ে হলুদ কালার দিন"*। ⛔ শুধু রঙ; তালিকা · গোনা ·
                //    চাপ কিচ্ছু বদলায়নি।
                setTextColor(android.graphics.Color.parseColor("#3D2B00"))
                setBackgroundColor(android.graphics.Color.parseColor("#F0B429"))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { val opening = rowsBox.visibility != View.VISIBLE; rowsBox.visibility = if (opening) View.VISIBLE else View.GONE; text = "$title  ${if (opening) "▲" else "▼"}" }
            })
            for (item in due) {
                val row = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
                row.addView(TextView(this@BriefingActivity).apply {
                    text = item.name + (if (item.branch.isNotBlank()) " · " + item.branch else "")
                    textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                })
                row.addView(TextView(this@BriefingActivity).apply {
                    // 💰 V1198 — কোন মাসের বেতন, সেটা এখন লেখাতেই থাকে
                    text = "Salary day " + item.salaryDay + " · " +
                        (if (item.forMonthLabel.isNotBlank()) item.forMonthLabel + " due" else "due") +
                        " · ₹" + "%,.0f".format(item.amount)
                    textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#B42318"))
                    setPadding(0, dp(4), 0, 0)
                })
                if (isMaster) {
                    val payBtn = TextView(this@BriefingActivity).apply {
                        text = "➕ Pay Salary"
                        textSize = 12.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.topMargin = dp(10)
                        layoutParams = lp
                        setOnClickListener {
                            val it2 = android.content.Intent(this@BriefingActivity, com.tkbiswas.pilesclinic.modules.StaffProfileActivity::class.java)
                            it2.putExtra("salaryFor", item.code)
                            startActivity(it2)
                        }
                    }
                    row.addView(payBtn)
                }
                rowsBox.addView(row)
                rowsBox.addView(View(this@BriefingActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                })
            }
            box.addView(rowsBox)
        }
    }

    // Master-only, exact same dynamic-build pattern as
    // loadBackdateRequests() just above, into its own container.
    private fun loadEditRequests() {
        if (user.role != "master") {
            binding.editRequestsContainer.visibility = View.GONE
            return
        }
        binding.editRequestsContainer.removeAllViews()
        binding.editRequestsContainer.visibility = View.VISIBLE
        binding.editRequestsContainer.addView(TextView(this).apply {
            text = "Loading..."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#8A93A6"))
            setPadding((16 * resources.displayMetrics.density).toInt(), (10 * resources.displayMetrics.density).toInt(), 0, 0)
        })
        lifecycleScope.launch {
            val requests = withContext(Dispatchers.IO) {
                try { PaymentRepository(this@BriefingActivity).fetchPendingEditRequests() } catch (_: Exception) { emptyList() }
            }
            // 🟢 B628 (11.08.2026): Referral Income এডিট/ডিলিটের অনুরোধও এই একই জায়গায়
            //   মাস্টার দেখে/Approve করে (payment edit-এর হুবহু পাশে)।
            val refRequests = withContext(Dispatchers.IO) {
                try { DoctorVisitRepository().fetchPendingReferralEditRequests() } catch (_: Exception) { emptyList() }
            }
            /* 🟢🆕 V401 (16.08.2026, TK-নির্দেশ): Staff/Doctor-এর "পুরনো তারিখের আয়-খরচ"
               অনুরোধও ঠিক এখানেই — Payment Edit ও Referral Edit-এর পাশে। ⛔ ওই দুটোর
               কোড এক অক্ষরও বদলায়নি; শুধু একটা নতুন সেকশন যোগ হলো। */
            val ieRequests = withContext(Dispatchers.IO) {
                try { com.tkbiswas.pilesclinic.modules.IeRequests.fetchPending() } catch (_: Exception) { org.json.JSONArray() }
            }
            val box = binding.editRequestsContainer
            box.removeAllViews()
            if (requests.isEmpty() && refRequests.isEmpty() && ieRequests.length() == 0) {
                box.visibility = View.GONE
                return@launch
            }
            box.visibility = View.VISIBLE
            val d = resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()

            if (ieRequests.length() > 0) {
                val ieTitle = "Pending Income & Expense Requests (${ieRequests.length()})"
                val ieRowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
                box.addView(TextView(this@BriefingActivity).apply {
                    text = "$ieTitle  ▼"
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#0B4F2A"))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    setOnClickListener {
                        val opening = ieRowsBox.visibility != View.VISIBLE
                        ieRowsBox.visibility = if (opening) View.VISIBLE else View.GONE
                        text = "$ieTitle  ${if (opening) "▲" else "▼"}"
                    }
                })
                for (i in 0 until ieRequests.length()) {
                    val req = ieRequests.getJSONObject(i)
                    val reqId = req.optString("id", "")
                    val row = LinearLayout(this@BriefingActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(android.graphics.Color.WHITE)
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                    }
                    row.addView(TextView(this@BriefingActivity).apply {
                        text = com.tkbiswas.pilesclinic.modules.IeRequests.describe(
                            req,
                            { n -> "₹" + "%,.0f".format(n) },
                            // 🔴🔒 V936 (TK-নির্দেশ — এক ফরম্যাট): স্ল্যাশ ছিল, এখন বিন্দু।
                            { iso -> com.tkbiswas.pilesclinic.native.DateUtil.display(iso) }
                        )
                        textSize = 13f
                        setTextColor(android.graphics.Color.parseColor("#10223A"))
                    })
                    val ieBtnRow = LinearLayout(this@BriefingActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(10), 0, 0)
                    }
                    ieBtnRow.addView(TextView(this@BriefingActivity).apply {
                        text = "Approve"
                        textSize = 12.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        setOnClickListener {
                            lifecycleScope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    com.tkbiswas.pilesclinic.modules.IeRequests.decide(reqId, true)
                                }
                                Toast.makeText(this@BriefingActivity,
                                    if (res.ok) "Approved" else res.message.ifBlank { "Failed - check network" },
                                    Toast.LENGTH_SHORT).show()
                                if (res.ok) loadEditRequests()
                            }
                        }
                    })
                    ieBtnRow.addView(TextView(this@BriefingActivity).apply {
                        text = "Reject"
                        textSize = 12.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { marginStart = dp(12) }
                        setOnClickListener {
                            lifecycleScope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    com.tkbiswas.pilesclinic.modules.IeRequests.decide(reqId, false)
                                }
                                Toast.makeText(this@BriefingActivity,
                                    if (res.ok) "Rejected" else res.message.ifBlank { "Failed - check network" },
                                    Toast.LENGTH_SHORT).show()
                                if (res.ok) loadEditRequests()
                            }
                        }
                    })
                    row.addView(ieBtnRow)
                    ieRowsBox.addView(row)
                    ieRowsBox.addView(View(this@BriefingActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                        setBackgroundColor(android.graphics.Color.parseColor("#E7ECF3"))
                    })
                }
                box.addView(ieRowsBox)
            }

            if (requests.isNotEmpty()) {
            val editTitle = "PENDING PAYMENT EDIT REQUESTS (${requests.size})"
            val editRowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            box.addView(TextView(this@BriefingActivity).apply {
                text = "$editTitle  ▼"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#B42318"))
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { val opening = editRowsBox.visibility != View.VISIBLE; editRowsBox.visibility = if (opening) View.VISIBLE else View.GONE; text = "$editTitle  ${if (opening) "▲" else "▼"}" }
            })
            for (req in requests) {
                val row = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                }
                row.addView(TextView(this@BriefingActivity).apply {
                    text = "${req.name.ifBlank { req.mobile }} — ₹${"%,.0f".format(req.oldAmount)} → ₹${"%,.0f".format(req.newAmount)} (${req.mode})"
                    textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                })
                val requesterName = StaffDirectory.findAccount(req.requestedBy)?.name ?: req.requestedByName.ifBlank { req.requestedBy }
                row.addView(TextView(this@BriefingActivity).apply {
                    text = NoBengali.s("Payment তারিখ: ${DateUtil.display(req.paymentDate)} · অনুরোধ: $requesterName" + (if (req.reason.isNotBlank()) " · কারণ: ${req.reason}" else ""))
                    textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                    setPadding(0, dp(4), 0, 0)
                })
                val btnRow = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(10), 0, 0)
                }
                val approveBtn = TextView(this@BriefingActivity).apply {
                    text = "✅ Approve"
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                    setOnClickListener {
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                PaymentRepository(this@BriefingActivity).approvePaymentEditRequest(req, user.mobile)
                            }
                            Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Approved" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                            if (ok) loadEditRequests()
                        }
                    }
                }
                val rejectBtn = TextView(this@BriefingActivity).apply {
                    text = "❌ Reject"
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                    setPadding(dp(18), dp(10), dp(18), dp(10))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.marginStart = dp(12)
                    layoutParams = lp
                    setOnClickListener {
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                PaymentRepository(this@BriefingActivity).rejectPaymentEditRequest(req.id, user.mobile)
                            }
                            Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Rejected" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                            if (ok) loadEditRequests()
                        }
                    }
                }
                btnRow.addView(approveBtn)
                btnRow.addView(rejectBtn)
                row.addView(btnRow)
                editRowsBox.addView(row)
                editRowsBox.addView(View(this@BriefingActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                })
            }
            box.addView(editRowsBox)
            }  // 🟢 B628: payment section শেষ
            // 🟢 B628: Referral Income এডিট/ডিলিটের অনুরোধ — মাস্টার Approve/Reject।
            if (refRequests.isNotEmpty()) {
                val refTitle = "🔒 Pending Referral Edit Requests (${refRequests.size})"
                val refRowsBox = LinearLayout(this@BriefingActivity).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
                box.addView(TextView(this@BriefingActivity).apply {
                    text = "$refTitle  ▼"
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundColor(android.graphics.Color.parseColor("#8A6D0B"))
                    setPadding(dp(16), dp(12), dp(16), dp(12))
                    setOnClickListener { val opening = refRowsBox.visibility != View.VISIBLE; refRowsBox.visibility = if (opening) View.VISIBLE else View.GONE; text = "$refTitle  ${if (opening) "▲" else "▼"}" }
                })
                for (req in refRequests) {
                    val row = LinearLayout(this@BriefingActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(android.graphics.Color.WHITE)
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                    }
                    val changeText = if (req.isDelete)
                        "DELETE — ₹${"%,.0f".format(req.oldAmount)} (${req.oldStatus})"
                    else
                        "₹${"%,.0f".format(req.oldAmount)} (${req.oldStatus}) → ₹${"%,.0f".format(req.newAmount)} (${req.newStatus})"
                    row.addView(TextView(this@BriefingActivity).apply {
                        text = "${req.docName.ifBlank { req.docMobile }} · ${req.patient} — $changeText"
                        textSize = 13.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#10223A"))
                    })
                    val requesterName = StaffDirectory.findAccount(req.requestedBy)?.name ?: req.requestedByName.ifBlank { req.requestedBy }
                    row.addView(TextView(this@BriefingActivity).apply {
                        text = NoBengali.s("Referral Income · অনুরোধ: $requesterName")
                        textSize = 11.5f
                        setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                        setPadding(0, dp(4), 0, 0)
                    })
                    val btnRow = LinearLayout(this@BriefingActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(10), 0, 0)
                    }
                    val approveBtn = TextView(this@BriefingActivity).apply {
                        text = "✅ Approve"
                        textSize = 12.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#0C9E33"))
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        setOnClickListener {
                            lifecycleScope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    DoctorVisitRepository().approveReferralEditRequest(req, user.mobile, applicationContext)
                                }
                                Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Approved" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                                if (ok) loadEditRequests()
                            }
                        }
                    }
                    val rejectBtn = TextView(this@BriefingActivity).apply {
                        text = "❌ Reject"
                        textSize = 12.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#7A1F3D"))
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.marginStart = dp(12)
                        layoutParams = lp
                        setOnClickListener {
                            lifecycleScope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    DoctorVisitRepository().rejectReferralEditRequest(req.id, user.mobile)
                                }
                                Toast.makeText(this@BriefingActivity, NoBengali.s(if (ok) "Rejected" else "ব্যর্থ — নেট চেক করুন"), Toast.LENGTH_SHORT).show()
                                if (ok) loadEditRequests()
                            }
                        }
                    }
                    btnRow.addView(approveBtn)
                    btnRow.addView(rejectBtn)
                    row.addView(btnRow)
                    refRowsBox.addView(row)
                    refRowsBox.addView(View(this@BriefingActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                        setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    })
                }
                box.addView(refRowsBox)
            }
        }
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Visit Fee Missing" visibility --
    // Master-only, read-only (no Approve/Reject -- there's nothing to
    // approve, this is just a heads-up so Master can manually check/
    // re-collect if the fee genuinely never got recorded). Same dynamic-
    // build pattern as loadBackdateRequests() just above.
    private fun loadMissingVisitFees() {
        if (user.role != "master") {
            binding.missingVisitFeeContainer.visibility = View.GONE
            return
        }
        binding.missingVisitFeeContainer.removeAllViews()
        binding.missingVisitFeeContainer.visibility = View.VISIBLE
        binding.missingVisitFeeContainer.addView(TextView(this).apply {
            text = "Loading..."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#8A93A6"))
            setPadding((16 * resources.displayMetrics.density).toInt(), (10 * resources.displayMetrics.density).toInt(), 0, 0)
        })
        lifecycleScope.launch {
            val repo = PaymentRepository(this@BriefingActivity)
            val all = withContext(Dispatchers.IO) {
                // 🟢 B662 (15.08.2026): এই পর্দাটাই তালিকাটা চোখের সামনে দেখায়, তাই
                //   fresh = true — ফি নেওয়ার সঙ্গে সঙ্গে নাম চলে যাওয়া আগের মতোই থাকে।
                try { repo.fetchMissingVisitFeePatients(fresh = true) } catch (_: Exception) { emptyList() }
            }
            // 🚨 খাতার সারি B87 (TK, 29.07.2026): নতুন APK বসালে ফোনের "পড়া
            // হয়েছে" চিহ্ন মুছে যেত, তাই পুরনো সব নাম আবার ফিরে আসত। এখন
            // চিহ্নটা ক্লাউডেও থাকে — তাই APK বদলালেও থাকবে।
            // ⛔ ফোনের পুরনো চিহ্নও আগের মতোই কাজ করে; দুটো মিলিয়ে দেখা হয়,
            //    তাই নেট না থাকলেও তালিকা আগের মতোই চলবে।
            val cloudSeen = withContext(Dispatchers.IO) {
                try { repo.fetchFeeMissingSeenKeys() } catch (_: Exception) { emptySet<String>() }
            }
            // 🔒 TK-APPROVED (28.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B48).
            // TK-এর কথা: *"এটা মনে করুন মেসেজ — যে মেসেজ আমি পড়লাম, সেই মেসেজ
            // পড়া হয়ে গেছে বলে সেখানে তার শো করার কথা না... লিস্টে ৩০টা নাম
            // আছে, আমি একটা দেখলাম, পরে ব্যাকে এলে ২৯টা থাকার কথা।"*
            // আর: *"দেখা হয়ে গেলে আর ওখানে লেখা রাখতে হবে না — বাস ক্লিয়ার।"*
            val missing = all.filter { !isFeeRowSeen(it) && !cloudSeen.contains(feeRowKey(it)) }
            val box = binding.missingVisitFeeContainer
            box.removeAllViews()
            if (missing.isEmpty()) {
                box.visibility = View.GONE
                return@launch
            }
            box.visibility = View.VISIBLE
            val d = resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()
            box.addView(TextView(this@BriefingActivity).apply {
                text = "VISIT FEE MISSING (${missing.size})"
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#B42318"))
                setPadding(dp(16), dp(12), dp(16), dp(12))
            })
            for (mv in missing) {
                val row = LinearLayout(this@BriefingActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(android.graphics.Color.WHITE)
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                    isClickable = true
                    isFocusable = true
                    val outValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    foreground = androidx.core.content.ContextCompat.getDrawable(this@BriefingActivity, outValue.resourceId)
                    // TK-REQUESTED (2026-07-24): tap opens this patient's Timeline
                    // (same pattern used everywhere else in the app).
                    setOnClickListener {
                        // 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B29): নামের উপর চাপ
                        // দিলে কিছুই হত না। **আসল কারণ:** মোবাইল জমা থাকে `+91` সহ, তাই
                        // সংখ্যা গুনলে ১২টা হয় — আর এখানে ঠিক ১০ খোঁজা হত, তাই কোনোদিন
                        // মিলত না। অ্যাপের বাকি সব জায়গায় **শেষ ১০টা সংখ্যা** নেওয়া হয়
                        // (`takeLast(10)`); এখানে সেটাই বাদ পড়েছিল।
                        val digits = mv.mobile.filter { it.isDigit() }.takeLast(10)
                        if (digits.length == 10) {
                            // খাতার সারি B48: দেখা মানেই ক্লিয়ার — ব্যাক করে
                            // এলে এই নামটা তালিকায় আর থাকবে না, সংখ্যাও কমবে।
                            markFeeRowSeen(mv)
                            // 🔒 খাতার সারি B87: ফোনের পাশাপাশি ক্লাউডেও চিহ্ন —
                            // পিছনে চলে, তাই পর্দা এক মুহূর্তও আটকায় না; ব্যর্থ
                            // হলেও ফোনের চিহ্নটা আগের মতোই কাজ করে।
                            val keyForCloud = feeRowKey(mv)
                            Thread {
                                try {
                                    PaymentRepository(this@BriefingActivity)
                                        .markFeeMissingSeenCloud(keyForCloud, user.mobile, user.name)
                                } catch (_: Throwable) { }
                            }.start()
                            startActivity(android.content.Intent(this@BriefingActivity, PatientTimelineActivity::class.java).putExtra("mobile", digits))
                        } else {
                            Toast.makeText(this@BriefingActivity, "No valid mobile number for this patient", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                row.addView(TextView(this@BriefingActivity).apply {
                    // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে নিচের লাইনে (patientId ফাঁকা হলে) মোবাইল দুইবার দেখাতে পারত।
                    text = mv.name.ifBlank { "UNKNOWN" }
                    textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                })
                row.addView(TextView(this@BriefingActivity).apply {
                    text = "${mv.patientId.ifBlank { mv.mobile }} · ${mv.branch} · Registered: ${if (mv.registrationDate.isBlank()) "—" else DateUtil.display(mv.registrationDate)}"
                    textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#5b6b81"))
                    setPadding(0, dp(4), 0, 0)
                })
                box.addView(row)
                box.addView(View(this@BriefingActivity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                })
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔒 খাতার সারি B48 — "Visit Fee Missing" তালিকা মেসেজের মতো চলে।
    //
    // ⛔ ক্লাউডে কিছুই লেখা হয় না — চিহ্নটা **শুধু এই ফোনে** জমা থাকে, তাই
    //    Supabase-এর খরচ এক পয়সাও বাড়ে না আর অন্য কারো তালিকা বদলায় না।
    // ⛔ টাকার কোনো হিসাব বদলায় না — শুধু এই তালিকায় দেখানো বন্ধ হয়।
    //    ফি সত্যিই নেওয়া হলে সারিটা এমনিতেই উঠে যেত, আগের মতোই।
    // ──────────────────────────────────────────────────────────────
    // 🔒🔒 খাতার সারি (29.07.2026 রাত — বেল/Briefing মেলেনি বাগের সমাধান):
    // এই তিনটে ফাংশন আগে এখানেই আলাদাভাবে লেখা ছিল, আর Dashboard-এর ঘন্টা
    // (BellCounter) এই নিয়মটা একদমই মানত না — তাই ঘন্টায় সংখ্যা থাকত,
    // এই পর্দা খুললে ফাঁকা দেখাত। এখন দুটোই `MissingFeeSeenGuard`-এর
    // ঠিক একই নিয়ম মানে (একই SharedPreferences ফাইল, তাই আগে "দেখা"
    // মার্ক করা নামগুলো অক্ষত থাকে) — বিস্তারিত `MissingFeeSeenGuard.kt`।
    private fun feeRowKey(mv: MissingVisitFee): String = MissingFeeSeenGuard.rowKey(mv)

    private fun isFeeRowSeen(mv: MissingVisitFee): Boolean = MissingFeeSeenGuard.isSeenLocal(this, mv)

    private fun markFeeRowSeen(mv: MissingVisitFee) {
        try {
            MissingFeeSeenGuard.markSeenLocal(this, mv)
        } catch (_: Throwable) { }
    }

    /**
     * 🔒 খাতার সারি B100 — স্টাফের ডিলিট-অনুরোধ মাস্টার এক চাপে অনুমোদন করেন।
     * ⛔ আগে **নিশ্চিত করার পর্দা** ওঠে — ভুল চাপে কিছু মুছবে না।
     * ⛔ ডিলিট হয় অ্যাপের সেই পুরনো পথেই (Trash-এ যায়, ফেরানো যায়, টাকার
     *    ইতিহাস অক্ষত)। ⛔ হয়ে গেলে নোটিশটা মাস্টারের তালিকা থেকে সরে যায়,
     *    আর কী হলো তা উত্তর হিসেবে লেখা থাকে — তাই হিসাব থেকে যায়।
     */
    // 🆕 B419 (04.08.2026, TK-নির্দেশ — "ভুল করে বন্ধ হলে আবার খোলা যাবে?")
    // — Master এখান থেকে এক চাপে অনুমোদন/প্রত্যাখ্যান করেন। ⛔ B414-এর একই
    // ডাবল-চাপ-সুরক্ষা প্যাটার্ন — বোতাম আগে `show()` করে, তারপর নিজস্ব
    // ক্লিক-লিসেনারে বসিয়ে, চাপার সাথে সাথেই দুটো বোতামই বন্ধ হয়ে যায়।
    private fun confirmApproveReopen(item: Briefing) {
        val branch = item.message.split("\n").map { it.trim() }
            .firstOrNull { it.startsWith("Branch :", ignoreCase = true) }
            ?.substringAfter("Branch :")?.trim().orEmpty()
        val date = item.message.split("\n").map { it.trim() }
            .firstOrNull { it.startsWith("Date :", ignoreCase = true) }
            ?.substringAfter("Date :")?.trim().orEmpty()
        val dlg = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Reopen: ${branch.ifBlank { "?" }}"))
            .setMessage(item.message)
            .setPositiveButton("✅ Approve", null)
            .setNeutralButton("❌ Reject", null)
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
        fun lockButtons() {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = false
            dlg.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false
        }
        dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            lockButtons()
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    ChamberReopenPermission.approveAndReopen(this@BriefingActivity, item.message, user.mobile)
                }
                val branchShown = branch.ifBlank { "?" }
                val dateShown = date.ifBlank { "?" }
                val msg = when (result) {
                    "OK" -> NoBengali.s("Reopened — $branchShown · $dateShown আবার এডিটযোগ্য")
                    "BAD_REQUEST" -> NoBengali.s("এই অনুরোধ থেকে Branch/Date চেনা গেল না")
                    else -> "Could not reopen — network issue, please retry"
                }
                Toast.makeText(this@BriefingActivity, msg, Toast.LENGTH_LONG).show()
                if (result == "OK") {
                    withContext(Dispatchers.IO) {
                        try {
                            val who = StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
                            BriefingRepository().addReply(this@BriefingActivity, item.id, "✅ Reopened by $who", user.mobile)
                            BriefingRepository().deleteOrHide(item.id, user)
                        } catch (_: Throwable) { }
                    }
                    loadList()
                }
                try { dlg.dismiss() } catch (_: Throwable) { }
            }
        }
        dlg.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            lockButtons()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val who = StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
                        BriefingRepository().addReply(this@BriefingActivity, item.id, "❌ Rejected by $who", user.mobile)
                        BriefingRepository().deleteOrHide(item.id, user)
                    } catch (_: Throwable) { }
                }
                Toast.makeText(this@BriefingActivity, "Rejected", Toast.LENGTH_SHORT).show()
                loadList()
                try { dlg.dismiss() } catch (_: Throwable) { }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 🆕🔒 TK-নির্দেশ (07.08.2026) — "অনুমতি অনেকগুলো একসাথে হয়ে গেলে সেগুলো
    // যেন মার্ক করা যায়, একবারে মার্ক করে অনুমতি দিতে পারে।"
    // কার্ডের বাছাই-বক্সে টিক দিলেই নিচে বার আসে → "Approve selected" চাপলে
    // একটার পর একটা (ক্রমে) অনুমোদন হয়, শেষে কতগুলো হলো তা জানানো হয়।
    // ⛔ প্রতিটা অনুমোদন **একই প্রমাণিত পথেই** (`DeletePermission.approveAndDelete`
    //    — এক-এক করে চাপলে যা হতো, হুবহু তাই), নতুন কোনো ডিলিট-লজিক নয়।
    // ⛔ Delete request ছাড়া অন্য ধরনের অনুরোধ (Refund/Reopen) এই একসাথে-
    //    অনুমোদনে ধরা হয় না — সেগুলোয় আলাদা Approve/Reject সিদ্ধান্ত লাগে,
    //    তাই ভুল করে একসাথে অনুমোদন হয়ে যাওয়ার ঝুঁকি নেই।
    // ─────────────────────────────────────────────────────────────────────
    private fun refreshBulkBar() {
        val n = adapter.selectedIds.size
        binding.bulkApproveBar.visibility = if (n > 0) View.VISIBLE else View.GONE
        binding.tvBulkCount.text = if (n == 1) "1 selected" else "$n selected"
    }

    private fun confirmBulkApprove() {
        val ids = adapter.selectedIds.toList()
        if (ids.isEmpty()) return
        val chosen = adapter.itemsSnapshot().filter { ids.contains(it.id) }
        // শুধু "Delete request" — বাকি ধরনের অনুরোধ বাদ (উপরের নোট দেখুন)।
        // 🔴🔒 V697 — একসাথে-অনুমোদনেও রিপ্লাই-নোটিশ বাদ (উপরের একই নিয়ম)।
        val deletable = chosen.filter { !BriefingModel.isReplyNotice(it.title) && it.title.contains("Delete request", ignoreCase = true) }
        val skipped = chosen.size - deletable.size
        if (deletable.isEmpty()) {
            Toast.makeText(this, "This request type cannot be approved in bulk — open and review one at a time.", Toast.LENGTH_LONG).show()
            return
        }
        val dlg = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Approve Selected Requests?"))
            .setMessage(
                "${deletable.size} selected delete requests will be approved.\n\n" +
                deletable.joinToString("\n") { "• " + it.title } +
                (if (skipped > 0) "\n\n($skipped other request(s) will remain unchanged.)" else "") +
                "\n\nRecords will move to Trash Bin and can be restored later."
            )
            .setPositiveButton("APPROVE ALL", null)
            .setNegativeButton("CANCEL", null)
            .show().also { PremiumAlert.paint(it) }
        dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            dlg.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false
            lifecycleScope.launch {
                var done = 0
                var failed = 0
                val who = StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
                for (item in deletable) {
                    val result = withContext(Dispatchers.IO) {
                        try { DeletePermission.approveAndDelete(item.message, user.mobile) }
                        catch (_: Throwable) { "NETWORK" }
                    }
                    // এক-এক করে চাপার সাথে হুবহু একই নিয়ম (NOT_FOUND-ও শেষ ধরা হয়)।
                    if (result == "OK" || result == "NOT_FOUND") {
                        done++
                        withContext(Dispatchers.IO) {
                            try {
                                val note = if (result == "OK") "✅ Approved & deleted by $who"
                                           else "ℹ️ Record was already deleted — closed by $who"
                                BriefingRepository().addReply(this@BriefingActivity, item.id, note, user.mobile)
                                // 🔴🔒 V666 — এখন context দেওয়া হচ্ছে, ব্যর্থ হলে রিট্রাই-জমা হয়।
                                BriefingRepository().deleteOrHide(item.id, user, this@BriefingActivity)
                            } catch (_: Throwable) { }
                        }
                    } else failed++
                }
                adapter.clearSelection()
                refreshBulkBar()
                Toast.makeText(
                    this@BriefingActivity,
                    "$done request(s) approved." + (if (failed > 0) " · $failed request(s) failed — please try again." else ""),
                    Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                try { dlg.dismiss() } catch (_: Throwable) { }
                loadList()
            }
        }
    }

    private fun confirmApproveDelete(item: Briefing) {
        // 🔴 B414 (04.08.2026, TK-নির্দেশে গভীর অডিট — "ডাবল-চাপে দুইবার কাজ
        // হয়ে যাওয়া" ধরন) — এই বোতাম আগে সাধারণ `setPositiveButton()` দিয়ে
        // তৈরি ছিল, যেটা ঠিক B334-এর মতোই ঝুঁকিতে ছিল (দ্রুত দুইবার চাপলে
        // async কাজ দুইবার চলতে পারত — এখানে দুইবার Trash-এ পাঠানো/দুইবার
        // "Approved" রিপ্লাই)। এখন B334-এর প্রমাণিত প্যাটার্ন (PaymentActivity.kt)
        // পুনর্ব্যবহার — `show()` করে বোতামের নিজস্ব ক্লিক-লিসেনারে বসানো
        // হলো, চাপার সাথে সাথেই দুটো বোতামই বন্ধ হয়ে যায়, কাজ শেষে নিজে
        // dismiss হয়। ⛔ ডিলিট/Trash/অনুমতির আসল লজিক এক অক্ষরও বদলায়নি।
        val dlg = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Approve & Delete?"))
            /* 📄 V1177 — শুধু দরকারি তিন লাইন (TK-অনুমোদিত প্রুফ)।
               ⛔ `item.message` অক্ষত, তাই নিচের Approve আগের মতোই চলে। */
            .setMessage(DeletePermission.shortSummary(item.message) +
                NoBengali.s("\n\nGoes to Trash — can be restored."))
            .setPositiveButton("Approve & Delete", null)
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
        dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
            dlg.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    DeletePermission.approveAndDelete(item.message, user.mobile)
                }
                val msg = when (result) {
                    "OK" -> "Approved — Deleted (Trash Bin-এ আছে)"
                    "NOT_FOUND" -> "Record not found — it may already be deleted"
                    "BAD_REQUEST" -> "এই অনুরোধ থেকে রেকর্ড চেনা গেল না"
                    else -> "Could not delete — network is too slow/unstable, please retry"
                }
                Toast.makeText(this@BriefingActivity, msg, Toast.LENGTH_LONG).show()
                // 🔴🔴 TK-রিপোর্ট (07.08.2026, ছবিসহ — "হাই স্পিড ইন্টারনেট চলছে,
                // ডিলিট করেছি তারপরেও থেকে যাচ্ছে")। **আসল কারণ (কোড ধরে যাচাই):**
                // এখানে আগে শুধু `result == "OK"` হলেই নোটিশ কার্ডটা সরানো হতো।
                // কিন্তু রেকর্ডটা যদি আগেই মুছে গিয়ে থাকে (স্টাফ নিজেই মুছেছেন,
                // বা আগে একবার অনুমোদন হয়ে গেছে) তখন `NOT_FOUND` আসে — আর
                // নোটিশ কার্ডটা বোর্ডে **চিরকাল থেকে যেত**, যতবারই চাপা হোক।
                // ⛔ নেটের কোনো দোষ নয় (TK-এর স্পিড-টেস্ট: ৩৯.৫↓/৫৯.৭↑ Mbps)।
                // **সমাধান:** রেকর্ড আগেই না-থাকলেও কাজ শেষ ধরে নোটিশটা সরে যায়।
                // ⛔ শুধু সত্যিকারের নেটওয়ার্ক-ব্যর্থতায় ("NETWORK") আগের মতোই
                //    কার্ডটা থাকে, যাতে আবার চেষ্টা করা যায় — কিছু হারায় না।
                if (result == "OK" || result == "NOT_FOUND") {
                    withContext(Dispatchers.IO) {
                        try {
                            val who = StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
                            val note = if (result == "OK") "✅ Approved & deleted by $who"
                                       else "ℹ️ Record was already deleted — closed by $who"
                            BriefingRepository().addReply(this@BriefingActivity, item.id, note, user.mobile)
                            // 🔴🔒 V666 — এখন context দেওয়া হচ্ছে, ব্যর্থ হলে রিট্রাই-জমা হয়।
                            BriefingRepository().deleteOrHide(item.id, user, this@BriefingActivity)
                        } catch (_: Throwable) { }
                    }
                    loadList()
                }
                try { dlg.dismiss() } catch (_: Throwable) { }
            }
        }
    }

    /**
     * 🔴🔴 B281 (02.08.2026, TK-রিপোর্ট — "Refund request" নোটিশ SEEN করেও
     * সরছে না, TK নিজে আলাদা ড্রপডাউন খুঁজতে রাজি নন): "Refund request"
     * নোটিশ কার্ড থেকেই সরাসরি Approve/Reject — ঠিক B100-এর Delete-request
     * প্যাটার্নেই (`DeletePermission.approveAndDelete`-এর মতো বার্তা থেকে
     * তথ্য বার করা)।
     *
     * ⛔ বার্তায় কোনো সরাসরি refund-row-id সংরক্ষিত নেই (তৈরির সময় থেকেই,
     *    বদলানো হয়নি) — তাই বার্তার লেখা থেকে **Patient ID** (মানুষ-পড়া-যায়
     *    কোড, যেমন "FLK-29072026-001") বার করে সেই কোড দিয়ে pending refund
     *    সারিটা লাইভ খোঁজা হয় (`patientCode` কলাম — `patientId` কলামে UUID
     *    থাকে, ভুল কলামে খুঁজলে কখনো মিলত না)।
     * ⛔ **নিরাপদ কেন:** সারি সত্যিই না পাওয়া গেলে (আগেই Approve/Reject হয়ে
     *    গেছে, বা অন্য ফোন থেকে এখনো সিঙ্ক হয়নি) স্পষ্ট বার্তা দেখায়, কিছু
     *    ভুল করে বদলায় না। Approve/Reject-এর আসল ফাংশন (PaymentRepository.
     *    approveRefund/rejectRefund) আগে থেকে TK-অনুমোদিত, অপরিবর্তিত।
     */
    private fun confirmApproveRefund(item: Briefing) {
        val patientCode = item.message.split("\u00b7").map { it.trim() }
            .firstOrNull { it.startsWith("Patient ID", ignoreCase = true) }
            ?.substringAfter("Patient ID")?.trim().orEmpty()
        if (patientCode.isBlank()) {
            Toast.makeText(this, NoBengali.s("এই নোটিশ থেকে Patient ID চেনা গেল না"), Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            binding.progressLoad.visibility = View.VISIBLE
            val rows = withContext(Dispatchers.IO) {
                try {
                    SupabaseClient.fetchList(
                        "payments",
                        "patientCode=eq.${java.net.URLEncoder.encode(patientCode, "UTF-8")}&payType=eq.refund&refundApprovalStatus=eq.pending",
                        5
                    )
                } catch (_: Throwable) { org.json.JSONArray() }
            }
            binding.progressLoad.visibility = View.GONE
            if (rows.length() == 0) {
                AlertDialog.Builder(this@BriefingActivity)
                    .setCustomTitle(PremiumAlert.header(this@BriefingActivity, "Not found"))
                    .setMessage(NoBengali.s(" has no Pending Refund now — it may already be Approved/Rejected, or has not reached the cloud from the other phone yet."))
                    .setPositiveButton("Mark notice done") { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) { try { BriefingRepository().deleteOrHide(item.id, user) } catch (_: Throwable) { } }
                            loadList()
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show().also { PremiumAlert.paint(it); try { NoBengali.installDialog(it) } catch (_: Throwable) { } }
                return@launch
            }
            val row = rows.getJSONObject(0)
            val refundId = row.optString("id")
            val amt = "\u20b9" + "%,.0f".format(row.optDouble("amount", 0.0))
            // 🔴 B414 (04.08.2026, একই ডাবল-চাপ ধরন, B334-এর প্রমাণিত প্যাটার্ন) —
            // Approve/Reject দুটো বোতামই নিজস্ব ক্লিক-লিসেনারে বসানো হলো, যাতে
            // চাপার সাথে সাথেই তিনটে বোতাম বন্ধ হয়ে যায় (দ্রুত দুইবার চাপলে
            // দুইবার Approve/Reject না চলে — টাকার রেকর্ডে দুইবার রিপ্লাই না যায়)।
            val dlg = AlertDialog.Builder(this@BriefingActivity)
                .setCustomTitle(PremiumAlert.header(this@BriefingActivity, "Refund: $amt"))
                .setMessage(item.message)
                .setPositiveButton("\u2705 Approve", null)
                .setNeutralButton("\u274c Reject", null)
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
            fun lockButtons() {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
                dlg.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled = false
                dlg.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled = false
            }
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                lockButtons(); resolveRefund(item, refundId, approve = true) { try { dlg.dismiss() } catch (_: Throwable) { } }
            }
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
                lockButtons(); resolveRefund(item, refundId, approve = false) { try { dlg.dismiss() } catch (_: Throwable) { } }
            }
        }
    }

    private fun resolveRefund(item: Briefing, refundId: String, approve: Boolean, onDone: () -> Unit = {}) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                val repo = PaymentRepository(this@BriefingActivity)
                if (approve) repo.approveRefund(refundId, user.mobile) else repo.rejectRefund(refundId, user.mobile)
            }
            Toast.makeText(
                this@BriefingActivity,
                if (ok) (if (approve) "Refund Approved" else "Refund Rejected") else "Failed — check connection",
                Toast.LENGTH_LONG
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            if (ok) {
                withContext(Dispatchers.IO) {
                    try {
                        val who = StaffDirectory.findAccount(user.mobile)?.name ?: user.mobile
                        BriefingRepository().addReply(this@BriefingActivity, item.id, "${if (approve) "\u2705 Approved" else "\u274c Rejected"} by $who", user.mobile)
                        BriefingRepository().deleteOrHide(item.id, user)
                    } catch (_: Throwable) { }
                }
                loadList()
            }
            onDone()
        }
    }

    /* 🟢🔒 V1134 (০৬.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK: *"আমি যদি অনুমতি
       না দেই, অর্থাৎ এপ্রুভ না করি, তাহলে আমাকে কোনটা চাপতে হবে?"*

       আগে "না" বলার কোনো বোতামই ছিল না — Close চাপলে শুধু নোটিশটা বোর্ড থেকে
       সরত, আর স্টাফ জানতেই পারতেন না মাস্টার রাজি হননি।

       ⇒ এখন **Reject**: টাকা/রেকর্ড কিচ্ছু মোছে না, স্টাফের কাছে উত্তর যায়
         (অ্যাপের নিজের প্রমাণিত রিপ্লাই-পথেই — নতুন টেবিল বা SQL লাগে না),
         তারপর অনুরোধটা বোর্ড থেকে সরে যায়।
       ⛔ উত্তর পাঠানো না গেলে অনুরোধটা **সরানো হয় না** — নইলে স্টাফ কিছু
          না জেনেই অপেক্ষা করতেন। */
    private fun confirmReject(item: Briefing) {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Reject this request?"))
            .setMessage(
                item.title.ifBlank { "Request" } +
                "\n\n\u26d4 Nothing will be deleted. The staff who asked will be told it was rejected."
            )
            .setPositiveButton("Reject") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            val told = repository.addReply(
                                this@BriefingActivity, item.id,
                                "Rejected by Master \u2014 nothing was deleted.", user.mobile
                            )
                            if (told) repository.deleteOrHide(item.id, user) else false
                        } catch (_: Throwable) { false }
                    }
                    Toast.makeText(
                        this@BriefingActivity,
                        if (ok) "Rejected" else "Failed \u2014 check connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (ok) loadList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /* ═══════════════════════════════════════════════════════════════════════
       👉🔒 V1438 (১৩.০৯.২০২৬, TK-নির্দেশ ছবিসহ — Notice Board-এর ছবি)
       TK: *"এই সব সাধারণ নোটিফিকেশন ডান দিকে স্ক্রল করলে যেন সরে যায়।
       অর্থাৎ যেখানে আমার অ্যাকশন নিতে হবে সেটা থাক; যেখানে অ্যাকশন বা
       অ্যাপ্রুভের আর কোনো ব্যাপার নেই সেগুলো ডান দিকে সরালেই যেন সরে যায়।"*

       ─── যা হলো ──────────────────────────────────────────────────────────
       সাধারণ নোটিশের কার্ড **ডান দিকে সরালেই** বোর্ড থেকে চলে যায় — Close
       চেপে, তারপর জানলার "Hide/Delete" চেপে আর করতে হয় না।

       ─── ⛔ যেগুলো কখনো সরবে না (TK-এর কথা মতো "অ্যাকশন বাকি") ──────────
       Refund request · Delete request · Chamber reopen request · Leave
       request — অর্থাৎ `BriefingModel.needsMasterApproval()` যেগুলোকে
       "অনুমতি/অ্যাকশন বাকি" বলে। ⛔ নিয়মটা **নতুন করে লেখা হয়নি** — বোর্ডে
       কোন নোটিশ থেকে যাবে সেটা ঠিক করার পুরনো সেই একটাই ফাংশনই ডাকা হলো,
       তাই দুই জায়গা কখনো আলাদা হতে পারবে না (নিয়ম ৭)।
       ⛔ "একসাথে অনুমোদন"-এর টিক দেওয়া অবস্থায় সরানো বন্ধ — নইলে বাছাই-করা
          কার্ড ভুল করে সরে যেত।

       ─── ⛔ নিরাপত্তা ───────────────────────────────────────────────────
       সরানো মানে **শুধু নিজের তালিকা থেকে লুকানো** (`hideForMe` — প্রজেক্টের
       প্রমাণিত সেই একই পথ, যেটা "Seen"-এ অটো-ক্লিয়ারে চলে)। ⛔ মাস্টার
       সরালেও অন্য কারো বোর্ড থেকে নোটিশ মোছে না — অর্থাৎ ভুল করে সরে গেলেও
       কারো কিছু হারায় না। Close বোতামের পুরনো কাজ (মাস্টারের সবার জন্য
       ডিলিট, জানলা সহ) এক অক্ষরও বদলায়নি।
       ⛔ ক্লাউডে লেখা ব্যর্থ হলে তালিকা আবার লোড হয় — কার্ডটা ফিরে আসে,
          মিথ্যে "হয়ে গেছে" দেখানো হয় না।
       ═══════════════════════════════════════════════════════════════════ */
    private fun attachSwipeToClear() {
        try {
            val cb = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
                0, androidx.recyclerview.widget.ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    rv: androidx.recyclerview.widget.RecyclerView,
                    vh: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean = false

                override fun getSwipeDirs(
                    rv: androidx.recyclerview.widget.RecyclerView,
                    vh: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Int {
                    if (adapter.selectedIds.isNotEmpty()) return 0
                    val item = adapter.itemAt(vh.bindingAdapterPosition) ?: return 0
                    if (needsMasterApproval(item.title)) return 0   // অ্যাকশন বাকি — থাকবে
                    return androidx.recyclerview.widget.ItemTouchHelper.RIGHT
                }

                override fun onSwiped(
                    vh: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    direction: Int
                ) {
                    val pos = vh.bindingAdapterPosition
                    val item = adapter.itemAt(pos)
                    if (item == null) { adapter.notifyDataSetChanged(); return }
                    adapter.removeAt(pos)
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            try { repository.hideForMe(item.id, user.mobile) } catch (_: Throwable) { false }
                        }
                        if (isFinishing || isDestroyed) return@launch
                        if (!ok) {
                            Toast.makeText(
                                this@BriefingActivity,
                                "Could not clear — check connection",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadList()   // কার্ডটা ফিরে আসে
                        }
                    }
                }
            }
            androidx.recyclerview.widget.ItemTouchHelper(cb)
                .attachToRecyclerView(binding.recyclerView)
        } catch (_: Throwable) {
            // সরানোর সুবিধাটা বসাতে ভুল হলেও পর্দা আগের মতোই চলবে
        }
    }

    private fun confirmDelete(item: Briefing) {
        val msg = if (user.role == "master")
            "Delete this notice for everyone?"
        else
            "Hide this notice from your list?"
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, item.title.ifBlank { "Notice" }))
            .setMessage(msg)
            .setPositiveButton(if (user.role == "master") "Delete" else "Hide") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { repository.deleteOrHide(item.id, user) }
                    Toast.makeText(this@BriefingActivity, if (ok) "Done" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                    if (ok) loadList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // 🔒 B467 (06.08.2026, TK-নির্দেশ) — "Staff IN TIME" শুধু তথ্য জানানোর
    // নোটিশ, পরে আর দরকার পড়ে না। TK-এর সিদ্ধান্ত: "সীন করলেই মুছে যাক"।
    // ⛔ বাকি সব ধরনের নোটিশ (Payment deleted, Refund/Delete/Reopen request,
    // Patient Decision, TK নিজে লেখা নোটিশ) আগের মতোই সীন করলে শুধু ঘন্টা
    // থেকে বাদ পড়ে, তালিকায় থেকেই যায় — এখানে শুধু "Staff IN TIME"-এর
    // জন্যই আলাদা আচরণ, বাকি কোনো টাইপ ছোঁয়া হয়নি।
    private val AUTO_DELETE_ON_SEEN_TITLES = setOf("Staff IN TIME")

    /**
     * 🔒 TK-ORDER (2026-08-06): notices that need the Master's permission/action
     * must STAY on the board until acted upon — they must NOT auto-hide when
     * "Seen" is pressed. These are the payment/permission requests. Everything
     * else is an ordinary notice and disappears from a person's own list once
     * they have seen it.
     */
    // 🔵 খাতার সারি (TK, 09.08.2026): নিয়মটা এখন এক জায়গায় — BriefingModel-এ।
    // তালিকা-ফিল্টার (visibleForUser) ও এই অটো-ক্লিয়ার দুটো একই সংজ্ঞা মানে,
    // তাই কখনো আলাদা হতে পারবে না। আচরণ হুবহু আগের (Refund/Delete/Reopen request)।
    private fun needsMasterApproval(title: String): Boolean =
        BriefingModel.needsMasterApproval(title)

    // 🔵 TK-ORDER (07.08.2026): এই সেশনে যে নোটিশ-id গুলো ইতিমধ্যে অটো-hide করার
    // চেষ্টা হয়েছে — একই লোডে (onCreate+onResume) দুবার লেখা এড়াতে। ⛔ শুধু
    // সেশন-মেমরি, কিছুই স্থায়ীভাবে সেভ হয় না।
    private val autoClearedThisSession = HashSet<String>()

    /**
     * 🔵 TK-ORDER (07.08.2026): Master Briefing খুললেই **অ্যাকশন লাগে না এমন**
     * নোটিশ (Staff IN TIME/OUT TIME/সাধারণ) নিজে থেকেই Master-এর তালিকা থেকে সরে
     * যায় — পরের বার খুললে আর দেখাবে না, Seen/Delete চাপা লাগে না। **অনুমতি লাগে
     * এমন** (Refund/Delete/Reopen request) বাদ — সেগুলো অ্যাকশন না দেওয়া পর্যন্ত থাকে।
     *
     * কীভাবে: প্রতিটা যোগ্য নোটিশে `hideForMe(master)` — অর্থাৎ **শুধু Master-এর
     * জন্যই লুকোনো** (per-user hiddenFor), কখনো global delete নয়, তাই অন্য কারো
     * বোর্ডে প্রভাব পড়ে না। তালিকা ও ঘন্টার গোনা — দুটোই `visibleForUser` মানে,
     * তাই "সংখ্যা আছে কিন্তু ভিতরে ফাঁকা" হয় না।
     *
     * ⛔ নিরাপত্তা: (১) শুধু `user.role=="master"`। (২) শুধু Master-কে **target-করা**
     * নোটিশ (`targetsHit`) — Master-এর নিজের পাঠানো নোটিশ নয়। (৩) `needsMasterApproval`
     * নোটিশ কখনো নয়। (৪) আগে থেকেই hidden হলে আবার নয়। (৫) লেখা ব্যর্থ হলে
     * `updateById`-এর ভিতরের CloudWriteQueue নিজে থেকে পরে সম্পন্ন করে।
     * 📌 TK-এর ইন্টারনেট দ্রুত (স্পিড-টেস্ট: ২৭.৯↓/৫৯.৩↑ Mbps) — নোটিশ থেকে
     *    যাওয়ার কারণ নেট নয়, আগের কোডের যাচাই-না-করা আচরণ।
     */
    // 🔴🔴 TK-নির্দেশ (07.08.2026, স্ক্রিনশটসহ — "কিছুক্ষণ পরে পরে এরকম অপশন কেন
    // আসবে, মাস্টারের কি কোনো কাজ নেই?" + "আমি সিন করছি মানেই সিন — খুললেই তো
    // অটোমেটিক সিন হয়ে যাচ্ছে, আবার কেন চাপব")। **আসল কারণ (কোড ধরে যাচাই):**
    // `BriefingReminderWorker` প্রতি ১০ মিনিটে "🔔 Unread Notice" পাঠায় যতক্ষণ
    // `unseenCount() > 0`; আর অনুমোদন-নোটিশ (Delete/Refund/Reopen request) কখনো
    // নিজে থেকে "seen" হতো না — Seen বোতাম হাতে না চাপলে সংখ্যা কমতই না। তাই
    // সারাদিন বাজত। ⛔ নেটের কোনো ব্যাপার নয়।
    // **সমাধান:** Briefing খোলামাত্র —
    //   (ক) আমাকে-target-করা **সব** নোটিশ "seen" হয়ে যায় → ঘন্টার সংখ্যা শূন্য,
    //       ১০-মিনিটের রিমাইন্ডারও নিজে থেকেই থেমে যায়;
    //   (খ) **অ্যাকশন লাগে না** এমন নোটিশ আমার তালিকা থেকে সরানো হয় (hideForMe)
    //       — এই বারের পর্দায় থেকে যায় (পড়ার সুযোগ থাকে), পরেরবার খুললে আর নেই;
    //   (গ) **অনুমতি লাগে** এমন নোটিশ কখনো সরে না — Approve/Reject না দেওয়া
    //       পর্যন্ত থেকেই যায় (শুধু "seen" হয়, যাতে অকারণে অ্যালার্ম না বাজে)।
    // ⛔ আগে এটা শুধু Master-এর জন্য চলত; এখন সব ব্যবহারকারীর জন্য — কারণ TK-এর
    //    নির্দেশে "Seen" বোতামটাই তুলে দেওয়া হয়েছে, নইলে স্টাফের নোটিশ কখনো
    //    সরত না। hideForMe সবসময় **শুধু নিজের জন্য**, কখনো global delete নয়।
    // 🔴🔒 V1621 (২০.০৯.২০২৬, TK-নির্দেশ) — "একই রিকোয়েস্ট যেন বারবার কেউ না
    // পাঠাতে পারে" ইতিমধ্যেই আছে (ChamberReopenPermission.findPendingRequest /
    // wlv1FindPendingReopen — পাঠানোর ঠিক আগে যাচাই করে, একই ব্রাঞ্চ+তারিখের
    // এখনো-অমীমাংসিত অনুরোধ থাকলে নতুন করে পাঠতেই দেয় না)। এই ফাংশন সেটার
    // উল্টো দিক — পুরনো, কেউ না-ছোঁয়া অনুরোধ **নিজে থেকে বন্ধ** করে দেয়,
    // যাতে ওই একই পাহারা পরদিন থেকে নতুন করে অনুরোধ পাঠাতে দেয়।
    // ⛔ শুধু Master role — স্টাফের পথ ছোঁয়া হয়নি।
    // ⛔ শুধু "reopen request" — অন্য কোনো ধরনের অনুমতি-নোটিশ নয়।
    // ফেরত: যেসব id বন্ধ করা হলো, যাতে এই বারের রেন্ডার থেকেও সঙ্গে সঙ্গে বাদ যায়।
    private fun expireStaleReopenRequests(rawRows: org.json.JSONArray): Set<String> {
        if (!user.role.equals("master", ignoreCase = true)) return emptySet()
        val today = PatientIdGenerator.todayIso()
        val staleIds = mutableListOf<String>()
        try {
            for (i in 0 until rawRows.length()) {
                val row = rawRows.optJSONObject(i) ?: continue
                val id = row.optString("id")
                if (id.isBlank()) continue
                // 🔴🔒 V1638 (TK-রিপোর্ট, ২০.০৯.২০২৬ — "পরের দিন বন্ধ হচ্ছে না
                // কেন", App Version 16.36 নিশ্চিত করেই ধরা পড়ল) — আসল কারণ:
                // এখানে `row.optString("deletedAt")` ব্যবহার হয়েছিল, `row.s(...)`
                // নয়। বন্ধ-না-হওয়া প্রতিটা সারিতে `deletedAt` SQL NULL, আর
                // org.json-এর `optString()` সেই NULL-কে **আক্ষরিক "null" টেক্সট**
                // ফেরায় (এই প্রজেক্টেরই বহুবার-ধরা-পড়া ফাঁদ — JsonExt.kt-এর
                // মন্তব্যেই লেখা, V696/V812/V819-এ অন্য জায়গায় সারানো হয়েছিল,
                // এখানে ধরা পড়েনি)। "null".isNotBlank() সত্যি, তাই **প্রতিটা**
                // এখনো-বন্ধ-না-হওয়া অনুরোধই "আগেই বন্ধ" ভেবে বাদ পড়ে যেত —
                // কোনো Reopen-অনুরোধ কখনোই নিজে থেকে বন্ধ হতে পারত না। এখন
                // NULL-নিরাপদ `row.s("deletedAt")` — ফাঁকা/NULL দুটোতেই "".
                if (row.s("deletedAt").isNotBlank()) continue // আগেই বন্ধ
                val title = row.optString("title").lowercase()
                if (!title.contains("reopen request")) continue
                val createdDay = row.optString("createdAt").take(10)
                if (createdDay.isBlank() || createdDay >= today) continue // আজই পাঠানো — ছোঁয়া হয় না
                staleIds.add(id)
            }
        } catch (_: Throwable) { }
        if (staleIds.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                for (id in staleIds) {
                    try {
                        repository.addReply(this@BriefingActivity, id,
                            "⏳ Auto-closed — a new day started without a decision. Please send the request again if still needed.",
                            user.mobile)
                        repository.deleteOrHide(id, user)
                    } catch (_: Throwable) { }
                }
            }
        }
        return staleIds.toSet()
    }

    private fun autoClearActionlessForMaster(rawRows: org.json.JSONArray) {
        // 🔵 (07.08.2026, নিজের অডিটে ধরা) — শুধু "নোটিশ লিখতে" খোলা হলে
        // (NotificationsActivity-র ➕ → openCompose) ব্যবহারকারী তালিকাটা
        // পড়েনই না; তখন অটো-সিন/অটো-হাইড করলে না-পড়া নোটিশ হারিয়ে যেত।
        if (intent.getBooleanExtra("openCompose", false)) return
        /* 👁️ V753 — **তালিকাটা পড়া হয় এখানেই (মূল থ্রেডে), আলাদা থ্রেডে নয়।**
           কারণ ঠিক এই ফাংশনের পরের লাইনেই `parseForUser(rawRows)` দিয়ে পর্দা
           আঁকা হয়। আগে পুরো কাজটা `launch(Dispatchers.IO)`-র ভিতরে ছিল, তাই
           `seen`-এ নাম বসার **আগেই** পর্দা আঁকা হয়ে যেত ⇒ সংখ্যা পুরনোই থাকত।
           ⛔ এখানে **কোনো নেট-কল নেই** — শুধু JSON পড়া, তাই মূল থ্রেডে নিরাপদ।
              নেটের কাজগুলো (নিচে) আগের মতোই আলাদা থ্রেডেই হয়। */
        val seenIds = mutableListOf<String>()
        /** ⚡ V756 — প্রতিটা নোটিশের **আগের** seen-তালিকা, যাতে মেঘ থেকে আবার
         *  টানতে না হয় (`markSeenWithRow`)। */
        val seenPayload = HashMap<String, org.json.JSONArray>()
        val hideIds = mutableListOf<String>()
        val titleById = HashMap<String, String>()
        try {
                for (i in 0 until rawRows.length()) {
                    val row = rawRows.optJSONObject(i) ?: continue
                    val id = row.optString("id")
                    if (id.isBlank() || autoClearedThisSession.contains(id)) continue
                    if (BriefingModel.isDeletedForMe(row, user.mobile)) continue        // আগেই সরানো
                    /* 👁️🔒 V753 (২৭.০৮.২০২৬, TK-সিদ্ধান্ত "১" — *"seen by 0 কেন
                       দেখাচ্ছে? আমি নিজেই তো কয়েকবার সিন করেছি"*)

                       **আসল কারণ (কোড ধরে যাচাই):** `targetsHit()` শুধু তাঁকেই
                       মেলায় যাঁর উদ্দেশ্যে নোটিশটা পাঠানো — ব্রাঞ্চ/ভূমিকা/মোবাইল
                       ধরে। মাস্টারের ব্রাঞ্চ **"All"**, তাই "Birpara" বা
                       "Cooch Behar"-এর নোটিশে কখনোই মেলে না ⇒ তিনি যতবারই পড়ুন,
                       নাম বসত না, চিরকাল "Seen by 0" থাকত।

                       ⚠️ **শুধু মাস্টারের জন্যই বদল** — স্টাফের পথে এক অক্ষরও নয়।
                       ⛔ আর **শুধু "seen"** — তালিকা থেকে সরানো (`hideForMe`)
                          আগের মতোই শুধু target-করা নোটিশে; নইলে অন্য ব্রাঞ্চের
                          নোটিশ মাস্টারের চোখের আড়ালে চলে যেত। */
                    val isMasterUser = user.role.equals("master", ignoreCase = true)
                    val mine = BriefingModel.targetsHit(row, user.mobile, user.role, user.branch)
                    if (!mine && !isMasterUser) continue // আমাকে target নয় (নিজের পাঠানো) — বাদ
                    // (ক) সবগুলোই "seen" — ঘন্টা ও ১০-মিনিটের রিমাইন্ডার থামাতে।
                    /* ⚡🔒 V756 — **খরচের পাহারা (নিজের কাজ যাচাই করে ধরা)।**
                       V753-এ মাস্টারের জন্য সব নোটিশ "seen" হতে শুরু করেছিল।
                       কিন্তু নোটিশ **৫০০০ পর্যন্ত** আনা হয় (BriefingRepository),
                       আর প্রতি "seen"-এ নেট-কল হয় ⇒ Supabase free প্ল্যানে
                       বিপুল অকারণ খরচের ঝুঁকি ছিল।
                       এখন দুটো বাঁধন:
                         ১. যেগুলো **পর্দায় সত্যিই দেখা যায়** শুধু সেগুলোই
                            (`visibleForUser`) — অদৃশ্য পুরনো নোটিশ নয়।
                         ২. একবারে **সর্বোচ্চ ৬০টা** — বাকিগুলো পরের বার।
                       ⛔ TK-এর নিয়ম: *"supabase free প্ল্যানে ঝুঁকি থাকলে আগে
                          আমাকে বলবেন"* — তাই ঝুঁকিটা তৈরিই হতে দেওয়া হলো না। */
                    if (!BriefingModel.hasSeen(row, user.mobile) &&
                        seenIds.size < 60 &&
                        repository.visibleForUser(row, user)) {
                        // ⛔ মূল `seen` তালিকাটা **কপি করে** রাখি — ঠিক নিচেই
                        //    নামটা বসানো হয়, তাই পরে ব্যবহার করলে দুবার বসত।
                        val before = org.json.JSONArray()
                        try {
                            val cur = row.optJSONArray("seen") ?: org.json.JSONArray()
                            for (k in 0 until cur.length()) before.put(cur.get(k))
                        } catch (_: Throwable) { }
                        seenPayload[id] = before
                        seenIds.add(id)
                        /* 👁️ V753 — এই সারিটার নিজের `seen` তালিকাতেও **এখনই**
                           নামটা বসিয়ে দিই। কারণ ঠিক নিচেই `parseForUser(rawRows)`
                           দিয়ে পর্দাটা আঁকা হয় — মেঘে লেখা শেষ হওয়ার **আগেই**।
                           না বসালে এই বারের পর্দায় পুরনো সংখ্যাই থাকত (TK-এর
                           রিপোর্ট: *"আমি নিজেই তো কয়েকবার সিন করেছি"*)।
                           ⛔ শুধু এই বারের দেখানোর জন্য — আসল লেখা নিচের
                              `repository.markSeen()`-ই করে, তাই দুবার লেখা হয় না। */
                        try {
                            val arr = row.optJSONArray("seen") ?: org.json.JSONArray()
                            arr.put(BriefingModel.mob(user.mobile))
                            row.put("seen", arr)
                        } catch (_: Throwable) { }
                    }
                    // (খ) অ্যাকশন লাগে না এমনগুলোই শুধু নিজের তালিকা থেকে সরানো।
                    if (mine && !needsMasterApproval(row.optString("title"))) {
                        hideIds.add(id); titleById[id] = row.optString("title")
                    }
                }
        } catch (_: Throwable) { }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                for (id in seenIds) {
                    // ⚡ V756 — সারিটা হাতেই আছে, তাই মেঘ থেকে আর টানা হয় না
                    //    (আগে প্রতি নোটিশে ২টো নেট-কল হত, এখন ১টা)।
                    val before = seenPayload[id]
                    try {
                        if (before != null) repository.markSeenWithRow(id, before, user.mobile)
                        else repository.markSeen(id, user.mobile)
                    } catch (_: Throwable) { }
                }
                for (id in hideIds) {
                    autoClearedThisSession.add(id)
                    // 🔵 (07.08.2026, নিজের অডিটে ধরা) — "Staff IN TIME" ধরনের
                    // নোটিশ আগে Seen চাপলে **সম্পূর্ণ মুছে** যেত
                    // (AUTO_DELETE_ON_SEEN_TITLES → deleteOrHide)। Seen বোতাম
                    // তুলে দেওয়ায় সেই পথটা আর চলত না, ফলে নোটিশগুলো ডেটাবেসে
                    // জমতেই থাকত। এখন অটো-ক্লিয়ারেই আগের সেই একই নিয়ম চলে।
                    val title = titleById[id].orEmpty()
                    try {
                        if (title in AUTO_DELETE_ON_SEEN_TITLES) repository.deleteOrHide(id, user)
                        else repository.hideForMe(id, user.mobile)
                    } catch (_: Throwable) { }
                }
            } catch (_: Throwable) { }
        }
    }

    private fun markSeen(item: Briefing) {        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.markSeen(item.id, user.mobile) }
            var removed = false
            if (ok) {
                if (item.title in AUTO_DELETE_ON_SEEN_TITLES) {
                    // existing behaviour (e.g. "Staff IN TIME") — unchanged
                    withContext(Dispatchers.IO) { try { repository.deleteOrHide(item.id, user) } catch (_: Throwable) { } }
                    removed = true
                } else if (!needsMasterApproval(item.title)) {
                    // 🔒 TK-ORDER (2026-08-06): once seen, an ordinary notice
                    // disappears from THIS person's list only (per-user hide).
                    // Approval/payment requests are excluded above and stay
                    // until the Master approves/rejects them.
                    removed = withContext(Dispatchers.IO) {
                        try { repository.hideForMe(item.id, user.mobile) } catch (_: Throwable) { false }
                    }
                }
            }
            Toast.makeText(
                this@BriefingActivity,
                if (!ok) "Failed — check connection" else if (removed) "Seen — removed from your list" else "Marked as seen",
                Toast.LENGTH_SHORT
            ).show()
            if (ok) loadList()
        }
    }

    private fun showReplyDialog(item: Briefing) {
        val input = EditText(this).apply {
            hint = "Your reply"
            setPadding(40, 30, 40, 30)
        }
        UppercaseInputUtil.applyToAll(input)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Reply to: ${item.title.ifBlank { "Notice" }}"))
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isBlank()) {
                    Toast.makeText(this, "Reply cannot be empty", Toast.LENGTH_SHORT).show()
                    input.requestFocus()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { repository.addReply(this@BriefingActivity, item.id, text, user.mobile) }
                    Toast.makeText(this@BriefingActivity, if (ok) "Reply sent" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                    if (ok) loadList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // ────────────────────────────────────────────────────────────────
    // 🔒🔒 TK-ORDER (30.07.2026, ফটো-প্রুফ দেখে "ঠিক আছে" — শুধু Briefing-এর
    // জন্য, Follow-up-এর প্যাশেন্ট কার্ড নয়): "REPLY" চাপলে এখন থেকে ছোট
    // AlertDialog-এর বদলে WhatsApp-এর মতো পুরো-স্ক্রিন চ্যাট থ্রেড খোলে —
    // প্রতিটা কথা আলাদা বাবল (কে বলেছে বাঁয়ে, নিজেরটা ডানে), আর "Individual"
    // (একজন নির্দিষ্ট স্টাফের) নোটিশ হলে হেডারে 📞/📹 আইকন — সেই স্টাফের
    // WhatsApp চ্যাট খোলে (Voice/Video Call বোতাম দুটো WhatsApp-এর ভিতরেই,
    // কারণ WhatsApp সরাসরি কল-শুরুর কোনো পাবলিক লিংক দেয় না — এটা এই ফাইলের
    // অন্য জায়গায় ব্যবহৃত হওয়া wa.me লিংকেরই পুনর্ব্যবহার)।
    // ⛔ মূল তালিকার কার্ড (শিরোনাম/তারিখ/মেসেজ/Seen/Delete) এক অক্ষরও
    //    বদলায়নি — শুধু "REPLY" বোতামের গন্তব্য বদলেছে। ⛔ addReply() ও
    //    loadList() হুবহু আগের ফাংশন, নতুন কোনো ক্লাউড-কল নেই।
    // ────────────────────────────────────────────────────────────────

    private fun waTimeOnly(iso: String): String {
        if (iso.isBlank()) return ""
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (p in formats) {
            try {
                val d = java.text.SimpleDateFormat(p, java.util.Locale.US).parse(iso) ?: continue
                return java.text.SimpleDateFormat("h.mm a", java.util.Locale.US).format(d)
            } catch (_: Exception) { }
        }
        return ""
    }

    private fun showChatThread(item: Briefing) {
        val d = resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()

        // "Individual" নোটিশের একমাত্র লক্ষ্য-মোবাইলটা বার করা — এটা পাওয়া
        // গেলে ও StaffDirectory-তে সত্যিই কেউ মিলে গেলে তবেই কল/ভিডিও আইকন
        // দেখানো হয়; নইলে (Branch/Role/All Staff) আইকন দুটোই লুকানো থাকে,
        // কারণ তখন একটামাত্র নির্দিষ্ট মানুষ নেই।
        val targetMobile = item.raw.optJSONObject("targets")?.optJSONArray("mobiles")?.optString(0).orEmpty()
        val targetAccount = if (targetMobile.isNotBlank()) StaffDirectory.findAccount(targetMobile) else null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#EEF2F5"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }

        // ── হেডার ──
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(px(16), px(18), px(14), px(16))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(android.graphics.Color.parseColor("#1560C7"), android.graphics.Color.parseColor("#1E9B6A"))
            )
        }
        lateinit var chatDialog: AlertDialog
        header.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, px(14), 0)
            setOnClickListener { chatDialog.dismiss() }
        })
        header.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@BriefingActivity).apply {
                text = targetAccount?.name?.ifBlank { item.title.ifBlank { "Notice" } } ?: item.title.ifBlank { "Notice" }
                textSize = 17f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(android.graphics.Color.WHITE)
            })
            val subText = if (targetAccount != null) "${targetAccount.branch} · ${targetAccount.role.replaceFirstChar { it.uppercase() }}" else item.targetsSummary
            addView(TextView(this@BriefingActivity).apply {
                text = subText; textSize = 12f; setTextColor(android.graphics.Color.parseColor("#E4EEF9"))
            })
        })
        if (targetAccount != null) {
            val digits = targetAccount.mobile.filter { it.isDigit() }.takeLast(10)
            header.addView(TextView(this).apply {
                text = "📞"; textSize = 20f; setPadding(px(6), 0, px(14), 0)
                setOnClickListener {
                    // ⚠️ সৎ কথা: WhatsApp সরাসরি "কল শুরু করো" বলে কোনো
                    // পাবলিক লিংক দেয় না — এটা ওই স্টাফের WhatsApp চ্যাট
                    // খুলে দেয়, সেখান থেকে WhatsApp-এর নিজের 📞 বোতামে
                    // একটা মাত্র বাড়তি চাপ লাগবে।
                    // 🔒 V235 (TK, WhatsApp Chooser project-wide): কেন্দ্রীয় chooser (Personal/Business)।
                    WhatsAppMessageChooser.send(this@BriefingActivity, digits)
                }
            })
            header.addView(TextView(this).apply {
                text = "📹"; textSize = 20f
                setOnClickListener {
                    // 🔒 V235 (TK, WhatsApp Chooser project-wide): কেন্দ্রীয় chooser (Personal/Business)।
                    WhatsAppMessageChooser.send(this@BriefingActivity, digits)
                }
            })
        }
        root.addView(header)

        // ── চ্যাট বডি (স্ক্রল) ──
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(12), px(14), px(12), px(14))
        }
        scroll.addView(body)
        root.addView(scroll)

        // 🔴 B335 (03.08.2026, TK-রিপোর্ট — চ্যাট বাবলে লেখা "ভালো ভাবে সাজানো
        // নেই"): title ("Payment deleted") আর message (বিস্তারিত বর্ণনা) একই
        // প্লেইন TextView-এ শুধু একটা "\n" দিয়ে জোড়া ছিল — কোনো bold/আলাদা
        // আকার না থাকায় শিরোনাম আর বিবরণ একটাই প্যারাগ্রাফের মতো দেখাত।
        // এখন `title` আলাদা প্যারামিটার — নিজের bold লাইন পায় (senderLabel-এর
        // মতোই আলাদা স্টাইল), body শুরু হয় তার ঠিক নিচে থেকে।
        fun bubble(senderLabel: String, title: String, text: String, whenIso: String, mine: Boolean) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = if (mine) android.view.Gravity.END else android.view.Gravity.START
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = px(10)
                }
            }
            val bubbleBg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = px(14).toFloat()
                setColor(android.graphics.Color.parseColor(if (mine) "#DCF3E6" else "#F1F2F6"))
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = bubbleBg
                setPadding(px(12), px(8), px(12), px(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val maxBubbleWidth = (resources.displayMetrics.widthPixels * 0.78).toInt()
            col.addView(TextView(this).apply {
                this.text = senderLabel; textSize = 11.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(if (mine) "#1E9B6A" else "#1560C7"))
                maxWidth = maxBubbleWidth
            })
            if (title.isNotBlank()) {
                col.addView(TextView(this).apply {
                    this.text = title; textSize = 15.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#1C2530"))
                    maxWidth = maxBubbleWidth
                    setPadding(0, px(2), 0, px(1))
                })
            }
            col.addView(TextView(this).apply {
                this.text = text; textSize = 15f; setTextColor(android.graphics.Color.parseColor("#1C2530"))
                maxWidth = maxBubbleWidth
            })
            val timeText = waTimeOnly(whenIso)
            if (timeText.isNotBlank()) {
                col.addView(TextView(this).apply {
                    this.text = timeText; textSize = 10.5f; setTextColor(android.graphics.Color.parseColor("#8A93A6"))
                    gravity = android.view.Gravity.END
                    maxWidth = maxBubbleWidth
                })
            }
            row.addView(col)
            body.addView(row)
        }

        // মূল নোটিশটাই প্রথম বার্তা — যিনি পোস্ট করেছিলেন তাঁর তরফ থেকে।
        val noticeIsMine = BriefingModel.mob(item.createdBy) == BriefingModel.mob(user.mobile)
        val noticeSender = if (noticeIsMine) "You" else (StaffDirectory.findAccount(item.createdBy)?.name ?: "Master")
        bubble(noticeSender, item.title, item.message, item.date, noticeIsMine)

        for (r in item.replies) {
            val mine = BriefingModel.mob(r.by) == BriefingModel.mob(user.mobile)
            val label = if (mine) "You" else (StaffDirectory.findAccount(r.by)?.name ?: r.by)
            bubble(label, "", r.text, r.at, mine)
        }

        // ── নিচের ইনপুট বার ──
        val inputBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(px(10), px(8), px(10), px(10))
            setBackgroundColor(android.graphics.Color.parseColor("#EEF2F5"))
        }
        val replyInput = EditText(this).apply {
            hint = "Type a reply…"
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val padH = px(16); val padV = px(10)
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        UppercaseInputUtil.applyToAll(replyInput)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL
        val sendBtn = TextView(this).apply {
            text = "➤"; textSize = 20f; setTextColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#1E9B6A"))
            }
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(px(44), px(44)).apply { marginStart = px(10) }
        }
        sendBtn.setOnClickListener {
            val text = replyInput.text.toString().trim()
            if (text.isBlank()) { replyInput.requestFocus(); return@setOnClickListener }
            sendBtn.isEnabled = false
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { repository.addReply(this@BriefingActivity, item.id, text, user.mobile) }
                sendBtn.isEnabled = true
                if (ok) {
                    replyInput.setText("")
                    bubble("You", "", text, isoNowForBriefing(), true)
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                    loadList()
                } else {
                    Toast.makeText(this@BriefingActivity, "Failed — check connection", Toast.LENGTH_SHORT).show()
                }
            }
        }
        inputBar.addView(replyInput)
        inputBar.addView(sendBtn)
        root.addView(inputBar)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24)
        chatDialog = AlertDialog.Builder(this).setView(root).create()
        chatDialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        chatDialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(chatDialog) } catch (_: Throwable) { }   // 🤫 V774
        chatDialog.window?.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun isoNowForBriefing(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())

    // 🔒 TK-নির্দেশ (06.08.2026, খাতার সারি B483) — Post Notice-এ Title
    // ঘর তুলে দেওয়ার পরে, মেসেজের প্রথম কয়েক শব্দ থেকে শিরোনাম বানায়।
    // ৬টা শব্দ বা ৪০ অক্ষর — যেটা আগে আসে; ছাঁটা হলে "…" যোগ হয়। ⛔ এটা
    // শুধু দেখানোর শিরোনাম — আসল বার্তা (message) সম্পূর্ণ অক্ষত সেভ হয়।
    private fun buildAutoTitle(message: String): String {
        val words = message.trim().split(Regex("\\s+"))
        var title = words.take(6).joinToString(" ")
        if (title.length > 40) title = title.take(40)
        val truncated = title.length < message.trim().length
        return if (truncated) "$title…" else title
    }

    private fun showPostDialog() {
        // 🔒 B579 (TK-অনুমোদিত প্রুফ, 08.08.2026): Post Notice আরও প্রফেশনাল —
        // পরিষ্কার লেবেল (MESSAGE / SEND TO) + রাউন্ড-বর্ডার মেসেজ ঘর + ফাঁকা।
        // ⛔ পোস্ট করার লজিক/টার্গেট নির্বাচন কিছুই বদলায়নি — শুধু চেহারা।
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        fun fieldLabel(t: String, topGap: Int = 4) = TextView(this).apply {
            text = t; textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#5A6472"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.04f
            setPadding(dp(2), dp(topGap), 0, dp(5))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), 0)
        }
        // 🔒 TK-নির্দেশ (06.08.2026, খাতার সারি B483) — সাধারণ স্টাফ নিজে
        // লিখে পাঠানোর সময় Title ঘরটা বিভ্রান্তিকর লাগছিল ("আমি একজন
        // সাধারণ ব্যবহারকারী... টাইটেল থাকলে আমি বিভ্রান্ত হই")। ⛔
        // সিস্টেম-জেনারেট নোটিশ (Delete/Refund/Payment deleted ইত্যাদি,
        // কোড থেকে সরাসরি `BriefingRepository.post()` ডাকে) এই পপ-আপ দিয়ে
        // যায়ই না, তাই ওগুলোর নিজস্ব শিরোনাম আগের মতোই অক্ষত। এখানে শুধু
        // Title-এর আলাদা বাক্সটা তুলে দেওয়া হলো — মেসেজের প্রথম কয়েক শব্দ
        // থেকেই (৬টা শব্দ বা ৪০ অক্ষর, যেটা আগে আসে) স্বয়ংক্রিয়ভাবে
        // শিরোনাম তৈরি হয় (`buildAutoTitle()`), TK-কে আলাদা করে কিছু
        // টাইপ করতে হয় না। ⛔ `BriefingRepository.post()`-এর স্বাক্ষর/
        // ডেটাবেস-কলাম কিছুই বদলায়নি — শুধু এই একটা কল-সাইট থেকে কী
        // পাঠানো হচ্ছে সেটাই বদলাল।
        container.addView(fieldLabel("MESSAGE", 2))
        val messageInput = EditText(this).apply {
            hint = "Type your notice here…"
            setHintTextColor(android.graphics.Color.parseColor("#9AA4B0"))
            setTextColor(android.graphics.Color.parseColor("#16233A"))
            textSize = 14f
            background = androidx.core.content.ContextCompat.getDrawable(this@BriefingActivity, com.tkbiswas.pilesclinic.R.drawable.bg_input_rounded)
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            setPadding(dp(12), dp(11), dp(12), dp(11))
            minHeight = dp(82)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }
        container.addView(messageInput)

        container.addView(fieldLabel("SEND TO", 14))
        val targetOptions = listOf("All Staff", "A Branch", "A Role", "An Individual")
        val targetSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@BriefingActivity, android.R.layout.simple_spinner_dropdown_item, targetOptions)
        }
        container.addView(targetSpinner)

        val branchSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@BriefingActivity, android.R.layout.simple_spinner_dropdown_item, branches)
            visibility = View.GONE
        }
        container.addView(branchSpinner)
        val roleSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@BriefingActivity, android.R.layout.simple_spinner_dropdown_item, roles)
            visibility = View.GONE
        }
        container.addView(roleSpinner)

        val staffAccounts = StaffDirectory.allAccounts().filter { it.role == "staff" || it.role == "doctor" || it.role == "field" }
        val staffLabels = staffAccounts.map { "${it.name} (${it.branch})" }
        val individualSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@BriefingActivity, android.R.layout.simple_spinner_dropdown_item,
                if (staffLabels.isEmpty()) listOf("No staff found") else staffLabels)
            visibility = View.GONE
        }
        container.addView(individualSpinner)

        targetSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                branchSpinner.visibility = if (pos == 1) View.VISIBLE else View.GONE
                roleSpinner.visibility = if (pos == 2) View.VISIBLE else View.GONE
                individualSpinner.visibility = if (pos == 3) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        UppercaseInputUtil.applyToAll(container)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val postDlg = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "🔔 Post Notice"))
            .setView(ScrollView(this).apply { addView(container) })
            .setPositiveButton("Post", null)
            .setNegativeButton("Cancel", null)
            .create()
        // TK-DECISION (2026-07-22): don't auto-close on Post -- so a missing
        // Title/Message is shown red (FieldError) with the dialog still open.
        postDlg.setOnShowListener {
            postDlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val message = messageInput.text.toString().trim()
                val vmsg = FieldError.validate(listOf<Triple<View, Boolean, String>>(
                    Triple(messageInput, message.isNotBlank(), "Message দিন")
                ))
                if (vmsg != null) {
                    Toast.makeText(this, vmsg, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val title = buildAutoTitle(message)
                val target: String
                val branch: String
                val role: String
                var targetMobile = ""
                when (targetSpinner.selectedItemPosition) {
                    1 -> { target = "branch"; branch = branchSpinner.selectedItem.toString(); role = "" }
                    2 -> { target = "role"; branch = user.branch; role = roleSpinner.selectedItem.toString() }
                    3 -> {
                        target = "individual"; branch = user.branch; role = ""
                        targetMobile = staffAccounts.getOrNull(individualSpinner.selectedItemPosition)?.mobile ?: ""
                        if (targetMobile.isBlank()) {
                            Toast.makeText(this, "Select a staff member", Toast.LENGTH_SHORT).show()
                            individualSpinner.requestFocus()
                            return@setOnClickListener
                        }
                    }
                    else -> { target = "allStaff"; branch = user.branch; role = "" }
                }
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        repository.post(this@BriefingActivity, title, message, target, branch, role, user.mobile, targetMobile)
                    }
                    Toast.makeText(this@BriefingActivity, if (ok) "Notice posted" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                    if (ok) { postDlg.dismiss(); loadList() }
                }
            }
        }
        postDlg.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(postDlg) } catch (_: Throwable) { }   // 🤫 V774
        // 🔒🔒 খাতার সারি B181 (TK, 30.07.2026 — "সম্পূর্ণ প্রজেক্ট ব্যবহার
        // করার সময় কেন এখনো অনেক জায়গায় বাংলা আসছে?")। **আসল কারণ:** পপ-আপের
        // নিজের আলাদা উইন্ডো থাকে, তাই পর্দার সাধারণ পাহারা সেখানে পৌঁছায় না —
        // শুধু `PremiumAlert.paint()` ডাকলেই `NoBengali.installDialog()` চালু
        // হয়। এই পপ-আপে এতদিন এই ডাকটাই ছিল না।
        PremiumAlert.paint(postDlg)
    }
}
