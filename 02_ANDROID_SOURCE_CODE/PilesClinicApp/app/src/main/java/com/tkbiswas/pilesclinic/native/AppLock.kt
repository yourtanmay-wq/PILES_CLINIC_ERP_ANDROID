package com.tkbiswas.pilesclinic.native

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity

/**
 * 🔒 V499 (২১.০৮.২০২৬, TK-এর সিদ্ধান্ত) — **অ্যাপ খোলার তালা।**
 *
 * ─── TK-এর নিয়ম (হুবহু) ───────────────────────────────────────────────────
 *  > *"একদম সিম্পল রুল — ফিঙ্গারপ্রিন্ট খুলবে, আবার পাসওয়ার্ড দিলেও খুলবে।
 *  >  যেটা বর্তমান যুগের সব ফোনেই একই রকম ব্যবস্থা আছে… ফিঙ্গারপ্রিন্টে
 *  >  কাজ না করলে সে পাসওয়ার্ড দিতে পারবে সেরকম ব্যবস্থা রাখুন।"*
 *  > *"এটা শুধু মাস্টারের ক্ষেত্রে নয় — যারা যারা ব্যবহার করে প্রত্যেকের
 *  >  ক্ষেত্রেই একই নিয়ম।"*
 *
 * ⇒ **Master · Doctor · Staff · Field — সবার জন্য একই।**
 * ⇒ Android-এর নিজের চেনা পর্দাটাই খোলে (আঙুল না মিললে নিচে "Use PIN /
 *   Password")। অ্যাপ আলাদা কোনো পাসওয়ার্ড রাখে না, দেখেও না।
 *
 * ─── কখন তালা লাগে ────────────────────────────────────────────────────────
 * অ্যাপ পুরোপুরি পিছনে চলে গেলে (সব পর্দা বন্ধ) তালা লাগে; পরের বার সামনে
 * আনলে চাইবে। অ্যাপের ভিতরে এক পর্দা থেকে আরেক পর্দায় গেলে চায় না।
 *
 * ─── ⚠️ যে ফোনে স্ক্রিন-লকই নেই ───────────────────────────────────────────
 * ফোনে না আছে আঙুল, না আছে PIN/প্যাটার্ন — তখন Android-এর কাছে চাওয়ার
 * মতো কিছুই নেই। তখন **অ্যাপ খোলে, কিন্তু চুপচাপ নয়** — প্রতিবার একটা
 * স্পষ্ট বার্তা দেখায়, আর ফোনের Settings খুলে দেওয়ার বোতাম থাকে।
 * ⛔ এখানে আটকে দিলে ব্যবহারকারী **চিরতরে বাইরে** থেকে যেতেন, কারণ ঢোকার
 *    কোনো পথই থাকত না — সেটা তালা নয়, ফাঁদ হত।
 *
 * ─── ⛔ হাজিরার সঙ্গে গুলিয়ে ফেলবেন না ────────────────────────────────────
 * এই তালা **শুধু অ্যাপ খোলার**। হাজিরা (IN TIME) বসে একমাত্র
 * `wn.mark_check_in()` দিয়ে — সার্ভার নিজে সময় ও ভূমিকা ঠিক করে।
 * V500 থেকে হাজিরার পর্দাতেও **একই নিয়ম** (আঙুল অথবা ফোনের পাসওয়ার্ড),
 * TK-এর স্পষ্ট সিদ্ধান্তে। তবে হাজিরায় বাড়তি একটা শর্ত আছে —
 * **ক্লিনিকে উপস্থিত থাকতে হবে** (`ClinicPresence`, GPS)।
 *
 * ─── 🔓 সৎ সীমাবদ্ধতা ─────────────────────────────────────────────────────
 *  • ফোনের PIN যে জানে, সে অ্যাপও খুলতে পারবে — এটাই TK-এর চাওয়া নিয়ম।
 *  • তালাটা এই অ্যাপের ভিতরে; অ্যাপ মুছে দিলে তালাও যায়।
 */
object AppLock {

    /* 🟢🔒 V601 (২৪.০৮.২০২৬, TK-স্পষ্ট নির্দেশ) — **২৪ ঘণ্টায় একবার তালা,
       V527-এর সিদ্ধান্তের আংশিক সংশোধন।**

       V527-এ (২২.০৮.২০২৬) TK বলেছিলেন "বারবার Fingerprint আসবে না... বাকি
       কখনো যেন ফিঙ্গারপ্রিন্ট চায় না" — তখন অ্যাপ-খোলার তালা সম্পূর্ণ বন্ধ
       করা হয়েছিল (নিচের guard()/onAppBackgrounded() আর ডাকা হয় না)।

       আজ (২৪.০৮.২০২৬) TK নতুন নির্দেশ দিয়েছেন: *"২৪ ঘণ্টা একবার ফিঙ্গারপ্রিন্ট
       চাইবে"* — V527-এর নিয়মের **আংশিক** সংশোধন, নিচের পুরনো ১৫-মিনিট-গ্রেস
       ব্যবস্থাটা ফেরানো হয়নি (সেটা অন্য জিনিস — বারবার ব্যাকগ্রাউন্ড হলে),
       বরং সম্পূর্ণ নতুন, সরল একটা টাইমার:

         · শেষ সফল আনলকের সময় ফোনের SharedPreferences-এ জমা থাকে (process
           বা ফোন রিস্টার্ট হলেও টেকে — TK "দিনে একবার" বলেছেন, প্রতি
           app-open-এ নয়)।
         · ২৪ ঘণ্টা না পেরোলে **কিছুই দেখায় না** — TK-এর "বারবার না" নিয়মটা
           এখনো মানা হচ্ছে।
         · ২৪ ঘণ্টা পেরোলে ঠিক একবার — আঙুল অথবা ফোনের পাসওয়ার্ড (V499-এর
           প্রমাণিত `BiometricGate` পুনর্ব্যবহার, নতুন কিছু বানানো হয়নি)।
         · সফল হলে সময়টা আবার নতুন করে বসে — পরের ২৪ ঘণ্টা শুরু।
         · স্ক্রিন-লকই নেই এমন ফোনে (V499-এর একই নিয়ম) আটকানো হয় না —
           জানিয়ে দিয়ে অ্যাপ খোলে, নইলে চিরতরে বাইরে থেকে যেতেন।
         · Master-সহ **সবার জন্য একই** (V499-এর মূল নিয়ম অক্ষত)।
       ⛔ পুরনো `guard()`/`onAppBackgrounded()`/১৫-মিনিট-গ্রেস কিছুই বদলায়নি,
          এখনো ডাকা হয় না — এটা সম্পূর্ণ আলাদা, নতুন পথ। */
    private const val DAILY_PREFS = "piles_clinic_app_lock_daily"
    private const val KEY_LAST_UNLOCK = "lastUnlockAt"
    private const val DAILY_MS = 24L * 60L * 60L * 1000L

    @Volatile private var dailyAsking = false

    /** ফোনে জমানো শেষ সফল আনলকের সময় (epoch ms), না থাকলে ০। */
    private fun lastUnlockAt(context: android.content.Context): Long =
        context.getSharedPreferences(DAILY_PREFS, android.content.Context.MODE_PRIVATE).getLong(KEY_LAST_UNLOCK, 0L)

    private fun markUnlockedNow(context: android.content.Context) {
        context.getSharedPreferences(DAILY_PREFS, android.content.Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_UNLOCK, System.currentTimeMillis()).apply()
    }

    /** সদ্য মোবাইল+পাসওয়ার্ড দিয়ে লগইন করলে ২৪-ঘণ্টার ঘড়ি সেই মুহূর্ত থেকেই
     *  শুরু হয় — নইলে লগইনের সাথে সাথেই আবার আঙুল চাইত, যেটা TK-এর
     *  "বারবার না" নিয়মের বিরুদ্ধে যেত। `NativeSession.save()` থেকে ডাকা হয়। */
    fun recordLoginUnlock(context: android.content.Context) {
        try { markUnlockedNow(context) } catch (_: Throwable) { }
    }

    /** লগআউটের সময় — পরের জনের জন্য অবশ্যই আবার চাইবে (V521-এর reset()-এর
     *  মতোই যুক্তি, কিন্তু এই আলাদা দৈনিক-টাইমারের জন্য)। */
    fun resetDaily(context: android.content.Context) {
        try {
            context.getSharedPreferences(DAILY_PREFS, android.content.Context.MODE_PRIVATE).edit()
                .putLong(KEY_LAST_UNLOCK, 0L).apply()
        } catch (_: Throwable) { }
    }

    /** ২৪ ঘণ্টা পেরিয়ে থাকলে ঠিক একবার আঙুল/পাসওয়ার্ড চায়; নইলে চুপচাপ ফেরে। */
    fun guardDaily(activity: Activity, user: NativeUser?) {
        if (user == null) return
        val act = activity as? AppCompatActivity ?: return
        if (dailyAsking) return
        val last = lastUnlockAt(act)
        val now = System.currentTimeMillis()
        if (last > 0L && now - last < DAILY_MS) return   // এখনো ২৪ ঘণ্টা হয়নি

        val ready = BiometricGate.unlockAvailability(act)
        if (ready != BiometricGate.Reason.SUCCESS) {
            // ফোনে চাওয়ার মতো কিছুই নেই — V499-এর একই নিয়ম, আটকানো হয় না।
            markUnlockedNow(act)
            return
        }

        dailyAsking = true
        BiometricGate.promptUnlock(
            act,
            "Unlock App",
            "Once a day — use your fingerprint, or your phone password"
        ) { res ->
            dailyAsking = false
            if (res.ok) markUnlockedNow(act)
            // ⛔ ব্যর্থ/বাতিল হলে জোর করা হয় না (V499-এর মতো "Close App" আটকানো
            //    এখানে বসানো হয়নি) — পরের বার অ্যাপ সামনে এলে আবার চাইবে,
            //    কিন্তু TK-এর "বারবার না" নিয়ম মেনে সঙ্গে সঙ্গেই না।
        }
    }

    @Volatile private var locked = true          // অ্যাপ চালু হলে প্রথমেই তালা
    @Volatile private var asking = false
    @Volatile private var warnedThisRun = false

    /**
     * 🔴🔴🔒 V521 (২২.০৮.২০২৬, TK-এর স্পষ্ট সিদ্ধান্ত) — **ছাড়ের সময়।**
     *
     * TK-এর কথা: *"অ্যাপ থেকে যতবারই বের হই যতবারই ঢুকি তখন এরকম দেখায়…
     * কেন এরকম দেখে আমাকে বিভ্রান্ত করবে? এত হাই সিকিউরিটি প্রয়োজনে আমার
     * দরকার নেই। একটা নির্দিষ্ট টাইম পিরিয়ডের পরে প্রয়োজন এরকম দেখাবে।"*
     *
     * **আগে যা হত (কোডে প্রমাণিত):** শেষ পর্দাটা বন্ধ হওয়া মাত্রই
     * (`PilesClinicApplication.onActivityStopped`, `visibleScreens == 0`)
     * `onAppBackgrounded()` চলত আর **সঙ্গে সঙ্গে** তালা লেগে যেত। তাই
     * রোগীর ছবি তুলতে ক্যামেরায় গেলে, WhatsApp-এ বার্তা পাঠাতে গেলে, বা
     * স্ক্রিন এক মুহূর্তের জন্য নিভে গেলেও ফিরে এসে আবার আঙুল চাইত।
     * কোনো সময়সীমা ছিল না — এটাই TK-কে বারবার আটকাচ্ছিল।
     *
     * **এখন:** পিছনে যাওয়ার সময়টা মনে রাখা হয়। ফিরে আসার সময় যদি
     * **১৫ মিনিটের কম** কেটে থাকে, তালা লাগে না — কাজ চলতেই থাকে।
     * ১৫ মিনিট বা তার বেশি হলে আগের মতোই আঙুল/পাসওয়ার্ড চাইবে।
     *
     * ⛔ **অ্যাপ পুরোপুরি বন্ধ হলে বা ফোন রিস্টার্ট হলে সবসময় চাইবে**
     *    (TK-এর নিজের বাছা নিয়ম) — কারণ `locked` ও নিচের সময়টা দুটোই
     *    শুধু চলতি process-এ থাকে, ফোনে জমা হয় না। অ্যাপ নতুন করে চালু
     *    হলে `locked = true` দিয়েই শুরু হয়, ঠিক আগের মতোই।
     * ⛔ লগআউট করলেও (`reset()`) আগের মতোই তালা — সময় ধরা হয় না।
     * ⛔ **হাজিরার নিয়ম এক অক্ষরও বদলায়নি** — সেটা আলাদা গেট
     *    (`WorkNotebookActivity` + `ClinicPresence`), এই ছাড় সেখানে খাটে না।
     * ⛔ ফোনের নিজের স্ক্রিন-লক আগের মতোই কাজ করে — সেটা অ্যাপের হাতে নয়।
     *
     * 🔓 **সৎ কথা:** এটা তালাটাকে একটু হালকা করে। ১৫ মিনিটের মধ্যে ফোনটা
     *    অন্য কারো হাতে পড়লে অ্যাপ খোলা পাওয়া যাবে। TK এটা জেনেই বেছেছেন
     *    ("এত হাই সিকিউরিটি প্রয়োজনে আমার দরকার নেই")।
     */
    private const val GRACE_MS = 15L * 60L * 1000L      // ১৫ মিনিট
    @Volatile private var awayAt = 0L                   // কখন পিছনে গেল

    /** অ্যাপ পুরোপুরি পিছনে গেছে — সময়টা মনে রাখা হয়, তালা এখনই নয়। */
    fun onAppBackgrounded() {
        awayAt = android.os.SystemClock.elapsedRealtime()
        warnedThisRun = false
    }

    /** লগআউটের সময় — পরের জনের জন্য আবার তালা। */
    fun reset() {
        locked = true
        asking = false
        warnedThisRun = false
        awayAt = 0L        // 🔵 V521: লগআউটে ছাড় নয় — পরের জনকে অবশ্যই চাইবে
    }

    fun isLocked(): Boolean = locked

    /** দরকার হলে আঙুল/পাসওয়ার্ড চায়। **সব ভূমিকার জন্য একই।** */
    fun guard(activity: Activity, user: NativeUser?) {
        if (user == null) return
        /* 🔵🔒 V521: পিছনে যাওয়ার পরে ফিরেছি — কতক্ষণ পরে?
           ⛔ `elapsedRealtime()` ফোনের ঘড়ি বদলালেও পিছিয়ে যায় না, তাই
              ঘড়ি বদলে ছাড় বাড়িয়ে নেওয়া যায় না। */
        val away = awayAt
        if (away > 0L) {
            awayAt = 0L
            val gone = android.os.SystemClock.elapsedRealtime() - away
            if (gone >= GRACE_MS) locked = true
        }
        if (!locked || asking) return
        val act = activity as? AppCompatActivity ?: return

        val ready = BiometricGate.unlockAvailability(act)
        if (ready != BiometricGate.Reason.SUCCESS) {
            // ফোনে চাওয়ার মতো কিছুই নেই — আটকালে ঢোকার পথই থাকত না।
            locked = false
            if (!warnedThisRun) {
                warnedThisRun = true
                showNoLockNotice(act, BiometricGate.messageFor(ready))
            }
            return
        }

        asking = true
        // 🔤 V509 (২১.০৮.২০২৬, TK-নির্দেশ — ছবিসহ: "এই ধরনের বাংলা থাকবে না"):
        // তালার পর্দার সব লেখা এখন ইংরেজি। ⛔ শুধু লেখা — নিয়ম অপরিবর্তিত।
        BiometricGate.promptUnlock(
            act,
            "Unlock App",
            "Use your fingerprint, or your phone password"
        ) { res ->
            asking = false
            if (res.ok) locked = false else showBlocked(act, res.message)
        }
    }

    /** স্ক্রিন-লক নেই — অ্যাপ খুলল, কিন্তু জানিয়ে দেওয়া হলো। */
    private fun showNoLockNotice(act: AppCompatActivity, why: String) {
        try {
            val b = androidx.appcompat.app.AlertDialog.Builder(act)
                .setCustomTitle(PremiumAlert.header(act, "App Lock Is Off"))
                .setMessage(
                    "This phone has no fingerprint and no screen lock, so the app opened directly. " +
                        "For safety, please turn on a screen lock in the phone Settings." +
                        "\n\n" + why)
                .setPositiveButton("OK", null)
                .setNegativeButton("Open Settings") { _, _ ->
                    BiometricGate.openEnrollSettings(act)
                }
            PremiumAlert.paint(b.show())
        } catch (_: Throwable) { }
    }

    /** মেলেনি বা বাতিল করেছেন — আবার চেষ্টা, নয়তো অ্যাপ বন্ধ। */
    private fun showBlocked(act: AppCompatActivity, message: String) {
        try {
            val b = androidx.appcompat.app.AlertDialog.Builder(act)
                .setCustomTitle(PremiumAlert.header(act, "Unlock App"))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Try Again") { _, _ ->
                    asking = false
                    guard(act, NativeSession.current(act))
                }
                .setNegativeButton("Close App") { _, _ ->
                    try { act.finishAffinity() } catch (_: Throwable) { act.finish() }
                }
            PremiumAlert.paint(b.show())
        } catch (_: Throwable) {
            asking = false
            try { act.finishAffinity() } catch (_: Throwable) { }
        }
    }
}
