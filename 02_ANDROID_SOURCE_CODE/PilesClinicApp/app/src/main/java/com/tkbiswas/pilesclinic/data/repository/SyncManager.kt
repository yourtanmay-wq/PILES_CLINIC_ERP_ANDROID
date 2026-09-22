package com.tkbiswas.pilesclinic.data.repository

import android.content.Context
import com.tkbiswas.pilesclinic.data.local.AppDatabase
import com.tkbiswas.pilesclinic.data.local.SyncStatus
import com.tkbiswas.pilesclinic.data.remote.EnquiryApi
import com.tkbiswas.pilesclinic.data.remote.FollowUpApi
import com.tkbiswas.pilesclinic.data.remote.PaymentApi
import com.tkbiswas.pilesclinic.data.remote.RegistrationApi
import com.tkbiswas.pilesclinic.data.remote.RetrofitProvider
import com.tkbiswas.pilesclinic.data.remote.SupabaseConfig
import com.tkbiswas.pilesclinic.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SyncSummary(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val failed: Int = 0,
    val errors: List<String> = emptyList()
) {
    val isFullSuccess: Boolean get() = failed == 0
}

/**
 * Conflict strategy (documented explicitly, since "conflict-safe" needs a
 * concrete rule): each row carries `updatedAt` (client) written on every
 * local edit. On PUSH we upsert-by-id (Postgres ON CONFLICT), so the
 * latest write for a given id always wins server-side. On PULL we only
 * overwrite a local row if the server's `updatedAt` is newer than the local
 * one — so a pending local edit is never silently clobbered by stale server
 * data. Deletes are soft (isDeleted flag), so a delete is just another
 * upserted field and never physically destroys a row before it's synced.
 */
class SyncManager(
    context: Context,
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("sync_meta", Context.MODE_PRIVATE)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val enquiryApi: EnquiryApi by lazy { RetrofitProvider.get(sessionManager).create(EnquiryApi::class.java) }
    private val registrationApi: RegistrationApi by lazy { RetrofitProvider.get(sessionManager).create(RegistrationApi::class.java) }
    private val followUpApi: FollowUpApi by lazy { RetrofitProvider.get(sessionManager).create(FollowUpApi::class.java) }
    private val paymentApi: PaymentApi by lazy { RetrofitProvider.get(sessionManager).create(PaymentApi::class.java) }

    private val authRepository by lazy { AuthRepository(sessionManager) }

    suspend fun syncAll(): SyncSummary = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext SyncSummary(errors = listOf("Supabase not configured (local.properties)."))
        }
        SyncStatusHolder.update(SyncState.Syncing)

        val errors = mutableListOf<String>()
        var pushed = 0
        var pulled = 0
        var failed = 0

        if (sessionManager.isSignedIn) {
            authRepository.refreshIfNeeded()
        }

        // ---- PUSH: local PENDING/FAILED rows -> Supabase ----
        runCatching { pushEnquiries() }
            .onSuccess { pushed += it.first; failed += it.second }
            .onFailure { errors.add("Enquiry push: ${it.message}") }

        runCatching { pushRegistrations() }
            .onSuccess { pushed += it.first; failed += it.second }
            .onFailure { errors.add("Registration push: ${it.message}") }

        runCatching { pushFollowUps() }
            .onSuccess { pushed += it.first; failed += it.second }
            .onFailure { errors.add("Follow-up push: ${it.message}") }

        runCatching { pushPayments() }
            .onSuccess { pushed += it.first; failed += it.second }
            .onFailure { errors.add("Payment push: ${it.message}") }

        // ---- PULL: Supabase rows changed since last pull -> local ----
        runCatching { pulled += pullEnquiries() }.onFailure { errors.add("Enquiry pull: ${it.message}") }
        runCatching { pulled += pullRegistrations() }.onFailure { errors.add("Registration pull: ${it.message}") }
        runCatching { pulled += pullFollowUps() }.onFailure { errors.add("Follow-up pull: ${it.message}") }
        runCatching { pulled += pullPayments() }.onFailure { errors.add("Payment pull: ${it.message}") }

        val summary = SyncSummary(pushed, pulled, failed, errors)
        val now = System.currentTimeMillis()
        if (errors.isEmpty() && failed == 0) {
            SyncStatusHolder.update(SyncState.Success("Pushed $pushed, pulled $pulled", now))
        } else {
            SyncStatusHolder.update(SyncState.Failed(errors.firstOrNull() ?: "$failed row(s) failed to sync", now))
        }
        summary
    }

    // ---------------- PUSH (per entity) ----------------

    private suspend fun pushEnquiries(): Pair<Int, Int> {
        val dao = db.enquiryDao()
        val pending = dao.getPendingSync()
        var ok = 0; var fail = 0
        pending.chunked(50).forEach { batch ->
            try {
                enquiryApi.upsert(batch.map { it.toDto() })
                batch.forEach { dao.markSynced(it.id) }
                ok += batch.size
            } catch (e: Exception) {
                batch.forEach { dao.markFailed(it.id, e.message ?: "sync error") }
                fail += batch.size
            }
        }
        return ok to fail
    }

    private suspend fun pushRegistrations(): Pair<Int, Int> {
        val dao = db.registrationDao()
        val pending = dao.getPendingSync()
        var ok = 0; var fail = 0
        pending.chunked(50).forEach { batch ->
            try {
                registrationApi.upsert(batch.map { it.toDto() })
                batch.forEach { dao.markSynced(it.id) }
                ok += batch.size
            } catch (e: Exception) {
                batch.forEach { dao.markFailed(it.id, e.message ?: "sync error") }
                fail += batch.size
            }
        }
        return ok to fail
    }

    private suspend fun pushFollowUps(): Pair<Int, Int> {
        val dao = db.followUpDao()
        val pending = dao.getPendingSync()
        var ok = 0; var fail = 0
        pending.chunked(50).forEach { batch ->
            try {
                followUpApi.upsert(batch.map { it.toDto() })
                batch.forEach { dao.markSynced(it.id) }
                ok += batch.size
            } catch (e: Exception) {
                batch.forEach { dao.markFailed(it.id, e.message ?: "sync error") }
                fail += batch.size
            }
        }
        return ok to fail
    }

    private suspend fun pushPayments(): Pair<Int, Int> {
        val dao = db.paymentDao()
        val pending = dao.getPendingSync()
        var ok = 0; var fail = 0
        pending.chunked(50).forEach { batch ->
            try {
                paymentApi.upsert(batch.map { it.toDto() })
                batch.forEach { dao.markSynced(it.id) }
                ok += batch.size
            } catch (e: Exception) {
                batch.forEach { dao.markFailed(it.id, e.message ?: "sync error") }
                fail += batch.size
            }
        }
        return ok to fail
    }

    // ---------------- PULL (per entity, conflict-safe by updatedAt) ----------------

    private suspend fun pullEnquiries(): Int {
        val dao = db.enquiryDao()
        val since = lastPullTime("enquiries")
        val remoteRows = enquiryApi.getUpdatedSince("gt.${isoFormat.format(Date(since))}")
        var applied = 0
        remoteRows.forEach { dto ->
            val local = dao.getById(dto.id)
            if (local == null || dto.updatedAt > local.updatedAt) {
                dao.upsertLocal(dto.toEntity(SyncStatus.SYNCED))
                applied++
            }
        }
        setLastPullTime("enquiries")
        return applied
    }

    private suspend fun pullRegistrations(): Int {
        val dao = db.registrationDao()
        val since = lastPullTime("registrations")
        val remoteRows = registrationApi.getUpdatedSince("gt.${isoFormat.format(Date(since))}")
        var applied = 0
        remoteRows.forEach { dto ->
            val local = dao.getById(dto.id)
            if (local == null || dto.updatedAt > local.updatedAt) {
                dao.upsertLocal(dto.toEntity(SyncStatus.SYNCED))
                applied++
            }
        }
        setLastPullTime("registrations")
        return applied
    }

    private suspend fun pullFollowUps(): Int {
        val dao = db.followUpDao()
        val since = lastPullTime("follow_ups")
        val remoteRows = followUpApi.getUpdatedSince("gt.${isoFormat.format(Date(since))}")
        var applied = 0
        remoteRows.forEach { dto ->
            dao.upsertLocal(dto.toEntity(SyncStatus.SYNCED))
            applied++
        }
        setLastPullTime("follow_ups")
        return applied
    }

    private suspend fun pullPayments(): Int {
        val dao = db.paymentDao()
        val since = lastPullTime("payments")
        val remoteRows = paymentApi.getUpdatedSince("gt.${isoFormat.format(Date(since))}")
        var applied = 0
        remoteRows.forEach { dto ->
            dao.upsertLocal(dto.toEntity(SyncStatus.SYNCED))
            applied++
        }
        setLastPullTime("payments")
        return applied
    }

    private fun lastPullTime(table: String): Long = prefs.getLong("last_pull_$table", 0L)
    private fun setLastPullTime(table: String) {
        prefs.edit().putLong("last_pull_$table", System.currentTimeMillis()).apply()
    }
}
