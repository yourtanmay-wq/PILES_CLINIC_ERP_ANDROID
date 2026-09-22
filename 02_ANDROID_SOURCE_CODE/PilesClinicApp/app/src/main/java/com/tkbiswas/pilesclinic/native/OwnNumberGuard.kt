package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🛡️🔒 V863 (৩০.০৮.২০২৬, TK-অনুমোদিত ডেমো প্রুফ) — **নিজেদের নম্বর পাহারা**
 *
 * TK-এর প্রশ্ন ছিল: *"সত্যতা যাচাই করুন কেন এই ধরনের সমস্যা হলো"* — কেন
 * স্টাফ/ডাক্তার/ক্লিনিকের নম্বরগুলো এনকোয়ারি-ভিজিট-রোগী হয়ে বসে ছিল।
 *
 * 🔬 **আসল কারণ (কোড ধরে যাচাই, আন্দাজ নয়):** অ্যাপে **কোনো পাহারাই ছিল না**।
 *   `EnquiryRepository.checkDuplicate()` শুধু দেখে নম্বরটা আগে থেকে আছে কিনা —
 *   **কার নম্বর** সেটা কখনো দেখে না। তাই টেস্ট করার সময় স্টাফরা নিজেদের
 *   নম্বর দিয়ে যা তৈরি করেছিলেন, সেগুলোই আসল রেকর্ড হয়ে গিয়েছিল
 *   (৩০.০৮.২০২৬-এ ৮টা নম্বর, ≈₹১,৪২,৪০০ ডেমো টাকা মুছতে হয়েছে)।
 *
 * ⛔ **আটকায় না, শুধু সতর্ক করে** — কারণ যাচাই করে দেখা গেছে **আসল রোগীও**
 *    স্টাফ/ডাক্তারের নম্বর দিতে পারেন (TK নিজে নিশ্চিত করেছেন: Raja Roy ও
 *    SERINA KHATTON আসল রোগী, ডাক্তারের নম্বরে রেজিস্টার করা)। আটকে দিলে
 *    তাঁদের রেজিস্ট্রেশনই করা যেত না।
 * ⛔ **কোনো ক্লাউড-পড়া নেই** — নম্বরের তালিকা অ্যাপের ভিতরেই আছে
 *    (`StaffDirectory` · `BranchCatalog`)। Egress শূন্য।
 */
object OwnNumberGuard {

    /**
     * নম্বরটা আমাদের নিজেদের কারো হলে **কার**, সেটা ফেরত দেয় (স্টাফের পর্দায়
     * দেখানোর মতো ইংরেজি লেখা)। আমাদের কারো না হলে `null`।
     */
    fun ownerOf(rawMobile: String): String? {
        val m = StaffDirectory.normalizeMobile(rawMobile)
        if (m.length != 10) return null

        // ১) ক্লিনিকের নিজের নম্বর (৫ ব্রাঞ্চ + সর্বজনীন হেল্পলাইন)
        try {
            for (b in com.tkbiswas.pilesclinic.print.BranchCatalog.all) {
                if (StaffDirectory.normalizeMobile(b.phoneLine) == m) {
                    return b.displayName + " clinic number"
                }
            }
            if (StaffDirectory.normalizeMobile(
                    com.tkbiswas.pilesclinic.print.BranchCatalog.HELPLINE) == m) {
                return "Clinic helpline number"
            }
        } catch (_: Throwable) { }

        // ২) আমাদের স্টাফ / ডাক্তার / ফিল্ড-এর নিজের নম্বর
        try {
            val acc = StaffDirectory.findAccount(m)
            if (acc != null) {
                val who = listOf(acc.name, acc.branch).filter { it.isNotBlank() }.joinToString(" · ")
                return (if (who.isNotBlank()) who else m) + " — " + acc.role
            }
        } catch (_: Throwable) { }

        return null
    }

    /**
     * সেভ করার ঠিক আগে ডাকুন। আমাদের নম্বর না হলে **সঙ্গে সঙ্গে** `onContinue()`
     * চলে (এক মুহূর্তও দেরি নয়, আগের আচরণ হুবহু অপরিবর্তিত)। আমাদের নম্বর হলে
     * সতর্কবার্তা — "Continue" চাপলে তবেই আগের কাজটা হয়।
     */
    fun confirmIfOwn(ctx: Context, rawMobile: String, onContinue: () -> Unit) {
        val owner = try { ownerOf(rawMobile) } catch (_: Throwable) { null }
        if (owner == null) { onContinue(); return }
        try {
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setCustomTitle(PremiumAlert.header(ctx, "⚠️ This is our own number"))
                .setMessage(
                    owner + "\n\n" +
                    "This number belongs to us, so saving it as a patient is usually a demo or a mistake.\n\n" +
                    "Continue only if this patient really uses this number."
                )
                .setPositiveButton("Continue") { _, _ -> onContinue() }
                .setNegativeButton("Cancel", null)
                .show().also {
                    PremiumAlert.paint(it)
                    try { NoAutofill.scrubAnyDialog(it) } catch (_: Throwable) { }   // 🤫 V774
                }
        } catch (_: Throwable) {
            // পপ-আপ কোনো কারণে না দেখানো গেলে কাজ আটকাবে না — আগের মতোই চলে।
            onContinue()
        }
    }
}
