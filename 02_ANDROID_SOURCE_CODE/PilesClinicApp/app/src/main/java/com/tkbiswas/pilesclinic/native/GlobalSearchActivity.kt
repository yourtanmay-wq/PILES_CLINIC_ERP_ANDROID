package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tkbiswas.pilesclinic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Native rebuild of the WebView global search (searchResults()): searches both
 * enquiries and patients by name (contains) or mobile (last-10 match), with the
 * same visibility rule (master/staff see all; doctor limited to own branch).
 * Tapping a result opens that person's timeline.
 */
class GlobalSearchActivity : AppCompatActivity() {

    /* 🔍🔒 V1107 (০৫.০৯.২০২৬, TK-রিপোর্ট) — অন্য পর্দা থেকে **টাইপ করা নামটা
       সঙ্গে পাঠানো যায়**, তখন এই পর্দা খুলেই নিজে থেকে খুঁজে ফেলে।
       ⛔ কেউ না পাঠালে (মেনু/নিচের বার থেকে খোলা) আচরণ হুবহু আগের মতোই —
          ফাঁকা ঘর, "Type a name or mobile number to search."।
       ⛔ Intent-এর extra ব্যবহার করা হয়নি — এই প্রকল্পের পাহারাদার
          (`verify_kotlin_compile`) androidx চেনে না বলে `intent` লিখলেই
          মিথ্যা "unresolved reference" দেখায় (নিজে চালিয়ে ধরা পড়েছে)।
          তাই `RoleSession`-এর মতোই একটা ছোট স্থির ঘর — **একবার পড়া হলেই
          মুছে যায়**, তাই পরে পর্দাটা আবার খুললে পুরনো লেখা ফিরে আসে না। */
    companion object { @Volatile @JvmStatic var pendingQuery: String = "" }

    private lateinit var progressLoad: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var recycler: RecyclerView
    private val results = mutableListOf<SearchHit>()
    private lateinit var adapter: SearchAdapter
    private lateinit var voiceAnswerHost: android.widget.FrameLayout   // 🎤 V1415

    /* 💊 V985 — মোবাইল → মেডিসিনের বাকি (এই পর্দার নিজের ছোট তালিকা)। */
    private val medDue = HashMap<String, Double>()
    /* 🏷️🔒 V1401 — মোবাইল → (সেকশন-লেবেল, লাল-চিহ্ন)। Follow-up খাতা থেকে ছোট
       একটা batched পড়া (শুধু খোঁজে-ওঠা নম্বরগুলো) — TK মেপে অনুমোদন দিয়েছেন
       ("ফ্রি প্ল্যানে ঝুঁকি বাড়ায় না")। পড়া ব্যর্থ/দেরি হলে কার্ডে নিরাপদ
       লেবেল (ENQUIRY / REGISTERED) থাকে — কখনো ভুল সেকশন দেখায় না। */
    private val stageByMobile = HashMap<String, Pair<String, String>>()
    private var searchJob: Job? = null

    // 🆔 TK-এর নিয়ম (28.07.2026): নাম ও মোবাইলের সঙ্গে Patient ID-ও দেখাতে হবে।
    // এনকোয়ারিতে ID থাকে না, তাই ডিফল্ট ফাঁকা — তখন কিছুই বাড়তি দেখায় না।
    // ⛔ কোনো বাড়তি ক্লাউড-কল হয়নি: patients সারিটা আগে থেকেই নামানো হত।
    /**
     * 🔵🔒 V517 (২২.০৮.২০২৬, TK-অনুমোদিত): `rowId` — এই ফলটা ঠিক **কোন** রোগীর
     * সারি। এক মোবাইলে একাধিক রোগী থাকলে (স্বামী/স্ত্রী) এটা দিয়েই
     * Full Journey সঠিক রোগীরটাই খোলে।
     * ⛔ ডিফল্ট ফাঁকা — Enquiry-র ফলে সারি-আইডি লাগে না, আচরণ আগের মতোই।
     */
    /* 🔵🔒 V538 (২২.০৮.২০২৬, TK-নির্দেশ): এই খোঁজা এমনিতেই `disease` ঘরটা
       আনে (উপরের `patCloud`/`enqCloud`-এর কলাম তালিকা দেখুন), অথচ কার্ডে
       ধরে রাখা হত না — তাই ক্লিনিক্যাল পর্দায় রোগের নাম ফাঁকা যেত।
       ⛔ **নতুন কোনো ক্লাউড-অনুরোধ নয়** — যে তথ্য আগেই আসছে, সেটাই রাখা হলো।
       ⛔ ডিফল্ট ফাঁকা, তাই পুরোনো কোনো ডাক ভাঙে না। */
    /* 🎨🔒 V1401 (১২.০৯.২০২৬ রাত, TK-নির্দেশ, ডেমো-প্রুফ পাশ) — `address` ·
       `altMobile` · `date` কার্ডে দেখানোর জন্য। ⛔ নতুন কোনো ক্লাউড-পড়া নয় —
       তিনটেই উপরের enqCloud/patCloud তালিকায় আগে থেকেই আসত, শুধু ধরে রাখা
       হত না। সব ডিফল্ট ফাঁকা, তাই পুরোনো কোনো ডাক ভাঙে না। */
    data class SearchHit(val name: String, val mobile: String, val branch: String, val type: String, val patientId: String = "", val rowId: String = "", val disease: String = "",
                         val address: String = "", val altMobile: String = "", val date: String = "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_search)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        progressLoad = findViewById(R.id.progressLoad)
        tvEmpty = findViewById(R.id.tvEmpty)
        voiceAnswerHost = findViewById(R.id.voiceAnswerHost)   // 🎤 V1415
        recycler = findViewById(R.id.recyclerView)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SearchAdapter(
            results,
            onFullJourney = { hit -> openTimeline(hit.mobile, hit.rowId) },
            onCall = { hit -> callHit(hit.mobile) },
            onWhatsApp = { hit -> whatsAppHit(hit.mobile) },
            onPayment = { hit -> openPaymentForHit(hit.mobile, hit.rowId, hit.patientId) },
            onPrescription = { hit -> openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.PrescriptionActivity::class.java) },
            onMedicineSlip = { hit -> openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.MedicineSlipActivity::class.java) },
            onBloodTest = { hit -> openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.InvestigationAdviceActivity::class.java) },
            onDietChart = { hit -> openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.DietChartActivity::class.java) },
            onMarkArrived = { hit -> markArrivedHit(hit) },
            onRemark = { hit -> writeRemarkForHit(hit) },   // 📝 V827
            onPrint = { hit -> showPrintPicker(hit) },      // 🖨️ V827
            /* 💊 V985 — বাকির অঙ্ক (একবারই আনা, তাই বারবার নেট-কল হয় না)। */
            dueOf = { mobile -> medDue[mobile.filter { c -> c.isDigit() }.takeLast(10)] ?: 0.0 },
            onCollectDue = { hit -> openMedicineForDue(hit) },
            // 🎨 V1401
            onTakeAction = { hit -> openTimeline(hit.mobile, hit.rowId, autoAction = true) },
            onCallNumber = { number -> callHit(number) },
            stageOf = { hit -> stageByMobile[hit.mobile.filter { c -> c.isDigit() }.takeLast(10)] ?: ("" to "") }
        )
        recycler.adapter = adapter

        val etQuery = findViewById<EditText>(R.id.etQuery)
        etQuery.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable) {
                val q = s.toString().trim()
                searchJob?.cancel()
                if (q.length < 2) { results.clear(); adapter.notifyDataSetChanged(); tvEmpty.visibility = View.VISIBLE; tvEmpty.text = "Type a name or mobile number to search."; voiceAnswerHost.removeAllViews(); voiceAnswerHost.visibility = View.GONE; return }
                /* 🎤🔒 V1415 (আপডেট ১৩.০৯.২০২৬, TK-নির্দেশ: "আপাতত শুধু মাস্টারের
                   জন্য") — প্রশ্নের মতো লেখা হলে (কতজন/কালেকশন/বিক্রি) ভারী
                   নাম-খোঁজার ক্লাউড-পড়া এড়িয়ে সরাসরি রিপোর্ট-উত্তর দেখানো হয়।
                   ⛔ শুধু Master; স্টাফ/ডাক্তারের জন্য এই লেখাটাও সাধারণ
                   নাম/নম্বর খোঁজা হিসেবেই চলে (আচরণ আগের মতোই)।
                   ⛔ ভবিষ্যতে বাকিদের জন্য চালু করতে হলে শুধু এই একটা শর্ত
                   (`isMaster`) সরালেই হবে — বাকি কোড অপরিবর্তিত থাকবে। */
                val isMaster = NativeSession.current(this@GlobalSearchActivity)?.role == "master"
                if (isMaster && VoiceReportModel.isQuestionLike(q)) {
                    results.clear(); adapter.notifyDataSetChanged(); recycler.visibility = View.GONE
                    tvEmpty.visibility = View.GONE
                    showVoiceAnswer(q)
                    return
                }
                recycler.visibility = View.VISIBLE
                voiceAnswerHost.removeAllViews(); voiceAnswerHost.visibility = View.GONE
                searchJob = lifecycleScope.launch {
                    delay(250)
                    runSearch(q)
                }
            }
        })
        /* 🔍 V1107 — পাঠানো নামটা বসিয়ে দিলেই উপরের TextWatcher নিজেই
           খোঁজাটা চালায়; নতুন কোনো আলাদা পথ বানানো হয়নি, তাই ফলাফল ও
           নিয়ম হুবহু হাতে টাইপ করার মতোই। */
        try {
            val passed = pendingQuery.trim()
            pendingQuery = ""
            if (passed.isNotBlank()) {
                etQuery.setText(passed)
                etQuery.setSelection(etQuery.text?.length ?: 0)
            }
        } catch (_: Throwable) { }
    }

    private fun runSearch(q: String) {
        progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        tvEmpty.visibility = View.GONE
        val user = NativeSession.current(this)
        // TK FIX (2026-07-15): typing "+919304173739" (with country code) did not
        // match, only the plain 10-digit "9304173739" did. qDigits kept all 12
        // digits (91 + 10), and a 10-digit mobile suffix can never "contain" a
        // 12-digit query. Now trimmed to the last 10 digits like the mobile
        // itself, so both formats match identically.
        val qDigits = q.filter { it.isDigit() }.takeLast(10)
        lifecycleScope.launch {
            val hits = withContext(Dispatchers.IO) {
                // ONE-NUMBER-ONE-SECTION (web formula): a mobile must appear once,
                // in its CURRENT/most-advanced section. We key by last-10 digits and
                // let a Patient row override an Enquiry row for the same number, so
                // the same number never shows twice across sections/branches.
                val byMobile = LinkedHashMap<String, SearchHit>()
                // 🔒 SPEED FIX (28.07.2026, TK-approved · khata row B26): every
                // search used to drag down EVERY column of up to 2,000 enquiries
                // and 2,000 patients -- doctor's notes, medical history, the
                // full complaint text -- to look at a handful of fields.
                // ⛔ NOTHING ABOUT THE SEARCH CHANGES: same tables, same rows,
                // same limit, same order, and every matching rule below is left
                // word for word. Only columns this screen never reads are no
                // longer sent. The lists are exactly what the code below uses:
                //   enquiries -> id, name, mobile, branch, disease, address, date
                //   patients  -> the same, plus patientId, diagnosis,
                //                registrationDate and bill (bill is what
                //                PatientIdentity.pickPatientRow needs)
                // (the enquiries table has no patientId column at all, so that
                // field was always blank here and is not asked for)
                // 🔒 And if a narrowed read ever fails, fetchListSlim asks for
                // every column again by itself -- so a search can never come
                // back wrongly empty because of this.
                // 🔴🔒 V1347 — ২০০০→৫০০০ (বাকি cross-branch fetch-এর (DuplicateCheck,
                // DoctorVisit, PaymentRepository) সাথে মিলিয়ে) — নাম/রোগ/ঠিকানা
                // দিয়ে খোঁজার সময়ও পুরনো রোগী যেন বাদ না পড়ে যায়।
                val enqSelectCols = "id,name,mobile,branch,disease,address,date,updatedAt"
                val patSelectCols = "id,name,mobile,altMobile,branch,bill,patientId,disease,diagnosis,address,registrationDate,date,updatedAt"
                /* 🔴🔴🔒 V1573 (১৭.০৯.২০২৬, TK-নির্দেশ: "সততার সাথে, সাবধানে,
                   গভীরে যাচাই করে") — TK রিপোর্ট করলেন Search দেরিতে আসছে।
                   **মেপে পাওয়া আসল কারণ:** উপরের দুটো fetchListSlim() আগে
                   সবসময় `filter=null` দিয়ে ডাকা হতো — অর্থাৎ **প্রতিটা
                   টাইপের বিরতিতেই** পুরো enquiries+patients টেবিল (আজ,
                   ১৭.০৯.২০২৬ মেপে: patients 2193 + enquiries 579 = ২৭৭২
                   সারি, সীমা ৫০০০ পর্যন্ত বাড়ে) ফোনে নেমে আসত, তারপর নিচের
                   match()-এ ফোনেই বেছে বার করা হতো।

                   **সমাধান — নতুন কিছু আবিষ্কার নয়, ইতিমধ্যে প্রমাণিত কৌশলের
                   পুনর্ব্যবহার:** `ChamberAttendanceRepository.searchPatients()`
                   -এ TK নিজেই ঠিক এই একই কাজ আগে অনুমোদন করে সার্ভার-সাইড
                   `or=(...ilike...)` ফিল্টারে করিয়েছিলেন (১৬.০৭.২০২৬,
                   লাইভ, মাসের পর মাস প্রমাণিত) — এখানে হুবহু একই
                   URLEncoder-প্যাটার্ন, একই ধরনের ilike-ঘরগুলো ব্যবহার হলো,
                   যাতে match()-এর প্রতিটা শর্ত (নাম/রোগ/ঠিকানা/ঘর/তারিখ/
                   patientId/altMobile) সার্ভারেই মেলানো যায় — নিচের match()
                   ফাংশন **এক অক্ষরও বদলায়নি**, তাই যা মিলত তাই মিলবে, শুধু
                   কম ডেটা নেমে আসবে।

                   ⛔ **নিরাপত্তা (কোনো ভালো কাজ যেন নষ্ট না হয়):** নতুন
                   ফিল্টার-করা পড়া ব্যর্থ হলে (নেট/সার্ভার যে কারণেই হোক,
                   `fetchListOrNull` null ফেরালে) সঙ্গে সঙ্গে **আজকের
                   প্রমাণিত পুরনো পথেই** (পুরো টেবিল, `filter=null`) ফিরে
                   যাওয়া হয় — তাই ফলাফল কখনো আগের চেয়ে খারাপ/কম হতে পারে
                   না, শুধু বেশিরভাগ সময় দ্রুত হবে। মোবাইল-মিল রাজি
                   ঘর-অনুসন্ধান (raw ilike, ডিজিট-বাদ-দেওয়া নয়) — ঠিক V1347-এর
                   নিচের বাড়তি-fetch ও ChamberAttendanceRepository-র মতোই,
                   তাই এই একই সীমা এই প্রকল্পেই আগে থেকে গ্রহণযোগ্য ধরা।
                   ⛔ ওয়েবে এই সমস্যাটাই নেই (তাই ওখানে কিছু বদলানো হয়নি) —
                   ওয়েবের Search সবসময় ফোনে/ব্রাউজারে আগে থেকে জমানো তথ্য
                   (IndexedDB) থেকেই চলে, প্রতি অক্ষরে নতুন করে নেট-কল করে না। */
                val pattern = java.net.URLEncoder.encode("*$q*", "UTF-8")
                var enqOr = "name.ilike.$pattern,disease.ilike.$pattern,address.ilike.$pattern,date.ilike.$pattern"
                var patOr = "name.ilike.$pattern,disease.ilike.$pattern,diagnosis.ilike.$pattern,address.ilike.$pattern,patientId.ilike.$pattern,registrationDate.ilike.$pattern,date.ilike.$pattern"
                if (qDigits.length >= 3) {
                    val mobPattern = java.net.URLEncoder.encode("*$qDigits*", "UTF-8")
                    enqOr += ",mobile.ilike.$mobPattern"
                    patOr += ",mobile.ilike.$mobPattern,altMobile.ilike.$mobPattern"
                }
                val enqCloud = SupabaseClient.fetchListOrNull("enquiries", "or=($enqOr)", 5000, select = enqSelectCols)
                    ?: SupabaseClient.fetchListSlim("enquiries", null, 5000, enqSelectCols)
                val patCloud = SupabaseClient.fetchListOrNull("patients", "or=($patOr)", 5000, select = patSelectCols)
                    ?: SupabaseClient.fetchListSlim("patients", null, 5000, patSelectCols)
                // TK-REQUESTED BUG FIX (2026-07-16): same fix as Follow-up/
                // Doctor Queue/Today's Collection -- a just-created enquiry
                // or just-registered patient could be briefly missing from
                // search results because this always read straight from the
                // cloud, with no awareness of a save still syncing in the
                // background. Any locally-pending row not yet in the cloud
                // result is merged in too; matching/scoring logic below is
                // completely unchanged.
                val enq = org.json.JSONArray()
                for (i in 0 until enqCloud.length()) enq.put(enqCloud.getJSONObject(i))
                val pat = org.json.JSONArray()
                for (i in 0 until patCloud.length()) pat.put(patCloud.getJSONObject(i))
                run {
                    try { LocalWorkflowStore(this@GlobalSearchActivity).markSyncedWhereCloudCaughtUp("enquiries", enq); LocalWorkflowStore(this@GlobalSearchActivity).markSyncedWhereCloudCaughtUp("patients", pat) } catch (_: Throwable) { }   // 🔴 V1311 (তালিকা ৪২৩)
                    val pendingEnq = LocalWorkflowStore(this@GlobalSearchActivity).pendingEnquiries()
                    val seenEnqIds = HashSet<String>()
                    for (i in 0 until enq.length()) seenEnqIds.add(enq.getJSONObject(i).optString("id"))
                    for (i in 0 until pendingEnq.length()) {
                        val row = pendingEnq.getJSONObject(i)
                        val id = row.optString("id")
                        if (id.isNotBlank() && seenEnqIds.add(id)) enq.put(row)
                    }
                    val pendingPat = LocalWorkflowStore(this@GlobalSearchActivity).pendingPatients()
                    val seenPatIds = HashSet<String>()
                    for (i in 0 until pat.length()) seenPatIds.add(pat.getJSONObject(i).optString("id"))
                    for (i in 0 until pendingPat.length()) {
                        val row = pendingPat.getJSONObject(i)
                        val id = row.optString("id")
                        if (id.isNotBlank() && seenPatIds.add(id)) pat.put(row)
                    }
                    /* 🔴🔒 V1347 (১১.০৯.২০২৬, TK-রিপোর্ট — মোবাইল নম্বর দিয়ে খুঁজলে
                       Follow-up-এ মিলছে, Global Search-এ "No match found") — **আসল
                       কারণ কোডে মিলিয়ে পাওয়া:** উপরের ২০০০-সীমার fetchListSlim()
                       সব ব্রাঞ্চের রোগী/এনকোয়ারি একসাথে আনে, সবচেয়ে সম্প্রতি-বদলানো
                       (`updatedAt.desc`) ২০০০টাই — অনেকদিন কোনো কাজ না হওয়া পুরনো
                       রোগী (এই কেসে ১৩ দিন আগে রেজিস্টার, তারপর কোনো নতুন পেমেন্ট/
                       কল/আপডেট নেই) মোট সংখ্যা ২০০০ ছাড়ালে এই তালিকার বাইরে পড়ে
                       যেতে পারে। Follow-up ব্রাঞ্চ-ধরে খোঁজে (একেক ব্রাঞ্চে সীমা
                       ৫০০০, তাই ব্যবহারিকভাবে বাদ পড়ে না) — তাই সেখানে পাওয়া যায়,
                       এখানে যায় না। ⛔ পুরনো ২০০০-সীমার fetch অক্ষত রাখা হলো (নাম/
                       রোগ/ঠিকানা দিয়ে খোঁজায় কিছু বদলায়নি) — শুধু নম্বর দিয়ে খোঁজার
                       সময় সরাসরি ডাটাবেসেই ওই নম্বর ধরে একটা বাড়তি টার্গেটেড কল
                       (রেজাল্ট অল্প, তাই দ্রুত) দিয়ে টেবিল যত বড়ই হোক না কেন
                       নম্বর-মিল কখনো বাদ না পড়া নিশ্চিত করা হলো। */
                    if (qDigits.length >= 3) {
                        try {
                            val extraEnq = SupabaseClient.fetchListSlim(
                                "enquiries", "mobile.like.*$qDigits*", 200,
                                "id,name,mobile,branch,disease,address,date,updatedAt"
                            )
                            for (i in 0 until extraEnq.length()) {
                                val row = extraEnq.getJSONObject(i)
                                val id = row.optString("id")
                                if (id.isNotBlank() && seenEnqIds.add(id)) enq.put(row)
                            }
                            val extraPat = SupabaseClient.fetchListSlim(
                                "patients", "or=(mobile.like.*$qDigits*,altMobile.like.*$qDigits*)", 200,
                                "id,name,mobile,altMobile,branch,bill,patientId,disease,diagnosis,address,registrationDate,date,updatedAt"
                            )
                            for (i in 0 until extraPat.length()) {
                                val row = extraPat.getJSONObject(i)
                                val id = row.optString("id")
                                if (id.isNotBlank() && seenPatIds.add(id)) pat.put(row)
                            }
                        } catch (_: Throwable) { }
                    }
                }
                // TK APPROVED (2026-07-15): Dashboard/Global Search by mobile number
                // shows the same way across ALL branches for every role (Master,
                // Doctor, Staff alike) — this is the one deliberate "see everything"
                // lookup point in the app; branch-scoping only applies to the
                // Follow-up tabs and Payment editing, not here.
                fun canSee(branch: String): Boolean {
                    if (user == null) return false
                    return true
                }
                // TK-REQUESTED ADDITION (2026-07-24): search now also
                // matches Disease, Address, Patient ID, and the record's
                // own date (Enquiry date for an Enquiry row, Registration
                // date for a Patient row) -- was name/mobile only before.
                // Same safe "contains" matching pattern as name, just
                // extended to more fields already present in the same
                // fetched row.
                fun match(name: String, mobile: String, disease: String, address: String, patientId: String, dateText: String, altMobile: String = ""): Boolean {
                    val nameHit = q.length >= 2 && name.contains(q, ignoreCase = true)
                    val mobHit = qDigits.length >= 3 && mobile.filter { it.isDigit() }.takeLast(10).contains(qDigits)
                    // 🔒 V235: Alternate নম্বরেও মেলে (additive — আগের মিল অপরিবর্তিত)।
                    val altMobHit = qDigits.length >= 3 && altMobile.filter { it.isDigit() }.takeLast(10).let { it.isNotBlank() && it.contains(qDigits) }
                    val diseaseHit = q.length >= 2 && disease.contains(q, ignoreCase = true)
                    val addressHit = q.length >= 2 && address.contains(q, ignoreCase = true)
                    val patientIdHit = q.length >= 2 && patientId.contains(q, ignoreCase = true)
                    val dateHit = q.length >= 2 && dateText.contains(q, ignoreCase = true)
                    return nameHit || mobHit || altMobHit || diseaseHit || addressHit || patientIdHit || dateHit
                }
                fun key(mobile: String) = mobile.filter { it.isDigit() }.takeLast(10)
                // Enquiries first (lowest stage) …
                for (i in 0 until enq.length()) {
                    val r = enq.getJSONObject(i)
                    val br = r.s("branch")
                    if (!canSee(br)) continue
                    // 🟢🔒 B633 (11.08.2026, TK-রিপোর্ট ছবিসহ: "ডেমো নম্বর ডিলিটের পরেও Search-এ
                    //   থেকে যায়")। আসল কারণ: Search এতদিন ট্র্যাশ/মুছে-ফেলার চিহ্ন **একটুও মেলাত না**
                    //   (grep=0) — তাই ডিলিট-করা (tombstoned/hidden) রেকর্ডও দেখাত, যদিও Follow-up/
                    //   Reject তালিকায় ঠিকই লুকাত। এখন বাকি পর্দার **হুবহু একই নিয়ম**: DeletedGuard
                    //   (cloud-synced, তাই সব ফোনে একই) বা GhostHide-এ মুছে-ফেলা হলে Search-ও লুকায়।
                    //   ⛔ শুধু ফিল্টার — কোনো রেকর্ড মোছে না, কোনো ডিজাইন/ফ্লো বদলায় না। Restore করলে
                    //   DeletedGuard unmark হয়ে আবার দেখাবে (আগের মতোই)।
                    if (DeletedGuard.isDeleted("enquiries", r.s("id"), this@GlobalSearchActivity) ||
                        DraftRepository.GhostHide.isHidden(r.s("id"), r.s("mobile"))) continue
                    if (!match(r.s("name"), r.s("mobile"), r.s("disease"), r.s("address"), r.s("patientId"), r.s("date"))) continue
                    val k = key(r.s("mobile"))
                    if (k.isNotBlank() && !byMobile.containsKey(k))
                        byMobile[k] = SearchHit(r.s("name"), r.s("mobile"), br, "Enquiry", disease = r.s("disease"),
                            address = r.s("address"), date = r.s("date"))   // 🎨 V1401
                }
                // … then Patients override the same number (higher stage wins).
                // TK-REQUESTED (2026-07-27), "ছ'টা পর্দা এক নিয়মে" step 1 of 6:
                // when the same person has TWO patients rows (a duplicate
                // registration), this loop simply let whichever row the cloud
                // happened to return LAST win -- so the name/branch shown in
                // Search could be the abandoned duplicate, while the payment
                // screen, Patient Details and the Report Card all showed the
                // real one. Those three were already put on one rule in V143
                // (PatientIdentity.pickPatientRow); Search was still on its own.
                // Now it uses that same rule: current branch -> the row that
                // carries a real bill -> the first row. With only one row (the
                // normal case) nothing about this screen changes at all.
                val patRowsByMobile = LinkedHashMap<String, org.json.JSONArray>()
                for (i in 0 until pat.length()) {
                    val r = pat.getJSONObject(i)
                    val br = r.s("branch")
                    if (!canSee(br)) continue
                    // 🟢🔒 B633: patients-এও একই ট্র্যাশ/মুছে-ফেলা ফিল্টার (উপরের enquiries-এর মতোই)।
                    if (DeletedGuard.isDeleted("patients", r.s("id"), this@GlobalSearchActivity) ||
                        DraftRepository.GhostHide.isHidden(r.s("id"), r.s("mobile"))) continue
                    if (!match(r.s("name"), r.s("mobile"), r.s("disease").ifBlank { r.s("diagnosis") }, r.s("address"), r.s("patientId"), r.s("registrationDate").ifBlank { r.s("date") }, altMobile = r.s("altMobile"))) continue
                    val k = key(r.s("mobile"))
                    if (k.isBlank()) continue
                    patRowsByMobile.getOrPut(k) { org.json.JSONArray() }.put(r)
                }
                /* 🔵🔴🔒 V517 (২২.০৮.২০২৬, TK-অনুমোদিত) — **এক নম্বরে একাধিক রোগী
                   হলে প্রত্যেকে আলাদা ফল।**

                   TK-এর দাবি ৮: *"Search-এ একই mobile লিখলে ওই নম্বরের সঙ্গে যুক্ত
                   সব Patient আলাদা আলাদা card হিসেবে দেখাবে।"*

                   ⛔ কিন্তু উপরের `pickPatientRow` নিয়মটা (V143, "ছ'টা পর্দা এক
                      নিয়মে") একটা **ভালো সুরক্ষা**: ভুল করে একই রোগীর দুটো
                      রেজিস্ট্রেশন হয়ে গেলে শুধু আসলটাই দেখায়, পরিত্যক্ত
                      duplicate-টা লুকায়। সেটা এক অক্ষরও ভাঙা যাবে না।

                   **দুটোকে আলাদা করার প্রমাণিত চিহ্ন:** V516-এ স্টাফ যখন নিজে
                   বেছে *"Different Patient — Same Mobile"* চাপেন, একমাত্র তখনই
                   আইডি হয় `pat_<১০ সংখ্যা>_<...>` ধাঁচের
                   (`PatientModel.newRowIdForSameMobile`)। অন্য কোনো পথে এই ধাঁচ
                   কখনো তৈরি হয় না। তাই —
                     · এই ধাঁচের সারি = **স্টাফের ঘোষিত আলাদা রোগী** ⇒ নিজের card
                     · বাকি সব সারি = আগের মতোই একত্র, `pickPatientRow` বেছে দেয়
                   ⇒ পুরোনো ভুল-duplicate গুলো **আগের মতোই লুকানো** থাকে। */
                fun isDeclaredSeparatePatient(rowId: String, mobileKey: String): Boolean {
                    if (mobileKey.length != 10) return false
                    val prefix = "pat_" + mobileKey + "_"
                    return rowId.startsWith(prefix) && rowId.length > prefix.length
                }
                val extraHits = mutableListOf<SearchHit>()
                for ((k, rows) in patRowsByMobile) {
                    val ordinary = org.json.JSONArray()
                    for (i in 0 until rows.length()) {
                        val r = rows.getJSONObject(i)
                        if (isDeclaredSeparatePatient(r.s("id"), k)) {
                            extraHits.add(
                                SearchHit(r.s("name"), r.s("mobile"), r.s("branch"), "Patient", r.s("patientId"), r.s("id"), r.s("disease"),
                                    address = r.s("address"), altMobile = r.s("altMobile"), date = r.s("registrationDate").ifBlank { r.s("date") })   // 🎨 V1401
                            )
                        } else {
                            ordinary.put(r)
                        }
                    }
                    // পুরোনো পথ — হুবহু আগের মতোই (একটাই সারি থাকলে কিছুই বদলায় না)
                    val chosen = PatientIdentity.pickPatientRow(ordinary, user?.branch ?: "") ?: continue
                    byMobile[k] = SearchHit(chosen.s("name"), chosen.s("mobile"), chosen.s("branch"), "Patient", chosen.s("patientId"), chosen.s("id"), chosen.s("disease"),
                        address = chosen.s("address"), altMobile = chosen.s("altMobile"), date = chosen.s("registrationDate").ifBlank { chosen.s("date") })   // 🎨 V1401
                }
                /* ঘোষিত আলাদা রোগীরা মূল ফলের ঠিক পরে বসেন, তাই এক নম্বরের
                   সবাই পাশাপাশি দেখা যায়। ⛔ কেউ কখনো বাদ পড়ে না। */
                val out = byMobile.values.toMutableList()
                out.addAll(extraHits)
                out
            }
            progressLoad.visibility = View.GONE
            results.clear()
            results.addAll(hits)
            adapter.notifyDataSetChanged()
            /* 💊🔒 V985 — মেডিসিনের বাকি: খোঁজার **সব নম্বর একসাথে**, একটাই
               ছোট অনুরোধে (মাত্র ৫টা ঘর)। ⛔ ব্যর্থ হলে চুপচাপ কিছুই দেখায় না —
               কার্ড হুবহু আগের মতোই, কোথাও কিছু আটকায় না। */
            if (hits.isNotEmpty()) lifecycleScope.launch {
                val map = withContext(Dispatchers.IO) {
                    try { MedicineDue.dueByMobile(hits.map { it.mobile }) } catch (_: Throwable) { emptyMap() }
                }
                if (map.isNotEmpty()) {
                    medDue.clear(); medDue.putAll(map)
                    adapter.notifyDataSetChanged()
                }
            }
            /* 🏷️🔒 V1401 — সেকশন (VISIT/PATIENT) ও লাল চিহ্ন (REJECTED/INCOMPLETE):
               Follow-up খাতার একটাই ছোট batched পড়া, উপরের মেডিসিন-বাকির হুবহু
               একই ধরনে। ⛔ ব্যর্থ হলে চুপচাপ — কার্ডে নিরাপদ লেবেলই থাকে। */
            if (hits.isNotEmpty()) lifecycleScope.launch {
                val map = withContext(Dispatchers.IO) {
                    try { fetchStages(hits.map { it.mobile }) } catch (_: Throwable) { emptyMap() }
                }
                if (map.isNotEmpty()) {
                    stageByMobile.clear(); stageByMobile.putAll(map)
                    adapter.notifyDataSetChanged()
                }
            }
            tvEmpty.visibility = if (hits.isEmpty()) View.VISIBLE else View.GONE
            if (hits.isEmpty()) tvEmpty.text = "No match found."
        }
    }

    /**
     * 🏷️🔒 V1401 — খোঁজে-ওঠা নম্বরগুলোর **চলতি** Follow-up সারি থেকে সেকশন ও অবস্থা।
     * · বাছার নিয়ম নিচের `findLiveFollowUpRow`-এর হুবহু একই (স্টেজ-অগ্রাধিকার
     *   Treatment > Patient > Inquiry, সমান হলে সাম্প্রতিক `updatedAt`)।
     * · লেবেল: Follow-up-এর Treatment ⇒ PATIENT · Patient ⇒ VISIT (রেজিস্টার্ড, চিকিৎসা
     *   শুরু হয়নি) · অন্য কিছু ⇒ ফাঁকা (কার্ড নিজের নিরাপদ লেবেল রাখে)।
     * · লাল চিহ্ন: status Cancelled ⇒ REJECTED · Incomplete ⇒ INCOMPLETE
     *   (PatientTimelineActivity-র Reject-পথ ঠিক এই দুটো লেখে)।
     * ⛔ ২৫টা করে ভাগে, প্রতি ভাগে একটাই অনুরোধ (MedicineDue.fetchFor-এর নিয়ম)।
     *    নম্বর "+91…" বা "…" যেভাবেই থাকুক, শেষ ১০ অঙ্ক দিয়ে মেলে (findByMobileOrNull-এর মতো)।
     */
    private fun fetchStages(mobiles: List<String>): Map<String, Pair<String, String>> {
        val wanted = mobiles.map { it.filter { c -> c.isDigit() }.takeLast(10) }.filter { it.length == 10 }.distinct()
        if (wanted.isEmpty()) return emptyMap()
        fun pr(st: String) = when (st) { "Treatment" -> 3; "Patient" -> 2; "Inquiry" -> 1; else -> 0 }
        val best = HashMap<String, JSONObject>()
        for (part in wanted.chunked(25)) {
            val filter = "or=(" + part.joinToString(",") { "mobile.like.*$it" } + ")"
            val rows = try { SupabaseClient.fetchListSlimOrNull("followups", filter, 500, "mobile,stage,status,updatedAt") }
                       catch (_: Throwable) { null } ?: continue
            for (i in 0 until rows.length()) {
                val r = rows.optJSONObject(i) ?: continue
                val k = r.s("mobile").filter { c -> c.isDigit() }.takeLast(10)
                if (k.length != 10) continue
                val cur = best[k]
                if (cur == null || pr(r.s("stage")) > pr(cur.s("stage")) ||
                    (pr(r.s("stage")) == pr(cur.s("stage")) && r.s("updatedAt") > cur.s("updatedAt"))) best[k] = r
            }
        }
        val out = HashMap<String, Pair<String, String>>()
        for ((k, r) in best) {
            val label = when (r.s("stage")) { "Treatment" -> "PATIENT"; "Patient" -> "VISIT"; else -> "" }
            val flag = when (r.s("status").trim().lowercase()) { "cancelled", "rejected" -> "REJECTED"; "incomplete" -> "INCOMPLETE"; else -> "" }
            out[k] = label to flag
        }
        return out
    }

    /* 🎤🔒 V1415 (১৩.০৯.২০২৬, TK-নির্দেশ, ডেমো পাশ) — প্রশ্নের মতো লেখায় এই
       কার্ডটা বসে। চেনা প্যাটার্ন মিললে সংখ্যা দেখায়, চাপ দিলে
       `VoiceReportDetailActivity`-তে (আসল তালিকায়) যায়। না মিললে সৎভাবে
       "বুঝতে পারিনি" — কখনো ভুল সংখ্যা বানানো হয় না। */
    private fun showVoiceAnswer(q: String) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        voiceAnswerHost.removeAllViews()
        val parsed = VoiceReportModel.parse(q)
        if (parsed == null) {
            val box = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FDEEEE")); cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), android.graphics.Color.parseColor("#F5D6D2"))
                }
            }
            box.addView(TextView(this).apply { text = "Not understood"; textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(android.graphics.Color.parseColor("#B42318")) })
            box.addView(TextView(this).apply {
                text = "Try like: “Yesterday how many patients came in Jalpaiguri” or “last 7 days collection in Cooch Behar”"
                textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#7A8699")); setPadding(0, dp(4), 0, 0)
            })
            voiceAnswerHost.addView(box)
            voiceAnswerHost.visibility = View.VISIBLE
            return
        }
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#EAF6EE")); cornerRadius = dp(12).toFloat()
                setStroke(dp(1), android.graphics.Color.parseColor("#CBEBD6"))
            }
        }
        box.addView(TextView(this).apply {
            text = "${parsed.branchLabel} · ${VoiceReportModel.displayPeriod(parsed.from, parsed.to, parsed.periodLabel)}"
            textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#5A6474"))
        })
        val numView = TextView(this).apply {
            text = "…"; textSize = 22f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(android.graphics.Color.parseColor("#0B8A3E"))
        }
        box.addView(numView)
        val subView = TextView(this).apply { textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#5A6474")) }
        box.addView(subView)
        voiceAnswerHost.addView(box)
        voiceAnswerHost.visibility = View.VISIBLE

        val title = "${parsed.branchLabel} — ${VoiceReportModel.displayPeriod(parsed.from, parsed.to, parsed.periodLabel)}"
        // V1420 — নতুন প্রশ্নগুলোর জন্য ছোট্ট সাহায্যকারী (আগেরগুলো যেমন ছিল তেমনই রইল)
        fun openDetail(name: String) {
            box.setOnClickListener {
                startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                    .putExtra("metric", name).putExtra("branch", parsed.branch)
                    .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title)
                    .putExtra("extra", parsed.extra))
            }
        }
        fun show(num: String, sub: String) { numView.text = num; subView.text = "$sub • tap to see list ›" }
        fun showFail(msg: String) { numView.text = "?"; subView.text = msg.ifBlank { "Could not verify" } }
        fun rs(v: Double): String = "₹${"%,.0f".format(v)}"
        when (parsed.metric) {
            VoiceReportModel.Metric.APPOINTMENT_COUNT -> { openDetail("APPOINTMENT_COUNT"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.appointmentCount(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.toString(), "appointments") else showFail(got.message) } }
            VoiceReportModel.Metric.EXPECTED_COUNT -> { openDetail("EXPECTED_COUNT"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.expectedCount(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.toString(), "patients marked expected") else showFail(got.message) } }
            VoiceReportModel.Metric.HANDOVER_PENDING -> { openDetail("HANDOVER_PENDING"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.handoverPendingSummary(parsed.branch) }
                if (got.ok && got.value != null) show(rs(got.value.total), "${got.value.dayCount} days not handed over") else showFail(got.message) } }
            VoiceReportModel.Metric.PAYMENT_REQUESTS -> { openDetail("PAYMENT_REQUESTS"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.paymentRequestsSummary(parsed.branch) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "pending: ${got.value.backdate} backdate · ${got.value.edit} edit · ${got.value.refund} refund") else showFail(got.message) } }
            VoiceReportModel.Metric.REFERRAL_REQUESTS -> { openDetail("REFERRAL_REQUESTS"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.referralRequestsSummary(parsed.branch) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "pending referral requests (${got.value.deleteCount} delete)") else showFail(got.message) } }
            VoiceReportModel.Metric.LEAVE_COUNT -> { openDetail("LEAVE_COUNT"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.leaveSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "leave-days applied: ${got.value.confirmed} confirmed · ${got.value.pending} pending · ${got.value.rejected} rejected") else showFail(got.message) } }
            VoiceReportModel.Metric.DOCTOR_REMINDER -> { openDetail("DOCTOR_REMINDER"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.doctorReminderSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "doctor reminders sent · ${got.value.notAccepted} not accepted yet") else showFail(got.message) } }
            VoiceReportModel.Metric.STAFF_REMINDER_OPEN -> { openDetail("STAFF_REMINDER_OPEN"); lifecycleScope.launch {
                if (parsed.extra.isNotBlank()) {   // 🎤 V1428 — নাম ধরে (আইটেম ৪৮)
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.staffReminderOpenList(parsed.branch) }
                    val rows = got.value?.filter { VoiceReportModel.nameMatch(parsed.extra, it.toName, it.toCode) }
                    if (got.ok && rows != null) show(rows.size.toString(), "open reminders for ${parsed.extra}") else showFail(got.message)
                } else {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.staffReminderOpenSummary(parsed.branch) }
                if (got.ok && got.value != null) show(got.value.toString(), "staff reminders still open") else showFail(got.message) } } }
            VoiceReportModel.Metric.FEE_RETURN -> { openDetail("FEE_RETURN"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.feeReturnSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(rs(got.value.total), "${got.value.patientCount} patients' visit fee returned") else showFail(got.message) } }
            // ── V1421 ──
            VoiceReportModel.Metric.CHAMBER_UNCLOSED -> { openDetail("CHAMBER_UNCLOSED"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.chamberUnclosedSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.toString(), "days chamber not closed (today not counted)") else showFail(got.message) } }
            VoiceReportModel.Metric.NO_SHOW -> { openDetail("NO_SHOW"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.noShowSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.noShow.toString(), "did not come · ${got.value.arrived} came · ${got.value.expectedTotal} were expected") else showFail(got.message) } }
            VoiceReportModel.Metric.OUT_MISSING -> { openDetail("OUT_MISSING"); lifecycleScope.launch {
                if (parsed.extra.isNotBlank()) {   // 🎤 V1428 — নাম/কোড ধরে
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.outMissingList(parsed.branch, parsed.from, parsed.to) }
                    val rows = got.value?.filter { VoiceReportModel.nameMatch(parsed.extra, it.staffCode) }
                    if (got.ok && rows != null) show(rows.size.toString(), "days OUT time not given · ${parsed.extra}") else showFail(got.message)
                } else {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.outMissingSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "days OUT time not given · ${got.value.staffCount} staff") else showFail(got.message) } } }
            VoiceReportModel.Metric.WFH_COUNT -> { openDetail("WFH_COUNT"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.wfhSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "WFH applications: ${got.value.approved} approved · ${got.value.pending} pending · ${got.value.rejected} rejected") else showFail(got.message) } }
            VoiceReportModel.Metric.DUPLICATE_PATIENTS -> { openDetail("DUPLICATE_PATIENTS"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.duplicateSummary(parsed.branch) }
                if (got.ok && got.value != null) show((got.value.mobileGroups + got.value.nameGroups + got.value.paymentGroups).toString(), "${got.value.mobileGroups} same mobile · ${got.value.nameGroups} same name · ${got.value.paymentGroups} same payment") else showFail(got.message) } }
            // 🎤🔒 V1504 (TK-নির্দেশ: "পাহারা ব্যবস্থা রাখতে হবে") — RIMPA ROY/KHAGEN BHAGAT-এর মতো ডুপ্লিকেট পেমেন্ট।
            VoiceReportModel.Metric.DUPLICATE_PAYMENTS -> { openDetail("DUPLICATE_PAYMENTS"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.duplicatePaymentsSummary(parsed.branch) }
                if (got.ok && got.value != null) show(got.value.extraCount.toString(), "extra rows across ${got.value.patientCount} patients") else showFail(got.message) } }
            VoiceReportModel.Metric.FEE_UNPAID -> { openDetail("FEE_UNPAID"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.feeUnpaidSummary(parsed.branch) }
                if (got.ok && got.value != null) show(got.value.toString(), "patients' visit fee not received (registered from 05/09/2026)") else showFail(got.message) } }
            VoiceReportModel.Metric.CALLS_PENDING -> { openDetail("CALLS_PENDING"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.callsPendingSummary(parsed.branch) }
                if (got.ok && got.value != null) show(got.value.toString(), "follow-up calls still pending today") else showFail(got.message) } }
            VoiceReportModel.Metric.MESSAGES_SENT -> { openDetail("MESSAGES_SENT"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.messagesSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "messages opened to send: ${got.value.whatsapp} WhatsApp · ${got.value.sms} SMS") else showFail(got.message) } }
            // ── V1426 — মাস-তুলনা (এই মাস বনাম গত মাসের একই কটা দিন) ──
            VoiceReportModel.Metric.MONTH_COMPARE_PATIENTS, VoiceReportModel.Metric.MONTH_COMPARE_COLLECTION -> {
                val money = parsed.metric == VoiceReportModel.Metric.MONTH_COMPARE_COLLECTION
                val parts = parsed.extra.split("|")
                val lf = parts.getOrElse(0) { "" }; val lt = parts.getOrElse(1) { "" }
                openDetail(if (money) "COLLECTION" else "REGISTRATION_COUNT")
                lifecycleScope.launch {
                    val pair: Pair<Double?, Double?> = withContext(Dispatchers.IO) {
                        if (money) {
                            val a = VoiceReportRepository.collectionSummary(parsed.branch, parsed.from, parsed.to)
                            val b = VoiceReportRepository.collectionSummary(parsed.branch, lf, lt)
                            Pair(a.value?.total, b.value?.total)
                        } else {
                            val a = VoiceReportRepository.patientsRegisteredCount(parsed.branch, parsed.from, parsed.to)
                            val b = VoiceReportRepository.patientsRegisteredCount(parsed.branch, lf, lt)
                            Pair(a.value?.toDouble(), b.value?.toDouble())
                        }
                    }
                    val a = pair.first; val b = pair.second
                    if (a == null || b == null) { showFail("Could not verify"); return@launch }
                    val diff = a - b
                    val f: (Double) -> String = if (money) { v -> rs(v) } else { v -> "%.0f".format(v) }
                    show((if (diff >= 0) "+" else "−") + f(kotlin.math.abs(diff)), "this month ${f(a)} · last month (same days) ${f(b)}" + (if (money) "" else " patients"))
                }
            }
            // ── V1422 ──
            VoiceReportModel.Metric.NEW_PATIENTS -> { openDetail("NEW_PATIENTS"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.newPatientsCount(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.toString(), "registered, treatment not started") else showFail(got.message) } }
            VoiceReportModel.Metric.FOLLOWUP_CALLS_DONE -> { openDetail("FOLLOWUP_CALLS_DONE"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.fuCallsDoneSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "follow-up calls noted (one per patient per day) · ${got.value.patientCount} patients") else showFail(got.message) } }
            VoiceReportModel.Metric.DISEASE_COUNT -> { openDetail("DISEASE_COUNT"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.diseaseCount(parsed.branch, parsed.from, parsed.to, parsed.extra) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "${parsed.extra} patients · of ${got.value.allPatients} registered") else showFail(got.message) } }
            VoiceReportModel.Metric.RMP_CALLED -> { openDetail("RMP_CALLED"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpCalledCount(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.toString(), "RMP doctors called") else showFail(got.message) } }
            VoiceReportModel.Metric.RMP_CALL_DUE -> { openDetail("RMP_CALL_DUE"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpCallDueCount(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.toString(), "RMP doctors due for a call") else showFail(got.message) } }
            VoiceReportModel.Metric.FIELD_VISIT -> { openDetail("FIELD_VISIT"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.fieldVisitSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.visits.toString(), "visits marked · ${"%.1f".format(got.value.km)} km · ${got.value.staffCount} field staff") else showFail(got.message) } }
            // 🎤🔒 V1578 (আইটেম ৩৩) — অসময়ের এনকোয়ারির ইনসেন্টিভ। মাস = রেজিস্ট্রেশনের
            // দিন, ব্রাঞ্চ = রোগীর ব্রাঞ্চ — সার্ভারের reports.incentive_* এতেই বাঁধা।
            VoiceReportModel.Metric.INCENTIVE -> { openDetail("INCENTIVE"); lifecycleScope.launch {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.incentiveSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(rs(got.value.total), "${got.value.entryCount} entries · ${got.value.staffCount} staff") else showFail(got.message) } }
            // 🎤 V1428 (তালিকা ৫৩৮) — কোন ব্রাঞ্চে সবচেয়ে বেশি/কম · RMP-কে দেওয়া কমিশন · IN-বাদ · নাম ধরে হাজিরা/ঘণ্টা
            VoiceReportModel.Metric.BRANCH_TOP_COLLECTION, VoiceReportModel.Metric.BRANCH_TOP_PATIENTS -> {
                val money = parsed.metric == VoiceReportModel.Metric.BRANCH_TOP_COLLECTION
                val lowest = parsed.extra == "min"
                openDetail(if (money) "BRANCH_TOP_COLLECTION" else "BRANCH_TOP_PATIENTS")
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { if (money) VoiceReportRepository.branchRankCollection(parsed.from, parsed.to, lowest) else VoiceReportRepository.branchRankPatients(parsed.from, parsed.to, lowest) }
                    val rows = got.value
                    if (got.ok && rows != null && rows.isNotEmpty()) {
                        fun fmt(r: VoiceReportRepository.BranchRank) = if (money) rs(r.value) else "${r.patients} patients"
                        val rest = rows.drop(1).joinToString(" · ") { "${it.branch} ${fmt(it)}" }
                        show(rows[0].branch, "${if (lowest) "lowest" else "highest"}: ${fmt(rows[0])} · then $rest")
                    } else showFail(got.message)
                }
            }
            VoiceReportModel.Metric.RMP_PAID -> { openDetail("RMP_PAID"); lifecycleScope.launch {
                if (parsed.extra.isNotBlank()) {   // নাম ধরে (আইটেম ১৭) — তালিকা ছেঁকে যোগ
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpPaidList(parsed.branch, parsed.from, parsed.to) }
                    val rows = got.value?.filter { VoiceReportModel.nameMatch(parsed.extra, it.rmpName) }
                    if (got.ok && rows != null) show(rs(rows.sumOf { it.amount }), "paid to ${parsed.extra} · ${rows.size} payments") else showFail(got.message)
                } else {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpPaidSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(rs(got.value.total), "paid to ${got.value.rmpCount} RMPs · ${got.value.paymentCount} payments") else showFail(got.message) } } }
            VoiceReportModel.Metric.IN_MISSING -> { openDetail("IN_MISSING"); lifecycleScope.launch {
                if (parsed.extra.isNotBlank()) {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.inMissingList(parsed.branch, parsed.from, parsed.to) }
                    val rows = got.value?.filter { VoiceReportModel.nameMatch(parsed.extra, it.staffCode, it.staffName) }
                    if (got.ok && rows != null) show(rows.size.toString(), "days IN time not given · ${parsed.extra}") else showFail(got.message)
                } else {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.inMissingSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.total.toString(), "days IN time not given · ${got.value.staffCount} staff") else showFail(got.message) } } }
            VoiceReportModel.Metric.STAFF_HOURS -> { openDetail("STAFF_HOURS"); lifecycleScope.launch {
                if (parsed.extra.isNotBlank()) {   // 🎤 V1428 — নাম/কোড ধরে
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.staffHoursList(parsed.branch, parsed.from, parsed.to) }
                    val rows = got.value?.filter { VoiceReportModel.nameMatch(parsed.extra, it.staffCode) }
                    if (got.ok && rows != null) show("${"%.1f".format(rows.sumOf { it.hours })} h", "hours · ${parsed.extra} · ${rows.sumOf { it.days }} days") else showFail(got.message)
                } else {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.staffHoursSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show("${"%.1f".format(got.value.totalHours)} h", "total hours · ${got.value.staffCount} staff") else showFail(got.message) } } }
            VoiceReportModel.Metric.STAFF_PRESENT -> { openDetail("STAFF_PRESENT"); lifecycleScope.launch {
                if (parsed.extra.isNotBlank()) {   // 🎤 V1428 — নাম/কোড ধরে (আইটেম ১৬)
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.staffPresentList(parsed.branch, parsed.from, parsed.to) }
                    val rows = got.value?.filter { VoiceReportModel.nameMatch(parsed.extra, it.staffCode) }
                    if (got.ok && rows != null) show(rows.size.toString(), "attendance days · ${parsed.extra}") else showFail(got.message)
                } else {
                val got = withContext(Dispatchers.IO) { VoiceReportRepository.staffPresentSummary(parsed.branch, parsed.from, parsed.to) }
                if (got.ok && got.value != null) show(got.value.staffCount.toString(), "staff present · ${got.value.total} attendance days") else showFail(got.message) } } }
            VoiceReportModel.Metric.REGISTRATION_COUNT -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "REGISTRATION_COUNT").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.patientsRegisteredCount(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.toString(); subView.text = "patients registered • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            // 🎤 V1503 (১৫.০৯.২০২৬, TK-নির্দেশ) — "কতজন পেশেন্ট এসেছিল" এখন
            // এই মেট্রিক দেখায় (নতুন রেজিস্ট্রেশন + পুরনো রোগীর ভিজিট-প্রমাণ)।
            VoiceReportModel.Metric.PATIENTS_VISITED -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "PATIENTS_VISITED").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.patientsVisitedCount(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.toString(); subView.text = "patients visited (new + old) • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.COLLECTION -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "COLLECTION").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.collectionSummary(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.total)}"; subView.text = "${got.value.patientCount} patients • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            // 🎤🔒 V1531 (১৮.০৯.২০২৬, TK-নির্দেশ) — "কতজনের কাছ থেকে টাকা এসেছে"।
            // ⛔ নতুন SQL লাগেনি — একই collection_summary RPC, শুধু বড় সংখ্যাটা
            // এখানে patientCount (মানুষ), আর টাকার অঙ্কটা সাবটাইটেলে (আগের
            // COLLECTION-এ যেটা উল্টো ছিল)।
            VoiceReportModel.Metric.PAYING_PATIENTS_COUNT -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "COLLECTION").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.collectionSummary(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.patientCount.toString(); subView.text = "patients paid • ₹${"%,.0f".format(got.value.total)} total • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            // 🎤🔒 V1533 (১৮.০৯.২০২৬, TK-নির্দেশ) — "গত সপ্তাহে/গত মাসে কতজন
            // পেশেন্ট RMP পাঠিয়েছে" — নতুন SQL (00_SQL/V1533...) লাগবে।
            VoiceReportModel.Metric.RMP_REFERRED_COUNT -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "RMP_REFERRED_COUNT").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpReferredCount(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.toString(); subView.text = "patients referred by RMP • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            // 🎤🔒 V1534 (১৮.০৯.২০২৬, TK-নির্দেশ) — "শুধু ডিসকাউন্ট কত হয়েছে" · "কতজন রোগীর ছবি তোলা হয়েছে"
            VoiceReportModel.Metric.DISCOUNT_SUMMARY -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "DISCOUNT_SUMMARY").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.discountSummary(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.total)}"; subView.text = "${got.value.patientCount} patients • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.PHOTO_COUNT -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "PHOTO_COUNT").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.photoCount(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.toString(); subView.text = "patients with photo • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.MEDICINE_SALE, VoiceReportModel.Metric.SALINE_SALE -> {
                val kind = if (parsed.metric == VoiceReportModel.Metric.MEDICINE_SALE) "medicinePayment" else "salinePayment"
                val metricName = if (parsed.metric == VoiceReportModel.Metric.MEDICINE_SALE) "MEDICINE_SALE" else "SALINE_SALE"
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", metricName).putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.productSaleSummary(parsed.branch, parsed.from, parsed.to, kind) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.total)}"; subView.text = "${got.value.saleCount} sales • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.ENQUIRY_COUNT -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "ENQUIRY_COUNT").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.enquiryCount(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.toString(); subView.text = "enquiries • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.REFUND -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "REFUND").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.refundSummary(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.total)}"; subView.text = "${got.value.refundCount} refunds • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.CASH_HANDOVER -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "CASH_HANDOVER").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.cashHandoverSummary(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.total)}"; subView.text = "${got.value.dayCount} days • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.RMP_DUE -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "RMP_DUE").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpDueSummary(parsed.branch) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.totalDue)}"; subView.text = "${got.value.rmpCount} RMP • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.MEDICINE_DUE -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "MEDICINE_DUE").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.productDueSummary(parsed.branch) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.total)}"; subView.text = "${got.value.rowCount} bills with due • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.CALL_COUNT -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "CALL_COUNT").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.callCount(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.toString(); subView.text = "calls from app • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.TRASH_COUNT -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "TRASH_COUNT").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.trashSummary(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = got.value.toString(); subView.text = "records moved to Trash • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
            VoiceReportModel.Metric.RMP_ADVANCE -> {
                box.setOnClickListener {
                    startActivity(Intent(this, VoiceReportDetailActivity::class.java)
                        .putExtra("metric", "RMP_ADVANCE").putExtra("branch", parsed.branch)
                        .putExtra("from", parsed.from).putExtra("to", parsed.to).putExtra("title", title))
                }
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) { VoiceReportRepository.rmpAdvanceSummary(parsed.branch, parsed.from, parsed.to) }
                    if (got.ok && got.value != null) { numView.text = "₹${"%,.0f".format(got.value.total)}"; subView.text = "${got.value.advanceCount} advance payments • tap to see list ›" }
                    else { numView.text = "?"; subView.text = got.message.ifBlank { "Could not verify" } }
                }
            }
        }
    }

    /**
     * 🔵🔒 V517 (TK-অনুমোদিত): মোবাইলের সঙ্গে **কোন রোগী** সেটাও পাঠানো হয়।
     * ⛔ `mobile` extra আগের মতোই যায়, তাই Timeline-এর পুরোনো সব পথ অটুট।
     * ⛔ `patientRowId` ফাঁকা হলে Timeline হুবহু আগের মতোই আচরণ করে।
     */
    private fun openTimeline(mobile: String, patientRowId: String = "", autoAction: Boolean = false) {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        val i = Intent(this, PatientTimelineActivity::class.java).putExtra("mobile", digits)
        if (patientRowId.isNotBlank()) i.putExtra("patientRowId", patientRowId)
        /* ⚡🔒 V1401 (TK-নির্দেশ: "⋮-এর মধ্যে Action বটমে যা যা থাকে তাই") — CHECK-UP
           Queue-র "Action" বোতামের হুবহু একই পথ: Full Journey খুলে তথ্য এলেই
           Take Action তালিকা নিজে থেকে ওঠে। তালিকাটা ওই পর্দাই বানায় (রোগীর
           আসল অবস্থা দেখে), তাই Search-এ ভুল আইটেম দেখানোর কোনো সুযোগ নেই। */
        if (autoAction) i.putExtra("autoAction", true)
        startActivity(i)
    }

    private fun markArrivedHit(hit: SearchHit) {
        val digits = hit.mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            android.widget.Toast.makeText(this, "No valid 10-digit mobile to mark", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val staffMobile = NativeSession.current(this)?.mobile ?: ""
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Mark Arrived?"))
            .setMessage(NoBengali.s("Mark ${hit.name.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?"))
            .setPositiveButton("Yes, Arrived") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            ChamberAttendanceRepository.markArrived(this@GlobalSearchActivity, "+91$digits", hit.name, hit.branch, staffMobile)
                            true
                        } catch (_: Throwable) { false }
                    }
                    android.widget.Toast.makeText(this@GlobalSearchActivity, if (ok) "Marked Arrived ✅" else "Could not mark — please retry", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /* ════════════════════════════════════════════════════════════════════
       📝🔒🔒 V827 (২৯.০৮.২০২৬, TK-নির্দেশ, ছবিসহ)

       *"মনে করুন কিশনগঞ্জের স্টাফ কল রিসিভ করেছিল, কিন্তু কলটা অটোমেটিক
        জলপাইগুড়ির কোনো এনকোয়ারি ছিল — কিশনগঞ্জের স্টাফ কোনো রিমার্ক লিখতে
        পারে না। … আমি চাইছি এখানে নাম্বার সার্চ করলে যে staff কলটা রিসিভ
        করেছে সে যেন রিমার্কটা লিখে দিতে পারে, তাতে জলপাইগুড়ির স্টাফের
        সুবিধা হবে বুঝতে যে লাস্ট কে কথা বলেছিল। … রিমার্কটা ফলোআপ কার্ডে
        যেখানে রিমার্ক লেখা হয় সেখানে অটোমেটিক চলে যেতে হবে।"*

       ⛔ লেখাটা যায় প্রজেক্টের **একটাই প্রমাণিত পথে** — `FollowUpRepository.
          updateRemark()` (Chamber · Dialer · Appointment · Follow-up সবাই
          এটাই ব্যবহার করে)। নতুন কোনো লেখার নিয়ম বানানো হয়নি।
       ⛔ TK-অনুমোদিত **তৃতীয় পথ**: `LAST CALL`-এর তারিখ আজকের হয় ও স্টাফের
          নাম বসে, কিন্তু **কল-গোনা বাড়ে না** — তাই "৫ কলের পর বাতিল"
          নিয়মে এক অক্ষরও প্রভাব পড়ে না।
       ⛔ **নতুন কোনো রেকর্ড তৈরি হয় না।** ওই নম্বরের Follow-up সারি না
          থাকলে পরিষ্কার বার্তা দিয়ে থেমে যায়।
       ⛔ Search পর্দা আগে থেকেই **সব ব্রাঞ্চ** দেখায় (এই অ্যাপের একমাত্র
          ইচ্ছাকৃত "সব দেখা" জায়গা — উপরে `canSee()`-তে লেখা আছে), তাই
          অন্য ব্রাঞ্চের সারিতে লেখাটা এই পর্দার নিজের নিয়মের সাথেই মেলে।
       ⛔ Egress: একটা সরু পড়া (কয়েকটা ঘর) + একটা লেখা। নগণ্য।
       ════════════════════════════════════════════════════════════════════ */
    private fun writeRemarkForHit(hit: SearchHit) {
        val digits = hit.mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            android.widget.Toast.makeText(this, "No valid 10-digit mobile", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val user = NativeSession.current(this) ?: return
        lifecycleScope.launch {
            val row = withContext(Dispatchers.IO) { findLiveFollowUpRow(digits) }
            if (isFinishing || isDestroyed) return@launch
            if (row == null) {
                android.widget.Toast.makeText(
                    this@GlobalSearchActivity,
                    "No follow-up record for this number yet — remark not saved",
                    android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            showRemarkDialog(hit, row, user)
        }
    }

    /**
     * ওই নম্বরের **চলতি** Follow-up সারিটা বার করা।
     * ⛔ বাছার নিয়ম প্রজেক্টের হুবহু একই (V638/V646): আগে স্টেজ-অগ্রাধিকার
     *    (Treatment > Patient > Inquiry), সমান হলে সবচেয়ে সাম্প্রতিক
     *    `updatedAt`. নতুন কোনো নিয়ম বানানো হয়নি।
     * ⛔ `findByMobileOrNull` প্রজেক্টের প্রমাণিত, ৪০+ জায়গায় ব্যবহৃত পথ —
     *    শেষ ১০ অঙ্ক মেলায়, তাই ওয়েব ("9046…") ও ফোন ("+919046…") দুই
     *    ধাঁচের সারিই ধরা পড়ে।
     */
    private fun findLiveFollowUpRow(digits: String): org.json.JSONObject? {
        val arr = SupabaseClient.findByMobileOrNull(
            "followups", "+91$digits",
            "id,mobile,name,branch,stage,status,lastRemark,lastCallDate,updatedAt", 20) ?: return null
        fun pr(st: String) = when (st) {
            "Treatment" -> 3
            "Patient" -> 2
            "Inquiry" -> 1
            else -> 0
        }
        var best: org.json.JSONObject? = null
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            val b = best
            if (b == null) { best = r; continue }
            val pn = pr(r.s("stage"))
            val pb = pr(b.s("stage"))
            if (pn > pb || (pn == pb && r.s("updatedAt") > b.s("updatedAt"))) best = r
        }
        return best
    }

    private fun showRemarkDialog(hit: SearchHit, row: org.json.JSONObject, user: NativeUser) {
        val d = resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(18), px(6), px(18), px(2))
        }
        val who = listOf(
            row.s("name").ifBlank { hit.name }.ifBlank { "UNKNOWN" },
            row.s("branch").ifBlank { hit.branch },
            row.s("stage")
        ).filter { it.isNotBlank() }.joinToString("  ·  ")
        box.addView(android.widget.TextView(this).apply {
            text = who
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
        })
        val old = row.s("lastRemark")
        if (old.isNotBlank()) {
            box.addView(android.widget.TextView(this).apply {
                text = "Last remark: $old"
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#9AA6B4"))
                setPadding(0, px(4), 0, 0)
            })
        }
        val input = android.widget.EditText(this).apply {
            hint = "What did the caller say?"
            setSingleLine(false)
            minLines = 2
            maxLines = 5
            textSize = 14f
            val p = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            p.topMargin = px(8)
            layoutParams = p
        }
        box.addView(input)
        // প্রজেক্টের স্থায়ী নিয়ম (২৪.০৭.২০২৬): ইংরেজি লেখা নিজে থেকে বড় হাতের।
        try { UppercaseInputUtil.applyToAll(box) } catch (_: Throwable) { }

        val id = row.s("id")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Write Remark"))
            .setView(box)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create().also { dlg ->
                dlg.setOnShowListener {
                    dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val text = input.text.toString().trim()
                        /* 🔒 খাতার সারি B54-এর একই পাহারা: ফাঁকা লেখায় আগের
                           রিমার্ক কখনো মুছবে না — তাই এখানেই আটকে দেওয়া হয়। */
                        if (text.isBlank()) {
                            android.widget.Toast.makeText(this, "Please write the remark first", android.widget.Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        dlg.dismiss()
                        val staffName = user.name.ifBlank { user.mobile }
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                try {
                                    FollowUpRepository(this@GlobalSearchActivity)
                                        .updateRemark(id, text, staffName, incrementCall = false, stampCallDate = true)
                                } catch (_: Throwable) { false }
                            }
                            if (isFinishing || isDestroyed) return@launch
                            android.widget.Toast.makeText(
                                this@GlobalSearchActivity,
                                if (ok) "Remark saved to Follow-up" else "Could not save — check connection and try again",
                                android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                dlg.show()
                PremiumAlert.paint(dlg)
            }
    }

    private fun callHit(mobile: String) {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        try {
            // TK-REQUESTED (2026-07-24): "everywhere calling is possible in
            // the project" -- shared CallChooser.kt (Phone/Superfone/etc.
            // picker, Truecaller excluded).
            CallChooser.open(this, digits)
        } catch (e: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(this, "No phone app found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun whatsAppHit(mobile: String) {
        // 🔒 V235 (TK, WhatsApp Chooser project-wide): কেন্দ্রীয় chooser (Personal/Business)।
        WhatsAppMessageChooser.send(this, mobile)
    }

    private fun openPaymentForHit(mobile: String, rowId: String = "", patientCode: String = "") {
        val digits = mobile.filter { it.isDigit() }.takeLast(10)
        /* 🔵🔒 V520 (২২.০৮.২০২৬): এক নম্বরে দুজন আলাদা রোগী থাকলে **এই কার্ডটা
           কার** সেটা সাথে পাঠানো হয়, তাই Payment ঠিক এই রোগীরই ফর্ম খোলে।
           ⛔ ফাঁকা থাকলে আচরণ হুবহু আগের মতোই। */
        startActivity(
            Intent(this, PaymentActivity::class.java)
                .putExtra("mobile", digits)
                .putExtra("patientRowId", rowId)
                .putExtra("patientCode", patientCode)
        )
    }

    /**
     * 🖨️🔒 V827 (২৯.০৮.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — একটাই "Print" বোতাম,
     * ভিতরে সেই চারটেই।
     *
     * ⛔ প্রতিটা সারি ঠিক আগের ফাংশনটাই ডাকে (`openClinicalDoc(...)`) —
     *    কোন পর্দা খুলবে · কী তথ্য যাবে · কে ছাপতে পারবে, কিচ্ছু বদলায়নি।
     * ⛔ ক্রমও আগের মতোই: Prescription → Medicine Slip → Blood Test → Diet Chart।
     */
    /** 💊 V985 — বাকি নেওয়ার জন্য সোজা Medicine পর্দায়, ওই রোগীর নম্বর বসানো। */
    private fun openMedicineForDue(hit: SearchHit) {
        try {
            startActivity(
                Intent(this, MedicinePaymentActivity::class.java)
                    .putExtra("prefill_search", hit.mobile.filter { it.isDigit() }.takeLast(10))
            )
        } catch (_: Throwable) { }
    }

    private fun showPrintPicker(hit: SearchHit) {
        val d = resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, px(4), 0, px(4))
        }
        fun rowItem(icon: String, label: String, open: () -> Unit): View {
            val r = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(px(22), px(15), px(22), px(15))
                isClickable = true
                isFocusable = true
            }
            r.addView(TextView(this).apply {
                text = icon
                textSize = 17f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT).also { it.marginEnd = px(14) }
            })
            r.addView(TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#101828"))
            })
            r.setOnClickListener { open() }
            return r
        }
        lateinit var dlg: androidx.appcompat.app.AlertDialog
        col.addView(rowItem("📝", "Prescription") { dlg.dismiss(); openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.PrescriptionActivity::class.java) })
        col.addView(rowItem("💊", "Medicine Slip") { dlg.dismiss(); openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.MedicineSlipActivity::class.java) })
        col.addView(rowItem("🩸", "Blood Test") { dlg.dismiss(); openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.InvestigationAdviceActivity::class.java) })
        col.addView(rowItem("🥗", "Diet Chart") { dlg.dismiss(); openClinicalDoc(hit, com.tkbiswas.pilesclinic.clinical.DietChartActivity::class.java) })
        dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Print"))
            .setView(col)
            .setNegativeButton("Cancel", null)
            .create()
        dlg.show()
        PremiumAlert.paint(dlg)
    }

    // TK APPROVED (2026-07-15): Search result card redesigned -- the four
    // clinical documents now each have their own direct one-tap button
    // (instead of hiding behind a "Docs" picker dialog), same destination
    // screens as before (PrescriptionActivity / MedicineSlipActivity /
    // InvestigationAdviceActivity / DietChartActivity), same RoleSession
    // setup as the old openClinicalForHit.
    private fun openClinicalDoc(hit: SearchHit, target: Class<*>) {
        val user = NativeSession.current(this) ?: return
        val roleStr = if (user.role.equals("doctor", true)) "DOCTOR" else "STAFF"
        val digits = hit.mobile.filter { it.isDigit() }.takeLast(10)
        // 🔒🔒 খাতার সারি B179 (TK, 30.07.2026 — TK-এর স্পষ্ট অনুমতি: "জায়গাতেও
        // ঠিক করতে চাই")। `SearchHit`-এ address/age/sex নেই, তাই এখানে **একটা
        // নতুন ছোট, সরু (slim) ক্লাউড-কল** — শুধু ওই তিনটে ঘর আনতে। ব্যর্থ
        // হলেও (অফলাইন ইত্যাদি) পর্দা খুলবে, শুধু ওই তিনটে ঘর ফাঁকা থাকবে —
        // আগের মতোই, কিছু ভাঙে না।
        lifecycleScope.launch {
            val (address, age, sex) = withContext(Dispatchers.IO) {
                try { com.tkbiswas.pilesclinic.native.AddressTagRepository.fetchDemographics("+91$digits", hit.rowId)   /* 🔵 V531 */ }
                catch (_: Throwable) { Triple("", "", "") }
            }
            com.tkbiswas.pilesclinic.clinical.RoleSession.applyFrom(
                roleStr, hit.name, digits, hit.branch, digits, address, age, sex, hit.disease,   // 🔵 V538
                // 🔒 খাতার সারি B175: `hit.patientId` (মানুষ-পড়া-যায় কোড) এমনিতেই
                // এই খোঁজার ফলাফলে আছে (SearchHit-এর নিজের ঘর), শুধু পাঠানো হত না।
                patientDisplayId = hit.patientId
            )
            startActivity(Intent(this@GlobalSearchActivity, target))
        }
    }

    /* 🎨🔒 V1401 (১২.০৯.২০২৬ রাত, TK-নির্দেশ *"ডিজাইন চেঞ্জ করুন… কোন প্রকার
       ঝুঁকি নেবেন না"*, ডেমো-প্রুফ ধাপে ধাপে পাশ — সাধারণ ও সরু ফোন দুটোতেই)।
       নতুন কার্ড: সাদা, বাঁয়ে সবুজ দাগ · নাম (২ লাইন পর্যন্ত) + ⋮ · ব্রাঞ্চ · সেকশন-চিপ
       (ENQUIRY / VISIT / PATIENT / REGISTERED) · রোগ-চিপ · তারিখ · লাল REJECTED/
       INCOMPLETE · 📞 নম্বর (এক চাপে কল, লং-প্রেসে কপি; দ্বিতীয় নম্বর থাকলে
       পাশাপাশি, নইলে পাশে Patient ID) · 📍 ঠিকানা (এক লাইন, লং-প্রেসে কপি) ·
       Payment / Full Journey / Mark Arrived এক সারিতে · মেডিসিন-বাকি এক লাইনে।
       ⋮ = Call · WhatsApp · Print · ⚡ Take Action।
       ⛔ প্রতিটা বোতাম/মেনুর কাজ আগের সেই একই ফাংশনই ডাকে — কেবল চেহারা ও
          বসার জায়গা বদলেছে; নতুন ক্লাউড-পড়া শুধু `fetchStages` (TK-অনুমোদিত)। */
    private class SearchAdapter(
        val items: List<SearchHit>,
        val onFullJourney: (SearchHit) -> Unit,
        val onCall: (SearchHit) -> Unit,
        val onWhatsApp: (SearchHit) -> Unit,
        val onPayment: (SearchHit) -> Unit,
        val onPrescription: (SearchHit) -> Unit,
        val onMedicineSlip: (SearchHit) -> Unit,
        val onBloodTest: (SearchHit) -> Unit,
        val onDietChart: (SearchHit) -> Unit,
        // TK-REQUESTED (2026-07-20): mark a searched patient Arrived into
        // today's Chamber Attendance directly from Search.
        val onMarkArrived: (SearchHit) -> Unit,
        /* 📝🔒 V827 (২৯.০৮.২০২৬, TK-নির্দেশ) — অন্য ব্রাঞ্চের কল ধরা স্টাফও
           যেন এখান থেকে রিমার্ক লিখতে পারেন। */
        val onRemark: (SearchHit) -> Unit,
        /* 🖨️🔒 V827 — চারটে ছাপার পর্দা এখন একটাই "Print" বোতামের ভিতরে। */
        val onPrint: (SearchHit) -> Unit,
        /* 💊🔒 V985 (TK-নির্দেশ: *"মেডিসিন বা স্যালাইনের টাকা বাকি থাকলে তো
           দেখার কোনো উপায় নেই"*) — মোবাইল ধরে বাকির অঙ্ক; ফাঁকা থাকলে
           কার্ড হুবহু আগের মতোই দেখায়। */
        val dueOf: (String) -> Double,
        val onCollectDue: (SearchHit) -> Unit,
        /* ⚡ V1401 — ⋮ → Take Action (Full Journey + নিজে-থেকে-ওঠা Action তালিকা)। */
        val onTakeAction: (SearchHit) -> Unit,
        /* 📞 V1401 — নম্বরে এক চাপে কল (মূল বা Alt, যেটায় চাপা হলো)। */
        val onCallNumber: (String) -> Unit,
        /* 🏷️ V1401 — (সেকশন-লেবেল, লাল-চিহ্ন); ফাঁকা হলে কার্ড নিরাপদ লেবেল বসায়। */
        val stageOf: (SearchHit) -> Pair<String, String>
    ) : RecyclerView.Adapter<SearchAdapter.VH>() {
        class VH(
            val root: LinearLayout,
            val dots: TextView,
            val tvName: TextView,
            val tvChips: TextView,
            val tvMob1: TextView,
            val tvSep: TextView,
            val tvSecond: TextView,
            val tvPid: TextView,
            val tvAddr: TextView,
            val btnRow: LinearLayout,
            val tvDue: TextView
        ) : RecyclerView.ViewHolder(root)

        /** গোল-কোণা চিপ — একটা TextView-এর ভিতরেই বসে, তাই সরু ফোনে না ধরলে
         *  পরের লাইনে নেমে যায়, কিছু ভাঙে না বা গায়ে লাগে না (ডেমোতে মাপা)। */
        private class ChipSpan(private val bg: Int, private val fg: Int, private val padH: Float, private val padV: Float, private val radius: Float) : android.text.style.ReplacementSpan() {
            private fun paintFor(base: android.graphics.Paint) = android.graphics.Paint(base).apply { isFakeBoldText = true; isAntiAlias = true }
            override fun getSize(paint: android.graphics.Paint, text: CharSequence, start: Int, end: Int, fm: android.graphics.Paint.FontMetricsInt?): Int =
                (paintFor(paint).measureText(text, start, end) + padH * 2).toInt()
            override fun draw(canvas: android.graphics.Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: android.graphics.Paint) {
                val p = paintFor(paint)
                val w = p.measureText(text, start, end)
                val fm = p.fontMetrics
                val rect = android.graphics.RectF(x, y + fm.ascent - padV, x + w + padH * 2, y + fm.descent + padV)
                p.color = bg; canvas.drawRoundRect(rect, radius, radius, p)
                p.color = fg; canvas.drawText(text, start, end, x + padH, y.toFloat(), p)
            }
        }

        private fun c(hex: String) = android.graphics.Color.parseColor(hex)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val dens = ctx.resources.displayMetrics.density
            fun dp(v: Int) = (v * dens).toInt()

            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(8), dp(6), dp(8), dp(6))
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(14).toFloat()
                    setColor(android.graphics.Color.WHITE)
                }
                elevation = 3f * dens
                clipToOutline = true
                isClickable = true; isFocusable = true
            }
            // বাঁয়ের সবুজ দাগ
            root.addView(View(ctx).apply {
                setBackgroundColor(c("#0EA25F"))
                layoutParams = LinearLayout.LayoutParams(dp(5), ViewGroup.LayoutParams.MATCH_PARENT)
            })
            val col = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(11), dp(11), dp(11), dp(11))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            root.addView(col)

            val topRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.TOP }
            val tvName = TextView(ctx).apply {
                textSize = 15.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(c("#0B2B59"))
                maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also { it.marginEnd = dp(8) }
            }
            val dots = TextView(ctx).apply {
                text = "⋮"; textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(c("#0B2B59"))
                gravity = android.view.Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(9).toFloat()
                    setColor(c("#EEF2F7"))
                }
                isClickable = true; isFocusable = true
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            }
            topRow.addView(tvName); topRow.addView(dots)
            col.addView(topRow)

            val tvChips = TextView(ctx).apply {
                textSize = 11f
                setTextColor(c("#5B6B7C"))
                setLineSpacing(dp(7).toFloat(), 1f)
                setPadding(0, dp(5), 0, dp(1))
            }
            col.addView(tvChips)

            val mobRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(5), 0, 0)
            }
            val tvMob1 = TextView(ctx).apply {
                textSize = 12.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(c("#0B2B59")); maxLines = 1
                isClickable = true; isFocusable = true
            }
            val tvSep = TextView(ctx).apply { text = "  ·  "; textSize = 12f; setTextColor(c("#8B98A9")); maxLines = 1 }
            val tvSecond = TextView(ctx).apply {
                textSize = 12.5f; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            mobRow.addView(tvMob1); mobRow.addView(tvSep); mobRow.addView(tvSecond)
            col.addView(mobRow)

            val tvPid = TextView(ctx).apply {
                textSize = 11.5f; setTextColor(c("#5B6B7C")); maxLines = 1
                setPadding(0, dp(2), 0, 0)
            }
            col.addView(tvPid)

            val tvAddr = TextView(ctx).apply {
                textSize = 11.5f; setTextColor(c("#6B7A8C"))
                maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(3), 0, 0)
                isClickable = true; isFocusable = true
            }
            col.addView(tvAddr)

            val btnRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                val p = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                p.topMargin = dp(9); layoutParams = p
            }
            col.addView(btnRow)

            val tvDue = TextView(ctx).apply {
                textSize = 11.5f; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(7), 0, 0)
            }
            col.addView(tvDue)

            return VH(root, dots, tvName, tvChips, tvMob1, tvSep, tvSecond, tvPid, tvAddr, btnRow, tvDue)
        }

        override fun getItemCount() = items.size

        /** তারিখ সবসময় dd/MM/yyyy (TK-র স্থায়ী নিয়ম, খাতার সারি B76); ফাঁকা হলে ফাঁকা। */
        private fun dmy(raw: String): String {
            val t = raw.trim().take(10)
            if (t.isBlank()) return ""
            if (t.length == 10 && t[4] == '-' && t[7] == '-') return t.substring(8, 10) + "/" + t.substring(5, 7) + "/" + t.substring(0, 4)
            return t
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val h = items[position]
            val ctx = holder.root.context
            val dens = ctx.resources.displayMetrics.density
            fun dp(v: Int) = (v * dens).toInt()
            val digits = h.mobile.filter { it.isDigit() }.takeLast(10)
            val alt = h.altMobile.filter { it.isDigit() }.takeLast(10).takeIf { it.length == 10 && it != digits } ?: ""

            // নাম — চাপলে Full Journey (আগের হেডার-চাপের নিয়ম), লং-প্রেসে কপি
            holder.tvName.text = h.name.ifBlank { "(no name)" }
            holder.tvName.setOnClickListener { onFullJourney(h) }
            holder.tvName.copyOnLongPress("Name", h.name)
            holder.root.setOnClickListener { onFullJourney(h) }

            // ব্রাঞ্চ · সেকশন · রোগ · তারিখ · লাল চিহ্ন — একটাই লেখায়, সরু ফোনে নিজে থেকে পরের লাইনে
            val (stageLabel, flag) = stageOf(h)
            val label = when {
                h.type == "Enquiry" -> "ENQUIRY"
                stageLabel.isNotBlank() -> stageLabel
                else -> "REGISTERED"
            }
            val sb = android.text.SpannableStringBuilder()
            fun chip(text: String, bg: String, fg: String) {
                if (text.isBlank()) return
                if (sb.isNotEmpty()) sb.append("  ")
                val st = sb.length; sb.append(text)
                sb.setSpan(ChipSpan(c(bg), c(fg), dp(7).toFloat(), dp(3).toFloat(), dp(10).toFloat()), st, sb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (h.branch.isNotBlank()) {
                sb.append(h.branch)
                sb.setSpan(android.text.style.ForegroundColorSpan(c("#0A5428")), 0, sb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, sb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            chip(label, "#FBE9B7", "#7A5200")
            chip(h.disease.trim().uppercase(), "#E8F5EE", "#0A5428")
            val d = dmy(h.date)
            if (d.isNotBlank()) { if (sb.isNotEmpty()) sb.append("  "); sb.append(d) }
            chip(flag, "#B42318", "#FFFFFF")
            holder.tvChips.text = sb
            holder.tvChips.visibility = if (sb.isEmpty()) View.GONE else View.VISIBLE

            // 📞 নম্বর — এক চাপে কল, লং-প্রেসে কপি
            holder.tvMob1.text = "📞 " + digits.ifBlank { h.mobile }
            holder.tvMob1.setOnClickListener { if (digits.length == 10) onCallNumber(digits) else onCall(h) }
            holder.tvMob1.copyOnLongPress("Mobile number", digits.ifBlank { h.mobile })
            if (alt.isNotBlank()) {
                // দুটো নম্বর পাশাপাশি; Patient ID নিচের লাইনে
                holder.tvSecond.text = "📞 Alt $alt"
                holder.tvSecond.setTypeface(holder.tvSecond.typeface, android.graphics.Typeface.BOLD)
                holder.tvSecond.setTextColor(c("#1D6FE0"))
                holder.tvSecond.isClickable = true
                holder.tvSecond.setOnClickListener { onCallNumber(alt) }
                holder.tvSecond.copyOnLongPress("Mobile number", alt)
                holder.tvSep.visibility = View.VISIBLE; holder.tvSecond.visibility = View.VISIBLE
                holder.tvPid.text = h.patientId
                holder.tvPid.visibility = if (h.patientId.isBlank()) View.GONE else View.VISIBLE
            } else {
                // একটা নম্বর — পাশেই Patient ID
                holder.tvSecond.text = h.patientId
                holder.tvSecond.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                holder.tvSecond.setTextColor(c("#5B6B7C"))
                holder.tvSecond.isClickable = false
                holder.tvSecond.setOnClickListener(null)
                holder.tvSecond.setOnLongClickListener(null)
                val show = h.patientId.isNotBlank()
                holder.tvSep.visibility = if (show) View.VISIBLE else View.GONE
                holder.tvSecond.visibility = if (show) View.VISIBLE else View.GONE
                holder.tvPid.visibility = View.GONE
            }

            // 📍 ঠিকানা — এক লাইন, লং-প্রেসে কপি, চাপলে Full Journey
            val addr = h.address.trim()
            holder.tvAddr.text = "📍 $addr"
            holder.tvAddr.visibility = if (addr.isBlank()) View.GONE else View.VISIBLE
            holder.tvAddr.setOnClickListener { onFullJourney(h) }
            holder.tvAddr.copyOnLongPress("Address", addr)

            // ⋮ — Call · WhatsApp · Print · ⚡ Take Action
            holder.dots.setOnClickListener { v ->
                val menu = listOf(
                    Triple("📞", "Call") { onCall(h) },
                    Triple("💬", "WhatsApp") { onWhatsApp(h) },
                    Triple("🖨️", "Print") { onPrint(h) },
                    Triple("⚡", "Take Action") { onTakeAction(h) }
                )
                try {
                    val pm = android.widget.PopupMenu(ctx, v)
                    menu.forEachIndexed { i, (icon, text, _) -> pm.menu.add(0, i, i, "$icon  $text") }
                    pm.setOnMenuItemClickListener { mi -> menu.getOrNull(mi.itemId)?.third?.invoke(); true }
                    pm.show()
                } catch (_: Throwable) { }
            }

            // বোতাম — একই কাজ, একই রং (V1322), এখন তিনটে এক সারিতে
            // 🔴🔒 V1614 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ) — REJECTED লেখা কার্ডেও
            // Payment ও Mark Arrived আগে আগের মতোই সচল থাকত (Reject শুধু লেবেল
            // বদলাত, অন্য কোনো পর্দাই সেটা যাচাই করত না)। ফলে রিজেক্ট করার পরও
            // টাকা জমা ও Arrived দুটোই হয়ে যেত। এখন `flag == "REJECTED"` হলে এই
            // দুটো বোতাম ধূসর ও নিষ্ক্রিয় — Draft-এর Reject List থেকে Restore
            // না করা পর্যন্ত। Full Journey (শুধু দেখা, টাকা/অবস্থা বদলায় না)
            // আগের মতোই সবসময় সচল রাখা হলো।
            fun actionButton(icon: String, text: String, fillColors: IntArray, enabled: Boolean = true, action: () -> Unit): LinearLayout {
                return LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    // ৩৬০dp-র সরু ফোনেও "Mark Arrived" যেন না কাটে — মেপে: প্রতিটা বোতাম ≈১০১dp,
                    // আইকন ১৫ + ফাঁক ৪ + লেখা (৯.৫sp bold ≈ ৬৬dp) + প্যাডিং ৬ = ৯১dp < ১০১dp।
                    setPadding(dp(3), dp(9), dp(3), dp(9))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(11).toFloat()
                        orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                        colors = if (enabled) fillColors else intArrayOf(c("#B8C0CC"), c("#9AA5B1"))
                    }
                    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    lp.marginEnd = dp(2); lp.marginStart = dp(2)
                    layoutParams = lp
                    isClickable = true; isFocusable = true
                    setOnClickListener {
                        if (enabled) action()
                        else android.widget.Toast.makeText(ctx, NoBengali.s("Rejected — restore from Draft's Reject List first"), android.widget.Toast.LENGTH_LONG).show()
                    }
                    addView(TextView(ctx).apply {
                        this.text = icon; textSize = 12f
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.marginEnd = dp(4) }
                    })
                    addView(TextView(ctx).apply {
                        this.text = text; textSize = 9.5f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.WHITE)
                        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                }
            }
            val rejected = flag == "REJECTED"
            holder.btnRow.removeAllViews()
            holder.btnRow.addView(actionButton("💳", "Payment", intArrayOf(c("#1D6FE0"), c("#1457B8")), enabled = !rejected) { onPayment(h) })
            holder.btnRow.addView(actionButton("🧭", "Full Journey", intArrayOf(c("#8A63E8"), c("#6A3FCB"))) { onFullJourney(h) })
            holder.btnRow.addView(actionButton("🏥", "Mark Arrived", intArrayOf(c("#D98A2B"), c("#B45309")), enabled = !rejected) { onMarkArrived(h) })

            // 💊 মেডিসিনের বাকি — এক লাইন (V985-এর হিসাব অপরিবর্তিত)
            val due = dueOf(h.mobile)
            if (due > 0.0) {
                holder.tvDue.text = "💊 Med. Due ₹" + "%,.0f".format(due) + " — tap to collect"
                holder.tvDue.setTextColor(c("#B42318"))
                holder.tvDue.setTypeface(holder.tvDue.typeface, android.graphics.Typeface.BOLD)
                holder.tvDue.isClickable = true
                holder.tvDue.setOnClickListener { onCollectDue(h) }
            } else {
                holder.tvDue.text = "💊 No medicine due"
                holder.tvDue.setTextColor(c("#8B98A9"))
                holder.tvDue.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                holder.tvDue.isClickable = false
                holder.tvDue.setOnClickListener(null)
            }
        }
    }
}
