package com.example.attendencestudents

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.attendencestudents.ui.AttendanceViewModel
import com.example.attendencestudents.ui.MainContainer
import com.example.attendencestudents.ui.theme.AttendenceStudentsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendenceStudentsTheme {
                MainContainer(viewModel = viewModel)
            }
        }
    }
}
