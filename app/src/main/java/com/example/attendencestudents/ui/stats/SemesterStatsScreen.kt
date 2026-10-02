package com.example.attendencestudents.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.attendencestudents.data.model.SemesterSummary
import com.example.attendencestudents.data.model.StudentStat
import com.example.attendencestudents.ui.AttendanceViewModel
import java.util.Locale

@Composable
fun SemesterStatsScreen(
    viewModel: AttendanceViewModel
) {
    val semesterSummaries by viewModel.semesterSummaries.collectAsState()
    var selectedTabSemester by remember { mutableIntStateOf(0) } // 0 = ALL, 1..8 = Specific Semester

    val grandTotalClasses = semesterSummaries.sumOf { it.totalClasses }
    val grandTotalPresent = semesterSummaries.sumOf { it.totalPresent }
    val grandTotalAbsent = semesterSummaries.sumOf { it.totalAbsent }
    val grandTotalRecords = grandTotalPresent + grandTotalAbsent
    val grandPercentage = if (grandTotalRecords > 0) {
        (grandTotalPresent.toDouble() / grandTotalRecords.toDouble()) * 100.0
    } else 85.0

    val filteredSummaries = if (selectedTabSemester == 0) {
        semesterSummaries
    } else {
        semesterSummaries.filter { it.semester == selectedTabSemester }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        item {
            Column {
                Text(
                    text = "Academic Attendance Analytics",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Executive performance reports and class attendance logs for Semesters 1 to 8",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Executive Hero Summary Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Overall Academic Performance",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%% Avg", grandPercentage),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfessionalStatTile(
                            title = "Total Classes",
                            value = "$grandTotalClasses",
                            icon = Icons.Default.Class,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        ProfessionalStatTile(
                            title = "Present",
                            value = "$grandTotalPresent",
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        ProfessionalStatTile(
                            title = "Absent",
                            value = "$grandTotalAbsent",
                            icon = Icons.Default.Cancel,
                            color = Color(0xFFF43F5E),
                            modifier = Modifier.weight(1f)
                        )
                        ProfessionalStatTile(
                            title = "Overall %",
                            value = String.format(Locale.getDefault(), "%.1f%%", grandPercentage),
                            icon = Icons.Default.Percent,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Semester Filter Chips Row
        item {
            Column {
                Text(
                    text = "Filter by Semester",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isAllSelected = selectedTabSemester == 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isAllSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedTabSemester = 0 }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All Semesters",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    items((1..8).toList()) { sem ->
                        val isSelected = selectedTabSemester == sem
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedTabSemester = sem }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sem $sem",
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

        // List of Semester Cards
        items(filteredSummaries, key = { it.semester }) { summary ->
            ProfessionalSemesterCard(summary = summary)
        }
    }
}

@Composable
fun ProfessionalStatTile(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfessionalSemesterCard(summary: SemesterSummary) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when {
        summary.attendancePercentage >= 75.0 -> Color(0xFF10B981) // Green
        summary.attendancePercentage >= 60.0 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFF43F5E) // Red
    }

    val statusText = when {
        summary.attendancePercentage >= 75.0 -> "Good Standing"
        summary.attendancePercentage >= 60.0 -> "At Risk"
        else -> "Critical Low"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S${summary.semester}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Semester ${summary.semester}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${summary.studentStats.size} Students Enrolled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%% (%s)", summary.attendancePercentage, statusText),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (summary.attendancePercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Classes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "${summary.totalClasses}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Present", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "${summary.totalPresent}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Absent", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "${summary.totalAbsent}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E)
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Avg %", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", summary.attendancePercentage),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expand Button
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (expanded) "Hide Roster" else "View Student Breakdown (${summary.studentStats.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            // Student Roster Expandable
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Students Performance Roster — Semester ${summary.semester}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    summary.studentStats.forEach { stat ->
                        ProfessionalStudentRow(stat = stat)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalStudentRow(stat: StudentStat) {
    val statColor = when {
        stat.percentage >= 75.0 -> Color(0xFF10B981)
        stat.percentage >= 60.0 -> Color(0xFFF59E0B)
        else -> Color(0xFFF43F5E)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.studentName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Roll: ${stat.rollNumber} | Present: ${stat.presentCount} | Absent: ${stat.absentCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = statColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", stat.percentage),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = statColor,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
