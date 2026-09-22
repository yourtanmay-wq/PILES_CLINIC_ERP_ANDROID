package com.tkbiswas.pilesclinic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EnquiryDao {

    @Query("SELECT * FROM enquiries WHERE isDeleted = 0 ORDER BY enquiryDate DESC")
    fun observeAll(): Flow<List<EnquiryEntity>>

    @Query("SELECT * FROM enquiries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EnquiryEntity?

    @Query("SELECT * FROM enquiries WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<EnquiryEntity>

    @Query("SELECT COUNT(*) FROM enquiries WHERE syncStatus = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocal(entity: EnquiryEntity)

    @Update
    suspend fun update(entity: EnquiryEntity)

    /** Soft delete: keep the row (for sync) but hide it locally and mark it for a remote delete/sync. */
    @Query("UPDATE enquiries SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE enquiries SET syncStatus = 'SYNCED', syncError = NULL, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markSynced(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE enquiries SET syncStatus = 'FAILED', syncError = :error, lastSyncAttemptAt = :now WHERE id = :id")
    suspend fun markFailed(id: String, error: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM enquiries WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<EnquiryEntity?>
}
