package com.tkbiswas.pilesclinic.native

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

/**
 * TK-REPORTED (2026-07-27): "ইন্টারনেট স্পিড কম থাকলেও অ্যাপ যেন কোথাও থেমে
 * না থাকে।"
 *
 * WHAT THIS IS FOR
 * Several places in this app had to touch the cloud once per row / once per
 * patient, and did it strictly one-after-another. On a good line nobody
 * notices. On a slow line each call can take several seconds, so twenty rows
 * meant the staff stared at a dead-looking screen for minutes.
 *
 * This helper sends a SMALL GROUP of those same calls at the same time and
 * waits for the group, then the next group. Nothing about WHAT is sent
 * changes -- same queries, same order of the results, same total number of
 * cloud calls (so Supabase quota is completely unaffected) -- only the
 * waiting gets shorter.
 *
 * WHY A GROUP AND NOT ALL AT ONCE
 * OkHttp keeps a limited number of live connections per server, and a phone
 * on a weak signal does worse, not better, when it is flooded. A small fixed
 * group is the safe middle: fast enough to fix the freeze, gentle enough that
 * it can never make a weak connection worse.
 *
 * SAFETY
 *  - Every single task is wrapped in its own try/catch: one failure can never
 *    cancel the others and can never crash the caller.
 *  - Must be called from a background thread (every current caller already
 *    runs on Dispatchers.IO or a plain Thread), exactly like the existing
 *    runBlocking blocks in DraftRepository / ReportsRepository /
 *    PatientTimelineRepository.
 */
object ParallelCloud {

    /** How many cloud calls are allowed to be in flight together. */
    private const val GROUP = 6

    /**
     * Runs [work] for every item, a few at a time, and returns the results in
     * the SAME ORDER as [items]. A task that throws contributes null.
     */
    fun <T, R> map(items: List<T>, work: (T) -> R): List<R?> {
        if (items.isEmpty()) return emptyList()
        if (items.size == 1) {
            return listOf(try { work(items[0]) } catch (_: Throwable) { null })
        }
        return try {
            runBlocking {
                val out = ArrayList<R?>(items.size)
                for (group in items.chunked(GROUP)) {
                    val done = coroutineScope {
                        group.map { item ->
                            async(Dispatchers.IO) {
                                try { work(item) } catch (_: Throwable) { null }
                            }
                        }.map { it.await() }
                    }
                    out.addAll(done)
                }
                out
            }
        } catch (_: Throwable) {
            // Absolute last resort: fall back to the old one-at-a-time
            // behaviour so a problem here can never stop the work happening.
            items.map { try { work(it) } catch (_: Throwable) { null } }
        }
    }

    /**
     * Runs [work] for every item, a few at a time, and returns how many
     * returned true. Used where the caller only needs a success count.
     */
    fun <T> runAll(items: List<T>, work: (T) -> Boolean): Int =
        map(items, work).count { it == true }
}
