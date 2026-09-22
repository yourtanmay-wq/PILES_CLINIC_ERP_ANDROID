package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🧑‍⚕️🔒 V496 (২১.০৮.২০২৬, TK-এর চূড়ান্ত নির্দেশ §২) — **কে Doctor, কে Staff।**
 *
 * ─── 🔴 যে ফাঁদটা কোড পড়ে ধরা পড়েছে ──────────────────────────────────────
 * `NativeUser.permissionRole()` (NativeSession.kt:25) ডাক্তারকে **"staff"**
 * বানিয়ে দেয়:
 *
 *     fun permissionRole(actualRole: String): String =
 *         if (actualRole == "doctor" || actualRole == "field") "staff" else actualRole
 *
 * অর্থাৎ `user.role` দেখে Doctor আর Staff **আলাদা করাই যায় না** — দুজনেরই
 * `"staff"`। এই কারণেই V495 পর্যন্ত:
 *   • `DashboardActivity.kt:169` — Work Notebook টাইল `listOf("staff")`
 *     ⇒ **ডাক্তাররাও IN/OUT TIME-এর পর্দা পেতেন**
 *   • `AttendanceReminderWorker.kt:36` — `user.role == "staff"`
 *     ⇒ **ডাক্তারদের ফোনেও হাজিরার রিমাইন্ডার বাজত**
 *
 * ─── আসল পরিচয় কোথায় ─────────────────────────────────────────────────────
 * `displayRole` — আসল ভূমিকা (`master` · `doctor` · `field` · `staff`) এখানেই
 * অটুট থাকে (NativeSession.kt:19-22)। প্রজেক্টে **আগে থেকেই** এভাবে ব্যবহার হয়:
 *   • `IncomeExpenseActivity.kt:72, 150` — `displayRole == "doctor"`
 *   • `BriefingActivity.kt:901, 1008` — `displayRole == "doctor"`
 * ⇒ তাই এটাই প্রজেক্টের **প্রমাণিত** পথ; নতুন কিছু আবিষ্কার করা হয়নি।
 *
 * সার্ভারেও একই সত্য আছে — `hr.staff_profiles.role_kind` ও
 * `hr.app_identity.role_kind` (`master / staff / doctor / field`,
 * V246_ONE_RUN_SETUP.sql:37)। নতুন RPC ওটাই দেখে সিদ্ধান্ত নেয়, তাই
 * ফোনের কথার উপর কিছুই নির্ভর করে না।
 *
 * ─── TK-এর চূড়ান্ত নিয়ম ───────────────────────────────────────────────────
 *   Doctor : IN TIME নেই · OUT TIME নেই · ছুটির আবেদন নেই · হাজিরা নেই ·
 *            বেতন নেই। আঙুলের ছাপ **শুধু অ্যাপ খোলার** জন্য।
 *   Staff  : IN TIME (আঙুল-সহ) · OUT TIME · ছুটি · হাজিরা · বেতন।
 *   Master : সব আগের মতোই — কিছুই বদলায়নি।
 *
 * ⛔ শুধু নাম দেখে কাউকে Doctor ধরা হয় না।
 * ⛔ পুরনো কোনো ডেটাবেস সারি মোছা হয় না — শুধু নতুন ব্যবহার বন্ধ হয়।
 */
object RoleRules {

    const val ROLE_DOCTOR = "doctor"
    const val ROLE_MASTER = "master"
    const val ROLE_FIELD = "field"
    const val ROLE_STAFF = "staff"

    /** আসল ভূমিকা — `permissionRole()`-এর ঢাকা-পড়া নাম নয়। */
    fun actualRole(user: NativeUser?): String =
        (user?.displayRole ?: "").trim().lowercase()

    fun isDoctor(user: NativeUser?): Boolean = actualRole(user) == ROLE_DOCTOR

    fun isMaster(user: NativeUser?): Boolean =
        actualRole(user) == ROLE_MASTER || (user?.role == ROLE_MASTER)

    /**
     * **হাজিরার ব্যবস্থা (IN TIME · OUT TIME · ছুটি · উপস্থিতি · বেতন) কে পাবেন।**
     *
     * 🔒 TK-এর চূড়ান্ত নিয়ম (২১.০৮.২০২৬): **শুধুমাত্র আসল `staff`।**
     * ⛔ Doctor · Field · Master — কেউ নন।
     *
     * (V496-এর প্রথম খসড়ায় `field`-কেও রাখা হয়েছিল কারণ `permissionRole`
     *  তাঁকেও "staff" বানায়। TK স্পষ্টভাবে সেটি বাতিল করেছেন — তাই এখানে
     *  আর `ROLE_FIELD` নেই, এবং সার্ভারের `wn.mark_check_in()`-এও নেই।)
     *
     * ⛔ পুরোনো কোনো ডেটাবেস সারি মোছা হয় না — শুধু নতুন ব্যবহার বন্ধ হয়।
     */
    fun usesAttendance(user: NativeUser?): Boolean =
        actualRole(user) == ROLE_STAFF

    /**
     * 🔒🔒 V1380 (১২.০৯.২০২৬, TK-নির্দেশ স্পষ্ট করে দ্বিতীয়বার) — **ARMAN HOQUE
     * (কোচবিহার) শুধুমাত্র Doctor Visit আর তার নিজের IN TIME/OUT TIME দেখবেন,
     * আর কিছু না।**
     *
     * RUPAM-এর role এখনো "staff"-ই আছে (TK-নির্দেশ "RUPAM যা ছিল তাই থাক" —
     * সেটা ছোঁয়া হয়নি)। ARMAN-এর জন্য আলাদা, প্রকৃত কোড-স্তরের আটকানো — শুধু
     * এই একটা নম্বরের জন্য (role এখনো "staff", তাই login password অপরিবর্তিত)।
     * DashboardActivity/MoreMenuActivity এই ফাংশনটাই দেখে বাকি সব বাক্স/বোতাম/
     * সার্চ/ঘণ্টা/অনুস্মারক লুকায়।
     */
    private val DOCTOR_VISIT_ONLY_MOBILES = setOf("9883884394") // ARMAN HOQUE, Cooch Behar

    fun isDoctorVisitOnly(user: NativeUser?): Boolean =
        DOCTOR_VISIT_ONLY_MOBILES.contains(StaffDirectory.normalizeMobile(user?.mobile.orEmpty()))

    fun isDoctorVisitOnly(context: Context): Boolean =
        isDoctorVisitOnly(try { NativeSession.current(context) } catch (_: Throwable) { null })

    /**
     * **বেতনের (Salary) পর্দা ও বোতাম কে পাবেন — একই নিয়ম।**
     *
     * TK §৩: Doctor ও Field-এর বেতনের হিসাব অ্যাপে আর ব্যবহার হবে না।
     * পুরোনো সারি ডেটাবেসে **অক্ষত** থাকে, শুধু নতুন করে খোলা/বসানো বন্ধ।
     *
     * এখানে ভূমিকাটা দেখা হয় **যাঁর পাতা খোলা হচ্ছে তাঁর** (`role_kind`),
     * যিনি দেখছেন তাঁর নয় — মাস্টার দেখলেও ডাক্তারের বেতনের বোতাম আসবে না।
     */
    fun salaryAppliesToRoleKind(roleKind: String?): Boolean =
        (roleKind ?: "").trim().lowercase() == ROLE_STAFF

    fun usesSalary(user: NativeUser?): Boolean =
        actualRole(user) == ROLE_STAFF

    /** ডাক্তার/ফিল্ড-এর বেতনের বোতাম চাপলে যে একটাই বার্তা দেখানো হবে। */
    const val NO_SALARY_MSG =
        "এই অ্যাকাউন্টের জন্য বেতনের হিসাব নেই। বেতন শুধু স্টাফদের জন্য।"

    fun usesAttendance(context: Context): Boolean =
        usesAttendance(try { NativeSession.current(context) } catch (_: Throwable) { null })

    fun isDoctor(context: Context): Boolean =
        isDoctor(try { NativeSession.current(context) } catch (_: Throwable) { null })

    /** ডাক্তারকে হাজিরার পর্দায় দেখানোর একটাই বার্তা (সব জায়গায় একরকম)। */
    const val DOCTOR_NO_ATTENDANCE_MSG =
        "ডাক্তারদের জন্য হাজিরার ব্যবস্থা নেই — আপনি যেকোনো সময় আসতে ও যেতে পারেন।"
}
