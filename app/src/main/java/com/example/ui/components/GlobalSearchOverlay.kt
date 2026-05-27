package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.MainApplication
import com.example.ui.theme.LocalThemeController
import kotlinx.coroutines.flow.*

data class GlobalSearchResult(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String? = null,
    val onClick: () -> Unit = {}
)

@Composable
fun GlobalSearchOverlay(
    searchQuery: String,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (searchQuery.isBlank()) return

    val context = LocalContext.current
    val database = remember(context) { (context.applicationContext as MainApplication).database }
    val isDark = LocalThemeController.current.isDarkTheme

    val bgColor = if (isDark) Color(0xFF2C2C2C) else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color(0xFFAAAAAA) else Color.DarkGray

    val menuOptions = remember {
        listOf(
            GlobalSearchResult("Profile", "View and edit profile details", Icons.Default.Person, "profile"),
            GlobalSearchResult("Academics", "Degrees, colleges, and schools", Icons.Default.School, "academics"),
            GlobalSearchResult("Documents", "Files, folders, and digit locker", Icons.Default.Folder, "documents"),
            GlobalSearchResult("Wallet", "Secure passwords and IDs", Icons.Default.AccountBalanceWallet, "wallet_security"),
            GlobalSearchResult("Dashboard", "Home screen and notifications", Icons.Default.Home, "dashboard"),
            GlobalSearchResult("Tools", "Academic tools and utilities", Icons.Default.Build, "tools"),
            GlobalSearchResult("Settings", "Appearance and app preferences", Icons.Default.Settings, "settings"),
            GlobalSearchResult("Trash", "Deleted documents", Icons.Default.Delete, "documents")
        )
    }

    var searchResults by remember { mutableStateOf<List<GlobalSearchResult>>(emptyList()) }

    LaunchedEffect(searchQuery) {
        val q = searchQuery.lowercase()
        val staticResults = menuOptions.filter { 
            it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q) 
        }.map { res -> res.copy(onClick = { onNavigate(res.route ?: "") ; onDismiss() }) }

        val flows = buildList {
            // Documents Flow
            add(
                database.documentDao().searchFiles(q).map { files ->
                    files.map { f ->
                        GlobalSearchResult(
                            title = f.name,
                            subtitle = if (f.isFolder) "Folder" else "Document File",
                            icon = if (f.isFolder) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            onClick = { onNavigate("documents") ; onDismiss() }
                        )
                    }
                }
            )

            // Academic Items Flow
            add(
                database.academicItemDao().searchAcademicItems(q).map { items ->
                    items.map { item ->
                        GlobalSearchResult(
                            title = item.title,
                            subtitle = "Academic • ${item.type}",
                            icon = Icons.Default.School,
                            onClick = { onNavigate("academics") ; onDismiss() }
                        )
                    }
                }
            )

            // Tasks Flow 
            add(
                database.taskDao().searchTasks(q).map { tasks ->
                    tasks.map { t ->
                        GlobalSearchResult(
                            title = t.title,
                            subtitle = "Task • ${if (t.completed) "Completed" else "In Progress"}",
                            icon = Icons.Default.CheckCircle,
                            onClick = { onNavigate("tasks") ; onDismiss() }
                        )
                    }
                }
            )

            // Reminders Flow
            add(
                database.reminderDao().searchReminders(q).map { reminders ->
                    reminders.map { r ->
                        val dateString = try {
                            java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(r.timeInMillis))
                        } catch (e: Exception) {
                            "Scheduled"
                        }
                        GlobalSearchResult(
                            title = r.title,
                            subtitle = "Reminder • At $dateString",
                            icon = Icons.Default.NotificationImportant,
                            onClick = { onNavigate("reminders") ; onDismiss() }
                        )
                    }
                }
            )

            // Wallet Cards Flow (Excludes sensitive password numbers or credit card card-numbers completely)
            add(
                database.walletDao().searchCards(q).map { cards ->
                    cards.map { c ->
                        GlobalSearchResult(
                            title = c.title,
                            subtitle = "Secure Wallet • ${c.notes?.take(30) ?: "Encrypted Item"}",
                            icon = Icons.Default.Fingerprint,
                            onClick = { onNavigate("wallet_security") ; onDismiss() }
                        )
                    }
                }
            )
        }

        combine(flows) { resultsArrays ->
            val dynamicResults = resultsArrays.flatMap { it.toList() }
            staticResults + dynamicResults
        }.collect { results ->
            searchResults = results
        }
    }

    Popup(
        alignment = Alignment.TopCenter,
        offset = androidx.compose.ui.unit.IntOffset(0, 180),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
        if (searchResults.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = 400.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = bgColor
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    item {
                        Text(
                            "Global Search Results",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = subtitleColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    items(searchResults) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { result.onClick() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = result.icon,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(result.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(result.subtitle, color = subtitleColor, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
