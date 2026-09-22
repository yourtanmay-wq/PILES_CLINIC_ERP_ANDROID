package com.tkbiswas.pilesclinic.native

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 🔐🔒 V496 (২১.০৮.২০২৬, TK-এর চূড়ান্ত নির্দেশ §১০ ও §১১)
 *
 * দুটো কাজ, দুটোই Staff ও Doctor — **দুজনের জন্যই**:
 *
 *  ১. **৭ দিন অ্যাপ না খুললে নিজে থেকে লগআউট।**
 *  ২. **মাস্টার বন্ধ করে দিলে (Suspend / Remove) চালু ফোনও বন্ধ।**
 *
 * ⛔ Master-এর নিজের নিয়ম ছোঁয়া হয়নি (TK §১০-এর শেষ লাইন) — মাস্টারের
 *    অ্যাকাউন্টে না ৭ দিনের হিসাব চলে, না suspend-যাচাই।
 *
 * ─── "ব্যবহার" কাকে বলে (TK §৬ ও §১০) ─────────────────────────────────────
 * শুধু **ব্যবহারকারী নিজে অ্যাপ সামনে আনলে** (`Activity.onStart`)।
 * ⛔ পিছনের সিঙ্ক · Realtime · নোটিফিকেশন · রিমাইন্ডার — একটাও নয়।
 *    সেই কারণেই এই ঘড়িটা কেবল `PilesClinicApplication`-এর
 *    `ActivityLifecycleCallbacks` থেকে চালু হয়, কোনো Worker থেকে নয়।
 *
 * ─── ফোনের ঘড়ি পিছিয়ে দিলে? (TK §১০-এর শেষ শর্ত) ─────────────────────────
 * শুধু "এখন − শেষবার" হিসাব করলে কেউ ঘড়ি পিছিয়ে দিয়ে চিরকাল লগইন থাকতে
 * পারত। তাই **সবচেয়ে বড় দেখা সময়ও** (`maxSeen`) জমা থাকে, আর হিসাব হয়:
 *
 *     কত সময় গেল = max(এখন, maxSeen) − শেষবার সামনে আনা
 *
 * ⇒ ঘড়ি পিছিয়ে দিলেও `maxSeen` কমে না, তাই হিসাব পিছোয় না।
 * ⇒ ঘড়ি এগিয়ে দিলে আগেভাগে লগআউট হতে পারে — সেটা নিরাপদ দিক, ক্ষতি নেই।
 * ⚠️ সৎ সীমা: ফোন রিসেট করে অ্যাপের তথ্য মুছে দিলে হিসাব নতুন করে শুরু হয়।
 *    তখন লগইনও থাকে না, তাই মোবাইল/পাসওয়ার্ড দিয়ে ঢুকতেই হবে।
 *
 * ─── Suspend/Remove যাচাই — খরচ না বাড়িয়ে ────────────────────────────────
 * সত্যের উৎস আগের মতোই `hr.staff_profiles.active` ও `suspended_until`
 * (V311/V404) — **নতুন কোনো ব্যবস্থা বানানো হয়নি**। বাদ-দেওয়া অ্যাকাউন্টে
 * সার্ভার `2999-12-31` ফেরায়, তাই একটাই ছোট ডাকেই দুটোই ধরা পড়ে।
 *
 *  • অ্যাপ সামনে এলে — **১৫ মিনিটে একবারের বেশি নয়**
 *  • গুরুত্বপূর্ণ কিছু লেখার আগে — ১৫ মিনিটের বেশি পুরনো হলে **তখনই টাটকা যাচাই**
 *  ⇒ দিনে জনপ্রতি সর্বোচ্চ ~৩০টা ছোট ডাক। V493-এর dedupe-ও এর উপরে কাজ করে।
 *
 * ⚠️ **অফলাইনে (সৎভাবে):** ফোনে ইন্টারনেট না থাকলে মাস্টারের নতুন সিদ্ধান্ত
 *    ওই ফোন **জানতেই পারে না** — এটা এড়ানোর কোনো উপায় নেই। সংযোগ ফিরলে
 *    পরের বার সামনে আনায় বা লেখার আগেই ধরা পড়ে ও লগআউট হয়।
 *    নেট-সমস্যায় কখনো ভুল করে লগআউট করা হয় না (fail-open) — নইলে সবার
 *    কাজ আটকে যেত।
 */
object SessionGuard {

    private const val PREFS = "piles_clinic_session_guard"
    private const val K_LAST_FG = "lastForegroundAt"   // শেষবার সামনে আনা
    private const val K_MAX_SEEN = "maxSeenAt"         // এ পর্যন্ত দেখা সবচেয়ে বড় সময়
    private const val K_LAST_CHECK = "lastSuspendCheckAt"
    private const val K_OWNER = "ownerMobile"          // কার হিসাব

    /** ৭ দিন। */
    const val INACTIVITY_LIMIT_MS = 7L * 24L * 60L * 60L * 1000L

    /** **সাধারণ** Suspend-যাচাই (অ্যাপ সামনে আনলে) এর চেয়ে ঘন ঘন নয় — ১৫ মিনিট। */
    const val SUSPEND_CHECK_GAP_MS = 15L * 60L * 1000L

    /**
     * **গুরুত্বপূর্ণ কিছু লেখার আগে** কত পুরনো ফল মানা হবে — ১ মিনিট।
     *
     * TK §৫ (২১.০৮.২০২৬): *"রুটিন যাচাই ১৫ মিনিটে একবার হতে পারে, কিন্তু
     * গুরুত্বপূর্ণ লেখার (হাজিরা · রোগী · টাকা) আগে টাটকা যাচাই লাগবে।"*
     *
     * ⇒ তাই লেখার পথে ১৫ মিনিটের পুরনো ফল **মানা হয় না**; ১ মিনিটের বেশি
     *   পুরনো হলেই সার্ভারে নতুন করে জিজ্ঞাসা করা হয়।
     * ⇒ ১ মিনিট রাখার কারণ: একটা রোগী সেভ করলে পরপর কয়েকটা সারি লেখা হয়
     *   (রোগী · পেমেন্ট · ফলো-আপ)। শূন্য রাখলে একই সেভে বারবার একই প্রশ্ন
     *   যেত — খরচ বাড়ত, কাজও ধীর হত।
     * ⚠️ সৎ সীমা: এর মানে মাস্টার বন্ধ করার পরে **সর্বোচ্চ ১ মিনিট** পর্যন্ত
     *   একটা লেখা ঢুকে যেতে পারে। এটা শূন্য করা সম্ভব নয় — নেটওয়ার্কের
     *   উত্তর আসতেও সময় লাগে।
     */
    const val WRITE_FRESH_MS = 60L * 1000L

    private fun p(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun mob(user: NativeUser?): String =
        (user?.mobile ?: "").filter { it.isDigit() }.takeLast(10)

    /** অন্য কেউ লগইন করলে আগের হিসাব মুছে নতুন করে শুরু। */
    private fun ensureOwner(context: Context, user: NativeUser?) {
        val sp = p(context)
        val now = mob(user)
        if (sp.getString(K_OWNER, "") != now) {
            sp.edit().clear().putString(K_OWNER, now).apply()
        }
    }

    // ── ৭ দিনের হিসাব ────────────────────────────────────────────────────────

    /** ব্যবহারকারী নিজে অ্যাপ সামনে এনেছেন — **শুধু এখান থেকেই** ডাকা হয়। */
    fun noteForeground(context: Context) {
        try {
            val user = NativeSession.current(context) ?: return
            ensureOwner(context, user)
            val sp = p(context)
            val now = System.currentTimeMillis()
            val maxSeen = maxOf(sp.getLong(K_MAX_SEEN, 0L), now)
            sp.edit().putLong(K_LAST_FG, now).putLong(K_MAX_SEEN, maxSeen).apply()
        } catch (_: Throwable) { }
    }

    /** এখনো লগইন থাকার কথা কিনা। `false` = ৭ দিন পেরিয়েছে। */
    fun isWithinInactivityWindow(context: Context): Boolean {
        return try {
            val sp = p(context)
            val last = sp.getLong(K_LAST_FG, 0L)
            if (last <= 0L) {
                // প্রথমবার — এখন থেকেই গোনা শুরু (পুরনো লগইন হঠাৎ কাটা যাবে না)
                noteForeground(context)
                return true
            }
            val now = System.currentTimeMillis()
            val maxSeen = maxOf(sp.getLong(K_MAX_SEEN, 0L), now)
            (maxSeen - last) < INACTIVITY_LIMIT_MS
        } catch (_: Throwable) { true }   // হিসাব করতে না পারলে কাউকে বার করে দেওয়া হয় না
    }

    // ── Suspend / Remove ─────────────────────────────────────────────────────

    enum class Verdict { OK, BLOCKED, UNKNOWN }

    // 🔴🔒 V1649 (২১.০৯.২০২৬) — পূর্ণ-পর্দা বার্তার জন্য বাড়তি তথ্য (staffCode/
    // until/removed/reason)। ডিফল্ট সবই ফাঁকা/false — আগে থেকে এই ক্লাস যাঁরা
    // ব্যবহার করেন (SupabaseClient.kt) শুধু verdict/message পড়েন, তাঁদের
    // একটুও প্রভাব পড়েনি।
    data class Check(
        val verdict: Verdict, val message: String = "",
        val staffCode: String = "", val until: String = "",
        val removed: Boolean = false, val reason: String = ""
    )

    /**
     * সার্ভারে সত্যিকারের যাচাই। **নেটওয়ার্ক থ্রেড থেকে ডাকতে হবে।**
     * নেট না থাকলে `UNKNOWN` — কখনো ভুল করে বন্ধ করা হয় না।
     */
    fun checkSuspensionNow(context: Context): Check {
        val user = try { NativeSession.current(context) } catch (_: Throwable) { null }
            ?: return Check(Verdict.UNKNOWN)
        if (RoleRules.isMaster(user)) return Check(Verdict.OK)   // মাস্টার ছোঁয়া হয় না

        val digits = mob(user)
        if (digits.length != 10) return Check(Verdict.UNKNOWN)

        val until = try {
            CloudPasswordCheck.fetchSuspendedUntil(digits)
        } catch (_: Throwable) {
            return Check(Verdict.UNKNOWN)
        } ?: run {
            // সফলভাবে "কিছু নেই" জানা গেল — সময় লিখে রাখি
            p(context).edit().putLong(K_LAST_CHECK, System.currentTimeMillis()).apply()
            return Check(Verdict.OK)
        }

        p(context).edit().putLong(K_LAST_CHECK, System.currentTimeMillis()).apply()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
            .format(java.util.Date())
        if (until >= today) {
            // V404-এর নিয়ম: বাদ দেওয়া হলে সার্ভার 2999-12-31 ফেরায়।
            val removed = until >= "2900-01-01"
            val msg = if (removed)
                "আপনার অ্যাকাউন্ট বন্ধ করা হয়েছে। মাস্টারকে জানান।"
            else
                "আপনি $until পর্যন্ত বন্ধ আছেন। মাস্টারকে জানান।"
            // 🔴🔒 V1649 — কারণ (থাকলে) — আলাদা, ছোট্ট একটা ডাক (ব্যর্থ হলে ফাঁকা)।
            val reason = try { CloudPasswordCheck.fetchSuspendReason(digits) ?: "" } catch (_: Throwable) { "" }
            return Check(Verdict.BLOCKED, msg, staffCode = user.name, until = until, removed = removed, reason = reason)
        }
        return Check(Verdict.OK)
    }

    /** শেষ যাচাইয়ের পরে [gapMs] সময় পেরিয়েছে কিনা (ডিফল্ট: রুটিন ১৫ মিনিট)। */
    fun suspendCheckDue(context: Context, gapMs: Long = SUSPEND_CHECK_GAP_MS): Boolean = try {
        System.currentTimeMillis() - p(context).getLong(K_LAST_CHECK, 0L) >= gapMs
    } catch (_: Throwable) { true }

    /**
     * **গুরুত্বপূর্ণ কিছু লেখার আগে** (TK §৫ ও §১১) — এখানে **রুটিনের ১৫ মিনিট
     * নয়, [WRITE_FRESH_MS] (১ মিনিট)** মানা হয়। ১ মিনিটের বেশি পুরনো হলে
     * সার্ভারে নতুন করে জিজ্ঞাসা করা হয়। নেটওয়ার্ক থ্রেড থেকে ডাকতে হবে।
     *
     * ─── ⚠️ যা এটা পারে না (সৎভাবে, লুকানো হয়নি) ─────────────────────────
     *  • **অফলাইনে কিছুই যাচাই হয় না।** নেট না থাকলে `UNKNOWN` ফেরে এবং
     *    লেখা **আটকানো হয় না** (fail-open)। নইলে সামান্য নেট-সমস্যাতেই
     *    সবার রোগী-এন্ট্রি ও টাকার হিসাব থেমে যেত (খাতার সারি B446)।
     *  • **সার্ভার উত্তর না দিলেও** একই — আটকানো হয় না।
     *  ⇒ অর্থাৎ এটা ফাঁকি **কঠিন করে**, ১০০% বন্ধ করে না। ১০০% নিরাপত্তার
     *    দাবি এখানে করা হচ্ছে না। আসল, না-ফসকানো পাহারা সার্ভারের RLS ও
     *    `wn.mark_check_in()`-এ — সেটা অফলাইনেও ফসকায় না, কারণ লেখাটা
     *    সার্ভারে পৌঁছালে তবেই বসে।
     */
    fun ensureFreshForWrite(context: Context): Check =
        if (suspendCheckDue(context, WRITE_FRESH_MS)) checkSuspensionNow(context)
        else Check(Verdict.OK)

    // ── লগআউট ────────────────────────────────────────────────────────────────

    /**
     * এই ফোনের লগইন বন্ধ করা।
     * ⛔ **কোনো তথ্য মোছা হয় না** — রোগী · হাজিরা · ছুটি · টাকা সব অটুট
     *    (ক্লাউডে তো বটেই, ফোনের জমানো কাজের সারিও)।
     * শুধু পরিচয় ও এই পাহারার নিজের হিসাব মুছে, পিছনের কাজ থামানো হয়।
     */
    fun logoutLocal(context: Context) {
        val app = context.applicationContext
        try { NativeSession.clear(app) } catch (_: Throwable) { }
        try { com.tkbiswas.pilesclinic.modules.ModuleAuth.signOut(app) } catch (_: Throwable) { }
        try { CloudReadDedupe.clear() } catch (_: Throwable) { }
        // 🟢🔒 V601 (২৪.০৮.২০২৬) — পরের যিনিই লগইন করুন, ২৪-ঘণ্টার টাইমার
        // যেন নতুন করে শুরু হয় (আগের ব্যবহারকারীর জমা সময় যেন না থেকে যায়)।
        try { com.tkbiswas.pilesclinic.native.AppLock.resetDaily(app) } catch (_: Throwable) { }
        try { CloudReadCache.clear() } catch (_: Throwable) { }
        try { p(app).edit().clear().apply() } catch (_: Throwable) { }
        try { androidx.work.WorkManager.getInstance(app).cancelAllWork() } catch (_: Throwable) { }
    }
}
