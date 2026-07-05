package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicDao {

    // --- ENQUIRIES ---
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


    // --- BRANCHES ---
    @Query("SELECT * FROM branches ORDER BY id DESC")
    fun getAllBranches(): Flow<List<Branch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: Branch): Long

    @Update
    suspend fun updateBranch(branch: Branch)

    @Delete
    suspend fun deleteBranch(branch: Branch)


    // --- PATIENTS ---
    @Query("SELECT * FROM patients ORDER BY id DESC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE regNo = :regNo LIMIT 1")
    suspend fun getPatientByRegNo(regNo: String): Patient?

    @Query("SELECT * FROM patients WHERE name LIKE :query OR mobile LIKE :query OR regNo LIKE :query")
    fun searchPatients(query: String): Flow<List<Patient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)


    // --- DOCTOR VISITS ---
    @Query("SELECT * FROM doctor_visits ORDER BY id DESC")
    fun getAllDoctorVisits(): Flow<List<DoctorVisit>>

    @Query("SELECT * FROM doctor_visits WHERE patientRegNo = :regNo ORDER BY id DESC")
    fun getDoctorVisitsByPatient(regNo: String): Flow<List<DoctorVisit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorVisit(visit: DoctorVisit): Long

    @Update
    suspend fun updateDoctorVisit(visit: DoctorVisit)

    @Delete
    suspend fun deleteDoctorVisit(visit: DoctorVisit)


    // --- PAYMENTS ---
    @Query("SELECT * FROM payments ORDER BY id DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE patientRegNo = :regNo ORDER BY id DESC")
    fun getPaymentsByPatient(regNo: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)


    // --- PRESCRIPTIONS ---
    @Query("SELECT * FROM prescriptions ORDER BY id DESC")
    fun getAllPrescriptions(): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions WHERE patientRegNo = :regNo ORDER BY id DESC")
    fun getPrescriptionsByPatient(regNo: String): Flow<List<Prescription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: Prescription): Long

    @Update
    suspend fun updatePrescription(prescription: Prescription)

    @Delete
    suspend fun deletePrescription(prescription: Prescription)


    // --- MEDICINE SLIPS ---
    @Query("SELECT * FROM medicine_slips ORDER BY id DESC")
    fun getAllMedicineSlips(): Flow<List<MedicineSlip>>

    @Query("SELECT * FROM medicine_slips WHERE patientRegNo = :regNo ORDER BY id DESC")
    fun getMedicineSlipsByPatient(regNo: String): Flow<List<MedicineSlip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicineSlip(slip: MedicineSlip): Long

    @Update
    suspend fun updateMedicineSlip(slip: MedicineSlip)

    @Delete
    suspend fun deleteMedicineSlip(slip: MedicineSlip)


    // --- BLOOD TESTS ---
    @Query("SELECT * FROM blood_tests ORDER BY id DESC")
    fun getAllBloodTests(): Flow<List<BloodTestAdvice>>

    @Query("SELECT * FROM blood_tests WHERE patientRegNo = :regNo ORDER BY id DESC")
    fun getBloodTestsByPatient(regNo: String): Flow<List<BloodTestAdvice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodTest(bloodTest: BloodTestAdvice): Long

    @Update
    suspend fun updateBloodTest(bloodTest: BloodTestAdvice)

    @Delete
    suspend fun deleteBloodTest(bloodTest: BloodTestAdvice)


    // --- DIET CHARTS ---
    @Query("SELECT * FROM diet_charts ORDER BY id DESC")
    fun getAllDietCharts(): Flow<List<DietChart>>

    @Query("SELECT * FROM diet_charts WHERE patientRegNo = :regNo ORDER BY id DESC")
    fun getDietChartsByPatient(regNo: String): Flow<List<DietChart>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietChart(dietChart: DietChart): Long

    @Update
    suspend fun updateDietChart(dietChart: DietChart)

    @Delete
    suspend fun deleteDietChart(dietChart: DietChart)
}
