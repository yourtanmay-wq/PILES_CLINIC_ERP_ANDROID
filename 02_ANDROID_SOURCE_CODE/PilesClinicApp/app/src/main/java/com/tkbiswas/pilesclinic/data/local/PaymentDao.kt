package com.tkbiswas.pilesclinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE isDeleted = 0 ORDER BY paymentDate DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE patientId = :patientId AND isDeleted = 0 ORDER BY paymentDate DESC")
    fun observeForPatient(patientId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<PaymentEntity>

    @Query("SELECT COUNT(*) FROM payments WHERE syncStatus = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocal(entity: PaymentEntity)

    @Update
    suspend fun update(entity: PaymentEntity)

    @Query("UPDATE payments SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE payments SET syncStatus = 'SYNCED', syncError = NULL, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markSynced(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE payments SET syncStatus = 'FAILED', syncError = :error, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markFailed(id: String, error: String, now: Long = System.currentTimeMillis())
}
