package com.tkbiswas.pilesclinic.native

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.modules.ModuleUi
import com.tkbiswas.pilesclinic.print.BranchCatalog
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 📒🔒 V1252 (০৯.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ — খাতার সারি ৩৭৩) —
 * **RMP COMMISSION SHEET — কত তারিখে কোন RMP-কে কোন রোগীর জন্য কত দেওয়া হলো।**
 *
 * TK-এর কথা (ধাপে ধাপে, প্রতিটাই প্রুফ দেখে পাশ করা):
 *  · *"কত তারিখে কোন RMP কে কত কমিশন দেওয়া হল সেটা আমি Google Sheet-এর মতো
 *    দেখতে চাই"*
 *  · *"কোন রোগীর জন্য দিলাম"* ⇒ PATIENT কলাম
 *  · *"উপরে ডান সাইডে ⋮-এর মধ্যে থাকবে share · print"*
 *  · *"ব্রাঞ্চ সিলেক্ট হেডারে থাকবে · RMP Commission Sheet লেখাটা ৫০% কম করুন"*
 *  · *"Total Paid নিচে থাকবে কলামের শেষে, উপরে থাকবে না · Cash & Online
 *    দেখতে হলে উপরের ডান দিকের ⋮-এর মধ্যে থাকবে"*
 *  · *"পেশেন্ট Id থাকবে না"*
 *
 * ⛔ **নতুন কোনো টেবিল · কলাম · SQL নেই** — সব সারি আগে থেকেই জমা ছিল
 *    (`RmpCommissionRepository.commissionSheet()`-এ পুরো হিসাব লেখা আছে)।
 * ⛔ **শুধু পড়া** — এই পর্দা থেকে একটাও সারি লেখা/বদলানো/মোছা যায় না।
 * ⛔ RMP বাছাই **পর্দাতেই** ছেঁকে দেখায় — এর জন্য আর একটাও ক্লাউড-ডাক যায় না
 *    (ফ্রি প্ল্যানে বাড়তি চাপ নেই)।
 * ⛔ পর্দায় কোনো বাংলা লেখা নেই (TK-এর স্থায়ী নিয়ম ৯)।
 * 🔒 শুধু Master — টাকার এই তালিকা B211-এর সুরেই মাস্টারের। TK চাইলে
 *    এক লাইনে স্টাফের জন্যও খোলা যাবে।
 */
class RmpCommissionSheetActivity : AppCompatActivity() {

    private var month: String = ""          // "2026-09"
    private var branch: String = ""         // "" = সব ব্রাঞ্চ
    private var rmpPick: String = ""        // "" = সব RMP
    private var all: List<RmpCommissionRepository.SheetRow> = emptyList()
    private var state: String = "loading"   // loading · ok · fail

    private lateinit var body: LinearLayout
    private var menuAnchor: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        month = ymNow()
        render()
        load()
    }

    private fun dp(v: Int) = ModuleUi.dp(this, v)

    private fun box(fill: String, stroke: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(fill))
        setStroke(dp(1), Color.parseColor(stroke))
        cornerRadius = dp(radius).toFloat()
    }

    // ───────────────────────── তারিখ ও টাকা ─────────────────────────
    private fun cal() = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))

    private fun ymNow(): String {
        val c = cal()
        return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
    }

    /** "2026-09" → মাসের প্রথম ও শেষ দিন। */
    private fun monthFrom(ym: String): String = "$ym-01"

    private fun monthTo(ym: String): String = try {
        val p = ym.split("-"); val y = p[0].toInt(); val m = p[1].toInt()
        val c = cal()
        c.set(y, m - 1, 1)
        String.format(Locale.US, "%04d-%02d-%02d", y, m, c.getActualMaximum(Calendar.DAY_OF_MONTH))
    } catch (_: Throwable) { "$ym-31" }

    private fun monthLabel(ym: String): String = try {
        val p = ym.split("-"); val m = p[1].toInt()
        arrayOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")[m - 1] + " " + p[0]
    } catch (_: Throwable) { ym }

    /** 🔴 V1158-এর লক করা ফরম্যাট — সব পর্দায় তারিখ dd/MM/yyyy। */
    private fun dmy(iso: String): String {
        val m = Regex("^(\\d{4})-(\\d{2})-(\\d{2})").find(iso.trim()) ?: return iso.trim()
        return m.groupValues[3] + "/" + m.groupValues[2] + "/" + m.groupValues[1]
    }

    private fun money(v: Double): String = String.format(Locale.US, "%,.2f", v)

    /** 📒 V1309 — recorded_at (timestamptz) → ভারতীয় সময় "5.42 PM"; না বোঝা গেলে ফাঁকা। */
    private fun timeIst(raw: String): String = try {
        val t = raw.trim()
        val m = Regex("^(\\d{4}-\\d{2}-\\d{2})[T ](\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?$").find(t)
        if (m == null) "" else {
            val f = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US)
            val off = m.groupValues[3].let { if (it.isBlank() || it == "Z") "+0000" else it.replace(":", "") }
            val d = f.parse(m.groupValues[1] + " " + m.groupValues[2] + off)
            val out = java.text.SimpleDateFormat("h.mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
            if (d == null) "" else out.format(d)
        }
    } catch (_: Throwable) { "" }
    /** 📒 V1309 — RMP-র নামে চাপ: ওই RMP-র পাতা (Dr. Visit / RMP, তাঁর নম্বর ধরে)। */
    private fun openRmp(rmpId: String, rmpName: String) {
        if (rmpId.isBlank()) return
        Thread {
            val rows = try { SupabaseClient.fetchListOrNull("doctor_visits", "id=eq." + java.net.URLEncoder.encode(rmpId, "UTF-8"), 1, select = "id,mobile,branch") } catch (_: Throwable) { null }
            val row = if (rows != null && rows.length() > 0) rows.optJSONObject(0) else null
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val mob = row?.optString("mobile", "").orEmpty()
                if (mob.isBlank()) { ModuleUi.toast(this, "Could not open " + rmpName + " now"); return@runOnUiThread }
                try {
                    startActivity(android.content.Intent(this, DoctorVisitActivity::class.java)
                        .putExtra("searchMobile", mob).putExtra("searchBranch", row?.optString("branch", "").orEmpty()))
                } catch (_: Throwable) { }
            }
        }.start()
    }
    /** 📒 V1309 — রোগীর নামে চাপ: তাঁর Patient Timeline। */
    private fun openPatient(rowId: String, mobile: String) {
        if (rowId.isBlank() && mobile.isBlank()) return
        try {
            startActivity(android.content.Intent(this, PatientTimelineActivity::class.java)
                .putExtra("mobile", mobile).putExtra("patientRowId", rowId))
        } catch (_: Throwable) { }
    }

    // ───────────────────────── ছেঁকে নেওয়া ─────────────────────────
    private fun shown(): List<RmpCommissionRepository.SheetRow> =
        if (rmpPick.isBlank()) all else all.filter { it.rmpName == rmpPick }

    // ───────────────────────── পর্দা ─────────────────────────
    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EEF2F6"))
        }

        // ── হেডার (TK: ব্রাঞ্চ সিলেক্ট হেডারেই, লেখাটা অর্ধেক মাপে) ──
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0B5E34"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        head.addView(TextView(this).apply {
            text = "◀"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 0, dp(8), 0)
            setOnClickListener { finish() }
        })
        /* 🔤 TK-নির্দেশ: *"RMP Commission sheet এই লেখাটা ৫০% কম করুন"*
           ⇒ ১৭sp ছিল, এখন ঠিক অর্ধেক ৮.৫sp। */
        head.addView(TextView(this).apply {
            text = "RMP COMMISSION SHEET"
            textSize = 8.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        head.addView(TextView(this).apply {
            text = (if (branch.isBlank()) "All Branches" else branch) + "  ▾"
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Color.parseColor("#8CC7A6"))
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(9), dp(5), dp(9), dp(5))
            setOnClickListener { pickBranch() }
        })
        val dots = TextView(this).apply {
            text = "⋮"
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(16), 0, dp(12), 0)   // 🔎 V1254
            setOnClickListener { openMenu() }
        }
        menuAnchor = dots
        head.addView(dots)
        root.addView(head)

        // ── মাস ও RMP বাছাই ──
        val chips = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(4))
        }
        chips.addView(chip(monthLabel(month), true) { pickMonth() })
        chips.addView(chip(if (rmpPick.isBlank()) "All RMP" else rmpPick, false) { pickRmp() })
        root.addView(chips)

        // ── শিট ──
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
        }
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            addView(body)
        }
        root.addView(scroll)

        root.addView(TextView(this).apply {
            text = "Slide sideways for Mode · Branch · Recorded by"   // 🔴🔒 V1314 (TK-নির্দেশ, ফটো-প্রুফে পাশ, ০৭.০৯.২০২৬-এর পরে): "রেফারেন্স ঘরটা আমার লাগবে না"
            textSize = 10f
            setTextColor(Color.parseColor("#8B98A9"))
            setPadding(dp(12), dp(4), dp(12), dp(10))
        })

        setContentView(root)
        paintBody()
    }

    private fun chip(label: String, on: Boolean, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 11.5f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (on) Color.WHITE else Color.parseColor("#41506A"))
            background = box(if (on) "#0F3D6B" else "#FFFFFF", if (on) "#0F3D6B" else "#E2E9F2", 10)
            setPadding(dp(4), dp(9), dp(4), dp(9))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { leftMargin = dp(3); rightMargin = dp(3) }
            setOnClickListener { onClick() }
        }

    // ───────────────────────── শিটের ঘর ─────────────────────────
    /* 📏 TK-এর পাশ-করা প্রুফের মাপ: পর্দায় DATE · RMP · PATIENT · AMOUNT
       ধরে যায়; বাকিগুলো পাশে টানলে আসে (Google Sheet-এর মতোই)। */
    private val wDate = 84      // 📒 V1309 — "07/09/2026" এক লাইনে, নিচে সময়
    private val wRmp = 104
    private val wPatient = 150
    private val wAmount = 74
    private val wMode = 56
    private val wBranch = 86
    private val wBy = 92   // 🔴🔒 V1314 — রেফারেন্স ঘর (wRef) TK-নির্দেশে বাদ

    /* 📒🔒 V1309 (তালিকা ৪১৮, TK: *"গুগল শিটের মতন বক্স থাকতে হবে"*) — প্রতিটা ঘরে বর্ডার,
       হেডার সবুজ; DATE-এর নিচে সময়; নামে চাপ দিলে পাতা খোলে। */
    private fun cellBg(fill: String, stroke: String = "#C9D3DF") = GradientDrawable().apply {
        setColor(Color.parseColor(fill)); setStroke(dp(1), Color.parseColor(stroke))
    }
    private fun cell(text: String, widthDp: Int, bold: Boolean, color: String,
                     right: Boolean = false, size: Float = 10.5f, fill: String = "#FFFFFF"): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTypeface(typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(Color.parseColor(color))
            gravity = if (right) (Gravity.END or Gravity.CENTER_VERTICAL) else Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(7), dp(6), dp(7))
            background = cellBg(fill)
            layoutParams = LinearLayout.LayoutParams(dp(widthDp), LinearLayout.LayoutParams.MATCH_PARENT)
        }
    /** একটা ঘরে অনেক লাইন (নাম-লিঙ্ক · ছোট লেখা) — বর্ডারসহ। */
    private fun cellBox(widthDp: Int, fill: String = "#FFFFFF"): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(6), dp(5), dp(6), dp(5))
        background = cellBg(fill)
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), LinearLayout.LayoutParams.MATCH_PARENT)
    }
    private fun link(text: String, size: Float = 10.5f, onTap: () -> Unit): TextView = TextView(this).apply {
        this.text = text; textSize = size
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor("#0B57D0"))
        paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
        setSingleLine(true); ellipsize = android.text.TextUtils.TruncateAt.END
        setOnClickListener { onTap() }
    }
    private fun small(text: String, color: String = "#5B6B81"): TextView = TextView(this).apply {
        this.text = text; textSize = 8.5f; setTextColor(Color.parseColor(color))
        setSingleLine(true); ellipsize = android.text.TextUtils.TruncateAt.END
    }

    private fun rowBox(fill: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(Color.parseColor(fill))
    }

    private fun paintBody() {
        body.removeAllViews()
        if (state == "loading") {
            body.addView(ModuleUi.body(this, "Loading...")); return
        }
        if (state == "fail") {
            body.addView(ModuleUi.body(this,
                "⚠️ Could not load now — weak internet. Nothing is lost; open again when online."))
            return
        }
        val list = shown()
        if (list.isEmpty()) {
            body.addView(ModuleUi.body(this, "No commission was paid in " + monthLabel(month) + "."))
            return
        }

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#FFFFFF", "#D8E0EA", 8)
        }

        // শিরোনামের সারি
        val hr = rowBox("#0B5E34")
        val hf = "#0B5E34"
        hr.addView(cell("DATE / TIME", wDate, true, "#FFFFFF", size = 9.5f, fill = hf))
        hr.addView(cell("RMP", wRmp, true, "#FFFFFF", size = 9.5f, fill = hf))
        hr.addView(cell("PATIENT (for whom)", wPatient, true, "#FFFFFF", size = 9.5f, fill = hf))
        hr.addView(cell("AMOUNT", wAmount, true, "#FFFFFF", right = true, size = 9.5f, fill = hf))
        hr.addView(cell("MODE", wMode, true, "#FFFFFF", size = 9.5f, fill = hf))
        hr.addView(cell("BRANCH", wBranch, true, "#FFFFFF", size = 9.5f, fill = hf))
        hr.addView(cell("RECORDED BY", wBy, true, "#FFFFFF", size = 9.5f, fill = hf))
        grid.addView(hr)

        var total = 0.0
        for ((i, r) in list.withIndex()) {
            total += r.amount
            val bg = if (i % 2 == 0) "#FFFFFF" else "#FAFCFE"
            val row = rowBox(bg)
            // DATE / TIME — তারিখের নিচে এন্ট্রির সময় (ভারতীয়)
            row.addView(cellBox(wDate, bg).apply {
                addView(TextView(this@RmpCommissionSheetActivity).apply { text = dmy(r.paidOn); textSize = 10.5f; setTextColor(Color.parseColor("#1B2733")) })
                val t = timeIst(r.recordedAt); if (t.isNotBlank()) addView(small(t))
            })
            row.addView(cellBox(wRmp, bg).apply { addView(link(r.rmpName) { openRmp(r.rmpId, r.rmpName) }) })
            /* 📒 V1309 — PATIENT: রোগীর নামে দেওয়া হলে সেই নাম; RMP-কে দেওয়া হলে কোন কোন রোগীর জন্য
               (হাতে adjust / পুরনো বকেয়া আগে) — নাম + টাকা; কারো বকেয়া না থাকলে "No patient due" */
            row.addView(cellBox(wPatient, bg).apply {
                if (!r.isAdvance) addView(link(r.patientName.ifBlank { "—" }) { openPatient(r.patientRowId, r.patientMobile) })
                else {
                    var shownSum = 0.0
                    for ((k, c) in r.covers.withIndex()) {
                        shownSum += c.amount
                        addView(LinearLayout(this@RmpCommissionSheetActivity).apply {
                            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                            addView(link(c.name.ifBlank { "—" }, if (k == 0) 10.5f else 9f) { openPatient(c.rowId, c.mobile) })
                            addView(small("  " + money(c.amount) + (if (c.kind == "allocated") " · adjusted" else "")))
                        })
                    }
                    if (r.legacyCovered > 0.0) addView(small("Earlier patients (before app) " + money(r.legacyCovered), "#8A5A00"))
                    val rest = r.amount - shownSum - r.legacyCovered
                    if (r.covers.isEmpty() && r.legacyCovered <= 0.0) addView(small("No patient due — RMP payment", "#B45309"))
                    else if (rest > 0.5) addView(small("Not yet against any patient " + money(rest), "#B45309"))
                }
            })
            row.addView(cell(money(r.amount), wAmount, true, "#0B4F2A", right = true, fill = bg))
            row.addView(cell(modeText(r.mode), wMode, false, "#1B2733", fill = bg))
            row.addView(cell(r.branch, wBranch, false, "#1B2733", fill = bg))
            row.addView(cell(r.recordedBy, wBy, false, "#5B6B81", fill = bg))
            grid.addView(row)
        }

        /* 💰 TK-নির্দেশ: *"Total Paid নিচে থাকবে কলামের শেষে, উপরে থাকবে না"* */
        val tr = rowBox("#0B5E34")
        tr.addView(cell("TOTAL  ·  " + list.size + " payments", wDate + wRmp + wPatient, true, "#FFFFFF", size = 11f, fill = "#0B5E34"))
        tr.addView(cell(money(total), wAmount, true, "#FFFFFF", right = true, size = 11f, fill = "#0B5E34"))
        tr.addView(cell("", wMode + wBranch + wBy, false, "#FFFFFF", fill = "#0B5E34"))
        grid.addView(tr)

        body.addView(HorizontalScrollView(this).apply { addView(grid) })
    }

    private fun line() = TextView(this).apply {
        setBackgroundColor(Color.parseColor("#EDF1F6"))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
    }

    private fun modeText(m: String): String =
        if (m.equals("ONLINE", true)) "Online" else if (m.equals("CASH", true)) "Cash" else m

    // ───────────────────────── ⋮ ─────────────────────────
    private fun openMenu() {
        try {
            val pm = android.widget.PopupMenu(this, menuAnchor ?: body)
            pm.menu.add(0, 0, 0, "Year: " + month.substringBefore("-"))
            pm.menu.add(0, 1, 1, "Cash & Online")
            pm.menu.add(0, 2, 2, "Share")
            pm.menu.add(0, 3, 3, "Print / PDF")
            pm.setOnMenuItemClickListener { mi ->
                when (mi.itemId) {
                    0 -> pickYear()
                    1 -> showCashOnline()
                    2 -> sharePaper()
                    3 -> printPaper()
                }
                true
            }
            pm.show()
        } catch (_: Throwable) { }
    }

    // ───────────────────────── বাছাই ─────────────────────────
    private fun pickBranch() {
        val names = listOf("All Branches") + BranchCatalog.all.map { it.displayName }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Branch"))
            .setItems(names.toTypedArray()) { _, which ->
                branch = if (which == 0) "" else names[which]
                rmpPick = ""
                state = "loading"
                render()
                load()
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun pickYear() {
        val now = cal()
        val nowYear = now.get(Calendar.YEAR)
        val minYear = BranchCatalog.minYearFor(branch)
        val years = (nowYear downTo maxOf(minYear, nowYear - 5)).toList()
        val currentYear = month.substringBefore("-").toIntOrNull() ?: nowYear
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Year"))
            .setSingleChoiceItems(years.map { it.toString() }.toTypedArray(), years.indexOf(currentYear).coerceAtLeast(0)) { dialog, which ->
                val y = years[which]
                val currentMonth = month.substringAfter("-", "01").toIntOrNull()?.coerceIn(1, 12) ?: 1
                val safeMonth = if (y == nowYear) minOf(currentMonth, now.get(Calendar.MONTH) + 1) else currentMonth
                month = String.format(Locale.US, "%04d-%02d", y, safeMonth)
                rmpPick = ""
                state = "loading"
                dialog.dismiss()
                render()
                load()
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun pickMonth() {
        val months = ArrayList<String>()
        val now = cal()
        val nowYear = now.get(Calendar.YEAR)
        val y = month.substringBefore("-").toIntOrNull() ?: nowYear
        val maxMonth = if (y == nowYear) now.get(Calendar.MONTH) + 1 else 12
        for (m in maxMonth downTo 1) {
            months.add(String.format(Locale.US, "%04d-%02d", y, m))
        }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Month"))
            .setItems(months.map { monthLabel(it) }.toTypedArray()) { _, which ->
                month = months[which]
                rmpPick = ""
                state = "loading"
                render()
                load()
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    /** ⛔ RMP-র তালিকা **এই মাসের পড়া সারি থেকেই** বানানো — বাড়তি ক্লাউড-ডাক নেই। */
    private fun pickRmp() {
        val names = listOf("All RMP") + all.map { it.rmpName }.filter { it.isNotBlank() }.distinct().sorted()
        if (names.size == 1) { ModuleUi.toast(this, "No RMP in this month"); return }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "RMP"))
            .setItems(names.toTypedArray()) { _, which ->
                rmpPick = if (which == 0) "" else names[which]
                render()
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    /* 💵 TK-নির্দেশ: *"Cash & Online দেখতে হলে উপরের ডান দিকের ⋮-এর মধ্যে
       থাকবে, সেখানে গিয়ে দেখবে"*। ⛔ হিসাবটা পর্দার ওই সারিগুলোরই যোগফল —
       আলাদা কোনো নিয়ম বা ক্লাউড-ডাক নেই, তাই দুই জায়গায় সংখ্যা আলাদা হতে পারে না। */
    private fun showCashOnline() {
        val list = shown()
        val cash = list.filter { it.mode.equals("CASH", true) }.sumOf { it.amount }
        val online = list.filter { it.mode.equals("ONLINE", true) }.sumOf { it.amount }
        AlertDialog.Builder(this)
            .setCustomTitle(PremiumAlert.header(this, "Cash & Online"))
            .setMessage(
                monthLabel(month) + "\n\n" +
                    "Cash  " + money(cash) + "\n" +
                    "Online " + money(online) + "\n\n" +
                    "Total  " + money(cash + online))
            .setPositiveButton("OK", null)
            .show().also { try { PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    // ───────────────────────── কাগজ ─────────────────────────
    /** ⛔ ছাপা ও শেয়ার — একই HTML, তাই দুটো কাগজ কখনো আলাদা হতে পারে না। */
    private fun paperHtml(): String {
        val list = shown()
        val esc = { s: String -> s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") }
        var total = 0.0
        val rows = StringBuilder()
        for (r in list) {
            total += r.amount
            val tm = timeIst(r.recordedAt)
            val pat = StringBuilder()
            if (!r.isAdvance) pat.append("<b>").append(esc(r.patientName)).append("</b>")
            else {
                var shownSum = 0.0
                for (c in r.covers) { shownSum += c.amount
                    pat.append("<div><b>").append(esc(c.name)).append("</b> ").append(money(c.amount))
                        .append(if (c.kind == "allocated") " · adjusted" else "").append("</div>") }
                if (r.legacyCovered > 0.0) pat.append("<div class=\"m\">Earlier patients (before app) ").append(money(r.legacyCovered)).append("</div>")
                val rest = r.amount - shownSum - r.legacyCovered
                if (r.covers.isEmpty() && r.legacyCovered <= 0.0) pat.append("<span class=\"m\">No patient due — RMP payment</span>")
                else if (rest > 0.5) pat.append("<div class=\"m\">Not yet against any patient ").append(money(rest)).append("</div>")
            }
            rows.append("<tr><td>").append(esc(dmy(r.paidOn))).append(if (tm.isBlank()) "" else "<div class=\"m\">" + tm + "</div>")
                .append("</td><td>").append(esc(r.rmpName))
                .append("</td><td>").append(pat)
                .append("</td><td>").append(esc(modeText(r.mode)))
                .append("</td><td>").append(esc(r.branch))
                .append("</td><td>").append(esc(r.recordedBy))
                .append("</td><td class=\"r\">").append(money(r.amount))
                .append("</td></tr>")
        }
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=794\"><style>" +
            "@page{size:A4;margin:0}*{box-sizing:border-box;margin:0;padding:0;-webkit-print-color-adjust:exact;print-color-adjust:exact}" +
            "body{font-family:Arial,sans-serif;color:#111;background:#fff}" +
            ".sheet{width:210mm;min-height:297mm;padding:10mm 9mm}" +
            "h1{font-size:15px;color:#0B5E34;letter-spacing:.5px}" +
            ".sub{font-size:11px;color:#5B6B81;margin-top:3px}" +
            "table{border-collapse:collapse;width:100%;margin-top:7mm;font-size:10.5px}" +
            "th{background:#F2F6FA;color:#3C4A5C;text-align:left;padding:5px 6px;border:1px solid #D8E0EA;font-size:9.5px}" +
            "td{padding:5px 6px;border:1px solid #E6ECF3}" +
            ".r{text-align:right;font-weight:bold;color:#0B4F2A}" +
            ".m{font-size:9px;color:#5B6B81}" +
            "tr:nth-child(even) td{background:#FAFCFE}" +
            ".tot td{background:#0B5E34;color:#fff;font-weight:bold;border-color:#0B5E34}" +
            ".tot td.r{color:#fff}" +
            "</style></head><body><div class=\"sheet\">" +
            "<h1>RMP COMMISSION SHEET</h1><div class=\"sub\">" +
            monthLabel(month) + "  ·  " + (if (branch.isBlank()) "All Branches" else branch) +
            (if (rmpPick.isBlank()) "" else "  ·  " + rmpPick) + "</div>" +
            "<table><tr><th>DATE / TIME</th><th>RMP</th><th>PATIENT (for whom)</th><th>MODE</th><th>BRANCH</th>" +
            "<th>RECORDED BY</th><th>AMOUNT</th></tr>" + rows +
            "<tr class=\"tot\"><td colspan=\"6\">TOTAL  ·  " + list.size + " payments</td>" +
            "<td class=\"r\">" + money(total) + "</td></tr></table>" +
            "</div></body></html>"
    }

    private var printWeb: android.webkit.WebView? = null

    private fun printPaper() {
        if (shown().isEmpty()) { ModuleUi.toast(this, "Nothing to print"); return }
        try {
            val wv = android.webkit.WebView(this)
            wv.settings.javaScriptEnabled = false
            wv.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String) {
                    try {
                        val pm = getSystemService(android.content.Context.PRINT_SERVICE)
                            as android.print.PrintManager
                        pm.print("RMP Commission Sheet",
                            view.createPrintDocumentAdapter("RMP Commission Sheet"),
                            PrintQuality.builder().build())
                    } catch (_: Throwable) {
                        ModuleUi.toast(this@RmpCommissionSheetActivity, "Print not available")
                    }
                }
            }
            printWeb = wv
            wv.loadDataWithBaseURL("file:///android_asset/", paperHtml(), "text/html", "UTF-8", null)
        } catch (_: Throwable) { ModuleUi.toast(this, "Print not available") }
    }

    private fun sharePaper() {
        if (shown().isEmpty()) { ModuleUi.toast(this, "Nothing to share"); return }
        try {
            com.tkbiswas.pilesclinic.print.PrescriptionWhatsAppShare.shareHtml(
                activity = this,
                html = paperHtml(),
                documentTitle = "RMP Commission Sheet",
                patientName = monthLabel(month),
                allowPrint = true
            )
        } catch (_: Throwable) { ModuleUi.toast(this, "Could not share now") }
    }

    // ───────────────────────── পড়া ─────────────────────────
    private fun load() {
        val ym = month
        val br = branch
        Thread {
            val res = try {
                RmpCommissionRepository.commissionSheet(monthFrom(ym), monthTo(ym), br)
            } catch (_: Throwable) { null }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                // ⛔ এর মধ্যে TK মাস/ব্রাঞ্চ বদলে ফেললে পুরনো উত্তর বসতে দেওয়া হয় না।
                if (ym != month || br != branch) return@runOnUiThread
                if (res == null || !res.ok) { state = "fail"; paintBody(); return@runOnUiThread }
                all = res.value ?: emptyList()
                state = "ok"
                paintBody()
            }
        }.start()
    }
}
