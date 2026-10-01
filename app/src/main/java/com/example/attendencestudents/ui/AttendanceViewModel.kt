package com.example.attendencestudents.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendencestudents.data.auth.AuthManager
import com.example.attendencestudents.data.model.ActivityLog
import com.example.attendencestudents.data.model.AttendanceStatus
import com.example.attendencestudents.data.model.SemesterSummary
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceViewModel(
    val repository: AttendanceRepository = AttendanceRepository()
) : ViewModel() {

    val isLoggedIn = AuthManager.isLoggedIn
    val currentUser = AuthManager.currentUser

    val syncStatus = repository.syncStatus
    val isSyncing = repository.isSyncing

    private val _selectedSemester = MutableStateFlow<Int>(1)
    val selectedSemester: StateFlow<Int> = _selectedSemester.asStateFlow()

    private val _selectedSubject = MutableStateFlow<String>("Data Structures & Algorithms")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _selectedDate = MutableStateFlow<String>(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Student ID -> AttendanceStatus (UNMARKED / NEUTRAL, PRESENT, ABSENT)
    private val _studentAttendanceMap = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val studentAttendanceMap: StateFlow<Map<String, AttendanceStatus>> = _studentAttendanceMap.asStateFlow()

    val activityLogs: StateFlow<List<ActivityLog>> = repository.activityLogs

    val studentsInSelectedSemester: StateFlow<List<Student>> = combine(
        repository.students,
        _selectedSemester
    ) { allStudents, sem ->
        allStudents.filter { it.semester == sem }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _semesterSummaries = MutableStateFlow<List<SemesterSummary>>(emptyList())
    val semesterSummaries: StateFlow<List<SemesterSummary>> = _semesterSummaries.asStateFlow()

    private val _submissionMessage = MutableStateFlow<String?>(null)
    val submissionMessage: StateFlow<String?> = _submissionMessage.asStateFlow()

    init {
        // Exclusively fetch from Supabase on startup
        viewModelScope.launch {
            repository.syncWithSupabase()
            updateSemesterSummaries()
        }
    }

    fun login(email: String, pass: String): Result<Boolean> {
        val result = AuthManager.login(email, pass)
        return if (result.isSuccess) {
            Result.success(true)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Login failed"))
        }
    }

    fun logout() {
        AuthManager.logout()
    }

    fun setSemester(sem: Int) {
        _selectedSemester.value = sem
        val defaultSubject = when (sem) {
            1 -> "Programming in C"
            2 -> "Data Structures & Algorithms"
            3 -> "Object Oriented Programming"
            4 -> "Database Management Systems"
            5 -> "Operating Systems"
            6 -> "Computer Networks"
            7 -> "Artificial Intelligence & ML"
            8 -> "Cloud Computing & DevOps"
            else -> "Core Computer Science"
        }
        _selectedSubject.value = defaultSubject
        resetAttendanceForCurrentSemester()
    }

    fun setSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun setStudentStatus(studentId: String, status: AttendanceStatus) {
        val currentMap = _studentAttendanceMap.value.toMutableMap()
        currentMap[studentId] = status
        _studentAttendanceMap.value = currentMap
    }

    fun toggleStudentStatus(studentId: String) {
        val currentMap = _studentAttendanceMap.value.toMutableMap()
        val currentStatus = currentMap[studentId] ?: AttendanceStatus.UNMARKED
        val nextStatus = when (currentStatus) {
            AttendanceStatus.UNMARKED -> AttendanceStatus.PRESENT
            AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
            AttendanceStatus.ABSENT -> AttendanceStatus.UNMARKED
        }
        currentMap[studentId] = nextStatus
        _studentAttendanceMap.value = currentMap
    }

    fun markAllPresent() {
        val currentStudents = studentsInSelectedSemester.value
        val newMap = currentStudents.associate { it.id to AttendanceStatus.PRESENT }
        _studentAttendanceMap.value = newMap
    }

    fun markAllAbsent() {
        val currentStudents = studentsInSelectedSemester.value
        val newMap = currentStudents.associate { it.id to AttendanceStatus.ABSENT }
        _studentAttendanceMap.value = newMap
    }

    fun resetToNeutral() {
        resetAttendanceForCurrentSemester()
    }

    private fun resetAttendanceForCurrentSemester() {
        val currentStudents = studentsInSelectedSemester.value
        val defaultMap = currentStudents.associate { it.id to AttendanceStatus.UNMARKED }
        _studentAttendanceMap.value = defaultMap
    }

    fun submitAttendance() {
        val sem = _selectedSemester.value
        val subj = _selectedSubject.value
        val date = _selectedDate.value
        val map = _studentAttendanceMap.value

        viewModelScope.launch {
            val res = repository.submitAttendance(sem, subj, date, map)
            if (res.isSuccess) {
                _submissionMessage.value = "Attendance for Semester $sem ($subj) submitted! Unmarked students set to Absent."
                repository.syncWithSupabase()
                updateSemesterSummaries()
                resetAttendanceForCurrentSemester()
            }
        }
    }

    fun clearSubmissionMessage() {
        _submissionMessage.value = null
    }

    fun addStudent(name: String, rollNumber: String, semester: Int, department: String) {
        viewModelScope.launch {
            repository.addStudent(name, rollNumber, semester, department)
            repository.syncWithSupabase()
            updateSemesterSummaries()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.syncWithSupabase()
            updateSemesterSummaries()
        }
    }

    private fun updateSemesterSummaries() {
        _semesterSummaries.value = repository.getAllSemesterSummaries()
    }
}
