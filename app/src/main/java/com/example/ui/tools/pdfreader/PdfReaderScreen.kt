package com.example.ui.tools.pdfreader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    filePath: String,
    onBack: () -> Unit,
    viewModel: PdfReaderViewModel = viewModel()
) {
    val file = File(filePath)
    val pageCount by viewModel.pageCount.collectAsState()
    val savedPage by viewModel.savedPage.collectAsState()
    val twoPageMode by viewModel.twoPageMode.collectAsState()
    val strokesPerPage by viewModel.strokesPerPage.collectAsState()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isInkMode by remember { mutableStateOf(false) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var currentNoteText by remember { mutableStateOf("") }
    
    val noteFlow = remember(filePath, savedPage) { viewModel.getNoteFlow(filePath, savedPage) }
    val currentNote by noteFlow.collectAsState(initial = null)
    LaunchedEffect(currentNote) {
        currentNoteText = currentNote?.noteText ?: ""
    }
    
    LaunchedEffect(filePath) {
        viewModel.loadPdf(file)
    }

    Scaffold(
        topBar = {
            if (isSearchMode) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    if (query.isNotEmpty()) {
                                        searchResults = PdfTextExtractor.searchInPage(file, savedPage, query)
                                    } else {
                                        searchResults = emptyList()
                                    }
                                }
                            },
                            placeholder = { Text("Search this page...") },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearchMode = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(file.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchMode = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showNoteDialog = true }) {
                            Icon(Icons.Default.NoteAdd, contentDescription = "Add Note")
                        }
                        IconButton(onClick = { isInkMode = !isInkMode }) {
                            Icon(Icons.Default.Brush, contentDescription = "Draw", tint = if (isInkMode) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                        IconButton(onClick = { viewModel.toggleTwoPageMode() }) {
                            Icon(Icons.Default.ImportExport, contentDescription = "Toggle Two Page")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val out = File(context.cacheDir, "exported_${file.name}")
                                viewModel.exportWithAnnotations(file, out)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (pageCount > 0) {
                if (twoPageMode && pageCount >= 2) {
                    val twoPageCount = (pageCount + 1) / 2
                    val pagerState = rememberPagerState(initialPage = savedPage / 2, pageCount = { twoPageCount })
                    
                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.onPageChanged(filePath, pagerState.currentPage * 2)
                    }
    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pairIndex ->
                        Row(modifier = Modifier.fillMaxSize()) {
                            val leftPage = pairIndex * 2
                            val rightPage = leftPage + 1
                            PdfPageContent(
                                viewModel = viewModel,
                                pageIndex = leftPage,
                                isInkMode = isInkMode,
                                strokes = strokesPerPage[leftPage] ?: emptyList(),
                                modifier = Modifier.weight(1f)
                            )
                            if (rightPage < pageCount) {
                                PdfPageContent(
                                    viewModel = viewModel,
                                    pageIndex = rightPage,
                                    isInkMode = isInkMode,
                                    strokes = strokesPerPage[rightPage] ?: emptyList(),
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(initialPage = savedPage, pageCount = { pageCount })
                    
                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.onPageChanged(filePath, pagerState.currentPage)
                    }
    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        PdfPageContent(
                            viewModel = viewModel,
                            pageIndex = pageIndex,
                            isInkMode = isInkMode,
                            strokes = strokesPerPage[pageIndex] ?: emptyList(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (isSearchMode && searchResults.isNotEmpty()) {
                    Text(
                        text = "${searchResults.size} matches found",
                        modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).background(MaterialTheme.colorScheme.primaryContainer, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (showNoteDialog) {
                    PdfNoteDialog(
                        pageIndex = savedPage,
                        existingNote = currentNoteText,
                        onSave = { text ->
                            viewModel.saveNote(filePath, savedPage, text)
                            showNoteDialog = false
                        },
                        onDismiss = { showNoteDialog = false }
                    )
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun PdfPageContent(
    viewModel: PdfReaderViewModel,
    pageIndex: Int,
    isInkMode: Boolean,
    strokes: List<List<androidx.compose.ui.geometry.Offset>>,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(pageIndex) {
        bitmap = viewModel.renderPage(pageIndex)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            if (isInkMode) {
                PdfInkOverlay(
                    strokes = strokes,
                    onStrokeComplete = { stroke ->
                        viewModel.saveStroke(pageIndex, stroke)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
