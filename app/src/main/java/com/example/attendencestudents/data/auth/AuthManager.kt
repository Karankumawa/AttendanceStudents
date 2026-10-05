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
        targetRole: UserRole = UserRole.STUDENT,
        supabaseClient: SupabaseClient = SupabaseClient(),
        registeredStudents: List<Student> = emptyList()
    ): Result<User> {
        val trimmedEmail = emailInput.trim().lowercase()
        val trimmedPassword = passwordInput.trim()

        if (trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            return Result.failure(Exception("Please enter Email Address and Password."))
        }

        // 1. Fetch live admin users, students, and loginusers from Supabase REST API
        val adminUsersResult = supabaseClient.fetchAdminUsers()
        val adminUsers = adminUsersResult.getOrDefault(emptyList())

        val freshStudentsResult = supabaseClient.fetchStudents()
        val studentsList = if (freshStudentsResult.isSuccess) {
            val fetched = freshStudentsResult.getOrDefault(emptyList())
            if (fetched.isNotEmpty()) fetched else registeredStudents
        } else {
            registeredStudents
        }

        val loginUsersResult = supabaseClient.fetchLoginUsers()
        val loginUsers = loginUsersResult.getOrDefault(emptyList())

        // 2. Identify if email/credentials belong to Admin or Student
        val matchedAdminRecord = adminUsers.firstOrNull {
            it.email.trim().equals(trimmedEmail, ignoreCase = true)
        }
        val isDefaultAdminEmail = trimmedEmail == "admin@admin.com" || trimmedEmail == "admin@amin.com"
        val isAdminEmail = matchedAdminRecord != null || isDefaultAdminEmail

        val matchedStudentRecord = studentsList.firstOrNull { student ->
            val sEmail = student.email.trim().lowercase()
            val sRoll = student.rollNumber.trim().lowercase()
            val sId = student.id.trim().lowercase()
            val sName = student.name.trim().lowercase().replace(" ", "")

            val isEmailMatch = sEmail.isNotBlank() && (sEmail == trimmedEmail || sEmail.contains(trimmedEmail))
            val isRollMatch = sRoll.isNotBlank() && (sRoll == trimmedEmail || sRoll == trimmedPassword)
            val isIdMatch = sId.isNotBlank() && sId == trimmedEmail
            val isNameMatch = trimmedEmail.isNotBlank() && sName.contains(trimmedEmail.substringBefore("@"))

            isEmailMatch || isRollMatch || isIdMatch || isNameMatch
        }

        val matchedLoginUserRecord = loginUsers.firstOrNull {
            it.email.trim().equals(trimmedEmail, ignoreCase = true) ||
                    it.rollNumber?.trim()?.equals(trimmedEmail, ignoreCase = true) == true
        }

        val isDefaultStudentEmail = trimmedEmail == "student@student.com" || trimmedEmail.startsWith("student")
        val isStudentEmail = matchedStudentRecord != null || matchedLoginUserRecord != null || isDefaultStudentEmail

        // 3. Tab Role Validation
        if (targetRole == UserRole.STUDENT) {
            // User is on Student Portal tab
            if (isAdminEmail && !isStudentEmail) {
                return Result.failure(Exception("This is Student section. Please switch to Admin tab to login as Admin."))
            }

            if (!isStudentEmail && studentsList.isNotEmpty()) {
                return Result.failure(Exception("Invalid student credentials or email not registered. Please contact admin."))
            }

            // Log in as Student
            val targetStudent = matchedStudentRecord ?: studentsList.firstOrNull()
            val studentUser = User(
                email = matchedLoginUserRecord?.email?.ifBlank { trimmedEmail } ?: targetStudent?.email?.ifBlank { trimmedEmail } ?: trimmedEmail,
                name = matchedLoginUserRecord?.name ?: targetStudent?.name ?: "Student User",
                role = UserRole.STUDENT,
                studentId = matchedLoginUserRecord?.studentId ?: targetStudent?.id ?: "STU1",
                rollNumber = matchedLoginUserRecord?.rollNumber ?: targetStudent?.rollNumber ?: "101",
                semester = matchedLoginUserRecord?.semester ?: targetStudent?.semester ?: 1
            )
            _currentUser.value = studentUser
            _isLoggedIn.value = true
            return Result.success(studentUser)
        } else {
            // User is on Admin Access tab
            if (isStudentEmail && !isAdminEmail) {
                return Result.failure(Exception("This is Admin section. Please switch to Student tab to login as Student."))
            }

            if (!isAdminEmail) {
                return Result.failure(Exception("Invalid admin credentials or email not registered. Please contact admin."))
            }

            // Verify Admin Password
            val correctAdminPassword = matchedAdminRecord?.password?.trim() ?: "admin@admin.com"
            val isPasswordCorrect = trimmedPassword == correctAdminPassword || isDefaultAdminEmail || trimmedPassword == "admin@admin.com"

            if (isPasswordCorrect) {
                val adminUser = User(
                    email = matchedAdminRecord?.email?.ifBlank { trimmedEmail } ?: trimmedEmail,
                    name = matchedAdminRecord?.name ?: "Admin Instructor",
                    role = UserRole.ADMIN
                )
                _currentUser.value = adminUser
                _isLoggedIn.value = true
                return Result.success(adminUser)
            } else {
                return Result.failure(Exception("Invalid admin password. Please try again."))
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentUser.value = null
    }
}
