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

        // 1. Query Supabase admin_users Table for Admin Login
        val adminUsersResult = supabaseClient.fetchAdminUsers()
        if (adminUsersResult.isSuccess) {
            val adminUsers = adminUsersResult.getOrDefault(emptyList())
            val matchedAdmin = adminUsers.firstOrNull {
                it.email.trim().equals(trimmedEmail, ignoreCase = true) &&
                        it.password.trim() == trimmedPassword
            }

            if (matchedAdmin != null) {
                val adminUser = User(
                    email = matchedAdmin.email.ifBlank { trimmedEmail },
                    name = matchedAdmin.name ?: "Admin Instructor",
                    role = UserRole.ADMIN
                )
                _currentUser.value = adminUser
                _isLoggedIn.value = true
                return Result.success(adminUser)
            }
        }

        // Fallback Admin Check (for default/initial admin setup)
        val isDefaultAdmin = (trimmedEmail == "admin@admin.com" && trimmedPassword == "admin@admin.com") ||
                (trimmedEmail == "admin@amin.com" && trimmedPassword == "admin@amin.com")

        if (isDefaultAdmin) {
            val adminUser = User(
                email = if (trimmedEmail.isNotBlank()) trimmedEmail else "admin@admin.com",
                name = "Admin Instructor",
                role = UserRole.ADMIN
            )
            _currentUser.value = adminUser
            _isLoggedIn.value = true
            return Result.success(adminUser)
        }

        // Always fetch fresh live students list from Supabase REST API on login
        val freshStudentsResult = supabaseClient.fetchStudents()
        val studentsList = if (freshStudentsResult.isSuccess) {
            val fetched = freshStudentsResult.getOrDefault(emptyList())
            if (fetched.isNotEmpty()) fetched else registeredStudents
        } else {
            registeredStudents
        }

        // 2. Query Supabase loginuser Table (if present in Supabase)
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

        // 3. Direct Authentication against Supabase Students Table
        val matchedStudent = studentsList.firstOrNull { student ->
            val cleanEmailInput = trimmedEmail.lowercase()
            val cleanStudentEmail = student.email.trim().lowercase()
            val cleanRollNumber = student.rollNumber.trim().lowercase()
            val cleanName = student.name.trim().lowercase().replace(" ", "")

            val isEmailMatch = cleanStudentEmail.isNotBlank() && cleanStudentEmail == cleanEmailInput
            val isRollMatch = cleanRollNumber == cleanEmailInput || cleanRollNumber == trimmedPassword
            val isNameMatch = cleanEmailInput.isNotBlank() && cleanName.contains(cleanEmailInput.substringBefore("@"))

            isEmailMatch || isRollMatch || isNameMatch
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
        return Result.failure(Exception("Invalid student credentials. Please contact admin."))
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}
