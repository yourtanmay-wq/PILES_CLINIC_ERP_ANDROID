package com.tkbiswas.pilesclinic.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String, val atMillis: Long) : SyncState()
    data class Failed(val message: String, val atMillis: Long) : SyncState()
}

/** App-wide singleton so any screen can observe "is a sync running right now". */
object SyncStatusHolder {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state

    fun update(newState: SyncState) {
        _state.value = newState
    }
}
