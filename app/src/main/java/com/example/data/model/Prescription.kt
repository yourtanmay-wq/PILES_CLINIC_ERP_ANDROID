package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prescriptions")
data class Prescription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientRegNo: String,
    val date: String,
    val doctorName: String,
    val complaints: String,
    val diagnosis: String,
    val generalInstructions: String = ""
)
