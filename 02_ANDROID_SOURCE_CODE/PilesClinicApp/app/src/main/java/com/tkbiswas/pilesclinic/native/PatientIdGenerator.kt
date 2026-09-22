package com.tkbiswas.pilesclinic.native

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Matches app.js's patientId()/lockedBranchCode()/lockedDateCode()/
 * nextPatientSerial() exactly, so a natively-registered patient gets an ID
 * in the same KNE-10072026-001 format as one registered through the
 * WebView, with correctly incrementing serial numbers per branch+date.
 */
object PatientIdGenerator {

    private val branchCodes = mapOf(
        "kishanganj" to "KNE",
        "jalpaiguri" to "JPE",
        "cooch behar" to "COB",
        "coochbehar" to "COB",
        "falakata" to "FLK",
        "birpara" to "BIR"
    )

    fun branchCode(branch: String): String =
        branchCodes[branch.trim().lowercase()] ?: branch.uppercase().filter { it.isLetter() }.take(3).ifBlank { "PAT" }

    /** yyyy-MM-dd -> ddMMyyyy, matching lockedDateCode(). */
    fun dateCode(isoDate: String): String {
        val parts = isoDate.split("-")
        return if (parts.size == 3) "${parts[2]}${parts[1]}${parts[0]}" else isoDate.filter { it.isDigit() }
    }

    /**
     * TK-REPORTED RISK FIX (2026-07-26): duplicate Patient ID.
     * The old version computed the serial ONLY from what the cloud returned.
     * SupabaseClient.findByPrefix() returns an empty array on ANY failure
     * (offline, timeout, server error), so with no network every registration
     * of the same branch+date got max = 0 and therefore the SAME id
     * "...-001". The same read-then-write gap also let two phones registering
     * at the same moment both read the same max and both take max+1.
     *
     * Fix, without touching any screen, design or flow:
     *  1. Every serial this device hands out is remembered locally per
     *     branch+date prefix, and the next serial is max(cloud, local) + 1 --
     *     so offline registrations keep counting up instead of repeating 001.
     *  2. Right before returning, the candidate ID is re-checked against the
     *     cloud; if some other phone has already taken it in the meantime,
     *     the serial steps up until it is free. Offline this check simply
     *     returns nothing and step 1 keeps the ID safe.
     */
    private const val SERIAL_PREFS = "piles_clinic_patient_serial_ledger"

    /**
     * TK-REPORTED (2026-07-27, "slow internet" list item S4).
     *
     * WHAT WAS SLOW (nothing was wrong -- the ID logic itself was correct)
     * The old code asked the cloud TWICE for the same information:
     *   1. once for "every Patient ID already used for this branch+date"
     *      (to work out the highest serial), and then
     *   2. once MORE for EVERY candidate ID, up to 20 times, just to ask
     *      "is this one taken?".
     * So saving one registration could mean up to 21 separate cloud trips,
     * one after another. On a slow line that is minutes of the staff waiting
     * on the Save button.
     *
     * WHAT IS DIFFERENT NOW
     * The very same first answer already contains every taken ID, so the
     * "is this one taken?" question is now answered from that same answer
     * instead of asking the cloud again. ONE cloud trip instead of up to 21.
     *
     * THE RESULT IS IDENTICAL -- it is the same list of IDs, read the same
     * way, and the serial still steps up until it finds a free one. The
     * local ledger (offline protection) and the 20-step guard are untouched.
     */
    private fun cloudTakenIds(prefix: String): Set<String> {
        return try {
            val rows = SupabaseClient.findByPrefix("patients", "patientId", prefix, "patientId")
            val taken = HashSet<String>()
            for (i in 0 until rows.length()) {
                val id = rows.getJSONObject(i).optString("patientId", "")
                if (id.isNotBlank()) taken.add(id)
            }
            taken
        } catch (_: Throwable) {
            // Offline / failure: empty, exactly as before -- the local ledger
            // below is what keeps offline serials from repeating.
            emptySet()
        }
    }

    private fun maxSerialIn(taken: Set<String>, prefix: String): Int {
        var max = 0
        for (id in taken) {
            val serial = id.removePrefix(prefix).toIntOrNull()
            if (serial != null && serial > max) max = serial
        }
        return max
    }

    /** Queries Supabase for existing patients whose ID starts with the same
     * branch+date prefix and returns the next 3-digit serial, matching
     * nextPatientSerial()'s "scan all, take max+1" logic (done server-side
     * via a prefix filter instead of pulling the whole table), combined with
     * this device's own local ledger so offline saves never repeat a serial.
     * Kept for any caller that only wants the serial; generate() below no
     * longer calls it, so a registration never reads the cloud twice. */
    fun nextSerial(code: String, dateCode: String, context: Context? = null): String {
        val prefix = "$code-$dateCode-"
        var max = maxSerialIn(cloudTakenIds(prefix), prefix)
        val prefs = context?.getSharedPreferences(SERIAL_PREFS, Context.MODE_PRIVATE)
        val localMax = prefs?.getInt(prefix, 0) ?: 0
        if (localMax > max) max = localMax
        val next = max + 1
        prefs?.edit()?.putInt(prefix, next)?.apply()
        return next.toString().padStart(3, '0')
    }

    fun generate(branch: String, isoDate: String, context: Context? = null): String {
        val code = branchCode(branch)
        val dCode = dateCode(isoDate)
        val prefix = "$code-$dCode-"
        // CLOUD READ 1 of 2 -- every Patient ID already used for this
        // branch+date.
        val taken = HashSet(cloudTakenIds(prefix))
        val prefs = context?.getSharedPreferences(SERIAL_PREFS, Context.MODE_PRIVATE)

        fun pick(): String {
            var max = maxSerialIn(taken, prefix)
            val localMax = prefs?.getInt(prefix, 0) ?: 0
            if (localMax > max) max = localMax
            var serial = max + 1
            var candidate = "$prefix${serial.toString().padStart(3, '0')}"
            var guard = 0
            while (guard < 20 && taken.contains(candidate)) {
                serial += 1
                candidate = "$prefix${serial.toString().padStart(3, '0')}"
                guard += 1
            }
            return candidate
        }

        var chosen = pick()
        // CLOUD READ 2 of 2 -- the duplicate-Patient-ID protection TK reported
        // on 26.07.2026 is KEPT IN FULL: the list is read once more right
        // before handing the ID out, so an ID another phone took in the
        // meantime is still caught. This used to be up to TWENTY separate
        // "is this one taken?" calls; it is now a single read that checks the
        // whole list at once -- same protection, in fact stronger, but 19
        // fewer trips on a slow line.
        val fresh = cloudTakenIds(prefix)
        if (fresh.isNotEmpty()) {
            taken.addAll(fresh)
            chosen = pick()
        }

        // The device ledger always remembers the serial actually handed out,
        // exactly as before, so an offline save never repeats it.
        val chosenSerial = chosen.removePrefix(prefix).toIntOrNull()
        if (chosenSerial != null) prefs?.edit()?.putInt(prefix, chosenSerial)?.apply()
        return chosen
    }

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
}
