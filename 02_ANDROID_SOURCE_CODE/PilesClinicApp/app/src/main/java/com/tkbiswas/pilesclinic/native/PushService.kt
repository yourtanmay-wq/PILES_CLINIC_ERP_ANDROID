package com.tkbiswas.pilesclinic.native

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tkbiswas.pilesclinic.R

/**
 * 🔔 V1298 (১০.০৯.২০২৬, তালিকা ৪১৪, TK: *"SMS-এর মতো — নোটিফিকেশন আসামাত্র একবার সাউন্ড"*)
 *
 * Google-এর push (FCM)। নোটিশ ডেটাবেসে ঢোকামাত্র সার্ভার (Netlify function) এই ফোনে
 * বার্তা পাঠায়; অ্যাপ বন্ধ থাকলেও Android নিজে নোটিফিকেশন দেখায় — চ্যানেল
 * `clinic_notices_v2` (শব্দ + কাঁপুনি, NoticeChannels)। অ্যাপ খোলা থাকলে এই ক্লাসের
 * onMessageReceived নিজে দেখায় — একই চ্যানেলে, একবার।
 *
 * ⛔ আগের ১৫-মিনিটের BellNotifier যেমন ছিল তেমনই থাকে (push না পৌঁছলে সেটাই জাল)।
 * ⛔ Firebase চালু না থাকলে (google-services.json নেই) কিছুই ভাঙে না — service ডাকা হয় না।
 */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        try { PushTokenSync.register(applicationContext, token) } catch (_: Throwable) { }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val n = message.notification
            val title = n?.title ?: message.data["title"] ?: "🔔 New notice"
            val body = n?.body ?: message.data["body"] ?: "Tap to open the Notice Board."
            show(applicationContext, title, body, message.data["id"] ?: "")
        } catch (_: Throwable) { }
    }

    companion object {
        private const val CHANNEL_ID = "clinic_notices"   // NoticeChannels.ensure → "clinic_notices_v2"

        fun show(ctx: Context, title: String, body: String, briefingId: String) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NoticeChannels.ensure(ctx, CHANNEL_ID, "Clinic Notices", "New notices and pending approvals")
            val open = Intent(ctx, BriefingActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
            val pi = PendingIntent.getActivity(ctx, 7, open, flags)
            val notif = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setColor(android.graphics.Color.parseColor("#0B3B73"))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            // একই নোটিশ দুবার এলে একটাই থাকে (id ধরে)
            nm.notify(("push_" + briefingId).hashCode(), notif)
        }
    }
}
