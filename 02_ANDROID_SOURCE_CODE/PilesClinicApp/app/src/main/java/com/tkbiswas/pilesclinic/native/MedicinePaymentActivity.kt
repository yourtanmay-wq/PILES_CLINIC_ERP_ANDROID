package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.tkbiswas.pilesclinic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

/**
 * Native rebuild of the WebView medicinePaymentHome() / saveMedicinePayment():
 * records a medicine/product sale into the "products" table with the same
 * fields and formula (due = max(0, bill - deposit), mode CASH/ONLINE), and lists
 * the medicine payment history below, scoped to the user's branch.
 */
class MedicinePaymentActivity : AppCompatActivity() {

    private lateinit var etCustomer: EditText
    private lateinit var btnBranchChip: TextView
    private lateinit var customerSuggestions: LinearLayout
    private lateinit var etProduct: EditText
    private lateinit var etBill: EditText
    private lateinit var etDeposit: EditText
    /* 🔄🔒 V849 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — TK: "Remarks বক্সটাই
       থাকবে না, সেখানে হবে … প্রকৃত জমার তারিখ"। PaymentActivity-র হুবহু একই
       ধাঁচ: না ছুঁলে আজকের তারিখ · চাপলে পুরনো তারিখ বাছা যায় · ভবিষ্যতের
       তারিখ বাছা যায় না (maxDate = আজ)। */
    private lateinit var tvActualDate: TextView
    private var pickedActualDate: String = ""
    private lateinit var historyContainer: LinearLayout
    private lateinit var progressLoad: ProgressBar
    private lateinit var tvEmpty: TextView

    // 🔒 B558-B560 (08.08.2026, TK-অনুমোদিত প্রুফ) — একাধিক মেডিসিন,
    // Save/Share/Print, প্রফেশনাল History-র নতুন ঘরগুলো।
    private lateinit var medicineContainer: LinearLayout
    private lateinit var btnAddMedicine: TextView
    private lateinit var etHistorySearch: EditText
    private lateinit var chipToday: TextView
    private lateinit var chip7: TextView
    private lateinit var chip30: TextView
    private lateinit var chipPick: TextView
    private lateinit var chipStatement: TextView  // 🆕 V847
    private lateinit var chipDue: TextView        // 🆕 V846
    private lateinit var tvHistoryTotal: TextView

    // History ছাঁকা (সব client-side, কোনো বাড়তি সার্ভার-কল নয়): সর্বশেষ
    // ক্লাউড/ক্যাশ থেকে নামানো পূর্ণ তালিকা এখানে রাখি, তারপর ব্রাঞ্চ + তারিখ +
    // সার্চ প্রয়োগ করে দেখাই।
    private var loadedRows: org.json.JSONArray = org.json.JSONArray()
    private var userBranchFilter: String = "All"
    private var activeDateFilter: String = "today"   // today | 7 | 30 | pick | range
    private var pickedDate: String? = null            // yyyy-MM-dd
    private var searchQuery: String = ""

    /* 🆕🔒 V846 (৩০.০৮.২০২৬) — TK: "বিল ২০০০, দিলেন ১২০০, বাকি ৮০০ — এই বাকি
       টাকাটা কোথায় শো করবে?" ⇒ "Due Only" চিপ চাপলে **তারিখ না দেখে** সব দিনের
       বাকি-থাকা বিক্রি একসাথে দেখায়, উপরে মোট বাকির অঙ্ক। */
    private var dueOnly: Boolean = false

    /* 🆕🔒 V847 (৩০.০৮.২০২৬) — TK: "Pick Date-এর পরে … কত তারিখ থেকে কত তারিখ
       পর্যন্ত"। নাম TK নিজে বেছেছেন — **Statement**। */
    private var rangeFrom: String? = null            // yyyy-MM-dd
    private var rangeTo: String? = null              // yyyy-MM-dd

    private val branches = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")

    companion object {
        /** 🆕 V805 — `products.kind`-এর দুটো মান। পুরনো সব সারি = ওষুধ। */
        const val KIND_MEDICINE = "medicinePayment"
        const val KIND_SALINE = "salinePayment"
        /** এই পর্দার তালিকায় **দুটোই** দেখাবে। */
        fun isSaleRow(kind: String) = kind == KIND_MEDICINE || kind == KIND_SALINE

        /* 🆕🔒 V846 — পরে বাকি টাকা জমা নেওয়ার নিয়ম।
           ─── কেন পুরনো সারিটা বদলাই না ────────────────────────────────────
           পুরনো সারির `deposit` বাড়ালে **ওই পুরনো দিনের** কালেকশনের মোট টাকা
           পিছন-ফিরে বদলে যেত (PaymentRepository ও ChamberAttendanceRepository
           দুটোই `date` ধরে `deposit` যোগ করে) — অর্থাৎ ছাপানো পুরনো রেজিস্টারের
           সঙ্গে অ্যাপের হিসাব আর মিলত না, আর আজ হাতে-পাওয়া টাকাটা আজকের
           কালেকশনে উঠতই না।
           ─── তাই যা করি ──────────────────────────────────────────────────
           বাকি জমা নিলে **আজকের তারিখে একটা নতুন সারি** বসে (deposit = যত টাকা
           নেওয়া হলো, bill = 0)। ⇒ আজকের কালেকশন ঠিক, পুরনো দিনের হিসাবে
           একটুও হাত পড়ে না।
           ─── পুরনো সারিটা "শোধ হয়েছে" বোঝা যায় কী করে ───────────────────
           নতুন সারির `id` = "due_" + পুরনো id + "_" + সময়। কোনো নতুন ডেটাবেস-ঘর
           বা SQL লাগে না, আর `remarks`-এর মতো হাতে-লেখা ঘরের উপর ভরসা করতে
           হয় না। */
        const val DUE_ID_PREFIX = "due_"
        fun isDueSettlementId(id: String) = id.startsWith(DUE_ID_PREFIX)
        /** "due_<origId>_<millis>" → "<origId>" (না হলে ফাঁকা)। */
        fun settledSaleIdOf(id: String): String {
            if (!isDueSettlementId(id)) return ""
            val body = id.substring(DUE_ID_PREFIX.length)
            val cut = body.lastIndexOf('_')
            return if (cut > 0) body.substring(0, cut) else ""
        }
    }
    private val modes = listOf("CASH", "ONLINE")

    // TK-REQUESTED REDESIGN (2026-07-20): Branch is now a compact tappable
    // chip (see activity_medicine_payment.xml) instead of a Spinner --
    // selection is tracked here the same way spBranch.selectedItem was read.
    private var selectedBranch = "Kishanganj"
    private var selectedMpMode = "CASH"

    /* 🆕🔒 V805 (২৮.০৮.২০২৬, TK-অনুমোদিত) — TK-নির্দেশ: *"রোগীকে স্যালাইন লাগানো
       হলে তার জন্য আলাদা টাকা নেয়া হয় … এখানে সেটা জয়েন করে দিন, নাম হবে
       medicine or saline"*।
       ⛔ **নতুন কোনো ডেটাবেস-ঘর লাগেনি** — `products` টেবিলের পুরনো `kind` ঘরটাই
          ব্যবহার হচ্ছে: ওষুধ হলে আগের মতোই `medicinePayment`, স্যালাইন হলে
          `salinePayment`। ⇒ TK-কে কোনো SQL চালাতে হবে না, আর **পুরনো সব সারি
          আগের মতোই ওষুধ হিসেবেই থাকবে** (একটাও সারি ছোঁয়া হয়নি)। */
    private var selectedSaleType = KIND_MEDICINE

    // 🔒 B619 (11.08.2026, TK-নির্দেশ): ওষুধ বিক্রিও টাকা — তাই নিজের ব্রাঞ্চ ছাড়া
    // অন্য ব্রাঞ্চ দেখা/বেছে নেওয়া যাবে না (master সব ব্রাঞ্চ পারবেন)। MoneyBranchGuard-এর
    // হুবহু একই নিয়ম, শুধু এখানে UI-তেই লক করা হয় যাতে অন্য ব্রাঞ্চ চোখেই না পড়ে।
    private var isMasterUser = false

    // TK-REPORTED CRITICAL BUG FIX (2026-07-26): true only while a Medicine
    // Payment save is actually in flight, so a second tap on Save during a
    // slow upload can never write a second "products" row for the same money.
    private var mpSaving = false

    private fun setupBranchChip(defaultBranch: String) {
        selectedBranch = defaultBranch
        // 🔒 B619 (11.08.2026, TK-নির্দেশ): master ছাড়া কেউ অন্য ব্রাঞ্চ দেখতেই পারবে না —
        // চিপ তালাবন্ধ (▾ নেই, ট্যাপে কিছু হয় না), শুধু নিজের ব্রাঞ্চ দেখায়।
        if (!isMasterUser) {
            btnBranchChip.text = "🏥 $selectedBranch"
            btnBranchChip.isClickable = false
            btnBranchChip.setOnClickListener(null)
            return
        }
        btnBranchChip.text = "🏥 $selectedBranch ▾"
        btnBranchChip.setOnClickListener {
            // 🔒 খাতার সারি B84 — Follow-up-এর হুবহু একই পপ-আপ।
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "Branch"))
                .setSingleChoiceItems(branches.toTypedArray(), branches.indexOf(selectedBranch)) { dialog, which ->
                    selectedBranch = branches[which]
                    btnBranchChip.text = "🏥 $selectedBranch ▾"
                    dialog.dismiss()
                    /* 🔴🔒 V804 (২৮.০৮.২০২৬) — TK: "Kishanganj ব্রাঞ্চ সিলেক্ট করা আছে,
                       কিন্তু এখানে Jalpaiguri-র মেডিসিন পেমেন্ট কেন দেখাচ্ছে?"
                       ─── আসল কারণ ─────────────────────────────────────────────
                       উপরের এই চিপটা এতদিন শুধু **নিজের লেখাটাই** বদলাত আর নতুন
                       বিক্রি কোন ব্রাঞ্চে বসবে সেটা ঠিক করত — **নিচের তালিকাটা
                       একবারও নতুন করে আঁকা হত না**। আর তালিকা ছাঁকা হত
                       `user.branch` দিয়ে (`loadHistory(user?.branch)`), যেটা
                       Master-এর ক্ষেত্রে **"All"** ⇒ কোনো ছাঁকনিই বসত না।
                       ⇒ Master যে ব্রাঞ্চই বাছুন, **সব ব্রাঞ্চের** বিক্রি দেখাত,
                         আর উপরের সবুজ ঘরের মোট টাকা/সংখ্যাও সব ব্রাঞ্চ মিলিয়ে।
                       ─── সারানো ───────────────────────────────────────────────
                       চিপ বদলালেই তালিকা আবার আঁকা হয়, আর ছাঁকনি হয় **চিপের
                       ব্রাঞ্চ** দিয়ে। ⛔ Master ছাড়া কারো কিছুই বদলায়নি — তাদের
                       `selectedBranch` আর `user.branch` একই (লাইন ২১৭)। */
                    showRows(loadedRows, selectedBranch)
                }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
        }
    }

    // TK-REQUESTED ADDITION (2026-07-20): as the person types a name/mobile,
    // show matching registered patients below the field to tap-select --
    // reuses the exact same PaymentRepository.searchPatients() already built
    // (and TK-approved) for Payment Collection's Search Patient dialog, so
    // the matching logic is identical between the two screens.
    private var customerSearchJob: kotlinx.coroutines.Job? = null
    private fun setupCustomerSuggestions() {
        val repo = PaymentRepository(this)
        etCustomer.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val q = s?.toString().orEmpty()
                customerSearchJob?.cancel()
                if (q.trim().length < 2) { customerSuggestions.removeAllViews(); return }
                customerSearchJob = lifecycleScope.launch {
                    kotlinx.coroutines.delay(250)
                    val found = try {
                        withContext(Dispatchers.IO) { repo.searchPatients(q) }
                    } catch (t: Throwable) { emptyList() }
                    // 🏥🔒 V1235 (নিয়ম ৭ — একই দোষ সব জায়গায়) — Payment-এর
                    // Search Patient-এর মতো এখানেও তালিকাটা টাকার নিয়মেই ছাঁকা হয়
                    // (`MoneyBranchGuard`, TK-এর ২৭.০৭.২০২৬-এর নিজের নিয়ম)।
                    // ⛔ মাস্টার আগের মতোই সবাইকে দেখেন।
                    val matches = found.filter {
                        MoneyBranchGuard.canTakeMoney(this@MedicinePaymentActivity, it.branch, it.patientId)
                    }
                    customerSuggestions.removeAllViews()
                    val density = resources.displayMetrics.density
                    fun dp(v: Int) = (v * density).toInt()
                    for (m in matches.take(5)) {
                        val mobileDigits = m.mobile.filter { it.isDigit() }.takeLast(10)
                        val row = TextView(this@MedicinePaymentActivity).apply {
                            text = PatientIdText.line(m.name, mobileDigits, m.patientId, phoneIcon = true)
                            textSize = 12.5f
                            setPadding(dp(10), dp(8), dp(10), dp(8))
                            setBackgroundColor(android.graphics.Color.parseColor("#F2F8F4"))
                            setTextColor(android.graphics.Color.parseColor("#0A5428"))
                            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                            lp.bottomMargin = dp(4)
                            layoutParams = lp
                            setOnClickListener {
                                etCustomer.setText(m.name.ifBlank { mobileDigits })
                                etCustomer.setSelection(etCustomer.text?.length ?: 0)
                                // 🔒 B619: শুধু master-এর ক্ষেত্রে রোগীর ব্রাঞ্চ ধরে চিপ বদলায়;
                                // নন-মাস্টার সবসময় নিজের ব্রাঞ্চে তালাবন্ধ থাকে।
                                if (isMasterUser && m.branch.isNotBlank() && branches.contains(m.branch)) {
                                    selectedBranch = m.branch
                                    btnBranchChip.text = "🏥 $selectedBranch ▾"
                                }
                                customerSuggestions.removeAllViews()
                            }
                        }
                        customerSuggestions.addView(row)
                    }
                }
            }
        })
    }

    /* 🔄🔒 V849 — প্রকৃত জমার তারিখ। না ছুঁলে `pickedActualDate` ফাঁকাই থাকে
       ⇒ সেভের সময় আজকের তারিখ বসে (আগের আচরণ হুবহু অটুট)। */
    private fun actualDate(): String = pickedActualDate.ifBlank { today() }

    private fun resetActualDate() {
        pickedActualDate = ""
        tvActualDate.text = "Actual deposit date"
        tvActualDate.setTextColor(0xFF9AA4B0.toInt())
    }

    private fun setupActualDate() {
        resetActualDate()
        tvActualDate.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            try {
                if (pickedActualDate.isNotBlank()) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(pickedActualDate)?.let { cal.time = it }
                }
            } catch (_: Exception) { }
            android.app.DatePickerDialog(this, { _, y, m, d ->
                val c = java.util.Calendar.getInstance().apply { set(y, m, d) }
                val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
                if (iso == today()) {
                    resetActualDate()
                } else {
                    pickedActualDate = iso
                    tvActualDate.text = DateUtil.display(iso)
                    tvActualDate.setTextColor(0xFF10223A.toInt())
                }
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH),
               cal.get(java.util.Calendar.DAY_OF_MONTH)).apply {
                // ⛔ ভবিষ্যতের তারিখ বাছা যাবে না (TK-নির্দেশ)
                try { datePicker.maxDate = System.currentTimeMillis() } catch (_: Throwable) { }
            }.show()
        }
    }

    private fun setupMpModeButtons() {
        val cash = findViewById<android.widget.Button>(R.id.btnMpCash)
        val upi = findViewById<android.widget.Button>(R.id.btnMpUpi)
        val blue = android.graphics.Color.parseColor("#1167D8")
        val lightBlue = android.graphics.Color.parseColor("#E8F2FF")
        fun render() {
            cash.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selectedMpMode == "CASH") blue else lightBlue)
            cash.setTextColor(if (selectedMpMode == "CASH") android.graphics.Color.WHITE else blue)
            upi.backgroundTintList = android.content.res.ColorStateList.valueOf(if (selectedMpMode == "ONLINE") blue else lightBlue)
            upi.setTextColor(if (selectedMpMode == "ONLINE") android.graphics.Color.WHITE else blue)
        }
        cash.setOnClickListener { selectedMpMode = "CASH"; render() }
        upi.setOnClickListener { selectedMpMode = "ONLINE"; render() }
        render()
    }

    /* 🆕🔒 V805 — MEDICINE / SALINE বাছাই। উপরের `setupMpModeButtons()`-এর
       **হুবহু একই কায়দা** (একই render/click নকশা), শুধু রঙ সবুজ — যাতে Mode-এর
       নীল দুটোর সঙ্গে গুলিয়ে না যায়। ঘরের নামদুটোও সঙ্গে সঙ্গে বদলায়, তাই
       স্যালাইন বাছলে "Medicine / Product Name" লেখা থেকে বিভ্রান্তি হয় না। */
    private fun setupSaleTypeButtons() {
        val med = findViewById<android.widget.Button>(R.id.btnTypeMedicine)
        val sal = findViewById<android.widget.Button>(R.id.btnTypeSaline)
        val lblProduct = findViewById<android.widget.TextView>(R.id.lblProduct)
        val lblBill = findViewById<android.widget.TextView>(R.id.lblBill)
        val green = android.graphics.Color.parseColor("#0B6E33")
        val lightGreen = android.graphics.Color.parseColor("#E6F4EC")
        fun render() {
            val isMed = selectedSaleType == KIND_MEDICINE
            med.backgroundTintList = android.content.res.ColorStateList.valueOf(if (isMed) green else lightGreen)
            med.setTextColor(if (isMed) android.graphics.Color.WHITE else green)
            sal.backgroundTintList = android.content.res.ColorStateList.valueOf(if (!isMed) green else lightGreen)
            sal.setTextColor(if (!isMed) android.graphics.Color.WHITE else green)
            lblProduct?.text = if (isMed) "Medicine / Product Name" else "Saline Name / Details"
            lblBill?.text = if (isMed) "Total Medicine Bill" else "Total Saline Bill"
        }
        med.setOnClickListener { selectedSaleType = KIND_MEDICINE; render() }
        sal.setOnClickListener { selectedSaleType = KIND_SALINE; render() }
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_medicine_payment)
        UppercaseInputUtil.applyToAll(window.decorView.findViewById(android.R.id.content))  // TK-REQUESTED GLOBAL RULE (2026-07-24): English text auto-CAPITAL, Password fields excluded automatically
        BottomNav.wire(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etCustomer = findViewById(R.id.etCustomer)
        btnBranchChip = findViewById(R.id.btnBranchChip)
        customerSuggestions = findViewById(R.id.customerSuggestions)
        etProduct = findViewById(R.id.etProduct)
        etBill = findViewById(R.id.etBill)
        etDeposit = findViewById(R.id.etDeposit)
        tvActualDate = findViewById(R.id.tvActualDate)   // 🔄 V849
        historyContainer = findViewById(R.id.historyContainer)
        progressLoad = findViewById(R.id.progressLoad)
        tvEmpty = findViewById(R.id.tvEmpty)
        medicineContainer = findViewById(R.id.medicineContainer)
        btnAddMedicine = findViewById(R.id.btnAddMedicine)
        etHistorySearch = findViewById(R.id.etHistorySearch)
        /* 📕🔒 V985 (TK-নির্দেশ: *"মেডিসিন স্যালাইন History এরকম খোলা থাকবে না
           পর্দা"*) — শিরোনামে চাপ দিলে খোলে, আবার চাপলে গুটিয়ে যায়।
           ⛔ ভিতরের সব কিছু (খোঁজা · চিপ · মোট · তালিকা) হুবহু আগের মতোই। */
        try {
            val head = findViewById<TextView>(R.id.tvHistoryHead)
            val bodyBox = findViewById<LinearLayout>(R.id.boxHistoryBody)
            head.setOnClickListener {
                val open = bodyBox.visibility != View.VISIBLE
                bodyBox.visibility = if (open) View.VISIBLE else View.GONE
                head.text = (if (open) "▼  " else "▶  ") + "Medicine / Saline History"
            }
        } catch (_: Throwable) { }
        /* 💊🔒 V985 (TK-নির্দেশ) — Search-এর "Med. Due" বোতাম থেকে এলে ওই
           রোগীর নম্বরটাই আগে থেকে বসানো থাকে ও শুধু বাকিগুলোই দেখায়, যাতে
           স্টাফকে খুঁজতে না হয়। ⛔ এমনি খুললে কিচ্ছু বদলায় না। */
        try {
            val pre = intent?.getStringExtra("prefill_search").orEmpty()
            if (pre.isNotBlank()) {
                searchQuery = pre; dueOnly = true
                etHistorySearch.setText(pre)
                // Search থেকে এলে তালিকাটা খোলাই থাকে (নইলে কিছুই দেখা যেত না)।
                findViewById<LinearLayout>(R.id.boxHistoryBody).visibility = View.VISIBLE
                findViewById<TextView>(R.id.tvHistoryHead).text = "▼  Medicine / Saline History"
            }
        } catch (_: Throwable) { }
        chipToday = findViewById(R.id.chipToday)
        chip7 = findViewById(R.id.chip7)
        chip30 = findViewById(R.id.chip30)
        chipPick = findViewById(R.id.chipPick)
        chipStatement = findViewById(R.id.chipStatement)  // 🆕 V847
        chipDue = findViewById(R.id.chipDue)          // 🆕 V846
        tvHistoryTotal = findViewById(R.id.tvHistoryTotal)

        // 🔒 B619: ব্রাঞ্চ-লক করার আগে জেনে নিই এই ইউজার master কিনা (master সব ব্রাঞ্চ পারবেন)।
        isMasterUser = NativeSession.current(this)?.role == "master"

        /* 💧🔒 V1598 (১৯.০৯.২০২৬, TK-নির্দেশ) — Payment পর্দার ⋮ মেনুতে "Saline
           Payment" আলাদা চাপলে এই পর্দাই খোলে, শুধু Saline টগল আগে থেকে বাছা
           অবস্থায়। ⛔ ঘরটা না পাঠালে (Medicine Payment থেকে এলে) আগের মতোই
           Medicine ডিফল্ট — কিছু বদলায়নি। */
        try {
            if (intent?.getStringExtra("startKind") == KIND_SALINE) selectedSaleType = KIND_SALINE
        } catch (_: Throwable) { }
        setupMpModeButtons()
        setupSaleTypeButtons()   // 🆕 V805
        setupCustomerSuggestions()
        setupAddMedicine()
        setupHistoryControls()
        setupActualDate()                            // 🔄 V849

        val user = NativeSession.current(this)
        val defaultBranch = if (user != null && user.branch != "All" && branches.contains(user.branch)) user.branch else branches.first()
        setupBranchChip(defaultBranch)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave).setOnClickListener {
            // TK-REPORTED CRITICAL BUG FIX (2026-07-26): same duplicate-money
            // guard as the Advance / Nth Payment / Payment-screen buttons. This
            // one writes a "products" row (Medicine Payment) with a brand new
            // id on every tap, so a second impatient tap during a slow save
            // charged the customer twice. Cleared in commit()'s finally block.
            if (mpSaving) return@setOnClickListener
            save(user?.mobile ?: "", user?.branch ?: "", null)
        }
        // 🔒 B559 (08.08.2026, TK-অনুমোদিত প্রুফ) — Share/Print চাপলে আগে অটো-সেভ,
        // তারপর কাজ। সেভ সফল হলে ফর্ম খালি হয়, তাই দ্বিতীয়বার চাপলে খালি ফর্ম
        // যাচাইয়ে আটকে যায় — এক বিক্রি কখনও দুবার সেভ হয় না (mpSaving গার্ডও আছে)।
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShare).setOnClickListener {
            if (mpSaving) return@setOnClickListener
            save(user?.mobile ?: "", user?.branch ?: "") { row -> shareReceipt(row) }
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPrint).setOnClickListener {
            if (mpSaving) return@setOnClickListener
            save(user?.mobile ?: "", user?.branch ?: "") { row -> printReceipt(row) }
        }

        loadHistory(selectedBranch)   // 🔴 V804 — চিপের ব্রাঞ্চ, user.branch নয়
    }

    // 🔒 B558 — "+ ADD MEDICINE": নতুন ওষুধ-ঘর যোগ, ✕ দিয়ে মোছা।
    private fun setupAddMedicine() {
        btnAddMedicine.setOnClickListener { addMedicineRow(focus = true) }
    }

    private fun addMedicineRow(focus: Boolean) {
        val row = layoutInflater.inflate(R.layout.item_medicine_row, medicineContainer, false)
        val del = row.findViewById<TextView>(R.id.btnMedDel)
        del.setOnClickListener { medicineContainer.removeView(row) }
        medicineContainer.addView(row)
        UppercaseInputUtil.applyToAll(row)  // একই গ্লোবাল নিয়ম: ইংরেজি লেখা বড় হাতের
        if (focus) row.findViewById<EditText>(R.id.etMedItem).requestFocus()
    }

    /** medicineContainer-এর সব ওষুধ-ঘর (প্রথম etProduct + যোগ-করা সব সারি) থেকে
     *  নাম নিয়ে, ফাঁকা বাদ দিয়ে, কমা দিয়ে জুড়ে একটাই স্ট্রিং — পুরনো একক-product
     *  ডেটা-মডেল হুবহু অটুট থাকে। */
    private fun collectMedicines(): String {
        val names = ArrayList<String>()
        fun scan(v: View) {
            when (v) {
                is EditText -> { val t = v.text?.toString()?.trim().orEmpty(); if (t.isNotBlank()) names.add(t) }
                is android.view.ViewGroup -> for (i in 0 until v.childCount) scan(v.getChildAt(i))
            }
        }
        scan(medicineContainer)
        return names.joinToString(", ")
    }

    private fun clearMedicineRows() {
        // প্রথম ঘর (etProduct) রেখে যোগ-করা সব সারি সরাই, তারপর প্রথম ঘর খালি করি।
        for (i in medicineContainer.childCount - 1 downTo 0) {
            if (medicineContainer.getChildAt(i).id != R.id.etProduct) medicineContainer.removeViewAt(i)
        }
        etProduct.setText("")
    }

    private fun save(staffMobile: String, userBranch: String, postAction: ((JSONObject) -> Unit)? = null) {
        // 🔒 B558 — একাধিক ঘরের সব ওষুধ কমা দিয়ে জুড়ে একটাই product।
        val product = collectMedicines()
        val bill = etBill.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        val deposit = etDeposit.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
        val vmsg = FieldError.validate(listOf<Triple<View, Boolean, String>>(
            Triple(etProduct, product.isNotBlank(), "Product দিন"),
            Triple(etBill, bill > 0, "সঠিক Bill দিন"),
            Triple(etDeposit, deposit > 0, "সঠিক Deposit দিন")
        ))
        if (vmsg != null) {
            Toast.makeText(this, vmsg, Toast.LENGTH_SHORT).show()
            return
        }
        if (deposit > bill) {
            AlertDialog.Builder(this)
                .setCustomTitle(PremiumAlert.header(this, "⚠️ Overpaying"))
                .setMessage("Deposit is more than bill. Continue?")
                .setPositiveButton("Continue") { _, _ -> commit(staffMobile, product, bill, deposit, postAction) }
                .setNegativeButton("Cancel", null)
                .show().also { PremiumAlert.paint(it) }
            return
        }
        commit(staffMobile, product, bill, deposit, postAction)
    }

    private fun commit(staffMobile: String, product: String, bill: Double, deposit: Double, postAction: ((JSONObject) -> Unit)? = null) {
        val branch = selectedBranch
        val customer = etCustomer.text?.toString()?.trim().orEmpty().ifBlank { "Walk-in" }
val mode = selectedMpMode
        val row = JSONObject()
            .put("id", "prd_" + UUID.randomUUID().toString().replace("-", ""))
            .put("kind", selectedSaleType)   // 🆕 V805 — ওষুধ না স্যালাইন
            .put("customer", customer)
            .put("product", product)
            .put("bill", bill)
            .put("total", bill)
            .put("deposit", deposit)
            .put("due", max(0.0, bill - deposit))
            .put("mode", mode)
            .put("remarks", "")                          // 🔄 V849 — Remarks ঘরটাই আর নেই
            .put("date", actualDate())                   // 🔄 V849 — প্রকৃত জমার তারিখ
            .put("branch", branch)
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", isoNow())
            .put("updatedAt", isoNow())
        progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
        mpSaving = true
        lifecycleScope.launch {
            val ok = try {
                withContext(Dispatchers.IO) { SupabaseClient.upsert("products", row) }
            } finally { mpSaving = false }
            progressLoad.visibility = View.GONE
            // 🔒 TK'S PERMANENT RULE (28.07.2026, khata row B25): what THIS
            // phone saved must show on THIS phone straight away. Noted down
            // only once the cloud has ACCEPTED the sale -- on a failure the
            // form deliberately stays filled so the staff can save again, and
            // noting a failed row here could then leave two sales on screen.
            // Display only: the sending itself is completely unchanged.
            if (ok) {
                try {
                    MyPhoneWrites.remember(
                        this@MedicinePaymentActivity, MP_TABLE, row.optString("id"), row
                    )
                } catch (_: Throwable) { }
            }
            Toast.makeText(
                this@MedicinePaymentActivity,
                if (ok) "Medicine payment saved" else "Saved failed — check connection",
                Toast.LENGTH_SHORT
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            if (ok) {
                // Share/Print-এর জন্য ফর্ম খালি করার আগেই সেভ-হওয়া row হাতে
                // ধরিয়ে দিই — তখন WhatsApp টেক্সট/প্রিন্ট রসিদ ওই row থেকেই বানে।
                postAction?.invoke(row)
                clearMedicineRows()
                etBill.setText(""); etDeposit.setText(""); etCustomer.setText("")
                resetActualDate()                        // 🔄 V849
                customerSuggestions.removeAllViews()
                loadHistory(selectedBranch)   // 🔴 V804
            }
        }
    }

        private var firstResume = true
    override fun onResume() {
        super.onResume()
        if (firstResume) { firstResume = false; return }
        loadHistory(selectedBranch)   // 🔴 V804
    }

    // TK-REQUESTED ADDITION (2026-07-20): same "show what was already on the
    // phone instantly" pattern added to other screens today. Raw JSONArray
    // is cached directly (no data-class mapping needed here since the rows
    // are already read as plain JSONObject) -- the fetch/filter logic below
    // is otherwise completely unchanged.
    /** Note-book key for this screen's rows. The table is shared ("products"),
     *  so the kind is part of the key -- a medicine sale can never mix with
     *  any other kind of product row. */
    private val MP_TABLE = "products_medicinePayment"

    private fun loadCachedHistory(): org.json.JSONArray? {
        val json = getSharedPreferences("medicine_payment_history_cache", MODE_PRIVATE).getString("cache", null) ?: return null
        // 🔒 TK'S PERMANENT RULE (28.07.2026, khata row B25): the stored list is
        // what stays on screen on a weak line, so a sale just taken on THIS
        // phone used to be missing from it. This phone's own rows are put back
        // on top; each note retires itself once the cloud copy is equally new.
        return try { MyPhoneWrites.overlay(this, MP_TABLE, org.json.JSONArray(json), addNewAtTop = true) } catch (t: Throwable) { null }
    }

    private fun saveCachedHistory(rows: org.json.JSONArray) {
        try {
            getSharedPreferences("medicine_payment_history_cache", MODE_PRIVATE).edit().putString("cache", rows.toString()).apply()
        } catch (_: Throwable) { }
    }

    // 🔒 B560 — নামানো পূর্ণ তালিকা মনে রাখি, তারপর ব্রাঞ্চ + তারিখ + সার্চ
    // (সব client-side) প্রয়োগ করে দেখাই। বাড়তি কোনো সার্ভার-কল নেই।
    private fun showRows(rows: org.json.JSONArray, userBranch: String) {
        loadedRows = rows
        userBranchFilter = userBranch
        applyFilters()
    }

    /** History-র সব ছাঁকা এক জায়গায়: ব্রাঞ্চ → তারিখ-চিপ → সার্চ; তারপর
     *  কার্ড আঁকা ও উপরে মোট বিক্রি বসানো। */
    private fun applyFilters() {
        val today = today()
        val start7 = dateNDaysAgo(6)
        val start30 = dateNDaysAgo(29)
        val q = searchQuery.trim().lowercase()

        /* 🔴🔒 V509 (TK-রিপোর্ট ২১.০৮.২০২৬ — "স্ক্রিন কম্পন দিচ্ছে"):
           এই তালিকা আগে **দুবার** আঁকত (প্রথমে ফোনে জমানো, তারপর ক্লাউড থেকে
           এসে আবার) — বেশিরভাগ সময় দুটো হুবহু এক, তাই দ্বিতীয়বার মুছে-আঁকাটাই
           চোখে ঝিলিক লাগত।
           ⛔ চিহ্নে **ছাঁকনির সব অবস্থাও** ধরা আছে (ব্রাঞ্চ · তারিখ · বাছাই
              তারিখ · খোঁজা লেখা) — তাই ছাঁকনি বদলালে আগের মতোই পুরো আঁকে,
              কিছু আটকে থাকে না। */
        if (com.tkbiswas.pilesclinic.native.RedrawGuard.alreadyShowing(
                historyContainer,
                loadedRows.toString() + "|" + userBranchFilter + "|" +
                    activeDateFilter + "|" + (pickedDate ?: "") + "|" + q +
                    "|" + dueOnly +
                    "|" + (rangeFrom ?: "") + "|" + (rangeTo ?: ""))) return   // 🆕 V846 · V847

        rebuildSettledMap()                                       // 🆕 V846
        historyContainer.removeAllViews()
        var shown = 0
        var totalCollected = 0.0
        var cashCollected = 0.0
        var onlineCollected = 0.0
        var outstanding = 0.0                                     // 🆕 V846

        for (i in 0 until loadedRows.length()) {
            val r = loadedRows.optJSONObject(i) ?: continue
            val br = r.s("branch")
            if (userBranchFilter != "All" && br.isNotBlank() && br != userBranchFilter) continue

            /* 🆕 V846 — "Due Only" চললে তারিখ দেখা হয় না (বাকি টাকা পুরনো
               দিনেরও হতে পারে); বদলে শুধু যাদের এখনো বাকি আছে তারাই আসে। */
            /* 🔍🔒 V985 (০২.০৯.২০২৬, TK-নির্দেশ: *"কোন পেশেন্টের কত মেডিসিনের
               বিল হলো সেটা যেন সার্চ করার অপশন থাকে"*)।
               **আসল কারণ (কোড ধরে):** খোঁজাটা তারিখ-ছাঁকার **পরে** চলত, আর
               শুরুতে "Today" বাছা থাকে — তাই পুরনো দিনের রোগী খুঁজলে কিছুই
               আসত না। এখন কিছু লেখা থাকলে **সব তারিখ** দেখানো হয়।
               ⛔ কিছু না লিখলে হুবহু আগের আচরণ (Today/7/30/Pick/Statement)। */
            if (dueOnly) {
                if (netDueOf(r) <= 0.0) continue
            } else if (q.isNotEmpty()) {
                // খোঁজার সময় তারিখ ধরা হয় না — সব দিনের বিল একসাথে।
            } else {
                // তারিখ-ঘর: সঞ্চিত "date" (yyyy-MM-dd) দিয়ে তুলনা (স্ট্রিং তুলনা নিরাপদ)।
                val d = r.s("date")
                val dateOk = when (activeDateFilter) {
                    "today" -> d == today
                    "7" -> d.isNotBlank() && d >= start7
                    "30" -> d.isNotBlank() && d >= start30
                    "pick" -> pickedDate != null && d == pickedDate
                    // 🆕 V847 — Statement: From ≤ তারিখ ≤ To (দুই প্রান্তই ধরা)
                    "range" -> rangeFrom != null && rangeTo != null &&
                        d.isNotBlank() && d >= rangeFrom!! && d <= rangeTo!!
                    else -> true
                }
                if (!dateOk) continue
            }

            if (q.isNotEmpty()) {
                val hay = (r.s("customer") + " " + r.s("product") + " " + r.s("mobile") + " " + r.s("receivedBy")).lowercase()
                if (!hay.contains(q)) continue
            }

            historyContainer.addView(historyCard(r))
            shown++
            val dep = r.optDouble("deposit", 0.0)
            totalCollected += dep
            if (r.s("mode") == "ONLINE") onlineCollected += dep else cashCollected += dep
            outstanding += netDueOf(r)                            // 🆕 V846
        }

        if (dueOnly) renderDueTotal(shown, outstanding)           // 🆕 V846
        else renderTotal(shown, totalCollected, cashCollected, onlineCollected)
        renderChips()
        if (shown == 0) {
            tvEmpty.text = when {
                loadedRows.length() == 0 -> "No medicine / saline payment yet."
                dueOnly -> "No due pending. All cleared."          // 🆕 V846
                else -> "No sale in this filter."
            }
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    private fun loadHistory(userBranch: String) {
        var hadCache = false
        val cached = loadCachedHistory()
        if (cached != null && cached.length() > 0) {
            hadCache = true
            progressLoad.visibility = View.GONE
            showRows(cached, userBranch)
        } else {
            progressLoad.visibility = View.GONE  // TK-REQUESTED (2026-07-20): spinner must NEVER spin anywhere; cache-first shows old data instantly, content appears when ready.
            // TK-REQUESTED (2026-07-24): only reachable on first-ever open
            // (no cache yet) -- plain "Loading..." instead of blank.
            tvEmpty.text = "Loading..."
            tvEmpty.visibility = View.VISIBLE
            historyContainer.removeAllViews()
        }
        lifecycleScope.launch {
            // TK-REQUESTED FIX (2026-07-20): this had no error handling --
            // needed so a fetch failure after cached data was shown leaves
            // that cached data on screen instead of crashing/going blank.
            // TK-REPORTED BUG FIX (2026-07-23): this try/catch never
            // actually caught a network failure -- SupabaseClient.fetchList()
            // swallows its own exceptions internally and returns an empty
            // (non-null) JSONArray, so "rows" was never null on a failed
            // fetch, meaning the code below still ran saveCachedHistory()
            // with a wrongly-empty result, poisoning the cache. Switched to
            // fetchListOrNull(), which actually returns null on a genuine
            // failure, so that case is now correctly detected and the
            // cache is left untouched instead of being overwritten with
            // wrong empty data.
            val rows = try {
                withContext(Dispatchers.IO) {
                    /* 🆕🔒 V805 — এখন দুই রকমের সারিই আসে (ওষুধ + স্যালাইন)।
                       ⛔ `in.(...)` PostgREST-এর নিজের লেখা, `eq.`-এর মতোই নিরাপদ;
                          পুরনো সব সারি `medicinePayment` বলে একটাও বাদ যায় না। */
                    SupabaseClient.fetchListOrNull(
                        "products", "kind=in.($KIND_MEDICINE,$KIND_SALINE)", 500)
                }
            } catch (t: Throwable) {
                null
            }
            progressLoad.visibility = View.GONE
            if (rows == null) {
                if (!hadCache) {
                    tvEmpty.text = "No medicine / saline payment yet."
                    tvEmpty.visibility = View.VISIBLE
                }
                return@launch
            }
            saveCachedHistory(rows)
            // The cache above is saved from the UNTOUCHED cloud rows; only what
            // is shown gets this phone's own sales put on top (same rule as
            // loadCachedHistory).
            showRows(
                try { MyPhoneWrites.overlay(this@MedicinePaymentActivity, MP_TABLE, rows, addNewAtTop = true) } catch (_: Throwable) { rows },
                userBranch
            )
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    /** 🔒 B560 (08.08.2026, TK-অনুমোদিত প্রুফ) — প্রফেশনাল History কার্ড:
     *  রোগীর নাম + মোড-ব্যাজ + কালেক্ট টাকা, ওষুধ, 🏥 ব্রাঞ্চ · 👤 কোন স্টাফ নিল
     *  · ⏰ তারিখ-সময়, নিচে Bill/Deposit/Due। কোনো ডেটা বদলায় না — শুধু দেখানো। */
    private fun historyCard(r: JSONObject): View {
        val bill = r.optDouble("bill", r.optDouble("total", 0.0))
        val deposit = r.optDouble("deposit", 0.0)
        val due = max(0.0, bill - deposit)
        val mode = r.s("mode").ifBlank { "CASH" }
        val cust = r.s("customer").ifBlank { r.s("mobile").ifBlank { "Walk-in" } }
        val meds = r.s("product").ifBlank { "—" }
        val branch = r.s("branch").ifBlank { "—" }
        val staffMobile = r.s("receivedBy").ifBlank { r.s("createdBy") }
        val staffName = StaffDirectory.findAccount(staffMobile)?.name?.takeIf { it.isNotBlank() }
            ?: staffMobile.ifBlank { "—" }
        val whenStr = r.s("createdAt").let { if (it.isNotBlank()) DateUtil.displayWithTime(it) else DateUtil.display(r.s("date")) }

        val navy = 0xFF0B2B59.toInt()
        val green = 0xFF0B7A34.toInt()
        val grey = 0xFF475467.toInt()
        val faint = 0xFF8A949E.toInt()

        // বাইরের সবুজ প্রান্ত + ভেতরে সাদা রাউন্ড কার্ড
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = android.graphics.drawable.GradientDrawable().apply {
                // 🆕 V805 — স্যালাইন হলে বাঁ দিকের দাগটা নীল, ওষুধ হলে আগের মতোই সবুজ
                cornerRadius = dp(10).toFloat()
                setColor(if (r.s("kind") == KIND_SALINE) 0xFF1167D8.toInt() else green)
            }
            setPadding(dp(4), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(9).toFloat(); setColor(0xFFFFFFFF.toInt())
                setStroke(dp(1), 0xFFE6ECF3.toInt())
            }
            setPadding(dp(11), dp(9), dp(11), dp(9))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // সারি ১: নাম + মোড-ব্যাজ  ......  কালেক্ট টাকা
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(TextView(this).apply {
            text = cust; textSize = 13.5f; setTextColor(navy)
            setTypeface(typeface, android.graphics.Typeface.BOLD); maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        left.addView(modeBadge(mode))
        row1.addView(left)
        row1.addView(TextView(this).apply {
            text = "₹ ${fmt(deposit)}"; textSize = 13.5f; setTextColor(green)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        card.addView(row1)

        // সারি ২: ওষুধ
        card.addView(TextView(this).apply {
            /* 🆕 V805 — স্যালাইনের সারিতে আলাদা আইকন, যাতে তালিকায় এক নজরেই
               বোঝা যায় কোনটা ওষুধ আর কোনটা স্যালাইন। ⛔ ওষুধের সারি হুবহু আগের মতোই। */
            val isSaline = r.s("kind") == KIND_SALINE
            text = (if (isSaline) "💧 " else "💊 ") + meds; textSize = 12f; setTextColor(grey)
            setPadding(0, dp(3), 0, 0)
        })

        // সারি ৩: 🏥 ব্রাঞ্চ · 👤 কোন স্টাফ · ⏰ তারিখ-সময়
        // ⛔ B313/B202 শিক্ষা: ⏰/🕐 ইমোজি ফোনের ফন্ট নিজে "Jul 17"/"1:00" এঁকে দেয়
        //    (আসল ডেটা নয়) — তাই সময়ের আইকনে প্রমাণিত-নিরাপদ ⏰ (কোনো সংখ্যা আঁকা নেই)।
        card.addView(TextView(this).apply {
            text = "🏥 $branch    👤 $staffName    ⏰ $whenStr"
            textSize = 10.5f; setTextColor(faint); setPadding(0, dp(3), 0, 0)
        })

        // বিভাজক
        card.addView(View(this).apply {
            setBackgroundColor(0xFFE6ECF3.toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                .apply { setMargins(0, dp(6), 0, dp(5)) }
        })

        /* সারি ৪: Bill / Deposit / Due
           🆕🔒 V846 — এখানে দেখানো "Due" এখন **এই মুহূর্তের** বাকি: পরে যত টাকা
           জমা নেওয়া হয়েছে সেটা বাদ দিয়ে। Bill আর Deposit ওই দিনের নিজের অঙ্কই
           থাকে (বদলানো হয়নি), তাই পুরনো কাগজের সঙ্গে মিলবে। */
        val settledLater = settledById[r.s("id")] ?: 0.0
        val liveDue = netDueOf(r)
        card.addView(TextView(this).apply {
            text = "Bill ₹${fmt(bill)}  ·  Deposit ₹${fmt(deposit)}  ·  Due ₹${fmt(if (isDueSettlementId(r.s("id"))) due else liveDue)}"
            textSize = 11.5f; setTextColor(0xFF10223A.toInt())
        })
        if (settledLater > 0.0) {
            card.addView(TextView(this).apply {
                text = "Due collected later: ₹${fmt(settledLater)}"
                textSize = 10.5f; setTextColor(green); setPadding(0, dp(2), 0, 0)
            })
        }
        // 🆕 V846 — বাকি থাকলে তবেই বোতাম; শোধ হয়ে গেলে নিজে থেকেই উধাও।
        if (liveDue > 0.0) card.addView(collectDueButton(r, liveDue))

        outer.addView(card)
        return outer
    }

    /* ══════════════ 🆕🔒 V846 — বাকি টাকা (Due) ══════════════
       TK (৩০.০৮.২০২৬): "বিল হয়েছে ২০০০ টাকা কিন্তু তিনি আজকে ১২০০ টাকা দিয়ে
       গেলেন, তাহলে বাকি থাকলো ৮০০ টাকা — এই বাকি টাকাটা কোথায় শো করবে?"
       ⛔ পুরনো একটাও সারি বদলানো হয় না · নতুন কোনো ডেটাবেস-ঘর/SQL লাগে না। */

    /** পুরনো বিক্রির id → পরে জমা-নেওয়া মোট টাকা। প্রতিবার তালিকা আঁকার
     *  আগে নতুন করে গোনা হয়, তাই কখনো বাসি থাকে না। */
    private val settledById = HashMap<String, Double>()

    private fun rebuildSettledMap() {
        settledById.clear()
        for (i in 0 until loadedRows.length()) {
            val r = loadedRows.optJSONObject(i) ?: continue
            val origId = settledSaleIdOf(r.s("id"))
            if (origId.isBlank()) continue
            settledById[origId] = (settledById[origId] ?: 0.0) + r.optDouble("deposit", 0.0)
        }
    }

    /** এখনো কত বাকি: বিল − ওই দিনের জমা − পরে জমা-নেওয়া টাকা।
     *  বাকি-জমার সারিটার নিজের কোনো বাকি নেই (bill = 0)। */
    private fun netDueOf(r: JSONObject): Double {
        val id = r.s("id")
        if (isDueSettlementId(id)) return 0.0
        val bill = r.optDouble("bill", r.optDouble("total", 0.0))
        val paid = r.optDouble("deposit", 0.0) + (settledById[id] ?: 0.0)
        return max(0.0, bill - paid)
    }

    private fun renderDueTotal(count: Int, outstanding: Double) {
        val sb = android.text.SpannableStringBuilder()
        sb.append("TOTAL OUTSTANDING DUE").append("\n")
        val s2 = sb.length
        sb.append("₹ ${fmt(outstanding)}   ·   $count ${if (count == 1) "sale" else "sales"}")
        sb.setSpan(android.text.style.RelativeSizeSpan(1.6f), s2, sb.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), s2, sb.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n").append("All dates · tap a card's button to collect")
        tvHistoryTotal.text = sb
    }

    /** "💰 COLLECT DUE" বোতাম — শুধু যে সারিতে সত্যিই বাকি আছে সেখানে। */
    private fun collectDueButton(r: JSONObject, remaining: Double): TextView {
        return TextView(this).apply {
            text = "💰 COLLECT DUE ₹${fmt(remaining)}"
            textSize = 11.5f
            gravity = android.view.Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat(); setColor(0xFFC62828.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(7), 0, 0) }
            setOnClickListener { openCollectDue(r, remaining) }
        }
    }

    /** এই মুহূর্তে একটাই বাকি-জমা যাতে পাঠানো যায় (দুবার চাপলে দুবার টাকা নয়)। */
    private var dueSaving = false

    private fun openCollectDue(r: JSONObject, remaining: Double) {
        if (dueSaving) return
        val origId = r.s("id")
        if (origId.isBlank()) {
            Toast.makeText(this, "This sale has no id — cannot collect", Toast.LENGTH_SHORT).show()
            return
        }
        val cust = r.s("customer").ifBlank { "Walk-in" }

        val pad = dp(16)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, dp(10), pad, 0)
        }
        box.addView(TextView(this).apply {
            text = "$cust  ·  Remaining ₹${fmt(remaining)}"
            textSize = 12.5f; setTextColor(0xFF0B2B59.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        // ⛔ §৯.১৭ (B411): একা TYPE_CLASS_NUMBER নয় — TEXT + DigitsKeyListener,
        //    নইলে কিছু ফোনে সংখ্যার কীবোর্ডই খোলে না।
        val etAmt = EditText(this).apply {
            hint = "Amount"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
            setText(fmt(remaining).replace(",", ""))
            setSelection(text?.length ?: 0)
        }
        box.addView(etAmt)

        var mode = "CASH"
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(8), 0, 0) }
        }
        val blue = 0xFF1167D8.toInt()
        val cashBtn = TextView(this)
        val onlineBtn = TextView(this)
        fun paintModes() {
            for ((b, on) in listOf(cashBtn to (mode == "CASH"), onlineBtn to (mode == "ONLINE"))) {
                b.setTextColor(if (on) 0xFFFFFFFF.toInt() else blue)
                b.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(if (on) blue else 0xFFE8F2FF.toInt())
                }
            }
        }
        for ((b, label) in listOf(cashBtn to "CASH", onlineBtn to "ONLINE")) {
            b.text = label; b.textSize = 12f; b.gravity = android.view.Gravity.CENTER
            b.setTypeface(b.typeface, android.graphics.Typeface.BOLD)
            b.setPadding(dp(10), dp(8), dp(10), dp(8))
            b.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { setMargins(dp(3), 0, dp(3), 0) }
            b.setOnClickListener { mode = label; paintModes() }
            row.addView(b)
        }
        paintModes()
        box.addView(row)

        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "💰 Collect Due"))
            .setView(box)
            .setPositiveButton("Collect") { _, _ ->
                val amt = etAmt.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
                when {
                    amt <= 0.0 -> Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show()
                    amt > remaining -> Toast.makeText(this, "More than the remaining due", Toast.LENGTH_SHORT).show()
                    else -> saveDueCollection(r, amt, mode)
                }
            }
            .setNegativeButton("Cancel", null)
            .show().also {
                PremiumAlert.paint(it)
                try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { }   // 🤫 V774
            }
    }

    /** আজকের তারিখে একটা নতুন সারি বসে — পুরনো সারিতে একটুও হাত পড়ে না,
     *  তাই পুরনো দিনের কালেকশন/ছাপানো রেজিস্টার হুবহু আগের মতোই থাকে, আর
     *  আজ হাতে-পাওয়া টাকাটা আজকের কালেকশনেই ওঠে। */
    private fun saveDueCollection(orig: JSONObject, amount: Double, mode: String) {
        if (dueSaving) return
        /* 🔒 V853 (নিজের যাচাইয়ে যোগ করা শেষ পাহারা — টাকার কাজ) — পপ-আপ খোলার
           পর তালিকা নতুন করে এসে থাকলে বাকির অঙ্ক বদলে যেতে পারে। লেখার ঠিক
           আগে আরেকবার মেপে নিই, যাতে বাকির চেয়ে বেশি টাকা কখনো বসতে না পারে।
           ⛔ ওয়েবেও ঠিক এই যাচাইটাই আছে (medSaveDueCollection)। */
        rebuildSettledMap()
        val liveNow = netDueOf(orig)
        if (amount > liveNow) {
            Toast.makeText(this, "More than the remaining due", Toast.LENGTH_SHORT).show()
            applyFilters()
            return
        }
        val user = NativeSession.current(this)
        val staffMobile = user?.mobile ?: ""
        val origId = orig.s("id")
        val newRow = JSONObject()
            .put("id", DUE_ID_PREFIX + origId + "_" + System.currentTimeMillis())
            .put("kind", orig.s("kind").ifBlank { KIND_MEDICINE })
            .put("customer", orig.s("customer").ifBlank { "Walk-in" })
            .put("product", "DUE PAYMENT — " + orig.s("product").ifBlank { "—" })
            .put("bill", 0.0)
            .put("total", 0.0)
            .put("deposit", amount)
            .put("due", 0.0)
            .put("mode", mode)
            .put("remarks", "Due collection for sale dated " + DateUtil.display(orig.s("date")))
            .put("date", today())
            .put("branch", orig.s("branch").ifBlank { selectedBranch })
            .put("receivedBy", staffMobile)
            .put("createdBy", staffMobile)
            .put("createdAt", isoNow())
            .put("updatedAt", isoNow())

        dueSaving = true
        lifecycleScope.launch {
            val ok = try {
                withContext(Dispatchers.IO) { SupabaseClient.upsert("products", newRow) }
            } finally { dueSaving = false }
            if (ok) {
                try { MyPhoneWrites.remember(this@MedicinePaymentActivity, MP_TABLE, newRow.optString("id"), newRow) } catch (_: Throwable) { }
            }
            Toast.makeText(
                this@MedicinePaymentActivity,
                if (ok) "Due collected ₹${fmt(amount)}" else "Save failed — check connection",
                Toast.LENGTH_SHORT
            ).show().also { try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            if (ok) loadHistory(selectedBranch)
        }
    }

    private fun modeBadge(mode: String): TextView {
        val online = mode == "ONLINE"
        val bg = if (online) 0xFFE8F0FB.toInt() else 0xFFE6F4EA.toInt()
        val fg = if (online) 0xFF1F6FE0.toInt() else 0xFF0B7A34.toInt()
        return TextView(this).apply {
            text = if (online) "ONLINE" else "CASH"
            textSize = 8.5f; setTextColor(fg)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(6), dp(1), dp(6), dp(1))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat(); setColor(bg)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dp(5), 0, 0, 0) }
        }
    }

    // ───────────────────── History নিয়ন্ত্রণ (সার্চ + চিপ + মোট) ─────────────────────
    // 🔒 B560 — সব ছাঁকা আগে-নামানো ডেটার উপরেই; Supabase-এ বাড়তি কল নেই।
    private fun setupHistoryControls() {
        etHistorySearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                searchQuery = s?.toString().orEmpty()
                applyFilters()
            }
        })
        chipToday.setOnClickListener { activeDateFilter = "today"; applyFilters() }
        chip7.setOnClickListener { activeDateFilter = "7"; applyFilters() }
        chip30.setOnClickListener { activeDateFilter = "30"; applyFilters() }
        chipPick.setOnClickListener { openDatePicker() }
        chipStatement.setOnClickListener { openStatementRange() }   // 🆕 V847
        // 🆕 V846 — টগল: চাপলে শুধু বাকি, আবার চাপলে আগের তারিখ-তালিকা।
        chipDue.setOnClickListener { dueOnly = !dueOnly; applyFilters() }
        renderChips()
    }

    private fun openDatePicker() {
        val cal = java.util.Calendar.getInstance()
        pickedDate?.let {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)
                if (d != null) cal.time = d
            } catch (_: Exception) {}
        }
        android.app.DatePickerDialog(
            this,
            { _, y, m, day ->
                val c = java.util.Calendar.getInstance().apply { set(y, m, day) }
                pickedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
                activeDateFilter = "pick"
                applyFilters()
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }

    /** 🆕🔒 V847 — Statement: আগে From তারিখ, তারপর To তারিখ। দুটোই বাছা হলে
     *  তবেই তালিকা বদলায় — মাঝপথে ছেড়ে দিলে আগের ছাঁকনি হুবহু আগের মতোই থাকে। */
    private fun openStatementRange() {
        pickOneDate("Statement — From", rangeFrom) { from ->
            pickOneDate("Statement — To", rangeTo ?: from) { to ->
                // উল্টো বাছলে নিজে থেকেই ঠিক করে নেয় (ভুল ছাঁকনি হতে পারবে না)
                val a = if (from <= to) from else to
                val b = if (from <= to) to else from
                rangeFrom = a; rangeTo = b
                activeDateFilter = "range"
                dueOnly = false
                applyFilters()
            }
        }
    }

    private fun pickOneDate(title: String, preset: String?, onPicked: (String) -> Unit) {
        val cal = java.util.Calendar.getInstance()
        preset?.let {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)
                if (d != null) cal.time = d
            } catch (_: Exception) {}
        }
        val dlg = android.app.DatePickerDialog(
            this,
            { _, y, m, day ->
                val c = java.util.Calendar.getInstance().apply { set(y, m, day) }
                onPicked(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time))
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
        dlg.setTitle(title)
        // বিক্রি ভবিষ্যতে হয় না — আগামী তারিখ বাছা বন্ধ (ওয়েবেও max=today)
        try { dlg.datePicker.maxDate = System.currentTimeMillis() } catch (_: Throwable) { }
        dlg.show()
    }

    private fun renderChips() {
        val green = 0xFF0B7A34.toInt()
        /* 🔒 V848 — background বদলালে Android ভিউয়ের padding মুছে দিতে পারে;
           চিপগুলো এখন এক সারিতে স্ক্রল করে বলে padding হারালে ওগুলো চেপ্টা
           দেখাত। তাই আগের padding মেপে রেখে আবার বসানো হয়। */
        fun paint(chip: TextView, on: Boolean) {
            val pl = chip.paddingLeft; val pt = chip.paddingTop
            val pr = chip.paddingRight; val pb = chip.paddingBottom
            if (on) {
                chip.background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(16).toFloat(); setColor(green)
                }
                chip.setTextColor(0xFFFFFFFF.toInt())
            } else {
                chip.setBackgroundResource(R.drawable.bg_price_chip)
                chip.setTextColor(green)
            }
            chip.setPadding(pl, pt, pr, pb)
        }
        paint(chipToday, activeDateFilter == "today")
        paint(chip7, activeDateFilter == "7")
        paint(chip30, activeDateFilter == "30")
        paint(chipPick, activeDateFilter == "pick")
        paint(chipStatement, activeDateFilter == "range")           // 🆕 V847
        chipStatement.text = if (activeDateFilter == "range" && rangeFrom != null && rangeTo != null)
            DateUtil.display(rangeFrom) + " – " + DateUtil.display(rangeTo) else "Statement"
        // 🆕 V846 — বাকি-চিপ চালু থাকলে লাল, যাতে তারিখ-চিপের সঙ্গে না মেলে।
        val dl = chipDue.paddingLeft; val dt = chipDue.paddingTop
        val dr = chipDue.paddingRight; val db = chipDue.paddingBottom
        if (dueOnly) {
            chipDue.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(16).toFloat(); setColor(0xFFC62828.toInt())
            }
            chipDue.setTextColor(0xFFFFFFFF.toInt())
        } else {
            chipDue.setBackgroundResource(R.drawable.bg_price_chip)
            chipDue.setTextColor(0xFFC62828.toInt())
        }
        chipDue.setPadding(dl, dt, dr, db)
        // ⛔ B313/B202: ⏰ ইমোজি ফোনের ফন্টে "Jul 17" এঁকে দেয় (বিভ্রান্তিকর) —
        //    তাই এখানে ইমোজি ছাড়া পরিষ্কার লেখা; তারিখ বাছলে আসল তারিখ দেখায়।
        chipPick.text = if (activeDateFilter == "pick" && pickedDate != null)
            DateUtil.display(pickedDate) else "Pick Date"
    }

    private fun renderTotal(count: Int, total: Double, cash: Double, online: Double) {
        val label = when (activeDateFilter) {
            "today" -> "TODAY'S MEDICINE SALE"
            "7" -> "LAST 7 DAYS SALE"
            "30" -> "LAST 30 DAYS SALE"
            "pick" -> "SALE ON ${DateUtil.display(pickedDate)}"
            // 🆕 V847
            "range" -> "STATEMENT ${DateUtil.display(rangeFrom)} – ${DateUtil.display(rangeTo)}"
            else -> "MEDICINE SALE"
        }
        val line2 = "₹ ${fmt(total)}   ·   $count ${if (count == 1) "sale" else "sales"}"
        val line3 = "Cash ₹${fmt(cash)} · Online ₹${fmt(online)}"
        val sb = android.text.SpannableStringBuilder()
        sb.append(label).append("\n")
        val s2 = sb.length
        sb.append(line2)
        sb.setSpan(android.text.style.RelativeSizeSpan(1.6f), s2, sb.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), s2, sb.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        sb.append("\n").append(line3)
        tvHistoryTotal.text = sb
    }

    private fun dateNDaysAgo(n: Int): String {
        val c = java.util.Calendar.getInstance()
        c.add(java.util.Calendar.DAY_OF_MONTH, -n)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    // ───────────────────── Share / Print রসিদ ─────────────────────
    // 🔒 B559 — Share/Print চাপার আগে অটো-সেভ হয়ে গেছে; এখানে সেভ-হওয়া row
    // থেকেই রসিদ বানাই। কোনো নতুন ডেটা লেখা হয় না — শুধু পাঠানো/ছাপানো।
    private fun shareReceipt(r: JSONObject) {
        WhatsAppMessageChooser.sendGeneric(this, receiptText(r))
    }

    private var printWebView: android.webkit.WebView? = null
    private fun printReceipt(r: JSONObject) {
        val html = receiptHtml(r)
        try {
            val wv = android.webkit.WebView(this)
            wv.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                    try {
                        val pm = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val adapter = view.createPrintDocumentAdapter("MedicinePayment")
                        pm.print("Medicine Payment", adapter, PrintQuality.builder().build())
                    } catch (_: Throwable) {
                        Toast.makeText(this@MedicinePaymentActivity, "Print not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            printWebView = wv
            wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        } catch (_: Throwable) {
            Toast.makeText(this, "Print not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun receiptText(r: JSONObject): String {
        val b = com.tkbiswas.pilesclinic.print.BranchCatalog.byName(r.s("branch"))
        val bill = r.optDouble("bill", r.optDouble("total", 0.0))
        val deposit = r.optDouble("deposit", 0.0)
        val due = max(0.0, bill - deposit)
        val cust = r.s("customer").ifBlank { "Walk-in" }
        val meds = r.s("product").ifBlank { "-" }
        val staffMobile = r.s("receivedBy").ifBlank { r.s("createdBy") }
        val staffName = StaffDirectory.findAccount(staffMobile)?.name?.takeIf { it.isNotBlank() } ?: staffMobile
        val whenStr = r.s("createdAt").let { if (it.isNotBlank()) DateUtil.displayWithTime(it) else DateUtil.display(r.s("date")) }
        val mode = r.s("mode").ifBlank { "CASH" }
        return buildString {
            append("*${b.clinicName}*\n")
            append("${b.addressLine}\n")
            append("Ph: ${b.phoneLine}  |  Helpline: ${com.tkbiswas.pilesclinic.print.BranchCatalog.HELPLINE}\n")
            append("--------------------------------\n")
            append("*MEDICINE PAYMENT RECEIPT*\n\n")
            append("Patient  : $cust\n")
            append("Medicine : $meds\n")
            append("Bill     : ₹${fmt(bill)}\n")
            append("Paid     : ₹${fmt(deposit)}  ($mode)\n")
            append("Due      : ₹${fmt(due)}\n\n")
            append("Date     : $whenStr\n")
            append("Branch   : ${b.displayName}\n")
            append("Received by: $staffName\n\n")
            append("Thank you. Get well soon.")
        }
    }

    private fun receiptHtml(r: JSONObject): String {
        val b = com.tkbiswas.pilesclinic.print.BranchCatalog.byName(r.s("branch"))
        val bill = r.optDouble("bill", r.optDouble("total", 0.0))
        val deposit = r.optDouble("deposit", 0.0)
        val due = max(0.0, bill - deposit)
        val cust = esc(r.s("customer").ifBlank { "Walk-in" })
        val meds = esc(r.s("product").ifBlank { "-" })
        val staffMobile = r.s("receivedBy").ifBlank { r.s("createdBy") }
        val staffName = esc(StaffDirectory.findAccount(staffMobile)?.name?.takeIf { it.isNotBlank() } ?: staffMobile)
        val whenStr = esc(r.s("createdAt").let { if (it.isNotBlank()) DateUtil.displayWithTime(it) else DateUtil.display(r.s("date")) })
        val mode = esc(r.s("mode").ifBlank { "CASH" })
        return """
<!DOCTYPE html><html><head><meta charset="utf-8">
<style>
@page{size:A4;margin:14mm}
*{box-sizing:border-box;margin:0;padding:0;font-family:Arial,sans-serif}
body{color:#111;font-size:13px}
.h{text-align:center;border-bottom:2px solid #0B2B59;padding-bottom:8px;margin-bottom:12px}
.cn{font-size:20px;font-weight:800;color:#0B2B59}
.ad{font-size:12px;color:#555;margin-top:3px}
.tt{text-align:center;font-size:14px;font-weight:800;color:#0B7A34;margin:6px 0 14px;letter-spacing:1px}
table{width:100%;border-collapse:collapse}
td{padding:7px 4px;border-bottom:1px solid #eee;font-size:13.5px}
td.k{color:#555;width:38%}
td.v{font-weight:700;color:#10223A}
.amt{font-size:15px}
.due{color:#b0392b}
.ft{margin-top:22px;font-size:12px;color:#555;display:flex;justify-content:space-between}
.ty{text-align:center;margin-top:26px;font-size:13px;color:#0B7A34;font-weight:700}
</style></head><body>
<div class="h"><div class="cn">${esc(b.clinicName)}</div><div class="ad">${esc(b.addressLine)} &nbsp;·&nbsp; Ph: ${esc(b.phoneLine)} &nbsp;·&nbsp; Helpline: ${esc(com.tkbiswas.pilesclinic.print.BranchCatalog.HELPLINE)}</div></div>
<div class="tt">MEDICINE PAYMENT RECEIPT</div>
<table>
<tr><td class="k">Patient</td><td class="v">$cust</td></tr>
<tr><td class="k">Medicine</td><td class="v">$meds</td></tr>
<tr><td class="k">Total Bill</td><td class="v amt">₹ ${fmt(bill)}</td></tr>
<tr><td class="k">Paid ($mode)</td><td class="v amt">₹ ${fmt(deposit)}</td></tr>
<tr><td class="k">Due</td><td class="v amt due">₹ ${fmt(due)}</td></tr>
<tr><td class="k">Date &amp; Time</td><td class="v">$whenStr</td></tr>
<tr><td class="k">Branch</td><td class="v">${esc(b.displayName)}</td></tr>
<tr><td class="k">Received by</td><td class="v">$staffName</td></tr>
</table>
<div class="ty">Thank you. Get well soon.</div>
</body></html>
""".trimIndent()
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun fmt(v: Double): String = "%,.0f".format(v)
    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun isoNow(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        return f.format(Date())
    }
}
