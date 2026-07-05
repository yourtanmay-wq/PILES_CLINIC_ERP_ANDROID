package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ClinicViewModel(private val repository: ClinicRepository) : ViewModel() {

    // --- SHARED FLOWS ---
    val enquiries: StateFlow<List<Enquiry>> = repository.allEnquiries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val branches: StateFlow<List<Branch>> = repository.allBranches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val patients: StateFlow<List<Patient>> = repository.allPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doctorVisits: StateFlow<List<DoctorVisit>> = repository.allDoctorVisits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prescriptions: StateFlow<List<Prescription>> = repository.allPrescriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicineSlips: StateFlow<List<MedicineSlip>> = repository.allMedicineSlips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bloodTests: StateFlow<List<BloodTestAdvice>> = repository.allBloodTests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dietCharts: StateFlow<List<DietChart>> = repository.allDietCharts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- STATS/REPORTS FLOWS ---
    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    init {
        // Pre-populate branches if empty
        viewModelScope.launch {
            repository.allBranches.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repository.insertBranch(Branch(name = "KNE", code = "KNE", address = "KishanGanj Clinic, Bihar", phone = "9883605917"))
                    repository.insertBranch(Branch(name = "JPE", code = "JPE", address = "Jalpaiguri Clinic, West Bengal", phone = "8101397763"))
                    repository.insertBranch(Branch(name = "COB", code = "COB", address = "Cooch Behar Clinic, West Bengal", phone = "7679751521"))
                    repository.insertBranch(Branch(name = "FLK", code = "FLK", address = "Falakata Clinic, West Bengal", phone = "9883623823"))
                    repository.insertBranch(Branch(name = "BIR", code = "BIR", address = "Birpara Clinic, West Bengal", phone = "7501275078"))
                }
            }
        }
    }

    // --- BRANCH OPERATIONS ---
    fun addBranch(name: String, code: String, address: String, phone: String) {
        viewModelScope.launch {
            repository.insertBranch(Branch(name = name.trim(), code = code.trim().uppercase(), address = address.trim(), phone = phone.trim()))
        }
    }

    fun updateBranch(branch: Branch) {
        viewModelScope.launch {
            repository.updateBranch(branch)
        }
    }

    fun deleteBranch(branch: Branch) {
        viewModelScope.launch {
            repository.deleteBranch(branch)
        }
    }

    // --- PATIENT REGISTRATION ---
    fun registerPatient(
        name: String,
        mobile: String,
        age: String,
        gender: String,
        address: String,
        branch: String,
        disease: String,
        remarks: String,
        initialPaymentAmount: String,
        paymentMode: String,
        registeredBy: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (name.isBlank() || mobile.isBlank() || age.isBlank() || branch.isBlank()) {
            onComplete(false, "Please complete all mandatory fields")
            return
        }

        viewModelScope.launch {
            try {
                // Auto-generate next Patient Reg No
                val year = DateUtils.getTodayDateString().substring(0, 4)
                val allCurrent = repository.allPatients.first()
                val nextNumber = allCurrent.size + 1
                val regNo = "PC-$year-${nextNumber.toString().padStart(4, '0')}"

                val patient = Patient(
                    regNo = regNo,
                    date = DateUtils.getTodayDateString(),
                    name = name.trim(),
                    mobile = mobile.trim(),
                    age = age.toIntOrNull() ?: 30,
                    gender = gender,
                    address = address.trim(),
                    branch = branch,
                    disease = disease,
                    remarks = remarks.trim(),
                    registeredBy = registeredBy
                )

                repository.insertPatient(patient)

                // Save Initial payment if provided
                val paymentVal = initialPaymentAmount.toDoubleOrNull() ?: 0.0
                if (paymentVal > 0.0) {
                    val payment = Payment(
                        patientRegNo = regNo,
                        date = DateUtils.getTodayDateString(),
                        category = "Registration",
                        amount = paymentVal,
                        paymentMode = paymentMode,
                        remarks = "Initial payment at registration",
                        receivedBy = registeredBy
                    )
                    repository.insertPayment(payment)
                }

                onComplete(true, "Patient Registered Successfully with Reg No: $regNo")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    fun updatePatient(patient: Patient, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updatePatient(patient)
                onComplete(true, "Patient updated successfully")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    fun deletePatient(patient: Patient, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePatient(patient)
                onComplete(true, "Patient record deleted successfully")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    // --- DOCTOR VISIT SECTION ---
    fun recordDoctorVisit(
        patientRegNo: String,
        symptoms: String,
        bp: String,
        weight: String,
        pulse: String,
        diagnosis: String,
        advice: String,
        doctorName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (patientRegNo.isBlank() || symptoms.isBlank() || diagnosis.isBlank()) {
            onComplete(false, "Patient, symptoms, and diagnosis are mandatory")
            return
        }

        viewModelScope.launch {
            try {
                val visit = DoctorVisit(
                    patientRegNo = patientRegNo,
                    date = DateUtils.getTodayDateString(),
                    symptoms = symptoms.trim(),
                    bp = bp.trim(),
                    weight = weight.trim(),
                    pulse = pulse.trim(),
                    diagnosis = diagnosis.trim(),
                    advice = advice.trim(),
                    doctorName = doctorName
                )
                repository.insertDoctorVisit(visit)
                onComplete(true, "Doctor visit recorded successfully")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    // --- PAYMENT MODULE ---
    fun addPayment(
        patientRegNo: String,
        category: String,
        amountStr: String,
        paymentMode: String,
        transactionId: String,
        remarks: String,
        receivedBy: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val amount = amountStr.toDoubleOrNull()
        if (patientRegNo.isBlank() || amount == null || amount <= 0.0) {
            onComplete(false, "Patient and valid positive amount are mandatory")
            return
        }

        viewModelScope.launch {
            try {
                val payment = Payment(
                    patientRegNo = patientRegNo,
                    date = DateUtils.getTodayDateString(),
                    category = category,
                    amount = amount,
                    paymentMode = paymentMode,
                    transactionId = transactionId.trim(),
                    remarks = remarks.trim(),
                    receivedBy = receivedBy
                )
                repository.insertPayment(payment)
                onComplete(true, "Payment of INR $amount recorded successfully")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    // --- PRESCRIPTION, MEDICINE SLIP, BLOOD TEST, DIET CHART ---
    fun saveFullPrescription(
        patientRegNo: String,
        doctorName: String,
        complaints: String,
        diagnosis: String,
        generalInstructions: String,
        medicinesList: List<Triple<String, String, String>>, // Name, Dosage, Timing
        bloodTestsAdvised: List<String>,
        dietChartInstructions: String, // morning, lunch, evening, dinner
        onComplete: (Boolean, String) -> Unit
    ) {
        if (patientRegNo.isBlank()) {
            onComplete(false, "Patient Selection is mandatory")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Prescription
                val prescription = Prescription(
                    patientRegNo = patientRegNo,
                    date = DateUtils.getTodayDateString(),
                    doctorName = doctorName,
                    complaints = complaints.trim(),
                    diagnosis = diagnosis.trim(),
                    generalInstructions = generalInstructions.trim()
                )
                val prescriptionId = repository.insertPrescription(prescription).toInt()

                // 2. Medicine Slip
                if (medicinesList.isNotEmpty()) {
                    val jArray = JSONArray()
                    medicinesList.forEach { (name, dose, time) ->
                        val obj = JSONObject()
                        obj.put("name", name)
                        obj.put("dosage", dose)
                        obj.put("timing", time)
                        jArray.put(obj)
                    }
                    val medicineSlip = MedicineSlip(
                        patientRegNo = patientRegNo,
                        prescriptionId = prescriptionId,
                        date = DateUtils.getTodayDateString(),
                        medicinesJson = jArray.toString()
                    )
                    repository.insertMedicineSlip(medicineSlip)
                }

                // 3. Blood Test Advice
                if (bloodTestsAdvised.isNotEmpty()) {
                    val bloodTest = BloodTestAdvice(
                        patientRegNo = patientRegNo,
                        date = DateUtils.getTodayDateString(),
                        tests = bloodTestsAdvised.joinToString(", "),
                        status = "Advised"
                    )
                    repository.insertBloodTest(bloodTest)
                }

                // 4. Diet Chart
                if (dietChartInstructions.isNotBlank()) {
                    val dietChart = DietChart(
                        patientRegNo = patientRegNo,
                        date = DateUtils.getTodayDateString(),
                        instructions = dietChartInstructions.trim()
                    )
                    repository.insertDietChart(dietChart)
                }

                onComplete(true, "Prescription saved successfully!")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    // --- DIET CHART INDEPENDENT SAVE ---
    fun saveDietChart(
        patientRegNo: String,
        morning: String,
        lunch: String,
        evening: String,
        dinner: String,
        instructions: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (patientRegNo.isBlank()) {
            onComplete(false, "Patient selection is mandatory")
            return
        }
        viewModelScope.launch {
            try {
                val chart = DietChart(
                    patientRegNo = patientRegNo,
                    date = DateUtils.getTodayDateString(),
                    morningDiet = morning.trim(),
                    lunchDiet = lunch.trim(),
                    eveningDiet = evening.trim(),
                    dinnerDiet = dinner.trim(),
                    instructions = instructions.trim()
                )
                repository.insertDietChart(chart)
                onComplete(true, "Diet chart saved successfully!")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    // --- SUPABASE CLOUD SYNC LOGIC ---
    fun syncWithSupabase(url: String, key: String) {
        if (url.isBlank() || key.isBlank()) {
            _syncStatus.value = "Failed: URL and Key cannot be blank."
            return
        }

        _isSyncing.value = true
        _syncStatus.value = "Initiating Supabase Cloud Sync..."

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()

                val cleanUrl = if (url.endsWith("/")) url.substring(0, url.length - 1) else url
                val jsonMediaType = "application/json; charset=utf-8".toMediaType()

                // Sync 1: Patients Table
                val patientList = repository.allPatients.first()
                if (patientList.isNotEmpty()) {
                    val pArray = JSONArray()
                    patientList.forEach {
                        val pObj = JSONObject()
                        pObj.put("reg_no", it.regNo)
                        pObj.put("date", it.date)
                        pObj.put("name", it.name)
                        pObj.put("mobile", it.mobile)
                        pObj.put("age", it.age)
                        pObj.put("gender", it.gender)
                        pObj.put("address", it.address)
                        pObj.put("branch", it.branch)
                        pObj.put("disease", it.disease)
                        pObj.put("remarks", it.remarks)
                        pArray.put(pObj)
                    }

                    val request = Request.Builder()
                        .url("$cleanUrl/rest/v1/patients")
                        .addHeader("apikey", key)
                        .addHeader("Authorization", "Bearer $key")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .post(pArray.toString().toRequestBody(jsonMediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _syncStatus.value = "Patient upload returned code: ${response.code}"
                        }
                    }
                }

                // Sync 2: Enquiries Table
                val enquiryList = repository.allEnquiries.first()
                if (enquiryList.isNotEmpty()) {
                    val eArray = JSONArray()
                    enquiryList.forEach {
                        val eObj = JSONObject()
                        eObj.put("date", it.date)
                        eObj.put("mobile", it.mobile)
                        eObj.put("patient_name", it.patientName)
                        eObj.put("branch", it.branch)
                        eObj.put("call_received_by", it.callReceivedBy)
                        eObj.put("disease", it.disease)
                        eObj.put("remarks", it.remarks)
                        eObj.put("next_follow_up_date", it.nextFollowUpDate)
                        eObj.put("status", it.status)
                        eArray.put(eObj)
                    }

                    val request = Request.Builder()
                        .url("$cleanUrl/rest/v1/enquiries")
                        .addHeader("apikey", key)
                        .addHeader("Authorization", "Bearer $key")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .post(eArray.toString().toRequestBody(jsonMediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _syncStatus.value = "Enquiries sync returned code: ${response.code}"
                        }
                    }
                }

                // Sync 3: Payments Table
                val paymentList = repository.allPayments.first()
                if (paymentList.isNotEmpty()) {
                    val payArray = JSONArray()
                    paymentList.forEach {
                        val payObj = JSONObject()
                        payObj.put("patient_reg_no", it.patientRegNo)
                        payObj.put("date", it.date)
                        payObj.put("category", it.category)
                        payObj.put("amount", it.amount)
                        payObj.put("payment_mode", it.paymentMode)
                        payObj.put("transaction_id", it.transactionId)
                        payArray.put(payObj)
                    }

                    val request = Request.Builder()
                        .url("$cleanUrl/rest/v1/payments")
                        .addHeader("apikey", key)
                        .addHeader("Authorization", "Bearer $key")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .post(payArray.toString().toRequestBody(jsonMediaType))
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _syncStatus.value = "Payments sync returned code: ${response.code}"
                        }
                    }
                }

                _syncStatus.value = "Success: Local database fully synchronized with Cloud Supabase!"
            } catch (e: Exception) {
                _syncStatus.value = "Error during Cloud Sync: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun startNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            val networkRequest = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            try {
                connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        super.onAvailable(network)
                        // Automatically trigger sync if internet returns
                        syncWithSupabase(
                            url = "https://piles-clinic-erp.supabase.co",
                            key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBpbGVzLWNsaW5pYyIsImV4cCI6MTgwMDAwMDAwMH0.ExampleKey"
                        )
                    }
                })
            } catch (e: Exception) {
                // Ignore registration exceptions (e.g., duplicate callbacks)
            }
        }
    }
}

class ClinicViewModelFactory(private val repository: ClinicRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClinicViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClinicViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
