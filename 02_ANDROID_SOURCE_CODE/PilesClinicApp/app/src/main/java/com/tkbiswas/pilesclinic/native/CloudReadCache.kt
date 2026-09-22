package com.tkbiswas.pilesclinic.native

import org.json.JSONArray

/**
 * TK-REPORTED (2026-07-27): "ডাটা লোড হতে প্রচুর সময় লাগে — এত স্লো হলে
 * অ্যাপ্লিকেশনটা ব্যবহার করব কিভাবে?"
 *
 * WHAT WAS WRONG
 * Opening the Follow-up screen starts FOUR loads within the same second:
 * three to work out the Enquiry / Visit / Patient tab numbers, and a fourth
 * for the list actually being shown. Two of those loads each pull the WHOLE
 * "patients" table AND the WHOLE "payments" table -- and the fourth pulls
 * them again. So the same two big tables were being downloaded up to three
 * times over, at the same moment, on the same thin connection. On TK's line
 * (the screenshots show 0.16-2.00 KB/s) that alone is minutes of "Loading...".
 *
 * WHAT THIS DOES
 * Remembers the answer to an expensive read for a FEW SECONDS, and -- just as
 * importantly -- makes callers that ask for the same thing AT THE SAME MOMENT
 * share ONE download instead of starting their own. Same data, same code
 * reading it, simply not fetched three times over.
 *
 * WHY THE WINDOW IS SO SHORT
 * TK's rule is that money and remarks written on one screen must show
 * correctly on every other screen. A long cache would break that. Twenty
 * seconds is deliberately just long enough to cover the burst of loads that
 * happens when a screen opens, and far too short to ever show yesterday's --
 * or even last minute's -- figures.
 *
 * SAFETY
 *  - A FAILED read is never remembered. If a fetch comes back null (bad
 *    line, server error), nothing is stored, so the very next attempt goes to
 *    the cloud again exactly as before.
 *  - The stored value is only ever READ by callers, never modified.
 *  - If anything at all goes wrong here, the caller still gets the normal
 *    result of its own fetch -- this can never block or break a load.
 */
object CloudReadCache {

    /** How long an answer stays usable. Deliberately very short. */
    private const val TTL_MS = 20_000L

    private class Entry(val at: Long, val data: JSONArray)

    private val mapLock = Any()
    private val entries = HashMap<String, Entry>()
    private val keyLocks = HashMap<String, Any>()

    private fun peek(key: String): JSONArray? {
        synchronized(mapLock) {
            val e = entries[key] ?: return null
            if (System.currentTimeMillis() - e.at > TTL_MS) {
                entries.remove(key)
                return null
            }
            return e.data
        }
    }

    private fun lockFor(key: String): Any = synchronized(mapLock) {
        keyLocks.getOrPut(key) { Any() }
    }

    /**
     * Returns a recent answer for [key] if there is one, otherwise runs
     * [fetch] once. Callers asking for the same [key] at the same time wait
     * for that single fetch instead of each starting their own.
     */
    fun get(key: String, fetch: () -> JSONArray?): JSONArray? {
        peek(key)?.let { return it }
        return try {
            synchronized(lockFor(key)) {
                // Another thread may have filled it while this one waited.
                peek(key)?.let { return it }
                val fresh = fetch()
                if (fresh != null) {
                    synchronized(mapLock) { entries[key] = Entry(System.currentTimeMillis(), fresh) }
                }
                fresh
            }
        } catch (_: Throwable) {
            // Last resort: behave exactly as if this cache did not exist.
            try { fetch() } catch (_: Throwable) { null }
        }
    }

    /** Forgets everything. Called after a write, so a fresh read follows. */
    fun clear() {
        synchronized(mapLock) { entries.clear() }
    }
}
