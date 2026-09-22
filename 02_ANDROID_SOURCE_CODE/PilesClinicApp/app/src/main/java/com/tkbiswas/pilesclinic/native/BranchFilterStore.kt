package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🟢🔒 V398 (16.08.2026, TK-অনুমোদিত) — মাস্টারের ব্রাঞ্চ **একবার** বাছলেই
 * পুরো অ্যাপের সব পর্দায় মনে থাকবে।
 *
 * TK-এর নির্দেশ (16.08.2026):
 *   "আমি চাইছি মাস্টার প্রতিবার কোন ব্রাঞ্চ সিলেক্ট করে নেবে / লাস্ট যে ব্রাঞ্চ
 *    সিলেক্ট করা থাকবে / প্রতিবার প্রতিটা সেকশনের সেই ব্রাঞ্চই থেকে যাবে /
 *    বারবার যেন সিলেক্ট করার প্রয়োজন না পড়ে / মাস্টার যদি চায় অল ব্রাঞ্চ
 *    সিলেক্ট করে রাখতে তবেই অল ব্রাঞ্চ সিলেক্ট থাকবে"
 *
 * আগে কী ছিল: প্রতিটি Activity-র নিজস্ব `pickedBranch`/`selectedBranch` চলক
 * প্রতিবার নতুন Activity-তে ফাঁকা হয়ে **নিজে থেকে "All"** হয়ে যেত — ফলে পাঁচ
 * ব্রাঞ্চের সব সারি নামত (DoctorQueueRepository:129, DraftRepository:332,
 * PaymentRepository:154, ChamberAttendanceRepository:253 ইত্যাদি সার্ভারেই
 * ব্রাঞ্চ ছাঁকে — তাই "All" মানে সত্যিই বেশি ডাটা)। একমাত্র B670-তে
 * DoctorQueueActivity-তে মনে-রাখা যোগ হয়েছিল; বাকি কোথাও নয়।
 *
 * ⛔ এটি **শুধু একটা ছাঁকনি** — লগইন-করা ইউজারের নিজের `NativeSession` branch
 *    কখনোই ছোঁয়া হয় না। মাস্টারের নিজের branch আক্ষরিক "All"
 *    (StaffDirectory.kt:26); সেটা বদলালে মাস্টারের সব permission ভেঙে যেত।
 * ⛔ স্টাফ/ডাক্তারের জন্য কিচ্ছু বদলায় না — তাঁদের পর্দায় ব্রাঞ্চ-পিলই থাকে না,
 *    আর তাঁদের সারি আগের মতোই নিজের ব্রাঞ্চেই সীমিত।
 * ⛔ ক্লাউডে একটাও নতুন অনুরোধ যায় না — মানটা শুধু এই ফোনে জমা থাকে।
 * ⛔ প্রিন্টের `BranchSession` (BranchInfo.kt) সম্পূর্ণ আলাদা জিনিস (ক্লিনিকের
 *    নাম/ঠিকানা/লোগো) — সেটা এখানে একটুও ছোঁয়া হয়নি।
 *
 * তিনটি অবস্থা:
 *   ""      = মাস্টার এখনো কিছু বাছেননি → তালিকা নয়, "Branch বাছুন" বার্তা
 *   "All"   = মাস্টার **নিজে** All বেছেছেন → সব ব্রাঞ্চ
 *   "<নাম>" = ওই একটি ব্রাঞ্চ
 */
object BranchFilterStore {

    private const val PREFS = "piles_branch_filter"
    private const val KEY = "branch"

    const val NONE = ""
    const val ALL = "All"

    /** ব্রাঞ্চের তালিকা — BranchCatalog/StaffDirectory-র সাথে হুবহু একই পাঁচটি নাম। */
    val BRANCHES: List<String> =
        listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")

    /** পর্দার বাছাই-তালিকা: "All" + পাঁচ ব্রাঞ্চ। */
    fun choices(): List<String> = listOf(ALL) + BRANCHES

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * মনে রাখা মান। তালিকায় না-থাকা নাম জমা থাকলে (ব্রাঞ্চের নাম বদলালে)
     * নিরাপদে "" ধরা হয় — ভুল ব্রাঞ্চের তালিকা কখনো দেখাবে না।
     */
    fun get(ctx: Context): String {
        val v = try { prefs(ctx).getString(KEY, "") ?: "" } catch (_: Throwable) { "" }
        return if (v == ALL || BRANCHES.contains(v)) v else NONE
    }

    /** বাছাই জমা রাখা। তালিকার বাইরের কিছু এলে "" জমা হয়। */
    fun set(ctx: Context, v: String?): String {
        val s = (v ?: "").trim()
        val safe = if (s == ALL || BRANCHES.contains(s)) s else NONE
        try { prefs(ctx).edit().putString(KEY, safe).apply() } catch (_: Throwable) {}
        return safe
    }

    /** মাস্টার একবারও কিছু বেছেছেন কিনা। */
    fun chosen(ctx: Context): Boolean = get(ctx).isNotBlank()

    /** পিলে যা লেখা থাকবে। */
    fun label(ctx: Context): String {
        val v = get(ctx)
        return if (v.isBlank()) "Select Branch" else v
    }

    fun pillText(ctx: Context): String = "🏥 " + label(ctx) + " ▾"

    /** বাছাই-বাক্সে কোন সারিটা আগে থেকে টিক দেওয়া থাকবে (না-বাছা হলে -1)। */
    fun indexInChoices(ctx: Context): Int {
        val v = get(ctx)
        return if (v.isBlank()) -1 else choices().indexOf(v)
    }

    /**
     * এই ইউজারের জন্য কার্যকর ব্রাঞ্চ।
     * মাস্টার  → মনে রাখা মান ("" হতে পারে)
     * অন্য সবাই → নিজের ব্রাঞ্চ (আগের আচরণ হুবহু অটুট)
     */
    fun effective(ctx: Context, user: NativeUser?): String =
        if (user != null && user.role == "master") get(ctx) else (user?.branch ?: "")

    /**
     * Repository-তে পাঠানোর মান।
     * ""      → null নয়, বরং [notChosen] সত্য — কল করাই উচিত নয় (কিছু নামবে না)
     * "All"   → null (সার্ভারে ব্রাঞ্চ-ছাঁকনি বসে না, আগের মতোই সব)
     * "<নাম>" → ওই নামটাই
     */
    fun repoFilter(branch: String): String? =
        if (branch.isBlank() || branch == ALL) null else branch

    /** মাস্টার এখনো কিছু বাছেননি — তালিকা আনা হবে না, বার্তা দেখাতে হবে। */
    fun notChosen(ctx: Context, user: NativeUser?): Boolean =
        user != null && user.role == "master" && get(ctx).isBlank()

    /** পর্দায় দেখানোর সহজ বাংলা বার্তা (সব পর্দায় একই লেখা)। */
    const val ASK_TEXT =
        "🏥 উপরে ডান দিকের Branch বাক্স থেকে একটি ব্রাঞ্চ বাছুন।\n" +
        "একবার বাছলেই সব পর্দায় এটাই মনে থাকবে — বারবার বাছতে হবে না।\n" +
        "সব ব্রাঞ্চ একসঙ্গে দেখতে চাইলে All বাছুন।"
}
