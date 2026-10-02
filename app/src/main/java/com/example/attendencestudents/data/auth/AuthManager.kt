package com.example.attendencestudents.data.auth

import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.data.remote.SupabaseClient
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
    val role: UserRole,
    val studentId: String? = null,
    val rollNumber: String? = null,
    val semester: Int? = null
)

object AuthManager {

    private val _isLoggedIn = MutableStateFlow<Boolean>(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun login(
        emailInput: String,
        passwordInput: String,
        supabaseClient: SupabaseClient = SupabaseClient(),
        registeredStudents: List<Student> = emptyList()
    ): Result<User> {
        val trimmedEmail = emailInput.trim().lowercase()
        val trimmedPassword = passwordInput.trim()

        // 1. Admin Login Check
        val isAdmin = (trimmedEmail == "admin@admin.com" && trimmedPassword == "admin@admin.com") ||
                (trimmedEmail == "admin@amin.com" && trimmedPassword == "admin@amin.com")

        if (isAdmin) {
            val adminUser = User(
                email = if (trimmedEmail.isNotBlank()) trimmedEmail else "admin@admin.com",
                name = "Admin Instructor",
                role = UserRole.ADMIN
            )
            _currentUser.value = adminUser
            _isLoggedIn.value = true
            return Result.success(adminUser)
        }

        // Fetch fresh students from Supabase if list is currently empty
        val studentsList = if (registeredStudents.isEmpty()) {
            val fetchRes = supabaseClient.fetchStudents()
            fetchRes.getOrDefault(emptyList())
        } else {
            registeredStudents
        }

        // 2. Query Supabase loginuser Table (if present)
        val loginUsersResult = supabaseClient.fetchLoginUsers()
        if (loginUsersResult.isSuccess) {
            val loginUsers = loginUsersResult.getOrDefault(emptyList())
            val matchedUser = loginUsers.firstOrNull {
                it.email.trim().equals(trimmedEmail, ignoreCase = true) &&
                        it.password.trim() == trimmedPassword
            }

            if (matchedUser != null) {
                val matchingStudent = studentsList.firstOrNull {
                    it.id == matchedUser.studentId || it.email.trim().equals(matchedUser.email.trim(), ignoreCase = true)
                }

                val studentUser = User(
                    email = matchedUser.email.ifBlank { trimmedEmail },
                    name = matchedUser.name ?: matchingStudent?.name ?: "Student User",
                    role = UserRole.STUDENT,
                    studentId = matchedUser.studentId ?: matchingStudent?.id ?: "STU${matchedUser.id ?: "1"}",
                    rollNumber = matchedUser.rollNumber ?: matchingStudent?.rollNumber,
                    semester = matchedUser.semester ?: matchingStudent?.semester ?: 1
                )
                _currentUser.value = studentUser
                _isLoggedIn.value = true
                return Result.success(studentUser)
            }
        }

        // 3. Fallback Check: Authenticate directly against Supabase Students Table
        val matchedStudent = studentsList.firstOrNull { student ->
            val emailMatch = student.email.trim().equals(trimmedEmail, ignoreCase = true)
            val rollMatch = student.rollNumber.trim().equals(trimmedEmail, ignoreCase = true) ||
                    student.rollNumber.trim().equals(trimmedPassword, ignoreCase = true)
            val nameMatch = student.name.lowercase().replace(" ", "").contains(trimmedEmail.substringBefore("@"))

            emailMatch || rollMatch || nameMatch
        }

        val isDefaultDemoCreds = trimmedEmail == "student@student.com" && trimmedPassword == "student@student.com"

        if (matchedStudent != null || isDefaultDemoCreds) {
            val targetStudent = matchedStudent ?: studentsList.firstOrNull()
            val studentUser = User(
                email = targetStudent?.email?.ifBlank { trimmedEmail } ?: trimmedEmail,
                name = targetStudent?.name ?: "Student User",
                role = UserRole.STUDENT,
                studentId = targetStudent?.id ?: "STU1",
                rollNumber = targetStudent?.rollNumber ?: "101",
                semester = targetStudent?.semester ?: 1
            )
            _currentUser.value = studentUser
            _isLoggedIn.value = true
            return Result.success(studentUser)
        }

        // 4. Invalid Student Credentials Error
        return Result.failure(Exception("Invalid student, please contact admin."))
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}
