package com.tkbiswas.pilesclinic.modules

import android.content.Context

/**
 * 🔎🔒 V932 (৩১.০৮.২০২৬, TK-নির্দেশ: *"হ্যাঁ বসান"*) —
 * **"Opening…" পর্দায় কোন ধাপে আটকাল, সেটা পর্দাতেই দেখা যাবে।**
 *
 * TK-এর রিপোর্ট (ছবিসহ, ৮:৫৪ → ৮:৫৬): নোটিফিকেশনে চাপার পরে পর্দা
 * "Opening… Please wait a moment." দেখিয়ে **চিরকাল দাঁড়িয়ে ছিল**, Back-ও
 * কাজ করেনি। তিনটে প্রমাণ মিলিয়ে বোঝা গেছে অ্যাপের **মূল থ্রেড জমে গেছে**
 * (ANR): (১) আধ সেকেন্ড পরপর সেকেন্ড-গোনা বসার কথা — বসেনি; (২) ২৫ সেকেন্ডে
 * "Could not open" বার্তা আসার কথা — আসেনি; (৩) Back কাজ করেনি।
 *
 * ⛔ **কোন লাইনে আটকাচ্ছে সেটা আন্দাজ করা হয়নি।** এই ছোট্ট ঘরটা প্রতিটা
 *    ধাপের নাম ফোনের নিজের ভিতরে লিখে রাখে, আর পর্দায় দেখায় — পরেরবার
 *    আটকালে **শেষ ধাপটাই পর্দায় থেকে যাবে**, তখন ঠিক জায়গাটা ধরে সারানো যাবে।
 *
 * ⛔ **সম্পূর্ণ নিরাপদ:** শুধু ফোনের নিজের SharedPreferences-এ ছোট একটা লেখা
 *    (রোগীর কোনো তথ্য নয়, টাকার কিছু নয়), ক্লাউডে কিচ্ছু যায় না, কোনো নিয়ম
 *    বা হিসাব বদলায় না। ব্যর্থ হলে চুপচাপ ছেড়ে দেয়।
 * ⛔ লেখা ইংরেজিতে (নিয়ম ৯) — স্টাফের পর্দায় বাংলা নয়।
 */
object OpenTrace {

    private const val PREFS = "open_trace"
    private const val KEY_LAST = "last_step"
    private const val KEY_DONE = "last_done"

    /** পর্দায় লেখাটা তখনই বদলে দেওয়ার জন্য (থাকলে)। */
    @Volatile var onStep: ((String) -> Unit)? = null

    @Volatile private var current: String = ""

    /** এখনকার ধাপ (পর্দায় দেখানোর জন্য)। */
    fun current(): String = current

    /** একটা ধাপ শুরু হলো। */
    fun step(ctx: Context?, name: String) {
        current = name
        try {
            ctx?.applicationContext
                ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ?.edit()?.putString(KEY_LAST, name)?.putBoolean(KEY_DONE, false)?.apply()
        } catch (_: Throwable) { }
        try { onStep?.invoke(name) } catch (_: Throwable) { }
    }

    /** পর্দা সফলভাবে খুলে গেছে — অর্থাৎ গতবারের অভিযোগ আর প্রযোজ্য নয়। */
    fun done(ctx: Context?) {
        current = ""
        try {
            ctx?.applicationContext
                ?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                ?.edit()?.putBoolean(KEY_DONE, true)?.apply()
        } catch (_: Throwable) { }
    }

    /** গতবার যদি খোলা শেষ না হয়ে থাকে, তাহলে কোন ধাপে থেমেছিল — নইলে ফাঁকা। */
    fun lastStuckStep(ctx: Context?): String = try {
        val p = ctx?.applicationContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (p == null || p.getBoolean(KEY_DONE, true)) "" else p.getString(KEY_LAST, "") ?: ""
    } catch (_: Throwable) { "" }
}
