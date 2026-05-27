package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalThemeController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val isDark = LocalThemeController.current.isDarkTheme
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA)
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = textColor
                )
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Identity
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Studez", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
                Text("Your Ultimate Academic Companion", fontSize = 14.sp, color = subTextColor)
            }

            Text(
                "Studez is designed to simplify your student life by centralizing all your academic data, documents, and reminders in one highly secure, offline-first application.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            AboutSection(
                icon = Icons.Default.Dashboard,
                title = "Dashboard",
                description = "Your central command center. View upcoming reminders, tracked tasks, daily inspiration quotes, and an immediate summary of your academic progress."
            , cardColor, textColor, subTextColor)

            AboutSection(
                icon = Icons.Default.School,
                title = "Academics",
                description = "Maintain a chronological timeline of your education. Track semesters, courses, CGPAs, and link important certificates or transcripts directly to each milestone."
            , cardColor, textColor, subTextColor)

            AboutSection(
                icon = Icons.Default.Folder,
                title = "Documents & Wallet",
                description = "A secure vault for your digital life. Organize files into folders, and use the 'Wallet' for highly sensitive IDs encrypted with hardware-backed security."
            , cardColor, textColor, subTextColor)

            AboutSection(
                icon = Icons.Default.Build,
                title = "Tools",
                description = "Equipped with essential student utilities: a Focus Timer for deep work sessions, a CGPA Calculator for grade projections, and Quick Notes for rapid capturing."
            , cardColor, textColor, subTextColor)

            AboutSection(
                icon = Icons.Default.Notifications,
                title = "Reminders & Alerts",
                description = "Never miss a deadline. Set exact alarms for exams, meetings, or project submissions with native Android notification integration."
            , cardColor, textColor, subTextColor)

            Divider(color = subTextColor.copy(alpha = 0.2f))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Developed with ❤️ by @suvadippatra", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Version 1.2.0 (Build 36)", fontSize = 12.sp, color = subTextColor)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AboutSection(icon: ImageVector, title: String, description: String, cardColor: Color, textColor: Color, subTextColor: Color) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 14.sp, color = subTextColor, lineHeight = 20.sp)
            }
        }
    }
}
