package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.clinical.ClinicalModulesActivity
import com.tkbiswas.pilesclinic.clinical.RoleSession
import com.tkbiswas.pilesclinic.databinding.ActivityDoctorqueueBinding
import com.tkbiswas.pilesclinic.print.PrintCenterActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Native rebuild -- Visit / Doctor Queue.
 *
 * Lists patients currently waiting for the doctor (branch-scoped, same rule as
 * every other native screen), matching the WebView's doctorQueue()/visitQueueRows().
 * Each patient offers three actions, matching the WebView's queue card buttons:
 *   - Check-up  -> opens the existing native Clinical hub for that patient
 *                  (Doctor Check-up / Prescription / Diet / Investigation),
 *                  passing the patient via RoleSession's documented extras.
 *   - Summary   -> opens the same Clinical hub (which contains the patient's
 *                  clinical history), in the current user's role.
 *   - Print     -> opens the native Print Center.
 *
 * Role gate matches doctorQueue()'s allowed roles: master, doctor, staff.
 *
 * SCOPED LIMITATION (honest disclosure):
 * - The WebView remembers the "last opened" queue patient (rk_last_visit_queue_id)
 *   and floats it to the top even after it leaves the normal queue condition.
 *   That is a local-convenience nicety, not part of the shared clinic data, so
 *   it is intentionally not reproduced here; the queue is otherwise identical.
 */
class DoctorQueueActivity : AppCompatActivity() {

    // TK-REQUESTED PROACTIVE FIX (2026-07-25): the same overlapping.refresh
    // guard already proven on Follow-up and Chamber Attendance. Two loads can
    // overlap (screen reopened, an action refreshing, a slow first fetch
    // finishing late) . without this the OLDER result could land last and
    // overwrite fresh data on screen. Only the newest load may paint now.
    private var loadGuardToken = 0

    private lateinit var binding: ActivityDoctorqueueBinding
    private lateinit var repository: DoctorQueueRepository
    private lateinit var adapter: DoctorQueueAdapter
    private lateinit var user: NativeUser
    // TK-REQUESTED (2026-07-20): "Pending / Overdue" starts collapsed --
    // only "Today" is open by default; tapping the Overdue header reveals it.
    // 🟢🔒 B659 (15.08.2026, TK-অনুমোদিত · Egress-২): onCreate-এ একবার তালিকা টানা হয়,
    //   তারপর Android নিজেই onResume ডাকে — ফলে পর্দা একবার খুললেই **দু'বার** সব রোগীর
    //   base64 ছবি নামত। এই চিহ্নটা প্রথম onResume-এর বাড়তি ডাকটা বাদ দেয়।
    //   ⛔ চেকআপ থেকে ফেরা/অন্য অ্যাপ থেকে ফেরার রিফ্রেশ আগের মতোই হয়।
    private var skipNextResumeLoad = false

    private var doneExpanded = false
    private var lastPendingItems = listOf<QueuePatient>()
    private var lastDoneItems = listOf<QueuePatient>()

    /* 🟢🔒 V1504 (১৫.০৯.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ পাশ) — New/Old ছাঁকার
       বাছাই। null = সবাই (ডিফল্ট), "NEW"/"OLD" = শুধু সেটাই। একই বোতামে
       আবার চাপলে বা অন্যটায় চাপলে বদলায়; একটাও রোগীর ডেটা মোছে না, শুধু
       এই পর্দায় দেখানো হয় কিনা। */
    private var queueBadgeFilter: String? = null

    // 🔔 খাতার সারি B151 (TK, 30.07.2026) — নিজে থেকে নতুন হওয়ার ব্যবস্থা।
    // ⛔ নিয়ম ও সময় দুটোই `LiveRefresh`-এ, তাই চার পর্দায় চার নিয়ম হতে পারে না।
    private val autoHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var autoScreenFocused = true
    private var autoBusy = false
    private val autoWatch = LiveRefresh.Watch("patients")

    /* 🔍🔒 V972 (০২.০৯.২০২৬, TK-নির্দেশ) — *"এখানে patient Search করার মত
       অপশন থাকতে হবে"*। শুধু পর্দায় ছাঁকা হয় — ক্লাউডে একটাও নতুন অনুরোধ
       যায় না, তাই ফ্রি প্ল্যানে বাড়তি চাপ নেই।
       ⛔ তালিকা আনা · সাজানো · Today/Overdue ভাগ — কিছুই বদলায়নি। */
    private var queueSearch = ""
    /* 🔍🔒 V1108 — আজকের লাইনের বাইরে পাওয়া রোগীরা। ⛔ শুধু দেখানোর জন্য;
       আসল তালিকা (`lastPendingItems`/`lastDoneItems`) ছোঁয়া হয় না। */
    private var wideResults: List<QueuePatient> = emptyList()
    private var wideForQuery = ""       // কোন লেখার ফল — একই লেখায় আবার অনুরোধ যায় না
    private var wideSearching = false
    private val wideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var wideTask: Runnable? = null
    private val autoTick = object : Runnable {
        override fun run() {
            try { autoCheckForChanges() } catch (_: Throwable) { }
            autoHandler.postDelayed(this, LiveRefresh.TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorqueueBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        repository = DoctorQueueRepository(this)

        val session = NativeSession.current(this)
        if (session == null) { finish(); return }
        user = session

        if (!(user.role == "master" || user.role == "doctor" || user.role == "staff")) {
            Toast.makeText(this, "Master / Doctor / Staff only", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        adapter = DoctorQueueAdapter(
            this, emptyList<QueueRow>(),
            /* 🩺🔒 V839 (২৯.০৮.২০২৬, TK-নির্দেশ: *"আগে মনে করিয়ে দেয়"*) —
               Check-up চাপলে **ফর্ম খোলার আগে** গত বারের প্ল্যান দেখানো হয়।
               ⛔ প্ল্যান না থাকলে পপ-আপ আসেই না — সরাসরি আগের মতোই খোলে।
               ⛔ দিনে **একবারই** (নিচে `nvpReminderShown` দেখুন)। */
            onCheckup = { p ->
                showNvpReminderThen(p) {
                    openClinical(p, asDoctor = user.role == "doctor", autoOpen = "CHECKUP")
                }
            },
            // 🔴 TK-নির্দেশ (04.08.2026, আলোচনার পরে — অপশন ১ বেছেছেন):
            // "Journey" বোতাম এখন সরাসরি পূর্ণ চিকিৎসা-ইতিহাস (Full Journey)
            // খোলে -- প্রথম আসার দিনের অভিযোগ থেকে শুরু করে checkup ·
            // blood test · prescription · diet chart · প্রতিটা এন্ট্রির
            // পূর্ণ বিবরণ, তারিখ-ক্রমে। নতুন কোনো বোতাম/স্ক্রিন লাগেনি --
            // Patient Timeline-এর আগে থেকে থাকা `fullJourney` extra-ই
            // ব্যবহার হলো (যেটা "🧭 Full Journey" বোতাম নিজেও পাঠায়)।
            // ⛔ প্রথমবার আসা রোগীর জন্য এতে তেমন বাড়তি কিছু দেখাবে না
            // (তাঁর ইতিহাসই কম) -- ঝুঁকিহীন, কারো কিছু হারায় না।
            onFullJourney = { p ->
                startActivity(Intent(this, PatientTimelineActivity::class.java)
                    .putExtra("mobile", p.mobile)
                    .putExtra("fullJourney", true))
            },
            // TK-REQUESTED (2026-07-28): the new fourth button opens this
            // patient's Report Card -- the very same screen (and the same
            // "mobile" it is opened with) that Chamber Date and Dr. Visit
            // already use, so nothing new had to be built for it.
            onReportCard = { p ->
                /* 🔵🔒 V522 (২২.০৮.২০২৬, TK-নির্দেশ): এক নম্বরে দুজন আলাদা রোগী থাকলে
                   Report Card যেন **এই** রোগীরই খোলে ও ছাপে।
                   ⛔ আইডি ফাঁকা/না মিললে হুবহু আগের আচরণ। */
                startActivity(Intent(this, ReportCardActivity::class.java)
                    .putExtra("mobile", p.mobile)
                    .putExtra("patientRowId", p.id)
                    .putExtra("patientCode", p.patientId))
            },
            onHeaderTap = { doneExpanded = !doneExpanded; renderRows() }
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
            loadQueue(withPhotos = false)
            binding.swipeRefresh.postDelayed({
                try { binding.swipeRefresh.isRefreshing = false } catch (_: Throwable) {}
            }, 2500L)
        }

        binding.btnBack.setOnClickListener { finish() }
        /* 🔄🔒 V972 (TK-রিপোর্ট ছবিসহ: *"উপরে ডান দিকে Refreshing icon কোন কাজ
           করে না"*) — **আসল কারণ (যাচাই করা):** বোতামটা কাজ করত, তালিকা নতুন
           করে আনত; কিন্তু জমানো তালিকা সঙ্গে সঙ্গে আঁকা হয় বলে **চোখে কোনো
           বদল দেখা যেত না** ⇒ মনে হত কিছুই হয়নি। এখন চাপলে ছোট একটা বার্তা।
           ⛔ স্পিনার ঘোরানো হয়নি (TK-এর পুরনো নির্দেশ), তালিকা আনার পথও একই। */
        binding.btnRefresh.setOnClickListener {
            loadQueue(withPhotos = false)
            /* ✅ V983 — "কতজন অপেক্ষায়" গোনায় **হয়ে যাওয়া** রোগী ধরা হয় না। */
            val waiting = lastPendingItems.size
            android.widget.Toast.makeText(
                this, "Queue refreshed  ·  $waiting waiting", android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        /* 🟢🔒 V1504 (১৫.০৯.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ পাশ) — New/Old
           ছাঁকার মেনু। একই বাছাইয়ে আবার চাপলে সবাই ফিরে আসে (টগল)।
           ⛔ নতুন কোনো ক্লাউড-অনুরোধ নেই — শুধু পর্দায় ছাঁকা হয়। */
        binding.btnQueueFilter.setOnClickListener { v ->
            val menu = android.widget.PopupMenu(this@DoctorQueueActivity, v)
            menu.menu.add(0, 1, 0, "New")
            menu.menu.add(0, 2, 1, "Old")
            menu.setOnMenuItemClickListener { item ->
                val picked = if (item.itemId == 1) "NEW" else "OLD"
                queueBadgeFilter = if (queueBadgeFilter == picked) null else picked
                renderRows()
                true
            }
            menu.show()
        }
        // 🔍 V972 — লেখামাত্র তালিকা ছাঁকে (নতুন কোনো ক্লাউড-অনুরোধ নেই)।
        binding.etQueueSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                queueSearch = s?.toString().orEmpty().trim().lowercase()
                renderRows()
                /* 🔍🔒 V1108 (TK-নির্দেশ: *"সার্চ বক্সে নাম টাইপ করলেই চলে আসতে
                   হবে"*) — লেখা থামার পরেই আজকের লাইনের **বাইরের** রোগীও
                   নিজে থেকে উঠে আসে। ⛔ ৩ অক্ষরের কম হলে একটাও অনুরোধ যায় না। */
                scheduleWideSearch()
            }
            override fun beforeTextChanged(t: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(t: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        /* 🔍 V1013 → 🔵 V1107 — কীবোর্ডের Search চাপলেও **টাইপ করা নামটা সঙ্গে
           যায়** (আগে যেত না, তাই নতুন পর্দায় আবার লিখতে হত)। */
        binding.etQueueSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                openAllPatientSearch()
                true
            } else false
        }
        setupBranchPicker()   // খাতার সারি B42 — শুধু মাস্টারের পর্দায় দেখা যায়

        skipNextResumeLoad = true   // 🟢 B659: onCreate-এর পরেই আসা onResume-এ আর দ্বিতীয়বার টানা হবে না
        // 🟢🔒 B660 (15.08.2026, TK-অনুমোদিত · Egress-২ পরের ধাপ): পর্দা খুললে আগে **ওই
        //   ব্রাঞ্চের সব রোগীর** base64 ছবি নামত (limit 5000, লাইনে কে আছে তা ফোনে বসে
        //   ছাঁকা হয়) — দিনে যতবার পর্দা খোলা হত ততবার। এটাই ছিল Egress শেষ হওয়ার সবচেয়ে
        //   বড় কারণ। এখন ছবি ছাড়া টানা হয়, ছবি ফোনে জমানো তালিকা থেকে বসে, আর যাঁর ছবি
        //   ফোনে নেই **শুধু তাঁর** ছবিটুকু আনা হয় (৫০ জন করে ভাগে) — তাও শুধু লাইনে
        //   দাঁড়ানো রোগীদের। ⛔ পর্দায় দেখতে কোনো বদল নেই।
        //   🔵🔒 EGRESS-SAFE (19.08.2026): Refresh / pull-to-refresh / branch change-ও
        //     একই slim path ব্যবহার করে। আগের ছবি cache থেকে থাকে; লাইনে নতুন যাঁদের
        //     ছবি cache-এ নেই, শুধু তাঁদের `id,photo` ছোট batch-এ আসে — পুরো branch-এর ছবি নয়।
        loadQueue(withPhotos = false)
    }

    override fun onResume() {
        super.onResume()
        // Coming back from a check-up (which may mark the patient complete) should
        // refresh the queue so a seen patient drops off, matching the WebView's
        // re-render after doctorCheck().
        // 🟢🔒 B659 (15.08.2026, TK-অনুমোদিত · Egress-২): তালিকা আগের মতোই নতুন হয়, শুধু
        //   ছবি ছাড়া টানা হয় (withPhotos=false) — ছবি ফোনে জমানো তালিকা থেকে বসে যায়
        //   (DoctorQueueRepository.fillPhotosFromCache), আর একেবারে নতুন রোগীর ক্ষেত্রে
        //   শুধু তার id-র ছবিটুকু আনা হয়। ⛔ পর্দায় দেখতে কোনো বদল নেই।
        if (::adapter.isInitialized) {
            if (skipNextResumeLoad) skipNextResumeLoad = false
            else loadQueue(withPhotos = false)
        }
        // 🔔 খাতার সারি B151 (TK, 30.07.2026): *"রিফ্রেস না টানলেও যেন ডাক্তার
        //    চেকআপ ... কার্যকরী হয়।"* — এই পর্দা আগে **নিজে থেকে কখনো নতুন হত না**।
        autoHandler.removeCallbacks(autoTick)
        autoHandler.postDelayed(autoTick, LiveRefresh.TICK_MS)
        autoWatch.startRealtime({ shownBranch() }) {
            if (!::adapter.isInitialized || !::repository.isInitialized || !autoScreenFocused) return@startRealtime
            loadQueue(withPhotos = false, useDelta = true)
        }
    }

    override fun onPause() {
        super.onPause()
        // ⛔ পর্দা সামনে না থাকলে একটাও প্রশ্ন যাবে না।
        autoHandler.removeCallbacks(autoTick)
        autoWatch.stopRealtime()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 🔒 পপ-আপ/বাক্স খোলা থাকলে রিফ্রেশ বন্ধ — চালু কাজ যেন না ভাঙে।
        autoScreenFocused = hasFocus
    }

    /**
     * 🔔 খাতার সারি B151 — নিয়মটা `LiveRefresh`-এ এক জায়গায় (চেম্বার · Follow-up-ও
     * একই নিয়মে)। ৩০ সেকেন্ডে শুধু একটা ছোট প্রশ্ন; **বদলালে তবেই** তালিকা নামে।
     * ⛔ রাত ১০টা – সকাল ৬টা কিছুই যায় না · উত্তর না এলে কিছুই করা হয় না।
     */
    private fun autoCheckForChanges() {
        if (!::adapter.isInitialized || !::user.isInitialized || !::repository.isInitialized) return
        if (!autoScreenFocused || autoBusy) return
        if (!LiveRefresh.awake()) return
        val br = shownBranch()
        autoBusy = true
        lifecycleScope.launch {
            try {
                val changed = withContext(Dispatchers.IO) {
                    autoWatch.changed("queue|$br", br)
                }
                if (isFinishing || isDestroyed) return@launch
                if (!changed) return@launch
                // 🟢🔒 B632 (11.08.2026, Egress ফিক্স): ৩০-সেকেন্ডের অটো-রিফ্রেশ এখন ছবি ছাড়া টানে
                //   (includePhoto=false)। ফোনে আগে থেকে জমানো ছবি রেখে দেওয়া হয় (saveCachedQueue-এর
                //   B625 backfill), তাই কার্ডে ছবি অটুট। এর আগে প্রতি ৩০ সেকেন্ডে ওই ব্রাঞ্চের সব রোগীর
                //   base64 ছবি বারবার নামত — Free-plan egress (১৫GB) শেষ হওয়ার সবচেয়ে বড় কারণ ছিল এটাই।
                //   ছবি এখন শুধু পর্দা খোলা/onResume/ব্রাঞ্চ বদলানোর সময় নামে (withPhotos=true, ডিফল্ট)।
                if (br == shownBranch() && autoScreenFocused) loadQueue(withPhotos = false, useDelta = true)
            } catch (_: Throwable) {
            } finally {
                autoBusy = false
            }
        }
    }

    // TK-REQUESTED ADDITION (2026-07-20): "at least the data that was already
    // on the phone before should show up" -- on a slow connection this
    // screen used to sit on a blank spinner until the network call finished.
    // Now: if a cached result exists from last time, show it INSTANTLY
    // (no spinner), then quietly fetch fresh data in the background and
    // swap it in when ready. The blank spinner + "waiting" state is now
    // only seen on the very first-ever load of this screen (no cache yet)
    // or if the cache itself is empty.
    /**
     * 🔒 TK-REQUESTED (28.07.2026, খাতার সারি B42): *"আমি তো মাস্টার এডমিন,
     * আমাকে তো ব্রাঞ্চ সিলেক্ট করতে হবে। হেডারের ডানপাশে ছোট বক্সের মধ্যে সমস্ত
     * ব্রাঞ্চ চুজ করা যাবে এবং যেকোনো ব্রাঞ্চ চুজ করা যাবে।"*
     *
     * ⛔ Follow-up পর্দায় TK-এর আগেই পাশ করা **হুবহু একই বাক্স ও একই নিয়ম** —
     * নতুন কোনো ডিজাইন তৈরি করা হয়নি।
     * ⛔ **স্টাফ ও ডাক্তারের জন্য কিচ্ছু বদলায়নি** — তাঁরা আগের মতোই শুধু নিজের
     * ব্রাঞ্চ দেখেন, বাক্সটাই তাঁদের পর্দায় থাকে না।
     */
    private var pickedBranch: String = ""

    /**
     * 🟢🔒 B670 (15.08.2026, TK-অনুমোদিত) — মাস্টার শেষবার কোন ব্রাঞ্চ বেছেছিলেন,
     * শুধু সেটুকু এই ফোনে মনে রাখা হয়। ⛔ কোনো রোগীর তথ্য নয় · ক্লাউডে কিছু যায় না ·
     * কোনো নতুন অনুরোধ নেই। ⛔ তালিকায় না-থাকা নাম জমা থাকলে (ব্রাঞ্চের নাম বদলালে)
     * নিরাপদে "All" ধরা হয়, যাতে কখনো ফাঁকা পর্দা না আসে।
     */
    /**
     * 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): মনে-রাখাটা এখন **পুরো অ্যাপের একটাই
     * জায়গায়** — `BranchFilterStore`। আগে এই পর্দার নিজস্ব "doctor_queue_pick"
     * ফাইলে আলাদা করে রাখা হত (B670), তাই অন্য পর্দায় সেটা কাজে লাগত না।
     * ⛔ আচরণ এক — শুধু জায়গাটা এক হলো, আর "এখনো বাছা হয়নি" অবস্থা যোগ হলো।
     */
    private fun branchChoices() = BranchFilterStore.choices()

    private fun rememberedBranch(): String = BranchFilterStore.get(this)

    private fun rememberBranch(v: String) { BranchFilterStore.set(this, v) }

    /* 🔴🔴🔒 V456 (TK-নির্দেশ ১৮.০৮.২০২৬: "Dr. K.H MANDAL অন্য ব্রাঞ্চেও রোগী
       দেখতে পারবেন, কিন্তু শুধু Check-up-এই — বাকি সব জায়গায় নিজের ব্রাঞ্চ")।
       একজনের মোবাইল ধরে ব্যতিক্রম, ঠিক NoBengali.kt-এর প্রমাণিত একই কৌশল।
       ⛔ user.branch এখানে **বদলানো হয়নি** (session ছুঁয়ে অন্য সব পর্দাও
          প্রভাবিত হবে এই ভয়ে) — শুধু এই স্ক্রিনের `shownBranch()`/ব্রাঞ্চ-বাছাই
          মাস্টারের নিয়মে চলবে, বাকি সব পর্দা (Follow-up/Payment/Report ইত্যাদি)
          user.branch সরাসরি পড়ে বলে সেগুলো এক অক্ষরও বদলায় না। */
    private fun isCrossBranchDoctorQueueAccess(): Boolean =
        user.mobile.filter { it.isDigit() }.takeLast(10) == "7980993652"

    /** কোন ব্রাঞ্চের তালিকা দেখানো হবে — একটাই জায়গা থেকে সিদ্ধান্ত।
     *  মাস্টার কিছু না-বাছলে "" ফেরে — তখন কিচ্ছু আনা হয় না (loadQueue দেখুন)। */
    private fun shownBranch(): String =
        if (user.role == "master" || isCrossBranchDoctorQueueAccess()) pickedBranch else user.branch

    private fun setupBranchPicker() {
        if (user.role != "master" && !isCrossBranchDoctorQueueAccess()) {
            binding.branchPicker.visibility = View.GONE
            return
        }
        // 🟢🔒 B670 (15.08.2026, TK-অনুমোদিত · Egress): আগে পর্দাটা **প্রতিবার খুললেই
        //   নিজে থেকে "All" হয়ে যেত** (`pickedBranch` প্রতিবার নতুন Activity-তে ফাঁকা)।
        //   ফলে মাস্টার আগেরবার এক ব্রাঞ্চ বেছে থাকলেও পরের বার আবার **পাঁচ ব্রাঞ্চের
        //   সব রোগী** নামত (এই পর্দার পড়ায় তারিখের সীমা নেই — DoctorQueueRepository:129)।
        //   এখন শেষ বাছাইটা ফোনে মনে রাখা হয়। ⛔ দরকারে এক চাপে আগের মতোই "All" করা যায়।
        //   ⛔ staff/doctor-এর কিছুই বদলায়নি — তাঁদের বাক্সটাই পর্দায় থাকে না (উপরের if)।
        pickedBranch = rememberedBranch()
        binding.branchPicker.visibility = View.VISIBLE
        binding.branchPicker.text = BranchFilterStore.pillText(this)
        binding.branchPicker.setOnClickListener {
            // 🟢 B670: তালিকাটা এখন একটাই জায়গায় (`branchChoices()`) — নইলে দুই জায়গায়
            //   দুই তালিকা হয়ে গেলে "মনে রাখা" নামটা ভুল করে বাতিল হয়ে যেতে পারত।
            val branches = branchChoices()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(branches.toTypedArray(), BranchFilterStore.indexInChoices(this)) { dialog, which ->
                    pickedBranch = branches[which]
                    rememberBranch(pickedBranch)      // 🟢 V398: সব পর্দার জন্য মনে রাখা
                    binding.branchPicker.text = BranchFilterStore.pillText(this@DoctorQueueActivity)
                    dialog.dismiss()
                    loadQueue(withPhotos = false)
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    private fun loadQueue(withPhotos: Boolean = false, useDelta: Boolean = false) {
        val myLoadToken = ++loadGuardToken
        // 🟢🔒 V398: মাস্টার এখনো ব্রাঞ্চ না-বাছলে **একটাও ক্লাউড-অনুরোধ যাবে না** —
        //   শুধু সহজ বাংলায় বার্তা। ⛔ স্টাফ/ডাক্তারের ক্ষেত্রে এই শর্ত কখনো সত্য হয় না।
        if (BranchFilterStore.notChosen(this, user)) {
            binding.progressLoad.visibility = View.GONE
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.text = BranchFilterStore.ASK_TEXT
            binding.tvEmpty.visibility = View.VISIBLE
            lastPendingItems = emptyList()
            lastDoneItems = emptyList()
            return
        }
        val cached = repository.loadCachedQueue(shownBranch())   // খাতার সারি B42
        val hasCache = !cached.isNullOrEmpty()
        if (hasCache) {
            // Show what we already have right away -- no spinner, no blank screen.
            binding.progressLoad.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
            binding.progressLoad.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            lastPendingItems = cached!!.filter { !it.done }
            lastDoneItems = cached.filter { it.done }
            renderRows()
        } else {
            binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
            // TK-REQUESTED (2026-07-24): only reachable on first-ever open
            // (no cache yet) -- plain "Loading..." instead of blank.
            binding.tvEmpty.text = "Loading..."
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        }
        lifecycleScope.launch {
            val guardAtStart = myLoadToken
            val items = try {
                // 🔴🔒 V454 (20.08.2026, TK-অনুমোদিত পাইলট): শুধু auto-refresh
                // (useDelta=true) পথে নতুন delta-fetch — স্ক্রিন প্রথম খোলা/
                // Resume/ব্রাঞ্চ-বদল সবসময়ই আগের নিরাপদ পূর্ণ fetchQueue()।
                withContext(Dispatchers.IO) {
                    if (useDelta) repository.fetchQueueDelta(shownBranch(), includePhoto = withPhotos)
                    else repository.fetchQueue(shownBranch(), includePhoto = withPhotos)
                }   // খাতার সারি B42 · B632: অটো-রিফ্রেশে ছবি ছাড়া
            } catch (t: Throwable) {
                null
            }
            if (guardAtStart != loadGuardToken) return@launch
            if (items == null) {
                // Fresh fetch failed -- if we already showed cached data, leave it
                // exactly as-is (better a slightly-old list than a blank/error
                // screen). Only show the empty/error state if there was no cache.
                if (!hasCache) {
                    binding.progressLoad.visibility = View.GONE
                    binding.tvEmpty.text = "No Patient In Queue"
                    binding.tvEmpty.visibility = View.VISIBLE
                }
                return@launch
            }
            binding.progressLoad.visibility = View.GONE
            if (items.isEmpty()) {
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.text = "No Patient In Queue"
                binding.tvEmpty.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                lastPendingItems = emptyList()
                lastDoneItems = emptyList()
            } else {
                binding.progressLoad.visibility = View.GONE
                // 🔴🔒 V1312 (TK-রিপোর্ট, ছবিসহ — "Loading..." তালিকার উপরেই আটকে
                //   থাকছিল): তাজা পড়া সারি পেলেও এই "না-খালি" পথে tvEmpty কখনো
                //   GONE করা হত না, তাই cache-ছাড়া প্রথম খোলায় বসানো "Loading..."
                //   পাকাপাকি রয়ে যেত (উপরের cache-পথে ঠিক এই লাইনটাই আছে)।
                binding.tvEmpty.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                // TK-REQUESTED ADDITION (2026-07-20): split into "Today" and
                // "Pending / Overdue" sections instead of one flat list, so a
                // day-old still-pending patient doesn't look mixed in with
                // today's queue. Sort order within each section (newest
                // first) from the repository is preserved -- only grouped.
                /* ✅🔒 V983 (০২.০৯.২০২৬, TK-নির্দেশ) — *"Pending / Overdue
                   থাকবে না… এসেছে তাদের গুলোই থাকবে; যাদের চেকআপ অলরেডি হয়ে
                   গেছে তাদেরকেও এখানে শো করতে হবে… ওভারডিউর বদলে আজকে এখনো
                   বাকি, বা হয়ে গেছে"*।
                   ⇒ ভাগ এখন **রেজিস্ট্রেশনের তারিখ দিয়ে নয়**, চেকআপ হয়েছে
                     কিনা তাই দিয়ে। দুটো ভাগই শুধু আজকের — রাত ১২টায় খালি। */
                lastPendingItems = items.filter { !it.done }
                lastDoneItems = items.filter { it.done }
                renderRows()
            }
        }
    }

    // TK-REQUESTED (2026-07-20, follow-up): "Today" is always shown open.
    // "Pending / Overdue" starts collapsed (header only, arrow ▶) and its
    // patient cards only appear after tapping the header (arrow ▼).
    /** 🔍 V972 — নাম · মোবাইল · রোগীর আইডি, তিনটের যেকোনোটায় মিললেই। */
    /* 🔴 V975 বিল্ড-ফিক্স (০২.০৯.২০২৬, TK-এর Android Studio-র ছবি) — আগে টাইপটা
       object-এর নাম দিয়ে লেখা হয়েছিল, অথচ `QueuePatient` **আলাদা** ক্লাস
       (মডেল-ফাইলের ২৪ নম্বর লাইনে, object-এর বাইরে) ⇒ বিল্ডে
       "Unresolved reference: QueuePatient"। এই ফাইলের বাকি সব জায়গায়
       (৬১ · ৬২ · ৫১২ · ৫৪৩ লাইন) সবসময় শুধু `QueuePatient`-ই লেখা ছিল।
       ⇒ একই ধরনের ভুল আর যেন পার না হয়, তাই পাহারায় নতুন নিয়ম [৯.৪১]। */
    private fun matchesSearch(p: QueuePatient): Boolean {
        if (queueSearch.isBlank()) return true
        val digits = queueSearch.filter { it.isDigit() }
        return p.name.lowercase().contains(queueSearch) ||
            p.patientId.lowercase().contains(queueSearch) ||
            (digits.isNotEmpty() && p.mobile.filter { it.isDigit() }.contains(digits))
    }

    /** 🔍🔒 V1108 — লেখা থামার ৪৫০ মিলিসেকেন্ড পরে একবারই খোঁজা হয়।
     *  ⛔ প্রতিটা অক্ষরে অনুরোধ যায় **না** — টাইপ থামলে তবেই একবার।
     *  ⛔ ৩ অক্ষরের কম · একই লেখা আগে খোঁজা হয়ে থাকলে · অথবা আজকের লাইনেই
     *     রোগী পাওয়া গেলে — কোনো অনুরোধই যায় না। */
    private fun scheduleWideSearch() {
        wideTask?.let { wideHandler.removeCallbacks(it) }
        val q = queueSearch
        if (q.length < 3) {
            if (wideResults.isNotEmpty() || wideForQuery.isNotBlank()) {
                wideResults = emptyList(); wideForQuery = ""; renderRows()
            }
            return
        }
        if (q == wideForQuery) return
        // আজকের লাইনেই পাওয়া গেলে ক্লাউডে যাওয়ার দরকার নেই।
        if (lastPendingItems.any { matchesSearch(it) } || lastDoneItems.any { matchesSearch(it) }) return
        val task = Runnable {
            if (isFinishing || isDestroyed) return@Runnable
            if (queueSearch != q) return@Runnable
            wideSearching = true
            renderRows()
            lifecycleScope.launch {
                val found = withContext(Dispatchers.IO) {
                    try { repository.searchAllPatients(q, shownBranch()) } catch (_: Throwable) { emptyList() }
                }
                wideSearching = false
                if (isFinishing || isDestroyed) return@launch
                if (queueSearch != q) return@launch
                wideForQuery = q
                wideResults = found
                renderRows()
            }
        }
        wideTask = task
        wideHandler.postDelayed(task, 450L)
    }

    /** 🔍🔒 V1107 — টাইপ করা নামটা নিয়ে "সব রোগীর মধ্যে খোঁজা" পর্দা।
     *  ⛔ পর্দাটা নিজের চেনা নিয়মেই খোঁজে — এখানে নতুন কোনো ক্লাউড-পড়া নেই। */
    private fun openAllPatientSearch() {
        val q = binding.etQueueSearch.text?.toString().orEmpty().trim()
        try {
            GlobalSearchActivity.pendingQuery = q
            startActivity(android.content.Intent(this, GlobalSearchActivity::class.java))
        } catch (_: Throwable) { }
    }

    /* 🟢🔒 V1504 — New/Old ছাঁকা। বাছাই না থাকলে (null) সবাই মেলে। */
    private fun matchesBadgeFilter(p: QueuePatient): Boolean {
        val f = queueBadgeFilter ?: return true
        return com.tkbiswas.pilesclinic.clinical.NextVisitPlan.oldOrNew(p.registrationDate) == f
    }

    private fun renderRows() {
        val rows = mutableListOf<QueueRow>()
        // 🔍 V972 — খোঁজার লেখা থাকলে ছেঁকে নেওয়া তালিকাই দেখানো হয়; ফাঁকা
        //    থাকলে সব আগের মতোই। ⛔ জমানো আসল তালিকা ছোঁয়া হয় না।
        val pendingShown = lastPendingItems.filter { matchesSearch(it) && matchesBadgeFilter(it) }
        val doneShown = lastDoneItems.filter { matchesSearch(it) && matchesBadgeFilter(it) }
        if (pendingShown.isNotEmpty()) {
            rows.add(QueueRow.Header("PENDING TODAY (${pendingShown.size})"))
            pendingShown.forEach { rows.add(QueueRow.Item(it)) }
        }
        if (doneShown.isNotEmpty()) {
            /* ✅ V983 — TK: *"DONE TODAY পর্দা ওপেন থাকবে না"* ⇒ গুটানো থাকে,
               শিরোনামে চাপ দিলে খোলে।
               🔍 V972 — খোঁজার সময় গুটানো থাকলে ফল দেখা যেত না, তাই তখন খোলাই থাকে। */
            val open = doneExpanded || queueSearch.isNotBlank()
            val arrow = if (open) "▼" else "▶"
            rows.add(QueueRow.Header("$arrow DONE TODAY (${doneShown.size})", collapsible = true))
            if (open) doneShown.forEach { rows.add(QueueRow.Item(it)) }
        }
        /* 🔍🔒 V1013 (০৩.০৯.২০২৬, TK-রিপোর্ট: *"উপরে সার্চ করলে কোন পেশেন্ট
           সংখ্যা আসে না কেন?"*) — **কারণ:** এই ঘরটা শুধু **আজকের তালিকার
           ভিতরেই** খোঁজে; তালিকার বাইরের রোগী এখানে কোনোদিন আসতেন না, অথচ
           লেখা ছিল শুধু "No patient found" — কেন পাওয়া গেল না তা বোঝাই যেত না।
           ⇒ এখন লেখাটা স্পষ্ট, আর কীবোর্ডের Search চাপলে **সব রোগীর মধ্যে**
             খোঁজার পর্দাটা খুলে যায়।
           ⛔ নতুন কোনো ক্লাউড-পড়া যোগ হয়নি — খোঁজার পর্দা নিজের নিয়মেই চলে। */
        /* 🔍🔒 V1107 (০৫.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — DIPANKAR, Jalpaiguri:
           *"এখানে সার্চ করলে পেসেন্ট আসে না তো"*)।
           🔴 **এটা TK দ্বিতীয়বার বলেছেন — আমার আগের কাজটা (V1013) আধখানা ছিল।**
              তখন শুধু লেখাটা বদলেছিলাম ("press Search on the keyboard"), কিন্তু
              ① ঘরটায় `imeOptions` বসাইনি ⇒ কীবোর্ডে **Search বোতামটাই দেখা যেত না**
              ② চাপলেও টাইপ করা নামটা সঙ্গে যেত না ⇒ নতুন পর্দায় আবার লিখতে হত।
           ⇒ এখন লেখাটাই একটা **নীল বোতাম** — একবার চাপলেই এই নামটা নিয়ে সব
             রোগীর মধ্যে খোঁজার পর্দা খুলে যায়, আর কিছু টাইপ করতে হয় না।
           ⛔ নতুন কোনো ক্লাউড-পড়া **এই পর্দায় যোগ হয়নি** — খোঁজা তখনই হয় যখন
              স্টাফ নিজে বোতামটা চাপেন (Egress অপরিবর্তিত)। */
        /* 🔍🔒 V1108 (TK-নির্দেশ: *"সার্চ বক্সে নাম টাইপ করলেই চলে আসতে হবে"*) —
           আজকের লাইনে না পেলে **নিজে থেকেই** সব রোগীর মধ্যে খোঁজা হয়ে যায়,
           আর ফল এখানেই কার্ড হয়ে বসে — কোনো বোতাম চাপতে হয় না।
           ⛔ কার্ডগুলো হুবহু আগের কার্ড (একই adapter), তাই Check-up · History ·
              Action · Report সব বোতাম আগের মতোই কাজ করে।
           ⛔ ৩ অক্ষরের কম লিখলে খোঁজাই হয় না, তাই অকারণে কিছু দেখায় না। */
        if (queueSearch.isNotBlank() && pendingShown.isEmpty() && doneShown.isEmpty()) {
            val typed = binding.etQueueSearch.text?.toString().orEmpty().trim()
            when {
                queueSearch.length < 3 ->
                    rows.add(QueueRow.Header("NOT IN TODAY'S QUEUE — TYPE 3 LETTERS TO SEARCH ALL"))
                wideSearching ->
                    rows.add(QueueRow.Header("🔍  SEARCHING ALL PATIENTS…"))
                wideResults.isNotEmpty() -> {
                    rows.add(QueueRow.Header("🔍  ALL PATIENTS (${wideResults.size})"))
                    wideResults.forEach { rows.add(QueueRow.Item(it)) }
                }
                wideForQuery == queueSearch ->
                    rows.add(QueueRow.Header("NO PATIENT NAMED \"${typed.uppercase()}\""))
                else ->
                    rows.add(QueueRow.Header("NOT IN TODAY'S QUEUE"))
            }
        }
        // 🔒 V217 (§B216, Master Fix Order §14, item 7 "CHECK-UP থেকে Back
        // দিলে একই জায়গায় ফিরবে"): CHECK-UP থেকে ফিরে এলে `onResume()`
        // সবসময় `loadQueue()` ডেকে তালিকা আবার বানায় (রোগী doctorComplete
        // হয়ে তালিকা থেকে বাদ গেছে কিনা দেখতে — এটা ইচ্ছাকৃত, বদলানো হয়নি)।
        // কিন্তু `notifyDataSetChanged()`-এর পরে স্ক্রল মাঝেমধ্যে উপরে উঠে
        // যেতে পারে (RecyclerView-এর স্বাভাবিক আচরণ)। এখন redraw-এর আগে-পরে
        // scroll position ধরে রাখা হয় — তালিকার কনটেন্ট সত্যিই বদলালে
        // (যেমন কেউ queue থেকে বাদ গেলে) স্বাভাবিকভাবেই একটু নড়বে, কিন্তু
        // শুধু ফিরে এসে একই ডেটা আবার আঁকা হলে জায়গা একই থাকবে।
        val lm = binding.recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
        val firstPos = lm?.findFirstVisibleItemPosition() ?: -1
        val firstOffset = if (firstPos >= 0) {
            lm?.findViewByPosition(firstPos)?.top ?: 0
        } else 0
        adapter.updateItems(rows)
        if (firstPos in 0 until rows.size) {
            lm?.scrollToPositionWithOffset(firstPos, firstOffset)
        }
    }

    // TK-REQUESTED (2026-07-17): Check-up and Summary used to both land on the
    // same Clinical Modules list -- confusing, since both buttons then did
    // the exact same thing. Now each jumps straight to its own module
    // ("CHECKUP" -> Doctor Check-up, "SUMMARY" -> Patient History). Staff is
    // never blocked: RoleSession.canEditClinical() already returns true for
    // every role, so this shortcut works the same for Staff and Doctor.
    // IMPORTANT: still routes through ClinicalModulesActivity (not a direct
    // jump to the sub-screen) so RoleSession.applyFrom() runs first with
    // THIS patient's extras -- skipping that hub would risk the clinical
    // screen showing a stale/previous patient's data.
    // ═══════════════════════════════════════════════════════════════════
    // 🩺🔒 V839 — "গত বারের প্ল্যান" মনে করিয়ে দেওয়ার পপ-আপ
    // TK-নির্দেশ (২৯.০৮.২০২৬, ফটো-প্রুফ দেখিয়ে অনুমোদিত):
    //   *"এটা পরের দিন যখন পেশেন্ট আসবে তখন যেন আগে মনে করিয়ে দেয়।"*
    // ⛔ কোনো বাড়তি ক্লাউড-অনুরোধ নেই — লেখাটা তালিকার সঙ্গেই এসে গেছে।
    // ⛔ পপ-আপ বন্ধ/OK — দুই পথেই চেকআপ পর্দা আগের মতোই খোলে; কখনো আটকায় না।
    // ═══════════════════════════════════════════════════════════════════

    /** দিনে একবারের পাহারা — একই রোগীর জন্য আজ আগে দেখানো হয়েছে কি না। */
    private fun nvpAlreadyShownToday(patientId: String): Boolean {
        return try {
            val sp = getSharedPreferences("nvp_reminder", MODE_PRIVATE)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            val seen = sp.getStringSet("shown_" + today, emptySet()) ?: emptySet()
            if (seen.contains(patientId)) return true
            val updated = HashSet(seen); updated.add(patientId)
            /* পুরনো দিনের চাবি জমতে দেওয়া হয় না — আজকেরটা লিখে বাকিগুলো মোছা। */
            sp.edit().clear().putStringSet("shown_" + today, updated).apply()
            false
        } catch (_: Throwable) { false }
    }

    private fun showNvpReminderThen(p: QueuePatient, go: () -> Unit) {
        val hasPlan = p.nvpLine.isNotBlank() || p.nvpItems.isNotEmpty() ||
            p.nvpMedicine.isNotBlank() || p.nvpNote.isNotBlank()
        val key = p.patientId.ifBlank { p.id }
        if (!hasPlan || key.isBlank() || nvpAlreadyShownToday(key)) { go(); return }

        val badge = com.tkbiswas.pilesclinic.clinical.NextVisitPlan.oldOrNew(p.registrationDate)
        val labels = com.tkbiswas.pilesclinic.clinical.NextVisitPlan.OPTIONS
            .filter { p.nvpItems.contains(it.key) }.map { it.label }
        val body = StringBuilder()
        val whenBy = listOfNotNull(p.nvpWhen.ifBlank { null }, p.nvpBy.ifBlank { null })
            .joinToString(" · ")
        if (whenBy.isNotBlank()) body.append(whenBy).append("\n\n")
        if (labels.isEmpty()) body.append("• ").append(p.nvpLine).append("\n")
        else for (l in labels) body.append("• ").append(l).append("\n")
        if (p.nvpMedicine.isNotBlank()) body.append("\nMedicine: ").append(p.nvpMedicine).append("\n")
        if (p.nvpNote.isNotBlank()) body.append("\n").append(p.nvpNote).append("\n")

        val title = p.name.ifBlank { "Patient" } +
            (if (badge.isNotBlank()) "  ($badge)" else "") + " — Last plan"
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(body.toString().trim())
                .setPositiveButton("OK") { _, _ -> go() }
                .setOnCancelListener { go() }
                .show()
                .also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }
        } catch (_: Throwable) { go() }   // পপ-আপ ব্যর্থ হলেও পর্দা কখনো আটকাবে না
    }

    private fun openClinical(patient: QueuePatient, asDoctor: Boolean, autoOpen: String? = null) {
        // 🔒🔒 খাতার সারি B179 (TK, 30.07.2026 — TK-এর স্পষ্ট অনুমতি: "জায়গাতেও
        // ঠিক করতে চাই")। `QueuePatient`-এ address/age/sex নেই, তাই এখানে
        // **একটা নতুন ছোট, সরু ক্লাউড-কল** — শুধু ওই তিনটে ঘর আনতে। ব্যর্থ
        // হলেও পর্দা খুলবে, শুধু ওই তিনটে ঘর ফাঁকা থাকবে — কিছু ভাঙে না।
        lifecycleScope.launch {
            val (address, age, sex) = withContext(Dispatchers.IO) {
                try { com.tkbiswas.pilesclinic.native.AddressTagRepository.fetchDemographicsCached("+91${patient.mobile.filter { it.isDigit() }.takeLast(10)}", patient.id)   /* 🔵 V531: এক নম্বরে দু'জন হলে ঠিক এই রোগীরই */ }
                catch (_: Throwable) { Triple("", "", "") }
            }
            val intent = Intent(this@DoctorQueueActivity, ClinicalModulesActivity::class.java)
            intent.putExtra(RoleSession.EXTRA_ROLE, if (asDoctor) "DOCTOR" else "STAFF")
            intent.putExtra(RoleSession.EXTRA_PATIENT_NAME, patient.name)
            intent.putExtra(RoleSession.EXTRA_PATIENT_ID, patient.patientId.ifBlank { patient.id })
            // 🔒 খাতার সারি B175: মানুষ-পড়া-যায় Patient ID আলাদা extra দিয়ে —
            // ছাপায় এখন এটাই ব্যবহার হবে। ⛔ উপরের `EXTRA_PATIENT_ID`-এর পুরনো
            // নিয়ম একটুও বদলানো হয়নি।
            intent.putExtra(RoleSession.EXTRA_PATIENT_DISPLAY_ID, patient.patientId)
            intent.putExtra(RoleSession.EXTRA_PATIENT_BRANCH, patient.branch)
            intent.putExtra(RoleSession.EXTRA_PATIENT_MOBILE, patient.mobile.filter { it.isDigit() }.takeLast(10))
            intent.putExtra(RoleSession.EXTRA_PATIENT_DISEASE, patient.disease)
            intent.putExtra(RoleSession.EXTRA_PATIENT_ADDRESS, address)
            intent.putExtra(RoleSession.EXTRA_PATIENT_AGE, age)
            intent.putExtra(RoleSession.EXTRA_PATIENT_SEX, sex)
            if (autoOpen != null) intent.putExtra(ClinicalModulesActivity.EXTRA_AUTO_OPEN, autoOpen)
            startActivity(intent)
        }
    }

    private fun openPrintCenter() {
        startActivity(Intent(this, PrintCenterActivity::class.java))
    }
}
