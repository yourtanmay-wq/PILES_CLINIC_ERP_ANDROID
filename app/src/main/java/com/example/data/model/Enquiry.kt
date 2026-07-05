package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enquiries")
data class Enquiry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "YYYY-MM-DD"
    val mobile: String,
    val patientName: String = "",
    val branch: String,
    val callReceivedBy: String,
    val disease: String,
    val shortAddress: String = "",
    val callTiming: String,
    val remarks: String,
    val nextFollowUpDate: String, // format "YYYY-MM-DD"
    val status: String = "Pending", // Default is Pending
    val callCount: Int = 0,
    val lastCallDate: String? = null, // "YYYY-MM-DD"
    val historyText: String = "" // Multi-line text for previous call details
)
