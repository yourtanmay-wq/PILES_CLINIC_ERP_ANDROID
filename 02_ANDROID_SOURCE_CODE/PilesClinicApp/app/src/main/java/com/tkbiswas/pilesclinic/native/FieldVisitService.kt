package com.tkbiswas.pilesclinic.native

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tkbiswas.pilesclinic.R

/**
 * 🏍️🔒 V968 (০২.০৯.২০২৬, TK-নির্দেশ) — **ফিল্ড ভিজিটের GPS চালু রাখা।**
 *
 * IN TIME হলেই এই সেবা automatic চালু হয়, OUT TIME-এ (বা রাত ১২টায়)
 * বন্ধ। RUPAM/ARMAN-এর distance+route এবং অন্য attendance staff-এর last
 * location একই foreground service থেকে আসে। Android-এর নিয়মে **স্থায়ী একটা নোটিফিকেশন** দেখাতেই হয় — TK-কে
 * কাজ শুরুর আগেই এটা জানানো হয়েছে, লুকানোর কোনো উপায় নেই।
 *
 * ⛔ ব্যাকগ্রাউন্ড-লোকেশনের অনুমতি চাওয়া হয়নি; সেবা চালু থাকা অবস্থাতেই
 *    (foreground service) অবস্থান নেওয়া হয় — এটাই Play-এর নিরাপদ পথ।
 * ⛔ পুরো পথের দাগ কোথাও পাঠানো হয় না — শুধু মোট কিমি ও শেষ অবস্থান।
 */
class FieldVisitService : Service() {

    private var lm: LocationManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastPushMs = 0L
    private var lastRemindMs = 0L

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            FieldVisit.onLocation(this@FieldVisitService, location)
            updateNotice()
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val tick = object : Runnable {
        override fun run() {
            try {
                // রাত ১২টা পেরিয়ে গেছে ⇒ অ্যাপ নিজেই বন্ধ করে দেয় (TK-নির্দেশ)।
                if (FieldVisit.pastMidnight(this@FieldVisitService)) {
                    FieldVisit.endDay(this@FieldVisitService, auto = true)
                    Thread { FieldVisit.push(this@FieldVisitService, ended = true, auto = true) }.start()
                    stopSelf()
                    return
                }
                val now = System.currentTimeMillis()
                if (now - lastPushMs >= PUSH_EVERY_MS) {
                    lastPushMs = now
                    Thread { FieldVisit.push(this@FieldVisitService, ended = false, auto = false) }.start()
                }
                // রাত ৯টার পর বারবার মনে করানো (TK-নির্দেশ)।
                if (FieldVisit.pastReminderHour() && now - lastRemindMs >= REMIND_EVERY_MS) {
                    lastRemindMs = now
                    remindOutTime()
                }
                /* 🛰️🔒 V1156 — আগে অবস্থান নেওয়ার ব্যবস্থা **একবারই** বসত
                   (সেবা চালু হওয়ার সময়)। তখন অনুমতি না থাকলে বা Location
                   সুইচ বন্ধ থাকলে চুপচাপ ফিরে যেত, আর পরে অনুমতি দিলে/সুইচ
                   চালু করলেও সারাদিনে আর একবারও চেষ্টা হত না ⇒ কিলোমিটার
                   চিরকাল ০.০। ⇒ এখন প্রতি মিনিটে আবার চেষ্টা হয়, বসে গেলে
                   আর নয়। ⛔ বসে যাওয়ার পর বাড়তি কোনো কাজ হয় না। */
                if (!registered) startUpdates()
                updateNotice()
            } catch (_: Throwable) { }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FieldVisit.isRunning(this)) { stopSelf(); return START_NOT_STICKY }
        /* 🔴🔒 V1432 (১৩.০৯.২০২৬, স্টাফের ক্র্যাশ-ছবি, তালিকা ৫৪৮) — Android 14-এ location-ধরনের
           foreground service `startForeground` করার সময় Location অনুমতি না থাকলে (বা ব্যাকগ্রাউন্ড
           থেকে চালু হলে) SecurityException ছোড়ে — আগে এটা ধরা হতো না ⇒ পুরো অ্যাপ ক্র্যাশ
           ("keeps stopping")। এখন: অনুমতি নেই ⇒ চুপচাপ নিজেই বন্ধ; তবু কোনো কারণে ছুড়লে ধরে
           নিজেই বন্ধ — অ্যাপ কখনো ক্র্যাশ করবে না, হাজিরা (IN/OUT) অটুট, শুধু কিমি গোনা হবে না। */
        if (!hasPermission()) { stopSelf(); return START_NOT_STICKY }
        try {
            startForeground(NOTIF_ID, notice())
        } catch (_: Throwable) {
            stopSelf(); return START_NOT_STICKY
        }
        startUpdates()
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        try { handler.removeCallbacks(tick) } catch (_: Throwable) { }
        try { lm?.removeUpdates(listener) } catch (_: Throwable) { }
        registered = false
        super.onDestroy()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** 🛰️ V1156 — অবস্থান নেওয়া সত্যিই বসেছে কি না। না বসলে প্রতি মিনিটে আবার। */
    private var registered = false

    private fun startUpdates() {
        if (!hasPermission()) return
        try {
            lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var any = false
            for (p in providers) {
                try {
                    if (lm?.isProviderEnabled(p) == true) {
                        lm?.requestLocationUpdates(p, MIN_TIME_MS, MIN_DIST_M, listener, Looper.getMainLooper())
                        any = true
                        /* 🛰️ V1156 — শেষ জানা অবস্থানটা দিয়েই গোনার শুরুর বিন্দু
                           বসে যায়, নইলে প্রথম সত্যিকারের মাপ আসা পর্যন্ত (কখনো
                           কয়েক মিনিট) যতটা পথ যাওয়া হয় সেটা হারিয়ে যেত।
                           ⛔ এটা শুধু শুরুর বিন্দু — দূরত্ব এতে বাড়ে না। */
                        try {
                            val last = lm?.getLastKnownLocation(p)
                            /* 📍🔒 V1516 — পুরনো/stale last-known location-কে
                               "এখনকার" বলে save করা যাবে না। শুধু সাম্প্রতিক
                               fix seed হিসেবে নেওয়া হয়; নইলে live fix-এর অপেক্ষা। */
                            if (last != null && last.time > 0L) {
                                val age = System.currentTimeMillis() - last.time
                                if (age in 0..LAST_KNOWN_MAX_AGE_MS) FieldVisit.onLocation(this, last)
                            }
                        } catch (_: Throwable) { }
                    }
                } catch (_: Throwable) { }
            }
            registered = any
        } catch (_: Throwable) { }
    }

    private fun noticeText(): String {
        val km = FieldVisit.kmText(FieldVisit.distanceMeters(this))
        val hrs = FieldVisit.hoursText(FieldVisit.startedAt(this), System.currentTimeMillis())
        return "$hrs  ·  $km"
    }

    private fun notice(): android.app.Notification {
        val channel = NoticeChannels.ensure(
            this, CHANNEL_ID, "Field Visit",
            "Shows while a field visit is being counted"
        )
        val open = Intent(this, com.tkbiswas.pilesclinic.modules.WorkNotebookActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val piFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        val pi = android.app.PendingIntent.getActivity(this, 9681, open, piFlags)
        return NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setColor(android.graphics.Color.parseColor("#0B3B73"))
            .setContentTitle("Field Visit running")
            .setContentText(noticeText())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
    }

    private fun updateNotice() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NOTIF_ID, notice())
        } catch (_: Throwable) { }
    }

    private fun remindOutTime() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channel = NoticeChannels.ensure(
                this, CHANNEL_REMIND, "Field Visit Reminder",
                "Reminds you to mark OUT TIME after 9 PM"
            )
            val open = Intent(this, com.tkbiswas.pilesclinic.modules.WorkNotebookActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("quick_mark", "out")
            val piFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            val pi = android.app.PendingIntent.getActivity(this, 9682, open, piFlags)
            val text = "Mark OUT TIME now - the app will close it by itself at 12:00 AM."
            val n = NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setColor(android.graphics.Color.parseColor("#B42318"))
                .setContentTitle("Field visit still running")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            nm.notify(NOTIF_ID_REMIND, n)
        } catch (_: Throwable) { }
    }

    internal companion object {
        const val CHANNEL_ID = "piles_field_visit"
        private const val CHANNEL_REMIND = "piles_field_visit_remind"
        private const val NOTIF_ID = 9681
        private const val NOTIF_ID_REMIND = 9682
        /* 🗺️🔒 V1500 (১৫.০৯.২০২৬, TK-নির্দেশ — "লাইভ ম্যাপ") — আগে ৩ মিনিটে
           একবার ক্লাউডে পাঠানো হত, তাতে অ্যাপের ভিতরের নতুন লাইভ মানচিত্র
           (FieldVisitActivity.kt) পুরনো বিন্দু দেখাত। এখন ৩০ সেকেন্ডে
           একবার — Master-এর পর্দায় প্রায় সত্যিকারের-লাইভ মনে হবে। ⛔ IN/OUT
           TIME, কিলোমিটার-গোনার নিয়ম কিছুই বদলায়নি, শুধু কতবার পাঠানো হয়। */
        private const val MIN_TIME_MS = 30_000L      // ৩০ সেকেন্ডে একবারের বেশি নয়
        private const val LAST_KNOWN_MAX_AGE_MS = 10 * 60_000L  // V1516: stale point নয়
        private const val MIN_DIST_M = 25f           // ২৫ মিটারের কম সরলে ডাকে না
        private const val TICK_MS = 30_000L
        private const val PUSH_EVERY_MS = 30_000L    // ৩০ সেকেন্ডে একবার ক্লাউডে
        const val REMIND_EVERY_MS = 1_800_000L // রাত ৯টার পর ৩০ মিনিটে একবার
    }
}

/**
 * 🏍️ V968 — সেবাটা চালু/বন্ধ করার একটাই দরজা।
 * ⛔ ইচ্ছে করে আলাদা `object` (ক্লাসের `companion object`-এ নয়) — তাতে
 *    প্রজেক্টের পাহারা [৯.২৮] পুরো-নামে ডাকা এই দুটো ফাংশন খুঁজে পায়,
 *    আর ভবিষ্যতে নাম ভুল লিখলে সঙ্গে সঙ্গে ধরা পড়ে।
 */
object FieldVisitControl {

    /* 🛰️🔒 V1353 (১১.০৯.২০২৬, TK-রিপোর্ট — RUPAM-এর ফোনে "RUNNING · Location
       on" দেখাচ্ছে, কিন্তু নোটিফিকেশনই নেই, একাধিক দিন ধরে দূরত্ব চিরকাল ০.০)।
       **আসল কারণ (কোডে মিলিয়ে পাওয়া):** আগে এখানে `catch (_: Throwable) { }`
       ছিল — Android 12+-এ `startForegroundService()` মাঝেমধ্যে সময়ের কারণে
       ব্যর্থ হতে পারে (`ForegroundServiceStartNotAllowedException`, বিশেষত
       কিছু ফোনের নিজস্ব ব্যাটারি-ব্যবস্থাপনায়), আর এই ব্যর্থতা সম্পূর্ণ
       নিঃশব্দে হারিয়ে যেত — সেবা কখনো শুরুই হত না, কোনো নোটিফিকেশনও আসত না,
       অথচ ফোনের নিজের `isRunning()` চিহ্ন (attendance-এর জন্য, আলাদা) ঠিকই
       "RUNNING" বলে যেত। ⇒ প্রথম চেষ্টা ব্যর্থ হলে ২ সেকেন্ড পর **একবার
       আবার** চেষ্টা করা হয় (এই ধরনের বাধা প্রায়ই সাময়িক/সময়ের-দোষ) — এখনো
       ব্যর্থ হলেও অ্যাপ ক্র্যাশ করে না, আগের মতোই নিঃশব্দ, কিন্তু অন্তত
       একবার সত্যিকারের দ্বিতীয় সুযোগ পায়। */
    fun start(context: Context) {
        if (tryStart(context)) return
        val appCtx = context.applicationContext
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            tryStart(appCtx)
        }, 2000L)
    }

    private fun tryStart(context: Context): Boolean {
        // 🔴 V1432 — Location অনুমতি ছাড়া সেবা চালুই করা হবে না (Android 14-এ startForeground ছোড়ে)।
        // অনুমতি পেলে WorkNotebookActivity-র permission-callback নিজেই start() ডাকে (আগের নিয়ম)।
        if (!FieldVisit.hasLocationPermission(context)) return false
        return try {
            val i = Intent(context, FieldVisitService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= 26)
                context.startForegroundService(i) else context.startService(i)
            true
        } catch (_: Throwable) { false }
    }

    fun stop(context: Context) {
        try { context.stopService(Intent(context, FieldVisitService::class.java)) } catch (_: Throwable) { }
    }

    /* 🏍️🔒 V1364 (১১.০৯.২০২৬, পুরো-প্রজেক্ট দেরি-অডিট, তালিকা ৪৬২-ঝ) — TK:
       "ফিল্ড ভিজিট সংক্রান্ত দেরি"। আগে সেবাটা OEM-এর ব্যাটারি-ব্যবস্থাপনায়
       মরে গেলে, স্টাফ `Work Notebook` পর্দাটাই আবার না খুলতেন ততক্ষণ কেউ
       আবার চালু করত না (RUPAM-এর ৩ দিন ধরে ০.০ কিমি — V1353 শুধু "প্রথমবার
       চালু করার" ব্যর্থতা সারিয়েছিল, "পরে মরে যাওয়া"-র জন্য কিছু ছিল না)।
       ⇒ এই একই যাচাই (আগে শুধু `WorkNotebookActivity.resumeFieldVisitIfNeeded()`
       -এ ছিল, এখন এখানে সরানো) এখন **যেকোনো পর্দা খোলার সময়ই** চলে
       (`PilesClinicApplication.onActivityStarted` — মানুষ নিজে অ্যাপ খুলেছেন
       এমন মুহূর্তেই, তাই `startForegroundService()`-এর পূর্ণ অনুমতি থাকে)।
       ⛔ হাজিরার (IN/OUT TIME) কোনো লজিক ছোঁয়া হয়নি — শুধু GPS-সেবা বেঁচে
          আছে কিনা তার যাচাই। ⛔ ব্যর্থ হলে (পুরনো আচরণের মতোই) নিঃশব্দে বাদ।
       ⚠️ সৎ সীমা: অনেক OEM Android 12+-এ **পুরো ব্যাকগ্রাউন্ড** থেকে
          `startForegroundService()` আটকে দেয়; এই যাচাই তাই কোনো Worker/
          Alarm থেকে নয়, শুধু মানুষ নিজে পর্দা খুললে চলে — সেটাই সবচেয়ে
          বেশি সুযোগ দেয়, ১০০% নিশ্চয়তা কোনো ফোনেই দেওয়া সম্ভব নয়। */
    fun resumeIfNeeded(context: Context) {
        try {
            val fv = FieldVisit
            if (!fv.tracksAttendanceLocation(context) || !fv.isRunning(context)) return
            if (fv.pastMidnight(context)) {
                fv.endDay(context, auto = true)
                stop(context)
                val ctx = context.applicationContext
                Thread { fv.push(ctx, ended = true, auto = true) }.start()
                return
            }
            start(context)
        } catch (_: Throwable) { }
    }
}
