package com.tkbiswas.pilesclinic.native

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.databinding.ActivityRegistrationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Native rebuild Step 3 -- Registration.
 *
 * Same fields, same validation rules (see savePatient() in app.js), and the
 * same Patient ID format (branch code + date code + serial) as the WebView.
 * A saved patient is fully interchangeable with a WebView-saved one -- same
 * Visit tab, same Patient ID scheme, same Visit Fee payment record.
 *
 * SCOPED LIMITATIONS for this step (see RegistrationRepository's own
 * comment for the duplicate-patient one):
 * - Patient Photo capture is not in this screen yet -- registering here
 *   creates the patient with no photo; it can still be added later from the
 *   WebView's existing photo-upload screens, nothing is blocked.
 * - The "convert this Enquiry to a Registration" pre-fill flow (arriving
 *   here from an existing Enquiry with its details already filled in) isn't
 *   wired yet -- for now, staff re-type the details, matching a fresh
 *   walk-in registration. This will connect naturally once the Enquiry and
 *   Registration native screens are linked together.
 */
class RegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var repository: RegistrationRepository

    private val branches = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")

    // TK RULE (2026-07-27): Master and Field Officer must CHOOSE the branch, so
    // their list starts on this placeholder and nothing is picked for them.
    // Staff and Doctor keep their own branch filled in automatically and locked.
    private val SELECT_BRANCH = "Select Branch"
    private var branchItems: List<String> = branches
    private val sexes = listOf("Male", "Female", "Other")
    private val occupations = listOf("Choose Occupation", "Farmer", "Housewife", "Business", "Service", "Student", "Labour", "Retired", "Others")
    private val durationUnits = listOf("Days", "Months", "Years")
    private val refByOptions = listOf("Self", "Online", "Offline", "Dr. Visit", "Old Patient", "Others")
    private val diseaseOptions = listOf("Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog", "Other")
    private val symptomOptions = listOf("Pain", "Itching", "Burning", "Bleeding", "Pus Discharge", "Fluid Discharge", "Massa Bara Hua")
    private val medHistOptions = listOf("Previous Medication", "Previous Doctor Treatment", "Previous Operation History", "Previous Ayurvedic/Herbal Treatment")
    private val payModes = listOf("CASH", "ONLINE")

    /* 🔴🔒 V1143 (০৬.০৯.২০২৬, TK-নির্দেশ) — আর আপনা-আপনি আজকের তারিখ বসে না।
       স্টাফকে নিজে তারিখ বাছতেই হবে, নইলে সেভ আটকে যায় (নিচে validateAndSave)।
       নিয়মটা বদলানোর একটাই জায়গা: `RegDateRule.MUST_PICK` — ওটা `false` করলেই
       আগের মতো আজকের তারিখ নিজে থেকে বসে যাবে, আর কিছু বদলাতে হবে না। */
    private var selectedDate: String = if (RegDateRule.MUST_PICK) "" else PatientIdGenerator.todayIso()
    private val diseaseChecks = mutableListOf<com.google.android.material.chip.Chip>()
    private val symptomChecks = mutableListOf<com.google.android.material.chip.Chip>()
    private val medHistChecks = mutableListOf<com.google.android.material.chip.Chip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FormStar.paint(binding.root)  // red '*' on mandatory labels (visual only)
        UppercaseInputUtil.applyToAll(binding.root)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)
        repository = RegistrationRepository(this)
        MobileInput.attach(binding.etMobile)
        MobileInput.attach(binding.etAltMobile)   // 🔒 V235: Alternate/Enquiry Mobile
        setupEnquiryAutofill()

        val user = NativeSession.current(this)
        if (user == null) { finish(); return }

        // setupSpinners also attaches the pickers, including the 3-tap Branch
        // lock for Staff and Doctor (TK RULE 2026-07-27, unchanged).
        setupSpinners(user)
        setupSexButtons()
        setupPayButtons()
        setupTimingButtons()
        setupCheckboxGroups()
        showRegDate()
        binding.tvDate.setOnClickListener { pickDate() }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { validateAndSave(user) }
        binding.btnPatientPhoto.setOnClickListener { showPhotoSourceDialog() }
        /* 🎨🔒 V829 (২৯.০৮.২০২৬, TK-অনুমোদিত ফটো-প্রুফ: *"হ্যাঁ করুন, তবে সাবধানে"*)
           — অ্যাপের থিমে XML-এর সাদামাটা `<Button>` আপনা-আপনি **MaterialButton**
           হয়ে যায়, আর সেটা `android:background` **অগ্রাহ্য করে** নিজের গাঢ় নীল
           `backgroundTint` বসিয়ে দেয়। ফলে XML-এ লেখা রংটা ফোনে কখনো দেখা যেত না
           (কম্পিউটারে ঠিকই দেখা যেত)। `backgroundTintList = null` বসালে তবেই
           XML-এর drawable-টা দেখা যায় — প্রজেক্টের নিজেরই প্রমাণিত ওষুধ
           (`DoctorQueueAdapter` · `DraftCardAdapter`-এ আগে থেকেই চলছে, পাহারা ৯.৩২)।
           ⛔ শুধু চেহারা — বোতামের কাজ · জায়গা · লেখা কিচ্ছু বদলায়নি। */
        /* 🟢🔒 V895 (৩১.০৮.২০২৬, TK ডেমো ফটো দেখে **"হ্যাঁ পাশ, বসিয়ে দিন"**) —
           সবুজ "Select Saved RMP / Doctor" বোতামটা তুলে দেওয়া হলো; এখন নামের
           ঘরে **নাম · এলাকা** বা মোবাইলের ঘরে **নম্বর** লিখতে শুরু করলেই নিচে
           সেভ করা RMP-র তালিকা নামে (TK-এর নির্দেশ)।
           ⛔ তালিকার উৎস · ছাঁকনি · বেছে নিলে যা বসে — সবই আগের পপ-আপের হুবহু
              একই কোড থেকে (`cachedRmpChoices` · `searchText`), নতুন কিছু নয়। */
        wireRmpSuggest(user)

        lifecycleScope.launch(Dispatchers.IO) { repository.flushPending() }

        // Draft restore / deep-link: pre-fill the mobile so enquiry autofill runs.
        intent.getStringExtra("prefillMobile")?.let { pm ->
            val d = pm.filter { it.isDigit() }.takeLast(10)
            // 🔒 V235: Enquiry থেকে খোলা হলে এই নম্বরই "Enquiry-র মূল নম্বর"।
            // Primary-তে বসে (আগের মতোই); পরে Primary বদলালে এটি Alternate-এ যাবে।
            if (d.length == 10) { enquiryOriginMobile = d; binding.etMobile.setText(d) }
        }
    }

    /**
     * Owner-approved RMP selector for Registration.
     *
     * IMPORTANT FREE-PLAN GUARANTEE: this function never calls Supabase. It
     * reads the exact SharedPreferences cache that DoctorVisitActivity already
     * maintains on this phone, then searches that small in-memory list. The
     * existing manual name/mobile fields remain untouched as the fallback.
     */
    private data class RmpChoice(
        val id: String,
        val name: String,
        val mobile: String,
        val altMobiles: String,
        val area: String,
        val branch: String
    ) {
        fun searchText(): String = "$name $mobile $altMobiles $area $branch".lowercase(Locale.US)
        fun label(): String {
            val line2 = listOf(mobile, area, branch).filter { it.isNotBlank() }.joinToString("  ·  ")
            return if (line2.isBlank()) name else "$name\n$line2"
        }
    }

    /** TK-REPORTED (16.09.2026): এই ফাংশন `user.branch` (লগ-ইন করা স্টাফের
     *  নিজের ব্রাঞ্চ) দিয়ে ছাঁকত, ফর্মে তখন **আসলে কোন ব্রাঞ্চ বাছা আছে** তা
     *  দেখতই না। Staff/Doctor-এর ব্রাঞ্চ ফর্মেও লক (V1110) বলে ওদের ক্ষেত্রে
     *  দুটো এক থাকায় ভুল ধরা পড়েনি, কিন্তু Master/Field Officer প্রতিবার নিজে
     *  ব্রাঞ্চ বাছেন (কখনো প্রি-ফিল হয় না) — তাঁদের জন্য "All"-এর ক্যাশে
     *  সবকটা ব্রাঞ্চ একসাথে মিশে যেত, ফর্মে যে ব্রাঞ্চই বাছা হোক না কেন।
     *  এখন Enquiry-র `RmpPicker.cachedRmpChoices`-এর (V1132) একই নিয়মে ফর্মের
     *  নিজের বাছা ব্রাঞ্চ (`branchScope`) দিয়ে ছাঁকা হয়। */
    private fun cachedRmpChoices(user: NativeUser, branchScope: String): List<RmpChoice> {
        val scope = branchScope.trim().ifBlank { user.branch }
        return try {
            val prefs = getSharedPreferences("doctor_visit_cache", android.content.Context.MODE_PRIVATE)
            // Staff/Doctor only ever need their own branch cache. Master may
            // have opened either All or an individual branch, so all existing
            // cache buckets are safely combined without any network request.
            val keys = if (scope.equals("All", ignoreCase = true)) {
                listOf("All") + branches
            } else listOf(scope)
            val combined = org.json.JSONArray()
            for (branch in keys.distinct()) {
                val raw = prefs.getString("cache_${branch.ifBlank { "All" }}", null) ?: continue
                val arr = try { org.json.JSONArray(raw) } catch (_: Throwable) { continue }
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { combined.put(it) }
            }
            // 🟢🔒 V802 — Doctor Visit পর্দার জমানো ঘরের সঙ্গে RMP-বাছার নিজস্ব
            // হালকা ঘরটাও যোগ (নিচের `seen` ইতিমধ্যেই ডুপ্লিকেট বাদ দেয়)।
            run {
                val extra = RmpDirectory.cachedRows(this, scope)
                for (i in 0 until extra.length()) extra.optJSONObject(i)?.let { combined.put(it) }
            }
            // Include a doctor/RMP just added on this phone even if its cloud
            // copy has not yet appeared in the saved cache.
            val rows = MyPhoneWrites.overlay(this, "doctor_visits", combined)
            val seen = HashSet<String>()
            val out = ArrayList<RmpChoice>()
            for (i in 0 until rows.length()) {
                val row = rows.optJSONObject(i) ?: continue
                val id = row.optString("id").trim()
                val name = row.optString("name").trim()
                val mobile = row.optString("mobile").filter { it.isDigit() }.takeLast(10)
                val branch = row.optString("branch").trim()
                val status = row.optString("status").trim()
                if (name.isBlank()) continue
                if (status.isNotBlank() && !status.equals("Active", ignoreCase = true)) continue
                if (!scope.equals("All", ignoreCase = true) &&
                    branch.isNotBlank() && !branch.equals(scope, ignoreCase = true)) continue
                if (id.isNotBlank() && try { DeletedGuard.isDeleted("doctor_visits", id, this) } catch (_: Throwable) { false }) continue
                val unique = id.ifBlank { "$mobile|${name.lowercase(Locale.US)}" }
                if (!seen.add(unique)) continue
                out.add(RmpChoice(
                    id = id,
                    name = name,
                    mobile = mobile,
                    altMobiles = row.optString("altMobiles"),
                    area = row.optString("area").trim(),
                    branch = branch
                ))
            }
            out.sortedBy { it.name.lowercase(Locale.US) }
        } catch (_: Throwable) { emptyList() }
    }

    /** ফর্মে এই মুহূর্তে আসলে কোন ব্রাঞ্চ বাছা আছে (SELECT BRANCH হলে ফাঁকা) —
     *  Enquiry-র `enqSelectedBranch()`-এর হুবহু একই ধরনের সাহায্যকারী। */
    private fun regSelectedBranch(): String =
        binding.spBranch.selectedItem?.toString()?.takeIf { it != SELECT_BRANCH }.orEmpty()

    // ---------- 🟢🔒 V895: টাইপ করতে করতে RMP সাজেশন ----------

    /** ফোনে জমা থাকা তালিকা — একবারই পড়া হয়, তারপর মনে থাকে। */
    private var rmpSuggestCache: List<RmpChoice>? = null
    /** TK-REPORTED (16.09.2026) ফিক্সের অংশ — এই ক্যাশ কোন ব্রাঞ্চের জন্য
     *  বানানো হয়েছিল সেটা মনে রাখা, যাতে Master/Field Officer ফর্মে ব্রাঞ্চ
     *  বদলালে পুরনো ব্রাঞ্চের তালিকা ভুল করে থেকে না যায়। */
    private var rmpSuggestCacheScope: String? = null
    /** ঘরে নিজেরা লেখা বসানোর সময় যেন সাজেশন আবার না খোলে। */
    private var rmpSuggestMuted = false
    /** ফাঁকা তালিকা হলে একবারই হালকা করে নামানো (V802-এর হুবহু নিয়ম)। */
    private var rmpSuggestFetchTried = false

    /* 🔴🔒 V1397 (১২.০৯.২০২৬, TK-অনুমোদিত ডেমো: "হ্যাঁ, ঠিক আছে, কোড শুরু
       করুন") — PKB-র ঘটনার (নাম "Dr. PKB (birpara)" লিখলে সেভ করা "PKB"
       RMP-টা কখনো সাজেশনে আসত না, তাই কমিশন লিংক হতনি) পর TK-র তিনটে নতুন
       নিয়ম: ① মোবাইল নম্বর মিললে সেটাই সবচেয়ে বিশ্বাসযোগ্য — নাম যাই লেখা
       থাকুক, সেভ করা আসল নাম আগে দেখানো ② নামের সাজেশন এখন দুই-দিকেই মেলে
       (আগে শুধু "সেভ করা নাম টাইপ করা লেখার ভেতরে আছে কিনা" দেখত না, শুধু
       উল্টোটা — তাই "PKB" সেভ থাকলেও "Dr. PKB (birpara)" লিখলে ধরা পড়ত না)
       ③ কোনো মিলই না পেলে নাম-ঘর থেকে সরে গেলে নতুন RMP হিসেবে সেভ করার
       সাজেশন — "না" দিলে বা "হ্যাঁ" দিলে দুটোতেই রেজিস্ট্রেশন থামে না।
       ⛔ পুরনো আচরণ (মিল না থাকলে) অক্ষত — শুধু নতুন সুযোগ যোগ হলো। */
    private var rmpNewSuggestAskedFor = ""

    private fun rmpChoiceMatches(choice: RmpChoice, q: String, mobileDigits: String): Boolean {
        if (mobileDigits.length == 10 && choice.mobile == mobileDigits) return true
        if (q.length < 2) return false
        if (choice.searchText().contains(q)) return true
        val nm = choice.name.trim().lowercase(Locale.US)
        return nm.length >= 2 && q.contains(nm)
    }

    private fun wireRmpSuggest(user: NativeUser) {
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                if (!rmpSuggestMuted) showRmpSuggest(user, s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.etRefDoctorName.addTextChangedListener(watcher)
        binding.etRefDoctorMobile.addTextChangedListener(watcher)
        // 🔴🔒 V1397 — নাম-ঘর থেকে সরে গেলে (ফোকাস হারালে) মিল না পেলে
        // নতুন RMP সেভের সাজেশন। ঘর ফাঁকা/মিল-থাকা অবস্থায় কিছুই দেখায় না।
        binding.etRefDoctorName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) maybeOfferNewRmp(user)
        }
    }

    private fun showRmpSuggest(user: NativeUser, typed: String) {
        val boxView = binding.llRmpSuggest
        val scope = regSelectedBranch()
        var all = rmpSuggestCache
        if (all == null || rmpSuggestCacheScope != scope) {
            all = cachedRmpChoices(user, scope)
            rmpSuggestCache = all
            rmpSuggestCacheScope = scope
            rmpSuggestFetchTried = false
        }
        /* ফোনে তালিকা না থাকলে (এই ফোনে Doctor Visit পর্দা কখনো খোলা হয়নি)
           একবারই হালকা করে নামানো — আগের বোতামেও ঠিক এই ব্যবস্থাই ছিল (V802)। */
        if (all.isEmpty() && !rmpSuggestFetchTried &&
            !RmpDirectory.hasDoctorVisitCache(this, scope) &&
            RmpDirectory.cachedRows(this, scope).length() == 0) {
            rmpSuggestFetchTried = true
            Thread {
                /* ⚠️ Activity-context-ই যথেষ্ট (SharedPreferences একই ফাইল) —
                   উপরের পুরোনো পথটাও ঠিক এটাই ব্যবহার করে। */
                val got = try { RmpDirectory.refreshFromCloud(this@RegistrationActivity, scope) } catch (_: Throwable) { false }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    // পর্দা বন্ধ হয়ে গেলে যেন কিছু না ভাঙে — try/catch-ই পাহারা।
                    try {
                        if (got) {
                            rmpSuggestCache = cachedRmpChoices(user, scope)
                            rmpSuggestCacheScope = scope
                            showRmpSuggest(user, binding.etRefDoctorName.text.toString().ifBlank {
                                binding.etRefDoctorMobile.text.toString() })
                        }
                    } catch (_: Throwable) { }
                }
            }.start()
        }
        val q = typed.trim().lowercase(Locale.US)
        val mobileDigits = MobileInput.digits(binding.etRefDoctorMobile)
        // 🔴 V1397 (ক) — মোবাইল ১০ ডিজিট পূর্ণ হলে সেটাই সবার আগে, নামের
        // সাথে না মিললেও। এতে ভুল বানানে লেখা নাম আর কমিশন হারায় না।
        val mobileHit = if (mobileDigits.length == 10) all.firstOrNull { it.mobile == mobileDigits } else null
        if (mobileHit != null) {
            boxView.removeAllViews()
            boxView.visibility = View.VISIBLE
            boxView.addView(TextView(this).apply {
                text = "✓ Saved RMP found for this number"
                textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#118452"))
                setPadding(dpRmp(10), dpRmp(7), dpRmp(10), dpRmp(5))
            })
            boxView.addView(rmpSuggestRow(mobileHit, forBranchLabel = true))
            return
        }
        if (q.length < 2) { boxView.removeAllViews(); boxView.visibility = View.GONE; return }
        val hits = all.filter { rmpChoiceMatches(it, q, mobileDigits) }.take(6)
        boxView.removeAllViews()
        if (hits.isEmpty()) { boxView.visibility = View.GONE; return }
        boxView.visibility = View.VISIBLE
        boxView.addView(TextView(this).apply {
            text = if (hits.size == 1) "1 saved RMP found" else "${hits.size} saved RMP found"
            textSize = 10.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#118452"))
            setPadding(dpRmp(10), dpRmp(7), dpRmp(10), dpRmp(5))
        })
        for ((i, item) in hits.withIndex()) {
            if (i > 0) boxView.addView(View(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#EDF2F8"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpRmp(1))
            })
            boxView.addView(rmpSuggestRow(item))
        }
    }

    /** V1397 (গ) — মিল না পেলে নতুন RMP হিসেবে সেভ করার সাজেশন। রেজিস্ট্রেশন
     *  কখনো আটকায় না, ব্লক করে না — শুধু নাম-ঘর ছাড়ার সময় একটা প্রশ্ন। */
    private fun maybeOfferNewRmp(user: NativeUser) {
        if (binding.llRefDoctor.visibility != View.VISIBLE) return
        val name = binding.etRefDoctorName.text.toString().trim()
        if (name.isBlank()) return
        val mobileDigits = MobileInput.digits(binding.etRefDoctorMobile)
        val askKey = "$name|$mobileDigits"
        if (askKey == rmpNewSuggestAskedFor) return
        val all = rmpSuggestCache ?: cachedRmpChoices(user, regSelectedBranch())
        val q = name.lowercase(Locale.US)
        val matched = all.any { rmpChoiceMatches(it, q, mobileDigits) }
        if (matched) return
        if (mobileDigits.length != 10) return   // নম্বর ছাড়া নতুন RMP তৈরি করা যায় না (Add RMP ফর্মের একই নিয়ম)
        rmpNewSuggestAskedFor = askKey
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "New RMP?"))
            .setMessage("No saved RMP found for \"$name\".\nSave as a new RMP so future commission links automatically?")
            .setNegativeButton("Not now", null)
            .setPositiveButton("Save") { _, _ ->
                val branch = binding.spBranch.selectedItem?.toString().orEmpty()
                val staffMobile = NativeSession.current(this)?.mobile.orEmpty()
                if (branch.isBlank()) return@setPositiveButton
                lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        DoctorVisitRepository().addNewDoctor(name, mobileDigits, branch, "", "", "", staffMobile, this@RegistrationActivity)
                    }
                    rmpSuggestCache = null
                    android.widget.Toast.makeText(
                        this@RegistrationActivity,
                        if (ok) "RMP saved" else "Saved on this phone — will sync when online",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show().also { PremiumAlert.paint(it) }
    }

    /** forBranchLabel = true (V1397) — মোবাইল-মিলের বিশেষ সারি: এলাকা/ব্রাঞ্চ-চিপ
     *  না দেখিয়ে "মোবাইল · For <branch> Branch" একই লাইনে, ডেমো-প্রুফের হুবহু। */
    private fun rmpSuggestRow(item: RmpChoice, forBranchLabel: Boolean = false): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpRmp(10), dpRmp(8), dpRmp(10), dpRmp(8))
            isClickable = true
        }
        row.addView(View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#118452"))
            layoutParams = LinearLayout.LayoutParams(dpRmp(3), dpRmp(30)).apply { marginEnd = dpRmp(9) }
        })
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        texts.addView(TextView(this).apply {
            // 👁 V752-এর একই নিয়ম — তালিকায় নাম সবসময় বড় হাতে।
            text = item.name.trim().uppercase()
            textSize = 13.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#17312A"))
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        val line2 = if (forBranchLabel) {
            listOf(item.mobile, if (item.branch.isNotBlank()) "For ${item.branch} Branch" else "")
                .filter { it.isNotBlank() }.joinToString(" · ")
        } else {
            listOf(item.mobile, item.area.trim().uppercase()).filter { it.isNotBlank() }.joinToString(" · ")
        }
        if (line2.isNotBlank()) texts.addView(TextView(this).apply {
            text = line2; textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#60766D"))
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        })
        row.addView(texts)
        if (!forBranchLabel && item.branch.isNotBlank()) row.addView(TextView(this).apply {
            text = item.branch
            textSize = 10f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#15549B"))
            setPadding(dpRmp(9), dpRmp(3), dpRmp(9), dpRmp(3))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpRmp(12).toFloat()
                setColor(android.graphics.Color.parseColor("#E8F2FF"))
            }
        })
        if (forBranchLabel) row.addView(TextView(this).apply {
            text = "TAP TO USE"
            textSize = 9.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(dpRmp(9), dpRmp(4), dpRmp(9), dpRmp(4))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpRmp(12).toFloat()
                setColor(android.graphics.Color.parseColor("#118452"))
            }
        })
        row.setOnClickListener {
            rmpSuggestMuted = true
            try {
                binding.etRefDoctorName.setText(item.name.trim().uppercase())
                binding.etRefDoctorMobile.setText(item.mobile)
            } finally { rmpSuggestMuted = false }
            binding.llRmpSuggest.removeAllViews()
            binding.llRmpSuggest.visibility = View.GONE
        }
        return row
    }

    private fun dpRmp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun showSavedRmpPicker(user: NativeUser) {
        val scope = regSelectedBranch()
        val all = cachedRmpChoices(user, scope)
        /* 🟢🔒 V802 (২৮.০৮.২০২৬) — TK: "RMP পাঠিয়েছে, তাহলে এখন কেন দেখাচ্ছে না
           আরএমপি লিস্ট?" ─ কারণ ছিল: তালিকাটা শুধু `doctor_visit_cache` থেকে পড়ত,
           আর ওই ঘরটা ভরে **একমাত্র Doctor Visit পর্দা খুললে**। যে ফোনে কেউ ওই
           পর্দা কখনো খোলেনি, সেখানে তালিকা চিরকাল ফাঁকাই থাকত (পুরনো লেখাতেই
           ছিল — "No cloud search was made")।
           এখন ফাঁকা হলে **একবার** হালকা পড়া হয় (৮টা ঘর, ভারী `callHistory` ও
           `referralPayments` ছাড়া ⇒ কয়েক KB), তারপর ফোনে জমা থাকে — পরের বার
           এক বাইটও খরচ নেই। নেট না থাকলে আগের মতোই হাতে লেখা যায়। */
        if (all.isEmpty() && !RmpDirectory.hasDoctorVisitCache(this@RegistrationActivity, scope) &&
            RmpDirectory.cachedRows(this@RegistrationActivity, scope).length() == 0) {   // ⛔ একবারই — নামানো হয়ে গেলে আর নয়
            android.widget.Toast.makeText(this@RegistrationActivity, "Loading saved RMP list…", android.widget.Toast.LENGTH_SHORT).show()
            Thread {
                val got = RmpDirectory.refreshFromCloud(this@RegistrationActivity, scope)   // SharedPreferences একই ফাইল, তাই Activity-context যথেষ্ট
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    // ⛔ পর্দা ইতিমধ্যে বন্ধ হয়ে গেলে ডায়ালগ দেখাতে গিয়ে যেন অ্যাপ না থামে
                    try {
                    if (got) showSavedRmpPicker(user) else AlertDialog.Builder(this@RegistrationActivity)
                        .setCustomTitle(PremiumAlert.header(this@RegistrationActivity, "Saved RMP list not available"))
                        .setMessage("Could not load the saved RMP list. Check the internet connection and try again, or enter the Doctor / RMP name and mobile manually below.")
                        .setPositiveButton("OK", null)
                        .show().also { PremiumAlert.paint(it) }
                    } catch (_: Throwable) { }
                }
            }.start()
            return
        }
        if (all.isEmpty()) {
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Saved RMP list not available"))
                .setMessage("No saved RMP list is available on this phone yet. You can enter the Doctor / RMP name and mobile manually below. No cloud search was made.")
                .setPositiveButton("OK", null)
                .show().also { PremiumAlert.paint(it) }
            return
        }

        val pad = (16 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        val search = EditText(this).apply {
            hint = "Search by name, mobile or area"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
            setBackgroundResource(com.tkbiswas.pilesclinic.R.drawable.bg_input_field)
            setPadding(pad, pad / 2, pad, pad / 2)
        }
        val status = TextView(this).apply {
            textSize = 13f
            setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#147A45"))
            setPadding(6, pad / 2, 4, pad / 2)
        }
        val list = ListView(this).apply {
            divider = android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            dividerHeight = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, pad / 2)
            clipToPadding = false
            setBackgroundColor(android.graphics.Color.WHITE)
            isFastScrollEnabled = all.size > 30
        }
        box.addView(search, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        box.addView(status, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        box.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (360 * resources.displayMetrics.density).toInt()))

        var shown = all
        fun render(query: String = "") {
            val q = query.trim().lowercase(Locale.US)
            shown = if (q.isBlank()) all else all.filter { it.searchText().contains(q) }
            status.text = if (shown.isEmpty()) "No matching saved RMP — use manual entry" else "${shown.size} saved RMP found"
            list.adapter = object : android.widget.BaseAdapter() {
                override fun getCount(): Int = shown.size
                override fun getItem(position: Int): RmpChoice = shown[position]
                override fun getItemId(position: Int): Long = position.toLong()

                private fun branchTag(text: String): TextView = TextView(this@RegistrationActivity).apply {
                    this.text = text
                    textSize = 11f
                    setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#15549B"))
                    setPadding(pad / 2, pad / 4, pad / 2, pad / 4)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        cornerRadius = 18 * resources.displayMetrics.density
                        setColor(android.graphics.Color.parseColor("#E8F2FF"))
                    }
                }

                override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup?): android.view.View {
                    val item = getItem(position)
                    return LinearLayout(this@RegistrationActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 14 * resources.displayMetrics.density
                            setColor(android.graphics.Color.WHITE)
                            setStroke((1 * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#DBE8E2"))
                        }
                        elevation = 2f * resources.displayMetrics.density
                        addView(android.view.View(this@RegistrationActivity).apply {
                            setBackgroundColor(android.graphics.Color.parseColor("#118452"))
                        }, LinearLayout.LayoutParams((4 * resources.displayMetrics.density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT))
                        addView(LinearLayout(this@RegistrationActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(pad * 3 / 4, pad * 3 / 4, pad * 3 / 4, pad * 3 / 4)
                            addView(LinearLayout(this@RegistrationActivity).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                addView(TextView(this@RegistrationActivity).apply {
                                    // 👁️ V752 (২৭.০৮.২০২৬, TK-রিপোর্ট ছবিসহ:
                                    //    *"একই যায়গায় ২ রকম — সবাই আমরা বিভ্রান্ত হয়ে যাচ্ছি"*)
                                    //    স্টাফেরা কেউ ছোট হাতে, কেউ বড় হাতে নাম লিখে সেভ
                                    //    করেছেন ("amit goldar" বনাম "AMIT LAL BARMAN"), তাই এক
                                    //    তালিকাতেই দু'রকম দেখাত। ⛔ ডেটাবেসের লেখা বদলানো হয়নি —
                                    //    শুধু **দেখানোর সময়** এক রকম করা হলো।
                                    text = item.name.trim().uppercase()
                                    textSize = 17f
                                    setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                    setTextColor(android.graphics.Color.parseColor("#17312A"))
                                    maxLines = 2
                                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = pad / 2 })
                                if (item.branch.isNotBlank()) addView(branchTag(item.branch))
                            })
                            addView(TextView(this@RegistrationActivity).apply {
                                text = if (item.mobile.isBlank()) "Mobile not saved" else item.mobile
                                textSize = 14f
                                setTextColor(android.graphics.Color.parseColor("#29483C"))
                                setPadding(0, pad / 3, 0, 0)
                            })
                            if (item.area.isNotBlank()) addView(TextView(this@RegistrationActivity).apply {
                                // 👁️ V752 — এলাকার নামও একই কারণে এক রকম।
                                text = item.area.trim().uppercase()
                                textSize = 13f
                                setTextColor(android.graphics.Color.parseColor("#60766D"))
                                setPadding(0, pad / 5, 0, 0)
                                maxLines = 2
                            })
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    }
                }
            }
        }
        render()

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Select Saved RMP / Doctor"))
            .setView(box)
            .setNegativeButton("Manual Entry", null)
            .create()
        list.setOnItemClickListener { _, _, position, _ ->
            val selected = shown.getOrNull(position) ?: return@setOnItemClickListener
            // 👁️ V752 — তালিকায় যেমন দেখাচ্ছে, ঘরেও ঠিক তেমনই বসবে
            //    (নইলে বেছে নেওয়ার পরে চেহারা বদলে যেত — সেটাই বিভ্রান্তির শুরু)।
            binding.etRefDoctorName.setText(selected.name.trim().uppercase())
            binding.etRefDoctorMobile.setText(selected.mobile)
            dialog.dismiss()
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        dialog.setOnShowListener { PremiumAlert.paint(dialog) }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    // 🔒 V235: Enquiry থেকে Registration খোলা হলে সেই পুরনো নম্বর (থাকলে)।
    private var enquiryOriginMobile = ""

    private var lastAutofilledMobile = ""

    /** When a 10-digit mobile is entered, pull that person's saved Enquiry and
     * auto-fill Name / Branch / Disease (only empty fields), matching the web
     * flow where an Enquiry continues into Registration. */
    private fun setupEnquiryAutofill() {
        binding.etMobile.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable) {
                val digits = MobileInput.digits(binding.etMobile)
                if (digits.length == 10 && digits != lastAutofilledMobile) {
                    lastAutofilledMobile = digits
                    autofillFromEnquiry(digits)
                    checkExistingPatientPopup(digits)
                }
                // 🔒 V235: Chamber-এ নতুন নম্বর দিলে (Primary ≠ Enquiry-র মূল নম্বর)
                // পুরনো Enquiry নম্বর আপনা থেকেই Alternate-এ বসে (Alternate ফাঁকা হলে)।
                // একই নম্বর হলে কিছুই বসে না → duplicate হয় না।
                if (enquiryOriginMobile.length == 10 && digits.length == 10 &&
                    digits != enquiryOriginMobile &&
                    MobileInput.digits(binding.etAltMobile).isBlank()) {
                    // ⛔ V754 — অ্যাপ **নিজে** বসাচ্ছে, তাই এতে পপ-আপ দেখানো হবে না
                    //    (নইলে প্রতিবার নিজের বসানো নম্বরেই সতর্কবার্তা আসত)।
                    altFilledByApp = true
                    binding.etAltMobile.setText(enquiryOriginMobile)
                    altFilledByApp = false
                }
            }
        })

        /* 🔍🔒 V754 (২৭.০৮.২০২৬, TK-নির্দেশ — *"অল্টারনেট নাম্বার বসালে আগে যদি
           আমাদের ডাটাবেসে থাকে তাহলে কেন শো করবে না"*)

           **আসল কারণ (কোড ধরে যাচাই):** পুরনো রোগী খোঁজার কাজটা **শুধু মূল
           Mobile ঘরে** লেখা ছিল (উপরের TextWatcher)। Alternate ঘরে কোনো
           লুকআপই ছিল না, তাই ডেটাবেসে থাকা নম্বর ওখানে বসালেও কিছু দেখাত না।

           এখন Alternate ঘরেও **হুবহু একই পপ-আপ** আসে।
           ⛔ শুধু **দেখানো** — ফর্মের কোনো ঘর নিজে থেকে ভরে না
              (`autofillFromEnquiry` ইচ্ছে করেই ডাকা হয়নি; নইলে স্টাফের টাইপ
              করা তথ্য অন্য রোগীর তথ্যে চাপা পড়ে যেত)।
           ⛔ মূল নম্বরের সমান হলে কিছুই হয় না (একই রোগী, অকারণ পপ-আপ নয়)। */
        binding.etAltMobile.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable) {
                if (altFilledByApp) return
                val alt = MobileInput.digits(binding.etAltMobile)
                if (alt.length != 10) return
                if (alt == MobileInput.digits(binding.etMobile)) return
                if (alt == lastDupCheckedAltMobile) return
                lastDupCheckedAltMobile = alt
                checkExistingPatientPopup(alt, fromAlt = true)
            }
        })
    }

    private var lastDupCheckedMobile = ""

    /** 🔍 V754 — Alternate ঘরের নিজের আলাদা পাহারা। মূল ঘরেরটার সঙ্গে মিলিয়ে
     *  ফেললে একটা আরেকটাকে আটকে দিত (একই নম্বর দু'ঘরে বসলে দ্বিতীয়বার আর
     *  দেখাত না) — তাই আলাদা রাখা হলো। */
    private var lastDupCheckedAltMobile = ""

    /** ⛔ অ্যাপ নিজে Alternate ঘর ভরছে কিনা — তখন পপ-আপ দেখানো হয় না। */
    private var altFilledByApp = false

    /** Web parity: as soon as a full mobile number is entered, if that patient
     *  already exists show the duplicate popup immediately (not after the form is
     *  filled). The final "Update Existing" confirm still runs at save time. */
    private fun checkExistingPatientPopup(digits: String, fromAlt: Boolean = false) {
        // 🔍 V754 — Alternate ঘর থেকে এলে নিজের পাহারা আগেই দেখা হয়ে গেছে,
        //    তাই মূল ঘরের পাহারায় আটকানো চলবে না (নইলে কিছুই দেখাত না)।
        if (!fromAlt) {
            if (digits == lastDupCheckedMobile) return
            lastDupCheckedMobile = digits
        }
        lifecycleScope.launch {
            val dup = withContext(Dispatchers.IO) { repository.checkDuplicatePatient(digits) }
            if (!dup.found) return@launch
            // Same premium popup Enquiry uses (dialog_duplicate). The mobile field
            // is never cleared — the number the user typed stays exactly as-is.
            val view = layoutInflater.inflate(com.tkbiswas.pilesclinic.R.layout.dialog_duplicate, null)
            val tvDupName = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupName)
            val tvDupMobile = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupMobile)
            tvDupName.text = dup.name.ifBlank { "-" }
            tvDupMobile.text = digits
            tvDupName.copyOnLongPress("Name", dup.name)
            tvDupMobile.copyOnLongPress("Mobile", digits)
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupBranch).text = dup.branch.ifBlank { "-" }
            // 🔍 V754 — কোন ঘরের নম্বরে মিলেছে, সেটা স্পষ্ট থাকুক।
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupSection).text =
                if (fromAlt) "Patient (Alternate number)" else "Patient"
            UppercaseInputUtil.applyToAll(view)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
            val dialog = AlertDialog.Builder(this@RegistrationActivity).setView(view).setCancelable(true).create()
            dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupView).setOnClickListener {
                // 🔴 TK-REPORTED (04.08.2026, একই ক্লাসের বাগ): dialog.dismiss()
                // ডাকা হত, তাই Timeline থেকে Back করলে এই পপ-আপ আর দেখা যেত না।
                startActivity(android.content.Intent(this@RegistrationActivity, PatientTimelineActivity::class.java).putExtra("mobile", digits))
            }
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupClose).setOnClickListener { dialog.dismiss() }
            /* 👥🔒 V1011 (০৩.০৯.২০২৬, TK-রিপোর্ট: *"একই নাম্বার দিয়ে দ্বিতীয়
               পেশেন্টের রেজিস্ট্রেশন করা সম্ভব হচ্ছে না"* — ফোনেও)।
               **কোড পড়ে যা পাওয়া গেল:** নম্বরটা লেখামাত্রই এই পপ-আপটা আসে,
               আর এতে বোতাম ছিল মাত্র দুটো — **View Existing** ও **Close**।
               "Different Patient — Same Mobile" বাছাইটা আছে **সেভ চাপার পরের**
               পপ-আপে; কিন্তু স্টাফ এখানেই *"এই নম্বর তো আগেই আছে"* দেখে থেমে
               যান, ফর্মটাই আর ভরেন না — তাই দ্বিতীয় রোগী কখনো তৈরি হয় না।
               ⇒ এখন এই প্রথম পপ-আপেই তৃতীয় বোতামটা থাকে; চাপলে পপ-আপ বন্ধ হয়
                 আর স্টাফ জানেন ফর্ম ভরে Save চাপলেই আলাদা রোগী হিসেবে বাছা যাবে।
               ⛔ এখানে কিছুই সেভ হয় না, কোনো সিদ্ধান্তও জমা হয় না — আসল বাছাই
                  আগের মতোই সেভের পপ-আপে (V516), এক অক্ষরও বদলায়নি। */
            val btnDiffHint: android.widget.TextView =
                view.findViewById(com.tkbiswas.pilesclinic.R.id.btnDupDifferent)
            btnDiffHint.visibility = android.view.View.VISIBLE
            btnDiffHint.setOnClickListener {
                dialog.dismiss()
                android.widget.Toast.makeText(
                    this@RegistrationActivity,
                    "Fill the form and press Save, then choose \"Different Patient\".",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            dialog.show()
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
        }
    }

    private fun autofillFromEnquiry(digits: String) {
        // 🟢🔒 V620 (২৪.০৮.২০২৬) — নম্বর বদলালে আগের নম্বরের "Unexpected
        // Time" অবস্থা যেন কখনো নতুন নম্বরে রয়ে না যায় (নতুন Enquiry
        // খোঁজার ফলাফল আসার আগেই এখানে রিসেট) — নইলে mobile-A (Unexpected)
        // থেকে mobile-B (Enquiry নেই/Official)-এ গেলে বোতাম ভুলভাবে
        // visible/selected থেকে যেত।
        binding.btnRegTimingUnexpected.visibility = android.view.View.GONE
        if (selectedTiming == "Unexpected Time") selectRegTiming("Official Time")
        lifecycleScope.launch {
            val enq = withContext(Dispatchers.IO) {
                /* 🟣🔒 V961 (০১.০৯.২০২৬, TK-নির্দেশ) — TK: *"এই লোক কোন
                   আনএক্সপেক্টেড টাইমে কল করেনি, অফিশিয়াল টাইমেই কল করেছে,
                   তাহলে এনাকে আনএক্সপেক্টেড বলে কেন গণ্য করা হলো"*।
                   **আসল দোষ:** এই খোঁজাটা সাজানো ছাড়া একটামাত্র সারি আনত, তাই
                   এক নম্বরে একাধিক এনকোয়ারি থাকলে **পুরনো/অন্য যেকোনো একটা**
                   জিতে যেত — আর তার Timing ("Unexpected Time") রোগীর সারিতে
                   বসে গিয়ে Extra Income তৈরি করত। সার্ভারের নিয়ম (V418 SQL)
                   কিন্তু **সবচেয়ে নতুন** এনকোয়ারিই ধরে। এখন এখানেও তাই। */
                val rows = SupabaseClient.findByMobile(
                    "enquiries", "+91$digits", "name,branch,disease,address,timeType",
                    order = "createdAt.desc.nullslast,date.desc.nullslast"
                )
                if (rows.length() == 0) null else rows.getJSONObject(0)
            } ?: return@launch

            if (binding.etName.text.isNullOrBlank()) {
                binding.etName.setText(enq.s("name"))
            }
            /* ═══════════════════════════════════════════════════════════
               🔴🔒 V1110 (০৫.০৯.২০২৬, TK-এর স্থায়ী নিয়ম — KASHAB MANDAL-এর
               ঘটনার পরে): *"ইনকোয়ারি যে কোন ব্রাঞ্চের হতেই পারে, সেটা কোন
               ব্যাপারই না। **রেজিস্ট্রেশন কোন ব্রাঞ্চে হলো সেটাই ম্যাটার করে**।
               … জলপাইগুড়ির স্টাফ যখন রেজিস্ট্রেশন করেছে, জলপাইগুড়ির চেম্বারে
               এসেছে পেশেন্ট — তাই অটোমেটিক জলপাইগুড়ি হতে হবে, এবং টাকা পয়সা
               সমস্ত হিসাব জলপাইগুড়ির নামেই হবে।"*

               🔴 আগে এখানে **এনকোয়ারির ব্রাঞ্চটা জোর করে বসিয়ে দেওয়া হত** —
                  স্টাফের নিজের ব্রাঞ্চ মুছে দিয়ে, চুপচাপ, ৩-বার-চাপার তালাও
                  না খুলিয়ে। ঠিক এই কারণেই কোচবিহারের পুরনো এনকোয়ারি থাকা
                  রোগীর আইডি `COB-…` হয়ে গিয়েছিল।
               ⇒ লাইনটা তুলে দেওয়া হলো। ব্রাঞ্চ এখন **সবসময় রেজিস্ট্রেশন করা
                 স্টাফের নিজের ব্রাঞ্চ** (নিচের setupSpinners + সেভের পাহারা)।
               ⛔ এনকোয়ারি থেকে নাম · রোগ · ঠিকানা · সময় — বাকি সব আগের মতোই আসে।
               ═══════════════════════════════════════════════════════════ */
            /* 🩺🔒 V1000 (০৩.০৯.২০২৬) — Enquiry-তে এখন একাধিক রোগ বাছা যায়
               (TK: "একই লোকের তো দুই রকমের রোগ থাকতেই পারে"), আর একাধিক হলে
               ঘরটায় ", " দিয়ে জোড়া লেখা থাকে। আগে হুবহু এক নামে মেলানো হত,
               তাই "Piles, Fissure" এলে একটাও টিক বসত না — রোগের নামটাই
               হারিয়ে যেত। এখন কমা দিয়ে ভেঙে প্রতিটা নামে টিক বসে।
               ⛔ একটামাত্র রোগ হলে আচরণ হুবহু আগের মতোই। */
            val disease = enq.s("disease")
            if (disease.isNotBlank()) {
                disease.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { one ->
                    val cb = diseaseChecks.firstOrNull { it.text.toString().equals(one, ignoreCase = true) }
                    if (cb != null && !cb.isChecked) cb.isChecked = true
                }
            }
            // TK-REQUESTED ADDITION (2026-07-24): Registration Timing
            // auto-fills from the source Enquiry's own Official/Unexpected
            // choice, same as Name/Branch/Disease just above.
            // 🟢🔒 V620 (২৪.০৮.২০২৬, TK-নির্দেশ) — আগে স্টাফ এরপরও হাতে
            // বদলে নিতে পারতেন ("doesn't lock it")। এখন নিয়ম কড়া:
            // Registration "Unexpected Time" শুধু তখনই বাছা/দেখানো যাবে
            // যখন এই Enquiry নিজেই "Unexpected Time" ছিল। Enquiry
            // Official হলে বা না থাকলে বোতামটাই লুকানো থাকে (উপরে
            // `setupTimingButtons()`-এ ডিফল্ট GONE)।
            val timeType = enq.s("timeType")
            if (timeType.equals("Unexpected Time", ignoreCase = true)) {
                binding.btnRegTimingUnexpected.visibility = android.view.View.VISIBLE
                selectRegTiming("Unexpected Time")
            }
            /* 🩺🔒 V1070 (TK-নির্দেশ) — এনকোয়ারিতে লেখা "কে পাঠিয়েছেন" এখানেও
               নিজে থেকে বসে যায়, তাই রোগী এলে RMP-র নামটা আর হাতে লিখতে হয় না
               এবং দুই জায়গায় দুরকম হওয়ার ভয় থাকে না।
               ⛔ এনকোয়ারিতে ঘরটা ফাঁকা থাকলে এখানে কিছুই বদলায় না (আগের মতোই)।
               ⛔ স্টাফ এখানে পরে বদলাতে পারবেন — কিছু লক করা হয়নি। */
            val eRefBy = enq.s("refBy").trim()
            if (eRefBy.isNotBlank()) {
                val at = refByOptions.indexOfFirst { it.equals(eRefBy, ignoreCase = true) }
                if (at >= 0) binding.spRefBy.setSelection(at)
                if (eRefBy.equals("Dr. Visit", ignoreCase = true)) {
                    binding.llRefDoctor.visibility = android.view.View.VISIBLE
                    enq.s("refDoctor").trim().takeIf { it.isNotBlank() }
                        ?.let { binding.etRefDoctorName.setText(it) }
                    enq.s("refDoctorMobile").trim().takeIf { it.isNotBlank() }
                        ?.let { binding.etRefDoctorMobile.setText(it) }
                }
            }
            android.widget.Toast.makeText(this@RegistrationActivity, "Details filled from Enquiry", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private var cameraPhotoUri: Uri? = null
    private var patientPhotoData: String? = null

    private fun showPhotoSourceDialog() {
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Patient Photo"))
            .setItems(arrayOf("📷 Camera", "🖼️ Gallery")) { _, which ->
                if (which == 0) launchCameraWithPermission() else pickPatientPhoto.launch("image/*")
            }
            .setNegativeButton("Cancel", null)
            .show().also { PremiumAlert.paint(it) }
    }

    // CAMERA is declared in the manifest, so on Android 6+ it must be granted at
    // runtime before the camera can launch — otherwise it fails with "Camera open
    // করা গেল না". Request it on demand, then open the camera.
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else android.widget.Toast.makeText(this, "Camera permission is required", android.widget.Toast.LENGTH_SHORT).show()
        }

    private fun launchCameraWithPermission() {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) openCamera() else requestCameraPermission.launch(android.Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        try {
            val dir = java.io.File(cacheDir, "images").apply { mkdirs() }
            val file = java.io.File(dir, "patient_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )
            cameraPhotoUri = uri
            takePicture.launch(uri)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Camera could not open", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraPhotoUri
            if (success && uri != null) applyPickedPhoto(uri)
        }

    /**
     * 🔄🔒 V532 (২২.০৮.২০২৬, TK-নির্দেশ) — রেজিস্ট্রেশনের সময়েও ছবি ঘোরানো।
     *
     * Patient Photo পর্দায় যে নিয়মটা V524 থেকে চলছে, **হুবহু সেটাই**: মূল
     * ছবিটা (`photoBaseBitmap`) ধরে রাখা হয় আর মোট কত ডিগ্রি ঘোরানো হয়েছে
     * সেটা গোনা হয়। প্রতিবার **মূল থেকেই একবার** ঘুরিয়ে বানানো হয় — দশবার
     * ঘোরালেও ছবির মান একটুও নষ্ট হয় না (বারবার JPEG চাপলে ঝাপসা হয়)।
     */
    private var photoBaseBitmap: android.graphics.Bitmap? = null
    private var photoRotateDegrees: Int = 0

    private fun rotatePatientPhoto() {
        val base = photoBaseBitmap ?: return
        photoRotateDegrees = (photoRotateDegrees + 90) % 360
        val shown = PhotoUtils.rotated(base, photoRotateDegrees)
        val dataUrl = PhotoUtils.encodeBitmap(shown)
        if (dataUrl == null) {
            android.widget.Toast.makeText(this, "Could not rotate", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        // ⛔ সেভের পথ এক অক্ষরও বদলায়নি — `patientPhotoData` সেই একই ধরনের
        //    data-URL, ঠিক আগের মতোই Save-এর সময় ব্যবহার হবে।
        patientPhotoData = dataUrl
        binding.imgPatientPhoto.setImageBitmap(shown)
    }

    /** Shared for both camera and gallery: compress (400px / q70) then preview. */
    private fun applyPickedPhoto(uri: Uri) {
        lifecycleScope.launch {
            val dataUrl = withContext(Dispatchers.IO) { PhotoUtils.encodeResized(this@RegistrationActivity, uri) }
            if (dataUrl == null) {
                android.widget.Toast.makeText(this@RegistrationActivity, "Could not get the image", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            patientPhotoData = dataUrl
            val bmp = PhotoUtils.decodeDataUrl(dataUrl)
            if (bmp != null) {
                binding.imgPatientPhoto.setImageBitmap(bmp)
                binding.imgPatientPhoto.visibility = View.VISIBLE
                binding.btnPatientPhoto.text = "📷 Change Photo"
                /* 🔄 V532: নতুন ছবি এলে এটাই "মূল", গোনা শূন্য থেকে শুরু।
                   ছবি আছে ⇒ তবেই Rotate বোতাম দেখা যায়। */
                photoBaseBitmap = bmp
                photoRotateDegrees = 0
                binding.btnRotatePatientPhoto.visibility = View.VISIBLE
                binding.btnRotatePatientPhoto.setOnClickListener { rotatePatientPhoto() }
            }
        }
    }

    private val pickPatientPhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) applyPickedPhoto(uri)
        }

    private var selectedSex = "Male"
    private var selectedPayMode = "CASH"
    // TK-REQUESTED ADDITION (2026-07-24): matches Enquiry form's
    // selectedTiming default/pattern exactly.
    private var selectedTiming = "Official Time"

    private fun tint(btn: android.widget.Button, on: Boolean) {
        val blue = android.graphics.Color.parseColor("#1167D8")
        val lightBlue = android.graphics.Color.parseColor("#E8F2FF")
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(if (on) blue else lightBlue)
        btn.setTextColor(if (on) android.graphics.Color.WHITE else blue)
    }

    private fun setupSexButtons() {
        val map = listOf(binding.btnSexMale to "Male", binding.btnSexFemale to "Female", binding.btnSexOther to "Other")
        map.forEach { (b, v) -> b.setOnClickListener { selectedSex = v; map.forEach { (bb, vv) -> tint(bb, vv == v) } } }
        map.forEach { (b, v) -> tint(b, v == selectedSex) }
    }

    private fun setupPayButtons() {
        val map = listOf(binding.btnPayCash to "CASH", binding.btnPayUpi to "ONLINE")
        map.forEach { (b, v) -> b.setOnClickListener { selectedPayMode = v; map.forEach { (bb, vv) -> tint(bb, vv == v) } } }
        map.forEach { (b, v) -> tint(b, v == selectedPayMode) }
    }

    // TK-REQUESTED ADDITION (2026-07-24): same Official/Unexpected Time
    // toggle the Enquiry form already has -- same tint()/map pattern as
    // setupSexButtons/setupPayButtons just above, nothing else changed.
    // Refactored into selectRegTiming() (below) so autofillFromEnquiry can
    // also set this when auto-filling from an existing Enquiry.
    /* 🕘🔒 V962 (০১.০৯.২০২৬, TK-নির্দেশ) — TK: *"রেজিস্ট্রেশন ফরমে আনএক্সপেক্টেড
       থাকবে না, এক্সপেক্টেড ও থাকবে না … আনএক্সপেক্টেড টাইম সেটাই গণ্য হবে, অল
       ব্রাঞ্চ ইনকয়ারি ফর্ম যদি সকাল ৯টা থেকে সন্ধ্যা ৬টার মধ্যে (না হয়)"*।
       ⇒ এখানে আর কিছু বাছার নেই: সারিটাই লুকানো, কোনো ক্লিকও বসে না। রোগীর
         সময়টা **শুধু এনকোয়ারি থেকেই** আসে (নিচে `autofillFromEnquiry`)।
       ⚠️ এই দুটো বোতাম আগে TK-এর লক-করা তালিকায় ছিল — TK নিজেই তুলতে বলেছেন
          (০১.০৯.২০২৬), তাই তোলা হলো। */
    private fun setupTimingButtons() {
        selectRegTiming(selectedTiming)
        try { binding.rowRegTiming.visibility = android.view.View.GONE } catch (_: Throwable) { }
        // 🟢🔒 V620 (২৪.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট প্রশ্নে নিশ্চিত হয়ে) —
        // "Enquiry-তে Unexpected থাকলে তবেই Registration-এ Unexpected
        // হতে হবে, অন্যথায় না।" আগে স্টাফ যেকোনো সময় নিজে ইচ্ছেমতো
        // "Unexpected Time" বেছে নিতে পারতেন (কোনো Enquiry না থাকলেও)।
        // এখন ডিফল্টভাবে এই বোতামটা **লুকানো** — শুধু নিচের
        // `autofillFromEnquiry()`-এ শর্ত মিললে (আসল Enquiry-ই Unexpected
        // হলে) তবেই দেখা যাবে। ⛔ Official Time বোতাম/আচরণ অপরিবর্তিত।
        binding.btnRegTimingUnexpected.visibility = android.view.View.GONE
    }

    private fun selectRegTiming(value: String) {
        selectedTiming = value
        val map = listOf(binding.btnRegTimingOfficial to "Official Time", binding.btnRegTimingUnexpected to "Unexpected Time")
        map.forEach { (b, v) -> tint(b, v == value) }
    }

    /** Shows a spinner's items in CAPITALS (TK, 2026-07-27) while
     *  getItem()/selectedItem keep returning the original text, so every
     *  comparison and every saved value stays exactly as it was. */
    /**
     * 🔵🔒 V594 (২৩.০৮.২০২৬, TK-অনুমোদিত): `boxTextSp` **শুধু হেডারের ছোট
     * ব্রাঞ্চ-ঘরটার** জন্য। না দিলে (`null`) সব আগের মতোই — অর্থাৎ
     * Occupation · Duration · Ref By-এর ঘরগুলোর একটুও বদলায় না।
     * ⛔ খোলা তালিকাটার (`getDropDownView`) লেখা ছোট করা হয় **না** — বেছে
     *    নেওয়ার সময় বড় লেখাই থাকবে, নইলে পড়তে কষ্ট হত।
     */
    private fun capsAdapter(
        items: List<String>,
        hideFirstInList: Boolean = false,
        hintFirstInBox: Boolean = false,
        boxTextSp: Float? = null
    ): ArrayAdapter<String> =
        object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items) {
            // TK-REPORTED (2026-07-28): "Choose Occupation" / "Select Branch"
            // were drawn in the same dark colour as a real answer, so an empty
            // box looked already filled in. The placeholder row is now drawn in
            // the same light grey every other empty box's hint uses. ONLY the
            // colour changes -- the text, the list positions and the saved
            // value are untouched.
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val tv = v as? android.widget.TextView
                tv?.setAllCaps(true)
                // 🔵 V594 — শুধু যেখানে চাওয়া হয়েছে সেখানেই ছোট লেখা।
                //    ভিতরের ডিফল্ট ফাঁকটাও শূন্য, নইলে ২৮dp ঘরে জায়গা খেয়ে
                //    নিয়ে লেখা আবার কেটে যেত।
                if (boxTextSp != null) {
                    tv?.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, boxTextSp)
                    tv?.setPadding(0, 0, 0, 0)
                    /* 🎨🔒 V892 (৩১.০৮.২০২৬, TK ডেমো ফটো দেখে "হ্যাঁ পাশ") —
                       TK: *"উপরে ব্রাঞ্চের নাম সিলেক্ট করার জায়গার উজ্জ্বলতা কম"*।
                       হেডারের ঘরটার লেখা মোটা (bold) হলো, আর নিচের রঙের নিয়মে
                       "SELECT BRANCH" ফ্যাকাশে ধূসরের বদলে গাঢ় নেভি।
                       ⛔ শুধু হেডারের এই ঘরটাতেই (`boxTextSp` একমাত্র এখানেই
                          পাঠানো হয়) — ফর্মের বাকি বাছাই-ঘরগুলো অপরিবর্তিত। */
                    tv?.setTypeface(null, android.graphics.Typeface.BOLD)
                }
                tv?.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                        this@RegistrationActivity,
                        if (hintFirstInBox && position == 0)
                            (if (boxTextSp != null) com.tkbiswas.pilesclinic.R.color.brand_navy
                             else com.tkbiswas.pilesclinic.R.color.field_hint)
                        else com.tkbiswas.pilesclinic.R.color.clinic_text_primary
                    )
                )
                return v
            }
            // TK-REPORTED (2026-07-27): "প্রথমবার Choose Occupation লেখা আছে ঠিক
            // আছে, পরেরবার আবার ওই বক্সের মধ্যেও কেন Choose Occupation থাকবে?"
            // The first entry is only a placeholder for the closed box, so it is
            // no longer drawn inside the opened list. It stays in the list's
            // positions, so every selection index and every saved value is
            // exactly as before -- only its row is not shown.
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent)
                (v as? android.widget.TextView)?.setAllCaps(true)
                if (hideFirstInList && position == 0) {
                    v.visibility = View.GONE
                    v.layoutParams = android.widget.AbsListView.LayoutParams(
                        android.widget.AbsListView.LayoutParams.MATCH_PARENT, 0
                    )
                } else {
                    v.visibility = View.VISIBLE
                    v.layoutParams = android.widget.AbsListView.LayoutParams(
                        android.widget.AbsListView.LayoutParams.MATCH_PARENT,
                        android.widget.AbsListView.LayoutParams.WRAP_CONTENT
                    )
                }
                return v
            }
        }

    private fun setupSpinners(user: NativeUser) {
        val ownBranchUser = user.role == "staff" || user.role == "doctor"
        /* 🔴🔒 V1110 (TK-এর স্থায়ী নিয়ম, ০৫.০৯.২০২৬): *"যে স্টাফ যে ব্রাঞ্চের,
           শুধুমাত্র সেই ব্রাঞ্চের পেশেন্টের রেজিস্ট্রেশন করতে পারবে।"*
           ⇒ স্টাফ/ডাক্তারের তালিকায় এখন **নিজের ব্রাঞ্চটা ছাড়া আর কিছুই নেই**,
             তাই ভুল করেও অন্য ব্রাঞ্চ বাছা যায় না (৩ বার চেপেও নয়)।
           ⛔ মাস্টার ও Field Officer আগের মতোই নিজে বেছে নেবেন।
           ⛔ ব্রাঞ্চ "All" হলে (কোনো স্টাফের তা-ই থাকতে পারে) আগের তালিকাই থাকে। */
        val lockedOwnBranch = ownBranchUser && user.branch.isNotBlank() && user.branch != "All"
        branchItems = when {
            lockedOwnBranch -> listOf(user.branch)
            ownBranchUser   -> branches
            else            -> listOf(SELECT_BRANCH) + branches
        }
        binding.spBranch.adapter = capsAdapter(
            branchItems,
            hideFirstInList = !ownBranchUser,
            hintFirstInBox = !ownBranchUser,
            // 🔵 V594 (TK-অনুমোদিত) — হেডারের ঘরটা ছোট, তাই লেখাও ১০sp।
            //    এই একটাই জায়গায়; বাকি তিনটে বাছাই-ঘর আগের মতোই।
            boxTextSp = 10f
        )
        // TK RULE (2026-07-28, Registration only): Master and Field Officer must
        // pick the Branch themselves EVERY time -- nothing is pre-filled for
        // them, even when their own account carries a branch name. Staff and
        // Doctor keep their own branch filled in and 3-tap locked, exactly as
        // before.
        if (ownBranchUser && user.branch != "All") {
            val idx = branchItems.indexOf(user.branch)
            if (idx >= 0) binding.spBranch.setSelection(idx)
        }
        binding.spOccupation.adapter =
            capsAdapter(occupations, hideFirstInList = true, hintFirstInBox = true)
        binding.spDurationUnit.adapter = capsAdapter(durationUnits)
        binding.spRefBy.adapter = capsAdapter(refByOptions)

        // TK APPROVED (2026-07-28, proof 2): the three lists now open as a clean
        // centred popup instead of a drop-down that sat on top of the form.
        // Staff and Doctor keep the 3-tap Branch lock exactly as before.
        SpinnerPicker.attach(
            binding.spBranch,
            "SELECT BRANCH",
            hidePlaceholder = !ownBranchUser,
            // 🔴 V1110 — নিজের ব্রাঞ্চে বাঁধা থাকলে খোলার কিছু নেই (একটাই নাম)।
            tapsToUnlock = if (ownBranchUser && !lockedOwnBranch) 3 else 1,
            lockLabel = "Branch"
        )
        SpinnerPicker.attach(binding.spOccupation, "CHOOSE OCCUPATION", hidePlaceholder = true)
        SpinnerPicker.attach(binding.spDurationUnit, "CHOOSE DURATION UNIT")
        SpinnerPicker.attach(binding.spRefBy, "REFERRED BY")

        // TK-REPORTED (16.09.2026) ফিক্সের অংশ — Master/Field Officer ব্রাঞ্চ
        // বদলালে আগের ব্রাঞ্চের RMP-ক্যাশ যেন সাথে সাথেই সরে যায়, পরের অক্ষর
        // টাইপ করার আগেই পুরনো ব্রাঞ্চের নাম দেখা না যায়।
        binding.spBranch.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                rmpSuggestCache = null
                rmpSuggestCacheScope = null
                rmpSuggestFetchTried = false
                binding.llRmpSuggest.removeAllViews()
                binding.llRmpSuggest.visibility = View.GONE
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        // Referring-Doctor fields open only for Dr. Visit / RMP.
        binding.spRefBy.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val show = refByOptions.getOrNull(pos) == "Dr. Visit"
                binding.llRefDoctor.visibility = if (show) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    /** A checkable Material chip (no close icon) used for Disease / Symptoms /
     *  Previous Treatment History. Reads the same as the old checkboxes. */
    private fun makeChip(label: String): com.google.android.material.chip.Chip =
        com.google.android.material.chip.Chip(this).apply {
            text = label
            // TK APPROVED (2026-07-27): everything the staff selects is shown in
            // CAPITALS. setAllCaps only changes what is drawn -- text (and so the
            // saved disease/symptom/history value) stays exactly as before, so no
            // old record and no report grouping is affected.
            setAllCaps(true)
            // TK-REQUESTED (2026-07-27): a little smaller so two long chips fit
            // on one line. Only the drawn size changes -- text and saved value
            // are untouched.
            textSize = 11.5f
            textStartPadding = 8f * resources.displayMetrics.density
            textEndPadding = 8f * resources.displayMetrics.density
            isCheckable = true
            isClickable = true
            isCloseIconVisible = false
            // TK-requested (2026-07-16): selected chip was too dull (default
            // Material grey-on-grey checkmark). Now selected = bright green
            // with white text; unchecked look is unchanged. Color only.
            chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(this@RegistrationActivity, com.tkbiswas.pilesclinic.R.color.chip_bg_selector)
            setTextColor(androidx.core.content.ContextCompat.getColorStateList(this@RegistrationActivity, com.tkbiswas.pilesclinic.R.color.chip_text_selector))
            checkedIconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        }

    private fun setupCheckboxGroups() {
        diseaseOptions.forEach { label ->
            val cb = makeChip(label); diseaseChecks.add(cb); binding.diseaseGroup.addView(cb)
            // 🔒 TK-ORDER (30.07.2026, Disease এখন বাধ্যতামূলক): অন্য সব
            // বাধ্যতামূলক ঘরের মতোই (EditText-এ TextWatcher, FieldError.kt
            // দেখুন) — কোনো একটা চিপ বাছামাত্র লাল বর্ডার সরে যায়।
            cb.setOnCheckedChangeListener { _, isChecked -> if (isChecked) FieldError.clear(binding.diseaseGroup) }
        }
        // TK-REQUESTED (2026-07-27): three fixed rows -- 3 / 2 / 2 -- so
        // FLUID DISCHARGE and MASSA BARA HUA are always on the same line.
        symptomOptions.forEachIndexed { index, label ->
            val cb = makeChip(label)
            symptomChecks.add(cb)
            when {
                index < 3 -> binding.symptomGroup.addView(cb)
                index < 5 -> binding.symptomGroup2.addView(cb)
                else -> binding.symptomGroup3.addView(cb)
            }
        }
        medHistOptions.forEach { label ->
            val cb = makeChip(label); medHistChecks.add(cb); binding.medHistGroup.addView(cb)
        }
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()
        DatePickerDialog(this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker, { _, y, m, d ->
            val cal2 = Calendar.getInstance().apply { set(y, m, d) }
            val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal2.time)
            selectedDate = iso
            showRegDate()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = today.timeInMillis
        }.show()
    }

    /** তারিখের ঘরে কী লেখা থাকবে — ফাঁকা থাকলে স্টাফকে বলা হয় চেপে বাছতে। */
    private fun showRegDate() {
        binding.tvDate.text = if (selectedDate.isBlank()) "Tap to select" else displayDate(selectedDate)
    }

    private fun displayDate(iso: String): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso)
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(parsed!!)
    } catch (e: Exception) { iso }

    private fun validateAndSave(user: NativeUser) {
        hideError()
        val name = binding.etName.text.toString().trim()
        val mobile = StaffDirectory.normalizeMobile(binding.etMobile.text.toString())
        val branch = binding.spBranch.selectedItem?.toString() ?: ""
        val feeRaw = binding.etFee.text.toString().trim()
        val fee = feeRaw.toDoubleOrNull() ?: 0.0

        // Same validation order/messages as savePatient() in app.js.
        // 🔴🔒 V1143 (TK-নির্দেশ) — তারিখ না বাছলে রেজিস্ট্রেশন সেভ হবে না।
        if (selectedDate.isBlank()) { focusError(binding.tvDate, "Registration date mandatory — tap to select"); return }
        if (selectedDate > PatientIdGenerator.todayIso()) { focusError(binding.tvDate, "Future registration date not allowed"); return }
        if (name.isBlank()) { focusError(binding.etName, "Patient name mandatory"); return }
        if (mobile.length != 10) { focusError(binding.etMobile, "Valid mobile number mandatory"); return }
        if (branch.isBlank() || branch == SELECT_BRANCH) { focusError(binding.spBranch, "Branch mandatory"); return }
        /* 🔴🔒 V1110 (TK-এর স্থায়ী নিয়ম) — শেষ পাহারা: স্টাফ/ডাক্তার নিজের
           ব্রাঞ্চ ছাড়া অন্য কোনো ব্রাঞ্চে রেজিস্ট্রেশন করতে পারবেন না।
           ⛔ উপরের তালিকাতেই আর অন্য ব্রাঞ্চ নেই, তবু নিয়মটা **এখানেও** থাকা
              দরকার — নইলে ভবিষ্যতে অন্য কোনো পথ এই পর্দা ব্যবহার করলে ফাঁকটা
              আবার ফিরে আসবে (ঠিক এভাবেই খাতার সারি B98-এর পরেও একটা জায়গা
              বাদ পড়ে গিয়েছিল)।
           ⛔ মাস্টার ও Field Officer অপরিবর্তিত। */
        if ((user.role == "staff" || user.role == "doctor") &&
            user.branch.isNotBlank() && user.branch != "All" &&
            !branch.equals(user.branch, ignoreCase = true)) {
            focusError(binding.spBranch, "You can register only ${user.branch} patients")
            return
        }
        if (!(fee > 0)) { focusError(binding.etFee, "Registration Fee mandatory"); return }
        // 🔒 TK-ORDER (30.07.2026): "Registration Form-এ রোগের নাম বাধ্যতামূলক
        // করে দিন।" আগে Disease-এ একটাও চিপ না বাছলেও Save হয়ে যেত। এখন
        // বাকি ৪টা বাধ্যতামূলক ঘরের (Name/Mobile/Branch/Fee) হুবহু একই
        // নিয়মে (focusError → লাল বর্ডার + সরাসরি ওখানে স্ক্রল+ফোকাস)।
        if (diseaseChecks.none { it.isChecked }) { focusError(binding.diseaseGroup, "Disease mandatory — select at least one"); return }

        /* 🛡️🔒 V863 (৩০.০৮.২০২৬, TK-অনুমোদিত) — এনকোয়ারির হুবহু একই পাহারা।
           ⛔ আমাদের নম্বর না হলে নিচের সবটা এক অক্ষরও না বদলে আগের মতোই চলে। */
        OwnNumberGuard.confirmIfOwn(this, mobile) { continueSave(user, name, mobile, branch, fee) }
    }

    private fun continueSave(user: NativeUser, name: String, mobile: String, branch: String, fee: Double) {
        setLoading(true)
        lifecycleScope.launch {
            val duplicate = withContext(Dispatchers.IO) { repository.checkDuplicatePatient(mobile) }
            // 🚨 খাতার সারি B133 (TK ফটো-প্রুফে পাশ, 29.07.2026 রাত ৮.৫০):
            // রেকর্ডটা আগে Registration Cancel / Incomplete করা ছিল কিনা —
            // থাকলে বাক্সে লাল সতর্কবার্তা, নইলে বাক্সটা হুবহু আগের মতোই।
            // ⛔ Enquiry-র সঙ্গে **একই ফাংশন** (`EnquiryRepository.closedInfo`),
            //    তাই দুই পর্দা কখনো দুই কথা বলবে না।
            // ⛔ শুধু ডুপ্লিকেট ধরা পড়লেই এই পড়াটা হয়, রোজকার সেভে নয়।
            val closed = if (duplicate.found)
                withContext(Dispatchers.IO) { EnquiryRepository(this@RegistrationActivity).closedInfo(mobile) }
            else EnquiryRepository.ClosedInfo(false)
            setLoading(false)
            if (duplicate.found) {
                showDuplicateDialog(duplicate, user, name, mobile, branch, fee, closed)
            } else if (!duplicate.verified) {
                // 🚨 TK'S RULE (28.07.2026, খাতার সারি B30): *"কোন প্রকার রোগীর যেন
                // ডুপ্লিকেট না হয়। সিস্টেমে যদি আগে থেকে থাকে অবশ্যই ওয়ার্নিং দিতে হবে।"*
                // লাইন খারাপ থাকলে ক্লাউডে দেখাই যায় না — আগে সেটাকে "নতুন নম্বর"
                // ধরে নিয়ে চুপচাপ দ্বিতীয় রোগী তৈরি হয়ে যেত। এখন স্টাফকে জানানো হয়
                // এবং সিদ্ধান্তটা তাঁর হাতে দেওয়া হয়। ⛔ কিছুই আটকানো হয়নি — তিনি
                // চাইলে আগের মতোই সেভ করতে পারবেন।
                AlertDialog.Builder(this@RegistrationActivity)
                    .setCustomTitle(PremiumAlert.header(this@RegistrationActivity, "⚠️ Could not be checked")   /* 🔤 V726 */)
                    .setMessage(
                        "নেট ঠিকমতো কাজ করছে না, তাই এই নম্বরটা আগে থেকে রেজিস্টার করা আছে কিনা দেখা গেল না।\n\n" +
                        "এখনই সেভ করলে একই রোগীর দ্বিতীয় রেকর্ড তৈরি হয়ে যেতে পারে।\n\n" +
                        "লাইন ঠিক হলে \"আবার দেখুন\" চাপুন।"
                    )
                    .setPositiveButton("Check Again") { _, _ -> validateAndSave(user) }
                    .setNegativeButton("Save Anyway") { _, _ -> performSave(user, name, mobile, branch, fee) }
                    .setNeutralButton("Close", null)
                    .show().also { PremiumAlert.paint(it) }
            } else {
                // 🔒🔒 B601 (10.08.2026, TK-অনুমোদিত প্রুফ · "ফাইনাল/লক"): active
                // patient সারি নেই — কিন্তু এই নম্বরটা আগে **Reject/Incomplete/
                // Cancelled** হয়ে থাকলে (পুরনো ডেমো/বাতিল রেজিস্ট্রেশন) স্টাফকে
                // Warning দেখানো হয় (View · Continue · Cancel)। ⛔ স্বাভাবিক
                // enquiry→register ফ্লোতে ভুল ওয়ার্নিং আসে না — কারণ closedInfo শুধু
                // Cancelled/Incomplete সারি ধরে (চালু Enquiry নয়)। কোনো নম্বরে
                // এমন ইতিহাস না থাকলে আগের মতোই সরাসরি সেভ, বাড়তি কিছু নয়।
                val hist = withContext(Dispatchers.IO) {
                    try { EnquiryRepository(this@RegistrationActivity).closedInfo(mobile) }
                    catch (_: Throwable) { EnquiryRepository.ClosedInfo(false) }
                }
                if (hist.closed) { showHistoryWarningDialog(user, name, mobile, branch, fee, hist); return@launch }
                /* 🔴🔒 V1269 (০৯.০৯.২০২৬, TK-অনুমোদিত — খাতার সারি ৩৮৯/৩৮৯ক) —
                   নম্বরে কোনো মিল পাওয়া যায়নি, কিন্তু **একই নামে একই ব্রাঞ্চে**
                   আগে রেজিস্ট্রেশন থাকতে পারে (নম্বরে এক ঘর টাইপ-ভুল হলে ঠিক
                   এটাই হয়েছিল — TK-র SQL-এ তিন জোড়া প্রমাণ)।
                   ⛔ আটকানো হয় না — শুধু দেখানো হয়, স্টাফ চাইলে এগোবেন। */
                val sameName = withContext(Dispatchers.IO) {
                    try { repository.checkSameNamePatient(name, branch) }
                    catch (_: Throwable) { RegistrationRepository.DuplicatePatient(false, "", "", "") }
                }
                if (sameName.found) showSameNameWarningDialog(user, name, mobile, branch, fee, sameName)
                else performSave(user, name, mobile, branch, fee)
            }
        }
    }

    /* 🔴🔒 V1269 (০৯.০৯.২০২৬, TK-অনুমোদিত) — **একই নামে একই ব্রাঞ্চে আগে
       রেজিস্ট্রেশন আছে** — শুধু সতর্কবার্তা।
       ⛔ সেভ **আটকানো হয় না** (নামের মিল সব সময় ডুপ্লিকেট নয় — TK-র নিজের
          SQL-ফলেই ১৩ জোড়া আলাদা মানুষ ছিল); স্টাফ "Continue" চাপলে হুবহু
          আগের পথেই নতুন রেজিস্ট্রেশন হয়, Visit Fee/Patient ID-র নিয়ম
          এক অক্ষরও বদলায় না।
       ⛔ পুরনো নম্বরটাও দেখানো হয়, যাতে টাইপ-ভুল সঙ্গে সঙ্গে চোখে পড়ে। */
    private fun showSameNameWarningDialog(
        user: NativeUser, name: String, mobile: String, branch: String, fee: Double,
        dup: RegistrationRepository.DuplicatePatient
    ) {
        val lines = dup.matches.joinToString("\n") { m ->
            "• " + m.name.uppercase() + (if (m.patientId.isNotBlank()) "  ·  " + m.patientId else "")
        }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "⚠️ Same name already registered"))
            .setMessage(
                "This name is already registered in this branch:\n\n" + lines +
                "\n\nThe mobile number is different, so this may be another person.\n" +
                "Please check the number once — one wrong digit creates a second record for the same patient."
            )
            .setPositiveButton("Continue") { _, _ -> performSave(user, name, mobile, branch, fee) }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show().also { PremiumAlert.paint(it) }
    }

    /** 🔒 B601 (10.08.2026, TK-অনুমোদিত প্রুফ): নম্বরটা আগে Reject/Incomplete/
     *  Cancelled ছিল অথচ এখন active patient নেই — এই Warning। বিদ্যমান locked
     *  `dialog_duplicate` ডিজাইনই, শুধু মাঝের বোতাম "CONTINUE" (নতুন রেজিস্ট্রেশন
     *  হিসেবে এগোয়; Visit Fee/patientId লজিক একটুও বদলায় না — performSave আগের
     *  "নতুন" পথেই ডাকা হয়, existing id ছাড়া)। */
    private fun showHistoryWarningDialog(
        user: NativeUser, name: String, mobile: String, branch: String, fee: Double,
        hist: EnquiryRepository.ClosedInfo
    ) {
        val view = layoutInflater.inflate(com.tkbiswas.pilesclinic.R.layout.dialog_duplicate, null)
        val isIncomplete = hist.what.contains("INCOMPLETE", true)
        val title = when {
            isIncomplete -> "🚫  MARKED INCOMPLETE"
            hist.stage.equals("Inquiry", true) -> "🚫  THIS NUMBER WAS REJECTED"
            else -> "🚫  REGISTRATION WAS CANCELLED"
        }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupClosedTitle).text = title
        val whoWhen = buildString {
            if (hist.whenText.isNotBlank()) append("On ").append(hist.whenText)
            if (hist.byName.isNotBlank()) { if (isNotEmpty()) append("  ·  "); append("by ").append(hist.byName) }
        }
        val tvWhen = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupClosedWhen)
        if (whoWhen.isBlank()) tvWhen.visibility = android.view.View.GONE else tvWhen.text = whoWhen
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupClosedWhere).text =
            "Old records are in " + hist.listName.ifBlank { "Follow-up history" } + ".\n" +
                "Continue will register this number as new."
        view.findViewById<android.widget.LinearLayout>(com.tkbiswas.pilesclinic.R.id.boxDupClosed).visibility =
            android.view.View.VISIBLE
        val tvDupName = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupName)
        val tvDupMobile = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupMobile)
        tvDupName.text = hist.name.ifBlank { "-" }
        tvDupMobile.text = mobile
        tvDupName.copyOnLongPress("Name", hist.name)
        tvDupMobile.copyOnLongPress("Mobile", mobile)
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupBranch).text = hist.branch.ifBlank { "-" }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupSection).text = "Follow-up History"
        UppercaseInputUtil.applyToAll(view)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(true).create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        val btnContinue = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupUpdate)
        btnContinue.visibility = android.view.View.VISIBLE
        btnContinue.text = "CONTINUE"
        btnContinue.setOnClickListener {
            dialog.dismiss()
            performSave(user, name, mobile, branch, fee)
        }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupView).setOnClickListener {
            startActivity(android.content.Intent(this, PatientTimelineActivity::class.java).putExtra("mobile", mobile))
        }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupClose).setOnClickListener { dialog.dismiss() }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    /** APPROVED UPDATE #3: Professional Duplicate Popup (same dialog_duplicate
     *  design used elsewhere) with three buttons: View / Update Existing / Cancel. */
    private fun showDuplicateDialog(
        duplicate: RegistrationRepository.DuplicatePatient, user: NativeUser,
        name: String, mobile: String, branch: String, fee: Double,
        // 🔒 খাতার সারি B133 — রেকর্ডটা আগে বাতিল করা ছিল কিনা।
        //    ফাঁকা (closed = false) হলে বাক্সটা হুবহু আগের মতোই দেখায়।
        closed: EnquiryRepository.ClosedInfo = EnquiryRepository.ClosedInfo(false)
    ) {
        val view = layoutInflater.inflate(com.tkbiswas.pilesclinic.R.layout.dialog_duplicate, null)
        // 🚨 খাতার সারি B133 (TK ফটো-প্রুফে পাশ, 29.07.2026 রাত ৮.৫০):
        // TK-এর কথা (রাত ৮.০০): *"সমস্ত staff-কে জিজ্ঞাসা করলাম, কেউ Restore
        // করেনি"* — তবু বাতিল করা রেকর্ড চালু তালিকায় ফিরে আসত, কারণ এই বাক্সটা
        // কোথাও বলত না যে রেকর্ডটা **আগে বাতিল করা হয়েছিল**।
        // ⛔ **শুধু এই লাল বাক্সটাই নতুন** — Name · Mobile · Branch · Section ও
        //    তিনটে বোতাম হুবহু আগের মতোই, একটাও বদলায়নি।
        // ⛔ **কোনো বোতাম আটকানো হয়নি** — স্টাফ জেনেবুঝে চাপলে আগের মতোই কাজ হবে।
        // ⛔ বন্ধ না থাকলে বাক্সটা লুকানোই থাকে (লেআউটে `visibility="gone"`)।
        if (closed.closed) {
            val isIncomplete = closed.what.contains("INCOMPLETE", true)
            val title = when {
                isIncomplete -> "\uD83D\uDEAB  MARKED INCOMPLETE"
                closed.stage.equals("Inquiry", true) -> "\uD83D\uDEAB  THIS NUMBER WAS REJECTED"
                else -> "\uD83D\uDEAB  REGISTRATION WAS CANCELLED"
            }
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupClosedTitle).text = title
            // কে ও কবে — যেটুকু সত্যিই জানা আছে শুধু সেটুকুই, আন্দাজে কিছু নয়।
            val whoWhen = buildString {
                if (closed.whenText.isNotBlank()) append("On ").append(closed.whenText)
                if (closed.byName.isNotBlank()) {
                    if (isNotEmpty()) append("  \u00B7  ")
                    append("by ").append(closed.byName)
                }
            }
            val tvWhen = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupClosedWhen)
            if (whoWhen.isBlank()) tvWhen.visibility = android.view.View.GONE else tvWhen.text = whoWhen
            view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupClosedWhere).text =
                "It is now in " + closed.listName + ".\n" +
                    "Update Existing will bring it back to the live list."
            view.findViewById<android.widget.LinearLayout>(com.tkbiswas.pilesclinic.R.id.boxDupClosed).visibility =
                android.view.View.VISIBLE
        }
        val tvDupName = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupName)
        val tvDupMobile = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupMobile)
        tvDupName.text = duplicate.name.ifBlank { "-" }
        tvDupMobile.text = mobile
        tvDupName.copyOnLongPress("Name", duplicate.name)
        tvDupMobile.copyOnLongPress("Mobile", mobile)
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupBranch).text = duplicate.branch.ifBlank { "-" }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupSection).text = "Patient"

        /* 🔵🔒 V516 (২২.০৮.২০২৬, TK-অনুমোদিত) — **Patient ID দেখানো।**
           TK-এর নির্দেশ: ডুপ্লিকেট ধরা পড়লে Name + **Patient ID** + Branch —
           তিনটেই দেখাতে হবে, যাতে স্টাফ নিশ্চিত হতে পারেন ইনি কে।
           ⛔ ঘরটা না থাকলে (পুরোনো রেকর্ড) সারিটা লুকানোই থাকে, বাক্স আগের মতোই। */
        val tvDupPid = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupPatientId)
        if (duplicate.patientId.isNotBlank()) {
            tvDupPid.text = duplicate.patientId
            tvDupPid.copyOnLongPress("Patient ID", duplicate.patientId)
            view.findViewById<android.widget.LinearLayout>(com.tkbiswas.pilesclinic.R.id.rowDupPatientId).visibility =
                android.view.View.VISIBLE
        }

        /* 🔵🔒 V516 — এই নম্বরে **একাধিক** রোগী থাকলে কাউকে লুকানো হয় না।
           উপরের ঘরগুলো প্রথমজনকে দেখায় (UPDATE EXISTING ঠিক তাঁকেই আপডেট করে),
           বাকিরা এখানে তালিকা হয়ে থাকে। ⛔ একজনই থাকলে লাইনটা লুকানো, বাক্স আগের মতোই। */
        if (duplicate.matches.size > 1) {
            val others = duplicate.matches.drop(1).joinToString("\n") { m ->
                "• " + m.name.ifBlank { "-" } +
                    (if (m.patientId.isNotBlank()) "  ·  " + m.patientId else "") +
                    (if (m.branch.isNotBlank()) "  ·  " + m.branch else "")
            }
            val tvOthers = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDupOthers)
            tvOthers.text = NoBengali.s("এই নম্বরে আরও ") + (duplicate.matches.size - 1) + NoBengali.s(" জন রোগী আছেন:\n") + others
            tvOthers.visibility = android.view.View.VISIBLE
        }
        UppercaseInputUtil.applyToAll(view)  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(true).create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        val btnUpdate = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupUpdate)
        btnUpdate.visibility = android.view.View.VISIBLE
        btnUpdate.setOnClickListener {
            dialog.dismiss()
            performSave(user, name, mobile, branch, fee, duplicate.patientId, duplicate.rowId)
        }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupView).setOnClickListener {
            // 🔴 TK-REPORTED (04.08.2026, একই ক্লাসের বাগ): dialog.dismiss()
            // সাথে সাথেই ডাকা হত, তাই Timeline থেকে Back করলে এই ডুপ্লিকেট-
            // পপ-আপ আর দেখা যেত না। এখন পপ-আপ খোলা থাকে।
            startActivity(android.content.Intent(this, PatientTimelineActivity::class.java).putExtra("mobile", mobile))
        }
        view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupClose).setOnClickListener { dialog.dismiss() }

        /* 🔵🔴🔒 V516 (২২.০৮.২০২৬, TK-অনুমোদিত) — **"Different Patient — Same Mobile"।**
           TK-এর কথা: এক পরিবারে স্বামী ও স্ত্রী দুজনেই রোগী, কিন্তু যোগাযোগের
           মোবাইল একটাই — দুজনকে সম্পূর্ণ আলাদা দুই রোগী হিসেবে রাখতে হবে।

           এখানে **তখনই** একটা নতুন অনন্য সারি-আইডি তৈরি হয়, আর সেটা সেভে যায়।
           ⛔ পুরোনো রোগীর সারিতে **এক অক্ষরও লেখা হয় না** — নতুন আইডি মানে
              নতুন সারি; তাঁর নাম · Patient ID · বিল · ইতিহাস সব অটুট।
           ⛔ নতুন রোগী **নিজের নতুন Official Patient ID** পান (`PatientIdGenerator`
              ব্রাঞ্চ+তারিখ ধরে সিরিয়াল দেয়, মোবাইলের সঙ্গে সম্পর্ক নেই)।
           ⛔ **Visit Fee আগের নিয়মেই কাটে** — নতুন রোগীর নিজের ফি (B455 অটুট,
              কারণ `existingRowId` ফাঁকাই থাকে)।
           ⛔ আইডিটা এখানে **একবারই** তৈরি হয়; নেট খারাপ হলে retry queue ওই
              সারিটাই আবার পাঠায়, তাই দুটো সারি তৈরি হওয়ার ভয় নেই।
           ⛔ স্টাফকে একবার নিশ্চিত করতে বলা হয় — ভুল করে চাপলে যেন
              অকারণে দ্বিতীয় রোগী তৈরি না হয়। */
        val btnDifferent = view.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.btnDupDifferent)
        btnDifferent.visibility = android.view.View.VISIBLE
        btnDifferent.setOnClickListener {
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "A new, separate patient?")   /* 🔤 V726 */)
                .setMessage(
                    "এই মোবাইল নম্বরটা ইতিমধ্যে " +
                        duplicate.name.ifBlank { "একজন রোগীর" } +
                        "-এর নামে আছে।\n\n" +
                        "\"" + name + "\" কি সত্যিই অন্য একজন রোগী (যেমন স্বামী / স্ত্রী / পরিবারের অন্য কেউ)?\n\n" +
                        "হ্যাঁ চাপলে সম্পূর্ণ নতুন একজন রোগী তৈরি হবে — পুরোনো রোগীর " +
                        "কোনো তথ্য বদলাবে না, আর নতুন রোগীর নিজের Visit Fee কাটবে।"
                )
                .setPositiveButton("Yes, Different Patient") { d, _ ->
                    d.dismiss()
                    dialog.dismiss()
                    performSave(
                        user, name, mobile, branch, fee,
                        existingPatientId = "", existingRowId = "",
                        forceNewPatientRowId = PatientModel.newRowIdForSameMobile(mobile)
                    )
                }
                .setNegativeButton("No") { d, _ -> d.dismiss() }
                .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
        }
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }

    private fun performSave(user: NativeUser, name: String, mobile: String, branch: String, fee: Double, existingPatientId: String = "", existingRowId: String = "", forceNewPatientRowId: String = "") {
        val draft = RegistrationDraft(
            date = selectedDate,
            name = name,
            mobileDigitsOnly = mobile,
            // 🔒 V235: Alternate/Enquiry নম্বর — টাইপ করা থাকলে সেটা; Primary-র সমান
            // হলে বাদ (duplicate নয়); নইলে Enquiry-র মূল নম্বর (যদি Primary থেকে আলাদা)।
            altMobileDigitsOnly = run {
                val typed = binding.etAltMobile.text.toString().filter { it.isDigit() }.takeLast(10)
                val alt = if (typed.length == 10) typed else ""
                when {
                    alt.isNotBlank() && alt != mobile -> alt
                    enquiryOriginMobile.length == 10 && enquiryOriginMobile != mobile -> enquiryOriginMobile
                    else -> ""
                }
            },
            branch = branch,
            age = binding.etAge.text.toString().trim(),
            sex = selectedSex,
            village = binding.etVillage.text.toString().trim(),
            po = binding.etPo.text.toString().trim(),
            ps = binding.etPs.text.toString().trim(),
            district = binding.etDistrict.text.toString().trim(),
            pin = binding.etPin.text.toString().trim(),
            occupation = binding.spOccupation.selectedItem?.toString()?.let { if (it == "Choose Occupation") "" else it } ?: "",
            refBy = binding.spRefBy.selectedItem?.toString() ?: "Self",
            diseases = diseaseChecks.filter { it.isChecked }.map { it.text.toString() },
            // Display text remains owner-approved; saved/printed English is the
            // professional wording requested by the owner.
            symptoms = symptomChecks.filter { it.isChecked }.map {
                when (it.text.toString()) {
                    "Massa Bara Hua" -> "Prolapsed Lump"
                    else -> it.text.toString()
                }
            },
            complaintNote = binding.etComplaintNote.text.toString().trim(),
            medicalHistory = medHistChecks.filter { it.isChecked }.map {
                when (it.text.toString()) {
                    "Previous Doctor Treatment" -> "Previous Medical Treatment"
                    "Previous Operation History" -> "Previous Surgical History"
                    else -> it.text.toString()
                }
            },
            durationNote = binding.etDurationNote.text.toString().trim().let { amount ->
                if (amount.isBlank()) "" else "$amount ${binding.spDurationUnit.selectedItem ?: "Days"}"
            },
            // The selected history and optional typed note travel together to
            // Doctor Check-up/print; neither can hide the other.
            prevTreatmentNote = listOf(
                medHistChecks.filter { it.isChecked }.map {
                    when (it.text.toString()) {
                        "Previous Doctor Treatment" -> "Previous Medical Treatment"
                        "Previous Operation History" -> "Previous Surgical History"
                        else -> it.text.toString()
                    }
                }.joinToString(", "),
                binding.etPrevTreatmentNote.text.toString().trim()
            ).filter { it.isNotBlank() }.joinToString(" | "),
            regFee = fee,
            payMode = selectedPayMode,
            photo = patientPhotoData ?: "",
            refDoctor = binding.etRefDoctorName.text.toString().trim(),
            refDoctorMobile = binding.etRefDoctorMobile.text.toString().filter { it.isDigit() }.takeLast(10),
            timeType = selectedTiming
        )
        setLoading(true)
        lifecycleScope.launch {
            try {
                val patientId = withContext(Dispatchers.IO) { repository.save(draft, user.mobile, existingPatientId, existingRowId, forceNewPatientRowId) }
                setLoading(false)
                if (patientId != null) {
                    // 🔒 TK-APPROVED (28.07.2026): সেভ হওয়ার পরে রোগীকে খবর
                    // পাঠানোর বাক্স। "পরে পাঠাব" চাপলেও পর্দা আগের মতোই বন্ধ
                    // হয়, তাই স্টাফের চলতি কাজ কখনো আটকায় না।
                    android.widget.Toast.makeText(
                        this@RegistrationActivity,
                        "Patient registered — ID: $patientId",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    // 🔒 B597/B598 (TK-অনুমোদিত, প্রুফ দেখিয়ে): বার্তা ও A4 রসিদ —
                    // দুটোর জন্য একই মান একবার হিসাব করে নিই (আন্দাজ নয়, draft থেকে)।
                    val a4AgeSex = listOf(draft.sex, draft.age).filter { it.isNotBlank() }.joinToString(" / ")
                    val a4Disease = draft.diseases.joinToString(", ")
                    val a4Address = listOf(
                        if (draft.village.isNotBlank()) "Vill: ${draft.village}" else "",
                        if (draft.po.isNotBlank()) "PO: ${draft.po}" else "",
                        if (draft.ps.isNotBlank()) "PS: ${draft.ps}" else "",
                        if (draft.district.isNotBlank()) "Dist: ${draft.district}" else "",
                        if (draft.pin.isNotBlank()) "PIN: ${draft.pin}" else ""
                    ).filter { it.isNotBlank() }.joinToString(", ")
                    /* 🔴🔒 V505 (TK-নির্দেশ ২১.০৮.২০২৬): তারিখ ও সময় এখন **আলাদা**
                       করে বার্তায় যায় — "DATE: 31/12/2026 TIME: 12.34Pm"।
                       আগে দুটো জোড়া লাগিয়ে (`31/12/2026 · 12.34 PM`) একটাই লেখা
                       পাঠানো হতো, তাই TK-এর চাওয়া ধরনে সাজানো যেত না।
                       ⛔ A4 কাগজে আগের জোড়া-লাগানো লেখাটাই যায় (`a4DateTime`) —
                          ছাপা এক অক্ষরও বদলায়নি। */
                    val a4DateOnly = try {
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US)   // 🔴🔒 V936 — এক ফরম্যাট
                            .format(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(draft.date)!!)
                    } catch (_: Throwable) { draft.date }
                    // এই মুহূর্তটাই রেজিস্ট্রেশনের আসল সময় (এখনই সেভ হচ্ছে)।
                    val a4TimeOnly = java.text.SimpleDateFormat("h.mm a", java.util.Locale.US)   // 🔴 V1158
                        .format(java.util.Date())
                        .let { if (it.length >= 2) it.dropLast(2) + it.takeLast(2).first().uppercase() + it.takeLast(1).lowercase() else it }
                    val a4DateTime = a4DateOnly + " · " + a4TimeOnly
                    // A4 রসিদের জন্য patient JSONObject — বিদ্যমান PrintMappersCloud.registration()
                    // ঠিক এই ঘরগুলোই পড়ে; নতুন কিছু বানাচ্ছি না, শুধু ওই স্ক্রিনে পৌঁছাচ্ছি।
                    val a4PatientJson = org.json.JSONObject().apply {
                        put("name", draft.name)
                        put("patientId", patientId)
                        put("mobile", draft.mobileDigitsOnly)
                        put("branch", draft.branch)
                        put("disease", a4Disease)
                        put("address", a4Address)
                        put("age", draft.age)
                        put("sex", draft.sex)
                        // 🔴🔒 V506 (TK-নির্দেশ): কাগজেও "DATE: 21/08/2026 TIME: 10.48Am"।
                        //    আগে এখানে কাঁচা `2026-08-21` যেত আর সময় যেতই না।
                        put("registrationDate", a4DateOnly)
                        put("time", a4TimeOnly)
                        put("refBy", draft.refBy)
                        // 🔵 R-A4 (09.08.2026): পূর্ণ Registration Form A4-এর জন্য বাড়তি ঘর
                        put("altMobile", draft.altMobileDigitsOnly)
                        put("occupation", draft.occupation)
                        put("complaint", draft.complaintNote.ifBlank { draft.symptoms.joinToString(", ") })
                        put("sinceWhen", draft.durationNote)
                        put("previousTreatment", draft.prevTreatmentNote)
                        put("refDoctor", draft.refDoctor)
                        put("refDoctorMobile", draft.refDoctorMobile)
                        put("regFee", draft.regFee)
                        put("regMode", draft.payMode)
                    }
                    PatientMessage.show(
                        activity = this@RegistrationActivity,
                        branch = draft.branch,
                        name = draft.name,
                        mobile = draft.mobileDigitsOnly,
                        patientId = patientId,
                        kind = PatientMessage.Kind.REGISTRATION,
                        // 🔒 খাতার সারি B92 (TK, 29.07.2026): রোগীর বার্তায়
                        // **রেজিস্ট্রেশন ফি কত ও কীভাবে দিলেন** তাও থাকবে।
                        // ⛔ ফরমে যে ফি ও যে মোড বাছা হয়েছে ঠিক সেটাই —
                        //    নতুন কোনো হিসাব করা হয়নি।
                        //
                        // 🚨 **সবচেয়ে জরুরি পাহারা (যাচাই করতে গিয়ে ধরা পড়েছে):**
                        // এই ফাংশনটা \"Update Existing\"-এর সময়ও চলে, আর তখন
                        // **নতুন কোনো ফি নেওয়া হয় না** (খাতার সারি B88 — দুবার
                        // চার্জ ঠেকাতে)। তাই তখন ফি-র লাইনটা বার্তায় গেলে
                        // রোগীকে **ভুল তথ্য** যেত। এখন ফি-র কথা তখনই যায় যখন
                        // সত্যিই নতুন রেজিস্ট্রেশন ও সত্যিই ফি নেওয়া হয়েছে।
                        regFee = if (existingRowId.isBlank()) fee else 0.0,
                        regFeeMode = if (existingRowId.isBlank()) selectedPayMode else "",
                        // 🔒 B597 (TK-অনুমোদিত): রেজিস্ট্রেশন বার্তায় স্টাফের ভরা সম্পূর্ণ
                        // বিবরণ + তারিখের পাশে সময় (6.30 PM)। পেশা যাবে না। মোবাইল আগে থেকেই যায়।
                        dateText = a4DateOnly,
                        timeText = a4TimeOnly,
                        ageSex = a4AgeSex,
                        disease = a4Disease,
                        address = a4Address,
                        // 🔒 B598 (TK-অনুমোদিত): বার্তা-বাক্স থেকেই A4 রসিদ (বিদ্যমান PrintPreviewActivity রি-ইউজ)।
                        // 🔵 খাতার সারি (TK-নির্দেশ, 09.09.2026): A4 প্রিন্ট **নতুন + পুরনো** দুই রোগীতেই দেখাবে
                        //    (আগে শুধু নতুনে দেখাত)। ⛔ ফি-র লাইন পুরনো রোগীতে আগের মতোই বাদ (regFee/regFeeMode
                        //    উপরে existingRowId দিয়ে গেটেড; a4PatientJson-এ ফি নেই, তাই A4-তে ফি দেখায় না) —
                        //    রোগীকে ভুল তথ্য যাবে না, শুধু রেজিস্ট্রেশন-বিবরণ A4 প্রিন্ট করা যাবে।
                        a4Patient = a4PatientJson
                    ) {
                        // 🔴🟢 খাতার সারি B430 (TK-নির্দেশ, 05.08.2026 — "Save-এর
                        // পরে ফর্ম বন্ধ না হয়ে একই পর্দায় ঘরগুলো খালি হয়ে
                        // যাক")। শুধু **নতুন** রেজিস্ট্রেশনে (existingRowId
                        // ফাঁকা) — এটাই "পরের রোগী সাথে সাথে লেখা যাবে" ফ্লো।
                        // "Update Existing" (existingRowId থাকলে — একজন
                        // নির্দিষ্ট রোগীর তথ্য এডিট করা) আগের মতোই finish() —
                        // ওখানে "পরের রোগী" ধারণাটা প্রযোজ্যই না।
                        if (existingRowId.isBlank()) clearForm() else finish()
                    }
                } else {
                    // 🚨 খাতার সারি B88 (TK, 29.07.2026): রেজিস্ট্রেশনের সঙ্গে
                    // **ভিজিট ফি-র সারিটাও** ক্লাউডে যেতে হয়। ধীর লাইনে ওটা
                    // ফোনে অপেক্ষায় থেকে যায় — আর তখন **নতুন APK বসালে বা অ্যাপের
                    // ডেটা মুছলে ওই অপেক্ষমাণ সারিটা চিরতরে হারিয়ে যায়**; রোগী
                    // ক্লাউডে ওঠে কিন্তু ফি-র সারি ওঠে না, তাই পরে তিনি
                    // \"Visit Fee Missing\"-এ দেখান (TK-এর ১৪.০৭ ও ২৫.০৭-এর নামগুলো
                    // ঠিক এভাবেই তৈরি হয়েছিল)।
                    // ⛔ কিছু আটকানো হয়নি, কোনো ডিজাইন বদলায়নি — শুধু বার্তাটা
                    //    স্পষ্ট করা হলো, যাতে স্টাফ জানেন কাজ এখনো বাকি আছে।
                    toastAndFinish(
                        "Saved — Visit Fee is still waiting to upload. " +
                        "Keep the app installed and open it once the network is back."
                    )
                }
            } catch (e: Exception) {
                // CRASH-SAFETY FIX (TK-reported, 2026-07-16): this save path
                // had no error handling -- a problem here could crash the
                // whole app instead of just failing this one save attempt.
                setLoading(false)
                android.widget.Toast.makeText(this@RegistrationActivity, "Could not save — check connection and try again", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toastAndFinish(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        finish()
    }

    // 🔴🟢 খাতার সারি B430 — Save-এর পরে (শুধু নতুন রেজিস্ট্রেশনে) ফর্ম খালি
    // করার ফাংশন। ⛔ **Branch স্পিনার ইচ্ছাকৃতভাবে ছোঁয়া হয়নি** — TK-এর
    // লক করা নিয়ম (28.07.2026): Staff/Doctor-এর ব্রাঞ্চ অটো-ভরা ও 3-tap
    // লকড থাকে, প্রতিবার নতুন করে বাছতে হয় না; Master/Field-এর জন্য
    // এমনিতেই প্রতিবার নিজে বাছতে হয় (placeholder-এ ফেরে না, যা তাঁরা
    // শেষ বেছেছেন তাই থাকে — একই ব্রাঞ্চের একাধিক রোগী থাকলে সুবিধাজনক)।
    // TK চাইলে ব্রাঞ্চও রিসেট করা যাবে, কিন্তু সেটা লকড নিয়ম ভাঙবে তাই
    // এখন ছোঁয়া হলো না — TK-কে জানিয়ে রাখা হলো।
    private fun clearForm() {
        binding.etName.setText("")
        binding.etMobile.setText("")
        binding.etAltMobile.setText("")
        binding.etAge.setText("")
        binding.etVillage.setText("")
        binding.etPo.setText("")
        binding.etPs.setText("")
        binding.etDistrict.setText("")
        binding.etPin.setText("")
        binding.etFee.setText("")
        binding.etComplaintNote.setText("")
        binding.etDurationNote.setText("")
        binding.spDurationUnit.setSelection(0)
        binding.etPrevTreatmentNote.setText("")
        binding.etRefDoctorName.setText("")
        binding.etRefDoctorMobile.setText("")
        binding.llRefDoctor.visibility = View.GONE
        // 🟢 V895 — ফর্ম ফাঁকা হলে সাজেশনের তালিকাও বন্ধ।
        binding.llRmpSuggest.removeAllViews()
        binding.llRmpSuggest.visibility = View.GONE

        diseaseChecks.forEach { it.isChecked = false }
        symptomChecks.forEach { it.isChecked = false }
        medHistChecks.forEach { it.isChecked = false }

        selectedSex = "Male"
        setupSexButtons()
        selectedPayMode = "CASH"
        setupPayButtons()
        selectedTiming = "Official Time"
        setupTimingButtons()
        binding.spOccupation.setSelection(0)
        binding.spRefBy.setSelection(0)

        patientPhotoData = null
        binding.imgPatientPhoto.setImageDrawable(null)
        binding.imgPatientPhoto.visibility = View.GONE
        binding.btnPatientPhoto.text = "📷 Add Patient Photo"

        // পরের রোগীর নম্বরে যেন আগের রোগীর enquiry-autofill/duplicate-check
        // ইতিহাস ভুল করে জড়িয়ে না যায়।
        enquiryOriginMobile = ""
        lastAutofilledMobile = ""
        lastDupCheckedMobile = ""

        selectedDate = if (RegDateRule.MUST_PICK) "" else PatientIdGenerator.todayIso()
        showRegDate()
        binding.etName.requestFocus()
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
