package com.tkbiswas.pilesclinic.clinical

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.modules.ModuleUi
import com.tkbiswas.pilesclinic.native.PremiumAlert
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 🖥️🔒 V982 (০২.০৯.২০২৬, TK-এর পাশ-করা ফটো-প্রুফ) —
 * **এস্টিমেটের ফুল-স্ক্রিন কাগজ।**
 *
 * TK-এর নিজের কথা:
 *  • *"আমি তো চেয়েছিলাম A4 সাইজ প্রিন্ট আউট হওয়ার পর যেমন হবে, ঠিক সে রকম
 *    ভাবেই এখানে থাকবে"*
 *  • *"সেভ করলে সেভাবে, প্রিন্ট করলে প্রিন্ট হবে, শেয়ার করলে শেয়ার হবে"*
 *  • *"তাহলে ফুল স্ক্রিন পর্দা খুলবে, আলাদা পপ-আপ লাগবে না, zoom করা যাবে"*
 *
 * ─── কীভাবে ─────────────────────────────────────────────────────────────
 * পর্দায় যেটা দেখা যায় সেটা **ঠিক ছাপার কাগজটাই** (`EstimateHtmlPrint`)।
 * শুধু পর্দার জন্য হলুদ ঘর ও লিঙ্ক বসে (`editable = true`); ছাপা ও শেয়ারের
 * সময় ওগুলো থাকে না — অর্থাৎ **যা দেখছি হুবহু তাই ছাপে**।
 *
 * ⛔ কোনো JavaScript-সেতু নেই — হলুদ ঘরে চাপ দিলে পাতা `est://…` ঠিকানায়
 *    যেতে চায়, WebView সেটা আটকে দেয় আর অ্যাপ ছোট বাক্স খুলে দেয়। তাই
 *    হিসাব সবসময় একটাই জায়গায় (`EstimateModel`) — পর্দা আর কাগজে কখনো
 *    দুরকম সংখ্যা হতে পারে না।
 * ⛔ ছাপা ও শেয়ার প্রকল্পের **প্রমাণিত পথেই** (`PrescriptionWhatsAppShare`)।
 */
class EstimatePaperActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SHEET = "estimate_sheet_json"
        const val RESULT_SHEET = "estimate_sheet_json_out"
    }

    private lateinit var sheet: EstimateModel.Sheet
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sheet = try { EstimateModel.parse(intent?.getStringExtra(EXTRA_SHEET).orEmpty()) }
                catch (_: Throwable) { EstimateModel.Sheet() }
        /* 💊🔒 V986 (০২.০৯.২০২৬, TK-নির্দেশ: *"এইগুলি ফরমে অটোমেটিক বসে থাকবে…
           প্রত্যেকবার আমি কেন চুস করতে যাব"*) — নতুন এস্টিমেট খুললে ওষুধ ও
           অন্যান্য (Dressing · Nursing) কাগজে **আগে থেকেই** বসানো থাকে; যেটা
           লাগবে না সেটা কেটে বা মুছে দিলেই হলো।
           ⛔ চিকিৎসার লাইন বসানো হয় না — গ্রেড/ইঞ্চি রোগভেদে আলাদা, ডাক্তারকেই
              বাছতে হয়। ⛔ আগের সেভ করা হিসাব খুললে কিচ্ছু যোগ হয় না। */
        /* 🔴🔴🔒 V1233 (০৮.০৯.২০২৬ — TK, ছবিসহ: *"এই ব্যক্তির এস্টিমেট সেভ করা
           হয়েছিল, পুনরায় খুললাম তখন কেন রোগের নামগুলো কাটা হয়ে গেল · মেডিসিনের
           কোয়ান্টিটি একবার সেভ করলে সেটাই ডিফল্ট থাকবে, কেন আবার ১ হয়ে যাচ্ছে"*)।

           **গভীরে যাচাই করে পাওয়া আসল কারণ:** এই পর্দার **SAVE** বোতাম হিসাবটা
           শুধু চেকআপ-পর্দায় **ফেরত পাঠায়** (`setResult`), সেটা মেমরিতে বসে।
           কাগজটা সত্যিই পাকাপাকি লেখা হয় **চেকআপ পর্দার নিজের Save-এ**
           (`estimateJson`, DoctorCheckupActivity)। ⇒ ডাক্তার এখানে SAVE চেপে
           যদি চেকআপের Save না চেপে বেরিয়ে যান, হিসাবটা কোথাও থাকে না।
           পরের বার খুললে `sheet.lines` ফাঁকা ⇒ নিচের `prefillFromPriceList()`
           চলে ⇒ দরের তালিকা থেকে ওষুধ/অন্যান্য নতুন করে বসে — **সবগুলো কাটা
           অবস্থায় (V1113-এর নিয়ম) আর qty = ১**। TK-র ছবিতে ঠিক এটাই।

           ⇒ **সমাধান (কোনো পুরনো পথ না ভেঙে):** SAVE চাপলেই হিসাবটা **এই ফোনেই
             রোগীর নামে জমা** থাকে; পরের বার কাগজ খুললে ক্লাউড/চেকআপ থেকে কিছু
             না এলে **ওই জমা হিসাবটাই** ফিরে আসে — কাটা-দাগ ও qty সহ হুবহু।
           ⛔ চেকআপের Save চাপলে আগের মতোই ক্লাউডে যায়; তখন সেটাই জেতে।
           ⛔ কোনো টাকার হিসাব · ছাড়ের নিয়ম · V1113-এর কাটা-নিয়ম বদলায়নি।
           ⛔ ফাঁকা হিসাব কখনো জমা হয় না, তাই নতুন রোগীর কাগজ আগের মতোই খোলে। */
        if (sheet.lines.isEmpty()) {
            val remembered = estRememberedJson()
            if (remembered.isNotBlank()) {
                sheet = try { EstimateModel.parse(remembered) } catch (_: Throwable) { sheet }
            }
        }
        if (sheet.lines.isEmpty()) prefillFromPriceList()
        setContentView(buildScreen())
        render()
    }

    /* 🔴🔒 V786-এর নিয়ম এই পর্দাতেও — কল এলে বা মেমরির চাপে Android পর্দা
       ভেঙে আবার বানালে রোগীর পরিচয় যেন হারিয়ে না যায়।
       ⛔ মেমরিতে রোগী থাকলে `restoreFrom()` কিচ্ছু করে না (RoleSession.kt)। */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        RoleSession.saveTo(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        RoleSession.restoreFrom(savedInstanceState)
    }

    /* 🔴🔒 V1233 — এই ফোনে রোগীর নামে হিসাব মনে রাখা।
       ⛔ শুধু এই ফোনে (SharedPreferences) — ক্লাউডে একটাও নতুন লেখা/পড়া নেই,
          তাই খরচ এক পয়সাও বাড়ে না। ⛔ রোগী চেনা না গেলে কিছুই জমে না। */
    /* ⛔ অ্যাপের নিজের Context ধরে (প্রকল্পের বাকি সব জায়গার হুবহু একই ধরন) —
       Activity-র উত্তরাধিকারী ঘর ব্যবহার করলে কম্পাইল-পাহারা ধরতে পারে না। */
    private fun estPrefs(): android.content.SharedPreferences? = try {
        com.tkbiswas.pilesclinic.PilesClinicApplication.appContext
            ?.getSharedPreferences("piles_clinic_estimate_last", android.content.Context.MODE_PRIVATE)
    } catch (_: Throwable) { null }

    private fun estKey(): String = try {
        val pid = RoleSession.currentPatientId.orEmpty().trim()
        val mob = RoleSession.currentPatientMobile.orEmpty().filter { it.isDigit() }.takeLast(10)
        if (pid.isNotBlank()) "pid_" + pid else if (mob.length == 10) "mob_" + mob else ""
    } catch (_: Throwable) { "" }

    private fun estRemember(s: EstimateModel.Sheet) {
        try {
            val k = estKey(); if (k.isBlank() || s.isEmpty) return
            val pr = estPrefs() ?: return
            pr.edit().putString(k, s.toJson().toString()).commit()
        } catch (_: Throwable) { }
    }

    private fun estRememberedJson(): String = try {
        val k = estKey()
        if (k.isBlank()) "" else (estPrefs()?.getString(k, "") ?: "")
    } catch (_: Throwable) { "" }

    /** 💊 V986 — দরের তালিকা থেকে ওষুধ ও অন্যান্য সব লাইন বসানো। */
    private fun prefillFromPriceList() {
        try {
            /* 🚫🔒 V1113 (TK-নির্দেশ: *"ট্রিটমেন্টের খরচ ছাড়া বাকিগুলো স্ট্রাইক
               কাট হিসাবে অল টাইম সেট থাকবে, আমি চাইলে পরিবর্তন করতে পারবো"*) —
               কাগজ খোলার সময় নিজে থেকে বসা এই ওষুধ ও অন্যান্য লাইনগুলোও
               **কাটা অবস্থাতেই** বসে (`EstimateDialog`-এর হুবহু একই নিয়ম)।
               ⛔ টাকাটা প্রমাণিত `onStrikeToggled` পথেই ছাড়ে যায় — নতুন কোনো
                  হিসাব লেখা হয়নি। ⛔ চাপ দিয়ে যেকোনো সময় তোলা যাবে। */
            for (g in listOf(EstimatePrices.G_MEDICINE, EstimatePrices.G_OTHER)) {
                for (item in EstimatePrices.inGroup(this, g)) {
                    /* 🔢 V1251 (TK-নির্দেশ) — Price List-এ বসানো "Default
                       quantity" এখান থেকেই আসে। ⛔ কিছু বসানো না থাকলে
                       `qtyOf` আগের মতোই ১ দেয়, তাই কিছুই বদলায় না। */
                    val line = EstimateModel.Line(
                        name = item.name, rate = item.rate,
                        qty = EstimatePrices.qtyOf(item))
                    sheet.lines.add(line)
                    line.struck = true
                    sheet.onStrikeToggled(line, true)
                }
            }
        } catch (_: Throwable) { }
    }

    private fun dp(v: Int) = ModuleUi.dp(this, v)

    private fun box(fill: String, stroke: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(fill))
        setStroke(dp(1), Color.parseColor(stroke))
        cornerRadius = dp(radius).toFloat()
    }

    // ─────────────────────────── পর্দা ───────────────────────────
    private fun buildScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EDF1EE"))
        }

        // উপরের সবুজ পট্টি
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0B5B2F"))
            setPadding(dp(14), dp(12), dp(16), dp(12))
            addView(TextView(this@EstimatePaperActivity).apply {
                text = "◀"
                textSize = 17f
                setTextColor(Color.WHITE)
                setPadding(dp(4), 0, dp(12), 0)
                setOnClickListener { askBeforeLeaving() }
            })
            addView(TextView(this@EstimatePaperActivity).apply {
                text = "COST ESTIMATE  ·  A4"
                textSize = 15.5f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.WHITE)
                letterSpacing = 0.04f
            })
        })

        // যোগ করার সারি
        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(7), dp(8), dp(7))
        }
        fun chip(text: String, weight: Float, colour: String, dashed: Boolean, run: () -> Unit) {
            tools.addView(TextView(this).apply {
                this.text = text
                textSize = 12.5f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(colour))
                background = box("#FFFFFF", if (dashed) "#C9D6CD" else "#B9CBE0", 11)
                setPadding(dp(4), dp(9), dp(4), dp(9))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                    .apply { leftMargin = dp(3); rightMargin = dp(3) }
                setOnClickListener { run() }
            })
        }
        chip("+ Treatment", 1f, "#0B66D8", false) { EstimateDialog.pickTreatment(this, sheet) { render() } }
        chip("+ Medicine", 1f, "#0B66D8", false) { EstimateDialog.pickMedicine(this, sheet) { render() } }
        chip("+ Other", 1f, "#0B66D8", false) { EstimateDialog.pickOther(this, sheet) { render() } }
        chip("Price List", 0.9f, "#5B6B61", true) {
            startActivity(android.content.Intent(this, PriceListActivity::class.java))
        }
        root.addView(tools)

        /* ⏳🔒 V1280 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — তালিকা সারি ৪০৩):
           *"শুধু কত সময় চাওয়া হলো সেটাও যদি এস্টিমেটের মধ্যে যুক্ত করে দেয়া
           হয়"* ⇒ বোতাম-সারির ঠিক নিচে **Time Asked [সংখ্যা] [Days ▾]**।
           লেখাটা কাগজেও ছাপে (`EstimateHtmlPrint`) আর চেকআপে ফিরে গিয়ে
           আগের `timeAsked` ঘরেই বসে। ⛔ টাকার হিসাবে এর কোনো হাত নেই। */
        val taRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), dp(2), dp(12), dp(8))
        }
        /* 🇧🇩🔒 V1327 (১১.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK নিজে বাংলা
           লেখা চেয়েছেন এই একটা লেবেলে। প্রকল্পের আগে থেকেই এই একই কথার প্রমাণিত
           বাংলা অনুবাদ আছে (`CheckupA4Lang`-এর "recovery" চাবি, A4 কাগজে ব্যবহৃত)
           — সেটাই এখানে বসানো হলো, নতুন কোনো অনুবাদ বানানো হয়নি। */
        taRow.addView(TextView(this).apply {
            text = "কতদিন সময় চাওয়া হল"; textSize = 12.5f
            setTextColor(Color.parseColor("#123A26"))
            setPadding(0, 0, dp(10), 0)
        })
        val (taAmt0, taUnit0) = CounselModel.splitTimeAsked(sheet.timeAsked)
        /* 🎨🔒 V1460 (১৪.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — "Days এর বক্সের
           উচ্চতা এত বেশি কেন... Treatment Medicine Other এর বক্স গুলি যেমন ঠিক
           তেমন ই হতে হবে"। এখন দুটো বাক্সই উপরের chip()-এর মতোই সাদা+নীল-বর্ডার
           সাজ ও উচ্চতা — নেটিভ Spinner সরিয়ে TextView+PopupMenu বসানো হলো। */
        var taUnitSel = CounselModel.UNITS.indexOf(taUnit0).let { if (it < 0) 0 else it }
        val taAmt = android.widget.EditText(this).apply {
            setText(taAmt0)
            // ⛔ পাহারা ৯.১৭ — প্রকল্পের নিয়ম: TYPE_CLASS_TEXT + DigitsKeyListener (কীবোর্ড না-খোলার ফাঁদ এড়াতে)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
            textSize = 13f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B66D8"))
            background = box("#FFFFFF", "#B9CBE0", 11)
            setPadding(dp(4), dp(9), dp(4), dp(9))
            layoutParams = LinearLayout.LayoutParams(dp(44), LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { rightMargin = dp(6) }
        }
        val taUnit = TextView(this).apply {
            text = "$taUnit0 ▾"
            textSize = 12.5f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B66D8"))
            background = box("#FFFFFF", "#B9CBE0", 11)
            setPadding(dp(10), dp(9), dp(10), dp(9))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        fun pushTimeAsked() {
            sheet.timeAsked = CounselModel.timeAsked(
                taAmt.text?.toString().orEmpty(),
                CounselModel.UNITS.getOrElse(taUnitSel) { "Days" })
            render()
        }
        taAmt.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(e: android.text.Editable?) { pushTimeAsked() }
            override fun beforeTextChanged(t: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(t: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        taUnit.setOnClickListener {
            val popup = android.widget.PopupMenu(this, taUnit)
            CounselModel.UNITS.forEachIndexed { idx, u -> popup.menu.add(0, idx, idx, u) }
            popup.setOnMenuItemClickListener { item ->
                taUnitSel = item.itemId
                taUnit.text = "${CounselModel.UNITS[taUnitSel]} ▾"
                pushTimeAsked()
                true
            }
            popup.show()
        }
        taRow.addView(taAmt); taRow.addView(taUnit)
        root.addView(taRow)

        // কাগজ — দুই আঙুলে জুম হয়
        web = WebView(this).apply {
            setBackgroundColor(Color.parseColor("#EDF1EE"))
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            isVerticalScrollBarEnabled = true
            webViewClient = object : WebViewClient() {
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                    handleTap(url)
                override fun shouldOverrideUrlLoading(
                    view: WebView, request: android.webkit.WebResourceRequest
                ): Boolean = handleTap(request.url?.toString())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(web)

        // নিচের তিনটে বোতাম
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(8), dp(8), dp(8), dp(10))
        }
        fun action(text: String, fill: String, ink: String, run: () -> Unit) {
            bottom.addView(TextView(this).apply {
                this.text = text
                textSize = 13.5f
                gravity = Gravity.CENTER
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(Color.parseColor(ink))
                background = box(fill, fill, 12)
                setPadding(dp(4), dp(13), dp(4), dp(13))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { leftMargin = dp(4); rightMargin = dp(4) }
                setOnClickListener { run() }
            })
        }
        action("💾  SAVE", "#EEF2F7", "#41506A") { saveAndClose() }
        action("🖨  PRINT", "#0F3D6B", "#FFFFFF") { printPaper() }
        action("📤  SHARE", "#0B4F2A", "#FFFFFF") { sharePaper() }
        root.addView(bottom)
        return root
    }

    // ─────────────────────────── কাগজ আঁকা ───────────────────────────
    /** পর্দার কাগজ — ছাপার কাগজটাই, শুধু হলুদ ঘর ও লিঙ্ক যোগ করা। */
    private fun paperHtml(editable: Boolean): String {
        val age = RoleSession.currentPatientAge
        val sex = RoleSession.currentPatientSex
        val ageSex = listOf(age, sex).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "-" }
        return EstimateHtmlPrint.build(
            sheet = sheet,
            branch = RoleSession.currentPatientBranch,
            name = RoleSession.currentPatientName.ifBlank { "-" },
            patientId = RoleSession.currentPatientDisplayId.ifBlank { RoleSession.currentPatientId },
            ageSex = ageSex,
            mobile = RoleSession.currentPatientMobile.ifBlank { "-" },
            address = RoleSession.currentPatientAddress.ifBlank { "-" },
            date = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(java.util.Date()),
            editable = editable
        )
    }

    private fun render() {
        try {
            web.loadDataWithBaseURL(
                "file:///android_asset/", paperHtml(true), "text/html", "UTF-8", null
            )
        } catch (_: Throwable) { }
    }

    // ─────────────────────────── চাপ দিলে ───────────────────────────
    /** `est://line/3` বা `est://discount` — অ্যাপ নিজে ছোট বাক্স খোলে। */
    private fun handleTap(url: String?): Boolean {
        val u = url.orEmpty()
        if (!u.startsWith("est://")) return false
        try {
            if (u.startsWith("est://discount")) { editDiscount(); return true }
            val at = u.removePrefix("est://line/").toIntOrNull() ?: return true
            if (at in sheet.lines.indices) editLine(at)
        } catch (_: Throwable) { }
        return true
    }

    private fun numberField(value: String): EditText = EditText(this).apply {
        setText(value)
        textSize = 14f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#101C2E"))
        /* 🔒 খাতার সারি B411 — শুধু সংখ্যার ধরন দিলে কিছু ফোনে কীবোর্ড খোলে না। */
        inputType = InputType.TYPE_CLASS_TEXT
        keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
        background = box("#F8FBFE", "#D6E1EE", 9)
        setPadding(dp(10), dp(8), dp(10), dp(8))
    }

    private fun caption(text: String) = TextView(this).apply {
        this.text = text
        textSize = 9.5f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#8B98A9"))
        letterSpacing = 0.09f
        setPadding(0, dp(8), 0, dp(3))
    }

    private fun editLine(at: Int) {
        val line = sheet.lines[at]
        val rate = numberField(EstimateModel.moneyShort(line.rate))
        val qty = numberField(EstimateModel.moneyShort(line.qty))
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(6), dp(18), dp(4))
            addView(caption("RATE (₹)")); addView(rate)
            addView(caption("QUANTITY")); addView(qty)
        }
        val strikeLabel = if (line.struck) "Un-strike" else "Strike out"
        val dlg = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "✏️ " + line.name))
            .setView(body)
            .setPositiveButton("Save") { _, _ ->
                line.rate = EstimateModel.num(rate.text?.toString())
                line.qty = EstimateModel.num(qty.text?.toString())
                render()
            }
            .setNeutralButton(strikeLabel) { _, _ -> line.struck = !line.struck; render() }
            .setNegativeButton("Delete") { _, _ ->
                try { sheet.lines.removeAt(at) } catch (_: Throwable) { }
                render()
            }
            .create()
        dlg.show()
        try { PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    /* ═══════════════════════════════════════════════════════════════════
       💸🔒 V1113 (০৫.০৯.২০২৬, TK-রিপোর্ট ছবিসহ): *"আমি এখানে বুঝতেই পারলাম না
       যে পার্সেন্টেজ কত দিব। সহজ সরল ভাষা এখানে লিখে রাখুন অনলি পার্সেন্টেজ —
       তার উপরে আমি ৮ ৯ ১০ ১৫ ২০ যা করব সেটাই হবে।"*
       TK-এর সিদ্ধান্ত: *"₹ রাখুন, % ডিফল্ট থাকবে"*।

       🔴 আগে কী হত: ঘরে "9" লিখলে সেটা ₹৯ না ৯% — বোঝার উপায় ছিল না, কারণ
          ডিফল্ট ছিল **₹**, আর নিচের লেখাটা ("Now in ₹ — tap to use %") ছোট ও
          দ্ব্যর্থক।
       ⇒ এখন ① নতুন ছাড় **সবসময় শতাংশেই** শুরু হয় ② ঘরের মাথায় বড় করে লেখা
         থাকে ছাড়টা কীসে ③ নিচের বোতামে স্পষ্ট লেখা, চাপ দিলে ₹-এ যাওয়া যায়।
       ⛔ ₹ তোলা হয়নি (TK-নির্দেশ) — টাকায় ছাড় আগের মতোই দেওয়া যায়।
       ⛔ হিসাবের নিয়ম এক অক্ষরও বদলায়নি।
       ═══════════════════════════════════════════════════════════════════ */
    private fun discModeText(pct: Boolean): String =
        if (pct) "NOW: PERCENT %   ·   tap for RUPEE ₹" else "NOW: RUPEE ₹   ·   tap for PERCENT %"

    private fun editDiscount() {
        /* 💸 V1113 — ছাড় বসানোই না থাকলে শতাংশ দিয়েই শুরু (TK-এর ডিফল্ট)। */
        if (sheet.discount <= 0.0) sheet.discountPct = true
        val field = numberField(if (sheet.discount > 0) EstimateModel.moneyShort(sheet.discount) else "")
        /* 💸 V1113 — মাথার লেখাটাই বলে দেয় ছাড়টা কীসে, তাই "9" মানে কী সেটা
           নিয়ে আর সন্দেহ থাকে না। */
        val capt: TextView? = caption(
            if (sheet.discountPct) "DISCOUNT IN PERCENT  (%)" else "DISCOUNT IN RUPEE  (₹)"
        ).apply {
            // 💸 V1113 — TK যেন এক নজরেই পড়তে পারেন: বড় ও গাঢ় (৯.৫sp ছিল, খুব ছোট)।
            textSize = 12.5f
            setTextColor(Color.parseColor("#0F3D6B"))
        }
        val mode = TextView(this).apply {
            text = discModeText(sheet.discountPct)
            textSize = 12.5f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F3D6B"))
            background = box("#EEF2F7", "#DCE4EF", 10)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        mode.setOnClickListener {
            sheet.discountPct = !sheet.discountPct
            mode.text = discModeText(sheet.discountPct)
            capt?.text = if (sheet.discountPct) "DISCOUNT IN PERCENT  (%)" else "DISCOUNT IN RUPEE  (₹)"
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(6), dp(18), dp(4))
            addView(capt!!); addView(field)
            addView(mode.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            })
        }
        val dlg = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "💸 Discount"))
            .setView(body)
            .setPositiveButton("Save") { _, _ ->
                sheet.discount = EstimateModel.num(field.text?.toString())
                render()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dlg.show()
        try { PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    // ─────────────────────────── তিনটে কাজ ───────────────────────────
    private fun saveAndClose() {
        estRemember(sheet)   // 🔴 V1233 — ফোনেই জমা, যাতে পরের বার ফিরে আসে
        setResult(RESULT_OK, android.content.Intent().putExtra(RESULT_SHEET, sheet.toJson().toString()))
        finish()
    }

    /** A4 কাগজে ছাপা — প্রকল্পের প্রমাণিত পথেই (WebView → PrintManager)। */
    private var printWeb: WebView? = null

    private fun printPaper() {
        if (sheet.isEmpty) { Toast.makeText(this, "Add at least one item first", Toast.LENGTH_SHORT).show(); return }
        try {
            val wv = WebView(this)
            wv.settings.javaScriptEnabled = false
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    try {
                        val pm = getSystemService(android.content.Context.PRINT_SERVICE)
                            as android.print.PrintManager
                        pm.print(
                            "Estimate", view.createPrintDocumentAdapter("Estimate"),
                            com.tkbiswas.pilesclinic.native.PrintQuality.builder().build()
                        )
                    } catch (_: Throwable) {
                        Toast.makeText(this@EstimatePaperActivity, "Print not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            printWeb = wv
            wv.loadDataWithBaseURL("file:///android_asset/", paperHtml(false), "text/html", "UTF-8", null)
        } catch (_: Throwable) {
            Toast.makeText(this, "Print not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePaper() {
        if (sheet.isEmpty) { Toast.makeText(this, "Add at least one item first", Toast.LENGTH_SHORT).show(); return }
        try {
            com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
                activity = this,
                html = paperHtml(false),
                documentTitle = "Estimate",
                patientName = RoleSession.currentPatientName,
                allowPrint = true
            )
        } catch (e: Throwable) {
            Toast.makeText(this, "Share not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** ⛔ ভুল করে বেরিয়ে গেলে হিসাব যেন হারিয়ে না যায়। */
    private fun askBeforeLeaving() {
        if (sheet.isEmpty) { finish(); return }
        val dlg = AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "⚠️ Are you sure?"))
            .setMessage("Leave without saving this estimate?")
            .setPositiveButton("Save") { _, _ -> saveAndClose() }
            .setNegativeButton("Leave") { _, _ -> finish() }
            .setNeutralButton("Stay", null)
            .create()
        dlg.show()
        try { PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() { askBeforeLeaving() }
}
