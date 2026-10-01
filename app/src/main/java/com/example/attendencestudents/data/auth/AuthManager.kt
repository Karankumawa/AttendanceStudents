package com.example.attendencestudents.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminUser(
    val email: String,
    val name: String = "Admin Instructor",
    val role: String = "System Administrator"
)

object AuthManager {

    private val _isLoggedIn = MutableStateFlow<Boolean>(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<AdminUser?>(null)
    val currentUser: StateFlow<AdminUser?> = _currentUser.asStateFlow()

    fun login(emailInput: String, passwordInput: String): Result<AdminUser> {
        val trimmedEmail = emailInput.trim().lowercase()
        val trimmedPassword = passwordInput.trim()

        val isValidAdmin = (trimmedEmail == "admin@admin.com" && trimmedPassword == "admin@admin.com") ||
                (trimmedEmail == "admin@amin.com" && trimmedPassword == "admin@amin.com")

        return if (isValidAdmin) {
            val user = AdminUser(email = trimmedEmail)
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid credentials. Please enter valid Admin Email and Password."))
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}
