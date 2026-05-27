package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore by preferencesDataStore(name = "scholar_viewer_prefs")

object PrefKeys {
    val PDF_VIEWER_MODE = stringPreferencesKey("pdf_viewer_mode")
    // "google_drive" | "custom"  — default: "custom"
    val PDF_FLIP_ANIMATION = booleanPreferencesKey("pdf_flip_anim")
    val PDF_SCROLL_DIRECTION = stringPreferencesKey("pdf_scroll_dir")
    // "horizontal" | "vertical"
    val PDF_FIT_MODE = stringPreferencesKey("pdf_fit_mode")
    // "fit" | "grid"
    val THEME_MODE = stringPreferencesKey("theme_mode")
    // "system" | "light" | "dark"
}

class AppPreferences(private val context: Context) {
    
    val themeMode: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.THEME_MODE] ?: "system" }

    val pdfViewerMode: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_VIEWER_MODE] ?: "custom" }

    val pdfFlipAnimation: Flow<Boolean> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_FLIP_ANIMATION] ?: false }

    val pdfScrollDirection: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_SCROLL_DIRECTION] ?: "vertical" }

    val pdfFitMode: Flow<String> = context.appPrefsDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[PrefKeys.PDF_FIT_MODE] ?: "fit" }

    suspend fun setPdfViewerMode(mode: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_VIEWER_MODE] = mode }
    }

    suspend fun setPdfFlipAnimation(enabled: Boolean) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_FLIP_ANIMATION] = enabled }
    }

    suspend fun setPdfScrollDirection(dir: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_SCROLL_DIRECTION] = dir }
    }

    suspend fun setPdfFitMode(mode: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.PDF_FIT_MODE] = mode }
    }

    suspend fun resetTransientViewerState() {
        context.appPrefsDataStore.edit { prefs ->
            prefs[PrefKeys.PDF_FIT_MODE] = "fit"
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.appPrefsDataStore.edit { it[PrefKeys.THEME_MODE] = mode }
    }
}
