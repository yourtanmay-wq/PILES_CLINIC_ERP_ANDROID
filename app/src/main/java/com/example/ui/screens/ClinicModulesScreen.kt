package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.viewmodel.ClinicViewModel
import org.json.JSONArray
import org.json.JSONObject

// Safe JSON parser helper for composables
fun parseMedicinesJson(json: String): List<String> {
    val list = mutableListOf<String>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add("• ${obj.optString("name")} | ${obj.optString("dosage")} | ${obj.optString("timing")}")
        }
    } catch (e: Exception) {
        list.add(json)
    }
    return list
}

// ==========================================
// 1. BRANCH MANAGEMENT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchManagementScreen(
    viewModel: ClinicViewModel,
    onBack: () -> Unit
) {
    val branches by viewModel.branches.collectAsState()
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Branch Management", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Create New Branch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Branch Name (e.g., KishanGanj)") },
                            modifier = Modifier.fillMaxWidth().testTag("branch_name_field")
                        )

                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Branch Code (e.g., KNE)") },
                            modifier = Modifier.fillMaxWidth().testTag("branch_code_field")
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (name.isBlank() || code.isBlank()) {
                                    Toast.makeText(context, "Name and Code are required", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addBranch(name, code, address, phone)
                                    name = ""
                                    code = ""
                                    address = ""
                                    phone = ""
                                    Toast.makeText(context, "Branch Created successfully", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("branch_save_btn")
                        ) {
                            Icon(Icons.Default.AddHomeWork, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("SAVE BRANCH")
                        }
                    }
                }
            }

            item {
                Text(
                    "Existing Branches (${branches.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(branches) { branch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(branch.code, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(branch.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Address: ${branch.address.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodyMedium)
                            Text("Phone: ${branch.phone.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                        }

                        IconButton(onClick = {
                            viewModel.deleteBranch(branch)
                            Toast.makeText(context, "Branch Deleted", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Branch", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. PATIENT REGISTRATION SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRegistrationScreen(
    viewModel: ClinicViewModel,
    currentUserRole: String,
    onBack: () -> Unit
) {
    val branches by viewModel.branches.collectAsState()

    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var address by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("") }
    var disease by remember { mutableStateOf("Piles") }
    var remarks by remember { mutableStateOf("") }
    var initialPaymentAmount by remember { mutableStateOf("500") }
    var paymentMode by remember { mutableStateOf("Cash") }

    val context = LocalContext.current

    // Set default branch when loaded
    LaunchedEffect(branches) {
        if (branches.isNotEmpty() && selectedBranch.isEmpty()) {
            selectedBranch = branches.first().name
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Patient Registration", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Patient Demographics",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("patient_name_field"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Mobile Number *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("patient_mobile_field"),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Age *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("patient_age_field")
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gender", style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Male", "Female", "Other").forEach { g ->
                                    FilterChip(
                                        selected = gender == g,
                                        onClick = { gender = g },
                                        label = { Text(g, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Home Address") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Clinical Details",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Branch Selector
                    Text("Assigned Branch *", style = MaterialTheme.typography.bodySmall)
                    if (branches.isEmpty()) {
                        Text("No branches loaded. Add one in Branch Management first.")
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            branches.forEach { br ->
                                FilterChip(
                                    selected = selectedBranch == br.name,
                                    onClick = { selectedBranch = br.name },
                                    label = { Text(br.name) }
                                )
                            }
                        }
                    }

                    // Disease Category Selectors
                    Text("Select Disease/Condition", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Piles", "Fistula", "Fissure", "Pilonidal Sinus", "Constipation").forEach { d ->
                            FilterChip(
                                selected = disease == d,
                                onClick = { disease = d },
                                label = { Text(d) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Clinical History / General Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Registration & Initial Payment Module",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = initialPaymentAmount,
                        onValueChange = { initialPaymentAmount = it },
                        label = { Text("Initial Payment Amount (INR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) }
                    )

                    Text("Payment Mode", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "UPI", "Card", "NetBanking").forEach { mode ->
                            FilterChip(
                                selected = paymentMode == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (name.isBlank() || mobile.isBlank() || age.isBlank() || selectedBranch.isBlank()) {
                        Toast.makeText(context, "Please complete all fields marked with *", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.registerPatient(
                            name = name,
                            mobile = mobile,
                            age = age,
                            gender = gender,
                            address = address,
                            branch = selectedBranch,
                            disease = disease,
                            remarks = remarks,
                            initialPaymentAmount = initialPaymentAmount,
                            paymentMode = paymentMode,
                            registeredBy = currentUserRole,
                            onComplete = { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    onBack()
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("patient_register_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("SUBMIT PATIENT REGISTRATION", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// 3. DOCTOR VISIT SECTION
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorVisitScreen(
    viewModel: ClinicViewModel,
    doctorName: String,
    onBack: () -> Unit
) {
    val patients by viewModel.patients.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val paidPatientRegNos = remember(payments) {
        payments.filter { it.category.equals("Registration", ignoreCase = true) && it.amount > 0 }.map { it.patientRegNo }.toSet()
    }
    val eligiblePatients = remember(patients, paidPatientRegNos) {
        patients.filter { paidPatientRegNos.contains(it.regNo) }
    }

    var selectedPatientRegNo by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var bp by remember { mutableStateOf("120/80") }
    var weight by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("72") }
    var diagnosis by remember { mutableStateOf("") }
    var advice by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Doctor Visit Logs", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "1. Select Patient (Showing Paid Registrations) *",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (eligiblePatients.isEmpty()) {
                        Text("No patients with paid registration found. Please register and pay the registration fee first.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    } else {
                        eligiblePatients.forEach { patient ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedPatientRegNo == patient.regNo)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { selectedPatientRegNo = patient.regNo }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPatientRegNo == patient.regNo,
                                    onClick = { selectedPatientRegNo = patient.regNo }
                                )
                                Column {
                                    Text("${patient.name} (${patient.regNo})", fontWeight = FontWeight.Bold)
                                    Text("Age: ${patient.age} | Gender: ${patient.gender} | Mobile: ${patient.mobile}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedPatientRegNo.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "2. Enter Vitals & Clinical Examination",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = bp,
                                onValueChange = { bp = it },
                                label = { Text("Blood Pressure (BP)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pulse,
                                onValueChange = { pulse = it },
                                label = { Text("Pulse Rate (bpm)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = weight,
                            onValueChange = { weight = it },
                            label = { Text("Patient Weight (kg)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = symptoms,
                            onValueChange = { symptoms = it },
                            label = { Text("Chief Symptoms *") },
                            modifier = Modifier.fillMaxWidth().testTag("doctor_symptoms_field"),
                            minLines = 2
                        )

                        OutlinedTextField(
                            value = diagnosis,
                            onValueChange = { diagnosis = it },
                            label = { Text("Clinical Diagnosis *") },
                            modifier = Modifier.fillMaxWidth().testTag("doctor_diagnosis_field"),
                            minLines = 2
                        )

                        OutlinedTextField(
                            value = advice,
                            onValueChange = { advice = it },
                            label = { Text("Doctor's Advice / Next Plan") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Button(
                            onClick = {
                                viewModel.recordDoctorVisit(
                                    patientRegNo = selectedPatientRegNo,
                                    symptoms = symptoms,
                                    bp = bp,
                                    weight = weight,
                                    pulse = pulse,
                                    diagnosis = diagnosis,
                                    advice = advice,
                                    doctorName = doctorName,
                                    onComplete = { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            onBack()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("doctor_visit_save_btn")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("SAVE VISIT & VITALS")
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. PAYMENT MODULE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: ClinicViewModel,
    receivedBy: String,
    onBack: () -> Unit
) {
    val patients by viewModel.patients.collectAsState()

    var selectedPatientRegNo by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Treatment") }
    var amount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var txnId by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Billing & Payments", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "1. Select Patient *",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    patients.forEach { patient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedPatientRegNo == patient.regNo)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable { selectedPatientRegNo = patient.regNo }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPatientRegNo == patient.regNo,
                                onClick = { selectedPatientRegNo = patient.regNo }
                            )
                            Column {
                                Text("${patient.name} (${patient.regNo})", fontWeight = FontWeight.Bold)
                                Text("Disease: ${patient.disease} | Mobile: ${patient.mobile}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedPatientRegNo.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "2. Payment Receipt Details",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Payment Category", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Treatment", "Medicine", "Blood Test", "Diet Chart", "Kshar Sutra").forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 10.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount Paid (INR) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("payment_amount_field")
                        )

                        Text("Payment Mode", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cash", "UPI", "Card", "NetBanking").forEach { mode ->
                                FilterChip(
                                    selected = paymentMode == mode,
                                    onClick = { paymentMode = mode },
                                    label = { Text(mode) }
                                )
                            }
                        }

                        if (paymentMode != "Cash") {
                            OutlinedTextField(
                                value = txnId,
                                onValueChange = { txnId = it },
                                label = { Text("Transaction ID / Reference ID") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Remarks (Receipt detail etc)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.addPayment(
                                    patientRegNo = selectedPatientRegNo,
                                    category = category,
                                    amountStr = amount,
                                    paymentMode = paymentMode,
                                    transactionId = txnId,
                                    remarks = remarks,
                                    receivedBy = receivedBy,
                                    onComplete = { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            onBack()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("payment_submit_btn")
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("SUBMIT PAYMENT RECEIPT")
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. PRESCRIPTION & CLINICAL SLIPS BUILDER
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionBuilderScreen(
    viewModel: ClinicViewModel,
    doctorName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsState()
    val doctorVisits by viewModel.doctorVisits.collectAsState()

    val visitedPatientRegNos = remember(doctorVisits) {
        doctorVisits.map { it.patientRegNo }.toSet()
    }

    var selectedPatientRegNo by remember { mutableStateOf("") }
    var complaints by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var generalInstructions by remember { mutableStateOf("") }

    // Auto-fill diagnosis & complaints from latest doctor visit of selected patient
    LaunchedEffect(selectedPatientRegNo, doctorVisits) {
        if (selectedPatientRegNo.isNotEmpty()) {
            val lastVisit = doctorVisits.filter { it.patientRegNo == selectedPatientRegNo }
                .maxByOrNull { it.id } // Use ID to get the absolutely latest visit
            if (lastVisit != null) {
                complaints = lastVisit.symptoms
                diagnosis = lastVisit.diagnosis
            }
        }
    }

    // Medicine Slip builder items
    var medName by remember { mutableStateOf("") }
    var medDose by remember { mutableStateOf("1-0-1") }
    var medTiming by remember { mutableStateOf("After Food") }
    val medicinesList = remember { mutableStateListOf<Triple<String, String, String>>() }

    // Blood test options
    val bloodTestOptions = listOf("CBC", "LFT", "KFT", "HIV", "HBsAg", "Blood Sugar", "Bleeding/Clotting Time")
    val selectedBloodTests = remember { mutableStateListOf<String>() }

    // Diet options
    var morningDiet by remember { mutableStateOf("Warm water with lemon, high fiber oatmeal") }
    var lunchDiet by remember { mutableStateOf("Green salad, boiled vegetables, multi-grain roti") }
    var eveningDiet by remember { mutableStateOf("Papaya or butter milk, dry fruits") }
    var dinnerDiet by remember { mutableStateOf("Light khichdi, avoid oily & spicy food") }
    var generalDietInstructions by remember { mutableStateOf("Avoid red meat, deep fry, and fast foods. Drink 3-4 liters of water daily.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Prescription & Slips", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Patient Select
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Select Patient *", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    patients.forEach { patient ->
                        val hasVisit = visitedPatientRegNos.contains(patient.regNo)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedPatientRegNo == patient.regNo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedPatientRegNo = patient.regNo }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedPatientRegNo == patient.regNo, onClick = { selectedPatientRegNo = patient.regNo })
                            Column {
                                Text("${patient.name} (${patient.regNo}) - ${patient.disease}", fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasVisit) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Doctor Visit Recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("No Visit Logged Yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedPatientRegNo.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Diagnosis & Complaints
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("2. Chief Complaints & Diagnosis", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = complaints,
                                onValueChange = { complaints = it },
                                label = { Text("Chief Complaints") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = diagnosis,
                                onValueChange = { diagnosis = it },
                                label = { Text("Clinical Diagnosis") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Medicine Slip Builder
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("3. Medicine Slip (Dosage & Timing)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                            OutlinedTextField(
                                value = medName,
                                onValueChange = { medName = it },
                                label = { Text("Medicine Name") },
                                modifier = Modifier.fillMaxWidth().testTag("medicine_name_field")
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = medDose,
                                    onValueChange = { medDose = it },
                                    label = { Text("Dose (e.g. 1-0-1)") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = medTiming,
                                    onValueChange = { medTiming = it },
                                    label = { Text("Timing (e.g. After Food)") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    if (medName.isNotBlank()) {
                                        medicinesList.add(Triple(medName.trim(), medDose.trim(), medTiming.trim()))
                                        medName = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End).testTag("add_medicine_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Add to Slip")
                            }

                            // Render Added list
                            medicinesList.forEachIndexed { idx, med ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(med.first, fontWeight = FontWeight.Bold)
                                        Text("${med.second} | ${med.third}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { medicinesList.removeAt(idx) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Blood Test Advice Module
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("4. Blood Test Advice", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Select advised lab tests:", style = MaterialTheme.typography.bodySmall)

                            bloodTestOptions.forEach { test ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedBloodTests.contains(test)) selectedBloodTests.remove(test)
                                            else selectedBloodTests.add(test)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedBloodTests.contains(test),
                                        onCheckedChange = {
                                            if (selectedBloodTests.contains(test)) selectedBloodTests.remove(test)
                                            else selectedBloodTests.add(test)
                                        }
                                    )
                                    Text(test)
                                }
                            }
                        }
                    }

                    // Diet Chart Module
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("5. Custom Diet Chart Guidance", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = morningDiet,
                                onValueChange = { morningDiet = it },
                                label = { Text("Morning (Breakfast)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = lunchDiet,
                                onValueChange = { lunchDiet = it },
                                label = { Text("Lunch") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = eveningDiet,
                                onValueChange = { eveningDiet = it },
                                label = { Text("Evening Snacks") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = dinnerDiet,
                                onValueChange = { dinnerDiet = it },
                                label = { Text("Dinner") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = generalDietInstructions,
                                onValueChange = { generalDietInstructions = it },
                                label = { Text("Do's and Don'ts / Water limits") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                    }

                    // General Instructions & Save Prescription
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("6. General Advice & Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(
                                value = generalInstructions,
                                onValueChange = { generalInstructions = it },
                                label = { Text("General Instructions") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val fullDietText = "Morning: $morningDiet\nLunch: $lunchDiet\nEvening: $eveningDiet\nDinner: $dinnerDiet\nGuidelines: $generalDietInstructions"
                                    viewModel.saveFullPrescription(
                                        patientRegNo = selectedPatientRegNo,
                                        doctorName = doctorName,
                                        complaints = complaints,
                                        diagnosis = diagnosis,
                                        generalInstructions = generalInstructions,
                                        medicinesList = medicinesList.toList(),
                                        bloodTestsAdvised = selectedBloodTests.toList(),
                                        dietChartInstructions = fullDietText,
                                        onComplete = { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            if (success) {
                                                onBack()
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().testTag("prescription_save_btn")
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("SAVE & PROCESS PRESCRIPTION")
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 6. PATIENT SEARCH & UNIFIED EMR SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSearchScreen(
    viewModel: ClinicViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val patients by viewModel.patients.collectAsState()
    val visits by viewModel.doctorVisits.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val prescriptions by viewModel.prescriptions.collectAsState()
    val medicineSlips by viewModel.medicineSlips.collectAsState()
    val bloodTests by viewModel.bloodTests.collectAsState()
    val dietCharts by viewModel.dietCharts.collectAsState()

    var selectedPatient by remember { mutableStateOf<Patient?>(null) }
    var showPrintPreview by remember { mutableStateOf(false) }

    // Dialogs for Edit/Delete
    var showEditPatientDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editMobile by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editGender by remember { mutableStateOf("Male") }
    var editAddress by remember { mutableStateOf("") }
    var editDisease by remember { mutableStateOf("Piles") }
    var editRemarks by remember { mutableStateOf("") }

    val filteredPatients = patients.filter {
        it.name.contains(query, ignoreCase = true) ||
                it.mobile.contains(query) ||
                it.regNo.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Patient Directory (EMR)", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedPatient == null) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Type Name, Mobile, or Reg No...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("emr_search_field"),
                    singleLine = true
                )

                Text("Search Results (${filteredPatients.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPatients) { patient ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPatient = patient },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(patient.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Reg: ${patient.regNo} | Mobile: ${patient.mobile}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Condition: ${patient.disease} | Branch: ${patient.branch}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Icon(Icons.Default.MedicalInformation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                // Unified EMR Summary view
                val patient = selectedPatient!!
                val patientVisits = visits.filter { it.patientRegNo == patient.regNo }
                val patientPayments = payments.filter { it.patientRegNo == patient.regNo }
                val patientPrescriptions = prescriptions.filter { it.patientRegNo == patient.regNo }
                val patientSlips = medicineSlips.filter { it.patientRegNo == patient.regNo }
                val patientBloodTests = bloodTests.filter { it.patientRegNo == patient.regNo }
                val patientDiets = dietCharts.filter { it.patientRegNo == patient.regNo }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { selectedPatient = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Back to Search")
                    }

                    Button(
                        onClick = { showPrintPreview = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Print EMR Record")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Patient Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(patient.name.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                Badge { Text(patient.regNo) }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Mobile: ${patient.mobile} | Age: ${patient.age} | Gender: ${patient.gender}")
                            Text("Address: ${patient.address}")
                            Text("Condition: ${patient.disease} | Registered: ${patient.date}", fontWeight = FontWeight.SemiBold)
                            
                            Spacer(Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Spacer(Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        editName = patient.name
                                        editMobile = patient.mobile
                                        editAge = patient.age.toString()
                                        editGender = patient.gender
                                        editAddress = patient.address
                                        editDisease = patient.disease
                                        editRemarks = patient.remarks
                                        showEditPatientDialog = true
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("EDIT DEMOGRAPHICS")
                                }
                                
                                Spacer(Modifier.width(16.dp))
                                
                                TextButton(
                                    onClick = { showDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("DELETE PATIENT")
                                }
                            }
                        }
                    }

                    // Clinical History Tabs / Sections
                    Text("ELECTRONIC MEDICAL RECORD (EMR)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                    // Section A: Doctor Clinical Visits
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Doctor Clinical Visits (${patientVisits.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            if (patientVisits.isEmpty()) {
                                Text("No visit records found.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                patientVisits.forEach { visit ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("Date: ${visit.date} | Dr. ${visit.doctorName}", fontWeight = FontWeight.SemiBold)
                                        Text("Symptoms: ${visit.symptoms}")
                                        Text("BP: ${visit.bp} | Pulse: ${visit.pulse} | Weight: ${visit.weight} kg", style = MaterialTheme.typography.bodySmall)
                                        Text("Diagnosis: ${visit.diagnosis}", fontWeight = FontWeight.Medium)
                                        Text("Advice: ${visit.advice}")
                                        Divider(modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Section B: Prescriptions & Medicines
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Prescriptions & Medicines", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            if (patientPrescriptions.isEmpty()) {
                                Text("No active prescriptions.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                patientPrescriptions.forEach { pres ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("Date: ${pres.date} | Prescribed by Dr. ${pres.doctorName}", fontWeight = FontWeight.SemiBold)
                                        Text("Complaints: ${pres.complaints}")
                                        Text("Diagnosis: ${pres.diagnosis}")
                                        Text("Instructions: ${pres.generalInstructions}")

                                        // Try render matching slips
                                        patientSlips.forEach { slip ->
                                            if (slip.prescriptionId == pres.id || slip.date == pres.date) {
                                                Text("Medicines Advised:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                                                val parsedMedicines = parseMedicinesJson(slip.medicinesJson)
                                                parsedMedicines.forEach { medicineLine ->
                                                    Text(medicineLine, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                        Divider(modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Section C: Labs & Blood Tests
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Lab Reports & Blood Tests Advice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            if (patientBloodTests.isEmpty()) {
                                Text("No blood tests advised.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                patientBloodTests.forEach { test ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Date: ${test.date}", fontWeight = FontWeight.SemiBold)
                                            Text("Advised Tests: ${test.tests}")
                                        }
                                        Badge { Text(test.status) }
                                    }
                                }
                            }
                        }
                    }

                    // Section D: Diet Charts
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Custom Clinical Diet Charts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            if (patientDiets.isEmpty()) {
                                Text("No diet chart assigned.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                patientDiets.forEach { diet ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("Date: ${diet.date}", fontWeight = FontWeight.SemiBold)
                                        if (diet.morningDiet.isNotEmpty()) {
                                            Text("Morning: ${diet.morningDiet}")
                                            Text("Lunch: ${diet.lunchDiet}")
                                            Text("Evening: ${diet.eveningDiet}")
                                            Text("Dinner: ${diet.dinnerDiet}")
                                        }
                                        Text("Instructions: ${diet.instructions}")
                                        Divider(modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Section E: Payment History
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Billing & Transaction History", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            if (patientPayments.isEmpty()) {
                                Text("No payment receipts recorded.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                val totalPaid = patientPayments.sumOf { it.amount }
                                Text("Total Revenue Collected: INR $totalPaid", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                patientPayments.forEach { pay ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Date: ${pay.date} | Category: ${pay.category}", fontWeight = FontWeight.SemiBold)
                                            Text("Mode: ${pay.paymentMode} | Ref: ${pay.transactionId.ifEmpty { "Cash Tx" }}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("INR ${pay.amount}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Print System Dialog Modal
    if (showPrintPreview && selectedPatient != null) {
        val p = selectedPatient!!
        val pVisits = visits.filter { it.patientRegNo == p.regNo }
        val pPrescriptions = prescriptions.filter { it.patientRegNo == p.regNo }
        val pPayments = payments.filter { it.patientRegNo == p.regNo }

        Dialog(
            onDismissRequest = { showPrintPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Print Preview (Receipt & Prescription Sheet)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showPrintPreview = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Print formatted paper representation
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .background(Color.White)
                            .border(BorderStroke(1.dp, Color.Black))
                            .padding(24.dp)
                    ) {
                        // Header
                        Text("PILES CLINIC ERP SYSTEM", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("Standard Clinical Medical Slip", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))

                        // Patient details row
                        Text("PATIENT REGISTRATION DETAILS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        Text("Reg No: ${p.regNo}     Date: ${p.date}", fontSize = 12.sp, color = Color.Black)
                        Text("Name: ${p.name}     Age: ${p.age}     Gender: ${p.gender}", fontSize = 12.sp, color = Color.Black)
                        Text("Mobile: ${p.mobile}     Condition: ${p.disease}", fontSize = 12.sp, color = Color.Black)
                        Text("Address: ${p.address}", fontSize = 12.sp, color = Color.Black)
                        Divider(color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))

                        // Clinical Section
                        if (pVisits.isNotEmpty()) {
                            Text("CLINICAL VITAL SIGNS & OBSERVATIONS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            val last = pVisits.first()
                            Text("Date: ${last.date}     Doctor: Dr. ${last.doctorName}", fontSize = 12.sp, color = Color.Black)
                            Text("BP: ${last.bp}   Pulse: ${last.pulse}   Weight: ${last.weight} kg", fontSize = 12.sp, color = Color.Black)
                            Text("Symptoms: ${last.symptoms}", fontSize = 12.sp, color = Color.Black)
                            Text("Diagnosis: ${last.diagnosis}", fontSize = 12.sp, color = Color.Black)
                            Divider(color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))
                        }

                        // Prescriptions
                        if (pPrescriptions.isNotEmpty()) {
                            Text("PRESCRIBED TREATMENT / MEDICINE SLIP", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            val pres = pPrescriptions.first()
                            Text("Instructions: ${pres.generalInstructions}", fontSize = 12.sp, color = Color.Black)
                            Divider(color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))
                        }

                        // Payments Receipt
                        if (pPayments.isNotEmpty()) {
                            Text("OFFICIAL BILLING RECEIPTS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            pPayments.forEach { pay ->
                                Text("Date: ${pay.date} | ${pay.category} | Mode: ${pay.paymentMode} : INR ${pay.amount}", fontSize = 12.sp, color = Color.Black)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Grand Total Paid: INR ${pPayments.sumOf { it.amount }}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            Divider(color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Text("Computer Generated Receipt. Signature Not Required.", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Printed successfully / Sent to local print spooler!", Toast.LENGTH_SHORT).show()
                            showPrintPreview = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("CONFIRM PRINT JOB")
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && selectedPatient != null) {
        val p = selectedPatient!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete patient ${p.name} (${p.regNo}) and all associated records? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePatient(p) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                selectedPatient = null
                                showDeleteConfirmDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("DELETE PERMANENTLY")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Edit Patient Demographics Dialog
    if (showEditPatientDialog && selectedPatient != null) {
        val p = selectedPatient!!
        AlertDialog(
            onDismissRequest = { showEditPatientDialog = false },
            title = { Text("Edit Patient Demographics", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editMobile,
                        onValueChange = { editMobile = it },
                        label = { Text("Mobile Number *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAge,
                        onValueChange = { editAge = it },
                        label = { Text("Age *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text("Gender", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Male", "Female", "Other").forEach { g ->
                                FilterChip(
                                    selected = editGender == g,
                                    onClick = { editGender = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column {
                        Text("Disease Condition", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Piles", "Fistula", "Fissure", "Pilonidal Sinus", "Constipation").forEach { d ->
                                FilterChip(
                                    selected = editDisease == d,
                                    onClick = { editDisease = d },
                                    label = { Text(d, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editRemarks,
                        onValueChange = { editRemarks = it },
                        label = { Text("General Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editMobile.isBlank() || editAge.isBlank()) {
                            Toast.makeText(context, "Mandatory fields cannot be blank", Toast.LENGTH_SHORT).show()
                        } else {
                            val updated = p.copy(
                                name = editName.trim(),
                                mobile = editMobile.trim(),
                                age = editAge.toIntOrNull() ?: p.age,
                                gender = editGender,
                                address = editAddress.trim(),
                                disease = editDisease,
                                remarks = editRemarks.trim()
                            )
                            viewModel.updatePatient(updated) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    selectedPatient = updated
                                    showEditPatientDialog = false
                                }
                            }
                        }
                    }
                ) {
                    Text("SAVE CHANGES")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPatientDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}


// ==========================================
// 7. REPORTS MODULE SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ClinicViewModel,
    onBack: () -> Unit
) {
    val enquiries by viewModel.enquiries.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val totalRevenue = payments.sumOf { it.amount }
    val cashRevenue = payments.filter { it.paymentMode == "Cash" }.sumOf { it.amount }
    val upiRevenue = payments.filter { it.paymentMode == "UPI" }.sumOf { it.amount }
    val cardRevenue = payments.filter { it.paymentMode == "Card" }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("ERP Business Reports", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Stats
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Overall Performance Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Enquiries", fontWeight = FontWeight.Bold)
                            Text("${enquiries.size}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Registrations", fontWeight = FontWeight.Bold)
                            Text("${patients.size}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Revenue", fontWeight = FontWeight.Bold)
                            Text("INR ${totalRevenue.toInt()}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // Financial breakdown
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Financial Revenue Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cash Collections:")
                        Text("INR ${cashRevenue.toInt()}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("UPI Transactions:")
                        Text("INR ${upiRevenue.toInt()}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Card Swipes:")
                        Text("INR ${cardRevenue.toInt()}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Enquiry Performance Reports
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enquiry Conversion Statistics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Divider()
                    val conversionRate = if (enquiries.isNotEmpty()) {
                        (enquiries.count { it.status == "Visit Confirmed" }.toDouble() / enquiries.size * 100).toInt()
                    } else 0

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Conversion Rate:")
                        Text("$conversionRate%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Confirmed Visits:")
                        Text("${enquiries.count { it.status == "Visit Confirmed" }}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pending Actions:")
                        Text("${enquiries.count { it.status == "Pending" }}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rejected/Dead Leads:")
                        Text("${enquiries.count { it.status == "Rejected" }}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


// ==========================================
// 8. CLOUD SYNC MODULE (SUPABASE)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: ClinicViewModel,
    onBack: () -> Unit
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var supabaseUrl by remember { mutableStateOf("https://piles-clinic-erp.supabase.co") }
    var supabaseAnonKey by remember { mutableStateOf("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBpbGVzLWNsaW5pYyIsImV4cCI6MTgwMDAwMDAwMH0.ExampleKey") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Cloud Sync Configuration", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Supabase Backend Connection",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase Project REST URL") },
                        modifier = Modifier.fillMaxWidth().testTag("sync_url_field")
                    )

                    OutlinedTextField(
                        value = supabaseAnonKey,
                        onValueChange = { supabaseAnonKey = it },
                        label = { Text("Supabase Anon API Key") },
                        modifier = Modifier.fillMaxWidth().testTag("sync_key_field")
                    )

                    Text(
                        "Configure Supabase tables (patients, enquiries, payments) with matching JSON columns for persistent clinical cloud backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.syncWithSupabase(supabaseUrl, supabaseAnonKey)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("cloud_sync_trigger_btn"),
                enabled = !isSyncing,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SYNCING...")
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("START BACKUP SYNC")
                }
            }

            if (syncStatus != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (syncStatus!!.startsWith("Success"))
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = syncStatus ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
