package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Enquiry
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.EnquiryViewModel
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpScreen(
    viewModel: EnquiryViewModel,
    modifier: Modifier = Modifier,
    onContinueRegistration: ((Enquiry) -> Unit)? = null
) {
    val context = LocalContext.current
    val enquiries by viewModel.enquiries.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedEnquiryForUpdate by remember { mutableStateOf<Enquiry?>(null) }
    var selectedFilter by remember { mutableStateOf("All Active") } // "All Active", "Pending Only", "Closed/Completed"

    // Filtered list
    val filteredEnquiries = remember(enquiries, searchQuery, selectedFilter) {
        enquiries.filter { enquiry ->
            // Search filter
            val matchesSearch = enquiry.mobile.contains(searchQuery, ignoreCase = true) ||
                    enquiry.patientName.contains(searchQuery, ignoreCase = true) ||
                    enquiry.disease.contains(searchQuery, ignoreCase = true) ||
                    enquiry.branch.contains(searchQuery, ignoreCase = true)

            // Status filter
            val matchesStatus = when (selectedFilter) {
                "Pending Only" -> enquiry.status == "Pending"
                "Closed/Completed" -> enquiry.status == "Rejected" || enquiry.status == "Visit Confirmed" || enquiry.status == "Not Interested"
                else -> enquiry.status != "Rejected" && enquiry.status != "Visit Confirmed" && enquiry.status != "Not Interested"
            }

            matchesSearch && matchesStatus
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
                imageVector = Icons.Default.PhoneCallback,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Enquiry Follow-up List",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or mobile number...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("followup_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Filters Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All Active", "Pending Only", "Closed/Completed").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilter = filter },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = filter,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Results List
            if (filteredEnquiries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No active follow-ups found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEnquiries) { enquiry ->
                        FollowUpEnquiryCard(
                            enquiry = enquiry,
                            onClick = { selectedEnquiryForUpdate = enquiry },
                            onContinueRegistration = onContinueRegistration
                        )
                    }
                }
            }
        }
    }

    // Detail Update Dialog
    selectedEnquiryForUpdate?.let { enquiry ->
        FollowUpUpdateDialog(
            enquiry = enquiry,
            onDismiss = { selectedEnquiryForUpdate = null },
            onUpdate = { newStatus, newRemarks, newNextFollowUpDate ->
                viewModel.recordFollowUpCall(
                    enquiryId = enquiry.id,
                    newStatus = newStatus,
                    newRemarks = newRemarks,
                    newNextFollowUpDate = newNextFollowUpDate
                ) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (success) {
                        selectedEnquiryForUpdate = null
                    }
                }
            },
            onReject = { reason ->
                viewModel.rejectEnquiry(enquiryId = enquiry.id, reason = reason) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (success) {
                        selectedEnquiryForUpdate = null
                    }
                }
            }
        )
    }
}

@Composable
fun SignalIndicator(callCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.width(28.dp).height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val activeBars = callCount.coerceIn(0, 5)
        for (i in 1..5) {
            val isActive = i <= activeBars
            val barHeight = (3 * i).dp
            val color = if (isActive) {
                if (activeBars >= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun FollowUpEnquiryCard(
    enquiry: Enquiry,
    onClick: () -> Unit,
    onContinueRegistration: ((Enquiry) -> Unit)? = null
) {
    val context = LocalContext.current
    val isDueTodayOrPast = DateUtils.isTodayOrPastDDMMYYYY(enquiry.nextFollowUpDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("followup_item_${enquiry.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal Indicator on left side of card
                SignalIndicator(
                    callCount = enquiry.callCount,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = enquiry.patientName.ifEmpty { "Patient: (Optional)" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isDueTodayOrPast && enquiry.status != "Visit Confirmed" && enquiry.status != "Rejected") {
                            Surface(
                                color = WarningAmber.copy(alpha = 0.15f),
                                contentColor = WarningAmber,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "TODAY DUE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Disease: ${enquiry.disease} • Branch: ${enquiry.branch}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Phone: ${enquiry.mobile}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "First Enquiry Date: ${DateUtils.formatDisplayDate(enquiry.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (enquiry.remarks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Last Remark: ${enquiry.remarks}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(8.dp),
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons ordered: Call, WhatsApp, View All, Next Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${enquiry.mobile}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open dialer", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp, maxLines = 1)
                }

                // WhatsApp
                OutlinedButton(
                    onClick = {
                        try {
                            val cleanNumber = enquiry.mobile.filter { it.isDigit() }
                            val target = if (cleanNumber.startsWith("91") && cleanNumber.length == 12) cleanNumber else "91$cleanNumber"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$target&text=Hello%20${enquiry.patientName},%20this%20is%20from%20the%20clinic.")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp, maxLines = 1)
                }

                // View All
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("View All", fontSize = 11.sp, maxLines = 1)
                }

                // Next Call
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Next Call", fontSize = 11.sp, maxLines = 1)
                }
            }

            if (onContinueRegistration != null && enquiry.status != "Visit Confirmed" && enquiry.status != "Rejected") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onContinueRegistration(enquiry) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue Registration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpUpdateDialog(
    enquiry: Enquiry,
    onDismiss: () -> Unit,
    onUpdate: (String, String, String) -> Unit,
    onReject: (String) -> Unit
) {
    val context = LocalContext.current
    var updatedRemarks by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Connected") }
    var nextFollowUpDate by remember { mutableStateOf(DateUtils.getTodayDateStringDDMMYYYY()) }
    var isRejectMode by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }
    var statusExpanded by remember { mutableStateOf(false) }

    val callStatusList = listOf(
        "Connected", "Call Later", "Not Reachable", "Switched Off",
        "WhatsApp Sent", "Visit Confirmed", "Not Interested"
    )

    val todayStr = DateUtils.getTodayDateStringDDMMYYYY()
    val isAlreadyCalledToday = enquiry.lastCallDate == todayStr

    val calendar = Calendar.getInstance()
    val showDatePicker = { currentDateStr: String, onDateSelected: (String) -> Unit ->
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
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRejectMode) "Reject Enquiry" else "Update Patient Follow-up",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Patient brief details card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Patient: ${enquiry.patientName.ifEmpty { "(Optional)" }}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(text = "Mobile: ${enquiry.mobile}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Disease: ${enquiry.disease} | Branch: ${enquiry.branch}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Call Count: ${enquiry.callCount} calls made", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Rule Warning: Daily call limit info
                if (isAlreadyCalledToday) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Already called today. Multiple calls today are allowed, but the main Call Count will only increment once per calendar day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Rule Warning: 5 calls count warning
                if (enquiry.callCount >= 5) {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = WarningAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Warning: This enquiry has already reached 5 follow-up calls limit!",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = WarningAmber
                            )
                        }
                    }
                }

                if (!isRejectMode) {
                    // Update Follow up Form
                    // 1. Status dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Call Status *") },
                            leadingIcon = { Icon(Icons.Default.SettingsPhone, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { statusExpanded = !statusExpanded }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { statusExpanded = !statusExpanded }
                        )
                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            callStatusList.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = {
                                        selectedStatus = status
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Remarks
                    OutlinedTextField(
                        value = updatedRemarks,
                        onValueChange = { updatedRemarks = it },
                        label = { Text("Call Conversation Remarks *") },
                        placeholder = { Text("e.g. Patient requested callback on Sunday") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    // 3. Next Follow-up Date (Entire box clickable)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker(nextFollowUpDate) { nextFollowUpDate = it } }
                    ) {
                        OutlinedTextField(
                            value = nextFollowUpDate,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Next Follow-up Date *", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("DD-MM-YYYY") },
                            supportingText = { Text("Mandatory, today or future only. Tap to select.") },
                            leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = MaterialTheme.colorScheme.secondary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.secondary,
                                disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }

                    // Previous History Expandable/Section
                    if (enquiry.historyText.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Previous Call Records History:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = enquiry.historyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Reject option button
                    OutlinedButton(
                        onClick = { isRejectMode = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REJECT THIS ENQUIRY")
                    }

                } else {
                    // Reject Form
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Rejection Reason *") },
                        placeholder = { Text("e.g. Not interested, too far away") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Button(
                        onClick = { isRejectMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go Back to Update Form")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isRejectMode) {
                        if (rejectReason.trim().isEmpty()) {
                            // Empty rejection warning handles inside trigger
                        } else {
                            onReject(rejectReason.trim())
                        }
                    } else {
                        if (updatedRemarks.trim().isEmpty()) {
                            // Warn empty
                        } else {
                            onUpdate(selectedStatus, updatedRemarks.trim(), nextFollowUpDate)
                        }
                    }
                },
                enabled = if (isRejectMode) rejectReason.trim().isNotEmpty() else (updatedRemarks.trim().isNotEmpty())
            ) {
                Text(text = if (isRejectMode) "CONFIRM REJECTION" else "SAVE RECORD")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "CANCEL")
            }
        }
    )
}
