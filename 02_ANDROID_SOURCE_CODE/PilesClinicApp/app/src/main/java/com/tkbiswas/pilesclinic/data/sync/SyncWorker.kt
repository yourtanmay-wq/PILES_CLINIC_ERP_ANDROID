package com.tkbiswas.pilesclinic.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.data.local.AppDatabase
import com.tkbiswas.pilesclinic.data.repository.SyncManager
import com.tkbiswas.pilesclinic.data.session.SessionManager

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // TK-REQUESTED (2026-07-24): app বন্ধ থাকলেও pending Enquiry/
        // Registration/Payment/Follow-up/Chamber/Clinical/Briefing/other
        // edits যেন sync হয় -- এই Worker আগে থেকেই প্রতি ১৫ মিনিটে ও নেট
        // ফিরলে চলত (SyncScheduler/ConnectivityObserver), কিন্তু শুধু পুরনো
        // SyncManager ডাকত, যেটা এখনকার আসল pending-queue রিপোজিটরিগুলো
        // (BottomNav.wire() যেগুলো চালায়) ছুঁতোই না। এখন সেই একই, আগে থেকে
        // নিরাপদ প্রমাণিত flushPending() ফাংশনগুলো এখানেও ডাকা হচ্ছে --
        // BottomNav-এর লজিক অপরিবর্তিত, শুধু app বন্ধ অবস্থাতেও একই কাজ
        // এখন চলবে। প্রতিটা try-catch আলাদা যাতে একটা ব্যর্থ হলে বাকিগুলো
        // থেমে না যায় (BottomNav.wire()-এর মতোই)।
        val ctx = applicationContext
        // 🔒 খাতার সারি B169 (TK-এর ৬ নম্বর সন্দেহ, 30.07.2026): WorkManager-এর
        // এই কাজটাও আগে অন্যদের তালার কথা জানত না, তাই পর্দা-খোলার দফা বা
        // পিছনের কাজের সঙ্গে **একই সময়ে** চলতে পারত। এখন একটাই দরজা।
        com.tkbiswas.pilesclinic.native.SyncGate.tryRun {
        // 🚨🚨 খাতার সারি B170 (TK-এর ৭ নম্বর সন্দেহ, 30.07.2026): *"কিছু worker
        // কেন্দ্রীয় pending তালিকাও পরীক্ষা করে না।"* — এই worker-টাই ছিল সেই
        // "কিছু worker": এটা `CloudWriteQueue` (ওষুধ বিক্রি · বিল সংশোধন ·
        // অনুমোদন · Trash restore · ডাক্তার এন্ট্রি · পাসওয়ার্ড) আর Chamber
        // Close-এর নিজের তালিকা — কোনোটাই ছুঁতো না। তাই App বন্ধ থাকলে ওই দুই
        // জায়গায় আটকে থাকা কাজ **এই worker দিয়ে কখনো যেত না**।
        try { com.tkbiswas.pilesclinic.native.CloudWriteQueue.attach(ctx) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.CloudWriteQueue.flush(ctx) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.ChamberCloseRepository.flushPending(ctx) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.EnquiryRepository(ctx).flushPending() } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.RegistrationRepository(ctx).flushPending() } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.PaymentRepository(ctx).flushPending() } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.FollowUpRepository(ctx).flushPending() } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.ChamberAttendanceRepository.flushPending(ctx) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.clinical.ClinicalCloudRepository.flushPending(ctx) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.BriefingRepository().flushPending(ctx) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.GenericUpdateQueue.flushPending(ctx) } catch (_: Throwable) { }
        }   // 🔒 খাতার সারি B169 — দরজা শেষ

        // ── TK-APPROVED TECHNICAL FIX (2026-07-26, V132) ──────────────────
        // WHAT WAS HERE: the OLD, first-generation sync -- SyncManager
        // .syncAll() over the Room database -- ran right after the eight
        // flushPending() calls above, on this same schedule: every 15 minutes
        // AND again after every single save (six repositories call
        // SyncScheduler.syncNow() on every write).
        //
        // WHY IT IS SWITCHED OFF (verified by a full reference search across
        // the project before touching anything):
        //   * SyncManager is referenced from exactly ONE place -- this file.
        //     No Activity, Adapter or Repository uses it.
        //   * NOTHING in the live app writes to the Room tables any more --
        //     every screen goes through LocalWorkflowStore + SupabaseClient --
        //     so its PUSH half always had zero rows to push.
        //   * Its PULL half still downloaded FOUR complete tables (enquiries,
        //     registrations, followups, payments) from Supabase into Room on
        //     every run, and nothing ever read those rows back. Pure Supabase
        //     free-plan quota, mobile data and battery burn, repeated after
        //     every save on all five branches' phones -- and it made the app
        //     feel heavy right after each save.
        //   * SessionManager is only ever filled by AuthRepository, which this
        //     app's own login never calls, so syncAll() also reported errors
        //     forever, which made this Worker return Result.retry() in a loop.
        //   * SyncStatusHolder / SyncState, which syncAll() updated, are not
        //     displayed on any screen -- so switching it off changes NOTHING
        //     the user can see.
        //   * Settings' "Backup Now" does not depend on it either: the real
        //     backup (exportCloudJson + CSV) reads straight from Supabase.
        //
        // NOTHING DELETED: SyncManager, AppDatabase, the DAOs and SyncState
        // all remain in the project, simply no longer driven from here, so
        // this is reversible by restoring this one file.
        //
        // RETRY IS NOT WEAKENED -- it is now more accurate. Previously the
        // retry that kept the pending queues moving was an accident of
        // syncAll() failing. Now we ask the queues themselves: if anything is
        // still waiting, tell WorkManager to come back; if everything went
        // through, report success. Read-only checks, no writes.
        val stillPending = try {
            val prefsNames = listOf(
                "piles_clinic_enquiry_pending",
                "piles_clinic_registration_pending",
                "piles_clinic_payment_pending",
                "piles_clinic_followup_pending",
                "piles_clinic_followup_heal_pending",
                "piles_clinic_chamber_pending",
                "piles_clinic_medical_pending",
                "piles_clinic_briefing_pending",
                "piles_clinic_generic_update_pending"
            )
            prefsNames.any { name ->
                val raw = ctx.getSharedPreferences(name, android.content.Context.MODE_PRIVATE)
                    .getString("queue", "[]") ?: "[]"
                org.json.JSONArray(raw).length() > 0
            } ||
                // RegistrationRepository keeps a SECOND queue under its own
                // "closeQueue" key (the "close the source Enquiry after
                // Registration" step). Counted too, so a Registration that is
                // only half-finished still keeps this Worker coming back.
                (org.json.JSONArray(
                    ctx.getSharedPreferences("piles_clinic_registration_pending", android.content.Context.MODE_PRIVATE)
                        .getString("closeQueue", "[]") ?: "[]"
                ).length() > 0) ||
                // 🚨 খাতার সারি B170: কেন্দ্রীয় তালিকা ও Chamber Close-এর
                // তালিকা আটকে থাকলেও এখন WorkManager আবার আসার কথা মনে রাখে।
                (try { com.tkbiswas.pilesclinic.native.CloudWriteQueue.pendingCount(ctx) > 0 } catch (_: Throwable) { false }) ||
                (try { com.tkbiswas.pilesclinic.native.ChamberCloseRepository.pendingCount(ctx) > 0 } catch (_: Throwable) { false })
        } catch (_: Throwable) { false }

        return if (stillPending) Result.retry() else Result.success()
    }
}
