package com.tkbiswas.pilesclinic.clinical

import com.tkbiswas.pilesclinic.native.SupabaseClient
import com.tkbiswas.pilesclinic.native.s
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * ROOT-CAUSE FIX for the previously in-memory clinical modules.
 *
 * ClinicalRepository is session-only (cleared on app restart). This repository
 * PERMANENTLY persists each clinical record (Doctor Checkup / Prescription /
 * Investigation / Diet) to the live Supabase "medical" table, linked to the
 * current patient — matching the WebView's saveMedicalRecord(id,type,selected,
 * details). Fields follow the medical table schema in app.js.
 *
 * The in-memory ClinicalRepository is still used for the on-screen working copy
 * and the print hand-off; this repository adds the durable save on top of it.
 */
object ClinicalCloudRepository {

    enum class SameDayPrescriptionCheck { EXISTS, NONE, UNVERIFIED }

    // TK-REQUESTED FIX (2026-07-19): same singleton-shared-lock fix as
    // ChamberAttendanceRepository -- protects saveMedical()'s queue write
    // from racing with flushPending()'s read-modify-write.
    private val LOCK = Any()

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /**
     * Checks the phone first, then the cloud, before another Prescription is
     * saved for the same patient today.  A cloud failure is NOT treated as
     * "none found"; callers can safely stop instead of silently duplicating.
     * Only id is downloaded and limit=1, keeping the verification tiny.
     */
    fun checkSameDayPrescription(context: android.content.Context, patientId: String): SameDayPrescriptionCheck {
        if (patientId.isBlank()) return SameDayPrescriptionCheck.UNVERIFIED
        val day = today()
        val local = com.tkbiswas.pilesclinic.native.LocalWorkflowStore(context).medicalForPatient(patientId)
        for (i in 0 until local.length()) {
            val row = local.optJSONObject(i) ?: continue
            if (row.optString("type").equals("Prescription", ignoreCase = true) &&
                row.optString("date").take(10) == day) {
                return SameDayPrescriptionCheck.EXISTS
            }
        }
        val encPid = java.net.URLEncoder.encode(patientId, "UTF-8")
        val cloud = try {
            SupabaseClient.fetchListOrNull(
                "medical",
                "patientId=eq.$encPid&type=eq.Prescription&date=eq.$day",
                1,
                select = "id"
            )
        } catch (_: Throwable) { null }
        return when {
            cloud == null -> SameDayPrescriptionCheck.UNVERIFIED
            cloud.length() > 0 -> SameDayPrescriptionCheck.EXISTS
            else -> SameDayPrescriptionCheck.NONE
        }
    }

    /**
     * Saves one clinical record. Runs a blocking network call, so callers MUST
     * invoke it from a background thread (Dispatchers.IO). Returns true on success.
     *
     * TK-FOUND RISK FIX (2026-07-18): this used to be a single direct
     * network call with no offline protection at all -- a weak connection
     * meant the clinical record was just silently lost, no retry, nothing
     * saved anywhere. Now: saved to this device FIRST (so it's never lost
     * even offline), then the cloud is tried; on failure it's queued and
     * retried automatically (same pattern as Registration/Enquiry/Payment/
     * Chamber Attendance). Always returns true once the local save
     * succeeds -- the caller shouldn't show "failed" for something that
     * genuinely is saved and will sync.
     *
     * @param patientId  the current patient's id/patientId (from RoleSession)
     * @param patientName the current patient's name (from RoleSession)
     * @param type       "Doctor Checkup" | "Prescription" | "Investigation" | "Diet Chart"
     * @param selected   comma-joined selected items (medicines/tests/diet), or ""
     * @param details    free-text details (checkup notes / dose / remarks)
     * @param createdByMobile the logged-in user's mobile
     */
    fun saveMedical(
        context: android.content.Context,
        patientId: String,
        patientName: String,
        type: String,
        selected: String,
        details: String,
        createdByMobile: String,
        photos: String = "",
        // 🟢🔒 V676 (২৫.০৮.২০২৬, TK-নির্দেশ) — আজকের নিজের Doctor Checkup
        // এডিট করলে এই একই id-তে upsert হবে (নতুন সারি নয়, পুরনোটাই বদলে
        // যাবে)। ⛔ ফাঁকা রাখলে (ডিফল্ট) আগের মতোই সবসময় নতুন id — কোনো
        // পুরনো caller (Prescription/Investigation/Diet) এতে ছোঁয়া হয়নি।
        existingId: String = ""
    ): Boolean {
        val now = isoNow()
        val row = JSONObject()
            .put("id", existingId.ifBlank { "med_" + UUID.randomUUID().toString().replace("-", "") })
            .put("patientId", patientId)
            .put("name", patientName)
            .put("type", type)
            .put("date", today())
            .put("selected", selected)
            .put("details", details)
            .put("createdBy", createdByMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
        if (photos.isNotBlank()) row.put("photos", photos)

        val localStore = com.tkbiswas.pilesclinic.native.LocalWorkflowStore(context)
        localStore.upsertMedical(row) // PENDING -- visible in Patient History on this device now
        val ok = try { SupabaseClient.upsert("medical", row) } catch (_: Throwable) { false }
        if (ok) {
            localStore.upsertMedical(row, "SYNCED")
        } else {
            synchronized(LOCK) {
            val prefs = context.getSharedPreferences("piles_clinic_medical_pending", android.content.Context.MODE_PRIVATE)
            val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
            queue.put(row)
            prefs.edit().putString("queue", queue.toString()).commit()
            }
            // 🚨 TK'S ORDER (27.07.2026): "স্টাফ অ্যাপ থেকে বেরিয়ে গেলেও যেন
            // ফোন থেকে ডেটা Supabase-এ চলে যায়।" Six other queues (Enquiry,
            // Registration, Payment, Follow-up, Chamber, corrections) already
            // ask WorkManager to upload right away, which keeps working with
            // the app closed. This one -- Doctor Check-up / Prescription /
            // Diet Chart / Investigation -- was left out, so an unsent record
            // here waited for the next screen-open. Same one-line trigger,
            // same proven flushPending() work, nothing else changed.
            try { com.tkbiswas.pilesclinic.data.sync.SyncScheduler.syncNow(context) } catch (_: Throwable) { }
        }
        return true
    }

    /** Old signature, kept in case anything else references it -- same
     *  no-offline-protection behavior as before. Prefer the context version. */
    fun saveMedical(
        patientId: String,
        patientName: String,
        type: String,
        selected: String,
        details: String,
        createdByMobile: String,
        photos: String = ""
    ): Boolean {
        val now = isoNow()
        val row = JSONObject()
            .put("id", "med_" + UUID.randomUUID().toString().replace("-", ""))
            .put("patientId", patientId)
            .put("name", patientName)
            .put("type", type)
            .put("date", today())
            .put("selected", selected)
            .put("details", details)
            .put("createdBy", createdByMobile)
            .put("createdAt", now)
            .put("updatedAt", now)
        if (photos.isNotBlank()) row.put("photos", photos)
        return SupabaseClient.upsert("medical", row)
    }

    /** Retries every clinical record still stuck on this device. Called
     *  from BottomNav.wire() on every screen open, same as everything else.
     *  Does nothing (no network call at all) once the queue is empty. */
    fun flushPending(context: android.content.Context) {
        synchronized(LOCK) {
        val prefs = context.getSharedPreferences("piles_clinic_medical_pending", android.content.Context.MODE_PRIVATE)
        val queue = try { org.json.JSONArray(prefs.getString("queue", "[]") ?: "[]") } catch (_: Exception) { org.json.JSONArray() }
        if (queue.length() == 0) return
        val stillPending = org.json.JSONArray()
        for (i in 0 until queue.length()) {
            val row = queue.optJSONObject(i) ?: continue
            // TK-REQUESTED (2026-07-26): never re-create a deleted record.
            if (com.tkbiswas.pilesclinic.native.DeletedGuard.isDeleted("medical", row.optString("id", ""), context)) continue
            val ok = try { SupabaseClient.upsert("medical", row) } catch (_: Throwable) { false }
            if (ok) {
                com.tkbiswas.pilesclinic.native.LocalWorkflowStore(context).upsertMedical(row, "SYNCED")
            } else {
                stillPending.put(row)
            }
        }
        prefs.edit().putString("queue", stillPending.toString()).commit()
        }
    }

    /**
     * Loads a patient's saved clinical records from the "medical" table (newest
     * first) so Patient History survives app restarts — the previous version
     * read the in-memory session list, which was empty after any relaunch.
     *
     * TK-FOUND RISK FIX (2026-07-18): now also merges any locally-pending
     * record for this patient (not yet synced), so a just-saved Checkup/
     * Prescription/etc. shows up immediately even if it's still offline —
     * matching the same "merge locally-pending" pattern already used for
     * Visit Card / Draft / Global Search / Full Journey.
     */
    fun loadMedical(patientId: String, context: android.content.Context? = null): List<ClinicalVisit> {
        if (patientId.isBlank()) return emptyList()
        val enc = java.net.URLEncoder.encode(patientId, "UTF-8")
        // 🔴🔒 V794 — ছবি ছাড়া (`photos` কেউ পড়ে না; বিশদ SupabaseClient.MEDICAL_COLS)
        val rows = try { SupabaseClient.fetchListSlim("medical", "patientId=eq.$enc", 500,
                            SupabaseClient.MEDICAL_COLS) }
        catch (e: Exception) { org.json.JSONArray() }
        return buildFromRows(rows, patientId, context)
    }

    // TK-REQUESTED (2026-07-24): raw-fetch split out so
    // PatientClinicalHistoryActivity can cache these rows per patientId
    // (SharedPreferences, same pattern as Trash/Briefing) and show them
    // instantly next time -- loadMedical() above still behaves exactly as
    // before for any other caller.
    fun loadMedicalRaw(patientId: String): org.json.JSONArray {
        if (patientId.isBlank()) return org.json.JSONArray()
        val enc = java.net.URLEncoder.encode(patientId, "UTF-8")
        return try { SupabaseClient.fetchListSlim("medical", "patientId=eq.$enc", 500,
                        SupabaseClient.MEDICAL_COLS) }   // 🔴🔒 V794 — ছবি ছাড়া
        catch (e: Exception) { org.json.JSONArray() }
    }

    // 🔵 TK-ORDER (07.08.2026): loadMedicalRaw-এর মতোই, তবে পড়া **ব্যর্থ হলে null**
    // ফেরে (fetchListOrNull) — "সত্যিই কোনো রেকর্ড নেই" আর "পড়া হলোই না" আলাদা বোঝা
    // যায়। এতে History স্ক্রিন ব্যর্থ পড়াকে খালি ধরে **ভালো cache-এর ওপর খালি বসাবে
    // না** (আগে বসাত → পরের বারও ফাঁকা)। ⛔ একই একটাই cloud-read (Supabase free-plan-এ
    // বাড়তি কিছু নয়); পুরনো loadMedicalRaw/loadMedical এক অক্ষরও বদলায়নি।
    fun loadMedicalRawOrNull(patientId: String): org.json.JSONArray? {
        if (patientId.isBlank()) return org.json.JSONArray()   // ফাঁকা id = "রেকর্ড নেই" (ব্যর্থতা নয়)
        val enc = java.net.URLEncoder.encode(patientId, "UTF-8")
        // 🔴🔒 V794 — ছবি ছাড়া
        return try { SupabaseClient.fetchListSlimOrNull("medical", "patientId=eq.$enc", 500,
                        SupabaseClient.MEDICAL_COLS) }
        catch (e: Exception) { null }
    }

    fun buildFromRows(rows: org.json.JSONArray, patientId: String, context: android.content.Context? = null): List<ClinicalVisit> {
        val seenIds = HashSet<String>()
        val out = ArrayList<ClinicalVisit>()
        fun addRow(r: JSONObject) {
            val id = r.optString("id", UUID.randomUUID().toString())
            if (!seenIds.add(id)) return
            val selected = r.s("selected")
            val details = r.s("details")
            val summary = listOf(selected, details).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { "-" }
            out.add(
                ClinicalVisit(
                    id = id,
                    patientName = r.s("name"),
                    type = r.s("type").ifBlank { "Record" },
                    summary = summary,
                    timestamp = parseTs(r.s("createdAt").ifBlank { r.s("date") }),
                    doneByRole = UserRole.STAFF
                )
            )
        }
        for (i in 0 until rows.length()) addRow(rows.getJSONObject(i))
        context?.let { ctx ->
            val pending = com.tkbiswas.pilesclinic.native.LocalWorkflowStore(ctx).medicalForPatient(patientId)
            for (i in 0 until pending.length()) addRow(pending.getJSONObject(i))
        }
        return out.sortedByDescending { it.timestamp }
    }

    private fun parseTs(s: String): Long {
        if (s.isBlank()) return 0L
        val fmts = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")
        for (f in fmts) {
            try { return SimpleDateFormat(f, Locale.US).parse(s.take(f.length + 4))?.time ?: continue }
            catch (_: Exception) { }
        }
        return 0L
    }
}
