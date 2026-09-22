package com.tkbiswas.pilesclinic.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Mirrors the Supabase table "payments". */
@Entity(tableName = "payments", indices = [Index("syncStatus"), Index("patientId")])
data class PaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val patientId: String = "", // links to RegistrationEntity.id
    val amount: Double = 0.0,
    val paymentDate: Long = System.currentTimeMillis(),
    val mode: String = "Cash", // CASH / ONLINE (Lock Book §11)
    val purpose: String? = null,
    val receivedByRole: String = "STAFF",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttemptAt: Long? = null,
    val syncError: String? = null
)
