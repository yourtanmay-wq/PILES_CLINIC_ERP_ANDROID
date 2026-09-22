package com.tkbiswas.pilesclinic.native

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityEnquiryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume   // 🔵 V534
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Native rebuild Step 2 -- Enquiry.
 *
 * Same fields, same validation rules, and the same duplicate-mobile check
 * as the WebView's enquiryForm()/saveEnq() (see EnquiryModel.kt /
 * EnquiryRepository.kt for exactly which rules were mirrored). A saved
 * enquiry is fully interchangeable with a WebView-saved one -- it appears
 * in the same Follow-up (Enquiry tab) list on any device/screen.
 *
 * SCOPED LIMITATION (by design, for this step): the WebView's duplicate
 * popup offers "Continue to Registration" / "Reject" actions that open
 * other native screens (Registration, Follow-up), which now exist. This
 * native version shows the same duplicate information but only offers
 * "Save Anyway" / "Cancel" -- the fuller duplicate-handling options will
 * naturally become available here as Registration and Follow-up get their
 * own native rebuild steps.
 */
class EnquiryActivity : AppCompatActivity() {

    /**
     * 🔵🔒 V534: প্রজেক্টের সেই একই পর্দা (Payment · Print · Doctor Visit ·
     * Chamber-এ যেটা চলছে) — একাধিক আলাদা রোগী থাকলে তবেই দেখা যায়।
     */
    private suspend fun askWhichRestorePatient(
        mobile: String, people: List<org.json.JSONObject>
    ): org.json.JSONObject? = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val labels = people.map { r ->
            (r.s("name").ifBlank { "UNKNOWN" }) + "\n" + (r.s("patientId").ifBlank { "-" })
        }.toTypedArray()
        var done = false
        fun finishWith(v: org.json.JSONObject?) {
            if (done) return
            done = true
            if (cont.isActive) cont.resume(v)
        }
        if (isFinishing || isDestroyed) { finishWith(null); return@suspendCancellableCoroutine }
        AlertDialog.Builder(this)
            .setTitle("\uD83D\uDCDE $mobile \u2014 which patient?")
            .setItems(labels) { _, which -> finishWith(people.getOrNull(which)) }
            .setNegativeButton("Cancel") { _, _ -> finishWith(null) }
            .setOnCancelListener { finishWith(null) }
            .show()
    }

    private lateinit var binding: ActivityEnquiryBinding
    private lateinit var repository: EnquiryRepository

    private val branches = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
    // TK-REQUESTED (2026-07-24): placeholder-first list for the mandatory,
    // never-auto-selected Branch spinner on this form.
    private val branchesWithPlaceholder = listOf("Select Branch") + branches
    private val diseases = listOf("Choose Disease", "🩸 Piles", "✂️ Fissure", "🔄 Fistula", "💧 Hydrocele", "🛡️ Gupt Rog", "📋 Other")
    private val diseaseValues = listOf("", "Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog", "Other")
    private val timings = listOf("Official Time", "Unexpected Time")
    /* 🩺 V1070 — Registration-এর হুবহু একই তালিকা (`RegistrationActivity.refByOptions`) */
    private val enqRefByOptions = listOf("Self", "Online", "Offline", "Dr. Visit", "Old Patient", "Others")

    /* 🔴🔒 V1151 (০৬.০৯.২০২৬, TK-নির্দেশ: *"যে কোন Enquiry / Visit / পেশেন্টের
       Edit করার প্রয়োজন হলে full form edit-এর option আসবে — Registration হওয়ার
       আগ অবদি All Branch Enquiry Form"*)।
       ফাঁকা হলে পর্দাটা **হুবহু আগের মতোই** নতুন এনকোয়ারির ফর্ম; ভরা থাকলে
       ওই সারিটাই এডিট হচ্ছে ⇒ সেভ করলে **নতুন সারি বসে না**, একই সারিতে লেখা হয়।
       ⛔ কল-হিস্ট্রি · কল-গোনা · ধাপ · status — কিচ্ছু ছোঁয়া হয় না। */
    private var editEnquiryId: String = ""

    private var selectedDate: String = EnquiryModel.today()
    private var selectedNextFollow: String = ""
    private var lastDupChecked = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnquiryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FormStar.paint(binding.root)  // red '*' on mandatory labels (visual only)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        repository = EnquiryRepository(this)
        MobileInput.attach(binding.etMobile)

        // Live duplicate check (web's eMobDupBox): as soon as a full 10-digit
        // number is typed, show whether it already exists in enquiries/patients.
        binding.etMobile.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val digits = (s?.toString() ?: "").filter { it.isDigit() }.takeLast(10)
                if (digits.length != 10) { binding.tvDupWarning.visibility = View.GONE; lastDupChecked = ""; return }
                if (digits == lastDupChecked) return
                lastDupChecked = digits
                refreshTimingForMobile(digits)   // ☎️ V963
                lifecycleScope.launch {
                    val dup = withContext(Dispatchers.IO) { repository.checkDuplicate(StaffDirectory.normalizeMobile(digits)) }
                    binding.tvDupWarning.visibility = View.GONE
                    // As soon as a full number that already exists is entered, show
                    // an immediate popup (which branch / section) with View / Cancel.
                    if (dup.found) showDuplicatePopup(dup, digits)
                }
            }
        })
        // 🆕 B467 (05.08.2026, TK-নির্দেশ) — Briefing-এ পাঠানো নম্বরে কল করে
        // ফেরত এলে, সেই নম্বর কোনো Enquiry/Visit/Patient-এ না মিললে এই
        // পর্দা "prefillMobile" extra দিয়ে খোলে — ঘরটা আগে থেকেই ভরা
        // থাকে, স্টাফকে আবার টাইপ করতে হয় না। ⛔ extra না থাকলে (স্বাভাবিক
        // Enquiry) এক অক্ষরও বদলায় না। **TextWatcher বসানোর পরে বসানো
        // হলো** (যাচাইয়ের সময় নিজে ধরা — আগে বসালে prefill করা নম্বরে
        // ডুপ্লিকেট-চেক স্বয়ংক্রিয়ভাবে চলত না, staff-কে আবার একবার লিখে
        // মুছে টাইপ করতে হতো)।
        intent.getStringExtra("prefillMobile")?.let { if (it.isNotBlank()) binding.etMobile.setText(it) }
        // 🔴 V1151 — এডিট-মোড: সারিটা এনে ঘরগুলো ভরে দেওয়া হয়।
        editEnquiryId = intent.getStringExtra("editEnquiryId")?.trim().orEmpty()
        if (editEnquiryId.isNotBlank()) loadEnquiryForEdit(editEnquiryId)

        val user = NativeSession.current(this)
        if (user == null) {
            finish()
            return
        }

        setupSpinners(user)
        // TK-REQUESTED (2026-07-24): Branch on this All-Branch Enquiry form
        // must NEVER auto-select or stay locked -- every save requires an
        // active, deliberate choice, every single time. 3-tap lock removed
        // for Branch only; "Call Received By" lock below is untouched.
        SpinnerLock.attach(binding.spReceivedBy, "Call Received By")
        setupEnqRefBy()   // 🩺 V1070
        wireEnqRmpSuggest(user)   // 🩺 V1095 — টাইপ করতে করতে RMP সাজেশন
        setupTimingButtons()
        setupDiseaseButtons()
        binding.tvDate.text = displayDate(selectedDate)
        binding.tvDate.setOnClickListener { pickDate() }
        binding.tvNextFollow.setOnClickListener { pickNextFollow() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { validateAndSave(user) }
        // TK-REQUESTED (2026-07-18): Save Enquiry button color -> green.
        // Only this button on this screen; the shared bg_gradient_button
        // drawable (used by Registration/Follow-Calendar/Login) is untouched.
        binding.btnSave.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#16A36D"))

        // Retry any enquiry that was queued locally because the device was
        // offline at the time, quietly, in the background.
        lifecycleScope.launch(Dispatchers.IO) { repository.flushPending() }
    }

    private fun setupSpinners(user: NativeUser) {
        // TK-REQUESTED (2026-07-24): no auto-select, no default -- Branch
        // starts on this placeholder ("Select Branch") every single time,
        // so Save's existing "Branch mandatory" check (validateAndSave
        // below) always forces an active choice.
        // TK RULE (2026-07-28): in the ENQUIRY form this is the same for every
        // role -- Master, Staff, Doctor -- everybody picks the Branch each time
        // and nothing is locked here. (The 3-tap lock belongs to Registration.)
        binding.spBranch.adapter = object : ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_dropdown_item, branchesWithPlaceholder
        ) {
            // TK-REPORTED (2026-07-28): an unanswered box must not look filled
            // in, so the placeholder row is drawn in the light hint grey.
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                (v as? android.widget.TextView)?.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        this@EnquiryActivity,
                        if (position == 0) com.tkbiswas.pilesclinic.R.color.field_hint
                        else com.tkbiswas.pilesclinic.R.color.clinic_text_primary
                    )
                )
                return v
            }
        }
        binding.spBranch.setSelection(0)
        SpinnerPicker.attach(binding.spBranch, "SELECT BRANCH", hidePlaceholder = true)

        // Call Received By (locked with TK): ALL branches' ALL staff + master.
        // The dropdown shows only the code (name); the mobile stays in the system.
        val eligible = StaffDirectory.allAccounts()
            .filter { it.role == "master" || it.role == "staff" }
            .toMutableList()
        if (eligible.none { it.mobile == user.mobile }) {
            StaffDirectory.findAccount(user.mobile)?.let { eligible.add(0, it) }
        }
        receivedByMobiles = eligible.map { it.mobile }
        val display = eligible.map { if (it.role == "master") "Dr. ${it.name}" else it.name }
        binding.spReceivedBy.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, display)
        val meIdx = receivedByMobiles.indexOf(user.mobile)
        if (meIdx >= 0) binding.spReceivedBy.setSelection(meIdx)
    }

    private var receivedByMobiles: List<String> = emptyList()

    /* 🩺🔒 V1000 (০৩.০৯.২০২৬, TK-নির্দেশ: "এখানে একাধিক রোগ চুজ করা যায় না —
       একই লোকের তো দুই রকমের রোগ থাকতেই পারে") — আগে একটাই রোগ বাছা যেত।
       এখন যতগুলো দরকার বাছা যায়; আবার চাপলে বাছাই উঠে যায়।
       ⛔ সেভে যা যায় সেটা আগের মতোই একটাই লেখা — একাধিক হলে ", " দিয়ে
          জোড়া (রেজিস্ট্রেশনের `diagnosis` ঘরে বহুদিন ধরে এই নিয়মই চলে,
          PatientModel.kt-এ `diseases.joinToString(", ")`)। তাই ডেটাবেসে
          নতুন কোনো ঘর লাগেনি। */
    private val selectedDiseaseSet = LinkedHashSet<String>()

    /** বাছা রোগগুলো — সবসময় বোতামের নিজের ক্রমেই, ", " দিয়ে জোড়া। */
    private val selectedDisease: String
        get() = diseaseValues.filter { it.isNotEmpty() && selectedDiseaseSet.contains(it) }.joinToString(", ")


    private fun setupDiseaseButtons() {
        val map = listOf(
            binding.btnDisPiles to "Piles",
            binding.btnDisFissure to "Fissure",
            binding.btnDisFistula to "Fistula",
            binding.btnDisHydrocele to "Hydrocele",
            binding.btnDisGupt to "Gupt Rog",
            binding.btnDisOther to "Other"
        )
        map.forEach { (btn, value) -> btn.setOnClickListener { selectDisease(value, map) } }

        // TK-REQUESTED (2026-07-18): default (nothing tapped yet) look for these
        // 6 disease buttons was the app's dark-navy theme color, which TK does
        // not want here. Default/unselected is now grey; tapping a button turns
        // it green (see selectDisease below). Nothing else on this form changed.
        // 🩺 V1000 — এক জায়গা থেকেই রং বসে, তাই বাছা থাকলে সবুজই থাকে।
        paintDiseaseButtons(map)
    }

    /* 🔴🔒 V1151 — এডিট-মোডে সারিটা এনে ঘরগুলো ভরে দেয়।
       ⛔ পড়া ব্যর্থ হলে ফর্মটা ফাঁকাই থাকে আর একটা বার্তা ওঠে — ভুল তথ্য
          বসিয়ে দেওয়ার চেয়ে কিছু না বসানোই নিরাপদ। */
    private fun loadEnquiryForEdit(id: String) {
        BackgroundWork.run {
            val rows = try {
                SupabaseClient.fetchListOrNull("enquiries", "id=eq.$id", 1, "createdAt.desc.nullslast", "*")
            } catch (_: Throwable) { null }
            val row = if (rows != null && rows.length() > 0) rows.optJSONObject(0) else null
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (row == null) {
                    editEnquiryId = ""
                    android.widget.Toast.makeText(this, "Could not open this enquiry — please try again", android.widget.Toast.LENGTH_LONG)
                        .show().also { try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }
                    return@post
                }
                try { fillFormFromRow(row) } catch (_: Throwable) { }
            }
        }
    }

    /** সারির লেখাগুলো ঘরে বসানো — ওয়েবের `wlv1EnqFillForm`-এর হুবহু যমজ। */
    private fun fillFormFromRow(row: org.json.JSONObject) {
        fun sv(k: String) = row.optString(k, "").trim()
        binding.etMobile.setText(StaffDirectory.normalizeMobile(sv("mobile")))
        binding.etName.setText(sv("name"))
        binding.etAddress.setText(sv("address"))
        binding.etRemarks.setText(sv("remarks"))
        sv("date").take(10).let { if (it.isNotBlank()) { selectedDate = it; binding.tvDate.text = displayDate(it) } }
        sv("nextFollow").take(10).let { if (it.isNotBlank()) { selectedNextFollow = it; binding.tvNextFollow.text = displayDate(it) } }
        // ব্রাঞ্চ
        val br = sv("branch")
        if (br.isNotBlank()) {
            val ad = binding.spBranch.adapter
            for (i in 0 until (ad?.count ?: 0)) {
                if (ad.getItem(i)?.toString()?.trim().equals(br, ignoreCase = true)) { binding.spBranch.setSelection(i); break }
            }
        }
        // রোগ (একাধিক হতে পারে, ", " দিয়ে জোড়া)
        selectedDiseaseSet.clear()
        sv("disease").split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { d ->
            diseaseValues.firstOrNull { it.equals(d, ignoreCase = true) }?.let { selectedDiseaseSet.add(it) }
        }
        try { setupDiseaseButtons() } catch (_: Throwable) { }
        // সময়ের ধরন — সারিতে যা আছে সেটাই বাছা থাকে
        val tt = sv("timeType")
        if (tt.isNotBlank()) { try { selectTiming(tt) } catch (_: Throwable) { } }
        /* ⛔ এডিটে সময়ের ঘরটা আর কল-তালিকা দেখে বদলানো হয় না — সারিতে যা জমা
           আছে সেটাই থাকে (স্টাফ চাইলে বোতাম চেপে বদলাতে পারেন, আগের নিয়মেই)। */
        timingIsAuto = false
        // কে পাঠিয়েছেন
        val rb = sv("refBy")
        if (rb.isNotBlank()) {
            val ad = binding.spEnqRefBy.adapter
            for (i in 0 until (ad?.count ?: 0)) {
                if (ad.getItem(i)?.toString()?.trim().equals(rb, ignoreCase = true)) { binding.spEnqRefBy.setSelection(i); break }
            }
        }
        binding.etEnqRefDoctorName.setText(sv("refDoctor"))
        binding.etEnqRefDoctorMobile.setText(sv("refDoctorMobile"))
        // কে কল ধরেছিলেন
        val rcv = StaffDirectory.normalizeMobile(sv("receivedBy"))
        if (rcv.length == 10) {
            val idx = receivedByMobiles.indexOfFirst { StaffDirectory.normalizeMobile(it) == rcv }
            if (idx >= 0) binding.spReceivedBy.setSelection(idx)
        }
    }

    private fun selectDisease(value: String, map: List<Pair<android.widget.Button, String>>) {
        // 🩺 V1000 — চাপলে যোগ, আবার চাপলে বাদ (একাধিক রোগ বাছা যায়)।
        if (!selectedDiseaseSet.remove(value)) selectedDiseaseSet.add(value)
        paintDiseaseButtons(map)
    }

    private fun paintDiseaseButtons(map: List<Pair<android.widget.Button, String>>) {
        val green = android.graphics.Color.parseColor("#16A36D")
        val grey = android.graphics.Color.parseColor("#9AA5B1")
        map.forEach { (btn, v) ->
            val on = selectedDiseaseSet.contains(v)
            btn.backgroundTintList = android.content.res.ColorStateList.valueOf(if (on) green else grey)
            btn.setTextColor(android.graphics.Color.WHITE)
        }
    }

    private var selectedTiming = "Official Time"

    /* ☎️🕘🔒 V963 (০১.০৯.২০২৬, TK-এর লক করা নিয়ম, ফটো-প্রুফ পাশ) —
       TK-এর নিয়ম হুবহু:
         · ভারতীয় সময়ে সকাল ৯.০০ – সন্ধ্যা ৬.০০ = Official, বাইরে = Unexpected
         · নম্বর লেখামাত্রই অ্যাপ **ব্রাঞ্চের ফোনের কল-তালিকায়** ওই নম্বরের
           **প্রথম কল** খোঁজে (গত ৩ দিনের মধ্যে)
         · কল পাওয়া গেল ও সেটা **অফিসিয়াল** সময়ের ⇒ Unexpected বোতামটা
           **লুকিয়ে যায়** — স্টাফ চাইলেও বাছতে পারবে না
         · কল পাওয়া গেল ও সেটা **অসময়ের** ⇒ দুটো বোতামই থাকে, Unexpected
           বাছা থাকে (TK: *"অ্যাপ তো অনেক সময় ভুল করতেই পারে"*)
         · কল পাওয়া গেল না ⇒ দুটোই থাকে, Official বাছা; Unexpected চাপলে
           Unexpected চাপলে শুধু একটা "Are you sure? Yes/No" সতর্কতা (V966·V967)
       ⛔ ক্লাউডে একটাও অনুরোধ নেই — ফোনের নিজের কল-তালিকা থেকেই।
       ⛔ V962-এর "কোনো বোতামই চাপা যাবে না" নিয়মটা TK-এর নতুন নির্দেশে
          তুলে নেওয়া হলো। */
    private fun timingOf(whenMs: Long): String {
        val c = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        c.timeInMillis = whenMs
        val mins = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        // সকাল ৯.০০ (৫৪০) থেকে সন্ধ্যা ৬.০০ (১০৮০) পর্যন্ত — দুটোই ধরা
        return if (mins in 540..1080) "Official Time" else "Unexpected Time"
    }

    private fun autoTimingNow(): String = timingOf(System.currentTimeMillis())

    /** কল-তালিকায় ওই নম্বরের প্রথম কল পাওয়া গেছে কি না (পেলে তার সময়)। */
    private var branchCallMs: Long? = null

    /* 🕐🔒 V1042 (TK-নির্দেশ) — সময়টা **অ্যাপ নিজে** কল-তালিকা থেকে বুঝেছে কিনা।
       কলটা চেম্বারের ফোনে এলে কল-তালিকায় পাওয়া যায় ⇒ auto। স্টাফের নিজের
       ফোনে এলে অ্যাপ কিছুই জানে না, স্টাফ হাতে বেছে দেন ⇒ hand।
       ⛔ অ্যাপ বুঝে দেওয়ার পরেও স্টাফ বোতাম চেপে বদলালে সেটা আর auto নয়। */
    private var timingIsAuto = false

    /* 🩺🔒 V1070 (TK-নির্দেশ) — "Dr. Visit" বাছলে তবেই ডাক্তারের নাম ও নম্বরের
       ঘর দেখা যায় (Registration-এর হুবহু একই আচরণ)।
       ⛔ কোনো ঘর বাধ্যতামূলক নয়; অন্য কিছু বাছলে ঘর দুটো ফাঁকা করে লুকিয়ে যায়। */
    private fun setupEnqRefBy() {
        binding.spEnqRefBy.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, enqRefByOptions)
        binding.spEnqRefBy.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    val show = enqRefByOptions.getOrNull(pos) == "Dr. Visit"
                    binding.llEnqRefDoctor.visibility = if (show) View.VISIBLE else View.GONE
                    if (!show) {
                        binding.etEnqRefDoctorName.setText("")
                        binding.etEnqRefDoctorMobile.setText("")
                    }
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
    }

    /* ═══════════════════════════════════════════════════════════════
       🩺🔒 V1095 (০৫.০৯.২০২৬) — TK: *"All Branch Enquiry Form-এ RMP-র নাম
       সাজেশন কেন দেখাচ্ছে না"*

       🔴 **আমারই বাদ পড়া:** V1070-এ এই ফর্মে "Doctor / RMP Name" ও "Doctor
       Mobile" ঘর দুটো বসিয়েছিলাম, কিন্তু Registration-এর **টাইপ করতে করতে
       সাজেশন** (V895) এখানে বসাতে ভুলে গিয়েছিলাম — তাই "PK" লিখলেও কিছু নামত না।

       ⛔ তালিকাটা `RmpPicker.cachedRmpChoices` থেকেই আসে — অর্থাৎ **ফোনে আগে
          থেকে জমানো ঘর**; নতুন কোনো ইন্টারনেট-ডাক নেই, Egress-এ এক বাইটও বাড়ে না।
       ⛔ `RegistrationActivity`-তে এক অক্ষরও হাত দেওয়া হয়নি — চালু পর্দাটা যেন
          কোনোভাবেই না ভাঙে।
       ⛔ নাম বা নম্বর — যেটাতেই লিখুন, দুটোতেই কাজ করে (Registration-এর মতোই)।
       ⛔ দুই অক্ষরের কম লিখলে তালিকা ওঠে না; কিছু না মিললে চুপচাপ লুকিয়ে থাকে।
       ═══════════════════════════════════════════════════════════════ */
    private var enqRmpMuted = false

    /* 🟢🔒 V1132 (০৬.০৯.২০২৬, TK-অনুমোদিত) — TK: *"জলপাইগুড়ির স্টাফ কোচবিহারের
       রোগীর জন্য RMP TK BISWAS বাছতে চাইছে, নাম-নম্বর সম্পূর্ণ দিয়েও সাজেস্ট
       করছে না।"*
       🔴 কারণ: তালিকাটা ছাঁকা হত **লগ-ইন করা স্টাফের নিজের ব্রাঞ্চ** ধরে।
       ⇒ এখন **ফর্মে বাছা ব্রাঞ্চ** ধরে চলে। ব্রাঞ্চ না বাছা পর্যন্ত তালিকা ওঠে না —
         "Select branch first" লেখা থাকে (TK-এর নিজের সিদ্ধান্ত — নইলে এক নাম
         পাঁচ ব্রাঞ্চ থেকে পাঁচবার নামত)।
       ⛔ Registration ফর্মে এক অক্ষরও হাত পড়েনি — সেখানে স্টাফ নিজের ব্রাঞ্চেই
         রেজিস্টার করেন (TK-এর লক করা নিয়ম)। */
    private val enqRmpFetched = HashSet<String>()

    /** ফর্মে এখন কোন ব্রাঞ্চ বাছা আছে — না বাছলে ফাঁকা। */
    private fun enqSelectedBranch(): String {
        val b = binding.spBranch.selectedItem?.toString()?.trim().orEmpty()
        return if (b.isBlank() || b.equals("Select Branch", ignoreCase = true)) "" else b
    }

    /** এখন যেটা টাইপ করা আছে, সেটা দিয়েই তালিকাটা আবার আঁকা। */
    private fun refreshEnqRmpSuggest(user: NativeUser) {
        val typed = binding.etEnqRefDoctorName.text?.toString().orEmpty().ifBlank {
            binding.etEnqRefDoctorMobile.text?.toString().orEmpty()
        }
        showEnqRmpSuggest(user, typed)
    }

    private fun wireEnqRmpSuggest(user: NativeUser) {
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                if (!enqRmpMuted) showEnqRmpSuggest(user, s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.etEnqRefDoctorName.addTextChangedListener(watcher)
        binding.etEnqRefDoctorMobile.addTextChangedListener(watcher)
        // 🟢 V1132 — ব্রাঞ্চ বদলালেই তালিকাটা ওই ব্রাঞ্চের হয়ে যায়।
        binding.spBranch.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    if (!enqRmpMuted) refreshEnqRmpSuggest(user)
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
    }

    private fun dpEnq(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** ছোট এক লাইনের বার্তা (তালিকা নয়) — একই ঘরেই বসে। */
    private fun enqRmpNote(text: String) {
        val box = binding.llEnqRmpSuggest
        box.removeAllViews()
        box.visibility = View.VISIBLE
        box.addView(android.widget.TextView(this).apply {
            this.text = text
            textSize = 10.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#94721B"))
            setPadding(dpEnq(10), dpEnq(7), dpEnq(10), dpEnq(7))
        })
    }

    private fun showEnqRmpSuggest(user: NativeUser, typed: String) {
        val box = binding.llEnqRmpSuggest
        val q = typed.trim().lowercase(Locale.US)
        if (q.length < 2) { box.removeAllViews(); box.visibility = View.GONE; return }
        // 🟢 V1132 — ব্রাঞ্চ না বাছলে কোন ব্রাঞ্চের RMP দেখাব সেটাই জানা নেই।
        val scope = enqSelectedBranch()
        if (scope.isBlank()) { enqRmpNote("Select branch first"); return }
        val hits = try { RmpPicker.cachedRmpChoices(this, user, scope) } catch (_: Throwable) { emptyList() }
            .filter { it.searchText().contains(q) }.take(6)
        box.removeAllViews()
        if (hits.isEmpty()) {
            /* 🟢 V1132 — অন্য ব্রাঞ্চের তালিকা এই ফোনে জমা নাও থাকতে পারে।
               তখন **একবারই** ওই ব্রাঞ্চের হালকা তালিকা নামে (৮টা ঘর, ভারী
               callHistory/referralPayments ছাড়া ⇒ কয়েক KB), তারপর ফোনে জমা থাকে —
               পরের বার এক বাইটও খরচ নেই (V802-এর হুবহু একই পথ)। */
            if (enqRmpFetched.add(scope)) {
                enqRmpNote("Loading " + scope + " RMP list\u2026")
                lifecycleScope.launch {
                    val got = withContext(Dispatchers.IO) {
                        RmpDirectory.refreshFromCloud(this@EnquiryActivity, scope)
                    }
                    if (!got) enqRmpFetched.remove(scope)   // নেট না থাকলে পরে আবার চেষ্টা হবে
                    if (!isFinishing && !isDestroyed) refreshEnqRmpSuggest(user)
                }
            } else box.visibility = View.GONE
            return
        }
        box.visibility = View.VISIBLE
        box.addView(android.widget.TextView(this).apply {
            text = if (hits.size == 1) "1 saved RMP found" else "${hits.size} saved RMP found"
            textSize = 10.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#118452"))
            setPadding(dpEnq(10), dpEnq(7), dpEnq(10), dpEnq(5))
        })
        for ((i, item) in hits.withIndex()) {
            if (i > 0) box.addView(View(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#EDF2F8"))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dpEnq(1))
            })
            box.addView(enqRmpRow(item))
        }
    }

    private fun enqRmpRow(item: RmpPicker.RmpChoice): View {
        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpEnq(10), dpEnq(8), dpEnq(10), dpEnq(8))
            isClickable = true
        }
        row.addView(View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#118452"))
            layoutParams = android.widget.LinearLayout.LayoutParams(dpEnq(3), dpEnq(30))
                .apply { marginEnd = dpEnq(9) }
        })
        val texts = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(android.widget.TextView(this).apply {
            text = item.name.trim().uppercase()
            textSize = 13.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#17312A"))
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        val line2 = listOf(item.mobile, item.area.trim().uppercase())
            .filter { it.isNotBlank() }.joinToString(" · ")
        if (line2.isNotBlank()) texts.addView(android.widget.TextView(this).apply {
            text = line2; textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#60766D"))
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        row.addView(texts)
        if (item.branch.isNotBlank()) row.addView(android.widget.TextView(this).apply {
            text = item.branch
            textSize = 10f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#15549B"))
            setPadding(dpEnq(9), dpEnq(3), dpEnq(9), dpEnq(3))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpEnq(12).toFloat()
                setColor(android.graphics.Color.parseColor("#E8F2FF"))
            }
        })
        row.setOnClickListener {
            enqRmpMuted = true
            try {
                binding.etEnqRefDoctorName.setText(item.name.trim().uppercase())
                binding.etEnqRefDoctorMobile.setText(item.mobile)
            } finally { enqRmpMuted = false }
            binding.llEnqRmpSuggest.removeAllViews()
            binding.llEnqRmpSuggest.visibility = View.GONE
        }
        return row
    }

    private fun setupTimingButtons() {
        binding.btnTimingOfficial.setOnClickListener {
            timingIsAuto = false                       // 🕐 V1042 — স্টাফ নিজে বাছলেন
            selectTiming("Official Time")
        }
        binding.btnTimingUnexpected.setOnClickListener {
            timingIsAuto = false                       // 🕐 V1042 — স্টাফ নিজে বাছলেন
            // কল পাওয়া যায়নি ⇒ স্টাফের নিজের বাছাই ⇒ আগে সতর্কতা, তারপর বাছাই
            if (branchCallMs == null) confirmUnexpected() else selectTiming("Unexpected Time")
        }
        applyTimingFromCall(null)
    }

    /** কল-তালিকার ফল অনুযায়ী বোতাম দুটো সাজায়। */
    private fun applyTimingFromCall(callMs: Long?) {
        /* 🔴 V1151 — এডিট-মোডে সারিতে জমা থাকা সময়টাই শেষ কথা; কল-তালিকা দেখে
           সেটা বদলানো হয় না (নইলে পুরনো এনকোয়ারি এডিট করতে গেলেই সময়ের ঘরটা
           আজকের কল অনুযায়ী পাল্টে যেত — টাকার হিসাবেও তার ছাপ পড়ত)। */
        if (editEnquiryId.isNotBlank()) { branchCallMs = callMs; return }
        branchCallMs = callMs
        timingIsAuto = callMs != null                  // 🕐 V1042
        if (callMs == null) {
            binding.btnTimingUnexpected.visibility = View.VISIBLE
            selectTiming("Official Time")
            return
        }
        val fromCall = timingOf(callMs)
        if (fromCall == "Official Time") {
            // অফিসিয়াল সময়ের কল ⇒ Unexpected বাছার সুযোগই থাকবে না
            binding.btnTimingUnexpected.visibility = View.GONE
            selectTiming("Official Time")
        } else {
            binding.btnTimingUnexpected.visibility = View.VISIBLE
            selectTiming("Unexpected Time")
        }
    }

    /** নম্বর লেখা হলে ব্রাঞ্চের কল-তালিকায় প্রথম কলটা খুঁজে বোতাম সাজানো। */
    private fun refreshTimingForMobile(digits: String) {
        lifecycleScope.launch {
            val ms = withContext(Dispatchers.IO) {
                try { BranchSimHelper.firstCallTimeMs(this@EnquiryActivity, digits) }
                catch (_: Throwable) { null }
            }
            if (!isFinishing && !isDestroyed) applyTimingFromCall(ms)
        }
    }

    /* ⚠️🔒 V966 (০২.০৯.২০২৬, TK-এর স্পষ্ট নির্দেশ) — *"এই সতর্কবার্তা লাগবে না,
       শুধুমাত্র ওয়ার্নিং PopUp আসুক — Are You Sure Yes/No"*। তাই V963/V965-এর
       বড় ফ্ল্যাশ বার্তাটা পুরো তুলে দেওয়া হলো; এখন শুধু একটা সাধারণ নিশ্চিতকরণ।
       ⛔ কোথাও কিছু পাঠানো হয় না — Yes চাপলে তবেই Unexpected Time বসে। */
    private fun confirmUnexpected() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "⚠️ Are you sure?"))
                .setMessage("Mark this call as UNEXPECTED TIME?")
                .setPositiveButton("Yes") { _, _ -> selectTiming("Unexpected Time") }
                .setNegativeButton("No") { d, _ -> try { d.dismiss() } catch (_: Throwable) { } }
                .setCancelable(false)
                .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
        } catch (_: Throwable) { selectTiming("Unexpected Time") }
    }

    private fun selectTiming(value: String) {
        selectedTiming = value
        val blue = android.graphics.Color.parseColor("#1167D8")
        val lightBlue = android.graphics.Color.parseColor("#E8F2FF")
        val official = value == "Official Time"
        binding.btnTimingOfficial.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (official) blue else lightBlue)
        binding.btnTimingOfficial.setTextColor(if (official) android.graphics.Color.WHITE else blue)
        binding.btnTimingUnexpected.backgroundTintList =
            android.content.res.ColorStateList.valueOf(if (!official) blue else lightBlue)
        binding.btnTimingUnexpected.setTextColor(if (!official) android.graphics.Color.WHITE else blue)
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()
        DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, d ->
            val cal2 = Calendar.getInstance().apply { set(y, m, d) }
            val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal2.time)
            selectedDate = iso
            binding.tvDate.text = displayDate(iso)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            // Future date not allowed, matching saveEnq()'s check.
            datePicker.maxDate = today.timeInMillis
        }.show()
    }

    private fun pickNextFollow() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, d ->
            val cal2 = Calendar.getInstance().apply { set(y, m, d) }
            val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal2.time)
            selectedNextFollow = iso
            binding.tvNextFollow.text = displayDate(iso)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            // Past next-follow-up date not allowed, matching saveEnq()'s check.
            datePicker.minDate = cal.timeInMillis
        }.show()
    }

    private fun displayDate(iso: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(parsed!!)
    } catch (e: Exception) {
        iso
    }

    private fun validateAndSave(user: NativeUser) {
        hideError()
        val mobile = StaffDirectory.normalizeMobile(binding.etMobile.text.toString())
        val branch = binding.spBranch.selectedItem?.toString() ?: ""
        val name = binding.etName.text.toString().trim()
val disease = selectedDisease
        val address = binding.etAddress.text.toString().trim()
        val remarks = binding.etRemarks.text.toString().trim()
        // ☎️ V963 — বোতামে যা দেখাচ্ছে সেটাই; কল পাওয়া গেলে অ্যাপ আগেই বসিয়ে
        //   রেখেছে, না পেলে স্টাফের বাছাই (Unexpected হলে ফ্ল্যাশ বার্তা দেখানো হয়েছে)।
        val timing = selectedTiming

        // Same validation order as saveEnq(); on failure, jump to that field.
        if (mobile.length != 10) { focusError(binding.etMobile, "Valid mobile number required"); return }
        if (branch.isBlank() || branch == "Select Branch") { focusError(binding.spBranch, "Branch mandatory"); return }
        if (disease.isBlank()) { focusError(binding.btnDisPiles, "Please select a disease"); return }
        if (remarks.isBlank()) { focusError(binding.etRemarks, "Remarks mandatory"); return }
        if (selectedNextFollow.isBlank()) { focusError(binding.tvNextFollow, "Next Follow-up Call date mandatory"); return }

        /* 🛡️🔒 V863 (৩০.০৮.২০২৬, TK-অনুমোদিত) — নম্বরটা আমাদের নিজেদের কারো
           (স্টাফ · ক্লিনিক · ডাক্তার) হলে সেভের আগে একবার সতর্কবার্তা।
           ⛔ আমাদের নম্বর না হলে **এক মুহূর্তও দেরি নয়** — নিচের সবটা হুবহু
              আগের মতোই চলে। ⛔ আটকায় না, শুধু জিজ্ঞেস করে (আসল রোগীও
              স্টাফ/ডাক্তারের নম্বর দিতে পারেন — যাচাই করে দেখা)। */
        OwnNumberGuard.confirmIfOwn(this, mobile) { continueSave(user, mobile, branch, name, disease, address, remarks, timing) }
    }

    private fun continueSave(
        user: NativeUser, mobile: String, branch: String, name: String,
        disease: String, address: String, remarks: String, timing: String
    ) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val duplicate = withContext(Dispatchers.IO) { repository.checkDuplicate(mobile) }
                // 🚨 খাতার সারি B132 (TK প্রুফে পাশ, 29.07.2026 রাত ৮.২০):
                // নম্বরটা আগে Reject/Incomplete করা ছিল কিনা — এটা জানা থাকলে
                // বাক্সে লাল সতর্কবার্তা দেখানো হয়, নইলে বাক্সটা হুবহু আগের মতোই।
                // ⛔ শুধু ডুপ্লিকেট ধরা পড়লেই এই পড়াটা হয়, রোজকার সেভে নয়।
                val closed = if (duplicate.found)
                    withContext(Dispatchers.IO) { repository.closedInfo(mobile) }
                else EnquiryRepository.ClosedInfo(false)
                setLoading(false)
                if (duplicate.found && editEnquiryId.isBlank()) {
                    // 🔴 V1151 — এডিটে "এই নম্বর আগে থেকেই আছে" পপ-আপ ওঠে না;
                    //    সারিটা তো ওই ব্যক্তিরই। ⛔ নতুন এনকোয়ারিতে পাহারা অটুট।
                    showRestoreDialog(user, duplicate, mobile, branch, disease, remarks, closed)
                } else {
                    performSave(user, mobile, branch, name, disease, address, remarks, timing)
                }
            } catch (e: Exception) {
                // CRASH-SAFETY FIX (TK-reported, 2026-07-16): this used to have
                // no error handling -- a problem during the duplicate-check
                // network call could crash the whole app instead of just
                // failing this one save attempt.
                setLoading(false)
                android.widget.Toast.makeText(this@EnquiryActivity, "Could not check — check connection and try again", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 🔴 V431 — ভাগের নাম ব্যবহারকারীর ভাষায় (ভিতরের কাঁচা নাম নয়)। */
    private fun dupSectionLabel(stage: String): String = when (stage.trim().lowercase()) {
        "inquiry", "enquiry" -> "Enquiry"
        "patient" -> "Visit"
        "treatment" -> "Patient"
        else -> stage.ifBlank { "-" }
    }

    /** Immediate popup shown the moment a full 10-digit number that already
     *  exists is entered: shows which branch / section it is in, with a View
     *  button (opens that patient's timeline) and Cancel (clears the field). */
    private fun showDuplicatePopup(dup: EnquiryRepository.DuplicateResult, digits: String) {
        val view = layoutInflater.inflate(com.tkbiswas.pilesclinic.R.layout.dialog_duplicate, null)
        val tvDupName = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupName)
        val tvDupMobile = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupMobile)
        tvDupName.text = dup.name.ifBlank { "-" }
        tvDupMobile.text = digits
        tvDupName.copyOnLongPress("Name", dup.name)
        tvDupMobile.copyOnLongPress("Mobile", digits)
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupBranch).text = dup.branch.ifBlank { "-" }
        // 🔴 V431 (TK-রিপোর্ট ১৮.০৮.২০২৬, ছবিসহ — "বানান ঠিক আছে কি?"):
        //    এখানে **ভিতরের নামটাই** কাঁচা দেখানো হচ্ছিল ("Inquiry"), অথচ
        //    ব্যবহারকারী সারা অ্যাপে ওই ভাগটাকে **"Enquiry"** নামেই চেনেন
        //    (এই পর্দারই নাম "New Enquiry", ফলো-আপের ট্যাব "👥 Enquiry",
        //    Draft-এ "Enquiry Reject")। তাই অ্যাপের নিজের নিয়মেই বদলে
        //    দেখানো হলো: Inquiry → Enquiry · Patient → Visit ·
        //    Treatment → Patient (DialerActivity.kt:431-435-এর একই নিয়ম)।
        //    ⛔ ডেটাবেসে `stage` আগের মতোই "Inquiry" থাকে — শুধু দেখানোর লেখা।
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupSection).text =
            dupSectionLabel(dup.stage)

        UppercaseInputUtil.applyToAll(view)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupView).setOnClickListener {
            // 🔴 TK-REPORTED (04.08.2026, একই ক্লাসের বাগ): dialog.dismiss()
            // ডাকা হত, তাই Timeline থেকে Back করলে এই পপ-আপ আর দেখা যেত না।
            val intent = Intent(this, PatientTimelineActivity::class.java)
            intent.putExtra("mobile", digits)
            startActivity(intent)
        }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
        // 🔒 খাতার সারি B181 (TK, 30.07.2026): এই পপ-আপে এখন সরাসরি বাংলা লেখা
        // পাওয়া যায়নি, তবু ধারাবাহিকতার জন্য পাহারা বসানো হলো (ভবিষ্যতে কেউ
        // বাংলা যোগ করলেও যেন আটকায়)।
        PremiumAlert.paint(dialog)
    }

    /**
     * TK APPROVED (2026-07-27, full-screen photo proof, and LOCKED by TK):
     * the "this number already exists" popup used to be a plain message box --
     * TK's words: "সাদা মিঠা লাগছে... উপরে যে লেখার ধরন আমার পছন্দ হচ্ছে না"।
     *
     * The look is now: one clean header line, then Name / Branch / Section on
     * three separate lines (Section shows the same word the rest of the app
     * uses -- Enquiry / Visit / Patient -- not the internal one), the
     * explanation inside a soft note box, then a wide "Restore & Move" button
     * with "পুরনো History" and "Cancel" side by side under it.
     *
     * ⛔ NOTHING ABOUT THE BEHAVIOUR CHANGED: the same three actions do exactly
     * the same thing as before (restore-and-move / open the old timeline /
     * close). No new record is ever created here.
     */
    private fun showRestoreDialog(
        user: NativeUser, dup: EnquiryRepository.DuplicateResult,
        mobile: String, branch: String, disease: String, remarks: String,
        // 🔒 খাতার সারি B132 — রেকর্ডটা আগে Reject/Incomplete করা ছিল কিনা।
        //    ফাঁকা (closed = false) হলে বাক্সটা হুবহু আগের মতোই দেখায়।
        closed: EnquiryRepository.ClosedInfo = EnquiryRepository.ClosedInfo(false)
    ) {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val sectionLabel = when (dup.stage) {
            "Treatment" -> "Patient"
            "Patient" -> "Visit"
            "" -> "-"
            else -> "Enquiry"
        }

        val body = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(18))
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        fun infoRow(label: String, value: String, withDivider: Boolean) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            row.addView(android.widget.TextView(this).apply {
                text = label; textSize = 13.5f
                setTextColor(android.graphics.Color.parseColor("#6B7280"))
                layoutParams = android.widget.LinearLayout.LayoutParams(dp(96), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            row.addView(android.widget.TextView(this).apply {
                text = value.ifBlank { "-" }; textSize = 15.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#1F2937"))
            })
            row.setPadding(0, dp(8), 0, dp(8))
            body.addView(row)
            if (withDivider) {
                body.addView(android.view.View(this).apply {
                    setBackgroundColor(android.graphics.Color.parseColor("#EEF0F4"))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                    )
                })
            }
        }
        infoRow("Name", dup.name, true)
        infoRow("Branch", dup.branch, true)
        infoRow("Section", sectionLabel, false)

        // 🚨🚨 TK-APPROVED (ফটো-প্রুফ, 29.07.2026 রাত ৮.২০ · খাতার সারি B132):
        // TK: *"সমস্ত staff-কে জিজ্ঞাসা করলাম, কেউ Restore করেনি"* — অথচ Reject
        // করা নম্বর চালু তালিকায় ফিরে আসত। কারণ এই বাক্সটা কোথাও বলত না যে
        // রেকর্ডটা **আগে Reject/Incomplete করা হয়েছিল**, তাই স্টাফ `Restore &
        // Move`-কে "নতুন এনকোয়ারি সেভ করলাম" ভেবে চাপতেন।
        //
        // ⛔ **শুধু এই লাল বাক্সটাই নতুন** — বাকি সব লেখা · বোতাম · রং · ক্রম
        //    হুবহু আগের মতোই, একটাও বদলায়নি (TK প্রুফে পাশ করেছেন)।
        // ⛔ রেকর্ডটা বন্ধ না থাকলে (স্বাভাবিক ডুপ্লিকেট) এটা **দেখানোই হয় না**।
        // ⛔ কোনো বোতাম আটকানো হয়নি — স্টাফ জেনেবুঝে চাপলে আগের মতোই কাজ হবে।
        if (closed.closed) {
            val warn = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(android.graphics.Color.parseColor("#FDECEC"))
                    setStroke(dp(2), android.graphics.Color.parseColor("#D32F2F"))
                }
                setPadding(dp(14), dp(12), dp(14), dp(12))
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(14)
                layoutParams = lp
            }
            warn.addView(android.widget.TextView(this).apply {
                text = "\uD83D\uDEAB  THIS NUMBER WAS " + closed.what
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#B3261E"))
            })
            // কে ও কবে — যেটুকু সত্যিই জানা আছে শুধু সেটুকুই লেখা হয়,
            // কোনো কিছু আন্দাজে বসানো হয় না (TK-এর স্থায়ী নিয়ম)।
            val whoWhen = buildString {
                if (closed.whenText.isNotBlank()) append("On ").append(closed.whenText)
                if (closed.byName.isNotBlank()) {
                    if (isNotEmpty()) append("  \u00B7  ")
                    append("by ").append(closed.byName)
                }
            }
            if (whoWhen.isNotBlank()) {
                warn.addView(android.widget.TextView(this).apply {
                    text = whoWhen
                    textSize = 12.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#7A1F1F"))
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.topMargin = dp(6)
                    layoutParams = lp
                })
            }
            warn.addView(android.widget.TextView(this).apply {
                text = "It is now in " + closed.listName + ".\n" +
                    "Restore & Move will bring it back to the live list."
                textSize = 12.5f
                setTextColor(android.graphics.Color.parseColor("#7A1F1F"))
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(6)
                layoutParams = lp
            })
            body.addView(warn)
        }

        body.addView(android.widget.TextView(this).apply {
            text = NoBengali.s("নতুন রেকর্ড হবে না — পুরনো রেকর্ডটাই Restore হয়ে '$branch' ব্রাঞ্চে আসবে। " +
                "যে সেকশনে ছিল সেখানেই থাকবে, পুরনো history অটুট।")
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#7A5A0F"))
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_dup_note)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14)
            layoutParams = lp
        })

        val btnRestore = android.widget.TextView(this).apply {
            text = "Restore & Move"; textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_dup_amber_pill)
            setPadding(dp(10), dp(14), dp(10), dp(14))
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(18)
            layoutParams = lp
        }
        body.addView(btnRestore)

        val pair = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }
        fun sideButton(label: String, marginEnd: Int): android.widget.TextView =
            android.widget.TextView(this).apply {
                text = label; textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#1F2937"))
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_dup_outline_pill)
                setPadding(dp(6), dp(12), dp(6), dp(12))
                val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginEnd = marginEnd
                layoutParams = lp
            }
        val btnHistory = sideButton("Old History", dp(8))
        val btnCancel = sideButton("Cancel", 0)
        pair.addView(btnHistory)
        pair.addView(btnCancel)
        body.addView(pair)

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "⚠️ This number already exists"))
            .setView(body)
            .setCancelable(true)
            .create()

        btnRestore.setOnClickListener {
            dialog.dismiss()
            setLoading(true)
            lifecycleScope.launch {
                /* 🔵🔒 V534 (২২.০৮.২০২৬, TK-নির্দেশ) — এক নম্বরে সত্যিই একাধিক
                   আলাদা রোগী থাকলে **কার Restore** সেটা জিজ্ঞাসা করা হয়, নইলে
                   অন্যজনের রোগ/ব্রাঞ্চও বদলে যেত।
                   ⛔ একজন থাকলে (রোজকার ৯৯%) কিছুই জিজ্ঞাসা করা হয় না এবং
                      নিচের ডাকটা **হুবহু আগের মতোই** (দুটো ঘর ফাঁকা)। */
                var pickCode = ""
                var pickRowId = ""
                val people = withContext(Dispatchers.IO) {
                    try {
                        val rows = SupabaseClient.findByMobile(
                            "patients", EnquiryModel.normalizedMobile(mobile),
                            "id,name,mobile,branch,patientId,bill", 20
                        )
                        PatientIdentity.separateIdentities(rows, mobile)
                    } catch (_: Throwable) { emptyList() }
                }
                if (people.size >= 2) {
                    val chosen = askWhichRestorePatient(mobile, people)
                    if (chosen == null) { setLoading(false); return@launch }
                    pickRowId = chosen.s("id")
                    pickCode = chosen.s("patientId")
                }
                val ok = withContext(Dispatchers.IO) {
                    repository.restoreAndMove(
                        mobile, branch, disease, remarks, selectedNextFollow, user.name,
                        preferPatientCode = pickCode, preferRowId = pickRowId
                    )
                }
                setLoading(false)
                toastAndFinish(if (ok) "Restored into the '$branch' branch" else "Restore failed — check the network")
            }
        }
        // TK-REQUESTED ADDITION (2026-07-23), unchanged: staff can look at the
        // old full history before deciding. Opens the same PatientTimelineActivity
        // used everywhere else; the form data is untouched.
        // 🔴 TK-REPORTED (04.08.2026, একই ক্লাসের বাগ): আগে dialog.dismiss()
        // করে দেওয়া হত, তাই Timeline থেকে Back করলে এই ডুপ্লিকেট-পপ-আপ আর
        // দেখা যেত না — সরাসরি ফাঁকা ফর্মে থাকতে হত। এখন পপ-আপ খোলা রাখা হয়।
        btnHistory.setOnClickListener {
            startActivity(Intent(this, PatientTimelineActivity::class.java).putExtra("mobile", mobile))
        }
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
        PremiumAlert.paint(dialog)
    }

    private fun showDuplicateDialog(
        duplicate: EnquiryRepository.DuplicateResult,
        user: NativeUser, mobile: String, branch: String, name: String,
        disease: String, address: String, remarks: String, timing: String
    ) {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "⚠️ This number is already in the system"))
            .setMessage("Name: ${duplicate.name}\nBranch: ${duplicate.branch}\nStatus: ${duplicate.stage}")
            .setPositiveButton("Save Anyway") { _, _ ->
                performSave(user, mobile, branch, name, disease, address, remarks, timing)
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    private fun performSave(
        user: NativeUser, mobile: String, branch: String, name: String,
        disease: String, address: String, remarks: String, timing: String
    ) {
        val draft = EnquiryDraft(
            date = selectedDate,
            branch = branch,
            name = name,
            mobileDigitsOnly = mobile,
            disease = disease,
            address = address,
            remarks = remarks,
            nextFollow = selectedNextFollow,
            timeType = timing,
            timeSource = if (timingIsAuto) "auto" else "hand",   // 🕐 V1042
            // 🩺 V1070 — কে পাঠিয়েছেন (Dr. Visit ছাড়া নাম-নম্বর ফাঁকাই যায়)
            refBy = binding.spEnqRefBy.selectedItem?.toString() ?: "Self",
            refDoctor = if (binding.spEnqRefBy.selectedItem?.toString() == "Dr. Visit")
                binding.etEnqRefDoctorName.text.toString().trim() else "",
            refDoctorMobile = if (binding.spEnqRefBy.selectedItem?.toString() == "Dr. Visit")
                binding.etEnqRefDoctorMobile.text.toString().filter { it.isDigit() }.takeLast(10) else "",
            receivedByMobile = receivedByMobiles.getOrNull(binding.spReceivedBy.selectedItemPosition) ?: user.mobile
        )
        setLoading(true)
        /* ═══════════════════════════════════════════════════════════════════
           🔴🔒 V1151 — **এডিট-মোড: নতুন সারি তৈরি হয় না।** পুরনো সারিটাতেই
           ফর্মের ঘরগুলো বসে, আর ওই ব্যক্তির Inquiry-ধাপের ফলো-আপ সারিতেও
           দেখানোর ঘরগুলো মিলিয়ে দেওয়া হয় (ওয়েবের হুবহু একই নিয়ম)।
           ⛔ `id` · `createdAt` · `createdBy` · `stage` · `status` ·
              `callCount` · `history` · `lastCallDate` — একটাও ছোঁয়া হয় না। */
        if (editEnquiryId.isNotBlank()) {
            val editId = editEnquiryId
            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val f = org.json.JSONObject()
                            .put("date", draft.date).put("branch", draft.branch).put("name", draft.name)
                            .put("mobile", draft.mobileDigitsOnly).put("disease", draft.disease)
                            .put("address", draft.address).put("remarks", draft.remarks)
                            .put("nextFollow", draft.nextFollow).put("timeType", draft.timeType)
                            .put("refBy", draft.refBy).put("refDoctor", draft.refDoctor)
                            .put("refDoctorMobile", draft.refDoctorMobile)
                            .put("receivedBy", draft.receivedByMobile)
                            .put("updatedAt", EnquiryModel.isoNow())
                        val main = SupabaseClient.updateById("enquiries", editId, f)
                        if (main) {
                            // ফলো-আপের দেখানোর ঘরগুলো মেলানো — শুধু Inquiry ধাপে।
                            try {
                                val rows = SupabaseClient.findByMobile(
                                    "followups", draft.mobileDigitsOnly, "id,stage", 20)
                                for (i in 0 until rows.length()) {
                                    val r = rows.optJSONObject(i) ?: continue
                                    if (!r.optString("stage", "").equals("Inquiry", ignoreCase = true)) continue
                                    val fid = r.optString("id", "")
                                    if (fid.isBlank()) continue
                                    SupabaseClient.updateById("followups", fid, org.json.JSONObject()
                                        .put("name", draft.name).put("branch", draft.branch)
                                        .put("disease", draft.disease).put("address", draft.address)
                                        .put("nextFollow", draft.nextFollow).put("timeType", draft.timeType)
                                        .put("lastRemark", draft.remarks)
                                        .put("updatedAt", EnquiryModel.isoNow()))
                                }
                            } catch (_: Throwable) { }
                        }
                        main
                    } catch (_: Throwable) { false }
                }
                setLoading(false)
                android.widget.Toast.makeText(
                    this@EnquiryActivity,
                    if (ok) "Enquiry updated" else "Could not save — check connection and try again",
                    android.widget.Toast.LENGTH_LONG
                ).show().also { try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }
                if (ok) finish()
            }
            return
        }
        lifecycleScope.launch {
            try {
                val savedOnline = withContext(Dispatchers.IO) {
                    repository.save(draft, user.mobile, user.name)
                }
                setLoading(false)
                // 🔒 TK-APPROVED (28.07.2026): এনকোয়ারি নেওয়ার পরে রোগীকে
                // ক্লিনিকের খবর পাঠানোর বাক্স। "পরে পাঠাব" চাপলেও পর্দা আগের
                // মতোই বন্ধ হয়, তাই স্টাফের চলতি কাজ কখনো আটকায় না।
                android.widget.Toast.makeText(
                    this@EnquiryActivity,
                    if (savedOnline) "Enquiry saved" else "Saved — will sync when online",
                    android.widget.Toast.LENGTH_LONG
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                // 🔒 ENQUIRY_WHATSAPP_MESSAGE_FINAL_LOCK_2026-07-31_0950_IST —
                // পুরনো তিন-ভাষা-একসাথে ENQUIRY বার্তার বদলে এখন আগে ভাষা
                // বাছাই (বাংলা/হিন্দি/English), তারপর সেই ভাষার লকড টেমপ্লেট।
                // 🔴🟢 খাতার সারি B430 (TK-নির্দেশ, 05.08.2026 — "সেভ করার পর
                // ফর্ম বন্ধ না হয়ে, একই পর্দায় ঘরগুলো খালি হয়ে যাক, যাতে
                // পরের রোগী সাথে সাথে লেখা যায়")। আগে এখানে `finish()` —
                // পর্দা বন্ধ হয়ে Dashboard-এ ফিরে যেত। এখন `clearForm()` —
                // পর্দা খোলাই থাকে, শুধু সব ঘর খালি/ডিফল্ট হয়ে যায়। ⛔
                // সেভ/ডুপ্লিকেট-চেক/বার্তা-পাঠানোর লজিক এক অক্ষরও বদলায়নি।
                PatientMessage.showEnquiryMessage(
                    activity = this@EnquiryActivity,
                    branch = draft.branch,
                    name = draft.name,
                    mobile = draft.mobileDigitsOnly,
                    disease = draft.disease
                ) { clearForm() }
            } catch (e: Exception) {
                // CRASH-SAFETY FIX (TK-reported, 2026-07-16): same fix as
                // validateAndSave above.
                setLoading(false)
                android.widget.Toast.makeText(this@EnquiryActivity, "Could not save — check connection and try again", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toastAndFinish(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }

    // 🔴🟢 খাতার সারি B430 — Save-এর পরে ফর্ম খালি করার ফাংশন। প্রতিটা ঘর/
    // বোতাম ঠিক সেই ডিফল্ট অবস্থায় ফেরে যা `onCreate()`-এ প্রথমবার থাকে।
    // ⛔ "Call Received By" স্পিনার (কে কল রিসিভ করছেন) ইচ্ছাকৃতভাবে ছোঁয়া
    // হয়নি — এটা রোগীর তথ্য না, পুরো সেশন জুড়ে একই স্টাফ থাকেন।
    private fun clearForm() {
        binding.etMobile.setText("")
        binding.etName.setText("")
        binding.etAddress.setText("")
        binding.etRemarks.setText("")
        binding.tvDupWarning.visibility = View.GONE
        lastDupChecked = ""
        binding.spBranch.setSelection(0)
        selectedDate = EnquiryModel.today()
        binding.tvDate.text = displayDate(selectedDate)
        selectedNextFollow = ""
        binding.tvNextFollow.text = "Tap to select (optional)"
        selectedDiseaseSet.clear()
        setupDiseaseButtons()
        setupTimingButtons()
        binding.etMobile.requestFocus()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressSave.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
        binding.btnSave.alpha = if (loading) 0.6f else 1f
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    /** Shows the error and scrolls/focuses to the first empty mandatory field. */
    private fun focusError(target: View, msg: String) {
        showError(msg)
        FieldError.mark(target)
        binding.scrollForm.post {
            var top = 0
            var v: View = target
            while (v.parent is View && v.parent !== binding.scrollForm) {
                top += v.top
                v = v.parent as View
            }
            binding.scrollForm.smoothScrollTo(0, (top - 40).coerceAtLeast(0))
            target.requestFocus()
        }
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
}
