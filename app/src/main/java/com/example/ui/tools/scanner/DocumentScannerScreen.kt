package com.example.ui.tools.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

enum class ScannerState {
    CAPTURE, CROP, FILTER, SUMMARY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerScreen(
    onBack: () -> Unit,
    onImageCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var currentState by remember { mutableStateOf(ScannerState.CAPTURE) }
    var currentCaptureFile by remember { mutableStateOf<File?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scannedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    if (!hasCameraPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan documents.")
        }
        return
    }

    when (currentState) {
        ScannerState.CAPTURE -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Scan Document") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (scannedPages.isNotEmpty()) {
                                TextButton(onClick = { currentState = ScannerState.SUMMARY }) {
                                    Text("Done (${scannedPages.size})")
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    FloatingActionButton(
                        onClick = {
                            val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        currentCaptureFile = file
                                        currentBitmap = parseCapturedImage(file)
                                        currentState = ScannerState.CROP
                                    }
                                    override fun onError(e: ImageCaptureException) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture")
                    }
                }
            }
        }
        ScannerState.CROP -> {
            currentBitmap?.let { bmp ->
                PolygonCropScreen(
                    imageBitmap = bmp,
                    onCropConfirmed = { cropped ->
                        currentBitmap = cropped
                        currentState = ScannerState.FILTER
                    },
                    onCancel = {
                        currentState = ScannerState.CAPTURE
                    }
                )
            }
        }
        ScannerState.FILTER -> {
            currentBitmap?.let { bmp ->
                ScanFilterScreen(
                    imageBitmap = bmp,
                    onFilterConfirmed = { filtered ->
                        scannedPages = scannedPages + filtered
                        currentState = ScannerState.CAPTURE
                    },
                    onCancel = {
                        currentState = ScannerState.CROP
                    }
                )
            }
        }
        ScannerState.SUMMARY -> {
            ScanSummaryScreen(
                pages = scannedPages,
                onAddMore = { currentState = ScannerState.CAPTURE },
                onSavePdf = { outputFileName ->
                    val file = File(context.getExternalFilesDir(null), "$outputFileName.pdf")
                    val isSuccess = createPdfFromBitmaps(scannedPages, file).isSuccess
                    if (isSuccess) {
                        onImageCaptured(file)
                        onBack()
                    }
                },
                onCancel = { onBack() }
            )
        }
    }
}

fun parseCapturedImage(file: File): Bitmap {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val exifInterface = android.media.ExifInterface(file.absolutePath)
    val orientation = exifInterface.getAttributeInt(
        android.media.ExifInterface.TAG_ORIENTATION,
        android.media.ExifInterface.ORIENTATION_UNDEFINED
    )
    val matrix = Matrix()
    when (orientation) {
        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun createPdfFromBitmaps(pages: List<Bitmap>, outputFile: File): Result<File> {
    return try {
        val doc = android.graphics.pdf.PdfDocument()
        pages.forEachIndexed { i, bmp ->
            val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(bmp.width, bmp.height, i + 1).create()
            val page = doc.startPage(info)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(page)
        }
        java.io.FileOutputStream(outputFile).use { doc.writeTo(it) }
        doc.close()
        Result.success(outputFile)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
