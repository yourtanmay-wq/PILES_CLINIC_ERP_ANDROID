package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.repository.EnquiryRepository
import com.example.data.repository.ClinicRepository
import com.example.data.session.SessionManager
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EnquiryViewModel
import com.example.ui.viewmodel.EnquiryViewModelFactory
import com.example.ui.viewmodel.ClinicViewModel
import com.example.ui.viewmodel.ClinicViewModelFactory
import com.example.ui.viewmodel.LoginViewModel
import com.example.ui.viewmodel.LoginViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    
                    // Core Data & Session layer wiring
                    val database = AppDatabase.getDatabase(context)
                    val enquiryRepository = EnquiryRepository(database.enquiryDao())
                    val clinicRepository = ClinicRepository(database.clinicDao())
                    val sessionManager = SessionManager(context)

                    // Construct ViewModels reactively inside the lifecycle scope
                    val loginViewModel: LoginViewModel = viewModel(
                        factory = LoginViewModelFactory(sessionManager)
                    )
                    val enquiryViewModel: EnquiryViewModel = viewModel(
                        factory = EnquiryViewModelFactory(enquiryRepository)
                    )
                    val clinicViewModel: ClinicViewModel = viewModel(
                        factory = ClinicViewModelFactory(clinicRepository)
                    )
                    clinicViewModel.startNetworkCallback(context)

                    AppContentOrchestrator(
                        loginViewModel = loginViewModel,
                        enquiryViewModel = enquiryViewModel,
                        clinicViewModel = clinicViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppContentOrchestrator(
    loginViewModel: LoginViewModel,
    enquiryViewModel: EnquiryViewModel,
    clinicViewModel: ClinicViewModel
) {
    val sessionRole by loginViewModel.sessionRole.collectAsState()

    if (sessionRole == null) {
        LoginScreen(
            viewModel = loginViewModel,
            onLoginSuccess = {
                // sessionRole triggers recomposition when changed internally in loginViewModel
            }
        )
    } else {
        DashboardScreen(
            enquiryViewModel = enquiryViewModel,
            loginViewModel = loginViewModel,
            clinicViewModel = clinicViewModel,
            onLogout = {
                // sessionRole triggers login state view on clear session
            }
        )
    }
}
