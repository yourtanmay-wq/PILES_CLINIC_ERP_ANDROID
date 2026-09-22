/* =====================================================================
   V245 — MODULE 3 : INCOME & EXPENSE. Manual entries only.
   Master: all branches. B617 (11.08.2026): অংশীদার-ডাক্তারও নিজের ব্রাঞ্চে
   আয়-ব্যয় লিখতে/দেখতে পারেন (Amit Goldar · P.K Roy বাদ) — ব্রাঞ্চ লক।
   Never reads/writes existing Patient Payment/Refund/Collection. Data in
   schema `fin` (RLS: master সব; partner নিজের ব্রাঞ্চ, V307/V310)।
   ===================================================================== */
package com.tkbiswas.pilesclinic.modules

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.tkbiswas.pilesclinic.native.NoBengali
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.TableRowEqualizer
import com.tkbiswas.pilesclinic.native.s
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class IncomeExpenseActivity : AppCompatActivity() {

    /* 🟢🔒 V929 — TK: *"01/09/2026 থেকে এই নীয়ম করুন"*। এই তারিখের আগের কোনো
       দিনে অটো-আয় কখনো বসবে না। এক জায়গায় লেখা, যাতে পরে সরাতে বা বদলাতে
       একটাই লাইন লাগে। */
    private val AUTO_INCOME_FROM = "2026-09-01"


    // 🔴 বাগ-ফিক্স (02.08.2026, TK-রিপোর্ট): এই পর্দাগুলো একই Activity-র ভিতরে
    // setContentView বদলে বদলে দেখানো হয় (আলাদা Activity/Fragment নয়), তাই ফোনের
    // সিস্টেম Back বোতাম আগে সরাসরি পুরো Activity বন্ধ করে দিত (হোমে চলে যেত),
    // ভিতরের কোন পর্দায় আছি সেটা বিবেচনাই করত না। এখন প্রতিটা পর্দা খোলার সময়
    // backAction-এ "কোথায় ফিরব" লেখা থাকে; সিস্টেম Back সেটাই মেনে চলে।
    private var backAction: () -> Unit = { finish() }
    override fun onBackPressed() { backAction() }

    /* 🔴🔒 V418 (TK-রিপোর্ট: "এত ডুপ্লিকেট কেন হবে") — একটা টাকার এন্ট্রি সেভ
       হওয়ার মাঝপথে দ্বিতীয় সেভ শুরু হতে দেওয়া হয় না। ⛔ পর্দা নতুন করে খুললেই
       তালা খুলে যায় (নিচে Add Collection / Add Expense-এ), তাই কোনো অবস্থাতেই
       বোতাম চিরতরে আটকে থাকতে পারে না। */
    private var ieSaveBusy: Boolean = false

    // 🟢🆕 TK-অনুমোদিত (10.08.2026): হোমের "আজকের হিসাব" কার্ড কোন ব্রাঞ্চের দেখাবে (ডিফল্ট সব)।
    private var homeBranch: String = "All Branches"

    /* 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত): টাকার হিসাবও এখন **একই মনে-রাখা ব্রাঞ্চ**
       মানে (native/BranchFilterStore)। এখানে নামটা "All Branches", ওখানে "All" —
       তাই এই দুটো ছোট অনুবাদক। ⛔ অংশীদার-ডাক্তারের ব্রাঞ্চ-লক (B617) অটুট।
       ⛔ কিছু বাছা না-থাকলে আগের মতোই "All Branches" — টাকার পর্দা কখনো ফাঁকা হয় না। */
    private fun v398Branch(): String {
        val v = com.tkbiswas.pilesclinic.native.BranchFilterStore.get(this)
        return when {
            v == com.tkbiswas.pilesclinic.native.BranchFilterStore.ALL -> "All Branches"
            v.isNotBlank() -> v
            else -> "All Branches"
        }
    }
    private fun v398Remember(v: String) {
        com.tkbiswas.pilesclinic.native.BranchFilterStore.set(
            this, if (v == "All Branches") com.tkbiswas.pilesclinic.native.BranchFilterStore.ALL else v)
    }

    // 🔵🔒 B617 (11.08.2026, TK-নির্দেশ, প্রুফ-অনুমোদিত): অংশীদার-ডাক্তার নিজের
    // ব্রাঞ্চের আয়-ব্যয় লিখতে/দেখতে পারবেন (master সবখানে, ডাক্তার নিজ ব্রাঞ্চে-লক)।
    // Amit Goldar · P.K Roy অংশীদার নন (শুধু রোগী দেখেন) — তাঁরা ঢুকতে পারবেন না।
    // ⛔ আসল নিরাপত্তা DB-র RLS-এ (fin.can_entry_branch / is_my_partner_branch);
    //    এই UI-গেট শুধু পর্দা খোলা/ব্রাঞ্চ-লক করে, টাকার সুরক্ষা DB-ই দেয়।
    private val PATIENT_ONLY_DOCTORS = setOf("9046366596", "6297625447")  // Amit Goldar, P.K Roy
    private fun mob10(s: String?): String = (s ?: "").filter { it.isDigit() }.takeLast(10)
    private val isPartnerDoctor: Boolean
        get() {
            val u = NativeSession.current(this) ?: return false
            return !ModuleAuth.isMaster && u.displayRole == "doctor" && mob10(u.mobile) !in PATIENT_ONLY_DOCTORS
        }
    // ডাক্তার হলে তাঁর নিজের লগইন-ব্রাঞ্চ (লক); master হলে null (সব ব্রাঞ্চ বাছা যায়)।
    // 🟢 V401: চাবি-পাওয়া staff/doctor-ও নিজের ব্রাঞ্চে লক — চাবির টেবিলে যে ব্রাঞ্চ
    //          লেখা আছে সেটাই। একাধিক ব্রাঞ্চে চাবি থাকলে লক নয়, শুধু ওই ক'টার মধ্যে বাছাই।
    private val lockedBranch: String?
        get() = when {
            isPartnerDoctor -> NativeSession.current(this)?.branch?.takeIf { it.isNotBlank() }
            ModuleAuth.isMaster -> null
            else -> IePermit.cached(this).singleOrNull()
        }

    /** এই ব্যবহারকারী কোন কোন ব্রাঞ্চ বাছতে পারবেন (মাস্টার হলে সব + All Branches)। */
    private fun ieBranchChoices(): List<String> = when {
        ModuleAuth.isMaster -> listOf("All Branches") + BRANCHES
        isPartnerDoctor -> listOfNotNull(lockedBranch)
        else -> IePermit.cached(this)
    }
    // এন্ট্রির created_by: ডাক্তার-অংশীদারের ক্ষেত্রে DB-র RLS created_by-র শেষ ১০ অঙ্ক
    // = নিজের মোবাইল মেলায়, তাই মোবাইল বসাই (নইলে লেখা RLS আটকাবে)। master-এ আগের কোড।
    private fun entryCreatedBy(): String =
        if (isPartnerDoctor) mob10(NativeSession.current(this)?.mobile) else (ModuleAuth.personCode ?: "master")

    private val BRANCHES = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
    // 🔵🔒 Category তালিকা (09.08.2026, TK-অনুমোদিত): লেখা TK-র, চেহারা প্রফেশনাল — যেগুলোতে
    // " — " আছে সেগুলো ডায়ালগে মূল-লেখা (বোল্ড) + নিচে ছোট সাব-লেখা দেখায় (catDisplay দেখুন)।
    private val CATS = listOf(
        "RMP Commission", "Staff unexpected time Commission", "Staff Salary", "Chamber Rent",
        "Bills — Electricity / Water / Internet", "Medicine / Surgical", "Advertisement",
        "Office — Printing / Cleaning / Repair / Equipment", "Transport / Parcel", "Food",
        "License / Govt Fee", "Other Expense"
    )
    // মূল-লেখা (বোল্ড) + সাব-লেখা (ধূসর) — " — " দিয়ে ভাগ; না থাকলে যেমন আছে তেমন।
    private fun catDisplay(v: String): CharSequence {
        val i = v.indexOf(" — ")
        if (i <= 0) return v
        val main = v.substring(0, i); val sub = v.substring(i + 3)
        return android.text.Html.fromHtml("<b>" + main + "</b><br><small><font color=\"#8A95A5\">" + sub + "</font></small>")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = NativeSession.current(this)?.name ?: ""
        ModuleUi.ensureSignedIn(this, code) {
            /* 🟢🔒 V401 (TK-নির্দেশ): মাস্টার ও অংশীদার-ডাক্তারের পথ **হুবহু আগের মতোই**।
               বাকিদের (staff / অন্য doctor) জন্য নতুন পথ — মাস্টার চাবি চালু করলে তবেই।
               চাবিটা ক্লাউড থেকে একবার মিলিয়ে নেওয়া হয়, তাই পুরনো তথ্যে ভুল বার্তা আসে না। */
            if (ModuleAuth.isMaster || isPartnerDoctor) {
                // 🔵 B617: ডাক্তার হলে নিজের ব্রাঞ্চেই শুরু ও লক (master হলে আগের মতোই)।
                lockedBranch?.let { homeBranch = it }
                renderMenu()
            } else {
                val col = ModuleUi.screen(this, "Income & Expense")
                col.addView(ModuleUi.body(this, "Checking…"))
                Thread {
                    val branches = IePermit.refresh(this)
                    runOnUiThread {
                        if (branches.isNotEmpty()) {
                            lockedBranch?.let { homeBranch = it }
                            renderMenu()
                        } else {
                            val c2 = ModuleUi.screen(this, "Income & Expense")
                            c2.addView(ModuleUi.body(this,
                                "You do not have permission to enter Income & Expense.\n\nPlease ask the Master to switch it on."))
                            c2.addView(ModuleUi.button(this, "Back") { finish() })
                        }
                    }
                }.start()
            }
        }
    }

    /* 🟢🔒 V401 — নিচের ছোট প্রশ্নগুলো থেকেই বাকি সব পর্দা ঠিক হয়।
       ⛔ মাস্টারের ক্ষেত্রে সবকটাই আগের মতোই "সব পারবে"। */
    /** মাস্টার নন — অর্থাৎ যার উপরে তারিখ ও ব্রাঞ্চের নিয়ম খাটবে। */
    private val ieRestricted: Boolean get() = !ModuleAuth.isMaster
    /** ডাক্তার (অংশীদার হোন বা না হোন) — পুরনো হিসাব **দেখতে** পাবেন। */
    private val ieIsDoctor: Boolean
        get() = NativeSession.current(this)?.displayRole == "doctor"
    /** তারিখটা কি আজকের? (ভারতীয় সময় — ডেটাবেসের fin.ie_today()-এর সাথে এক) */
    private fun ieIsToday(iso: String): Boolean = iso == todayIso()

    private fun todayIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US); f.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }
    /* 🔴 V430 (TK-সিদ্ধান্ত ১৮.০৮.২০২৬: "₹2,10,850 — ভারতীয় ভাগ") — এই একটা
       জায়গায় `Locale.US` বসানো ছিল, তাই ২,১০,৮৫০-এর বদলে ২১০,৮৫০ দেখাত —
       অ্যাপের বাকি সব পর্দার (ও কম্পিউটারের) সঙ্গে মিলত না। এখন ভারতীয়
       ভাগেই দেখাবে। ⛔ অঙ্ক একটুও বদলায়নি, শুধু কমা বসার জায়গা। */
    private fun money(n: Double): String = "₹" + com.tkbiswas.pilesclinic.native.MoneyFormat.inr(n)

    // 🔵 খাতার সারি (TK-নির্দেশ, 09.09.2026 — Add Collection/Expense তারিখ):
    // তারিখ **দেখায় dd.MM.yyyy** কিন্তু ভেতরে (tag-এ) **yyyy-MM-dd জমা** রাখে —
    // entry_date query/report/sort সব ISO ধরেই চলে, তাই কিছু ভাঙে না।
    // 🔵 তারিখ দেখানোর ফরম্যাট — খাতার মতোই dd/mm/yyyy (TK-অনুমোদিত)। ⛔ ভেতরে/ক্লাউডে
    // তারিখ সবসময় yyyy-mm-dd (ISO)-ই থাকে (ঘরের .tag-এ), শুধু চোখে দেখা slash-এ।
    // 🔴🔒 V936 (TK-নির্দেশ ৩১.০৮.২০২৬ — সম্পূর্ণ প্রজেক্টে এক ফরম্যাট):
    // এখানে স্ল্যাশ (`31/08/2026`) দেখাত, এখন প্রজেক্টের বিন্দু (`31.08.2026`)।
    // ⛔ ঘরের `.tag`-এ আসল ISO আগের মতোই থাকে — হিসাব/সেভ কিছুই বদলায়নি।
    private fun slashIso(iso: String): String = try {
        val p = iso.split("-"); if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso   // 🔴 V1158
    } catch (e: Exception) { iso }
    // read-only তারিখ-ঘর: চাপলেই ক্যালেন্ডার খোলে (পুরনো দিনও বাছা যায়, কোনো ইমোজি নেই)।
    private fun dateField(seedIso: String = todayIso()): android.widget.EditText {
        val f = ModuleUi.input(this, "Date")
        f.setText(slashIso(seedIso)); f.tag = seedIso
        f.isFocusable = false; f.isFocusableInTouchMode = false; f.isClickable = true; f.keyListener = null
        f.setOnClickListener {
            val cur = (f.tag as? String) ?: seedIso
            val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            try { val p = cur.split("-"); cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) } catch (_: Exception) { }
            android.app.DatePickerDialog(this, { _, y, m, d ->
                val iso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                f.tag = iso; f.setText(slashIso(iso))
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
        return f
    }

    // =====================================================================
    // 🆕 Ledger Sheet (02.08.2026, TK-নির্দেশে, ধাপে ধাপে মকআপ দেখিয়ে অনুমোদন) —
    // Google Sheet-এর মতো গ্রিড: Date | Cash | Online | Expense। Expense ঘরে
    // TK নিজের ভাষায় একাধিক আইটেম লিখবেন (এক লাইনে একাধিকও চলবে); বাক্সের
    // ভেতরের **সব সংখ্যা** (যেখানেই থাকুক) যোগ হয়ে অটো-টোটাল দেখায়। পুরনো Add
    // Collection/Add Expense/Daily Ledger/Monthly Summary — কোনোটাই বদলায়নি,
    // এটা শুধু একটা নতুন, আলাদা, সহজ পথ (fin.collections-এর নতুন কলামে সেভ হয়,
    // পুরনো fin.expenses টেবিল এই পথে ছোঁয়া হয় না)।
    // =====================================================================

    /** বাক্সের ভেতরের সব সংখ্যা (কমা-সহ হলেও) খুঁজে যোগ করে — নাম/শব্দ এমনিতেই বাদ পড়ে। */
    private fun sumNumbersInText(text: String): Double {
        val re = Regex("[0-9][0-9,]*")
        var total = 0.0
        for (m in re.findAll(text)) {
            val n = m.value.replace(",", "").toDoubleOrNull()
            if (n != null) total += n
        }
        return total
    }

    private fun monthLabel(ym: String): String = try {
        val yy = ym.substring(0, 4).toInt(); val mo = ym.substring(5, 7).toInt()
        val names = arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        names[mo - 1] + " " + yy
    } catch (e: Exception) { ym }

    // 🔵🔒 টাকার খাতা — TK-অনুমোদিত প্রুফ (09.08.2026): এটা শুধু পুরনো এন্ট্রি দেখার খাতা
    // (এখানে নতুন এন্ট্রি হয় না — টাকা জমা Add Collection-এ হয়)। উপরে সবুজ হেডারে **একটাই**
    // ব্রাঞ্চ-চিপ + রিফ্রেশ (ফর্মে আর আলাদা Branch ঘর নেই — TK: "দু জায়গায় কেন"), একটাই
    // Month বাছাই, **Back/Show একদম নিচে** (fillViewport + spacer)। কোনো দিন এডিট করতে সেই
    // সারিতে **তিনবার চাপ**; খরচে **একবার** চাপ দিলে ভাঙা-হিসাব। ⛔ টাকার হিসাব/সেভ অটুট।
    private fun sheet(initialYm: String? = null) {
        backAction = { renderMenu() }
        val chosenBranch = lockedBranch ?: homeBranch
        if (chosenBranch !in BRANCHES) {
            ModuleUi.toast(this, "আগে টাকার হিসাব পর্দায় একটি ব্রাঞ্চ বাছুন")
            return
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
        }
        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        scroll.addView(col); page.addView(scroll); setContentView(page)

        /* 🟢🔒 V628 (২৪.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট) — "ওটা তো হিসাবের খাতা...
           প্রতিটা ব্রাঞ্চের হিসাব থাকবে আলাদা, সমস্ত ব্রাঞ্চ একসাথে দেখানো
           যাবে না"। "All Branches" আর বাছা যাবে না — সবসময় একটাই নির্দিষ্ট
           ব্রাঞ্চের হিসাব দেখাবে, কখনো মিশবে না। পুরনো মনে-রাখা মান
           "All Branches" হলে প্রথম আসল ব্রাঞ্চে নামিয়ে আনা হয়। */
        val branchSel = chosenBranch
        val out = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var reload: () -> Unit = {}

        // শুধু মাসের পুরো নাম—Month label/বড় header/Branch পুনরাবৃত্তি নেই।
        val month = android.widget.TextView(this).apply {
            isClickable = true; isFocusable = true
            val ym = initialYm ?: todayIso().substring(0, 7)
            setText(monthLabel(ym)); tag = ym
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#17352A"))
            gravity = android.view.Gravity.CENTER
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#CFE2D5"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        month.setOnClickListener {
            val yms = ArrayList<String>()
            val cal = java.util.Calendar.getInstance()
            for (k in 0 until 15) {
                yms.add(String.format(Locale.US, "%04d-%02d",
                    cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1))
                cal.add(java.util.Calendar.MONTH, -1)
            }
            val labels = yms.map { monthLabel(it) }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Select Month")   /* 🔤 V726 */)
                .setItems(labels) { _, which ->
                    month.tag = yms[which]; month.setText(labels[which]); reload()
                }
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
        }
        col.addView(month)

        /* ➕🔒 V1174 (০৭.০৯.২০২৬, TK-নির্দেশ) — TK, হাতের খাতার ছবিসহ:
           *"26/08/2026 এর হিসাব কি ভাবে তুলিবো"*।

           **কারণ (কোডে মেপে দেখা):** খাতায় শুধু **সেই দিনগুলোই** ওঠে যেদিনে
           ক্লাউডে কিছু আছে (`fin.collections`-এর সারি, নয়তো `fin.expenses`-এর
           খরচ)। যে দিনে কিচ্ছু হয়নি, সেই দিনের সারিই থাকে না — তাই ৩-চাপ দেওয়ার
           জায়গাও নেই। আর V630-এ TK-রই নির্দেশে আলাদা "Add Collection" বোতামটা
           তুলে দেওয়া হয়েছিল (*"আয় এবং ব্যয় দুই রকম আলাদা কলম থাকবে না"*), তাই
           **একদম বাদ পড়া দিন যোগ করার কোনো পথই ছিল না**।

           **এখন:** মাসের নামের নিচে একটাই বোতাম — চাপলে **ফাঁকা Ledger Entry**
           খোলে, ক্যালেন্ডার থেকে দিন বেছে Cash/Online/খরচ লিখে Save।

           ⛔ `existing`-এ **শুধু ব্রাঞ্চ** যায় — কোনো `id` নেই, খরচের লেখাও নেই ⇒
              ① Save সবসময় **নতুন** সারি বানায়, পুরনো কোনো দিন ওভাররাইট হয় না
              ② "Delete Entry" বোতাম আসে না ③ খরচ কপি হয় না।
           ⛔ পুরনো-তারিখের অনুমতির নিয়ম (`ieRestricted`) হুবহু আগেরটাই — মাস্টার
              নন এমন কেউ চাপলে আগের মতোই মাস্টারের কাছে অনুরোধ যায়।
           ⛔ কোনো টাকার হিসাব · মোট · আগের ব্যালেন্স — কিচ্ছু ছোঁয়া হয়নি। */
        col.addView(android.widget.TextView(this).apply {
            text = com.tkbiswas.pilesclinic.native.NoBengali.s("➕ নতুন দিন")
            isClickable = true; isFocusable = true
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            gravity = android.view.Gravity.CENTER
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#EEF7F1"))
                setStroke(dp(1), android.graphics.Color.parseColor("#CFE2D5"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            setOnClickListener {
                /* দেখানো মাসেরই ১ তারিখ থেকে শুরু — ক্যালেন্ডার ওই মাসেই খোলে,
                   TK-কে অন্য মাস থেকে ঘুরে আসতে হয় না। */
                val ym = (month.tag as? String) ?: todayIso().substring(0, 7)
                openSheetRowEditor(ym + "-01", JSONObject().put("branch", branchSel)) { sheet(ym) }
            }
        })

        col.addView(out)
        val footer = compactFooter("← Back", "Show", { renderMenu() }) {
            loadSheet(month.tag as String, branchSel, out)
        }
        footer.setPadding(dp(14), dp(4), dp(14), dp(8))
        for (i in 0 until footer.childCount) (footer.getChildAt(i) as? android.widget.TextView)?.apply {
            minimumHeight = 0; minHeight = 0; setPadding(dp(10), dp(9), dp(10), dp(9))
        }
        page.addView(footer)

        reload = { loadSheet(month.tag as String, branchSel, out) }
        reload()
    }


    /* ═══════════════════════════════════════════════════════════════════════
       🟢🔒 V929 (৩১.০৮.২০২৬, TK-নির্দেশ: *"হ্যাঁ, অ্যান্ড্রয়েডেও অটো ইনকাম
       বসিয়ে দিন"*) — **টাকার হিসাবের আয় নিজে থেকে বসবে।**
       ওয়েবে এটা V927-এ বসেছে; এটা তারই হুবহু যমজ (নিয়ম ৬.৬)।

       ⛔⛔ **ডেটাবেসে একটা অক্ষরও লেখা হয় না** — শুধু গুনে দেখানো। তাই TK-এর
          পছন্দ না হলে এই অংশটুকু সরালেই আগের অবস্থা হুবহু ফিরে আসে।
       ⛔ **০১/০৯/২০২৬-এর আগের কোনো দিন কখনো ছোঁয়া হয় না** (TK: *"পুরানো দিনে
          অনেক ডেমো করা হয়েছিল… সেটা তো আর আমাদের ইনকাম না"*)।
       ⛔ যে দিনে **হাতে লেখা `collections` সারি আছে**, সেখানে চলে না —
          মানুষের লেখাই সবসময় জেতে।
       ⛔ গোনার নিয়ম নতুন নয় — Payment পর্দার প্রমাণিত
          `PaymentRepository.fetchCollectionRange()`; refund-এর নিয়ম, ব্রাঞ্চ-
          ছাঁকনি, cashAmount/onlineAmount — সবই ওর ভিতরেই আগে থেকে আছে।
       ⛔ পড়া ব্যর্থ হলে (null) কিচ্ছু বসে না — ভুল/অসম্পূর্ণ সংখ্যা কখনো নয়।
       ⚠️ ক্লাউড ছোঁয় — শুধু background thread থেকে ডাকা হয়।
       ═══════════════════════════════════════════════════════════════════════ */
    /* 👥🔒 V1215 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"সেই কালেকশনের
       উপরে ক্লিক করলে আমি যেন দেখতে পাই ওই কালেকশনগুলো কার কার কাছ থেকে এসেছে …
       কারণ ০৩/০৯/২০২৬ এই তারিখে আমাদের চেম্বার বন্ধ ছিল"*।
       ⇒ অটো-আয়ের সংখ্যাটা যে সারিগুলো থেকে বসে, **সেগুলোই** এখানে দিন ধরে জমা
         থাকে — রোগীর নাম · Patient ID · সময় · মোড · টাকা সবই ওদের ভিতরেই আছে।
       ⛔ **একটাও নতুন ক্লাউড-পড়া নেই** — হুবহু সেই একই `fetchCollectionRange()`,
          একই ছাঁকনি; শুধু যোগফলের পাশাপাশি সারিগুলোও রাখা হচ্ছে।
       ⛔ টাকার এক পয়সাও এতে বদলায় না। */
    /* 🔴🔒 V1220 (০৮.০৯.২০২৬ — TK-র Android Studio-তে বিল্ড ভাঙল, আমারই ভুল)।
       **আসল কারণ (মেপে পাওয়া, আন্দাজ নয়):** kapt (Room-এর জন্য চালু) প্রতিটা
       ক্লাসের **Java stub** বানায়, আর সেখানে অন্য প্যাকেজের টাইপ পুরো নাম ধরে
       লেখে — `com.tkbiswas.pilesclinic.native.CollectionRow`। কিন্তু `native`
       **Java-র সংরক্ষিত শব্দ**, তাই ওই stub-টা Java হিসেবে পড়াই যায় না
       (`<identifier> expected`)। এই ফাইলটা `modules` প্যাকেজে, তাই পুরো নামটাই
       লিখতে হত ⇒ ভাঙত। (`native` প্যাকেজের ভিতরের ক্লাসে একই টাইপ থাকলে ছোট
       নামেই চলে, তাই ওখানে কখনো ভাঙেনি — যেমন `PaymentActivity.kt`।)
       ⇒ তাই ঘরটার টাইপ এখন `Any` — ভিতরে যা রাখা হয় তা হুবহু আগের সেই
         `CollectionRow` সারিগুলোই, শুধু পড়ার সময় ছেঁকে নেওয়া হয়
         (`filterIsInstance`, ফাংশনের ভিতরে — stub-এ যায় না)।
       ⛔ কোনো তথ্য · টাকার অঙ্ক · ছাঁকনি · ক্লাউড-পড়া কিচ্ছু বদলায়নি।
       ⛔ V1215-এর কাজ (কালেকশনে চাপলে কার কার টাকা) হুবহু আগের মতোই চলে। */
    private val collRowsByDate = HashMap<String, MutableList<Any>>()

    private fun autoIncomeByDate(ym: String, branchSel: String): Map<String, Pair<Double, Double>> {
        val out = HashMap<String, Pair<Double, Double>>()
        collRowsByDate.clear()   // 👥 V1215
        try {
            val start = "$ym-01"
            if (start < AUTO_INCOME_FROM) {
                // মাসটা পুরোপুরি নিয়মের আগে হলে একটাও অনুরোধ পাঠানোর দরকার নেই
                if (ym < AUTO_INCOME_FROM.substring(0, 7)) return out
            }
            val y = ym.substring(0, 4).toInt(); val m = ym.substring(5, 7).toInt()
            val cal = java.util.Calendar.getInstance()
            cal.set(y, m - 1, 1)
            val last = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val from = if (start < AUTO_INCOME_FROM) AUTO_INCOME_FROM else start
            val to = String.format(Locale.US, "%04d-%02d-%02d", y, m, last)
            if (from > to) return out
            val br = if (branchSel == "All Branches") "All" else branchSel
            /* ⛔ Context ছাড়াই — `fetchCollectionRange()` নিজে Context ব্যবহার
               করে না; Context শুধু `fillPatientIds()`-এ রোগীর ID/রোগ/ঠিকানা
               **দেখানোর** জন্য লাগে, আর সেটা এখানে দরকারই নেই (আমরা শুধু
               টাকার অঙ্ক গুনছি)। Context না দিলে ওই ধাপটা সারিগুলো অবিকৃত
               ফেরত দেয় — টাকার এক পয়সাও বদলায় না। */
            val rows = com.tkbiswas.pilesclinic.native.PaymentRepository()
                .fetchCollectionRange(br, from, to) ?: return out
            /* 🔴🔒 V930 (নিজের যাচাইয়ে ধরা পড়া দোষ) — **ফেরত-দেওয়া রেকর্ডের টাকা
               বাদ দিতে হবে।** খাতার সারি B110 (TK, ২৯.০৭.২০২৬): Reject / Delete /
               Registration Cancel হয়ে যাওয়া রেকর্ডের টাকা রোগীকে ফেরত দেওয়া
               হয়েছে, তাই দিনের হিসাবে সেটা ধরা যাবে না।
               `fetchTodayCollection()` এই ছাঁকনি বসায়, কিন্তু `fetchCollectionRange()`
               বসায় না — আমি ওটাই ব্যবহার করেছিলাম, ফলে ফোনের অটো-আয় ওয়েবের
               চেয়ে বেশি দেখাত (ওয়েবের `collectionRows()` ছাঁকনিটা বসায়)।
               ⇒ এখানেই একই `RefundedRecords` দিয়ে ছেঁকে নেওয়া হলো — দুই দিকের
               সংখ্যা এক (নিয়ম ৬.৬)। ⛔ খোঁজা ব্যর্থ হলে তালিকা ফাঁকা ফেরে,
               তখন আগের মতোই কারও টাকা বাদ যায় না। */
            val refundedMobiles = try {
                com.tkbiswas.pilesclinic.native.RefundedRecords.fetch(br)
            } catch (_: Throwable) { HashSet<String>() }
            fun isRefunded(mob: String): Boolean {
                if (refundedMobiles.isEmpty()) return false
                val dg = mob.filter { it.isDigit() }.takeLast(10)
                return dg.length == 10 && dg in refundedMobiles
            }
            for (row in rows) {
                val d = row.date.take(10)
                if (d.length != 10 || d < AUTO_INCOME_FROM) continue
                /* 🔴🔒 V1100 (০৫.০৯.২০২৬, TK-নির্দেশ: *"পুরনো তারিখের কোনো পেমেন্ট
                   যদি খাতায় ওঠে, সেগুলি যেন আয়-ব্যয়ের খাতায় কোনো প্রভাব না ফেলে"*)
                   — স্টাফ প্রতিনিয়ত পুরনো রোগীর নাম · নম্বর · **পুরনো টাকা** অ্যাপে
                   তুলছেন। ওই টাকা ক্লিনিকে অনেক আগেই এসেছে, আজকের আয় নয়।
                   ⇒ টাকার **তারিখ** যদি সেটা **অ্যাপে তোলার দিনের আগের** হয়,
                     সেটা পুরনো টাকা তোলা — খাতায় ধরা হবে না।
                   ⛔ আজকের/পরের দিনের সাধারণ পেমেন্ট আগের মতোই ধরা হয়।
                   ⛔ তোলার দিনটা জানা না গেলে (পুরনো সারিতে ঘরটা ফাঁকা) কিছুই
                      বাদ যায় না — পুরনো নিরাপদ আচরণই চলে।
                   ⛔ Payment/Collection পর্দার সংখ্যা এতে বদলায় না — এই ছাঁকনি
                      শুধু আয়-ব্যয়ের খাতার নিজে-বসা আয়ের জন্য। */
                val enteredOn = row.paidAt.take(10)
                if (enteredOn.length == 10 && d < enteredOn) continue
                if (isRefunded(row.mobile)) continue
                val cur = out[d] ?: Pair(0.0, 0.0)
                out[d] = Pair(cur.first + row.cashAmount, cur.second + row.onlineAmount)
                collRowsByDate.getOrPut(d) { ArrayList() }.add(row)   // 👥 V1215
            }
            // ঋণাত্মক (বেশি refund) হলে ০ ধরা হয় — ওয়েবের হুবহু একই নিয়ম
            for (k in out.keys.toList()) {
                val v = out[k] ?: continue
                out[k] = Pair(if (v.first > 0) v.first else 0.0, if (v.second > 0) v.second else 0.0)
            }
        } catch (_: Throwable) { }
        return out
    }

    /* 👥🔒 V1215 — "এই টাকা কার কার কাছ থেকে" পপ-আপ।
       ⛔ কোনো হিসাব এখানে কষা হয় না — যে সারিগুলো থেকে ঘরের সংখ্যাটা বসেছে,
          হুবহু সেগুলোই দেখানো হয়, তাই ঘর ও তালিকা কখনো আলাদা হতে পারে না।
       ⛔ তালিকা না পাওয়া গেলে সৎ বার্তা — বানানো কিছু দেখানো হয় না। */
    private fun showCollectionPeople(
        dateIso: String, mode: String, row: JSONObject, rowBranch: String
    ) {
        val isCash = mode == "cash"
        // 🔴 V1220 — ঘরটা `Any` (উপরের কারণ), তাই এখানে ছেঁকে নেওয়া হয়।
        val all = collRowsByDate[dateIso].orEmpty()
            .filterIsInstance<com.tkbiswas.pilesclinic.native.CollectionRow>()
        val mine = all.filter { if (isCash) it.cashAmount > 0.0 else it.onlineAmount > 0.0 }
            .sortedBy { it.time }
        val total = mine.sumOf { if (isCash) it.cashAmount else it.onlineAmount }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(6))
        }
        if (mine.isEmpty()) {
            body.addView(ModuleUi.body(this, NoBengali.s(
                "এই দিনের অ্যাপে তোলা কোনো পেমেন্ট পাওয়া গেল না (হাতে লেখা সংখ্যা হতে পারে)।")))
        } else {
            for (cr in mine) {
                val amt = if (isCash) cr.cashAmount else cr.onlineAmount
                val line = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(android.graphics.Color.parseColor("#F4FBF6"))
                        setStroke(dp(1), android.graphics.Color.parseColor("#CFE3D4"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(8) }
                }
                val texts = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                /* 🐞🔒 V1322 (TK-নির্দেশ, ডেমো-প্রুফ "A" পাশ, ১১.০৯.২০২৬: *"পেমেন্ট
                   টা কোন staff নিয়েছে কোন তারিখে কোন সময়ে সেটা জরুরী"*) —
                   স্টাফের নাম এখন সবচেয়ে উপরের লাইনে (সবুজ), রোগীর নামের ঠিক আগে। */
                texts.addView(android.widget.TextView(this).apply {
                    val patientPart = cr.name.ifBlank { cr.mobile }
                    if (cr.staff.isNotBlank()) {
                        val full = cr.staff + "  —  " + patientPart
                        text = android.text.SpannableString(full).apply {
                            setSpan(
                                android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#0A7C3F")),
                                0, cr.staff.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    } else {
                        text = patientPart
                    }
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#17212B"))
                })
                val sub = listOf(cr.patientId, slashIso(dateIso), cr.time, cr.source)
                    .map { it.trim() }.filter { it.isNotBlank() }.joinToString("  ·  ")
                if (sub.isNotBlank()) texts.addView(android.widget.TextView(this).apply {
                    text = sub; textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor("#5B6B81"))
                })
                line.addView(texts)
                line.addView(android.widget.TextView(this).apply {
                    text = money(amt); textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0A7C3F"))
                })
                body.addView(line)
            }
            body.addView(android.view.View(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#CFE3D4"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                    .apply { topMargin = dp(4); bottomMargin = dp(10) }
            })
            body.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                    text = if (isCash) "Total Cash" else "Total Online"
                    textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0A7C3F"))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                    text = money(total); textSize = 15f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0A7C3F"))
                })
            })
        }
        val editBtn = android.widget.Button(this).apply {
            text = NoBengali.s("✏️ এই সংখ্যাটা হাতে বদলান")
            isAllCaps = false; textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(2), android.graphics.Color.parseColor("#0B4F2A"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                .apply { topMargin = dp(12) }
        }
        body.addView(editBtn)

        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(
                this, (if (isCash) "💵 " else "🏦 ") + slashIso(dateIso) + " — " +
                    (if (isCash) "Cash Collection" else "Online Collection")))
            .setView(android.widget.ScrollView(this).apply { addView(body) })
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }

        editBtn.setOnClickListener {
            try { dlg.dismiss() } catch (_: Throwable) { }
            quickFieldEditor(row, dateIso, rowBranch, mode, if (isCash) "Cash" else "Online") {
                sheet(dateIso.substring(0, 7))
            }
        }
    }

    private fun loadSheet(ym: String, branchSel: String, out: LinearLayout) {
        // 🔒🔒 B602 (10.08.2026, TK-নির্দেশ "cache-first, খুব সাবধানে"): এই ফোনে জমানো
        // শেষ সফল খাতা থাকলে সাথে সাথে দেখানো হয় (Loading... এর বদলে), তারপর ক্লাউড
        // থেকে হালনাগাদ এলে ঠিক একই buildSheetTable দিয়ে বদলে যায়। ⛔ হিসাব-লজিক
        // (buildSheetTable) একটুও বদলায়নি — শুধু আগে ক্যাশ, পরে আসল; ব্যর্থ রিফ্রেশে ক্যাশ অক্ষত।
        val cacheKey = "sheet_${ym}_${branchSel}"
        val cachedSheet = loadSheetCache(cacheKey)
        out.removeAllViews()
        if (cachedSheet != null) buildSheetTable(cachedSheet.first, cachedSheet.second, cachedSheet.third, branchSel, out)
        else out.addView(ModuleUi.body(this, "Loading..."))
        val start = "$ym-01"
        val y = ym.substring(0, 4).toInt(); val m = ym.substring(5, 7).toInt()
        val nextY = if (m == 12) y + 1 else y; val nextM = if (m == 12) 1 else m + 1
        val end = String.format(Locale.US, "%04d-%02d-01", nextY, nextM)
        // 🔵 TK-সিদ্ধান্ত (09.08.2026): ব্রাঞ্চ ধরে আলাদা। "All Branches" হলে ফিল্টার নেই
        // (আগের মতোই সব ব্রাঞ্চ)। ⛔ Monthly Summary-র হুবহু একই ফিল্টার-প্যাটার্ন।
        val branchQ = if (branchSel != "All Branches") "&branch=eq.$branchSel" else ""
        Thread {
            // AUDIT FIX (2026-08-06): getRowsChecked tells a real empty month
            // apart from a network failure, so a weak signal no longer shows an
            // empty sheet (which used to look like all the money was gone).
            val r = ModuleAuth.getRowsChecked(
                "fin", "collections",
                "select=*&entry_date=gte.$start&entry_date=lt.$end&ignored=eq.false$branchQ&order=entry_date.asc"
            )
            // 🔵 আগের বাকি (Previous Balance) = এই মাসের আগের সব দিনের (একই ব্রাঞ্চের)
            // নগদ+অনলাইন − খরচ। শুধু দরকারি ৪টা কলাম টানা হয় (egress কম রাখতে)।
            // খাতার নিজের ঘর দিয়েই হিসাব — TK-অনুমোদিত প্রুফ অনুযায়ী।
            val prevR = ModuleAuth.getRowsChecked(
                "fin", "collections",
                "select=cash,online,expense_total,expense_notes&entry_date=lt.$start&ignored=eq.false$branchQ"
            )
            /* 🔴🔒 V399 (16.08.2026, TK-রিপোর্ট ছবিসহ — Birpara: খরচের ঘরে সব "-",
               Total খরচ ০, অথচ অংশীদার পর্দায় Total Expense ₹4,54,339)।

               **কারণ (কোড-প্রমাণ):** টাকার খাতা এতদিন খরচ পড়ত **শুধু** `collections`
               সারির `expense_total` (বা নোটের সংখ্যা) থেকে — `fin.expenses` টেবিল
               কখনো পড়ত না। কিন্তু "Add Expense" দিয়ে লেখা খরচ ওই টেবিলেই জমা হয়
               (লাইন ~১৫৭৩ `insert`)। ফলে ওই খরচগুলো খাতায় দেখাত না, আর
               **"অবশিষ্ট টাকা"ও আসলের চেয়ে বেশি দেখাত**।

               ⛔ নতুন কোনো নিয়ম বানানো হয়নি — Monthly Summary (লাইন ~১৬৭৮-১৭৩৩) ও
                  আজকের হিসাব (লাইন ~৯০৯) **আগে থেকেই দুটো উৎসই** পড়ে; এখানে সেই
                  প্রমাণিত নিয়মটাই বসানো হলো, তাই সব পর্দার হিসাব এক হবে।
               ⛔ ব্রাঞ্চ-ছাঁকনি ও `ignored=eq.false` হুবহু একই।
               ⛔ পড়া ব্যর্থ হলে আগের আচরণ — বাড়তি খরচ ০ ধরা হয়, কিছু ভাঙে না। */
            /* 🟢🔒 V400: আগে শুধু `entry_date,amount` টানা হতো (মোট যোগ করার জন্য)।
               এখন প্রতিটা খরচ আলাদা লাইনে দেখাতে ও এডিট করতে হবে, তাই ওই একই
               সারিগুলোরই আরো কয়েকটা ঘর টানা হয়। ⛔ সারির **সংখ্যা** এক — একই
               ফিল্টার, একই টেবিল; শুধু প্রতি সারিতে কয়েকটা ছোট ঘর বেশি আসে। */
            /* ⏱️🔒 V1215 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"যে খরচ আমি আগে তুলব
               সেটা আগে দেখাবে … টাইম অনুসারে সাজতে হবে"*।
               ⇒ খরচের সব পড়ায় এখন `created_at` ধরে **আগে-তোলাটা আগে** (asc)।
               ⛔ কোন সারি আসবে বা টাকার যোগফল — কিচ্ছু বদলায় না, শুধু ক্রম। */
            val expMonth = ModuleAuth.getRowsChecked(
                "fin", "expenses",
                "select=id,entry_date,branch,category,paid_to,amount,mode&entry_date=gte.$start&entry_date=lt.$end&ignored=eq.false$branchQ&order=created_at.asc"
            )
            val expByDate = HashMap<String, Double>()
            val expItemsByDate = HashMap<String, org.json.JSONArray>()
            if (expMonth.ok) {
                val er = expMonth.rows
                for (i in 0 until er.length()) {
                    val e = er.getJSONObject(i)
                    val d = e.optString("entry_date", "")
                    if (d.isNotBlank()) {
                        expByDate[d] = (expByDate[d] ?: 0.0) + e.optDouble("amount", 0.0)
                        val arr = expItemsByDate[d] ?: org.json.JSONArray().also { expItemsByDate[d] = it }
                        arr.put(e)
                    }
                }
            }
            val prevExpR = ModuleAuth.getRowsChecked(
                "fin", "expenses",
                "select=amount&entry_date=lt.$start&ignored=eq.false$branchQ"
            )
            var prevBal = 0.0
            val prevOk = prevR.ok && expMonth.ok && prevExpR.ok
            if (prevR.ok) {
                val pr = prevR.rows
                for (i in 0 until pr.length()) {
                    val c = pr.getJSONObject(i)
                    val note = c.optString("expense_notes", "").let { if (it == "null") "" else it }
                    val exp = c.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) }
                    prevBal += c.optDouble("cash", 0.0) + c.optDouble("online", 0.0) - exp
                }
            }
            if (prevExpR.ok) {
                val pe = prevExpR.rows
                for (i in 0 until pe.length()) prevBal -= pe.getJSONObject(i).optDouble("amount", 0.0)
            }
            /* 🔒 V399: বাড়তি খরচ সারির **ভিতরেই** বসানো হয় (`_v399ExtraExpense`), তাই
               ক্যাশ থেকে দেখানো হলেও সংখ্যা একই থাকে — buildSheetTable-এ আলাদা
               প্যারামিটার লাগে না, পুরনো কল-পথ এক অক্ষরও বদলায় না।
               যে দিনে শুধু খরচ আছে অথচ কোনো collection সারি নেই, সেই দিনের জন্য
               একটা সারি যোগ করা হয় (cash/online = ০) — নইলে ওই খরচ মোটেই দেখাত না।
               ⛔ ওই সারিগুলোতে ৩-চাপে এডিট খোলে না (`_v399ExpenseOnly`), কারণ
                  ক্লাউডে ওগুলোর কোনো collection সারি নেই। */
            val mergedRows = org.json.JSONArray()
            val seenDates = HashSet<String>()
            if (r.ok) {
                for (i in 0 until r.rows.length()) {
                    val c = r.rows.getJSONObject(i)
                    val d = c.optString("entry_date", "")
                    if (d.isNotBlank()) seenDates.add(d)
                    val extra = expByDate[d] ?: 0.0
                    if (extra > 0.0) try { c.put("_v399ExtraExpense", extra) } catch (_: Throwable) { }
                    // 🟢 V400: ওই দিনের আলাদা-আলাদা খরচগুলো সারির ভিতরেই (এডিটের জন্য)
                    expItemsByDate[d]?.let { try { c.put("_v400ExpItems", it) } catch (_: Throwable) { } }
                    mergedRows.put(c)
                }
                for ((d, amt) in expByDate) {
                    if (d.isNotBlank() && !seenDates.contains(d) && amt > 0.0) {
                        val only = org.json.JSONObject()
                            .put("entry_date", d).put("cash", 0.0).put("online", 0.0)
                            .put("_v399ExtraExpense", amt).put("_v399ExpenseOnly", true)
                        expItemsByDate[d]?.let { try { only.put("_v400ExpItems", it) } catch (_: Throwable) { } }
                        mergedRows.put(only)
                        // 🐞🔒 V1322 (TK-রিপোর্ট ছবিসহ, ১১.০৯.২০২৬: "এটা কি ফাজলামি") —
                        // এই তারিখটা এখানে `seenDates`-এ যোগ হতো না, তাই নিচের V929
                        // অটো-আয় লুপ একই তারিখের জন্য **দ্বিতীয়** একটা সারি বসিয়ে
                        // দিত (একই cash/online দুবার) — Total ও "অবশিষ্ট টাকা"-তে
                        // দ্বিগুণ যোগ হতো। ডেটাবেসের টাকা ছোঁয়া হয়নি, শুধু এই
                        // পর্দার যোগফল। এখন যোগ করা হলো, তাই আর দ্বিতীয় সারি বসবে না।
                        seenDates.add(d)
                    }
                }
                /* 🟢🔒 V630 (২৪.০৮.২০২৬, TK-নির্দেশ) — চলতি মাস দেখলে, আজকের সারি
                   সবসময় সবার নিচে (এখনো কোনো এন্ট্রি না থাকলেও) — নতুন দিন শুরু
                   করতে আলাদা "আয়" পর্দায় যেতে হবে না, এই খালি সারিতেই সরাসরি
                   Cash/Online বসানো যায় (৩-চাপে quickFieldEditor)। */
                val todayNow = todayIso()
                if (ym == todayNow.substring(0, 7) && !seenDates.contains(todayNow)) {
                    mergedRows.put(org.json.JSONObject()
                        .put("entry_date", todayNow).put("branch", branchSel).put("cash", 0.0).put("online", 0.0))
                    seenDates.add(todayNow)
                }
                /* 🟢🔒 V929 — অটো-আয় বসানো (ওয়েবের V927-এর হুবহু যমজ)।
                   ⛔ যে দিনে ক্লাউডে সত্যিকারের `collections` সারি আছে সেটা কখনো
                      ছোঁয়া হয় না — শুধু খরচ-only সারি · আজকের খালি সারি · আর
                      যে দিনের কোনো সারিই নেই, সেগুলোতেই বসে। */
                try {
                    val auto = autoIncomeByDate(ym, branchSel)
                    if (auto.isNotEmpty()) {
                        val realDates = HashSet<String>()
                        for (i in 0 until r.rows.length()) {
                            val d0 = r.rows.getJSONObject(i).optString("entry_date", "")
                            if (d0.isNotBlank()) realDates.add(d0)
                        }
                        for (i in 0 until mergedRows.length()) {
                            val row0 = mergedRows.getJSONObject(i)
                            val d0 = row0.optString("entry_date", "")
                            if (d0.isBlank() || realDates.contains(d0)) continue
                            val v = auto[d0] ?: continue
                            if (row0.optDouble("cash", 0.0) != 0.0 || row0.optDouble("online", 0.0) != 0.0) continue
                            row0.put("cash", v.first).put("online", v.second).put("_v929Auto", true)
                            row0.remove("_v399ExpenseOnly")   // এখন আয়ও আছে, তাই আর "শুধু খরচ" নয়
                        }
                        for ((d0, v) in auto) {
                            if (seenDates.contains(d0)) continue
                            if (v.first <= 0.0 && v.second <= 0.0) continue
                            mergedRows.put(org.json.JSONObject()
                                .put("entry_date", d0).put("branch", branchSel)
                                .put("cash", v.first).put("online", v.second).put("_v929Auto", true))
                            seenDates.add(d0)
                        }
                    }
                } catch (_: Throwable) { }
            }
            /* তারিখ অনুসারে সাজানো (ক্লাউড থেকে collections আগেই সাজানো আসে; নতুন
               যোগ হওয়া খরচ-দিনগুলোও যেন সঠিক জায়গায় বসে)। */
            val sortedRows = org.json.JSONArray()
            run {
                val list = ArrayList<org.json.JSONObject>()
                for (i in 0 until mergedRows.length()) list.add(mergedRows.getJSONObject(i))
                list.sortBy { it.optString("entry_date", "") }
                for (x in list) sortedRows.put(x)
            }
            runOnUiThread {
                if (r.ok) {
                    saveSheetCache(cacheKey, sortedRows, prevBal, prevOk)
                    out.removeAllViews()
                    buildSheetTable(sortedRows, prevBal, prevOk, branchSel, out)
                } else if (cachedSheet == null) {
                    // ব্যর্থ ও ক্যাশ নেই — আগের বার্তা। ⛔ ক্যাশ থাকলে সেটাই অক্ষত থাকে (মুছি না)।
                    out.removeAllViews()
                    out.addView(ModuleUi.body(this, "⚠️ Could not load right now — weak internet. Your data is safe; open this again when online."))
                }
            }
        }.start()
    }

    // 🔴 B309 (03.08.2026, TK-রিপোর্ট — "একটা আরেকটার গায়ে ঘেঁষে গেছে"): আগে প্রতিটা
    // ঘর `weight` দিয়ে টেবিলের একটা FIXED ৩৬০dp প্রস্থের ভিতরেই ভাগ হতো — তাই
    // "Date" ঘরের জন্য মাত্র ~৫৮dp বরাদ্দ হতো, "02.08.2026" (১০ অক্ষর) সেখানে
    // ধরত না, প্রতিটা অক্ষর আলাদা লাইনে নেমে ঘেঁষে যেত। এখন প্রতিটা ঘরের নিজস্ব
    // নির্দিষ্ট (fixed) dp-প্রস্থ — কনটেন্ট আগে থেকেই জানা টেক্সটের সাথে মিলিয়ে,
    // টেবিল স্বাভাবিকভাবেই চওড়া হয়, HorizontalScrollView-এ পাশে টেনে দেখা যায়
    // (এই স্ক্রিনের নিজের সাহায্য-লেখাতেই এটা আগে থেকে বলা ছিল)।
    // 🔴 B310 (03.08.2026, TK-রেফারেন্স ছবি — Patient Timeline "Full Journey"
    // টেবিল) — হুবহু একই বক্স-বর্ডার স্টাইল এখানেও।
    private fun cellBorderDrawable(): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.WHITE)
            setStroke(1, android.graphics.Color.parseColor("#D9E2EC"))
        }

    private fun sheetCell(text: String, widthDp: Int, bg: String, fg: String, bold: Boolean): android.widget.TextView =
        android.widget.TextView(this).apply {
            this.text = text; textSize = 11f
            setTextColor(android.graphics.Color.parseColor(fg))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(ModuleUi.dp(this@IncomeExpenseActivity, 6), ModuleUi.dp(this@IncomeExpenseActivity, 6),
                ModuleUi.dp(this@IncomeExpenseActivity, 6), ModuleUi.dp(this@IncomeExpenseActivity, 6))
            setBackgroundColor(android.graphics.Color.parseColor(bg))
            layoutParams = LinearLayout.LayoutParams(ModuleUi.dp(this@IncomeExpenseActivity, widthDp), LinearLayout.LayoutParams.WRAP_CONTENT)
            // ⛔ কোনো maxLines/ellipsize বসানো হয়নি — লেখা এখনো wrap করবে (কখনো কাটবে না),
            // শুধু এখন প্রতিটা ঘরের যথেষ্ট চওড়া জায়গা আছে বলে স্বাভাবিক লেখাতেই এক/দুই লাইনে ধরবে।
        }

    private fun gridDivider(vertical: Boolean): android.view.View =
        android.view.View(this).apply {
            layoutParams = if (vertical)
                LinearLayout.LayoutParams(ModuleUi.dp(this@IncomeExpenseActivity, 1), LinearLayout.LayoutParams.MATCH_PARENT)
            else
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ModuleUi.dp(this@IncomeExpenseActivity, 1))
            setBackgroundColor(android.graphics.Color.parseColor("#D6DEE6"))
        }

    // 🆕 (02.08.2026, TK-নির্দেশে) — Daily Ledger/Monthly Summary-ও এখন Google
    // Sheet-এর মতো বক্স-টেবিলে দেখায়, ঠিক Patient Timeline-এর "Full Journey"
    // টেবিলের একই লক করা প্যাটার্ন পুনর্ব্যবহার করে (TableRowEqualizer) — কোনো
    // লেখা দু-লাইনে গেলে সেই সারির (আর পুরো টেবিলের) সব বক্স একসাথে সমান
    // উচ্চতায় বেড়ে যায়, কখনো একটা বক্স ছোট আরেকটা বড় দেখায় না।
    private fun gridRow(label: String, value: String, bold: Boolean, header: Boolean = false): LinearLayout {
        val bg = if (header) "#0A7C3F" else if (bold) "#EAF6EE" else "#FFFFFF"
        val fg = if (header) "#FFFFFF" else if (bold) "#0A5C33" else "#222222"
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(sheetCell(label, 220, bg, fg, bold || header))
        row.addView(sheetCell(value, 130, bg, fg, bold || header))
        return row
    }

    /** লেবেল-মান জোড়ার তালিকা দিয়ে একটা সম্পূর্ণ Google-Sheet-স্টাইল টেবিল বানায়,
     *  সব সারির উচ্চতা এক করে দেয় (Full Journey-র মতোই)। */
    private fun renderGridTable(
        out: LinearLayout, headTitle: String, headValue: String,
        rows: List<Triple<String, String, Boolean>>, emptyText: String? = null
    ) {
        out.removeAllViews()
        if (rows.isEmpty() && emptyText != null) { out.addView(ModuleUi.body(this, emptyText)); return }
        val scrollX = android.widget.HorizontalScrollView(this)
        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val built = ArrayList<LinearLayout>()
        val head = gridRow(headTitle, headValue, true, header = true)
        table.addView(head); table.addView(gridDivider(false)); built.add(head)
        for ((l, v, b) in rows) {
            val r = gridRow(l, v, b)
            table.addView(r); table.addView(gridDivider(false)); built.add(r)
        }
        scrollX.addView(table)
        out.addView(scrollX)
        // 🔒 TK-LOCKED RULE পুনর্ব্যবহার — "সব সারির উচ্চতা এক" (V142)
        TableRowEqualizer.equalize(table, built)
    }

    private fun buildSheetTable(rows: JSONArray, prevBalance: Double, prevOk: Boolean, branchSel: String, out: LinearLayout) {
        // 🔴 B310 (03.08.2026, TK-রেফারেন্স ছবি দেখিয়ে "এই ধরনের বক্সে রাখবেন")
        // — আগের ধরে-নেওয়া (guessed) dp-প্রস্থের বদলে এখন Patient Timeline-এর
        // "Full Journey" টেবিলের হুবহু একই প্রমাণিত পদ্ধতি — Paint দিয়ে আসল
        // টেক্সট মেপে (header-সহ) সবচেয়ে চওড়া কনটেন্টের সমান কলাম-প্রস্থ বার
        // করা হয়, + slack (ওই টেবিলে TK-রিপোর্ট করা "ফন্ট মাপে সামান্য গরমিলে
        // অক্ষর কাটা" বাগের পরে যোগ করা নিরাপত্তা)। Expense কলাম Note-কলামের
        // মতোই বাকি সব জায়গা নেয় (weight=1), তাই আর কোনো HorizontalScroll লাগে
        // না — ভবিষ্যতে কলাম-প্রস্থ ভুল হয়ে টেক্সট ভাঙার ঝুঁকি অনেক কমে গেল।
        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val dpx = { v: Int -> ModuleUi.dp(this, v) }
        val boldTf = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val measure = android.graphics.Paint().apply { typeface = boldTf; textSize = 11f * resources.displayMetrics.scaledDensity }
        val cellPadPx = dpx(8) * 2
        val cellSlackPx = dpx(10)
        var dateColPx = measure.measureText("Date")
        var amtColPx = maxOf(measure.measureText("Cash"), measure.measureText("Online"))
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            val dotted = try {
                val p = r.optString("entry_date").split("-"); p[2] + "/" + p[1] + "/" + p[0]
            } catch (e: Exception) { r.optString("entry_date") }
            /* 🔴🔒 V929 — মাস্টারের পর্দায় তারিখের পাশে ট্যাগ বসে, তাই কলামের
               প্রস্থ মাপার সময়ও সেটাই মাপতে হবে — নইলে লেখাটা কেটে যেত। */
            val dottedM = if (ModuleAuth.isMaster && r.optString("entry_date") >= AUTO_INCOME_FROM) {
                if (r.optBoolean("_v929Auto", false)) "$dotted  AUTO"
                else if (r.optString("id", "").isNotBlank()) "$dotted  ✎"
                else dotted
            } else dotted
            dateColPx = maxOf(dateColPx, measure.measureText(dottedM))
            amtColPx = maxOf(amtColPx, measure.measureText(money(r.optDouble("cash", 0.0)).removePrefix("₹")),
                measure.measureText(money(r.optDouble("online", 0.0)).removePrefix("₹")))
        }
        dateColPx = maxOf(dateColPx, measure.measureText("Total"))
        val dateColWidth = dateColPx.toInt() + cellPadPx + cellSlackPx
        val amtColWidth = amtColPx.toInt() + cellPadPx + cellSlackPx

        // 🔵 খাতার সারি (TK-অনুমোদিত প্রুফ, 09.09.2026): gravity যোগ — হেডার মাঝখানে,
        // টাকার ঘর ডানে। ⛔ শুধু দেখানোর সাজ · কোনো টাকার হিসাব ছোঁয়া হয়নি।
        fun boxCell(text: String, w: Int, bg: String, fg: String, bold: Boolean, weight: Float? = null, gravityV: Int? = null): android.widget.TextView =
            android.widget.TextView(this).apply {
                this.text = text; textSize = 11f
                setTextColor(android.graphics.Color.parseColor(fg))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (gravityV != null) gravity = gravityV
                setPadding(dpx(8), dpx(8), dpx(8), dpx(8))
                layoutParams = if (weight != null) LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                    else LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT)
                background = if (bg == "#0A7C3F") android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor(bg)) }
                    else cellBorderDrawable().apply { setColor(android.graphics.Color.parseColor(bg)) }
                if (weight != null) { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
            }

        // হেডার সারি
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(boxCell("Date", dateColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("Cash", amtColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("Online", amtColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("খরচ", 0, "#0A7C3F", "#FFFFFF", true, weight = 1f, gravityV = android.view.Gravity.CENTER))
        table.addView(head)
        val builtRows = ArrayList<LinearLayout>(); builtRows.add(head)

        var cashTot = 0.0; var onlineTot = 0.0; var expTot = 0.0
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            val d = r.optString("entry_date")
            val dotted = try {
                val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0]
            } catch (e: Exception) { d }
            val cash = r.optDouble("cash", 0.0); val online = r.optDouble("online", 0.0)
            val note = r.optString("expense_notes", "").let { if (it == "null") "" else it }
            /* 🔴🔒 V399: `collections`-এর নিজের খরচ + "Add Expense"-এর খরচ — দুটোই।
               (`_v399ExtraExpense` উপরে সারির ভিতরেই বসানো হয়েছে।) */
            val expSum = r.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) } +
                r.optDouble("_v399ExtraExpense", 0.0)
            cashTot += cash; onlineTot += online; expTot += expSum
            val bg = if (i % 2 == 0) "#FFFFFF" else "#F7FBF8"
            // 🔵 TK-নির্দেশ (09.08.2026): এক-চাপে ভুল করে এডিটে ঢুকে যাওয়া ঠেকাতে — কোনো
            // দিন এডিট করতে সেই সারিতে **তিনবার** চাপতে হবে (১.২ সেকেন্ডের মধ্যে)। খরচের ঘর
            // আলাদা করে এক-চাপে ভাঙা-হিসাব দেখায় (সেটা নিচে আলাদা listener)।
            val tapCount = intArrayOf(0)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                isClickable = true; isFocusable = true
            }
            val resetTaps = Runnable { tapCount[0] = 0 }
            row.setOnClickListener {
                tapCount[0]++
                row.removeCallbacks(resetTaps)
                if (tapCount[0] >= 3) {
                    tapCount[0] = 0
                    /* 💰🔒 V1173 (০৭.০৯.২০২৬, TK-নির্দেশ) — TK, ছবিসহ:
                       *"একটা পেমেন্ট ভুল করে ওঠেনি, আমি এখান থেকে করতে পারছি না কেন?
                       আমি মাস্টার, আমাকে এরকম বিভ্রান্ত কেন করা হবে?"*

                       **যা ছিল (V399):** যে দিনে শুধু খরচ আছে, সেদিন `fin.collections`-এ
                       কোনো সারিই নেই — খাতার সারিটা অ্যাপ নিজে বানিয়ে দেখায়। এডিটর
                       খুলত না, শুধু বার্তা দেখাত। **ওটা ইচ্ছাকৃত সুরক্ষা ছিল, দোষ নয়** —
                       ওই বানানো সারিটা `existing` হিসেবে পাঠালে তার **খরচের লেখাটা**
                       `collections.expense_notes`-এ কপি হয়ে যেত, আর ওই দিনের খরচ
                       **দুবার গোনা** হত। কিন্তু ফল খারাপ: মাস্টারও বাদ পড়া টাকা
                       বসাতে পারতেন না।

                       **এখন:** ওই সারিতে ৩-চাপে **ফাঁকা নতুন এন্ট্রি** খোলে — তারিখ ও
                       ব্রাঞ্চ আগে থেকে বসানো, Cash/Online/খরচ তিনটেই ফাঁকা। শুধু
                       Cash/Online বসিয়ে Save করলেই ওই দিনের আয় বসে যায়।

                       ⛔ `existing`-এ **শুধু ব্রাঞ্চ** পাঠানো হয় — কোনো `id` নেই, খরচের
                          লেখাও নেই। তাই ① খরচ কপি হয় না (দুবার গোনার ঝুঁকি নেই)
                          ② Save নতুন সারি বানায় (পুরনো কিছু ওভাররাইট হয় না)
                          ③ "Delete Entry" বোতামও আসে না (id ফাঁকা)।
                       ⛔ পুরনো-তারিখের অনুমতির নিয়ম (`ieRestricted`) হুবহু আগেরটাই —
                          মাস্টার নন এমন কেউ চাইলে মাস্টারের কাছেই অনুরোধ যাবে।
                       ⛔ খরচের সারিগুলো (`fin.expenses`) এক অক্ষরও ছোঁয়া হয় না। */
                    if (r.optBoolean("_v399ExpenseOnly", false)) {
                        val fresh = JSONObject().put("branch", r.optString("branch", branchSel))
                        openSheetRowEditor(d, fresh) { sheet(d.substring(0, 7)) }
                    } else openSheetRowEditor(d, r) { sheet(d.substring(0, 7)) }
                }
                else row.postDelayed(resetTaps, 1200)
            }
            val isExpenseOnly = r.optBoolean("_v399ExpenseOnly", false)
            val rowBranch = r.optString("branch", branchSel)
            val cashCell = boxCell(if (isExpenseOnly) "-" else money(cash).removePrefix("₹"), amtColWidth, bg, "#0A7C3F", false, gravityV = android.view.Gravity.END)
            val onlineCell = boxCell(if (isExpenseOnly) "-" else money(online).removePrefix("₹"), amtColWidth, bg, "#0A7C3F", false, gravityV = android.view.Gravity.END)
            // 🟢🔒 V630 (২৪.০৮.২০২৬, TK-নির্দেশ) — Cash/Online ঘরে নিজস্ব ৩-চাপ,
            // শুধু সেই একটা সংখ্যার জন্য ছোট এডিটর খোলে (পুরো সারির ফর্ম নয়)।
            // ⛔ শুধু-খরচের সারিতে (isExpenseOnly) কোনো collections সারিই নেই,
            // তাই এখানে এডিট চালু হয় না — আগের মতোই।
            /* 👥🔒 V1215 (TK-অনুমোদিত ফটো-প্রুফ) — **এক চাপেই** ওই দিনের ওই ঘরের
               টাকা কার কার কাছ থেকে এসেছে তার তালিকা। হাতে বদলানোর পথটা হারায়নি —
               তালিকার ভিতরেই "✏️ এই সংখ্যাটা হাতে বদলান" বোতাম, যেটা সেই একই
               প্রমাণিত `quickFieldEditor()`-ই খোলে (নতুন কোনো সেভ-পথ নয়)।
               ⛔ TK নিজে বেছে নিয়েছেন: *"হ্যাঁ পাশ, এভাবেই বসিয়ে দিন"* — তাই
                  V630-এর ৩-চাপ আর লাগে না, ভুলে সংখ্যা বদলে যাওয়ার পথও নেই
                  (বদলাতে হলে পপ-আপের ভিতরের বোতামে চাপতেই হবে)। */
            if (!isExpenseOnly) {
                cashCell.isClickable = true
                cashCell.setOnClickListener { showCollectionPeople(d, "cash", r, rowBranch) }
                onlineCell.isClickable = true
                onlineCell.setOnClickListener { showCollectionPeople(d, "online", r, rowBranch) }
            }
            /* 🟢🔒 V929 — TK: *"অটো না হাতে ঠিক করা এটা মাস্টার ছাড়া কেউ দেখতে
               পাবে না"*। তাই ট্যাগটা শুধু মাস্টারের পর্দায়; স্টাফ/ডাক্তার
               শুধু সংখ্যাটাই দেখেন। ⛔ টাকার অঙ্কে কোনো হাত পড়ে না। */
            val dottedShown = if (ModuleAuth.isMaster && d >= AUTO_INCOME_FROM) {
                if (r.optBoolean("_v929Auto", false)) "$dotted  AUTO"
                else if (r.optString("id", "").isNotBlank()) "$dotted  ✎"
                else dotted
            } else dotted
            row.addView(boxCell(dottedShown, dateColWidth, bg, "#41506A", true))
            row.addView(cashCell)
            row.addView(onlineCell)
            // 🔵 খাতার সারি (TK-প্রুফ): খরচের ঘরে শুধু মোট টাকা (লাল)। সংখ্যায় চাপ দিলে
            // "কিসে খরচ" ভাঙা-হিসাব পপ-আপ (নাম-টাকা)। বাকি ঘরে চাপ → ওই দিন এডিট (আগের মতোই)।
            val expText = if (expSum > 0.0) money(expSum).removePrefix("₹") else "-"
            val expCell = boxCell(expText, 0, bg, "#B42318", false, weight = 1f, gravityV = android.view.Gravity.END)
            if (expSum > 0.0 || note.isNotBlank()) {
                expCell.isClickable = true
                // 🟢 V400: সারির নিজের খরচ (note/expense_total) আর "Add Expense"-এর
                // আলাদা খরচগুলো — দুটোই পপ-আপে পাঠানো হয়, যাতে ভুলটায় চেপে বদলানো যায়।
                val ownExp = r.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) }
                val items = r.optJSONArray("_v400ExpItems")
                expCell.setOnClickListener {
                    showExpenseBreakdown(dotted, note, expSum, ownExp, items, null, d, rowBranch)   // ➕ V1203
                }
            } else if (!isExpenseOnly) {
                // 🟢🔒 V630 (TK-নির্দেশ, "হ্যাঁ চাই") — খালি খরচ ঘরে চাপলে নতুন
                // খরচ যোগ করার ফর্ম খোলে (addExpense()-এর প্রমাণিত পথ, শুধু এই
                // দিন+ব্রাঞ্চ প্রি-ফিল করে)। ⛔ কিছু ওভাররাইট হয় না — নতুন যোগ,
                // তাই এখানে ৩-চাপের দরকার নেই (Cash/Online-এর মতো বিদ্যমান
                // সংখ্যা বদলানোর ঝুঁকি নেই)।
                expCell.isClickable = true
                expCell.setOnClickListener { addExpense(prefillDate = d, prefillBranch = rowBranch) }
            }
            row.addView(expCell)
            table.addView(row)
            builtRows.add(row)
        }
        // গ্র্যান্ড টোটাল সারি
        val tot = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tot.addView(boxCell("Total", dateColWidth, "#EAF6EE", "#0A5C33", true))
        tot.addView(boxCell(money(cashTot).removePrefix("₹"), amtColWidth, "#EAF6EE", "#0A5C33", true, gravityV = android.view.Gravity.END))
        tot.addView(boxCell(money(onlineTot).removePrefix("₹"), amtColWidth, "#EAF6EE", "#0A5C33", true, gravityV = android.view.Gravity.END))
        tot.addView(boxCell(money(expTot).removePrefix("₹"), 0, "#EAF6EE", "#B42318", true, weight = 1f, gravityV = android.view.Gravity.END))
        table.addView(tot)
        builtRows.add(tot)
        // 🔴 B602 (TK-নির্দেশ, ১০.০৮): টেবিলের উপরের নির্দেশ-লাইন ("তিনবার চাপুন…") বাদ —
        // প্রফেশনাল ভিউতে ডেমি-লেখা থাকবে না। ⛔ তিনবার-চাপে এডিট আচরণ আগের মতোই অটুট,
        // শুধু দেখানো লেখাটা সরানো হলো।
        out.addView(table)
        if (rows.length() == 0) out.addView(ModuleUi.body(this, "No entries yet this month."))
        // 🔒 TK-LOCKED RULE পুনর্ব্যবহার — "সব সারির উচ্চতা এক" (V142)
        TableRowEqualizer.equalize(table, builtRows)
        // 🔴 V453 (TK-নির্দেশ ১৮.০৮.২০২৬) — Previous Balance ও অবশিষ্ট টাকা এখন
        // টেবিলের নিচে একটাই পাশাপাশি সারিতে (আগে দুটো আলাদা বড়-খোলা বার,
        // একটা টেবিলের ওপরে একটা নিচে — ব্রাঞ্চের নামও দুবার লেখা থাকত)।
        // হিসাব এক অক্ষরও বদলায়নি — আগের বাকি + এই মাসের (নগদ+অনলাইন) − খরচ।
        val remaining = prevBalance + cashTot + onlineTot - expTot
        out.addView(balanceBarPair(if (prevOk) money(prevBalance) else "—", if (prevOk) money(remaining) else "—"))
    }

    // 🔴🔒 V453 (TK-নির্দেশ ১৮.০৮.২০২৬: "একই পাতায় ব্রাঞ্চের নাম বারবার কেন,
    // Previous Balance/অবশিষ্ট টাকা এত খোলামেলা কেন — পাশাপাশি রাখুন") —
    // হেডারেই ব্রাঞ্চ-চিপ (Jalpaiguri ▾) আছে, তাই এই বারে আর আলাদা করে
    // ব্রাঞ্চের নাম বসে না। দুটো সংখ্যা এখন এক সারিতে পাশাপাশি (ছোট, কম
    // জায়গা জুড়ে), আগের মতো দুটো আলাদা বড়-খোলা বার নয়।
    private fun balanceBarPair(prevValue: String, remValue: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(4) }
        }
        fun half(label: String, value: String, bg: String, fg: String, last: Boolean): LinearLayout {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(dp(10), dp(10), dp(10), dp(10))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(android.graphics.Color.parseColor(bg))
                    if (bg == "#EEFAF0") setStroke(dp(1), android.graphics.Color.parseColor("#CDE9D5"))
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (!last) marginEnd = dp(8) }
            }
            box.addView(android.widget.TextView(this).apply {
                text = label; textSize = 11.5f
                setTextColor(android.graphics.Color.parseColor(fg))
                gravity = android.view.Gravity.CENTER
            })
            box.addView(android.widget.TextView(this).apply {
                text = value; textSize = 15f
                setTextColor(android.graphics.Color.parseColor(fg))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(0, dp(2), 0, 0)
            })
            return box
        }
        row.addView(half("Previous Balance", prevValue, "#EEFAF0", "#0A5C33", false))
        row.addView(half("অবশিষ্ট টাকা", remValue, "#0B4F2A", "#FFFFFF", true))
        return row
    }

    // 🔵🔒 খাতা-স্টাইল বার (TK-প্রুফ অনুমোদিত): উপরে হালকা-সবুজ "Previous Balance",
    // নিচে গাঢ়-সবুজ "অবশিষ্ট টাকা"। শুধু দেখানোর সাজ — কোনো টাকা যোগ/বিয়োগ/সংরক্ষণ নয়।
    // ⛔ V453-এর পর এই ফাংশন আর নতুন করে ডাকা হয় না (উপরের balanceBarPair
    // ব্যবহার হয়), শুধু পুরনো রেফারেন্স/ব্যাকআপ হিসেবে রাখা হলো — মুছে ফেলা হয়নি।
    private fun balanceBar(label: String, value: String, strong: Boolean, branchSel: String): LinearLayout {
        val bg = if (strong) "#0B4F2A" else "#EEFAF0"
        val fg = if (strong) "#FFFFFF" else "#0A5C33"
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor(bg))
                if (!strong) setStroke(dp(1), android.graphics.Color.parseColor("#CDE9D5"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(4) }
        }
        val lblTv = android.widget.TextView(this).apply {
            text = if (branchSel == "All Branches") label else "$label · $branchSel"
            textSize = if (strong) 15f else 13f
            setTextColor(android.graphics.Color.parseColor(fg))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val valTv = android.widget.TextView(this).apply {
            text = value
            textSize = if (strong) 16f else 14f
            setTextColor(android.graphics.Color.parseColor(fg))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        bar.addView(lblTv); bar.addView(valTv)
        return bar
    }

    // 🔵🔒 খাতা-স্টাইল (TK-প্রুফ অনুমোদিত, 08.08.2026): Ledger Sheet-এর খরচের ঘরে
    // চাপ দিলে "ওই দিনের খরচ কিসে হয়েছিল" ভাঙা-হিসাব পপ-আপ। খরচ TK লেখেন নাম-টাকা
    // কমা দিয়ে (যেমন "Rupam-1348, CRP-2759, Medicine-10000")। এখানে শুধু সেই লেখাটাই
    // সুন্দর করে সাজিয়ে দেখানো হয় — কোনো টাকা যোগ/বিয়োগ/সংরক্ষণ হয় না, হিসাবের
    // নিয়ম এক অক্ষরও বদলায় না। মোট = সারিতে যা দেখাচ্ছে সেই একই সংখ্যা।
    private fun showExpenseBreakdown(dotted: String, note: String, total: Double) =
        showExpenseBreakdown(dotted, note, total, -1.0, null, null)

    /* 🟢🔒 V400 (16.08.2026, TK-অনুমোদিত মকআপ): এই পপ-আপেই এখন ওই দিনের প্রতিটা
       "Add Expense" খরচ আলাদা লাইনে — চাপলে বদলানো/মোছার পর্দা খোলে। খাতার সারিতে
       নিজের হাতে লেখা খরচ আলাদা করে দেখানো হয় (ওটা ৩-চাপে Ledger Entry-তে বদলায়)।
       ⛔ কোনো টাকা যোগ/বিয়োগের নিয়ম বদলায়নি — মোট = সারিতে যা দেখাচ্ছে সেই সংখ্যাই।
       ⛔ TK-নির্দেশ: খরচের **সব** সংখ্যা লাল (#B42318)। */
    private fun showExpenseBreakdown(
        dotted: String, note: String, total: Double, ownExpense: Double, items: JSONArray?,
        /* 🟢🔒 V628 (২৪.০৮.২০২৬, TK-নির্দেশ) — এই তারিখের আসল `collections`
           সারি (Monthly Summary থেকে আসলে পাঠানো হয়, যেখানে এখন ব্রাঞ্চ সবসময়
           একটাই নির্দিষ্ট — তাই কোন সারি এডিট হবে তা নিয়ে কোনো দ্বিধা নেই)।
           null হলে (যেমন Ledger Sheet নিজের পর্দা থেকে ডাকলে, যেখানে ইতিমধ্যেই
           ৩-চাপে সরাসরি এডিট করা যায়) নিচের বোতামটা দেখানো হয় না। */
        editRow: JSONObject?,
        /* ➕🔒 V1203 (০৮.০৯.২০২৬, TK-রিপোর্ট, হুবহু): *"উক্ত দিনে আরও ব্যয় ছিল,
           সেই ব্যয়টা আমি এখন কেন লিখতে পারছি না এখানে … আমি চাইছি এই পর্দাতেই
           যেন আরো খরচা হলে আমি সেটা যোগ করতে পারি"*।
           🔬 কোডে মেপে দেখা — কথাটা সত্যি: খরচের ঘর **ফাঁকা থাকলেই** কেবল নতুন
             খরচের ফর্ম খুলত (V630); ওই দিনে আগে থেকে খরচ থাকলে এই পপ-আপ শুধু
             বিবরণ দেখাত, যোগ করার কোনো পথ ছিল না।
           ⇒ তাই ওই দিন ও ব্রাঞ্চ এখানে পাঠানো হয়, আর নিচে "➕ Add Expense on
             this day" বোতাম বসে — যেটা `addExpense()`-এর **প্রমাণিত পুরনো
             পথটাই** খোলে (নতুন কোনো সেভ-লজিক নয়, কিছু ওভাররাইট হয় না)। */
        isoDate: String? = null,
        forBranch: String? = null
    ) {
        val clean = note.let { if (it == "null") "" else it }.trim()
        val amtRe = Regex("[0-9][0-9,]*(?:\\.[0-9]+)?")
        val lines = ArrayList<Pair<String, Double>>()
        // কমা বা নতুন-লাইন দিয়ে আলাদা আইটেম
        for (raw in clean.split(",", "\n", ";")) {
            val seg = raw.trim()
            if (seg.isEmpty()) continue
            val m = amtRe.findAll(seg).lastOrNull()
            if (m == null) continue
            val amt = m.value.replace(",", "").toDoubleOrNull() ?: continue
            // সংখ্যা ও সংখ্যার আগের যোগচিহ্ন (- : = ·) বাদ দিয়ে বাকিটাই নাম
            var name = (seg.substring(0, m.range.first) + seg.substring(m.range.last + 1))
                .trim().trim('-', ':', '=', '·', '.', '/', '(', ')').trim()
            if (name.isEmpty()) name = "খরচ"
            lines.add(name to amt)
        }
        val RED = android.graphics.Color.parseColor("#B42318")
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(6))
        }
        val nItems = items?.length() ?: 0
        // লাইনের View আর তার খরচ-সারি একসাথে রাখা হয় — পরে ঠিক ওই সারিতেই চাপ বসে।
        val itemViews = ArrayList<Pair<android.view.View, JSONObject>>()
        // 🟢🔒 V628 — "✏️ Edit This Day" বোতামের রেফারেন্স, dlg তৈরির আগেই body-তে
        // বসে যায় (তাই দেখা যায়), কিন্তু ক্লিক-লিসেনার dlg তৈরি হওয়ার পরে বসানো হয়
        // (dismiss()-এ dlg লাগে) — নিচে দেখুন।
        var editBtnRef: android.widget.Button? = null

        // 🔴 V413 (TK-নির্দেশ, ১৭.০৮.২০২৬): *"কোন ডেমি লেখা থাকবে না"* — এখানে
        //    "Tap the wrong line to edit it" ধরনের নির্দেশ-লাইনটা ছিল, তুলে দেওয়া হলো।
        //    ⛔ চাপলে এডিট খোলার কাজটা আগের মতোই আছে, শুধু লেখাটা নেই।
        // ১) "Add Expense" পর্দা থেকে লেখা খরচ — প্রতিটা আলাদা, চাপলে এডিট
        for (i in 0 until nItems) {
            val e = items!!.optJSONObject(i) ?: continue
            val amt = e.optDouble("amount", 0.0)
            val label = listOf(e.s("category"), e.s("paid_to"), e.s("mode"))
                .filter { it.isNotBlank() }.joinToString(" · ")
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(11), dp(12), dp(11))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(android.graphics.Color.parseColor("#F2FAF5"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#9FD3B4"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) }
                isClickable = true
            }
            line.addView(android.widget.TextView(this).apply {
                text = (if (label.isBlank()) "খরচ" else label) + "   ✏️"
                textSize = 14.5f; setTextColor(android.graphics.Color.parseColor("#22312A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            line.addView(android.widget.TextView(this).apply {
                text = money(amt); textSize = 15.5f
                setTextColor(RED); setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            body.addView(line)
            itemViews.add(line to e)
        }
        // ২) খাতার সারিতে নিজের হাতে লেখা খরচ (ওটা ৩-চাপে Ledger Entry-তে বদলায়)
        val own = if (ownExpense >= 0.0) ownExpense else lines.fold(0.0) { a, b -> a + b.second }
        if (own > 0.0 || clean.isNotEmpty()) {
            if (nItems > 0) {
                body.addView(android.widget.TextView(this).apply {
                    text = NoBengali.s("নিচের ") + money(own) + NoBengali.s(" খাতার সারিতেই লেখা — বদলাতে হলে ওই সারির তারিখ / Cash / Online ঘরে 3 বার চাপুন")
                    textSize = 12.5f; setTextColor(android.graphics.Color.parseColor("#6A5320"))
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(android.graphics.Color.parseColor("#FFF7E6"))
                        setStroke(dp(1), android.graphics.Color.parseColor("#F0D9A0"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(8) }
                })
            }
            if (lines.isEmpty()) {
                body.addView(breakdownLine(
                    if (clean.isEmpty()) "খাতার সারিতে লেখা খরচ" else clean, own, RED))
            } else {
                for ((name, amt) in lines) body.addView(breakdownLine(name, amt, RED))
            }
        } else if (nItems == 0) {
            body.addView(android.widget.TextView(this).apply {
                text = NoBengali.s("এই দিনের খরচের কোনো বিবরণ লেখা নেই।")
                textSize = 15f; setTextColor(android.graphics.Color.parseColor("#22312A"))
            })
        }
        // ৩) মোট (TK-নির্দেশ: এটাও লাল)
        body.addView(android.view.View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#E2B3AD"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
                .apply { topMargin = dp(6); bottomMargin = dp(10) }
        })
        body.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                text = NoBengali.s("মোট খরচ"); textSize = 16.5f
                setTextColor(RED); setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                text = money(total); textSize = 16.5f
                setTextColor(RED); setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
        })

        /* 🟢🔒 V628 (২৪.০৮.২০২৬, TK-নির্দেশ) — Monthly Summary থেকে এখানে এলে
           (editRow পাঠানো হলে) আর Master হলে — সরাসরি ওই দিনের Ledger এডিটর
           খোলার বোতাম। ⛔ এডিটরের কোড/সেভ-লজিক নতুন কিছু নয় — Ledger Sheet-এর
           নিজের ৩-চাপ এডিটরই (`openSheetRowEditor`) পুনর্ব্যবহার হচ্ছে, শুধু
           এখান থেকেও পৌঁছানো যাচ্ছে। ব্রাঞ্চ এখন সবসময় নির্দিষ্ট বলে (V628-এর
           "All Branches" বাদ) কোন সারি এডিট হবে তা নিয়ে কোনো দ্বিধা নেই। */
        // ➕ V1203 — ওই দিনে আরও খরচ যোগ করার বোতাম (দিন ও ব্রাঞ্চ দুটোই জানা থাকলেই)।
        var addBtnRef: android.widget.Button? = null
        if (!isoDate.isNullOrBlank() && !forBranch.isNullOrBlank() && forBranch in BRANCHES) {
            val addBtn = android.widget.Button(this).apply {
                text = "➕ Add Expense on this day"; isAllCaps = false; textSize = 14.5f
                setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat()
                    setColor(android.graphics.Color.WHITE)
                    setStroke(dp(2), android.graphics.Color.parseColor("#0B4F2A"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                    .apply { topMargin = dp(12) }
            }
            body.addView(addBtn)
            addBtnRef = addBtn
        }
        if (editRow != null && ModuleAuth.isMaster) {
            val editBtn = android.widget.Button(this).apply {
                text = "✏️ Edit This Day"; isAllCaps = false; textSize = 14.5f
                setTextColor(android.graphics.Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.parseColor("#0B4F2A"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46))
                    .apply { topMargin = dp(12) }
            }
            body.addView(editBtn)
            editBtnRef = editBtn
        }

        val scroll = android.widget.ScrollView(this).apply { addView(body) }
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, dotted + " — খরচের বিবরণ"))
            .setView(scroll)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }

        // লাইনে চাপ → এডিট পর্দা (পপ-আপ বন্ধ করে)
        for ((v, e) in itemViews) {
            v.setOnClickListener {
                try { dlg.dismiss() } catch (_: Throwable) { }
                openExpenseEditor(e)
            }
        }
        // ➕ V1203 — পপ-আপ বন্ধ করে ওই দিন+ব্রাঞ্চ প্রি-ফিল করা Add Expense ফর্ম।
        addBtnRef?.setOnClickListener {
            try { dlg.dismiss() } catch (_: Throwable) { }
            addExpense(prefillDate = isoDate, prefillBranch = forBranch)
        }
        // 🟢🔒 V628 — "✏️ Edit This Day" — পপ-আপ বন্ধ করে সরাসরি Ledger এডিটর।
        if (editRow != null) editBtnRef?.setOnClickListener {
            try { dlg.dismiss() } catch (_: Throwable) { }
            val d = editRow.optString("entry_date")
            openSheetRowEditor(d, editRow) { sheet(d.substring(0, 7)) }
        }
    }

    /** পপ-আপের একটা সাধারণ লাইন — নাম বাঁয়ে, টাকা ডানে (লাল)। */
    private fun breakdownLine(name: String, amt: Double, red: Int): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#FCFEFD"))
                setStroke(dp(1), android.graphics.Color.parseColor("#D9E2EC"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
            addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                text = name; textSize = 14.5f
                setTextColor(android.graphics.Color.parseColor("#22312A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                text = money(amt); textSize = 15.5f
                setTextColor(red); setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
        }

    /* 🟢🔒🆕 V400 (16.08.2026, TK-নির্দেশ ও অনুমোদিত মকআপ — "আমি যেন Edit করতে পারি
       / কখনো কমও হতে পারে কখনো বেশিও হতে পারে")।

       এতদিন `fin.expenses`-এ লেখা খরচ **শুধু যোগ করা** যেত (`insert`, লাইন ~১৭৫০) —
       অ্যাপের কোথাও বদলানো বা মোছার কোনো পথ ছিল না। এখন খাতার খরচ-পপ-আপ থেকে
       ওই খরচেই চাপ দিলে এই পর্দা খোলে।

       ⛔ যা হুবহু আগের মতোই রাখা হয়েছে:
         · Save = `ModuleAuth.update("fin","expenses","id=eq.…")` — Staff Profile ও
           Leave-এ বহু দিন ধরে চলা একই ফাংশন; নতুন কিছু বানানো হয়নি।
         · Delete = `ignored=true` — খাতার সারির "Delete Entry"-র (লাইন ~৮৭০)
           হুবহু একই প্রমাণিত নিয়ম: হিসাব থেকে বাদ যায়, সারি চিরতরে মোছে না।
         · Category তালিকা, ব্রাঞ্চ তালিকা, তারিখ-ঘর — Add Expense পর্দার একই ঘর।
         · `fin.expenses`-এ আগে থেকেই আপডেট-অডিট ট্রিগার আছে (fin.audit), তাই
           কে কী বদলাল তার পুরনো মান নিজে থেকেই জমা থাকে।
       ⛔ কোনো হিসাবের সূত্র ছোঁয়া হয়নি — আজকের হিসাব / মাসের হিসাব / অংশীদার
          সবাই আগে থেকেই এই টেবিলই পড়ে, তাই বদল সব জায়গায় নিজে থেকেই মিলে যাবে। */
    private fun openExpenseEditor(exp: JSONObject) {
        val expId = exp.s("id")
        val startIso = exp.s("entry_date").ifBlank { todayIso() }
        if (expId.isBlank()) { ModuleUi.toast(this, "এই খরচটি এখন বদলানো যাচ্ছে না — একবার ↻ চেপে আবার দেখুন"); return }
        backAction = { sheet(startIso.substring(0, 7)) }

        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(16))
        }
        scroll.addView(col); setContentView(scroll)
        col.addView(hero("✏️ খরচ বদলান"))

        val dateInp = dateField(startIso)
        val branch = spinner(BRANCHES)
        BRANCHES.indexOf(exp.s("branch")).let { if (it >= 0) branch.setSelection(it) }
        /* 📝🔒 V1203 — Add Expense-এর মতোই এখানেও **একটাই লেখার ঘর**। আগে
           Category তালিকা-ঘর ছিল; টাইপ-করা নতুন লেখা ওই তালিকায় না থাকায়
           বদলাতে গেলে ঘরটা ফাঁকা দেখাত ও সেভ আটকে যেত — সেই ফাঁকটাও এতে বন্ধ।
           পুরনো সারিতে দুটো আলাদা লেখা থাকলে " · " দিয়ে জুড়ে দেখানো হয়, যাতে
           কিছু হারায় না। */
        val oldCat = exp.s("category")
        val oldPaid = exp.s("paid_to")
        val joined = listOf(oldCat, oldPaid).filter { it.isNotBlank() && it != "Other Expense" }
            .distinct().joinToString(" · ")
        val spentOn = ModuleUi.input(this, "Spent On (optional)").apply { setText(joined) }
        // 🔴 TK-নির্দেশ: খরচের সব সংখ্যা লাল — তাই Amount ঘরের লেখাও লাল।
        val amount = ModuleUi.numberInput(this, "Amount", allowDecimal = true).apply {
            setText(exp.optDouble("amount", 0.0).let { if (it == 0.0) "" else String.format(Locale.US, "%.0f", it) })
            setTextColor(android.graphics.Color.parseColor("#B42318"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val modes = listOf("Cash", "Online")
        val mode = spinner(modes)
        modes.indexOf(exp.s("mode")).let { if (it >= 0) mode.setSelection(it) }

        col.addView(entryCard(listOf(
            "Date" to dateInp, "Branch" to branch, "Amount" to amount,
            "Mode" to mode, "Spent On (optional)" to spentOn
        )))
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        // সেভ/মোছার পরে খাতাটাই আবার খোলা হয় — নতুন তারিখের মাস ধরে, ক্যাশ মোছার পরে,
        // তাই ঠিক নতুন সংখ্যাটাই দেখা যায়।
        val goBack = { sheet(((dateInp.tag as? String) ?: startIso).substring(0, 7)) }

        col.addView(compactFooter("← Back", "Save", { sheet(startIso.substring(0, 7)) }) {
            /* 📝 V1203 — ঘরটা ঐচ্ছিক; ফাঁকা হলে "Other Expense"। */
            val typedE = spentOn.text.toString().trim()
            val c = if (typedE.isBlank()) "Other Expense" else typedE
            val amt = amount.text.toString().toDoubleOrNull() ?: 0.0
            if (amt <= 0.0) { ModuleUi.toast(this, "Enter Amount")   /* 🔤 V726 */; return@compactFooter }
            /* 🟢🔒 V401: পুরনো তারিখের খরচ — মাস্টারের অনুমতি লাগবে। */
            val dNow = (dateInp.tag as? String) ?: startIso
            if (ieRestricted && !ieIsToday(dNow)) {
                ieAskApproval(IePermit.EDIT_EXPENSE, branch.selectedItem.toString(), dNow, expId,
                    JSONObject().put("category", c).put("paid_to", "")
                        .put("amount", amt).put("mode", mode.selectedItem.toString())) { goBack() }
                return@compactFooter
            }
            val patch = JSONObject()
                .put("entry_date", (dateInp.tag as? String) ?: startIso)
                .put("branch", branch.selectedItem.toString())
                .put("category", c)
                .put("paid_to", "")
                .put("amount", amt)
                .put("mode", mode.selectedItem.toString())
            // কখন বদলানো হলো — টেবিলে `updated_at` নিজে থেকে বসে না, তাই এখানে বসানো হয়।
            try {
                val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                f.timeZone = TimeZone.getTimeZone("UTC")
                patch.put("updated_at", f.format(java.util.Date()))
            } catch (_: Throwable) { }
            ModuleUi.toast(this, "Saving...")
            Thread {
                val ok = ModuleAuth.update("fin", "expenses", "id=eq.$expId", patch)
                runOnUiThread {
                    if (ok) { clearSheetCaches(); ModuleUi.toast(this, "খরচ বদলানো হয়েছে।"); goBack() }
                    else ModuleUi.toast(this, "বদলানো গেল না (নেট?) — আবার চেষ্টা করুন")
                }
            }.start()
        })

        // 🟢🔒 V401 (TK-নির্দেশ "মোছা শুধু মাস্টার"): মাস্টার ছাড়া এই বোতাম দেখানোই হয় না।
        if (ModuleAuth.isMaster) col.addView(android.widget.Button(this).apply {
            text = "🗑️ Delete Entry"; isAllCaps = false; textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat(); setColor(android.graphics.Color.parseColor("#B42318"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply { topMargin = dp(12) }
            setOnClickListener {
                val amtNow = amount.text.toString().toDoubleOrNull() ?: exp.optDouble("amount", 0.0)
                androidx.appcompat.app.AlertDialog.Builder(this@IncomeExpenseActivity)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this@IncomeExpenseActivity, "🗑️ Delete this expense?")   /* 🔤 V726 */)
                    /* 📄 V1177 (TK-অনুমোদিত ফটো-প্রুফ) — *"যেটুকু প্রয়োজন
                       সেটুকুই রাখুন"*: উপরে **কীসের খরচ**, তারপর **তারিখ ও টাকা**,
                       নিচে **ব্রাঞ্চ · কাকে দেওয়া**। ⛔ মোছার নিয়ম অটুট। */
                    .setMessage(
                        spentOn.text.toString().trim().ifBlank { "Other Expense" } + "\n" +
                        slashIso((dateInp.tag as? String) ?: startIso) + " · " + money(amtNow) + "\n" +
                        branch.selectedItem.toString() + "\n\n" +
                        com.tkbiswas.pilesclinic.native.NoBengali.s("Goes to Trash — can be restored.")
                    )
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        ModuleUi.toast(this@IncomeExpenseActivity, "Deleting...")
                        Thread {
                            val ok = ModuleAuth.update("fin", "expenses", "id=eq.$expId", JSONObject().put("ignored", true))
                            runOnUiThread {
                                if (ok) { clearSheetCaches(); ModuleUi.toast(this@IncomeExpenseActivity, "খরচটি মুছে ফেলা হয়েছে।"); goBack() }
                                else ModuleUi.toast(this@IncomeExpenseActivity, "মোছা গেল না (নেট?) — আবার চেষ্টা করুন")
                            }
                        }.start()
                    }
                    .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
            }
        })
        // 🔴 V413 (TK-নির্দেশ): নিচের নির্দেশ-লাইনটা তুলে দেওয়া হলো।
    }

    /* 🟢 V400: খরচ বদলানো/মোছার পরে ফোনে জমানো পুরনো খাতা যেন পুরনো সংখ্যা না দেখায় —
       তাই খাতার সব ক্যাশ মুছে দেওয়া হয় (পরের বার ক্লাউড থেকেই আসল সংখ্যা আসবে)।
       ⛔ শুধু দেখানোর ক্যাশ; ক্লাউডের কোনো তথ্য মোছে না। */
    private fun clearSheetCaches() {
        // খাতার ক্যাশ (`sheet_…`) আর দিনের হিসাবের ক্যাশ (`coll_/exp_/net_…`) —
        // দুটোই এই একই ফাইলে থাকে (income_expense_cache), তাই একবারেই মুছে দেওয়া হয়।
        try {
            val p = daySummaryCachePrefs()
            val ed = p.edit()
            for (k in p.all.keys.toList()) ed.remove(k)
            ed.apply()
        } catch (_: Throwable) { }
    }

    private fun openSheetRowEditor(date: String, existing: JSONObject?, onSaved: () -> Unit) {
        backAction = { sheet(date.substring(0, 7)) }
        /* 🎨🔒 V1175 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — TK:
           *"কোচবিহারের মধ্যে ঢুকে আয় ব্যয় তুলছে, সেখানে আবার ব্রাঞ্চ কেন সিলেক্ট
           করতে হবে · ক্যাশ এবং অনলাইন পাশাপাশি রাখুন, বক্স আকারে · খরচের লেখাটাও
           একটা বক্সের মধ্যে রাখুন যাতে প্রফেশনাল দেখতে লাগে"*।

           ⛔ **সেভের নিয়ম · টাকার অঙ্ক · অনুমতির শর্ত — কিচ্ছু বদলায়নি**; শুধু
              চেহারা। ব্রাঞ্চটা এখন **বাছতে হয় না** — যে ব্রাঞ্চের খাতা থেকে ঢোকা
              হয়েছে সেটাই উপরের পট্টিতে লেখা থাকে, আর সেভেও ঠিক সেটাই যায়
              (আগে spinner-এ ওটাই বাছা থাকত, তাই ফলে কোনো পার্থক্য নেই)।
           ⛔ তারিখের ঘরে কোনো ছবি-অক্ষর নেই (TK-নির্দেশ) — চাপলে আগের মতোই
              ক্যালেন্ডার খোলে। */
        val col = ModuleUi.screen(this, "")
        val dateInp = dateField(date)
        /* যে ব্রাঞ্চের সারি, সেটাই — না পেলে খাতার চলতি ব্রাঞ্চ। */
        val rowBranch = (existing?.s("branch")?.takeIf { it.isNotBlank() && it in BRANCHES })
            ?: (lockedBranch?.takeIf { it in BRANCHES })
            ?: homeBranch.takeIf { it in BRANCHES }
            ?: BRANCHES.first()
        col.addView(ledgerHeader("Ledger Entry", rowBranch))
        val cash = ModuleUi.numberInput(this, "", allowDecimal = true)
        val online = ModuleUi.numberInput(this, "", allowDecimal = true)
        /* 💰🔒 V1211 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"খরচ কিসে
           করছি সেটা বাক্স আলাদা রাখতে হবে"* · *"আগে রাখতে হবে কত টাকা খরচ করলাম,
           আর সেটা কিসে খরচা করলাম"* · *"আরও একটা দুটো হতে পারে, প্লাস চিহ্ন রাখতে
           হবে"*।
           ⇒ এক দিনে যতগুলো খরচ, ততগুলো **জোড়া ঘর** (কত টাকা · কীসে), আর নিচে ＋।
           ⛔ **সেভ হয় হুবহু আগের ধাঁচেই** — ঘরগুলো জুড়ে "নাম টাকা, নাম টাকা"
              লেখা বানিয়ে সেই একই `expense_notes`/`expense_total`-এ যায়। তাই
              খরচের বিবরণ · মাসের হিসাব · ছাপা — সব আগের মতোই চলে, আর পুরনো
              দিনের লেখাও এখানে ভেঙে ঘরে বসে যায় (নিচে `expLoad`)। */
        val expRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val expPairs = ArrayList<Pair<android.widget.EditText, android.widget.EditText>>()
        val totalTv = ModuleUi.body(this, "₹0")
        col.addView(ModuleUi.label(this, "Date")); col.addView(boxify(dateInp))
        /* 🎨 V1175 — Cash ও Online পাশাপাশি, সমান চওড়া (TK-নির্দেশ)। */
        col.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            addView(LinearLayout(this@IncomeExpenseActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(ModuleUi.label(this@IncomeExpenseActivity, "Cash"))
                addView(boxify(cash, big = true))
            })
            addView(LinearLayout(this@IncomeExpenseActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { leftMargin = dp(10) }
                addView(ModuleUi.label(this@IncomeExpenseActivity, "Online"))
                addView(boxify(online, big = true))
            })
        })
        col.addView(ModuleUi.label(this, "Expense / ব্যায়"))
        col.addView(expRows)
        val addMore = android.widget.TextView(this).apply {
            text = NoBengali.s("＋  আরও একটা খরচ")
            textSize = 13.5f; gravity = android.view.Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#B42318"))
            setPadding(0, dp(11), 0, dp(11))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(11).toFloat()
                setColor(android.graphics.Color.parseColor("#FFF3F1"))
                setStroke(dp(2), android.graphics.Color.parseColor("#D9A8A1"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
            isClickable = true; isFocusable = true
        }
        col.addView(addMore)
        /* 🎨 V1175 — মোট খরচ এখন নিজের একটা হালকা পট্টিতে। ⛔ অঙ্কটা
           হুবহু আগের ফাংশনেই (`sumNumbersInText`) হিসাব হয়। */
        col.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(9).toFloat()
                setColor(android.graphics.Color.parseColor("#EEF7F1"))
                setStroke(dp(1), android.graphics.Color.parseColor("#CFE2D5"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                text = "Total Expense"; textSize = 13f
                setTextColor(android.graphics.Color.parseColor("#33404F"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(totalTv.apply {
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#B42318"))
            })
        })

        /* 💰 V1211 — ঘরগুলো থেকে সেই পুরনো ধাঁচের লেখা: "নাম টাকা, নাম টাকা"।
           ⛔ নামের ভিতরে কমা থাকলে সেটা সরানো হয়, নইলে ভাঙার সময় দুটো হয়ে যেত। */
        fun expText(): String = expPairs.mapNotNull { (amtF, whatF) ->
            val a0 = amtF.text.toString().trim().replace(",", "")
            val w0 = whatF.text.toString().trim().replace(",", " ")
            val n = a0.toDoubleOrNull() ?: 0.0
            if (n == 0.0 && w0.isBlank()) null
            else (if (w0.isBlank()) "খরচ" else w0) + " " + String.format(Locale.US, "%.0f", n)
        }.joinToString(", ")

        fun refreshTotal() { totalTv.text = money(sumNumbersInText(expText())) }

        /** 💰 V1211 — একটা জোড়া ঘর (কত টাকা উপরে, কীসে নিচে)। */
        fun addExpPair(amount: String, what: String) {
            val act = this
            val amtF = ModuleUi.numberInput(act, "", allowDecimal = true).apply {
                if (amount.isNotBlank()) setText(amount)
                setTextColor(android.graphics.Color.parseColor("#B42318"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            val whatF = android.widget.EditText(act).apply {
                hint = ""
                if (what.isNotBlank()) setText(what)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            val card = LinearLayout(act).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(4), dp(12), dp(12))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(android.graphics.Color.WHITE)
                    setStroke(dp(1), android.graphics.Color.parseColor("#EBD6D2"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                addView(ModuleUi.label(act, NoBengali.s("কত টাকা খরচ করলাম")))
                addView(boxify(amtF, big = true))
                addView(ModuleUi.label(act, NoBengali.s("কীসে খরচ করলাম")))
                addView(boxify(whatF))
            }
            expPairs.add(amtF to whatF)
            expRows.addView(card)
            val w = object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) { refreshTotal() }
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            }
            amtF.addTextChangedListener(w); whatF.addTextChangedListener(w)
        }
        addMore.setOnClickListener { addExpPair("", ""); refreshTotal() }

        if (existing != null) {
            cash.setText(existing.optDouble("cash", 0.0).let { if (it == 0.0) "" else it.toInt().toString() })
            online.setText(existing.optDouble("online", 0.0).let { if (it == 0.0) "" else it.toInt().toString() })
            /* 💰 V1211 — পুরনো লেখা ভেঙে ঘরে বসানো। ⛔ ভাঙার নিয়ম **হুবহু খরচের
               বিবরণ পপ-আপেরই** (`showExpenseBreakdown`) — সংখ্যাটা শেষে, তার
               আগের অংশটাই নাম। তাই পর্দা ও এখানকার ভাঙা কখনো আলাদা হবে না। */
            val old = existing.optString("expense_notes", "").let { if (it == "null") "" else it }.trim()
            val amtRe = Regex("[0-9][0-9,]*(?:\\.[0-9]+)?")
            for (raw in old.split(",", "\n", ";")) {
                val seg = raw.trim()
                if (seg.isEmpty()) continue
                val m = amtRe.findAll(seg).lastOrNull() ?: continue
                val amt = m.value.replace(",", "").toDoubleOrNull() ?: continue
                var name = (seg.substring(0, m.range.first) + seg.substring(m.range.last + 1))
                    .trim().trim('-', ':', '=', '·', '.', '/', '(', ')').trim()
                if (name.isEmpty()) name = "খরচ"
                addExpPair(String.format(Locale.US, "%.0f", amt), name)
            }
            /* 🎨 V1175 — ব্রাঞ্চ এখন `rowBranch`-এ, উপরের পট্টিতে দেখানো হয়। */
        }
        if (expPairs.isEmpty()) addExpPair("", "")   // 💰 V1211 — শুরুতে একটাই জোড়া
        refreshTotal()

        // 🔵 খাতার সারি (TK-নির্দেশ, 09.09.2026): Save/Cancel আগে উপর-নিচে ভেসে
        // একটা আরেকটার গায়ে পড়ছিল। এখন compactFooter দিয়ে **পাশাপাশি** (মাঝে ফাঁক),
        // Cancel বাঁয়ে সাদা-আউটলাইন · Save ডানে সবুজ। ⛔ সেভের কাজ/হিসাব হুবহু অটুট।
        col.addView(compactFooter("Cancel", "Save", { onSaved() }) {
            val note = expText()   // 💰 V1211 — ঘরগুলো জুড়ে সেই পুরনো ধাঁচের লেখা
            /* 🟢🔒 V401: মাস্টার নন এমন কেউ পুরনো তারিখের সারি বদলাতে পারবেন না —
               বদলে মাস্টারের কাছে অনুরোধ যাবে। (ডেটাবেসেও আটকানো; এখানে আগেভাগে
               ধরা হয় যাতে "Saved" দেখিয়ে আসলে কিছু না-হওয়ার ভুল না ঘটে।) */
            val dNow = (dateInp.tag as? String) ?: date
            val exId = existing?.optString("id")
            if (ieRestricted && !ieIsToday(dNow)) {
                ieAskApproval(IePermit.EDIT_COLLECTION, rowBranch, dNow, exId,
                    JSONObject()
                        .put("cash", cash.text.toString().toDoubleOrNull() ?: 0.0)
                        .put("online", online.text.toString().toDoubleOrNull() ?: 0.0)
                        .put("expense_notes", note)
                        .put("expense_total", sumNumbersInText(note))) { onSaved() }
                return@compactFooter
            }
            val row = JSONObject()
                .put("entry_date", (dateInp.tag as? String) ?: date)
                .put("branch", rowBranch)
                .put("cash", cash.text.toString().toDoubleOrNull() ?: 0.0)
                .put("online", online.text.toString().toDoubleOrNull() ?: 0.0)
                .put("expense_notes", note)
                .put("expense_total", sumNumbersInText(note))
                .put("created_by", entryCreatedBy())
            // 🔴 বাগ-প্রতিরোধ: existing row এডিট করার সময় তার আসল id-ই পাঠাতে হবে,
            // নইলে upsert (merge-duplicates) primary-key না মিলে নতুন সারি বানিয়ে
            // ফেলত — একই দিনের টাকা দুইবার গোনা হয়ে যেত। id থাকলে ঠিক সেই সারিতেই আপডেট হয়।
            val existingId = existing?.optString("id")
            if (!existingId.isNullOrBlank()) row.put("id", existingId)
            ModuleUi.toast(this, "Saving...")
            Thread {
                val ok = ModuleAuth.upsert("fin", "collections", row)
                runOnUiThread { ModuleUi.toast(this, if (ok) "Saved" else "Saved offline / retry"); onSaved() }
            }.start()
        })

        // 🔴🆕 TK-অনুমোদিত প্রুফ (10.08.2026): পুরনো সারি এডিটের সময় নিচে লাল "Delete Entry"।
        // চাপলে লাল নিশ্চিতকরণ → সারিটা fin.collections-এ ignored=true (আড়াল — আজকের SQL-এর মতোই,
        // মোছে না, দরকারে ফেরানো যায়)। ⛔ শুধু existing (আগে-সেভ-করা) সারিতেই দেখায়, নতুন এন্ট্রিতে নয়।
        // 🟢🔒 V401 (TK-নির্দেশ "মোছা শুধু মাস্টার"): মাস্টার ছাড়া এই বোতাম **দেখানোই হয় না**।
        //    ডেটাবেসেও আটকানো আছে, তাই দেখালে শুধু ব্যর্থ চেষ্টা হতো।
        val delId = if (ModuleAuth.isMaster) existing?.optString("id") else null
        if (!delId.isNullOrBlank()) {
            col.addView(android.widget.Button(this).apply {
                text = "🗑️ Delete Entry"; isAllCaps = false; textSize = 15f
                setTextColor(android.graphics.Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat(); setColor(android.graphics.Color.parseColor("#B42318"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply { topMargin = dp(12) }
                setOnClickListener {
                    val dShow = slashIso((dateInp.tag as? String) ?: date)
                    val bShow = rowBranch
                    val cShow = money(cash.text.toString().toDoubleOrNull() ?: 0.0)
                    val oShow = money(online.text.toString().toDoubleOrNull() ?: 0.0)
                    androidx.appcompat.app.AlertDialog.Builder(this@IncomeExpenseActivity)
                        .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this@IncomeExpenseActivity, "🗑️ Delete this entry?"))
                        /* 📄 V1177 — একই ধাঁচ, ছোট ও পরিষ্কার (নিয়ম ৭)। */
                        .setMessage("$dShow · $bShow\nCash $cShow · Online $oShow\n\n" +
                            com.tkbiswas.pilesclinic.native.NoBengali.s("Goes to Trash — can be restored."))
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            ModuleUi.toast(this@IncomeExpenseActivity, "Deleting...")
                            Thread {
                                val ok = ModuleAuth.update("fin", "collections", "id=eq.$delId",
                                    JSONObject().put("ignored", true))
                                runOnUiThread {
                                    ModuleUi.toast(this@IncomeExpenseActivity, if (ok) "Entry deleted." else "Could not delete (network?) — try again.")
                                    if (ok) onSaved()
                                }
                            }.start()
                        }
                        .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
                }
            })
        }
    }

    /**
     * 🟢🔒 V630 (২৪.০৮.২০২৬, TK-নির্দেশ) — Sheet-এর Cash/Online ঘরে ৩-বার
     * চাপে শুধু **সেই একটা সংখ্যার** জন্য ছোট, দ্রুত এডিটর — পুরো সারির ফর্ম
     * (Date/Branch/Cash/Online/Expense) খোলার দরকার নেই। "আয়" (Add
     * Collection)-এর আলাদা পর্দাটার জায়গা এখন এটাই নিল, সরাসরি সেই দিনের
     * সারিতেই। ⛔ পুরনো-তারিখের একই অনুমতি-নিয়ম (ieRestricted/ieAskApproval)
     * অক্ষত। ⛔ শুধু ডাকা `field`-টাই বদলায় (আংশিক PATCH) — বাকি ঘর
     * (Date/Branch/অন্য field/Expense) এক অক্ষরও ছোঁয়া হয় না। নতুন
     * (id-বিহীন) সারি হলে নতুন সারি বানানো হয় — `addCollection()`-এর
     * প্রমাণিত insert-পথেই।
     */
    private fun quickFieldEditor(row: JSONObject, date: String, branch: String, field: String, label: String, onSaved: () -> Unit) {
        val input = ModuleUi.numberInput(this, label, allowDecimal = true)
        val current = row.optDouble(field, 0.0)
        if (current > 0.0) input.setText(if (current == Math.floor(current)) current.toLong().toString() else current.toString())
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "$label — ${slashIso(date)}"))
            .setView(entryCard(listOf(label to input)))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val v = input.text.toString().toDoubleOrNull() ?: 0.0
                val id = row.optString("id")
                if (ieRestricted && !ieIsToday(date)) {
                    ieAskApproval(if (id.isNotBlank()) IePermit.EDIT_COLLECTION else IePermit.ADD_COLLECTION,
                        branch, date, id.ifBlank { null }, JSONObject().put(field, v)) { onSaved() }
                    return@setPositiveButton
                }
                ModuleUi.toast(this, "Saving...")
                Thread {
                    val ok = if (id.isNotBlank()) {
                        ModuleAuth.update("fin", "collections", "id=eq.$id", JSONObject().put(field, v))
                    } else {
                        val newRow = JSONObject()
                            .put("entry_date", date).put("branch", branch)
                            .put("cash", if (field == "cash") v else 0.0)
                            .put("online", if (field == "online") v else 0.0)
                            .put("created_by", entryCreatedBy())
                        ModuleAuth.insertChecked("fin", "collections", newRow).ok
                    }
                    runOnUiThread {
                        ModuleUi.toast(this, if (ok) "Saved" else "Could not save — try again")
                        if (ok) onSaved()
                    }
                }.start()
            }
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    /** হোমের আজকের Cash/Online — এক চাপেই দিনের মোট অঙ্ক বসানো। */
    private fun quickTodayIncomeEditor(field: String, label: String) {
        val branch = homeBranch
        if (branch !in BRANCHES) {
            ModuleUi.toast(this, "উপরে একটি ব্রাঞ্চ বাছুন")
            return
        }
        val date = todayIso()
        val input = ModuleUi.numberInput(this, label, allowDecimal = true)
        val cached = cachedDaySummary(date, branch)
        val current = if (field == "cash") cached?.cashColl ?: 0.0 else cached?.onlineColl ?: 0.0
        if (current > 0.0) input.setText(if (current == Math.floor(current)) current.toLong().toString() else current.toString())
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "$label — ${slashIso(date)}"))
            .setView(entryCard(listOf(label to input)))
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val desired = input.text.toString().toDoubleOrNull() ?: 0.0
                if (desired < 0.0) { ModuleUi.toast(this, "Enter Amount")   /* 🔤 V726 */; return@setPositiveButton }
                ModuleUi.toast(this, "Saving...")
                Thread {
                    val bq = java.net.URLEncoder.encode(branch, "UTF-8").replace("+", "%20")
                    val rowsR = ModuleAuth.getRowsChecked("fin", "collections",
                        "select=*&entry_date=eq.$date&branch=eq.$bq&ignored=eq.false&order=created_at")
                    var ok = rowsR.ok
                    if (ok && rowsR.rows.length() == 0) {
                        val row = JSONObject().put("entry_date", date).put("branch", branch)
                            .put("cash", if (field == "cash") desired else 0.0)
                            .put("online", if (field == "online") desired else 0.0)
                            .put("created_by", entryCreatedBy())
                        ok = ModuleAuth.insertChecked("fin", "collections", row).ok
                    } else if (ok) {
                        var otherTotal = 0.0
                        for (i in 1 until rowsR.rows.length()) otherTotal += rowsR.rows.getJSONObject(i).optDouble(field, 0.0)
                        if (desired < otherTotal) ok = false
                        else {
                            val first = rowsR.rows.getJSONObject(0)
                            val id = first.optString("id")
                            ok = id.isNotBlank() && ModuleAuth.updateAtLeastOne(
                                "fin", "collections", "id=eq.$id", JSONObject().put(field, desired - otherTotal))
                        }
                    }
                    runOnUiThread {
                        if (ok) { ModuleUi.toast(this, "Saved"); renderMenu() }
                        else ModuleUi.toast(this, "Could not save — open Full Ledger and check this date")
                    }
                }.start()
            }
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }


    // Expense পর্দা সহজ করা হলো। TK: "সর্বমোট জিনিসটা একটু কঠিন লাগছে, সহজ করে
    // দিন।" নতুন সাজ: (১) উপরে "আজকের হিসাব" কার্ড নিজে থেকেই দেখায় (আলাদা Daily
    // Ledger-এ ঢোকা লাগে না), (২) নিচে ঠিক ৪টা সমান বক্স (২×২) — টাকা জমা · খরচ /
    // এই মাসের হিসাব · পুরো খাতা। ⛔ হিসাবের নিয়ম/ডেটা/অন্য পর্দা (addCollection/
    // addExpense/monthly/sheet/dailyLedger) একটুও বদলায়নি — শুধু এই মেনু-পর্দার সাজ;
    // ওই ফাংশনগুলোই ডাকা হচ্ছে। ⛔ ক্যালেন্ডার-ইমোজি (স্থির "17 July") বাদ — আসল
    // লাইভ তারিখ দেখানো হয় (কার্ডের শিরোনামে ও "এই মাসের হিসাব" বক্সের আইকনে)।
    private fun renderMenu() {
        backAction = { finish() }
        // 🔵🔒 TK-প্রুফ (09.08.2026): "Back একদম নিচে, বক্স ছোট" — তাই এই পর্দায় নিজস্ব
        // fillViewport ScrollView + নিচে-ঠেলা spacer, যাতে বক্স ছোট হলেও Back পর্দার
        // একদম নিচে বসে। ⛔ শুধু এই মেনু-পর্দার সাজ; হিসাব/ডেটা/অন্য পর্দা কিছু বদলায়নি।
        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(16))
        }
        scroll.addView(col)
        setContentView(scroll)

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
        val titleTv = android.widget.TextView(this).apply {
            text = "💵 " + NoBengali.s("টাকার হিসাব"); textSize = 19f
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleTv)
        // 🟢🆕 TK-অনুমোদিত প্রুফ (10.08.2026): হোমে ব্রাঞ্চ-চিপ। চাপলে ব্রাঞ্চ বাছা যায় →
        // "আজকের হিসাব" কার্ড ওই ব্রাঞ্চেরই দেখায়। ⛔ অন্য পর্দা/হিসাব কিছু বদলায় না।
        val homeBranchItems = (listOf("All Branches") + BRANCHES).toTypedArray()
        val branchLocked = lockedBranch != null   // 🔵 B617: ডাক্তার হলে ব্রাঞ্চ বদলানো যাবে না।
        if (!branchLocked) homeBranch = v398Branch()   // 🟢🔒 V398: মনে-রাখা ব্রাঞ্চ
        headerRow.addView(android.widget.TextView(this).apply {
            text = if (branchLocked) "🏥 " + homeBranch else "🏥 " + homeBranch + " ▾"; textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#E7F6EC"))
                setStroke(dp(1), android.graphics.Color.parseColor("#B7E3C5"))
            }
            isClickable = !branchLocked
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(8) }
            if (!branchLocked) setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this@IncomeExpenseActivity)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this@IncomeExpenseActivity, "Select Branch")   /* 🔤 V726 */)
                    .setItems(homeBranchItems) { _, which ->
                        homeBranch = homeBranchItems[which]
                        v398Remember(homeBranch)   // 🟢🔒 V398: সব পর্দার জন্য মনে রাখা
                        renderMenu()
                    }
                    .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
            }
        })
        headerRow.addView(ModuleUi.liveDateIconButton(this) { pickDateForSummary() })
        /* ⋮🔒 V1216 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"এই মাসের হিসাব ·
           পুরো খাতা · statement · অংশীদারী ভাগ · entry permission — এই সবগুলো উপরে
           LG-3 ডট এর মধ্যে রাখুন। সেই ক্ষেত্রে ব্রাঞ্চ আর ক্যালেন্ডার আরেকটু বাঁদিকে
           করুন, আর তার ডান পাশে থাকবে LG-3 ডট"*।
           ⛔ **কে কোনটা দেখবেন সেই নিয়ম এক অক্ষরও বদলায়নি** — নিচের `menuItems()`
              হুবহু `buildActionGrid()`-এর শর্তগুলোই মানে (staff-only-today ·
              master · অংশীদার-ডাক্তার)।
           ⛔ পর্দাগুলো নিজে কিছুই বদলায়নি — শুধু পৌঁছানোর পথ।
           ⛔ ⛔ 📅 ইমোজি কোথাও বসানো হয়নি (TK-র চিরস্থায়ী নিষেধ, সারি ২১৩)। */
        /* 🔎🔒 V1254 (০৯.০৯.২০২৬, TK-রিপোর্ট ছবিসহ, খাতার সারি ৩৭৬) — TK:
           *"উপরে ব্রাঞ্চ সিলেক্ট এবং ক্যালেন্ডার একটু বাঁ দিকে সরান, অন্যথায়
           ⋮ চাপ দিতে পারছি না"*।
           **কারণ (কোডে মেপে):** ⋮-এর ডান দিকের ফাঁক ছিল মাত্র ২dp, তাই ওর
           ছোঁয়ার জায়গাটা পর্দার একদম কিনারা ঘেঁষে বসত — আঙুল পড়ত পাশে।
           ⇒ ডানে ১২dp ও বাঁয়ে ১৬dp ফাঁক ⇒ ছোঁয়ার জায়গা বড়, আর ব্রাঞ্চ ও
             ক্যালেন্ডার নিজে থেকেই একটু বাঁ দিকে সরে যায় (TK যা চেয়েছেন)।
           ⛔ **উঁচু-নিচু ফাঁক ছোঁয়া হয়নি**, তাই হেডারের উচ্চতা এক চুলও বাড়ে না।
           ⛔ লেখা · রং · মাপ · মেনুর সারি — কিচ্ছু বদলায়নি।
           🔁 প্রজেক্টের **সব ⋮-এই** একই মাপ বসানো হলো (Staff Profile · Work
              Notebook · Money Handover · Doctor Reminder · Monthly Summary ·
              RMP Commission Sheet) — একই ধরনের সমস্যা যেন আর কোথাও না থাকে। */
        headerRow.addView(android.widget.TextView(this).apply {
            text = "⋮"; textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setPadding(dp(16), dp(2), dp(12), dp(2))   // 🔎 V1254
            isClickable = true; isFocusable = true
            setOnClickListener { v -> showIeMenu(v) }
        })
        col.addView(headerRow)

        col.addView(buildTodaySummaryCard())
        // 🔵 spacer — Back-কে একদম নিচে ঠেলে দেয়
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.button(this, "Back") { finish() })
    }

    /** "আজকের হিসাব" কার্ড — আজকের নগদ/অনলাইন জমা, মোট জমা ও মোট খরচ। পর্দা খুললেই
     *  একবার ক্লাউড থেকে আজকের তথ্য টেনে বসায় (আগে Daily Ledger-এ চাপ দিলে ঠিক এই
     *  একটাই কল হতো — Supabase free-plan-এ বাড়তি কিছু নয়)। ব্যর্থ হলে "—" দেখায়। */
    // 🔵🔒 B617 (11.08.2026, TK-অনুমোদিত প্রুফ "সাজ ক"): আজকের হিসাব এখন একটা
    // প্রফেশনাল টেবিল-কার্ড — সবুজ হেডার, কলাম (Cash · Online · মোট), সারি আয় (সবুজ) ·
    // ব্যয় (লাল) · অবশিষ্ট (নীল ব্যান্ড = আয়−ব্যয়, Cash ও Online আলাদা)। খরচের
    // Cash/Online আসে fin.expenses-এর `mode` ঘর থেকে (আগেই ছিল)। ⛔ মোট ব্যয় আগের
    // মতোই (inline collection-খরচ mode-হীন বলে Cash-এ ধরা হয়, তাই যোগফল অটুট)।
    private fun buildTodaySummaryCard(): LinearLayout {
        val iso = todayIso()
        fun dpf(v: Int) = dp(v).toFloat()
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpf(16)
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#E2ECE6"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
        // ---- সবুজ হেডার (উপরের কোণ গোল) ----
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(12), dp(15), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(android.graphics.Color.parseColor("#0B4F2A"), android.graphics.Color.parseColor("#16A34A"))
                cornerRadii = floatArrayOf(dpf(16), dpf(16), dpf(16), dpf(16), 0f, 0f, 0f, 0f)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val title = android.widget.TextView(this).apply {
            text = "💵 " + NoBengali.s("আজকের হিসাব"); textSize = 14f
            setTextColor(android.graphics.Color.WHITE); setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)
        // 🔴 V414 (TK-রিপোর্ট, ছবিসহ, ১৭.০৮.২০২৬): *"staff এর ফোনে Jalpaiguri ২ বার
        //    কেন দেখাচ্ছে"* — উপরের হেডারে ব্রাঞ্চের নাম (`🏥 Jalpaiguri 🔒`), আর ঠিক
        //    তার নিচে এই কার্ডের মাথাতেও একই নাম। স্টাফ/ডাক্তারের ব্রাঞ্চ তো বাঁধা,
        //    বদলানোই যায় না — তাই দ্বিতীয়বার লেখাটা শুধু জায়গা নষ্ট করছিল।
        //    ⇒ ব্রাঞ্চ বাঁধা থাকলে এই চিপটা আর বসে না।
        //    ⛔ Master-এর পর্দা অপরিবর্তিত — তিনি ব্রাঞ্চ বদলাতে পারেন, তাই কার্ডে
        //       কোন ব্রাঞ্চের হিসাব দেখাচ্ছে সেটা লেখা থাকা দরকার।
        //    ⛔ কোনো হিসাব · সংখ্যা · রং কিছুই ছোঁয়া হয়নি।
        // 🔴 V415 (TK-নির্দেশ, ১৭.০৮.২০২৬): *"Branch এর নাম ২ জায়গায় কেন?"* —
        //    উপরের হেডারে নামটা আছে, তাই কার্ডের মাথায় আর বসানো হয় না।
        //    Master ও Staff — সবার ক্ষেত্রেই এক নিয়ম।
        card.addView(header)

        fun cellTv(weight: Float, color: String, bold: Boolean): android.widget.TextView =
            android.widget.TextView(this).apply {
                text = "…"; textSize = 13.5f; gravity = android.view.Gravity.END
                setTextColor(android.graphics.Color.parseColor(color))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            }
        fun headCell(txt: String, weight: Float, gravityEnd: Boolean): android.widget.TextView =
            android.widget.TextView(this).apply {
                text = txt; textSize = 11f
                gravity = if (gravityEnd) android.view.Gravity.END else android.view.Gravity.START
                setTextColor(android.graphics.Color.parseColor("#6A7D72")); setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            }
        fun labelCell(txt: String, color: String): android.widget.TextView =
            android.widget.TextView(this).apply {
                text = txt; textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(color))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
            }
        fun rowBox(): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(10), dp(15), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // কলাম-হেডার
        val colH = rowBox().apply {
            setPadding(dp(15), dp(8), dp(15), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor("#F3F7F4")) }
        }
        colH.addView(headCell("", 1.1f, false)); colH.addView(headCell("Cash", 1f, true))
        colH.addView(headCell("Online", 1f, true)); colH.addView(headCell("মোট", 1.05f, true))
        card.addView(colH)

        // আয় সারি
        val incRow = rowBox()
        val incLabelTv = labelCell("আয়", "#0A7C3F")
        val incCashTv = cellTv(1f, "#12704A", false); val incOnlineTv = cellTv(1f, "#12704A", false); val incTotTv = cellTv(1.05f, "#0A7C3F", true)
        incRow.addView(incLabelTv); incRow.addView(incCashTv); incRow.addView(incOnlineTv); incRow.addView(incTotTv)
        // 🔴🔴🔒 V659 (২৫.০৮.২০২৬, TK-কড়া-সংশোধন) — **আমার নিজের ভুল স্বীকার
        // করে ঠিক করা:** V658-এ এখানে ভুল করে `addCollection()`
        // (আলাদা ফর্ম) খুলতাম — কিন্তু TK এক দিন আগেই V630-এ স্পষ্ট
        // নির্দেশ দিয়েছিলেন: *"আয় এবং ব্যয় এটা দুই রকম ভাবে আলাদা কলম
        // থাকবে না... এখন থেকে 📄 পুরো খাতা-ই একমাত্র পথ: Cash/Online
        // ঘরে ৩-চাপে... টাকা ঢোকানো যায়।"* — অর্থাৎ আলাদা ফর্ম খোলা
        // **স্পষ্টভাবে বাতিল করা একটা পুরনো ডিজাইন**, যেটা আমি গভীরে
        // না গিয়ে ভুল করে আবার চালু করে ফেলেছিলাম। এখন ঠিক করা হলো:
        // "আয়" চাপলে সরাসরি "পুরো খাতা" (Sheet)-এ যায়, আজকের মাসেই
        // খোলে — সেখানেই TK-এর লক করা, প্রমাণিত ৩-চাপ পদ্ধতিতে টাকা
        // ঢোকানো যাবে। কোনো নতুন ফর্ম/পথ তৈরি হয়নি।
        // TK-এর বর্তমান নির্দেশ: কয়েকবার নয়—Cash/Online ঘরে এক চাপেই
        // সেই নির্দিষ্ট আজকের আয়ের ঘর খুলবে। পুরো খাতা আর খুলবে না।
        incCashTv.isClickable = true; incCashTv.isFocusable = true
        incCashTv.setOnClickListener { quickTodayIncomeEditor("cash", "Cash") }
        incOnlineTv.isClickable = true; incOnlineTv.isFocusable = true
        incOnlineTv.setOnClickListener { quickTodayIncomeEditor("online", "Online") }
        val chooseIncomeMode = {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Add Income"))
                .setItems(arrayOf("Cash", "Online")) { _, which ->
                    if (which == 0) quickTodayIncomeEditor("cash", "Cash")
                    else quickTodayIncomeEditor("online", "Online")
                }
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
        }
        incLabelTv.isClickable = true; incLabelTv.setOnClickListener { chooseIncomeMode() }
        incTotTv.isClickable = true; incTotTv.setOnClickListener { chooseIncomeMode() }
        card.addView(incRow)
        card.addView(android.view.View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)); setBackgroundColor(android.graphics.Color.parseColor("#F1F5F2")) })

        // ব্যয় সারি
        val expRow = rowBox()
        val expCashTv = cellTv(1f, "#B0392B", false); val expOnlineTv = cellTv(1f, "#B0392B", false); val expTotTv = cellTv(1.05f, "#B42318", true)
        expRow.addView(labelCell("ব্যয়", "#B42318")); expRow.addView(expCashTv); expRow.addView(expOnlineTv); expRow.addView(expTotTv)
        // 🔴🔴🔒 V659 (২৫.০৮.২০২৬, TK-কড়া-সংশোধন) — একই ভুল-সংশোধন,
        // "ব্যয়"-এও। V630-এর লক করা নিয়ম মেনে সরাসরি "পুরো খাতা"
        // (Sheet)-এ যায়, আজকের মাসেই খোলে — নতুন কোনো আলাদা ফর্ম না।
        expRow.isClickable = true; expRow.isFocusable = true
        expRow.setOnClickListener {
            if (homeBranch !in BRANCHES) ModuleUi.toast(this, "উপরে একটি ব্রাঞ্চ বাছুন")
            else addExpense(todayIso(), homeBranch)
        }
        card.addView(expRow)

        // অবশিষ্ট ব্যান্ড (নীল, নিচের কোণ গোল)
        val remRow = rowBox().apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(android.graphics.Color.parseColor("#0B2B59"), android.graphics.Color.parseColor("#155EAE"))
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, dpf(16), dpf(16), dpf(16), dpf(16))
            }
        }
        val remCashTv = cellTv(1f, "#DBE9FF", false); val remOnlineTv = cellTv(1f, "#DBE9FF", false); val remTotTv = cellTv(1.05f, "#FFFFFF", true).apply { textSize = 14.5f }
        remRow.addView(labelCell("অবশিষ্ট", "#FFFFFF")); remRow.addView(remCashTv); remRow.addView(remOnlineTv); remRow.addView(remTotTv)
        card.addView(remRow)

        Thread {
            val branchQ = if (homeBranch != "All Branches")
                "&branch=eq." + java.net.URLEncoder.encode(homeBranch, "UTF-8").replace("+", "%20") else ""
            val collR = ModuleAuth.getRowsChecked("fin", "collections", "select=*&entry_date=eq.$iso&ignored=eq.false$branchQ&order=created_at.asc")
            val expR = ModuleAuth.getRowsChecked("fin", "expenses", "select=*&entry_date=eq.$iso&ignored=eq.false$branchQ&order=created_at.asc")
            if (!collR.ok || !expR.ok) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    for (t in listOf(incCashTv, incOnlineTv, incTotTv, expCashTv, expOnlineTv, expTotTv, remCashTv, remOnlineTv, remTotTv)) t.text = "—"
                    title.text = "💵 " + NoBengali.s("আজকের হিসাব") + "  (" + NoBengali.s("লোড হয়নি") + ")"
                }
                return@Thread
            }
            val coll = collR.rows; val exp = expR.rows
            var incCash = 0.0; var incOnline = 0.0
            for (i in 0 until coll.length()) {
                incCash += coll.getJSONObject(i).optDouble("cash", 0.0)
                incOnline += coll.getJSONObject(i).optDouble("online", 0.0)
            }
            var expCash = 0.0; var expOnline = 0.0
            for (i in 0 until exp.length()) {
                val e = exp.getJSONObject(i); val amt = e.optDouble("amount", 0.0)
                if (e.optString("mode", "Cash").equals("Online", true)) expOnline += amt else expCash += amt
            }
            for (i in 0 until coll.length()) {
                val c = coll.getJSONObject(i)
                val et = c.optDouble("expense_total", -1.0)
                expCash += if (et >= 0.0) et else sumNumbersInText(c.optString("expense_notes", "").let { if (it == "null") "" else it })
            }
            val incTot = incCash + incOnline; val expTot = expCash + expOnline
            val remCash = incCash - expCash; val remOnline = incOnline - expOnline
            saveDaySummaryCache(iso, homeBranch, DaySummary(incCash, incOnline, expCash, expOnline))
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                fun bare(n: Double) = money(n).removePrefix("₹")
                incCashTv.text = bare(incCash); incOnlineTv.text = bare(incOnline); incTotTv.text = money(incTot)
                expCashTv.text = bare(expCash); expOnlineTv.text = bare(expOnline); expTotTv.text = money(expTot)
                remCashTv.text = bare(remCash); remOnlineTv.text = bare(remOnline); remTotTv.text = money(remCash + remOnline)
            }
        }.start()
        return card
    }

    private fun todaySummaryRow(parent: LinearLayout, label: String, valueColor: String, bold: Boolean): android.widget.TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val l = android.widget.TextView(this).apply {
            text = label; textSize = 13.5f
            setTextColor(android.graphics.Color.parseColor(if (bold) valueColor else "#2B4B3A"))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val v = android.widget.TextView(this).apply {
            text = "…"; textSize = 13.5f; gravity = android.view.Gravity.END
            setTextColor(android.graphics.Color.parseColor(valueColor))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        row.addView(l); row.addView(v)
        parent.addView(row)
        return v
    }

    /** নিচের ৪টা সমান বক্স (২×২): টাকা জমা · খরচ / এই মাসের হিসাব · পুরো খাতা। */
    /* ⋮ V1216 — মেনুর সারিগুলো। ⛔ শর্তগুলো `buildActionGrid()`-এর হুবহু নকল,
       তাই কে কী দেখবেন তা কোথাও বদলায় না। ⛔ `buildActionGrid()` মোছা হয়নি
       (TK-নিয়ম: নিজে থেকে কোড মোছা হয় না) — শুধু আর ডাকা হয় না। */
    private fun ieMenuItems(): List<Pair<String, () -> Unit>> {
        val out = ArrayList<Pair<String, () -> Unit>>()
        val staffOnlyToday = ieRestricted && !ieIsDoctor && !isPartnerDoctor
        if (staffOnlyToday) {
            out.add("Today's Entries" to { dailyLedger() })
        } else {
            out.add(NoBengali.s("এই মাসের হিসাব") to { monthly() })
            out.add(NoBengali.s("পুরো খাতা") to { sheet() })
            out.add("📄 Statement" to { statement() })
        }
        if (ModuleAuth.isMaster || isPartnerDoctor) {
            out.add("🤝 " + NoBengali.s("অংশীদারি ভাগ") to {
                startActivity(android.content.Intent(this, PartnerSharesActivity::class.java))
            })
        }
        if (ModuleAuth.isMaster) out.add("Entry Permission" to { entryPermission() })
        return out
    }

    private fun showIeMenu(anchor: android.view.View) {
        val items = ieMenuItems()
        if (items.isEmpty()) return
        val pm = android.widget.PopupMenu(this, anchor)
        items.forEachIndexed { i, (label, _) -> pm.menu.add(0, i, i, label) }
        pm.setOnMenuItemClickListener { mi ->
            items.getOrNull(mi.itemId)?.second?.invoke(); true
        }
        pm.show()
    }

    private fun buildActionGrid(): LinearLayout {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        /* 🟢🔒 V630 (২৪.০৮.২০২৬, TK-নির্দেশ) — "আয় এবং ব্যয় এটা দুই রকম ভাবে
           আলাদা কলম থাকবে না।" আলাদা "Add Collection"/"Add Expense" পর্দা খোলার
           বোতাম দুটো সরানো হলো — এখন থেকে "📄 পুরো খাতা"-ই (Sheet) একমাত্র
           পথ: Cash/Online ঘরে ৩-চাপে বা খালি খরচ ঘরে চাপ দিয়েই টাকা ঢোকানো যায়।
           ⛔ `addCollection()`/`addExpense()` ফাংশন দুটো মোছা হয়নি (TK-নিয়ম:
              নিজে থেকে কোড মোছা হয় না) — `addExpense()` এখনো Sheet-এর খালি
              খরচ-ঘর থেকে ডাকা হয়, `addCollection()` আপাতত অব্যবহৃত রাখা হলো। */

        /* 🟢🔒 V401 (TK-নির্দেশ): কে কী দেখবে।
             মাস্টার        → আগের মতোই সব + Entry Permission
             ডাক্তার        → পুরো খাতা ও মাসের হিসাব দেখতে পাবেন (বদলাতে পারবেন না)
             স্টাফ          → শুধু আজকের — খাতা ও মাসের হিসাব **দেখানোই হবে না**
                              (TK: "এগুলি দেখানোর কোন দরকার আছে কি?") — বদলে "Today's Entries"
           ⛔ আসল আটকানো ডেটাবেসেই (V401 RLS); এটা শুধু পর্দার সাজ। */
        val ieStaffOnlyToday = ieRestricted && !ieIsDoctor && !isPartnerDoctor
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        if (ieStaffOnlyToday) {
            row2.addView(actionBox(null, "Today's Entries", "#EEF7F1", "#EEF7F1", "#0A5C33", false) { dailyLedger() })
        } else {
            row2.addView(actionBox(null, "এই মাসের হিসাব", "#EEF7F1", "#EEF7F1", "#0A5C33", false) { monthly() })
            row2.addView(actionBox(null, "পুরো খাতা", "#EEF7F1", "#EEF7F1", "#0A5C33", false) { sheet() })
        }

        /* 🟢🔒 V629 (২৪.০৮.২০২৬, TK-নির্দেশ) — "ব্যাংকে যেমন স্টেটমেন্ট বের করা
           যায়, আমার অ্যাপেও সেরকম চাই।" যেকোনো From–To তারিখের মধ্যে, প্রতিদিনের
           পরে **চলতি ব্যালেন্স (running balance)** দেখানো — যাতে হাতের হিসাবের
           সাথে ঠিক কোন দিনে মিল ভাঙছে তা চোখেই দেখা যায়। Staff-only-today
           দেখবেন না (Ledger Sheet/Monthly-র মতোই বিধিনিষেধ)। */
        val row2b = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        if (!ieStaffOnlyToday) {
            row2b.addView(actionBox(null, "📄 Statement", "#EEF7F1", "#EEF7F1", "#0A5C33", false) { statement() })
        }
        // 🔵 V306 (TK-নির্দেশ, ১০.০৮): টাকার হিসাবের ভেতরেই "অংশীদারি ভাগ" — আলাদা
        // PartnerSharesActivity খোলে (isolated)। ⛔ পুরনো কোনো হিসাব/স্ক্রিন ছোঁয়া হয়নি।
        val row3 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        // অংশীদারি ভাগ — মাস্টার ও অংশীদার-ডাক্তার (আগের মতোই)। চাবি-পাওয়া সাধারণ
        // staff/doctor-এর কাছে দেখানো হয় না; ওই পর্দার নিজের নিরাপত্তা আগের মতোই অটুট।
        if (ModuleAuth.isMaster || isPartnerDoctor) {
            row3.addView(actionBox(null, "🤝 অংশীদারি ভাগ", "#EEF7F1", "#EEF7F1", "#0A5C33", false) {
                startActivity(android.content.Intent(this, PartnerSharesActivity::class.java))
            })
        }
        // 🟢🆕 V401: শুধু মাস্টার — কে আয়-খরচ তুলতে পারবে তার চাবি।
        if (ModuleAuth.isMaster) {
            row3.addView(actionBox(null, "Entry Permission", "#FFF7E6", "#FFF7E6", "#6A5320", false) { entryPermission() })
        }
        wrap.addView(row1) // 🟢🔒 V630 — এখন খালি (childCount 0), তাই কোনো ফাঁকা জায়গা দেখাবে না
        wrap.addView(row2)
        if (row2b.childCount > 0) wrap.addView(row2b)
        if (row3.childCount > 0) wrap.addView(row3)
        return wrap
    }

    /* =====================================================================
       🟢🆕🔒 V401 — Entry Permission (শুধু মাস্টার)
       TK-অনুমোদিত মকআপ: ব্রাঞ্চ বেছে নিলে সেই ব্রাঞ্চের staff ও doctor-দের নাম,
       পাশে চালু/বন্ধ। লেখা ইংরেজিতে, কোনো icon নেই (TK-নির্দেশ)।
       ⛔ চাবি লেখা যায় শুধু মাস্টারের হাতে (ডেটাবেসের ie_permits_master নীতি)।
       ===================================================================== */
    private fun entryPermission() {
        if (!ModuleAuth.isMaster) { finish(); return }
        backAction = { renderMenu() }
        var branchSel = BRANCHES.firstOrNull() ?: "Birpara"

        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6")); isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(16))
        }
        scroll.addView(col); setContentView(scroll)

        val listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var reload: () -> Unit = {}

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(12), dp(13))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(android.graphics.Color.parseColor("#0B4F2A"),
                                    android.graphics.Color.parseColor("#16A34A"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        header.addView(android.widget.TextView(this).apply {
            text = "←  Entry Permission"; textSize = 17f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true; setOnClickListener { renderMenu() }
        })
        val chip = android.widget.TextView(this).apply {
            text = "$branchSel ▾"; textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#88FFFFFF"))
            }
            isClickable = true
        }
        chip.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Select Branch"))
                .setItems(BRANCHES.toTypedArray()) { _, which ->
                    branchSel = BRANCHES[which]; chip.text = "$branchSel ▾"; reload()
                }
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
        }
        header.addView(chip)
        col.addView(header)
        col.addView(listBox)
        col.addView(android.widget.TextView(this).apply {
            text = "ON = may add today's Income & Expense for this branch, and may edit their own entry on the same day. " +
                   "Older dates need Master approval. Delete stays with Master only."
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#5C6B62"))
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(android.graphics.Color.parseColor("#F7FAF8"))
                setStroke(dp(1), android.graphics.Color.parseColor("#DDE7E0"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        })
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.button(this, "Back") { renderMenu() })

        reload = {
            listBox.removeAllViews()
            listBox.addView(ModuleUi.body(this, "Loading…"))
            val b = branchSel
            Thread {
                val r = ModuleAuth.rpc("fin", "ie_permit_candidates", JSONObject().put("p_branch", b))
                val rows = try { JSONArray(r.body) } catch (_: Throwable) { JSONArray() }
                runOnUiThread {
                    listBox.removeAllViews()
                    if (!r.ok) { listBox.addView(ModuleUi.body(this, "Could not load — try again.")); return@runOnUiThread }
                    if (rows.length() == 0) { listBox.addView(ModuleUi.body(this, "No staff or doctor found for this branch.")); return@runOnUiThread }
                    val card = ModuleUi.card(this)
                    for (i in 0 until rows.length()) {
                        val p = rows.getJSONObject(i)
                        card.addView(ieCandidateRow(p, b) { reload() })
                        if (i < rows.length() - 1) card.addView(cardDivider())
                    }
                    listBox.addView(card)
                }
            }.start()
        }
        reload()
    }

    /** Entry Permission-এর একটা সারি — নাম, নিচে ছোট করে role · branch · Partner, ডানে সুইচ। */
    private fun ieCandidateRow(p: JSONObject, branch: String, onDone: () -> Unit): LinearLayout {
        val code = p.s("person_code")
        val on = p.optBoolean("can_entry", false)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }
        val names = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        names.addView(android.widget.TextView(this).apply {
            text = p.s("full_name").ifBlank { code }; textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#16241D"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        val sub = StringBuilder()
        sub.append(p.s("role_kind").replaceFirstChar { it.uppercase() }).append(" · ").append(branch)
        if (p.optBoolean("is_partner", false)) sub.append(" · Partner")
        names.addView(android.widget.TextView(this).apply {
            text = sub.toString(); textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#7A8A80"))
        })
        row.addView(names)

        val sw = android.widget.Switch(this).apply {
            isChecked = on
            setOnCheckedChangeListener { view, checked ->
                if (!view.isPressed) return@setOnCheckedChangeListener
                view.isEnabled = false
                val patch = JSONObject()
                    .put("person_code", code).put("branch", branch)
                    .put("can_entry", checked).put("updated_by", ModuleAuth.personCode ?: "master")
                try {
                    val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    f.timeZone = TimeZone.getTimeZone("UTC")
                    patch.put("updated_at", f.format(java.util.Date()))
                } catch (_: Throwable) { }
                Thread {
                    // 🔒 PK এখানে (person_code, branch) — id নয়, তাই on_conflict স্পষ্ট করে
                    //    বলা হয়; নইলে একই মানুষের দুই ব্রাঞ্চের চাবি ধাক্কা খেতে পারত।
                    val ok = ModuleAuth.upsertOnConflict("fin", "entry_permits", patch, "person_code,branch")
                    runOnUiThread {
                        view.isEnabled = true
                        if (ok) ModuleUi.toast(this@IncomeExpenseActivity,
                            if (checked) "Turned ON" else "Turned OFF")
                        else { view.isChecked = !checked
                               ModuleUi.toast(this@IncomeExpenseActivity, "Could not save — try again") }
                    }
                }.start()
            }
        }
        row.addView(sw)
        return row
    }

    /* =====================================================================
       🟢🆕🔒 V401 — "Master approval required" — পুরনো তারিখে কিছু করতে গেলে।
       TK-অনুমোদিত মকআপ: কারণ লিখে "Send Request"; মাস্টারের ঘণ্টায় গিয়ে বসে।
       ===================================================================== */
    private fun ieAskApproval(
        kind: String, branch: String, entryDateIso: String,
        targetId: String?, payload: JSONObject, onSent: () -> Unit
    ) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(4))
        }
        box.addView(android.widget.TextView(this).apply {
            text = slashIso(entryDateIso) + " is not today, so it cannot be changed directly.\n\n" +
                   "Send a request to Master. Write what you want to change and why."
            textSize = 14.5f
            setTextColor(android.graphics.Color.parseColor("#22312A"))
        })
        val reason = android.widget.EditText(this).apply {
            hint = "Reason"
            minLines = 2
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        box.addView(reason)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Master approval required"))
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send Request") { _, _ ->
                ModuleUi.toast(this, "Sending…")
                val why = reason.text.toString()
                Thread {
                    val res = IePermit.sendRequest(kind, branch, entryDateIso, targetId, payload, why)
                    runOnUiThread {
                        if (res.ok) { ModuleUi.toast(this, "Request sent to Master."); onSent() }
                        else ModuleUi.toast(this, res.message.ifBlank { "Could not send — try again" })
                    }
                }.start()
            }
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun actionBox(icon: android.view.View?, label: String, topColor: String, bottomColor: String, fg: String, gradient: Boolean, onClick: () -> Unit): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(dp(8), dp(12), dp(8), dp(12))
            minimumHeight = dp(44)
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                if (gradient) {
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.TL_BR
                    colors = intArrayOf(
                        android.graphics.Color.parseColor(topColor),
                        android.graphics.Color.parseColor(bottomColor)
                    )
                } else {
                    setColor(android.graphics.Color.parseColor(topColor))
                    setStroke(dp(1), android.graphics.Color.parseColor("#CFE4D6"))
                }
            }
            setOnClickListener { onClick() }
        }
        if (icon != null) box.addView(icon)   // 🔵 TK-প্রুফ: বক্সে কোনো আইকন/ইমোজি নেই — শুধু লেখা
        box.addView(android.widget.TextView(this).apply {
            text = label; textSize = 14f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor(fg))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        box.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(5), 0, dp(5), 0)
        }
        return box
    }

    private fun emojiIcon(s: String, color: String): android.widget.TextView =
        android.widget.TextView(this).apply {
            text = s; textSize = 19f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor(color))
        }

    private fun liveCalIcon(): LinearLayout {
        val cal = java.util.Calendar.getInstance()
        val mon = java.text.SimpleDateFormat("MMM", java.util.Locale.ENGLISH).format(cal.time)
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(7).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#BFDCC9"))
            }
        }
        box.addView(android.widget.TextView(this).apply {
            text = mon; textSize = 7.5f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#B42318"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        box.addView(android.widget.TextView(this).apply {
            text = day; textSize = 11f; gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        return box
    }

    // 🔴🔒 V452 (TK-নির্দেশ ১৮.০৮.২০২৬: "All branch কেন দেখাবে, cash/online
    // আলাদা কেন দেখাবে না") — এখন হোমের ব্রাঞ্চ-চিপ (`homeBranch`) মেনেই
    // ফিল্টার হয় ("All Branches" বাছা থাকলে আগের মতোই সব ব্রাঞ্চ), আর
    // Cash/Online আলাদা করে দেখানো হয় (Collection ও Expense দুটোতেই)।
    private fun pickDateForSummary() {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        android.app.DatePickerDialog(this, { _, y, m, d ->
            val iso = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
            val dotted = String.format(Locale.US, "%02d/%02d/%04d", d, m + 1, y)
            showDaySummary(iso, dotted)
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    // 🔴🔒 B504 (06.08.2026, TK-নির্দেশ, সাবধানে করার শর্তে) — দিনের
    // টাকার সারাংশ (Collection/Expense/Net) আগে সবসময় ক্লাউডের উত্তরের
    // অপেক্ষা করে "Loading..." দেখাত। এখন আগের সফল হিসাব এই ফোনে জমা
    // থাকে — থাকলে **সাথে সাথেই** (স্পষ্ট "শেষ জানা তথ্য, হালনাগাদ
    // হচ্ছে..." লেখা-সহ) দেখানো হয়, তারপর ক্লাউড থেকে আসল/হালনাগাদ
    // সংখ্যা এলে সেই একই ডায়ালগ বন্ধ করে আসল সংখ্যা দিয়ে আবার দেখানো
    // হয় — টাকার সংখ্যা কখনো "শেষ জানা" আর "আসল" গুলিয়ে একসাথে
    // দেখানো হয় না, দুটো ধাপ স্পষ্ট আলাদা। ⛔ হিসাবের নিয়ম (Ledger
    // Sheet-এর expense_total যোগ করা-সহ) এক অক্ষরও বদলায়নি।
    private fun daySummaryCachePrefs() = getSharedPreferences("income_expense_cache", MODE_PRIVATE)
    // 🔒 B602 (10.08.2026): Daily Ledger খাতার cache-first (raw collections rows + prevBal)।
    // ⛔ শুধু দেখানো দ্রুত করা — buildSheetTable-এর হিসাব-লজিক অপরিবর্তিত।
    private fun loadSheetCache(key: String): Triple<JSONArray, Double, Boolean>? {
        return try {
            val s = daySummaryCachePrefs().getString(key, null) ?: return null
            val o = JSONObject(s)
            Triple(o.getJSONArray("rows"), o.optDouble("prevBal", 0.0), o.optBoolean("prevOk", true))
        } catch (_: Throwable) { null }
    }
    private fun saveSheetCache(key: String, rows: JSONArray, prevBal: Double, prevOk: Boolean) {
        try {
            daySummaryCachePrefs().edit()
                .putString(key, JSONObject().put("rows", rows).put("prevBal", prevBal).put("prevOk", prevOk).toString())
                .apply()
        } catch (_: Throwable) { }
    }
    private fun cachedDaySummary(iso: String, branchKey: String): DaySummary? {
        return try {
            val p = daySummaryCachePrefs()
            val k = branchKey + "|" + iso
            if (!p.contains("cashC_$k")) return null
            DaySummary(
                p.getFloat("cashC_$k", 0f).toDouble(), p.getFloat("onlC_$k", 0f).toDouble(),
                p.getFloat("cashE_$k", 0f).toDouble(), p.getFloat("onlE_$k", 0f).toDouble()
            )
        } catch (_: Throwable) { null }
    }
    private fun saveDaySummaryCache(iso: String, branchKey: String, s: DaySummary) {
        try {
            val k = branchKey + "|" + iso
            daySummaryCachePrefs().edit()
                .putFloat("cashC_$k", s.cashColl.toFloat()).putFloat("onlC_$k", s.onlineColl.toFloat())
                .putFloat("cashE_$k", s.cashExp.toFloat()).putFloat("onlE_$k", s.onlineExp.toFloat())
                .apply()
        } catch (_: Throwable) { }
    }
    /** V452 — Collection/Expense-এর Cash ও Online আলাদা রাখা হয়, যাতে পপ-আপে
     *  দুটোই আলাদা করে দেখানো যায় (আগে শুধু যোগফল থাকত)। */
    private data class DaySummary(val cashColl: Double, val onlineColl: Double, val cashExp: Double, val onlineExp: Double) {
        val totalColl get() = cashColl + onlineColl
        val totalExp get() = cashExp + onlineExp
        val net get() = totalColl - totalExp
    }

    /**
     * 🟢🔒 V1440 (১৩.০৯.২০২৬, TK ছবি-প্রুফে "Option C" পাশ — খাতার সারি ৫৬৪)
     * "TK: এটার চেহারা চেঞ্জ করতে হবে" → তিনটে ডিজাইন দেখানো হলো, TK এটা
     * পাশ করলেন — বড় Net সংখ্যা আগে, নিচে Collection/Expense-এর তুলনা-বার।
     * ⛔ হিসাব একটুও বদলায়নি — dialog_day_summary.xml ভরাট করাই এই ফাংশনের কাজ।
     */
    private fun buildDaySummaryView(s: DaySummary, stale: Boolean): android.view.View {
        val v = layoutInflater.inflate(com.tkbiswas.pilesclinic.R.layout.dialog_day_summary, null)
        v.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDsNetAmount).text = money(s.net)
        v.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDsCollAmount).text = money(s.totalColl)
        v.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDsCollSplit).text =
            "Cash " + money(s.cashColl) + "  ·  Online " + money(s.onlineColl)
        v.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDsExpAmount).text = money(s.totalExp)
        v.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDsExpSplit).text =
            "Cash " + money(s.cashExp) + "  ·  Online " + money(s.onlineExp)
        // Expense-বার Collection-এর তুলনায় (০–১০০% বাঁধা); Collection ০ হলে ও
        // খরচ থাকলে পুরো লাল বার — Collection নিজে সবসময় পূর্ণ (নিজের ভিত্তি)।
        val ratio = when {
            s.totalColl > 0.0 -> (s.totalExp / s.totalColl).coerceIn(0.0, 1.0)
            s.totalExp > 0.0 -> 1.0
            else -> 0.0
        }
        val fill = v.findViewById<android.view.View>(com.tkbiswas.pilesclinic.R.id.viewDsExpFill)
        val spacer = v.findViewById<android.view.View>(com.tkbiswas.pilesclinic.R.id.viewDsExpSpacer)
        (fill.layoutParams as? LinearLayout.LayoutParams)?.let { it.weight = ratio.toFloat(); fill.layoutParams = it }
        (spacer.layoutParams as? LinearLayout.LayoutParams)?.let { it.weight = (1.0 - ratio).toFloat(); spacer.layoutParams = it }
        if (stale) {
            v.findViewById<android.widget.TextView>(com.tkbiswas.pilesclinic.R.id.tvDsStale).visibility = android.view.View.VISIBLE
        }
        return v
    }

    private fun showDaySummary(iso: String, dotted: String) {
        fun enc(x: String) = try { java.net.URLEncoder.encode(x, "UTF-8") } catch (_: Throwable) { x }
        // V452 — homeBranch-এর হুবহু সেই ব্রাঞ্চ-চিপ; "All Branches" হলে ফিল্টার নেই।
        val branchSel = homeBranch
        val branchQ = if (branchSel != "All Branches") "&branch=eq." + enc(branchSel) else ""
        val branchKey = branchSel
        val title = dotted + " · " + branchSel
        var instantDialog: androidx.appcompat.app.AlertDialog? = null
        val cached = cachedDaySummary(iso, branchKey)
        if (cached != null) {
            instantDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title))
                .setView(buildDaySummaryView(cached, stale = true))
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
        } else {
            ModuleUi.toast(this, "Loading...")
        }
        Thread {
            val collR = ModuleAuth.getRowsChecked("fin", "collections", "select=*&entry_date=eq.$iso&ignored=eq.false$branchQ&order=created_at.asc")
            val expR = ModuleAuth.getRowsChecked("fin", "expenses", "select=*&entry_date=eq.$iso&ignored=eq.false$branchQ&order=created_at.asc")
            if (!collR.ok || !expR.ok) {
                runOnUiThread {
                    if (cached == null) {
                        try { instantDialog?.dismiss() } catch (_: Throwable) { }
                        ModuleUi.toast(this, "লোড করা গেল না — একটু পরে আবার দেখুন")
                    }
                    // শেষ-জানা তথ্য থাকলে সেই ডায়ালগই থাক — ₹0 দিয়ে ঢাকা হবে না।
                }
                return@Thread
            }
            val coll = collR.rows
            val exp = expR.rows
            var cashColl = 0.0; var onlineColl = 0.0
            for (i in 0 until coll.length()) {
                val c = coll.getJSONObject(i)
                cashColl += c.optDouble("cash", 0.0)
                onlineColl += c.optDouble("online", 0.0)
            }
            /* 🐞🔒 V1322 (TK-রিপোর্ট ছবিসহ, ১১.০৯.২০২৬: "দুই পর্দায় দুই রকম কেন") —
               এই পপ-আপ শুধু `fin.collections`-এর **হাতে-লেখা** সারি পড়ত। যেদিন
               এখনও কেউ হাতে "নতুন দিন" এন্ট্রি বসাননি, ওই টেবিলে সারিই নেই ⇒
               সৎভাবে ₹0 দেখাত — অথচ Payment স্ক্রিন আসল টাকা (payments টেবিল
               থেকে) ঠিকই দেখাত। Ledger Sheet ও Monthly Summary অনেক আগে থেকেই
               এই ফাঁক ভরে দেয় `autoIncomeByDate()` দিয়ে (V929) — এই একটা
               পপ-আপেই সেটা ডাকা হতো না। এখন এখানেও ডাকা হলো, শুধু তখনই যখন
               `fin.collections`-এ সত্যিই কোনো হাতে-লেখা সারি নেই — থাকলে সেই
               সারিই চলবে, আগের মতো (V929-এর নিয়মের সঙ্গে হুবহু মেলানো)। */
            if (coll.length() == 0) {
                try {
                    val auto = autoIncomeByDate(iso.substring(0, 7), branchSel)[iso]
                    if (auto != null) { cashColl = auto.first; onlineColl = auto.second }
                } catch (_: Throwable) { }
            }
            var cashExp = 0.0; var onlineExp = 0.0
            for (i in 0 until exp.length()) {
                val e = exp.getJSONObject(i)
                val amt = e.optDouble("amount", 0.0)
                if (e.optString("mode", "CASH").uppercase(Locale.US) == "ONLINE") onlineExp += amt else cashExp += amt
            }
            // 🔴 একই টাকা যেন সব জায়গায় এক দেখায়: Ledger Sheet-এর expense_total-ও
            // যোগ (এই সারিগুলোর মোড জানা নেই, তাই Cash-এ ধরা হয় — আগের কোডেও
            // এগুলো mode-নির্বিশেষে মোট expense-এই যুক্ত হতো)।
            for (i in 0 until coll.length()) {
                val c = coll.getJSONObject(i)
                val et = c.optDouble("expense_total", -1.0)
                if (et >= 0.0) cashExp += et
                else cashExp += sumNumbersInText(c.optString("expense_notes", "").let { if (it == "null") "" else it })
            }
            val summary = DaySummary(cashColl, onlineColl, cashExp, onlineExp)
            saveDaySummaryCache(iso, branchKey, summary)
            runOnUiThread {
                try { instantDialog?.dismiss() } catch (_: Throwable) { }
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title))
                    .setView(buildDaySummaryView(summary, stale = false))
                    .setPositiveButton("OK", null)
                    .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
            }
        }.start()
    }

    /* 🔴🔒 V415 (TK-নির্দেশ, ১৭.০৮.২০২৬) — **"Paid To"-তে সংখ্যা বসানো আটকানো।**
       TK ছবি দিয়ে ধরালেন: PAID TO ঘরে `2000` লেখা, অথচ ওখানে **নাম** থাকার কথা।
       আগে কোনো পাহারা ছিল না, তাই ভুল করে অঙ্ক বসিয়ে সেভ করা যেত — পরে খাতায়
       "RMP Commission · 2000" দেখে কাকে টাকা দেওয়া হয়েছিল বোঝাই যেত না।
       ⇒ নাম ফাঁকা হলে, বা নামে একটাও অক্ষর না থাকলে (শুধু সংখ্যা/চিহ্ন) সেভ আটকায়।
       ⛔ টাকার অঙ্ক · ক্যাটেগরি · তারিখ · মোড — কোনো নিয়ম বদলায়নি। */
    private fun ieBadPaidTo(name: String): Boolean {
        val t = name.trim()
        if (t.isEmpty()) return true
        return t.none { it.isLetter() }
    }

    private fun spinner(items: List<String>): Spinner {
        val sp = Spinner(this)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        return sp
    }

    // 🔴 B312 (03.08.2026, TK-অনুমোদিত মকআপ — "বাস্তবে যেন এটাই হয়") — সবুজ
    // গ্রেডিয়েন্ট হিরো হেডার + বর্ডার-করা কার্ডে ফিল্ড + ছোট পাশাপাশি
    // Save/Cancel বা Show/Back বোতাম। Staff Profile-এর View স্ক্রিনে (B308)
    // যে একই টেকনিক প্রমাণিত হয়েছে সেটাই এখানে পুনর্ব্যবহার — নতুন প্যাটার্ন
    // আবিষ্কার করা হয়নি।
    private fun hero(title: String): LinearLayout {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(android.graphics.Color.parseColor("#0B4F2A"), android.graphics.Color.parseColor("#0B8A3E"))
            ).apply { cornerRadius = dp(16).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(12) }
        }
        h.addView(android.widget.TextView(this).apply {
            text = title; textSize = 19f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
        })
        return h
    }

    /**
     * 🟢🔒 V891 (৩০.০৮.২০২৬, TK ডেমো ফটো দেখে **"হ্যাঁ পাশ, বসিয়ে দিন"**) —
     * Monthly Summary-র উপরের হেডার **আরো কম্প্যাক্ট ও প্রফেশনাল**।
     *
     * TK-এর কথা: *"উপরের হেডার আরো কম্প্যাক্ট হবে … উচ্চতায় আরো কম হবে"* ও
     * *"হেডারে এত গ্রিন থাকবেনা"*।
     *
     * আগে: সবুজ গ্রেডিয়েন্ট হেডার, নিচে Month ও Branch দুটো আলাদা ঘর (দুই সারি)।
     * এখন: **একটাই সাদা কার্ড, একটাই সারি** — বাঁয়ে সবুজ শিরোনাম, ডানে Month ও
     *      Branch দুটো ছোট পিল। উচ্চতা প্রায় অর্ধেক, টেবিলের সারি বেশি দেখা যায়।
     *
     * ⛔ শুধু এই পর্দাটার সাজ। টেবিল · নিচের মোট আয়/ব্যয়/অবশিষ্ট · টাকার
     *    কোনো হিসাব — এক অক্ষরও ছোঁয়া হয়নি (TK: *"সেগুলোতে কোন পরিবর্তন করতে
     *    আপনাকে বলা হয় নাই"*)।
     */
    private fun heroWithFields(title: String, leftView: android.view.View,
                               rightView: android.view.View,
                               extraView: android.view.View? = null): LinearLayout {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(9), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#DCE6E0"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(8) }
        }
        h.addView(android.widget.TextView(this).apply {
            text = title; textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        // ডানের দুটো ছোট পিল — হালকা সবুজ-ধূসর, ভিতরের লেখা গাঢ় সবুজ।
        fun pill(field: android.view.View): LinearLayout {
            val b = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(9), dp(5), dp(9), dp(5))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(android.graphics.Color.parseColor("#F1F6F3"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#D6E4DC"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = dp(6) }
            }
            b.addView(field)
            return b
        }
        h.addView(pill(leftView)); h.addView(pill(rightView))
        /* ⋮ V1217 — ঐচ্ছিক তৃতীয় ভিউ (পিল ছাড়া), Monthly Summary-র ⋮ মেনুর জন্য।
           ⛔ ডিফল্ট null ⇒ এই হেল্পারের বাকি সব ডাক এক অক্ষরও বদলায়নি। */
        if (extraView != null) h.addView(extraView)
        return h
    }

    private fun dp(v: Int) = ModuleUi.dp(this, v)

    /** কার্ডের ভিতরে এক সারি — লেবেল (ছোট, ধূসর) + ফিল্ড (স্বাভাবিক, আন্ডারলাইন-বিহীন)। */
    private fun fieldInCard(label: String, field: android.view.View): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(9), dp(14), dp(9))
        }
        row.addView(android.widget.TextView(this).apply {
            text = label; textSize = 10.5f
            setTextColor(android.graphics.Color.parseColor("#8A9A90"))
            isAllCaps = true; letterSpacing = 0.02f
        })
        if (field is android.widget.EditText) {
            field.background = null
            field.textSize = 14.5f
            field.setTextColor(android.graphics.Color.parseColor("#1C2B22"))
            field.setPadding(0, dp(2), 0, 0)
        } else if (field is Spinner) {
            field.setPadding(0, dp(2), 0, 0)
        }
        row.addView(field)
        return row
    }

    private fun cardDivider(): android.view.View = android.view.View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        setBackgroundColor(android.graphics.Color.parseColor("#F0F5F1"))
    }

    private fun entryCard(fields: List<Pair<String, android.view.View>>): LinearLayout {
        val card = ModuleUi.card(this)
        for ((i, pair) in fields.withIndex()) {
            card.addView(fieldInCard(pair.first, pair.second))
            if (i < fields.size - 1) card.addView(cardDivider())
        }
        return card
    }

    /** ছোট পাশাপাশি বোতাম-জোড়া — বাঁয়ে সাদা/বর্ডার (secondary), ডানে ভরাট সবুজ (primary)। */
    /* 🟢🔒 V693 (২৬.০৮.২০২৬, TK-নির্দেশ ছবিসহ) — মাঝখানে একটা ঐচ্ছিক বোতাম
       (Monthly-র "\u2022\u2022\u2022 Options")। ⛔ `middleText` ডিফল্ট ফাঁকা, তাই এই
       ফাংশনের পুরনো সব ডাক (Back/Show) এক অক্ষরও বদলায়নি — ফাঁকা হলে
       মাঝের বোতামটা বসেই না, আগের মতো দুটোই থাকে। */
    /* 🎨🔒 V1175 (০৭.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — Ledger Entry-র
       উপরের পট্টি: বাঁয়ে নাম, ডানে **যে ব্রাঞ্চের খাতা** সেটার নাম।
       ⛔ শুধু দেখানো — এখানে কিছু বাছা যায় না, তাই ভুল ব্রাঞ্চে সেভ হওয়ার
          কোনো পথও থাকে না (আগে spinner-এ ভুল করে অন্য ব্রাঞ্চ বাছা যেত)। */
    private fun ledgerHeader(title: String, branch: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(12), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(
                    android.graphics.Color.parseColor("#123F86"),
                    android.graphics.Color.parseColor("#16A34A")
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
            addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                text = title; textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(android.widget.TextView(this@IncomeExpenseActivity).apply {
                text = branch; textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setPadding(dp(11), dp(5), dp(11), dp(5))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(android.graphics.Color.parseColor("#33FFFFFF"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#66FFFFFF"))
                }
            })
        }

    /* 🎨🔒 V1175 — ঘরটাকে **বাক্স** বানায় (TK: *"বক্স আকারে"*)।
       ⛔ শুধু সাজ — ঘরের id · লেখা · কীবোর্ড · সেভ কিচ্ছু বদলায় না।
       `big = true` হলে টাকার ঘর: বড়, মোটা, ডানে-ঘেঁষা সবুজ। */
    private fun <T : android.view.View> boxify(v: T, big: Boolean = false): T {
        v.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(9).toFloat()
            setColor(android.graphics.Color.parseColor("#FAFDFB"))
            setStroke(dp(1), android.graphics.Color.parseColor("#D7E0DA"))
        }
        v.setPadding(dp(11), dp(9), dp(11), dp(9))
        if (v is android.widget.TextView) {
            if (big) {
                v.textSize = 17f
                v.setTypeface(v.typeface, android.graphics.Typeface.BOLD)
                v.gravity = android.view.Gravity.END
                v.setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            } else v.setTextColor(android.graphics.Color.parseColor("#123A26"))
        }
        (v.layoutParams as? LinearLayout.LayoutParams)?.let { it.topMargin = dp(2) }
        return v
    }

    private fun compactFooter(
        secondaryText: String,
        primaryText: String,
        onSecondary: () -> Unit,
        middleText: String = "",
        onMiddle: (android.view.View) -> Unit = { },
        onPrimary: () -> Unit
    ): LinearLayout {
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(12) }
        }
        val secBtn = ModuleUi.button(this, secondaryText, onSecondary)
        secBtn.setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
        secBtn.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.WHITE)
            setStroke(dp(1), android.graphics.Color.parseColor("#CFE9D8"))
        }
        secBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
        val priBtn = ModuleUi.button(this, primaryText, onPrimary)
        priBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) }
        footer.addView(secBtn)
        if (middleText.isNotBlank()) {
            // মাঝের বোতাম — Back-এর মতোই সাদা/সবুজ-পাড়, TK-এর ছবির মতো।
            val midBtn = ModuleUi.button(this, middleText) { }
            midBtn.setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
            midBtn.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#CFE9D8"))
            }
            midBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { marginStart = dp(6); marginEnd = dp(6) }
            midBtn.setOnClickListener { v -> onMiddle(v) }
            footer.addView(midBtn)
        }
        footer.addView(priBtn)
        return footer
    }

    // 🔵🔒 Add Collection নতুন সাজ (09.08.2026, TK-প্রুফ অনুমোদিত): (১) তারিখ read-only,
    // চাপলে ক্যালেন্ডার (পুরনো দিনও বাছা যায়), দেখায় dd/mm/yyyy, কোনো ইমোজি নেই —
    // ভেতরে yyyy-mm-dd (.tag) অটুট। (২) Branch একবার বাছলে ওখানেই থাকে (spinner)।
    // (৩) "Save & Add More" — টাকা জমা হয় কিন্তু পর্দাতেই থাকে; নগদ/অনলাইন/নোট ফাঁকা
    // হয়ে যায় (তারিখ+ব্রাঞ্চ থাকে) যাতে আরেকটা যোগ করা যায়। (৪) "← Back" চাপলে তবেই
    // আগের পর্দায়। ⛔ সেভ/হিসাবের নিয়ম বদলায়নি — একই fin.collections-এ insert,
    // একই কলাম। শুধু খালি টাকায় ভুল-সারি ঠেকাতে ছোট সুরক্ষা (নগদ+অনলাইন দুটোই ০ হলে
    // সেভ হয় না — নতুন Add-More পথে ভুল করে ফাঁকা সারি না জমে)।
    // 🔵🔒 Add Collection নতুন সাজ (09.08.2026, TK-প্রুফ অনুমোদিত): CHECK-UP-এর মতো হেডার —
    // বাঁয়ে ←+"Add Collection", ডানে ব্রাঞ্চ-চিপ ("🏥 Select ▾") + ↻। ফর্মে আলাদা Branch/Note
    // ঘর নেই। ব্রাঞ্চ প্রতিবার নিজে বাছতে হবে (না বাছলে Save নয়); একবার বাছলে Save&Add More-এ
    // থেকে যায়। ↻ = পর্দা নতুন করে (ফর্ম ফাঁকা)। ⛔ সেভ/হিসাব অটুট — শুধু সাজ ও ব্রাঞ্চ-বাছাই স্থান।
    private fun addCollection() {
        backAction = { renderMenu() }
        ieSaveBusy = false   // 🔒 V418: পর্দা খুললেই তালা খোলা — কখনো আটকে থাকে না
        // 🔵 TK-নির্দেশ (09.08.2026): Back/Save একদম নিচে — তাই fillViewport স্ক্রল + spacer।
        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(16))
        }
        scroll.addView(col)
        setContentView(scroll)
        var selectedBranch: String? = lockedBranch   // 🔵 B617: ডাক্তার হলে নিজের ব্রাঞ্চ প্রি-সেট + লক
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(12), dp(13))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(
                    android.graphics.Color.parseColor("#123F86"),
                    android.graphics.Color.parseColor("#16A34A")
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        header.addView(android.widget.TextView(this).apply {
            text = "←  Add Collection"; textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true
            setOnClickListener { renderMenu() }
        })
        val branchChip = android.widget.TextView(this).apply {
            text = if (selectedBranch != null) "🏥 $selectedBranch" else "🏥 Select ▾"; textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#88FFFFFF"))
            }
            isClickable = (selectedBranch == null)
        }
        if (selectedBranch == null) branchChip.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Select Branch"))
                /* 🟢🔒 V401: মাস্টার নন এমন কারো সামনে **শুধু নিজের ব্রাঞ্চই** আসে —
                   অন্য ব্রাঞ্চ বেছে ফেলে সেভ করতে গিয়ে ব্যর্থ হওয়ার ঝামেলা থাকে না।
                   (ডেটাবেসেও আটকানো; এটা শুধু সামনের তালিকা।) */
                .setItems(ieBranchChoices().filter { it != "All Branches" }.toTypedArray()) { _, which ->
                    val opts = ieBranchChoices().filter { it != "All Branches" }
                    selectedBranch = opts[which]
                    branchChip.text = "🏥 " + opts[which] + " ▾"
                }
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
        }
        header.addView(branchChip)
        header.addView(android.widget.TextView(this).apply {
            text = "↻"; textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(dp(10), 0, dp(2), 0)
            isClickable = true
            setOnClickListener { addCollection() }
        })
        col.addView(header)

        val date = dateField()
        val cash = ModuleUi.numberInput(this, "Cash", allowDecimal = true)
        val online = ModuleUi.numberInput(this, "Online", allowDecimal = true)
        col.addView(entryCard(listOf(
            "Date" to date, "Cash Collection" to cash, "Online Collection" to online
        )))
        // 🔵 spacer — Back/Save একদম নিচে ঠেলে দেয়
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(compactFooter("← Back", "Save & Add More", { renderMenu() }) {
            /* 🔴🔒 V418 (TK-রিপোর্ট, ১৭.০৮.২০২৬ — *"এত ডুপ্লিকেট কেন হবে"*):
               ১৩/০৩/২০২৬-এর একই এন্ট্রি তিনবার বসে গিয়েছিল, তিনটেই এক সেকেন্ডের
               মধ্যে (০৮:২২:৫১→৫২)। ডুপ্লিকেট-সতর্কতা আগেও ছিল, কিন্তু সেটা ক্লাউডে
               জিজ্ঞাসা করে — তিনটে চাপই উত্তর আসার **আগেই** বেরিয়ে গিয়েছিল।
               এখন একটা সেভ পুরো শেষ না হওয়া পর্যন্ত দ্বিতীয় সেভ শুরুই হয় না।
               ⛔ প্রতিটা বেরোনোর পথে তালা খোলা হয় ⇒ বোতাম চিরতরে আটকানোর
                  কোনো সুযোগ নেই। ⛔ হিসাব/সেভের নিয়ম এক অক্ষরও বদলায়নি। */
            if (ieSaveBusy) return@compactFooter
            ieSaveBusy = true
            val br = selectedBranch
            if (br == null) {
                ieSaveBusy = false
                ModuleUi.toast(this, "উপরে ডানে ব্রাঞ্চ বাছুন")
                return@compactFooter
            }
            val cashV = cash.text.toString().toDoubleOrNull() ?: 0.0
            val onlineV = online.text.toString().toDoubleOrNull() ?: 0.0
            if (cashV <= 0.0 && onlineV <= 0.0) {
                ieSaveBusy = false
                ModuleUi.toast(this, "নগদ বা অনলাইন — অন্তত একটা লিখুন")
                return@compactFooter
            }
            val entryDate = (date.tag as? String) ?: todayIso()
            /* 🟢🔒 V401 (TK-নির্দেশ): "দিনের দিন হতে হবে / পুরাতন হিসাব তুলতে গেলে
               Master এর অনুমতি লাগবে"। মাস্টার হলে আগের মতোই সরাসরি বসে। */
            if (ieRestricted && !ieIsToday(entryDate)) {
                ieSaveBusy = false
                ieAskApproval(IePermit.ADD_COLLECTION, br, entryDate, null,
                    JSONObject().put("cash", cashV).put("online", onlineV)) {
                    cash.setText(""); online.setText("")
                }
                return@compactFooter
            }
            val row = JSONObject()
                .put("entry_date", entryDate)
                .put("branch", br)
                .put("cash", cashV)
                .put("online", onlineV)
                .put("created_by", entryCreatedBy())
            fun encV(x: String) = try { java.net.URLEncoder.encode(x, "UTF-8").replace("+", "%20") } catch (_: Throwable) { x }
            val doInsert = {
                ModuleUi.toast(this, "Saving...")
                Thread {
                    val res = ModuleAuth.insertChecked("fin", "collections", row)
                    runOnUiThread {
                        ieSaveBusy = false
                        /* 🔴🔒 V418 (TK-নির্দেশ): ডেটাবেস নিজেই এখন একই দিনের · একই
                           ব্রাঞ্চের · হুবহু একই Cash+Online দ্বিতীয়বার বসতে দেয় না।
                           সেটা আটকালে **সৎ কথা** বলা হয় — "Saved" বলা হয় না।
                           ⛔ নেট-সমস্যা আর ডুপ্লিকেট — দুটো আলাদা বার্তা। */
                        val msg = when {
                            res.ok -> "Saved — আরেকটা যোগ করতে পারেন"
                            res.duplicate -> "এই দিনে এই অঙ্ক আগেই জমা আছে — আর যোগ হয়নি"
                            else -> "Saved offline / retry"
                        }
                        ModuleUi.toast(this, msg)
                        // 🔵 সফল হলে পর্দাতেই থাকে; টাকার ঘর ফাঁকা (তারিখ+ব্রাঞ্চ থাকে)।
                        if (res.ok) { cash.setText(""); online.setText("") }
                    }
                }.start()
            }
            // 🔵🆕 TK-অনুমোদিত (10.08.2026): একই দিনে · একই ব্রাঞ্চে · ঠিক একই Cash+Online আগে
            // থেকে থাকলে সেভের আগে সবুজ সতর্কতা (দুবার-ওঠা ঠেকাতে)। কখনো জোর করে আটকায় না।
            ModuleUi.toast(this, "Checking...")
            Thread {
                val dup = ModuleAuth.getRows("fin", "collections",
                    "select=id&entry_date=eq.$entryDate&branch=eq.${encV(br)}&cash=eq.$cashV&online=eq.$onlineV&ignored=eq.false&limit=1")
                runOnUiThread {
                    if (dup.length() > 0) {
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Duplicate entry?"))
                            .setMessage("This looks identical to an entry already saved for this date and branch — add anyway?")
                            .setPositiveButton("Add anyway") { _, _ -> doInsert() }
                            .setNegativeButton("Cancel") { _, _ -> ieSaveBusy = false }
                            .setOnCancelListener { ieSaveBusy = false }
                            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
                    } else doInsert()
                }
            }.start()
        })
    }

    // 🔵🔒 Add Expense নতুন সাজ (09.08.2026, TK-অনুমোদিত — Add Collection-এর মতোই): তারিখ
    // read-only, চাপলে ক্যালেন্ডার (dd/mm/yyyy, ইমোজি নেই, ভেতরে yyyy-mm-dd)। "Save & Add More"
    // চাপলে জমা হয় কিন্তু পর্দাতেই থাকে — Amount/Paid To/Note ফাঁকা হয় (তারিখ+ব্রাঞ্চ+ক্যাটেগরি+মোড
    // থাকে) যাতে আরেকটা যোগ করা যায়; "← Back" চাপলে তবেই ফেরে। ⛔ ডুপ্লিকেট-গার্ড/সেভ/হিসাব
    // কিছু বদলায়নি — শুধু তারিখ-ঘর ও ফুটার। খালি (০) Amount-এ সেভ নয় (ভুল-সারি ঠেকাতে)।
    private fun addExpense(prefillDate: String? = null, prefillBranch: String? = null) {
        backAction = if (prefillDate != null) { { sheet(prefillDate.substring(0, 7)) } } else { { renderMenu() } }
        ieSaveBusy = false   // 🔒 V418: পর্দা খুললেই তালা খোলা — কখনো আটকে থাকে না
        // 🔵🔒 TK-প্রুফ (09.08.2026): Add Collection-এর মতোই তবে উপরে লাল হেডার। ফর্মে Branch/Note
        // ঘর নেই; ব্রাঞ্চ ও Category প্রতিবার নিজে বাছতে হবে; Back/Save একদম নিচে (fillViewport+spacer)।
        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(16))
        }
        scroll.addView(col)
        setContentView(scroll)
        var selectedBranch: String? = lockedBranch ?: prefillBranch   // 🔵 B617 · 🟢🔒 V630: Sheet থেকে এলে প্রি-ফিল
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(12), dp(13))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(
                    android.graphics.Color.parseColor("#7A1212"),
                    android.graphics.Color.parseColor("#C0271B")
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        header.addView(android.widget.TextView(this).apply {
            text = "←  Add Expense — " + NoBengali.s("ব্যয়"); textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true
            setOnClickListener { renderMenu() }
        })
        val branchChip = android.widget.TextView(this).apply {
            text = if (selectedBranch != null) "🏥 $selectedBranch" else "🏥 Select ▾"; textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#88FFFFFF"))
            }
            isClickable = (selectedBranch == null)
        }
        if (selectedBranch == null) branchChip.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Select Branch"))
                /* 🟢🔒 V401: মাস্টার নন এমন কারো সামনে **শুধু নিজের ব্রাঞ্চই** আসে —
                   অন্য ব্রাঞ্চ বেছে ফেলে সেভ করতে গিয়ে ব্যর্থ হওয়ার ঝামেলা থাকে না।
                   (ডেটাবেসেও আটকানো; এটা শুধু সামনের তালিকা।) */
                .setItems(ieBranchChoices().filter { it != "All Branches" }.toTypedArray()) { _, which ->
                    val opts = ieBranchChoices().filter { it != "All Branches" }
                    selectedBranch = opts[which]
                    branchChip.text = "🏥 " + opts[which] + " ▾"
                }
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
        }
        header.addView(branchChip)
        header.addView(android.widget.TextView(this).apply {
            text = "↻"; textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(dp(10), 0, dp(2), 0)
            isClickable = true
            setOnClickListener { addExpense(prefillDate, prefillBranch) }
        })
        col.addView(header)

        /* 📝🔒 V1203 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"আমি এখানে
           ক্যাটাগরি চাইছি না · একটা ব্লাঙ্ক বক্স থাকবে · কিসে কিসে খরচা করলাম আমি
           টাইপ করে লিখব · Category ও Paid To — এরকম দুটো বক্স আলাদা থাকবে না,
           একটাই বক্স থাকবে ঐচ্ছিক"* আর *"আগে ব্যয় করার অপশনটা থাকবে, তারপরে থাকবে
           ব্যয়টা কি কারণে করা হলো ঐচ্ছিক"*।
           ⇒ Category-র তালিকা-ঘর ও Paid To — দুটো মিলে **একটাই লেখার ঘর**
             ("Spent On", ঐচ্ছিক), আর ক্রম: Date → Amount → Mode → Spent On।
           🔬 কোডে মেপে দেখা: `category` ঘরটা শুধু **লেখা দেখানোর** কাজে লাগে
             (দিনের খাতা · মাসের সারাংশ · ছাপা) — কোনো যোগফল বা হিসাব ওটা ধরে হয়
             না, তাই টাকার কোনো হিসাব এতে ভাঙে না।
           ⛔ পুরনো সারিগুলো যেমন ছিল তেমনই থাকে (category + paid_to দুটোই দেখায়)। */
        val date = dateField(prefillDate ?: todayIso())
        val spentOn = ModuleUi.input(this, "Spent On (optional)")
        val amount = ModuleUi.numberInput(this, "Amount", allowDecimal = true)
        val mode = spinner(listOf("Cash", "Online"))
        col.addView(entryCard(listOf("Date" to date, "Amount" to amount,
            "Mode" to mode, "Spent On (optional)" to spentOn)))
        // 🔵 spacer — Back/Save একদম নিচে
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(compactFooter("← Back", "Save", { renderMenu() }) {
            /* 🔴🔒 V418: Add Collection-এর মতোই — একটা সেভ শেষ না হওয়া পর্যন্ত
               দ্বিতীয় সেভ শুরু হয় না (ডুপ্লিকেট এন্ট্রি ঠেকাতে)। */
            if (ieSaveBusy) return@compactFooter
            ieSaveBusy = true
            val b = selectedBranch
            if (b == null) { ieSaveBusy = false; ModuleUi.toast(this, "উপরে ডানে ব্রাঞ্চ বাছুন"); return@compactFooter }
            /* 📝 V1203 — ঘরটা **ঐচ্ছিক**, তাই ফাঁকা থাকলেও সেভ আটকায় না
               (TK: *"একটাই বক্স থাকবে ঐচ্ছিক"*)। ফাঁকা হলে আগের মতোই
               "Other Expense" লেখা হয়, যাতে পুরনো সারির সঙ্গে চেহারা মেলে। */
            val amt = amount.text.toString().toDoubleOrNull() ?: 0.0
            if (amt <= 0.0) { ieSaveBusy = false; ModuleUi.toast(this, "Enter Amount")   /* 🔤 V726 */; return@compactFooter }
            val d = (date.tag as? String) ?: todayIso()
            val typed = spentOn.text.toString().trim()
            val c = if (typed.isBlank()) "Other Expense" else typed
            val p = ""
            val md = mode.selectedItem.toString()
            fun enc(x: String) = try { java.net.URLEncoder.encode(x, "UTF-8") } catch (_: Throwable) { x }
            /* ➕🔒 V1215 (০৮.০৯.২০২৬, TK-রিপোর্ট, হুবহু): *"একটা খরচ তুললেই সেভ হয়ে
               যাওয়ার পরে ব্যাকে চলে আসছি · + চিহ্ন নেই কেন? · টাকা থাকলে আমি ওই
               তারিখে আরও কিছু পেমেন্ট তুলতে পারি"*।
               🔬 **কোডে মেপে দেখা — কথাটা সত্যি:** সেভের পরে `renderMenu()` ডাকা
                  হত, তাই একটা খরচ লিখলেই একদম "টাকার হিসাব" পর্দায় ফিরে যেত;
                  দ্বিতীয় খরচের জন্য আবার পুরো পথ হেঁটে আসতে হত।
               ⇒ এখন সেভের পরে **ওই একই দিন ও ব্রাঞ্চ নিয়ে ফর্মটাই আবার খোলে**
                 (টাকা ও "Spent On" ফাঁকা) — পরপর যতগুলো খরচ দরকার লেখা যায়।
               ⛔ ফেরার পথ হারায়নি — উপরের ← আর নিচের Back আগের মতোই আছে।
               ⛔ সেভের নিয়ম · ডুপ্লিকেট-সতর্কতা · অনুমতির নিয়ম কিচ্ছু বদলায়নি। */
            val finishSave = {
                ieSaveBusy = false
                val keepDate = (date.tag as? String) ?: prefillDate
                addExpense(prefillDate = keepDate, prefillBranch = b)
            }
            /* 🟢🔒 V401: পুরনো তারিখ হলে সরাসরি নয় — মাস্টারের অনুমতি চাইতে হবে। */
            if (ieRestricted && !ieIsToday(d)) {
                ieSaveBusy = false
                ieAskApproval(IePermit.ADD_EXPENSE, b, d, null,
                    JSONObject().put("category", c).put("paid_to", p).put("amount", amt).put("mode", md),
                    finishSave)
                return@compactFooter
            }
            Thread {
                // soft duplicate guard: warn on identical recent entry, never block
                val dup = ModuleAuth.getRows("fin", "expenses",
                    "select=id&entry_date=eq.$d&branch=eq.${enc(b)}&category=eq.${enc(c)}&paid_to=eq.${enc(p)}&amount=eq.$amt&mode=eq.$md&limit=1")
                runOnUiThread {
                    if (dup.length() > 0) {
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Duplicate entry?"))
                            .setMessage("This looks identical to an entry already saved — add anyway?")
                            .setPositiveButton("Add") { _, _ -> saveExpense(d, b, c, p, amt, md, "", finishSave) }
                            .setNegativeButton("Cancel") { _, _ -> ieSaveBusy = false }
                            .setOnCancelListener { ieSaveBusy = false }
                            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
                    } else saveExpense(d, b, c, p, amt, md, "", finishSave)
                }
            }.start()
        })
    }

    private fun saveExpense(d: String, b: String, c: String, p: String, amt: Double, md: String, note: String, onSaved: (() -> Unit)? = null) {
        val row = JSONObject().put("entry_date", d).put("branch", b).put("category", c)
            .put("paid_to", p).put("amount", amt).put("mode", md).put("note", note)
            .put("created_by", entryCreatedBy())
        ModuleUi.toast(this, "Saving...")
        Thread {
            val ok = ModuleAuth.insert("fin", "expenses", row)
            runOnUiThread {
                /* 🔒 V418: সফল হোক বা না হোক — তালা এখানেই খোলে। নইলে সেভ ব্যর্থ
                   হলে (নেট নেই) বোতামটা মরে পড়ে থাকত। */
                ieSaveBusy = false
                ModuleUi.toast(this, if (ok) "Saved" else "Saved offline / retry")
                // onSaved থাকলে (Add More): সফল হলে ফাঁকা করে পর্দাতেই থাকে; না থাকলে আগের মতো ফেরে।
                if (onSaved != null) { if (ok) onSaved() } else renderMenu()
            }
        }.start()
    }

    private fun dailyLedger() {
        backAction = { renderMenu() }
        val col = ModuleUi.screen(this, "")
        col.addView(hero("⏰ Daily Ledger"))
        val d = todayIso()
        /* 🔴🔒 V936 (TK-নির্দেশ — এক ফরম্যাট) — আগে কাঁচা `2026-08-31` দেখাত। */
        col.addView(ModuleUi.body(this, "Date: " + com.tkbiswas.pilesclinic.native.DateUtil.display(d)))
        val out = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        out.addView(ModuleUi.body(this, "Loading..."))
        col.addView(out)
        val backBtn = ModuleUi.button(this, "Back") { renderMenu() }
        backBtn.setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
        backBtn.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.WHITE)
            setStroke(dp(1), android.graphics.Color.parseColor("#CFE9D8"))
        }
        backBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(12) }
        col.addView(backBtn)
        Thread {
            // AUDIT FIX (2026-08-06): checked fetch — on network failure show a
            // clear message instead of ₹0 totals (loadOk gates the render below).
            val collR = ModuleAuth.getRowsChecked("fin", "collections", "select=*&entry_date=eq.$d&ignored=eq.false&order=created_at.asc")
            val expR = ModuleAuth.getRowsChecked("fin", "expenses", "select=*&entry_date=eq.$d&ignored=eq.false&order=created_at.asc")
            val loadOk = collR.ok && expR.ok
            val coll = collR.rows
            val exp = expR.rows
            var cash = 0.0; var online = 0.0
            for (i in 0 until coll.length()) { cash += coll.getJSONObject(i).optDouble("cash", 0.0); online += coll.getJSONObject(i).optDouble("online", 0.0) }
            val rows = ArrayList<Triple<String, String, Boolean>>()
            rows.add(Triple("Cash Collection", money(cash), false))
            rows.add(Triple("Online Collection", money(online), false))
            rows.add(Triple("Total Collection", money(cash + online), true))
            var expTotal = 0.0
            for (i in 0 until exp.length()) {
                val e = exp.getJSONObject(i)
                val amt = e.optDouble("amount", 0.0); expTotal += amt
                // 📝 V1203 — `paid_to` ফাঁকা হতে পারে (একটাই ঘর), তাই ফাঁকা বাদ দিয়ে জোড়া।
                rows.add(Triple(listOf(e.s("category"), e.s("paid_to"), e.s("mode"))
                    .filter { it.isNotBlank() }.joinToString(" · "), money(amt), false))
            }
            // 🔴 একই টাকা সব জায়গায় এক দেখাতে: Ledger Sheet-এ লেখা খরচও এখানে যোগ
            for (i in 0 until coll.length()) {
                val c = coll.getJSONObject(i)
                val note = c.optString("expense_notes", "").let { if (it == "null") "" else it }
                if (note.isNotBlank()) {
                    val et = c.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) }
                    expTotal += et
                    rows.add(Triple("[Sheet] " + note, money(et), false))
                }
            }
            rows.add(Triple("Total Expense", money(expTotal), true))
            runOnUiThread {
                if (loadOk) renderGridTable(out, "Item", "Amount", rows)
                else { out.removeAllViews(); out.addView(ModuleUi.body(this, "⚠️ Could not load right now — weak internet. Your data is safe; open this again when online.")) }
            }
        }.start()
    }

    /* 📈🔒 V1217 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ, হুবহু): *"Option
       থাকবে না — উপরে ডান দিকে LG থ্রি ডট, তার মধ্যে WhatsApp share · pdf Download ·
       print"* · *"Show থাকবে না — মাস সিলেক্ট করলে Show হতে হবে"* · *"উপরে যেখানে লেখা
       আছে 2026-09, তার বদলে মাসের নাম থাকবে, আর সেখানে চাপ দিলে মাসের লিস্ট আসবে"* ·
       *"Back বটম একদম নিচে"*।
       ⛔ ভিতরে মাসটা আগের মতোই `yyyy-MM` (`monthYm`) — `runMonthly()`-তে হুবহু
          একই মান যাচ্ছে। হিসাবের কোনো নিয়ম, ছাঁকনি বা টাকার অঙ্ক এক অক্ষরও
          বদলায়নি — শুধু পর্দায় মাসের নামটা দেখানো হয়।
       ⛔ V412-এর সুরক্ষা অক্ষত — মাস/ব্রাঞ্চ বদলালেই আগের টেবিল সঙ্গে সঙ্গে মুছে
          গিয়ে নতুন হিসাব আসে (`runMonthly` নিজেই আগে `out` ফাঁকা করে) — এক
          ব্রাঞ্চের টাকা কখনো অন্য ব্রাঞ্চের নামে পড়া যাবে না।
       ⛔ 📅 ইমোজি কোথাও বসানো হয়নি (TK-র চিরস্থায়ী নিষেধ, সারি ২১৩)। */
    private fun monthly() {
        backAction = { renderMenu() }
        /* Back একদম নিচে রাখার জন্য পর্দা = page(নিচে ফুটার) + scroll(weight 1)।
           এই ধরনটা এই ফাইলেই `sheet()`-এ আগে থেকে প্রমাণিত — নতুন কিছু নয়। */
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
        }
        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(8))
        }
        scroll.addView(col); page.addView(scroll); setContentView(page)

        var monthYm = todayIso().substring(0, 7)
        /* মাসের নাম — চাপলে মাসের তালিকা। `monthLabel()` এই ফাইলেরই পুরনো,
           `sheet()`-এ প্রমাণিত হেল্পার — নতুন কিছু লেখা হয়নি। */
        val month = android.widget.TextView(this).apply {
            text = monthLabel(monthYm) + "  ▾"
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            isClickable = true; isFocusable = true
        }
        /* 🟢🔒 V628 (২৪.০৮.২০২৬, TK-নির্দেশ, স্পষ্ট) — "ওটা তো হিসাবের খাতা...
           প্রতিটা ব্রাঞ্চের হিসাব থাকবে আলাদা, সমস্ত ব্রাঞ্চ একসাথে দেখানো
           যাবে না"। "All Branches" আর অপশনেই নেই। */
        var branchSel = v398Branch().let { if (it in BRANCHES) it else BRANCHES.first() }
        val branchBox = android.widget.TextView(this).apply {
            text = "$branchSel  ▾"
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            isClickable = true; isFocusable = true
        }
        val dotsTv = android.widget.TextView(this).apply {
            text = "⋮"; textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setPadding(dp(16), dp(2), dp(12), dp(2))   // 🔎 V1254
            isClickable = true; isFocusable = true
        }
        /* 📏 V1217 — কেন এক সারির `heroWithFields()` এখানে চলল না (মেপে দেখা,
           আন্দাজ নয়): ৩৬০dp চওড়া ফোনে হেডারে জায়গা থাকে ~৩১১dp, অথচ
           "📈 Monthly Summary" (~১৬৬dp) + "September 2026 ▾" (~১৪৩dp) +
           ব্রাঞ্চ (~১২২dp) + ⋮ (~২৪dp) = ~৪৫৫dp। আগে "2026-09" ছোট ছিল বলে আঁটত;
           মাসের পুরো নাম বসতেই শিরোনাম কেটে "📈 M…" হয়ে যেত।
           ⇒ তাই এই পর্দায় হেডার দু-সারির: উপরে শিরোনাম ও ডানে ⋮ (TK: *"উপরে ডান
              দিকে LG থ্রি ডট"*), নিচে ডানদিকে মাস ও ব্রাঞ্চ।
           ⛔ `heroWithFields()` ও বাকি সব পর্দার হেডার এক অক্ষরও বদলায়নি। */
        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(9), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#DCE6E0"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(8) }
        }
        val heroTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        heroTop.addView(android.widget.TextView(this).apply {
            text = "📈 Monthly Summary"; textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        heroTop.addView(dotsTv)
        val heroBottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(6) }
        }
        // পিল দুটো হুবহু `heroWithFields()`-এর মতোই — হালকা সবুজ-ধূসর, গাঢ় সবুজ লেখা।
        fun heroPill(field: android.view.View): LinearLayout {
            val b = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(9), dp(5), dp(9), dp(5))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(android.graphics.Color.parseColor("#F1F6F3"))
                    setStroke(dp(1), android.graphics.Color.parseColor("#D6E4DC"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply { marginStart = dp(6) }
            }
            b.addView(field)
            return b
        }
        heroBottom.addView(heroPill(month)); heroBottom.addView(heroPill(branchBox))
        heroCard.addView(heroTop); heroCard.addView(heroBottom)
        col.addView(heroCard)
        val out = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        /* মাস বা ব্রাঞ্চ বদলালেই সঙ্গে সঙ্গে নতুন হিসাব — TK: *"Show থাকবে না,
           মাস সিলেক্ট করলে Show হতে হবে"*। ⛔ শেয়ার/PDF-এর লেখা আগের মতোই
           পুরনো ফল মুছে তবেই নতুন করে বসে। */
        val reload = {
            v398Remember(branchSel)   // 🟢🔒 V398
            monthlyShareText = null; monthlyPdfHtml = null
            runMonthly(monthYm, branchSel, out)
        }
        month.setOnClickListener {
            val yms = ArrayList<String>()
            val cal = java.util.Calendar.getInstance()
            for (k in 0 until 24) {
                yms.add(String.format(Locale.US, "%04d-%02d",
                    cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1))
                cal.add(java.util.Calendar.MONTH, -1)
            }
            val labels = yms.map { monthLabel(it) }.toTypedArray()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Select Month"))
                .setItems(labels) { _, which ->
                    monthYm = yms[which]
                    month.text = labels[which] + "  ▾"
                    reload()
                }
                .setNegativeButton("Cancel", null)
                .show().also { d -> try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(d) } catch (_: Throwable) { } }
        }
        branchBox.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "🏥 Branch"))
                .setItems(BRANCHES.toTypedArray()) { _, which ->
                    branchSel = BRANCHES[which]
                    branchBox.text = "$branchSel  ▾"
                    reload()
                }
                .setNegativeButton("Cancel", null)
                .show().also { d -> try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(d) } catch (_: Throwable) { } }
        }
        dotsTv.setOnClickListener { v -> showMonthlyOptions(v) }

        col.addView(out)

        // Back — পর্দার একদম নিচে (TK-নির্দেশ, V1217)।
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(4), dp(14), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val backBtn = ModuleUi.button(this, "← Back") { renderMenu() }
        backBtn.setTextColor(android.graphics.Color.parseColor("#0B4F2A"))
        backBtn.background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.WHITE)
            setStroke(dp(1), android.graphics.Color.parseColor("#CFE9D8"))
        }
        backBtn.minimumHeight = 0; backBtn.minHeight = 0
        backBtn.setPadding(dp(10), dp(9), dp(10), dp(9))
        backBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        footer.addView(backBtn)
        page.addView(footer)

        reload()
    }

    // 🔵🔒 Monthly Summary — TK-অনুমোদিত প্রুফ (09.08.2026): টাকার খাতার হুবহু একই
    // খাতা-ডিজাইন। উপরে "Previous Balance (গত মাসের ব্যালেন্স)", মাঝে দিন-ধরে
    // Date/Cash/Online/খরচ বক্স-টেবিল, খরচে চাপ দিলে ওই দিনের ভাঙা-হিসাব পপ-আপ,
    // একদম নিচে "অবশিষ্ট টাকা"। ⛔ টাকার হিসাবের নিয়ম আগের summarizeRows-এর সাথে
    // হুবহু এক — মোট আয় = নগদ+অনলাইন; মোট খরচ = Add-Expense এন্ট্রি + খাতায় লেখা
    // খরচ (expense_notes); ব্যালেন্স = আয় − খরচ। শুধু দেখানোর ধরন বদলেছে (দিন-ধরে
    // টেবিল), কোনো সংখ্যা যোগ/বিয়োগ/হিসাব বদলায়নি।
    private fun runMonthly(ym: String, branchSel: String, out: LinearLayout) {
        out.removeAllViews(); out.addView(ModuleUi.body(this, "Loading..."))
        val start = "$ym-01"
        // first day of next month
        val y = ym.substring(0, 4).toInt(); val m = ym.substring(5, 7).toInt()
        val nextY = if (m == 12) y + 1 else y; val nextM = if (m == 12) 1 else m + 1
        val end = String.format(Locale.US, "%04d-%02d-01", nextY, nextM)
        val branchQ = if (branchSel != "All Branches") "&branch=eq.$branchSel" else ""
        Thread {
            // AUDIT FIX (2026-08-06): checked fetch — on network failure show a
            // clear message instead of a ₹0 monthly summary.
            val collR = ModuleAuth.getRowsChecked("fin", "collections",
                "select=*&entry_date=gte.$start&entry_date=lt.$end&ignored=eq.false$branchQ&order=entry_date.asc")
            val expR = ModuleAuth.getRowsChecked("fin", "expenses",
                "select=*&entry_date=gte.$start&entry_date=lt.$end&ignored=eq.false$branchQ&order=entry_date.asc,created_at.asc")
            // 🔵 আগের বাকি (Previous Balance) = এই মাসের আগের সব দিনের (একই ব্রাঞ্চের)
            // আয় − খরচ। দুই উৎস থেকেই খরচ বাদ যায়: খাতার নিজের খরচ (collections) +
            // Add-Expense এন্ট্রি (expenses)। মাসের ভিতরের হিসাবের সাথে হুবহু মেলে।
            val prevCollR = ModuleAuth.getRowsChecked("fin", "collections",
                "select=cash,online,expense_total,expense_notes&entry_date=lt.$start&ignored=eq.false$branchQ")
            val prevExpR = ModuleAuth.getRowsChecked("fin", "expenses",
                "select=amount&entry_date=lt.$start&ignored=eq.false$branchQ")
            val loadOk = collR.ok && expR.ok
            val prevOk = prevCollR.ok && prevExpR.ok
            var prevBal = 0.0
            if (prevOk) {
                val pc = prevCollR.rows
                for (i in 0 until pc.length()) {
                    val c = pc.getJSONObject(i)
                    val note = c.optString("expense_notes", "").let { if (it == "null") "" else it }
                    val exp = c.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) }
                    prevBal += c.optDouble("cash", 0.0) + c.optDouble("online", 0.0) - exp
                }
                val pe = prevExpR.rows
                for (i in 0 until pe.length()) prevBal -= pe.getJSONObject(i).optDouble("amount", 0.0)
            }
            val coll = collR.rows
            val exp = expR.rows
            /* 🟢🔒 V929 — Monthly Summary-তেও অটো-আয় (loadSheet-এর হুবহু নিয়ম)।
               ⛔ background thread-এই আনা হয়, UI-তে নয়। পড়া ব্যর্থ হলে খালি
                  মানচিত্র — তখন আগের মতোই কিছু বসে না। */
            val autoMonthly: Map<String, Pair<Double, Double>> =
                if (loadOk) (try { autoIncomeByDate(ym, branchSel) } catch (_: Throwable) { emptyMap() }) else emptyMap()
            runOnUiThread {
                out.removeAllViews()
                if (loadOk) buildMonthlyKhata(coll, exp, prevBal, prevOk, branchSel, out, autoMonthly)
                else out.addView(ModuleUi.body(this, "⚠️ Could not load right now — weak internet. Your data is safe; open this again when online."))
            }
        }.start()
    }

    // দিন-ধরে খাতা-টেবিল বানায় (buildSheetTable-এর হুবহু একই বক্স-স্টাইল ও কলাম-মাপ)।
    // প্রতিটি দিনের খরচ = ওই দিনের খাতার খরচ (expense_notes) + ওই দিনের Add-Expense
    // এন্ট্রিগুলোর যোগফল। খরচের ঘরে চাপ দিলে দুই উৎস মিলিয়ে ভাঙা-হিসাব দেখায়।

    /* 🟢🔒 V929 — তারিখের পাশে ট্যাগ, **শুধু মাস্টারের পর্দায়** (TK: *"অটো না
       হাতে ঠিক করা এটা মাস্টার ছাড়া কেউ দেখতে পাবে না"*)। মাপা ও দেখানো —
       দুই জায়গায় যেন হুবহু একই লেখা হয়, তাই একটাই ঘরে রাখা। */
    private fun monthlyDateLabel(d: String, dotted: String, autoDays: Set<String>,
                                 rowByDate: Map<String, JSONObject>): String {
        if (!ModuleAuth.isMaster || d < AUTO_INCOME_FROM) return dotted
        return when {
            autoDays.contains(d) -> "$dotted  AUTO"
            rowByDate.containsKey(d) -> "$dotted  ✎"
            else -> dotted
        }
    }

    private fun buildMonthlyKhata(coll: JSONArray, exp: JSONArray, prevBalance: Double, prevOk: Boolean, branchSel: String, out: LinearLayout,
                                 autoIncome: Map<String, Pair<Double, Double>> = emptyMap()) {
        val dayCash = LinkedHashMap<String, Double>()
        val dayOnline = LinkedHashMap<String, Double>()
        val dayExp = LinkedHashMap<String, Double>()
        val daySeg = LinkedHashMap<String, StringBuilder>()
        /* 🟠🔒 V960 (০১.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ) — TK: *"এখানে এডিট বা
           ডিলিটের কোন অপশনই বা নেই কেন?"*
           **আসল কারণ (কোড ধরে):** এই পর্দার খরচ-পপ-আপ ডাকা হত `items = null`
           দিয়ে, অর্থাৎ খরচের **আসল সারিগুলো পাঠানোই হত না** — শুধু লেখাটা
           দেখানো হত, তাই ✏️ বসত না ও চাপা যেত না। অথচ আয়-ব্যয়ের খাতার
           পপ-আপে (লাইন ~৮০২) ঠিক ওই একই খরচে ✏️ ছিল, চেপে বদলানো/মোছা যেত।
           **সমাধান:** এখানেও সারিগুলো পাঠানো হয়। এডিটরের কোড এক অক্ষরও নতুন নয়
           — V400-এর প্রমাণিত `openExpenseEditor`-ই খোলে।
           ⛔ দুবার দেখানো ঠেকাতে: পপ-আপে **লেখা** হিসেবে যাবে শুধু খাতার নিজের
              সারিতে লেখা খরচ (`dayOwnSeg`), আর Add-Expense এন্ট্রিগুলো যাবে
              সারি হিসেবে — মোট (`expSum`) আগের মতোই অপরিবর্তিত। */
        val dayOwnSeg = LinkedHashMap<String, StringBuilder>()
        val dayOwnExp = LinkedHashMap<String, Double>()
        val expItemsByDate = LinkedHashMap<String, JSONArray>()
        // 🟢🔒 V628 (২৪.০৮.২০২৬) — তারিখ ধরে আসল `collections` সারি মনে রাখা, যাতে
        // "✏️ Edit This Day" বোতাম সরাসরি সঠিক সারিতে পৌঁছাতে পারে। ব্রাঞ্চ এখন
        // সবসময় একটাই নির্দিষ্ট (V628-এর "All Branches" অপসারণ) — তাই প্রতি
        // তারিখে বড়জোর একটাই সারি, দ্বিধার কোনো সুযোগ নেই।
        val rowByDate = LinkedHashMap<String, JSONObject>()
        val dates = java.util.TreeSet<String>()
        fun addSeg(d: String, text: String) {
            val sb = daySeg.getOrPut(d) { StringBuilder() }
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(text)
        }
        fun addOwnSeg(d: String, text: String) {   // 🟠 V960
            val sb = dayOwnSeg.getOrPut(d) { StringBuilder() }
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(text)
        }
        fun segAmt(a: Double): String = if (a == Math.floor(a)) a.toLong().toString() else a.toString()
        for (i in 0 until coll.length()) {
            val c = coll.getJSONObject(i)
            val d = c.optString("entry_date"); if (d.isBlank()) continue
            dates.add(d)
            rowByDate[d] = c   // 🟢🔒 V628
            dayCash[d] = (dayCash[d] ?: 0.0) + c.optDouble("cash", 0.0)
            dayOnline[d] = (dayOnline[d] ?: 0.0) + c.optDouble("online", 0.0)
            val note = c.optString("expense_notes", "").let { if (it == "null") "" else it }
            val se = c.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) }
            if (se != 0.0 || note.isNotBlank()) {
                dayExp[d] = (dayExp[d] ?: 0.0) + se
                dayOwnExp[d] = (dayOwnExp[d] ?: 0.0) + se        // 🟠 V960
                if (note.isNotBlank()) { addSeg(d, note); addOwnSeg(d, note) }
            }
        }
        for (i in 0 until exp.length()) {
            val e = exp.getJSONObject(i)
            val d = e.optString("entry_date"); if (d.isBlank()) continue
            dates.add(d)
            val a = e.optDouble("amount", 0.0)
            dayExp[d] = (dayExp[d] ?: 0.0) + a
            val cat = e.s("category"); val pt = e.s("paid_to")
            val label = if (pt.isNotBlank()) "$cat — $pt" else cat
            addSeg(d, "$label-${segAmt(a)}")
            expItemsByDate.getOrPut(d) { JSONArray() }.put(e)    // 🟠 V960
        }
        /* 🟢🔒 V929 — অটো-আয় বসানো। ⛔ যে দিনে হাতে লেখা `collections` সারি আছে
           (`rowByDate`) সেখানে কখনো নয় — মানুষের লেখাই জেতে। ⛔ ০১/০৯/২০২৬-এর
           আগে কখনো নয় (`autoIncomeByDate` নিজেই আটকায়)। */
        val autoDays = HashSet<String>()
        for ((d, v) in autoIncome) {
            if (d.isBlank() || rowByDate.containsKey(d)) continue
            if ((dayCash[d] ?: 0.0) != 0.0 || (dayOnline[d] ?: 0.0) != 0.0) continue
            if (v.first <= 0.0 && v.second <= 0.0) continue
            dayCash[d] = v.first
            dayOnline[d] = v.second
            dates.add(d)
            autoDays.add(d)
        }

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val dpx = { v: Int -> ModuleUi.dp(this, v) }
        val boldTf = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val measure = android.graphics.Paint().apply { typeface = boldTf; textSize = 11f * resources.displayMetrics.scaledDensity }
        val cellPadPx = dpx(8) * 2
        val cellSlackPx = dpx(10)
        // 🔵 Date ঘর কনটেন্টের সমান সরু (TK: "Date fixed, ডান পাশে এত জায়গা দরকার নেই"),
        // Cash/Online মাপা হয় সবচেয়ে বড় সংখ্যার সাথে (TK: "সংখ্যা কমবেশি হতে পারে"),
        // খরচ ঘর বাকি সব জায়গা নেয় (weight=1)।
        var dateColPx = maxOf(measure.measureText("Date"), measure.measureText("Total"))
        var amtColPx = maxOf(measure.measureText("Cash"), measure.measureText("Online"))
        for (d in dates) {
            val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
            dateColPx = maxOf(dateColPx, measure.measureText(monthlyDateLabel(d, dotted, autoDays, rowByDate)))
            amtColPx = maxOf(amtColPx, measure.measureText(money(dayCash[d] ?: 0.0).removePrefix("₹")),
                measure.measureText(money(dayOnline[d] ?: 0.0).removePrefix("₹")))
        }
        val dateColWidth = dateColPx.toInt() + cellPadPx + cellSlackPx
        val amtColWidth = amtColPx.toInt() + cellPadPx + cellSlackPx

        fun boxCell(text: String, w: Int, bg: String, fg: String, bold: Boolean, weight: Float? = null, gravityV: Int? = null): android.widget.TextView =
            android.widget.TextView(this).apply {
                this.text = text; textSize = 11f
                setTextColor(android.graphics.Color.parseColor(fg))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (gravityV != null) gravity = gravityV
                setPadding(dpx(8), dpx(8), dpx(8), dpx(8))
                layoutParams = if (weight != null) LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                    else LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT)
                background = if (bg == "#0A7C3F") android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor(bg)) }
                    else cellBorderDrawable().apply { setColor(android.graphics.Color.parseColor(bg)) }
                if (weight != null) { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
            }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(boxCell("Date", dateColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("Cash", amtColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("Online", amtColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("খরচ", 0, "#0A7C3F", "#FFFFFF", true, weight = 1f, gravityV = android.view.Gravity.CENTER))
        table.addView(head)
        val builtRows = ArrayList<LinearLayout>(); builtRows.add(head)

        var cashTot = 0.0; var onlineTot = 0.0; var expTot = 0.0
        var idx = 0
        for (d in dates) {
            val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
            val dottedShown = monthlyDateLabel(d, dotted, autoDays, rowByDate)
            val cash = dayCash[d] ?: 0.0; val online = dayOnline[d] ?: 0.0
            val expSum = dayExp[d] ?: 0.0; val seg = daySeg[d]?.toString() ?: ""
            cashTot += cash; onlineTot += online; expTot += expSum
            val bg = if (idx % 2 == 0) "#FFFFFF" else "#F7FBF8"; idx++
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(boxCell(dottedShown, dateColWidth, bg, "#41506A", true))
            row.addView(boxCell(money(cash).removePrefix("₹"), amtColWidth, bg, "#0A7C3F", false, gravityV = android.view.Gravity.END))
            row.addView(boxCell(money(online).removePrefix("₹"), amtColWidth, bg, "#0A7C3F", false, gravityV = android.view.Gravity.END))
            val expText = if (expSum > 0.0) money(expSum).removePrefix("₹") else "-"
            val expCell = boxCell(expText, 0, bg, "#B42318", false, weight = 1f, gravityV = android.view.Gravity.END)
            if (expSum > 0.0 || seg.isNotBlank()) {
                expCell.isClickable = true
                // 🟠🔒 V960 — খরচের আসল সারিগুলোও পাঠানো হয় (✏️ চেপে বদল/মোছা),
                //    আর "লেখা" হিসেবে যায় শুধু খাতার নিজের লেখাটুকু — তাই একই
                //    খরচ দুবার দেখায় না। মোট (`expSum`) আগের মতোই।
                expCell.setOnClickListener {
                    showExpenseBreakdown(
                        dotted, dayOwnSeg[d]?.toString() ?: "", expSum,
                        dayOwnExp[d] ?: 0.0, expItemsByDate[d], rowByDate[d],
                        d, branchSel                                       // ➕ V1203
                    )
                }
            }
            row.addView(expCell)
            table.addView(row); builtRows.add(row)
        }
        val tot = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tot.addView(boxCell("Total", dateColWidth, "#EAF6EE", "#0A5C33", true))
        tot.addView(boxCell(money(cashTot).removePrefix("₹"), amtColWidth, "#EAF6EE", "#0A5C33", true, gravityV = android.view.Gravity.END))
        tot.addView(boxCell(money(onlineTot).removePrefix("₹"), amtColWidth, "#EAF6EE", "#0A5C33", true, gravityV = android.view.Gravity.END))
        tot.addView(boxCell(money(expTot).removePrefix("₹"), 0, "#EAF6EE", "#B42318", true, weight = 1f, gravityV = android.view.Gravity.END))
        table.addView(tot); builtRows.add(tot)

        // 🔴 V453 — Previous Balance ও অবশিষ্ট টাকা একটাই পাশাপাশি সারিতে, টেবিলের নিচে।
        // 🔴 V413 (TK-নির্দেশ): উপরের নির্দেশ-লাইনটা তুলে দেওয়া হলো।
        out.addView(table)
        if (dates.isEmpty()) out.addView(ModuleUi.body(this, "এই মাসে এখনো কোনো এন্ট্রি নেই।"))
        TableRowEqualizer.equalize(table, builtRows)
        /* 🟢🔒 V693 (২৬.০৮.২০২৬, TK-নির্দেশ ছবিসহ, তাঁর "হ্যাঁ" নিয়ে) —
           নিচের বাক্সটা এখন TK-এর ছবির মতো: **মোট আয় · মোট ব্যয় · অবশিষ্ট**।
           আগে ছিল "Previous Balance | অবশিষ্ট টাকা" (V453)।
           ⚠️ **টাকার হিসাবেও একটা বদল** — TK-কে দেখিয়ে, জিজ্ঞাসা করে,
              তাঁর অনুমতি নিয়ে: অবশিষ্ট = মোট আয় − মোট ব্যয়; **গত মাসের
              বাকি (prevBalance) আর যোগ হয় না**। TK-এর ছবির সংখ্যাগুলোও
              ঠিক এই হিসাবেই মেলে (৩,১২,৬৯০ − ১,৬৮,৫২০ = ১,৪৪,১৭০)।
           ⛔ Daily Ledger (`buildSheetTable`)-এর নিচের বার এক অক্ষরও
              বদলায়নি — সেখানে গত মাসের বাকি আগের মতোই ধরা হয়। */
        val incomeTot = cashTot + onlineTot
        val remaining = incomeTot - expTot
        out.addView(monthTotalsBox(incomeTot, expTot, remaining))
        // 🔵🔒 R6 (TK-অনুমোদিত, 09.08.2026): Monthly হিসাব WhatsApp-এ শেয়ার।
        // 🟢🔒 V693 — লেখাটা আগের মতোই বানানো হয়, শুধু বোতামটা আর এখানে
        //    বসে না; এখন নিচের "••• Options" মেনুর ভিতরে (TK-এর ছবি)।
        if (dates.isNotEmpty()) {
            val monthLbl = dates.firstOrNull()?.let { try { monthLabel(it.substring(0, 7)) } catch (e: Exception) { "" } } ?: ""
            val sbx = StringBuilder()
            sbx.append("📒 টাকার হিসাব — ").append(if (monthLbl.isNotBlank()) monthLbl else "Monthly").append("\n")
            sbx.append(if (branchSel == "All Branches") "সব ব্রাঞ্চ" else branchSel).append("\n")
            sbx.append("————————————\n")
            // 🟢🔒 V693 — পর্দায় যা দেখা যায়, শেয়ারের লেখাতেও ঠিক তাই।
            //    আগে এখানে "গত মাসের ব্যালেন্স" লেখা হত; পর্দা থেকে সেটা
            //    উঠে যাওয়ায় লেখাতেও রাখা হলো না — নইলে পর্দা আর লেখা
            //    দুরকম বলত, সেটাই নতুন একটা ভুল হত।
            for (d in dates) {
                val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
                sbx.append(dotted).append(" — Cash ").append(money(dayCash[d] ?: 0.0))
                    .append(" · Online ").append(money(dayOnline[d] ?: 0.0))
                    .append(" · খরচ ").append(money(dayExp[d] ?: 0.0)).append("\n")
            }
            sbx.append("————————————\n")
            sbx.append("মোট: Cash ").append(money(cashTot)).append(" · Online ").append(money(onlineTot)).append(" · খরচ ").append(money(expTot)).append("\n")
            sbx.append("মোট আয়: ").append(money(incomeTot)).append("\n")
            sbx.append("মোট ব্যয়: ").append(money(expTot)).append("\n")
            sbx.append("অবশিষ্ট: ").append(money(remaining))
            monthlyShareText = sbx.toString()

            /* 🟢🔒 V693 — PDF/Print-এর জন্য একই টেবিলের HTML প্রতিলিপি।
               ⛔ Statement-এর `statementPdfHtml`-এর হুবহু একই ধাঁচ ও একই
                  প্রমাণিত পথ (`printStatementPdf`)। কোনো টাকা নতুন করে
                  গোনা হয়নি — উপরের একই dates/dayCash/dayOnline/dayExp। */
            val sbh = StringBuilder()
            sbh.append("<html><head><meta charset='utf-8'><style>")
                .append("*{-webkit-print-color-adjust:exact;print-color-adjust:exact}")
                .append("body{font-family:sans-serif;padding:14px;color:#222}")
                .append("h2{color:#0A5C33;margin-bottom:2px}")
                .append(".sub{color:#667085;font-size:13px;margin-bottom:14px}")
                .append("table{border-collapse:collapse;width:100%;font-size:13px}")
                .append("th,td{border:1px solid #D9E2EC;padding:6px 8px;text-align:right}")
                .append("th{background:#0A7C3F;color:#fff}")
                .append("td:first-child,th:first-child{text-align:left}")
                .append(".tot{background:#EAF6EE;font-weight:bold;color:#0A5C33}")
                .append(".exp{color:#B42318}")
                .append(".sum{margin-top:14px;font-size:14px}")
                .append(".sum div{padding:4px 0}")
                .append("</style></head><body>")
            sbh.append("<h2>").append(if (monthLbl.isNotBlank()) monthLbl else "Monthly").append("</h2>")
            sbh.append("<div class='sub'>").append(if (branchSel == "All Branches") "All Branches" else branchSel).append("</div>")
            sbh.append("<table><tr><th>Date</th><th>Cash</th><th>Online</th><th>Expense</th></tr>")
            for (d in dates) {
                val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
                val c2 = dayCash[d] ?: 0.0; val o2 = dayOnline[d] ?: 0.0; val e2 = dayExp[d] ?: 0.0
                sbh.append("<tr><td>").append(dotted).append("</td><td>")
                    .append(if (c2 > 0) money(c2).removePrefix("₹") else "-").append("</td><td>")
                    .append(if (o2 > 0) money(o2).removePrefix("₹") else "-").append("</td><td class='exp'>")
                    .append(if (e2 > 0) money(e2).removePrefix("₹") else "-").append("</td></tr>")
            }
            sbh.append("<tr class='tot'><td>Total</td><td>").append(money(cashTot).removePrefix("₹"))
                .append("</td><td>").append(money(onlineTot).removePrefix("₹"))
                .append("</td><td>").append(money(expTot).removePrefix("₹")).append("</td></tr>")
            sbh.append("</table>")
            sbh.append("<div class='sum'>")
                .append("<div>মোট আয় = ").append(money(incomeTot)).append("</div>")
                .append("<div>মোট ব্যয় = ").append(money(expTot)).append("</div>")
                .append("<div><b>অবশিষ্ট = ").append(money(remaining)).append("</b></div>")
                .append("</div>")
            sbh.append("</body></html>")
            monthlyPdfHtml = sbh.toString()
        } else {
            // এই মাসে কিছু নেই — Options-এ পুরনো মাসের লেখা যেন থেকে না যায়।
            monthlyShareText = null
            monthlyPdfHtml = null
        }
    }

    // =====================================================================
    // 🟢🔒 V629 (২৪.০৮.২০২৬, TK-নির্দেশ) — "Statement": ব্যাংক-স্টেটমেন্টের মতো,
    // যেকোনো From–To তারিখের মধ্যে প্রতিদিনের **পরে চলতি ব্যালেন্স (running
    // balance)** দেখায়। TK নিজে ক্যালকুলেটরে হাতে হিসাব করে যাচাই করছিলেন,
    // তাই প্রতিটা দিনের পরের ব্যালেন্স আলাদা করে দেখানো হচ্ছে — ঠিক কোন দিনে
    // মিল ভাঙছে সেটা এক নজরে ধরা যাবে।
    // ⛔ এটা শুধু **নতুন দেখার পর্দা** — Ledger Sheet/Monthly Summary-র প্রমাণিত
    //    হিসাব-সূত্রই (cash+online−খরচ, দুই উৎস থেকেই) হুবহু পুনর্ব্যবহার করা
    //    হয়েছে। কোনো নতুন হিসাব-নিয়ম বানানো হয়নি, তাই সংখ্যা অন্য পর্দার সাথে
    //    কখনো আলাদা হতে পারে না।
    // ⛔ ব্রাঞ্চ সবসময় একটাই নির্দিষ্ট (V628-এর নিয়ম মেনেই) — হিসাবের খাতায়
    //    ব্রাঞ্চ মিশবে না।
    // =====================================================================
    // 🟢🔒 V657 (২৫.০৮.২০২৬) — সবচেয়ে সাম্প্রতিক দেখানো স্টেটমেন্টের HTML —
    // PDF বোতাম চাপলে এটাই প্রিন্ট/PDF-এ যায়। ⛔ টেবিল দেখানোর সাথে সাথেই
    // বসে (buildStatementTable-এর শেষে), তাই বোতামটা সবসময় সাম্প্রতিক ডেটা
    // দেখায়।
    private var statementPdfHtml: String? = null

    /* 🟢🔒 V693 (২৬.০৮.২০২৬, TK-নির্দেশ ছবিসহ) — Monthly-র "\u2022\u2022\u2022 Options"-এর
       তিনটে কাজের জন্য। "Show" চাপার সময় ভরা হয়; ভরা না থাকলে Options
       চাপলে ভদ্রভাবে "আগে Show চাপুন" বলা হয় (Statement-এর PDF চিপ ঠিক
       যেভাবে করে, হুবহু সেই ধরন)। */
    private var monthlyShareText: String? = null
    private var monthlyPdfHtml: String? = null

    /**
     * 🟢🔒 V693 (২৬.০৮.২০২৬, TK-নির্দেশ, ছবিসহ) — মাসের হিসাবের নিচের
     * **তিন লাইনের বাক্স**: মোট আয় · মোট ব্যয় · অবশিষ্ট।
     *
     * TK-এর ছবিতে ঠিক এই তিনটেই আছে — "Previous Balance" নেই, আর
     * **অবশিষ্ট = মোট আয় − মোট ব্যয়** (গত মাসের বাকি ধরা হয় না)।
     * TK-কে দেখিয়ে, জিজ্ঞাসা করে, তাঁর "হ্যাঁ" নিয়ে তবেই বদলানো হলো।
     *
     * ⛔ এটা **শুধু Monthly-র জন্য** — Daily Ledger (`buildSheetTable`)-এর
     *    নিচের `balanceBarPair()` (Previous Balance | অবশিষ্ট টাকা) এক
     *    অক্ষরও বদলায়নি, ওখানে গত মাসের বাকি আগের মতোই ধরা হয়।
     */
    private fun monthTotalsBox(incomeTot: Double, expenseTot: Double, remaining: Double): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#E3ECE6"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10); bottomMargin = dp(4) }
        }
        fun line(label: String, value: String, colorHex: String, divider: Boolean) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(7), 0, dp(7))
            }
            row.addView(android.widget.TextView(this).apply {
                text = label; textSize = 14f
                setTextColor(android.graphics.Color.parseColor(colorHex))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            row.addView(android.widget.TextView(this).apply {
                text = "="; textSize = 14f
                setTextColor(android.graphics.Color.parseColor(colorHex))
                setPadding(dp(8), 0, dp(8), 0)
            })
            row.addView(android.widget.TextView(this).apply {
                text = value; textSize = 15f
                setTextColor(android.graphics.Color.parseColor(colorHex))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            card.addView(row)
            if (divider) card.addView(android.view.View(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#EEF3F0"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
            })
        }
        line("মোট আয়", money(incomeTot), "#0A7C3F", true)
        line("মোট ব্যয়", money(expenseTot), "#B42318", true)
        line("অবশিষ্ট", money(remaining), "#1B4E9B", false)
        return card
    }

    /** 🟢🔒 V693 — TK-এর ছবির "\u2022\u2022\u2022 Options" মেনু: WhatsApp-এ শেয়ার ·
     *  PDF Download · Print। ⛔ তিনটেই আগে থেকে প্রমাণিত পথ —
     *  `WhatsAppMessageChooser.sendGeneric()` আর `printStatementPdf()`
     *  (WebView + Android-এর নিজের Print, যেখানে "Save as PDF" বেছে নিলেই
     *  পিডিএফ)। নতুন কোনো লাইব্রেরি বা অনুমতি লাগেনি। */
    private fun showMonthlyOptions(anchor: android.view.View) {
        val share = monthlyShareText
        val html = monthlyPdfHtml
        if (share.isNullOrBlank() || html.isNullOrBlank()) {
            ModuleUi.toast(this, "হিসাব আসছে — এক মুহূর্ত পরে আবার চাপুন।")
            return
        }
        val menu = android.widget.PopupMenu(this, anchor)
        menu.menu.add(0, 1, 0, "📤 WhatsApp-এ শেয়ার")
        menu.menu.add(0, 2, 1, "📄 PDF Download")
        menu.menu.add(0, 3, 2, "🖨️ Print")
        menu.setOnMenuItemClickListener { mi ->
            when (mi.itemId) {
                1 -> try {
                    com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, share)
                } catch (e: Throwable) { ModuleUi.toast(this, "শেয়ার করা গেল না") }
                // ⛔ PDF ও Print — একই পর্দাই খোলে (Android-এর নিজের Print
                //    পর্দা)। সেখানে গন্তব্যে "Save as PDF" বাছলে পিডিএফ,
                //    প্রিন্টার বাছলে ছাপা। এটাই Android-এর স্বাভাবিক নিয়ম।
                2, 3 -> printStatementPdf(html)
            }
            true
        }
        menu.show()
    }

    // 🟢🔒🔒 V657 (২৫.০৮.২০২৬, TK-নির্দেশ — PDF ডাউনলোড) — `printCheckupA4()`
    // (PatientTimelineActivity.kt)-এর হুবহু একই, প্রমাণিত পথ: WebView-এ
    // HTML বসিয়ে Android-এর নিজস্ব Print ব্যবস্থা ডাকা হয় — সেখানে
    // "Save as PDF" ডেস্টিনেশন বেছে নিলেই ফোনে PDF জমা হয়ে যায়। কোনো নতুন
    // লাইব্রেরি/স্টোরেজ-অনুমতি লাগে না।
    private var statementPrintWebView: android.webkit.WebView? = null
    private fun printStatementPdf(html: String) {
        try {
            val wv = android.webkit.WebView(this)
            wv.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                    try {
                        val pm = getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val adapter = view.createPrintDocumentAdapter("Statement")
                        pm.print("Statement", adapter, com.tkbiswas.pilesclinic.native.PrintQuality.builder().build())
                    } catch (_: Throwable) {
                        android.widget.Toast.makeText(this@IncomeExpenseActivity, "Print not available", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            statementPrintWebView = wv
            wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, "Print not available", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun statement() {
        backAction = { renderMenu() }
        val scroll = android.widget.ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6"))
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(16))
        }
        scroll.addView(col); setContentView(scroll)

        var branchSel = (lockedBranch ?: v398Branch()).let { if (it == "All Branches" || it !in BRANCHES) BRANCHES.first() else it }
        val out = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        var reload: () -> Unit = {}

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(13), dp(12), dp(13))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(android.graphics.Color.parseColor("#0B4F2A"), android.graphics.Color.parseColor("#16A34A"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(12) }
        }
        header.addView(android.widget.TextView(this).apply {
            text = "←  📄 Statement"; textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true
            setOnClickListener { renderMenu() }
        })
        val branchLocked = lockedBranch != null
        val branchChip = android.widget.TextView(this).apply {
            text = if (branchLocked) "🏥 $branchSel" else "🏥 $branchSel ▾"; textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#88FFFFFF"))
            }
            isClickable = !branchLocked
        }
        if (!branchLocked) branchChip.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "ব্রাঞ্চ বাছুন"))
                .setItems(BRANCHES.toTypedArray()) { _, which ->
                    branchSel = BRANCHES[which]
                    v398Remember(branchSel)
                    branchChip.text = "🏥 " + BRANCHES[which] + " ▾"
                    reload()
                }
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
        }
        // 🟢🔒🔒 V657 (২৫.০৮.২০২৬, TK-নির্দেশ — "pdf ডাউনলোড করা যাবে সেরকম
        // ব্যবস্থা রাখবেন") — নতুন বোতাম, ব্রাঞ্চ-চিপের পাশে। চাপলে এই
        // পাতার (বর্তমান From–To/ব্রাঞ্চ) স্টেটমেন্ট টেবিলটাই Android-এর
        // নিজের Print/Save-as-PDF ব্যবস্থায় (WebView+PrintManager,
        // printCheckupA4()-এর হুবহু একই প্রমাণিত পথ) পিডিএফ হিসেবে
        // সেভ করার সুযোগ দেয়। ⛔ কোনো নতুন লাইব্রেরি/অনুমতি লাগে না।
        val pdfChip = android.widget.TextView(this).apply {
            text = "⬇️ PDF"; textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(android.graphics.Color.parseColor("#33FFFFFF"))
                setStroke(dp(1), android.graphics.Color.parseColor("#88FFFFFF"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginStart = dp(8) }
            isClickable = true
            setOnClickListener {
                val html = statementPdfHtml
                if (html.isNullOrBlank()) {
                    android.widget.Toast.makeText(this@IncomeExpenseActivity, "Show the statement first, then tap PDF.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    printStatementPdf(html)
                }
            }
        }
        header.addView(branchChip)
        header.addView(pdfChip)
        col.addView(header)

        // আজ থেকে ৩০ দিন আগে — একটা যুক্তিসঙ্গত ডিফল্ট, TK চাইলে বদলে নেবেন।
        val toDate = dateField(todayIso())
        val fromCal = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply { add(java.util.Calendar.DAY_OF_MONTH, -30) }
        val fromIso = String.format(Locale.US, "%04d-%02d-%02d",
            fromCal.get(java.util.Calendar.YEAR), fromCal.get(java.util.Calendar.MONTH) + 1, fromCal.get(java.util.Calendar.DAY_OF_MONTH))
        val fromDate = dateField(fromIso)
        col.addView(entryCard(listOf("From" to fromDate, "To" to toDate)))

        col.addView(out)
        col.addView(android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(compactFooter("← Back", "Show", { renderMenu() }) {
            loadStatement((fromDate.tag as String), (toDate.tag as String), branchSel, out)
        })

        reload = { loadStatement((fromDate.tag as String), (toDate.tag as String), branchSel, out) }
        reload()
    }

    private fun loadStatement(fromIso: String, toIso: String, branchSel: String, out: LinearLayout) {
        out.removeAllViews()
        if (fromIso > toIso) {
            out.addView(ModuleUi.body(this, NoBengali.s("\"From\" তারিখ \"To\"-এর পরে হতে পারে না।")))
            return
        }
        out.addView(ModuleUi.body(this, "Loading..."))
        // পরের দিন — endExclusive হিসেবে ব্যবহার করা হবে (gte/lt-এর জন্য)।
        val toNext = try {
            val p = toIso.split("-"); val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
            c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()); c.add(java.util.Calendar.DAY_OF_MONTH, 1)
            String.format(Locale.US, "%04d-%02d-%02d", c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1, c.get(java.util.Calendar.DAY_OF_MONTH))
        } catch (_: Throwable) { toIso }
        val branchQ = "&branch=eq.$branchSel"
        Thread {
            val collR = ModuleAuth.getRowsChecked("fin", "collections",
                "select=*&entry_date=gte.$fromIso&entry_date=lt.$toNext&ignored=eq.false$branchQ&order=entry_date.asc")
            val expR = ModuleAuth.getRowsChecked("fin", "expenses",
                "select=*&entry_date=gte.$fromIso&entry_date=lt.$toNext&ignored=eq.false$branchQ&order=entry_date.asc")
            // ওপেনিং ব্যালেন্স — "From"-এর আগের সব দিনের (এই ব্রাঞ্চের) আয়−খরচ।
            // Ledger Sheet/Monthly Summary-র prevBal-এর হুবহু একই দুই-উৎস হিসাব।
            val prevCollR = ModuleAuth.getRowsChecked("fin", "collections",
                "select=cash,online,expense_total,expense_notes&entry_date=lt.$fromIso&ignored=eq.false$branchQ")
            val prevExpR = ModuleAuth.getRowsChecked("fin", "expenses",
                "select=amount&entry_date=lt.$fromIso&ignored=eq.false$branchQ")
            val loadOk = collR.ok && expR.ok
            val prevOk = prevCollR.ok && prevExpR.ok
            var opening = 0.0
            if (prevOk) {
                val pc = prevCollR.rows
                for (i in 0 until pc.length()) {
                    val c = pc.getJSONObject(i)
                    val note = c.optString("expense_notes", "").let { if (it == "null") "" else it }
                    val e = c.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) }
                    opening += c.optDouble("cash", 0.0) + c.optDouble("online", 0.0) - e
                }
                val pe = prevExpR.rows
                for (i in 0 until pe.length()) opening -= pe.getJSONObject(i).optDouble("amount", 0.0)
            }
            val coll = collR.rows; val exp = expR.rows
            runOnUiThread {
                out.removeAllViews()
                if (loadOk) buildStatementTable(coll, exp, opening, prevOk, fromIso, toIso, branchSel, out)
                else out.addView(ModuleUi.body(this, "⚠️ Could not load right now — weak internet. Your data is safe; open this again when online."))
            }
        }.start()
    }

    /** দিন-ধরে টেবিল বানায় (buildMonthlyKhata-র হুবহু একই বক্স-স্টাইল), শুধু
     *  ডানদিকে একটা বাড়তি "চলতি ব্যালেন্স" কলাম — প্রতিটা দিনের পরে জমা কত হলো। */
    private fun buildStatementTable(
        coll: JSONArray, exp: JSONArray, opening: Double, openingOk: Boolean,
        fromIso: String, toIso: String, branchSel: String, out: LinearLayout
    ) {
        val dayCash = LinkedHashMap<String, Double>()
        val dayOnline = LinkedHashMap<String, Double>()
        val dayExp = LinkedHashMap<String, Double>()
        val dates = java.util.TreeSet<String>()
        for (i in 0 until coll.length()) {
            val c = coll.getJSONObject(i)
            val d = c.optString("entry_date"); if (d.isBlank()) continue
            dates.add(d)
            dayCash[d] = (dayCash[d] ?: 0.0) + c.optDouble("cash", 0.0)
            dayOnline[d] = (dayOnline[d] ?: 0.0) + c.optDouble("online", 0.0)
            val note = c.optString("expense_notes", "").let { if (it == "null") "" else it }
            val se = c.optDouble("expense_total", -1.0).let { if (it >= 0.0) it else sumNumbersInText(note) }
            if (se != 0.0 || note.isNotBlank()) dayExp[d] = (dayExp[d] ?: 0.0) + se
        }
        for (i in 0 until exp.length()) {
            val e = exp.getJSONObject(i)
            val d = e.optString("entry_date"); if (d.isBlank()) continue
            dates.add(d)
            dayExp[d] = (dayExp[d] ?: 0.0) + e.optDouble("amount", 0.0)
        }

        val dpx = { v: Int -> ModuleUi.dp(this, v) }
        val boldTf = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        val measure = android.graphics.Paint().apply { typeface = boldTf; textSize = 11f * resources.displayMetrics.scaledDensity }
        val cellPadPx = dpx(8) * 2; val cellSlackPx = dpx(10)
        // 🟢🔒🔒 V657 (২৫.০৮.২০২৬, TK-নির্দেশ — "Date যখন Fixed তাহলে কলম টা
        // ছোট করুন") — Date-এ সবসময় সাধারণ সংখ্যা+স্ল্যাশ (যেমন
        // "02/01/2026") থাকে, জটিল হরফের মাপ-গরমিলের ঝুঁকি নেই — তাই এখানে
        // ছোট slack (bal/amt কলামের ১০dp-র বদলে মাত্র ৪dp) দিয়ে কলামটা
        // সরু করা হলো। ⛔ এখনো পুরো তারিখ কখনো কাটবে না (মাপ ধরেই আসে)।
        val dateSlackPx = dpx(4)
        var dateColPx = maxOf(measure.measureText("Date"), measure.measureText("Total"))
        var amtColPx = maxOf(measure.measureText("Cash"), measure.measureText("Online"))
        var balColPx = maxOf(measure.measureText("Balance"), measure.measureText(money(opening).removePrefix("₹")))
        for (d in dates) {
            val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
            dateColPx = maxOf(dateColPx, measure.measureText(dotted))
            amtColPx = maxOf(amtColPx, measure.measureText(money(dayCash[d] ?: 0.0).removePrefix("₹")),
                measure.measureText(money(dayOnline[d] ?: 0.0).removePrefix("₹")))
        }
        val dateColWidth = dateColPx.toInt() + cellPadPx + dateSlackPx
        val amtColWidth = amtColPx.toInt() + cellPadPx + cellSlackPx

        fun boxCell(text: String, w: Int, bg: String, fg: String, bold: Boolean, weight: Float? = null, gravityV: Int? = null, noWrap: Boolean = false): android.widget.TextView =
            android.widget.TextView(this).apply {
                this.text = text; textSize = 11f
                setTextColor(android.graphics.Color.parseColor(fg))
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                if (gravityV != null) gravity = gravityV
                setPadding(dpx(8), dpx(8), dpx(8), dpx(8))
                layoutParams = if (weight != null) LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                    else LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT)
                background = cellBorderDrawable().apply { setColor(android.graphics.Color.parseColor(bg)) }
                // 🟢🔒🔒 V657 (২৫.০৮.২০২৬, TK-নির্দেশ, ছবিসহ — "চলতি ব্যালেন্স...
                // যাতে টাকার অংশ কেটে না যায়") — Balance ঘরে সংখ্যা দুই লাইনে
                // ভেঙে যাচ্ছিল (maxLines=2 সব weight-ঘরেই বসত)। এখন `noWrap=true`
                // দিলে এক লাইনেই থাকে, কখনো ভাঙে না।
                if (weight != null && !noWrap) { maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END }
            }

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        head.addView(boxCell("Date", dateColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("Cash", amtColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("Online", amtColWidth, "#0A7C3F", "#FFFFFF", true, gravityV = android.view.Gravity.CENTER))
        // 🟢🔒🔒 V657 (২৫.০৮.২০২৬, TK-নির্দেশ — "স্টেটমেন্ট ইংরেজিতে লেখা হবে")
        // — "খরচ"/"চলতি ব্যালেন্স" আগে সরাসরি বাংলা লেখা ছিল (বা শুধু
        // NoBengali-ফ্ল্যাগড স্টাফের ফোনেই ইংরেজি হতো) — TK চান এই পাতা
        // **সবসময়ই** ইংরেজিতে থাকুক, কে দেখছেন তার উপর নির্ভর না করে।
        // তাই এখানে সরাসরি ইংরেজি লেখা বসানো হলো, কোনো শর্ত ছাড়াই।
        head.addView(boxCell("Expense", 0, "#0A7C3F", "#FFFFFF", true, weight = 1f, gravityV = android.view.Gravity.CENTER))
        head.addView(boxCell("Balance", 0, "#0A7C3F", "#FFFFFF", true, weight = 1.6f, gravityV = android.view.Gravity.CENTER, noWrap = true))
        table.addView(head)
        val builtRows = ArrayList<LinearLayout>(); builtRows.add(head)

        // ওপেনিং ব্যালেন্স-এর নিজের সারি — যাতে TK এখান থেকেই যাচাই শুরু করতে পারেন।
        val openRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        openRow.addView(boxCell("Opening", dateColWidth, "#EAF6EE", "#0A5C33", true))
        openRow.addView(boxCell("—", amtColWidth, "#EAF6EE", "#0A5C33", false, gravityV = android.view.Gravity.END))
        openRow.addView(boxCell("—", amtColWidth, "#EAF6EE", "#0A5C33", false, gravityV = android.view.Gravity.END))
        openRow.addView(boxCell("—", 0, "#EAF6EE", "#0A5C33", false, weight = 1f, gravityV = android.view.Gravity.END))
        openRow.addView(boxCell(if (openingOk) money(opening).removePrefix("₹") else "—", 0, "#EAF6EE", "#0A5C33", true, weight = 1.6f, gravityV = android.view.Gravity.END, noWrap = true))
        table.addView(openRow); builtRows.add(openRow)

        var cashTot = 0.0; var onlineTot = 0.0; var expTot = 0.0
        var running = opening
        var idx = 0
        for (d in dates) {
            val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
            val cash = dayCash[d] ?: 0.0; val online = dayOnline[d] ?: 0.0; val e = dayExp[d] ?: 0.0
            cashTot += cash; onlineTot += online; expTot += e
            running += cash + online - e
            val bg = if (idx % 2 == 0) "#FFFFFF" else "#F7FBF8"; idx++
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(boxCell(dotted, dateColWidth, bg, "#41506A", true))
            row.addView(boxCell(if (cash > 0) money(cash).removePrefix("₹") else "-", amtColWidth, bg, "#0A7C3F", false, gravityV = android.view.Gravity.END))
            row.addView(boxCell(if (online > 0) money(online).removePrefix("₹") else "-", amtColWidth, bg, "#0A7C3F", false, gravityV = android.view.Gravity.END))
            // 🔴🔒 V1610 (১৯.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ — "খরচেরগুলো
            // সম্পূর্ণ লাল থাকতে হবে") — খরচ না থাকা দিনের "-"ও আগে থেকেই
            // লাল ছিল, কিন্তু পাতলা (bold নয়) বলে চোখেই পড়ত না। এখন বাকি
            // সংখ্যাগুলোর মতোই বোল্ড, তাই পুরো কলামটাই স্পষ্ট লাল দেখায়।
            row.addView(boxCell(if (e > 0) money(e).removePrefix("₹") else "-", 0, bg, "#B42318", true, weight = 1f, gravityV = android.view.Gravity.END))
            row.addView(boxCell(money(running).removePrefix("₹"), 0, bg, "#0F3A66", true, weight = 1.6f, gravityV = android.view.Gravity.END, noWrap = true))
            table.addView(row); builtRows.add(row)
        }
        val tot = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tot.addView(boxCell("Total", dateColWidth, "#EAF6EE", "#0A5C33", true))
        tot.addView(boxCell(money(cashTot).removePrefix("₹"), amtColWidth, "#EAF6EE", "#0A5C33", true, gravityV = android.view.Gravity.END))
        tot.addView(boxCell(money(onlineTot).removePrefix("₹"), amtColWidth, "#EAF6EE", "#0A5C33", true, gravityV = android.view.Gravity.END))
        tot.addView(boxCell(money(expTot).removePrefix("₹"), 0, "#EAF6EE", "#B42318", true, weight = 1f, gravityV = android.view.Gravity.END))
        tot.addView(boxCell(if (openingOk) money(running).removePrefix("₹") else "—", 0, "#EAF6EE", "#0F3A66", true, weight = 1.6f, gravityV = android.view.Gravity.END, noWrap = true))
        table.addView(tot); builtRows.add(tot)

        out.addView(table)
        if (dates.isEmpty()) out.addView(ModuleUi.body(this, NoBengali.s("এই সময়ের মধ্যে এখনো কোনো এন্ট্রি নেই।")))
        TableRowEqualizer.equalize(table, builtRows)

        // 🟢🔒🔒 V657 (২৫.০৮.২০২৬, TK-নির্দেশ — "pdf ডাউনলোড করা যাবে সেরকম
        // ব্যবস্থা রাখবেন") — উপরের একই টেবিলের একটা HTML প্রতিলিপি, শুধু
        // PDF বোতামের জন্য (printStatementPdf → WebView+PrintManager)।
        // ⛔ কোনো টাকার হিসাব নতুন করে গোনা হয়নি — একই dates/dayCash/
        // dayOnline/dayExp/running-এর উপর দিয়েই আরেকবার লেখা হয়।
        /* 🎨🔒 V1610 (১৯.০৯.২০২৬, TK-নির্দেশ, ফটো-প্রুফ পাশ — "আরো প্রফেশনাল
           বানাতে হবে", "খরচেরগুলো সম্পূর্ণ লাল থাকতে হবে") — আগে শুধু সাদা
           টেবিল, ব্র্যান্ডিং/সারাংশ কিছুই ছিল না, খরচের "-" পাতলা লাল বলে
           চোখেই পড়ত না। এখন: ① ক্লিনিকের নাম + তৈরির তারিখ সহ লেটারহেড
           ② Opening/Total In/Total Expense/Closing — চারটে সারাংশ বাক্স,
           এক নজরে বোঝার জন্য ③ Expense কলামের সংখ্যা ও "-" দুটোই বোল্ড লাল।
           ⛔ টাকার হিসাব এক অক্ষরও বদলায়নি — শুধু দেখানোর ধরন। */
        run {
            val sb = StringBuilder()
            sb.append("<html><head><meta charset='utf-8'><style>")
                .append("*{-webkit-print-color-adjust:exact;print-color-adjust:exact}")
                .append("body{font-family:sans-serif;padding:16px;color:#101C2E;font-variant-numeric:tabular-nums}")
                .append(".lh{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #0A5C33;padding-bottom:10px;margin-bottom:14px}")
                .append(".lh .clinic{font-size:15px;font-weight:bold;color:#0A5C33}")
                .append(".lh .branch{font-size:12px;color:#667085;margin-top:2px}")
                .append(".lh .right{text-align:right;font-size:11px;color:#667085;line-height:1.6}")
                .append(".lh .right b{color:#101C2E}")
                .append(".summary{display:flex;gap:8px;margin-bottom:14px}")
                .append(".stile{flex:1;border-radius:9px;padding:8px 10px;border:1px solid #E3E9EF;background:#FAFCFB}")
                .append(".stile .k{font-size:9px;font-weight:bold;letter-spacing:.06em;color:#667085;text-transform:uppercase}")
                .append(".stile .v{font-size:14px;font-weight:bold;margin-top:3px}")
                .append(".stile.op{border-color:#CDEAD9}.stile.op .v{color:#0A5C33}")
                .append(".stile.in{border-color:#CDEAD9}.stile.in .v{color:#0A7C3F}")
                .append(".stile.exp{border-color:#F3C6BE;background:#FDEDEB}.stile.exp .v{color:#B42318}")
                .append(".stile.cl{border-color:#C7D7EA}.stile.cl .v{color:#0F3A66}")
                .append("table{border-collapse:collapse;width:100%;font-size:13px}")
                .append("th,td{border:1px solid #D9E2EC;padding:6px 8px;text-align:right}")
                .append("th{background:#0A7C3F;color:#fff}")
                .append("td:first-child,th:first-child{text-align:left}")
                .append(".open,.tot{background:#EAF6EE;font-weight:bold;color:#0A5C33}")
                .append(".exp{color:#B42318;font-weight:bold}.bal{color:#0F3A66;font-weight:bold}")
                .append("</style></head><body>")
            val fromDot2 = try { val p = fromIso.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { fromIso }
            val toDot2 = try { val p = toIso.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { toIso }
            val genDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
            sb.append("<div class='lh'><div><div class='clinic'>MAA AYURVED PILES CLINIC</div>")
                .append("<div class='branch'>Statement — ").append(branchSel).append("</div></div>")
                .append("<div class='right'>").append(fromDot2).append(" – ").append(toDot2)
                .append("<br>Generated: <b>").append(genDate).append("</b></div></div>")
            sb.append("<div class='summary'>")
                .append("<div class='stile op'><div class='k'>Opening</div><div class='v'>")
                .append(if (openingOk) money(opening) else "—").append("</div></div>")
                .append("<div class='stile in'><div class='k'>Total In</div><div class='v'>")
                .append(money(cashTot + onlineTot)).append("</div></div>")
                .append("<div class='stile exp'><div class='k'>Total Expense</div><div class='v'>")
                .append(money(expTot)).append("</div></div>")
                .append("<div class='stile cl'><div class='k'>Closing</div><div class='v'>")
                .append(if (openingOk) money(running) else "—").append("</div></div>")
                .append("</div>")
            sb.append("<table><tr><th>Date</th><th>Cash</th><th>Online</th><th>Expense</th><th>Balance</th></tr>")
            sb.append("<tr class='open'><td>Opening</td><td>—</td><td>—</td><td>—</td><td>")
                .append(if (openingOk) money(opening).removePrefix("₹") else "—").append("</td></tr>")
            var runningPdf = opening
            for (d in dates) {
                val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
                val cash = dayCash[d] ?: 0.0; val online = dayOnline[d] ?: 0.0; val e2 = dayExp[d] ?: 0.0
                runningPdf += cash + online - e2
                sb.append("<tr><td>").append(dotted).append("</td><td>")
                    .append(if (cash > 0) money(cash).removePrefix("₹") else "-").append("</td><td>")
                    .append(if (online > 0) money(online).removePrefix("₹") else "-").append("</td><td class='exp'>")
                    .append(if (e2 > 0) money(e2).removePrefix("₹") else "-").append("</td><td class='bal'>")
                    .append(money(runningPdf).removePrefix("₹")).append("</td></tr>")
            }
            sb.append("<tr class='tot'><td>Total</td><td>").append(money(cashTot).removePrefix("₹"))
                .append("</td><td>").append(money(onlineTot).removePrefix("₹"))
                .append("</td><td class='exp'>").append(money(expTot).removePrefix("₹"))
                .append("</td><td>").append(if (openingOk) money(runningPdf).removePrefix("₹") else "—").append("</td></tr>")
            sb.append("</table></body></html>")
            statementPdfHtml = sb.toString()
        }

        // 🔵 R6-এর হুবহু একই প্যাটার্নে WhatsApp শেয়ার — প্রতিটা দিনের পরের
        // চলতি ব্যালেন্সও লেখায় যায়, TK চাইলে কাউকে পাঠিয়ে মিলিয়ে নিতে পারবেন।
        if (dates.isNotEmpty()) {
            val fromDotted = try { val p = fromIso.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { fromIso }
            val toDotted = try { val p = toIso.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { toIso }
            val sbx = StringBuilder()
            sbx.append("📄 স্টেটমেন্ট — ").append(fromDotted).append(" থেকে ").append(toDotted).append("\n")
            sbx.append(branchSel).append("\n")
            sbx.append("————————————\n")
            sbx.append("Opening Balance: ").append(if (openingOk) money(opening) else "—").append("\n")
            var run2 = opening
            for (d in dates) {
                val dotted = try { val p = d.split("-"); p[2] + "/" + p[1] + "/" + p[0] } catch (e: Exception) { d }
                run2 += (dayCash[d] ?: 0.0) + (dayOnline[d] ?: 0.0) - (dayExp[d] ?: 0.0)
                sbx.append(dotted).append(" — Cash ").append(money(dayCash[d] ?: 0.0))
                    .append(" · Online ").append(money(dayOnline[d] ?: 0.0))
                    .append(" · খরচ ").append(money(dayExp[d] ?: 0.0))
                    .append(" · Balance ").append(money(run2)).append("\n")
            }
            sbx.append("————————————\n")
            sbx.append("মোট: Cash ").append(money(cashTot)).append(" · Online ").append(money(onlineTot)).append(" · খরচ ").append(money(expTot)).append("\n")
            sbx.append("Closing Balance: ").append(if (openingOk) money(running) else "—")
            val shareBtn = ModuleUi.button(this, "📤 WhatsApp-এ শেয়ার") {
                try { com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, sbx.toString()) }
                catch (e: Throwable) { ModuleUi.toast(this, "শেয়ার করা গেল না") }
            }
            shareBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
            out.addView(shareBtn)
        }
    }
}
