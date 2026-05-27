package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.model.AcademicItem
import com.example.data.repository.AcademicRepository
import com.example.data.repository.DocumentRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.viewmodel.DocumentViewModel
import kotlinx.coroutines.launch
import java.util.Date

class MainActivity : FragmentActivity() {
    private lateinit var repository: AcademicRepository
    private lateinit var docRepository: DocumentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        val app = application as MainApplication
        repository = AcademicRepository(app.database.academicItemDao())
        docRepository = DocumentRepository(app.database.documentDao(), app.database.walletDao())

        val viewModelFactory = AcademicViewModel.Factory(repository)
        val docViewModelFactory = DocumentViewModel.Factory(docRepository)

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = androidx.compose.runtime.remember { com.example.data.AppPreferences(context) }
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                permissions.entries.forEach { (permission, isGranted) ->
                    if (!isGranted) {
                        android.util.Log.d("Permission", "$permission denied")
                    }
                }
            }

            androidx.compose.runtime.LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
                    permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                    permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
                
                permissionsToRequest.add(android.Manifest.permission.CAMERA)
                permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)

                val ungranted = permissionsToRequest.filter { perm ->
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, perm
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                }

                if (ungranted.isNotEmpty()) {
                    permissionLauncher.launch(ungranted.toTypedArray())
                }
            }
            
            var animationsEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            var imagePreviewCacheEnabled by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            val themeController = androidx.compose.runtime.remember(themeMode, animationsEnabled, imagePreviewCacheEnabled) {
                com.example.ui.theme.ThemeController(
                    themeMode = themeMode,
                    setThemeMode = { mode -> scope.launch { prefs.setThemeMode(mode) } },
                    animationsEnabled = animationsEnabled,
                    toggleAnimations = { animationsEnabled = !animationsEnabled },
                    imagePreviewCacheEnabled = imagePreviewCacheEnabled,
                    toggleImagePreviewCache = { imagePreviewCacheEnabled = !imagePreviewCacheEnabled }
                )
            }

            val isActuallyDark = themeController.isDarkTheme

            androidx.compose.runtime.LaunchedEffect(isActuallyDark) {
                val statusBarStyle = if (isActuallyDark) {
                    androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    androidx.activity.SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                val navBarStyle = if (isActuallyDark) {
                    androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    androidx.activity.SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                }
                enableEdgeToEdge(statusBarStyle = statusBarStyle, navigationBarStyle = navBarStyle)
            }

            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.theme.LocalThemeController provides themeController
            ) {
                MyApplicationTheme(themeMode = themeMode) {
                    val viewModel: AcademicViewModel = viewModel(factory = viewModelFactory)
                    val docViewModel: DocumentViewModel = viewModel(factory = docViewModelFactory)
                    com.example.ui.MainScreen(viewModel = viewModel, docViewModel = docViewModel)
                }
            }
        }
    }
}