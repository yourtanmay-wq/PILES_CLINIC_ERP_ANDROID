package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.ui.PaymentRingView

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.databinding.ActivityFollowupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Native rebuild Step 4 -- Follow-up.
 *
 * Same three tabs as the WebView (Enquiry / Visit / Patient, internally
 * stage=Inquiry/Patient/Treatment -- same naming as app.js, kept identical
 * rather than renamed without being asked), same Today-Due/Overdue/X-Days
 * badge logic on the Enquiry tab, and the two most common actions: Call,
 * WhatsApp, updating the Last Remark, and setting the Next Follow-up Date.
 *
 * SCOPED LIMITATIONS for this step (disclosed clearly, same pattern as
 * every step so far):
 * - "View All" (the full patient timeline modal built earlier in the
 *   WebView redesign) isn't in this native screen yet -- Remark editing
 *   covers the most common day-to-day action.
 * - The Patient tab's payment-percentage circle isn't shown here yet --
 *   Payment itself is a separate not-yet-rebuilt module; this tab still
 *   correctly lists Patient-stage records with all their other info.
  * - Triple-tap a card opens the Continue / Cancel Entry menu (native port).
 * - Branch-based visibility (staff sees only their own branch) IS applied
 *   here (fetchTab() filters by effectiveBranch()), matching the WebView's
 *   core rule. Master defaults to seeing all branches, but can narrow the
 *   list (and the tab-count numbers) to one branch via the top branch
 *   picker -- both the counts and the list below always stay in sync
 *   (TK-REPORTED FIX, 2026-07-23). The one WebView edge case NOT yet
 *   replicated: a staff member keeps seeing a record they personally
 *   created even when viewing a different branch context ("creator
 *   override") -- that specific edge case isn't handled here yet.
 */
class FollowUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowupBinding
    private lateinit var repository: FollowUpRepository
    private lateinit var user: NativeUser
    private var currentStage = "Inquiry"

    /** 🔒 খাতার সারি B90: শুধু ড্যাশবোর্ডের "calls pending today" বোতাম থেকে
     *  খুললে `true` — তখন তিন সেকশনের আজকের সবাই এক তালিকায় দেখায়।
     *  ⛔ যে কোনো ট্যাবে চাপ দিলেই `false` হয়ে আগের আচরণ ফিরে আসে। */
    private var todayAllSections = false

    /* ═══════════════════════════════════════════════════════════════════════
       🟣🔒🔁 V710 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত)

       TK: *"staff রা বিভ্রান্ত হয়ে যাচ্ছে — Enquiry এর মধ্যে patient কেন
       দেখাচ্ছে"* → *"ওখান থেকে আসলে Enquiry Visit Patient, তা ছাড়া নিচের
       ফিল্টারগুলিও যদি না রাখা হয় তাহলে ভালো হয়"*।

       **কারণ:** ড্যাশবোর্ডের "N calls pending" ব্যানার থেকে এলে অ্যাপ
       ইচ্ছে করেই **তিন সেকশন একসাথে** দেখায় (TK-এর ২৯.০৭.২০২৬-এর নির্দেশ,
       খাতার সারি B90)। কিন্তু উপরে "Enquiry" ট্যাবটা সবুজ হয়েই থাকত, তাই
       স্টাফ ভাবতেন এটা শুধু Enquiry-র তালিকা।

       ⇒ ওই মোডে ট্যাবের সারি ও ফিল্টারের সারি **লুকানো** হয়, জায়গায় একটাই
         পরিষ্কার বেগুনি লাইন।

       🔁🔁 **TK পরে পছন্দ না করলে এক লাইনেই ফেরানো যাবে:**
            নিচের `HIDE_TABS_IN_CALL_LIST`-কে `false` করে দিলেই V709-এর
            হুবহু আগের চেহারা ফিরে আসে (ট্যাব ও ফিল্টার আবার দেখা যাবে)।
            আর কোথাও কিছু বদলাতে হবে না — বাকি সব কোড এই একটা মান দেখেই চলে।
       ⛔ তালিকার তথ্য · কার্ড · Remark · বোতাম · সেভ — কিছুই এতে বদলায় না।
       ═══════════════════════════════════════════════════════════════════════ */
    private val HIDE_TABS_IN_CALL_LIST = true

    /**
     * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK লাইভ টেস্টে ধরেছেন — ব্যানারে ৭, ভিতরে ৮)।
     *
     * TK-এর কথা (হুবহু): *"today pending call-এ যা দেখাবে, সেখানে ক্লিক করলে
     * তো আজকে যাদের কল করতে হবে তাদের নম্বরই দেখানো উচিত।"*
     *
     * ─── কেন সংখ্যা দুরকম হচ্ছিল (কোড ধরে মিলিয়ে দেখা, আন্দাজ নয়) ──────────
     *  · ব্যানার গোনে **শুধু** `nextFollow == আজ` — অর্থাৎ আজ কল করার কথা
     *    (`DashboardActivity.refreshCallBanner()`)।
     *  · কিন্তু ভিতরের "Today" ছাঁকনি TK-এরই **০৫.০৮.২০২৬-এর নির্দেশে**
     *    (খাতার সারি B454) দুটো জিনিস দেখায় — `nextFollow == আজ` **অথবা**
     *    `recordDate == আজ` (আজ যাঁর Visit/Registration হয়েছে, কল ডিউ না
     *    থাকলেও)। তাই ভিতরে বেশি দেখাত।
     *
     * ─── সমাধান — দুটো নিয়মই বাঁচে (TK-অনুমোদিত, ২১.০৮.২০২৬) ─────────────
     *  · **ব্যানারে চাপ দিয়ে এলে** (`todayOnly`) → শুধু আজ কল করার লোক।
     *    ব্যানারে যা লেখা, ভিতরেও ঠিক তাই — একজন বেশিও না, কমও না।
     *  · **মেনু → Follow-up → Today** খুললে → আগের মতোই কল-ডিউ + আজকের নতুন
     *    Visit/Registration। ০৫.০৮-এর নিয়ম **এক অক্ষরও ভাঙে না**।
     *
     * ⛔ এটা শুধু "কাকে দেখাব" — গোনার নিয়ম · ব্রাঞ্চের ছাঁকনি · টাকার হিসাব ·
     *    কল-বোতাম কিছুই ছোঁয়া হয়নি। তাই তালিকার প্রত্যেককে আগের মতোই সরাসরি
     *    কল করা যায়।
     * ⛔ উপরের ট্যাবের সংখ্যাগুলোও (`paintTabCounts`) এই একই ছাঁকনি দিয়েই বসে,
     *    তাই ট্যাবের যোগফল আর ব্যানারের সংখ্যা কখনো আলাদা হতে পারে না।
     */
    private var bannerCallsOnly = false
    /* 🟢🔒 V692 (২৬.০৮.২০২৬, TK-নির্দেশ) — Briefing-এর ⚠️ Overdue Follow-up
       Alert-এর "View" থেকে এলে তালিকায় **শুধু ৩+ দিন দেরি হওয়া** কলগুলোই।
       TK-এর বাছা: নোটিশে যত জন লেখা, ঠিক তত জন — ব্রাঞ্চের সব Overdue নয়।
       ⛔ `bannerCallsOnly`-র হুবহু একই ধাঁচ: উপরের কোনো চিপে নিজে চাপ দিলেই
          এই মোড বন্ধ, স্বাভাবিক Overdue ফিরে আসে। */
    private var overdue3PlusOnly = false
    // TK-REPORTED BUG FIX (2026-07-25, same root cause found and fixed in
    // ChamberAttendanceActivity today -- editing something briefly showed
    // correctly then silently reverted): the existing "stage != currentStage"
    // guard below only protects against a DIFFERENT tab's stale result
    // landing here -- it does NOT protect against two overlapping fetches
    // for the SAME tab (e.g. an edit-triggered reload racing the 25s
    // silent background refresh), where an older, slower response can
    // still land after a newer one and overwrite it with stale data. This
    // token makes sure only the MOST RECENT loadTab() call for a stage is
    // ever allowed to update the screen.
    private var tabLoadToken = 0
    // TK-REQUESTED ADDITION (2026-07-23): Master-only branch filter for the
    // tab count numbers (130/40/15 style). "All" = every branch. Does NOT
    // affect the actual tab list/data below -- that keeps using its own
    // existing user.branch visibility rule, completely unchanged.
    private var countBranch = "All"
    // TK APPROVED (2026-07-15): auto-refresh every 25s while this screen is on
    // screen, so a same-branch staff/Doctor/Master sees a new entry almost
    // immediately without needing to back-out and reopen the tab. Runs ONLY
    // while visible (started in onResume, stopped in onPause) -- no battery/
    // data use while the app is in the background.
    /**
     * 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B40):
     * *"বারবার নিজে থেকে বাফারিং-এর সমস্যা।"*
     *
     * **আসল কারণ:** অ্যাপ **প্রতি ২৫ সেকেন্ডে** নিজে থেকে পুরো তালিকা আবার
     * নামাত (followups + patients + payments)। TK-এর লাইনে (০.১৬–২.০০ KB/s)
     * **এক দফা শেষ হতেই ২৫ সেকেন্ডের অনেক বেশি লাগে** — তাই একটার উপর আরেকটা
     * চেপে বসত, আর অ্যাপ কার্যত **সারাক্ষণই লোড করতে থাকত**। ঠিক এটাই "বারবার
     * বাফারিং"।
     *
     * **এখন দুটো নিয়ম:**
     *  ১. সময় **২৫ সেকেন্ড → ৩ মিনিট**।
     *  ২. **আগের দফা শেষ না হলে নতুন দফা শুরুই হবে না** — তখন শুধু পরেরবারের
     *     জন্য সময় ধরা হয়।
     * ⛔ টেনে-নামিয়ে রিফ্রেশ ও ট্যাব বদল আগের মতোই সঙ্গে সঙ্গে কাজ করে —
     * এটা শুধু **নিজে থেকে** হওয়া রিফ্রেশের নিয়ম।
     */
    @Volatile private var loadInFlight = false

    private val autoRefreshHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * 🔔🔒 খাতার সারি B151 (TK, 30.07.2026): *"অফিশিয়াল টাইমে রিফ্রেস না টানলেও
     * যেন ভিজিট সেকশন · পেশেন্ট সেকশন — এ সমস্ত জায়গায় কার্যকরী হয়।"*
     *
     * **আগে যা হত:** প্রতি ৩ মিনিটে **পুরো তালিকা** আবার নামত — কিছু না বদলালেও।
     * তাই নতুন এন্ট্রি দেখতে ৩ মিনিট পর্যন্ত অপেক্ষা, আর ফ্রি প্ল্যানে অকারণ খরচ।
     *
     * **এখন:** প্রতি **৩০ সেকেন্ডে** শুধু একটা **ছোট প্রশ্ন** যায় — *"গতবারের পরে
     * কিছু বদলেছে?"* (⛔ একটাও সারি নামে না)। **বদলালে তবেই** পুরো তালিকা নামে।
     * 👉 তাই দেখা যায় **৬ গুণ তাড়াতাড়ি**, অথচ **খরচ কমে** — কারণ আগে কিছু না
     *    বদলালেও তালিকা নামত, এখন নামে না।
     * ⛔ নিয়মটা `LiveRefresh`-এ এক জায়গায় (চেম্বার · ডাক্তার কিউ-ও একই নিয়মে)।
     * ⛔ রাত ১০টা – সকাল ৬টা কোনো প্রশ্নই যায় না (TK-এর নিয়ম)।
     * ⛔ আগের দফা শেষ না হলে নতুন দফা শুরু হয় না (খাতার সারি B40-এর সুরক্ষা অক্ষত)।
     * ⛔ কোনো পপ-আপ/বাক্স খোলা থাকলে কিছুই করা হয় না — স্টাফের লেখা হারাতে পারে না।
     * ⛔ টেনে-নামিয়ে রিফ্রেশ · ট্যাব বদল · ছাঁকনি — সব আগের মতোই।
     */
    private var autoScreenFocused = true
    @Volatile private var autoWatchBusy = false
    // 🔒 খাতার সারি B171 (TK-এর ৮ নম্বর সন্দেহ): `FollowUpRepository.fetchTab()`
    // সবসময় `followups` পড়ে, আর ট্যাব অনুযায়ী `enquiries` (Inquiry) বা
    // `patients`·`payments` (Patient/Treatment)-ও পড়ে (মিলিয়ে দেখা হয়েছে)।
    // আগে শুধু `followups` দেখা হত, তাই ওই টেবিলগুলোয় বদল হলে (যেমন অন্য
    // ফোনে টাকা জমা) পর্দা জানতই না।
    private val autoWatch = LiveRefresh.Watch("followups", "enquiries", "patients", "payments")
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            try { autoCheckForChanges() } catch (_: Throwable) { }
            autoRefreshHandler.postDelayed(this, LiveRefresh.TICK_MS)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        autoScreenFocused = hasFocus
    }

    private fun autoCheckForChanges() {
        if (loadInFlight || autoWatchBusy) return
        if (!autoScreenFocused) return
        if (!LiveRefresh.awake()) return
        if (BranchFilterStore.notChosen(this, user)) return   // 🟢🔒 V398
        val stage = currentStage
        val br = countBranch
        autoWatchBusy = true
        lifecycleScope.launch {
            try {
                val changed = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    autoWatch.changed("follow|$stage|$br", br)
                }
                if (isFinishing || isDestroyed) return@launch
                if (!changed) return@launch
                if (stage == currentStage && br == countBranch &&
                    autoScreenFocused && !loadInFlight
                ) {
                    loadTab(currentStage, silent = true)
                }
            } catch (_: Throwable) {
            } finally {
                autoWatchBusy = false
            }
        }
    }

    // (todayOnly is now read safely inside onCreate, not via a lazy at construction time)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityFollowupBinding.inflate(layoutInflater)
            setContentView(binding.root)
            // 🔴🔒 V1316 (১০.০৯.২০২৬, TK-রিপোর্ট ও ভিডিও-প্রুফ — "Follow-up চাপলেই
            // পর্দা কালো হয়ে আটকে যাচ্ছে", একাধিকবার প্রমাণিত, Clear Data-তেও ফিরে
            // এসেছে): আগের V1013-এর "জানালার আঁকা বাদ" পারফরম্যান্স-কৌশল
            // (`window.setBackgroundDrawable(null)`) সরানো হলো — এই একটাই পর্দা
            // প্রজেক্টে এটা করত, ঠিক এই পর্দাটাই কালো হয়ে আটকাত। ⛔ চেহারায় কিছু
            // বদলায় না — থিমের `windowBackground`-ও ঠিক এই একই গ্রেডিয়েন্ট
            // (`bg_app_gradient`, themes.xml), তাই স্বাভাবিক অবস্থায় দৃশ্যত অভিন্ন;
            // কিন্তু এখন নিচের ঘরের আঁকা কোনো কারণে ব্যর্থ হলেও জানালার নিজের
            // রংটা তবু দেখা যাবে, খালি কালো নয়।
            UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
            // ⚡ ধাপ ২ (খাতার সারি B19, TK 28.07.2026 "হ্যাঁ") — Enquiry-র তালিকা
            // এখন RecyclerView-তে: পর্দায় যতটুকু দেখা যায় ততটুকু কার্ডই ফোনের
            // মাথায় থাকে, বাকিগুলো নয়। কার্ড তৈরির ফাংশন `buildFollowCard`
            // এক লাইনও বদলানো হয়নি — ওটাই এখানেও ডাকা হয়, তাই কার্ডের চেহারা,
            // ৩-ট্যাপ, টাকার পপ-আপ, সব বোতাম হুবহু আগের মতোই।
            // ⛔ Visit ও Patient সেকশন এখনো পুরনো পথেই চলে (TK-এর টেস্টের পরে)।
            try {
                binding.rvList.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
                binding.rvList.itemAnimator = null      // no fade/slide: the list must look exactly as before
                binding.rvList.setHasFixedSize(false)   // cards differ in height (remark box grows)
                // Visit / Patient cards carry the patient photo. A few extra
                // ready-made cards are kept aside so a short scroll up-and-down
                // does not have to open the photo again and again.
                binding.rvList.setItemViewCacheSize(8)
                binding.rvList.adapter = followAdapter
            } catch (_: Throwable) { }
            BottomNav.wire(this)
            repository = FollowUpRepository(this)

            val session = NativeSession.current(this)
            if (session == null) {
                startActivity(android.content.Intent(this, LoginActivity::class.java))
                finish(); return
            }
            user = session

            // Safe here: the Activity is attached, so intent is available.
            // 🟢🔒 V692 — Overdue সতর্কতার View থেকে এলে সোজা Overdue ছাঁকনিতে।
            overdue3PlusOnly = intent.getBooleanExtra("overdue3Plus", false)
            dateFilter = when {
                intent.getBooleanExtra("todayOnly", false) -> "Today"
                overdue3PlusOnly -> "Overdue"
                else -> "All"
            }
            // 🔒 খাতার সারি B90 (TK, 29.07.2026 বিকেল ৩.১০): ড্যাশবোর্ডের
            // "N calls pending today" বোতামে চাপ দিলে **তিন সেকশনের আজকের সবাই
            // এক তালিকায়** দেখাবে — Enquiry · Visit · Patient একসাথে।
            // TK-এর কথা: *"ওখানে চাপ দিলে একই লাইনে সমস্ত নম্বরগুলো শো করতে হবে,
            // এনকোয়ারি হোক ভিজিট হোক বা পেশেন্ট হোক।"*
            // ⛔ উপরের কোনো ট্যাবে চাপ দিলেই এই মোড বন্ধ হয়ে আগের স্বাভাবিক
            //    আচরণ ফিরে আসে — রোজকার ব্যবহারে কিছুই বদলায়নি।
            // 🟢🔒 V692 — Overdue সতর্কতাতেও তিন সেকশন একসাথে। কারণ
            //   DashboardActivity ওই "৯ জন" গোনে Enquiry · Visit · Patient
            //   তিনটে মিলিয়ে; এক ট্যাবে দেখালে TK কম দেখতেন, সংখ্যা মিলত না।
            todayAllSections = intent.getBooleanExtra("todayOnly", false) || overdue3PlusOnly
            // 🔴 V511 (উপরের বড় নোট দ্রষ্টব্য) — ব্যানার থেকে এলে তালিকায় শুধু
            //    "আজ কল করার কথা" যাঁদের, তাঁরাই।
            bannerCallsOnly = intent.getBooleanExtra("todayOnly", false)
            // 🔒 খাতার সারি B51: ঘন্টা/Dashboard থেকে আসা নাম (থাকলে)।
            pendingFocusMobile = intent.getStringExtra("remarkMobile") ?: ""
            // 🔵 TK (10.08.2026): "কাল আসার কথা" কার্ডে চাপলে সোজা ওই ব্যক্তির
            // Follow-Up সেকশনে (Enquiry/Visit/Patient) নিয়ে গিয়ে কার্ডটা হাইলাইট।
            pendingFocusCardMobile = intent.getStringExtra("focusCardMobile") ?: ""
            focusCardTag = intent.getStringExtra("focusCardTag") ?: ""   // 🏷️ V1254

            binding.btnBack.setOnClickListener { finish() }
            binding.btnCalendar.setOnClickListener {
                startActivity(android.content.Intent(this, FollowCalendarActivity::class.java))
            }
            // TK-REPORTED BUG FIX (2026-07-16): this button used to just be the
            // ⏰ emoji character -- some Android emoji fonts draw that glyph
            // with a fixed month/day baked into the artwork itself (unrelated
            // to today's real date), which looked like a wrong date to TK.
            // Now it's a real two-line badge, filled in below with today's
            // actual month/day so it's always correct.
            run {
                val cal = java.util.Calendar.getInstance()
                binding.tvCalMonth.text = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time)
                binding.tvCalDay.text = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
            }
            binding.tabEnquiry.setOnClickListener { switchTab("Inquiry") }
            binding.tabVisit.setOnClickListener { switchTab("Patient") }
            binding.tabPatient.setOnClickListener { switchTab("Treatment") }

            // TK-REQUESTED ADDITION (2026-07-23): tab count numbers + Master-
            // only branch picker. Fetched ONCE here, and again only when the
            // branch picker changes; NOT part of the existing 25s auto-
            // refresh loop, so it doesn't add to that recurring cost.
            // TK-REPORTED FIX (2026-07-23, later same day): originally used
            // fetchCount() (a cheap count-only query) here, but that missed
            // fetchTab()'s safety-net fallback cards and showed wrong
            // numbers (e.g. "Visit: 0" while cards were visible below). Now
            // reuses fetchTab() itself so the number always matches the
            // list -- costs more Supabase reads than the old count-only
            // version, TK-approved trade-off.
            if (user.role == "master") {
                // 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): আগে এখানে জোর করে "All" বসত,
                //   তাই প্রতিবার পর্দা খুললেই পাঁচ ব্রাঞ্চের সব সারি নামত। এখন
                //   শেষবার বাছা ব্রাঞ্চটাই বসে (`BranchFilterStore` — পুরো অ্যাপে এক)।
                countBranch = BranchFilterStore.get(this)
                // 🔽🔒 (16.09.2026, TK photo-proof reviewed and reverted: TK
                // said "আমি তো সেরকম বলি নাই") — pill stays visible in the
                // header row exactly as before, own tap target restored.
                binding.branchPicker.visibility = View.VISIBLE
                binding.branchPicker.text = BranchFilterStore.pillText(this)
                binding.branchPicker.setOnClickListener { showBranchPickerMenu() }
            } else {
                countBranch = user.branch
            }
            // 🆕⋮🔒 (16.09.2026, TK photo-proof approved) — this button now
            // opens ONLY the Patient-tab Year picker directly (TK: "ওখানে শুধু
            // 2026/2025/2024 এরকমই থাকবে") — branch pill and calendar badge
            // both stayed in the header row, unchanged.
            // ⛔ V1510 (TK-রিপোর্ট, ১৬.০৯.২০২৬ — "সাদা গোলাকার কেন রেখেছেন,
            // ওটা বাদ দিন") — আগের সাদা বৃত্ত-ব্যাকগ্রাউন্ড (StaffProfileActivity-র
            // ধাঁচ) বাদ দেওয়া হলো, এখন হেডারের গ্রেডিয়েন্টের উপর শুধু সাদা "⋮"।
            binding.btnFollowMenu.background = null
            binding.btnFollowMenu.setTextColor(android.graphics.Color.WHITE)
            binding.btnFollowMenu.setOnClickListener { showPatientYearPickerMenu() }
            // খাতার সারি B31: খোলার সময় শুধু জমানো সংখ্যা বসে। নতুন সংখ্যা আনার
            // কাজটা শুরু হয় চোখে-দেখা তালিকাটা আসার পরে (loadTab-এর শেষে),
            // যাতে ওই তালিকাটা সবচেয়ে আগে আসে।
            refreshTabCounts(withNetwork = false)

            binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) { applySearch() }
            })

            setupDateFilterButtons()

            // 🔵 TK (10.08.2026): সাধারণভাবে Enquiry দিয়েই শুরু; কিন্তু "কাল আসার
            // কথা" থেকে এলে ওই ব্যক্তি যে সেকশনে আছেন সেটা দিয়েই শুরু হয় — ক্যাশ
            // থেকে জানা গেলে সঙ্গে সঙ্গে, না জানলে তাজা তালিকা এলে ঠিক সেকশনে সরে।
            applyCallListChrome()   // 🟣 V710 — ব্যানার-মোড হলে ট্যাব ও ফিল্টার লুকায়
            val startStage = initialFocusStage()
            /* 🔵 V523: শর্তে `pendingFocusCardMobile` আর দরকার নেই — আগে
               ওটা ফাঁকা থাকলে `initialFocusStage()` **সবসময়** "Inquiry"
               ফেরাত, তাই ডান দিকের শর্তটাই কখনো সত্যি হত না। এখন Reports
               থেকেও অন্য ট্যাব চাওয়া যায়, তাই শুধু ট্যাবটাই দেখা হয়।
               ⛔ পুরোনো সব পথে ফল **অবিকল একই**। */
            if (startStage != "Inquiry") {
                switchTab(startStage)
                if (!tabCountsAsked) { tabCountsAsked = true; refreshTabCounts() }
            } else {
                loadTab(startStage)
            }
        } catch (e: Throwable) {
            // Never crash — show the exact reason in a dialog that stays on screen
            // so it can be read/screenshotted, then return to the dashboard.
            try {
                AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, "Follow-up problem (reason)"))
                    .setMessage("${e.javaClass.simpleName}\n\n${e.message}\n\n${e.stackTrace.take(4).joinToString("\n") { it.toString() }}")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .setCancelable(false)
                    .show().also { PremiumAlert.paint(it) }
            } catch (_: Throwable) {
                finish()
            }
        }
    }

    private fun setupDateFilterButtons() {
        // 🟢🔒 V692 — উপরের চিপে স্টাফ নিজে চাপ দিলেই "৩+ দিন" মোড বন্ধ,
        //   স্বাভাবিক ছাঁকনি ফিরে আসে (`todayAllSections`-এর মতোই আচরণ)।
        btns.forEach { (b, v) -> b.setOnClickListener { overdue3PlusOnly = false; dateFilter = v; paintDateFilterButtons(); paintTabCounts(); applySearch() } }
        binding.fCustom.setOnClickListener { pickCustomDateRange { paintDateFilterButtons() } }
        // 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B68):
        // TK: *"ফিল্টার by সিরিয়াল নাম্বার... সেখানে চাপ দিলে এক নম্বর থেকে
        // সমস্ত পেশেন্টের ডিটেলস দেখা যাবে।"* — তারিখের কোনো ছাঁকনি ছাড়াই
        // ওই ভাগের সবাই, ১ নম্বর থেকে পরপর।
        binding.fSerial.setOnClickListener { dateFilter = "Serial"; paintDateFilterButtons(); paintTabCounts(); applySearch() }
        // 🟢🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ) — বাকি চিপগুলোর হুবহু একই ধাঁচ।
        binding.fUnexpected.setOnClickListener { dateFilter = "Unexpected"; paintDateFilterButtons(); paintTabCounts(); applySearch() }
        // 🟢🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ — "২ যায়গায়ই থাকবে, Unexpected
        // Time-এর মতন") — বাকি চিপগুলোর হুবহু একই ধাঁচ।
        binding.fRunning.setOnClickListener { dateFilter = "Running"; paintDateFilterButtons(); paintTabCounts(); applySearch() }
        binding.fIncompletePatient.setOnClickListener { dateFilter = "Incomplete"; paintDateFilterButtons(); paintTabCounts(); applySearch() }
        binding.fCompletePatient.setOnClickListener { dateFilter = "Complete"; paintDateFilterButtons(); paintTabCounts(); applySearch() }
        // 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B69)
        binding.fSheet.setOnClickListener { showDownloadSheetOptions() }
        // 🔒 খাতার সারি B69: এটা ছাঁকনি নয়, একটা কাজ — তাই বাকি চিপের নীল রঙে না
        // রেখে হালকা সবুজে রাখা হলো (প্রুফে TK যেভাবে পাশ করেছেন)। রং বসানো
        // হয় এখানেই, কারণ `paintDateFilterButtons()` শুধু ছাঁকনির চিপগুলো রাঙায়।
        binding.fSheet.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E2F6E8"))
        binding.fSheet.setTextColor(android.graphics.Color.parseColor("#0F7A3C"))
        paintDateFilterButtons()
    }

    private val btns by lazy {
        listOf(
            binding.fAll to "All",
            // 🔒 TK-ORDER (31.07.2026, খাতার সারি B213 — "all এর পরে তাহলে my call হবে"):
            binding.fMyCall to "My Call",
            binding.fToday to "Today",
            binding.fOverdue to "Overdue",
            binding.fWeek to "This Week",
            binding.fMonth to "This Month"
        )
    }

    // TK-REQUESTED (2026-07-24): pulled out of setupDateFilterButtons() so
    // the card badge (below) can also switch the same top filter -- exact
    // same repaint logic, nothing about the top buttons' own behavior changes.
    private fun paintDateFilterButtons() {
        val blue = android.graphics.Color.parseColor("#1167D8")
        val light = android.graphics.Color.parseColor("#E8F2FF")
        (btns + (binding.fCustom to "Custom") + (binding.fSerial to "Serial") + (binding.fUnexpected to "Unexpected") +
            (binding.fRunning to "Running") + (binding.fIncompletePatient to "Incomplete") + (binding.fCompletePatient to "Complete")).forEach { (b, v) ->
            val on = v == dateFilter
            b.backgroundTintList = android.content.res.ColorStateList.valueOf(if (on) blue else light)
            b.setTextColor(if (on) android.graphics.Color.WHITE else blue)
        }
    }

    // TK-REQUESTED (2026-07-24): tapping the "⏰ Overdue" / "⏰ Today Due"
    // badge on a card now jumps the top filter row to that same tab (Overdue
    // / Today) -- same effect as tapping the tab itself.
    private fun applyQuickFilter(value: String) {
        dateFilter = value
        paintDateFilterButtons()
        applySearch()
    }

    /** Custom Date filter: pick a From date then a To date; rows are then
     *  filtered to nextFollow within that range. No other flow is touched. */
    private fun pickCustomDateRange(after: () -> Unit) {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val c = java.util.Calendar.getInstance()
        DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, d ->
            val f = java.util.Calendar.getInstance().apply { set(y, m, d) }
            customFrom = fmt.format(f.time)
            DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y2, m2, d2 ->
                val t = java.util.Calendar.getInstance().apply { set(y2, m2, d2) }
                customTo = fmt.format(t.time)
                dateFilter = "Custom"
                after()
                applySearch()
                Toast.makeText(this, "Custom: $customFrom to $customTo", Toast.LENGTH_SHORT).show()
            }, y, m, d).apply { datePicker.minDate = f.timeInMillis }.show()
        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    /**
     * 🔒 খাতার সারি B90 (TK, 29.07.2026 বিকেল ৩.১০) — **আজকের সব কল এক তালিকায়**।
     *
     * ড্যাশবোর্ডের ব্যানার তিন সেকশন থেকে গোনে (সারি B61), তাই চাপ দিলে
     * তিন সেকশনের আজকের সবাইকেই এক তালিকায় দেখাতে হবে।
     *
     * ⛔ **নতুন কোনো ক্লাউড-কল নেই** — ঠিক সেই তিনটে ডাকই হয় যেগুলো ট্যাবের
     *    সংখ্যা আনতে এমনিতেই হত (`refreshTabCounts`), আর তিনটে **একসাথে**
     *    (পাশাপাশি) আনা হয়, তাই দেরিও হয় না।
     * ⛔ কার্ড তৈরির কোড (`buildFollowCard`) এক অক্ষরও বদলানো হয়নি — প্রতিটা
     *    কার্ড তার **নিজের সেকশন অনুযায়ীই** বোতাম ও নিয়ম পায়, ঠিক আগের মতো।
     * ⛔ ছাঁকনি · খোঁজা · ব্রাঞ্চের পাহারা · টাকার হিসাব — সব আগের পথেই চলে
     *    (`applySearch()` অপরিবর্তিত)।
     * ⚠️ সিরিয়াল নম্বর প্রতি সেকশনে আলাদা, তাই মিশ্র তালিকায় নম্বর এলোমেলো
     *    দেখাবে — **TK জেনেশুনে এটা মেনে নিয়েছেন** ("মিশ্র নম্বর থাকলেও অসুবিধা নেই")।
     * 🔒 এক নম্বর একবারই থাকে: অ্যাপের "এক নম্বর = এক সেকশন" নিয়মের কারণে একই
     *    মানুষ দুই সেকশনে থাকেন না; তবু আইডি ধরে দ্বিতীয়বার ঢোকা আটকানো হয়েছে।
     */
    private fun loadTodayAllSections(silent: Boolean) {
        val branch = effectiveBranch()

        // ১) আগে ফোনের জমানো তালিকা — সঙ্গে সঙ্গে কিছু দেখানোর জন্য।
        if (!silent) {
            val cached = mutableListOf<FollowUpItem>()
            val seen = HashSet<String>()
            for (st in listOf("Inquiry", "Patient", "Treatment")) {
                val c = try { repository.loadCachedTab(st, branch) } catch (_: Throwable) { null } ?: continue
                for (row in c) if (row.id.isBlank() || seen.add(row.id)) cached.add(row)
            }
            if (cached.isNotEmpty()) {
                loadedItems = cached
                try { applySearch() } catch (_: Throwable) { }
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
            } else {
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.text = "Loading..."
                binding.tvEmpty.visibility = View.VISIBLE
                setListVisible(false)
            }
        }

        val myToken = ++tabLoadToken
        val crashGuard = kotlinx.coroutines.CoroutineExceptionHandler { _, _ ->
            runOnUiThread {
                try {
                    if (!silent) binding.progressLoad.visibility = View.GONE
                    stopPullSpinner()
                } catch (_: Throwable) { }
            }
        }
        loadInFlight = true
        autoRefreshHandler.removeCallbacks(loadFlagRelease)
        autoRefreshHandler.postDelayed(loadFlagRelease, 90_000L)
        lifecycleScope.launch(crashGuard) {
            try {
                val three = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.coroutineScope {
                        // 🚀🔒 V1531 — একই মুহূর্তে অন্য কোথাও (যেমন refreshTabCounts)
                        // একই স্টেজের fetchTab() চললে দ্বিতীয়বার নতুন করে শুরু না
                        // করে সেটার সাথেই জোড়া লাগে (নিচে fetchTabDeduped)।
                        val a = async { try { FollowUpRepository.fetchTabDeduped(repository, "Inquiry", branch, user.name, user.mobile).await() } catch (_: Throwable) { null } }
                        val b = async { try { FollowUpRepository.fetchTabDeduped(repository, "Patient", branch, user.name, user.mobile).await() } catch (_: Throwable) { null } }
                        val c = async { try { FollowUpRepository.fetchTabDeduped(repository, "Treatment", branch, user.name, user.mobile).await() } catch (_: Throwable) { null } }
                        Triple(a.await(), b.await(), c.await())
                    }
                }
                if (!silent) binding.progressLoad.visibility = View.GONE
                stopPullSpinner()
                // পর্দা এর মধ্যে বদলে গেলে (ট্যাবে চাপ পড়লে) পুরনো ফল ফেলে দাও।
                if (!todayAllSections || myToken != tabLoadToken) return@launch

                // ট্যাবের সংখ্যাগুলোও এই একই তালিকা থেকেই বসে — বাড়তি ডাক নেই।
                three.first?.let { lastEnqAll = it }
                three.second?.let { lastVisitAll = it }
                three.third?.let { lastPatAll = it }
                paintTabCounts()

                val merged = mutableListOf<FollowUpItem>()
                val seen = HashSet<String>()
                for (list in listOf(three.first, three.second, three.third)) {
                    if (list == null) continue
                    for (row in list) if (row.id.isBlank() || seen.add(row.id)) merged.add(row)
                }
                // ⛔ একটাও তালিকা না এলে (লাইন খারাপ) পর্দায় যা আছে তাই থাক —
                //    জমানো তালিকা মুছে ফাঁকা পর্দা দেখানো হবে না।
                if (three.first == null && three.second == null && three.third == null) return@launch
                loadedItems = merged
                try { applySearch() } catch (_: Throwable) { }
            } finally {
                loadInFlight = false
                autoRefreshHandler.removeCallbacks(loadFlagRelease)
            }
        }
    }

    /** 🟣🔒🔁 V710 — ব্যানার থেকে আসা "আজকের কল" তালিকায় ট্যাব ও ফিল্টারের
     *  সারি লুকিয়ে একটাই পরিষ্কার লাইন দেখায়; অন্য সব ক্ষেত্রে হুবহু আগের চেহারা।
     *  ⛔ শুধু **দেখা/লুকানো** — কোনো তথ্য, ছাঁকনি বা সেভ এতে বদলায় না।
     *  ⛔ `HIDE_TABS_IN_CALL_LIST = false` করলেই পুরোটা আগের মতো (উপরে দেখুন)। */
    private fun applyCallListChrome() {
        try {
            val hide = HIDE_TABS_IN_CALL_LIST && todayAllSections
            binding.tabRow.visibility = if (hide) View.GONE else View.VISIBLE
            binding.filterRow.visibility = if (hide) View.GONE else View.VISIBLE
            binding.tvAllSectionsHead.visibility = if (hide) View.VISIBLE else View.GONE
            if (hide) {
                // ⛔ লেখাটা ইংরেজি-বাংলা মেশানো নয় — বাংলা-বন্ধ স্টাফের ফোনেও
                //    যাতে হুবহু একই দেখায়, তাই পুরোটা ইংরেজি।
                val what = if (overdue3PlusOnly) "OVERDUE CALLS" else "TODAY'S CALLS"
                binding.tvAllSectionsHead.text = "\uD83D\uDCDE  $what  -  Enquiry . Visit . Patient"
            }
        } catch (_: Throwable) { /* চেহারার কাজ — ব্যর্থ হলেও তালিকা আগের মতোই চলে */ }
    }

    private fun switchTab(stage: String) {
        // 🔒 খাতার সারি B90: মিশ্র মোডে থাকলে **একই ট্যাবে** চাপ দিলেও মোডটা
        // বন্ধ হয়ে স্বাভাবিক তালিকায় ফিরতে হবে — তাই পুরনো "একই ট্যাব হলে কিছু
        // কোরো না" নিয়মটা শুধু মিশ্র মোড বন্ধ থাকলেই খাটে।
        if (stage == currentStage && !todayAllSections) return
        todayAllSections = false
        applyCallListChrome()   // 🟣 V710 — ট্যাবে চাপ দিলে সারি দুটো আবার ফিরে আসে
        listOf(binding.tabEnquiry, binding.tabVisit, binding.tabPatient).forEach {
            it.setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_tab_inactive)
            it.setTextColor(getColor(com.tkbiswas.pilesclinic.R.color.clinic_text_primary))
        }
        val activeTab = when (stage) { "Inquiry" -> binding.tabEnquiry; "Patient" -> binding.tabVisit; else -> binding.tabPatient }
        activeTab.setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_tab_active)
        activeTab.setTextColor(getColor(com.tkbiswas.pilesclinic.R.color.white))
        // TK-REPORTED (2026-07-25, every staff complains): tapping another tab
        // used to keep showing the PREVIOUS tab's cards until the network
        // fetch finished -- which on a slow connection is several seconds, so
        // the app felt very slow. The cache-first trick already used when the
        // screen is opened cold is now used here too: this tab's last-known
        // list appears instantly, and the fresh data quietly replaces it the
        // moment it arrives (exactly as before). Nothing else changes -- same
        // fetch, same data, same design.
        currentStage = stage
        val cachedForTab = try { repository.loadCachedTab(stage, effectiveBranch()) } catch (_: Throwable) { null }
        loadedItems = cachedForTab ?: emptyList()
        try { applySearch() } catch (_: Throwable) { }
        if (loadedItems.isEmpty()) {
            binding.tvEmpty.text = "Loading..."
            binding.tvEmpty.visibility = View.VISIBLE
            setListVisible(false)
        }
        loadTab(stage, silent = true, fromTabSwitch = true)
    }

    // TK-REPORTED BUG FIX (2026-07-23): this used to be a raw count-only
    // query straight on the "followups" table, so it silently EXCLUDED the
    // safety-net fallback cards fetchTab() synthesizes for a patient whose
    // followups row hasn't been created yet (see FollowUpRepository's
    // "Patient"/"Treatment" fallback blocks) -- causing "Visit: 0" while
    // the list below still showed real cards. Now the count is simply the
    // size of the exact same list fetchTab() returns, so the number on the
    // tab and the cards below can never disagree again. Also now uses
    // effectiveBranch() so a Master's branch-picker choice matches what the
    // list shows. TK-DISCLOSED TRADE-OFF: this fetches all 3 stages fully
    // (same queries loadTab already runs for one stage) instead of a cheap
    // HEAD/count request, so it costs more Supabase reads than before --
    // TK approved this (2026-07-23) to fix the mismatch.
    /**
     * 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B31): *"Follow-up
     * সেখানে ৫-৭ বার টাচ করার পরে এটা ওপেন হলো... আরো স্লো করে দিয়েছেন।"*
     *
     * **আসল কারণ:** পর্দা খোলার সময় **একসাথে চারটে ভারী কাজ** শুরু হত —
     * তিনটে ট্যাবের সংখ্যা গোনার জন্য তিনটে পুরো তালিকা, আর যে তালিকাটা
     * চোখে দেখা যাচ্ছে তার জন্য আরেকটা। চারটেই একই সরু লাইনে ভাগাভাগি করত,
     * তাই **যেটা দেখা দরকার সেটাই সবচেয়ে দেরিতে আসত**।
     *
     * **এখন:** জমানো সংখ্যা আগের মতোই সঙ্গে সঙ্গে বসে; কিন্তু **নতুন সংখ্যা
     * আনার তিনটে কাজ শুরু হয় শুধু চোখে-দেখা তালিকাটা আসার পরে**
     * ([withNetwork] = false দিলে ওগুলো এখন চলবে না)।
     * ⛔ সংখ্যা কোথা থেকে আসে সেটা এক অক্ষরও বদলায়নি — TK-এর ২৩.০৭-এ পাশ করা
     * নিয়ম (সংখ্যা = তালিকার মাপ) হুবহু আগের মতোই আছে, শুধু **ক্রম** বদলেছে।
     */
    /** খোলার পরে ট্যাবের সংখ্যা একবারই আনা হয়; তারপর স্বাভাবিক নিয়মে (ট্যাব
     *  বদল · ব্রাঞ্চ বদল · টেনে-নামিয়ে রিফ্রেশ) আগের মতোই আপডেট হয়। */
    private var tabCountsAsked = false

    /**
     * 🚨 TK-REPORTED (29.07.2026 দুপুর ২.৪০, দুটো ছবিসহ · খাতার সারি B89):
     * ড্যাশবোর্ডে *"3 calls pending today"* দেখাচ্ছিল, কিন্তু চাপ দিয়ে ঢুকলে
     * মাত্র **দুটো নাম**।
     *
     * **আসল কারণ (কোড দেখে, আন্দাজ নয়):** ব্যানারটা TK-এর নিয়ম মেনে
     * **তিনটে ভাগ থেকেই** গোনে (খাতার সারি B61 — Enquiry · Visit · Patient),
     * কিন্তু চাপ দিলে পর্দা খোলে **শুধু Enquiry ট্যাবে**, আর ট্যাবের উপরের
     * সংখ্যাগুলো (৫০ · ৪০ · ৩৭) ছিল **মোট সংখ্যা** — ছাঁকনির সঙ্গে বদলাত না।
     * তাই তৃতীয় জন Visit/Patient ট্যাবে লুকিয়ে থাকত, আর দুটো সংখ্যা কখনো
     * মিলত না।
     *
     * **এখন ট্যাবের সংখ্যাগুলো চলতি ছাঁকনি মেনে চলে** — Today বাছলে তিনটে
     * ট্যাবে আজকের কতগুলো তা দেখায়, আর **তিনটে যোগ করলে ব্যানারের সংখ্যাটাই
     * হয়**। তাই TK দেখেই বুঝবেন তৃতীয়জন কোন ট্যাবে।
     *
     * ⛔ **বাড়তি একটাও ক্লাউড-কল নয়** — আগের সেই একই তিনটে তালিকা মনে রাখা
     *    হয়, শুধু গোনাটা ফোনের ভিতরেই আবার করা হয়।
     * ⛔ ছাঁকনি "All" থাকলে সংখ্যা আগের মতোই মোট সংখ্যাই দেখায় — কিছু বদলায়নি।
     * ⛔ কার্ডের তালিকা · ডিজাইন · ব্রাঞ্চের নিয়ম কিছুই ছোঁয়া হয়নি।
     */
    private var lastEnqAll: List<FollowUpItem>? = null
    private var lastVisitAll: List<FollowUpItem>? = null
    private var lastPatAll: List<FollowUpItem>? = null

    private fun paintTabCounts() {
        // 🗓️🔒 (16.09.2026) — explicit `stage=` on each call so the Patient
        // badge's count reflects the active Year filter (patientYearFilter)
        // even when a different tab is currently open, while Enquiry/Visit
        // counts stay completely untouched by the year filter.
        lastEnqAll?.let { binding.tabEnquiry.text = "👥 ${applyDateFilter(it, "Inquiry").size} Enquiry" }
        lastVisitAll?.let { binding.tabVisit.text = "👣 ${applyDateFilter(it, "Patient").size} Visit" }
        lastPatAll?.let { binding.tabPatient.text = "👤 ${applyDateFilter(it, "Treatment").size} Patient" }
    }

    private fun refreshTabCounts(withNetwork: Boolean = true) {
        // TK-REPORTED (2026-07-24): these 3 counts took a long time to
        // appear every time, because they always waited on 3 full network
        // fetches before showing anything. Now shows cached counts INSTANTLY
        // (same loadCachedTab() already used/trusted elsewhere in this
        // file), then quietly fetches the real numbers in the background
        // and corrects them the instant that finishes -- nothing about
        // which numbers are shown, or the fetchTab()-based accuracy fix
        // from 2026-07-23, changes.
        // 🟢🔒 V398: ব্রাঞ্চ না-বাছা থাকলে সংখ্যা আনার জন্যও কোনো অনুরোধ যাবে না।
        if (BranchFilterStore.notChosen(this, user)) return
        val branch = effectiveBranch()
        val cachedEnq = repository.loadCachedTab("Inquiry", branch)
        val cachedVisit = repository.loadCachedTab("Patient", branch)
        val cachedPat = repository.loadCachedTab("Treatment", branch)
        // TK-REPORTED (2026-07-27, photo-proof: "46 Enquiry · 0 Visit · 0
        // Patient" while the list was still loading). All three labels used to
        // be written together the moment ANY ONE of them had a cache, and a
        // tab with no cache was written as "0". So a real 35 could be shown as
        // 0 -- not "not known yet", but a wrong number. Now each tab is only
        // written when that tab's own figure is actually known.
        if (cachedEnq != null) lastEnqAll = cachedEnq
        if (cachedVisit != null) lastVisitAll = cachedVisit
        if (cachedPat != null) lastPatAll = cachedPat
        paintTabCounts()
        if (!withNetwork) return
        lifecycleScope.launch {
            try {
                // PERFORMANCE FIX (2026-07-25, TK-reported slowness): these
                // three were fetched one after the other, so the counts (and
                // the work they trigger) took as long as all three added up.
                // They do not depend on each other, so they now run at the
                // same time — same three fetches, same numbers, just not
                // queued behind one another.
                //
                // TK-REPORTED (2026-07-27): they used to be collected with a
                // single Triple(...) that waited for ALL THREE before any
                // number was shown -- so one slow tab held the other two's
                // correct figures back. Each tab now updates the moment its
                // own figure arrives.
                // 🚀🔒 V1531 — dedup: switchTab()-এর নিজের fetchTab() এখানকারই কোনো
                // ডাক তখনো চলতে থাকলে সেটার সাথেই জোড়া লাগে, দ্বিতীয়বার শুরু হয় না।
                val dEnq = async(Dispatchers.IO) { try { FollowUpRepository.fetchTabDeduped(repository, "Inquiry", branch, user.name, user.mobile).await() } catch (_: Throwable) { null } }
                val dVisit = async(Dispatchers.IO) { try { FollowUpRepository.fetchTabDeduped(repository, "Patient", branch, user.name, user.mobile).await() } catch (_: Throwable) { null } }
                val dPat = async(Dispatchers.IO) { try { FollowUpRepository.fetchTabDeduped(repository, "Treatment", branch, user.name, user.mobile).await() } catch (_: Throwable) { null } }
                launch { val r = dEnq.await(); if (r != null) { lastEnqAll = r; paintTabCounts(); maybeFocusFromLists() } }
                launch { val r = dVisit.await(); if (r != null) { lastVisitAll = r; paintTabCounts(); maybeFocusFromLists() } }
                launch { val r = dPat.await(); if (r != null) { lastPatAll = r; paintTabCounts(); maybeFocusFromLists() } }
            } catch (_: Throwable) { /* counts are a nice-to-have -- never blocks/breaks the tabs themselves */ }
        }
    }

    // TK-REQUESTED FIX (2026-07-23): the branch picker used to ONLY change
    // the tab-count numbers, not the actual card list below -- a Master
    // could select "Kishanganj" up top and still see every branch's
    // patients in the list. Now both the counts AND the list use this same
    // single source of truth. Non-master users are completely untouched --
    // they always keep seeing their own user.branch, exactly as before.
    // 🔴🔒 V453 (20.08.2026, TK-অনুমোদিত): JPE-CRP-এর জন্য Falakata+Birpara
    // অতিরিক্ত দেখা/Edit — CrossBranchStaffAccess.kt কেন্দ্রীয়ভাবে সিদ্ধান্ত
    // নেয়। বাকি সবার জন্য এই ফাংশনের ফলাফল আগের মতোই অভিন্ন।
    private fun effectiveBranch(): String =
        if (user.role == "master") countBranch else CrossBranchStaffAccess.effectiveViewBranch(user)

    private fun showBranchPickerMenu() {
        val branches = BranchFilterStore.choices()
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Branch"))
            .setSingleChoiceItems(branches.toTypedArray(), BranchFilterStore.indexInChoices(this)) { dialog, which ->
                countBranch = branches[which]
                BranchFilterStore.set(this@FollowUpActivity, countBranch)   // 🟢 V398: সব পর্দার জন্য মনে রাখা
                binding.branchPicker.text = BranchFilterStore.pillText(this@FollowUpActivity)
                refreshTabCounts()
                /* 🐞🔒 V1324 (TK-রিপোর্ট, ১১.০৯.২০২৬: "ব্রাঞ্চ বদলালে হেডার
                   বদলে যায় কিন্তু নিচের তালিকা আগের ব্রাঞ্চেরই থেকে যায়,
                   অনেক পরে বদলায়") — এখানে আগে `silent = true` দেওয়া থাকায়
                   `loadTab()`-এর ব্রাঞ্চ-ভিত্তিক cache-first ধাপটাই (নিচে)
                   বাদ পড়ে যেত — `loadedItems` তখনও পুরনো ব্রাঞ্চের, আর নতুন
                   ব্রাঞ্চের আসল তালিকা আসা পর্যন্ত সেটাই দেখাত। এখন
                   `silent = false` — switchTab()-এর মতোই — তাই ব্রাঞ্চ
                   বদলালেই সঙ্গে সঙ্গে **নতুন ব্রাঞ্চের নিজের** জমানো তালিকা
                   (থাকলে) দেখায়, নইলে "Loading...", পুরনো ব্রাঞ্চের সারি
                   একমুহূর্তের জন্যও দেখায় না। */
                loadTab(currentStage, silent = false)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /* 🗓️🔒 (16.09.2026, TK photo-proof approved) — lets the Patient tab
       switch to a different registration year. Purely a client-side filter
       (no network call, rule ৭খ "no delay" honoured) — switching years never
       loses data, it's always reversible from this same menu. */
    private fun showPatientYearPickerMenu() {
        val years = availablePatientYears()
        val labels = years.map { it.toString() }.toTypedArray()
        val selectedIdx = years.indexOf(patientYearFilter).let { if (it < 0) 0 else it }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Year"))
            .setSingleChoiceItems(labels, selectedIdx) { dialog, which ->
                // 🗓️🔒 (17.09.2026) — আগে এখানে শুধু Patient/Treatment ট্যাব
                // খোলা থাকলেই তালিকা আবার আঁকা হতো (তখন বছর-ছাঁকনি শুধু ওই
                // ট্যাবেই কাজ করত)। এখন Enquiry/Visit ট্যাবও বছর মানে, তাই
                // যে ট্যাবই খোলা থাকুক, সবসময় আবার আঁকতে হবে — নইলে Enquiry/
                // Visit ট্যাবে বছর বাছলেও পর্দা পুরনোই থেকে যেত (ঠিক এই
                // বাগটাই TK ধরেছিলেন, ১৭.০৯.২০২৬)।
                patientYearFilter = years[which]
                paintTabCounts()
                applySearch()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private var firstResume = true
    // TK-REQUESTED (2026-07-18): remembers which item's call button was
    // tapped, so onResume can offer to update the remark -- same
    // no-permission pattern as FollowCalendarActivity.
    private var pendingCallItem: FollowUpItem? = null

    // 🔒 খাতার সারি B51: ঘন্টা (Briefing) বা Dashboard থেকে কোনো নামে চাপ দিয়ে
    // এই পর্দা খুললে ওই নামের রিমার্ক বাক্সটাই সরাসরি খুলবে — মাঝখানে আর
    // একটা পপ-আপ নয়। কাজ হয়ে গেলে ঘরটা ফাঁকা করে দেওয়া হয়।
    private var pendingFocusMobile: String = ""
    /** 🔵 TK (10.08.2026): "কাল আসার কথা" কার্ড থেকে ফোকাস করার নম্বর (থাকলে)। */
    private var pendingFocusCardMobile: String = ""
    /* 🏷️🔒 V1254 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, খাতার সারি ৩৭৫) —
       নোটিশ থেকে আসা কার্ডে যে ছোট ট্যাগটা বসবে ("NEW ENQUIRY" / "NEW
       REGISTRATION")। ⛔ ফাঁকা থাকলে ট্যাগ বসে না — অন্য পথে (যেমন "কাল
       আসার কথা" কার্ড) এলে আগের মতোই শুধু হাইলাইট হয়, বানানো লেখা নয়। */
    private var focusCardTag: String = ""
    /** 🔒 খাতার সারি B65: কার্ডের আইডি → ওই ব্রাঞ্চের ভিতরের সিরিয়াল নম্বর। */
    private var serialByItemId: Map<String, Int> = emptyMap()
    /** 🔒 খাতার সারি B69: পর্দায় এখন যে তালিকাটা যে ক্রমে দেখা যাচ্ছে। */
    private var shownItems: List<FollowUpItem> = emptyList()
    private var sheetBusy = false
    private var todayEnq = listOf<String>()
    private var todayReg = listOf<String>()
    private var todayAdv = listOf<String>()
    override fun onResume() {
        super.onResume()
        // খাতার সারি B40: পর্দায় ফিরলে আগের কোনো আটকে-থাকা চাকতি থাকলে থামিয়ে দাও।
        stopPullSpinner()
        // Cancel any pending tick first so onResume never double-schedules.
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
        autoRefreshHandler.postDelayed(autoRefreshRunnable, LiveRefresh.TICK_MS)
        autoWatch.startRealtime({ countBranch }) {
            if (!autoScreenFocused || loadInFlight || autoWatchBusy) return@startRealtime
            if (BranchFilterStore.notChosen(this, user)) return@startRealtime
            loadTab(currentStage, silent = true)
        }
        // TK-REQUESTED (2026-07-27): pull the list down to refresh, like other
        // phone apps. It simply runs the SAME load this screen already does --
        // no new query, no new rule. The little spinner stops as soon as that
        // load finishes (stopPullSpinner below).
        binding.swipeRefresh.setColorSchemeColors(
            android.graphics.Color.parseColor("#0EA25F"),
            android.graphics.Color.parseColor("#1167D8")
        )
        binding.swipeRefresh.setOnRefreshListener {
            armSpinnerWatchdog()   // খাতার সারি B40: চাকতি কখনো আটকে থাকবে না
            loadTab(currentStage)
        }
        // Two lists now sit inside one frame, so SwipeRefreshLayout can no
        // longer work out by itself whether the list is already at the top.
        // This tells it to ask whichever list is actually on screen, so
        // pull-to-refresh keeps behaving exactly as TK approved.
        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            if (binding.rvList.visibility == View.VISIBLE) binding.rvList.canScrollVertically(-1)
            else binding.scrollList.canScrollVertically(-1)
        }
        // 🔒 খাতার সারি B51 (TK, 28.07.2026): কল করার পরে স্টাফ যখনই ফিরবেন
        // তখনই মনে করানো হবে — অ্যাপ একবার বন্ধ হয়ে গেলেও। পপ-আপের চেহারা
        // আগের মতোই, শুধু নামটা এখন ফোনের জমানো তালিকা থেকেও আসতে পারে।
        remindPendingRemark()
        if (firstResume) { firstResume = false; return }
        loadTab(currentStage, silent = true)
    }

    override fun onPause() {
        super.onPause()
        stopPullSpinner()   // খাতার সারি B40
        autoRefreshHandler.removeCallbacks(spinnerWatchdog)
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable)
        autoWatch.stopRealtime()
    }


    private fun showTodayListDialog(title: String, items: List<String>) {
        val body = if (items.isEmpty()) "Nothing today." else items.joinToString("\n\n")
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "$title (${items.size})"))
            .setMessage(body)
            .setPositiveButton("Close", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** Stops the pull-to-refresh circle; safe to call as often as needed. */
    /** খাতার সারি B40: লোড শেষ — নিজে থেকে হওয়া পরের রিফ্রেশ এবার চলতে পারে। */
    private val loadFlagRelease = Runnable { loadInFlight = false }

    /**
     * 🚨 TK-REPORTED (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B40):
     * *"আমাদের সামনে যেন না করে — ব্যাকগ্রাউন্ডে চুপিচুপি ঘুরুক, অথবা উপর থেকে
     * টানলে তখন যেন refresh হয়।"*
     *
     * **চাকতিটা কেন নিজে থেকে ঘুরত (আসল কারণ):** ওটা টেনে-নামানোর চাকতি
     * (রংও মিলে যায় — সবুজ ও নীল)। স্টাফ একবার টানলে ওটা ঘুরতে শুরু করে আর
     * লোড শেষ হলে থামে। কিন্তু ধীর লাইনে লোড শেষ হওয়ার আগেই **নিজে-নিজে
     * রিফ্রেশ নতুন লোড শুরু করে দিত, আর আগেরটা বাতিল হয়ে যেত** — তখন থামানোর
     * লাইনটা আর চলতই না, ফলে **চাকতিটা চিরকাল ঘুরতে থাকত**।
     *
     * এই পাহারাদার নিশ্চিত করে: চাকতি একবার ঘুরলে **সর্বোচ্চ ১২ সেকেন্ড**,
     * তারপর যাই হোক থেমে যাবে। ⛔ লোড থামে না — শুধু চাকতিটা লুকোয়, কাজ
     * পিছনে চলতেই থাকে। ঠিক যেমন TK চেয়েছেন।
     */
    private val spinnerWatchdog = Runnable {
        try { if (binding.swipeRefresh.isRefreshing) binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
    }

    private fun armSpinnerWatchdog() {
        autoRefreshHandler.removeCallbacks(spinnerWatchdog)
        autoRefreshHandler.postDelayed(spinnerWatchdog, 12_000L)
    }

    private fun clearLoadFlag() {
        loadInFlight = false
        try { autoRefreshHandler.removeCallbacks(loadFlagRelease) } catch (_: Throwable) {}
    }

    private fun stopPullSpinner() {
        clearLoadFlag()
        try { autoRefreshHandler.removeCallbacks(spinnerWatchdog) } catch (_: Throwable) {}
        try { if (binding.swipeRefresh.isRefreshing) binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
    }

    private fun loadTab(stage: String, silent: Boolean = false, fromTabSwitch: Boolean = false) {
        // 🔒 খাতার সারি B90: মিশ্র মোড চালু থাকলে এক সেকশনের তালিকা আনা হয় না —
        // তিনটে একসাথে আনা হয়। ⛔ নিচের পুরনো কোডের একটি লাইনও বদলানো হয়নি,
        // তাই স্বাভাবিক ব্যবহারে (মোড বন্ধ) সবকিছু আগের মতোই চলে।
        // ⚠️ পর্দার নিজে-নিজে রিফ্রেশও (২৫ সেকেন্ড পরপর) এই একই পথে আসে,
        //    তাই রিফ্রেশে মিশ্র তালিকা ভেঙে যায় না।
        // 🟢🔒 V398: মাস্টার এখনো ব্রাঞ্চ না-বাছলে **একটাও ক্লাউড-অনুরোধ যাবে না**।
        //   ⚠️ গার্ডটা মিশ্র-মোডের **আগেই** — FollowUpRepository.branchScopeFilter()
        //   (লাইন ১১৫) ফাঁকা নামকে "ছাঁকনি নেই" ধরে, তাই গার্ড ছাড়া সব ব্রাঞ্চ নেমে যেত।
        if (BranchFilterStore.notChosen(this, user)) {
            currentStage = stage
            loadedItems = emptyList()
            setListVisible(false)
            binding.tvEmpty.text = BranchFilterStore.ASK_TEXT
            binding.tvEmpty.visibility = View.VISIBLE
            binding.progressLoad.visibility = View.GONE
            return
        }
        if (todayAllSections) { loadTodayAllSections(silent); return }
        currentStage = stage
        // TK-REQUESTED ADDITION (2026-07-20): same "show what was already on
        // the phone instantly" pattern added to Doctor Queue -- on a cold
        // (non-silent) open of a tab, if a cached result exists, show it
        // right away instead of a blank spinner, then still fetch fresh
        // data in the background exactly as before. Silent (background
        // auto-refresh / tab-switch) calls are completely untouched.
        var hadCache = false
        if (!silent) {
            val cached = repository.loadCachedTab(stage, effectiveBranch())
            if (!cached.isNullOrEmpty()) {
                hadCache = true
                loadedItems = cached
                try { applySearch() } catch (_: Throwable) { }
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.visibility = View.GONE
            } else {
                binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
                // TK-REQUESTED (2026-07-24): only reachable on first-ever
                // open (no cache yet) -- plain "Loading..." instead of blank.
                binding.tvEmpty.text = "Loading..."
                binding.tvEmpty.visibility = View.VISIBLE
                setListVisible(false)
            }
        }
        val myTabToken = ++tabLoadToken
        val crashGuard = kotlinx.coroutines.CoroutineExceptionHandler { _, err ->
            // Absolute last resort: even an Error/Throwable on any thread must NOT close the app.
            runOnUiThread {
                try {
                    if (!silent) binding.progressLoad.visibility = View.GONE
                    stopPullSpinner()
                    if (silent || stage != currentStage || myTabToken != tabLoadToken) return@runOnUiThread // background/stale refresh: keep showing whatever is already on screen
                    // TK-REQUESTED (2026-07-20): a fresh-fetch failure after cached
                    // data was already shown should NOT wipe that data with an
                    // error message -- better a slightly-old list than a blank/
                    // error screen. Only show the error text when there was
                    // nothing cached to fall back on.
                    if (hadCache) return@runOnUiThread
                    loadedItems = emptyList()
                    setListVisible(false)
                    binding.progressLoad.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Follow-up opened, but data did not load\n(${err.javaClass.simpleName}: ${err.message})"
                } catch (_: Throwable) {}
            }
        }
        // খাতার সারি B40: লোড শুরু। ⛔ কোনো কারণে শেষ না হলেও যেন চিরকাল আটকে
        // না থাকে — ৯০ সেকেন্ড পরে চিহ্নটা নিজে থেকেই খুলে যায়, নইলে নিজে-নিজে
        // রিফ্রেশ আর কখনো চলত না।
        loadInFlight = true
        autoRefreshHandler.removeCallbacks(loadFlagRelease)
        autoRefreshHandler.postDelayed(loadFlagRelease, 90_000L)
        lifecycleScope.launch(crashGuard) {
            try {
                // 🔴🔒 V456 (20.08.2026, TK-অনুমোদিত পাইলট, ধাপ ১): শুধু silent
                // (auto-refresh) পথে, শুধু Inquiry ট্যাবে delta-fetch — বাকি সব
                // ক্ষেত্রে (প্রথম খোলা/tab-switch/Patient/Treatment ট্যাব) আগের
                // মতোই পূর্ণ fetchTab()।
                // 🚀🔒 V1531 — dedup: refreshTabCounts/loadTodayAllSections একই
                // মুহূর্তে এই একই ট্যাবের fetchTab() চালাচ্ছে থাকলে সেটার সাথেই
                // জোড়া লাগে, দুবার একই ভারী কাজ হয় না।
                /* 🚨🔒 V1539 (১৮.০৯.২০২৬, TK-রিপোর্ট, বারবার — "এক ট্যাব থেকে
                   অন্য ট্যাবে যেতে অনেক সময় লাগে")। fetchTabDelta()-এর নিজের
                   টীকাই (উপরে, ১৪৪৬-৪৭ লাইন) বলছে এটা **শুধু ২৫-সেকেন্ড
                   নিজে-নিজে-রিফ্রেশের জন্য**, ট্যাব-বদলে নয় — কিন্তু switchTab()
                   ও নিজে-নিজে-রিফ্রেশ দুটোই `loadTab(..., silent = true)`
                   ডাকত, তাই `loadTab()`-এর ভিতর থেকে দুটো আলাদা করার উপায়ই
                   ছিল না। ফল: ট্যাব বদলালেও delta-পথে যেত — "since" জমা না
                   থাকলে (বা ৩ ঘণ্টার পুরনো হলে) ভিতরেই একটা **পূর্ণ fetchTab()**
                   চালিয়ে তার উপরে আরও delta-জমানোর বাড়তি কাজ করত ⇒ সাধারণ
                   fetchTabDeduped()-এর চেয়েও বেশি সময় লাগত। এখন switchTab()
                   স্পষ্ট করে `fromTabSwitch = true` পাঠায়, তাই ট্যাব-বদল
                   সবসময় সরাসরি fetchTabDeduped() (পূর্ণ, dedup করা) ব্যবহার
                   করে — নিজে-নিজে-রিফ্রেশ (currentStage-এই থাকা) আগের মতোই
                   delta-পথে। */
                val items = withContext(Dispatchers.IO) {
                    if (silent && !fromTabSwitch) repository.fetchTabDelta(stage, effectiveBranch(), user.name, user.mobile)
                    else FollowUpRepository.fetchTabDeduped(repository, stage, effectiveBranch(), user.name, user.mobile).await()
                }
                // TK-REPORTED BUG FIX (2026-07-19): if the person switched tabs
                // (e.g. Enquiry -> Patient) while this fetch was still running --
                // or the 25-second silent background refresh for the OLD tab
                // finishes after the new tab's own fetch -- this result is for a
                // tab that is no longer the one on screen. Applying it anyway
                // used to overwrite the newly-selected tab with the old tab's
                // data (Patient tab showing Enquiry cards). Now any result whose
                // `stage` no longer matches `currentStage` is silently dropped.
                // TK-REPORTED BUG FIX (2026-07-20): the line above used to run
                // BEFORE hiding the spinner, so if the tab was switched while
                // this fetch was still in flight, this branch returned early
                // and the spinner THIS call had shown was never hidden -- it
                // stayed stuck spinning on screen forever, floating over the
                // next tab's cards. Hiding it first (unconditionally for the
                // call that showed it) fixes that, without touching which
                // tab's data actually gets applied below.
                if (!silent) binding.progressLoad.visibility = View.GONE
                stopPullSpinner()
                if (stage != currentStage || myTabToken != tabLoadToken) return@launch
                loadedItems = items
                // 🚨 খাতার সারি B31: চোখে-দেখা তালিকাটা এসে গেছে — এখন সরু লাইনটা
                // খালি, তাই ট্যাবের নতুন সংখ্যা আনার কাজ এখন শুরু হয়। খোলার সময়
                // চারটে ভারী কাজ একসাথে চলত, তাই দেখার জিনিসটাই দেরিতে আসত।
                // ⛔ সংখ্যা কোথা থেকে আসে বা কী দেখায় — কিছুই বদলায়নি, শুধু ক্রম।
                if (!silent && !tabCountsAsked) { tabCountsAsked = true; refreshTabCounts() }
                try { applySearch() } catch (t: Throwable) {
                    if (silent || myTabToken != tabLoadToken) return@launch // background refresh: don't disturb the visible list on a hiccup
                    loadedItems = emptyList()
                    setListVisible(false)
                    binding.progressLoad.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Could not show the list\n(${t.javaClass.simpleName}: ${t.message})"
                }
            } catch (e: Throwable) {
                // Never let a load failure crash the screen — show empty + the real reason.
                if (!silent) binding.progressLoad.visibility = View.GONE
                stopPullSpinner()
                if (silent || stage != currentStage || myTabToken != tabLoadToken) return@launch
                // TK-REQUESTED (2026-07-20): same as the crashGuard above -- keep
                // showing cached data on a fetch failure instead of overwriting
                // it with an error message.
                if (hadCache) return@launch
                loadedItems = emptyList()
                setListVisible(false)
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Data did not load — please retry\n(${e.javaClass.simpleName}: ${e.message})"
            }
        }
    }

    private var loadedItems: List<FollowUpItem> = emptyList()

    /** Web filterFollowRows(): filter the current tab by name or mobile digits. */
    // CRASH FIX: do NOT read intent here — this initializer runs during Activity
    // construction (before intent is attached), which was throwing NullPointerException
    // ("Unable to instantiate activity"). The real value is set in onCreate() instead.
    private var dateFilter: String = "All"
    private var customFrom: String = ""
    private var customTo: String = ""

    /* 🗓️🔒 (16.09.2026, TK photo-proof approved: "হ্যাঁ পাশ, বসিয়ে দিন") — the
       "👤 ... Patient" tab (screen label; internally stage="Treatment" — see
       the big top-of-file note and switchTab() below, TK's own words:
       "এভাবে এক জায়গায় এতগুলো পেশেন্টের ডিটেলস থাকলে বিভ্রান্ত হয়ে যাচ্ছে,
       বছর ভিত্তিক আলাদাভাবে করে রাখলে ভালো হবে"). Default = CURRENT year
       (TK: "default কিন্তু Recent Year ই থাকতে হবে"), switchable via the new
       "Year: 20XX ▾" row in the ⋮ overflow menu (showPatientYearPickerMenu()).
       ⛔ ADDITIONAL filter only, layered on top of the My Call/Today/Overdue/
          This Week chips below — never replaces them (applyDateFilter applies
          both, chips first then year).
       🗓️🔒 (17.09.2026, TK-নির্দেশ — "হ্যাঁ লাগবে তো, তবে কোন প্রকার ঝুঁকি নিতে
       চাই না") — এখন Enquiry ও Visit ট্যাবেও (stage="Inquiry"/"Patient") এই
       একই বছর-ছাঁকনি কাজ করে (আগে শুধু Patient/Treatment ট্যাবে হতো) —
       applyDateFilter()-এর শেষের শর্ত দ্রষ্টব্য। ⛔ একটাই ব্যতিক্রম, ইচ্ছে
       করে অটুট রাখা: Dashboard/Briefing-এর "আজকের কল" মিশ্র তালিকা
       (todayAllSections=true) কখনো বছর-ছাঁকনি মানে না — নইলে গত বছরের
       বকেয়া কল চুপচাপ হারিয়ে যেতে পারত। ⛔ যে বছরে কোনো তথ্য নেই, সেই
       বছর বাছলে honest "No records found" দেখাবে (ordered.isEmpty() পথ,
       নিচে) — পুরনো/ভুল তথ্য দেখাবে না। */
    private var patientYearFilter: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

    /** Registration/record year of a followup-card, whichever stage
     *  (Enquiry/Visit/Patient tab) it belongs to, or null if unknown.
     *  Prefers `regDate` (V851 — the real registration date, populated only
     *  for Treatment-stage rows), falls back to `recordDate` (the row's own
     *  `date` — populated for every stage, see FollowUpModel/FollowUpRepository)
     *  for Enquiry/Visit rows and any older/edge-case Treatment row. */
    private fun patientItemYear(item: FollowUpItem): Int? {
        val y = item.regDate.take(4).ifBlank { item.recordDate.take(4) }
        return y.toIntOrNull()
    }

    /** Real years present across all three tabs' already-fetched/cached data
     *  — no network call, enumerated from lastEnqAll/lastVisitAll/lastPatAll
     *  (the same lists the tabs' own count badges use). Current year is
     *  always included so switching back to it is always possible, even with
     *  zero records so far this year. */
    private fun availablePatientYears(): List<Int> {
        val fallback = when (currentStage) { "Inquiry" -> lastEnqAll; "Patient" -> lastVisitAll; else -> lastPatAll }
            ?: loadedItems
        val src = (lastEnqAll.orEmpty() + lastVisitAll.orEmpty() + lastPatAll.orEmpty()).ifEmpty { fallback }
        val years = src.mapNotNull { patientItemYear(it) }.toMutableSet()
        years.add(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR))
        return years.sortedDescending()
    }

    private fun applyDateFilter(items: List<FollowUpItem>, stage: String = currentStage): List<FollowUpItem> {
        val chipFiltered = if (dateFilter == "All") items else {
        val today = FollowUpModel.today()
        when (dateFilter) {
            // 🔒 TK-ORDER (31.07.2026, খাতার সারি B213 — TK: "সেই স্টাফ শেষ যার
            // সাথে কথা বলেছিলেন (last caller) তারাই, তারিখ যাই হোক")। ⛔ নতুন
            // কোনো ক্লাউড-কল/কলাম লাগেনি — `item.lastCallBy` আগে থেকেই প্রতিটা
            // রেকর্ডে পার্স হয় (history-র সবচেয়ে নতুন এন্ট্রির স্টাফ-নাম,
            // খাতার সারি B39)। তারিখের কোনো শর্ত নেই — TK-এর স্পষ্ট কথামতো।
            "My Call" -> items.filter { it.lastCallBy.trim().equals(user.name.trim(), ignoreCase = true) }
            // 🔴🔴🔒 খাতার সারি B454 (TK-নির্দেশ, 05.08.2026, ধাপে ধাপে
            // জিজ্ঞাসা করে নিশ্চিত হয়ে — "আজ যারা ভিজিট/রেজিস্ট্রেশন
            // করলেন তাঁরাও Today ট্যাবে দেখাতে হবে, কল ডিউ থাকুক বা না
            // থাকুক")। আগে শুধু `nextFollow == today` (আজ কল ডিউ) দেখাত —
            // তাই আজ রেজিস্ট্রেশন করা রোগী (যাঁর এখনো কোনো ফলো-আপ তারিখ
            // বসানোই হয়নি) এখানে দেখাত না, যদিও "All"-এ ঠিকই দেখাত। এখন
            // দুটো শর্তের **যেকোনো একটা** মিললেই দেখাবে — কল ডিউ আজ, বা
            // রেকর্ডটাই আজ তৈরি হয়েছে (Visit/Registration)।
            // 🔴 V511 (২১.০৮.২০২৬, TK-অনুমোদিত): ড্যাশবোর্ডের "calls pending
            //    today" ব্যানার থেকে এলে **শুধু আজ কল করার কথা** যাঁদের —
            //    ব্যানারের সংখ্যা আর ভিতরের তালিকা তখন হুবহু এক (উপরের
            //    `bannerCallsOnly`-র বড় নোট দ্রষ্টব্য)। অন্য যেকোনো ভাবে
            //    খুললে নিচের ০৫.০৮.২০২৬-এর নিয়মই আগের মতো চলে।
            /* 🟢🔒 V590 (২৩.০৮.২০২৬, TK-রিপোর্ট: *"এতগুলো ওভারডিউ রয়েছে · স্টাফদের
               কাছে কি নোটিফিকেশন যায় না?"*)
               **যাচাই করে পাওয়া আসল কারণ:** মনে-করানো ও ড্যাশবোর্ডের ব্যানার —
               দুটোই শুধু **ঠিক আজকের তারিখের** কল গুনত। একদিন কল বাদ পড়লে
               সেটা পরদিন থেকেই দুই জায়গা থেকেই **অদৃশ্য** হয়ে যেত, তাই স্টাফ
               আর কোনোদিন মনে করানো পেতেন না।
               ⇒ ব্যানার/নোটিফিকেশন থেকে এলে এখন **আজকের + বকেয়া** দুটোই আসে।
               ⛔ উপরের "Today" বোতামটা স্টাফ নিজে চাপলে আগের মতোই শুধু আজকের
                  (নিচের `else` শাখা) — সেটা এক অক্ষরও বদলায়নি। */
            /* 📵 V1206 — "আর কল লাগবে না" বলা সারি কল-তালিকায় আর আসে না।
               ⛔ শুধু এই দুটো কল-তালিকায় (আজকের ও বকেয়া); অন্য কোনো ট্যাব,
                  খোঁজা বা গোনা ছোঁয়া হয়নি — সারিটা কোথাও হারায় না। */
            /* 📞 V1403 — "ওই তারিখে/পরে কল হয়ে গেলে আর বাকি নয়" (FollowUpModel.callPending) */
            "Today" -> if (bannerCallsOnly) items.filter { FollowUpModel.callPending(it, today) }
                       else items.filter { !it.noMoreCalls && ((it.nextFollow == today && !FollowUpModel.alreadyCalled(it.nextFollow, it.lastCallDate)) || it.recordDate == today) }
            /* 🟢🔒 V692 — সাধারণ Overdue আগের মতোই। শুধু Briefing-এর ⚠️ Overdue
               Follow-up Alert-এর View থেকে এলে **৩+ দিন** পেরোনোগুলোই —
               DashboardActivity যে হিসাবে নোটিশের সংখ্যাটা বানায়
               (`nextFollow <= threeDaysAgo`), হুবহু সেই একই হিসাব, যাতে
               নোটিশের সংখ্যা আর এই তালিকা কখনো আলাদা না হয়। */
            "Overdue" -> if (overdue3PlusOnly) {
                val threeDaysAgo = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date(System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000))
                items.filter { FollowUpModel.callPending(it, today) && it.nextFollow <= threeDaysAgo }   // 📞 V1403
            } else items.filter { FollowUpModel.callPending(it, today) && it.nextFollow < today }   // 📞 V1403
            "This Week" -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val start = fmt.format(cal.time)
                cal.add(java.util.Calendar.DAY_OF_YEAR, 6)
                val end = fmt.format(cal.time)
                items.filter { it.nextFollow.isNotBlank() && it.nextFollow in start..end }
            }
            "This Month" -> {
                val ym = today.substring(0, 7)
                items.filter { it.nextFollow.startsWith(ym) }
            }
            "Custom" -> {
                if (customFrom.isBlank() || customTo.isBlank()) items
                else items.filter { it.nextFollow.isNotBlank() && it.nextFollow in customFrom..customTo }
            }
            // 🔒 খাতার সারি B68: সিরিয়ালে তারিখের কোনো বাছাই নেই — ওই ভাগের
            // **সবাই** থাকবে, শুধু সাজানোর ক্রমটা নিচে বদলায়।
            "Serial" -> items
            // 🟢🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ) — Draft-এর "Unexpected Time
            // Calls"-এর সাথে মেলানো একই ছাঁকনি: শুধু timeType="Unexpected
            // Time" রেকর্ড। Enquiry ট্যাবেই কার্যকর (item.timeType Enquiry-
            // stage-এর জন্যই বসে); অন্য ট্যাবে ফাঁকা থাকায় এমনিতেই কিছু
            // দেখাবে না। ⛔ নতুন কোনো Supabase কল নেই — client-side ফিল্টার।
            "Unexpected" -> items.filter { it.timeType.equals("Unexpected Time", ignoreCase = true) }
            // 🟢🔒 V646 (২৫.০৮.২০২৬, TK-নির্দেশ — "২ যায়গায়ই থাকবে") — Patient
            // ট্যাব (stage="Treatment") ইতিমধ্যেই শুধু Treatment-stage রো
            // দেখায়, তাই এখানে stage আলাদা করে ছাঁকতে হয় না। নিয়ম Draft-এর
            // V644/V645-এর সাথে হুবহু মেলানো (ফ্রি-প্ল্যান-নিরাপদ, client-side)।
            "Complete" -> items.filter { it.bill > 0.0 && (it.bill - it.paid) <= 0.0 }
            "Incomplete" -> items.filter {
                val ageDays = try {
                    val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it.recordDate.take(10))
                    if (d == null) 0 else ((java.util.Date().time - d.time) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
                } catch (_: Throwable) { 0 }
                it.paid <= 0.0 && ageDays >= 60
            }
            "Running" -> items.filter {
                val complete = it.bill > 0.0 && (it.bill - it.paid) <= 0.0
                val ageDays = try {
                    val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it.recordDate.take(10))
                    if (d == null) 0 else ((java.util.Date().time - d.time) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
                } catch (_: Throwable) { 0 }
                val incomplete = it.paid <= 0.0 && ageDays >= 60
                !complete && !incomplete
            }
            else -> items
        }
        }
        // 🗓️🔒 (16.09.2026, extended 17.09.2026 — TK-নির্দেশ) — ADDITIONAL year
        // filter, now all three tabs (Enquiry/Visit/Patient — stage="Inquiry"/
        // "Patient"/"Treatment"). Unknown-year cards (blank regDate AND blank
        // recordDate — very old/edge-case rows) are never hidden by this
        // filter, so a pending follow-up call can never silently disappear
        // just because its year couldn't be determined.
        // ⛔ todayAllSections (Dashboard/Briefing's mixed "today's calls"
        // banner list) is deliberately EXEMPT — that list mixes all three
        // stages together to show every due/overdue call regardless of
        // registration year; year-filtering it could hide a genuinely
        // pending call from a previous year (web's __wlv1SkipPatientYearFilter
        // guards the exact same case, rule ৮).
        return if (!todayAllSections && (stage == "Treatment" || stage == "Inquiry" || stage == "Patient")) {
            chipFiltered.filter { val y = patientItemYear(it); y == null || y == patientYearFilter }
        } else chipFiltered
    }

    /**
     * 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B65)
     *
     * TK: *"RMP-এ যেমন প্রতিটা ডাক্তারের নামের আগে সিরিয়াল নাম্বার আছে, এখানেও
     * পেশেন্টের নামের আগে... সিরিয়াল নাম্বার দেবেন। সুন্দরভাবে সিরিয়াল মেন্টেন
     * হবে।"* · *"সিরিয়াল নাম্বার প্রতি ব্রাঞ্চে আলাদা।"*
     *
     * নিয়ম (TK-এর কথা অনুযায়ী):
     *  • **প্রতি ব্রাঞ্চে আলাদা** — Cooch Behar-এ ১,২,৩… Jalpaiguri-তেও ১,২,৩…
     *  • **তিন ভাগে আলাদা** — Enquiry-র নিজের, Visit-এর নিজের, Patient-এর নিজের।
     *    অ্যাডভান্স দিয়ে কেউ Patient-এ গেলে সেখানে **নতুন নম্বর** পাবে; Visit-এর
     *    পুরনো নম্বরের সঙ্গে কোনো সম্পর্ক থাকবে না।
     *  • **সবচেয়ে পুরনো = ১**, তারপর পরপর — কখনো ফাঁক পড়ে না।
     *  • **ছাঁকনি (Today/Overdue/This Week) বা খোঁজায় নম্বর বদলায় না** — গোনা হয়
     *    সবসময় ওই ভাগের **পুরো তালিকা** ধরে।
     *
     * ⛔ ক্রম ধরা হয় **সারিটা কবে তৈরি হয়েছিল (`createdAt`)** তা দিয়ে, `date`
     *    দিয়ে নয় — কারণ টাকা নিলেই `date` আজকের হয়ে যায় (কার্ড উপরে তোলার
     *    জন্য), ফলে নম্বর বারবার লাফাত। এখন নম্বর একবার বসলে আর নড়ে না।
     * ⛔ **ক্লাউডে একটাও বাড়তি অনুরোধ নেই** — আগেই নামানো তালিকা থেকেই গোনা।
     * ⛔ ডেটাবেসে কিছু লেখা হয় না।
     */
    /**
     * 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B69)
     *
     * TK: *"গুগল সিট — অর্থাৎ আমি ডাউনলোড করলে যেন গুগল সিটে দেখতে পাই।"*
     *
     * পর্দায় **তখন যা দেখা যাচ্ছে ঠিক তা-ই** CSV হয়ে নামে — যে ট্যাব খোলা,
     * যে ব্রাঞ্চ বাছা, যে ছাঁকনি চালু, খোঁজার লেখা সহ, আর **একই ক্রমে**।
     * তারপর ফোনের চেনা "Share/Save" পর্দা খোলে — সেখান থেকে Drive/Downloads-এ
     * রাখা যায় বা WhatsApp/Gmail-এ পাঠানো যায়।
     *
     * ⛔ **Patient ভাগে শুধু একবার** টাকার তালিকা আনা হয় (প্রথম অ্যাডভান্সের
     *    তারিখের জন্য); Enquiry/Visit-এ **ক্লাউডে একটাও বাড়তি অনুরোধ নেই**।
     * ⛔ ডেটাবেসে কিছু লেখা হয় না · নতুন টেবিল/কলাম/SQL কিছুই লাগে না।
     * ⛔ দুবার চাপ পড়লেও একবারই চলে (`sheetBusy`)।
     */
    /** একটা রোগীর `disease` ঘরে একাধিক রোগ কমা দিয়ে জোড়া থাকতে পারে (V1000,
     *  যেমন "Piles, Fissure") — তাই হুবহু-মিল নয়, তালিকার ভিতরে আছে কিনা দেখা
     *  হয়। একজন দুই রোগেই থাকলে দুটো ডাউনলোডেই আসবেন — রিপোর্টের জন্য এটাই ঠিক। */
    private fun matchesDiseaseFilter(item: FollowUpItem, target: String): Boolean {
        if (target == "All") return true
        return item.disease.split(",").map { it.trim() }.any { it.equals(target, ignoreCase = true) }
    }

    /** 📄🔒 V1623 (২০.০৯.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ পাশ) — Sheet ডাউনলোডের
     *  আগে রোগ (Piles/Fissure/Fistula/Hydrocele/Gupt Rog/Other) ও এনকোয়ারি/
     *  রেজিস্ট্রেশনের তারিখ-সীমা (From/To, `recordDate`) বেছে নেওয়ার পপ-আপ।
     *  ⛔ বর্তমান ট্যাব (Enquiry/Visit/Patient) ও ব্রাঞ্চ-অনুমতি (branchSafeItems,
     *  V509) অপরিবর্তিত — শুধু তার উপরে এই দুটো দিয়ে আরেকটু ছেঁকে ডাউনলোড হয়।
     *  ⛔ স্ক্রিনের উপরের সার্চ/Custom-ফিল্টার (ওটা `nextFollow` ধরে) এখানে
     *  ছোঁয়া হয়নি — TK স্পষ্ট বলেছেন এনকোয়ারি/রেজিস্ট্রেশনের তারিখ (নিয়ম ৫)। */
    private fun showDownloadSheetOptions() {
        if (sheetBusy) return
        val all = branchSafeItems()
        if (all.isEmpty()) {
            Toast.makeText(this, "Nothing to download — the list is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val diseaseOptions = listOf("All", "Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog", "Other")
        var pickedDisease = "All"
        var fromIso: String? = null
        var toIso: String? = null
        val fmtIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fmtShow = SimpleDateFormat("dd/MM/yyyy", Locale.US)

        fun inRange(item: FollowUpItem): Boolean {
            val d = item.recordDate
            if (fromIso != null && (d.isBlank() || d < fromIso!!)) return false
            if (toIso != null && (d.isBlank() || d > toIso!!)) return false
            return true
        }
        fun countFor(disease: String) = all.count { inRange(it) && matchesDiseaseFilter(it, disease) }

        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(px(20), px(14), px(20), 0)
        }

        val dateRow = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
        fun makeDateField(labelText: String): android.widget.TextView {
            val col = android.widget.LinearLayout(this@FollowUpActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = px(6)
                }
            }
            col.addView(android.widget.TextView(this@FollowUpActivity).apply {
                text = labelText; textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#6A5320"))
            })
            val field = android.widget.TextView(this@FollowUpActivity).apply {
                text = "Any date"; textSize = 13.5f
                setTextColor(android.graphics.Color.parseColor("#145A32"))
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
                setPadding(px(10), px(10), px(10), px(10))
            }
            col.addView(field)
            dateRow.addView(col)
            return field
        }
        val fromField = makeDateField("FROM DATE (Enquiry/Reg.)")
        val toField = makeDateField("TO DATE")
        container.addView(dateRow)

        val hint = android.widget.TextView(this).apply {
            text = "Which disease should this sheet include?"; textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#4A5568"))
            setPadding(0, px(14), 0, px(6))
        }
        container.addView(hint)

        val rows = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL }
        container.addView(rows)
        val rowViews = HashMap<String, Pair<android.widget.LinearLayout, android.widget.TextView>>()

        fun paintRow(disease: String) {
            val (row, countLabel) = rowViews[disease] ?: return
            val on = disease == pickedDisease
            row.setBackgroundColor(android.graphics.Color.parseColor(if (on) "#EAFBF0" else "#FFFFFF"))
            (row.getChildAt(0) as? android.widget.TextView)?.let {
                it.setTextColor(android.graphics.Color.parseColor(if (on) "#0C9E33" else "#145A32"))
            }
            countLabel.text = countFor(disease).toString()
        }
        fun paintAll() { diseaseOptions.forEach { paintRow(it) } }

        diseaseOptions.forEach { disease ->
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(px(14), px(12), px(14), px(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE)
                    cornerRadius = px(10).toFloat()
                    setStroke(px(1), android.graphics.Color.parseColor("#D9E0E7"))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = px(8) }
            }
            val nameLabel = android.widget.TextView(this).apply {
                text = if (disease == "All") "All Diseases" else disease
                textSize = 14.5f; setTextColor(android.graphics.Color.parseColor("#145A32"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val countLabel = android.widget.TextView(this).apply {
                textSize = 12.5f; setTextColor(android.graphics.Color.parseColor("#6B7686"))
            }
            row.addView(nameLabel); row.addView(countLabel)
            row.setOnClickListener { pickedDisease = disease; paintAll() }
            rowViews[disease] = row to countLabel
            rows.addView(row)
        }
        paintAll()

        fun pickDate(target: android.widget.TextView, applyTo: (String?) -> Unit) {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d) }
                val iso = fmtIso.format(picked.time)
                applyTo(iso)
                target.text = fmtShow.format(picked.time)
                paintAll()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        fromField.setOnClickListener { pickDate(fromField) { fromIso = it } }
        toField.setOnClickListener { pickDate(toField) { toIso = it } }

        val scroll = android.widget.ScrollView(this).apply { addView(container) }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "📄 Download Sheet"))
            .setView(scroll)
            .setPositiveButton("Download") { _, _ ->
                val filtered = all.filter { inRange(it) && matchesDiseaseFilter(it, pickedDisease) }
                if (filtered.isEmpty()) {
                    Toast.makeText(this@FollowUpActivity, "Nothing to download for this disease/date range", Toast.LENGTH_LONG).show()
                } else {
                    downloadSheet(filtered)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun downloadSheet(rows: List<FollowUpItem> = shownItems) {
        if (sheetBusy) return
        if (rows.isEmpty()) {
            Toast.makeText(this, "Nothing to download — the list is empty", Toast.LENGTH_SHORT).show()
            return
        }
        sheetBusy = true
        Toast.makeText(this, "Preparing sheet…", Toast.LENGTH_SHORT).show()
        val stage = currentStage
        val serials = serialByItemId
        lifecycleScope.launch {
            // 🔴🔒 V1620 (২০.০৯.২০২৬, TK-রিপোর্ট) — TK: "high speed internet
            // থাকা সত্ত্বেও এরকম কেন দেখাবে"। যাচাই করে দেখা গেল: Enquiry/Visit
            // ভাগে (Patient ছাড়া) এই কাজে ইন্টারনেট **একবারও লাগে না** — পুরোটাই
            // ফোনের নিজের জায়গায় ফাইল বানানো। তাই ব্যর্থ হলে "connection"-কে দোষ
            // দেওয়াটাই ভুল বার্তা ছিল — আসল কারণ (যেমন ফোনের জায়গা শেষ) ধরাই
            // পড়ত না, কারণ এতদিন ভুলটা চুপচাপ গিলে ফেলা হতো। এখন আসল কারণ
            // (Exception-এর নিজের কথা) সরাসরি দেখানো হয়, যাতে পরের বার সত্যিই
            // কী আটকাল তা বোঝা যায়।
            var failReason = ""
            val file = withContext(Dispatchers.IO) {
                try {
                    val firstAdvance: Map<String, String> = if (stage == "Treatment") {
                        // ওই তালিকায় যে ব্রাঞ্চগুলো আছে শুধু সেগুলোর টাকার সারি —
                        // একটাই অনুরোধ।
                        val branches = rows.map { it.branch.trim() }.filter { it.isNotBlank() }.distinct()
                        val filter = if (branches.size == 1)
                            "branch=eq." + java.net.URLEncoder.encode(branches[0], "UTF-8") else null
                        // শুধু যেটুকু দরকার সেটুকুই নামানো হয় — কম তথ্য, কম খরচ।
                        val pay = SupabaseClient.fetchListSlim(
                            "payments", filter, 5000,
                            cols = "mobile,patientCode,date,payType,remarks"
                        )
                        FollowUpSheetExporter.firstAdvanceDates(pay)
                    } else emptyMap()
                    FollowUpSheetExporter.write(this@FollowUpActivity, stage, rows, serials, firstAdvance)
                } catch (t: Throwable) {
                    failReason = t.javaClass.simpleName + (t.message?.let { ": $it" } ?: "")
                    null
                }
            }
            sheetBusy = false
            if (file == null || !file.exists()) {
                val why = failReason.ifBlank { "file not created" }
                Toast.makeText(this@FollowUpActivity, "Could not make the sheet — $why", Toast.LENGTH_LONG).show()
                return@launch
            }
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    this@FollowUpActivity, "$packageName.fileprovider", file
                )
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, file.name)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(send, "Save / Send sheet"))
            } catch (_: Throwable) {
                Toast.makeText(this@FollowUpActivity, "Sheet made, but this phone could not open the share window", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun rebuildSerials() {
        val map = HashMap<String, Int>()
        try {
            loadedItems.groupBy { it.branch.trim().uppercase() }.forEach { entry ->
                entry.value
                    .sortedWith(compareBy<FollowUpItem>({ serialOrderKey(it) }, { it.id }))
                    .forEachIndexed { idx, row ->
                        if (row.id.isNotBlank()) map[row.id] = idx + 1
                    }
            }
        } catch (_: Throwable) { }
        serialByItemId = map
    }

    /** সবচেয়ে পুরনো আগে — সারি তৈরির সময় ধরে; না থাকলে তারিখ ধরে। */
    private fun serialOrderKey(item: FollowUpItem): String =
        item.createdAt.ifBlank { item.recordDate }.ifBlank { "9999-99-99" }

    // ──────────────────────────────────────────────────────────────────────
    // 🔵 TK (10.08.2026): "কাল আসার কথা" → Follow-Up রিডাইরেক্ট।
    // ওই কার্ডে শুধু নাম+নম্বর থাকে, তাই ইনি কোন সেকশনে (Enquiry/Visit/Patient)
    // বোঝা যায় না। কার্ডে চাপলে এই পর্দা খোলে নম্বরটা নিয়ে; নিচের সাহায্যকারীরা
    // ঠিক সেকশনটা বের করে সেই ট্যাবে সরে যায়, কার্ডটা উপরে এনে অল্প সময়ের জন্য
    // হাইলাইট করে। ⛔ বাড়তি কোনো ক্লাউড-কল নেই — ট্যাবের সংখ্যা আনতে যে তিনটে
    // তালিকা এমনিতেই আসে (refreshTabCounts) সেগুলোই ব্যবহার হয়।
    private fun mob10(raw: String): String = raw.filter { it.isDigit() }.takeLast(10)

    private fun stageContains(list: List<FollowUpItem>?, target: String): Boolean =
        list?.any { mob10(it.mobile) == target } == true

    /** খোলার সময় কোন ট্যাব দিয়ে শুরু হবে — ক্যাশে ওই নম্বর যে সেকশনে পাওয়া যায়
     *  সেটাই; না পেলে স্বাভাবিক "Inquiry"। */
    private fun initialFocusStage(): String {
        /* 🔵🔒 V523 (২২.০৮.২০২৬, TK-নির্দেশ): Reports-এর উপরের বাক্সে চাপ দিলে
           সোজা ঠিক ট্যাবেই আসা যায় (Enquiry / Visit / Patient)।
           ⛔ ঘরটা না এলে (পুরোনো সব ডাক) নিচের আচরণ **অবিকল আগের মতোই**। */
        val asked = intent.getStringExtra("startStage").orEmpty()
        if (asked == "Inquiry" || asked == "Patient" || asked == "Treatment") return asked
        if (pendingFocusCardMobile.isBlank()) return "Inquiry"
        val target = mob10(pendingFocusCardMobile)
        if (target.length != 10) return "Inquiry"
        val br = effectiveBranch()
        return when {
            stageContains(try { repository.loadCachedTab("Inquiry", br) } catch (_: Throwable) { null }, target) -> "Inquiry"
            stageContains(try { repository.loadCachedTab("Patient", br) } catch (_: Throwable) { null }, target) -> "Patient"
            stageContains(try { repository.loadCachedTab("Treatment", br) } catch (_: Throwable) { null }, target) -> "Treatment"
            else -> "Inquiry"
        }
    }

    /** তিনটে তাজা তালিকা এলে যেটায় নম্বরটা আছে সেই ট্যাবে সরিয়ে কার্ডটা ফোকাস
     *  করে। তিনটেই এসে গেলেও কোথাও না পেলে চেষ্টা থামায় (ছোট বার্তা দিয়ে)। */
    private fun maybeFocusFromLists() {
        if (pendingFocusCardMobile.isBlank()) return
        val target = mob10(pendingFocusCardMobile)
        if (target.length != 10) { pendingFocusCardMobile = ""; return }
        val stage = when {
            stageContains(lastEnqAll, target) -> "Inquiry"
            stageContains(lastVisitAll, target) -> "Patient"
            stageContains(lastPatAll, target) -> "Treatment"
            else -> null
        }
        if (stage != null) {
            if (currentStage != stage) switchTab(stage)
            binding.rvList.post { tryFocusScroll() }
        } else if (lastEnqAll != null && lastVisitAll != null && lastPatAll != null) {
            pendingFocusCardMobile = ""
            try { Toast.makeText(this, "Not found in the Follow-Up list", Toast.LENGTH_SHORT).show() } catch (_: Throwable) { }
        }
    }

    /** চলতি তালিকায় নম্বরটা থাকলে সেই কার্ডে স্ক্রল করে হাইলাইট করে — একবারই। */
    private fun tryFocusScroll() {
        if (pendingFocusCardMobile.isBlank()) return
        val target = mob10(pendingFocusCardMobile)
        if (target.length != 10) { pendingFocusCardMobile = ""; return }
        val pos = shownItems.indexOfFirst { mob10(it.mobile) == target }
        if (pos < 0) return
        pendingFocusCardMobile = ""
        try {
            (binding.rvList.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)
                ?.scrollToPositionWithOffset(pos, dpx(6))
        } catch (_: Throwable) { }
        binding.rvList.postDelayed({ highlightCardAt(pos) }, 300L)
    }

    /* 🏷️🔴🔒 V1254 (০৯.০৯.২০২৬, TK-রিপোর্ট ও ফটো-প্রুফ পাশ — খাতার সারি ৩৭৫)।
       TK: *"কি নামে Enquiry সেটা দেখার জন্য যখন চাপ দেই তখন বুঝতে পারি না কোন
       নম্বরটা Enquiry-তে নতুন এসেছে"*।

       🔴 **আসল কারণ (কোডে মেপে, আন্দাজ নয়):** হাইলাইটটা **হত**ই — কিন্তু
          হালকা হলুদ রংটা বসত কার্ডের **পিছনের মোড়কে** (`box`), আর কার্ডের
          নিজের সাদা পটভূমি সেটা প্রায় পুরোটাই ঢেকে দিত; তার উপর থাকত মাত্র
          **২.২ সেকেন্ড**। তাই TK চোখেই দেখতে পেতেন না।

       ⇒ এখন মোড়কটায় ৪dp ফাঁক রেখে **কমলা বেড় + হালকা হলুদ** বসে, তাই দাগটা
         কার্ডের চারপাশে স্পষ্ট দেখা যায়; উপরে ছোট একটা ট্যাগও বসে
         ("NEW ENQUIRY" / "NEW REGISTRATION")। TK-র সিদ্ধান্ত: থাকবে
         **৬ সেকেন্ড** (*"৬ সেকেন্ডই রাখুন"*), তারপর নিজে থেকেই মিলিয়ে যায়।
       ⛔ কার্ডের ভিতরের কিছুই বদলায়নি — লেখা · রং · বোতাম · হিসাব সব আগের।
       ⛔ ট্যাগের লেখাটা নোটিশ থেকেই আসে; না এলে ট্যাগ বসে না (বানানো নয়)।
       ⛔ ৬ সেকেন্ডের মধ্যে তালিকা সরে গেলেও কিছু আটকে থাকে না — কার্ড আবার
          বসানোর সময় (`onBindViewHolder`) মোড়কের সাজ ও ফাঁক দুটোই মুছে যায়। */
    private fun highlightCardAt(pos: Int) {
        try {
            val holder = binding.rvList.findViewHolderForAdapterPosition(pos) as? FollowCardHolder ?: return
            val box = holder.box
            val pad = dpx(4)
            box.setPadding(pad, pad, pad, pad)
            box.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#FFF3CD"))
                cornerRadius = dpx(16).toFloat()
                setStroke(dpx(2), android.graphics.Color.parseColor("#F59E0B"))
            }
            var tag: android.widget.TextView? = null
            val tagText = focusCardTag.trim()
            focusCardTag = ""
            if (tagText.isNotBlank()) {
                tag = android.widget.TextView(this).apply {
                    text = tagText
                    textSize = 9.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.WHITE)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#F59E0B"))
                        cornerRadius = dpx(6).toFloat()
                    }
                    setPadding(dpx(8), dpx(2), dpx(8), dpx(2))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dpx(4) }
                }
                box.addView(tag, 0)
            }
            val chip = tag
            box.postDelayed({
                try {
                    box.background = null
                    box.setPadding(0, 0, 0, 0)
                    if (chip != null && chip.parent === box) box.removeView(chip)
                } catch (_: Throwable) { }
            }, 6000L)
        } catch (_: Throwable) { }
    }

    /** V509-এর অনুমোদিত-অতিরিক্ত-ব্রাঞ্চ নিয়মটা একটাই জায়গায় — applySearch()
     *  আর নিচের disease-sheet পপ-আপ দুটোই এখান থেকেই ডাকে, যাতে দুই জায়গায়
     *  আলাদা করে লেখা ভবিষ্যতে একে অন্যের থেকে সরে না যায় (নিয়ম ৭ক-১)। */
    private fun branchSafeItems(): List<FollowUpItem> = if (user.role == "master") loadedItems else {
        val allowed = CrossBranchStaffAccess.effectiveViewBranch(user)
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (allowed.isEmpty() || allowed.any { it.equals("All", ignoreCase = true) }) loadedItems
        else loadedItems.filter { item ->
            val b = item.branch.trim()
            allowed.any { a ->
                b.equals(a, ignoreCase = true) ||
                    (b.isBlank() && run {
                        val code = item.patientId.substringBefore('-').trim()
                        code.length == 3 && code.equals(PatientIdGenerator.branchCode(a), ignoreCase = true)
                    })
            }
        }
    }

    private fun applySearch() {
        rebuildSerials()
        val q = binding.etSearch.text?.toString()?.trim()?.lowercase() ?: ""
        val qDigits = q.filter { it.isDigit() }
        // TK-ORDER (2026-07-25, Falakata staff was seeing other branches):
        // last line of defence. Whatever any repository/fallback/cache may
        // hand over, a non-Master user's screen shows ONLY their own branch.
        // Master (branch picker) is untouched.
        //
        // 🔴🔴🔴 V509 (২১.০৮.২০২৬, TK-রিপোর্ট — জলপাইগুড়ি স্টাফ, ছবিসহ):
        // *"আমি গুনে দেখলাম এখানে আমার পেশেন্ট সংখ্যা ৪৭ জন, কিন্তু উপরে
        //   পেশেন্ট সংখ্যার ফিগার দেখাচ্ছে ৫৬ জন — এটা কেন হচ্ছে?"*
        //
        // ─── আসল কারণ (কোড ধরে প্রমাণিত, অনুমান নয়) ────────────────────────
        // উপরের **সংখ্যা** আর নিচের **তালিকা** দুই আলাদা নিয়মে ছাঁকা হত —
        //   • সংখ্যা  : repository-র `branchAllows()` — যেটা V453-এর অনুমোদিত
        //               **অতিরিক্ত ব্রাঞ্চও** (JPE-CRP-এর জন্য Falakata +
        //               Birpara) হিসাবের মধ্যে ধরে।
        //   • তালিকা : ঠিক এই লাইনটা — শুধু `user.branch`-এর সঙ্গে **হুবহু**
        //               মিলিয়ে, তাই অনুমোদিত ঐ কার্ডগুলোই **ছেঁটে ফেলত**।
        // ⇒ একই ফোনে উপরে ৫৬, নিচে ৪৭। TK-এর V453 নির্দেশ ছিল *"JPE-CRP যেন
        //   FALAKATA & BIRPARA-র সমস্ত ডিটেইলস দেখতে পারে"* — তালিকায় সেটা
        //   মানা হচ্ছিল না।
        //
        // ─── ⛔ পাহারা এক চুলও আলগা হয়নি ───────────────────────────────────
        // অনুমোদিত ব্রাঞ্চের তালিকাটা আসে `CrossBranchStaffAccess` থেকে, যেটা
        // মোবাইল-নম্বর ধরে **TK-এর নিজের অনুমোদন ছাড়া কাউকে কিছুই** বাড়িয়ে
        // দেয় না। যাঁর অতিরিক্ত অনুমতি নেই, তাঁর জন্য ফল **অবিকল আগের মতোই**।
        // ⛔ টাকা/Payment-এর নিয়ম এই ফাইল ছোঁয় না — সেটা `MoneyBranchGuard`-এ,
        //    আর সেটা `user.branch`-ই দেখে। তাই দেখতে পেলেও অন্য ব্রাঞ্চের টাকা
        //    নেওয়া আগের মতোই বন্ধ থাকে।
        //
        // ⚠️ সৎ সীমা: এতেও সংখ্যা ও তালিকা **সবসময় হুবহু মিলবে এমন নয়** —
        //    repository আরও দু-একটা কারণে সারি রাখে (যেমন "যিনি নিজে তৈরি
        //    করেছেন" তাঁর নিজের সারি)। সেই পুরোনো আচরণ এখানে বদলানো হয়নি,
        //    কারণ ওতে হাত দিলে অন্য ব্রাঞ্চের তথ্য পর্দায় আসার ঝুঁকি থাকত।
        //    এই সংশোধনে **সবচেয়ে বড় ফাঁকটা** (অনুমোদিত অতিরিক্ত ব্রাঞ্চ ও
        //    ব্রাঞ্চ-ফাঁকা সারি) বন্ধ হলো।
        val branchSafe = branchSafeItems()
        val base = applyDateFilter(branchSafe)
        // TK-REQUESTED ADDITION (2026-07-24): search now also matches
        // Address, Patient ID, and the record's own date (Enquiry date on
        // the Enquiry tab, Registration date on Visit/Patient tabs -- same
        // recordDate field, already correctly stage-specific) -- was
        // name/disease/branch/mobile only before.
        val shown = if (q.isBlank()) base else base.filter {
            val text = "${it.name} ${it.disease} ${it.branch} ${it.address} ${it.patientId} ${it.recordDate}".lowercase()
            val digits = it.mobile.filter { c -> c.isDigit() }
            text.contains(q) || (qDigits.isNotEmpty() && digits.contains(qDigits))
        }
        // 🔒 TK-APPROVED (29.07.2026 · খাতার সারি B68): "Serial No." চাপলে তালিকা
        // সাজে **ব্রাঞ্চ ধরে ধরে, তারপর সিরিয়াল ১,২,৩…**।
        // TK: *"মাস্টার যদি অল করে সে ক্ষেত্রে ব্রাঞ্চ ভিত্তিক আলাদা আলাদা ভাবে
        // দেখাবে — জলপাইগুড়ির ১ থেকে ১০০, তারপর কোচবিহারের ১ থেকে ২০০।"*
        // ⛔ এটা শুধু **দেখানোর ক্রম** — নম্বর নিজে বদলায় না, তিন ভাগেরই নিজের
        //    নিজের সিরিয়াল আলাদা থাকে।
        val ordered = when {
            dateFilter == "Serial" -> shown.sortedWith(
                compareBy<FollowUpItem>(
                    { it.branch.trim().uppercase() },
                    { serialByItemId[it.id] ?: Int.MAX_VALUE }
                )
            )
            currentStage == "Inquiry" -> sortEnquiryByUrgency(shown)
            else -> shown
        }
        // 🔒 খাতার সারি B69: "Sheet" বোতামে চাপলে ঠিক **এই তালিকাটাই, এই ক্রমেই**
        // নামবে — যে ট্যাব · যে ব্রাঞ্চ · যে ছাঁকনি · যা খোঁজা হয়েছে, সব মিলিয়ে।
        shownItems = ordered
        if (ordered.isEmpty()) {
            setListVisible(false)
            binding.progressLoad.visibility = View.GONE
            binding.tvEmpty.text = "No records found"
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.progressLoad.visibility = View.GONE
            setListVisible(true)
            buildRows(ordered)
            // 🔵 TK (10.08.2026): "কাল আসার কথা" থেকে এলে — তালিকা আঁকা হয়ে গেলে
            // ওই কার্ডে স্ক্রল করে হাইলাইট (একবারই, নম্বর তালিকায় থাকলে)।
            if (pendingFocusCardMobile.isNotBlank()) binding.rvList.post { tryFocusScroll() }
        }
    }

    /** APPROVED UPDATE #2: Enquiry list order = Overdue -> Today -> Tomorrow ->
     *  Future (NOT by Enquiry Date). Only applied to the Enquiry (Inquiry) tab;
     *  Visit/Patient tabs keep their existing order untouched. */
    private fun sortEnquiryByUrgency(items: List<FollowUpItem>): List<FollowUpItem> {
        val today = FollowUpModel.today()
        val tomorrow = run {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val cal = java.util.Calendar.getInstance()
            cal.time = fmt.parse(today) ?: java.util.Date()
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            fmt.format(cal.time)
        }
        fun rank(nf: String): Int = when {
            nf.isBlank() -> 4
            nf < today -> 0      // Overdue
            nf == today -> 1     // Today
            nf == tomorrow -> 2  // Tomorrow
            else -> 3            // Future
        }
        return items.sortedWith(compareBy({ rank(it.nextFollow) }, { it.nextFollow }))
    }

    private fun dpx(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** Crash-safe list: each card is built in plain code (no XML inflation, so
     *  the screen always opens) but now mirrors the WebView fuCard() design —
     *  a two-column card. */
    // 🚨 TK-REPORTED (2026-07-27): "Follow up card এর প্রতিটা সেকশন লোডিং হতে সময়
    // লাগছে... স্ক্রল করলে মনে হচ্ছে 60Hz থেকেও কম।"
    //
    // WHY IT WAS SLOW: every card of the whole list was built here in ONE go.
    // A card is ~35 views, so 40 patients = ~1,400 views created, measured and
    // laid out inside a single frame -- the screen froze while that ran, and
    // the phone had to carry that whole tree while scrolling.
    //
    // WHAT IS DIFFERENT NOW: the first screenful is built at once (so the list
    // appears immediately) and the rest are added a few at a time, one batch
    // per frame. The cards themselves, their order, their design and every
    // click are byte-for-byte the same -- only WHEN they are created changed.
    // A new render cancels the running batches, so switching tab or typing in
    // the search box can never leave two lists half-drawn on top of each other.
    private var renderJob: kotlinx.coroutines.Job? = null
    private var lastRenderSignature: String = ""

    private fun signatureOf(items: List<FollowUpItem>): String {
        val sb = StringBuilder(items.size * 24)
        for (i in items) {
            sb.append(i.id).append('|').append(i.nextFollow).append('|')
                .append(i.lastRemark).append('|').append(i.stage).append('|')
                .append(i.paid).append('|').append(i.bill).append('|')
                .append(i.callCount).append('|')
                .append(i.lastCallDate).append('|').append(i.lastCallBy).append('|')
                // 🆕 V851 — এই দুটোও কার্ডে দেখা যায়, তাই চিহ্নে থাকতেই হবে;
                //    নইলে বদলালেও তালিকা নতুন করে আঁকত না।
                .append(i.regDate).append('|').append(i.regBy).append(';')
        }
        return sb.toString()
    }

    /** ⚡ খাতার সারি B19, ধাপ ২ — TK-এর নির্দেশ (28.07.2026 ১২.৩৮ pm):
     *  *"আপনার কাজ আপনি করুন, তারপর আমি একবারে লাইভ টেস্ট করে রিপোর্ট দেবো।"*
     *  তাই তিনটে সেকশনই (Enquiry · Visit · Patient) এখন এই একটাই তালিকায়।
     *
     *  ⛔ পুরনো পথ (নিচের ব্যাচে-আঁকা কৌশল ও `listContainer`) **মোছা হয়নি**।
     *  কোনো কারণে ফিরে যেতে হলে শুধু এই একটা লাইনে `false` করে দিলেই
     *  আগের দিনের ব্যবহার হুবহু ফিরে আসবে, আর কিছু বদলাতে হবে না। */
    private val useRecyclerList = true

    /** All three sections (Enquiry · Visit · Patient) share this one adapter.
     *  It only carries the cards; every card is still built by the untouched
     *  `buildFollowCard` below. */
    private val followAdapter by lazy { FollowAdapter() }

    /** কার্ড ধরে রাখার ঘর — আলাদা করে রাখা হয়েছে যাতে কোনো দ্বিধা না থাকে। */
    private class FollowCardHolder(val box: android.widget.LinearLayout) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(box)

    private inner class FollowAdapter :
        androidx.recyclerview.widget.RecyclerView.Adapter<FollowCardHolder>() {

        private val rows = ArrayList<FollowUpItem>()

        fun submit(items: List<FollowUpItem>) {
            rows.clear()
            rows.addAll(items)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FollowCardHolder {
            val box = android.widget.LinearLayout(this@FollowUpActivity)
            box.orientation = android.widget.LinearLayout.VERTICAL
            box.layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                androidx.recyclerview.widget.RecyclerView.LayoutParams.MATCH_PARENT,
                androidx.recyclerview.widget.RecyclerView.LayoutParams.WRAP_CONTENT
            )
            return FollowCardHolder(box)
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: FollowCardHolder, position: Int) {
            holder.box.removeAllViews()
            holder.box.background = null   // 🔵 ফোকাস-হাইলাইট রিসাইকেল হলে যেন থেকে না যায়
            holder.box.setPadding(0, 0, 0, 0)   // 🏷️ V1254 — বেড়ের ফাঁকটাও যেন থেকে না যায়
            // exactly the same card builder as before, not one line changed
            buildFollowCard(holder.box, rows[position])
        }
    }

    /** One place decides which of the two lists is on screen, so an error or an
     *  empty result can never leave both visible (or both hidden). */
    private fun setListVisible(visible: Boolean) {
        if (!visible) {
            binding.listContainer.visibility = View.GONE
            binding.scrollList.visibility = View.GONE
            binding.rvList.visibility = View.GONE
            return
        }
        val useRecycler = useRecyclerList
        binding.rvList.visibility = if (useRecycler) View.VISIBLE else View.GONE
        binding.scrollList.visibility = if (useRecycler) View.GONE else View.VISIBLE
        binding.listContainer.visibility = View.VISIBLE
    }

    private fun buildRows(items: List<FollowUpItem>) {
        if (useRecyclerList) {
            renderJob?.cancel()
            if (binding.listContainer.childCount > 0) binding.listContainer.removeAllViews()
            val rvSig = signatureOf(items)
            if (rvSig == lastRenderSignature && followAdapter.itemCount == items.size && items.isNotEmpty()) return
            lastRenderSignature = rvSig
            // 🔒 V217 (§B216, Master Fix Order §14, item 8 "Follow-up Scroll
            // Position ঠিক রাখুন"): `submit()`-এর ভিতরের `notifyDataSetChanged()`
            // পুরো তালিকা নতুন করে আঁকে — সাধারণত RecyclerView নিজেই স্ক্রল
            // ধরে রাখে, কিন্তু তালিকার উপরের কার্ডগুলোর উচ্চতা রিমার্কের
            // দৈর্ঘ্যভেদে বদলাতে পারে (একটা কার্ডের রিমার্ক-বাক্স বড়/ছোট
            // হলে), তখন নিচের কার্ডগুলো একটু নড়ে যেতে পারে। এখন redraw-এর
            // ঠিক আগে-পরে প্রথম দৃশ্যমান কার্ডের position+pixel-offset ধরে
            // রেখে ফিরিয়ে দেওয়া হয় — ৩-ট্যাপ পপ-আপ থেকে ফেরা, রিমার্ক সেভ,
            // বা অন্য পর্দা থেকে Back — সব ক্ষেত্রেই যতটা সম্ভব একই জায়গায়।
            val lm = binding.rvList.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
            val firstPos = lm?.findFirstVisibleItemPosition() ?: -1
            val firstOffset = if (firstPos >= 0) lm?.findViewByPosition(firstPos)?.top ?: 0 else 0
            followAdapter.submit(items)
            if (firstPos in 0 until items.size) {
                lm?.scrollToPositionWithOffset(firstPos, firstOffset)
            }
            return
        }
        val container = binding.listContainer
        renderJob?.cancel()
        val sig = signatureOf(items)
        // Nothing on the list actually changed and it is fully drawn already --
        // rebuilding it would only cause a visible stutter for no reason.
        if (sig == lastRenderSignature && container.childCount == items.size && items.isNotEmpty()) return
        lastRenderSignature = sig
        container.removeAllViews()
        val firstBatch = minOf(items.size, 6)
        for (i in 0 until firstBatch) buildFollowCard(container, items[i])
        if (items.size <= firstBatch) return
        renderJob = lifecycleScope.launch {
            var i = firstBatch
            while (i < items.size) {
                val end = minOf(items.size, i + 5)
                for (j in i until end) buildFollowCard(container, items[j])
                i = end
                kotlinx.coroutines.delay(16)   // hand one frame back to the screen
            }
        }
    }

    /** One follow-up card, matching the WebView fuCard() layout:
     *   LEFT  : Enquiry -> call-signal meter; Visit/Patient -> avatar +
     *           VISITED/PATIENT pill + registration date.
     *   MAIN  : name / mobile / BRANCH|DISEASE tags, with the badge (Enquiry),
     *           Visit-Advance pill (Visit) or Prescription+payment-ring
     *           (Patient) pinned top-right; then the Next Follow-up line, the
     *           Last Remark box and four action buttons (Call/WhatsApp/View/Next). */
    private fun buildFollowCard(container: android.widget.LinearLayout, item: FollowUpItem) {
        val MATCH = android.widget.LinearLayout.LayoutParams.MATCH_PARENT
        val WRAP = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        val dens = resources.displayMetrics.density
        fun ll(orient: Int) = android.widget.LinearLayout(this).apply { orientation = orient }
        fun tv(text: CharSequence, size: Float, colorHex: String, bold: Boolean = false) =
            android.widget.TextView(this).apply {
                this.text = text; textSize = size
                setTextColor(android.graphics.Color.parseColor(colorHex))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        fun rounded(colorHex: String, radiusDp: Float) = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = radiusDp * dens; setColor(android.graphics.Color.parseColor(colorHex))
        }

        val isInquiry = item.stage == "Inquiry"
        val isTreatment = item.stage == "Treatment"
        val regDate = if (item.recordDate.isNotBlank()) FollowUpModel.displayDate(item.recordDate) else ""

        // ---------- Card shell (horizontal: left | main) ----------
        val card = ll(android.widget.LinearLayout.VERTICAL)
        val topRow = ll(android.widget.LinearLayout.HORIZONTAL)
        topRow.layoutParams = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)
        card.setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_follow_card)
        val clp = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)
        clp.setMargins(dpx(8), dpx(5), dpx(8), dpx(5)); card.layoutParams = clp
        card.setPadding(dpx(10), dpx(10), dpx(10), dpx(10))

        // ---------- LEFT column (62dp) ----------
        val left = ll(android.widget.LinearLayout.VERTICAL).apply { gravity = android.view.Gravity.CENTER }
        left.setPadding(0, dpx(6), 0, 0)  // balance: nudge signal/photo + label + date slightly down
        val leftLp = android.widget.LinearLayout.LayoutParams(dpx(62), WRAP); leftLp.marginEnd = dpx(8)
        left.layoutParams = leftLp
        if (isInquiry) {
            val n = item.callCount.coerceIn(0, 5)
            val wifiDrawable = when (n) {
                0 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_0
                1 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_1
                2 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_2
                3 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_3
                4 -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_4
                else -> com.tkbiswas.pilesclinic.R.drawable.ic_wifi_calls_5
            }
            val wifi = android.widget.ImageView(this).apply {
                setImageResource(wifiDrawable)
                contentDescription = "Enquiry calls: $n of 5"
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                /* 🎨🔒 V882 (৩০.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত):
                   TK: *"Wi-Fi signal তার নিচে তারিখটা কেটে গেল কেন — সিগনাল
                   সামান্য উপরে তুলুন বা সাইজে ছোট করুন"*। উচ্চতা ৪০ → ৩৪dp,
                   নিচের ফাঁক ৭ → ৩dp ⇒ ১০dp জায়গা খালি, তারিখ পুরো বসে।
                   ⛔ Draft কার্ডেও (item_followup_card.xml) হুবহু একই বদল। */
                layoutParams = android.widget.LinearLayout.LayoutParams(dpx(48), dpx(34))
            }
            left.addView(wifi)
            TripleTapEdit.attach(wifi) { entryActionMenu(item) }
            val status = tv("Enquiry", 10f, "#1067D8", true).apply {   // V229: section name slightly bigger (readability)
                val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP); p.topMargin = dpx(3); layoutParams = p   // 🎨 V882
            }
            left.addView(status)
            TripleTapEdit.attach(status) { entryActionMenu(item) }
            if (regDate.isNotBlank()) left.addView(tv(regDate, 9.5f, "#667085").apply {   // V229: date slightly bigger (readability)
                val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP)
                p.marginStart = dpx(2)
                layoutParams = p
            })
        } else {
            val avatar = tv("\uD83D\uDC64", 24f, "#10223A").apply {
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_avatar_square)
                layoutParams = android.widget.LinearLayout.LayoutParams(dpx(54), dpx(54))
            }
            left.addView(avatar)
            TripleTapEdit.attach(avatar) { openPatientPhotoEditor(item) }

            // TK-REPORTED BUG FIX (2026-07-16), PERFORMANCE-CORRECTED: the
            // card used to always show the generic avatar emoji, even after
            // a photo was saved. Fixed using item.photo (already fetched as
            // part of this list's normal load, select=* -- zero extra
            // network calls) instead of a separate live lookup per card.
            if (item.photo.isNotBlank()) {
                val avatarIndex = left.indexOfChild(avatar)
                lifecycleScope.launch {
                    val bmp = withContext(Dispatchers.Default) {
                        try { PhotoUtils.decodeDataUrl(item.photo) } catch (_: Exception) { null }
                    }
                    if (bmp != null && avatarIndex >= 0 && avatarIndex < left.childCount) {
                        val photoView = android.widget.ImageView(this@FollowUpActivity).apply {
                            setImageBitmap(bmp)
                            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_avatar_square)
                            clipToOutline = true
                            layoutParams = avatar.layoutParams
                        }
                        TripleTapEdit.attach(photoView) { openPatientPhotoEditor(item) }
                        left.removeViewAt(avatarIndex)
                        left.addView(photoView, avatarIndex)
                    }
                }
            }

            val statusText = if (isTreatment) "PATIENT" else "VISITED"
            val status = tv(statusText, 11f, "#FFFFFF", true).apply {   // V229: Visit/Patient section name slightly bigger (readability)
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_visited_pill)
                setPadding(dpx(8), dpx(3), dpx(8), dpx(3))
                val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP)
                p.topMargin = dpx(if (isTreatment) 9 else 5)  // APPROVED #5: Patient card nudged down for more photo visibility
                layoutParams = p
            }
            left.addView(status)
            TripleTapEdit.attach(status) { entryActionMenu(item) }

            val idText = item.patientId.ifBlank { regDate }
            if (idText.isNotBlank()) {
                val idView = tv(idText, 9.5f, "#10223A", true).apply {   // V229: Visit/Patient date/ID slightly bigger (readability)
                    // TK APPROVED (2026-07-15): Patient ID must stay on ONE line
                    // (never break/wrap) and must never be cut off either — so
                    // the text auto-shrinks just enough to fit this column,
                    // instead of wrapping (previous fix) or clipping (original bug).
                    setSingleLine(true); maxLines = 1
                    gravity = android.view.Gravity.CENTER
                    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this, 6, 10, 1, android.util.TypedValue.COMPLEX_UNIT_SP
                    )
                    val p = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)
                    p.topMargin = dpx(if (isTreatment) 5 else 3)  // APPROVED #5
                    layoutParams = p
                }
                left.addView(idView)
                TripleTapEdit.attach(idView) { showEditDialog(item) }
            }
        }
        // TK-APPROVED (2026-07-27, via before/after photo proof): the 62dp left
        // column is kept ONLY on the Enquiry card, where it carries the call
        // signal bars. On the Visit and Patient cards it is removed entirely
        // (photo tile + VISITED/PATIENT pill + Patient ID), exactly like the
        // RMP card, so the NAME AND MOBILE START AT THE LEFT EDGE and gain
        // about 80dp of width. The pill and the Patient ID are not lost --
        // they move onto their own line under the branch/disease tags (added
        // further below). Nothing else on the card moves: ADVANCE HERE, TEST
        // HERE, PRESCRIPTION, the payment ring, Bill/Due, the Remark box and
        // the four action buttons all keep their exact size and position.
        if (isInquiry) topRow.addView(left)
        val main = ll(android.widget.LinearLayout.VERTICAL)
        main.layoutParams = android.widget.LinearLayout.LayoutParams(0, WRAP, 1f).apply {
            if (isInquiry) marginStart = dpx(10)
        }

        // top row: info block (weight 1) + right slot
        val top = ll(android.widget.LinearLayout.HORIZONTAL)
        top.layoutParams = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)

        val info = ll(android.widget.LinearLayout.VERTICAL)
        info.layoutParams = android.widget.LinearLayout.LayoutParams(0, WRAP, 1f).apply {
            if (isTreatment) topMargin = dpx(8)
        }
        // 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B65):
        // TK: *"পেশেন্টের নামের আগে যে ফটো আইকন টাইপের ছোট্ট একটা বক্স আছে,
        // সেটাকে সরিয়ে ওখানে সিরিয়াল নাম্বার দেবেন।"* — তাই 👤 আইকনের জায়গায়
        // এখন RMP কার্ডের হুবহু একই লাল সিরিয়াল ব্যাজ (`bg_serial_red`)।
        //
        // TK: *"নাম এবং মোবাইল নাম্বার সামান্য একটু ডানদিকে সরিয়ে দেন... যেহেতু
        // আজ সিরিয়াল নাম্বার ১ আছে, কোন এক সময় এক লাখও হতে পারে, সুতরাং সেটুকু
        // গ্যাপ রাখবেন... গায়ে গায়েও যেন ভেসে না যায়।"*
        // তাই নাম ও মোবাইল এখন **ব্যাজের ডান পাশের নিজের কলামে** — দুটোরই বাঁ
        // প্রান্ত এক জায়গায়, আর ব্যাজের সঙ্গে **পাকা ফাঁক** (৯dp)। ব্যাজটা
        // নম্বরের সঙ্গে নিজে চওড়া হয়, লেখা ততটাই ডানে সরে — **এক লাখ হলেও
        // কোনোদিন গায়ে লাগবে না, আর নম্বর কাটবেও না**।
        // ⛔ নম্বর না জানা গেলে (বিরল) আগের 👤 আইকনটাই থাকে, কার্ড ফাঁকা দেখায় না।
        val serialNo = serialByItemId[item.id] ?: 0
        val nameRow = ll(android.widget.LinearLayout.HORIZONTAL).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)
        }
        if (serialNo > 0) {
            val serialView = tv(serialNo.toString(), 11f, "#FFFFFF", true).apply {
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_serial_red)
                setPadding(dpx(6), dpx(3), dpx(6), dpx(3))
                minWidth = dpx(24)
                maxLines = 1
                val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP)
                p.marginEnd = dpx(9)
                layoutParams = p
            }
            nameRow.addView(serialView)
            TripleTapEdit.attach(serialView) { showEditDialog(item) }
        }
        val nameCol = ll(android.widget.LinearLayout.VERTICAL).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        val nameView = tv(
            // 🔴🔴 TK-REPORTED (31.07.2026 — "Patient Name-এর জায়গায় Mobile
            // দুবার দেখানো"): নাম না থাকলে আগে এখানে মোবাইল বসত, আর ঠিক
            // নিচের লাইনেও (📞) মোবাইল দেখাত — একই নম্বর দুইবার। এখন নাম
            // না থাকলে "UNKNOWN" — মোবাইল শুধু নিচের লাইনে,
            // একবারই। ⛔ নাম থাকলে কিছুই বদলায়নি।
            (if (serialNo > 0) "" else "\uD83D\uDC64 ") + item.name.ifBlank { "UNKNOWN" },
            16f, "#10223A", true
        ).apply {
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        }
        nameCol.addView(nameView)
        TripleTapEdit.attach(nameView) { showEditDialog(item) }
        /* 📋🔒 V827 (২৯.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) — নামের উপরে **লম্বা চাপ**
           দিলে নামটা কপি হয়। ⛔ কপি হয় **শুধু নামটাই** — সামনের 👤 চিহ্ন বা
           ক্রমিক সংখ্যা নয়। এক-চাপ ও তিন-চাপে-এডিট এক অক্ষরও বদলায়নি
           (লম্বা চাপে Android কখনো `onClick` ডাকে না)।
           ⛔ কপি করা হয় প্রজেক্টের একটাই প্রমাণিত পথ `Clip` দিয়ে (পাহারা ৯.২৯),
              তাই কীবোর্ডের সাজেশনে নামটা জমে থাকবে না — FollowUpAdapter-এ
              নম্বর কপির হুবহু একই ব্যবস্থা। */
        nameView.setOnLongClickListener {
            if (item.name.isBlank()) false
            else { Clip.copyWithToast(it.context, "Name", item.name.trim()); true }
        }

        val mobileView = tv("\uD83D\uDCDE " + formatMobileForDisplay(item.mobile), 12.5f, "#5B6B81").apply {
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            val p = android.widget.LinearLayout.LayoutParams(MATCH, WRAP); p.topMargin = dpx(3); layoutParams = p
        }
        nameCol.addView(mobileView)
        TripleTapEdit.attach(mobileView) { showEditDialog(item) }
        /* 📋🔒 V827 — নম্বরের উপরে লম্বা চাপ দিলে নম্বরটা কপি হয়।
           ⛔ 📞 চিহ্নটা বাদ দিয়ে **পর্দায় যেমন দেখাচ্ছে ঠিক সেই নম্বরটাই**
              (যেমন +919382292755) কপি হয়। */
        mobileView.setOnLongClickListener {
            if (item.mobile.isBlank()) false
            else { Clip.copyWithToast(it.context, "Mobile", formatMobileForDisplay(item.mobile)); true }
        }
        nameRow.addView(nameCol)
        info.addView(nameRow)
        // 🔒🔒 খাতার সারি B184 (TK, 30.07.2026 বিকেল ৪.৫০ — ফটো-প্রুফে ফাইনাল):
        // *"কোনটা যেন কোনটার গায়ে ঘেঁষে না যায়, কোনটা যেন কাটা না পড়ে যায়,
        // কোনটা যেন কার্ড থেকে বেরিয়ে না যায়।"*
        // তাই ট্যাগগুলো আর **এক লাইনে জোর করে ধরানো হয় না** — `tagsWrap`
        // (খাড়া) এখন এক বা একাধিক আড়াআড়ি সারি ধরে; জায়গায় না কুলোলে পুরো
        // ট্যাগটা **নিচের লাইনে নেমে যায়**। ফন্ট মাপ · রং · প্যাডিং সব ট্যাগে
        // হুবহু এক, বাক্সের মাপ লেখার মাপে, আর কোনো ট্যাগ কখনো "…" দিয়ে কাটে না।
        val tagsWrap = ll(android.widget.LinearLayout.VERTICAL)
        val tags = ll(android.widget.LinearLayout.HORIZONTAL).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        // 🔒🔒 খাতার সারি B172 (TK, 30.07.2026 — "সবগুলি বক্সের ফ্রন্ট একই মাপের
        // হতে হবে... দরকারে আগের ফিক্সড গুলির সাইজ একটু ছোট করুন... প্রত্যেকটা
        // কালাও ও একই রাখুন"): Branch · Disease · ঠিকানা-ট্যাগ · RMP/Unexpected
        // Time — এই চারটেই এখন **একই রং, একই ফন্ট মাপ**। আগে Branch/Disease
        // ছিল fixed (নিজের যতটুকু লাগে) আর তৃতীয় ট্যাগ ছিল flexible; এখন
        // চারটেই একসাথে।
        // ⛔ সংশোধন (খাতার সারি B184, 30.07.2026 বিকেল): উপরের নিয়মের শেষ
        //    অংশটা — "না কুলোলে সবার ফন্ট ছোট · তারপর ব্র্যাঞ্চের সংক্ষিপ্ত কোড ·
        //    তারপরও না কুলোলে … দিয়ে কাটা" — **বাতিল করা হয়েছে**, কারণ TK-এর
        //    ছবিতে ওতেই লেখা কেটে যাচ্ছিল (`JPE | HYDROC… | UNEXPE…`)। এখন
        //    ফন্ট সবসময় ১০.৫, ব্র্যাঞ্চের পুরো নামই থাকে, আর জায়গা না কুলোলে
        //    ট্যাগ **নিচের লাইনে নামে** (নিচের `layoutTagsInRows`)।
        val tagsLp = android.widget.LinearLayout.LayoutParams(MATCH, WRAP); tagsLp.topMargin = dpx(5); tagsWrap.layoutParams = tagsLp
        tags.layoutParams = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)

        val diseaseLabel = item.disease.ifBlank { "-" }.uppercase()
        // 🔒 খাতার সারি B173 (TK, 30.07.2026 — "থাক বাদ দিন, পেসেন্ট কার্ডে
        // লাগবে না ওগুলি, যেহেতু View All-এ চাপলে সব দেখা যাবে"): ঠিকানার
        // ট্যাগ শুধু Enquiry ও Visit কার্ডে — Patient (Treatment) কার্ডে নয়,
        // কারণ সেখানে বিস্তারিত এমনিতেই "View All"/Patient Timeline-এ দেখা যায়।
        val addressLabel = if (isTreatment) "" else item.addressTag.trim().uppercase()
        // ⛔ "RMP" প্রাধান্য পায় (এই রোগী সবসময়ই RMP-র, Unexpected Time শুধু
        //    সর্বশেষ কলের সময় নিয়ে) — আগের নিয়ম হুবহু অক্ষত।
        val extraLabel = when {
            item.refDoctor.isNotBlank() -> "RMP"
            item.timeType.equals("Unexpected Time", ignoreCase = true) -> "UNEXPECTED"
            else -> ""
        }

        // 🔒 খাতার সারি B184: `ellipsize` তুলে দেওয়া হলো — ট্যাগ আর কখনো
        //    "…" দিয়ে কাটবে না; জায়গা না কুলোলে নিচের লাইনে নামবে।
        /* 🟣🔒 V707 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত):
           *"ব্রাঞ্চ+রোগের নাম একই কালার হবে · ঠিকানা+Unexpected+RMP অন্য কালার হবে"*।
           ⛔ ডিফল্ট আগের নীল (`bg_tag_branch`), তাই ব্রাঞ্চ ও রোগের ট্যাগ
              এক অক্ষরও বদলায়নি — শুধু দ্বিতীয় সারিটা বেগুনি চায়। */
        fun pill(text: String, bgRes: Int = com.tkbiswas.pilesclinic.R.drawable.bg_tag_branch): android.widget.TextView = tv(text, 10.5f, "#FFFFFF", true).apply {
            setBackgroundResource(bgRes)
            setPadding(dpx(7), dpx(4), dpx(7), dpx(4))
            maxLines = 1
            ellipsize = null
            gravity = android.view.Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(WRAP, WRAP)
        }
        fun sep() = tv("|", 12f, "#98A2B3").apply {
            val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP); p.marginStart = dpx(6); p.marginEnd = dpx(6); layoutParams = p
        }

        val branchView = pill(item.branch.ifBlank { "-" }.uppercase())
        tags.addView(branchView)
        TripleTapEdit.attach(branchView) { showEditDialog(item) }

        tags.addView(sep())
        val diseaseView = pill(diseaseLabel)
        tags.addView(diseaseView)
        TripleTapEdit.attach(diseaseView) { showEditDialog(item) }

        var addressView: android.widget.TextView? = null
        if (addressLabel.isNotBlank()) {
            tags.addView(sep())
            val av = pill(addressLabel, com.tkbiswas.pilesclinic.R.drawable.bg_tag_extra_purple)
            tags.addView(av)
            // 🔒 খাতার সারি B172: এখানে শুধু **এক ট্যাপ** — পুরো ফর্ম খোলে না,
            // শুধু এই ট্যাগটাই আলাদাভাবে বদলানোর ছোট পপ-আপ খোলে (TK-এর
            // স্পষ্ট নির্দেশ: "শুধুমাত্র ওই ট্যাগ টুকু যেন আলাদাভাবে
            // পরিবর্তন করা যায়")।
            av.setOnClickListener { showAddressTagEditor(item) }
            addressView = av
        }

        var extraView: android.widget.TextView? = null
        if (extraLabel.isNotBlank()) {
            tags.addView(sep())
            extraView = pill(extraLabel, com.tkbiswas.pilesclinic.R.drawable.bg_tag_extra_purple)
            tags.addView(extraView)
        }
        tagsWrap.addView(tags)
        /* 🟣🔒 V707 — TK-রিপোর্ট (ছবিসহ): *"BAPPA এনার tag উপর নিচে কেন"*।
           **আসল কারণ:** ট্যাগগুলো বসত `info`-র ভিতরে, আর `info` হলো ডান
           দিকের ব্যাজ/রিং-এর **পাশের সরু কলাম** (top → info, weight=1)।
           ব্যাজের **নিচের** ফাঁকা জায়গাটা তাই ট্যাগ ব্যবহার করতেই পারত না —
           BAPPA-র দুটো ট্যাগ কয়েক পিক্সেলের জন্য না কুলিয়ে আলাদা লাইনে নামত।
           ⇒ **Enquiry কার্ডে** ট্যাগের সারিটা এখন `main`-এ বসে (নিচে দেখুন),
             অর্থাৎ কার্ডের পুরো চওড়া পায়।
           ⛔ Visit/Patient কার্ডে **হাত দেওয়া হয়নি** — ওখানে ডান দিকে
              ADVANCE HERE · TEST HERE · PRESCRIPTION · টাকার রিং লম্বা করে
              বসে, ট্যাগ নিচে নামালে চেহারা বদলে যেত (TK অনুমোদন দেননি)।
              ওখানে আগের জায়গাতেই, শুধু রং ও দল-ভাগ নতুন। */
        if (!isInquiry) info.addView(tagsWrap)

        // 🔒 খাতার সারি B184: লেআউট হয়ে যাওয়ার ঠিক পরেই (আসল প্রস্থ তখনই জানা
        // যায়) ট্যাগগুলো এক বা একাধিক সারিতে সাজানো হয় — কেউ কাটে না, কেউ
        // কার্ডের বাইরে বেরোয় না, কেউ কারো গায়ে ঘেঁষে না। ⛔ প্রতিটা কার্ডে
        // একবারই চলে (post{}), তাই স্ক্রল করার সময় বাড়তি ভার নেই।
        // 🔴🔴🟢 খাতার সারি [পরবর্তী] (TK-রিপোর্ট, ছবিসহ — Branch ও Disease
        // ট্যাগ ডানদিকে অনেক ফাঁকা জায়গা থাকা সত্ত্বেও আলাদা আলাদা লাইনে
        // নেমে যাচ্ছিল)। **আসল কারণ:** `post{}` একবারই চলত — কিন্তু এই কার্ডের
        // `tagsWrap`-এর আসল (চূড়ান্ত) width ততক্ষণে সবসময় ঠিকভাবে মেপে ওঠে না,
        // কারণ ওজন-ভিত্তিক (weight) নেস্টেড LinearLayout (top→info, weight=1)
        // মাঝে মাঝে এক ফ্রেমেই পুরোপুরি resolve হয় না — তাই মাঝে মাঝে একটা
        // পুরনো/ছোট width ধরে হিসাব হতো, ট্যাগ তাড়াতাড়ি নিচের লাইনে নেমে যেত।
        // **সমাধান (ঝুঁকিহীন):** `post{}`-এর ভিতরে আরেকটা `post{}` — এক ফ্রেম
        // বেশি অপেক্ষা করে, ততক্ষণে width স্থির/চূড়ান্ত হয়ে যায়। ⛔ বাকি সব
        // যুক্তি (`layoutTagsInRows`, একবারই চলা, স্ক্রলে বাড়তি ভার না থাকা)
        // এক অক্ষরও বদলায়নি — শুধু এক ফ্রেম (~১৬ms, চোখে ধরা পড়ে না) দেরি।
        /* 🟣🔒 V1543 (১৯.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — "Cooch Behar ·
           Piles · Kotwali পাশাপাশি থাকা যাবে?" আগে V707-এ ব্রাঞ্চ+রোগ আর
           ঠিকানা+Unexpected/RMP দুটো আলাদা দল ছিল, দল বদলালেই জোর করে নতুন
           সারি হত (জায়গা থাকলেও)। এখন সবগুলো ট্যাগ **একটাই দল** — জায়গা
           কুলোলে সবাই এক সারিতে, না কুলোলে (লম্বা নাম) আগের নিয়মেই নিজে
           থেকে নিচে নামে — কেউ কাটে না, কার্ডের বাইরে বেরোয় না (খাতার সারি
           B184-এর প্রতিশ্রুতি অক্ষত, layoutTagsInRows()-এর ভিতরের কোনো
           লজিক বদলানো হয়নি, শুধু গ্রুপিং এক করা হলো)। */
        val pillGroups = listOf(
            listOfNotNull(branchView, diseaseView, addressView, extraView)
        ).filter { it.isNotEmpty() }
        tagsWrap.post {
            tagsWrap.post {
                try { layoutTagsInRows(tagsWrap, pillGroups) } catch (_: Throwable) { }
            }
        }

        // TK-APPROVED (2026-07-27, via before/after photo proof): the
        // VISITED / PATIENT pill and the Patient ID used to live in the 62dp
        // left column that has just been removed from these two cards. They
        // are NOT dropped -- they sit here on their own line, right under the
        // branch/disease tags, so no information is lost. Same pill colour,
        // same pill shape, same Patient ID text. The Enquiry card never had
        // them and is not touched.
        /* 📵🔒 V718 (২৬.০৮.২০২৬, TK-নির্দেশ — লাইভে ধরা):
           V711-এ কার্ডে "📵 কল বন্ধ" লেখাটা বসানো হয়েছিল, শর্ত ছিল
           "পরের তারিখ ফাঁকা + আগে কল হয়েছে"। **সেটা ভুল ছিল** — যে রোগীর
           পরের তারিখটা এখনো বসানোই হয়নি, তাঁর কার্ডেও লেখাটা উঠত
           (TK-এর ছবি: MOKIM · MUZAFFAR HUSSAIN)।
           TK-এর সিদ্ধান্ত: *"সিস্টেমে ওটা দেখানোরও দরকার নেই, লুকিয়ে থাকলেও
           চলবে"* ⇒ **লেখাটা পুরোপুরি তুলে দেওয়া হলো**।
           ⛔ কোনো তথ্য · হিসাব · ছাঁকনি · অন্য কোনো ব্যাজ ছোঁয়া হয়নি। */
        if (!isInquiry) {
            val idRow = ll(android.widget.LinearLayout.HORIZONTAL).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val idRowLp = android.widget.LinearLayout.LayoutParams(WRAP, WRAP)
            idRowLp.topMargin = dpx(6)
            idRow.layoutParams = idRowLp
            val statusPill = tv(if (isTreatment) "PATIENT" else "VISITED", 10f, "#FFFFFF", true).apply {
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_visited_pill)
                setPadding(dpx(9), dpx(3), dpx(9), dpx(3))
            }
            idRow.addView(statusPill)
            TripleTapEdit.attach(statusPill) { entryActionMenu(item) }
            val idText2 = item.patientId.ifBlank { regDate }
            if (idText2.isNotBlank()) {
                val idOnRow = tv(idText2, 10f, "#10223A", true).apply {
                    maxLines = 1
                    val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP)
                    p.marginStart = dpx(8)
                    layoutParams = p
                }
                idRow.addView(idOnRow)
                TripleTapEdit.attach(idOnRow) { showEditDialog(item) }
            }
            info.addView(idRow)
        }
        top.addView(info)

        // right slot: badge / Visit-Advance / payment ring
        val right = ll(android.widget.LinearLayout.VERTICAL).apply { gravity = android.view.Gravity.CENTER_HORIZONTAL }
        val rlp = android.widget.LinearLayout.LayoutParams(WRAP, WRAP); rlp.marginStart = dpx(6); right.layoutParams = rlp
        if (isInquiry) {
            val days = FollowUpModel.daysUntil(item.nextFollow)
            if (days != null) {
                val label: String; val colorHex: String; val quickFilter: String?
                when {
                    days < 0 -> { label = "\u23F0 Overdue"; colorHex = "#E5484D"; quickFilter = "Overdue" }
                // 🔴🔴🔴 খাতার সারি B202 (TK, 30.07.2026 রাত, স্ক্রিনশটসহ — TK
                // তীব্র ক্ষুব্ধ): *"কোন ব্রাঞ্চের স্টাফ ৩০ শে জুলাই ইনকোয়ারি
                // করেছে তাকে ক্যালেন্ডারে ১৭ জুলাই দেখাচ্ছে এটা কেন হচ্ছে।"*
                //
                // **আসল কারণ (যাচাই করে ১০০% নিশ্চিত হয়ে বলছি):** এই "১৭"
                // সংখ্যাটা অ্যাপ থেকে আসেনি — এটা ⏰ (calendar) ইমোজি
                // অক্ষরটার নিজের আঁকা ছবির ভিতরেই বসানো (ফোনের ফন্ট এটা এভাবেই
                // আঁকে, ঠিক যেমন WhatsApp/Gmail-সহ প্রতিটা অ্যাপে এই একই ইমোজি
                // এই একই "১৭" নিয়ে দেখা যায়) — পাশের "1d Due"/"3d Due" লেখাটাই
                // আসল, সঠিক তথ্য।
                //
                // ⛔⛔ **এটাই সবচেয়ে বড় সমস্যা: এই একই বাগ ১৬.০৭.২০২৬-এ একবার
                // TK রিপোর্ট করেছিলেন (এই ফাইলেরই উপরের দিকে, হেডারের ক্যালেন্ডার
                // বোতামে) এবং তখন ঠিক করা হয়েছিল — কিন্তু ঠিক এই "Due" ব্যাজের
                // ⏰ চিহ্নটা তখন বাদ পড়ে গিয়েছিল, তাই একই বাগ অন্য জায়গায় আবার
                // দেখা দিল।** এবার সমাধান: ⏰-এর বদলে ⏰ (এই একই আইকন "Overdue"
                // লেবেলে আগে থেকেই আছে, ওখানে কখনো এই সমস্যা হয়নি কারণ ⏰-তে
                // কোনো বাড়তি সংখ্যা আঁকা থাকে না)। ⛔ রং/লজিক/ছাঁকনি কিছুই
                // বদলায়নি, শুধু আইকন। **প্রজেক্টের অন্য কোথাও একই প্যাটার্ন
                // (FollowUpAdapter.kt) খুঁজে পাওয়া গেছে, সেটাও একইসঙ্গে ঠিক
                // করা হলো।**
                    days == 0 -> { label = "\u23F0 Today Due"; colorHex = "#E5484D"; quickFilter = "Today" }
                    else -> { label = "\u23F0 ${days}d Due"; colorHex = "#F79009"; quickFilter = null }
                }
                // 🔒 TK-APPROVED (29.07.2026, ফটো-প্রুফে পাশ · খাতার সারি B66):
                // TK: *"Over Due — এই লেখাটা ফিফটি পার্সেন্ট ছোট করুন।"*
                // লেখা ১১ → ৫.৫, চারপাশের ফাঁকও ঠিক অর্ধেক, কোণের গোলও অর্ধেক —
                // অর্থাৎ চিপটা মাপে হুবহু অর্ধেক। ⛔ রং · লেখা · চাপ দিলে যা হয়
                // (ওই ছাঁকনিতে চলে যাওয়া) কিছুই বদলায়নি।
                // ⛔ একই চিপ থেকেই "Today Due" ও "৩d Due" আসে, তাই তিনটেই একসঙ্গে
                //    ছোট হলো — নইলে একই তালিকায় একটা ছোট আরেকটা বড় দেখাত।
                // 🔒 TK-APPROVED (29.07.2026 সন্ধ্যা ৭.০০, ফটো-প্রুফে পাশ ·
                // খাতার সারি B94): TK-এর কথা — *"Overdue সাইজে আরও বড় হবে।
                // এখন যদি 50% রয়েছে তো আমি চাইছি 80% হোক।"*
                // অর্থাৎ **আসল মাপের 80%** — লেখা 11 × 0.8 = **8.8sp**,
                // চারপাশের ফাঁক 7/5 × 0.8 = **6/4**, কোণের গোল 8 × 0.8 = **6**।
                // (খাতার সারি B66-এ এটা অর্ধেক করা হয়েছিল — 5.5sp · 4/3 · 5।)
                // ⛔ রং · লেখা · চাপ দিলে ওই ছাঁকনিতে চলে যাওয়া — কিছুই বদলায়নি।
                // ⛔ একই চিপ থেকেই "Today Due" ও "3d Due" আসে, তাই তিনটেই
                //    একসঙ্গে বড় হলো — নইলে একই তালিকায় একটা ছোট আরেকটা বড় দেখাত।
                right.addView(tv(label, 8.8f, "#FFFFFF", true).apply {
                    setPadding(dpx(6), dpx(4), dpx(6), dpx(4)); background = rounded(colorHex, 6f)
                    if (quickFilter != null) setOnClickListener { applyQuickFilter(quickFilter) }
                })
            }
        } else if (!isTreatment) {
            /* 🔴🔒 V1543 (19.09.2026, TK-নির্দেশ, ফটো-প্রুফ পাশ) — "এখানে তেমন একটা ব্যবহার করা হয় না, সরিয়ে দিলে কার্ড হালকা হবে"।
               ADVANCE HERE / TEST HERE বোতাম দুটো Visit কার্ড থেকে বাদ — showAdvancePaymentDialog/
               openBloodTestDirect এখনো Patient Timeline/Action মেনু থেকে আগের মতোই করা যায়,
               শুধু কার্ড থেকে এক-চাপে হয় না। কোনো ডেটা/হিসাব ছোঁয়া হয়নি। */
        } else {
            right.addView(tv("PRESCRIPTION", 9.5f, "#1067D8", true).apply {
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_chip_blue)
                setPadding(dpx(8), dpx(3), dpx(8), dpx(3))
                setSingleLine(true)
                typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
                val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP); layoutParams = p
                setOnClickListener { openClinicalMenu(item) }
            })
            val pct = if (item.bill > 0) Math.min(100.0, Math.round(item.paid / item.bill * 100.0).toDouble()).toInt() else 0
            right.addView(PaymentRingView(this).apply {
                percent = pct
                val p = android.widget.LinearLayout.LayoutParams(dpx(48), dpx(48)); p.topMargin = dpx(7); layoutParams = p
                setOnClickListener { showNthPaymentDialog(item) }
            })
            val due = Math.max(0.0, item.bill - item.paid)
            val moneyRow = ll(android.widget.LinearLayout.HORIZONTAL).apply { gravity = android.view.Gravity.CENTER }
            val moneyLp = android.widget.LinearLayout.LayoutParams(WRAP, WRAP); moneyLp.topMargin = dpx(4); moneyRow.layoutParams = moneyLp
            val billView = tv("Bill\n₹${"%,.0f".format(item.bill)}", 8.3f, "#0B8F3C", true).apply {
                gravity = android.view.Gravity.CENTER
                setPadding(dpx(5), dpx(3), dpx(5), dpx(3))
                background = rounded("#EAF8EF", 7f)
            }
            moneyRow.addView(billView)
            TripleTapEdit.attach(billView) { openPaymentFor(item.mobile, item.refId, item.patientId) }
            val dueView = tv("Due\n₹${"%,.0f".format(due)}", 8.3f, "#D92D20", true).apply {
                gravity = android.view.Gravity.CENTER
                setPadding(dpx(5), dpx(3), dpx(5), dpx(3))
                background = rounded("#FDECEC", 7f)
                val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP); p.marginStart = dpx(4); layoutParams = p
            }
            moneyRow.addView(dueView)
            TripleTapEdit.attach(dueView) { openPaymentFor(item.mobile, item.refId, item.patientId) }
            right.addView(moneyRow)
        }
        top.addView(right)
        main.addView(top)
        // 🟣🔒 V707 — Enquiry কার্ডে ট্যাগ পুরো চওড়া পায় (উপরের কারণ দেখুন)।
        if (isInquiry) main.addView(tagsWrap)

        // ---------- Status line (TK APPROVED 2026-07-28, proof 6) ----------
        // ONE line, running from the left edge of the card to the right edge:
        //   "Last Call 20.07.2026 KNE-LAXMI" ......... "Next Follow up Call 29.07.2026"
        // Each half gets its own half of the row, so the two can never touch.
        // Both shrink themselves (auto-size) when a name or a date is long --
        // TK: "প্রয়োজনে সাইজের ছোট হবে তাও ভালো", but it must stay on ONE line.
        // 🚨 TK-REPORTED আবার (28.07.2026 ৭.৩৬ pm, ফটো-প্রুফসহ · খাতার সারি B49):
        // *"ফলোআপ সেকশনে Enquiry / Visit / Patient — সেখানে থাকতে বলেছিলাম
        //  LAST CALL 19.07.2026 (JPE-CRP) · NEXT CALL 29.07.2026। এই একই কথা
        //  আর কতবার কত সেশনে বলতে হবে?"*
        //
        // **আসল কারণ (কোড দেখে):** লাইনটার চেহারা ঠিকই ছিল, কিন্তু নিচের শর্তে
        // **দুটো ঘরই ফাঁকা হলে পুরো লাইনটাই তৈরি হত না** — তাই Patient কার্ডে
        // (এবং যে কোনো নতুন রেকর্ডে) লাইনটা একেবারে উধাও থাকত।
        //
        // **এখন:** তিনটে সেকশনেই (Enquiry · Visit · Patient) লাইনটা **সব সময়**
        // থাকে; তথ্য না থাকলে `—` বসে। ⛔ চেহারা · রং · মাপ · জায়গা — কিছুই
        // বদলানো হয়নি, শুধু লাইনটা আর লুকোয় না।
        // 🔒 TK-APPROVED (29.07.2026 সন্ধ্যা ৭.০০, ফটো-প্রুফে পাশ · খাতার সারি B94):
        // TK-এর কথা — *"LAST CALL … NEXT CALL … এগুলি remarks বক্সের মধ্যে রাখুন,
        // এক লাইনে হবে, তার নিচে একটা পাতলা লম্বা দাগ, তার নীচে রিমার্ক লেখা হবে।"*
        // তাই সারিটা এখানে **তৈরি হয়** কিন্তু আর `main`-এ বসে না — নিচে রিমার্ক
        // বাক্সের ভিতরে বসে। ⛔ সারিটার নিজের চেহারা · লেখা · মাপ · দুই অর্ধেকে
        // ভাগ হওয়া — কিছুই বদলানো হয়নি, শুধু জায়গা বদলেছে।
        // ⛔ এটা **তিন রকম কার্ডেই** (Enquiry · Visit · Patient) একসঙ্গে হয়,
        //    কারণ কার্ড তৈরির এই কোডটাই তিন সেকশন ব্যবহার করে।
        var statusRowForBox: android.widget.LinearLayout? = null
        /* 🟢🔒 V874 (৩০.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত):
           রেজিস্টার হওয়া কার্ডে REGISTERED-এর লাইনটা আগের মতোই থাকে (TK-এর
           পুরোনো লক করা নিয়ম), আর তার **ঠিক নিচে** LAST CALL-এর তারিখ · সময় ·
           কে করেছিল — একটা নতুন লাইনে।
           ⛔ TK: *"NEXT CALL এ time থাকবে না"* ⇒ NEXT CALL-এ শুধু তারিখই। */
        var lastCallLineForBox: android.widget.TextView? = null
        run {
            val statusRow = ll(android.widget.LinearLayout.HORIZONTAL).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val statusLp = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)
            statusLp.topMargin = 0
            statusRow.layoutParams = statusLp

            // 🚨 TK-REPORTED আবার (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B39):
            // TK-এর নিজের লেখা উদাহরণ — `LAST CALL 21.07.2026 (JPE-CRP)`।
            // স্টাফের নাম এখন **বন্ধনীর ভিতরে**, আগে খালি ফাঁক দিয়ে বসত।
            // 🔒 TK-FINAL, ফটো-প্রুফে লক (28.07.2026 · খাতার সারি B39):
            //   `LAST CALL 21.07.2026 (JPE-CRP)`  ......  `NEXT CALL 29.07.2026`
            // স্টাফের কোডটা **আলাদা হালকা সোনালি রঙে**।
            // ⛔ লাল ইচ্ছে করেই নেওয়া হয়নি — এই অ্যাপে লাল মানে Overdue/সতর্কতা,
            // স্টাফের নামেও লাল দিলে দুটো গুলিয়ে যেত।
            /* 🆕🔒 V851 (৩০.০৮.২০২৬, TK-অনুমোদিত) — TK: "যেগুলো এনকোয়ারি কার্ড
               সেগুলোতে লাস্ট কল থাকবে; যেগুলো রেজিস্ট্রেশন করা হয়েছে সেখানে
               লিখতে হবে কত তারিখে রেজিস্ট্রেশন হয়েছে এবং কে করেছিল"।
               V850-এ Draft-এর কার্ডে বসেছে, এখানেও একই নিয়ম (খাতার নিয়ম ৬.২)।
               ⛔ `regDate` ফাঁকা (নিছক এনকোয়ারি) হলে নিচের সবটা **হুবহু আগের
                  মতোই** চলে — এনকোয়ারি কার্ড এক অক্ষরও বদলায়নি। */
            /* 🔒 V931 (৩১.০৮.২০২৬, TK ডেমো প্রুফ দেখে "হ্যাঁ পাশ, দুটোই বসিয়ে দিন") —
               TK: *"Registered এর তারিখ এবং কে করেছে এখানে থাকবে না — View All
               এ ক্লিক করলে দেখা যায় শুধুমাত্র সেটাই থাকবে"*।
               ⚠️ ৩০.০৮.২০২৬-এ TK এই লাইনটা *"যা আছে তাই থাকবে"* বলে বন্ধ
                  করেছিলেন; ৩১.০৮-এ তিনি নিজে উল্টো নির্দেশ দিয়েছেন।
               ⛔ তথ্য কোথাও মোছা হয়নি — View All-এ দুটোই আগের মতোই আছে।
               ⇒ `isReg` এখন সবসময় false, তাই কার্ডে শুধু LAST CALL-এর লাইন।
                 (নামটা রাখা হলো, নিচের `whoRaw` ওই একই ধারায় চলে।) */
            val isReg = false
            val whoRaw = if (isReg) item.regBy.trim() else item.lastCallBy.trim()
            val lastText = if (isReg) {
                if (whoRaw.isNotBlank()) "REGISTERED ${FollowUpModel.displayDate(item.regDate)} ($whoRaw)"
                else "REGISTERED ${FollowUpModel.displayDate(item.regDate)}"
            } else if (item.lastCallDate.isNotBlank()) {
                /* 🔵🔒 V543 (২২.০৮.২০২৬, TK-নির্দেশ): *"শুধুমাত্র RMP সেকশনে নয়,
                   Follow-up সেকশনেও একই নিয়ম থাকবে"* — LAST CALL-এ তারিখের
                   সাথে সময়, তারপর স্টাফের নাম। NEXT CALL-এ সময় নয়।
                   ⛔ সময় না থাকলে (২০.০৭.২০২৬-এর আগের কল) লাইনটা **হুবহু
                      আগের মতোই** শুধু তারিখ দেখায়। */
                if (whoRaw.isNotBlank()) "LAST CALL ${fuLastWhen(item)} ($whoRaw)"
                else "LAST CALL ${fuLastWhen(item)}"
            } else "LAST CALL \u2014"

            // 🔒 TK-FINAL (28.07.2026, খাতার সারি B39): লেখা ছোট — "NEXT CALL"।
            // আগে "Next Follow up Call" এত লম্বা ছিল যে বাঁ দিকের স্টাফ-কোড কেটে যেত।
            val nextLabel = "NEXT CALL"
            // খাতার সারি B49: তারিখ না থাকলেও লেখাটা থাকে — `NEXT CALL —`।
            val nextText = if (item.nextFollow.isNotBlank())
                "$nextLabel ${FollowUpModel.displayDate(item.nextFollow)}" else "$nextLabel \u2014"

            // 🚨 TK-REPORTED আবার (28.07.2026, ফটো-প্রুফসহ · খাতার সারি B39):
            // *"LAST CALL 21.07.2026 (JPE-CRP)   NEXT FOLLOW UP CALL 29.07.2026 —
            //  এরকম থাকার কথা ছিল। গত সেশনেও ফাইনাল লক করা হলো, তারপরও করেন নাই।"*
            //
            // **আসল কারণ:** দুই অর্ধেককে ঠিক **৫০-৫০ ভাগ** (দুটোরই ওজন ১) করে
            // দেওয়া হয়েছিল। বাঁ দিকের লেখা ছোট (`Last Call —`) হলেও অর্ধেক জায়গা
            // দখল করে থাকত, আর ডান দিকের লম্বা লেখাটা ওই অর্ধেকে ধরত না — তাই
            // **"Next"-এর শুরুটা কেটে যেত** (TK-এর ফটোয় `xt Follow up Call`)।
            // ছোট হওয়ার সীমা ৭sp, তার নিচে আর ছোট হতে পারত না।
            //
            // **এখন:** ডান দিকের লেখাটা **যতটুকু দরকার ঠিক ততটুকুই জায়গা নেয়**,
            // তাই ওটা কখনোই কাটবে না। বাঁ দিকের লেখা বাকি পুরো জায়গা পায় এবং
            // দরকারে নিজে ছোট হয়। দুটোর মাঝে ফাঁক আছে, তাই গায়ে লাগবে না।
            val rightText = tv(nextText, 8f, "#344054", true).apply {
                setSingleLine(true)
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                // যতটুকু দরকার ততটুকু — এটাই কাটা বন্ধ করার আসল চাবি।
                layoutParams = android.widget.LinearLayout.LayoutParams(WRAP, WRAP)
            }

            // স্টাফের কোডের অংশটুকু আলাদা রঙে — বাকি লেখা আগের রঙেই।
            val lastStyled: CharSequence = if (whoRaw.isNotBlank() && (isReg || item.lastCallDate.isNotBlank())) {
                val open = lastText.lastIndexOf("(")
                if (open >= 0) android.text.SpannableString(lastText).apply {
                    setSpan(
                        android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#B8860B")),
                        open, lastText.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else lastText
            } else lastText

            val leftText = tv(lastStyled, 8f, "#344054", true).apply {
                // 🚨 TK-REPORTED, LIVE (29.07.2026 — TK-এর ছবি, ANIKUL HAQUE):
                //     `LAST CALL 28.07.2026 (KNE-KISHAN...` — **স্টাফের কোড কেটে গেছে**।
                // TK: *"এখানে স্টাফের কোড নাম্বার কেটে গেল কেন? সম্পূর্ণ প্রজেক্টে
                // এরকম সমস্যা যেন দেখা না দেয়।"*  · খাতার সারি B63
                //
                // **আগে কী ছিল:** এক লাইনে জোর করে ধরানোর চেষ্টা — `setSingleLine`
                // + শেষে `...` + লেখা নিজে থেকে ছোট হওয়ার ব্যবস্থা (৫–৮sp)।
                // Android-এ ওই নিজে-ছোট-হওয়ার ব্যবস্থা `setSingleLine` +
                // `ellipsize` -এর সঙ্গে ভরসা করা যায় না — তাই ছোট না হয়ে লেখাটা
                // কেটেই গেছে (TK-এর ছবিই তার প্রমাণ)।
                //
                // **এখন:** কাটার কোনো ব্যবস্থাই রাখা হয়নি। ছোট কোড আগের মতোই
                // এক লাইনে হুবহু আগের চেহারায় বসে; **কোড লম্বা হলে দ্বিতীয়
                // লাইনে নেমে যায়** — কিন্তু কখনো কাটে না।
                maxLines = 2
                ellipsize = null
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                val p = android.widget.LinearLayout.LayoutParams(0, WRAP, 1f)
                p.marginEnd = dpx(6)
                layoutParams = p
            }
            statusRow.addView(leftText)
            statusRow.addView(rightText)

            statusRowForBox = statusRow

            /* 🟢🔒 V874 — শুধু রেজিস্টার হওয়া কার্ডে, আর কল হয়ে থাকলে।
               ⛔ এনকোয়ারি কার্ডে LAST CALL উপরের লাইনেই থাকে (আগের মতোই),
                  তাই সেখানে এই দ্বিতীয় লাইনটা বসে না — একই কথা দু'বার নয়। */
            if (isReg && item.lastCallDate.isNotBlank()) {
                val lcWho = item.lastCallBy.trim()
                val lcText = if (lcWho.isNotBlank()) "LAST CALL ${fuLastWhen(item)} ($lcWho)"
                             else "LAST CALL ${fuLastWhen(item)}"
                val lcStyled: CharSequence = if (lcWho.isNotBlank()) {
                    val open = lcText.lastIndexOf("(")
                    if (open >= 0) android.text.SpannableString(lcText).apply {
                        setSpan(
                            android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#B8860B")),
                            open, lcText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    } else lcText
                } else lcText
                lastCallLineForBox = tv(lcStyled, 8f, "#344054", true).apply {
                    maxLines = 2
                    ellipsize = null
                    gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                    val p = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)
                    p.topMargin = dpx(3)
                    layoutParams = p
                }
            }
        }

        // Last Remark box (dashed, light green) — tap to edit remark
        // TK-APPROVED (2026-07-25, via photo proof): "Last Remark:" label
        // removed -- the box's own light-green color now makes it
        // recognizable at a glance, applied consistently project-wide.
        // TK-REPORTED, REPEATEDLY (last on 2026-07-26): the Remark box must run
        // the FULL width of the card, from the left edge to the right edge,
        // exactly like the button row underneath it.
        // WHY IT KEPT COMING BACK: the box was being added to "main", which is
        // only the RIGHT-HAND column of the card (the 70dp signal/photo column
        // sits to its left), so it could never reach the left edge. An earlier
        // attempt changed item_followup_card.xml . but this screen does not
        // use that layout file at all, it builds the card in code here, so
        // that change was invisible on the phone.
        // FIX: the box is now added to "card" itself, between the top row and
        // the button row . the same parent the buttons use, so both start and
        // end at exactly the same place. Nothing else about the card changed.
        // 🔒 খাতার সারি B94 (TK, 29.07.2026 সন্ধ্যা ৭.০০ — ফটো-প্রুফে পাশ):
        // রিমার্কের বাক্সটা এখন **তিনটে জিনিস ধরে** —
        //   (১) উপরে এক লাইনে  `LAST CALL … (STAFF)`  …  `NEXT CALL …`
        //   (২) তার নিচে একটা **পাতলা লম্বা দাগ**
        //   (৩) তার নিচে রিমার্কের লেখা
        // ⛔ **উচ্চতা নিজে থেকেই বাড়ে** — রিমার্ক দুই বা তিন লাইন হলে বাক্সটাও
        //    সেই অনুযায়ী লম্বা হয় (`WRAP` উচ্চতা, কোনো `maxLines` নেই, কোথাও
        //    কাটে না)। TK-এর নির্দেশ ঠিক এটাই।
        // ⛔ বাক্সের রং · ড্যাশ-বর্ডার · চওড়া · চাপ দিলে রিমার্ক খোলা — সব আগের
        //    মতোই। শুধু ভিতরে দুটো জিনিস যোগ হলো।
        // 🔒 খাতার সারি B184 (TK, 30.07.2026 বিকেল ৪.৫০): *"Remarks লেখার ঘরে
        // এত জায়গা থাকা সত্ত্বেও Remarks লেখা উপর-নিচে কেন? সেটা এক লাইনে
        // থাকতে হবে, যদি এক লাইনে না ধরে তবে দু-লাইনে হবে।"*
        // **আসল কারণ:** স্টাফ রিমার্ক লেখার সময় Enter চাপলে লেখাটার ভিতরেই
        // লাইন-ভাঙা (`\n`) ঢুকে যেত, তাই জায়গা থাকা সত্ত্বেও "KAL AAYENGE" ও
        // "KABJ HOTA HAI" আলাদা লাইনে বসত।
        // **সমাধান (শুধু দেখানোর জন্য):** কার্ডে দেখানোর সময় লাইন-ভাঙা ও
        // বাড়তি ফাঁকা জায়গা একটা করে স্পেস হয়ে যায় — তাই লেখা এক লাইনে বসে,
        // আর সত্যিই লম্বা হলে নিজে থেকেই পরের লাইনে গড়ায়।
        // ⛔ **ডেটাবেসে সেভ থাকা রিমার্ক এক অক্ষরও বদলায় না** — এডিট পপ-আপে
        //    স্টাফ আগের মতোই হুবহু নিজের লেখাটাই দেখেন ও সেভ করেন।
        val remarkOneLine = item.lastRemark.replace(Regex("\\s+"), " ").trim()
        val remarkText = tv(remarkOneLine.ifBlank { "No remark" }, 12f, "#10223A").apply {
            val p = android.widget.LinearLayout.LayoutParams(MATCH, WRAP)
            p.topMargin = dpx(5)
            layoutParams = p
        }
        val remarkBox = ll(android.widget.LinearLayout.VERTICAL).apply {
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_remark_dashed)
            setPadding(dpx(10), dpx(7), dpx(10), dpx(8))
            val p = android.widget.LinearLayout.LayoutParams(MATCH, WRAP); p.topMargin = dpx(6); layoutParams = p
            statusRowForBox?.let { addView(it) }
            lastCallLineForBox?.let { addView(it) }   // 🟢 V874
            // পাতলা লম্বা দাগ — বাক্সের এক প্রান্ত থেকে আরেক প্রান্ত পর্যন্ত।
            addView(android.view.View(this@FollowUpActivity).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#A8D8BC"))
                val lp = android.widget.LinearLayout.LayoutParams(MATCH, dpx(1))
                lp.topMargin = dpx(6)
                layoutParams = lp
            })
            addView(remarkText)
            setOnClickListener { showRemarkDialog(item) }
        }
        remarkText.setOnClickListener { showRemarkDialog(item) }

        // Action buttons: Call / WhatsApp / View / Next
        val btnRow = ll(android.widget.LinearLayout.HORIZONTAL)
        val brlp = android.widget.LinearLayout.LayoutParams(MATCH, WRAP); brlp.topMargin = dpx(8); btnRow.layoutParams = brlp
        fun actionBtn(emoji: String, bg: Int, last: Boolean, action: () -> Unit) {
            val b = android.widget.TextView(this)
            b.text = emoji; b.textSize = 15f; b.gravity = android.view.Gravity.CENTER
            b.setTextColor(android.graphics.Color.WHITE)
            b.setBackgroundResource(bg)
            // TK item 8 (2026-08-01): four bottom action buttons made 20% shorter
            // (34dp → 27dp). Colours already per spec (Call/WhatsApp green, View
            // blue); the Next button is set to the same blue below. Equal 5dp gap
            // + card's own 10dp padding as the slight outer margin — unchanged.
            val lp = android.widget.LinearLayout.LayoutParams(0, dpx(27), 1f)
            if (!last) lp.marginEnd = dpx(5)
            b.layoutParams = lp
            b.setOnClickListener {
                try { action() } catch (e: Throwable) {
                    Toast.makeText(this, "Couldn't open — try again (${e.javaClass.simpleName})", Toast.LENGTH_SHORT).show()
                }
            }
            btnRow.addView(b)
        }
        // 🔒 খাতার সারি B51 (TK, 28.07.2026): কল-বোতামে চাপ দেওয়ামাত্র নামটা
        // ফোনের ঘরে জমা হয় ("এর রিমার্ক লেখা বাকি")। আগে শুধু নিচের চলতি
        // স্মৃতিতে থাকত, তাই কল লম্বা হলে Android অ্যাপ মেরে দিলে মনে করানোটা
        // হারিয়ে যেত। ⛔ কল-গোনা বা টাকার কোনো হিসাবে হাত পড়ে না।
        actionBtn("\uD83D\uDCDE", com.tkbiswas.pilesclinic.R.drawable.bg_action_call, false) {
            pendingCallItem = item
            // 🆕 (03.08.2026, TK-নির্দেশ, B360) — "staff করবে শুধু মাত্র তাকেই
            // দেখাতে হবে। Master/Doctor-কে যেন না দেখায়।" তাই role স্টাফ না
            // হলে এই এন্ট্রিই তৈরি হয় না — Master/Doctor কল করলে কোনো
            // রিমার্ক-মনে-করানো কারো কাছেই আসবে না।
            if (user.role.equals("staff", ignoreCase = true)) {
                try { PendingRemarkStore.add(this@FollowUpActivity, item, user.mobile) } catch (_: Throwable) { }
            }
            callNumber(item.mobile)
        }
        actionBtn("\uD83D\uDCAC", com.tkbiswas.pilesclinic.R.drawable.bg_action_whatsapp, false) { openWhatsApp(item.mobile) }
        actionBtn("\uD83D\uDC41", com.tkbiswas.pilesclinic.R.drawable.bg_action_view, false) { openTimelineFor(item) }
        // TK item 8 (2026-08-01): Next Call button uses the SAME blue as View
        // (bg_action_view) instead of the purple bg_action_next. Localised to
        // this card only; the shared bg_action_next drawable (used by
        // dashboard/doctor/queue layouts) is deliberately left untouched.
        actionBtn("\u279C", com.tkbiswas.pilesclinic.R.drawable.bg_action_view, true) { pickNextFollow(item) }
        topRow.addView(main)
        card.addView(topRow)
        card.addView(remarkBox)
        card.addView(btnRow)
        container.addView(card)
    }

    /**
     * 🔒🔒 খাতার সারি B184 (TK, 30.07.2026 বিকেল ৪.৫০ — ফটো-প্রুফে ফাইনাল):
     * *"চারটা বক্সে যে লেখা থাকবে সেগুলির ফ্রন্ট একই সাইজের হতে হবে · বক্সের
     * সাইজ লেখার সাথে সামঞ্জস্যপূর্ণ থাকতে হবে · একই কালারের হতে হবে · কোনোটা
     * যেন কোনোটার গায়ে এসে না যায় · কোনো কিছু যেন অন্য কিছুর লেখার উপরে উঠে
     * না যায়।"*
     *
     * **আগে কী ছিল (এবং কেন কেটে যেত):** সব ট্যাগ **এক লাইনে** জোর করে ধরানো
     * হত — না কুলোলে আগে সবার ফন্ট ছোট করা হত, তারপরও না কুলোলে ব্র্যাঞ্চের
     * সংক্ষিপ্ত কোড (JPE), আর একদম শেষে প্রতিটা ট্যাগের প্রস্থ বেঁধে দিয়ে
     * "…" দিয়ে কেটে ফেলা হত। TK-এর ছবিতে ঠিক সেটাই দেখা গেছে —
     * `JPE | HYDROC… | UNEXPE…`।
     *
     * **এখন:** ফন্টের মাপ (১০.৫) · রং · প্যাডিং — সব ট্যাগে **হুবহু এক ও
     * অপরিবর্তিত**; প্রতিটা বাক্স নিজের লেখার মাপেই থাকে; আর জায়গায় না কুলোলে
     * পুরো ট্যাগটা **নিচের লাইনে নেমে যায়**। ⛔ কোনো ট্যাগ কখনো কাটে না,
     * কার্ডের বাইরে বেরোয় না, বা আরেকটার গায়ে ঘেঁষে না — গাণিতিকভাবেই সম্ভব নয়,
     * কারণ একটা সারিতে ততটুকুই বসে যতটুকু মাপা প্রস্থে ধরে।
     *
     * ⛔ `Paint.measureText()` দিয়ে হিসাব (আলাদা measure-পাস ছাড়াই), তাই
     *    প্রতিটা কার্ডে একবারই সস্তায় চলে। ⛔ ট্যাগে চাপ দেওয়ার নিয়ম
     *    (ট্রিপল-ট্যাপ এডিট · ঠিকানা-ট্যাগে এক-ট্যাপ) হুবহু অক্ষত — ভিউগুলোই
     *    সরানো হয়, নতুন করে বানানো হয় না, তাই তাদের লিসেনারও সঙ্গে যায়।
     */
    /* 🟣🔒 V707 — এখন **দল ধরে** সাজায়: প্রতিটা দল নিজের সারিতে শুরু হয়।
       ⛔ একই দলের ভিতরে জায়গা মাপার হিসাব এক অক্ষরও বদলায়নি — না কুলোলে
          আগের মতোই নিচের লাইনে নামে। */
    private fun layoutTagsInRows(
        wrap: android.widget.LinearLayout,
        groups: List<List<android.widget.TextView>>
    ) {
        val WRAPC = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        val avail = wrap.width - wrap.paddingLeft - wrap.paddingRight
        if (avail <= 0 || groups.isEmpty()) return

        fun newRow(): android.widget.LinearLayout {
            val r = android.widget.LinearLayout(this)
            r.orientation = android.widget.LinearLayout.HORIZONTAL
            r.gravity = android.view.Gravity.CENTER_VERTICAL
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, WRAPC
            )
            if (wrap.childCount > 0) lp.topMargin = dpx(5)
            r.layoutParams = lp
            return r
        }
        fun newSep(): android.widget.TextView {
            // ⛔ সংশোধন (30.07.2026 বিকেল ৪.৪৫): এখানে আগে `tv(...)` ডাকা
            //    হচ্ছিল, কিন্তু ওই সহায়ক ফাংশনটা `buildFollowCard`-এর **ভিতরের**
            //    ফাংশন — এই ক্লাস-লেভেল ফাংশন থেকে সেটা দেখাই যায় না, তাই
            //    Android Studio-তে বিল্ড ভাঙত। এখন সেপারেটরটা এখানেই তৈরি হয়,
            //    চেহারা হুবহু আগের মতোই (একই লেখা · মাপ · রং · মার্জিন)।
            val t = android.widget.TextView(this)
            t.text = "|"
            t.textSize = 12f
            t.setTextColor(android.graphics.Color.parseColor("#98A2B3"))
            val p = android.widget.LinearLayout.LayoutParams(WRAPC, WRAPC)
            p.marginStart = dpx(6); p.marginEnd = dpx(6)
            t.layoutParams = p
            return t
        }

        val sepSample = newSep()
        val sepW = sepSample.paint.measureText("|").toInt() + dpx(12)

        // ⛔ আগে সব ভিউ খুলে নেওয়া হয় (নইলে "already has a parent" হয়)।
        for (i in wrap.childCount - 1 downTo 0) {
            val c = wrap.getChildAt(i)
            if (c is android.view.ViewGroup) c.removeAllViews()
        }
        wrap.removeAllViews()

        var row = newRow()
        var used = 0
        for ((gi, group) in groups.withIndex()) {
        // 🟣 V707 — নতুন দল মানে নতুন সারি (আগের সারিতে জায়গা থাকলেও)।
        if (gi > 0 && row.childCount > 0) { wrap.addView(row); row = newRow(); used = 0 }
        for (p in group) {
            (p.parent as? android.view.ViewGroup)?.removeView(p)
            p.maxLines = 1
            p.ellipsize = null
            p.layoutParams = android.widget.LinearLayout.LayoutParams(WRAPC, WRAPC)
            var w = p.paddingLeft + p.paddingRight + p.paint.measureText(p.text.toString()).toInt() + dpx(2)
            // ⛔ একটামাত্র ট্যাগই যদি পুরো চওড়ার চেয়ে বড় হয় (বাস্তবে ঘটে না,
            //    তবু পাহারা) — তখন সে দুই লাইনে বসে, বাক্স লম্বা হয়, কিন্তু
            //    কাটে না ও বাইরে বেরোয় না।
            if (w > avail) {
                p.maxLines = 2
                p.maxWidth = avail
                w = avail
            }
            if (row.childCount > 0 && used + sepW + w > avail) {
                wrap.addView(row); row = newRow(); used = 0
            }
            if (row.childCount > 0) { row.addView(newSep()); used += sepW }
            row.addView(p); used += w
        }
        }
        wrap.addView(row)
        wrap.requestLayout()
    }

    /**
     * 🔒🔒 খাতার সারি B172 (TK, 30.07.2026) — শুধু ঠিকানার ট্যাগটাই আলাদা করে
     * বদলানোর ছোট পপ-আপ। TK-এর নির্দেশ: *"শুধুমাত্র ওই ট্যাগ টুকু যেন
     * আলাদাভাবে পরিবর্তন করা যায়"* — তাই পুরো Registration/Visit ফর্ম
     * খোলে না, শুধু এইটুকুই।
     *
     * গ্রাম/পোস্ট/থানা/জেলা — যেগুলো রেজিস্ট্রেশনের ঠিকানায় সত্যিই আছে,
     * শুধু সেগুলোই বোতাম হিসেবে দেখানো হয় (আন্দাজে বসানো হয় না)। এনকোয়ারি-
     * স্তরে (এখনো রেজিস্ট্রেশন হয়নি) এসব আলাদা করে থাকেই না, তখন শুধু
     * নিজে-লিখুন ঘরটাই দেখা যায়।
     */
    private fun showAddressTagEditor(item: FollowUpItem) {
        val WRAP = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        val user = NativeSession.current(this) ?: return
        val pad = dpx(16)
        val parts = AddressTagRepository.parseAddress(item.address)

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
        }
        val info = android.widget.TextView(this).apply {
            // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে মোবাইল দুইবার
            // দেখাত (নামের জায়গায় ও পরে আবার) — এখন "UNKNOWN"।
            text = (item.name.ifBlank { "UNKNOWN" }) + " · " + item.mobile
            setTextColor(android.graphics.Color.parseColor("#10223A"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            textSize = 14f
        }
        container.addView(info)
        if (item.address.isNotBlank()) {
            container.addView(android.widget.TextView(this).apply {
                text = "Full address: " + item.address.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ) — শুধু দেখানোর সময় বড় হাতে; ডেটাবেসে কিছু বদলায় না।
                setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                textSize = 12.5f
                setPadding(0, dpx(4), 0, dpx(12))
            })
        }
        val orderedKeys = listOf("VILLAGE", "POST", "THANA", "DISTRICT")
        val input = EditText(this).apply {
            setText(item.addressTag)
            hint = "Type your own"
        }
        if (parts.isNotEmpty()) {
            container.addView(android.widget.TextView(this).apply {
                text = "Choose one for the card tag:"
                setTextColor(android.graphics.Color.parseColor("#10223A"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                textSize = 13f
                setPadding(0, 0, 0, dpx(6))
            })
            for (key in orderedKeys) {
                val value = parts[key] ?: continue
                val row = android.widget.TextView(this).apply {
                    text = "$key: $value"
                    setPadding(dpx(14), dpx(10), dpx(14), dpx(10))
                    textSize = 13.5f
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dpx(8).toFloat()
                        setColor(android.graphics.Color.parseColor("#EEF2F7"))
                    }
                    setTextColor(android.graphics.Color.parseColor("#10223A"))
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, WRAP
                    ); lp.topMargin = dpx(6); layoutParams = lp
                    setOnClickListener { input.setText(value) }
                }
                container.addView(row)
            }
            container.addView(android.widget.TextView(this).apply {
                text = "\u270F\uFE0F Or type your own:"
                setTextColor(android.graphics.Color.parseColor("#10223A"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                textSize = 13f
                setPadding(0, dpx(12), 0, dpx(6))
            })
        }
        container.addView(input)
        val scroller = android.widget.ScrollView(this).apply { addView(container) }

        val header = PremiumAlert.header(this, "\uD83D\uDCCD Address Tag")
        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(header)
            .setView(scroller)
            .setPositiveButton("Save") { _, _ ->
                val chosen = input.text.toString().trim()
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        AddressTagRepository.saveTag(item.mobile, chosen, user.mobile)
                    }
                    android.widget.Toast.makeText(
                        this@FollowUpActivity,
                        if (ok) "Address tag saved" else "Saved on this phone — will sync when network is back",
                        android.widget.Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    // ⛔ ব্যর্থ হলেও পর্দায় সঙ্গে সঙ্গে দেখানো হয় (TK-এর লক করা
                    // নিয়ম — "আমার ফোনে যা করলাম তা সাথে সাথে দেখাবে")। ব্যর্থ
                    // লেখাটা `SupabaseClient.upsert()`-এর নিজের নিয়মেই
                    // `CloudWriteQueue`-তে জমা থেকে পরে আবার চেষ্টা হয়।
                    loadTab(currentStage)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        // 🔒🔒 খাতার সারি B158/B177-এর লক করা নিয়ম: নতুন পপ-আপে
        // `PremiumAlert.paint()` বাধ্যতামূলক — এটাই বাংলা-বন্ধ স্টাফের
        // (KNE-KISHAN5) পর্দায় এই পপ-আপ ঢেকে দেয়, আর বোতামের রংও এটাই
        // ঠিক করে (তাই আলাদা করে হাতে বোতামের রং বসাতে হয়নি)।
        dialog.show()
        PremiumAlert.paint(dialog)
    }

    /** ➜ button: pick a Next Follow-up date and save it (matches web nextFollowDate). */
    // ─────────────────────────────────────────────────────────────────────
    // NEW (2026-08-07, TK-approved "একদিন আগে আসার কথা" feature).
    // Shared next-date flow used by BOTH the ➜ button and the mandatory
    // post-remark prompt. Stage behaviour:
    //   • Inquiry (Enquiry): ask আসবে / শুধু ফোন করব first.
    //       "আসবে" → chamber-day calendar + marks আসার কথা (day-before call).
    //       "ফোন"  → any-day calendar, saves the CALL date only (old rule).
    //   • Patient (Visit) & Treatment (Patient): any-day calendar (chamber days
    //       highlighted) + marks আসার কথা.
    // ⛔ Until 2026-08-07 only Treatment marked আসার কথা (locked 2026-07-27);
    //   TK changed it so a Visit date now counts too. updateNextFollow and the
    //   Treatment-only patient WhatsApp are unchanged.
    private fun startNextFollowDate(item: FollowUpItem, mandatory: Boolean) {
        val defaultIso = if (item.nextFollow.isNotBlank() && item.nextFollow >= FollowUpModel.today())
            item.nextFollow else null
        if (item.stage == "Inquiry") {
            showComeOrCallChooser(
                mandatory = mandatory,
                onCome = {
                    /* 📵🔒 V1332 (১১.০৯.২০২৬) → 🟢 V1488 (১৫.০৯.২০২৬, TK-নির্দেশ) —
                       পেশেন্ট সত্যিই আজ আসতে পারেন, তাই Expected Date (আসার তারিখ)
                       কখনো "আজ" বন্ধ করে না। শুধু "পরের ফোন কবে?"-তেই (নিচে) আজ
                       বন্ধ থাকে, যাতে পরের কল আজই আবার বকেয়া না হয়ে যায়। */
                    ChamberCalendarDialog.show(
                        this, item.branch, "Expected Date?",
                        chamberOnly = true, initialIso = defaultIso, mandatory = mandatory,
                        blockToday = false
                    ) { iso -> saveNextFollowDate(item, iso, markExpected = true) }
                },
                onCallOnly = {
                    ChamberCalendarDialog.show(
                        this, item.branch, NoBengali.s("পরের ফোন কবে?"),
                        chamberOnly = false, initialIso = defaultIso, mandatory = mandatory,
                        blockToday = mandatory
                    ) { iso -> saveNextFollowDate(item, iso, markExpected = false) }
                },
                /* 📵🔒 V718 (২৬.০৮.২০২৬) — তখন TK বলেছিলেন *"enquiry visit এই সমস্ত
                   ক্ষেত্রে হবে না, কারণ পাঁচ বার ফোন কল করার পরে অটোমেটিক রিজেক্ট
                   হয়ে যায়"*, তাই Enquiry-তে বোতামটা বসানো হয়নি।
                   📵🔒 V1219 (০৮.০৯.২০২৬, TK-রিপোর্ট ও অনুমতি, হুবহু): *"Today Pending
                   Call-এ দেখাচ্ছে, কিন্তু আমরা তাকে আর কল করতে চাই না — সেখানে
                   অপশনটা আসছে না"* → *"হ্যাঁ পাশ, Enquiry-তেও বসিয়ে দিন সাবধানে"*।
                   ⇒ এখন Enquiry ধাপেও বোতামটা বসে।
                   ⛔ পাঁচ-কলে অটো-রিজেক্টের পুরোনো নিয়ম এক অক্ষরও বদলায়নি — এটা
                      শুধু হাতে থামানোর একটা বাড়তি পথ, আগেরটার বদলে নয়।
                   ⛔ আসবে / শুধু ফোন করব — দুটো বোতামই আগের মতোই অক্ষত।
                   ⛔ উপ-লেখাটা Enquiry-র জন্য আলাদা — এঁদের "চিকিৎসা চলছে" নয়। */
                onNoMoreCalls = { saveNoMoreCalls(item) },
                noMoreCallsSub = "আর ফোন করা হবে না — কল-তালিকা ও ব্যানার থেকে সরে যাবে"
            )
        } else {
            /* 📵🔒 V711 — Visit/Patient কার্ডে বাছাইয়ের পর্দা নেই (এক চাপেই
               ক্যালেন্ডার খোলে, TK-এর পুরোনো নিয়ম)। তাই ওই এক চাপ বাঁচিয়ে
               রেখে বোতামটা **ক্যালেন্ডারের নিচেই** বসে — কোনো বাড়তি ধাপ নেই।
               ⛔ ক্যালেন্ডার · তারিখ বাছা · Set · Cancel — কিছুই বদলায়নি। */
            /* 📵🔒 V718 (২৬.০৮.২০২৬, TK-নির্দেশ) — **শুধু "Patient" ট্যাবের
               কার্ডেই** (অর্থাৎ যাঁরা সত্যিই ট্রিটমেন্ট করাচ্ছেন) এই বোতামটা।
               TK: *"শুধুমাত্র পেশেন্ট ট্যাগ লাগানো থাকলে … তাদের ক্ষেত্রে
               পরবর্তীতে ফোন কল না করলেও চলবে।"*
               ⛔ কোডে যাচাই করা: "Patient" ট্যাব = `stage "Treatment"`
                  (`binding.tabPatient.setOnClickListener { switchTab("Treatment") }`),
                  আর "Visit" ট্যাব = `stage "Patient"` — নাম দুটো উল্টো, তাই
                  আন্দাজে নয়, কোড ধরে মিলিয়ে নেওয়া হয়েছে।
               ⛔ Visit কার্ডে বোতামটা আর আসবে না; ক্যালেন্ডার · তারিখ বাছা ·
                  Set · Cancel — কিছুই বদলায়নি। */
            ChamberCalendarDialog.show(
                this, item.branch, NoBengali.s("পরের আসার দিন"),
                chamberOnly = false, initialIso = defaultIso, mandatory = mandatory,
                /* 🟢 V1488 (১৫.০৯.২০২৬) — এটাও Expected Date (markExpected=true,
                   নিচে দেখুন) — পেশেন্ট আজ আসতে পারেন, তাই আজ বন্ধ নয়। */
                blockToday = false,
                /* 📵🔒 V1206 (০৮.০৯.২০২৬, TK-রিপোর্ট: *"এই পেসেন্ট এর নো মোর কল
                   অপশন কেনো নেই"* — কার্ডটা ছিল **VISITED**)। V711-এ বোতামটা
                   শুধু Treatment ধাপে বসত; TK-র নিজের কথায় নিয়মটা ছিল *"কোন
                   পেশেন্ট যখন কন্টিনিউ পেশেন্ট অথবা কন্টিনিউ ট্রিটমেন্ট করাচ্ছে"* —
                   অর্থাৎ Visit-ও এর মধ্যে পড়ে। ⇒ এখন Patient (Visit) ধাপেও বসে।
                   ⛔ Enquiry ধাপে বসানো হয়নি — ওঁরা এখনো রোগীই নন; TK চাইলে
                      বসিয়ে দেওয়া যাবে (TK-কে জানানো হয়েছে)। */
                onNoMoreCalls = if (item.stage == "Treatment" || item.stage == "Patient")
                    ({ saveNoMoreCalls(item) }) else null
            ) { iso -> saveNextFollowDate(item, iso, markExpected = true) }
        }
    }

    /* ═══════════════════════════════════════════════════════════════════════
       📵🔒 V711 (২৬.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত)

       TK: *"কোন পেশেন্ট যখন কন্টিনিউ পেশেন্ট অথবা কন্টিনিউ ট্রিটমেন্ট করাচ্ছে,
       তাদেরকে আর ফোন না করলেও চলে — সেটার ব্যবস্থা কীভাবে করা যায়"*।

       ⛔ **নতুন কোনো কলাম বা SQL লাগেনি** — অ্যাপে নিয়মটা **আগে থেকেই আছে**:
          পরের কলের তারিখ (`nextFollow`) ফাঁকা থাকলে ড্যাশবোর্ডের ব্যানার ও
          Overdue — দুটোর কোনোটাই তাঁকে গোনে না
          (`DashboardActivity.isDue`: `nextFollow.isNotBlank() && ...`)।
          তাই এখানে শুধু তারিখটা **ফাঁকা করে দেওয়া হয়** — Supabase-এ খরচ
          এক পয়সাও বাড়ে না।
       ⛔ রোগীর কোনো তথ্য মোছা হয় না · টাকার হিসাবে হাত পড়ে না ·
          পুরোনো Remark/ইতিহাস সব অক্ষত।
       ⛔ ফেরানো সহজ: ➜ বোতামে চেপে আবার একটা তারিখ দিলেই তিনি আগের মতোই
          কল-তালিকায় ফিরে আসবেন।
       ═══════════════════════════════════════════════════════════════════════ */
    private fun saveNoMoreCalls(item: FollowUpItem) {
        android.widget.Toast.makeText(
            this, NoBengali.s("ঠিক আছে — এঁকে আর কল-তালিকায় দেখাবে না"),
            android.widget.Toast.LENGTH_SHORT
        ).show()
        BackgroundWork.run {
            // 📵 V1206 — থামানোটা এখন আলাদা করে মনে রাখা হয় (নইলে পরের রিমার্ক/
            //    টাকা বসলেই অ্যাপ নিজে থেকে আজকের তারিখ বসিয়ে নামটা ফিরিয়ে আনত)।
            val ok = try { repository.updateNextFollow(resolveFollowUpId(item), "", stop = true) } catch (_: Throwable) { false }
            if (ok && !isFinishing && !isDestroyed) {
                runOnUiThread { if (!isFinishing && !isDestroyed) loadTab(currentStage) }
            }
        }
    }

    /** Saves the chosen date exactly as before, then (when markExpected) writes
     *  the person's "আসার কথা" via the SAME shared markExpected used elsewhere. */
    /* 🔴🔒 V1222 ③ (০৮.০৯.২০২৬ — Laxmi-র রিপোর্ট, TK-র অনুমতি নিয়ে):
       "Next follow-up set" লেখাটা দেখানো হত সেভের **আগেই**, আর ফলটা কেউ কখনো
       মিলিয়ে দেখত না। তাই নেট দুর্বল হয়ে কাজটা শুধু ফোনে জমা হলেও স্টাফ
       "হয়ে গেছে" দেখে নিশ্চিন্তে পরের কলে চলে যেতেন।
       ⛔ **TK-র ২৮.০৭.২০২৬-এর নিয়ম অটুট রাখা হয়েছে** — *"তারিখ বেছে দেওয়ামাত্র
          স্টাফ পরের কাজে যেতে পারবেন, ক্লাউডের উত্তরের জন্য পর্দা আটকে থাকবে না"*।
          তাই সঙ্গে সঙ্গের বার্তাটা **আগের মতোই** আছে।
       ⇒ শুধু যোগ হলো: কাজটা সত্যিই ক্লাউডে **না বসলে** পরে একটা সৎ বার্তা —
         *"Not sent yet — saved on this phone, will send when online"*।
       ⛔ সেভের নিয়ম · তারিখ · কোনো হিসাব কিচ্ছু বদলায়নি। */
    private fun saveNextFollowDate(item: FollowUpItem, iso: String, markExpected: Boolean) {
        android.widget.Toast.makeText(this, "Next follow-up set", android.widget.Toast.LENGTH_SHORT).show()
        BackgroundWork.run {
            // 📵 V1206 — স্টাফ নতুন তারিখ বাছলেন ⇒ থামানো বাতিল, আবার চালু।
            val saved = repository.updateNextFollowSaved(resolveFollowUpId(item), iso, stop = false)
            if (!saved) runOnUiThread {
                if (!isFinishing && !isDestroyed) android.widget.Toast.makeText(
                    this@FollowUpActivity,
                    "Not sent yet — saved on this phone, will send when online",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            // ⛔ পুরনো আচরণ হুবহু: `updateNextFollow()` Activity-তে context থাকায়
            //    আগেও সবসময় true-ই ফেরাত, তাই নিচের ধাপগুলো আগের মতোই চলে।
            val ok = true
            if (ok && markExpected) {
                try {
                    ChamberAttendanceRepository.markExpected(
                        this@FollowUpActivity, item.mobile, item.name, item.branch, iso, user.mobile
                    )
                } catch (_: Throwable) {}
                if (item.stage == "Treatment") {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            PatientMessage.show(
                                this@FollowUpActivity, item.branch, item.name,
                                item.mobile, item.patientId,
                                PatientMessage.Kind.VISIT_DATE,
                                dateText = FollowUpModel.displayDate(iso)
                            )
                        }
                    }
                }
            }
            if (ok && !isFinishing && !isDestroyed) {
                runOnUiThread { if (!isFinishing && !isDestroyed) loadTab(currentStage) }
            }
        }
    }

    /** Two-choice card for an Enquiry follow-up: আসবে (চেম্বারে) vs শুধু ফোন করব.
     *  Non-dismissable in the mandatory (post-remark) flow. */
    private fun showComeOrCallChooser(
        mandatory: Boolean,
        onCome: () -> Unit,
        onCallOnly: () -> Unit,
        // 📵 V711 — ডিফল্ট null, তাই পুরোনো কোনো ডাক ভাঙে না।
        onNoMoreCalls: (() -> Unit)? = null,
        /* 📵 V1219 — বোতামের নিচের ছোট লেখাটা এখন বদলানো যায়। ডিফল্ট হুবহু
           আগেরটাই, তাই Treatment/Visit-এ এক অক্ষরও বদলায়নি। */
        noMoreCallsSub: String = "চিকিৎসা চলছে — কল-তালিকা ও ব্যানার থেকে সরে যাবে"
    ) {
        val d = android.app.Dialog(this)
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        d.setCancelable(!mandatory)
        d.setCanceledOnTouchOutside(!mandatory)
        val density = resources.displayMetrics.density
        fun dpp(v: Int) = (v * density).toInt()
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dpp(18), dpp(18), dpp(18), dpp(16))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE); cornerRadius = dpp(16).toFloat()
            }
        }
        root.addView(android.widget.TextView(this).apply {
            text = NoBengali.s("রোগী ফোনে কী বলল?")
            textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#101828"))
            setPadding(0, 0, 0, dpp(12))
        })
        fun bigBtn(title: String, sub: String, filled: Boolean, onClick: () -> Unit) {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpp(14).toFloat()
                if (filled) setColor(android.graphics.Color.parseColor("#12805C"))
                else { setColor(android.graphics.Color.WHITE); setStroke(dpp(2), android.graphics.Color.parseColor("#0B5E8A")) }
            }
            root.addView(android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                background = bg
                setPadding(dpp(16), dpp(14), dpp(16), dpp(14))
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, dpp(12)); layoutParams = lp
                isClickable = true; isFocusable = true
                addView(android.widget.TextView(this@FollowUpActivity).apply {
                    text = title; textSize = 16f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(if (filled) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#0B5E8A"))
                })
                addView(android.widget.TextView(this@FollowUpActivity).apply {
                    text = sub; textSize = 12f
                    setTextColor(if (filled) android.graphics.Color.parseColor("#DDF3E8") else android.graphics.Color.parseColor("#5A6B76"))
                    setPadding(0, dpp(3), 0, 0)
                })
                setOnClickListener { d.dismiss(); onClick() }
            })
        }
        // 🔒 B604 (TK-নির্দেশ): বিভ্রান্তিকর ⏰ (কিছু ফন্টে "Jul 17" আঁকে) বাদ।
        bigBtn(NoBengali.s("আসবে (চেম্বারে)"), NoBengali.s("চেম্বার-দিন বাছুন → একদিন আগে ফোন-রিমাইন্ডার"), true) { onCome() }
        bigBtn(NoBengali.s("📞 শুধু ফোন করব"), NoBengali.s("যেকোনো দিন → ওইদিনই ফোনের তারিখ"), false) { onCallOnly() }
        /* 📵🔒 V711 — তৃতীয় বোতাম (TK-এর অনুমোদিত ডেমো অনুযায়ী)। উপরের দুটো
           বোতাম **এক অক্ষরও বদলায়নি**। */
        if (onNoMoreCalls != null) {
            bigBtn(
                NoBengali.s("📵 আর কল লাগবে না"),
                NoBengali.s(noMoreCallsSub),
                false
            ) { onNoMoreCalls.invoke() }
        }
        d.setContentView(root)
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        d.window?.setLayout((resources.displayMetrics.widthPixels * 0.9f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        try { NoBengali.installDialog(d) } catch (_: Throwable) {}
        d.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(d) } catch (_: Throwable) { }   // 🤫 V774
    }

    private fun pickNextFollow(item: FollowUpItem) {
        startNextFollowDate(item, mandatory = false)
        /* OLD pickNextFollow body (pre 2026-08-07), kept for revert:
        val cal = java.util.Calendar.getInstance()
        // 🔴🔴🟢 খাতার সারি [পরবর্তী] (TK-রিপোর্ট, ছবিসহ — ক্যালেন্ডার খুললে
        // বিগত দিনের (আজকের আগের) তারিখ বেছে বসানো থাকত)। **আসল কারণ:**
        // পুরনো `item.nextFollow` (যেমন ২৫.০৭.২০২৬, আজ ০৫.০৮.২০২৬) যাই থাকুক
        // — সেটাই ক্যালেন্ডারের ডিফল্ট বাছাই হিসেবে বসানো হতো, তারিখ পার হয়ে
        // গেছে কিনা যাচাই না করেই। **ওয়েবে এই একই জায়গায় আগে থেকেই এই যাচাই
        // আছে** (`x.nextFollow>=today()` — `app.js`-এর `nextFollowDate()`),
        // Android-এ এতদিন বাদ পড়েছিল। **সমাধান:** পুরনো তারিখ শুধু তখনই
        // ডিফল্ট হবে যদি সেটা আজ বা তার পরের হয়; বিগত হলে আজকের তারিখই
        // ডিফল্ট থাকবে (`cal` এমনিতেই আজকের তারিখে শুরু হয়)। ⛔ তারিখ সেভ
        // হওয়ার লজিক/`updateNextFollow`/আসার কথা — কিছুই বদলায়নি, শুধু
        // ক্যালেন্ডার খোলার সময় কোন তারিখ প্রি-সিলেক্ট থাকবে তাই বদলেছে।
        val todayIso = FollowUpModel.today()
        try {
            if (item.nextFollow.isNotBlank() && item.nextFollow >= todayIso) {
                val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(item.nextFollow)
                if (d != null) cal.time = d
            }
        } catch (_: Exception) {}
        /* 🔴 V843 — নিচে `.apply { datePicker.minDate = ... }` দেখুন */
        DatePickerDialog(this, { _, y, m, day ->
            val iso = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, day)
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { repository.updateNextFollow(resolveFollowUpId(item), iso) }
                // TK-DECISION (2026-07-22): saving a nextFollow date ALSO
                // marks the person "আসার কথা" for that date automatically.
                // Written ONCE right here (not recomputed on every Chamber
                // board open), so it costs no extra free-plan quota.
                //
                // TK-CORRECTED (2026-07-27): this used to run for EVERY stage
                // except Enquiry -- so a Visit-card date also landed in আসার
                // কথা. TK's rule: "নেক্সট ফলো-আপ ডেট একমাত্র পেশেন্ট কার্ড
                // থেকে দিলেই হবে; ইনকোয়ারি আর ভিজিট কার্ডে ওটা শুধু ফোন করার
                // তারিখ।" So only the Patient card (stage "Treatment") now
                // marks আসার কথা. Enquiry and Visit only save the call date.
                if (ok && item.stage == "Treatment") {
                    withContext(Dispatchers.IO) {
                        ChamberAttendanceRepository.markExpected(
                            this@FollowUpActivity, item.mobile, item.name, item.branch, iso, user.mobile
                        )
                    }
                }
                android.widget.Toast.makeText(
                    this@FollowUpActivity,
                    if (ok) "Next follow-up set" else "Failed \u2014 check connection",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                if (ok) loadTab(currentStage)
            }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).apply {
            /* 🔴🔒 V843 (২৯.০৮.২০২৬, নিয়ম ৭ — একটা দোষ পেলে পুরো প্রজেক্টে):
               **পরের** তারিখ বাছার ঘরে অতীতের তারিখ বাছা যেত। V671-এর
               প্রমাণিত একই লাইন বসানো হলো।
               ⛔ অতীতের তারিখ শুধু ধূসর হয় — জমা থাকা পুরনো তারিখ মোছে না। */
            datePicker.minDate = System.currentTimeMillis() - 1000L
        }.show()
        ---- end old pickNextFollow body ---- */
    }
    /** 🔵 V543: `31.12.2026 : 12.30 PM` — সময় না থাকলে শুধু তারিখ।
     *  ⏰ V835: সময়ের ধাঁচ `3:15 PM` → `3.15 PM` (TK-নির্দেশ, ২৯.০৮.২০২৬)। */
    private fun fuLastWhen(item: FollowUpItem): String {
        val d = FollowUpModel.displayDate(item.lastCallDate)
        val t = PaymentModel.displayTime12Dot(item.lastCallTime)   /* ⏰ V835 — 3.15 PM */
        return if (t.isNotBlank()) "$d : $t" else d
    }



    private fun openPatientPhotoEditor(item: FollowUpItem) {
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)
        val intent = Intent(this, PatientPhotoActivity::class.java)
        intent.putExtra("mobile", digits)
        /* 🔵🔒 V530 (২২.০৮.২০২৬, TK-নির্দেশ): এক নম্বরে দু'জন আলাদা রোগী থাকলে
           **এই কার্ডটা কার**, সেটাই সাথে যায় — তাই অন্যজনের ছবি খুলবে না ও
           ভুল করে অন্যজনের ছবি বদলে যাবে না।
           ⛔ `refId`/`patientId` ফাঁকা হলে আচরণ হুবহু আগের মতোই।
           (হুবহু সেই দুটো ঘর যেগুলো `openPaymentFor` V520 থেকে পাঠায়।) */
        intent.putExtra("patientRowId", item.refId)
        intent.putExtra("patientCode", item.patientId)
        startActivity(intent)
    }

    /** Display-only formatting. Stored mobile and call/WhatsApp logic stay unchanged. */
    private fun formatMobileForDisplay(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            raw.trim().startsWith("+") -> raw.trim()
            else -> raw.trim()
        }
    }

    /** TK-REQUESTED (2026-07-27): "চাপ দিলে একবারেই কাজ হতে হবে, লোডিং হওয়ার
     *  জন্য আলাদা সময় নেবে না... অন্যান্য অ্যাপ যেমন স্মুথভাবে ওপেন হয়।"
     *
     *  This card ALREADY holds everything the Patient Details header shows --
     *  name, mobile, branch, disease, age, sex, address, Patient ID -- and
     *  it already knows which stage the patient is at. All of it is handed
     *  over with the tap, so that screen can draw its complete header, in the
     *  correct stage colour, the instant it opens, with no network call at
     *  all. The fresh copy still arrives in the background exactly as before
     *  and quietly replaces it, so nothing can go stale.
     *
     *  This is extra information only. Any screen that opens Patient Details
     *  without it behaves exactly as it does today. */
    private fun openTimelineFor(item: FollowUpItem) {
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)
        val intent = Intent(this, PatientTimelineActivity::class.java)
        intent.putExtra("mobile", digits)
        // TK APPROVED (2026-07-15): show only this tab's own entries; the
        // Dashboard/Global Search "View" still shows everything (no section extra).
        intent.putExtra("section", currentStage)
        intent.putExtra("preStage", item.stage)
        intent.putExtra("preName", item.name)
        intent.putExtra("preBranch", item.branch)
        intent.putExtra("preDisease", item.disease)
        intent.putExtra("preAge", item.age)
        intent.putExtra("preSex", item.sex)
        intent.putExtra("preAddress", item.address)
        intent.putExtra("prePatientId", item.patientId)
        startActivity(intent)
    }

    private fun openPaymentFor(mobile: String, rowId: String = "", patientCode: String = "") {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra("mobile", digits)
        /* 🔵🔒 V520 (২২.০৮.২০২৬): এক নম্বরে দুজন আলাদা রোগী থাকলে **এই কার্ডটা
           কার** সেটা সাথে পাঠানো হয়, তাই Payment ঠিক এই রোগীরই ফর্ম খোলে।
           ⛔ ফাঁকা থাকলে আচরণ হুবহু আগের মতোই। */
        intent.putExtra("patientRowId", rowId)
        intent.putExtra("patientCode", patientCode)
        startActivity(intent)
    }

    /** Visit "Advance" tap → direct Advance Payment popup for THIS patient
     *  (mirrors web openVisitAdvancePayment): TOTAL AMOUNT (locked once set,
     *  3-tap to unlock), Advance Payment, Payment Mode CASH/ONLINE, Save.
     *  On save the bill+payment are stored and the Visit card moves to Patient. */
    private fun showAdvancePaymentDialog(item: FollowUpItem) {
        // 🔒 B619 (11.08.2026, TK-নির্দেশ "ক"): যে ব্রাঞ্চের স্টাফ শুধু সেই ব্রাঞ্চের রোগীর
        // টাকা নিতে পারবে — অন্য ব্রাঞ্চের রোগী হলে টাকার ঘর খোলার আগেই আটকাই (সেভের
        // guard backstop হিসেবে অটুট)। কার্ডেই item.branch/item.patientId আছে।
        if (!MoneyBranchGuard.canTakeMoney(this@FollowUpActivity, item.branch, item.patientId)) {
            Toast.makeText(this@FollowUpActivity, MoneyBranchGuard.blockMessage(item.branch), Toast.LENGTH_LONG).show()
            return
        }
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)
        run {
            val pr = PaymentRepository(this@FollowUpActivity)
            // 🔒 TK-এর নিয়ম (28.07.2026): "কাজ করবো, হাতে হাতে কাজ হতে হবে।"
            // পপ-আপ এখন সঙ্গে সঙ্গে খোলে — কার্ডে যে নাম, ব্রাঞ্চ ও বিল আগে
            // থেকেই আছে তা দিয়ে। রোগীর আসল সারি পিছনে আসে, এসে গেলে সংখ্যা
            // নিজে থেকে ঠিক হয়ে যায়।
            // 💰 টাকার সুরক্ষা: আসল সারি হাতে না আসা পর্যন্ত Save কাজ করে না,
            // তাই আন্দাজের অঙ্কে কখনো সেভ হবে না।
            var patient: PatientBillInfo? = null
            var billTouched = false
            val view = layoutInflater.inflate(com.tkbiswas.pilesclinic.R.layout.dialog_advance, null)
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvAdvWho).text =
                // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে digits দুইবার দেখাত।
                "${item.name.ifBlank { "UNKNOWN" }} · ${digits} · ${item.branch}"
            val etTotal = view.findViewById<EditText>(com.tkbiswas.pilesclinic.R.id.etAdvTotal)
            val etAmount = view.findViewById<EditText>(com.tkbiswas.pilesclinic.R.id.etAdvAmount)
            val lock = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvAdvLock)
            val chipCash = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.chipCash)
            val chipOnline = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.chipOnline)
            val rowTotal = view.findViewById<android.widget.LinearLayout>(com.tkbiswas.pilesclinic.R.id.rowAdvTotal)
            val rowAmount = view.findViewById<android.widget.LinearLayout>(com.tkbiswas.pilesclinic.R.id.rowAdvAmount)
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

            // TK APPROVED (2026-07-15): tapping anywhere in the Advance box now
            // focuses it and opens the keyboard, not just the number itself.
            rowAmount.setOnClickListener {
                etAmount.requestFocus()
                imm.showSoftInput(etAmount, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }

            if (item.bill > 0) etTotal.setText("%.0f".format(item.bill))
            etTotal.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(e: android.text.Editable?) { billTouched = true }
                override fun beforeTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun onTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
            })
            // TK APPROVED (2026-07-15): lock only once an advance has actually
            // been PAID (paid > 0) for this patient — not just because "bill"
            // has some value from another source (e.g. Registration Fee). Once
            // an advance is genuinely saved here the patient is promoted out of
            // the Visit tab (ONE-NUMBER-ONE-SECTION rule), so a still-visible
            // Visit card should practically never show locked going forward.
            // তালা আসল সারি এলে বসানো হয় (নিচের BackgroundWork অংশে)।
            val trulyLocked = false
            if (trulyLocked) {
                etTotal.isFocusable = false; etTotal.isFocusableInTouchMode = false
                var bt = 0; var bl = 0L
                rowTotal.setOnClickListener {
                    val now = System.currentTimeMillis()
                    if (now - bl > 1200) bt = 0
                    bl = now; bt++
                    if (bt >= 3) { bt = 0; etTotal.isFocusable = true; etTotal.isFocusableInTouchMode = true; etTotal.requestFocus(); lock.visibility = View.GONE }
                }
            } else {
                lock.visibility = View.GONE
                // TK APPROVED (2026-07-15): tapping anywhere in the Total box now
                // focuses it and opens the keyboard, not just the number itself
                // (only while it's still unlocked/editable).
                rowTotal.setOnClickListener {
                    etTotal.requestFocus()
                    imm.showSoftInput(etTotal, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }

            // CASH / ONLINE chip toggle
            var mode = "CASH"
            fun paintChips() {
                val onBg = com.tkbiswas.pilesclinic.R.drawable.bg_chip_seg_on; val offBg = com.tkbiswas.pilesclinic.R.drawable.bg_chip_seg
                chipCash.setBackgroundResource(if (mode == "CASH") onBg else offBg)
                chipCash.setTextColor(android.graphics.Color.parseColor(if (mode == "CASH") "#FFFFFF" else "#5B6B81"))
                chipOnline.setBackgroundResource(if (mode == "ONLINE") onBg else offBg)
                chipOnline.setTextColor(android.graphics.Color.parseColor(if (mode == "ONLINE") "#FFFFFF" else "#5B6B81"))
            }
            chipCash.setOnClickListener { mode = "CASH"; paintChips() }
            chipOnline.setOnClickListener { mode = "ONLINE"; paintChips() }
            paintChips()

            UppercaseInputUtil.applyToAll(view)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
            val dialog = AlertDialog.Builder(this@FollowUpActivity).setView(view).setCancelable(true).create()
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnAdvClose).setOnClickListener { dialog.dismiss() }
            // TK-REPORTED CRITICAL BUG FIX (2026-07-26): saving a payment takes
            // a few seconds on a slow connection, and this button stayed fully
            // tappable that whole time. Staff saw "nothing happening" and
            // tapped Save again -- and again -- so TWO or THREE completely
            // separate payment rows were written for ONE real payment (each
            // gets its own new id, so nothing de-duplicated them). TK's own
            // proof: a patient who paid Rs 10,000 showed Advance + 2nd Payment
            // + 3rd Payment of Rs 10,000 each = Rs 30,000. This flag makes the
            // first tap the only one that counts until that save finishes.
            // Deliberately set AFTER validation passes, so a tap that was
            // rejected (blank/zero amount) never leaves the button stuck.
            var advSaving = false
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnAdvSave).setOnClickListener {
                if (advSaving) return@setOnClickListener
                // 💰 আসল সারি না এলে সেভ নয় — ভুল অঙ্ক কখনো লেখা হবে না।
                val patientNow = patient
                if (patientNow == null) {
                    Toast.makeText(this@FollowUpActivity, "Please wait — loading this patient's bill", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val enteredBill = etTotal.text.toString().filter { it.isDigit() }.toDoubleOrNull() ?: patientNow.bill
                val amount = etAmount.text.toString().filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                if (amount <= 0) {
                    // TK-REQUESTED (2026-07-24, staff-reported): a patient who
                    // hasn't paid any Advance yet should still be able to get
                    // their Bill recorded -- previously this just showed
                    // "Enter advance amount" and did nothing at all, so the
                    // Bill typed here was lost and the patient stayed stuck
                    // with no Bill on record. Reuses the SAME already-tested
                    // updateBillOnly() used for bill corrections elsewhere --
                    // bill-only, no payment row created, patient stays on the
                    // Visit tab exactly as before (no stage promotion; that
                    // still only happens on a real Advance, per TK's rule).
                    if (enteredBill > 0 && enteredBill != patientNow.bill) {
                        advSaving = true
                        lifecycleScope.launch {
                            val ok = try {
                                withContext(Dispatchers.IO) {
                                    pr.updateBillOnly(patientNow, enteredBill, patientNow.bill, user.mobile, user.name.ifBlank { user.mobile })
                                }
                            } finally { advSaving = false }
                            Toast.makeText(this@FollowUpActivity,
                                if (ok) "Bill saved" else "Save failed — check connection",
                                Toast.LENGTH_SHORT).show()
                            if (ok) {
                                dialog.dismiss(); applySearch()
                                // 🔒 TK-APPROVED (28.07.2026): বিল তৈরির খবর রোগীকে।
                                PatientMessage.show(
                                    this@FollowUpActivity, patientNow.branch, patientNow.name,
                                    patientNow.mobile, patientNow.patientId,
                                    PatientMessage.Kind.BILL,
                                    bill = enteredBill, paid = patientNow.paid,
                                    dateText = FollowUpModel.displayDate(FollowUpModel.today())
                                )
                            }
                        }
                    } else {
                        Toast.makeText(this@FollowUpActivity, "Enter advance amount, or just a Bill amount to save it without advance", Toast.LENGTH_SHORT).show()
                    }
                }
                // TK-DECISION (2026-07-26, final): an ADVANCE may be taken
                // WITHOUT a bill . the total is often decided later. The bill
                // is demanded at the 2nd payment instead.
                else if (!MoneyBranchGuard.canTakeMoney(this@FollowUpActivity, patientNow.branch, patientNow.patientId)) {
                    // 🔒 TK'S LOCKED RULE (27.07.2026): only the patient's own
                    // branch (staff or doctor) or Master may take money.
                    Toast.makeText(this@FollowUpActivity, MoneyBranchGuard.blockMessage(patientNow.branch), Toast.LENGTH_LONG).show()
                }
                else {
                    // 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত): আজ এই রোগীর নামে
                    // ইতিমধ্যে টাকা নেওয়া হয়ে থাকলে আগে একবার জিজ্ঞাসা। আজ কিছু
                    // নেওয়া না হলে কোনো পপ-আপ আসে না — সেভ হুবহু আগের মতোই।
                    /* 🔴 V1106 (TK-নির্দেশ) — সেভ চাপার মুহূর্তে ক্লাউডেও
                       একবার দেখা হয়, তাই অন্য ফোনে নেওয়া একই অঙ্কের টাকাও
                       ধরা পড়ে। ⛔ কিছুই আটকানো হয় না — শুধু প্রশ্ন। */
                    /* 🔴🔒 V1504 (১৫.০৯.২০২৬, TK-রিপোর্ট — RIMPA ROY-র ₹2,000
                       দুবার) — advSaving আগে শুধু "হ্যাঁ" চাপার পরে সেট হতো,
                       তাই প্রশ্ন ওঠার মুহূর্তেই (ক্লাউড-যাচাই শেষে) বোতাম
                       খোলা থাকত। এখন প্রশ্ন ওঠার আগেই তালা লাগে, "No, cancel"-এ খোলে। */
                    advSaving = true
                    PaymentDayGuard.confirmBeforeSave(
                        this@FollowUpActivity,
                        pr,
                        patientNow,
                        amount,
                        pr.paidOnDateFor(patientNow.id),
                        pr.nextLabelFor(patientNow.id),
                        mode = mode,   // 🔴 V1152 — একই অঙ্ক ও একই ধরন হলে তবেই সতর্কবার্তা
                        onProceed = {
                    lifecycleScope.launch {
                        val ok = try {
                            withContext(Dispatchers.IO) {
                                pr.saveTreatmentPayment(patientNow, enteredBill, amount, mode, "Advance Payment", user.mobile)
                            }
                        } finally { advSaving = false }
                        Toast.makeText(this@FollowUpActivity,
                            if (ok) "Advance saved — patient moved to Patient" else "Save failed — check connection",
                            Toast.LENGTH_SHORT).show()
                        if (ok) {
                            dialog.dismiss(); switchTab("Treatment")
                            // 🔒 TK-APPROVED (28.07.2026): অ্যাডভান্সের খবর রোগীকে।
                            PatientMessage.show(
                                this@FollowUpActivity, patientNow.branch, patientNow.name,
                                patientNow.mobile, patientNow.patientId,
                                PatientMessage.Kind.ADVANCE,
                                amount = amount, mode = mode,
                                bill = enteredBill, paid = patientNow.paid + amount,
                                dateText = FollowUpModel.displayDate(FollowUpModel.today()),
                                // 🔒 B600 (TK-অনুমোদিত): A4 রসিদে রোগ/ঠিকানাও থাকবে।
                                disease = patientNow.disease, address = patientNow.address
                            )
                        }
                    }
                        },
                        onCancel = { advSaving = false }
                    )   // 🔒 খাতার সারি B52: PaymentDayGuard-এর ব্লক শেষ
                }
            }
            dialog.show()
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774

            // রোগীর আসল সারি পিছনে আনা হয়; এলে নাম/ব্রাঞ্চ/বিল ঠিক হয়ে যায় ও
            // Save কাজ করতে শুরু করে। স্টাফ ততক্ষণ টাকার ঘরে টাইপ করতে পারেন।
            BackgroundWork.run {
                val loaded = try {
                    pr.findOrMakePatient(item.name, digits, item.branch, item.patientId)
                } catch (_: Throwable) { null }
                if (loaded == null && !isFinishing && !isDestroyed) {
                    // 🚨 TK-এর নিয়ম (28.07.2026, খাতার সারি B30): নেট যাচাই না হলে
                    // এখন আর নতুন রোগী তৈরি হয় না — স্টাফকে স্পষ্ট জানাতে হবে,
                    // নইলে তিনি বুঝতেই পারতেন না কেন Save কাজ করছে না।
                    // ⛔ টাকার নিয়ম অপরিবর্তিত: হিসাব না এলে Save আগেও কাজ করত না।
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        Toast.makeText(
                            this@FollowUpActivity, NoBengali.s("Could not check the network — cannot tell whether this number is already registered. To avoid a duplicate patient, please try again in a little while."),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                if (loaded != null && !isFinishing && !isDestroyed) {
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        patient = loaded
                        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvAdvWho).text =
                            "${loaded.name} · ${digits} · ${loaded.branch}"
                        if (!billTouched && loaded.bill > 0) etTotal.setText("%.0f".format(loaded.bill))
                        if (loaded.billLocked && loaded.paid > 0) {
                            etTotal.isFocusable = false; etTotal.isFocusableInTouchMode = false
                            lock.visibility = View.VISIBLE
                            var bt2 = 0; var bl2 = 0L
                            rowTotal.setOnClickListener {
                                val now = System.currentTimeMillis()
                                if (now - bl2 > 1200) bt2 = 0
                                bl2 = now; bt2++
                                if (bt2 >= 3) { bt2 = 0; etTotal.isFocusable = true; etTotal.isFocusableInTouchMode = true; etTotal.requestFocus(); lock.visibility = View.GONE }
                            }
                        }
                    }
                }
            }
        }
    }

    /** TK APPROVED (2026-07-15): Patient card's payment ring opens this small
     *  premium popup directly — Total Bill / Already Paid / Due summary, plus
     *  the next installment ("This Payment"). No separate Payment Collection
     *  screen. Installment numbering (1st/2nd/3rd…) reuses the same counter
     *  the rest of the app already uses (PaymentRepository.nextLabelFor), so
     *  counting stays consistent everywhere. */
    private fun showNthPaymentDialog(item: FollowUpItem) {
        // 🔒 B619 (11.08.2026, TK-নির্দেশ "ক"): অন্য ব্রাঞ্চের রোগী হলে টাকার ঘর খোলার
        // আগেই আটকাই — যে ব্রাঞ্চের স্টাফ শুধু সেই ব্রাঞ্চের রোগীর টাকা নিতে পারবে।
        if (!MoneyBranchGuard.canTakeMoney(this@FollowUpActivity, item.branch, item.patientId)) {
            Toast.makeText(this@FollowUpActivity, MoneyBranchGuard.blockMessage(item.branch), Toast.LENGTH_LONG).show()
            return
        }
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)
        run {
            val pr = PaymentRepository(this@FollowUpActivity)
            // 🔒 TK-এর নিয়ম (28.07.2026): পপ-আপ সঙ্গে সঙ্গে খুলবে — কার্ডে যে
            // নাম · Patient ID · বিল · জমা আগে থেকেই আছে তা দিয়ে। আসল সারি
            // পিছনে এলে সব সংখ্যা ও "কততম পেমেন্ট" লেখাটা নিজে থেকে ঠিক হয়।
            // 💰 আসল সারি না আসা পর্যন্ত Save কাজ করে না — ভুল অঙ্কে কখনো সেভ হবে না।
            var patient: PatientBillInfo? = null
            var label = ""
            var billTouched = false
            val due = kotlin.math.max(0.0, item.bill - item.paid)

            val view = layoutInflater.inflate(com.tkbiswas.pilesclinic.R.layout.dialog_nth_payment, null)
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthTitle).apply {
                isClickable = true; isFocusable = true
                setOnClickListener {
                    val p0 = patient
                    if (p0 == null) Toast.makeText(this@FollowUpActivity, "Please wait — loading this patient's payments", Toast.LENGTH_SHORT).show()
                    else showPaymentHistoryDialog(p0)
                }
            }
            // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে digits দুইবার দেখাত (এখানে ও tvNthIdMobile-এ)।
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthName).text = item.name.ifBlank { "UNKNOWN" }
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthIdMobile).text =
                "🆔 ${item.patientId.ifBlank { "—" }}  ·  📞 $digits"
            val addressView = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthAddress)
            if (item.address.isNotBlank()) {
                addressView.text = "📍 " + item.address.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ) — শুধু দেখানোর সময় বড় হাতে; ডেটাবেসে কিছু বদলায় না।
                addressView.visibility = View.VISIBLE
            } else addressView.visibility = View.GONE

            val ivPhoto = view.findViewById<android.widget.ImageView>(com.tkbiswas.pilesclinic.R.id.ivNthPhoto)
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try { PhotoUtils.decodeDataUrl(PatientPhotoRepository().findByMobile(digits)?.photo) } catch (_: Exception) { null }
                }
                if (bitmap != null) ivPhoto.setImageBitmap(bitmap)
            }

            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthPaid).text =
                "₹${"%,.0f".format(item.paid)}"
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthDue).text =
                "₹${"%,.0f".format(due)}"
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthThisLabel).text = "This Payment"

            val etTotal = view.findViewById<EditText>(com.tkbiswas.pilesclinic.R.id.etNthTotal)
            val etAmount = view.findViewById<EditText>(com.tkbiswas.pilesclinic.R.id.etNthAmount)
            val lock = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthLock)
            val chipCash = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.chipNthCash)
            val chipOnline = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.chipNthOnline)
            val rowTotal = view.findViewById<android.widget.LinearLayout>(com.tkbiswas.pilesclinic.R.id.rowNthTotal)
            val rowAmount = view.findViewById<android.widget.LinearLayout>(com.tkbiswas.pilesclinic.R.id.rowNthAmount)
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

            // TK APPROVED (2026-07-15): tap ANYWHERE in a box to focus it and
            // open the keyboard — not just precisely on the number/hint.
            rowAmount.setOnClickListener {
                etAmount.requestFocus()
                imm.showSoftInput(etAmount, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }

            if (item.bill > 0) etTotal.setText("%.0f".format(item.bill))
            etTotal.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(e: android.text.Editable?) { billTouched = true }
                override fun beforeTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
                override fun onTextChanged(c: CharSequence?, a: Int, b: Int, d: Int) {}
            })
            // TK APPROVED (2026-07-15): every summary box here supports the
            // same silent 3-tap-to-edit already used elsewhere in the app (no
            // visible "tap 3 times" hint anywhere) — Total Bill unlocks this way.
            // তালা আসল সারি এলে বসে (নিচের BackgroundWork অংশে)।
            val nthLockedInitially = false
            if (nthLockedInitially) {
                etTotal.isFocusable = false; etTotal.isFocusableInTouchMode = false
                var bt = 0; var bl = 0L
                rowTotal.setOnClickListener {
                    val now = System.currentTimeMillis()
                    if (now - bl > 1200) bt = 0
                    bl = now; bt++
                    if (bt >= 3) { bt = 0; etTotal.isFocusable = true; etTotal.isFocusableInTouchMode = true; etTotal.requestFocus(); lock.visibility = View.GONE }
                }
            } else {
                lock.visibility = View.GONE
                rowTotal.setOnClickListener {
                    etTotal.requestFocus()
                    imm.showSoftInput(etTotal, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }

            var mode = "CASH"
            fun paintChips() {
                val onBg = com.tkbiswas.pilesclinic.R.drawable.bg_chip_seg_on; val offBg = com.tkbiswas.pilesclinic.R.drawable.bg_chip_seg
                chipCash.setBackgroundResource(if (mode == "CASH") onBg else offBg)
                chipCash.setTextColor(android.graphics.Color.parseColor(if (mode == "CASH") "#FFFFFF" else "#5B6B81"))
                chipOnline.setBackgroundResource(if (mode == "ONLINE") onBg else offBg)
                chipOnline.setTextColor(android.graphics.Color.parseColor(if (mode == "ONLINE") "#FFFFFF" else "#5B6B81"))
            }
            chipCash.setOnClickListener { mode = "CASH"; paintChips() }
            chipOnline.setOnClickListener { mode = "ONLINE"; paintChips() }
            paintChips()

            UppercaseInputUtil.applyToAll(view)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
            val dialog = AlertDialog.Builder(this@FollowUpActivity).setView(view).setCancelable(true).create()
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnNthClose).setOnClickListener { dialog.dismiss() }
            // TK-REPORTED CRITICAL BUG FIX (2026-07-26): same duplicate-payment
            // guard as the Advance dialog above -- this is the button that
            // produced the "2nd Payment" and "3rd Payment" duplicate rows in
            // TK's proof. One tap only, until that save finishes.
            var nthSaving = false
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnNthSave).setOnClickListener {
                if (nthSaving) return@setOnClickListener
                // 💰 আসল সারি না এলে সেভ নয়।
                val patientNow = patient
                if (patientNow == null) {
                    Toast.makeText(this@FollowUpActivity, "Please wait — loading this patient's bill", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val enteredBill = etTotal.text.toString().filter { it.isDigit() }.toDoubleOrNull() ?: patientNow.bill
                val amount = etAmount.text.toString().filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                // 🆕 TK-REPORTED (04.08.2026, ছবিসহ — RABINDRA CHANDRA NAHA,
                // JPE-04082026-001): এডভান্স আগেই নেওয়া (বিল তখনো ঠিক হয়নি),
                // পরে ডাক্তার বিল ঠিক করার পরে সেই বিলটা বসাতে গিয়ে Save
                // কাজ করছিল না — "This Payment" ফাঁকা থাকলেই Save পুরো
                // আটকে যেত, যদিও স্টাফ শুধু বিলটাই বসাতে চাইছিলেন, নতুন
                // কোনো টাকা এই মুহূর্তে নিচ্ছিলেন না।
                // TK নিজে নিশ্চিত করেছেন: "শুধু বিল বসিয়ে (০ টাকা পেমেন্ট)
                // Save করা যাবে" — এটাই ঠিক সমাধান।
                // ⛔ **নতুন কোনো ফাংশন বানানো হয়নি** — এই ফাইলেরই
                // `showAdvancePaymentDialog()`-এ (24.07.2026, TK-অনুমোদিত)
                // ঠিক এই একই কাজের জন্য আগে থেকেই থাকা `updateBillOnly()`
                // পুনর্ব্যবহার করা হলো (একই ফাংশন, একই আচরণ, দুই জায়গাতেই
                // এখন সমান — বিল ঠিক করে, Follow-up রিমার্কে অডিট-নোট লেখে,
                // কোনো ভুয়ো "₹0 Payment" তৈরি করে না, পেমেন্ট-লেবেল/
                // অর্ডিনাল/রোগীর মেসেজ কিছুই ছোঁয় না)।
                // ⛔ amount>0 হলে (সত্যিকারের পেমেন্ট) নিচের আগের নিয়মই
                // অক্ষত — এই বদল শুধু amount<=0 আর বিল সত্যিই বদলেছে,
                // এই একটা নতুন কেসেই প্রযোজ্য।
                val billOnlyUpdate = amount <= 0 && enteredBill > 0.0 && enteredBill != patientNow.bill
                if (amount <= 0 && !billOnlyUpdate) { Toast.makeText(this@FollowUpActivity, "Enter payment amount", Toast.LENGTH_SHORT).show() }
                else if (billOnlyUpdate) {
                    nthSaving = true
                    lifecycleScope.launch {
                        val ok = try {
                            withContext(Dispatchers.IO) { pr.updateBillOnly(patientNow, enteredBill, patientNow.bill, user.mobile, user.name.ifBlank { user.mobile }) }
                        } finally { nthSaving = false }
                        Toast.makeText(this@FollowUpActivity, if (ok) "Bill saved" else "Save failed — check connection", Toast.LENGTH_SHORT).show()
                        if (ok) { dialog.dismiss(); loadTab(currentStage) }
                    }
                }
                else if (enteredBill <= 0.0 && patientNow.bill <= 0.0) {
                    // TK-DECISION (2026-07-26, final): the Advance may be taken
                    // WITHOUT a bill, but by the SECOND payment the bill must
                    // exist . otherwise the patient keeps showing 0% / Bill ₹0
                    // / Due ₹0 and their Report Card stays hidden (TK's Raj
                    // Routh case). Patients who already have a bill are
                    // unaffected; only the bill-less ones are stopped here.
                    Toast.makeText(
                        this@FollowUpActivity,
                        "This patient's Bill has not been created yet — please create the Bill first",
                        Toast.LENGTH_LONG
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    etTotal.requestFocus()
                }
                else if (!MoneyBranchGuard.canTakeMoney(this@FollowUpActivity, patientNow.branch, patientNow.patientId)) {
                    // 🔒 TK'S LOCKED RULE (27.07.2026): same rule as the Advance
                    // above -- money belongs to the patient's own branch.
                    Toast.makeText(this@FollowUpActivity, MoneyBranchGuard.blockMessage(patientNow.branch), Toast.LENGTH_LONG).show()
                }
                else {
                    // 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত): আজ এই রোগীর নামে
                    // ইতিমধ্যে টাকা নেওয়া হয়ে থাকলে আগে একবার জিজ্ঞাসা।
                    /* 🔴 V1106 (TK-নির্দেশ) — সেভ চাপার মুহূর্তে ক্লাউডেও
                       একবার দেখা হয়, তাই অন্য ফোনে নেওয়া একই অঙ্কের টাকাও
                       ধরা পড়ে। ⛔ কিছুই আটকানো হয় না — শুধু প্রশ্ন। */
                    /* 🔴🔒 V1504 (১৫.০৯.২০২৬, TK-রিপোর্ট — RIMPA ROY-র ₹2,000
                       দুবার) — প্রশ্ন ওঠার আগেই তালা, "No, cancel"-এ খোলে। */
                    nthSaving = true
                    PaymentDayGuard.confirmBeforeSave(
                        this@FollowUpActivity,
                        pr,
                        patientNow,
                        amount,
                        pr.paidOnDateFor(patientNow.id),
                        pr.nextLabelFor(patientNow.id),
                        mode = mode,   // 🔴 V1152 — একই অঙ্ক ও একই ধরন হলে তবেই সতর্কবার্তা
                        onProceed = {
                    lifecycleScope.launch {
                        val ok = try {
                            withContext(Dispatchers.IO) {
                                pr.saveTreatmentPayment(patientNow, enteredBill, amount, mode, label.ifBlank { pr.nextLabelFor(patientNow.id) }, user.mobile)
                            }
                        } finally { nthSaving = false }
                        Toast.makeText(this@FollowUpActivity,
                            if (ok) "${label.ifBlank { "Payment" }} saved" else "Save failed — check connection",
                            Toast.LENGTH_SHORT).show()
                        if (ok) {
                            dialog.dismiss(); loadTab(currentStage)
                            // 🔒 TK-APPROVED (28.07.2026): পেমেন্টের খবর রোগীকে।
                            PatientMessage.show(
                                this@FollowUpActivity, patientNow.branch, patientNow.name,
                                patientNow.mobile, patientNow.patientId,
                                PatientMessage.Kind.PAYMENT,
                                amount = amount, mode = mode,
                                bill = enteredBill, paid = patientNow.paid + amount,
                                dateText = FollowUpModel.displayDate(FollowUpModel.today()),
                                // 🔒 খাতার সারি B92: বার্তায় **কততম পেমেন্ট** তা
                                // লেখা থাকবে (২য় · ৩য় · ৪র্থ …)। এটা ঠিক সেই
                                // লেখাটাই যা টাকার সারিতে সেভ হচ্ছে — নতুন করে
                                // কিছু গোনা হয়নি।
                                payLabel = label.ifBlank { pr.nextLabelFor(patientNow.id) },
                                // 🔒 B600 (TK-অনুমোদিত): A4 রসিদে রোগ/ঠিকানাও থাকবে।
                                disease = patientNow.disease, address = patientNow.address
                            )
                        }
                    }
                        },
                        onCancel = { nthSaving = false }
                    )   // 🔒 খাতার সারি B52: PaymentDayGuard-এর ব্লক শেষ
                }
            }
            dialog.show()
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774

            // আসল সারি পিছনে আনা হয়; এলে নাম · Patient ID · বিল · জমা · বকেয়া ·
            // "কততম পেমেন্ট" সব ঠিক হয়ে যায় এবং Save কাজ করতে শুরু করে।
            BackgroundWork.run {
                val loaded = try {
                    /* 🔵🔒 V520 (২২.০৮.২০২৬): এক মোবাইলে দুজন আলাদা রোগী থাকলে
                       এই কার্ডটা **কার** — সেটা কার্ডের নিজের দুটো আইডি-ই বলে দেয়
                       (`refId` = রোগীর সারির আইডি, `patientId` = Official Patient ID)।
                       তাই টাকার হিসাব ঠিক এই রোগীরই আসে, অন্যজনের নয়।
                       ⛔ দুটোই ফাঁকা হলে (পুরোনো সারি) হুবহু আগের পথ। */
                    pr.findPatientByMobile(
                        digits, item.branch,
                        preferPatientCode = item.patientId, preferRowId = item.refId
                    )
                        ?: pr.findOrMakePatient(item.name, digits, item.branch, item.patientId)
                } catch (_: Throwable) { null }
                if (loaded == null && !isFinishing && !isDestroyed) {
                    // 🚨 TK-এর নিয়ম (28.07.2026, খাতার সারি B30): নেট যাচাই না হলে
                    // এখন আর নতুন রোগী তৈরি হয় না — স্টাফকে স্পষ্ট জানাতে হবে,
                    // নইলে তিনি বুঝতেই পারতেন না কেন Save কাজ করছে না।
                    // ⛔ টাকার নিয়ম অপরিবর্তিত: হিসাব না এলে Save আগেও কাজ করত না।
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        Toast.makeText(
                            this@FollowUpActivity, NoBengali.s("Could not check the network — cannot tell whether this number is already registered. To avoid a duplicate patient, please try again in a little while."),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                val lbl = if (loaded != null) try { pr.nextLabelFor(loaded.id) } catch (_: Throwable) { "" } else ""
                if (loaded != null && !isFinishing && !isDestroyed) {
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        patient = loaded
                        label = lbl
                        val realDue = kotlin.math.max(0.0, loaded.bill - loaded.paid)
                        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthName).text =
                            // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে digits দুইবার দেখাত।
                            loaded.name.ifBlank { item.name.ifBlank { "UNKNOWN" } }
                        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthIdMobile).text =
                            "🆔 ${loaded.patientId.ifBlank { "—" }}  ·  📞 $digits"
                        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthPaid).text =
                            "₹${"%,.0f".format(loaded.paid)}"
                        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthDue).text =
                            "₹${"%,.0f".format(realDue)}"
                        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvNthThisLabel).text =
                            if (lbl.isBlank()) "This Payment" else "This Payment ($lbl)"
                        if (!billTouched && loaded.bill > 0) etTotal.setText("%.0f".format(loaded.bill))
                        if (loaded.billLocked) {
                            etTotal.isFocusable = false; etTotal.isFocusableInTouchMode = false
                            lock.visibility = View.VISIBLE
                            var bt2 = 0; var bl2 = 0L
                            rowTotal.setOnClickListener {
                                val now = System.currentTimeMillis()
                                if (now - bl2 > 1200) bt2 = 0
                                bl2 = now; bt2++
                                if (bt2 >= 3) { bt2 = 0; etTotal.isFocusable = true; etTotal.isFocusableInTouchMode = true; etTotal.requestFocus(); lock.visibility = View.GONE }
                            }
                        }
                    }
                }
            }
        }
    }

    /** TK APPROVED (2026-07-15): "Payment History" badge in the payment popup
     *  header — shows every past payment for this one patient (date, which
     *  installment, amount, mode), newest first. Read-only list. */
    private fun showPaymentHistoryDialog(patient: PatientBillInfo) {
        lifecycleScope.launch {
            val rows = withContext(Dispatchers.IO) {
                // TK-REQUESTED (2026-07-27), ধাপ ২: this asked ONLY by the
                // patients row id, so a payment filed under the human Patient
                // ID code (the web app's Chamber screen does that) never showed
                // in this history. Same single request, now matching either
                // identity. Mobile is deliberately NOT used here -- a family
                // sharing one number must not see each other's payments.
                PatientIdentity.paymentsFor(patient.id, patient.patientId, 500)
            }
            val entries = mutableListOf<org.json.JSONObject>()
            for (i in 0 until rows.length()) entries.add(rows.getJSONObject(i))
            entries.sortByDescending { it.optString("date", "") }
            // 🔒 খাতার সারি B52 (TK, 28.07.2026 রাত): নম্বরটা দিন ধরে নতুন করে
            // হিসাব করা হয়, তাই **পুরনো রেকর্ডেও** একই দিনের সব টাকা একটাই
            // নম্বরে দেখায়। ⛔ ক্লাউডে বাড়তি অনুরোধ নেই — উপরের ওই তালিকাটাই
            // ব্যবহার হয়। ⛔ ডেটাবেসে কিছু লেখা হয় না।
            val dayLabels = PaymentModel.dayBasedLabelById(entries)

            val d2 = resources.displayMetrics.density
            fun px(v: Int) = (v * d2).toInt()
            // ROOT-CAUSE FIX (2026-07-15): MATCH/WRAP were only defined inside
            // buildFollowCard() — a completely different function — so using
            // them here failed to compile ("Unresolved reference"). Defined
            // locally in this function too, before any use.
            val MATCH = android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            val WRAP = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            val root = android.widget.LinearLayout(this@FollowUpActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = px(20).toFloat(); setColor(android.graphics.Color.WHITE)
                }
            }
            val header = android.widget.LinearLayout(this@FollowUpActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(px(18), px(16), px(18), px(16))
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_header_navy_top_round)
            }
            header.addView(android.widget.TextView(this@FollowUpActivity).apply {
                text = "📜 Payment History"; textSize = 17f
                setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(android.graphics.Color.WHITE)
            })
            header.addView(android.widget.TextView(this@FollowUpActivity).apply {
                text = patient.name.ifBlank { patient.mobile }; textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#B8C6D8"))
                val p = android.widget.LinearLayout.LayoutParams(WRAP, WRAP); p.topMargin = px(2); layoutParams = p
            })
            root.addView(header)

            val rowsContainer = android.widget.LinearLayout(this@FollowUpActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(px(18), px(14), px(18), px(6))
            }
            var dlgRef: AlertDialog? = null
            if (entries.isEmpty()) {
                rowsContainer.addView(android.widget.TextView(this@FollowUpActivity).apply {
                    text = "No payments recorded yet."
                    setTextColor(android.graphics.Color.parseColor("#8A93A6")); textSize = 14f
                    setPadding(0, px(10), 0, px(10))
                })
            } else {
                entries.forEach { r ->
                    val date = r.s("date")
                    val label = dayLabels[r.s("id")]
                        ?: r.s("paymentLabel").ifBlank { r.s("payLabel").ifBlank { r.s("remarks").ifBlank { "Payment" } } }
                    val amount = r.optDouble("amount", 0.0)
                    val mode = r.s("mode").ifBlank { "CASH" }
                    val row = android.widget.LinearLayout(this@FollowUpActivity).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
                        setPadding(px(14), px(10), px(14), px(10))
                        val p = android.widget.LinearLayout.LayoutParams(MATCH, WRAP); p.bottomMargin = px(8); layoutParams = p
                        isClickable = true; isFocusable = true
                    }
                    val left = android.widget.LinearLayout(this@FollowUpActivity).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams = android.widget.LinearLayout.LayoutParams(0, WRAP, 1f)
                    }
                    left.addView(android.widget.TextView(this@FollowUpActivity).apply {
                        text = label; textSize = 14f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#10223A"))
                    })
                    left.addView(android.widget.TextView(this@FollowUpActivity).apply {
                        text = "${FollowUpModel.displayDate(date)} · $mode"; textSize = 11.5f
                        setTextColor(android.graphics.Color.parseColor("#8A93A6"))
                    })
                    // TK-REQUESTED (2026-07-21): every payment already records
                    // WHO took it (receivedBy/createdBy) -- was never shown here.
                    val staff = r.s("editedBy").ifBlank { r.s("receivedBy").ifBlank { r.s("createdBy") } }
                    if (staff.isNotBlank()) {
                        left.addView(android.widget.TextView(this@FollowUpActivity).apply {
                            // TK-REQUESTED (2026-07-22): staff CODE, not raw mobile.
                            /* 🔴 V1171 — এখানেও আগে সম্পূর্ণ নাম (নিয়ম ৭); না
                               পেলে আগের নিয়মেই কোড/নম্বর। */
                            val byFull = try {
                                CloudStaffDirectory.cachedNameFor(this@FollowUpActivity,
                                    StaffDirectory.normalizeMobile(staff))?.takeIf { it.isNotBlank() }
                            } catch (_: Throwable) { null }
                            text = "By: ${byFull ?: (StaffDirectory.findAccount(staff)?.name ?: staff)}"; textSize = 10.5f
                            setTextColor(android.graphics.Color.parseColor("#A8B2C2"))
                        })
                    }
                    row.addView(left)
                    // 🔴🔒 V1605 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ) — এখানেও প্রতিটা
                    // পেমেন্টের রং একই সবুজ ছিল, Refund হলেও। PaymentActivity.kt-
                    // এর পপ-আপের হুবহু একই ফিক্স (লাল, সামনে "−")।
                    val isRefundRow = r.s("payType").equals("refund", true)
                    row.addView(android.widget.TextView(this@FollowUpActivity).apply {
                        text = (if (isRefundRow) "−₹" else "₹") + "%,.0f".format(amount); textSize = 16f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor(if (isRefundRow) "#B3261E" else "#16A36D"))
                    })
                    // TK-REPORTED BUG FIX (2026-07-15): this list used to be
                    // read-only — a wrongly-entered amount (e.g. staff typed
                    // ₹10,000 by mistake) had nowhere to be corrected, since the
                    // Nth-Payment dialog's own 3-tap only unlocks the TOTAL BILL
                    // field, not any individual past payment. Every row here now
                    // supports the same 3-tap-to-edit as Payment Collection's own
                    // list, on THIS exact entry.
                    TripleTapEdit.attach(row) {
                        tryEditFollowUpPayment(r, patient) {
                            dlgRef?.dismiss()
                            showPaymentHistoryDialog(patient)
                            loadTab(currentStage)
                        }
                    }
                    rowsContainer.addView(row)
                }
            }
            val scroll = android.widget.ScrollView(this@FollowUpActivity).apply {
                addView(rowsContainer)
                layoutParams = android.widget.LinearLayout.LayoutParams(MATCH, px(420))
            }
            root.addView(scroll)

            val close = android.widget.TextView(this@FollowUpActivity).apply {
                text = "CLOSE"; gravity = android.view.Gravity.CENTER; textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                setPadding(0, px(14), 0, px(16))
                isClickable = true; isFocusable = true
            }
            root.addView(close)

            UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
            val dlg = AlertDialog.Builder(this@FollowUpActivity).setView(root).setCancelable(true).create()
            dlgRef = dlg
            dlg.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            close.setOnClickListener { dlg.dismiss() }
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
        }
    }

    /** TK-REPORTED BUG FIX (2026-07-15): lets a wrongly-entered payment amount
     *  (e.g. ₹10,000 typed by mistake) be corrected directly from Payment
     *  History — same permission rule as Payment Collection's own editor
     *  (canEditPaymentEntry): Master may edit any entry; Staff only a
     *  same-day, same-branch entry of their own. `onSuccess` lets the caller
     *  refresh whatever list/dialog is showing this payment. */
    private fun tryEditFollowUpPayment(row: org.json.JSONObject, patient: PatientBillInfo, onSuccess: () -> Unit) {
        // 🔴🔒 V1301 (তালিকা ৪১৩): চিহ্ন-সারি এডিট নয় — Timeline/Payment পর্দার একই নিয়ম।
        if (PaymentModel.isMarkerOnlyRow(row.s("payType"))) {
            Toast.makeText(this, "This is a system marker row (Bill edit / Expected / Arrived) — it holds no money and can't be edited", Toast.LENGTH_LONG).show()
            return
        }
        val eventCount = row.optJSONArray("dailyEvents")?.length()?.coerceAtLeast(1) ?: 1
        if (row.s("payType").equals("treatment", true) && eventCount > 1) {
            Toast.makeText(this, "This day's payment combines $eventCount entries. Cash/Online split will not be guessed.", Toast.LENGTH_LONG).show()
            return
        }
        val isMaster = user.role == "master"
        val rowBranch = row.s("branch").ifBlank { patient.branch }
        val sameBranch = rowBranch.isNotBlank() && rowBranch == user.branch
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val canEdit = isMaster || (sameBranch && row.s("date") == today)
        if (!canEdit) {
            Toast.makeText(this, "Only Master or same-day entries can be edited", Toast.LENGTH_SHORT).show()
            return
        }
        val id = row.s("id")
        if (id.isBlank()) {
            Toast.makeText(this, "This entry is not editable", Toast.LENGTH_SHORT).show()
            return
        }
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(0))
        }
        val amtInput = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT; keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
            setText(row.optDouble("amount", 0.0).toInt().toString())
            hint = "Amount"
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(12); setPadding(p, p, p, p)
        }
        val modeInput = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@FollowUpActivity, android.R.layout.simple_spinner_dropdown_item, listOf("CASH", "ONLINE"))
            setSelection(if (row.s("mode").ifBlank { "CASH" }.equals("CASH", true)) 0 else 1)
        }
        container.addView(android.widget.TextView(this).apply { text = "Amount"; setPadding(0, 0, 0, dp(4)) })
        container.addView(amtInput)
        container.addView(android.widget.TextView(this).apply { text = "Mode"; setPadding(0, dp(12), 0, dp(4)) })
        container.addView(modeInput)

        UppercaseInputUtil.applyToAll(container)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "💰 Edit Payment"))
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton("Save") { _, _ ->
                val amt = amtInput.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (amt <= 0) {
                    Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newMode = modeInput.selectedItem.toString()
                val oldAmt = row.optDouble("amount", 0.0)
                val oldMode = row.s("mode").ifBlank { "CASH" }
                val by = user.name.ifBlank { user.mobile }
                val whenStr = DateUtil.displayWithTime(java.util.Date())
                val changes = mutableListOf<String>()
                // 🔴 TK-REPORTED (30.07.2026 রাত, Kishanganj স্টাফের হিন্দি
                // বার্তা): "থেকে"/"করেছেন" বাংলা শব্দ এখন Mode-লাইনের মতোই
                // ইংরেজি "→"/"by"। ⛔ টাকার অঙ্ক/হিসাব/লজিক কিছুই বদলায়নি।
                if (amt != oldAmt) changes.add("Amount ₹${"%,.0f".format(oldAmt)} → ₹${"%,.0f".format(amt)}")
                if (!newMode.equals(oldMode, true)) changes.add("Mode $oldMode → $newMode")
                val existingRemark = row.s("remarks")
                val remarks = if (changes.isNotEmpty()) {
                    val audit = "Audit: ${changes.joinToString(", ")} by $by | Date: $whenStr"
                    if (existingRemark.isNotBlank() && !existingRemark.startsWith("Audit:")) "$existingRemark | $audit" else audit
                } else existingRemark
                val editStamp = SupabaseClient.localWriteStamp()
                val fields = org.json.JSONObject()
                    .put("amount", amt)
                    .put("mode", newMode)
                    .put("remarks", remarks)
                    .put("editedBy", user.mobile)
                    .put("editedAt", editStamp)
                    .put("updatedAt", editStamp)
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { SupabaseClient.updateById("payments", id, fields) }
                    Toast.makeText(
                        this@FollowUpActivity,
                        if (ok) "Payment updated" else "Failed — check connection",
                        Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) onSuccess()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun callNumber(mobile: String) {
        try {
            // TK-REQUESTED (2026-07-24): "everywhere calling is possible in
            // the project" -- shared CallChooser.kt (Phone/Superfone/etc.
            // picker, Truecaller excluded).
            CallChooser.open(this, mobile)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No phone app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWhatsApp(mobile: String) {
        // 🔒 V235 (TK, WhatsApp Chooser project-wide): কেন্দ্রীয় chooser দিয়ে
        // Personal/Business বাছাই (একটি থাকলেও silent default নয়)।
        WhatsAppMessageChooser.send(this, mobile)
    }

    /** Global 3-tap edit: correct a saved record's Name / Mobile / Branch /
     *  Disease. Updates the follow-up row and best-effort keeps the matching
     *  patient / enquiry rows consistent. */
    // TK-REQUESTED ADDITION (2026-07-24): dd/MM/yyyy display for the new
    // Registration/Visit Date field in showEditDialog below. Falls back to
    // the raw stored value if it isn't a plain yyyy-MM-dd date.
    // TK-REPORTED BUG FIX (2026-07-24): shared fix for the ROOT CAUSE found
    // in Edit Record (section 113 of the master note) -- item.id can be an
    // "enquiries" or "patients" row's id, not a real "followups" id, for
    // any item shown via FollowUpRepository's fallback synthesis (used
    // whenever the real followups row wasn't found by the main fetch).
    // Supabase's PATCH-by-id returns HTTP success even when zero rows
    // match, so every action below that used to call
    // repository.xxx(item.id, ...) directly could silently do nothing
    // while still reporting success. This looks up the REAL followups row
    // by mobile+stage first (same safe, already-proven pattern used for
    // patients/enquiries in saveRecordEdit) and returns its id -- falls
    // back to item.id itself if no real row is found (matches the exact
    // previous behaviour in that case, so nothing regresses). Must be
    // called from a background thread (network call) -- every call site
    // below already runs this inside withContext(Dispatchers.IO).
    /** 🔒 খাতার সারি B78 (TK, 29.07.2026 — স্টাফ লক্ষ্মীর Reject কাজ করছিল না):
     *  নিয়মটা এখন **এক জায়গায়** — `FollowUpRepository.ensureFollowUpRowId()`।
     *  ওটা আগের মতোই মোবাইল ধরে আসল সারিটা খোঁজে, **আর সারিটা সত্যিই না
     *  থাকলে তৈরি করে দেয়** — নইলে বদলটা এমন আইডিতে যেত যার কোনো সারিই নেই,
     *  আর Supabase তাতেও "200 OK" বলে বলে অ্যাপ ভুল করে "হয়ে গেছে" দেখাত।
     *  ⛔ নেট খারাপ হলে নতুন সারি তৈরি হয় না — পুরনো আচরণেই ফিরে যায়। */
    private fun resolveFollowUpId(item: FollowUpItem): String =
        repository.ensureFollowUpRowId(item)

    private fun displayDateForEdit(iso: String): String = try {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(iso)
        if (parsed != null) java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(parsed) else iso
    } catch (e: Exception) { iso }

    private fun showEditDialog(item: FollowUpItem) {
        val branches = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
        /* 🩺🔒 V1000 (০৩.০৯.২০২৬) — Enquiry-তে এখন একাধিক রোগ বাছা যায়, তাই
           ঘরটায় "Piles, Fissure"-এর মতো জোড়া লেখাও থাকতে পারে। আগে এই
           তালিকায় না মিললে স্পিনার নিঃশব্দে প্রথমটা ("Piles") দেখাত এবং
           সেভ করলেই আসল লেখাটা মুছে যেত। এখন যা আছে সেটাই তালিকার শুরুতে
           বসে, তাই কিছু না ছুঁলে কিছুই বদলায় না। */
        val baseDiseases = listOf("Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog", "Other")
        val diseases = if (item.disease.isNotBlank() && !baseDiseases.contains(item.disease))
            listOf(item.disease) + baseDiseases else baseDiseases
        val pad = (16 * resources.displayMetrics.density).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad * 3, pad, pad * 3, 0)
        }
        fun label(t: String) = android.widget.TextView(this).apply { text = t; setPadding(0, pad, 0, 0) }
        val nameInput = EditText(this).apply { setText(item.name); hint = "Name" }
        val mobileInput = EditText(this).apply {
            setText(item.mobile); hint = "Mobile"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val branchSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@FollowUpActivity, android.R.layout.simple_spinner_dropdown_item, branches)
            val bi = branches.indexOf(item.branch); if (bi >= 0) setSelection(bi)
        }
        val diseaseSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@FollowUpActivity, android.R.layout.simple_spinner_dropdown_item, diseases)
            val di = diseases.indexOf(item.disease); if (di >= 0) setSelection(di)
        }
        val ageInput = EditText(this).apply {
            setText(item.age); hint = "Age"
            inputType = android.text.InputType.TYPE_CLASS_TEXT; keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
        }
        val sexOptions = listOf("Male", "Female", "Other")
        val sexSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@FollowUpActivity, android.R.layout.simple_spinner_dropdown_item, sexOptions)
            val si = sexOptions.indexOfFirst { it.equals(item.sex, true) }; if (si >= 0) setSelection(si)
        }
        val addressInput = EditText(this).apply { setText(item.address); hint = "Address" }

        // TK-REQUESTED ADDITION (2026-07-24, moved to TOP per TK's
        // follow-up instruction): let staff correct the Registration/Visit
        // date after the fact (e.g. patient actually came yesterday but
        // was only entered into the app today). Only shown for an
        // already-registered patient (Visit/Treatment stage) -- a pure
        // Enquiry has no "registration date" concept yet, and its own
        // Enquiry date is a different thing this dialog doesn't touch.
        // Defaults to the existing date (item.recordDate); leaving it
        // untouched means nothing changes, exactly like every other field.
        var pickedRegDate = item.recordDate
        if (!item.stage.equals("Inquiry", ignoreCase = true)) {
            container.addView(label("⏰ Registration/Visit Date"))
            val dateValue = EditText(this).apply {
                setText(displayDateForEdit(item.recordDate))
                isFocusable = false
                isFocusableInTouchMode = false
                setOnClickListener {
                    val cal = java.util.Calendar.getInstance()
                    try {
                        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(item.recordDate)
                        if (parsed != null) cal.time = parsed
                    } catch (_: Exception) { }
                    android.app.DatePickerDialog(this@FollowUpActivity, { _, y, m, d ->
                        val cal2 = java.util.Calendar.getInstance().apply { set(y, m, d) }
                        val iso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal2.time)
                        pickedRegDate = iso
                        setText(displayDateForEdit(iso))
                    }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).apply {
                        datePicker.maxDate = System.currentTimeMillis()
                    }.show()
                }
            }
            container.addView(dateValue)
        }

        container.addView(label("Name")); container.addView(nameInput)
        container.addView(label("Mobile")); container.addView(mobileInput)
        container.addView(label("Branch")); container.addView(branchSpinner)
        container.addView(label("Disease")); container.addView(diseaseSpinner)
        container.addView(label("Age")); container.addView(ageInput)
        container.addView(label("Sex")); container.addView(sexSpinner)
        container.addView(label("Address")); container.addView(addressInput)

        // Navy premium styling (look only — no field/logic change).
        val dm = resources.displayMetrics.density
        val ip = (12 * dm).toInt(); val ipH = (14 * dm).toInt(); val gap = (6 * dm).toInt()
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            when (v) {
                is EditText -> {
                    v.setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
                    v.setPadding(ipH, ip, ipH, ip)
                    (v.layoutParams as? android.widget.LinearLayout.LayoutParams)?.topMargin = gap
                }
                is android.widget.TextView -> {
                    v.setTextColor(android.graphics.Color.parseColor("#0A5428"))
                    v.setTypeface(v.typeface, android.graphics.Typeface.BOLD)
                }
            }
        }
        val header = android.widget.TextView(this).apply {
            text = "Edit Record"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            // TK APPROVED (2026-07-15): header colour updated from solid navy
            // to the dual-green look used everywhere else this session --
            // fields/logic below completely unchanged.
            background = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(android.graphics.Color.parseColor("#0A5428"), android.graphics.Color.parseColor("#0EA25F"))
            }
            setPadding(pad * 2, pad, pad * 2, pad)
        }

        UppercaseInputUtil.applyToAll(container)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(header)
            .setView(
                // TK-REPORTED BUG FIX (2026-07-20): this dialog's content was a
                // plain LinearLayout with no ScrollView -- on a screen where
                // the form (Name/Mobile/Branch/Disease/Age/Sex/Address) is
                // taller than the dialog's max height, the bottom fields
                // (Address) became completely unreachable, with no way to
                // scroll down to them. Wrapping in a ScrollView fixes this;
                // no field, label, or Save/Cancel behavior changes.
                android.widget.ScrollView(this).apply { addView(container) }
            )
            .setPositiveButton("Save") { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newMobile = mobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
                val newBranch = branchSpinner.selectedItem.toString()
                val newDisease = diseaseSpinner.selectedItem.toString()
                // TK-REQUESTED (2026-07-24): Name is no longer mandatory
                // for an Enquiry-stage record (a patient may just call in
                // and give only their mobile number at first) -- still
                // required for an already-registered Visit/Treatment
                // patient, unchanged from before.
                if (newName.isBlank() && !item.stage.equals("Inquiry", ignoreCase = true)) {
                    android.widget.Toast.makeText(this, "Name required", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val newAge = ageInput.text.toString().trim()
                    val newSex = sexSpinner.selectedItem.toString()
                    val newAddress = addressInput.text.toString().trim()
                    // TK-REQUESTED ADDITION (2026-07-24): only passed when it
                    // actually changed from what was loaded -- leaving the
                    // date picker untouched means no date fields get
                    // touched at all, exactly like every other field here.
                    val newRegDate = if (pickedRegDate != item.recordDate) pickedRegDate else ""
                    val ok = withContext(Dispatchers.IO) { saveRecordEdit(item, newName, newMobile, newBranch, newDisease, newAge, newSex, newAddress, newRegDate) }
                    android.widget.Toast.makeText(
                        this@FollowUpActivity,
                        if (ok) "Record updated" else "Failed — check connection",
                        android.widget.Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) loadTab(currentStage)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            val green = android.graphics.Color.parseColor("#0A5428")
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(green)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(green)
        }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    private fun saveRecordEdit(
        item: FollowUpItem, name: String, mobile: String, branch: String, disease: String,
        age: String = "", sex: String = "", address: String = "",
        // TK-REQUESTED ADDITION (2026-07-24): registration date correction.
        // Blank ("") means "unchanged", matching age/sex/address's own
        // blank-means-keep-existing convention just above.
        registrationDate: String = ""
    ): Boolean {
        return try {
            val fields = org.json.JSONObject()
                .put("name", name).put("branch", branch).put("disease", disease)
            /* 🔴🔒 V1139 (০৬.০৯.২০২৬ — TK: *"ঠিকানাটাও ভুল শুনেছিলাম, এখান থেকে
               এডিট করতে পারছি না"* — মিলিয়ে দেখতে গিয়ে ধরা পড়া **আসল ফাঁক**):
               ঠিকানা এতদিন লেখা হত শুধু `patients`/`enquiries`-এ। নিচের পুরনো
               মন্তব্যে লেখা ছিল "followups-এ address নেই" — **সেটা ভুল**, ঘরটা
               আছে, আর "কাল আসার কথা" পর্দা ঠিকানা **ওখান থেকেই** পড়ে। ⇒ ঠিকানা
               বদলালেও ওই পর্দায় পুরনোটাই থেকে যেত।
               ⛔ ফাঁকা হলে ঘরটা ছোঁয়াই হয় না (age/sex/address-এর পুরনো নিয়মই)। */
            if (address.isNotBlank()) fields.put("address", address)
            if (mobile.length == 10) fields.put("mobile", mobile)
            // TK-REQUESTED ADDITION (2026-07-24): only followups' own
            // date/registrationDate/visitDate fields -- deliberately NOT
            // added to the shared `patientFields` below, because that same
            // object is also used to update "enquiries" (a few lines down),
            // and an Enquiry's own "date" means something completely
            // different (when the enquiry call happened) -- it must never
            // be overwritten by a Registration-date correction.
            if (registrationDate.isNotBlank()) {
                fields.put("date", registrationDate).put("registrationDate", registrationDate).put("visitDate", registrationDate)
            }
            // TK-REPORTED BUG FIX (2026-07-24): ROOT CAUSE of "shows Saved
            // but the branch never actually changes" -- item.id here can
            // be an "enquiries" row's id, not a real "followups" id, when
            // this item was being shown via the Enquiry/Visit/Treatment-tab
            // fallback synthesis (FollowUpRepository.kt, used whenever the
            // real followups row wasn't found in the main fetch -- see
            // section 111's self-heal fix for WHY that happens). Supabase's
            // PATCH-by-id returns HTTP success even when ZERO rows match
            // (isSuccessful only checks the status code, not rows-affected),
            // so updateById("followups", item.id, ...) silently did nothing
            // and STILL reported success. Now looks the real followups
            // row(s) up by mobile+stage (same safe pattern already used for
            // patients/enquiries just below) and updates those directly,
            // so this works correctly regardless of whether item.id was
            // ever a real followups id. item.id is still tried first (fast
            // path for the normal, non-fallback case); the mobile+stage
            // lookup below additionally covers/corrects every case.
            var fOk = SupabaseClient.updateById("followups", item.id, fields)
            try {
                val oldMobileForLookup = item.mobile.filter { it.isDigit() }.takeLast(10)
                /* 🔴🔒 V794 — এখানেও সারি থেকে শুধু `id` নেওয়া হয় (যাচাই করা), তাই
                   ছবি/ইতিহাস ছাড়া ছোট্ট তালিকা। */
                val realFollowUps = SupabaseClient.fetchListSlim("followups",
                    "mobile=like.*$oldMobileForLookup&stage=eq.${item.stage}", 20,
                    SupabaseClient.FOLLOWUP_ID_COLS)
                for (i in 0 until realFollowUps.length()) {
                    val realId = realFollowUps.getJSONObject(i).optString("id")
                    if (realId.isBlank() || realId == item.id) continue
                    if (SupabaseClient.updateById("followups", realId, fields)) fOk = true
                }
            } catch (_: Exception) { }
            // patients/enquiries carry age + address (followups has no such columns),
            // so build a richer field set for those two tables. Age/address are only
            // written when the user actually typed something (blank = keep existing).
            val patientFields = org.json.JSONObject()
                .put("name", name).put("branch", branch).put("disease", disease)
            if (mobile.length == 10) patientFields.put("mobile", mobile)
            if (age.isNotBlank()) patientFields.put("age", age)
            if (sex.isNotBlank()) patientFields.put("sex", sex)
            if (address.isNotBlank()) patientFields.put("address", address)
            val oldMobile = item.mobile.filter { it.isDigit() }.takeLast(10)
            // AUDIT FIX v2 (2026-07-26): set to true when the linked
            // patients/enquiries rows could NOT be identified by Patient ID.
            // In that case nothing is written to them and the person is told.
            var linkedUpdateSkipped = false
            for (table in listOf("patients", "enquiries")) {
                try {
                    // AUDIT FIX v2 (2026-07-26, TK's instruction): this loop
                    // writes NAME / BRANCH / DISEASE / AGE / SEX / ADDRESS, not
                    // just the mobile. Families here share one number, and two
                    // different patients can even have the SAME NAME . so name
                    // matching is NOT allowed and no row may ever be guessed.
                    // A row is updated ONLY when its "patientId" is exactly this
                    // patient's Patient ID. If this record has no Patient ID, or
                    // no row carries it, NOTHING is written here and the person
                    // is told, so the correction can be done by hand.
                    val myPid = item.patientId.trim().uppercase()
                    if (myPid.isBlank()) {
                        linkedUpdateSkipped = true
                    } else {
                        val rows = SupabaseClient.findByMobile(table, "+91$oldMobile", "id,patientId", 50)
                        var touched = 0
                        for (i in 0 until rows.length()) {
                            val row = rows.getJSONObject(i)
                            val id = row.optString("id")
                            if (id.isBlank()) continue
                            if (row.optString("patientId", "").trim().uppercase() != myPid) continue
                            touched++
                            // TK-REPORTED (2026-07-27): a failed write here used to be
                            // lost silently; it now retries like every other save.
                            if (!SupabaseClient.updateById(table, id, patientFields)) {
                                try { GenericUpdateQueue.queue(this@FollowUpActivity, table, id, patientFields) } catch (_: Throwable) { }
                            }
                            // TK-REQUESTED ADDITION (2026-07-24): registration
                            // date correction applied ONLY to "patients" here
                            // (never "enquiries" -- see comment above).
                            if (registrationDate.isNotBlank() && table == "patients") {
                                val dateFields = org.json.JSONObject()
                                    .put("date", registrationDate).put("registrationDate", registrationDate).put("visitDate", registrationDate)
                                if (!SupabaseClient.updateById(table, id, dateFields)) {
                                    try { GenericUpdateQueue.queue(this@FollowUpActivity, table, id, dateFields) } catch (_: Throwable) { }
                                }
                            }
                        }
                        if (touched == 0) linkedUpdateSkipped = true
                    }
                } catch (_: Exception) { }
            }
            // TK-REPORTED (2026-07-26): a corrected mobile was only carried
            // over to followups/patients/enquiries . the patient's Payment
            // rows kept the OLD number, so screens that look a
            // patient up by mobile stopped showing that money. Shared family
            // numbers are protected inside MobileChangeSync itself.
            if (mobile.length == 10) {
                try { MobileChangeSync.sync(item.mobile, mobile, item.patientId, this@FollowUpActivity) } catch (_: Exception) { }
            }
            // AUDIT FIX v2 (2026-07-26): tell the person when the linked
            // Patient/Enquiry rows could not be matched by Patient ID, so they
            // know to correct those by hand instead of assuming it was done.
            if (linkedUpdateSkipped) {
                runOnUiThread {
                    Toast.makeText(
                        this@FollowUpActivity,
                        "Saved. Linked Patient/Enquiry records were NOT changed (no matching Patient ID) - please check them.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            fOk
        } catch (e: Exception) {
            false
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 🔒 খাতার সারি B51 (TK, 28.07.2026 রাত) — কল করার পরে রিমার্ক বাকি থাকলে
    //    স্টাফ যখনই অ্যাপে ফিরবেন তখনই মনে করিয়ে দেওয়া।
    //
    // ⛔ পপ-আপের চেহারা · লেখা · বোতাম — আগের মতোই, কিছুই বদলানো হয়নি।
    // ⛔ কল-গোনার নিয়ম অক্ষত: দাগ আগের মতোই কেবল রিমার্ক সেভ করলে বাড়ে।
    // ⛔ ক্লাউডে কিছুই লেখা হয় না — তালিকাটা শুধু এই ফোনে।
    // ──────────────────────────────────────────────────────────────────────
    private fun remindPendingRemark() {
        try {
            // ঘন্টা/Dashboard থেকে নাম ধরে আসা হলে — সোজা রিমার্কের বাক্স।
            val focus = pendingFocusMobile.filter { it.isDigit() }.takeLast(10)
            if (focus.length == 10) {
                pendingFocusMobile = ""
                val p = PendingRemarkStore.list(this, user.mobile)
                    .firstOrNull { it.mobile.filter { c -> c.isDigit() }.takeLast(10) == focus }
                if (p != null) { openRemarkForPending(p); return }
            }
            // এই বসাতেই কল করা হয়েছিল — হাতে থাকা কার্ডটাই সবচেয়ে নির্ভরযোগ্য।
            val calledItem = pendingCallItem
            if (calledItem != null) {
                pendingCallItem = null
                askRemarkReminder(calledItem.name.ifBlank { calledItem.mobile }) { showRemarkDialog(calledItem) }
                return
            }
            // অ্যাপ বন্ধ হয়ে যাওয়ার পরেও বাকি থেকে গেছে — ফোনের জমানো তালিকা।
            val pending = PendingRemarkStore.list(this, user.mobile)
            if (pending.isEmpty()) return
            val first = pending[0]
            askRemarkReminder(first.name.ifBlank { first.mobile }) { openRemarkForPending(first) }
        } catch (_: Throwable) { }
    }

    /** আগের সেই একই পপ-আপ — শুধু নামটা যেখান থেকেই আসুক। */
    private fun askRemarkReminder(who: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Update remark?"))
            .setMessage("Add a remark for $who after that call?")
            .setPositiveButton("Add Remark") { _, _ -> onYes() }
            .setNegativeButton("Not now", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** পর্দায় থাকা আসল কার্ডটা পেলে সেটাই ব্যবহার হয় (৫-কলের নিয়ম ও পুরনো
     *  রিমার্ক তখন ঠিকঠাক আসে); না পেলে জমানো তথ্য দিয়ে চালানো হয় —
     *  `resolveFollowUpId()` মোবাইল ধরে আসল সারিটা নিজেই খুঁজে নেয়। */
    private fun openRemarkForPending(p: PendingRemark, retry: Boolean = true) {
        val digits = p.mobile.filter { it.isDigit() }.takeLast(10)
        val live = loadedItems.firstOrNull { it.mobile.filter { c -> c.isDigit() }.takeLast(10) == digits }
        if (live != null) { showRemarkDialog(live); return }
        // ঘন্টা থেকে সোজা এখানে এলে তালিকাটা তখনো আসেনি — এক সেকেন্ডেরও কম
        // অপেক্ষা করে আসল কার্ডটা খোঁজা হয়, তাতে পুরনো রিমার্ক ও ৫-কলের
        // নিয়ম দুটোই ঠিকঠাক আসে। না পেলে জমানো তথ্য দিয়েই চলে, কিছু আটকায় না।
        if (retry && loadedItems.isEmpty()) {
            binding.root.postDelayed({
                if (!isFinishing && !isDestroyed) openRemarkForPending(p, retry = false)
            }, 700L)
            return
        }
        showRemarkDialog(
            FollowUpItem(
                id = p.followUpId, name = p.name, mobile = p.mobile, branch = p.branch,
                disease = "", stage = p.stage, lastRemark = "", nextFollow = "",
                recordDate = "", bill = 0.0, paid = 0.0,
                // 🔒 খাতার সারি B57 (TK, 29.07.2026): আগে এখানে গোনাটা ০ ধরা হত,
                // তাই তালিকা না এলে ৫ কল হয়ে যাওয়া এন্ট্রিতেও সিদ্ধান্ত-পপ-আপটা
                // (Convert / Cancel) আসত না। এখন কল করার সময়ের আসল গোনাটাই
                // সঙ্গে থাকে, তাই ওই পথেও নিয়ম এক।
                callCount = p.callCount
            )
        )
    }

    private fun showRemarkDialog(item: FollowUpItem) {
        // For an Inquiry enquiry, logging a remark is a call log: enforce the
        // 5-call decision guard first (web followActionGuard), then increment.
        if (item.stage == "Inquiry" && item.callCount >= 5) {
            showFiveCallDecision(item)
            return
        }
        // 🔒 B603 (TK-নির্দেশ 09.08.2026): দ্বিতীয়বার রিমার্ক লিখতে গেলে ঘরটা খালি
        // থাকবে (আগের লেখা প্রি-ফিল হবে না) — যাতে প্রতিটা কথা আলাদা এন্ট্রি হয়,
        // মিশে না যায়। শুধু হালকা ধূসর হিন্ট। ⛔ সেভ-লজিক অপরিবর্তিত (ফাঁকা দিলে
        // আগের রিমার্ক মুছবে না — সেই পাহারা repository-তে আছে)।
        val input = EditText(this).apply { hint = NoBengali.s("এখানে কিছু লিখুন…") }
        UppercaseInputUtil.applyToAll(input)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        // 🔒 TK-এর নির্দেশ (item 11, 01.08.2026, ফটো-প্রুফসহ — "আমার দেখতে
        // একদম পছন্দ হচ্ছে না"): এই পপ-আপটা প্রজেক্টের আগে থেকে থাকা premium
        // চেহারায় নেওয়া হয়নি — শুধু এই একটাই নয়, একই বাগ আরও কয়েক জায়গায়
        // পাওয়া গেছে ও একসঙ্গে সারানো হয়েছে (নিচে দেখুন)। কাজ/সেভ-লজিক এক
        // অক্ষরও বদলায়নি — শুধু .setTitle() → PremiumAlert.header(), আর
        // .show() → .show().also { PremiumAlert.paint(it) }।
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "📝 Update Remark — ${item.name.ifBlank { item.mobile }}"))
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val remark = input.text.toString().trim()
                // 🔒 খাতার সারি B54 (TK, 28.07.2026 রাত): ঘরটা ফাঁকা করে Save
                // করলে আগে `lastRemark` ফাঁকা হয়ে যেত — স্টাফের লেখা কথাটা
                // চিরতরে চলে যেত। এখন ফাঁকা হলে কিছুই সেভ হয় না, আগের
                // রিমার্কটাই থাকে (repository-তেও একই পাহারা বসানো আছে)।
                if (remark.isBlank()) {
                    Toast.makeText(this@FollowUpActivity, "Nothing written — the earlier remark is kept", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // 🔒 V215 (§17, 31.07.2026): আগে শুধু Inquiry-তে remark save হলে
                // call গোনা হত ও Last Call Date বসত; Visit/Patient stage-এ স্টাফ
                // রোগীর সঙ্গে কথা বলে remark দিলেও Signal বাড়ত না, Last Call Date
                // পুরোনো থেকে যেত (Jalpaiguri-র রিপোর্ট: 23.07.2026 আটকে ছিল)।
                // এখন যে কোনো stage-এ post-call remark সফল হলে সেটা একটা Completed
                // Call — repository-তে দিনে-একবার de-dup আছে (একই দিনে দুবার লিখলে
                // গোনা বাড়ে না, শুধু তারিখ আজকের হয়), তাই double count হয় না।
                // ⛔ 5-বারের decision guard শুধু Inquiry-তেই (উপরে অপরিবর্তিত)।
                val countAsCall = true
                // 🔒 TK-এর নিয়ম (28.07.2026): Save চাপার সঙ্গে সঙ্গে ক্যালেন্ডার।
                // TK: "প্রায় ১৫ সেকেন্ড পরে ক্যালেন্ডার ওপেন হলো... স্টাফের কাজ
                // তো রিমার্ক লিখে পরের জনকে কল করা।" ক্যালেন্ডার আঁকতে ক্লাউডের
                // কোনো তথ্যই লাগে না, তাই সে আর অপেক্ষা করে না। রিমার্কটা ফোনে
                // সঙ্গে সঙ্গে বসে, ক্লাউডে যাওয়ার কাজটা পিছনে চলে — না গেলে
                // অপেক্ষমাণ তালিকায় জমা থেকে নিজে থেকেই আবার যায়, তাই কিছু
                // হারায় না।
                // 🔒 খাতার সারি B51: রিমার্ক লেখা হয়ে গেছে — বাকির তালিকা ও
                // ঘন্টার সংখ্যা থেকে নামটা সঙ্গে সঙ্গে উঠে যায়।
                try { PendingRemarkStore.remove(this@FollowUpActivity, item.mobile) } catch (_: Throwable) { }
                Toast.makeText(this@FollowUpActivity, "Remark updated", Toast.LENGTH_SHORT).show()
                /* 🔴🔒 V1360 (১১.০৯.২০২৬, পুরো প্রজেক্ট যাচাই — TK: "রিমার্ক লেখা সংক্রান্ত
                   দেরি"): আগে একটা রিমার্কে **পুরো তালিকা দু'বার** ক্লাউড থেকে নামত —
                   রিমার্ক বসার পরে একবার (এখানে `loadTab`), তারপর বাধ্যতামূলক তারিখ
                   দেওয়ার পরে আবার (`saveNextFollowDate`)। এখন কার্ডটা **সঙ্গে সঙ্গে
                   পর্দাতেই** নতুন লেখা পায় (নিয়ম ৭খ), প্রথম নামানোটা বাদ; তারিখ দেওয়ার
                   পরের নামানোটা আগের মতোই থাকে (গোনা · তারিখ ওখানেই আসে)।
                   ⛔ সেভের নিয়ম/জমা/পাঠানো — কিচ্ছু বদলায়নি; শুধু একটা বাড়তি নামানো কম। */
                try {
                    loadedItems = loadedItems.map {
                        if (it.id == item.id || (it.mobile == item.mobile && it.stage == item.stage)) it.copy(lastRemark = remark) else it
                    }
                    applySearch()
                } catch (_: Throwable) { }
                showMandatoryNextFollowPrompt(item)
                BackgroundWork.run {
                    repository.updateRemark(resolveFollowUpId(item), remark, user.name, countAsCall)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // TK-REQUESTED (2026-07-24, simplified per TK's follow-up instruction
    // "সাথে সাথে Automatic Calendar Popup"): after every Remark save, the
    // date-picker now opens DIRECTLY and immediately -- no intermediate
    // "tap a button first" step (the earlier version of this function
    // showed a premium card with a button before the calendar; that extra
    // tap is now removed). Still fully MANDATORY -- setCancelable(false),
    // and the DatePicker's own built-in Cancel button is hidden after it
    // shows -- so there is still no way to dismiss this without actually
    // picking a date, just faster to reach.
    private fun showMandatoryNextFollowPrompt(item: FollowUpItem) {
        // NEW (2026-08-07): mandatory post-remark date now goes through the same
        // shared chamber-day flow (Enquiry chooser + Visit/Patient mark আসার কথা).
        Toast.makeText(this, NoBengali.s("এখন পরের Follow-up Call তারিখ দিন"), Toast.LENGTH_SHORT).show()
        startNextFollowDate(item, mandatory = true)
        /* OLD mandatory body (pre 2026-08-07), kept for revert:
        val cal = java.util.Calendar.getInstance()
        // 🔴🔴🟢 pickNextFollow()-এর ঠিক একই ফিক্স এখানেও (একই বাগ-ক্লাস,
        // TK-এর নিয়ম ৬.২) — বিগত `item.nextFollow` থাকলে সেটা ডিফল্ট করা
        // হবে না, আজকের তারিখই থাকবে।
        val todayIso = FollowUpModel.today()
        try {
            if (item.nextFollow.isNotBlank() && item.nextFollow >= todayIso) {
                val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(item.nextFollow)
                if (parsed != null) cal.time = parsed
            }
        } catch (_: Exception) { }
        val picker = DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, day ->
            val iso = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, day)
            // 🔒 TK-এর নিয়ম (28.07.2026): তারিখ বেছে দেওয়ামাত্র স্টাফ পরের কাজে
            // যেতে পারবেন — ক্লাউডের উত্তরের জন্য পর্দা আটকে থাকবে না।
            Toast.makeText(this@FollowUpActivity, "Next follow-up set", Toast.LENGTH_SHORT).show()
            BackgroundWork.run {
                // 📵 V1206 — স্টাফ নতুন তারিখ বাছলেন ⇒ থামানো বাতিল, আবার চালু।
                // 🔴🔒 V1222 ③ — উপরের saveNextFollowDate-এর হুবহু একই সুরক্ষা
                //    TK-র নিয়ম ৭ মেনে: একটা দোষ পেলে একই ধরনের সব জায়গা।
                //    সঙ্গে সঙ্গের বার্তা অটুট; শুধু না-বসলে পরে সৎ খবরটা যায়।
            val ok = repository.updateNextFollowSaved(resolveFollowUpId(item), iso, stop = false).also { s2 ->
                if (!s2) runOnUiThread {
                    if (!isFinishing && !isDestroyed) Toast.makeText(
                        this@FollowUpActivity,
                        "Not sent yet — saved on this phone, will send when online",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } || true
                // TK-CORRECTED (2026-07-27): only the Patient card (stage
                // "Treatment") marks আসার কথা -- see pickNextFollow() above.
                if (ok && item.stage == "Treatment") {
                    try {
                        ChamberAttendanceRepository.markExpected(
                            this@FollowUpActivity, item.mobile, item.name, item.branch, iso, user.mobile
                        )
                    } catch (_: Throwable) { }
                    // 🔒 TK-APPROVED (28.07.2026): আসার তারিখের খবর রোগীকে।
                    // শুধু Patient কার্ড থেকে (stage = Treatment) — কারণ ২৭.০৭.২০২৬-এর
                    // লক করা নিয়ম অনুযায়ী তখনই এটা সত্যিকারের "আসার কথা" হয়।
                    // Enquiry ও Visit-এর তারিখ শুধু ফোন করার তারিখ, তাই সেখানে নয়।
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            PatientMessage.show(
                                this@FollowUpActivity, item.branch, item.name,
                                item.mobile, item.patientId,
                                PatientMessage.Kind.VISIT_DATE,
                                dateText = FollowUpModel.displayDate(iso)
                            )
                        }
                    }
                }
                if (ok && !isFinishing && !isDestroyed) {
                    runOnUiThread { if (!isFinishing && !isDestroyed) loadTab(currentStage) }
                }
            }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
        picker.setTitle(NoBengali.s("⏰ Next Follow-up Call — বাধ্যতামূলক"))
        picker.setCancelable(false)
        picker.setCanceledOnTouchOutside(false)
        picker.setOnShowListener {
            // 🔒 খাতার সারি B158: এই তারিখ-বাছাই পপ-আপের শিরোনামে বাংলা আছে
            // আর এটা PremiumAlert.paint দিয়ে যায় না — তাই এখানেই ঢাকা হলো।
            try { NoBengali.installDialog(picker) } catch (_: Throwable) { }
            // No escape without picking a date -- hide the built-in Cancel button.
            picker.getButton(AlertDialog.BUTTON_NEGATIVE)?.visibility = View.GONE
        }
        /* 🔴🔒 V843 (২৯.০৮.২০২৬, নিয়ম ৭) — বাধ্যতামূলক Next Follow-up-এও
           অতীতের তারিখ বাছা যেত। V671-এর প্রমাণিত একই লাইন।
           ⛔ জমা থাকা পুরনো তারিখ মোছে না, শুধু নতুন বাছা আটকায়। */
        try { picker.datePicker.minDate = System.currentTimeMillis() - 1000L } catch (_: Throwable) { }
        picker.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(picker) } catch (_: Throwable) { }   // 🤫 V774
        ---- end old mandatory body ---- */
    }

    private fun showFiveCallDecision(item: FollowUpItem) {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "5 calls completed"))
            .setMessage("${item.name.ifBlank { item.mobile }} — called 5 times. Continue this enquiry or close it?")
            .setPositiveButton("Continue (reset)") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { repository.resetCallCount(resolveFollowUpId(item)) }
                    loadTab(currentStage)
                }
            }
            .setNegativeButton("Cancel Entry") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { repository.updateStatus(
                        resolveFollowUpId(item), "Cancelled", "5-call limit — closed", user.mobile,
                        mobileHint = item.mobile, stageHint = item.stage
                    ) }
                    // 🔒 খাতার সারি B51: বন্ধ হয়ে যাওয়া এন্ট্রির রিমার্ক আর বাকি নেই।
                    try { PendingRemarkStore.remove(this@FollowUpActivity, item.mobile) } catch (_: Throwable) { }
                    loadTab(currentStage)
                }
            }
            .show().also { PremiumAlert.paint(it) }
    }

    /** Stage-aware 3-tap entry menu on the signal / photo block.
     *  Enquiry (Inquiry) & Visit (Patient): Continue / Reject.
     *  Patient (Treatment): Continue / Incomplete. Mirrors web signalTapMenu. */
    private fun entryActionMenu(item: FollowUpItem) {
        val isTreatmentStage = item.stage == "Treatment"
        // TK-REQUESTED REDESIGN (2026-07-20): same premium look as the
        // Patient Report Card's Take Action -- gradient header + soft
        // coloured round icon badges. Actions/logic below are UNCHANGED.
        val stageLabel = when (item.stage) {
            "Treatment" -> "Patient"
            "Patient" -> "Visit"
            else -> "Enquiry"
        }
        fun dp(v: Int) = dpx(v)
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        val header = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(11))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(android.graphics.Color.parseColor("#0B2B59"), android.graphics.Color.parseColor("#16A36D"))
            ).apply { cornerRadii = floatArrayOf(dp(6).toFloat(), dp(6).toFloat(), dp(6).toFloat(), dp(6).toFloat(), 0f, 0f, 0f, 0f) }
        }
        header.addView(android.widget.TextView(this).apply {
            text = "⚡  Take Action"
            textSize = 16.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
        })
        header.addView(android.widget.TextView(this).apply {
            text = "${item.name.ifBlank { item.mobile }}  ·  $stageLabel"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#DCE8F0"))
            setPadding(0, dp(2), 0, 0)
        })
        root.addView(header)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(0, dp(3), 0, dp(3))
        }
        root.addView(container)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this)
            .setView(android.widget.ScrollView(this).apply { addView(root) })
            .setNegativeButton("Close", null)
            .create()

        fun actionRow(icon: String, label: String, tint: String, onClick: () -> Unit) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(8), dp(16), dp(8))
                isClickable = true; isFocusable = true
                val ov = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, ov, true)
                setBackgroundResource(ov.resourceId)
                setOnClickListener { onClick() }
            }
            val badge = android.widget.TextView(this).apply {
                text = icon; textSize = 13.5f; gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(11) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor("#22" + tint.removePrefix("#")))
                }
            }
            val tv = android.widget.TextView(this).apply {
                text = label; textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#10223A"))
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(badge); row.addView(tv)
            container.addView(row)
        }

        actionRow("✅", "Continue (keep active)", "#0C9E33") {
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { repository.updateStatus(
                    resolveFollowUpId(item), "Active", "Continued", user.mobile,
                    mobileHint = item.mobile, stageHint = item.stage
                ) }
                Toast.makeText(this@FollowUpActivity, if (ok) "Continue — kept active" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                if (ok) { dialog.dismiss(); loadTab(currentStage) }
            }
        }
        val destIcon = if (isTreatmentStage) "⏳" else "🚫"
        // 🔒 খাতার সারি B113: Timeline-এর Take Action-এর সঙ্গে **একই শব্দ**, যাতে
        // দুই পর্দায় দুই নাম দেখে স্টাফ বিভ্রান্ত না হন। কাজ হুবহু আগেরটাই।
        val destLabel = if (isTreatmentStage) "Incomplete Patient" else "Reject List"
        val destTint = if (isTreatmentStage) "#B8860B" else "#D32F2F"
        actionRow(destIcon, destLabel, destTint) {
            val status = if (isTreatmentStage) "Incomplete" else "Cancelled"
            val remark = if (isTreatmentStage) "Marked Incomplete" else "Rejected"
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    repository.updateStatus(
                        resolveFollowUpId(item), status, remark, user.mobile,
                        mobileHint = item.mobile, stageHint = item.stage
                    )
                }
                // 🔒 খাতার সারি B51: এন্ট্রিটাই বাতিল হয়ে গেলে আর রিমার্ক চাওয়ার
                // মানে হয় না — বাকির তালিকা থেকে নামটা তুলে দেওয়া হয়।
                if (ok) { try { PendingRemarkStore.remove(this@FollowUpActivity, item.mobile) } catch (_: Throwable) { } }
                Toast.makeText(this@FollowUpActivity,
                    if (ok) (if (isTreatmentStage) "Moved to Incomplete list" else "Moved to Reject list") else "Failed — check connection",
                    Toast.LENGTH_SHORT).show()
                if (ok) { dialog.dismiss(); loadTab(currentStage) }
            }
        }
        // TK-REQUESTED ADDITION (2026-07-20): "Mark Arrived (এসেছেন)" straight
        // from the Visit and Patient cards, so a patient who walks in can be
        // marked into today's Chamber Attendance without opening the Chamber
        // screen and searching for them. Not shown on Enquiry cards (they are
        // not yet a Visit/Patient). Uses the same offline-safe markArrived the
        // Chamber screen uses; the mark shows on today's board (matched by
        // last-10 digits, so the mobile format doesn't matter).
        if (item.stage == "Patient" || item.stage == "Treatment") {
            actionRow("🏥", "Mark Arrived (এসেছেন)", "#0E7C7B") {
                dialog.dismiss()
                markArrivedFromCard(item.name, item.mobile, item.branch)
            }
        }
        // ── TK-FINAL (28.07.2026, প্রুফ ১৪): বাকি পাঁচটা বার্তা ─────────────
        // কোনো নতুন পর্দা নয়, কোনো নতুন ক্লাউড-কল নয়। কার্ডে যে নাম · বিল ·
        // জমা · তারিখ আগে থেকেই আছে, বার্তা তা থেকেই তৈরি হয়। কোনো সারি
        // রোগীর অবস্থা, তালিকা বা টাকার হিসাব বদলায় না — শুধু বার্তার
        // বাক্সটা তোলে, যেটা আগে থেকেই অ্যাপে আছে।
        val msgDue = kotlin.math.max(0.0, item.bill - item.paid)
        val isPaidStage = item.stage == "Patient" || item.stage == "Treatment"

        if (isPaidStage && msgDue > 0.0) {
            actionRow("💰", "Due Reminder", "#D32F2F") {
                dialog.dismiss()
                PatientMessage.show(
                    activity = this, branch = item.branch, name = item.name,
                    mobile = item.mobile, patientId = item.patientId,
                    kind = PatientMessage.Kind.DUE_REMINDER,
                    bill = item.bill, paid = item.paid
                )
            }
        }
        if (isPaidStage && item.paid > 0.0) {
            actionRow("🧾", "Send Receipt", "#0C9E33") {
                dialog.dismiss()
                // TK-APPROVED ADDITION (31.07.2026): Receipt Number এখন এই
                // পেশেন্টের সবচেয়ে সাম্প্রতিক Saved payment row-এর নিজের
                // `id` থেকে আসে (আন্দাজ/Placeholder নয়)। না পাওয়া গেলে
                // লাইনটাই বাদ যায় (আগের মতোই), Receipt পাঠানো আটকায় না।
                lifecycleScope.launch {
                    val rn = try {
                        withContext(Dispatchers.IO) {
                            val rows = SupabaseClient.fetchList("payments", "patientId=eq.${item.patientId}", 1)
                            if (rows.length() > 0) rows.getJSONObject(0).optString("id", "") else ""
                        }
                    } catch (_: Throwable) { "" }
                    PatientMessage.show(
                        activity = this@FollowUpActivity, branch = item.branch, name = item.name,
                        mobile = item.mobile, patientId = item.patientId,
                        kind = PatientMessage.Kind.RECEIPT,
                        bill = item.bill, paid = item.paid,
                        dateText = FollowUpModel.displayDate(FollowUpModel.today()),
                        whatsAppOnly = true, receiptNumber = rn
                    )
                }
            }
        }
        if (item.nextFollow.isNotBlank()) {
            actionRow("⏰", "Visit Reminder", "#1067D8") {
                dialog.dismiss()
                PatientMessage.show(
                    activity = this, branch = item.branch, name = item.name,
                    mobile = item.mobile, patientId = item.patientId,
                    kind = PatientMessage.Kind.VISIT_REMINDER,
                    dateText = FollowUpModel.displayDate(item.nextFollow)
                )
            }
        }
        if (isPaidStage) {
            actionRow("📄", "Send Document (Rx · Diet · Test)", "#16A36D") {
                dialog.dismiss()
                sendDocumentMenu(item)
            }
            actionRow("🎉", "Treatment Complete", "#0B2B59") {
                dialog.dismiss()
                PatientMessage.show(
                    activity = this, branch = item.branch, name = item.name,
                    mobile = item.mobile, patientId = item.patientId,
                    kind = PatientMessage.Kind.TREATMENT_DONE
                )
            }
        }
        dialog.show()
        PremiumAlert.paint(dialog)
    }

    /** TK-FINAL (28.07.2026): "Send Document" — আগে রোগীকে WhatsApp-এ একটা
     *  ছোট বার্তা যায়, তারপর যে কাগজটা দরকার সেই পর্দাটাই খোলে, যেখানে
     *  আগে থেকেই SAVE / SHARE / PRINT আছে। নতুন কোনো ছাপার কাজ লেখা হয়নি —
     *  Global Search-এ যেভাবে খোলে, হুবহু সেভাবেই খোলে। */
    private fun sendDocumentMenu(item: FollowUpItem) {
        val labels = arrayOf("📝 Prescription", "🥗 Diet Chart", "🩸 Blood Test")
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Send Document"))
            .setItems(labels) { _, which ->
                PatientMessage.show(
                    activity = this, branch = item.branch, name = item.name,
                    mobile = item.mobile, patientId = item.patientId,
                    kind = PatientMessage.Kind.DOCUMENT,
                    whatsAppOnly = true
                ) {
                    val target: Class<*> = when (which) {
                        0 -> com.tkbiswas.pilesclinic.clinical.PrescriptionActivity::class.java
                        1 -> com.tkbiswas.pilesclinic.clinical.DietChartActivity::class.java
                        else -> com.tkbiswas.pilesclinic.clinical.InvestigationAdviceActivity::class.java
                    }
                    openClinicalDocForItem(item, target)
                }
            }
            .setNegativeButton("Close", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** Global Search-এর `openClinicalDoc`-এর হুবহু একই ধাপ — একই RoleSession,
     *  একই পর্দা। এখানে শুধু SearchHit-এর বদলে FollowUpItem থেকে নেওয়া হয়। */
    private fun openClinicalDocForItem(item: FollowUpItem, target: Class<*>) {
        val roleStr = if (user.role.equals("doctor", true)) "DOCTOR" else "STAFF"
        val digits = item.mobile.filter { it.isDigit() }.takeLast(10)
        // 🔒🔒 খাতার সারি B174 (TK, 30.07.2026 — "প্রেসক্রিপশনে বয়স/লিঙ্গ/
        // ঠিকানা auto-fill হচ্ছে না")। **আসল কারণ:** এখানে আগে খালি স্ট্রিং
        // ("", "", "") পাঠানো হত, অথচ `item` (FollowUpItem)-এর নিজের ঘরেই
        // এই তথ্য আগে থেকেই আছে — এমনিতেই তালিকার সঙ্গে নামা, বাড়তি কোনো
        // ক্লাউড-কল লাগেনি।
        com.tkbiswas.pilesclinic.clinical.RoleSession.applyFrom(
            roleStr, item.name, digits, item.branch, digits,
            item.address, item.age, item.sex, item.disease,
            // 🔒 খাতার সারি B175: ছাপায় মানুষ-পড়া-যায় Patient ID দেখাতে
            // (raw মোবাইল-ভিত্তিক আইডির বদলে)।
            patientDisplayId = item.patientId
        )
        startActivity(android.content.Intent(this, target))
    }

    /** TK-REQUESTED (2026-07-20): shared Mark-Arrived confirm used by the
     *  Visit/Patient card action menu. Marks the patient into TODAY's chamber. */
    private fun markArrivedFromCard(name: String, mobile: String, branch: String) {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            Toast.makeText(this, "No valid 10-digit mobile to mark", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Mark Arrived?"))
            .setMessage(NoBengali.s("Mark ${name.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?"))
            .setPositiveButton("Yes, Arrived") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            ChamberAttendanceRepository.markArrived(this@FollowUpActivity, "+91$digits", name, branch, user.mobile)
                            true
                        } catch (_: Throwable) { false }
                    }
                    Toast.makeText(this@FollowUpActivity, if (ok) "Marked Arrived ✅" else "Could not mark — please retry", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** Continue / Cancel Entry menu (triple-tap on a card). */
    private fun showContinueCancelMenu(item: FollowUpItem) {
        val user = NativeSession.current(this) ?: return
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Entry Action — ${item.name.ifBlank { item.mobile }}"))
            .setItems(arrayOf("✅ Continue (keep active)", "🚫 Cancel Entry")) { _, which ->
                val status = if (which == 0) "Active" else "Cancelled"
                val remark = if (which == 0) "Continued" else "Entry cancelled"
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { repository.updateStatus(
                        resolveFollowUpId(item), status, remark, user.mobile,
                        mobileHint = item.mobile, stageHint = item.stage
                    ) }
                    Toast.makeText(
                        this@FollowUpActivity,
                        if (ok) (if (which == 0) "Continued" else "Entry cancelled") else "Failed — check connection",
                        Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) loadTab(currentStage)
                }
            }
            .setNegativeButton("Close", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** TK APPROVED (2026-07-15): Visit card's new "Blood Test" label — same
     *  RoleSession wiring as openClinicalMenu() above, but skips the 4-option
     *  grid and opens Investigation Advice directly (only Blood Test, nothing
     *  else). That screen already has Save & Print + Share as Text. */
    private fun openBloodTestDirect(item: FollowUpItem) {
        val roleStr = if (user.role.equals("doctor", true)) "DOCTOR" else "STAFF"
        com.tkbiswas.pilesclinic.clinical.RoleSession.applyFrom(
            roleStr,
            item.name,
            item.patientId.ifBlank { item.id },
            item.branch,
            item.mobile,
            item.address,
            item.age,
            item.sex,
            item.disease,
            // 🔒 খাতার সারি B175: ছাপায় মানুষ-পড়া-যায় Patient ID নিশ্চিত করতে
            // আলাদা ঘরে পাঠানো হলো। ⛔ উপরের তৃতীয় ঘরের (raw/মানুষ-পড়া-যায়
            // যেটাই থাক) পুরনো নিয়ম **একটুও বদলানো হয়নি** — সেভ/খোঁজার সঙ্গে
            // জড়িত থাকতে পারে বলে ইচ্ছে করেই ছোঁয়া হয়নি।
            patientDisplayId = item.patientId
        )
        startActivity(android.content.Intent(this, com.tkbiswas.pilesclinic.clinical.InvestigationAdviceActivity::class.java))
    }

    /**
     * Prescription text on the Patient card opens a menu with EXACTLY four
     * clinical documents (no Doctor Check-up, no Patient History). The real
     * logged-in role is passed through so Doctor keeps edit rights and Staff
     * follows the existing rules. Payment %, Bill and Due stay separate.
     */
    private fun openClinicalMenu(item: FollowUpItem) {
        val roleStr = if (user.role.equals("doctor", true)) "DOCTOR" else "STAFF"
        com.tkbiswas.pilesclinic.clinical.RoleSession.applyFrom(
            roleStr,
            item.name,
            item.patientId.ifBlank { item.id },
            item.branch,
            item.mobile,
            item.address,
            item.age,
            item.sex,
            item.disease,
            // 🔒 খাতার সারি B175 — একই কারণ, একই সমাধান।
            patientDisplayId = item.patientId
        )
        val options = arrayOf("📝 Prescription", "💊 Medicine Slip", "🩸 Blood Test", "🥗 Diet Chart")
        val targets = arrayOf(
            com.tkbiswas.pilesclinic.clinical.PrescriptionActivity::class.java,
            com.tkbiswas.pilesclinic.clinical.MedicineSlipActivity::class.java,
            com.tkbiswas.pilesclinic.clinical.InvestigationAdviceActivity::class.java,
            com.tkbiswas.pilesclinic.clinical.DietChartActivity::class.java
        )
        showClinicalGridDialog(item, options, targets)
    }

    /** APPROVED UPDATE #6: Clinical Documents popup as a professional compact
     *  2x2 grid (Prescription / Medicine Slip / Blood Test / Diet Chart)
     *  instead of a plain list.
     *  TK APPROVED (2026-07-15): premium navy header + Prescription/Medicine
     *  Slip emphasised as bigger, coloured, primary cards in the top row
     *  (green for Prescription/Ayurvedic, blue for Medicine Slip/Allopathic),
     *  with a light scale+fade press animation; Blood Test/Diet Chart stay as
     *  smaller secondary cards underneath. Same 4 targets, same navigation —
     *  only the look changed. */
    private fun showClinicalGridDialog(
        item: FollowUpItem, options: Array<String>, targets: Array<out Class<*>>
    ) {
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        fun rounded(colorHex: String, strokeHex: String?, radiusDp: Int) = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(radiusDp).toFloat()
            setColor(android.graphics.Color.parseColor(colorHex))
            if (strokeHex != null) setStroke(dp(2), android.graphics.Color.parseColor(strokeHex))
        }

        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        root.background = rounded("#FFFFFF", null, 20)

        val header = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_header_navy_top_round)
        }
        header.addView(android.widget.TextView(this).apply {
            text = "Clinical Document"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
        })
        header.addView(android.widget.TextView(this).apply {
            text = PatientIdText.line(item.name, item.mobile, item.patientId)
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#B8C6D8"))
            val p = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(3); layoutParams = p
        })
        root.addView(header)

        val body = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(6))
        }
        root.addView(body)

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this).setView(root).setCancelable(true).create()

        fun animatedOpen(which: Int, view: android.view.View) {
            view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(90).withEndAction {
                    dialog.dismiss()
                    startActivity(android.content.Intent(this@FollowUpActivity, targets[which]))
                }.start()
            }.start()
        }

        fun primaryCell(label: String, subtitle: String, iconRes: Int, which: Int, badgeHex: String, textHex: String): android.widget.LinearLayout {
            return android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                background = rounded("#FFFFFF", "#E7EAEF", 16)
                setPadding(dp(8), dp(12), dp(8), dp(12))
                val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(dp(5), dp(5), dp(5), dp(5)); layoutParams = lp
                val badge = android.widget.FrameLayout(this@FollowUpActivity).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(dp(38), dp(38))
                    background = rounded(badgeHex, null, 11)
                }
                badge.addView(android.widget.ImageView(this@FollowUpActivity).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(dp(20), dp(20)).apply { gravity = android.view.Gravity.CENTER }
                    setImageResource(iconRes)
                })
                addView(badge)
                addView(android.widget.TextView(this@FollowUpActivity).apply {
                    text = label; textSize = 12.5f; gravity = android.view.Gravity.CENTER
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor(textHex))
                    val p = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                    p.topMargin = dp(6); layoutParams = p
                })
                addView(android.widget.TextView(this@FollowUpActivity).apply {
                    text = subtitle; textSize = 9.5f; gravity = android.view.Gravity.CENTER
                    setTextColor(android.graphics.Color.parseColor("#8A93A3"))
                })
                isClickable = true; isFocusable = true
                setOnClickListener { animatedOpen(which, this) }
            }
        }

        fun secondaryCell(label: String, iconRes: Int, which: Int, badgeHex: String): android.widget.LinearLayout {
            return android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                background = rounded("#F5F7FA", "#E1E6ED", 14)
                setPadding(dp(8), dp(10), dp(8), dp(10))
                val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(dp(5), dp(5), dp(5), dp(5)); layoutParams = lp
                val badge = android.widget.FrameLayout(this@FollowUpActivity).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(dp(32), dp(32))
                    background = rounded(badgeHex, null, 9)
                }
                badge.addView(android.widget.ImageView(this@FollowUpActivity).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(dp(17), dp(17)).apply { gravity = android.view.Gravity.CENTER }
                    setImageResource(iconRes)
                })
                addView(badge)
                addView(android.widget.TextView(this@FollowUpActivity).apply {
                    text = label; textSize = 11f; gravity = android.view.Gravity.CENTER
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                    val p = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                    p.topMargin = dp(5); layoutParams = p
                })
                isClickable = true; isFocusable = true
                setOnClickListener { animatedOpen(which, this) }
            }
        }

        val row1 = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
        row1.addView(primaryCell(options[0], "Ayurvedic · Rx", com.tkbiswas.pilesclinic.R.drawable.ic_prescription_ayurvedic, 0, "#2FA86B", "#0B4F2A"))
        row1.addView(primaryCell(options[1], "Allopathic · Slip", com.tkbiswas.pilesclinic.R.drawable.ic_medicine_allopathic, 1, "#EFF1F5", "#10223A"))
        body.addView(row1)

        val row2 = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            val p = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = dp(4); layoutParams = p
        }
        row2.addView(secondaryCell(options[2], com.tkbiswas.pilesclinic.R.drawable.ic_blood_test_tube, 2, "#EC4899"))
        row2.addView(secondaryCell(options[3], com.tkbiswas.pilesclinic.R.drawable.ic_diet_chart_bowl, 3, "#F59E0B"))
        body.addView(row2)

        val close = android.widget.TextView(this).apply {
            text = "CLOSE"
            gravity = android.view.Gravity.CENTER
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(14), 0, dp(6))
            isClickable = true; isFocusable = true
            setOnClickListener { dialog.dismiss() }
        }
        body.addView(close)

        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    private fun showNextFollowPicker(item: FollowUpItem) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, d ->
            val cal2 = Calendar.getInstance().apply { set(y, m, d) }
            val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal2.time)
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) { repository.updateNextFollow(resolveFollowUpId(item), iso) }
                Toast.makeText(this@FollowUpActivity, if (ok) "Next follow-up set" else "Failed — check connection", Toast.LENGTH_SHORT).show()
                if (ok) loadTab(currentStage)
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = cal.timeInMillis
        }.show()
    }
}
