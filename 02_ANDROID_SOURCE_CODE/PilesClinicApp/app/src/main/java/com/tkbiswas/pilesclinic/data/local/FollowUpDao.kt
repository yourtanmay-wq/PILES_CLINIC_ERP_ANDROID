package com.tkbiswas.pilesclinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowUpDao {

    @Query("SELECT * FROM follow_ups WHERE isDeleted = 0 ORDER BY followUpDate DESC")
    fun observeAll(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE patientId = :patientId AND isDeleted = 0 ORDER BY followUpDate DESC")
    fun observeForPatient(patientId: String): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<FollowUpEntity>

    @Query("SELECT COUNT(*) FROM follow_ups WHERE syncStatus = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocal(entity: FollowUpEntity)

    @Update
    suspend fun update(entity: FollowUpEntity)

    @Query("UPDATE follow_ups SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE follow_ups SET syncStatus = 'SYNCED', syncError = NULL, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markSynced(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE follow_ups SET syncStatus = 'FAILED', syncError = :error, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markFailed(id: String, error: String, now: Long = System.currentTimeMillis())
}
