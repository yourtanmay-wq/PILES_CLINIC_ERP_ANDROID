package com.tkbiswas.pilesclinic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.tkbiswas.pilesclinic.native.AppLock
import com.tkbiswas.pilesclinic.native.NativeSession
import com.tkbiswas.pilesclinic.native.RoleRules
import com.tkbiswas.pilesclinic.native.SessionGuard
import com.tkbiswas.pilesclinic.native.SuspendedActivity

/**
 * 🔐🔒 V496 (২১.০৮.২০২৬, TK §১০ ও §১১) — **অ্যাপ সামনে এলে যা যা দেখা হয়।**
 *
 * `PilesClinicApplication`-এর `onActivityStarted` থেকে ঠিক একবার ডাকা হয়।
 * দুটো কাজ:
 *
 *  ১. **৭ দিনের হিসাব** — ফোনেই, ইন্টারনেট ছাড়াই। পেরিয়ে গেলে সঙ্গে সঙ্গে
 *     লগআউট, পিছনের কাজ বন্ধ, লগইন পর্দা।
 *  ২. **মাস্টার বন্ধ করেছেন কিনা** — নেটওয়ার্ক থ্রেডে, **১৫ মিনিটে একবারের
 *     বেশি নয়** (Supabase-এর খরচ যেন না বাড়ে, TK §১১)।
 *
 * ⛔ **কোনো তথ্য মোছা হয় না** — রোগী · হাজিরা · ছুটি · টাকা সব অটুট।
 * ⛔ মাস্টারের অ্যাকাউন্ট ছোঁয়া হয় না।
 * ⛔ লগইন পর্দায় থাকলে কিছুই করা হয় না (নইলে অসীম লুপ হত)।
 * ⛔ নেট না থাকলে কাউকে বার করে দেওয়া হয় না (fail-open) — নইলে সবার কাজ
 *    আটকে যেত। অফলাইনের এই সীমা নথিতে সৎভাবে লেখা আছে।
 */
object SessionGuardBridge {

    /** একই সঙ্গে দুবার যেন না চলে। */
    @Volatile private var busy = false

    /** যে পর্দাগুলোতে এই পাহারা চালানো হয় না। */
    private fun isExempt(activity: Activity): Boolean {
        val n = activity.javaClass.simpleName
        return n == "LoginActivity" || n == "PublicSiteActivity" || n == "SplashActivity"
    }

    fun onForeground(context: Context, activity: Activity) {
        if (isExempt(activity)) return
        val user = try { NativeSession.current(context) } catch (_: Throwable) { null } ?: return

        // 🟢🔒 V601 (২৪.০৮.২০২৬, TK-নির্দেশ) — ২৪ ঘণ্টায় একবার আঙুল/পাসওয়ার্ড,
        // Master-সহ সবার জন্য (V499-এর মূল নিয়ম) — তাই নিচের Master-ছাড়ের
        // আগেই। V527-এর "বারবার না" নিয়ম অক্ষত: ২৪ ঘণ্টার কমে কিছুই দেখাবে না।
        try { com.tkbiswas.pilesclinic.native.AppLock.guardDaily(activity, user) } catch (_: Throwable) { }

        /* ══════════════════════════════════════════════════════════════════
           🔴🔴🔒 V527 (২২.০৮.২০২৬, TK-এর স্পষ্ট নির্দেশ) — **অ্যাপ খোলার তালা
           তুলে দেওয়া হলো।**

           TK-এর কথা: *"বারবার Fingerprint আসবে না। যখন Login-এর ব্যাপার থাকবে
           তখন চাইবে। আর In Time-এর সময় চাইবে, পেমেন্ট নেওয়ার সময় চাইবে।
           বাকি কখনো যেন ফিঙ্গারপ্রিন্ট চায় না।"*

           এতদিন এখানে (`AppLock.guard`) **প্রতিবার অ্যাপ সামনে এলেই** আঙুল
           চাওয়া হত — V521-এ ১৫ মিনিটের ছাড় বসানো হয়েছিল, কিন্তু TK চান
           এটা **একেবারেই না থাকুক**।

           ⇒ এখন আঙুল চাওয়া হয় **ঠিক তিন জায়গায়, আর কোথাও নয়**:
               ১. **Login** — পাসওয়ার্ড অথবা আঙুল (আঙুলটা আগে)
               ২. **IN TIME (হাজিরা)** — `WorkNotebookActivity`, অপরিবর্তিত
               ৩. **টাকা** — Refund (এক টাকাতেও) ও বড় অঙ্কের পেমেন্ট

           ⛔ `AppLock.kt` **মোছা হয়নি** — শুধু আর ডাকা হয় না। TK পরে চাইলে
              এই একটা লাইন ফিরিয়ে দিলেই আবার চালু হবে।
           ⛔ নিচের ৭-দিন ও suspend-যাচাই এক অক্ষরও বদলায়নি।
           ⛔ ফোনের নিজের স্ক্রিন-লক আগের মতোই কাজ করে — সেটা অ্যাপের হাতে নয়।

           🔓 **সৎ কথা:** এতে অ্যাপ খোলার তালাটা আর থাকল না। ফোন অন্য কারো
              হাতে পড়লে সে অ্যাপ খুলে **দেখতে** পারবে। তবে **টাকা ফেরত দেওয়া
              বা বড় অঙ্ক নেওয়া আর হাজিরা দেওয়া** — এগুলো আঙুল ছাড়া হবে না।
              TK এটা জেনেই সিদ্ধান্ত নিয়েছেন।
           ══════════════════════════════════════════════════════════════════ */

        // নিচের ৭ দিন ও suspend-যাচাই আগের মতোই মাস্টারের জন্য নয়
        // (TK §১০-এর শেষ লাইন) — সেটা ছোঁয়া হয়নি।
        if (RoleRules.isMaster(user)) return

        // ── ১) ৭ দিন — ফোনেই, সঙ্গে সঙ্গে ─────────────────────────────────
        if (!SessionGuard.isWithinInactivityWindow(context)) {
            forceLogout(context, activity,
                "অনেকদিন অ্যাপ ব্যবহার হয়নি, তাই নিরাপত্তার জন্য লগআউট করা হলো। মোবাইল ও পাসওয়ার্ড দিয়ে আবার লগইন করুন।")
            return
        }
        // এই খোলাটাই নতুন "শেষবার ব্যবহার"
        SessionGuard.noteForeground(context)

        // ── ২) মাস্টার বন্ধ করেছেন কিনা — ১৫ মিনিটে একবার ─────────────────
        if (busy || !SessionGuard.suspendCheckDue(context)) return
        busy = true
        Thread {
            val check = try {
                SessionGuard.checkSuspensionNow(context)
            } catch (_: Throwable) {
                SessionGuard.Check(SessionGuard.Verdict.UNKNOWN)
            }
            busy = false
            if (check.verdict == SessionGuard.Verdict.BLOCKED) {
                Handler(Looper.getMainLooper()).post {
                    // 🔴🔒 V1649 (TK-নির্দেশ, "সম্পূর্ণ ডিসপ্লে জুড়ে") — আগে শুধু
                    // এক ঝলকের Toast + লগইন-পর্দা দেখাত, মিস হয়ে যেত। এখন
                    // আগে লগআউট (এক অক্ষরও বদলায়নি), তারপর পূর্ণ-পর্দা বার্তা।
                    try { SessionGuard.logoutLocal(context) } catch (_: Throwable) { }
                    try { AppLock.reset() } catch (_: Throwable) { }
                    try {
                        SuspendedActivity.launch(
                            activity, check.staffCode, check.until, check.removed, check.reason,
                            alreadyLoggedOut = true
                        )
                    } catch (_: Throwable) {
                        forceLogout(context, activity, check.message)   // ⛔ কিছু ভুল হলেও আগের প্রমাণিত পথ
                    }
                }
            }
        }.start()
    }

    /** লগআউট করে লগইন পর্দায় পাঠানো, একটা পরিষ্কার বার্তাসহ। */
    private fun forceLogout(context: Context, activity: Activity, message: String) {
        try { SessionGuard.logoutLocal(context) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.native.AppLock.reset() } catch (_: Throwable) { }
        try {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        } catch (_: Throwable) { }
        try {
            val i = Intent(context, com.tkbiswas.pilesclinic.native.LoginActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(i)
            activity.finish()
        } catch (_: Throwable) { }
    }
}
