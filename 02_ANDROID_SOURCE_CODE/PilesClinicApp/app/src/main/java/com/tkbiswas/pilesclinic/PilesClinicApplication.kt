package com.tkbiswas.pilesclinic

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tkbiswas.pilesclinic.data.sync.ConnectivityObserver
import com.tkbiswas.pilesclinic.data.sync.SyncScheduler
import com.tkbiswas.pilesclinic.security.AppSettings
import com.tkbiswas.pilesclinic.security.CrashHandler
import com.tkbiswas.pilesclinic.security.SessionTimeoutManager

/**
 * Phase 5 added background offline-first sync scheduling.
 * Phase 9 adds, without touching any existing screen's code:
 *   - Crash Protection (logs uncaught exceptions before the OS handles them)
 *   - Session Management (auto-expires the Supabase session after inactivity,
 *     checked on every Activity resume via ActivityLifecycleCallbacks)
 *   - respects the "Auto Sync" App Setting instead of always-on
 */
class PilesClinicApplication : Application() {

    // 🔐 V496 (TK §১১): লেখার আগে "বন্ধ করা হয়েছে কিনা" দেখতে অ্যাপের
    // Context দরকার, অথচ `SupabaseClient`-এর লেখা-ফাংশনগুলোতে Context যায় না।
    // তাই এখানে একবারই ধরে রাখা হয় (Application-এর Context, কোনো Activity নয়,
    // তাই মেমরি লিকের ঝুঁকি নেই)।
    companion object {
        @Volatile @JvmStatic var appContext: android.content.Context? = null
            private set
    }

    /** এখন কটা পর্দা দৃশ্যমান — ০ হলে অ্যাপ পুরোপুরি পিছনে (V496)। */
    private var visibleScreens: Int = 0

    private lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        /* 🔴🔒 V721 (২৭.০৮.২০২৬) — ফোনে কল এলে/মেমরি কম পড়লে Android অ্যাপের
           প্রসেস বন্ধ করে দেয়; পরে পর্দা আবার খুললেও **রোগীর তথ্য (নাম ·
           ব্রাঞ্চ · ঠিকানা …) মেমরি থেকে মুছে যেত** — তখন ছাপা কাগজে সব `"-"`
           আর ভুল ব্রাঞ্চের হেডার আসত। এখানে একবারই ফিরিয়ে আনা হয়।
           ⛔ মেমরিতে রোগী থাকলে কিছুই করে না · ৩০ মিনিটের বেশি পুরোনো হলে
              ফেরানো হয় না · কোনো ক্লাউড-কল নেই (RoleSession.kt দ্রষ্টব্য)। */
        try { com.tkbiswas.pilesclinic.clinical.RoleSession.restoreIfEmpty() } catch (_: Throwable) { }

        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(this, Thread.getDefaultUncaughtExceptionHandler())
        )

        // 🚨 TK'S ORDER (27.07.2026): "স্টাফ অ্যাপ্লিকেশন থেকে বেরিয়ে গেলেও যেন
        // তার ফোন থেকে ডেটা Supabase-এ চলে যায় — অ্যাপের বাইরে থাকলেও automatic
        // sync হবে।"
        //
        // WHAT WAS WRONG: the background upload (every 15 minutes, and again the
        // moment the line comes back) was switched on ONLY if the "Auto Sync"
        // setting happened to be ON. If anyone ever turned that setting off --
        // or it got written as off on a phone -- then an unsent enquiry,
        // registration, bill, advance or payment was retried only when a screen
        // was opened. Leaving the app stopped every retry, and that phone's
        // entries stayed invisible to everyone else until it was reopened.
        //
        // A staff setting must never be able to hold the clinic's own records
        // hostage on one handset, so the pending-queue upload is now always
        // scheduled. (The setting still controls nothing else that is lost --
        // this is only the retry of saves the staff already made.) Guarded so a
        // scheduling failure can never stop the app from starting.
        try { SyncScheduler.schedulePeriodic(this) } catch (_: Throwable) {}
        try {
            connectivityObserver = ConnectivityObserver(this)
            connectivityObserver.start()
        } catch (_: Throwable) {}

        // TK-REQUESTED (2026-07-26): remembers deleted row ids so a stuck
        // retry queue can never push a deleted record back into the cloud.
        // Guarded: a failure here must never stop the app from starting.
        // 🔒 TK-ORDER (30.07.2026 সকাল ১১.১০ · খাতার সারি B158): কিশানগঞ্জের
        // স্টাফ KNE-KISHAN5 বাংলা পড়তে পারেন না — তাঁর লগইনে অ্যাপের পর্দায়
        // একটাও বাংলা অক্ষর থাকবে না (শুধু ইংরেজি/হিন্দি)। এটা **এক জায়গায়**
        // বসানো হলো, তাই ৩৪টা পর্দার একটাতেও আলাদা কোড লাগেনি এবং ভবিষ্যতের
        // নতুন পর্দাও নিজে থেকেই ঢেকে যাবে।
        // ⛔ অন্য কোনো স্টাফের ফোনে কিছুই বদলায় না (TK-এর সিদ্ধান্ত D30)।
        // Guarded: এখানে কিছু ভুল হলেও অ্যাপ চালু হতে বাধা পাবে না।
        try { com.tkbiswas.pilesclinic.native.NoBengali.hookApp(this) } catch (_: Throwable) {}

        // 🔵🔒 V418 (TK-নির্দেশ, ১৭.০৮.২০২৬: *"এখানে অটো সাজেশ কেন থাকবে"*) —
        // "Add Collection"-এর Cash ঘরে চাপ দিলে ফোনের নিজের Autofill পুরনো
        // মোবাইল নম্বর সাজেশন দিচ্ছিল (টাকার ঘরে নম্বর বসে যাওয়ার ঝুঁকি)।
        // উপরের NoBengali-র মতোই **এক জায়গায়** বন্ধ করা হলো, তাই ৩৪টা পর্দার
        // একটাতেও আলাদা কোড লাগেনি — নতুন পর্দাও নিজে থেকেই ঢাকা পড়বে।
        // ⛔ কীবোর্ড/টাইপ/সেভ কিছুই বদলায় না। Guarded — ভুল হলেও অ্যাপ চালু হবে।
        try { com.tkbiswas.pilesclinic.native.NoAutofill.hookApp(this) } catch (_: Throwable) {}

        // 🔴🔒 V429 — ক্লাউডে ডাক পাঠানোর গোপন লগইনটা যেন **প্রথম ডাকেও** কাজ
        //    করে, তাই অ্যাপ চালু হওয়ার সময়েই context ধরে রাখা হয়। এটা না থাকায়
        //    চেম্বারের RMP কমিশন চুপচাপ ফাঁকা দেখাচ্ছিল। Guarded — ভুল হলেও
        //    অ্যাপ চালু হতে বাধা পাবে না।
        try { com.tkbiswas.pilesclinic.modules.ModuleAuth.attachContext(this) } catch (_: Throwable) {}

        try { com.tkbiswas.pilesclinic.native.DeletedGuard.init(this) } catch (_: Throwable) {}

        // Daily "আজ X কল বাকি" reminder at 10 AM / 12 PM / 2 PM.
        // Guarded so a scheduling failure can never crash app startup.
        // 🚨 TK'S ORDER (2026-07-28): the safety net that keeps a failed cloud
        // write and sends it again later needs to know the app, so it is given
        // that here, before anything else can write.
        try { com.tkbiswas.pilesclinic.native.CloudWriteQueue.attach(this) } catch (_: Throwable) {}
        // V331: keep medicine Type/Dose/When/Days available after reinstall or
        // on another clinic phone. Local cache keeps every screen instant/offline.
        try {
            com.tkbiswas.pilesclinic.clinical.ClinicalRepository.attachDoseMemory(this)
            com.tkbiswas.pilesclinic.clinical.MedicineDefaultsCloudRepository.refreshIfNeeded(this)
        } catch (_: Throwable) {}
        try {
            com.tkbiswas.pilesclinic.native.CallReminderScheduler.scheduleNext(this)
            // TK'S RULE (2026-07-27): from 7 PM, every 10 minutes until
            // midnight, a staff whose chamber is still open is reminded.
            com.tkbiswas.pilesclinic.native.ChamberCloseReminderScheduler.scheduleNext(this)
            // 🔔🔒 V1437 (১৩.০৯.২০২৬, TK-নির্দেশ · সমস্যা-তালিকা ৫৬১) — উপরের
            // তাগাদাটা শুধু **আজকের** দিনের, রাত ১২টায় থেমে যায়। পুরনো যে
            // দিনগুলোর চেম্বার বন্ধ হয়নি সেগুলোর জন্য রোজ **বেলা ১১টায়**
            // স্টাফকে একবার মনে করানো। ⛔ শুধু স্টাফের ফোনে (কাজটা নিজেই
            // ভিতরে দেখে নেয়), ⛔ বাকি কিছু না থাকলে কোনো নোটিফিকেশনই নয়,
            // ⛔ উপরের সন্ধের চেইন একটুও ছোঁয়া হয়নি — আলাদা স্বাধীন চেইন।
            com.tkbiswas.pilesclinic.native.ChamberBacklogReminderScheduler.scheduleNext(this)
            // 🔴 B325/B326 (03.08.2026, TK-নির্দেশ): সকাল ১০টা থেকে IN TIME ও
            // সন্ধ্যা ৬টা থেকে OUT TIME এখনো মার্ক না হলে Staff-এর ফোনে
            // নোটিফিকেশন (১০ মিনিট পরপর, সর্বোচ্চ ৩ বার) — CallReminderScheduler-এর
            // একই প্রমাণিত WorkManager-chain প্যাটার্ন, exact-alarm অনুমতি লাগে না।
            com.tkbiswas.pilesclinic.native.AttendanceReminderScheduler.scheduleFreshDay(this)
            // 🔴🔴 V511 (২১.০৮.২০২৬, TK-নির্দেশ) — রাত ৯টায় মাস্টারকে জানানো
            //   আজ কারা OUT TIME দেননি। ⛔ কাজটা নিজেই ভিতরে দেখে নেয় এটা
            //   মাস্টারের ফোন কিনা; স্টাফ/ডাক্তারের ফোনে কিছুই হয় না।
            //   ⛔ কেউ বাকি না থাকলে কোনো নোটিফিকেশনই আসে না।
            com.tkbiswas.pilesclinic.native.MasterOutTimeScheduler.scheduleNext(this)
            // 🛰️🔒 V1345 (১১.০৯.২০২৬, TK-নির্দেশ) — দুপুর ৩টায় মাস্টারকে জানানো
            //   কোন ফিল্ড-স্টাফের লোকেশন আজ ৪৫ মিনিটেরও বেশি সময় ধরে
            //   হালনাগাদ হচ্ছে না। ⛔ MasterOutTimeScheduler-এর থেকে সম্পূর্ণ
            //   আলাদা, স্বাধীন চেইন — উপরেরটা ছোঁয়া হয়নি।
            com.tkbiswas.pilesclinic.native.FieldVisitAlertScheduler.scheduleNext(this)
            // 🆕 B467 (05.08.2026, TK-নির্দেশ) — অপঠিত Briefing/Notice থাকলে
            // ১০ মিনিট পরপর জোরপূর্বক নোটিফিকেশন (একই প্রমাণিত chain-প্যাটার্ন)।
            com.tkbiswas.pilesclinic.native.BriefingReminderScheduler.start(this)
            // 🆕 (07.08.2026, TK-অনুমোদিত) — কাল যাদের চেম্বারে আসার কথা, একদিন
            // আগে সন্ধ্যা ৫টায় ব্রাঞ্চ-স্টাফকে একবার মনে করিয়ে দেয় (মাস্টারকে নয়)।
            // একই প্রমাণিত WorkManager-chain প্যাটার্ন; দিনে একবারই বাজে।
            com.tkbiswas.pilesclinic.native.ExpectedTomorrowReminderScheduler.scheduleNext(this)
            // 🟢🔒🔒 V656 (২৫.০৮.২০২৬, TK-নির্দেশ) — Doctor Note & Reminder:
            // ডাক্তার Doctor Checkup-এর History পাতায় ভবিষ্যতের একটা নোট +
            // তারিখ বসালে, সেই তারিখের আগের দিন সন্ধ্যা ৫টায় শুধু ডাক্তারকেই
            // একবার মনে করিয়ে দেওয়া হয়। একই প্রমাণিত WorkManager-chain
            // প্যাটার্ন; দিনে একবারই বাজে। ⛔ ভিতরে নিজেই দেখে নেয় role
            // "doctor" কিনা — স্টাফ/মাস্টারের ফোনে কিছুই হয় না।
            com.tkbiswas.pilesclinic.native.DoctorReminderScheduler.scheduleNext(this)
            // 🚨 TK'S ORDER (2026-07-27): "অ্যাপ্লিকেশন বন্ধ থাকলেও... ইন্টারনেট অন
            // থাকলেই ব্যাকগ্রাউন্ডে লোডিং ও আপডেটের কাজ চলতে থাকবে।"
            // The old background job only PUSHED unsent records up; nothing ever
            // pulled the Follow-up lists down, so every tab had to download from
            // scratch the moment it was opened. This keeps the same lists ready
            // on the phone in the background, so the screen opens at once.
            // Runs only with a connection, and skips itself if the lists were
            // already refreshed less than 45 minutes ago.
            // 🚨 TK'S ORDER (28.07.2026, খাতার সারি B41): *"অ্যাপ্লিকেশন বন্ধ থাকলো
            // অথবা অন্য কোনো অ্যাপ্লিকেশনে থাকলো — আমাদের ফোনে যদি নেট কানেক্ট
            // থাকে তাহলে যেন অটোমেটিক আপডেট নিয়ে নেয়... নির্দিষ্ট টাইমের মধ্যে
            // যেন অটোমেটিক রিফ্রেশ হয়ে যায়, কিন্তু বাফারিং ছাড়া।"*
            //
            // আগে ছিল **১ ঘণ্টা** পরপর। এখন **১৫ মিনিট** — Android এর চেয়ে কম
            // সময়ে ব্যাকগ্রাউন্ডের কাজ চালাতেই দেয় না, তাই এটাই সবচেয়ে ঘন।
            //
            // ⛔ `UPDATE` (আগে ছিল `KEEP`) — নইলে যে ফোনে অ্যাপ আগে থেকেই বসানো
            // আছে সেখানে **পুরনো ১ ঘণ্টার নিয়মটাই চিরকাল থেকে যেত**, নতুন নিয়ম
            // কোনোদিন চালু হত না।
            //
            // নেট না থাকলে চলে না, আর সবটাই চুপচাপ — কোনো চাকতি বা লেখা নয়।
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "piles_clinic_background_refresh",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                // 🔴🔴 V509 (২১.০৮.২০২৬) — **এখানে হাত দেওয়া হয়নি, ইচ্ছে করেই।**
                //
                // Egress কমানোর সময় প্রথমে এটাকে ১৫ মিনিট → ১ ঘণ্টা করা হয়েছিল।
                // পরে কোড ধরে যাচাই করে দেখা গেল **সেটা ভুল সিদ্ধান্ত**, তাই ফিরিয়ে
                // আনা হলো। কারণ দুটো, দুটোই কোড থেকে প্রমাণিত:
                //
                //  ১) ভারী কাজটা এমনিতেই ঘণ্টায় একবারের বেশি চলে না —
                //     `BackgroundRefreshWorker.MIN_GAP_MS` **আগে থেকেই ৬০ মিনিট**
                //     (ঐ ফাইলের ২৮৪ নম্বর লাইন, আর পাহারাটা ৫৮ নম্বর লাইনে, সব
                //     ভারী পড়ার **আগে**)। অর্থাৎ ১৫ মিনিটে একবার ডাকা হলেও
                //     চারবারের তিনবার সঙ্গে সঙ্গেই ফিরে যায়, কিছু নামায় না।
                //     ⇒ ১ ঘণ্টা করলে Egress প্রায় **কিছুই** বাঁচত না।
                //
                //  ২) কিন্তু **ক্ষতি হত।** ঐ ৬০-মিনিটের পাহারার **উপরে**
                //     (`doWork()`-এর ৪৯ নম্বর লাইন) `flushEverythingWaiting()`
                //     চলে — এই ফোনে আটকে থাকা পেমেন্ট/এন্ট্রি ক্লাউডে পাঠানোর
                //     একমাত্র নিয়মিত রাস্তা। ১ ঘণ্টা করলে সেটাও ৪ গুণ ধীর হয়ে
                //     যেত — TK-এর "আটকে থাকা পেমেন্ট" সমস্যাটা আরও খারাপ হত।
                //
                // ⇒ Egress-এর আসল ফুটোটা এখানে ছিল না। সেটা ধরা পড়েছে
                //   `DashboardActivity.refreshCallBanner()`-এ (হোম পর্দায় ফিরলেই
                //   তিনটে পূর্ণ Follow-up ট্যাব নামত) — সেখানেই ঠিক করা হয়েছে।
                androidx.work.PeriodicWorkRequestBuilder<com.tkbiswas.pilesclinic.native.BackgroundRefreshWorker>(
                    15, java.util.concurrent.TimeUnit.MINUTES
                ).setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                ).build()
            )
            // অ্যাপ চালু হওয়ামাত্র একবার — যাতে নতুন ফোনে বা অনেকক্ষণ পরে খুললেও
            // পরের ১৫ মিনিটের জন্য অপেক্ষা করতে না হয়। এটাও পিছনে, চুপচাপ।
            androidx.work.WorkManager.getInstance(this).enqueueUniqueWork(
                "piles_clinic_background_refresh_now",
                androidx.work.ExistingWorkPolicy.KEEP,
                androidx.work.OneTimeWorkRequestBuilder<com.tkbiswas.pilesclinic.native.BackgroundRefreshWorker>()
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                            .build()
                    ).build()
            )
        } catch (_: Throwable) {}

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                SessionTimeoutManager.enforceTimeoutIfNeeded(applicationContext)
            }

            override fun onActivityPaused(activity: Activity) {
                SessionTimeoutManager.recordActivityNow(applicationContext)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            // 🔐🔒 V496 (২১.০৮.২০২৬, TK §১০ ও §১১) — **এটাই একমাত্র জায়গা যেখানে
            // "ব্যবহারকারী নিজে অ্যাপ সামনে এনেছেন" ধরা হয়।**
            //
            // `onActivityStarted` কেবল তখনই ডাকা হয় যখন একটা পর্দা সত্যিই
            // দৃশ্যমান হয় — অর্থাৎ মানুষ অ্যাপ খুলেছেন। পিছনের কাজ
            // (Worker · Realtime · নোটিফিকেশন · সিঙ্ক) কখনো Activity চালু
            // করে না, তাই সেগুলো **৭ দিনের হিসাব একটুও পিছিয়ে দিতে পারে না**
            // — TK-এর §৬ ও §১০-এর শর্ত এখানেই রক্ষা হয়।
            //
            // ⛔ পুরনো `SessionTimeoutManager` (security/) ছোঁয়া হয়নি — ওর
            //    auto-logout আগে থেকেই ইচ্ছে করে বন্ধ (ফাইলের নোট দেখুন),
            //    সে শুধু সময় লিখে রাখে। তাই দুটো নিয়মের বিরোধ নেই।
            override fun onActivityStarted(activity: Activity) {
                visibleScreens += 1
                try { SessionGuardBridge.onForeground(applicationContext, activity) } catch (_: Throwable) { }
                /* 🏍️🔒 V1364 (১১.০৯.২০২৬, পুরো-প্রজেক্ট দেরি-অডিট, তালিকা ৪৬২-ঝ) —
                   ফিল্ড ভিজিটের GPS-সেবা OEM-এর ব্যাটারি-ব্যবস্থাপনায় মরে গেলে
                   আগে শুধু `Work Notebook` পর্দা খুললে তবেই আবার চালু হত।
                   এখন **মানুষ নিজে অ্যাপের যেকোনো পর্দা খুললেই** (এই একমাত্র
                   জায়গা, উপরের নোট দ্রষ্টব্য) একই যাচাই চলে — সেটাই
                   `startForegroundService()`-এর জন্য সবচেয়ে নিরাপদ ও পূর্ণ-
                   অনুমতির মুহূর্ত। ⛔ হাজিরার কোনো লজিক ছোঁয়া হয়নি। */
                try { com.tkbiswas.pilesclinic.native.FieldVisitControl.resumeIfNeeded(applicationContext) } catch (_: Throwable) { }
            }
            // 🩺 V496 (TK §৩): অ্যাপ **পুরোপুরি** পিছনে গেলে ডাক্তারের তালা আবার লাগে।
            // এক পর্দা থেকে অন্য পর্দায় গেলে লাগে না (নইলে কাজ করা যেত না) —
            // তাই দৃশ্যমান পর্দার সংখ্যা গোনা হয়।
            override fun onActivityStopped(activity: Activity) {
                visibleScreens = (visibleScreens - 1).coerceAtLeast(0)
                if (visibleScreens == 0) {
                    try { com.tkbiswas.pilesclinic.native.AppLock.onAppBackgrounded() } catch (_: Throwable) { }
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
