package com.tkbiswas.pilesclinic.native

/**
 * Native mirror of the staff/master/doctor accounts and role passwords defined
 * in the website's config.js (window.RK_CONFIG.users / .passwords), currently
 * 03_NETLIFY_READY/config.js. (B271, 02.08.2026: the old duplicate phone-copy
 * at assets/www/config.js was removed as unused -- this file's own hardcoded
 * list below was never read from it at runtime, it was a manually-kept copy.)
 *
 * WHY THIS EXISTS: Step 1 of the native rebuild (Login + Dashboard) needs to
 * authenticate the same accounts the WebView login already uses, without
 * requiring a network call just to show the Login screen. This list is a
 * plain, manually-kept copy of config.js -- if staff/passwords are ever
 * changed in config.js, this file must be updated to match (see
 * NATIVE_REBUILD_NOTES.md for the exact sync procedure). The individual
 * user's password can still be overridden live via the existing
 * "usercredentials" Supabase table (see CloudPasswordCheck.kt), which is
 * checked FIRST -- this bundled list is only the fallback/default, exactly
 * mirroring how the JS login() function already behaves.
 */
data class StaffAccount(val mobile: String, val name: String, val branch: String, val role: String)

object StaffDirectory {

    private val master = listOf(
        StaffAccount("8001080080", "TK BISWAS", "All", "master")
    )

    private val staff = listOf(
        StaffAccount("9883605917", "KNE-LAXMI", "Kishanganj", "staff"),
        StaffAccount("8676002200", "KNE-BRANCH", "Kishanganj", "staff"),
        // 🟢🔒 V1136 (০৬.০৯.২০২৬, TK-নির্দেশ ও ডিটেলস সহ) — কিশানগঞ্জের নতুন
        //    স্টাফ **MANISHA LAKRA**, জয়েন ০৩.০৯.২০২৬। কোডটা TK নিজে ঠিক করেছেন
        //    (প্রথমে "KISHAN-10" বলেছিলেন, ব্রাঞ্চের বাকি কোডের সঙ্গে মিলিয়ে
        //    **KNE-KISHAN10** চূড়ান্ত)।
        // ⛔ বাংলা-বন্ধ ব্যবস্থা আলাদা করে যোগ করতে হয়নি — ওটা **ব্রাঞ্চ ধরে**
        //    চলে (V451), কিশানগঞ্জের সব Staff account এমনিতেই ইংরেজি পায়।
        StaffAccount("7482966958", "KNE-KISHAN10", "Kishanganj", "staff"),
        // 🔴🔒 V1441 (১৩.০৯.২০২৬, TK-নির্দেশ) — TK: *"KNE-KISHAN-11 অনেক আগে
        //    থেকেই নেই"* — ARCHANA MANDAL BISWAS (V1137, ০৬.০৯.২০২৬-এ যোগ
        //    হয়েছিলেন) কাজ ছেড়ে দিয়েছেন। V453/V404/V450-এর হুবহু একই নিয়ম:
        //    পুরনো নম্বর (7478288608) এখানে আর নেই ⇒ ওই নম্বর দিয়ে আর লগইন
        //    হবে না। ⛔ পুরনো এন্ট্রিতে করা কাজ/হাজিরা/বেতনের রেকর্ড কিছুই
        //    মোছা হয়নি — শুধু ঢোকার পথ বন্ধ। ডেটাবেসের `hr.staff_profiles`
        //    ও `usercredentials`-এ সারিটা বন্ধ করার SQL আলাদা দেওয়া হলো
        //    (V834-এর সতর্কতা মিলিয়ে — শুধু এখান থেকে তুললে যথেষ্ট নয়)।
        // 🔴🔒 V1510 (১৬.০৯.২০২৬, TK-নির্দেশ) — ISHRAT PARWEEN (KNE-KISHAN12,
        //    V1378-এ যোগ হয়েছিলেন) কাজ ছেড়ে দিয়েছেন। KISHAN5/KISHAN8-এর হুবহু
        //    একই নিয়ম: পুরনো নম্বর (9679319516) এখানে আর নেই ⇒ ওই নম্বর দিয়ে
        //    আর লগইন হবে না। পুরনো এন্ট্রিতে করা কাজ/রেকর্ড কিছুই মোছা হয়নি —
        //    শুধু লগইনের দরজা বন্ধ। ডেটাবেসের hr.staff_profiles-এও বন্ধ করার
        //    SQL আলাদা দেওয়া হলো (V834-এর সতর্কতা মিলিয়ে — শুধু এখান থেকে
        //    তুললে যথেষ্ট নয়)।
        // 🔴 V453 (20.08.2026) — TK: *"KISHAN-5 কাজ ছেড়ে দিয়েছে, তার বদলে নতুন
        //    ক্যান্ডিডেট।"* FALA-15/PK-ROY/old BIR নম্বরের একই নিয়ম: পুরনো নম্বর
        //    (6207841890) এখানে আর নেই ⇒ ওই নম্বর দিয়ে আর লগইন হবে না। পুরনো
        //    এন্ট্রিতে করা কাজ/রেকর্ড কিছুই মোছা হয়নি — শুধু লগইনের দরজা বন্ধ।
        // 🔴🔒 V734 (২৭.০৮.২০২৬, TK-এর সরাসরি নির্দেশ) — কিশানগঞ্জের
        //    **KNE-KISHAN6** ঘরের স্টাফ কাজ থেকে বাদ। TK-এর নির্দেশ অনুযায়ী
        //    তাঁর নাম ও মোবাইল নম্বর কোথাও রাখা হয়নি — কমেন্টেও নয়।
        //    ⇒ ওই এন্ট্রি এখানে আর নেই, তাই ওই নম্বরে আর লগইন হবে না।
        //    ⛔ V453-এ KNE-KISHAN5-কে যেভাবে সরানো হয়েছিল, হুবহু সেই নিয়ম।
        //    ⛔ তাঁর করা পুরোনো কাজ/রেকর্ড কিছুই মোছা হয়নি — ব্যবসার হিসাব
        //       নষ্ট করা যায় না; শুধু ঢোকার পথ বন্ধ।
        //    🆕 তাঁর জায়গায় নতুন স্টাফ নিচে যোগ হলো (V735)।
        // 🟢🔒 V735 (২৭.০৮.২০২৬, TK-এর সরাসরি নির্দেশ) — কিশানগঞ্জের নতুন
        //    স্টাফ। TK-এর দেওয়া কোড: KNE-KISHAN8।
        // 🔒🔒 V834 (২৯.০৮.২০২৬, TK-নির্দেশ: *"কাজ থেকে বের করে দিয়েছি"*)
        //    — KNE-KISHAN8 (7321960416) এই বাঁধা তালিকা থেকে **তুলে নেওয়া হলো**,
        //    তাই এই কোড দিয়ে আর লগইন হবে না।
        //    ⚠️ **কেন শুধু এটুকুতে হয় না — যাচাই করে পাওয়া:** `LoginActivity`
        //       আগে এই বাঁধা তালিকা দেখে, **না পেলে তবেই** মেঘে
        //       (`staff_login_list()`) যায়; আর সার্ভারের ওই ফাংশন
        //       `where coalesce(active, true)` মানে — অর্থাৎ `hr.staff_profiles`-এ
        //       সারিটা `active = false` না করা পর্যন্ত মেঘের পথে ঢোকা যাবে।
        //       ⇒ **দুটোই লাগে**: এখান থেকে তোলা + মেঘে Remove/`active=false`।
        //    ⛔ V453-এ KNE-KISHAN5-এর হুবহু একই নিয়ম — **পুরোনো রেকর্ড
        //       (হাজিরা · রিমার্ক · কাজ) কিচ্ছু মোছা হয়নি**, শুধু ঢোকার পথ বন্ধ।
        StaffAccount("9647840067", "JPE-CRP", "Jalpaiguri", "staff"),
        StaffAccount("8101397763", "JPE-JALPAI-13", "Jalpaiguri", "staff"),
        // 🔒 TK-এর নির্দেশ (02.08.2026 দুপুর ~১২.১৫ pm, খাতার সারি B282): নতুন
        // স্টাফ RUPAM, জলপাইগুড়ি ব্রাঞ্চ।
        StaffAccount("8167096595", "JPE-RUPAM", "Jalpaiguri", "staff"),
        StaffAccount("8436002200", "JPE-BRANCH", "Jalpaiguri", "staff"),
        StaffAccount("7679751521", "COB-UTTAMA", "Cooch Behar", "staff"),
        StaffAccount("7501256248", "COB-4", "Cooch Behar", "staff"),
        StaffAccount("8514002200", "COB-BRANCH", "Cooch Behar", "staff"),
        // 🟢🔒 V1377 (১২.০৯.২০২৬, TK-নির্দেশ) — কোচবিহারের নতুন স্টাফ
        //    **ARMAN HOQUE** — TK: "RUPAM যা ছিল তাই থাক" (JPE-RUPAM-এর হুবহু
        //    একই ভূমিকা — শুধু Doctor Visit ও IN/OUT TIME; নতুন কোনো
        //    কোড-স্তরের আটকানো নয়, RUPAM-এর মতোই ভরসার উপর)।
        StaffAccount("9883884394", "COB-ARMAN", "Cooch Behar", "staff"),
        StaffAccount("9883623823", "FLK-1", "Falakata", "staff"),
        StaffAccount("8514001100", "FLK-BRANCH", "Falakata", "staff"),
        // 🔒 TK-এর নির্দেশ (29.07.2026 দুপুর ১.৩০, খাতার সারি B86): বিরপাড়ার
        // বিরপাড়ার ব্রাঞ্চ নম্বর ও কোড নাম বদলানো হয়েছে — এখন 8538002200 · BIR-BRANCH।
        // TK: *"ওই নাম্বার চিরতরে মুছে দিন।"* তাই পুরনো নম্বরটা কোথাও রাখা হয়নি —
        // ওটা দিয়ে আর লগইন হবে না, আর পুরনো এন্ট্রিতে নামের বদলে কাঁচা নম্বর দেখাবে
        // (TK জেনেশুনে এটাই বলেছেন)। বাকি ব্রাঞ্চের মতোই নাম এখন `<CODE>-BRANCH`।
        StaffAccount("8538002200", "BIR-BRANCH", "Birpara", "staff")
        // 🔴 V404 (16.08.2026) — TK: *"Swapna Adhikari কাজ ছেড়ে দিয়েছে ·
        //    ওই নাম্বারের কোনো অংশ যেন না থাকে।"* FALA-15 (SWAPNA ADHIKARI) এখানে
        //    আর নেই ⇒ ওই নম্বর দিয়ে লগইন হবে না। উপরের BIR-BRANCH-এর মতোই নিয়ম:
        //    পুরনো এন্ট্রিতে নামের বদলে কাঁচা নম্বর দেখাবে — TK জেনেশুনে
        //    "রেকর্ড অটুট থাক" বেছেছেন, পুরনো কিছুই মোছা হয়নি।
    )

    private val doctor = listOf(
        StaffAccount("7980993652", "Dr. K.H MANDAL", "Cooch Behar", "doctor"),
        StaffAccount("8001800148", "Dr. JAY BANIK", "Jalpaiguri", "doctor"),
        StaffAccount("9046366596", "AMIT GOLDAR", "Kishanganj", "doctor"),
        // 🔴 V450 (18.08.2026, TK-নির্দেশ: "ভুল করে তোলা হয়ে গেছিল, যেন না
        //    থাকে, এই নম্বর দিয়ে যেন কিশানগঞ্জে লগইন করতে না পারে")।
        //    PK ROY (6297625447) এখানে আর নেই ⇒ এই নম্বর দিয়ে আর লগইন হবে
        //    না। FALA-15-এর (V404) ঠিক একই নিয়ম: পুরনো এন্ট্রি/রেকর্ড কিছুই
        //    মোছা হয়নি, শুধু লগইনের দরজা বন্ধ।
        // 🔵 V308 (১০.০৮, TK-নির্দেশ: সব অংশীদারই ডাক্তার): ৪ জন এখন ডাক্তার (পুরো ডাক্তার-অ্যাক্সেস
        // + নিজের My Share Ledger)। Saikat-এর ডাক্তার-ব্রাঞ্চ Falakata (TK)। config.js users.doctor-এর হুবহু।
        StaffAccount("7479173399", "J.H MANDAL", "Cooch Behar", "doctor"),
        StaffAccount("9002610352", "GOKUL", "Cooch Behar", "doctor"),
        StaffAccount("7810907954", "Dr. SAIKAT ROY", "Falakata", "doctor"),
        StaffAccount("9242009205", "Dr. PRANAB BISWAS", "Birpara", "doctor")
    )

    // Field Officer / RMP account — mirrors config.js users.field. Was missing
    // from the native directory so this login could not authenticate on the app
    // even though the "field" role (tiles + password below) was already wired.
    private val field = listOf(
        StaffAccount("9002003540", "Field Officer", "All", "field")
    )

    // TK DECISION (2026-07-16): a stronger-password version was prepared but
    // TK asked to hold off for now (wants to coordinate with staff first,
    // to avoid anyone getting locked out) -- reverted back to the original
    // defaults so today's login keeps working exactly as before. The
    // stronger set is saved below, ready to use whenever TK says go:
    //   master -> "Tk#Master@2026", staff -> "Pcl#Staff@2026",
    //   doctor -> "Pcl#Doctor@2026", field -> "Pcl#Field@2026"
    private val rolePasswords = mapOf(
        "master" to "admin123",
        "staff" to "staff123",
        "doctor" to "doctor123",
        "field" to "field123"
    )

    private val all: List<StaffAccount> = master + staff + doctor + field

    /** Normalizes a typed mobile number the same way mob()/normMob() do in app.js:
     * digits only, last 10 digits (so "+91XXXXXXXXXX" and "XXXXXXXXXX" match). */
    fun normalizeMobile(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun findAccount(mobile: String): StaffAccount? {
        val target = normalizeMobile(mobile)
        if (target.length != 10) return null
        return all.find { normalizeMobile(it.mobile) == target }
    }

    fun defaultPasswordFor(role: String): String = rolePasswords[role] ?: ""

    /** All configured accounts (master + staff + doctor + field), used by the
     * native Password Center to list every user. Read-only copy. */
    fun allAccounts(): List<StaffAccount> = all
}
