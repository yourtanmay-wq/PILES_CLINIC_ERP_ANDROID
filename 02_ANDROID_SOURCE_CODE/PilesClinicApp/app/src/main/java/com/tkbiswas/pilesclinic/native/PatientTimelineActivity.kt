package com.tkbiswas.pilesclinic.native

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tkbiswas.pilesclinic.databinding.ActivityPatientTimelineBinding
import com.tkbiswas.pilesclinic.clinical.checkupRecordFromJsonStringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View-All: a premium patient header plus the full update timeline (newest
 * first), matching the WebView's viewFollow(). Launched with a 10-digit mobile.
 */
class PatientTimelineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientTimelineBinding
    private lateinit var adapter: TimelineAdapter
    // 🔒🔒 V1381 (১২.০৯.২০২৬, TK-রিপোর্ট — ANAND KUMAR-এর ₹3,250 Due দুইবার
    // সেভ হয়ে গিয়েছিল, কোনো ওয়ার্নিং ছাড়াই) — "Add Referral Income"-এর
    // Save বোতামে PaymentActivity-র refundSaving-এর হুবহু একই ডাবল-ট্যাপ লক।
    private var referralSaving = false
    private var currentMobile: String = ""
    // TK-CLARIFIED (2026-07-20): Report Card only makes sense once this
    // patient has a real bill (created via Advance Payment / Registration).
    // Enquiry and Visit stage patients have no bill yet, so the option must
    // stay hidden until billTotal > 0.
    private var currentBillTotal: Double = 0.0
    // 🏷️ TK-APPROVED (03.09.2026): discount already forgiven on this bill,
    // so the Take Action dialog can show what was given before.
    private var currentDiscount: Double = 0.0
    // TK-APPROVED (2026-07-20): when opened from the CHECK-UP Queue "Action"
    // button, auto-open the Take Action menu once the patient data has loaded.
    private var autoActionPending = false
    // 🔒 V1527 (18.09.2026): cache-first Timeline can paint old visible totals
    // before the fresh cloud read finishes. Take Action must NEVER build from
    // that half-loaded state, otherwise the first tap and second tap can show
    // different options. These two flags make the first tap wait for the same
    // verified live state that later taps use.
    private var actionStateReady = false
    private var actionOpenPending = false
    private var currentSection: String? = null
    // TK-REQUESTED (2026-07-24): true only when this screen was opened via
    // the "Full Journey" button -- controls whether buildEnquiryHistoryTable
    // shows the complete A-to-Z history or the current-stage-only view.
    private var forceFullJourney: Boolean = false
    private var currentPatientRowId: String = ""

    /** 🔵 V517: Search/অন্য পর্দা থেকে আসা "ঠিক কোন রোগী" — ফাঁকা হলে আগের আচরণ। */
    private var preferPatientRowId: String = ""
    private var currentPatientName: String = ""
    private var currentRefDoctor: String = ""
    private var currentRefDoctorMobile: String = ""
    private var currentFollowupId: String = ""
    private var currentFollowupStage: String = ""
    /** 🔒 খাতার সারি B97: রেকর্ডটা এখন Reject/Incomplete অবস্থায় আছে কি না। */
    private var currentFollowupStatus = ""
    /** 🔒 খাতার সারি B98: ডিলিটের অনুরোধে রোগীর পড়ার আইডিটাও পাঠানো হয়,
     *  যাতে মাস্টার এক নজরেই চিনতে পারেন। ফাঁকা থাকলে কিছু ভাঙে না। */
    private var currentPatientCode: String = ""
    private var currentEnquiryId: String = ""
    private var currentBranch: String = ""
    private var currentDisease: String = ""
    // 🔒 খাতার সারি B174 (TK, 30.07.2026 — "প্রেসক্রিপশনে বয়স/লিঙ্গ/ঠিকানা
    // auto-fill হচ্ছে না")। এই পর্দাতেই (হেডারে "MALE-40" ও ঠিকানা) এই তথ্য
    // থাকে (`data.age`/`data.sex`/`data.address`), কিন্তু আগে এখানে ধরে
    // রাখা হত না, তাই Take Action → Prescription/Diet Chart-এ পাঠানো যেত না।
    private var currentPatientAge: String = ""
    private var currentPatientSex: String = ""
    private var currentPatientAddress: String = ""
    // 🔒 B569 (08.08.2026): রোগীর ছবি (data URL) — A4 চেকআপ রিপোর্টে ডিটেলসের বাঁ পাশে বসে।
    private var currentPatientPhoto: String = ""
    /** 🔴 V505 — `2026-08-21` → `21/08/2026` (TK-এর বাছাই করা ধরন)।
     *  পড়া না গেলে যা আছে তাই ফেরে — কখনো আন্দাজে কিছু বসে না। */
    // 🔴🔒 V936 (TK-নির্দেশ — এক ফরম্যাট): স্ল্যাশ ছিল, এখন প্রজেক্টের বিন্দু।
    private fun tkSlashDate(raw: String): String = try {
        if (raw.isBlank()) "" else java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)
            .format(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(raw.take(10))!!)
    } catch (_: Throwable) { raw }

    /** 🔴 V505 — ISO `createdAt` → `12.34Pm`। জানা না গেলে ফাঁকা (তখন শুধু তারিখ)। */
    private fun tkClockTime(iso: String): String = try {
        if (iso.isBlank()) "" else {
            val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            f.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val d = f.parse(iso) ?: throw IllegalStateException()
            val out = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US)   // 🔴 V1158
            out.timeZone = java.util.TimeZone.getDefault()
            out.format(d).let { it.dropLast(2) + it.takeLast(2).first().uppercase() + it.takeLast(1).lowercase() }
        }
    } catch (_: Throwable) { "" }

    private var currentRegistrationDate: String = ""
    /* 🔴🔒 V1145 (০৬.০৯.২০২৬, TK-নির্দেশ) — রোগীর সময়ের ধরন ("Official Time" /
       "Unexpected Time")। এডিট পপ-আপে এটাও শুধরানো যায়, কারণ স্টাফের বাড়তি
       পাওনা প্রতিবার **এই ঘরটা ধরেই** গোনা হয় (UnexpectedIncentive)। */
    private var currentTimeType: String = ""
    /** 🔴🔒 V505 (TK-নির্দেশ ২১.০৮.২০২৬) — রোগীর সারির `createdAt`, অর্থাৎ
     *  **রেজিস্ট্রেশনের আসল সময়**। পরে আবার বার্তা পাঠালেও এই সময়টাই যায়। */
    private var currentRegistrationCreatedAt: String = ""
    private var currentRegisteredByMobile: String = ""
    private var currentEnquiryDate: String = ""
    private var currentEnquiryReceivedBy: String = ""
    // TK-REQUESTED ADDITION (2026-07-24): "Complete despite Due" workflow --
    // Staff requests, Master approves/rejects, all from Take Action. Real
    // Due amount is never changed by this, only tracked here so the menu
    // knows whether to show "Request" (no request yet), "Pending" (already
    // requested, waiting), or "Approve/Reject" (Master, request pending).
    private var currentDue: Double = -1.0
    // 🔒 TK-এর নির্দেশ (01.08.2026, স্ক্রিনশটসহ): "Bill বার্তা"-য় আজকে কত
    // পেমেন্ট হয়েছে সেটাও দেখাতে হবে — এই তালিকাটা দরকার হয় সেই হিসাব বের
    // করতে (Take Action মেনু আলাদা ফাংশনে, তাই class-level রাখা হলো)।
    private var currentEntries: List<TimelineEntry> = emptyList()
    private var currentCompleteRequestedBy: String = ""
    private var currentCompleteApprovedBy: String = ""
    // TK-REQUESTED ADDITION (2026-07-24): call-then-remind, same pattern
    // already proven in FollowUpActivity -- tapping the new call icon opens
    // the phone's app-chooser (Intent.createChooser, always asks even if a
    // default dialer is set, so Phone/Truecaller/Superfone etc. all show up
    // every time); onResume then offers to log a dated Remark for this call.
    private var pendingCall = false

    // TK-REQUESTED (2026-07-24): strips the stored "Label: value" prefixes
    // (Vill:/PO:/PS:/Dist:/PIN: -- see PatientModel.buildAddress()) for
    // this header's display only, joining whatever non-blank parts exist
    // with plain commas. The actual stored address (used everywhere else
    // -- prescriptions, print, Registration edit) is never touched; this
    // is purely a display transform run fresh every time the header
    // renders.
    private fun stripAddressLabels(addressIn: String): String {
        val address = addressIn.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ) — শুধু দেখানোর সময় বড় হাতে; ডেটাবেসে কিছু বদলায় না।
        if (address.isBlank()) return address
        fun strip(a: String): String = a.split(",").joinToString(",") { part ->
            val trimmed = part.trim()
            val colonIdx = trimmed.indexOf(':')
            (if (colonIdx >= 0) trimmed.substring(colonIdx + 1) else trimmed).trim()
        }
        // 🔒 B554 (08.08.2026, TK-অনুমোদিত প্রুফ "গ্লোবাল রুলস") — ঠিকানা দু'লাইনে:
        // থানা-চিহ্নের ঠিক আগে ভাঙা হয় (raw ঠিকানায়, লেবেল কাটার আগে), তাই ১ম
        // লাইনে গ্রাম+পোস্ট, ২য় লাইনে থানা+জেলা — তারপর প্রতিটা লাইন থেকে লেবেল
        // (Vill:/PO:/PS:/Dist:) কাটা হয় (এই পর্দায় আগে থেকেই লেবেল ছাড়া দেখায়,
        // TK-নির্দেশ 2026-07-24)। চিহ্ন না পেলে আগের মতোই এক লাইন। tvAddressVal
        // TextView, তাই "\n" দুই লাইনে দেখায়। ⛔ সেভ-হওয়া ঠিকানা বদলায় না।
        val markers = listOf("PS:", "P.S", "P/S", "Thana", "থানা", "Police Station")
        var idx = -1
        for (m in markers) { val i = address.indexOf(m, ignoreCase = true); if (i > 0 && (idx == -1 || i < idx)) idx = i }
        if (idx <= 0) return strip(address)
        val first = strip(address.substring(0, idx).trim().trimEnd(',').trim())
        val second = strip(address.substring(idx).trim())
        return if (first.isBlank() || second.isBlank()) strip(address) else "$first\n$second"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TK-REPORTED (2026-07-27): this screen has no bottom bar, so until
        // now opening it never retried a save that was still stuck on this
        // phone. A staff member could sit here while a registration or a
        // payment stayed unsent. Same retry every other screen already does.
        try { BottomNav.retryStuckSaves(this) } catch (_: Throwable) { }
        // ULTIMATE CRASH-SAFETY FIX (TK-reported via video, 2026-07-16): the
        // whole screen-setup is now wrapped -- the earlier fix only guarded
        // the data-loading part; TK's video shows the crash happening almost
        // instantly (too fast to be a network call), which means whatever
        // is failing is likely in this synchronous setup code, not the
        // network load. This guarantees NOTHING in this screen can crash
        // the app anymore -- worst case it shows an error and returns.
        try {
            binding = ActivityPatientTimelineBinding.inflate(layoutInflater)
            setContentView(binding.root)
            UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

            val mobile = intent.getStringExtra("mobile")?.filter { it.isDigit() }?.takeLast(10) ?: ""
            /* 🔵🔒 V517 (TK-অনুমোদিত): এক নম্বরে একাধিক রোগী থাকলে Search/অন্য
               পর্দা **কোন রোগী** সেটাও পাঠায়। ⛔ না পাঠালে ফাঁকা — তখন এই পর্দা
               হুবহু আগের মতোই চলে, তাই পুরোনো ২২টা ডাকার জায়গার একটাও ভাঙে না। */
            preferPatientRowId = intent.getStringExtra("patientRowId").orEmpty()
            if (mobile.length != 10) {
                // CLARITY FIX (TK-reported, 2026-07-16): this used to finish()
                // silently with no message -- looked exactly like "View All does
                // nothing / bounces back" with no explanation. Now it says why.
                android.widget.Toast.makeText(this, "No valid mobile number for this entry", android.widget.Toast.LENGTH_SHORT).show()
                finish(); return
            }
            currentMobile = mobile
            autoActionPending = intent.getBooleanExtra("autoAction", false)
            // TK-DECISION (2026-07-24, supersedes the 2026-07-16 note below):
            // View All now shows the CURRENT STAGE's own history by default
            // (Enquiry card -> Enquiry calls; Visit card -> only what
            // happened after Registration; Patient/Treatment card -> the
            // complete history, since nothing comes after it). Only the
            // dedicated "Full Journey" button forces the complete unfiltered
            // A-to-Z view now, via the fullJourney intent extra below.
            // (Old 2026-07-16 note, no longer current: "View All now ALWAYS
            // shows the complete, unfiltered timeline for Enquiry, Visit,
            // and Patient cards alike -- no more partial/section-only view.")
            currentSection = null
            forceFullJourney = intent.getBooleanExtra("fullJourney", false)

            // TK-REQUESTED (2026-07-27): draw the WHOLE header immediately from
            // what the card already knew, before any network call, so this
            // screen opens in one go like any other app instead of showing
            // "Loading..." in a default colour and then jumping.
            paintInstantHeader()

            adapter = TimelineAdapter(this, emptyList()) { entry -> tryEditTimelinePayment(entry) }
            binding.recyclerView.layoutManager = LinearLayoutManager(this)
            binding.recyclerView.adapter = adapter

            // TK-নির্দেশ (03.08.2026, "ওকে লক") — ← Back তীর XML থেকে সরানো
            // হয়েছে (একদম বাঁ-কিনারে ছবি, বেশি জায়গা টেক্সটের জন্য), তাই
            // এই ক্লিক-লিসেনারও সরানো হলো — ফোনের নিজের Back বোতাম/জেসচার
            // দিয়েই আগের মতোই ফিরে যাওয়া যাবে (কাস্টম onBackPressed ছিল না)।
            // TK-REQUESTED ADDITION (2026-07-24): call icon next to Mob --
            // shows a "which app?" picker (Phone/Superfone/anything else
            // installed EXCEPT Truecaller, see filter just below) every
            // time, even when a default dialer is set. pendingCall is
            // checked in onResume() below to offer a dated Remark once
            // they return from the call.
            binding.btnCallMob.setOnClickListener {
                if (currentMobile.length == 10) {
                    pendingCall = true
                    // TK-REQUESTED (2026-07-24): now uses the shared
                    // CallChooser.kt (same exact behaviour, just no longer
                    // duplicated inline here) -- see CallChooser.kt for why.
                    CallChooser.open(this, currentMobile)
                }
            }

            // 🔒 B569 (08.08.2026, TK-অনুমোদিত প্রুফ): আগের "Full Journey" বোতামটা
            // আসলে এই একই পর্দাই আবার খুলত (View All = Full Journey — TK-এর যাচাই),
            // তাই ডুপ্লিকেট ছিল। এখন এটা "History" — চাপলে এই রোগীর A4 চেকআপ
            // রিপোর্ট (ফটোসহ) খোলে। ⛔ বাকি ৩ বোতাম (Report Card/Payment/Action)
            // অপরিবর্তিত। XML-এ btnCall-এর লেখাও "📜 History" করা হয়েছে।
            // 🟢🔒 V627 (২৪.০৮.২০২৬, TK-নির্দেশ) — নাম বদলে "Checkup History"
            // (XML দেখুন)। চেকআপ না থাকলে এখন Toast না দেখিয়ে সরাসরি Doctor
            // Checkup ফর্ম খোলে (openCheckupHistory()-এর ভিতরেই)।
            binding.btnCall.setOnClickListener { openCheckupHistory() }
            // TK-REQUESTED (2026-07-22): was WhatsApp -- now Report Card.
            // Report Card needs a bill to exist (same condition Take Action
            // already uses); a plain message here instead of an empty screen
            // if there's no bill yet.
            binding.btnWhatsApp.setOnClickListener {
                if (currentBillTotal > 0.0) {
                    /* 🔵🔒 V522: এই Timeline যে রোগীরটা দেখাচ্ছে, Report Card-ও তাঁরই। */
                    startActivity(
                        Intent(this, ReportCardActivity::class.java)
                            .putExtra("mobile", currentMobile)
                            .putExtra("patientRowId", preferPatientRowId)
                    )
                } else {
                    android.widget.Toast.makeText(this, "No bill yet for this patient — Report Card needs an Advance Payment first", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            binding.btnPayment.setOnClickListener {
                // TK-REQUESTED (2026-07-27): "directForm" makes PaymentActivity
                // skip its own Collection screen and open this patient's
                // payment form immediately; closing the form returns straight
                // here. Every other caller omits this extra and is unaffected.
                reloadOnReturn = true
                startActivity(
                    Intent(this, PaymentActivity::class.java)
                        .putExtra("mobile", mobile)
                        .putExtra("directForm", true)
                        /* 🔵🔒 V520: এই Timeline যে রোগীরটা দেখাচ্ছে, টাকাও তাঁরই। */
                        .putExtra("patientRowId", preferPatientRowId)
                        .putExtra("patientCode", currentPatientCode)
                )
            }
            binding.btnTakeAction.setOnClickListener { requestTakeActionMenu() }

            // 🔒 item 12 (৯২-item তালিকা, TK-এর স্ক্রিনশট 01.08.2026 রাত ১১.২৩ pm
            // — "Full J..." · "Repor..." · "Payme..." কেটে যাচ্ছিল): চারটে
            // বোতামই একটাই সারিতে সমান চওড়ায়, লেখা কাটার (ellipsize) বদলে
            // এখন প্রয়োজনে নিজে থেকে ছোট হয়ে পুরো লেখা এক লাইনেই দেখায় —
            // ঠিক সেই একই প্রমাণিত পদ্ধতি যা Patient ID কলামে আগে থেকেই আছে
            // (FollowUpActivity.kt, TK-অনুমোদিত)। ⛔ বোতামের রং/কাজ/মাপ/ক্রম
            // কিছুই বদলায়নি — শুধু লেখার মাপ এখন 9–12.5sp-এর মধ্যে নিজে
            // মানিয়ে নেয়, `ellipsize` তবু পাহারা হিসেবে থেকে গেল।
            // 🔴 TK-REPORTED AGAIN (02.08.2026, স্ক্রিনশট — "Full J...", "Repor...",
            // "Paym..." তখনও কাটছিল): আগের B260 ফিক্স (TextViewCompat.
            // setAutoSizeTextTypeUniformWithConfiguration) আসলে কাজই করছিল না —
            // ব্র্যাকেট/কম্পাইল ঠিক ছিল, পাহারাদারও পাশ দিয়েছিল, কিন্তু চোখে
            // দেখলে বোঝা যায়নি যে এই অটোসাইজ পদ্ধতিটা `MaterialButton`-এ
            // নির্ভরযোগ্যভাবে কাজ করে না (FollowUpActivity.kt-এর Patient ID
            // কলামে যেটা কাজ করে, সেটা একটা সাধারণ TextView — MaterialButton
            // নয়; দুটো আলাদা widget, Material Components-এর নিজস্ব টেক্সট-
            // হ্যান্ডলিং autosize-কে চাপা দিয়ে দেয়)। **সমাধান:** পর্দা আঁকা
            // শেষ হওয়ার পরে (`doOnLayout`) প্রতিটা বোতামের আসল চওড়ায় লেখাটা
            // সত্যিই ধরে কিনা মেপে দেখা হয়, না ধরলে টেক্সট-সাইজ এক ধাপ ছোট
            // করে আবার মাপা হয় — যতক্ষণ না ধরে (৮sp পর্যন্ত, তার নিচে আর
            // ছোট করা হয় না)। ⛔ বোতামের রং/কাজ/মাপ/ক্রম কিছুই বদলায়নি।
            for (btn in listOf(binding.btnCall, binding.btnWhatsApp, binding.btnPayment, binding.btnTakeAction)) {
                btn.setSingleLine(true)
                btn.doOnLayout {
                    val avail = btn.width - btn.paddingLeft - btn.paddingRight -
                        btn.compoundDrawablePadding -
                        (btn.compoundDrawables.getOrNull(0)?.bounds?.width() ?: 0)
                    if (avail <= 0) return@doOnLayout
                    var size = 13f
                    val paint = android.graphics.Paint(btn.paint)
                    while (size > 8f) {
                        paint.textSize = size * resources.displayMetrics.scaledDensity
                        if (paint.measureText(btn.text.toString()) <= avail) break
                        size -= 0.5f
                    }
                    btn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size)
                }
            }

            // TK-REQUESTED (2026-07-27): the patient photo used to be changed
            // by triple-tapping the photo tile on the Follow-up Visit/Patient
            // card. That tile has now been removed from those cards (TK's
            // decision: photo only inside View All), which would have left NO
            // way at all to add or change a patient's photo. The exact same
            // 3-tap action is therefore moved onto the photo box HERE, where
            // the photo now lives. Nothing on screen changes -- no new button,
            // no new text; the box simply responds to three taps, exactly the
            // way the card's tile always did.
            TripleTapEdit.attach(binding.ivPhoto) {
                val digits = currentMobile.filter { it.isDigit() }.takeLast(10)
                if (digits.length != 10) {
                    android.widget.Toast.makeText(this, "No valid 10-digit mobile", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    reloadOnReturn = true
                    startActivity(
                        // 🔵🔒 V530: এই Timeline যে রোগীর, ছবির পর্দাও ঠিক তাঁরই খুলবে।
                        Intent(this, PatientPhotoActivity::class.java)
                            .putExtra("mobile", digits)
                            .putExtra("patientRowId", preferPatientRowId)
                    )
                }
            }

            load(mobile, currentSection)
        } catch (e: Throwable) {
            val root = e.cause ?: e
            val trace = root.stackTrace.take(4).joinToString("\n") { "  at $it" }
            val fullMsg = root.javaClass.name + (root.message?.let { ": $it" } ?: "") + "\n\n" + trace
            val scrollView = android.widget.ScrollView(this)
            val tv = android.widget.TextView(this).apply {
                text = fullMsg
                textSize = 12f
                setPadding(24, 16, 24, 16)
                setTextIsSelectable(true)
            }
            scrollView.addView(tv)
            UppercaseInputUtil.applyToAll(scrollView)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "⚠️ Error detail (for fixing)"))
                .setView(scrollView)
                .setPositiveButton("Close") { _, _ -> finish() }
                .setCancelable(false)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    // TK-REQUESTED ADDITION (2026-07-24): call-then-remind -- same proven
    // pattern as FollowUpActivity.onResume(). If the person came back from
    // the call-app chooser (pendingCall set by btnCallMob above), offer to
    // log a dated Remark; declining just does nothing further (no forced
    // entry). currentFollowupId must exist for showQuickRemarkDialog() to
    // actually save (same guard it already has).
    // TK-REQUESTED (2026-07-27): set only when this screen sends the staff to
    // the Advance form. Coming back, the new payment must already be visible
    // in the table, so the screen reloads once. Deliberately NOT a reload on
    // every onResume -- that would cost a full fetch every time the staff
    // switches apps, which is exactly the slowness TK reported.
    private var reloadOnReturn = false

    // 🟢🔒 V1519 — while this patient's timeline is actually open, a payment/
    // patient/follow-up change for this same person wakes the existing load().
    // No extra row data is streamed; RealtimeRefresh carries only identifiers.
    private val timelineRealtimeWatch = LiveRefresh.Watch("payments", "patients", "followups")

    override fun onResume() {
        super.onResume()
        timelineRealtimeWatch.startRealtime(
            branchProvider = { currentBranch },
            rowFilter = { change ->
                val eventMobile = change.mobile?.filter { it.isDigit() }?.takeLast(10).orEmpty()
                val ownMobile = currentMobile.filter { it.isDigit() }.takeLast(10)
                val eventPid = change.patientId?.trim().orEmpty()
                val ownPid = currentPatientCode.trim()
                // DELETE/legacy rows may not carry mobile/patientId; refresh safely in that case.
                (eventMobile.isBlank() && eventPid.isBlank()) ||
                    (eventMobile.isNotBlank() && eventMobile == ownMobile) ||
                    (eventPid.isNotBlank() && ownPid.isNotBlank() && eventPid == ownPid)
            }
        ) {
            if (currentMobile.isNotBlank() && !isFinishing && !isDestroyed) {
                try { load(currentMobile, currentSection) } catch (_: Throwable) { }
            }
        }
        if (reloadOnReturn) {
            reloadOnReturn = false
            try { load(currentMobile, currentSection) } catch (_: Throwable) { }
        }
        if (pendingCall) {
            pendingCall = false
            if (currentFollowupId.isNotBlank()) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, "Update remark?"))
                    .setMessage("Add a remark for ${currentPatientName.ifBlank { currentMobile }} after that call?")
                    .setPositiveButton("Add Remark") { _, _ -> showQuickRemarkDialog() }
                    .setNegativeButton("Not now", null)
                    .show().also { PremiumAlert.paint(it) }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        timelineRealtimeWatch.stopRealtime()
    }

    /** TK-REQUESTED (2026-07-18): 3-tap edit for the Patient Card header
     *  (name/mobile). Saves to the "patients" (Registration) row — same
     *  Master/Staff permission rule as tryEditTimelinePayment above. If no
     *  Registration exists yet for this person (Enquiry-only), tells the
     *  user instead of attempting a save that has nothing to update. */
    private fun showPatientHeaderEdit() {
        val user = NativeSession.current(this)
        // 🔴🔴🔒 খাতার সারি B442 (TK-নির্দেশ, 05.08.2026 — "পেমেন্ট ছাড়া
        // বাকি সব কাজে স্টাফের Master-এর অনুমতি লাগবে কেন? Master-এর কি আর
        // কোনো কাজ নেই?")। **আসল কারণ:** এখানে আগে "Only Master can edit
        // patient name/mobile here" — স্টাফের জন্য সম্পূর্ণ বন্ধ ছিল, কোনো
        // অনুরোধ-পাঠানোর ব্যবস্থাও ছিল না। নাম/মোবাইল/রেফারেল-ডাক্তার —
        // এসব **টাকার তথ্য না**, তাই TK-এর নতুন নিয়ম অনুযায়ী এখন যেকোনো
        // লগইন-করা স্টাফ সরাসরি এডিট করতে পারবেন — ঠিক Follow-up কার্ডের
        // নিজস্ব "Edit Record" (`FollowUpActivity.showEditDialog`, যেখানে
        // কখনোই এই বাধা ছিল না)-এর মতোই। ⛔ **টাকার তথ্য (Payment/Bill/
        // Estimated) এই ফাংশন ছোঁয় না** — সেগুলোর নিজস্ব same-day/Master
        // নিয়ম (`tryEditTimelinePayment`, `PaymentActivity`) অক্ষত আছে,
        // TK-এর রুলের সাথে ইতিমধ্যেই মিলে যায় বলে এক অক্ষরও বদলানো হয়নি।
        if (user == null) { finish(); return }
        if (currentPatientRowId.isBlank()) {
            android.widget.Toast.makeText(this, "No Registration record yet for this patient — nothing to edit here", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(0))
        }
        val nameInput = android.widget.EditText(this).apply { setText(currentPatientName); hint = "Patient Name" }
        val mobileInput = android.widget.EditText(this).apply {
            setText(currentMobile); hint = "Mobile"; inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        // 🟢🔒🔒 V664 (২৫.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট) — "ফলোআপ কার্ডে তিনবার
        // চাপ দিলে যে এডিট হয়... আমি চাইছি ওইটাই এখানে থাকুক।" — Follow-up
        // কার্ডের নিজের "Edit Record" (`FollowUpActivity.showEditDialog`)-এ
        // Branch/Disease/Age/Sex/Address আছে, এখানে ছিল না — এখন সেই একই
        // ঘরগুলো এখানেও যোগ করা হলো। ⛔ সেভের নিরাপত্তা (মোবাইল বদলালে
        // Follow-up/Enquiry/Payment সিঙ্ক) নিচে **অক্ষত** রাখা হয়েছে —
        // নতুন ঘরগুলো শুধু `patients` টেবিলে সরাসরি বসে, কোনো নতুন ঝুঁকি
        // নেই।
        /* 🔴🔒 V1642 (২০.০৯.২০২৬, TK-নির্দেশ, ছবিসহ — COB-ID অথচ Birpara ব্রাঞ্চ)
           — এই সাধারণ Edit Patient ফর্মে আগে একটা Branch ঘর ছিল, যেটা বদলালে
           শুধু `patients.branch` লেখা পড়ত, কিন্তু Patient ID-র ব্রাঞ্চ-সংকেত
           (আইডির প্রথম অক্ষরগুলো) কখনো নতুন করে বানানো হতো না — উদ্দেশ্যেই
           তৈরি করা "Change Branch" (BranchTransferRepository, V1630) বোতাম
           এই কাজটা সঠিকভাবে করে (ID + payments-এর patientCode দুটোই ঠিক
           করে), কিন্তু এই ফর্মের ঘরটা সেই পথ এড়িয়ে যেত। ⛔ ব্রাঞ্চ বদলানোর
           একটাই পথ থাকুক — তাই এই ঘরটা সরিয়ে দেওয়া হলো, ব্রাঞ্চ বদলাতে হলে
           এখন থেকে শুধু "Change Branch" বোতামই ব্যবহার হবে। */
        /* 🩺🔒 V1000 (০৩.০৯.২০২৬) — একজন রোগীর একাধিক রোগ থাকতে পারে
           (রেজিস্ট্রেশনে বহুদিন ধরেই টিক দিয়ে একাধিক বাছা যায়, ঘরে ", "
           দিয়ে জোড়া বসে)। আগে তালিকায় না মিললে স্পিনার নিঃশব্দে "Piles"
           দেখাত, আর সেভ করলেই জোড়া লেখাটা মুছে যেত। এখন যা আছে সেটাই
           তালিকার শুরুতে বসে — কিছু না ছুঁলে কিছুই বদলায় না। */
        val baseDiseaseOptions = listOf("Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog", "Other")
        val diseaseOptions = if (currentDisease.isNotBlank() &&
            baseDiseaseOptions.none { it.equals(currentDisease, ignoreCase = true) })
            listOf(currentDisease) + baseDiseaseOptions else baseDiseaseOptions
        val diseaseLabel = android.widget.TextView(this).apply { text = "Disease"; textSize = 11.5f; setPadding(0, dp(14), 0, dp(2)) }
        val diseaseSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@PatientTimelineActivity, android.R.layout.simple_spinner_dropdown_item, diseaseOptions)
            val di = diseaseOptions.indexOfFirst { it.equals(currentDisease, ignoreCase = true) }; if (di >= 0) setSelection(di)
        }
        val ageLabel = android.widget.TextView(this).apply { text = "Age"; textSize = 11.5f; setPadding(0, dp(14), 0, dp(2)) }
        val ageInput = android.widget.EditText(this).apply {
            setText(currentPatientAge); hint = "Age"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
        }
        val sexOptions = listOf("Male", "Female", "Other")
        val sexLabel = android.widget.TextView(this).apply { text = "Sex"; textSize = 11.5f; setPadding(0, dp(14), 0, dp(2)) }
        val sexSpinner = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@PatientTimelineActivity, android.R.layout.simple_spinner_dropdown_item, sexOptions)
            val si = sexOptions.indexOfFirst { it.equals(currentPatientSex, ignoreCase = true) }; if (si >= 0) setSelection(si)
        }
        val addressLabel = android.widget.TextView(this).apply { text = "Address"; textSize = 11.5f; setPadding(0, dp(14), 0, dp(2)) }
        val addressInput = android.widget.EditText(this).apply { setText(currentPatientAddress); hint = "Address" }
        /* 🔴🔒 V1142 (০৬.০৯.২০২৬, TK-নির্দেশ ও অনুমতি) — TK: *"পুরানো দিনের
           পেশেন্টের এন্ট্রি এখন করছি, ভুল করে তারিখটা আজকের হয়ে গেছে; তারিখ
           চেঞ্জ করার তো কোনো এডিট অপশনই নেই"* · *"রেজিস্ট্রেশন ফর্মটাই আবার
           এডিট হবে"* · আর আইডি নিয়ে জিজ্ঞাসা করায় TK: *"আইডিও বদলাবে"*।

           ⇒ এখানে এখন **রেজিস্ট্রেশনের তারিখ** ও ফর্মের বাকি ঘরগুলোও আছে।
           ⛔ তারিখ বদলালে রোগীর **আইডিও** নতুন তারিখ ধরে বানানো হয় (অ্যাপের
              নিজের প্রমাণিত `PatientIdGenerator`), আর সেই নতুন আইডি রোগীর সারি ·
              ফলোআপ · এনকোয়ারি · টাকার সারি · মেডিকেল — সব জায়গায় বসে, যাতে
              কোথাও পুরনো আইডি পড়ে না থাকে।
           ⛔ **টাকার অঙ্ক · তারিখ · ধরন কিচ্ছু ছোঁয়া হয় না** — টাকার সারিতে
              কেবল আইডির ঘরটা।
           ⛔ নিচের বাড়তি ঘরগুলো **ফাঁকা থাকলে সেভই হয় না** (blank = ছুঁয়ো না),
              তাই পড়া ব্যর্থ হলেও পুরনো তথ্য হারানোর পথ নেই। */
        var pickedRegDate = currentRegistrationDate.take(10)
        val regDateBtn = android.widget.TextView(this).apply {
            text = if (pickedRegDate.isBlank()) "Tap to set" else FollowUpModel.displayDate(pickedRegDate)
            textSize = 15f
            setPadding(dp(4), dp(10), dp(4), dp(10))
            setTextColor(android.graphics.Color.parseColor("#0B5F2E"))
            isClickable = true; isFocusable = true
            setOnClickListener {
                val cal = java.util.Calendar.getInstance()
                try {
                    val p2 = pickedRegDate.split("-")
                    if (p2.size == 3) cal.set(p2[0].toInt(), p2[1].toInt() - 1, p2[2].toInt())
                } catch (_: Throwable) { }
                android.app.DatePickerDialog(this@PatientTimelineActivity, { _, y, m, dd ->
                    pickedRegDate = "%04d-%02d-%02d".format(y, m + 1, dd)
                    text = FollowUpModel.displayDate(pickedRegDate)
                }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
                    cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
            }
        }
        /* 🔴🔒 V1145 (০৬.০৯.২০২৬, TK-নির্দেশ) — TK: *"অফিসিয়াল টাইমে কল
           এসেছিল, স্টাফ ভুল করে আনএক্সপেক্টেড করে দিয়েছিল … এগুলো তো টাকা
           পয়সার হিসাব"*।
           ⇒ দুটো বোতাম, রেজিস্ট্রেশন ফর্মের মতোই। বাছাই বদলালে সেভের সময়
             `timeType` **তিন জায়গাতেই** বসে — patients (কার্ডের ব্যাজ) ·
             enquiries (টাকার হিসেব) · followups।
           ⛔ হাত না দিলে কোথাও কিছু লেখা হয় না (আগের মানটাই থাকে)। */
        var pickedTiming = currentTimeType.trim()
        val timingRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        val timingBtns = ArrayList<android.widget.TextView>()
        fun paintTiming() {
            timingBtns.forEach { b ->
                val on = b.text.toString().equals(pickedTiming, ignoreCase = true)
                b.setBackgroundColor(android.graphics.Color.parseColor(if (on) "#0B5F2E" else "#FFFFFF"))
                b.setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#475467"))
            }
        }
        listOf("Official Time", "Unexpected Time").forEach { t ->
            val b = android.widget.TextView(this).apply {
                text = t
                textSize = 13.5f
                gravity = android.view.Gravity.CENTER
                setPadding(dp(6), dp(10), dp(6), dp(10))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                ).apply { setMargins(dp(3), 0, dp(3), 0) }
                isClickable = true; isFocusable = true
                setOnClickListener { pickedTiming = t; paintTiming() }
            }
            timingBtns.add(b); timingRow.addView(b)
        }
        paintTiming()

        fun formBox(h: String) = android.widget.EditText(this).apply { hint = h }
        val occupationInput = formBox("Occupation")
        val sinceWhenInput = formBox("Since when")
        val complaintInput = formBox("Complaint")
        val medHistoryInput = formBox("Medical history")
        val prevTreatInput = formBox("Previous treatment")
        val prevResultInput = formBox("Previous result")
        val prevCostInput = formBox("Previous cost").apply {
            // ⛔ B411-এর নিয়ম: একা TYPE_CLASS_NUMBER-এ কিছু ফোনে কীবোর্ড খোলে না।
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
        }
        // ⛔ ফর্মের এই ঘরগুলো এই পর্দায় আগে থেকে জমা নেই — একটাই সারি পড়ে
        //    ভরা হয় (কয়েকশো বাইট)। না এলে ফাঁকা থাকে, আর ফাঁকা মানে "বদলিও না"।
        run {
            val rowId = currentPatientRowId
            BackgroundWork.run {
                val row = try {
                    val arr = SupabaseClient.fetchListSlim(
                        "patients", "id=eq.$rowId", 1,
                        "occupation,sinceWhen,complaint,medicalHistory,previousTreatment,previousResult,previousCost")
                    if (arr.length() > 0) arr.optJSONObject(0) else null
                } catch (_: Throwable) { null } ?: return@run
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (isFinishing || isDestroyed) return@post
                    fun fill(v: android.widget.EditText, key: String) {
                        if (v.text.isNullOrBlank()) v.setText(row.s(key))
                    }
                    fill(occupationInput, "occupation"); fill(sinceWhenInput, "sinceWhen")
                    fill(complaintInput, "complaint"); fill(medHistoryInput, "medicalHistory")
                    fill(prevTreatInput, "previousTreatment"); fill(prevResultInput, "previousResult")
                    fill(prevCostInput, "previousCost")
                }
            }
        }
        val refLabel = android.widget.TextView(this).apply {
            text = "Referred by Doctor (optional — fill in later if it becomes known)"
            textSize = 11.5f
            setPadding(0, dp(14), 0, dp(2))
        }
        val refDoctorInput = android.widget.EditText(this).apply { setText(currentRefDoctor); hint = "Doctor Name" }
        val refDoctorMobileInput = android.widget.EditText(this).apply {
            setText(currentRefDoctorMobile); hint = "Doctor Mobile"; inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        container.addView(nameInput)
        container.addView(mobileInput)
        // 🟢🔒 V664 — নতুন ঘরগুলো, নাম/মোবাইলের ঠিক পরে (রেফারিং ডাক্তারের আগে)
        container.addView(diseaseLabel); container.addView(diseaseSpinner)
        container.addView(ageLabel); container.addView(ageInput)
        container.addView(sexLabel); container.addView(sexSpinner)
        container.addView(addressLabel); container.addView(addressInput)
        // 🔴 V1142 — রেজিস্ট্রেশনের তারিখ ও ফর্মের বাকি ঘর
        fun formLabel(t: String) = android.widget.TextView(this).apply {
            text = t; textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#475467"))
            setPadding(0, dp(10), 0, dp(2))
        }
        container.addView(formLabel("Registration Date")); container.addView(regDateBtn)
        container.addView(formLabel("Time type")); container.addView(timingRow)
        container.addView(formLabel("Occupation")); container.addView(occupationInput)
        container.addView(formLabel("Since when")); container.addView(sinceWhenInput)
        container.addView(formLabel("Complaint")); container.addView(complaintInput)
        container.addView(formLabel("Medical history")); container.addView(medHistoryInput)
        container.addView(formLabel("Previous treatment")); container.addView(prevTreatInput)
        container.addView(formLabel("Previous result")); container.addView(prevResultInput)
        container.addView(formLabel("Previous cost")); container.addView(prevCostInput)
        container.addView(refLabel)
        container.addView(refDoctorInput)
        container.addView(refDoctorMobileInput)
        // 🟢🔒 V664 — ScrollView-এর ভিতরে বসানো (আগে থেকে ScrollView-এই
        // container বসে, নিচেই দেখুন — নতুন ঘর যোগ হওয়ায় ফর্ম লম্বা হলো,
        // তাই এখন স্ক্রল করে দেখা যাবে, কিছু হারানোর ঝুঁকি নেই)।
        UppercaseInputUtil.applyToAll(container)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val editDlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "✏️ Edit Patient"))
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()
        // TK-DECISION (2026-07-22): don't auto-close on Save -- so a wrong
        // field can be shown red (FieldError) with the dialog still open.
        editDlg.setOnShowListener {
            editDlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newName = nameInput.text.toString().trim()
                val newMobile = mobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
                val vmsg = FieldError.validate(listOf<Triple<View, Boolean, String>>(
                    Triple(nameInput, newName.isNotBlank(), "Patient নাম দিন"),
                    Triple(mobileInput, newMobile.length == 10, "সঠিক 10 ডিজিট মোবাইল দিন")
                ))
                if (vmsg != null) {
                    android.widget.Toast.makeText(this, vmsg, android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val newRefDoctor = refDoctorInput.text.toString().trim()
                val newRefDoctorMobile = refDoctorMobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
                // 🟢🔒 V664 (২৫.০৮.২০২৬, TK-নির্দেশ) — নতুন ঘরগুলোর মান।
                // ⛔ V1642 — Branch এখান থেকে বাদ (উপরের মন্তব্য দেখুন)।
                val newDisease = diseaseSpinner.selectedItem?.toString().orEmpty()
                val newAge = ageInput.text.toString().trim()
                val newSex = sexSpinner.selectedItem?.toString().orEmpty()
                val newAddress = addressInput.text.toString().trim()
                val fields = org.json.JSONObject()
                    .put("name", newName)
                    .put("mobile", newMobile)
                    .put("refDoctor", newRefDoctor)
                    .put("refDoctorMobile", newRefDoctorMobile)
                    // 🟢🔒 V664 — Follow-up কার্ডের "Edit Record"-এর সাথে মিলিয়ে,
                    // এই ঘরগুলোও একইভাবে সরাসরি লেখা হয় (ফাঁকা হলেও
                    // Sex/Disease-এর জন্য সমস্যা নেই — স্পিনারে সবসময়
                    // একটা মান বাছাই থাকে; Age/Address ফাঁকা থাকলে ফাঁকাই বসে,
                    // ঠিক আগে থেকে থাকা নিয়মের মতোই)।
                    .put("disease", newDisease)
                    .put("age", newAge)
                    .put("sex", newSex)
                    .put("address", newAddress)
                    // TK-REQUESTED (2026-07-18): if the referring doctor is
                    // filled in after the fact, also mark refBy = "Dr. Visit"
                    // so this patient correctly shows up on that doctor's
                    // own "Referred Patients" list (DoctorVisitActivity),
                    // exactly like it would if entered at Registration time.
                    .apply { if (newRefDoctor.isNotBlank()) put("refBy", "Dr. Visit") }
                /* 🔴🔒 V1142 — ফর্মের বাকি ঘরগুলো: **ফাঁকা মানে ছুঁয়ো না**,
                   তাই ঘরটা না ভরলে পুরনো লেখাই অক্ষত থাকে। */
                fun putIf(key: String, v: android.widget.EditText) {
                    val t = v.text.toString().trim()
                    if (t.isNotBlank()) fields.put(key, t)
                }
                putIf("occupation", occupationInput); putIf("sinceWhen", sinceWhenInput)
                putIf("complaint", complaintInput); putIf("medicalHistory", medHistoryInput)
                putIf("previousTreatment", prevTreatInput); putIf("previousResult", prevResultInput)
                putIf("previousCost", prevCostInput)
                /* 🔴🔒 V1142 — তারিখ বদলালে তিনটে তারিখের ঘরই বসে, আর রোগীর
                   **আইডিও** নতুন তারিখ ধরে বানানো হয় (TK: *"আইডিও বদলাবে"*)।
                   ⛔ তারিখ না বদলালে আইডি ও তারিখ কিচ্ছু ছোঁয়া হয় না। */
                val oldRegDate = currentRegistrationDate.take(10)
                val dateChanged = pickedRegDate.isNotBlank() && pickedRegDate != oldRegDate
                if (dateChanged) {
                    fields.put("date", pickedRegDate)
                        .put("registrationDate", pickedRegDate)
                        .put("visitDate", pickedRegDate)
                }
                val oldCode = currentPatientCode
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        var newCode = ""
                        if (dateChanged) {
                            newCode = try {
                                // ⛔ V1642 — Branch ঘর এই ফর্ম থেকে সরানো হয়েছে, তাই
                                // এখানে সবসময় রোগীর নিজের বর্তমান ব্রাঞ্চই ব্যবহার হয়।
                                PatientIdGenerator.generate(currentBranch,
                                    pickedRegDate, this@PatientTimelineActivity)
                            } catch (_: Throwable) { "" }
                            if (newCode.isNotBlank()) fields.put("patientId", newCode)
                        }
                        val mainOk = SupabaseClient.updateById("patients", currentPatientRowId, fields)
                        /* 🔴🔒 V1145 (TK-নির্দেশ) — সময়ের ধরন বদলালে তিন টেবিলেই
                           বসে (patients · enquiries · followups)। **টাকার হিসেব
                           পড়ে `enquiries.timeType`**, তাই ওটা না বসলে স্টাফের
                           বাড়তি পাওনা ভুলই থেকে যেত।
                           ⛔ হাত না দিলে (মান এক থাকলে) কিছুই লেখা হয় না। */
                        val newTiming = pickedTiming.trim()
                        if (newTiming.isNotBlank() &&
                            !newTiming.equals(currentTimeType.trim(), ignoreCase = true)) {
                            try { PersonEditSync.updateTimeType(currentMobile, newTiming) } catch (_: Throwable) { }
                        }
                        /* 🔴 V1142 — নতুন আইডি সব জায়গায় বসানো, যাতে কোথাও পুরনোটা
                           পড়ে না থাকে। ⛔ টাকার সারিতে **কেবল আইডির ঘর** — অঙ্ক ·
                           তারিখ · ধরন কিচ্ছু ছোঁয়া হয় না। */
                        if (mainOk && dateChanged && newCode.isNotBlank() && oldCode.isNotBlank()) {
                            try {
                                for ((table, cols) in listOf(
                                    "followups" to listOf("patientId"),
                                    "enquiries" to listOf("patientId"),
                                    "medical" to listOf("patientId"),
                                    "payments" to listOf("patientId", "patientCode")
                                )) {
                                    for (col in cols) {
                                        val rows = SupabaseClient.fetchListSlim(
                                            table, "$col=eq.$oldCode", 200, "id")
                                        for (i in 0 until rows.length()) {
                                            val id = rows.optJSONObject(i)?.optString("id").orEmpty()
                                            if (id.isBlank()) continue
                                            SupabaseClient.updateById(table, id,
                                                org.json.JSONObject().put(col, newCode))
                                        }
                                    }
                                }
                                android.os.Handler(android.os.Looper.getMainLooper())
                                    .post { currentPatientCode = newCode }
                            } catch (_: Throwable) { }
                        }
                        // TK-REQUESTED (2026-07-18): mobile is the join key
                        // Enquiry/Follow-up/Payments all use to find this
                        // person -- if it changes here but not there, those
                        // records silently stop matching this patient.
                        // Best-effort propagate to any followups/enquiries
                        // row still under the OLD number.
                        /* 🔴🔒 V1130 (০৬.০৯.২০২৬, TK-এর SQL-এ ধরা — রোগীর ঘরে
                           "SUMAN SARKAR", অথচ ফলোআপের ঘরে "SUBAL SARKAR")।

                           **কারণ (কোডে মেপে পাওয়া):** নিচের ধাপটা চলত **কেবল
                           তখনই, যখন মোবাইল নম্বরও বদলাত**। তাই স্টাফ শুধু **নাম**
                           ঠিক করলে নতুন নামটা রোগীর ঘরে বসত, কিন্তু ফলোআপ ও
                           এনকোয়ারির ঘরে **পুরনো নামই রয়ে যেত** — এক পর্দায় এক
                           নাম, আরেক পর্দায় আরেক নাম। রোগী কিন্তু একজনই।

                           ⇒ এখন **নাম বদলালেও** ধাপটা চলে।
                           ⛔ ভিতরের পাহারা এক অক্ষরও বদলায়নি — এক নম্বরে দুজন
                              রোগী থাকলে (`PatientIdentity`) শুধু **এই রোগীরই**
                              সারিগুলো বদলায়, অন্যজনেরটা ছোঁয়াই হয় না।
                           ⛔ নম্বর না বদলালে ওই ঘরে পুরনো নম্বরই বসে (একই মান),
                              তাই নম্বরের কিছুই বদলায় না। */
                        if (mainOk && (newMobile != currentMobile ||
                                !newName.equals(currentPatientName, ignoreCase = false))) {
                            try {
                                /* 🔵🔒 V534 (২২.০৮.২০২৬, TK-নির্দেশ) — **অন্য রোগীর সারি আর ছোঁয়া হয় না।**
                                   আগে এখানে ওই নম্বরের **সব** followups/enquiries সারিতে নতুন নাম ও
                                   নতুন নম্বর বসিয়ে দেওয়া হত — কে কার সারি তা না দেখেই। এক নম্বরে
                                   স্বামী-স্ত্রী দু'জন আলাদা রোগী থাকলে একজনের নম্বর বদলালে
                                   **অন্যজনের রেকর্ডও তুলে নিয়ে যেত আর তাঁর নামটাও মুছে দিত**।
                                   ⇒ এখন প্রজেক্টের সেই প্রমাণিত পাহারা (`PatientIdentity`, যেটা
                                     `MobileChangeSync` V134 থেকে ব্যবহার করছে): নম্বর ভাগ করা না
                                     হলে **হুবহু আগের মতোই সব সারি**; ভাগ করা হলে **শুধু প্রমাণসহ
                                     এই রোগীর সারিগুলো**।
                                   ⛔ enquiries সারিতে রোগী চেনার কোনো ঘরই নেই — তাই নম্বর ভাগ
                                      করা থাকলে সেগুলো **ছোঁয়াই হয় না** (ভুল করে অন্যের নাম বসিয়ে
                                      দেওয়ার চেয়ে কিছু না করা নিরাপদ)। */
                                val myCode = try {
                                    val prow0 = SupabaseClient.fetchListSlim("patients", "id=eq.$currentPatientRowId", 1,
                                        SupabaseClient.PATIENT_NO_PHOTO_COLS)   // 🔴 V794 — ছবি ছাড়া
                                    if (prow0.length() > 0) prow0.getJSONObject(0).s("patientId") else ""
                                } catch (_: Exception) { "" }
                                /* 🔴🔒 V794 — এই পড়াটা সারি থেকে **শুধু `id`** নেয় (যাচাই করা), অথচ
                       `followups`-এ `photo` ও `history` দুটোই ভারী — ২০ সারিতে
                       ≈১.৮ MB পর্যন্ত নামত। এখন ছোট্ট তালিকা। */
                    val oldFollowups = SupabaseClient.fetchListSlim("followups",
                        "mobile=like.*$currentMobile", 20, SupabaseClient.FOLLOWUP_ID_COLS)
                                val shared = PatientIdentity.isSharedNumber(oldFollowups, currentMobile)
                                for (i in 0 until oldFollowups.length()) {
                                    val frow = oldFollowups.optJSONObject(i)
                                    if (shared && !PatientIdentity.rowBelongsTo(frow, currentPatientRowId, myCode)) continue
                                    val fid = frow?.s("id").orEmpty()
                                    if (fid.isNotBlank()) {
                                        // TK-REPORTED (2026-07-27): a failed write here used to
                                        // be lost silently; it now retries like every other save.
                                        val f = org.json.JSONObject().put("mobile", newMobile).put("name", newName)
                                        if (!SupabaseClient.updateById("followups", fid, f)) {
                                            try { GenericUpdateQueue.queue(this@PatientTimelineActivity, "followups", fid, f) } catch (_: Throwable) { }
                                        }
                                    }
                                }
                                // ⛔ V534: নম্বর ভাগ করা থাকলে enquiries একটাও ছোঁয়া হয় না।
                                val oldEnquiries = if (shared) org.json.JSONArray()
                                    else SupabaseClient.fetchList("enquiries", "mobile=like.*$currentMobile", 20)
                                for (i in 0 until oldEnquiries.length()) {
                                    val eid = oldEnquiries.getJSONObject(i).s("id")
                                    if (eid.isNotBlank()) {
                                        val f = org.json.JSONObject().put("mobile", newMobile).put("name", newName)
                                        if (!SupabaseClient.updateById("enquiries", eid, f)) {
                                            try { GenericUpdateQueue.queue(this@PatientTimelineActivity, "enquiries", eid, f) } catch (_: Throwable) { }
                                        }
                                    }
                                }
                            } catch (_: Exception) { }
                            // TK-REPORTED (2026-07-26): also move this patient's
                            // Payment / Dr. Visit rows onto the new number, so
                            // nothing disappears from Timeline / Report Card.
                            try {
                                val pid = try {
                                    val prow = SupabaseClient.fetchListSlim("patients", "id=eq.$currentPatientRowId", 1,
                                        SupabaseClient.PATIENT_NO_PHOTO_COLS)   // 🔴 V794 — ছবি ছাড়া
                                    if (prow.length() > 0) prow.getJSONObject(0).optString("patientId", "") else ""
                                } catch (_: Exception) { "" }
                                /* ⛔ V1130 — নম্বর সত্যিই বদলালে তবেই এই ধাপ।
                                   শুধু নাম বদলানোয় এটা চালানোর কিছু নেই। */
                                if (newMobile != currentMobile)
                                    MobileChangeSync.sync(currentMobile, newMobile, pid, this@PatientTimelineActivity)
                            } catch (_: Exception) { }
                        }
                        mainOk
                    }
                    android.widget.Toast.makeText(
                        this@PatientTimelineActivity,
                        if (ok) "Patient updated" else "Failed — check connection",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    // Mobile is this screen's lookup key -- if it changed, reload
                    // using the NEW number so the timeline still finds this patient.
                    if (ok) { editDlg.dismiss(); load(newMobile, currentSection) }
                }
            }
        }
        editDlg.show()
        PremiumAlert.paint(editDlg)
        // 🔒 খাতার সারি B181 (TK, 30.07.2026): "Edit Patient" পপ-আপের নিজের
        // পাহারা ছিল না — লেবেল-বার্তা (Toast) এখন FieldError.validate()-এর
        // কেন্দ্রীয় ফিক্সে ঢাকা পড়ে, কিন্তু ডায়ালগের নিজের টাইটেল/হিন্টেরও
        // এই আলাদা পাহারা লাগে (পপ-আপের নিজের উইন্ডো)।
        PremiumAlert.paint(editDlg)
    }

    /**
     * 🔒 V1527 — Action menu is allowed to open only after the fresh cloud
     * Timeline has populated every field used by its conditions (bill, due,
     * paid entries, stage, registration, referring doctor, completion state).
     * The cached Timeline remains display-only; it can never decide actions.
     */
    private fun requestTakeActionMenu() {
        if (actionStateReady) {
            showTakeActionMenu()
            return
        }
        actionOpenPending = true
        android.widget.Toast.makeText(
            this,
            "Checking latest patient data…",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    // TK-REQUESTED ADDITION (2026-07-18): "Take Action" menu. Grounded in
    // the app's OWN already-approved stage pattern (FollowUpActivity's
    // entryActionMenu — Continue/Reject for Inquiry+Patient stage,
    // Continue/Incomplete for Treatment stage), not invented ideas.
    // Registration is only offered when there's genuinely no Registration
    // row yet. Print/Change Branch/Share/Referral-link deliberately not
    // added yet — no existing pattern for those to safely copy.
    private fun showTakeActionMenu() {
        // V1527 defensive guard: even an internal/legacy caller cannot open a
        // stale/partial menu before the fresh cloud state is ready.
        if (!actionStateReady) {
            actionOpenPending = true
            return
        }
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        // TK-REPORTED (2026-07-27, photo-proof): the Take Action menu showed
        // "Registration করুন" and "Edit Enquiry" for a patient who has FIVE
        // payments -- and offered NO Delete at all.
        //
        // ROOT CAUSE: this line read only currentPatientRowId, which comes
        // from the "patients" table read. That read returns an EMPTY result
        // both when someone genuinely is not registered AND when the read
        // simply failed (slow line -- TK's screenshots show 0.16 KB/s). So a
        // failed read was being treated as "this person never registered":
        // the menu fell back to Enquiry actions, and Delete disappeared
        // entirely, because the Enquiry branch needs an enquiries id (this
        // patient had none) and the registered branch was never reached.
        //
        // FIX: the follow-up stage is separate evidence of the same fact --
        // a Patient/Treatment stage row only ever exists for someone who IS
        // registered. Trusting it too means a failed patients read can no
        // longer make a real patient look like a fresh enquiry.
        val isRegistered = currentPatientRowId.isNotBlank() ||
            currentFollowupStage.equals("Patient", ignoreCase = true) ||
            currentFollowupStage.equals("Treatment", ignoreCase = true)
        val isTreatmentStage = currentFollowupStage.equals("Treatment", ignoreCase = true)

        // TK-REQUESTED REDESIGN (2026-07-20): premium look for this action
        // sheet -- a brand gradient header with the patient's name, and each
        // action as a row with a soft coloured round icon badge + bold label,
        // instead of the old plain white text list. Every action, condition
        // and onClick below is UNCHANGED -- only the visual wrapper is new.
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
        /* 📋🔒 V1322/V1323 (১১.০৯.২০২৬) → 🟢 V1489 (১৫.০৯.২০২৬, TK-সংশোধন) —
           TK জানালেন তাঁর আসল নির্দেশ ছিল Edit Patient/Return Fees/Change
           Branch/Payment এই চারটে **শুধু** ⋮-এর ভিতরেই থাকবে; V1322-এ ভুল
           বোঝাবুঝি করে নিচের পুরো তালিকাতেও (duplicate) রেখে দেওয়া হয়েছিল
           — একই কাজ দুই জায়গায় দেখাচ্ছিল, TK-র চোখে সেটাই দোষ। এখন এই চারটে
           নিচের actionRow তালিকা থেকে তুলে নেওয়া হলো, শুধু ⋮-এই থাকবে। বাকি
           সব আইটেম (Give Discount, Next Follow-up, Mark Arrived, Doctor
           Checkup..., Referring Doctor, ইত্যাদি) আগের মতোই নিচে অক্ষত। */
        val titleRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        titleRow.addView(android.widget.TextView(this).apply {
            text = "⚡  Take Action"
            textSize = 16.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val quickDots = android.widget.TextView(this).apply {
            text = "⋮"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(android.graphics.Color.argb(46, 255, 255, 255))
            }
            isClickable = true; isFocusable = true
            layoutParams = android.widget.LinearLayout.LayoutParams(dp(30), dp(30))
        }
        titleRow.addView(quickDots)
        header.addView(titleRow)
        header.addView(android.widget.TextView(this).apply {
            text = currentPatientName.ifBlank { currentMobile }
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

        fun actionRow(icon: String, label: String, tint: String, onClick: () -> Unit) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(8), dp(16), dp(8))
                isClickable = true
                isFocusable = true
                val outValue = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
                setOnClickListener { onClick() }
            }
            val badge = android.widget.TextView(this).apply {
                text = icon
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(11) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor("#22" + tint.removePrefix("#")))
                }
            }
            val text = android.widget.TextView(this).apply {
                text = label
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#10223A"))
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(badge)
            row.addView(text)
            container.addView(row)
        }

        UppercaseInputUtil.applyToAll(root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(android.widget.ScrollView(this).apply { addView(root) })
            .setNegativeButton("Close", null)
            .create()

        /* 📋🔒 V1322 → 🟢 V1489 — চারটে কাজ (Edit Patient/Enquiry · Return
           Fees · Change Branch · Payment) এখানে **একবারই** বাঁধা হলো, ⋮
           কুইক-মেনু এই lambda-গুলোই ডাকে। এখন নিচের তালিকায় এই চারটের
           actionRow আর নেই (V1489) — শুধু ⋮-এই আছে, TK-র নির্দেশ মতো। */
        val doEditPatient: () -> Unit = {
            dialog.dismiss()
            if (!isRegistered && currentEnquiryId.isNotBlank()) {
                startActivity(Intent(this, EnquiryActivity::class.java).putExtra("editEnquiryId", currentEnquiryId))
            } else showPatientHeaderEdit()
        }
        val doChangeBranch: () -> Unit = { dialog.dismiss(); showChangeBranchDialog() }
        val doReturnFees: () -> Unit = { dialog.dismiss(); showReturnFeesDialog() }
        val doPayment: () -> Unit = {
            dialog.dismiss()
            startActivity(
                Intent(this, PaymentActivity::class.java)
                    .putExtra("mobile", currentMobile)
                    .putExtra("patientRowId", preferPatientRowId)
                    .putExtra("patientCode", currentPatientCode)
            )
            finish()
        }
        // 🔴🔒 V1615 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ) — GlobalSearchActivity-র
        // কার্ডে REJECTED হলে Payment/Mark Arrived বন্ধ করা হয়েছিল (V1614),
        // কিন্তু এই একই রোগীর Full Journey খুলে Take Action মেনু থেকে ঠিক
        // ওই দুটো কাজই আগের মতো সচল থেকে যেত — একই ফাঁক, শুধু অন্য দরজা।
        // `currentFollowupStatus` আগে থেকেই এই পর্দার নিজের তথ্যেই আসে
        // (বাড়তি কোনো cloud-read নেই) — শুধু GlobalSearchActivity.fetchStages-
        // এর হুবহু একই "cancelled"/"rejected" যাচাই এখানে প্রয়োগ হলো।
        val isRejected = currentFollowupStatus.trim().lowercase().let { it == "cancelled" || it == "rejected" }
        val showRejectedBlockedToast: () -> Unit = {
            dialog.dismiss()
            android.widget.Toast.makeText(this, NoBengali.s("Rejected — restore from Draft's Reject List first"), android.widget.Toast.LENGTH_LONG).show()
        }
        val isMasterRole = NativeSession.current(this)?.role == "master"
        quickDots.setOnClickListener {
            val quickItems = mutableListOf<Pair<String, () -> Unit>>()
            quickItems.add("✏️  " + (if (isRegistered) "Edit Patient" else "Edit Enquiry Form") to doEditPatient)
            if (isRegistered) quickItems.add("💸  Return Fees" to doReturnFees)
            if (isMasterRole) quickItems.add("🔀  Change Branch" to doChangeBranch)
            if (isRegistered) quickItems.add("💳  Payment" to (if (isRejected) showRejectedBlockedToast else doPayment))
            try {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setItems(quickItems.map { it.first }.toTypedArray()) { _, which -> quickItems.getOrNull(which)?.second?.invoke() }
                    .setNegativeButton("Close", null)
                    .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
            } catch (_: Throwable) { }
        }

        // TK-REQUESTED (2026-07-22): "Full Journey" moved OUT of this menu --
        // it's now the dedicated "🧭 Full Journey" button at the top of the
        // screen (was Call), so keeping it here too would just be a
        // duplicate.
        // TK-REQUESTED (V215, 2026-07-31): "Remark" REMOVED from this Take Action
        // popup. Remark is now done ONLY from the dedicated standalone Remark
        // button/box workflow (showQuickRemarkDialog() is still reachable from the
        // post-call reminder and the dedicated Remark box — those are unchanged).
        // Order §12: no Remark option inside any Action button / Action popup.
        if (!isRegistered) {
            actionRow("🧾", "Registration করুন", "#0B2B59") {
                dialog.dismiss()
                startActivity(Intent(this@PatientTimelineActivity, RegistrationActivity::class.java).putExtra("prefillMobile", currentMobile))
                // V215 (§11.4, 31.07.2026): Follow-up/Queue → Patient Detail →
                // Register-এর পর একবার Back দিলে যেন সরাসরি আগের তালিকায় ফেরে,
                // Patient Detail-এ আটকে না থাকে — তাই এই মাঝের Detail পর্দা এখন
                // finish() হয়। ⛔ Blood Test/Edit Record-এর লক করা back (§2.13/
                // §2.14) এই action row-এ নেই, তাই ওগুলো অপরিবর্তিত।
                finish()
            }
        }
        // 🏷️ TK-APPROVED (03.09.2026, ছবি-প্রুফসহ) — "Give Discount". TK-এর
        // উদাহরণ: ২৫,০০০ বিলের রোগী ২২,০০০ দিয়ে ৩,০০০ ক্ষমা চাইল — ছাড় দিলে
        // বিল কমে ২২,০০০, বাকি ০, রোগী Complete হয়, আর ছাড়ের হিসাবটা চিরকাল
        // ইতিহাসে থাকে। TK-নির্দেশ: শুধু Master নয়, ডাক্তারসহ সবাই দিতে পারবে।
        // ⛔ বিল না থাকলে (Enquiry/Visit) ছাড় দেওয়ার কিছু নেই — তাই লুকনো।
        if (isRegistered && currentBillTotal > 0.0) {
            actionRow("🏷️", "Give Discount", "#0B7A34") { dialog.dismiss(); showDiscountDialog() }
        }
        actionRow("⏰", "Next Follow-up তারিখ", "#B8860B") { dialog.dismiss(); showQuickNextFollowDialog() }
        // TK-REQUESTED ADDITION (2026-07-20): "Mark Arrived (এসেছেন)" from the
        // Patient Card too -- marks this patient into TODAY's Chamber
        // Attendance without opening the Chamber screen. Same offline-safe
        // markArrived; board matches by last-10 digits.
        // TK-DECISION (2026-07-22): shown ONLY for an already-REGISTERED
        // patient. An Enquiry-only person can never go straight to "এসেছেন" --
        // doing a Registration is what moves them into Arrived automatically --
        // so offering Mark Arrived on a pure Enquiry made no sense and is
        // removed there.
        if (isRegistered) {
            actionRow("🏥", "Mark Arrived (এসেছেন)", if (isRejected) "#9AA5B1" else "#0E7C7B") {
                if (isRejected) showRejectedBlockedToast() else { dialog.dismiss(); markArrivedFromTimeline() }
            }
        }
        // TK-REQUESTED ADDITION (2026-07-19): "everything that can be done
        // for this patient anywhere in the app" should be reachable from
        // Take Action -- Payment and the Clinical Document set (Prescription
        // / Medicine Slip / Blood Test / Diet Chart) were only reachable
        // from other screens before. Only shown once the patient is
        // genuinely registered (a Patient/Doctor Queue row exists) -- for a
        // still-Enquiry-only person there is no patient record yet to
        // attach a payment or clinical document to.
        if (isRegistered) {
            actionRow("📋", "Doctor Checkup / Prescription / Medicine Slip / Blood Test / Diet Chart", "#7A1F3D") {
                dialog.dismiss(); showClinicalDocumentMenu()
            }
        }
        // TK-CORRECTED (2026-07-18): a Referring Doctor can be known at ANY
        // stage (Enquiry/Visit/Patient) — it's set on the Patient Card edit,
        // independent of stage. Earlier version wrongly tied this to stage;
        // now it just checks whether a referring doctor is actually known.
        if (currentRefDoctorMobile.filter { it.isDigit() }.takeLast(10).length == 10) {   // খাতার সারি B29: +91 সহ থাকলে ১২টা সংখ্যা হয়, তাই শেষ ১০টা নিতে হয়
            actionRow("🧑‍⚕️", "Referring Doctor (${currentRefDoctor.ifBlank { "Dr." }})", "#0E7C7B") {
                // 🔴 TK-REPORTED (04.08.2026, একই ক্লাসের বাগ): dialog.dismiss()
                // করা হত, তাই ডাক্তারের প্রোফাইল থেকে Back করলে সরাসরি Follow-up
                // তালিকায় চলে যেত — Take Action মেনু আর দেখা যেত না। এখন খোলা রাখা হয়।
                startActivity(Intent(this, DoctorVisitActivity::class.java).putExtra("searchMobile", currentRefDoctorMobile))
            }
        }
        // TK-REQUESTED CHANGE (2026-07-20): "Add Referral Income" now shows
        // for EVERY patient, not only when a referring doctor is already
        // saved. If none is set yet, the dialog lets staff enter the
        // referring doctor right there (and remembers it on the patient).
        // TK-DECISION (2026-07-23): "Add Referral Income" should only ever
        // show on the Patient Card (Treatment stage) -- not Enquiry, not
        // Visit Card. Overrides the 2026-07-20 "show for every patient"
        // change above.
        if (isTreatmentStage) {
            actionRow("💰", "Add Referral Income", "#B8860B") { dialog.dismiss(); showAddReferralIncomeDialog() }
        }
        // TK-REQUESTED ADDITION (2026-07-24): "Complete despite Due" -- some
        // patients leave with a genuine unpaid balance and ask to be
        // forgiven; TK wants a way to stop chasing them for calls without
        // ever touching the real Due (Reports/collections stay accurate).
        // Same Staff-request/Master-approve pattern already proven for
        // Doctor delete (DoctorVisitActivity.confirmDeleteDoctor). Only
        // relevant on Patient/Treatment stage with Due actually > 0; once
        // approved (currentCompleteApprovedBy set) this whole block is
        // skipped -- nothing left to do, Draft/Follow-up already reflect
        // it (DraftRepository.kt / FollowUpRepository.kt).
        if (isTreatmentStage && currentDue > 0.0 && currentCompleteApprovedBy.isBlank()) {
            val completeUser = NativeSession.current(this)
            if (completeUser != null) {
                if (currentCompleteRequestedBy.isNotBlank()) {
                    if (completeUser.role == "master") {
                        actionRow("✅", "Approve", "#0C9E33") { dialog.dismiss(); approveCompleteDespiteDue(completeUser) }
                        actionRow("❌", "Reject", "#7A1F3D") { dialog.dismiss(); rejectCompleteDespiteDue() }
                    } else {
                        actionRow("⏳", "Waiting for Master approval", "#8A97AB") {
                            dialog.dismiss()
                            android.widget.Toast.makeText(this, NoBengali.s("Waiting for Master approval"), android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (completeUser.role == "master") {
                        actionRow("✅", "Complete Patient", "#0C9E33") { dialog.dismiss(); approveCompleteDespiteDue(completeUser) }
                    } else {
                        actionRow("✅", "Complete Patient", "#0C9E33") { dialog.dismiss(); requestCompleteDespiteDue(completeUser) }
                    }
                }
            }
        }
        // TK-REQUESTED (2026-07-22): "Report Card" moved OUT of this menu --
        // it's now the dedicated "📋 Report Card" button at the top of the
        // screen (was WhatsApp), so keeping it here too would just be a
        // duplicate.
        // 🚨 TK-REPORTED (29.07.2026 রাত ৯.১০, দুটো ছবিসহ · খাতার সারি B97):
        // *"Reject list-এ গেছি, View করেছি — সেখানে আবার Reject / Registration
        //   Cancel, এটা থাকার মানে কি?"*
        // ⛔ রেকর্ডটা **আগেই বাতিল হয়ে গেছে** (Reject/Incomplete তালিকায় আছে),
        //    তাই আবার একই কাজের বোতাম দেখানো অর্থহীন — এখন লুকানো থাকে।
        // ⛔ স্বাভাবিক (চালু) রেকর্ডে আগের মতোই দেখা যায় — কিছু বদলায়নি।
        // ⛔ অবস্থা জানা না গেলে (ফাঁকা) আগের মতোই দেখায়, তাই কিছু হারায় না।
        // 🚨🚨 TK-REPORTED (29.07.2026 সন্ধ্যা ৬.১৮, দুটো ছবিসহ · খাতার সারি B124
        //      — SUSMITA DAS · +919635608042, চালু Enquiry কার্ড থেকে View
        //      করে Take Action-এ **Reject List নেই, Delete Enquiry আছে**)।
        //      TK: *"মেইন কার্ড থেকে আমাকে ডিলিট অপশন কেন দেখাবে? সেখানে তো
        //      আগে রিজেক্ট আসতে হতো।"*
        //
        // **আসল কারণ (কোড ধরে, আন্দাজ নয়):** উপরের `alreadyClosed` হিসাবটা
        // হত **রেকর্ডের `followups.status` দেখে**। ওই ঘরে Cancelled/Incomplete
        // লেখা থাকলে (পুরনো রেকর্ড · আধা-হয়ে-থাকা Reject · খাতার সারি B108-এর
        // পুরনো ডেটা) কার্ডটা চালু তালিকায় থাকা সত্ত্বেও Reject লুকিয়ে যেত আর
        // Delete উঠে যেত — ঠিক উল্টোটা।
        //
        // **ওষুধ:** এখন ঠিক হয় **কোথা থেকে পর্দাটা খোলা হলো** তা দেখে —
        //  · Draft-এর বাতিল তালিকা (Reject / Incomplete / Complete /
        //    Unexpected) থেকে খুললে → শুধু `🗑️ Delete`
        //  · অন্য যে কোনো জায়গা (চালু Enquiry · Visit · Patient কার্ড ·
        //    Global Search · Chamber …) থেকে খুললে → শুধু `🚫 Reject List` /
        //    `⏳ Incomplete Patient`, **ডিলিট কখনো নয়**।
        // ⛔ কাজের ফাংশন একটাও বদলায়নি (`confirmStatusChange` ·
        //    `confirmDeleteEnquiry` · `confirmDeletePatient`) — Trash ·
        //    Restore · টাকার ইতিহাস · মাস্টারের অনুমতি সব অপরিবর্তিত।
        // ⛔ খাতার সারি B97 আগের মতোই মানা হচ্ছে: বাতিল তালিকার View-তে আর
        //    "Reject / Registration Cancel" দেখায় না।
        val fromDraftClosed = intent.getBooleanExtra("fromDraftClosed", false)
        if (currentFollowupId.isNotBlank() && !fromDraftClosed) {
            // 🔒🔒 খাতার সারি B113 (TK, 29.07.2026 বিকেল ৬.৪০ — ফটো-প্রুফে পাশ):
            // *"Enquiry/Visit কার্ড থেকে ডাইরেক্ট ডিলিট করা যাবে না — এখানে
            //   Reject List লেখা থাকবে, চাপ দিলে Draft-এর Reject List-এ যাবে।
            //   Patient কার্ডে থাকবে Incomplete Patient / Complete Patient।
            //   ডিলিট করতে হলে Draft-এর View All-এ ডিলিট অপশন থাকবে।"*
            // ⛔ কাজটা **আগের সেই একই ফাংশনেই** হয় (`confirmStatusChange`) —
            //    শুধু নামটা বদলাল, তাই কোনো working flow ভাঙেনি।
            if (isTreatmentStage) {
                actionRow("⏳", "Incomplete Patient", "#B8860B") { dialog.dismiss(); confirmStatusChange(true) }
            } else {
                actionRow("🚫", "Reject List", "#7A1F3D") { dialog.dismiss(); confirmStatusChange(false) }
            }
        }
        // TK-REQUESTED ADDITION (2026-07-18): Delete, extended here from
        // Draft's Enquiry-only Delete to also cover a registered Patient —
        // same TrashHelper, same same-day+creator (or Master) permission
        // rule, same "moves to Trash Bin, never a hard delete" safety.
        val user = NativeSession.current(this)
        if (user != null) {
        // 🔒🔒 V217 (§B216, Master Fix Order §14): B98-এর Master-only ব্লক এখন
        // Same-Day বিশেষ নিয়মে বদলানো হয়েছে (confirmDeleteEnquiry/
        // confirmDeletePatient দেখুন) — Master সবসময় পারবেন, Staff আজ/
        // গতকালের নিজের ব্রাঞ্চের এন্ট্রি নিজে মুছতে পারবেন (চেম্বার বন্ধ না
        // থাকলে), পুরনো হলে Master-এর অনুমতি লাগবে। এই লেবেলটা শুধু চোখে
        // দেখানোর জন্য — cloud ছুঁয়ে চেম্বার-বন্ধ যাচাই মেনু খোলার সময় নয়,
        // আসল বোতাম চাপার সময় হয় (উপরের ফাংশন দুটোতে); তাই কালেভদ্রে
        // (চেম্বার ঠিক ওই মুহূর্তে বন্ধ হলে) লেবেল "যাবে" দেখালেও বোতাম
        // চাপলে "Master's approval needed"-তে পড়তে পারে — এটা নিরাপদ দিকেই,
        // কখনো উল্টো (না-বলা জিনিস চুপচাপ হয়ে যাওয়া) হয় না।
        val todayLocal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val yesterdayLocal = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(
            java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }.time
        )
        val entryDateForLabel = if (isRegistered) currentRegistrationDate else currentEnquiryDate
        val sameDayForLabel = entryDateForLabel.take(10).let { it == todayLocal || it == yesterdayLocal } &&
            currentBranch.isNotBlank() && currentBranch == user.branch
        val isMasterUser = DeletePermission.canDeleteNow(user) || sameDayForLabel
        // 🔒🔒 খাতার সারি B113: **ডিলিট এখন শুধু Draft-এর তালিকার রেকর্ডে** —
        // অর্থাৎ যেটা আগেই Reject / Registration Cancel / Incomplete হয়ে গেছে।
        // চালু কার্ড (Enquiry · Visit · Patient) থেকে ডিলিট সম্পূর্ণ উঠে গেল;
        // সেখানে এখন শুধু Reject List / Incomplete Patient / Complete Patient।
        // ⛔ ডিলিটের কাজটা **হুবহু আগের ফাংশনেই** হয় — Trash · Restore ·
        //    টাকার ইতিহাস · মাস্টারের অনুমতি, সব অপরিবর্তিত।
        // 🔒 খাতার সারি B124: শর্তটা এখন **status নয়, উৎস** — চালু কার্ড থেকে
        //    খুললে ডিলিট কখনো দেখাবে না, তার status যা-ই লেখা থাক।
        // 🔴🔴 TK-REPORTED (02.08.2026, দুপুর ~১১.৪৫ am): নিজে Enquiry ভরে →
        // Reject করে → Draft-এর Reject List-এ গিয়ে Delete খুঁজে পাননি।
        // **আসল কারণ (কোড দেখে, আন্দাজ নয়):** নিচের শর্তে আগে
        // `currentEnquiryId.isNotBlank()`-ও লাগত — কিন্তু `currentEnquiryId`
        // শুধু পর্দা-খোলার সময়কার একবার-পড়া তথ্য (মোবাইল-মেলানো খোঁজায়
        // মাঝে-মাঝে ফাঁকা থাকতে পারে)। অথচ Delete চাপার **আসল কাজটা**
        // (নিচের `showDeleteEnquiryDialog`) নিজে থেকেই আবার মোবাইল ধরে
        // খুঁজে নেয় (`findByMobile`) — তাই বোতাম দেখানোর শর্তটা কাজের
        // চেয়ে বেশি কড়া ছিল, ফলে সত্যিকারের Enquiry থাকা সত্ত্বেও বোতামই
        // দেখা যায়নি। **সমাধান:** `fromDraftClosed && !isRegistered`
        // যথেষ্ট — বোতাম এখন সবসময় দেখাবে, চাপলে যেই ফাংশন আগে থেকেই
        // নিরাপদে "Already deleted" সামলায়, ঠিক সেটাই কাজ করবে। ⛔ কোনো
        // ঝুঁকি নেই — সত্যিই কিছু না থাকলে নিরাপদে "Already deleted"
        // বলে Detail Screen বন্ধ হয়ে যায়, ভুল কিছু মোছে না।
        if (!fromDraftClosed) {
            // চালু রেকর্ডে ডিলিট দেখানো হবে না — কিছুই যোগ করার নেই।
        } else if (!isRegistered) {
                run {
                    val label = if (isMasterUser) "Delete Enquiry" else "Delete Enquiry (Master-এর অনুমতি লাগবে)"
                    actionRow("🗑️", label, "#D32F2F") { dialog.dismiss(); confirmDeleteEnquiry(user) }
                }
        } else if (isRegistered) {
                run {
                    // TK-DECISION (2026-07-24): Visit-stage (not yet in
                    // Treatment) uses the label "🚫 Registration Cancel"
                    // instead of "🗑️ Delete Patient" -- same underlying
                    // confirmDeletePatient() function, same Trash Bin +
                    // same permission rule + Payment history stays intact
                    // (TK explicitly confirmed all three via photo-proof
                    // Q&A) -- this is purely clearer wording for the
                    // "registered this by mistake" scenario, not a new
                    // feature. Patient/Treatment stage keeps the original
                    // "🗑️ Delete Patient" label, unchanged.
                    // 🔒 খাতার সারি B97 (TK, 29.07.2026): *"এখানে ডিলিট নেই কেন?
                    // মাস্টারের জন্য ডিলিট করার অপশন রাখতে হবে।"*
                    // ⛔ আসলে ডিলিটটা এখানেই ছিল, কিন্তু **"Registration Cancel"**
                    //    নামে (TK-এর নিজের 24.07.2026-এর সিদ্ধান্ত — "ভুল করে
                    //    রেজিস্ট্রেশন হয়ে গেছে" অবস্থার জন্য পরিষ্কার শব্দ)।
                    //    তাই ওই নামটা **স্বাভাবিক রেকর্ডে অপরিবর্তিত রাখা হলো**।
                    // ⛔ কিন্তু রেকর্ডটা **আগেই বাতিল হয়ে গেলে** "Registration
                    //    Cancel" কথাটার আর কোনো মানে থাকে না — তখন সোজা
                    //    **"🗑️ Delete"** দেখায়। কাজ একই (`confirmDeletePatient`),
                    //    শুধু নামটা পরিষ্কার।
                    // 🔒 খাতার সারি B98: মাস্টার না হলে লেখাটাই বলে দেয় যে
                    // অনুমতি লাগবে — চাপলে মাস্টারের ঘন্টায় অনুরোধ যায়।
                    // 🔒 খাতার সারি B113: এই অংশে এখন **শুধু বাতিল হয়ে যাওয়া**
                    // রেকর্ডই আসে, তাই "Registration Cancel" নামটার আর দরকার নেই —
                    // সোজা Delete। কাজ একই (`confirmDeletePatient`), কিছু ভাঙেনি।
                    val suffix = if (isMasterUser) "" else " (Master-এর অনুমতি লাগবে)"
                    val delLabel = if (isTreatmentStage) "Delete Patient$suffix" else "Delete$suffix"
                    actionRow("🗑️", delLabel, "#D32F2F") { dialog.dismiss(); confirmDeletePatient(user, isTreatmentStage) }
                }
            }
        }
        // ────────────────────────────────────────────────────────────────
        // 🔒🔒 TK-ORDER (29.07.2026 রাত ১১.৫৫): "SMS/WhatsApp বার্তাগুলো একবারই
        // শো করে, পরে পাঠাতে চাইলে কোনো জায়গা নেই — Enquiry/Visit/Patient
        // প্রতিটা কার্ডের View All → Action-এ এই বার্তাগুলো পাঠানোর অপশন
        // চাই।"
        //
        // এই বার্তাগুলো (PatientMessage.Kind) আগে থেকেই বানানো ও পরীক্ষিত —
        // FollowUpActivity-র নিজের কার্ড-ভিত্তিক "Take Action" (entryActionMenu,
        // ট্রিপল-ট্যাপে খোলে) মেনুতে এই একই ৫টা (Due Reminder · Send Receipt ·
        // Visit Reminder · Send Document · Treatment Complete) আগে থেকেই আছে —
        // কিন্তু View All-এর এই "⚡ Take Action"-এ (যেটা আসলে সহজে চোখে পড়ে)
        // একটাও ছিল না। এখন এখানেও ঠিক সেই একই শর্ত ও ফাংশন পুনর্ব্যবহার করে
        // যোগ করা হলো, সঙ্গে Registration/Bill রিসেন্ড (যেগুলো আগে কোথাও
        // resend করা যেত না, শুধু সেভের মুহূর্তেই একবার দেখাত)।
        // ⛔ কোনো নতুন বার্তার লেখা বানানো হয়নি — যা আগে থেকে আছে তাই।
        // ⛔ টাকা/তালিকা/রোগীর অবস্থা কিছুই বদলায় না — শুধু বার্তার বাক্স খোলে।
        run {
            // TK-ভাষায়: Enquiry কার্ড = এখানে stage "Inquiry" (এখনো রেজিস্টার
            // হননি) · Visit কার্ড = internal stage "Patient" · Patient কার্ড =
            // internal stage "Treatment" (অ্যাপের পুরনো নামকরণ, UI-তে যেটা
            // "Patient" ট্যাব)।
            if (!isRegistered) {
                actionRow("📨", "Enquiry বার্তা পাঠান", "#1067D8") {
                    dialog.dismiss()
                    // 🔒 ENQUIRY_WHATSAPP_MESSAGE_FINAL_LOCK_2026-07-31_0950_IST
                    PatientMessage.showEnquiryMessage(
                        activity = this, branch = currentBranch, name = currentPatientName,
                        mobile = currentMobile, disease = currentDisease
                    )
                }
            }
            // "আপনার এপয়েন্টমেন্ট বুক হয়েছে / পরের বার আসার তারিখ" — যে কোনো
            // স্টেজেই দেখানো হয় যদি Next Follow-up তারিখ জানা থাকে (ঠিক
            // entryActionMenu-এর নিয়মের মতোই — স্টেজ-নির্ভর নয়, তারিখ থাকলেই)।
            // তারিখটা এই পর্দায় আগে থেকে জমা থাকে না, তাই বোতাম চাপলেই একটা
            // হালকা lookup (শুধু ১ ঘর, ১ সারি) হয় — রোজকার লোডে বাড়তি কিছু নয়।
            if (currentFollowupId.isNotBlank()) {
                actionRow("⏰", "আসার তারিখ মনে করিয়ে দিন", "#B8860B") {
                    dialog.dismiss()
                    lifecycleScope.launch {
                        val nf = withContext(Dispatchers.IO) {
                            try {
                                SupabaseClient.fetchListSlim("followups", "id=eq.$currentFollowupId", 1, "nextFollow")
                                    .optJSONObject(0)?.s("nextFollow").orEmpty()
                            } catch (_: Throwable) { "" }
                        }
                        if (nf.isBlank()) {
                            android.widget.Toast.makeText(this@PatientTimelineActivity, NoBengali.s("কোনো আসার তারিখ ঠিক করা নেই"), android.widget.Toast.LENGTH_SHORT).show()
                        } else if ((FollowUpModel.daysUntil(nf) ?: 0) < 0) {
                            // 🔴🔴 TK-REPORTED (01.08.2026 সন্ধ্যা, ছবিসহ): "24.07.2026" পার হয়ে
                            // যাওয়া তারিখ দিয়েও "Appointment Confirmed"/"NEXT VISIT SCHEDULED"
                            // বার্তা পাঠানো যাচ্ছিল — এই হ্যান্ডলারে কখনোই কোনো তারিখ-পাহারা
                            // ছিল না (নতুন First Visit বার্তা ও পুরনো VISIT_DATE দুটোই একই
                            // ফাঁকা জায়গা দিয়ে যেত)। এখন পার-হয়ে-যাওয়া তারিখ ধরা পড়লে
                            // (FollowUpModel.daysUntil() — প্রজেক্টে আগে থেকেই প্রমাণিত,
                            // অন্য জায়গায় Overdue দেখাতে ব্যবহার হয়) কোনো বার্তাই পাঠানো
                            // হয় না — দুই শাখাতেই (Enquiry ও Visit/Patient) সমানভাবে আটকায়।
                            android.widget.Toast.makeText(
                                this@PatientTimelineActivity,
                                NoBengali.s("এই আসার তারিখ (${FollowUpModel.displayDate(nf)}) পার হয়ে গেছে — আগে নতুন তারিখ ঠিক করুন"),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else if (!isRegistered) {
                            // 🔒 FIRST_VISIT_APPOINTMENT_MESSAGE_FINAL_LOCK_2026-08-01_1218_IST
                            // (TK verified live-test): Enquiry ব্যক্তি এখনো ক্লিনিকে আসেননি —
                            // এখানে কখনোই "NEXT VISIT SCHEDULED" নয়। ভাষা-বাছাই সহ (একটি ভাষা)
                            // Final-Locked "FIRST VISIT APPOINTMENT CONFIRMED" বার্তা যায়।
                            // ⛔ নিচের Visit/Patient শাখার VISIT_DATE একটুও বদলায়নি।
                            PatientMessage.showFirstVisitAppointment(
                                activity = this@PatientTimelineActivity, branch = currentBranch,
                                name = currentPatientName, mobile = currentMobile,
                                patientId = currentPatientCode, dateText = FollowUpModel.displayDate(nf)
                            )
                        } else {
                            // Visit/Patient (আগে ক্লিনিকে এসেছেন) — আগের মতোই অপরিবর্তিত।
                            PatientMessage.show(
                                activity = this@PatientTimelineActivity, branch = currentBranch, name = currentPatientName,
                                mobile = currentMobile, patientId = currentPatientCode,
                                kind = PatientMessage.Kind.VISIT_DATE,
                                dateText = FollowUpModel.displayDate(nf)
                            )
                        }
                    }
                }
            }
            if (isRegistered) {
                actionRow("🧾", "Registration বার্তা পাঠান", "#0B2B59") {
                    dialog.dismiss()
                    // 🔵🔒 TK-রিপোর্ট (09.08.2026): সেভের সময় বার্তা-বাক্সে "📄 A4 Registration
                    // — Send / Print" বোতাম আসত, কিন্তু পরে Timeline/View থেকে এই বার্তা খুললে
                    // বোতাম আসত না (কারণ a4Patient পাঠানো হতো না)। এখন এই লোডে আগে থেকেই থাকা
                    // রোগীর ঘরগুলো দিয়ে (নতুন কোনো ক্লাউড-কল নয়) RegistrationActivity-র হুবহু একই
                    // a4Patient JSON বানিয়ে পাঠানো হচ্ছে — তাই এখানেও A4 রসিদ Print/Share(PDF) খুলবে।
                    // ⛔ বার্তার লেখা/সেভ/অন্য কিছু বদলায়নি — শুধু বোতামটা আসবে।
                    val a4Reg = org.json.JSONObject().apply {
                        put("name", currentPatientName)
                        put("patientId", currentPatientCode)
                        put("mobile", currentMobile)
                        put("branch", currentBranch)
                        put("disease", currentDisease)
                        put("address", currentPatientAddress)
                        put("age", currentPatientAge)
                        put("sex", currentPatientSex)
                        // 🔴🔒 V506 (TK-নির্দেশ): কাগজেও বার্তার হুবহু একই ধরন।
                        //    আগে কাঁচা `2026-08-21` যেত ও সময় যেতই না।
                        put("registrationDate", tkSlashDate(currentRegistrationDate))
                        put("time", tkClockTime(currentRegistrationCreatedAt))
                        put("refBy", "")
                    }
                    /* 🔴🔒 V505 (TK-নির্দেশ ২১.০৮.২০২৬): *"পেশেন্ট আইডি তার নিচে
                       রেজিস্ট্রেশনের তারিখ এবং সময় থাকবে।"*
                       আগে এই পথে (রোগীর পাতা থেকে আবার পাঠানো) তারিখই যেত না —
                       `dateText` পাঠানোই হতো না, তাই লাইনটা বাদ পড়ত। TK-এর
                       ফটোতে ঠিক সেটাই ধরা পড়েছে।
                       এখন **রেকর্ডে জমা থাকা আসল** তারিখ ও সময় যায় (পাঠানোর
                       সময় নয়) — TK-এর বাছাই অনুযায়ী।
                       ⛔ সময় জমা না থাকলে শুধু তারিখ যায়, আন্দাজে কিছু বসে না। */
                    val regDate = tkSlashDate(currentRegistrationDate)
                    val regTime = tkClockTime(currentRegistrationCreatedAt)
                    PatientMessage.show(
                        activity = this, branch = currentBranch, name = currentPatientName,
                        mobile = currentMobile, patientId = currentPatientCode,
                        kind = PatientMessage.Kind.REGISTRATION,
                        dateText = regDate, timeText = regTime,
                        a4Patient = a4Reg
                    )
                }
            }
            if (isRegistered && currentBillTotal > 0.0) {
                val paidNow = if (currentDue >= 0.0) (currentBillTotal - currentDue).coerceAtLeast(0.0) else 0.0
                actionRow("💵", "Bill বার্তা পাঠান", "#7A1F3D") {
                    dialog.dismiss()
                    // 🔒 TK-এর নির্দেশ (01.08.2026, স্ক্রিনশটসহ): "আজকে কত
                    // পেমেন্ট করেছে সেটাও দেখতে চাইছি" — আজকের তারিখের
                    // পেমেন্ট-সারিগুলো যোগ করা হলো, ঠিক সেই একই নিয়মে যেভাবে
                    // Total Paid/Due হিসাব হয় (visit_fee/attendance_mark বাদ,
                    // paidEffect — refund থাকলে তার সাইনও ঠিক থাকে)।
                    val todayIso = FollowUpModel.today()
                    val treatmentRows = currentEntries.filter {
                        it.paymentId != null && it.payType != "visit_fee" && it.payType != "attendance_mark"
                    }
                    val paidToday = treatmentRows.filter { it.date == todayIso }.sumOf { it.paidEffect }
                    // 🔒 TK-এর নির্দেশ (01.08.2026, দ্বিতীয় দফা): "সর্বমোট বিল"
                    // শুধু প্রথমবার (বিল তৈরি/প্রথম Advance) দেখাবে — অর্থাৎ
                    // আজই ওই রোগীর প্রথম পেমেন্টের দিন হলে তবেই। বাকি সব
                    // দিনের বার্তায় (২য়/৩য়... পেমেন্ট) সর্বমোট বিল বাদ, শুধু
                    // Total Paid ও Amount Due।
                    val treatmentDays = treatmentRows.map { it.date.take(10) }.filter { it.isNotBlank() }.distinct().sorted()
                    val isFirstPaymentDay = treatmentDays.firstOrNull() == todayIso
                    // আজকের সবচেয়ে সাম্প্রতিক পেমেন্টের আসল সময় — তারিখ/বার/
                    // সময় তিনটেই এখান থেকে বের হয় (TK-এর কড়া নির্দেশ, ভুল
                    // করা যাবে না)।
                    val todaysLatest = treatmentRows.filter { it.date == todayIso }.maxByOrNull { it.callTime }
                    val paidTodayAt = todaysLatest?.callTime ?: ""
                    // 🔒 TK-এর নির্দেশ (01.08.2026): "টাকাটা কে রিসিভ করেছে
                    // তার বিবরণ থাকবে" — সেই সারির `by` (স্টাফের নাম/কোড,
                    // TimelineRepository-তে আগে থেকেই বসানো, কাঁচা নম্বর নয়)।
                    val receivedByName = todaysLatest?.by ?: ""
                    PatientMessage.show(
                        activity = this, branch = currentBranch, name = currentPatientName,
                        mobile = currentMobile, patientId = currentPatientCode,
                        kind = PatientMessage.Kind.BILL,
                        bill = currentBillTotal, paid = paidNow,
                        amount = paidToday.coerceAtLeast(0.0),
                        showBillTotal = isFirstPaymentDay,
                        paidTodayAtIso = paidTodayAt,
                        receivedBy = receivedByName
                    )
                }
                if (currentDue > 0.0) {
                    actionRow("💰", "Due Reminder", "#D32F2F") {
                        dialog.dismiss()
                        PatientMessage.show(
                            activity = this, branch = currentBranch, name = currentPatientName,
                            mobile = currentMobile, patientId = currentPatientCode,
                            kind = PatientMessage.Kind.DUE_REMINDER,
                            bill = currentBillTotal, paid = paidNow
                        )
                    }
                }
                if (paidNow > 0.0) {
                    actionRow("🧾", "Send Receipt", "#0C9E33") {
                        dialog.dismiss()
                        // TK-APPROVED ADDITION (31.07.2026): Receipt Number
                        // এই পেশেন্টের সবচেয়ে সাম্প্রতিক Saved payment
                        // row-এর `id` থেকে (আন্দাজ/Placeholder নয়); না পেলে
                        // লাইনটাই বাদ যায়, Receipt পাঠানো আটকায় না।
                        lifecycleScope.launch {
                            val rn = try {
                                withContext(Dispatchers.IO) {
                                    val rows = SupabaseClient.fetchList("payments", "patientId=eq.$currentPatientCode", 1)
                                    if (rows.length() > 0) rows.getJSONObject(0).optString("id", "") else ""
                                }
                            } catch (_: Throwable) { "" }
                            PatientMessage.show(
                                activity = this@PatientTimelineActivity, branch = currentBranch, name = currentPatientName,
                                mobile = currentMobile, patientId = currentPatientCode,
                                kind = PatientMessage.Kind.RECEIPT,
                                bill = currentBillTotal, paid = paidNow,
                                dateText = FollowUpModel.displayDate(FollowUpModel.today()),
                                whatsAppOnly = true, receiptNumber = rn
                            )
                        }
                    }
                }
            }
            if (isTreatmentStage) {
                actionRow("🎉", "Treatment Complete বার্তা", "#0B2B59") {
                    dialog.dismiss()
                    PatientMessage.show(
                        activity = this, branch = currentBranch, name = currentPatientName,
                        mobile = currentMobile, patientId = currentPatientCode,
                        kind = PatientMessage.Kind.TREATMENT_DONE
                    )
                }
            }
        }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
        // 🔒 খাতার সারি B181 (TK, 30.07.2026): "Take Action" মেনুর নিজের
        // পাহারা ছিল না।
        PremiumAlert.paint(dialog)
    }

    /** 🟢🔒 V627 (২৪.০৮.২০২৬, TK-নির্দেশ) — `showClinicalDocumentMenu()`-এর
     *  `RoleSession.applyFrom(...)` অংশটা আলাদা ফাংশনে বার করা হলো, যাতে
     *  "Checkup History" বোতাম চেকআপ না-থাকা রোগীর ক্ষেত্রে সরাসরি
     *  `DoctorCheckupActivity` খুলতে একই প্রমাণিত সেটআপ পুনর্ব্যবহার করতে
     *  পারে — দ্বিতীয়বার একই কোড লেখা লাগেনি, তাই দুই জায়গায় কখনো আলাদা
     *  হয়ে যাওয়ার ঝুঁকি নেই।
     */
    private fun prepareClinicalRoleSession() {
        val user = NativeSession.current(this)
        val roleStr = if (user?.role?.equals("doctor", true) == true) "DOCTOR" else "STAFF"
        com.tkbiswas.pilesclinic.clinical.RoleSession.applyFrom(
            roleStr,
            currentPatientName,
            currentPatientRowId.ifBlank { currentFollowupId },
            currentBranch,
            currentMobile,
            currentPatientAddress, currentPatientAge, currentPatientSex,
            currentDisease,
            patientDisplayId = currentPatientCode
        )
    }

    /** TK-REQUESTED ADDITION (2026-07-19): opens the same Prescription /
     *  Medicine Slip / Blood Test / Diet Chart choice already used from the
     *  Follow-up card's "Prescription" tag (FollowUpActivity.openClinicalMenu)
     *  -- same targets, same RoleSession hand-off, just reachable from Take
     *  Action here too so nothing requires leaving this screen. */
    private fun showClinicalDocumentMenu() {
        prepareClinicalRoleSession()
        val options = arrayOf("🩺 Doctor Checkup", "📝 Prescription", "💊 Medicine Slip", "🩸 Blood Test", "🥗 Diet Chart")
        val targets = arrayOf(
            com.tkbiswas.pilesclinic.clinical.DoctorCheckupActivity::class.java,
            com.tkbiswas.pilesclinic.clinical.PrescriptionActivity::class.java,
            com.tkbiswas.pilesclinic.clinical.MedicineSlipActivity::class.java,
            com.tkbiswas.pilesclinic.clinical.InvestigationAdviceActivity::class.java,
            com.tkbiswas.pilesclinic.clinical.DietChartActivity::class.java
        )
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Clinical Document — ${currentPatientName.ifBlank { currentMobile }}"))
            .setItems(options) { _, which -> startActivity(Intent(this, targets[which])) }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** TK-REQUESTED ADDITION (2026-07-19): "Add Referral Income" from inside
     *  Take Action -- reuses DoctorVisitRepository's existing save logic
     *  (same one DoctorVisitActivity's own Add Referral Income uses), with
     *  the referring doctor and this patient already known here so only
     *  Amount + Paid/Unpaid need to be entered. */
    private fun markArrivedFromTimeline() {
        val digits = currentMobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            android.widget.Toast.makeText(this, "No valid 10-digit mobile to mark", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val staffMobile = NativeSession.current(this)?.mobile ?: ""
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Mark Arrived?"))
            .setMessage(NoBengali.s("Mark ${currentPatientName.ifBlank { digits }} as Arrived (এসেছেন) in today's chamber?"))
            .setPositiveButton("Yes, Arrived") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            ChamberAttendanceRepository.markArrived(this@PatientTimelineActivity, "+91$digits", currentPatientName, currentBranch, staffMobile)
                            true
                        } catch (_: Throwable) { false }
                    }
                    android.widget.Toast.makeText(this@PatientTimelineActivity, if (ok) "Marked Arrived ✅" else "Could not mark — please retry", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** TK-DECISION (2026-07-22): manual "আসার কথা" for an ENQUIRY. Staff pick
     *  the date the patient promised to come; it writes ONE chamber_expected
     *  entry (deterministic id -> re-picking simply moves the date, never a
     *  duplicate). Same offline-safe markExpected the Chamber screen uses. */
    private fun pickExpectedDateForEnquiry() {
        val digits = currentMobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            android.widget.Toast.makeText(this, "No valid 10-digit mobile", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        // TK-REQUESTED (2026-07-27): the old middle menu ("তারিখ দিন / বদলান"
        // + "বাতিল করুন") is gone. Tapping আসার কথা now goes STRAIGHT to the
        // calendar when this person has no date yet. If a date already
        // exists, the staff is first reminded WHICH date it is and asked
        // whether to change it or cancel it -- so a date can never be
        // overwritten by accident.
        //
        // If the date cannot be read at all (bad line), the answer is null
        // and the calendar opens exactly as it does today: the work is never
        // blocked, only the reminder is skipped that once.
        lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) {
                try { ChamberAttendanceRepository.findExpectedDate(this@PatientTimelineActivity, digits) }
                catch (_: Throwable) { null }
            }
            if (existing.isNullOrBlank()) {
                pickExpectedDatePicker(digits)
            } else {
                showExpectedExistsDialog(digits, existing)
            }
        }
    }

    /** TK-APPROVED (2026-07-27, via photo proof): "তারিখ বদলান" and
     *  "বাতিল করুন" sit SIDE BY SIDE, with "Close" on its own line beneath
     *  them. Built as a small custom view because a normal dialog stacks its
     *  buttons in the order Android chooses, which is what produced the
     *  crooked layout TK photographed. */
    private fun showExpectedExistsDialog(digits: String, existingIso: String) {
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dpx(18), dpx(14), dpx(18), dpx(14))
        }
        box.addView(android.widget.TextView(this).apply {
            text = NoBengali.s("এই রোগীর আসার কথা ইতিমধ্যে দেওয়া হয়েছে —")
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#1D2939"))
        })
        box.addView(android.widget.TextView(this).apply {
            text = displayDate(existingIso)
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#B8791A"))
            val p = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            p.topMargin = dpx(8)
            layoutParams = p
        })

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "⏰ Expected visit already set"))
            .setView(box)
            .setCancelable(true)
            .create()

        fun actionButton(label: String, colorHex: String, weight: Float, endGap: Boolean, onTap: () -> Unit) =
            android.widget.TextView(this).apply {
                text = label
                textSize = 14.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.WHITE)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 10f * resources.displayMetrics.density
                    setColor(android.graphics.Color.parseColor(colorHex))
                }
                val p = android.widget.LinearLayout.LayoutParams(0, dpx(46), weight)
                if (endGap) p.marginEnd = dpx(10)
                layoutParams = p
                setOnClickListener { dialog.dismiss(); onTap() }
            }

        val pairRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            val p = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            p.topMargin = dpx(16)
            layoutParams = p
        }
        pairRow.addView(actionButton("তারিখ বদলান", "#B8791A", 1f, true) { pickExpectedDatePicker(digits) })
        pairRow.addView(actionButton("বাতিল করুন", "#A93226", 1f, false) { cancelExpectedFromTimeline(digits) })
        box.addView(pairRow)
        box.addView(android.widget.TextView(this).apply {
            text = "Close"
            textSize = 14.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#42526B"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 10f * resources.displayMetrics.density
                setColor(android.graphics.Color.parseColor("#F1F4F8"))
            }
            val p = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpx(44)
            )
            p.topMargin = dpx(10)
            layoutParams = p
            setOnClickListener { dialog.dismiss() }
        })
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
        // Same rounded white popup shell every other dialog in this app uses.
        // paint() is fully guarded internally and simply does nothing about
        // the standard buttons this dialog deliberately does not have.
        PremiumAlert.paint(dialog)
    }

    private fun cancelExpectedFromTimeline(digits: String) {
        // TK-REQUESTED (2026-07-27): second reason reworded, and "অন্য কারণ"
        // now makes the staff TYPE why -- a blank reason is refused, so there
        // is always a real trail of why an আসার কথা was cancelled.
        val reasons = arrayOf("আসতে পারবেন না", "আসার তারিখ পরিবর্তন করেছেন", "অন্য কারণ")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "আসার কথা বাতিল"))
            .setItems(reasons) { _, which ->
                if (which == reasons.size - 1) askOtherCancelReason(digits)
                else doCancelExpected(digits, reasons[which])
            }
            .setNegativeButton("Close", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun askOtherCancelReason(digits: String) {
        val input = android.widget.EditText(this).apply {
            hint = NoBengali.s("এখানে লিখুন…")
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dpx(12); setPadding(p, p, p, p)
            minLines = 2
            gravity = android.view.Gravity.TOP
        }
        val wrap = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dpx(18), dpx(12), dpx(18), 0)
            addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                text = NoBengali.s("কী কারণে বাতিল করছেন, লিখুন")
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#1D2939"))
            })
            addView(input)
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "অন্য কারণ"))
            .setView(wrap)
            .setPositiveButton("Save", null)   // set below so a blank entry does not close it
            .setNegativeButton("Close", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val typed = input.text.toString().trim()
                if (typed.isEmpty()) {
                    android.widget.Toast.makeText(this, NoBengali.s("কারণ লিখুন"), android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    dialog.dismiss()
                    doCancelExpected(digits, typed)
                }
            }
        }
        dialog.show()
        PremiumAlert.paint(dialog)
    }

    private fun doCancelExpected(digits: String, reason: String) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    ChamberAttendanceRepository.cancelExpected(this@PatientTimelineActivity, "+91$digits")
                    if (currentFollowupId.isNotBlank()) {
                        val by = NativeSession.current(this@PatientTimelineActivity)?.let { it.name.ifBlank { it.mobile } } ?: ""
                        FollowUpRepository(this@PatientTimelineActivity).updateRemark(resolveFollowUpIdHere(), "আসার কথা বাতিল: $reason", by)
                    }
                    true
                } catch (_: Throwable) { false }
            }
            android.widget.Toast.makeText(this@PatientTimelineActivity, NoBengali.s(if (ok) "বাতিল হলো ✅" else "Failed — retry"), android.widget.Toast.LENGTH_SHORT).show()
            if (ok) load(currentMobile, currentSection)
        }
    }

    private fun pickExpectedDatePicker(digits: String) {
        val staffMobile = NativeSession.current(this)?.mobile ?: ""
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(this, { _, y, m, dayOfMonth ->
            val picked = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, dayOfMonth)
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        ChamberAttendanceRepository.markExpected(this@PatientTimelineActivity, "+91$digits", currentPatientName, currentBranch, picked, staffMobile)
                        true
                    } catch (_: Throwable) { false }
                }
                android.widget.Toast.makeText(this@PatientTimelineActivity, NoBengali.s(if (ok) "আসার কথায় যোগ হলো ✅" else "Could not save — please retry"), android.widget.Toast.LENGTH_SHORT).show()
            }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
            .apply { datePicker.minDate = cal.timeInMillis }
            .show()
    }

    /** V467 (20.08.2026) — পুরনো Save-লজিকের হুবহু একই অংশ (patient-এর সাথে
     *  ডাক্তার মনে রাখা + referral entry সেভ), শুধু আলাদা ফাংশনে বার করা —
     *  যাতে "ডাক্তার আগে থেকেই আছে" আর "নতুন RMP তৈরির পরে" — দুই পথ থেকেই
     *  ডাকা যায়, একই যুক্তি, কোনো ডুপ্লিকেট কোড ছাড়াই। */
    private fun saveReferralAfterDoctorKnown(repo: DoctorVisitRepository, docId: String, refName: String, refMobile: String, amt: Double, status: String): Boolean {
        val storedMobile = currentRefDoctorMobile.filter { it.isDigit() }.takeLast(10)
        if (currentPatientRowId.isNotBlank() && (refMobile != storedMobile || refName != currentRefDoctor)) {
            try {
                SupabaseClient.updateById("patients", currentPatientRowId,
                    org.json.JSONObject()
                        .put("refDoctor", refName)
                        .put("refDoctorMobile", refMobile)
                        .put("refBy", "Dr. Visit"))
            } catch (_: Exception) { }
        }
        return repo.addReferralEntry(docId, currentPatientName.ifBlank { currentMobile }, currentMobile, amt, status, applicationContext)
    }

    private fun showAddReferralIncomeDialog() {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(6), dp(20), dp(4))
        }
        box.addView(android.widget.TextView(this).apply {
            text = "Referring doctor"
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#7A8699"))
            setPadding(0, 0, 0, dp(4))
        })
        // TK-REQUESTED CHANGE (2026-07-20): editable so a referring doctor can
        // be set here even when the patient had none saved yet. Pre-filled
        // when one is already known. On Save it's remembered on the patient.
        /* 🟢🔒 B672 (15.08.2026, TK-এর সংশোধিত নির্দেশ): *"নামের উপর যখন টাইপ করবে
           তখনই যেন চলে আসে — মোবাইল নাম্বার দে, একবার নাম দে, আর জায়গার নাম দে।"*
           তাই আলাদা বোতাম নয় — এই ঘরে টাইপ করতে করতেই নিচে মিলে-যাওয়া ডাক্তারের
           তালিকা নামবে। নাম · মোবাইল · জায়গা · ব্রাঞ্চ — চারটের যেকোনোটা দিয়েই।
           বেছে নিলে নাম ও নম্বর দুটোই বসে যায়।
           ⛔ হাতে লেখা আগের মতোই চলবে — তালিকায় না থাকলে নিজের মতো লিখে দিলেই হবে।
           ⚡ তালিকাটা ফোনে জমানো ঘর থেকে আসে — **Supabase-এ একটাও অনুরোধ যায় না**। */
        val refNameInput = android.widget.AutoCompleteTextView(this).apply {
            setText(currentRefDoctor); hint = "Doctor Name / mobile / area"
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            threshold = 1                       // এক অক্ষর টাইপ করলেই দেখাতে শুরু করবে
            setSingleLine(true)
        }
        val refMobileInput = android.widget.EditText(this).apply {
            setText(currentRefDoctorMobile); hint = "Doctor Mobile"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        /* 🔒🔒 V1414 (১৩.০৯.২০২৬, TK-নির্দেশ, ডেমো পাশ — খাতার সারি ৫১৬):
           TK: *"বসে যখন গেছে অটোমেটিক... মনে হলো যে এখনই আমি করলাম"* — রোগীর
           রেজিস্ট্রেশনে যে ডাক্তারের নাম আগে থেকেই ছিল, সেটা এই ঘরে এসে বসত ঠিকই
           (B672, ২০.০৭.২০২৬-এর নিয়ম অপরিবর্তিত), কিন্তু দেখতে ঠিক নিজে-টাইপ-করা
           লেখার মতোই ছিল। এখন আগে থেকে বসানো নাম/নম্বর হালকা রঙে + 🔒 আইকন সহ
           দেখায়; ঘরে চাপ দিলেই (ফোকাস পেলেই) সঙ্গে সঙ্গে সাধারণ রঙে বদলে যায় ও
           সম্পাদনযোগ্য থাকে — এক অক্ষরও লক নয়, শুধু "এটা আগে থেকে বসানো" বোঝানো। */
        val refNormalColor = refNameInput.currentTextColor
        val refMobileNormalColor = refMobileInput.currentTextColor
        val refMutedColor = android.graphics.Color.parseColor("#94A3B8")
        var refNameLocked = currentRefDoctor.isNotBlank()
        var refMobileLocked = currentRefDoctorMobile.isNotBlank()
        fun lockIcon(): android.widget.TextView = android.widget.TextView(this).apply {
            text = "🔒"; textSize = 13f
            setPadding(dp(12), 0, 0, 0)
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL }
        }
        val refNameLockIcon = lockIcon()
        val refMobileLockIcon = lockIcon()
        fun applyNameLockStyle() {
            if (refNameLocked) {
                refNameInput.setTextColor(refMutedColor)
                refNameInput.setPadding(dp(34), dp(12), dp(14), dp(12))
                refNameLockIcon.visibility = View.VISIBLE
            } else {
                refNameInput.setTextColor(refNormalColor)
                refNameInput.setPadding(dp(14), dp(12), dp(14), dp(12))
                refNameLockIcon.visibility = View.GONE
            }
        }
        fun applyMobileLockStyle() {
            if (refMobileLocked) {
                refMobileInput.setTextColor(refMutedColor)
                refMobileInput.setPadding(dp(34), dp(12), dp(14), dp(12))
                refMobileLockIcon.visibility = View.VISIBLE
            } else {
                refMobileInput.setTextColor(refMobileNormalColor)
                refMobileInput.setPadding(dp(14), dp(12), dp(14), dp(12))
                refMobileLockIcon.visibility = View.GONE
            }
        }
        applyNameLockStyle(); applyMobileLockStyle()
        refNameInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus && refNameLocked) { refNameLocked = false; applyNameLockStyle() } }
        refMobileInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus && refMobileLocked) { refMobileLocked = false; applyMobileLockStyle() } }
        val refNameFrame = android.widget.FrameLayout(this).apply { addView(refNameInput); addView(refNameLockIcon) }
        val refMobileFrame = android.widget.FrameLayout(this).apply { addView(refMobileInput); addView(refMobileLockIcon) }
        box.addView(refNameFrame, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })
        box.addView(refMobileFrame, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
        /* ═══════════════════════════════════════════════════════════════════
           🟢🔒 B673 (15.08.2026, TK-অনুমোদিত) — **টাকার অঙ্ক না লিখে % দিয়েও দেওয়া যাবে**।
           TK: *"এমাউন্টের বদলে যদি পার্সেন্টেজ করা হয় সেটা তো আপনি এখানে করলেন না।"*

           **কিসের উপরে % বসে (TK-কে বুঝিয়ে বলে অনুমোদিত):** রোগী **এ পর্যন্ত যত টাকা
           দিয়েছেন** তার উপরে — কারণ অ্যাপের নিজের Commission হিসাবও ঠিক এই নিয়মেই চলে
           (`RmpCommissionModel.kt:54` — `eligible = min(collection, bill)`)। আলাদা নিয়ম
           করলে একই ডাক্তারের দুই পর্দায় দুই সংখ্যা দেখাত।

           ⚡ **Free plan:** হিসাবের দুটো সংখ্যাই (`currentBillTotal`, `currentDue`) এই
              পর্দায় **আগে থেকেই আছে** — % হিসাব করতে **একটাও ক্লাউড-অনুরোধ লাগে না**।
           ⛔ ডিফল্ট আগের মতোই **₹ Amount** — কেউ কিছু না ছুঁলে আচরণ হুবহু আগের মতো।
           ⛔ সেভ হওয়ার সময় শেষ পর্যন্ত **টাকার অঙ্কই** জমা হয় (পুরনো সেভ-পথ অপরিবর্তিত),
              তাই পুরনো সব রেকর্ড/রিপোর্ট আগের মতোই পড়া যাবে।
           ═══════════════════════════════════════════════════════════════════ */
        var amtMode = "AMOUNT"
        val paidSoFar = if (currentDue >= 0.0)
            (currentBillTotal - currentDue).coerceAtLeast(0.0) else 0.0
        val commissionBase = minOf(paidSoFar, currentBillTotal).coerceAtLeast(0.0)
        fun money2(v: Double): Double = Math.round(v * 100.0) / 100.0

        val amountInput = android.widget.EditText(this).apply {
            hint = "Amount"
            inputType = android.text.InputType.TYPE_CLASS_TEXT; keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val calcInfo = android.widget.TextView(this).apply {
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#147A45"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(4), dp(2), dp(4), dp(2))
            visibility = View.GONE
        }
        fun refreshCalc() {
            if (amtMode != "PERCENT") { calcInfo.visibility = View.GONE; return }
            calcInfo.visibility = View.VISIBLE
            val pct = amountInput.text.toString().trim().toDoubleOrNull()
            calcInfo.text = when {
                commissionBase <= 0.0 ->
                    "⚠️ Patient has paid nothing yet — % cannot be used, type an Amount"
                pct == null || pct <= 0.0 ->
                    "Patient paid ₹${money2(commissionBase)} — type the % you want to give"
                pct > 100.0 ->
                    "⚠️ Percent cannot be more than 100"
                else ->
                    "₹${money2(commissionBase)} (patient paid) × ${money2(pct)}% = ₹${money2(commissionBase * pct / 100.0)}"
            }
        }
        val modeRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }
        fun modeChip(label: String, value: String): android.widget.TextView =
            android.widget.TextView(this).apply {
                text = label; textSize = 13f; gravity = android.view.Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(14), dp(9), dp(14), dp(9))
                fun paint(sel: Boolean) {
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(android.graphics.Color.parseColor(if (sel) "#0C9E33" else "#F0F2F5"))
                    }
                    setTextColor(android.graphics.Color.parseColor(if (sel) "#FFFFFF" else "#33404F"))
                }
                paint(value == amtMode)
                setOnClickListener {
                    amtMode = value
                    for (i in 0 until modeRow.childCount) {
                        (modeRow.getChildAt(i).tag as? () -> Unit)?.invoke()
                    }
                    amountInput.hint = if (value == "PERCENT") "Percent (%)" else "Amount"
                    refreshCalc()
                }
                tag = { paint(value == amtMode) }
            }
        modeRow.addView(modeChip("₹  Amount", "AMOUNT").apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginEnd = dp(6) }
        })
        modeRow.addView(modeChip("%  Percent", "PERCENT").apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(6) }
        })
        box.addView(modeRow, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })
        box.addView(amountInput, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(4) })
        box.addView(calcInfo, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) })
        amountInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { refreshCalc() }
            override fun afterTextChanged(e: android.text.Editable?) {}
        })
        /* 🔴🔒 V1504 (১৫.০৯.২০২৬, TK-নির্দেশ, ছবিসহ — AMIT GOLDAR দুবার) —
           আগে এখানে রোগীর ব্রাঞ্চ পাঠানো হতো না, তাই `RmpPicker.cachedRmpChoices`
           নিজে থেকেই লগ-ইন করা স্টাফের ব্রাঞ্চ ধরে নিত — Master-এর branch="All"
           হওয়ায় Master-এর সার্চে **সব ব্রাঞ্চের** RMP একসাথে দেখাত (যেমন
           জলপাইগুড়ির রোগীর জন্য কিশানগঞ্জের "AMIT GOLDAR"ও)। TK-এর স্থায়ী নিয়ম:
           "সেই রোগীর ব্রাঞ্চে RMP না থাকলে নতুন সেভের সাজেশন দেবে, কিন্তু অন্য
           ব্রাঞ্চের RMP দেখানো হবে না।" এখন রোগীর নিজের ব্রাঞ্চ (`currentBranch`)
           পাঠানো হয়, তাই সার্চ ফলাফল সবসময় ওই একটা ব্রাঞ্চেই সীমাবদ্ধ থাকে। */
        val rmpChoices = try {
            val u = NativeSession.current(this)
            if (u == null) emptyList() else RmpPicker.cachedRmpChoices(this, u, branchScope = currentBranch)
        } catch (_: Throwable) { emptyList() }
        if (rmpChoices.isNotEmpty()) {
            val labelToChoice = LinkedHashMap<String, RmpPicker.RmpChoice>()
            for (c in rmpChoices) labelToChoice[c.flatLabel()] = c
            /* 🟢🔒 V1504 (১৫.০৯.২০২৬, TK ডেমো-প্রুফ পাশ) — আগে এই তালিকা
               Android-এর সাধারণ সাদা এক-লাইনের ডিফল্ট চেহারায় দেখাত, TK
               "unprofessional" বলেছিলেন। এখন নাম বড়/বোল্ড আর মোবাইল·এলাকা
               ছোট ধূসর সাব-টেক্সটে, প্রতিটা সারির নিচে হালকা দাগ। ⛔ খোঁজা/
               বাছার নিয়ম (flatLabel-ভিত্তিক ফিল্টার, ফোনের জমানো ঘর থেকেই
               আসা) এক অক্ষরও বদলায়নি — শুধু সারির চেহারা। */
            refNameInput.setAdapter(
                object : android.widget.ArrayAdapter<String>(this, 0, labelToChoice.keys.toList()) {
                    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                        val label = getItem(position) ?: ""
                        val choice = labelToChoice[label]
                        val row = android.widget.LinearLayout(this@PatientTimelineActivity).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            setBackgroundColor(android.graphics.Color.WHITE)
                        }
                        val content = android.widget.LinearLayout(this@PatientTimelineActivity).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            setPadding(dp(12), dp(10), dp(12), dp(10))
                        }
                        content.addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                            text = (choice?.name ?: label).trim().uppercase()
                            textSize = 13.5f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            setTextColor(android.graphics.Color.parseColor("#182230"))
                        })
                        val sub = listOfNotNull(
                            choice?.mobile?.trim()?.takeIf { it.isNotBlank() },
                            choice?.area?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (sub.isNotBlank()) {
                            content.addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                                text = sub; textSize = 11f
                                setTextColor(android.graphics.Color.parseColor("#7A8699"))
                            })
                        }
                        row.addView(content)
                        row.addView(View(this@PatientTimelineActivity).apply {
                            layoutParams = android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1)
                            setBackgroundColor(android.graphics.Color.parseColor("#F0F2F5"))
                        })
                        return row
                    }
                    override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                        getView(position, convertView, parent)
                }
            )
            refNameInput.setOnItemClickListener { parent, _, position, _ ->
                val picked = labelToChoice[parent.getItemAtPosition(position) as? String ?: ""]
                if (picked != null) {
                    // ⛔ দুই-প্যারামিটারের setText — নইলে বসানোর সঙ্গে সঙ্গে তালিকা আবার খুলত
                    refNameInput.setText(picked.name, false)
                    refNameLocked = false; applyNameLockStyle()
                    if (picked.mobile.isNotBlank()) { refMobileInput.setText(picked.mobile); refMobileLocked = false; applyMobileLockStyle() }
                    /* 🟢🔒 B673 (TK: "হ্যাঁ, তবে ফিনান্স খোলা থাকলেই") — এই ডাক্তারের
                       সেভ করা কমিশন (% বা টাকা) থাকলে নিজে থেকে বসে যায়।
                       ⛔ Finance মডিউল খোলা না থাকলে `getDefault` নিজেই ব্যর্থ হয়ে
                          ফেরে — তখন **চুপচাপ কিছুই হয় না**, হাতে লেখা আগের মতোই চলে।
                       ⚡ খরচ: শুধু **একটাই ছোট সারি** (rmp_id + mode + value), আর তাও
                          শুধু ডাক্তার বাছার মুহূর্তে — কয়েকশো বাইট। */
                    if (picked.id.isNotBlank()) {
                        lifecycleScope.launch {
                            // 🔴🔒 V483 (20.08.2026, TK-নির্দেশ — "সততার সাথে
                            // সঠিকভাবে কার্যকরী করুন, কোনো ভালো কাজ যেন খারাপ
                            // না হয়") — আগে শুধু বৈশ্বিক Default (`getDefault`)
                            // ব্যবহার হতো, তাই যাদের ব্রাঞ্চ-ভিত্তিক আলাদা %
                            // সেট করা আছে (V470) তাদের জন্য এখানে ভুল সংখ্যা
                            // auto-fill হতো। এখন `getBranchDefault()` — এই
                            // ফাংশনটাই **সার্ভার-সাইডে নিজে থেকেই** ব্রাঞ্চ-
                            // নির্দিষ্ট থাকলে সেটা, না থাকলে **ঠিক আগের মতোই**
                            // বৈশ্বিক Default ফেরত দেয় (RmpCommissionRepository.
                            // kt-এর নিজস্ব মন্তব্য দ্রষ্টব্য) — তাই যাদের কখনো
                            // ব্রাঞ্চ-Default সেট হয়নি, তাদের ক্ষেত্রে ফলাফল
                            // **অক্ষত**, একটুও বদলায়নি। ⛔ Finance মডিউল বন্ধ
                            // থাকলে আগের মতোই চুপচাপ কিছু হয় না (ব্যর্থ হলে
                            // null ফেরে, হাতে-লেখা চলতেই থাকে)।
                            val def = withContext(Dispatchers.IO) {
                                try { RmpCommissionRepository.getBranchDefault(picked.id, currentBranch) } catch (_: Throwable) { null }
                            }
                            val d = def?.value
                            if (def != null && def.ok && d != null && d.value > 0.0) {
                                amtMode = if (d.mode == RmpCommissionModel.Mode.PERCENT) "PERCENT" else "AMOUNT"
                                for (i in 0 until modeRow.childCount) {
                                    (modeRow.getChildAt(i).tag as? () -> Unit)?.invoke()
                                }
                                amountInput.hint = if (amtMode == "PERCENT") "Percent (%)" else "Amount"
                                amountInput.setText(money2(d.value).toString())
                                refreshCalc()
                            }
                        }
                    }
                }
            }
        }
        /* 🔴🔒 V1399 (১২.০৯.২০২৬, TK-নির্দেশ, ডেমো ফটো পাশ) — *"এই পেশেন্টের
           জন্য সেই আরএমপিকে কত পার্সেন্টেজ অথবা কত টাকা দেব সেটা এখান থেকে
           নির্বাচন করব, কিন্তু টাকা যেটা দিতে হবে সেটা আরএমপি সেকশনে গিয়েই
           দিতে হবে।"*
           এই ফর্মের কাজ শুধু **লিংক করা** (কোন RMP, কত টাকা/%) — টাকা সত্যিই
           দেওয়া হয়েছে কিনা সেটা তখনই বোঝা যায় না, তাই "Due/Paid" জিজ্ঞাসা করা
           বিভ্রান্তিকর ছিল (TK-র রিপোর্ট)। এখন সবসময় **Due** হয়েই সেভ হয় —
           RMP-কে সত্যিই টাকা দেওয়ার সময় RMP-র নিজের Referral Income পাতা
           থেকে (Edit → Paid) বসাতে হবে, যেটা আগে থেকেই আছে (openReferralEdit)।
           ⛔ RMP-লিংক · Amount/Percent বাছাই — এসবের কোনো আচরণ বদলায়নি। */
        val status = "Unpaid"
        box.addView(android.widget.TextView(this).apply {
            text = "This links the patient to the RMP for the amount above — always saved as Due.\nTo actually pay the RMP, open the RMP's own Referral Income screen and mark it Paid there."
            textSize = 11.5f
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(6) })

        UppercaseInputUtil.applyToAll(box)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "💰 Add Referral Income — ${currentPatientName.ifBlank { currentMobile }}"))
            .setView(box)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                // 🔒🔒 V1381 — ডাবল-ট্যাপে দ্বিতীয়বার সেভ আটকানো (Refund-এর মতোই)।
                if (referralSaving) return@setOnClickListener
                // 🟢 B673: % বাছা থাকলে এখানেই টাকায় বদলে নেওয়া হয় — নিচের সেভ-পথ,
                //   যাচাই, বার্তা কিচ্ছু বদলায়নি; শেষ পর্যন্ত টাকার অঙ্কই (`amt`) জমা হয়।
                val typedValue = amountInput.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (amtMode == "PERCENT" && typedValue > 100.0) {
                    android.widget.Toast.makeText(this@PatientTimelineActivity,
                        "Percent cannot be more than 100", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (amtMode == "PERCENT" && commissionBase <= 0.0) {
                    android.widget.Toast.makeText(this@PatientTimelineActivity,
                        "Patient has paid nothing yet — type an Amount", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val amt = if (amtMode == "PERCENT")
                    money2(commissionBase * typedValue / 100.0) else typedValue
                val refName = refNameInput.text.toString().trim()
                val refMobile = refMobileInput.text.toString().filter { it.isDigit() }.takeLast(10)
                val vmsg = FieldError.validate(listOf<Triple<View, Boolean, String>>(
                    Triple(amountInput, amt > 0, "সঠিক Amount দিন"),
                    Triple(refNameInput, refName.isNotBlank(), "Doctor নাম দিন"),
                    Triple(refMobileInput, refMobile.length == 10, "সঠিক 10 ডিজিট মোবাইল দিন")
                ))
                if (vmsg != null) {
                    android.widget.Toast.makeText(this@PatientTimelineActivity, vmsg, android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                /* 🔴🔒 V1398 (১২.০৯.২০২৬, নিজের-অডিটে ধরা পড়া ভুল, TK-কে জানিয়ে) —
                   V1381-এর লক আগে `lifecycleScope.launch`-এর **ভিতরে**, নেট-ডাক
                   শেষ হওয়ার পরে বসত — refundSaving (PaymentActivity.kt)-এর
                   প্রমাণিত নিয়মে ছিল না, তাই দুই ট্যাপের মাঝের নেট-বিলম্বে লকটা
                   আসলে কাজই করত না। এখন ক্লিক হ্যান্ডলারেই সঙ্গে সঙ্গে লক, আর
                   প্রতিটা বেরিয়ে-যাওয়ার পথে (Cancel/ব্যর্থতা/সেভ-শেষ) খুলে দেওয়া হয়। */
                if (referralSaving) return@setOnClickListener
                referralSaving = true
                lifecycleScope.launch {
                    val repo = DoctorVisitRepository()
                    // 🔴🔒 V1395 — নিজের ব্রাঞ্চ পাঠানো হলো, একই মোবাইল/নাম একাধিক
                    // ব্রাঞ্চে থাকলে যেন ভুল ব্রাঞ্চের RMP বেছে না নেয়।
                    val existingDoc = withContext(Dispatchers.IO) { repo.findReferringDoctor(refName, refMobile, currentBranch) }
                    if (existingDoc != null) {
                        val docId = existingDoc.optString("id")
                        if (docId.isBlank()) {
                            referralSaving = false
                            android.widget.Toast.makeText(this@PatientTimelineActivity, "Could not save — doctor not found on Dr. Visit list, or check connection", android.widget.Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        /* 🔒🔒 V1381 (TK-রিপোর্ট, ANAND KUMAR ₹3,250 দুইবার) — Refund-এর
                           V786-এর হুবহু একই নিয়ম: আজকের দিনে এই রোগীর জন্য এই ডাক্তারের
                           কাছে **একই পরিমাণ** Referral Income আগে থেকে থাকলে সতর্কবার্তা —
                           Cancel = কিছুই হবে না · OK = জেনেশুনে তবুও। */
                        val dup = withContext(Dispatchers.IO) { repo.todaysReferralLike(docId, currentMobile, amt) }
                        if (dup != null) {
                            val prevStatus = dup.optString("status")
                            referralSaving = false
                            android.widget.Toast.makeText(
                                this@PatientTimelineActivity,
                                "Duplicate blocked: ₹${"%,.0f".format(amt)} for ${currentPatientName.ifBlank { currentMobile }} is already recorded today" +
                                    (if (prevStatus.isNotBlank()) " ($prevStatus)" else ""),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                        try {
                            val ok = withContext(Dispatchers.IO) { saveReferralAfterDoctorKnown(repo, docId, refName, refMobile, amt, status) }
                            android.widget.Toast.makeText(this@PatientTimelineActivity, if (ok) "Referral income saved for Dr. $refName" else "Could not save — check connection", android.widget.Toast.LENGTH_LONG).show()
                            if (ok) { currentRefDoctor = refName; currentRefDoctorMobile = refMobile; dialog.dismiss() }
                        } finally { referralSaving = false }
                        return@launch
                    }
                    // 🔴🔒 V467 (20.08.2026, TK-অনুমোদিত ফটো-প্রুফ, ফাইনাল) —
                    // "Rmp তালিকায় না থাকলে নতুন RMP হিসেবে যোগ করা যাবে।"
                    // ⛔ পুরনো/তালিকায়-থাকা ডাক্তারের জন্য উপরের পথ (existingDoc
                    // != null) এক অক্ষরও বদলায়নি। এই নতুন পথ শুধু তখনই, যখন
                    // নাম/মোবাইল সত্যিই কোনো RMP-র সাথে মেলেনি।
                    val branches = BranchFilterStore.BRANCHES
                    var chosenBranch = branches.first()
                    val bd = resources.displayMetrics.density
                    fun bdp(v: Int) = (v * bd).toInt()
                    val warnBox = android.widget.LinearLayout(this@PatientTimelineActivity).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(bdp(20), bdp(6), bdp(20), bdp(4))
                    }
                    warnBox.addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                        text = "\"Dr. $refName\" ($refMobile) is not in the RMP list yet. Add as a new RMP?"
                        textSize = 14f; setTextColor(android.graphics.Color.parseColor("#33404F"))
                        setPadding(0, 0, 0, bdp(10))
                    })
                    warnBox.addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                        text = "Branch *"; textSize = 12.5f; setTextColor(android.graphics.Color.parseColor("#7A8699"))
                        setPadding(0, 0, 0, bdp(4))
                    })
                    val branchSpinner = android.widget.Spinner(this@PatientTimelineActivity).apply {
                        adapter = android.widget.ArrayAdapter(this@PatientTimelineActivity, android.R.layout.simple_spinner_dropdown_item, branches)
                        setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
                        onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) { chosenBranch = branches[pos] }
                            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                        }
                    }
                    warnBox.addView(branchSpinner)
                    androidx.appcompat.app.AlertDialog.Builder(this@PatientTimelineActivity)
                        .setCustomTitle(PremiumAlert.header(this@PatientTimelineActivity, "🩺 New RMP"))
                        .setView(warnBox)
                        .setPositiveButton("Yes, Add as New RMP") { _, _ ->
                            lifecycleScope.launch {
                                val staffMobile = NativeSession.current(this@PatientTimelineActivity)?.mobile ?: ""
                                val created = withContext(Dispatchers.IO) {
                                    repo.addNewDoctor(refName, refMobile, chosenBranch, "", "", "", staffMobile, applicationContext)
                                }
                                if (!created) {
                                    referralSaving = false
                                    android.widget.Toast.makeText(this@PatientTimelineActivity, "Could not create new RMP — check connection", android.widget.Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                // 🔴🔒 V1395 — এইমাত্র chosenBranch-এ যে RMP বানানো হলো, সেটাই যেন ফেরত আসে।
                                val newDoc = withContext(Dispatchers.IO) { repo.findReferringDoctor(refName, refMobile, chosenBranch) }
                                val newDocId = newDoc?.optString("id").orEmpty()
                                if (newDocId.isBlank()) {
                                    referralSaving = false
                                    android.widget.Toast.makeText(this@PatientTimelineActivity, "RMP created, but could not link — please retry Save", android.widget.Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                // 🔒🔒 V1381 — এই নতুন-RMP পথেও একই ডাবল-ট্যাপ লক (আগে থেকেই
                                // ক্লিক হ্যান্ডলারে বসানো, এখানে শুধু শেষে খোলা হয়)।
                                val ok = try {
                                    withContext(Dispatchers.IO) { saveReferralAfterDoctorKnown(repo, newDocId, refName, refMobile, amt, status) }
                                } finally { referralSaving = false }
                                android.widget.Toast.makeText(this@PatientTimelineActivity, if (ok) "New RMP added · Referral income saved for Dr. $refName" else "RMP created, but referral income could not be saved — check connection", android.widget.Toast.LENGTH_LONG).show()
                                if (ok) { currentRefDoctor = refName; currentRefDoctorMobile = refMobile; dialog.dismiss() }
                            }
                        }
                        .setNegativeButton("Cancel") { _, _ -> referralSaving = false }
                        .setOnCancelListener { referralSaving = false }
                        .show().also { PremiumAlert.paint(it) }
                }
            }
        }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
        // 🔒 খাতার সারি B181 (TK, 30.07.2026): "Add Referral Income" পপ-আপের
        // নিজের পাহারা ছিল না।
        PremiumAlert.paint(dialog)
    }

    /** Quick Remark — same underlying save (FollowUpRepository.updateRemark)
     *  used by every other remark box in the app (Chamber Attendance,
     *  Follow-up itself), so this behaves identically everywhere. */
    private fun showQuickRemarkDialog() {
        if (currentFollowupId.isBlank()) {
            android.widget.Toast.makeText(this, "No Follow-up record yet for this patient", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val input = android.widget.EditText(this).apply { hint = "Remark" }
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad, pad, 0)
            addView(input)
        }
        UppercaseInputUtil.applyToAll(box)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Add Remark"))
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isBlank()) {
                    android.widget.Toast.makeText(this, "Remark required", android.widget.Toast.LENGTH_SHORT).show()
                    input.requestFocus()
                    return@setPositiveButton
                }
                // 🟡🔒 V691 (২৬.০৮.২০২৬, TK-নির্দেশ ছবিসহ) — একই দিনে দুবার
                // Remark লেখা হলে আগে সাবধান করা হয়। সেখানেই ঠিক করা যায় —
                // আগেরটা বদলাবেন, নাকি নতুন একটা সারি হবে।
                val sameDay = todaysRemarkEntry()
                if (sameDay == null) saveQuickRemark(text) else showSameDayRemarkWarning(sameDay, text)
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /**
     * 🟡🔒 V691 (২৬.০৮.২০২৬, TK-নির্দেশ, দুটো ছবিসহ) — **একই দিনে
     * দুটো Remark চুপচাপ জমা হয়ে যাবে না।**
     *
     * TK-এর ছবিতে (+919707360144) একই দিনে ৮.৫৭ ও ৮.৫৮-তে দুটো প্রায়-এক
     * Remark ("1 SAPTAHO PRE ASBENI" ও "1 SAPTAHO PRE ASBEN") বসে গেছে —
     * লেখার সময় কোনো সাবধানবাণী আসেনি। এখন আসে।
     *
     * ⚠️ নেটের খরচ শূন্য — পর্দায় ইতিমধ্যে ধরা এই রোগীর তালিকা
     * (`currentEntries`) থেকেই দেখা হয়, নতুন কোনো Supabase অনুরোধ যায় না।
     *
     * আজকের সবচেয়ে শেষ follow-up history রিমার্ক সারিটা ফেরায়; আজ
     * কিছু লেখা না হয়ে থাকলে null।
     */
    private fun todaysRemarkEntry(): TimelineEntry? {
        val today = FollowUpModel.today()
        return currentEntries
            .filter {
                !it.followUpHistoryId.isNullOrBlank() && it.followUpHistoryIndex >= 0 &&
                    it.date.take(10) == today && it.note.isNotBlank()
            }
            // একই দিনে একাধিক সারি থাকলে সবচেয়ে শেষটাই "আগের Remark"।
            // time না থাকলে (পুরনো সারি) history-র ক্রমই শেষ কথা।
            .maxWithOrNull(compareBy({ it.callTime }, { it.followUpHistoryIndex }))
    }

    /**
     * 🟡🔒 V691 — TK-এর ২য় ছবির সাবধানবাণী। তিনটে পথই রাখা হলো —
     * **Update Previous** (আজকের লেখাটাই বদলাবে, নতুন সারি জমবে না),
     * **Save New Remark** (আগে যা হত ঠিক তাই — নতুন সারি), আর **Cancel**।
     *
     * ⛔ পপ-আপের রং নিজে থেকে বানানো হয়নি — প্রজেক্টের লক করা
     *    `PremiumAlert`-ই ব্যবহার করা হলো। শিরোনাম "⚠️" দিয়ে শুরু বলে
     *    সেটা নিজেই হলুদ (caution) হয় — ঠিক যেমন "এই নম্বর আগেই আছে"
     *    পপ-আপটা হয়।
     */
    private fun showSameDayRemarkWarning(previous: TimelineEntry, newRemark: String) {
        val d = resources.displayMetrics.density
        val pad = (18 * d).toInt()
        val body = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, (14 * d).toInt(), pad, 0)
        }
        // ⛔ লেখাগুলো ইচ্ছে করেই ইংরেজি — TK-এর পাঠানো ছবির হুবহু সেই কথা।
        //    তাই বাংলা-বন্ধ স্টাফের পর্দাতেও এটা এমনিই ঠিক থাকে।
        body.addView(android.widget.TextView(this).apply {
            text = "A remark has already been saved today."
            textSize = 14.5f
            setTextColor(android.graphics.Color.parseColor("#101828"))
        })
        /* 🎨🔒 V719 (২৬.০৮.২০২৬, TK-নির্দেশ *"এবার এটাকে প্রফেশনাল বানান"*,
           ডেমো-প্রুফে **প্রস্তাব ১** অনুমোদিত):

           **আগে কী দেখাত:** Android নিজের তিনটে বোতাম (Positive/Negative/
           Neutral) ডান দিকে **তিন মাপে** সাজাত, আর "Cancel" মাঝখানে পড়ে
           যেত — অগোছালো (TK-এর পাঠানো ছবি)।

           **এখন:** আজকের লেখাটা আলাদা একটা হালকা-হলুদ বাক্সে, আর তিনটে
           বোতামই **সমান চওড়া, উপর-নিচ** — পপ-আপের ভিতরেই।
           ক্রম: **Save New Remark** (মূল) → **Update Previous** → **Cancel**।

           ⛔ **কাজ এক অক্ষরও বদলায়নি** — তিনটে বোতাম ঠিক আগের তিনটে কাজই
              করে (`saveQuickRemark` · `replaceRemarkText` · কিছু না)।
           ⛔ রং নিজে বানানো হয়নি — `PremiumAlert`-এর লক করা হলুদ
              (#E8A100 · লেখা #3A2600) ও নিরপেক্ষ মানগুলোই নেওয়া হয়েছে,
              যাতে বাকি পপ-আপের সঙ্গে হুবহু মেলে।
           ⛔ শিরোনাম · হলুদ হেডার · `PremiumAlert.paint()` অপরিবর্তিত।
              `paint()` বোতাম না পেলে চুপচাপ ছেড়ে দেয় (`?.let`) — নিরাপদ।
              বাংলা-বন্ধ স্টাফের ঢাকনা (`NoBengali.installDialog`) পুরো
              পপ-আপেই বসে, তাই নতুন বোতামগুলোও ঢাকা পড়ে।
           ⛔ সব লেখা ইংরেজি — TK-এর নিয়ম। */
        body.addView(android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding((13 * d).toInt(), (10 * d).toInt(), (13 * d).toInt(), (11 * d).toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 12 * d
                setColor(android.graphics.Color.parseColor("#FFF9EC"))
                setStroke((1.5f * d).toInt(), android.graphics.Color.parseColor("#F3DFAF"))
            }
            addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                text = "TODAY'S REMARK"
                textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#9A6B00"))
            })
            addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                text = previous.note
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#3A2A00"))
                setPadding(0, (3 * d).toInt(), 0, 0)
            })
            layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (12 * d).toInt(); bottomMargin = (14 * d).toInt()
            }
        })
        body.addView(android.widget.TextView(this).apply {
            text = "What would you like to do?"
            textSize = 13.5f
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
        })

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "\u26A0\uFE0F Same-day Remark"))
            .setView(body)
            .create()

        // সমান চওড়া, উপর-নিচ সাজানো একটা বোতাম।
        fun wideBtn(label: String, bg: String, fg: String, edge: String?, top: Int, action: (() -> Unit)?) {
            body.addView(android.widget.TextView(this).apply {
                text = label
                textSize = 15.5f
                gravity = android.view.Gravity.CENTER
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(fg))
                setPadding(0, (14 * d).toInt(), 0, (14 * d).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 14 * d
                    setColor(android.graphics.Color.parseColor(bg))
                    if (edge != null) setStroke((2 * d).toInt(), android.graphics.Color.parseColor(edge))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { dialog.dismiss(); action?.invoke() }
                layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (top * d).toInt()
                }
            })
        }
        wideBtn("Save New Remark", "#E8A100", "#3A2600", null, 13) { saveQuickRemark(newRemark) }
        wideBtn("Update Previous", "#FFFFFF", "#22304A", "#D4DAE4", 11) { replaceRemarkText(previous, newRemark) }
        wideBtn("Cancel", "#00000000", "#7A8798", null, 4, null)
        body.setPadding(pad, (14 * d).toInt(), pad, (14 * d).toInt())

        dialog.show()
        PremiumAlert.paint(dialog)
    }

    /** 🟡🔒 V691 — আগে যে সেভটা Save-এর ভিতরেই লেখা ছিল, সেটাই এখানে
     *  সরানো হলো — এক অক্ষরও বদলায়নি। সাবধানবাণীর "Save New Remark"
     *  ও সাবধানবাণী না-ওঠা — দুটো পথই এই একটা ফাংশনে এসে মেলে। */
    private fun saveQuickRemark(text: String) {
        val user = NativeSession.current(this) ?: return
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                // V215 (§17, 31.07.2026): আগে শুধু Inquiry-তে call গোনা হত।
                // এখন যে কোনো stage-এ post-call remark সফল হলে সেটা একটা
                // Completed Call — Last Call Date/Time আজকের হয়, Signal এক
                // ধাপ বাড়ে (repository-তে দিনে-একবার de-dup আছে, double count
                // হয় না)। এটাই ছিল Visit/Patient card-এ Last Call Date না
                // বদলানোর আসল কারণ।
                val countAsCall = true
                FollowUpRepository(this@PatientTimelineActivity).updateRemark(
                    resolveFollowUpIdHere(), text, user.name.ifBlank { user.mobile }, countAsCall
                )
            }
            android.widget.Toast.makeText(
                this@PatientTimelineActivity,
                if (ok) "Remark saved" else "Failed — check connection",
                android.widget.Toast.LENGTH_SHORT
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            if (ok) load(currentMobile, currentSection)
        }
    }

    /** 🟡🔒 V691 — "Update Previous": আজকের যে সারিটা আগেই আছে, তার
     *  লেখাটাই বদলে যায় — ইতিহাসে নতুন সারি জমে না। এটা হুবহু সেই
     *  কাজ যা 3-tap "✏️ Edit Note" করে — তাই একই `writeFollowUpHistoryRemark()`
     *  ডাকা হয়, দুরকম হিসাব তৈরি হয় না।
     *  ⛔ কল গোনা (`callCount`) বা Last Call তারিখ ছোঁয়া হয় না — আজকের
     *     কল আগেই গোনা হয়ে গেছে, লেখা শুধরানো নতুন কল নয়। */
    private fun replaceRemarkText(previous: TimelineEntry, text: String) {
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try { writeFollowUpHistoryRemark(previous, text) } catch (_: Throwable) { false }
            }
            android.widget.Toast.makeText(
                this@PatientTimelineActivity,
                if (ok) "Remark updated" else "Failed — retry",
                android.widget.Toast.LENGTH_SHORT
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            if (ok) load(currentMobile, currentSection)
        }
    }

    /** 🟡🔒 V691 — follow-up-এর `history` তালিকায় একটা নির্দিষ্ট সারির
     *  লেখা বদলানোর একমাত্র জায়গা। আগে এই কোডটা `editEnquiryHistoryNote()`-এর
     *  ভিতরে লেখা ছিল; V691-এ "Update Previous"-ও হুবহু একই কাজ চায় বলে
     *  এখানে সরানো হলো — ভিতরের কাজে এক অক্ষরও বদলায়নি।
     *  ⚠️ IO thread থেকেই ডাকতে হবে (নেট পড়ে ও লেখে)। */
    private fun writeFollowUpHistoryRemark(e: TimelineEntry, text: String): Boolean {
        val fuId = e.followUpHistoryId ?: return false
        if (e.followUpHistoryIndex < 0) return false
        /* 🟢🔒 V837 (২৯.০৮.২০২৬, TK-নির্দেশে মেপে পাওয়া) — আগে এখানে `"*"`
           লেখা ছিল, অর্থাৎ ওই নম্বরের প্রতিটা followups সারির **সব ঘর**
           নামত — **রোগীর ছবিসহ** (`photo` ঘরটা base64 ছবি, সবচেয়ে ভারী)।
           ⛔ অথচ নিচের কোড এই সারিগুলো থেকে পড়ে **মাত্র দুটো ঘর**:
              `id` (কোন সারিটা) আর `history` (কোন লেখাটা বদলাবে) —
              পুরো ফাংশনটা পড়ে গুনে দেখা, আর কিচ্ছু ছোঁয়া হয় না।
           ⛔ লেখাটা আগের মতোই `updateById`-তে শুধু `history` (+ শেষ সারি হলে
              `lastRemark`) পাঠায় — **পুরো সারি কখনো ফেরত লেখা হয় না**,
              তাই ছবি হারানোর কোনো সুযোগ নেই।
           ⛔ ৫০০০ সীমা আগের মতোই রইল (এক নম্বরে ৩-৪টার বেশি সারি হয় না;
              সংখ্যাটা কমালে কোনো লাভ নেই, বরং ঝুঁকি)। */
        val rows = SupabaseClient.findByMobile("followups", "+91$currentMobile", "id,history", 5000)
        var target: org.json.JSONObject? = null
        for (i in 0 until rows.length()) { if (rows.getJSONObject(i).optString("id") == fuId) { target = rows.getJSONObject(i); break } }
        val row = target ?: return false
        val hist = row.optJSONArray("history") ?: return false
        if (e.followUpHistoryIndex >= hist.length()) return false
        val item = hist.getJSONObject(e.followUpHistoryIndex)
        item.put("remark", text)
        val fields = org.json.JSONObject().put("history", hist)
        if (e.followUpHistoryIndex == hist.length() - 1) fields.put("lastRemark", text)
        val saved = SupabaseClient.updateById("followups", fuId, fields)
        if (!saved) GenericUpdateQueue.queue(this@PatientTimelineActivity, "followups", fuId, fields)
        return saved
    }

    /** Quick Next Follow-up date picker — same underlying save
     *  (FollowUpRepository.updateNextFollow) used by the Follow-up screen. */
    /**
     * 🟢🔒 V616 (২৪.০৮.২০২৬, TK-নির্দেশ) — ভুল ব্রাঞ্চে রেজিস্টার হওয়া
     * রোগীর সব তথ্য (patients/followups/payments) সঠিক ব্রাঞ্চে সরানো।
     * Master-only (menu-তেই role-চেক করা)। ঝুঁকিপূর্ণ/অপরিবর্তনীয় বলে
     * PremiumAlert-এর লাল (severe) হেডার + স্পষ্ট সংখ্যা দেখিয়ে দুবার
     * নিশ্চিত করা হয় — TK-এর নিজের "৩ বার চাপ" চাওয়ার চেয়ে নিরাপদ ধরন,
     * কারণ এখানে ভুল হলে ফেরানো কঠিন (তাই "৩ চাপ" না, দুটো পূর্ণ
     * নিশ্চিতকরণ-পর্দা)।
     */
    private fun showChangeBranchDialog() {
        val mobile = currentMobile
        android.widget.Toast.makeText(this, NoBengali.s("খোঁজা হচ্ছে…"), android.widget.Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val preview = withContext(Dispatchers.IO) { BranchTransferRepository.preview(mobile) }
            if (preview == null || preview.totalCount == 0) {
                android.widget.Toast.makeText(this@PatientTimelineActivity, NoBengali.s("কোনো রেকর্ড পাওয়া যায়নি — নেট চেক করুন"), android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            val fromBranches = preview.currentBranches.joinToString(", ").ifBlank { "—" }
            val branches = BranchFilterStore.BRANCHES
            // 🔴🔒 V616 — প্রতিটা স্থির (static) বাংলা অংশ আলাদাভাবে NoBengali.s()-এ
            // মোড়া, মাঝে সংখ্যা/নাম জোড়া — যাতে অভিধান-মেলানো নিশ্চিত হয়
            // (একই কারণ V607/CallReminderWorker-এ ব্যবহার হয়েছিল)।
            val msg = "$currentPatientName ($mobile)\n\n" +
                NoBengali.s("এখনকার ব্রাঞ্চ: ") + fromBranches + "\n" +
                NoBengali.s("মোট সারি সরবে: ") + preview.totalCount + " " +
                "(Patient " + preview.patientRows.size + " · Follow-up " + preview.followupRows.size + " · Payment " + preview.paymentRows.size + ")\n\n" +
                NoBengali.s("নতুন ব্রাঞ্চ বেছে নিন:")
            // 🔴🔒 V679 (২৫.০৮.২০২৬, TK-লাইভ-টেস্ট রিপোর্ট — "কোথাও চাপ দিলে
            // কোনো কাজই হয় না") — আসল কারণ: AlertDialog-এ `setMessage()`
            // ও `setSingleChoiceItems()` একসাথে দিলে Android নিজস্ব
            // তালিকাটাই আর দেখায় না (`setView()`-এর সাথে মিলিয়ে দিলেও
            // একই সমস্যা — দুটো কখনোই একসাথে কাজ করে না)। প্রজেক্টের
            // বাকি প্রতিটা ব্রাঞ্চ-পিকার এই দুটো একসাথে ব্যবহারই করে না,
            // তাই ওখানে এই বাগ কখনো ছিল না।
            //
            // সমাধান: লেখা + তালিকা দুটোই এখন একটাই কাস্টম View-তে হাতে
            // বসানো (TextView হেডার + প্রতিটা ব্রাঞ্চের নিজের ক্লিকযোগ্য
            // সারি) — `setSingleChoiceItems()` আর ব্যবহার হচ্ছে না।
            val d = resources.displayMetrics.density
            val container = android.widget.LinearLayout(this@PatientTimelineActivity).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding((20 * d).toInt(), (16 * d).toInt(), (20 * d).toInt(), (8 * d).toInt())
            }
            container.addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                text = msg
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#344054"))
                setLineSpacing(0f, 1.2f)
                setPadding(0, 0, 0, (10 * d).toInt())
            })
            var chosenDialog: androidx.appcompat.app.AlertDialog? = null
            for (b in branches) {
                container.addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                    text = b
                    textSize = 15.5f
                    setTextColor(android.graphics.Color.parseColor("#1B2A4A"))
                    setPadding((4 * d).toInt(), (12 * d).toInt(), (4 * d).toInt(), (12 * d).toInt())
                    isClickable = true; isFocusable = true
                    background = android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E9EEF5")),
                        null, null
                    )
                    setOnClickListener {
                        chosenDialog?.dismiss()
                        confirmChangeBranch(preview, b, fromBranches)
                    }
                })
                if (b != branches.last()) container.addView(android.view.View(this@PatientTimelineActivity).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#EEF2F7"))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1 * d).toInt()
                    )
                })
            }
            chosenDialog = androidx.appcompat.app.AlertDialog.Builder(this@PatientTimelineActivity)
                .setCustomTitle(PremiumAlert.header(this@PatientTimelineActivity, "🔀 Change Branch"))
                .setView(container)
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    private fun confirmChangeBranch(preview: BranchTransferRepository.TransferPreview, newBranch: String, fromBranches: String) {
        // 🔴 দ্বিতীয় নিশ্চিতকরণ — লাল/গুরুতর, কারণ এটা একবার হয়ে গেলে
        // হাতে-ধরে আবার উল্টাতে হবে (কোনো "Undo" নেই)।
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, NoBengali.s("⚠️ This branch change is permanent")))
            .setMessage(
                "$fromBranches → $newBranch\n" +
                preview.totalCount + NoBengali.s("টা সারি সরবে (Patient/Follow-up/Payment)।\n\n") +
                NoBengali.s("✅ Patient ID নতুন ব্রাঞ্চ অনুযায়ী নতুন করে বানানো হবে — ") +
                NoBengali.s("⛔ পুরনো ছাপা কাগজ/প্রেসক্রিপশন/রসিদে যে ID ছাপা ছিল, সেটা আর মিলবে না।\n\n") +
                NoBengali.s("সত্যিই এগোতে চান?")
            )
            .setPositiveButton(NoBengali.s("Yes, move it")) { _, _ ->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        BranchTransferRepository.transfer(preview, newBranch, this@PatientTimelineActivity)
                    }
                    val idLine = if (result.newPatientIds.isNotEmpty())
                        "\n" + NoBengali.s("নতুন ID: ") + result.newPatientIds.joinToString(", ") else ""
                    val resultMsg = if (result.failed == 0)
                        "✅ " + result.moved + NoBengali.s("টা সারি ") + newBranch + NoBengali.s("-এ সরানো হলো") + idLine
                    else
                        "⚠️ " + result.moved + NoBengali.s("টা সরেছে, ") + result.failed + NoBengali.s("টা ব্যর্থ — আবার চেষ্টা করুন") + idLine
                    android.widget.Toast.makeText(this@PatientTimelineActivity, resultMsg, android.widget.Toast.LENGTH_LONG).show()
                    if (result.moved > 0) load(currentMobile, currentSection)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /**
     * 🟢🔒 V621 (২৪.০৮.২০২৬, TK-নির্দেশ) — Visit Card থেকে Fees (Visit
     * Fee/Registration Fee) ফেরত। ⛔ কোনো নতুন টাকা-হিসাবের পথ বানানো
     * হয়নি — একই প্রমাণিত `PaymentRepository.identitiesOnMobile()` +
     * `saveRefund()` (V509-এ Visit Fee-সহ refundable) ব্যবহার হচ্ছে,
     * যা Refund স্ক্রিনও ব্যবহার করে। শুধু এখানে সফল হওয়ার পরে অতিরিক্ত
     * একটা কাজ: এই Visit-কে "Returned" ট্যাগ করা, যাতে —
     *   ১) Draft-এ নতুন "Return Visit" তালিকায় দেখা যায়
     *   ২) Chamber Date থেকে সম্পূর্ণ বাদ যায় (RefundedRecords-এর নতুন
     *      `fetchReturnedVisits()`, বিদ্যমান Cancelled-নিয়ম অক্ষত রেখে)
     */
    private fun showReturnFeesDialog() {
        val mobile = currentMobile
        android.widget.Toast.makeText(this, NoBengali.s("লোড হচ্ছে…"), android.widget.Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val user = NativeSession.current(this@PatientTimelineActivity) ?: return@launch
            val patients = withContext(Dispatchers.IO) {
                try { PaymentRepository(this@PatientTimelineActivity).identitiesOnMobile(mobile) } catch (_: Throwable) { emptyList() }
            }
            val patient = patients.firstOrNull()
            if (patient == null || patient.visitFeePaid <= 0.0) {
                android.widget.Toast.makeText(this@PatientTimelineActivity, NoBengali.s("ফেরতযোগ্য Fees নেই এই রোগীর"), android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            // 🔒 একই দিন-ভিত্তিক নিয়ম যা বাকি সব পেমেন্ট-সংশোধনে চলে —
            // চেম্বার আজ খোলা থাকলে (বা Master হলে) সরাসরি, নইলে অনুমতি লাগবে।
            val allowed = user.role == "master" || withContext(Dispatchers.IO) {
                try { PaymentRepository(this@PatientTimelineActivity).chamberOpenToday(patient.branch) } catch (_: Throwable) { false }
            }
            if (!allowed) {
                android.widget.Toast.makeText(this@PatientTimelineActivity, NoBengali.s("আজকের চেম্বার বন্ধ হয়ে গেছে — এখন Master-এর অনুমতি লাগবে"), android.widget.Toast.LENGTH_LONG).show()
                return@launch
            }
            val amt = patient.visitFeePaid
            androidx.appcompat.app.AlertDialog.Builder(this@PatientTimelineActivity)
                .setCustomTitle(PremiumAlert.header(this@PatientTimelineActivity, NoBengali.s("⚠️ Return Fees — permanent")))
                .setMessage(
                    "₹${"%,.0f".format(amt)} " + NoBengali.s("ফেরত দেওয়া হবে।\n") +
                    NoBengali.s("এই Visit \"Return Visit\" তালিকায় (Draft) সরে যাবে — Chamber Date-সহ সক্রিয় তালিকা থেকে বাদ পড়বে।\n\n") +
                    NoBengali.s("সত্যিই এগোতে চান?")
                )
                .setPositiveButton(NoBengali.s("Yes, Return it")) { _, _ ->
                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            PaymentRepository(this@PatientTimelineActivity).saveRefund(patient, amt, "CASH", "Fees Return (Visit Card)", user)
                        }
                        if (!result.success) {
                            android.widget.Toast.makeText(this@PatientTimelineActivity, result.message.ifBlank { NoBengali.s("ব্যর্থ — আবার চেষ্টা করুন") }, android.widget.Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        val marked = withContext(Dispatchers.IO) {
                            try {
                                if (currentFollowupId.isBlank()) false
                                else SupabaseClient.updateById("followups", currentFollowupId, org.json.JSONObject().put("status", "Returned"))
                            } catch (_: Throwable) { false }
                        }
                        android.widget.Toast.makeText(
                            this@PatientTimelineActivity,
                            NoBengali.s(if (marked) "✅ Fees ফেরত হলো — Return Visit-এ সরানো হলো" else "Fees ফেরত হয়েছে, কিন্তু Return Visit-ট্যাগ ব্যর্থ — Draft-এ হাতে ঠিক করুন"),
                            android.widget.Toast.LENGTH_LONG
                        ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                        load(currentMobile, currentSection)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    /**
     * 🏷️🔒 V1014 (০৩.০৯.২০২৬) — **Give Discount**, TK-অনুমোদিত (ছবি-প্রুফসহ)।
     *
     * TK-এর নিজের উদাহরণ: *"কোন একটা পেশেন্টের ২৫০০০ বিল হয়েছিল… ২২ হাজার
     * পরিশোধ করলো… বলল ৩ হাজার ক্ষমা করে দিন… সেই ক্ষেত্রে আমরা তাকে তিন হাজার
     * টাকা ডিসকাউন্ট করলাম… এবার আমি চাইছি সে কমপ্লিট পেশেন্ট হয়ে যায়, কিন্তু
     * ভবিষ্যতের জন্য আমি যেন দেখতে পাই যে এই পেশেন্টকে আমি তিন হাজার টাকা
     * ডিসকাউন্ট করেছিলাম।"*
     *
     * **TK যেভাবে চেয়েছেন (০৩.০৯.২০২৬-এ দুটো প্রশ্নের উত্তরে):**
     *  · *"প্রথমটা করুন"* — **বিলটাই কমবে** (২৫,০০০ → ২২,০০০)। তাই Due-এর
     *    হিসাব (`বিল − জমা`) অ্যাপের ১০+ জায়গায় যেমন লেখা আছে **তেমনই থাকল**,
     *    একটা অক্ষরও বদলাতে হয়নি — এটাই সবচেয়ে কম-ঝুঁকির পথ।
     *  · *"ডিসকাউন্ট যদি না দেয়া হয় সেই ক্ষেত্রে তিনটে বক্স ই থাকতে হবে"* —
     *    তাই `boxDiscount` শূন্যে GONE (TK-এর পুরনো "শূন্য দেখানোর দরকার নেই"
     *    নিয়মটাই)।
     *  · *"শুধু টিকে বিশ্বাস না সবাই করতে পারবে"* + *"ডিসকাউন্ট ডাক্তারও দিতে
     *    পারবে"* — তাই এখানে **কোনো role-বাধা নেই**; কে দিল সেটা
     *    `discountBy`-তে চিরকাল লেখা থাকে।
     *
     * **যা জমা থাকে (`patients` সারিতেই, নতুন কোনো টেবিল নয়):**
     * `bill` (কমানো) · `discount` (মোট ছাড়) · `billBeforeDiscount` (একদম আসল
     * বিল, প্রথমবারেই বসে, পরে আর বদলায় না) · `discountReason` · `discountBy` ·
     * `discountAt`। ইতিহাসের সারিটা এখান থেকেই তৈরি হয়
     * (`PatientTimelineRepository`) — তাই ছাড়ের কথা কোনোদিন হারাবে না।
     *
     * ⛔ ছাড় কখনো বাকি টাকার চেয়ে বেশি হতে দেওয়া হয় না — নইলে বিল জমার নিচে
     *    নেমে যেত আর Paid% ১০০-এর উপরে উঠত।
     */
    private fun showDiscountDialog() {
        val rowId = currentPatientRowId
        if (rowId.isBlank()) {
            android.widget.Toast.makeText(this, "No Registration record yet for this patient", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        if (currentBillTotal <= 0.0) {
            android.widget.Toast.makeText(this, "No bill on this patient yet", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val due = if (currentDue > 0.0) currentDue else 0.0
        if (due <= 0.0) {
            android.widget.Toast.makeText(this, "Nothing due — no discount needed", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val paidSoFar = (currentBillTotal - due).coerceAtLeast(0.0)
        fun money(v: Double) = "₹" + "%,.0f".format(v)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(0))
        }
        box.addView(android.widget.TextView(this).apply {
            text = "Bill " + money(currentBillTotal) + "   ·   Paid " + money(paidSoFar) + "   ·   Due " + money(due)
            textSize = 12.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#10223A"))
        })
        /* 🏷️🔒 V1213 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"ডিসকাউন্ট
           আমি চাইলে % অথবা Amount হিসাবে দিতে পারি তার ব্যবস্থা রাখতে হবে"*।
           ⇒ দুটো চিপ — **₹ Amount** (আগের মতোই) আর **% Percent**।
           ⛔ % দিলে অঙ্কটা **বিলের উপরে** কষা হয়, আর সঙ্গে সঙ্গে "= ₹x · y% of
              bill" দেখানো হয় — তাই কত টাকা ছাড় পড়ছে তা না দেখে সেভ করা যায় না।
           ⛔ শেষ পর্যন্ত যা জমা হয় তা **টাকার অঙ্কই** (আগের হুবহু একই ঘর), তাই
              হিসাব · ইতিহাস · ছাপা কোথাও কিছু বদলায় না।
           ⛔ "ছাড় বাকির চেয়ে বেশি নয়" — পুরনো পাহারাটা এক অক্ষরও বদলায়নি। */
        var pctMode = false
        val labelTv = android.widget.TextView(this).apply {
            text = "Discount amount"
            textSize = 11.5f
            setPadding(0, dp(14), 0, dp(2))
        }
        val amountInput = android.widget.EditText(this).apply {
            hint = "Discount amount"
            // B411 — শুধু TYPE_CLASS_NUMBER দিলে কিছু ফোনে কীবোর্ডই খোলে না;
            // প্রজেক্টের বাকি সব টাকার ঘরের মতো একই জোড়া ব্যবহার করা হলো।
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
            setText("%.0f".format(due))
        }
        val calcTv = android.widget.TextView(this).apply {
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0B5E2A"))
            setPadding(0, dp(6), 0, 0)
            visibility = android.view.View.GONE
        }
        /** 🏷️ V1213 — এই মুহূর্তে ছাড়ের আসল টাকার অঙ্ক। */
        fun discountValue(): Double {
            val n = amountInput.text.toString().filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            return if (!pctMode) n else Math.round(currentBillTotal * n / 100.0).toDouble()
        }
        fun refreshCalc() {
            if (!pctMode) { calcTv.visibility = android.view.View.GONE; return }
            val n = amountInput.text.toString().filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            calcTv.visibility = android.view.View.VISIBLE
            calcTv.text = "= " + money(discountValue()) + "   ·   " + "%.0f".format(n) + "% of bill"
        }
        val chipRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
        }
        lateinit var chipAmt: android.widget.TextView
        lateinit var chipPct: android.widget.TextView
        fun paintChips() {
            fun paint(t: android.widget.TextView, on: Boolean) {
                t.setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#0B5E2A"))
                t.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(android.graphics.Color.parseColor(if (on) "#0B7A34" else "#F4FBF6"))
                    setStroke(dp(2), android.graphics.Color.parseColor(if (on) "#0B7A34" else "#CFE3D4"))
                }
            }
            paint(chipAmt, !pctMode); paint(chipPct, pctMode)
        }
        fun chip(text: String): android.widget.TextView = android.widget.TextView(this).apply {
            this.text = text
            textSize = 13f
            gravity = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(9), 0, dp(9))
            isClickable = true; isFocusable = true
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        chipAmt = chip("₹ Amount")
        chipPct = chip("% Percent").apply {
            (layoutParams as android.widget.LinearLayout.LayoutParams).leftMargin = dp(10)
        }
        chipRow.addView(chipAmt); chipRow.addView(chipPct)
        chipAmt.setOnClickListener {
            if (!pctMode) return@setOnClickListener
            pctMode = false; paintChips()
            labelTv.text = "Discount amount"
            amountInput.hint = "Discount amount"
            amountInput.setText("%.0f".format(due))
            refreshCalc()
        }
        chipPct.setOnClickListener {
            if (pctMode) return@setOnClickListener
            pctMode = true; paintChips()
            labelTv.text = "Discount percent (%)"
            amountInput.hint = "Discount percent (%)"
            amountInput.setText("")
            refreshCalc()
        }
        paintChips()
        box.addView(android.widget.TextView(this).apply {
            text = NoBengali.s("কীভাবে ছাড় দেবেন")
            textSize = 11.5f
            setPadding(0, dp(14), 0, dp(2))
        })
        box.addView(chipRow)
        box.addView(labelTv)
        box.addView(amountInput)
        box.addView(calcTv)
        amountInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { refreshCalc() }
            override fun beforeTextChanged(s: CharSequence?, a2: Int, b2: Int, c2: Int) {}
            override fun onTextChanged(s: CharSequence?, a2: Int, b2: Int, c2: Int) {}
        })
        box.addView(android.widget.TextView(this).apply {
            text = "Reason"
            textSize = 11.5f
            setPadding(0, dp(14), 0, dp(2))
        })
        val reasonInput = android.widget.EditText(this).apply { hint = "Why this discount is given" }
        box.addView(reasonInput)
        box.addView(android.widget.TextView(this).apply {
            text = "The bill comes down by this amount, so the Due becomes 0 and the patient can be completed. The discount stays saved in this patient's history forever."
            textSize = 11f
            setPadding(0, dp(12), 0, dp(4))
            setTextColor(android.graphics.Color.parseColor("#8A5B00"))
        })
        UppercaseInputUtil.applyToAll(box)  // TK-REQUESTED GLOBAL RULE (2026-07-24)

        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "🏷️ Give Discount"))
            .setView(android.widget.ScrollView(this).apply { addView(box) })
            .setPositiveButton("Save Discount", null)
            .setNegativeButton("Cancel", null)
            .create()
        dlg.setOnShowListener {
            dlg.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                /* 🏷️ V1213 — % হলে বিলের উপরে কষা টাকাটাই যায়; ₹ হলে হুবহু আগের মতো। */
                val amount = discountValue()
                val why = reasonInput.text.toString().trim()
                if (amount <= 0.0) {
                    android.widget.Toast.makeText(this,
                        if (pctMode) "Write the discount percent" else "Write the discount amount",
                        android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (amount > due) {
                    android.widget.Toast.makeText(this, "Discount cannot be more than the Due (" + money(due) + ")", android.widget.Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (why.isBlank()) {
                    android.widget.Toast.makeText(this, "Write the reason", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val user = NativeSession.current(this)
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(PremiumAlert.header(this, "⚠️ Confirm discount"))
                    .setMessage(
                        money(amount) + " will be forgiven.\n\n" +
                        "Bill " + money(currentBillTotal) + " → " + money(currentBillTotal - amount) + "\n" +
                        "Due " + money(due) + " → " + money(due - amount) + "\n\n" +
                        "This stays in the patient's history forever. Go ahead?"
                    )
                    .setPositiveButton("Yes, give discount") { _, _ ->
                        lifecycleScope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                try {
                                    // আসল সারিটা আবার পড়া হয় — অন্য কেউ ইতিমধ্যে
                                    // ছাড় দিয়ে থাকলে বা টাকা বসিয়ে থাকলে যেন তার
                                    // উপরেই যোগ হয়, তার লেখা মুছে না যায়।
                                    /* 🟢🔒 V1275 (TK-নির্দেশ, Egress) — আগে সব ঘর
                                       নামত (base64 `photo`-সহ), অথচ নিচে পড়া হয়
                                       শুধু `bill` · `discount` · `billBeforeDiscount`।
                                       ⛔ ছাড়ের হিসাব এক অক্ষরও বদলায়নি। */
                                    val rows = SupabaseClient.fetchList("patients", "id=eq.$rowId", 1,
                                        select = "id,bill,discount,billBeforeDiscount")
                                    val row = if (rows.length() > 0) rows.getJSONObject(0) else null
                                    val liveBill = row?.optDouble("bill", currentBillTotal) ?: currentBillTotal
                                    val hadDiscount = row?.optDouble("discount", 0.0) ?: 0.0
                                    val origBill = row?.optDouble("billBeforeDiscount", 0.0) ?: 0.0
                                    if (liveBill - amount < 0.0) return@withContext false
                                    val fields = org.json.JSONObject()
                                        .put("bill", liveBill - amount)
                                        .put("discount", hadDiscount + amount)
                                        .put("billBeforeDiscount", if (origBill > 0.0) origBill else liveBill)
                                        .put("discountReason", why)
                                        .put("discountBy", user?.mobile ?: "")
                                        .put("discountAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                                            .format(java.util.Date()))
                                        .put("updatedAt", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                                            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                                            .format(java.util.Date()))
                                    SupabaseClient.updateById("patients", rowId, fields)
                                } catch (_: Throwable) { false }
                            }
                            android.widget.Toast.makeText(
                                this@PatientTimelineActivity,
                                if (ok) "✅ Discount saved" else "Failed — check connection and try again",
                                android.widget.Toast.LENGTH_LONG
                            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                            // load() নিজেই নতুন হিসাব নামিয়ে ক্যাশে বসায়
                            // (TimelineCache.save) — তাই আলাদা করে ক্যাশ মোছার
                            // দরকার নেই, আর ভুল করে ক্যাশ মুছে ফেলার ঝুঁকিও নেই।
                            if (ok) load(currentMobile, currentSection)
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show().also { PremiumAlert.paint(it) }
                dlg.dismiss()
            }
        }
        dlg.show()
        PremiumAlert.paint(dlg)
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
    }

    private fun showQuickNextFollowDialog() {
        if (currentFollowupId.isBlank()) {
            android.widget.Toast.makeText(this, "No Follow-up record yet for this patient", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(this, { _, y, m, dayOfMonth ->
            val picked = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, dayOfMonth)
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    FollowUpRepository(this@PatientTimelineActivity).updateNextFollow(resolveFollowUpIdHere(), picked)
                }
                // TK-DECISION (2026-07-22): saving a nextFollow date also
                // marks "আসার কথা" for that date -- written once here, never
                // recomputed on board load.
                //
                // TK-CORRECTED (2026-07-27): this used to run for every stage
                // except Enquiry, so a Visit-stage date also landed in আসার
                // কথা. TK's rule: only the PATIENT card (Treatment stage)
                // does that; on Enquiry and Visit the date only means "call
                // this patient on this day".
                // NEW (2026-08-07, TK-approved): Visit (stage "Patient") now ALSO
                // marks আসার কথা, not only Treatment — same rule as the Follow-up
                // screen. Enquiry ("Inquiry") is still excluded here (its "আসবে"
                // choice lives on the Follow-up screen).
                // OLD: if (ok && currentFollowupStage.equals("Treatment", ignoreCase = true)) {
                if (ok && (currentFollowupStage.equals("Treatment", ignoreCase = true) ||
                        currentFollowupStage.equals("Patient", ignoreCase = true))) {
                    val staffMobile = NativeSession.current(this@PatientTimelineActivity)?.mobile ?: ""
                    withContext(Dispatchers.IO) {
                        ChamberAttendanceRepository.markExpected(
                            this@PatientTimelineActivity, currentMobile, currentPatientName, currentBranch, picked, staffMobile
                        )
                    }
                }
                android.widget.Toast.makeText(
                    this@PatientTimelineActivity,
                    if (ok) "Next Follow-up updated" else "Failed — check connection",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                if (ok) load(currentMobile, currentSection)
            }
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
            .apply { datePicker.minDate = cal.timeInMillis }
            .show()
    }

    /**
     * TK-REPORTED (2026-07-27): "ইনকমপ্লিট করলাম, তারপরেও ভিজিট কার্ডে রয়ে গেছে।"
     *
     * ROOT CAUSE -- the same one already fixed on the Follow-up screen
     * (FollowUpActivity.resolveFollowUpId, 2026-07-24), but never applied
     * here. Supabase's PATCH-by-id answers HTTP 200 even when it matched
     * ZERO rows, so a status/remark write aimed at an id that is NOT a real
     * "followups" id reports "Saved" and quietly changes nothing. This
     * screen's `currentFollowupId` can legitimately be a synthesized card's
     * id (a patients/enquiries row id) whenever the real followups row was
     * missing from the fetch -- exactly the case in TK's photo-proof -- so
     * Incomplete said "Saved" and the patient stayed on the Visit card.
     *
     * FIX: before any followups write, look the REAL row up by this
     * patient's mobile + stage (the same safe lookup FollowUpActivity uses)
     * and write to that. The current id is still preferred when it really
     * is a followups row, and if the lookup cannot run (offline, no stage
     * yet) the old id is returned unchanged -- so this can only ever find
     * the right row, never lose one.
     */
    private fun resolveFollowUpIdHere(): String {
        // 🚨 TK-REPORTED, LIVE (29.07.2026 রাত ৮.১০, ছবিসহ · খাতার সারি B96):
        // *"Reject করছি এবং ডিলিট করছি — কোনটাতেই কোনো কাজ হয় না।"*
        //
        // **আসল কারণ:** এটা **খাতার সারি B78-এর হুবহু একই রোগ**। নিচের পুরনো
        // কোডটা সারিটা না পেলে **এনকোয়ারির আইডিটাই** ফেরত দিত; তারপর
        // `followups?id=eq.<এনকোয়ারির আইডি>`-তে বদল পাঠানো হত — **কোনো সারির
        // সঙ্গে মেলে না, অথচ Supabase "200 OK" বলে**, তাই অ্যাপ ভাবত কাজ হয়ে
        // গেছে আর আসলে কিছুই হত না। "Enquiry only — not registered" রেকর্ডে
        // (যেখানে `followups` সারিটা কখনো তৈরিই হয়নি) এটা **প্রতিবার** ঘটত।
        //
        // B78-এর ওষুধটা তখন Follow-up ও Follow-up ক্যালেন্ডারে বসানো হয়েছিল,
        // **এই পর্দায় বসানো হয়নি** — সেটাই এখন বসল। ⛔ ঠিক সেই একই ফাংশন,
        // তাই তিন পর্দায় আচরণ কখনো আলাদা হবে না।
        // ⛔ নেট খারাপ হলে আগের মতোই পুরনো আইডি ফেরত যায় — কিছু ভাঙে না।
        return try {
            val digits = currentMobile.filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10) return currentFollowupId
            // এই আইডিটা কি সত্যিই একটা followups সারি? হলে সেটাই।
            if (currentFollowupId.isNotBlank()) {
                val self = SupabaseClient.fetchList("followups", "id=eq.$currentFollowupId", 1, select = "id")
                if (self.length() > 0) return currentFollowupId
            }
            FollowUpRepository(this).ensureFollowUpRowIdFor(
                mobile = currentMobile,
                stage = currentFollowupStage,
                name = currentPatientName,
                branch = currentBranch,
                disease = currentDisease,
                recordDate = currentEnquiryDate,
                fallbackId = currentFollowupId
            )
        } catch (_: Throwable) {
            currentFollowupId
        }
    }

    /** Reject/Incomplete — reuses FollowUpRepository.updateStatus(), the
     *  EXACT same function FollowUpActivity's own entryActionMenu() already
     *  uses for this (Inquiry/Patient stage → Cancelled = "Reject";
     *  Treatment stage → Incomplete). Nothing new invented here. */
    private fun confirmStatusChange(isTreatmentStage: Boolean) {
        if (currentFollowupId.isBlank()) {
            android.widget.Toast.makeText(this, "No Follow-up record yet for this patient", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        // 🔒🔒 V217 (§B216, Master Fix Order §14, item 1): "একই দিনের ভুল হলে
        // Staff Master Admin Approval ছাড়াই Reject করতে পারবে" — অর্থাৎ
        // Reject-ও এখন Same-Day বিশেষ নিয়মে (B98-এর সাধারণ নিয়মের বাইরে,
        // ঠিক TK-এর কথা মতো)। আজ/গতকালের এন্ট্রি, নিজের ব্রাঞ্চ, চেম্বার বন্ধ
        // না থাকলে সঙ্গে সঙ্গে Reject/Incomplete হয়ে যায় (আগের মতোই)।
        // পুরনো এন্ট্রি হলে এখন সরাসরি না করে Master-কে ঘন্টায় জানানো হয় —
        // Master নিজে খুলে Reject করবেন (Master সবসময় করতে পারেন, আগের মতোই)।
        val user = NativeSession.current(this)
        if (user == null) return
        // 🆕 TK-নির্দেশ (03.08.2026) — "যে ব্যক্তি এখনো Patient হননি (কোনো টাকা
        // জমা পড়েনি — Visit Fee-ও না), তাঁর Enquiry/Visit Reject করতে Master-এর
        // অনুমতি লাগার দরকার নেই — স্টাফ নিজেই যেকোনো সময় করতে পারবেন। শুধু
        // Patient (Treatment stage, Advance/Treatment টাকা জমা হয়েছে)
        // Reject/Incomplete করতে হলে Master-এর অনুমতি লাগবে, আগের নিয়মেই।"
        // ⛔ নিচের canDeleteEntryNow (তারিখ/চেম্বার-বন্ধ) চেকটা তাই শুধু
        // isTreatmentStage-এর ক্ষেত্রেই হয় — Enquiry/Visit সবসময় সরাসরি চলে।
        if (!isTreatmentStage) {
            showStatusChangeDialog(isTreatmentStage)
            return
        }
        // 🔒🔒 V217 (§B216, Master Fix Order §14, item 1): "একই দিনের ভুল হলে
        // Staff Master Admin Approval ছাড়াই Reject করতে পারবে" — অর্থাৎ
        // Reject-ও এখন Same-Day বিশেষ নিয়মে (B98-এর সাধারণ নিয়মের বাইরে,
        // ঠিক TK-এর কথা মতো)। আজ/গতকালের এন্ট্রি, নিজের ব্রাঞ্চ, চেম্বার বন্ধ
        // না থাকলে সঙ্গে সঙ্গে Reject/Incomplete হয়ে যায় (আগের মতোই)।
        // পুরনো এন্ট্রি হলে এখন সরাসরি না করে Master-কে ঘন্টায় জানানো হয় —
        // Master নিজে খুলে Reject করবেন (Master সবসময় করতে পারেন, আগের মতোই)।
        // 🔴 V217 self-audit fix (31.07.2026): আগে এখানে ভুল করে শুধু
        // isTreatmentStage দেখে তারিখ বাছা হচ্ছিল — কিন্তু "Reject" বোতাম
        // Enquiry-stage ও Visit-stage (রেজিস্টার্ড কিন্তু Treatment নয়)
        // দুটোতেই দেখা যায়। Visit-stage হলে currentEnquiryDate ভুল (অনেক
        // পুরনো হতে পারে, রেজিস্ট্রেশন যেদিন হয়েছে সেদিনের সাথে সম্পর্কহীন)
        // — তাতে আজকের Visit ভুল করে "পুরনো" ধরে Master-অনুমতি চাইত।
        // এখন ঠিক সেই একই isRegistered নিয়ম (উপরের showTakeActionMenu-এর
        // মতো) — রেজিস্টার্ড হলে registration date, নইলে enquiry date।
        val isRegisteredNow = currentPatientRowId.isNotBlank() ||
            currentFollowupStage.equals("Patient", ignoreCase = true) ||
            currentFollowupStage.equals("Treatment", ignoreCase = true)
        val entryDate = if (isRegisteredNow) currentRegistrationDate else currentEnquiryDate
        lifecycleScope.launch {
            val allowedNow = withContext(Dispatchers.IO) {
                try { DeletePermission.canDeleteEntryNow(this@PatientTimelineActivity, user, entryDate, currentBranch, paid = isTreatmentStage) }
                catch (_: Throwable) { false }
            }
            if (!allowedNow) {
                val what = if (isTreatmentStage) "Incomplete Patient" else "Reject Enquiry/Visit"
                androidx.appcompat.app.AlertDialog.Builder(this@PatientTimelineActivity)
                    .setCustomTitle(PremiumAlert.header(this@PatientTimelineActivity, "Master approval needed"))
                    .setMessage(
                        "${currentPatientName.ifBlank { currentMobile }}\n\n" +
                        "This is an old entry — only Master can Reject/Incomplete it now.\n" +
                        "Master will be notified in the bell.\n\n⛔ Nothing changes yet."
                    )
                    .setPositiveButton("Notify Master") { _, _ ->
                        lifecycleScope.launch {
                            val sent = withContext(Dispatchers.IO) {
                                DeletePermission.sendRequest(
                                    this@PatientTimelineActivity, user, what,
                                    currentPatientName, currentMobile, currentPatientCode, currentBranch,
                                    disease = currentDisease
                                )
                            }
                            android.widget.Toast.makeText(
                                this@PatientTimelineActivity,
                                NoBengali.s(DeletePermission.lastMessage()),   /* 🔁 V1176 — বার্তাটা এখন `DeletePermission` নিজেই ঠিক করে দেয় (পাঠানো হলো · আগেই পাঠানো · ব্যর্থ)। */
                                android.widget.Toast.LENGTH_LONG
                            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show().also { PremiumAlert.paint(it) }
                return@launch
            }
            showStatusChangeDialog(isTreatmentStage)
        }
    }

    private fun showStatusChangeDialog(isTreatmentStage: Boolean) {
        val status = if (isTreatmentStage) "Incomplete" else "Cancelled"
        val remark = if (isTreatmentStage) "Marked Incomplete" else "Rejected"
        val title = if (isTreatmentStage) "Mark Incomplete?" else "Reject this Enquiry?"
        val listName = if (isTreatmentStage) "Draft's \"Incomplete Patient\" list" else "Draft's \"Enquiry Reject List\""
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, title))
            .setMessage("${currentPatientName.ifBlank { currentMobile }} — this moves to $listName. You can restore it from there anytime.")
            .setPositiveButton("Confirm") { _, _ ->
                lifecycleScope.launch {
                    val staffMobile = NativeSession.current(this@PatientTimelineActivity)?.mobile ?: ""
                    // V215 (§16/§6): সত্যিকারের cloud-confirm আলাদা করে জানা হয়।
                    val cloudConfirmed = booleanArrayOf(false)
                    val ok = withContext(Dispatchers.IO) {
                        val repo = FollowUpRepository(this@PatientTimelineActivity)
                        repo.updateStatus(
                            resolveFollowUpIdHere(), status, remark, staffMobile, cloudConfirmed,
                            mobileHint = currentMobile, stageHint = currentFollowupStage
                        )
                    }
                    // V215 (§16.7/§6.9): cloud confirm না হলে মিথ্যা "Saved" নয় —
                    // পরিষ্কার বলা হয় এই ফোনে সেভ, cloud যাওয়া বাকি (নিজে থেকেই যাবে)।
                    val msg = when {
                        !ok -> "Failed — check connection"
                        cloudConfirmed[0] -> if (isTreatmentStage) "Marked Incomplete ✓" else "Rejected ✓"
                        else -> "Saved on this phone — Cloud confirmation pending"
                    }
                    android.widget.Toast.makeText(this@PatientTimelineActivity, msg, android.widget.Toast.LENGTH_SHORT).show()
                    // V215 (§16.1-16.5): সফল হলে record active/source list থেকে সরে
                    // যায় (locally-pending Incomplete/Cancelled তালিকা-query থেকে বাদ),
                    // আর এই Timeline-এ আটকে না থেকে সরাসরি আগের Source List-এ ফেরে।
                    // finish() → যেখান থেকে খোলা হয়েছিল (Follow-up/Queue) সেই তালিকা,
                    // যা onResume-এ নতুন করে load করে count আপডেট করে।
                    if (ok) finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // TK-REQUESTED ADDITION (2026-07-18): Delete, extended from Draft's
    // Enquiry-only Delete to also work here — same TrashHelper, same
    // "moves to Trash Bin, Master can Restore" safety, permission
    // re-checked here too (not just in the menu) so it can never be
    // called successfully out of turn.
    private fun confirmDeleteEnquiry(user: NativeUser) {
        // 🔒🔒 V217 (§B216, Master Fix Order §14): B98-এর সাধারণ Master-only
        // নিয়মের বদলে এখন **Same-Day বিশেষ নিয়ম** (Payment-এ যে নিয়ম B112-এ
        // আগে থেকেই আছে, ঠিক সেটাই — `DeletePermission.canDeleteEntryNow`) —
        // আজ/গতকালের এন্ট্রি, নিজের ব্রাঞ্চ, আর সেই দিনের চেম্বার বন্ধ না হলে
        // Staff নিজেই মুছতে পারবেন (Trash-এ যায়, Master Restore করতে পারবেন,
        // Permanent delete নয়)। পুরনো এন্ট্রি হলে আগের মতোই Master-এর
        // অনুমতি লাগবে। ⚠️ চেম্বার-বন্ধ যাচাই ক্লাউড ছুঁতে পারে, তাই
        // ব্যাকগ্রাউন্ডে (সতর্কবার্তার ৭ নম্বর নিয়ম)।
        lifecycleScope.launch {
            val allowedNow = withContext(Dispatchers.IO) {
                try { DeletePermission.canDeleteEntryNow(this@PatientTimelineActivity, user, currentEnquiryDate, currentBranch, paid = false) }
                catch (_: Throwable) { false }
            }
            if (!allowedNow) { askMasterToDelete(user, "Enquiry"); return@launch }
            showDeleteEnquiryDialog(user)
        }
    }

    private fun showDeleteEnquiryDialog(user: NativeUser) {
        // 🔴 B334 (03.08.2026, একই ক্লাসের বাগ) — প্রথম চাপেই বোতাম বন্ধ,
        // একাধিক ডিলিট-কল (এখানে Follow-up cascade-সহ) আটকাতে।
        val delDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Delete Enquiry?"))
            .setMessage("${currentPatientName.ifBlank { currentMobile }} — this will move to Trash Bin (Master can restore it from there).")
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
        delDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            delDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
            delDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        // 🔒 V217: চূড়ান্ত পাহারা এখানেও আবার (একই নিয়ম) — মাঝখানে
                        // সময় গড়িয়ে চেম্বার বন্ধ হয়ে গেলেও যেন ফাঁক না থাকে।
                        if (!DeletePermission.canDeleteEntryNow(this@PatientTimelineActivity, user, currentEnquiryDate, currentBranch, paid = false)) return@withContext "PERMISSION"
                        // 🔒 খাতার সারি B79 (TK, 29.07.2026: *"আর যদি ডিলিট করা
                        // হয়?"*): আগে কার্ডের আইডির উপরেই ভরসা করা হত — আইডিটা
                        // না জানা থাকলে (ধীর লাইনে আগের পড়াটা ব্যর্থ হলে) Delete
                        // শুধু "Record not found" বলত, আর স্টাফ কিছুতেই রেকর্ডটা
                        // মুছতে পারতেন না। এখন আইডি না থাকলে বা ওই আইডির সারি না
                        // মিললে **মোবাইল ধরে** খোঁজা হয় — অ্যাপের বাকি সব জায়গার
                        // সেই একই `findByMobile`। ⛔ এটা ঠিক সেই একই পাহারা যা
                        // `confirmDeletePatient()`-এ 27.07.2026-এ বসানো হয়েছিল।
                        // ⛔ অনুমতির যাচাই আগের মতোই উপরে আছে, আর সারি সত্যিই না
                        // পাওয়া গেলে আগের মতোই "Record not found"।
                        val rows = if (currentEnquiryId.isNotBlank()) {
                            val byId = SupabaseClient.fetchList("enquiries", "id=eq.$currentEnquiryId", 1)
                            if (byId.length() > 0) byId
                            else SupabaseClient.findByMobile("enquiries", currentMobile, "*", 1)
                        } else {
                            SupabaseClient.findByMobile("enquiries", currentMobile, "*", 1)
                        }
                        if (rows.length() == 0) return@withContext "NOT_FOUND"
                        if (TrashHelper.moveToTrashWithFollowupCascade("enquiries", rows.getJSONObject(0), user.mobile, currentMobile)) "OK" else "NETWORK"
                    }
                    // 🔒🔒 খাতার সারি B224 (TK verified live-test, 01.08.2026): এই সারি
                    // cloud-এ id ও mobile দুভাবেই সত্যিই না-পাওয়া গেছে (NOT_FOUND)।
                    // আগে শুধু "Record not found" দেখিয়ে এই Detail Screen খোলা রেখে
                    // দিত, আর Incomplete তালিকায় সারিটা থেকে যেত। এখন এই ফোনের
                    // জমানো ছায়া-কপি মুছে (purgeGhostFromCache) Detail Screen বন্ধ
                    // করা হয়। ⛔ কোনো cloud tombstone নয় — অন্য ফোনের এখনো sync
                    // না-হওয়া আসল রেকর্ড "not found" পাওয়ার কারণে স্থায়ীভাবে মুছবে না।
                    if (result == "NOT_FOUND") {
                        withContext(Dispatchers.IO) { DraftRepository(this@PatientTimelineActivity).purgeGhostFromCache(currentEnquiryId, currentMobile) }
                        android.widget.Toast.makeText(this@PatientTimelineActivity, "Already deleted — this record is gone", android.widget.Toast.LENGTH_SHORT).show()
                        delDialog.dismiss()
                        finish()
                        return@launch
                    }
                    val msg = when (result) {
                        "OK" -> "Deleted — moved to Trash Bin"
                        "PERMISSION" -> "You don't have permission to delete this"
                        else -> "Could not delete — network is too slow/unstable, please retry"
                    }
                    android.widget.Toast.makeText(this@PatientTimelineActivity, msg, android.widget.Toast.LENGTH_SHORT).show()
                    delDialog.dismiss()
                    if (result == "OK") finish()
                }
        }
    }

    /**
     * 🔒 খাতার সারি B98 — স্টাফের ডিলিট-অনুরোধ মাস্টারের ঘন্টায় পাঠায়।
     * ⛔ এই মুহূর্তে **কিছুই মোছে না**; মাস্টার নিজে দেখে তবেই মুছবেন।
     */
    private fun askMasterToDelete(user: NativeUser, what: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Master's approval needed"))
            .setMessage(
                "${currentPatientName.ifBlank { currentMobile }}\n\n" +
                "ডিলিট করতে পারেন শুধু Master Admin।\n" +
                "অনুরোধ পাঠালে সেটা Master-এর ঘন্টায় যাবে — তিনি অনুমতি দিলে তবেই মুছবে।\n\n" +
                "⛔ এখনই কিছুই মুছবে না।"
            )
            .setPositiveButton("Send Request") { _, _ ->
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        DeletePermission.sendRequest(
                            this@PatientTimelineActivity, user, what,
                            currentPatientName, currentMobile, currentPatientCode, currentBranch,
                            disease = currentDisease
                        )
                    }
                    android.widget.Toast.makeText(
                        this@PatientTimelineActivity, NoBengali.s(DeletePermission.lastMessage()),   /* 🔁 V1176 — বার্তাটা এখন `DeletePermission` নিজেই ঠিক করে দেয় (পাঠানো হলো · আগেই পাঠানো · ব্যর্থ)। */
                        android.widget.Toast.LENGTH_LONG
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun confirmDeletePatient(user: NativeUser, isTreatmentStage: Boolean) {
        // 🔴🔴🔒 খাতার সারি B467 (TK-নির্দেশ, 06.08.2026 — B443-এর কড়া
        // নিয়ম আজ TK নিজেই বদলে দিয়েছেন, এটাই এখন চূড়ান্ত): Enquiry/Visit
        // (টাকা জমা পড়েনি) সবসময় স্টাফ নিজে পারবেন। টাকা জমা পড়া
        // (Treatment stage) এন্ট্রিও **আজকের দিন, নিজের OUT TIME সেভ করার
        // আগ পর্যন্ত** স্টাফ নিজে পারবেন — তারপর (বা পুরনো দিন হলে, বা
        // চেম্বার বন্ধ হয়ে গেলে) Master-এর অনুমতি লাগবে। দুটো ক্ষেত্রেই
        // এখন একই `DeletePermission.canDeleteEntryNow(..., paid=...)`
        // ফাংশন — `isTreatmentStage` সরাসরি `paid` হিসেবে পাঠানো হয়।
        lifecycleScope.launch {
            val allowedNow = withContext(Dispatchers.IO) {
                try { DeletePermission.canDeleteEntryNow(this@PatientTimelineActivity, user, currentRegistrationDate, currentBranch, paid = isTreatmentStage) }
                catch (_: Throwable) { false }
            }
            if (!allowedNow) {
                askMasterToDelete(user, if (isTreatmentStage) "Patient" else "Registration")
                return@launch
            }
            showDeletePatientDialog(user, isTreatmentStage)
        }
    }

    private fun showDeletePatientDialog(user: NativeUser, isTreatmentStage: Boolean) {
        // TK-DECISION (2026-07-24): title/message now reflect which label
        // opened this (Registration Cancel vs Delete Patient) -- the
        // Trash-move, permission check, and Payment-safety below are
        // completely unchanged either way.
        val title = if (isTreatmentStage) "Delete Patient?" else "Cancel this Registration?"
        val verb = if (isTreatmentStage) "delete" else "cancel"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, title))
            .setMessage("${currentPatientName.ifBlank { currentMobile }} — this will move to Trash Bin (Master can restore it from there). Their Follow-up/Payment history stays intact and returns with them if restored.")
            .setPositiveButton(if (isTreatmentStage) "Delete" else "Cancel Registration") { _, _ ->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        // 🔒 V217: চূড়ান্ত পাহারা এখানেও আবার — একই নিয়ম, মাঝের
                        // সময়ে চেম্বার বন্ধ হয়ে গেলেও ফাঁক না থাকে।
                        if (!DeletePermission.canDeleteEntryNow(this@PatientTimelineActivity, user, currentRegistrationDate, currentBranch, paid = isTreatmentStage)) return@withContext "PERMISSION"
                        // 🔴🔴 খাতার সারি B191 (TK, 30.07.2026 সন্ধ্যা, ছবিসহ — Master
                        // Admin হয়েও Delete চাপলে "Record not found" আসছিল, একটু
                        // পরেই Draft-এর Incomplete তালিকায় সেই একই রোগীকে দেখা
                        // যাচ্ছিল): *"এগুলো ডিলিট কেন করতে পারছি না, আমি তো মাস্টার
                        // এডমিন।"*
                        //
                        // **আসল কারণ (কোড ধরে, ঠিক উপরের `confirmDeleteEnquiry()`-এর
                        // সঙ্গে মিলিয়ে):** `currentPatientRowId` ফাঁকা না থাকলে
                        // (মানে একটা আইডি জানা আছে) কোড **শুধু সেই আইডি দিয়েই**
                        // খুঁজত — আইডিটা পুরনো/না-মেলা হলে (যেমন অফলাইনে সেভ হওয়া
                        // রেকর্ড পরে ক্লাউডে ভিন্ন আইডি নিয়ে গেলে) সারি না পেয়ে
                        // সঙ্গে সঙ্গে "NOT_FOUND" ফিরিয়ে দিত — **মোবাইল দিয়ে
                        // খোঁজার ব্যবস্থাটাই কখনো চলত না**। ঠিক উপরের
                        // `confirmDeleteEnquiry()`-এ (এই একই ফাইলে) এই ফলব্যাকটা
                        // আগে থেকেই ঠিকভাবে বসানো আছে — সেটা মিস করে এখানে বাদ
                        // পড়ে গিয়েছিল।
                        //
                        // ⛔ **সমাধান:** আইডি দিয়ে খুঁজে না পেলে এখন এখানেও মোবাইল
                        // দিয়ে (অ্যাপের বাকি সব জায়গার সেই একই `findByMobile`)
                        // আরেকবার খোঁজা হয় — তবেই সত্যিই না পেলে "Record not
                        // found"। ⛔ অনুমতির যাচাই, Trash-এ সরানো, Payment/History
                        // অক্ষত রাখা — কিছুই বদলায়নি।
                        val rows = if (currentPatientRowId.isNotBlank()) {
                            val byId = SupabaseClient.fetchListSlim("patients", "id=eq.$currentPatientRowId", 1,
                                        SupabaseClient.PATIENT_NO_PHOTO_COLS)   // 🔴 V794 — ছবি ছাড়া
                            if (byId.length() > 0) byId
                            else SupabaseClient.findByMobile("patients", currentMobile, "*", 1)
                        } else {
                            SupabaseClient.findByMobile("patients", currentMobile, "*", 1)
                        }
                        if (rows.length() == 0) return@withContext "NOT_FOUND"
                        if (TrashHelper.moveToTrashWithFollowupCascade("patients", rows.getJSONObject(0), user.mobile, currentMobile)) "OK" else "NETWORK"
                    }
                    // 🔒🔒 খাতার সারি B224 (TK verified live-test, 01.08.2026): উপরের
                    // Enquiry-Delete পথের সঙ্গে **হুবহু একই** আচরণ — cloud-এ id ও
                    // mobile দুভাবেই সত্যিই না-পাওয়া সারি (NOT_FOUND) এখন এই ফোনের
                    // জমানো ছায়া-কপি থেকে সরিয়ে Detail Screen বন্ধ করা হয়। ⛔ কোনো
                    // cloud tombstone নয় — অন্য ফোনের এখনো sync না-হওয়া আসল রেকর্ড
                    // "not found" পাওয়ার কারণে স্থায়ীভাবে মুছবে না।
                    if (result == "NOT_FOUND") {
                        withContext(Dispatchers.IO) { DraftRepository(this@PatientTimelineActivity).purgeGhostFromCache(currentPatientRowId, currentMobile) }
                        android.widget.Toast.makeText(this@PatientTimelineActivity, "Already deleted — this record is gone", android.widget.Toast.LENGTH_SHORT).show()
                        finish()
                        return@launch
                    }
                    val msg = when (result) {
                        "OK" -> if (isTreatmentStage) "Deleted — moved to Trash Bin" else "Registration cancelled — moved to Trash Bin"
                        "PERMISSION" -> "You don't have permission to $verb this"
                        else -> "Could not $verb — network is too slow/unstable, please retry"
                    }
                    android.widget.Toast.makeText(this@PatientTimelineActivity, msg, android.widget.Toast.LENGTH_SHORT).show()
                    if (result == "OK") finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Complete despite Due" -- Staff
    // side. Nothing changes yet, just records the request; Master must
    // Approve for it to actually take effect (see approveCompleteDespiteDue).
    private fun requestCompleteDespiteDue(user: NativeUser) {
        if (currentPatientRowId.isBlank()) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, NoBengali.s("Send complete request?")))
            .setMessage(NoBengali.s("${currentPatientName.ifBlank { currentMobile }}\n\nDue ₹${"%,.0f".format(currentDue)} stays unchanged."))
            .setPositiveButton("Send Request") { _, _ ->
                lifecycleScope.launch {
                    val fields = org.json.JSONObject().put("completeRequestedBy", user.mobile)
                    val ok = withContext(Dispatchers.IO) { SupabaseClient.updateById("patients", currentPatientRowId, fields) }
                    android.widget.Toast.makeText(
                        this@PatientTimelineActivity, NoBengali.s(if (ok) "Request sent to Master" else "Failed — check network"),
                        android.widget.Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) load(currentMobile, currentSection)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Complete despite Due" -- Master
    // side (also used when Master presses the button directly, skipping the
    // request step). Sets completeApprovedBy and clears any pending
    // completeRequestedBy; the real Due/bill/paid fields are NEVER touched
    // here -- Reports/collections keep showing the true numbers. Only
    // DraftRepository's "Complete Patient" bucket and FollowUpRepository's
    // Treatment-tab exclusion look at completeApprovedBy.
    private fun approveCompleteDespiteDue(user: NativeUser) {
        if (currentPatientRowId.isBlank()) return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, NoBengali.s("Approve complete?")))
            .setMessage(NoBengali.s("${currentPatientName.ifBlank { currentMobile }}\n\nDue ₹${"%,.0f".format(currentDue)} stays unchanged."))
            .setPositiveButton("Approve") { _, _ ->
                lifecycleScope.launch {
                    val fields = org.json.JSONObject().put("completeApprovedBy", user.mobile).put("completeRequestedBy", "")
                    val ok = withContext(Dispatchers.IO) { SupabaseClient.updateById("patients", currentPatientRowId, fields) }
                    android.widget.Toast.makeText(
                        this@PatientTimelineActivity, NoBengali.s(if (ok) "Approved — moved to Complete Patient" else "Failed — check network"),
                        android.widget.Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) load(currentMobile, currentSection)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // TK-REQUESTED ADDITION (2026-07-24): "Complete despite Due" -- Master
    // rejects a pending request. Just clears completeRequestedBy; nothing
    // else changes, patient stays exactly where they were (still in the
    // active Treatment/Follow-up tab, Due unchanged).
    private fun rejectCompleteDespiteDue() {
        if (currentPatientRowId.isBlank()) return
        lifecycleScope.launch {
            val fields = org.json.JSONObject().put("completeRequestedBy", "")
            val ok = withContext(Dispatchers.IO) { SupabaseClient.updateById("patients", currentPatientRowId, fields) }
            android.widget.Toast.makeText(
                this@PatientTimelineActivity, NoBengali.s(if (ok) "Request rejected" else "Failed — check network"),
                android.widget.Toast.LENGTH_SHORT
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            if (ok) load(currentMobile, currentSection)
        }
    }

    // ---------------- Enquiry & Call History (Enquiry stage only) ----------------

    private fun dpx(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun isUnexpectedHour(iso: String): Boolean {
        if (iso.length < 16) return false
        return try {
            val hour = iso.substring(11, 13).toInt()
            // 🔒 গ্লোবাল রুল (TK, 28.07.2026, খাতার সারি B32): এই ৯টা—৬টা হলো
            // **স্টাফের ডিউটি টাইম** — ডিউটির বাইরে করা কল লাল দেখানোর জন্য।
            // ⛔ এটা রোগীর সময় নয় এবং রোগীর কাছে কখনো যায় না। রোগীর সময়
            // **সকাল ১১টা — বিকেল ৪টা** (PatientMessage দেখুন)। দুটো আলাদা,
            // কখনো মেলানো যাবে না।
            hour < 9 || hour >= 18
        } catch (_: Exception) { false }
    }

    // 🔒 GLOBAL RULE — সময় (TK, 29.07.2026 সকাল ১০.০০, খাতার সারি B76):
    // সময় সবসময় **`11.30 AM`** ধাঁচে — h.mm, আর AM/PM **বড় হাতের অক্ষরে**।
    // (24.07.2026-এর পুরনো নিয়মে ছোট হাতের `am/pm` ছিল; TK নিজে বদলেছেন।)
    // BUG FIX (2026-07-26, full-project audit): this still
    // emitted 24-hour "17:40". Display-only; the stored callTime string, the
    // isUnexpectedHour() red-highlight check and every sort/compare all
    // still read the raw ISO value and are untouched.
    private fun displayTime(iso: String): String {
        if (iso.length < 16) return "\u2014"
        return try {
            val sdf = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US)
            val d = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).parse(iso)
            sdf.timeZone = java.util.TimeZone.getDefault()
            sdf.format(d ?: return "\u2014")
        } catch (_: Exception) { "\u2014" }
    }

    // GLOBAL RULE (2026-07-24, Locked): every date shown to the user reads
    // 31.12.2026 (dots), never 31/12/2026 and never raw ISO. BUG FIX
    // (2026-07-26, full-project audit): this helper still emitted slashes.
    // Display-only -- stored/sorted/compared values are untouched.
    private fun displayDate(iso: String): String {
        val parts = iso.take(10).split("-")
        return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else iso
    }

    // TK-LOCKED DESIGN (2026-07-23): today's colourful grid look (green
    // header, bordered cells, category-coloured Type text) applied to this
    // EXISTING table -- Date/Time/By/Note/callTime/isUnexpectedHour/3-tap
    // edit logic are all unchanged, only the visual rendering + a new Type
    // column (reusing the same colorHex already computed per entry) are new.
    private fun cellBorderDrawable(): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.WHITE)
            setStroke(1, android.graphics.Color.parseColor("#D9E2EC"))
        }

    private fun buildEnquiryHistoryTable(data: TimelineData) {
        val box = binding.enquiryHistoryContainer
        box.removeAllViews()

        /* 🟢🔒 V1118 (০৫.০৯.২০২৬, TK-নির্দেশ "হ্যাঁ করুন, সাবধানে") —
           TK: *"কোন পেশেন্টের এনকোয়ারি ডিটেইলস দেখাচ্ছে না কেন? শুধুমাত্র রেজিস্ট্রেশন
           থেকে শুরু হয়েছে কেন? রেজিস্ট্রেশনের আগে অনেকবার কল করা হয়েছে, সেগুলোর
           হিস্টোরি কেন নেই? আগে তো দেখাতো"*

           🔴 **কারণ (কোডে মেপে পাওয়া):** ২৪.০৭.২০২৬-এ এখানে একটা ছাঁকনি বসেছিল —
           Visit কার্ডে (stage `Patient`) Registration-এর আগের সব সারি বাদ যেত। ছাঁকনি
           খোলার একমাত্র চাবি ছিল "Full Journey" বোতাম, কিন্তু **ঠিক ওই একই দিনে**
           Visit ও Enquiry কার্ড থেকে বোতামটাও তুলে দেওয়া হয়েছিল ⇒ ছাঁকনি রয়ে গেল,
           খোলার পথটা রইল না। ⇒ **ছাঁকনিটা তুলে দেওয়া হলো** — এখন প্রতিটা কার্ডে
           (Enquiry · Visit · Patient/Treatment) পুরো A-to-Z হিস্ট্রি দেখায়, ঠিক যেমন
           **ওয়েবে আগে থেকেই দেখাত** (ওখানে এই ছাঁকনি কখনো বসানোই হয়নি) — দুই পর্দা এখন এক।

           ⛔ ডেটায় এক অক্ষরও হাত দেওয়া হয়নি — `PatientTimelineRepository` আগেও প্রতিটা
           `followups` সারির `history` পড়ত, এখনও পড়ে। শুধু দেখানোর ছাঁকনিটা গেল।
           ⛔ কোনো বাড়তি ক্লাউড-কল নেই (Supabase free-plan নিরাপদ) — এই সারিগুলো আগেই
           `data.entries`-এ এসে বসে আছে, শুধু পর্দায় আসত না।
           ⛔ উপরের গোনার পট্টি (Enquiry Calls / Visit Calls / Payments) এই একই `list` থেকেই
           গোনে (২৪.০৭-এর TK-সিদ্ধান্ত), তাই পট্টিতে এখন "Enquiry Calls"-ও ঠিক দেখাবে —
           পর্দার সংখ্যা আর তালিকা একই রইল।
           ⛔ `forceFullJourney` পতাকাটা তোলা হয়নি — Treatment Summary কার্ড ও Doctor Queue-এর
           "Journey" বোতাম আগের মতোই চলে। */
        val list = data.entries

        // 🔴 TK-নির্দেশ (04.08.2026, আলোচনার পরে -- CHECK-UP Queue-এর
        // "📜 History" বোতাম, TK একমত): প্রথমবার আসা রোগীর (আগে কখনো
        // Doctor Checkup হয়নি) জন্য আগে যা দেখাত (কল/এনকোয়ারি হিস্ট্রি,
        // উপরে) তাই -- এই ব্লক কিছুই বদলায় না তখন। কিন্তু দ্বিতীয়বার বা
        // তার বেশিবার আসা রোগীর (আগে অন্তত একবার Doctor Checkup হয়ে
        // গেছে) জন্য Full Journey-তে সবার উপরে একটা "Treatment Summary"
        // কার্ড দেখানো হয় -- রোগ, কতদিন থেকে, আগে চিকিৎসা হয়েছিল কিনা,
        // তার ফলাফল কী ছিল। কল/এনকোয়ারি হিস্ট্রি মোছা হয়নি -- ঠিক আগের
        // মতোই নিচে টেবিলে থেকে যায়, শুধু ক্রমে নিচে নেমেছে।
        // ⛔ শুধু forceFullJourney (Full Journey/History বোতামে খোলা)
        // অবস্থাতেই এই কার্ড দেখানো হয় -- সাধারণ Patient Timeline ভিউ
        // (৩-স্টেজ ফিল্টার করা) অপরিবর্তিত।
        // ⛔ কোনো নতুন ডেটা/কলাম লাগেনি -- Doctor Checkup সেভের সময় আগে
        // থেকেই লেখা "details" টেক্সট (Complaint/Duration/Prev
        // Treatment/Prev Result) থেকে পার্স করা হয়, যা DoctorCheckupActivity-
        // র buildDetails()-এর হুবহু ফরম্যাট।
        if (forceFullJourney) {
            val firstCheckup = data.entries
                .filter { it.title.equals("Doctor Checkup", ignoreCase = true) }
                .minByOrNull { it.sortKey.ifBlank { it.date } }
            if (firstCheckup != null) {
                // 🔴 TK-নির্দেশে দ্বিতীয়বার যাচাই করে ধরা বাগ (04.08.2026): সাধারণ
                // indexOf("Duration: ") "Treatment Duration: " এর ভিতরেও ভুল করে
                // মিলে যেতে পারত যদি আসল "Duration" ফাঁকা থাকত কিন্তু "Treatment
                // Duration" ভরা থাকত -- তখন ভুল মান "Since"-এ দেখাত। এখন লেবেলটা
                // হয় স্ট্রিং-এর একদম শুরুতে, নয়তো ঠিক তার আগে "; " (buildDetails()-
                // এর নিজস্ব ফিল্ড-বিভাজক) থাকলে তবেই মেলে -- তাই "Treatment
                // Duration"-এর মাঝে থাকা "Duration: " আর ধরা পড়বে না।
                fun field(label: String): String {
                    val note = firstCheckup.note
                    val marker = "$label: "
                    var searchFrom = 0
                    var idx = -1
                    while (true) {
                        val found = note.indexOf(marker, searchFrom)
                        if (found < 0) break
                        if (found == 0 || (found >= 2 && note.startsWith("; ", found - 2))) { idx = found; break }
                        searchFrom = found + 1
                    }
                    if (idx < 0) return ""
                    val from = idx + marker.length
                    val end = note.indexOf("; ", from).let { if (it < 0) note.length else it }
                    return note.substring(from, end).trim()
                }
                val duration = field("Duration")
                val prevTreatment = field("Prev Treatment")
                val prevResult = field("Prev Result")
                if (data.disease.isNotBlank() || duration.isNotBlank() || prevTreatment.isNotBlank() || prevResult.isNotBlank()) {
                    box.addView(android.widget.TextView(this).apply {
                        text = "🩺 Treatment Summary"
                        textSize = 11f
                        setTextColor(android.graphics.Color.parseColor("#0F5C42"))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setPadding(dpx(8), dpx(8), dpx(8), dpx(2))
                    })
                    // 🔴 TK-নির্দেশ (04.08.2026): লেবেল ইংরেজিতেই -- Kishanganj-এর
                    // বাংলা-বন্ধ স্টাফের পর্দায় এক অক্ষর বাংলাও থাকতে পারবে না
                    // (NoBengali.kt নিয়ম, পাহারাদার [৯.১৪])।
                    val summaryLines = mutableListOf<String>()
                    if (data.disease.isNotBlank()) summaryLines.add("Disease: ${data.disease}")
                    if (duration.isNotBlank()) summaryLines.add("Since: $duration")
                    if (prevTreatment.isNotBlank()) summaryLines.add("Previous Treatment: $prevTreatment")
                    if (prevResult.isNotBlank()) summaryLines.add("Previous Result: $prevResult")
                    box.addView(android.widget.TextView(this).apply {
                        text = summaryLines.joinToString("\n")
                        textSize = 10f
                        setTextColor(android.graphics.Color.parseColor("#333333"))
                        setPadding(dpx(8), dpx(2), dpx(8), dpx(8))
                        setBackgroundColor(android.graphics.Color.parseColor("#F3F7F5"))
                    })
                }
            }
        }

        // TK-REQUESTED ADDITION (2026-07-23): count summary (Enquiry Calls /
        // Visit Calls / Payments) shown just above the table -- categories
        // with 0 count are simply omitted, whole strip hides if all zero.
        // "Enquiry Calls" = generic call/remark entries before the
        // Registration/Visit entry; "Visit Calls" = the same, after it.
        // TK-DECISION (2026-07-24): now counted from `list` (the same
        // filtered set actually shown below), not the unfiltered
        // data.entries, so these numbers always match what's on screen.
        run {
            val chronological = list.sortedBy { it.visitNo }
            var registrationSeen = false
            var enquiryCalls = 0
            var visitCalls = 0
            var payments = 0
            for (e in chronological) {
                val t = e.title.lowercase()
                // \ud83d\udfe2\ud83d\udd12 V1090 — \u099a\u09bf\u0995\u09bf\u09ce\u09b8\u09be\u09b0 \u09a8\u09cb\u099f \u0995\u0996\u09a8\u09cb \u0995\u09b2 \u09a8\u09af\u09bc, \u09a4\u09be\u0987 \u0997\u09cb\u09a8\u09be\u09af\u09bc \u09a7\u09b0\u09be \u09b9\u09df \u09a8\u09be\u0964
                if (e.isTreatmentNote) continue
                when {
                    t.contains("registration") || t.contains("visit") -> registrationSeen = true
                    t.contains("payment") || t.contains("advance") -> payments++
                    t.contains("enquiry") || t.contains("treatment complete") ||
                        t.contains("prescription") || t.contains("medicine") || t.contains("diet") ||
                        t.contains("blood") || t.contains("medical") || t.contains("checkup") ||
                        t.contains("investigation") -> { /* not a call -- not counted */ }
                    else -> if (registrationSeen) visitCalls++ else enquiryCalls++
                }
            }
            val parts = mutableListOf<String>()
            if (enquiryCalls > 0) parts.add("📞 Enquiry Calls: $enquiryCalls")
            if (visitCalls > 0) parts.add("👣 Visit Calls: $visitCalls")
            if (payments > 0) parts.add("💰 Payments: $payments")
            if (parts.isNotEmpty()) {
                box.addView(android.widget.TextView(this).apply {
                    text = parts.joinToString("   ")
                    textSize = 9.5f
                    setTextColor(android.graphics.Color.parseColor("#0F5C42"))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setBackgroundColor(android.graphics.Color.parseColor("#E6F4EE"))
                    setPadding(dpx(8), dpx(6), dpx(8), dpx(6))
                })
            }
        }

        // TK-REPORTED FIX (2026-07-24): all rows now use the SAME computed
        // width for the Date/Time and Type/By columns (measured from the
        // actual content, header text included), instead of each row
        // sizing independently by WRAP_CONTENT -- which let the vertical
        // grid lines between columns drift/curve row to row whenever one
        // row's text happened to be wider than another's (TK
        // screenshot-caught the crooked line). Both columns still end up
        // exactly as wide as they need to be overall (the WIDEST single
        // piece of content across every row), just the SAME width on
        // every row now, so the grid lines are perfectly straight.
        val cellPadPx = dpx(8) * 2
        val boldTf = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val measureMain = android.graphics.Paint().apply { typeface = boldTf; textSize = 9.3f * resources.displayMetrics.scaledDensity }
        val measureSub = android.graphics.Paint().apply { typeface = boldTf; textSize = 8.5f * resources.displayMetrics.scaledDensity }
        val measureHead = android.graphics.Paint().apply { typeface = boldTf; textSize = 9.5f * resources.displayMetrics.scaledDensity }
        var dateTimeColPx = measureHead.measureText("Date/Time")
        var typeByColPx = measureHead.measureText("Type / By")
        for (e in list) {
            val dateText = displayDate(e.date.ifBlank { e.callTime })
            val timeText = displayTime(e.callTime)
            dateTimeColPx = maxOf(dateTimeColPx, measureMain.measureText(dateText), measureSub.measureText(timeText))
            val byNameForMeasure = if (e.by.isNotBlank()) StaffDirectory.findAccount(e.by)?.name ?: e.by else ""
            typeByColPx = maxOf(typeByColPx, measureMain.measureText(e.title), measureSub.measureText(byNameForMeasure.ifBlank { "\u2014" }))
        }
        // TK-REPORTED (2026-07-27, photo proof: header read "ate/Time" and the
        // date read "5.07.2026"). ROOT CAUSE: the widths below are measured
        // with a Paint built from Typeface.DEFAULT, but the TextViews actually
        // draw with the theme's own font. When the real font is even slightly
        // wider than the measured one, the text does not fit the fixed width
        // and gets clipped at the cell edge. A small fixed slack is added so
        // the cell is always a little wider than the text needs. Same columns,
        // same alignment, just never cut.
        val cellSlackPx = dpx(10)
        val dateTimeColWidth = dateTimeColPx.toInt() + cellPadPx + cellSlackPx
        val typeByColWidth = typeByColPx.toInt() + cellPadPx + cellSlackPx

        val head = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.parseColor("#0F8F6F"))
        }
        // TK-REQUESTED (2026-07-24, final revision): Date/Time and Type/By
        // are exactly as wide as the widest content actually needs (fixed
        // px, computed above -- same value used for every row so the grid
        // lines stay straight). Note takes ALL remaining space (weight=1f).
        // Font sizes unchanged (header 9.5sp, primary cell text 9.3sp,
        // secondary cell text 8.5sp).
        fun headCell(t: String, w: Int) = android.widget.TextView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(w, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            text = t; textSize = 9.5f; setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dpx(8), dpx(7), dpx(8), dpx(7))
        }
        val headNote = android.widget.TextView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Note"; textSize = 9.5f; setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dpx(8), dpx(7), dpx(8), dpx(7))
        }
        // TK-REQUESTED CHANGE (2026-07-23): Date+Time merged into ONE column,
        // and now Type+By also merged into ONE column (was 4 separate fixed
        // columns) -- frees more width for the Note/Remarks column, which
        // was cramped.
        head.addView(headCell("Date/Time", dateTimeColWidth))
        head.addView(headCell("Type / By", typeByColWidth))
        head.addView(headNote)
        box.addView(head)

        // TK-DECISION (2026-07-24): `list` already computed at the top of
        // this function (stage-filtered, or full on Full Journey/
        // Patient-Treatment stage) -- reused here unchanged. Row rendering
        // itself (grid cells, borders, 3-tap edit) is untouched.
        //
        // TK-REQUESTED (2026-07-27): "প্রতিটা বক্সের উচ্চতা একই রকম থাকবে" --
        // every row the same height as the TALLEST row. A row's height is
        // decided by its Note (the only cell that can wrap), so rows used to
        // be different heights. Real heights are only known after Android has
        // laid the table out, so the rows are collected here and evened up in
        // a post() right after (see the block below the loop). Column widths
        // are untouched, exactly as TK asked.
        val builtRows = ArrayList<android.widget.LinearLayout>()
        for (e in list) {
            // TK-SAFETY DECISION (2026-07-26): payment rows now carry a real
            // callTime (so the Date/Time column can finally show the time),
            // but the red "unexpected hour" highlight must stay exactly what
            // it always was -- a CALL-timing warning. Evening payments are
            // completely normal at the chamber, so colouring them red would
            // be a visible change TK never approved. Payment rows (which are
            // the only entries carrying a paymentId) keep the normal colour.
            val unexpected = e.paymentId == null && isUnexpectedHour(e.callTime)
            // TK-REQUESTED FIX (2026-07-23): border moved from the whole row
            // to EACH cell individually below (Date/Time, Type, By, Note all
            // get their own bordered box) -- a proper grid with lines all
            // around every cell, like Google Sheets, instead of only a
            // border around the outside of each row.
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
            }
            // TK-REQUESTED ADDITION (2026-07-23): Date on top, Time
            // underneath, in ONE column (was two side-by-side columns) --
            // same unexpected-hour red highlight on the Time line only.
            fun cellDateTime(dateText: String, timeText: String): android.widget.LinearLayout {
                return android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    // TK-REPORTED BUG FIX (2026-07-25): same MATCH_PARENT
                    // clipping risk as cellTypeBy below -- WRAP_CONTENT so
                    // this cell always sizes to its own real content.
                    layoutParams = android.widget.LinearLayout.LayoutParams(dateTimeColWidth, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                    setPadding(dpx(8), dpx(8), dpx(8), dpx(8))
                    background = cellBorderDrawable()
                    addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                        text = dateText; textSize = 9.3f; setTextColor(android.graphics.Color.parseColor("#334155"))
                        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                    if (timeText.isNotBlank()) {
                        addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                            text = timeText; textSize = 8.5f
                            setTextColor(android.graphics.Color.parseColor(if (unexpected) "#c0392b" else "#5b6b81"))
                            if (unexpected) setTypeface(typeface, android.graphics.Typeface.BOLD)
                            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                            setPadding(0, dpx(1), 0, 0)
                        })
                    }
                }
            }
            // TK-REQUESTED ADDITION (2026-07-23): Type on top, staff code-
            // name (By) underneath, in ONE column (was two side-by-side
            // columns) -- frees more width for Note. Same colours each
            // already had (Type = e.colorHex, By = dark navy bold).
            fun cellTypeBy(typeText: String, byText: String, typeColor: String): android.widget.LinearLayout {
                return android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    // TK-REPORTED BUG FIX (2026-07-25): MATCH_PARENT height
                    // here, next to a WRAP_CONTENT Note column in a
                    // WRAP_CONTENT row, could under-measure this cell's own
                    // 2-line content (Type + By) and clip the bottom line --
                    // exactly the cut-off "COB-UTTAMA" TK's photo-proof
                    // showed. WRAP_CONTENT sizes this cell to its own real
                    // content, so it never clips no matter what height the
                    // Note column ends up needing.
                    layoutParams = android.widget.LinearLayout.LayoutParams(typeByColWidth, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                    setPadding(dpx(8), dpx(8), dpx(8), dpx(8))
                    background = cellBorderDrawable()
                    addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                        text = typeText; textSize = 9.3f; setTextColor(android.graphics.Color.parseColor(typeColor))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
                    })
                    addView(android.widget.TextView(this@PatientTimelineActivity).apply {
                        text = byText; textSize = 8.5f; setTextColor(android.graphics.Color.parseColor("#10223A"))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        // 🔒 খাতার সারি B63 (TK, 29.07.2026): *"সম্পূর্ণ প্রজেক্টে
                        // এরকম সমস্যা যেন দেখা না দেয়।"* — স্টাফের নাম/কোড কোথাও
                        // কাটা যাবে না। লম্বা হলে নিচের লাইনে নামবে, কাটবে না।
                        maxLines = 2; ellipsize = null
                        setPadding(0, dpx(1), 0, 0)
                    })
                }
            }
            val noteCell = android.widget.TextView(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                // TK-REPORTED (2026-07-27): this was 14sp while the staff name
                // in the previous cell is 8.5sp, so the Note text looked far
                // bigger than everything around it. TK's instruction: keep it
                // the SAME size as the staff name. Colour and weight unchanged.
                text = e.note.ifBlank { "\u2014" }; textSize = 8.5f; setTextColor(android.graphics.Color.parseColor("#334155"))
                // TK-REQUESTED (2026-07-24): Note text bold and bigger
                // (same bold weight as the Staff Code/"By" text in Type/By),
                // still darker/legible (#334155, not the old lighter grey).
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                // TK-REQUESTED (2026-07-20): unlike the other columns (Date/
                // Time/By, which must stay single-line), Note can be long --
                // let it wrap onto a 2nd line instead of hiding text.
                maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(dpx(8), dpx(8), dpx(8), dpx(8))
                background = cellBorderDrawable()
                // TK-REQUESTED (2026-07-24): full Note text was invisible
                // once it ran past 2 lines (just "..."). Tap now shows the
                // complete text in a popup -- table layout/row height itself
                // stays exactly as before, this only adds a way to read the
                // rest when it's cut off.
                val fullNote = e.note
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    // 🆕🔒 (07.08.2026, TK-অনুমোদিত) — সারিটা যদি "Doctor Checkup"
                    // হয়, তাহলে সাদামাটা লেখার বদলে সেই দিনের **A4 চেকআপ-রিপোর্ট**
                    // (ক্লিনিক-লেটারহেডসহ) খোলে, নিচে Print। বাকি সব সারিতে আগের
                    // মতোই সাধারণ Note পপ-আপ — এক অক্ষরও বদলায়নি।
                    // ⛔ নতুন কোনো ক্লাউড-কল নেই (একই সেভ-করা `note` টেক্সট থেকেই)।
                    // ⛔ কোনো কারণে ভুল হলে নিচের পুরনো পপ-আপেই ফিরে যায়।
                    val isCheckupRow = e.title.equals("Doctor Checkup", ignoreCase = true) ||
                        e.title.equals("Doctor Check-up", ignoreCase = true)
                    if (isCheckupRow && fullNote.isNotBlank()) {
                        try {
                            showCheckupA4Dialog(fullNote, e.date)
                            return@setOnClickListener
                        } catch (_: Throwable) { /* নিচের সাধারণ পপ-আপে ফিরে যায় */ }
                    }
                    // 🔴🔴🔴🔴 V480 (20.08.2026, TK-রিপোর্ট, ছবিসহ — "প্রেসক্রিপশন
                    // তৈরি করেছিলাম, এখন এটা রিমার্ক হিসেবে দেখাচ্ছে, চাপ দিলে
                    // A4 প্রিন্ট-আউটের মতো দেখাতে হবে")। Doctor Checkup-এর জন্য
                    // এই একই কাজ উপরে আগে থেকেই আছে — Medicine Slip-এর জন্য
                    // এখনো ছিল না, এটাই আসল কারণ। ⛔ note সেভ হওয়ার ঠিক যে
                    // ফরম্যাটে (MedicineSlipActivity.persistSlipToHistory —
                    // "Name · Dose · Freq · Duration; Name2 · ...") — সেটাই
                    // হুবহু মিলিয়ে পার্স করা হয়, আন্দাজে নয়। ব্যর্থ হলে নিচের
                    // পুরনো সাধারণ Note পপ-আপেই ফিরে যায় — কিছু ভাঙে না।
                    val isMedicineSlipRow = e.title.equals("Medicine Slip", ignoreCase = true)
                    if (isMedicineSlipRow && fullNote.isNotBlank()) {
                        try {
                            showMedicineSlipA4Print(fullNote, e.date)
                            return@setOnClickListener
                        } catch (_: Throwable) { /* নিচের পুরনো পপ-আপে ফিরে যায় */ }
                    }
                    // 🔒 B568: সাধারণ সারিতেও এখন নীল-কার্ড Note (রিমার্ক/স্ট্যাটাস/
                    // চেক-আপ/পেমেন্ট আলাদা)। ব্যর্থ হলে নিচের পুরনো পপ-আপে ফিরে যায়।
                    // ══════════════════════════════════════════════════════
                    // 🔴🔴🔒 V512 (২১.০৮.২০২৬, TK-এর পুরোনো রিপোর্ট) — **"Note
                    //    পপ-আপ থেকে Remark বদলানো যায় না"**।
                    //
                    // ─── কারণ (কোড ধরে প্রমাণিত, আন্দাজে নয়) ───────────
                    //   সংশোধনের ব্যবস্থা আগে থেকেই আছে — সারির উপরে **৩ বার
                    //   চাপলে** `editEnquiryHistoryNote(e)` খোলে (নিচে
                    //   `TripleTapEdit.attach(row)`)। কিন্তু ঐ শোনার ব্যবস্থাটা
                    //   বসানো **পুরো সারিতে**, আর এই Note ঘরটা নিজেই
                    //   `isClickable = true` — তাই Note-এর লেখার উপরে চাপ দিলে
                    //   সেটা এখানেই আটকে যায়, সারির ৩-ট্যাপ পর্যন্ত পৌঁছায় না।
                    //   TK স্বাভাবিকভাবেই লেখাটার উপরেই চাপ দিতেন, তাই তাঁর
                    //   কাছে সংশোধনের কোনো পথই খোলা ছিল না।
                    //
                    // ⇒ সমাধান: পপ-আপের ভিতরে একটা **✏️ Edit** বোতাম, যেটা ঠিক
                    //   **ঐ একই পুরোনো ফাংশনটাই** ডাকে। নতুন কোনো সেভের পথ
                    //   তৈরি হয়নি, তাই নতুন ঝুঁকিও নেই।
                    // ⛔ কে বদলাতে পারবেন সেই নিয়ম **এক অক্ষরও বদলায়নি** — যিনি
                    //    আগে ৩-ট্যাপে বদলাতে পারতেন, ঠিক তিনিই পারবেন; শুধু
                    //    পথটা এখন চোখে দেখা যায়।
                    // ⛔ শর্তটা নিচের `TripleTapEdit` বসানোর শর্তের **হুবহু
                    //    নকল** — পেমেন্টের সারিতে এই বোতাম আসে না (পেমেন্টের
                    //    নিজস্ব সম্পাদক আছে, `tryEditTimelinePayment`)।
                    // ⛔ Doctor Checkup · Medicine Slip · Prescription — এই তিনটে
                    //    উপরেই আলাদা পর্দায় চলে যায়, তাই সেখানে কোনো বোতাম
                    //    যোগ হয়নি (V327-এ প্রেসক্রিপশন "শুধু দেখার" বলে লক করা)।
                    // ══════════════════════════════════════════════════════
                    /* ✏️🔒 V724 (২৭.০৮.২০২৬, ডা. কে. এইচ. মণ্ডলের রিপোর্ট ·
                       TK-অনুমোদিত · TK: *"এত বাংলায় লেখা থাকবে না"* ⇒ সব লেখা ইংরেজি):

                       **ডাক্তারের অভিযোগ:** স্টাফের ভুল লেখা ("KSHAR SUTRA করা হল")
                       বদলানোর কোনো অপশনই দেখা যায় না — শুধু "Close"।

                       **আসল কারণ (কোড ধরে):** ✏️ Edit বোতামটা আগে থেকেই আছে
                       (V512), কিন্তু সেটা তখনই দেখায় যখন সারিটার **সংশোধনের চাবি**
                       আছে। পর্দাটা প্রথমে **ফোনে জমানো কপি** দেখায়, আর
                       `TimelineCache` **ইচ্ছে করে** ওই চাবিগুলো জমা রাখে না —
                       যাতে পুরোনো সারিতে চাপ দিয়ে ভুল রেকর্ড বদলে না যায়।
                       ক্লাউড থেকে তাজা কপি এলে চাবি ফিরে আসে। নেট ধীর/খারাপ হলে
                       (বা তার আগেই খুললে) বোতামটা থাকত না।

                       **সমাধান:** বোতামটা এখন **সবসময়** থাকে। চাবি না থাকলে
                       চাপার পরে **আগে তাজা তথ্য আনা হয়**, তারপর এডিট খোলে।
                       ⛔ জমানো কপির চাবিহীন সারি দিয়ে **কখনো** এডিট করা হয় না —
                          `TimelineCache`-এর সুরক্ষা-নিয়মটা অটুট।
                       ⛔ পেমেন্টের সারি আগের মতোই আলাদা পথে (এখানে বাদ)। */
                    /* ✏️🔒 V736 (TK-অনুমোদিত অপশন ৩) — কোন সারিতে কোন সম্পাদক:
                         · "Registration / Visit" (রেজিস্ট্রেশনের ঘর আছে) ⇒ **নতুন
                           সম্পাদক** — Complaint · Duration · Previous Treatment ·
                           Payment Note আলাদা আলাদা ঘরে, টাকার অঙ্ক তালাবন্ধ।
                           ডাক্তার যে সারিটা ঠিক করতে চেয়েছিলেন সেটা **এটাই**;
                           V724-এ `paymentId == null` শর্তের জন্য বোতামটাই আসত না।
                         · সাধারণ নোটের সারি ⇒ আগের সাদামাটা নোট-বাক্সই (অপরিবর্তিত)
                         · শুধু পেমেন্টের সারি ⇒ কোনো বোতাম নয় (টাকার নিজস্ব পথ আছে) */
                    val noteEditAction: (() -> Unit)? = when {
                        !e.regPatientRowId.isNullOrBlank() -> ({ editRegistrationAndPayNote(e) })
                        e.paymentId == null -> ({ editNoteEnsuringFresh(e) })
                        else -> null
                    }
                    try {
                        showNoteCardsDialog(fullNote, e.date, e.title, noteEditAction)
                        return@setOnClickListener
                    } catch (_: Throwable) { /* নিচের পুরনো পপ-আপে ফিরে যায় */ }
                    androidx.appcompat.app.AlertDialog.Builder(this@PatientTimelineActivity)
                        .setCustomTitle(PremiumAlert.header(this@PatientTimelineActivity, "Note — " + displayDate(e.date)))
                        .setMessage(fullNote.ifBlank { "\u2014" })
                        .setPositiveButton("Close", null)
                        .apply {
                            if (noteEditAction != null) setNeutralButton("✏️ Edit") { _, _ -> noteEditAction?.invoke() }
                        }
                        .show().also { PremiumAlert.paint(it) }
                }
            }
            // TK-REPORTED BUG FIX (2026-07-23): this was "#334" -- an
            // invalid/truncated color (Android only accepts 6 or 8 hex
            // digits, this had 3) -- Color.parseColor() threw
            // IllegalArgumentException on the very first row of this table
            // every time, which is why the WHOLE table (Enquiry/Visit/
            // Patient -- all three share this same rendering code) always
            // showed empty with an "Unknown color" error, even though the
            // header and "Payments: N" count (built separately, before this
            // loop) looked fine. Fixed to a complete, valid 6-digit hex in
            // the same dark-slate family as this table's other columns.
            // \ud83d\udd12 B607 (TK-\u09a8\u09bf\u09b0\u09cd\u09a6\u09c7\u09b6 09.08.2026): "\u0986\u09b8\u09ac\u09c7 \u09ac\u09b2\u09c7\u099b\u09c7" (Marked Expected) \u09b8\u09be\u09b0\u09bf\u09b0
            // \u09a4\u09be\u09b0\u09bf\u0996 \u09b9\u09ac\u09c7 **\u09af\u09c7\u09a6\u09bf\u09a8 \u09ae\u09be\u09b0\u09cd\u0995 \u09b9\u09af\u09bc\u09c7\u099b\u09c7** (callTime/createdAt) \u2014 \u09ad\u09ac\u09bf\u09b7\u09cd\u09af\u09a4\u09c7\u09b0 \u0986\u09b8\u09be\u09b0
            // \u09a6\u09bf\u09a8 \u09a8\u09af\u09bc (History = \u0986\u099c \u09aa\u09b0\u09cd\u09af\u09a8\u09cd\u09a4 \u0995\u09c0 \u0998\u099f\u09c7\u099b\u09c7)\u0964 \u0985\u09a8\u09cd\u09af \u09b8\u09ac \u09b8\u09be\u09b0\u09bf \u0986\u0997\u09c7\u09b0 \u09ae\u09a4\u09cb\u0987\u0964
            val rowDateSrc = if (e.title.contains("Expected", ignoreCase = true) && e.callTime.isNotBlank())
                e.callTime else e.date.ifBlank { e.callTime }
            row.addView(cellDateTime(displayDate(rowDateSrc), displayTime(e.callTime)))
            // TK-REPORTED FIX (2026-07-23): this used to show the raw staff
            // mobile number ("62078418...") -- now resolves to the same
            // staff code-name the header's own "By:" line already shows
            // (e.g. "KNE-KISHAN5"), via the same StaffDirectory lookup.
            val byName = if (e.by.isNotBlank()) StaffDirectory.findAccount(e.by)?.name ?: e.by else ""
            // \ud83d\udd12 B607 (TK-\u09a8\u09bf\u09b0\u09cd\u09a6\u09c7\u09b6): "Marked Expected" \u2192 \u09ac\u09be\u0982\u09b2\u09be "\u0986\u09b8\u09ac\u09c7 \u09ac\u09b2\u09c7\u099b\u09c7"; \u09b8\u09be\u09a7\u09be\u09b0\u09a3
            // \u09ab\u09cb\u09a8/\u098f\u09a8\u0995\u09cb\u09af\u09bc\u09be\u09b0\u09bf \u09b8\u09be\u09b0\u09bf\u09a4\u09c7 "Called By"; Registration/Payment \u0987\u0982\u09b0\u09c7\u099c\u09bf\u09a4\u09c7 \u0985\u09aa\u09b0\u09bf\u09ac\u09b0\u09cd\u09a4\u09bf\u09a4\u0964
            // \u26d4 \u09b6\u09c1\u09a7\u09c1 \u09a6\u09c7\u0996\u09be\u09a8\u09cb\u09b0 \u09b2\u09c7\u0996\u09be \u09ac\u09a6\u09b2\u09be\u09af\u09bc \u2014 e.title/\u09a1\u09c7\u099f\u09be/\u0985\u09a8\u09cd\u09af \u09aa\u09b0\u09cd\u09a6\u09be \u0985\u099f\u09c1\u099f\u0964
            val __tl = e.title.lowercase()
            val displayTitle = when {
                __tl.contains("expected") -> "\u0986\u09b8\u09ac\u09c7 \u09ac\u09b2\u09c7\u099b\u09c7"
                // \ud83d\udfe2\ud83d\udd12 V1090 (\u09e6\u09eb.\u09e6\u09ef.\u09e8\u09e6\u09e8\u09ec, TK: *"check up done \u09a6\u09c1\u0987\u09ac\u09be\u09b0 \u0995\u09c7\u09a8"*) —
                // \u099a\u09bf\u0995\u09bf\u09ce\u09b8\u09be\u09b0 \u09a8\u09cb\u099f\u09c7\u09b0 \u09b8\u09be\u09b0\u09bf\u09a4\u09c7 \u0986\u0997\u09c7 "Called By" \u09b2\u09c7\u0996\u09be \u0989\u09a0\u09a4 \u2014 \u0995\u09c7\u0989 \u09ab\u09cb\u09a8
                // \u0995\u09b0\u09c7\u09a8\u09a8\u09bf, \u09a4\u09be\u0987 \u098f\u0996\u09a8 "Treatment"\u0964 \u26d4 \u09b6\u09c1\u09a7\u09c1 \u09a6\u09c7\u0996\u09be\u09a8\u09cb\u09b0 \u09b2\u09c7\u0996\u09be\u0964
                e.isTreatmentNote -> "Treatment"
                __tl.contains("enquiry") || __tl.contains("follow") || __tl.contains("call") -> "Called By"
                else -> e.title
            }
            row.addView(cellTypeBy(displayTitle, byName.ifBlank { "\u2014" }, e.colorHex))
            row.addView(noteCell)
            box.addView(row)
            builtRows.add(row)

            // 🟢 TK-APPROVED (2026-08-07 · 09.08.2026 B607): ধরন অনুযায়ী সারির হালকা
            // background — এক নজরে ঘটনা বোঝা যায়। ⛔ শুধু এই History পর্দার রেন্ডার
            // (Report Card আলাদা কোড → অটুট); গ্রিড-বর্ডার/উচ্চতা/লজিক/৩-ট্যাপ কিছুই বদলায় না।
            //   • আসবে বলেছে (Expected) → সবুজ
            //   • Registration/Visit → সোনালি
            //   • বিল সম্পূর্ণ (Complete) → সবুজ
            //   • Payment/Advance → নীল ; সাধারণ ফোন → সাদা (রঙ নেই)
            run {
                val __t2 = e.title.lowercase()
                val __bg: Pair<String, String>? = when {
                    __t2.contains("expected") -> "#E6F7EE" to "#B7E4C7"
                    __t2.contains("registration") || __t2.contains("visit") -> "#FFF6E4" to "#EAD6A8"
                    __t2.contains("complete") -> "#E6F7EE" to "#B7E4C7"
                    __t2.contains("payment") || __t2.contains("advance") -> "#EAF1FB" to "#C6D8F0"
                    else -> null
                }
                if (__bg != null) {
                    for (ci in 0 until row.childCount) {
                        row.getChildAt(ci).background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor(__bg.first))
                            setStroke(1, android.graphics.Color.parseColor(__bg.second))
                        }
                    }
                }
            }

            // TK-PRESERVED (2026-07-23): this table now also carries
            // Payment entries (Patient Card uses it too) -- 3-tap on a
            // Payment row still opens the existing payment editor
            // (tryEditTimelinePayment, same Master/same-day rule as
            // before); notes still edit via editEnquiryHistoryNote.
            if (e.paymentId != null) {
                TripleTapEdit.attach(row) { tryEditTimelinePayment(e) }
            } else if (e.enquiryRowId != null || e.followUpHistoryId != null) {
                TripleTapEdit.attach(row) { editEnquiryHistoryNote(e) }
            }
        }

        // 🔒 TK-LOCKED RULE — "সব সারির উচ্চতা এক" (খাতার (ঞ) সারি ৮ · V142)।
        // TK-কে ২৮.০৭.২০২৬-এ দ্বিতীয়বার বলতে হয়েছে, তাই হিসেবটা এখন একটাই
        // জায়গায় (TableRowEqualizer) — এই টেবিল যত পর্দা থেকেই খুলুক, সবাই
        // ওটাই ডাকে, তাই আর কোথাও আলাদা হয়ে যেতে পারবে না।
        TableRowEqualizer.equalize(box, builtRows)
    }

    /**
     * ✏️🔒 V724 — চাবি থাকলে সোজা এডিট; না থাকলে আগে ক্লাউড থেকে তাজা তথ্য এনে
     * ঠিক ওই সারিটা খুঁজে তারপর এডিট। না পেলে ইংরেজিতে স্পষ্ট বার্তা।
     * ⛔ সেভের পথ নতুন কিছু নয় — সেই পুরোনো `editEnquiryHistoryNote()`-ই।
     */
    private fun editNoteEnsuringFresh(e: TimelineEntry) {
        val hasKey = e.enquiryRowId != null || (e.followUpHistoryId != null && e.followUpHistoryIndex >= 0)
        if (hasKey) { editEnquiryHistoryNote(e); return }
        android.widget.Toast.makeText(this, "Loading latest…", android.widget.Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val fresh = withContext(Dispatchers.IO) {
                try {
                    PatientTimelineRepository.build(
                        currentMobile, null, this@PatientTimelineActivity,
                        keepVisitFeeAsOwnRow = true, separateRowsPerEvent = true,
                        preferRowId = preferPatientRowId
                    )
                } catch (_: Throwable) { null }
            }
            if (isFinishing || isDestroyed) return@launch
            val match = fresh?.entries?.firstOrNull {
                it.paymentId == null && it.date == e.date && it.title == e.title &&
                    it.note.trim() == e.note.trim() &&
                    (it.enquiryRowId != null || (it.followUpHistoryId != null && it.followUpHistoryIndex >= 0))
            }
            if (match == null) {
                androidx.appcompat.app.AlertDialog.Builder(this@PatientTimelineActivity)
                    .setCustomTitle(PremiumAlert.header(this@PatientTimelineActivity, "\u26A0 Not ready yet"))
                    .setMessage("This note could not be loaded from the cloud just now. Please check the connection and try again in a moment.")
                    .setPositiveButton("OK", null)
                    .show().also { PremiumAlert.paint(it) }
                return@launch
            }
            currentEntries = fresh.entries
            editEnquiryHistoryNote(match)
        }
    }

    private fun editEnquiryHistoryNote(e: TimelineEntry) {
        val input = android.widget.EditText(this).apply {
            setText(e.note)
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dpx(12); setPadding(p, p, p, p); minLines = 2; gravity = android.view.Gravity.TOP
        }
        UppercaseInputUtil.applyToAll(input)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "\u270f\ufe0f Edit Note"))
            .setView(android.widget.LinearLayout(this).apply { setPadding(dpx(18), dpx(12), dpx(18), 0); addView(input) })
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        try {
                            if (e.enquiryRowId != null) {
                                /* 🔵🔒 V533 (২২.০৮.২০২৬, TK-সিদ্ধান্ত) — **স্টাফের লেখা Remark আর মুছবে না।**
                                   আগে এই লেখাটা payments-এর `remarks` ঘরে বসত। ফল: সেদিন কেউ পেমেন্ট ফর্মে
                                   নিজের হাতে যে Remark লিখেছেন — এমনকি **Refund-এর কারণ** — সব মুছে গিয়ে
                                   চিকিৎসার নোট বসে যেত (TK-এর ছবিতে ₹1,000 Advance-এ "DRESSING করা হল")।
                                   ⇒ এখন নিজের আলাদা ঘর `progress`। ⛔ `remarks` আর ছোঁয়াই হয় না। */
                                val fields = org.json.JSONObject().put("progress", text)
                                val saved = SupabaseClient.updateById("enquiries", e.enquiryRowId, fields)
                                if (!saved) GenericUpdateQueue.queue(this@PatientTimelineActivity, "enquiries", e.enquiryRowId, fields)
                                saved
                            } else if (e.followUpHistoryId != null && e.followUpHistoryIndex >= 0) {
                                writeFollowUpHistoryRemark(e, text)
                            } else false
                        } catch (_: Throwable) { false }
                    }
                    android.widget.Toast.makeText(this@PatientTimelineActivity, if (ok) "Saved \u2705" else "Failed \u2014 retry", android.widget.Toast.LENGTH_SHORT).show()
                    if (ok) load(currentMobile, currentSection)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }


    /* ═══════════════════════════════════════════════════════════════════
       ✏️🔒 V736 (২৭.০৮.২০২৬, TK-অনুমোদিত **অপশন ৩**, ফটো-প্রুফ দেখে পাশ)
       —————————————————————————————————————————————————————————————————
       ডা. কে. এইচ. মণ্ডল: *"এটা staff ভুল তুলেছে। এটা আমাকে ঠিক করার
       অধিকার দেওয়া হোক।"*  TK: *"৩ — চারটেই বদলানো যাবে, তবে পুরোনো লেখা
       জমা থাকবে।"*

       "Registration / Visit" সারির লেখাটা আসলে **দুই টেবিলের চার টুকরো**:
         · patients.complaint          — Complaint
         · patients.sinceWhen          — Duration
         · patients.previousTreatment  — Previous Treatment
         · payments.progress           — Payment Note
       তাই একটাই সাধারণ নোট-বাক্স দিয়ে এডিট করা যেত না — মেশানো লেখা সেভ
       করলে টাকার লাইনটাও রিমার্কের ঘরে ঢুকে যেত। এখানে প্রতিটা টুকরোর
       **নিজের ঘর**, আর সেভ হয় **নিজের ঠিকানায়**।

       🔒 কেন এটা নিরাপদ:
       ⛔ **টাকার অঙ্ক ও ধরন (₹ · CASH/ONLINE) এই পর্দায় নেই** — দেখা যায়,
          ছোঁয়া যায় না। টাকা বদলানোর পুরোনো পথ (Payment পর্দা · ৩-ট্যাপ)
          এক অক্ষরও বদলায়নি।
       ⛔ **যে ঘর বদলায়নি সেটা ছোঁয়াই হয় না** — শুধু বদলানো ঘরগুলো যায়।
       ⛔ **পুরোনো লেখা কখনো হারায় না** — `editHistory`-তে কে · কবে · কী
          থেকে কী, সব জমা হয় (patients-এ V736-এর নতুন ঘর, payments-এ
          আগে থেকেই ছিল)।
       ⛔ `editHistory` তালিকা-পড়ায় **টানা হয় না** (egress বাঁচাতে) — শুধু
          এই পর্দা খুললে ওই এক রোগীর জন্য একবার আনা হয়।
       ⛔ কিছু সেভ না হলে সৎ বার্তা দেখানো হয়, চুপচাপ "হয়ে গেছে" বলা হয় না।
       ═══════════════════════════════════════════════════════════════════ */
    private fun editRegistrationAndPayNote(e: TimelineEntry) {
        val patientRowId = e.regPatientRowId
        if (patientRowId.isNullOrBlank() && e.paymentId.isNullOrBlank()) {
            android.widget.Toast.makeText(this, "This row cannot be edited", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        android.widget.Toast.makeText(this, "Loading…", android.widget.Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            // পুরোনো ইতিহাস — শুধু এই রোগীর/এই পেমেন্টের, একবার
            val hist = withContext(Dispatchers.IO) { loadEditHistory(patientRowId, e.paymentId) }
            if (isFinishing || isDestroyed) return@launch
            showRegPayEditor(e, hist)
        }
    }

    /** patients ও payments — দুই দিকের `editHistory` একসাথে (শুধু এই সারির)। */
    private fun loadEditHistory(patientRowId: String?, paymentId: String?): List<String> {
        val out = ArrayList<String>()
        fun pull(table: String, id: String) {
            try {
                val rows = SupabaseClient.fetchListSlim(table, "id=eq.$id", 1, "id,editHistory")
                if (rows.length() == 0) return
                val arr = rows.getJSONObject(0).optJSONArray("editHistory") ?: return
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val line = o.optString("text").trim()
                    if (line.isNotEmpty()) out.add(line)
                }
            } catch (_: Throwable) { }
        }
        if (!patientRowId.isNullOrBlank()) pull("patients", patientRowId)
        if (!paymentId.isNullOrBlank()) pull("payments", paymentId)
        return out.takeLast(6)
    }

    private fun showRegPayEditor(e: TimelineEntry, history: List<String>) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        // এই পর্দার নিজের ধরনেই (PremiumAlert হেডার + ScrollView + ফর্ম)
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(4))
        }

        fun sectionLabel(text: String) = android.widget.TextView(this).apply {
            this.text = text
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(12), 0, dp(2))
        }
        fun fieldLabelView(text: String) = android.widget.TextView(this).apply {
            this.text = text
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#5B6B81"))
            setPadding(0, dp(8), 0, dp(3))
        }
        fun box(value: String) = android.widget.EditText(this).apply {
            setText(value)
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(11); setPadding(p, p, p, p)
            textSize = 13.5f
            setTextColor(android.graphics.Color.parseColor("#10223A"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val hasReg = !e.regPatientRowId.isNullOrBlank()
        val hasPay = !e.paymentId.isNullOrBlank()

        var complaintIn: android.widget.EditText? = null
        var durationIn: android.widget.EditText? = null
        var prevIn: android.widget.EditText? = null
        var payNoteIn: android.widget.EditText? = null

        if (hasReg) {
            container.addView(sectionLabel("REGISTRATION DETAILS"))
            container.addView(fieldLabelView("Complaint"))
            complaintIn = box(e.regComplaint); container.addView(complaintIn)
            container.addView(fieldLabelView("Duration"))
            durationIn = box(e.regDuration); container.addView(durationIn)
            container.addView(fieldLabelView("Previous Treatment"))
            prevIn = box(e.regPrevTreatment); container.addView(prevIn)
        }
        if (hasPay) {
            container.addView(sectionLabel("PAYMENT NOTE"))
            container.addView(fieldLabelView("Note"))
            payNoteIn = box(e.payTypedNote); container.addView(payNoteIn)
            container.addView(fieldLabelView("Amount & Mode"))
            // 🔒 টাকার লাইন — শুধু দেখার, ছোঁয়ার নয়
            container.addView(android.widget.TextView(this).apply {
                text = "₹" + "%,.0f".format(e.paymentAmount) + "  ·  " +
                       e.paymentMode + "      🔒 LOCKED"
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#7A879A"))
                val p = dp(11); setPadding(p, p, p, p)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#F1F4F8"))
                    cornerRadius = 12f * d
                    setStroke(dp(1), android.graphics.Color.parseColor("#C6D2E0"))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            })
        }
        container.addView(android.widget.TextView(this).apply {
            text = "⚠️ The amount and mode cannot be changed here — use the Payment screen for that."
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#8A5A00"))
            val p = dp(10); setPadding(p, p, p, p)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#FFF6E0")); cornerRadius = 10f * d
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        })
        if (history.isNotEmpty()) {
            container.addView(android.widget.TextView(this).apply {
                text = "Earlier versions (kept)\n" + history.joinToString("\n")
                textSize = 11.5f
                setTextColor(android.graphics.Color.parseColor("#3C4859"))
                val p = dp(10); setPadding(p, p, p, p)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#F4F8FF")); cornerRadius = 12f * d
                    setStroke(dp(1), android.graphics.Color.parseColor("#D9E4F5"))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            })
        }
        container.addView(android.widget.TextView(this).apply {
            text = "📝 Nothing is lost — who changed what, and when, is always kept."
            textSize = 11.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#1A4E8A"))
            val p = dp(10); setPadding(p, p, p, p)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#E8F1FC")); cornerRadius = 10f * d
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        })

        UppercaseInputUtil.applyToAll(container)  // প্রজেক্টের নিয়ম: ইংরেজি লেখা বড় হাতের
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "✏️ Edit Note"))
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton("💾 Save") { _, _ ->
                val newComplaint = complaintIn?.text?.toString()?.trim() ?: e.regComplaint
                val newDuration  = durationIn?.text?.toString()?.trim() ?: e.regDuration
                val newPrev      = prevIn?.text?.toString()?.trim() ?: e.regPrevTreatment
                val newPayNote   = payNoteIn?.text?.toString()?.trim() ?: e.payTypedNote
                saveRegPayNote(e, newComplaint, newDuration, newPrev, newPayNote)
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** 🔒 V736 — সেভ। **শুধু বদলানো ঘরগুলো** যায়; প্রতিটার পুরোনো লেখা
     *  `editHistory`-তে জমা হয়। কিছু ব্যর্থ হলে সৎ বার্তা দেখানো হয়। */
    private fun saveRegPayNote(
        e: TimelineEntry, complaint: String, duration: String, prevTreat: String, payNote: String
    ) {
        val user = NativeSession.current(this)
        val who = (user?.name ?: "").ifBlank { user?.mobile ?: "" }
        val whenIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .format(java.util.Date())
        val whenShown = FollowUpModel.displayDate(whenIso.take(10))

        val regChanges = ArrayList<Triple<String, String, String>>()   // ঘর · আগে · পরে
        if (!e.regPatientRowId.isNullOrBlank()) {
            if (complaint != e.regComplaint) regChanges.add(Triple("Complaint", e.regComplaint, complaint))
            if (duration != e.regDuration) regChanges.add(Triple("Duration", e.regDuration, duration))
            if (prevTreat != e.regPrevTreatment) regChanges.add(Triple("Previous Treatment", e.regPrevTreatment, prevTreat))
        }
        val payChanged = !e.paymentId.isNullOrBlank() && payNote != e.payTypedNote

        if (regChanges.isEmpty() && !payChanged) {
            android.widget.Toast.makeText(this, "Nothing changed", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        android.widget.Toast.makeText(this, "Saving…", android.widget.Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val failed = ArrayList<String>()
            withContext(Dispatchers.IO) {
                // ── ১. রেজিস্ট্রেশনের ঘরগুলো ───────────────────────────
                if (regChanges.isNotEmpty()) {
                    val id = e.regPatientRowId!!
                    val fields = org.json.JSONObject()
                    regChanges.forEach { (label, _, now) ->
                        when (label) {
                            "Complaint" -> fields.put("complaint", now)
                            "Duration" -> fields.put("sinceWhen", now)
                            "Previous Treatment" -> fields.put("previousTreatment", now)
                        }
                    }
                    val lines = regChanges.map { (label, was, now) ->
                        "$whenShown · $who — $label: \"${was.ifBlank { "(blank)" }}\" → \"${now.ifBlank { "(blank)" }}\""
                    }
                    fields.put("editHistory", appendHistory("patients", id, lines, whenIso, who))
                    fields.put("updatedAt", whenIso)
                    val ok = try { SupabaseClient.updateById("patients", id, fields) } catch (_: Throwable) { false }
                    if (!ok) {
                        GenericUpdateQueue.queue(this@PatientTimelineActivity, "patients", id, fields)
                        failed.add("Registration details")
                    }
                }
                // ── ২. পেমেন্টের নোট ───────────────────────────────────
                if (payChanged) {
                    val pid = e.paymentId!!
                    val line = "$whenShown · $who — Payment Note: " +
                        "\"${e.payTypedNote.ifBlank { "(blank)" }}\" → \"${payNote.ifBlank { "(blank)" }}\""
                    val fields = org.json.JSONObject()
                        .put("progress", payNote)
                        .put("editHistory", appendHistory("payments", pid, listOf(line), whenIso, who))
                        .put("editedBy", user?.mobile ?: "")
                        .put("editedAt", whenIso)
                        .put("updatedAt", whenIso)
                    val ok = try { SupabaseClient.updateById("payments", pid, fields) } catch (_: Throwable) { false }
                    if (!ok) {
                        GenericUpdateQueue.queue(this@PatientTimelineActivity, "payments", pid, fields)
                        failed.add("Payment note")
                    }
                }
            }
            if (isFinishing || isDestroyed) return@launch
            if (failed.isEmpty()) {
                android.widget.Toast.makeText(this@PatientTimelineActivity, "Saved ✅", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this@PatientTimelineActivity)
                    .setCustomTitle(PremiumAlert.header(this@PatientTimelineActivity, "⚠ Saved offline"))
                    .setMessage(
                        failed.joinToString(", ") + " could not reach the cloud right now.\n\n" +
                        "It is saved on this phone and will go up by itself when the internet is back. " +
                        "Nothing is lost."
                    )
                    .setPositiveButton("OK", null)
                    .show().also { PremiumAlert.paint(it) }
            }
            load(currentMobile, currentSection)
        }
    }

    /** পুরোনো `editHistory` এনে তার শেষে নতুন লাইন যোগ করে ফেরত দেয়।
     *  ⛔ পুরোনো কিছু কখনো মোছা হয় না — শুধু যোগ হয়। */
    private fun appendHistory(
        table: String, id: String, lines: List<String>, whenIso: String, who: String
    ): org.json.JSONArray {
        val arr = try {
            val rows = SupabaseClient.fetchListSlim(table, "id=eq.$id", 1, "id,editHistory")
            if (rows.length() > 0) rows.getJSONObject(0).optJSONArray("editHistory") ?: org.json.JSONArray()
            else org.json.JSONArray()
        } catch (_: Throwable) { org.json.JSONArray() }
        lines.forEach { t ->
            arr.put(org.json.JSONObject().put("at", whenIso).put("by", who).put("text", t))
        }
        return arr
    }

    /** TK APPROVED (2026-07-15): standing rule — 3-tap must edit everything.
     *  Same permission rule as every other payment editor in the app (Master
     *  edits any entry; Staff only a same-day, same-branch entry). */
    private fun tryEditTimelinePayment(entry: TimelineEntry) {
        val id = entry.paymentId
        if (id.isNullOrBlank()) return
        /* 🔴🔒 V1301 (তালিকা ৪১৩): চিহ্ন-সারি (Bill Edited · Expected · Arrived) এডিট করা যাবে না —
           ওতে টাকা বসালে Paid-এর হিসাব গুলিয়ে যায় (Tinku Bauli)। ওয়েবেও একই নিয়ম। */
        if (PaymentModel.isMarkerOnlyRow(entry.payType)) {
            android.widget.Toast.makeText(this, "This is a system marker row (Bill edit / Expected / Arrived) — it holds no money and can't be edited", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        // 🔒 V217 (§B216, 31.07.2026): Refund row-এর টাকা এই সাধারণ 3-tap দিয়ে
        // বদলানো বন্ধ — refund-এর নিজস্ব Approve/Reject ব্যবস্থা আছে (Payment
        // স্ক্রিন → Refund ফর্ম, Briefing পর্দা); এখানে amount বদলালে
        // approved/pending হিসাব ও over-refund পাহারা গুলিয়ে যেতে পারত।
        if (entry.payType == "refund") {
            android.widget.Toast.makeText(this, "Refund amount can't be edited here — use the Refund option on the Payment screen", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        // 🔒 V452 (19.08.2026, TK-approved A): a combined same-day Treatment
        // Payment may contain multiple real money events and a CASH+ONLINE split.
        // The old editor has only one Amount + one Mode, so editing it here would
        // silently destroy that split. Single-event payments remain editable.
        if (entry.payType.equals("treatment", true) && entry.paymentEventCount > 1) {
            android.widget.Toast.makeText(
                this,
                "This day's payment combines ${entry.paymentEventCount} entries. Use the Payment screen for a split-safe correction.",
                android.widget.Toast.LENGTH_LONG
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            return
        }
        val user = NativeSession.current(this)
        val isMaster = user?.role == "master"
        val sameBranch = entry.paymentBranch.isNotBlank() && entry.paymentBranch == user?.branch
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val canEdit = isMaster || (sameBranch && entry.date == today)
        if (!canEdit) {
            android.widget.Toast.makeText(this, "Only Master or same-day entries can be edited", android.widget.Toast.LENGTH_SHORT).show()
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
            setText(entry.paymentAmount.toInt().toString())
            hint = "Amount"
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            val p = dp(12); setPadding(p, p, p, p)
        }
        val modeInput = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@PatientTimelineActivity, android.R.layout.simple_spinner_dropdown_item, listOf("CASH", "ONLINE"))
            setSelection(if (entry.paymentMode.equals("CASH", true)) 0 else 1)
        }
        container.addView(android.widget.TextView(this).apply { text = "Amount"; setPadding(0, 0, 0, dp(4)) })
        container.addView(amtInput)
        container.addView(android.widget.TextView(this).apply { text = "Mode"; setPadding(0, dp(12), 0, dp(4)) })
        container.addView(modeInput)

        UppercaseInputUtil.applyToAll(container)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "💰 Edit Payment"))
            .setView(android.widget.ScrollView(this).apply { addView(container) })
            .setPositiveButton("Save") { _, _ ->
                val amt = amtInput.text.toString().trim().toDoubleOrNull() ?: 0.0
                if (amt <= 0) {
                    android.widget.Toast.makeText(this, "Enter a valid amount", android.widget.Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val newMode = modeInput.selectedItem.toString()
                val by = user?.name?.ifBlank { user.mobile } ?: (user?.mobile ?: "User")
                val whenStr = DateUtil.displayWithTime(java.util.Date())
                val changes = mutableListOf<String>()
                // 🔴 TK-REPORTED (30.07.2026 রাত, Kishanganj স্টাফের হিন্দি
                // বার্তা): "থেকে"/"করেছেন" বাংলা শব্দ এখন Mode-লাইনের মতোই
                // ইংরেজি "→"/"by"। ⛔ টাকার অঙ্ক/হিসাব/লজিক কিছুই বদলায়নি।
                if (amt != entry.paymentAmount) changes.add("Amount ₹${"%,.0f".format(entry.paymentAmount)} → ₹${"%,.0f".format(amt)}")
                if (!newMode.equals(entry.paymentMode, true)) changes.add("Mode ${entry.paymentMode} → $newMode")
                val audit = if (changes.isNotEmpty()) "Audit: ${changes.joinToString(", ")} by $by | Date: $whenStr" else ""
                val editStamp = SupabaseClient.localWriteStamp()
                val fields = org.json.JSONObject()
                    .put("amount", amt)
                    .put("mode", newMode)
                    .apply { if (audit.isNotBlank()) put("remarks", audit) }
                    .put("editedBy", user?.mobile ?: "")
                    .put("editedAt", editStamp)
                    .put("updatedAt", editStamp)
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { SupabaseClient.updateById("payments", id, fields) }
                    android.widget.Toast.makeText(
                        this@PatientTimelineActivity,
                        if (ok) "Payment updated" else "Failed — check connection",
                        android.widget.Toast.LENGTH_SHORT
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                    if (ok) load(currentMobile, currentSection)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /** TK-REQUESTED (2026-07-27): "চাপ দিলে একবারেই কাজ হতে হবে... ইনকোয়ারি
     *  কার্ডে হেডার আলাদা, ভিজিট কার্ডে আলাদা, পেশেন্ট কার্ডে আলাদা — সেটাই
     *  ওপেন হতে হবে।"
     *
     *  The Follow-up card hands over everything this header shows, so all of
     *  it is drawn here in onCreate -- with the CORRECT stage colour already
     *  in place -- before a single byte is fetched. The background load then
     *  overwrites these same fields with the fresh copy, using exactly the
     *  same formatting rules (see load()), so the staff sees no change and
     *  nothing can go stale.
     *
     *  Opened from anywhere that does not send this information, the function
     *  simply does nothing and the screen behaves exactly as it does today. */
    private fun paintInstantHeader() {
        try {
            val preStage = intent.getStringExtra("preStage").orEmpty()
            if (preStage.isBlank()) return
            currentFollowupStage = preStage

            // 1) stage colour -- the locked design: Enquiry blended,
            //    Visit pale yellow, Patient pale green.
            binding.patientHeaderRoot.setBackgroundResource(
                when {
                    preStage.equals("Inquiry", ignoreCase = true) -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_enquiry
                    preStage.equals("Patient", ignoreCase = true) -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_visit
                    else -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_patient
                }
            )

            // 2) the same fields, formatted by the same rules load() uses.
            val name = intent.getStringExtra("preName").orEmpty()
            binding.tvName.text = name.ifBlank { currentMobile }
            currentPatientName = name
            // 🆔 TK-এর নিয়ম (28.07.2026): নাম ও মোবাইলের সঙ্গে Patient ID-ও।
            // ডাটা আসার পরে ID এমনিতেই চিপে দেখায়; পর্দা খোলার প্রথম মুহূর্তেও
            // যেন দেখা যায়, তাই কার্ড থেকে আসা ID এখানেই বসানো হলো।
            // ⛔ বাড়তি কোনো ক্লাউড-কল নয় — ID আগে থেকেই সঙ্গে পাঠানো হয়।
            binding.tvMobVal.text = PatientIdText.append(
                "+91$currentMobile", intent.getStringExtra("prePatientId").orEmpty()
            )

            val branch = intent.getStringExtra("preBranch").orEmpty()
            val disease = intent.getStringExtra("preDisease").orEmpty()
            currentBranch = branch
            // 🔒 B573 (08.08.2026, TK-অনুমোদিত প্রুফ): কার্ড কমপ্যাক্ট — Branch ·
            // Disease · Sex-Age এখন একই লাইনে (আগে দুই আলাদা লাইন, কার্ড লম্বা হত)।
            // tvSexAgeVal লুকানো। ঠিকানা আগের মতোই ২ লাইনে। ⛔ তথ্য একটাও বাদ নেই।
            val branchU = branch.uppercase()
            val diseaseU = disease.uppercase()
            val age = intent.getStringExtra("preAge").orEmpty()
            val sex = intent.getStringExtra("preSex").orEmpty()
            val sexAge = when {
                age.isBlank() && sex.isBlank() -> ""
                age.isBlank() -> sex.uppercase()
                sex.isBlank() -> age
                else -> "${sex.uppercase()}-$age"
            }
            val hdrLine1 = listOf(branchU, diseaseU, sexAge).filter { it.isNotBlank() }.joinToString(" · ")
            binding.tvBranchDiseaseVal.text = hdrLine1
            binding.tvBranchDiseaseVal.visibility = if (hdrLine1.isBlank()) View.GONE else View.VISIBLE
            binding.tvSexAgeVal.visibility = View.GONE

            val address = intent.getStringExtra("preAddress").orEmpty()
            binding.tvAddressVal.text = stripAddressLabels(address)
            binding.tvAddressVal.visibility = if (address.isBlank()) View.GONE else View.VISIBLE

            // Enquiry stage shows no ID line at all (TK's 2026-07-24 rule).
            if (preStage.equals("Inquiry", ignoreCase = true)) {
                binding.tvChips.visibility = View.GONE
            } else {
                val pid = intent.getStringExtra("prePatientId").orEmpty()
                val chipsText = if (pid.isNotBlank()) "🆔 $pid".uppercase() else ""
                binding.tvChips.text = chipsText
                binding.tvChips.visibility = if (chipsText.isBlank()) View.GONE else View.VISIBLE
            }
            headerAlreadyPainted = true
        } catch (_: Throwable) {
            // Never allowed to stop the screen opening -- the normal load
            // below fills everything in either way.
        }
    }

    /** True once paintInstantHeader() has drawn a real name, so load() does
     *  not blank it back to "Loading..." underneath the staff. */
    private var headerAlreadyPainted = false

    /**
     * TK-REQUESTED (2026-07-27): draws the saved copy of this patient's
     * screen straight away, so tapping View All shows the details at once.
     *
     * Display only. The saved rows carry NO record ids (TimelineCache never
     * stores them), so a 3-tap on one of them cannot open an editor and can
     * never change the wrong record. As soon as the real data arrives, the
     * same table is rebuilt with the real rows and editing works normally.
     * The money totals used by Take Action / Report Card are deliberately NOT
     * taken from here either -- those keep waiting for the real figures.
     */
    private fun showCachedTimeline(data: TimelineData) {
        try {
            if (!headerAlreadyPainted) {
                // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে নিচের Mobile
                // লাইনের সাথে মিলে দুইবার দেখাত।
                binding.tvName.text = data.name.ifBlank { "UNKNOWN" }
                binding.tvMobVal.text = "+91$currentMobile"
                // 🔒 B573 (TK-অনুমোদিত প্রুফ): Branch · Disease · Sex-Age এক লাইনে।
                val branchU = data.branch.uppercase()
                val diseaseU = data.disease.uppercase()
                val sexAge = when {
                    data.age.isBlank() && data.sex.isBlank() -> ""
                    data.age.isBlank() -> data.sex.uppercase()
                    data.sex.isBlank() -> data.age
                    else -> "${data.sex.uppercase()}-${data.age}"
                }
                val hdrLine1 = listOf(branchU, diseaseU, sexAge).filter { it.isNotBlank() }.joinToString(" · ")
                binding.tvBranchDiseaseVal.text = hdrLine1
                binding.tvBranchDiseaseVal.visibility = if (hdrLine1.isBlank()) View.GONE else View.VISIBLE
                binding.tvSexAgeVal.visibility = View.GONE
                binding.tvAddressVal.text = stripAddressLabels(data.address)
                binding.tvAddressVal.visibility = if (data.address.isBlank()) View.GONE else View.VISIBLE
                if (data.followupStage.equals("Inquiry", ignoreCase = true)) {
                    binding.tvChips.visibility = View.GONE
                } else {
                    val chipsText = if (data.patientId.isNotBlank()) "🆔 ${data.patientId}".uppercase() else ""
                    binding.tvChips.text = chipsText
                    binding.tvChips.visibility = if (chipsText.isBlank()) View.GONE else View.VISIBLE
                }
                binding.patientHeaderRoot.setBackgroundResource(
                    when {
                        data.followupStage.equals("Inquiry", ignoreCase = true) -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_enquiry
                        data.followupStage.equals("Patient", ignoreCase = true) -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_visit
                        else -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_patient
                    }
                )
                headerAlreadyPainted = true
            }

            // The money boxes, by the same "zero is not shown" rule.
            fun money(v: Double) = "\u20B9" + "%,.0f".format(v)
            val latestPaid = data.entries.maxByOrNull { it.visitNo }?.runningPaid ?: 0.0
            val latestDue = data.entries.maxByOrNull { it.visitNo }?.runningDue ?: -1.0
            /* \ud83c\udff7\ufe0f\ud83d\udd12 V1508 \u2014 same pre-discount fallback as the live path below. */
            val cachedDisplayBillEst = if (data.billBeforeDiscount > 0.0 && data.billBeforeDiscount != data.billTotal)
                data.billBeforeDiscount else data.billTotal
            binding.tvChipEstimated.text = if (data.billTotal > 0.0) money(cachedDisplayBillEst) else "\u2014"
            binding.tvChipPaid.text = money(latestPaid)
            binding.tvChipDue.text = if (latestDue < 0) "\u2014" else money(latestDue)
            val showEstimated = data.billTotal > 0.0
            val showPaid = latestPaid > 0.0
            val showDue = latestDue > 0.0
            val showDiscount = data.discount > 0.0
            binding.tvChipDiscount.text = if (showDiscount) money(data.discount) else "\u2014"
            binding.boxEstimated.visibility = if (showEstimated) View.VISIBLE else View.GONE
            binding.boxPaid.visibility = if (showPaid) View.VISIBLE else View.GONE
            binding.boxDue.visibility = if (showDue) View.VISIBLE else View.GONE
            binding.boxDiscount.visibility = if (showDiscount) View.VISIBLE else View.GONE
            binding.billSummaryRow.visibility =
                if (showEstimated || showPaid || showDue || showDiscount) View.VISIBLE else View.GONE

            // The Updates table itself, built by the SAME function the live
            // data uses, so it looks identical.
            binding.progressLoad.visibility = View.GONE
            binding.tvEmpty.visibility = View.GONE
            binding.defaultUpdatesGroup.visibility = View.GONE
            binding.enquiryHistoryScroll.visibility = View.VISIBLE
            buildEnquiryHistoryTable(data)
        } catch (_: Throwable) {
            // Showing the saved copy is a convenience -- if anything at all
            // goes wrong here, the screen simply waits for the real data as
            // it does today.
        }
    }

    private fun load(mobile: String, section: String?) {
        // V1527: every fresh reload temporarily invalidates Action decisions.
        // Cached rows may still paint instantly, but they are display-only.
        actionStateReady = false
        binding.progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        // TK-REQUESTED (2026-07-24): this screen used to look completely
        // blank (name/details/table all empty) while waiting on slow
        // network. This ONLY sets a temporary "Loading..." label -- it's
        // overwritten by the real name the instant data arrives below, no
        // other part of this TK-LOCKED screen design is touched.
        if (!headerAlreadyPainted) binding.tvName.text = "Loading..."
        // TK-REQUESTED (2026-07-27): "ভিউ অল চাপলে যেন সেই পেশেন্টের সমস্ত
        // ডিটেলস ওপেন হয়... লোডিংয়ের জন্য অপেক্ষা করতে না হয়।" If this patient
        // was opened before, everything that was on the screen is already on
        // the phone -- so it is drawn right now, and the fresh copy below
        // simply replaces it a moment later.
        /* 🔴🔵🔒 V522 (২২.০৮.২০২৬): জমানো কপিটাও **এই রোগীর**।
           আগে চাবি ছিল শুধু মোবাইল, তাই এক নম্বরে দুজন থাকলে স্বামীর পরে
           স্ত্রীরটা খুললে প্রথম মুহূর্তে **স্বামীর** তথ্য আঁকা হত।
           ⛔ আইডি ফাঁকা হলে চাবিটা অবিকল আগের মতোই — কিছুই ভাঙে না। */
        val cachedNow = try { TimelineCache.load(this, mobile, preferPatientRowId) } catch (_: Throwable) { null }
        if (cachedNow != null) showCachedTimeline(cachedNow)
        lifecycleScope.launch {
            // CRASH-SAFETY FIX (TK-reported, 2026-07-16): any unexpected error
            // while building/rendering the timeline used to crash the whole
            // app -- Android then relaunches at Login/Dashboard, which looks
            // exactly like "View All takes me back to Home". Now it shows a
            // toast and an empty screen instead; nothing else about this
            // screen's behavior changes when data is normal.
            try {
                // 🔒 B607 (TK-অনুমোদিত): শুধু এই History পর্দায় প্রতি ঘটনা আলাদা সারি।
                // Report Card/অন্য কলার এই প্যারাম পাঠায় না → আগের মতোই merged।
                val data = withContext(Dispatchers.IO) { PatientTimelineRepository.build(mobile, section, this@PatientTimelineActivity, keepVisitFeeAsOwnRow = true, separateRowsPerEvent = true, preferRowId = preferPatientRowId) }
                binding.progressLoad.visibility = View.GONE

                // 🔴🔴 TK-REPORTED (31.07.2026): নাম না থাকলে Mobile দুইবার দেখাত।
                binding.tvName.text = data.name.ifBlank { "UNKNOWN" }
                // TK-DECISION (2026-07-24): computed here (not further below)
                // so it can also control the tags line's visibility right
                // after the header fields are set; still reused unchanged
                // for the button-swap logic later in this function.
                val isInquiryStage = data.followupStage.equals("Inquiry", ignoreCase = true)

                // TK-LOCKED DESIGN (2026-07-24): Mob/Name/(Branch+Disease)/
                // (Age+Sex)/Address fixed order, photo-proof approved
                // (supersedes the 2026-07-23 single-column Age-Sex/Disease
                // order).
                // TK-DECISION (2026-07-24, hide-if-blank): unlike Mob/Name
                // (always present), Branch/Disease/Age/Sex/Address each hide
                // individually when blank instead of showing "—"; if BOTH
                // halves of a paired row are blank, the whole row hides too
                // so no empty gap is left (common pre-Registration case:
                // Age+Sex row disappears completely).
                // 🔒 V235 (TK verified 01.08.2026): Primary নম্বরের পাশে Alternate/
                // Enquiry নম্বর (থাকলে) পরিষ্কার label-সহ। না থাকলে আগের মতোই শুধু
                // Primary — layout/design অপরিবর্তিত।
                binding.tvMobVal.text = if (data.altMobile.length == 10 && data.altMobile != data.mobile)
                    "+91${data.mobile}   ·   Alt: +91${data.altMobile}"
                else "+91${data.mobile}"

                // TK-LOCKED DESIGN (2026-07-24, v2): no field labels, and
                // Branch+Disease / Sex+Age are each combined onto a single
                // line now (photo-proof approved). Hide-if-blank rule is
                // UNCHANGED — still applies exactly as before, just to
                // these now-combined fields: whole line hides if BOTH
                // parts are blank; if only one part is blank, shows just
                // the other (no dangling separator).
                val branchU = data.branch.uppercase()
                val diseaseU = data.disease.uppercase()
                val branchBlank = data.branch.isBlank()
                val diseaseBlank = data.disease.isBlank()
                binding.tvBranchDiseaseVal.text = when {
                    branchBlank && diseaseBlank -> ""
                    branchBlank -> diseaseU
                    diseaseBlank -> branchU
                    else -> "$branchU - $diseaseU"
                }
                binding.tvBranchDiseaseVal.visibility = if (branchBlank && diseaseBlank) View.GONE else View.VISIBLE

                val sexU = data.sex.uppercase()
                val ageBlank = data.age.isBlank()
                val sexBlank = data.sex.isBlank()
                binding.tvSexAgeVal.text = when {
                    ageBlank && sexBlank -> ""
                    ageBlank -> sexU
                    sexBlank -> data.age
                    else -> "$sexU-${data.age}"
                }
                binding.tvSexAgeVal.visibility = if (ageBlank && sexBlank) View.GONE else View.VISIBLE

                // TK-REQUESTED (2026-07-24): address shown WITHOUT the
                // stored "Vill:"/"PO:"/"PS:"/"Dist:"/"PIN:" labels — just
                // the raw values, comma-separated. The underlying stored
                // address (PatientModel.buildAddress -- used everywhere
                // else: prescriptions, print, Registration edit, etc.) is
                // completely untouched; this only strips labels for THIS
                // header's display.
                val addressBlank = data.address.isBlank()
                binding.tvAddressVal.text = stripAddressLabels(data.address)
                binding.tvAddressVal.visibility = if (addressBlank) View.GONE else View.VISIBLE

                // TK-REQUESTED (2026-07-24): "By- Dr. NAME (AREA)", one
                // line, directly below Address — moved here from the
                // separate tags line (tvChips) below the header, per TK's
                // explicit instruction. Hides entirely when there's no
                // referring doctor.
                val refDoctorBlank = data.refDoctorDisplay.isBlank()
                binding.tvRefDoctorVal.text = if (refDoctorBlank) "" else "By- ${data.refDoctorDisplay}"
                binding.tvRefDoctorVal.visibility = if (refDoctorBlank) View.GONE else View.VISIBLE

                // TK-DECISION (2026-07-24): Enquiry stage — no tags line at
                // all. Visit/Patient stages show Patient ID only now (the
                // referring-RMP line moved to tvRefDoctorVal above, inside
                // the header block, per TK's 2026-07-24 instruction — no
                // longer duplicated here).
                /* 🔵🔒 V521 (২২.০৮.২০২৬, TK-নির্দেশ) — **অসময়ের এনকোয়ারি কিনা,
                   সেটা এই পর্দাতেই দেখা যাবে।**
                   TK-এর কথা: *"আমি তো ভিউ চেপে কিছু বুঝতেই পারছি না যে এটা
                   আনএক্সপেক্টেড টাইমের না এক্সপেক্টেড টাইমের… তাহলে আমি বুঝবো
                   কী করে যে স্টাফটা কী কারণে টাকা নিচ্ছে।"*
                   Extra Income শুধু **Unexpected Time**-এর এনকোয়ারিতেই হয়
                   (`V418_INCENTIVE_AUTO_2026-08-17.sql`), তাই ID-র পাশেই চিহ্নটা।
                   ⛔ Follow-up কার্ডে এই চিহ্ন **আগে থেকেই আছে** (`FollowUpAdapter`),
                      তাই দেখতে নতুন কিছু নয় — একই ভাষা, একই জায়গা।
                   ⛔ ঘরটা ফাঁকা থাকলে (পুরোনো রেকর্ড) কিছুই দেখায় না — আগের মতোই।
                   ⛔ বাড়তি কোনো ক্লাউড-কল নেই। */
                fun timingChip(): String {
                    val tt = data.timeType.trim()
                    if (tt.isBlank()) return ""
                    return if (tt.equals("Unexpected Time", ignoreCase = true)) "⏰ UNEXPECTED TIME"
                    else tt.uppercase()
                }
                if (isInquiryStage) {
                    // TK-DECISION (2026-07-24): Enquiry ধাপে ID-র লাইন থাকে না।
                    // কিন্তু Timing-টা এখানেই সবচেয়ে দরকারি (এখান থেকেই তো সব শুরু),
                    // তাই ID না থাকলেও শুধু Timing চিপটা দেখানো হয়।
                    val onlyTiming = timingChip()
                    binding.tvChips.text = onlyTiming
                    binding.tvChips.visibility = if (onlyTiming.isBlank()) View.GONE else View.VISIBLE
                } else {
                    val idPart = if (data.patientId.isNotBlank()) "🆔 ${data.patientId}".uppercase() else ""
                    val tPart = timingChip()
                    val chipsText = listOf(idPart, tPart).filter { it.isNotBlank() }.joinToString("   ·   ")
                    binding.tvChips.text = chipsText
                    binding.tvChips.visibility = if (chipsText.isBlank()) View.GONE else View.VISIBLE
                }

                // TK-REQUESTED (2026-07-18): Patient Card header — 3-tap to
                // edit name/mobile, long-press to copy. Edit saves to the
                // "patients" (Registration) row when one exists; if this
                // person only has an Enquiry so far (no Registration yet),
                // Edit tells the user that instead of silently failing.
                currentPatientRowId = data.rowId
                currentPatientName = data.name
                currentRefDoctor = data.refDoctor
                currentRefDoctorMobile = data.refDoctorMobile
                currentCompleteRequestedBy = data.completeRequestedBy
                currentCompleteApprovedBy = data.completeApprovedBy
                currentFollowupId = data.followupId
                currentFollowupStage = data.followupStage
            currentFollowupStatus = data.followupStatus
            currentPatientCode = data.patientId
                currentEnquiryId = data.enquiryId
                currentBranch = data.branch
                currentDisease = data.disease
                // 🔒 খাতার সারি B174: এই তথ্যগুলো এই পর্দাতেই হেডারে দেখানো হয়
                // (`data.age`/`data.sex`/`data.address`), এখন থেকে ধরেও রাখা
                // হচ্ছে — যাতে Take Action → Prescription/Diet Chart-এ ঠিকভাবে
                // পাঠানো যায়। ⛔ বাড়তি কোনো ক্লাউড-কল নেই, এই ডেটা আগে থেকেই
                // এই একই লোডের সঙ্গে আসে।
                currentPatientAge = data.age
                currentPatientSex = data.sex
                currentPatientAddress = data.address
                currentRegistrationDate = data.registrationDate
                currentTimeType = data.timeType
                currentRegistrationCreatedAt = data.registrationCreatedAt
                currentRegisteredByMobile = data.registeredByMobile
                currentEnquiryDate = data.enquiryDate
                currentEnquiryReceivedBy = data.enquiryReceivedBy
                TripleTapEdit.attach(binding.tvName) { showPatientHeaderEdit() }
                TripleTapEdit.attach(binding.tvMobVal) { showPatientHeaderEdit() }
                binding.tvName.setOnLongClickListener {
                    com.tkbiswas.pilesclinic.native.Clip.copy(this@PatientTimelineActivity, "name", currentPatientName)   // 🤫 V772
                    android.widget.Toast.makeText(this@PatientTimelineActivity, "Name copied", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                binding.tvMobVal.setOnLongClickListener {
                    com.tkbiswas.pilesclinic.native.Clip.copy(this@PatientTimelineActivity, "mobile", currentMobile)   // 🤫 V772
                    android.widget.Toast.makeText(this@PatientTimelineActivity, "Mobile copied", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }

                // 🔒 B569: A4 রিপোর্টে পাঠানোর জন্য রোগীর ছবি ধরে রাখা হয়।
                currentPatientPhoto = data.photo
                val bmp = PhotoUtils.decodeDataUrl(data.photo)
                if (bmp != null) {
                    // TK-REQUESTED (2026-07-24): pure square photo (sharp
                    // corners, no rounding) -- reverts the 2026-07-23
                    // rounded-square look back to the original square rule.
                    binding.ivPhoto.setImageBitmap(bmp)
                }

                if (data.entries.isEmpty()) {
                    binding.progressLoad.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    currentEntries = emptyList()
                } else {
                    // V227 (item 26): preserve scroll position on in-place
                    // refresh (e.g. returning from Payment/Photo), same proven
                    // pattern as Doctor Queue / Follow-up.
                    val lm = binding.recyclerView.layoutManager as? LinearLayoutManager
                    val firstPos = lm?.findFirstVisibleItemPosition() ?: -1
                    val firstOffset = if (firstPos >= 0) (lm?.findViewByPosition(firstPos)?.top ?: 0) else 0
                    // 🟢🔒 B682 (15.08.2026): পুরো পাতাটা এখন একটা NestedScrollView-এর ভিতরে
                    //   (TK-অনুমোদিত "সব গুলি উপরে যাবে")। তাই তালিকার নিজের স্ক্রোল আর নেই —
                    //   জায়গা ধরে রাখার কাজটা এখন বাইরের স্ক্রোলের উপর করতে হয়, নইলে
                    //   Payment/ছবি থেকে ফিরলে পাতা একদম উপরে লাফ দিত (V227 item 26-এর
                    //   সুবিধাটা হারাত)। ⛔ নিচের পুরনো দু'লাইন রাখা হলো — অন্য কোনো
                    //   কলার/ভবিষ্যতে তালিকা আবার নিজে স্ক্রোল করলে সেটাও কাজ করবে।
                    val pageY = binding.pageScroll.scrollY
                    adapter.update(data.entries)
                    currentEntries = data.entries
                    if (firstPos in 0 until data.entries.size) lm?.scrollToPositionWithOffset(firstPos, firstOffset)
                    if (pageY > 0) binding.pageScroll.post { binding.pageScroll.scrollTo(0, pageY) }
                }

                // TK-REQUESTED ADDITION (2026-07-16): summary chips — Estimated
                // from the patient's bill, Paid/Due from the latest running totals
                // (the most recent entry chronologically, i.e. index 0 since the
                // list is newest-first).
                fun money(v: Double) = "\u20B9" + "%,.0f".format(v)
                val latestPaid = data.entries.maxByOrNull { it.visitNo }?.runningPaid ?: 0.0
                val latestDue = data.entries.maxByOrNull { it.visitNo }?.runningDue ?: -1.0
                currentDue = latestDue
                // TK-REQUESTED (2026-07-18): when this is Enquiry-only (no
                // bill, no registration/visit yet — Estimated and Due both
                // have nothing real to show), hide the whole row instead of
                // showing three empty dashes. As soon as any real billing
                // exists (Estimated set, or a Due value computed), it shows
                // exactly as before.
                /* 🔴🔒 V1086 (০৫.০৯.২০২৬, TK-রিপোর্ট: MD AKBAR ALI — ₹১,০০০
                   Advance নেওয়া, অথচ উপরে Paid-এর ঘরটাই নেই)।
                   কারণ: এই শর্তে আগে **শুধু বিল** দেখা হত। বিল না বসানো
                   পর্যন্ত `latestDue` থাকে −১, তাই টাকা জমা পড়লেও পুরো
                   সারিটা (ও Report Card বোতামটা) লুকিয়ে থাকত — অর্থাৎ
                   **নেওয়া টাকা কার্ডে দেখাই যেত না**।
                   ⇒ জমা টাকা বা ছাড় থাকলেও এখন সারিটা ওঠে।
                   ⛔ শুধু ভিজিট ফি দেওয়া রোগীর কিছুই বদলায়নি — ভিজিট ফি
                      `latestPaid`-এ গোনাই হয় না, তাই তাঁদের ক্ষেত্রে আগের
                      মতোই সারিটা ওঠে না (এনকোয়ারি-মাত্র রোগীর নিয়ম অটুট)। */
                val hasBillingData = data.billTotal > 0.0 || latestDue >= 0 ||
                    latestPaid > 0.0 || data.discount > 0.0
                currentBillTotal = data.billTotal
                /* 🟢🔒 V1131 (০৬.০৯.২০২৬, TK-অনুমোদিত — "হ্যাঁ করুন, সাবধানে")
                   TK: MD AKBAR ALI-র কার্ডে *"এক হাজার টাকা জমা করেছে, কিন্তু
                   তার বিল কত দেখতে পাচ্ছি না, বাকি কত সেটাও না"*।

                   **কারণ (কোডে মেপে পাওয়া):** এই রোগীর বিল **০** — চিকিৎসার
                   মোট খরচ কোথাও বসানো হয়নি। নিয়ম হলো বিল ০ হলে Estimated ও
                   Due দুটো ঘরই লুকানো, তাই কার্ডটা দেখে বোঝার উপায় ছিল না
                   "বিল বসানোই হয়নি" নাকি "অ্যাপ দেখাচ্ছে না"।

                   ⇒ এখন **টাকা জমা আছে অথচ বিল বসেনি** — ঠিক এই অবস্থায়
                     Estimated ঘরে **"Not set"** লেখা ওঠে।
                   ⛔ বিল বসানো থাকলে আগের মতোই টাকার অঙ্কই বসে।
                   ⛔ এনকোয়ারি-মাত্র বা শুধু ভিজিট ফি দেওয়া রোগীর কার্ড এক
                      অক্ষরও বদলায়নি (তাঁদের `latestPaid` ০, তাই এই অবস্থাই আসে না)।
                   ⛔ Due ঘরটা আগের মতোই লুকানো থাকে — বিল ছাড়া "বাকি" কত সেটা
                      অ্যাপ জানে না, আন্দাজে সংখ্যা দেখানো হবে না। */
                val noBillYet = data.billTotal <= 0.0 && latestPaid > 0.0
                /* \ud83c\udff7\ufe0f\ud83d\udd12 V1508 (\u09e7\u09ec.\u09e6\u09ef.\u09e8\u09e6\u09e8\u09ec, TK-\u09a8\u09bf\u09b0\u09cd\u09a6\u09c7\u09b6 \u2014 MD ANARUL HOWK): *"\u0986\u09ae\u09be\u09b0
                   \u09b9\u09bf\u09b8\u09be\u09ac\u09c7 25000 \u09a6\u09c7\u0996\u09be\u09a4\u09c7 \u09b9\u09ac\u09c7, Paid \u09e8\u09ea\u09e6\u09e6\u09e6, \u098f\u09ac\u0982 \u09a1\u09bf\u09b8\u0995\u09be\u0989\u09a8\u09cd\u099f \u09e7\u09e6\u09e6\u09e6"*\u0964
                   \u21d2 "Estimated" \u099a\u09bf\u09aa \u099b\u09be\u09a1\u09bc \u09a6\u09c7\u0993\u09af\u09bc\u09be \u09a5\u09be\u0995\u09b2\u09c7 \u0986\u09b8\u09b2 (\u099b\u09be\u09a1\u09bc\u09c7\u09b0 \u0986\u0997\u09c7\u09b0) \u09ac\u09bf\u09b2
                   \u09a6\u09c7\u0996\u09be\u09af\u09bc, \u0995\u09ae\u09be\u09a8\u09cb \u09ac\u09bf\u09b2 \u09a8\u09af\u09bc\u0964 \u26d4 Due-\u098f\u09b0 \u09b9\u09bf\u09b8\u09be\u09ac/currentBillTotal/
                   Discount \u09a1\u09be\u09af\u09bc\u09be\u09b2\u0997 \u2014 \u0995\u09cb\u09a8\u09cb\u099f\u09be\u0987 \u099b\u09cb\u0981\u09af\u09bc\u09be \u09b9\u09af\u09bc\u09a8\u09bf, \u098f\u0987 \u099a\u09bf\u09aa\u09c7\u09b0 \u099f\u09c7\u0995\u09cd\u09b8\u099f\u099f\u09be\u0987 \u09b6\u09c1\u09a7\u09c1\u0964 */
                val displayBillEst = if (data.billBeforeDiscount > 0.0 && data.billBeforeDiscount != data.billTotal)
                    data.billBeforeDiscount else data.billTotal
                binding.tvChipEstimated.text = when {
                    data.billTotal > 0.0 -> money(displayBillEst)
                    noBillYet -> "Not set"
                    else -> "\u2014"
                }
                binding.tvChipPaid.text = money(latestPaid)
                binding.tvChipDue.text = if (latestDue < 0) "\u2014" else money(latestDue)
                // TK-STANDING RULE (restated 2026-07-27): "যখন জিরো থাকবে
                // সেগুলো দেখানোর কোনো দরকার নেই". Each box is now shown only
                // when its own amount is really greater than zero, and if all
                // three are zero the whole row disappears. The amounts and
                // their colours are untouched -- only whether a zero box is
                // drawn at all.
                val showEstimated = data.billTotal > 0.0 || noBillYet   // 🟢 V1131
                val showPaid = latestPaid > 0.0
                val showDue = latestDue > 0.0
                // 🏷️ TK-APPROVED (03.09.2026): the Discount box follows the very
                // same zero-rule -- no discount given, no box, and the other
                // three stay exactly where they always were.
                currentDiscount = data.discount
                val showDiscount = data.discount > 0.0
                binding.tvChipDiscount.text = if (showDiscount) money(data.discount) else "\u2014"
                binding.boxEstimated.visibility = if (showEstimated) View.VISIBLE else View.GONE
                binding.boxPaid.visibility = if (showPaid) View.VISIBLE else View.GONE
                binding.boxDue.visibility = if (showDue) View.VISIBLE else View.GONE
                binding.boxDiscount.visibility = if (showDiscount) View.VISIBLE else View.GONE
                binding.billSummaryRow.visibility =
                    if (hasBillingData && (showEstimated || showPaid || showDue || showDiscount)) View.VISIBLE else View.GONE

                // TK-LOCKED DESIGN (2026-07-23): the Date/Time/Type/By/Note
                // history table (originally Enquiry+Visit only) now replaces
                // the Paid/Due register table on the Patient Card too --
                // approved via full photo-proof (5 Enquiry calls + 5 Visit
                // calls + Advance + 5 Payments + Treatment Complete, all in
                // one table). The old register table/adapter code is left
                // in place (unused) rather than deleted, in case it's ever
                // needed again -- nothing else about it changed.
                binding.defaultUpdatesGroup.visibility = View.GONE
                binding.enquiryHistoryScroll.visibility = View.VISIBLE
                // isInquiryStage now computed earlier (right after tvName),
                // reused here unchanged.
                val isVisitStage = data.followupStage.equals("Patient", ignoreCase = true)
                // TK-APPROVED (2026-07-27, via photo proof): the header colour
                // itself now says which stage this patient is at, so a staff
                // member knows at a glance without reading anything:
                //   Enquiry  -> pale yellow blended into pale green
                //   Visit    -> pale yellow
                //   Patient  -> pale green
                // All three are pale, which is why every text in that header
                // is dark in the layout (white would be unreadable on them).
                binding.patientHeaderRoot.setBackgroundResource(
                    when {
                        isInquiryStage -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_enquiry
                        isVisitStage -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_visit
                        else -> com.tkbiswas.pilesclinic.R.drawable.bg_ptl_header_patient
                    }
                )
                // TK-DECISION (2026-07-24): default state before the
                // stage-specific block below; Enquiry/Visit hide/repurpose
                // it again as needed. Explicit reset here (not just relying
                // on XML's default) so this is correct even if a screen
                // instance somehow reloads across a stage change.
                binding.btnCall.visibility = View.VISIBLE
                binding.btnWhatsApp.visibility = View.VISIBLE
                if (isInquiryStage) {
                    // Not registered at all yet -- next step is Registration.
                    binding.btnPayment.text = "📝 Register"
                    binding.btnPayment.setBackgroundColor(android.graphics.Color.parseColor("#B8860B"))
                    binding.btnPayment.backgroundTintList = null
                    binding.btnPayment.setOnClickListener {
                        startActivity(Intent(this@PatientTimelineActivity, RegistrationActivity::class.java).putExtra("prefillMobile", currentMobile))
                    }
                    // TK-DECISION (2026-07-24): Full Journey removed on the
                    // Enquiry card -- there is no earlier stage to combine,
                    // so View All (default, unfiltered here) and Full
                    // Journey would show the exact same thing.
                    binding.btnCall.visibility = View.GONE
                    // TK-DECISION (2026-07-22): on an ENQUIRY only, the WhatsApp
                    // button is replaced by "⏰ আসার কথা". Reason: WhatsApp is
                    // already on the Enquiry tab card, and Enquiry has NO
                    // automatic Expected (uncertain), so a manual "আসার কথা"
                    // button is exactly what's needed here. Staff pick the date
                    // the patient actually promised to come; it writes one
                    // chamber_expected entry (deterministic id, so re-picking
                    // just moves the date). Other stages keep WhatsApp as-is.
                    binding.btnWhatsApp.text = NoBengali.s("⏰ আসার কথা")
                    binding.btnWhatsApp.setBackgroundColor(android.graphics.Color.parseColor("#0E7C7B"))
                    binding.btnWhatsApp.backgroundTintList = null
                    binding.btnWhatsApp.setOnClickListener { pickExpectedDateForEnquiry() }
                } else if (isVisitStage) {
                    // TK-CLARIFIED (2026-07-20): label only -- opens the same
                    // Payment/Advance screen as before, unchanged.
                    binding.btnPayment.text = "💵 Advance"
                    // TK-DECISION (2026-07-24, TK-LOCKED): Visit card also
                    // gets exactly 3 buttons now -- Full Journey removed
                    // (View All here IS filtered to Visit-only, but TK
                    // decided the button isn't needed regardless) and
                    // "📋 Report Card" replaced by the same "⏰ আসার কথা"
                    // Enquiry already has (identical function/behaviour,
                    // pickExpectedDateForEnquiry() is not actually
                    // Enquiry-specific -- it only keys off currentMobile).
                    binding.btnCall.visibility = View.GONE
                    binding.btnWhatsApp.text = NoBengali.s("⏰ আসার কথা")
                    binding.btnWhatsApp.setBackgroundColor(android.graphics.Color.parseColor("#0E7C7B"))
                    binding.btnWhatsApp.backgroundTintList = null
                    binding.btnWhatsApp.setOnClickListener { pickExpectedDateForEnquiry() }
                }
                // TK-REQUESTED (2026-07-24): Report Card only makes sense
                // once a bill exists (i.e. Advance has been taken) -- before
                // that it used to sit there and just Toast "no bill yet" on
                // tap. Now the button itself is hidden until hasBillingData
                // is true, reusing the same flag the Estimated/Paid/Due
                // chips already use.
                // TK-DECISION (2026-07-24): only applies on Patient/
                // Treatment stage now -- Enquiry and Visit both use that
                // button slot for "⏰ আসার কথা" instead (never Report Card),
                // set unconditionally in their own blocks above.
                if (!isInquiryStage && !isVisitStage) {
                    binding.btnWhatsApp.visibility = if (hasBillingData) View.VISIBLE else View.GONE
                }
                buildEnquiryHistoryTable(data)
                // TK-REQUESTED (2026-07-27): keep this patient's screen on the
                // phone so the NEXT time View All is tapped it appears at once
                // instead of waiting. Saving cannot fail loudly and cannot
                // affect anything on screen (see TimelineCache).
                // 🔵 TK-ORDER (07.08.2026): তথ্য খালি হলে (যা পড়া ব্যর্থ হলেও হতে
                // পারে) আর cache-এ বসাব না — নইলে ব্যর্থ পড়ার খালি Timeline ভালো
                // cache-এর ওপর বসে যেত ও পরের বারও ফাঁকা দেখাত। আসল রোগীর সবসময়
                // অন্তত একটা এন্ট্রি থাকে, তাই সত্যিকারের তথ্য কখনো cache-হারা হয় না।
                if (data.entries.isNotEmpty()) TimelineCache.save(this@PatientTimelineActivity, currentMobile, data, preferPatientRowId)   // 🔵 V522

                // 🔒 V1527: only NOW is every Action condition backed by the
                // fresh cloud read. A tap made during cache-first rendering is
                // released here, so the first menu and every later menu are built
                // from the same verified state. Queue auto-action uses the same gate.
                actionStateReady = true
                val shouldOpenAction = actionOpenPending || autoActionPending
                actionOpenPending = false
                autoActionPending = false
                if (shouldOpenAction) showTakeActionMenu()
            } catch (e: Exception) {
                // Never fall back to stale cache for an action that can change data.
                actionStateReady = false
                if (actionOpenPending || autoActionPending) {
                    actionOpenPending = false
                    autoActionPending = false
                    android.widget.Toast.makeText(
                        this@PatientTimelineActivity,
                        "Latest patient data could not be verified — please try again",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                binding.progressLoad.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                // TEMP DIAGNOSTIC (TK-approved, 2026-07-23): show the REAL
                // error reason so the exact cause can be found if this
                // happens again. Revert to the friendly message once found.
                android.widget.Toast.makeText(
                    this@PatientTimelineActivity,
                    "ERR: " + (e::class.java.simpleName) + " — " + (e.message ?: "no message"),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 🆕🔒 (07.08.2026, TK-অনুমোদিত) — 📜 History-র "Doctor Checkup" সারিতে
    // চাপলে সেই দিনের **A4 চেকআপ-রিপোর্ট** (ক্লিনিক-লেটারহেড + রোগীর তথ্য +
    // সব সেকশন) একটা পপ-আপে দেখায়, নিচে Print। রিপোর্টের চেহারা
    // `CheckupA4Report`-এ — সেভের পরে Checkup পর্দায় যা দেখা যায়, হুবহু তাই।
    //
    // ⛔ **ঝুঁকিহীন:** সেভ-করা `note` টেক্সট থেকেই বানানো হয় — কোনো নতুন
    // ক্লাউড-কল নেই (Supabase free-plan নিরাপদ), কোনো ডেটা লেখা/বদলানো হয় না,
    // এই পর্দার আর কোনো অংশ ছোঁয়া হয়নি।
    // ⚠️ সৎ সীমাবদ্ধতা: History-র সারিতে ছবি (Before/During/After) থাকে না
    // (ওগুলো আলাদা কলামে সেভ হয়), তাই পুরনো রিপোর্টে ছবির তিনটে ঘর ফাঁকা
    // দেখাবে — বাকি সব তথ্য ঠিকঠাক আসে।
    // ─────────────────────────────────────────────────────────────────────
    private var a4PrintWebView: android.webkit.WebView? = null

    // 🔒 B569 (08.08.2026, TK-অনুমোদিত প্রুফ): ডুপ্লিকেট "Full Journey" বোতামটাকে
    // "📜 History" বানানো হলো (View All আর Full Journey একই পর্দা — TK-এর যাচাই)।
    // চাপলে এই রোগীর সর্বশেষ Doctor Checkup সারির A4 রিপোর্ট (ফটোসহ) খোলে।
    // ⛔ চেকআপ না থাকলে শুধু জানানো হয়, কিছু ভাঙে না।
    /**
     * 🟢🔒 V627 (২৪.০৮.২০২৬, TK-নির্দেশ) — নাম বদলে "Checkup History", আর
     * চেকআপ **এখনো করা না হলে** টোস্ট দেখিয়ে থেমে না গিয়ে সরাসরি Doctor
     * Checkup ফর্ম খোলে (যাতে ওখান থেকেই চেকআপ শুরু করা যায়)। চেকআপ আগে
     * থেকেই থাকলে আচরণ অপরিবর্তিত — A4 রিপোর্ট দেখায়।
     */
    private fun openCheckupHistory() {
        // তালিকা নতুন-আগে সাজানো — তাই firstOrNull = সবচেয়ে সাম্প্রতিক চেকআপ।
        val checkup = currentEntries.firstOrNull {
            it.title.equals("Doctor Checkup", ignoreCase = true) ||
            it.title.equals("Doctor Check-up", ignoreCase = true)
        }
        if (checkup == null || checkup.note.isBlank()) {
            prepareClinicalRoleSession()
            startActivity(Intent(this, com.tkbiswas.pilesclinic.clinical.DoctorCheckupActivity::class.java))
            return
        }
        // 🟢🔒 V676 (২৫.০৮.২০২৬, TK-নির্দেশ) — এটা যদি **আজকের নিজের** চেকআপ
        // হয় এবং structured JSON থাকে (নতুন চেকআপ থেকেই সেভ হয়, V676-এর
        // আগের পুরনো রেকর্ডে নেই) — তাহলে View/Edit জিজ্ঞাসা করা হয়। নইলে
        // (পুরনো রেকর্ড/অন্য দিনের) আগের মতোই সরাসরি A4 দেখা যায়, কোনো
        // পরিবর্তন নেই।
        val isToday = checkup.date.take(10) == PaymentModel.today()
        if (isToday && checkup.medicalRecordId.isNotBlank() &&
            checkupRecordFromJsonStringOrNull(checkup.medicalSelected) != null
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, NoBengali.s("Today's Check-up")))
                .setMessage(NoBengali.s("Today's check-up is already saved — view it or edit it?"))
                .setPositiveButton("Edit") { _, _ ->
                    prepareClinicalRoleSession()
                    startActivity(
                        Intent(this, com.tkbiswas.pilesclinic.clinical.DoctorCheckupActivity::class.java)
                            .putExtra(com.tkbiswas.pilesclinic.clinical.DoctorCheckupActivity.EXTRA_EDIT_MEDICAL_ID, checkup.medicalRecordId)
                            .putExtra(com.tkbiswas.pilesclinic.clinical.DoctorCheckupActivity.EXTRA_EDIT_MEDICAL_JSON, checkup.medicalSelected)
                    )
                }
                .setNegativeButton("View") { _, _ ->
                    try {
                        showCheckupA4Dialog(checkup.note, checkup.date)
                    } catch (_: Throwable) {
                        android.widget.Toast.makeText(this, "Could not open the check-up record", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .show().also { PremiumAlert.paint(it) }
            return
        }
        try {
            showCheckupA4Dialog(checkup.note, checkup.date)
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, "Could not open the check-up record", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 🔴🔴🔴🔴 V480 (20.08.2026, TK-রিপোর্ট) — Medicine Slip টাইমলাইন-এন্ট্রি
    // চাপলে, সেভ-হওয়া flat note টেক্সট থেকে ঠিক আসল প্রিন্ট-পাইপলাইন
    // (`PrintDataHolder` → `PrintPreviewActivity`, MedicineSlipActivity যে
    // একই পথ ব্যবহার করে) দিয়ে A4 দেখায় — নতুন কোনো আলাদা টেমপ্লেট বানানো
    // হয়নি, TK যেটা মূলত তৈরি করেছিলেন ঠিক সেই একই ছাপার নিয়মেই দেখাবে।
    // ⛔ Save PDF/Share PDF/Print — সবই স্বয়ংক্রিয়ভাবে কাজ করে, কারণ
    // PrintPreviewActivity নিজেই এই কাজগুলো করে (নতুন কিছু বানাতে হয়নি)।
    private fun showMedicineSlipA4Print(note: String, dateText: String) {
        // সেভ হওয়ার ঠিক ফরম্যাট (persistSlipToHistory): প্রতিটা ওষুধ ";" দিয়ে
        // আলাদা, প্রতিটার ভেতরে "নাম · ডোজ · সময় · মেয়াদ" — কিন্তু ডোজ/সময়/
        // মেয়াদ ফাঁকা থাকলে বাদ পড়ে যেত, তাই অবস্থান ধরে না নিয়ে প্রথম অংশ
        // সবসময় নাম, বাকি অংশ জোড়া দিয়ে এক লাইন — এটাই PrintMappers.
        // medicineSlip()-এর নিজের lines-ফরম্যাটের সাথে মেলে ("dose • freq • days")।
        val medicineParts = note.split(";").map { it.trim() }.filter { it.isNotBlank() }
        val rxNames = ArrayList<String>()
        val lines = ArrayList<String>()
        for (part in medicineParts) {
            val fields = part.split("·").map { it.trim() }.filter { it.isNotBlank() }
            if (fields.isEmpty()) continue
            rxNames.add(fields[0])
            lines.add(if (fields.size > 1) fields.drop(1).joinToString(" • ") else "As advised")
        }
        if (rxNames.isEmpty()) throw IllegalStateException("no medicines parsed")
        val model = com.tkbiswas.pilesclinic.print.PrintDocumentModel(
            documentTitle = "Medicine Slip",
            branchName = currentBranch,
            patientName = currentPatientName,
            patientId = currentPatientCode.ifBlank { currentMobile },
            dateLabel = dateText,
            sections = listOf(com.tkbiswas.pilesclinic.print.PrintSection(null, lines, null, rxNames, null, null, null)),
            qrPayload = "PILESCLINIC|SLIP|$currentPatientCode|${System.currentTimeMillis()}",
            footerNote = "Please follow dosage exactly as advised by the doctor.",
            patientAddress = currentPatientAddress,
            patientAgeSex = listOf(currentPatientAge, currentPatientSex).filter { it.isNotBlank() }.joinToString(" / "),
            patientDisease = currentDisease,
            patientMobile = currentMobile
        )
        com.tkbiswas.pilesclinic.print.PrintDataHolder.pendingModel = model
        startActivity(android.content.Intent(this, com.tkbiswas.pilesclinic.print.PrintPreviewActivity::class.java))
    }


    /* ═══════════════════════════════════════════════════════════════════
       🩹🔒 V737 (২৭.০৮.২০২৬) — **পপ-আপের "কম্পন" চিরতরে বন্ধ**
       —————————————————————————————————————————————————————————————————
       TK-রিপোর্ট (ভিডিও): *"কম্পন হচ্ছে কেন?"* — Note পপ-আপ সেকেন্ডে
       কয়েকবার ছোট-বড় হচ্ছিল, দুটো Close বোতাম দেখা যাচ্ছিল।

       ── আসল কারণ (ভিডিওর ফ্রেম বের করে মেপে + কোড ধরে) ──────────────
       পপ-আপের ভিতরে **WebView** বসানো ছিল, তার উচ্চতা বাঁধা ছিল না
       (`WRAP_CONTENT`)। তাতে একটা **গোল চক্র** তৈরি হয়:
           পপ-আপের মাপ  →  WebView-এর চওড়া  →  লেখার উচ্চতা
                 ↑                                      ↓
                 └──────────  পপ-আপ আবার মাপে  ←────────┘
       একবার এদিকে, একবার ওদিকে — সেটাই চোখে **কম্পন**। ফিকে Close
       বোতামটা ছিল আগের মাপের ছবি, নতুনটা আসার আগে।
       `useWideViewPort` + `loadWithOverviewMode` চালু থাকায় WebView
       লেখাকে আবার মাপমতো ছোট-বড় করত — চক্রটা তাতে আরও জোরালো হতো,
       আর ছোট লেখার নিচে অত ফাঁকা জায়গাও তাই দেখাত।

       ── সমাধান ───────────────────────────────────────────────────────
       **চক্রটাই কেটে দেওয়া হলো।** WebView-এর উচ্চতা আর লেখার উপর
       নির্ভর করে না — একবার মেপে **বসিয়ে দেওয়া হয়, তারপর আর বদলায় না**
       (`applied` পাহারা)। পপ-আপ তাই দ্বিতীয়বার মাপেই না ⇒ কম্পনের
       কারণটাই আর থাকে না।

       ⛔ **লেখা · সাজ · রং · কার্ড — এক অক্ষরও বদলায়নি**; শুধু কত উঁচু
          হবে সেটা ঠিক করার নিয়ম বদলেছে।
       🔴 **V738-এ সংশোধন:** V737-এ `useWideViewPort`/`loadWithOverviewMode`
          তিনটে পাতাতেই বন্ধ করে দিয়েছিলাম — সেটা A4 কাগজের জন্য **ভুল** ছিল
          (নিচে বিস্তারিত)। এখন A4-এ ওরা আগের মতোই চালু; কম্পন থামানোর আসল
          কাজটা করে **উচ্চতা বেঁধে দেওয়া**, ওই দুটো সেটিং নয়।
       ⛔ মাপ না পেলে (কিছু ফোনে `contentHeight` ০ আসতে পারে) নিরাপদ
          মাপে বসে — তখনও কম্পন হয় না, শুধু ভিতরে স্ক্রল করতে হয়।
       ⛔ সর্বোচ্চ পর্দার ৭০% — লম্বা লেখায় পপ-আপ পর্দা ছাড়িয়ে যায় না।
       ⛔ সবচেয়ে ছোট ১৪০dp — দু-লাইনের নোটেও বোতাম চাপা পড়ে না।
       ⛔ এই একটাই ফাংশন **তিনটে পপ-আপেই** বসেছে (Note · Check-up Record ·
          Prescription Details), তাই একই দোষ আর কোথাও থাকল না।
       ═══════════════════════════════════════════════════════════════════ */
    private fun steadyWebView(
        html: String,
        zoomable: Boolean = false
    ): android.widget.FrameLayout {
        val d = resources.displayMetrics.density
        val screenH = resources.displayMetrics.heightPixels
        val maxH = (screenH * 0.70f).toInt()
        val minH = (140 * d).toInt()
        val fallbackH = (screenH * 0.55f).toInt().coerceIn(minH, maxH)

        val holder = android.widget.FrameLayout(this)
        // শুরুতেই একটা নিরাপদ মাপ — তাই প্রথম মাপাতেই পপ-আপ স্থির
        holder.layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, fallbackH
        )

        var applied = false      // 🔒 একবারই — এটাই চক্র ভাঙার চাবি
        val wv = android.webkit.WebView(this).apply {
            settings.javaScriptEnabled = false
            /* 🔴🔒 V738 — **নিজের V737-এর ভুল নিজেই ধরে ঠিক করা।**
               V737-এ আমি এই দুটো **সব পাতায়** বন্ধ করে দিয়েছিলাম। Note ও
               Prescription পাতায় তাতে কিছুই বদলায় না (ওদের পাতায় লেখা আছে
               `width=device-width` — বন্ধ থাকলেও WebView ওই মাপই নিত)।
               কিন্তু **Check-up Record-এর A4 কাগজে লেখা আছে `width=794`** —
               ওটা পড়তে হলে `useWideViewPort` **চালু** থাকতেই হবে, আর
               ৭৯৪-এর কাগজটাকে পর্দার মাপে ছোট করে দেখাতে `loadWithOverviewMode`।
               বন্ধ করে দিলে A4 কাগজটা ~৩৬০ পিক্সেলের সরু কলামে ঠাসা হয়ে যেত
               (হুবহু V698-এর সেই দোষ, যেটা TK ছবি দিয়ে ধরিয়েছিলেন)।
               ⇒ তাই A4-এর ক্ষেত্রে দুটোই **V737-এর আগের মতোই চালু**।
               ⛔ এতে কম্পন ফেরে না — কম্পন থামে **উচ্চতা বেঁধে দেওয়ায়**
                  (নিচের `applied` পাহারা), এই দুটোতে নয়। */
            settings.loadWithOverviewMode = zoomable
            settings.useWideViewPort = zoomable
            if (zoomable) {
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
            }
            isVerticalScrollBarEnabled = true
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                    if (applied) return
                    applied = true
                    try {
                        val ch = (view?.contentHeight ?: 0)
                        val px = (ch * resources.displayMetrics.density).toInt()
                        val want = if (px <= 0) fallbackH else px.coerceIn(minH, maxH)
                        val lp = holder.layoutParams
                        if (lp != null && lp.height != want) {
                            lp.height = want
                            holder.layoutParams = lp
                        }
                    } catch (_: Throwable) { /* মাপ না পেলে নিরাপদ মাপেই থাকবে */ }
                }
            }
            loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        }
        holder.addView(
            wv,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return holder
    }

    private fun showCheckupA4Dialog(note: String, dateText: String) {
        val html = com.tkbiswas.pilesclinic.clinical.CheckupA4Report.html(
            com.tkbiswas.pilesclinic.clinical.CheckupA4Report.Info(
                name = currentPatientName,
                patientId = currentPatientCode.ifBlank { currentMobile },
                age = currentPatientAge,
                sex = currentPatientSex,
                mobile = currentMobile,
                disease = currentDisease,
                address = currentPatientAddress,
                branch = currentBranch,
                date = dateText,
                photo = currentPatientPhoto   // 🔒 B569: টাইমলাইন থেকে খুললেও রোগীর ছবি বসে
            ),
            com.tkbiswas.pilesclinic.clinical.CheckupA4Report.parseDetails(note)
        )
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Check-up Record — " + displayDate(dateText)))   // 🟢 V1124
            // 🩹🔒 V737: কম্পন-মুক্ত — উচ্চতা একবার বসে, তারপর আর বদলায় না
            .setView(steadyWebView(html, zoomable = true))
            .setPositiveButton("Close", null)
            .setNeutralButton("Print") { _, _ -> printCheckupA4(html) }
            .create()
        dlg.show()
        PremiumAlert.paint(dlg)
    }

    private fun printCheckupA4(html: String) {
        try {
            val wv = android.webkit.WebView(this)
            wv.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                    try {
                        val pm = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val adapter = view.createPrintDocumentAdapter("DoctorCheckup")
                        pm.print("Doctor Checkup", adapter, PrintQuality.builder().build())
                    } catch (_: Throwable) {
                        android.widget.Toast.makeText(this@PatientTimelineActivity, "Print not available", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            a4PrintWebView = wv
            wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, "Print not available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 🔒 B568 (08.08.2026, TK-অনুমোদিত প্রুফ "সাজ ২ — নীল কার্ড"): টাইমলাইনের
    // জট-পাকানো Note একই লেখা ধরে পরিষ্কার নীল-কার্ডে দেখানো হয় (রিমার্ক /
    // স্ট্যাটাস / চেক-আপ / পেমেন্ট আলাদা কার্ডে)। ⛔ কোনো সেভ-করা তথ্য বদলায় না —
    // শুধু দেখানোর ধরন। ওয়েব app.js-এর wlv1NoteParse/wlv1NoteCards-এর হুবহু একই
    // নিয়ম, WebView-তে (CheckupA4Dialog-এর মতোই) — তাই Android ও ওয়েব এক রকম।
    // ⛔ কোনো ব্যতিক্রম হলে ডাকা জায়গায় try/catch পুরনো সাধারণ পপ-আপে ফিরে যায়।
    private fun noteHtmlEsc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /**
     * 🔴🔒 V512 — নতুন প্যারামিটার `onEdit`। `null` হলে পপ-আপ **হুবহু আগের
     * মতোই** (শুধু Close)। `null` না হলে পাশে একটা ✏️ Edit বোতাম বসে, যেটা
     * ডাকার জায়গা থেকে পাঠানো ঐ পুরোনো `editEnquiryHistoryNote()`-ই চালায়।
     * ⛔ Prescription-এর সারি নিচেই আলাদা পর্দায় চলে যায় — সেখানে `onEdit`
     *    পাঠানো হয় না, তাই V327-এর "শুধু দেখার" লক অটুট।
     */
    private fun showNoteCardsDialog(note: String, dateText: String, rowTitle: String = "", onEdit: (() -> Unit)? = null) {
        val txt = note.trim()
        val isPrescription = rowTitle.contains("Prescription", ignoreCase = true)
        if (isPrescription && txt.isNotBlank()) {
            showPrescriptionDetailsDialog(txt, dateText)
            return
        }
        val blocks = txt.split("|").map { it.trim() }.filter { it.isNotBlank() }
        val remarks = ArrayList<String>()
        val status = ArrayList<String>()
        val kv = ArrayList<Pair<String, String>>()
        var pay: String? = null
        val payRe = Regex("^₹\\s*[0-9,]+")
        val statusRe = Regex(
            "^(Converted|Registered|Advance|Bill|Follow|Visit|Treatment|Refund|Enquiry|Marked|Moved|Inquiry)",
            RegexOption.IGNORE_CASE
        )
        /* 💰🔒 V736 (TK-অনুমোদিত ডেমো) — আগে শুধু **"₹" দিয়ে শুরু** হওয়া
           টুকরোটাকে টাকার কার্ডে পাঠানো হত। কিন্তু পেমেন্টের নোট আসে
           "KSHAR SUTRA করা হল — ₹400 · CASH" ধাঁচে — শুরুতে মানুষের লেখা,
           তাই ওটা রিমার্কের কার্ডে ঢুকে যেত আর সব একসাথে মেশানো দেখাত।
           এখন **যে টুকরোয় "— ₹" আছে** সেটাও টাকার কার্ডে যায়।
           ⛔ যে টুকরোয় ₹ নেই তার আচরণ এক অক্ষরও বদলায়নি। */
        val payJoined = Regex("[—-]\\s*₹\\s*[0-9,]")
        for (b in blocks) {
            if (payRe.containsMatchIn(b) || payJoined.containsMatchIn(b)) { pay = b; continue }
            if (b.contains(":") && b.count { it == ';' } >= 1) {
                for (pairRaw in b.split(";")) {
                    val pair = pairRaw.trim()
                    if (pair.isBlank()) continue
                    val ci = pair.indexOf(":")
                    if (ci > 0) {
                        val k = pair.substring(0, ci).trim()
                        val v = pair.substring(ci + 1).trim()
                        if (v.isNotBlank()) kv.add(k to v) else if (k.isNotBlank()) status.add(k)
                    } else status.add(pair)
                }
                continue
            }
            if (statusRe.containsMatchIn(b)) { status.add(b); continue }
            remarks.add(b)
        }
        val sb = StringBuilder()
        if (remarks.isEmpty() && status.isEmpty() && kv.isEmpty() && pay == null) {
            sb.append("<div class=\"nkCard\"><div class=\"nkNote\">")
                .append(noteHtmlEsc(txt.ifBlank { "—" })).append("</div></div>")
        } else {
            /* 🟢🔒 V1124 (TK-রিপোর্ট ছবিসহ) — রেজিস্ট্রেশনের সারিতে লেখাটা
               "REMARK" নয়, ওটা **রোগীর সমস্যা ও আগের চিকিৎসার বিবরণ**; ভুল
               শিরোনাম বিভ্রান্ত করত। ⛔ বাকি প্রতিটা সারিতে শিরোনাম হুবহু আগের। */
            val remarkTitle = if (rowTitle.contains("Registration", ignoreCase = true))
                "Complaint & History" else "Remark"
            if (remarks.isNotEmpty()) sb.append("<div class=\"nkCard\"><div class=\"nkT\">"+noteHtmlEsc(NoBengali.s(remarkTitle))+"</div><div class=\"nkNote\">")
                .append(remarks.joinToString("<br>") { noteHtmlEsc(it) }).append("</div></div>")
            if (status.isNotEmpty()) {
                sb.append("<div class=\"nkCard\"><div class=\"nkT\">"+noteHtmlEsc(NoBengali.s("🧾 স্ট্যাটাস"))+"</div><div class=\"nkChips\">")
                for (s in status) sb.append("<span class=\"nkChip\">").append(noteHtmlEsc(s)).append("</span>")
                sb.append("</div></div>")
            }
            if (kv.isNotEmpty()) {
                sb.append("<div class=\"nkCard\"><div class=\"nkT\">"+noteHtmlEsc(NoBengali.s("🩺 ডাক্তার চেক-আপ"))+"</div>")
                for (p in kv) sb.append("<div class=\"nkKv\"><span class=\"nkK\">").append(noteHtmlEsc(p.first))
                    .append("</span><span class=\"nkV\">").append(noteHtmlEsc(p.second)).append("</span></div>")
                sb.append("</div>")
            }
            if (pay != null) sb.append("<div class=\"nkCard\"><div class=\"nkT\">"+noteHtmlEsc(NoBengali.s("💰 Payment Note"))+"</div><div class=\"nkPay\">")
                .append(noteHtmlEsc(pay!!)).append("</div></div>")
        }
        val html = "<!doctype html><html><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>" +
            "*{box-sizing:border-box;margin:0;padding:0;font-family:'Noto Sans Bengali',system-ui,Arial,sans-serif;-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
            "body{background:#eef3fa;padding:10px}" +
            ".nkCard{background:#fff;border:1px solid #e2ebf6;border-radius:14px;padding:11px 13px;margin-bottom:10px;box-shadow:0 4px 12px rgba(16,34,58,.05)}" +
            ".nkCard:last-child{margin-bottom:0}" +
            ".nkT{font-size:12px;font-weight:900;color:#1457b8;text-transform:uppercase;margin-bottom:7px}" +
            ".nkNote{font-size:14px;color:#10223a;line-height:1.45}" +
            ".nkChips{display:flex;flex-wrap:wrap;gap:5px}" +
            ".nkChip{display:inline-block;background:#eef4ff;color:#1457b8;font-size:12.5px;font-weight:800;padding:3px 10px;border-radius:10px}" +
            ".nkKv{display:grid;grid-template-columns:120px 1fr;gap:6px;padding:3px 0;font-size:13.5px}" +
            ".nkKv .nkK{color:#667085;font-weight:700}.nkKv .nkV{color:#10223a;font-weight:800}" +
            ".nkPay{color:#0b7a34;font-size:18px;font-weight:900}" +
            "</style></head><body>" + sb.toString() + "</body></html>"
        /* 🟢🔒 V1124 (০৫.০৯.২০২৬, TK-রিপোর্ট ছবিসহ — *"এটা কি ঠিক?"*) —
           পপ-আপের মাথায় তারিখটা **কাঁচা ধাঁচে** (2026-07-06) দেখাত, অথচ TK-এর
           লক করা নিয়ম (২৪.০৭.২০২৬): পর্দার প্রতিটা তারিখ **০৬.০৭.২০২৬** ধাঁচে।
           এই পর্দাতেই ঠিক ধাঁচটা (`displayDate`) আগে থেকেই আছে, শুধু এখানে
           ব্যবহার হয়নি। ⇒ চারটে পপ-আপেই এখন সেটাই ব্যবহার হয় (নিয়ম ৭)।
           ⛔ `displayDate` আগে থেকে সাজানো তারিখে হাত দেয় না (ঠিক ধাঁচে থাকলে
              হুবহু ফেরত দেয়), তাই কোনো ডাকনেওয়ালা ভাঙে না। */
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Note — " + displayDate(dateText)))
            // 🩹🔒 V737: কম্পন-মুক্ত — উচ্চতা একবার বসে, তারপর আর বদলায় না
            .setView(steadyWebView(html))
            .setPositiveButton("Close", null)
            // 🔴🔒 V512: সংশোধনের পথ এখন চোখে দেখা যায়। ⛔ কাজটা করে ঐ
            //    পুরোনো `editEnquiryHistoryNote()`-ই — নতুন সেভের পথ নেই।
            .apply { if (onEdit != null) setNeutralButton("✏️ Edit") { _, _ -> onEdit?.invoke() } }
            .create()
        dlg.show()
        PremiumAlert.paint(dlg)
    }

    /** V327 owner-approved proof: Prescription history is display-only and
     * keeps the exact saved note. Semicolon-separated medicines become
     * separate white cards; dot-separated directions become plain boxes with
     * no How often / Use / Dose / Direction / When labels. */
    private fun showPrescriptionDetailsDialog(note: String, dateText: String) {
        val medicines = note.split(";").map { it.trim() }.filter { it.isNotBlank() }
        val body = StringBuilder()
        for ((index, raw) in medicines.withIndex()) {
            val parts = raw.split("·").map { it.trim() }.filter { it.isNotBlank() }
            val name = parts.firstOrNull().orEmpty().ifBlank { raw }
            val directions = if (parts.size > 1) parts.drop(1) else emptyList()
            body.append("<div class=\"rxCard\"><div class=\"rxHead\"><span class=\"rxNo\">")
                .append(index + 1).append("</span><span class=\"rxName\">")
                .append(noteHtmlEsc(name)).append("</span></div>")
            if (directions.isNotEmpty()) {
                body.append("<div class=\"rxBoxes\">")
                for (direction in directions) body.append("<span class=\"rxBox\">")
                    .append(noteHtmlEsc(direction)).append("</span>")
                body.append("</div>")
            }
            body.append("</div>")
        }
        val html = "<!doctype html><html><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><style>" +
            "*{box-sizing:border-box;margin:0;padding:0;font-family:'Noto Sans Bengali',system-ui,Arial,sans-serif;-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
            "body{background:#fff;padding:10px}" +
            ".rxCard{background:#fff;border:1px solid #cfe2d8;border-radius:15px;padding:13px;margin-bottom:11px;box-shadow:0 3px 10px rgba(18,64,47,.07)}" +
            ".rxCard:last-child{margin-bottom:0}.rxHead{display:flex;align-items:center;gap:10px}" +
            ".rxNo{width:28px;height:28px;border-radius:8px;display:flex;align-items:center;justify-content:center;background:#0f7b4d;color:#fff;font-size:13px;font-weight:800;flex:none}" +
            ".rxName{color:#17312a;font-size:15px;font-weight:800;line-height:1.35}" +
            ".rxBoxes{display:flex;flex-wrap:wrap;gap:7px;margin-top:10px}" +
            ".rxBox{background:#eef8f2;color:#29483c;border-radius:9px;padding:7px 9px;font-size:12.5px;font-weight:700;line-height:1.3;flex:1 1 42%}" +
            ".rxBox:nth-child(even){background:#eef5ff}" +
            "</style></head><body>" + body + "</body></html>"
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Prescription Details — " + displayDate(dateText)))   // 🟢 V1124
            // 🩹🔒 V737: কম্পন-মুক্ত — উচ্চতা একবার বসে, তারপর আর বদলায় না
            .setView(steadyWebView(html))
            .setPositiveButton("Close", null)
            .create()
        dlg.show()
        PremiumAlert.paint(dlg)
    }
}
