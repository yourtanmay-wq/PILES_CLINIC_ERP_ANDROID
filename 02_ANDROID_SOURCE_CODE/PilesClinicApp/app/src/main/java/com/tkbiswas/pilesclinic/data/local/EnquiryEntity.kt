package com.tkbiswas.pilesclinic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Local, offline-first record for a patient enquiry.
 * Mirrors the Supabase table "enquiries" (see SUPABASE_SETUP.md for the
 * matching SQL schema). The id is generated on-device (UUID) so a record can
 * be created fully offline and safely upserted later without a server round
 * trip to obtain a primary key.
 */
@Entity(tableName = "enquiries", indices = [Index("syncStatus")])
data class EnquiryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val patientName: String = "",
    val phone: String = "",
    val address: String? = null,
    val enquiryDate: Long = System.currentTimeMillis(),
    val source: String? = null, // e.g. Walk-in / Phone / Reference
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttemptAt: Long? = null,
    val syncError: String? = null
)
