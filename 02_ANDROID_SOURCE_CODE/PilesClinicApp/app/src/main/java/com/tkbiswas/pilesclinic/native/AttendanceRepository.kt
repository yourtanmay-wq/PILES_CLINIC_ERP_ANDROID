package com.tkbiswas.pilesclinic.native

import android.content.Context
import com.tkbiswas.pilesclinic.modules.ModuleAuth
import org.json.JSONArray
import org.json.JSONObject

/**
 * 🕐🔒 V496 (২১.০৮.২০২৬, TK-এর চূড়ান্ত নির্দেশ §৪ ও §৬) — **IN TIME সেভ করা।**
 *
 * অ্যাপ **কিছুই পাঠায় না** — staff_code নয়, branch নয়, তারিখ নয়, সময় নয়।
 * সার্ভারের `wn.mark_check_in()` নিজে সব ঠিক করে (ফাইল:
 * `04_SUPABASE_DATABASE_SETUP/V496_MARK_CHECK_IN_2026-08-21.sql`)।
 *
 * ⛔ অ্যাপ কখনো নিজে সিদ্ধান্ত নেয় না — শুধু সার্ভারের `status` দেখে বার্তা দেখায়।
 * ⛔ ব্যর্থ হলে ফোনে জমিয়ে পরে পাঠানো হয় **না** — সময়টা সার্ভারের হতেই হবে,
 *    নইলে "পরে পাঠানো" মানেই ভুল সময় বসে যাওয়া।
 */
/**
 * 🔤🔒 V519 (২২.০৮.২০২৬, TK-নির্দেশ, ছবিসহ): হাজিরার পর্দার লেখা সব
 * ব্রাঞ্চেই ইংরেজি — আগে বাংলা ছিল এবং ইংরেজি হত শুধু `NoBengali` চালু
 * থাকলে (কেবল কিশানগঞ্জ)। ⛔ কোনো নিয়ম · হিসাব · ডেটা বদলায়নি, শুধু লেখা।
 */
object AttendanceRepository {

    /** সার্ভার যা যা বলতে পারে। */
    enum class Status {
        SAVED,        // এইমাত্র বসল
        ALREADY,      // আজ আগেই ছিল — পুরনো সময় অপরিবর্তিত
        ON_LEAVE,     // আজ অনুমোদিত ছুটি
        NOT_STAFF,    // ডাক্তার/মাস্টার — হাজিরার ব্যবস্থা নেই
        INACTIVE,     // বাদ দেওয়া হয়েছে
        SUSPENDED,    // নির্দিষ্ট তারিখ পর্যন্ত বন্ধ
        NO_PROFILE,
        /* 🕒🔒 V1243 (০৮.০৯.২০২৬, TK-নির্দেশ হুবহু — খাতার সারি ৩৬৪:
           *"সকাল ৯টা থেকে সন্ধ্যা ৬টার বাইরে একেবারে আটকে দিন"*)। */
        OUTSIDE_HOURS,
        NETWORK,      // পৌঁছানোই গেল না
        ERROR
    }

    data class Outcome(
        val status: Status,
        val checkIn: String = "",
        val branch: String = "",
        val message: String = ""
    ) {
        val ok: Boolean get() = status == Status.SAVED
    }

    private fun parseStatus(raw: String): Status = when (raw.trim().lowercase()) {
        "saved" -> Status.SAVED
        "already" -> Status.ALREADY
        "on_leave" -> Status.ON_LEAVE
        "not_staff" -> Status.NOT_STAFF
        "inactive" -> Status.INACTIVE
        "suspended" -> Status.SUSPENDED
        "no_profile" -> Status.NO_PROFILE
        "outside_hours" -> Status.OUTSIDE_HOURS      // 🕒 V1243
        else -> Status.ERROR
    }

    /**
     * IN TIME বসানোর চেষ্টা। **নেটওয়ার্ক থ্রেড থেকে ডাকতে হবে।**
     *
     * ⛔ এখানে সব ভুল একটা বড় `catch`-এ চাপা দেওয়া হয়নি — লগইন-সমস্যা,
     *    নেটওয়ার্ক-সমস্যা ও সার্ভারের উত্তর আলাদা করে ধরা হয়েছে।
     */
    /* 🕒🔒 V1243 (০৮.০৯.২০২৬, TK-রিপোর্ট ছবিসহ ও সরাসরি নির্দেশ,
       খাতার সারি ৩৬৪) — TK: *"এত রাত্রে একটা স্টাফ চেম্বারে আসলো কি করে ·
       সকাল ৯টা থেকে সন্ধ্যা ৬টা পর্যন্ত আমাদের চেম্বার খোলা থাকে · রাত ১১টা
       ২১-এ আপনি কিভাবে তাকে এলাউ করলেন"* ⇒ *"সকাল ৯টা থেকে সন্ধ্যা ৬টার
       বাইরে একেবারে আটকে দিন"*।

       🔴 **যাচাই করে যা পাওয়া গেল (আমারই ফাঁক):** IN TIME-এ **কোনো সময়ের
          সীমা কোথাও বসানোই ছিল না** — না এই অ্যাপে, না সার্ভারের
          `wn.mark_check_in()`-এ। শুধু ১১টার পরে হলে পর্দায় "(Late)" লেখা হত,
          কিন্তু আটকানো হত না। তাই রাত ১১.২১-এও হাজিরা বসে গিয়েছিল।

       ⇒ এখন **সকাল ৯টা – সন্ধ্যা ৬টা**র বাইরে হলে ক্লাউডে অনুরোধই যায় না।
       ⛔ সময় নেওয়া হয় **ভারতীয় ঘড়িতে** (Asia/Kolkata) — ফোনের সময়-অঞ্চল
          যা-ই থাকুক, নিয়ম একই।
       ⛔ ভিতরের সময়ে আচরণ **হুবহু আগের মতোই** — একটাও অন্য নিয়ম বদলায়নি।
       ⚠️ এটা প্রথম দরজা। **আসল তালা সার্ভারে** — TK-কে দেওয়া SQL চালালে
          পুরনো APK থেকেও আর কেউ সময়ের বাইরে হাজিরা দিতে পারবেন না। */
    const val OPEN_FROM_MIN = 9 * 60        // সকাল ৯টা
    const val OPEN_TO_MIN = 18 * 60         // সন্ধ্যা ৬টা
    const val OUTSIDE_HOURS_TEXT =
        "Attendance can only be marked between 9:00 AM and 6:00 PM."

    /** ভারতীয় ঘড়িতে এখন কত মিনিট (00:00 থেকে)। */
    private fun nowMinutesIst(): Int = try {
        val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
    } catch (_: Throwable) { -1 }

    fun markCheckIn(context: Context): Outcome {
        // ০) চেম্বারের সময়ের বাইরে হলে এখানেই শেষ (🕒 V1243)
        val nowMin = nowMinutesIst()
        if (nowMin >= 0 && (nowMin < OPEN_FROM_MIN || nowMin > OPEN_TO_MIN)) {
            return Outcome(Status.OUTSIDE_HOURS, message = OUTSIDE_HOURS_TEXT)
        }
        // ১) লগইন নিশ্চিত করা (প্রজেক্টের প্রমাণিত পথ — অন্য RPC-গুলোর মতোই)
        if (!ModuleAuth.isSignedIn) {
            val err = try {
                ModuleAuth.signInCurrentSession(context.applicationContext)
            } catch (t: Throwable) {
                return Outcome(Status.NETWORK, message = "Could not verify your login. Please check your internet and try again.")
            }
            if (err != null) {
                return Outcome(Status.NETWORK, message = "Could not verify your login. Please check your internet and try again.")
            }
        }

        // ২) সার্ভারকে ডাকা — কোনো তথ্য পাঠানো হয় না
        val rpc = try {
            ModuleAuth.rpc("wn", "mark_check_in", JSONObject())
        } catch (t: Throwable) {
            return Outcome(Status.NETWORK, message = "Could not send your attendance. Please check your internet and try again.")
        }
        if (!rpc.ok) {
            return Outcome(
                Status.NETWORK,
                message = "Could not save your attendance right now. Please check your internet and try again."
            )
        }

        // ৩) উত্তর পড়া — একটাই সারি
        return try {
            val arr = JSONArray(rpc.body)
            if (arr.length() == 0) {
                Outcome(Status.ERROR, message = "The server did not reply. Please try again.")
            } else {
                val o = arr.getJSONObject(0)
                val st = parseStatus(o.optString("status", ""))
                Outcome(
                    status = st,
                    checkIn = o.optString("check_in", ""),
                    branch = o.optString("branch", ""),
                    /* 🔤 V519: সার্ভারের বার্তাও ইংরেজিতে দেখানো হয় (নিচে দেখুন) —
                       ⛔ ডেটাবেসে একটাও অক্ষর বদলাতে হয়নি, TK-কে কোনো SQL
                          চালাতে হবে না। */
                    message = englishMessage(o.optString("message", ""), st)
                )
            }
        } catch (t: Throwable) {
            Outcome(Status.ERROR, message = "The server's reply could not be read. Please try again.")
        }
    }

    /**
     * 🔤🔒 V519 (২২.০৮.২০২৬, TK-নির্দেশ): **সার্ভারের বাংলা বার্তাও ইংরেজিতে।**
     *
     * হাজিরার সিদ্ধান্ত নেয় ডেটাবেসের `wn.mark_check_in()` ফাংশন, আর সে
     * বার্তাগুলো **বাংলায়** পাঠায় (V496-এর SQL)। তাই অ্যাপের লেখা ইংরেজি
     * করলেও ওই বার্তাগুলো বাংলাই থেকে যেত।
     *
     * ⛔ **ডেটাবেস ছোঁয়া হয়নি** — বদলে এখানে, দেখানোর ঠিক আগে, চেনা
     *    টুকরোগুলো ইংরেজিতে বদলে দেওয়া হয়। TK-কে কোনো SQL চালাতে হবে না,
     *    আর পুরোনো APK-ও আগের মতোই চলবে।
     * ⛔ তারিখ/সময় বার্তার মাঝখানে বসে (যেমন "আপনি <তারিখ> পর্যন্ত বন্ধ"),
     *    তাই আস্ত বাক্য নয় — **টুকরো ধরে** বদলানো হয়, সংখ্যাগুলো অটুট থাকে।
     * ⛔ ভবিষ্যতে সার্ভার নতুন কোনো বাংলা বার্তা পাঠালে সেটা যেন পর্দায়
     *    না ওঠে, তাই শেষে একটা জাল আছে — তখন ওই অবস্থার জন্য একটা সাধারণ
     *    ইংরেজি বাক্য দেখানো হয়।
     * ⛔ ইংরেজি বার্তা এলে (বা ফাঁকা এলে) কিছুই বদলায় না।
     */
    private val SERVER_TEXT = listOf(
        "আপনার প্রোফাইল পাওয়া যায়নি। মাস্টারকে জানান।"
            to "Your profile was not found. Please inform the Master.",
        "ডাক্তারদের জন্য হাজিরার ব্যবস্থা নেই — আপনি যেকোনো সময় আসতে ও যেতে পারেন।"
            to "Doctors do not mark attendance - you may come and go at any time.",
        "এই অ্যাকাউন্টে হাজিরার ব্যবস্থা নেই।"
            to "Attendance is not used for this account.",
        "আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে। মাস্টারকে জানান।"
            to "Your account has been closed. Please inform the Master.",
        "আজ আপনার ছুটি অনুমোদিত — হাজিরা লাগবে না।"
            to "Your leave for today is approved - no attendance needed.",
        "হাজিরা বসানো গেল না। আবার চেষ্টা করুন।"
            to "Attendance could not be marked. Please try again.",
        "হাজিরা হয়ে গেছে।" to "Attendance done.",
        // এই দুটোর মাঝখানে সার্ভার তারিখ/সময় বসায় — তাই টুকরো ধরে
        "আজ আগেই হাজিরা হয়েছে — " to "Attendance was already marked today - ",
        "। দিনে একবারই দেওয়া যায়।" to ". It can only be given once a day.",
        "আপনি " to "You are suspended until ",
        " পর্যন্ত বন্ধ আছেন। মাস্টারকে জানান।" to ". Please inform the Master."
    )

    private fun hasBengali(s: String): Boolean = s.any { it.code in 0x0980..0x09FF }

    private fun defaultFor(status: Status): String = when (status) {
        Status.SAVED, Status.ALREADY -> "Attendance done."
        Status.ON_LEAVE -> "Your leave for today is approved - no attendance needed."
        Status.NOT_STAFF -> "Attendance is not used for this account."
        Status.INACTIVE, Status.SUSPENDED -> "Your account has been closed. Please inform the Master."
        Status.NO_PROFILE -> "Your profile was not found. Please inform the Master."
        Status.OUTSIDE_HOURS -> OUTSIDE_HOURS_TEXT   // 🕒 V1243
        else -> "Could not mark attendance. Please try again."
    }

    private fun englishMessage(raw: String, status: Status): String {
        if (raw.isBlank() || !hasBengali(raw)) return raw
        var out = raw
        for ((bn, en) in SERVER_TEXT) out = out.replace(bn, en)
        return if (hasBengali(out)) defaultFor(status) else out
    }
}
