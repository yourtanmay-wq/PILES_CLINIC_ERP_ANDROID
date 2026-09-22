package com.tkbiswas.pilesclinic.native

/**
 * ⛔🔒🔒 V890 (৩০.০৮.২০২৬, TK-নির্দেশ) — কাজ ছেড়ে যাওয়া/বাদ দেওয়া স্টাফ।
 *
 * TK: *"তাকে কাজ থেকে বের করে দেওয়া হয়েছে · তার কোনো ডিটেল যেন অ্যাপে না
 * থাকে · সম্পূর্ণ প্রজেক্টের কোথাও যেন না থাকে · তার নম্বর দিয়ে যেন লগইন
 * না করা যায়।"*
 *
 * **কেন এই একটাই ফাইল:** নাম/নম্বর কোডের নানা জায়গায় ছড়িয়ে থাকলে
 * ভবিষ্যতে একটা জায়গা বাদ পড়বেই। তাই **একটাই তালিকা**, আর প্রতিটা পর্দা
 * ও লগইন এখান থেকেই যাচাই করে।
 *
 * ⛔ এতে রোগীর কোনো তথ্য · টাকা · ইতিহাস মোছে না — শুধু ওই ব্যক্তি
 *    অ্যাপে **দেখা যাবে না ও ঢুকতে পারবে না**।
 * ⛔ নতুন কাউকে বাদ দিতে হলে শুধু নিচের তালিকায় একটা লাইন যোগ করুন।
 */
object BlockedStaff {

    /** শেষ ১০ অঙ্ক ধরে মেলানো হয় — যেভাবেই নম্বর লেখা থাক। */
    private val MOBILES = setOf(
        "9339139852"    // BIR-5 · RESAM KHATUN · Birpara (TK-নির্দেশ ৩০.০৮.২০২৬)
    )

    /** কর্মী-কোড ধরেও আটকানো হয়, নম্বর বদলে গেলেও যেন ফাঁক না থাকে। */
    private val CODES = setOf("BIR-5")

    private fun digits(s: String?): String =
        (s ?: "").filter { it.isDigit() }.takeLast(10)

    fun isBlockedMobile(mobile: String?): Boolean = digits(mobile) in MOBILES

    fun isBlockedCode(code: String?): Boolean =
        (code ?: "").trim().uppercase() in CODES

    /** নাম · কোড · নম্বর — যেকোনো একটাতে মিললেই বাদ। */
    fun isBlocked(mobile: String? = null, code: String? = null): Boolean =
        isBlockedMobile(mobile) || isBlockedCode(code)
}
