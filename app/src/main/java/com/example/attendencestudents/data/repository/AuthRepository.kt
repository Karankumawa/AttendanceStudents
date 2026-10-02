package com.example.attendencestudents.data.repository

import com.example.attendencestudents.data.auth.AuthManager
import com.example.attendencestudents.data.auth.User
import com.example.attendencestudents.data.auth.UserRole
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.data.model.UserProfile
import com.example.attendencestudents.data.remote.SupabaseClient
import com.example.attendencestudents.data.remote.SupabaseClientProvider
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val supabaseClient: SupabaseClient = SupabaseClientProvider.client
) {
    val isLoggedIn: StateFlow<Boolean> = AuthManager.isLoggedIn
    val currentUser: StateFlow<User?> = AuthManager.currentUser

    suspend fun signIn(emailInput: String, passwordInput: String, registeredStudents: List<Student> = emptyList()): Result<User> {
        return AuthManager.login(emailInput, passwordInput, supabaseClient, registeredStudents)
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val user = AuthManager.currentUser.value
            if (user != null) {
                Result.success(
                    UserProfile(
                        id = user.studentId ?: userId,
                        email = user.email,
                        name = user.name,
                        role = if (user.role == UserRole.ADMIN) "admin" else "student",
                        rollNumber = user.rollNumber,
                        studentId = user.studentId,
                        semester = user.semester
                    )
                )
            } else {
                Result.failure(Exception("No current user session"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        AuthManager.logout()
    }
}
