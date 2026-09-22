package com.tkbiswas.pilesclinic.security

import com.tkbiswas.pilesclinic.clinical.RoleSession
import com.tkbiswas.pilesclinic.clinical.UserRole
import com.tkbiswas.pilesclinic.print.BranchInfo
import com.tkbiswas.pilesclinic.print.BranchSession

/**
 * Single place that answers "is this allowed?" for role- and branch-gated
 * actions, so the rule lives in one file instead of being copy-pasted across
 * screens. Existing per-screen checks (e.g. the clinical package's *Activity read-only
 * mode for Staff) already enforce role at the UI level — this adds the
 * shared policy other new Phase 9+ screens (Settings, Backup/Restore) use,
 * plus the branch-switch rule requested in this phase.
 */
object SecurityGuard {

    /** Doctor-only actions: kept only for legacy labels. Per requirement, the
     * Doctor role has NO functional meaning — Staff performs every action — so
     * none of the gates below depend on it anymore. */
    fun isDoctor(): Boolean = RoleSession.currentRole == UserRole.DOCTOR

    // 🔴🔒 খাতার সারি B444 (TK-নির্দেশ, 05.08.2026 — "Only Doctor can switch
    // branch" মন্তব্যটা কোডে ছিল, কিন্তু আসল চেক সবসময় true ছিল — স্টাফও
    // বদলাতে পারতেন)। TK নিশ্চিত করেছেন: শুধু Doctor/Master ব্রাঞ্চ
    // বদলাতে পারবেন, বাকিরা না। ⚠️ **TK-কে জানানো (সততার সাথে):** এই
    // ফাইলের উপরের মন্তব্যে আগে লেখা ছিল "Doctor role has NO functional
    // meaning — Staff performs every action" (একটা আগের সিদ্ধান্ত, ঠিক
    // উল্টো) — TK-এর আজকের স্পষ্ট নির্দেশ সেটাকে ওভাররাইড করছে, শুধু এই
    // ব্রাঞ্চ-বদলের জন্যই (canAccessBranch/canRunBackupOrRestore/
    // canChangeSettings — এই তিনটে অপরিবর্তিত রাখা হয়েছে, TK শুধু
    // ব্রাঞ্চ-বদল নিয়ে বলেছেন, বাকিগুলো ছোঁয়া হয়নি)। ⛔ পুরনো `RoleSession`
    // (এই ফাইলেই ব্যবহৃত, `object RoleSession` — legacy/আলাদা সিস্টেম)
    // ব্যবহার না করে সরাসরি `NativeSession` (আসল, সক্রিয় লগইন-সিস্টেম)
    // দেখা হচ্ছে, যাতে ভুল/পুরনো রোল ধরে না যায়।
    // 🔴🔴🔒 খাতার সারি B444 (TK-নির্দেশ সংশোধন, 05.08.2026 — "মাস্টার ছাড়া
    // ব্রাঞ্চ বদলের ভূমিকা আর কারোর থাকবে না")। আগে ভুল করে Doctor-কেও
    // রাখা হয়েছিল — TK স্পষ্ট করে দিয়েছেন: **শুধুমাত্র Master**।
    fun canSwitchBranch(context: android.content.Context): Boolean {
        val role = com.tkbiswas.pilesclinic.native.NativeSession.current(context)?.displayRole ?: ""
        return role == "master"
    }

    fun canAccessBranch(branch: BranchInfo): Boolean = true

    fun canRunBackupOrRestore(): Boolean = true

    fun canChangeSettings(): Boolean = true
}
