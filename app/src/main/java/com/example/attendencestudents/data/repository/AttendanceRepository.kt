package com.example.attendencestudents.data.repository

import com.example.attendencestudents.data.model.ActivityLog
import com.example.attendencestudents.data.model.AttendanceRecord
import com.example.attendencestudents.data.model.AttendanceStatus
import com.example.attendencestudents.data.model.SemesterSummary
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.data.model.StudentStat
import com.example.attendencestudents.data.remote.MongoClientManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AttendanceRepository(
    val mongoClientManager: MongoClientManager = MongoClientManager()
) {

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private val _syncStatus = MutableStateFlow<String>("Connecting to MongoDB...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow<Boolean>(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun syncWithSupabase() {
        _isSyncing.value = true
        _syncStatus.value = "Connecting to MongoDB Atlas..."

        var lastErr = ""

        val remoteStudentsResult = mongoClientManager.fetchStudents()
        if (remoteStudentsResult.isSuccess) {
            val fetched = remoteStudentsResult.getOrDefault(emptyList())
            if (fetched.isNotEmpty()) {
                val fetchedIds = fetched.map { it.id }.toSet()
                val localOnly = _students.value.filter { it.id !in fetchedIds }
                _students.value = fetched + localOnly
            }
        } else {
            lastErr = remoteStudentsResult.exceptionOrNull()?.message ?: "Error fetching students"
        }

        val remoteAttendanceResult = mongoClientManager.fetchAttendanceRecords()
        if (remoteAttendanceResult.isSuccess) {
            val fetchedRecs = remoteAttendanceResult.getOrDefault(emptyList())
            if (fetchedRecs.isNotEmpty()) {
                val fetchedIds = fetchedRecs.map { it.id }.toSet()
                val localOnly = _attendanceRecords.value.filter { it.id !in fetchedIds }
                _attendanceRecords.value = fetchedRecs + localOnly
            }
        } else if (lastErr.isEmpty()) {
            lastErr = remoteAttendanceResult.exceptionOrNull()?.message ?: "Error fetching attendance"
        }

        val remoteActivityResult = mongoClientManager.fetchActivityLogs()
        if (remoteActivityResult.isSuccess) {
            val fetchedLogs = remoteActivityResult.getOrDefault(emptyList())
            if (fetchedLogs.isNotEmpty()) {
                val fetchedIds = fetchedLogs.map { it.id }.toSet()
                val localOnly = _activityLogs.value.filter { it.id !in fetchedIds }
                _activityLogs.value = fetchedLogs + localOnly
            }
        } else if (lastErr.isEmpty()) {
            lastErr = remoteActivityResult.exceptionOrNull()?.message ?: "Error fetching activity"
        }

        _isSyncing.value = false
        if (lastErr.isEmpty()) {
            _syncStatus.value = "Synced with MongoDB Live"
        } else {
            _syncStatus.value = lastErr
        }
    }

    suspend fun submitAttendance(
        semester: Int,
        subject: String,
        date: String,
        studentStatusMap: Map<String, AttendanceStatus>
    ): Result<Boolean> {
        _isSyncing.value = true
        _syncStatus.value = "Submitting to MongoDB..."

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

        try {
            val recRes = mongoClientManager.insertAttendanceRecords(newRecords)
            val actRes = mongoClientManager.insertActivityLog(activity)
            if (recRes.isSuccess && actRes.isSuccess) {
                _syncStatus.value = "Saved & Synced with MongoDB"
            } else {
                _syncStatus.value = "MongoDB error: ${recRes.exceptionOrNull()?.message ?: actRes.exceptionOrNull()?.message}"
            }
        } catch (e: Exception) {
            _syncStatus.value = "MongoDB error: ${e.message}"
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

        // Save locally first so user sees the added student immediately in UI
        _students.value = _students.value + newStudent

        try {
            val res = mongoClientManager.insertStudent(newStudent)
            if (res.isSuccess) {
                _syncStatus.value = "Student Added & Synced with MongoDB"
            } else {
                _syncStatus.value = "Student Added locally (${res.exceptionOrNull()?.message})"
            }
        } catch (e: Exception) {
            _syncStatus.value = "Student Added locally (${e.message})"
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
