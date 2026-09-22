package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🔒🔒 TK-REPORTED (29.07.2026 রাত — TK-এর কথা: "এই সমস্যা তো গত সেশনেও
 * বলেছিলাম, তার আগের সেশনেও বলেছিলাম, আবার কেন বলতে হচ্ছে?"):
 * Dashboard-এর ঘন্টায় "🔔 4" দেখাচ্ছিল, অথচ ঘন্টায় চাপ দিয়ে Briefing পর্দা
 * খুললে "No briefing / notice yet" — সম্পূর্ণ ফাঁকা।
 *
 * ⛔ স্থায়ী নিয়ম (এই ফাইলটাই তার প্রমাণ): ঘন্টা যা যা গোনে, ঘন্টায় চাপ
 *    দিলে যে পর্দা খোলে সেখানে ঠিক ততগুলোই যেন সত্যিই দেখা যায় — কখনো
 *    দুই জায়গায় দুই আলাদা হিসাব রাখা যাবে না।
 *
 * ROOT CAUSE (কোড ধরে খুঁজে পাওয়া, আন্দাজ নয়):
 * "Missing Visit Fee" অংশের জন্য দুটো সম্পূর্ণ আলাদা গণনা ছিল —
 *   • ঘন্টা (BellCounter) গুনত `fetchMissingVisitFeePatients()`-এর
 *     **কাঁচা সংখ্যা** — কে আগে "দেখে ফেলেছেন" (Master একবার নাম দেখলে
 *     সেটা লুকিয়ে যায়, খাতার সারি B48/B87) তা এখানে **বাদ দেওয়া হত না**।
 *   • Briefing পর্দা (ঘন্টায় চাপ দিলে ঠিক এখানেই আসা হয়) সেই একই তালিকা
 *     থেকে **আগে দেখা নামগুলো বাদ দিয়ে** দেখাত।
 * ফল: Master যে নামগুলো আগে একবার দেখে ফেলেছেন সেগুলো ঘন্টায় **চিরকাল**
 * যোগ হতেই থাকত, অথচ পর্দায় আর কখনো দেখা যেত না। এটাই "সংখ্যা আছে,
 * পর্দা ফাঁকা"-র আসল কারণ — শুধু এই একটা ঘরেই, বাকি চারটে (Briefing
 * notice · Remark Pending · Backdate · Edit request) আগে থেকেই মিলে যেত
 * (মিলিয়ে দেখা হয়েছে)।
 *
 * FIX: "দেখা হয়েছে কিনা" যাচাই করার নিয়মটা এই একটাই জায়গায় আনা হলো।
 * BellCounter ও BriefingActivity — দুটোই এখন ঠিক এই একই ফাংশন ডাকে,
 * তাই ভবিষ্যতে এই দুটো সংখ্যা আর কখনো আলাদা হতে পারবে না।
 *
 * ⛔ কোনো নতুন SharedPreferences ফাইল নয় — আগের
 *    "piles_clinic_fee_missing_seen" প্রেফসটাই ব্যবহার করা হয়েছে, তাই
 *    আগে যে নামগুলো "দেখা হয়েছে" মার্ক করা ছিল সেগুলো অক্ষত থাকবে,
 *    কারো নতুন করে কিছু "দেখতে" হবে না।
 * ⛔ টাকার কোনো হিসাব ছোঁয়া হয়নি — এটা শুধু "কোন নাম দেখানো হবে" তার
 *    নিয়ম, ফি সত্যিই নেওয়া হলে সারিটা এমনিতেই দুই জায়গা থেকেই উঠে যায়।
 */
object MissingFeeSeenGuard {

    private const val PREFS = "piles_clinic_fee_missing_seen"

    /** এক রোগীর জন্য একটাই চাবি — Patient ID থাকলে সেটাই, নইলে মোবাইলের
     *  শেষ ১০ সংখ্যা। (BriefingActivity-র পুরনো feeRowKey()-র হুবহু একই।) */
    fun rowKey(mv: MissingVisitFee): String {
        val id = mv.patientId.trim()
        if (id.isNotBlank()) return id
        return mv.mobile.filter { it.isDigit() }.takeLast(10)
    }

    /** এই ফোনে আগে কখনো "দেখা হয়েছে" মার্ক করা কিনা। */
    fun isSeenLocal(ctx: Context, mv: MissingVisitFee): Boolean = try {
        val key = rowKey(mv)
        key.isNotBlank() &&
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet("seen", emptySet())?.contains(key) == true
    } catch (_: Throwable) { false }

    /** এই ফোনে "দেখা হয়েছে" বলে মার্ক করা — সর্বোচ্চ ১০০০টা চাবি রাখা হয়। */
    fun markSeenLocal(ctx: Context, mv: MissingVisitFee) {
        try {
            val key = rowKey(mv)
            if (key.isBlank()) return
            val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val set = HashSet(p.getStringSet("seen", emptySet()) ?: emptySet())
            set.add(key)
            val trimmed = if (set.size > 1000) set.sortedDescending().take(1000).toHashSet() else set
            p.edit().putStringSet("seen", trimmed).apply()
        } catch (_: Throwable) { }
    }

    /** ঘন্টা ও Briefing পর্দা — দুটোই এই একটাই ফাংশন দিয়ে "সত্যিকারের
     *  বাকি" তালিকাটা বার করে (local + cloud, দুই সিঁ-চিহ্নই বাদ দিয়ে)।
     *  ব্যর্থ হলে (cloudSeen ফাঁকা এলে) স্বাভাবিকভাবেই local-চিহ্নটুকু
     *  দিয়ে কাজ চলে — আগের আচরণ অক্ষত। */
    fun trulyPending(ctx: Context, all: List<MissingVisitFee>, cloudSeen: Set<String>): List<MissingVisitFee> =
        all.filter { !isSeenLocal(ctx, it) && !cloudSeen.contains(rowKey(it)) }
}
