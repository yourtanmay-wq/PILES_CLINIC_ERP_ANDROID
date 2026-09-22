package com.tkbiswas.pilesclinic.native

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * 📊 V824 — বার্ষিক রেজিস্ট্রেশনের বিস্তারিত (শুধু মাস্টার)।
 *
 * TK-নির্দেশ (২৯.০৮.২০২৬): মাসভিত্তিক হিসাব + রোগীর তালিকা, আর
 * *"ওই লিস্ট থেকে আমি যদি কিছু বাদ দিয়ে দেই… সেটা প্রয়োজনে আমি দেখে দেখে
 * বাদ দিতে পারব, তার ব্যবস্থা রাখবেন"* — তাই প্রতিটি নামের পাশে **Skip**,
 * আর ভুল হলে **Undo**।
 *
 * ⛔ তালিকাটা Draft পর্দা থেকেই তৈরি হয়ে আসে — এই পর্দা রোগীর কোনো টেবিল
 *    নতুন করে পড়ে না (Egress বাড়ে না)। শুধু "বাদ-দেওয়া" ছোট্ট তালিকাটা
 *    (`fin.registration_count_excluded`, কয়েকটা সারি) ক্লাউড থেকে মিলিয়ে
 *    নেওয়া হয়, যাতে অন্য ফোনে করা বাদও এখানে দেখা যায়।
 * ⛔ কোনো রোগীর রেকর্ড · টাকা · Follow-up কিচ্ছু ছোঁয়া হয় না। "Skip" মানে
 *    শুধু **এই গোনায় ধরা হবে না**।
 */
class YearlyRegistrationActivity : AppCompatActivity() {

    private var branch: String = ""
    private var year: String = ""
    private var rows: MutableList<DraftEntry> = mutableListOf()

    /* 🆕🔒 V-YEARSW (১৬.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ, "হ্যাঁ পাশ, বসিয়ে
       দিন") — বছর-বদল। `DraftRepository.load()` প্রতিটা বছরের হিসাব একসাথে
       বানিয়ে এই ফোনের মেমোরিতে (`YearlyRegistration.cacheAllYears`) রেখে
       দেয়; এখানে বছর বদলালে **নতুন কোনো ক্লাউড-কল ছাড়াই**, ওই মেমোরি
       থেকেই সাথে সাথে দেখানো হয় — নতুন কোনো Activity খোলে না, এই পর্দাতেই
       রিফ্রেশ হয়। ⛔ App প্রসেস মেরে দেওয়া থাকলে (মেমোরি খালি) শুধু চলতি
       বছরটাই বিকল্প থাকে — ভুল সংখ্যা দেখায় না, শুধু বিকল্প কম। */
    private var availableYears: List<String> = listOf()

    /* 🆕🔒 V852 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — ছাঁকনি · টিক-মার্ক ·
       "কতজন বাদ পড়ল"। ⛔ সবই এই পর্দার ভিতরে, কোনো নতুন ক্লাউড-পড়া নেই। */
    private var outDemo: Int = 0
    private var outNoDate: Int = 0
    private var filter: String = "all"   // all | counted | skipped | return | refund
    private val picked = HashSet<String>()

    /** ক্লাউড থেকে সদ্য-মিলানো (পূর্ণাঙ্গ) "বাদ" তালিকা — বছর বদলালেও এটাই
     *  আবার বসানো হয়, যাতে প্রতিটা বছরের skip-দাগ সমান নির্ভরযোগ্য থাকে। */
    private var fullExcludedIds: Set<String>? = null

    /** এই স্ক্রিনেই কেউ Skip/Undo করলে `fullExcludedIds`-ও সাথে সাথে হাল-নাগাদ —
     *  নইলে অন্য বছরে গিয়ে ফিরে এলে এই বদলটা পুরনো ক্যাশ দিয়ে হারিয়ে যেত। */
    private fun markExcludedLocally(id: String, skipped: Boolean) {
        val cur = fullExcludedIds ?: return
        fullExcludedIds = if (skipped) cur + id else cur - id
    }

    private lateinit var bodyCol: LinearLayout
    private lateinit var totalView: TextView

    /* 🔢🔒 V1001 (০৩.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK: *"নামের আগে
       সিরিয়াল নাম্বার দরকার — বছরের প্রথম দিন যে পেশেন্ট এসেছে তার সিরিয়াল
       নাম্বার ১ … এটা তারিখ এবং সময় অনুসারে নির্ধারণ হবে। যদি পুরনো তারিখে
       অন্য কেউকে আবার রেজিস্ট্রেশন করা হয়ে থাকে সেই ক্ষেত্রে সমস্ত সিরিয়াল
       নাম্বার পরিবর্তন হয়ে যাবে"*।
       · **সময়** আসে রোগীর নিজের কোড থেকে — `KNE-02092026-003`-এর শেষ
         সংখ্যাটাই ওই দিনের ক্রম (কে আগে, কে পরে)। আলাদা কোনো ঘর লাগেনি।
       · নম্বর **কোথাও জমা হয় না** — পর্দা খোলার সময় প্রতিবার নতুন করে গোনা
         হয়। তাই পুরনো তারিখে কেউ বসলে বাকি সবার নম্বর নিজে থেকেই সরে যায়।
       · TK: *"রিমুভ তো আমি তাদেরই করব যেগুলো ডেমো ছিল, সুতরাং ডেমো নাম্বারের
         কোন অধিকার থাকবে না সিরিয়াল পাওয়ার"* ⇒ Skip করা সারি নম্বর পায় না,
         আর ওরা কারো নম্বরও খায় না। ⇒ সবচেয়ে বড় নম্বরটাই উপরের মোট সংখ্যা।
       ⛔ একটাও নতুন ক্লাউড-পড়া নেই, ডেটাবেসে কিছু লেখা হয় না। */
    private val serialOf = HashMap<String, Int>()

    /** রোগীর কোডের শেষ অংশ = ওই দিনের ক্রম (না থাকলে ০)। */
    private fun daySeqOf(e: DraftEntry): Int =
        e.patientId.substringAfterLast('-', "").filter { it.isDigit() }.toIntOrNull() ?: 0

    /* 📜🔒 V1003 (০৩.০৯.২০২৬, TK-নির্দেশ) — TK: *"মোবাইল ডিসপ্লের প্রথমে
       থাকবে এক নাম্বার। স্ক্রোল করতে করতে পরবর্তী নাম্বারের দিকে যাব"*।
       ⇒ তালিকা এখন **পুরনো থেকে নতুন** (আগে ছিল নতুন সবার উপরে)।
       তাই একদম উপরে বছরের প্রথম রোগী = ১, নিচে নামলে নম্বর বাড়ে।
       ⛔ গোনা · মাসের টেবিল · Remove/Undo — কিছুই এতে বদলায় না। */
    /** তারিখ ⇢ ওই দিনের ক্রম ⇢ নাম — পুরনো সবার উপরে। */
    private fun sortForScreen() {
        rows.sortWith(
            compareBy<DraftEntry> { it.recordDate }
                .thenBy { daySeqOf(it) }
                .thenBy { it.name })
    }

    /** গোনা-হওয়া সারিগুলোকে উপর থেকে নিচে ১, ২, ৩ … বসায়। */
    private fun rebuildSerials() {
        serialOf.clear()
        var n = 0
        for (e in rows) {
            if (e.extra == YearlyRegistration.SKIP_MARK) continue
            n += 1
            serialOf[e.id] = n
        }
    }

    private fun d(): Float = resources.displayMetrics.density
    private fun px(v: Int): Int = (v * d()).toInt()

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        branch = intent.getStringExtra("branch").orEmpty()
        year = intent.getStringExtra("year").orEmpty().ifBlank { YearlyRegistration.currentYear() }
        rows = ((intent.getSerializableExtra("entries") as? ArrayList<DraftEntry>) ?: ArrayList()).toMutableList()
        outDemo = intent.getIntExtra("outDemo", 0)        // 🆕 V852
        outNoDate = intent.getIntExtra("outNoDate", 0)
        // 🆕🔒 V-YEARSW — বছর-বদলের তালিকা; খালি এলে অন্তত চলতি বছরটা থাকবে।
        // ⛔ `entries`-এর মতোই Serializable পথে (এই Activity-র প্রমাণিত ধরন)।
        availableYears = ((intent.getSerializableExtra("availableYears") as? ArrayList<String>)?.toList()
            ?: listOf()).ifEmpty { listOf(year) }
        sortForScreen()   // 🔢 V1001

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#F4F7FB"))
        }
        root.addView(header())

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
        }
        bodyCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(12), px(10), px(12), px(20))
        }
        scroll.addView(bodyCol)
        root.addView(scroll)
        setContentView(root)

        render()
        refreshExclusionsFromCloud()
    }

    private fun header(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#0B7A3E"))
            setPadding(px(12), px(12), px(12), px(12))
        }
        bar.addView(TextView(this).apply {
            text = "←"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { finish() }
            setPadding(0, 0, px(12), 0)
        })
        bar.addView(TextView(this).apply {
            text = "Yearly Registration"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        /* 📊🔒 V1208 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — TK: *"এটা আমি
           গুগল শিট ডাউনলোড করতে চাই, কেন অপশন নেই?"*
           ⇒ উপরে ডানে "⬇ Sheet" — চাপলে CSV বানিয়ে শেয়ার/সেভের পর্দা খোলে
             (Draft তালিকার **প্রমাণিত পথটাই**, নতুন কিছু বানানো হয়নি)।
           ⛔ পর্দার চালু ফিল্টারের সারিগুলোই যায় — পর্দা ও শিট কখনো আলাদা নয়। */
        bar.addView(TextView(this).apply {
            text = "⬇ Sheet"
            textSize = 12.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(px(12), px(5), px(12), px(5))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 10f * d()
                setColor(android.graphics.Color.parseColor("#26FFFFFF"))
                setStroke(px(1), android.graphics.Color.parseColor("#A6FFFFFF"))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { downloadSheet() }
        })
        return bar
    }

    /* 📊 V1208 — শিট বানিয়ে শেয়ার। ⛔ `DraftListActivity.downloadSheet()`-এর
       হুবহু একই পথ (FileProvider + ACTION_SEND), তাই নতুন কোনো ঝুঁকি নেই। */
    @Volatile private var sheetBusy = false

    private fun downloadSheet() {
        if (sheetBusy) return
        val shown = rows.filter { visibleIn(it) }
        if (shown.isEmpty()) {
            android.widget.Toast.makeText(this, "Nothing to download — the list is empty",
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        sheetBusy = true
        android.widget.Toast.makeText(this, "Preparing sheet…", android.widget.Toast.LENGTH_SHORT).show()
        val br = branch.ifBlank { "All" }
        val yr = year
        val act = this
        Thread {
            // 🔴🔒 V1620 (২০.০৯.২০২৬, TK-রিপোর্ট — Follow-up-এর একই ধরনের বার্তায়)
            // — আগে ব্যর্থ হলে আসল কারণ চুপচাপ গিলে ফেলা হতো। এখন Exception-এর
            // নিজের কথা দেখানো হয়, যাতে সত্যিই কী আটকাল বোঝা যায়।
            var failReason = ""
            val file = try { YearlyRegistrationSheetExporter.write(this, br, yr, shown) }
            catch (t: Throwable) {
                failReason = t.javaClass.simpleName + (t.message?.let { ": $it" } ?: "")
                null
            }
            runOnUiThread {
                sheetBusy = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (file == null || !file.exists()) {
                    val why = failReason.ifBlank { "file not created" }
                    android.widget.Toast.makeText(this, "Could not make the sheet — $why",
                        android.widget.Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                try {
                    /* 📊 V1208 — FileProvider-এর নাম `AndroidManifest.xml`-এ যা বসানো
                       আছে হুবহু তাই (`applicationId` + ".fileprovider")। */
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        act, com.tkbiswas.pilesclinic.BuildConfig.APPLICATION_ID + ".fileprovider", file)
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(android.content.Intent.createChooser(send, "Save / Send sheet"))
                } catch (_: Throwable) {
                    android.widget.Toast.makeText(this,
                        "Sheet made, but this phone could not open the share window",
                        android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun label(text: String, size: Float, color: String, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(android.graphics.Color.parseColor(color))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    /* 🆕🔒 V-YEARSW — "branch · year" লাইনটা এখন চাপ দেওয়া যায় (TK-পাশ-করা
       ফটো-প্রুফ অনুযায়ী)। একটার বেশি বছর না থাকলে (নতুন ব্রাঞ্চ/মাত্র চলতি
       বছরের তথ্য) "▾" দেখানো হয় না — চাপার কিছু নেই বলে বিভ্রান্তি এড়ানো। */
    private fun yearSwitcherLabel(): View {
        val txt = (branch.ifBlank { "All" }) + " · " + year + (if (availableYears.size > 1) " ▾" else "")
        return label(txt, 13f, "#5B6B81", bold = availableYears.size > 1).apply {
            if (availableYears.size > 1) {
                isClickable = true
                isFocusable = true
                setOnClickListener { showYearMenu(this) }
            }
        }
    }

    private fun showYearMenu(anchor: View) {
        if (availableYears.size <= 1) return
        try {
            val pm = android.widget.PopupMenu(this, anchor)
            availableYears.forEachIndexed { i, yr -> pm.menu.add(0, i, i, yr) }
            pm.setOnMenuItemClickListener { mi ->
                availableYears.getOrNull(mi.itemId)?.let { switchYear(it) }
                true
            }
            pm.show()
        } catch (_: Throwable) { }
    }

    /** নতুন বছরে বদলে **এই স্ক্রিনেই** রিফ্রেশ — নতুন কোনো ক্লাউড-কল/Activity
     *  খোলে না; `YearlyRegistration.cacheAllYears`-এ Draft পর্দা খোলার সময়ই
     *  জমা হওয়া হিসাব থেকে সরাসরি দেখানো হয় (নিয়ম ৭খ — দেরি নয়)। */
    private fun switchYear(newYear: String) {
        if (newYear == year) return
        val branchKey = branch.ifBlank { "All" }
        val snap = YearlyRegistration.cachedYears(branchKey)?.get(newYear)
        if (snap == null) {
            Toast.makeText(this,
                "Could not load $newYear from memory — please reopen this screen from Draft",
                Toast.LENGTH_LONG).show()
            return
        }
        year = newYear
        // পূর্ণাঙ্গ ক্লাউড-মিলানো "বাদ" তালিকা ইতিমধ্যে থাকলে (refreshExclusionsFromCloud
        // আগে সফল হয়ে থাকলে) নতুন বছরের সারিতেও সেটাই বসানো হয় — যাতে skip-দাগ
        // সব বছরে সমান নির্ভরযোগ্য থাকে (লোড-সময়ের ক্যাশে যা ছিল তার চেয়ে বেশি)।
        val excl = fullExcludedIds
        rows = if (excl == null) snap.rows.toMutableList() else snap.rows.map { e ->
            val want = if (excl.contains(e.id)) YearlyRegistration.SKIP_MARK else ""
            if (e.extra == want) e else e.copy(extra = want)
        }.toMutableList()
        outDemo = snap.outDemo
        outNoDate = snap.outNoDate
        picked.clear()
        filter = "all"
        sortForScreen()
        render()
    }

    private fun render() {
        bodyCol.removeAllViews()
        rebuildSerials()   // 🔢 V1001 — প্রতিবার নতুন করে গোনা হয়

        bodyCol.addView(yearSwitcherLabel())

        totalView = label(YearlyRegistration.countedOf(rows).toString(), 30f, "#0B5E2A", true)
        totalView.setPadding(0, px(2), 0, px(8))
        bodyCol.addView(totalView)

        // 🆕 V852 — TK: "কতজন বাদ পড়ল ও কেন" (তালিকাতেও নেই, গোনাতেও নেই)।
        excludedLine()?.let { bodyCol.addView(it) }

        bodyCol.addView(monthTable())

        bodyCol.addView(filterChips())   // 🆕 V852

        /* 🔤🔒 V852 — TK: *"Patients লিখেছেন কেন, এতে বিভ্রান্ত হয়ে যাচ্ছি —
           যত লোক চেম্বারে এসেছে তারা প্রত্যেকে ট্রিটমেন্ট শুরু করেনি"*।
           তাই লেখাটা "Registered"। */
        bodyCol.addView(label("Registered", 14f, "#0B5E2A", true).apply {
            setPadding(0, px(14), 0, px(6))
        })

        if (picked.isNotEmpty()) bodyCol.addView(selectionBar())   // 🆕 V852

        val shown = rows.filter { visibleIn(it) }
        if (shown.isEmpty()) {
            bodyCol.addView(label(
                if (rows.isEmpty()) "No registration in this year." else "Nothing in this filter.",
                13f, "#7A8794"))
            return
        }
        for (e in shown) bodyCol.addView(patientRow(e))
    }

    // ───────────────────── 🆕 V852 — নতুন অংশগুলো ─────────────────────

    private fun excludedLine(): View? {
        if (outDemo <= 0 && outNoDate <= 0) return null
        val parts = ArrayList<String>()
        if (outDemo > 0) parts.add("$outDemo demo/test name")
        if (outNoDate > 0) parts.add("$outNoDate no registration date")
        return label("Not in this list: " + parts.joinToString(" · "), 11.5f, "#7A8794").apply {
            setBackgroundColor(android.graphics.Color.parseColor("#EDF2F8"))
            setPadding(px(9), px(7), px(9), px(7))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = px(10)
            layoutParams = lp
        }
    }

    private fun visibleIn(e: DraftEntry): Boolean = when (filter) {
        "counted" -> e.extra != YearlyRegistration.SKIP_MARK
        "skipped" -> e.extra == YearlyRegistration.SKIP_MARK
        "return" -> e.regTag == YearlyRegistration.TAG_RETURN
        "refund" -> e.regTag == YearlyRegistration.TAG_REFUND
        else -> true
    }

    private fun filterChips(): View {
        val holder = android.widget.HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = px(14)
            layoutParams = lp
        }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val items = listOf(
            "all" to "All", "counted" to "Counted", "skipped" to "Removed",   // 🎨 V891 — শুধু লেখা (V884-এর একই নিয়ম)
            "return" to YearlyRegistration.TAG_RETURN, "refund" to YearlyRegistration.TAG_REFUND
        )
        for ((key, text) in items) {
            val on = filter == key
            bar.addView(TextView(this).apply {
                this.text = text
                textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(if (on) "#FFFFFF" else "#0B7A34"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 16f * d()
                    setColor(android.graphics.Color.parseColor(if (on) "#0B7A34" else "#EFF7F1"))
                    if (!on) setStroke(px(1), android.graphics.Color.parseColor("#CFE9D8"))
                }
                setPadding(px(12), px(6), px(12), px(6))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginEnd = px(5)
                layoutParams = lp
                setOnClickListener { filter = key; render() }
            })
        }
        holder.addView(bar)
        return holder
    }

    /** টিক-মার্ক করা থাকলে উপরে এই বার — একসাথে Skip / Restore। */
    private fun selectionBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 8f * d()
                setColor(android.graphics.Color.parseColor("#0B2B59"))
            }
            setPadding(px(10), px(8), px(10), px(8))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = px(8)
            layoutParams = lp
        }
        bar.addView(label("${picked.size} selected", 12f, "#FFFFFF").apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        fun act(text: String, fg: String, bg: String, wantSkip: Boolean): TextView =
            TextView(this).apply {
                this.text = text
                textSize = 11.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(fg))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 7f * d()
                    setColor(android.graphics.Color.parseColor(bg))
                }
                setPadding(px(12), px(6), px(12), px(6))
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.marginStart = px(8)
                layoutParams = lp
                setOnClickListener { confirmBulk(wantSkip) }
            }
        bar.addView(act("Remove", "#B3261E", "#FDECEA", true))   // 🎨 V884 — শুধু লেখা
        bar.addView(act("Restore", "#0B6E33", "#E9F6EE", false))
        return bar
    }

    /** মাসভিত্তিক হিসাব — দুই কলামে (জানু–জুন | জুলাই–ডিসে), শেষে Total। */
    private fun monthTable(): View {
        val names = listOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
        val counts = IntArray(12)
        for (e in rows) {
            if (e.extra == YearlyRegistration.SKIP_MARK) continue
            val d = e.recordDate
            if (d.length < 7) continue
            val m = d.substring(5, 7).toIntOrNull() ?: continue
            if (m in 1..12) counts[m - 1]++
        }

        val table = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(px(10), px(8), px(10), px(8))
        }
        fun column(from: Int, to: Int): LinearLayout {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            head.addView(label("Month", 11f, "#7A8794", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            head.addView(label("Count", 11f, "#7A8794", true))
            col.addView(head)
            var sum = 0
            for (i in from..to) {
                sum += counts[i]
                val r = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, px(4), 0, px(4))
                }
                r.addView(label(names[i], 12.5f, "#101828").apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                r.addView(label(counts[i].toString(), 12.5f, "#101828"))
                col.addView(r)
            }
            col.addView(View(this).apply {
                setBackgroundColor(android.graphics.Color.parseColor("#0B7A3E"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(1))
            })
            val tot = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, px(5), 0, 0)
            }
            tot.addView(label("Total", 12.5f, "#0B5E2A", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            tot.addView(label(sum.toString(), 12.5f, "#0B5E2A", true))
            col.addView(tot)
            return col
        }
        table.addView(column(0, 5))
        table.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(px(12), px(1))
        })
        table.addView(column(6, 11))
        return table
    }

    /* 🟢🔒 V878 — নামে চাপ দিলে রোগীর নিজস্ব পাতা।
       ⛔ `FollowUpActivity.openTimelineFor()`-এর হুবহু একই extras — তাই এক
          নম্বরে দুজন রোগী থাকলেও ঠিক এই রোগীরই পাতা খোলে (`prePatientId`)।
       ⛔ `section` পাঠানো হয় না ⇒ ড্যাশবোর্ডের View-এর মতো **সব** ইতিহাস
          দেখায়, কোনো ভাগ বাদ পড়ে না। */
    private fun openPatientPage(e: DraftEntry) {
        val digits = e.mobile.filter { it.isDigit() }.takeLast(10)
        if (digits.length != 10) {
            android.widget.Toast.makeText(this, "No mobile number saved for this patient",
                android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val i = android.content.Intent(this, PatientTimelineActivity::class.java)
        i.putExtra("mobile", digits)
        i.putExtra("preStage", e.stage)
        i.putExtra("preName", e.name)
        i.putExtra("preBranch", e.branch.ifBlank { branch })
        i.putExtra("preDisease", e.disease)
        i.putExtra("preAge", e.age)
        i.putExtra("preSex", e.sex)
        i.putExtra("preAddress", e.address)
        i.putExtra("prePatientId", e.patientId)
        try { startActivity(i) } catch (_: Throwable) { }
    }

    private fun patientRow(e: DraftEntry): View {
        val skipped = e.extra == YearlyRegistration.SKIP_MARK
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor(if (skipped) "#F7F9FC" else "#FFFFFF"))
            setPadding(px(10), px(9), px(10), px(9))
        }
        // 🆕 V852 — টিক-মার্ক (একসাথে অনেককে Skip / Restore করার জন্য)।
        row.addView(TextView(this).apply {
            val on = picked.contains(e.id)
            text = if (on) "✓" else ""
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 4f * d()
                setColor(android.graphics.Color.parseColor(if (on) "#0B7A34" else "#FFFFFF"))
                setStroke(px(2), android.graphics.Color.parseColor(if (on) "#0B7A34" else "#B7C3D1"))
            }
            // 📏 V1003 — TK: "টিক মারার বক্সের থেকে এতটা ব্যবধানে শুরু হয়েছে কেন
            //   সিরিয়াল নাম্বার" ⇒ ফাঁকটা ১০ → ৪dp (ওয়েবেও হুবহু একই)।
            layoutParams = LinearLayout.LayoutParams(px(19), px(19)).apply { marginEnd = px(4) }
            setOnClickListener {
                if (!picked.remove(e.id)) picked.add(e.id)
                render()
            }
        })

        /* 🔢 V1001 — নামের ঠিক আগে সিরিয়াল নাম্বার (TK-এর পাশ করা প্রুফ)।
           Skip করা সারিতে নম্বরের জায়গাটা ফাঁকা থাকে, কিন্তু চওড়া একই — তাই
           নামের কলাম সব সারিতে এক জায়গা থেকেই শুরু হয়। */
        row.addView(TextView(this).apply {
            val n = serialOf[e.id]
            text = if (n != null) n.toString() else ""
            textSize = 12.5f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextColor(android.graphics.Color.parseColor("#7A8794"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(px(22), LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = px(6) }
        })

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        /* 🆕 V852 — TK: *"নামের আশে পাশে রোগের নাম চাই"* + *"return visit /
           refund … ফার্স্ট ব্র্যাকেটের মধ্যে মেনশন থাকবে"*। নাম · রোগ · ট্যাগ
           একটাই লাইনে, রঙ আলাদা (Follow-up কার্ডের সবুজ রোগ-পিলের মতোই)। */
        val nameLine = android.text.SpannableStringBuilder(e.name.ifBlank { "UNKNOWN" })
        val nameEnd = nameLine.length
        if (e.disease.isNotBlank()) {
            val a = nameLine.length
            nameLine.append("  ").append(e.disease.uppercase())
            nameLine.setSpan(android.text.style.ForegroundColorSpan(
                android.graphics.Color.parseColor("#0C9E33")), a, nameLine.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            nameLine.setSpan(android.text.style.RelativeSizeSpan(0.82f), a, nameLine.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (e.regTag.isNotBlank()) {
            val a = nameLine.length
            nameLine.append("  (").append(e.regTag).append(")")
            nameLine.setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor(
                if (e.regTag == YearlyRegistration.TAG_REFUND) "#B3261E" else "#B54708")),
                a, nameLine.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            nameLine.setSpan(android.text.style.RelativeSizeSpan(0.82f), a, nameLine.length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        texts.addView(label("", 13.5f, if (skipped) "#9AA6B4" else "#101828", true).apply {
            text = nameLine
            if (skipped) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            if (skipped) setTextColor(android.graphics.Color.parseColor("#9AA6B4"))
            /* 🟢🔒 V878 (৩০.০৮.২০২৬, TK-নির্দেশ, জিজ্ঞাসা করে নিশ্চিত হয়ে):
               *"এখানে পেশেন্টের নামে চাপ দিলে রি-ডাইরেক্ট হতে হবে যে সেকশনে
               আছে"* ⇒ নামে চাপ দিলে **ওই রোগীর নিজস্ব পাতা** খোলে — কার্ডের
               👁 বোতাম যেখানে নিয়ে যায়, হুবহু সেই পর্দা ও সেই তথ্যগুলোই
               (`FollowUpActivity.openTimelineFor`-এর প্রমাণিত নিয়ম, নতুন কিছু
               বানানো হয়নি)।
               ⛔ বাঁয়ের টিক-বাক্স ও ডানের Skip/Undo বোতাম নিজেরা চাপ ধরে,
                  তাই ওগুলোর কাজ এক অক্ষরও বদলায়নি।
               ⛔ কোনো নতুন ক্লাউড-পড়া নেই — তথ্যগুলো এই সারিতেই ছিল। */
            isClickable = true
            isFocusable = true
            setOnClickListener { openPatientPage(e) }
        })
        texts.addView(label(
            listOf(e.patientId, DateUtil.display(e.recordDate)).filter { it.isNotBlank() }.joinToString(" · "),
            11.5f, "#9AA6B4"))
        row.addView(texts)

        val btn = TextView(this).apply {
            /* 🎨🔒 V884 (৩০.০৮.২০২৬, TK-নির্দেশ — *"নামটা Skip না করে
               Remove করলে কেমন হয়"*, জিজ্ঞাসা করে নিশ্চিত: **শুধু লেখাটা**):
               বোতামের শব্দ Skip → Remove। ⛔ কাজ এক অক্ষরও বদলায়নি — এখনো
               শুধু বছরের গোনা থেকে বাদ, রোগী/টাকা/Follow-up কিচ্ছু মোছে না,
               আর Undo-ও আগের মতোই আছে। */
            /* 📏🔒 V1003 (০৩.০৯.২০২৬, TK-নির্দেশ: "remove এত বড় করার দরকার
               নেই তো, কারণ কোন পেশেন্টের নাম যদি বড় হয় তাহলে তো নাম ব্রেক হয়ে
               যেতে পারে") — বোতামটা ছোট হলো, তাই নামের জন্য জায়গা বাড়ল
               (ওয়েবেও হুবহু একই)। ⛔ লেখা · রং · কাজ কিছুই বদলায়নি। */
            text = if (skipped) "Undo" else "Remove"
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(if (skipped) "#0B6E33" else "#B3261E"))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 8f * d()
                setColor(android.graphics.Color.parseColor(if (skipped) "#E9F6EE" else "#FDECEA"))
            }
            setPadding(px(9), px(5), px(9), px(5))
            // 🆕 V852 — TK: *"Skip করলে যেন বাধা দেয়, warning দেখাতে হবে
            //    are you sure — yes / no"*। ⛔ Undo-তে বাধা নেই (ওটা ফেরানো)।
            setOnClickListener {
                if (skipped) toggle(e, this)
                else AlertDialog.Builder(this@YearlyRegistrationActivity)
                    .setCustomTitle(PremiumAlert.header(this@YearlyRegistrationActivity, "Remove from count?"))
                    .setMessage("Are you sure? " + e.name.ifBlank { "This person" } +
                        " will not be counted in " + year + ".")
                    .setPositiveButton("Yes") { _, _ -> toggle(e, this) }
                    .setNegativeButton("No", null)
                    .show().also { PremiumAlert.paint(it) }
            }
        }
        row.addView(btn)

        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(View(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#ECF1F6"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(1))
        })
        return wrap
    }

    /** 🆕 V852 — টিক-মার্ক করা সবাইকে একসাথে Skip / Restore (আগে সতর্কবার্তা)। */
    private fun confirmBulk(wantSkip: Boolean) {
        val ids = picked.toList()
        if (ids.isEmpty()) return
        val what = if (wantSkip) "Remove" else "Restore"   // 🎨 V884 — শুধু লেখা
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "$what ${ids.size} selected?"))
            .setMessage(
                if (wantSkip) "Are you sure? They will not be counted in $year."
                else "Are you sure? They will be counted again in $year.")
            .setPositiveButton("Yes") { _, _ -> applyBulk(ids, wantSkip) }
            .setNegativeButton("No", null)
            .show().also { PremiumAlert.paint(it) }
    }

    /* ⛔ একটা একটা করে পাঠানো হয় ও প্রতিটার উত্তর দেখে তবেই দাগ বসে — নেট
       খারাপ হলে যেগুলো সত্যিই হয়নি সেগুলো আগের মতোই থাকে, আর কতগুলো ব্যর্থ
       হলো সেটা পর্দায় বলা হয় (চুপচাপ ভুল সংখ্যা দেখাবে না)। */
    private fun applyBulk(ids: List<String>, wantSkip: Boolean) {
        val me = try { NativeSession.current(this)?.name.orEmpty() } catch (_: Throwable) { "" }
        Thread {
            var failed = 0
            for (id in ids) {
                val e = rows.firstOrNull { it.id == id } ?: continue
                val already = (e.extra == YearlyRegistration.SKIP_MARK)
                if (already == wantSkip) continue
                val ok = try {
                    if (wantSkip) YearlyRegistration.exclude(this, e.id, e.patientId, e.name, me)
                    else YearlyRegistration.include(this, e.id)
                } catch (_: Throwable) { false }
                if (!ok) { failed++; continue }
                val idx = rows.indexOfFirst { it.id == e.id }
                if (idx >= 0) rows[idx] = rows[idx].copy(
                    extra = if (wantSkip) YearlyRegistration.SKIP_MARK else "")
                markExcludedLocally(e.id, wantSkip)
            }
            val bad = failed
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                picked.clear()
                if (bad > 0) Toast.makeText(
                    this, "$bad could not be saved — check connection and try again",
                    Toast.LENGTH_LONG).show()
                render()
            }
        }.start()
    }

    private fun toggle(e: DraftEntry, btn: TextView) {
        val wantSkip = e.extra != YearlyRegistration.SKIP_MARK
        btn.isEnabled = false
        val me = try { NativeSession.current(this)?.name.orEmpty() } catch (_: Throwable) { "" }
        Thread {
            val ok = try {
                if (wantSkip) YearlyRegistration.exclude(this, e.id, e.patientId, e.name, me)
                else YearlyRegistration.include(this, e.id)
            } catch (_: Throwable) { false }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                btn.isEnabled = true
                if (!ok) {
                    Toast.makeText(this, "Could not save — check connection and try again",
                        Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val idx = rows.indexOfFirst { it.id == e.id }
                if (idx >= 0) rows[idx] = rows[idx].copy(
                    extra = if (wantSkip) YearlyRegistration.SKIP_MARK else "")
                markExcludedLocally(e.id, wantSkip)
                // ⛔ Draft-এর সংখ্যাটা আলাদা করে ঠিক করতে হয় না — ফিরে গেলে
                //    Draft নিজেই আবার হিসাব করে, আর সেই হিসাব এই ফোনে সদ্য
                //    জমা হওয়া "বাদ" তালিকাটাই পড়ে (নতুন কোনো ক্লাউড-কল নয়)।
                render()
            }
        }.start()
    }

    /**
     * অন্য ফোনে করা "বাদ" এখানেও দেখা যাবে — ছোট্ট তালিকাটা (কয়েকটা সারি)
     * একবার মিলিয়ে নেওয়া হয়। ⛔ ব্যর্থ হলে কিছুই বদলায় না (আগের, জমানো
     * তালিকাই বহাল থাকে) — তাই নেট খারাপ থাকলে ভুল সংখ্যা দেখাবে না।
     */
    private fun refreshExclusionsFromCloud() {
        Thread {
            val fetched = try { YearlyRegistration.fetchExcluded(this) } catch (_: Throwable) { null }
            if (fetched == null) return@Thread
            val ids = HashSet<String>()
            for (o in fetched) {
                val id = o.s("patient_row_id")
                if (id.isNotBlank()) ids.add(id)
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                fullExcludedIds = ids   // 🆕 V-YEARSW — বছর বদলালেও এই পূর্ণাঙ্গ তালিকাই পুনরায় বসবে
                var changed = false
                for (i in rows.indices) {
                    val want = if (ids.contains(rows[i].id)) YearlyRegistration.SKIP_MARK else ""
                    if (rows[i].extra != want) { rows[i] = rows[i].copy(extra = want); changed = true }
                }
                if (changed) render()
            }
        }.start()
    }
}
