package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Enquiry
import com.example.data.repository.EnquiryRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EnquiryViewModel(private val repository: EnquiryRepository) : ViewModel() {

    val enquiries: StateFlow<List<Enquiry>> = repository.allEnquiries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _insertSuccess = MutableStateFlow<Boolean?>(null)
    val insertSuccess: StateFlow<Boolean?> = _insertSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun resetInsertStatus() {
        _insertSuccess.value = null
        _errorMessage.value = null
    }

    fun addEnquiry(
        date: String,
        mobile: String,
        patientName: String,
        branch: String,
        callReceivedBy: String,
        disease: String,
        shortAddress: String,
        callTiming: String,
        remarks: String,
        nextFollowUpDate: String
    ) {
        if (mobile.isBlank()) {
            _errorMessage.value = "Mobile number is mandatory"
            return
        }
        if (branch.isBlank()) {
            _errorMessage.value = "Branch is mandatory"
            return
        }
        if (remarks.isBlank()) {
            _errorMessage.value = "Remarks is mandatory"
            return
        }
        if (nextFollowUpDate.isBlank()) {
            _errorMessage.value = "Next follow-up date is mandatory"
            return
        }

        // Validate Date (today or past only)
        if (!DateUtils.isTodayOrPast(date)) {
            _errorMessage.value = "Enquiry date must be today or a past date"
            return
        }

        // Validate Next Follow-up Date (today or future only)
        if (!DateUtils.isTodayOrFuture(nextFollowUpDate)) {
            _errorMessage.value = "Next follow-up date must be today or a future date"
            return
        }

        // Sanitize Mobile Number (+91 handled once)
        val sanitizedMobile = formatMobileNumber(mobile)

        viewModelScope.launch {
            try {
                val newEnquiry = Enquiry(
                    date = date,
                    mobile = sanitizedMobile,
                    patientName = patientName.trim(),
                    branch = branch,
                    callReceivedBy = callReceivedBy,
                    disease = disease,
                    shortAddress = shortAddress.trim(),
                    callTiming = callTiming,
                    remarks = remarks.trim(),
                    nextFollowUpDate = nextFollowUpDate,
                    status = "Pending"
                )
                repository.insert(newEnquiry)
                _insertSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Database error: ${e.message}"
            }
        }
    }

    fun formatMobileNumber(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("+91")) {
            val rest = trimmed.substring(3).filter { it.isDigit() }
            return "+91$rest"
        }
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length == 10) {
            return "+91$digits"
        }
        // If it starts with 91 and has 12 digits
        if (digits.startsWith("91") && digits.length == 12) {
            return "+$digits"
        }
        return if (digits.isNotEmpty() && !digits.startsWith("+")) "+91$digits" else digits
    }

    // Call Follow-up logic
    fun recordFollowUpCall(
        enquiryId: Int,
        newStatus: String,
        newRemarks: String,
        newNextFollowUpDate: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val enquiry = repository.getEnquiryById(enquiryId)
                if (enquiry == null) {
                    onComplete(false, "Enquiry not found")
                    return@launch
                }

                val todayStr = DateUtils.getTodayDateString()

                // Rule: Max 1 call per day
                if (enquiry.lastCallDate == todayStr) {
                    onComplete(false, "Maximum 1 call per day is allowed for this enquiry.")
                    return@launch
                }

                // Rule: Max 5 calls warning is shown, let's keep track
                val newCallCount = enquiry.callCount + 1

                // Construct history log entry
                val logEntry = "Call #$newCallCount on $todayStr - Status: $newStatus\nRemarks: $newRemarks\n"
                val updatedHistory = enquiry.historyText + logEntry

                val updatedEnquiry = enquiry.copy(
                    status = newStatus,
                    remarks = newRemarks,
                    nextFollowUpDate = newNextFollowUpDate,
                    callCount = newCallCount,
                    lastCallDate = todayStr,
                    historyText = updatedHistory
                )

                repository.update(updatedEnquiry)

                val successMsg = if (newCallCount >= 5) {
                    "Updated successfully. Warning: This enquiry has reached $newCallCount follow-up calls!"
                } else {
                    "Follow-up call recorded successfully"
                }

                onComplete(true, successMsg)
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }

    // Reject enquiry shortcut
    fun rejectEnquiry(enquiryId: Int, reason: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val enquiry = repository.getEnquiryById(enquiryId)
                if (enquiry == null) {
                    onComplete(false, "Enquiry not found")
                    return@launch
                }

                val todayStr = DateUtils.getTodayDateString()
                val updatedHistory = enquiry.historyText + "Rejected on $todayStr. Reason: $reason\n"

                val updatedEnquiry = enquiry.copy(
                    status = "Rejected",
                    historyText = updatedHistory,
                    lastCallDate = todayStr
                )

                repository.update(updatedEnquiry)
                onComplete(true, "Enquiry has been rejected successfully")
            } catch (e: Exception) {
                onComplete(false, "Error: ${e.message}")
            }
        }
    }
}

class EnquiryViewModelFactory(private val repository: EnquiryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnquiryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EnquiryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
