package com.example.attendencestudents.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendencestudents.data.auth.AuthManager
import com.example.attendencestudents.data.model.ActivityLog
import com.example.attendencestudents.data.model.AttendanceStatus
import com.example.attendencestudents.data.model.SemesterSummary
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.data.repository.AttendanceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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

    private val _selectedSubject = MutableStateFlow<String>("CSE - Data Structures & Algorithms")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _selectedDepartmentFilter = MutableStateFlow<String>("ALL")
    val selectedDepartmentFilter: StateFlow<String> = _selectedDepartmentFilter.asStateFlow()

    private val _liveDate = MutableStateFlow<String>(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val liveDate: StateFlow<String> = _liveDate.asStateFlow()

    private val _liveTime = MutableStateFlow<String>(
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
    )
    val liveTime: StateFlow<String> = _liveTime.asStateFlow()

    // Student ID -> AttendanceStatus (UNMARKED / NEUTRAL, PRESENT, ABSENT)
    private val _studentAttendanceMap = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    val studentAttendanceMap: StateFlow<Map<String, AttendanceStatus>> = _studentAttendanceMap.asStateFlow()

    val activityLogs: StateFlow<List<ActivityLog>> = repository.activityLogs

    // Combines Students, Semester, Subject, and Department Filter to filter roster dynamically
    val studentsInSelectedSemester: StateFlow<List<Student>> = combine(
        repository.students,
        _selectedSemester,
        _selectedSubject,
        _selectedDepartmentFilter
    ) { allStudents, sem, subj, deptFilter ->
        // 1. Filter by Semester
        val semStudents = allStudents.filter { it.semester == sem }

        // 2. Extract Branch/Department Prefix from Subject Name e.g. "EEE - Power Systems" -> "EEE"
        val subjectDeptPrefix = if (subj.contains("-")) {
            subj.substringBefore("-").trim()
        } else ""

        val effectiveDept = if (deptFilter != "ALL") deptFilter else subjectDeptPrefix

        if (effectiveDept.isNotBlank() && effectiveDept != "ALL" && effectiveDept != "General") {
            val branchFiltered = semStudents.filter { student ->
                val sDept = student.department.uppercase()
                val targetDept = effectiveDept.uppercase()

                sDept.contains(targetDept) ||
                        (targetDept == "CSE" && (sDept.contains("COMPUTER") || sDept.contains("CSE"))) ||
                        (targetDept == "ECE" && (sDept.contains("ELECTRONICS") || sDept.contains("ECE"))) ||
                        (targetDept == "EEE" && (sDept.contains("ELECTRICAL") || sDept.contains("EEE"))) ||
                        (targetDept == "CIVIL" && sDept.contains("CIVIL")) ||
                        (targetDept == "MECH" && (sDept.contains("MECHANICAL") || sDept.contains("MECH")))
            }

            // If students exist in that specific branch, return filtered list; else fallback to all semester students
            if (branchFiltered.isNotEmpty()) branchFiltered else semStudents
        } else {
            semStudents
        }
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

        // Live clock ticker loop
        viewModelScope.launch {
            while (isActive) {
                updateLiveDateTime()
                delay(1000L)
            }
        }
    }

    fun updateLiveDateTime() {
        val now = Date()
        _liveDate.value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        _liveTime.value = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(now)
    }

    suspend fun login(email: String, pass: String): Result<Boolean> {
        val result = AuthManager.login(email, pass, repository.supabaseClient, repository.students.value)
        return if (result.isSuccess) {
            Result.success(true)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Invalid student, please contact admin."))
        }
    }

    fun logout() {
        AuthManager.logout()
    }

    fun setSemester(sem: Int) {
        _selectedSemester.value = sem
        val defaultSubject = when (sem) {
            1 -> "CSE - Programming in C"
            2 -> "CSE - Data Structures & Algorithms"
            3 -> "ECE - Object Oriented Programming"
            4 -> "CSE - Database Management Systems"
            5 -> "ECE - Communication Systems"
            6 -> "EEE - Power Electronics & Drives"
            7 -> "CIVIL - Structural Analysis & Design"
            8 -> "MECH - Robotics & Automation"
            else -> "CSE - Core Computer Science"
        }
        _selectedSubject.value = defaultSubject
        resetAttendanceForCurrentSemester()
    }

    fun setSubject(subject: String) {
        _selectedSubject.value = subject
        // Reset manual department filter so subject prefix takes effect
        _selectedDepartmentFilter.value = "ALL"
        resetAttendanceForCurrentSemester()
    }

    fun setDepartmentFilter(dept: String) {
        _selectedDepartmentFilter.value = dept
        resetAttendanceForCurrentSemester()
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
            AttendanceStatus.ABSENT -> AttendanceStatus.LEAVE
            else -> AttendanceStatus.UNMARKED
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
        updateLiveDateTime()
        val sem = _selectedSemester.value
        val subj = _selectedSubject.value
        val date = _liveDate.value
        val time = _liveTime.value
        val map = _studentAttendanceMap.value

        viewModelScope.launch {
            val res = repository.submitAttendance(sem, subj, date, time, map)
            if (res.isSuccess) {
                _submissionMessage.value = "Attendance for Semester $sem ($subj) submitted on $date at $time!"
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

    fun deleteStudent(studentId: String) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
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
