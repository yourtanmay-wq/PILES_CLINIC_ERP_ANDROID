package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🆕🔒 খাতার সারি — Dialer → Contacts কার্ডের ট্যাগ Hide (TK-নির্দেশ,
 * 05.08.2026)। TK-এর স্পষ্ট সিদ্ধান্ত (প্রশ্ন করে নিশ্চিত হয়ে):
 *   ১) Hide করলে **শুধু এই স্টাফের ফোনে**, শুধু Dialer/Contacts কার্ডে —
 *      Follow-up কার্ডে বা অন্য কারো ফোনে কিছুই বদলায় না।
 *   ২) Hide **প্রতি-কন্টাক্ট, প্রতি-ট্যাগ** (যেমন শুধু Amit Roy-র
 *      "MOINAGURI" ট্যাগ, বাকি সবার MOINAGURI ঠিকই থাকবে)।
 *   ৩) মাঝের ট্যাগ হাইড হলে বাকিগুলো ফাঁকা জায়গা ছাড়া পাশাপাশি বসবে
 *      (রেন্ডার করার সময় হাইড-করা ট্যাগ বাদ দিয়ে বাকিগুলোই সাজানো হয়,
 *      তাই এটা এমনিতেই হয় — আলাদা কোনো কোড লাগে না)।
 *
 * ⛔ এটা সম্পূর্ণ স্থানীয় (SharedPreferences) — কোনো ক্লাউড-কল/SQL/নতুন
 *    টেবিল লাগেনি, কারণ TK নিজেই বলেছেন এটা শুধু ব্যক্তিগত সুবিধার জন্য।
 * ⛔ underlying তথ্য (branch/disease/address) কোথাও মোছা বা বদলানো হয় না
 *    — শুধু এই একটা কার্ডে দেখানো/না-দেখানো।
 */
object DialerTagPrefs {

    enum class TagType { STAGE, BRANCH, DISEASE, ADDRESS }

    private fun prefs(context: Context, staffMobile: String) =
        context.getSharedPreferences("dialer_tag_hide_" + staffMobile.filter { it.isDigit() }.takeLast(10), Context.MODE_PRIVATE)

    private fun keyFor(contactMobile: String, tag: TagType): String =
        contactMobile.filter { it.isDigit() }.takeLast(10) + "_" + tag.name

    fun isHidden(context: Context, staffMobile: String, contactMobile: String, tag: TagType): Boolean =
        prefs(context, staffMobile).getBoolean(keyFor(contactMobile, tag), false)

    fun setHidden(context: Context, staffMobile: String, contactMobile: String, tag: TagType, hidden: Boolean) {
        prefs(context, staffMobile).edit().putBoolean(keyFor(contactMobile, tag), hidden).apply()
    }
}
