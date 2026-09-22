package com.tkbiswas.pilesclinic.native

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.databinding.ActivityDraftListBinding
import com.tkbiswas.pilesclinic.print.BranchCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One Draft category as a scrollable list of follow-up-style cards, each with
 * Call / WhatsApp / View / Restore. Restore hands the record back to the exact
 * section it came from (web restoreDraftEntry) and removes it from this list.
 */
class DraftListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDraftListBinding
    private val repository = DraftRepository(this)
    private val entries = ArrayList<DraftEntry>()
    private lateinit var user: NativeUser
    // 🔒 খাতার সারি B192: হেডারে দেখানো টাইটেলটাই Sheet-ফাইলের নামেও ব্যবহার
    // হয় (যেমন "Incomplete Patient"), তাই ক্লাস-লেভেলে ধরে রাখা হলো।
    private var screenTitle = "Draft"
    // ⛔ দুবার চাপ পড়লেও একবারই চলে — Follow-up-এর `sheetBusy`-র হুবহু একই নিয়ম।
    private var sheetBusy = false
    // V215 (§18): কোন bucket ও কোন branch/date দিয়ে খোলা হয়েছে — pull-to-refresh-এ
    // সত্যিকারের নতুন data আনতে লাগে। না থাকলে (পুরোনো caller) আগের আচরণ অক্ষত।
    private var bucketKey: String = ""
    private var branchArg: String? = null
    private var fromArg: String? = null
    private var toArg: String? = null
    // V1521 — Year filter is an overlay on the existing Draft date filter.
    // BASE keeps the caller's All/This Month/Custom range exactly as it was;
    // YEAR temporarily replaces it with Jan-01..Dec-31; ALL YEARS removes only
    // the date range. Branch/bucket/restore/delete/payment logic is untouched.
    private var baseFromArg: String? = null
    private var baseToArg: String? = null
    private var selectedYear: Int? = null
    private var allYearsSelected: Boolean = false
    // 🆕🔒 V1650 (২১.০৯.২০২৬, TK-নির্দেশ) — "My Enquiry (All Branch)"-এ ইতিমধ্যে
    // পর্দায় থাকা তালিকাকে ব্রাঞ্চ ধরে আলাদা করে দেখানো (কোনো নতুন ক্লাউড-পড়া
    // নেই) — শুধু client-side ফিল্টার। null/"All" = সব দেখাবে (আগের মতোই)।
    private var myEnqBranchFilter: String? = null

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TK-REPORTED (2026-07-27): this screen has no bottom bar, so until
        // now opening it never retried a save that was still stuck on this
        // phone. A staff member could sit here while a registration or a
        // payment stayed unsent. Same retry every other screen already does.
        try { BottomNav.retryStuckSaves(this) } catch (_: Throwable) { }
        binding = ActivityDraftListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        val title = intent.getStringExtra("title") ?: "Draft"
        screenTitle = title
        bucketKey = intent.getStringExtra("bucket") ?: ""
        branchArg = intent.getStringExtra("branch")
        fromArg = intent.getStringExtra("from")
        toArg = intent.getStringExtra("to")
        baseFromArg = fromArg
        baseToArg = toArg
        val incoming = (intent.getSerializableExtra("entries") as? ArrayList<DraftEntry>) ?: arrayListOf()
        entries.addAll(incoming)

        binding.tvTitle.text = "$title (${entries.size})"
        binding.btnBack.setOnClickListener { finish() }
        // 🔒 TK-APPROVED (30.07.2026 সন্ধ্যা, ফটো-প্রুফে "ঠিক আছে" · খাতার
        // সারি B192): ছয়টা Draft সেকশনের সবগুলোতেই এই একটা বোতাম কাজ করে,
        // কারণ ছয়টাই এই একটা পর্দা দিয়ে খোলে। Follow-up-এর "⬇ Sheet"
        // বোতামের (খাতার সারি B69) হুবহু একই আচরণ — চাপলে এই স্ক্রিনে তখন
        // যা দেখা যাচ্ছে ঠিক তাই CSV হয়ে নামে, তারপর ফোনের চেনা Share/Save
        // পর্দা খোলে।
        binding.btnSheet.setOnClickListener { downloadSheet() }
        setupMoreMenu()

        setupBranchPicker()
        setupMyEnquiryBranchFilter()
        wirePullToRefresh()
        renderList()
    }

    private fun effectiveRange(): Pair<String?, String?> {
        if (allYearsSelected) return Pair(null, null)
        val y = selectedYear
        if (y != null) return Pair(String.format(java.util.Locale.US, "%04d-01-01", y), String.format(java.util.Locale.US, "%04d-12-31", y))
        return Pair(baseFromArg, baseToArg)
    }

    private fun updateHeaderTitle(count: Int = entries.size) {
        val suffix = when {
            allYearsSelected -> " · All Years"
            selectedYear != null -> " · ${selectedYear}"
            else -> ""
        }
        binding.tvTitle.text = "$screenTitle$suffix ($count)"
    }

    /** V1521 — top-right ⋮: year filter + the existing Sheet action.
     *  Default remains the caller's existing date filter, so no historical/live
     *  list silently changes when upgrading the app. */
    private fun setupMoreMenu() {
        binding.btnMore.setOnClickListener { anchor ->
            val pm = android.widget.PopupMenu(this, anchor)
            var id = 1
            val baseActive = !allYearsSelected && selectedYear == null
            if (!baseFromArg.isNullOrBlank() || !baseToArg.isNullOrBlank()) {
                pm.menu.add(0, id++, 0, (if (baseActive) "✓ " else "") + "Current Date Filter")
            }
            val cal = java.util.Calendar.getInstance()
            val nowYear = cal.get(java.util.Calendar.YEAR)
            val minYear = BranchCatalog.minYearFor(branchArg)
            for (y in nowYear downTo maxOf(minYear, nowYear - 5)) {
                pm.menu.add(0, 10000 + y, 0, (if (!allYearsSelected && selectedYear == y) "✓ " else "") + "Year $y")
            }
            pm.menu.add(0, 20001, 0, (if (allYearsSelected) "✓ " else "") + "All Years")
            pm.menu.add(0, 30001, 0, "Download Sheet")
            pm.setOnMenuItemClickListener { item ->
                when {
                    item.itemId == 30001 -> downloadSheet()
                    item.itemId == 20001 -> { allYearsSelected = true; selectedYear = null; reloadFromCloud() }
                    item.itemId >= 10000 && item.itemId < 20000 -> {
                        selectedYear = item.itemId - 10000
                        allYearsSelected = false
                        reloadFromCloud()
                    }
                    else -> {
                        selectedYear = null
                        allYearsSelected = false
                        reloadFromCloud()
                    }
                }
                true
            }
            pm.show()
        }
    }

    /** 🔒 TK-APPROVED (01.08.2026, ফটো-প্রুফে "ওকে"): Master শুধু এই তালিকাতেই
     *  ব্রাঞ্চ বদলাতে পারেন — DraftActivity-র branchPicker-এর হুবহু একই বাক্স
     *  ও নিয়ম। স্টাফ/ডাক্তারের জন্য বোতামটাই দেখা যায় না, কিছুই বদলায়নি।
     *  ব্রাঞ্চ বদলালে reloadFromCloud() দিয়ে **এই bucket-টাই** নতুন করে
     *  আনা হয় — bucketKey জানা না থাকলে (পুরনো caller) বোতাম দেখানো হয় না। */
    private fun setupBranchPicker() {
        if (user.role != "master" || bucketKey.isBlank()) return
        binding.branchPicker.visibility = android.view.View.VISIBLE
        // 🟢🔒 V398: Draft পর্দা থেকে যে ব্রাঞ্চ নিয়ে আসা হয়েছে সেটাই মনে রাখা
        //   ব্রাঞ্চ; না-থাকলে সরাসরি BranchFilterStore থেকে।
        branchArg = (branchArg?.takeIf { it.isNotBlank() }) ?: BranchFilterStore.get(this)
        // 🔴🔒 V1600 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ) — আগে এখানে
        // `BranchFilterStore.pillText(this)` ব্যবহার হতো, যেটা সবসময়ই
        // **এই মুহূর্তের গ্লোবাল** ব্রাঞ্চ দেখাত — অথচ এই তালিকার `entries`
        // ফেচ হয়েছিল `branchArg`-এর জন্য। যদি এই পর্দা খোলার পরে অন্য কোনো
        // পর্দায় গ্লোবাল ব্রাঞ্চ বদলে যায় (বা তালিকার নিজের রিলোড ব্যর্থ হয়ে
        // পুরনো তালিকাই থেকে যায়), পিলে নতুন ব্রাঞ্চের নাম অথচ কার্ডে পুরনো
        // ব্রাঞ্চের রোগী দেখাত — এখন পিল সবসময় `branchArg`-এরই প্রতিচ্ছবি,
        // তালিকার সাথে কখনো আলাদা হবে না।
        binding.branchPicker.text = "🏥 " + branchArg + " ▾"
        binding.branchPicker.setOnClickListener {
            val branches = BranchFilterStore.choices()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(branches.toTypedArray(), BranchFilterStore.indexInChoices(this)) { dialog, which ->
                    val picked = branches[which]
                    branchArg = BranchFilterStore.set(this@DraftListActivity, picked)   // 🟢 V398
                    binding.branchPicker.text = "🏥 " + branchArg + " ▾"
                    // 🔴🔒 V1600 — নতুন ব্রাঞ্চের তাজা তালিকা না আসা পর্যন্ত
                    // পুরনো ব্রাঞ্চের কার্ড দেখানো যাবে না (পিল আগেই বদলে
                    // গেছে, তালিকা রিলোড ব্যর্থ হলেও যেন পুরনো ব্রাঞ্চের
                    // রোগী না দেখায়)।
                    entries.clear()
                    renderList()
                    dialog.dismiss()
                    reloadFromCloud()
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    /** 🆕🔒 V1650 (২১.০৯.২০২৬, TK-নির্দেশ) — "My Enquiry (All Branch)"-এ সব
     *  ব্রাঞ্চ একসাথে থাকায় নিজের ব্রাঞ্চের সারি খুঁজতে স্টাফকে অনেক স্ক্রল
     *  করতে হত। এই বোতাম সবার জন্য (Staff/Doctor/Master) — উপরের
     *  branchPicker-এর (Master-শুধু, নতুন করে ক্লাউড থেকে আনে) থেকে সম্পূর্ণ
     *  আলাদা ও স্বাধীন: এটা ইতিমধ্যে পর্দায় থাকা ৯৫টা (উদাহরণ) সারি থেকেই
     *  শুধু একটা ব্রাঞ্চ বেছে দেখায় — কোনো নতুন ক্লাউড-পড়া নেই, তাই খরচ বাড়ে না। */
    private fun setupMyEnquiryBranchFilter() {
        if (bucketKey != "received") return
        binding.myEnqBranchFilter.visibility = android.view.View.VISIBLE
        fun label() = "🏥 " + (myEnqBranchFilter ?: "All") + " ▾"
        binding.myEnqBranchFilter.text = label()
        binding.myEnqBranchFilter.setOnClickListener {
            val branches = BranchFilterStore.choices()   // "All" + পাঁচ ব্রাঞ্চ — Master-এর branchPicker-এরই প্রমাণিত তালিকা
            val current = branches.indexOf(myEnqBranchFilter ?: "All").let { if (it < 0) 0 else it }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(branches.toTypedArray(), current) { dialog, which ->
                    myEnqBranchFilter = if (branches[which] == "All") null else branches[which]
                    binding.myEnqBranchFilter.text = label()
                    renderList()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    /** bucketKey == "received"-এ myEnqBranchFilter থাকলে সেই ব্রাঞ্চের সারি
     *  বাদে বাকি সব লুকায় — নতুন কোনো ডেটা আনা হয় না, `entries`-ও বদলায় না। */
    private fun visibleEntries(): List<DraftEntry> {
        val f = myEnqBranchFilter
        return if (bucketKey == "received" && !f.isNullOrBlank()) entries.filter { it.branch == f } else entries
    }

    private fun wirePullToRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            android.graphics.Color.parseColor("#0EA25F"),
            android.graphics.Color.parseColor("#1167D8")
        )
        // V215 (§18.6, 31.07.2026): আগে pull-to-refresh শুধু পুরোনো memory তালিকা
        // আবার আঁকত — Delete/Restore অন্য পর্দায় হলে এখানে পুরোনো কার্ড থেকে যেত।
        // এখন সত্যিকারের নতুন data আনা হয় (repository.load) — যদি bucket/branch
        // জানা থাকে; না জানলে অন্তত tombstone-বাদ-দিয়ে redraw (deleted কার্ড আর
        // ফিরে আসে না)। ⛔ Free-plan: শুধু pull করলে বা onResume-এই fetch, ঘনঘন নয়।
        binding.swipeRefresh.setOnRefreshListener { reloadFromCloud() }
    }

    /** V215 (§18): এই bucket-এর সত্যিকারের নতুন তালিকা cloud থেকে এনে দেখায়। */
    private fun reloadFromCloud() {
        if (bucketKey.isBlank()) {
            // পুরোনো caller — bucket জানা নেই; অন্তত deleted কার্ড বাদ দিয়ে redraw।
            dropDeletedFromEntries()
            renderList()
            binding.swipeRefresh.postDelayed({
                try { binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
            }, 400L)
            return
        }
        lifecycleScope.launch {
            val fresh = try {
                withContext(Dispatchers.IO) {
                    // 🟢🔒🔒 V647 (২৫.০৮.২০২৬) — reloadFromCloud() সবসময় সত্যিকারের
                    // তাজা ডেটা চায় (Restore/Delete/Remark/Next-Follow সেভের পরে,
                    // বা Pull-to-Refresh) — তাই নতুন বসানো ২০-সেকেন্ডের ক্যাশ এখানে
                    // প্রথমেই সাফ করা হয়, যাতে নিজের হাতে-করা এডিট নিজেরই চোখে
                    // পুরনো/স্টেল দেখা না যায় (TK-এর "টাকা/রিমার্কস সবসময় সঙ্গে
                    // সঙ্গে সঠিক দেখাবে" নিয়ম অক্ষত রাখতে)। ⛔ শুধু এই ২০-সেকেন্ড-
                    // ক্যাশ সাফ হয় — বাকি কোনো ডেটা/queue/লজিক ছোঁয়া হয় না।
                    CloudReadCache.clear()
                    val range = effectiveRange()
                    val b = repository.load(branchArg, range.first, range.second)
                    when (bucketKey) {
                        "received" -> b.received
                        "enqReject" -> b.enqReject
                        "visitReject" -> b.visitReject
                        "notComplete" -> b.notComplete
                        "complete" -> b.complete
                        "unexpectedTime" -> b.unexpectedTime
                        "refunded" -> b.refunded
                        "returnVisit" -> b.returnVisit
                        // 🟢🔒 V644 (২৫.০৮.২০২৬, TK-নির্দেশ) — একই প্যাটার্ন, নতুন bucket।
                        "runningTreatment" -> b.runningTreatment
                        else -> null
                    }
                }
            } catch (_: Throwable) { null }
            if (fresh != null) {
                entries.clear()
                entries.addAll(fresh)   // repository.load ইতিমধ্যে DeletedGuard মেনে বাদ দিয়েছে
            } else {
                // নেট না পেলে অন্তত locally-known deleted কার্ড সরিয়ে দেওয়া হয়।
                dropDeletedFromEntries()
            }
            renderList()
            try { binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
        }
    }

    /** locally tombstoned (এই ফোনেই Delete হওয়া) কার্ড বর্তমান তালিকা থেকে সরায়। */
    private fun dropDeletedFromEntries() {
        try {
            val it = entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                // 🔒 খাতার সারি B224: DeletedGuard tombstone (এই ফোনে সত্যি Delete)
                // ছাড়াও, Detail Screen-এ "not found" পাওয়া ghost সারিও এখানে সরে —
                // তাই Detail থেকে সরিয়ে Back এলে Incomplete তালিকাতেও সঙ্গে সঙ্গে যায়।
                if (DeletedGuard.isDeleted("followups", e.id, this) ||
                    DraftRepository.GhostHide.isHidden(e.id, e.mobile)) it.remove()
            }
        } catch (_: Throwable) { }
    }

    // V215 (§18): এই পর্দায় ফিরলেই (যেমন Timeline থেকে Delete করে Back) সদ্য-Delete
    // হওয়া কার্ড সঙ্গে সঙ্গে সরে যায় ও count ঠিক হয় — cloud call ছাড়াই (Free-plan)।
    override fun onResume() {
        super.onResume()
        dropDeletedFromEntries()
        renderList()
        // V1652 — My Enquiry থেকে Call করে ফোনে ফিরে এলে post-call Remark popup।
        // শুধু staff + received bucket; অন্য Draft/Call flow একদম অপরিবর্তিত।
        try { binding.root.post { remindMyEnquiryPostCallRemark() } } catch (_: Throwable) { }
    }

    // 🔴🔴 TK-রিপোর্ট (07.08.2026): "নিচে স্ক্রল করে কোনো অ্যাকশন নিলে আবার একদম
    // প্রথমের দিকে চলে আসে কেন? আমি চাই যেখানে ছিলাম ঠিক সেই জায়গাতেই থাকব।"
    // **আসল কারণ (কোড ধরে যাচাই):** নিচের `renderList()` **প্রতিবার নতুন
    // `DraftCardAdapter` বানিয়ে** `recyclerDraft.adapter`-এ বসাত — নতুন adapter
    // বসানো মানেই RecyclerView-এর স্ক্রল আবার একদম শুরু থেকে। **সমাধান:**
    // Adapter একবারই তৈরি হয় (নিচের `draftAdapter`), পরে শুধু `updateItems()` —
    // তাই তালিকা হালনাগাদ হলেও স্ক্রলের জায়গা অক্ষত থাকে।
    // ⛔ কোনো ডেটা/অনুমতি/মোছার লজিক বদলায়নি — শুধু adapter পুনর্ব্যবহার।
    private var draftAdapter: DraftCardAdapter? = null

    // 🟢 B631 (11.08.2026): বাল্ক Delete/Restore চলাকালীন ছোট ভাসমান "মুছে ফেলা হচ্ছে x/N" ইঙ্গিত।
    private var bulkProgressView: android.widget.TextView? = null

    // 🟢🔒🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ, বহু-দফা প্রশ্ন করে নিশ্চিত হওয়া —
    // "একই কার্ড সব জায়গায়, ঝুঁকি ছাড়া") — এই পাঁচটা "জীবন্ত" তালিকা এখন
    // Follow-up-এর কার্ডের মতোই দেখায় ও কাজ করে (আলাদা, স্বাধীন
    // `FollowUpAdapter` পুনর্ব্যবহার — FollowUpActivity-এর নিজস্ব কোড
    // (`buildFollowCard`) এক অক্ষরও ছোঁয়া হয়নি, ঝুঁকি নেওয়া হয়নি)। বাকি
    // ৪টা ("মৃত": enqreject/visitreject/refunded/returnvisit) সম্পূর্ণ
    // অপরিবর্তিত — DraftCardAdapter-ই থাকে।
    private val ALIVE_BUCKETS = setOf("received", "unexpectedTime", "notComplete", "complete", "runningTreatment")
    private var followListAdapter: FollowUpAdapter? = null

    private fun renderList() {
        // 🆕 V1650 — "My Enquiry (All Branch)"-এ ব্রাঞ্চ-ফিল্টার সক্রিয় থাকলে
        // খালি/সংখ্যা দুটোই ফিল্টার-করা তালিকা ধরেই ঠিক হওয়া উচিত।
        val visible = visibleEntries()
        updateHeaderTitle(visible.size)
        if (visible.isEmpty()) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            binding.recyclerDraft.visibility = android.view.View.GONE
            binding.pickBar.visibility = android.view.View.GONE
            return
        }
        binding.tvEmpty.visibility = android.view.View.GONE
        binding.recyclerDraft.visibility = android.view.View.VISIBLE

        // 🟢🔒🔒 V646 — "জীবন্ত" পাঁচটা তালিকায় বাল্ক-পিক/Restore/Delete
        // কিছুই নেই (এখানে সেই বোতামই দেখানো হয় না) — তাই পিক-বার সবসময়
        // লুকানো থাকে, DraftCardAdapter-এর পিক-লজিক এই তালিকায় ছোঁয়া হয় না।
        if (bucketKey in ALIVE_BUCKETS) {
            binding.pickBar.visibility = android.view.View.GONE
            val items = visible.map { it.toFollowUpItem() }
            val existingFollow = followListAdapter
            if (existingFollow == null) {
                binding.recyclerDraft.layoutManager = LinearLayoutManager(this)
                val a = FollowUpAdapter(
                    context = this,
                    items = items,
                    onCall = { item ->
                        if (bucketKey == "received") callFromMyEnquiry(item) else dial(item.mobile)
                    },
                    onWhatsApp = { item -> whatsApp(item.mobile) },
                    onRemark = { item -> showDraftRemarkDialog(item) },
                    onNextFollow = { item -> showDraftNextFollowDialog(item) },
                    onPayment = { item -> openDraftPaymentFor(item) },
                    onPrescription = { item -> openDraftPrescriptionFor(item) },
                    onView = { item -> viewTimeline(item.mobile) }
                    // ⛔ onStatusMenu/onCallSignal/onEdit/onPhotoEdit ইচ্ছাকৃতভাবে
                    // ডিফল্ট (কিছুই করে না) — এই ট্রিপল-ট্যাপ এডিট-সুবিধাগুলো
                    // Follow-up-এর নিজের পর্দাতেই থাকে, TK-কে এই সীমাবদ্ধতা
                    // স্পষ্টভাবে জানানো হয়েছে (ঝুঁকি না নেওয়ার জন্য)।
                )
                followListAdapter = a
                binding.recyclerDraft.adapter = a
            } else {
                existingFollow.updateItems(items)
            }
            return
        }

        val existing = draftAdapter
        if (existing == null) {
            binding.recyclerDraft.layoutManager = LinearLayoutManager(this)
            val a = DraftCardAdapter(
                entries,
                currentUser = user,
                onCall = { e -> dial(e.mobile) },
                onWhatsApp = { e -> whatsApp(e.mobile) },
                onView = { e -> viewTimeline(e.mobile) },
                onRestore = { e -> restore(e) },
                onDelete = { e -> confirmDelete(e) },
                onPickChanged = { refreshPickBar() }
            )
            draftAdapter = a
            binding.recyclerDraft.adapter = a
            binding.btnPickCancel.setOnClickListener { a.clearPicks(); refreshPickBar() }
            binding.btnPickRestore.setOnClickListener { confirmBulk(restoreMode = true) }
            binding.btnPickDelete.setOnClickListener { confirmBulk(restoreMode = false) }
        } else {
            existing.updateItems(entries)
        }
        refreshPickBar()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 🟢🔒🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ) — "জীবন্ত" কার্ডের বোতাম-ফাংশন।
    // প্রতিটাই বিদ্যমান, প্রমাণিত পথে যায় (PaymentActivity/DoctorCheckupActivity/
    // FollowUpRepository-এর নিজস্ব public ফাংশন) — কোনো নতুন ডায়ালগ-লজিক
    // পুনর্লিখন করা হয়নি, তাই ঝুঁকি নেই। FollowUpActivity.kt এক অক্ষরও
    // ছোঁয়া হয়নি।
    // ─────────────────────────────────────────────────────────────────────

    /** FollowUpActivity.openPaymentFor()-এর হুবহু একই, প্রমাণিত ধরন। */
    private fun openDraftPaymentFor(item: FollowUpItem) {
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra("mobile", digits)
        intent.putExtra("patientRowId", item.refId)
        intent.putExtra("patientCode", item.patientId)
        startActivity(intent)
    }

    /** PatientTimelineActivity.prepareClinicalRoleSession()-এর হুবহু একই
     *  RoleSession-পথ — DoctorCheckupActivity তাই ঠিক রোগীর সাথেই খোলে। */
    private fun openDraftPrescriptionFor(item: FollowUpItem) {
        val roleStr = if (user.role.equals("doctor", true)) "DOCTOR" else "STAFF"
        com.tkbiswas.pilesclinic.clinical.RoleSession.applyFrom(
            roleStr, item.name, item.refId.ifBlank { item.id }, item.branch,
            item.mobile, item.address, item.age, item.sex, item.disease,
            patientDisplayId = item.patientId
        )
        startActivity(Intent(this, com.tkbiswas.pilesclinic.clinical.DoctorCheckupActivity::class.java))
    }

    /** FollowUpRepository.updateRemark()-এর প্রমাণিত ফাংশনে সরাসরি লেখে —
     *  নতুন কোনো রিমার্কস-সেভ-লজিক বানানো হয়নি। */
    private fun showDraftRemarkDialog(item: FollowUpItem) {
        val input = android.widget.EditText(this).apply {
            setText(item.lastRemark.takeIf { it != "No remark" } ?: "")
            hint = "Remark"
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, item.name.ifBlank { "Remark" }))
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isBlank()) return@setPositiveButton
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        FollowUpRepository(this@DraftListActivity).updateRemark(item.id, text, user.name.ifBlank { user.mobile })
                    }
                    if (ok) { Toast.makeText(this@DraftListActivity, "Saved", Toast.LENGTH_SHORT).show(); reloadFromCloud() }
                    else Toast.makeText(this@DraftListActivity, "Could not save — check connection", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** FollowUpRepository.updateNextFollow()-এর প্রমাণিত ফাংশনে সরাসরি লেখে। */
    private fun showDraftNextFollowDialog(item: FollowUpItem) {
        val cal = java.util.Calendar.getInstance()
        try {
            if (item.nextFollow.isNotBlank()) {
                val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(item.nextFollow.take(10))
                if (d != null) cal.time = d
            }
        } catch (_: Throwable) { }
        android.app.DatePickerDialog(this, { _, y, m, d ->
            val picked = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.GregorianCalendar(y, m, d).time)
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    FollowUpRepository(this@DraftListActivity).updateNextFollow(item.id, picked)
                }
                if (ok) { Toast.makeText(this@DraftListActivity, "Saved", Toast.LENGTH_SHORT).show(); reloadFromCloud() }
                else Toast.makeText(this@DraftListActivity, "Could not save — check connection", Toast.LENGTH_LONG).show()
            }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    // ─────────────────────────────────────────────────────────────────────
    // 🆕🔒 TK-নির্দেশ (07.08.2026, ফটো-প্রুফ অনুমোদিত) — "অনেকগুলো একসাথে মার্ক
    // করেও ডিলিট করতে পারি, অথবা অনেকগুলো একসাথে মার্ক করেও Restore করতে পারি"।
    // ⛔ **ঝুঁকি-নিয়ন্ত্রণ:** (১) প্রতিটা কাজ **একই প্রমাণিত পথেই** — Delete-এ
    //    `repository.deleteEnquiry(...)` ও Restore-এ `repository.restore(...)`,
    //    অর্থাৎ এক-এক করে চাপলে যা হতো হুবহু তাই; নতুন কোনো মোছার লজিক নেই।
    //    (২) অনুমতির নিয়ম অক্ষত — যেসব তালিকায় Master-এর অনুমতি লাগে সেখানে
    //    টিক-বক্সই দেখা যায় না (DraftCardAdapter.pickableTabs)। (৩) মোছা সবই
    //    Trash Bin-এ যায়, Master ফেরাতে পারেন। (৪) চাপার আগে "কতগুলো" দেখিয়ে
    //    নিশ্চিত করা হয়, বোতাম একবারেই বন্ধ হয় (দুইবার চাপে দুইবার চলে না)।
    // ─────────────────────────────────────────────────────────────────────
    private fun refreshPickBar() {
        val n = draftAdapter?.pickedIds?.size ?: 0
        binding.pickBar.visibility = if (n > 0) android.view.View.VISIBLE else android.view.View.GONE
        binding.tvPickCount.text = "$n selected"
        // Restore যেসব তালিকায় প্রযোজ্য নয় (My Enquiry / Unexpected Time) সেখানে
        // বোতামটা লুকানো — নইলে চাপলে কিছুই হতো না, বিভ্রান্তি হতো।
        val restorable = bucketKey == "enqReject" || bucketKey == "visitReject" || bucketKey == "returnVisit"
        binding.btnPickRestore.visibility = if (restorable) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun confirmBulk(restoreMode: Boolean) {
        val a = draftAdapter ?: return
        val picked = a.pickedEntries()
        if (picked.isEmpty()) return
        val what = if (restoreMode) NoBengali.s("Restore") else NoBengali.s("Delete")
        val msg = if (restoreMode)
            "${picked.size} selected record(s) will be restored to the previous section."
        else
            "${picked.size} selected record(s) will be moved to Trash Bin. Master can restore them later."
        val bulkTitle = if (restoreMode) "Restore Selected Records?" else "Delete Selected Records?"
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, bulkTitle))
            .setMessage(msg + "\n\n" + picked.joinToString("\n") { "• " + it.name.ifBlank { it.mobile } })
            .setPositiveButton(what, null)
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
        dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // 🟢 B631 (11.08.2026, TK-রিপোর্ট "একবার চাপলে হচ্ছে না, ২০ বার চাপতে হয়"):
            // আসল সমস্যা চাপ নয় — ধীর নেটে অনেকগুলো নাম একটার পর একটা Trash-cascade
            // হতে ১৫-২৫ সেকেন্ড লাগত, আর তখন Delete বোতাম শুধু নিষ্ক্রিয় থাকত (কোনো
            // "মুছছে…" ইঙ্গিত নেই) — তাই জমে থাকা মনে হত, বারবার চাপা হত।
            // এখন: সঙ্গে সঙ্গে ডায়ালগ বন্ধ + বাছাই করা কার্ড তালিকা থেকে সরিয়ে দিই
            // (instant), পেছনে cascade চলে, ছোট "মুছে ফেলা হচ্ছে x/N" ভাসমান ইঙ্গিত;
            // কোনোটা পেছনে ব্যর্থ হলে সেই কার্ড আবার ফিরিয়ে আনি — কিছুই হারায় না।
            // ⛔ মোছার/ফেরানোর আসল ডাক (deleteEnquiry / restore) এক অক্ষরও বদলায়নি;
            //    ক্লাউড-কল সংখ্যা হুবহু আগের মতোই (Free-plan egress বাড়ে না)।
            try { dlg.dismiss() } catch (_: Throwable) { }
            val items = picked.toList()
            entries.removeAll(items)   // instant: কার্ড সঙ্গে সঙ্গে সরে
            a.clearPicks()
            renderList()
            showBulkProgress(items.size, restoreMode)
            lifecycleScope.launch {
                var done = 0
                var failed = 0
                val failedItems = mutableListOf<DraftEntry>()
                for (e in items) {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            // ⛔ একক Restore/Delete-এর হুবহু একই ডাক (আচরণ/হিসাব অপরিবর্তিত)।
                            if (restoreMode) repository.restore(e, user)
                            else repository.deleteEnquiry(e, user, isFreeDelete(e)) == "OK"   // 🔴🔒 V717
                        } catch (_: Throwable) { false }
                    }
                    if (ok) done++ else { failed++; failedItems.add(e) }
                    updateBulkProgress(done + failed, items.size, restoreMode)
                }
                // ব্যর্থগুলো তালিকায় নিরাপদে ফিরিয়ে আনি (কিছু হারায় না, TK আবার চেষ্টা করতে পারেন)
                if (failedItems.isNotEmpty()) {
                    for (fe in failedItems) if (entries.none { it.id == fe.id }) entries.add(fe)
                    renderList()
                }
                removeBulkProgress()
                val completedAction = if (restoreMode) "restored" else "deleted"
                Toast.makeText(
                    this@DraftListActivity,
                    "$done record(s) $completedAction." + (if (failed > 0) " · $failed record(s) failed — please try again." else ""),
                    Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            }
        }
    }

    // 🟢 B631: বাল্ক Delete/Restore চলাকালীন ছোট ভাসমান ইঙ্গিত — কোনো নতুন XML/লেআউট নয়,
    // content-রুটে একটা TextView বসিয়ে-সরিয়ে দেখানো; অন্য পর্দা/ডিজাইন কিছুই ছোঁয় না।
    private fun bulkProgressLabel(count: Int, total: Int, restoreMode: Boolean): String =
        NoBengali.s((if (restoreMode) "ফেরানো হচ্ছে" else "মুছে ফেলা হচ্ছে") + " $count/$total")
    private fun showBulkProgress(total: Int, restoreMode: Boolean) {
        try {
            removeBulkProgress()
            val root = findViewById<android.view.ViewGroup>(android.R.id.content) ?: return
            val d = resources.displayMetrics.density
            val tv = android.widget.TextView(this).apply {
                text = bulkProgressLabel(0, total, restoreMode)
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                val padH = (20 * d).toInt(); val padV = (13 * d).toInt()
                setPadding(padH, padV, padH, padV)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 16 * d
                    setColor(android.graphics.Color.parseColor("#EE12233F"))
                }
            }
            val lp = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.BOTTOM
                bottomMargin = (100 * d).toInt()
            }
            root.addView(tv, lp)
            bulkProgressView = tv
        } catch (_: Throwable) { }
    }
    private fun updateBulkProgress(count: Int, total: Int, restoreMode: Boolean) {
        try { bulkProgressView?.text = bulkProgressLabel(count, total, restoreMode) } catch (_: Throwable) { }
    }
    private fun removeBulkProgress() {
        try {
            bulkProgressView?.let { v -> (v.parent as? android.view.ViewGroup)?.removeView(v) }
            bulkProgressView = null
        } catch (_: Throwable) { }
    }

    // TK-REQUESTED ADDITION (2026-07-18), Phase 1: same-day self-delete for
    // My Enquiry entries. Always confirms first (destructive-looking action,
    // even though it's recoverable via Trash Bin) and never silently fails.
    // 🔴 B302.1 (02.08.2026): "Refunded" ঘর থেকে Delete চাপলে এটা রোগীর
    // রেকর্ড মোছে (Enquiry নয়) — তাই এখানে আলাদা পথে পাঠানো হয়, নিচের
    // Enquiry-নির্দিষ্ট অংশ একটুও বদলায়নি।
    /** 🆕🔴 TK-নির্দেশ (07.08.2026) — যেসব তালিকা থেকে **মাস্টারের অনুমতি ছাড়াই**
     *  ডিলিট করা যাবে (সবই এনকোয়ারির তালিকা, কোনো টাকা জড়িত নয়)। পেশেন্ট
     *  সেকশনের তালিকা (notcomplete / complete / refunded) ইচ্ছাকৃতভাবে বাদ —
     *  সেখানে আগের মতোই Master-এর অনুমতি লাগবে। */
    private fun isFreeDeleteTab(tab: String): Boolean =
        tab == "received" || tab == "enqreject" || tab == "visitreject" || tab == "unexpected"

    /* 🔴🔴🔒 V717 (নিজে গভীরে যাচাই করে ধরা — TK-নির্দেশ *"কোন ভাল কাজ যেন
       খারাপ না হয়"*):

       উপরের ছাড়ের **ভিত্তি** ছিল TK-এর কথা (০৩ ও ০৭.০৮.২০২৬) — *"Enquiry
       reject, Visit card reject — পেমেন্ট সংক্রান্ত কোনো ব্যাপার না থাকলে
       স্টাফ নিজেই করতে পারবে"* — আর কোডে লেখা যুক্তি: **"এই দুই তালিকায়
       কোনো টাকা জমা নেই বলে নিশ্চিত, কারণ টাকা শুধু Patient/Treatment ধাপেই
       থাকে।"**

       V716-এ "Visit Reject" তালিকায় **Treatment-ধাপের** (বাতিল করা) কার্ডও
       দেখানো শুরু হলো — অর্থাৎ ওই "টাকা নেই" ভিত্তিটাই আর সত্যি নয়। তাহলে
       স্টাফ **মাস্টারের অনুমতি ছাড়াই** একজন Treatment-রোগীর কার্ড মুছে
       ফেলতে পারতেন। সেটা TK-এর লক করা নিয়মের বিরুদ্ধে।

       ⇒ তাই ছাড়টা এখন **শুধু সেই কার্ডেই**, যার ধাপ Treatment নয়।
       Treatment-ধাপের কার্ডে আগের মতোই **মাস্টারের অনুমতি লাগবে**।
       ⛔ পুরোনো (Patient/Inquiry ধাপের) কার্ডে আচরণ এক অক্ষরও বদলায়নি। */
    private fun isFreeDelete(e: DraftEntry): Boolean =
        isFreeDeleteTab(e.tab) && !e.stage.equals("Treatment", ignoreCase = true)

    private fun confirmDelete(e: DraftEntry) {
        // 🆕🔴 TK-নির্দেশ (২১.০৯.২০২৬) — Incomplete Patient / Complete Patient /
        // Return Visit List-ও পেশেন্ট-সারি (enquiries নয়) — তাই এরাও "refunded"-এর
        // হুবহু একই পথে (deletePatientRecord → DeletePermission.canDeleteEntryNow):
        // Master সবসময়; নইলে শুধু আজকের নিজের তৈরি করা রেকর্ড (চেম্বার খোলা, নিজের
        // OUT TIME সেভ না হওয়া পর্যন্ত); নইলে Master-এর অনুমতির অনুরোধ যায়।
        if (e.tab == "refunded" || e.tab == "notcomplete" || e.tab == "complete" || e.tab == "returnvisit") {
            confirmDeleteRefundedPatient(e); return
        }
        // 🆕 TK-নির্দেশ (03.08.2026) — "Enquiry reject, Visit card reject —
        // পেমেন্ট সংক্রান্ত কোনো ব্যাপার না থাকলে স্টাফ নিজেই করতে পারবে,
        // Master-এর অনুমতি লাগবে না।" এই দুই তালিকায় (enqreject/visitreject)
        // কোনো টাকা জমা নেই বলে নিশ্চিত (Patient/Treatment ধাপেই শুধু টাকা
        // থাকে — notcomplete/complete তালিকা, ওখানে এই ছাড় নেই, আগের নিয়মই)।
        // তাই এই দুই তালিকায় সরাসরি Delete ডায়ালগ, Master-চেক ছাড়াই।
        // 🆕🔴 TK-নির্দেশ (07.08.2026, স্ক্রিনশটসহ — "ইনকয়ারি ডিলিট করার জন্য
        // মাস্টারের অনুমতি লাগবে না; ভিজিট সেকশন থেকে রিজেক্ট করতেও লাগবে না;
        // শুধুমাত্র পেশেন্ট সেকশন থেকে ডিলিট করতে গেলে অনুমতি লাগবে")।
        // TK নিজে বেছে দিয়েছেন কোন কোন তালিকায় ছাড় থাকবে:
        //   ছাড় (অনুমতি লাগবে না) — এগুলো সবই এনকোয়ারির তালিকা, টাকা জড়িত নয়:
        //     · received   = "My Enquiry (All Branch)"      🆕 (আগে অনুমতি লাগত)
        //     · enqreject  = "Enquiry Reject List"           (আগেও ছাড় ছিল)
        //     · visitreject= "Visit Reject List"             (আগেও ছাড় ছিল)
        //     · unexpected = "Unexpected Time Calls"        🆕 (আগে অনুমতি লাগত)
        //   অনুমতি লাগবে (আগের মতোই) — পেশেন্ট সেকশন:
        //     · notcomplete = "Incomplete Patient" · complete = "Complete Patient"
        //     · refunded    = উপরের আলাদা পথে (confirmDeleteRefundedPatient)
        // ⛔ মোছার আসল পথ (Trash Bin + followup cascade) এক অক্ষরও বদলায়নি —
        //    ডিলিট আগের মতোই ফেরানো যায় (Master → Trash Bin → Restore)।
        // (আগের কোড ছিল: if (e.tab == "enqreject" || e.tab == "visitreject"))
        if (isFreeDelete(e)) {   // 🔴🔒 V717 — Treatment-ধাপে ছাড় নেই
            showDeleteEnquiryDialogDirect(e)
            return
        }
        // 🔒 খাতার সারি B98 (TK, 29.07.2026 রাত ১০.১০): *"মাস্টার ছাড়া কেউ ডিলিট
        // করবে না। staff-রা করতে চাইলে Master admin-এর কাছে অনুমতি নিতে হবে —
        // Master-এর ঘন্টাতে যাবে।"*
        // ⛔ Timeline পর্দার সঙ্গে **হুবহু একই নিয়ম** — দুই জায়গায় আলাদা আচরণ
        //    যেন কখনো না হয়।
        if (!DeletePermission.canDeleteNow(user)) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Master's approval needed"))
                .setMessage(
                    "${e.name.ifBlank { e.mobile }}\n\n" +
                    "ডিলিট করতে পারেন শুধু Master Admin।\n" +
                    "অনুরোধ পাঠালে সেটা Master-এর ঘন্টায় যাবে — তিনি অনুমতি দিলে তবেই মুছবে।\n\n" +
                    "⛔ এখনই কিছুই মুছবে না।"
                )
                .setPositiveButton("Send Request") { _, _ ->
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            DeletePermission.sendRequest(
                                this@DraftListActivity, user, "Enquiry",
                                e.name, e.mobile, e.patientId, e.branch,
                                disease = e.disease
                            )
                        }
                        Toast.makeText(
                            this@DraftListActivity, NoBengali.s(DeletePermission.lastMessage()),   /* 🔁 V1176 — বার্তাটা এখন `DeletePermission` নিজেই ঠিক করে দেয় (পাঠানো হলো · আগেই পাঠানো · ব্যর্থ)। */
                            Toast.LENGTH_LONG
                        ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    }
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
            return
        }
        showDeleteEnquiryDialogDirect(e)
    }

    /** আসল Delete-ডায়ালগ — Master-চেক আগেই পাশ হয়ে গেছে (বা লাগেই না)
     *  এমন অবস্থায় ডাকা হয়, দুই জায়গা থেকে (উপরের দুটো পথ থেকেই) পুনর্ব্যবহার। */
    private fun showDeleteEnquiryDialogDirect(e: DraftEntry) {
        // 🔴 B334 (03.08.2026, একই ক্লাসের বাগ — PaymentActivity.kt-এ TK
        // যেটা ধরেছিলেন): "Delete" বোতাম চাপার পরেও সক্রিয় থাকত, একাধিকবার
        // চাপলে একাধিকবার ডিলিট-কল যেতে পারত। প্রথম চাপেই বোতাম বন্ধ।
        val delEnqDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Delete Enquiry?"))
            .setMessage("${e.name.ifBlank { e.mobile }} — this will move to Trash Bin (Master can restore it from there).")
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
        delEnqDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            delEnqDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
            delEnqDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                lifecycleScope.launch {
                    // 🆕 (07.08.2026) — উপরের একই তালিকা-নিয়ম এখানেও (ভেতরের
                    // দ্বিতীয় গেট `TrashHelper.canDelete`-ও পাশ কাটে), নইলে
                    // পর্দায় ছাড় দিলেও "PERMISSION" ফেরত আসত।
                    // (আগে: e.tab == "enqreject" || e.tab == "visitreject")
                    val bypass = isFreeDelete(e)   // 🔴🔒 V717
                    val result = withContext(Dispatchers.IO) { repository.deleteEnquiry(e, user, bypass) }
                    if (result == "OK") {
                        entries.remove(e)
                        Toast.makeText(this@DraftListActivity, "Deleted — moved to Trash Bin", Toast.LENGTH_SHORT).show()
                        renderList()
                    } else if (result == "NOT_FOUND") {
                        // 🔒🔒 খাতার সারি B224 (TK verified live-test, 01.08.2026): এই সারি
                        // database-এ সত্যিই আর নেই। আগে শুধু "Record not found" দেখিয়ে
                        // সারিটা তালিকায় রেখে দিত (Action-ও খোলা যেত)। এখন এই ফোনের
                        // চোখে-দেখা তালিকা/জমানো cache থেকে সঙ্গে সঙ্গে সরিয়ে count ঠিক
                        // করা হয়। ⛔ কোনো cloud/tombstone নয় (purgeGhostFromCache-এর
                        // নোট দ্রষ্টব্য) — অন্য ফোনের এখনো sync না-হওয়া আসল রেকর্ড
                        // "not found" পাওয়ার কারণে স্থায়ীভাবে মুছবে না।
                        entries.remove(e)
                        withContext(Dispatchers.IO) { repository.purgeGhostFromCache(e.id, e.mobile) }
                        Toast.makeText(this@DraftListActivity, "Already deleted — removed from the list", Toast.LENGTH_SHORT).show()
                        renderList()
                    } else {
                        val msg = when (result) {
                            "PERMISSION" -> "You don't have permission to delete this"
                            else -> "Could not delete — network is too slow/unstable, please retry"
                        }
                        Toast.makeText(this@DraftListActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    delEnqDialog.dismiss()
                }
        }
    }

    /** 🔴 B302.1 (02.08.2026): "Refunded" ঘরের Delete — উপরের Enquiry-পথের
     *  সমান্তরাল, শুধু রোগীর রেকর্ডের জন্য। `deletePatientRecord()` অনুমতি
     *  না থাকলে "PERMISSION" ফেরায়, তখন Master-এর ঘন্টায় অনুরোধ পাঠানোর
     *  একই বার্তা দেখানো হয় (উপরের Enquiry পপ-আপের হুবহু একই ভাষা)। */
    private fun confirmDeleteRefundedPatient(e: DraftEntry) {
        // 🔴 B334 (03.08.2026, একই ক্লাসের বাগ) — প্রথম চাপেই বোতাম বন্ধ,
        // একাধিক ডিলিট-কল আটকাতে।
        val delPatDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Delete Patient?"))
            .setMessage("${e.name.ifBlank { e.mobile }} — this will move to Trash Bin (Master can restore it from there). Their Follow-up/Payment history stays intact and returns with them if restored.")
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
        delPatDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            delPatDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
            delPatDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { repository.deletePatientRecord(e, user) }
                    when (result) {
                        "OK" -> {
                            entries.remove(e)
                            Toast.makeText(this@DraftListActivity, "Deleted — moved to Trash Bin", Toast.LENGTH_SHORT).show()
                            renderList()
                        }
                        "NOT_FOUND" -> {
                            entries.remove(e)
                            withContext(Dispatchers.IO) { repository.purgeGhostFromCache(e.id, e.mobile) }
                            Toast.makeText(this@DraftListActivity, "Already deleted — removed from the list", Toast.LENGTH_SHORT).show()
                            renderList()
                        }
                        "PERMISSION" -> {
                            androidx.appcompat.app.AlertDialog.Builder(this@DraftListActivity)
                                .setCustomTitle(PremiumAlert.header(this@DraftListActivity, "Master's approval needed"))
                                .setMessage(
                                    "${e.name.ifBlank { e.mobile }}\n\n" +
                                    "ডিলিট করতে পারেন শুধু Master Admin।\n" +
                                    "অনুরোধ পাঠালে সেটা Master-এর ঘন্টায় যাবে — তিনি অনুমতি দিলে তবেই মুছবে।\n\n" +
                                    "⛔ এখনই কিছুই মুছবে না।"
                                )
                                .setPositiveButton("Send Request") { _, _ ->
                                    lifecycleScope.launch {
                                        val ok = withContext(Dispatchers.IO) {
                                            DeletePermission.sendRequest(
                                                this@DraftListActivity, user, "Patient",
                                                e.name, e.mobile, e.patientId, e.branch,
                                                disease = e.disease
                                            )
                                        }
                                        Toast.makeText(
                                            this@DraftListActivity, NoBengali.s(DeletePermission.lastMessage()),   /* 🔁 V1176 — বার্তাটা এখন `DeletePermission` নিজেই ঠিক করে দেয় (পাঠানো হলো · আগেই পাঠানো · ব্যর্থ)। */
                                            Toast.LENGTH_LONG
                                        ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show().also { PremiumAlert.paint(it) }
                        }
                        else -> Toast.makeText(this@DraftListActivity, "Could not delete — network is too slow/unstable, please retry", Toast.LENGTH_SHORT).show()
                    }
                    delPatDialog.dismiss()
                }
        }
    }

    /**
     * V1652 (21.09.2026) — My Enquiry-এর Call button আগে শুধু Dialer খুলত।
     * Follow-up screen-এর মতো PendingRemarkStore-এ কিছু লিখত না, তাই Call শেষ
     * করে এই screen-এ ফিরলেও Remark popup দেখানোর মতো কোনো trigger-ই থাকত না।
     *
     * এখানে শুধু সেই missing bridge যোগ হয়েছে। Branch কোনো permission gate নয়:
     * JPE-CRP নিজের assigned Falakata enquiry-তেও একইভাবে popup পাবে।
     */
    private fun callFromMyEnquiry(item: FollowUpItem) {
        if (user.role.equals("staff", ignoreCase = true)) {
            try { PendingRemarkStore.add(this, item, user.mobile, source = "my_enquiry") } catch (_: Throwable) { }
        }
        dial(item.mobile)
    }

    /** Call/Dialer থেকে My Enquiry-তে ফিরে এলে শুধু ওই source-এর pending row দেখায়। */
    private fun remindMyEnquiryPostCallRemark() {
        if (bucketKey != "received" || !user.role.equals("staff", ignoreCase = true)) return
        if (isFinishing || isDestroyed) return
        val now = System.currentTimeMillis()
        val visibleMobiles = try {
            visibleEntries().map { it.mobile.filter { c -> c.isDigit() }.takeLast(10) }.toHashSet()
        } catch (_: Throwable) { hashSetOf<String>() }
        val pending = try {
            PendingRemarkStore.list(this, user.mobile).firstOrNull {
                it.source == "my_enquiry" &&
                    it.snoozeUntil <= now && it.shownCount < 3 &&
                    visibleMobiles.contains(it.mobile.filter { c -> c.isDigit() }.takeLast(10))
            }
        } catch (_: Throwable) { null } ?: return

        val item = visibleEntries().firstOrNull {
            it.mobile.filter { c -> c.isDigit() }.takeLast(10) == pending.mobile.filter(Char::isDigit).takeLast(10)
        }?.toFollowUpItem() ?: FollowUpItem(
            id = pending.followUpId, name = pending.name, mobile = pending.mobile,
            branch = pending.branch, disease = "", stage = pending.stage.ifBlank { "Inquiry" },
            lastRemark = "", nextFollow = "", recordDate = "", bill = 0.0, paid = 0.0,
            callCount = pending.callCount
        )

        try { PendingRemarkStore.markShown(this, pending.mobile) } catch (_: Throwable) { }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Update remark?"))
            .setMessage("Add a remark for ${item.name.ifBlank { item.mobile }} after that call?")
            .setPositiveButton("Add Remark") { _, _ -> openMyEnquiryCallRemark(item, pending.at) }
            .setNegativeButton("Not now") { _, _ ->
                try { PendingRemarkStore.snooze(this, pending.mobile) } catch (_: Throwable) { }
            }
            .setOnCancelListener {
                try { PendingRemarkStore.snooze(this, pending.mobile) } catch (_: Throwable) { }
            }
            .show().also { PremiumAlert.paint(it) }
    }

    /**
     * Existing CallRemarkActivity-ই reuse করা হয় — আলাদা save logic বানানো হয়নি।
     * আসল followups id আগে নিশ্চিত করা হয়, যাতে enquiry-id fallback-এ silent 200 OK
     * হয়ে Remark হারিয়ে না যায়।
     */
    private fun openMyEnquiryCallRemark(item: FollowUpItem, calledAtMs: Long) {
        lifecycleScope.launch {
            val realFollowupId = withContext(Dispatchers.IO) {
                try { FollowUpRepository(this@DraftListActivity).ensureFollowUpRowId(item) }
                catch (_: Throwable) { item.id }
            }
            val calledAtIso = try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date(if (calledAtMs > 0L) calledAtMs else System.currentTimeMillis()))
            } catch (_: Throwable) { "" }
            if (isFinishing || isDestroyed) return@launch
            startActivity(
                Intent(this@DraftListActivity, CallRemarkActivity::class.java)
                    .putExtra("mobile", item.mobile)
                    .putExtra("direction", "outgoing")
                    .putExtra("patientId", item.patientId)
                    .putExtra("patientName", item.name)
                    .putExtra("branch", item.branch)
                    .putExtra("calledAt", calledAtIso)
                    .putExtra("existingRemark", item.lastRemark)
                    .putExtra("followupId", realFollowupId)
                    .putExtra("clearPendingOnSave", true)
            )
        }
    }

    private fun dial(mobile: String) {
        // TK-REQUESTED (2026-07-24): "everywhere calling is possible in the
        // project" -- shared CallChooser.kt (Phone/Superfone/etc. picker,
        // Truecaller excluded).
        try { CallChooser.open(this, mobile) }
        catch (e: ActivityNotFoundException) { Toast.makeText(this, "No phone app", Toast.LENGTH_SHORT).show() }
    }

    private fun whatsApp(mobile: String) {
        // 🔒 V235 (TK, WhatsApp Chooser project-wide): কেন্দ্রীয় chooser (Personal/Business)।
        WhatsAppMessageChooser.send(this, mobile)
    }

    private fun viewTimeline(mobile: String) {
        val d = mobile.filter { it.isDigit() }.takeLast(10)
        // 🚨 TK-REPORTED (29.07.2026 সন্ধ্যা ৬.১৮, ছবিসহ · খাতার সারি B124):
        // *"মেইন কার্ড থেকে আমাকে ডিলিট অপশন কেন দেখাবে? সেখানে তো আগে
        //   রিজেক্ট আসতে হতো।"*
        // ⛔ আগে Timeline ঠিক করত **রেকর্ডের status দেখে** — status যদি কোনো
        //    কারণে Cancelled/Incomplete থাকে অথচ কার্ডটা চালু তালিকায় থাকে,
        //    তখন চালু কার্ডেও Delete উঠে যেত। এখন ঠিক হয় **কোথা থেকে খোলা
        //    হলো** তা দেখে: ডিলিট শুধু Draft-এর বাতিল তালিকা থেকে খুললে।
        // ⛔ "My Enquiry (All Branch)" চালু তালিকা — সেখান থেকে ডিলিট নয়।
        val t = intent.getStringExtra("title") ?: ""
        val closedBucket = t.contains("Reject", true) ||
            t.contains("Complete", true) ||     // Incomplete Patient · Complete Patient
            t.contains("Unexpected", true)
        startActivity(
            Intent(this, PatientTimelineActivity::class.java)
                .putExtra("mobile", d)
                .putExtra("fromDraftClosed", closedBucket)
        )
    }

    private fun restore(e: DraftEntry) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { repository.restore(e, user) }
            if (ok) {
                entries.remove(e)
                Toast.makeText(this@DraftListActivity, "Restored — moved back to its section", Toast.LENGTH_SHORT).show()
                renderList()
            } else {
                Toast.makeText(this@DraftListActivity, "Restore failed — check connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 🔒 TK-APPROVED (30.07.2026 সন্ধ্যা, ফটো-প্রুফে "ঠিক আছে" · খাতার সারি
     * B192): TK: *"শুধুমাত্র ইনকমপ্লিট পেশেন্ট এর ক্ষেত্রেই নয়, সমস্ত
     * সেকশনের ক্ষেত্রেই Google Sheets এ ডাউনলোড করার ব্যবস্থা রাখবেন।"*
     *
     * এই স্ক্রিনে **তখন যা দেখা যাচ্ছে ঠিক তা-ই** (Restore/Delete হয়ে যাওয়া
     * সারি বাদ দিয়ে, একই ক্রমে) CSV হয়ে নামে। তারপর ফোনের চেনা Share/Save
     * পর্দা খোলে — সেখান থেকে Drive/Downloads-এ রাখা যায় বা Gmail/WhatsApp-এ
     * পাঠানো যায়। Google Sheets/Excel দুটোতেই সরাসরি খোলে।
     *
     * ⛔ ছয়টা Draft সেকশনই এই একটা পর্দা দিয়ে খোলে, তাই এই একটা ফাংশনেই
     *    সবকটাতে কাজ করে — আলাদা করে কিছু বসাতে হয়নি।
     * ⛔ ডেটাবেসে কিছু লেখা হয় না, ক্লাউডে একটাও বাড়তি অনুরোধ নেই — আগে
     *    থেকেই স্ক্রিনে-লোড-করা তালিকা থেকেই ফাইল বানানো হয়।
     */
    private fun downloadSheet() {
        if (sheetBusy) return
        if (entries.isEmpty()) {
            Toast.makeText(this, "Nothing to download — the list is empty", Toast.LENGTH_SHORT).show()
            return
        }
        sheetBusy = true
        Toast.makeText(this, "Preparing sheet…", Toast.LENGTH_SHORT).show()
        val rows = ArrayList(entries)
        val title = screenTitle
        lifecycleScope.launch {
            // 🔴🔒 V1620 (২০.০৯.২০২৬, TK-রিপোর্ট — Follow-up-এর একই ধরনের বার্তায়)
            // — আগে ব্যর্থ হলে আসল কারণ চুপচাপ গিলে ফেলা হতো। এখন Exception-এর
            // নিজের কথা দেখানো হয়, যাতে সত্যিই কী আটকাল বোঝা যায়।
            var failReason = ""
            val file = try {
                withContext(Dispatchers.IO) { DraftSheetExporter.write(this@DraftListActivity, title, rows) }
            } catch (t: Throwable) {
                failReason = t.javaClass.simpleName + (t.message?.let { ": $it" } ?: "")
                null
            }
            sheetBusy = false
            if (file == null || !file.exists()) {
                val why = failReason.ifBlank { "file not created" }
                Toast.makeText(this@DraftListActivity, "Could not make the sheet — $why", Toast.LENGTH_LONG).show()
                return@launch
            }
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@DraftListActivity, "$packageName.fileprovider", file
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, file.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(send, "Save / Send sheet"))
            } catch (_: Throwable) {
                Toast.makeText(this@DraftListActivity, "Sheet made, but this phone could not open the share window", Toast.LENGTH_LONG).show()
            }
        }
    }
}
