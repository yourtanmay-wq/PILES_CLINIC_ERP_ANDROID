package com.tkbiswas.pilesclinic.modules

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.NoBengali
import com.tkbiswas.pilesclinic.native.PremiumAlert
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🔵🔒 V306 · অংশীদারি ভাগ (Partner Shares) — PHASE 1 · ফোনের মাস্টার-পাশ।
 * ওয়েবের partners.js-এর হুবহু আচরণ, Kotlin-এ। টাকার হিসাব (IncomeExpenseActivity)-এর
 * ভেতর থেকে খোলে (ওখানে শুধু ১টা বোতাম যোগ হয়েছে — পুরনো কোড ছোঁয়া হয়নি)।
 *
 * ⛔ সম্পূর্ণ আলাদা Activity — বিদ্যমান কোনো হিসাব/স্ক্রিন এতে বদলায় না।
 * ⛔ শুধু মাস্টার। fin schema-র V306 টেবিল (partners / partner_drawings) পড়ে/লেখে।
 * নেট লাভ = fin.collections-এর (cash+online−খরচ), Jan→আজ — finance.js-এর হুবহু নিয়ম।
 */
class PartnerSharesActivity : Activity() {

    private val BRANCHES = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
    // V1651: Income-Expense-এর locked rule — 01/09/2026 থেকে হাতে লেখা collections
    // সারি না থাকলে Visit Fee/Treatment/Medicine payment থেকে AUTO income।
    private val AUTO_INCOME_FROM = "2026-09-01"

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun today(): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }
    private fun yearStart(): String = today().substring(0, 4) + "-01-01"
    /* 🔴🔒 V819 (২৯.০৮.২০২৬, TK-নির্দেশে ঝুঁকি-যাচাই) — **"null" ছাপার পুরনো ফাঁদ।**
       `fin.partners.name` ঘরটা ডেটাবেসে ফাঁকা (SQL NULL) থাকতে পারে, আর
       org.json-এর `optString()` তখন **আক্ষরিক "null" শব্দটাই** ফেরায়।
       ফলে `ifBlank { }` ছাঁকনিটা ওটাকে ফাঁকা ধরতে পারত না — পর্দায়, কাগজে
       ও WhatsApp-বার্তায় পার্টনারের নামের জায়গায় **"null"** লেখা উঠত।
       ⛔ এই একই দোষ আগে দুবার ধরা পড়েছে (V696 — Treatment Progress-এ,
          V812 — কল-নোটিফিকেশনে)। তাই এখানেও একই সাফাই বসানো হলো, আর
          নিয়ম ৬.২ মেনে **এই পর্দার প্রতিটা নামের জায়গাতেই**।
       ⛔ কোনো হিসাব · শতাংশ · টাকার অঙ্ক ছোঁয়া হয়নি — শুধু দেখানোর নাম। */
    private fun ps(o: org.json.JSONObject?, key: String): String {
        val t = (o?.optString(key) ?: "").trim()
        return if (t.equals("null", ignoreCase = true) || t.equals("undefined", ignoreCase = true)) "" else t
    }

    private fun n10(s: String?): String = (s ?: "").filter { it.isDigit() }.takeLast(10)
    private fun money(n: Double): String {
        val nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale("en", "IN"))
        val neg = n < 0
        return (if (neg) "−₹" else "₹") + nf.format(Math.round(Math.abs(n)))
    }
    private fun myMobile(): String = n10(NativeSession.current(this)?.mobile)
    // HTML-escape (Print/Export-এ ব্যবহার) — RegistrationHtml.esc-এর হুবহু।
    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    // URL-এনকোড — স্পেস যেন `+` না হয়ে `%20` হয় (নাহলে "Cooch Behar"-এর মতো ব্রাঞ্চ ম্যাচ করে না)।
    private fun urlv(s: String): String = java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 🔵 V307: মডিউলে সাইন-ইন নিশ্চিত করি (যেকোনো role-এ কাজ করে)। মাস্টার হলে ব্রাঞ্চ-ওভারভিউ;
        // নইলে (ডাক্তার-অংশীদার সহ) নিজের খাতা read-only। ⛔ মাস্টারের আচরণ আগের মতোই।
        val loading = TextView(this).apply { text = "Opening…"; setPadding(dp(20), dp(40), dp(20), dp(20)); textSize = 16f }
        setContentView(loading)
        ModuleUi.ensureSignedIn(this, NativeSession.current(this)?.name ?: "") {
            if (ModuleAuth.isMaster) renderBranchList() else renderMyLedger()
        }
    }

    // ---------- PARTNER MODE (non-master) — own ledger, read-only, no % ----------
    private fun renderMyLedger() {
        val (_, col) = screen("📗 My Share Ledger")
        val loading = TextView(this).apply { text = "Loading…"; setTextColor(android.graphics.Color.parseColor("#7c8a83")) }
        col.addView(loading)
        Thread {
            val res = ModuleAuth.getRowsChecked("fin", "partners", "select=*")   // RLS → only my own rows
            val mine = ArrayList<JSONObject>()
            if (res.ok) for (i in 0 until res.rows.length()) { val p = res.rows.getJSONObject(i); if (p.optBoolean("active", true)) mine.add(p) }
            val data = ArrayList<Triple<String, BData?, PLine?>>()
            for (p in mine) {
                val br = p.optString("branch")
                val d = try { computeBranch(br) } catch (e: Throwable) { null }
                val line = d?.list?.firstOrNull { n10(it.p.optString("mobile")) == n10(p.optString("mobile")) }
                data.add(Triple(br, d, line))
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                col.removeView(loading)
                if (mine.isEmpty()) { col.addView(TextView(this).apply { text = "No partner record found for your login yet. Please contact the master."; setTextColor(android.graphics.Color.parseColor("#7c8a83")); setPadding(0, dp(6), 0, dp(6)) }); return@runOnUiThread }
                for (triple in data) {
                    val br = triple.first; val dat = triple.second; val line = triple.third
                    val c = card()
                    if (line == null) { kvRow(c, br, "could not load", "#B42318", true); col.addView(c); continue }
                    kvRow(c, "$br · Jan → Today", "", "#0A5C33", true)
                    if (dat != null) {
                        kvRow(c, "Total Income", money(dat.income), "#111111", false)
                        kvRow(c, "Total Expense", money(dat.expense), "#B42318", false)
                        kvRow(c, "Net Profit", money(dat.net), if (dat.net < 0) "#B42318" else "#0A7C3F", true)
                    }
                    col.addView(c)
                    val c2 = card()
                    kvRow(c2, "My Share Account", "", "#0A5C33", true)
                    kvRow(c2, "Total Due", money(line.due), "#111111", true)
                    kvRow(c2, "Total Withdrawn", money(-line.drawn), "#B42318", false)
                    val red = line.bal < 0
                    kvRow(c2, "Current Balance", (if (red) "🔴 " else "🟢 ") + money(line.bal), if (red) "#B42318" else "#0A7C3F", true)
                    col.addView(c2)
                    col.addView(TextView(this).apply {
                        text = if (red) "🔴 You have taken " + money(-line.bal) + " more than your share. You may return it, or it adjusts from your next share."
                               else "🟢 Your " + money(line.bal) + " share is still held in the business — you may withdraw it."
                        textSize = 11.5f
                        setTextColor(android.graphics.Color.parseColor(if (red) "#8f2a20" else "#0A6b38"))
                        setPadding(dp(11), dp(9), dp(11), dp(9))
                        background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.parseColor(if (red) "#FBEAE8" else "#E7F6EC")) }
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }
                    })
                }
                col.addView(greenButton("← Back", "#5b6b62") { finish() }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(6) } })
            }
        }.start()
    }

    // ---------- shared chrome ----------
    private fun screen(title: String, onMenu: ((android.view.View) -> Unit)? = null): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#F4FBF6")); isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(16))
        }
        scroll.addView(col)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10) }
        }
        header.addView(TextView(this).apply {
            text = title; textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        if (onMenu != null) {
            header.addView(TextView(this).apply {
                text = "⋮"; textSize = 28f; gravity = android.view.Gravity.CENTER
                setTextColor(android.graphics.Color.parseColor("#0A5C33"))
                setPadding(dp(12), 0, dp(4), dp(2))
                isClickable = true; isFocusable = true
                setOnClickListener { onMenu(this) }
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            })
        }
        col.addView(header)
        setContentView(scroll)
        return Pair(scroll, col)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(13), dp(12), dp(13), dp(12))
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(14).toFloat(); setColor(android.graphics.Color.WHITE)
            setStroke(dp(1), android.graphics.Color.parseColor("#D9E6DD"))
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
    }

    private fun kvRow(parent: LinearLayout, label: String, value: String, valueColor: String, bold: Boolean) {
        val r = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(5), 0, dp(5))
        }
        r.addView(TextView(this).apply {
            text = label; textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#33463d"))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        r.addView(TextView(this).apply {
            text = value; textSize = if (bold) 16f else 14f; gravity = android.view.Gravity.END
            setTextColor(android.graphics.Color.parseColor(valueColor))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        parent.addView(r)
    }

    private fun greenButton(label: String, bg: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label; isAllCaps = false; textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat(); setColor(android.graphics.Color.parseColor(bg))
            }
            setOnClickListener { onClick() }
        }

    // ---------- BRANCH LIST ----------
    private fun renderBranchList() {
        val (_, col) = screen("🤝 Partner Shares")
        val info = TextView(this).apply {
            text = "Loading…"; setTextColor(android.graphics.Color.parseColor("#7c8a83"))
        }
        col.addView(info)
        col.addView(greenButton("← Back", "#5b6b62") { finish() }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(14) }
        })
        Thread {
            val res = ModuleAuth.getRowsChecked("fin", "partners", "select=branch,mobile,name,pct,active")
            val byB = HashMap<String, MutableList<JSONObject>>()
            if (res.ok) {
                for (i in 0 until res.rows.length()) {
                    val r = res.rows.getJSONObject(i)
                    if (r.optBoolean("active", true)) byB.getOrPut(r.optString("branch")) { mutableListOf() }.add(r)
                }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                col.removeView(info)
                // rebuild: insert branch rows before the Back button (last child)
                val backBtn = col.getChildAt(col.childCount - 1)
                col.removeView(backBtn)
                for (b in BRANCHES) {
                    val list = byB[b] ?: mutableListOf()
                    val sub = if (list.isEmpty()) "not set up"
                    else list.joinToString(" · ") { (ps(it, "name").ifBlank { n10(ps(it, "mobile")) }) + " " + fmtPct(it.optDouble("pct", 0.0)) + "%" }
                    val rowCard = card().apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        isClickable = true
                        setOnClickListener { openBranch(b) }
                    }
                    val left = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    left.addView(TextView(this).apply {
                        text = b; textSize = 15f
                        setTextColor(android.graphics.Color.parseColor("#0A5C33"))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                    left.addView(TextView(this).apply {
                        text = list.size.toString() + " partner" + (if (list.size == 1) "" else "s") + " · " + sub
                        textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#7c8a83"))
                    })
                    rowCard.addView(left)
                    rowCard.addView(TextView(this).apply {
                        text = "›"; textSize = 20f
                        setTextColor(android.graphics.Color.parseColor("#9fb0a5"))
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                    col.addView(rowCard)
                }
                col.addView(backBtn)
            }
        }.start()
    }

    private fun fmtPct(p: Double): String = if (p == Math.floor(p)) p.toInt().toString() else p.toString()

    // ---------- COMPUTE ----------
    private data class PLine(val p: JSONObject, val due: Double, val drawn: Double, val bal: Double)
    private data class BData(val income: Double, val expense: Double, val net: Double, val list: List<PLine>)

    private fun sumNumbersInText(text: String): Double {
        if (text.isBlank()) return 0.0
        var sum = 0.0
        val m = Regex("\\d+(\\.\\d+)?").findAll(text)
        for (x in m) sum += x.value.toDoubleOrNull() ?: 0.0
        return sum
    }
    private fun rowExpense(r: JSONObject): Double {
        val et = r.optDouble("expense_total", -1.0)
        return if (et >= 0.0) et else sumNumbersInText(r.optString("expense_notes", "").let { if (it == "null") "" else it })
    }
    private fun dayAfter(s: String): String {
        return try {
            val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val c = java.util.Calendar.getInstance(); c.time = f.parse(s)!!; c.add(java.util.Calendar.DAY_OF_MONTH, 1); f.format(c.time)
        } catch (_: Throwable) { s }
    }
    // ওয়েবের netInRange-এর হুবহু: start থেকে endExcl-এর আগে পর্যন্ত আয় − দুই উৎসের খরচ।
    private fun netInRange(coll: JSONArray, exps: JSONArray, start: String, endExcl: String): Double {
        var inc = 0.0; var exp = 0.0
        for (i in 0 until coll.length()) { val r = coll.getJSONObject(i); val d = r.optString("entry_date"); if (d >= start && d < endExcl) { inc += r.optDouble("cash", 0.0) + r.optDouble("online", 0.0); exp += rowExpense(r) } }
        for (i in 0 until exps.length()) { val e = exps.getJSONObject(i); val d = e.optString("entry_date"); if (d >= start && d < endExcl) exp += e.optDouble("amount", 0.0) }
        return inc - exp
    }
    /* 🟢🔒 V1651 (21.09.2026, TK অনুমোদিত) — Partner Shares এখন Income-Expense-এর
       একই AUTO-income rule ব্যবহার করে। 01/09/2026-এর আগে কিছুই বদলায় না; যে
       তারিখে সত্যিকারের fin.collections সারি আছে সেখানে manual-ই জেতে; বাকি
       তারিখে PaymentRepository-এর প্রমাণিত collection rows যোগ হয়।
       ⛔ DB-তে কিছু লেখা হয় না। ⛔ পড়া ব্যর্থ হলে কম/ভুল ভাগ দেখানোর বদলে screen
          fail করে — টাকা/Settlement-এ ভুল অঙ্ক ব্যবহার করা যাবে না। */
    private fun withAutoIncome(branch: String, manual: JSONArray): JSONArray {
        val out = JSONArray()
        val realDates = HashSet<String>()
        for (i in 0 until manual.length()) {
            val r = manual.getJSONObject(i)
            out.put(r)
            val d = r.optString("entry_date", "").take(10)
            if (d.length == 10) realDates.add(d)
        }
        val now = today()
        if (now < AUTO_INCOME_FROM) return out
        val from = if (yearStart() > AUTO_INCOME_FROM) yearStart() else AUTO_INCOME_FROM
        val rows = com.tkbiswas.pilesclinic.native.PaymentRepository()
            .fetchCollectionRange(branch, from, now)
            ?: throw IllegalStateException("AUTO income could not load")
        val refundedMobiles = try {
            com.tkbiswas.pilesclinic.native.RefundedRecords.fetch(branch)
        } catch (_: Throwable) { HashSet<String>() }
        fun isRefunded(mob: String): Boolean {
            if (refundedMobiles.isEmpty()) return false
            val dg = mob.filter { it.isDigit() }.takeLast(10)
            return dg.length == 10 && dg in refundedMobiles
        }
        val daily = HashMap<String, DoubleArray>()
        for (row in rows) {
            val d = row.date.take(10)
            if (d.length != 10 || d < AUTO_INCOME_FROM || d > now) continue
            val enteredOn = row.paidAt.take(10)
            if (enteredOn.length == 10 && d < enteredOn) continue
            if (isRefunded(row.mobile)) continue
            val a = daily.getOrPut(d) { doubleArrayOf(0.0, 0.0) }
            a[0] += row.cashAmount
            a[1] += row.onlineAmount
        }
        for (d in daily.keys.sorted()) {
            if (realDates.contains(d)) continue
            val a = daily[d] ?: continue
            val cash = if (a[0] > 0.0) a[0] else 0.0
            val online = if (a[1] > 0.0) a[1] else 0.0
            if (cash <= 0.0 && online <= 0.0) continue
            out.put(JSONObject()
                .put("entry_date", d).put("branch", branch)
                .put("cash", cash).put("online", online)
                .put("expense_total", 0.0).put("_v1651Auto", true))
        }
        return out
    }

    // forward-only accrued: %-ইতিহাসের সেগমেন্ট ধরে; ইতিহাস না থাকলে currentPct × পুরো-বছর নেট।
    private fun accruedFor(hist: List<JSONObject>, coll: JSONArray, exps: JSONArray, currentPct: Double): Double {
        val yr = yearStart(); val endAll = dayAfter(today())
        if (hist.isEmpty()) return (currentPct / 100.0) * netInRange(coll, exps, yr, endAll)
        val segs = hist.sortedBy { it.optString("effective_from") }
        var total = 0.0
        for (i in segs.indices) {
            val start = if (segs[i].optString("effective_from") < yr) yr else segs[i].optString("effective_from")
            val end = if (i + 1 < segs.size) segs[i + 1].optString("effective_from") else endAll
            if (end <= yr || start >= end) continue
            total += (segs[i].optDouble("pct", 0.0) / 100.0) * netInRange(coll, exps, start, end)
        }
        return total
    }

    private fun computeBranch(branch: String): BData {
        val enc = urlv(branch)
        val manualColl = ModuleAuth.getRowsChecked("fin", "collections",
            "select=cash,online,expense_total,expense_notes,entry_date&entry_date=gte.${yearStart()}&entry_date=lte.${today()}&branch=eq.$enc&ignored=eq.false").rows
        // V1651: Income-Expense-এর মতোই manual + missing-day AUTO income।
        val coll = withAutoIncome(branch, manualColl)
        val parts = ModuleAuth.getRowsChecked("fin", "partners", "select=*&branch=eq.$enc").rows
        val draws = ModuleAuth.getRowsChecked("fin", "partner_drawings", "select=*&branch=eq.$enc&ignored=eq.false").rows
        // 🔵 খরচ দুই উৎস: collections-এ ঢোকানো + আলাদা fin.expenses — ফোনের today-কার্ডের মতোই।
        val exps = ModuleAuth.getRowsChecked("fin", "expenses",
            "select=amount,entry_date&entry_date=gte.${yearStart()}&entry_date=lte.${today()}&branch=eq.$enc&ignored=eq.false").rows
        val hist = ModuleAuth.getRowsChecked("fin", "partner_pct_history", "select=mobile,pct,effective_from&branch=eq.$enc").rows
        var income = 0.0; var expense = 0.0
        for (i in 0 until coll.length()) {
            val r = coll.getJSONObject(i)
            income += r.optDouble("cash", 0.0) + r.optDouble("online", 0.0)
            expense += rowExpense(r)
        }
        for (i in 0 until exps.length()) expense += exps.getJSONObject(i).optDouble("amount", 0.0)
        val net = income - expense
        val drawnBy = HashMap<String, Double>()
        for (i in 0 until draws.length()) {
            val d = draws.getJSONObject(i)
            val k = n10(d.optString("mobile")); val amt = d.optDouble("amount", 0.0)
            drawnBy[k] = (drawnBy[k] ?: 0.0) + (if (d.optString("kind") == "return") -amt else amt)
        }
        val histBy = HashMap<String, MutableList<JSONObject>>()
        for (i in 0 until hist.length()) { val h = hist.getJSONObject(i); histBy.getOrPut(n10(h.optString("mobile"))) { mutableListOf() }.add(h) }
        val list = ArrayList<PLine>()
        for (i in 0 until parts.length()) {
            val p = parts.getJSONObject(i)
            if (!p.optBoolean("active", true)) continue
            val accrued = accruedFor(histBy[n10(p.optString("mobile"))] ?: emptyList(), coll, exps, p.optDouble("pct", 0.0))
            val due = p.optDouble("opening", 0.0) + accrued
            val drawn = drawnBy[n10(p.optString("mobile"))] ?: 0.0
            list.add(PLine(p, due, drawn, due - drawn))
        }
        return BData(income, expense, net, list)
    }

    // ---------- OVERVIEW ----------
    private fun openBranch(branch: String) {
        var freshD: BData? = null
        val (_, col) = screen("🤝 $branch") { anchor ->
            val current = freshD
            if (current == null) Toast.makeText(this, "Please wait — latest balance is loading.", Toast.LENGTH_SHORT).show()
            else showBranchMenu(anchor, branch, current)
        }
        // 🔒🔒 B604 (10.08.2026, TK-নির্দেশ "cache-first, ঝুঁকি ছাড়া"): সব dynamic
        // কনটেন্ট একটা content-container-এ। জমানো থাকলে সাথে সাথে Net Profit +
        // অংশীদার-কার্ড দেখানো হয়। ⛔⛔ টাকার বোতাম (Withdraw/Return · Settlement ·
        // Print · Setup) **কখনো বাসি ক্যাশ থেকে নয়** — শুধু ক্লাউড থেকে fresh `d`
        // এলে বসে, যাতে পুরনো balance-এ ভুল টাকা-কাজ কখনো না হয়।
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(content)
        val cachedD = loadBranchCache(branch)
        if (cachedD != null) {
            try { renderPartnerCards(content, cachedD) } catch (_: Throwable) {}
            content.addView(TextView(this).apply { text = NoBengali.s("হালনাগাদ হচ্ছে…"); textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#7c8a83")); setPadding(0, dp(8), 0, dp(8)) })
        } else {
            content.addView(TextView(this).apply { text = "Loading…"; setTextColor(android.graphics.Color.parseColor("#7c8a83")) })
        }
        Thread {
            val d = try { computeBranch(branch) } catch (e: Throwable) { null }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (d == null) {
                    // ⛔ cache থাকলে পুরনো display-ই থাক (নেট এলে হালনাগাদ); টাকার বোতাম আসেনি বলে ঝুঁকি নেই।
                    if (cachedD == null) {
                        content.removeAllViews()
                        content.addView(TextView(this).apply { text = "Could not load (net?)."; setTextColor(android.graphics.Color.parseColor("#B42318")) })
                    }
                    content.addView(greenButton("← Back", "#5b6b62") { renderBranchList() }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(12) } })
                    return@runOnUiThread
                }
                freshD = d
                saveBranchCache(branch, d)
                content.removeAllViews()
                try {
                renderPartnerCards(content, d)
                // V1656: Financial actions moved into the top-right overflow (⋮).
                // They stay unavailable until fresh cloud balance has loaded.
                content.addView(TextView(this).apply {
                    text = "💵 A withdrawal reduces the branch cash balance — not the Net Profit."
                    textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#0A6b38"))
                    setPadding(dp(11), dp(9), dp(11), dp(9))
                    background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(10).toFloat(); setColor(android.graphics.Color.parseColor("#E7F6EC")) }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
                })
                content.addView(greenButton("← Back", "#5b6b62") { renderBranchList() }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(12) } })
                } catch (e: Throwable) {
                    content.addView(TextView(this).apply { text = "Screen error: " + (e.message ?: e.toString()); setTextColor(android.graphics.Color.parseColor("#B42318")); setPadding(0, dp(8), 0, dp(8)) })
                    content.addView(greenButton("← Back", "#5b6b62") { renderBranchList() }.apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(12) } })
                }
            }
        }.start()
    }

    private fun showBranchMenu(anchor: android.view.View, branch: String, d: BData) {
        android.widget.PopupMenu(this, anchor).apply {
            menu.add("Withdraw / Return")
            menu.add("Setup")
            menu.add("Settlement")
            menu.add("Print / Export")
            setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Withdraw / Return" -> renderDraw(branch, d)
                    "Setup" -> renderSetup(branch)
                    "Settlement" -> settleBranch(branch, d)
                    "Print / Export" -> printBranch(branch, d)
                }
                true
            }
            show()
        }
    }

    // 🔒 B604: Net Profit কার্ড + অংশীদার-কার্ড দেখানো (আগে openBranch-এর ভিতরে ছিল —
    // হুবহু একই কোড; ক্যাশ ও fresh দুই ক্ষেত্রেই এটাই ডাকা হয়)। ⛔ কোনো টাকার বোতাম নেই।
    private fun renderPartnerCards(host: LinearLayout, d: BData) {
        val net = card()
        kvRow(net, "Net Profit · Jan → Today", "", "#0A5C33", true)
        kvRow(net, "Total Income", money(d.income), "#111111", false)
        kvRow(net, "Total Expense", money(d.expense), "#B42318", false)
        kvRow(net, "Net Profit", money(d.net), if (d.net < 0) "#B42318" else "#0A7C3F", true)
        host.addView(net)
        if (d.list.isEmpty()) host.addView(TextView(this).apply { text = "No partners yet. Tap Setup."; setTextColor(android.graphics.Color.parseColor("#7c8a83")); setPadding(0, dp(6), 0, dp(6)) })
        for (x in d.list) {
            val outer = card().apply { orientation = LinearLayout.VERTICAL }
            val c = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
            val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            left.addView(TextView(this).apply { text = ps(x.p, "name").ifBlank { n10(ps(x.p, "mobile")) }; textSize = 14f; setTextColor(android.graphics.Color.parseColor("#111111")); setTypeface(typeface, android.graphics.Typeface.BOLD) })
            left.addView(TextView(this).apply { text = "Due " + money(x.due) + " · Withdrawn " + money(x.drawn); textSize = 11f; setTextColor(android.graphics.Color.parseColor("#0A5C33")); setTypeface(typeface, android.graphics.Typeface.BOLD) })
            c.addView(left)
            val red = x.bal < 0
            c.addView(TextView(this).apply {
                text = (if (red) "🔴 " else "🟢 ") + money(x.bal)
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor(if (red) "#B42318" else "#0A7C3F"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(android.graphics.Color.parseColor(if (red) "#FBEAE8" else "#E7F6EC"))
                    setStroke(dp(1), android.graphics.Color.parseColor(if (red) "#F0C4BE" else "#B7E3C5"))
                }
            })
            outer.addView(c)
            // V1655: Partner Shares is financial-only. Location controls belong to Doctor/Staff location screens, not partner cards.
            host.addView(outer)
        }
    }

    // 🔒 B604: Partner overview-এর হালকা display-ক্যাশ (income/expense/net + প্রতি
    // অংশীদারের name/mobile/due/drawn/bal)। ⛔ শুধু দেখানোর জন্য — টাকার কাজ fresh `d`-তে।
    private fun partnerCachePrefs() = getSharedPreferences("partner_overview_cache", MODE_PRIVATE)
    private fun saveBranchCache(branch: String, d: BData) {
        try {
            val arr = JSONArray()
            for (x in d.list) arr.put(JSONObject().put("name", ps(x.p, "name")).put("mobile", ps(x.p, "mobile")).put("due", x.due).put("drawn", x.drawn).put("bal", x.bal))
            val o = JSONObject().put("income", d.income).put("expense", d.expense).put("net", d.net).put("list", arr)
            partnerCachePrefs().edit().putString("br_$branch", o.toString()).apply()
        } catch (_: Throwable) {}
    }
    private fun loadBranchCache(branch: String): BData? {
        return try {
            val o = JSONObject(partnerCachePrefs().getString("br_$branch", null) ?: return null)
            val arr = o.optJSONArray("list") ?: JSONArray()
            val list = ArrayList<PLine>()
            for (i in 0 until arr.length()) { val x = arr.getJSONObject(i); list.add(PLine(JSONObject().put("name", ps(x, "name")).put("mobile", ps(x, "mobile")), x.optDouble("due"), x.optDouble("drawn"), x.optDouble("bal"))) }
            BData(o.optDouble("income"), o.optDouble("expense"), o.optDouble("net"), list)
        } catch (_: Throwable) { null }
    }

    // ---------- PRINT / EXPORT (master) — WebView → PrintManager ----------
    @Suppress("StaticFieldLeak")
    private var printWv: android.webkit.WebView? = null
    private fun printBranch(branch: String, d: BData) {
        val sb = StringBuilder()
        sb.append("<div style=\"font-family:Arial;padding:6px\">")
        sb.append("<h2 style=\"color:#0A5C33;margin:0 0 2px\">Partner Shares — ").append(esc(branch)).append("</h2>")
        sb.append("<div style=\"color:#555;font-size:12px;margin-bottom:10px\">January &#8594; ").append(esc(today()))
            .append(" &middot; Net Profit: <b>").append(esc(money(d.net))).append("</b> (Income ").append(esc(money(d.income)))
            .append(" &minus; Expense ").append(esc(money(d.expense))).append(")</div>")
        sb.append("<table style=\"border-collapse:collapse;width:100%;font-size:13px\">")
        sb.append("<tr style=\"background:#EAF6EE;color:#0A5C33;-webkit-print-color-adjust:exact;print-color-adjust:exact\"><th style=\"border:1px solid #cfe0d6;padding:6px;text-align:left\">Partner</th>")
        sb.append("<th style=\"border:1px solid #cfe0d6;padding:6px\">Due</th><th style=\"border:1px solid #cfe0d6;padding:6px\">Withdrawn</th><th style=\"border:1px solid #cfe0d6;padding:6px\">Balance</th></tr>")
        for (x in d.list) {
            val red = x.bal < 0
            sb.append("<tr><td style=\"border:1px solid #cfe0d6;padding:6px\">").append(esc(ps(x.p, "name").ifBlank { n10(ps(x.p, "mobile")) }))
                .append("<br><small style=\"color:#777\">+91 ").append(esc(n10(x.p.optString("mobile")))).append("</small></td>")
                .append("<td style=\"border:1px solid #cfe0d6;padding:6px;text-align:right\">").append(esc(money(x.due))).append("</td>")
                .append("<td style=\"border:1px solid #cfe0d6;padding:6px;text-align:right\">").append(esc(money(x.drawn))).append("</td>")
                .append("<td style=\"border:1px solid #cfe0d6;padding:6px;text-align:right;font-weight:800;color:").append(if (red) "#B42318" else "#0A7C3F").append("\">")
                .append(esc(money(x.bal))).append("</td></tr>")
        }
        sb.append("</table><div style=\"color:#777;font-size:11px;margin-top:10px\">Green = still owed to the partner &middot; Red = over-drawn (owes back). Auto-forwards to next year.</div></div>")
        val html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>$sb</body></html>"
        try {
            val wv = android.webkit.WebView(this)
            wv.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: android.webkit.WebView, url: String) {
                    try {
                        val pm = this@PartnerSharesActivity.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                        val job = "Partner Shares - $branch"
                        pm.print(job, view.createPrintDocumentAdapter(job),
                            com.tkbiswas.pilesclinic.native.PrintQuality.builder().setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).build())
                    } catch (e: Throwable) { Toast.makeText(this@PartnerSharesActivity, "Could not open print.", Toast.LENGTH_SHORT).show() }
                }
            }
            printWv = wv
            wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        } catch (e: Throwable) { Toast.makeText(this, "Could not open print.", Toast.LENGTH_SHORT).show() }
    }

    // ---------- SETTLEMENT ----------
    // V1657 safety: Settlement never writes on the first tap. Master must review the exact
    // balances, type a mandatory note and tick an explicit confirmation. The actual write is
    // one authenticated DB RPC transaction, so a network/error cannot leave half the partners settled.
    private var settlementInProgress = false

    private fun settleBranch(branch: String, d: BData) {
        if (settlementInProgress) {
            Toast.makeText(this, "A settlement is already in progress.", Toast.LENGTH_SHORT).show()
            return
        }
        val toDo = d.list.filter { Math.abs(it.bal) >= 0.5 }
        if (toDo.isEmpty()) {
            Toast.makeText(this, "All balances are already zero — nothing to settle.", Toast.LENGTH_LONG).show()
            return
        }
        val lines = toDo.joinToString("\n") {
            (ps(it.p, "name").ifBlank { n10(ps(it.p, "mobile")) }) + ": " +
                (if (it.bal > 0) "pay " else "collect ") + money(Math.abs(it.bal))
        }
        val total = toDo.sumOf { Math.abs(it.bal) }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(4), dp(18), 0)
        }
        body.addView(TextView(this).apply {
            text = "⚠ Review carefully\n\n$lines\n\nTotal Settlement: ${money(total)}\n\nAfter settlement, these balances become ₹0."
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#7A1A1A"))
            setPadding(0, 0, 0, dp(10))
        })
        body.addView(TextView(this).apply {
            text = "Remarks / Note *"
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0A5C33"))
        })
        val note = EditText(this).apply {
            hint = "e.g. Paid by cash today / settled for August 2026"
            minLines = 2
            maxLines = 3
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        body.addView(note, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
        val confirmBox = CheckBox(this).apply {
            text = "I confirm that the above payment / collection has actually been made and I want to proceed."
            textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#243447"))
        }
        body.addView(confirmBox)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Confirm Settlement")
            .setView(body)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm & Settle", null)
            .create()
        dialog.setOnShowListener {
            try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dialog) } catch (_: Throwable) { }
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            fun refreshEnabled() {
                positive.isEnabled = note.text.toString().trim().isNotEmpty() && confirmBox.isChecked && !settlementInProgress
            }
            positive.isEnabled = false
            note.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshEnabled() }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
            confirmBox.setOnCheckedChangeListener { _, _ -> refreshEnabled() }
            positive.setOnClickListener {
                val typedNote = note.text.toString().trim()
                if (typedNote.isEmpty()) {
                    note.error = "Remarks / Note is mandatory"
                    return@setOnClickListener
                }
                if (!confirmBox.isChecked) {
                    Toast.makeText(this, "Please tick the confirmation box.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (settlementInProgress) return@setOnClickListener
                settlementInProgress = true
                positive.isEnabled = false
                negative.isEnabled = false
                dialog.setCanceledOnTouchOutside(false)
                positive.text = "Settling…"

                val rows = JSONArray()
                for (x in toDo) {
                    rows.put(JSONObject()
                        .put("mobile", n10(x.p.optString("mobile")))
                        .put("amount", Math.abs(x.bal))
                        .put("kind", if (x.bal > 0) "withdraw" else "return")
                        .put("balance_before", x.bal))
                }
                Thread {
                    val r = ModuleAuth.rpc("fin", "partner_settle_atomic", JSONObject()
                        .put("p_branch", branch)
                        .put("p_note", typedNote)
                        .put("p_rows", rows))
                    runOnUiThread {
                        settlementInProgress = false
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        if (r.ok) {
                            dialog.dismiss()
                            Toast.makeText(this, "Settlement completed safely — all selected balances are now zero.", Toast.LENGTH_LONG).show()
                            openBranch(branch)
                        } else {
                            positive.text = "Confirm & Settle"
                            negative.isEnabled = true
                            refreshEnabled()
                            Toast.makeText(this, "Settlement was not saved. Nothing was partially settled. " + r.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
        }
        dialog.show()
    }

    // ---------- SETUP ----------
    private class SetupRow(var id: String, val oldPct: Double, val name: EditText, val pct: EditText, val mobile: EditText, val opening: EditText, val canEntry: CheckBox, val active: CheckBox, val histFirst: String?, val hadHistory: Boolean, val dateView: TextView)
    private var setupIsLaunch = false

    // 🔒 B595: "ভাগ শুরুর তারিখ" ঘরে চাপলে ডেট-পিকার (yyyy-MM-dd রাখে)।
    private fun attachDatePicker(tv: TextView) {
        tv.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            try {
                val d = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(tv.text.toString())
                if (d != null) cal.time = d
            } catch (_: Throwable) {}
            android.app.DatePickerDialog(this, { _, y, m, day ->
                tv.text = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, day)
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun renderSetup(branch: String) {
        val (_, col) = screen("⚙ Setup — $branch")
        val loading = TextView(this).apply { text = "Loading…"; setTextColor(android.graphics.Color.parseColor("#7c8a83")) }
        col.addView(loading)
        Thread {
            val res = ModuleAuth.getRowsChecked("fin", "partners", "select=*&branch=eq." + urlv(branch) + "&order=created_at.asc")
            val existing = res.rows
            // B595: oldest share-start date per partner. Data rules are unchanged; V1658 is UI-only.
            val firstBy595 = HashMap<String, String>()
            try {
                val hr = ModuleAuth.getRowsChecked("fin", "partner_pct_history", "select=mobile,effective_from&branch=eq." + urlv(branch)).rows
                for (i in 0 until hr.length()) {
                    val h = hr.getJSONObject(i); val m = n10(h.optString("mobile")); val d = h.optString("effective_from")
                    if (d.isNotBlank() && (firstBy595[m] == null || d < firstBy595[m]!!)) firstBy595[m] = d
                }
            } catch (_: Throwable) {}
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                col.removeView(loading)
                setupIsLaunch = (existing.length() == 0)
                val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                col.addView(container)
                val rows = ArrayList<SetupRow>()

                fun boxBg(fill: String = "#FBFDFC"): android.graphics.drawable.GradientDrawable =
                    android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = dp(9).toFloat()
                        setColor(android.graphics.Color.parseColor(fill))
                        setStroke(dp(1), android.graphics.Color.parseColor("#CFDED5"))
                    }
                fun label(t: String): TextView = TextView(this).apply {
                    text = t.uppercase(java.util.Locale.US)
                    textSize = 10.5f
                    setTextColor(android.graphics.Color.parseColor("#66756D"))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(dp(2), 0, dp(2), dp(3))
                }
                fun fieldCol(title: String, field: android.view.View, weight: Float = 1f): LinearLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                    addView(label(title))
                    addView(field)
                }
                fun initials(name: String): String {
                    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (words.isEmpty()) return "P"
                    val a = words.first().firstOrNull()?.uppercaseChar()?.toString() ?: "P"
                    val b = if (words.size > 1) words.last().firstOrNull()?.uppercaseChar()?.toString() ?: "" else ""
                    return (a + b).take(2)
                }

                fun addRow(p: JSONObject?) {
                    val c = card().apply { setPadding(dp(12), dp(11), dp(12), dp(11)) }

                    // Professional header: initials + editable partner name.
                    val header = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    val avatar = TextView(this).apply {
                        val nm = p?.let { ps(it, "name") } ?: ""
                        text = initials(nm)
                        gravity = android.view.Gravity.CENTER
                        textSize = 13f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#0A5C33"))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.OVAL
                            setColor(android.graphics.Color.parseColor("#E6F5EB"))
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(10) }
                    }
                    val nameEt = EditText(this).apply {
                        hint = "Partner name"
                        setText(p?.let { ps(it, "name") } ?: "")
                        textSize = 15f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setSingleLine(true)
                        setPadding(dp(10), dp(8), dp(10), dp(8))
                        background = boxBg("#FFFFFF")
                        layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                        setOnFocusChangeListener { _, _ -> avatar.text = initials(text.toString()) }
                    }
                    header.addView(avatar); header.addView(nameEt); c.addView(header)

                    val pctEt = EditText(this).apply {
                        setText(p?.let { fmtPct(it.optDouble("pct", 0.0)) } ?: "")
                        hint = "0"
                        textSize = 13f; setSingleLine(true)
                        inputType = android.text.InputType.TYPE_CLASS_TEXT
                        keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(dp(10), 0, dp(10), 0); background = boxBg()
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                    }
                    val mobEt = EditText(this).apply {
                        setText(p?.let { n10(it.optString("mobile")) } ?: "")
                        hint = "10-digit mobile"
                        textSize = 13f; setSingleLine(true)
                        inputType = android.text.InputType.TYPE_CLASS_PHONE
                        setPadding(dp(10), 0, dp(10), 0); background = boxBg()
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                    }
                    val rowA = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(10), 0, 0)
                    }
                    rowA.addView(fieldCol("Share %", pctEt, 0.7f))
                    rowA.addView(android.view.View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })
                    rowA.addView(fieldCol("Mobile", mobEt, 1.3f))
                    c.addView(rowA)

                    val openEt = EditText(this).apply {
                        setText(p?.let { fmtNum(it.optDouble("opening", 0.0)) } ?: "")
                        hint = "0"
                        textSize = 13f; setSingleLine(true)
                        inputType = android.text.InputType.TYPE_CLASS_TEXT
                        keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.-")
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(dp(10), 0, dp(10), 0); background = boxBg()
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                    }
                    val mob0595 = p?.let { n10(it.optString("mobile")) }
                    val hf595 = if (mob0595 != null) firstBy595[mob0595] else null
                    val defDate595 = hf595 ?: (if (setupIsLaunch) yearStart() else today())
                    val dateTv = TextView(this).apply {
                        text = defDate595
                        textSize = 13f
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(dp(10), 0, dp(10), 0)
                        setTextColor(android.graphics.Color.parseColor("#17221D"))
                        background = boxBg()
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
                        setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_my_calendar, 0)
                        compoundDrawablePadding = dp(6)
                    }
                    attachDatePicker(dateTv)
                    val rowB = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, dp(9), 0, 0)
                    }
                    rowB.addView(fieldCol("Opening", openEt, 0.7f))
                    rowB.addView(android.view.View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(10), 1) })
                    rowB.addView(fieldCol("Share from", dateTv, 1.3f))
                    c.addView(rowB)

                    val divider = android.view.View(this).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("#E5ECE8"))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                            topMargin = dp(10); bottomMargin = dp(4)
                        }
                    }
                    c.addView(divider)
                    val line3 = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    val greenTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0A7C3F"))
                    val canEt = CheckBox(this).apply {
                        text = "Can add I/E"; textSize = 12.5f
                        isChecked = p?.optBoolean("can_entry", false) ?: false
                        buttonTintList = greenTint
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    val actEt = CheckBox(this).apply {
                        text = "Active"; textSize = 12.5f
                        isChecked = p?.optBoolean("active", true) ?: true
                        buttonTintList = greenTint
                        gravity = android.view.Gravity.END
                    }
                    line3.addView(canEt); line3.addView(actEt); c.addView(line3)
                    container.addView(c)

                    val oldP = p?.optDouble("pct", Double.NaN) ?: Double.NaN
                    rows.add(SetupRow(p?.optString("id") ?: "", oldP, nameEt, pctEt, mobEt, openEt, canEt, actEt, hf595, hf595 != null, dateTv))
                }

                for (i in 0 until existing.length()) addRow(existing.getJSONObject(i))
                if (existing.length() == 0) addRow(null)

                col.addView(greenButton("＋ Add Partner", "#0A6B3A") { addRow(null) }.apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply { topMargin = dp(4) }
                })
                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(8), 0, 0)
                }
                val back = greenButton("← Back", "#E9F0EC") { openBranch(branch) }.apply {
                    setTextColor(android.graphics.Color.parseColor("#254137"))
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(5) }
                }
                val save = greenButton("💾 Save", "#1E7C43") { saveSetup(branch, rows) }.apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(5) }
                }
                actions.addView(back); actions.addView(save); col.addView(actions)
            }
        }.start()
    }

    private fun fmtNum(d: Double): String = if (d == Math.floor(d)) d.toLong().toString() else d.toString()

    private fun saveSetup(branch: String, rows: List<SetupRow>) {
        // সব মান UI-থ্রেডেই পড়ে নিই (ব্যাকগ্রাউন্ডে View ছোঁয়া হয় না)।
        val names = ArrayList<String>(); val pcts = ArrayList<Double>(); val mobiles = ArrayList<String>()
        val openings = ArrayList<Double>(); val canEntries = ArrayList<Boolean>(); val actives = ArrayList<Boolean>()
        val oldPcts = ArrayList<Double>()
        // 🔒 B595: প্রতি অংশীদারের বাছা "ভাগ শুরুর তারিখ" ও পুরনো ইতিহাস-তথ্য।
        val startDates = ArrayList<String>(); val histFirsts = ArrayList<String?>(); val hadHistories = ArrayList<Boolean>()
        for (r in rows) {
            names.add(r.name.text.toString().trim())
            pcts.add(r.pct.text.toString().toDoubleOrNull() ?: 0.0)
            mobiles.add(n10(r.mobile.text.toString()))
            openings.add(r.opening.text.toString().toDoubleOrNull() ?: 0.0)
            canEntries.add(r.canEntry.isChecked)
            actives.add(r.active.isChecked)
            oldPcts.add(r.oldPct)
            startDates.add(r.dateView.text.toString().trim())
            histFirsts.add(r.histFirst)
            hadHistories.add(r.hadHistory)
        }
        val isLaunch = setupIsLaunch
        var bad = false
        for (i in rows.indices) if (actives[i] && mobiles[i].length != 10) bad = true
        if (bad) { Toast.makeText(this, "Enter a valid 10-digit mobile for each active partner.", Toast.LENGTH_LONG).show(); return }
        var sum = 0.0
        for (i in rows.indices) if (actives[i]) sum += pcts[i]
        if (Math.abs(sum - 100.0) >= 0.001) { Toast.makeText(this, "Total % must be exactly 100 (now $sum%).", Toast.LENGTH_LONG).show(); return }
        val known = knownUsers()
        val me = myMobile()
        Toast.makeText(this, "Saving…", Toast.LENGTH_SHORT).show()
        Thread {
            var ok = true
            for (i in rows.indices) {
                val mobile = mobiles[i]
                if (mobile.isBlank()) continue
                val row = JSONObject()
                    .put("branch", branch).put("mobile", mobile).put("name", names[i])
                    .put("pct", pcts[i]).put("opening", openings[i])
                    .put("can_entry", canEntries[i]).put("in_app", known.contains(mobile))
                    .put("active", actives[i]).put("created_by", me)
                    .put("updated_at", isoNow())
                if (!ModuleAuth.upsertOnConflict("fin", "partners", row, "branch,mobile")) ok = false
            }
            // forward-only %: বদল/নতুন হলে %-ইতিহাস বসাই। partner_id লাগে — তাই upsert-এর পর
            // ব্রাঞ্চের partners আবার টেনে mobile→id ম্যাপ বানাই। effective_from: launch→জানুয়ারি, নইলে আজ।
            try {
                val encB = urlv(branch)
                val fresh = ModuleAuth.getRowsChecked("fin", "partners", "select=id,mobile&branch=eq.$encB").rows
                val idByMob = HashMap<String, String>()
                for (i in 0 until fresh.length()) { val f = fresh.getJSONObject(i); idByMob[n10(f.optString("mobile"))] = f.optString("id") }
                for (i in rows.indices) {
                    val mobile = mobiles[i]
                    if (mobile.isBlank()) continue
                    val pid = idByMob[mobile] ?: continue
                    // 🔒🔒 B595 (10.08.2026, TK-অনুমোদিত প্রুফ): প্রতি অংশীদারের নিজের
                    // "ভাগ শুরুর তারিখ"। আর কখনো সবাইকে জোর করে জানুয়ারি থেকে ধরা হয় না।
                    val sd = if (Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(startDates[i])) startDates[i]
                             else (if (isLaunch) yearStart() else today())
                    if (!hadHistories[i]) {
                        // নতুন অংশীদার / আগে কোনো ইতিহাস নেই → একটাই সারি, বাছা তারিখ থেকে।
                        val h = JSONObject().put("partner_id", pid).put("branch", branch).put("mobile", mobile)
                            .put("pct", pcts[i]).put("effective_from", sd).put("created_by", me)
                        ModuleAuth.insert("fin", "partner_pct_history", h)
                    } else {
                        // পুরনো অংশীদার — (ক) শুরুর তারিখ বদলালে শুধু সবচেয়ে-পুরনো সারির তারিখ ঠিক;
                        val hf = histFirsts[i]
                        if (hf != null && sd != hf) {
                            ModuleAuth.update("fin", "partner_pct_history",
                                "branch=eq.$encB&mobile=eq.$mobile&effective_from=eq.$hf",
                                JSONObject().put("effective_from", sd))
                        }
                        // (খ) % বদলালে আগের নিয়মেই আজ থেকে নতুন সেগমেন্ট — পুরনো accrued অটুট।
                        val changed = oldPcts[i].isNaN() || oldPcts[i] != pcts[i]
                        if (changed) {
                            val h = JSONObject().put("partner_id", pid).put("branch", branch).put("mobile", mobile)
                                .put("pct", pcts[i]).put("effective_from", today()).put("created_by", me)
                            ModuleAuth.insert("fin", "partner_pct_history", h)
                        }
                    }
                }
            } catch (_: Throwable) {}
            val fok = ok
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                Toast.makeText(this, if (fok) "Partner setup saved." else "Some rows didn't save (network?)", Toast.LENGTH_LONG).show()
                if (fok) openBranch(branch)
            }
        }.start()
    }

    private fun knownUsers(): Set<String> {
        // app-এ থাকা পরিচিত ব্যবহারকারীদের মোবাইল (in_app অটো-ম্যাচ ফ্ল্যাগের জন্য)।
        val set = HashSet<String>()
        try {
            for (a in com.tkbiswas.pilesclinic.native.StaffDirectory.allAccounts()) set.add(n10(a.mobile))
        } catch (_: Throwable) {}
        return set
    }

    private fun isoNow(): String {
        val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        f.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date())
    }

    // ---------- WITHDRAW / RETURN ----------
    private fun renderDraw(branch: String, d: BData) {
        val (_, col) = screen("Withdraw / Return")
        val c = card().apply { setPadding(dp(14), dp(10), dp(14), dp(12)) }

        fun fieldBg() = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(10).toFloat()
            setColor(android.graphics.Color.parseColor("#FFFFFF"))
            setStroke(dp(1), android.graphics.Color.parseColor("#D4DED8"))
        }
        fun styleSpinner(sp: android.widget.Spinner) {
            sp.background = fieldBg()
            sp.setPadding(dp(10), 0, dp(8), 0)
            sp.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(4) }
        }
        fun styleEdit(et: EditText) {
            et.background = fieldBg(); et.setPadding(dp(12), 0, dp(12), 0); et.textSize = 14f
            et.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(4) }
        }

        c.addView(labelTv("Partner *"))
        val partnerNames = listOf("Select Partner") + d.list.map { ps(it.p, "name").ifBlank { n10(ps(it.p, "mobile")) } }
        val partnerMobiles = d.list.map { n10(it.p.optString("mobile")) }
        val spWho = android.widget.Spinner(this)
        spWho.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, partnerNames)
        spWho.setSelection(0, false); styleSpinner(spWho); c.addView(spWho)

        c.addView(labelTv("Type *"))
        val spKind = android.widget.Spinner(this)
        spKind.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Withdraw (money out)", "Return (money back)"))
        styleSpinner(spKind); c.addView(spKind)

        c.addView(labelTv("Mode *"))
        val spMode = android.widget.Spinner(this)
        spMode.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Cash", "Online"))
        styleSpinner(spMode); c.addView(spMode)

        var selectedDateIso = today()
        c.addView(labelTv("Date *"))
        val dateView = TextView(this).apply {
            text = displayDate(selectedDateIso); textSize = 14f; gravity = android.view.Gravity.CENTER_VERTICAL
            setTextColor(android.graphics.Color.parseColor("#222222")); setPadding(dp(12), 0, dp(12), 0)
            background = fieldBg(); isClickable = true; isFocusable = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply { bottomMargin = dp(4) }
            setOnClickListener {
                val p = selectedDateIso.split("-")
                val y = p.getOrNull(0)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val m = (p.getOrNull(1)?.toIntOrNull() ?: 1) - 1
                val dd = p.getOrNull(2)?.toIntOrNull() ?: 1
                android.app.DatePickerDialog(this@PartnerSharesActivity, { _, yy, mm, day ->
                    selectedDateIso = String.format(java.util.Locale.US, "%04d-%02d-%02d", yy, mm + 1, day)
                    text = displayDate(selectedDateIso)
                }, y, m, dd).show()
            }
        }
        c.addView(dateView)

        c.addView(labelTv("Amount (₹) *"))
        val amtEt = EditText(this).apply {
            hint = "Enter amount"; inputType = android.text.InputType.TYPE_CLASS_TEXT
            keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789.")
        }
        styleEdit(amtEt); c.addView(amtEt)

        c.addView(labelTv("Note (optional)"))
        val noteEt = EditText(this).apply { hint = "e.g. cash in hand" }
        styleEdit(noteEt); c.addView(noteEt)
        col.addView(c)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
        }
        val back = greenButton("← Back", "#5b6b62") { openBranch(branch) }.apply {
            layoutParams = LinearLayout.LayoutParams(dp(118), dp(42)).apply { rightMargin = dp(5) }
        }
        val save = greenButton("💾 Save", "#1E7C43") {
            val idx = spWho.selectedItemPosition
            if (idx <= 0 || idx > partnerMobiles.size) { Toast.makeText(this, "Select a partner.", Toast.LENGTH_SHORT).show(); return@greenButton }
            val amt = amtEt.text.toString().toDoubleOrNull() ?: 0.0
            if (amt <= 0.0) { Toast.makeText(this, "Enter an amount.", Toast.LENGTH_SHORT).show(); return@greenButton }
            val mobile = partnerMobiles[idx - 1]
            val kind = if (spKind.selectedItemPosition == 1) "return" else "withdraw"
            val mode = if (spMode.selectedItemPosition == 1) "online" else "cash"
            val note = noteEt.text.toString()
            val me = myMobile()
            Toast.makeText(this, "Saving…", Toast.LENGTH_SHORT).show()
            Thread {
                val row = JSONObject().put("branch", branch).put("mobile", mobile)
                    .put("entry_date", selectedDateIso).put("amount", amt).put("kind", kind)
                    .put("mode", mode).put("note", note).put("created_by", me)
                val ok = ModuleAuth.insert("fin", "partner_drawings", row)
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    Toast.makeText(this, if (ok) "Saved." else "Not saved (network?)", Toast.LENGTH_LONG).show()
                    if (ok) openBranch(branch)
                }
            }.start()
        }.apply { layoutParams = LinearLayout.LayoutParams(dp(118), dp(42)).apply { leftMargin = dp(5) } }
        actions.addView(back); actions.addView(save); col.addView(actions)
    }

    private fun displayDate(iso: String): String {
        return try {
            val input = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val output = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
            output.format(input.parse(iso)!!)
        } catch (_: Throwable) { iso }
    }

    private fun labelTv(t: String): TextView = TextView(this).apply {
        text = t; textSize = 12.5f; setTextColor(android.graphics.Color.parseColor("#315448")); setPadding(0, dp(9), 0, dp(4))
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    // 🟢🆕 কম্প্যাক্ট অ্যাকশন-টাইল (আইকন বাঁয়ে ছোট বাক্সে, পাশে লেখা)। সাদা কার্ড, সবুজ বর্ডার —
    // অ্যাপের হোম-গ্রিডের সাথে সঙ্গতিপূর্ণ। লেখা ২ লাইন হলেও কাটে না (wrap করে)।
    private fun actionTile(icon: String, label: String, iconBg: String, onClick: () -> Unit): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(9), dp(8), dp(9), dp(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(android.graphics.Color.WHITE)
                setStroke(dp(1), android.graphics.Color.parseColor("#D9E6DD"))
            }
            isClickable = true
            setOnClickListener { onClick() }
        }
        tile.addView(TextView(this).apply {
            text = icon; textSize = 15f; gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { rightMargin = dp(8) }
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(8).toFloat(); setColor(android.graphics.Color.parseColor(iconBg))
            }
        })
        tile.addView(TextView(this).apply {
            text = label; textSize = 12.5f
            setTextColor(android.graphics.Color.parseColor("#112211"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        return tile
    }
}
