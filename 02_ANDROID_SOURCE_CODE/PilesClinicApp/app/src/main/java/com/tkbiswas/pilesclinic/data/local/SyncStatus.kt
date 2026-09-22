package com.tkbiswas.pilesclinic.data.local

/**
 * Sync state for every locally-stored record.
 *
 * PENDING -> created/edited locally, not yet confirmed on Supabase.
 * SYNCED  -> matches the last known server state.
 * FAILED  -> a sync attempt was made and failed (network/server error); the
 *            record stays fully usable locally and is retried automatically.
 */
enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}
