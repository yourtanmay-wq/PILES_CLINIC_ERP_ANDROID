package com.tkbiswas.pilesclinic.clinical

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.Toast
import android.widget.ViewFlipper
import com.tkbiswas.pilesclinic.native.s
import com.tkbiswas.pilesclinic.native.NoBengali
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.tkbiswas.pilesclinic.R
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.BackgroundWork
import com.tkbiswas.pilesclinic.native.PhotoUtils
import com.tkbiswas.pilesclinic.native.SupabaseClient
import com.tkbiswas.pilesclinic.native.SpinnerPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tkbiswas.pilesclinic.native.UppercaseInputUtil

/**
 * Doctor Check-up — native rebuild of the WebView doctorCheck() form, faithful
 * to its 6 sections: Basic History, Previous Treatment History, Clinical
 * Findings (Visual + DRE + Grade + Investigation + Other), Counselling,
 * Financial Discussion, and Patient Decision.
 */
class DoctorCheckupActivity : AppCompatActivity() {

    // 🟢🔒 V676 (২৫.০৮.২০২৬, TK-নির্দেশ) — আজকের নিজের Doctor Checkup এডিট
    // করার প্রবেশদ্বার। PatientTimelineActivity এই দুটো extra পাঠালে Save
    // চাপলে নতুন সারি না বানিয়ে ঠিক এই id-তেই upsert হয় (নিচে populate() +
    // Save-এ ব্যবহার হয়)। ⛔ ফাঁকা থাকলে (স্বাভাবিক নতুন Checkup) আচরণ
    // অবিকল আগের মতোই।
    companion object {
        const val EXTRA_EDIT_MEDICAL_ID = "edit_medical_id"
        const val EXTRA_EDIT_MEDICAL_JSON = "edit_medical_json"
    }
    private var editingMedicalId: String = ""

    private lateinit var etComplaint: EditText
    private lateinit var etDuration: EditText
    // V455 (TK-নির্দেশ ১৮.০৮.২০২৬): spAcuteChronic ঘর বাদ (UI থেকে সরানো)।
    private lateinit var spOccupation: Spinner
    private lateinit var etPrevTreatment: EditText
    // 🔵 B622 (11.08.2026, TK-নির্দেশ): etPrevResult · etPrevCost · etTreatmentDuration ঘর বাদ।
    // V455 (TK-নির্দেশ ১৮.০৮.২০২৬): etVisualOther · etDreOther · etOtherFindings ঘর বাদ।
    private lateinit var spGrade: Spinner
    private lateinit var etOnProbing: EditText
    // 🆕 TK-নির্দেশ (04.08.2026): Treatment Plan — 6টা Tick-বক্স, ৩টেতে
    // এডিটেবল টাকা।
    private lateinit var cbTxPerPiles: CheckBox
    private lateinit var etAmtPerPiles: EditText
    private lateinit var cbTxFistulaInch: CheckBox
    private lateinit var etAmtFistulaInch: EditText
    private lateinit var cbTxMachine: CheckBox
    private lateinit var cbTxKsharSutra: CheckBox
    private lateinit var etAmtKsharSutra: EditText
    private lateinit var cbTxLis: CheckBox
    private lateinit var cbTxInjection: CheckBox
    private lateinit var etCounselling: EditText
    private lateinit var etEstimatedCost: EditText
    /* 💰🔒 V971 (০২.০৯.২০২৬, TK-অনুমোদিত) — এস্টিমেটের ভাঙা হিসাব। ফাঁকা
       থাকলে পর্দা হুবহু আগের মতোই চলে (পুরনো চেকআপে কিচ্ছু ভাঙে না)। */
    private var estimateSheet: EstimateModel.Sheet = EstimateModel.Sheet()
    /* 🟢🔒 V589 (২৩.০৮.২০২৬, TK-নির্দেশ) — "Estimated Recovery Time" ঘরটা পর্দা
       থেকে উঠে গেছে (একই কথা ভাগ ৩-এ আছে)। কিন্তু **পুরনো রোগীর সেভ করা লেখা
       যেন কোনোভাবেই মুছে না যায়** — তাই রেকর্ড খোলার সময় ওখানে যা ছিল তা এই
       ঘরে রাখা হয়, আর আবার Save করলে হুবহু সেটাই ফিরে বসে।
       ⛔ নতুন রোগীর ক্ষেত্রে এটা ফাঁকাই থাকে — কোনো আন্দাজে কিছু বসে না। */
    private var keptRecoveryTime: String = ""
    // 🔵 B622 (11.08.2026, TK-নির্দেশ): "Advance Payment to be Done" (etAdvanceDiscussed) ঘর বাদ।
    // V455 (TK-নির্দেশ ১৮.০৮.২০২৬): spPatientDecision · etDecisionRemark · etDocuments ঘর বাদ।

    private lateinit var ivBeforePhoto: ImageView
    private lateinit var ivDuringPhoto: ImageView
    private lateinit var ivAfterPhoto: ImageView
    private var beforePhotoData = ""
    private var duringPhotoData = ""
    private var afterPhotoData = ""
    private var cameraPhotoUri: Uri? = null
    private var currentPhotoTarget = 0  // 0=before, 1=during, 2=after
    /* 🔵 V573 — ৩ মানে "রোগের ছবির তালিকায় যোগ করুন" (চেক-আপের নিজের ছবি নয়)।
       ⛔ ছবি নেওয়ার পথটা আগের সেই একই (`showPhotoDialog` → ক্যামেরা/গ্যালারি →
          `PhotoUtils.encodeResized`) — নতুন কিছু বানানো হয়নি। */
    private val ANAT_PHOTO_TARGET = 3

    private val visualChecks = mutableListOf<CheckBox>()
    // 🔵 V556 (TK: "B রাখুন") — V455-এ সরানো DRE-র টিকগুলো ফিরে এল
    private val dreChecks = mutableListOf<CheckBox>()
    // V455 — dreChecks/dreOptions/dreGroup আর ব্যবহার হয় না (B সেকশন UI থেকে বাদ),
    // কিন্তু বাকি বিল্ড-চেইন (buildChecks কল) থেকে সরানো হলো নিচে; পুরনো সেভ
    // করা DRE মান (dreChecks-ভিত্তিক) ছোঁয়া হয়নি, শুধু নতুন টিক করার অপশন নেই।
    private val investigationChecks = mutableListOf<CheckBox>()

    // 🔴🟢 খাতার সারি B436 (TK-নির্দেশ, 05.08.2026, ছবিসহ) — Visual
    // Examination-এ "Internal Piles · অভ্যন্তরীণ অর্শ" নতুন যোগ হলো। ⛔
    // DRE সেকশনের নিজস্ব "Internal Piles" (dreOptions, নিচে) সম্পূর্ণ
    // আলাদা, আলাদা সেভ-হওয়া মান — এটা তার সাথে গুলিয়ে যায় না, দুটোই
    // স্বাধীনভাবে টিক করা যাবে।
    private val visualOptions = listOf("External Piles", "Internal Piles", "Fissure", "Fistula Opening", "Bleeding", "Swelling")
    // 🔒 B549 (08.08.2026, TK-নির্দেশ — স্ক্রিনশটে গোল দাগ দিয়ে: "একই লেখা দুইবার
    // কেন? নিচেরটা থেকে সরিয়ে দিন")। TK-কে আগে বোঝানো হয় Visual ও DRE আলাদা
    // পরীক্ষা, তবু TK নিচের (DRE) সেকশন থেকে ডুপ্লিকেট দুটো — "Internal Piles"
    // ও "Fissure" — সরাতে বলেন। তাই DRE-তে এখন শুধু Tenderness ও Fistula।
    // ⛔ Visual-এর "Internal Piles"/"Fissure" অপরিবর্তিত (উপরের তালিকা)। dreBn/
    // dreIcons-এ ওই দুটোর ম্যাপ-এন্ট্রি রইল (অব্যবহৃত, নিরীহ)। পুরনো কোনো
    // রেকর্ডে DRE-তে ওই মান সেভ থাকলে সেটা রিপোর্টে/ডিসপ্লেতে আগের মতোই দেখাবে
    // (ডিসপ্লে সরাসরি সেভ-করা লেখা থেকে হয়) — শুধু নতুন করে ওই দুটো আর টিক
    // করার অপশন থাকবে না।
    private val dreOptions = listOf("Tenderness", "Fistula")
    private val investigationOptions = listOf("MRI", "USG", "Colonoscopy", "Lab Reports")
    // 🆕 (03.08.2026, TK-অনুমোদিত মকআপ অনুযায়ী) — চেকবক্সের পাশে বাংলা দেখানোর
    // জন্য শুধু ডিসপ্লে-লেবেল ম্যাপ। ⛔ আসল সেভ-হওয়া মান (cb.tag, নিচে
    // buildChecks-এ) এখনো হুবহু আগের ইংরেজি শব্দ — পুরনো রেকর্ডে সেভ করা মান
    // এই বদলে এক অক্ষরও ভাঙে না।
    private val visualBn = mapOf(
        "External Piles" to "বাহ্যিক অর্শ", "Internal Piles" to "অভ্যন্তরীণ অর্শ", "Fissure" to "ফিসার", "Fistula Opening" to "ফিস্টুলার মুখ",
        "Bleeding" to "রক্ত পড়ে", "Swelling" to "ফোলাভাব"
    )
    private val dreBn = mapOf(
        "Tenderness" to "কোমলতা/ব্যথা", "Internal Piles" to "অভ্যন্তরীণ অর্শ", "Fissure" to "ফিসার", "Fistula" to "ফিস্টুলা"
    )
    private val investigationBn = mapOf(
        "MRI" to "এমআরআই", "USG" to "আল্ট্রাসাউন্ড", "Colonoscopy" to "কোলোনোস্কোপি", "Lab Reports" to "ল্যাব রিপোর্ট"
    )
    // TK APPROVED (2026-07-15): one icon + one accent colour per checklist
    // group, for a professional colourful look. Text/values/save logic are
    // completely unchanged -- only how each row is drawn.
    private val visualIcons = mapOf(
        "External Piles" to "🔴", "Internal Piles" to "🟠", "Fissure" to "➖", "Fistula Opening" to "🕳️",
        "Bleeding" to "🩸", "Swelling" to "🔵"
    )
    private val dreIcons = mapOf(
        "Tenderness" to "🤚", "Internal Piles" to "🔴", "Fissure" to "➖", "Fistula" to "🕳️"
    )
    private val investigationIcons = mapOf(
        "MRI" to "🧲", "USG" to "📡", "Colonoscopy" to "🔬", "Lab Reports" to "📋"
    )
    private val gradeOptions = listOf("", "Grade I", "Grade II", "Grade III", "Grade IV")
    // 🆕 (03.08.2026, TK-অনুমোদিত মকআপ, "ওকে লক") — শুধু ডিসপ্লে-লেবেল, আসল
    // সেভ-হওয়া মান (gradeOptions/decisionOptions, উপরে/নিচে) অপরিবর্তিত।
    // ⛔ রোমান সংখ্যা (I/II/III/IV) অপরিবর্তিত রাখা হয়েছে — বাংলা সংখ্যা
    // ব্যবহার নিষেধ (অ্যাপের স্থায়ী নিয়ম ৯.১১)।
    private val gradeBn = mapOf(
        "Grade I" to "গ্রেড I", "Grade II" to "গ্রেড II", "Grade III" to "গ্রেড III", "Grade IV" to "গ্রেড IV"
    )
    private val decisionBn = mapOf(
        "Agree for Treatment" to "চিকিৎসায় রাজি", "Not Agree" to "রাজি নয়", "Will Think" to "ভেবে দেখব",
        "Family Discussion" to "পরিবারের সাথে আলোচনা", "Financial Problem" to "আর্থিক সমস্যা", "Other" to "অন্যান্য"
    )
    private val decisionOptions = listOf(
        "Agree for Treatment", "Not Agree", "Will Think",
        "Family Discussion", "Financial Problem", "Other"
    )
    // 🆕 TK-নির্দেশ (04.08.2026): হঠাৎ হয় নাকি ধীরে ধীরে বাড়ে।
    private val acuteChronicOptions = listOf("", "Acute Onset (Sudden)", "Chronic Onset (Gradual)")
    private val acuteChronicBn = mapOf(
        "Acute Onset (Sudden)" to "হঠাৎ শুরু হয়েছে", "Chronic Onset (Gradual)" to "ধীরে ধীরে বেড়েছে"
    )
    // 🆕 TK-নির্দেশ (04.08.2026): Registration-এর spOccupation-এর হুবহু একই
    // তালিকা (RegistrationActivity.kt) — প্রজেক্টে দুই জায়গায় দুই রকম
    // পেশার তালিকা যেন না হয়ে যায়।
    private val occupationOptions = listOf(
        "Choose Occupation", "Farmer", "Housewife", "Business", "Service", "Student", "Labour", "Retired", "Others"
    )

    /* 🔴🔒 V786 (২৮.০৮.২০২৬, TK-রিপোর্ট: হেডারে "Patient / - / -") —
       ফোনে কল এলে বা মেমরি কম পড়লে Android অ্যাপের প্রসেস বন্ধ করে দেয়;
       পরে এই পর্দাটা আবার খোলে, কিন্তু মেমরির `RoleSession` ততক্ষণে ফাঁকা।
       তাই রোগীর পরিচয় এই পর্দার নিজের Bundle-এও রাখা হয় — Bundle প্রসেস
       মরলেও বাঁচে, আর V721-এর ৩০ মিনিটের সীমাও এতে লাগে না।
       ⛔ মেমরিতে রোগী থাকলে `restoreFrom()` কিচ্ছু করে না (RoleSession.kt)। */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        RoleSession.saveTo(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoleSession.restoreFrom(savedInstanceState)   // 🔴🔒 V786 — কল/মেমরির কারণে হারানো রোগী ফেরানো
        setContentView(R.layout.activity_doctor_checkup)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically

        // 🟢🔒 V676 — এই পর্দা এডিট-মোডে খোলা হয়েছে কিনা তা এখানেই ধরা হয়,
        // নিচে ফর্ম বসানোর পরে এই ঘরটাই populate()/Save-কে জানায়।
        editingMedicalId = intent.getStringExtra(EXTRA_EDIT_MEDICAL_ID).orEmpty()

        // 🔴🎨🔒 TK-নির্দেশ (06.08.2026): টুলবার/হেডার সম্পূর্ণ বাদ (XML
        // থেকেও সরানো হয়েছে) — তাই এই তিন লাইন (toolbar খোঁজা/
        // setSupportActionBar/navigation-click) আর দরকার নেই, সরানো হলো।
        // ফোনের নিজের Back (গেসচার/বোতাম) দিয়েই এই পর্দা থেকে বেরোনো যায়,
        // এখানে আলাদা কোনো onBackPressed override নেই।

        etComplaint = findViewById(R.id.etComplaint)
        etDuration = findViewById(R.id.etDuration)
        // V455 (18.08.2026): spAcuteChronic findViewById বাদ (ঘরটাই নেই)।
        spOccupation = findViewById(R.id.spOccupation)
        etPrevTreatment = findViewById(R.id.etPrevTreatment)
        // V455 (18.08.2026): etVisualOther · etDreOther · etOtherFindings findViewById বাদ।
        spGrade = findViewById(R.id.spGrade)
        etOnProbing = findViewById(R.id.etOnProbing)
        cbTxPerPiles = findViewById(R.id.cbTxPerPiles)
        etAmtPerPiles = findViewById(R.id.etAmtPerPiles)
        cbTxFistulaInch = findViewById(R.id.cbTxFistulaInch)
        etAmtFistulaInch = findViewById(R.id.etAmtFistulaInch)
        cbTxMachine = findViewById(R.id.cbTxMachine)
        cbTxKsharSutra = findViewById(R.id.cbTxKsharSutra)
        etAmtKsharSutra = findViewById(R.id.etAmtKsharSutra)
        cbTxLis = findViewById(R.id.cbTxLis)
        cbTxInjection = findViewById(R.id.cbTxInjection)
        // 🔒 TK-এর নিয়ম (৯.১৪, Kishanganj no-Bengali): visual/dre/investigation
        // checkbox-এর মতোই — .tag-এ স্থির ইংরেজি মান বসানো হলো, যাতে সেভ হওয়া
        // মান (checkedText() যেটা .tag পড়ে) Kishanganj/অন্য সব স্টাফের ফোনে
        // সবসময় হুবহু একই থাকে, বাংলা লেখা NoBengali.kt-এ মুছে গেলেও।
        cbTxPerPiles.tag = "Per Piles"
        /* 🔵🔒 V541 (২২.০৮.২০২৬, TK-নির্দেশ: *"Fistula Per CM (centimetre) করুন"*)
           ⛔ **পুরোনো রেকর্ড ভাঙবে না:** এতদিন সেভ হত `"Fistula Per Inch"`।
              নতুন সেভ হবে `"Fistula Per CM"`, আর ফেরত পড়ার সময় **দুটোই**
              মেনে নেওয়া হয় (নিচে `FISTULA_TAGS`) — তাই আগের প্রতিটা চেকআপে
              টিকটা আগের মতোই বসে থাকবে।
           ⛔ ঘরের নাম (`amtFistulaPerInch`) **বদলানো হয়নি** — ওটা ভিতরের নাম,
              বদলালে ওয়েব ও পুরোনো জমা তথ্যের সাথে মিল নষ্ট হত। */
        cbTxFistulaInch.tag = FISTULA_TAG_NOW
        cbTxMachine.tag = "Machine Treatment"
        cbTxKsharSutra.tag = "Kshar Sutra"
        cbTxLis.tag = "LIS Treatment"
        cbTxInjection.tag = "Injection (Vaccination) Treatment"
        etCounselling = findViewById(R.id.etCounselling)
        etEstimatedCost = findViewById(R.id.etEstimatedCost)
        wireEstimateBuilder()   // 💰 V971
        // 🟢 V589: etRecoveryTime findViewById বাদ (ঘরটাই আর নেই)।
        // V455 (18.08.2026): spPatientDecision · etDecisionRemark · etDocuments findViewById বাদ।
        wireDoctorReminder()   // 🟢🔒 V656 — Doctor Note & Reminder

        bindPatientHeader()

        ivBeforePhoto = findViewById(R.id.ivBeforePhoto)
        ivDuringPhoto = findViewById(R.id.ivDuringPhoto)
        ivAfterPhoto = findViewById(R.id.ivAfterPhoto)
        findViewById<MaterialButton>(R.id.btnBeforePhoto).setOnClickListener { showPhotoDialog(0) }
        findViewById<MaterialButton>(R.id.btnDuringPhoto).setOnClickListener { showPhotoDialog(1) }
        findViewById<MaterialButton>(R.id.btnAfterPhoto).setOnClickListener { showPhotoDialog(2) }

        buildSymptomRows()   // 🔵 V554
        wireSymptomFold()    // 🔵 V574 — চাপ দিলে ভাগ ২ খোলে
        buildHistoryDetailRows()   // 🔵 V555
        buildLifestyleRows()       // 🔵 V556
        wireLifeFold()             // 🔵 V578 — চাপ দিলে ভাগ ৪ খোলে
        wirePhotoFold()            // 🟢 V600 — চাপ দিলে ভাগ ৫ (Photo & Video) খোলে
        buildAnatomyBoard()        // 🔵 V558 — রোগের ছবি
        /* 🔵 V573 — ছবির তালিকাটা (যোগ/বিয়োগ) পিছনে একবার এনে নেওয়া।
           ⛔ ১৫ মিনিটে একবারের বেশি নয়, আর না এলেও কিছু আটকায় না —
              ফোনে জমা থাকা শেষ তালিকাটাই চলতে থাকে। */
        BackgroundWork.run {
            if (AnatomyPictureRepository.pull(this)) runOnUiThread {
                val strip = findViewById<android.widget.LinearLayout>(R.id.anatomyStrip)
                val v = anatomyView
                if (strip != null && v != null) buildAnatomyStrip(strip, v)
            }
        }
        // 🔵 V556: B. আঙুল দিয়ে দেখে (DRE) — পুরোনো সেই একই তালিকা ও একই রং
        buildChecks(findViewById(R.id.dreGroup), dreOptions, dreChecks, dreIcons, "#0B4F2A", dreBn)
        buildChecks(findViewById(R.id.visualGroup), visualOptions, visualChecks, visualIcons, "#D64545", visualBn)
        /* 🔵 V539: Internal Piles-এ চাপ দিলেই Grade বাছার তালিকা। ⛔ বাকি
           চেকবক্সগুলো এক অক্ষরও বদলায়নি। */
        wireClinicalFold()   // 🟢 V703 — ধাপ ২ বন্ধ অবস্থায় শুরু হয়
        wireTtdFold()        // 🔴 V938 — TODAY'S TREATMENT DONE (NEXT PLAN-এর ঠিক উপরে)
        wireNvpFold()        // 🟢 V866 — NEXT VISIT PLAN বন্ধ অবস্থায় শুরু হয়
        wireMainFolds()      // 🟢 V886 — ধাপ ১ · ৩ · ৪-ও বন্ধ অবস্থায় শুরু হয়
        /* 🟢 V1119 — ভরা-ও-বন্ধ ধাপগুলোর জন্য নিচের একটাই "SAVED" বাক্স।
           ⛔ `post`-এ, যাতে পর্দার সব অংশ বসে যাওয়ার পরেই সাজানোটা হয়। */
        wireSavedGroup()
        findViewById<android.view.View>(R.id.secSavedGroup)?.post { refreshSavedGroup() }
        internalPilesBox()?.setOnClickListener { askInternalGrade() }
        /* 🔵 V540: Grade বাছা হলে চেকবক্স নিজে থেকেই টিক পড়ে ও পাশে Grade দেখায়।
           ⛔ শোনার কাজটা **একবারই** বসে (পপ-আপ খোলার সময় বারবার নয়)। */
        spGrade.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: android.view.View?, p2: Int, p3: Long) {
                internalPilesBox()?.isChecked = gradeOptions.getOrElse(p2) { "" }.isNotBlank()
                refreshInternalGradeLabel()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        })
        refreshInternalGradeLabel()
        // V455 (18.08.2026): dreGroup buildChecks বাদ — পুরো "B. DRE" সেকশন UI-তে নেই।
        // 🔴 TK-নির্দেশ (05.08.2026): E. Investigations-এর চারটে চেকবক্সে
        // (MRI/USG/Colonoscopy/Lab Reports) এখন শুধু ইংরেজি — বাংলা বাদ,
        // তাই এখানে investigationBn পাঠানো হচ্ছে না (buildChecks-এর ডিফল্ট
        // emptyMap())। ⛔ investigationBn ম্যাপটা মোছা হয়নি (ভবিষ্যতে
        // লাগলে ফেরানো যাবে), শুধু এই কলে ব্যবহার বন্ধ।
        // 🔴 V501: Investigation এখন Quick Action-এর নিচে ও তাদের সমান মাপে।
        buildChecks(findViewById(R.id.investigationGroup), investigationOptions,
            investigationChecks, investigationIcons, "#6F42C1", compact = true)
        spGrade.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            gradeOptions.map { it.ifBlank { "— Select Grade —" } }.let { list ->
                list.mapIndexed { i, s -> gradeBn[gradeOptions[i]]?.let { "$s · $it" } ?: s }
            })
        // 🔴🎨🔒 TK-নির্দেশ (05.08.2026, "প্রফেশনাল লোক খুঁজে পাচ্ছিলাম না...
        // ডানে ভেঙে যায়") — Android-এর নিজের নেটিভ ড্রপডাউন লম্বা ইংরেজি-
        // বাংলা মেশানো লেখা ডান দিকে কেটে ("...") দেখাত। spOccupation-এ
        // আগে থেকেই TK-অনুমোদিত (28.07.2026, ফটো-প্রুফ ২) সমাধান আছে —
        // `SpinnerPicker` (কেন্দ্রে খোলা প্রিমিয়াম পপ-আপ, পুরো লেখা দেখা
        // যায়, কাটে না) — এই তিনটে Spinner-এও (Grade/Patient Decision/
        // Acute-Chronic) হুবহু একই প্রমাণিত ব্যবস্থা বসানো হলো। ⛔ Spinner/
        // adapter/positions/সেভ-হওয়া মান (selectedItemPosition-ভিত্তিক)
        // এক অক্ষরও বদলায়নি — শুধু চাপলে কোন পপ-আপ খোলে সেটাই বদলাল।
        SpinnerPicker.attach(spGrade, "SELECT GRADE", hidePlaceholder = true)
        // V455 (18.08.2026): spPatientDecision · spAcuteChronic adapter/SpinnerPicker সেটআপ বাদ (ঘর নেই)।
        // 🔴🔒 V501 (TK-নির্দেশ ২১.০৮.২০২৬, ফটো-প্রুফ অনুমোদিত) — Occupation-এর
        //    বাক্স বাকি ঘরগুলোর সমান। আগে Android-এর `simple_spinner_dropdown_item`
        //    ব্যবহার হতো, যার ভিতরে ≈৪৮dp বাধ্যতামূলক উচ্চতা বসানো — তাই বাক্সটা
        //    প্রায় দ্বিগুণ লম্বা দেখাত। এখন প্রকল্পের নিজের ছোট item
        //    (`item_docnote_spinner`, `item_branch_spinner`-এর প্রমাণিত ধাঁচ)।
        //    ⛔ বাছাইয়ের তালিকা · মান · সেভ — কিছুই বদলায়নি।
        /* 🔵 V557: রোগের তালিকা রেজিস্ট্রেশনের হুবহু একই, আর সময়ের একক
           সেই একই তিনটে — দুটোই প্রজেক্টের নিজের প্রিমিয়াম পিকারে খোলে। */
        /* 🔵🔒 V946 (০১.০৯.২০২৬, TK-নির্দেশ — "এই রোগীর PILES ও FISTULA দুটো রোগই
           আছে, কিন্তু চুস করা যাচ্ছে না কেন?", ফটো-প্রুফ পাশ করা): আগে এক-বাছাইয়ের
           Spinner ছিল, তাই একটার বেশি রোগ নেওয়া যেত না। এখন রেজিস্ট্রেশনের হুবহু
           একই চিপ (`RegistrationActivity.makeChip`-এর ধাঁচ ও একই রং-সিলেক্টর)।
           ⛔ রোগের তালিকা এক অক্ষরও বদলায়নি · সেভের ঘর আগের মতোই লেখা-ধরনের,
              একাধিক হলে "Piles, Fistula" এভাবে কমা দিয়ে বসে (রেজিস্ট্রেশনের মতোই)। */
        buildDiseaseChips()
        val spTimeUnit = findViewById<android.widget.Spinner>(R.id.spTimeAskedUnit)
        spTimeUnit.adapter = ArrayAdapter(this, R.layout.item_docnote_spinner, CounselModel.UNITS)
        SpinnerPicker.attach(spTimeUnit, "কতদিন সময় চাওয়া হল?")

        spOccupation.adapter = ArrayAdapter(this, R.layout.item_docnote_spinner, occupationOptions)
        SpinnerPicker.attach(spOccupation, "CHOOSE OCCUPATION", hidePlaceholder = true)

        wireSteps()
        setupKeyboardVisibilityToggle()

        val btnSave = findViewById<MaterialButton>(R.id.btnSaveCheckup)
        val btnContinue = findViewById<MaterialButton>(R.id.btnGoToPrescription)

        // 🔴🔴🔴 খাতার সারি B437 (TK-নির্দেশ, 05.08.2026 — "আগের সেভ করাগুলো
        // লক থাকতে হবে")। **গভীরে যাচাই করে গুরুতর বাগ পাওয়া গেছে (TK যা
        // বলেননি, কিন্তু ঠিক এই আশঙ্কারই আসল কারণ):** `ClinicalRepository.
        // lastCheckup` কোনো নির্দিষ্ট রোগীর সাথে বাঁধা ছিল না, আর কখনো
        // রিসেট হতো না (`resetForNewPatient()` ফাংশন লেখা ছিল কিন্তু
        // **কোথাও ডাকাই হতো না**)। ফলে: একজন ডাক্তার এক রোগীর Checkup
        // Save করার পরে, অ্যাপ বন্ধ না করেই (স্বাভাবিক দিনের কাজে যা হয়েই
        // থাকে) **অন্য যেকোনো রোগীর** Checkup পর্দা খুললেই সেই **আগের
        // রোগীর** তথ্য চুপচাপ ফর্মে ভরে যেত — রোগী ভুল হয়ে যাওয়ার আসল,
        // গুরুতর ঝুঁকি ছিল। **সমাধান:** এই স্বয়ংক্রিয়-ভরাট সম্পূর্ণ
        // সরানো হলো — প্রতিটা নতুন Checkup এখন সবসময় ফাঁকা ফর্ম দিয়ে শুরু
        // হয়, কখনো অন্য (বা এমনকি একই) রোগীর আগের তথ্য থেকে ভরে না।
        // পুরনো তথ্য এখন শুধু "📜 History"-তেই দেখা যায় (নিচে নতুন বোতাম),
        // **সম্পাদনাযোগ্য ফর্মে কখনো আসবে না** — এটাই TK-এর চাওয়া "লক"।

        val readOnly = !RoleSession.canEditClinical()
        if (readOnly) {
            findViewById<TextView>(R.id.tvReadOnlyNotice).visibility = android.view.View.VISIBLE
            setFieldsEnabled(false)
            btnSave.isEnabled = false
            btnSave.text = "Doctor-only"
        }

        btnSave.setOnClickListener {
            /* 🔴🔒 V786 — রোগী চেনা না গেলে (কল/মেমরির কারণে প্রসেস মরে পর্দা
               আবার খোলা) এখানেই থেমে যায়। আগে ফাঁকা আইডিতেও সেভ হয়ে যেত আর
               "saved" লেখা উঠত — ডাক্তারের লেখা চুপচাপ হারাত। */
            if (RoleSession.blockIfNoPatient(this)) return@setOnClickListener
            val record = collect()
            // 🟢🔒 V656 — মূল থ্রেডেই (View স্পর্শ করা নিরাপদ) ধরে রাখা হলো,
            // নিচের ব্যাকগ্রাউন্ড-সেভে ব্যবহারের জন্য (View off-thread ছুঁলে
            // ক্র্যাশ হতো)।
            val reminderNoteNow = findViewById<android.widget.EditText>(R.id.etDoctorReminderNote).text?.toString().orEmpty().trim()
            val reminderDateNow = doctorReminderDateIso
            val reminderForNow = doctorReminderForMobile   // 🩺 V1109
            // 🟢🔒 V671 — সময়ও একই মূল-থ্রেডেই ধরে রাখা হলো।
            val reminderTimeNow = doctorReminderTimeStr
            PrescriptionOptionsStore.captureCheckup(applicationContext, record)
            /* 🩺 V839 — View মূল থ্রেডেই পড়া হয় (V656-এর প্রমাণিত নিয়ম),
               নিচের ব্যাকগ্রাউন্ড-সেভ শুধু জমা মানটা ব্যবহার করে। */
            pendingNvp = try { collectNextVisitPlan() } catch (_: Throwable) { null }
            /* 🔴🔒 V938 — View মূল থ্রেডেই পড়া হয় (উপরের V839-এর একই নিয়ম);
               নিচের ব্যাকগ্রাউন্ড-কাজ শুধু জমা লেখাটাই ব্যবহার করে। */
            val ttdNow = try { collectTodayTreatment() } catch (_: Throwable) { "" }
            /* 🔵🔒 V947 — ডাক্তারের মন্তব্যও মূল থ্রেডেই পড়া হয় (উপরের একই নিয়ম)। */
            val docRemarkNow = try {
                findViewById<android.widget.EditText>(R.id.etDoctorRemark).text?.toString()?.trim().orEmpty()
            } catch (_: Throwable) { "" }
            val ttdMobile = RoleSession.currentPatientMobile
            // 🔴 B437 — আগে এখানে `ClinicalRepository.lastCheckup = record`
            // বসত, যেটাই উপরের ক্রস-রোগী বাগের উৎস ছিল। যেহেতু এখন কেউ এই
            // ভ্যারিয়েবলটা পড়েই না (populate() ডাক সরানো হয়েছে), এই লাইনটাও
            // সরানো হলো — নইলে অকারণে মেমোরিতে আগের রোগীর তথ্য থেকে যেত।
            ClinicalRepository.addVisit(
                type = "Check-up",
                summary = record.complaint.ifBlank { record.patientDecision }.take(80),
                role = RoleSession.currentRole
            )
            val details = buildDetails(record)
            val createdBy = NativeSession.current(this)?.mobile ?: ""
            val pid = RoleSession.currentPatientId
            val pname = RoleSession.currentPatientName
            val photosJson = org.json.JSONObject().apply {
                if (record.beforePhoto.isNotBlank()) put("before", record.beforePhoto)
                if (record.duringPhoto.isNotBlank()) put("during", record.duringPhoto)
                if (record.afterPhoto.isNotBlank()) put("after", record.afterPhoto)
            }
            val photosStr = if (photosJson.length() > 0) photosJson.toString() else ""
            // 🔒🔒 খাতার সারি B188 (TK, 30.07.2026 সন্ধ্যা — "ডক্টর ভিজিটের নাম
            // এন্ট্রি ও মেডিসিন প্রেসক্রাইবের পরে লোডিং" রিপোর্টের অংশ):
            // TK-এর ২৮.০৭.২০২৬-এর নিয়ম — "Save চাপার সঙ্গে সঙ্গে পরের পর্দা
            // খুলবে, ক্লাউডে পাঠানো শেষ হওয়ার জন্য স্টাফকে বসিয়ে রাখা যাবে না"
            // — ইতিমধ্যে Prescription · Investigation Advice · Diet Chart-এ
            // (এই একই ফোল্ডারে) বসানো আছে; শুধু **এই একটা স্ক্রিন (Doctor
            // Checkup) বাদ পড়ে গিয়েছিল** — এখানে Save চাপলে `saveMedical()`
            // (ক্লাউডে আসল আপলোড, নেটওয়ার্ক শেষ না হওয়া পর্যন্ত আটকে থাকে)
            // আর তার ঠিক পরে `markDoctorComplete()` (আরেকটা নেটওয়ার্ক কল) —
            // দুটোই **একের পর এক অপেক্ষা করে**, তারপর Toast দেখাত। এটাই
            // "Doctor Visit-এ নাম এন্ট্রির পরে লোডিং"-এর একটা কারণ।
            //
            // ⛔ **কী বদলাল:** এখন বাকি তিনটে স্ক্রিনের **হুবহু একই প্যাটার্ন**
            // (Investigation Advice-এর সঙ্গে মিলিয়ে দেখা হয়েছে) — Toast
            // **সঙ্গে সঙ্গে** দেখানো হয়, আর `saveMedical()` (যেটা নিজে
            // ফোনে-আগে-সেভ + retry-queue-সহ, কিন্তু ভিতরে ব্লকিং নেটওয়ার্ক
            // কল আছে) এবং `markDoctorComplete()` দুটোই **`BackgroundWork.run{}`
            // -এর ভিতরে** পিছনে চলে — মূল থ্রেডে (UI) কোনো অপেক্ষা নেই।
            // ⛔ **কিছুই হারায় না** — `saveMedical()`-এর ভিতরে ফোনে-আগে-সেভ
            // + ব্যর্থ হলে retry-queue আগে থেকেই ছিল, অক্ষত। ⛔ ৭-ধাপের ফর্ম,
            // checkbox, autofill, ছবি (before/during/after) — কিছুই ছোঁয়া
            // হয়নি, শুধু Toast/ক্লাউড-কলের সময়টা।
            Toast.makeText(this@DoctorCheckupActivity, "Check-up saved.", Toast.LENGTH_SHORT).show()
            /* 💾 V1255 (TK-নির্দেশ) — এইবার থেকেই ভরা-ও-বন্ধ ধাপগুলো নিচের
               বাক্সে নামতে পারবে। ⛔ সেভের কোনো কাজ/ক্রম বদলায়নি — শুধু
                  দেখার সাজানোটা এখান থেকে শুরু হয়। */
            savedInThisScreen = true
            try { refreshSavedGroup() } catch (_: Throwable) { }
            val appCtx = applicationContext
            val branch = RoleSession.currentPatientBranch
            val displayId = RoleSession.currentPatientDisplayId.ifBlank { pid }
            // 🔵 V557: পিছনের কাজে যাওয়ার আগেই দরকারি মানগুলো ধরে রাখা
            val costBefore = lastSavedCost
            val byName = NativeSession.current(this)?.name.orEmpty().ifBlank { createdBy }
            lastSavedCost = record.estimatedCost
            // 🔵 V559: এই রোগীর যে লেখাটা রোগীর সারিতে বসবে
            pendingNote = record
            // 🟢🔒 V676 — structured JSON `selected` ঘরে (আগে ফাঁকা যেত), যাতে
            // আজকের এই সারিটা পরে আবার এডিট করা যায়। `editingMedicalId`
            // থাকলে (এই পর্দা এডিট-মোডে খোলা হয়েছিল) সেই একই id-তেই বসে —
            // নতুন সারি বানানো হয় না।
            val selectedJson = record.toJsonString()
            val editId = editingMedicalId
            com.tkbiswas.pilesclinic.native.BackgroundWork.run {
                ClinicalCloudRepository.saveMedical(appCtx, pid, pname, "Doctor Checkup", selectedJson, details, createdBy, photosStr, editId)
                // 🔴 TK-নির্দেশ (04.08.2026): আগে শুধু "Agree for Treatment"
                // বাছলেই doctorComplete=true হত — বাকি পাঁচটা সিদ্ধান্তে
                // (Not Agree/Will Think/Family Discussion/Financial Problem/
                // Other) ডাক্তার সত্যিই চেক-আপ শেষ করেও রোগী CHECK-UP Queue-তে
                // চিরকাল আটকে থাকতেন। এখন যেকোনো সিদ্ধান্তেই checkup শেষ ধরা
                // হয় — Save-এর সাথে সাথেই doctorComplete=true, Queue থেকে সরে
                // যায়। Best-effort; কখনো checkup সেভ আটকায় না।
                markDoctorComplete(pid)
                /* 🔴🔴🔒 V938 (TK-নির্দেশ) — ডাক্তার আজ যা করেছেন সেটা এখন
                   **নিজে থেকেই** চেম্বার Date-এর Treatment Progress-এ বসে যায়
                   (ও Report Card-এ), ঠিক যেভাবে চেম্বার থেকে লিখলে বসত।
                   ⛔ কিছু না বাছলে একটা অক্ষরও লেখা হয় না — পুরনো Progress অক্ষত।
                   ⛔ ব্যর্থ হলেও চেকআপ সেভ কখনো আটকায় না (ভিতরে try-catch)। */
                try {
                    com.tkbiswas.pilesclinic.native.TodayTreatmentSync.push(
                        appCtx, ttdMobile, pid, ttdNow, byName, doctorRemark = docRemarkNow
                    )
                } catch (_: Throwable) { }
                // 🔒 কাজ শেষ — সাথে সাথেই মুছে ফেলা হয়, যাতে পরের রোগীর
                // ঘরে আগেরজনের লেখা বসার কোনো পথ না থাকে (B437-এর শিক্ষা)।
                pendingNote = null
                /* 🩺🔒 V839 — একই কারণে প্ল্যানটাও সাথে সাথে মুছে ফেলা হয়
                   (B437-এর নিয়ম): নইলে এই পর্দা খোলা রেখে অন্য রোগীর
                   চেকআপে গেলে **আগের রোগীর প্ল্যান** চুপচাপ বসে যেতে পারত। */
                pendingNvp = null
                // 🔔 TK-নির্দেশ (04.08.2026): "Agree for Treatment" ছাড়া অন্য
                // যেকোনো সিদ্ধান্তে সেই রোগীর ব্রাঞ্চের স্টাফের ঘন্টায় নোটিশ
                // যাবে (কে ফলো-আপ করবেন বোঝার জন্য)। ⛔ "Agree for
                // Treatment"-এ কোনো নোটিশ নেই (এটাই স্বাভাবিক পথ, খবর দেওয়ার
                // দরকার নেই)। ⛔ নোটিশ ব্যর্থ হলেও checkup সেভ/doctorComplete
                // কিছুই আটকায় না (আলাদা try-catch)।
                /* ═══ 🔵🔒 V557 (২২.০৮.২০২৬, TK-এর সরাসরি নির্দেশ) ═══
                   ১. **সম্ভাব্য রোগ বদলালে রোগীর সারিতেও লেখা হয়** — তাহলেই হেডার ·
                      তালিকা · ছাপা সব জায়গায় নতুন নামটা দেখায়।
                      ⛔ ডাক্তার হাত না দিলে (বা যা আছে তাই বাছলে) **এক অক্ষরও লেখা হয় না**
                         (`CounselModel.diseaseChanged` — চালিয়ে যাচাই করা)।
                      ⛔ শুধু `disease` ঘরটাই; রোগীর আর কিছু ছোঁয়া হয় না।
                   ২. **আনুমানিক খরচ বলা হলে সেই ব্রাঞ্চের সব স্টাফের ঘণ্টায় নোটিশ**
                      (TK: *"নোটিফিকেশন সেই ব্রাঞ্চের সবার কাছে যাবে"*)।
                      ⛔ খরচ ফাঁকা/শূন্য হলে বা আগের বারের সমান হলে **নোটিশ যায় না** —
                         নইলে প্রতিবার Save-এ অকারণে ঘণ্টা বাজত।
                      ⛔ নোটিশের পথটা নতুন নয় — নিচের "Patient Decision"-এর হুবহু একই।
                   ⛔ দুটোই আলাদা try-catch — ব্যর্থ হলেও চেক-আপ সেভ কখনো আটকায় না। */
                try {
                    if (CounselModel.diseaseChanged(patDisease, record.probableDisease)) {
                        SupabaseClient.updateById(
                            "patients", pid,
                            org.json.JSONObject().put("disease", record.probableDisease)
                        )
                        /* 🔵🔒 V722 (২৭.০৮.২০২৬, ডা. কে. এইচ. মণ্ডলের রিপোর্ট) —
                           ডেটাবেসে লেখা হলেও **এই ফোনের মেমরিতে পুরোনো নামটাই**
                           থেকে যেত, আর প্রেসক্রিপশন ছাপা হয় ঠিক ওখান থেকেই
                           (`PrescriptionOptionsStore.printLines()` → `RoleSession
                           .currentPatientDisease`) ও এই পর্দার A4-ও `patDisease`
                           থেকে। তাই সেভ করেই ছাপলে **পুরোনো রোগ** ছাপত।
                           ⇒ এখন দুটোই সঙ্গে সঙ্গে হালনাগাদ হয়।
                           ⛔ নেট খারাপ থাকলেও ডাক্তার যা ঠিক করেছেন কাগজে তাই
                              যাবে (ক্লাউডে পরে গেলেও কাগজ ভুল হবে না)।
                           ⛔ শুধু রোগের নাম; আর কিছু ছোঁয়া হয়নি। */
                        patDisease = record.probableDisease
                        RoleSession.updateDisease(record.probableDisease)
                    }
                } catch (_: Throwable) { }
                // 🟢🔒🔒 V656 (২৫.০৮.২০২৬, TK-নির্দেশ — Doctor Note & Reminder)
                // — নোট/তারিখ যদি বদলে থাকে, সেভ হয়। ⛔ দুটোই ফাঁকা হলে
                // (ডাক্তার এই বাক্সে কিছুই লেখেননি) কোনো বাড়তি Supabase কল
                // হয় না — এটাই বেশিরভাগ চেকআপের স্বাভাবিক অবস্থা।
                try {
                    if (reminderNoteNow.isNotBlank() || reminderDateNow.isNotBlank() || reminderTimeNow.isNotBlank()) {
                        SupabaseClient.updateById(
                            "patients", pid,
                            org.json.JSONObject()
                                .put("doctorReminderNote", reminderNoteNow)
                                .put("doctorReminderDate", reminderDateNow)
                                .put("doctorReminderFor", reminderForNow)   // 🩺 V1109
                                // 🟢🔒 V671 (২৫.০৮.২০২৬, TK-নির্দেশ) — সময়ও সেভ হয়,
                                // নইলে "ঠিক সময়ে" মনে করানো সম্ভব না।
                                .put("doctorReminderTime", reminderTimeNow)
                        )
                    }
                } catch (_: Throwable) { }
                try {
                    if (CounselModel.shouldNotifyCost(costBefore, record.estimatedCost)) {
                        val shownDisease = if (record.probableDisease.isNotBlank() &&
                            record.probableDisease != CounselModel.PICK_NONE) record.probableDisease else patDisease
                        com.tkbiswas.pilesclinic.native.BriefingRepository().post(
                            appCtx, CounselModel.COST_TITLE,
                            CounselModel.costMessage(pname, displayId, branch, record.estimatedCost,
                                shownDisease, record.timeAsked, byName),
                            "branch", branch, "", createdBy
                        )
                    }
                } catch (_: Throwable) { }

                if (record.patientDecision.isNotBlank() && record.patientDecision != "Agree for Treatment") {
                    try {
                        val msg = "$pname ($displayId) — Doctor checkup done. " +
                            "Patient decision: ${record.patientDecision}. Please follow up."
                        com.tkbiswas.pilesclinic.native.BriefingRepository().post(
                            appCtx, "Patient Decision", msg, "branch",
                            branch, "", createdBy
                        )
                    } catch (_: Throwable) { }
                }
            }
            // 🔴 (07.08.2026, scroll_C মকআপ) — সেভ হওয়ামাত্র সব সেকশন লক
            // (read-only summary), Estimate লুকানো, Quick Actions বাদ, নিচে
            // Share/Print। ⛔ উপরের সেভ/ক্লাউড-লজিক অপরিবর্তিত — শুধু দেখার লক।
            enterLockedMode(record)
        }

        btnContinue.setOnClickListener {
            startActivity(Intent(this, PrescriptionActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnMedicineSlip).setOnClickListener {
            startActivity(Intent(this, MedicineSlipActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnBloodTest).setOnClickListener {
            startActivity(Intent(this, InvestigationAdviceActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnDiet).setOnClickListener {
            startActivity(Intent(this, DietChartActivity::class.java))
        }
    }

    /** Patient header card (web: queueRow card). Name/Reg/Branch are shown
     *  instantly from the session; mobile/age/sex/photo are filled best-effort
     *  from Supabase and never block the screen. */
    /** Professional step form (web parity content, minimal scrolling): the
     *  sections show one at a time via a ViewFlipper, with a tappable step bar
     *  on top and a sticky Back / Save / Next bar at the bottom.
     *  🔒 TK-নির্দেশ (04.08.2026, ফটো-প্রুফে "লক করুন"): আগে ৭টা ধাপ ছিল,
     *  এখন History+Previous এক করে ও Finance+Decision এক করে ৫টা ধাপ। */
    private val stepTitles = listOf(
        "1 History & Previous", "2 Clinical", "3 Counsel",
        "4 Estimate & Decision", "5 Photo & Video"
    )
    private var stepChips = mutableListOf<TextView>()   // 🎨 এখন এগুলো নম্বর-বৃত্ত (TextView)
    // 🎨 (07.08.2026, প্রুফ-চেহারা) — প্রতি চিপের নিচের ছোট লেবেল (History/Clinical…)
    private var stepChipLabels = mutableListOf<TextView>()
    /* 🧮🔒 V1280 (০৯.০৯.২০২৬, TK-র ফাইনাল অনুমতি ও ফটো-প্রুফ পাশ — তালিকা
       সারি ৪০২ · ৪০৩): TK: *"এই 4 নম্বর অপশন টা থাকবেই না"* · *"Estimate"*।
       ⇒ ধাপ এখন **চারটে**: History · Clinical · Estimate · Photo।
       ⛔ ধাপ ৪-এর (Probable Disease · Time Asked) ঘরগুলো লেআউটে **রয়ে গেছে,
          শুধু লুকানো** — রোগ এখন COST ESTIMATE-এর লাইন থেকে, সময় এস্টিমেট
          পর্দা থেকে আসে (নিচে `onActivityResult`), তাই সেভ · কাগজ · রোগীর
          `disease` ঘর — সবই আগের ঘর দিয়েই চলে; পুরনো রেকর্ড অটুট। */
    /* 🔢🔒 V1494 (১৫.০৯.২০২৬, TK-নির্দেশ ও ডেমো-প্রুফ পাশ) — উপরের ৪টা ধাপ-চিপ
       ৭টা হলো (sectionIds-এর সাথে হুবহু মিলিয়ে) — Remark/Treatment/Next Visit
       এখন নিজের নম্বর পায়, তাই ১,২,৩-এর পর নম্বরহীন-তারপর-৪ আর দেখা যাবে না। */
    private val stepShort = listOf("History", "Clinical", "Estimate", "Remark", "Treatment", "Next Visit", "Photo")
    private var currentStep = 0

    // 🆕 (07.08.2026, TK-অনুমোদিত) — সেভ-করা রেকর্ড ও A4 রিপোর্টের পেশেন্ট-তথ্য
    // (bindPatientHeader-এর ক্লাউড-খোঁজায় ভরে; কোনো নতুন কল নয়)।
    private var lastSavedRecord: CheckupRecord? = null
    private var patMobile = ""
    private var patAge = ""
    private var patSex = ""
    private var patDisease = ""
    private var patAddress = ""
    // 🔒 B551 (08.08.2026, TK-অনুমোদিত) — A4 রেকর্ডে রোগীর ছবি দেখানোর জন্য
    // মনে রাখা (checkup খোলার সময় `p.s("photo")` আগে থেকেই আসে, নতুন কল নয়)।
    private var patPhoto = ""

    // 🎨 (07.08.2026) — ধাপ-নম্বরের গোল বৃত্ত (active/done হলে সবুজ, নইলে ধূসর)।
    private fun stepCircleBg(green: Boolean): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor(if (green) "#12805C" else "#DFE5EC"))
        }

    /* 🎨🔒 V897 (৩১.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — ধাপের বৃত্তের তিন
       অবস্থা: **হয়ে গেছে** (ভরাট সবুজ) · **এখন এখানে** (ভরাট সবুজ, চারপাশে
       হালকা সবুজ বলয়) · **এখনো বাকি** (সাদা, ধূসর পাড়)। ⛔ পুরোনো
       `stepCircleBg()` অক্ষত — লক-হয়ে-যাওয়া পর্দা ওটাই ব্যবহার করে। */
    private fun stepDotBg(done: Boolean, current: Boolean): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            when {
                current -> {
                    setColor(android.graphics.Color.parseColor("#12805C"))
                    setStroke(dp(4), android.graphics.Color.parseColor("#CFE9DD"))
                }
                done -> setColor(android.graphics.Color.parseColor("#12805C"))
                else -> {
                    setColor(android.graphics.Color.WHITE)
                    setStroke(dp(2), android.graphics.Color.parseColor("#DDE5EE"))
                }
            }
        }

    /** ধাপের বৃত্তগুলোর মাঝের সরু রেখা — বাঁ দিকেরটা ও ডান দিকেরটা আলাদা। */
    private var stepLineLeft = mutableListOf<android.view.View>()
    private var stepLineRight = mutableListOf<android.view.View>()

    private fun wireSteps() {
        val bar = findViewById<LinearLayout>(R.id.stepBar)
        // 🎨🔒 (07.08.2026, প্রুফ-চেহারা, TK-অনুমোদিত) — আগে প্রতিটা চিপ ছিল
        // বড় পিল (padding 16/8, marginEnd 6), scroll-view-এ পরপর — ৩টার পর
        // ডান দিকে বেরিয়ে যেত। এখন ৫টা "নম্বর-বৃত্ত + নিচে ছোট লেবেল" সমান
        // ভাগে (weight 1) এক লাইনে বসে। চাপলে আগের মতোই showStep(i)।
        // আগের কোড:
        //   stepTitles.forEachIndexed { i, title ->
        //     val chip = TextView(this).apply { text=title; textSize=12.5f
        //       setPadding(dp(16),dp(8),dp(16),dp(8)); marginEnd=dp(6)
        //       setOnClickListener { showStep(i) } }
        //     bar.addView(chip); stepChips.add(chip) }
        bar.removeAllViews(); stepChips.clear(); stepChipLabels.clear()
        stepLineLeft.clear(); stepLineRight.clear()
        stepShort.forEachIndexed { i, short ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                isClickable = true; isFocusable = true
                setOnClickListener { showStep(i) }
            }
            val circle = TextView(this).apply {
                text = (i + 1).toString()
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#9AA6B4"))
                background = stepDotBg(done = false, current = false)
                val s = dp(26)
                layoutParams = LinearLayout.LayoutParams(s, s)
            }
            /* 🎨 V897 — বৃত্তের দু'পাশে সরু রেখা, তাই পাঁচটা ধাপ একটা সরু
               পথে জোড়া দেখায় (TK-অনুমোদিত প্রুফ)। প্রথমটার বাঁ দিকের ও শেষেরটার
               ডান দিকের রেখা অদৃশ্য থাকে। */
            fun line(visible: Boolean): android.view.View = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(3), 1f)
                setBackgroundColor(android.graphics.Color.parseColor("#E4EAF1"))
                visibility = if (visible) android.view.View.VISIBLE else android.view.View.INVISIBLE
            }
            val lLine = line(i > 0)
            val rLine = line(i < stepShort.size - 1)
            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            top.addView(lLine); top.addView(circle); top.addView(rLine)
            val lbl = TextView(this).apply {
                text = short
                textSize = 9f
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                setTextColor(android.graphics.Color.parseColor("#8A97A6"))
                setPadding(0, dp(4), 0, 0)
            }
            cell.addView(top); cell.addView(lbl)
            bar.addView(cell)
            stepChips.add(circle); stepChipLabels.add(lbl)
            stepLineLeft.add(lLine); stepLineRight.add(rLine)
        }
        // 🔴 (07.08.2026, scroll_A মকআপ) — এক-পেজ স্ক্রলে "Next" ধাপ নেই;
        // btnNext লুকানো, btnBack এখন পর্দা থেকে বেরোয় (finish)। রোগীর পূর্ণ
        // কার্ড সবসময় উপরে থাকে, সংকুচিত-কার্ড লুকানো।
        findViewById<MaterialButton>(R.id.btnNext).visibility = android.view.View.GONE
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.patientDetailsFull).visibility = android.view.View.VISIBLE
        findViewById<android.view.View>(R.id.patientDetailsCollapsed).visibility = android.view.View.GONE
        // Share / Print — সব সেভ হয়ে লক হলে দেখা যায় (enterLockedMode)।
        findViewById<MaterialButton>(R.id.btnShareCheckup).setOnClickListener { shareCheckup() }
        // 🔴 V501 — Back ও Save-এর মাঝের নতুন Share বোতাম (TK-নির্দেশ)।
        findViewById<MaterialButton>(R.id.btnShareNow).setOnClickListener { saveThenShare() }
        findViewById<MaterialButton>(R.id.btnPrintCheckup).setOnClickListener { printCheckup() }
        /* 📝🔒 V1276 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — তালিকা সারি ৩৯৮):
           TK: *"রোগীকে সেই কাগজ প্রিন্ট আউট দিব তখন … ডাক্তারের রিমার্কটা
           ওখানে থাকবে না সেই হিসেবে প্রিন্ট আউট হবে … থাকবে সেই ক্ষেত্রে
           … তার একটা অপশন রাখবেন"*।
           ⛔ ঘোরালেই কাগজটা নতুন করে আঁকা হয়, আর `buildA4Html()`-এ একই ঘরটা
              বসে ⇒ পর্দা · 🖨 Print · 🔗 Share তিনটেতেই **হুবহু একই** কাগজ যায়,
              তিন জায়গায় তিনরকম হওয়ার পথ নেই (নিয়ম ৭ক-এর ২)।
           ⛔ শুনানিটা এখানে **একবারই** বসে; `showSavedA4()` সুইচের অবস্থা
              ছোঁয় না, তাই নিজে-নিজে বারবার আঁকার (recursion) কোনো পথ নেই। */
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swDrRemark)
            .setOnCheckedChangeListener { _, on ->
                a4ShowDocRemark = on
                lastSavedRecord?.let { showSavedA4(buildA4Html(it)) }
            }
        /* 📝 V1280 — দ্বিতীয় সুইচ: চুক্তি/পরিকল্পনার নোট (ডিফল্ট বন্ধ)। একই নিয়ম —
           ঘোরালেই কাগজ নতুন করে আঁকা, পর্দা · Print · Share এক। */
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.swAgreement)
            .setOnCheckedChangeListener { _, on ->
                a4ShowAgreement = on
                lastSavedRecord?.let { showSavedA4(buildA4Html(it)) }
            }
        // 🆕 (07.08.2026) — A4 রিপোর্ট থেকে ✎ Edit চাপলে আবার ফর্মে ফেরে।
        findViewById<MaterialButton>(R.id.btnEditCheckup).setOnClickListener { showEditForm() }
        showStep(0)
    }

    // 🔴🎨🔒 খাতার সারি B432 (TK-নির্দেশ, 05.08.2026 — "কীবোর্ড খোলা থাকলে
    // BACK/SAVE/NEXT বার কীবোর্ডের ঠিক উপরে ভেসে থাকা প্রফেশনাল লাগছে
    // না")। **পদ্ধতি:** root content view-এর দৃশ্যমান উচ্চতা মাপা হয় —
    // কীবোর্ড খুললে সেই উচ্চতা হঠাৎ অনেকখানি কমে যায় (স্ক্রিনের অন্তত
    // ১৫%), তখনই বোঝা যায় কীবোর্ড খোলা। এটাই Android-এ কীবোর্ড দেখা/না-
    // দেখা বোঝার সবচেয়ে প্রচলিত, নির্ভরযোগ্য পদ্ধতি (নতুন কোনো বিশেষ
    // permission/API লাগে না) — Manifest-এ এই স্ক্রিনের জন্য
    // `windowSoftInputMode="adjustResize"` বসানো হয়েছে যাতে এই মাপাটা
    // সব ফোনে (Xiaomi/Vivo/Oppo-ঘরানার ফোনেও) একইভাবে কাজ করে।
    // ⛔ **ঝুঁকি/সীমা (TK-কে জানানো হলো):** এটা উচ্চতা-মাপার উপর ভিত্তি
    // করে, তাই অত্যন্ত বিরল/অস্বাভাবিক স্ক্রিন-মোডে (split-screen, ভাঁজ-
    // করা ফোন) সামান্য ভুল হতে পারে — সেক্ষেত্রে সবচেয়ে খারাপ ফল শুধু
    // এই যে বারটা মাঝে মাঝে ভুল সময়ে দেখা/লুকানো হবে, অ্যাপ ক্র্যাশ
    // করবে না, কোনো ডেটা/সেভ-লজিক এতে ছোঁয়া হয় না।
    private fun setupKeyboardVisibilityToggle() {
        try {
            val rootView = findViewById<android.view.View>(android.R.id.content)
            val bottomBar = findViewById<android.view.View>(R.id.bottomActionBar)
            rootView.viewTreeObserver.addOnGlobalLayoutListener {
                try {
                    val r = android.graphics.Rect()
                    rootView.getWindowVisibleDisplayFrame(r)
                    val screenHeight = rootView.rootView.height
                    if (screenHeight <= 0) return@addOnGlobalLayoutListener
                    val keypadHeight = screenHeight - r.bottom
                    val keyboardVisible = keypadHeight > screenHeight * 0.15
                    bottomBar.visibility =
                        if (keyboardVisible) android.view.View.GONE else android.view.View.VISIBLE
                } catch (_: Throwable) { }
            }
        } catch (_: Throwable) { }
    }

    // 🔴🎨🔒 TK-নির্দেশ (07.08.2026, scroll_A/B/C মকআপ) — এক-পেজ স্ক্রল।
    // flipper এখন LinearLayout; চিপ চাপলে সেই সেকশনে স্ক্রল হয় (আগের
    // displayedChild নেই)। রোগীর পূর্ণ কার্ড সবসময় উপরে থাকে (সংকুচিত-কার্ড
    // টগল বাদ)। ⛔ সব ফিল্ড/ডেটা-সেভ অপরিবর্তিত।
    /* 🔢🔒 V1494 (১৫.০৯.২০২৬, TK-নির্দেশ ও ডেমো-প্রুফ পাশ) — TK ধরলেন উপরের
       ধাপ-নম্বর ১,২,৩-এর পর হঠাৎ DOCTOR'S REMARK/TODAY'S TREATMENT DONE/
       NEXT VISIT PLAN নম্বরহীন, তারপর আবার ৪ — এলোমেলো লাগছিল। TK চাইলেন
       এই তিনটেও নম্বর পাক, উপরের বৃত্তও ৭ পর্যন্ত হোক। secDocRemark/
       secTodayTreat/secNextVisitPlan যোগ হলো — এগুলো secCounsel-এর ঠিক
       নিচে, secPhoto-র ঠিক আগে, XML-এ যে ক্রমে বসানো ঠিক সেই ক্রমেই। */
    private val sectionIds = intArrayOf(
        R.id.secHistory, R.id.secClinical, R.id.secCounsel,
        R.id.secDocRemark, R.id.secTodayTreat, R.id.secNextVisitPlan, R.id.secPhoto
    )
    /**
     * 🎓🔒 V783 — **রোগীকে বোঝানোর পর্দা।** পাইলস · ফিশার · ফিস্টুলা —
     * তিনটে বোতাম সবসময় থাকে, ইচ্ছেমতো বদলানো যায় (একই রোগীর তিন রোগ
     * থাকলেও একটার পর একটা দেখানো যাবে — TK-এর শর্ত)।
     *
     * ⛔ **কিচ্ছু সেভ হয় না** — কোনো ডেটাবেস-কল নেই, প্রিন্ট নেই, A4 নেই।
     * ⛔ ডাক্তারের আঁকা ছবি ছোঁয়া হয় না — আলাদা View, আলাদা পর্দা।
     * ⛔ পুরোটা try/catch-এ; এখানে কিছু ভুল হলেও Check-up পর্দা ভাঙে না।
     */
    private fun openKsharTeach() {
        try {
            val d = symDp(1)
            val view = KsharTeachView(this)
            val root = android.widget.FrameLayout(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#08111C"))
            }
            root.addView(view, android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT))

            val dlg = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

            // ── নিচের প্যানেল: রোগ বাছাই + ধাপের লেখা + বোতাম ──
            val panel = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(symDp(10), symDp(9), symDp(10), symDp(11))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = symDp(18).toFloat()
                    setColor(android.graphics.Color.parseColor("#D908111C"))
                    setStroke(symDp(1), android.graphics.Color.parseColor("#40FFFFFF"))
                }
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.view.Gravity.BOTTOM).apply { setMargins(symDp(10), 0, symDp(10), symDp(10)) }
            }

            val chipRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, symDp(8))
            }
            val chips = ArrayList<TextView>()
            /* 🔤🔒 V787 (TK-নির্দেশ ২৮.০৮.২০২৬: *"রোগের নাম ইংরেজিতে রাখুন"*) —
               তিনটে রোগের নাম প্রজেক্টের বাকি জায়গার মতোই ইংরেজিতে
               (Registration/Enquiry-তে যেমন Piles · Fissure · Fistula)।
               ⛔ ধাপের ব্যাখ্যা আগের মতোই বাংলা — ওটা রোগীর জন্য। */
            val names = listOf("\uD83E\uDE78 Piles", "✂️ Fissure", "\uD83D\uDD04 Fistula")

            val hint = TextView(this).apply {
                text = NoBengali.s("👆 ছবিতে ছুঁয়ে নিশানার জায়গা সরাতে পারেন")
                textSize = 11.5f
                setTextColor(android.graphics.Color.parseColor("#9FB6C8"))
                setPadding(symDp(2), 0, 0, symDp(6))
            }
            val cap = TextView(this).apply {
                textSize = 13.5f
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            fun btn(label: String, wide: Boolean): TextView = TextView(this).apply {
                text = label; textSize = 13.5f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.WHITE)
                setPadding(symDp(12), symDp(9), symDp(12), symDp(9))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = symDp(9).toFloat()
                    setColor(android.graphics.Color.parseColor("#1F6D4A"))
                }
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    if (wide) 0 else android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    if (wide) 1f else 0f).apply { setMargins(symDp(4), 0, symDp(4), 0) }
            }
            val bPrev = btn("◀", false)
            val bNext = btn("পরের ধাপ ▶", true)
            val bClose = btn("✕", false)

            var anim: android.animation.ValueAnimator? = null
            fun run() {
                anim?.cancel()
                view.t = 0f; view.invalidate()
                val a = android.animation.ValueAnimator.ofFloat(0f, 1f)
                a.duration = 1100L
                a.addUpdateListener { v -> view.t = v.animatedValue as Float; view.invalidate() }
                anim = a; a.start()
            }
            fun paint() {
                cap.text = KsharTeachView.caption(view.disease, view.step)
                bPrev.visibility = if (view.step > 0) android.view.View.VISIBLE else android.view.View.GONE
                bNext.visibility = if (view.step < KsharTeachView.stepCount(view.disease) - 1)
                    android.view.View.VISIBLE else android.view.View.GONE
                chips.forEachIndexed { i, ch ->
                    val on = i == view.disease
                    ch.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = symDp(10).toFloat()
                        setColor(android.graphics.Color.parseColor(if (on) "#1F6D4A" else "#2216222E"))
                        setStroke(symDp(1), android.graphics.Color.parseColor(if (on) "#2E9366" else "#553B4A5A"))
                    }
                    ch.setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#B9C6D4"))
                }
            }
            /* 🖼️🔒 V783 — **ভিত্তি-ছবি বাছাই** (TK-এর বাছাই: "১ অথবা ২")।
               ⛔ ছবি বদলালে নিশানার জায়গা ওই ছবির স্বাভাবিক জায়গায় ফেরে,
                  তাই কখনো ভুল জায়গায় বসে থাকে না। */
            val picRow = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, symDp(7))
            }
            val pics = ArrayList<TextView>()
            fun paintPics() {
                pics.forEachIndexed { i, ch ->
                    val on = i == view.base
                    ch.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = symDp(9).toFloat()
                        setColor(android.graphics.Color.parseColor(if (on) "#1A4E7A" else "#2216222E"))
                        setStroke(symDp(1), android.graphics.Color.parseColor(if (on) "#3B84C4" else "#553B4A5A"))
                    }
                    ch.setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#B9C6D4"))
                }
            }
            listOf("\uD83D\uDDBC ছবি 1 · সামনে", "\uD83D\uDDBC ছবি 2 · পাশ").forEachIndexed { i, nm ->
                val ch = TextView(this).apply {
                    text = nm; textSize = 12.5f
                    gravity = android.view.Gravity.CENTER
                    setPadding(symDp(6), symDp(8), symDp(6), symDp(8))
                    isClickable = true; isFocusable = true
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { setMargins(symDp(3), 0, symDp(3), 0) }
                    setOnClickListener { view.base = i; paintPics(); run() }
                }
                pics.add(ch); picRow.addView(ch)
            }

            names.forEachIndexed { i, nm ->
                val ch = TextView(this).apply {
                    text = nm; textSize = 13f
                    gravity = android.view.Gravity.CENTER
                    setPadding(symDp(6), symDp(9), symDp(6), symDp(9))
                    isClickable = true; isFocusable = true
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .apply { setMargins(symDp(3), 0, symDp(3), 0) }
                    setOnClickListener {
                        view.disease = i; view.step = 0; paint(); run()
                    }
                }
                chips.add(ch); chipRow.addView(ch)
            }
            bPrev.setOnClickListener { if (view.step > 0) { view.step--; paint(); run() } }
            bNext.setOnClickListener {
                if (view.step < KsharTeachView.stepCount(view.disease) - 1) { view.step++; paint(); run() }
            }
            bClose.setOnClickListener { anim?.cancel(); dlg.dismiss() }

            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            row.addView(cap); row.addView(bPrev); row.addView(bNext); row.addView(bClose)
            panel.addView(picRow); panel.addView(chipRow); panel.addView(hint); panel.addView(row)
            root.addView(panel)
            dlg.setOnDismissListener { anim?.cancel() }
            dlg.setContentView(root)
            paint(); paintPics()
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }
            run()
        } catch (e: Throwable) {
            Toast.makeText(this, NoBengali.s("পর্দাটা খোলা গেল না — আবার চেষ্টা করুন"), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showStep(i: Int) {
        currentStep = i
        val scroll = findViewById<ScrollView>(R.id.stepScroll)
        val target = findViewById<android.view.View>(sectionIds.getOrElse(i) { R.id.secHistory })
        /* 🎨🔒 V776 (২৮.০৮.২০২৬, TK-অনুমোদিত ডিজাইন B) — ধাপ ১-এ স্ক্রল
           **একদম উপরে** নেওয়া হয়, `secHistory`-তে নয়। কারণ V776-এ রোগীর
           কার্ড ও ৫টা ধাপ-চিপ দুটোই এখন স্ক্রলের ভিতরে, `secHistory`-র
           **উপরে** বসে — আগের নিয়মে স্ক্রল করলে ওই দুটোই পর্দার বাইরে
           চলে যেত, অর্থাৎ TK যা চেয়েছেন (হেডার উপরে, নিচে ১-৫) সেটাই
           দেখা যেত না।
           ⛔ ধাপ ২-৫ আগের মতোই নিজের সেকশনে যায় — এক অক্ষরও বদলায়নি। */
        // 🟢 V1119 — ধাপটা যদি নিচের "SAVED" বাক্সের ভিতরে থাকে, চিপে চাপলে
        // বাক্সটা নিজে থেকেই খুলে যায়; নইলে চিপ চেপে কিছুই দেখা যেত না।
        if (target != null) openSavedGroupIfHolding(target)
        /* 🟢🔒 V1502 (১৫.০৯.২০২৬, TK-নির্দেশ) — TK: "উপরের যেকোনো সংখ্যায় চাপ
           দিলে সেই ঘরের ফরমটা ওপেন হতে হবে তো"। V886-এ এটাই accordion-এর
           নিজের মাথায় (নাম) চাপ দিলে হত ("হ্যাঁ, ধাপ ২ ও ৫-এর মতোই") — কিন্তু
           উপরের নম্বর-বৃত্তে (showStep) শুধু স্ক্রল হত, ভাঁজ খুলত না। এখন
           দুটোই একই আচরণ করে — নম্বরে চাপলে স্ক্রলের সাথে সাথে ভাঁজও খোলে,
           বন্ধ থাকলে। ইতিমধ্যে খোলা থাকলে ছোঁয়া হয় না (কেউ লেখার মাঝপথে
           পর্দা লাফাবে না)। */
        if (target != null) sectionFoldKey(target.id)?.let { openFold(it) }
        if (target != null) scroll.post {
            scroll.smoothScrollTo(0, if (i == 0) 0 else yInScroll(target))   // 🟢 V1119
        }
        // 🎨 (07.08.2026, প্রুফ-চেহারা) — নম্বর-বৃত্ত: চলতি ধাপ সবুজ+সাদা নম্বর,
        // বাকিগুলো ধূসর। নিচের লেবেলও সেই অনুযায়ী রঙ। (আগে বড় পিলে
        // bg_chip_seg_on_green / bg_chip_seg ব্যবহার হতো।)
        /* 🎨🔒 V897 — আগের ধাপগুলো সবুজ (হয়ে গেছে), চলতি ধাপ সবুজ+বলয়,
           পরেরগুলো সাদা-ধূসর; মাঝের রেখাও সেই অনুযায়ী রং বদলায়। */
        stepChips.forEachIndexed { idx, circle ->
            val active = idx == i
            val done = idx < i
            circle.background = stepDotBg(done = done, current = active)
            circle.text = (idx + 1).toString()
            circle.setTextColor(
                if (active || done) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#9AA6B4"))
            circle.setTypeface(null, android.graphics.Typeface.BOLD)
            stepChipLabels.getOrNull(idx)?.setTextColor(
                when {
                    active -> android.graphics.Color.parseColor("#0E6B4E")
                    done -> android.graphics.Color.parseColor("#5B7F72")
                    else -> android.graphics.Color.parseColor("#8A97A6")
                }
            )
            stepChipLabels.getOrNull(idx)?.setTypeface(
                null, if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            val doneCol = android.graphics.Color.parseColor("#12805C")
            val restCol = android.graphics.Color.parseColor("#E4EAF1")
            stepLineLeft.getOrNull(idx)?.setBackgroundColor(if (idx <= i) doneCol else restCol)
            stepLineRight.getOrNull(idx)?.setBackgroundColor(if (idx < i) doneCol else restCol)
        }
        // (এক-পেজ স্ক্রলে btnBack/btnNext isEnabled + smoothScrollTo(0,0)
        //  আর দরকার নেই — স্ক্রল উপরে target.top-এ হয়।)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // 🔵 TK-নির্দেশ (07.08.2026): ঠিকানা দু'লাইনে সাজানো — থানা-চিহ্নের
    // (PS: / P.S / P/S / Thana / থানা / Police Station) ঠিক আগে একটা লাইন-ব্রেক
    // বসে, তাই ১ম লাইনে গ্রাম/পোস্ট, ২য় লাইনে থানা/জেলা। চিহ্ন না পেলে ঠিকানা
    // আগের মতোই এক লাইনে থাকে (কিছু ভাঙে না)। ⛔ সেভ-হওয়া ঠিকানার আসল মান
    // বদলায় না — শুধু পর্দায় দেখানোর সময় ভাঙা হয়।
    private fun formatAddressTwoLines(addrIn: String): String {
        val addr = addrIn.uppercase(java.util.Locale.US)   // 🔠🔒 V1009 (০৩.০৯.২০২৬, TK-নির্দেশ) — শুধু দেখানোর সময় বড় হাতে; ডেটাবেসে কিছু বদলায় না।
        val markers = listOf("PS:", "P.S", "P/S", "Thana", "থানা", "Police Station")
        var idx = -1
        for (m in markers) {
            val i = addr.indexOf(m, ignoreCase = true)
            if (i > 0 && (idx == -1 || i < idx)) idx = i
        }
        if (idx <= 0) return addr
        val first = addr.substring(0, idx).trim().trimEnd(',').trim()
        val second = addr.substring(idx).trim()
        if (first.isBlank() || second.isBlank()) return addr
        return "$first\n$second"
    }

    /**
     * 🟢🔒🔒 V656 (২৫.০৮.২০২৬, TK-নির্দেশ, তিনটে প্রশ্ন করে নিশ্চিত হয়ে) —
     * "Doctor Note & Reminder" — ডাক্তার এখানে ভবিষ্যতের একটা কাজ/ওষুধের
     * কথা লিখে একটা তারিখ বাছেন; সেই তারিখের **আগের দিন সন্ধ্যা ৬টায়**
     * শুধু এই ডাক্তারকেই একটা মনে-করানো নোটিফিকেশন যায়
     * (`DoctorReminderWorker` — ঠিক `ExpectedTomorrowReminderWorker`-এর
     * প্রমাণিত একই ছাঁচে বানানো)।
     * ⛔ তারিখ আজকের আগের কখনো বাছা যাবে না (past-date guard, minDate)।
     */
    /**
     * 🟢🔒🔒 V671 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ — "ফর্ম আকারে থাকবে
     * না, একটা আইকন থাকবে... তারিখ বাছলে ক্যালেন্ডার, সময় বাছলে ঘড়ি, কোনোটাই
     * বাধ্যতামূলক নয়") — 🩺 আইকনে চাপলে `secDoctorReminder` কার্ডটাই
     * (XML-এ GONE হয়ে বসে থাকে) সাময়িকভাবে তার আসল জায়গা থেকে সরিয়ে একটা
     * পপ-আপে দেখানো হয়, বন্ধ করলে **ঠিক আগের জায়গায়** ফিরিয়ে দেওয়া হয়
     * (কোনো view তৈরি/মোছা হয় না — id/hint/সেভ-লজিক এক অক্ষরও বদলায়নি)।
     */
    private fun wireDoctorReminder() {
        val card = findViewById<androidx.cardview.widget.CardView>(R.id.secDoctorReminder)
        val originalParent = card.parent as android.view.ViewGroup
        val originalIndex = originalParent.indexOfChild(card)
        val btn = findViewById<TextView>(R.id.btnDoctorReminder)
        val dot = findViewById<android.view.View>(R.id.dotDoctorReminder)

        val tv = findViewById<TextView>(R.id.tvDoctorReminderDate)
        val tvTime = findViewById<TextView>(R.id.tvDoctorReminderTime)
        /* 🔴🔒 V700 — আইকনটা এখন লেখার পাশে নিজের ঘরে, তাই চাপ ধরার কাজটা
           **বাইরের বাক্সটা** করে (নইলে আইকনের উপরে চাপ দিলে কিছুই হত না)।
           লেখা বদলানো আগের মতোই `tv`/`tvTime`-এ। */
        val dateBox = findViewById<android.view.View>(R.id.boxDoctorReminderDate)
        val timeBox = findViewById<android.view.View>(R.id.boxDoctorReminderTime)
        /* 🩺🔒 V1109 (TK-নির্দেশ) — কোন ডাক্তারকে মনে করাবে, সেটা এখানেই বাছা যায়।
           ⛔ তালিকা প্রকল্পের নিজের `StaffDirectory` থেকেই — নতুন কোনো টেবিল বা
              ক্লাউড-পড়া লাগেনি।
           ⛔ প্রথমে এই রোগীর ব্রাঞ্চের ডাক্তাররা, তারপর বাকিরা — বাছতে সুবিধা হয়।
           ⛔ সবার উপরে "সব ডাক্তার" — কেউ নির্দিষ্ট করতে না চাইলে আগের আচরণই। */
        val whoBox = findViewById<android.view.View>(R.id.boxDoctorReminderWho)
        val tvWho = findViewById<TextView>(R.id.tvDoctorReminderWho)
        whoBox?.setOnClickListener {
            val docs = doctorChoices()
            val labels = (listOf(NoBengali.s("সব ডাক্তার")) + docs.map { it.first }).toTypedArray()
            val mobiles = listOf("") + docs.map { it.second }
            val current = mobiles.indexOf(doctorReminderForMobile).let { if (it < 0) 0 else it }
            val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("কোন ডাক্তারকে মনে করাবে?")))
                .setSingleChoiceItems(labels, current) { d, which ->
                    doctorReminderForMobile = mobiles[which]
                    paintReminderWho(tvWho)
                    d.dismiss()
                }
                .setNegativeButton(NoBengali.s("বাতিল"), null)
                .create()
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
        }
        paintReminderWho(tvWho)
        dateBox.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            if (doctorReminderDateIso.isNotBlank()) {
                try {
                    val parts = doctorReminderDateIso.split("-").map { it.toInt() }
                    cal.set(parts[0], parts[1] - 1, parts[2])
                } catch (_: Throwable) { }
            }
            android.app.DatePickerDialog(
                this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker,
                { _, y, m, d ->
                    val picked = java.util.Calendar.getInstance().apply { set(y, m, d) }
                    doctorReminderDateIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(picked.time)
                    tv.text = displayDateForReminder(doctorReminderDateIso)
                    tv.setTextColor(android.graphics.Color.parseColor("#101828"))
                },
                cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).apply {
                // ⛔ অতীতের তারিখ বাছা যাবে না — রিমাইন্ডার সবসময় ভবিষ্যতের জন্য।
                datePicker.minDate = System.currentTimeMillis() - 1000L
            }.show()
        }
        // 🟢🔒 V671 — নতুন সময়-বাছাই (ঘড়ি), তারিখের একই প্যাটার্নে।
        // ⛔ TK-নিয়ম: "কোনোটাই বাধ্যতামূলক নয়" — তাই কোনো ভ্যালিডেশন নেই।
        timeBox.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            if (doctorReminderTimeStr.isNotBlank()) {
                try {
                    val parts = doctorReminderTimeStr.split(":").map { it.toInt() }
                    cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0]); cal.set(java.util.Calendar.MINUTE, parts[1])
                } catch (_: Throwable) { }
            }
            android.app.TimePickerDialog(
                this,
                { _, h, min ->
                    doctorReminderTimeStr = "%02d:%02d".format(h, min)
                    tvTime.text = displayTimeForReminder(doctorReminderTimeStr)
                    tvTime.setTextColor(android.graphics.Color.parseColor("#101828"))
                },
                cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), false
            ).show()
        }

        fun refreshDot() {
            dot.visibility = if (findViewById<android.widget.EditText>(R.id.etDoctorReminderNote).text?.toString()
                    ?.trim().orEmpty().isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
        }
        refreshDot()

        /* 🟢🔒 V699 (২৬.০৮.২০২৬, TK-নির্দেশ — "এই pop up কে প্রফেশনাল বানিয়ে
           দিন"; ডেমো ছবি দেখিয়ে TK **"1 করুন"** বলেছেন) —

           ① পপ-আপে এখন অ্যাপের নিজের **সবুজ হেডার** (`PremiumAlert.header`,
              প্রজেক্টের ৬০+ পপ-আপে ব্যবহৃত লক করা চেহারা)। হেডারেই নাম
              লেখা থাকে বলে কার্ডের ভিতরের 🩺-শিরোনামের সারিটা পপ-আপে
              লুকানো হয়, বন্ধ করলে **আবার দেখানো হয়** (কার্ডটা তো নিজের
              জায়গায় ফিরে যায়, সেখানে ওই শিরোনাম দরকার)।

           ② নিজের **💾 Save** বোতাম। TK-এর প্রশ্ন ছিল *"save না করলে কাজ
              হবে কি করে"* — ঠিক কথা: এতদিন লেখাটা শুধু নিচের মূল SAVE
              চাপলেই জমত, কেউ লিখে Close করে বেরিয়ে গেলে **হারিয়ে যেত**।
              ⛔ নতুন কোনো সেভ-নিয়ম বানানো হয়নি — মূল Save যে তিনটে ঘরে
                 (`doctorReminderNote/Date/Time`) যে ভাবে লেখে, হুবহু সেই
                 একই `SupabaseClient.updateById("patients", …)`। তাই দুই
                 পথে একই মানই বসে, গোলমালের সুযোগ নেই।
           ⛔ V671-এর নিয়ম অক্ষত: কার্ডটা সরিয়ে এনে দেখানো হয় ও বন্ধ করলে
              **ঠিক আগের জায়গায়** ফিরিয়ে দেওয়া হয়; কোনো view তৈরি/মোছা নয়। */
        btn.setOnClickListener {
            originalParent.removeView(card)
            card.visibility = android.view.View.VISIBLE
            /* 🔴🔒 V700 (২৬.০৮.২০২৬, TK-এর ছবিতে ধরা — শিরোনাম দুবার দেখাচ্ছিল)।
               **আসল কারণ:** ঠিক উপরের লাইনে কার্ডটা পর্দা থেকে **খুলে নেওয়া
               হয়** (`removeView`), তার পরে `findViewById(...)` ডাকলে
               Activity-র গাছে ওই ঘরগুলো আর **থাকেই না** — তাই `null` ফিরত,
               আর শিরোনামটা কখনো লুকাত না। ⇒ এখন কার্ডের নিজের ভিতরেই খোঁজা
               হয় (`card.findViewById`)। */
            val headRow1 = card.findViewById<TextView?>(R.id.tvDoctorReminderHeading)
            val headRow2 = card.findViewById<TextView?>(R.id.tvDoctorReminderHeadingText)
            headRow1?.visibility = android.view.View.GONE
            headRow2?.visibility = android.view.View.GONE
            v952PaintSavedStrip(card)   // 🟢 V952 — পপ-আপেও সেভ-অবস্থা দেখা যায়
            val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(
                    com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "🩺 Doctor Note & Reminder")
                )
                .setView(card)
                /* 🟢🔒 V952 — আগে কিছু সেভ থাকলে বোতামের লেখা UPDATE (TK-এর প্রুফ)। */
                .setPositiveButton(
                    if (v952SavedNote.isNotBlank()) "\uD83D\uDCBE Update" else "\uD83D\uDCBE Save"
                ) { _, _ -> v952SaveOrWarn(card) }
                .setNegativeButton("Close", null)
                .create()
            dlg.setOnDismissListener {
                card.visibility = android.view.View.GONE
                headRow1?.visibility = android.view.View.VISIBLE
                headRow2?.visibility = android.view.View.VISIBLE
                (card.parent as? android.view.ViewGroup)?.removeView(card)
                originalParent.addView(card, originalIndex.coerceIn(0, originalParent.childCount))
                refreshDot()
            }
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
        }
    }

    /**
     * 🟢🔒 V699 — পপ-আপের 💾 Save। মূল SAVE-এর হুবহু একই তিনটে ঘর, একই
     * লেখার পথ (`SupabaseClient.updateById("patients", …)`), তাই দুই পথে
     * কখনো আলাদা মান বসতে পারে না।
     * ⛔ চেক-আপের বাকি কিছুই এখানে সেভ হয় না — শুধু এই তিনটে ঘর।
     * ⛔ রোগীর id না জানা গেলে কিছুই লেখা হয় না (ভুল সারিতে লেখার ঝুঁকি নেই)।
     */
    /* ══ 🟢🔒 V952 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) ════════════════════
       TK: *"একই জিনিস দুবার সেভ হলে তাতেও কোন ওয়ার্নিং আসলো না · একবার সেভ
       হওয়ার পরে বোঝার ক্ষমতা নেই কোন কিছু সেভ হয়ে আছে কিনা।"*
         (ক) কী সেভ আছে — সবুজ পট্টিতে পর্দাতেই দেখায়
         (খ) **হুবহু একই** নোট+তারিখ+সময় আবার সেভ করলে জিজ্ঞাসা করে
       ⛔ নতুন/বদলানো রিমাইন্ডারে আগের মতোই সরাসরি সেভ — একটাও বাড়তি চাপ নয়।
       ⛔ সেভের পথ (`saveDoctorReminderNow`) এক অক্ষরও বদলায়নি। */
    private var v952SavedNote: String = ""
    private var v952SavedDate: String = ""
    private var v952SavedTime: String = ""

    private fun v952PaintSavedStrip(card: android.view.View?) {
        try {
            val root: android.view.View = card ?: findViewById(R.id.secDoctorReminder)
            val tv: TextView = root.findViewById(R.id.tvDoctorReminderSaved) ?: return
            if (v952SavedNote.isBlank() && v952SavedDate.isBlank() && v952SavedTime.isBlank()) {
                tv.visibility = android.view.View.GONE
                return
            }
            val bits = listOfNotNull(
                v952SavedNote.ifBlank { null },
                v952SavedDate.ifBlank { null }?.let { displayDateForReminder(it) },
                v952SavedTime.ifBlank { null }?.let { displayTimeForReminder(it) }
            ).joinToString(" · ")
            tv.text = NoBengali.s("✓ আগেই সেভ করা আছে") + "\n" + bits
            tv.visibility = android.view.View.VISIBLE
        } catch (_: Throwable) { }
    }

    /** সেভের ঠিক আগে — হুবহু একই হলে জিজ্ঞাসা, নইলে আগের মতোই সোজা সেভ। */
    private fun v952SaveOrWarn(card: android.view.View) {
        val noteBox = card.findViewById<android.widget.EditText?>(R.id.etDoctorReminderNote)
        val note = noteBox?.text?.toString().orEmpty().trim()
        val same = note == v952SavedNote &&
            doctorReminderDateIso.trim() == v952SavedDate &&
            doctorReminderTimeStr.trim() == v952SavedTime &&
            (v952SavedNote.isNotBlank() || v952SavedDate.isNotBlank() || v952SavedTime.isNotBlank())
        if (!same) { saveDoctorReminderNow(card); return }
        val bits = listOfNotNull(
            v952SavedNote.ifBlank { null },
            v952SavedDate.ifBlank { null }?.let { displayDateForReminder(it) },
            v952SavedTime.ifBlank { null }?.let { displayTimeForReminder(it) }
        ).joinToString(" · ")
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "⚠ Already saved"))
            .setMessage(NoBengali.s("এই রোগীর জন্য হুবহু একই রিমাইন্ডার আগেই সেভ করা আছে। আবার সেভ করতে চান?") + "\n\n" + bits)
            .setPositiveButton(NoBengali.s("হ্যাঁ")) { _, _ -> saveDoctorReminderNow(card) }
            .setNegativeButton(NoBengali.s("না"), null)
            .create()
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    private fun saveDoctorReminderNow(card: android.view.View) {
        /* 🔴🔒 V700 — একই ফাঁদ, আরও মারাত্মক জায়গায়: পপ-আপ খোলা থাকা মানে
           কার্ডটা Activity-র গাছ থেকে খোলা। তখন `findViewById(...)` **null**
           ফেরাত, আর Save চাপলে অ্যাপ **বন্ধ হয়ে যেত (crash)**। এখন কার্ডের
           নিজের ভিতর থেকেই নেওয়া হয়। */
        val noteBox = card.findViewById<android.widget.EditText?>(R.id.etDoctorReminderNote)
        if (noteBox == null) {
            Toast.makeText(this, NoBengali.s("সেভ হয়নি — নিচের SAVE চাপুন"), Toast.LENGTH_LONG).show()
            return
        }
        val note = noteBox.text?.toString().orEmpty().trim()
        val dateIso = doctorReminderDateIso
        val timeStr = doctorReminderTimeStr
        if (note.isBlank() && dateIso.isBlank() && timeStr.isBlank()) {
            Toast.makeText(this, NoBengali.s("কিছু লেখা বা বাছা হয়নি"), Toast.LENGTH_SHORT).show()
            return
        }
        val pid = RoleSession.currentPatientId
        if (pid.isBlank()) {
            Toast.makeText(this, NoBengali.s("রোগী পাওয়া যায়নি — নিচের SAVE চাপুন"), Toast.LENGTH_LONG).show()
            return
        }
        Thread {
            val ok = try {
                SupabaseClient.updateById(
                    "patients", pid,
                    org.json.JSONObject()
                        .put("doctorReminderNote", note)
                        .put("doctorReminderDate", dateIso)
                        .put("doctorReminderTime", timeStr)
                        /* 🩺 V1109 (TK-নির্দেশ) — কোন ডাক্তারের ফোনে বাজবে।
                           ⛔ ফাঁকা = সব ডাক্তার (পুরনো আচরণ, কিছুই হারায় না)। */
                        .put("doctorReminderFor", doctorReminderForMobile)
                )
            } catch (_: Throwable) { false }
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) NoBengali.s("মনে করানোর নোট সেভ হয়েছে") else NoBengali.s("সেভ হয়নি — নিচের SAVE চাপুন"),
                    Toast.LENGTH_SHORT
                ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            }
        }.start()
    }

    /** 🩺🔒 V1109 — ডাক্তারদের তালিকা: এই রোগীর ব্রাঞ্চেরটা আগে, তারপর বাকিরা।
     *  ⛔ প্রকল্পের নিজের `StaffDirectory` থেকেই — নতুন টেবিল/ক্লাউড-পড়া নেই।
     *
     *  🔴🔒 V1120 (০৫.০৯.২০২৬ — TK-এর Studio-তে বিল্ড ভেঙেছিল, তাঁর ছবি থেকে ধরা):
     *  আগে এই ফাংশনটা **নাম বলে দিত** `List<...pilesclinic.native.StaffAccount>`।
     *  বিল্ডের সময় kapt প্রতিটা Kotlin ক্লাসের একটা **Java নকল** (stub) বানায়, আর
     *  Java-তে **`native` একটা সংরক্ষিত শব্দ** — তাই নকল ফাইলে ওই নামটা লিখতেই
     *  `<identifier> expected` · `illegal start of type` (মোট ৫টা ভুল) হয়ে বিল্ড
     *  থেমে যেত। ⇒ এখন ঘোষণায় ওই প্যাকেজের নাম আর নেই — শুধু **নাম ও নম্বরের জোড়া**
     *  ফেরে (ডাকার জায়গায় ঠিক এই দুটোই লাগত)।
     *  ⛔ ফাংশনের **ভিতরে** ওই নাম থাকলে কোনো সমস্যা নেই — kapt শরীরটা ফেলে দেয়;
     *     সমস্যা শুধু **ঘোষণায়** (return/parameter/property-র ধরন)। প্রকল্পের আর
     *     কোথাও এমন নেই — পুরোটা খুঁজে দেখা হয়েছে, আর পাহারাদারে নিয়ম [৯.৪৪] বসানো হলো।
     *  ⛔ তালিকা · ক্রম · আচরণ এক অক্ষরও বদলায়নি। */
    private fun doctorChoices(): List<Pair<String, String>> {
        val all = try {
            com.tkbiswas.pilesclinic.native.StaffDirectory.allAccounts()
                .filter { it.role == "doctor" }
        } catch (_: Throwable) { emptyList() }
        val br = RoleSession.currentPatientBranch.trim()
        val ordered = if (br.isBlank()) all
            else all.sortedByDescending { it.branch.equals(br, ignoreCase = true) }
        return ordered.map { it.name to it.mobile }
    }

    /** 🩺 V1109 — বাছা ডাক্তারের নাম ঘরে বসানো। ⛔ ফাঁকা হলে আগের ধূসর লেখা। */
    private fun paintReminderWho(tv: TextView?) {
        if (tv == null) return
        if (doctorReminderForMobile.isBlank()) {
            tv.text = NoBengali.s("সব ডাক্তার")
            tv.setTextColor(android.graphics.Color.parseColor("#8A93A0"))
            return
        }
        val nm = try {
            com.tkbiswas.pilesclinic.native.StaffDirectory.findAccount(doctorReminderForMobile)?.name
        } catch (_: Throwable) { null }
        tv.text = nm ?: doctorReminderForMobile
        tv.setTextColor(android.graphics.Color.parseColor("#101828"))
    }

    private fun displayDateForReminder(iso: String): String = try {
        val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(iso)
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(d!!)
    } catch (_: Throwable) { iso }

    // 🟢🔒 V671 — সময়ের ফরম্যাট (h.mm a — প্রজেক্টের 12-ঘণ্টা AM/PM নিয়ম)।
    private fun displayTimeForReminder(hhmm: String): String = try {
        val parts = hhmm.split(":").map { it.toInt() }
        val cal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, parts[0]); set(java.util.Calendar.MINUTE, parts[1]) }
        java.text.SimpleDateFormat("h.mm a", java.util.Locale.US).format(cal.time)
    } catch (_: Throwable) { hhmm }

    private fun bindPatientHeader() {
        /* 🔴🔒 V786 (২৮.০৮.২০২৬, TK-এর ফটো: নাম "Patient", ID "-") — রোগী
           চেনা না গেলে আগে চুপচাপ "Patient / - / -" দেখাত, ডাক্তার বুঝতেই
           পারতেন না যে পর্দাটা রোগী হারিয়ে ফেলেছে। এখন হেডারেই স্পষ্ট
           সতর্কতা ওঠে (Save-ও বন্ধ — উপরে btnSave দেখুন)।
           ⛔ কার্ডের নকশা/মাপ/রং কিচ্ছু বদলায়নি — শুধু লেখাটা। */
        val noPatient = RoleSession.currentPatientId.isBlank()
        val name = if (noPatient) "\u26A0\uFE0F Patient not loaded"
                   else RoleSession.currentPatientName.ifBlank { "Patient" }
        // 🔒 খাতার সারি B175 (TK, 30.07.2026): "Reg ID:"-এ raw আইডি (pat_...)
        // দেখাত। এখন `displayId()` — মানুষ-পড়া-যায় কোড থাকলে সেটাই দেখাবে।
        // ⛔ নিচের ক্লাউড-খোঁজাও অক্ষত থাকল — ওটা আগে থেকেই মানুষ-পড়া-যায়
        // কোড (`patientId=eq.`) প্রথমে খোঁজে, তারপর raw আইডি (`id=eq.`) —
        // তাই এই বদলে খোঁজাটা বরং আরও সঠিক হলো, কিছু ভাঙল না।
        val pid = RoleSession.displayId()
        val branch = RoleSession.currentPatientBranch
        // 🔒 TK-LOCKED DESIGN (04.08.2026, ফটো-প্রুফে "লক করুন") — Patient
        // Timeline-এর হেডারের হুবহু একই তথ্য-ক্রম: মোবাইল (কল-বোতাম সহ),
        // নাম, Branch-Disease, Sex-Age, Address, ID।
        // 🔴🎨🔒 B431 — নাম কখনো দু'লাইনে/ব্রেক না হওয়ার জন্য Patient ID-র
        // (FollowUpActivity) প্রমাণিত একই autosize কৌশল, পূর্ণ কার্ড ও
        // সংকুচিত বার — দুই জায়গার নামেই।
        val tvName = findViewById<TextView>(R.id.tvPatientName)
        tvName.text = name
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            tvName, 11, 17, 1, android.util.TypedValue.COMPLEX_UNIT_SP
        )
        val tvNameMini = findViewById<TextView>(R.id.tvPatientNameMini)
        tvNameMini.text = name
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            tvNameMini, 9, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP
        )
        findViewById<TextView>(R.id.tvPatientReg).text = pid.ifBlank { "-" }
        findViewById<TextView>(R.id.tvPatientRegMini).text = pid.ifBlank { "-" }
        findViewById<TextView>(R.id.tvPatientBranchDisease).text = branch.ifBlank { "-" }
        if (pid.isBlank()) return
        lifecycleScope.launch {
            val p = withContext(Dispatchers.IO) {
                try {
                    val enc = java.net.URLEncoder.encode(pid, "UTF-8")
                    /* 🔴🔒 V794 — সারিটা **ছবি ছাড়া** পড়া হয়; ছবিটা নিচে
                       `PatientPhotoCache` থেকে আসে (জমা থাকলে বিনা খরচে,
                       নইলে শুধু ওই এক সারির `id,photo`)। ⛔ হেডারের ছবি,
                       A4 কাগজের ছবি ও ৩-চাপে ঘোরানো — তিনটেই অটুট। */
                    /* 🩺🔒 V839 — নিজের ঘরটাও চাওয়া হয় (`nextVisitPlan`)।
                       ⛔ শেয়ার করা `PATIENT_NO_PHOTO_COLS` **ছোঁয়া হয়নি** —
                          ওটা প্রকল্পের ১৩টা জায়গায় চলে, তার একটায় ৫০০ সারি
                          পড়া হয়; ওখানে ঘরটা যোগ করলে অকারণে Egress বাড়ত।
                          এখানে সারি মাত্র **একটাই**, তাই খরচ নগণ্য। */
                    val colsWithPlan = SupabaseClient.PATIENT_NO_PHOTO_COLS + ",${NextVisitPlan.FIELD}"
                    var rows = SupabaseClient.fetchListSlim("patients", "patientId=eq.$enc", 1,
                        colsWithPlan)
                    if (rows.length() == 0) rows = SupabaseClient.fetchListSlim("patients",
                        "id=eq.$enc", 1, colsWithPlan)
                    if (rows.length() > 0) rows.getJSONObject(0) else null
                } catch (_: Exception) { null }
            } ?: return@launch
            val mobile = p.s("mobile")
            val age = p.s("age")
            val sex = p.s("sex")
            val disease = p.s("disease")
            val address = p.s("address")
            /* 🔴🔒 V794 — ছবিটা আর সারির সঙ্গে নামে না; জমা থাকলে ফোন থেকেই,
               নইলে একবারই ক্লাউড থেকে (শুধু `id,photo`)। ⛔ ছবি হারায় না। */
            val photo = com.tkbiswas.pilesclinic.native.PatientPhotoCache.photoFor(
                applicationContext, p.s("id").ifBlank { pid }, p.s("updatedAt"))
            // 🆕 (07.08.2026) — A4 রিপোর্টের জন্য একই তথ্য মনে রাখা (কোনো নতুন কল নয়)।
            patMobile = mobile; patAge = age; patSex = sex; patDisease = disease; patAddress = address; patPhoto = photo
            // 🟢🔒 V656 (২৫.০৮.২০২৬, TK-নির্দেশ) — আগে থেকে বসানো Doctor Note &
            // Reminder থাকলে, ফর্ম আবার খুললে সেটা যেন মুছে না যায় — সেভ করা
            // মান এখানেই ফিরিয়ে বসানো হচ্ছে।
            val savedReminderNote = p.s("doctorReminderNote")
            val savedReminderDate = p.s("doctorReminderDate")
            // 🟢🔒 V671 — সময়ও একইভাবে ফিরিয়ে বসানো হয়।
            val savedReminderTime = p.s("doctorReminderTime")
            /* 🩺 V1109 — বাছা ডাক্তারও ফিরিয়ে বসানো হয়। ⛔ ঘরটা না থাকলে
               (SQL এখনো চালানো হয়নি) ফাঁকাই আসে ⇒ আচরণ হুবহু আগের মতোই। */
            doctorReminderForMobile = p.s("doctorReminderFor").trim()
            paintReminderWho(findViewById(R.id.tvDoctorReminderWho))
            findViewById<android.widget.EditText>(R.id.etDoctorReminderNote).setText(savedReminderNote)
            if (savedReminderDate.isNotBlank()) {
                doctorReminderDateIso = savedReminderDate
                val tv = findViewById<TextView>(R.id.tvDoctorReminderDate)
                tv.text = displayDateForReminder(savedReminderDate)
                tv.setTextColor(android.graphics.Color.parseColor("#101828"))
            }
            if (savedReminderTime.isNotBlank()) {
                doctorReminderTimeStr = savedReminderTime
                val tvT = findViewById<TextView>(R.id.tvDoctorReminderTime)
                tvT.text = displayTimeForReminder(savedReminderTime)
                tvT.setTextColor(android.graphics.Color.parseColor("#101828"))
            }
            findViewById<android.view.View>(R.id.dotDoctorReminder).visibility =
                if (savedReminderNote.trim().isNotBlank()) android.view.View.VISIBLE else android.view.View.GONE
            /* 🟢🔒 V952 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — কী সেভ আছে সেটা
               মনে রাখা হয়, যাতে (ক) পর্দায় দেখানো যায়, (খ) হুবহু একই জিনিস আবার
               সেভ করলে সতর্ক করা যায়। ⛔ কোনো ঘরের মান বদলানো হয় না। */
            v952SavedNote = savedReminderNote.trim()
            v952SavedDate = savedReminderDate.trim()
            v952SavedTime = savedReminderTime.trim()
            v952PaintSavedStrip(null)
            val tvMobile = findViewById<TextView>(R.id.tvPatientMobile)
            tvMobile.text = mobile.ifBlank { "—" }
            findViewById<TextView>(R.id.tvPatientMobileMini).text = mobile.ifBlank { "—" }
            /* 🔵 V584 (২৩.০৮.২০২৬, TK-নির্দেশ): *"হেডারে কল এবং হিস্টোরির যে
               আইকন এটা যেন না থাকে ... এখানে যে হিস্টরি থাকবে এটা হবে চেকআপ
               হিস্টরি"* ⇒ কল বোতামটা layout থেকেই সরানো হয়েছে, তাই এখানে
               তার কোনো listener-ও নেই। ⛔ `CallChooser` ক্লাসটা মোছা হয়নি —
               প্রকল্পের অন্য পর্দাগুলো (Doctor Queue, Follow-up ইত্যাদি)
               আগের মতোই সেটা ব্যবহার করে। */
            /* 🔵 V584 — এই সারিটা **এইমাত্র উপরে আনা হয়েছে** (patients),
               তাই Check-up History-র জন্য **একটাও বাড়তি Supabase কল লাগে না**
               (TK: *"আমি ফ্রি প্লানে চালাতে চাই"*)। */
            hdrDoctorComplete = p.optBoolean("doctorComplete", false)
            hdrDoctorNote = noteObjOf(p)
            findViewById<TextView>(R.id.btnPatientHistory).setOnClickListener {
                openCheckupHistory()
            }
            /* 🩺🔒 V839 — NEXT VISIT PLAN অংশটা চালু করা।
               ⛔ সারিটা **এইমাত্র এই রোগীরই** id দিয়ে আনা হয়েছে (উপরে
                  `patientId=eq.$enc`), তাই B437-এর ক্রস-রোগী ফাঁদ এখানে নেই। */
            setupNextVisitPlan(p)
            /* 🔵🔒 V559 (২২.০৮.২০২৬, TK-অনুমোদিত: *"হ্যাঁ"*) — আগে সেভ করা
               চেকআপ ফর্মে ফিরিয়ে আনা।

               এতদিন ফোনে ফিরত না (B437 সারানোর সময় `populate()`-এর ডাক তুলে
               দেওয়া হয়েছিল), অথচ ওয়েবে ফিরত। এখন ওয়েব যেখানে রাখে ঠিক
               সেখান থেকেই পড়া হয় — `patients.doctorFullNote`।

               🔒 B437 যেন না ফেরে: সারিটা এইমাত্র **এই রোগীরই** id দিয়ে আনা
                  হয়েছে (উপরে `patientId=eq.$enc`), তাই অন্য রোগীর তথ্য এখানে
                  আসার পথ নেই। তবু নিচে আরেকবার id মিলিয়ে দেখা হয়।
               ⛔ বাড়তি কোনো query লাগেনি — সারিটা এমনিতেই আনা হচ্ছিল।
               ⛔ ডাক্তার কিছু টাইপ করে ফেলে থাকলে হাত দেওয়া হয় না। */
            try {
                val rowKey = p.s("patientId").ifBlank { p.s("id") }
                val samePatient = rowKey.isNotBlank() &&
                    (rowKey.equals(pid, true) || p.s("id").equals(pid, true))
                // সারিতে ঘরটা সাধারণত JSON হয়ে আসে; কোনো পুরনো সারিতে যদি
                // লেখা (string) হয়ে থাকে, সেটাও পড়ে নেওয়া হয় — নইলে সেই
                // রোগীর তথ্য চুপচাপ ফিরত না।
                val noteObj = p.optJSONObject("doctorFullNote")
                    ?: try {
                        val raw = p.s("doctorFullNote")
                        if (raw.trimStart().startsWith("{")) org.json.JSONObject(raw) else null
                    } catch (_: Throwable) { null }
                if (samePatient && noteObj != null && noteObj.length() > 0 &&
                    etComplaint.text.isNullOrBlank() && !restoredOnce) {
                    restoredOnce = true
                    populate(CheckupNoteJson.fromMap(jsonToMap(noteObj)))
                    /* 🟢🔒 V1504 (১৫.০৯.২০২৬, TK-নির্দেশ — "History/Clinical/
                       Estimate/Photo জীবনে একবারই করা হয়, বোঝাই যায় না আগে
                       হয়েছে কিনা") — আগে সেভ করা তথ্য ফিরে এলেই এই ধাপগুলো
                       সঙ্গে সঙ্গে ধূসর/SAVED বাক্সে নামে, Save না চেপেও।
                       ⛔ Doctor's Remark/Today's Treatment/Next Visit Plan
                       এই তালিকায় নেই (savedGroupIds দেখুন) — TK: "এগুলো
                       পেশেন্ট যতবার আসে ততবারই চেঞ্জ হতে পারে", তাই সবসময়
                       খোলা/স্বাভাবিক থাকে। */
                    savedInThisScreen = true
                    findViewById<android.view.View>(R.id.secSavedGroup)?.post { refreshSavedGroup() }
                }
            } catch (_: Throwable) { }
            // 🟢🔒 V676 — আজকের নিজের Checkup এডিট করতে খোলা হলে, উপরের
            // ড্রাফট-রিস্টোরের ঠিক পরেই সেই সেভ-করা মান দিয়ে ফর্ম ভরে দেওয়া
            // হয় (ড্রাফটের চেয়ে অগ্রাধিকার — এটাই আসল সেভ-করা সারি)।
            if (editingMedicalId.isNotBlank()) {
                val editJson = intent.getStringExtra(EXTRA_EDIT_MEDICAL_JSON).orEmpty()
                val record = checkupRecordFromJsonStringOrNull(editJson)
                if (record != null) {
                    restoredOnce = true
                    populate(record)
                    savedInThisScreen = true   // 🟢🔒 V1504 — উপরের একই নিয়ম, এডিট-মোডেও
                    findViewById<android.view.View>(R.id.secSavedGroup)?.post { refreshSavedGroup() }
                } else {
                    // ⛔ JSON না মিললে (কখনো ঘটার কথা না, তবু নিরাপদ থাকতে)
                    // edit-mode বন্ধ — Save তখন আগের মতোই নতুন সারি বানাবে,
                    // কোনো ডেটা এলোমেলো/হারানোর ঝুঁকি নেই।
                    editingMedicalId = ""
                }
            }

            val branchDisease = listOf(branch, disease).filter { it.isNotBlank() }.joinToString(" - ")
            if (branchDisease.isNotBlank()) findViewById<TextView>(R.id.tvPatientBranchDisease).text = branchDisease
            val tvSexAge = findViewById<TextView>(R.id.tvPatientSexAge)
            val sexAge = listOf(sex, age).filter { it.isNotBlank() }.joinToString("-")
            if (sexAge.isNotBlank()) {
                tvSexAge.text = sexAge
                tvSexAge.visibility = android.view.View.VISIBLE
            } else {
                tvSexAge.visibility = android.view.View.GONE
            }
            /* 🔵 V553 (২২.০৮.২০২৬, TK-নির্দেশ): কাগজের **"Refd. by"** ঘরটা হেডারে।
               ⛔ তথ্যটা **এই একই সারিতেই** আগে থেকে আছে (রেজিস্ট্রেশনের `refBy`
                  ও `refDoctor` — RegistrationActivity.kt:1014,1050) — তাই
                  **Supabase-এ একটাও বাড়তি query নেই**, নতুন কলাম/SQL-ও লাগেনি।
               ⛔ "Dr. Visit" হলে সঙ্গে ডাক্তারের নামটাও দেখায় (দুটোই সেভ করা তথ্য,
                  কিছু বানানো হয়নি)। কিছুই না থাকলে **লাইনটাই দেখায় না** — তাই
                  পুরোনো রোগীর কার্ড আগের মতোই থাকে।
               ⛔ উপরের কোনো লাইন সরানো হয়নি। */
            val refBy = p.s("refBy")
            val refDoctor = p.s("refDoctor")
            val refText = listOf(refBy, refDoctor).map { it.trim() }.filter { it.isNotBlank() }.joinToString(" · ")
            val tvRefBy = findViewById<TextView>(R.id.tvPatientRefBy)
            if (refText.isNotBlank()) {
                tvRefBy.text = "Ref By: $refText"
                tvRefBy.visibility = android.view.View.VISIBLE
            } else {
                tvRefBy.visibility = android.view.View.GONE
            }

            val tvAddress = findViewById<TextView>(R.id.tvPatientAddress)
            if (address.isNotBlank()) {
                // 🔵 TK-নির্দেশ (07.08.2026): ঠিকানা দু'লাইনে — ১ম লাইনে গ্রাম/
                // পোস্ট, ২য় লাইনে থানা/জেলা। (আগে: এক লাইনেই বসত, tvAddress.text = address)
                tvAddress.text = formatAddressTwoLines(address)
                tvAddress.visibility = android.view.View.VISIBLE
            } else {
                tvAddress.visibility = android.view.View.GONE
            }
            /* 🔵🔒 V539 (২২.০৮.২০২৬, TK-নির্দেশ): *"occupation ফর্ম থেকে সরিয়ে উপরে
               বয়সের পাশে রাখুন।"* ⇒ "Male-55 · Business"। ওই লেখাটায় চাপ দিলেই
               পেশা বাছার তালিকা খোলে, আর সেটা ফর্মের সেই **পুরোনো ঘরেই** বসে
               (`spOccupation`, শুধু লুকানো) — তাই সেভ/পড়া কিছুই বদলায়নি। */
            refreshSexAgeOccupation()
            /* ⛔ লুকানো (GONE) Spinner-এ `performClick()` ভরসাযোগ্য নয় — তালিকা
               নাও খুলতে পারে। তাই নিজেরই একটা তালিকা, আর বাছাইটা সেই
               পুরোনো Spinner-এই বসে (সেভের পথ এক অক্ষরও বদলায়নি)। */
            tvSexAge.setOnClickListener { askOccupation() }
            spOccupation.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: android.view.View?, p2: Int, p3: Long) {
                    refreshSexAgeOccupation()
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            })
            if (photo.isNotBlank()) {
                val bmp = PhotoUtils.decodeDataUrl(photo)
                if (bmp != null) {
                    val iv = findViewById<ImageView>(R.id.ivPatientPhoto)
                    iv.setImageBitmap(bmp)
                    iv.visibility = android.view.View.VISIBLE
                    findViewById<TextView>(R.id.ivPatientPhotoBlank).visibility = android.view.View.GONE
                }
            }
            wireCheckupPhotoRotate()   // 🔵 V539: ছবিতে তিনবার চাপ = ঘুরবে ও সেভ হবে
            autofillFromRegistration(p)
        }
    }

    /** Web parity (doctorCheck): pre-fill Basic History + Previous Treatment
     *  from the Registration record. Only fills fields still empty, so a prior
     *  saved check-up or the doctor's own edits are never overwritten. */
    private fun autofillFromRegistration(p: org.json.JSONObject) {
        fun first(vararg keys: String): String {
            for (k in keys) {
                val v = p.optString(k)
                if (v.isNotBlank() && v != "null") return v
            }
            return ""
        }
        fun fill(et: EditText, value: String) {
            if (et.text.isNullOrBlank() && value.isNotBlank()) et.setText(value)
        }
        // 🔴 TK-REPORTED (04.08.2026, ছবিসহ — Chief Complaint/Duration/
        // Occupation Registration সেভ হওয়ার সময় অটোফিল হওয়ার কথা ছিল,
        // হয়নি): Occupation আগে EditText ছিল, এখন Spinner — তাই এখানে
        // পুরনো fill(etOccupation,...) কাজ করত না, কম্পাইলই হতো না।
        // এখন নতুন fillSpinner() — শুধু তখনই বসে যখন Spinner এখনো
        // ফাঁকা/প্রথম-অপশনে আছে (ডাক্তার নিজে কিছু বেছে না থাকলে),
        // Registration-এর সেভ করা মান তালিকায় থাকলে তবেই। ⛔ ডাক্তার
        // নিজে কিছু বেছে থাকলে (বা তালিকায় নেই এমন মান হলে) কিছুই
        // বদলায় না।
        fun fillSpinner(sp: Spinner, options: List<String>, value: String) {
            if (value.isBlank()) return
            if (sp.selectedItemPosition > 0) return
            val idx = options.indexOfFirst { it.equals(value, ignoreCase = true) }
            if (idx > 0) sp.setSelection(idx)
        }
        fill(etComplaint, first("complaint", "diagnosis", "disease"))
        fill(etDuration, first("sinceWhen", "duration"))
        fillSpinner(spOccupation, occupationOptions, first("occupation"))
        fill(etPrevTreatment, first("previousTreatment", "prevTreatment", "medicalHistory"))
        // 🔵 B622: Result/Spent/Treatment Duration ঘর বাদ — এদের আর লোড করা হয় না।
    }

    // 🔴🔒 V1635 (TK-রিপোর্ট, ছবিসহ, ২০.০৯.২০২৬) — "এখানে ছবি তুলেছি, ডিলিট
    // কেন করতে পারছি না"। আগে Before/During/After ছবি একবার তুললে সরানোর
    // কোনো উপায়ই ছিল না — শুধু আবার তুলে/বেছে ওভাররাইট করা যেত। এখন ছবি
    // থাকলে এই একই চুজারে "🗑️ Remove Photo" যোগ হয়।
    private fun currentPhotoData(target: Int): String = when (target) {
        0 -> beforePhotoData
        1 -> duringPhotoData
        else -> afterPhotoData
    }

    private fun removePhoto(target: Int) {
        val iv = when (target) { 0 -> ivBeforePhoto; 1 -> ivDuringPhoto; else -> ivAfterPhoto }
        iv.setImageDrawable(null)
        when (target) {
            0 -> beforePhotoData = ""
            1 -> duringPhotoData = ""
            else -> afterPhotoData = ""
        }
    }

    private fun showPhotoDialog(target: Int) {
        currentPhotoTarget = target
        // ANAT_PHOTO_TARGET (রোগের ছবির তালিকায় নতুন ছবি জোড়া) কখনো একক
        // before/during/after ঘরে বসে না — তাই ওখানে এই "Remove" বসানো হয়নি।
        val hasPhoto = target != ANAT_PHOTO_TARGET && currentPhotoData(target).isNotBlank()
        val options = if (hasPhoto) arrayOf("📷 Camera", "🖼️ Gallery", "🗑️ Remove Photo") else arrayOf("📷 Camera", "🖼️ Gallery")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Treatment Photo"))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCameraWithPermission()
                    1 -> pickImage.launch("image/*")
                    else -> removePhoto(target)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
    }

    // BUG FIX (audit): CAMERA is a runtime-dangerous permission. Since the app
    // manifest declares android.permission.CAMERA, launching TakePicture()
    // without first holding the granted permission throws a SecurityException
    // and crashes this screen on first use. Registration already had this
    // check (launchCameraWithPermission); Doctor Check-up was missing it.
    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
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
            val file = java.io.File(dir, "checkup_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            cameraPhotoUri = uri
            takePicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera could not open", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraPhotoUri
            if (success && uri != null) applyPhoto(uri)
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) applyPhoto(uri)
        }

    private fun applyPhoto(uri: Uri) {
        lifecycleScope.launch {
            val dataUrl = withContext(Dispatchers.IO) { PhotoUtils.encodeResized(this@DoctorCheckupActivity, uri) }
            if (dataUrl == null) {
                Toast.makeText(this@DoctorCheckupActivity, "Could not get the image", Toast.LENGTH_SHORT).show()
                return@launch
            }
            when (currentPhotoTarget) {
                0 -> { beforePhotoData = dataUrl; showThumb(ivBeforePhoto, dataUrl) }
                1 -> { duringPhotoData = dataUrl; showThumb(ivDuringPhoto, dataUrl) }
                ANAT_PHOTO_TARGET -> addAnatomyPicture(dataUrl)   // 🔵 V573
                else -> { afterPhotoData = dataUrl; showThumb(ivAfterPhoto, dataUrl) }
            }
        }
    }

    /**
     * 🔵🔒 V573 — ক্যামেরা/গ্যালারি থেকে নেওয়া ছবিটা **রোগের ছবির তালিকায়** বসায়।
     * ডাক্তার নাম দিতে পারেন; না দিলে "নিজের তোলা ছবি"।
     * ⛔ আগে ফোনে বসে (সঙ্গে সঙ্গে দেখা যায়), তারপর পিছনে ক্লাউডে যায় —
     *    ইন্টারনেট না থাকলেও কাজ আটকায় না।
     */
    private fun addAnatomyPicture(dataUrl: String) {
        val input = android.widget.EditText(this).apply {
            // 🔴🔒 V610 (২৪.০৮.২০২৬, TK-নির্দেশ — গভীর যাচাই) — EditText-এর
            // ভিতরের লেখা কখনো sweep() ছোঁয় না (উপরের নিয়ম, রোগীর আসল
            // বাংলা ডেটা নষ্ট হওয়া এড়াতে), তাই এখানে হাতেই মোড়া হলো।
            setText(NoBengali.s("নিজের তোলা ছবি"))
            setSelection(text.length)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(symDp(14), symDp(10), symDp(14), symDp(10))
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(NoBengali.s("Picture name"))   /* 🔤 V728 */
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add") { _, _ ->
                val label = input.text?.toString()?.trim().orEmpty().ifBlank { NoBengali.s("নিজের তোলা ছবি") }
                val row = AnatomyPictureRepository.newPhotoRow(label, dataUrl, currentUserMobile())
                AnatomyPictureRepository.saveLocal(this, row)
                BackgroundWork.run { AnatomyPictureRepository.pushCloud(row) }
                val strip = findViewById<android.widget.LinearLayout>(R.id.anatomyStrip)
                val view = anatomyView
                if (strip != null && view != null) {
                    buildAnatomyStrip(strip, view)
                    val key = AnatomyModel.CLOUD_PREFIX + row.optString("id", "")
                    view.setPictureBitmap(key, PhotoUtils.decodeDataUrl(dataUrl))
                    paintAnatomyThumbs(key)
                }
                Toast.makeText(this, "Photo added", Toast.LENGTH_SHORT).show()
            }
            .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
    }

    /** কে যোগ করলেন — শুধু হিসাব রাখার জন্য, কোনো নিয়মে লাগে না।
        (এই পর্দাটা `createdBy`-র জন্য আগে থেকেই এটাই ব্যবহার করে।) */
    private fun currentUserMobile(): String = try {
        NativeSession.current(this)?.mobile ?: ""
    } catch (_: Throwable) { "" }

    private fun showThumb(iv: ImageView, dataUrl: String) {
        val bmp = PhotoUtils.decodeDataUrl(dataUrl)
        if (bmp != null) iv.setImageBitmap(bmp)
    }

    /** TK APPROVED (2026-07-15): each option now renders as a colourful
     *  rounded card (icon + label + tinted checkbox) instead of a plain
     *  default CheckBox, with a light press animation on tap. The CheckBox
     *  objects themselves (added to `into`) are completely unchanged --
     *  checkedText(), enable/disable, and restore-on-load all keep working
     *  exactly as before, since they only ever read isChecked/text. */
    /**
     * @param compact 🔴 V501 (TK-নির্দেশ ২১.০৮.২০২৬, ফটো-প্রুফ অনুমোদিত) —
     *        `true` হলে চিপগুলো Quick Action-এর বোতামের হুবহু এক মাপে বসে
     *        (উচ্চতা ৩২dp · লেখা ১১.৫sp · কোণ ৭dp)। শুধু Investigation-এ
     *        ব্যবহার হয়; Visual/DRE আগের মতোই (ডিফল্ট `false`)।
     *        ⛔ টিকের মান · `tag` · সেভ-লজিক কিছুই বদলায় না — শুধু মাপ ও রং।
     */
    private fun buildChecks(
        group: LinearLayout, options: List<String>, into: MutableList<CheckBox>,
        icons: Map<String, String> = emptyMap(), accentHex: String = "#0B4F2A",
        bnLabels: Map<String, String> = emptyMap(),
        compact: Boolean = false
    ) {
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        val accent = android.graphics.Color.parseColor(accentHex)
        val lightBg = android.graphics.Color.argb(28, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent))
        var row: LinearLayout? = null
        options.forEachIndexed { idx, label ->
            if (idx % 2 == 0) {
                row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = dp(if (compact) 4 else 6) }
                }
                group.addView(row)
            }
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                // 🎨 (07.08.2026, প্রুফ-হুবহু) — পিলের রং নিচে paintChip()-এ বসে
                // (বাছাই-অবস্থা অনুযায়ী ধূসর/সবুজ), তাই এখানে স্থির background নেই।
                if (compact) setPadding(dp(2), 0, dp(2), 0)
                else setPadding(dp(10), dp(8), dp(10), dp(8))
                val lp = LinearLayout.LayoutParams(
                    0,
                    if (compact) dp(32) else LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                lp.setMargins(dp(if (compact) 3 else 4), 0, dp(if (compact) 3 else 4), 0)
                layoutParams = lp
                if (compact) gravity = android.view.Gravity.CENTER
                isClickable = true; isFocusable = true
            }
            // 🔴🔒 TK-নির্দেশ (06.08.2026): প্রতিটা চেকবক্সের পাশের ছোট
            // ইমোজি/ডট (রঙিন গোল আইকন, বা আইকন না থাকলে "•") সম্পূর্ণ বাদ
            // — "এগুলো কেন থাকবে" বলার পরে। ⛔ `icons`/`visualIcons`/
            // `dreIcons`/`investigationIcons` ম্যাপ ও প্যারামিটার মোছা হয়নি
            // (dead code হিসেবে থাকল, পরে দরকার হলে ফেরানো যাবে) — শুধু
            // এখানে ব্যবহার বন্ধ করা হলো, buildChecks তিনটে গ্রুপেই
            // (Visual/DRE/Investigation) একই পরিবর্তন প্রযোজ্য।
            val cb = CheckBox(this).apply {
                // 🔴🔴🔒 TK-নির্দেশ (06.08.2026, দ্বিতীয়বার স্পষ্ট করে বলার
                // পরে — "প্রতিবার পাইলস লেখার পাশে একটা ডট কেন") — এতদিন
                // ধরে থাকা " · " ফুটকি-চিহ্নটাই আসল সমস্যা ছিল (05.08.2026-এ
                // ভুল করে শুধু "Fistula Opening"/"Bleeding"-এ বাদ দেওয়া
                // হয়েছিল, বাকি সবগুলোতে থেকে গিয়েছিল)। এখন Visual/DRE/
                // Investigation — তিনটে গ্রুপের **সব** চেকবক্সেই ইংরেজি ও
                // বাংলা নতুন লাইনে (উপর-নিচে), " · " আর কোথাও নেই।
                text = if (compact) label
                       else bnLabels[label]?.let { bn -> "$label\n$bn" } ?: label
                tag = label
                textSize = if (compact) 11.5f else 12.5f
                if (compact) {
                    gravity = android.view.Gravity.CENTER
                    isSingleLine = true
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                buttonDrawable = null   // 🎨 (07.08.2026, প্রুফ-হুবহু) চেকবক্সের বর্গ-চিহ্ন লুকানো
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            // 🎨 (07.08.2026, প্রুফ-হুবহু, TK-অনুমোদিত) — প্রুফের ধূসর পিল: বাছলে সবুজ
            // ভরাট+সবুজ লেখা, নইলে ধূসর বর্ডার+ধূসর লেখা (লোড ও টগল দুটোতেই চলে)।
            // ⛔ cb.tag / isChecked / checkedText সেভ-লজিক অপরিবর্তিত।
            run {
                val greyStroke = android.graphics.Color.parseColor("#D0D5DD")
                val greyText = android.graphics.Color.parseColor("#475467")
                val greenLine = android.graphics.Color.parseColor("#12805C")
                val greenFill = android.graphics.Color.parseColor("#EAF7F0")
                fun paintChip(on: Boolean) {
                    card.background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(if (on) greenFill else android.graphics.Color.TRANSPARENT)
                        cornerRadius = dp(if (compact) 7 else 16).toFloat()
                        setStroke(dp(1), if (on) greenLine else greyStroke)
                    }
                    cb.setTextColor(if (on) greenLine else greyText)
                    cb.setTypeface(null, if (on) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                }
                paintChip(cb.isChecked)
                cb.setOnCheckedChangeListener { _, isC -> paintChip(isC) }
            }
            card.addView(cb)
            // Light press-bounce animation on tap (same proven pattern already
            // used for the Clinical Document grid elsewhere in the app) --
            // the CheckBox's own default toggle behaviour is untouched.
            card.setOnClickListener {
                card.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                    card.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }.start()
                cb.toggle()
            }
            row!!.addView(card)
            into.add(cb)
        }
    }

    /** 🔵 V541: এখন যেটা সেভ হয়, আর পুরোনো যেগুলোও মেনে নিতে হবে। */
    private val FISTULA_TAG_NOW = "Fistula Per CM"
    private val FISTULA_TAGS = listOf("Fistula Per CM", "Fistula Per Inch", "Fistula Per ইঞ্চি")

    private fun checkedText(list: List<CheckBox>): String =
        list.filter { it.isChecked }.joinToString(", ") { (it.tag as? String) ?: it.text.toString() }

    /** Best-effort: finds this patient's Supabase row (by patientId, falling
     *  back to the row id) and sets doctorComplete=true so they drop out of the
     *  Doctor Queue. Runs on an IO thread; swallows all errors so it can never
     *  break the check-up save. */
    /* 🔴🔒 V1362 (১১.০৯.২০২৬, পুরো-প্রজেক্ট দেরি-অডিট) — এই পড়াটা (`fetchListSlim`,
       কখনো retry/queue করে না) নেটের দুর্বল মুহূর্তে ব্যর্থ হলে ফাংশনটা চুপচাপ
       `return` করত — না "checkup complete" চিহ্ন বসত, না ডাক্তারের পুরো নোট/পরের
       প্ল্যান জমা হত, আর কোনো retry-ও ছিল না। রোগী CHECK-UP তালিকায় আটকে থাকত।
       ⇒ FieldVisitControl.tryStart()-এর একই প্রমাণিত ধরনে (V1353): ব্যর্থ হলে
       ২ সেকেন্ড পরে **একবার** আবার চেষ্টা — সাময়িক দুর্বল লাইনে এতেই কাজ হয়ে
       যায়। এই ফাংশন এমনিতেই `BackgroundWork.run{}`-এর পিছনের সুতোয় চলে, তাই
       এই ছোট অপেক্ষায় পর্দা আটকায় না। এখনো ব্যর্থ হলে (সত্যিকারের নেট-বিচ্ছিন্নতা)
       আগের মতোই চুপচাপ বাদ — নতুন কোনো স্থায়ী জমা-তালিকা তৈরি করা হয়নি, কারণ
       `patientKey` (patientId না internal id, কোনটা তা নিশ্চিত নয়) দিয়ে নির্ভরযোগ্য
       ভাবে পরে আবার খোঁজার উপায় নেই — ভুল সারিতে জোর করে লেখার চেয়ে এটাই নিরাপদ। */
    private fun markDoctorComplete(patientKey: String, isRetry: Boolean = false) {
        if (patientKey.isBlank()) return
        try {
            val enc = java.net.URLEncoder.encode(patientKey, "UTF-8")
            // 🔴🔒 V794 — এখানে ছবি লাগে না (markDoctorComplete), তাই ছবি ছাড়া
            /* 🩺 V839 — পুরনো প্ল্যান-তালিকাটাও পড়া হয়, যাতে নতুনটা **যোগ**
               করা যায় (কখনো উপরে বসানো নয়)। একই সারি, বাড়তি অনুরোধ নেই। */
            val colsSave = SupabaseClient.PATIENT_NO_PHOTO_COLS + ",${NextVisitPlan.FIELD}"
            var rows = SupabaseClient.fetchListSlim("patients", "patientId=eq.$enc", 1, colsSave)
            if (rows.length() == 0) rows = SupabaseClient.fetchListSlim("patients", "id=eq.$enc", 1, colsSave)
            if (rows.length() == 0) {
                if (!isRetry) { try { Thread.sleep(2000) } catch (_: Throwable) { }; markDoctorComplete(patientKey, isRetry = true) }
                return
            }
            val id = rows.getJSONObject(0).optString("id")
            if (id.isBlank()) return
            /* 🔵 V559: doctorComplete-এর সাথে একই কলে চেকআপের পুরো লেখাটাও
               বসে যায় — বাড়তি কোনো নেটওয়ার্ক কল নয়। ওয়েব যা লিখে রেখেছিল
               তার উপরে বসানো হয় (merge), তাই ওয়েবের কোনো ঘর হারায় না। */
            val body = org.json.JSONObject().put("doctorComplete", true)
            try {
                val row0 = rows.getJSONObject(0)
                val oldNote = row0.optJSONObject("doctorFullNote")
                    ?: try {
                        val raw = row0.optString("doctorFullNote", "")
                        if (raw.trimStart().startsWith("{")) org.json.JSONObject(raw) else null
                    } catch (_: Throwable) { null }
                val rec = pendingNote
                if (rec != null) body.put("doctorFullNote",
                    mapToJson(CheckupNoteJson.merge(jsonToMap(oldNote), rec)))
                /* 🩺🔒 V839 — NEXT VISIT PLAN **একই কলেই** বসে, বাড়তি কোনো
                   নেটওয়ার্ক অনুরোধ নয়।
                   ⛔ পুরনো তালিকা হুবহু রেখে নতুন সারিটা **শেষে যোগ** হয়
                      (`NextVisitPlan.appended`) — একটাও পুরনো প্ল্যান মোছে না।
                   ⛔ ডাক্তার কিছুই না বাছলে (`isEmpty`) ঘরটা **ছোঁয়াই হয় না** —
                      তখন আগের প্ল্যান আগের মতোই থেকে যায়। */
                val plan = pendingNvp
                if (plan != null && !plan.isEmpty) {
                    body.put(NextVisitPlan.FIELD, NextVisitPlan.appended(row0, plan))
                }
            } catch (_: Throwable) { }
            // 🔴🔒 V1600 (১৯.০৯.২০২৬, TK-রিপোর্ট, ছবিসহ — নেট খুব ধীর থাকা
            // অবস্থায় সেভ করা একটা চেকআপ, "Check-up History" খুললে "এখনো
            // সম্পূর্ণ হয়নি" দেখাচ্ছিল, অথচ তথ্য ঠিকই সেভ হয়েছিল) — আসল
            // কারণ: এই শেষ লেখাটাই (doctorComplete=true + doctorFullNote +
            // Next Visit Plan) কোনো retry ছাড়াই একবার মাত্র পাঠানো হতো —
            // দুর্বল নেটে ব্যর্থ হলে চিরকালের জন্য হারিয়ে যেত, অথচ চেকআপের
            // আসল তথ্য (`saveMedical()`) আলাদা, প্রমাণিত সেভ-পথ দিয়ে
            // যেত বলে ঠিকই বসে যেত। এখানে ততক্ষণে আসল সারির `id` জানা হয়ে
            // গেছে (উপরে), তাই এখন ব্যর্থ হলে প্রমাণিত `GenericUpdateQueue`-তে
            // (App-এর প্রতিটা পর্দা খোলার সময় নিজে থেকেই আবার পাঠানোর চেষ্টা
            // করে) জমা রাখা হয় — patientId/id নিয়ে অনিশ্চয়তার কোনো ঝুঁকি নেই,
            // কারণ `id`-টা ততক্ষণে নির্দিষ্ট হয়েই গেছে।
            val ok = try { SupabaseClient.updateById("patients", id, body) } catch (_: Throwable) { false }
            if (!ok) {
                try { com.tkbiswas.pilesclinic.native.GenericUpdateQueue.queue(this, "patients", id, body) } catch (_: Throwable) { }
            }
        } catch (_: Exception) {
        }
    }

    private fun collect(): CheckupRecord = CheckupRecord(
        complaint = etComplaint.text?.toString().orEmpty(),
        duration = etDuration.text?.toString().orEmpty(),
        // V455 (18.08.2026): acuteChronic ঘর বাদ — মডেলের ডিফল্ট "" থাকে।
        occupation = occupationOptions.getOrElse(spOccupation.selectedItemPosition) { "" }
            .let { if (it == "Choose Occupation") "" else it },
        prevTreatment = etPrevTreatment.text?.toString().orEmpty(),
        patientSaid = findViewById<android.widget.EditText>(R.id.etPatientSaid).text?.toString().orEmpty(),   // 🔵 V539
        symptomHistory = collectSymptomHistory(),   // 🔵 V554
        historyDetail = collectHistoryDetail(),     // 🔵 V555
        lifestyle = collectLifestyle(),             // 🔵 V556
        probableDisease = collectProbableDisease(),   // 🔵 V557 · V946 (একাধিক রোগ)
        doctorRemark = findViewById<android.widget.EditText>(R.id.etDoctorRemark)
            .text?.toString()?.trim().orEmpty(),   // 🔵 V947
        timeAsked = CounselModel.timeAsked(
            findViewById<android.widget.EditText>(R.id.etTimeAsked).text?.toString().orEmpty(),
            CounselModel.UNITS.getOrElse(findViewById<android.widget.Spinner>(R.id.spTimeAskedUnit).selectedItemPosition) { "Days" }),
        anatomy = collectAnatomy(),                 // 🔵 V558
        dre = checkedText(dreChecks),               // 🔵 V556 (ফেরানো)
        dreOther = findViewById<android.widget.EditText>(R.id.etDreOther).text?.toString().orEmpty(),
        // 🔵 B622 (11.08.2026): Result/Spent/Treatment Duration ঘর বাদ — মডেলের ডিফল্ট "" থাকে।
        visual = checkedText(visualChecks),
        // V455 (18.08.2026): visualOther · dre · dreOther · otherFindings ঘর বাদ — মডেলের ডিফল্ট থাকে।
        grade = gradeOptions.getOrElse(spGrade.selectedItemPosition) { "" },   // 🔵 V539: এখন Internal Piles-এর Grade
        proctoscopy = findViewById<android.widget.EditText>(R.id.etProctoscopy).text?.toString().orEmpty(),   // 🔵 V539
        onProbing = etOnProbing.text?.toString().orEmpty(),
        investigation = checkedText(investigationChecks),
        treatmentPlan = checkedText(treatmentChecks()),
        amtPerPiles = etAmtPerPiles.text?.toString().orEmpty(),
        amtFistulaPerInch = etAmtFistulaInch.text?.toString().orEmpty(),
        amtKsharSutra = etAmtKsharSutra.text?.toString().orEmpty(),
        counselling = etCounselling.text?.toString().orEmpty(),
        estimatedCost = etEstimatedCost.text?.toString().orEmpty(),
        estimateJson = if (estimateSheet.isEmpty) "" else estimateSheet.toJson().toString(),   // 💰 V971
        recoveryTime = keptRecoveryTime,   // 🟢 V589: পুরনো লেখা হুবহু ফিরে বসে, মুছে যায় না
        // 🔵 B622 (11.08.2026): "Advance Payment to be Done" ঘর বাদ — মডেলের ডিফল্ট "" থাকে।
        // V455 (18.08.2026): patientDecision · decisionRemark ঘর বাদ — মডেলের ডিফল্ট "" থাকে।
        beforePhoto = beforePhotoData,
        duringPhoto = duringPhotoData,
        afterPhoto = afterPhotoData,
        // V455 (18.08.2026): documents ঘর বাদ — মডেলের ডিফল্ট "" থাকে।
        savedAt = System.currentTimeMillis(),
        savedByRole = RoleSession.currentRole
    )

    // 🆕 TK-নির্দেশ (04.08.2026): Treatment Plan-এর ৬টা Tick-বক্স একসাথে —
    // visualChecks/dreChecks-এর মতোই checkedText()-এ ব্যবহারের জন্য।
    private fun treatmentChecks(): List<CheckBox> =
        listOf(cbTxPerPiles, cbTxFistulaInch, cbTxMachine, cbTxKsharSutra, cbTxLis, cbTxInjection)

    private fun populate(r: CheckupRecord) {
        etComplaint.setText(r.complaint)
        etDuration.setText(r.duration)
        // V455 (18.08.2026): acuteChronic populate বাদ (ঘর নেই); পুরনো রেকর্ডে
        // মান থাকলেও এখন আর কোথাও দেখানো হয় না, সেভ-করা ডেটা ছোঁয়া হয়নি।
        val oi = occupationOptions.indexOf(r.occupation)
        if (oi >= 0) spOccupation.setSelection(oi)
        etPrevTreatment.setText(r.prevTreatment)
        findViewById<android.widget.EditText>(R.id.etPatientSaid).setText(r.patientSaid)   // 🔵 V539
        applySymptomHistory(r.symptomHistory)   // 🔵 V554
        applyHistoryDetail(r.historyDetail)     // 🔵 V555
        applyLifestyle(r.lifestyle)             // 🔵 V556
        // 🔵 V557
        applyProbableDisease(r.probableDisease)   // 🔵 V946
        findViewById<android.widget.EditText>(R.id.etDoctorRemark).setText(r.doctorRemark)   // 🔵 V947
        /* 📝 V996 — ভাঁজ বন্ধ থাকে, তাই ভিতরে লেখা আছে কি না সেটা মাথার
           সবুজ ব্যাজেই জানা যায় (বাকি ভাঁজগুলোর হুবহু একই নিয়ম)। */
        setFoldCount("drem", if (r.doctorRemark.isNotBlank()) 1 else 0)
        val (tAmt, tUnit) = CounselModel.splitTimeAsked(r.timeAsked)
        findViewById<android.widget.EditText>(R.id.etTimeAsked).setText(tAmt)
        val ui = CounselModel.UNITS.indexOf(tUnit)
        if (ui >= 0) findViewById<android.widget.Spinner>(R.id.spTimeAskedUnit).setSelection(ui)
        lastSavedCost = r.estimatedCost   // 🔔 V557: এই খরচেই আগে নোটিশ গেছে
        applyAnatomy(r.anatomy)                 // 🔵 V558
        val dreSel = r.dre.split(", ").map { it.trim() }   // 🔵 V556
        dreChecks.forEach { it.isChecked = dreSel.contains((it.tag as? String) ?: it.text.toString()) }
        findViewById<android.widget.EditText>(R.id.etDreOther).setText(r.dreOther)
        // 🔵 B622: Result/Spent/Treatment Duration ঘর বাদ।
        val vis = r.visual.split(", ").map { it.trim() }
        visualChecks.forEach { it.isChecked = vis.contains((it.tag as? String) ?: it.text.toString()) }
        // V455 (18.08.2026): visualOther/dre*/otherFindings populate বাদ (ঘর নেই)।
        val gi = gradeOptions.indexOf(r.grade)
        if (gi >= 0) spGrade.setSelection(gi)
        findViewById<android.widget.EditText>(R.id.etProctoscopy).setText(r.proctoscopy)   // 🔵 V539
        refreshInternalGradeLabel()   // 🔵 V539: Internal Piles-এর পাশে Grade দেখানো
        etOnProbing.setText(r.onProbing)
        refreshClinicalFold()   // 🟢 V703 — পুরোনো রেকর্ড খুললেও ব্যাজের সংখ্যা ঠিক থাকে
        val inv = r.investigation.split(", ").map { it.trim() }
        investigationChecks.forEach { it.isChecked = inv.contains((it.tag as? String) ?: it.text.toString()) }
        val tx = r.treatmentPlan.split(", ").map { it.trim() }
        /* 🔵 V541: Fistula-র বেলায় পুরোনো লেখাগুলোও মেনে নেওয়া হয়, তাই
           আগে সেভ হওয়া চেকআপে টিকটা আগের মতোই থাকে। ⛔ বাকি প্রতিটা
           চেকবক্সে মিলের নিয়ম **হুবহু আগের**। */
        treatmentChecks().forEach { cb ->
            val tag = (cb.tag as? String) ?: cb.text.toString()
            cb.isChecked = if (tag == FISTULA_TAG_NOW) tx.any { it in FISTULA_TAGS } else tx.contains(tag)
        }
        if (r.amtPerPiles.isNotBlank()) etAmtPerPiles.setText(r.amtPerPiles)
        if (r.amtFistulaPerInch.isNotBlank()) etAmtFistulaInch.setText(r.amtFistulaPerInch)
        if (r.amtKsharSutra.isNotBlank()) etAmtKsharSutra.setText(r.amtKsharSutra)
        etCounselling.setText(r.counselling)
        etEstimatedCost.setText(r.estimatedCost)
        estimateSheet = EstimateModel.parse(r.estimateJson)   // 💰 V971
        renderEstimateSummary()   // 🧮 V1280
        keptRecoveryTime = r.recoveryTime   // 🟢 V589: দেখানো হয় না, কিন্তু হারায়ও না
        // 🔵 B622: "Advance Payment to be Done" ঘর বাদ।
        // V455 (18.08.2026): patientDecision/decisionRemark/documents populate বাদ (ঘর নেই)।
        beforePhotoData = r.beforePhoto
        duringPhotoData = r.duringPhoto
        afterPhotoData = r.afterPhoto
        if (r.beforePhoto.isNotBlank()) showThumb(ivBeforePhoto, r.beforePhoto)
        if (r.duringPhoto.isNotBlank()) showThumb(ivDuringPhoto, r.duringPhoto)
        if (r.afterPhoto.isNotBlank()) showThumb(ivAfterPhoto, r.afterPhoto)
        /* 🟢 V1119 — পুরনো রেকর্ড খুলে বসার পর ভরা ধাপগুলো সঙ্গে সঙ্গেই
           নিচের "SAVED" বাক্সে নেমে যায় (সবগুলো ভাঁজ শুরুতে বন্ধই থাকে)। */
        findViewById<android.view.View>(R.id.secSavedGroup)?.post { refreshSavedGroup() }
    }

    /**
     * 🔵🔒 V539 (২২.০৮.২০২৬, TK-নির্দেশ): *"Internal Piles — এখানে চাপ দিলে
     * Grade 1/2/3/4 আসবে, যেটা সিলেক্ট করব সেটাই থাকবে।"*
     *
     * ⛔ Grade জমা হয় **পুরোনো সেই `grade` ঘরেই** (TK-এর নিজের উত্তর: *"হ্যাঁ,
     *    একই — পুরোনো ঘরটাই ব্যবহার করুন"*), মানও হুবহু আগের ("Grade I"…)।
     *    ⇒ **পুরোনো প্রতিটা রেকর্ড আগের মতোই ঠিক দেখাবে।**
     * ⛔ চেকবক্সের **সেভ-হওয়া লেখা বদলায়নি** — `visual`-এ আগের মতোই শুধু
     *    "Internal Piles" যায়, Grade আলাদা ঘরে। তাই ছাপা/পুরোনো পড়া কিছুই ভাঙে না।
     */
    /** 🔵 V539: হেডারের "Male-55" লাইনে পেশাও যোগ করে। ⛔ পেশা ফাঁকা হলে
     *  লাইনটা **হুবহু আগের মতোই** থাকে। */
    /** 🔵 V539: পেশা বাছার তালিকা — বাছাই বসে পুরোনো `spOccupation`-এই। */
    private fun askOccupation() {
        // 🔵 V540: এখানেও প্রজেক্টের সেই একই প্রিমিয়াম পিকার।
        SpinnerPicker.open(spOccupation, "CHOOSE OCCUPATION", hidePlaceholder = true)
    }

    private fun refreshSexAgeOccupation() {
        val tv = findViewById<TextView>(R.id.tvPatientSexAge) ?: return
        val sexAge = listOf(patSex, patAge).filter { it.isNotBlank() }.joinToString("-")
        val occ = occupationOptions.getOrElse(spOccupation.selectedItemPosition) { "" }
            .let { if (it == "Choose Occupation") "" else it }
        val text = listOf(sexAge, occ).filter { it.isNotBlank() }.joinToString(" · ")
        if (text.isNotBlank()) { tv.text = text; tv.visibility = android.view.View.VISIBLE }
        else tv.visibility = android.view.View.GONE
    }

    /* ═══════════════════════════════════════════════════════════════════
       🔵🔒 V554 (২২.০৮.২০২৬) — কাগজের *"রোগী এসে প্রথমে কি কি সমস্যার কথা বললেন?"*
       TK-অনুমোদিত ডেমো: **সাজ "গ"** (কাগজের মতো সারি) · **শুরুতে টিক** ·
       ব্যথার **তীব্র/মৃদু এক লাইনেই** · শেষে **"এছাড়া অন্য কিছু"** বাক্স।

       ⛔ সারির নাম/ক্রম/একক — সব `SymptomHistoryModel`-এ, **এক জায়গাতেই**।
       ⛔ "কবে থেকে?"-র ঘর দুটো **রেজিস্ট্রেশনের হুবহু একই জোড়া** — সংখ্যার ঘর
          (`inputType=number`) + Days/Months/Years, আর তালিকাটা প্রজেক্টের নিজের
          প্রিমিয়াম পিকারেই খোলে (`SpinnerPicker.attach`)। নতুন নকশা বানানো হয়নি।
       ⛔ উপরের Chief Complaint ও Duration ঘর দুটো **সরানো হয়নি**।
       ═══════════════════════════════════════════════════════════════ */
    private val symptomTicks = LinkedHashMap<String, android.widget.CheckBox>()
    private val symptomAmounts = LinkedHashMap<String, android.widget.EditText>()
    private val symptomUnits = LinkedHashMap<String, android.widget.Spinner>()
    private val symptomSeverity = LinkedHashMap<String, MutableList<TextView>>()

    private fun symDp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /* ═══════════════════════════════════════════════════════════════════
       🔵🔒 V574 (২৩.০৮.২০২৬ — TK ডেমো দেখে অনুমোদন করেছেন) — **চাপ দিলে খোলে**।
       TK-এর নির্দেশ: *"রোগী এসে কি কি বললেন সেখানে চাপ দিলে ওই 6 টা অপশন
       ওপেন হতে হবে"* এবং *"রক্তপাতের ইতিহাস / ব্যথার ইতিহাস / ফোলা
       মাংসপিন্ডের ইতিহাস / পুজ ও জল পড়ার ইতিহাস — এগুলো এখানে চাপ দিলে
       তখনই ফর্মগুলো ওপেন হবে"*।

       বন্ধ থাকলেও যেন বোঝা যায় ভিতরে কিছু ভরা আছে — তাই ডান দিকে ছোট
       সবুজ ব্যাজে সংখ্যা বসে (কম্পিউটারের `.wlv1FoldN`-এর যমজ)।
       ⛔ ভিতরের একটা ঘরও বদলায়নি · সেভ/পড়ার কোড ছোঁয়া হয়নি — শুধু
          দেখা-না-দেখা (`visibility`)।
       ⚠️ এখানে `android.view.View` পুরো নাম লিখে ব্যবহার করা হয়েছে, কারণ
          এই ফাইলে ওটা import করা নেই (V575-এ এই ভুলেই বিল্ড ভেঙেছিল)।
       ═══════════════════════════════════════════════════════════════════ */
    private class DocFold(
        val num: TextView,
        val chev: TextView,
        val body: android.view.View
    )

    private val docFolds = LinkedHashMap<String, DocFold>()

    /** মাথায় চাপ দিলে শরীরটা খোলে/বন্ধ হয়। শুরুতে বন্ধ। */
    private fun attachFold(key: String, head: android.view.View?,
                           num: TextView?, chev: TextView?,
                           body: android.view.View?,
                           onToggle: (() -> Unit)? = null) {
        if (head == null || num == null || chev == null || body == null) return
        body.visibility = android.view.View.GONE
        chev.text = "\u2304"
        docFolds[key] = DocFold(num, chev, body)
        head.setOnClickListener {
            val open = body.visibility != android.view.View.VISIBLE
            body.visibility = if (open) android.view.View.VISIBLE else android.view.View.GONE
            chev.text = if (open) "\u2303" else "\u2304"
            /* 🟢🔒 V703 — বন্ধ করার মুহূর্তে মাথার সংখ্যাটা নতুন করে বসানোর
               সুযোগ। ⛔ ডিফল্ট null, তাই আগের তিনটে ভাঁজ (sym/life/photo) এক
               অক্ষরও বদলায়নি — Kotlin-এর default argument। */
            onToggle?.invoke()
            /* 🟢 V1119 — ভাঁজ বন্ধ করলেই ভরা ধাপটা নিচের "SAVED" বাক্সে নেমে
               যায়, খুললেই আবার উপরে ফেরে। ⛔ `post` — চাপাচাপির হিসাব শেষ
               হওয়ার পরে তবেই কার্ডটা সরানো হয়। */
            head.post { refreshSavedGroup() }
        }
    }

    /** বন্ধ অবস্থাতেও ভিতরে কতগুলো ভরা আছে সেটা দেখানো। ০ হলে ব্যাজ বসেই না। */
    private fun setFoldCount(key: String, n: Int) {
        val f = docFolds[key] ?: return
        if (n > 0) {
            f.num.text = n.toString() + "\u099F\u09BF"     // "টি"
            f.num.visibility = android.view.View.VISIBLE
        } else {
            f.num.visibility = android.view.View.GONE
        }
    }

    /** ভাগ ২-এ কতগুলো টিক পড়েছে — মাথার সংখ্যাটা নতুন করে বসানো হয়। */
    private fun refreshSymptomFold() {
        var n = 0
        for (line in SymptomHistoryModel.LINES) {
            if (symptomTicks[line.key]?.isChecked == true) n++
        }
        setFoldCount("sym", n)
    }

    /* 🔵 V574 (TK-নির্দেশ, ছবি দেখে): *"দিন সময় / যে বক্সগুলো / সম্পূর্ণ ডিজাইন
       কেন বদলে দিলেন / দুটো পাশাপাশি থাকবো"* — তাই বাক্স দুটো **পাশাপাশিই**
       আছে, ডিজাইন এক অক্ষরও বদলায়নি। TK-এর বাছাই ছিল **প্রস্তাব খ**: টিক
       দিলে তবেই বাক্স দুটো দেখা যাবে, নইলে লাইনটা পরিষ্কার থাকবে। */
    private fun syncSymptomBoxes(key: String) {
        val on = symptomTicks[key]?.isChecked == true
        // ⚠️ `GONE` — `INVISIBLE` নয়। কম্পিউটারের অ্যাপে (`display:none`) বাক্স
        //    দুটো একেবারে সরে যায়; ফোনেও হুবহু তাই হওয়া দরকার, নইলে TK-এর
        //    অনুমোদিত ডেমোর সঙ্গে দেখতে মিলত না (ফাঁকা জায়গা পড়ে থাকত)।
        val vis = if (on) android.view.View.VISIBLE else android.view.View.GONE
        symptomAmounts[key]?.visibility = vis
        symptomUnits[key]?.visibility = vis
    }

    private fun symChipBg(on: Boolean): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = symDp(20).toFloat()
            setColor(android.graphics.Color.parseColor(if (on) "#EAFBF0" else "#FFFFFF"))
            setStroke(symDp(1), android.graphics.Color.parseColor(if (on) "#BFE9CE" else "#DBE2EA"))
        }

    private fun paintSeverity(key: String) {
        val chips = symptomSeverity[key] ?: return
        chips.forEach { chip ->
            val on = (chip.tag as? String) == (chip.getTag(R.id.symptomGroup) as? String)
            chip.background = symChipBg(on)
            chip.setTextColor(android.graphics.Color.parseColor(if (on) "#0B4F2A" else "#93A1B2"))
        }
    }

    private fun currentSeverity(key: String): String {
        val chips = symptomSeverity[key] ?: return ""
        val chosen = chips.firstOrNull()?.getTag(R.id.symptomGroup) as? String
        return chosen.orEmpty()
    }

    private fun setSeverity(key: String, value: String) {
        symptomSeverity[key]?.forEach { it.setTag(R.id.symptomGroup, value) }
        paintSeverity(key)
    }

    private fun buildSymptomRows() {
        val box = findViewById<android.widget.LinearLayout>(R.id.symptomGroup) ?: return
        box.removeAllViews()
        symptomTicks.clear(); symptomAmounts.clear(); symptomUnits.clear(); symptomSeverity.clear()
        for (line in SymptomHistoryModel.LINES) {
            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, symDp(3), 0, symDp(3))
            }
            val cb = android.widget.CheckBox(this)
            row.addView(cb)
            symptomTicks[line.key] = cb
            // 🔵 V574: টিক বদলালেই বাক্স দুটো দেখা/লুকানো + মাথার সংখ্যা বদলায়
            cb.setOnCheckedChangeListener { _, _ ->
                syncSymptomBoxes(line.key)
                refreshSymptomFold()
            }

            row.addView(TextView(this).apply {
                text = line.label
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#10223A"))
                maxLines = 2
                val lp = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginEnd = symDp(6)
                layoutParams = lp
            })

            if (line.severity) {
                val chips = ArrayList<TextView>()
                for (sev in SymptomHistoryModel.SEVERITY) {
                    val chip = TextView(this).apply {
                        text = sev
                        textSize = 11.5f
                        tag = sev
                        setPadding(symDp(10), symDp(3), symDp(10), symDp(3))
                        isClickable = true
                        val lp = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        lp.marginStart = symDp(6)
                        layoutParams = lp
                    }
                    chip.setOnClickListener {
                        // একই চিপে আবার চাপ দিলে বাছাইটা উঠে যায়
                        setSeverity(line.key, if (currentSeverity(line.key) == sev) "" else sev)
                    }
                    chips.add(chip)
                    row.addView(chip)
                }
                symptomSeverity[line.key] = chips
                setSeverity(line.key, "")
            }

            // ২. TK-এর নির্দেশ: *"রেজিস্ট্রেশন ফর্মে যেমন ২, ৩, ৪ তার পাশে বক্স
            //    থাকবে ... ঠিক এখানেও তেমনি রাখবেন"* — তাই সংখ্যার ঘরটা
            //    রেজিস্ট্রেশনের মতোই বক্স, শুধু নিচে একটা দাগ নয়।
            val amount = android.widget.EditText(this).apply {
                /* 🔴 V566 — নিজের যাচাইয়ে ধরা পড়ল: এখানে শুধু সংখ্যার ধরনটাই
                   বসানো ছিল। B411-এ শেখা হয়েছিল, কিছু ফোনে ওতে কীবোর্ডই খোলে না।
                   প্রজেক্টের বাকি সব জায়গার (ModuleUi · WorkNotebook ·
                   PartnerShares) মতোই এখন `TYPE_CLASS_TEXT` + `DigitsKeyListener` —
                   কীবোর্ড খুলবে, আর শুধু ০–৯ ছাড়া কিছু টাইপ হবে না। */
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
                /* 🔵🔒 V578 (TK-নির্দেশ ২৩.০৮.২০২৬): *"দুইটা বক্সের সাইজ একই রকম
                   থাকবে"* ⇒ সংখ্যার ঘর ও Days-এর ঘর — দুটোই এখন **৮২×৪০**।
                   কম্পিউটারের অ্যাপেও ঠিক এই মাপ (`.wlv1SymAmt,.wlv1SymUnit`)। */
                textSize = 12.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setSingleLine(true)
                setBackgroundResource(R.drawable.bg_input_field)
                setPadding(symDp(4), 0, symDp(4), 0)
                layoutParams = android.widget.LinearLayout.LayoutParams(symDp(82), symDp(40))
            }
            row.addView(amount)
            symptomAmounts[line.key] = amount

            val unit = android.widget.Spinner(this).apply {
                /* 🔵🔒 V578 (TK-নির্দেশ): *"Days এটা বক্সের মধ্যে থাকবে — বর্তমানে
                   দেখে মনে হচ্ছে বক্সের থেকে বেরিয়ে গেছে"*।
                   কারণ কী ছিল: লেখাটা ১৪.৫sp, বাঁ দিকে কোনো ফাঁক নেই (item-এর
                   `paddingHorizontal=0dp`), আর তিরটা ১৮dp চওড়া হয়ে ডান দিক থেকে
                   ১৪dp ভিতরে — তাই ছোট ঘরে "Months" তিরের নিচে ঢুকে যেত।
                   ⇒ এখন ছোট লেখার নিজস্ব item (`item_docnote_spinner_unit`) ও
                     ছোট তিরের নিজস্ব বাক্স (`bg_unit_picker_small`), আর দু'পাশে
                     ফাঁক — লেখাটা পুরোপুরি বাক্সের ভিতরেই থাকে।
                   ⛔ বাছাইয়ের তালিকা · মান · সেভ কিছুই বদলায়নি। */
                adapter = ArrayAdapter(this@DoctorCheckupActivity, R.layout.item_docnote_spinner_unit, SymptomHistoryModel.UNITS)
                setBackgroundResource(R.drawable.bg_unit_picker_small)
                /* 🔴 V567 — আগে এখানে `paddingStart` ছিল। কাজ একই, কিন্তু `setPadding()`
                   বাঁ/ডান হিসেবেই কাজ করে, তাই `paddingLeft`-ই মানানসই — আর প্রজেক্টের
                   বাকি জায়গাতেও (`FieldError.kt`) ওটাই ব্যবহার হয়। কম্পাইল-পাহারা
                   `paddingStart`-কে চিনতে না পেরে ভুল বলে ধরছিল। */
                setPadding(symDp(6), 0, symDp(18), 0)
                val lp = android.widget.LinearLayout.LayoutParams(symDp(82), symDp(40))
                lp.marginStart = symDp(6)
                layoutParams = lp
            }
            row.addView(unit)
            symptomUnits[line.key] = unit
            /* ⛔ এখানে `hidePlaceholder` **নয়** — "Days" একটা সত্যিকারের বাছাই,
                  লুকিয়ে দিলে ওটা আর বাছাই করা যেত না। */
            SpinnerPicker.attach(unit, "কবে থেকে?")

            box.addView(row)
            syncSymptomBoxes(line.key)      // 🔵 V574 — শুরুতেই ঠিক অবস্থায়
        }
        refreshSymptomFold()
    }

    /** 🔵 V574 — XML-এ বসানো মাথাটার সঙ্গে ভাঁজের কাজ জুড়ে দেওয়া। */
    private fun wireSymptomFold() {
        attachFold(
            "sym",
            findViewById<android.widget.LinearLayout>(R.id.symptomFoldHead),
            findViewById<TextView>(R.id.symptomFoldNum),
            findViewById<TextView>(R.id.symptomFoldChev),
            findViewById<android.widget.LinearLayout>(R.id.symptomFoldBody)
        )
        refreshSymptomFold()
    }

    /** 🟢 V600 (২৩.০৮.২০২৬, TK-নির্দেশ) — Photo & Video হেডারে চাপ দিলে
        তিনটে ছবির সারি খোলে। সংখ্যার ব্যাজ (photoFoldNum) ব্যবহার হয় না,
        তাই কখনো ডাকা হয় না — বরাবরই লুকানো থাকে (XML-এ ডিফল্ট gone)। */
    private fun wirePhotoFold() {
        attachFold(
            "photo",
            findViewById<android.widget.LinearLayout>(R.id.photoFoldHead),
            findViewById<TextView>(R.id.photoFoldNum),
            findViewById<TextView>(R.id.photoFoldChev),
            findViewById<android.widget.LinearLayout>(R.id.photoFoldBody)
        )
    }

    /* 🟢🔒 V703 (২৬.০৮.২০২৬, TK-নির্দেশ ডেমো-প্রুফে অনুমোদিত):
       *"এখানেও Form টা ওপেন থাকবে না"* — ধাপ ২ (Clinical পরীক্ষা) এখন
       ভাগ ১-এর মতোই **বন্ধ অবস্থায়** শুরু হয়, মাথায় চাপ দিলে খোলে।
       ⛔ TK স্পষ্ট বলেছেন *"শুধুমাত্র যেটা বলা হলো সেটা করুন"* ⇒ ধাপ ৩·৪·৫
          ছোঁয়া হয়নি, আর ধাপ ২-এর ভিতরের একটাও ঘর/টিক/সেভ বদলায়নি —
          XML-এ শুধু একটা মোড়ক (`clinicalFoldBody`) যোগ হয়েছে।
       ⛔ নতুন কোনো ভাঁজ-ব্যবস্থা বানানো হয়নি — চালু `attachFold`-ই ব্যবহার। */
    private fun wireClinicalFold() {
        attachFold(
            "clin",
            findViewById<android.widget.LinearLayout>(R.id.clinicalFoldHead),
            findViewById<TextView>(R.id.clinicalFoldNum),
            findViewById<TextView>(R.id.clinicalFoldChev),
            findViewById<android.widget.LinearLayout>(R.id.clinicalFoldBody)
        ) { refreshClinicalFold() }
        refreshClinicalFold()
    }

    /* 🟢🔒 V866 (৩০.০৮.২০২৬, TK-নির্দেশ ডেমো-প্রুফে অনুমোদিত):
       *"next visit plan — এই ফর্মটা ওপেন থাকবে না"* — ধাপ ২ (V703) ও ধাপ ৫
       (V600)-এর হুবহু একই ব্যবস্থা: শুরুতে বন্ধ, মাথায় চাপ দিলে খোলে।
       ⛔ নতুন কোনো ভাঁজ-ব্যবস্থা বানানো হয়নি — চালু `attachFold`-ই।
       ⛔ NEXT VISIT PLAN-এর ভিতরের একটাও ঘর · টিক · তারিখ · সেভ বদলায়নি। */
    /* 🔴🔒 V938 — TODAY'S TREATMENT DONE-এর ভাঁজ। NEXT VISIT PLAN-এর হুবহু
       একই `attachFold` — নতুন কোনো ভাঁজ-ব্যবস্থা বানানো হয়নি। */
    private fun ttdBoxes(): List<CheckBox> = listOf(
        findViewById(R.id.cbTtd1), findViewById(R.id.cbTtd2), findViewById(R.id.cbTtd3),
        findViewById(R.id.cbTtd4), findViewById(R.id.cbTtd5), findViewById(R.id.cbTtd6),
        findViewById(R.id.cbTtd7), findViewById(R.id.cbTtd8), findViewById(R.id.cbTtd9),
        findViewById(R.id.cbTtd10)
    )

    private fun refreshTtdFold() {
        var n = 0
        for (cb in ttdBoxes()) if (cb.isChecked) n++
        setFoldCount("ttd", n)
    }

    private fun wireTtdFold() {
        attachFold(
            "ttd",
            findViewById<android.widget.LinearLayout>(R.id.ttdFoldHead),
            findViewById<TextView>(R.id.ttdFoldNum),
            findViewById<TextView>(R.id.ttdFoldChev),
            findViewById<android.widget.LinearLayout>(R.id.ttdFoldBody)
        ) { refreshTtdFold() }
        for (cb in ttdBoxes()) cb.setOnCheckedChangeListener { _, _ -> refreshTtdFold() }
        refreshTtdFold()
    }

    /* ══ 🔵🔒 V946 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — "সম্ভাব্য কি রোগ?"
       একাধিক বাছা যাবে ══════════════════════════════════════════════════════
       রেজিস্ট্রেশনের (`RegistrationActivity`) হুবহু একই চিপ ও একই রং-সিলেক্টর,
       তাই দুই পর্দা দেখতে এক রকম।
       ⛔ তালিকাটা `CounselModel.DISEASES`-ই — শুধু "বাছেননি" ঘরটা চিপে থাকে না
          (চিপে কিছু না বাছলেই সেটা "বাছেননি")।
       ⛔ সেভ/পড়া আগের মতোই একটাই লেখা-ঘরে — একাধিক হলে ", " দিয়ে জোড়া। */
    private val v946DiseaseChips = ArrayList<com.google.android.material.chip.Chip>()

    private fun v946DiseaseOptions(): List<String> =
        CounselModel.DISEASES.filter { it != CounselModel.PICK_NONE }

    private fun buildDiseaseChips() {
        val group = findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgProbableDisease) ?: return
        group.removeAllViews()
        v946DiseaseChips.clear()
        v946DiseaseOptions().forEach { label ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = label
                isCheckable = true
                isCloseIconVisible = false
                chipBackgroundColor = androidx.core.content.ContextCompat
                    .getColorStateList(this@DoctorCheckupActivity, R.color.chip_bg_selector)
                setTextColor(androidx.core.content.ContextCompat
                    .getColorStateList(this@DoctorCheckupActivity, R.color.chip_text_selector))
                checkedIconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            }
            v946DiseaseChips.add(chip)
            group.addView(chip)
        }
    }

    /** ডাক্তার যা যা বেছেছেন — ", " দিয়ে জোড়া। কিছু না বাছলে "বাছেননি"। */
    private fun collectProbableDisease(): String {
        val picked = v946DiseaseChips.filter { it.isChecked }.map { it.text.toString() }
        return if (picked.isEmpty()) CounselModel.PICK_NONE else picked.joinToString(", ")
    }

    /** আগে সেভ করা লেখা ফিরিয়ে বসানো — পুরনো একটামাত্র রোগও ঠিক বসে। */
    private fun applyProbableDisease(saved: String) {
        val want = saved.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        v946DiseaseChips.forEach { chip -> chip.isChecked = want.contains(chip.text.toString()) }
    }

    /** 🔴🔒 V938 — ডাক্তার আজ যা বেছেছেন, চেম্বারের লেখার হুবহু একই ধাঁচে
     *  (" · " দিয়ে জোড়া, ঠিক `TreatmentQuickNotes`-এর লেখাগুলোই)।
     *  ⛔ কিছু না বাছলে ফাঁকা ফেরে ⇒ চেম্বারের পুরনো লেখা ছোঁয়াই হয় না। */
    private fun collectTodayTreatment(): String {
        val parts = ArrayList<String>()
        for (cb in ttdBoxes()) if (cb.isChecked) parts.add(cb.text.toString().trim())
        val other = findViewById<android.widget.EditText>(R.id.etTtdOther).text?.toString()?.trim().orEmpty()
        if (other.isNotBlank()) parts.add(other)
        return parts.joinToString(" · ")
    }

    private fun wireNvpFold() {
        attachFold(
            "nvp",
            findViewById<android.widget.LinearLayout>(R.id.nvpFoldHead),
            findViewById<TextView>(R.id.nvpFoldNum),
            findViewById<TextView>(R.id.nvpFoldChev),
            findViewById<android.widget.LinearLayout>(R.id.nvpFoldBody)
        ) { refreshNvpFold() }
        refreshNvpFold()
    }

    /* 🟢🔒🔒 V886 (৩০.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফে অনুমোদিত):
       TK: *"মেইন পয়েন্টগুলো ফর্ম খোলা থাকবে না"* — তিনি নিজে নিশ্চিত করেছেন
       *"নামের উপরে চাপ দিলে ফর্ম ওপেন হবে তো"* ⇒ হ্যাঁ, ঠিক ধাপ ২ ও ধাপ ৫-এর
       মতোই।
       ⇒ ধাপ ১ (History & Previous) · ধাপ ৩ (Counsel) · ধাপ ৪ (Probable Disease
         and Time Asked) এখন **বন্ধ অবস্থায়** শুরু হয়।
       ⛔ নতুন কোনো ভাঁজ-ব্যবস্থা বানানো হয়নি — চালু `attachFold`-ই।
       ⛔ ভিতরের একটাও ঘর · টিক · তারিখ · সেভ বদলায়নি — শুধু মোড়ক ও চ্যাভরন।
       ⛔ ধাপ ১-এর ভিতরের পুরোনো ছোট ভাঁজগুলো (symptom/life) আগের মতোই কাজ করে। */
    private fun wireMainFolds() {
        /* 📝🔒 V996 (০৩.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — DOCTOR'S REMARK
           ("drem") এই তালিকায় যোগ হলো, তাই সেটাও বাকিগুলোর মতোই **বন্ধ
           অবস্থায় শুরু হয়**। ⛔ ভাঁজের নিয়ম এক অক্ষরও বদলায়নি। */
        for (key in listOf("hist", "couns", "estm", "drem")) {
            val head = resources.getIdentifier("${key}FoldHead", "id", packageName)
            val num = resources.getIdentifier("${key}FoldNum", "id", packageName)
            val chev = resources.getIdentifier("${key}FoldChev", "id", packageName)
            val body = resources.getIdentifier("${key}FoldBody", "id", packageName)
            if (head == 0 || num == 0 || chev == 0 || body == 0) continue
            attachFold(
                key,
                findViewById<android.widget.LinearLayout>(head),
                findViewById<TextView>(num),
                findViewById<TextView>(chev),
                findViewById<android.widget.LinearLayout>(body)
            )
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       🟢🔒 V1119 (০৫.০৯.২০২৬) — **ভরা ধাপগুলো নিচে একটাই "SAVED" বাক্সে**
       TK-এর নির্দেশ (ফটো-প্রুফ দেখে *"পাশ, বসিয়ে দিন"*):
       *"যেগুলো সেভ হয়ে যাবে সেগুলো নিচে থাকবে, কিন্তু সবগুলো একটার মধ্যে
       থাকবে — যেখানে চাপ দিলে তখন সেগুলো ওপেন হবে"*

       নিয়ম (মেপে বসানো, আন্দাজ নয়):
       ① একটা ধাপ নিচে নামে **শুধু তখনই** যখন সেটা (ক) ভরা **এবং** (খ) বন্ধ।
          ⛔ খোলা ধাপ কখনোই সরে না — ডাক্তার লেখার মাঝপথে পর্দা লাফাবে না।
       ② ফাঁকা হয়ে গেলে ধাপটা **নিজের পুরনো জায়গায় ফিরে যায়** (ক্রম অটুট,
          কারণ ৮টা কার্ডই পাশাপাশি ভাই আর SAVED বাক্সটা সবার শেষে)।
       ③ নিচে নামলে কার্ডটা **ফিকে** (alpha 0.55) — কিন্তু ভিতরের একটাও ঘর ·
          বোতাম · টিক অকেজো হয় না; চাপ দিলে আগের মতোই খোলে ও লেখা যায়।

       ⛔ **কোনো ডেটা/সেভ/ক্লাউড কোড ছোঁয়া হয়নি** — কার্ডগুলো শুধু গাছের এক
          ডাল থেকে আরেক ডালে যায়; `findViewById` পুরো পর্দা জুড়ে খোঁজে, তাই
          `collect()` · `lockSection` · ছবি — সবই আগের মতোই কাজ করে।
       ⛔ `secDoctorReminder` (ডাক্তারের নিজের নোট) এই তালিকায় নেই — ওটা
          আলাদা মোড়কে থাকে, ছোঁয়া হয়নি।
       ⚠️ কার্ড সরানোর কাজটা `post {}`-এর ভিতরে, কারণ চাপাচাপির (click) হিসাব
          শেষ হওয়ার আগেই কার্ডটাকে গাছ থেকে তুলে নিলে ঝুঁকি থাকে।
       ═══════════════════════════════════════════════════════════════════ */
    /* 🟢🔒 V1504 (১৫.০৯.২০২৬, TK-নির্দেশ) — "History/Clinical/Estimate/Photo
       জীবনে একবারই করা হয়, কিন্তু Remark/Today's Treatment/Next Visit
       রোগী যতবার আসে ততবারই বদলাতে পারে — এটা আমাদের ব্যবসার নিয়ম।"
       ⇒ এই তিনটে (secDocRemark/secTodayTreat/secNextVisitPlan) এই
       তালিকা থেকে বাদ — তারা কখনোই ধূসর/SAVED বাক্সে নামে না, প্রতিবারই
       খোলা/স্বাভাবিক অবস্থায় থাকে যাতে ডাক্তার সহজে নতুন করে লিখতে পারেন। */
    private val savedGroupIds = intArrayOf(
        R.id.secHistory, R.id.secClinical, R.id.secCounsel, R.id.secEstimate, R.id.secPhoto
    )
    private var savedGroupOpen = false
    private var savedGroupBusy = false
    /* 💾🔒 V1255 (০৯.০৯.২০২৬, TK-নির্দেশ, খাতার সারি ৩৭৭) — TK: *"এই চেকআপ
       পর্দায় আসার পরে যেটা সেভ করা হবে সেগুলোই নিচে যাবে"*।
       ⇒ **SAVE চাপার আগে নিচের বাক্সে একটা কার্ডও নামে না।** এই ঘরটা তখনই
         `true` হয় যখন সেভ সত্যিই হয়ে যায়।
       ⛔ কে সেভ করতে পারবেন সেই নিয়ম **এক অক্ষরও বদলায়নি** (TK: *"শুধুমাত্র
          ডাক্তারের জন্য নয়, যে কেউ"* — এই ঘরটাও কোনো role দেখে না, যিনিই
          সেভ করুন, তাঁর জন্যই কাজ করে)। */
    private var savedInThisScreen = false

    /** ভিতরে কিছু ভরা আছে কি না — ঘর · টিক · তালিকা · চিপ সব মিলিয়ে। */
    private fun sectionHasContent(v: android.view.View): Boolean {
        when (v) {
            is android.widget.EditText ->
                if (v.text?.toString()?.isNotBlank() == true) return true
            is android.widget.CompoundButton ->
                if (v.isChecked) return true
            is android.widget.Spinner ->
                if (v.selectedItemPosition > 0) return true
            is TextView -> {
                if (v.getTag(R.id.historyDetailGroup) == true) return true
                val sev = v.getTag(R.id.symptomGroup) as? String
                if (!sev.isNullOrBlank()) return true
            }
        }
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) if (sectionHasContent(v.getChildAt(i))) return true
        }
        return false
    }

    /** ধাপটা কি ভরা? (ছবির ধাপে ছবিগুলোও গোনা হয় — ওগুলো কোনো ঘরে থাকে না।) */
    private fun sectionFilled(card: android.view.View): Boolean {
        if (card.id == R.id.secPhoto &&
            (beforePhotoData.isNotBlank() || duringPhotoData.isNotBlank() || afterPhotoData.isNotBlank())
        ) return true
        return sectionHasContent(card)
    }

    /** ধাপটা কি এই মুহূর্তে বন্ধ? (ভাঁজের শরীরটা দেখা না গেলে বন্ধ।) */
    private fun sectionClosed(card: android.view.View): Boolean {
        val bodyId = when (card.id) {
            R.id.secHistory -> R.id.histFoldBody
            R.id.secClinical -> R.id.clinicalFoldBody
            R.id.secCounsel -> R.id.counsFoldBody
            R.id.secEstimate -> R.id.estmFoldBody
            R.id.secDocRemark -> R.id.dremFoldBody
            R.id.secTodayTreat -> R.id.ttdFoldBody
            R.id.secNextVisitPlan -> R.id.nvpFoldBody
            R.id.secPhoto -> R.id.photoFoldBody
            else -> return false
        }
        return findViewById<android.view.View>(bodyId)?.visibility != android.view.View.VISIBLE
    }

    /* 🟢🔒 V1502 — সেকশনের id থেকে তার `docFolds` চাবি (attachFold-এ যে নামে
       জমা হয়েছিল)। `sectionClosed()`-এর ঠিক একই ৮টা সেকশন, শুধু body-id-র
       বদলে চাবি-নাম ফেরত দেয়। */
    private fun sectionFoldKey(sectionId: Int): String? = when (sectionId) {
        R.id.secHistory -> "hist"
        R.id.secClinical -> "clin"
        R.id.secCounsel -> "couns"
        R.id.secEstimate -> "estm"
        R.id.secDocRemark -> "drem"
        R.id.secTodayTreat -> "ttd"
        R.id.secNextVisitPlan -> "nvp"
        R.id.secPhoto -> "photo"
        else -> null
    }

    /** এই ভাঁজটা প্রোগ্রাম থেকেই খুলে দেওয়া (ক্লিক না করেও) — বন্ধ থাকলে তবেই,
     *  খোলা থাকলে কিছুই বদলায় না। `attachFold`-এর ক্লিক-হ্যান্ডলারের হুবহু
     *  একই দুটো কাজ (body দেখানো + chevron ঘোরানো + SAVED-বাক্স ঠিক করা)। */
    private fun openFold(key: String) {
        val f = docFolds[key] ?: return
        if (f.body.visibility == android.view.View.VISIBLE) return
        f.body.visibility = android.view.View.VISIBLE
        f.chev.text = "⌃"
        f.body.post { refreshSavedGroup() }
    }

    private fun wireSavedGroup() {
        val head = findViewById<android.view.View>(R.id.savedGroupHead) ?: return
        val body = findViewById<android.view.View>(R.id.savedGroupBody) ?: return
        val chev = findViewById<TextView>(R.id.savedGroupChev) ?: return
        body.visibility = android.view.View.GONE
        chev.text = "⌄"
        head.setOnClickListener {
            savedGroupOpen = !savedGroupOpen
            body.visibility = if (savedGroupOpen) android.view.View.VISIBLE else android.view.View.GONE
            chev.text = if (savedGroupOpen) "⌃" else "⌄"
        }
    }

    /** ভরা-ও-বন্ধ ধাপগুলো নিচের বাক্সে নামায়, বাকিগুলো উপরে ফেরায়। */
    private fun refreshSavedGroup() {
        if (savedGroupBusy) return
        try {
            val group = findViewById<android.view.View>(R.id.secSavedGroup) ?: return
            val body = findViewById<android.view.ViewGroup>(R.id.savedGroupBody) ?: return
            val host = group.parent as? android.view.ViewGroup ?: return
            savedGroupBusy = true

            var down = 0
            for (id in savedGroupIds) {
                val card = findViewById<android.view.View>(id) ?: continue
                val isDown = card.parent === body
                /* 🔒 V1525 — Save/restore হয়ে গেলে completed lifetime section আর
                   active অংশে থাকবে না। খোলা/বন্ধ যাই হোক, নিচের SAVED group-এই
                   থাকবে; ভিতরটা আবার সত্যিই ফাঁকা হলে তবেই আগের জায়গায় ফিরবে। */
                /* 💾 V1255 — সেভ না হওয়া পর্যন্ত কিছুই নামে না। আগে শুধু
                   "ভরা" দেখা হত, তাই Registration থেকে আসা History আর
                   লেআউটে আগে থেকে লেখা দরের ঘরগুলোর জন্য Counselling —
                   দুটোই না-ছোঁয়া অবস্থাতেই "SAVED" বাক্সে নেমে যেত। */
                // V1525: Once a lifetime check-up section is saved/restored, keep it in the
                // bottom SAVED group even while expanded. This makes completed sections
                // visually distinct and prevents them from jumping back into the active area.
                val goesDown = savedInThisScreen && sectionFilled(card)
                if (goesDown && !isDown) {
                    (card.parent as? android.view.ViewGroup)?.removeView(card)
                    body.addView(card)
                } else if (!goesDown && isDown) {
                    body.removeView(card)
                    // ⛔ ঠিক নিজের ক্রমে ফেরে: এর পরের যে ধাপটা এখনো উপরে আছে
                    //    তার ঠিক আগে; কেউ না থাকলে SAVED বাক্সটার ঠিক আগে।
                    var at = host.indexOfChild(group)
                    var after = false
                    for (nid in savedGroupIds) {
                        if (nid == id) { after = true; continue }
                        if (!after) continue
                        val nxt = findViewById<android.view.View>(nid) ?: continue
                        val idx = host.indexOfChild(nxt)
                        if (idx >= 0) { at = idx; break }
                    }
                    host.addView(card, at)
                }
                // ⛔ নিচে থাকলেও **খোলা অবস্থায় ফিকে নয়** — লেখার সময় ঝাপসা
                //    পর্দায় ডাক্তার কাজ করতে পারবেন না।
                // Saved/completed sections stay grey so the doctor can immediately see
                // what is already completed. Opening one for review does not make it look active.
                card.alpha = if (goesDown) 0.55f else 1.0f
                if (goesDown) down++
            }

            findViewById<TextView>(R.id.savedGroupNum)?.text = down.toString()
            group.visibility = if (down > 0) android.view.View.VISIBLE else android.view.View.GONE
            if (down == 0) {
                savedGroupOpen = false
                body.visibility = android.view.View.GONE
                findViewById<TextView>(R.id.savedGroupChev)?.text = "⌄"
            }
            /* 🟢🔒 V1536 (১৮.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — "SAVED · N ⌄"
               লেখাটার বদলে একবারের তথ্যগুলো (Complaint/Grade/DRE-Proctoscopy/
               Treatment Plan/Estimated Cost/Photos) ছোট করে এক নজরে দেখানো।
               ⛔ এডিট/সেভ-এর কোনো নিয়ম ছোঁয়া হয়নি — মাথায় (savedGroupHead)
               চাপলে আগের মতোই নিচের আসল ঘরগুলো খুলবে, ওখানেই বদলানো যাবে। */
            renderSavedGroupPreview(down)
        } catch (_: Throwable) {
            // ⛔ এখানে কিছু ভুল হলেও চেকআপ পর্দা ভাঙে না — সাজানোটা শুধু দেখার জিনিস।
        } finally {
            savedGroupBusy = false
        }
    }

    /* 🟢🔒 V1536 (১৮.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফে পাশ) — "History/Clinical/
       Estimate/Photo জীবনে একবারই" তথ্যগুলো ছোট, পাশাপাশি দুই-কলামে এক
       নজরে (TK-এর কথায় "Patient Treatment Summary")। ⛔ শুধু দেখার জিনিস —
       `collect()`-এর হুবহু একই মান পড়ে, কোনো নতুন সেভ/নিয়ম নেই। ফাঁকা ঘর
       (কিছুই লেখা হয়নি) দেখানো হয় না (৭ক-৩: ডেটা না থাকলেও ঠিক দেখাতে হবে)। */
    private fun renderSavedGroupPreview(down: Int) {
        val box = findViewById<android.view.ViewGroup>(R.id.savedGroupPreview) ?: return
        box.removeAllViews()
        if (down == 0) return
        val r = try { collect() } catch (_: Throwable) { return }
        val photos = listOf(
            "Before" to r.beforePhoto, "During" to r.duringPhoto, "After" to r.afterPhoto
        ).filter { it.second.isNotBlank() }.map { it.first }
        val rows = listOf(
            "Complaint" to r.complaint,
            "Duration" to r.duration,
            "Grade" to r.grade,
            "DRE / Proctoscopy" to listOf(r.dre, r.proctoscopy).filter { it.isNotBlank() }.joinToString(", "),
            "Treatment Plan" to r.treatmentPlan,
            "Estimated Cost" to r.estimatedCost,
            "Photos" to photos.joinToString(", ")
        ).filter { it.second.isNotBlank() }
        if (rows.isEmpty()) return
        var i = 0
        while (i < rows.size) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            line.addView(previewCell(rows[i]))
            if (i + 1 < rows.size) line.addView(previewCell(rows[i + 1]))
            box.addView(line)
            i += 2
        }
        // "View full history →" — আগে থেকেই থাকা openCheckupHistory()-ই ডাকা হয়,
        // নতুন কোনো ডেটা-লজিক নেই।
        box.addView(TextView(this).apply {
            text = "View full history →"
            setTextColor(android.graphics.Color.parseColor("#0B5E8A")); textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
            isClickable = true; isFocusable = true
            setOnClickListener { openCheckupHistory() }
        })
    }

    private fun previewCell(kv: Pair<String, String>): android.view.View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(5), dp(2), dp(5))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@DoctorCheckupActivity).apply {
                text = kv.first.uppercase()
                setTextColor(android.graphics.Color.parseColor("#5A6B76")); textSize = 9.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(TextView(this@DoctorCheckupActivity).apply {
                text = kv.second
                setTextColor(android.graphics.Color.parseColor("#101828")); textSize = 12.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
            })
        }

    /** ধাপটা নিচের "SAVED" বাক্সের ভিতরে থাকলে বাক্সটা খুলে দেয়। */
    private fun openSavedGroupIfHolding(target: android.view.View) {
        try {
            val body = findViewById<android.view.ViewGroup>(R.id.savedGroupBody) ?: return
            if (target.parent !== body) return
            savedGroupOpen = true
            body.visibility = android.view.View.VISIBLE
            findViewById<TextView>(R.id.savedGroupChev)?.text = "⌃"
        } catch (_: Throwable) { }
    }

    /** ScrollView-এর ভিতরে কোনো View আসলে কত নিচে — নিজের বাবার সাপেক্ষে নয়।
     *  🔴 V1119-এ দরকার হলো: ধাপের কার্ড এখন SAVED বাক্সের ভিতরেও থাকতে পারে,
     *  তখন `card.top` ওই বাক্সের সাপেক্ষে — পুরনো হিসাবে স্ক্রল ভুল জায়গায় যেত। */
    private fun yInScroll(v: android.view.View): Int {
        var y = 0
        var cur: android.view.View = v
        while (true) {
            y += cur.top
            val p = cur.parent as? android.view.View ?: break
            if (p.id == R.id.stepScroll) break
            cur = p
        }
        return y
    }


    /** বন্ধ অবস্থাতেও ভিতরে কতগুলো টিক পড়েছে সেটা মাথার সবুজ ব্যাজে দেখানো। */
    private fun refreshNvpFold() {
        var n = 0
        for ((_, cb) in nvpBoxes()) if (cb.isChecked) n++
        setFoldCount("nvp", n)
    }

    /** বন্ধ অবস্থাতেও ধাপ ২-এ কতগুলো ভরা আছে সেটা মাথার ব্যাজে দেখানো।
     *  ⛔ চেকবক্সে নতুন শোনার-কাজ (listener) বসানো হয়নি — `buildChecks`-এর
     *     নিজের `setOnCheckedChangeListener` (পিলের রং) তাহলে মুছে যেত।
     *     তাই সংখ্যাটা বসে: শুরুতে · পুরোনো রেকর্ড খোলার পরে · আর ভাঁজ
     *     খোলা-বন্ধ করার সময় (ব্যাজ তো বন্ধ অবস্থাতেই দেখা যায়)। */
    private fun refreshClinicalFold() {
        var n = 0
        n += visualChecks.count { it.isChecked }
        n += dreChecks.count { it.isChecked }
        if (findViewById<android.widget.EditText>(R.id.etDreOther)?.text?.toString()?.isNotBlank() == true) n++
        if (findViewById<android.widget.EditText>(R.id.etProctoscopy)?.text?.toString()?.isNotBlank() == true) n++
        if (findViewById<android.widget.EditText>(R.id.etOnProbing)?.text?.toString()?.isNotBlank() == true) n++
        setFoldCount("clin", n)
    }

    /* ═══════════════════════════════════════════════════════════════════
       🔵🔒 V555 (২২.০৮.২০২৬, TK-অনুমোদিত ডেমো) — কাগজের **ভাগ ৩**: চারটে "ইতিহাস"।
       TK-এর নির্দেশ হুবহু: **টিকের জিনিস পাশাপাশি চিপ**, **লেখার জিনিস বক্স**,
       আর **প্রতিটা প্রশ্নে একাধিক উত্তর** বাছা যাবে।
       ⛔ চিপ পাশাপাশি বসানোর নিয়ম প্রজেক্টের নিজের `buildChecks`-এর ধরনেই
          (দুটো করে এক সারিতে); শুধু লম্বা লেখা একাই পুরো সারি পায় — নইলে ফোনের
          পর্দায় কেটে যেত। নিয়মটা `HistoryDetailModel.rowsFor()`-এ, তাই চালিয়ে যাচাই করা।
       ⛔ কাগজের একটাও শব্দ বদলানো হয়নি।
       ═══════════════════════════════════════════════════════════════ */
    private val historyChips = LinkedHashMap<String, MutableList<TextView>>()

    /** 🔵 V574 — কোন দলে কোন চিপগুলো, যাতে মাথার সংখ্যাটা গোনা যায়। */
    private val historyFoldChips = LinkedHashMap<String, MutableList<TextView>>()

    private fun histChipBg(on: Boolean): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = symDp(20).toFloat()
            setColor(android.graphics.Color.parseColor(if (on) "#EAFBF0" else "#FFFFFF"))
            setStroke(symDp(1), android.graphics.Color.parseColor(if (on) "#0B4F2A" else "#DBE2EA"))
        }

    private fun paintHistoryChip(chip: TextView) {
        val on = chip.getTag(R.id.historyDetailGroup) == true
        chip.background = histChipBg(on)
        chip.setTextColor(android.graphics.Color.parseColor(if (on) "#0B4F2A" else "#7C8A9C"))
        chip.setTypeface(chip.typeface, if (on) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun buildHistoryDetailRows() {
        val box = findViewById<android.widget.LinearLayout>(R.id.historyDetailGroup) ?: return
        box.removeAllViews()
        historyChips.clear()
        historyFoldChips.clear()
        for ((gi, group) in HistoryDetailModel.GROUPS.withIndex()) {
            /* 🔵 V574 (TK-নির্দেশ): *"রক্তপাতের ইতিহাস / ব্যথার ইতিহাস / ফোলা
               মাংসপিন্ডের ইতিহাস / পুজ ও জল পড়ার ইতিহাস — এগুলো এখানে চাপ
               দিলে তখনই ফর্মগুলো ওপেন হবে"*। দলের নামটাই এখন মাথা, ডান দিকে
               কতগুলো বাছা হয়েছে তার সবুজ ব্যাজ আর `⌄` চিহ্ন। */
            val foldKey = "hist" + gi
            /* 🟢🔒🔒 V653 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবিসহ — "চাপ দিলে ফর্মটা
               খোলে এটা এখন বোঝাই যাচ্ছে না, লেখার ডান পাশে থাকবে, আরো
               উজ্জ্বল") — **আসল কারণ:** শিরোনাম-লেখার `layout_weight=1`
               ছিল — তাই লেখা যতই ছোট হোক, বাকি পুরো জায়গা তার নিজের হয়ে
               যেত, আর ⌄ চিহ্নটা পর্দার একদম ডান কিনারায় গিয়ে বসত —
               লেখা আর চিহ্নের মধ্যে বিশাল ফাঁকা জায়গা। **সমাধান:** weight
               সরানো হলো (শিরোনাম এখন নিজের লেখার মাপেই থাকে), তাই ⌄
               এখন লেখার ঠিক পরেই বসে। রংও ধূসর থেকে গাঢ় সবুজ করা হলো
               (আরও স্পষ্ট বোঝা যায় এটা চাপার জিনিস)। */
            val head = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setPadding(0, symDp(10), 0, symDp(8))
            }
            head.addView(TextView(this).apply {
                text = group.title
                textSize = 15f   // 🎨 V897 — TK: "ফন্টের সাইজ সামান্য বড় করতে হবে"
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0B2B59"))
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            val num = TextView(this).apply {
                textSize = 11.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                setBackgroundResource(R.drawable.bg_fold_count_chip)
                setPadding(symDp(9), symDp(2), symDp(9), symDp(2))
                visibility = android.view.View.GONE
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginStart = symDp(8)
                layoutParams = lp
            }
            head.addView(num)
            val chev = TextView(this).apply {
                text = "\u2304"
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_fold_chevron)
                val lp = android.widget.LinearLayout.LayoutParams(symDp(24), symDp(24))
                lp.marginStart = symDp(8)
                layoutParams = lp
            }
            head.addView(chev)
            // 🟢🔒 V653 — চিহ্নের পরে একটা নমনীয় (weight=1) ফাঁকা জায়গা,
            // যাতে পুরো গুচ্ছটা (লেখা+ব্যাজ+চিহ্ন) বাঁ দিকে জড়ো থাকে আর
            // সারিটা তবুও সম্পূর্ণ চওড়া জুড়ে চাপার-যোগ্য থাকে (মাঝের ফাঁকা
            // জায়গায় চাপলেও খোলে/বন্ধ হয়)।
            head.addView(android.view.View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            })
            box.addView(head)

            val body = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            box.addView(body)
            attachFold(foldKey, head, num, chev, body)
            val groupChips = ArrayList<TextView>()
            historyFoldChips[foldKey] = groupChips

            for (q in group.questions) {
                if (q.label.isNotBlank()) body.addView(TextView(this).apply {
                    text = q.label
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                    setPadding(0, symDp(5), 0, symDp(3))
                })
                val field = HistoryDetailModel.fieldKey(group, q)
                val chips = ArrayList<TextView>()
                for (rowOptions in HistoryDetailModel.rowsFor(q.options)) {
                    val row = android.widget.LinearLayout(this).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        val lp = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        lp.topMargin = symDp(4)
                        layoutParams = lp
                    }
                    for ((i, opt) in rowOptions.withIndex()) {
                        val chip = TextView(this).apply {
                            text = opt
                            textSize = 13f
                            gravity = android.view.Gravity.CENTER
                            setPadding(symDp(10), symDp(6), symDp(10), symDp(6))
                            isClickable = true
                            setTag(R.id.historyDetailGroup, false)
                            tag = opt
                            val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            if (i > 0) lp.marginStart = symDp(6)
                            layoutParams = lp
                        }
                        // TK: প্রতিটা প্রশ্নে একাধিক উত্তর — তাই চাপে শুধু নিজেরটাই বদলায়
                        chip.setOnClickListener {
                            chip.setTag(R.id.historyDetailGroup, chip.getTag(R.id.historyDetailGroup) != true)
                            paintHistoryChip(chip)
                            refreshHistoryFolds()   // 🔵 V574 — মাথার সংখ্যা সঙ্গে সঙ্গে
                        }
                        paintHistoryChip(chip)
                        chips.add(chip)
                        row.addView(chip)
                    }
                    body.addView(row)
                }
                historyChips[field] = chips
                groupChips.addAll(chips)
            }
        }
        refreshHistoryFolds()
    }

    /** 🔵 V574 — চারটে দলের মাথায় কতগুলো বাছা হয়েছে সেটা নতুন করে বসানো। */
    private fun refreshHistoryFolds() {
        for ((key, chips) in historyFoldChips) {
            setFoldCount(key, chips.count { it.getTag(R.id.historyDetailGroup) == true })
        }
    }

    /* ═══ 🔵🔒 V556 (TK-অনুমোদিত ডেমো) — কাগজের ভাগ ৪-এর নতুন অংশ।
       ⛔ চিপ বসানো, একাধিক বাছাই, লেখার বক্স — সবই ভাগ ৩-এর হুবহু একই নিয়মে।
       ⛔ জল-এর ঘরটা সংখ্যার, পাশে "লিটার" — কাগজে যেমন "—— লিটার"। ═══ */
    private val lifestyleChips = LinkedHashMap<String, MutableList<TextView>>()

    /* 🔔 V557: গতবার যে খরচে নোটিশ গিয়েছিল। এটা ধরেই ঠিক হয় নতুন নোটিশ যাবে
       কি না — নইলে প্রতিবার Save-এ স্টাফের ঘণ্টা অকারণে বাজত। */
    private var lastSavedCost: String = ""

    // ══════════════════════════════════════════════════════════════════
    // 🔵🔒 V558 (২২.০৮.২০২৬, TK-অনুমোদিত) — রোগের ছবি
    //
    // TK-এর কথা: *"ডাক্তার চাইলে যে কোন ফটোর উপর যেন সেই কাজটা করতে
    // পারে"* এবং *"মাংসের উপরে আঙুল দিয়ে টান দিলে যেন মাংস বেড়ে যায়"*।
    //
    // উপরে ছবি বাছার সারি · মাঝে আঁকার পর্দা · নিচে কাজের বোতাম।
    // ⛔ নতুন কলাম বা SQL লাগেনি — লেখাটা চেকআপের সাথেই জমা হয়।
    // ══════════════════════════════════════════════════════════════════

    private var anatomyView: AnatomyView? = null
    private val anatomyThumbs = mutableListOf<android.widget.ImageView>()

    /** ছবির নাম → ফোনের ভিতরের ছবি। না পেলে ০, তখন ছবিটা বাদ যায়। */
    private fun anatomyResId(key: String): Int = try {
        resources.getIdentifier(key, "drawable", packageName)
    } catch (_: Throwable) { 0 }

    /* 🔵🔒 V600 (২৩.০৮.২০২৬) — বাক্সের চওড়া অনুযায়ী উচ্চতা ছবির নিজের
       অনুপাতে বসায়। চওড়া এখনো মাপা না হলে (পর্দা প্রথমবার আঁকার সময়)
       ViewTreeObserver দিয়ে একবার অপেক্ষা করে, তারপর মাপে। */
    private fun fitAnatomyHolderToImage(holder: android.widget.FrameLayout, bmp: android.graphics.Bitmap?) {
        if (bmp == null || bmp.width <= 0 || bmp.height <= 0) return
        val minH = symDp(220)
        val maxH = symDp(700)
        fun apply() {
            val w = holder.width
            if (w <= 0) return
            var h = (w.toFloat() * bmp.height / bmp.width).toInt()
            if (h < minH) h = minH
            if (h > maxH) h = maxH
            val lp = holder.layoutParams ?: return
            if (lp.height == h) return
            lp.height = h
            holder.layoutParams = lp
        }
        if (holder.width > 0) {
            apply()
        } else {
            /* 🟢🔒 V704 — আগে প্রথমবার layout হলেই শোনাটা খুলে নেওয়া হত। ধাপ ৫-এ
               আসার পরে বাক্সটা শুরুতে **বন্ধ ভাঁজের ভিতরে** থাকে, তাই তখন চওড়া
               ০-ই থাকে — একবার শুনেই খুলে নিলে পরে ভাঁজ খুললেও উচ্চতা আর বসত না
               (ছবি বিকৃত দেখাত)। ⇒ এখন চওড়া সত্যিই মাপা না হওয়া পর্যন্ত শোনা
               চালু থাকে, মাপা হলেই একবার বসিয়ে নিজে থেকে খুলে যায়।
               ⛔ মাপার হিসাব (`apply`) এক অক্ষরও বদলায়নি। */
            holder.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (holder.width <= 0) return
                    holder.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    apply()
                }
            })
        }
    }

    private fun buildAnatomyBoard() {
        val holder = findViewById<android.widget.FrameLayout>(R.id.anatomyHolder) ?: return
        val strip = findViewById<android.widget.LinearLayout>(R.id.anatomyStrip) ?: return
        val tools = findViewById<android.widget.LinearLayout>(R.id.anatomyTools) ?: return

        val view = AnatomyView(this)
        anatomyView = view
        /* 🔵 V585 (২৩.০৮.২০২৬, TK-অনুমোদিত) — ডাক্তার "কেন্দ্র" ছুঁয়ে দিলে ওই
           **ছবির জন্য** ফোনেই জমা হয় (SharedPreferences), তাই ওই ছবিতে আর
           কখনো জিজ্ঞাসা করতে হয় না। ⛔ একটাও ক্লাউড-কল নেই — TK: *"আমি ফ্রি
           প্লানে চালাতে চাই"*। */
        view.onCentreSet = { x, y ->
            AnatomyClock.setCentre(this, view.picKeyNow(), x, y)
            Toast.makeText(this, NoBengali.s("কেন্দ্র বসানো হলো — এবার চিহ্ন দিলেই ঘড়ির সময় নিজে বসবে"),
                Toast.LENGTH_SHORT).show()
        }
        /* 🟢🔒 V626 (২৪.০৮.২০২৬, TK-নির্দেশ, ডেমো-প্রুফ অনুমোদিত) — "ফোলান"
           ছবির ভুল জায়গায় (মলদ্বারের কেন্দ্র থেকে দূরে) চাপা হলে একটা মনে
           করিয়ে দেওয়া বার্তা — কেন্দ্র না জানা থাকলে আগে সেটা ঠিক করতে বলা,
           জানা থাকলে কাছাকাছি চাপতে বলা। */
        view.onBulgeBlocked = {
            val msg = if (view.clockCentre == null)
                "আগে ⊕ কেন্দ্র দিয়ে পায়ুপথের মাঝখানে একবার ছুঁয়ে দিন — তবেই ফোলান কাজ করবে"
            else
                "শুধু মলদ্বারের কাছেই ফোলানো যায় — এখানে নয়"
            Toast.makeText(this, NoBengali.s(msg), Toast.LENGTH_SHORT).show()
        }
        /* 🔵🔒 V600 (২৩.০৮.২০২৬, TK-অনুমোদিত "Option 1", ছবি-প্রুফ পাশ) —
           TK: "মোবাইল ডিসপ্লে ডানে এবং দুদিকেই জায়গা আছে, তাহলে ফটোটা এত
           চাপা কেন?" — কারণ বাক্সের উচ্চতা fixed ৩০০dp ছিল, লম্বা ছবির
           (যেমন anat26, 408×628) দুই পাশে ফাঁকা থেকে যেত।
           এখন ছবি বসা/বদলানো মাত্রই বাক্সের উচ্চতা সেই ছবির **নিজের
           অনুপাতে** বসানো হয় — তাই কোনো ছবিতেই দুই পাশ ফাঁকা থাকে না,
           ছবিও বেঁকায় না (Option 2-এর মতো নয়)। প্রতিটা ছবির অনুপাত আলাদা
           হতে পারে বলে ৩৮০dp–৭০০dp-এর মধ্যে বাঁধা — কোনো অস্বাভাবিক লম্বা
           ছবি এলেও পর্দা অসীম লম্বা হয়ে যাবে না। */
        view.onBaseImageSet = { bmp -> fitAnatomyHolderToImage(holder, bmp) }
        holder.removeAllViews()
        holder.addView(view, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT))

        // ── ছবি বাছার সারি ──
        buildAnatomyStrip(strip, view)


        // ── কাজের বোতাম ──
        // TK: *"ছবিটা আঁকার সময় কোন প্রকার ভুল যদি হয়ে থাকে, ব্যাকে যাওয়ার
        // কোন অপশন নাই কেন, অথবা সেই ভুলটুকু মোছার মতন কোন অপশন নেই কেন?"*
        //
        // বোতামগুলো ছিলই, কিন্তু এক লাইনে বসানোয় ডান দিকেরগুলো পর্দার বাইরে
        // চলে যেত — ভুল শোধরানোর বোতামই হাতে পাওয়া যেত না। এখন পর্দার চওড়া
        // মেপে মেপে সারিতে ভাগ করা হয়, তাই ছোট ফোনেও **সব বোতাম দেখা যায়**।
        // সাথে "সব মুছুন" যোগ হল — এতদিন কাজটা কোডে ছিল, বোতাম ছিল না।
        /* 🔵🔒 V570 (২২.০৮.২০২৬, TK-অনুমোদিত "প্রস্তাব ক") — বোতামের সারির নতুন
           চেহারা। TK: *"এগুলো ... দেখতে ভালো লাগছে? আমার কাছে তো প্রফেশনালহীন
           মনে"*। **একটা বোতামও বাদ যায়নি** — সংখ্যা সমস্যা ছিল না, চেহারা ছিল।
           ইমোজি + বাংলা লেখা মিলিয়ে আটটা চওড়া বোতাম এক সারিতে ধরত না, তাই
           ২–৩ সারিতে ভেঙে ছবির অনেকটা ঢেকে দিত (V568-এ "গোল" কেটেও যাচ্ছিল)।
           এখন পরিষ্কার আইকন — নয়টাই **এক সারিতে**, আর কোন হাতিয়ারটা চলছে ও
           কী করতে হবে সেটা নিচে **একটাই লাইনে** লেখা থাকে।
           ⚠️ ওয়েবের `wlv1AnatBarHtml()`-এর হুবহু যমজ, আইকনের পথও এক। */
        tools.removeAllViews()
        anatomyBar = buildToolBar(tools, view, dark = false, full = false)
    }

    /** নিচের এক-লাইনের লেখাটা — কোন হাতিয়ার চলছে, কী করতে হবে। */
    private var anatomyBar: TextView? = null
    /** পুরো পর্দা বন্ধ করার পরে ছোট সারিটা আবার ঠিক হাতিয়ারে রং করার পথ। */
    private var anatomyRepaint: ((AnatomyView.Tool) -> Unit)? = null

    private fun toolTip(t: AnatomyView.Tool): String = when (t) {
        AnatomyView.Tool.BULGE -> "ফোলান — মাংসের উপরে আঙুল টানুন"
        AnatomyView.Tool.PILE  -> "চিহ্ন — যেখানে চিহ্ন দেবেন সেখানে ছুঁয়ে দিন"
        AnatomyView.Tool.TRACT -> "নালী — নালীর পথ ধরে আঙুল টানুন"
        AnatomyView.Tool.RING  -> "গোল — যেটা ঘিরে দেখাবেন তার উপরে টানুন"
        AnatomyView.Tool.ARROW -> "তীর — যেদিকে দেখাবেন সেদিকে টানুন"
        AnatomyView.Tool.ERASE -> "মুছুন — যে দাগটা তুলবেন তার উপরে ছুঁয়ে দিন"
        AnatomyView.Tool.PEN   -> "কলম — আঙুল দিয়ে লিখুন"
        /* 🔴🔒 V793 (TK: *"ফিসারের দাগটা যেন আমি বেকা আঁকতে পারি … আঙুল
           দিয়ে যেখানে ঘষা দিব সেখানে যেন দাগ হয়ে যায়"*) */
        AnatomyView.Tool.FISSURE -> "ফাটল — ফাটল যে বরাবর, সেই বরাবর আঙুল টানুন"
        /* 🔵 V585 (TK-নির্দেশ) — নতুন হাতিয়ার */
        AnatomyView.Tool.CENTRE -> "কেন্দ্র — পায়ুপথের ঠিক মাঝখানে একবার ছুঁয়ে দিন"
    }

    /**
     * এক সারিতে আইকনের বোতাম + নিচে এক লাইনের লেখা। ছোট বোর্ড আর পুরো পর্দা,
     * দুটোতেই এই একই ফাংশন — তাই দুটোর চেহারা কখনো আলাদা হয়ে যাবে না।
     * `full=true` হলে "পুরো পর্দা" বোতামটা থাকে না (ওখানে ✕ উপরেই আছে)।
     */
    private fun buildToolBar(box: android.widget.LinearLayout, view: AnatomyView,
                             dark: Boolean, full: Boolean): TextView {
        box.removeAllViews()
        box.orientation = android.widget.LinearLayout.VERTICAL

        val row = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val tip = TextView(this).apply {
            textSize = 11.5f
            gravity = android.view.Gravity.CENTER
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(if (dark) "#FFFFFF" else "#5B6B81"))
            setPadding(0, symDp(3), 0, 0)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val icons = ArrayList<AnatToolIcon>()
        fun paintOn(chosen: AnatomyView.Tool) {
            for (ic in icons) ic.on = (ic.tag as? AnatomyView.Tool) == chosen
            // 🟢 V589 (TK-নির্দেশ) — নিচের নির্দেশ-লেখাটা আর লেখা হয় না।
        }
        fun addIcon(kind: String, desc: String, tool: AnatomyView.Tool?,
                    danger: Boolean = false, click: () -> Unit): AnatToolIcon {
            val ic = AnatToolIcon(this, kind).apply {
                this.darkBar = dark
                this.danger = danger
                contentDescription = desc
                tag = tool
                layoutParams = android.widget.LinearLayout.LayoutParams(0, symDp(38), 1f)
                setOnClickListener { click() }
            }
            row.addView(ic)
            if (tool != null) icons.add(ic)
            return ic
        }
        fun addSep() {
            // 🔴 V575 বিল্ড-ফিক্স (TK-এর Android Studio-র স্ক্রিনশট, ২৩.০৮.২০২৬):
            //    এখানে খালি `View(this)` লেখা ছিল, কিন্তু এই ফাইলে
            //    `android.view.View` import করা নেই — তাই "Unresolved reference:
            //    View / setBackgroundColor / layoutParams" এসে বিল্ড ভাঙত।
            //    ফাইলের বাকি জায়গার মতোই পুরো নাম লিখে দেওয়া হলো।
            row.addView(android.view.View(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor(if (dark) "#3AFFFFFF" else "#D7E1EC"))
                layoutParams = android.widget.LinearLayout.LayoutParams(symDp(1), symDp(22))
                    .apply { setMargins(symDp(3), 0, symDp(3), 0) }
            })
        }

        /* 🟢🔒 V589 (২৩.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) —
           *"লোকেশনের মতন যেটা দেখতে সেটা প্রথমে রাখবেন, আর সেটাই পাইলসের মাংস ·
             তারপরে দাগ থাকবে যেটা ফিস্টুলার দাগ দেখানো যাবে"*
           ⇒ ক্রম বদলাল: **📍 (লোকেশনের মতো) সবার আগে**, তারপর **〰️ দাগ (নালী ·
             ফিস্টুলা)**, তারপর বাকিগুলো আগের সেই ক্রমেই।
           *"দ্বিতীয় ফটোতে যে তীর চিহ্ন আছে, ওটা আমার লাগবে না"*
           ⇒ **তীর বোতামটা সারি থেকে বাদ**।
           ⛔ `Tool.ARROW` ও তীর আঁকার কোড **মোছা হয়নি** — পুরোনো কোনো ছবিতে
              তীর আঁকা থাকলে সেটা আগের মতোই দেখা যায়, হারায় না। শুধু নতুন করে
              তীর আঁকার বোতামটা আর নেই। */
        // 🟢🔒🔒 V673 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবি-প্রুফ পাশ — "প্রথমটা থাকবে
        // পাইলসের মাংস ফোলানোর জন্য, দ্বিতীয়টা ফিস্টুলার নালি দেখানোর জন্য,
        // তারপরে যেগুলো ছিল পরপর থাকবে") — ক্রম বদলাল: ফোলান প্রথমে, নালী
        // দ্বিতীয়, তারপর বাকিগুলো (চিহ্ন/গোল/মুছুন) আগের সেই ক্রমেই।
        val toolList = listOf(
            Triple("bulge", "ফোলান", AnatomyView.Tool.BULGE),
            Triple("tract", "নালী",   AnatomyView.Tool.TRACT),
            Triple("fis",   "ফাটল",   AnatomyView.Tool.FISSURE),   // 🔴 V793
            Triple("pile",  "চিহ্ন",  AnatomyView.Tool.PILE),
            Triple("ring",  "গোল",    AnatomyView.Tool.RING),
            Triple("erase", "মুছুন",  AnatomyView.Tool.ERASE),
            /* 🔵 V585 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) — "কেন্দ্র"।
               ছবির পায়ুপথের মাঝখানে একবার ছুঁয়ে দিলে ওই ছবির জন্য মনে থাকে,
               তারপর প্রতিটা চিহ্নের o'clock নিজে হিসাব হয়। */
            Triple("centre", "কেন্দ্র", AnatomyView.Tool.CENTRE)
        )
        for ((kind, desc, tool) in toolList) {
            addIcon(kind, desc, tool) {
                view.tool = tool
                /* 🔵 V585 — আগে "চিহ্ন" বাছলেই ঘড়ির তালিকা খুলত আর সেই একটা
                   লেখাই এরপর সব চিহ্নে বসে যেত (TK: *"যেখানেই থাকবে চারটা কেন
                   বাঁচবে"*)। এখন তালিকাটা আর খোলে না — এক ছোঁয়াতেই চিহ্ন বসে,
                   o'clock নিজে হিসাব হয়। কেন্দ্র জানা না থাকলে অ্যাপ একবার
                   মনে করিয়ে দেয়।
                   ⛔ `askPileLabel()` ফাংশনটা মোছা হয়নি (TK-এর নিয়ম: নিজে থেকে
                      কিছু সরাই না) — শুধু আর ডাকা হয় না। */
                if (tool == AnatomyView.Tool.PILE && view.clockCentre == null) {
                    Toast.makeText(this@DoctorCheckupActivity,
                        NoBengali.s("আগে ⊕ কেন্দ্র দিয়ে পায়ুপথের মাঝখানে একবার ছুঁয়ে দিন — তবেই ঘড়ির সময় নিজে বসবে"),
                        Toast.LENGTH_LONG).show()
                }
                paintOn(tool)
            }
        }
        addSep()
        // ↺ একটা পিছনে — শেষ যেটা আঁকা হয়েছে সেটা তুলে নেয়
        addIcon("undo", "একটা পিছনে", null) {
            if (view.markCount() == 0)
                Toast.makeText(this@DoctorCheckupActivity, NoBengali.s("মোছার মত কিছু নেই"), Toast.LENGTH_SHORT).show()
            else view.undo()
        }
        // 🗑 সব মুছুন — জিজ্ঞাসা করে তবেই, নইলে ভুল করে চাপ পড়লে সব চলে যেত
        addIcon("trash", "সব মুছুন", null, danger = true) {
            if (view.markCount() == 0) {
                Toast.makeText(this@DoctorCheckupActivity, NoBengali.s("মোছার মত কিছু নেই"), Toast.LENGTH_SHORT).show()
            } else {
                android.app.AlertDialog.Builder(this@DoctorCheckupActivity)
                    .setMessage("All marks on the picture will be erased. Erase?")   /* 🔤 V728 */
                    .setNegativeButton("No", null)
                    .setPositiveButton("Yes") { _, _ -> view.clearMarks() }
                    // 🔴🔒 V610 (২৪.০৮.২০২৬) — V512-এর একই প্রমাণিত পথ।
                    .show().also { com.tkbiswas.pilesclinic.native.NoBengali.installDialog(it); com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) }   // 🤫 V774
            }
        }
        if (!full) {
            addSep()
            /* 🔵 V567 (TK): ছবিটা গোটা পর্দা জুড়ে খোলে, রোগীকে দেখানোর জন্য। */
            addIcon("full", "পুরো পর্দা", null) { openAnatomyFullScreen() }
        }

        box.addView(row)
        /* 🟢🔒 V589 (২৩.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) — *"ফোলান — মাংসের উপরে
           আঙুল টানুন — এই ধরনের ডেমি লেখা রাখা যাবে না"*
           ⇒ আইকনের নিচের এক-লাইনের নির্দেশ-লেখাটা আর বসানো হয় না।
           ⛔ ঘরটা (`tip`) মোছা হয়নি — এই ফাংশন সেটাই ফেরত দেয় আর ডাকার
              জায়গাগুলো আগের মতোই চলে; শুধু পর্দায় বসে না ও লেখা হয় না।
              দরকার হলে এক লাইনেই ফেরানো যাবে। */
        tip.visibility = android.view.View.GONE
        if (!full) anatomyRepaint = { t -> paintOn(t) }
        paintOn(view.tool)
        return tip
    }


    /**
     * 🔵🔒 V573 (২২.০৮.২০২৬, TK-অনুমোদিত) — ছবি বাছার সারি: **যোগ ও বিয়োগ**।
     *
     * TK: *"যেখানে সমস্ত ফটো আছে সেখানে যেন গ্যালারি থেকেও ফটো নেয়া যায় অথবা
     * ক্যামেরা থেকেও ফটো নিয়ে দেখানো যায় ... এর আগে যে সমস্ত ফটো আছে সেগুলো
     * আমরা চাইলে যোগ এবং বিয়োগ যেন করতে পারি"*।
     *
     * সারির প্রথমে **＋ ছবি যোগ** ঘর, আর প্রতিটা ছবির কোণে ছোট **✕**।
     * ⛔ ✕ ছবি **মোছে না** — শুধু তালিকা থেকে সরায়। পুরোনো চেক-আপে ওই ছবির
     *    উপরে আঁকা থাকলে সেটা আগের মতোই ঠিক দেখাবে।
     * ⚠️ ওয়েবের `wlv1AnatStripHtml()`-এর যমজ; তালিকা মেলানোর নিয়ম দু'জায়গাতেই
     *    `AnatomyModel.mergePictures()`।
     */
    private fun buildAnatomyStrip(strip: android.widget.LinearLayout, view: AnatomyView) {
        strip.removeAllViews(); anatomyThumbs.clear()

        // ＋ ছবি যোগ — ক্যামেরা বা গ্যালারি
        strip.addView(TextView(this).apply {
            text = "+\nAdd Photo"
            textSize = 10.5f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = symDp(8).toFloat()
                setColor(android.graphics.Color.parseColor("#F4F8FC"))
                setStroke(symDp(1), android.graphics.Color.parseColor("#9DB4CC"))
            }
            val lp = android.widget.LinearLayout.LayoutParams(symDp(62), symDp(62))
            lp.setMargins(0, 0, symDp(6), 0)
            layoutParams = lp
            setOnClickListener { showPhotoDialog(ANAT_PHOTO_TARGET) }
        })

        /* ♻ ফেরান — 🟢🔒 V591 (২৩.০৮.২০২৬), TK-এর প্রশ্ন: *"এখান থেকে যে ফটো
           গুলি বাদ দেওয়া হয়েছে — সে গুলি পরে আবার কিভাবে এবং কোথায় পাবো?"*
           সরানো ছবি একটাও না থাকলে এই ঘরটা **দেখাই যায় না**, তাই সারিতে
           বাড়তি কিছু যোগ হয় না। */
        val dropped = AnatomyPictureRepository.hiddenRows(this)
        if (dropped.isNotEmpty()) {
            strip.addView(TextView(this).apply {
                text = "♻\nRestore " + dropped.size
                textSize = 10.5f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#7A4A00"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = symDp(8).toFloat()
                    setColor(android.graphics.Color.parseColor("#FFF6E6"))
                    setStroke(symDp(1), android.graphics.Color.parseColor("#E0BE80"))
                }
                val lp = android.widget.LinearLayout.LayoutParams(symDp(62), symDp(62))
                lp.setMargins(0, 0, symDp(6), 0)
                layoutParams = lp
                contentDescription = "Restore removed photo"
                setOnClickListener { askRestorePicture(dropped, strip, view) }
            })
        }

        for (pic in AnatomyPictureRepository.pictures(this)) {
            val cloud = AnatomyModel.isCloudKey(pic.key)
            val bmp = if (cloud) PhotoUtils.decodeDataUrl(pic.photo) else null
            val resId = if (cloud) 0 else anatomyResId(pic.key)
            if (!cloud && resId == 0) continue           // ছবিটা নেই — চুপচাপ বাদ
            if (cloud && bmp == null) continue           // ছবিটা পড়া গেল না — চুপচাপ বাদ

            val box = android.widget.FrameLayout(this).apply {
                val lp = android.widget.LinearLayout.LayoutParams(symDp(62), symDp(62))
                lp.setMargins(0, 0, symDp(6), 0)
                layoutParams = lp
            }
            val img = android.widget.ImageView(this).apply {
                if (cloud) setImageBitmap(bmp) else setImageResource(resId)
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                tag = pic.key
                contentDescription = pic.label
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT)
                setPadding(symDp(2), symDp(2), symDp(2), symDp(2))
                setOnClickListener {
                    if (cloud) view.setPictureBitmap(pic.key, bmp) else view.setPicture(pic.key, resId)
                    paintAnatomyThumbs(pic.key)
                }
            }
            box.addView(img)
            box.addView(TextView(this).apply {
                text = "✕"
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.WHITE)
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(android.graphics.Color.parseColor("#EBB3261E"))
                    setStroke(symDp(1), android.graphics.Color.parseColor("#E6FFFFFF"))
                }
                contentDescription = "তালিকা থেকে সরান"
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    symDp(18), symDp(18), android.view.Gravity.TOP or android.view.Gravity.END)
                    .apply { setMargins(0, symDp(3), symDp(3), 0) }
                setOnClickListener { askDropPicture(pic, strip, view) }
            })
            strip.addView(box)
            anatomyThumbs.add(img)
        }
        paintAnatomyThumbs(anatomyView?.let { AnatomyModel.parse(it.save()).pic } ?: "")
    }

    /** ছবিটা তালিকা থেকে সরাব? — জিজ্ঞাসা করে তবেই। */
    private fun askDropPicture(pic: AnatomyModel.Picture,
                               strip: android.widget.LinearLayout, view: AnatomyView) {
        android.app.AlertDialog.Builder(this)
            .setMessage("\"" + pic.label + "\" — remove this picture from the list?\n\n" +
                        "ছবিটা মুছে যাবে না — পুরোনো চেক-আপে এর উপরে আঁকা থাকলে সেটা আগের মতোই দেখা যাবে।")
            .setNegativeButton("No", null)
            .setPositiveButton("Yes") { _, _ ->
                val row = if (AnatomyModel.isCloudKey(pic.key))
                    AnatomyPictureRepository.hideAddedRow(this, pic.key.removePrefix(AnatomyModel.CLOUD_PREFIX))
                else AnatomyPictureRepository.hideBuiltInRow(pic.key, pic.label)
                AnatomyPictureRepository.saveLocal(this, row)
                BackgroundWork.run { AnatomyPictureRepository.pushCloud(row) }
                buildAnatomyStrip(strip, view)
                Toast.makeText(this, NoBengali.s("তালিকা থেকে সরানো হলো"), Toast.LENGTH_SHORT).show()
            }
            .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
    }

    /**
     * 🟢🔒 V591 (২৩.০৮.২০২৬) — **সরানো ছবি ফেরানো**।
     *
     * TK: *"এখান থেকে যে ফটো গুলি বাদ দেওয়া হয়েছে — সে গুলি পরে আবার কিভাবে
     * এবং কোথায় পাবো?"*
     *
     * যাচাই করে পাওয়া সত্যি: ছবিগুলো কখনো মোছা হয়নি (`hidden = "1"` বসত
     * মাত্র), কিন্তু ফেরানোর কোনো পথ অ্যাপে ছিল না। এখন এই তালিকা থেকে যেটা
     * চাই সেটাই এক চাপে ফিরে আসে — আগের ক্রমেই, আগের নামেই।
     *
     * ⛔ নেটওয়ার্কের কাজটা (ডাক্তারের যোগ-করা ছবির ছবিটুকু নামানো) আলাদা
     *    থ্রেডে, তাই পর্দা কখনো আটকায় না। না পারলে কিছুই জমা হয় না, আর
     *    পরিষ্কার করে বলে দেওয়া হয় — চুপচাপ ফাঁকা ছবি বসে না।
     * ⚠️ একটাই ক্লাউড-তালিকা, তাই ফেরানোটাও **সব ব্রাঞ্চেই** দেখা যাবে
     *    (সরানোটাও আগে থেকে তাই — V573-এর নকশা, বদলানো হয়নি)।
     */
    private fun askRestorePicture(dropped: List<AnatomyModel.PicRow>,
                                  strip: android.widget.LinearLayout, view: AnatomyView) {
        val names = dropped.map { r ->
            val nm = r.label.ifBlank { r.picKey.ifBlank { "ছবি" } }
            if (r.picKey.isBlank()) "📷 " + nm else "🖼 " + nm
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Restore removed photo")
            .setItems(names.map { NoBengali.s(it) }.toTypedArray()) { d, which ->
                d.dismiss()
                doRestorePicture(dropped[which], strip, view)
            }
            .setNegativeButton("← Back", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
    }

    private fun doRestorePicture(row: AnatomyModel.PicRow,
                                 strip: android.widget.LinearLayout, view: AnatomyView) {
        Toast.makeText(this, "Restoring…", Toast.LENGTH_SHORT).show()
        BackgroundWork.run {
            val fixed = try { AnatomyPictureRepository.restoreRow(this, row.id) } catch (_: Throwable) { null }
            if (fixed == null) {
                runOnUiThread {
                    Toast.makeText(this,
                        "Could not restore — check internet and try again", Toast.LENGTH_LONG).show()
                }
            } else {
                AnatomyPictureRepository.saveLocal(this, fixed)
                val ok = AnatomyPictureRepository.pushCloud(fixed)
                runOnUiThread {
                    buildAnatomyStrip(strip, view)
                    Toast.makeText(this, NoBengali.s(
                        if (ok) "ফিরে এসেছে" else "এই ফোনে ফিরল — ক্লাউডে পরে যাবে"),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 🔵🔒 V567 (২২.০৮.২০২৬, TK-নির্দেশ) — রোগের ছবি **সম্পূর্ণ ডিসপ্লেতে**।
     *
     * TK: *"ফটোটা যখন আমি পেসেন্টকে দেখাবো সম্পূর্ণ ডিসপ্লে তে যেন আমি
     * দেখাতে পারি তার ব্যবস্থা রাখবেন"*।
     *
     * ছবিটা গোটা পর্দা জুড়ে খোলে (কালো পটভূমি, কোনো হেডার/ফুটার নেই), যাতে
     * রোগীকে দেখিয়ে বোঝানো যায়। ওখানেও একই সাত রকম কাজ করা যায় — ফোলান ·
     * চিহ্ন · নালী · গোল · তীর · মুছুন · একটা পিছনে · সব মুছুন।
     * 🧰 বোতামে বোতামের সারিটা লুকিয়ে ফেলা যায়, তখন **শুধু ছবিটাই** থাকে।
     *
     * ⛔ নতুন কোনো তথ্য/কলাম/SQL লাগে না। ছোট বোর্ডের অবস্থাটা `save()` দিয়ে
     *    লেখা হয়ে বড় বোর্ডে `load()` হয়, আর বন্ধ করার সময় ঠিক উল্টোটা —
     *    অর্থাৎ **যা আঁকা হল সব ফিরে আসে**, জমা হওয়ার লেখা এক অক্ষরও বদলায় না।
     * ⛔ ছবি না বাছা থাকলে খোলেই না (ফাঁকা কালো পর্দা রোগীকে দেখানোর মানে নেই)।
     */
    private fun openAnatomyFullScreen() {
        val small = anatomyView ?: return
        if (!small.hasPicture()) {
            Toast.makeText(this, NoBengali.s("আগে উপরের সারি থেকে একটা ছবি বাছুন"), Toast.LENGTH_SHORT).show()
            return
        }

        val big = AnatomyView(this)
        /* 🔵 V569 (TK): ছবিটা **গোটা পর্দা ভরে** বসবে, আর দু'আঙুলে ছোট-বড় ও
           সরানো যাবে। দু'বার ছুঁলে আগের মাপে ফিরে যায়। */
        big.fillScreen = true
        big.allowZoom = true
        big.load(small.save()) { key -> anatomyResId(key) }
        big.tool = small.tool
        big.pileLabel = small.pileLabel
        /* 🟢🔒 V626 — পুরো পর্দাতেও একই "ফোলান" জায়গা-পাহারা ও বার্তা,
           ছোট বোর্ডের `view.onBulgeBlocked`-এর হুবহু একই লজিক। */
        big.onBulgeBlocked = {
            val msg = if (big.clockCentre == null)
                "আগে ⊕ কেন্দ্র দিয়ে পায়ুপথের মাঝখানে একবার ছুঁয়ে দিন — তবেই ফোলান কাজ করবে"
            else
                "শুধু মলদ্বারের কাছেই ফোলানো যায় — এখানে নয়"
            Toast.makeText(this, NoBengali.s(msg), Toast.LENGTH_SHORT).show()
        }

        val root = android.widget.FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#08111C"))
        }
        root.addView(big, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT))

        // ── নিচের বোতামের সারি ──
        val bar = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(symDp(8), symDp(8), symDp(8), symDp(5))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = symDp(22).toFloat()
                setColor(android.graphics.Color.parseColor("#B808111C"))
                setStroke(symDp(1), android.graphics.Color.parseColor("#29FFFFFF"))
            }
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM).apply { setMargins(symDp(10), 0, symDp(10), symDp(10)) }
        }

        /* 🔵 V570 — পুরো পর্দাতেও ছোট বোর্ডের **হুবহু একই** সারি (এক ফাংশন,
           তাই দুটোর চেহারা কখনো আলাদা হয়ে যাবে না)। */
        buildToolBar(bar, big, dark = true, full = true)
        root.addView(bar)

        // ── উপরের দুটো ছোট গোল বোতাম: 🧰 বোতাম লুকান · ✕ বন্ধ ──
        val topRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP or android.view.Gravity.END).apply {
                setMargins(0, symDp(10), symDp(10), 0)
            }
        }
        fun roundBtn(label: String): TextView = TextView(this).apply {
            text = label
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                // 🔵 V569 — হালকা ছবির উপরে বোতাম যেন ফ্যাকাশে হয়ে না যায়
                setColor(android.graphics.Color.parseColor("#9E08111C"))
                setStroke(symDp(1), android.graphics.Color.parseColor("#8CFFFFFF"))
            }
            /* প্রজেক্টের বাকি সব জায়গার মতোই `setMargins()` — `leftMargin` ঘরটা
               এই প্রজেক্টে আর কোথাও ব্যবহার হয়নি, তাই কম্পাইল-পাহারা ওটাকে
               চিনতে না পেরে নতুন ভুল বলে ধরছিল। */
            layoutParams = android.widget.LinearLayout.LayoutParams(symDp(42), symDp(42)).apply {
                setMargins(symDp(8), 0, 0, 0)
            }
        }
        val btnOut = roundBtn("➖").apply { setOnClickListener { big.zoomBy(1f / 1.35f) } }
        val btnIn  = roundBtn("➕").apply { setOnClickListener { big.zoomBy(1.35f) } }
        topRow.addView(btnOut); topRow.addView(btnIn)

        val btnBar = roundBtn("🧰").apply {
            setOnClickListener {
                bar.visibility = if (bar.visibility == android.view.View.VISIBLE)
                    android.view.View.GONE else android.view.View.VISIBLE
                big.requestLayout()
            }
        }
        topRow.addView(btnBar)

        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val btnClose = roundBtn("✕").apply { setOnClickListener { dialog.dismiss() } }
        topRow.addView(btnClose)
        root.addView(topRow)

        /* ═══════════════════════════════════════════════════════════════════
           🔵🔒 V587 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) —
           **ক্ষারসূত্রের ধাপ রোগীকে দেখানো।**
           TK-এর বাছাই: পুরো পর্দায় · একটা ফোলা/নালী ছুঁয়ে বেছে · চাপ দিলে
           পরের ধাপ · ইনজেকশনের ধাপ বাদ দেওয়া যায়।
           ⛔ **কিছু সেভ হয় না, প্রিন্টেও যায় না** — শুধু দেখানোর জিনিস।
              মোড চালু থাকলে ছবিতে নতুন দাগও পড়তে পারে না (AnatomyView)।
           ═══════════════════════════════════════════════════════════════════ */
        val ksBox = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            visibility = android.view.View.GONE
            setPadding(symDp(14), symDp(10), symDp(14), symDp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = symDp(22).toFloat()
                setColor(android.graphics.Color.parseColor("#D908111C"))
                setStroke(symDp(1), android.graphics.Color.parseColor("#40FFFFFF"))
            }
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.BOTTOM).apply { setMargins(symDp(10), 0, symDp(10), symDp(10)) }
        }
        val ksCap = TextView(this).apply {
            text = NoBengali.s("যে ফোলা বা নালীতে ক্ষারসূত্র দেখাবেন, সেটা ছুঁয়ে দিন")
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, symDp(9))
        }
        /* ═══ 🔴🔒 V793 (২৮.০৮.২০২৬, TK-নির্দেশ ও ডেমো-প্রুফ অনুমোদনের পরে) ═══
           TK: *"সেই রোগীর চিকিৎসা না করলে সমস্যাটা কত বাড়তে পারে সেটা যেন
           বোঝাতে পারি, এবং চিকিৎসা কিভাবে করি সেটা ধাপে ধাপে বোঝাতে পারি"*
           ⇒ **কী বোঝাবেন** — ⚠️ না সারালে · 🩺 চিকিৎসা।
           ⛔ পুরোনো ধাপগুলো (চিকিৎসা) এক অক্ষরও বদলায়নি; "না সারালে" নতুন। */
        val ksModeRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, symDp(8))
        }
        fun ksChip(label: String): TextView = TextView(this).apply {
            text = NoBengali.s(label); textSize = 13.5f
            gravity = android.view.Gravity.CENTER
            setPadding(symDp(6), symDp(9), symDp(6), symDp(9))
            isClickable = true; isFocusable = true
            layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(symDp(3), 0, symDp(3), 0) }
        }
        val ksChipWorse = ksChip("⚠️ না সারালে")
        val ksChipCure  = ksChip("🩺 চিকিৎসা")
        ksModeRow.addView(ksChipWorse); ksModeRow.addView(ksChipCure)
        ksBox.addView(ksModeRow)

        /* 🔴🔒 V793 — TK: *"কোন কোন পেশেন্টের তো চার সপ্তাহেও ঠিক হয়ে যেতে
           পারে"* ⇒ ফিস্টুলায় কত সপ্তাহে সারবে সেটা ➖ ➕ দিয়ে ডাক্তার বসান;
           সুতোর গোল তত ধাপে ছোট হয়ে আসে। ⛔ অন্য রোগে সারিটা লুকানো থাকে। */
        val ksWeekRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, symDp(8))
            visibility = android.view.View.GONE
        }
        val ksWkMinus = ksChip("➖")
        val ksWkText  = ksChip("সপ্তাহ : 4")
        val ksWkPlus  = ksChip("➕")
        ksWeekRow.addView(ksWkMinus); ksWeekRow.addView(ksWkText); ksWeekRow.addView(ksWkPlus)
        ksBox.addView(ksWeekRow)

        ksBox.addView(ksCap)
        val ksRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        fun ksBtn(label: String, wide: Boolean = false): TextView = TextView(this).apply {
            text = label
            textSize = 14.5f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            setPadding(symDp(14), symDp(9), symDp(14), symDp(9))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = symDp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#1F6D4A"))
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                if (wide) 0 else android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                if (wide) 1f else 0f
            ).apply { setMargins(symDp(4), 0, symDp(4), 0) }
        }
        val ksPrev = ksBtn("◀ আগের")
        val ksNext = ksBtn("পরের ধাপ ▶", wide = true)
        val ksInj  = ksBtn("💉")
        /* 🔵🔒 V898 (TK-অনুমোদিত প্রুফ) — বাক্সটা ছোট করে রাখার বোতাম। ছোট
           অবস্থায় শুধু ধাপের নাম ও "পরের ধাপ" থাকে, তাই ছবি ঢাকা পড়ে না;
           ⌃ চাপলে না সারালে · চিকিৎসা · সপ্তাহ · আগের · 💉 আবার ফিরে আসে। */
        val ksMore = ksBtn("⌃")
        val ksEnd  = ksBtn("✕")
        ksRow.addView(ksPrev); ksRow.addView(ksNext); ksRow.addView(ksInj)
        ksRow.addView(ksMore); ksRow.addView(ksEnd)
        ksBox.addView(ksRow)
        root.addView(ksBox)

        var ksWithInjection = true
        var ksSteps: List<Int> = emptyList()
        var ksAt = 0
        var ksWorse = false                       // 🔴 V793 — "না সারালে" মোড
        /* 🔵 V898 — নিচের বাক্স বড় (সব বোতাম) না ছোট (শুধু ধাপের নাম +
           "পরের ধাপ")। ছোট রাখলে ছবি ঢাকা পড়ে না — TK-অনুমোদিত প্রুফ। */
        var ksFull = false
        var ksKind = ""                           // 🔴 V793 — বাছা চিহ্নের ধরন
        var ksAnim: android.animation.ValueAnimator? = null

        /** 🔴 V793 — কোন মোডটা চালু, চিপের রঙে সেটাই দেখা যায়। */
        fun ksPaintChips() {
            for ((i, ch) in listOf(ksChipWorse, ksChipCure).withIndex()) {
                val on = (i == 0) == ksWorse
                ch.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = symDp(10).toFloat()
                    setColor(android.graphics.Color.parseColor(
                        if (!on) "#2216222E" else if (i == 0) "#8A3A2E" else "#1F6D4A"))
                    setStroke(symDp(1), android.graphics.Color.parseColor(
                        if (!on) "#553B4A5A" else if (i == 0) "#C4564A" else "#2E9366"))
                }
                ch.setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#B9C6D4"))
            }
            for (ch in listOf(ksWkMinus, ksWkText, ksWkPlus)) {
                ch.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = symDp(9).toFloat()
                    setColor(android.graphics.Color.parseColor("#1A4E7A"))
                }
                ch.setTextColor(android.graphics.Color.WHITE)
            }
        }
        fun ksPaint() {
            if (ksSteps.isEmpty()) {
                ksCap.text = NoBengali.s("যে ফোলা বা নালীতে ক্ষারসূত্র দেখাবেন, সেটা ছুঁয়ে দিন")
                ksPrev.visibility = android.view.View.GONE
                ksNext.visibility = android.view.View.GONE
                ksInj.visibility = android.view.View.GONE
                return
            }
            // 🔴 V793 — নতুন ধাপগুলোর লেখা (পুরোনোগুলোর লেখা অবিকল একই থাকে)
            ksCap.text = NoBengali.s(KsharSutraAnim.caption2(ksSteps[ksAt], ksKind, big.ksWeeks))
            ksPrev.visibility = if (ksFull && ksAt > 0) android.view.View.VISIBLE else android.view.View.GONE   // 🔵 V898
            ksNext.visibility = if (ksAt < ksSteps.size - 1) android.view.View.VISIBLE else android.view.View.GONE
            // ইনজেকশনের বোতাম শুধু মাংসের চিকিৎসায়
            val injUse = !ksWorse && (ksKind == AnatomyModel.KIND_BULGE || ksKind == AnatomyModel.KIND_PILE)
            ksInj.visibility = if (ksFull && injUse) android.view.View.VISIBLE else android.view.View.GONE   // 🔵 V898
            ksInj.alpha = if (ksWithInjection) 1f else 0.45f
            // সপ্তাহের সারি শুধু ফিস্টুলার চিকিৎসায়
            ksWeekRow.visibility =
                if (ksFull && !ksWorse && ksKind == AnatomyModel.KIND_TRACT) android.view.View.VISIBLE   // 🔵 V898
                else android.view.View.GONE
            ksModeRow.visibility = if (ksFull) android.view.View.VISIBLE else android.view.View.GONE   // 🔵 V898
            ksWkText.text = NoBengali.s("সপ্তাহ : ${big.ksWeeks}")
            ksPaintChips()
        }
        /** ধাপটা নরম করে চালানো — ০ থেকে ১। */
        fun ksRun(step: Int) {
            ksAnim?.cancel()
            big.ksStep = step
            big.ksT = 0f
            big.invalidate()
            val a = android.animation.ValueAnimator.ofFloat(0f, 1f)
            a.duration = if (step == KsharSutraAnim.LUMP_DRAWN ||
                             step == KsharSutraAnim.TRACT_DRAWN ||
                             step == KsharSutraAnim.FIS_DRAWN ||
                             step == KsharSutraAnim.WORSE_1) 1L else 1100L
            a.addUpdateListener { v -> big.ksT = v.animatedValue as Float; big.invalidate() }
            ksAnim = a
            a.start()
        }
        fun ksGo(i: Int) {
            if (ksSteps.isEmpty()) return
            ksAt = i.coerceIn(0, ksSteps.size - 1)
            ksPaint()
            ksRun(ksSteps[ksAt])
        }
        fun ksSelect(index: Int) {
            val m = AnatomyModel.parse(big.save()).marks.getOrNull(index) ?: return
            ksKind = m.kind                                     // 🔴 V793
            ksSteps = KsharSutraAnim.stepsFor2(m.kind, ksWithInjection, ksWorse, big.ksWeeks)
            if (ksSteps.isEmpty()) {
                Toast.makeText(this@DoctorCheckupActivity,
                    NoBengali.s("এখানে ধাপ দেখানো যায় না — ফোলা · ফাটল · নালী ছুঁয়ে দিন"),
                    Toast.LENGTH_SHORT).show()
                return
            }
            big.ksIndex = index
            /* 🟢🔒 V589 (২৩.০৮.২০২৬, TK-নির্দেশ) — নতুন একটা মাংস বাছলে
               ইনজেকশন ও সুতোর জায়গা **ডিফল্টে** ফেরে — সুতো গোড়ায়
               (TK: *"পাইলসের মাংসের গোড়ায় বাঁধতে হয়"*)। তারপর ডাক্তার
               ছুঁয়ে যেখানে খুশি সরাতে পারেন। */
            big.ksInjAlong = 0.55f
            big.ksInjAcross = 0.10f
            big.ksTieAt = KsharSutraAnim.TIE_AT_BASE
            ksGo(0)
        }
        big.onKsPick = { i -> ksSelect(i) }
        /* 🟢 V589 — ডাক্তার ছুঁয়ে জায়গা দেখালে ওই ধাপটা তখনই আবার চলে,
           তাই সুচ/সুতো নতুন জায়গায় যেতে দেখা যায়। */
        big.onKsSpot = { if (ksSteps.isNotEmpty()) ksRun(ksSteps[ksAt]) }
        /* 🔴🔒 V793 — মোড বদলালে ওই রোগের ওই তালিকাটাই আবার প্রথম থেকে চলে।
           ⛔ ডাক্তারের আঁকা দাগ কিচ্ছু বদলায় না — শুধু কোন ধাপগুলো দেখানো হবে। */
        ksChipWorse.setOnClickListener {
            if (!ksWorse) { ksWorse = true; if (big.ksIndex >= 0) ksSelect(big.ksIndex) else ksPaint() }
        }
        ksChipCure.setOnClickListener {
            if (ksWorse) { ksWorse = false; if (big.ksIndex >= 0) ksSelect(big.ksIndex) else ksPaint() }
        }
        ksWkMinus.setOnClickListener {
            big.ksWeeks = KsharSutraAnim.clampWeeks(big.ksWeeks - 1)
            if (big.ksIndex >= 0) ksSelect(big.ksIndex) else ksPaint()
        }
        ksWkPlus.setOnClickListener {
            big.ksWeeks = KsharSutraAnim.clampWeeks(big.ksWeeks + 1)
            if (big.ksIndex >= 0) ksSelect(big.ksIndex) else ksPaint()
        }
        ksNext.setOnClickListener { ksGo(ksAt + 1) }
        ksPrev.setOnClickListener { ksGo(ksAt - 1) }
        ksInj.setOnClickListener {
            ksWithInjection = !ksWithInjection
            Toast.makeText(this@DoctorCheckupActivity,
                if (ksWithInjection) NoBengali.s("ইনজেকশনের ধাপ থাকবে") else NoBengali.s("ইনজেকশনের ধাপ বাদ"),
                Toast.LENGTH_SHORT).show()
            if (big.ksIndex >= 0) ksSelect(big.ksIndex) else ksPaint()
        }

        fun ksStop() {
            ksAnim?.cancel(); ksAnim = null
            big.ksOn = false; big.ksIndex = -1; big.ksStep = 0; big.ksT = 0f
            ksSteps = emptyList(); ksAt = 0
            ksWorse = false; ksKind = ""          // 🔴 V793
            ksBox.visibility = android.view.View.GONE
            bar.visibility = android.view.View.VISIBLE
            topRow.visibility = android.view.View.VISIBLE   // 🔵 V898
            big.ksDrawAllowed = false                       // 🔵 V898
            big.fillScreen = true                           // 🔵 V898 — আগের চেহারায়
            big.resetZoom()
            big.invalidate()
        }
        ksEnd.setOnClickListener { ksStop() }

        /* ═══ 🔵🔒🔒 V898 (৩১.০৮.২০২৬, TK ডেমো প্রুফ দেখে "হ্যাঁ পাশ") ═══
           TK: *"ফিস্টুলার চিকিৎসা কীভাবে হয় দেখতে পারছি না, ফটো ঢেকে যাচ্ছে"* ·
           *"ফটোটাই মনে হয় বেশি বড় করে ফেলেছেন"* ·
           *"ফটোতে একবার চাপ দিলে যেন শুধু ফটোটাই থাকে … আবার চাপ দিলে
           পরের ধাপ · ইনজেকশন এগুলো ফিরে আসে"* · *"হাইড হলেও যেন ছবি আঁকা যায়"*।

           তিনটে কাজ:
             ১) ধাপ দেখানোর সময় ছবিটা **পর্দা-ভরা** না রেখে **পুরোটা** দেখানো হয়
                (`fillScreen=false`) — তাই নিচের নালী/সুতো আর কাটা যায় না;
             ২) ছবির ফাঁকা জায়গায় **এক টোকা** — উপরের গোল বোতাম ও নিচের বাক্স
                লুকিয়ে যায়, আবার টোকায় ফিরে আসে;
             ৩) লুকানো অবস্থায় আঙুল টানলে **আঁকা যায়** (AnatomyView-এ V898)।
           ⛔ ধাপের হিসাব · অ্যানিমেশন · সেভ — কিচ্ছু ছোঁয়া হয়নি, শুধু দেখানো। */
        var ksHidden = false
        fun ksSetChrome(show: Boolean) {
            ksHidden = !show
            val vis = if (show) android.view.View.VISIBLE else android.view.View.GONE
            topRow.visibility = vis
            ksBox.visibility = if (big.ksOn) vis else android.view.View.GONE
            if (!big.ksOn) bar.visibility = vis
            big.ksDrawAllowed = !show          // লুকানো থাকলে আঁকা যাবে
            big.invalidate()
        }
        big.onKsBlankTap = { ksSetChrome(ksHidden) }

        // 🔵 V898 — ⌃ / ⌄ চাপলে বাক্স বড় বা ছোট।
        ksMore.setOnClickListener {
            ksFull = !ksFull
            ksMore.text = if (ksFull) "⌄" else "⌃"
            ksPaint()
        }
        ksMore.text = "⌃"

        val btnKs = roundBtn("🧵").apply {
            setOnClickListener {
                if (big.ksOn) { ksStop(); return@setOnClickListener }
                if (AnatomyModel.parse(big.save()).marks.none {
                        it.kind == AnatomyModel.KIND_BULGE || it.kind == AnatomyModel.KIND_TRACT ||
                        it.kind == AnatomyModel.KIND_FISSURE }) {          // 🔴 V793 — ফাটলও
                    Toast.makeText(this@DoctorCheckupActivity,
                        NoBengali.s("আগে ছবিতে ফোলা · ফাটল বা নালী আঁকুন — তারপর ধাপ দেখানো যাবে"),
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                big.ksOn = true; big.ksIndex = -1; big.ksStep = 0; big.ksT = 0f
                ksSteps = emptyList(); ksAt = 0
                bar.visibility = android.view.View.GONE
                ksBox.visibility = android.view.View.VISIBLE
                /* 🔵🔒 V898 — TK: *"ফটোটাই বেশি বড় করে ফেলেছেন"*। ধাপ দেখানোর
                   সময় ছবিটা পর্দা-ভরা নয়, **পুরোটা** দেখানো হয় — তাই নিচের
                   নালী ও সুতো আর কাটা পড়ে না। বন্ধ করলে আগের মতোই ফিরে যায়। */
                big.fillScreen = false
                big.resetZoom()
                ksPaint()
                big.invalidate()
            }
        }
        topRow.addView(btnKs, 2)

        /* 🎓🔒 V783 (২৮.০৮.২০২৬, TK-নির্দেশ ও ডেমো-ফটো অনুমোদনের পরে) —
           **রোগীকে বোঝানোর পর্দা: পাইলস · ফিশার · ফিস্টুলা।**
           TK: *"আমি যেন বেছে নিতে পারি — যে রোগীর যে সমস্যা সেই অনুসারে যেন
           বোঝাতে পারি"* · *"এটা লিখে রাখার কিছু নেই"* · *"একই পেশেন্টের তিন
           রকম রোগও তো থাকতে পারে"*।
           ⛔ **কিচ্ছু সেভ হয় না** — ডেটাবেস · প্রিন্ট · A4 কোথাও যায় না।
           ⛔ ডাক্তারের নিজের আঁকা ছবি এক বিন্দুও ছোঁয়া হয় না — এটা সম্পূর্ণ
              আলাদা একটা পর্দা, আলাদা View (`KsharTeachView`)।
           ⛔ পুরনো 🧵 (নিজের আঁকা ছবির উপরে অ্যানিমেশন) আগের মতোই আছে। */
        val btnTeach = roundBtn("🎓").apply { setOnClickListener { openKsharTeach() } }
        topRow.addView(btnTeach, 3)

        /* বন্ধ করার সময় বড় বোর্ডে যা আঁকা হয়েছে সেটাই ছোট বোর্ডে ফেরত যায়।
           ⛔ লেখাটা (`note`) ছোট বোর্ডেরটাই থাকে — বড় পর্দায় লেখার ঘর নেই,
              তাই ওখান থেকে ফাঁকা লেখা এসে আগেরটা মুছে দেওয়ার পথ রাখা হয়নি। */
        dialog.setOnDismissListener {
            val keepNote = AnatomyModel.parse(small.save()).note
            small.load(big.save()) { key -> anatomyResId(key) }
            small.setNote(keepNote)
            small.tool = big.tool
            small.pileLabel = big.pileLabel
            paintAnatomyThumbs(AnatomyModel.parse(small.save()).pic)
            anatomyRepaint?.invoke(small.tool)
        }
        dialog.setContentView(root)
        dialog.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }   // 🤫 V774
    }




    private fun paintAnatomyThumbs(chosen: String) {
        for (img in anatomyThumbs) {
            val on = (img.tag as? String) == chosen
            img.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = symDp(8).toFloat()
                setColor(android.graphics.Color.parseColor("#FFFFFF"))
                setStroke(symDp(if (on) 3 else 1),
                    android.graphics.Color.parseColor(if (on) "#D81E3F" else "#DBE2EA"))
            }
        }
    }


    /**
     * 🔵🔒 V583 (TK-এর উত্তর এসে গেল, ২৩.০৮.২০২৬): *"এখানে তো ঘড়ি মাপের
     * থাকবে — যে কত o'clock-এ তার পাইলস হয়েছে বা ফিস্টুলা হয়েছে?"*
     * ⇒ এতদিন **দুটো তালিকাই** রাখা ছিল (ঘড়ির কাঁটা + সামনে/ডান পাশ…),
     *   কারণ TK-কে জিজ্ঞাসা করে উত্তরের অপেক্ষা করা হচ্ছিল। উত্তর এসেছে,
     *   তাই এখন **শুধু ঘড়ির কাঁটাই** থাকল — ১টা থেকে ১২টা।
     *
     * 🔵 আর TK-এর দ্বিতীয় কথা: *"একবার এই পপ-আপ খুললে এখান থেকে ব্যাক হয় না
     *   কেন?"* — সত্যিই ফেরার কোনো বোতাম ছিল না। এখন **"← ফিরে যান"** বোতাম
     *   আছে, আর বাইরে চাপলেও বন্ধ হয়। ⛔ বন্ধ করলে চিহ্নের নাম **আগের মতোই**
     *   থাকে, কিছু বদলায় না।
     */
    private fun askPileLabel(target: AnatomyView? = null) {
        val view = target ?: anatomyView ?: return
        val all = listOf("নাম ছাড়াই") + (1..12).map { "${it}টা" }
        android.app.AlertDialog.Builder(this)
            .setTitle("Clock position")   /* 🔤 V728 */
            .setItems(all.toTypedArray()) { d, which ->
                view.pileLabel = if (which == 0) "" else all[which]
                d.dismiss()
            }
            .setNegativeButton("← Back") { d, _ -> d.dismiss() }
            .setCancelable(true)
            // 🔴🔒 V610 (২৪.০৮.২০২৬) — V512-এর একই প্রমাণিত পথ; এই dialog-এ
            // .setItems()-এর তালিকাও (নাম ছাড়াই/১টা/২টা…) বাংলা ছিল, একবারেই ঢেকে যায়।
            .show().also { com.tkbiswas.pilesclinic.native.NoBengali.installDialog(it); com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) }   // 🤫 V774
    }

    /** শেষবার যা জমা ছিল — পর্দা তৈরি না থাকলে এটাই ফেরত যায়, নইলে
        সেভ করতে গিয়ে আগের আঁকা ছবিটা মুছে যেত। */
    /** V559: এইমাত্র যে চেকআপটা সেভ হচ্ছে — `markDoctorComplete()` ওটাই
        রোগীর সারিতে বসায়। ⛔ এক রোগীর সেভ শেষ হলেই মুছে ফেলা হয়, যাতে
        পরের রোগীর ঘরে আগেরজনের তথ্য বসার পথ না থাকে (B437-এর শিক্ষা)। */
    @Volatile private var pendingNote: CheckupRecord? = null
    /** 🩺 V839 — সেভের সময় ব্যাকগ্রাউন্ড থ্রেডে পাঠানোর জন্য, মূল থ্রেডেই
     *  ধরে রাখা প্ল্যানটা (View off-thread ছোঁয়া নিরাপদ নয় — V656-এর নিয়ম)। */
    @Volatile private var pendingNvp: NextVisitPlan.Entry? = null

    // 🟢🔒 V656 (২৫.০৮.২০২৬, TK-নির্দেশ — Doctor Note & Reminder) — বাছা
    // তারিখটা (ISO, yyyy-MM-dd) এখানে ধরে রাখা হয়; TextView-তে শুধু দেখানোর
    // লেখা থাকে। ⛔ RegistrationActivity.kt-এর selectedDate-এর একই প্যাটার্ন।
    private var doctorReminderDateIso: String = ""
    // 🟢🔒 V671 (২৫.০৮.২০২৬, TK-নির্দেশ) — সময়ও লাগবে, নইলে নোটিফিকেশন
    // ঠিক সময়ে বাজবে কীভাবে জানা যায় না। ফরম্যাট "HH:mm" (24-ঘণ্টা)।
    private var doctorReminderTimeStr: String = ""
    /* 🩺🔒 V1109 (০৫.০৯.২০২৬, TK-নির্দেশ: *"কোন ডাক্তারকে মনে করিয়ে দিতে হবে
       সেই ডাক্তারের নাম যেন চুস করা যায়, এবং এটা যেন কার্যকরী হয়"*) —
       বাছা ডাক্তারের **মোবাইল নম্বর** এখানে থাকে (নাম নয় — নাম বদলাতে পারে,
       নম্বরই আসল পরিচয়, প্রকল্পের সব জায়গার একই নিয়ম)।
       ⛔ ফাঁকা মানে **সব ডাক্তার** — পুরনো সেভ করা রিমাইন্ডারে ঠিক তাই থাকে,
          তাই আগের কিছুই হারায় না। */
    private var doctorReminderForMobile: String = ""

    /** পর্দা খোলার পর একবারই ফর্ম ভরানো হয়। */
    private var restoredOnce: Boolean = false

    /** JSON → সাধারণ ম্যাপ। তালিকার ঘরগুলো কমা দিয়ে জোড়া লেখা হয়ে যায়। */
    private fun jsonToMap(o: org.json.JSONObject?): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        if (o == null) return out
        val it = o.keys()
        while (it.hasNext()) {
            val k = it.next()
            val v = o.opt(k)
            out[k] = when (v) {
                null, org.json.JSONObject.NULL -> ""
                is org.json.JSONArray -> {
                    val parts = ArrayList<String>()
                    for (i in 0 until v.length()) {
                        val e = v.optString(i, "")
                        if (e.isNotBlank()) parts.add(e)
                    }
                    CheckupNoteJson.joinList(parts)
                }
                is org.json.JSONObject -> ""      // ভিতরে আরেকটা বাক্স — ফোনে পড়ার দরকার নেই
                else -> v.toString()
            }
        }
        return out
    }

    /** ম্যাপ → JSON। ওয়েব যে ঘরগুলো **তালিকা** হিসেবে পড়ে, সেগুলো তালিকা
        হিসেবেই লেখা হয় — নইলে ওয়েবের টিকগুলো ভেঙে যেত। */
    private fun mapToJson(m: Map<String, String>): org.json.JSONObject {
        val o = org.json.JSONObject()
        for ((k, v) in m) {
            if (CheckupNoteJson.LIST_KEYS.contains(k)) {
                val arr = org.json.JSONArray()
                CheckupNoteJson.splitList(v).forEach { arr.put(it) }
                o.put(k, arr)
            } else o.put(k, v)
        }
        return o
    }

    private var lastAnatomySaved: String = ""

    private fun collectAnatomy(): String {
        val view = anatomyView ?: return lastAnatomySaved
        /* 🔵 V572 — লেখার ঘরটা তুলে দেওয়া হয়েছে (TK-নির্দেশ)। তাই এখানে আর
           কিছু বসানো হয় না — বোর্ডে যা জমা ছিল সেটাই থাকে।
           ⛔ ফাঁকা লেখা বসিয়ে দিলে পুরোনো রেকর্ডের লেখা মুছে যেত। */
        return view.save()
    }

    private fun applyAnatomy(saved: String) {
        lastAnatomySaved = saved
        val view = anatomyView ?: return
        view.load(saved) { key -> anatomyResId(key) }
        val pic = AnatomyModel.parse(saved).pic
        /* 🔵 V573 — ছবিটা ডাক্তারের নিজের যোগ করা হলে সেটা resource নয়, তাই
           `load()` ছবিটা বসাতে পারে না — এখানে বসিয়ে দেওয়া হয়।
           ⛔ দাগ মুছে যায় না (`setBaseBitmap` শুধু ছবিটাই বসায়)।
           ⛔ ছবিটা তালিকা থেকে সরানো থাকলেও এখানে দেখা যায় — পুরোনো রেকর্ড
              যেন কখনো ফাঁকা না দেখায়। */
        if (AnatomyModel.isCloudKey(pic)) {
            val id = pic.removePrefix(AnatomyModel.CLOUD_PREFIX)
            val row = AnatomyPictureRepository.cachedRows(this).firstOrNull { it.id == id }
            if (row != null && row.photo.isNotBlank()) view.setBaseBitmap(PhotoUtils.decodeDataUrl(row.photo))
        }
        paintAnatomyThumbs(pic)
    }

    private fun buildLifestyleRows() {
        val box = findViewById<android.widget.LinearLayout>(R.id.lifestyleGroup) ?: return
        box.removeAllViews()
        lifestyleChips.clear()
        for (q in LifestyleModel.QUESTIONS) {
            box.addView(TextView(this).apply {
                text = q.label
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                setPadding(0, symDp(8), 0, symDp(3))
            })
            val chips = ArrayList<TextView>()
            for (rowOptions in HistoryDetailModel.rowsFor(q.options)) {
                val row = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.topMargin = symDp(4)
                    layoutParams = lp
                }
                for ((i, opt) in rowOptions.withIndex()) {
                    val chip = TextView(this).apply {
                        text = opt
                        textSize = 13f
                        gravity = android.view.Gravity.CENTER
                        setPadding(symDp(10), symDp(6), symDp(10), symDp(6))
                        isClickable = true
                        setTag(R.id.historyDetailGroup, false)
                        tag = opt
                        val lp = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        if (i > 0) lp.marginStart = symDp(6)
                        layoutParams = lp
                    }
                    chip.setOnClickListener {
                        if (q.singleSelect) {
                            // 🔴 V600 — শুধু একটাই বাছা থাকবে; একই চিপে আবার
                            // চাপ দিলে বাছাই উঠে যায় (severity-চিপের মতোই)।
                            val wasOn = chip.getTag(R.id.historyDetailGroup) == true
                            for (c in chips) {
                                c.setTag(R.id.historyDetailGroup, false)
                                paintHistoryChip(c)
                            }
                            chip.setTag(R.id.historyDetailGroup, !wasOn)
                        } else {
                            chip.setTag(R.id.historyDetailGroup, chip.getTag(R.id.historyDetailGroup) != true)
                        }
                        paintHistoryChip(chip)
                        refreshLifeFold()   // 🔵 V578 — মাথার সংখ্যা সঙ্গে সঙ্গে
                    }
                    paintHistoryChip(chip)
                    chips.add(chip)
                    row.addView(chip)
                }
                box.addView(row)
            }
            lifestyleChips[q.key] = chips
        }
        refreshLifeFold()
    }

    /** 🔵 V578 — XML-এর মাথাটার সঙ্গে ভাঁজের কাজ জুড়ে দেওয়া (ভাগ ৩-এর মতোই)। */
    private fun wireLifeFold() {
        attachFold(
            "life",
            findViewById<android.widget.LinearLayout>(R.id.lifeFoldHead),
            findViewById<TextView>(R.id.lifeFoldNum),
            findViewById<TextView>(R.id.lifeFoldChev),
            findViewById<android.widget.LinearLayout>(R.id.lifeFoldBody)
        )
        refreshLifeFold()
    }

    /** 🔵 V578 — "রোগ ও অভ্যাস"-এ কতগুলো বাছা হয়েছে। */
    private fun refreshLifeFold() {
        var n = 0
        for ((_, chips) in lifestyleChips) n += chips.count { it.getTag(R.id.historyDetailGroup) == true }
        setFoldCount("life", n)
    }

    private fun collectLifestyle(): String {
        val picked = LinkedHashMap<String, List<String>>()
        for ((key, chips) in lifestyleChips) {
            picked[key] = chips.filter { it.getTag(R.id.historyDetailGroup) == true }
                .map { (it.tag as? String).orEmpty() }
        }
        val water = findViewById<android.widget.EditText>(R.id.etWaterLitre)?.text?.toString().orEmpty()
        val other = findViewById<android.widget.EditText>(R.id.etLifestyleOther)?.text?.toString().orEmpty()
        return LifestyleModel.format(picked, water, other)
    }

    private fun applyLifestyle(saved: String) {
        val (map, water, other) = LifestyleModel.parse(saved)
        for ((key, chips) in lifestyleChips) {
            val chosen = map[key] ?: emptyList<String>()
            chips.forEach { chip ->
                chip.setTag(R.id.historyDetailGroup, chosen.contains((chip.tag as? String).orEmpty()))
                paintHistoryChip(chip)
            }
        }
        findViewById<android.widget.EditText>(R.id.etWaterLitre)?.setText(water)
        findViewById<android.widget.EditText>(R.id.etLifestyleOther)?.setText(other)
        refreshLifeFold()   // 🔵 V578 — পুরোনো রেকর্ড খুললেও সংখ্যা ঠিক থাকে
    }

    private fun collectHistoryDetail(): String {
        val picked = LinkedHashMap<String, List<String>>()
        for ((field, chips) in historyChips) {
            picked[field] = chips.filter { it.getTag(R.id.historyDetailGroup) == true }
                .map { (it.tag as? String).orEmpty() }
        }
        // 🟢🔒 V600 (২৩.০৮.২০২৬, TK-নির্দেশ): "এই ইতিহাস নিয়ে আর কিছু থাকলে
        // লিখুন" বাক্সটা (etHistoryNote) XML থেকে সম্পূর্ণ সরানো হয়েছিল,
        // কিন্তু এই লাইনে R.id.etHistoryNote রয়েই গিয়েছিল — তাই বিল্ড
        // "Unresolved reference" দিয়ে আটকাচ্ছিল (TK ধরিয়ে দিয়েছেন)। বাক্স
        // নেই মানে নতুন নোট নেই — তাই এখন সবসময় খালি স্ট্রিং।
        // ⛔ পুরনো রেকর্ডের নোট এখনো হারায়নি — HistoryDetailModel.parse()
        //    আগের মতোই সেটা পড়ে/জানে, শুধু নতুন করে টাইপ করার বাক্স নেই।
        val note = ""
        return HistoryDetailModel.format(picked, note)
    }

    private fun applyHistoryDetail(saved: String) {
        val (map, note) = HistoryDetailModel.parse(saved)
        for ((field, chips) in historyChips) {
            val chosen = map[field] ?: emptyList<String>()
            chips.forEach { chip ->
                chip.setTag(R.id.historyDetailGroup, chosen.contains((chip.tag as? String).orEmpty()))
                paintHistoryChip(chip)
            }
        }
        // 🟢🔒 V600 — উপরের একই কারণে; `note` পরিবর্তনশীলটা এখন কোথাও
        // দেখানো হয় না (বাক্স নেই), কিন্তু ভেরিয়েবলটা ব্যবহৃত থাকল যাতে
        // destructuring warning না আসে এবং ভবিষ্যতে দরকার হলে সহজেই ফেরানো যায়।
        note.let { }
        refreshHistoryFolds()   // 🔵 V574 — পুরোনো রেকর্ড খুললেও সংখ্যা ঠিক থাকে
    }

    private fun collectSymptomHistory(): String {
        val map = LinkedHashMap<String, SymptomHistoryModel.Entry>()
        for (line in SymptomHistoryModel.LINES) {
            val unitPos = symptomUnits[line.key]?.selectedItemPosition ?: 0
            map[line.key] = SymptomHistoryModel.Entry(
                ticked = symptomTicks[line.key]?.isChecked == true,
                amount = symptomAmounts[line.key]?.text?.toString().orEmpty(),
                unit = SymptomHistoryModel.UNITS.getOrElse(unitPos) { "Days" },
                severity = if (line.severity) currentSeverity(line.key) else ""
            )
        }
        val other = findViewById<android.widget.EditText>(R.id.etSymptomOther)?.text?.toString().orEmpty()
        return SymptomHistoryModel.format(map, other)
    }

    private fun applySymptomHistory(saved: String) {
        val (map, other) = SymptomHistoryModel.parse(saved)
        /* ⛔ আগে কিছু জমা না থাকলে রেজিস্ট্রেশনে যে উপসর্গে টিক দেওয়া ছিল
              সেগুলো **নিজে থেকেই** টিক হয়ে আসে — ডাক্তারকে দুবার একই কাজ
              করতে হয় না। জমা থাকলে জমাটাই আগে (কিছু বদলানো হয় না)। */
        if (saved.isBlank()) {
            val pre = SymptomHistoryModel.ticksFromComplaint(etComplaint.text?.toString().orEmpty())
            pre.forEach { key -> map[key]?.ticked = true }
        }
        for (line in SymptomHistoryModel.LINES) {
            val e = map[line.key] ?: continue
            symptomTicks[line.key]?.isChecked = e.ticked
            symptomAmounts[line.key]?.setText(e.amount)
            val pos = SymptomHistoryModel.UNITS.indexOf(e.unit)
            if (pos >= 0) symptomUnits[line.key]?.setSelection(pos)
            if (line.severity) setSeverity(line.key, e.severity)
        }
        findViewById<android.widget.EditText>(R.id.etSymptomOther)?.setText(other)
        // 🔵 V574 — পুরোনো রেকর্ড খোলার পরেও বাক্স ও সংখ্যা ঠিক অবস্থায় আসে
        for (line in SymptomHistoryModel.LINES) syncSymptomBoxes(line.key)
        refreshSymptomFold()
        refreshHistoryFolds()
    }

    private fun internalPilesBox(): android.widget.CheckBox? =
        visualChecks.firstOrNull { ((it.tag as? String) ?: it.text.toString()).startsWith("Internal Piles") }

    private fun refreshInternalGradeLabel() {
        val box = internalPilesBox() ?: return
        val g = gradeOptions.getOrElse(spGrade.selectedItemPosition) { "" }
        val base = "Internal Piles · অভ্যন্তরীণ অর্শ"
        box.text = if (box.isChecked && g.isNotBlank()) "$base — $g" else base
    }

    private fun askInternalGrade() {
        if (internalPilesBox() == null) return
        /* 🔵🔒 V540 (TK: *"gradation — প্রফেশনাল লুক তৈরি করুন"*): প্রজেক্টের
           নিজের প্রমাণিত পিকার — প্রিমিয়াম হেডার, গোল-বোতামের তালিকা,
           বাছাইটা টিক দেওয়া অবস্থায় দেখা যায়। ⛔ নতুন নকশা বানানো হয়নি। */
        SpinnerPicker.open(spGrade, "INTERNAL PILES — GRADE")
    }

    /**
     * 🔵🔒 V539 (TK-নির্দেশ): *"পেশেন্টের ফটো রোটেট করা যাচ্ছে না"* — এই
     * CHECK-UP পর্দায় Rotate কখনোই ছিল না (TK নিজে নিশ্চিত করেছেন)।
     * এখন উপরের ছবিতে **তিনবার চাপ** দিলে ৯০° ঘুরে যায় ও **সঙ্গে সঙ্গে সেভ**
     * হয় — তাই সব পর্দাতেই সোজা দেখাবে।
     *
     * ⛔ প্রজেক্টের প্রমাণিত জিনিসই ব্যবহার করা হলো — `TripleTapEdit` (ভুল
     *    ছোঁয়ায় যেন না ঘোরে), `PhotoUtils.rotated/encodeBitmap` (V524-এর সেই
     *    একই মাপ ও মান), আর `PatientPhotoRepository.savePhoto` (সেভের সেই
     *    একই পথ, আইডি ধরে)। **নতুন কোনো নিয়ম বানানো হয়নি।**
     * ⛔ ছবি না থাকলে কিছুই হয় না।
     */
    private fun wireCheckupPhotoRotate() {
        val iv = findViewById<ImageView>(R.id.ivPatientPhoto) ?: return
        com.tkbiswas.pilesclinic.native.TripleTapEdit.attach(iv) {
            val digits = patMobile.filter { it.isDigit() }.takeLast(10)
            if (digits.length != 10) {
                Toast.makeText(this, "No valid 10-digit mobile", Toast.LENGTH_SHORT).show()
                return@attach
            }
            val bmp = com.tkbiswas.pilesclinic.native.PhotoUtils.decodeDataUrl(patPhoto)
            if (bmp == null) {
                Toast.makeText(this, "No photo to rotate", Toast.LENGTH_SHORT).show()
                return@attach
            }
            val turned = com.tkbiswas.pilesclinic.native.PhotoUtils.rotated(bmp, 90)
            val dataUrl = com.tkbiswas.pilesclinic.native.PhotoUtils.encodeBitmap(turned)
            if (dataUrl == null) {
                Toast.makeText(this, "Could not rotate", Toast.LENGTH_SHORT).show()
                return@attach
            }
            iv.setImageBitmap(turned)
            patPhoto = dataUrl
            Toast.makeText(this, "Rotated — saving…", Toast.LENGTH_SHORT).show()
            val appCtx = applicationContext
            com.tkbiswas.pilesclinic.native.BackgroundWork.run {
                val repo = com.tkbiswas.pilesclinic.native.PatientPhotoRepository()
                val ref = repo.findByMobile(digits, RoleSession.currentPatientId, RoleSession.currentPatientDisplayId)
                if (ref != null) repo.savePhoto(ref, dataUrl, appCtx)
            }
        }
    }

    private fun buildDetails(r: CheckupRecord): String = buildString {
        if (r.complaint.isNotBlank()) append("Complaint: ${r.complaint}; ")
        if (r.duration.isNotBlank()) append("Duration: ${r.duration}; ")
        if (r.acuteChronic.isNotBlank()) append("Onset: ${r.acuteChronic}; ")
        if (r.occupation.isNotBlank()) append("Occupation: ${r.occupation}; ")
        if (r.prevTreatment.isNotBlank()) append("Prev Treatment: ${r.prevTreatment}; ")
        if (r.patientSaid.isNotBlank()) append("Patient Said: ${r.patientSaid}; ")   // 🔵 V539
        // 🔵 V554: টিক দেওয়া উপসর্গ ও তাদের "কবে থেকে?" — মানুষ-পড়া-যায় লেখায়
        if (r.symptomHistory.isNotBlank()) append("Patient Reported: ${SymptomHistoryModel.readable(r.symptomHistory)}; ")
        // 🔵 V555: কাগজের ভাগ ৩ — চারটে ইতিহাসের বাছাই
        if (r.historyDetail.isNotBlank()) append("History Detail: ${HistoryDetailModel.readable(r.historyDetail)}; ")
        // 🔵 V556: রোগ ও অভ্যাস
        if (r.lifestyle.isNotBlank()) append("Habits: ${LifestyleModel.readable(r.lifestyle)}; ")
        // 🔵 V557
        if (r.probableDisease.isNotBlank() && r.probableDisease != CounselModel.PICK_NONE)
            append("Probable Disease: ${r.probableDisease}; ")
        if (r.timeAsked.isNotBlank()) append("Time Asked: ${r.timeAsked}; ")
        // 🔵 V558: ছবিতে যা দেখানো হয়েছে — কিছু না আঁকলে কিছুই লেখা হয় না
        if (AnatomyModel.readable(r.anatomy).isNotBlank())
            append("Disease Picture: ${AnatomyModel.readable(r.anatomy)}; ")
        if (r.prevResult.isNotBlank()) append("Prev Result: ${r.prevResult}; ")
        if (r.prevCost.isNotBlank()) append("Prev Cost: ${r.prevCost}; ")
        if (r.treatmentDuration.isNotBlank()) append("Treatment Duration: ${r.treatmentDuration}; ")
        val visAll = listOf(r.visual, r.visualOther).filter { it.isNotBlank() }.joinToString(", ")
        if (visAll.isNotBlank()) append("Visual: $visAll; ")
        val dreAll = listOf(r.dre, r.dreOther).filter { it.isNotBlank() }.joinToString(", ")
        if (dreAll.isNotBlank()) append("DRE: $dreAll; ")
        if (r.grade.isNotBlank()) append("Internal Piles Grade: ${r.grade}; ")   // 🔵 V539
        if (r.proctoscopy.isNotBlank()) append("Proctoscopy: ${r.proctoscopy}; ")   // 🔵 V539
        if (r.onProbing.isNotBlank()) append("On Probing: ${r.onProbing}; ")
        if (r.investigation.isNotBlank()) append("Investigation: ${r.investigation}; ")
        if (r.otherFindings.isNotBlank()) append("Other Findings: ${r.otherFindings}; ")
        // 🆕 TK-নির্দেশ (04.08.2026): Treatment Plan + টাকা — শুধু যেগুলো
        // সত্যিই টিক দেওয়া, সেগুলোরই টাকা দেখানো হয় (ফাঁকা/না-টিক করা
        // অপশনের টাকা প্রিন্ট/হিস্ট্রিতে বিভ্রান্তি তৈরি করত না, তাও
        // সাবধানতার জন্য)।
        if (r.treatmentPlan.isNotBlank()) {
            val amtParts = mutableListOf<String>()
            if (cbTxPerPiles.isChecked && r.amtPerPiles.isNotBlank()) amtParts.add("Per Piles ₹${r.amtPerPiles}")
            if (cbTxFistulaInch.isChecked && r.amtFistulaPerInch.isNotBlank()) amtParts.add("Fistula Per CM ₹${r.amtFistulaPerInch}")   // 🔵 V541
            if (cbTxKsharSutra.isChecked && r.amtKsharSutra.isNotBlank()) amtParts.add("Kshar Sutra ₹${r.amtKsharSutra}")
            val amtText = if (amtParts.isNotEmpty()) " (${amtParts.joinToString(", ")})" else ""
            append("Treatment Plan: ${r.treatmentPlan}$amtText; ")
        }
        if (r.counselling.isNotBlank()) append("Other Treatment Note: ${r.counselling}; ")
        if (r.estimatedCost.isNotBlank()) append("Est Cost: ${r.estimatedCost}; ")
        if (r.recoveryTime.isNotBlank()) append("Recovery: ${r.recoveryTime}; ")
        if (r.advanceDiscussed.isNotBlank()) append("Advance: ${r.advanceDiscussed}; ")
        if (r.patientDecision.isNotBlank()) append("Decision: ${r.patientDecision}; ")
        if (r.decisionRemark.isNotBlank()) append("Remarks: ${r.decisionRemark}; ")
        if (r.documents.isNotBlank()) append("Documents: ${r.documents}")
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        listOf(
            // 🔵 B622: etPrevResult · etPrevCost · etTreatmentDuration · etAdvanceDiscussed ঘর বাদ।
            // V455 (18.08.2026): etVisualOther · etDreOther · etOtherFindings · etDecisionRemark · etDocuments ঘর বাদ।
            etComplaint, etDuration, etPrevTreatment,
            etOnProbing,
            etAmtPerPiles, etAmtFistulaInch, etAmtKsharSutra,
            // 🟢 V589: etRecoveryTime ঘর বাদ।
            etCounselling, etEstimatedCost
        ).forEach { it.isEnabled = enabled }
        (visualChecks + investigationChecks + treatmentChecks()).forEach { it.isEnabled = enabled }
        spGrade.isEnabled = enabled
        // V455 (18.08.2026): spPatientDecision · spAcuteChronic isEnabled বাদ (ঘর নেই)।
        spOccupation.isEnabled = enabled
    }

    // ─────────────────────────────────────────────────────────────────────
    // 🔴 NEW (07.08.2026, scroll_B/scroll_C মকআপ) — সেভ করলে "লক"।
    // এটা সম্পূর্ণ দেখার (UI) স্তর — উপরের collect()/saveMedical()/
    // markDoctorComplete()/ছবি — কোনো ডেটা-লজিক ছোঁয়া হয়নি।
    //   • History / Clinical / Counsel → সবুজ-টিক read-only summary।
    //   • Estimate & Decision → লুকানো (TK: সেভের পর আর দেখানোর দরকার নেই)।
    //   • Photo → ছবি থাকে, ক্যামেরা-বোতাম ও Quick Actions লুকানো।
    //   • নিচে Back/Save লুকিয়ে Share/Print।
    //   • কোনো সেকশনে তিনবার চাপলে (বা ✎) আবার এডিট করা যায়, Save করলে
    //     আবার লক হয়।
    // ─────────────────────────────────────────────────────────────────────
    private fun enterLockedMode(r: CheckupRecord) {
        // 🆕🔒 (07.08.2026, TK-অনুমোদিত) — সেভ হওয়ার পর সবুজ-summary-র বদলে
        // এখন পুরো A4 চেকআপ-রিপোর্ট (WebView) দেখাই, নিচে ✎ Edit · 🔗 Share ·
        // 🖨 Print। Edit চাপলে ফর্মে ফেরে (তথ্য অক্ষত), Save করলে আবার A4।
        // ⛔ উপরের সেভ/ক্লাউড-লজিক অপরিবর্তিত — এটা শুধু "দেখা"র রূপ।
        lastSavedRecord = r
        showSavedA4(buildA4Html(r))
    }

    // ⛔ পুরনো green-check summary lock (A4 রিপোর্ট আসার আগে ব্যবহৃত) — এখন আর
    // ডাকা হয় না, শুধু রেফারেন্স/দরকারে-ফেরানোর জন্য রইল।
    private fun enterLockedModeOld(r: CheckupRecord) {
        lockSection(R.id.secHistory, buildSectionSummary("History & Previous", listOf(
            "প্রধান সমস্যা" to r.complaint,
            "কতদিন থেকে" to r.duration,
            "পেশা" to r.occupation,
            "আগের চিকিৎসা" to r.prevTreatment,
            "Onset" to r.acuteChronic
        )) { unlockSection(R.id.secHistory) })

        val visAll = listOf(r.visual, r.visualOther).filter { it.isNotBlank() }.joinToString(", ")
        val dreAll = listOf(r.dre, r.dreOther).filter { it.isNotBlank() }.joinToString(", ")
        /* 🔴🔒 V542 (২২.০৮.২০২৬, TK-নির্দেশ): "E. Investigations" পর্দা থেকে
           সরানো হয়েছে, তাই নতুন চেকআপে ঘরটা ফাঁকা — ফাঁকা সারি দেখানোর
           মানে হয় না। ⛔ **পুরোনো চেকআপে লেখা থাকলে সারিটা আগের মতোই
           দেখাবে** (তখন কোন পরীক্ষা বলা হয়েছিল সেটা হারায় না)।
           ⛔ Grade-এর লেবেলটাও এখন "Internal Piles Grade" (V539), আর
              Proctoscopy-র লেখাটা আলাদা সারিতে। */
        lockSection(R.id.secClinical, buildSectionSummary("Clinical পরীক্ষা", listOf(
            "Visual" to visAll,
            "DRE" to dreAll,
            "Internal Piles Grade" to r.grade,
            "Proctoscopy" to r.proctoscopy,
            "On Probing" to r.onProbing,
            "Investigations" to r.investigation
        ).filter { it.second.isNotBlank() }) { unlockSection(R.id.secClinical) })

        /* 🧮 V1280 — সারাংশে এখন এস্টিমেট থেকে আসা তিনটে জিনিস + চুক্তি-নোট।
           🎨🔒 V1493 (১৫.০৯.২০২৬, TK-নির্দেশ) — শিরোনাম XML হেডারের সাথে মিলিয়ে
           "চিকিৎসা পরিকল্পনা" থেকে "আনুমানিক খরচ" করা হলো (সঠিক অনুবাদ)। */
        lockSection(R.id.secCounsel, buildSectionSummary("Estimated Cost · আনুমানিক খরচ", listOf(
            "রোগ" to (if (r.probableDisease == CounselModel.PICK_NONE) "" else r.probableDisease),
            "চিকিৎসা" to r.treatmentPlan,
            "Net Payable" to r.estimatedCost,
            "Time Asked" to r.timeAsked,
            "চুক্তি" to r.counselling
        ).filter { it.second.isNotBlank() }) { unlockSection(R.id.secCounsel) })

        // Estimate & Decision — সেভের পর লুকানো।
        findViewById<android.view.View>(R.id.secEstimate).visibility = android.view.View.GONE
        /* 🧮🔒 V995 — "🧮 Build" আইকনটা এখন হেডারে, Counsel-এর ভিতরে নয়। আগে
           সেভের পর Counsel সারাংশে বদলে যেত বলে বোতামটা নিজে থেকেই হারিয়ে
           যেত; সেই আচরণ হুবহু রাখতে এখানে আইকনটাও লুকানো হয়। */
        findViewById<android.view.View>(R.id.btnBuildEstimate)?.visibility = android.view.View.GONE
        /* 🧮 V1105 — ঘরের পাশের নতুন আইকনটাও ঠিক একই সময়ে লুকায়, নইলে সেভের
           পরেও একটা বোতাম থেকে যেত (হেডারেরটা লুকিয়ে যেত, এটা নয়)। */
        findViewById<android.view.View>(R.id.btnBuildEstimateInline)?.visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.btnBuildEstimateBig)?.visibility = android.view.View.GONE   // 🧮 V1280

        // Photo — ছবি থাকে, শুধু ক্যামেরা-বোতাম + Quick Actions লুকানো।
        findViewById<android.view.View>(R.id.btnBeforePhoto).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.btnDuringPhoto).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.btnAfterPhoto).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.quickActionsLabel).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.quickActionsRow1).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.quickActionsRow2).visibility = android.view.View.GONE
        // V455 (18.08.2026): etDocuments.isEnabled=false বাদ (ঘরটাই আর নেই)।

        lockedFooter(true)
        // 🎨 (07.08.2026, প্রুফ-চেহারা) — সব ধাপ সম্পন্ন: প্রতিটা নম্বর-বৃত্ত এখন
        // সবুজ ✓। (আগে: chip.text = "✓ " + stepTitles[idx].substringAfter(" "))
        stepChips.forEachIndexed { idx, circle ->
            circle.background = stepDotBg(done = true, current = false)   // 🎨 V897
            stepLineLeft.getOrNull(idx)?.setBackgroundColor(android.graphics.Color.parseColor("#12805C"))
            stepLineRight.getOrNull(idx)?.setBackgroundColor(android.graphics.Color.parseColor("#12805C"))
            circle.text = "✓"
            circle.setTextColor(android.graphics.Color.WHITE)
            circle.setTypeface(null, android.graphics.Typeface.BOLD)
            stepChipLabels.getOrNull(idx)?.setTextColor(android.graphics.Color.parseColor("#0F766E"))
        }
        findViewById<ScrollView>(R.id.stepScroll).post {
            findViewById<ScrollView>(R.id.stepScroll).smoothScrollTo(0, 0)
        }
    }

    private fun lockSection(cardId: Int, summary: android.view.View) {
        val card = findViewById<android.view.ViewGroup>(cardId)
        if (card.childCount > 1) card.removeViewAt(1)   // re-lock: drop old summary
        card.getChildAt(0).visibility = android.view.View.GONE
        card.addView(
            summary,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun unlockSection(cardId: Int) {
        val card = findViewById<android.view.ViewGroup>(cardId)
        if (card.childCount > 1) card.removeViewAt(1)
        card.getChildAt(0).visibility = android.view.View.VISIBLE
        lockedFooter(false)  // show Back|Save so a re-edit can be re-saved
        openSavedGroupIfHolding(card)   // 🟢 V1119
        findViewById<ScrollView>(R.id.stepScroll).post {
            findViewById<ScrollView>(R.id.stepScroll).smoothScrollTo(0, yInScroll(card))   // 🟢 V1119
        }
    }

    private fun lockedFooter(lock: Boolean) {
        fun vis(b: Boolean) = if (b) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<MaterialButton>(R.id.btnBack).visibility = vis(!lock)
        findViewById<MaterialButton>(R.id.btnSaveCheckup).visibility = vis(!lock)
        // 🔴 V501 — নতুন Share বোতামও Save-এর সঙ্গেই দেখা যায়/লুকায়।
        findViewById<MaterialButton>(R.id.btnShareNow).visibility = vis(!lock)
        findViewById<MaterialButton>(R.id.btnShareCheckup).visibility = vis(lock)
        findViewById<MaterialButton>(R.id.btnPrintCheckup).visibility = vis(lock)
    }

    // ─────────────────────────────────────────────────────────────────────
    // 🆕🔒 (07.08.2026, TK-অনুমোদিত) — সেভ হওয়ার পর A4 চেকআপ-রিপোর্ট (WebView)।
    //   • showSavedA4(): ফর্ম লুকিয়ে A4 রিপোর্ট দেখায়, নিচে ✎ Edit · 🔗 Share · 🖨 Print।
    //   • showEditForm(): আবার ফর্ম ফেরে (তথ্য অক্ষত), নিচে ◀ Back · 💾 Save।
    //   • buildA4Html(): সেভ-করা রেকর্ড থেকে ক্লিনিক-হেডারসহ A4 HTML বানায়।
    // ⛔ কোনো ফিল্ড/সেভ-লজিক ছোঁয়া হয়নি — শুধু "দেখা"র রূপ।
    // ─────────────────────────────────────────────────────────────────────
    /* 📝 V1276 — কাগজে ডাক্তারের মন্তব্য থাকবে কিনা। **ডিফল্ট চালু** —
       বন্ধ থাকলে TK-র অভিযোগটাই ("লেখাটা কোথাও নেই") ফিরে আসত। */
    private var a4ShowDocRemark = true
    /* 📝 V1280 — চুক্তি/পরিকল্পনার নোট কাগজে যাবে কিনা। **ডিফল্ট বন্ধ** (TK)। */
    private var a4ShowAgreement = false

    private fun showSavedA4(html: String) {
        val wv = findViewById<android.webkit.WebView>(R.id.savedA4View)
        wv.settings.javaScriptEnabled = false
        wv.settings.loadWithOverviewMode = true
        wv.settings.useWideViewPort = true
        wv.settings.builtInZoomControls = true
        wv.settings.displayZoomControls = false
        wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        findViewById<android.view.View>(R.id.stepBarWrap).visibility = android.view.View.GONE   // 🎨 V897
        findViewById<android.view.View>(R.id.stepScroll).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.patientDetailsFull).visibility = android.view.View.GONE
        wv.visibility = android.view.View.VISIBLE
        fun vis(b: Boolean) = if (b) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<MaterialButton>(R.id.btnBack).visibility = vis(false)
        findViewById<MaterialButton>(R.id.btnSaveCheckup).visibility = vis(false)
        findViewById<MaterialButton>(R.id.btnShareNow).visibility = vis(false)
        findViewById<MaterialButton>(R.id.btnNext).visibility = vis(false)
        findViewById<MaterialButton>(R.id.btnEditCheckup).visibility = vis(true)
        findViewById<MaterialButton>(R.id.btnShareCheckup).visibility = vis(true)
        findViewById<MaterialButton>(R.id.btnPrintCheckup).visibility = vis(true)
        /* 📝 V1276 — সুইচটা **শুধু তখনই** দেখা যায় যখন ডাক্তার সত্যিই কিছু
           লিখেছেন; নইলে কাগজে ওই ভাগটাই বসে না, তাই সুইচেরও মানে নেই। */
        findViewById<android.view.View>(R.id.drRemarkBar).visibility =
            vis(!(lastSavedRecord?.doctorRemark ?: "").isBlank())
        findViewById<android.view.View>(R.id.agreeBar).visibility =
            vis(!(lastSavedRecord?.counselling ?: "").isBlank())   // 📝 V1280
    }

    private fun showEditForm() {
        findViewById<android.view.View>(R.id.savedA4View).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.stepBarWrap).visibility = android.view.View.VISIBLE   // 🎨 V897
        findViewById<android.view.View>(R.id.stepScroll).visibility = android.view.View.VISIBLE
        findViewById<android.view.View>(R.id.patientDetailsFull).visibility = android.view.View.VISIBLE
        fun vis(b: Boolean) = if (b) android.view.View.VISIBLE else android.view.View.GONE
        findViewById<MaterialButton>(R.id.btnBack).visibility = vis(true)
        findViewById<MaterialButton>(R.id.btnSaveCheckup).visibility = vis(true)
        findViewById<MaterialButton>(R.id.btnShareNow).visibility = vis(true)   // 🔴 V501
        findViewById<MaterialButton>(R.id.btnNext).visibility = vis(false)
        findViewById<MaterialButton>(R.id.btnEditCheckup).visibility = vis(false)
        findViewById<MaterialButton>(R.id.btnShareCheckup).visibility = vis(false)
        findViewById<MaterialButton>(R.id.btnPrintCheckup).visibility = vis(false)
        findViewById<android.view.View>(R.id.drRemarkBar).visibility = vis(false)   // 📝 V1276
        findViewById<android.view.View>(R.id.agreeBar).visibility = vis(false)   // 📝 V1280
        showStep(currentStep)
    }

    /* 🔵 V584 (২৩.০৮.২০২৬, TK-অনুমোদিত): `lang` যোগ হলো — ডিফল্ট English,
       তাই আগের সব ডাক (সেভের পরের পর্দা · 🖨 Print · 📤 Share) হুবহু আগের
       মতোই ইংরেজি রিপোর্ট পায়। ভাষা বাছার সুযোগটা নতুন Check-up History
       পপ-আপে (`openCheckupHistory()`)। */
    private fun buildA4Html(r: CheckupRecord, lang: String = CheckupA4Lang.EN): String {
        // 🆕 (07.08.2026) — A4 রিপোর্টের চেহারা এখন একটাই জায়গায়:
        // `CheckupA4Report` (History-তেও ঠিক এই টেমপ্লেটই ব্যবহার হয়, তাই
        // দুই জায়গার রিপোর্ট কখনো আলাদা দেখাবে না)।
        val visAll = listOf(r.visual, r.visualOther).filter { it.isNotBlank() }.joinToString(", ")
        val dreAll = listOf(r.dre, r.dreOther).filter { it.isNotBlank() }.joinToString(", ")
        return CheckupA4Report.html(
            CheckupA4Report.Info(
                name = RoleSession.currentPatientName,
                patientId = RoleSession.displayId(),
                age = patAge, sex = patSex, mobile = patMobile,
                disease = patDisease.ifBlank { "Piles" },
                address = patAddress,
                branch = RoleSession.currentPatientBranch,
                date = CheckupA4Report.today(),
                photo = patPhoto
            ),
            CheckupA4Report.Fields(
                complaint = r.complaint, duration = r.duration, onset = r.acuteChronic,
                occupation = r.occupation, prevTreatment = r.prevTreatment,
                prevResult = r.prevResult, prevCost = r.prevCost,
                treatmentDuration = r.treatmentDuration,
                visual = visAll, dre = dreAll, grade = r.grade,
                onProbing = r.onProbing, investigation = r.investigation,
                otherFindings = r.otherFindings,
                treatmentPlan = r.treatmentPlan, rate = buildRateSummary(r),
                /* 📝🔒 V1280 — চুক্তি/পরিকল্পনার নোট: TK — *"শুধু ফোনে থাকবে, চেকআপ
                   কাগজেও ছাপবে না, তবে অপশন থাকবে আলাদা"* ⇒ সুইচ চালু থাকলে তবেই। */
                counselling = if (a4ShowAgreement) r.counselling else "",
                estCost = r.estimatedCost,
                /* 🟢🔒 V589 (২৩.০৮.২০২৬, TK-নির্দেশ) — *"অ্যাপের মধ্যে এক জায়গায়
                   তো আমি বেছে নিচ্ছি সেটাই ছেপে যাবে"*।
                   কাগজের এই ঘরটা আগে ভাগ ৪-এর হাতে-লেখা বক্স থেকে আসত; ওই বক্সটা
                   এখন নেই, তাই **ভাগ ৩-এর "কতদিন সময় চাওয়া হল?"** (যেমন `15 Days`)
                   ছাপা হয় — যেটা ডাক্তার সত্যিই বেছেছেন।
                   ⛔ পুরনো রোগীর রেকর্ডে ভাগ ৩ ফাঁকা কিন্তু ভাগ ৪-এ লেখা ছিল —
                      তখন আগের সেই লেখাটাই ছাপে, কাগজ ফাঁকা যায় না। */
                recovery = r.timeAsked.ifBlank { r.recoveryTime },
                advance = r.advanceDiscussed, decision = r.patientDecision,
                remarks = r.decisionRemark,
                beforePhoto = r.beforePhoto, duringPhoto = r.duringPhoto, afterPhoto = r.afterPhoto,
                // 🔵 V584 — কাগজের ভাগ ২ · ৩ · ৪ ও রোগের ছবি (ভাগ ৬)। এগুলো
                // আগে থেকেই রেকর্ডে সেভ হচ্ছিল, শুধু প্রিন্টে যেত না।
                patientSaid = r.patientSaid,
                symptomHistory = r.symptomHistory,
                historyDetail = r.historyDetail,
                lifestyle = r.lifestyle,
                anatomy = r.anatomy,
                anatomyImage = CheckupAnatomyImage.dataUrl(this, r.anatomy),
                probableDisease = if (r.probableDisease == CounselModel.PICK_NONE) "" else r.probableDisease,
                /* 📝🔒 V1276 — ডাক্তারের মন্তব্য এতদিন কাগজে **যেতই না**
                   (`Fields`-এ ঘরটাই ছিল না)। এখন যায় — তবে সুইচ বন্ধ থাকলে
                   ফাঁকা পাঠানো হয়, আর ফাঁকা হলে কাগজে ভাগটাই বসে না।
                   ⛔ পর্দা · Print · Share তিনটেই এই একই লাইন দিয়ে যায়। */
                doctorRemark = if (a4ShowDocRemark) r.doctorRemark else ""
            ),
            lang
        )
    }

    // 🔒 B550 (08.08.2026, TK-অনুমোদিত প্রুফ) — Rate-এ এখন শুধু যে চিকিৎসা
    // **টিক করা হয়েছে** তার দাম দেখাবে। আগে দাম-ঘরে ডিফল্ট মান সবসময় বসানো
    // থাকায় (৮০০০/১১০০০/৬০০০) তিনটে দামই আসত — যদিও একটাই টিক ছিল (TK ধরেছেন)।
    // এখন `treatmentPlan`-এ ওই চিকিৎসার নাম আছে কিনা মিলিয়ে তবেই দাম যোগ হয়।
    // ⛔ সেভ-হওয়া লেখা (buildDetails) আগে থেকেই টিক দেখে ঠিক ছিল — এটা শুধু
    // লাইভ রেকর্ডটাকে তার সাথে মিলিয়ে দিল।
    private fun buildRateSummary(r: CheckupRecord): String {
        val plan = r.treatmentPlan
        val parts = mutableListOf<String>()
        if (plan.contains("Per Piles") && r.amtPerPiles.isNotBlank()) parts.add("₹${r.amtPerPiles} / অর্শ")
        // 🔵 V541: পুরোনো "Inch" লেখা রেকর্ডও ধরা পড়ে, নইলে পুরোনো চেকআপে হার দেখাত না।
        if (FISTULA_TAGS.any { plan.contains(it) } && r.amtFistulaPerInch.isNotBlank()) parts.add("₹${r.amtFistulaPerInch} / সেমি")
        if (plan.contains("Kshar Sutra") && r.amtKsharSutra.isNotBlank()) parts.add("₹${r.amtKsharSutra} / ক্ষার সূত্র")
        return parts.joinToString(" · ")
    }

    private fun buildSectionSummary(
        title: String, rows: List<Pair<String, String>>, onEdit: () -> Unit
    ): android.view.View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, dp(2))
        }
        outer.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(4), LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#12805C"))
        })
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "✓"; setTextColor(android.graphics.Color.WHITE); textSize = 12f
            gravity = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#12805C"))
            }
            val lp = LinearLayout.LayoutParams(dp(22), dp(22)); lp.marginEnd = dp(8); layoutParams = lp
        })
        header.addView(TextView(this).apply {
            text = title; setTextColor(android.graphics.Color.parseColor("#101828")); textSize = 14.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "✎"; setTextColor(android.graphics.Color.parseColor("#0B5E8A")); textSize = 15f
            setPadding(dp(10), dp(2), dp(4), dp(2))
            isClickable = true; isFocusable = true
            setOnClickListener { onEdit() }
        })
        content.addView(header)
        rows.forEach { (label, value) ->
            val rowLl = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(7), 0, dp(7))
            }
            rowLl.addView(TextView(this).apply {
                text = label; setTextColor(android.graphics.Color.parseColor("#5A6B76")); textSize = 12.5f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.42f)
            })
            rowLl.addView(TextView(this).apply {
                text = value.ifBlank { "—" }
                setTextColor(android.graphics.Color.parseColor("#101828")); textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.58f)
            })
            content.addView(rowLl)
            content.addView(android.view.View(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#EEF1F5"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            })
        }
        // তিনবার চাপলে এডিট।
        var taps = 0; var last = 0L
        content.setOnClickListener {
            val now = android.os.SystemClock.elapsedRealtime()
            taps = if (now - last < 600) taps + 1 else 1
            last = now
            if (taps >= 3) { taps = 0; onEdit() }
        }
        outer.addView(content)
        return outer
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /**
     * 🔴🔒 V501 (TK-নির্দেশ ২১.০৮.২০২৬, ফটো-প্রুফ অনুমোদিত)
     *
     * TK-এর কথা: *"সেখানে চাপ দিলে অটোমেটিক জোর করে WhatsApp / business
     * WhatsApp ওপেন হবে… A4 size pdf চাইলে প্রিন্টও করা যাবে।"*
     *
     * ─── আগে যা হতো (এবং যা TK চাননি) ───────────────────────────────────
     * শুধু **সাদামাটা লেখা** (`text/plain`) Android-এর সাধারণ chooser-এ যেত —
     * ফোনের সব অ্যাপের লম্বা তালিকা আসত, কোনো A4 PDF যেত না, ছাপাও যেত না।
     *
     * ─── এখন ────────────────────────────────────────────────────────────
     * পর্দায় দেখা **হুবহু সেই A4 রিপোর্টই** (`buildA4Html`) PDF হয়ে যায়,
     * আর শুধু তিনটে পথ দেখানো হয়: WhatsApp · WhatsApp Business · Print/PDF।
     *
     * ⛔ নতুন কোনো ব্যবস্থা বানানো হয়নি — প্রকল্পের প্রমাণিত
     *    `PrescriptionWhatsAppShare` (Prescription-এ চলছে) ও
     *    `PdfPrintDocumentAdapter` (Print-এ চলছে) পুনর্ব্যবহার করা হলো।
     */
    private fun shareCheckup() {
        val r = lastSavedRecord ?: collect()
        try {
            com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
                activity = this,
                html = buildA4Html(r),
                documentTitle = "Check-up",
                patientName = RoleSession.currentPatientName,
                allowPrint = true
            )
        } catch (e: Throwable) {
            Toast.makeText(this, "Share not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 🔴 V501 — নিচের নতুন "📤 Share" বোতাম (Back ও Save-এর মাঝে)।
     * TK-এর সিদ্ধান্ত: **আগে নিজে থেকে সেভ হবে, তারপর শেয়ার** — যাতে কখনো
     * পুরোনো বা অসম্পূর্ণ রিপোর্ট রোগীর কাছে চলে না যায়।
     */
    private fun saveThenShare() {
        try {
            findViewById<MaterialButton>(R.id.btnSaveCheckup).performClick()
        } catch (_: Throwable) { }
        // সেভের কাজ শেষ হতে একটু সময় দিয়ে তবেই শেয়ার (সেভ ব্যর্থ হলেও
        // পর্দার নিজের বার্তা দেখা যাবে; শেয়ারে তখন বর্তমান ফর্মই যাবে)।
        findViewById<MaterialButton>(R.id.btnShareNow).postDelayed({ shareCheckup() }, 700L)
    }

    // ─────────────────────────────────────────────────────────────────────
    // 🔵🔒 V584 (২৩.০৮.২০২৬, TK-অনুমোদিত ডেমো-প্রুফের পরে) — হেডারের 📜
    // বোতামের নতুন কাজ: **Check-up History**।
    //
    // TK-এর নির্দেশ (হুবহু):
    //   *"যতক্ষণ চেকআপ ... হিস্টরি তৈরি না হবে ততক্ষণ ওই হিস্টরি তে ক্লিক
    //     করলে শুধুমাত্র ওয়ার্নিং দেখাবে যেই রোগীর এখনো চেকআপ সম্পূর্ণ হয়নি।
    //     আর যদি কোন রোগের চেকআপ কমপ্লিট হয়ে থাকে তবে ওখানে চাপ দিলে a4
    //     সাইজে প্রিন্ট আউট করার ব্যবস্থা থাকবে, ভিউ করার ব্যবস্থা থাকবে,
    //     এবং হোয়াটসঅ্যাপে শেয়ার করার ব্যবস্থা থাকবে"*
    //   *"এই ফর্মটা বাংলা এবং ইংরেজি দুটো যেন থাকে, আমরা যখন যেটা দেখতে
    //     চাইছি তখন সেটা যেন দেখতে পারি"* ⇒ পপ-আপের উপরেই ভাষা বাছাই।
    //
    // ⛔ **ফ্রি-প্লান নিরাপদ:** একটাও নতুন Supabase কল নেই। রোগীর সারিটা
    //    (`doctorComplete` + `doctorFullNote`) হেডার আঁকার সময় **আগে থেকেই**
    //    আনা হয়ে গেছে; এই সেশনে সেভ করলে সেটাও মনে রাখা হয়।
    // ⛔ কোনো ফর্ম-ফিল্ড/সেভ-লজিক বদলায়নি — এটা শুধু "দেখা/ছাপা/পাঠানো"র স্তর।
    // ─────────────────────────────────────────────────────────────────────

    /** হেডার আঁকার সময় পাওয়া রোগীর সারি থেকে — চেকআপ শেষ হয়েছে কি না। */
    private var hdrDoctorComplete = false
    /** সেই সারিতে সেভ করা চেকআপের পুরো লেখা (JSON), না থাকলে null। */
    private var hdrDoctorNote: org.json.JSONObject? = null

    /** সারির `doctorFullNote` — কোনো পুরোনো সারিতে লেখা (string) হয়ে থাকলেও পড়ে। */
    private fun noteObjOf(row: org.json.JSONObject): org.json.JSONObject? =
        row.optJSONObject("doctorFullNote") ?: try {
            val raw = row.optString("doctorFullNote", "")
            if (raw.trimStart().startsWith("{")) org.json.JSONObject(raw) else null
        } catch (_: Throwable) { null }

    /** কোন ভাষায় শেষবার দেখা হয়েছিল — পরের বার সেটাই আগে থেকে বাছা থাকে।
     *
     * 🟢🔒🔒 V643 (২৪.০৮.২০২৬, TK-রিপোর্ট — "কিশানগঞ্জের স্টাফের মোবাইলে
     * এখনো বাংলা কেন দেখাচ্ছে") — **আসল কারণ (কোড ধরে যাচাই, গভীরে গিয়ে
     * খোঁজা):** এই A4 রিপোর্ট (Doctor Check-up History) WebView-এ আঁকা হয়
     * — `NoBengali.sweep()` শুধু ফোনের নিজস্ব View-গাছ (TextView ইত্যাদি)
     * ঘুরে বাংলা মোছে, WebView-এর ভেতরের HTML কখনো ছুঁতে পারে না। এই
     * রিপোর্টের নিজস্ব দ্বি-ভাষা টগল (V584, TK-অনুমোদিত, বাংলা/English)
     * সম্পূর্ণ **স্বাধীন** — ডিফল্টই ছিল বাংলা (`CheckupA4Lang.BN`), আর
     * Kishanganj-এর বাংলা-বন্ধ নিয়মের সাথে কখনো জোড়াই হয়নি। ফলে
     * Kishanganj-এর যেকোনো স্টাফ এই পর্দায় ঢুকলে ডিফল্ট বাংলাই পেতেন,
     * আর ইচ্ছে করলে "বাংলা" বোতাম চেপে বাংলাও বেছে নিতে পারতেন — কোনো
     * বাধা ছিল না।
     * **সমাধান:** Kishanganj-এর বাংলা-বন্ধ স্টাফের জন্য ভাষা সবসময়
     * জোর করে English — জমানো পছন্দ/ডিফল্ট কিছুই আর ধরা হয় না। ⛔ অন্য
     * সব স্টাফের জন্য আগের দুই-ভাষা সুবিধা (TK-অনুমোদিত V584) এক অক্ষরও
     * বদলায়নি।
     */
    private fun a4Lang(): String {
        if (NoBengali.active()) return CheckupA4Lang.EN
        return getSharedPreferences("v584_a4", MODE_PRIVATE).getString("lang", CheckupA4Lang.BN)
            ?: CheckupA4Lang.BN
    }

    private fun setA4Lang(v: String) {
        try { getSharedPreferences("v584_a4", MODE_PRIVATE).edit().putString("lang", v).apply() }
        catch (_: Throwable) { }
    }

    /**
     * চেকআপ শেষ হয়েছে এমন রেকর্ড — এই সেশনে সেভ করা থাকলে সেটাই (সবচেয়ে
     * নতুন), নইলে ক্লাউড-সারিতে জমা থাকা লেখা থেকে। কিছুই না থাকলে null।
     */
    private fun completedCheckup(): CheckupRecord? {
        lastSavedRecord?.let { return it }
        if (!hdrDoctorComplete) return null
        val obj = hdrDoctorNote ?: return null
        if (obj.length() == 0) return null
        return try { CheckupNoteJson.fromMap(jsonToMap(obj)) } catch (_: Throwable) { null }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 🩺🔒 V839 — NEXT VISIT PLAN · পরের বার এই রোগীর কী হবে
    // TK-নির্দেশ (২৯.০৮.২০২৬, ফটো-প্রুফ দেখিয়ে অনুমোদিত)।
    // ⛔ চেকআপের আর কোনো ঘর · Treatment Plan · খরচ · ছাপার কাগজ — কিছুই
    //    ছোঁয়া হয়নি। এই অংশটা সম্পূর্ণ আলাদা, নিজের ঘরে (`nextVisitPlan`) লেখে।
    // ═══════════════════════════════════════════════════════════════════

    /** ৯টা চেকবক্স — XML-এর id-র সাথে `NextVisitPlan.OPTIONS`-এর চাবি মেলানো। */
    private fun nvpBoxes(): List<Pair<String, android.widget.CheckBox>> =
        NextVisitPlan.OPTIONS.mapNotNull { opt ->
            val idName = "cbNvp" + opt.key.replaceFirstChar { it.uppercase() }
            val id = resources.getIdentifier(idName, "id", packageName)
            if (id == 0) null else findViewById<android.widget.CheckBox>(id)?.let { opt.key to it }
        }

    private var nvpDateIso: String = ""

    private fun setupNextVisitPlan(row: org.json.JSONObject) {
        val boxes = nvpBoxes()
        val medRow = findViewById<android.view.View>(R.id.rowNvpMedicine)
        val etMed = findViewById<android.widget.EditText>(R.id.etNvpMedicine)
        val etNote = findViewById<android.widget.EditText>(R.id.etNvpNote)
        val tvDate = findViewById<TextView>(R.id.tvNvpDate)
        val tvBy = findViewById<TextView>(R.id.tvNvpBy)

        /* ঔষধ বাছলে তবেই "কোন ঔষধ" ঘরটা দেখায় — না বাছলে লুকানো। */
        fun syncMedicineRow() {
            val on = boxes.firstOrNull { it.first == NextVisitPlan.KEY_MEDICINE }?.second?.isChecked == true
            medRow.visibility = if (on) android.view.View.VISIBLE else android.view.View.GONE
        }
        for ((_, cb) in boxes) cb.setOnCheckedChangeListener { _, _ -> syncMedicineRow(); refreshNvpFold() }
        syncMedicineRow()
        refreshNvpFold()

        /* তারিখ — বাধ্যতামূলক নয় (TK-নির্দেশ)। অতীতের তারিখ বাছা যায় না। */
        tvDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            if (nvpDateIso.isNotBlank()) try {
                val q = nvpDateIso.split("-").map { it.toInt() }
                cal.set(q[0], q[1] - 1, q[2])
            } catch (_: Throwable) { }
            android.app.DatePickerDialog(
                this, com.tkbiswas.pilesclinic.R.style.PilesDatePicker,
                { _, y, m, d ->
                    nvpDateIso = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                    tvDate.text = com.tkbiswas.pilesclinic.native.FollowUpModel.displayDate(nvpDateIso)
                    tvDate.setTextColor(android.graphics.Color.parseColor("#101828"))
                },
                cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = System.currentTimeMillis() - 1000L }.show()
        }

        /* গত বারের প্ল্যান — শুধু **দেখানোর** জন্য, ফর্মে ভরা হয় না।
           ⛔ B437-এর নিয়ম: আগের লেখা কখনো সম্পাদনাযোগ্য ঘরে বসে না। */
        val last = NextVisitPlan.latest(row)
        if (last != null) {
            val whenTxt = if (last.at.length >= 10) com.tkbiswas.pilesclinic.native.FollowUpModel.displayDate(last.at.take(10)) else ""
            tvBy.text = "Last plan · " + last.shortLine() +
                (if (whenTxt.isNotBlank()) "  ($whenTxt" else "") +
                (if (last.byName.isNotBlank()) (if (whenTxt.isNotBlank()) " · " else "  (") + last.byName else "") +
                (if (whenTxt.isNotBlank() || last.byName.isNotBlank()) ")" else "")
            tvBy.visibility = android.view.View.VISIBLE
        } else {
            tvBy.visibility = android.view.View.GONE
        }
    }

    /** পর্দা থেকে আজকের প্ল্যানটা তোলা — **মূল থ্রেডেই** ডাকতে হবে। */
    private fun collectNextVisitPlan(): NextVisitPlan.Entry {
        val boxes = nvpBoxes()
        val items = boxes.filter { it.second.isChecked }.map { it.first }
        val med = findViewById<android.widget.EditText>(R.id.etNvpMedicine).text?.toString().orEmpty().trim()
        val note = findViewById<android.widget.EditText>(R.id.etNvpNote).text?.toString().orEmpty().trim()
        val user = com.tkbiswas.pilesclinic.native.NativeSession.current(this)
        return NextVisitPlan.Entry(
            date = nvpDateIso,
            items = items,
            /* ঔষধ না বাছলে লেখাটা জমা হয় না — ভুল তথ্য জমার সুযোগ নেই। */
            medicine = if (items.contains(NextVisitPlan.KEY_MEDICINE)) med else "",
            note = note,
            byName = user?.name.orEmpty(),
            byMobile = user?.mobile.orEmpty()
        )
    }

    private fun openCheckupHistory() {
        val rec = completedCheckup()
        if (rec == null) {
            // ── TK: "শুধুমাত্র ওয়ার্নিং দেখাবে" ──
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Check-up History")
                .setMessage(
                    "এই রোগীর ডাক্তার চেক-আপ এখনো সম্পূর্ণ হয়নি।\n\n" +
                    "চেক-আপ শেষ করে Save করলে এখান থেকে রিপোর্ট দেখা, A4 প্রিন্ট ও " +
                    "WhatsApp-এ পাঠানো যাবে।"
                )
                .setPositiveButton("OK", null)
                .show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            return
        }
        showCheckupHistoryDialog(rec)
    }

    private fun showCheckupHistoryDialog(rec: CheckupRecord) {
        var lang = a4Lang()
        fun dp(v: Int) = symDp(v)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, 0)
        }

        // ── ভাষা বাছাইয়ের সারি (বাংলা | English) ──
        val seg = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(2), android.graphics.Color.parseColor("#0F5132"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(dp(16), 0, dp(16), dp(4))
            layoutParams = lp
        }
        val segViews = HashMap<String, TextView>()
        fun paintSeg() {
            for ((k, tv) in segViews) {
                val on = (k == lang)
                tv.setBackgroundColor(
                    if (on) android.graphics.Color.parseColor("#0F5132") else android.graphics.Color.TRANSPARENT)
                tv.setTextColor(
                    if (on) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#0F5132"))
            }
        }
        for ((key, label) in listOf(CheckupA4Lang.BN to "বাংলা", CheckupA4Lang.EN to "English")) {
            // 🟢🔒 V643 — বাংলা-বন্ধ (Kishanganj) স্টাফের সামনে "বাংলা"
            // বোতামটাই দেখানো হয় না, তাই ইচ্ছে করেও বাংলা বেছে নিতে
            // পারবেন না। বাকি সব স্টাফের জন্য দুই-ভাষা বোতামই থাকে।
            if (key == CheckupA4Lang.BN && NoBengali.active()) continue
            val tv = TextView(this).apply {
                text = label
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, dp(9), 0, dp(9))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { lang = key; setA4Lang(key); paintSeg() }
            }
            segViews[key] = tv
            seg.addView(tv)
        }
        paintSeg()
        root.addView(seg)
        root.addView(TextView(this).apply {
            text = NoBengali.s("রিপোর্ট কোন ভাষায় বেরোবে — চাপ দিয়ে বদলান")
            textSize = 10.5f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#8A949E"))
            setPadding(dp(16), 0, dp(16), dp(6))
        })

        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Check-up History")
            .setView(root)
            .setNegativeButton("Back", null)
            .create()

        // ── তিনটে কাজ — TK-এর নির্দেশে লেখাগুলো ইংরেজিতে ──
        fun action(icon: String, label: String, run: (String) -> Unit) {
            root.addView(TextView(this).apply {
                text = "$icon   $label"
                textSize = 14.5f
                setTextColor(android.graphics.Color.parseColor("#12331F"))
                setPadding(dp(20), dp(12), dp(20), dp(12))
                setOnClickListener {
                    try { dlg.dismiss() } catch (_: Throwable) { }
                    run(lang)
                }
            })
        }
        action("👁", "View") { l -> showCheckupReportView(buildA4Html(rec, l)) }
        action("🖨", "A4 Print") { l -> printHtmlA4(buildA4Html(rec, l)) }
        action("📱", "Send on WhatsApp") { l ->
            try {
                com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
                    activity = this,
                    html = buildA4Html(rec, l),
                    documentTitle = "Check-up",
                    patientName = RoleSession.currentPatientName,
                    allowPrint = true
                )
            } catch (e: Throwable) {
                Toast.makeText(this, "Share not available: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
    }

    /** View — পুরো পর্দা জুড়ে A4 রিপোর্ট, উপরে Back। */
    private fun showCheckupReportView(html: String) {
        try {
            val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val bar = TextView(this).apply {
                text = "◀  Back"
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#0F5132"))
                setPadding(symDp(16), symDp(12), symDp(16), symDp(12))
            }
            val wv = android.webkit.WebView(this).apply {
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
            }
            box.addView(bar); box.addView(wv)
            val dlg = android.app.Dialog(this, android.R.style.Theme_Light_NoTitleBar)
            dlg.setContentView(box)
            bar.setOnClickListener { try { dlg.dismiss() } catch (_: Throwable) { } }
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
        } catch (e: Throwable) {
            Toast.makeText(this, "View not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** A4 Print — `printCheckup()`-এর হুবহু একই প্রমাণিত পথ, শুধু HTML বাইরে থেকে। */
    private fun printHtmlA4(html: String) {
        try {
            val wv = android.webkit.WebView(this)
            wv.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                    try {
                        val pm = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val adapter = view.createPrintDocumentAdapter("DoctorCheckup")
                        pm.print("Doctor Checkup", adapter, com.tkbiswas.pilesclinic.native.PrintQuality.builder().build())
                    } catch (_: Throwable) {
                        Toast.makeText(this@DoctorCheckupActivity, "Print not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            printWebView = wv
            wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        } catch (_: Throwable) {
            Toast.makeText(this, "Print not available", Toast.LENGTH_SHORT).show()
        }
    }

    private var printWebView: android.webkit.WebView? = null
    private fun printCheckup() {
        // 🆕 (07.08.2026) — অন-স্ক্রিন A4 রিপোর্টের হুবহু একই HTML ছাপে (আগে সরল
        // list-HTML ছিল)। সেভ-করা রেকর্ড থাকলে সেটাই, নইলে বর্তমান ফর্ম।
        val r = lastSavedRecord ?: collect()
        val html = buildA4Html(r)
        try {
            val wv = android.webkit.WebView(this)
            wv.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                    try {
                        val pm = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val adapter = view.createPrintDocumentAdapter("DoctorCheckup")
                        pm.print("Doctor Checkup", adapter, com.tkbiswas.pilesclinic.native.PrintQuality.builder().build())
                    } catch (_: Throwable) {
                        Toast.makeText(this@DoctorCheckupActivity, "Print not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            printWebView = wv
            // 🆕 baseURL = asset — যাতে ক্লিনিক-লোগো (www/assets/...) ছাপায় দেখা যায়।
            wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        } catch (_: Throwable) {
            Toast.makeText(this, "Print not available", Toast.LENGTH_SHORT).show()
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       💰🔒 V971 (০২.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ ও PDF নমুনা) —
       **এস্টিমেট বানানোর বোতাম।**

       TK: *"আমরা হাতে একটা অ্যামাউন্ট করি, আমি চাইছি তার পাশে একটা অপশন
       থাকুক, যে অপশনটা ক্লিক করলে একটা পপ-আপ খুলবে"*।

       ⛔ Estimated Cost ঘরটার id · সেভ-লজিক · A4 রিপোর্ট · মাস্টারের কাছে
          যাওয়া "খরচ বদলেছে" বার্তা — কিছুই বদলায়নি। পপ-আপ শুধু ওই ঘরে
          **Net Payable** বসিয়ে দেয়, বাকি সব আগের পথেই চলে।
       ⛔ ব্যর্থ হলেও (কোনো কারণে) চেকআপ একটুও আটকায় না।
       ═══════════════════════════════════════════════════════════════════ */
    /** 🖥️ V982 — কাগজের পর্দা থেকে ফেরার সংকেত। */
    private val REQ_ESTIMATE_PAPER = 9821

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_ESTIMATE_PAPER || resultCode != RESULT_OK) return
        try {
            val sheet = EstimateModel.parse(data?.getStringExtra(EstimatePaperActivity.RESULT_SHEET))
            estimateSheet = sheet
            if (!sheet.isEmpty) etEstimatedCost.setText(EstimateModel.moneyShort(sheet.netPayable))
            /* 🧮🔒 V1280 (TK: *"কস্ট এসটিমেটে চাপ দিলে যখন রোগের নাম এবং কত খরচ
               অটোমেটিক ক্যালকুলেশন হয়ে যাচ্ছে, তাহলে আবার হাতে টাইপ করার
               দরকার নেই · রোগের নামও ওখানে সিলেক্ট করা যায় · সময় চাওয়া
               হলো সেটাও এস্টিমেটের মধ্যে"*) — একই কাগজ থেকে **রোগ · চিকিৎসা ·
               সময়** তিনটেও বসে যায়, আগের লুকানো ঘরগুলোতেই:
               • রোগ → ধাপ ৪-এর চিপে (তাই `patients.disease` বদলানোর পুরনো
                 নিয়ম, নোটিফিকেশন, কাগজের "Probable Disease" — সব আগের মতোই)
               • চিকিৎসা → পুরনো টিক-বাক্সে (কাটা লাইন বাদ), তাই কাগজের
                 TREATMENT PLAN লাইন আগের মতোই ভরে
               • সময় → ধাপ ৪-এর `etTimeAsked` + একক
               ⛔ কোনো নতুন ঘর/টেবিল নয় — সেভের কোড এক অক্ষরও বদলায়নি।
               ⛔ এস্টিমেটে কিছু না বাছলে আগের মান হুবহু থাকে (মুছে যায় না)। */
            applyEstimateToCheckup(sheet)
        } catch (_: Throwable) { }
    }

    /** 🧮 V1280 — এস্টিমেটের কাগজ থেকে রোগ · চিকিৎসা · সময় ⇒ চেকআপের লুকানো ঘরে। */
    private fun applyEstimateToCheckup(sheet: EstimateModel.Sheet) {
        try {
            val live = sheet.lines.filter { !it.struck }
            // রোগ — চিকিৎসার লাইনের গ্রুপ থেকে (Piles · Fistula · Fissure · Hydrocele)
            val groups = LinkedHashSet<String>()
            for (l in live) {
                val g = EstimatePrices.groupOfLine(this, l.name)
                if (g.isNotBlank()) groups.add(g)
            }
            if (groups.isNotEmpty()) applyProbableDisease(groups.joinToString(", "))
            // চিকিৎসা — টিক-বাক্সে (নাম মিলিয়ে); কোনো চিকিৎসার লাইন না থাকলে ছোঁয়া হয় না
            val txNames = live.filter { EstimatePrices.groupOfLine(this, it.name).isNotBlank() }
                .map { it.name.lowercase() }
            if (txNames.isNotEmpty()) {
                cbTxPerPiles.isChecked = txNames.any { it.contains("piles") || it.contains("অর্শ") }
                cbTxFistulaInch.isChecked = txNames.any { it.contains("fistula") || it.contains("ফিস্টুলা") }
                cbTxKsharSutra.isChecked = txNames.any { it.contains("kshar") || it.contains("ক্ষার") }
            }
            // সময় — এস্টিমেটে লেখা থাকলে
            val (tAmt, tUnit) = CounselModel.splitTimeAsked(sheet.timeAsked)
            if (tAmt.isNotBlank()) {
                findViewById<android.widget.EditText>(R.id.etTimeAsked).setText(tAmt)
                val ui = CounselModel.UNITS.indexOf(tUnit).let { if (it < 0) 0 else it }
                findViewById<android.widget.Spinner>(R.id.spTimeAskedUnit).setSelection(ui)
            }
            renderEstimateSummary()
        } catch (_: Throwable) { }
    }

    /** 🧮 V1280 — ধাপ ৩-এর বোতামের নিচে "FROM COST ESTIMATE" সারাংশ (শুধু দেখার)। */
    private fun renderEstimateSummary() {
        try {
            val box = findViewById<TextView>(R.id.tvEstimateSummary) ?: return
            val disease = collectProbableDisease().let { if (it == CounselModel.PICK_NONE) "" else it }
            val cost = etEstimatedCost.text?.toString()?.trim().orEmpty()
            val time = CounselModel.timeAsked(
                findViewById<android.widget.EditText>(R.id.etTimeAsked).text?.toString().orEmpty(),
                CounselModel.UNITS.getOrElse(findViewById<android.widget.Spinner>(R.id.spTimeAskedUnit).selectedItemPosition) { "Days" })
            val parts = ArrayList<String>()
            if (disease.isNotBlank()) parts.add("Disease: $disease")
            if (cost.isNotBlank()) parts.add("Net Payable: ₹$cost")
            if (time.isNotBlank()) parts.add("Time Asked: $time")
            box.text = if (parts.isEmpty()) "Nothing added yet — tap COST ESTIMATE above" else parts.joinToString("\n")
        } catch (_: Throwable) { }
    }

    private fun wireEstimateBuilder() {
        try {
            /* 🧮🔒 V1105 (TK-রিপোর্ট) — হেডারের গোল আইকন **আর** Estimated Cost
               ঘরের পাশের আইকন, দুটোতেই হুবহু একই কাজ। একটাই লিসেনার লিখে দুটোতে
               বসানো হয়েছে, তাই ভবিষ্যতে একটা বদলে অন্যটা আলাদা হয়ে যেতে পারে না। */
            val openEstimate = android.view.View.OnClickListener {
                /* 🖥️🔒 V982 (TK-নির্দেশ: *"তাহলে ফুল স্ক্রিন পর্দা খুলবে, আলাদা
                   পপ-আপ লাগবে না, zoom করা যাবে"*) — এখন পপ-আপের বদলে
                   **কাগজের ফুল-স্ক্রিন পর্দা**। পুরনো পপ-আপের বাছাই-বাক্সগুলোই
                   ওখানে ব্যবহার হয়, তাই দর/গ্রেড/পজিশনের নিয়ম একটুও বদলায়নি। */
                startActivityForResult(
                    android.content.Intent(this, EstimatePaperActivity::class.java)
                        .putExtra(EstimatePaperActivity.EXTRA_SHEET, estimateSheet.toJson().toString()),
                    REQ_ESTIMATE_PAPER
                )
            }
            findViewById<TextView>(R.id.btnBuildEstimate)?.setOnClickListener(openEstimate)
            findViewById<TextView>(R.id.btnBuildEstimateInline)?.setOnClickListener(openEstimate)
            findViewById<TextView>(R.id.btnBuildEstimateBig)?.setOnClickListener(openEstimate)   // 🧮 V1280
        } catch (_: Throwable) { }
    }

    /** কাগজ — প্রকল্পের প্রমাণিত পথেই (PDF · WhatsApp · Print একসাথে)। */
    private fun shareEstimatePaper(sheet: EstimateModel.Sheet) {
        try {
            if (sheet.isEmpty) {
                Toast.makeText(this, "Add at least one item first", Toast.LENGTH_SHORT).show()
                return
            }
            val age = RoleSession.currentPatientAge
            val sex = RoleSession.currentPatientSex
            val ageSex = listOf(age, sex).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "-" }
            val html = EstimateHtmlPrint.build(
                sheet = sheet,
                branch = RoleSession.currentPatientBranch,
                name = RoleSession.currentPatientName.ifBlank { "-" },
                patientId = RoleSession.currentPatientDisplayId.ifBlank { RoleSession.currentPatientId },
                ageSex = ageSex,
                mobile = RoleSession.currentPatientMobile.ifBlank { "-" },
                address = RoleSession.currentPatientAddress.ifBlank { "-" },
                date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(java.util.Date())
            )
            com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
                activity = this,
                html = html,
                documentTitle = "Estimate",
                patientName = RoleSession.currentPatientName,
                allowPrint = true
            )
        } catch (e: Throwable) {
            Toast.makeText(this, "Share not available: " + e.message, Toast.LENGTH_LONG).show()
        }
    }
}
