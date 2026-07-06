package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StaffDirectory
import com.example.ui.viewmodel.EnquiryViewModel
import com.example.ui.viewmodel.LoginViewModel
import com.example.ui.viewmodel.ClinicViewModel
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnquiryFormScreen(
    viewModel: EnquiryViewModel,
    loginViewModel: LoginViewModel,
    clinicViewModel: ClinicViewModel,
    currentRole: String,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val insertSuccess by viewModel.insertSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val loggedInPhone by loginViewModel.userPhone.collectAsState()
    
    // Determine default staff display
    val defaultStaffDisplay = remember(loggedInPhone, currentRole) {
        val cleanPhone = loggedInPhone.filter { it.isDigit() }.takeLast(10)
        val matchingStaff = StaffDirectory.list.find { it.phone == cleanPhone }
        if (matchingStaff != null) {
            "${matchingStaff.name} (${matchingStaff.branch})"
        } else {
            // Default fallbacks
            when (currentRole) {
                "MASTER ADMIN" -> "TK BISWAS (ALL)"
                "STAFF" -> "LAXMI (KNE)"
                else -> {
                    val matchingRole = StaffDirectory.list.find { it.name.equals(currentRole, ignoreCase = true) }
                    if (matchingRole != null) {
                        "${matchingRole.name} (${matchingRole.branch})"
                    } else {
                        "LAXMI (KNE)"
                    }
                }
            }
        }
    }

    // Form states (Only Enquiry Date is auto-filled, others are blank initially)
    var enquiryDate by remember { mutableStateOf(DateUtils.getTodayDateStringDDMMYYYY()) }
    var mobile by remember { mutableStateOf("") }
    var patientName by remember { mutableStateOf("") }
    var selectedBranch by remember { mutableStateOf("") }
    var selectedStaff by remember(defaultStaffDisplay) { mutableStateOf(defaultStaffDisplay) }
    var selectedDisease by remember { mutableStateOf("") }
    var shortAddress by remember { mutableStateOf("") }
    var selectedCallTiming by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var nextFollowUpDate by remember { mutableStateOf("") }

    // Dropdown expanded states
    var branchExpanded by remember { mutableStateOf(false) }
    var staffExpanded by remember { mutableStateOf(false) }
    var diseaseExpanded by remember { mutableStateOf(false) }
    var timingExpanded by remember { mutableStateOf(false) }

    val branchList = listOf("Kishanganj", "Jalpaiguri", "Cooch Behar", "Falakata", "Birpara")
    val diseaseList = listOf("Piles", "Fissure", "Fistula", "Hydrocele", "Gupt Rog")
    val timingList = listOf("Official Time", "Unexpected Time")

    // Duplicate Mobile check state
    var showDuplicatePopup by remember { mutableStateOf(false) }
    var confirmedDuplicateMobile by remember { mutableStateOf("") }

    val enquiries by viewModel.enquiries.collectAsState()
    val patients by clinicViewModel.patients.collectAsState()

    // LaunchedEffect to check duplicate immediately after complete mobile entered/pasted
    LaunchedEffect(mobile) {
        if (mobile.length == 13 && mobile != confirmedDuplicateMobile) {
            val existsPatient = patients.any { it.mobile == mobile }
            val existsEnquiry = enquiries.any { it.mobile == mobile }
            if (existsPatient || existsEnquiry) {
                showDuplicatePopup = true
            }
        }
    }

    // Fresh form every time: Reset all fields whenever screen is launched/initialized
    LaunchedEffect(Unit) {
        enquiryDate = DateUtils.getTodayDateStringDDMMYYYY()
        mobile = ""
        patientName = ""
        selectedBranch = ""
        selectedStaff = defaultStaffDisplay
        selectedDisease = ""
        shortAddress = ""
        selectedCallTiming = ""
        remarks = ""
        nextFollowUpDate = ""
        viewModel.resetInsertStatus()
    }

    // Handle Success and Error states from viewmodel
    LaunchedEffect(insertSuccess) {
        if (insertSuccess == true) {
            Toast.makeText(context, "Enquiry added successfully!", Toast.LENGTH_LONG).show()
            viewModel.resetInsertStatus()
            onSuccess() // Redirects to Enquiry Follow-up tab
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, "Validation/Database Error: $it", Toast.LENGTH_LONG).show()
            viewModel.resetInsertStatus()
        }
    }

    // DatePicker Dialog Helper (DD-MM-YYYY format)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Premium Clinical Header with Elegant Teal-Blue Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AddBusiness,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NEW PATIENT ENQUIRY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = "Hospital ERP Intake Console",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-End Gold Clinical Status Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFEF3C7) // soft light gold
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)) // border amber-gold
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Fields marked with (*) are mandatory. Input dates are strictly validated in DD-MM-YYYY format.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF78350F)
                        )
                    )
                }
            }

            // Beautiful Clinical Card Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section Header
                    Text(
                        text = "Demographics & Call Intake",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    // 1. Enquiry Date (Entire Date box clickable)
                    ClickableDateBox(
                        label = "Enquiry Date *",
                        value = enquiryDate,
                        placeholder = "DD-MM-YYYY",
                        leadingIcon = Icons.Default.DateRange,
                        supportingText = "Auto-today. Clicks open date calendar.",
                        onClick = {
                            showDatePicker(enquiryDate, null, System.currentTimeMillis()) { enquiryDate = it }
                        },
                        testTag = "form_date_input"
                    )

                    // 2. Patient Mobile (Mandatory)
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = formatMobileInput(it) },
                        label = { Text("Patient Mobile *", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("e.g. 9883605917") },
                        supportingText = { Text("Required. 10-digits will auto-format to +91.") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_mobile_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // 3. Patient Name (Optional)
                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name (Optional)", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Enter patient full name") },
                        supportingText = { Text("Optional field.") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Branch & Clinical Routing",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    // 4. Branch Dropdown
                    ClickableDropdownField(
                        label = "Clinical Branch *",
                        value = selectedBranch,
                        placeholder = "Select Branch",
                        leadingIcon = Icons.Default.LocationOn,
                        items = branchList,
                        onItemSelected = { selectedBranch = it },
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = it },
                        testTag = "form_branch_dropdown"
                    )

                    // 5. Call Received By
                    val staffNames = StaffDirectory.list.map { "${it.name} (${it.branch})" }
                    ClickableDropdownField(
                        label = "Call Received By *",
                        value = selectedStaff,
                        placeholder = "Select Clinical Attendant",
                        leadingIcon = Icons.Default.SupportAgent,
                        items = staffNames,
                        onItemSelected = { selectedStaff = it },
                        expanded = staffExpanded,
                        onExpandedChange = { staffExpanded = it },
                        testTag = "form_staff_dropdown"
                    )

                    // 6. Disease Dropdown
                    ClickableDropdownField(
                        label = "Disease Selection (Optional)",
                        value = selectedDisease,
                        placeholder = "Select Disease",
                        leadingIcon = Icons.Default.MedicalInformation,
                        items = diseaseList,
                        onItemSelected = { selectedDisease = it },
                        expanded = diseaseExpanded,
                        onExpandedChange = { diseaseExpanded = it },
                        testTag = "form_disease_dropdown"
                    )

                    // 7. Short Address
                    OutlinedTextField(
                        value = shortAddress,
                        onValueChange = { shortAddress = it },
                        label = { Text("Short Address (Optional)", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Patient short residential location") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_address_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // 8. Call Timing Dropdown
                    ClickableDropdownField(
                        label = "Call Timing (Optional)",
                        value = selectedCallTiming,
                        placeholder = "Select Preferred Call Time",
                        leadingIcon = Icons.Default.Alarm,
                        items = timingList,
                        onItemSelected = { selectedCallTiming = it },
                        expanded = timingExpanded,
                        onExpandedChange = { timingExpanded = it },
                        testTag = "form_timing_dropdown"
                    )

                    // 9. Remarks
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Conversation Remarks *", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Enter detailed discussion details") },
                        leadingIcon = { Icon(Icons.Default.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("form_remarks_input"),
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // 10. Next Follow-up Date (Entire Date box clickable)
                    ClickableDateBox(
                        label = "Next Follow-up Date *",
                        value = nextFollowUpDate,
                        placeholder = "Select DD-MM-YYYY",
                        leadingIcon = Icons.Default.Event,
                        supportingText = "Today or future dates only. Tap box to select.",
                        onClick = {
                            val initialDate = if (nextFollowUpDate.isEmpty()) DateUtils.getTodayDateStringDDMMYYYY() else nextFollowUpDate
                            showDatePicker(initialDate, System.currentTimeMillis() - 1000, null) { nextFollowUpDate = it }
                        },
                        testTag = "form_followup_date_input"
                    )
                }
            }

            // Beautiful Emerald Green Premium Save Button
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
                    .height(58.dp)
                    .testTag("form_submit_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SAVE ENQUIRY RECORD",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }

    // Duplicate Mobile Detected AlertDialog
    if (showDuplicatePopup) {
        val matchedPatient = patients.find { it.mobile == mobile }
        val matchedEnquiry = enquiries.find { it.mobile == mobile }

        AlertDialog(
            onDismissRequest = {
                showDuplicatePopup = false
                confirmedDuplicateMobile = mobile
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Duplicate Mobile Detected")
                }
            },
            text = {
                Column {
                    Text(
                        text = "This mobile number ($mobile) is already registered in the system.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (matchedPatient != null) {
                        Text("• Registered Patient: ${matchedPatient.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("  Reg No: ${matchedPatient.regNo} | Branch: ${matchedPatient.branch}", style = MaterialTheme.typography.bodySmall)
                    } else if (matchedEnquiry != null) {
                        Text("• Active Enquiry: ${matchedEnquiry.patientName.ifEmpty { "N/A" }}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("  Branch: ${matchedEnquiry.branch} | Disease: ${matchedEnquiry.disease}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicatePopup = false
                        confirmedDuplicateMobile = mobile
                    }
                ) {
                    Text("ACKNOWLEDGE")
                }
            }
        )
    }
}

@Composable
fun ClickableDateBox(
    label: String,
    value: String,
    placeholder: String,
    leadingIcon: ImageVector,
    supportingText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, fontWeight = FontWeight.SemiBold) },
            placeholder = { Text(placeholder) },
            supportingText = { Text(supportingText) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.secondary) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.secondary,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onClick() }
        )
    }
}

@Composable
fun ClickableDropdownField(
    label: String,
    value: String,
    placeholder: String,
    leadingIcon: ImageVector,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    testTag: String
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, fontWeight = FontWeight.SemiBold) },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = if (value.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onExpandedChange(!expanded) }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (item == value) FontWeight.Bold else FontWeight.Normal,
                            color = if (item == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

