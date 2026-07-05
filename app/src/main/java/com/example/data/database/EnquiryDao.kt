package com.example.data.database

import androidx.room.*
import com.example.data.model.Enquiry
import kotlinx.coroutines.flow.Flow

@Dao
interface EnquiryDao {
    @Query("SELECT * FROM enquiries ORDER BY id DESC")
    fun getAllEnquiries(): Flow<List<Enquiry>>

    @Query("SELECT * FROM enquiries WHERE id = :id")
    suspend fun getEnquiryById(id: Int): Enquiry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnquiry(enquiry: Enquiry): Long

    @Update
    suspend fun updateEnquiry(enquiry: Enquiry)

    @Delete
    suspend fun deleteEnquiry(enquiry: Enquiry)
}
