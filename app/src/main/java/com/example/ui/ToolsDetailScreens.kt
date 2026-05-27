package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(onBack: () -> Unit) {
    var timeLeft by remember { mutableStateOf(25 * 60) } // 25 mins
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        if (timeLeft == 0) isRunning = false
    }

    Scaffold(
        topBar = {
            com.example.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "Focus Timer",
                showProfileIcon = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val minutes = timeLeft / 60
            val seconds = timeLeft % 60
            
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Button(
                    onClick = { timeLeft = 25 * 60; isRunning = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "Reset")
                }
                
                FloatingActionButton(
                    onClick = { isRunning = !isRunning },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Play"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgpaCalculatorScreen(onBack: () -> Unit) {
    data class CourseEntry(var credits: String = "", var gradePoints: String = "")
    var courses by remember { mutableStateOf(listOf(CourseEntry(), CourseEntry(), CourseEntry())) }
    var result by remember { mutableStateOf<Float?>(null) }

    Scaffold(
        topBar = {
            com.example.ui.components.TopSearchBar(
                onOpenDrawer = onBack,
                isBackButton = true,
                title = "SGPA / CGPA Calculator",
                showProfileIcon = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            ) {
                items(courses.size) { index ->
                    val course = courses[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = course.credits,
                            onValueChange = { newValue ->
                                val list = courses.toMutableList()
                                list[index] = course.copy(credits = newValue)
                                courses = list
                            },
                            label = { Text("Credits") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = course.gradePoints,
                            onValueChange = { newValue ->
                                val list = courses.toMutableList()
                                list[index] = course.copy(gradePoints = newValue)
                                courses = list
                            },
                            label = { Text("Grade Points (1-10)") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            val list = courses.toMutableList()
                            list.removeAt(index)
                            courses = list
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
                
                item {
                    Button(
                        onClick = { courses = courses + CourseEntry() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Course")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Course")
                    }
                }
            }
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (result != null) {
                        Text("Your SGPA/CGPA", fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(String.format("%.2f", result), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Text("Enter credits and grade points to calculate", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            var totalCredits = 0f
                            var totalPoints = 0f
                            courses.forEach {
                                val c = it.credits.toFloatOrNull() ?: 0f
                                val g = it.gradePoints.toFloatOrNull() ?: 0f
                                totalCredits += c
                                totalPoints += (c * g)
                            }
                            result = if (totalCredits > 0) totalPoints / totalCredits else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Calculate")
                    }
                }
            }
        }
    }
}
