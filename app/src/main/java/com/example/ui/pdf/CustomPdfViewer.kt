package com.example.ui.pdf

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppPreferences
import com.example.ui.viewmodel.PdfViewerViewModel
import com.example.util.SecurityVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPdfViewer(
    filePath: String,
    fileName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val viewModel: PdfViewerViewModel = viewModel()
    val pageCount by viewModel.pageCount.collectAsState()
    val thumbnails by viewModel.thumbnails.collectAsState()
    val fullPages by viewModel.fullPages.collectAsState()

    val prefs = remember { AppPreferences(context) }
    val flipAnimEnabled by prefs.pdfFlipAnimation.collectAsState(initial = false)
    val scrollDir by prefs.pdfScrollDirection.collectAsState(initial = "vertical")
    val fitMode by prefs.pdfFitMode.collectAsState(initial = "fit")

    var loadingError by remember { mutableStateOf<String?>(null) }
    var tempDecryptedFile by remember { mutableStateOf<File?>(null) }
    var actualFile by remember { mutableStateOf<File?>(null) }
    
    // Search query state
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // UI state
    var showMenu by remember { mutableStateOf(false) }
    var showDetailsSheet by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Control status bar behavior
    DisposableEffect(isFullScreen) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                try {
                    WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Back handler to exit fullscreen first
    BackHandler(enabled = isFullScreen) {
        isFullScreen = false
    }

    // Reset fit mode to "fit" on launch (Persistence Bug Fix)
    LaunchedEffect(filePath) {
        if (filePath.isNotBlank()) {
            scope.launch { prefs.setPdfFitMode("fit") }
            
            withContext(Dispatchers.IO) {
                try {
                    val file = if (filePath.startsWith("wallet_secure/")) {
                        File(context.filesDir, filePath)
                    } else {
                        File(filePath)
                    }
                    if (file.exists()) {
                        actualFile = file
                        val vault = SecurityVault(context)
                        val tFile = vault.getFileForViewing(file, context.cacheDir)
                        if (tFile != null) {
                            tempDecryptedFile = tFile
                            viewModel.loadPdf(tFile)
                            loadingError = null
                        } else {
                            loadingError = "Could not prepare file."
                        }
                    } else {
                        loadingError = "File does not exist."
                    }
                } catch (e: Exception) {
                    loadingError = "Error loading PDF: ${e.message}"
                }
            }
        }
    }

    // Trigger thumbnail renders when grid is activated (Removed manual loop)
    // LazyVerticalGrid items() will automatically request thumbnail renders for visible items
    LaunchedEffect(fitMode, pageCount) {
        // We only respond to mode changes if needed
    }

    DisposableEffect(filePath) {
        onDispose {
            if (tempDecryptedFile != null && tempDecryptedFile != actualFile) {
                tempDecryptedFile?.delete()
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })

    Scaffold(
        topBar = {
            if (!isFullScreen) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = fileName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (pageCount > 0) {
                                Text(
                                    text = "Page ${pagerState.currentPage + 1} of $pageCount",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isFullScreen = true }) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { searchVisible = !searchVisible }) {
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = "Search", 
                                tint = if (searchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // 1. Page Flip Switch
                            DropdownMenuItem(
                                text = { Text("Page Flip Animation", fontSize = 14.sp) },
                                trailingIcon = {
                                    Switch(
                                        checked = flipAnimEnabled,
                                        onCheckedChange = {
                                            scope.launch { prefs.setPdfFlipAnimation(it) }
                                        },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                },
                                onClick = {
                                    scope.launch { prefs.setPdfFlipAnimation(!flipAnimEnabled) }
                                }
                            )
                            // 2. Scroll Direction
                            DropdownMenuItem(
                                text = { Text("Scroll Direction", fontSize = 14.sp) },
                                trailingIcon = {
                                    Icon(
                                        if (scrollDir == "vertical") Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        prefs.setPdfScrollDirection(if (scrollDir == "vertical") "horizontal" else "vertical")
                                        showMenu = false
                                    }
                                }
                            )
                            // 3. Fit mode / Grid view
                            DropdownMenuItem(
                                text = { Text(if (fitMode == "grid") "Single Page View" else "Grid Overview", fontSize = 14.sp) },
                                trailingIcon = {
                                    Icon(
                                        if (fitMode == "grid") Icons.Default.SingleBed else Icons.Default.GridView,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        prefs.setPdfFitMode(if (fitMode == "grid") "fit" else "grid")
                                        showMenu = false
                                    }
                                }
                            )
                            // 4. Share PDF
                            DropdownMenuItem(
                                text = { Text("Share PDF", fontSize = 14.sp) },
                                trailingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val file = tempDecryptedFile ?: return@DropdownMenuItem
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share PDF File"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                            // 4b. Open in External Viewer
                            DropdownMenuItem(
                                text = { Text("Open in External Viewer", fontSize = 14.sp) },
                                trailingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val file = tempDecryptedFile ?: return@DropdownMenuItem
                                    try {
                                        val sharedDir = File(context.cacheDir, "shared").also { it.mkdirs() }
                                        val extPdfFile = File(sharedDir, "${file.nameWithoutExtension}.pdf")
                                        if (!extPdfFile.exists() || extPdfFile.length() != file.length()) {
                                            file.copyTo(extPdfFile, overwrite = true)
                                        }
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            extPdfFile
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Open PDF with"))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        android.widget.Toast.makeText(context, "Could not open external viewer: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            // 5. Print Document
                            DropdownMenuItem(
                                text = { Text("Print Document", fontSize = 14.sp) },
                                trailingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val file = tempDecryptedFile ?: return@DropdownMenuItem
                                    try {
                                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                                        if (printManager != null) {
                                            printManager.print(
                                                fileName,
                                                MyPdfDocumentAdapter(file),
                                                PrintAttributes.Builder().build()
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                            // 6. Detailed Metadata Information
                            DropdownMenuItem(
                                text = { Text("View Details", fontSize = 14.sp) },
                                trailingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showDetailsSheet = true
                                }
                            )
                            // 7. Help / Instructions
                            DropdownMenuItem(
                                text = { Text("Help", fontSize = 14.sp) },
                                trailingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showHelpDialog = true
                                }
                            )
                        }
                    }
                }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else paddingValues)
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            if (isFullScreen) {
                var showHint by remember { mutableStateOf(true) }
                LaunchedEffect(isFullScreen) {
                    if (isFullScreen) {
                        delay(4000)
                        showHint = false
                    } else {
                        showHint = true
                    }
                }
                
                AnimatedVisibility(
                    visible = showHint,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .zIndex(10f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { isFullScreen = false }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.y > 10f) {
                                        isFullScreen = false
                                    }
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Swipe down or tap to exit fullscreen",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Display visual-only text search bar
            Column(modifier = Modifier.fillMaxSize()) {
                if (searchVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search text patterns...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (loadingError != null) {
                        Card(
                            modifier = Modifier.padding(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(loadingError!!, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("Go Back")
                                }
                            }
                        }
                    } else if (pageCount > 0) {
                        if (fitMode == "grid") {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(pageCount) { index ->
                                    LaunchedEffect(index) {
                                        viewModel.renderThumbnail(index)
                                    }
                                    val thumb = thumbnails[index]
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.71f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                scope.launch {
                                                    prefs.setPdfFitMode("fit")
                                                    pagerState.scrollToPage(index)
                                                }
                                            },
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.White)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (thumb != null) {
                                                Image(
                                                    bitmap = thumb.asImageBitmap(),
                                                    contentDescription = "Page ${index + 1}",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .background(Color.Black.copy(alpha = 0.6f))
                                                        .fillMaxWidth()
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Custom Pager with Scroll Direction and beautiful Page Flip Transform
                            val key = scrollDir // trigger rebuild on scroll direction toggle
                            key(key) {
                                val pageContent: @Composable (Int) -> Unit = { pageIndex ->
                                    // Render page when it comes into view
                                    LaunchedEffect(pageIndex) {
                                        viewModel.renderPage(pageIndex)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                            .graphicsLayer {
                                                if (flipAnimEnabled) {
                                                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
                                                    if (scrollDir == "horizontal") {
                                                        cameraDistance = 14 * density
                                                        rotationY = pageOffset * -180f
                                                        translationX = pageOffset * size.width
                                                        alpha = if (pageOffset.absoluteValue > 0.5f) 0f else 1f
                                                    } else {
                                                        cameraDistance = 14 * density
                                                        rotationX = pageOffset * 180f
                                                        translationY = pageOffset * size.height
                                                        alpha = if (pageOffset.absoluteValue > 0.5f) 0f else 1f
                                                    }
                                                } else {
                                                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                                                    scaleX = 1f - (pageOffset.coerceIn(0f, 1f) * 0.08f)
                                                    scaleY = scaleX
                                                    alpha = 1f - (pageOffset.coerceIn(0f, 1f) * 0.5f)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val pageBitmap = fullPages[pageIndex]
                                        if (pageBitmap != null) {
                                            var scale by remember { mutableFloatStateOf(1f) }
                                            var offset by remember { mutableStateOf(Offset.Zero) }
                                            
                                            // Reset scale and offset when pager changes page
                                            LaunchedEffect(pagerState.currentPage) {
                                                if (scale > 1f && pageIndex != pagerState.currentPage) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                }
                                            }

                                            val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                                                scale = (scale * zoomChange).coerceIn(1f, 6f)
                                                if (scale > 1f) {
                                                    offset += offsetChange
                                                } else {
                                                    offset = Offset.Zero
                                                }
                                            }

                                            val imageModifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer(
                                                    scaleX = scale,
                                                    scaleY = scale,
                                                    translationX = offset.x,
                                                    translationY = offset.y
                                                )
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            if (scale <= 1f) {
                                                                isFullScreen = !isFullScreen
                                                            }
                                                        },
                                                        onDoubleTap = {
                                                            if (scale > 1f) {
                                                                scale = 1f
                                                                offset = Offset.Zero
                                                            } else {
                                                                scale = 3f
                                                            }
                                                        }
                                                    )
                                                }
                                                
                                            val finalModifier = imageModifier
                                                .transformable(state = transformableState)
                                                .then(
                                                    if (scale > 1f) {
                                                        Modifier.pointerInput(Unit) {
                                                            detectDragGestures { change, dragAmount -> 
                                                                change.consume()
                                                                offset += dragAmount 
                                                            }
                                                        }
                                                    } else {
                                                        Modifier
                                                    }
                                                )

                                            Image(
                                                bitmap = pageBitmap.asImageBitmap(),
                                                contentDescription = "Page ${pageIndex + 1}",
                                                modifier = finalModifier
                                            )
                                        } else {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }

                                if (scrollDir == "horizontal") {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                        pageContent = { pageContent(it) }
                                    )
                                } else {
                                    VerticalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 12.dp),
                                        pageContent = { pageContent(it) }
                                    )
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    // Modal Details Sheet
    if (showDetailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Document Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                DetailItem(label = "File Name", value = fileName)
                
                val file = tempDecryptedFile
                if (file != null) {
                    val formattedSize = formatFileSize(file.length())
                    DetailItem(label = "File Size", value = formattedSize)
                    DetailItem(label = "Page Count", value = pageCount.toString())
                    
                    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault())
                    val sysDate = sdf.format(Date(file.lastModified()))
                    DetailItem(label = "Access Decrypted Time", value = sysDate)
                    DetailItem(label = "Temporary Safe Path", value = file.absolutePath)
                } else {
                    DetailItem(label = "Storage Status", value = "Loading metadata...")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showDetailsSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text("How to use PDF Reader", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("• Swipe Left/Right (or Up/Down if horizontal vertical mode) to change pages.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Double Tap to zoom in instantly. Pinch to adjust zoom.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Tap once to enter/exit Fullscreen mode.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• While zoomed in, drag with 1 finger to pan around the page.", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Enable 'Page Flip Animation' in the menu to see a realistic page turning effect.", fontSize = 14.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.toDouble())).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.toDouble(), digitGroups.toDouble()), units[digitGroups])
}

// Custom Printed Document Adapter
private class MyPdfDocumentAdapter(private val file: File) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(file.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        var input: FileInputStream? = null
        var output: FileOutputStream? = null
        try {
            input = FileInputStream(file)
            output = FileOutputStream(destination.fileDescriptor)
            val buf = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } >= 0) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                output.write(buf, 0, bytesRead)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.toString())
        } finally {
            try { input?.close() } catch (ignored: Exception) {}
            try { output?.close() } catch (ignored: Exception) {}
        }
    }
}
