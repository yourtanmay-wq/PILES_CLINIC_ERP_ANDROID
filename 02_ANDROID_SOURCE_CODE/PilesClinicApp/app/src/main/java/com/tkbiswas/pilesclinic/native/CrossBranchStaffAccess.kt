package com.tkbiswas.pilesclinic.native

/**
 * 🔴🔒 V453 (20.08.2026, TK-নির্দেশ): *"JPE-CRP এই staff যেন FALAKATA & BIRPARA-র
 * সমস্ত ডিটেইলস দেখতে পারে — Enquiry Visit Patient — দেখা ও Edit।"*
 *
 * TK নিজেই স্পষ্ট করেছেন: **টাকা (Payment/Bill/Advance) এর বাইরে** — সেটা
 * `MoneyBranchGuard.kt`-এর লক করা নিয়মেই থাকবে, এই ফাইল সেটা কখনো ছোঁবে না।
 *
 * ঠিক `NoBengali.kt` / `DoctorQueueActivity.isCrossBranchDoctorQueueAccess()`-
 * এর একই প্রমাণিত কৌশল — একজনের মোবাইল ধরে ব্যতিক্রম, বাকি সবার জন্য কিছুই
 * বদলায় না। `user.branch` কোথাও বদলানো হয়নি (session ছুঁয়ে অন্য সব পর্দা
 * প্রভাবিত হওয়ার ভয়ে) — শুধু "দেখা/Edit"-এর জন্য effective branch-list এখান
 * থেকে চাওয়া হয়।
 *
 * ⛔ যা এই ফাইল কখনো করে না: টাকা/Payment/Bill/Advance/Commission-এর কোনো
 *    permission — ওসব `MoneyBranchGuard.kt`-এর নিয়মেই আটকানো থাকে, অক্ষত।
 */
object CrossBranchStaffAccess {

    /** মোবাইল (শেষ ১০ অঙ্ক) → অতিরিক্ত অনুমোদিত ব্রাঞ্চ (নিজের ব্রাঞ্চ বাদে)। */
    /* 🔴🔒 V949 (০১.০৯.২০২৬, TK-নির্দেশ: *"JPE CRP কে Falakata & Birpara-র
       Access দেওয়া হয়েছিল, সেটা বন্ধ করতে চাইছি"*) — তালিকাটা **ফাঁকা** করা হলো।
       ⇒ এখন প্রত্যেকে শুধু নিজের ব্রাঞ্চই দেখেন, ঠিক V453-এর আগের মতো।
       ⛔ ব্যবস্থাটা মোছা হয়নি, শুধু তালিকা ফাঁকা — ভবিষ্যতে TK চাইলে একটা
          সারি বসালেই আবার চালু হবে, নতুন করে কিছু লিখতে হবে না।
       ⛔ সব-ব্রাঞ্চ এনকোয়ারি ফর্ম এই তালিকার উপর নির্ভর করে না (যাচাই করা) —
          তাই যে কোনো স্টাফ আগের মতোই যে কোনো ব্রাঞ্চের এনকোয়ারি করতে পারবেন। */
    private val EXTRA_BRANCHES: Map<String, List<String>> = emptyMap()

    /**
     * এই ব্যবহারকারীর "দেখা/Edit"-এর জন্য effective branch-filter স্ট্রিং।
     * ব্যতিক্রম না থাকলে `user.branch`-ই ফেরত আসে (আগের আচরণ অভিন্ন)।
     * ব্যতিক্রম থাকলে "নিজের ব্রাঞ্চ,অতিরিক্ত১,অতিরিক্ত২" — FollowUpRepository-র
     * branchScopeFilter()/branchAllows() এই কমা-তালিকা বোঝে (V453)।
     */
    fun effectiveViewBranch(user: NativeUser): String {
        if (user.role == "master") return user.branch // Master অপরিবর্তিত (already "All")
        val mobile = user.mobile.filter { it.isDigit() }.takeLast(10)
        val extra = EXTRA_BRANCHES[mobile] ?: return user.branch
        val own = user.branch.trim()
        val all = (listOf(own) + extra).filter { it.isNotBlank() }.distinct()
        return all.joinToString(",")
    }

    /** এই ব্যবহারকারী কি নিজের ব্রাঞ্চের বাইরেও (শুধু ব্যতিক্রম-তালিকায় থাকা
     *  ব্রাঞ্চে) Edit করতে পারবেন — টাকা ছাড়া (canEdit-জাতীয় চেকের জন্য)। */
    fun canEditBranch(user: NativeUser, recordBranch: String): Boolean {
        if (user.role == "master") return true
        if (recordBranch.isBlank()) return false
        if (recordBranch.equals(user.branch, ignoreCase = true)) return true
        val mobile = user.mobile.filter { it.isDigit() }.takeLast(10)
        val extra = EXTRA_BRANCHES[mobile] ?: return false
        return extra.any { it.equals(recordBranch, ignoreCase = true) }
    }
}
