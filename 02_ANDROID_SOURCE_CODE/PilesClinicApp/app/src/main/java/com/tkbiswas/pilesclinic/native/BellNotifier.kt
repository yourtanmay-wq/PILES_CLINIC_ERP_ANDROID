package com.tkbiswas.pilesclinic.native

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tkbiswas.pilesclinic.R

/**
 * TK-REQUESTED (2026-07-27): the bell number used to change silently, so a new
 * notice or a pending approval could sit there unnoticed. Now, whenever the
 * number goes UP, the phone posts a notification with its normal notification
 * sound (same behaviour as the existing "Today's Pending Calls" reminder).
 *
 * Rules kept deliberately simple and safe:
 *  - it only speaks when the number is HIGHER than the last time it spoke, so
 *    the same notice never rings twice, and a number going down is silent;
 *  - the last number is remembered per staff mobile, so two people sharing a
 *    phone never see each other's alert;
 *  - it never shows what the notice says (privacy) -- only that something new
 *    arrived; tapping opens the Briefing screen, exactly where the bell goes.
 *  - any failure is swallowed: the bell itself must keep working.
 */
object BellNotifier {

    private const val CHANNEL_ID = "clinic_notices"
    private const val NOTIF_ID = 4202
    private const val PREF = "bell_notify_state"

    fun onCount(ctx: Context, session: NativeUser, count: Int) {
        try {
            val key = "last_" + session.mobile.filter { it.isDigit() }.takeLast(10)
            val sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val last = sp.getInt(key, 0)
            if (count > last) notify(ctx, count, count - last)
            sp.edit().putInt(key, count).apply()
        } catch (_: Exception) {}
    }

    private fun notify(ctx: Context, total: Int, fresh: Int) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 🔊 খাতার সারি B28 — শব্দ চ্যানেল থেকেই আসে; বিস্তারিত NoticeChannels-এ।
            val channel = NoticeChannels.ensure(
                ctx, CHANNEL_ID, "Clinic Notices", "New notices and pending approvals"
            )
            val open = Intent(ctx, BriefingActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            val pi = android.app.PendingIntent.getActivity(ctx, 1, open, flags)

            val n = NotificationCompat.Builder(ctx, channel)
                // 🎨 TK-APPROVED (2026-08-06): clean bell icon + brand accent + BigText.
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setColor(android.graphics.Color.parseColor("#0B3B73"))
                .setContentTitle("🔔 $fresh new notice")
                .setContentText("$total waiting in the bell — tap to open.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$total waiting in the bell — tap to open."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            nm.notify(NOTIF_ID, n)
        } catch (_: Exception) {}
    }
}
