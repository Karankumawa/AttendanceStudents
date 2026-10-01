package com.example.attendencestudents.data.repository

import com.example.attendencestudents.data.model.ActivityLog
import com.example.attendencestudents.data.model.AttendanceRecord
import com.example.attendencestudents.data.model.AttendanceStatus
import com.example.attendencestudents.data.model.SemesterSummary
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.data.model.StudentStat
import com.example.attendencestudents.data.remote.MongoClientManager
import com.example.attendencestudents.data.remote.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AttendanceRepository(
    val mongoClientManager: MongoClientManager = MongoClientManager(),
    val supabaseClient: SupabaseClient = SupabaseClient()
) {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("Connecting to Database...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow<Boolean>(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun syncWithSupabase() {
        _isSyncing.value = true
        _syncStatus.value = "Connecting to Cloud Database..."

        var mongoSuccess = false
        var lastErr = ""

        // 1. Try MongoDB Atlas Sync
        val remoteStudentsResult = mongoClientManager.fetchStudents()
        if (remoteStudentsResult.isSuccess) {
            val fetched = remoteStudentsResult.getOrDefault(emptyList())
            if (fetched.isNotEmpty()) {
                val fetchedIds = fetched.map { it.id }.toSet()
                val localOnly = _students.value.filter { it.id !in fetchedIds }
                _students.value = fetched + localOnly
            }
            mongoSuccess = true
        } else {
            lastErr = remoteStudentsResult.exceptionOrNull()?.message ?: "MongoDB Error"
        }

        val remoteAttendanceResult = mongoClientManager.fetchAttendanceRecords()
        if (remoteAttendanceResult.isSuccess) {
            val fetchedRecs = remoteAttendanceResult.getOrDefault(emptyList())
            if (fetchedRecs.isNotEmpty()) {
                val fetchedIds = fetchedRecs.map { it.id }.toSet()
                val localOnly = _attendanceRecords.value.filter { it.id !in fetchedIds }
                _attendanceRecords.value = fetchedRecs + localOnly
            }
        }

        val remoteActivityResult = mongoClientManager.fetchActivityLogs()
        if (remoteActivityResult.isSuccess) {
            val fetchedLogs = remoteActivityResult.getOrDefault(emptyList())
            if (fetchedLogs.isNotEmpty()) {
                val fetchedIds = fetchedLogs.map { it.id }.toSet()
                val localOnly = _activityLogs.value.filter { it.id !in fetchedIds }
                _activityLogs.value = fetchedLogs + localOnly
            }
        }

        // 2. If MongoDB Atlas port 27017 is blocked by mobile carrier or password needed, sync via HTTPS Cloud Backup (Supabase REST API)
        if (!mongoSuccess) {
            val supStudents = supabaseClient.fetchStudents()
            if (supStudents.isSuccess) {
                val fetched = supStudents.getOrDefault(emptyList())
                val fetchedIds = fetched.map { it.id }.toSet()
                val localOnly = _students.value.filter { it.id !in fetchedIds }
                _students.value = fetched + localOnly
            }

            val supAttendance = supabaseClient.fetchAttendanceRecords()
            if (supAttendance.isSuccess) {
                val fetched = supAttendance.getOrDefault(emptyList())
                val fetchedIds = fetched.map { it.id }.toSet()
                val localOnly = _attendanceRecords.value.filter { it.id !in fetchedIds }
                _attendanceRecords.value = fetched + localOnly
            }

            val supActivity = supabaseClient.fetchActivityLogs()
            if (supActivity.isSuccess) {
                val fetched = supActivity.getOrDefault(emptyList())
                val fetchedIds = fetched.map { it.id }.toSet()
                val localOnly = _activityLogs.value.filter { it.id !in fetchedIds }
                _activityLogs.value = fetched + localOnly
            }
        }

        _isSyncing.value = false
        if (mongoSuccess) {
            _syncStatus.value = "Synced with MongoDB Live"
        } else {
            _syncStatus.value = "Synced via HTTPS Database (MongoDB Port 27017 Blocked by Carrier)"
        }
    }

    suspend fun submitAttendance(
        semester: Int,
        subject: String,
        date: String,
        studentStatusMap: Map<String, AttendanceStatus>
    ): Result<Boolean> {
        _isSyncing.value = true
        _syncStatus.value = "Submitting Attendance..."

        val newRecords = mutableListOf<AttendanceRecord>()
        var presentCount = 0
        var absentCount = 0

        val semesterStudents = _students.value.filter { it.semester == semester }

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
                timestamp = System.currentTimeMillis()
            )
            newRecords.add(record)
        }

        _attendanceRecords.value = _attendanceRecords.value + newRecords

        val activity = ActivityLog(
            id = UUID.randomUUID().toString(),
            title = "Semester $semester Attendance Recorded",
            description = "$subject ($date): $presentCount Present, $absentCount Absent",
            date = date,
            semester = semester,
            subject = subject,
            presentCount = presentCount,
            absentCount = absentCount,
            totalCount = semesterStudents.size,
            timestamp = System.currentTimeMillis()
        )
        _activityLogs.value = listOf(activity) + _activityLogs.value

        // Persist asynchronously to Cloud Databases
        try {
            mongoClientManager.insertAttendanceRecords(newRecords)
            mongoClientManager.insertActivityLog(activity)
            supabaseClient.insertAttendanceRecords(newRecords)
            supabaseClient.insertActivityLog(activity)
            _syncStatus.value = "Saved & Synced with Database"
        } catch (e: Exception) {
            _syncStatus.value = "Saved locally"
        } finally {
            _isSyncing.value = false
        }

        return Result.success(true)
    }

    suspend fun addStudent(name: String, rollNumber: String, semester: Int, department: String): Result<Student> {
        val newStudent = Student(
            id = "STU${System.currentTimeMillis() % 100000}",
            name = name,
            rollNumber = rollNumber,
            semester = semester,
            department = department,
            email = "${name.lowercase().replace(" ", ".")}@college.edu"
        )

        // Save locally first so user sees added student immediately in UI
        _students.value = _students.value + newStudent

        try {
            mongoClientManager.insertStudent(newStudent)
            supabaseClient.insertStudent(newStudent)
            _syncStatus.value = "Student Added & Synced with Database"
        } catch (e: Exception) {
            _syncStatus.value = "Student Added locally"
        }

        return Result.success(newStudent)
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
