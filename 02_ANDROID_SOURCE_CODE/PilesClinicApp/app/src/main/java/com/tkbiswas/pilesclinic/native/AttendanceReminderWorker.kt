package com.tkbiswas.pilesclinic.native

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tkbiswas.pilesclinic.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * B325/B326 — IN TIME (সকাল ১০টা থেকে) ও OUT TIME (সন্ধ্যা ৬টা থেকে)
 * রিমাইন্ডার — দুটোরই একই লজিক, শুধু kind ("in"/"out") আলাদা। শুধু Staff-এর
 * জন্য (Master/Doctor/Field-এর চেম্বার-উপস্থিতি প্রযোজ্য না)। আজকের IN/OUT
 * মার্ক হয়েছে কিনা — এই ফোনের স্থানীয় (SharedPreferences) ফ্ল্যাগ দেখে ঠিক
 * হয় (`WorkNotebookActivity.kt`-এর `markReminderFlag()` বসায়), কোনো নতুন
 * ক্লাউড-কল/সাইন-ইন লাগে না।
 *
 * ⚠️ সৎ সীমাবদ্ধতা (CallReminderWorker.kt-এও একই কথা): কিছু ফোনের
 * battery-optimization ব্যাকগ্রাউন্ড কাজ দেরি/বাদ দিতে পারে — এটা
 * best-effort reminder, ১০০% গ্যারান্টিড alarm না।
 */
class AttendanceReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val kind = inputData.getString("kind") ?: AttendanceReminderScheduler.KIND_IN
        val attempt = inputData.getInt("attempt", 1)
        try {
            val user = NativeSession.current(ctx)
            // 🧑‍⚕️🔒 V496 (২১.০৮.২০২৬, TK §২): **ডাক্তারের হাজিরা নেই।**
            // আগে শর্ত ছিল `user.role == "staff"` — কিন্তু `permissionRole()`
            // ডাক্তারকেও "staff" বানায় (NativeSession.kt:25), তাই **ডাক্তারদের
            // ফোনেও IN/OUT TIME-এর রিমাইন্ডার বাজত**। এখন আসল ভূমিকা দেখা হয়।
            // ⛔ Staff ও Field-এর রিমাইন্ডার হুবহু আগের মতোই।
            if (user != null && RoleRules.usesAttendance(user)) {
                val today = todayIso()
                val prefKey = if (kind == AttendanceReminderScheduler.KIND_IN) "checkin_or_leave_date" else "checkout_or_leave_date"
                val prefs = ctx.getSharedPreferences("wn_prefs", Context.MODE_PRIVATE)
                var markedDate = prefs.getString(prefKey, "")
                // 🔵🔒 (09.08.2026, TK-অনুমোদিত — "IN TIME দ্বিতীয়বার আসে" ফিক্স):
                // নতুন APK ইনস্টল করলে Android এই ফোনের স্থানীয় ফ্ল্যাগ (wn_prefs)
                // মুছে দেয় — তখন ক্লাউডে আজকের IN/OUT আগেই থাকলেও রিমাইন্ডার আবার আসত।
                // তাই স্থানীয় ফ্ল্যাগে "আজ মার্ক হয়নি" দেখালে, নোটিফিকেশন দেখানোর
                // **আগে একবার ক্লাউডে দেখে নিই** — আজকের IN/OUT আগেই থাকলে ফ্ল্যাগ
                // বসিয়ে চুপ থাকি (আর জ্বালাই না)। ⛔ সেভ/হাজিরা/টাকা কিছু ছোঁয়া হয়নি —
                // শুধু রিমাইন্ডার দেখানো-না-দেখানোর সিদ্ধান্ত। পড়া ব্যর্থ/লগইন না হলে
                // আগের আচরণই (নোটিফাই) — কখনো ভুল করে চুপ থাকে না।
                if (markedDate != today && cloudAlreadyMarked(ctx, user.name, kind, today)) {
                    prefs.edit().putString(prefKey, today).apply()
                    markedDate = today
                }
                // 🔵 B608 (10.08.2026, TK-নির্দেশ): IN TIME না হলে OUT TIME-এর কোনো মানে
                // নেই — তাই OUT রিমাইন্ডার পাঠানোর আগে দেখি আজ IN হয়েছে কিনা (এই ফোনের
                // স্থানীয় ফ্ল্যাগ; নতুন ইনস্টলে ফ্ল্যাগ মুছে গেলে ক্লাউডে একবার দেখে নিই)।
                // IN না হলে আজ OUT রিমাইন্ডার একদমই পাঠানো হয় না — কালকের চেইন চালু থাকে।
                if (kind == AttendanceReminderScheduler.KIND_OUT) {
                    val inDone = prefs.getString("checkin_or_leave_date", "") == today ||
                        cloudAlreadyMarked(ctx, user.name, AttendanceReminderScheduler.KIND_IN, today)
                    if (!inDone) {
                        AttendanceReminderScheduler.scheduleTomorrowFirstAttempt(ctx, kind)
                        return Result.success()
                    }
                }
                if (markedDate != today) {
                    notify(ctx, kind, attempt)
                    if (attempt < AttendanceReminderScheduler.MAX_ATTEMPTS) {
                        AttendanceReminderScheduler.scheduleRepeat(ctx, kind, attempt + 1)
                        return Result.success()
                    }
                }
            }
        } catch (_: Exception) {
            // never crash the worker
        }
        // মার্ক হয়ে গেছে, অথবা আজকের ৩ বার শেষ, অথবা কোনো ভুল হয়েছে —
        // সব ক্ষেত্রেই কালকের প্রথম স্লট দিয়ে চেইন চালু রাখা হয়।
        AttendanceReminderScheduler.scheduleTomorrowFirstAttempt(ctx, kind)
        return Result.success()
    }

    private fun todayIso(): String {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US); f.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return f.format(java.util.Date())
    }

    // 🔵🔒 (09.08.2026, TK-অনুমোদিত) — ক্লাউডে আজকের IN/OUT আগেই মার্ক করা আছে কিনা
    // দেখে। **শুধু নিশ্চিতভাবে থাকলে** true ফেরায় — লগইন না হলে / পড়া ব্যর্থ হলে /
    // সারি না পেলে false (তখন আগের মতোই রিমাইন্ডার যায়, কখনো ভুল করে চুপ থাকে না)।
    // সেভ-লজিকের প্রমাণিত গোপন-লগইন (signInCurrentSession) পুনর্ব্যবহার। staff_code =
    // WorkNotebook যেভাবে সেভ করে সেই user.name; শুধু একটা ঘর/একটা সারি পড়ে (egress নগণ্য)।
    private suspend fun cloudAlreadyMarked(ctx: Context, staff: String, kind: String, today: String): Boolean {
        if (staff.isBlank()) return false
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val ma = com.tkbiswas.pilesclinic.modules.ModuleAuth
                if (!ma.isSignedIn) { try { ma.signInCurrentSession(ctx) } catch (_: Throwable) {} }
                if (!ma.isSignedIn) return@withContext false
                val col = if (kind == AttendanceReminderScheduler.KIND_IN) "check_in" else "check_out"
                val res = ma.getRowsChecked(
                    "wn", "notebook_days",
                    "select=$col&staff_code=eq.$staff&work_date=eq.$today&limit=1"
                )
                if (!res.ok || res.rows.length() == 0) return@withContext false
                val v = res.rows.getJSONObject(0).optString(col, "")
                v.isNotBlank() && v != "null"
            }
        } catch (_: Throwable) { false }
    }

    private fun notify(ctx: Context, kind: String, attempt: Int) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = NoticeChannels.ensure(
            ctx, CHANNEL_ID, "Attendance Reminder", "Reminds you to mark IN TIME / OUT TIME in Work Notebook"
        )
        val open = Intent(ctx, com.tkbiswas.pilesclinic.modules.WorkNotebookActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // 🔴 B418 (04.08.2026, TK-নির্দেশ) — নোটিফিকেশনে চাপলে সরাসরি
            // IN/OUT TIME-এর ছোট প্রশ্ন দেখাবে, পুরো পাতা প্রথমে না।
            .putExtra("quick_mark", kind)
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        else android.app.PendingIntent.FLAG_UPDATE_CURRENT
        // 🔴 প্রতিটা attempt-এ requestCode আলাদা, নইলে ১০ মিনিট পরের একই
        // kind-এর নোটিফিকেশন আগেরটাকে PendingIntent-লেভেলে ওভাররাইট করে ফেলত।
        val pi = android.app.PendingIntent.getActivity(ctx, (kind + attempt).hashCode(), open, flags)

        // 🎨🔒 B519 (06.08.2026, TK-নির্দেশ, ছোট/সহজ বাংলা মকআপ অনুমোদনের
        // পরে — "যত পারেন শর্টকাট লিখুন, স্টাফরা বুঝবে কিনা") — আগের
        // লম্বা ইংরেজি বাক্যের বদলে এখন খুব ছোট, সরাসরি বাংলা (Kishanganj
        // স্টাফের জন্য `NoBengali.s()` আগের মতোই স্বয়ংক্রিয়ভাবে ইংরেজিতে
        // ফিরিয়ে দেয়, প্রজেক্টের প্রমাণিত নিয়মে)।
        val title = if (kind == AttendanceReminderScheduler.KIND_IN)
            NoBengali.s("⏰ চেম্বারে পৌঁছেছেন?") else NoBengali.s("⏰ চেম্বার থেকে বেরিয়েছেন?")
        val text = if (kind == AttendanceReminderScheduler.KIND_IN)
            NoBengali.s("চাপুন — IN TIME বসান।") else NoBengali.s("চাপুন — OUT TIME বসান।")

        val n = NotificationCompat.Builder(ctx, channel)
            // 🎨 TK-APPROVED (2026-08-06): clean bell icon + brand accent + BigText.
            .setSmallIcon(R.drawable.ic_notif_bell)
            .setColor(android.graphics.Color.parseColor("#0B3B73"))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        val notifId = if (kind == AttendanceReminderScheduler.KIND_IN) NOTIF_ID_IN else NOTIF_ID_OUT
        // 🔵 (07.08.2026, TK-রিপোর্ট — "একসাথে IN ও OUT দুটো নোটিফিকেশন কেন"):
        // সন্ধ্যায় OUT রিমাইন্ডার এলে সকালের (এখনো ট্রে-তে থেকে যাওয়া, না-চাপা)
        // IN রিমাইন্ডারটা সরিয়ে দেওয়া হয় — যাতে IN ও OUT কখনো একসাথে না দেখায়।
        // ⛔ IN-মার্ক/OUT-মার্কের সেভ-লজিক কিছু বদলায়নি, শুধু পুরনো নোটিশ মোছা।
        if (kind == AttendanceReminderScheduler.KIND_OUT) {
            try { nm.cancel(NOTIF_ID_IN) } catch (_: Throwable) {}
        }
        nm.notify(notifId, n)
    }

    companion object {
        const val CHANNEL_ID = "attendance_reminder"
        // 🔴🔴🔴 খাতার সারি [পরবর্তী] (TK-রিপোর্ট — প্রতিটা স্টাফ বলছেন IN TIME
        // রিমাইন্ডার কখনো আসেনি, গভীরে যাচাই করে আসল কারণ পাওয়া গেছে)।
        // **আসল কারণ:** নোটিফিকেশন ID সংঘর্ষ — এই দুটো ID (4202/4203)
        // `BellNotifier.NOTIF_ID` (4202) ও `ChamberCloseReminderWorker.NOTIF_ID`
        // (4203)-এর সাথে **হুবহু মিলে যেত**। Android-এ দুটো ভিন্ন নোটিফিকেশন
        // একই ID দিয়ে পাঠালে পরেরটা আগেরটাকে নিঃশব্দে মুছে/বদলে দেয় — তাই
        // Bell-এর নোটিশ বা Chamber-রিমাইন্ডার এলেই IN/OUT TIME রিমাইন্ডার
        // ফোনের notification tray থেকে হারিয়ে যেত, প্রতিটা স্টাফের ফোনেই।
        // **সমাধান:** এখন সম্পূর্ণ আলাদা, প্রজেক্টের কোথাও ব্যবহার-না-হওয়া
        // ID (4205/4206)। ⛔ Bell/Chamber-রিমাইন্ডারের নিজস্ব ID/লজিক এক
        // অক্ষরও বদলানো হয়নি — শুধু এই দুটো ID সরানো হলো।
        const val NOTIF_ID_IN = 4205
        const val NOTIF_ID_OUT = 4206
    }
}
