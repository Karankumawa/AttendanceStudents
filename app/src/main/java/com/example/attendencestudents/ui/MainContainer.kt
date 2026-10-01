package com.example.attendencestudents.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendencestudents.ui.activity.ActivityScreen
import com.example.attendencestudents.ui.attendance.TakeAttendanceScreen
import com.example.attendencestudents.ui.auth.LoginScreen
import com.example.attendencestudents.ui.stats.SemesterStatsScreen
import com.example.attendencestudents.ui.students.StudentsScreen

enum class NavSection(val title: String, val icon: ImageVector) {
    TAKE_ATTENDANCE("Take Attendance", Icons.Default.CheckCircle),
    SEMESTER_STATS("All Semesters", Icons.Default.Assessment),
    ACTIVITY_LOGS("Activity", Icons.Default.History),
    STUDENTS("Students", Icons.Default.People)
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

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { email, pass ->
                viewModel.login(email, pass)
            }
        )
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Attendance Portal",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                )
                                Text(
                                    text = "Logged as: ${currentUser?.email ?: "admin@admin.com"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            // Supabase Sync Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSyncing) "Syncing..." else "Supabase Live",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            // Logout Button
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Logout Admin",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )

                    // Status Bar Banner
                    Surface(
                        color = if (syncStatus.contains("Synced") || syncStatus.contains("Live")) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status: $syncStatus",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (syncStatus.contains("Synced") || syncStatus.contains("Live")) Color(0xFF166534) else Color(0xFF92400E)
                                )
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavSection.values().forEach { section ->
                        val selected = currentSection == section
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentSection = section },
                            icon = {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = section.title
                                )
                            },
                            label = {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
