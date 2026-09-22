package com.tkbiswas.pilesclinic.native

import org.json.JSONArray
import org.json.JSONObject

/**
 * TK-REQUESTED (2026-07-22): a small, shared "retry this field update
 * later" queue for the several scattered edit dialogs (Bill correction,
 * Paid Amount, Estimated Amount, payment amount/mode edits, follow-up
 * remark edits, etc.) that previously had NO retry at all if the cloud
 * call failed at that moment -- unlike the app's main save flows
 * (Enquiry/Registration/Payment/Follow-up/Mark Arrived/Doctor Checkup/
 * Briefing), which already retry via BottomNav.wire().
 *
 * IMPORTANT: this does NOT change anything the user sees. Every call site
 * keeps showing exactly the same "Failed — retry" message it always did --
 * this only adds a silent background safety net so a queued update also
 * gets a second (third, fourth...) chance the next time any screen opens
 * on this device, the same way the other repositories' queues already
 * work. No UI, no design, no existing working flow touched.
 */
object GenericUpdateQueue {
    private val LOCK = Any()

    private fun prefs(context: android.content.Context) =
        context.getSharedPreferences("piles_clinic_generic_update_pending", android.content.Context.MODE_PRIVATE)

    /** Queue one field-update to retry later. Safe to call as often as
     *  needed -- flushPending() below simply replays each queued entry
     *  with a plain upsert-by-id (idempotent), so retrying an
     *  already-applied update is harmless. */
    fun queue(context: android.content.Context, table: String, id: String, fields: JSONObject) {
        if (table.isBlank() || id.isBlank()) return
        synchronized(LOCK) {
            val p = prefs(context)
            val arr = try { JSONArray(p.getString("queue", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            arr.put(JSONObject().put("table", table).put("id", id).put("fields", fields))
            p.edit().putString("queue", arr.toString()).commit()
        // TK-REQUESTED (2026-07-25): the moment anything is queued, ask
        // WorkManager to sync right away. WorkManager runs even when the
        // app is closed or the staff switched to another app, so a save
        // no longer waits for the next screen-open or the 15-minute
        // backstop . it reaches the cloud within seconds of the network
        // being available. Nothing else changes; the same proven
        // flushPending() work runs, just sooner.
        try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(context) } catch (_: Throwable) { }

        }
    }

    /** V378: remove only obsolete field snapshots for one row. A Referral
     * delete must not later be undone by an older queued full JSON array, but
     * unrelated pending fields (callHistory, remarks, dates, etc.) must stay. */
    fun discardFields(context: android.content.Context, table: String, id: String, fieldNames: Set<String>) {
        if (table.isBlank() || id.isBlank() || fieldNames.isEmpty()) return
        synchronized(LOCK) {
            val p = prefs(context)
            val arr = try { JSONArray(p.getString("queue", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            val kept = JSONArray()
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                if (e.optString("table") != table || e.optString("id") != id) { kept.put(e); continue }
                val fields = e.optJSONObject("fields") ?: JSONObject()
                fieldNames.forEach { fields.remove(it) }
                if (fields.length() > 0) { e.put("fields", fields); kept.put(e) }
            }
            p.edit().putString("queue", kept.toString()).commit()
        }
    }

    /** Retries every queued update still stuck on this device. Called from
     *  BottomNav.wire() on every screen open, same pattern as the other
     *  repositories' flushPending(). Does nothing (no network call at all)
     *  once everything is caught up. */
    fun flushPending(context: android.content.Context) {
        synchronized(LOCK) {
            val p = prefs(context)
            val arr = try { JSONArray(p.getString("queue", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
            if (arr.length() == 0) return
            val stillPending = JSONArray()
            for (i in 0 until arr.length()) {
                val e = arr.optJSONObject(i) ?: continue
                val table = e.optString("table")
                val id = e.optString("id")
                val fields = e.optJSONObject("fields") ?: JSONObject()
                // TK-REQUESTED (2026-07-26): drop a queued correction whose row
                // has since been deleted (a PATCH on a deleted row reports
                // success anyway, so it must not be retried forever either).
                if (DeletedGuard.isDeleted(table, id, context)) continue
                val ok = try { SupabaseClient.updateById(table, id, fields) } catch (_: Throwable) { false }
                if (!ok) stillPending.put(e)
            }
            p.edit().putString("queue", stillPending.toString()).commit()
        }
    }
}
