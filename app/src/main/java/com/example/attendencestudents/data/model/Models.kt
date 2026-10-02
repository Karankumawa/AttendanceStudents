package com.example.attendencestudents.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class AttendanceStatus {
    UNMARKED, // Neutral state before attendance is submitted
    PRESENT,  // Marked Present
    ABSENT,   // Marked Absent
    LEAVE     // Marked Leave
}

@Serializable
data class UserProfile(
    @SerialName("id") @SerializedName("id") val id: String = "",
    @SerialName("email") @SerializedName("email") val email: String = "",
    @SerialName("name") @SerializedName("name") val name: String = "",
    @SerialName("role") @SerializedName("role") val role: String = "student", // "admin" or "student"
    @SerialName("roll_number") @SerializedName("roll_number") val rollNumber: String? = null,
    @SerialName("student_id") @SerializedName("student_id") val studentId: String? = null,
    @SerialName("semester") @SerializedName("semester") val semester: Int? = null,
    @SerialName("department") @SerializedName("department") val department: String? = "CSE"
)

@Serializable
data class Student(
    @SerialName("id") @SerializedName("id") val id: String,
    @SerialName("name") @SerializedName("name") val name: String,
    @SerialName("roll_number") @SerializedName("roll_number") val rollNumber: String,
    @SerialName("semester") @SerializedName("semester") val semester: Int,
    @SerialName("department") @SerializedName("department") val department: String = "Computer Science & Engineering",
    @SerialName("email") @SerializedName("email") val email: String = ""
)

@Serializable
data class LoginUser(
    @SerialName("id") @SerializedName("id") val id: String? = null,
    @SerialName("email") @SerializedName("email") val email: String = "",
    @SerialName("password") @SerializedName("password") val password: String = "",
    @SerialName("name") @SerializedName("name") val name: String? = null,
    @SerialName("roll_number") @SerializedName("roll_number") val rollNumber: String? = null,
    @SerialName("student_id") @SerializedName("student_id") val studentId: String? = null,
    @SerialName("semester") @SerializedName("semester") val semester: Int? = null
)

@Serializable
data class AdminUserRecord(
    @SerialName("id") @SerializedName("id") val id: String? = null,
    @SerialName("email") @SerializedName("email") val email: String = "",
    @SerialName("password") @SerializedName("password") val password: String = "",
    @SerialName("name") @SerializedName("name") val name: String? = "Admin Instructor"
)

@Serializable
data class AttendanceRecord(
    @SerialName("id") @SerializedName("id") val id: String = "",
    @SerialName("student_id") @SerializedName("student_id") val studentId: String = "",
    @SerialName("student_name") @SerializedName("student_name") val studentName: String = "",
    @SerialName("semester") @SerializedName("semester") val semester: Int = 1,
    @SerialName("subject") @SerializedName("subject") val subject: String = "General",
    @SerialName("date") @SerializedName("date") val date: String = "",
    @SerialName("status") @SerializedName("status") val status: String = "PRESENT", // "PRESENT", "ABSENT", "LEAVE"
    @SerialName("timestamp") @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ActivityLog(
    @SerialName("id") @SerializedName("id") val id: String,
    @SerialName("title") @SerializedName("title") val title: String,
    @SerialName("description") @SerializedName("description") val description: String,
    @SerialName("date") @SerializedName("date") val date: String,
    @SerialName("semester") @SerializedName("semester") val semester: Int,
    @SerialName("subject") @SerializedName("subject") val subject: String,
    @SerialName("present_count") @SerializedName("present_count") val presentCount: Int,
    @SerialName("absent_count") @SerializedName("absent_count") val absentCount: Int,
    @SerialName("total_count") @SerializedName("total_count") val totalCount: Int,
    @SerialName("timestamp") @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class AdminStudentAttendanceOverview(
    val student: Student,
    val totalClasses: Int,
    val presentCount: Int,
    val absentCount: Int,
    val leaveCount: Int,
    val percentage: Double,
    val recentRecords: List<AttendanceRecord>
)

data class SemesterSummary(
    val semester: Int,
    val totalClasses: Int,
    val totalPresent: Int,
    val totalAbsent: Int,
    val attendancePercentage: Double,
    val studentStats: List<StudentStat>
)

data class StudentStat(
    val studentId: String,
    val studentName: String,
    val rollNumber: String,
    val semester: Int,
    val totalClasses: Int,
    val presentCount: Int,
    val absentCount: Int,
    val percentage: Double
)

data class AttendanceSubmission(
    val semester: Int,
    val subject: String,
    val date: String,
    val studentStatuses: Map<String, Boolean> // studentId -> isPresent
)
