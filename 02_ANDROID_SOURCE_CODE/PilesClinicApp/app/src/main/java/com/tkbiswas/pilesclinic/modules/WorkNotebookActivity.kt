/* =====================================================================
   V245 — MODULE 2 : MY WORK NOTEBOOK + DAILY/MONTHLY REPORT + OUTSIDE CALLS.
   Automatic counts are READ-ONLY from the EXISTING public tables (enquiries/
   patients/payments) — nothing is written back, so no duplicate record is
   ever created. Notebook data in schema `wn` (RLS: staff owns own). English UI.
   ===================================================================== */
package com.tkbiswas.pilesclinic.modules

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.Toast
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.NoBengali
// 🔴 V496 বিল্ড-ফিক্স (২১.০৮.২০২৬): আগে `val G = ...BiometricGate.Reason` লিখে
//    enum-টাকেই চলকে রাখার চেষ্টা হয়েছিল — Kotlin-এ সেটা অসম্ভব
//    ("Classifier 'Reason' does not have a companion object")। এখন সোজা
//    import করে `BiometricGate.Reason.FAILED` লেখা হচ্ছে।
import com.tkbiswas.pilesclinic.native.AttendanceRepository
import com.tkbiswas.pilesclinic.native.BiometricGate
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class WorkNotebookActivity : AppCompatActivity() {

    // 🔴 বাগ-ফিক্স (02.08.2026, TK-রিপোর্ট Income & Expense-এ, একই কারণ এখানেও
    // ছিল বলে খুঁজে ঠিক করা হলো): সিস্টেম Back আগে সরাসরি হোমে চলে যেত।
    private var backAction: () -> Unit = { finish() }
    override fun onBackPressed() { backAction() }

    private var mobile = ""
    private var staffCode = ""
    private var branch = ""
    private var day = JSONObject()   // current notebook_days row

    /**
     * 🔴🔴🔒 V511 (২১.০৮.২০২৬, TK-এর স্টাফের রিপোর্ট — *"আমি যখন চেম্বার থেকে
     * বেরোলাম তখন আমাকে OUT TIME show করছিল না, তাই আমি দিতে পারি নাই"*)।
     *
     * ─── আসল কারণ (কোড ধরে প্রমাণিত) ──────────────────────────────────────
     * এই পর্দা আজকের হাজিরার সারিটা **ফোনে কোথাও জমা রাখত না** — `day` খালি
     * দিয়ে শুরু হত, আর প্রতিবার ক্লাউড থেকে পড়ে নিত। `loadDay()`-এর পড়া ব্যর্থ
     * হলে (এক মুহূর্তের দুর্বল নেট) `day` **খালিই** থেকে যেত, ফলে `render()`
     * নিচের `when`-এ `inSet=false` পেয়ে **"IN TIME" অবস্থাটাই আঁকত** —
     * অর্থাৎ **OUT TIME বোতামটা থাকতই না**। স্টাফ শুধু দেখতেন OUT TIME নেই।
     * (উপরের Toast-এ লেখা ছিল *"showing last saved data"* — অথচ saved data
     *  বলে কিছুই ছিল না, তাই লেখাটাও ভুল ছিল।)
     *
     * ─── এখন কী হয় ───────────────────────────────────────────────────────
     *  ১) সফলভাবে পড়া/সেভ হলেই আজকের সারিটা **এই ফোনে জমা** থাকে।
     *  ২) ক্লাউড না পেলে ঐ জমানো কপি থেকেই পর্দা আঁকা হয় — **OUT TIME বোতাম
     *     ঠিক জায়গাতেই থাকে**, উপরে ছোট করে লেখা থাকে "saved copy"।
     *  ৩) জমানো কপিও না থাকলে **ভুল অবস্থা দেখানো হয় না** — লাল লেখা ও
     *     🔄 Try again, যাতে কেউ ভুল করে আবার IN TIME চেপে না দেন।
     *
     * ⛔ **সেভের নিয়ম এক অক্ষরও বদলায়নি** — OUT TIME আগের মতোই তখনই বসে যখন
     *    আজকের আসল সারি ক্লাউড থেকে নিশ্চিত (`askCheckOutReason` → `saveDay`);
     *    জমানো কপি শুধু **দেখানোর** জন্য। তাই পুরোনো IN TIME মুছে যাওয়ার
     *    কোনো নতুন ঝুঁকি তৈরি হয়নি।
     */
    private var dayFromCache = false
    private var dayLoadFailed = false

    private fun dayCacheKey(): String = "wn_day_" + staffCode + "_" + todayIso()

    private fun saveDayCache() {
        try {
            val prefs = getSharedPreferences("wn_prefs", MODE_PRIVATE)
            val e = prefs.edit()
            // ⛔ আজকের বাইরের পুরোনো কপিগুলো মুছে ফেলা হয় — ফোনে জমে থাকে না।
            for (k in prefs.all.keys) if (k.startsWith("wn_day_") && k != dayCacheKey()) e.remove(k)
            e.putString(dayCacheKey(), day.toString()).apply()
        } catch (_: Throwable) { }
    }

    private fun loadDayCache(): JSONObject? = try {
        val raw = getSharedPreferences("wn_prefs", MODE_PRIVATE).getString(dayCacheKey(), null)
        if (raw.isNullOrBlank()) null else JSONObject(raw)
    } catch (_: Throwable) { null }

    /* 🔴🔒 V592 (২৩.০৮.২০২৬ — TK: *"App এর কল গুলি সঠিক ভাবে কাউন্টিং কেন
       হচ্ছে না · এর আগে অনেকবার বলা হয়েছিল"*)

       **প্রমাণিত মূল কারণ — লেখার নাম আর পড়ার নাম এক ছিল না।**
         · কল-বোতাম চাপলে লেখা হয় `ModuleAuth.expectedCode()` দিয়ে —
           মাস্টারের জন্য "MASTER-TK", ডাক্তারের জন্য "DR-KH-MANDAL" …
         · অথচ এখানে পড়া হত `staffCode` দিয়ে, যেটা `user.name` —
           "TK BISWAS" · "Dr. K.H MANDAL" …
       ⇒ দুটো কখনো মেলে না, তাই সংখ্যা **সবসময় 0**।

       StaffDirectory-র ২২টা অ্যাকাউন্ট মিলিয়ে দেখা হয়েছে: ব্রাঞ্চ-স্টাফের
       ১৩টায় নাম আর কোড একই বলে ওগুলো মিলত, কিন্তু **মাস্টার · ৭ জন ডাক্তার ·
       ফিল্ড অফিসার — ৯টা অ্যাকাউন্টে কোনোদিনই মিলত না**।
       ⛔ একই ভুল **ফোনে-জমা গোনাতেও** ছিল (`localCallTapCount`), তাই নেট
          থাকলেও-না-থাকলেও দুই দিকেই 0 আসত।
       ⛔ শুধু কল-গোনার দুটো জায়গাতেই এটা ব্যবহার হয়। বাকি ৩৩ জায়গায়
          `staffCode` **এক অক্ষরও বদলানো হয়নি** (notebook_days · work_reports ·
          পর্দার লেখা) — ওগুলো আগের মতোই চলবে। */
    /* 🔴🔒 V593 (২৩.০৮.২০২৬ — TK: *"এই একই ধরনের সমস্যা বেশ কয়েকবার বলা
       হয়েছে · প্রতিবার আপনি বলেছেন ঠিক হয়ে গেছে · স্টাফদের কাছে আমার কথা
       শুনতে হয় শুধুমাত্র আপনার কারণে"*)

       **আরও খুঁটিয়ে দেখে যেটা পাওয়া গেল — V590 কাজটা অর্ধেক করেছিল।**
       New Enquiry · Registration · Collection — এই তিনটে পড়া ব্যর্থ হলে
       "…" দেখায় (B496 থেকেই), আর নিচে লেখা থাকে *"weak internet"*।
       কিন্তু **App Calls · Outside Calls · Total call** ব্যর্থ হলেও
       নির্লজ্জভাবে **`0`** ছাপত — পর্দাতেও, আর **স্টাফের পাঠানো WhatsApp
       রিপোর্টেও** (৭ জায়গায়)। অর্থাৎ TK-এর কাছে যে রিপোর্টটা যেত, সেটাই
       মিথ্যা 0 বয়ে নিয়ে যেত।

       ⇒ এখন কল-সংখ্যাও অন্য তিনটের মতোই আচরণ করে: পড়া না গেলে "…"।
       ⛔ পড়া সফল হলে সংখ্যা হুবহু আগের মতোই — একটুও বদলায়নি।
       ⛔ ফোনে-জমা গোনা থাকলে সেটাই দেখানো হয় (সেটা তো সত্যিই এই ফোনে
          চাপা কল), "…" তখন লাগে না। */
    private fun callsOk(s: JSONObject): Boolean = s.optBoolean("appOk", true)

    /** কল-সংখ্যা লেখার একমাত্র জায়গা — সফল হলে সংখ্যা, নইলে "…"। */
    private fun callTxt(s: JSONObject, key: String): String =
        if (callsOk(s)) s.optInt(key).toString() else "…"

    /**
     * 🚨🔒 V1187 (০৭.০৯.২০২৬, TK-রিপোর্ট): *"KISHAN-10 & 11 এরা কিছু
     * রেজিষ্ট্রেশন করেছে … কিন্তু তাদের টা কেন 0 আমাকে যখন Daily Report পাঠায়"*।
     *
     * 🔴 **আসল কারণ (কোডে মেপে দেখা, আন্দাজ নয়):** New Enquiry ও Registration-এর
     *    সংখ্যা মেঘ থেকে পড়া হয়, আর পড়া সফল হলো কিনা সেটা আগে থেকেই
     *    `enqOk` / `regOk`-এ জানা থাকে (B496)। **পর্দায়** ব্যর্থ হলে "…" দেখানো হয়,
     *    কিন্তু **WhatsApp-এর রিপোর্টে** ওই পাহারাটা বসানো হয়নি — সেখানে সোজা
     *    `optInt()` লেখা ছিল, তাই পড়া ব্যর্থ হলেই **"0"** চলে যেত। ⇒ কাজ করা
     *    সত্ত্বেও মাস্টারের কাছে "০ রেজিস্ট্রেশন" পৌঁছাত।
     *
     * ⇒ এখন কল-এর মতোই একই পাহারা: পড়া ব্যর্থ হলে **"…"**, কখনো মিথ্যে "0" নয়।
     * ⛔ পড়া সফল হলে সংখ্যা হুবহু আগের মতোই — একটাও গোনার নিয়ম বদলায়নি।
     * ⛔ পর্দার সংখ্যা আগে থেকেই ঠিক ছিল, সেটা ছোঁয়া হয়নি; এখন দুই জায়গা মেলে।
     */
    private fun statTxt(s: JSONObject, key: String, okKey: String): String =
        if (s.optBoolean(okKey, true)) s.optInt(key).toString() else "…"

    private fun callTapCode(): String =
        try { ModuleAuth.expectedCode(this) ?: staffCode } catch (_: Throwable) { staffCode }

    private fun todayIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US); f.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }
    private fun nowTime(): String {
        val f = SimpleDateFormat("HH:mm", Locale.US); f.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }
    private fun nowIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US); f.timeZone = TimeZone.getTimeZone("UTC")
        return f.format(java.util.Date())
    }
    // 🔵 B615 (11.08.2026, TK-নির্দেশ): স্টাফের ডিউটি সকাল ৯টা–সন্ধ্যা ৬টা।
    // IN TIME সকালের কাজ; দুপুর ১২টা (৯টা থেকে ৩ ঘণ্টা দেরি) পার হলে আর IN
    // দেওয়ার মানে নেই — তাই তখন IN TIME বোতাম দেখানো বন্ধ। ⛔ শুধু "দেখাব
    // কি দেখাব না" — কোনো সেভ/সময়-লেখার লজিক এতে বদলায়নি।
    private fun nowHourIST(): Int {
        val c = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return c.get(java.util.Calendar.HOUR_OF_DAY)
    }
    /* 🔴🔒 V508 (TK-এর চূড়ান্ত নিয়ম ২১.০৮.২০২৬, হুবহু):
         *"যখন খুশি আসুক। ১১টার পরে আর ৪টার আগে চলে গেলে Late কাউন্ট হবে।"*

       ⇒ **আসা-যাওয়ায় কোনো বাধা নেই** — IN TIME এখন দিনের যেকোনো সময় দেওয়া যায়।
       ⇒ শুধু **চিহ্ন** পড়ে:
            • IN TIME  সকাল ১১টার **পরে**  → Late
            • OUT TIME বিকেল ৪টার **আগে** → Late
       ⛔ নতুন কোনো ডেটাবেস-ঘর বা SQL লাগেনি — সময় দুটো আগে থেকেই
          `check_in` / `check_out`-এ সেভ হয়; Late সেই সময় দেখেই বলা হয়।
       ⛔ বেতন/হাজিরার হিসাব অক্ষত — `present_days` শুধু দেখে হাজিরা বসেছে
          কিনা, কখন বসেছে তা নয়।
       ⛔ সময় পড়া না গেলে **Late বলা হয় না** — আন্দাজে কিছু বসে না। */
    private fun inTimeWindowOpen(): Boolean = true

    /** সকাল ১১:০০ — এর পরে IN TIME হলে Late। */
    private fun lateInAfterMinutes(): Int = 11 * 60

    /** বিকেল ৪:০০ — এর আগে OUT TIME হলে Late। */
    private fun earlyOutBeforeMinutes(): Int = 16 * 60

    /* 🕒🔒 V1244 (০৮.০৯.২০২৬, TK-নির্দেশ ও অনুমোদন — খাতার সারি ৩৬৫):
       IN TIME-এর মতো **OUT TIME-এও সময়ের সীমা** — দুপুর ১২টা থেকে রাত ৯টা।
       রাত ২টোয় OUT TIME বসানো আর সম্ভব নয়।

       🏍️ **রূপম বাদ (TK-র হুবহু নির্দেশ):** *"রূপমের জন্য কোনো সময়সীমা
          থাকবে না, কারণ সে গ্রামে গ্রামে গিয়ে RMP ডাক্তারদের কাছে ভিজিট করে —
          কোনদিন সন্ধ্যাতে, কোনদিন রাত্রেও বাড়িতে আসতে পারে।"*
          চেনা হয় প্রকল্পের **আগে থেকে প্রমাণিত** `FieldVisit.isFieldStaff()`
          দিয়েই — নতুন কোনো তালিকা বা নম্বর বসানো হয়নি।

       ⚠️ যাচাই করে জানানো: TK-র সন্ধ্যা ৭.৩০-এর নিয়মটা (V1166) কোডে **আছে**,
          কিন্তু ওটা শুধু **মনে করিয়ে দেয়** — আটকায় না। সেটা অপরিবর্তিত রইল;
          এই সীমাটা তার উপরে বাড়তি।
       ⛔ ভিতরের সময়ে আচরণ হুবহু আগের মতোই। ⛔ IN TIME-এর নিয়ম ছোঁয়া হয়নি। */
    private fun outTimeFromMin(): Int = 12 * 60      // দুপুর ১২টা
    private fun outTimeToMin(): Int = 21 * 60        // রাত ৯টা

    /** OUT TIME এখন দেওয়া যাবে কিনা; না গেলে সৎ বার্তা দেখিয়ে false ফেরে। */
    private fun outTimeAllowedNow(): Boolean {
        try {
            // 🏍️ রূপমের (মাঠে ঘোরা স্টাফ) কোনো সীমা নেই
            if (com.tkbiswas.pilesclinic.native.FieldVisit.isFieldStaff(this)) return true
            val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            val m = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
            if (m in outTimeFromMin()..outTimeToMin()) return true
            ModuleUi.toast(this, "OUT TIME can only be marked between 12:00 PM and 9:00 PM.")
            return false
        } catch (_: Throwable) { return true }   // ⛔ সন্দেহ হলে কখনো আটকানো নয়
    }

    /** `"HH:mm"` → মিনিটে। পড়া না গেলে `null`। */
    private fun minutesOf(hhmm: String): Int? = try {
        val p = hhmm.trim().split(":")
        if (p.size < 2) null else p[0].toInt() * 60 + p[1].toInt()
    } catch (_: Throwable) { null }

    private fun isLateIn(hhmm: String): Boolean =
        minutesOf(hhmm)?.let { it > lateInAfterMinutes() } ?: false

    private fun isEarlyOut(hhmm: String): Boolean =
        minutesOf(hhmm)?.let { it < earlyOutBeforeMinutes() } ?: false

    /** IN TIME দেখানোর জন্য — ১১টার পরে হলে "(Late)"। */
    private fun timeWithLate(hhmm: String): String {
        val t = displayTime12(hhmm)
        if (t.isBlank()) return t
        return if (isLateIn(hhmm)) "$t (Late)" else t
    }

    /** OUT TIME দেখানোর জন্য — ৪টার আগে হলে "(Late)"। */
    private fun outWithLate(hhmm: String): String {
        val t = displayTime12(hhmm)
        if (t.isBlank()) return t
        return if (isEarlyOut(hhmm)) "$t (Late)" else t
    }
    /* 🔴 V430 (TK-সিদ্ধান্ত ১৮.০৮.২০২৬: "₹2,10,850 — ভারতীয় ভাগ") — এই একটা
       জায়গায় `Locale.US` বসানো ছিল, তাই ২,১০,৮৫০-এর বদলে ২১০,৮৫০ দেখাত —
       অ্যাপের বাকি সব পর্দার (ও কম্পিউটারের) সঙ্গে মিলত না। এখন ভারতীয়
       ভাগেই দেখাবে। ⛔ অঙ্ক একটুও বদলায়নি, শুধু কমা বসার জায়গা। */
    private fun money(n: Double): String = "₹" + com.tkbiswas.pilesclinic.native.MoneyFormat.inr(n)

    private var quickMarkKind = ""

    /* 🏥🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ) — অন্য ব্রাঞ্চে ডিউটি ও
       Work From Home-এর জন্য তিনটে ছোট মনে-রাখা ঘর।
       • [dutyBranch]        — IN TIME চাপার সময় স্টাফ **যে ক্লিনিকে দাঁড়িয়ে
                               ছিলেন** তার নাম; খাতার সারিতে এটাই বসে।
       • [workedFromHome]    — মাস্টারের অনুমতিতে বাড়ি থেকে হাজিরা কিনা।
       • [wfhApprovedToday]  — আজকের অনুমতি আছে কিনা, একবার পড়ে রাখা হয়
                               (`null` = এখনো পড়া হয়নি)। */
    private var dutyBranch = ""
    private var workedFromHome = false
    private var wfhApprovedToday: Boolean? = null

    // 🔴🆕🔒 খাতার সারি B440/B441 (TK-নির্দেশ, 05.08.2026 — "প্রতিটা
    // ব্রাঞ্চের নির্দিষ্ট ফোনে সারাদিন যত Incoming Call আসে, স্বয়ংক্রিয়ভাবে
    // গণনা হয়ে অ্যাপে বসে যাক")। TK-এর নিজের সিদ্ধান্ত: **অটো গণনা হবে,
    // কিন্তু স্টাফ চাইলে হাতে বদলাতেও পারবে** (ভুল হলে ঠিক করার সুযোগ থাকে)
    // — তাই এই নম্বর শুধু "Outside Calls Today" ঘরের **ডিফল্ট মান** হিসেবে
    // বসে, ঘরটা সবসময়ই আগের মতো সম্পাদনযোগ্যই থাকে।
    // 🔴🔴 **TK নিজে ধরিয়ে দিয়েছেন (05.08.2026):** প্রতিটা ব্রাঞ্চের ফোন
    // **দুই-SIM** — একটা ব্রাঞ্চের নম্বর, আরেকটা অন্য/ব্যক্তিগত। তাই **শুধু
    // ব্রাঞ্চের SIM-এর কলই** গোনা উচিত, দুটো SIM মিলিয়ে না — নইলে সংখ্যা
    // ভুল/বাড়িয়ে দেখাবে। এর জন্য কোন SIM (SIM 1/SIM 2) ব্রাঞ্চের নম্বর তা
    // স্টাফকে একবার বেছে দিতে হয় (ফোন নিজে থেকে "কোন SIM কার নম্বর" এটা
    // নির্ভরযোগ্যভাবে বলতে পারে না — Android-এর সীমাবদ্ধতা, সব ফোনে/
    // অপারেটরে এক না)। বাছাই এই ফোনে একবারই মনে থাকে (SharedPreferences)।
    /**
     * 🔴🔴🔒 V519 (২২.০৮.২০২৬, TK-রিপোর্ট ছবিসহ — *"ক্লিনিকে চলে এসেছে, তারপরও
     * হাজিরা দিতে পারছে না"*)।
     *
     * **আসল কারণ (কোড ধরে প্রমাণিত, আন্দাজ নয়):** `ClinicPresence.check()`
     * অনুমতি না থাকলে শুধু **"অনুমতি নেই" বলে** — কিন্তু অ্যাপ কোথাও
     * Location-এর অনুমতি **চাইতই না** (পুরো প্রজেক্ট খুঁজে দেখা হয়েছে:
     * `ACCESS_FINE_LOCATION` শুধু Manifest-এ ঘোষণা করা ছিল, একটাও
     * `requestPermissions`/launcher ছিল না)। ফলে পর্দায় লেখা উঠত
     * *"অনুমতি দিয়ে আবার চেষ্টা করুন"*, অথচ **অনুমতি দেওয়ার কোনো পথই
     * স্টাফের সামনে খুলত না** — "আবার চেষ্টা" চাপলে হুবহু একই বার্তা।
     * ⇒ ক্লিনিকে দাঁড়িয়ে থেকেও হাজিরা দেওয়া অসম্ভব ছিল।
     *
     * **এখন:** অনুমতি না থাকলে ফোনের নিজের অনুমতি-বাক্স খোলে; স্টাফ
     * "Allow" চাপলে **সঙ্গে সঙ্গে হাজিরার কাজটা আবার নিজে থেকেই চলে**।
     * আগে "Don't ask again" চাপা থাকলে ফোন আর বাক্স দেখায় না — তখন
     * **Open Settings** বোতাম দিয়ে সরাসরি অ্যাপের Settings পাতায় নেওয়া হয়।
     *
     * ⛔ GPS-এর পাহারা এক চুলও দুর্বল হয়নি — দূরত্ব · নকল-অবস্থান · নির্ভুলতা
     *    সব যাচাই আগের মতোই। শুধু **অনুমতি চাওয়ার পথটা** যোগ হলো।
     */
    private var afterLocationPermission: (() -> Unit)? = null

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            val next = afterLocationPermission
            afterLocationPermission = null
            if (granted) {
                // অনুমতি পাওয়া গেছে — স্টাফকে আর কিছু চাপতে হবে না।
                next?.invoke()
            } else {
                inTimeMessage(
                    "Location permission",
                    "Attendance cannot be marked without Location permission.\n\n" +
                        "If the permission box did not appear, please open Settings and allow " +
                        "Location for this app, then try again.",
                    "#A8281C",
                    retry = if (next != null) ({ next() }) else null,
                    extraLabel = "Open Settings",
                    extra = { openAppSettings() }
                )
            }
        }

    /* 🛰️🔒 V1156 (০৭.০৯.২০২৬, TK: *"১৩৯ Field Visit Tracking এবার ঠিক করুন"*)।
       **আসল কারণ (কোডে মেপে পাওয়া):** উপরের অনুমতি-বাক্সটা খোলে শুধু
       *ক্লিনিকে-আছেন-কিনা* যাচাইয়ের ভিতরে। বাইরে ঘোরা স্টাফ (RUPAM) TK-এর
       নিজেরই নিয়মে ওই যাচাই এড়িয়ে যান (V650) ⇒ তাঁর ফোনে অনুমতির বাক্স
       **কোনোদিন উঠতই না** ⇒ GPS-সেবা চুপচাপ ফিরে যেত ⇒ কিলোমিটার চিরকাল ০.০।
       ⇒ এখন ফিল্ড ভিজিটের জন্য **নিজের একটা অনুমতি-বাক্স**। অনুমতি পেলে
         গোনা সঙ্গে সঙ্গে চালু, না পেলে Settings-এ যাওয়ার পথ।
       ⛔ হাজিরা (IN/OUT TIME) এতে **কখনো আটকায় না** — অনুমতি না দিলেও IN TIME
          আগের মতোই সেভ হয়ে থাকে, শুধু কিলোমিটার গোনা হয় না। */
    /* 🛰️🔒 V1344 (১১.০৯.২০২৬, TK-নির্দেশ) — আগে fine-অথবা-coarse যেকোনো
       একটাতেই "granted" বলে "km will now be counted" দেখাত, কিন্তু আসল
       GPS-সেবা শুধু fine (Precise) ছাড়া চলেই না — "Approximate" বেছে নিলে
       এই মিথ্যা বার্তার পরেও কিমি চিরকাল ০.০ থেকে যেত (RUPAM-এর ফোনে এটাই
       ধরা পড়েছে)। এখন শুধু fine granted হলেই "হয়ে গেছে"; শুধু coarse হলে
       আলাদা, স্পষ্ট বার্তা — Settings থেকে "Precise" চালু করতে বলা হয়। */
    private val requestFieldLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val fine = result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fine) {
                try { com.tkbiswas.pilesclinic.native.FieldVisitControl.start(this) } catch (_: Throwable) { }
                /* 🔋🔒 V1516 — permission IN TIME-এর সময় প্রথমবার দিলে এই callback
                   দিয়েই আসা হয়; এখান থেকেও battery protection দেখাতে হবে,
                   নইলে service কয়েক ঘণ্টা পরে OEM battery saver-এ থেমে যেতে পারে। */
                try { askFieldBatterySettingsOnce() } catch (_: Throwable) { }
                ModuleUi.toast(this, "Location allowed - tracking is now running.")
            } else if (coarse) {
                inTimeMessage(
                    "Precise Location needed",
                    "Only \"Approximate\" location was allowed - km cannot be counted with this.\n\n" +
                        "Please open Settings, go to Location permission, and choose \"Precise\" " +
                        "(sometimes called \"Use precise location\"), then try again.",
                    "#A8281C",
                    extraLabel = "Open Settings",
                    extra = { openAppSettings() }
                )
            } else {
                ModuleUi.toast(this, "Without Location permission km cannot be counted.")
            }
            try { render() } catch (_: Throwable) { }
        }

    /**
     * 🛰️ V1156 — ফিল্ড ভিজিটের গোনা সত্যিই চলতে পারবে কি না, সেটা নিশ্চিত করা।
     * অনুমতি না থাকলে বাক্স খোলে; বাক্স না উঠলে (আগে "Don't ask again" চাপা
     * থাকলে) অ্যাপের Settings পাতা।
     * 🛰️🔒 V1346 (১১.০৯.২০২৬, TK-নির্দেশ) — আগে শুধু বাইরে ঘোরা স্টাফের
     * (RUPAM) ফোনে চলত। এখন `tracksAttendanceLocation()`-এর মাধ্যমে সব
     * আসল স্টাফের (branch-সহ) ফোনেই একই প্রম্পট আসে — ⛔ কখনো বাধ্যতামূলক
     * নয়, না দিলেও IN TIME/OUT TIME আগের মতোই কাজ করে (TK স্পষ্ট করেছেন)।
     */
    private fun ensureFieldLocationReady(fromButton: Boolean) {
        try {
            val fv = com.tkbiswas.pilesclinic.native.FieldVisit
            if (!fv.tracksAttendanceLocation(this)) return
            if (!fv.hasLocationPermission(this)) {
                try {
                    requestFieldLocationPermission.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } catch (_: Throwable) { if (fromButton) openAppSettings() }
                return
            }
            // অনুমতি আছে, কিন্তু ফোনের Location সুইচ বন্ধ — নীরবে ব্যর্থ নয়।
            if (!fv.isLocationOn(this)) {
                if (fromButton) openLocationSettings()
                else inTimeMessage(
                    "Location is OFF",
                    "IN TIME is saved, but location tracking cannot start while phone Location is OFF. Please turn Location ON.",
                    "#A8281C",
                    extraLabel = "Open Location Settings",
                    extra = { openLocationSettings() }
                )
                return
            }
            /* 🏍️🔒 V1516 — IN TIME-ই automatic start. আগে fromButton=false হলে
               এই block একেবারেই চলত না; তাই দৈনিক battery-protection prompt-ও
               বাস্তবে আসত না। start() idempotent, তাই আগের start-এর উপর নিরাপদ। */
            try { com.tkbiswas.pilesclinic.native.FieldVisitControl.start(this) } catch (_: Throwable) { }
            if (fromButton) ModuleUi.toast(this, "Location is on - tracking is running.")
            askFieldBatterySettingsOnce()
        } catch (_: Throwable) { }
    }

    /* 🏍️🔒 V1471 (১৪.০৯.২০২৬, TK: "RUPAM ৩০ কিমি ঘুরেছে, অ্যাপ ০.১ দেখাচ্ছে —
       বাড়তি সুরক্ষা বসান") — খাতা মিলিয়ে দেখা গেছে GPS-সেবা ফোনের নিজস্ব
       ব্যাটারি-ম্যানেজার/অটোস্টার্ট-সেটিংসে বারবার (তালিকা ৪৫১·৪৬৫·৫০৫·৫৪২·৫৪৮)
       থেমে গেছে — কোড দিয়ে এটা ১০০% আটকানো যায় না (Android-এর নিজস্ব সীমা),
       কিন্তু নিজে ফোনের Settings-এ একটা কাজ করলে অনেকটাই কমে।
       🟢🔒 V1472 (১৪.০৯.২০২৬, TK-নির্দেশ: "শুধু ওই ২ staff নয়, সবার ক্ষেত্রেই
       একই নিয়ম রাখুন") — RUPAM/ARMAN-এই সীমাবদ্ধ ছিল, এখন হাজিরা-ব্যবহারকারী
       **সব স্টাফ/ব্রাঞ্চের** ফোনে (এই ফাংশনটা যেহেতু আগে থেকেই শুধু
       `tracksAttendanceLocation()`-এর ভিতরেই ডাকা হয় — V1346, staff+branch
       উভয়েই কভার করে)। IN TIME-এ **একবারই** স্পষ্ট নির্দেশ — না করলেও
       IN TIME/OUT TIME আটকায় না। */
    /* 🔋🔒 V1499 (১৫.০৯.২০২৬, TK-নির্দেশ, তালিকা — ARMAN-এর GPS সকাল ১০.৫৯-এর
       পরে থেমে যাওয়া) — এতদিন এই সতর্কতা জীবনে **একবারই** (কখনো "এখন নয়"
       চাপলে চিরকাল আর দেখাত না) আসত। Android কোড দিয়ে মাঝপথে-বন্ধ-হওয়া
       সেবা জোর করে আবার চালু করা যায় না (উপরের V1364-এর মন্তব্যেই লেখা,
       "১০০% নিশ্চয়তা কোনো ফোনেই সম্ভব না") — তাই আসল সমাধান স্টাফের নিজের
       ফোনের Battery সেটিংসই। TK "প্রতিদিন একবার করে আবার চাওয়া হোক"
       বেছেছেন — এখন IN TIME-এ প্রতিদিন প্রথমবার একবার করে দেখাবে (আগের
       দিন "এখন নয়" চাপলেও পরের দিন আবার সুযোগ), সেদিন একবার দেখানোর পরে
       আর বিরক্ত করবে না। ⛔ IN TIME/OUT TIME-এর কাজ এতে কিছুই বদলায় না। */
    private fun askFieldBatterySettingsOnce() {
        try {
            val prefs = getSharedPreferences("piles_field_battery_prompt", MODE_PRIVATE)
            val today = com.tkbiswas.pilesclinic.native.FieldVisit.todayIso()
            if (prefs.getString("asked_date", "") == today) return
            prefs.edit().putString("asked_date", today).apply()
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("কিলোমিটার যেন কখনো বাদ না পড়ে")))
                .setMessage(NoBengali.s("ফোনের নিজস্ব ব্যাটারি-সেভার মাঝেমধ্যে লোকেশন-সেবা বন্ধ করে দিতে পারে, তখন কিলোমিটার আর গোনা হয় না।") + "\n\n" +
                    NoBengali.s("এখন এই অ্যাপের জন্য ফোনের Settings → Battery-তে গিয়ে \"No restriction\"/\"Don't optimize\" বেছে দিন, আর থাকলে Autostart-ও চালু করে দিন — এতে সারাদিনের কিলোমিটার সঠিক থাকবে।"))
                .setPositiveButton(NoBengali.s("Settings খুলি")) { _, _ ->
                    try {
                        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
                        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                            startActivity(android.content.Intent(
                                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                android.net.Uri.parse("package:$packageName")))
                        } else openAppSettings()
                    } catch (_: Throwable) { try { openAppSettings() } catch (_: Throwable) { } }
                }
                .setNegativeButton(NoBengali.s("এখন নয়"), null)
                .show().also { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) }
        } catch (_: Throwable) { }
    }

    /** অ্যাপের নিজের Settings পাতা — সেখান থেকে হাতে অনুমতি দেওয়া যায়। */
    private fun openAppSettings() {
        try {
            startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", packageName, null)
                )
            )
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, "Could not open Settings", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /** ফোনের Location চালু/বন্ধ করার পাতা। */
    private fun openLocationSettings() {
        try {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, "Could not open Location settings", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private val requestCallLogPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) requestPhoneStatePermission.launch(android.Manifest.permission.READ_PHONE_STATE)
        }
    private val requestPhoneStatePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // READ_PHONE_STATE ছাড়াও চলবে (তখন SIM আলাদা করা যাবে না, সৎভাবে
            // নিচে ফলব্যাক আছে) — তাই granted/denied দুটোতেই এগিয়ে যাওয়া হয়।
            maybeAskWhichSimIsBranch()
        }

    private fun hasCallLogPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_CALL_LOG
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun hasPhoneStatePermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun simPrefs() = getSharedPreferences("wn_prefs", MODE_PRIVATE)

    // 🔴🔒 B488 (06.08.2026, TK-নির্দেশ) — এই ফাংশন এখন প্রজেক্টের একই
    // জায়গা থেকে (`BranchSimHelper`, B484/B485-এ হালনাগাদ) সরাসরি ডেটা
    // নেয় — Dialer ও Work Notebook দুটোতেই হুবহু একই নিয়ম।
    private fun maybeAskWhichSimIsBranch() {
        if (com.tkbiswas.pilesclinic.native.BranchSimHelper.hasGenuinelyChosenSim(this)) { applyAutoOutsideCalls(); return } // 🔴🔒 B509
        if (com.tkbiswas.pilesclinic.native.BranchSimHelper.hasChamberAnswer(this)) { applyAutoOutsideCalls(); return }
        /* ☎️🔒 V1427 (১৩.০৯.২০২৬, TK-রিপোর্ট — "চেম্বারের সিম লাগানো স্টাফের
           ফোনেও ব্যানার আসে না") — **আমার দোষ (স্বীকার করছি):** আগে এখানে
           নিজে-পড়া সিম-নম্বর ব্রাঞ্চের নম্বরের সাথে না মিললে **চুপচাপ "না,
           এই ফোনে চেম্বারের নম্বর নেই"** লিখে রাখা হতো — স্টাফকে প্রশ্নই না
           করে। অথচ V633-এই লেখা আছে Android-এর `line1Number` প্রায়ই ভুল/পুরনো
           নম্বর দেয়। ফলে চেম্বারের সিম-লাগানো ফোনেও "না" বসে যেত, আর কখনো
           প্রশ্ন আসত না ⇒ কল-ব্যানার ওই ফোনে চিরতরে বন্ধ।
           **এখন:** নিজে-পড়া নম্বর ব্রাঞ্চের নম্বরের সাথে **মিললে তবেই** "হ্যাঁ"
           ধরা হয়; না মিললে/না পড়তে পারলে **স্টাফকেই জিজ্ঞাসা** করা হয়। */
        if (com.tkbiswas.pilesclinic.native.BranchSimHelper.tryAutoDetectChamberNumber(this) == true) {
            com.tkbiswas.pilesclinic.native.BranchSimHelper.saveHasChamberNumber(this, true)
            askWhichSimSlot()
            return
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06, দল ২): রঙিন হেডার + রাউন্ডেড কার্ড।
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("এই ফোনে কি চেম্বার/ব্রাঞ্চের নম্বর আছে?")))
            .setMessage(NoBengali.s("এই ফোনের কোনো সিমে কি ক্লিনিকের চেম্বার/ব্রাঞ্চের নম্বরটা আছে? ব্যক্তিগত নম্বর হলে \"না\" বলুন।"))
            .setPositiveButton(NoBengali.s("হ্যাঁ")) { _, _ ->
                com.tkbiswas.pilesclinic.native.BranchSimHelper.saveHasChamberNumber(this, true)
                askWhichSimSlot()
            }
            .setNegativeButton(NoBengali.s("No")) { _, _ ->
                com.tkbiswas.pilesclinic.native.BranchSimHelper.saveHasChamberNumber(this, false)
                applyAutoOutsideCalls()
            }
            .setNeutralButton(NoBengali.s("Cancel")) { _, _ -> finish() }
            .setCancelable(true)
            .setOnCancelListener { finish() }
            .show().also { try { NoBengali.installDialog(it); com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    private fun askWhichSimSlot() {
        val slots = try {
            if (!hasPhoneStatePermission()) emptyList()
            else {
                val sm = getSystemService(android.telephony.SubscriptionManager::class.java)
                sm?.activeSubscriptionInfoList?.map { it.simSlotIndex to (it.displayName?.toString() ?: "SIM ${it.simSlotIndex + 1}") } ?: emptyList()
            }
        } catch (_: Throwable) { emptyList() }
        if (slots.size < 2) {
            com.tkbiswas.pilesclinic.native.BranchSimHelper.save(this, -1)
            applyAutoOutsideCalls()
            return
        }
        val labels = slots.map { "${it.second} (SIM ${it.first + 1})" }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06, ৪টে ছোট পপ-আপ): রঙিন হেডার + paint (দাগ)।
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("এই ফোনে ব্রাঞ্চের নম্বর কোন SIM?")))
            .setItems(labels) { _, which ->
                com.tkbiswas.pilesclinic.native.BranchSimHelper.save(this, slots[which].first)
                applyAutoOutsideCalls()
            }
            .setNegativeButton(NoBengali.s("Back")) { _, _ ->
                com.tkbiswas.pilesclinic.native.BranchSimHelper.clearChamberAnswer(this)
                maybeAskWhichSimIsBranch()
            }
            .setCancelable(true)
            .setOnCancelListener {
                com.tkbiswas.pilesclinic.native.BranchSimHelper.clearChamberAnswer(this)
                maybeAskWhichSimIsBranch()
            }
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    /** আজ এই ফোনে আসা, সত্যিই রিসিভ করা (Missed বাদে) Incoming Call গোনে —
     *  `BranchSimHelper.fetchTodayCallLog()`-এর সেই একই তথ্য-উৎস। */
    private fun countTodayIncomingCalls(): Int {
        return try {
            com.tkbiswas.pilesclinic.native.BranchSimHelper.fetchTodayCallLog(this)
                .count { it.type == android.provider.CallLog.Calls.INCOMING_TYPE }
        } catch (_: Throwable) { 0 }
    }

    private var ocCountField: EditText? = null

    // পর্দা রেন্ডার হওয়ার সময় ডাকা হয় — অনুমতি/SIM-বাছাই সব ঠিক থাকলে
    // সঙ্গে সঙ্গে ঘরে বসায়, না থাকলে ধাপে ধাপে চেয়ে নেয়।
    private fun applyAutoOutsideCalls() {
        val field = ocCountField ?: return
        // ⛔ TK-এর নিয়ম: আজ ইতিমধ্যে হাতে/আগে সেভ করা সংখ্যা থাকলে অটো-গণনা
        // সেটা ওভাররাইড করবে না — স্টাফের নিজের হাতে বদলানো মান-ই চূড়ান্ত।
        if (day.optInt("outside_calls_manual", 0) > 0) return
        if (!hasCallLogPermission()) { requestCallLogPermission.launch(android.Manifest.permission.READ_CALL_LOG); return }
        val resolved = com.tkbiswas.pilesclinic.native.BranchSimHelper.hasGenuinelyChosenSim(this) || // 🔴🔒 B509
            com.tkbiswas.pilesclinic.native.BranchSimHelper.hasChamberAnswer(this)
        if (!resolved) { maybeAskWhichSimIsBranch(); return }
        val n = countTodayIncomingCalls()
        if (n > 0 && field.text.toString().trim().let { it.isBlank() || it == "0" }) {
            field.setText(n.toString())
            pushOutsideCallsSilently(n)
        }
    }

    /* 🔴🔒 V909 (৩১.০৮.২০২৬, TK-নির্দেশ: *"আসা কলও চেম্বারের ফোন থেকেই গুনবেন,
       … ওই নম্বর দিয়ে যে ফোনেই লগ ইন করবে একই রকম দেখাতে হবে"*)

       আগে ক্লিনিকের নম্বরে আসা কলের গোনাটা ক্লাউডে যেত **শুধু স্টাফ সেভ/OUT
       TIME চাপার পরে**। তার আগে পর্যন্ত অন্য ফোনে ওই ঘর ফাঁকা/০ থাকত — তাই
       একই আইডিতে দুই ফোনে দুই রকম দেখাত।
       এখন চেম্বারের ফোন সংখ্যাটা বার করার **সঙ্গে সঙ্গেই** ক্লাউডে বসিয়ে দেয়,
       তাই যে ফোনেই ওই আইডিতে খোলা হোক, একই সংখ্যা।
       ⛔ সংখ্যা কখনো কমে না — জমা সংখ্যার চেয়ে বড় হলে তবেই লেখা হয়।
       ⛔ চুপচাপ — ব্যর্থ হলে কোনো সতর্কবার্তা দেখায় না (স্টাফ কিছু চাপেননি);
          সেভ/OUT TIME-এর নিজের পথ ও তার বার্তা এক অক্ষরও বদলায়নি। */
    private fun pushOutsideCallsSilently(n: Int) {
        try {
            if (!day.has("work_date")) return
            if (day.optInt("outside_calls_manual", 0) >= n) return
            day.put("outside_calls_manual", n)
            day.put("updated_at", nowIso())
            val snapshot = try { JSONObject(day.toString()) } catch (_: Throwable) { return }
            try { saveDayCache() } catch (_: Throwable) { }
            Thread { try { robustSaveNotebookDay(snapshot) } catch (_: Throwable) { } }.start()
        } catch (_: Throwable) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val u = NativeSession.current(this)
        // 🧑‍⚕️🔒 V496 (২১.০৮.২০২৬, TK §২) — **নিরাপত্তা-জাল।**
        // Dashboard-এ ডাক্তারের টাইল লুকানো হয়েছে, কিন্তু পুরনো নোটিফিকেশন
        // বা অন্য কোনো পথে এই পর্দা খুলে যেতে পারত। তাই এখানেও একবার দেখা হয়।
        // ⛔ কোনো তথ্য মোছা হয় না — শুধু পর্দাটা খোলে না।
        if (u != null && !com.tkbiswas.pilesclinic.native.RoleRules.usesAttendance(u)) {
            android.widget.Toast.makeText(
                this,
                NoBengali.s(com.tkbiswas.pilesclinic.native.RoleRules.DOCTOR_NO_ATTENDANCE_MSG),
                android.widget.Toast.LENGTH_LONG
            ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
            finish()
            return
        }
        mobile = u?.mobile ?: ""
        staffCode = u?.name ?: ""
        branch = u?.branch ?: ""
        // 🔴 B418 (04.08.2026, TK-নির্দেশ — "নোটিফিকেশনে চাপলে শুধু IN/OUT
        // TIME-ই দেখাবে, বাকি পুরো পাতা না"): রিমাইন্ডার নোটিফিকেশনে চাপলে
        // এই extra আসে ("in"/"out") — নিচে `loadDay()`-এর পরে পুরো পাতা না
        // এঁকে শুধু একটা ছোট ডায়ালগ দেখানো হয়।
        quickMarkKind = intent.getStringExtra("quick_mark") ?: ""
        // 🔵 (07.08.2026) — নোটিফিকেশন থেকে এলে প্রথম মুহূর্ত থেকেই স্পষ্ট বার্তা,
        // যাতে পর্দা কখনো ফাঁকা/জমে না থাকে। ধাপে ধাপে বার্তা বদলায় ("খুলছি" →
        // "তথ্য আনছি" → OUT বক্স), তাই কোথায় আটকায় সঙ্গে সঙ্গে বোঝা যায়।
        if (quickMarkKind.isNotBlank()) showWnLoading(NoBengali.s("⏳ খুলছি... একটু অপেক্ষা করুন"))
        // 🔵 TK-ORDER (07.08.2026): লগইন নিশ্চিত হলে **আগে জমা-খাতা বসাও** (আগে
        // কোনো IN/OUT সেভ না-বসে জমা থাকলে সেটা ক্লাউডে বসে যায়), **তারপর আজকের
        // দিন লোড** — তাই "হারানো IN TIME" আর দেখাবে না। ⛔ জমা খালি থাকলে
        // flushPendingNotebook() সঙ্গে সঙ্গে ফিরে আসে (বাড়তি কিছু হয় না)।
        /* 🔎🔒 V932 — কোন ধাপে আটকাচ্ছে সেটা পর্দায় দেখানোর জন্য (OpenTrace)।
           ⛔ শুধু একটা ছোট লেখা — কোনো নিয়ম · হিসাব · সেভ কিছুই বদলায়নি। */
        OpenTrace.step(this, "0. notebook screen started")
        ModuleUi.ensureSignedIn(this, staffCode) {
            OpenTrace.step(this, "7. saving pending marks")
            flushThenLoad()
        }
    }

    // 🔴🆕 V433 (TK-নির্দেশ ১৮.০৮.২০২৬ — "WhatsApp এ একবার পাঠানো হয়ে গেলে আর
    // দেখানোর দরকার নেই / send in time WhatsApp again")। TK-এর বাছা পথ:
    // "বোতাম চাপার পরে একবার জিজ্ঞাসা করব"। তাই — WhatsApp খোলার আগে একটা
    // "জিজ্ঞাসা বাকি" চিহ্ন বসে; ব্যাক করে ফিরলে একবারই ছোট প্রশ্ন আসে,
    // "হ্যাঁ" বললে সেই দিনের জন্য বোতামটা আর দেখাবে না।
    // ⛔ নিরাপত্তা: চিহ্নটা **শুধু এই ফোনের ভিতরে** (SharedPreferences
    //    "wn_prefs" — markReminderFlag-এর হুবহু একই প্রমাণিত ধরন), Supabase-এ
    //    কোনো নতুন ঘর লেখা হয় না, তাই সেভ ভাঙার কোনো ঝুঁকি নেই।
    // ⛔ তারিখ মিলিয়ে দেখা হয় — পরের দিন নিজে থেকেই আবার বোতাম ফিরে আসে।
    // ⛔ "No" বললে বোতাম থেকেই যায় — কেউ কখনো আটকা পড়বে না।
    private fun waSentKey(kind: String) = if (kind == "in") "wa_sent_in_date" else "wa_sent_out_date"

    private fun isWaSent(kind: String): Boolean = try {
        getSharedPreferences("wn_prefs", MODE_PRIVATE).getString(waSentKey(kind), "") == todayIso()
    } catch (_: Throwable) { false }

    private fun setWaSent(kind: String) {
        try {
            getSharedPreferences("wn_prefs", MODE_PRIVATE).edit()
                .putString(waSentKey(kind), todayIso()).apply()
        } catch (_: Throwable) { }
    }

    /** কোন ধরনের বার্তার উত্তর এখনো জিজ্ঞাসা করা বাকি ("in"/"out"), নইলে ফাঁকা।
     *  ⛔ শুধু চলতি স্ক্রিনের মেমরি — কোথাও সেভ হয় না। */
    private var waAskKind = ""

    override fun onResume() {
        super.onResume()
        /* 👨‍⚕️ V1032 — পর্দায় ফিরলেই আজকের ডাক্তার-ভিজিটের গোনা নতুন করে
           (RMP পর্দা থেকে ফিরলে সঙ্গে সঙ্গে সংখ্যাটা মিলে যায়)। পিছনের
           থ্রেডে চলে, তাই পর্দা এক মুহূর্তও আটকায় না। */
        loadDocVisitToday()
        val kind = waAskKind
        waAskKind = ""
        if (kind.isBlank() || isWaSent(kind)) return
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(
                    this, NoBengali.s("পাঠানো হয়েছে?")))
                .setMessage(NoBengali.s("Has it been sent on WhatsApp?"))
                .setPositiveButton(NoBengali.s("Yes, sent")) { _, _ ->
                    setWaSent(kind)
                    try { render() } catch (_: Throwable) { }
                }
                .setNegativeButton(NoBengali.s("No, not sent"), null)
                .setCancelable(true)
                .show().also {
                    try { NoBengali.installDialog(it); com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { }
                }
        } catch (_: Throwable) { }
    }

    // 🔴 B325/B326 (03.08.2026, TK-নির্দেশ — IN TIME/OUT TIME ভুলে গেলে
    // অ্যালার্ম): সকাল ১০টা/সন্ধ্যা ৬টার রিমাইন্ডার
    // (`AttendanceReminderWorker.kt`) এই স্থানীয় ফ্ল্যাগ দুটো দেখেই ঠিক করে
    // নোটিফিকেশন দেখাবে কিনা — তাই IN TIME/OUT TIME/Leave মার্ক বা আনমার্ক
    // হওয়ার মুহূর্তেই এখানে সবসময় সিঙ্ক করা হয়।
    private fun markReminderFlag(kind: String, done: Boolean) {
        val prefKey = if (kind == "in") "checkin_or_leave_date" else "checkout_or_leave_date"
        val prefs = getSharedPreferences("wn_prefs", MODE_PRIVATE)
        prefs.edit().putString(prefKey, if (done) todayIso() else "").apply()
        // 🔵 (07.08.2026, TK-রিপোর্ট) — IN/OUT মার্ক হয়ে গেলে সেই রিমাইন্ডার
        // নোটিফিকেশনটা সঙ্গে সঙ্গে ট্রে থেকে সরিয়ে দেওয়া হয়, যাতে মার্ক করার
        // পরেও ঝুলে না থাকে। ⛔ শুধু নোটিশ মোছা — কোনো ডেটা/সেভ বদলায়নি।
        if (done) {
            try {
                val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.cancel(
                    if (kind == "in") com.tkbiswas.pilesclinic.native.AttendanceReminderWorker.NOTIF_ID_IN
                    else com.tkbiswas.pilesclinic.native.AttendanceReminderWorker.NOTIF_ID_OUT
                )
            } catch (_: Throwable) {}
        }
    }

    // 🔴 B327 (03.08.2026, TK-রিপোর্ট — Notes/Carry-forward/Problem-Help ঘরে
    // "null" লেখা আসছিল): Android-এর org.json-এ কোনো কলাম ডেটাবেসে SQL NULL
    // থাকলে `optString()` "" ফেরত দেয় না — শব্দ "null" ফেরত দেয় (এটাই একই
    // বাগ, StaffProfileActivity.kt-এ B286-এ আগে একবার ধরা পড়ে ঠিক হয়েছিল,
    // সেই একই fix এখানে প্রয়োগ করা হলো)। **সৎ কথা:** শুধু default-value-ছাড়া
    // `optString(key)` না — `optString(key, fallback)`-ও একই ভুল করে যখন
    // কলামটা আসলেই আছে কিন্তু মান SQL NULL (কলাম সম্পূর্ণ অনুপস্থিত থাকলে নয়) —
    // তাই এই ফাইলের **প্রতিটা** `day.optString(...)` কল (fallback থাকুক বা না
    // থাকুক) এই `ns()`-এ বদলানো হলো।
    private fun ns(o: JSONObject, key: String): String {
        val v = o.optString(key)
        return if (v.isBlank() || v == "null") "" else v
    }

    // 🔴🔴🔒 B496 (06.08.2026, TK-এর স্টাফের রিপোর্ট — "ইন টাইম করেছিলাম
    // কিন্তু এখন দেখাচ্ছে না, এনকোয়ারি ছিল এখন জিরো দেখাচ্ছে") — আসল
    // কারণ খুঁজে পাওয়া গেছে: পুরনো `getRows()` নেটওয়ার্ক ব্যর্থতা আর
    // সত্যিকারের-খালি — এই দুটো আলাদা করে না (দুটোতেই খালি JSONArray
    // দেয়, B316-এর নোটেই লেখা ছিল এই সীমাবদ্ধতা)। তাই ফোনের সিগন্যাল
    // এক মুহূর্তের জন্য দুর্বল হলেই আজকের IN TIME/সংখ্যা **সত্যিই মোছেনি,
    // শুধু আবার লোড করতে গিয়ে ব্যর্থ হয়ে ফাঁকা দেখাচ্ছিল** — আসল ডেটা
    // ক্লাউডেই অক্ষত ছিল। এখন `getRowsChecked()` (Staff Profile-এর জন্য
    // আগে থেকেই তৈরি নিরাপদ ফাংশন) ব্যবহার হয় — লোড ব্যর্থ হলে আগে যা
    // দেখা যাচ্ছিল সেটাই থেকে যায়, ফাঁকা দিয়ে ওভাররাইট হয় না।
    // 🔵 (07.08.2026, TK-রিপোর্ট — "OUT চাপলে কিছুই আসে না, পর্দা জমে থাকে"):
    // নোটিফিকেশন থেকে এলে (quick-mark) আগে খোলার সময় পর্দা ফাঁকা থাকত (onCreate-এ
    // setContentView নেই, সব async), তাই "কিছু হচ্ছে না" মনে হতো। এখন প্রতিটা
    // ধাপে স্পষ্ট বার্তা ("তথ্য আনছি..."), ব্যর্থ হলে "আবার চেষ্টা" বাটন।
    // ⛔ সেভ/মার্ক-লজিক এক অক্ষরও বদলায়নি — OUT আগের মতোই আজকের তথ্য আসার
    // পরেই বসে (তাই ভুল করে আগের IN TIME কখনো মুছবে না)।
    private var wnLoadingDlg: androidx.appcompat.app.AlertDialog? = null
    private fun loadDay() {
        val date = todayIso()
        if (quickMarkKind.isNotBlank()) showWnLoading(NoBengali.s("⏳ আজকের তথ্য আনছি..."))
        Thread {
            val r = ModuleAuth.getRowsChecked("wn", "notebook_days", "select=*&staff_code=eq.$staffCode&work_date=eq.$date&limit=1")
            if (!r.ok) {
                // নেটওয়ার্ক/সার্ভার ব্যর্থ — `day` বদলানো হয় না। quick-mark পথে
                // (নোটিফিকেশন থেকে) ফাঁকা day দিয়ে OUT বসালে ডেটা ঝুঁকিতে পড়ত,
                // তাই এখানে অসম্পূর্ণ মার্ক না দেখিয়ে "আবার চেষ্টা" দেখানো হয়।
                // 🔴 V511 (উপরের বড় নোট): এখন আগে **এই ফোনে জমানো আজকের কপিটা**
                //   দেখা হয় — থাকলে সেটা দিয়েই পর্দা আঁকা হয়, তাই OUT TIME বোতাম
                //   হারায় না। না থাকলে ভুল অবস্থা না দেখিয়ে পরিষ্কার ভুল-বার্তা।
                val cached = loadDayCache()
                runOnUiThread {
                    dismissWnLoading()
                    if (cached != null) {
                        day = cached
                        if (!day.has("manual_entries") || day.isNull("manual_entries")) day.put("manual_entries", JSONArray())
                        dayFromCache = true
                        dayLoadFailed = false
                    } else {
                        dayFromCache = false
                        dayLoadFailed = true
                    }
                    if (quickMarkKind.isNotBlank() && cached == null) {
                        showWnRetry()
                    } else {
                        if (cached != null) Toast.makeText(this, "Offline - showing this phone's saved copy", Toast.LENGTH_SHORT).show()
                        maybeShowQuickMark()
                    }
                }
                return@Thread
            }
            day = if (r.rows.length() > 0) r.rows.getJSONObject(0) else JSONObject()
                .put("staff_code", staffCode).put("staff_mobile", mobile).put("work_date", date)
                .put("manual_entries", JSONArray())
            if (!day.has("manual_entries") || day.isNull("manual_entries")) day.put("manual_entries", JSONArray())
            val leaveNow = day.optBoolean("is_leave", false)
            if (ns(day, "check_in").isNotBlank() || leaveNow) markReminderFlag("in", true)
            if (ns(day, "check_out").isNotBlank() || leaveNow) markReminderFlag("out", true)
            dayFromCache = false
            dayLoadFailed = false
            saveDayCache()   // 🔴 V511 — আজকের সারিটা এই ফোনে জমা রইল
            runOnUiThread {
                dismissWnLoading()
                /* ⏰ V1166 — জোর-করে-জিজ্ঞাসার পপ-আপটা **আগে** দেখা হয়; সেটা
                   উঠলে quick-mark পপ-আপ আর দেখানো হয় না (নইলে দুটো পপ-আপ
                   একটার উপর আরেকটা জমত — নিজে মেপে ধরা)। জোরেরটাই জেতে,
                   কারণ TK-র নিয়ম: *"জোর করে নোটিফিকেশন দেবে"*। */
                if (maybeAskOutTime()) { quickMarkKind = ""; render() } else maybeShowQuickMark()
            }
        }.start()
    }

    /* ⏰🔒 V1166 (০৭.০৯.২০২৬, TK-নির্দেশ) — সন্ধ্যা ৭.৩০-এর পরে OUT TIME না
       দেওয়া থাকলে **জোর করে** জিজ্ঞাসা: আর কতক্ষণ থাকবেন ও কেন। বসানো সময়
       পেরোলে আবার। ⛔ শুধু তখনই, যখন আজ IN TIME দেওয়া আছে কিন্তু OUT নেই —
       ছুটির দিনে বা না-আসা দিনে কখনো নয়। ⛔ হাজিরার কোনো ঘর এখান থেকে লেখা
       হয় না; "OUT TIME now" চাপলে প্রকল্পের পুরনো প্রমাণিত পথটাই চলে। */
    /** @return পপ-আপটা সত্যিই দেখানো হলো কিনা (তাহলে quick-mark আর দেখানো হয় না)। */
    private fun maybeAskOutTime(): Boolean {
        try {
            if (isFinishing || isDestroyed) return false
            if (day.optBoolean("is_leave", false)) return false
            val hasIn = ns(day, "check_in").isNotBlank()
            val hasOut = ns(day, "check_out").isNotBlank()
            if (!com.tkbiswas.pilesclinic.native.OutTimePrompt.shouldAsk(this, hasIn, hasOut)) return false
            com.tkbiswas.pilesclinic.native.OutTimePrompt.show(
                this, staffCode.ifBlank { mobile }, NativeSession.current(this)?.branch.orEmpty(), mobile
            ) {
                /* "OUT TIME now" ⇒ প্রকল্পের পুরনো প্রমাণিত পথ — quick-mark পপ-আপ।
                   ⛔ নতুন কোনো সেভ-পথ বানানো হয়নি। */
                quickMarkKind = "out"; maybeShowQuickMark()
            }
            return true
        } catch (_: Throwable) { return false }
    }

    // 🔵 (07.08.2026) — খোলার সময় স্পষ্ট ফিডব্যাক, যাতে পর্দা ফাঁকা/জমে না
    // থাকে। বার্তাটা ধাপ-নির্দেশক ("লগইন হচ্ছে" / "তথ্য আনছি") — কোন ধাপে
    // আটকায় তা সঙ্গে সঙ্গে দেখা যায়। ⛔ কোনো ডেটা/সেভ-লজিক নয়, শুধু বার্তা।
    private fun showWnLoading(msg: String) {
        try {
            dismissWnLoading()
            wnLoadingDlg = androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(true)
                .show()
                .also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) {} }
        } catch (_: Throwable) {}
    }
    private fun dismissWnLoading() {
        try { wnLoadingDlg?.dismiss() } catch (_: Throwable) {}
        wnLoadingDlg = null
    }
    private fun showWnRetry() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
            /* 🔤🔒 V832 (২৯.০৮.২০২৬, TK-নির্দেশ: *"ঝুঁকিহীনভাবে যেগুলি করা
               যাবে শুধুমাত্র সেগুলি করুন"*) — V728-এ এই লেখাগুলো বাংলা রাখা
               হয়েছিল **একটাই কারণে**: তখন ইংরেজি লেখাটা হিন্দি-তালিকার চাবি
               হয়ে গিয়ে অন্য পর্দার লেখা ভুল করে হিন্দি করে দিতে পারত।
               ⇒ **V730-এ হিন্দি পুরোটাই তুলে দেওয়া হয়েছে** (যাচাই করে দেখা:
                 HINDI-তালিকার ৩৫৩টা মানের একটাতেও আর দেবনাগরী নেই)।
               ⇒ তাই সেই ঝুঁকিটা **আর নেই**, আর এই লেখাগুলো ইংরেজি করা যায়।
               ⛔ ইংরেজি লেখাটা **নিজে বানানো হয়নি** — `NoBengali`-র নিজের
                  অনুবাদ ("Try Again" · "Close") হুবহু বসানো হলো, তাই
                  কিশানগঞ্জের স্টাফ আগে যা দেখতেন **হুবহু তাই** দেখবেন।
               ⛔ পপ-আপের রং যাচাই করা হয়েছে — `PremiumAlert.severityOf()`-এর
                  লাল/হলুদ কোনো শব্দই পুরনো বা নতুন শিরোনামে নেই ⇒ **রং এক**।
               ⛔ এগুলো **জমা/তুলনার মান নয়** — শুধু পর্দায় দেখানোর লেখা
                  (`অন্য কারণ` ও `হ্যাঁ` ইচ্ছাকৃতভাবে বাংলাই রইল, ওগুলো
                  ডেটাবেসে জমা হয় / কোডে মিলিয়ে দেখা হয়)। */
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("Try Again")))
                .setMessage(NoBengali.s("Today's information could not be loaded. It is required to save OUT TIME safely; otherwise the previous IN TIME could be lost. Please try again."))
                .setCancelable(true)
                .setPositiveButton(NoBengali.s("🔄 Try Again")) { _, _ -> loadDay() }
                .setNegativeButton(NoBengali.s("Close")) { _, _ -> render() }
                .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) {} }
        } catch (_: Throwable) {}
    }

    // 🔴 B418 (04.08.2026, TK-নির্দেশ) — নোটিফিকেশন থেকে এলে (quickMarkKind
    // সেট থাকলে) এবং সেই কাজটা এখনো না-হয়ে থাকলে, শুধু একটা ছোট ডায়ালগ —
    // "IN TIME মার্ক করবেন?" — পুরো Work Notebook পাতা প্রথমে দেখানো হয় না।
    // "Not now" চাপলে বা ডায়ালগ বন্ধ করলে স্বাভাবিক পুরো পাতাই খোলে (আটকে
    // থাকতে হয় না)। ⛔ সেভ-লজিক (`day.put`, `markReminderFlag`, `saveDay`)
    // ঠিক বোতাম চাপলে যা হতো তার হুবহু একই, শুধু আলাদা জায়গা থেকে ডাকা।
    // 🔴🆕🔒 B438 (TK-নির্দেশ, 05.08.2026) — নোটিফিকেশন থেকে এলে এখন
    // একাধিক উত্তর দেওয়ার সুযোগ (আগে শুধু Yes/Not now ছিল):
    //  ১) হ্যাঁ, এখনই মার্ক করুন (IN TIME-এ মার্কের পরে WhatsApp-এ শেয়ার
    //     করার সুযোগও খোলে — TK-নির্দেশ)
    //  ২) নির্দিষ্ট সময় বেছে পরে মনে করানোর ব্যবস্থা (TK: "নির্ধারিত টাইম
    //     বসাতে পারবে, শুধু ২ ঘণ্টা বলে বসিয়ে রাখবে না")
    //  ৩) শুধু IN TIME-এ: আজকের ছুটি (এক-চাপেই)
    //  ৪) Not now (আগের ১০-মিনিট-চেইন আগের মতোই চলবে)
    // ⛔ সেভ-লজিক (day.put/markReminderFlag/saveDay) এক অক্ষরও বদলায়নি।
    private fun maybeShowQuickMark() {
        val isLeave = day.optBoolean("is_leave", false)
        val already = if (quickMarkKind == "in") ns(day, "check_in").isNotBlank() else ns(day, "check_out").isNotBlank()
        if (quickMarkKind.isBlank() || isLeave || already) { render(); return }
        // 🔵 B608 (10.08.2026, TK-নির্দেশ): IN TIME না হলে OUT TIME-এর কোনো মানে নেই —
        // তাই IN না করে OUT quick-mark এলে পপ-আপ না দেখিয়ে স্বাভাবিক পাতা দেখাই
        // (সেখানে IN TIME বোতামই থাকবে)। ⛔ IN হয়ে গেলে আগের মতোই OUT আসবে।
        if (quickMarkKind == "out" && ns(day, "check_in").isBlank()) { render(); return }
        // 🔵 B615 (11.08.2026, TK-নির্দেশ): দুপুর ১২টা পার হলে IN TIME-এর সময়
        // শেষ — নোটিফিকেশন থেকে এলেও IN পপ-আপ না দেখিয়ে স্বাভাবিক পাতা দেখাই
        // (সেখানে "সময় শেষ" নোটিশ ও Mark As Leave থাকবে)।
        if (quickMarkKind == "in" && !inTimeWindowOpen()) { render(); return }
        val title = if (quickMarkKind == "in") "IN TIME" else "OUT TIME"
        // 🔴🔒 B536 (08.08.2026, TK-নির্দেশ, ভালো যাচাই করে অনুমোদনের পরে —
        // "একবার IN TIME হয়ে গেলে সেই দিন আর এই টাইমের দরকার নেই, একবার OUT
        // TIME হয়ে গেলে আর দরকার নেই"): আগে এখানে একটা "⏰ কখন আসব/যাব বলছি"
        // অপশন ছিল যেটা আসলে IN/OUT **মার্ক করত না** — শুধু রিমাইন্ডার পরে আবার
        // বাজাত (`askPostponeTime()` → শুধু `scheduleExactTime`, কোনো check_in/
        // check_out বা ফ্ল্যাগ বসত না)। ফলে স্টাফ ভাবত "সময় দিয়ে দিয়েছি",
        // অথচ রিমাইন্ডার আবার আসত, আর শেষে চাপলে **তখনকার অটোমেটিক সময়** বসে
        // যেত — এটাই TK-এর রিপোর্ট করা "IN TIME লিখছি, একটু পরে আবার অটোমেটিক
        // টাইম শো করছে" / "OUT TIME প্রেস করছি, আবার OUT TIME শো করছে" বাগ।
        // এখন সেই বিভ্রান্তিকর অপশন সরানো হলো — শুধু "এখনই মার্ক করুন"
        // (মার্ক হলেই ফ্ল্যাগ বসে, রিমাইন্ডার আর আসে না), ছুটি (IN-এ), ও "Not
        // now"। ⛔ `askPostponeTime()` ফাংশনটা মোছা হয়নি (প্রজেক্ট-নিয়ম),
        // শুধু আর কোথাও থেকে ডাকা হয় না; মার্ক/সেভ-লজিক এক অক্ষরও বদলায়নি।
        val options = (if (quickMarkKind == "in")
            arrayOf("✅ হ্যাঁ, এখনই IN TIME মার্ক করুন", "🏖️ আজকে আমার ছুটি", "Not now")
        else
            arrayOf("✅ হ্যাঁ, এখনই OUT TIME মার্ক করুন", "Not now")).map { NoBengali.s(it) }.toTypedArray()
        // 🔴 V509: OUT-এর পপ-আপ **খোলার সঙ্গে সঙ্গেই** অবস্থান দেখা শুরু — স্টাফ
        // যতক্ষণে বেছে নেন ততক্ষণে উত্তর হাতে, তাই সেভে অপেক্ষা করতে হয় না।
        if (quickMarkKind != "in") startPlaceProbe()
        lateinit var qmDlg: androidx.appcompat.app.AlertDialog
        qmDlg = androidx.appcompat.app.AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06, ৪টে ছোট পপ-আপ): রঙিন হেডার + paint (দাগ)। অপশন/লজিক অপরিবর্তিত।
            // 🔵 B608 (10.08.2026): setMessage আর setItems একসাথে দিলে Android
            // অপশন-তালিকা বসায় না (message থাকলে list দেখায় না) — তাই OUT-এ (message
            // ফাঁকা) পুরো পপ-আপ খালি ও অচল আসত। message বাদ; অপশন-লেখা নিজেই স্পষ্ট।
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title))
            .setItems(options) { _, which ->
                val picked = options[which]
                when {
                    picked.startsWith("✅") -> {
                        if (quickMarkKind == "in") {
                            // 🔒 V496: ফোনের ঘড়ি দিয়ে সরাসরি বসানো বন্ধ —
                            // এখন ক্লিনিক → আঙুলের ছাপ → সার্ভার, এই পথে।
                            startInTimeFlow { afterInTimeMarked { finish() } }
                        } else {
                            checkEmptyFieldsThenOut {
                              // 🔴 V509 (TK-সিদ্ধান্ত ২১.০৮.২০২৬, বিকল্প ৩): নোটিফিকেশন
                              // থেকে দ্রুত OUT TIME করলেও **একই নিয়ম** — নইলে এই
                              // শর্টকাট দিয়ে বাড়ি থেকে চুপচাপ বসিয়ে দেওয়া যেত।
                              // ⛔ কাউকে আটকায় না; শুধু লিখে রাখে।
                              // 🔒 সময়টা **এখনই** ধরা — অপেক্ষা করতে হলেও খাতায়
                              //    চাপার সময়টাই বসবে, পরের সময় নয়।
                              if (!outTimeAllowedNow()) { render(); return@checkEmptyFieldsThenOut }   // 🕒 V1244
                              val outAt = nowTime()
                              withPlaceNote { placeNote ->
                                day.put("check_out", outAt); markReminderFlag("out", true)
                                stopFieldVisitIfRunning()   // 🏍️ V968
                                if (placeNote.isNotBlank()) {
                                    val old = ns(day, "check_out_reason")
                                    day.put("check_out_reason", if (old.isBlank()) placeNote else "$old · $placeNote")
                                }
                                saveDay {
                                    android.widget.Toast.makeText(this, "OUT TIME marked", android.widget.Toast.LENGTH_SHORT).show()
                                    // 🔴🔴🔒 B518 — নোটিফিকেশন থেকে দ্রুত OUT TIME
                                    // মার্ক করলেও এখন একই পূর্ণাঙ্গ দিনের রিপোর্ট
                                    // WhatsApp-এ যায় (আগে এখানেও শুধু ৩-লাইনের
                                    // ছোট বার্তা যেত)।
                                    fetchStats("day", todayIso()) { s ->
                                        runOnUiThread {
                                            val text = StringBuilder()
                                            text.append("Daily Report ").append(dotDate(todayIso())).append(" Time-").append(shareTimeLabel())
                                                .append("\nStaff: $staffCode\n")
                                            text.append("IN TIME- ").append(displayTime12(ns(day, "check_in")).ifBlank { "-" }).append("\n")
                                            text.append("OUT TIME ").append(displayTime12(ns(day, "check_out")).ifBlank { "-" }).append("\n")
                                            // 🔴 V509: ক্লিনিকের বাইরে থেকে দিলে তবেই এই লাইন।
                                            if (placeNote.isNotBlank()) text.append("⚠️ ").append(placeNote).append("\n")
                                            text.append("\nNew Enquiry: ").append(statTxt(s, "enquiries", "enqOk"))
                                                .append("\nRegistration: ").append(statTxt(s, "registrations", "regOk"))
                                            // ⚠️ সৎ সীমাবদ্ধতা: নোটিফিকেশন থেকে দ্রুত
                                            // OUT TIME করলে "Today Patient" ফর্মের
                                            // লাইভ ঘর থেকে পড়া যায় না (এই পথে ফর্ম
                                            // খোলাই থাকে না) — তাই এখানে "0" যায়।
                                            // পুরো ফর্ম খুলে OUT TIME করলে (স্বাভাবিক
                                            // পথ) সঠিক সংখ্যাই যায়।
                                                .append("\nToday Patient: ").append("0")
                                                .append("\nApp Calls: ").append(callTxt(s, "appCalls"))
                                                .append("\nOutside Calls: ").append(callTxt(s, "outsideCalls"))
                                                .append("\nTotal call : ").append(callTxt(s, "totalCalls"))
                                                .append(docVisitLine())
                                            val notesTxt = ns(day, "day_note").trim()
                                            if (notesTxt.isNotBlank()) text.append("\n\nNotes: \n").append(notesTxt)
                                            submit("daily", todayIso(), s, text.toString())
                                            afterOutTimeMarked(text.toString()) { finish() }
                                        }
                                    }
                                }
                              }   // 🔴 V509 — withPlaceNote শেষ
                            }
                        }
                    }
                    picked.startsWith("⏰") -> askPostponeTime()
                    picked.startsWith("🏖️") -> applyLeaveFlow()   // 🔵 B618: নতুন ছুটি-আবেদন ফ্লো (গণনা+দ্বন্দ্ব)
                    else -> render()
                }
            }
            .setOnCancelListener { render() }
            .show()
        try { NoBengali.installDialog(qmDlg); com.tkbiswas.pilesclinic.native.PremiumAlert.paint(qmDlg) } catch (_: Throwable) { }
    }

    // 🔴🆕🔒 B438 — "নির্ধারিত সময়" বেছে দেওয়ার জন্য আসল TimePickerDialog
    // (TK স্পষ্ট করেছেন — "দুই ঘণ্টা বললে দুই ঘণ্টাই বসিয়ে রাখবেন না,
    // নির্দিষ্ট সময় বসাতে পারার ব্যবস্থা রাখুন")। বাছা সময়েই রিমাইন্ডার
    // আবার আসবে (`AttendanceReminderScheduler.scheduleExactTime`) — ততক্ষণ
    // পর্যন্ত পুরনো ১০-মিনিট-পরপর চেইন থামিয়ে দেওয়া হয় (নইলে দুটো আলাদা
    // রিমাইন্ডার-চেইন একসাথে চলত)।
    private fun askPostponeTime() {
        val cal = java.util.Calendar.getInstance()
        android.app.TimePickerDialog(this, { _, hour, minute ->
            try {
                com.tkbiswas.pilesclinic.native.AttendanceReminderScheduler.scheduleExactTime(
                    applicationContext, quickMarkKind, hour, minute
                )
            } catch (_: Throwable) { }
            // 🔴 V1158 — দেখানোর ঘড়ি সবসময় "2.30 PM" ধাঁচে (২৪-ঘণ্টা নয়)।
            val label = displayTime12(String.format(java.util.Locale.US, "%02d:%02d", hour, minute))
            android.widget.Toast.makeText(this, NoBengali.s("ঠিক আছে, $label-এ আবার মনে করানো হবে"), android.widget.Toast.LENGTH_LONG).show()
            finish()
        }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), false).show()
    }

    // 🔴 B320 (03.08.2026, TK-নির্দেশ) — OUT TIME-এ দুই ধাপের নিশ্চিতকরণ।
    // ধাপ ১: "Are you sure you want to leave?" (Yes/No)। ধাপ ২ (শুধু Yes-এ):
    // কারণ জিজ্ঞাসা — "Office time over" / "Personal work — leaving early"।
    // যেকোনো একটা বাছলে তবেই check_out + কারণ সেভ হয়। ⛔ কোনো নতুন UI-প্যাটার্ন
    // তৈরি হয়নি — প্রজেক্টের বাকি মডিউলে (IncomeExpenseActivity.kt) যে সাধারণ
    // AlertDialog.Builder ব্যবহার হয় সেটাই পুনর্ব্যবহার করা হলো।
    // 🔴 B323 (03.08.2026, TK-নির্দেশ) — ছুটি মার্ক করার সময় ছোট কারণ বাধ্যতামূলক
    // (TK নিজে বেছেছেন)। খালি রেখে Save করা যাবে না।
    private fun askLeaveReason() {
        // 🎨🔒 B512 (06.08.2026, TK-নির্দেশে মকআপ "B" দেখিয়ে অনুমোদনের
        // পরে) — তিনটে সাধারণ কারণ চিপ আকারে (Sick/Personal/Festival),
        // চাপলেই লেখার বক্সে বসে যায় (এডিট করাও যায়), তারপর Save। ⛔
        // "খালি রেখে Save করা যাবে না" (B323) নিয়ম অক্ষত।
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(10), dp(20), dp(4)) }
        val input = ModuleUi.input(this, "Reason (e.g. Sick, Personal, Festival)")
        fun chip(icon: String, label: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            isClickable = true; isFocusable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#F7F9FB"))
                setStroke(1, android.graphics.Color.parseColor("#E5E9ED"))
                cornerRadius = dp(10).toFloat()
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(8)
            layoutParams = lp
            addView(TextView(this@WorkNotebookActivity).apply {
                text = icon; textSize = 13f
                setPadding(0, 0, 0, 0)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FEF3E2")); cornerRadius = dp(7).toFloat()
                }
                this.layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).also { it.marginEnd = dp(8) }
                gravity = android.view.Gravity.CENTER
            })
            addView(TextView(this@WorkNotebookActivity).apply { text = label; textSize = 12.5f; setTextColor(android.graphics.Color.parseColor("#374151")) })
            setOnClickListener { input.setText(label); input.setSelection(input.text.length) }
        }
        box.addView(chip("🤒", "Sick"))
        box.addView(chip("🙋", "Personal"))
        box.addView(chip("🎉", "Festival"))
        box.addView(TextView(this).apply {
            text = NoBengali.s("অথবা নিজের কারণ লিখুন"); textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#6B7280"))
            setPadding(0, dp(2), 0, dp(4))
        })
        box.addView(spacedField(input))
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "🏖️ Mark Today as Leave"))
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isBlank()) { ModuleUi.toast(this, "Reason required"); return@setPositiveButton }
                day.put("is_leave", true); day.put("leave_reason", reason)
                markReminderFlag("in", true); markReminderFlag("out", true)
                saveDay { render() }
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    // 🔵🔒 B618 (11.08.2026, TK-নির্দেশ, ধাপে-ধাপে আলোচনা করে ফাইনাল + প্রুফ-অনুমোদিত):
    // ছুটির আবেদন — আজ বা অগ্রিম যেকোনো তারিখ (অতীত নয়)। **সরাসরি ছুটি** যদি:
    //   (ক) ওই মাসে confirmed ছুটি ৪টার কম, এবং (খ) ওই দিন ব্রাঞ্চে অন্য স্টাফ
    //   confirmed ছুটিতে নেই। এর যেকোনো একটা ভাঙলে (৫ম+ বা একই-দিনে দ্বিতীয়) →
    //   "pending" (ডাক্তার/মাস্টার Approve করলে তবেই — সেই অংশ পরের ধাপে)।
    // confirmed হলে: wn.notebook_days-এ is_leave (পুরনো হাজিরা/রিপোর্ট অটুট),
    //   ব্রাঞ্চে নোটিশ (Briefing target=branch → সবাই দেখবে), WhatsApp জোর।
    // ⛔ পুরনো একটাও টেবিল/হিসাব বদলায়নি — নতুন wn.leave_requests-এ লেখা।
    private var pendingLeaveDate: String = ""
    private var pendingLeaveToDate: String = ""
    // 🏖️🔒 V1336 (11.09.2026, TK-নির্দেশ) — একসাথে একাধিক দিনের ছুটি চাওয়া
    // যায়, প্রতিটা দিন পুরনো নিয়মেই (মাসে-৪, একই-দিনে-দুজন, চেম্বার-দিন)
    // আলাদাভাবে যাচাই হয়ে confirmed/pending ঠিক হয় — একদিনের আবেদন আগের
    // মতোই একদম অপরিবর্তিত থাকে (From=To হলে পুরনো পথই চলে)।
    private fun applyLeaveFlow() {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        pendingLeaveDate = todayIso()
        pendingLeaveToDate = todayIso()
        /* 🏖️🔒 V740 — চেম্বার-দিনের তালিকা **আলাদা থ্রেডে** আগেভাগে এনে রাখি।
           ⚠️ মূল থ্রেডে মেঘে গেলে Android অ্যাপ থামিয়ে দেয়, তাই এভাবে।
           ⛔ না এলেও কিছু ভাঙে না — বাঁধা তালিকাই কাজ করে। */
        val leaveBranch = NativeSession.current(this)?.branch ?: ""
        Thread {
            try { com.tkbiswas.pilesclinic.native.LeaveChamberDays.preload(leaveBranch) } catch (_: Throwable) { }
        }.start()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(10), dp(20), dp(4)) }
        box.addView(TextView(this).apply {
            text = NoBengali.s("From (কোন তারিখ থেকে ছুটি)"); textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#6B7280")); setPadding(0, 0, 0, dp(4))
        })
        /* 🏖️🔒 V740 (TK-অনুমোদিত ডেমো-প্রুফ) — চেম্বারের দিন বাছলে **আগেই**
           জানিয়ে দেওয়া হয়, স্টাফ পাঠানোর পরে অবাক হবেন না।
           ⛔ আটকানো হয় না — TK-এর সিদ্ধান্ত "২": আটকাবে, তবে অনুমতি চাওয়া যাবে।
           ⛔ লেখা ইংরেজিতে (TK-নির্দেশ)। ⛔ `isChamberDateNoNet` কখনো নেটে যায় না।
           ⛔ এখানেই ঘোষণা — নিচের তারিখ-বাছার listener এটা ব্যবহার করে, আর
              Kotlin-এ স্থানীয় ফাংশন ব্যবহারের আগে ঘোষণা করতেই হয়। */
        val chamberWarn = TextView(this).apply {
            textSize = 11.5f
            setTextColor(android.graphics.Color.parseColor("#B42318"))
            setPadding(0, 0, 0, dp(8))
            visibility = android.view.View.GONE
        }
        fun refreshChamberWarn() {
            val isCh = try {
                com.tkbiswas.pilesclinic.native.LeaveChamberDays
                    .isChamberDateNoNet(leaveBranch, pendingLeaveDate)
            } catch (_: Throwable) { false }
            chamberWarn.visibility =
                if (isCh) android.view.View.VISIBLE else android.view.View.GONE
            if (isCh) chamberWarn.text =
                "Chamber day — doctor sits. Master's permission will be needed."
        }

        val dateTv = TextView(this).apply {
            text = dotDate(pendingLeaveDate); textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#0A5C33")); setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(12), dp(11), dp(12), dp(11)); isClickable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#EAF6EE")); setStroke(1, android.graphics.Color.parseColor("#BFE0CB")); cornerRadius = dp(9).toFloat()
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10); layoutParams = lp
        }
        // 🏖️ V1336 — "To" দেখানোর জন্য আগেভাগে ঘোষণা (নিচে dateTv-এর listener এটা ব্যবহার করে)
        lateinit var toDateTv: TextView
        dateTv.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            val parts = pendingLeaveDate.split("-")
            try { cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()) } catch (_: Throwable) { }
            val dpd = android.app.DatePickerDialog(this, { _, y, mo, dd ->
                pendingLeaveDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, mo + 1, dd)
                dateTv.text = dotDate(pendingLeaveDate)
                refreshChamberWarn()   // 🏖️ V740
                // 🏖️ V1336 — From, To-এর পরে চলে গেলে To-ও এগিয়ে দিই, নইলে "শেষ তারিখ শুরুর আগে" হয়ে যেত
                // ⛔ toDateTv ততক্ষণে সবসময়ই বসানো থাকে — dateTv-এর ক্লিক
                // শুধু dialog দেখানোর পরে (নিচে toDateTv তৈরি হয়ে যাওয়ার পরে)
                // ব্যবহারকারী চাপলেই চলে, তাই lateinit-এর নিরাপদ ধরে-নেওয়া।
                if (pendingLeaveToDate < pendingLeaveDate) {
                    pendingLeaveToDate = pendingLeaveDate
                    toDateTv.text = dotDate(pendingLeaveToDate)
                }
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
            try { dpd.datePicker.minDate = System.currentTimeMillis() - 60000 } catch (_: Throwable) { }
            /* 🏖️🔒 V740 (২৭.০৮.২০২৬, TK-নির্দেশ) — **১০ দিনের সীমা**।
               TK: *"দশদিন আগে থেকেও যেন সে ইনফর্ম করতে পারে"* — অর্থাৎ ১০ দিন
               পর্যন্ত আগাম বলে রাখার সুবিধা। তাই বাছার পর্দাতেই সীমা বসানো হলো,
               স্টাফ ভুল দিন বেছে ফেলে পরে "হবে না" শুনবেন না।
               ⛔ আজকের ছুটি নেওয়া আগের মতোই খোলা (minDate বদলায়নি)। */
            try {
                dpd.datePicker.maxDate =
                    System.currentTimeMillis() + 10L * 24L * 60L * 60L * 1000L
            } catch (_: Throwable) { }
            dpd.show()
        }
        box.addView(dateTv)
        box.addView(chamberWarn)
        refreshChamberWarn()
        box.addView(TextView(this).apply {
            text = NoBengali.s("To (একদিনের ছুটি হলে From-এর মতোই রাখুন)"); textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#6B7280")); setPadding(0, dp(8), 0, dp(4))
        })
        toDateTv = TextView(this).apply {
            text = dotDate(pendingLeaveToDate); textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#0A5C33")); setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(12), dp(11), dp(12), dp(11)); isClickable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#EAF6EE")); setStroke(1, android.graphics.Color.parseColor("#BFE0CB")); cornerRadius = dp(9).toFloat()
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(10); layoutParams = lp
        }
        toDateTv.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            val parts = pendingLeaveToDate.split("-")
            try { cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt()) } catch (_: Throwable) { }
            val dpd = android.app.DatePickerDialog(this, { _, y, mo, dd ->
                val picked = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, mo + 1, dd)
                // ⛔ V1336 — To কখনো From-এর আগে হতে পারবে না
                pendingLeaveToDate = if (picked < pendingLeaveDate) pendingLeaveDate else picked
                toDateTv.text = dotDate(pendingLeaveToDate)
            }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH))
            try {
                val fromParts = pendingLeaveDate.split("-")
                val fromCal = java.util.Calendar.getInstance()
                fromCal.set(fromParts[0].toInt(), fromParts[1].toInt() - 1, fromParts[2].toInt())
                dpd.datePicker.minDate = fromCal.timeInMillis
            } catch (_: Throwable) { }
            try {
                dpd.datePicker.maxDate =
                    System.currentTimeMillis() + 10L * 24L * 60L * 60L * 1000L
            } catch (_: Throwable) { }
            dpd.show()
        }
        box.addView(toDateTv)
        val input = ModuleUi.input(this, "Reason (e.g. Sick, Personal, Festival)")
        fun chip(icon: String, label: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9)); isClickable = true; isFocusable = true
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#F7F9FB")); setStroke(1, android.graphics.Color.parseColor("#E5E9ED")); cornerRadius = dp(10).toFloat()
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(8); layoutParams = lp
            addView(TextView(this@WorkNotebookActivity).apply {
                text = icon; textSize = 13f
                background = android.graphics.drawable.GradientDrawable().apply { setColor(android.graphics.Color.parseColor("#FEF3E2")); cornerRadius = dp(7).toFloat() }
                this.layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).also { it.marginEnd = dp(8) }
                gravity = android.view.Gravity.CENTER
            })
            addView(TextView(this@WorkNotebookActivity).apply { text = label; textSize = 12.5f; setTextColor(android.graphics.Color.parseColor("#374151")) })
            setOnClickListener { input.setText(label); input.setSelection(input.text.length) }
        }
        box.addView(chip("🤒", "Sick"))
        box.addView(chip("🙋", "Personal"))
        box.addView(chip("🎉", "Festival"))
        box.addView(spacedField(input))
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("🏖️ Apply for Leave")))
            .setView(box)
            .setPositiveButton(NoBengali.s("ছুটির আবেদন করুন")) { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isBlank()) { ModuleUi.toast(this, "Reason required"); return@setPositiveButton }
                submitLeaveRange(pendingLeaveDate, pendingLeaveToDate, reason)
            }
            .setNegativeButton("Cancel", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    // 🏖️🔒 V1336 — একদিনের ফলাফল বহনকারী ছোট ক্লাস (range আর একদিন — দুটোতেই একই কোর ব্যবহার হয়)
    private data class LeaveDayOutcome(val date: String, val status: String, val needPretty: String, val ok: Boolean)

    private fun submitLeaveApplication(leaveDate: String, reason: String) {
        submitLeaveRange(leaveDate, leaveDate, reason)
    }

    // 🏖️🔒 V1336 (11.09.2026, TK-নির্দেশ) — একসাথে একাধিক দিনের ছুটি।
    // From..To-এর প্রতিটা দিন পুরনো নিয়মেই (নিচের processLeaveDay) আলাদাভাবে
    // যাচাই হয়, তারপর একটাই মিলিত ফলাফল দেখানো হয়। From==To (একদিন) হলে
    // ফলাফল-বার্তা ও WhatsApp শেয়ার আগের একদিনের ফরম্যাটের মতোই থাকে।
    private fun submitLeaveRange(fromDate: String, toDate: String, reason: String) {
        ModuleUi.toast(this, "Checking...")
        val br = NativeSession.current(this)?.branch ?: ""
        Thread {
            val dates = mutableListOf<String>()
            try {
                val cal = java.util.Calendar.getInstance()
                val p = fromDate.split("-"); cal.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt())
                val toCal = java.util.Calendar.getInstance()
                val q = toDate.split("-"); toCal.set(q[0].toInt(), q[1].toInt() - 1, q[2].toInt())
                var guard = 0
                // ⛔ guard<40 — সর্বোচ্চ দিন-বাছার সীমা (১০ দিন আগাম) নিজেই ছোট রাখে, এটা শুধু অসীম-লুপ ঠেকানোর সুরক্ষা
                while (!cal.after(toCal) && guard < 40) {
                    dates.add(String.format(java.util.Locale.US, "%04d-%02d-%02d",
                        cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH)))
                    cal.add(java.util.Calendar.DAY_OF_MONTH, 1); guard++
                }
            } catch (_: Throwable) { }
            if (dates.isEmpty()) dates.add(fromDate)
            val outcomes = dates.map { processLeaveDay(it, reason, br) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val confirmed = outcomes.filter { it.ok && it.status == "confirmed" }
                val pending = outcomes.filter { it.ok && it.status == "pending" }
                val failed = outcomes.count { !it.ok }
                if (confirmed.any { it.date == todayIso() }) {
                    day.put("is_leave", true); day.put("leave_reason", reason)
                    markReminderFlag("in", true); markReminderFlag("out", true)
                }
                if (outcomes.isNotEmpty() && failed == outcomes.size) {
                    ModuleUi.toast(this, "Net সমস্যা — আবার চেষ্টা করুন"); return@runOnUiThread
                }
                if (dates.size == 1) {
                    // ⛔ একদিনের আবেদন — আগের মতোই একদম অপরিবর্তিত বার্তা/ব্যবহার
                    val o = outcomes.first()
                    if (o.status == "confirmed") {
                        val shareText = "🏖️ Leave\nStaff: $staffCode\nBranch: $br\nDate: " + dotDate(o.date) + "\nReason: " + reason
                        com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, shareText) { render() }
                    } else {
                        ModuleUi.toast(this, NoBengali.s("ছুটির অনুরোধ পাঠানো হয়েছে — Pending"))
                        render()
                    }
                    return@runOnUiThread
                }
                val msg = StringBuilder()
                if (confirmed.isNotEmpty()) msg.append("✅ ${confirmed.joinToString(", ") { dotDate(it.date) }} — auto-approved\n")
                pending.forEach { msg.append("⏳ ${dotDate(it.date)} — needs Master's approval (${it.needPretty})\n") }
                if (failed > 0) msg.append("⚠️ $failed day(s) failed — please retry\n")
                ModuleUi.toast(this, NoBengali.s(msg.toString().trim()))
                if (confirmed.isNotEmpty()) {
                    val shareText = "🏖️ Leave\nStaff: $staffCode\nBranch: $br\nDates: " +
                        confirmed.joinToString(", ") { dotDate(it.date) } + "\nReason: " + reason +
                        if (pending.isNotEmpty()) "\n(+ ${pending.size} day(s) pending Master approval)" else ""
                    com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, shareText) { render() }
                } else {
                    render()
                }
            }
        }.start()
    }

    private fun processLeaveDay(leaveDate: String, reason: String, br: String): LeaveDayOutcome {
            val enc = { s: String -> try { java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20") } catch (_: Throwable) { s } }
            val ym = leaveDate.substring(0, 7)
            // ⛔ B618 ঠিক (11.08.2026): আগে উপরের সীমা "$ym-32" ছিল — Postgres date কলামে
            // '2026-08-32' পার্স-এরর দেয় (যাচাই করা), তাই query fail → count 0 → মাসে ৪-এর
            // নিয়ম কখনো চালু হতো না। এখন পরের মাসের ১ তারিখ (বৈধ তারিখ) দিয়ে সীমা।
            val nextMonthFirst = run {
                val y = ym.substring(0, 4).toInt(); val mo = ym.substring(5, 7).toInt()
                val ny = if (mo == 12) y + 1 else y; val nm = if (mo == 12) 1 else mo + 1
                "%04d-%02d-01".format(ny, nm)
            }
            // ১) এ মাসে নিজের confirmed ছুটির সংখ্যা
            val monthCount = try {
                ModuleAuth.getRows("wn", "leave_requests",
                    "select=id&staff_code=eq.${enc(staffCode)}&status=eq.confirmed&leave_date=gte.$ym-01&leave_date=lt.$nextMonthFirst").length()
            } catch (_: Throwable) { 0 }
            // ২) একই দিনে ব্রাঞ্চে অন্য স্টাফ confirmed ছুটিতে?
            val conflict = try {
                ModuleAuth.getRows("wn", "leave_requests",
                    "select=id&branch=eq.${enc(br)}&leave_date=eq.$leaveDate&status=eq.confirmed&staff_code=neq.${enc(staffCode)}").length() > 0
            } catch (_: Throwable) { false }
            /* 🏖️🔒 V740 (২৭.০৮.২০২৬, TK-নির্দেশ) — **চেম্বারের দিনে ছুটি নেই**।
               TK: *"চেম্বারের তারিখের দিন অন্তত কেউ ছুটি পাবে না।"* এবং পরে —
               চেম্বারের দিনে *"আটকাবে, কিন্তু মাস্টারের অনুমতি চাইতে পারবে"* (TK "২")।
               ⇒ তাই অন্য দুটো নিয়মের মতোই **pending** করা হয়, একদম বন্ধ নয়।
               ⛔ পুরনো দুটো নিয়ম (৪ দিন · একই দিনে দুজন) এক অক্ষরও বদলায়নি।
               ⛔ ব্রাঞ্চ চেনা না গেলে `isChamberDate` **false** দেয় — অচেনা
                  কারণে কারও ছুটি আটকে যায় না। */
            val chamberDay = try {
                com.tkbiswas.pilesclinic.native.LeaveChamberDays.isChamberDate(br, leaveDate)
            } catch (_: Throwable) { false }
            val needList = mutableListOf<String>()
            if (monthCount >= 4) needList.add("5th")
            if (conflict) needList.add("conflict")
            if (chamberDay) needList.add("chamber")
            val needReason = needList.joinToString("+")
            val needPretty = needList.joinToString(" + ") {
                when (it) {
                    "chamber" -> "Chamber day"
                    "conflict" -> "Colleague on leave"
                    "5th" -> "5th day this month"
                    else -> it
                }
            }
            val status = if (needReason.isEmpty()) "confirmed" else "pending"
            val row = JSONObject()
                .put("staff_code", staffCode).put("staff_mobile", mobile).put("staff_name", staffCode)
                .put("branch", br).put("leave_date", leaveDate).put("reason", reason)
                .put("status", status).put("need_reason", needReason).put("created_by", mobile)
                .put("updated_at", nowIso())
            val ok = try { ModuleAuth.upsertOnConflict("wn", "leave_requests", row, "staff_code,leave_date") } catch (_: Throwable) { false }
            if (ok && status == "confirmed") {
                // পুরনো হাজিরা অটুট রাখতে ওই তারিখের notebook_days-এ is_leave
                try {
                    val ndRow = JSONObject().put("staff_code", staffCode).put("staff_mobile", mobile)
                        .put("work_date", leaveDate).put("is_leave", true).put("leave_reason", reason).put("updated_at", nowIso())
                    ModuleAuth.upsertOnConflict("wn", "notebook_days", ndRow, "staff_code,work_date")
                } catch (_: Throwable) { }
                // ব্রাঞ্চে নোটিশ — সবাই (স্টাফ+ডাক্তার+মাস্টার) দেখবে
                try {
                    val bmsg = "👤 Staff : ${staffCode.ifBlank { mobile }}\n🏥 Branch : $br\n🏖️ Leave : " + dotDate(leaveDate) + "\nReason : " + reason
                    com.tkbiswas.pilesclinic.native.BriefingRepository().post(this, "Staff Leave", bmsg, "branch", br, "", mobile)
                } catch (_: Throwable) { }
            }
            if (ok && status == "pending") {
                // 🔵 B618: pending হলে "Leave request" নোটিশ — ব্রাঞ্চের ডাক্তার ও
                // মাস্টারের ঘন্টায় যাবে (target=branch; মাস্টার সব ব্রাঞ্চ দেখে)।
                // Approve/Reject বোতাম BriefingActivity-তে (পরের অংশ)।
                try {
                    // ⚠️ B618 ঠিক (11.08.2026): ওয়েবের approval-বেল (wlv1NoticeField) emoji-হীন
                    // ছোট-হাতের "key :" লাইন খোঁজে আর leave_date ISO ধরে টেবিলে মেলায় — তাই
                    // Android-এ পাঠানো এই বার্তাও পরিষ্কার ISO লাইন রাখি, যাতে কম্পিউটার থেকেও
                    // (মাস্টার/ডাক্তার) Android-এর ছুটি Approve করা যায়।
                    /* 🔴🔒 V936 (TK-নির্দেশ — এক ফরম্যাট) — আগে এখানে কাঁচা ISO
                       (`2026-09-05`) লেখা হত **ইচ্ছে করেই**, কারণ কম্পিউটারের
                       approval-বেল ওই লাইনটা পড়ে। এখন কম্পিউটারের পড়ার কোড
                       (`wlv1IsoDate`) বিন্দু-ধাঁচও বোঝে, তাই মানুষের জন্য
                       `05.09.2026` লেখা যায়। পুরনো অনুরোধগুলোও আগের মতোই চলে। */
                    val rmsg = "Staff : ${staffCode.ifBlank { mobile }}\nBranch : $br\nLeave date : " + dotDate(leaveDate) +
                        /* 🏖️🔒 V740 — কারণটা **পড়ার মতো ইংরেজিতে**। ⛔ "Need" লাইনটা
                           কেউ মেশিনে পড়ে না (যাচাই করা — ওয়েব শুধু Staff · Leave date ·
                           Branch · Reason পড়ে), তাই এটা বদলানো নিরাপদ, আর এতে
                           **ফোন ও কম্পিউটার দুই জায়গাতেই** একই লেখা দেখায়। */
                        "\nReason : " + reason + "\nNeed : " + needPretty
                    com.tkbiswas.pilesclinic.native.BriefingRepository().post(this, "Leave request", rmsg, "branch", br, "", mobile)
                } catch (_: Throwable) { }
                // 🔵 B618: এই pending তারিখ লোকালে রাখি — পরে Approve হলে স্টাফের
                // ফোনে WhatsApp জোর করে খোলা হবে (checkPendingLeaves)।
                try { addPendingLeaveDate(leaveDate) } catch (_: Throwable) { }
            }
            return LeaveDayOutcome(leaveDate, status, needPretty, ok)
    }

    /* ═══════════════════════════════════════════════════════════════════
       🏖️🔒 V740 (২৭.০৮.২০২৬) — **আগাম নেওয়া ছুটি দেখা ও বাতিল করা**
       —————————————————————————————————————————————————————————————————
       TK: *"নিজেই বাতিল করতে পারবে — তাহলে ওই দিনটা অন্য সহকর্মীর জন্য খালি
       হয়ে যাবে, আর তার ৪ দিনের হিসাবেও ফেরত আসবে।"*

       ⛔ **আজ ও তার পরের** ছুটিই দেখানো/বাতিল করা যায় — পুরনো দিনের হাজিরার
          হিসাব কেউ বদলাতে পারবে না।
       ⛔ বাতিল করলে `status = 'cancelled'` বসে। পুরনো গোনার শর্ত
          `status=eq.confirmed`, তাই ওই দিনটা **নিজে থেকেই** ৪ দিনের হিসাব ও
          "একই দিনে দুজন"-এর হিসাব — দুটো থেকেই বেরিয়ে যায়। **নতুন কোনো
          হিসাব লেখার দরকারই হয়নি**, তাই পুরনো কিছু ভাঙার ঝুঁকিও নেই।
       ⛔ ছুটি মঞ্জুর হয়ে থাকলে হাজিরা-খাতার `is_leave`-ও ফিরিয়ে দেওয়া হয়,
          নইলে খাতায় ভুল করে "LEAVE" লেখা থেকে যেত।
       ⛔ শুধু **নিজের** সারি — `staff_code` নিজেরটাই পাঠানো হয়।
       ═══════════════════════════════════════════════════════════════════ */
    private fun upcomingLeaveScreen() {
        ModuleUi.toast(this, "Loading...")
        Thread {
            val encQ = { x: String -> try { java.net.URLEncoder.encode(x, "UTF-8").replace("+", "%20") } catch (_: Throwable) { x } }
            val rows = try {
                ModuleAuth.getRows("wn", "leave_requests",
                    "select=id,leave_date,status,reason&staff_code=eq." + encQ(staffCode) +
                        "&leave_date=gte." + todayIso() + "&order=leave_date.asc")
            } catch (_: Throwable) { org.json.JSONArray() }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                val dm = resources.displayMetrics.density
                fun dpx(v: Int) = (v * dm).toInt()
                val box = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dpx(20), dpx(8), dpx(20), dpx(4))
                }
                var shown = 0
                for (i in 0 until rows.length()) {
                    val r = rows.optJSONObject(i)
                    if (r != null) {
                        val st = r.optString("status", "")
                        // ⛔ শুধু চালু ছুটি — বাতিল/নামঞ্জুর হয়ে যাওয়াগুলো দেখিয়ে লাভ নেই
                        if (st == "confirmed" || st == "pending") {
                            shown++
                            val dt = r.optString("leave_date", "")
                            val rid = r.optString("id", "")
                            box.addView(TextView(this).apply {
                                text = dotDate(dt) + "  ·  " +
                                    (if (st == "confirmed") "Approved" else "Waiting for Master")
                                textSize = 14f
                                setTypeface(typeface, android.graphics.Typeface.BOLD)
                                setTextColor(android.graphics.Color.parseColor(
                                    if (st == "confirmed") "#0A5C33" else "#9A5B00"))
                                setPadding(0, dpx(10), 0, 0)
                            })
                            val why = r.optString("reason", "")
                            if (why.isNotBlank()) box.addView(TextView(this).apply {
                                text = why; textSize = 11.5f
                                setTextColor(android.graphics.Color.parseColor("#6B7280"))
                            })
                            box.addView(ModuleUi.buttonSoft(this, "Cancel this leave") {
                                confirmCancelUpcomingLeave(rid, dt, st)
                            })
                        }
                    }
                }
                if (shown == 0) box.addView(TextView(this).apply {
                    text = "No upcoming leave."
                    textSize = 13f
                    setTextColor(android.graphics.Color.parseColor("#6B7280"))
                    setPadding(0, dpx(8), 0, dpx(8))
                })
                val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "My Upcoming Leave"))
                    .setView(android.widget.ScrollView(this).apply { addView(box) })
                    .setPositiveButton("Close", null)
                    .create()
                dlg.show()
                try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
                try {
                    NoBengali.installDialog(dlg)
                    com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg)
                } catch (_: Throwable) { }
            }
        }.start()
    }

    private fun confirmCancelUpcomingLeave(id: String, dateIso: String, status: String) {
        if (id.isBlank()) { ModuleUi.toast(this, "Could not read this leave"); return }
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Cancel Leave"))
            .setMessage("Cancel leave on " + dotDate(dateIso) + "?\n\n" +
                "The day becomes free for a colleague, and it comes back to your monthly count.")
            .setPositiveButton("Yes, cancel") { _, _ -> doCancelUpcomingLeave(id, dateIso, status) }
            .setNegativeButton("Keep it", null)
            .create()
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(dlg) } catch (_: Throwable) { }   // 🤫 V774
        try {
            NoBengali.installDialog(dlg)
            com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg)
        } catch (_: Throwable) { }
    }

    private fun doCancelUpcomingLeave(id: String, dateIso: String, status: String) {
        ModuleUi.toast(this, "Cancelling...")
        Thread {
            val encQ = { x: String -> try { java.net.URLEncoder.encode(x, "UTF-8").replace("+", "%20") } catch (_: Throwable) { x } }
            val patch = org.json.JSONObject()
                .put("status", "cancelled")
                .put("decided_by", mobile)
                .put("decided_at", nowIso())
                .put("updated_at", nowIso())
            val ok = try {
                ModuleAuth.update("wn", "leave_requests", "id=eq." + encQ(id), patch)
            } catch (_: Throwable) { false }
            // ⛔ মঞ্জুর হয়ে থাকলে হাজিরা-খাতার "LEAVE" চিহ্নটাও ফিরিয়ে দিই,
            //    নইলে খাতায় ভুল করে ছুটি লেখা থেকে যেত।
            if (ok && status == "confirmed") {
                try {
                    val ndRow = org.json.JSONObject()
                        .put("staff_code", staffCode).put("staff_mobile", mobile)
                        .put("work_date", dateIso).put("is_leave", false)
                        .put("leave_reason", "").put("updated_at", nowIso())
                    ModuleAuth.upsertOnConflict("wn", "notebook_days", ndRow, "staff_code,work_date")
                } catch (_: Throwable) { }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (!ok) { ModuleUi.toast(this, "Net problem — try again"); return@runOnUiThread }
                ModuleUi.toast(this, "Leave cancelled")
                // আজকের ছুটি বাতিল হলে পর্দার আজকের অবস্থাও ঠিক করে দিই
                if (dateIso == todayIso()) {
                    try { day.put("is_leave", false); day.put("leave_reason", "") } catch (_: Throwable) { }
                }
                render()
            }
        }.start()
    }

    // 🔵🔒 B618: নিজের pending ছুটির তারিখ লোকালে জমা — Approve হলে WhatsApp জোর।
    private fun nbLeavePendPrefs() = getSharedPreferences("leave_pending_$staffCode", MODE_PRIVATE)
    private fun addPendingLeaveDate(date: String) {
        val p = nbLeavePendPrefs()
        val cur = (p.getString("dates", "") ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
        cur.add(date); p.edit().putString("dates", cur.joinToString(",")).apply()
    }
    // পাতা খোলায় নিজের pending তারিখগুলোর status দেখি — confirmed হলে WhatsApp জোর,
    // rejected হলে Toast, তারপর তালিকা থেকে বাদ। ⛔ pending না থাকলে কোনো ক্লাউড-কল
    // হয় না (egress-হালকা)। পড়া ব্যর্থ হলে তারিখ রেখে দিই (পরে আবার)।
    private fun checkPendingLeaves() {
        val p = nbLeavePendPrefs()
        val dates = (p.getString("dates", "") ?: "").split(",").filter { it.isNotBlank() }
        if (dates.isEmpty()) return
        Thread {
            val enc = { s: String -> try { java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20") } catch (_: Throwable) { s } }
            val keep = mutableSetOf<String>()
            var approvedShare: String? = null
            var rejectedDate: String? = null
            for (dt in dates) {
                val rows = try {
                    ModuleAuth.getRows("wn", "leave_requests", "select=status,reason,branch&staff_code=eq.${enc(staffCode)}&leave_date=eq.$dt&limit=1")
                } catch (_: Throwable) { org.json.JSONArray() }
                if (rows.length() == 0) { keep.add(dt); continue }
                val r = rows.getJSONObject(0)
                when (r.optString("status")) {
                    "confirmed" -> approvedShare = "🏖️ Leave Approved\nStaff: $staffCode\nBranch: ${r.optString("branch")}\nDate: " + dotDate(dt) + "\nReason: " + r.optString("reason")
                    "rejected" -> rejectedDate = dt
                    else -> keep.add(dt)
                }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                p.edit().putString("dates", keep.joinToString(",")).apply()
                if (rejectedDate != null) ModuleUi.toast(this, "Leave rejected (" + dotDate(rejectedDate!!) + ") — please come to work")
                val share = approvedShare
                if (share != null) {
                    try { com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, share) { } } catch (_: Throwable) { }
                }
            }
        }.start()
    }

    // 🔴🆕🔒 B465 (05.08.2026, TK-নির্দেশ — "Staff IN TIME করে কিন্তু
    // মাস্টারের কাছে নোটিফিকেশন আসছে না... WhatsApp জোর করে অটোমেটিক ওপেন
    // হওয়ার কথা ছিল, তাও হচ্ছে না")। **আসল কারণ (কোড ধরে):** WhatsApp-শেয়ার
    // (B438) আগে শুধু নোটিফিকেশন থেকে "quick mark" পথে ছিল — সরাসরি স্ক্রিন
    // খুলে IN TIME বোতাম চাপলে (স্বাভাবিক ব্যবহার) WhatsApp খুলতই না। আর
    // Master-নোটিফিকেশন কোনো পথেই ছিল না। **সমাধান:** এই একটা শেয়ার্ড
    // ফাংশন — (১) Master-কে Briefing/ঘন্টা-নোটিশ পাঠায় (প্রজেক্টের আগে
    // থেকে থাকা `BriefingRepository().post(..., target="role", role=
    // "master", ...)` — Refund request-এর (PaymentRepository.kt) হুবহু একই
    // প্রমাণিত পথ পুনর্ব্যবহার, নতুন কিছু বানানো হয়নি), (২) WhatsApp জোর
    // করে খোলে (আগে থেকে থাকা `WhatsAppMessageChooser.sendGeneric`)। এখন
    // **দুটো IN TIME পথেই** (নোটিফিকেশন থেকে + সরাসরি বোতাম চেপে) এই একই
    // ফাংশন ডাকা হয়, তাই কখনো আলাদা হবে না। ⛔ ব্যর্থ হলেও (নেট না থাকলে)
    // নিঃশব্দে বাদ — IN TIME সেভ হওয়াটা কখনো এর জন্য আটকায় না।
    private fun afterInTimeMarked(then: () -> Unit) {
        // 🏍️🔒 V1516 — IN TIME হলেই এখান থেকেই tracking automatic শুরু। দুটো IN TIME
        //    পথেই (বোতাম ও নোটিফিকেশন) এই ফাংশনই ডাকা হয়, তাই কখনো আলাদা হবে না।
        try {
            val fv = com.tkbiswas.pilesclinic.native.FieldVisit
            /* 🏍️🔒 V977 (০২.০৯.২০২৬, TK-নির্দেশ) — *"IN TIME চাপলেই GPS চালু হয়ে
               যাবে"* · *"ফিল্ডে যাবে কি না, সমস্ত কথা জিজ্ঞাসা করার দরকার নেই,
               একটা বিভ্রান্ত হয়ে যেতে পারে"* ⇒ At Chamber / Field Visit বাছাইটা
               তুলে দেওয়া হলো; IN TIME-এই গোনা শুরু।
               🛰️🔒 V1346 (১১.০৯.২০২৬, TK-নির্দেশ) — আগে এখানে `isFieldStaff`
               (শুধু RUPAM) ছিল। TK এখন সব আসল স্টাফের (branch-সহ) ফোনেই
               IN TIME থেকে OUT TIME পর্যন্ত লোকেশন-অন প্রম্পট চেয়েছেন —
               `tracksAttendanceLocation()`-এ বদলানো হলো। ডাক্তারের কোনো
               IN TIME নেই বলে এই পথ তাঁর জন্য চলেই না (আলাদা ব্যবস্থা)। */
            if (fv.tracksAttendanceLocation(this) && !fv.isRunning(this)) {
                fv.startDay(this, staffCode.ifBlank { mobile }, branch)
                com.tkbiswas.pilesclinic.native.FieldVisitControl.start(this)
                /* 🔴🔒 V1076 (০৪.০৯.২০২৬, TK-নির্দেশ: *"In time চাপলেই যেন কাজ হয়"*)
                   — এতদিন `startDay()` দিনের সারিটা **শুধু ফোনের ভিতরে** বসাত;
                   সার্ভারে সারিটা লিখত GPS-সেবা, চালু হওয়ার ৩ মিনিট পর। Location
                   অনুমতি না থাকলে Android ওই সেবাটাই চালাতে দেয় না ⇒ সার্ভারে
                   একটাও সারি যেত না ⇒ TK-এর Field Visit Tracking ফাঁকা থাকত
                   (খাতার সারি ১৩৯ ও ১৭৭ — TK দুবার বলেছেন)।
                   ⇒ এখন IN TIME চাপার সঙ্গে সঙ্গেই সার্ভারে সারিটা বসে।
                   ⛔ ব্যাকগ্রাউন্ডে, তাই IN TIME সেভ হওয়া এর জন্য থমকায় না;
                      নেট না থাকলে নিঃশব্দে বাদ, GPS-সেবা পরে আবার লিখবে।
                   ⛔ `upsert` (staff_code + work_date) — একই দিনে দুবার চাপলেও
                      দ্বিতীয় সারি তৈরি হয় না, পুরনোটাই হালনাগাদ হয়। */
                val fvCtx = applicationContext
                Thread { try { fv.push(fvCtx, ended = false, auto = false) } catch (_: Throwable) { } }.start()
                /* 🛰️ V1156 — গোনা শুরু হলো, কিন্তু অনুমতি না থাকলে একটাও
                   অবস্থান আসবে না। তাই এখানেই একবার অনুমতি চাওয়া হয়।
                   ⛔ IN TIME ইতিমধ্যেই সেভ — এই বাক্স তাতে হাত দেয় না। */
                ensureFieldLocationReady(fromButton = false)
            }
        } catch (_: Throwable) { }
        try {
            // 🎨🔒 B513 (06.08.2026, TK-নির্দেশ — "সম্পূর্ণ প্রজেক্টে
            // যেখানে যেখানে নোটিফিকেশন প্লেইন-টেক্সট, প্রফেশনাল বানাতে
            // হবে") — এক লম্বা বাক্যের বদলে এখন সাজানো, লাইন-বাই-লাইন
            // (DeletePermission.kt/ChamberReopenPermission.kt-এর মতোই
            // প্রমাণিত ধরন) — Staff/Branch/Time আলাদা লাইনে, ইমোজি-সহ।
            /* 🏥 V1179 (TK-নির্দেশ) — অন্য ব্রাঞ্চে ডিউটি করলে মাস্টারের
               নোটিশেও **সেই ব্রাঞ্চই** লেখা থাকে, নইলে দুই পর্দায় দু'রকম
               হত। বাড়ি থেকে কাজ হলে সেটাও এক লাইনে বলা থাকে।
               ⛔ শিরোনাম "Staff IN TIME" অটুট (B467-এর নিয়ম)। */
            val whereLine = when {
                workedFromHome -> "🏥 Branch : $branch\n🏠 Work From Home (Master approved)"
                dutyBranch.isNotBlank() && !dutyBranch.equals(branch, true) ->
                    "🏥 Branch : $dutyBranch\n🔄 On duty from $branch"
                else -> "🏥 Branch : $branch"
            }
            val msg = "👤 Staff : ${staffCode.ifBlank { mobile }}\n" +
                whereLine + "\n" +
                "Time : " + displayTime12(ns(day, "check_in"))
            // ⛔ শিরোনাম ঠিক "Staff IN TIME"-ই রাখা হলো (ইমোজি যোগ করা
            // হয়নি) — `BriefingActivity.kt`-এর `AUTO_DELETE_ON_SEEN_TITLES`
            // এই হুবহু শব্দ মিলিয়ে "দেখা হলে নিজে থেকে মুছে যাওয়া"
            // ব্যবস্থা চালায় (B467); শিরোনাম বদলালে সেটা ভেঙে যেত।
            /* 🔴🔒 V1362 (১১.০৯.২০২৬, পুরো-প্রজেক্ট দেরি-অডিট) — এই কলটা
               `afterInTimeMarked` UI-থ্রেডেই চলে (উপরের `fv.push()`-এর মতোই),
               আর `post()` ভিতরে সরাসরি নেটওয়ার্ক-কল করে (`SupabaseClient.upsert`)
               — Android মেইন থ্রেডে নেট-কাজ **সবসময়ই** ব্যর্থ করে দেয়
               (NetworkOnMainThreadException), যা এখানেই চুপচাপ ধরা পড়ত। ফল:
               Master-এর "Staff IN TIME" খবর **কখনোই সরাসরি পৌঁছাত না** — সবসময়
               জমা হয়ে পরের সিঙ্কের অপেক্ষায় থাকত, তাই দেরি হতোই।
               ⇒ এখন উপরের `fv.push()`-এর একই প্রমাণিত ধরনে পিছনের সুতোয়। */
            /* 🔕🔒 V1436 (১৩.০৯.২০২৬ সন্ধ্যা, TK-নির্দেশ, তালিকা ৫৫৬): *"লাগবে না App-এর
               নোটিফিকেশনে, কারণ Staff-রা WhatsApp-এ আমাকে পাঠায়"* — মাস্টারের ঘন্টায়
               "Staff IN TIME" নোটিশ আর যায় না। ⛔ WhatsApp-এ পাঠানো (নিচে), হাজিরা-সেভ,
               বার্তার লেখা — সব হুবহু আগের মতোই। msg বানানোটা রাখা হলো, যাতে TK পরে
               চালু করতে বললে এক লাইনে ফেরানো যায়। */
            @Suppress("UNUSED_VARIABLE") val keepForLater = msg
        } catch (_: Throwable) { }
        waAskKind = "in"   // 🔴 V433 — ফিরে এলে একবার জিজ্ঞাসা: পাঠানো হয়েছে?
        com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, inTimeShareText()) { then() }
    }

    /** 🔴 V432 (TK-রিপোর্ট ১৮.০৮.২০২৬): *"IN TIME এ চাপ দিলে WhatsApp সাথে সাথে
     *  ওপেন হয়, কিন্তু একবার ব্যাকে আসলে তারপর আর পাঠানোর ব্যবস্থা নেই"*।
     *  বার্তাটা এখন **একটাই জায়গায়** বানানো হয় — তাই আবার পাঠালেও লেখা
     *  হুবহু একই থাকে, একটুও আলাদা হয় না। */
    private fun inTimeShareText(): String =
        "IN TIME- " + displayTime12(ns(day, "check_in")) +
            "\nStaff: " + staffCode + "\nDate: " + dotDate(todayIso())

    /** 🔴 V432 — মার্ক-করা IN TIME **আবার** WhatsApp-এ পাঠানো।
     *  ⛔ নিরাপদ: `check_in` · রিমাইন্ডার-ফ্ল্যাগ · Master-নোটিফিকেশন — কিছুই
     *     আবার বসে না, সময়ও বদলায় না। শুধু আগের সেই একই লেখা আবার খোলে,
     *     তাই দুবার পাঠালেও হিসাবে কোনো প্রভাব পড়ে না।
     *  ⛔ IN TIME বসানোই না থাকলে কিছুই হয় না (ভুল করে খালি বার্তা যাবে না)। */
    private fun resendInTimeWhatsApp() {
        if (ns(day, "check_in").isBlank()) return
        waAskKind = "in"   // 🔴 V433 — ফিরে এলে একবার জিজ্ঞাসা: পাঠানো হয়েছে?
        com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, inTimeShareText())
    }

    /** 🔴 V432 — দিনের পুরো রিপোর্ট **আবার** WhatsApp-এ পাঠানো (OUT TIME-এর পরে)।
     *  ⛔ `check_out` বা জমা-দেওয়া রিপোর্ট আবার লেখা হয় না — শুধু পাঠানো। */
    private fun resendDailyReportWhatsApp() {
        if (ns(day, "check_out").isBlank()) return
        fetchStats("day", todayIso()) { s ->
            runOnUiThread {
                val text = StringBuilder()
                text.append("Daily Report ").append(dotDate(todayIso())).append(" Time-").append(shareTimeLabel())
                    .append("\nStaff: $staffCode\n")
                text.append("IN TIME- ").append(displayTime12(ns(day, "check_in")).ifBlank { "-" }).append("\n")
                text.append("OUT TIME ").append(displayTime12(ns(day, "check_out")).ifBlank { "-" }).append("\n")
                text.append("\nNew Enquiry: ").append(statTxt(s, "enquiries", "enqOk"))
                    .append("\nRegistration: ").append(statTxt(s, "registrations", "regOk"))
                    .append("\nToday Patient: ").append(s.optInt("patients"))
                    .append("\nApp Calls: ").append(callTxt(s, "appCalls"))
                    .append("\nOutside Calls: ").append(callTxt(s, "outsideCalls"))
                    .append("\nTotal call : ").append(callTxt(s, "totalCalls"))
                    .append(docVisitLine())
                /* 👨‍⚕️🔒 V1032 (TK-নির্দেশ: *"কতজন ডাক্তারের কাছে ভিজিট করেছে
                   তাকে ম্যানুয়ালি এন্ট্রি করতে হয়েছে"*) — এখন নিজে থেকে গোনা হয়।
                   ⛔ লাইনটা **শুধু তখনই** বসে যখন আজ অন্তত একজনের কাছে যাওয়া
                      হয়েছে; নইলে রিপোর্ট হুবহু আগের মতোই থাকে। ⛔ কোনো পুরনো
                      সংখ্যা/লাইন ছোঁয়া হয়নি; পড়া ব্যর্থ হলেও কিছু বদলায় না। */
                val notesTxt = ns(day, "day_note").trim()
                if (notesTxt.isNotBlank()) text.append("\n\nNotes: \n").append(notesTxt)
                waAskKind = "out"   // 🔴 V433 — ফিরে এলে একবার জিজ্ঞাসা: পাঠানো হয়েছে?
                com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, text.toString())
            }
        }
    }

    // 🔴🆕🔒 B465 — TK-নির্দেশ: "কিছু কাজ বাকি/জিরো থেকে গেছে সেটা ফিলাপ
    // করার পরে যেন অটোমেটিক ফোর্স করে WhatsApp আসে" — OUT TIME সেভ হওয়ার
    // পরে (ফাঁকা ঘর ভরা হোক বা না হোক, `checkEmptyFieldsThenOut` শেষ হলেই)
    // IN TIME-এর মতোই WhatsApp জোর করে খোলে। ⛔ Master-নোটিফিকেশন OUT
    // TIME-এ TK চাননি (শুধু IN TIME-এর কথা বলেছেন), তাই এখানে যোগ করা
    // হয়নি — শুধু WhatsApp।
    // 🔴🔴🔴🔒 B518 (06.08.2026, TK-এর WhatsApp স্ক্রিনশট প্রুফে ধরা পড়েছে
    // — "আগে তো ভালো আসতো") — এই ফাংশনটা আগে থেকেই (B465-এরও আগে থেকে)
    // নিজের একটা ছোট, ৩-লাইনের বার্তা বানাত (শুধু OUT TIME/Staff/Date) —
    // অথচ `finishWithReason()`-এ পুরো দিনের রিপোর্ট (New Enquiry/
    // Registration/App Calls ইত্যাদি-সহ) আলাদাভাবে বানানো হতো শুধু
    // ডেটাবেসে জমা (`submit()`) দেওয়ার জন্য — WhatsApp-এ সেই পূর্ণাঙ্গ
    // লেখাটাই কখনো পাঠানো হতো না। এখন এই ফাংশন সেই **একই পূর্ণাঙ্গ
    // রিপোর্ট-লেখা** প্যারামিটার হিসেবে নেয় ও সেটাই WhatsApp-এ পাঠায়।
    private fun afterOutTimeMarked(reportText: String, then: () -> Unit) {
        com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, reportText) { then() }
    }

    // 🔴🆕🔒 B465 (05.08.2026, TK-নির্দেশ — "Out Time এ চাপ দিলে Are you
    // sure এই অপশন থাকবে না") — এই ধাপটা আর ডাকা হয় না (OUT TIME বোতাম
    // এখন সরাসরি `askCheckOutReason()` খোলে)। ফাংশনটা মোছা হয়নি, শুধু
    // ব্যবহার বন্ধ — TK-এর নিয়ম "কিছু মোছা যাবে না, শুধু লুকানো/বন্ধ করা
    // যাবে"।
    // 🔴🆕🔒 B466 (05.08.2026) — `askCheckOutReason()`-এর নতুন সিগনেচারে
    // (patientsField/ocCount/notesField লাগে) এই আগে-থেকে-অব্যবহৃত ফাংশনের
    // ভিতরের কলটা আর মেলে না — যেহেতু এটা কোথাও ডাকাই হয় না (উপরের নোট),
    // শুধু ভিতরের কলটা সরানো হলো যাতে বিল্ড না ভাঙে (ফাংশনের খোলস অক্ষত)।
    private fun confirmCheckOut() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            // 🎨 TK-APPROVED (2026-08-06): রঙিন হেডার + paint — যাতে কোথাও কোনো
            // প্লেইন পপ-আপ না থাকে (এই ফাংশনটা অব্যবহৃত/dead code, তাই ঝুঁকি শূন্য)।
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Confirm"))
            .setMessage("Are you sure you want to leave?")
            .setPositiveButton("Yes", null)
            .setNegativeButton("No", null)
            .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { } }
    }

    // 🔴🆕🔒 B465 (05.08.2026, TK-নির্দেশ, ছবিসহ) —
    // (১) "Why are you leaving?"-এর পাশে বাংলা ("আমি ক্লিনিক থেকে বেরোলাম")।
    // (২) "Office time over"-এর পাশে বাংলা ("অফিস টাইম শেষ")।
    // (৩) নতুন, আলাদা টাইপ-করার বক্স ("কেন যাচ্ছেন এই সম্পর্কে কিছু লিখুন")
    //     — সম্পূর্ণ ঐচ্ছিক। TK স্পষ্ট করেছেন: "যদি অফিস টাইমে চুস করে
    //     তাহলে যেন অটোমেটিক কাজ করে" — মানে প্রিসেট অপশনে চাপলে সঙ্গে সঙ্গেই
    //     কাজ হয়ে যায়, বক্সে কিছু লিখতে **বাধ্য** করা হয় না। বক্সটা শুধু
    //     তখনই লাগে যখন স্টাফ নিজের কথায় ভিন্ন কারণ লিখতে চান (তখন "Save"
    //     চাপবেন)। হিন্ট-লেখা টাইপ করা শুরু করলেই নিজে থেকে সরে যায় —
    //     Android-এর EditText-এর নিজস্ব, স্বাভাবিক আচরণ, আলাদা কিছু বানাতে
    //     হয়নি।
    /**
     * 🔴🔴 V509 (২১.০৮.২০২৬, TK-এর সিদ্ধান্ত — বিকল্প ৩) — **OUT TIME কোথা থেকে
     * দেওয়া হলো, শুধু সেটুকু লিখে রাখা।**
     *
     * TK-এর প্রশ্ন ছিল: *"Out Time দিতে যদি Staff ভুলে যায়, তাহলে বাড়িতে গিয়ে কি
     * সে Out time বসাতে পারবে?"* — যাচাই করে দেখা গেছে **পারবে** (OUT TIME-এ
     * কোনো GPS বা আঙুলের যাচাই কোনোদিনই ছিল না; ওটা শুধু IN TIME-এ)। TK
     * সিদ্ধান্ত নিয়েছেন — **আটকানো হবে না, কিন্তু লেখা থাকবে**।
     *
     * ─── এই ফাংশন কী করে ──────────────────────────────────────────────────
     * একবার অবস্থান দেখে, তারপর একটা ছোট লেখা ফেরত দেয় —
     *  • ক্লিনিকের **ভিতরে** → ফাঁকা লেখা (রিপোর্টে বাড়তি কিছু ওঠে না)।
     *  • ক্লিনিকের **বাইরে**  → "Marked from outside the clinic · about 4 km away"।
     *  • অবস্থান পাওয়াই গেল না (অনুমতি নেই · Location বন্ধ · সময় শেষ · নকল ·
     *    ব্রাঞ্চের অবস্থান বসানো নেই) → "Location not verified"।
     *
     * ─── ⚠️ প্রথম চেষ্টায় যে তিনটে ভুল করেছিলাম (নিজের যাচাইয়ে ধরা পড়েছে) ──
     * প্রথমে অবস্থান দেখা শুরু হত **সেভ চাপার পরে**। তাতে তিনটে বিপদ ছিল —
     *  ১) স্টাফ OUT TIME চাপার পরে পর্দায় **১২ সেকেন্ড কিছুই হত না**; তিনি
     *     ভাবতেন চাপ লাগেনি, আবার চাপতেন — **দুবার রিপোর্ট, দুবার WhatsApp**।
     *  ২) ঐ ১২ সেকেন্ডের মধ্যে ফোন ঘোরালে পর্দা নতুন করে তৈরি হয়ে পুরোনো
     *     (check_out ফাঁকা) তথ্য তুলে আনত — **বসানো OUT TIME মুছে যেতে পারত**।
     *  ৩) খাতায় বসত **চাপার সময় নয়, ১২ সেকেন্ড পরের সময়**।
     *
     * ⇒ **এখন উল্টো করা হয়েছে:** OUT TIME-এর পর্দা/পপ-আপ **খোলার সঙ্গে সঙ্গেই**
     *   অবস্থান দেখা শুরু হয় ([startPlaceProbe])। স্টাফ যতক্ষণে কারণ বাছেন,
     *   ততক্ষণে উত্তর হাতে চলে আসে — তাই সেভের সময় **বাড়তি অপেক্ষা প্রায় শূন্য**।
     *   উত্তর না এলে সর্বোচ্চ **৬ সেকেন্ড** অপেক্ষা, তাও একটা **আটকানো
     *   ("Checking location…") পপ-আপসহ** — তাই দুবার চাপা অসম্ভব।
     *   আর OUT TIME-এর **সময়টা চাপার মুহূর্তেই ধরা হয়** (নিচের `outAt`)।
     *
     * ─── ⛔ কেন এতে কারো হাজিরা হারাতে পারে না ─────────────────────────────
     *  • [withPlaceNote]-এ `then` **ঠিক একবার** ডাকা হয় (`used` পাহারা), আর
     *    সেটা try/catch-এর বাইরে — তাই ভিতরে কিছু ভাঙলেও দ্বিতীয়বার চলে না।
     *  • `ClinicPresence`-এর নিজের ১২ সেকেন্ডের সীমা; তার উপরে probe-এ
     *    ১৫ সেকেন্ড, আর সেভে ৬ সেকেন্ড — তিন স্তরের পাহারা। উত্তর না এলেও
     *    OUT TIME ঠিকই বসে যায় ("Location not verified" লেখা থাকে)।
     *  • GPS শুধু এই মুহূর্তটুকুর জন্য চালু, ফল পেলেই বন্ধ — ব্যাটারি খরচ নেই।
     *  • Late-এর নিয়ম · রিপোর্টের ছক · WhatsApp — একটাও ছোঁয়া হয়নি।
     */
    /** অবস্থান-যাচাইয়ের উত্তর (এসে গেলে); `null` = এখনো আসেনি। */
    @Volatile private var placeProbeResult: String? = null
    /** উত্তর এলে কাকে জানাতে হবে (সেভ অপেক্ষা করলে)। */
    private var placeProbeWaiter: ((String) -> Unit)? = null
    /** "Checking location…" আটকানো পপ-আপ (শুধু অপেক্ষা করতে হলে)। */
    private var placeProbeDialog: androidx.appcompat.app.AlertDialog? = null

    /** OUT TIME-এর পর্দা খোলার সঙ্গে সঙ্গে ডাকা হয় — উত্তরটা আগেভাগে এনে রাখে। */
    private fun startPlaceProbe() {
        placeProbeResult = null
        placeProbeWaiter = null
        var settled = false
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        fun deliver(note: String) {
            if (settled) return
            settled = true
            h.removeCallbacksAndMessages(null)
            placeProbeResult = note
            val w = placeProbeWaiter
            placeProbeWaiter = null
            if (w != null) h.post { w(note) }
        }
        h.postDelayed({ deliver("Location not verified") }, 15_000L)
        try {
            val user = com.tkbiswas.pilesclinic.native.NativeSession.current(this)
            /* 🏥 V1179 — অন্য ব্রাঞ্চে ডিউটি করলেও "বাইরে থেকে" লেখা উঠবে না
               (৫টা ক্লিনিকের যেকোনোটার কাছে থাকলেই ভিতরে ধরা হয়)। */
            com.tkbiswas.pilesclinic.native.ClinicPresence.check(this, user?.branch, anyBranch = true) { p ->
                deliver(
                    when {
                        p.ok -> ""     // ভিতরেই আছেন — রিপোর্টে বাড়তি কিছু লেখার দরকার নেই
                        p.reason == com.tkbiswas.pilesclinic.native.ClinicPresence.Reason.OUTSIDE ->
                            "Marked from outside the clinic" + awayText(p.distanceMeters)
                        else -> "Location not verified"
                    }
                )
            }
        } catch (_: Throwable) {
            deliver("Location not verified")
        }
    }

    /** সেভের মুহূর্তে — উত্তর এসে গেলে সঙ্গে সঙ্গে, নইলে সর্বোচ্চ ৬ সেকেন্ড। */
    private fun withPlaceNote(then: (String) -> Unit) {
        val ready = placeProbeResult
        if (ready != null) { then(ready); return }          // সাধারণত এটাই ঘটে — শূন্য অপেক্ষা
        var used = false
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        fun go(note: String) {
            if (used) return
            used = true
            h.removeCallbacksAndMessages(null)
            placeProbeWaiter = null
            try { placeProbeDialog?.dismiss() } catch (_: Throwable) { }
            placeProbeDialog = null
            then(note)                                       // ⛔ try/catch-এর বাইরে — দুবার নয়
        }
        placeProbeWaiter = { n -> go(n) }
        h.postDelayed({ go("Location not verified") }, 6_000L)
        // আটকানো পপ-আপ — স্টাফ দ্বিতীয়বার OUT TIME চাপতে পারবেন না।
        try {
            placeProbeDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "OUT TIME"))
                .setMessage("Checking location…")
                .setCancelable(false)
                .create().also {
                    it.show()
                    try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) { }
                }
        } catch (_: Throwable) { }
    }

    /** কত দূরে — ১ কিমি-র বেশি হলে কিমি-তে, নইলে মিটারে। জানা না গেলে ফাঁকা।
     *  🔒 `Locale.US` বাধ্যতামূলক — নইলে বাংলা-ভাষার ফোনে "৪.২" (বাংলা অঙ্ক)
     *     লেখা হয়ে যেত, আর সেটা মাস্টারের রিপোর্টে চলে যেত (TK-এর নিয়ম:
     *     "এই ধরনের বাংলা থাকবে না")। নিজের যাচাইয়ে ধরা পড়েছে। */
    private fun awayText(meters: Int?): String {
        if (meters == null || meters <= 0) return ""
        return if (meters >= 1000)
            " · about " + String.format(java.util.Locale.US, "%.1f", meters / 1000.0) + " km away"
        else " · about $meters m away"
    }

    private fun askCheckOutReason(patientsField: EditText, ocCount: EditText, notesField: EditText) {
        // 🔴 V509: এই পপ-আপ **খোলার সঙ্গে সঙ্গেই** অবস্থান দেখা শুরু। স্টাফ
        // যতক্ষণে কারণ বেছে নেন, ততক্ষণে উত্তর হাতে চলে আসে — তাই সেভ চাপার
        // পরে বসে থাকতে হয় না (প্রথম চেষ্টার সবচেয়ে বড় ভুল ছিল ঠিক এটাই)।
        startPlaceProbe()
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        var dlg: androidx.appcompat.app.AlertDialog? = null

        // 🔴🆕🔒 B466 (05.08.2026, TK-নির্দেশ, স্ক্রিনশট-আলোচনা) — এখন
        // OUT TIME-এর সাথেই পুরো দিনের রিপোর্ট (Today Patient/Outside Calls/
        // Notes) সেভ ও মাস্টারের কাছে চুপচাপ সাবমিট হয়ে যায় (আগে আলাদা
        // "Submit Report to Master" বোতাম লাগত, TK: "আলাদা করে কোথাও রাখতে
        // হবে না")। ⛔ পুরনো `submit()`/`fetchStats()`/রিপোর্ট-টেক্সট ফরম্যাট
        // এক অক্ষরও বদলায়নি — শুধু কোথা থেকে ডাকা হচ্ছে সেটা বদলেছে
        // (আগে submitDailyReport() বোতাম থেকে, এখন এখান থেকে সরাসরি)।
        fun finishWithReason(reason: String) {
            dlg?.dismiss()
            day.put("outside_calls_manual", ocCount.text.toString().toIntOrNull() ?: 0)
            day.put("day_note", notesField.text.toString())
            checkEmptyFieldsThenOut {
              // 🔴🔴 V509 (২১.০৮.২০২৬, TK-এর সিদ্ধান্ত — বিকল্প ৩, হুবহু:
              // *"বাড়ি থেকেও দিতে পারবে, কিন্তু আপনার রিপোর্টে লেখা থাকবে —
              //  ক্লিনিকের বাইরে থেকে দিয়েছে, ৪ কিমি দূরে"*)।
              //
              // ⛔ **কাউকে আটকানো হয় না।** যে সত্যিই ভুলে গেছে, সে আগের মতোই
              //    বাড়ি থেকে OUT TIME বসাতে পারবে — ঘর ফাঁকা থাকবে না।
              // ✅ শুধু কোথা থেকে দেওয়া হলো সেটা লিখে রাখা হয়, আর মাস্টারের
              //    রিপোর্টে দেখানো হয়। ফাঁকি দেওয়া কঠিন হলো, কারো কাজ আটকাল না।
              // ⛔ GPS না পাওয়া / অনুমতি নেই / সময় শেষ — কোনো অবস্থাতেই OUT TIME
              //    আটকায় না; তখন শুধু "Location not verified" লেখা থাকে।
              // 🔒 সময়টা **এখনই** ধরা — অপেক্ষা করতে হলেও খাতায় চাপার সময়টাই বসবে।
              if (!outTimeAllowedNow()) return@checkEmptyFieldsThenOut   // 🕒 V1244
              val outAt = nowTime()
              withPlaceNote { placeNote ->
                day.put("check_out", outAt)
                stopFieldVisitIfRunning()   // 🏍️ V968
                // 🔴 V509: জায়গার কথাটা আলাদা কোনো নতুন কলামে নয় — **আগে থেকেই
                // থাকা** `check_out_reason` ঘরেই জুড়ে দেওয়া হয়। তাই নতুন কোনো
                // SQL/ডেটাবেস পরিবর্তন লাগে না (পুরনো ফোনেও ভাঙবে না)।
                day.put("check_out_reason", if (placeNote.isBlank()) reason else "$reason · $placeNote")
                markReminderFlag("out", true)
                saveDay {
                    fetchStats("day", todayIso()) { s ->
                        runOnUiThread {
                            val text = StringBuilder()
                            text.append("Daily Report ").append(dotDate(todayIso())).append(" Time-").append(shareTimeLabel())
                                .append("\nStaff: $staffCode\n")
                            text.append("IN TIME- ").append(displayTime12(ns(day, "check_in")).ifBlank { "-" }).append("\n")
                            text.append("OUT TIME ").append(displayTime12(ns(day, "check_out")).ifBlank { "-" }).append("\n")
                            // 🔴 V509: ক্লিনিকের ভিতর থেকে দিলে এই লাইনটা আসেই না —
                            // শুধু বাইরে থেকে বা যাচাই করা না গেলে দেখা যায়।
                            if (placeNote.isNotBlank()) text.append("⚠️ ").append(placeNote).append("\n")
                            text.append("\nNew Enquiry: ").append(statTxt(s, "enquiries", "enqOk"))
                                .append("\nRegistration: ").append(statTxt(s, "registrations", "regOk"))
                                .append("\nToday Patient: ").append(patientsField.text.toString().trim().ifBlank { "0" })
                                .append("\nApp Calls: ").append(callTxt(s, "appCalls"))
                                .append("\nOutside Calls: ").append(callTxt(s, "outsideCalls"))
                                .append("\nTotal call : ").append(callTxt(s, "totalCalls"))
                                .append(docVisitLine())
                            val notesTxt = notesField.text.toString().trim()
                            if (notesTxt.isNotBlank()) text.append("\n\nNotes: \n").append(notesTxt)
                            submit("daily", todayIso(), s, text.toString())
                            afterOutTimeMarked(text.toString()) { render() }
                        }
                    }
                }
              }   // 🔴 V509 — withPlaceNote শেষ
            }
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(4), dp(20), dp(4))
        }
        box.addView(android.widget.TextView(this).apply {
            text = NoBengali.s("তুমি কেন চলে যেতে চাইছ")
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#5A6B7A"))
            setPadding(0, 0, 0, dp(10))
        })
        // 🎨🔒 B510 (06.08.2026, TK-নির্দেশে "প্রফেশনাল" মকআপ দেখিয়ে
        // অনুমোদনের পরে) — প্রতিটা অপশন এখন আইকন + দুই-লাইন লেখা
        // (ইংরেজি বোল্ড, নিচে বাংলা হালকা রঙে) + ডানে "›" তীর-চিহ্ন
        // (চাপা যায় এটা স্পষ্ট বোঝাতে) — হালকা বর্ডার-করা কার্ডে।
        // ⛔ "কেন যাচ্ছেন এই সম্পর্কে কিছু লিখুন" মুক্ত-লেখার ঘর ও আলাদা
        // "Save" বোতাম তুলে দেওয়া হলো (TK-নির্দেশ) — Personal work-এর
        // কারণ লেখার প্রশ্ন আগে থেকেই আলাদা পপ-আপে (askPersonalLeaveReason)
        // আছে, এখানে আর দরকার নেই।
        fun optionRow(icon: String, enText: String, bnText: String): LinearLayout {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
                isClickable = true; isFocusable = true
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#F7F9FB"))
                    setStroke(1, android.graphics.Color.parseColor("#E5E9ED"))
                    cornerRadius = dp(10).toFloat()
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.topMargin = dp(8)
                layoutParams = lp
            }
            val iconBox = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).also { it.marginEnd = dp(10) }
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#EAF3EF"))
                    cornerRadius = dp(8).toFloat()
                }
            }
            iconBox.addView(TextView(this).apply { text = icon; textSize = 15f; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).also { it.gravity = android.view.Gravity.CENTER } })
            row.addView(iconBox)
            val txtCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            txtCol.addView(TextView(this).apply { text = enText; textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD); setTextColor(android.graphics.Color.parseColor("#1F2937")) })
            txtCol.addView(TextView(this).apply { text = NoBengali.s(bnText); textSize = 11.5f; setTextColor(android.graphics.Color.parseColor("#6B7280")) })
            row.addView(txtCol)
            row.addView(TextView(this).apply { text = "›"; textSize = 18f; setTextColor(android.graphics.Color.parseColor("#9CA3AF")) })
            return row
        }
        val optOffice = optionRow("⏳", "Office time over", "অফিস টাইম শেষ")
        optOffice.setOnClickListener { finishWithReason("Office time over") }
        box.addView(optOffice)
        // 🔴🆕🔒 B466 (TK-নির্দেশ — "Office Time সিলেক্ট করলে প্রশ্ন আসার
        // কথা না, কিন্তু Personal Use হলে কেন যাচ্ছেন তা লিখতে বলতে হবে")—
        // Personal work চাপলে সরাসরি সেভ হয় না, একটা নতুন বাধ্যতামূলক
        // কারণ-লেখার পপ-আপ খোলে (askPersonalLeaveReason)।
        val optPersonal = optionRow("🚶", "Personal work — leaving early", "ব্যক্তিগত কাজ — আগে চলে যাচ্ছেন")
        // 🔴🔒 B511 (06.08.2026, TK-নির্দেশ — "চাপ দিলে আর কিছু লেখার মতো
        // অপশন যেন না থাকে") — Personal work-এ চাপলে এখন সরাসরি সেভ হয়,
        // আগের মতো আলাদা কারণ-লেখার পপ-আপ (askPersonalLeaveReason) আর
        // আসে না। ⛔ ওই ফাংশনটা মোছা হয়নি (dead code, পরে দরকার হলে
        // ফেরানো যাবে), শুধু এখানে ডাকা বন্ধ করা হলো।
        optPersonal.setOnClickListener { finishWithReason("Personal work — leaving early") }
        box.addView(optPersonal)

        dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("Why are you leaving?")))
            .setView(box)
            .setNegativeButton("Cancel", null)
            .show()
        try { NoBengali.installDialog(dlg); dlg?.let { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } } catch (_: Throwable) { }
    }

    // 🔴🆕🔒 B466 (05.08.2026, TK-নির্দেশ) — "Personal work" বেছে নিলে
    // এই ছোট পপ-আপ খোলে, কারণ না লিখলে এগোনো যায় না (Cancel চাপলে
    // পুরনো "Why are you leaving?" পপ-আপে ফিরিয়ে নেওয়া হয় না, স্টাফ চাইলে
    // OUT TIME বোতাম আবার চেপে শুরু করবেন — প্রজেক্টের অন্য জায়গার
    // Cancel-এর মতোই সাধারণ আচরণ)।
    private fun askPersonalLeaveReason(onDone: (String) -> Unit) {
        val input = ModuleUi.input(this, NoBengali.s("কেন বাড়ি যাচ্ছেন লিখুন"))
        lateinit var prDlg: androidx.appcompat.app.AlertDialog
        prDlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("Why are you leaving for personal work?")))
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val r = input.text.toString().trim()
                if (r.isBlank()) { ModuleUi.toast(this, "Please write a reason") }
                else onDone(r)
            }
            .setNegativeButton("Cancel", null)
            .show()
        try { NoBengali.installDialog(prDlg); com.tkbiswas.pilesclinic.native.PremiumAlert.paint(prDlg) } catch (_: Throwable) { }
    }

    // 🔴🆕🔒 B438 — TK: "Outside Calls ও Notes ঘর ফাঁকা থাকলে OUT TIME-এর
    // আগে সতর্কতা দেখাতে হবে।" ফাঁকা না থাকলে সঙ্গে সঙ্গে [then] চলে;
    // ফাঁকা থাকলে একটা ছোট পপ-আপে সেই ঘরগুলো ভরার সুযোগ (অথবা "Skip and
    // continue" — TK জোরপূর্বক আটকাতে বলেননি, শুধু মনে করিয়ে দিতে বলেছেন,
    // তাই প্রজেক্টের অন্য সব সতর্কতার মতোই — মনে করায়, আটকায় না)।
    private fun checkEmptyFieldsThenOut(then: () -> Unit) {
        val missingOc = day.optInt("outside_calls_manual", 0) <= 0
        val missingNote = ns(day, "day_note").isBlank()
        if (!missingOc && !missingNote) { then(); return }
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
        }
        // 🎨🔒 B512 (06.08.2026, TK-নির্দেশে মকআপ "A" দেখিয়ে অনুমোদনের
        // পরে) — প্রতিটা ফাঁকা ঘর এখন আইকন-সহ হালকা কমলা কার্ডে (আগে
        // প্লেইন বুলেট-টেক্সট ছিল)। রঙিন হেডার — প্রজেক্টের আগে থেকে
        // প্রমাণিত `PremiumAlert.header()/paint()` পুনর্ব্যবহার
        // (AppointmentActivity/BriefingActivity-তে একই প্যাটার্ন)।
        fun warnCard(icon: String, text: String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(9), dp(10), dp(9))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#FFF7ED"))
                setStroke(1, android.graphics.Color.parseColor("#FED7AA"))
                cornerRadius = dp(10).toFloat()
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(8)
            layoutParams = lp
            addView(TextView(this@WorkNotebookActivity).apply { this.text = icon; textSize = 14f; setPadding(0, 0, dp(8), 0) })
            addView(TextView(this@WorkNotebookActivity).apply {
                this.text = NoBengali.s(text); textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#7C2D12"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        if (missingOc) box.addView(warnCard("📞", "আজকে কতগুলি ফোন রিসিভ করেছেন — লেখা হয়নি"))
        if (missingNote) box.addView(warnCard("📝", "Notes — কিছু লেখা হয়নি"))
        val ocInput = if (missingOc) numericField("0").also { box.addView(spacedField(it)) } else null
        val noteInput = if (missingNote)
            ModuleUi.input(this, "What did you do today?", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                .apply { minLines = 2 }.also { box.addView(spacedField(it)) }
        else null
        lateinit var missDlg: androidx.appcompat.app.AlertDialog
        missDlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, NoBengali.s("⚠️ Some fields are empty")))
            .setView(box)
            .setPositiveButton(NoBengali.s("Fill & mark OUT TIME")) { _, _ ->
                ocInput?.text?.toString()?.toIntOrNull()?.let { day.put("outside_calls_manual", it) }
                noteInput?.text?.toString()?.let { day.put("day_note", it) }
                then()
            }
            .setNegativeButton(NoBengali.s("Skip")) { _, _ -> then() }
            .setOnCancelListener { then() }
            .show()
        try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(missDlg) } catch (_: Throwable) { }
        try { NoBengali.installDialog(missDlg) } catch (_: Throwable) { }
    }

    private fun saveDay(then: (() -> Unit)? = null) {
        day.put("updated_at", nowIso())
        // 🔵 TK-ORDER (07.08.2026): ঠিক এই মুহূর্তের সময় নিয়ে একটা **স্থায়ী স্ন্যাপশট**।
        // পরে জমা-খাতা থেকে বসলেও এই একই সময়ই বসে — তাই WhatsApp-এ যাওয়া IN/OUT
        // TIME আর অ্যাপে সেভ হওয়া সময় সবসময় এক থাকে (কখনো আলাদা হবে না)।
        val snapshot = try { JSONObject(day.toString()) } catch (_: Throwable) { day }
        Thread {
            // 🔴🔴🔒🔴 V478 (20.08.2026, TK-নির্দেশ — "অল্টারনেট রাস্তা না, মূল
            // সমস্যা ঠিক করুন") — **আসল, মূল কারণ (এতদিন ধরা পড়েনি):**
            // `robustSaveNotebookDay()`-এর true/false ফলাফল এতদিন **সম্পূর্ণ
            // উপেক্ষা করা হতো** — নিচের `then?.invoke()` **সবসময়ই** চলত,
            // ক্লাউডে সেভ সত্যিই সফল হোক বা না হোক। তাই "IN TIME marked ✅"
            // দেখিয়ে দিত, যদিও ভেতরে ভেতরে সেভ শুধু ফোনের জমা-ঘরে আটকে ছিল
            // (JWT-বাগ বা নেট-সমস্যা যেকোনো কারণে) — স্টাফ/Master কেউই
            // জানতেই পারতেন না, ঘণ্টাখানেক পরে (দুপুর ১২টা পার হয়ে) সেটা
            // ধরা পড়ত, ততক্ষণে আর ঠিক করার উপায় থাকত না।
            // **সমাধান:** সত্যিই ক্লাউডে না পৌঁছালে এখন স্পষ্ট সতর্কবার্তা
            // দেখাবে, আর `then()` (যেটাতে "marked ✅" Toast/finish()/WhatsApp-
            // পাঠানো থাকে) **তখন চলবে না** — স্টাফ তখনই বুঝবেন, তখনই আবার
            // চেষ্টা করতে পারবেন (দুপুর ১২টা পার হওয়ার আগে)। ⛔ সফল হলে
            // সবকিছু আগের মতোই, একটুও বদলায়নি।
            val ok = robustSaveNotebookDay(snapshot)
            runOnUiThread {
                if (ok) {
                    // 🔴 V511 — ক্লাউডে সত্যিই বসেছে; এই ফোনেও জমা রাখি, যাতে
                    //   পরে নেট না থাকলেও OUT TIME বোতাম হারিয়ে না যায়।
                    dayFromCache = false
                    dayLoadFailed = false
                    saveDayCache()
                    then?.invoke()
                } else {
                    android.widget.Toast.makeText(
                        this,
                        NoBengali.s("⚠️ এখনই ক্লাউডে সেভ হয়নি (ফোনে জমা আছে, নেট এলে নিজে বসে যাবে)। এখনই ইন্টারনেট/ওয়াইফাই চেক করে আবার বোতাম চাপুন।"),
                        android.widget.Toast.LENGTH_LONG
                    ).show().also { try { com.tkbiswas.pilesclinic.native.NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { } }   // 🤫 V774
                }
            }
        }.start()
    }

    // 🔵 TK-ORDER (07.08.2026): notebook_days **নিশ্চিতভাবে** সেভ। (১) দরকারে গোপনে
    // আবার লগইন (কোনো পপ-আপ নেই, স্টাফকে কিছু চাপতে হয় না)। (২) সঠিক দিন-চিহ্ন
    // (staff_code, work_date) ধরে বসানো — তাই ব্যর্থ হয় না, ডুপ্লিকেটও হয় না।
    // (৩) তাও না বসলে ফোনে **জমা** থাকে ও পরে নিজে বসে — IN/OUT কখনো হারায় না।
    // একটা দিন-সারি ক্লাউডে বসানোর একমাত্র নিরাপদ পথ:
    //  · প্রথমে সঠিক দিন-চিহ্ন (staff_code, work_date) ধরে (on_conflict) — সারি
    //    থাকলে আপডেট, না থাকলে নতুন; কখনো ব্যর্থ/ডুপ্লিকেট নয়।
    //  · 🔵 নিরাপত্তা-fallback: যদি DB-তে ওই unique-constraint না-থাকে (পুরনো
    //    টেবিল), on_conflict কাজ না-ও করতে পারে — তখন সাধারণ upsert দিয়ে বসানো
    //    হয়, যাতে কখনো আটকে না যায়। (constraint থাকলে এই fallback লাগেই না।)
    // 🔴🔴🔴 (07.08.2026, TK-রিপোর্ট — "একবার OUT TIME চাপার পরেও বারবার OUT
    // TIME দেখাচ্ছে", কয়েক সেশন ধরে চলছিল)। **আসল কারণ (কোড+SQL মিলিয়ে
    // যাচাই করা, অনুমান নয়):** B320 (03.08.2026) থেকে OUT TIME-এর সাথে
    // `check_out_reason` ঘরটাও পাঠানো হয় — কিন্তু `wn.notebook_days`
    // টেবিলে ওই কলামটা **কখনো বানানোই হয়নি** (V246-এ নেই, V256-এ শুধু
    // is_leave/leave_reason/outside_calls_manual/day_note যোগ হয়েছিল,
    // এটা বাদ পড়ে যায়)। PostgREST অচেনা কলাম পেলে **পুরো সারিটাই** ফিরিয়ে
    // দেয় (400) — তাই `check_out` কখনো ক্লাউডে বসত না, পরের বার খুললেই
    // আবার OUT TIME বোতাম। IN TIME-এ এই ঘরটা থাকে না বলে ওটা ঠিক কাজ করত।
    // (হুবহু একই ধরনের ভুল V256-এর নোটেও লেখা আছে — "পর্দায় দেখাচ্ছিল,
    // ক্লাউডে সেভ হচ্ছিল না"।) ⛔ নেটওয়ার্কের কোনো দোষ ছিল না।
    //
    // **সমাধান দুই স্তরে:**
    //  ১) `PATCH_2026-08-07_notebook_check_out_reason.sql` — কলামটা যোগ করে
    //     (তখন কারণও সেভ হবে)।
    //  ২) নিচের এই সুরক্ষা — SQL চালানো না থাকলেও OUT TIME যেন **হারিয়ে
    //     না যায়**: দুইবার ব্যর্থ হলে ঐচ্ছিক ঘরগুলো বাদ দিয়ে আরেকবার
    //     পাঠানো হয়। তখন `check_out` (আসল হাজিরা) ঠিকই বসে, শুধু কারণ-
    //     লেখাটা বাদ যায় — সারি হারানোর চেয়ে অনেক ভালো।
    // ⛔ সফল হলে এই বাড়তি চেষ্টা কখনো চলে না (আগের আচরণ হুবহু অপরিবর্তিত)।
    // ════════════════════════════════════════════════════════════════════════
    //  🔒 V496 (২১.০৮.২০২৬, TK-এর চূড়ান্ত নির্দেশ §৪) — **IN TIME-এর নতুন পাহারা**
    //
    //  ধাপে ধাপে, একটার পর একটা। যেকোনো ধাপ আটকালে হাজিরা বসে না, আর
    //  স্টাফ পরিষ্কার বাংলা বার্তা ও করণীয় দেখেন।
    //
    //    ১) তিনি Staff কিনা            → RoleRules (displayRole, প্রমাণিত পথ)
    //    ২) আজ অনুমোদিত ছুটি কিনা      → সার্ভার নিজেই দেখে (ধাপ ৫-এ)
    //    ৩) ক্লিনিকে আছেন কিনা         → ClinicPresence (GPS)
    //    ৪) আঙুলের ছাপ                 → BiometricGate (শুধু BIOMETRIC_STRONG)
    //    ৫) সার্ভারে atomic সেভ        → wn.mark_check_in()
    //
    //  ⛔ ফোনের ঘড়ির সময় আর ব্যবহারই হয় না — সার্ভার নিজে সময় বসায়।
    //  ⛔ `check_in` ঘর, `HH:mm` রূপ ও বেতনের হিসাব — কিছুই বদলায়নি।
    //  ⛔ OUT TIME · ছুটি · রিপোর্ট · WhatsApp — একটাও ছোঁয়া হয়নি।
    // ════════════════════════════════════════════════════════════════════════

    /** এক জায়গায় বার্তা দেখানো — সব ধাপ একই চেহারার পপ-আপ পায়। */
    private fun inTimeMessage(title: String, message: String, headerColor: String,
                              retry: (() -> Unit)? = null, extraLabel: String? = null,
                              extra: (() -> Unit)? = null) {
        try {
            val b = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, title))
                .setMessage(NoBengali.s(message))
            // 🔤 V519 (TK-নির্দেশ): হাজিরার এই বাক্সের বোতাম সব ব্রাঞ্চেই ইংরেজি।
            if (retry != null) b.setPositiveButton("Try again") { _, _ -> retry() }
            if (extra != null && extraLabel != null) b.setNeutralButton(extraLabel) { _, _ -> extra() }
            b.setNegativeButton("Close", null)
            com.tkbiswas.pilesclinic.native.PremiumAlert.paint(b.show())
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, NoBengali.s(message), android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /** ধাপ ১ থেকে শুরু। সফল হলে [onSaved] ডাকা হয় (UI থ্রেডে)। */
    private fun startInTimeFlow(onSaved: () -> Unit) {
        val user = com.tkbiswas.pilesclinic.native.NativeSession.current(this)

        // ধাপ ১ — ডাক্তার/মাস্টারের হাজিরা নেই
        if (!com.tkbiswas.pilesclinic.native.RoleRules.usesAttendance(user)) {
            inTimeMessage("IN TIME",
                com.tkbiswas.pilesclinic.native.RoleRules.DOCTOR_NO_ATTENDANCE_MSG, "#0B2B59")
            return
        }

        // 🟢🔒🔒 V650 (২৫.০৮.২০২৬, TK-নির্দেশ, দুই দফা প্রশ্ন করে নিশ্চিত হওয়া
        // — "জলপাইগুড়ির স্টাফ, RMP-দের কাছে ফিল্ড-ভিজিটে যান, চেম্বারে সপ্তাহে
        // ২ দিন — ফিল্ডে থাকলে IN TIME নিতে পারেন না") — **আসল কারণ:** IN
        // TIME-এ GPS-পাহারা আছে (ক্লিনিকের কাছাকাছি না থাকলে সম্পূর্ণ আটকে
        // যায়) — এটাই তাঁকে আটকাচ্ছিল। TK-এর স্পষ্ট, নির্দিষ্ট নির্দেশ:
        // "GPS সম্পূর্ণভাবে বন্ধ, শুধু আঙুলের ছাপ/পাসওয়ার্ড", আর "শুধু এই
        // নির্দিষ্ট স্টাফের নম্বর ধরে" — কোনো সাধারণ টগল না, শুধু এই একজনের
        // জন্যই। তাই এখানে একটা ছোট, নাম-করা allowlist — একটা মাত্র জায়গায়,
        // স্পষ্ট মন্তব্যসহ; TK নিজে না বললে কখনো বাড়ানো যাবে না।
        // (V869 — কল-ব্যানারের পুরোনো লগইন-তালিকাটা TK-এর নির্দেশে মুছে
        //  ফেলা হয়েছে, তাই সেই তুলনাটা এখান থেকে বাদ। এই GPS-ছাড়টা
        //  সম্পূর্ণ আলাদা জিনিস — TK-এর নিজের নির্দেশে, অক্ষত।)
        // ⛔ বাকি সব স্টাফের জন্য GPS-পাহারা আগের মতোই অক্ষত (নিচের `else`
        //    শাখা, ধাপ ৩)। ⛔ আঙুলের ছাপ/পাসওয়ার্ড এই স্টাফের জন্যও অক্ষত —
        //    শুধু GPS-ধাপটাই বাদ, নিরাপত্তার বাকি সবকটা স্তর একই থাকে।
        // 🟢🔒 V1377 (১২.০৯.২০২৬, TK-নির্দেশ "RUPAM যা ছিল তাই থাক") — কোচবিহারের
        // ARMAN HOQUE-ও যোগ হলো, RUPAM-এর হুবহু একই ভূমিকায়।
        val gpsExemptMobiles = setOf("8167096595", "9883884394")   // জলপাইগুড়ি RUPAM ও কোচবিহার ARMAN, RMP field-visit — TK-নির্দেশ ২৫.০৮.২০২৬ ও ১২.০৯.২০২৬
        val myDigits = (user?.mobile ?: "").filter { it.isDigit() }.takeLast(10)
        if (gpsExemptMobiles.contains(myDigits)) {
            startBiometricThenSaveInTime(onSaved)
            return
        }

        /* 🏠🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ) — **Work From Home।**
           TK: *"সে যদি Work from Home করতে চায় তার ব্যাবস্থা যেন থাকে এবং
           মাস্টারের অনুমতি নেওয়া জরুরী"*। ⇒ মাস্টার **আজকের দিনের জন্য**
           অনুমতি দিয়ে থাকলে GPS-ধাপটা বাদ যায়; বাকি সব ধাপ (আঙুলের ছাপ ·
           সার্ভারে সেভ) হুবহু আগের মতোই থাকে।
           ⛔ অনুমতি না থাকলে এক চুলও ছাড় নেই — নিচের GPS-পাহারাই চলে।
           ⛔ মেঘ থেকে একবারই পড়া হয় (এই পর্দা যতক্ষণ খোলা), তাই বাড়তি খরচ নেই। */
        val cachedWfh = wfhApprovedToday
        if (cachedWfh == true) { dutyBranch = ""; workedFromHome = true; startBiometricThenSaveInTime(onSaved); return }
        if (cachedWfh == null) {
            android.widget.Toast.makeText(this, "Checking...", android.widget.Toast.LENGTH_SHORT).show()
            Thread {
                val ok = try {
                    com.tkbiswas.pilesclinic.native.WfhRequests.approvedToday(user?.mobile ?: "")
                } catch (_: Throwable) { false }
                runOnUiThread { wfhApprovedToday = ok; startInTimeFlow(onSaved) }
            }.start()
            return
        }

        // ধাপ ৩ — ক্লিনিকে আছেন কিনা (GPS)
        // 🔤 V519 (TK-নির্দেশ): এই পর্দার লেখা সব ব্রাঞ্চেই ইংরেজি।
        android.widget.Toast.makeText(this, "Checking whether you are at the clinic...", android.widget.Toast.LENGTH_SHORT).show()
        com.tkbiswas.pilesclinic.native.ClinicPresence.check(this, user?.branch, anyBranch = true) { presence ->
            if (!presence.ok) {
                /* 🔴🔴🔒 V519 (TK-রিপোর্ট): অনুমতি না থাকলে আগে শুধু বার্তা দেখাত,
                   আর "আবার চেষ্টা" চাপলেও হুবহু একই বার্তা — কারণ অ্যাপ অনুমতি
                   **চাইতই না**। এখন ফোনের নিজের অনুমতি-বাক্স খোলে, আর "Allow"
                   চাপলে হাজিরার কাজটা **নিজে থেকেই আবার চলে**।
                   ⛔ GPS-এর যাচাই এক চুলও বদলায়নি। */
                if (presence.reason == com.tkbiswas.pilesclinic.native.ClinicPresence.Reason.NO_PERMISSION) {
                    afterLocationPermission = { startInTimeFlow(onSaved) }
                    try {
                        requestLocationPermission.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } catch (_: Throwable) {
                        afterLocationPermission = null
                        inTimeMessage("Location permission", presence.message, "#A8281C",
                            retry = { startInTimeFlow(onSaved) },
                            extraLabel = "Open Settings", extra = { openAppSettings() })
                    }
                    return@check
                }
                val canRetry = presence.reason != com.tkbiswas.pilesclinic.native.ClinicPresence.Reason.NOT_CONFIGURED &&
                    presence.reason != com.tkbiswas.pilesclinic.native.ClinicPresence.Reason.UNKNOWN_BRANCH
                /* ফোনের Location বন্ধ থাকলেও স্টাফ আটকে যেতেন — এখন সরাসরি
                   Location-এর পাতায় যাওয়ার বোতাম আছে। */
                val offSwitch = presence.reason == com.tkbiswas.pilesclinic.native.ClinicPresence.Reason.LOCATION_OFF
                /* 🏠🔒 V1179 (TK-নির্দেশ) — ক্লিনিকের বাইরে থাকলে এখান থেকেই
                   **Work From Home** চাওয়া যায়। ⛔ চাওয়া মানে পাওয়া নয় —
                   মাস্টার Approve না করা পর্যন্ত IN TIME হবে না।
                   ⛔ Location বন্ধ থাকলে ওই বোতামটা আগের মতোই "Open Settings"
                      থাকে (নইলে স্টাফ Location চালু করার পথটাই হারাতেন)। */
                val canAskWfh = !offSwitch &&
                    (presence.reason == com.tkbiswas.pilesclinic.native.ClinicPresence.Reason.OUTSIDE ||
                        presence.reason == com.tkbiswas.pilesclinic.native.ClinicPresence.Reason.TIMEOUT)
                inTimeMessage("At the clinic?", presence.message, "#A8281C",
                    retry = if (canRetry) ({ startInTimeFlow(onSaved) }) else null,
                    extraLabel = if (offSwitch) "Open Settings" else if (canAskWfh) "🏠 Work From Home" else null,
                    extra = if (offSwitch) ({ openLocationSettings() })
                            else if (canAskWfh) ({ askWorkFromHome(onSaved) }) else null)
                return@check
            }
            /* 🏥 V1179 — কোন ক্লিনিকে দাঁড়িয়ে আছেন সেটা মনে রাখা হয়;
               সেভের পরে খাতার সারিতে **ওই ব্রাঞ্চই** বসবে (নিজের ব্রাঞ্চ নয়)। */
            dutyBranch = presence.atBranch
            workedFromHome = false
            /* 🟢🔒 V529 (২২.০৮.২০২৬, TK-নির্দেশ: **"STAFF এর intime দেওয়ার
               সময় থাকুক"**) — V528-এ এই ধাপটা তুলে দেওয়া হয়েছিল; TK বলার পর
               সেটা git থেকে **অক্ষরে অক্ষরে** ফিরিয়ে আনা হয়েছে।
               ⇒ আঙুলের ছাপ এখন **ঠিক তিন জায়গায়** — Login · IN TIME · Refund।
               ⛔ ভবিষ্যতে এই ব্লক সরানো যাবে না, TK নিজে না বললে। */
            // ধাপ ৪ — আঙুলের ছাপ (+ ধাপ ৫ সেভ) — এখন `startBiometricThenSaveInTime()`-এ,
            // যাতে V650-এর GPS-exempt পথও এই একই, প্রমাণিত ধাপটাই ব্যবহার করে।
            startBiometricThenSaveInTime(onSaved)
        }
    }

    /**
     * ধাপ ৪ (আঙুলের ছাপ) + ধাপ ৫ (সার্ভারে সেভ) — GPS-পাহারা পেরিয়ে আসা সাধারণ
     * পথ, আর V650-এর নাম-করা GPS-exempt পথ — দুটোই এই একই ফাংশন ব্যবহার করে,
     * তাই নিরাপত্তার এই শেষ স্তরটা (আঙুল/পাসওয়ার্ড) কখনো আলাদা হয়ে যেতে
     * পারে না।
     */
    /**
     * 🏠🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"তাছাড়া সে যদি Work from
     * Home করতে চায় তার ব্যাবস্থা যেন থাকে এবং মাস্টারের অনুমতি নেওয়া জরুরী
     * Work From Home এর ক্ষেত্রে"*।
     *
     * স্টাফ কারণ লিখে অনুরোধ পাঠান — অনুরোধটা মাস্টারের Briefing পর্দায়
     * "PENDING WORK FROM HOME REQUESTS"-এ বসে। মাস্টার Approve করলে স্টাফের
     * ফোনে নোটিশ যায়, আর তখন IN TIME চাপলে GPS-ধাপটা বাদ পড়ে।
     * ⛔ এই বাক্স থেকে হাজিরা **বসে না** — শুধু অনুরোধ যায়।
     */
    private fun askWorkFromHome(onSaved: () -> Unit) {
        val user = com.tkbiswas.pilesclinic.native.NativeSession.current(this)
        val input = android.widget.EditText(this).apply {
            hint = "Why are you working from home today?"
            setSingleLine(false)
            setPadding(ModuleUi.dp(this@WorkNotebookActivity, 14), ModuleUi.dp(this@WorkNotebookActivity, 12),
                ModuleUi.dp(this@WorkNotebookActivity, 14), ModuleUi.dp(this@WorkNotebookActivity, 12))
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ModuleUi.dp(this@WorkNotebookActivity, 18), ModuleUi.dp(this@WorkNotebookActivity, 6),
                ModuleUi.dp(this@WorkNotebookActivity, 18), 0)
            addView(input)
        }
        try {
            val d = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "🏠 Work From Home"))
                .setView(box)
                .setPositiveButton("Ask Master", null)
                .setNegativeButton("Close", null)
                .create()
            d.show()
            com.tkbiswas.pilesclinic.native.PremiumAlert.paint(d)
            d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val reason = input.text.toString().trim()
                if (reason.isBlank()) {
                    android.widget.Toast.makeText(this, "Please write the reason", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                d.dismiss()
                android.widget.Toast.makeText(this, "Sending...", android.widget.Toast.LENGTH_SHORT).show()
                Thread {
                    val msg = com.tkbiswas.pilesclinic.native.WfhRequests.request(
                        applicationContext,
                        user?.mobile ?: "", staffCode,
                        (user?.name ?: "").ifBlank { staffCode },
                        user?.branch ?: "", reason
                    )
                    runOnUiThread {
                        /* মাস্টার ইতিমধ্যে অনুমতি দিয়ে থাকলে সঙ্গে সঙ্গেই হাজিরা
                           বসানো যায় — স্টাফকে আর অপেক্ষা করতে হয় না। */
                        wfhApprovedToday = null
                        inTimeMessage("🏠 Work From Home", msg, "#0B2B59",
                            retry = { startInTimeFlow(onSaved) })
                    }
                }.start()
            }
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, "Could not open", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBiometricThenSaveInTime(onSaved: () -> Unit) {
            /* 🔴🔒 V500 (২১.০৮.২০২৬) — TK-এর স্পষ্ট সিদ্ধান্ত:
               আমি জানিয়েছিলাম, হাজিরায় ফোনের PIN খুলে দিলে কেউ সহকর্মীকে
               PIN বলে দিয়ে হাজিরা বসিয়ে নিতে পারে (আর সেই হাজিরাতেই বেতন
               গোনা হয়)। TK সব জেনে **"হ্যাঁ"** বলেছেন।
               ⇒ তাই হাজিরাতেও এখন `promptUnlock()` — **আঙুল অথবা ফোনের
                 পাসওয়ার্ড**, অ্যাপ খোলার মতোই এক নিয়ম।
               ⛔ ক্লিনিকে আছেন কিনা (GPS) যাচাই আগের মতোই আছে — সেটাই এখন
                 সবচেয়ে শক্ত পাহারা (V650-এর নাম-করা exempt-স্টাফ বাদে)। */
            com.tkbiswas.pilesclinic.native.BiometricGate.promptUnlock(
                this,
                // 🔤 V509 (২১.০৮.২০২৬, TK-নির্দেশ "এই ধরনের বাংলা থাকবে না"):
                // তালার পর্দার লেখা ইংরেজি — নিয়ম ও GPS পাহারা অপরিবর্তিত।
                "Attendance",
                "Use your fingerprint, or your phone password"
            ) { bio ->
                if (!bio.ok) {
                    val r = bio.reason
                    val canRetry = r == BiometricGate.Reason.FAILED ||
                        r == BiometricGate.Reason.CANCELLED ||
                        r == BiometricGate.Reason.HW_UNAVAILABLE ||
                        r == BiometricGate.Reason.LOCKOUT
                    val notEnrolled = r == BiometricGate.Reason.NONE_ENROLLED
                    inTimeMessage("Fingerprint", bio.message, "#A8281C",
                        // 🔒 V650 — পুরনো আচরণ অক্ষত রাখতে আবার পুরো
                        // `startInTimeFlow()` থেকেই শুরু হয় (GPS-ধাপসহ,
                        // সাধারণ স্টাফের জন্য) — exempt স্টাফের জন্যও ঠিকই
                        // কাজ করে (তালিকায় থাকলে আবার সরাসরি এখানেই ফেরত আসবেন)।
                        retry = if (canRetry) ({ startInTimeFlow(onSaved) }) else null,
                        extraLabel = if (notEnrolled) "Open Settings" else null,
                        extra = if (notEnrolled) ({
                            BiometricGate.openEnrollSettings(this)
                        }) else null)
                    return@promptUnlock
                }
                // ধাপ ৫ — সার্ভারে atomic সেভ
                saveInTimeOnServer(onSaved)
            }
    }

    /** ধাপ ৫ — সার্ভারই সব ঠিক করে; অ্যাপ কিছু পাঠায় না, কিছু ঠিকও করে না। */
    private fun saveInTimeOnServer(onSaved: () -> Unit) {
        android.widget.Toast.makeText(this, "Saving attendance..."   /* 🔤 V519 */, android.widget.Toast.LENGTH_SHORT).show()
        Thread {
            val res = AttendanceRepository.markCheckIn(this)
            runOnUiThread {
                when (res.status) {
                    AttendanceRepository.Status.SAVED, AttendanceRepository.Status.ALREADY -> {
                        // পর্দার তথ্য সার্ভারের সত্যি দিয়ে মিলিয়ে নেওয়া
                        if (res.checkIn.isNotBlank()) day.put("check_in", res.checkIn)
                        markReminderFlag("in", true)
                        noteDutyBranch(res.branch)
                        android.widget.Toast.makeText(this,
                            res.message.ifBlank { "Attendance done." },   // 🔤 V519
                            android.widget.Toast.LENGTH_LONG).show()
                        onSaved()
                    }
                    AttendanceRepository.Status.ON_LEAVE -> {
                        markReminderFlag("in", true)
                        inTimeMessage("On leave today", res.message, "#0A5C33")   // 🔤 V519
                        render()
                    }
                    AttendanceRepository.Status.NOT_STAFF ->
                        inTimeMessage("IN TIME", res.message, "#0B2B59")
                    /* 🕒🔒 V1243 (TK-নির্দেশ) — চেম্বারের সময়ের বাইরে হাজিরা নয়।
                       ⛔ "আবার চেষ্টা করুন" বোতাম নেই — বারবার চেপে লাভ নেই,
                          সময় হলে তবেই হবে। */
                    AttendanceRepository.Status.OUTSIDE_HOURS ->
                        inTimeMessage("IN TIME", res.message, "#A8281C")
                    AttendanceRepository.Status.INACTIVE, AttendanceRepository.Status.SUSPENDED -> {
                        // মাস্টার বন্ধ করে দিয়েছেন — সার্ভারই জানাল (TK §১১)
                        inTimeMessage("Account closed", res.message, "#A8281C")   // 🔤 V519
                    }
                    AttendanceRepository.Status.NETWORK, AttendanceRepository.Status.ERROR, AttendanceRepository.Status.NO_PROFILE ->
                        inTimeMessage("Attendance", res.message.ifBlank { "Could not save right now." },   // 🔤 V519
                            "#A8620B", retry = { saveInTimeOnServer(onSaved) })
                }
            }
        }.start()
    }

    /**
     * 🏥🔒 V1179 (০৭.০৯.২০২৬, TK-নির্দেশ) — **অন্য ব্রাঞ্চে ডিউটি করলে
     * খাতার সারিতে সেই ব্রাঞ্চই বসে।**
     *
     * সার্ভারের `wn.mark_check_in()` ব্রাঞ্চ নেয় `hr.staff_profiles.branch`
     * থেকে — অর্থাৎ স্টাফের **নিজের** ব্রাঞ্চ। তাই কোচবিহারে ডিউটি করা
     * জলপাইগুড়ির স্টাফের হাজিরা জলপাইগুড়ির নামেই বসত।
     * ⇒ IN TIME চাপার সময় GPS যে ক্লিনিক চিনেছে ([dutyBranch]) সেটা আলাদা
     *   হলে সঙ্গে সঙ্গে সারিটার `branch` ঘরটা ঠিক করে দেওয়া হয়।
     *
     * ⛔ শুধু `branch` ঘরটাই লেখা হয় (upsert-এ যে ঘরগুলো পাঠানো হয় কেবল
     *    সেগুলোই বদলায়) — `check_in` · `check_out` · `is_leave` কিছুই ছোঁয়া হয় না।
     * ⛔ এক রকম হলে একটাও নেট-কল হয় না।
     * ⛔ `wn.notebook_days`-এর `branch` ঘরটা প্রকল্পের কোথাও ছাঁকনি বা
     *    হিসাবে ব্যবহার হয় না (মেপে দেখা হয়েছে), তাই বেতন/হাজিরার কোনো
     *    হিসাব এতে বদলায় না — এটা শুধু **তথ্য**।
     */
    /* ═══════════════════════════════════════════════════════════════════
       📌🔒 V1200 (০৮.০৯.২০২৬, TK-নির্দেশ ও ফটো-প্রুফ পাশ) — **PLAN MY DAY**
       TK: *"work from home / work on another branch — স্টাফ এরকম যেন আগে থেকে
       বিবেচনা করে নিতে পারে, আর সেই ক্ষেত্রে ৭ ঘন্টা ধরা হবে"*।
       ⇒ ⋮ → Plan My Day: আজ **বা আগামী** যেকোনো দিন বেছে —
          · 🏠 Work From Home — মাস্টারের অনুমতি লাগে (আগের নিয়মই)
          · 🚌 অন্য ব্রাঞ্চে ডিউটি — **শুধু জানানো** (TK-সিদ্ধান্ত), অনুমতি নয়
       ⛔ কোনো পুরনো পথ বদলায়নি — একই `wfh_requests` টেবিল, শুধু `kind`,
          `toBranch` ও ইচ্ছে-মতো তারিখ যোগ হলো।
       ═══════════════════════════════════════════════════════════════════ */
    private var todayPlan: JSONObject? = null
    private var todayPlanLoaded = false

    /** আজকের প্ল্যান একবার পড়ে রাখা — পর্দা যতক্ষণ খোলা, আর পড়া হয় না। */
    private fun loadTodayPlan() {
        if (todayPlanLoaded) return
        todayPlanLoaded = true
        val mob = mobile
        Thread {
            val p = try {
                com.tkbiswas.pilesclinic.native.WfhRequests.planFor(mob, todayIso())
            } catch (_: Throwable) { null }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (p != null) { todayPlan = p; render() }
            }
        }.start()
    }

    /** IN TIME-এর উপরের পট্টি — প্ল্যান না থাকলে `null` (কিছুই বসে না)। */
    private fun todayPlanCard(): LinearLayout? {
        loadTodayPlan()
        val p = todayPlan ?: return null
        val kind = p.optString("kind", com.tkbiswas.pilesclinic.native.WfhRequests.KIND_WFH)
        val status = p.optString("status", "")
        val branchTo = p.optString("toBranch", "")
        val isBranch = kind == com.tkbiswas.pilesclinic.native.WfhRequests.KIND_BRANCH
        val approved = status == com.tkbiswas.pilesclinic.native.WfhRequests.STATUS_APPROVED
        if (status == com.tkbiswas.pilesclinic.native.WfhRequests.STATUS_REJECTED) return null
        val fill = if (isBranch) "#E8F6ED" else "#FFF6E6"
        val stroke = if (isBranch) "#BFE3CD" else "#F0DCA8"
        val ink = if (isBranch) "#0A5C33" else "#8A5A00"
        val title = if (isBranch) "Today's plan — Duty at " + branchTo.uppercase()
                    else "Today's plan — Work From Home"
        val sub = when {
            isBranch -> "You told this in advance \u00b7 counted as 7 hours"
            approved -> "Approved by Master \u00b7 counted as 7 hours"
            else -> "Waiting for Master's approval"
        }
        val d = resources.displayMetrics.density
        fun dpx(v: Int) = (v * d).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpx(14), dpx(12), dpx(14), dpx(12))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpx(14).toFloat()
                setColor(android.graphics.Color.parseColor(fill))
                setStroke(dpx(1), android.graphics.Color.parseColor(stroke))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dpx(10) }
            addView(android.widget.TextView(this@WorkNotebookActivity).apply {
                text = if (isBranch) "🚌" else "🏠"; textSize = 18f
                setPadding(0, 0, dpx(12), 0)
            })
            addView(LinearLayout(this@WorkNotebookActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(android.widget.TextView(this@WorkNotebookActivity).apply {
                    text = title; textSize = 13.5f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(android.graphics.Color.parseColor(ink))
                })
                addView(android.widget.TextView(this@WorkNotebookActivity).apply {
                    text = sub; textSize = 11.5f
                    setTextColor(android.graphics.Color.parseColor(ink))
                    setPadding(0, dpx(3), 0, 0)
                })
            })
            addView(android.widget.TextView(this@WorkNotebookActivity).apply {
                text = if (isBranch || approved) "PLANNED" else "WAITING"
                textSize = 10.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setPadding(dpx(11), dpx(5), dpx(11), dpx(5))
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dpx(10).toFloat()
                    setColor(android.graphics.Color.parseColor(if (isBranch) "#0A7C3F" else "#B45309"))
                }
            })
        }
    }

    private fun planMyDayFlow() {
        var pickedDate = todayIso()
        var pickedKind = com.tkbiswas.pilesclinic.native.WfhRequests.KIND_WFH
        var pickedBranch = ""
        val d = resources.displayMetrics.density
        fun dpx(v: Int) = (v * d).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpx(20), dpx(8), dpx(20), dpx(4))
        }
        fun cap(t: String) = android.widget.TextView(this).apply {
            text = t; textSize = 10.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#8B98A9"))
            setPadding(0, dpx(10), 0, dpx(4))
        }
        val dateLine = android.widget.TextView(this).apply {
            text = dotDate(pickedDate); textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0B2B1C"))
            isClickable = true
            setPadding(dpx(12), dpx(11), dpx(12), dpx(11))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpx(11).toFloat()
                setColor(android.graphics.Color.parseColor("#FBFDFC"))
                setStroke(dpx(1), android.graphics.Color.parseColor("#E7ECEA"))
            }
            setOnClickListener {
                val c = java.util.Calendar.getInstance()
                val dp = android.app.DatePickerDialog(this@WorkNotebookActivity, { _, y, m, dd ->
                    pickedDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, dd)
                    text = dotDate(pickedDate)
                }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH))
                /* ⛔ অতীতের দিন বাছা যায় না — "আগে থেকে" পরিকল্পনার মানেই থাকত না। */
                try { dp.datePicker.minDate = System.currentTimeMillis() - 1000 } catch (_: Throwable) { }
                dp.show()
            }
        }
        val kindLine = android.widget.TextView(this).apply {
            text = "🏠  Work From Home"; textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0B2B1C"))
            isClickable = true
            setPadding(dpx(12), dpx(11), dpx(12), dpx(11))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpx(11).toFloat()
                setColor(android.graphics.Color.parseColor("#FBFDFC"))
                setStroke(dpx(1), android.graphics.Color.parseColor("#E7ECEA"))
            }
        }
        val branchLine = android.widget.TextView(this).apply {
            text = "Not chosen"; textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#8A93A0"))
            isClickable = true
            visibility = android.view.View.GONE
            setPadding(dpx(12), dpx(11), dpx(12), dpx(11))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dpx(11).toFloat()
                setColor(android.graphics.Color.parseColor("#FBFDFC"))
                setStroke(dpx(1), android.graphics.Color.parseColor("#E7ECEA"))
            }
        }
        val branchCap = cap("WHICH BRANCH").apply { visibility = android.view.View.GONE }
        val branches = try {
            com.tkbiswas.pilesclinic.native.ClinicLocations.ALL.map { it.displayName }
        } catch (_: Throwable) { emptyList() }
        branchLine.setOnClickListener {
            if (branches.isEmpty()) { ModuleUi.toast(this, "Branch list not found"); return@setOnClickListener }
            val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Which branch"))
                .setItems(branches.toTypedArray()) { _, w ->
                    pickedBranch = branches[w]
                    branchLine.text = pickedBranch
                    branchLine.setTextColor(android.graphics.Color.parseColor("#0B2B1C"))
                }
                .setNegativeButton("Close", null).create()
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
        }
        kindLine.setOnClickListener {
            val items = arrayOf("🏠  Work From Home", "🚌  Duty at another branch")
            val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
                .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "What is the plan"))
                .setItems(items) { _, w ->
                    pickedKind = if (w == 0) com.tkbiswas.pilesclinic.native.WfhRequests.KIND_WFH
                                 else com.tkbiswas.pilesclinic.native.WfhRequests.KIND_BRANCH
                    kindLine.text = items[w]
                    val show = (w == 1)
                    branchCap.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                    branchLine.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
                }
                .setNegativeButton("Close", null).create()
            dlg.show()
            try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
        }
        box.addView(cap("WHICH DAY")); box.addView(dateLine)
        box.addView(cap("WHAT")); box.addView(kindLine)
        box.addView(branchCap); box.addView(branchLine)
        box.addView(android.widget.TextView(this).apply {
            text = "Work From Home — master's approval needed.\n" +
                "Duty at another branch — no approval, master is only informed.\n" +
                "Both days are counted as 7 hours."
            textSize = 11.5f
            setTextColor(android.graphics.Color.parseColor("#8A5A00"))
            setPadding(dpx(2), dpx(12), 0, 0)
        })
        val dlg = androidx.appcompat.app.AlertDialog.Builder(this)
            .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "📌 Plan My Day"))
            .setView(box)
            .setPositiveButton("Send") { _, _ ->
                if (pickedKind == com.tkbiswas.pilesclinic.native.WfhRequests.KIND_BRANCH && pickedBranch.isBlank()) {
                    ModuleUi.toast(this, "Choose the branch"); return@setPositiveButton
                }
                ModuleUi.toast(this, "Sending...")
                val mob = mobile
                val br = NativeSession.current(this)?.branch ?: ""
                val nm = NativeSession.current(this)?.name ?: staffCode
                val dateP = pickedDate; val kindP = pickedKind; val toBr = pickedBranch
                Thread {
                    val msg = com.tkbiswas.pilesclinic.native.WfhRequests.request(
                        this, mob, staffCode, nm, br,
                        if (kindP == com.tkbiswas.pilesclinic.native.WfhRequests.KIND_BRANCH)
                            "Duty at " + toBr else "Work from home",
                        dateP, kindP, toBr
                    )
                    runOnUiThread {
                        ModuleUi.toast(this, msg)
                        todayPlanLoaded = false; todayPlan = null
                        render()
                    }
                }.start()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dlg.show()
        try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(dlg) } catch (_: Throwable) { }
    }

    private fun noteDutyBranch(homeBranch: String) {
        /* 🏠🔒 V1180 (০৭.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"Work from Home… মাস্টার
           অনুমতি দিলে সেটা 7 ঘন্টাই হিসাব করা হবে, কম বেশি হিসাবে সেদিনের জন্য
           হবে না"* (সকাল ১০টা–বিকেল ৫টা)। ⇒ ওই দিনের সারিতে `is_wfh` বসে, আর
           বেতনের ঘণ্টা-হিসাব সেটা দেখেই **৭ ঘণ্টা** ধরে।
           ⛔ স্টাফের আসল IN/OUT TIME মোছা হয় না — রেকর্ড হিসেবে থেকেই যায়;
              শুধু বেতনের গোনায় ওই দিনটা ৭ ঘণ্টা।
           ⛔ `is_wfh` ঘরটা ডেটাবেসে না থাকলেও হাজিরা ভাঙে না — নিচের
              `writeNotebookRowWithoutOptionalColumns()` ওটা বাদ দিয়ে আবার পাঠায়। */
        if (workedFromHome) {
            val codeW = staffCode
            val dateW = todayIso()
            Thread {
                try {
                    robustSaveNotebookDay(
                        JSONObject().put("staff_code", codeW).put("work_date", dateW).put("is_wfh", true)
                    )
                } catch (_: Throwable) { }
            }.start()
            try {
                android.widget.Toast.makeText(this,
                    "Work From Home today - counted as 7 hours",
                    android.widget.Toast.LENGTH_LONG).show()
            } catch (_: Throwable) { }
            return
        }
        /* 🚌🔒 V1200 (TK-সিদ্ধান্ত) — আগে থেকে জানানো "অন্য ব্রাঞ্চে ডিউটি" হলে
           ওই দিনের সারিতে `is_other_branch` বসে ⇒ বেতনের হিসাবে **৭ ঘণ্টা**।
           ⛔ ঘরটা কেবল **প্ল্যান থাকলেই** বসে; এমনি অন্য ব্রাঞ্চে গেলে আগের
              নিয়মই (আসল IN/OUT ধরে ঘণ্টা)। */
        run {
            val pl = todayPlan
            if (pl != null &&
                pl.optString("kind", "") == com.tkbiswas.pilesclinic.native.WfhRequests.KIND_BRANCH &&
                pl.optString("status", "") == com.tkbiswas.pilesclinic.native.WfhRequests.STATUS_APPROVED) {
                val toBr = pl.optString("toBranch", "").ifBlank { dutyBranch }
                val codeB = staffCode
                val dateB = todayIso()
                try { day.put("branch", toBr); day.put("is_other_branch", true) } catch (_: Throwable) { }
                Thread {
                    try {
                        robustSaveNotebookDay(
                            JSONObject().put("staff_code", codeB).put("work_date", dateB)
                                .put("branch", toBr).put("is_other_branch", true)
                        )
                    } catch (_: Throwable) { }
                }.start()
                try {
                    android.widget.Toast.makeText(this,
                        "Duty at " + toBr + " today - counted as 7 hours",
                        android.widget.Toast.LENGTH_LONG).show()
                } catch (_: Throwable) { }
                return
            }
        }
        val duty = dutyBranch
        if (duty.isBlank()) return
        if (duty.equals(homeBranch.ifBlank { NativeSession.current(this)?.branch ?: "" }, ignoreCase = true)) return
        try { day.put("branch", duty) } catch (_: Throwable) { }
        val code = staffCode
        val date = todayIso()
        Thread {
            try {
                robustSaveNotebookDay(
                    JSONObject().put("staff_code", code).put("work_date", date).put("branch", duty)
                )
            } catch (_: Throwable) { }
        }.start()
        try {
            android.widget.Toast.makeText(this, "Duty branch today: " + duty, android.widget.Toast.LENGTH_LONG).show()
        } catch (_: Throwable) { }
    }

    private fun writeNotebookRow(row: JSONObject): Boolean {
        var ok = try { ModuleAuth.upsertOnConflict("wn", "notebook_days", row, "staff_code,work_date") } catch (_: Throwable) { false }
        if (!ok) ok = try { ModuleAuth.upsert("wn", "notebook_days", row) } catch (_: Throwable) { false }
        if (!ok) ok = writeNotebookRowWithoutOptionalColumns(row)
        return ok
    }

    /** 🔵 শেষ চেষ্টা — টেবিলে হয়তো নেই এমন ঐচ্ছিক ঘরগুলো বাদ দিয়ে আবার পাঠায়,
     *  যাতে IN/OUT TIME-এর মতো আসল তথ্য কখনো হারিয়ে না যায়। ⛔ মূল `row`
     *  বদলানো হয় না (আলাদা কপি) — জমা-খাতার সারি অক্ষত থাকে, পরে SQL চালানোর
     *  পরে সেটা পূর্ণ তথ্যসহ আবার বসতে পারে। */
    private fun writeNotebookRowWithoutOptionalColumns(row: JSONObject): Boolean {
        // টেবিলে না-থাকতে পারা ঐচ্ছিক ঘর (আসল হাজিরার জন্য জরুরি নয়)।
        /* 🏠 V1180 — `is_wfh` ঘরটাও ঐচ্ছিক ধরা হলো: TK এখনো V1180-এর SQL
           না চালালে ওই ঘর ছাড়াই সারিটা বসবে, তাই IN/OUT TIME কখনো হারাবে না। */
        val optional = listOf("check_out_reason", "is_wfh", "is_other_branch")   // 🚌 V1200
        if (optional.none { row.has(it) }) return false
        val slim = try { JSONObject(row.toString()) } catch (_: Throwable) { return false }
        optional.forEach { slim.remove(it) }
        var ok = try { ModuleAuth.upsertOnConflict("wn", "notebook_days", slim, "staff_code,work_date") } catch (_: Throwable) { false }
        if (!ok) ok = try { ModuleAuth.upsert("wn", "notebook_days", slim) } catch (_: Throwable) { false }
        return ok
    }

    private fun robustSaveNotebookDay(row: JSONObject): Boolean {
        if (!ModuleAuth.isSignedIn) { try { ModuleAuth.signInCurrentSession(this) } catch (_: Throwable) { } }
        var ok = writeNotebookRow(row)
        if (!ok) {
            // লগইন-টোকেন মেয়াদ শেষ হতে পারে — গোপনে আবার লগইন করে একবার আবার চেষ্টা।
            try { ModuleAuth.signInCurrentSession(this) } catch (_: Throwable) { }
            ok = writeNotebookRow(row)
        }
        if (ok) WnNotebookQueue.remove(this, row) else WnNotebookQueue.enqueue(this, row)
        return ok
    }

    // 🔵 জমা থাকা দিনগুলো ক্লাউডে বসিয়ে দেয় (Work Notebook খোলার সময় — loadDay-এর আগে)।
    private fun flushPendingNotebook() {
        val pend = WnNotebookQueue.pending(this)
        if (pend.isEmpty()) return
        if (!ModuleAuth.isSignedIn) { try { ModuleAuth.signInCurrentSession(this) } catch (_: Throwable) { } }
        /* 🔎🔒 V1006 (০৩.০৯.২০২৬) — জমে থাকা মার্ক অনেকগুলো হলে এই অংশটাই
           মিনিটখানেক নিতে পারে। আগে পর্দায় কিছুই বোঝা যেত না; এখন কত নম্বরটা
           চলছে সেটা "Opening…" পর্দাতেই দেখা যায়।
           ⛔ কাজের ক্রম · সেভ · থ্রেড কিছুই বদলায়নি — শুধু একটা লেখা। */
        var i = 0
        for (r in pend) {
            i += 1
            OpenTrace.step(this, "7. saving pending marks  ($i/${pend.size})")
            if (writeNotebookRow(r)) WnNotebookQueue.remove(this, r)
        }
    }

    // 🔵 আগে জমা-থাকা দিন বসাও, তারপর আজকের দিন লোড করো — তাই স্ক্রিনে ঠিক তথ্যই দেখায়।
    private fun flushThenLoad() {
        /* 🔎🔒 V932 — শেষ দুটো ধাপও চিহ্নিত, যাতে "লগইন হয়ে গেল কিন্তু পর্দা
           আঁকতে গিয়ে আটকাল" — এই অবস্থাটাও ধরা পড়ে।
           ⛔ কাজের ক্রম · থ্রেড · সেভ কিছুই বদলায়নি। */
        Thread {
            flushPendingNotebook()
            runOnUiThread {
                OpenTrace.step(this, "8. drawing the day")
                loadDay()
                checkPendingLeaves()
                OpenTrace.done(this)
            }
        }.start()
    }

    // 🔴 B321 (03.08.2026, TK-অনুমোদিত মকআপ — "লক করে রাখুন") — সবুজ গ্রেডিয়েন্ট
    // হিরো হেডার, Staff Profile (B308)/Income & Expense (B312)-এর একই প্রমাণিত
    // প্যাটার্ন এখানেও (শুধু এই ফাইলে যোগ, অন্য মডিউল ছোঁয়া হয়নি)।
    /* 📌 V1200 (TK-নির্দেশ: *"plan my day — এটা উপরে ডান সাইডের ৩ ডট থাকবে,
       তার মধ্যে এটা থাকবে"*) — পট্টির ডানদিকে ⋮; `menu` না দিলে হুবহু আগের চেহারা। */
    private fun hero(title: String, subtitle: String, titleSize: Float = 19f,
                     menu: List<Pair<String, () -> Unit>> = emptyList()): LinearLayout {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ModuleUi.dp(this@WorkNotebookActivity, 16), ModuleUi.dp(this@WorkNotebookActivity, 16),
                ModuleUi.dp(this@WorkNotebookActivity, 16), ModuleUi.dp(this@WorkNotebookActivity, 16))
            background = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                intArrayOf(android.graphics.Color.parseColor("#0B4F2A"), android.graphics.Color.parseColor("#0B8A3E"))
            ).apply { cornerRadius = ModuleUi.dp(this@WorkNotebookActivity, 16).toFloat() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = ModuleUi.dp(this@WorkNotebookActivity, 12) }
        }
        h.addView(android.widget.TextView(this).apply {
            text = title; textSize = titleSize
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
        })
        h.addView(android.widget.TextView(this).apply {
            text = subtitle; textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#E8F5EC"))
            setPadding(0, ModuleUi.dp(this@WorkNotebookActivity, 3), 0, 0)
        })
        if (menu.isEmpty()) return h
        // ⋮ থাকলে শিরোনাম আর ⋮ পাশাপাশি বসে (লেখা এক অক্ষরও বদলায় না)
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        while (h.childCount > 0) {
            val v = h.getChildAt(0); h.removeViewAt(0); texts.addView(v)
        }
        h.orientation = LinearLayout.HORIZONTAL
        h.gravity = android.view.Gravity.CENTER_VERTICAL
        h.addView(texts)
        h.addView(android.widget.TextView(this).apply {
            text = "⋮"; textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(ModuleUi.dp(this@WorkNotebookActivity, 16), 0, ModuleUi.dp(this@WorkNotebookActivity, 12), 0)   // 🔎 V1254
            isClickable = true
            setOnClickListener { v ->
                try {
                    val pm = android.widget.PopupMenu(this@WorkNotebookActivity, v)
                    menu.forEachIndexed { i, (label, _) -> pm.menu.add(0, i, i, label) }
                    pm.setOnMenuItemClickListener { mi -> menu.getOrNull(mi.itemId)?.second?.invoke(); true }
                    pm.show()
                } catch (_: Throwable) { menu.firstOrNull()?.second?.invoke() }
            }
        })
        return h
    }

    // 🔴 B322 (03.08.2026, TK-নির্দেশ — "কোনোটা কোনোটার গায়ে ঘেঁষে না যায়"):
    // `ModuleUi.input()` নিজে থেকে কোনো margin/spacing দেয় না — তাই পরপর কয়েকটা
    // ফিল্ড লেবেল ছাড়া সরাসরি একটার পর একটা বসালে (Work Entries/Outside Calls)
    // ঘেঁষে থাকত। এই হেল্পার প্রতিটা ফিল্ডের উপরে/নিচে ছোট ফাঁক যোগ করে।
    // 🔴🔒 B507 (06.08.2026, TK-নির্দেশ — "স্ক্রল করার দরকার না পড়ে,
    // যতটা সম্ভব একসাথে দেখা যাক") — এই পাতার লেবেলগুলোর মাঝের ফাঁকা
    // জায়গা কমাতে একটা আলাদা, সংক্ষিপ্ত লেবেল-স্টাইল — শুধু এই ফাইলেই
    // ব্যবহার হয়, `ModuleUi.label()` (বাকি সব মডিউলে ব্যবহৃত) একটুও
    // ছোঁয়া হয়নি, তাই অন্য কোনো পাতায় প্রভাব পড়েনি।
    private fun compactLabel(text: String): TextView = TextView(this).apply {
        this.text = text; textSize = 12f
        setTextColor(android.graphics.Color.parseColor("#5B6B82"))
        setPadding(0, ModuleUi.dp(this@WorkNotebookActivity, 3), 0, ModuleUi.dp(this@WorkNotebookActivity, 1))
    }

    // 🔴🔒 B508 (06.08.2026, TK-নির্দেশ — HTML মকআপে "B" অপশন বেছে) —
    // ছোট বাক্সের গ্রিড: প্রতিটা ঘর (Enquiry/Registration/Today Patient/
    // App Calls/রিসিভ ফোন/Total call) এখন একটা ছোট, বর্ডার-করা বাক্সে
    // — উপরে ছোট লেবেল, নিচে বড় সংখ্যা/ইনপুট। শুধু এই ফাইলে ব্যবহৃত।

    /* 🔆🔒 V1204 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"যেগুলো দিল সেগুলো উজ্জ্বলতা
       কম থাকলে ঠিক আছে, কিন্তু যেখানে সংখ্যা আছে সেগুলোর উজ্জ্বলতা বেশি হতে হবে —
       আজকের কল ০ হলে কম, আজকের পেশেন্ট ২ হলে বেশি"*।
       ⇒ ঘরের সংখ্যাটা ০-র বেশি হলে গাঢ় ও বোল্ড; ০ বা ফাঁকা বা "…" হলে হালকা ধূসর।
       ⛔ কোনো সংখ্যা বদলায় না — শুধু রং। EditText-এও চলে, তাই স্টাফ টাইপ করার
         সঙ্গে সঙ্গেই রং বদলায় (TextWatcher)। */
    private fun brightIfNonZero(tv: TextView) {
        fun paint() {
            val t = tv.text?.toString()?.trim() ?: ""
            val bright = (t.replace(",", "").toDoubleOrNull() ?: 0.0) > 0.0
            tv.setTextColor(android.graphics.Color.parseColor(if (bright) "#0B2B59" else "#B9C0C8"))
            tv.typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                if (bright) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
        }
        paint()
        tv.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) { paint() }
        })
    }

    private fun gridCell(label: String, valueView: View): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ModuleUi.dp(this@WorkNotebookActivity, 8), ModuleUi.dp(this@WorkNotebookActivity, 6), ModuleUi.dp(this@WorkNotebookActivity, 8), ModuleUi.dp(this@WorkNotebookActivity, 6))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = ModuleUi.dp(this@WorkNotebookActivity, 8).toFloat()
                setColor(android.graphics.Color.parseColor("#F7F9FB"))
                setStroke(1, android.graphics.Color.parseColor("#E5E9ED"))
            }
            addView(TextView(this@WorkNotebookActivity).apply {
                text = label; textSize = 9.5f
                setTextColor(android.graphics.Color.parseColor("#6B7280"))
            })
            (valueView.parent as? ViewGroup)?.removeView(valueView)
            valueView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 2) }
            // ✏️🔒 B522 (06.08.2026, TK-নির্দেশ — "সংখ্যাগুলো যেন বোল্ড থাকে")
            if (valueView is TextView) {
                valueView.textSize = 16f; valueView.setPadding(0, 0, 0, 0); valueView.background = null
                valueView.setTypeface(valueView.typeface, android.graphics.Typeface.BOLD)
                brightIfNonZero(valueView)   // 🔆 V1204
            }
            addView(valueView)
        }
    }

    private fun gridRow(cellA: View, cellB: View): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
            val lpA = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = ModuleUi.dp(this@WorkNotebookActivity, 4) }
            val lpB = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = ModuleUi.dp(this@WorkNotebookActivity, 4) }
            cellA.layoutParams = lpA
            cellB.layoutParams = lpB
            addView(cellA); addView(cellB)
        }
    }

    private fun spacedField(field: android.widget.EditText): android.widget.EditText {
        field.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 3); bottomMargin = ModuleUi.dp(this@WorkNotebookActivity, 1) }
        return field
    }

    // 🔴 B328 (03.08.2026, TK-নির্দেশ — "Work Entry-র Time টাইপ করা কঠিন,
    // Notebook-এর মতো সহজ রাখুন"): টাইপ করার বদলে ট্যাপ করলে ফোনের নিজস্ব
    // TimePickerDialog খোলে — লেখা লাগে না, শুধু বাছতে হয়। ফিল্ড নিজে
    // non-focusable/non-typing রাখা হয়েছে (Android-এর প্রমাণিত কৌশল, EditText-কে
    // "tap to pick" বোতামের মতো আচরণ করানোর জন্য), তাই কখনো ভুল ফরম্যাট টাইপ
    // হওয়ার ঝুঁকি নেই।
    private fun timePickerField(hint: String): android.widget.EditText {
        val field = ModuleUi.input(this, hint)
        field.isFocusable = false
        field.isFocusableInTouchMode = false
        field.isCursorVisible = false
        field.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(this, { _, hour, minute ->
                field.setText(String.format(Locale.US, "%02d:%02d", hour, minute))
            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show()
        }
        return field
    }

    // 🔴 B331 (03.08.2026, TK-নির্দেশ — WhatsApp Share ফরম্যাট) — তারিখ সবসময়
    // DOT ফরম্যাটে (প্রজেক্টের GLOBAL RULE, TK নিজে নিশ্চিত করেছেন — স্ল্যাশ
    // নয়)। "YYYY-MM-DD" থেকে "DD.MM.YYYY"।
    private fun dotDate(iso: String): String {
        val p = iso.split("-")
        return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso   // 🔴 V1158
    }

    /** "HH:mm" (২৪-ঘণ্টা, ডাটাবেসে যেভাবে থাকে) থেকে "h.mm AM/PM" — TK-এর
     *  নমুনার হুবহু ফরম্যাট ("9.30 AM")। */
    private fun displayTime12(hhmm: String): String {
        if (hhmm.isBlank()) return ""
        /* 🔴🔒 V1162 (০৭.০৯.২০২৬, TK: *"টাইম-পিকারের ভিতরের ২৪-ঘণ্টার ঘর"*)।
           **মেপে পাওয়া দোষ:** "Fix Attendance" দিয়ে সময় শুধরালে ঘরে বসে
           `HH:mm:ss` (তিন টুকরো), অথচ এখানে শর্ত ছিল ঠিক দুই টুকরো ⇒ ওই
           সারিগুলো পর্দায় ও WhatsApp-এ কাঁচা **09:15:00** হয়ে দেখাত।
           ⇒ এখন দুই ও তিন টুকরো দুটোই চেনে। ⛔ জমা লেখায় হাত পড়েনি। */
        val p = hhmm.split(":"); if (p.size != 2 && p.size != 3) return hhmm
        val h24 = p[0].toIntOrNull() ?: return hhmm
        val m = p[1]
        val ampm = if (h24 < 12) "AM" else "PM"
        val h12 = when { h24 == 0 -> 12; h24 > 12 -> h24 - 12; else -> h24 }
        return "$h12.$m $ampm"
    }

    /** এখনকার সময় "6.30 PM" স্টাইলে। 🔴 V1158 — TK-এর নতুন নমুনা
     *  ("31/12/2026 : 3.15 PM") অনুযায়ী AM/PM বড় হাতে ও আগে একটা ফাঁক। */
    private fun shareTimeLabel(): String {
        val cal = java.util.Calendar.getInstance()
        val h24 = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val m = cal.get(java.util.Calendar.MINUTE)
        // 🔴 V1158 — TK-এর লক করা নিয়ম: AM/PM বড় হাতে, অঙ্কের পরে একটা ফাঁক।
        val ampm = if (h24 < 12) "AM" else "PM"
        val h12 = when { h24 == 0 -> 12; h24 > 12 -> h24 - 12; else -> h24 }
        return String.format(Locale.US, "%d.%02d %s", h12, m, ampm)
    }

    // 🔴 B329 — Daily Report-এর "Notes:" এখন Work Entries-এর "what you did"
    // লেখাগুলো থেকে তৈরি হয় (স্বতন্ত্র Notes ফিল্ড সরানোর পর)।
    private fun workEntriesSummary(): String {
        val arr = day.optJSONArray("manual_entries") ?: JSONArray()
        val lines = ArrayList<String>()
        for (i in 0 until arr.length()) {
            val t = ns(arr.getJSONObject(i), "text").trim()
            if (t.isNotBlank()) lines.add(t)
        }
        return lines.joinToString("\n")
    }

    // 🔴 B342 (03.08.2026, TK-নির্দেশ, রাতে — "যেগুলো রিপোর্ট হিসেবে গ্রুপে
    // যায় শুধু সেগুলোই ফর্ম আকারে থাকবে, প্রশ্ন-উত্তর, শেষে Submit"):
    // পুরনো আলাদা আলাদা কার্ড (Check-in/Auto-stats/Work-Entries/Outside-Calls/
    // Reports/Home) — সব মিশিয়ে **একটাই ফর্ম**, রিপোর্টে যা যা যায় ঠিক সেই
    // ক্রমে, শেষে একটাই "Submit Report to Master" বোতাম (Save + Submit +
    // WhatsApp Share — তিনটে আলাদা বোতাম চাপার বদলে একবারেই)।
    // ⛔ IN TIME/OUT TIME (TK স্পষ্ট করেছেন — "অটোমেটিক হবে, সিলেক্ট করা যাবে
    // না"): আগের মতোই শুধু বোতাম, চাপলেই এখনকার সময় বসে, কোনো টাইম-পিকার/
    // এডিট নেই।
    // ⛔ New Enquiry/Registration/App Calls/Total call — AUTO, ধূসর বাক্স,
    // EditText না (ModuleUi.autoValue) — কখনো টাইপ করা যায় না।
    // ⛔ Today Patient/Outside Calls Today — আগের মতোই এডিটযোগ্য।
    // ⛔ Notes — আগের একাধিক Work-Entry-লিস্টের বদলে এখন একটাই টেক্সট-বাক্স
    // (নতুন `day_note` কলাম-সদৃশ ফিল্ড); আজ ইতিমধ্যে কিছু Work Entry লেখা
    // থাকলে সেগুলোর লেখা প্রথমবার এই বাক্সে প্রি-ফিল হয়ে আসবে (হারাবে না),
    // এরপর থেকে নতুন লেখা সরাসরি এই একটা ফিল্ডেই সেভ হবে।
    // ⛔ Monthly Report/My Reports — মোছা হয়নি, শুধু ছোট লিংক হিসেবে নিচে
    // রাখা হলো (TK শুধু Daily-র ফর্ম নিয়ে বলেছিলেন, এই দুটো ছোঁয়া হয়নি)।
    // ⛔ পুরনো `report("daily",...)`/Add-Entry/drawEntries ফাংশন কোডে অক্ষত
    // আছে (মোছা হয়নি, শুধু এই পর্দা থেকে আর ডাকা হয় না)।
    // 🔴 B408/B409 (04.08.2026, TK-নির্দেশ — "অনেকের ফোনে ঘরে চাপ দিলে কাজ
    // করছে না, অন্য জায়গায় (Notes, TYPE_CLASS_TEXT) টাইপ করা যাচ্ছে")।
    // **আসল পার্থক্য ধরে বার করা হলো (আন্দাজ না):** "Today Patient" ও
    // "Outside Calls Today" দুটোই আগে `InputType.TYPE_CLASS_NUMBER` (একা,
    // কোনো flag ছাড়া) ব্যবহার করত — কিছু ফোনের (Xiaomi/Vivo/Oppo-ঘরানার)
    // কাস্টম কীবোর্ড এই একা-`TYPE_CLASS_NUMBER`-এ tap করলে numeric keypad-ই
    // খুলতে ব্যর্থ হয় (পরিচিত OEM-keyboard সীমাবদ্ধতা)। "Today Patient"
    // আগে থেকেই সংখ্যা বসানো থাকে (Chamber Attendance থেকে auto-suggest)
    // বলে স্টাফ প্রায়ই সেটা আসলে টাইপ করে দেখেনই না — তাই ওখানে সমস্যাটা
    // এতদিন আড়ালে ছিল, "Outside Calls Today" সবসময় ফাঁকা থাকে বলে সেখানেই
    // প্রথম ধরা পড়ল। "Notes" (`TYPE_CLASS_TEXT`) ভিন্ন input-type বলে
    // ওখানে সমস্যা নেই — এই প্যাটার্নটাই চূড়ান্ত প্রমাণ।
    // **সমাধান:** দুটো ঘরেই এখন `TYPE_CLASS_TEXT` + `DigitsKeyListener`
    // (শুধু ০-৯ টাইপ করা যায়, অন্য অক্ষর কীবোর্ডেই আটকে যায়) — এই কম্বিনেশন
    // সাধারণ টেক্সট-কীবোর্ড খোলে (যেটা সব ফোনেই নির্ভরযোগ্যভাবে খোলে,
    // Notes-এর মতোই), কিন্তু ভুলবশত অক্ষর/স্পেস লেখা যাবে না। সাথে
    // `requestFocus()`+`showSoftInput()` বাড়তি নিরাপত্তা হিসেবে রাখা হলো।
    // ⛔ সেভ-হওয়া মান/কলাম/গণনা-লজিক এক অক্ষরও বদলায়নি — শুধু কীবোর্ড
    // খোলার নির্ভরযোগ্যতা।
    private fun numericField(hint: String): EditText = ModuleUi.numberInput(this, hint)

    /* 👨‍⚕️🔒 V1032 (০৪.০৯.২০২৬, TK-নির্দেশ: *"কতজন ডাক্তারের কাছে ভিজিট করেছে
       তাকে ম্যানুয়ালি এন্ট্রি করতে হয়েছে"*) — আজকের ডাক্তার-ভিজিট নিজে থেকে গোনা।

       🔒 **নিজের যাচাইয়ে ধরা দুটো দোষ, তাই এই ধরনটাই নেওয়া হলো:**
       ১) প্রথমে গোনাটা রিপোর্ট পাঠানোর ঠিক আগে **মূল থ্রেডে** করা হয়েছিল —
          নেট ধীর হলে পর্দা ৪ সেকেন্ড আটকে থাকত (TK: *"স্লো হয়ে যায়"*)।
       ২) দিনের রিপোর্ট **পাঁচ জায়গায়** তৈরি হয়; একটাতে বসালে বাকি পথে
          লাইনটা উঠত না — TK কখনো দেখতেন, কখনো না।
       ⇒ এখন পর্দা খোলার সময় **একবারই**, পিছনের থ্রেডে গোনা হয় ও এখানে জমা
         থাকে; প্রতিটা রিপোর্ট শুধু এই সংখ্যাটাই পড়ে। কোথাও অপেক্ষা নেই।
       ⛔ উত্তর না এলে `-1` থাকে ⇒ লাইনটা ওঠেই না, রিপোর্ট হুবহু আগের মতোই। */
    @Volatile private var docVisitToday = -1
    /* 👨‍⚕️🔒 V1334 (TK-নির্দেশ "খ") — ফিল্ড-স্টাফের (RUPAM) MARK VISIT-এ
       গোনা আজকের আসল সংখ্যা। শুধু ফিল্ড-স্টাফের বেলায়ই ভরা হয় (নিচে
       `loadDocVisitToday()`); অন্য কারো জন্য -1-ই থেকে যায়, কিছুই বদলায় না। */
    @Volatile private var fieldVisitDocToday = -1

    private fun loadDocVisitToday() {
        try {
            val meMob = mobile
            val code = staffCode.ifBlank { mobile }
            val isField = com.tkbiswas.pilesclinic.native.FieldVisit.isFieldStaff(meMob)
            Thread {
                val n = try {
                    com.tkbiswas.pilesclinic.native.DoctorVisitDayCount.todayCount(meMob)
                } catch (_: Throwable) { -1 }
                docVisitToday = n
                if (isField) {
                    fieldVisitDocToday = try {
                        com.tkbiswas.pilesclinic.native.FieldVisit.todayMarkVisitCount(code)
                    } catch (_: Throwable) { -1 }
                }
            }.start()
        } catch (_: Throwable) { }
    }

    /** রিপোর্টে বসানোর লাইন — গোনা না হলে (বা শূন্য হলে) ফাঁকা।
     *  🔒 V1334 (TK-নির্দেশ "খ", তালিকা সারি ৪৩৪) — ফিল্ড-স্টাফের (RUPAM)
     *  বেলায় এখন MARK VISIT-এ গোনা আসল সংখ্যাই বসে (পুরনো call-history-
     *  ভিত্তিক গোনা তাঁর জন্য কখনো সঠিক ছিল না — তিনি ফোন করেন না, সরাসরি
     *  গিয়ে দেখা করেন)। বাকি সব স্টাফের রিপোর্ট এক অক্ষরও বদলায়নি। */
    private fun docVisitLine(): String =
        if (com.tkbiswas.pilesclinic.native.FieldVisit.isFieldStaff(mobile))
            (if (fieldVisitDocToday > 0) "\nDoctor Visit: " + fieldVisitDocToday else "")
        else if (docVisitToday > 0) "\nDoctor Visit: " + docVisitToday else ""


    /* 🏍️🔒 V968 (০২.০৯.২০২৬, TK-নির্দেশ) — **ফিল্ড ভিজিট (শুধু RUPAM)।**
       TK: *"শুধু বাইরে ঘোরা স্টাফদের জন্য"* · *"RUPAM নিজে চাপবে"*।
       ⛔ অন্য কোনো স্টাফের পর্দায় এই বোতাম বা কার্ড একটুও ওঠে না — নিচের
          `FieldVisit.isFieldStaff()` ছাড়া কিছুই আঁকা হয় না।
       ⛔ পুরনো IN/OUT TIME-এর সেভ-লজিক এক অক্ষরও বদলায়নি; এটা শুধু পাশে বসা
          বাড়তি গোনা, ব্যর্থ হলেও হাজিরা আটকায় না। */
    private fun addFieldVisitPicker(form: LinearLayout) {
        if (!com.tkbiswas.pilesclinic.native.FieldVisit.isFieldStaff(this)) return
        val fv = com.tkbiswas.pilesclinic.native.FieldVisit
        if (fv.chosenMode(this).isBlank()) fv.chooseMode(this, fv.MODE_CHAMBER)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 8) }
        }
        lateinit var paint: () -> Unit
        val btnChamber = ModuleUi.buttonSoft(this, "At Chamber") {
            fv.chooseMode(this, fv.MODE_CHAMBER); paint()
        }
        val btnField = ModuleUi.buttonSoft(this, "Field Visit") {
            fv.chooseMode(this, fv.MODE_FIELD); paint()
        }
        for (b in listOf(btnChamber, btnField)) {
            b.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply {
                    leftMargin = ModuleUi.dp(this@WorkNotebookActivity, 3)
                    rightMargin = ModuleUi.dp(this@WorkNotebookActivity, 3)
                }
        }
        paint = {
            val field = fv.chosenMode(this) == fv.MODE_FIELD
            val on = android.graphics.Color.parseColor("#0F3D6B")
            val off = android.graphics.Color.parseColor("#EFF3F8")
            val onTx = android.graphics.Color.WHITE
            val offTx = android.graphics.Color.parseColor("#63748C")
            btnChamber.backgroundTintList = android.content.res.ColorStateList.valueOf(if (field) off else on)
            btnChamber.setTextColor(if (field) offTx else onTx)
            btnField.backgroundTintList = android.content.res.ColorStateList.valueOf(if (field) on else off)
            btnField.setTextColor(if (field) onTx else offTx)
        }
        paint()
        row.addView(btnChamber); row.addView(btnField)
        form.addView(row)
        form.addView(ModuleUi.body(this,
            "Select Field Visit only when you are going out on the bike. Location stays on until you mark OUT TIME."))
    }

    /** IN TIME হয়ে যাওয়ার পরে — চলতে থাকা লোকেশন-গোনার কার্ড।
     * 🛰️🔒 V1346 (১১.০৯.২০২৬, TK-নির্দেশ) — আগে শুধু ফিল্ড-স্টাফের (RUPAM)
     * জন্য ছিল, এখন সব আসল স্টাফের জন্য। শিরোনাম ফিল্ড-স্টাফের বেলায় আগের
     * মতোই "FIELD VISIT", বাকি সবার বেলায় "ATTENDANCE LOCATION" — কারণ
     * বাকিদের বাইক নিয়ে ঘোরা নেই, শুধু হাজিরার সময়টুকু লোকেশন-অন থাকে। */
    private fun addFieldVisitRunningCard(form: LinearLayout) {
        val fv = com.tkbiswas.pilesclinic.native.FieldVisit
        resumeFieldVisitIfNeeded()
        if (!fv.tracksAttendanceLocation(this) || !fv.isRunning(this)) return
        val isField = fv.isFieldStaff(this)
        form.addView(TextView(this).apply {
            text = if (isField) "FIELD VISIT  ·  RUNNING" else "ATTENDANCE LOCATION  ·  RUNNING"
            textSize = 12.5f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#0E6E8C"))
        })
        val started = fv.startedAt(this)
        /* ⚠️🔒 V1032 (০৪.০৯.২০২৬, TK-রিপোর্ট: রুপম সারাদিন ঘুরেছেন অথচ
           Field Visit Tracking ফাঁকা, কিলোমিটারও নেই)। **আসল কারণ (যাচাই
           করা):** ফোন একটাও অবস্থান না দিলে দূরত্ব গোনাই শুরু হয় না — অথচ
           এই লাইনটা সবসময় "Location on" লিখত, তাই স্টাফ ভাবতেন সব ঠিক আছে।
           ⇒ এখন অবস্থান না এলে **লাল সতর্কতা** ওঠে, স্টাফ সঙ্গে সঙ্গে বুঝবেন।
           ⛔ হাজিরা · গোনা · সেভ — কিছুই ছোঁয়া হয়নি, শুধু সত্যি কথাটা দেখানো। */
        val gotFix = fv.hasFix(this)
        form.addView(ModuleUi.body(this,
            "Hours " + fv.hoursText(started, System.currentTimeMillis()) +
                "   ·   Distance " + fv.kmText(fv.distanceMeters(this)) +
                (if (gotFix) "   ·   Location on" else "")))
        if (!gotFix) {
            form.addView(TextView(this).apply {
                text = "\u26A0\uFE0F LOCATION NOT WORKING - km is NOT being counted.\n" +
                    "Tap FIX LOCATION below, then allow Location for this app."
                textSize = 12.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(android.graphics.Color.parseColor("#B42318"))
                setPadding(
                    ModuleUi.dp(this@WorkNotebookActivity, 10), ModuleUi.dp(this@WorkNotebookActivity, 8),
                    ModuleUi.dp(this@WorkNotebookActivity, 10), ModuleUi.dp(this@WorkNotebookActivity, 8)
                )
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = ModuleUi.dp(this@WorkNotebookActivity, 10).toFloat()
                    setColor(android.graphics.Color.parseColor("#FDECEA"))
                    setStroke(ModuleUi.dp(this@WorkNotebookActivity, 1), android.graphics.Color.parseColor("#F2C6C0"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
            })
            /* 🛰️🔒 V1156 — V1032-এ শুধু **লেখা** ছিল *"অনুমতি দিয়ে আবার
               চেষ্টা করুন"*, অথচ অনুমতি দেওয়ার কোনো পথই স্টাফের সামনে খুলত না
               (অনুমতির বাক্স এই স্টাফের ফোনে কোনোদিন ওঠেই না — উপরে কারণ লেখা)।
               ⇒ এখন একটা বোতাম: দরকার মতো অনুমতির বাক্স · Location-এর পাতা ·
                 অ্যাপের Settings — যেটা লাগে সেটাই খোলে। */
            form.addView(ModuleUi.buttonSoft(this, "FIX LOCATION") {
                ensureFieldLocationReady(fromButton = true)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
            })
        }
        // 🛰️🔒 V1346 — RMP Doctors/MARK VISIT এখনো শুধু আসল ফিল্ড-স্টাফের
        // (RUPAM) জন্য — সাধারণ হাজিরা-লোকেশন সবার জন্য হলেও এই বোতাম নয়।
        if (isField) form.addView(ModuleUi.buttonSoft(this, "RMP Doctors - mark visits") {
            startActivity(android.content.Intent(this, FieldVisitActivity::class.java))
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
        })
    }

    /* 🏍️🔒 V968 (নিজে ধরা, TK-কে পাঠানোর আগেই) — **দুটো ফাঁক ঢাকা:**
       ১) ফোন রিস্টার্ট হলে বা Android সেবাটা মেরে ফেললে গোনা থেমে যেত আর
          কেউ আর চালু করত না ⇒ পর্দা খুললেই আবার চালু হয় (একই দিনের হিসাব
          ফোনেই জমা থাকে, তাই কিছু হারায় না)।
       ২) রাত ১২টায় নিজে-বন্ধ হওয়ার কাজটা সেবাটা বেঁচে থাকলে তবেই হত ⇒ এখন
          পর্দা খুললেও দেখা হয়, দিন পেরিয়ে গেলে সঙ্গে সঙ্গে বন্ধ ও AUTO CLOSED।
       ⛔ হাজিরার (IN/OUT TIME) কোনো লজিক এখানেও ছোঁয়া হয়নি। */
    private fun resumeFieldVisitIfNeeded() {
        // 🏍️🔒 V1364 — এই একই যাচাই এখন `FieldVisitControl.resumeIfNeeded()`-এ
        // (শেয়ার্ড, একই নিয়ম) — অ্যাপের যেকোনো পর্দা খোলার সময়ও চলে।
        // এখানে ডাকাটা রয়ে গেছে (দুবার চালানো নিরাপদ, idempotent), যাতে এই
        // পর্দা খোলার মুহূর্তেই কার্ডটা সঙ্গে সঙ্গে সঠিক অবস্থা দেখায়।
        com.tkbiswas.pilesclinic.native.FieldVisitControl.resumeIfNeeded(this)
    }

    /** OUT TIME বসার সঙ্গে সঙ্গে গোনা বন্ধ ও শেষ হিসাব ক্লাউডে। */
    private fun stopFieldVisitIfRunning() {
        try {
            val fv = com.tkbiswas.pilesclinic.native.FieldVisit
            if (!fv.isRunning(this)) return
            fv.endDay(this, auto = false)
            com.tkbiswas.pilesclinic.native.FieldVisitControl.stop(this)
            val ctx = applicationContext
            Thread { fv.push(ctx, ended = true, auto = false) }.start()
        } catch (_: Throwable) { }
    }

    private fun render() {
        backAction = { finish() }
        val isKishanganjStaff = (NativeSession.current(this)?.branch ?: "").trim().lowercase() == "kishanganj"
        val col = ModuleUi.screen(this, "")
        col.addView(hero(
            if (isKishanganjStaff) "🗒️ Today Work" else "🗒️ Today Work / আজকের কাজ",
            todayIso() + " · " + staffCode, 14f,
            /* ⋮🔒 V1204 (০৮.০৯.২০২৬, TK-নির্দেশ, হুবহু): *"monthly report · my reports —
               উপরে ডান সাইডে এলজি থ্রি ডটের মধ্যে থাকবে"*। নিচের ছোট নীল লিংক
               দুটো তুলে দিয়ে এখানে বসানো হলো — খোলা পর্দা দুটো এক অক্ষরও বদলায়নি। */
            listOf(
                "📌 Plan My Day" to { planMyDayFlow() },
                "📊 Monthly Report" to { report("monthly", todayIso().substring(0, 7)) },
                "🗂️ My Reports" to { history() }
            )
        ))

        val form = ModuleUi.card(this); col.addView(form)

        // 🔴🔒 B506 (06.08.2026, TK-নির্দেশ — "এই ধরনের ডেমি লেখা থাকবে
        // না") — "Attendance (auto — no manual time entry)" লেবেলটা
        // সম্পূর্ণ বাদ। IN TIME/OUT TIME বোতাম আর নিচের Leave-বক্স
        // নিজেরাই স্পষ্ট, আলাদা ব্যাখ্যা-লেবেলের দরকার নেই।
        // 🔴🔴 V511 (উপরের `dayFromCache`-এর বড় নোট দ্রষ্টব্য) — আজকের তথ্যই
        //   আনা যায়নি, আর এই ফোনে জমানো কপিও নেই। তখন **কিছুই আন্দাজ করা হয় না**:
        //   IN TIME বোতাম দেখানো হয় না (নইলে স্টাফ ভুল করে আবার IN চেপে দিতেন),
        //   OUT TIME-ও নয়। শুধু পরিষ্কার ভুল-বার্তা আর 🔄 Try again।
        if (dayLoadFailed) {
            form.addView(TextView(this).apply {
                text = "⚠️ Today's data could not be loaded."
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#B42318"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            form.addView(ModuleUi.body(this,
                "Your IN / OUT TIME is safe in the cloud - nothing is lost. Check internet and try again."))
            form.addView(ModuleUi.button(this, "🔄 Try again") { loadDay() }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 8) }
            })
            return
        }
        // 🔴 V511 — জমানো কপি দেখানো হচ্ছে, সেটা লুকিয়ে রাখা হয় না।
        if (dayFromCache) {
            form.addView(ModuleUi.body(this, "📴 Offline - showing this phone's saved copy"))
        }
        val isLeave = day.optBoolean("is_leave", false)
        if (isLeave) {
            val leaveReason = ns(day, "leave_reason")
            form.addView(ModuleUi.body(this, "🏖️ On Leave" + (if (leaveReason.isNotBlank()) ": $leaveReason" else "")))
            val cancelLeaveBtn = ModuleUi.button(this, "Cancel Leave") {
                // 🔵 B608 (10.08.2026, TK-নির্দেশ): ছুটি নেওয়া মানে সারাদিন ছুটি —
                // তাই "Cancel Leave" যেন casual toggle মনে না হয়; আগে নিশ্চিত করি
                // এটা ভুল-শোধরানো (ভুল করে ছুটি / আসলে এসে গেছেন)। "হ্যাঁ" দিলে তবেই
                // বাতিল হয়। ⛔ বাতিলের সেভ-লজিক (is_leave/flag/saveDay) একটুও বদলায়নি।
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setCustomTitle(com.tkbiswas.pilesclinic.native.PremiumAlert.header(this, "Cancel Leave"))
                    .setMessage(NoBengali.s("Marked leave by mistake? Cancel the leave and resume today's attendance?"))
                    .setPositiveButton(NoBengali.s("Yes, Cancel")) { _, _ ->
                        day.put("is_leave", false); day.put("leave_reason", "")
                        markReminderFlag("in", false); markReminderFlag("out", false)
                        saveDay { render() }
                    }
                    .setNegativeButton(NoBengali.s("No"), null)
                    .show().also { try { com.tkbiswas.pilesclinic.native.PremiumAlert.paint(it) } catch (_: Throwable) {} }
            }
            cancelLeaveBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
            form.addView(cancelLeaveBtn)
        } else {
            val inSet = ns(day, "check_in").isNotBlank()
            val outSet = ns(day, "check_out").isNotBlank()
            // 🔴🆕🔒 B466 (05.08.2026, TK-নির্দেশ, স্ক্রিনশট-আলোচনা) — নতুন
            // ধাপ-ভিত্তিক নিয়ম: IN TIME না হওয়া পর্যন্ত শুধু IN TIME/Mark as
            // Leave বোতাম দেখা যাবে, নিচের কোনো ঘরই (New Enquiry, Registration,
            // Today Patient, App Calls, ফোন রিসিভ, Total call, Notes) দেখানো
            // হবে না। IN TIME হয়ে গেলে তবেই সব ঘর দেখা যাবে (এখন এই সবগুলো
            // ঘর এই when-ব্লকের ভিতরে সরিয়ে আনা হয়েছে, আগে যেমন সবসময়
            // দেখাত তা বদলে গেছে)। OUT TIME হয়ে গেলে শুধু সারাংশ-লাইন, দিন
            // পুরোপুরি লক — পরের ক্যালেন্ডার দিনে (নতুন `day` রো) আবার
            // IN TIME থেকেই শুরু হবে (এটা এমনিতেই `loadDay()`-এর তারিখ-ভিত্তিক
            // রো-লোডের কারণে হয়, নতুন কিছু বানাতে হয়নি)। ⛔ সেভ-লজিক এক
            // অক্ষরও বদলায়নি — শুধু কখন কোন ঘর দেখা যাবে সেটা।
            when {
                outSet -> {
                    // 🔴 V432 (TK-রিপোর্ট ১৮.০৮.২০২৬) — লাইনটায় চাপ দিলে দিনের
                    //    রিপোর্ট আবার WhatsApp-এ পাঠানো যায় (ব্যাক করে ফিরে
                    //    এলেও আর আটকে থাকতে হয় না)। ⛔ সময় বা জমা কিছুই বদলায় না।
                    form.addView(ModuleUi.body(this,
                        "✅ IN ${timeWithLate(ns(day, "check_in"))}   ·   OUT ${outWithLate(ns(day, "check_out"))}"))
                    // 🔴 V433 (TK): "WhatsApp এ একবার পাঠানো হয়ে গেলে আর দেখানোর
                    //    দরকার নেই" — পাঠানো নিশ্চিত হলে সেই দিনের জন্য বোতাম নেই।
                    if (!isWaSent("out")) form.addView(ModuleUi.button(this, NoBengali.s("📤 রিপোর্ট আবার WhatsApp-এ পাঠান")) {
                        resendDailyReportWhatsApp()
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 8) }
                    })
                }
                inSet -> {
                    lateinit var patientsField: EditText
                    lateinit var ocCount: EditText
                    lateinit var notesField: EditText

                    val outBtn = ModuleUi.button(this, "OUT TIME") { askCheckOutReason(patientsField, ocCount, notesField) }
                    outBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
                    form.addView(outBtn)
                    form.addView(ModuleUi.body(this, "✅ IN TIME ${timeWithLate(ns(day, "check_in"))}"))
                    // 🏍️ V968 — চলতে থাকা ফিল্ড ভিজিটের হিসাব (শুধু RUPAM)।
                    addFieldVisitRunningCard(form)
                    // 🔴 V432 (TK-রিপোর্ট ১৮.০৮.২০২৬) — WhatsApp খোলার পরে ব্যাক
                    //    করে এলে আগে আর পাঠানোর কোনো উপায় ছিল না। এখন এই বোতামে
                    //    চাপলেই **সেই একই বার্তাটাই** আবার খোলে।
                    //    ⛔ IN TIME-এর সময় · রিমাইন্ডার · Master-নোটিফিকেশন কিছুই
                    //       আবার বসে না — তাই বারবার চাপলেও কোনো ক্ষতি নেই।
                    // 🔴 V433 (TK): পাঠানো নিশ্চিত হলে সেই দিনের জন্য আর দেখাবে না।
                    /* 🟢🔒 V590 (২৩.০৮.২০২৬, TK-নির্দেশ) — *"ইন টাইম যদি একবার
                       পাঠিয়ে দিয়ে থাকে, তাহলে ওটা একটু উজ্জ্বলতা কম থাকবে।"*
                       IN TIME চিহ্ন দেওয়ার সময়েই বার্তাটা একবার পাঠানো হয়ে যায়,
                       তাই এটা "আবার" পাঠানোর বোতাম — দিনের আসল কাজ (OUT TIME)
                       নয়। এখন হালকা রঙে (`buttonSoft`), তাই দুটো আর সমান
                       উজ্জ্বল দেখায় না।
                       ⛔ কখন দেখা যায় · চাপলে কী হয় — কিছুই বদলায়নি, শুধু রং। */
                    if (!isWaSent("in")) form.addView(ModuleUi.buttonSoft(this, NoBengali.s("📤 IN TIME আবার WhatsApp-এ পাঠান")) {
                        resendInTimeWhatsApp()
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
                    })

                    // 🔴🔒 B508 (06.08.2026, TK-নির্দেশ, "B" অপশন) — ছয়টা
                    // ঘরই এখন ৩ সারি × ২ কলামের ছোট বাক্সের গ্রিডে। ⛔
                    // প্রতিটা ভিউ-রেফারেন্স (enqVal/regVal/patientsField/
                    // appVal/ocCount/totalVal) এবং তাদের ভেতরের যুক্তি
                    // (auto-গণনা/লোকাল-ক্যাশ/সেভ) একটুও বদলায়নি — শুধু
                    // সাজানোর ধরন বদলেছে।
                    val enqVal = ModuleUi.autoValue(this, "…")
                    val regVal = ModuleUi.autoValue(this, "…")
                    val appVal = ModuleUi.autoValue(this, "…")
                    val totalVal = ModuleUi.autoValue(this, "…")
                    patientsField = numericField("Today Patient")
                    ocCount = numericField("0")
                        .apply { val n = day.optInt("outside_calls_manual", 0); if (n > 0) setText(n.toString()) }
                    ocCountField = ocCount

                    form.addView(gridRow(gridCell("New Enquiry (auto)", enqVal), gridCell("Registration (auto)", regVal)))
                    form.addView(gridRow(gridCell("Today Patient", patientsField), gridCell("App Calls (auto)", appVal)))
                    // ✏️🔒 B521 (06.08.2026, TK-নির্দেশ) — লেবেল এখন সরাসরি
                    // ইংরেজিতে "Superfone / 200 Number Call" — সব স্টাফের
                    // জন্য একই (আগে বাংলা/হিন্দি আলাদা ছিল, এখন আর দরকার
                    // নেই যেহেতু ইংরেজিই থাকছে)।
                    val outsideCallsLabel = "Superfone/Clinic Number Call"
                    form.addView(gridRow(gridCell(outsideCallsLabel, ocCount), gridCell("Total call (auto)", totalVal)))

                    // 🔴🔒 B503 (06.08.2026, TK-নির্দেশ) — ফোনের নিজের
                    // স্থানীয় গণনা (আজ এই ফোনে কতবার কল-বোতাম চাপা
                    // হয়েছে) সাথে সাথেই দেখানো হয়, তারপর ক্লাউড থেকে
                    // মিলিয়ে/সংশোধন করে নেওয়া হয় (নিচের fetchStats-এ)।
                    /* 🔴🔒 V907 (৩১.০৮.২০২৬, JPE-CRP-এর রিপোর্ট, TK-নির্দেশ:
                       *"যাতে দুটো ফোনে একই দেখায় … এটাও আবার অরিজিনাল সংখ্যা"*)

                       এখানে আগে **এই ফোনে জমা গোনাটা** সঙ্গে সঙ্গে বসিয়ে দেওয়া
                       হত (B503), আর নিচে ক্লাউডের সঙ্গে **বড়টা** নেওয়া হত
                       (V590)। ফল: একই আইডি দুটো ফোনে খুললে দু'রকম সংখ্যা —
                       যে ফোন ক্লাউড পড়তে পারেনি সে শুধু নিজের গোনা দেখাত
                       (JPE-CRP-এর ছবিতে ৪ বনাম ১৫)।
                       এখন **একটাই সত্য — ক্লাউডের `wn.call_taps`**; পড়া না গেলে
                       সংখ্যা নয়, "…" থাকে (ভুল সংখ্যার চেয়ে সৎ)। ⇒ যত ফোনেই
                       খোলা হোক, সংখ্যা এক।
                       ⛔ কম্পিউটারের অ্যাপ (`notebook.js`) আগে থেকেই শুধু ক্লাউডই
                          পড়ে — এই বদলে তিন জায়গা এক নিয়মে এল।
                       ⛔ ফোনে-জমা গোনাটা মুছে ফেলা হয়নি; শুধু আর দেখানো হয় না। */

                    applyAutoOutsideCalls()

                    var appCallsNow = 0
                    fun refreshTotal() { totalVal.text = (appCallsNow + (ocCount.text.toString().toIntOrNull() ?: 0)).toString() }
                    ocCount.addTextChangedListener(object : android.text.TextWatcher {
                        override fun afterTextChanged(s: Editable?) { refreshTotal() }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })

                    // 🎨🔒 B520 (06.08.2026, TK-নির্দেশে সহজ মকআপ "A" দেখিয়ে
                    // অনুমোদনের পরে) — আলাদা লেবেল-লাইন তুলে দেওয়া হলো,
                    // এখন পুরো প্রশ্নটাই বক্সের ভেতরে হিন্ট-টেক্সট হিসেবে
                    // (টাইপ করা শুরু করলেই নিজে থেকে সরে যায় — EditText-এর
                    // স্বাভাবিক আচরণ, নতুন কিছু বানাতে হয়নি), হালকা
                    // বর্ডার-করা বক্সে। ⛔ ঐচ্ছিক (mandatory না) — আগের
                    // মতোই, ফাঁকা রেখে OUT TIME করা যায় (checkEmptyFieldsThenOut
                    // শুধু মনে করায়, আটকায় না)।
                    val notesHint = if (isKishanganjStaff) "What did you do today? (optional)"
                        else "আজকের সারাদিনে কি কি কাজ করেছেন লিখুন (ঐচ্ছিক)..."
                    notesField = ModuleUi.input(this, notesHint, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                        .apply { minLines = 3; gravity = android.view.Gravity.TOP; setText(ns(day, "day_note").ifBlank { workEntriesSummary() }) }
                    val notesBox = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        // 🔒 TK-FIX (07.08.2026): এই ফাংশনের scope-এ লোকাল dp()
                        // ছিল না (dp এখানে প্রতি-ফাংশনে আলাদা বানানো লোকাল ফাংশন),
                        // তাই "Unresolved reference: dp" বিল্ড এরর হচ্ছিল। ফাইলের
                        // চালু নিয়মেই এখানে d ও dp বানিয়ে দেওয়া হলো — আচরণ একই।
                        val d = resources.displayMetrics.density
                        fun dp(v: Int) = (v * d).toInt()
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#F7F9FB"))
                            setStroke(1, android.graphics.Color.parseColor("#E5E9ED"))
                            cornerRadius = dp(10).toFloat()
                        }
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.topMargin = dp(6)
                        layoutParams = lp
                        addView(notesField)
                    }
                    form.addView(notesBox)

                    fetchStats("day", todayIso()) { s ->
                        runOnUiThread {
                            // 🔴🔒 B496 (06.08.2026) — ব্যর্থ হলে "0" না দেখিয়ে
                            // "…" রাখা হয় (আগের মতোই), যাতে "ডেটা মুছে গেছে"
                            // বলে ভুল না বোঝা যায় — শুধু আবার চেষ্টা করা বাকি।
                            if (s.optBoolean("enqOk", true)) enqVal.text = s.optInt("enquiries").toString()
                            if (s.optBoolean("regOk", true)) regVal.text = s.optInt("registrations").toString()
                            // 🔴 TK-নির্দেশ (১৮.০৯.২০২৬) — নিজের রেজিস্টার/পেমেন্ট
                            // করা রোগীর সংখ্যা প্রি-ফিল; স্টাফ ইতিমধ্যে হাতে কিছু
                            // লিখে থাকলে সেটা মোছা হয় না (ফাঁকা থাকলেই বসে)।
                            if (s.optBoolean("myPatientsOk", true) && patientsField.text.toString().isBlank()) {
                                patientsField.setText(s.optInt("myPatients").toString())
                            }
                            /* 🟢🔒 V590 (TK-রিপোর্ট) — আগে এই দুটো লাইন **শর্ত ছাড়াই**
                               বসত, তাই ক্লাউড-পড়া ব্যর্থ হলে ০ এসে ফোনে জমা থাকা
                               আসল গোনাটা মুছে দিত (TK-এর ছবিতে App Calls ০, অথচ
                               কল হয়েছিল)।
                               এখন দুটো জাল:
                                 ১) পড়া ব্যর্থ হলে (`appOk` false) সংখ্যাটা **ছোঁয়াই
                                    হয় না** — ফোনের গোনাটাই থেকে যায়;
                                 ২) পড়া সফল হলেও **ফোনের গোনার চেয়ে বড়টাই** নেওয়া
                                    হয় — কারণ এইমাত্র করা কলটা ক্লাউডে পৌঁছাতে
                                    কয়েক সেকেন্ড লাগতে পারে, তখন ক্লাউড কম বলত।
                               ⛔ দুটোই মিলিয়ে: গোনা **কখনো কমে যায় না**। */
                            /* 🔴🔒 V907 — শুধু ক্লাউডের সংখ্যাই (উপরের টীকা)। */
                            val cloudCalls = s.optInt("appCalls")
                            if (callsOk(s)) appCallsNow = cloudCalls
                            /* 🔴 V593 — আগে এই লাইনটা **শর্ত ছাড়াই** বসত, তাই
                               পড়া ব্যর্থ হলে "…"-এর জায়গায় সাফ **0** লেখা হয়ে
                               যেত (অথচ পাশের New Enquiry তখন "…" দেখাত)।
                               এখন: পড়া সফল হলে, বা এই ফোনেই কল চাপা থাকলে
                               তবেই সংখ্যা — নইলে "…" আগের মতোই থাকে। */
                            if (callsOk(s)) {
                                appVal.text = appCallsNow.toString()
                                refreshTotal()
                            }
                        }
                    }
                }
                else -> {
                    // 🔵 B615 (11.08.2026, TK-নির্দেশ): ডিউটি সকাল ৯টা–সন্ধ্যা ৬টা।
                    // দুপুর ১২টার আগে পর্যন্ত IN TIME বোতাম থাকে (৩ ঘণ্টা দেরির ছাড়সহ,
                    // আর সকালে-আসা কেউ যেন আটকে না যায় বলে ১২টার আগে সবসময় খোলা)।
                    // ১২টা পার হলে IN TIME লুকিয়ে যায় — শুধু ছোট নোটিশ ও Mark As Leave
                    // থাকে (রাত/বিকেলে ভুল করে IN দেখানো বন্ধ)। ⛔ IN TIME চাপার
                    // সেভ-লজিক এক অক্ষরও বদলায়নি।
                    /* 📌🔒 V1200 (TK-নির্দেশ ও ফটো-প্রুফ পাশ) — আগে থেকে ঠিক করা
                       থাকলে সেদিনের প্ল্যানটা **IN TIME বোতামের ঠিক উপরে**।
                       ⛔ প্ল্যান না থাকলে কিছুই বসে না (পর্দা হুবহু আগের মতো)। */
                    todayPlanCard()?.let { form.addView(it) }
                    if (inTimeWindowOpen()) {
                        // 🏍️ V977 — TK-নির্দেশে বাছাইয়ের সারিটা আর দেখানো হয় না
                        //    (IN TIME চাপলেই নিজে থেকে চালু)। ⛔ ফাংশনটা মোছা
                        //    হয়নি (প্রজেক্ট-নিয়ম), শুধু আর ডাকা হয় না।
                        val inBtn = ModuleUi.button(this, "IN TIME") {
                            // 🔒 V496: একই নতুন পথ (উপরের startInTimeFlow দেখুন)।
                            startInTimeFlow { afterInTimeMarked { render() } }
                        }
                        inBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                            .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
                        form.addView(inBtn)
                    } else {
                        form.addView(ModuleUi.body(this,
                            // 🔴 V507: এখন এই লেখা শুধু **সন্ধ্যা ৬টার পরে** দেখা যায়।
                            if (isKishanganjStaff) "⏰ IN TIME window is over for today. Mark leave if absent."
                            else "⏰ " + NoBengali.s("আজকের IN TIME-এর সময় শেষ, না এলে ছুটি দিন")))
                    }
                    // 🔴🔒 B506 (06.08.2026, TK-নির্দেশ) — বোতামের বাংলা এখন
                    // ঠিক TK-এর নিজের লেখা অনুযায়ী "আজকে আমার ছুটি" (আগে
                    // "আজকে কি আপনার ছুটি" ছিল)। নিচে আলাদা কোনো হিন্ট
                    // লাইন নেই (আগেই B466-এ বাদ দেওয়া হয়েছিল, বহাল আছে)।
                    val markLeaveBtn = ModuleUi.button(this, if (isKishanganjStaff) "🏖️ Mark As Leave" else "🏖️ Mark As Leave / আজকে আমার ছুটি") { applyLeaveFlow() }
                    markLeaveBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
                    form.addView(markLeaveBtn)
                    /* 🏖️🔒 V740 (২৭.০৮.২০২৬, TK-নির্দেশ) — **নিজের ছুটি নিজে বাতিল**।
                       TK-এর সিদ্ধান্ত "১": *"নিজেই বাতিল করতে পারবে — তাহলে ওই দিনটা
                       অন্য সহকর্মীর জন্য খালি হয়ে যাবে, আর তার ৪ দিনের হিসাবেও ফেরত আসবে।"*
                       ⛔ এতদিন পাশের "Cancel Leave" শুধু **আজকের** ছুটি ফেরাত;
                          আগাম নেওয়া ছুটি ফেরানোর কোনো পথ ছিল না।
                       ⛔ পুরনো "Cancel Leave" বোতাম এক অক্ষরও বদলায়নি। */
                    val myLeaveBtn = ModuleUi.buttonSoft(this, "My Upcoming Leave") { upcomingLeaveScreen() }
                    myLeaveBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 6) }
                    form.addView(myLeaveBtn)
                }
            }
        }

        // ছোট, de-emphasized লিংক — সব ধাপেই দেখা যাবে (Locked দিনেও, যাতে
        // পুরনো রিপোর্ট দেখতে পারেন)
        val linksRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 12) }
        }
        fun smallLink(text: String, onClick: () -> Unit): TextView = TextView(this).apply {
            this.text = text; textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#1E6EDC"))
            setPadding(0, ModuleUi.dp(this@WorkNotebookActivity, 4), ModuleUi.dp(this@WorkNotebookActivity, 16), ModuleUi.dp(this@WorkNotebookActivity, 4))
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        /* ⋮ V1204 — দুটো লিংকই এখন উপরের ⋮ মেনুতে (TK-নির্দেশ)। `smallLink`
           ও `linksRow` মোছা হয়নি (TK-নিয়ম: নিজে থেকে কোড মোছা হয় না)। */
    }

    // 🔴 B342 — Daily-র জন্য এখন একটাই বোতাম: (১) Outside Calls/Notes সেভ,
    // (২) তাজা stats এনে ঠিক TK-এর নমুনার ফরম্যাটে টেক্সট বানানো (report()-এর
    // "daily" ব্রাঞ্চের হুবহু একই ফরম্যাট, শুধু Notes এখন `day_note` থেকে,
    // Work Entries লিস্ট থেকে না), (৩) Submit to Master (আগের মতোই
    // `wn.work_reports`-এ), (৪) WhatsApp/Share শিট খোলা — সবটা একবারেই।
    private fun submitDailyReport(patientsField: EditText, ocCount: EditText, notesField: EditText) {
        day.put("outside_calls_manual", ocCount.text.toString().toIntOrNull() ?: 0)
        day.put("day_note", notesField.text.toString())
        ModuleUi.toast(this, "Submitting...")
        saveDay {
            fetchStats("day", todayIso()) { s ->
                runOnUiThread {
                    val text = StringBuilder()
                    text.append("Daily Report ").append(dotDate(todayIso())).append(" Time-").append(shareTimeLabel())
                        .append("\nStaff: $staffCode\n")
                    if (day.optBoolean("is_leave", false)) {
                        text.append("🏖️ On Leave: ").append(ns(day, "leave_reason")).append("\n")
                    } else {
                        text.append("IN TIME- ").append(displayTime12(ns(day, "check_in")).ifBlank { "-" }).append("\n")
                        text.append("OUT TIME ").append(displayTime12(ns(day, "check_out")).ifBlank { "-" }).append("\n")
                    }
                    text.append("\nNew Enquiry: ").append(statTxt(s, "enquiries", "enqOk"))
                        .append("\nRegistration: ").append(statTxt(s, "registrations", "regOk"))
                        .append("\nToday Patient: ").append(patientsField.text.toString().trim().ifBlank { "0" })
                        .append("\nApp Calls: ").append(callTxt(s, "appCalls"))
                        .append("\nOutside Calls: ").append(callTxt(s, "outsideCalls"))
                        .append("\nTotal call : ").append(callTxt(s, "totalCalls"))
                        .append(docVisitLine())
                    val notesTxt = notesField.text.toString().trim()
                    if (notesTxt.isNotBlank()) text.append("\n\nNotes: \n").append(notesTxt)
                    val finalText = text.toString()
                    submit("daily", todayIso(), s, finalText)
                    shareText(finalText)
                }
            }
        }
    }

    private fun drawEntries(list: LinearLayout) {
        list.removeAllViews()
        val arr = day.optJSONArray("manual_entries") ?: JSONArray()
        if (arr.length() == 0) { list.addView(ModuleUi.body(this, "No entries yet.")); return }
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            val rowL = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            rowL.addView(ModuleUi.body(this, ns(e, "time") + "  " + ns(e, "text") + "  "))
            rowL.addView(ModuleUi.button(this, if (ns(e, "status") == "complete") "✅" else "⏳") {
                e.put("status", if (ns(e, "status") == "complete") "pending" else "complete")
                day.put("manual_entries", arr); saveDay { drawEntries(list) }
            })
            list.addView(rowL)
        }
    }

    // ---- automatic stats (read-only from existing public tables) ----
    // 🔴 B332 (03.08.2026, TK-অনুমোদিত — নিজে ধরা ঝুঁকি ঠিক করা): আগে সব
    // loadStats() কল একই ক্লাস-লেভেল `lastStats` ভেরিয়েবলে লিখত — মূল স্ক্রিন
    // (render()) আর Daily/Monthly Report (report()) দুটোই একসাথে খোলা থাকলে
    // (বা report() দ্রুত পরপর দুবার খোলা হলে) একটার ডেটা আরেকটাকে ওভাররাইট
    // করে ভুল সংখ্যা দেখাতে পারত। এখন `fetchStats()` — শুধু ডেটা আনে, প্রতিটা
    // কলার নিজের callback-এ নিজের কপি পায়, কোনো শেয়ার্ড ভেরিয়েবল নেই। পুরনো
    // `loadStats()` এখনো আছে (মূল স্ক্রিনের UI-আপডেটের জন্য), কিন্তু এখন ভিতরে
    // `fetchStats()`-ই ব্যবহার করে — গণনার লজিক এক অক্ষরও বদলায়নি, শুধু ডেটা
    // কোথায় জমা থাকে সেটা বদলেছে।
    /**
     * 🔴🔴🔴 V509 (২১.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ) — *"Monthly রিপোর্টে অ্যাপসের
     * কল শূন্য দেখাচ্ছে কেন? আমি তো আগস্ট মাসে অনেক কল করেছি।"*
     *
     * ─── আসল কারণ (প্রমাণ করে দেখা হয়েছে, অনুমান নয়) ──────────────────────
     * মাসের শেষ সীমা হিসেবে কোড লিখত **"2026-08-32"** — মাসের ৩২ তারিখ, যা
     * পৃথিবীতে নেই। এতদিন ধরা পড়েনি, কারণ Enquiry/Registration যে ঘরগুলো দেখে
     * (`createdAt`) সেগুলো **লেখার ঘর (text)** — সেখানে "৩২" নিছক একটা অক্ষর,
     * তুলনাটা কাজ করে যায়। কিন্তু কলের হিসাব আসে `wn.call_taps.call_date`
     * থেকে, আর সেটা **সত্যিকারের তারিখের ঘর (date)** — সেখানে ৩২ তারিখ দিলে
     * ডেটাবেস সরাসরি ভুল ধরিয়ে দেয়:
     *     ERROR: date/time field value out of range: "2026-08-32"
     * অনুরোধটাই ব্যর্থ হয়, ফাঁকা ফল আসে, আর পর্দায় বসে **0**।
     *
     * ⇒ তাই ঠিক তিনটে ঘরই ০ দেখাত — **App Calls · Outside Calls · Leave Days**
     *   (তিনটেই `date` ঘরে খোঁজে), অথচ New Enquiry ২০ ও Registration ৭ ঠিকই
     *   আসত। TK-এর ছবিতে হুবহু এটাই দেখা যাচ্ছে।
     *
     * ⇒ **সমাধান:** ৩২ তারিখের বদলে **পরের মাসের ১ তারিখ** (ডিসেম্বর হলে পরের
     *   বছরের ১ জানুয়ারি)। এটা সবসময় সত্যিকারের তারিখ, আর মাসের শেষ দিনটাও
     *   পুরোপুরি ধরা পড়ে।
     *
     * ⛔ যেগুলো এতদিন **ঠিকঠাক কাজ করত** (Enquiry · Registration · Collection —
     *    সবই `text` ঘর) তাদের ছাঁকনি **এক অক্ষরও ছোঁয়া হয়নি**। শুধু যে তিনটে
     *    সবসময় ০ দেখাত, সেগুলোই এখন আসল সংখ্যা দেখাবে।
     */
    private fun monthEndExclusive(key: String): String {
        return try {
            val y = key.substring(0, 4).toInt()
            val m = key.substring(5, 7).toInt()
            if (m >= 12) String.format(Locale.US, "%04d-01-01", y + 1)
            else String.format(Locale.US, "%04d-%02d-01", y, m + 1)
        } catch (_: Throwable) {
            "$key-32"   // চেনা না গেলে আগের আচরণেই — নতুন কিছু ভাঙে না
        }
    }

    private fun fetchStats(mode: String, key: String, callback: (JSONObject) -> Unit) {
        Thread {
            // 🔒 V509: `text` ঘরের এই ছাঁকনিটা আগের মতোই — এটা কাজ করত, তাই ছোঁয়া হয়নি।
            val dateFilter = if (mode == "day") "createdAt=gte.$key" else "createdAt=gte.$key-01&createdAt=lt.$key-32"
            // 🔴 V509: `date` ঘরের জন্য সত্যিকারের সীমা (পরের মাসের ১ তারিখ)।
            val monthEnd = monthEndExclusive(key)
            /* 🚨🔒 V1188 (০৭.০৯.২০২৬, TK-রিপোর্ট: *"KISHAN-10 & 11 এরা কিছু
               রেজিষ্ট্রেশন করেছে … কিন্তু তাদের টা কেন 0"* এবং TK-এর সঠিক আপত্তি
               *"আপনি আবার নেট দুর্বল বলছেন, আমার প্রতিটা চেম্বারে হাই স্পিডের
               ইন্টারনেট লাগানো"*)।

               🔴 **প্রথমে প্রমাণ (আন্দাজ নয়):** TK-র পাঠানো রিপোর্টে **App Calls: 0**
                  লেখা ছিল — "…" নয়। ওই ঘরটা পড়া ব্যর্থ হলে "…" দেখায় (V590)।
                  ⇒ **পড়াগুলো সফলই হয়েছিল**, নেটের কোনো দোষ ছিল না। সার্ভার সত্যিই
                  ০ ফেরত দিয়েছে — অর্থাৎ **ছাঁকনিটাই মিলছিল না**। আমার আগের
                  "নেট দুর্বল" কথাটা ভুল ছিল।

               🔴 **দুটো অমিল কোডে মেপে বের করা হলো — কম্পিউটারের নিজের নিয়মের
                  সঙ্গে মিলিয়ে (`notebook.js`-এর `autoStats`):**
                  ① **নম্বর মেলানো** — কম্পিউটার `mobEq()` দিয়ে **শেষ ১০ অঙ্ক** মেলায়,
                     তাই "+919883605917" আর "9883605917" একই ধরা হয়। ফোনে ছিল হুবহু
                     `eq.` — সারিটা অন্য ধাঁচে সেভ হলে (কম্পিউটার থেকে করা
                     রেজিস্ট্রেশন, পুরনো সারি) কখনোই মিলত না ⇒ ০।
                  ② **তারিখের ঘর** — কম্পিউটার `createdAt` **অথবা** `date` **অথবা**
                     `registrationDate` — তিনটের যেকোনোটা দেখে। ফোনে ছিল শুধু
                     `createdAt`। পুরনো সারি আবার সেভ হলে `createdAt`-এ **আসল
                     পুরনো সময়ই** থেকে যায় (V868-এর নিয়ম), তাই আজ কাজ করেও আজকের
                     গোনায় ঢুকত না ⇒ ০।

               ⇒ এখন ফোনেও ঠিক কম্পিউটারের নিয়ম: শেষ ১০ অঙ্ক ধরে মেলানো, আর
                 তিনটে তারিখ-ঘরের যেকোনোটা। ⛔ কাকে গোনা হবে (নিজের করা কাজ) —
                 সেই নিয়ম এক অক্ষরও বদলায়নি, শুধু মেলানোর ধরনটা উদার হলো।
               ⛔ নতুন ছাঁকনি কোনো কারণে সার্ভার না বুঝলে **আগের হুবহু ছাঁকনিতেই**
                 আবার চেষ্টা হয় (নিচে `countBoth`) — তাই আগের চেয়ে খারাপ কিছু
                 কখনো হতে পারে না। */
            val me10 = mobile.filter { it.isDigit() }.takeLast(10)
            val dayOr = if (mode == "day")
                "or(createdAt.gte.$key,date.eq.$key,registrationDate.eq.$key)"
            else "or(createdAt.gte.$key-01,date.gte.$key-01,registrationDate.gte.$key-01)"
            val mineEnqNew = "and=(or(createdBy.like.*$me10,receivedBy.like.*$me10),$dayOr)"
            val minePatNew = "and=(or(registeredBy.like.*$me10,createdBy.like.*$me10),$dayOr)"
            val minePayNew = "and=(or(receivedBy.like.*$me10,createdBy.like.*$me10),$dayOr)"
            val mineEnq = "or=(createdBy.eq.$mobile,receivedBy.eq.$mobile)&$dateFilter"
            val minePat = "or=(registeredBy.eq.$mobile,createdBy.eq.$mobile)&$dateFilter"
            val minePay = "or=(receivedBy.eq.$mobile,createdBy.eq.$mobile)&$dateFilter"
            // 🔴🔒 B496 (06.08.2026) — এখন `countPublicChecked()` ব্যবহার,
            // নেটওয়ার্ক ব্যর্থ হলে সংখ্যাটা "জিরো" না দেখিয়ে "…" দেখানো
            // হয় (নিচে loadStats()-এ), যাতে আসল ডেটা মুছে গেছে বলে ভুল
            // ধারণা না হয়।
            /* 🛡 V1188 — নতুন (উদার) ছাঁকনি আগে; সার্ভার না বুঝলে পুরনোটা।
               ⛔ তাই সবচেয়ে খারাপ ক্ষেত্রেও আগের সংখ্যাটাই আসে, কম নয়। */
            fun countBoth(table: String, wide: String, old: String): ModuleAuth.CountResult {
                val r = ModuleAuth.countPublicChecked(table, wide)
                return if (r.ok) r else ModuleAuth.countPublicChecked(table, old)
            }
            val enqR = countBoth("enquiries", mineEnqNew, mineEnq)
            val regR = countBoth("patients", minePatNew, minePat)
            val enq = enqR.count
            val reg = regR.count
            /* 🔴 TK-নির্দেশ (১৮.০৯.২০২৬) — "Today Patient" মানে যে স্টাফ
               পেমেন্ট নেবে অথবা যে স্টাফ রেজিস্ট্রেশন করবে, শুধু তার নিজের
               একাউন্টেই পেশেন্টটা যোগ হবে — চেম্বারে সেদিন মোট কতজন এসেছে
               (Chamber Attendance-এর ব্রাঞ্চ-মোট) তা নয়। আগে ওই ব্রাঞ্চ-মোট
               ফিল্ড স্টাফের রিপোর্টেও (যেমন Arman) সাজেস্ট হয়ে যেত। এখন:
               এই স্টাফ নিজে registeredBy/createdBy (patients) অথবা
               receivedBy/createdBy (payments) — এই দুই তালিকার রোগী
               (patientId ধরে) ডুপ্লিকেট বাদ দিয়ে গোনা হয়। পড়া ব্যর্থ হলে
               "patientsOk=false" — ভুল সংখ্যা না দেখিয়ে "…" দেখানো হবে। */
            fun idsBoth(table: String, col: String, wide: String, old: String): Pair<Boolean, Set<String>> {
                val w = ModuleAuth.getRowsChecked("public", table, "select=$col&$wide")
                val r = if (w.ok) w else ModuleAuth.getRowsChecked("public", table, "select=$col&$old")
                if (!r.ok) return false to emptySet()
                val ids = mutableSetOf<String>()
                for (i in 0 until r.rows.length()) {
                    val v = r.rows.getJSONObject(i).optString(col, "")
                    if (v.isNotBlank()) ids.add(v)
                }
                return true to ids
            }
            val myPatientIds = mutableSetOf<String>()
            val (regIdsOk, regIds) = idsBoth("patients", "id", minePatNew, minePat)
            val (payIdsOk, payIds) = idsBoth("payments", "patientId", minePayNew, minePay)
            if (regIdsOk) myPatientIds.addAll(regIds)
            if (payIdsOk) myPatientIds.addAll(payIds)
            val myPatientsOk = regIdsOk && payIdsOk
            val myPatients = myPatientIds.size
            // AUDIT FIX (2026-08-06): use the checked sum so a network failure
            // shows "…" instead of a misleading ₹0 (see loadStats/report below).
            val collR = ModuleAuth.sumPublicChecked("payments", minePayNew, "amount").let {
                if (it.ok) it else ModuleAuth.sumPublicChecked("payments", minePay, "amount")
            }
            val coll = collR.sum
            /* 🟢🔒 V590 (২৩.০৮.২০২৬, TK-রিপোর্ট, ছবিসহ: *"App থেকে অনেকগুলো কল
               আমার সামনেই করলো, কিন্তু এখন সব 0 কেন দেখাচ্ছে?"*)

               **আসল কারণ (কোড থেকে প্রমাণিত):** এখানে `getRows()` ব্যবহার হত।
               ওই ফাংশনটা **ব্যর্থ হলেও খালি তালিকাই** ফেরত দেয় (নেট নেই · সাইন-ইন
               হয়নি · HTTP এরর — সবেতেই `JSONArray()`)। অর্থাৎ "সত্যিই ০টা কল" আর
               "পড়াই যায়নি" — দুটো আলাদা করা যেত না, আর নিচে ওই ০ সংখ্যাটা
               **ফোনে জমা থাকা আসল গোনাটা মুছে দিত**।
               ⇒ এখন `getRowsChecked()` — ঠিক যেটা B316-এ Staff Profile-এর জন্য
                 বানানো হয়েছিল, আর যেটা Enquiry/Registration/Collection আগে
                 থেকেই ব্যবহার করে (`enqOk`/`regOk`/`collOk`)। App Calls-এর
                 জন্যও এখন সেই একই পাহারা: `appOk`।
               ⛔ পড়া সফল হলে সংখ্যা হুবহু আগের মতোই। */
            val appR = if (mode == "day")
                ModuleAuth.getRowsChecked("wn", "call_taps", "select=id&staff_code=eq.${callTapCode()}&call_date=eq.$key")
            else ModuleAuth.getRowsChecked("wn", "call_taps", "select=id&staff_code=eq.${callTapCode()}&call_date=gte.$key-01&call_date=lt.$monthEnd")
            val appCalls = appR.rows.length()
            // 🔴 B330 (03.08.2026, TK-নির্দেশ — Outside Calls এখন শুধু একটা
            // সংখ্যা, প্রতিটা কল আলাদা লগ না): দিন-হিসাবে সরাসরি `day`
            // অবজেক্ট থেকে পড়া হয় (নতুন নেটওয়ার্ক-কল লাগে না)। মাস-হিসাবে
            // `wn.notebook_days`-এর `outside_calls_manual` কলাম যোগ করে বার
            // করা হয়।
            val outCalls = if (mode == "day") day.optInt("outside_calls_manual", 0)
            else {
                val rows = ModuleAuth.getRows("wn", "notebook_days", "select=outside_calls_manual&staff_code=eq.$staffCode&work_date=gte.$key-01&work_date=lt.$monthEnd")
                var sum = 0
                for (i in 0 until rows.length()) sum += rows.getJSONObject(i).optInt("outside_calls_manual", 0)
                sum
            }
            // 🔴 B324 (03.08.2026, TK-নির্দেশ — "পরে না, এখনই ফাইনাল করে কাজ
            // করুন"): Monthly-তে ছুটির দিন গোনা। wn.notebook_days-এ is_leave=true
            // যতগুলো সারি এই মাসে আছে, তাই — Daily-তে এই কল লাগে না (day
            // অবজেক্টেই is_leave আগে থেকে আছে)।
            val leaveDays = if (mode == "month")
                ModuleAuth.getRows("wn", "notebook_days", "select=id&staff_code=eq.$staffCode&work_date=gte.$key-01&work_date=lt.$monthEnd&is_leave=eq.true").length()
            else 0
            val stats = JSONObject().put("enquiries", enq).put("registrations", reg).put("collection", coll)
                .put("enqOk", enqR.ok).put("regOk", regR.ok).put("collOk", collR.ok)
                .put("appOk", appR.ok)   // 🟢 V590
                .put("appCalls", appCalls).put("outsideCalls", outCalls).put("totalCalls", appCalls + outCalls)
                .put("leaveDays", leaveDays)
                .put("myPatients", myPatients).put("myPatientsOk", myPatientsOk)
            callback(stats)
        }.start()
    }

    private fun loadStats(host: LinearLayout, mode: String, key: String) {
        fetchStats(mode, key) { stats ->
            runOnUiThread {
                host.removeAllViews()
                host.addView(ModuleUi.heading(this, "Auto from App records"))
                // AUDIT FIX (2026-08-06): if a value could NOT be loaded (weak
                // network), show "…" instead of a misleading 0 / ₹0, so a real
                // working day is never made to look empty. Successful loads look
                // exactly as before.
                val enqTxt = if (stats.optBoolean("enqOk", true)) stats.optInt("enquiries").toString() else "…"
                val regTxt = if (stats.optBoolean("regOk", true)) stats.optInt("registrations").toString() else "…"
                val collTxt = if (stats.optBoolean("collOk", true)) money(stats.optDouble("collection", 0.0)) else "…"
                // 🔴 V593 — কলের তিনটে সংখ্যাও এখন এই তালিকায়; আগে ব্যর্থ
                //    পড়াতেও এরা "0" ছাপত, আর `anyFailed`-এও গোনা হত না, তাই
                //    নিচের "weak internet" সতর্কবার্তাটাও উঠত না।
                val appTxt = callTxt(stats, "appCalls")
                val outTxt = callTxt(stats, "outsideCalls")
                val totTxt = callTxt(stats, "totalCalls")
                val anyFailed = !stats.optBoolean("enqOk", true) || !stats.optBoolean("regOk", true) || !stats.optBoolean("collOk", true) || !callsOk(stats)
                host.addView(ModuleUi.body(this,
                    "New Enquiry: $enqTxt\nRegistration: $regTxt\nCollection recorded: $collTxt" +
                    "\nApp Calls: $appTxt | Outside Calls: $outTxt | Total Calls: $totTxt" +
                    (if (mode == "month") "\nLeave Days: ${stats.optInt("leaveDays")}" else "")))
                if (anyFailed) host.addView(ModuleUi.body(this, "…  = could not load right now (weak internet). Your data is safe — open again when online."))
                host.addView(ModuleUi.body(this, "App Calls = in-app Call buttons you pressed. Never means connected; no duration."))
            }
        }
    }

    private fun report(type: String, key: String) {
        backAction = { render() }
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val col = ModuleUi.screen(this, "")
        col.addView(hero(if (type == "daily") "📄 Daily Report" else "📊 Monthly Report", "$key · $staffCode"))
        val out = ModuleUi.card(this); col.addView(out); out.addView(ModuleUi.body(this, "Building..."))
        // 🔴 B332 — report()-এর নিজের স্বাধীন stats snapshot (fetchStats()-এর
        // callback-এ), মূল স্ক্রিনের সাথে কোনো শেয়ার্ড ভেরিয়েবল/race-condition
        // নেই, আর আগের `Thread.sleep(600)`-নির্ভর আন্দাজ-টাইমিংও বাদ গেল।
        fetchStats(if (type == "daily") "day" else "month", key) { s ->
            // 🔴 B331 (03.08.2026, TK-নির্দেশ — "কতজন পেশেন্ট এসেছিল" আগে থেকে
            // সাজেস্ট হবে, staff চাইলে বদলাবে) + TK-নির্দেশ (১৮.০৯.২০২৬,
            // চেম্বার-মোট নয়, শুধু এই স্টাফের নিজের registeredBy/receivedBy
            // রোগী) — fetchStats()-এর একই ব্যাকগ্রাউন্ড কলেই এই সংখ্যা আসে,
            // আলাদা network round-trip লাগে না। পড়া ব্যর্থ হলে ফাঁকা, staff
            // নিজে লিখবেন।
            var suggestedPatients = ""
            if (type == "daily" && s.optBoolean("myPatientsOk", true)) suggestedPatients = s.optInt("myPatients").toString()
            runOnUiThread {
                out.removeAllViews()
                // AUDIT FIX (2026-08-06): when a count could NOT be loaded (weak
                // network) show "…" instead of a misleading 0, both on screen and
                // in the shared report. A successful load is byte-identical to before.
                val enqTxt = if (s.optBoolean("enqOk", true)) s.optInt("enquiries").toString() else "…"
                val regTxt = if (s.optBoolean("regOk", true)) s.optInt("registrations").toString() else "…"
                val patientsField = if (type == "daily") numericField("Today Patient")
                    .apply { if (suggestedPatients.isNotBlank()) setText(suggestedPatients) } else null

                // 🔴 B331 — TK-এর নমুনার হুবহু ক্রম/ফরম্যাট। Submit/Share দুটোই
                // চাপার মুহূর্তে টেক্সট তৈরি হয় (patientsField-এর তখনকার মান
                // দিয়ে), তাই staff বদলালে সাথে সাথেই তাতেই পাঠানো/সাবমিট হয়।
                // 🔴 B333 (03.08.2026, TK-নির্দেশ — "প্রত্যেকের ক্ষেত্রেই এটা
                // করুন, Today Patient"): এই লাইনটা সব স্টাফের জন্যই ইংরেজিতে
                // "Today Patient" — আগে বাংলা ছিল, কিশানগঞ্জ (KNE-KISHAN5,
                // বাংলা পড়তে পারেন না) স্টাফের পর্দাতেও যেত বলে TK নির্দেশ
                // দিয়ে সবার জন্যই ইংরেজি করে দিয়েছেন।
                fun buildFinalText(): String {
                    // 🔴 B340 (03.08.2026, TK-নির্দেশ — গ্রুপে রিপোর্ট শেয়ার হয়,
                    // তাই Collection-এর টাকা অন্য কাউকে দেখাতে চান না): "Collection"
                    // লাইন Daily ও Monthly — দুটো শেয়ার-টেক্সট থেকেই বাদ। হোম-স্ক্রিনের
                    // নিজস্ব "Auto from App records" কার্ডে (শুধু স্টাফ নিজে দেখেন,
                    // WhatsApp-এ যায় না) অপরিবর্তিত আছে।
                    val text = StringBuilder()
                    if (type == "daily") {
                        text.append("Daily Report ").append(dotDate(key)).append(" Time-").append(shareTimeLabel())
                            .append("\nStaff: $staffCode\n")
                        if (day.optBoolean("is_leave", false)) {
                            text.append("🏖️ On Leave: ").append(ns(day, "leave_reason")).append("\n")
                        } else {
                            text.append("IN TIME- ").append(displayTime12(ns(day, "check_in")).ifBlank { "-" }).append("\n")
                            text.append("OUT TIME ").append(displayTime12(ns(day, "check_out")).ifBlank { "-" }).append("\n")
                        }
                        text.append("\nNew Enquiry: ").append(enqTxt)
                            .append("\nRegistration: ").append(regTxt)
                            .append("\nToday Patient: ").append(patientsField?.text?.toString()?.trim()?.ifBlank { "0" } ?: "0")
                            .append("\nApp Calls: ").append(callTxt(s, "appCalls"))
                            .append("\nOutside Calls: ").append(callTxt(s, "outsideCalls"))
                            .append("\nTotal call : ").append(callTxt(s, "totalCalls"))
                        val notesTxt = workEntriesSummary()
                        if (notesTxt.isNotBlank()) text.append("\n\nNotes: \n").append(notesTxt)
                    } else {
                        text.append("Monthly Report $key").append("\nStaff: $staffCode\n\n")
                            .append("New Enquiry: ").append(enqTxt).append("\nRegistration: ").append(regTxt)
                            .append("\nApp Calls: ").append(callTxt(s, "appCalls")).append(" | Outside Calls: ").append(callTxt(s, "outsideCalls"))
                            .append(" | Total: ").append(callTxt(s, "totalCalls"))
                            .append("\nLeave Days: ").append(s.optInt("leaveDays"))
                    }
                    return text.toString()
                }

                // 🎨🔒 B525 (06.08.2026, TK-নির্দেশে গ্রিড-কার্ড মকআপ লক করার
                // পরে) — স্ক্রিনে দেখানোর ধরন এখন গ্রিড-বাক্সে (WorkNotebook
                // মূল পাতার B508-এর একই ধাঁচ) — Submit/Share-এ যে টেক্সট
                // যায় (`buildFinalText()`) এক অক্ষরও বদলায়নি, শুধু স্ক্রিনে
                // দেখানোর ধরন।
                val reportPairs = if (type == "daily") listOf(
                    "New Enquiry" to enqTxt,
                    "Registration" to regTxt,
                    // 🔴 V593 — এখানেও ব্যর্থ পড়ায় "0" নয়, সৎ "…"
                    "App Calls" to callTxt(s, "appCalls"),
                    "Outside Calls" to callTxt(s, "outsideCalls"),
                    "Total Calls" to callTxt(s, "totalCalls"),
                    "IN / OUT" to (displayTime12(ns(day, "check_in")).ifBlank { "-" } + " / " + displayTime12(ns(day, "check_out")).ifBlank { "-" })
                ) else listOf(
                    "New Enquiry" to enqTxt,
                    "Registration" to regTxt,
                    // 🔴 V593 — এখানেও ব্যর্থ পড়ায় "0" নয়, সৎ "…"
                    "App Calls" to callTxt(s, "appCalls"),
                    "Outside Calls" to callTxt(s, "outsideCalls"),
                    "Total Calls" to callTxt(s, "totalCalls"),
                    "Leave Days" to s.optInt("leaveDays").toString()
                )
                val reportCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.WHITE); cornerRadius = dp(12).toFloat()
                    }
                    elevation = 2f
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.bottomMargin = dp(8)
                    layoutParams = lp
                }
                fun reportCell(label: String, value: String): LinearLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(8), dp(6), dp(8), dp(6))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.parseColor("#F7F9FB"))
                        setStroke(1, android.graphics.Color.parseColor("#E5E9ED"))
                        cornerRadius = dp(8).toFloat()
                    }
                    addView(TextView(this@WorkNotebookActivity).apply { text = label; textSize = 9.5f; setTextColor(android.graphics.Color.parseColor("#6B7280")) })
                    addView(TextView(this@WorkNotebookActivity).apply {
                        text = value; textSize = 15f
                        brightIfNonZero(this)   // 🔆 V1204 — ০ হলে ফিকে, ০-র বেশি হলে গাঢ়
                    })
                }
                var idx = 0
                while (idx < reportPairs.size) {
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.topMargin = dp(6); layoutParams = lp
                    }
                    val a = reportCell(reportPairs[idx].first, reportPairs[idx].second)
                    a.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(4) }
                    row.addView(a)
                    if (idx + 1 < reportPairs.size) {
                        val b = reportCell(reportPairs[idx + 1].first, reportPairs[idx + 1].second)
                        b.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) }
                        row.addView(b)
                    }
                    reportCard.addView(row)
                    idx += 2
                }
                out.addView(reportCard)
                if (patientsField != null) {
                    out.addView(ModuleUi.label(this, "Today Patient (tap to change)"))
                    out.addView(spacedField(patientsField))
                }
                val btnRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = ModuleUi.dp(this@WorkNotebookActivity, 8) }
                }
                // ✏️🔒 B524 (06.08.2026, TK-নির্দেশ) — বোতামের লেখা "Master"
                // থেকে বদলে সরাসরি "TK BISWAS" — টেবিল/সেভ-লজিক অপরিবর্তিত
                // (এখনো `wn.work_reports`-এ যায়, শুধু বোতামের লেখা বদলেছে)।
                val submitBtn = ModuleUi.button(this, "Submit to TK BISWAS") { submit(type, key, s, buildFinalText()) }
                submitBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginEnd = ModuleUi.dp(this@WorkNotebookActivity, 6) }
                val shareBtn = ModuleUi.button(this, "📤 Share") { shareText(buildFinalText()) }
                shareBtn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = ModuleUi.dp(this@WorkNotebookActivity, 6) }
                btnRow.addView(submitBtn); btnRow.addView(shareBtn)
                out.addView(btnRow)
                bottomBack(col) { render() }   // ⬇️ V1204
            }
        }
    }

    private fun submit(type: String, key: String, stats: JSONObject, summary: String) {
        Thread {
            val existing = ModuleAuth.getRows("wn", "work_reports",
                "select=id,version,status&staff_code=eq.$staffCode&period_type=eq.$type&period_key=eq.$key&order=version.desc&limit=1")
            val prevVer = if (existing.length() > 0) existing.getJSONObject(0).optInt("version", 0) else 0
            val row = JSONObject().put("staff_code", staffCode).put("branch", NativeSession.current(this)?.branch ?: "")
                .put("period_type", type).put("period_key", key).put("auto_stats", stats)
                .put("manual_summary", if (type == "daily") workEntriesSummary() else "")
                .put("status", "submitted").put("version", prevVer + 1).put("submitted_at", nowIso())
            // 🔵 TK-ORDER (07.08.2026): wn-লেখার persistent queue নেই — আগে ব্যর্থ হলে
            // রিপোর্ট হারাত ("Saved / retry" বললেও আসলে retry হত না)। এখন ব্যর্থ হলে
            // সঙ্গে সঙ্গে ৩ বার চেষ্টা (ছোট বিরতিতে) — ক্ষণিক নেট-ঝাঁকুনি নিজে সামলায়।
            // শেষেও ব্যর্থ হলে সৎ, স্পষ্ট বার্তা (মিথ্যা "submitted" নয়)।
            // ⛔ id/version হিসাব আগের মতোই; শুধু ব্যর্থতার পথে বেশিবার চেষ্টা।
            /* 📋🔒 V1435 (১৩.০৯.২০২৬, তালিকা ৫৫৪) — পাঠানো লেখাটা হুবহু জমা (report_text),
               মাস্টার Briefing-এর "View"-এ সেটাই দেখবেন। ⛔ সার্ভারে V1435 SQL এখনো না
               চললে (ঘরটা নেই) প্রথম চেষ্টা ব্যর্থ হবে — তখন ঘরটা বাদ দিয়ে আগের মতোই
               পাঠানো হয়, রিপোর্ট কখনো হারায় না। */
            row.put("report_text", summary)
            var ok = false
            var attempt = 0
            while (!ok && attempt < 3) {
                ok = ModuleAuth.insert("wn", "work_reports", row)
                if (!ok && row.has("report_text")) { row.remove("report_text"); continue }
                if (!ok) { attempt++; if (attempt < 3) try { Thread.sleep(1200) } catch (_: Throwable) { } }
            }
            /* 🔔🔒 V1204 (০৮.০৯.২০২৬, TK-রিপোর্ট, হুবহু): *"মান্থলি রিপোর্ট স্টাফ যখন
               সাবমিট করে, স্টাফকে দেখাচ্ছে সাবমিট সাকসেসফুল, কিন্তু মাস্টার মানে
               আমার কাছে তো আসেই না — না হোয়াটসঅ্যাপে, না অ্যাপের কোন নোটিফিকেশন"*।
               🔬 কোডে মেপে দেখা — কথাটা সত্যি ছিল: এই ফাংশনটা শুধু `wn.work_reports`-এ
                  সারিটা বসাত, মাস্টারের জন্য কোনো নোটিশই যেত না।
               ⇒ TK-র সিদ্ধান্ত *"দুটোই চাই"* মেনে: (১) মাস্টারের ঘন্টায় নোটিশ
                 (BriefingRepository-র সেই পুরনো প্রমাণিত পথ, MoneyHandover যেটা
                 ব্যবহার করে), (২) সাবমিটের পরেই WhatsApp/Share শিট নিজে থেকে খোলে।
               ⛔ সেভ ব্যর্থ হলে কোনোটাই হয় না — মিথ্যা "পাঠানো হয়েছে" নয়। */
            /* 🔕🔒 V1436 (TK-নির্দেশ, তালিকা ৫৫৬) — Daily Report-এর নোটিশ আর যায় না
               (স্টাফ WhatsApp-এ পাঠায়)। Monthly-র নোটিশ TK-র নিজের V1204-নির্দেশ
               ("দুটোই চাই"), তাই সেটা আগের মতোই থাকল। ⛔ রিপোর্ট সেভ ও WhatsApp অটুট। */
            if (ok && type == "monthly") try {
                val who = NativeSession.current(this)?.name ?: staffCode
                val br = NativeSession.current(this)?.branch ?: ""
                com.tkbiswas.pilesclinic.native.BriefingRepository().post(
                    this,
                    if (type == "daily") "Daily Report submitted" else "Monthly Report submitted",
                    who + " · " + staffCode + (if (br.isBlank()) "" else " · " + br) + " · " + key,
                    "role", br, "master", "", ""
                )
            } catch (_: Throwable) { }
            runOnUiThread {
                ModuleUi.toast(this, if (ok) "Submitted to Master" else "সেভ হয়নি — একটু পরে আবার Submit চাপুন")
                render()
                /* 💬 V1204 — TK: *"WhatsApp খুলতে হবে জোর করে"*। সাধারণ Share-শিট নয়,
                   WhatsAppMessageChooser-এর প্রমাণিত পথ (Personal/Business বেছে
                   সরাসরি WhatsApp-ই খোলে)। WhatsApp না থাকলে সৎ বার্তা দেখায়। */
                if (ok) try {
                    com.tkbiswas.pilesclinic.native.WhatsAppMessageChooser.sendGeneric(this, summary)
                } catch (_: Throwable) { }
            }
        }.start()
    }


    /* ⬇️🔒 V1204 (০৮.০৯.২০২৬, TK-নির্দেশ): *"ব্যাক বটম সব সময় ডিসপ্লের নিচেতে
       থাকবে"*। Doctor Reminder-এর হুবহু একই পথ — ScrollView-কে fillViewport
       করে উপরে ওজন-১ ফাঁকা জায়গা, তাই বোতামটা সবসময় পর্দার নিচে বসে; লেখা
       বড় হলে স্বাভাবিকভাবেই নিচে নেমে যায়, কখনো ঢাকা পড়ে না। */
    private fun bottomBack(col: LinearLayout, onClick: () -> Unit) {
        (col.parent as? android.widget.ScrollView)?.isFillViewport = true
        col.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        col.addView(ModuleUi.button(this, "Back") { onClick() })
    }

    private fun shareText(text: String) {
        val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
        startActivity(Intent.createChooser(i, "Share report"))
    }

    private var reportHistoryYear: Int? = null

    // 🎨🔒 B523 (06.08.2026, TK-নির্দেশে "My Reports" সহজ/প্রফেশনাল কার্ড-
    // ডিজাইন লক করার পরে — "আমি নিজেই বুঝতে পারছি না, স্টাফরা কী করে
    // বুঝবে") — আগে প্রতিটা সাবমিশন (v1/v2/v3...) আলাদা প্লেইন লাইনে
    // ("daily · 2026-08-06 v4 · Submitted") দেখাত — এখন একই তারিখের
    // সব সংস্করণ একটা কার্ডে একত্র (কতবার আপডেট হয়েছে, বাংলায়), তারিখ
    // DOT ফরম্যাটে, সবুজ "জমা হয়েছে" ব্যাজ। কোনো আইকন/ইমোজি নেই (TK-
    // নির্দেশ)। ⛔ ডেটা/কোয়েরি একটুও বদলায়নি — শুধু দেখানোর ধরন।
    private fun history() {
        backAction = { render() }
        val col = ModuleUi.screen(this, "")
        val nowYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val yearMenu = ArrayList<Pair<String, () -> Unit>>()
        for (y in nowYear downTo nowYear - 5) {
            yearMenu.add(Pair((if (reportHistoryYear == y) "✓ " else "") + "Year $y", { reportHistoryYear = y; history() }))
        }
        yearMenu.add(Pair((if (reportHistoryYear == null) "✓ " else "") + "All Years", { reportHistoryYear = null; history() }))
        val yearSub = reportHistoryYear?.let { "$staffCode · $it" } ?: "$staffCode · All Years"
        col.addView(hero("My Reports", yearSub, 19f, yearMenu))
        val box = ModuleUi.card(this); col.addView(box); box.addView(ModuleUi.body(this, "Loading..."))
        bottomBack(col) { render() }   // ⬇️ V1204
        Thread {
            /* 🔵🔒 V818 (২৯.০৮.২০২৬, TK-নির্দেশে Egress-এর পূর্ণ যাচাই) —
               আগে এখানে ছিল `select=*` **কোনো সীমা ও কোনো ছাঁকনি ছাড়া**।
               ফল: এই পর্দা খুললেই ওই স্টাফের (মাস্টার হলে **সবার**) জীবনের
               **সব রিপোর্ট, সব সংস্করণ** নামত — সঙ্গে সবচেয়ে ভারী দুটো ঘর
               `auto_stats` (jsonb) আর `manual_summary` (স্টাফের লেখা পুরো
               কথা), অথচ এই পর্দা ওদুটোর **একটাও ব্যবহার করে না**।
               ⛔ এখন শুধু যে চারটে ঘর সত্যিই পড়া হয় সেগুলোই আসে
                  (`period_key` · `version` · `accepted` · `seen_at`), নিজের
                  কোড দিয়ে ছাঁকা, আর সর্বোচ্চ ৪০০ সারি (কয়েক বছরের রিপোর্টও
                  এতে ধরে যায়)।
               ⛔ পর্দায় দেখানো এক অক্ষরও বদলায়নি — গোনা · সংস্করণ · অবস্থা
                  সবই ঠিক ওই একই ঘরগুলো থেকেই হয়। ওয়েবে (`notebook.js`)
                  আগে থেকেই `limit(100)` বসানো ছিল, ফোনেই বাকি ছিল। */
            val myScope = if (staffCode.isNotBlank()) "&staff_code=eq." + (try { java.net.URLEncoder.encode(staffCode, "UTF-8").replace("+", "%20") } catch (_: Throwable) { staffCode }) else ""
            val yearScope = reportHistoryYear?.let { y -> "&period_key=gte.${y}-01&period_key=lt.${y + 1}-01" } ?: ""
            val r = ModuleAuth.getRows(
                "wn", "work_reports",
                "select=period_key,version,accepted,seen_at" + myScope + yearScope +
                    "&order=submitted_at.desc&limit=400"
            )
            runOnUiThread {
                box.removeAllViews()
                if (r.length() == 0) { box.addView(ModuleUi.body(this, "No reports yet.")); return@runOnUiThread }
                // একই তারিখের একাধিক সংস্করণ একসাথে গোনা — সর্বোচ্চ version
                // ও তার অবস্থা (accepted/seen/submitted) কার্ডে দেখানো হয়,
                // ক্রম (সবচেয়ে নতুন তারিখ আগে) আগের মতোই।
                data class Grp(val key: String, var count: Int, var maxVer: Int, var state: String)
                val groups = LinkedHashMap<String, Grp>()
                for (i in 0 until r.length()) {
                    val w = r.getJSONObject(i)
                    val key = ns(w, "period_key")
                    val ver = w.optInt("version", 1)
                    val state = if (w.optBoolean("accepted", false)) "Accepted" else if (!w.isNull("seen_at") && ns(w, "seen_at").isNotBlank()) "Seen" else "Submitted"
                    val g = groups.getOrPut(key) { Grp(key, 0, 0, state) }
                    g.count++
                    if (ver >= g.maxVer) { g.maxVer = ver; g.state = state }
                }
                for (g in groups.values) {
                    val d = resources.displayMetrics.density
                    fun dp(v: Int) = (v * d).toInt()
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                        setPadding(dp(12), dp(10), dp(12), dp(10))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.WHITE)
                            cornerRadius = dp(10).toFloat()
                        }
                        elevation = 2f
                        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        lp.bottomMargin = dp(8)
                        layoutParams = lp
                    }
                    val textCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    textCol.addView(TextView(this).apply {
                        text = dotDate(g.key) + " " + NoBengali.s("— দৈনিক রিপোর্ট")
                        textSize = 13.5f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#0B2B59"))
                    })
                    textCol.addView(TextView(this).apply {
                        text = if (g.count > 1) NoBengali.s("N বার আপডেট হয়েছে (সর্বশেষ পাঠানো)").replace("N", g.count.toString())
                            else NoBengali.s("একবার পাঠানো হয়েছে")
                        textSize = 11f; setTextColor(android.graphics.Color.parseColor("#6B7280"))
                    })
                    card.addView(textCol)
                    card.addView(TextView(this).apply {
                        text = NoBengali.s("জমা হয়েছে")
                        textSize = 10f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#166534"))
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(android.graphics.Color.parseColor("#DCFCE7")); cornerRadius = dp(20).toFloat()
                        }
                    })
                    box.addView(card)
                }
            }
        }.start()
    }
}
