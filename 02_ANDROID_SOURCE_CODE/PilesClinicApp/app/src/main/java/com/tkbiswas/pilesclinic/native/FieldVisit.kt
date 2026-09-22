package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.location.Location
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 🏍️🔒 V968 (০২.০৯.২০২৬, TK-এর চূড়ান্ত নির্দেশ) — **ফিল্ড ভিজিট গোনা।**
 *
 * TK: *"সে যখন যায় বাইক নিয়ে যায় … তাকে কিলোমিটার প্রতি টাকা দিতে হয় … আমি
 * চাইছি তার GPS অন রেখে, IN TIME যখন করবে তখন থেকে GPS অটোমেটিক অন থাকবে,
 * যতক্ষণ না সে OUT TIME চাপবে … দিনের কোন নির্দিষ্ট টাইমে আমি যেন দেখতে পাই
 * সে কোথায় আছে এখন।"*
 *
 * ─── লক করা নিয়ম ──────────────────────────────────────────────────────────
 *  • **বাইরে ঘোরা স্টাফ** — বর্তমানে RUPAM (JPE-RUPAM) ও ARMAN
 *    (COB-ARMAN)। তাঁদের IN TIME চাপলেই distance + route tracking automatic;
 *    আলাদা Field Visit নির্বাচন লাগে না (V1516)।
 *  • না চাপলে অ্যাপ **নিজে থেকে ছুটি বসায় না** — TK: *"আমার পর্দায় লাল
 *    দেখাবে, ছুটির সিদ্ধান্ত আমি নেব"*।
 *  • রাত ৯টার পর বারবার মনে করানো, রাত ১২টায় অ্যাপ নিজেই বন্ধ করে দেয়
 *    (`auto_closed` চিহ্ন বসে, যাতে TK বুঝতে পারেন স্টাফ নিজে চাপেনি)।
 *  • দিনে একজনের **একটাই সারি** বারবার update হয়; route সর্বোচ্চ ২০টি
 *    sampled point-এ সীমাবদ্ধ, তাই Free-plan footprint ছোট থাকে।
 *
 * ⚠️ **সীমাবদ্ধতা (সৎভাবে):** নকল-অবস্থান অ্যাপ দিয়ে ফাঁকি পুরোপুরি ঠেকানো
 *    যায় না (`ClinicPresence`-এর একই সীমা), আর GPS-এর কিলোমিটার বাইকের
 *    মিটারের সাথে হুবহু মিলবে না।
 */
object FieldVisit {

    /* 🔒 TK-নির্দেশ (মূল, ০২.০৯.২০২৬): *"এটা যেন শুধুমাত্র RUPAM-এর ক্ষেত্রেই
       হয়ে থাকে"*। এই তালিকাটা এখনো শুধু **RMP Doctors — MARK VISIT** ফিচারের
       জন্যই (বাইক নিয়ে ঘোরা, কিলোমিটার-হিসাবের টাকা) — এটা বদলায়নি।
       ⛔ সাধারণ হাজিরা-লোকেশন (নিচের `tracksAttendanceLocation()`, V1346-এ
          TK-নির্দেশে সবার জন্য চালু হলো) সম্পূর্ণ **আলাদা** শর্ত — এই
          তালিকার সাথে গুলিয়ে ফেলা যাবে না।
       🟢🔒 V1377 (১২.০৯.২০২৬, TK-নির্দেশ "RUPAM যা ছিল তাই থাক") — কোচবিহারের
          নতুন স্টাফ ARMAN HOQUE-ও এখন এই একই তালিকায়, RUPAM-এর হুবহু একই
          ভূমিকায়। */
    private val FIELD_STAFF_MOBILES = setOf("8167096595", "9883884394")

    /* 🛰️🔒 V1346 (১১.০৯.২০২৬, TK-নির্দেশ, ধাপে-ধাপে আলোচনা — "staff/branch/
       doctor সব"-এর ফোনে IN TIME থেকে OUT TIME পর্যন্ত লোকেশন) — উপরের
       `isFieldStaff` (শুধু RUPAM, RMP-ভিজিট-মার্কের জন্য) থেকে **সম্পূর্ণ
       আলাদা**, বিস্তৃত শর্ত। TK-র নিজের সিদ্ধান্ত: IN TIME চাপলেই লোকেশন-অন
       করার প্রম্পট আসবে (অ-বাধ্যতামূলক — না দিলে IN TIME আটকাবে না)।
       ⛔ `RoleRules.usesAttendance()`-ই ব্যবহার করা হলো, নতুন কোনো তালিকা
          বানানো হয়নি — WorkNotebookActivity (যেখান থেকে এই ফাংশন ডাকা হয়)
          এমনিতেই শুধু আসল `staff` রোলের (branch-অ্যাকাউন্টসহ) জন্য খোলে,
          তাই এটা কার্যকরভাবে "staff + branch" দুটোই কভার করে। ডাক্তারের
          জন্য এই একই পথ চলে না — ডাক্তারের কোনো IN/OUT TIME-ই নেই বলে
          (TK-র নিজের সিদ্ধান্ত), তাঁর জন্য আলাদা, হালকা ব্যবস্থা
          (`DoctorLocationWorker.kt` দেখুন) — persistent notification ছাড়া। */
    fun tracksAttendanceLocation(context: Context): Boolean =
        try { RoleRules.usesAttendance(context) } catch (_: Throwable) { false }

    const val MODE_CHAMBER = "CHAMBER"
    const val MODE_FIELD = "FIELD"

    private const val PREF = "piles_field_visit"

    /** সবচেয়ে অনিশ্চিত যে অবস্থান গ্রহণ করা হবে (মিটার)। */
    /* 🏍️🔒 V1471 (১৪.০৯.২০২৬, TK: "৩০ কিমি ঘুরেছে, অ্যাপ ০.১ কিমি দেখাচ্ছে —
       ভুল কোথায় গভীরে যাচাই করুন") — RUPAM-এর আজকের সারিতে route_points-ও
       ১টার বেশি বসেনি, মানে সারাদিনে ৬০ মিটারের-ভিতরের নির্ভুল ফিক্স প্রায়
       একটা জায়গাতেই আটকে ছিল। বাইক চলার সময় ফোন পকেটে থাকলে GPS প্রায়ই
       ৬০ মিটারের চেয়ে কম নিখুঁত (কিন্তু তবুও বাস্তব, শুধু একটু আনুমানিক)
       হয় — আগের কড়া ৬০ মিটার সীমায় সেই বাস্তব চলাচলটাই বাদ পড়ে যাচ্ছিল
       (কোনো হিসাব-ভুল নয়, সীমাটাই বাস্তবের সাথে বড্ড কড়া ছিল)। TK-কে
       সুবিধা-অসুবিধা জানিয়ে অনুমতি নিয়ে (১৪.০৯.২০২৬) ১৫০ মিটারে বাড়ানো
       হলো — এখনো দাঁড়িয়ে-থাকলে সেটা MIN_STEP_M-এই বাদ পড়বে, শুধু আসল
       চলাচল আর অন্যায্যভাবে বাদ যাবে না। */
    private const val MAX_ACCURACY_M = 150f
    /* 📍🔒 V1431 (১৩.০৯.২০২৬, TK: "হ্যাঁ, করুন", তালিকা ৫৪৫) — ঘরের ভিতরে GPS লক হয় না,
       ফোন শুধু ১০০–১০০০ মিটারের আনুমানিক অবস্থান দেয়; আগে ৬০ মিটারের চেয়ে খারাপ সব বাদ
       যেত ⇒ RUPAM চেম্বারে বসে থাকলে "অবস্থান নেই"। এখন ১৫০০ মিটার পর্যন্ত আনুমানিক অবস্থানও
       **"Last seen"-এর জন্য** রাখা হয় (±মিটার সহ, Master-এর পর্দায় "approx." লেখা ওঠে);
       কিন্তু **কিলোমিটার শুধু MAX_ACCURACY_M-এর ভিতরের নির্ভুল অবস্থান থেকেই** — আলাদা নোঙর
       (acc_lat/acc_lng), যাতে আনুমানিক বিন্দু থেকে লাফ মেপে দূরত্ব ভুল না বাড়ে। */
    private const val COARSE_MAX_ACCURACY_M = 1500f
    /** এর কম সরলে ধরা হয় না — GPS-এর নিজের কাঁপুনিতে কিমি বেড়ে যাওয়া ঠেকায়। */
    private const val MIN_STEP_M = 20f
    /* 🏍️🔒 V1471 — MAX_ACCURACY_M ঢিলা হওয়ায় নির্ভুল ফিক্স আরও ঘনঘন বসবে
       (তাই দুটো নোঙরের মাঝের ফাঁকও সাধারণত ছোট হবে), কিন্তু মাঝে কয়েক
       মিনিট সিগন্যাল না পেলে বাইকে সত্যিকারের কয়েক কিমি ফাঁকও হতে পারে —
       আগের ৩০০০ মিটার সীমায় সেটাও "লাফ-ভুল" ধরে বাদ পড়ত। ৮০০০ মিটারে
       বাড়ানো হলো (GPS-এর সত্যিকারের ভুল-লাফ সাধারণত এর চেয়ে অনেক বড়/
       অবাস্তব দূরত্বে হয়, তাই সেই সুরক্ষা অটুট)। */
    /** এক লাফে এর বেশি হলে ধরা হয় না — লাফিয়ে-যাওয়া ভুল অবস্থান বাদ। */
    private const val MAX_STEP_M = 8000f

    /* 🛰️🔒 V1156 (০৭.০৯.২০২৬, TK: *"১৩৯ Field Visit Tracking এবার ঠিক করুন"*)।
       **আসল কারণ (কোডে মেপে পাওয়া, আন্দাজ নয়):** Location-এর অনুমতি অ্যাপ
       চাইত **শুধু ক্লিনিকে-আছেন-কিনা যাচাইয়ের সময়** (`startInTimeFlow`)।
       কিন্তু বাইরে ঘোরা স্টাফ (RUPAM) TK-এর নিজেরই নিয়মে **ওই যাচাই এড়িয়ে
       যান** (V650, ২৫.০৮.২০২৬ — মাঠে থাকলে ক্লিনিকের গণ্ডিতে থাকবেন না)।
       ⇒ তাঁর ফোনে অনুমতির বাক্স **কোনোদিন ওঠেই না** ⇒ GPS-সেবা চুপচাপ ফিরে
         যায় ⇒ একটাও অবস্থান আসে না ⇒ কিলোমিটার চিরকাল ০.০।
       ⛔ এই দুটো ফাংশনই সেই ফাঁক ধরার জন্য — কিছু বদলায় না, শুধু সত্যি বলে। */
    /* 🛰️🔒 V1344 (১১.০৯.২০২৬, TK-নির্দেশ, RUPAM-এর নতুন APK-তেও কিমি ০.০) —
       **আসল কারণ কোডে ধরা:** এই ফাংশন আগে fine-অথবা-coarse যেকোনো একটা
       অনুমতি পেলেই "আছে" বলত, তাই "Approximate" (আনুমানিক) বেছে নিলেও
       এখানে সন্তুষ্ট হয়ে যেত ও আর দ্বিতীয়বার জিজ্ঞাসা করত না। কিন্তু
       `FieldVisitService.hasPermission()` (আসল GPS চালু করার দরজা) শুধু
       **Precise/fine** অনুমতি ছাড়া চলেই না — তাই "Location allowed - km
       will now be counted" বলার পরেও সেবা ভিতরে চুপচাপ কিছুই গুনত না।
       ⇒ এখন এখানেও শুধু fine — দুটো জায়গার নিয়ম এক হলো, মিথ্যা "হয়ে গেছে"
       বার্তা বন্ধ। ⛔ কল-সাইট মাত্র একটা (WorkNotebookActivity.ensureField
       LocationReady) — যাচাই করা, বাকি কোথাও এই ফাংশন ব্যবহার হয় না। */
    fun hasLocationPermission(context: Context): Boolean = try {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    /** ⛔ শুধু বার্তা ঠিকভাবে বাছার জন্য — "Approximate"-ই দেওয়া হয়েছে কিনা। */
    fun hasApproxOnlyLocationPermission(context: Context): Boolean = try {
        !hasLocationPermission(context) &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    /** ফোনের Location সুইচটা চালু আছে কি না (অনুমতি থাকলেও বন্ধ থাকতে পারে)। */
    fun isLocationOn(context: Context): Boolean = try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    } catch (_: Throwable) { false }

    fun isFieldStaff(mobile: String?): Boolean =
        FIELD_STAFF_MOBILES.contains(StaffDirectory.normalizeMobile(mobile.orEmpty()))

    fun isFieldStaff(context: Context): Boolean =
        isFieldStaff(try { NativeSession.current(context)?.mobile } catch (_: Throwable) { null })

    /** স্টাফ-কোড (যেমন "JPE-RUPAM") ধরে — Master-এর পর্দায় বোতাম দেখানোর জন্য। */
    fun isFieldStaffCode(code: String?): Boolean {
        val c = code.orEmpty().trim()
        if (c.isBlank()) return false
        return StaffDirectory.allAccounts().any {
            it.name.equals(c, ignoreCase = true) && isFieldStaff(it.mobile)
        }
    }

    /** ওই কোডের স্টাফের নম্বর (না পেলে ফাঁকা)। */
    fun mobileForCode(code: String?): String {
        val c = code.orEmpty().trim()
        return StaffDirectory.allAccounts().firstOrNull { it.name.equals(c, ignoreCase = true) }?.mobile.orEmpty()
    }

    /* 👨‍⚕️🔒 V1334 (১১.০৯.২০২৬, TK-নির্দেশ "খ" — তালিকা সারি ৪৩৪): TK-নিজে
       বেছে দিয়েছেন — "শুধু MARK VISIT বোতাম চেপে গোনা ডাক্তারই আসল সংখ্যা"।
       WhatsApp-এর Daily Report-এ ফিল্ড-স্টাফের জন্য এই সংখ্যাটাই বসবে
       (`WorkNotebookActivity.docVisitLine()`), পুরনো call-history-ভিত্তিক
       গোনা (`DoctorVisitDayCount`, RMP-কে ফোন করা অন্য স্টাফদের জন্য) নয়।
       ⛔ এটা নেটওয়ার্ক-কল — মূল থ্রেডে ডাকা যাবে না (V1032-এর নিয়ম মেনে
          আলাদা থ্রেডে)। ⛔ RMP ডিরেক্টরি/MARK VISIT-এর নিজের কোনো লজিক
          বদলায়নি — শুধু পড়া হচ্ছে। */
    fun todayMarkVisitCount(staffCode: String): Int = try {
        val code = staffCode.trim()
        if (code.isBlank()) 0 else {
            val q = "select=doctor_mobile&staff_code=eq." +
                java.net.URLEncoder.encode(code, "UTF-8") + "&work_date=eq." + todayIso()
            com.tkbiswas.pilesclinic.modules.ModuleAuth.getRows("wn", "doctor_visits", q).length()
        }
    } catch (_: Throwable) { 0 }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }.format(java.util.Date())

    /** এখনকার সময়, ভারতের offset সহ — ডেটাবেসে পাঠানোর জন্য। */
    fun isoNow(): String = iso(System.currentTimeMillis())

    private fun iso(ms: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }.format(java.util.Date(ms))

    // ─── আজকের অবস্থা ────────────────────────────────────────────────────
    fun runningDate(context: Context): String = prefs(context).getString("run_date", "").orEmpty()
    fun isRunning(context: Context): Boolean = runningDate(context).isNotBlank()
    fun startedAt(context: Context): Long = prefs(context).getLong("started_at", 0L)
    fun distanceMeters(context: Context): Double =
        java.lang.Double.longBitsToDouble(prefs(context).getLong("dist_m", 0L))
    fun lastSeenAt(context: Context): Long = prefs(context).getLong("seen_at", 0L)
    /** ⚠️ V1032 — আজ একটাও অবস্থান পাওয়া গেছে কি না। না পেলে কিলোমিটার
     *  গোনাই শুরু হয় না, তাই স্টাফের পর্দায় লাল সতর্কতা দেখাতে এটা লাগে। */
    fun hasFix(context: Context): Boolean = prefs(context).getBoolean("has_fix", false)
    fun lastLat(context: Context): Double =
        java.lang.Double.longBitsToDouble(prefs(context).getLong("last_lat", 0L))
    fun lastLng(context: Context): Double =
        java.lang.Double.longBitsToDouble(prefs(context).getLong("last_lng", 0L))

    /* 🗺️🔒 V1452 (১৪.০৯.২০২৬, TK-নির্দেশ, তালিকা ৫৭২ — "বিগত দিনে সে কোথায়
       কোথায় ঘুরেছে তা যেন দেখতে পাই") — আগে শুধু দিনের **শেষ** অবস্থান জমা
       থাকত (route রাখা হত না — TK-এরই আগের সিদ্ধান্ত, ফ্রি প্ল্যানের ঝুঁকিতে,
       ০২.০৯.২০২৬)। TK-কে সুবিধা-অসুবিধা জানানোর পর তিনি নিজেই বেছেছেন:
       "Google Maps রুট" পদ্ধতি — বাড়তি কোনো মানচিত্র-সেবা/খরচ ছাড়াই, দিনে
       সর্বোচ্চ ROUTE_MAX_POINTS-টা থামার বিন্দু জমিয়ে Google Maps-কেই
       বাকি রাস্তা আঁকতে দেওয়া হয় (ঠিক TK-এরই পাঠানো স্ক্রিনশটের মতো)।
       ⛔ প্রতিটা GPS-ফিক্সে বিন্দু জমানো হয় না — নইলে দিনে শত শত বিন্দু জমে
          Google Maps-এর লিংক ভেঙে যেত (২৫-বিন্দুর সীমা, যাচাই করা)। তাই
          আগেরটা থেকে অন্তত ROUTE_STEP_M দূরে গেলে তবেই নতুন বিন্দু, আর
          ক্যাপে পৌঁছালে আর নতুন বিন্দু জমে না (দিনের প্রথম অংশটাই থাকে)।
       ⛔ পুরনো দিনের সারিতে এই কলাম ফাঁকা — সেখানে আগের মতোই শুধু শেষ
          অবস্থান দেখানো হবে (FieldVisitActivity/profile.js উভয়েই)। */
    private const val ROUTE_STEP_M = 800f
    private const val ROUTE_MAX_POINTS = 20

    fun routePointsJson(context: Context): String =
        prefs(context).getString("route_pts", "[]").orEmpty().ifBlank { "[]" }

    private fun addRoutePoint(context: Context, lat: Double, lng: Double) {
        try {
            var arr = org.json.JSONArray(routePointsJson(context))
            if (arr.length() > 0) {
                val last = arr.optJSONArray(arr.length() - 1)
                if (last != null) {
                    val prev = Location("prev").apply { latitude = last.optDouble(0); longitude = last.optDouble(1) }
                    val here = Location("here").apply { latitude = lat; longitude = lng }
                    if (prev.distanceTo(here) < ROUTE_STEP_M) return
                }
            }
            /* 🗺️🔒 V1516 — 20 point পূর্ণ হলে আগের কোড নতুন point চিরতরে
               নেওয়া বন্ধ করত; 20-30 km দিনের শেষার্ধ তাই map-এ হারিয়ে যেত।
               Cloud/Google limit 20-ই রাখা হলো, শুধু পুরনো points অর্ধেক করে
               ছড়িয়ে রেখে নতুন point-এর জায়গা বানাই। ফলে first→last পুরো
               দিনের coverage থাকে; DB row/Free-plan footprint বাড়ে না। */
            if (arr.length() >= ROUTE_MAX_POINTS) {
                val compact = org.json.JSONArray()
                for (i in 0 until arr.length()) {
                    if (i == 0 || i == arr.length() - 1 || i % 2 == 0) {
                        arr.optJSONArray(i)?.let { compact.put(it) }
                    }
                }
                arr = compact
            }
            arr.put(org.json.JSONArray().put(lat).put(lng))
            prefs(context).edit().putString("route_pts", arr.toString()).apply()
        } catch (_: Throwable) { }
    }

    /** এই ফোনে আজ কোনটা বাছা হয়েছিল — CHAMBER / FIELD / ফাঁকা। */
    fun chosenMode(context: Context): String {
        val p = prefs(context)
        return if (p.getString("mode_date", "") == todayIso()) p.getString("mode", "").orEmpty() else ""
    }

    fun chooseMode(context: Context, mode: String) {
        prefs(context).edit().putString("mode_date", todayIso()).putString("mode", mode).apply()
    }

    /** IN TIME-এর পরে ডাকা হয় — দিনের গোনা শুরু। */
    fun startDay(context: Context, staffCode: String, branch: String) {
        /* 🏍️🔒 V1516 (17.09.2026, TK-নির্দেশ) — Field Visit আর আলাদা করে
           বাছতে হবে না। RUPAM/ARMAN IN TIME চাপলেই তাঁদের field tracking
           স্বয়ংক্রিয়ভাবে সক্রিয় হবে; অন্য staff-এর attendance location
           চলবে, কিন্তু bike-distance/route গোনা হবে না। staffCode-ও দেখা হয়
           যাতে foreground service পরে session object না পেলেও নিয়ম না ভাঙে। */
        val autoField = isFieldStaff(context) || isFieldStaffCode(staffCode)
        prefs(context).edit()
            .putString("run_date", todayIso())
            .putString("staff_code", staffCode)
            .putString("branch", branch)
            .putString("mode_date", todayIso())
            .putString("mode", if (autoField) MODE_FIELD else MODE_CHAMBER)
            .putLong("started_at", System.currentTimeMillis())
            .putLong("dist_m", java.lang.Double.doubleToRawLongBits(0.0))
            .putLong("seen_at", 0L)
            .putLong("last_lat", 0L).putLong("last_lng", 0L)
            .putInt("last_acc", 0)
            .putBoolean("has_fix", false)
            .putLong("acc_lat", 0L).putLong("acc_lng", 0L).putBoolean("has_acc_fix", false)   // V1431
            .putString("route_pts", "[]")   // V1452
            .apply()
    }

    /** OUT TIME (auto=false) বা রাত ১২টা (auto=true) — গোনা শেষ। */
    fun endDay(context: Context, auto: Boolean) {
        prefs(context).edit()
            .putString("end_date", runningDate(context).ifBlank { todayIso() })
            .putLong("ended_at", System.currentTimeMillis())
            .putBoolean("auto_closed", auto)
            .putString("run_date", "")
            .apply()
    }

    fun staffCode(context: Context): String = prefs(context).getString("staff_code", "").orEmpty()
    fun branch(context: Context): String = prefs(context).getString("branch", "").orEmpty()

    /**
     * নতুন একটা অবস্থান এলে — দূরত্ব জমা করা।
     * ⛔ অনিশ্চিত · খুব ছোট · লাফিয়ে-যাওয়া মাপ গোনায় ঢোকে না।
     */
    /* 🚨🔒 V1497 (১৫.০৯.২০২৬, TK-রিপোর্ট — JPE-CRP সারাদিন এক চেম্বারেই বসে,
       অথচ অ্যাপ "Distance 47.4 km" দেখাচ্ছিল) — **এটা আমারই কালকের V1471-এর
       ভুল।** V1471-এ নির্ভুলতার সীমা ৬০→১৫০ মিটার আর এক-লাফের সীমা ৩০০০→৮০০০
       মিটার ঢিলা করা হয়েছিল যাতে RUPAM-এর মতো বাইক-স্টাফের আসল চলাচল বাদ না
       পড়ে — কিন্তু এই ঢিলা সীমা **সব স্টাফের** (এমনকি যারা কখনো বাইরে যানই
       না তাদেরও) GPS-এ প্রয়োগ হচ্ছিল। ঘরের ভিতরের স্বাভাবিক GPS কাঁপুনি
       (multipath) এখন "নির্ভুল" (≤১৫০মি) ধরা পড়ে যাচ্ছিল, আর প্রতিটা ছোট
       কাঁপুনি-লাফ (২০মি–৮কিমি) যোগ হয়ে ঘণ্টার পর ঘণ্টায় বড় মিথ্যা সংখ্যা
       তৈরি করছিল — একজন স্টাফকে অন্যায় সন্দেহে ফেলার মতো গুরুতর ঝুঁকি।
       **সমাধান:** কিলোমিটার শুধু তখনই জমা হবে যখন আজকের জন্য সত্যিই "Field
       Visit" বাছা হয়েছে (`chosenMode == MODE_FIELD`) — এটা শুধু ২ জন
       নির্দিষ্ট বাইক-স্টাফের (RUPAM/ARMAN) পর্দাতেই বাছার সুযোগ আছে, আর
       ডিফল্ট সবসময় "At Chamber"। বাকি সবার (JPE-CRP-সহ) জন্য chosenMode
       কখনো MODE_FIELD হয়ই না, তাই কিলোমিটার এখন থেকে চিরকাল ০.০ — সঠিক,
       কারণ তাদের বাইরে ঘোরার কথাই নয়। ⛔ "Last seen" অবস্থান (Master-এর
       পর্দায় কোথায় আছেন দেখার জন্য) আগের মতোই কাজ করবে — শুধু কিলোমিটার
       জমা হওয়া বন্ধ হলো। RUPAM/ARMAN নিজে "Field Visit" বাছলে তাঁদের
       বেলায় কিছুই বদলায়নি (V1471-এর ঢিলা সীমাই এখনো প্রযোজ্য)। */
    fun onLocation(context: Context, loc: Location) {
        try {
            if (!isRunning(context)) return
            val acc = if (loc.hasAccuracy()) loc.accuracy else 0f
            if (acc > COARSE_MAX_ACCURACY_M) return   // V1431 — এর চেয়ে খারাপ হলে কিছুই নয়
            if (isMock(loc)) return
            val p = prefs(context)
            val e = p.edit()
            /* 🏍️🔒 V1516 — পুরনো manual `chosenMode == FIELD` শর্তটাই ছিল
               RUPAM/ARMAN-এর route 0 থাকার আসল কারণ: picker বহু আগেই UI থেকে
               বাদ গেছে, তাই mode FIELD আর সেটই হচ্ছিল না। এখন staff identity
               থেকেই স্বয়ংক্রিয় সিদ্ধান্ত — IN TIME-ই একমাত্র start action। */
            val fieldToday = isFieldStaffCode(staffCode(context)) || isFieldStaff(context)
            if (acc <= MAX_ACCURACY_M) {
                // নির্ভুল অবস্থান — কিলোমিটার এখান থেকেই (আগের হুবহু নিয়ম, নোঙর শুধু নির্ভুল বিন্দু)
                if (fieldToday && p.getBoolean("has_acc_fix", false)) {
                    val prev = Location("prev").apply {
                        latitude = java.lang.Double.longBitsToDouble(p.getLong("acc_lat", 0L))
                        longitude = java.lang.Double.longBitsToDouble(p.getLong("acc_lng", 0L))
                    }
                    val step = prev.distanceTo(loc)
                    if (step >= MIN_STEP_M && step <= MAX_STEP_M) {
                        val total = distanceMeters(context) + step
                        e.putLong("dist_m", java.lang.Double.doubleToRawLongBits(total))
                    } else if (step < MIN_STEP_M) {
                        // দাঁড়িয়ে আছেন — অবস্থান হালনাগাদ হবে, দূরত্ব নয়।
                    }
                }
                e.putLong("acc_lat", java.lang.Double.doubleToRawLongBits(loc.latitude))
                    .putLong("acc_lng", java.lang.Double.doubleToRawLongBits(loc.longitude))
                    .putBoolean("has_acc_fix", true)
                if (fieldToday) addRoutePoint(context, loc.latitude, loc.longitude)   // V1452 · V1497 শুধু আসল Field Visit-দিনেই রুট জমে
            } else if (p.getBoolean("has_acc_fix", false) && p.getInt("last_acc", 0) <= MAX_ACCURACY_M.toInt()
                && System.currentTimeMillis() - lastSeenAt(context) < 15 * 60_000L) {
                // V1431 — ১৫ মিনিটের মধ্যে নির্ভুল অবস্থান থাকলে সেটাই থাক; আনুমানিকটা তার উপরে লিখব না
                return
            }
            e.putLong("last_lat", java.lang.Double.doubleToRawLongBits(loc.latitude))
                .putLong("last_lng", java.lang.Double.doubleToRawLongBits(loc.longitude))
                .putInt("last_acc", if (loc.hasAccuracy()) loc.accuracy.toInt() else 0)
                .putLong("seen_at", System.currentTimeMillis())
                .putBoolean("has_fix", true)
                .apply()
        } catch (_: Throwable) { }
    }

    private fun isMock(loc: Location): Boolean = try {
        if (android.os.Build.VERSION.SDK_INT >= 31) loc.isMock
        else @Suppress("DEPRECATION") loc.isFromMockProvider
    } catch (_: Throwable) { false }

    /** ক্লাউডে আজকের সারিটা লেখা — দিনে একটাই সারি, বারবার হালনাগাদ। */
    fun push(context: Context, ended: Boolean, auto: Boolean): Boolean {
        return try {
            val p = prefs(context)
            val date = (if (ended) p.getString("end_date", "") else runningDate(context)).orEmpty()
                .ifBlank { todayIso() }
            val code = staffCode(context)
            if (code.isBlank()) return false
            val row = JSONObject()
                .put("staff_code", code)
                .put("work_date", date)
                .put("branch", branch(context))
                .put("distance_m", Math.round(distanceMeters(context)))
                .put("updated_at", iso(System.currentTimeMillis()))
            val st = startedAt(context)
            if (st > 0L) row.put("started_at", iso(st))
            if (p.getBoolean("has_fix", false)) {
                row.put("last_lat", lastLat(context))
                    .put("last_lng", lastLng(context))
                    .put("last_acc_m", p.getInt("last_acc", 0))
            }
            val seen = lastSeenAt(context)
            if (seen > 0L) row.put("last_seen_at", iso(seen))
            // 🗺️ V1452 — দিনের রুট-বিন্দুগুলো (থাকলে); অন্তত ২টা না হলে রাস্তা আঁকা যায় না।
            try {
                val pts = org.json.JSONArray(routePointsJson(context))
                if (pts.length() >= 2) row.put("route_points", pts)
            } catch (_: Throwable) { }
            if (ended) {
                row.put("ended_at", iso(p.getLong("ended_at", System.currentTimeMillis())))
                row.put("auto_closed", auto)
            }
            com.tkbiswas.pilesclinic.modules.ModuleAuth.attachContext(context)
            com.tkbiswas.pilesclinic.modules.ModuleAuth
                .upsertOnConflict("wn", "field_visit_days", row, "staff_code,work_date")
        } catch (_: Throwable) { false }
    }

    /** রাত ৯টা পার হয়েছে কি না — তখন থেকেই OUT TIME মনে করানো শুরু। */
    fun pastReminderHour(): Boolean = hourNow() >= 21

    /** গোনা শুরুর দিনটা পেরিয়ে গেছে কি না — রাত ১২টায় নিজে বন্ধ করার জন্য। */
    fun pastMidnight(context: Context): Boolean {
        val started = runningDate(context)
        return started.isNotBlank() && started != todayIso()
    }

    private fun hourNow(): Int {
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return c.get(Calendar.HOUR_OF_DAY)
    }

    /** "21.4 km" — পর্দায় দেখানোর জন্য। */
    fun kmText(meters: Double): String =
        String.format(Locale.US, "%.1f", meters / 1000.0) + " km"

    /** "3h 12m" — পর্দায় দেখানোর জন্য। */
    fun hoursText(fromMs: Long, toMs: Long): String {
        if (fromMs <= 0L || toMs <= fromMs) return "0h 00m"
        val mins = ((toMs - fromMs) / 60000L).toInt()
        return String.format(Locale.US, "%dh %02dm", mins / 60, mins % 60)
    }
}
