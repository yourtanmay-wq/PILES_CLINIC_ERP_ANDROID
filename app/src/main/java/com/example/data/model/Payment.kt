package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientRegNo: String,
    val date: String,
    val category: String, // Registration, Treatment, Medicine, Blood Test, Diet Chart, etc.
    val amount: Double,
    val paymentMode: String, // Cash, UPI, Card, NetBanking
    val transactionId: String = "",
    val remarks: String = "",
    val receivedBy: String = ""
)
