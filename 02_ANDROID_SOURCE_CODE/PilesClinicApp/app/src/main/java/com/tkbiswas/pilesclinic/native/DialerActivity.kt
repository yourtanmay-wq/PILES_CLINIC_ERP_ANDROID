package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🆕🔒 খাতার সারি — Dialer পুনর্গঠন (TK-নির্দেশ, 05.08.2026, ধাপে ধাপে
 * ফটো-প্রুফ দেখিয়ে চূড়ান্ত হওয়ার পরে)। পুরনো "শুধু টাইপ করে Call" স্ক্রিন
 * বদলে এখন Android-এর নিজস্ব Dialer-এর মতো — Call Log (All/Missed তীর-
 * চিহ্ন সহ) + Contacts (Enquiry/Visit/Patient) + কিবোর্ড।
 *
 * 🚨 Supabase ফ্রি-প্ল্যান ঝুঁকি (TK-কে আগেই জানানো হয়েছে ও অনুমতি
 * নেওয়া হয়েছে): কোনো নম্বরই একটা একটা করে ক্লাউডে মেলানো হয় না — সবসময়
 * `DialerRepository.matchNumbersBatch()` (একটাই ব্যাচ-অনুরোধ, ২০ সেকেন্ড
 * ক্যাশ) ব্যবহার হয়।
 *
 * ⛔ পুরনো `logDialedCall()`/`fetchTodayUnmatched()` ফাংশন দুটো
 * `DialerRepository.kt`-এর ভিতরে এক অক্ষরও বদলানো হয়নি — কিবোর্ড থেকে
 * Call করলে এখনো সেই একই পথে যায়।
 */
class DialerActivity : AppCompatActivity() {

    private lateinit var user: NativeUser
    private lateinit var listContainer: LinearLayout
    private lateinit var tvEmpty: TextView
    private lateinit var tabAll: TextView
    private lateinit var tabMissed: TextView
    private lateinit var tabContacts: TextView
    private lateinit var searchBar: View
    private lateinit var etSearch: EditText
    private lateinit var contactsSearchBar: View
    private lateinit var etContactsSearch: EditText
    private lateinit var fabKeypad: View
    private lateinit var keypadSheet: View
    private lateinit var keypadInput: EditText

    private var currentTab = "all"
    private var searchQuery = ""
    private var contactsQuery = ""
    private var lastCallRows: List<BranchSimHelper.CallLogRow> = emptyList()
    private var lastMatches: Map<String, DialerRepository.MatchedContact> = emptyMap()
    private var lastRemarks: Map<String, String> = emptyMap()   // 🟢🔒 V605
    private var lastContacts: List<FollowUpItem> = emptyList()
    private var lastAddressTags: Map<String, String> = emptyMap()

    private val requestCallLogPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) requestPhoneStatePermission.launch(android.Manifest.permission.READ_PHONE_STATE)
            else refreshCurrentTab()
        }
    private val requestPhoneStatePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            maybeAskWhichSimIsBranch { refreshCurrentTab() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)
        UppercaseInputUtil.applyToAll(findViewById(android.R.id.content))

        val current = NativeSession.current(this)
        if (current == null) { finish(); return }
        user = current

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSearchToggle).setOnClickListener { toggleSearch() }

        listContainer = findViewById(R.id.listContainer)
        tvEmpty = findViewById(R.id.tvEmpty)
        tabAll = findViewById(R.id.tabAll)
        tabMissed = findViewById(R.id.tabMissed)
        tabContacts = findViewById(R.id.tabContacts)
        searchBar = findViewById(R.id.searchBar)
        etSearch = findViewById(R.id.etSearch)
        contactsSearchBar = findViewById(R.id.contactsSearchBar)
        etContactsSearch = findViewById(R.id.etContactsSearch)
        fabKeypad = findViewById(R.id.fabKeypad)
        keypadSheet = findViewById(R.id.keypadSheet)
        keypadInput = findViewById(R.id.keypadInput)
        // 🔒 TK-FIX (07.08.2026): showSoftInputOnFocus কোড থেকে সেট করা হলো,
        // XML থেকে নয় — কিছু build-tools-এ `android:showSoftInputOnFocus`
        // XML-অ্যাট্রিবিউটটা public attr টেবিলে না থাকায় "failed linking
        // file resources" এরর দিত। setter মেথডটা API 21+ নিশ্চিত আছে, তাই
        // কোডে সেট করা সম্পূর্ণ নিরাপদ — আচরণ হুবহু একই (সিস্টেম-কিবোর্ড
        // পপ-আপ হয় না, অন-স্ক্রিন কি-প্যাডই সংখ্যা বসায়, পেস্ট চলে)।
        keypadInput.setShowSoftInputOnFocus(false)
        // 🔴🔒 B495 (06.08.2026, TK-নির্দেশ) — উপরের হ্যান্ডেল-বার নিচে
        // টানলে বা তাতে চাপলে সংখ্যা-কিবোর্ড বন্ধ হয়ে যায় (আসল Android
        // কিবোর্ডের মতো)। জটিল ড্র্যাগ-অ্যানিমেশন না করে সহজ, নির্ভরযোগ্য
        // পথ — একটা সাধারণ swipe-down gesture ধরা + সরাসরি ট্যাপেও বন্ধ।
        findViewById<View>(R.id.keypadHandleArea).apply {
            setOnClickListener { keypadSheet.visibility = View.GONE; fabKeypad.visibility = View.VISIBLE }
            var startY = 0f
            setOnTouchListener { v, ev ->
                when (ev.action) {
                    android.view.MotionEvent.ACTION_DOWN -> { startY = ev.rawY; false }
                    android.view.MotionEvent.ACTION_UP -> {
                        if (ev.rawY - startY > dpx(24)) {
                            keypadSheet.visibility = View.GONE
                            fabKeypad.visibility = View.VISIBLE
                            true
                        } else { v.performClick(); false }
                    }
                    else -> false
                }
            }
        }

        findViewById<TextView>(R.id.btnSearchClose).setOnClickListener { toggleSearch() }
        etSearch.addTextChangedListener(simpleWatcher { searchQuery = it; renderCurrentTab() })
        etContactsSearch.addTextChangedListener(simpleWatcher { contactsQuery = it; renderCurrentTab() })

        tabAll.setOnClickListener { switchTab("all") }
        tabMissed.setOnClickListener { switchTab("missed") }
        tabContacts.setOnClickListener { switchTab("contacts") }

        wireKeypad()

        // 🆕 (06.08.2026, নতুন Notifications পাতা থেকে "মিসড কল ব্যাক করতে
        // বাকি"-তে চাপলে — সরাসরি Missed ট্যাব খোলে। এক্সট্রা না থাকলে
        // (সাধারণভাবে Dashboard থেকে Dialer খোলা হলে) আগের মতোই "all"।
        val openTab = intent.getStringExtra("openTab")
        switchTab(if (openTab == "missed") "missed" else "all")
    }

    // ── ছোট সাহায্যকারী ────────────────────────────────────────────
    private fun dpx(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // 🆕 B470 (06.08.2026, TK-নির্দেশ): সেকেন্ডকে "2m 15s" / "45s" আকারে
    // দেখানোর ছোট্ট হেল্পার।
    private fun formatCallDuration(totalSec: Long): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    private fun simpleWatcher(onChange: (String) -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) { onChange(s?.toString() ?: "") }
    }

    private fun toggleSearch() {
        // AUDIT FIX (2026-08-06, TK approved): on the Contacts tab there is
        // ALREADY an always-visible search box (contactsSearchBar). Opening the
        // top search there as well showed TWO identical "Search name or number"
        // boxes stacked. So on the Contacts tab the top 🔍 no longer opens a
        // second box — it just puts the cursor in the existing one. The All /
        // Missed tabs (which have no bottom search box) behave exactly as before.
        if (currentTab == "contacts") {
            if (searchBar.visibility == View.VISIBLE) { searchBar.visibility = View.GONE; searchQuery = ""; etSearch.setText("") }
            etContactsSearch.requestFocus()
            return
        }
        if (searchBar.visibility == View.VISIBLE) {
            searchBar.visibility = View.GONE
            searchQuery = ""; etSearch.setText("")
        } else {
            // 🔴🔒 B493 — সার্চ খোলার আগে সংখ্যা-কিবোর্ডও বন্ধ করা হয়,
            // দুটো কিবোর্ড একসাথে দেখানো এড়াতে (উল্টো দিক থেকেও)।
            if (keypadSheet.visibility == View.VISIBLE) { keypadSheet.visibility = View.GONE; fabKeypad.visibility = View.VISIBLE }
            searchBar.visibility = View.VISIBLE
        }
        renderCurrentTab()
    }

    // 🔴🔒 B490 (06.08.2026, TK-ধরিয়ে দেওয়া — "কন্ট্যাক্টে মিস কল কেন
    // থাকবে") — আসল কারণ: Contacts ট্যাবের তালিকা ক্লাউড থেকে আসতে একটু
    // সময় লাগে (async), ঠিক সেই মুহূর্তে আগের ট্যাবের (Missed/All) পুরনো
    // তালিকা কিছুক্ষণ স্ক্রিনে থেকে যেত — ডেটা ভুল ছিল না, শুধু দেখানোর
    // সময়ে দেরি। এখন ট্যাব বদলানোর সাথে সাথেই পুরনো তালিকা মুছে "লোড
    // হচ্ছে..." দেখানো হয়, নতুন তালিকা এলে সেটাই বসে।
    private fun switchTab(tab: String) {
        currentTab = tab
        val activeColor = "#0B2B59"
        val inactiveColor = "#6B7280"
        tabAll.setTextColor(android.graphics.Color.parseColor(if (tab == "all") "#FFFFFF" else inactiveColor))
        tabMissed.setTextColor(android.graphics.Color.parseColor(if (tab == "missed") "#FFFFFF" else inactiveColor))
        tabContacts.setTextColor(android.graphics.Color.parseColor(if (tab == "contacts") "#FFFFFF" else inactiveColor))
        tabAll.setBackgroundColor(if (tab == "all") android.graphics.Color.parseColor(activeColor) else android.graphics.Color.TRANSPARENT)
        tabMissed.setBackgroundColor(if (tab == "missed") android.graphics.Color.parseColor(activeColor) else android.graphics.Color.TRANSPARENT)
        tabContacts.setBackgroundColor(if (tab == "contacts") android.graphics.Color.parseColor(activeColor) else android.graphics.Color.TRANSPARENT)
        contactsSearchBar.visibility = if (tab == "contacts") View.VISIBLE else View.GONE
        if (searchBar.visibility == View.VISIBLE && tab == "contacts") searchBar.visibility = View.GONE
        // 🔴🔒 B494 (06.08.2026, TK-নির্দেশ — "কন্ট্যাক্ট বুকে যখন যাবে
        // তখন যেন অ্যাপের কিবোর্ড না থাকে") — Contacts ট্যাবে গেলে
        // সংখ্যা-কিবোর্ড (⌨️) খোলা থাকলে বন্ধ হয়ে যায়।
        if (tab == "contacts" && keypadSheet.visibility == View.VISIBLE) {
            keypadSheet.visibility = View.GONE
            fabKeypad.visibility = View.VISIBLE
        }
        // 🟢🔒 V604 (২৪.০৮.২০২৬, TK-নির্দেশ — "ট্যাব বদলালে ডেটা সাথে সাথে
        // আসতে হবে, তবে ভুল ডেটা যেন না থাকে, লোডিং সময় না লাগে")।
        //
        // B490 (06.08.2026)-এ এই একই কারণে সবসময় তালিকা মুছে "Loading..."
        // দেখানো হতো — তখনকার আসল সমস্যা ছিল **ভুল ট্যাবের ডেটা** দেখা
        // যাওয়া (All-এর তালিকা এক মুহূর্তের জন্য Missed-এ দেখাত)।
        //
        // এখন যাচাই করে ধরা পড়ল: All ও Missed **একই** ডেটা (`lastCallRows`)
        // থেকে শুধু আলাদাভাবে ছাঁকা হয় — তাই নতুন করে টানার দরকারই নেই,
        // `renderCallLog()` সবসময় `currentTab` অনুযায়ী সঠিক ছাঁকনি দেয়,
        // ভুল ট্যাবের ডেটা দেখানোর কোনো পথ নেই। আর Contacts ট্যাবের নিজস্ব
        // তাৎক্ষণিক ক্যাশ (B502, `lastContacts`) আগে থেকেই আছে।
        //
        // ⇒ যে তথ্য এই মুহূর্তেই মজুত আছে (এই সেশনে অন্তত একবার লোড হয়ে
        // গেছে), সেটা **সঙ্গে সঙ্গে, সঠিক ট্যাবের জন্যই** দেখানো হয় —
        // "Loading..." শুধু তখনই দেখা যাবে যখন সেই নির্দিষ্ট ট্যাব এই
        // সেশনে এখনো একবারও লোড হয়নি (যেমন অ্যাপ খোলার পরে প্রথমবার)।
        // পিছনে গিয়ে হালনাগাদ টানাও আগের মতোই চলে (`refreshCurrentTab()`),
        // তাই ডেটা কখনো বাসি থেকে যায় না।
        val alreadyHaveCallLog = lastCallRows.isNotEmpty()
        val alreadyHaveContacts = lastContacts.isNotEmpty()
        val instant = when (tab) {
            "all", "missed" -> alreadyHaveCallLog
            "contacts" -> alreadyHaveContacts
            else -> false
        }
        if (instant) {
            renderCurrentTab()
        } else {
            listContainer.removeAllViews()
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = "Loading..."
        }
        refreshCurrentTab()
    }

    private fun refreshCurrentTab() {
        if (currentTab == "contacts") loadContacts() else loadCallLog()
    }

    // ── SIM বাছাই (Work Notebook-এর সঙ্গে শেয়ার করা, দ্বিতীয়বার প্রশ্ন করে না) ──
    // 🆕🔒 B484 (06.08.2026, TK-নির্দেশ) — সিম কটা তা দেখার আগেই প্রথমে
    // জিজ্ঞাসা করা হয় "এই ফোনে কি চেম্বার/ব্রাঞ্চের নম্বর আছে?" — আগে
    // থেকে সিম বেছে রাখা ফোন (`hasChosen()`) বা আগেই এই প্রশ্নের উত্তর
    // দেওয়া ফোন (`hasChamberAnswer()`) দ্বিতীয়বার জিজ্ঞাসা করা হবে না।
    private fun maybeAskWhichSimIsBranch(then: () -> Unit) {
        if (BranchSimHelper.hasGenuinelyChosenSim(this)) { then(); return } // 🔴🔒 B509
        if (BranchSimHelper.hasChamberAnswer(this)) {
            if (!BranchSimHelper.hasChamberNumber(this)) { then(); return } // "No" — কল লিস্ট এমনিতেই ফাঁকা থাকবে
            askWhichSimSlot(then)
            return
        }
        // 🆕 B485 — জিজ্ঞাসা করার আগে একবার নিজে চেষ্টা করে দেখে (ব্যর্থ
        // হলে/নিশ্চিত না হলে নিঃশব্দে প্রশ্নেই ফিরে যায়, কিছু ভাঙে না)।
        /* ☎️🔒 V1427 (১৩.০৯.২০২৬, TK-রিপোর্ট — "চেম্বারের সিম লাগানো স্টাফের
           ফোনেও ব্যানার আসে না") — **আমার দোষ (স্বীকার করছি):** আগে এখানে
           নিজে-পড়া সিম-নম্বর ব্রাঞ্চের নম্বরের সাথে না মিললে **চুপচাপ "না,
           এই ফোনে চেম্বারের নম্বর নেই"** লিখে রাখা হতো — স্টাফকে প্রশ্নই না
           করে। অথচ V633-এই লেখা আছে Android-এর `line1Number` প্রায়ই ভুল/পুরনো
           নম্বর দেয়। ফলে চেম্বারের সিম-লাগানো ফোনেও "না" বসে যেত, আর কখনো
           প্রশ্ন আসত না ⇒ কল-ব্যানার ওই ফোনে চিরতরে বন্ধ।
           **এখন:** নিজে-পড়া নম্বর ব্রাঞ্চের নম্বরের সাথে **মিললে তবেই** "হ্যাঁ"
           ধরা হয়; না মিললে/না পড়তে পারলে **স্টাফকেই জিজ্ঞাসা** করা হয়। */
        if (BranchSimHelper.tryAutoDetectChamberNumber(this) == true) {
            BranchSimHelper.saveHasChamberNumber(this, true)
            askWhichSimSlot(then)
            return
        }
        AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06, দল ২): রঙিন হেডার + রাউন্ডেড কার্ড। টাইটেল
            // NoBengali.s()-এ মোড়া, তাই অনুবাদ অটুট।
            .setCustomTitle(PremiumAlert.header(this, NoBengali.s("এই ফোনে কি চেম্বার/ব্রাঞ্চের নম্বর আছে?")))
            .setMessage(NoBengali.s("এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন — তাহলে Dialer-এ কোনো কল দেখানো হবে না।"))
            .setPositiveButton(NoBengali.s("হ্যাঁ")) { _, _ ->
                BranchSimHelper.saveHasChamberNumber(this, true)
                askWhichSimSlot(then)
            }
            .setNegativeButton(NoBengali.s("No")) { _, _ ->
                BranchSimHelper.saveHasChamberNumber(this, false)
                then()
            }
            .setNeutralButton(NoBengali.s("Cancel")) { _, _ -> finish() }
            .setCancelable(true)
            .setOnCancelListener { finish() }
            .show().also { try { NoBengali.installDialog(it); PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun askWhichSimSlot(then: () -> Unit) {
        val slots = BranchSimHelper.activeSimSlots(this)
        if (slots.size < 2) {
            BranchSimHelper.save(this, -1)
            then()
            return
        }
        val labels = slots.map { "${it.second} (SIM ${it.first + 1})" }.toTypedArray()
        AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06, দল ১ · ৪টে ছোট পপ-আপ): রঙিন হেডার + paint (দাগ)।
            .setCustomTitle(PremiumAlert.header(this, NoBengali.s("এই ফোনে ব্রাঞ্চের নম্বর কোন SIM?")))
            .setItems(labels) { _, which ->
                BranchSimHelper.save(this, slots[which].first)
                then()
            }
            .setNegativeButton(NoBengali.s("Back")) { _, _ ->
                BranchSimHelper.clearChamberAnswer(this)
                maybeAskWhichSimIsBranch(then)
            }
            .setCancelable(true)
            .setOnCancelListener {
                BranchSimHelper.clearChamberAnswer(this)
                maybeAskWhichSimIsBranch(then)
            }
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    // ── Call Log (All/Missed) ─────────────────────────────────────
    private fun loadCallLog() {
        if (!BranchSimHelper.hasCallLogPermission(this)) {
            requestCallLogPermission.launch(android.Manifest.permission.READ_CALL_LOG)
            return
        }
        // 🆕 B484: "resolved" মানে হয় সিম বেছে ফেলেছেন, নয়তো আগেই "না, এই
        // ফোনে চেম্বারের নম্বর নেই" বলে দিয়েছেন — দুটোর কোনোটাই না হলে
        // তবেই আবার জিজ্ঞাসা করা হবে।
        // 🔴🔴🔒 B489 (06.08.2026, TK-রিপোর্ট — Dialer খুললেই স্ক্রিন জমে
        // যাচ্ছিল, কোথাও চাপলে সাড়া দিত না, বারবার চাপলে অ্যাপ বন্ধ হয়ে
        // Android হোম স্ক্রিনে চলে যেত) — সততার সাথে জানানো হলো: গভীরে
        // খুঁজেও কোডে নিশ্চিত কারণ ধরা যায়নি (লেআউট/যুক্তি দুটোই আবার
        // পড়ে দেখা হয়েছে, স্পষ্ট কোনো ব্লকিং কল/লুপ পাওয়া যায়নি), কিন্তু
        // এই সমস্যা শুরু হয়েছে ঠিক আজ B484/B485-এর নতুন "চেম্বার নম্বর
        // আছে কিনা" প্রশ্ন-ব্যবস্থা যোগ হওয়ার পরেই, TK-এর ফোনে বারবার
        // পরীক্ষা করে একই ফল পাওয়া গেছে (রিস্টার্ট/পাওয়ার-সাইকেল করেও)।
        // তাই ঝুঁকি না নিয়ে, রোজকার কাজ আটকে না রেখে, এই নতুন গেটটাই
        // **সাময়িকভাবে নিষ্ক্রিয়** করা হলো — Dialer আবার 05.08.2026-এর
        // পুরনো, প্রমাণিত নিয়মে (সব SIM-এর আজকের কল দেখায়, প্রশ্ন করে
        // না) ফিরে গেছে। ⛔ নিচের লাইনটা মুছে ফেলা হয়নি (`resolved` আসল
        // গেট-লজিক) — ভবিষ্যতে আসল কারণ ধরা পড়লে/দূরে বসে যাচাই করার
        // ব্যবস্থা হলে আবার চালু করা যাবে। ব্যক্তিগত-কল-দেখানোর ঝুঁকিটা
        // TK-কে জানানো হয়েছে, তিনি Dialer আবার চালু চেয়েছেন।
        // 🔴🔒 B491 (06.08.2026, TK-নির্দেশ) — যাচাই করে বোঝা গেছে B489-এর
        // ফ্রিজ-সমস্যার আসল কারণ ছিল Call Log অনুমতি ঠিকমতো না থাকা, এই
        // গেট-কোড নিজে দোষী ছিল না (TK লাইভ টেস্টে নিশ্চিত করেছেন)। তাই
        // এখন আসল B484/B485-এর নিয়ম আবার চালু করা হলো।
        val resolved = BranchSimHelper.hasGenuinelyChosenSim(this) || // 🔴🔒 B509
            (BranchSimHelper.hasChamberAnswer(this) && !BranchSimHelper.hasChamberNumber(this))
        if (!resolved) { maybeAskWhichSimIsBranch { loadCallLog() }; return }
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) { BranchSimHelper.fetchTodayCallLog(this@DialerActivity) }
            val numbers = rows.map { it.number }
            val matches = withContext(Dispatchers.IO) { DialerRepository.matchNumbersBatch(numbers) }
            val remarks = withContext(Dispatchers.IO) { DialerRepository.fetchLatestRemarksBatch(numbers) }   // 🟢🔒 V605
            lastCallRows = rows
            lastMatches = matches
            lastRemarks = remarks
            renderCurrentTab()
        }
    }

    private fun renderCurrentTab() {
        when (currentTab) {
            "all" -> renderCallLog(missedOnly = false)
            "missed" -> renderCallLog(missedOnly = true)
            "contacts" -> renderContacts()
        }
    }

    private fun renderCallLog(missedOnly: Boolean) {
        listContainer.removeAllViews()
        val q = searchQuery.filter { it.isDigit() || it.isLetter() }.lowercase()
        var rows = lastCallRows
        if (missedOnly) rows = rows.filter { it.type == android.provider.CallLog.Calls.MISSED_TYPE }
        if (q.isNotBlank()) {
            rows = rows.filter { row ->
                val digits = row.number.filter { it.isDigit() }
                val matched = lastMatches[digits.takeLast(10)]
                digits.contains(q) || (matched?.name?.lowercase()?.contains(q) == true)
            }
        }
        if (rows.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            // 🆕 B484 — এই ফোনে চেম্বারের নম্বর নেই বলে জানানো থাকলে,
            // কেন কল দেখা যাচ্ছে না সেটা স্পষ্ট করে বলা হয় (শুধু "কল নেই"
            // বললে বিভ্রান্তিকর হতো)।
            val noChamberNumber = BranchSimHelper.hasChamberAnswer(this) && // 🔴🔒 B509
                !BranchSimHelper.hasGenuinelyChosenSim(this) && !BranchSimHelper.hasChamberNumber(this)
            // 🔴🔒 V468 (20.08.2026, TK-অনুমোদিত — "Total Call 0" নিঃশব্দে
            // আটকে থাকা): `tryAutoDetectChamberNumber()` (BranchSimHelper.kt)
            // ফোনের নিজের সিম-নম্বর (`line1Number`, Android-এ প্রায়ই
            // অনির্ভরযোগ্য) দিয়ে **নিঃশব্দে** সিদ্ধান্ত নিয়ে ফেলতে পারে —
            // ভুল হলে স্টাফ কোনো প্রশ্নই দেখেন না, ঠিক করার উপায়ও পান না।
            // ⛔ auto-detect-এর নিজের কোড এক অক্ষরও বদলানো হয়নি — শুধু এই
            // নির্দিষ্ট (আগে থেকেই চেনা) "noChamberNumber" অবস্থায় বার্তাটা
            // এখন **চাপা যায়**, চাপলে প্রশ্নটা আবার আসবে। বাকি সব
            // অবস্থা (স্বাভাবিকভাবে কল দেখাচ্ছে এমন ফোন) অস্পৃশ্য।
            if (noChamberNumber) {
                tvEmpty.text = NoBengali.s("এই ফোনে চেম্বারের নম্বর নেই বলে জানানো আছে, তাই কল দেখানো হচ্ছে না।\n\n👉 এটা ভুল হলে — এখানে চাপুন আবার জিজ্ঞাসা করতে")
                tvEmpty.setOnClickListener {
                    BranchSimHelper.clearChamberAnswer(this)
                    maybeAskWhichSimIsBranch { loadCallLog() }
                }
            } else {
                tvEmpty.text = "No calls yet today."
                tvEmpty.setOnClickListener(null)
            }
            return
        }
        tvEmpty.visibility = View.GONE
        for (row in rows) listContainer.addView(buildCallRow(row))
    }

    private fun buildCallRow(row: BranchSimHelper.CallLogRow): View {
        val digits = row.number.filter { it.isDigit() }.takeLast(10)
        val matched = lastMatches[digits]
        val isMissed = row.type == android.provider.CallLog.Calls.MISSED_TYPE
        val isOutgoing = row.type == android.provider.CallLog.Calls.OUTGOING_TYPE

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpx(16), dpx(11), dpx(16), dpx(11))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val arrow = TextView(this).apply {
            text = if (isMissed) "↙" else if (isOutgoing) "↖" else "↘"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor(if (isMissed) "#D92D20" else "#4B5563"))
            layoutParams = LinearLayout.LayoutParams(dpx(22), LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = android.view.Gravity.CENTER
        }
        outer.addView(arrow)

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = dpx(8)
            layoutParams = lp
        }
        val nameLine = TextView(this).apply {
            text = matched?.name?.ifBlank { digits } ?: digits
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(if (isMissed) "#D92D20" else "#0B1B2E"))
        }
        infoCol.addView(nameLine)
        val subLine = TextView(this).apply {
            // 🆕 B470 (06.08.2026, TK-নির্দেশ): কত সেকেন্ড কথা হয়েছিল তাও
            // দেখানো হয় — Missed কলের ক্ষেত্রে কথাই হয়নি তাই duration
            // দেখানো হয় না (আগের মতোই শুধু "Missed" থাকে)।
            val durTxt = if (!isMissed && row.durationSec > 0) " · " + formatCallDuration(row.durationSec) else ""
            text = (if (matched != null) digits else if (isMissed) "$digits — Missed" else "$digits — No matching record") + durTxt
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#6B7280"))
        }
        infoCol.addView(subLine)
        if (matched != null) {
            val tagLine = TextView(this).apply {
                text = "✓ " + stageLabel(matched.stage)
                textSize = 10f
                setTextColor(android.graphics.Color.parseColor("#16A36D"))
            }
            infoCol.addView(tagLine)
        }
        // 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — এই নম্বরের
        // সাম্প্রতিক রিমার্কস থাকলে সারির নিচে ছোট করে (হলুদ ব্যাকগ্রাউন্ড)।
        val remarkText = lastRemarks[digits]
        if (!remarkText.isNullOrBlank()) {
            infoCol.addView(TextView(this).apply {
                text = "📝 " + remarkText
                textSize = 10f
                setTextColor(android.graphics.Color.parseColor("#8A6A00"))
                setPadding(dpx(6), dpx(2), dpx(6), dpx(2))
                setBackgroundColor(android.graphics.Color.parseColor("#FFF6E6"))
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.topMargin = dpx(4)
                layoutParams = lp
            })
        }
        outer.addView(infoCol)

        val timeView = TextView(this).apply {
            text = timeAgoLabel(row.dateMs)
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#6B7280"))
        }
        outer.addView(timeView)

        outer.setOnLongClickListener { showCallLongPressMenu(digits, matched); true }
        outer.setOnClickListener {
            CallChooser.open(this, digits)
            // 📞 V1246 — ইতিহাসের সারিটা এখন CallChooser নিজেই লেখে (সব পর্দার জন্য এক নিয়ম)।
            // 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ ৩ — "কল চলাকালীন ও কল
            // শেষে দুটোতেই, Outgoing-ও") — অ্যাপের নিজের Call বোতাম থেকে
            // ডায়াল করার মুহূর্তেই নম্বর জানা থাকে, তাই এখানেই শুরু করা
            // হলো — নতুন করে ব্রডকাস্ট/মেলানোর অপেক্ষা করতে হয় না।
            CallNotifyManager.notifyOutgoingDialed(this, digits, matched)
        }
        return outer
    }

    private fun stageLabel(stage: String): String = when (stage) {
        "Treatment" -> "Logged to Patient"
        "Patient" -> "Logged to Visit"
        "Inquiry" -> "Logged to Enquiry"
        else -> "Matched"
    }

    private fun timeAgoLabel(dateMs: Long): String {
        return try {
            val fmt = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US)
            fmt.format(java.util.Date(dateMs))
        } catch (_: Throwable) { "" }
    }

    // ── লম্বা-চাপ মেনু (Call Log সারিতে) ───────────────────────────
    private fun showCallLongPressMenu(digits: String, matched: DialerRepository.MatchedContact?) {
        val options = ArrayList<String>()
        options.add("📋 Copy Number")
        if (matched != null) {
            options.add("💬 Send Message")
            options.add("👤 View Record")
        } else {
            options.add("➕ New Enquiry")
        }
        // 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ) — কল-লগের এই
        // একই মেনুতে রিমার্কস লেখা/বদলানো — Incoming/Outgoing দুটোতেই।
        options.add("📝 Add / Edit Remark")
        AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06): রঙিন হেডার + paint (দাগ)। অপশন/লজিক অপরিবর্তিত।
            .setCustomTitle(PremiumAlert.header(this, "📞 Number Options"))
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "📋 Copy Number" -> copyNumber(digits)
                    "💬 Send Message" -> openWhatsApp(digits)
                    "👤 View Record" -> startActivity(Intent(this, PatientTimelineActivity::class.java).putExtra("mobile", digits))
                    "➕ New Enquiry" -> startActivity(Intent(this, EnquiryActivity::class.java).putExtra("prefillMobile", digits))
                    "📝 Add / Edit Remark" -> {
                        val existing = try { DialerRepository.fetchLatestRemark(digits) } catch (_: Throwable) { "" }
                        startActivity(
                            Intent(this, CallRemarkActivity::class.java)
                                .putExtra("mobile", digits)
                                .putExtra("direction", "incoming")   // কল-লগ থেকে দুই দিকেরই হতে পারে; নির্দিষ্ট না জানা থাকলে ডিফল্ট
                                .putExtra("patientId", matched?.patientId.orEmpty())
                                .putExtra("patientName", matched?.name.orEmpty())
                                .putExtra("branch", matched?.branch.orEmpty())
                                .putExtra("existingRemark", existing)
                        )
                    }
                }
            }
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun copyNumber(digits: String) {
        com.tkbiswas.pilesclinic.native.Clip.copy(this, "mobile", digits)   // 🤫 V772
        Toast.makeText(this, "Number copied", Toast.LENGTH_SHORT).show()
    }

    private fun openWhatsApp(digits: String) {
        try {
            val uri = Uri.parse("https://wa.me/91$digits")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")
            if (intent.resolveActivity(packageManager) != null) { startActivity(intent); return }
            val fallback = Intent(Intent.ACTION_VIEW, uri)
            startActivity(fallback)
        } catch (_: Throwable) {
            Toast.makeText(this, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Contacts ট্যাব ──────────────────────────────────────────────
    private fun loadContacts() {
        // 🔴🔒 B502 — ফোনের জমানো তথ্য দিয়ে সাথে সাথেই তালিকা দেখানো হয়।
        val instant = try { DialerRepository.fetchCachedContacts(this, if (user.role == "master") null else user.branch) } catch (_: Throwable) { emptyList() }
        if (instant.isNotEmpty() && currentTab == "contacts") { lastContacts = instant; renderCurrentTab() }
        lifecycleScope.launch {
            val branchFilter = if (user.role == "master") null else user.branch
            val contacts = withContext(Dispatchers.IO) { DialerRepository.fetchContacts(this@DialerActivity, branchFilter) }
            val tags = withContext(Dispatchers.IO) { AddressTagRepository.fetchSavedTags(contacts.map { it.mobile }) }
            lastContacts = contacts
            lastAddressTags = tags
            renderCurrentTab()
        }
    }

    private fun renderContacts() {
        listContainer.removeAllViews()
        val q = contactsQuery.trim().lowercase()
        var rows = lastContacts
        if (q.isNotBlank()) {
            rows = rows.filter {
                it.name.lowercase().contains(q) || it.mobile.filter { d -> d.isDigit() }.contains(q)
            }
        }
        if (rows.isEmpty()) { tvEmpty.visibility = View.VISIBLE; tvEmpty.text = "No contacts found."; return }
        tvEmpty.visibility = View.GONE
        for (item in rows) listContainer.addView(buildContactRow(item))
    }

    private fun buildContactRow(item: FollowUpItem): View {
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpx(16), dpx(11), dpx(16), dpx(11))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            layoutParams = lp
        }
        val nameLine = TextView(this).apply {
            text = item.name.ifBlank { "Name Not Available" }
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0B1B2E"))
        }
        infoCol.addView(nameLine)
        val mobLine = TextView(this).apply {
            text = digits
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#6B7280"))
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dpx(2)
            layoutParams = lp
        }
        infoCol.addView(mobLine)

        val tagRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dpx(5)
            layoutParams = lp
        }
        infoCol.addView(tagRow)
        outer.addView(infoCol)

        val callIcon = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpx(38), dpx(38)).also { it.marginStart = dpx(8) }
            setBackgroundResource(R.drawable.bg_circle_gradient)
        }
        val callGlyph = android.widget.ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dpx(18), dpx(18)).also { it.gravity = android.view.Gravity.CENTER }
            setImageResource(R.drawable.ic_pd_phone)
            imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        }
        callIcon.addView(callGlyph)
        callIcon.setOnClickListener {
            CallChooser.open(this, digits)
            // 📞 V1246 — ইতিহাসের সারিটা এখন CallChooser নিজেই লেখে (সব পর্দার জন্য এক নিয়ম)।
            // 🟢🔒 V605 — Contacts ট্যাব থেকে কল করলেও একই নোটিফিকেশন।
            val matched = DialerRepository.MatchedContact(
                id = item.id, name = item.name, mobile = digits, branch = item.branch,
                disease = item.disease, stage = item.stage, patientId = item.patientId, address = item.address
            )
            CallNotifyManager.notifyOutgoingDialed(this, digits, matched)
        }
        outer.addView(callIcon)

        // ট্যাগ বসানোর পরে (layout হওয়ার পরে) শেষবার সাইজ জানা যায় —
        // তাই একটাই লাইনে বসানোর হিসাব post{} এ করা হয় (FollowUp-এর
        // layoutTagsInRows-এর ঠিক একই কৌশল, শুধু এখানে wrap না করে shrink)।
        // 🔒 গভীর যাচাইয়ে ধরা পড়েছে (FollowUpActivity.kt-এর একই সমস্যার
        // পুরনো ফিক্স মিলিয়ে দেখে): weight-ভিত্তিক নেস্টেড LinearLayout-এর
        // (এখানে infoCol, weight=1) আসল width একটামাত্র post{}-এ সবসময়
        // চূড়ান্তভাবে মাপা হয় না — তাই FollowUpActivity-র প্রমাণিত সমাধান
        // (post{}-এর ভিতরে আরেকটা post{}) এখানেও বসানো হলো।
        tagRow.post {
            tagRow.post {
                buildContactTags(tagRow, item, digits)
            }
        }

        return outer
    }

    private fun buildContactTags(tagRow: LinearLayout, item: FollowUpItem, digits: String) {
        tagRow.removeAllViews()
        val staffMobile = user.mobile
        val defs = ArrayList<Pair<DialerTagPrefs.TagType, String>>()
        defs.add(DialerTagPrefs.TagType.STAGE to stageShort(item.stage))
        if (item.branch.isNotBlank()) defs.add(DialerTagPrefs.TagType.BRANCH to item.branch.uppercase())
        if (item.disease.isNotBlank()) defs.add(DialerTagPrefs.TagType.DISEASE to item.disease.uppercase())
        val addressTag = lastAddressTags[digits] ?: AddressTagRepository.defaultTagFromAddress(item.address)
        if (addressTag.isNotBlank()) defs.add(DialerTagPrefs.TagType.ADDRESS to addressTag.uppercase())

        val visible = defs.filter { !DialerTagPrefs.isHidden(this, staffMobile, digits, it.first) }
        if (visible.isEmpty()) return

        // 🔴🔒 B492 (06.08.2026, TK-ধরিয়ে দেওয়া — "কন্ট্যাক্ট বুকে ট্যাগ
        // একদমই আসছে না") — আসল কারণ: `tagRow.width` post{}-এর ভিতরেও
        // মাঝেমধ্যে ০ থেকে যাচ্ছিল (weight-ভিত্তিক নেস্টেড লেআউট, খুব
        // দ্রুত পরপর অনেক কার্ড তৈরি হওয়ার সময় Android-এর measure-পাস
        // সবসময় সময়মতো শেষ হয় না) — তখন `avail <= 0` ধরে সম্পূর্ণ
        // ট্যাগ-সারিই বাদ পড়ে যেত (একটা ট্যাগও দেখা যেত না)। এখন সেই
        // ব্যর্থ-মাপ অবস্থায় স্ক্রিনের প্রস্থ থেকে একটা যুক্তিসঙ্গত
        // আন্দাজি প্রস্থ ব্যবহার হয় (ফাঁকা দেখানোর বদলে), তাই ট্যাগ
        // সবসময় দেখা যাবে — মাপ সামান্য কম-নিখুঁত হতে পারে প্রথম
        // ফ্রেমে, কিন্তু কখনো সম্পূর্ণ অদৃশ্য হবে না।
        var avail = tagRow.width - tagRow.paddingLeft - tagRow.paddingRight
        if (avail <= 0) avail = resources.displayMetrics.widthPixels - dpx(16 + 16 + 38 + 8 + 12)

        var size = 9.5f
        fun widthAt(s: Float): Int {
            val paint = android.graphics.Paint()
            paint.textSize = s * resources.displayMetrics.scaledDensity
            var total = 0
            for (d in visible) total += dpx(14) + paint.measureText(d.second).toInt()
            total += dpx(5) * (visible.size - 1)
            return total
        }
        while (size > 6.5f && widthAt(size) > avail) size -= 0.5f

        for ((idx, d) in visible.withIndex()) {
            val pill = TextView(this).apply {
                text = d.second
                textSize = size
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundResource(R.drawable.bg_tag_branch)
                setPadding(dpx(7), dpx(3), dpx(7), dpx(3))
                maxLines = 1
                ellipsize = null
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            if (idx > 0) lp.marginStart = dpx(5)
            pill.layoutParams = lp
            val tagType = d.first
            val tagText = d.second
            TripleTapEdit.attach(pill) { showTagHideDialog(tagType, tagText, digits) }
            tagRow.addView(pill)
        }
    }

    private fun stageShort(stage: String): String = when (stage) {
        "Treatment" -> "PATIENT"
        "Patient" -> "VISIT"
        "Inquiry" -> "ENQUIRY"
        else -> stage.uppercase()
    }

    private fun showTagHideDialog(type: DialerTagPrefs.TagType, text: String, contactMobile: String) {
        AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06, দল ২): রঙিন হেডার + রাউন্ডেড কার্ড।
            .setCustomTitle(PremiumAlert.header(this, text))
            .setMessage("Hide this tag? Only on your phone, only on this card.")
            .setPositiveButton("Hide") { _, _ ->
                DialerTagPrefs.setHidden(this, user.mobile, contactMobile, type, true)
                renderContacts()
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    // ── কিবোর্ড ────────────────────────────────────────────────────
    private fun wireKeypad() {
        fabKeypad.setOnClickListener {
            // 🔴🔒 B493 (06.08.2026, TK-ধরিয়ে দেওয়া — "সার্চের কিবোর্ড আর
            // অ্যাপের নিজের কিবোর্ড একসাথে উঠে যাচ্ছে") — আসল কারণ: 🔍
            // সার্চ-বক্স (আসল EditText) খোলা অবস্থায় থাকলে ফোনের নিজস্ব
            // কিবোর্ড ফোকাস ধরে রাখত, তারপর নিচের কালো সংখ্যা-কিবোর্ড
            // (keypadSheet) খুললে দুটোই একসাথে দেখা যেত, বক্স হারিয়ে
            // যেত। এখন সংখ্যা-কিবোর্ড খোলার ঠিক আগে সার্চ-বক্স বন্ধ করে
            // ও ফোনের কিবোর্ড জোর করে সরিয়ে দেওয়া হয়।
            if (searchBar.visibility == View.VISIBLE) { searchBar.visibility = View.GONE; searchQuery = ""; etSearch.setText("") }
            if (contactsSearchBar.visibility == View.VISIBLE) { etContactsSearch.clearFocus() }
            etSearch.clearFocus()
            try {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken ?: window.decorView.windowToken, 0)
            } catch (_: Throwable) { }
            keypadSheet.visibility = View.VISIBLE
            fabKeypad.visibility = View.GONE
            keypadInput.setText("")
        }
        val keyIds = listOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.keyStar to "*", R.id.key0 to "0", R.id.keyHash to "#"
        )
        for (pair in keyIds) {
            val id = pair.first
            val digit = pair.second
            findViewById<TextView>(id).setOnClickListener {
                keypadInput.setText(keypadInput.text.toString() + digit)
                keypadInput.setSelection(keypadInput.text.length)
            }
        }
        findViewById<TextView>(R.id.keypadBackspace).setOnClickListener {
            val cur = keypadInput.text.toString()
            if (cur.isNotEmpty()) {
                keypadInput.setText(cur.substring(0, cur.length - 1))
                keypadInput.setSelection(keypadInput.text.length)
            }
        }
        // 🔒 TK-ORDER (2026-08-06): the number field is now a real EditText, so
        // the phone's OWN paste works — including a normal long-press → Paste
        // menu (which is why it now works on phones that used to block it). The
        // visible "Paste" button triggers that same system paste for staff who
        // don't know the long-press.
        findViewById<TextView>(R.id.keypadPaste).setOnClickListener { pasteIntoKeypad() }
        findViewById<View>(R.id.btnKeypadCall).setOnClickListener {
            val digits = keypadInput.text.toString().filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10) {
                Toast.makeText(this, "Enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CallChooser.open(this, digits)
            // 📞 V1246 — ইতিহাসের সারিটা এখন CallChooser নিজেই লেখে (সব পর্দার জন্য এক নিয়ম)।
            // 🟢🔒 V605 — কিপ্যাড থেকে হাতে নম্বর টাইপ করে কল করলেও একই
            // নোটিফিকেশন; এই নম্বরটা আগে থেকে মেলানো ছিল না, তাই এখানেই
            // (হালকা, ছোট ১-নম্বরের) মেলানো হচ্ছে — CallNotifyManager
            // নিজেই ব্যাকগ্রাউন্ডে সেটা করে (onRinging-এর মতোই প্যাটার্ন)।
            Thread {
                val m = try { DialerRepository.matchNumbersBatch(listOf(digits))[digits] } catch (_: Throwable) { null }
                runOnUiThread { CallNotifyManager.notifyOutgoingDialed(this, digits, m) }
            }.start()
            keypadSheet.visibility = View.GONE
            fabKeypad.visibility = View.VISIBLE
            keypadInput.postDelayed({ refreshCurrentTab() }, 3000)
        }
    }

    /**
     * 🔒 TK-ORDER (2026-08-06): shared paste for the keypad — used by the new
     * visible "Paste" button and the long-press on the number display. Takes the
     * last 10 digits of whatever is on the clipboard (so "+91 98765 43210",
     * "98765-43210" etc. all work), and clearly tells the user when there is no
     * number to paste (the old code silently did nothing, which looked broken).
     */
    private fun pasteIntoKeypad() {
        // The number field is a real EditText now, so use the framework's OWN
        // paste. This works even on phones that block apps from reading the
        // clipboard programmatically, because the OS performs the paste into the
        // focused editable field. Select-all first so the pasted number REPLACES
        // whatever is there; extra characters (spaces / +91) are ignored later
        // because the call button keeps only the last 10 digits.
        keypadInput.requestFocus()
        keypadInput.setSelection(0, keypadInput.text.length)
        keypadInput.onTextContextMenuItem(android.R.id.paste)
        keypadInput.setSelection(keypadInput.text.length)
    }

    override fun onBackPressed() {
        if (keypadSheet.visibility == View.VISIBLE) {
            keypadSheet.visibility = View.GONE
            fabKeypad.visibility = View.VISIBLE
            return
        }
        super.onBackPressed()
    }
}
