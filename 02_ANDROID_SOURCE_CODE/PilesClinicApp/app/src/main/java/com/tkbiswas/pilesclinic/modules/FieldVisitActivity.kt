package com.tkbiswas.pilesclinic.modules

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.native.FieldVisit
import com.tkbiswas.pilesclinic.native.NativeSession
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 🏍️🔒 V968 (০২.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ অনুযায়ী) — **ফিল্ড ভিজিট।**
 *
 * দুটো চেহারা, একই পর্দা:
 *  • **স্টাফের চেহারা** (RUPAM) — আজকের RMP ডাক্তারের তালিকা, "MARK VISIT"
 *    চাপলে সময় বসে যায়, নিচে মাসের হিসাব।
 *  • **মালিকের চেহারা** (TK) — RUPAM এখন কোথায় · কত ঘণ্টা · কত কিমি ·
 *    কজন ডাক্তার, ম্যাপে খোলার বোতাম, আর আগের দিনগুলোর তালিকা।
 *
 * ⛔ পুরনো কোনো পর্দা (Dr. Visit · Work Notebook · Staff Profile) ছোঁয়া হয়নি —
 *    এটা সম্পূর্ণ আলাদা একটা পর্দা।
 * ⛔ RMP ডাক্তারের তালিকা `public.doctor_visits` (অ্যাপের আগের থেকেই থাকা
 *    ডিরেক্টরি) থেকে **শুধু পড়া** হয় — একটাও লেখা হয় না।
 * ⛔ ভিজিটের চিহ্ন লেখা হয় আলাদা `wn.doctor_visits`-এ (আলাদা schema, তাই
 *    উপরের পুরনো টেবিলের সাথে কোনো সম্পর্কই নেই)।
 */
class FieldVisitActivity : AppCompatActivity() {

    private var ownerMode = false
    private var staffCode = ""
    private var staffMobile = ""
    private var branch = ""

    private var docs = JSONArray()
    private var doneToday = HashSet<String>()
    private var doneTime = HashMap<String, String>()

    /* 🗺️🔒 V1500 (১৫.০৯.২০২৬, TK-নির্দেশ ও ডেমো-প্রুফ পাশ) — আজকের চলমান
       (RUNNING) দিনের জন্য অ্যাপের ভিতরেই একটা ছোট্ট লাইভ মানচিত্র (osmdroid,
       বিনামূল্যে) — শেষ অবস্থানে বিন্দু + আজকের হাঁটা পথ, প্রতি ৪৫ সেকেন্ডে
       নিজে থেকে আপডেট হয়। ⛔ পুরনো দিনগুলোর "OPEN IN GOOGLE MAPS" বোতাম
       (V1452) এক অক্ষরও বদলায়নি — এটা শুধু আজকের চলমান দিনে বাড়তি। */
    private var liveMapView: MapView? = null
    private var liveMarker: Marker? = null
    private var livePolyline: Polyline? = null
    private val liveHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val liveTick = object : Runnable {
        override fun run() {
            refreshLiveMap()
            liveHandler.postDelayed(this, LIVE_REFRESH_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
            org.osmdroid.config.Configuration.getInstance().osmdroidTileCache =
                java.io.File(cacheDir, "osmdroid")
        } catch (_: Throwable) { }
        ModuleAuth.attachContext(this)
        val user = NativeSession.current(this)
        ownerMode = intent.getBooleanExtra(EXTRA_OWNER, false)
        staffCode = intent.getStringExtra(EXTRA_STAFF_CODE).orEmpty()
        staffMobile = intent.getStringExtra(EXTRA_STAFF_MOBILE).orEmpty()
        if (!ownerMode) {
            staffCode = user?.name.orEmpty().ifBlank { user?.mobile.orEmpty() }
            staffMobile = user?.mobile.orEmpty()
            branch = user?.branch.orEmpty()
            // 🔒 TK-নির্দেশ: শুধু বাইরে ঘোরা স্টাফ (এখন RUPAM)।
            if (!FieldVisit.isFieldStaff(staffMobile)) {
                ModuleUi.toast(this, "Not available for this staff")
                finish(); return
            }
        }
        render(loading = true)
        load()
    }

    override fun onResume() {
        super.onResume()
        try { liveMapView?.onResume() } catch (_: Throwable) { }
        liveHandler.removeCallbacks(liveTick)
        if (liveMapView != null) liveHandler.postDelayed(liveTick, LIVE_REFRESH_MS)
    }

    override fun onPause() {
        liveHandler.removeCallbacks(liveTick)
        try { liveMapView?.onPause() } catch (_: Throwable) { }
        super.onPause()
    }

    override fun onDestroy() {
        liveHandler.removeCallbacks(liveTick)
        super.onDestroy()
    }

    private fun today(): String = FieldVisit.todayIso()

    private fun monthKey(): String = today().take(7)

    private fun load() {
        Thread {
            val d = if (ownerMode) JSONArray() else readDoctors()
            val dv = readVisits(today())
            val month = readVisits(monthKey())
            val days = if (ownerMode) mergeOwnerDays(readDays(), readOwnerVisits()) else JSONArray()
            runOnUiThread {
                docs = d
                doneToday = HashSet()
                doneTime = HashMap()
                for (i in 0 until dv.length()) {
                    val r = dv.optJSONObject(i) ?: continue
                    val key = r.optString("doctor_mobile", "")
                    if (key.isNotBlank()) {
                        doneToday.add(key)
                        doneTime[key] = timeOf(r.optString("visited_at", ""))
                    }
                }
                if (isFinishing || isDestroyed) return@runOnUiThread
                render(loading = false, monthVisits = month.length(), days = days)
            }
        }.start()
    }

    /** RMP ডিরেক্টরি — শুধু পড়া, ব্রাঞ্চ ধরে। */
    private fun readDoctors(): JSONArray = try {
        val f = if (branch.isBlank()) null else "branch=eq." + Uri.encode(branch)
        com.tkbiswas.pilesclinic.native.SupabaseClient.fetchListSlim(
            "doctor_visits", f, 500, "id,name,mobile,area,branch", "name.asc.nullslast"
        )
    } catch (_: Throwable) { JSONArray() }

    /** আজকের (বা মাসের) ভিজিটের চিহ্ন। `key` ১০ অক্ষর = তারিখ, ৭ = মাস। */
    private fun readVisits(key: String): JSONArray = try {
        val code = if (ownerMode) staffCode else staffCode
        val q = if (key.length == 10)
            "select=doctor_mobile,visited_at&staff_code=eq." + Uri.encode(code) + "&work_date=eq." + key
        else
            "select=doctor_mobile,visited_at&staff_code=eq." + Uri.encode(code) +
                "&work_date=gte." + key + "-01&work_date=lt." + nextMonth(key) + "-01"
        ModuleAuth.getRows("wn", "doctor_visits", q)
    } catch (_: Throwable) { JSONArray() }

    private fun readDays(): JSONArray = try {
        ModuleAuth.getRows("wn", "field_visit_days",
            "select=*&staff_code=eq." + Uri.encode(staffCode) + "&order=work_date.desc&limit=30")
    } catch (_: Throwable) { JSONArray() }

    /* 🔴🔒 V1076 (০৪.০৯.২০২৬, TK: *"Rupam যে আজ ডাক্তার রেফারে গেল, কই আমি
       দেখতে পাচ্ছি না"* — খাতার সারি ১৩৯ ও ১৭৭, দুবার বলা)।
       এতদিন এই পর্দার দিনের তালিকা বানানো হত **শুধু GPS-এর সারি থেকে**
       (`field_visit_days`)। স্টাফের MARK VISIT করা ডাক্তারগুলো আলাদা ঘরে
       (`doctor_visits`) জমা হয়, তাই GPS-সারি না থাকলে ১৫টা ভিজিট থাকলেও
       দিনটাই উঠত না — TK দেখতেন "No field visit recorded yet"।
       ⇒ এখন দুটো ঘরই পড়া হয় আর দিন ধরে **মেলানো** হয়, তাই ডাক্তার মার্ক
          করা কোনো দিন আর হারাতে পারে না।
       ⛔ GPS-সারি থাকলে আগের কার্ডটা হুবহু আগের মতোই — ঘণ্টা · দূরত্ব ·
          অবস্থা · Google Maps কিছুই বদলায়নি, শুধু "Doctors N" লাইনটা যোগ। */
    private fun readOwnerVisits(): JSONArray = try {
        ModuleAuth.getRows("wn", "doctor_visits",
            "select=work_date,doctor_name,visited_at&staff_code=eq." + Uri.encode(staffCode) +
                "&order=visited_at.desc&limit=500")
    } catch (_: Throwable) { JSONArray() }

    /** দিন ধরে ডাক্তারের সংখ্যা — কার্ডে দেখানোর জন্য। */
    private var docsPerDay = HashMap<String, Int>()

    /**
     * GPS-এর দিন + ডাক্তার-মার্ক করা দিন — দুটো মিলিয়ে একটাই তালিকা,
     * নতুন তারিখ আগে। GPS-সারি না থাকা দিনের জন্য শুধু `work_date` বসানো
     * একটা সারি বানানো হয়; কার্ড আঁকার কোড ফাঁকা ঘরগুলো আগে থেকেই সামলায়
     * (ঘণ্টা "-", দূরত্ব 0.0 km, ম্যাপের বোতাম ওঠে না)।
     */
    private fun mergeOwnerDays(days: JSONArray, visits: JSONArray): JSONArray {
        docsPerDay = HashMap()
        for (i in 0 until visits.length()) {
            val d = (visits.optJSONObject(i) ?: continue).optString("work_date", "").take(10)
            if (d.isNotBlank()) docsPerDay[d] = (docsPerDay[d] ?: 0) + 1
        }
        val seen = HashSet<String>()
        val byDate = HashMap<String, org.json.JSONObject>()
        for (i in 0 until days.length()) {
            val r = days.optJSONObject(i) ?: continue
            val d = r.optString("work_date", "").take(10)
            if (d.isBlank() || !seen.add(d)) continue
            byDate[d] = r
        }
        for (d in docsPerDay.keys) {
            if (seen.add(d)) byDate[d] = org.json.JSONObject().put("work_date", d)
        }
        val out = JSONArray()
        for (d in seen.sortedDescending().take(30)) byDate[d]?.let { out.put(it) }
        return out
    }

    private fun nextMonth(key: String): String = try {
        val y = key.substring(0, 4).toInt(); val m = key.substring(5, 7).toInt()
        if (m >= 12) String.format(Locale.US, "%04d-01", y + 1)
        else String.format(Locale.US, "%04d-%02d", y, m + 1)
    } catch (_: Throwable) { key }

    /* 🕐🔒 V968 (নিজে ধরা, TK-কে পাঠানোর আগেই) — ডেটাবেসের `timestamptz` ঘর
       PostgREST **UTC-তে** ফেরত দেয় (যেমন "…T03:40:00+00:00")। আগের লেখাটা
       শুধু অক্ষর কেটে নিত, তাই পর্দায় সময় **৫ ঘণ্টা ৩০ মিনিট পিছিয়ে** দেখাত।
       এখন offset ধরে পড়া হয় আর ভারতের সময়ে দেখানো হয়। */
    private fun timeOf(iso: String): String = try {
        if (iso.isBlank()) "" else {
            val ms = parseIso(iso)
            if (ms <= 0L) "" else SimpleDateFormat("h.mm a", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            }.format(java.util.Date(ms))
        }
    } catch (_: Throwable) { "" }

    /** "…+00:00" · "…Z" · offset ছাড়া — তিনটেই পড়া যায়। */
    private fun parseIso(iso: String): Long {
        val cleaned = iso.trim().replace(" ", "T")
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss"
        )
        for ((i, p) in patterns.withIndex()) {
            try {
                val f = SimpleDateFormat(p, Locale.US)
                if (i >= 2) f.timeZone = TimeZone.getTimeZone("UTC")
                return f.parse(cleaned)?.time ?: continue
            } catch (_: Throwable) { }
        }
        return 0L
    }

    private fun dmy(iso: String): String = try {
        val p = iso.take(10).split("-")
        if (p.size == 3) p[2] + "/" + p[1] + "/" + p[0] else iso.take(10)
    } catch (_: Throwable) { iso.take(10) }

    // ─── পর্দা ────────────────────────────────────────────────────────────
    private fun render(loading: Boolean, monthVisits: Int = 0, days: JSONArray = JSONArray()) {
        val col = ModuleUi.screen(this, "")
        // 🛰️🔒 V1346 (১১.০৯.২০২৬, TK-নির্দেশ) — এই পর্দা এখন সব স্টাফের জন্যই
        // খোলে (আগে শুধু ফিল্ড-স্টাফের); ফিল্ড-স্টাফ না হলে শিরোনাম "Field
        // Visit Tracking" না বলে "Attendance Location" — বাকি সব অপরিবর্তিত।
        val ownerHeading = if (com.tkbiswas.pilesclinic.native.FieldVisit.isFieldStaffCode(staffCode))
            "Field Visit Tracking" else "Attendance Location"
        col.addView(ModuleUi.heading(this, if (ownerMode) ownerHeading else "RMP Doctors"))
        col.addView(ModuleUi.body(this,
            (if (ownerMode) staffCode.ifBlank { "-" } else branch) + "  ·  " + dmy(today())))
        if (loading) { col.addView(ModuleUi.body(this, "Loading...")); return }
        if (ownerMode) renderOwner(col, days) else renderStaff(col, monthVisits)
    }

    private fun renderStaff(col: LinearLayout, monthVisits: Int) {
        val card = ModuleUi.card(this)
        card.addView(sectionTitle("Today's visits  ·  " + doneToday.size + " done"))
        if (docs.length() == 0) {
            card.addView(ModuleUi.body(this, "No RMP doctors found for this branch."))
        }
        for (i in 0 until docs.length()) {
            val r = docs.optJSONObject(i) ?: continue
            val name = r.optString("name", "").trim().ifBlank { "Unknown" }
            val mob = r.optString("mobile", "").trim()
            val area = r.optString("area", "").trim()
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = ModuleUi.dp(this@FieldVisitActivity, 8) }
            }
            val left = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            left.addView(TextView(this).apply {
                text = name.uppercase(Locale.US)
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#101C2E"))
            })
            left.addView(TextView(this).apply {
                text = listOf(area, mob).filter { it.isNotBlank() }.joinToString("  ·  ")
                textSize = 11.5f
                setTextColor(android.graphics.Color.parseColor("#8B98A9"))
            })
            row.addView(left)
            if (doneToday.contains(mob)) {
                row.addView(TextView(this).apply {
                    text = doneTime[mob].orEmpty().ifBlank { "Visited" }
                    textSize = 11.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor("#0B7A4B"))
                })
            } else {
                row.addView(ModuleUi.buttonSoft(this, "MARK VISIT") { markVisit(name, mob, area) })
            }
            card.addView(row)
        }
        col.addView(card)

        val stat = ModuleUi.card(this)
        stat.addView(sectionTitle("This month"))
        stat.addView(ModuleUi.body(this, "Doctors visited: $monthVisits"))
        val fv = FieldVisit
        if (fv.isRunning(this)) {
            stat.addView(ModuleUi.body(this,
                "Field visit running  ·  " +
                    fv.hoursText(fv.startedAt(this), System.currentTimeMillis()) + "  ·  " +
                    fv.kmText(fv.distanceMeters(this))))
        }
        col.addView(stat)
    }

    private fun renderOwner(col: LinearLayout, days: JSONArray) {
        if (days.length() == 0) {
            col.addView(ModuleUi.card(this).also {
                it.addView(ModuleUi.body(this, "No field visit recorded yet."))
            })
            return
        }
        for (i in 0 until days.length()) {
            val r = days.optJSONObject(i) ?: continue
            val date = r.optString("work_date", "").take(10)
            val started = r.optString("started_at", "")
            val ended = r.optString("ended_at", "")
            val auto = r.optBoolean("auto_closed", false)
            val meters = r.optDouble("distance_m", 0.0)
            val card = ModuleUi.card(this)
            /* ⛔ V1076 — GPS-সারি নেই এমন দিন (শুধু ডাক্তার মার্ক করা) ভুল করে
               "NOT CLOSED" (লাল) দেখানো যাবে না; সেখানে GPS-ই চলেনি। */
            val noGps = started.isBlank() && ended.isBlank()
            /* 🩺🔒 V1333 (১১.০৯.২০২৬, TK-নির্দেশ ও গভীর যাচাই — তালিকা সারি ৪৩৪) —
               TK-রিপোর্ট: "COMPLETE" লেখা ছিল, অথচ "Hours -" — কারণ আগে এই
               লেবেলটা শুধু OUT চাপা হয়েছে কিনা দেখত, IN-এর তথ্য ঠিকঠাক এল
               কিনা মেলাত না। এখন IN-এর তথ্য সত্যিই পড়া যাচ্ছে কিনা সেটাও
               শর্তে যোগ হলো — না মিললে সৎভাবে "INCOMPLETE DATA" দেখাবে। */
            val startedValid = started.isNotBlank() && parseIso(started) > 0L
            val endedValid = ended.isNotBlank() && parseIso(ended) > 0L
            val status = when {
                noGps -> "NO GPS"
                ended.isBlank() && date == today() -> "RUNNING"
                ended.isBlank() -> "NOT CLOSED"
                !startedValid -> "INCOMPLETE DATA"
                auto -> "AUTO CLOSED"
                else -> "COMPLETE"
            }
            val colour = when (status) {
                "RUNNING" -> "#0B7A4B"
                "AUTO CLOSED" -> "#8A5A00"
                "NO GPS" -> "#8A5A00"
                "INCOMPLETE DATA" -> "#8A5A00"
                "NOT CLOSED" -> "#B42318"
                else -> "#0B7A4B"
            }
            /* 🎨🔒 V1349 (১১.০৯.২০২৬, TK-অনুমোদিত ফটো-প্রুফ) — TK: *"complete
               লেখা থাকবে না তারিখের পাশে In time Out Time থাকবে কোন লেখা
               যেন ব্রেক না হয়"*। আগে এই লাইনে "COMPLETE"/"AUTO CLOSED"/
               "INCOMPLETE DATA" ইত্যাদি শব্দ থাকত — এখন শব্দটা বাদ, বদলে
               আসল IN/OUT সময় সরাসরি তারিখের পাশে, একই লাইনে (single-line,
               কখনো একাধিক শব্দ জোড়া লেগে অক্ষর-ভাঙা দেখাবে না)। ⛔ রং
               (`colour`) আগের মতোই — লাল/অ্যাম্বার/সবুজ দিয়েই সমস্যার দিন
               বোঝা যায়, নিচের ব্যাখ্যা-লাইনগুলো (auto/noGps/zeroDistance)
               এক অক্ষরও বদলায়নি, তাই তথ্য হারায়নি। NOT CLOSED-এর দিনও লাল
               রঙেই আলাদা বোঝা যাবে। */
            card.addView(TextView(this).apply {
                text = if (noGps) dmy(date) else
                    dmy(date) + "   ·   IN " + (if (startedValid) timeOf(started) else "-") +
                        "   ·   OUT " + (if (endedValid) timeOf(ended) else "-")
                textSize = 13.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor(colour))
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            val hrs = hoursBetween(started, ended)
            card.addView(ModuleUi.body(this,
                "Hours " + hrs + "   ·   Distance " + FieldVisit.kmText(meters) +
                    "   ·   Doctors " + (docsPerDay[date] ?: 0)))
            if (auto) card.addView(ModuleUi.body(this, "OUT TIME not marked - closed by app at 12:00 AM"))
            if (noGps) card.addView(ModuleUi.body(this,
                "Location was off on the phone - only the doctor visits were recorded"))
            /* 📍🔒 V1333 (TK-নির্দেশ: "distance কত ঘুরেছে দেখতে চাই") — আসল
               কারণ (V1156-এ কোডে সারানো): লোকেশন-অনুমতি না থাকলে GPS একটাও
               অবস্থান দেয় না, তাই দূরত্ব চিরকাল ০.০ থাকে। পুরনো (এই ফিক্সের
               আগের) দিনগুলোর সারিতে এখনো এটাই দেখা যাবে, তাই এখানেই সৎভাবে
               কারণটা বলা হলো — কোনো টাকা/হিসাব বদলায়নি, শুধু ব্যাখ্যা। */
            val lat = r.optDouble("last_lat", Double.NaN)
            val lng = r.optDouble("last_lng", Double.NaN)
            val hasPoint = !lat.isNaN() && !lng.isNaN() && (lat != 0.0 || lng != 0.0)
            val accM = r.optInt("last_acc_m", 0)
            /* 📍 V1431 — অবস্থান আছে অথচ দূরত্ব ০ ⇒ অনুমতির দোষ নয়, নির্ভুল GPS পায়নি (ঘরের ভিতরে);
               অবস্থানই নেই তবেই আগের বার্তা। */
            val zeroDistanceUnexplained = !noGps && endedValid && startedValid && meters <= 0.0
            if (zeroDistanceUnexplained) card.addView(ModuleUi.body(this,
                if (hasPoint) "No accurate GPS fix (indoors?) - distance not counted"
                else "⚠ Location permission may have been off - distance not recorded"))
            if (hasPoint) {
                val seen = timeOf(r.optString("last_seen_at", ""))
                card.addView(ModuleUi.body(this,
                    "Last seen " + (if (seen.isBlank()) "-" else seen) +
                        "  ·  accuracy ±" + accM + " m" + (if (accM > 60) " (approx.)" else "")))
                /* 🗺️🔒 V1452 — route_points থাকলে (নতুন দিনের সারি) পুরো দিনের
                   রুট Google Maps-কে দিয়ে আঁকানো হয়; পুরনো দিনের সারিতে এই
                   কলাম না থাকলে আগের মতোই শুধু শেষ বিন্দু খোলে। */
                val routePts = r.optJSONArray("route_points")
                /* 🗺️🔒 V1500 — আজকের চলমান দিনেই শুধু বাড়তি লাইভ মানচিত্র;
                   পুরনো দিনে আগের মতোই শুধু বোতাম। */
                if (status == "RUNNING") card.addView(buildLiveMap(lat, lng, routePts))
                card.addView(ModuleUi.buttonSoft(this, "OPEN IN GOOGLE MAPS") {
                    if (routePts != null && routePts.length() >= 2) openRoute(routePts) else openMap(lat, lng)
                })
            }
            col.addView(card)
        }
    }

    private fun hoursBetween(a: String, b: String): String {
        return try {
            if (a.isBlank()) return "-"
            val s = parseIso(a)
            if (s <= 0L) return "-"
            val e = if (b.isBlank()) System.currentTimeMillis() else parseIso(b)
            if (e <= 0L) return "-"
            FieldVisit.hoursText(s, e)
        } catch (_: Throwable) { "-" }
    }

    /** 🗺️ V1500 — আজকের চলমান দিনের জন্য একটাই লাইভ মানচিত্র (MapView) বানায়, রেফারেন্স জমা রাখে
     *  যাতে পরের প্রতিটা আপডেটে পুরো পর্দা আবার না এঁকে শুধু বিন্দু/পথ সরানো যায়। */
    private fun buildLiveMap(lat: Double, lng: Double, routePts: JSONArray?): MapView {
        val map = MapView(this).apply {
            setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, ModuleUi.dp(this@FieldVisitActivity, 200)
            ).apply { topMargin = ModuleUi.dp(this@FieldVisitActivity, 6) }
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(lat, lng))
        }
        val marker = Marker(map).apply {
            position = GeoPoint(lat, lng)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Now"
        }
        map.overlays.add(marker)
        val poly = Polyline(map)
        routePointsToGeo(routePts)?.let { poly.setPoints(it) }
        map.overlays.add(poly)
        map.invalidate()
        liveMapView = map
        liveMarker = marker
        livePolyline = poly
        liveHandler.removeCallbacks(liveTick)
        liveHandler.postDelayed(liveTick, LIVE_REFRESH_MS)
        return map
    }

    private fun routePointsToGeo(pts: JSONArray?): List<GeoPoint>? {
        if (pts == null || pts.length() < 2) return null
        val out = ArrayList<GeoPoint>(pts.length())
        for (i in 0 until pts.length()) {
            val p = pts.optJSONArray(i) ?: continue
            out.add(GeoPoint(p.optDouble(0, 0.0), p.optDouble(1, 0.0)))
        }
        return if (out.size >= 2) out else null
    }

    /** ৪৫ সেকেন্ড পরপর — শুধু আজকের সারিটাই হালকা করে পড়ে, পুরো পর্দা আবার আঁকা হয় না। */
    private fun refreshLiveMap() {
        if (liveMapView == null) return
        Thread {
            try {
                val rows = ModuleAuth.getRows(
                    "wn", "field_visit_days",
                    "select=last_lat,last_lng,route_points&staff_code=eq." + Uri.encode(staffCode) +
                        "&work_date=eq." + today()
                )
                val r = rows.optJSONObject(0) ?: return@Thread
                val lat = r.optDouble("last_lat", Double.NaN)
                val lng = r.optDouble("last_lng", Double.NaN)
                if (lat.isNaN() || lng.isNaN()) return@Thread
                val pts = r.optJSONArray("route_points")
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    updateLiveMap(lat, lng, pts)
                }
            } catch (_: Throwable) { }
        }.start()
    }

    private fun updateLiveMap(lat: Double, lng: Double, routePts: JSONArray?) {
        val map = liveMapView ?: return
        val marker = liveMarker ?: return
        val poly = livePolyline ?: return
        marker.position = GeoPoint(lat, lng)
        routePointsToGeo(routePts)?.let { poly.setPoints(it) }
        map.controller.animateTo(GeoPoint(lat, lng))
        map.invalidate()
    }

    private fun openMap(lat: Double, lng: Double) {
        try {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Throwable) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")))
            } catch (_: Throwable) { ModuleUi.toast(this, "No map app found") }
        }
    }

    /* 🗺️🔒 V1452 (১৪.০৯.২০২৬, TK-নির্দেশ) — দিনের জমানো বিন্দুগুলো (প্রতিটা
       [lat,lng]) দিয়ে Google Maps-এর "dir" রুট-লিংক বানানো — প্রথমটা origin,
       শেষটা destination, মাঝেরগুলো waypoints। Google-এর নিজস্ব সীমা (২৫
       বিন্দু) যাচাই করে দেখা — আমরা দিনে সর্বোচ্চ ২০টাই জমাই, তাই সবসময়
       সীমার ভিতরে। */
    private fun openRoute(pts: JSONArray) {
        try {
            fun ll(i: Int): String {
                val p = pts.optJSONArray(i)
                return (p?.optDouble(0) ?: 0.0).toString() + "," + (p?.optDouble(1) ?: 0.0).toString()
            }
            val origin = ll(0)
            val dest = ll(pts.length() - 1)
            val mid = (1 until pts.length() - 1).joinToString("|") { ll(it) }
            var url = "https://www.google.com/maps/dir/?api=1&origin=$origin&destination=$dest&travelmode=driving"
            if (mid.isNotBlank()) url += "&waypoints=" + Uri.encode(mid)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Throwable) {
            ModuleUi.toast(this, "No map app found")
        }
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14.5f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(android.graphics.Color.parseColor("#101C2E"))
    }

    private fun markVisit(name: String, mobile: String, area: String) {
        if (mobile.isBlank()) { ModuleUi.toast(this, "This doctor has no mobile number"); return }
        val row = JSONObject()
            .put("staff_code", staffCode)
            .put("work_date", today())
            .put("branch", branch)
            .put("doctor_name", name)
            .put("doctor_mobile", mobile)
            .put("area", area)
            .put("visited_at", FieldVisit.isoNow())
        Thread {
            val ok = ModuleAuth.insert("wn", "doctor_visits", row)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (ok) { ModuleUi.toast(this, "Visit marked"); load() }
                else ModuleUi.toast(this, "Could not save - check internet")
            }
        }.start()
    }

    companion object {
        const val EXTRA_OWNER = "owner_mode"
        const val EXTRA_STAFF_CODE = "staff_code"
        const val EXTRA_STAFF_MOBILE = "staff_mobile"
        /** 🗺️ V1500 — লাইভ মানচিত্র কত ঘনঘন হালনাগাদ হবে। */
        private const val LIVE_REFRESH_MS = 45_000L
    }
}
