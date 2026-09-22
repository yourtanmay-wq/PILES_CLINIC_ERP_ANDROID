package com.tkbiswas.pilesclinic.native

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityDraftBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Native rebuild -- Draft home. Shows the five draffHome() categories with live
 * counts; tapping one opens its list. A filter row (All / This Month / Custom
 * Date) narrows every category by record date.
 */
class DraftActivity : AppCompatActivity() {

    // TK-REQUESTED PROACTIVE FIX (2026-07-25): the same overlapping.refresh
    // guard already proven on Follow-up and Chamber Attendance. Two loads can
    // overlap (screen reopened, an action refreshing, a slow first fetch
    // finishing late) . without this the OLDER result could land last and
    // overwrite fresh data on screen. Only the newest load may paint now.
    private var loadGuardToken = 0

    private lateinit var binding: ActivityDraftBinding
    private lateinit var repository: DraftRepository
    private lateinit var user: NativeUser
    private var buckets: DraftBuckets? = null

    private var fromDate: String? = null
    private var toDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDraftBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        repository = DraftRepository(this)
        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        binding.btnBack.setOnClickListener { finish() }
        binding.btnReceived.setOnClickListener { showList("My Enquiry (All Branch)", "received", buckets?.received) }
        binding.btnEnqReject.setOnClickListener { showList("Enquiry Reject List", "enqReject", buckets?.enqReject) }
        binding.btnVisitReject.setOnClickListener { showList("Visit Reject List", "visitReject", buckets?.visitReject) }
        binding.btnNotComplete.setOnClickListener { showList("Incomplete Patient", "notComplete", buckets?.notComplete) }
        // 🟢🔒 V644 (২৫.০৮.২০২৬, TK-নির্দেশ) — একই প্যাটার্ন, নতুন bucket।
        binding.btnRunningTreatment.setOnClickListener { showList("Running Patient", "runningTreatment", buckets?.runningTreatment) }
        binding.btnComplete.setOnClickListener { showList("Complete Patient", "complete", buckets?.complete) }
        binding.btnUnexpectedTime.setOnClickListener { showList("Unexpected Time Calls", "unexpectedTime", buckets?.unexpectedTime) }
        /* 📊🔒 V824 (২৯.০৮.২০২৬, TK-নির্দেশ: *"শুধুমাত্র মাস্টারের জন্য…
           আমার যে ব্রাঞ্চ সিলেক্ট করা থাকবে শুধুমাত্র সেই ব্রাঞ্চের"*) —
           "Yearly Registration"। ⛔ স্টাফ ও ডাক্তারের পর্দায় সারিটা
           একেবারেই থাকে না (XML-এ ডিফল্ট `gone`), তাই তাঁদের জন্য এক
           অক্ষরও বদলায়নি। */
        if (user.role == "master") {
            binding.btnYearlyReg.visibility = View.VISIBLE
            binding.tvYearlyRegSub.text = YearlyRegistration.currentYear()
            binding.btnYearlyReg.setOnClickListener { openYearlyReg() }
        } else {
            binding.btnYearlyReg.visibility = View.GONE
        }
        binding.btnRefunded.setOnClickListener { showList("Refunded", "refunded", buckets?.refunded) }
        // 🟢🔒 V621 (২৪.০৮.২০২৬, TK-নির্দেশ) — একই প্যাটার্ন, নতুন bucket।
        binding.btnReturnVisit.setOnClickListener { showList("Return Visit List", "returnVisit", buckets?.returnVisit) }

        // TK-REPORTED (2026-07-18): these 6 buttons were still showing the
        // app theme's default navy (never explicitly styled, unlike the
        // popups fixed earlier this session). Each gets its own distinct
        // color now — text/icon/click/order all unchanged.
        // 🎨 (07.08.2026, প্রফেশনাল কার্ড-ডিজাইন) — সাতটা ঘর আর ভরাট-রঙের
        // `Button` নয়, সাদা কার্ড; তাই এখানে রং বসানোর দরকার নেই — প্রতিটার
        // নিজের রং (হুবহু আগের সাতটাই) এখন XML-এ বাঁ-দাগ · আইকন-বাক্স ·
        // সংখ্যার রঙে বসানো আছে (activity_draft.xml)।
        // ⛔ পুরনো কোড রেফারেন্সের জন্য রইল:
        //   tint(btnReceived "#1D6FE0") · tint(btnEnqReject "#B8324A")
        //   tint(btnVisitReject "#C9702E") · tint(btnNotComplete "#B8860B")
        //   tint(btnComplete "#0C9E33") · tint(btnUnexpectedTime "#6A4C93")
        //   tint(btnRefunded "#8A5A2E")

        binding.chipAll.setOnClickListener { setFilterAll() }
        binding.chipMonth.setOnClickListener { setFilterMonth() }
        binding.chipCustom.setOnClickListener { pickCustomRange() }

        setupBranchPicker { load() }   // খাতার সারি B43

        load()
    }

    private var firstResume = true
    override fun onResume() {
        super.onResume()
        if (firstResume) { firstResume = false; return }
        load()
    }


    /**
     * 🔒 TK-REQUESTED (28.07.2026, খাতার সারি B43): *"আমি মাস্টার এডমিন — সমস্ত
     * প্রজেক্টের সব জায়গায় আমি চাইলে ব্রাঞ্চ চুজ করতে পারি, চাইলে অল ব্রাঞ্চ
     * রাখতে পারি।"*
     *
     * ⛔ Follow-up পর্দায় TK-এর আগেই পাশ করা **হুবহু একই বাক্স ও একই নিয়ম** —
     * নতুন কোনো ডিজাইন তৈরি করা হয়নি।
     * ⛔ **স্টাফ ও ডাক্তারের জন্য এক অক্ষরও বদলায়নি** — বাক্সটাই তাঁদের পর্দায়
     * থাকে না, তাঁরা আগের মতোই শুধু নিজের ব্রাঞ্চ দেখেন।
     */
    private var pickedBranch: String = ""

    /** কোন ব্রাঞ্চের তথ্য দেখানো হবে — একটাই জায়গা থেকে সিদ্ধান্ত, তাই জমানো ও
     *  নতুন তালিকা কখনো আলাদা ব্রাঞ্চ দেখাতে পারবে না। */
    /** 🟢🔒 V398: মাস্টার কিছু না-বাছলে "" ফেরে — তখন কিচ্ছু আনা হয় না। */
    private fun shownBranch(): String =
        if (user.role == "master") pickedBranch else user.branch

    private fun setupBranchPicker(onPicked: () -> Unit) {
        if (user.role != "master") {
            binding.branchPicker.visibility = View.GONE
            return
        }
        // 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): বাছাইটা আর এই পর্দার নিজের নয় —
        //   পুরো অ্যাপের একটাই জায়গা `BranchFilterStore`। তাই একবার বাছলেই সব
        //   পর্দায় সেটাই থাকে, বারবার বাছতে হয় না।
        pickedBranch = BranchFilterStore.get(this)
        binding.branchPicker.visibility = View.VISIBLE
        binding.branchPicker.text = BranchFilterStore.pillText(this)
        binding.branchPicker.setOnClickListener {
            val branches = BranchFilterStore.choices()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(branches.toTypedArray(), BranchFilterStore.indexInChoices(this)) { dialog, which ->
                    pickedBranch = branches[which]
                    BranchFilterStore.set(this@DraftActivity, pickedBranch)
                    binding.branchPicker.text = BranchFilterStore.pillText(this@DraftActivity)
                    dialog.dismiss()
                    onPicked()
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    private fun highlight(active: android.widget.TextView) {
        listOf(binding.chipAll, binding.chipMonth, binding.chipCustom).forEach {
            val on = it === active
            it.setBackgroundResource(if (on) com.tkbiswas.pilesclinic.R.drawable.bg_chip_seg_on else com.tkbiswas.pilesclinic.R.drawable.bg_chip_seg)
            it.setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#6B4A1E"))
        }
    }

    private fun setFilterAll() {
        fromDate = null; toDate = null
        highlight(binding.chipAll)
        binding.tvFilterInfo.visibility = View.GONE
        load()
    }

    private fun setFilterMonth() {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        toDate = fmt.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        fromDate = fmt.format(cal.time)
        highlight(binding.chipMonth)
        showFilterInfo()
        load()
    }

    private fun pickCustomRange() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val f = Calendar.getInstance().apply { set(y, m, d) }
            fromDate = fmt.format(f.time)
            DatePickerDialog(this, { _, y2, m2, d2 ->
                val t = Calendar.getInstance().apply { set(y2, m2, d2) }
                toDate = fmt.format(t.time)
                highlight(binding.chipCustom)
                showFilterInfo()
                load()
            }, y, m, d).apply { setTitle("To date"); datePicker.minDate = f.timeInMillis }.show()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply { setTitle("From date") }.show()
    }

    private fun showFilterInfo() {
        binding.tvFilterInfo.visibility = View.VISIBLE
        binding.tvFilterInfo.text = "Filter: ${fromDate?.let { DateUtil.display(it) } ?: "…"} → ${toDate?.let { DateUtil.display(it) } ?: "…"}"
    }

    private fun renderBuckets(b: DraftBuckets) {
        buckets = b
        // 🎨 (07.08.2026, প্রফেশনাল কার্ড-ডিজাইন) — সংখ্যাটা এখন কার্ডের ডান পাশের
        // নিজস্ব ঘরে বসে; নাম/আইকন/ব্যাখ্যা XML-এ স্থির। আগে পুরো লেখাটাই এখানে
        // বসত (যেমন "📥 My Enquiry (All Branch) — 141")।
        // ⛔ কোন সংখ্যা কোথা থেকে আসছে — একটুও বদলায়নি, হুবহু একই তালিকা।
        binding.tvCountReceived.text = b.received.size.toString()
        binding.tvCountEnqReject.text = b.enqReject.size.toString()
        binding.tvCountVisitReject.text = b.visitReject.size.toString()
        binding.tvCountNotComplete.text = b.notComplete.size.toString()
        // 🟢🔒 V644 (২৫.০৮.২০২৬, TK-নির্দেশ) — একই প্যাটার্ন, নতুন bucket।
        binding.tvCountRunningTreatment.text = b.runningTreatment.size.toString()
        binding.tvCountComplete.text = b.complete.size.toString()
        binding.tvCountUnexpected.text = b.unexpectedTime.size.toString()
        binding.tvCountRefunded.text = b.refunded.size.toString()
        // 🟢🔒 V621 — একই প্যাটার্ন।
        binding.tvCountReturnVisit.text = b.returnVisit.size.toString()
        // 📊🔒 V824 — একই প্যাটার্ন, নতুন ঘর (শুধু মাস্টারের পর্দায় দেখা যায়)।
        binding.tvCountYearlyReg.text = YearlyRegistration.countedOf(b.yearlyReg).toString()
    }

    private fun load() {
        val myLoadToken = ++loadGuardToken
        // 🟢🔒 V398: মাস্টার এখনো ব্রাঞ্চ না-বাছলে **একটাও ক্লাউড-অনুরোধ যাবে না** —
        //   সব ঘরে ০ দেখাবে আর ছোট্ট একটা বার্তা আসবে। ⛔ স্টাফ/ডাক্তারের ক্ষেত্রে
        //   এই শর্ত কখনো সত্য হয় না (তাঁদের নিজের ব্রাঞ্চই ব্যবহার হয়)।
        if (BranchFilterStore.notChosen(this, user)) {
            binding.progressLoad.visibility = View.GONE
            renderBuckets(DraftBuckets(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            android.widget.Toast.makeText(this, NoBengali.s("উপরে ডান দিকে Branch বাছুন"), android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        // TK-REQUESTED ADDITION (2026-07-20): same "show what was already on
        // the phone instantly" pattern added to the other screens today.
        var hadCache = false
        // ⚡ TK (28.07.2026): নিজের ফোনে করা এনকোয়ারি সঙ্গে সঙ্গে দেখাতে হবে।
        val cached = repository.loadCachedBuckets(shownBranch(), fromDate, toDate, user.mobile)   // খাতার সারি B43
        if (cached != null) {
            hadCache = true
            binding.progressLoad.visibility = View.GONE
            renderBuckets(cached)
        } else {
            binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
            // 🔴🔒 V1600 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ — "বীরপাড়া তালিকায়
            // কোচবিহারের রোগী কেন") — আসল কারণ: ব্রাঞ্চ বদলে এই নতুন ব্রাঞ্চের
            // কোনো ক্যাশ না থাকলে এখানে `renderBuckets()` ডাকা হতো না, তাই
            // পুরনো ব্রাঞ্চের `buckets` (গণনা ও তালিকা দুটোই) মেমোরিতে থেকেই
            // যেত। ততক্ষণে উপরের পিল/গণনা তাজা ব্রাঞ্চের নাম দেখাত, আর কেউ
            // ঠিক এই ফাঁকে কোনো তালিকার টাইলে চাপলে `showList()` পুরনো
            // ব্রাঞ্চের কার্ডই খুলে দিত (branch=নতুন কিন্তু কার্ড=পুরনো)।
            // এখন এই ফাঁকে buckets খালি দেখানো হয় — তাজা ডেটা এলেই
            // renderBuckets(b) আবার ঠিকভাবে ভরে দেবে (নিচে, ২৫৯ নং লাইন)।
            renderBuckets(DraftBuckets(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
        }
        lifecycleScope.launch {
            val guardAtStart = myLoadToken
            // TK-REQUESTED FIX (2026-07-20): this had no error handling --
            // needed so a fetch failure after cached counts were shown
            // leaves them on screen instead of crashing/going blank.
            val b = try {
                withContext(Dispatchers.IO) { repository.load(shownBranch(), fromDate, toDate) }   // খাতার সারি B43
            } catch (t: Throwable) {
                null
            }
            // stale load (a newer one started while this was in flight): stop here
            if (guardAtStart != loadGuardToken) return@launch
            binding.progressLoad.visibility = View.GONE
            if (b == null) {
                if (!hadCache) Toast.makeText(this@DraftActivity, "Could not load — check connection and try again", Toast.LENGTH_SHORT).show()
                return@launch
            }
            renderBuckets(b)
        }
    }

    /**
     * 📊🔒 V824 — বিস্তারিত পর্দা: মাসভিত্তিক হিসাব + রোগীর তালিকা, পাশে
     * Skip / Undo। ⛔ তালিকাটা **ইতিমধ্যে হিসাব-করা** সারি নিয়েই যায় —
     * নতুন কোনো ক্লাউড-পড়া হয় না।
     */
    private fun openYearlyReg() {
        val rows = buckets?.yearlyReg
        if (rows == null) {
            Toast.makeText(this, "Still loading — please wait", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            android.content.Intent(this, YearlyRegistrationActivity::class.java)
                .putExtra("branch", shownBranch())
                .putExtra("year", YearlyRegistration.currentYear())
                .putExtra("entries", ArrayList(rows))
                // 🆕 V852 — "কতজন বাদ পড়ল ও কেন" (তালিকাতেও নেই, গোনাতেও নেই)।
                .putExtra("outDemo", buckets?.yearlyOutDemo ?: 0)
                .putExtra("outNoDate", buckets?.yearlyOutNoDate ?: 0)
                // 🆕🔒 V-YEARSW (১৬.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — বছর-বদলের
                // বোতামে কোন কোন বছর দেখাবে (ছোট্ট তালিকা, শুধু "2026","2025" ধরনের
                // স্ট্রিং — ভারী কিছু নয়, Intent-সীমার কোনো ঝুঁকি নেই)।
                .putExtra("availableYears", ArrayList(
                    buckets?.yearlyAvailableYears?.ifEmpty { listOf(YearlyRegistration.currentYear()) }
                        ?: listOf(YearlyRegistration.currentYear())))
        )
    }

    // V215 (§18): `bucket` + branch/date এখন সঙ্গে পাঠানো হয় যাতে DraftListActivity
    // pull-to-refresh-এ সত্যিকারের নতুন data (repository.load) আনতে পারে — শুধু পুরোনো
    // memory তালিকা আবার আঁকা নয় (§18.6)। পুরোনো আচরণ অক্ষত: extra না পেলে সে
    // আগের মতোই snapshot দেখায়।
    private fun showList(title: String, bucket: String, entries: List<DraftEntry>?) {
        if (entries == null) return
        // V1521 — even a currently-empty bucket must open. Otherwise the user
        // could never reach ⋮ → Year to inspect an older year when This Month
        // happens to contain zero rows. The detail screen still shows the same
        // "No records" state until a year is chosen.
        startActivity(
            android.content.Intent(this, DraftListActivity::class.java)
                .putExtra("title", title)
                .putExtra("bucket", bucket)
                .putExtra("branch", shownBranch())
                .putExtra("from", fromDate)
                .putExtra("to", toDate)
                .putExtra("entries", ArrayList(entries))
        )
    }
}
