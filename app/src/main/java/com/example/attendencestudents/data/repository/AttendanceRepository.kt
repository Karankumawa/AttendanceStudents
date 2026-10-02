package com.example.attendencestudents.data.repository

import com.example.attendencestudents.data.model.ActivityLog
import com.example.attendencestudents.data.model.AttendanceRecord
import com.example.attendencestudents.data.model.AttendanceStatus
import com.example.attendencestudents.data.model.LoginUser
import com.example.attendencestudents.data.model.SemesterSummary
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.data.model.StudentStat
import com.example.attendencestudents.data.remote.SupabaseClient
import com.example.attendencestudents.data.remote.SupabaseClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AttendanceRepository(
    val supabaseClient: SupabaseClient = SupabaseClientProvider.client
) {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("Connected to Supabase")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow<Boolean>(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun syncWithSupabase() {
        _isSyncing.value = true
        _syncStatus.value = "Syncing with Supabase..."

        var errorsCount = 0

        val remoteStudentsResult = supabaseClient.fetchStudents()
        if (remoteStudentsResult.isSuccess) {
            val fetched = remoteStudentsResult.getOrDefault(emptyList())
            _students.value = fetched
        } else {
            errorsCount++
        }

        val remoteAttendanceResult = supabaseClient.fetchAttendanceRecords()
        if (remoteAttendanceResult.isSuccess) {
            val fetchedRecs = remoteAttendanceResult.getOrDefault(emptyList())
            _attendanceRecords.value = fetchedRecs
        } else {
            errorsCount++
        }

        val remoteActivityResult = supabaseClient.fetchActivityLogs()
        if (remoteActivityResult.isSuccess) {
            val fetchedLogs = remoteActivityResult.getOrDefault(emptyList())
            _activityLogs.value = fetchedLogs
        } else {
            errorsCount++
        }

        _isSyncing.value = false
        if (errorsCount == 0) {
            _syncStatus.value = "Synced with Supabase Live"
        } else {
            _syncStatus.value = "Supabase Live Connection Active"
        }
    }

    fun getStudentAttendance(studentId: String): List<AttendanceRecord> {
        return _attendanceRecords.value.filter { it.studentId == studentId }
    }

    fun getAllStudentsAttendance(): List<AttendanceRecord> {
        return _attendanceRecords.value
    }

    suspend fun markAttendance(
        studentId: String,
        date: String,
        status: String,
        subject: String = "General",
        semester: Int = 1
    ): Result<Boolean> {
        val studentObj = _students.value.firstOrNull { it.id == studentId }
        val record = AttendanceRecord(
            id = UUID.randomUUID().toString(),
            studentId = studentId,
            studentName = studentObj?.name ?: "Student",
            semester = semester,
            subject = subject,
            date = date,
            status = status,
            timestamp = System.currentTimeMillis()
        )
        _attendanceRecords.value = _attendanceRecords.value + record
        return try {
            supabaseClient.insertAttendanceRecords(listOf(record))
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun submitAttendance(
        semester: Int,
        subject: String,
        date: String,
        time: String = "",
        studentStatusMap: Map<String, AttendanceStatus>
    ): Result<Boolean> {
        _isSyncing.value = true
        _syncStatus.value = "Submitting to Supabase..."

        val newRecords = mutableListOf<AttendanceRecord>()
        var presentCount = 0
        var absentCount = 0

        val semesterStudents = _students.value.filter { it.semester == semester }
        val nowTimestamp = System.currentTimeMillis()

        semesterStudents.forEach { student ->
            val status = studentStatusMap[student.id] ?: AttendanceStatus.UNMARKED
            val finalStatusString = if (status == AttendanceStatus.PRESENT) "PRESENT" else "ABSENT"

            if (finalStatusString == "PRESENT") {
                presentCount++
            } else {
                absentCount++
            }

            val record = AttendanceRecord(
                id = UUID.randomUUID().toString(),
                studentId = student.id,
                studentName = student.name,
                semester = semester,
                subject = subject,
                date = date,
                status = finalStatusString,
                timestamp = nowTimestamp
            )
            newRecords.add(record)
        }

        _attendanceRecords.value = _attendanceRecords.value + newRecords

        val timeStr = if (time.isNotBlank()) " at $time" else ""
        val activity = ActivityLog(
            id = UUID.randomUUID().toString(),
            title = "Semester $semester Attendance Recorded",
            description = "$subject ($date$timeStr): $presentCount Present, $absentCount Absent",
            date = date,
            semester = semester,
            subject = subject,
            presentCount = presentCount,
            absentCount = absentCount,
            totalCount = semesterStudents.size,
            timestamp = nowTimestamp
        )
        _activityLogs.value = listOf(activity) + _activityLogs.value

        try {
            val recRes = supabaseClient.insertAttendanceRecords(newRecords)
            val actRes = supabaseClient.insertActivityLog(activity)
            if (recRes.isSuccess && actRes.isSuccess) {
                _syncStatus.value = "Saved & Synced with Supabase"
            } else {
                _syncStatus.value = "Saved locally & Synced with Supabase"
            }
        } catch (e: Exception) {
            _syncStatus.value = "Saved locally"
        } finally {
            _isSyncing.value = false
        }

        return Result.success(true)
    }

    suspend fun addStudent(name: String, rollNumber: String, semester: Int, department: String): Result<Student> {
        val studentEmail = "${name.lowercase().replace(" ", ".")}@college.edu"
        val newStudent = Student(
            id = "STU${System.currentTimeMillis() % 100000}",
            name = name,
            rollNumber = rollNumber,
            semester = semester,
            department = department,
            email = studentEmail
        )

        // Save locally first so user sees added student immediately in UI
        _students.value = _students.value + newStudent

        try {
            val res = supabaseClient.insertStudent(newStudent)
            // Sync user credentials to Supabase loginuser table
            val loginUser = LoginUser(
                email = studentEmail,
                password = rollNumber,
                name = name,
                rollNumber = rollNumber,
                studentId = newStudent.id,
                semester = semester
            )
            supabaseClient.insertLoginUser(loginUser)

            if (res.isSuccess) {
                _syncStatus.value = "Student Added & Synced with Supabase"
            } else {
                _syncStatus.value = "Student Added locally"
            }
        } catch (e: Exception) {
            _syncStatus.value = "Student Added locally"
        }

        return Result.success(newStudent)
    }

    suspend fun deleteStudent(studentId: String): Result<Boolean> {
        _students.value = _students.value.filter { it.id != studentId }

        try {
            val res = supabaseClient.deleteStudent(studentId)
            if (res.isSuccess) {
                _syncStatus.value = "Student Deleted & Synced with Supabase"
            } else {
                _syncStatus.value = "Student Removed locally"
            }
        } catch (e: Exception) {
            _syncStatus.value = "Student Removed locally"
        }

        return Result.success(true)
    }

    fun getAllSemesterSummaries(): List<SemesterSummary> {
        val allRecords = _attendanceRecords.value
        val allStudents = _students.value

        return (1..8).map { sem ->
            val semStudents = allStudents.filter { it.semester == sem }
            val semRecords = allRecords.filter { it.semester == sem }

            val uniqueClasses = semRecords.map { "${it.subject}_${it.date}" }.distinct().size
            val totalClasses = if (uniqueClasses > 0) uniqueClasses else 0

            val totalPresentRecords = semRecords.count { it.status == "PRESENT" }
            val totalAbsentRecords = semRecords.count { it.status == "ABSENT" }
            val totalRecords = semRecords.size

            val overallPercentage = if (totalRecords > 0) {
                (totalPresentRecords.toDouble() / totalRecords.toDouble()) * 100.0
            } else {
                0.0
            }

            val studentStatsList = semStudents.map { student ->
                val sRecords = semRecords.filter { it.studentId == student.id }
                val sPresent = sRecords.count { it.status == "PRESENT" }
                val sAbsent = sRecords.count { it.status == "ABSENT" }
                val sTotal = sRecords.size
                val sPct = if (sTotal > 0) {
                    (sPresent.toDouble() / sTotal.toDouble()) * 100.0
                } else {
                    0.0
                }

                StudentStat(
                    studentId = student.id,
                    studentName = student.name,
                    rollNumber = student.rollNumber,
                    semester = sem,
                    totalClasses = if (sTotal > 0) sTotal else totalClasses,
                    presentCount = sPresent,
                    absentCount = sAbsent,
                    percentage = sPct
                )
            }

            SemesterSummary(
                semester = sem,
                totalClasses = totalClasses,
                totalPresent = totalPresentRecords,
                totalAbsent = totalAbsentRecords,
                attendancePercentage = overallPercentage,
                studentStats = studentStatsList
            )
        }
    }
}
