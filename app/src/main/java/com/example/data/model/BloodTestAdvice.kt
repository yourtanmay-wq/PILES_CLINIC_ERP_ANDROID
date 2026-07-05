package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_tests")
data class BloodTestAdvice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientRegNo: String,
    val date: String,
    val tests: String, // comma separated tests
    val status: String = "Advised", // Advised, Done, Pending, Report Received
    val notes: String = ""
)
