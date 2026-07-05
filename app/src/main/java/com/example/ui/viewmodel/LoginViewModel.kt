package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private val _userPhone = MutableStateFlow("")
    val userPhone = _userPhone.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    private val _sessionRole = MutableStateFlow<String?>(sessionManager.getSessionRole())
    val sessionRole = _sessionRole.asStateFlow()

    fun updatePhone(phone: String) {
        _userPhone.value = phone
        _loginError.value = null
    }

    fun updatePassword(pwd: String) {
        _password.value = pwd
        _loginError.value = null
    }

    fun login(): Boolean {
        val phoneVal = _userPhone.value.trim()
        val pwdVal = _password.value.trim()

        if (phoneVal.isEmpty()) {
            _loginError.value = "Mobile number is required"
            return false
        }

        if (pwdVal.isEmpty()) {
            _loginError.value = "Password is required"
            return false
        }

        // Determine role based on password rules
        val determinedRole = when (pwdVal) {
            "admin123" -> "MASTER ADMIN"
            "staff123" -> "STAFF"
            "doctor123" -> "DOCTOR"
            "field123" -> "FIELD OFFICER"
            else -> null
        }

        if (determinedRole == null) {
            _loginError.value = "Invalid credentials. Please verify your role password."
            return false
        }

        // Save session
        sessionManager.saveSession(determinedRole)
        _sessionRole.value = determinedRole
        _loginError.value = null
        return true
    }

    fun logout() {
        sessionManager.clearSession()
        _sessionRole.value = null
        _userPhone.value = ""
        _password.value = ""
    }
}

class LoginViewModelFactory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
