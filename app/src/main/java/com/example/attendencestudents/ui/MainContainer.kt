package com.example.attendencestudents.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendencestudents.data.auth.UserRole
import com.example.attendencestudents.ui.activity.ActivityScreen
import com.example.attendencestudents.ui.attendance.TakeAttendanceScreen
import com.example.attendencestudents.ui.auth.LoginScreen
import com.example.attendencestudents.ui.components.AppHeaderBranding
import com.example.attendencestudents.ui.stats.SemesterStatsScreen
import com.example.attendencestudents.ui.studentportal.StudentPortalScreen
import com.example.attendencestudents.ui.students.StudentsScreen

enum class NavSection(val title: String, val icon: ImageVector) {
    TAKE_ATTENDANCE("Attendance", Icons.Default.CheckCircle),
    SEMESTER_STATS("Semesters", Icons.Default.Assessment),
    ACTIVITY_LOGS("Activity", Icons.Default.History),
    STUDENTS("Students", Icons.Default.People)
}

@Composable
fun AdminDashboardScreen(
    viewModel: AttendanceViewModel
) {
    MainContainer(viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    viewModel: AttendanceViewModel
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var currentSection by remember { mutableStateOf(NavSection.TAKE_ATTENDANCE) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error notification snackbar ONLY if an error occurs
    LaunchedEffect(syncStatus) {
        if (syncStatus.contains("Error", ignoreCase = true) || syncStatus.contains("Failed", ignoreCase = true)) {
            snackbarHostState.showSnackbar("Error Notification: $syncStatus")
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { email, pass, role ->
                viewModel.login(email, pass, role)
            }
        )
    } else {
        val isStudentRole = currentUser?.role == UserRole.STUDENT

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        AppHeaderBranding(
                            title = if (isStudentRole) "Student Portal" else "Attendance Portal",
                            subtitle = "Logged in as: ${currentUser?.email ?: "user"}"
                        )
                    },
                    actions = {
                        // Role / Read-Only Badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isStudentRole) MaterialTheme.colorScheme.tertiaryContainer else Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (isStudentRole) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF047857),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isStudentRole) "Student (Read-Only)" else if (isSyncing) "Syncing..." else "Supabase Live",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isStudentRole) MaterialTheme.colorScheme.onTertiaryContainer else Color(0xFF047857),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Logout Button
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            },
            bottomBar = {
                if (!isStudentRole) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavSection.entries.forEach { section ->
                            val selected = currentSection == section
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentSection = section },
                                alwaysShowLabel = true,
                                icon = {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = section.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isStudentRole) {
                    // Dedicated Student Portal View (Read-Only)
                    StudentPortalScreen(viewModel = viewModel)
                } else {
                    // Admin Views
                    when (currentSection) {
                        NavSection.TAKE_ATTENDANCE -> {
                            TakeAttendanceScreen(
                                viewModel = viewModel,
                                snackbarHostState = snackbarHostState
                            )
                        }
                        NavSection.SEMESTER_STATS -> {
                            SemesterStatsScreen(viewModel = viewModel)
                        }
                        NavSection.ACTIVITY_LOGS -> {
                            ActivityScreen(viewModel = viewModel)
                        }
                        NavSection.STUDENTS -> {
                            StudentsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
