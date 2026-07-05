package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StaffDirectory
import com.example.ui.viewmodel.EnquiryViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnquiryFormScreen(
    viewModel: EnquiryViewModel,
    currentRole: String,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val insertSuccess by viewModel.insertSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Form states
    var enquiryDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }
    var mobile by remember { mutableStateOf("") }
    var patientName by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("KNE") }
    var selectedStaff by remember { mutableStateOf(StaffDirectory.list.first().displayName) }
    var selectedDisease by remember { mutableStateOf("Piles") }
    var shortAddress by remember { mutableStateOf("") }
    var selectedCallTiming by remember { mutableStateOf("Official Time") }
    var remarks by remember { mutableStateOf("") }
    var nextFollowUpDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }

    // Dropdown expanded states
    var branchExpanded by remember { mutableStateOf(false) }
    var staffExpanded by remember { mutableStateOf(false) }
    var diseaseExpanded by remember { mutableStateOf(false) }
    var timingExpanded by remember { mutableStateOf(false) }

    val branchList = listOf("KNE", "JPE", "COB", "FLK", "BIR")
    val diseaseList = listOf("Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog")
    val timingList = listOf("Official Time", "Unexpected Time")

    // Handle Success and Error states from viewmodel
    LaunchedEffect(insertSuccess) {
        if (insertSuccess == true) {
            Toast.makeText(context, "Enquiry added successfully!", Toast.LENGTH_LONG).show()
            viewModel.resetInsertStatus()
            onSuccess()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Local Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AddBusiness,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "New Patient Enquiry",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General clinical warning or banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Please complete all fields carefully. (*) indicates mandatory fields.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Form Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Date
                    OutlinedTextField(
                        value = enquiryDate,
                        onValueChange = { enquiryDate = it },
                        label = { Text("Enquiry Date (YYYY-MM-DD) *") },
                        placeholder = { Text("e.g. 2026-07-04") },
                        supportingText = { Text("Auto-today, allow past/today dates only") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_date_input"),
                        singleLine = true
                    )

                    // 2. Mobile
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("Patient Mobile *") },
                        placeholder = { Text("e.g. 9883605917 or +919883605917") },
                        supportingText = { Text("Mandatory. +91 prefix is formatted automatically") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_mobile_input"),
                        singleLine = true
                    )

                    // 3. Patient Name
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name (Optional)") },
                        placeholder = { Text("Enter patient full name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_name_input"),
                        singleLine = true
                    )

                    // 4. Branch Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedBranch,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Branch *") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { branchExpanded = !branchExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { branchExpanded = !branchExpanded }
                                .testTag("form_branch_dropdown")
                        )
                        DropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            branchList.forEach { branch ->
                                DropdownMenuItem(
                                    text = { Text(branch) },
                                    onClick = {
                                        selectedBranch = branch
                                        branchExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 5. Call Received By
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedStaff,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Call Received By *") },
                            leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { staffExpanded = !staffExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { staffExpanded = !staffExpanded }
                                .testTag("form_staff_dropdown")
                        )
                        DropdownMenu(
                            expanded = staffExpanded,
                            onDismissRequest = { staffExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            StaffDirectory.list.forEach { staff ->
                                DropdownMenuItem(
                                    text = { Text(staff.displayName) },
                                    onClick = {
                                        selectedStaff = staff.displayName
                                        staffExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 6. Disease Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedDisease,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Disease Selection *") },
                            leadingIcon = { Icon(Icons.Default.MedicalInformation, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { diseaseExpanded = !diseaseExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { diseaseExpanded = !diseaseExpanded }
                                .testTag("form_disease_dropdown")
                        )
                        DropdownMenu(
                            expanded = diseaseExpanded,
                            onDismissRequest = { diseaseExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            diseaseList.forEach { disease ->
                                DropdownMenuItem(
                                    text = { Text(disease) },
                                    onClick = {
                                        selectedDisease = disease
                                        diseaseExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 7. Short Address
                    OutlinedTextField(
                        value = shortAddress,
                        onValueChange = { shortAddress = it },
                        label = { Text("Short Address") },
                        placeholder = { Text("Patient short residence location") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_address_input"),
                        singleLine = true
                    )

                    // 8. Call Timing Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCallTiming,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Call Timing *") },
                            leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { timingExpanded = !timingExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { timingExpanded = !timingExpanded }
                                .testTag("form_timing_dropdown")
                        )
                        DropdownMenu(
                            expanded = timingExpanded,
                            onDismissRequest = { timingExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            timingList.forEach { timing ->
                                DropdownMenuItem(
                                    text = { Text(timing) },
                                    onClick = {
                                        selectedCallTiming = timing
                                        timingExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 9. Remarks
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Call Conversation Remarks *") },
                        placeholder = { Text("Enter detailed discussion details") },
                        leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("form_remarks_input"),
                        maxLines = 4
                    )

                    // 10. Next Follow-up Date
                    OutlinedTextField(
                        value = nextFollowUpDate,
                        onValueChange = { nextFollowUpDate = it },
                        label = { Text("Next Follow-up Date (YYYY-MM-DD) *") },
                        placeholder = { Text("e.g. 2026-07-05") },
                        supportingText = { Text("Mandatory, today or future dates only") },
                        leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_followup_date_input"),
                        singleLine = true
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    viewModel.addEnquiry(
                        date = enquiryDate,
                        mobile = mobile,
                        patientName = patientName,
                        branch = selectedBranch,
                        callReceivedBy = selectedStaff,
                        disease = selectedDisease,
                        shortAddress = shortAddress,
                        callTiming = selectedCallTiming,
                        remarks = remarks,
                        nextFollowUpDate = nextFollowUpDate
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("form_submit_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE ENQUIRY RECORD",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
