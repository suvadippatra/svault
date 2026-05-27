package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TopSearchBar
import com.example.ui.theme.LocalThemeController

@Composable
fun ToolsScreen(onOpenDrawer: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
    val themeController = LocalThemeController.current
    val animationsEnabled = themeController.animationsEnabled
    
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Educational", "Image", "Files", "Utility")
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600
    
    val tools = listOf(
        ToolItem("CGPA Calculator", "Calculate and track your academic performance.", Icons.Default.Calculate, "Educational", Screen.CgpaCalculator.route),
        ToolItem("Focus Timer", "Concentrate on your study with Pomodoro technique.", Icons.Default.Timer, "Utility", Screen.FocusTimer.route),
        ToolItem("Tasks Manager", "Organize your daily study and life tasks.", Icons.Default.List, "Utility", Screen.Tasks.route),
        ToolItem("Reminders", "Never miss a deadline with scheduled alerts.", Icons.Default.Notifications, "Utility", Screen.Reminders.route),
        ToolItem("Interactive PDF", "Read, annotate, and organize text-rich PDFs.", Icons.Default.PictureAsPdf, "Educational", Screen.PdfReaderNav.route),
        ToolItem("Compress Image", "Reduce image size while maintaining quality.", Icons.Default.Compress, "Image", ""),
        ToolItem("Image to PDF", "Convert your photos into high-quality PDF files.", Icons.Default.PictureAsPdf, "Image", ""),
        ToolItem("Secure Backup", "Safely backup your files with AES-256 encryption.", Icons.Default.Backup, "Files", ""),
        ToolItem("Data Export", "Export your academic and wallet data easily.", Icons.Default.ImportExport, "Files", "")
    )
    
    val filteredTools = remember(selectedCategory) {
        if (selectedCategory == "All") tools else tools.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSearchBar(onOpenDrawer = onOpenDrawer, onNavigate = onNavigate)
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Smart Toolkit",
                        style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "A collection of productivity and utility tools designed to enhance your academic lifestyle and manage your data securely.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.size) { index ->
                        val cat = categories[index]
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                }
            }
            
            item {
                // Multi-column grid based on screen size
                val columns = if (isTablet) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.heightIn(max = 2000.dp).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false 
                ) {
                    items(filteredTools.size) { index ->
                        val tool = filteredTools[index]
                        ToolCard(tool) {
                            if (tool.route.isNotEmpty()) onNavigate(tool.route)
                        }
                    }
                }
            }
        }
    }
}

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: String,
    val route: String
)

@Composable
fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit
) {
    val isDark = LocalThemeController.current.isDarkTheme
    
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}
