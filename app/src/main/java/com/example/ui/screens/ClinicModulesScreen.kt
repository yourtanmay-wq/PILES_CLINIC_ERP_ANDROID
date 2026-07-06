package com.example.ui.screens

import android.widget.Toast
import android.app.DatePickerDialog
import java.util.Calendar
import com.example.util.DateUtils
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
import androidx.compose.ui.graphics.asImageBitmap

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

fun formatMobileInput(input: String): String {
    var cleaned = input.filter { it.isDigit() }
    
    // Strip leading 91 or 0 multiple times if present and number is longer than 10 digits
    while (cleaned.length > 10 && (cleaned.startsWith("91") || cleaned.startsWith("0"))) {
        if (cleaned.startsWith("91")) {
            cleaned = cleaned.substring(2)
        } else if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }
    }
    
    return if (cleaned.length >= 10) {
        "+91" + cleaned.take(10)
    } else {
        cleaned
    }
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
fun getAddressPart(address: String, prefix: String): String {
    val pattern = "\\[$prefix:\\s*([^\\]]*)\\]"
    val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
    val match = regex.find(address)
    if (match != null) {
        return match.groupValues[1].trim()
    }
    val altPattern = "$prefix:\\s*([^,]*)"
    val altRegex = altPattern.toRegex(RegexOption.IGNORE_CASE)
    val altMatch = altRegex.find(address)
    return altMatch?.groupValues?.get(1)?.trim() ?: ""
}

fun getRemarksPart(remarks: String, prefix: String): String {
    val pattern = "\\[$prefix:\\s*([^\\]]*)\\]"
    val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
    val match = regex.find(remarks)
    return match?.groupValues?.get(1)?.trim() ?: ""
}

fun getActualRemarks(remarks: String): String {
    val regex = "\\[[^:]+:[^\\]]*\\]".toRegex()
    return regex.replace(remarks, "").trim()
}

fun formatAddressForDisplay(address: String): String {
    if (!address.contains("[")) return address
    val village = getAddressPart(address, "Village")
    val po = getAddressPart(address, "PO")
    val ps = getAddressPart(address, "PS")
    val district = getAddressPart(address, "Dist")
    val pin = getAddressPart(address, "PIN")
    
    return buildList {
        if (village.isNotEmpty()) add(village)
        if (po.isNotEmpty()) add("P.O: $po")
        if (ps.isNotEmpty()) add("P.S: $ps")
        if (district.isNotEmpty()) add(district)
        if (pin.isNotEmpty()) add(pin)
    }.joinToString(", ")
}

fun formatRemarksForDisplay(remarks: String): String {
    val occupation = getRemarksPart(remarks, "Occupation")
    val refBy = getRemarksPart(remarks, "RefBy")
    val symptoms = getRemarksPart(remarks, "Symptoms")
    val actual = getActualRemarks(remarks)
    
    val parts = buildList {
        if (occupation.isNotEmpty()) add("Occ: $occupation")
        if (refBy.isNotEmpty()) add("Ref: $refBy")
        if (symptoms.isNotEmpty()) add("Symptoms: $symptoms")
    }
    val metadata = if (parts.isNotEmpty()) "[${parts.joinToString(" | ")}]" else ""
    return if (actual.isNotEmpty()) {
        if (metadata.isNotEmpty()) "$metadata\n$actual" else actual
    } else {
        metadata
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientRegistrationScreen(
    viewModel: ClinicViewModel,
    currentUserRole: String,
    prefillEnquiry: Enquiry? = null,
    onBack: () -> Unit
) {
    val branches by viewModel.branches.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val enquiries by viewModel.enquiries.collectAsState()
    val doctorVisits by viewModel.doctorVisits.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var name by remember { mutableStateOf(prefillEnquiry?.patientName ?: "") }
    var mobile by remember { mutableStateOf(if (prefillEnquiry?.mobile != null) formatMobileInput(prefillEnquiry.mobile) else "") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    
    // Structured Address States
    var village by remember { mutableStateOf("") }
    var po by remember { mutableStateOf("") }
    var ps by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    // Additional States
    var occupation by remember { mutableStateOf("") }
    var refBy by remember { mutableStateOf("Self") }

    // Dropdown options
    val branchOptions = remember(branches) {
        val defaults = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
        val dbBranchNames = branches.map { br ->
            when (br.name) {
                "KNE" -> "Kishanganj"
                "JPE" -> "Jalpaiguri"
                "COB" -> "Cooch Behar"
                "FLK" -> "Falakata"
                "BIR" -> "Birpara"
                else -> br.name
            }
        }
        (defaults + dbBranchNames).distinct()
    }
    val refByOptions = listOf("Self", "Online", "Offline", "Doctor Visit", "Old Patient Refer", "Other")

    var selectedBranch by remember { mutableStateOf("") }
    
    // Checkbox selections
    var selectedDiseases by remember { mutableStateOf(setOf<String>()) }
    var selectedSymptoms by remember { mutableStateOf(setOf<String>()) }

    var actualRemarks by remember { mutableStateOf("") }
    var initialPaymentAmount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") } // Default to uppercase CASH per rules
    var registrationDate by remember { mutableStateOf(DateUtils.getTodayDateStringDDMMYYYY()) }

    var showDuplicatePopup by remember { mutableStateOf(false) }
    var showAllDetails by remember { mutableStateOf(false) }
    var existingMatchData by remember { mutableStateOf<Any?>(null) }
    var confirmedDuplicateMobile by remember { mutableStateOf("") }

    val context = LocalContext.current

    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val inputStream: java.io.InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                photoBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let {
            photoBitmap = it
        }
    }

    // Helper to auto-fill the form from duplicate entity
    val autoFillFromEntity = { entity: Any ->
        if (entity is Patient) {
            name = entity.name
            mobile = entity.mobile
            age = entity.age.toString()
            gender = entity.gender
            
            // Extract structured address parts
            village = getAddressPart(entity.address, "Village")
            po = getAddressPart(entity.address, "PO")
            ps = getAddressPart(entity.address, "PS")
            district = getAddressPart(entity.address, "Dist")
            pin = getAddressPart(entity.address, "PIN")
            if (village.isEmpty() && entity.address.isNotEmpty()) {
                village = entity.address
            }
            
            selectedBranch = when (entity.branch) {
                "KNE" -> "Kishanganj"
                "JPE" -> "Jalpaiguri"
                "COB" -> "Cooch Behar"
                "FLK" -> "Falakata"
                "BIR" -> "Birpara"
                else -> entity.branch
            }

            // Extract checkboxes
            selectedDiseases = entity.disease.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            occupation = getRemarksPart(entity.remarks, "Occupation")
            refBy = getRemarksPart(entity.remarks, "RefBy").ifEmpty { "Self" }
            val symptomsStr = getRemarksPart(entity.remarks, "Symptoms")
            selectedSymptoms = symptomsStr.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            actualRemarks = getActualRemarks(entity.remarks)
        } else if (entity is Enquiry) {
            name = entity.patientName
            mobile = formatMobileInput(entity.mobile)
            
            selectedBranch = when (entity.branch) {
                "KNE" -> "Kishanganj"
                "JPE" -> "Jalpaiguri"
                "COB" -> "Cooch Behar"
                "FLK" -> "Falakata"
                "BIR" -> "Birpara"
                else -> entity.branch
            }

            selectedDiseases = entity.disease.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            actualRemarks = entity.remarks
        }
    }

    LaunchedEffect(prefillEnquiry) {
        prefillEnquiry?.let {
            autoFillFromEntity(it)
        }
    }

    // Set default branch when loaded
    LaunchedEffect(branchOptions) {
        if (branchOptions.isNotEmpty() && selectedBranch.isEmpty()) {
            selectedBranch = branchOptions.first()
        }
    }

    val calendar = Calendar.getInstance()
    val showDatePicker = { currentDateStr: String, minDate: Long?, maxDate: Long?, onDateSelected: (String) -> Unit ->
        val dateParts = currentDateStr.split("-")
        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH)
        var day = calendar.get(Calendar.DAY_OF_MONTH)

        if (dateParts.size == 3) {
            try {
                val d = dateParts[0].toInt()
                val m = dateParts[1].toInt() - 1
                val y = dateParts[2].toInt()
                if (d in 1..31 && m in 0..11 && y > 1900) {
                    day = d
                    month = m
                    year = y
                }
            } catch (e: Exception) {
                // Keep default calendar
            }
        }

        val dialog = DatePickerDialog(
            context,
            { _, selYear, selMonth, selDay ->
                val formattedDate = String.format("%02d-%02d-%04d", selDay, selMonth + 1, selYear)
                onDateSelected(formattedDate)
            },
            year,
            month,
            day
        )
        if (minDate != null) {
            dialog.datePicker.minDate = minDate
        }
        if (maxDate != null) {
            dialog.datePicker.maxDate = maxDate
        }
        dialog.show()
    }

    if (showDuplicatePopup) {
        val existingPatient = patients.find { it.mobile == mobile }
        val existingEnquiries = enquiries.filter { it.mobile == mobile }
        val patientRegNo = existingPatient?.regNo ?: ""
        val existingVisits = if (patientRegNo.isNotEmpty()) doctorVisits.filter { it.patientRegNo == patientRegNo } else emptyList()
        val existingPayments = if (patientRegNo.isNotEmpty()) payments.filter { it.patientRegNo == patientRegNo } else emptyList()

        AlertDialog(
            onDismissRequest = { showDuplicatePopup = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Duplicate Mobile Detected")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "This mobile number ($mobile) already exists in the system.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Display short summary
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Summary Information:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            if (existingPatient != null) {
                                Text("• Record: Registered Patient", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("• Name: ${existingPatient.name}", style = MaterialTheme.typography.bodySmall)
                                Text("• Reg No: ${existingPatient.regNo}", style = MaterialTheme.typography.bodySmall)
                                Text("• Branch: ${existingPatient.branch}", style = MaterialTheme.typography.bodySmall)
                            } else if (existingEnquiries.isNotEmpty()) {
                                val latestE = existingEnquiries.first()
                                Text("• Record: Enquiry Form", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text("• Name: ${latestE.patientName.ifEmpty { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                                Text("• Branch: ${latestE.branch}", style = MaterialTheme.typography.bodySmall)
                                Text("• Disease: ${latestE.disease}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // View All Details Toggle Button
                    TextButton(
                        onClick = { showAllDetails = !showAllDetails },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = if (showAllDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (showAllDetails) "Hide All Journey Details" else "View All Journey Details", fontWeight = FontWeight.Bold)
                    }

                    if (showAllDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 1. Enquiries Journey
                            if (existingEnquiries.isNotEmpty()) {
                                Text("Enquiry Records (${existingEnquiries.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                                existingEnquiries.forEach { eq ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Date: ${DateUtils.formatDisplayDate(eq.date)} | Status: ${eq.status}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                            Text("Branch: ${eq.branch} | Disease: ${eq.disease}", style = MaterialTheme.typography.bodySmall)
                                            if (eq.remarks.isNotEmpty()) {
                                                Text("Remarks: ${eq.remarks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Doctor Visits
                            if (existingVisits.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Doctor Visits (${existingVisits.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                                existingVisits.forEach { vs ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Date: ${DateUtils.formatDisplayDate(vs.date)} | Doctor: ${vs.doctorName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                            Text("Symptoms: ${vs.symptoms}", style = MaterialTheme.typography.bodySmall)
                                            Text("Diagnosis: ${vs.diagnosis}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            // 3. Payments
                            if (existingPayments.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Payments & Billing (${existingPayments.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                                existingPayments.forEach { py ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Date: ${DateUtils.formatDisplayDate(py.date)} | Category: ${py.category}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                            Text("Amount: INR ${py.amount} | Mode: ${py.paymentMode}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        onClick = {
                            showDuplicatePopup = false
                            mobile = ""
                            existingMatchData = null
                            showAllDetails = false
                        }
                    ) {
                        Text("Cancel", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            showDuplicatePopup = false
                            confirmedDuplicateMobile = mobile
                            existingMatchData?.let { autoFillFromEntity(it) }
                            showAllDetails = false
                        }
                    ) {
                        Text("Continue", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Patient Registration ERP", fontWeight = FontWeight.Bold) },
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
            
            // CARD 1: CLINICAL ARRIVAL INFO (DATE & BRANCH)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Arrival & Branch Assignment",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Registration Date Clickable Date Box
                    ClickableDateBox(
                        label = "Registration Date *",
                        value = registrationDate,
                        placeholder = "DD-MM-YYYY",
                        leadingIcon = Icons.Default.DateRange,
                        supportingText = "Auto-today. Clicks open date calendar.",
                        onClick = {
                            showDatePicker(registrationDate, null, System.currentTimeMillis()) { registrationDate = it }
                        },
                        testTag = "reg_date_input"
                    )

                    // Branch Dropdown (Mandatory)
                    var branchDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedBranch,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Branch *", fontWeight = FontWeight.Bold) },
                            trailingIcon = {
                                IconButton(onClick = { branchDropdownExpanded = !branchDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Branch")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { branchDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = branchDropdownExpanded,
                            onDismissRequest = { branchDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            branchOptions.forEach { bName ->
                                DropdownMenuItem(
                                    text = { Text(bName, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedBranch = bName
                                        branchDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // CARD 2: PATIENT PROFILE DEMOGRAPHICS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Patient Demographics",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Photo Picker Section
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(90.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            if (photoBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = photoBitmap!!.asImageBitmap(),
                                    contentDescription = "Patient Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Default Profile",
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Patient Photo",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Allowed from camera or gallery. Photo never blocks saving.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Camera", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Gallery", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Patient Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("patient_name_field"),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { inputVal ->
                            mobile = formatMobileInput(inputVal)

                            // Trigger real-time duplicate check when formatted mobile reaches full length
                            if (mobile.length == 13 && mobile != confirmedDuplicateMobile) {
                                val matchP = patients.find { it.mobile == mobile }
                                val matchE = enquiries.find { it.mobile == mobile }
                                if (matchP != null || matchE != null) {
                                    existingMatchData = matchP ?: matchE
                                    showDuplicatePopup = true
                                }
                            }
                        },
                        label = { Text("Patient Mobile *") },
                        placeholder = { Text("10-digit Indian Mobile") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("patient_mobile_field"),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
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
                            modifier = Modifier.weight(1f).testTag("patient_age_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Column(modifier = Modifier.weight(1.2f)) {
                            Text("Gender", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Male", "Female", "Other").forEach { g ->
                                    FilterChip(
                                        selected = gender == g,
                                        onClick = { gender = g },
                                        label = { Text(g, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = { Text("Occupation") },
                        placeholder = { Text("Farmer, Business, Service, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Ref By Dropdown
                    var refDropdownExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = refBy,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Referred By", fontWeight = FontWeight.Bold) },
                            trailingIcon = {
                                IconButton(onClick = { refDropdownExpanded = !refDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Referral Source")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { refDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = refDropdownExpanded,
                            onDismissRequest = { refDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            refByOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        refBy = option
                                        refDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // CARD 3: FULL ADDRESS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Full Address",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = village,
                        onValueChange = { village = it },
                        label = { Text("Village / Street") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = po,
                            onValueChange = { po = it },
                            label = { Text("PO") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = ps,
                            onValueChange = { ps = it },
                            label = { Text("PS") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = district,
                            onValueChange = { district = it },
                            label = { Text("District") },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // CARD 4: CLINICAL DIAGNOSIS (DISEASE & SYMPTOMS CHECKBOXES)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Clinical Assessment",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Diseases checkboxes
                    Text("Disease Options (Tick multiple) *", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val diseaseOptions = listOf("Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog", "Other")
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in diseaseOptions.indices step 2) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val current = diseaseOptions[i]
                                            selectedDiseases = if (selectedDiseases.contains(current)) selectedDiseases - current else selectedDiseases + current
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedDiseases.contains(diseaseOptions[i]),
                                        onCheckedChange = { checked ->
                                            val current = diseaseOptions[i]
                                            selectedDiseases = if (checked) selectedDiseases + current else selectedDiseases - current
                                        }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(diseaseOptions[i], style = MaterialTheme.typography.bodyMedium)
                                }
                                if (i + 1 < diseaseOptions.size) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val current = diseaseOptions[i + 1]
                                                selectedDiseases = if (selectedDiseases.contains(current)) selectedDiseases - current else selectedDiseases + current
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = selectedDiseases.contains(diseaseOptions[i + 1]),
                                            onCheckedChange = { checked ->
                                                val current = diseaseOptions[i + 1]
                                                selectedDiseases = if (checked) selectedDiseases + current else selectedDiseases - current
                                            }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(diseaseOptions[i + 1], style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Symptoms checkboxes
                    Text("Symptom Options (Tick multiple) *", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val symptomOptions = listOf("Pain", "Bleeding", "Burning", "Itching", "Swelling", "Pus Discharge", "Other")

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in symptomOptions.indices step 2) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            val current = symptomOptions[i]
                                            selectedSymptoms = if (selectedSymptoms.contains(current)) selectedSymptoms - current else selectedSymptoms + current
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = selectedSymptoms.contains(symptomOptions[i]),
                                        onCheckedChange = { checked ->
                                            val current = symptomOptions[i]
                                            selectedSymptoms = if (checked) selectedSymptoms + current else selectedSymptoms - current
                                        }
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(symptomOptions[i], style = MaterialTheme.typography.bodyMedium)
                                }
                                if (i + 1 < symptomOptions.size) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val current = symptomOptions[i + 1]
                                                selectedSymptoms = if (selectedSymptoms.contains(current)) selectedSymptoms - current else selectedSymptoms + current
                                            }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = selectedSymptoms.contains(symptomOptions[i + 1]),
                                            onCheckedChange = { checked ->
                                                val current = symptomOptions[i + 1]
                                                selectedSymptoms = if (checked) selectedSymptoms + current else selectedSymptoms - current
                                            }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(symptomOptions[i + 1], style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // CARD 5: PAYMENT & ADDITIONAL REMARKS
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Billing & General Remarks",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = actualRemarks,
                        onValueChange = { actualRemarks = it },
                        label = { Text("Clinical History / General Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = initialPaymentAmount,
                        onValueChange = { initialPaymentAmount = it },
                        label = { Text("Registration Fee *") },
                        supportingText = { Text("Mandatory fee (numeric only, must be greater than zero)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Payment Mode", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CASH", "UPI").forEach { mode ->
                            FilterChip(
                                selected = paymentMode.uppercase() == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val feeInt = initialPaymentAmount.trim().toIntOrNull() ?: 0
                    if (name.isBlank() || mobile.isBlank() || age.isBlank() || selectedBranch.isBlank() || selectedDiseases.isEmpty() || selectedSymptoms.isEmpty()) {
                        Toast.makeText(context, "Please complete all fields marked with * (Name, Mobile, Age, Branch, and at least one disease/symptom)", Toast.LENGTH_LONG).show()
                    } else if (feeInt <= 0) {
                        Toast.makeText(context, "Registration Fee is mandatory. It cannot be blank or zero.", Toast.LENGTH_LONG).show()
                    } else {
                        val matchP = patients.find { it.mobile == mobile }
                        val matchE = enquiries.find { it.mobile == mobile }
                        if ((matchP != null || matchE != null) && mobile != confirmedDuplicateMobile) {
                            existingMatchData = matchP ?: matchE
                            showDuplicatePopup = true
                        } else {
                            // Concatenate full address
                            val finalAddress = buildAddressString(
                                village = village,
                                po = po,
                                ps = ps,
                                district = district,
                                pin = pin
                             )

                            // Prepare diseases
                            val finalDiseases = selectedDiseases.joinToString(", ")

                            // Compile remarks metadata
                            val finalRemarks = buildRemarksString(
                                occupation = occupation,
                                refBy = refBy,
                                symptoms = selectedSymptoms.joinToString(", "),
                                actualRemarks = actualRemarks
                            )

                            viewModel.registerPatient(
                                name = name,
                                mobile = mobile,
                                age = age,
                                gender = gender,
                                address = finalAddress,
                                branch = selectedBranch,
                                disease = finalDiseases,
                                remarks = finalRemarks,
                                initialPaymentAmount = initialPaymentAmount,
                                paymentMode = paymentMode,
                                registeredBy = currentUserRole,
                                date = registrationDate,
                                onComplete = { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        // Auto-compress and save patient photo
                                        photoBitmap?.let { bitmap ->
                                            try {
                                                val prefix = "Reg No: "
                                                val index = msg.indexOf(prefix)
                                                val regNo = if (index != -1) {
                                                    msg.substring(index + prefix.length).trim()
                                                } else {
                                                    val uPrefix = "No: "
                                                    val uIndex = msg.indexOf(uPrefix)
                                                    if (uIndex != -1) msg.substring(uIndex + uPrefix.length).trim() else ""
                                                }
                                                if (regNo.isNotEmpty()) {
                                                    val maxDim = 800
                                                    val w = bitmap.width
                                                    val h = bitmap.height
                                                    val scaled = if (w > maxDim || h > maxDim) {
                                                        val ratio = w.toFloat() / h.toFloat()
                                                        val newW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                                                        val newH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                                                        android.graphics.Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                                                    } else {
                                                        bitmap
                                                    }
                                                    val file = java.io.File(context.filesDir, "${regNo}_photo.jpg")
                                                    val out = java.io.FileOutputStream(file)
                                                    scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
                                                    out.flush()
                                                    out.close()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }

                                        // Auto-close/Visited prefilled enquiry
                                        prefillEnquiry?.let {
                                            viewModel.closeEnquiry(it.id)
                                        }

                                        onBack()
                                    }
                                }
                            )
                        }
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

// Structured address and remarks helpers
private fun buildAddressString(village: String, po: String, ps: String, district: String, pin: String): String {
    return "[Village: $village][PO: $po][PS: $ps][Dist: $district][PIN: $pin]"
}

private fun buildRemarksString(
    occupation: String,
    refBy: String,
    symptoms: String,
    actualRemarks: String
): String {
    val base = "[Occupation: $occupation][RefBy: $refBy][Symptoms: $symptoms]"
    val clean = getActualRemarks(actualRemarks)
    return if (clean.isEmpty()) base else "$base\n$clean"
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
                            listOf("Registration", "Treatment", "Medicine", "Blood Test", "Diet Chart", "Kshar Sutra").forEach { cat ->
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
                            Text("Address: ${formatAddressForDisplay(patient.address)}")
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
                        Text("Address: ${formatAddressForDisplay(p.address)}", fontSize = 12.sp, color = Color.Black)
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

fun getPatientStage(remarks: String): String {
    val prefix = "[Stage: "
    val suffix = "]"
    if (remarks.contains(prefix)) {
        val startIndex = remarks.indexOf(prefix) + prefix.length
        val endIndex = remarks.indexOf(suffix, startIndex)
        if (endIndex > startIndex) {
            return remarks.substring(startIndex, endIndex)
        }
    }
    return "Registration Fee Paid"
}

fun setPatientStage(currentRemarks: String, newStage: String): String {
    val prefix = "[Stage: "
    val suffix = "]"
    val cleanRemarks = if (currentRemarks.contains(prefix)) {
        val startIndex = currentRemarks.indexOf(prefix)
        val endIndex = currentRemarks.indexOf(suffix, startIndex)
        if (endIndex >= startIndex) {
            currentRemarks.removeRange(startIndex, endIndex + suffix.length).trim()
        } else {
            currentRemarks
        }
    } else {
        currentRemarks.trim()
    }
    return if (cleanRemarks.isEmpty()) {
        "$prefix$newStage$suffix"
    } else {
        "$cleanRemarks\n$prefix$newStage$suffix"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientVisitSectionScreen(
    viewModel: ClinicViewModel,
    currentUserRole: String,
    onBack: () -> Unit
) {
    val patients by viewModel.patients.collectAsState()
    val payments by viewModel.payments.collectAsState()

    val paidPatientRegNos = remember(payments) {
        payments.filter { it.category.equals("Registration", ignoreCase = true) && it.amount > 0.0 }
            .map { it.patientRegNo }
            .toSet()
    }

    val visitSectionPatients = remember(patients, paidPatientRegNos) {
        patients.filter { paidPatientRegNos.contains(it.regNo) }
    }

    var selectedPatientForView by remember { mutableStateOf<Patient?>(null) }
    var selectedPatientForPrint by remember { mutableStateOf<Patient?>(null) }
    var selectedPatientForAdvance by remember { mutableStateOf<Patient?>(null) }

    var searchByMobileOrName by remember { mutableStateOf("") }

    val filteredList = remember(visitSectionPatients, searchByMobileOrName) {
        if (searchByMobileOrName.isBlank()) {
            visitSectionPatients
        } else {
            visitSectionPatients.filter {
                it.name.contains(searchByMobileOrName, ignoreCase = true) ||
                it.mobile.contains(searchByMobileOrName) ||
                it.regNo.contains(searchByMobileOrName, ignoreCase = true)
            }
        }
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Visit Section (Paid Patients)", fontWeight = FontWeight.Bold) },
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
            // Search field
            OutlinedTextField(
                value = searchByMobileOrName,
                onValueChange = { searchByMobileOrName = it },
                label = { Text("Search by Name / Mobile / ID") },
                placeholder = { Text("e.g. Rahul Kumar") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No paid patients in Visit Section yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "Registration Fee Payment is required to enter Visit Section.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { patient ->
                        val regPayment = payments.find {
                            it.patientRegNo == patient.regNo &&
                            it.category.equals("Registration", ignoreCase = true)
                        }
                        val visitDate = regPayment?.date ?: patient.date

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = patient.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "ID: ${patient.regNo}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    // Soft light gold branch badge
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                    ) {
                                        Text(
                                            text = "Branch: ${patient.branch}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF78350F)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Mobile", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(patient.mobile, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Visit Date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(visitDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                val currentStage = getPatientStage(patient.remarks)
                                var stageExpanded by remember { mutableStateOf(false) }
                                val stages = listOf(
                                    "Registration Fee Paid",
                                    "Advance Payment Done",
                                    "Treatment Running",
                                    "Treatment Follow-up",
                                    "Completed"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Treatment Stage:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Box {
                                        Surface(
                                            color = when (currentStage) {
                                                "Completed" -> MaterialTheme.colorScheme.primaryContainer
                                                "Treatment Running" -> MaterialTheme.colorScheme.tertiaryContainer
                                                "Treatment Follow-up" -> MaterialTheme.colorScheme.secondaryContainer
                                                "Advance Payment Done" -> Color(0xFFE0F2FE)
                                                else -> Color(0xFFF3F4F6)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier.clickable { stageExpanded = true }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val stageIcon = when (currentStage) {
                                                    "Completed" -> Icons.Default.CheckCircle
                                                    "Treatment Running" -> Icons.Default.PlayArrow
                                                    "Treatment Follow-up" -> Icons.Default.SettingsBackupRestore
                                                    "Advance Payment Done" -> Icons.Default.Payments
                                                    else -> Icons.Default.HourglassEmpty
                                                }
                                                Icon(
                                                    imageVector = stageIcon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = when (currentStage) {
                                                        "Completed" -> MaterialTheme.colorScheme.primary
                                                        "Treatment Running" -> MaterialTheme.colorScheme.tertiary
                                                        "Treatment Follow-up" -> MaterialTheme.colorScheme.secondary
                                                        "Advance Payment Done" -> Color(0xFF0369A1)
                                                        else -> Color.Gray
                                                    }
                                                )
                                                Text(
                                                    text = currentStage,
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = when (currentStage) {
                                                        "Completed" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "Treatment Running" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        "Treatment Follow-up" -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        "Advance Payment Done" -> Color(0xFF0369A1)
                                                        else -> Color.DarkGray
                                                    }
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = Color.Gray
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = stageExpanded,
                                            onDismissRequest = { stageExpanded = false }
                                        ) {
                                            stages.forEach { st ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = st,
                                                            fontWeight = if (st == currentStage) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    onClick = {
                                                        stageExpanded = false
                                                        if (st != currentStage) {
                                                            val updatedRemarks = setPatientStage(patient.remarks, st)
                                                            val updatedPatient = patient.copy(remarks = updatedRemarks)
                                                            viewModel.updatePatient(updatedPatient) { _, _ -> }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // View Button
                                    OutlinedButton(
                                        onClick = { selectedPatientForView = patient },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("View", fontSize = 12.sp)
                                    }

                                    // Print Button
                                    OutlinedButton(
                                        onClick = { selectedPatientForPrint = patient },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Print", fontSize = 12.sp)
                                    }

                                    // Advance Button (Filled healing green style)
                                    Button(
                                        onClick = { selectedPatientForAdvance = patient },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Advance", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // View Patient Detail Dialog
    selectedPatientForView?.let { patient ->
        val patientPayments = payments.filter { it.patientRegNo == patient.regNo }
        AlertDialog(
            onDismissRequest = { selectedPatientForView = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Patient EMR Clinical Overview")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    ListItem(
                        headlineContent = { Text("Name: ${patient.name}", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Age: ${patient.age} | Gender: ${patient.gender}") }
                    )
                    ListItem(
                        headlineContent = { Text("Mobile: ${patient.mobile}") },
                        supportingContent = { Text("Reg Date: ${patient.date} | Reg By: ${patient.registeredBy}") }
                    )
                    ListItem(
                        headlineContent = { Text("Condition/Disease: ${patient.disease}", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Branch: ${patient.branch}") }
                    )
                    if (patient.address.isNotEmpty()) {
                        ListItem(
                            headlineContent = { Text("Address") },
                            supportingContent = { Text(formatAddressForDisplay(patient.address)) }
                        )
                    }
                    if (patient.remarks.isNotEmpty()) {
                        ListItem(
                            headlineContent = { Text("Clinical History Remarks") },
                            supportingContent = { Text(formatRemarksForDisplay(patient.remarks)) }
                        )
                    }

                    Divider()
                    Text("Payment Records List:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (patientPayments.isEmpty()) {
                        Text("No payment records found.")
                    } else {
                        patientPayments.forEach { pay ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(pay.category, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("Date: ${pay.date} | Mode: ${pay.paymentMode}", fontSize = 10.sp)
                                    }
                                    Text("INR ${pay.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedPatientForView = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Print Receipt / Visit Pass Dialog
    selectedPatientForPrint?.let { patient ->
        val regPayment = payments.find {
            it.patientRegNo == patient.regNo &&
            it.category.equals("Registration", ignoreCase = true)
        }
        val visitDate = regPayment?.date ?: patient.date

        Dialog(onDismissRequest = { selectedPatientForPrint = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = "VISIT PASS & RECEIPT",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "HERBAL CLINIC SYSTEMS ERP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Divider(color = MaterialTheme.colorScheme.primary, thickness = 1.dp)

                    // Details block
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Patient ID:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(patient.regNo, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Patient Name:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(patient.name, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Mobile Number:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(patient.mobile, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Assigned Branch:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(patient.branch, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Clinical Date:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(visitDate, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Condition:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(patient.disease, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Registration Fee Paid:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("INR ${regPayment?.amount ?: 0.0}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        if (regPayment?.paymentMode != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Mode:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(regPayment.paymentMode, fontSize = 11.sp)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Text(
                        text = "This is a computer generated slip for clinical registration. Please proceed to the treatment check-up section.",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedPatientForPrint = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }
                        Button(
                            onClick = {
                                Toast.makeText(context, "Printing visit receipt for ${patient.name}...", Toast.LENGTH_SHORT).show()
                                selectedPatientForPrint = null
                            },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Print Pass")
                        }
                    }
                }
            }
        }
    }

    // Advance Payment Dialog
    selectedPatientForAdvance?.let { patient ->
        var advAmount by remember { mutableStateOf("1000") }
        var advPaymentMode by remember { mutableStateOf("Cash") }
        var advTxnId by remember { mutableStateOf("") }
        var advRemarks by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedPatientForAdvance = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text("Collect Advance Payment")
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Patient: ${patient.name} (${patient.regNo})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = advAmount,
                        onValueChange = { advAmount = it },
                        label = { Text("Advance Amount (INR) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Mode", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "UPI", "Card", "NetBanking").forEach { mode ->
                            FilterChip(
                                selected = advPaymentMode == mode,
                                onClick = { advPaymentMode = mode },
                                label = { Text(mode) }
                            )
                        }
                    }

                    if (advPaymentMode != "Cash") {
                        OutlinedTextField(
                            value = advTxnId,
                            onValueChange = { advTxnId = it },
                            label = { Text("Transaction ID / Reference") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = advRemarks,
                        onValueChange = { advRemarks = it },
                        label = { Text("Remarks (Treatment advance / booking)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    onClick = {
                        val amountVal = advAmount.toDoubleOrNull()
                        if (amountVal == null || amountVal <= 0.0) {
                            Toast.makeText(context, "Please enter a valid positive amount", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addPayment(
                                patientRegNo = patient.regNo,
                                category = "Treatment",
                                amountStr = advAmount,
                                paymentMode = advPaymentMode,
                                transactionId = advTxnId,
                                remarks = "Treatment Advance: ${advRemarks.trim()}".trim(),
                                receivedBy = currentUserRole,
                                onComplete = { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        val currentStage = getPatientStage(patient.remarks)
                                        if (currentStage == "Registration Fee Paid") {
                                            val updatedRemarks = setPatientStage(patient.remarks, "Advance Payment Done")
                                            val updatedPatient = patient.copy(remarks = updatedRemarks)
                                            viewModel.updatePatient(updatedPatient) { _, _ -> }
                                        }
                                        selectedPatientForAdvance = null
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Text("Record Advance Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPatientForAdvance = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
