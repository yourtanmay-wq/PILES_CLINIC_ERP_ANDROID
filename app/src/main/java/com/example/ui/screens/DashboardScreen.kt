package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Enquiry
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.EnquiryViewModel
import com.example.ui.viewmodel.ClinicViewModel
import com.example.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    enquiryViewModel: EnquiryViewModel,
    loginViewModel: LoginViewModel,
    clinicViewModel: ClinicViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    val enquiries by enquiryViewModel.enquiries.collectAsState()
    val sessionRole by loginViewModel.sessionRole.collectAsState()
    val userPhone by loginViewModel.userPhone.collectAsState()

    // Calculate dynamic counts
    val allCount = enquiries.size
    val pendingCount = enquiries.count { it.status == "Pending" }
    val followUpCount = enquiries.count {
        it.status != "Pending" && it.status != "Rejected" && it.status != "Visit Confirmed"
    }

    if (activeSubScreen != null) {
        when (activeSubScreen) {
            "branches" -> BranchManagementScreen(viewModel = clinicViewModel, onBack = { activeSubScreen = null })
            "patient_registration" -> PatientRegistrationScreen(viewModel = clinicViewModel, currentUserRole = sessionRole ?: "STAFF", onBack = { activeSubScreen = null })
            "doctor_visits" -> DoctorVisitScreen(viewModel = clinicViewModel, doctorName = sessionRole ?: "Doctor", onBack = { activeSubScreen = null })
            "payments" -> PaymentScreen(viewModel = clinicViewModel, receivedBy = sessionRole ?: "STAFF", onBack = { activeSubScreen = null })
            "prescriptions" -> PrescriptionBuilderScreen(viewModel = clinicViewModel, doctorName = sessionRole ?: "Doctor", onBack = { activeSubScreen = null })
            "patient_search" -> PatientSearchScreen(viewModel = clinicViewModel, onBack = { activeSubScreen = null })
            "reports" -> ReportsScreen(viewModel = clinicViewModel, onBack = { activeSubScreen = null })
            "cloud_sync" -> CloudSyncScreen(viewModel = clinicViewModel, onBack = { activeSubScreen = null })
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("dashboard_bottom_nav")
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard Home") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_tab_home")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.AddBox, contentDescription = "Add Enquiry") },
                        label = { Text("Enquiry Form") },
                        modifier = Modifier.testTag("nav_tab_form")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.PhoneCallback, contentDescription = "Follow Up") },
                        label = { Text("Follow-up") },
                        modifier = Modifier.testTag("nav_tab_followup")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardOverviewTab(
                        allCount = allCount,
                        pendingCount = pendingCount,
                        followUpCount = followUpCount,
                        role = sessionRole ?: "STAFF",
                        phone = userPhone,
                        recentEnquiries = enquiries.take(10),
                        onLogout = {
                            loginViewModel.logout()
                            onLogout()
                        },
                        onNavigateToForm = { selectedTab = 1 },
                        onNavigateToFollowUp = { selectedTab = 2 },
                        onLaunchModule = { module ->
                            activeSubScreen = module
                        }
                    )
                    1 -> EnquiryFormScreen(
                        viewModel = enquiryViewModel,
                        currentRole = sessionRole ?: "STAFF",
                        onSuccess = {
                            selectedTab = 0
                        }
                    )
                    2 -> FollowUpScreen(
                        viewModel = enquiryViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardOverviewTab(
    allCount: Int,
    pendingCount: Int,
    followUpCount: Int,
    role: String,
    phone: String,
    recentEnquiries: List<Enquiry>,
    onLogout: () -> Unit,
    onNavigateToForm: () -> Unit,
    onNavigateToFollowUp: () -> Unit,
    onLaunchModule: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Clinical App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "PILES CLINIC ERP",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Role: $role (${phone.ifEmpty { "8001080080" }})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Clinic Operations Launcher Grid
            Text(
                text = "Clinical ERP Operations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Dynamic launch items based on active role
            val operations = remember(role) {
                mutableListOf<Pair<String, Pair<String, ImageVector>>>().apply {
                    if (role == "MASTER ADMIN" || role == "STAFF") {
                        add("branches" to ("Branch Directory" to Icons.Default.HomeWork))
                        add("patient_registration" to ("Patient Registration" to Icons.Default.PersonAdd))
                        add("payments" to ("Billing & Payments" to Icons.Default.Payments))
                    }
                    if (role == "MASTER ADMIN" || role == "DOCTOR") {
                        add("doctor_visits" to ("Doctor Clinical Log" to Icons.Default.Assignment))
                        add("prescriptions" to ("Prescriptions & Diet" to Icons.Default.ReceiptLong))
                    }
                    // All roles can search patients (EMR Access)
                    add("patient_search" to ("EMR Patient Search" to Icons.Default.FolderShared))
                    
                    if (role == "MASTER ADMIN" || role == "STAFF") {
                        add("reports" to ("Analytics Reports" to Icons.Default.Analytics))
                    }
                    if (role == "MASTER ADMIN") {
                        add("cloud_sync" to ("Cloud Backup Sync" to Icons.Default.CloudSync))
                    }
                }
            }

            // Grid items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                operations.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(96.dp)
                                    .clickable { onLaunchModule(item.first) }
                                    .testTag("launch_btn_${item.first}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = item.second.second,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.second.first,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Operational Metrics
            Text(
                text = "Operational Lead Metrics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "All Branch Enquiry",
                    value = allCount.toString(),
                    icon = Icons.Default.MedicalServices,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToForm,
                    testTag = "all_enquiries_card"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MetricCard(
                            title = "Pending Enquiry",
                            value = pendingCount.toString(),
                            icon = Icons.Default.HourglassEmpty,
                            color = WarningAmber,
                            onClick = onNavigateToFollowUp,
                            testTag = "pending_enquiries_card"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MetricCard(
                            title = "Enquiry Follow-up",
                            value = followUpCount.toString(),
                            icon = Icons.Default.PhoneCallback,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = onNavigateToFollowUp,
                            testTag = "followup_enquiries_card"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recent activity heading
            Text(
                text = "Recent Enquiries (Total: $allCount)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (recentEnquiries.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No records found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToForm,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Enquiry")
                        }
                    }
                }
            } else {
                recentEnquiries.forEach { enquiry ->
                    EnquiryItemRow(enquiry = enquiry)
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun EnquiryItemRow(enquiry: Enquiry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enquiry.patientName.isEmpty()) "Patient: (Optional)" else enquiry.patientName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${enquiry.disease} • Branch: ${enquiry.branch}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "Mobile: ${enquiry.mobile}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Status chip
            val statusColor = when (enquiry.status) {
                "Pending" -> WarningAmber
                "Rejected" -> MaterialTheme.colorScheme.error
                "Visit Confirmed" -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }

            Surface(
                color = statusColor.copy(alpha = 0.15f),
                contentColor = statusColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = enquiry.status,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
