package com.tkbiswas.pilesclinic.native

import java.text.SimpleDateFormat
import java.util.Locale

/** TK-REQUESTED GLOBAL RULE (2026-07-24, Locked): every date shown to the
 *  user anywhere in the app must read DD/MM/YYYY (e.g. 31/12/2026) --
 *  never the raw ISO "yyyy-MM-dd" stored in Supabase.
 *
 *  display() is DISPLAY-ONLY -- it never changes what's stored, sorted,
 *  compared, or sent to the server; those all keep using the original
 *  "yyyy-MM-dd" string exactly as before. Call this only at the point a
 *  date is put into a TextView/Toast/etc. for the user to read.
 *
 *  Safe by design: if the input isn't a recognised date (blank, "--",
 *  already-formatted, or unexpected), it's returned unchanged -- never
 *  throws, never blanks a field.
 */
object DateUtil {

    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)   // 🔴 V1158

    // Formats we might receive from Supabase / local storage, in the order
    // we try to parse them.
    private val inputFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    ).map { SimpleDateFormat(it, Locale.US) }

    /* 🔴🔒 V936 (৩১.০৮.২০২৬, TK-নির্দেশ: *"সম্পূর্ণ প্রজেক্টে তারিখ একই ফরমেটে
       থাকতে হবে … ঝুঁকিহীন ভাবে"*) — দেখানোর তারিখ এখন সব জায়গায় **31.08.2026**।
       কিন্তু কিছু নোটিশের ভিতরের তারিখ **মেশিনও পড়ে** (Reopen/Leave-এর Approve
       বোতাম ওই লাইনটা পড়েই বোঝে কোন দিনের কাজ)। তাই দেখানোর লেখা বদলানোর
       **আগে** এই ফাংশনটা বসানো হলো: যেকোনো চেনা ধাঁচ থেকে আসল `yyyy-MM-dd`
       ফিরিয়ে দেয় — বিন্দু · স্ল্যাশ · হাইফেন · আগের কাঁচা ISO, সবই।
       ⇒ **পুরনো অপেক্ষমাণ অনুরোধগুলোতেও Approve আগের মতোই কাজ করে।**
       ⛔ চেনা না গেলে যা এসেছে তাই ফেরে — কখনো ফাঁকা বা ভুল তারিখ নয়। */
    fun iso(raw: String?): String {
        val t = (raw ?: "").trim()
        if (t.isBlank()) return ""
        if (t.length >= 10 && t[4] == '-' && t[7] == '-') return t.substring(0, 10)
        val m = Regex("^(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})").find(t) ?: return t
        val d = m.groupValues[1].padStart(2, '0')
        val mo = m.groupValues[2].padStart(2, '0')
        return m.groupValues[3] + "-" + mo + "-" + d
    }

    fun display(raw: String?): String {
        if (raw.isNullOrBlank()) return raw ?: ""
        val datePart = if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-') raw.substring(0, 10) else raw
        for (f in inputFormats) {
            try {
                val parsed = f.parse(if (f.toPattern() == "yyyy-MM-dd") datePart else raw) ?: continue
                return displayFormat.format(parsed)
            } catch (_: Exception) { /* try next format */ }
        }
        return raw // unrecognised -- leave exactly as-is, never break the screen
    }

    /** TK-REQUESTED (2026-07-24): date+time display for Medicine Slip / Visit
     *  History. Takes a live Date (these two screens stamp "now", not a
     *  stored value).
     *
     *  🔒 TK-এর নতুন স্থায়ী নিয়ম (29.07.2026 সকাল ১০.০০ — খাতার সারি B76):
     *  **তারিখ সবসময় `31.12.2026` (বা `31/12/2026`) আর সময় সবসময় `11.30 AM`**
     *  — অর্থাৎ AM/PM **বড় হাতের অক্ষরে**। আগে ছোট হাতের `am/pm` করা হত
     *  (24.07.2026-এর পুরনো নিয়ম); TK নিজে সেটা বদলেছেন, তাই নিচের
     *  `replace()` দুটো তুলে দেওয়া হয়েছে। উদাহরণ: `31.12.2026 5.40 PM`।
     *  ⛔ এটা আর ছোট হাতের অক্ষরে ফেরানো যাবে না। */
    /* 🔴🔴🔒 V1158 (৩১.১২.২০২৬ ৩.১৫ PM, TK-নির্দেশ, স্পষ্ট উদাহরণসহ:
       *"31/12/2026 : 3.15 PM — সম্পূর্ণ প্রজেক্ট এরকম হতে হবে, প্রিন্ট আউট থেকে
       শুরু করে অ্যাপের মধ্যে থাকা সমস্ত জায়গায়"*) — তারিখে **স্ল্যাশ**, তারিখ ও
       সময়ের মাঝে **" : "**, সময়ে **বিন্দু** ও বড় হাতের AM/PM।
       ⛔ শুধু **দেখানোর** লেখা — ডেটাবেসে · সাজানোয় · তুলনায় সবখানে আগের মতোই
          `yyyy-MM-dd` যায়, এক অক্ষরও বদলায়নি।
       ⛔ পুরনো লেখা পড়ার নিয়ম (`iso()`) বিন্দু ও স্ল্যাশ **দুটোই** চেনে, তাই
          আগের জমা কোনো নোটিশ/অনুরোধ ভাঙে না। */
    const val DISPLAY_DATE = "dd/MM/yyyy"
    const val DISPLAY_TIME = "h.mm a"
    const val DISPLAY_DATE_TIME = "dd/MM/yyyy : h.mm a"

    fun displayWithTime(d: java.util.Date): String {
        return SimpleDateFormat(DISPLAY_DATE_TIME, Locale.US).format(d)
    }

    // 🔒 TK-নির্দেশ (04.08.2026, ছবিসহ — Briefing-এর ডিলিট-অনুরোধ কার্ডে শুধু
    // তারিখ ছিল, সময় ছিল না, তাই "এটা কি আজকের নাকি পুরনো" বোঝা যাচ্ছিল না —
    // TK-কে বারবার জিজ্ঞাসা করতে হচ্ছিল)। এখন থেকে সময়ও দেখানো হবে, একই
    // লক-করা ফরম্যাটে (B76): `31.12.2026 5.40 PM`। ⛔ পার্স করার নিয়ম হুবহু
    // `display()`-এর মতোই (একই ইনপুট-ফরম্যাটের তালিকা পুনর্ব্যবহার) — কোনো
    // নতুন ঝুঁকি নেই। ⛔ পুরনো `display(raw)` (শুধু তারিখ) অন্য কোথাও যেভাবে
    // ব্যবহার হয়, তা এক অক্ষরও বদলায়নি — এটা সম্পূর্ণ নতুন, আলাদা ফাংশন।
    fun displayWithTime(raw: String?): String {
        if (raw.isNullOrBlank()) return raw ?: ""
        for (f in inputFormats) {
            if (f.toPattern() == "yyyy-MM-dd") continue // এই ফরম্যাটে সময় থাকেই না
            try {
                val parsed = f.parse(raw) ?: continue
                return displayWithTime(parsed)
            } catch (_: Exception) { /* try next format */ }
        }
        return display(raw) // সময় না পাওয়া গেলে অন্তত তারিখটুকু (আগের আচরণ)
    }
}
