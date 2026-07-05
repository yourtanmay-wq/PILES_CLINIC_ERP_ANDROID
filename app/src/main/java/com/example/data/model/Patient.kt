package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val regNo: String, // PC-YYYY-XXXX format
    val date: String,
    val name: String,
    val mobile: String,
    val age: Int,
    val gender: String,
    val address: String,
    val branch: String,
    val disease: String,
    val remarks: String = "",
    val registeredBy: String = ""
)
