package com.example.data.repository

import com.example.data.database.EnquiryDao
import com.example.data.model.Enquiry
import kotlinx.coroutines.flow.Flow

class EnquiryRepository(private val enquiryDao: EnquiryDao) {
    val allEnquiries: Flow<List<Enquiry>> = enquiryDao.getAllEnquiries()

    suspend fun getEnquiryById(id: Int): Enquiry? = enquiryDao.getEnquiryById(id)

    suspend fun insert(enquiry: Enquiry): Long = enquiryDao.insertEnquiry(enquiry)

    suspend fun update(enquiry: Enquiry) = enquiryDao.updateEnquiry(enquiry)

    suspend fun delete(enquiry: Enquiry) = enquiryDao.deleteEnquiry(enquiry)
}
