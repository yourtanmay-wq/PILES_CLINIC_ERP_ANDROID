package com.tkbiswas.pilesclinic.clinical

import android.content.Context
import com.tkbiswas.pilesclinic.native.BackgroundWork
import com.tkbiswas.pilesclinic.native.SupabaseClient
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Permanent, very small cloud copy of the medicine defaults.
 *
 * The screen continues to read SharedPreferences, so it opens at the same speed
 * and keeps working offline.  This repository only refreshes that local cache
 * occasionally and quietly writes a changed medicine behind the screen.
 */
object MedicineDefaultsCloudRepository {
    private const val TABLE = "medicine_defaults"
    private const val PREF = "medicine_defaults_cloud_state"
    private const val LAST_REFRESH = "last_refresh_ms"
    private const val REFRESH_AFTER_MS = 15L * 60L * 1000L
    private val refreshing = AtomicBoolean(false)

    private fun nowIso(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

    private fun stableId(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT)
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return "rx_" + digest.take(16).joinToString("") { "%02x".format(it) }
    }

    /** Local first; one small cloud upsert runs behind the screen and uses the
     * shared retry safety net if the connection is temporarily unavailable. */
    fun rememberAndSync(
        context: Context,
        name: String,
        type: String,
        dose: String,
        whenText: String,
        days: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val updatedAt = nowIso()
        ClinicalRepository.applyPermanentDefault(
            cleanName, type.trim(), dose.trim(), whenText.trim(), days.trim(), updatedAt
        )
        val row = JSONObject()
            .put("id", stableId(cleanName))
            .put("name", cleanName)
            .put("medicineType", type.trim())
            .put("dose", dose.trim())
            .put("whenText", whenText.trim())
            .put("days", days.trim())
            .put("updatedAt", updatedAt)
        BackgroundWork.run { SupabaseClient.upsert(TABLE, row) }
    }

    /**
     * At most one tiny read every 15 minutes while a medicine screen is used.
     * Cached values remain usable if the internet/table is unavailable.
     */
    fun refreshIfNeeded(context: Context, force: Boolean = false, onUpdated: (() -> Unit)? = null) {
        val app = context.applicationContext
        val state = app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val fresh = System.currentTimeMillis() - state.getLong(LAST_REFRESH, 0L) < REFRESH_AFTER_MS
        if (!force && fresh) return
        if (!refreshing.compareAndSet(false, true)) return
        BackgroundWork.run {
            var changed = false
            try {
                val rows = SupabaseClient.fetchListOrNull(
                    TABLE,
                    limit = 500,
                    order = "updatedAt.desc.nullslast",
                    select = "id,name,medicineType,dose,whenText,days,updatedAt"
                )
                if (rows != null) {
                    for (i in 0 until rows.length()) {
                        val row = rows.optJSONObject(i) ?: continue
                        val name = row.optString("name").trim()
                        if (name.isBlank()) continue
                        changed = ClinicalRepository.applyPermanentDefault(
                            name = name,
                            type = row.optString("medicineType"),
                            dose = row.optString("dose"),
                            whenText = row.optString("whenText"),
                            days = row.optString("days"),
                            updatedAt = row.optString("updatedAt")
                        ) || changed
                    }
                    state.edit().putLong(LAST_REFRESH, System.currentTimeMillis()).apply()
                }
            } catch (_: Throwable) {
                // Offline or setup not run: keep the proven local defaults.
            } finally {
                refreshing.set(false)
            }
            if (changed && onUpdated != null) {
                try { android.os.Handler(android.os.Looper.getMainLooper()).post { onUpdated() } } catch (_: Throwable) { }
            }
        }
    }
}
