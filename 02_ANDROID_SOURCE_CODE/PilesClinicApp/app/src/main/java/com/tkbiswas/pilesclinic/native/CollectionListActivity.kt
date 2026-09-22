package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.databinding.ActivityCollectionListBinding
import com.tkbiswas.pilesclinic.print.BranchCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 *  TK-APPROVED (2026-07-25, photo proof): Master Admin only.
 *
 *  Two lists that the app did not have before:
 *    . "Monthly Collection"   -> every collection of ONE chosen month
 *    . "Collection History"   -> every collection ever taken
 *
 *  A BRANCH selector sits on top (All Branch + every branch) . TK asked for
 *  this twice, so it must never be removed.
 *
 *  Nothing about the daily Payment screen changed. The rows are read with
 *  PaymentRepository.fetchCollectionRange(), which reads the SAME two tables
 *  (payments + products) and uses the SAME row parsing as Today's Collection,
 *  so an amount can never be counted differently here.
 */
class CollectionListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionListBinding
    private lateinit var repository: PaymentRepository
    private lateinit var adapter: CollectionAdapter
    private lateinit var user: NativeUser

    private var monthly = true
    private val branches = BranchFilterStore.choices()   // 🟢🔒 V398: একটাই তালিকা
    private val monthKeys = mutableListOf<String>()     // yyyy-MM, newest first
    private var selectedBranch = ""                      // 🟢🔒 V398: মনে-রাখা মানের প্রতিচ্ছবি
    private var selectedMonth = ""
    // V1521 — Collection History keeps its old All-Years default. Year is only
    // applied when Master explicitly picks one from ⋮, so upgrading cannot hide
    // older money records silently. Monthly Collection remains month-driven.
    private var selectedHistoryYear: Int? = null

    // 🔴🔒 V457 (20.08.2026, TK-অনুমোদিত ছোট কাজ): PaymentActivity.kt-এর
    // প্রমাণিত একই lifecycle pattern — Monthly Collection/History-ও ৩০
    // সেকেন্ডে চেক করে স্বয়ংক্রিয়ভাবে নতুন হবে। ⛔ `load()`-এর ভেতরের
    // হিসাব/ছাঁকনি/cache-লজিক এক অক্ষরও বদলায়নি — শুধু "কখন আবার load()
    // ডাকা হবে" তার নিয়ম যোগ হয়েছে।
    private val autoHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var autoScreenFocused = true
    private var autoBusy = false
    private val autoWatch = LiveRefresh.Watch("payments")
    private val autoTick = object : Runnable {
        override fun run() {
            try { autoCheckForChanges() } catch (_: Throwable) { }
            autoHandler.postDelayed(this, LiveRefresh.TICK_MS)
        }
    }

    private fun autoCheckForChanges() {
        if (!::repository.isInitialized || !::user.isInitialized) return
        if (!autoScreenFocused || autoBusy) return
        if (!LiveRefresh.awake()) return
        if (BranchFilterStore.notChosen(this, user)) return
        val br = selectedBranch
        autoBusy = true
        lifecycleScope.launch {
            try {
                val changed = withContext(Dispatchers.IO) {
                    autoWatch.changed("colllist|$monthly|$selectedMonth|${selectedHistoryYear ?: "all"}|$br", br)
                }
                if (isFinishing || isDestroyed) return@launch
                if (!changed) return@launch
                if (br == selectedBranch && autoScreenFocused) load()
            } catch (_: Throwable) {
            } finally {
                autoBusy = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        autoHandler.removeCallbacks(autoTick)
        autoHandler.postDelayed(autoTick, LiveRefresh.TICK_MS)
        autoWatch.startRealtime({ selectedBranch }) {
            if (!::repository.isInitialized || !::user.isInitialized || !autoScreenFocused) return@startRealtime
            if (BranchFilterStore.notChosen(this, user)) return@startRealtime
            load()
        }
    }

    override fun onPause() {
        super.onPause()
        autoHandler.removeCallbacks(autoTick)
        autoWatch.stopRealtime()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        autoScreenFocused = hasFocus
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TK-REPORTED (2026-07-27): this screen has no bottom bar, so until
        // now opening it never retried a save that was still stuck on this
        // phone. A staff member could sit here while a registration or a
        // payment stayed unsent. Same retry every other screen already does.
        try { BottomNav.retryStuckSaves(this) } catch (_: Throwable) { }
        binding = ActivityCollectionListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = PaymentRepository(this)

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session
        // Master only. Anyone else can never open this screen.
        if (user.role != "master") { finish(); return }

        monthly = intent.getStringExtra("mode") != "history"
        binding.tvTitle.text = if (monthly) "Monthly Collection" else "Collection History"
        binding.spMonth.visibility = if (monthly) View.VISIBLE else View.GONE

        // 🔒 B572 (TK-নির্দেশ): নামে ট্যাপ করলে ওই রোগীর History (Full Journey) খোলে।
        adapter = CollectionAdapter(this, emptyList(),
            onNameClick = { row ->
                val d = row.mobile.filter { it.isDigit() }.takeLast(10)
                if (d.length == 10) startActivity(android.content.Intent(this, PatientTimelineActivity::class.java).putExtra("mobile", d))
            })
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        // TK-REQUESTED (2026-07-27): pull the list down to refresh -- the same
        // gesture as Follow-up, on every section. It runs the screen's OWN
        // existing load, so no new query and no rule changes; the little circle
        // stops on its own when that load has had time to finish.
        binding.swipeRefresh.setColorSchemeColors(
            android.graphics.Color.parseColor("#0EA25F"),
            android.graphics.Color.parseColor("#1167D8")
        )
        binding.swipeRefresh.setOnRefreshListener {
            load()
            binding.swipeRefresh.postDelayed({
                try { binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
            }, 2500L)
        }

        binding.btnBack.setOnClickListener { finish() }
        setupHistoryYearMenu()

        setupBranchSpinner()
        if (monthly) setupMonthSpinner()

        load()
    }

    private fun setupHistoryYearMenu() {
        if (monthly) { binding.btnMore.visibility = View.GONE; return }
        binding.btnMore.visibility = View.VISIBLE
        binding.btnMore.setOnClickListener { anchor ->
            val pm = android.widget.PopupMenu(this, anchor)
            val nowYear = Calendar.getInstance().get(Calendar.YEAR)
            val minYear = BranchCatalog.minYearFor(selectedBranch)
            for (y in nowYear downTo maxOf(minYear, nowYear - 5)) {
                pm.menu.add(0, 10000 + y, 0, (if (selectedHistoryYear == y) "✓ " else "") + "Year $y")
            }
            pm.menu.add(0, 20001, 0, (if (selectedHistoryYear == null) "✓ " else "") + "All Years")
            pm.setOnMenuItemClickListener { item ->
                selectedHistoryYear = if (item.itemId == 20001) null else item.itemId - 10000
                updateHistoryTitle()
                load()
                true
            }
            pm.show()
        }
        updateHistoryTitle()
    }

    private fun updateHistoryTitle() {
        if (monthly) return
        binding.tvTitle.text = selectedHistoryYear?.let { "Collection History · $it" } ?: "Collection History"
    }

    private fun setupBranchSpinner() {
        val a = ArrayAdapter(this, android.R.layout.simple_spinner_item, branches)
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spBranch.adapter = a
        // 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): আগে সবসময় ০ নম্বর ("All") বসত;
        //   এখন শেষবার বাছা ব্রাঞ্চটাই বসে (পুরো অ্যাপে একটাই জায়গা)।
        selectedBranch = if (user.role == "master") BranchFilterStore.get(this) else user.branch
        binding.branchPicker.text = BranchFilterStore.pillText(this)
        binding.spBranch.setSelection(branches.indexOf(selectedBranch).coerceAtLeast(0))
        binding.spBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBranch = BranchFilterStore.set(
                    this@CollectionListActivity, branches.getOrElse(position) { BranchFilterStore.ALL })   // 🟢 V398
                // 🔒 খাতার সারি B84: হেডারের পিলের লেখাও এখানেই ঠিক হয়, তাই
                // দুটো কখনো আলাদা কথা বলবে না।
                binding.branchPicker.text = BranchFilterStore.pillText(this@CollectionListActivity)
                load()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 🔒 TK-APPROVED (29.07.2026, খাতার সারি B84): ব্রাঞ্চ বাছার ঘর এখন
        // হেডারের ডান দিকে — সব পর্দার এক মডেল। পিলে চাপ দিলে ঠিক ওই একই
        // তালিকা খোলে, আর বেছে নিলে **পুরনো Spinner-কেই** সেট করা হয় — অর্থাৎ
        // নিয়ম চালায় সেই আগের কোডই। ⛔ টাকার হিসাব · ছাঁকনি · কিছুই বদলায়নি।
        // 🔒 খাতার সারি B84 — পপ-আপটা **Follow-up-এর হুবহু একই** (TK ২৯.০৭.২০২৬-এ
        // ছবি দিয়ে দেখিয়ে দিয়েছেন): শিরোনাম "Branch" · গোল বোতামের তালিকা
        // (এখন যেটা বাছা আছে সেটায় দাগ) · নিচে "Cancel"। ⛔ এর থেকে আলাদা
        // কোনো পপ-আপ কোথাও বসানো যাবে না।
        binding.branchPicker.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(branches.toTypedArray(), BranchFilterStore.indexInChoices(this)) { dialog, which ->
                    binding.spBranch.setSelection(which)
                    // 🚨 খাতার সারি B126 (TK, 29.07.2026 সন্ধ্যা ৬.২৩): লুকানো
                    // (`gone`) Spinner layout হয় না বলে তার `onItemSelected`
                    // চলত না — বাছাই হারিয়ে যেত। কাজটা এখন এখানেই সরাসরি হয়।
                    // ⛔ টাকার হিসাব · ছাঁকনি কিছুই বদলায়নি — সেই একই `load()`।
                    selectedBranch = BranchFilterStore.set(
                        this@CollectionListActivity, branches.getOrElse(which) { BranchFilterStore.ALL })   // 🟢 V398
                    binding.branchPicker.text = BranchFilterStore.pillText(this@CollectionListActivity)
                    load()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    /** Last 24 months, newest first, shown as "JULY 2026". */
    private fun setupMonthSpinner() {
        val keyFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val showFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
        val cal = Calendar.getInstance()
        val labels = mutableListOf<String>()
        for (i in 0 until 24) {
            monthKeys.add(keyFmt.format(cal.time))
            labels.add(showFmt.format(cal.time).uppercase(Locale.US))
            cal.add(Calendar.MONTH, -1)
        }
        selectedMonth = monthKeys.firstOrNull() ?: keyFmt.format(Calendar.getInstance().time)
        val a = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spMonth.adapter = a
        binding.spMonth.setSelection(0)
        binding.spMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedMonth = monthKeys.getOrElse(position) { selectedMonth }
                load()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun monthRange(monthKey: String): Pair<String, String> {
        val parts = monthKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val from = String.format(Locale.US, "%04d-%02d-01", year, month)
        val to = String.format(Locale.US, "%04d-%02d-%02d", year, month, last)
        return Pair(from, to)
    }

    private fun load() {
        // 🟢🔒 V398: ব্রাঞ্চ না-বাছা থাকলে কোনো অনুরোধ যাবে না — শুধু বার্তা।
        if (BranchFilterStore.notChosen(this, user)) {
            adapter.updateItems(emptyList())
            binding.tvTotal.text = "₹0"
            binding.tvEmpty.text = BranchFilterStore.ASK_TEXT
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }
        val from: String
        val to: String
        if (monthly) {
            val r = monthRange(selectedMonth)
            from = r.first
            to = r.second
        } else {
            val y = selectedHistoryYear
            if (y == null) {
                from = "2000-01-01"
                to = PaymentModel.today()
            } else {
                from = String.format(Locale.US, "%04d-01-01", y)
                to = String.format(Locale.US, "%04d-12-31", y)
            }
        }
        // 🔵🔒 (09.08.2026, TK-নির্দেশ "লোডিং দেরি ঠিক করুন, খুব সাবধানে" — Appointment/
        // ExpectedTomorrow-এর প্রমাণিত cache-first প্যাটার্নের মিরর): শেষবার সফলভাবে আনা
        // তালিকা এই পর্দার নিজের SharedPreferences-এ (ব্রাঞ্চ+মাস/সব ধরে) জমা থাকে; পাতা
        // খুললেই সাথে সাথে দেখায়, তারপর ক্লাউড থেকে হালনাগাদ এলে বদলায়। ⛔ সারি/টোটাল/
        // ছাঁকনি/নেট-ফেল-হ্যান্ডলিং কিছু বদলায়নি — শুধু প্রথম দেখানো এগিয়ে আনা (additive)।
        val cacheKey = collCacheKey()
        val cached = loadCachedCollection(cacheKey)
        if (cached != null) {
            renderCollection(cached)
        } else {
            binding.tvEmpty.text = "Loading..."
            binding.tvEmpty.visibility = View.VISIBLE
        }
        lifecycleScope.launch {
            val rows = try {
                withContext(Dispatchers.IO) { repository.fetchCollectionRange(selectedBranch, from, to) }
            } catch (_: Throwable) {
                null
            }
            // 🔵 TK-ORDER (07.08.2026): পড়া ব্যর্থ (null) হলে আর ₹0/"No collection"
            // দেখাব না — আগের তালিকা/টোটাল যেমন আছে তেমন থাক। প্রথম লোডে (তালিকা
            // খালি) শুধু "লোড করা গেল না" জানাই। ⛔ বাড়তি কোনো cloud-call নেই।
            // 🔁 পুরনো: catch-এ `emptyList<CollectionRow>()` ছিল → ব্যর্থে ₹0 দেখাত।
            if (rows == null) {
                if (adapter.itemCount == 0) {
                    binding.tvEmpty.text = "Could not load — check connection and try again"
                    binding.tvEmpty.visibility = View.VISIBLE
                }
                return@launch
            }
            saveCachedCollection(cacheKey, rows)
            renderCollection(rows)
        }
    }

    // একটাই render — cache ও তাজা দুই পথেই ব্যবহার (আগের load()-এর হুবহু কোড, শুধু ফাংশনে সরানো)।
    private fun renderCollection(rows: List<CollectionRow>) {
        val shown = rows.map {
            val extra = listOf(DateUtil.display(it.date), it.branch)
                .filter { part -> part.isNotBlank() }
                .joinToString(" · ")
            if (extra.isBlank()) it else it.copy(source = it.source + " · " + extra)
        }
        adapter.updateItems(shown)
        val total = rows.sumOf { it.amount }
        binding.tvTotal.text = "₹" + "%,.0f".format(total)
        val where = if (selectedBranch == "All") "ALL BRANCH" else selectedBranch.uppercase(Locale.US)
        binding.tvSubTotal.text = "${rows.size} Transactions · $where"
        // 🟢 B634 (11.08.2026, TK-অনুমোদিত প্রুফ): Cash/Online/Patients — Today's Collection-এর
        //   (PaymentActivity) হুবহু একই হিসাব। ONLINE = CASH-নয় সবকিছু (পুরনো UPI-ও ধরা পড়ে);
        //   Patients = আলাদা মোবাইল (ফাঁকা হলে নাম) সংখ্যা। ⛔ একই rows থেকেই — নতুন query/Egress নেই।
        val cash = rows.sumOf { it.cashAmount }
        val online = rows.sumOf { it.onlineAmount }
        val patientCount = rows.map { it.mobile.ifBlank { it.name } }.filter { it.isNotBlank() }.distinct().size
        binding.tvClCash.text = "₹" + "%,.0f".format(cash)
        binding.tvClOnline.text = "₹" + "%,.0f".format(online)
        binding.tvClPatients.text = patientCount.toString()
        /* 🔴 V487: Today's Collection-এর হুবহু একই নিয়ম — মাইনাস হলে লাল। */
        binding.tvTotal.setTextColor(android.graphics.Color.parseColor(if (total < 0.0) "#FFB4AB" else "#FFFFFF"))
        binding.tvClCash.setTextColor(android.graphics.Color.parseColor(if (cash < 0.0) "#B3261E" else "#0B7A34"))
        binding.tvClOnline.setTextColor(android.graphics.Color.parseColor(if (online < 0.0) "#B3261E" else "#1457B8"))
        binding.tvEmpty.text = "No collection found"
        binding.tvEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }

    // 🔵 এই পর্দার নিজের cache (Appointment/ExpectedTomorrow-এর মতোই) — ব্রাঞ্চ+মাস/সব ধরে।
    // ⛔ টাকার হিসাব এখানে নয় (শুধু শেষ-জানা তালিকা); আসল সংখ্যা ক্লাউড রিফ্রেশে বসে।
    private fun collCachePrefs() = getSharedPreferences("collection_list_cache", MODE_PRIVATE)
    private fun collCacheKey() = "cl_" + selectedBranch + "_" +
        (if (monthly) "m_" + selectedMonth else "history_" + (selectedHistoryYear?.toString() ?: "all"))
    private fun loadCachedCollection(key: String): List<CollectionRow>? {
        return try {
            val json = collCachePrefs().getString(key, null) ?: return null
            val arr = org.json.JSONArray(json)
            val list = ArrayList<CollectionRow>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(CollectionRow(
                    source = o.optString("source", ""), date = o.optString("date", ""),
                    name = o.optString("name", ""), mobile = o.optString("mobile", ""),
                    branch = o.optString("branch", ""), mode = o.optString("mode", ""),
                    amount = o.optDouble("amount", 0.0), patientId = o.optString("patientId", ""),
                    cashAmount = o.optDouble("cashAmount", if (o.optString("mode", "").equals("CASH", true)) o.optDouble("amount", 0.0) else 0.0),
                    onlineAmount = o.optDouble("onlineAmount", if (!o.optString("mode", "").equals("CASH", true)) o.optDouble("amount", 0.0) else 0.0),
                    paymentEventCount = o.optInt("paymentEventCount", 1).coerceAtLeast(1)
                ))
            }
            list
        } catch (_: Throwable) { null }
    }
    private fun saveCachedCollection(key: String, rows: List<CollectionRow>) {
        try {
            val arr = org.json.JSONArray()
            for (r in rows) arr.put(
                org.json.JSONObject()
                    .put("source", r.source).put("date", r.date).put("name", r.name)
                    .put("mobile", r.mobile).put("branch", r.branch).put("mode", r.mode)
                    .put("amount", r.amount).put("patientId", r.patientId)
                    .put("cashAmount", r.cashAmount).put("onlineAmount", r.onlineAmount)
                    .put("paymentEventCount", r.paymentEventCount)
            )
            collCachePrefs().edit().putString(key, arr.toString()).apply()
        } catch (_: Throwable) { }
    }
}
