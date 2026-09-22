package com.tkbiswas.pilesclinic.native

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 🟢🔒 V619 (২৪.০৮.২০২৬, TK-রিপোর্ট — "ভুল নম্বর হলে Cancel থাকতে হত") —
 * `CallNotifyManager`-এর নোটিফিকেশনের "✕ Dismiss" বোতাম চাপলে এখানে আসে,
 * শুধু ওই একটা নোটিফিকেশন সরিয়ে দেয়। আর কিছু ছোঁয় না — কল/অ্যাপের কোনো
 * অবস্থা বদলায় না, শুধু পর্দা থেকে সরে যায়।
 */
class CallDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(CallNotifyManager.NOTIF_ID)
        } catch (_: Throwable) { }
    }
}
