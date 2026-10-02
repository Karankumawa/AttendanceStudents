package com.example.attendencestudents.data.model

import com.google.gson.annotations.SerializedName

enum class AttendanceStatus {
    UNMARKED, // Neutral state before attendance is submitted
    PRESENT,  // Marked Present
    ABSENT    // Marked Absent
}

data class Student(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("roll_number") val rollNumber: String,
    @SerializedName("semester") val semester: Int,
    @SerializedName("department") val department: String = "Computer Science & Engineering",
    @SerializedName("email") val email: String = ""
)

data class LoginUser(
    @SerializedName("id") val id: String? = null,
    @SerializedName("email") val email: String = "",
    @SerializedName("password") val password: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("roll_number") val rollNumber: String? = null,
    @SerializedName("student_id") val studentId: String? = null,
    @SerializedName("semester") val semester: Int? = null
)

data class AdminUserRecord(
    @SerializedName("id") val id: String? = null,
    @SerializedName("email") val email: String = "",
    @SerializedName("password") val password: String = "",
    @SerializedName("name") val name: String? = "Admin Instructor"
)

data class AttendanceRecord(
    @SerializedName("id") val id: String,
    @SerializedName("student_id") val studentId: String,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("semester") val semester: Int,
    @SerializedName("subject") val subject: String,
    @SerializedName("date") val date: String,
    @SerializedName("status") val status: String, // "PRESENT" or "ABSENT"
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class ActivityLog(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("date") val date: String,
    @SerializedName("semester") val semester: Int,
    @SerializedName("subject") val subject: String,
    @SerializedName("present_count") val presentCount: Int,
    @SerializedName("absent_count") val absentCount: Int,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
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
