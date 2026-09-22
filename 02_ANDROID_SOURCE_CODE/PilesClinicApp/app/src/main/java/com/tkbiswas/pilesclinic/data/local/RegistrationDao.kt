package com.tkbiswas.pilesclinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistrationDao {

    @Query("SELECT * FROM registrations WHERE isDeleted = 0 ORDER BY registrationDate DESC")
    fun observeAll(): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RegistrationEntity?

    @Query("SELECT * FROM registrations WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<RegistrationEntity>

    @Query("SELECT COUNT(*) FROM registrations WHERE syncStatus = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocal(entity: RegistrationEntity)

    @Update
    suspend fun update(entity: RegistrationEntity)

    @Query("UPDATE registrations SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE registrations SET syncStatus = 'SYNCED', syncError = NULL, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markSynced(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE registrations SET syncStatus = 'FAILED', syncError = :error, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markFailed(id: String, error: String, now: Long = System.currentTimeMillis())
}
