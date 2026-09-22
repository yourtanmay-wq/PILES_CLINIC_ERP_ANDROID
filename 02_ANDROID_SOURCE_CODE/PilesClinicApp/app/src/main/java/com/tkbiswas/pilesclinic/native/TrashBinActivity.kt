package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.databinding.ActivityTrashBinBinding
import com.tkbiswas.pilesclinic.print.BranchCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native rebuild -- Trash Bin (Master only). Lists soft-deleted records from the
 * Supabase "trash" table and restores them, matching trashBin()/restoreTrash().
 */
class TrashBinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrashBinBinding
    private lateinit var repository: TrashRepository
    private lateinit var adapter: TrashAdapter
    private lateinit var user: NativeUser
    // V227 (item 46): Master-only branch filter (default "All" = every branch,
    // exactly as before). Staff never see this — they are blocked from the
    // whole screen by the master-only guard in onCreate.
    private var pickedBranch: String = ""   // 🟢🔒 V398: BranchFilterStore-এর প্রতিচ্ছবি

    // ══════════════════════════════════════════════════════════════════════
    // 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-অনুমোদিত প্রুফ)
    // TK-এর কথা: *"অনেকগুলো একসাথে সিলেক্ট করা যাবে তারও ব্যবস্থা রাখতে হবে,
    // যাতে অনেকগুলো সিলেক্ট করে আমি রিস্টোর অথবা ডিলিট ফরএভার যে কোনো একটা
    // করতে পারি।"*
    // ⛔ `loaded` = ক্লাউড/ক্যাশ থেকে আসা **আসল** তালিকা (ব্রাঞ্চের ছাঁকনি বসানো)।
    //    `query` শুধু দেখার সময় ছাঁকে — ক্লাউডে একটাও নতুন অনুরোধ যায় না।
    // ⛔ `picked` রাখে **আইডি**, গোটা সারি নয় — তালিকা নতুন করে এলেও বাছাটা
    //    ঠিক সারির সঙ্গেই থাকে।
    // ══════════════════════════════════════════════════════════════════════
    private var loaded: List<TrashItem> = emptyList()
    private var query: String = ""
    // V1521 — All Years remains the default so upgrading cannot hide old Trash.
    // The year filter is local to already-loaded deletedAt and never changes
    // Restore/Delete permissions or cloud verification.
    private var selectedYear: Int? = null
    private val picked = LinkedHashSet<String>()

    // ══════════════════════════════════════════════════════════════════════
    // 🔴🔴🔒 V512 (২১.০৮.২০২৬) — **ক্লাউড থেকে সত্যি তালিকা না এলে Restore /
    //    Delete Forever কিছুই করা যাবে না।**
    //
    // কেন (নিজের কাজ আবার যাচাই করতে গিয়ে ধরা পড়েছে): V512-এর আগে পড়া ব্যর্থ
    // হলে তালিকা ফাঁকা হয়ে যেত, তাই কিছু করার সুযোগই ছিল না। এখন (ঠিকভাবেই)
    // জমানো তালিকা পর্দায় থেকে যায় — কিন্তু ওতে এমন সারিও থাকতে পারে যেটা
    // ইতিমধ্যে **চিরতরে মুছে ফেলা** হয়েছে (এই ফোনে বা অন্য ফোনে)। ঐ সারিতে
    // Restore চাপলে `upsertRestoreSafe` ক্লাউডে কিছু না পেয়ে রেকর্ডটা **আবার
    // বসিয়ে দিত** — ইচ্ছে করে ধ্বংস করা রেকর্ড ফিরে আসত।
    //
    // ⇒ তাই: শেষ পড়াটা সফল না হলে দুটো কাজই ভদ্রভাবে আটকে দেওয়া হয়।
    //    ওয়েব অ্যাপে ঠিক এই নিয়মই আগে থেকেই আছে — *"Cloud could not be
    //    verified. Nothing can be Restore/Delete until internet/cloud is
    //    available."* (app.js — trashBin())। এখন ফোনও এক।
    // ⛔ দেখা (👁 View) ও খোঁজা আগের মতোই চলে — শুধু বদলে দেওয়ার কাজ আটকায়।
    // ══════════════════════════════════════════════════════════════════════
    private var cloudVerified = false

    /** সত্যি তালিকা না এলে `true` ফেরায় ও কারণটা বলে — তখন কাজটা থেমে যায়। */
    private fun blockedUnverified(): Boolean {
        if (cloudVerified) return false
        Toast.makeText(
            this,
            "Cloud not verified - Restore / Delete stay locked until Trash loads from cloud",
            Toast.LENGTH_LONG
        ).show()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrashBinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        repository = TrashRepository()

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session
        if (user.role != "master") {
            Toast.makeText(this, "Only Master Admin", Toast.LENGTH_LONG).show()
            finish(); return
        }

        // Only a Master ever reaches this screen (guard above), so the
        // Delete Forever button is shown here; the flag keeps that explicit.
        adapter = TrashAdapter(
            this, emptyList(),
            onRestore = { confirmRestore(it) },
            showDelete = user.role == "master",
            onDelete = { confirmDeleteForever(it) },
            // 🔴 V511 (TK-অনুমোদিত প্রুফ): 👁 View — সিদ্ধান্ত নেওয়ার আগে
            //    রেকর্ডটা একবার দেখে নেওয়া। আর একসাথে অনেকগুলো বাছা।
            onView = { showRecord(it) },
            isPicked = { picked.contains(it.id) },
            onTogglePick = { togglePick(it) }
        )
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
            // ⛔ V512: এখানে ইচ্ছে করেই অ্যাপ-জুড়ে জমানো উত্তর মোছা **হয় না**।
            //   এই পর্দার পড়াটা (`fetchTrashRawOrNull` → `fetchListOrNullDirect`)
            //   dedupe-এর বাইরে দিয়ে যায়, তাই টানলেই সরাসরি সার্ভার থেকেই আসে।
            //   সব ক্যাশ মুছে দিলে **অন্য সব পর্দার** তথ্যও আবার নামত ⇒ Egress
            //   অকারণে বাড়ত (V509-এর কাজ নষ্ট হতো)।
            loadList()
            binding.swipeRefresh.postDelayed({
                try { binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
            }, 2500L)
        }
        binding.btnBack.setOnClickListener { finish() }
        setupYearMenu()

        // ══════════════════════════════════════════════════════════════════
        // 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-অনুমোদিত প্রুফ) — খোঁজা · Select মোড ·
        //    একসাথে Restore/Delete।
        // ⛔ কোনো নতুন ক্লাউড-অনুরোধ নেই — খোঁজাটা **ফোনের ভিতরেই** ইতিমধ্যে
        //    নামানো তালিকার উপরে চলে (Egress-এ প্রভাব শূন্য)।
        // ══════════════════════════════════════════════════════════════════
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                query = s?.toString()?.trim() ?: ""
                paint()
            }
        })
        binding.btnSelectMode.setOnClickListener { setSelectMode(!adapter.selectMode) }
        /* 🔴🔒 V790 — উপরের সবুজ/লাল দুটো বোতামও একই ফাঁদে পড়েছিল
           (MaterialButton `android:background` অগ্রাহ্য করে; বিশদ ব্যাখ্যা
           `TrashAdapter.onBindViewHolder`-এ)। প্রকল্পের প্রমাণিত ওষুধ —
           `backgroundTintList = null`। ⛔ কী চাপলে কী হয়, কিচ্ছু বদলায়নি। */
        binding.btnBulkRestore.backgroundTintList = null
        binding.btnBulkDelete.backgroundTintList = null
        binding.btnBulkRestore.isAllCaps = false
        binding.btnBulkDelete.isAllCaps = false

        binding.btnBulkRestore.setOnClickListener { confirmBulkRestore() }
        binding.btnBulkDelete.setOnClickListener { confirmBulkDelete() }

        // V227 (item 46): Master picks a branch (or keeps All) — same rule as the
        // Draft / Reject / Incomplete lists. Cloud query is untouched; the filter
        // is applied client-side in branchScoped(), so no extra cloud usage.
        if (user.role == "master") {
            // 🟢🔒 V398: শেষবার বাছা ব্রাঞ্চটাই বসে (পুরো অ্যাপে একটাই জায়গা)।
            pickedBranch = BranchFilterStore.get(this)
            binding.branchFilter.visibility = View.VISIBLE
            binding.branchFilter.text = BranchFilterStore.pillText(this)
            binding.branchFilter.setOnClickListener { showBranchPicker() }
        } else {
            binding.branchFilter.visibility = View.GONE
        }

        loadList()
    }

    private fun setupYearMenu() {
        binding.btnMore.setOnClickListener { anchor ->
            val pm = android.widget.PopupMenu(this, anchor)
            val nowYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val effectiveBranch = if (user.role == "master") pickedBranch else user.branch
            val minYear = BranchCatalog.minYearFor(effectiveBranch)
            for (y in nowYear downTo maxOf(minYear, nowYear - 5)) {
                pm.menu.add(0, 10000 + y, 0, (if (selectedYear == y) "✓ " else "") + "Deleted in $y")
            }
            pm.menu.add(0, 20001, 0, (if (selectedYear == null) "✓ " else "") + "All Years")
            pm.setOnMenuItemClickListener { item ->
                selectedYear = if (item.itemId == 20001) null else item.itemId - 10000
                binding.tvTitle.text = selectedYear?.let { "Trash Bin · $it" } ?: "Trash Bin"
                picked.clear()
                if (adapter.selectMode) setSelectMode(false) else paint()
                true
            }
            pm.show()
        }
    }

    // V227 (item 46): branch chooser for Master only.
    private fun showBranchPicker() {
        val options = BranchFilterStore.choices().toTypedArray()
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Branch"))
            .setItems(options) { _, which ->
                pickedBranch = options[which]
                BranchFilterStore.set(this@TrashBinActivity, pickedBranch)   // 🟢 V398
                binding.branchFilter.text = BranchFilterStore.pillText(this@TrashBinActivity)
                loadList()
            }
            .show().also { PremiumAlert.paint(it) }
    }

        private var firstResume = true
    override fun onResume() {
        super.onResume()
        if (firstResume) { firstResume = false; return }
        loadList()
    }

    private fun loadList() {
        binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        // TK-REQUESTED (2026-07-24): real cache-first, same SharedPreferences
        // pattern as Payment/Doctor Visit/Follow-up -- raw rows saved after
        // every successful fetch, shown instantly next time before the fresh
        // fetch below finishes. Nothing about restore/confirm logic changes.
        val cachePrefs = getSharedPreferences("piles_clinic_trash_cache", MODE_PRIVATE)
        var hasCache = false
        try {
            val json = cachePrefs.getString("rows", null)
            if (!json.isNullOrBlank()) {
                val cachedItems = branchScoped(repository.parseTrash(org.json.JSONArray(json)))
                if (cachedItems.isNotEmpty()) {
                    hasCache = true
                    loaded = cachedItems          // 🔴 V511
                    paint()
                }
            }
        } catch (_: Throwable) { }
        if (!hasCache) {
            binding.tvEmpty.text = "Loading..."
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
        lifecycleScope.launch {
            // 🔴🔴🔒 V512 (২১.০৮.২০২৬) — আগে এখানে `fetchTrashRaw()` ডাকা হতো,
            //   কিন্তু সেটা নেট খারাপ হলেও ফাঁকা তালিকা `[]` ফেরাত (কারণ
            //   TrashRepository.fetchTrashRawOrNull-এর মন্তব্যে লেখা আছে)।
            //   ফলে নিচের `rawRows == null` পাহারাটা কখনো চালু হতো না, জমানো
            //   কপির উপরে `"[]"` লেখা হয়ে যেত আর পর্দায় বসত "Trash empty" —
            //   অথচ একটাও রেকর্ড মোছা হয়নি। এখন সত্যিকারের ব্যর্থতায় `null`
            //   আসে, তাই জমানো তালিকা পর্দাতেই থেকে যায়।
            val rawRows = try {
                withContext(Dispatchers.IO) { repository.fetchTrashRawOrNull() }
            } catch (_: Throwable) { null }
            binding.progressLoad.visibility = View.GONE
            if (rawRows == null) {
                // 🔴 V512: সত্যি তালিকা আসেনি ⇒ বদলে দেওয়ার কাজ বন্ধ, আর আগের
                //    বাছাগুলোও ছেড়ে দেওয়া হয় (নইলে "৩টা বাছা" লেখা থেকে যেত
                //    অথচ ঐ সারিগুলো এখন সত্যি আছে কিনা জানা নেই)।
                cloudVerified = false
                picked.clear()
                if (adapter.selectMode) setSelectMode(false) else refreshBulkBar()
                // ⛔ ব্যর্থ পড়া কখনো জমা হবে না — জমানো কপি অক্ষত থাকে।
                if (hasCache) {
                    android.widget.Toast.makeText(
                        this@TrashBinActivity,
                        "Offline - showing this phone's saved copy",
                        android.widget.Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                } else {
                    // ⚠️ সৎ বার্তা: "Trash empty" নয় — কারণ আমরা জানিই না
                    //    ভিতরে কী আছে। ভুল করে "সব মুছে গেছে" মনে হওয়ার
                    //    কোনো সুযোগ রাখা হলো না।
                    binding.tvEmpty.text = "Could not load Trash - check internet and pull down to retry"
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                }
                return@launch
            }
            try { cachePrefs.edit().putString("rows", rawRows.toString()).apply() } catch (_: Throwable) { }
            val items = branchScoped(repository.parseTrash(rawRows))
            binding.progressLoad.visibility = View.GONE
            // 🔴 V511: তালিকা নতুন করে এলে যে সারিগুলো আর নেই, তাদের বাছাও
            //    ছেড়ে দেওয়া হয় — নইলে "৩টা বাছা" লেখা থাকত অথচ সারি নেই।
            cloudVerified = true      // 🔴 V512: এখন তালিকাটা সত্যিই ক্লাউডের
            loaded = items
            val alive = items.map { it.id }.toSet()
            picked.retainAll(alive)
            paint()
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-অনুমোদিত প্রুফ) — নিচের সবটুকুই নতুন।
    // ⛔ একটাও নতুন ক্লাউড-অনুরোধ নেই: খোঁজা ও বাছা সম্পূর্ণ ফোনের ভিতরে।
    // ⛔ Restore/Delete আসলে কী করে — সেই নিয়ম এক অক্ষরও বদলায়নি; একসাথে
    //    কয়েকটা করলে সেই **একই** ফাংশনগুলোই একটার পর একটা চলে।
    // ══════════════════════════════════════════════════════════════════════

    /** খোঁজার লেখা মিলিয়ে দেখার তালিকা (আসল তালিকা অক্ষত থাকে)। */
    private fun visible(): List<TrashItem> {
        val y = selectedYear
        val yearScoped = if (y == null) loaded else loaded.filter { item ->
            item.deletedAt.trim().take(4).toIntOrNull() == y
        }
        val q = query.trim().lowercase()
        if (q.isBlank()) return yearScoped
        return yearScoped.filter { item ->
            val r = item.record
            listOf(
                item.label, r.s("name"), r.s("mobile"), r.s("altMobile"),
                r.s("patientId"), r.s("patientCode"), r.s("branch"), item.table
            ).any { it.lowercase().contains(q) }
        }
    }

    /** তালিকা ও নিচের পটি — দুটোই একসাথে নতুন করে আঁকে। */
    private fun paint() {
        val list = visible()
        adapter.updateItems(list)
        if (list.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.text = if (query.isNotBlank()) "No match" else "Trash empty"
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
        }
        refreshBulkBar()
    }

    private fun setSelectMode(on: Boolean) {
        adapter.selectMode = on
        if (!on) picked.clear()
        binding.btnSelectMode.text = if (on) "✕ Cancel" else "☑ Select"
        binding.btnSelectMode.setBackgroundResource(
            if (on) com.tkbiswas.pilesclinic.R.drawable.bg_trash_select_on
            else com.tkbiswas.pilesclinic.R.drawable.bg_trash_select_off
        )
        binding.btnSelectMode.setTextColor(
            android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#1167D8")
        )
        adapter.notifyDataSetChanged()
        refreshBulkBar()
    }

    private fun togglePick(item: TrashItem) {
        if (item.id.isBlank()) return
        if (!picked.remove(item.id)) picked.add(item.id)
        refreshBulkBar()
    }

    private fun refreshBulkBar() {
        val n = picked.size
        val show = adapter.selectMode && n > 0
        binding.bulkBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvBulkCount.text = "$n selected"
        binding.btnBulkRestore.text = "♻ Restore ($n)"
        binding.btnBulkDelete.text = "🗑 Delete ($n)"
        binding.btnBulkDelete.visibility =
            if (user.role == "master") View.VISIBLE else View.GONE
    }

    /** বাছা আইডিগুলোর আসল সারি — যেগুলো এখনো তালিকায় আছে, শুধু সেগুলোই। */
    private fun pickedItems(): List<TrashItem> = loaded.filter { picked.contains(it.id) }

    /**
     * 👁 View — TK-এর কথা: *"সেখানে চাপ দিলে অন্তত আমি একবার দেখে নিতে পারি
     * যে এনাকে আমি রিস্টোর করবো না ডিলিট ফরএভার করব।"*
     * ⛔ ক্লাউডে কিছু যায় না — Trash সারির ভিতরে যা আছে, শুধু সেটাই দেখানো হয়।
     */
    private fun showRecord(item: TrashItem) {
        val d = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "👁 " + item.label.ifBlank { "Deleted record" }))
            .setPositiveButton("♻ Restore") { _, _ -> confirmRestore(item) }
            .setNegativeButton("Close", null)
        if (user.role == "master") d.setNeutralButton("🗑 Delete") { _, _ -> confirmDeleteForever(item) }

        /* 🟢🔒 V590 (২৩.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) — *"View-তে চাপার পর আগে
           যেখানে ছিল সেখানকার মতনই চেহারা দেখতে হতে হবে।"*
           ⇒ যে পর্দায় সারিটা ছিল, সেই পর্দার **আসল কার্ডটাই** উপরে বসে
             (`TrashSourceCard` — ওখানেই পুরো ব্যাখ্যা)। তার নিচে ছোট লাল
             লাইনে কে মুছেছেন ও কখন।
           ⛔ কার্ড বানানো না গেলে (patients · doctor_visits · অন্য টেবিল)
              **হুবহু আগের সেই লেখার তালিকাই** দেখায় — পুরোনো আচরণ অক্ষত। */
        val card = TrashSourceCard.build(this, item)
        if (card == null) {
            val fields = TrashCardText.viewFields(item)
            val sb = StringBuilder()
            for ((k, v) in fields) sb.append(k).append(":  ").append(v).append("\n\n")
            d.setMessage(sb.toString().trimEnd())
        } else {
            val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
            val box = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(4))
            }
            box.addView(TextView(this).apply {
                text = "As it was in " + TrashCardText.sourceLabel(item.table)
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                setPadding(0, 0, 0, dp(6))
            })
            box.addView(card)
            // 🗑 কে মুছেছেন, কখন — Trash-এর নিজের তথ্য, কার্ডে থাকে না
            val who = TrashCardText.deletedByName(item)
            val whenT = TrashCardText.whenText(item)   // 🔴 V1162 — লাইন-ভাঙা আর নেই
            val line = listOf(who, whenT).filter { it.isNotBlank() }.joinToString("  ·  ")
            if (line.isNotBlank()) {
                box.addView(TextView(this).apply {
                    text = "🗑  Deleted by $line"
                    textSize = 12f
                    setTextColor(android.graphics.Color.parseColor("#8A2C26"))
                    /* ⛔ বাক্সের সাজটা প্রজেক্টের আগে থেকেই থাকা drawable থেকে
                       (নতুন করে বানানো হয়নি) — তাই বাকি সতর্ক-বাক্সগুলোর সাথে
                       এক দেখায়। */
                    setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_dup_note)
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.topMargin = dp(10); layoutParams = lp
                })
            }
            d.setView(android.widget.ScrollView(this).apply { addView(box) })
        }
        d.show().also { PremiumAlert.paint(it) }
    }

    private fun confirmBulkRestore() {
        if (blockedUnverified()) return   // 🔴 V512
        val list = pickedItems()
        if (list.isEmpty()) return
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Restore ${list.size} records"))
            .setMessage("Restore these ${list.size} deleted records back to their own lists?")
            .setPositiveButton("Restore") { _, _ -> runBulk(list, restore = true) }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun confirmBulkDelete() {
        if (blockedUnverified()) return   // 🔴 V512
        if (user.role != "master") {
            Toast.makeText(this, "Only Master Admin", Toast.LENGTH_SHORT).show()
            return
        }
        val list = pickedItems()
        if (list.isEmpty()) return
        // ⛔ TK-এর ২০.০৭.২০২৬-এর নিয়ম — চিরতরে মোছার আগে "Sure?" পপ-আপ।
        //    একসাথে অনেকগুলোতেও সেই একই পাহারা, সংখ্যাসহ।
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Sure?"))
            .setMessage("Delete these ${list.size} records forever? This cannot be undone.")
            .setPositiveButton("Yes, Delete") { _, _ -> runBulk(list, restore = false) }
            .setNegativeButton("No", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /**
     * একটার পর একটা — **হুবহু সেই একই** `restore()` / `permanentDelete()`।
     * ⛔ একটা ব্যর্থ হলে বাকিগুলো থেমে যায় না; শেষে কতগুলো হলো, কতগুলো হলো না
     *    সেটা পরিষ্কার করে বলা হয় (TK-এর নিয়ম: *"সফল বলে দাবি করবেন না"*)।
     */
    private fun runBulk(list: List<TrashItem>, restore: Boolean) {
        binding.btnBulkRestore.isEnabled = false
        binding.btnBulkDelete.isEnabled = false
        lifecycleScope.launch {
            var ok = 0
            var fail = 0
            for (item in list) {
                val done = withContext(Dispatchers.IO) {
                    try {
                        if (restore) repository.restore(item, applicationContext)
                        else repository.permanentDelete(item)
                    } catch (_: Throwable) { false }
                }
                if (done) ok++ else fail++
            }
            binding.btnBulkRestore.isEnabled = true
            binding.btnBulkDelete.isEnabled = true
            picked.clear()
            setSelectMode(false)
            val word = if (restore) "Restored" else "Deleted"
            Toast.makeText(
                this@TrashBinActivity,
                if (fail == 0) "$word $ok" else "$word $ok · $fail failed — check connection",
                Toast.LENGTH_LONG
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            loadList()
        }
    }

    private fun confirmRestore(item: TrashItem) {
        if (blockedUnverified()) return   // 🔴 V512
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Restore record"))
            .setMessage("Restore \"${item.label}\" back to ${item.table}?")
            .setPositiveButton("Restore") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { repository.restore(item, applicationContext) }
                    Toast.makeText(this@TrashBinActivity, if (ok) "Restored" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                    if (ok) loadList()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // TK APPROVED (2026-07-20): "Sure?" popup before a permanent delete.
    // Only Yes actually removes the record from the app for good; there is
    // no undo after this. Master-only (this whole screen is Master-only).
    private fun confirmDeleteForever(item: TrashItem) {
        if (blockedUnverified()) return   // 🔴 V512
        if (user.role != "master") {
            Toast.makeText(this, "Only Master Admin", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Sure?"))
            .setMessage("Delete \"${item.label}\" forever? This cannot be undone.")
            .setPositiveButton("Yes, Delete") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { repository.permanentDelete(item) }
                    Toast.makeText(
                        this@TrashBinActivity,
                        if (ok) "Deleted forever" else "Failed — check connection",
                        Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) loadList()
                }
            }
            .setNegativeButton("No", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** TK-ORDER (2026-07-25, branch-leak sweep): the Trash bin used to list
     *  deleted records from EVERY branch to everyone. A staff must only ever
     *  see their own branch's deleted records; Master still sees all. */
    private fun branchScoped(items: List<TrashItem>): List<TrashItem> {
        val me = NativeSession.current(this) ?: return emptyList()
        if (me.role == "master") {
            // V227 (item 46): Master default "All" = every branch (unchanged);
            // if a branch is picked, show only that one.
            val pick = pickedBranch.trim()
            // 🟢🔒 V398: ফাঁকা মানে "এখনো বাছা হয়নি" → কিছুই দেখাবে না (আগে
            //   ফাঁকা মানে সব দেখাত)। ⛔ "All" বাছলে আগের মতোই সব।
            if (pick.isBlank()) return emptyList()
            if (pick.equals("All", ignoreCase = true)) return items
            return items.filter { it.record.s("branch").trim().equals(pick, ignoreCase = true) }
        }
        val mine = me.branch.trim()
        if (mine.isBlank() || mine.equals("All", ignoreCase = true)) return items
        return items.filter { it.record.s("branch").trim().equals(mine, ignoreCase = true) }
    }

}
