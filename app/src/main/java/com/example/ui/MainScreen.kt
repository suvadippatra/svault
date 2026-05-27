// Path: app/src/main/java/com/example/ui/MainScreen.kt
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.LocalThemeController
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.DocumentViewModel
import com.example.ui.viewmodel.DashboardViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Academics : Screen("academics", "Academics", Icons.Default.School)
    object Documents : Screen("documents", "Documents", Icons.Default.Folder)
    object ViewWallet : Screen("view_wallet", "Wallet", Icons.Default.AccountBalanceWallet)
    object Trash : Screen("trash", "Trash", Icons.Default.Delete)
    object WalletCardDetail : Screen("wallet_card/{cardId}", "Card Detail", Icons.Default.CreditCard) {
        fun createRoute(cardId: Int) = "wallet_card/$cardId"
    }
    object EditWalletCard : Screen("edit_wallet_card/{categoryId}/{cardId}", "Edit Card", Icons.Default.Edit) {
        fun createRoute(categoryId: Int, cardId: Int = -1) = "edit_wallet_card/$categoryId/$cardId"
    }
    object Tools : Screen("tools", "Tools", Icons.Default.Build)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object AcademicDetail : Screen("academic_detail/{id}", "Academic Detail", Icons.Default.School) {
        fun createRoute(id: String) = "academic_detail/$id"
    }
    object AddAcademicItem : Screen("add_academic_item/{category}", "Add Academic", Icons.Default.Add) {
        fun createRoute(category: String) = "add_academic_item/$category"
    }
    object EditAcademicItem : Screen("edit_academic_item/{id}", "Edit Academic", Icons.Default.Edit) {
        fun createRoute(id: String) = "edit_academic_item/$id"
    }
    object Viewer : Screen("viewer/{fileType}/{filePath}/{fileName}", "Viewer", Icons.Default.Fullscreen) {
        fun createRoute(fileType: String, filePath: String, fileName: String): String {
            val encodedPath = android.util.Base64.encodeToString((if (filePath.isEmpty()) "empty" else filePath).toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val encodedName = android.util.Base64.encodeToString(fileName.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            return "viewer/$fileType/$encodedPath/$encodedName"
        }
    }
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object FocusTimer : Screen("focus_timer", "Focus Timer", Icons.Default.Timer)
    object CgpaCalculator : Screen("cgpa_calculator", "CGPA Calculator", Icons.Default.Calculate)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.List)
    object Reminders : Screen("reminders", "Reminders", Icons.Default.Notifications)
    object About : Screen("about", "About", Icons.Default.Info)
    object PdfReaderNav : Screen("pdf_reader_nav", "PDF Reader", Icons.Default.PictureAsPdf)
    object InteractivePdfViewer : Screen("interactive_pdf_viewer/{filePath}", "Interactive Reader", Icons.Default.Fullscreen) {
        fun createRoute(filePath: String): String {
            val encodedPath = android.util.Base64.encodeToString(filePath.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            return "interactive_pdf_viewer/$encodedPath"
        }
    }
    object DocumentScanner : Screen("document_scanner", "Document Scanner", Icons.Default.DocumentScanner)
}

val BottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Academics,
    Screen.Documents,
    Screen.Tools
)

@Composable
fun MainScreen(viewModel: AcademicViewModel, docViewModel: DocumentViewModel) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var isFabMenuOpen by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    val isDark = LocalThemeController.current.isDarkTheme
    val sidebarBgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD9D9D9)
    
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600
    val isWideScreen = configuration.screenWidthDp > 840

    val drawerContentBlock: @Composable () -> Unit = {
        if (isTablet) {
            PermanentDrawerSheet(
                modifier = Modifier.width(if (isWideScreen) 320.dp else 280.dp),
                drawerContainerColor = sidebarBgColor,
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
            ) {
                SidebarContent(
                    onClose = {},
                    onProfileClick = { showProfileMenu = true },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        } else {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = sidebarBgColor,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                SidebarContent(
                    onClose = { scope.launch { drawerState.close() } },
                    onProfileClick = { showProfileMenu = true },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    }

    val screenContent: @Composable () -> Unit = {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val isBottomBarVisible = currentRoute in listOf(
            Screen.Dashboard.route,
            Screen.Academics.route,
            Screen.Documents.route,
            Screen.Tools.route
        )
        
        val scaffoldBgColor = if (isDark) Color(0xFF121212) else Color(0xFFFCFCFC)
        val navBgColor = if (isDark) Color(0xFF2C2C30) else Color(0xFFDCD5E4)
        
        Scaffold(
            containerColor = scaffoldBgColor
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isBottomBarVisible && !isTablet) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .align(Alignment.BottomCenter)
                            .background(navBgColor)
                    )
                }
                
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    val animationsEnabled = LocalThemeController.current.animationsEnabled
                    val animationDuration = if (isTablet) 300 else 400
                    
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        enterTransition = {
                            if (animationsEnabled) {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(animationDuration)) +
                                androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(animationDuration))
                            } else androidx.compose.animation.EnterTransition.None
                        },
                        exitTransition = {
                            if (animationsEnabled) {
                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(animationDuration))
                            } else androidx.compose.animation.ExitTransition.None
                        },
                        popEnterTransition = {
                            if (animationsEnabled) {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(animationDuration)) +
                                androidx.compose.animation.scaleIn(initialScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(animationDuration))
                            } else androidx.compose.animation.EnterTransition.None
                        },
                        popExitTransition = {
                            if (animationsEnabled) {
                                androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(animationDuration))
                            } else androidx.compose.animation.ExitTransition.None
                        }
                    ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            onOpenDrawer = { 
                                if (!isTablet) scope.launch { drawerState.open() } 
                            },
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = dashboardViewModel
                        )
                    }
                    composable("ids_list") {
                        IdsListScreen(onBack = { navController.popBackStack() }, docViewModel = docViewModel)
                    }
                    composable(Screen.Academics.route) {
                        AcademicsScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { 
                                if (!isTablet) scope.launch { drawerState.open() } 
                            },
                            onNavigate = { route -> navController.navigate(route) },
                            onNavigateToDetail = { id -> navController.navigate(Screen.AcademicDetail.createRoute(id)) },
                            onNavigateToAddItem = { cat -> navController.navigate(Screen.AddAcademicItem.createRoute(cat)) },
                            onNavigateToIdsList = { navController.navigate("ids_list") }
                        )
                    }
                    composable(Screen.AddAcademicItem.route) { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category") ?: "Skill-Based"
                        val context = androidx.compose.ui.platform.LocalContext.current
                        com.example.ui.components.AddAcademicItemScreen(
                            category = category,
                            docViewModel = docViewModel,
                            onBack = { navController.popBackStack() },
                            onSave = { item, semesters, externalUri, externalName, walletDocId -> 
                                scope.launch {
                                    viewModel.insertAcademicItem(item)
                                    semesters.forEach { viewModel.insertSemester(it) }
                                    
                                    var finalDocId = walletDocId
                                    if (externalUri != null && externalName != null) {
                                        var externalSize = 0L
                                        context.contentResolver.query(externalUri, null, null, null, null)?.use { cursor ->
                                            if (cursor.moveToFirst()) {
                                                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                                if (sizeIndex != -1) {
                                                    externalSize = cursor.getLong(sizeIndex)
                                                }
                                            }
                                        }
                                        val docFileToSave = com.example.data.model.DocumentFile(
                                            name = externalName,
                                            isFolder = false,
                                            parentFolderId = null,
                                            filePath = "",
                                            extension = externalName.substringAfterLast(".", ""),
                                            sizeBytes = externalSize,
                                            isEncrypted = true,
                                            tags = listOf("wallet")
                                        )
                                        val newDocId = docViewModel.insertAttachmentFile(context, docFileToSave, externalUri)
                                        if (newDocId != -1L) {
                                            finalDocId = newDocId.toInt()
                                        }
                                    }
                                    
                                    if (finalDocId != null) {
                                        viewModel.insertAcademicDocumentLink(
                                            com.example.data.model.AcademicDocumentLink(
                                                academicItemId = item.id,
                                                walletDocumentId = finalDocId.toString(),
                                                linkLabel = "Attached Document"
                                            )
                                        )
                                    }
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                    composable(Screen.AcademicDetail.route) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        AcademicDetailScreen(
                            itemId = id, 
                            viewModel = viewModel, 
                            docViewModel = docViewModel,
                            onNavigateToEdit = { editId ->
                                navController.navigate(Screen.EditAcademicItem.createRoute(editId))
                            },
                            onNavigateToViewer = { route -> navController.navigate(route) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.EditAcademicItem.route) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: ""
                        val courseState by viewModel.getCourseWithSemesters(id).collectAsState()
                        val item = courseState?.course
                        val semesters = courseState?.semesters ?: emptyList()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        if (item != null) {
                            com.example.ui.components.EditAcademicItemScreen(
                                item = item,
                                semesters = semesters,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onSave = { updatedItem, updatedSemesters, extUri, extName, existingDocId ->
                                    scope.launch {
                                        viewModel.insertAcademicItem(updatedItem)
                                        // Simple sync: delete old and insert new or rely on Room to handle if IDs match.
                                        // For safety with duration changes, clear first.
                                        viewModel.deleteSemestersForCourse(updatedItem.id)
                                        updatedSemesters.forEach { viewModel.insertSemester(it) }
                                        
                                        var finalDocId = existingDocId
                                        if (extUri != null && extName != null) {
                                            // Handle attachment update logic if needed
                                            val docFileToSave = com.example.data.model.DocumentFile(
                                                name = extName,
                                                isFolder = false,
                                                parentFolderId = null,
                                                filePath = "",
                                                extension = extName.substringAfterLast(".", ""),
                                                sizeBytes = 0, // Should measure
                                                isEncrypted = true,
                                                tags = listOf("wallet")
                                            )
                                            val newDocId = docViewModel.insertAttachmentFile(context, docFileToSave, extUri)
                                            if (newDocId != -1L) finalDocId = newDocId.toInt()
                                        }
                                        
                                        if (finalDocId != null) {
                                            viewModel.insertAcademicDocumentLink(
                                                com.example.data.model.AcademicDocumentLink(
                                                    academicItemId = updatedItem.id,
                                                    walletDocumentId = finalDocId.toString(),
                                                    linkLabel = "Updated Document"
                                                )
                                            )
                                        }
                                        navController.popBackStack()
                                    }
                                }
                            )
                        } else {
                            // loading or error state, fallback
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    composable(Screen.Documents.route) {
                        DocumentsScreen(
                            docViewModel = docViewModel,
                            onOpenDrawer = { 
                                if (!isTablet) scope.launch { drawerState.open() } 
                            },
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }
                    composable(Screen.Trash.route) {
                        TrashScreen(
                            docViewModel = docViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.ViewWallet.route) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.example.security.WalletSecurityManager(context) }
                        com.example.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            ViewWalletScreen(
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToCategory = { catId -> navController.navigate("view_wallet_category/$catId") },
                                onNavigateToCard = { cardId -> navController.navigate(Screen.WalletCardDetail.createRoute(cardId)) }
                            )
                        }
                    }
                    composable("view_wallet_category/{categoryId}") { backStackEntry ->
                        val catIdStr = backStackEntry.arguments?.getString("categoryId") ?: "-1"
                        val categoryId = catIdStr.toIntOrNull() ?: -1
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.example.security.WalletSecurityManager(context) }
                        com.example.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            WalletCategoryScreen(
                                categoryId = categoryId,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onNavigateToCard = { cardId -> navController.navigate(Screen.WalletCardDetail.createRoute(cardId)) },
                                onAddCard = { catId -> navController.navigate(Screen.EditWalletCard.createRoute(catId, -1)) }
                            )
                        }
                    }
                    composable(Screen.WalletCardDetail.route) { backStackEntry ->
                        val cardIdStr = backStackEntry.arguments?.getString("cardId") ?: "-1"
                        val cardId = cardIdStr.toIntOrNull() ?: -1
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.example.security.WalletSecurityManager(context) }
                        com.example.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            WalletCardDetailScreen(
                                cardId = cardId,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() },
                                onEdit = { cId, catId -> navController.navigate(Screen.EditWalletCard.createRoute(catId, cId)) },
                                onNavigateToViewer = { route -> navController.navigate(route) }
                            )
                        }
                    }
                    composable(Screen.EditWalletCard.route) { backStackEntry ->
                        val catIdStr = backStackEntry.arguments?.getString("categoryId") ?: "-1"
                        val cardIdStr = backStackEntry.arguments?.getString("cardId") ?: "-1"
                        val categoryId = catIdStr.toIntOrNull() ?: -1
                        val cardId = cardIdStr.toIntOrNull() ?: -1
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val securityManager = remember { com.example.security.WalletSecurityManager(context) }
                        com.example.security.WalletAuthenticationWrapper(
                            securityManager = securityManager,
                            onUnlockSuccess = {},
                            onCancel = { navController.popBackStack() }
                        ) {
                            EditWalletCardScreen(
                                categoryId = categoryId,
                                cardId = cardId,
                                docViewModel = docViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(Screen.Viewer.route) { backStackEntry ->
                        val fileType = backStackEntry.arguments?.getString("fileType") ?: "text"
                        val filePathEnc = backStackEntry.arguments?.getString("filePath") ?: "empty"
                        val fileNameEnc = backStackEntry.arguments?.getString("fileName") ?: "Unknown"
                        
                        val filePath = try {
                            String(android.util.Base64.decode(filePathEnc, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                        } catch (e: Exception) { "empty" }
                        
                        val fileName = try {
                            String(android.util.Base64.decode(fileNameEnc, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                        } catch (e: Exception) { "Unknown" }

                        ViewerScreen(
                            fileType = fileType,
                            filePath = if (filePath == "empty") "" else filePath,
                            fileName = fileName,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToWalletSecurity = { navController.navigate("wallet_security") }
                        )
                    }
                    composable("wallet_security") {
                        com.example.ui.WalletSecurityScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.FocusTimer.route) {
                        FocusTimerScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.CgpaCalculator.route) {
                        CgpaCalculatorScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Tasks.route) {
                        TasksScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = dashboardViewModel
                        )
                    }
                    composable(Screen.Reminders.route) {
                        RemindersScreen(
                            onBack = { navController.popBackStack() },
                            onNavigate = { route -> navController.navigate(route) },
                            viewModel = dashboardViewModel
                        )
                    }
                    composable(Screen.Tools.route) {
                        ToolsScreen(
                            onOpenDrawer = { 
                                if (!isTablet) scope.launch { drawerState.open() } 
                            },
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }
                    composable(Screen.DocumentScanner.route) {
                        com.example.ui.tools.scanner.DocumentScannerScreen(
                            onBack = { navController.popBackStack() },
                            onImageCaptured = { file -> 
                                // navigate to next step or handle accordingly
                            }
                        )
                    }
                    composable(Screen.PdfReaderNav.route) {
                        com.example.ui.tools.pdfreader.PdfFolderScreen(
                            onBack = { navController.popBackStack() },
                            onOpenPdf = { path ->
                                navController.navigate(Screen.InteractivePdfViewer.createRoute(path))
                            }
                        )
                    }
                    composable(Screen.InteractivePdfViewer.route) { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("filePath") ?: "empty"
                        var filePath = ""
                        try {
                            filePath = String(android.util.Base64.decode(encodedPath, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))
                        } catch (e: Exception) {}
                        
                        com.example.ui.tools.pdfreader.PdfReaderScreen(
                            filePath = filePath,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.Profile.route) {
                        com.example.ui.ProfileScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.About.route) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (!isTablet) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        CustomBottomNavigationBar(
                            navController = navController,
                            isFabMenuOpen = isFabMenuOpen,
                            onToggleFabMenu = { isFabMenuOpen = !isFabMenuOpen },
                            onNavigate = { isFabMenuOpen = false }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isFabMenuOpen,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(
                        initialScale = 0.8f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    ),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(
                        targetScale = 0.8f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    androidx.activity.compose.BackHandler { isFabMenuOpen = false }
                    FabMenuOverlay(
                        onClose = { isFabMenuOpen = false },
                        onNavigate = { route -> navController.navigate(route) },
                        onUploadClick = { 
                            navController.navigate(Screen.Documents.route)
                            scope.launch { docViewModel.triggerUpload.emit(Unit) }
                        },
                        paddingBottom = 80.dp
                    )
                }

                if (!isTablet) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        TopLevelFab(
                            isFabMenuOpen = isFabMenuOpen,
                            onToggleFabMenu = { isFabMenuOpen = !isFabMenuOpen }
                        )
                    }
                } else {
                    // Desktop FAB - Bottom Right
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isBottomBarVisible,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { isFabMenuOpen = !isFabMenuOpen },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isFabMenuOpen) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Menu",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            } // Close inner Box
            } // Close outer Box
        } // Close Scaffold
    } // Close screenContent lambda

    Box(modifier = Modifier.fillMaxSize()) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isDrawerGesturesEnabled = currentRoute?.startsWith("viewer/") != true

        if (isTablet) {
            PermanentNavigationDrawer(
                drawerContent = drawerContentBlock,
                content = screenContent
            )
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = drawerContentBlock,
                gesturesEnabled = isDrawerGesturesEnabled,
                content = screenContent
            )
        }

        if (showProfileMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { showProfileMenu = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier.width(340.dp).clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "SP",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Suvadip Patra",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "joysuvapatra@gmail.com",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Menu Item 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfileMenu = false }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Account Settings", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        // Menu Item 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    showProfileMenu = false
                                    navController.navigate(Screen.About.route)
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("About App", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Developer Link
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    showProfileMenu = false
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://github.com/suvadippatra")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Developer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Developer: @suvadippatra", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { showProfileMenu = false },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Close", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(navController: NavHostController, isFabMenuOpen: Boolean, onToggleFabMenu: () -> Unit, onNavigate: () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = LocalThemeController.current.isDarkTheme
    
    var rotationTarget by remember { mutableStateOf(0f) }
    var previousIsFabMenuOpen by remember { mutableStateOf(isFabMenuOpen) }
    
    LaunchedEffect(isFabMenuOpen) {
        if (isFabMenuOpen != previousIsFabMenuOpen) {
            rotationTarget += 45f
            previousIsFabMenuOpen = isFabMenuOpen
        }
    }

    val navBgColor = if (isDark) Color(0xFF2C2C30) else Color(0xFFDCD5E4)
    val fabIconColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF4A148C)

    val density = androidx.compose.ui.platform.LocalDensity.current
    val fabRadius = 27f * density.density
    val gap = 6f * density.density
    val cutoutRadius = fabRadius + gap
    val fabCenterY = 0f // FAB center relative to navBar top

    val bottomNavShape = remember(density) {
        object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density
            ): androidx.compose.ui.graphics.Outline {
                if (size.width <= 0f || size.height <= 0f) {
                    return androidx.compose.ui.graphics.Outline.Generic(androidx.compose.ui.graphics.Path())
                }
                
                val path = androidx.compose.ui.graphics.Path().apply {
                    val cornerRadius = kotlin.math.min(24f * density.density, size.height / 2f)
                    val center = size.width / 2f
                    
                    moveTo(0f, cornerRadius)
                    quadraticTo(0f, 0f, cornerRadius, 0f)
                    
                    val dx = if (cutoutRadius > fabCenterY && cutoutRadius > 0f) {
                         val inner = (cutoutRadius * cutoutRadius) - (fabCenterY * fabCenterY)
                         if (inner > 0f) kotlin.math.sqrt(inner) else 0f
                    } else {
                         0f
                    }
                    
                    // Safe guard for layout width
                    if (center - dx > cornerRadius) {
                        lineTo(center - dx, 0f)
                    } else {
                        lineTo(cornerRadius, 0f) // fallback to simple shape
                    }
                    
                    if (cutoutRadius > 0f) {
                        val angleRad = kotlin.math.asin((fabCenterY / cutoutRadius).toDouble().coerceIn(-1.0, 1.0))
                        val angleDeg = Math.toDegrees(angleRad).toFloat()
                        val startAngle = 180f + angleDeg
                        val sweepAngle = -(180f + 2f * angleDeg)
                        
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                left = center - cutoutRadius,
                                top = fabCenterY - cutoutRadius,
                                right = center + cutoutRadius,
                                bottom = fabCenterY + cutoutRadius
                            ),
                            startAngleDegrees = startAngle,
                            sweepAngleDegrees = sweepAngle,
                            forceMoveTo = false
                        )
                    }
                    
                    if (center + dx < size.width - cornerRadius) {
                        lineTo(center + dx, 0f)
                    } else {
                        lineTo(size.width - cornerRadius, 0f)
                    }
                    
                    lineTo(size.width - cornerRadius, 0f)
                    quadraticTo(size.width, 0f, size.width, cornerRadius)
                    
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                return androidx.compose.ui.graphics.Outline.Generic(path)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // The background navigation bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(navBgColor, bottomNavShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NavBarItem(Screen.Dashboard, currentRoute, navController, onNavigate)
                        NavBarItem(Screen.Academics, currentRoute, navController, onNavigate)
                    }
                    
                    Spacer(modifier = Modifier.width(60.dp)) // Space for FAB
    
                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NavBarItem(Screen.Documents, currentRoute, navController, onNavigate)
                        NavBarItem(Screen.Tools, currentRoute, navController, onNavigate)
                    }
                }
            }
        }
    }
}

@Composable
fun TopLevelFab(isFabMenuOpen: Boolean, onToggleFabMenu: () -> Unit) {
    var rotationTarget by remember { mutableStateOf(0f) }
    var previousIsFabMenuOpen by remember { mutableStateOf(isFabMenuOpen) }
    
    LaunchedEffect(isFabMenuOpen) {
        if (isFabMenuOpen != previousIsFabMenuOpen) {
            rotationTarget += 45f
            previousIsFabMenuOpen = isFabMenuOpen
        }
    }

    Box(
        modifier = Modifier
            .padding(bottom = 80.dp - 27.dp)
            .size(54.dp),
        contentAlignment = Alignment.Center
    ) {
        val rotation by androidx.compose.animation.core.animateFloatAsState(
            targetValue = rotationTarget, 
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
        )
        FloatingActionButton(
            onClick = onToggleFabMenu,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxSize(),
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Menu",
                modifier = Modifier.size(32.dp).rotate(rotation)
            )
        }
    }
}

@Composable
fun NavBarItem(screen: Screen, currentRoute: String?, navController: NavHostController, onNavigate: () -> Unit) {
    val selected = currentRoute == screen.route
    val isDark = LocalThemeController.current.isDarkTheme
    val selectedColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF311B92)
    val unselectedColor = if (isDark) Color(0xFFAAAAAA) else Color.DarkGray
    val color = if (selected) selectedColor else unselectedColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = androidx.compose.ui.Modifier
            .clickable {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onNavigate()
            }
            .padding(8.dp)
    ) {
        Icon(screen.icon, contentDescription = screen.title, tint = color)
        Text(screen.title, fontSize = 10.sp, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun FabMenuOverlay(onClose: () -> Unit, onNavigate: (String) -> Unit, onUploadClick: () -> Unit = {}, paddingBottom: Dp) {
    val context = LocalContext.current
    val isDark = LocalThemeController.current.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val overlayBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFDCDCDC)
    val itemBg = if (isDark) Color(0xFF2C2C2E) else Color.White
    val bgAlpha = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)

    var isEditingLayout by remember { mutableStateOf(false) }

    val key = "fab_menu_order"
    val defaultItemsList = remember {
        listOf(
            "Upload" to (Icons.Default.CloudUpload to { onUploadClick() }),
            "Wallet" to (Icons.Default.AccountBalanceWallet to { onNavigate(Screen.ViewWallet.route) }),
            "Transaction" to (Icons.Default.CurrencyExchange to {}),
            "Notes" to (Icons.Default.Description to {}),
            "Capture" to (Icons.Default.DocumentScanner to { onNavigate(Screen.DocumentScanner.route) }),
            "Record" to (Icons.Default.Mic to {})
        )
    }

    var itemsOrder by remember {
        val vault = com.example.util.SecurityVault(context)
        val orderStr = vault.getSecureString(key) ?: "Upload,Wallet,Transaction,Notes,Capture,Record"
        val list = mutableListOf<Pair<String, Pair<ImageVector, () -> Unit>>>()
        val saved = orderStr.split(",")
        saved.forEach { name ->
            val found = defaultItemsList.find { it.first == name }
            if (found != null) {
                list.add(found)
            }
        }
        // Fallback for missing/corrupted
        defaultItemsList.forEach { def ->
            if (list.none { it.first == def.first }) {
                list.add(def)
            }
        }
        mutableStateOf(list)
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Save ordering helper
    val saveItemsOrder: (List<Pair<String, Pair<ImageVector, () -> Unit>>>) -> Unit = { list ->
        val names = list.map { it.first }.joinToString(",")
        val vault = com.example.util.SecurityVault(context)
        vault.saveSecureString(key, names)
    }

    // Capture coordinates in overlay coordinate space
    var overlayContainerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemCoords = remember { mutableMapOf<Int, Rect>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgAlpha)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { if (!isEditingLayout) onClose() }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = paddingBottom + 40.dp) // Lift above FAB
                .padding(horizontal = 16.dp)
                .wrapContentHeight() // Make it shrink to content
                .background(overlayBg, RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .onGloballyPositioned { overlayContainerCoordinates = it }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (!isEditingLayout) onClose()
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = itemBg, contentColor = textColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(0.dp)
                ) { Text("Back", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor) }
                Button(
                    onClick = {
                        isEditingLayout = !isEditingLayout
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEditingLayout) MaterialTheme.colorScheme.primary else itemBg,
                        contentColor = if (isEditingLayout) MaterialTheme.colorScheme.onPrimary else textColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(0.dp)
                ) { 
                    Text(
                        if (isEditingLayout) "Done" else "Edit Layout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isEditingLayout) MaterialTheme.colorScheme.onPrimary else textColor
                    ) 
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isEditingLayout) {
                Text(
                    text = "Press & drag any item to rearrange layout",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chunked = itemsOrder.chunked(3)
                chunked.forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEachIndexed { colIndex, item ->
                            val flatIndex = rowIndex * 3 + colIndex
                            val isItemDragged = draggedIndex == flatIndex

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coords ->
                                        val parent = overlayContainerCoordinates
                                        if (parent != null && parent.isAttached && coords.isAttached) {
                                            val localOffset = parent.localPositionOf(coords, Offset.Zero)
                                            val size = coords.size
                                            itemCoords[flatIndex] = Rect(localOffset, Size(size.width.toFloat(), size.height.toFloat()))
                                        }
                                    }
                                    .graphicsLayer {
                                        if (isItemDragged) {
                                            translationX = dragOffset.x
                                            translationY = dragOffset.y
                                            shadowElevation = 8.dp.toPx()
                                            scaleX = 1.1f
                                            scaleY = 1.1f
                                            alpha = 0.9f
                                        }
                                    }
                                    .pointerInput(isEditingLayout, flatIndex) {
                                        if (!isEditingLayout) return@pointerInput
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedIndex = flatIndex
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount

                                                // Find if our dragged center is over another item
                                                val currentRect = itemCoords[flatIndex]
                                                if (currentRect != null) {
                                                    val currentCenter = currentRect.center + dragOffset
                                                    var targetIdx: Int? = null
                                                    for ((idx, rect) in itemCoords) {
                                                        if (idx != flatIndex && rect.contains(currentCenter)) {
                                                            targetIdx = idx
                                                            break
                                                        }
                                                    }
                                                    if (targetIdx != null && targetIdx < itemsOrder.size) {
                                                        val newList = itemsOrder.toMutableList()
                                                        val temp = newList[flatIndex]
                                                        newList[flatIndex] = newList[targetIdx]
                                                        newList[targetIdx] = temp
                                                        itemsOrder = newList
                                                        saveItemsOrder(newList)
                                                        draggedIndex = targetIdx
                                                        dragOffset = Offset.Zero
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragOffset = Offset.Zero
                                            }
                                        )
                                    }
                            ) {
                                FabMenuItem(
                                    title = item.first,
                                    icon = item.second.first,
                                    bg = itemBg,
                                    tc = textColor,
                                    isEditingLayout = isEditingLayout,
                                    isDragged = isItemDragged,
                                    onClick = {
                                        if (!isEditingLayout) {
                                            onClose()
                                            item.second.second()
                                        }
                                    }
                                )
                            }
                        }
                        if (rowItems.size < 3) {
                            for (k in 0 until (3 - rowItems.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FabMenuItem(
    title: String,
    icon: ImageVector,
    bg: Color,
    tc: Color,
    modifier: Modifier = Modifier,
    isEditingLayout: Boolean = false,
    isDragged: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isDark = LocalThemeController.current.isDarkTheme
    val borderStroke = if (isEditingLayout) {
        BorderStroke(1.dp, if (isDragged) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f))
    } else {
        null
    }
    
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditingLayout) { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = tc)
                if (isEditingLayout) {
                    Box(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-12).dp)
                            .size(14.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = tc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SidebarContent(onClose: () -> Unit, onProfileClick: () -> Unit, onNavigate: (String) -> Unit, profileViewModel: ProfileViewModel = viewModel()) {
    val isDark = LocalThemeController.current.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val iconColor = if (isDark) Color.White else Color.Black
    val sbBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFD1D1D1)
    
    val profileFlow by profileViewModel.profileStream.collectAsState()
    val profile = profileFlow?.profile
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sbBg)
            .padding(vertical = 16.dp)
    ) {
        // --- PINNED HEADER ---
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomEnd = 16.dp, topEnd = 16.dp))
                        .background(if (isDark) Color(0xFF4A4A4A) else Color(0xFFD0C3B5))
                        .padding(12.dp)
                        .clickable { onClose() }
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Close", tint = iconColor, modifier = Modifier.size(28.dp))
                }
                val themeController = LocalThemeController.current
                IconButton(
                    onClick = { 
                        themeController.setThemeMode(if (isDark) "light" else "dark")
                    },
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF333333) else Color.White, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        if (isDark) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                        contentDescription = "Theme",
                        tint = if(isDark) Color.Yellow else Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(if (isDark) Color(0xFF333333) else Color.Black, CircleShape)
                        .clip(CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profile?.profilePicUri != null) {
                        CachedImage(uri = Uri.parse(profile.profilePicUri))
                    } else {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(70.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val fullName = listOf(profile?.firstName, profile?.middleName, profile?.lastName).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
                Text(if(fullName.isNotBlank()) fullName else "User Profile", fontSize = 22.sp, fontWeight = FontWeight.Normal, color = textColor)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- SCROLLABLE LIST ---
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            DrawerItem("View Profile") { onNavigate(Screen.Profile.route) }
            DrawerItem("Dashboard") { onNavigate(Screen.Dashboard.route) }
            DrawerItem("Academics") { onNavigate(Screen.Academics.route) }
            DrawerItem("Documents") { onNavigate(Screen.Documents.route) }
            DrawerItem("Tools") { onNavigate(Screen.Tools.route) }
            DrawerItem("About App") { onNavigate(Screen.About.route) }
            DrawerItem("Exam Notifications") { }

            Spacer(modifier = Modifier.height(24.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            DrawerItem("Developer: @suvadippatra") { 
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://github.com/suvadippatra")
                    )
                    context.startActivity(intent)
                } catch (e: Exception) {}
            }
            DrawerItem("Settings") { onNavigate(Screen.Settings.route) }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DrawerItem(title: String, onClick: () -> Unit) {
    val isDark = LocalThemeController.current.isDarkTheme
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE0E0E0)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = textColor)
        }
    }
}




