package com.example.attendencestudents.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendencestudents.data.auth.User
import com.example.attendencestudents.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val user: User) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter Email Address and Password.")
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass)
            if (result.isSuccess) {
                _uiState.value = LoginUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = LoginUiState.Error(
                    result.exceptionOrNull()?.message ?: "Invalid email or password"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = LoginUiState.Idle
    }
}
