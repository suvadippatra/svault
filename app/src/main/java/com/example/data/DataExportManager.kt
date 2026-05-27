package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DataExportManager(private val context: Context) {

    enum class ExportScope { TEXT_DATA_ONLY, INCLUDE_FILES, INCLUDE_WALLET }

    suspend fun exportToSv(
        outputFile: File,
        scope: ExportScope
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            outputFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(outputFile)).use { zip ->

                // 1. Manifest
                val manifest = buildManifest(scope)
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toByteArray())
                zip.closeEntry()

                // 2. Room database dump (always included)
                val dbNames = listOf("scholar_vault_db", "scholar_vault_db-shm", "scholar_vault_db-wal")
                dbNames.forEach { dbName ->
                    val dbFile = context.getDatabasePath(dbName)
                    if (dbFile.exists()) {
                        zip.putNextEntry(ZipEntry("database/$dbName"))
                        FileInputStream(dbFile).use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }

                // 3. Document files (stored in filesDir)
                if (scope != ExportScope.TEXT_DATA_ONLY) {
                    val filesDir = context.filesDir
                    if (filesDir.exists()) {
                        filesDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            val relativePath = file.relativeTo(filesDir).path
                            // Skip cache, temp files, db files, and wallet if not requested
                            val isSecure = relativePath.startsWith("wallet_secure")
                            val isCache = relativePath.contains("cache") || relativePath.contains("app_webview")
                            val isDbFile = dbNames.any { relativePath.endsWith(it) } || relativePath.contains("shared_prefs")
                            val isTempViewFile = relativePath.contains("sv_view_")
                            if (!isSecure && !isCache && !isDbFile && !isTempViewFile) {
                                zip.putNextEntry(ZipEntry("documents/$relativePath"))
                                FileInputStream(file).use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }

                // 4. Encrypted Wallet files (stored in filesDir/wallet_secure)
                if (scope == ExportScope.INCLUDE_WALLET) {
                    val walletDir = File(context.filesDir, "wallet_secure")
                    if (walletDir.exists()) {
                        walletDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            val relativePath = file.relativeTo(walletDir).path
                            zip.putNextEntry(ZipEntry("wallet/$relativePath"))
                            FileInputStream(file).use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importFromSv(svFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(FileInputStream(svFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "manifest.json" -> {
                            // Can parse metadata if needed
                        }
                        entry.name.startsWith("database/") -> {
                            val dbName = entry.name.substringAfter("database/")
                            val dest = context.getDatabasePath(dbName)
                            dest.parentFile?.mkdirs()
                            FileOutputStream(dest).use { zip.copyTo(it) }
                        }
                        entry.name.startsWith("documents/") -> {
                            val relativePath = entry.name.substringAfter("documents/")
                            val dest = File(context.filesDir, relativePath)
                            dest.parentFile?.mkdirs()
                            FileOutputStream(dest).use { zip.copyTo(it) }
                        }
                        entry.name.startsWith("wallet/") -> {
                            val relativePath = entry.name.substringAfter("wallet/")
                            val dest = File(context.filesDir, "wallet_secure/$relativePath")
                            dest.parentFile?.mkdirs()
                            FileOutputStream(dest).use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun buildManifest(scope: ExportScope): String {
        return """
        {
          "app": "ScholarVault",
          "version": 1,
          "exportedAt": "${System.currentTimeMillis()}",
          "scope": "${scope.name}"
        }
        """.trimIndent()
    }
}
