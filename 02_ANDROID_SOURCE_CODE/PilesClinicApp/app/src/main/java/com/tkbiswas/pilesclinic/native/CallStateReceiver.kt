package com.tkbiswas.pilesclinic.native

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

/**
 * 🟢🔒 V605 (২৪.০৮.২০২৬, TK-নির্দেশ) — ফোনের অবস্থা (বাজছে/কথা হচ্ছে/
 * শেষ) শোনে, `CallNotifyManager`-কে জানায়। আসল কল করা/ধরা/কাটার সাথে
 * এই ফাইলটার কোনো সম্পর্ক নেই — শুধু **শোনে**।
 *
 * ⛔ শুধু আগে-থেকে-থাকা READ_PHONE_STATE অনুমতি ব্যবহার করে (B441-এ যোগ
 *    হয়েছিল, দুই-SIM আলাদা করতে) — নতুন কোনো অনুমতি চাওয়া হয়নি।
 * ⚠️ সৎ সীমাবদ্ধতা: Android-ভেদে (বিশেষ করে নতুন Android-এ কিছু ফোনের
 *    নিজস্ব ব্যাটারি-সাশ্রয় নিয়মে) এই ব্রডকাস্ট মাঝে মাঝে দেরিতে/না-ও
 *    আসতে পারে — এটা এই অ্যাপের কোডের সীমাবদ্ধতা নয়, Android-এর নিজের।
 *    ব্যর্থ হলে (নোটিফিকেশন না এলে) বাকি কল-লগ/রিমার্কস-লেখা (পরে,
 *    কল-লগ থেকে) ঠিকই কাজ করে — এটা শুধু "লাইভ" অংশটুকু।
 */
class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
                    if (number.isNotBlank()) CallNotifyManager.onRinging(context.applicationContext, number)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    CallNotifyManager.onOffhook(context.applicationContext)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    CallNotifyManager.onIdle(context.applicationContext)
                }
            }
        } catch (_: Throwable) {
            // এই রিসিভার কখনো কল/অ্যাপ আটকাতে পারবে না।
        }
    }
}
