package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        AcademicItem::class,
        Semester::class,
        AcademicDocumentLink::class,
        Transaction::class,
        UserProfile::class,
        ProfileExperience::class,
        ProfileWork::class,
        DocumentFile::class,
        WalletCategory::class,
        WalletCard::class,
        TrashEntity::class,
        WalletAttachmentEntity::class,
        FolderEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        PdfReaderEntry::class,
        PdfPageNote::class,
        PdfFolder::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun academicItemDao(): AcademicItemDao
    abstract fun semesterDao(): SemesterDao
    abstract fun academicDocumentLinkDao(): AcademicDocumentLinkDao
    abstract fun transactionDao(): TransactionDao
    abstract fun profileDao(): ProfileDao
    abstract fun documentDao(): DocumentDao
    abstract fun walletDao(): WalletDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun pdfReaderDao(): PdfReaderDao
}
