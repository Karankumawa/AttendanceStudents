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

        // 2. Query Supabase loginuser Table for Student Login
        val loginUsersResult = supabaseClient.fetchLoginUsers()
        if (loginUsersResult.isSuccess) {
            val loginUsers = loginUsersResult.getOrDefault(emptyList())
            val matchedUser = loginUsers.firstOrNull {
                it.email.trim().equals(trimmedEmail, ignoreCase = true) &&
                        it.password.trim() == trimmedPassword
            }

            if (matchedUser != null) {
                val matchingStudent = registeredStudents.firstOrNull {
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

        // 3. Fallback Student Login Check against registered students table or default credentials
        val matchingRegisteredStudent = registeredStudents.firstOrNull {
            it.email.trim().equals(trimmedEmail, ignoreCase = true) ||
                    it.name.lowercase().replace(" ", "") == trimmedEmail.substringBefore("@")
        }

        val isDefaultStudentCreds = (trimmedEmail == "student@student.com" && trimmedPassword == "student@student.com") ||
                (matchingRegisteredStudent != null && (trimmedPassword == matchingRegisteredStudent.rollNumber || trimmedPassword == "student@student.com"))

        if (isDefaultStudentCreds || matchingRegisteredStudent != null) {
            val studentObj = matchingRegisteredStudent ?: registeredStudents.firstOrNull()
            val studentUser = User(
                email = if (trimmedEmail.isNotBlank()) trimmedEmail else (studentObj?.email ?: "student@student.com"),
                name = studentObj?.name ?: "Student User",
                role = UserRole.STUDENT,
                studentId = studentObj?.id ?: "STU1",
                rollNumber = studentObj?.rollNumber ?: "101",
                semester = studentObj?.semester ?: 1
            )
            _currentUser.value = studentUser
            _isLoggedIn.value = true
            return Result.success(studentUser)
        }

        // 4. Invalid Student Notification
        return Result.failure(Exception("Invalid student, please contact admin."))
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}
