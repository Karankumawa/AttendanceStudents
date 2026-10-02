package com.example.attendencestudents.ui.studentportal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendencestudents.data.model.AttendanceRecord
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.ui.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PortalTab(val title: String) {
    DAY("Day View"),
    MONTH("Month View"),
    SEMESTER("Semester View")
}

@Composable
fun StudentPortalScreen(
    viewModel: AttendanceViewModel
) {
    val allStudents by viewModel.repository.students.collectAsState()
    val allRecords by viewModel.repository.attendanceRecords.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Binds strictly to the logged-in student's record
    val loggedInStudent = remember(allStudents, currentUser) {
        allStudents.firstOrNull { student ->
            student.id == currentUser?.studentId ||
                    student.email.trim().equals(currentUser?.email?.trim(), ignoreCase = true) ||
                    student.rollNumber == currentUser?.rollNumber
        } ?: allStudents.firstOrNull()
    }

    var activeTab by remember { mutableStateOf(PortalTab.DAY) }

    // Date state for Day View
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    var selectedDayDate by remember { mutableStateOf(todayDateStr) }

    // Calendar state for Month View
    val calendarMonth = remember { Calendar.getInstance() }
    var currentYearMonth by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendarMonth.time))
    }

    // Semester state for Semester View
    var selectedSemester by remember { mutableIntStateOf(loggedInStudent?.semester ?: 1) }

    LaunchedEffect(loggedInStudent) {
        loggedInStudent?.let { selectedSemester = it.semester }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Banner - Read Only Notice
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Read Only",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Student Portal — Personal Attendance",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                    Text(
                        text = "Viewing personal records only. Attendance modification disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Student Profile Header Card (Single isolated student)
            item {
                StudentPersonalProfileCard(student = loggedInStudent)
            }

            // Tab Selector (Day, Month, Semester)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PortalTab.entries.forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { activeTab = tab }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content
            when (activeTab) {
                PortalTab.DAY -> {
                    item {
                        DayAttendanceView(
                            student = loggedInStudent,
                            allRecords = allRecords,
                            selectedDate = selectedDayDate,
                            onDateChanged = { selectedDayDate = it }
                        )
                    }
                }
                PortalTab.MONTH -> {
                    item {
                        MonthAttendanceView(
                            student = loggedInStudent,
                            allRecords = allRecords,
                            currentYearMonth = currentYearMonth,
                            onPrevMonth = {
                                calendarMonth.add(Calendar.MONTH, -1)
                                currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendarMonth.time)
                            },
                            onNextMonth = {
                                calendarMonth.add(Calendar.MONTH, 1)
                                currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendarMonth.time)
                            }
                        )
                    }
                }
                PortalTab.SEMESTER -> {
                    item {
                        SemesterAttendanceView(
                            student = loggedInStudent,
                            allRecords = allRecords,
                            selectedSemester = selectedSemester,
                            onSemesterChanged = { selectedSemester = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentPersonalProfileCard(student: Student?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student?.name ?: "Student Profile",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Roll: ${student?.rollNumber ?: "N/A"} | Sem ${student?.semester ?: "-"} (${student?.department ?: "CSE"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "MY RECORD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DayAttendanceView(
    student: Student?,
    allRecords: List<AttendanceRecord>,
    selectedDate: String,
    onDateChanged: (String) -> Unit
) {
    if (student == null) {
        NoStudentCard()
        return
    }

    val recordsForDay = remember(allRecords, student, selectedDate) {
        allRecords.filter { it.studentId == student.id && it.date == selectedDate }
    }

    val presentCount = recordsForDay.count { it.status == "PRESENT" }
    val absentCount = recordsForDay.count { it.status == "ABSENT" }
    val totalClasses = recordsForDay.size

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Date Selector Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Date: $selectedDate",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    OutlinedButton(
                        onClick = { onDateChanged(todayStr) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Today", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Summary Tiles for the Day
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCardTile(
                title = "Total Classes",
                value = "$totalClasses",
                icon = Icons.Default.Assessment,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCardTile(
                title = "Present",
                value = "$presentCount",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
            StatCardTile(
                title = "Absent",
                value = "$absentCount",
                icon = Icons.Default.Cancel,
                color = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
        }

        // List of Day's Records
        Text(
            text = "Classes Conducted on $selectedDate",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 4.dp)
        )

        if (recordsForDay.isEmpty()) {
            EmptyStateCard(message = "No attendance records recorded for ${student.name} on $selectedDate.")
        } else {
            recordsForDay.forEach { record ->
                AttendanceRecordRow(record = record)
            }
        }
    }
}

@Composable
fun MonthAttendanceView(
    student: Student?,
    allRecords: List<AttendanceRecord>,
    currentYearMonth: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    if (student == null) {
        NoStudentCard()
        return
    }

    val recordsForMonth = remember(allRecords, student, currentYearMonth) {
        allRecords.filter { it.studentId == student.id && it.date.startsWith(currentYearMonth) }
    }

    val presentCount = recordsForMonth.count { it.status == "PRESENT" }
    val absentCount = recordsForMonth.count { it.status == "ABSENT" }
    val totalClasses = recordsForMonth.size

    val percentage = if (totalClasses > 0) {
        (presentCount.toDouble() / totalClasses.toDouble()) * 100.0
    } else 0.0

    val statusColor = when {
        totalClasses == 0 -> Color.Gray
        percentage >= 75.0 -> Color(0xFF16A34A)
        percentage >= 60.0 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Month Selector Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Month: $currentYearMonth",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        // Monthly Performance Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Attendance Percentage",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Classes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("$totalClasses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Present", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("$presentCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Absent", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("$absentCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)))
                    }
                }
            }
        }

        // Records List for the Month
        Text(
            text = "Monthly Class Logs ($currentYearMonth)",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 4.dp)
        )

        if (recordsForMonth.isEmpty()) {
            EmptyStateCard(message = "No attendance records logged for ${student.name} in $currentYearMonth.")
        } else {
            recordsForMonth.sortedByDescending { it.date }.forEach { record ->
                AttendanceRecordRow(record = record)
            }
        }
    }
}

@Composable
fun SemesterAttendanceView(
    student: Student?,
    allRecords: List<AttendanceRecord>,
    selectedSemester: Int,
    onSemesterChanged: (Int) -> Unit
) {
    if (student == null) {
        NoStudentCard()
        return
    }

    val recordsForSemester = remember(allRecords, student, selectedSemester) {
        allRecords.filter { it.studentId == student.id && it.semester == selectedSemester }
    }

    val presentCount = recordsForSemester.count { it.status == "PRESENT" }
    val absentCount = recordsForSemester.count { it.status == "ABSENT" }
    val totalClasses = recordsForSemester.size

    val percentage = if (totalClasses > 0) {
        (presentCount.toDouble() / totalClasses.toDouble()) * 100.0
    } else 0.0

    val statusColor = when {
        totalClasses == 0 -> Color.Gray
        percentage >= 75.0 -> Color(0xFF16A34A)
        percentage >= 60.0 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    // Group records by subject
    val subjectMap = remember(recordsForSemester) {
        recordsForSemester.groupBy { it.subject }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Semester Selector Chips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Select Semester (1 to 8)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..8).forEach { sem ->
                        val isSelected = selectedSemester == sem
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSemesterChanged(sem) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S$sem",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        }

        // Low Attendance Warning Card if < 75%
        if (totalClasses > 0 && percentage < 75.0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Low Attendance Warning",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        )
                        Text(
                            text = "Current attendance (${String.format(Locale.getDefault(), "%.1f%%", percentage)}) is below mandatory 75% threshold.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }

        // Semester Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Semester $selectedSemester Overall Status",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Classes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("$totalClasses", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Present", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("$presentCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A)))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Absent", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("$absentCount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)))
                    }
                }
            }
        }

        // Subject-wise breakdown
        Text(
            text = "Subject-Wise Breakdown (Semester $selectedSemester)",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 4.dp)
        )

        if (subjectMap.isEmpty()) {
            EmptyStateCard(message = "No subject records available for Semester $selectedSemester.")
        } else {
            subjectMap.forEach { (subjectName, recList) ->
                val subjPresent = recList.count { it.status == "PRESENT" }
                val subjTotal = recList.size
                val subjPct = if (subjTotal > 0) (subjPresent.toDouble() / subjTotal.toDouble()) * 100.0 else 0.0

                SubjectStatCard(
                    subjectName = subjectName,
                    totalClasses = subjTotal,
                    presentCount = subjPresent,
                    absentCount = subjTotal - subjPresent,
                    percentage = subjPct
                )
            }
        }
    }
}

@Composable
fun StatCardTile(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
            Text(title, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AttendanceRecordRow(record: AttendanceRecord) {
    val isPresent = record.status == "PRESENT"
    val badgeColor = if (isPresent) Color(0xFF16A34A) else Color(0xFFDC2626)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.subject,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Date: ${record.date} | Semester ${record.semester}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isPresent) "PRESENT" else "ABSENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun SubjectStatCard(
    subjectName: String,
    totalClasses: Int,
    presentCount: Int,
    absentCount: Int,
    percentage: Double
) {
    val color = when {
        percentage >= 75.0 -> Color(0xFF16A34A)
        percentage >= 60.0 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subjectName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = color)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total: $totalClasses classes | Present: $presentCount | Absent: $absentCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoStudentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No students found in system database. Please contact admin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
