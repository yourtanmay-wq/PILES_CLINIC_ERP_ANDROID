package com.example.data.repository

import com.example.data.database.ClinicDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class ClinicRepository(private val clinicDao: ClinicDao) {

    // --- ENQUIRIES ---
    val allEnquiries: Flow<List<Enquiry>> = clinicDao.getAllEnquiries()
    suspend fun getEnquiryById(id: Int): Enquiry? = clinicDao.getEnquiryById(id)
    suspend fun insertEnquiry(enquiry: Enquiry) = clinicDao.insertEnquiry(enquiry)
    suspend fun updateEnquiry(enquiry: Enquiry) = clinicDao.updateEnquiry(enquiry)
    suspend fun deleteEnquiry(enquiry: Enquiry) = clinicDao.deleteEnquiry(enquiry)

    // --- BRANCHES ---
    val allBranches: Flow<List<Branch>> = clinicDao.getAllBranches()
    suspend fun insertBranch(branch: Branch) = clinicDao.insertBranch(branch)
    suspend fun updateBranch(branch: Branch) = clinicDao.updateBranch(branch)
    suspend fun deleteBranch(branch: Branch) = clinicDao.deleteBranch(branch)

    // --- PATIENTS ---
    val allPatients: Flow<List<Patient>> = clinicDao.getAllPatients()
    suspend fun getPatientByRegNo(regNo: String): Patient? = clinicDao.getPatientByRegNo(regNo)
    fun searchPatients(query: String): Flow<List<Patient>> = clinicDao.searchPatients(query)
    suspend fun insertPatient(patient: Patient) = clinicDao.insertPatient(patient)
    suspend fun updatePatient(patient: Patient) = clinicDao.updatePatient(patient)
    suspend fun deletePatient(patient: Patient) = clinicDao.deletePatient(patient)

    // --- DOCTOR VISITS ---
    val allDoctorVisits: Flow<List<DoctorVisit>> = clinicDao.getAllDoctorVisits()
    fun getDoctorVisitsByPatient(regNo: String): Flow<List<DoctorVisit>> = clinicDao.getDoctorVisitsByPatient(regNo)
    suspend fun insertDoctorVisit(visit: DoctorVisit) = clinicDao.insertDoctorVisit(visit)
    suspend fun updateDoctorVisit(visit: DoctorVisit) = clinicDao.updateDoctorVisit(visit)
    suspend fun deleteDoctorVisit(visit: DoctorVisit) = clinicDao.deleteDoctorVisit(visit)

    // --- PAYMENTS ---
    val allPayments: Flow<List<Payment>> = clinicDao.getAllPayments()
    fun getPaymentsByPatient(regNo: String): Flow<List<Payment>> = clinicDao.getPaymentsByPatient(regNo)
    suspend fun insertPayment(payment: Payment) = clinicDao.insertPayment(payment)
    suspend fun updatePayment(payment: Payment) = clinicDao.updatePayment(payment)
    suspend fun deletePayment(payment: Payment) = clinicDao.deletePayment(payment)

    // --- PRESCRIPTIONS ---
    val allPrescriptions: Flow<List<Prescription>> = clinicDao.getAllPrescriptions()
    fun getPrescriptionsByPatient(regNo: String): Flow<List<Prescription>> = clinicDao.getPrescriptionsByPatient(regNo)
    suspend fun insertPrescription(prescription: Prescription) = clinicDao.insertPrescription(prescription)
    suspend fun updatePrescription(prescription: Prescription) = clinicDao.updatePrescription(prescription)
    suspend fun deletePrescription(prescription: Prescription) = clinicDao.deletePrescription(prescription)

    // --- MEDICINE SLIPS ---
    val allMedicineSlips: Flow<List<MedicineSlip>> = clinicDao.getAllMedicineSlips()
    fun getMedicineSlipsByPatient(regNo: String): Flow<List<MedicineSlip>> = clinicDao.getMedicineSlipsByPatient(regNo)
    suspend fun insertMedicineSlip(slip: MedicineSlip) = clinicDao.insertMedicineSlip(slip)
    suspend fun updateMedicineSlip(slip: MedicineSlip) = clinicDao.updateMedicineSlip(slip)
    suspend fun deleteMedicineSlip(slip: MedicineSlip) = clinicDao.deleteMedicineSlip(slip)

    // --- BLOOD TESTS ---
    val allBloodTests: Flow<List<BloodTestAdvice>> = clinicDao.getAllBloodTests()
    fun getBloodTestsByPatient(regNo: String): Flow<List<BloodTestAdvice>> = clinicDao.getBloodTestsByPatient(regNo)
    suspend fun insertBloodTest(bloodTest: BloodTestAdvice) = clinicDao.insertBloodTest(bloodTest)
    suspend fun updateBloodTest(bloodTest: BloodTestAdvice) = clinicDao.updateBloodTest(bloodTest)
    suspend fun deleteBloodTest(bloodTest: BloodTestAdvice) = clinicDao.deleteBloodTest(bloodTest)

    // --- DIET CHARTS ---
    val allDietCharts: Flow<List<DietChart>> = clinicDao.getAllDietCharts()
    fun getDietChartsByPatient(regNo: String): Flow<List<DietChart>> = clinicDao.getDietChartsByPatient(regNo)
    suspend fun insertDietChart(dietChart: DietChart) = clinicDao.insertDietChart(dietChart)
    suspend fun updateDietChart(dietChart: DietChart) = clinicDao.updateDietChart(dietChart)
    suspend fun deleteDietChart(dietChart: DietChart) = clinicDao.deleteDietChart(dietChart)
}
