package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_slips")
data class MedicineSlip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientRegNo: String,
    val prescriptionId: Int = 0,
    val date: String,
    val medicinesJson: String // Serialized JSON or flat text representation of medicines list
)
