package com.example

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.AppDatabase
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class MainApplication : Application(), ImageLoaderFactory {
    lateinit var database: AppDatabase
        private set

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Semesters table updates
            database.execSQL("ALTER TABLE semesters ADD COLUMN semester_label TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE semesters ADD COLUMN subjects_json TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE semesters ADD COLUMN total_credits REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE semesters ADD COLUMN earned_sgpa REAL")

            // AcademicItem table updates
            database.execSQL("ALTER TABLE academic_items ADD COLUMN board_roll_number TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN board_roll_code TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN board_registration_number TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN marksheet_index_number TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN school_code TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN medium_of_instruction TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN stream TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN use_best_of_five INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN passing_status TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN nta_percentile REAL")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN all_india_rank INTEGER")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN category_rank INTEGER")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN domicile_state TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN institution_district TEXT")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE semesters ADD COLUMN roll_number TEXT")
            database.execSQL("ALTER TABLE semesters ADD COLUMN override_roll_number INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE semesters ADD COLUMN attached_document_id TEXT")
            database.execSQL("ALTER TABLE semesters ADD COLUMN attached_document_name TEXT")

            database.execSQL("ALTER TABLE academic_items ADD COLUMN grade_conversion_formula TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN best_of_n INTEGER")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `pdf_reader_entries` (`filePath` TEXT NOT NULL, `displayName` TEXT NOT NULL, `folderId` INTEGER, `lastReadPage` INTEGER NOT NULL, `lastScrollOffset` REAL NOT NULL, `lastOpenedAt` INTEGER NOT NULL, `totalPages` INTEGER NOT NULL, PRIMARY KEY(`filePath`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `pdf_page_notes` (`filePath` TEXT NOT NULL, `pageIndex` INTEGER NOT NULL, `noteText` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`filePath`, `pageIndex`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `pdf_reader_folders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `parentFolderId` INTEGER, `createdAt` INTEGER NOT NULL)")
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "scholar-vault-db"
        )
        .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
        .fallbackToDestructiveMigration(true)
        .build()

        // Asynchronously sweep decrypt file cache older than 1 hour
        Thread {
            try {
                val oneHourAgo = System.currentTimeMillis() - 3600000L
                val sharedDir = java.io.File(cacheDir, "shared")
                if (sharedDir.exists() && sharedDir.isDirectory) {
                    sharedDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < oneHourAgo) {
                            file.delete()
                        }
                    }
                }
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < oneHourAgo) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of RAM
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 128MB disk cache
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
