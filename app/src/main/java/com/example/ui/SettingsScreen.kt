package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalThemeController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToWalletSecurity: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val securityManager = remember { com.example.security.WalletSecurityManager(context) }
    
    val themeController = LocalThemeController.current
    val isSecurityEnabled by securityManager.isSecurityEnabled.collectAsState()
    
    val prefs = remember { com.example.data.AppPreferences(context) }
    val pdfMode by prefs.pdfViewerMode.collectAsState(initial = "google_drive")
    val flipAnim by prefs.pdfFlipAnimation.collectAsState(initial = false)

    var showPdfModeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val exportManager = remember { com.example.data.DataExportManager(context) }
    
    // Pick sv file launcher
    val importBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val tempBackupFile = java.io.File(context.cacheDir, "temp_import.sv")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(tempBackupFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    val result = exportManager.importFromSv(tempBackupFile)
                    if (result.isSuccess) {
                        android.widget.Toast.makeText(context, "Import successful! Please restart the application to apply.", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(context, "Import failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                    tempBackupFile.delete()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Error importing: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Save sv file launcher
    val exportBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val tempBackupFile = java.io.File(context.cacheDir, "scholar_vault_export.sv")
                    val result = exportManager.exportToSv(tempBackupFile, com.example.data.DataExportManager.ExportScope.INCLUDE_WALLET)
                    if (result.isSuccess) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            java.io.FileInputStream(tempBackupFile).use { input ->
                                input.copyTo(output)
                            }
                        }
                        android.widget.Toast.makeText(context, "Backup exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        android.widget.Toast.makeText(context, "Export failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                    tempBackupFile.delete()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Error exporting: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Text(
                        text = "Wallet Security",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                    )
                }
                
                item {
                    SettingClickItem(
                        title = "Wallet Security Settings",
                        subtitle = if (isSecurityEnabled) "ON - Secured" else "OFF - Unsecured",
                        onClick = { onNavigateToWalletSecurity() }
                    )
                }
                
                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
                
                item {
                    Text(
                        text = "Appearance & Performance",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingClickItem(
                        title = "App Theme",
                        subtitle = when (themeController.themeMode) {
                            "light" -> "Light Mode"
                            "dark" -> "Dark Mode"
                            else -> "System Default"
                        },
                        onClick = { showThemeDialog = true }
                    )
                }
                
                item {
                    SettingToggleItem(
                        title = "Enable Animations",
                        subtitle = "Disable to improve performance on low-end devices",
                        checked = themeController.animationsEnabled,
                        onCheckedChange = { themeController.toggleAnimations() }
                    )
                }

                item {
                    SettingToggleItem(
                        title = "Reader Page Flip Animations",
                        subtitle = if (flipAnim) "Flip Transition Enabled" else "Flip Transition Disabled",
                        checked = flipAnim,
                        onCheckedChange = { checked ->
                            scope.launch { prefs.setPdfFlipAnimation(checked) }
                        }
                    )
                }
                
                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
                
                item {
                    Text(
                        text = "App Integrations",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
                
                item {
                    SettingClickItem(
                        title = "Default PDF Viewer",
                        subtitle = if (pdfMode == "google_drive") "Google Drive Viewer" else "Built-in Custom Viewer",
                        onClick = { showPdfModeDialog = true }
                    )
                }

                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Text(
                        text = "Storage & Persistence",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingClickItem(
                        title = "Clear Document Cache",
                        subtitle = "Deletes temporary decrypted files and thumbnails",
                        onClick = {
                            scope.launch {
                                val cacheDir = context.cacheDir
                                cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                android.widget.Toast.makeText(context, "Cache cleared successfully", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Text(
                        text = "Data Backup & Recovery",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    SettingClickItem(
                        title = "Export ScholarVault Backup (.sv)",
                        subtitle = "Secures and exports database containing academic & wallet data",
                        onClick = {
                            try {
                                exportBackupLauncher.launch("scholadvault_backup_${System.currentTimeMillis()}.sv")
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Cannot open document creator: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                item {
                    SettingClickItem(
                        title = "Import Backup File (.sv)",
                        subtitle = "Restores database and decrypted documents from a .sv file",
                        onClick = {
                            try {
                                importBackupLauncher.launch("*/*")
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Cannot open document picker: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                item {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Text(
                        text = "About ScholarVault",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "ScholarVault is an offline-first secure document management system designed for students and professionals. It combines high-performance reading with zero-knowledge security.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Core Use Cases:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        BulletPoint("Secure Document Storage with AES-256")
                        BulletPoint("Advanced PDF Reader with Page Flip Animations")
                        BulletPoint("Privacy-Focused Wallet for IDs & Certificates")
                        BulletPoint("High Performance on Low-End Devices")
                        BulletPoint("Fully Offline Functionality")
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("App Version", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("1.0.0 (Build 20260527)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Developed By", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Column(
                            modifier = Modifier
                                .clickable {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://github.com/suvadippatra")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Text(
                                "Suvadip Patra (@suvadippatra)",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            )
                        }
                    }
                }
            }

            if (showThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showThemeDialog = false },
                    title = { Text("Select Theme") },
                    text = {
                        Column {
                            listOf(
                                "system" to "System Default",
                                "light" to "Light Mode",
                                "dark" to "Dark Mode"
                            ).forEach { (mode, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            themeController.setThemeMode(mode)
                                            showThemeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    RadioButton(
                                        selected = (themeController.themeMode == mode),
                                        onClick = {
                                            themeController.setThemeMode(mode)
                                            showThemeDialog = false
                                        }
                                    )
                                    Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            if (showPdfModeDialog) {
                AlertDialog(
                    onDismissRequest = { showPdfModeDialog = false },
                    title = { Text("PDF Viewer Mode") },
                    text = {
                        Column {
                            listOf(
                                "google_drive" to "Google Drive Viewer",
                                "custom" to "Built-in Custom Viewer"
                            ).forEach { (key, label) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { prefs.setPdfViewerMode(key) }
                                            showPdfModeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    RadioButton(
                                        selected = (pdfMode == key),
                                        onClick = {
                                            scope.launch { prefs.setPdfViewerMode(key) }
                                            showPdfModeDialog = false
                                        }
                                    )
                                    Text(label, modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingClickItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(text = subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

