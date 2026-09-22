package com.tkbiswas.pilesclinic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Mirrors the Supabase table "follow_ups". */
@Entity(tableName = "follow_ups", indices = [Index("syncStatus"), Index("patientId")])
data class FollowUpEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val patientId: String = "", // links to RegistrationEntity.id
    val followUpDate: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val nextVisitDate: Long? = null,
    val doneByRole: String = "STAFF",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttemptAt: Long? = null,
    val syncError: String? = null
)
