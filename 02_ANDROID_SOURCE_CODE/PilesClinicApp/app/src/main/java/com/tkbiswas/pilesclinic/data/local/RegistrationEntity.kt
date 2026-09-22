package com.tkbiswas.pilesclinic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Mirrors the Supabase table "registrations". */
@Entity(tableName = "registrations", indices = [Index("syncStatus")])
data class RegistrationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val enquiryId: String? = null,
    val regNo: String = "",
    val patientName: String = "",
    val age: Int? = null,
    val gender: String? = null,
    val phone: String = "",
    val address: String? = null,
    val registrationDate: Long = System.currentTimeMillis(),
    val referredBy: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttemptAt: Long? = null,
    val syncError: String? = null
)
