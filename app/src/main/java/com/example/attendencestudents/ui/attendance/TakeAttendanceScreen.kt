package com.example.attendencestudents.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendencestudents.data.model.AttendanceStatus
import com.example.attendencestudents.data.model.Student
import com.example.attendencestudents.ui.AttendanceViewModel

val SUBJECT_OPTIONS = listOf(
    "CSE - Data Structures & Algorithms",
    "CSE - Operating Systems & Networks",
    "ECE - Digital Electronics & Signals",
    "ECE - Communication Systems",
    "CIVIL - Structural Analysis & Design",
    "MECH - Robotics & Automation",
    "EEE - Power Electronics & Drives"
)

@Composable
fun TakeAttendanceScreen(
    viewModel: AttendanceViewModel,
    snackbarHostState: SnackbarHostState
) {
    val selectedSemester by viewModel.selectedSemester.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val liveDate by viewModel.liveDate.collectAsState()
    val liveTime by viewModel.liveTime.collectAsState()
    val rawStudents by viewModel.studentsInSelectedSemester.collectAsState()
    val studentAttendanceMap by viewModel.studentAttendanceMap.collectAsState()
    val submissionMessage by viewModel.submissionMessage.collectAsState()

    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    // Sorted students by Department & Roll Number
    val students = remember(rawStudents) {
        rawStudents.sortedWith(compareBy({ it.department }, { it.rollNumber }))
    }

    LaunchedEffect(submissionMessage) {
        submissionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSubmissionMessage()
        }
    }

    val presentCount = students.count { (studentAttendanceMap[it.id] ?: AttendanceStatus.UNMARKED) == AttendanceStatus.PRESENT }
    val absentCount = students.count { (studentAttendanceMap[it.id] ?: AttendanceStatus.UNMARKED) == AttendanceStatus.ABSENT }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth > 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar: Semester Selector
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Text(
                        text = "Select Semester",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..8).toList()) { sem ->
                            val isSelected = sem == selectedSemester
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.setSemester(sem) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Semester $sem",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Session Configuration Card
            Column(modifier = Modifier.padding(16.dp)) {
                // Live Session Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE SESSION",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = liveDate,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = liveTime,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subject Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject / Course") },
                        trailingIcon = {
                            IconButton(onClick = { subjectDropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Subject")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subjectDropdownExpanded = true },
                        singleLine = true
                    )

                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        SUBJECT_OPTIONS.forEach { subj ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = subj,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (subj == selectedSubject) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                onClick = {
                                    viewModel.setSubject(subj)
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action Buttons & Status Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ElevatedButton(
                            onClick = { viewModel.markAllPresent() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = Color(0xFFDCFCE7),
                                contentColor = Color(0xFF166534)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.markAllAbsent() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF991B1B)
                            )
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("All Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetToNeutral() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Counters Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("P: $presentCount", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF15803D), fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("A: $absentCount", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Student Roster Grid/List
            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No students found in Semester $selectedSemester",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (isWideScreen) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val status = studentAttendanceMap[student.id] ?: AttendanceStatus.UNMARKED
                        StudentAttendanceRow(
                            student = student,
                            status = status,
                            onSetStatus = { newStatus ->
                                viewModel.setStudentStatus(student.id, newStatus)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        val status = studentAttendanceMap[student.id] ?: AttendanceStatus.UNMARKED
                        StudentAttendanceRow(
                            student = student,
                            status = status,
                            onSetStatus = { newStatus ->
                                viewModel.setStudentStatus(student.id, newStatus)
                            }
                        )
                    }
                }
            }

            // Bottom Submit Button Bar
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Button(
                        onClick = { viewModel.submitAttendance() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "SUBMIT ATTENDANCE TO SUPABASE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceRow(
    student: Student,
    status: AttendanceStatus,
    onSetStatus: (AttendanceStatus) -> Unit
) {
    val cardBg = when (status) {
        AttendanceStatus.PRESENT -> Color(0xFFF0FDF4)
        AttendanceStatus.ABSENT -> Color(0xFFFFF1F2)
        AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                AttendanceStatus.PRESENT -> Color(0xFFBBF7D0)
                                AttendanceStatus.ABSENT -> Color(0xFFFECDD3)
                                AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = student.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                AttendanceStatus.PRESENT -> Color(0xFF166534)
                                AttendanceStatus.ABSENT -> Color(0xFF991B1B)
                                AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = student.department.take(5),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Text(
                        text = "Roll: ${student.rollNumber} | Sem ${student.semester}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Choice Pills (P, A)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPillButton(
                    label = "P",
                    isSelected = status == AttendanceStatus.PRESENT,
                    activeColor = Color(0xFF16A34A),
                    activeBg = Color(0xFFDCFCE7),
                    onClick = {
                        val next = if (status == AttendanceStatus.PRESENT) AttendanceStatus.UNMARKED else AttendanceStatus.PRESENT
                        onSetStatus(next)
                    }
                )

                StatusPillButton(
                    label = "A",
                    isSelected = status == AttendanceStatus.ABSENT,
                    activeColor = Color(0xFFDC2626),
                    activeBg = Color(0xFFFEE2E2),
                    onClick = {
                        val next = if (status == AttendanceStatus.ABSENT) AttendanceStatus.UNMARKED else AttendanceStatus.ABSENT
                        onSetStatus(next)
                    }
                )
            }
        }
    }
}

@Composable
fun StatusPillButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeBg else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) activeColor else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) activeColor else Color.Gray
            )
        )
    }
}
