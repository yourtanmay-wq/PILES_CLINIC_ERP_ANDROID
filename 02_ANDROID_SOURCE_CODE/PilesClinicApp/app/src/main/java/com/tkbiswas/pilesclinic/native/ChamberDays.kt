package com.tkbiswas.pilesclinic.native

import com.tkbiswas.pilesclinic.print.BranchCatalog
import java.util.Calendar

/**
 * NEW (2026-08-07, TK-approved "একদিন আগে আসার কথা" feature).
 *
 * Which weekdays each branch actually runs its chamber. TK provided these
 * exactly (voice + notebook):
 *   Jalpaiguri  → শনি (Sat) + মঙ্গল (Tue)
 *   Cooch Behar → সোম (Mon) + শুক্র (Fri)
 *   Birpara     → বুধ (Wed) + রবি (Sun)
 *   Falakata    → মঙ্গল (Tue) + বৃহ (Thu) + শনি (Sat)
 *   Kishanganj  → প্রতিদিন, শুধু রবিবার ছাড়া (daily except Sunday)
 *
 * This is used ONLY to LIGHTLY HIGHLIGHT chamber-days in the follow-up
 * calendar (and, for an Enquiry "আসবে" pick, to limit selection to a real
 * chamber-day). It reads nothing from the cloud and changes no existing
 * data — a pure lookup table. If a branch name is not recognised, every
 * weekday is treated as a chamber-day (never blocks the staff).
 *
 * Calendar day-of-week ints: SUN=1, MON=2, TUE=3, WED=4, THU=5, FRI=6, SAT=7.
 */
object ChamberDays {

    private val JALPAIGURI = setOf(Calendar.SATURDAY, Calendar.TUESDAY)
    private val COOCH_BEHAR = setOf(Calendar.MONDAY, Calendar.FRIDAY)
    private val BIRPARA = setOf(Calendar.WEDNESDAY, Calendar.SUNDAY)
    private val FALAKATA = setOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY)
    private val KISHANGANJ = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    ) // every day except Sunday

    /** Weekday set for a branch, or null when the branch isn't recognised
     *  (caller then treats all days as selectable — never blocks). */
    fun weekdaysFor(branch: String?): Set<Int>? {
        return when (BranchCatalog.byName(branch).id) {
            "jalpaiguri" -> JALPAIGURI
            "cooch_behar" -> COOCH_BEHAR
            "birpara" -> BIRPARA
            "falakata" -> FALAKATA
            "kishanganj" -> KISHANGANJ
            else -> null
        }
    }

    /** True if the given day-of-week (Calendar.SUNDAY..SATURDAY) is a chamber
     *  day for this branch. Unknown branch → true (highlight nothing special,
     *  allow everything). */
    fun isChamberWeekday(branch: String?, dayOfWeek: Int): Boolean {
        val set = weekdaysFor(branch) ?: return true
        return set.contains(dayOfWeek)
    }

    /** Short English label of a branch's chamber days, e.g. "Sat, Tue" —
     *  used only as a caption in the calendar header. */
    fun labelFor(branch: String?): String {
        val set = weekdaysFor(branch) ?: return "Every Day"
        if (set.size >= 6) return "Every Day Except Sunday"
        val names = mapOf(
            Calendar.SUNDAY to "Sun", Calendar.MONDAY to "Mon", Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed", Calendar.THURSDAY to "Thu", Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )
        val order = listOf(
            Calendar.SATURDAY, Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY
        )
        return order.filter { set.contains(it) }.joinToString(", ") { names[it] ?: "" }
    }
}
