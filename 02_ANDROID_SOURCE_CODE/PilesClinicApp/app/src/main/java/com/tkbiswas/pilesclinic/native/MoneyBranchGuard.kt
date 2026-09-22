package com.tkbiswas.pilesclinic.native

import android.content.Context

/**
 * 🔒 TK'S LOCKED RULE (27.07.2026, in his own words):
 *
 *   "Bill · advance · any payment — যে ব্রাঞ্চের স্টাফ তারাই করতে পারবে।
 *    অন্য কোনো ব্রাঞ্চের স্টাফ করতে পারবে না। সংশ্লিষ্ট ব্রাঞ্চের ডাক্তার
 *    করতে পারবে। মাস্টার করতে পারবে।"
 *
 * WHY THIS IS DIFFERENT FROM ENQUIRY / REGISTRATION
 * An enquiry comes in on ONE advertised number that any branch's staff may
 * answer, and a registration form carries its own branch picker -- both are
 * deliberately cross-branch (see the "এক নম্বরে সব কল" rule). Money is not:
 * the cash is physically handed over at one chamber. If another branch's staff
 * could take it, that branch's Today's Collection and its Chamber Register
 * would never agree with the cash actually in the drawer.
 *
 * WHO MAY TAKE MONEY FOR A PATIENT
 *   - Master: any branch.
 *   - The patient's own branch: its staff AND its doctor (a doctor's branch is
 *     checked exactly like a staff's -- "সংশ্লিষ্ট ব্রাঞ্চের ডাক্তার").
 *   - Anyone else: no.
 *
 * HOW THE PATIENT'S BRANCH IS DECIDED
 * The record's `branch` field, OR the branch code inside the Patient ID
 * (COB-26072026-001 = Cooch Behar) -- the same two-way check the rest of the
 * app already uses, because the branch field can be blank or edited later while
 * the Patient ID is fixed at registration and is what is printed on the
 * patient's papers. Either one matching is enough.
 *
 * DELIBERATELY PERMISSIVE IN ONE CASE: if BOTH the branch field and the Patient
 * ID are blank/unreadable, this allows the payment. A real payment must never
 * be blocked because a record is missing its branch -- that would lose money,
 * which is far worse than the case this rule protects against.
 */
object MoneyBranchGuard {

    /** True when this user may take/edit money for a record of this branch. */
    fun canTakeMoney(user: NativeUser?, patientBranch: String, patientCode: String): Boolean {
        if (user == null) return true            // nothing to check against; never block a payment
        if (user.role == "master") return true
        val mine = user.branch.trim()
        if (mine.isBlank() || mine.equals("All", ignoreCase = true)) return true
        val theirs = patientBranch.trim()
        if (theirs.isNotBlank() && theirs.equals(mine, ignoreCase = true)) return true
        val code = patientCode.substringBefore('-').trim()
        if (code.length == 3) {
            return code.equals(PatientIdGenerator.branchCode(mine), ignoreCase = true)
        }
        // Branch field disagreed and there is no readable Patient ID code.
        return theirs.isBlank()
    }

    /** Convenience overload for the screens, which all have a Context. */
    fun canTakeMoney(context: Context?, patientBranch: String, patientCode: String): Boolean {
        val user = context?.let { NativeSession.current(it) }
        return canTakeMoney(user, patientBranch, patientCode)
    }

    /** The one message shown everywhere this is blocked (English only, per the
     *  project-wide rule that no screen text is in Bengali). */
    fun blockMessage(patientBranch: String): String {
        val br = patientBranch.trim().ifBlank { "another branch" }.uppercase()
        return "This patient belongs to $br. Only $br staff, its doctor, or Master can take a Bill / Advance / Payment."
    }
}
