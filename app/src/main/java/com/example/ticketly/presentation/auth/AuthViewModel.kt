package com.example.ticketly.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ticketly.model.User
import com.example.ticketly.repository.AuthRepository
import com.example.ticketly.ui.components.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the profile icon + login/account sheet wherever it's shown outside a
 * screen that already owns auth state itself (e.g. the Detail screen's app bar).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val currentUser: StateFlow<User?> = authRepository.currentUser

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            authRepository.login(username, password)
                .onSuccess { _loginState.value = LoginUiState.Idle }
                .onFailure { _loginState.value = LoginUiState.Error(it.message ?: "Login failed") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _loginState.value = LoginUiState.Idle
        }
    }
}
