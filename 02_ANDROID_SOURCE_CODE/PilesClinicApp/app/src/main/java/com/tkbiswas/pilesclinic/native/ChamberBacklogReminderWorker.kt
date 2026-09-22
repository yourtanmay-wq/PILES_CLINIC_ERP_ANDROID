package com.tkbiswas.pilesclinic.native

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.R

/**
 * 🔔🔒 V1437 (১৩.০৯.২০২৬, TK-নির্দেশ · সমস্যা-তালিকা সারি ৫৬১) —
 * **পুরনো যে দিনগুলোর চেম্বার বন্ধ করা হয়নি, রোজ বেলা ১১টায় স্টাফকে একবার
 * মনে করিয়ে দেওয়া।**
 *
 * ─── কেন দরকার হলো ───────────────────────────────────────────────────────
 * সন্ধের তাগাদাটা (`ChamberCloseReminderWorker`) শুধু **ওই দিনের** ৭টা–১২টা
 * চলে। রাত ১২টা পেরোলে ওই দিনটা আর কোনোদিন কাউকে ডাকত না — মাস্টারের ☰
 * মেনুর লাল ব্যাজ ছাড়া কোথাও কিছু দেখাত না, আর ওটা স্টাফ দেখতেই পান না।
 * তাই জলপাইগুড়িতে ১২ দিন জমে গিয়েছিল (TK-র ছবি)।
 *
 * ─── যা করে ───────────────────────────────────────────────────────────────
 * রোজ বেলা ১১টায় (একবার) নিজের ব্রাঞ্চের গত ৩০ দিন দেখে — যেদিন **রোগী
 * এসেছেন বা টাকা উঠেছে অথচ চেম্বার বন্ধ হয়নি**, তেমন দিন বাকি থাকলে একটা
 * নোটিফিকেশন। চাপলে সোজা "CHAMBER CLOSE" পর্দা — ওখান থেকে প্রতিটা দিন
 * পুরনো সেই একই নিয়মে (রিভিউ · Confirm & Print) বন্ধ করা যায়।
 *
 * ─── ⛔ নিরাপত্তা · খরচ · TK-র পুরনো নিয়ম ─────────────────────────────────
 *  • **শুধু স্টাফের ফোনে** (`role == "staff"`), নিজের ব্রাঞ্চের দিনই — মাস্টার
 *    বা ডাক্তারের ফোনে একটাও নোটিফিকেশন যায় না (TK-র সিদ্ধান্ত, ১৩.০৯.২০২৬)।
 *  • **কেউ না এলে ও টাকা না উঠলে ওই দিন নিয়ে কিচ্ছু বলা হয় না** — সন্ধের
 *    তাগাদার হুবহু একই নিয়ম, `ChamberUnclosedRepository` নিজেই সেটা মানে।
 *  • বাকি কিছু না থাকলে **একদম চুপ**, আর আগের নোটিফিকেশনটাও সরিয়ে দেওয়া হয়।
 *  • পড়া **দিনে একবার**, মাত্র দুটো অনুরোধ, আর টাকার সারিগুলো এবার
 *    **সার্ভারেই নিজের ব্রাঞ্চে ছাঁকা** (V1437) — তাই পাঁচ ব্রাঞ্চের বদলে
 *    এক ব্রাঞ্চের সারি নামে। ⛔ কিছুই লেখা হয় না — শুধু পড়া।
 *  • মেনুর "Chamber Close" ঘরটা **আগের মতোই মাস্টার-only** (TK-র সিদ্ধান্ত) —
 *    স্টাফ এই নোটিফিকেশনে চেপেই পর্দাটায় ঢোকেন; পর্দাটা আগে থেকেই স্টাফের
 *    জন্য নিজের ব্রাঞ্চ ধরে তৈরি (ChamberCloseActivity — ব্রাঞ্চ-পিল থাকে না)।
 *  • সন্ধে ৭টা–১২টার পুরনো চেইন এক অক্ষরও ছোঁয়া হয়নি — আলাদা কাজ, আলাদা
 *    চ্যানেল, আলাদা নোটিফিকেশন আইডি, তাই দুটো একে অপরকে মোছে না।
 *
 * ⚠️ সৎ সীমা (অন্য সব Worker-এও একই কথা): কিছু ফোনের battery-optimization
 *    ব্যাকগ্রাউন্ড কাজ দেরি করাতে/বাদ দিতে পারে — এটা best-effort, ১০০%
 *    গ্যারান্টিড অ্যালার্ম নয়। ⛔ কম্পিউটারে (ওয়েব) ব্যাকগ্রাউন্ড কাজ কখনোই
 *    ছিল না, তাই এই তাগাদা Android-only — মাস্টারের ওয়েব পর্দা অপরিবর্তিত।
 */
class ChamberBacklogReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        try {
            val user = NativeSession.current(ctx)
            if (user != null && user.role == "staff") {
                val branch = user.branch.trim()
                if (branch.isNotBlank() && !branch.equals("All", ignoreCase = true)) {
                    // এই ফোন থেকেই বন্ধ করা হয়েছে অথচ ক্লাউডে পৌঁছায়নি — সেটা
                    // আগে পাঠানো হয়, নইলে বন্ধ-করা দিনও "বাকি" দেখাত।
                    // (সন্ধের তাগাদাটাও ঠিক এটাই করে।)
                    try { ChamberCloseRepository.flushPending(ctx) } catch (_: Throwable) { }
                    val days = ChamberUnclosedRepository.findUnclosed(
                        ctx, branch, LOOK_BACK_DAYS, serverBranchFilter = true
                    )
                    // `null` = পড়া ব্যর্থ (নেট নেই ইত্যাদি) — তখন চুপ থাকা হয়,
                    // ⛔ "সব বন্ধ আছে" ধরে নেওয়া হয় না, ভুল সতর্কতাও যায় না।
                    if (days != null) {
                        if (days.isEmpty()) clear(ctx) else notify(ctx, days)
                    }
                }
            }
        } catch (_: Throwable) {
            // একটা তাগাদা কখনো কিছু ভাঙবে না
        } finally {
            // ⛔ ফল যাই হোক, কালকের ১১টার স্লট আবার বসিয়ে চেইন চালু রাখা হয়।
            try { ChamberBacklogReminderScheduler.scheduleNext(applicationContext) } catch (_: Throwable) { }
        }
        return Result.success()
    }

    /** বাকি কিছু নেই — আগের নোটিফিকেশনটা পড়ে থাকলে সরিয়ে দেওয়া হয়। */
    private fun clear(ctx: Context) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)
        } catch (_: Throwable) { }
    }

    private fun notify(ctx: Context, days: List<ChamberUnclosedRepository.UnclosedDay>) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // 🔊 খাতার সারি B28 — একই কারণ, একই সমাধান (NoticeChannels দেখুন)।
            val channel = NoticeChannels.ensure(
                ctx, CHANNEL_ID, "Old chambers not closed",
                "Reminds the staff about earlier days whose chamber was never closed"
            )
            // 🇬🇧 নিয়ম ৯ — স্টাফের পর্দার সব লেখা ইংরেজিতে।
            val oldest = days.minByOrNull { it.date }?.date
            val line = buildString {
                append("${days.size} day(s) still open")
                if (oldest != null) append(" — oldest ${DateUtil.display(oldest)}")
                append(". Tap to open Chamber Close and finish them.")
            }
            val open = Intent(ctx, ChamberCloseActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            val pi = android.app.PendingIntent.getActivity(ctx, REQ_CODE, open, flags)

            // 🎨 সন্ধের তাগাদার হুবহু একই চেহারা (ঘণ্টা-আইকন + brand accent + BigText)।
            val n = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setColor(android.graphics.Color.parseColor("#0B3B73"))
                .setContentTitle("📋 Earlier chambers not closed (${days.size})")
                .setContentText(line)
                .setStyle(NotificationCompat.BigTextStyle().bigText(line))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
            nm.notify(NOTIF_ID, n)
        } catch (_: Throwable) { }
    }

    companion object {
        private const val CHANNEL_ID = "chamber_backlog_not_closed"

        /** ⛔ সন্ধের তাগাদার আইডি (4203) থেকে আলাদা — দুটো একে অপরকে মোছে না। */
        private const val NOTIF_ID = 4204
        private const val REQ_CODE = 4204

        /** মাস্টারের পর্দাটাও ঠিক এক মাস পিছনে দেখে (ChamberCloseActivity) — একই মাপ। */
        private const val LOOK_BACK_DAYS = 30
    }
}
