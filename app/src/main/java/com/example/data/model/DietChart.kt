package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diet_charts")
data class DietChart(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientRegNo: String,
    val date: String,
    val morningDiet: String = "",
    val lunchDiet: String = "",
    val eveningDiet: String = "",
    val dinnerDiet: String = "",
    val instructions: String = "" // Do's & Don'ts
)
