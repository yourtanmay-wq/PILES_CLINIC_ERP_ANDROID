package com.tkbiswas.pilesclinic.native

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.R
import org.json.JSONArray
import org.json.JSONObject

/* 🔍🔒 V1284 (১০.০৯.২০২৬, TK-অনুমোদিত প্ল্যান ④ — তালিকা সারি ৪১০, ফটো-প্রুফ পাশ):
   TK: *"ভবিষ্যতে এরকম একই ব্যক্তির নাম যেন দুইবার না হয় · একই রকমের পেমেন্ট একই
   দিনে ওয়ার্নিং দেখাবে"* → *"২ আর ৪ করুন, সাবধানে"* → *"হ্যাঁ পাশ, দুটোতেই"*।

   মাস্টারের **Duplicate Check** পর্দা — Reports-এর ভিতরে একটা কার্ড থেকে খোলে।
   ১০.০৯-এ হাতে SQL চালিয়ে যে তিনটে খোঁজ করা হয়েছিল, ঠিক সেই তিনটেই:
     ① একই মোবাইলে একাধিক রোগী-সারি
     ② একই নাম + একই ব্রাঞ্চ (মোবাইল আলাদা) — ①-এ যাঁরা আগেই আছেন তাঁরা বাদ
     ③ একই রোগীর একই দিনে একই অঙ্ক · একই ধরনের একাধিক পেমেন্ট
   ⛔ **শুধু দেখা** — এই পর্দা থেকে কিছু মোছা/বদলানো হয় না।
   ⛔ Egress: চাপলে তবেই patients + payments একবার সরু ঘরে নামে (ছবি নয়);
      স্বয়ংক্রিয় কোনো পড়া নেই। শুধু মাস্টার।
   ⛔ ধরা পড়া মানেই ভুল নয় — পরিবারের এক নম্বর, পুরনো-হিসাবের কিস্তি হতে পারে;
      সিদ্ধান্ত TK-র (নিয়ম ৫ক)। */
class DuplicateCheckActivity : AppCompatActivity() {

    private lateinit var col: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var tvSum: List<TextView>
    private lateinit var btnAgain: TextView
    private var loading = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun money(v: Double) = "₹" + "%,.0f".format(v)
    private fun digits(s: String) = s.filter { it.isDigit() }.takeLast(10)
    private fun JSONObject.s(k: String) = if (isNull(k)) "" else optString(k, "")
    private fun ddmm(iso: String): String {
        val d = iso.take(10)
        return if (d.length == 10) d.substring(8, 10) + "/" + d.substring(5, 7) else d
    }
    private fun box(fill: String, stroke: String, radius: Int, left: String? = null) = GradientDrawable().apply {
        setColor(Color.parseColor(fill)); cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.parseColor(stroke))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = NativeSession.current(this)
        if (session == null || session.role != "master") {
            Toast.makeText(this, "Only Master Admin", Toast.LENGTH_LONG).show()
            finish(); return
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F2F6FA"))
        }
        val hdr = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_more_header_green)
            setPadding(dp(16), dp(18), dp(16), dp(18))
        }
        hdr.addView(TextView(this).apply {
            text = "←"; textSize = 22f; setTextColor(Color.WHITE)
            setPadding(0, 0, dp(14), 0); setOnClickListener { finish() }
        })
        hdr.addView(TextView(this).apply {
            text = "Duplicate Check"; textSize = 20f; setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(hdr)

        val scroll = ScrollView(this)
        col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }
        // summary
        val sum = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = box("#FFFFFF", "#DDE5EE", 14)
            setPadding(dp(10), dp(12), dp(10), dp(12))
        }
        val labels = listOf("Same mobile", "Same name · branch", "Same amount · day")
        tvSum = labels.map { lb ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val n = TextView(this).apply {
                text = "–"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#0B5C56")); gravity = Gravity.CENTER
            }
            cell.addView(n)
            cell.addView(TextView(this).apply {
                text = lb; textSize = 11.5f; setTextColor(Color.parseColor("#5A6B7C")); gravity = Gravity.CENTER
            })
            sum.addView(cell); n
        }
        col.addView(sum)
        btnAgain = TextView(this).apply {
            text = "⟳ Check again"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = box("#0B5C56", "#0B5C56", 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
            setOnClickListener { load() }
        }
        col.addView(btnAgain)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(body)
        scroll.addView(col)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        load()
    }

    private fun note(text: String, color: String = "#7A8699") {
        body.addView(TextView(this).apply {
            this.text = text; textSize = 12.5f; setTextColor(Color.parseColor(color))
            gravity = Gravity.CENTER; setPadding(dp(8), dp(18), dp(8), dp(8))
        })
    }

    private fun load() {
        if (loading) return
        loading = true
        body.removeAllViews()
        note("Checking…", "#5A6B7C")
        btnAgain.alpha = 0.6f
        Thread {
            val pats = try {
                SupabaseClient.fetchListSlimOrNull("patients", null, 5000,
                    "id,patientId,name,mobile,age,sex,branch,bill,createdAt")
            } catch (_: Throwable) { null }
            val pays = try {
                SupabaseClient.fetchListSlimOrNull("payments", null, 5000,
                    "id,patientId,date,amount,payType,mode,receivedBy,createdAt")
            } catch (_: Throwable) { null }
            runOnUiThread {
                loading = false
                btnAgain.alpha = 1f
                body.removeAllViews()
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (pats == null || pays == null) {
                    tvSum.forEach { it.text = "–" }
                    note("Could not read from cloud — check internet and tap Check again", "#C44E4E")
                    return@runOnUiThread
                }
                render(pats, pays)
            }
        }.start()
    }

    private class P(val id: String, val code: String, val name: String, val mobile: String, val age: String,
                    val sex: String, val branch: String, val bill: Double, val created: String) {
        var paid = 0.0
    }

    private fun render(pats: JSONArray, pays: JSONArray) {
        val byId = LinkedHashMap<String, P>()
        for (i in 0 until pats.length()) {
            val r = pats.optJSONObject(i) ?: continue
            val id = r.s("id"); if (id.isBlank()) continue
            byId[id] = P(id, r.s("patientId"), r.s("name").trim(), digits(r.s("mobile")), r.s("age"), r.s("sex").take(1),
                r.s("branch"), r.optDouble("bill", 0.0), r.s("createdAt"))
        }
        val skipTypes = setOf("chamber_expected", "bill_edit", "attendance_mark")
        val payGroups = LinkedHashMap<String, ArrayList<JSONObject>>()
        for (i in 0 until pays.length()) {
            val y = pays.optJSONObject(i) ?: continue
            val pt = y.s("payType")
            if (pt in skipTypes) continue
            val pid = y.s("patientId")
            val amt = y.optDouble("amount", 0.0)
            if (pt != "refund") byId[pid]?.let { it.paid += amt }
            val k = pid + "|" + y.s("date").take(10) + "|" + amt + "|" + pt
            payGroups.getOrPut(k) { ArrayList() }.add(y)
        }
        val all = byId.values.toList()
        // ① same mobile
        val byMob = LinkedHashMap<String, ArrayList<P>>()
        for (p in all) if (p.mobile.length == 10) byMob.getOrPut(p.mobile) { ArrayList() }.add(p)
        val mobGroups = byMob.values.filter { it.size > 1 }.sortedBy { it[0].name }
        val inMob = HashSet<String>(); mobGroups.forEach { g -> g.forEach { inMob.add(it.id) } }
        // ② same name + branch (not already in ①)
        val byName = LinkedHashMap<String, ArrayList<P>>()
        for (p in all) if (p.name.isNotBlank()) byName.getOrPut(p.branch.trim().uppercase() + "|" + p.name.uppercase()) { ArrayList() }.add(p)
        val nameGroups = byName.values.map { g -> g.filter { it.id !in inMob } }.filter { it.size > 1 }.sortedBy { it[0].name }
        // ③ same amount same day
        val dupPays = payGroups.values.filter { it.size > 1 }.sortedBy { byId[it[0].s("patientId")]?.name ?: "" }

        tvSum[0].text = mobGroups.size.toString()
        tvSum[1].text = nameGroups.size.toString()
        tvSum[2].text = dupPays.size.toString()

        if (mobGroups.isEmpty() && nameGroups.isEmpty() && dupPays.isEmpty()) {
            note("No duplicates found ✓", "#0EA25F"); return
        }
        if (mobGroups.isNotEmpty()) {
            /* 🔴🔒 V1505 (১৫.০৯.২০২৬, TK-রিপোর্ট, স্টাফ ছবিসহ — RUHMAT/PROMOTH/
               SARFARAZ একই নম্বরে ৩ জন আলাদা মানুষ, অথচ এই পর্দা সবাইকে
               "PROMOTH PARVIN — 3 rows" বলে দেখাচ্ছিল) — একই মোবাইলে একাধিক
               সারি থাকলে তারা সবসময় একই ব্যক্তি নয় (পরিবারের নম্বর ভাগাভাগি
               হতেই পারে), কিন্তু সাব-লাইনে নাম ছিলই না — শুধু প্রথম জনের নাম
               উপরের হেডারে বসত, বাকিদের নাম হারিয়ে যেত। এখন প্রতিটা সারিতে
               নিজের নাম আগে বসে, তাই আলাদা মানুষ স্পষ্ট বোঝা যায়। */
            section("SAME MOBILE — ${mobGroups.size}")
            for (g in mobGroups) card("#D9612F", "#FDEEE9", "#D9612F", g[0].name, "${g.size} rows",
                "📞 +91${g[0].mobile} · ${g[0].branch.uppercase()}",
                g.sortedBy { it.created }.map { p -> Pair("${p.name.ifBlank { "-" }} · ${p.code} · ${p.age} ${p.sex} · Bill ${money(p.bill)} · Paid ${money(p.paid)}", ddmm(p.created)) })
        }
        if (nameGroups.isNotEmpty()) {
            section("SAME NAME IN BRANCH — ${nameGroups.size}")
            for (g in nameGroups) card("#C99A19", "#FFF8E1", "#C99A19", g[0].name, "${g.size} rows",
                "${g[0].branch.uppercase()} · ${g[0].age} ${g[0].sex}",
                g.sortedBy { it.created }.map { p -> Pair("${p.code} · ${p.mobile} · Bill ${money(p.bill)} · Paid ${money(p.paid)}", ddmm(p.created)) })
        }
        if (dupPays.isNotEmpty()) {
            section("SAME AMOUNT · SAME DAY — ${dupPays.size}")
            for (g in dupPays) {
                val y = g[0]; val p = byId[y.s("patientId")]
                val amt = y.optDouble("amount", 0.0)
                card("#D9612F", "#FDEEE9", "#D9612F", p?.name ?: y.s("patientId"), "${g.size} × ${money(amt)}",
                    "${p?.code ?: ""} · ${(p?.branch ?: "").uppercase()}",
                    listOf(Pair("${DateUtil.display(y.s("date"))} · ${y.s("payType").replace('_', ' ')} · ${y.s("mode")} · ${y.s("receivedBy")}", "${g.size} rows")))
            }
        }
        note("View only — nothing is deleted from this screen")
    }

    private fun section(t: String) {
        body.addView(TextView(this).apply {
            text = t; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#0B5C56")); setPadding(0, dp(14), 0, dp(4))
        })
    }

    private fun card(edge: String, pillBg: String, pillFg: String, name: String, pill: String, sub: String, rows: List<Pair<String, String>>) {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = box("#FFFFFF", "#DDE5EE", 14)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(8) }
        }
        outer.addView(View(this).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor(edge)); cornerRadius = dp(3).toFloat() }
            layoutParams = LinearLayout.LayoutParams(dp(5), LinearLayout.LayoutParams.MATCH_PARENT)
        })
        val inner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = name.ifBlank { "-" }; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#101C2E"))
        })
        top.addView(TextView(this).apply {
            text = pill; textSize = 10.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor(pillFg))
            background = box(pillBg, pillBg, 999); setPadding(dp(8), dp(2), dp(8), dp(2))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8) }
        })
        inner.addView(top)
        inner.addView(TextView(this).apply { text = sub; textSize = 13f; setTextColor(Color.parseColor("#4A78D6")) })
        for ((left, right) in rows) {
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, 0)
            }
            line.addView(TextView(this).apply {
                text = left; textSize = 12.5f; setTextColor(Color.parseColor("#334455"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            line.addView(TextView(this).apply {
                text = right; textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#101C2E"))
                setPadding(dp(8), 0, 0, 0)
            })
            inner.addView(line)
        }
        outer.addView(inner)
        body.addView(outer)
    }
}
