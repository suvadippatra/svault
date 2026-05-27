package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.TopSearchBar
import com.example.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    docViewModel: DocumentViewModel,
    profileViewModel: com.example.ui.viewmodel.ProfileViewModel = viewModel(),
    onOpenDrawer: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentFolderId by remember { mutableStateOf<Int?>(null) }
    val rootFiles by docViewModel.rootFiles.collectAsState()
    val allFolders by docViewModel.allFolders.collectAsState()
    val allNonFolderFiles by docViewModel.allNonFolderFiles.collectAsState()

    val metrics = remember(allNonFolderFiles) {
        val totalBytes = allNonFolderFiles.sumOf { it.sizeBytes }
        val imagesBytes = allNonFolderFiles.filter { it.extension?.lowercase() in listOf("jpg", "png", "jpeg", "webp", "gif") }.sumOf { it.sizeBytes }
        val pdfsBytes = allNonFolderFiles.filter { it.extension?.lowercase() == "pdf" }.sumOf { it.sizeBytes }
        val audioBytes = allNonFolderFiles.filter { it.extension?.lowercase() in listOf("mp3", "wav", "m4a", "ogg") }.sumOf { it.sizeBytes }
        val otherBytes = totalBytes - (imagesBytes + pdfsBytes + audioBytes)
        
        mapOf(
            "total" to totalBytes,
            "images" to imagesBytes,
            "pdfs" to pdfsBytes,
            "audio" to audioBytes,
            "other" to otherBytes
        )
    }

    val currentFolderFilesFlowByGroup = remember(currentFolderId) {
        if (currentFolderId == null) {
            docViewModel.rootFiles
        } else {
            docViewModel.getFilesByFolder(currentFolderId!!)
        }
    }
    val currentFolderFiles by currentFolderFilesFlowByGroup.collectAsState()

    val folderPathChain = remember(currentFolderId, allFolders) {
        val chain = mutableListOf<com.example.data.model.DocumentFile>()
        var curr = allFolders.find { it.id == currentFolderId }
        while (curr != null) {
            chain.add(0, curr)
            curr = curr.parentFolderId?.let { pid -> allFolders.find { it.id == pid } }
        }
        chain
    }

    if (currentFolderId != null) {
        androidx.activity.compose.BackHandler {
            val parentFolder = allFolders.find { it.id == currentFolderId }
            currentFolderId = parentFolder?.parentFolderId
        }
    }

    var isGridView by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showMetricsDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val currentSort by docViewModel.sortOrder.collectAsState()
    
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedFileIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    var showMoveDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagsInputForSelection by remember { mutableStateOf("") }
    
    val searchQuery by profileViewModel.searchQuery.collectAsState()
    
    val scope = rememberCoroutineScope()
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                docViewModel.importFiles(context, uris, currentFolderId)
            }
        }
    }
    
    val singleFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            showImportDialog = true
        }
    }
    
    LaunchedEffect(Unit) {
        docViewModel.triggerUpload.collect {
            singleFilePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            docViewModel.insertFile(
                                context = context,
                                file = com.example.data.model.DocumentFile(
                                    name = newFolderName,
                                    isFolder = true,
                                    parentFolderId = currentFolderId,
                                    filePath = ""
                                )
                            )
                        }
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    var fileToRename by remember { mutableStateOf<com.example.data.model.DocumentFile?>(null) }
    var renameText by remember { mutableStateOf("") }
    
    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    shape = RoundedCornerShape(24.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        docViewModel.updateFile(fileToRename!!.copy(name = renameText))
                        fileToRename = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) { Text("Cancel") }
            }
        )
    }

    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Assign Tags") },
            text = {
                OutlinedTextField(
                    value = tagsInputForSelection,
                    onValueChange = { tagsInputForSelection = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val filesToUpdate = rootFiles.filter { selectedFileIds.contains(it.id) }
                    filesToUpdate.forEach { file ->
                        val currentTags = file.tags
                        val newTags = tagsInputForSelection.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val updatedTags = (currentTags + newTags).distinct()
                        docViewModel.updateFile(file.copy(tags = updatedTags))
                    }
                    tagsInputForSelection = ""
                    showTagDialog = false
                    isSelectionMode = false
                    selectedFileIds = emptySet()
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    if (showMoveDialog) {
        val allFoldersFlow by docViewModel.allFolders.collectAsState()
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move Selected") },
            text = {
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text("Root") },
                            leadingContent = { Icon(Icons.Default.Home, contentDescription = null) },
                            modifier = Modifier.clickable {
                                val filesToMove = currentFolderFiles.filter { selectedFileIds.contains(it.id) }
                                docViewModel.moveFiles(filesToMove, null)
                                showMoveDialog = false
                                isSelectionMode = false
                                selectedFileIds = emptySet()
                            }
                        )
                    }
                    items(allFoldersFlow.size) { index ->
                        val folder = allFoldersFlow[index]
                        // Simple guard to not move a folder into itself or its selection
                        if (!selectedFileIds.contains(folder.id)) {
                            ListItem(
                                headlineContent = { Text(folder.name) },
                                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                                modifier = Modifier.clickable {
                                    val filesToMove = currentFolderFiles.filter { selectedFileIds.contains(it.id) }
                                    docViewModel.moveFiles(filesToMove, folder.id)
                                    showMoveDialog = false
                                    isSelectionMode = false
                                    selectedFileIds = emptySet()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
            }
        )
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    // Tracks whether the wallet category row has scrolled off screen
    var walletRowVisible by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopSearchBar(
            onOpenDrawer = onOpenDrawer, 
            onNavigate = onNavigate,
            searchQuery = searchQuery,
            onSearchChange = { profileViewModel.updateSearchQuery(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header Category Buttons
        val allCategories by docViewModel.allCategories.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = if (isTablet) 32.dp else 16.dp, vertical = 8.dp)
                .onGloballyPositioned { coords ->
                    walletRowVisible = coords.boundsInWindow().top >= 0f
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allCategories.size) { index ->
                    val cat = allCategories[index]
                    CategoryButton(
                        title = cat.title,
                        icon = if (cat.iconName == "badge") Icons.Default.Badge else Icons.Default.Folder,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.width(if (isTablet) 140.dp else 100.dp),
                        onClick = { onNavigate("view_wallet_category/${cat.id}") }
                    )
                }
            }

            CategoryButton(
                title = "View Wallet",
                icon = Icons.Default.AccountBalanceWallet,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.width(if (isTablet) 140.dp else 100.dp),
                onClick = { onNavigate("view_wallet") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { /* View Digilocker */ }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Cloud, contentDescription = "Digilocker", modifier = Modifier.size(20.dp))
                    Text("View Digilocker", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var sortMenuExpanded by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.FilterList, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.example.ui.components.CustomPopupMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        items = listOf(
                            com.example.ui.components.MenuItem("Name A-Z", null) {
                                docViewModel.setSortOrder("Name A-Z")
                            },
                            com.example.ui.components.MenuItem("Date Newest", null) {
                                docViewModel.setSortOrder("Date Newest")
                            },
                            com.example.ui.components.MenuItem("Size", null) {
                                docViewModel.setSortOrder("Size")
                            }
                        )
                    )
                }
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/List",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                var showAddMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showAddMenu = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.example.ui.components.CustomPopupMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        items = listOf(
                            com.example.ui.components.MenuItem("Create Folder", Icons.Default.CreateNewFolder) { showCreateFolderDialog = true },
                            com.example.ui.components.MenuItem("Import File", Icons.Default.FileUpload) { singleFilePickerLauncher.launch(arrayOf("*/*")) }
                        )
                    )
                }
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    com.example.ui.components.CustomPopupMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        items = listOf(
                            com.example.ui.components.MenuItem("Multiselect", Icons.Default.Checklist) {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedFileIds = emptySet()
                            },
                            com.example.ui.components.MenuItem("Create Folder", Icons.Default.CreateNewFolder) { showCreateFolderDialog = true },
                            com.example.ui.components.MenuItem("Import Files", Icons.Default.FileUpload) { singleFilePickerLauncher.launch(arrayOf("*/*")) },
                            com.example.ui.components.MenuItem("Storage Metrics", Icons.Default.PieChart) { showMetricsDialog = true },
                            com.example.ui.components.MenuItem("View Trash", Icons.Default.Delete) { onNavigate("trash") }
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Breadcrumb Navigation
        if (currentFolderId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val parentFolder = allFolders.find { it.id == currentFolderId }
                        currentFolderId = parentFolder?.parentFolderId
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Up",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Root",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { currentFolderId = null },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    folderPathChain.forEach { pathFolder ->
                        Text(
                            text = " / ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Text(
                            text = pathFolder.name,
                            color = if (pathFolder.id == currentFolderId) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { currentFolderId = pathFolder.id },
                            fontSize = 14.sp,
                            fontWeight = if (pathFolder.id == currentFolderId) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        val sortedFiles = remember(currentFolderFiles, allFolders, currentSort, searchQuery) {
            val baseList = if (searchQuery.isNotBlank()) {
                val combined = (currentFolderFiles + allFolders).distinctBy { it.id }
                combined.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                currentFolderFiles
            }
            
            when (currentSort) {
                "Name A-Z" -> baseList.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
                "Date Newest" -> baseList.sortedWith(compareBy<com.example.data.model.DocumentFile>({ !it.isFolder }).thenByDescending { it.createdAt })
                "Size" -> baseList.sortedWith(compareBy<com.example.data.model.DocumentFile>({ !it.isFolder }).thenByDescending { it.sizeBytes })
                else -> baseList.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))
            }
        }
        
        // Files Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (sortedFiles.isEmpty()) {
                Text(
                    "No files yet. Import or create a folder.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (isGridView || isTablet) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isTablet) 5 else 3),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedFiles.size) { index ->
                            val file = sortedFiles[index]
                            DocumentItemGrid(
                                file = file, 
                                onNavigate = onNavigate,
                                onTrashClick = { docViewModel.trashFile(file.id) },
                                onRenameClick = { 
                                    fileToRename = file
                                    renameText = file.name
                                },
                                onDeleteClick = { docViewModel.deleteFile(context, file) },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedFileIds.contains(file.id),
                                onSelectionChange = {
                                    if (it) selectedFileIds += file.id else selectedFileIds -= file.id
                                },
                                onFolderClick = { currentFolderId = it.id }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 140.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedFiles.size) { index ->
                            val file = sortedFiles[index]
                            DocumentItemList(
                                file = file, 
                                onNavigate = onNavigate,
                                onTrashClick = { docViewModel.trashFile(file.id) },
                                onRenameClick = { 
                                    fileToRename = file
                                    renameText = file.name
                                },
                                onDeleteClick = { docViewModel.deleteFile(context, file) },
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedFileIds.contains(file.id),
                                onSelectionChange = {
                                    if (it) selectedFileIds += file.id else selectedFileIds -= file.id
                                },
                                onFolderClick = { currentFolderId = it.id }
                            )
                        }
                    }
                }
            }
            
            // Sticky "View Wallet" pill — appears when wallet row scrolls off screen
            androidx.compose.animation.AnimatedVisibility(
                visible = !walletRowVisible,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(32.dp)
                        .clickable { onNavigate("view_wallet") }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "View Wallet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isSelectionMode,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            val filesToDelete = rootFiles.filter { selectedFileIds.contains(it.id) }
                            filesToDelete.forEach { docViewModel.trashFile(it.id) }
                            selectedFileIds = emptySet()
                            isSelectionMode = false 
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = "Trash", tint = MaterialTheme.colorScheme.error)
                                Text("Trash", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        IconButton(onClick = { if (selectedFileIds.isNotEmpty()) showMoveDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Move", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        
                        IconButton(onClick = { if (selectedFileIds.isNotEmpty()) showTagDialog = true }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Tag", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Tag", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        
                        IconButton(onClick = {
                            selectedFileIds = emptySet()
                            isSelectionMode = false
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Cancel", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ModalBottomSheet(onDismissRequest = { 
            showImportDialog = false 
            selectedUri = null 
        }) {
            var fileNameToImport by remember(selectedUri) { 
                var defaultName = "New_File_Imported"
                if (selectedUri != null) {
                    val cursor = context.contentResolver.query(selectedUri!!, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                defaultName = it.getString(nameIndex).substringBeforeLast(".") // remove extension to just allow user rename the base
                            }
                        }
                    }
                }
                mutableStateOf(defaultName) 
            }
            var selectedType by remember { mutableStateOf("pdf") }
            var tagsInput by remember { mutableStateOf("") }
            
            // Add folder picking
            val allFolders by docViewModel.allFolders.collectAsState()
            var selectedFolder by remember { mutableStateOf<com.example.data.model.DocumentFile?>(null) }
            var folderDropdownExpanded by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Import File", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = fileNameToImport,
                    onValueChange = { fileNameToImport = it },
                    label = { Text("Rename File (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Box {
                    OutlinedTextField(
                        value = selectedFolder?.name ?: "Root Directory",
                        onValueChange = { },
                        label = { Text("Destination Folder") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { folderDropdownExpanded = true },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        shape = RoundedCornerShape(24.dp)
                    )
                    DropdownMenu(
                        expanded = folderDropdownExpanded,
                        onDismissRequest = { folderDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f).shadow(8.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Root Directory") },
                            onClick = {
                                selectedFolder = null
                                folderDropdownExpanded = false
                            }
                        )
                        allFolders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    selectedFolder = folder
                                    folderDropdownExpanded = false
                                }
                            )
                        }
                    }
                    // Overlay to capture clicks across the whole text field area
                    Spacer(modifier = Modifier.matchParentSize().clickable { folderDropdownExpanded = true })
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { 
                        showImportDialog = false 
                        selectedUri = null
                    }) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { 
                        var finalExt = selectedType
                        var finalSize = 0L
                        
                        if (selectedUri != null) {
                            val cursor = context.contentResolver.query(selectedUri!!, null, null, null, null)
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                    if (nameIndex != -1) {
                                        val fullName = it.getString(nameIndex)
                                        finalExt = fullName.substringAfterLast(".", finalExt)
                                    }
                                    if (sizeIndex != -1) {
                                        finalSize = it.getLong(sizeIndex)
                                    }
                                }
                            }
                        }
                        
                        val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        docViewModel.insertFile(
                            context = context,
                            file = com.example.data.model.DocumentFile(
                                name = "$fileNameToImport.$finalExt",
                                isFolder = false,
                                parentFolderId = selectedFolder?.id,
                                filePath = "", // handled by viewmodel
                                extension = finalExt,
                                sizeBytes = finalSize,
                                tags = parsedTags
                            ),
                            uri = selectedUri
                        )
                        showImportDialog = false 
                        selectedUri = null
                    }) { Text("Import") }
                }
            }
        }
    }

    if (showMetricsDialog) {
        AlertDialog(
            onDismissRequest = { showMetricsDialog = false },
            title = { Text("Storage Metrics") },
            text = {
                Column {
                    Text("Total Used: ${formatFileSize(metrics["total"] ?: 0L)}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("• Images: ${formatFileSize(metrics["images"] ?: 0L)}")
                    Text("• PDFs: ${formatFileSize(metrics["pdfs"] ?: 0L)}")
                    Text("• Audio: ${formatFileSize(metrics["audio"] ?: 0L)}")
                    Text("• Other: ${formatFileSize(metrics["other"] ?: 0L)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showMetricsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun CategoryButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val contentColor = androidx.compose.material3.contentColorFor(color)
            Icon(imageVector = icon, contentDescription = title, tint = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun DocumentItemGrid(
    file: com.example.data.model.DocumentFile,
    onNavigate: (String) -> Unit,
    onTrashClick: () -> Unit,
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    onFolderClick: (com.example.data.model.DocumentFile) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = Modifier.clickable {
            if (isSelectionMode) {
                onSelectionChange(!isSelected)
            } else {
                if (file.isFolder) {
                    onFolderClick(file)
                } else {
                    val mappedType = when (file.extension?.lowercase()) {
                        "pdf" -> "pdf"
                        "mp4", "mkv", "avi", "webm" -> "video"
                        "mp3", "wav", "m4a", "ogg" -> "audio"
                        "jpg", "png", "jpeg", "webp", "gif" -> "image"
                        else -> "text"
                    }
                    onNavigate(Screen.Viewer.createRoute(mappedType, file.filePath, file.name))
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp).fillMaxWidth()
            ) {
                val icon = if (file.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
                val tint = if (file.isFolder) Color(0xFFFFCA28) else MaterialTheme.colorScheme.primary
                Box(
                    modifier = Modifier.size(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(36.dp), tint = tint)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = file.name,
                    fontSize = 12.sp,
                    maxLines = if (file.isFolder) 2 else 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!file.isFolder) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatFileSize(file.sizeBytes),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange(it) },
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
            }
            
            // Context menu in top right
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                }
                com.example.ui.components.CustomPopupMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = if (!file.isFolder) {
                        listOf(
                            com.example.ui.components.MenuItem("Rename", Icons.Default.Edit, onClick = { onRenameClick() }),
                            com.example.ui.components.MenuItem("Trash", Icons.Default.Delete, isDestructive = true, onClick = { onTrashClick() })
                        )
                    } else {
                        listOf(
                            com.example.ui.components.MenuItem("Rename", Icons.Default.Edit, onClick = { onRenameClick() }),
                            com.example.ui.components.MenuItem("Delete", Icons.Default.DeleteForever, isDestructive = true, onClick = { onDeleteClick() })
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun DocumentItemList(
    file: com.example.data.model.DocumentFile,
    onNavigate: (String) -> Unit,
    onTrashClick: () -> Unit,
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    onFolderClick: (com.example.data.model.DocumentFile) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { 
                if (isSelectionMode) {
                    onSelectionChange(!isSelected)
                } else {
                    if (file.isFolder) {
                        onFolderClick(file)
                    } else {
                        val mappedType = when (file.extension?.lowercase()) {
                            "pdf" -> "pdf"
                            "mp4", "mkv", "avi", "webm" -> "video"
                            "mp3", "wav", "m4a", "ogg" -> "audio"
                            "jpg", "png", "jpeg", "webp", "gif" -> "image"
                            else -> "text"
                        }
                        onNavigate(Screen.Viewer.createRoute(mappedType, file.filePath, file.name))
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange(it) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            val icon = if (file.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
            val tint = if (file.isFolder) Color(0xFFFFCA28) else MaterialTheme.colorScheme.primary
            
            Box(
                modifier = Modifier.size(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = tint)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(file.createdAt)
                Text(
                    text = if (file.isFolder) "Folder • $dateStr" else "${file.extension?.uppercase() ?: "FILE"} • ${formatFileSize(file.sizeBytes)} • $dateStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                }
                com.example.ui.components.CustomPopupMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    items = if (!file.isFolder) {
                        listOf(
                            com.example.ui.components.MenuItem("Rename", Icons.Default.Edit, onClick = { onRenameClick() }),
                            com.example.ui.components.MenuItem("Trash", Icons.Default.Delete, isDestructive = true, onClick = { onTrashClick() })
                        )
                    } else {
                        listOf(
                            com.example.ui.components.MenuItem("Rename", Icons.Default.Edit, onClick = { onRenameClick() }),
                            com.example.ui.components.MenuItem("Delete", Icons.Default.DeleteForever, isDestructive = true, onClick = { onDeleteClick() })
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun TrashScreen(
    docViewModel: com.example.ui.viewmodel.DocumentViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val trashedFiles by docViewModel.trashedFiles.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        com.example.ui.components.TopSearchBar(
            onOpenDrawer = onBack,
            isBackButton = true,
            showProfileIcon = false,
            searchQuery = "Trash"
        )
        if (trashedFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Trash is empty.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(trashedFiles.size) { index ->
                    val file = trashedFiles[index]
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = file.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (file.isFolder) "Folder" else "File", 
                                    fontSize = 12.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { docViewModel.restoreFile(file.id) }) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore")
                            }
                            IconButton(onClick = { docViewModel.deleteFile(context, file) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.toDouble())).toInt().coerceIn(0, units.size - 1)
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}
