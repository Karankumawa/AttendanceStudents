package com.example.attendencestudents.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UserRole {
    ADMIN,
    STUDENT
}

data class User(
    val email: String,
    val name: String,
    val role: UserRole
)


object AuthManager {

    private val _isLoggedIn = MutableStateFlow<Boolean>(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun login(emailInput: String, passwordInput: String): Result<User> {
        val trimmedEmail = emailInput.trim().lowercase()
        val trimmedPassword = passwordInput.trim()

        val isAdmin = (trimmedEmail == "admin@admin.com" && trimmedPassword == "admin@admin.com") ||
                (trimmedEmail == "admin@amin.com" && trimmedPassword == "admin@amin.com")

        val isStudent = (trimmedEmail == "student@student.com" || trimmedPassword == "student@student.com" ||
                trimmedEmail.endsWith("@student.com") || trimmedEmail.startsWith("student"))

        return when {
            isAdmin -> {
                val user = User(
                    email = if (trimmedEmail.isNotBlank()) trimmedEmail else "admin@admin.com",
                    name = "Admin Instructor",
                    role = UserRole.ADMIN
                )
                _currentUser.value = user
                _isLoggedIn.value = true
                Result.success(user)
            }
            isStudent -> {
                val user = User(
                    email = if (trimmedEmail.isNotBlank()) trimmedEmail else "student@student.com",
                    name = "Student Portal User",
                    role = UserRole.STUDENT
                )
                _currentUser.value = user
                _isLoggedIn.value = true
                Result.success(user)
            }
            else -> {
                Result.failure(Exception("Invalid credentials. Enter admin@admin.com (Admin) or student@student.com (Student)."))
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}
