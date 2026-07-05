package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctor_visits")
data class DoctorVisit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientRegNo: String,
    val date: String,
    val symptoms: String,
    val bp: String,
    val weight: String,
    val pulse: String,
    val diagnosis: String,
    val advice: String,
    val doctorName: String
)
