package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.data.model.ReminderEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.ReminderRepository
import com.example.data.repository.TaskRepository
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class Quote(
    val text: String,
    val author: String
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as MainApplication).database
    private val taskRepo = TaskRepository(db.taskDao())
    private val reminderRepo = ReminderRepository(db.reminderDao())
    private val scheduler = AlarmScheduler(application)

    private val _quote = MutableStateFlow(Quote("Loading...", ""))
    val quote: StateFlow<Quote> = _quote

    private val allQuotes = listOf(
        Quote("A well-educated mind will always have more questions than answers.", "Helen Keller"),
        Quote("The beautiful thing about learning is that no one can take it away from you.", "B.B. King"),
        Quote("Education is the most powerful weapon which you can use to change the world.", "Nelson Mandela"),
        Quote("Learning is not attained by chance, it must be sought for with ardor and diligence.", "Abigail Adams"),
        Quote("An investment in knowledge pays the best interest.", "Benjamin Franklin")
    )

    init {
        _quote.value = allQuotes.random()
    }

    // ── Academic & Wallet Stats ──────────────────────────────
    val academicCount: StateFlow<Int> = db.academicItemDao().getAllAcademicItems()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val walletDocCount: StateFlow<Int> = db.documentDao().getAllNonFolderFiles()
        .map { files -> files.count { it.tags.contains("wallet") || it.isEncrypted } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Tasks ────────────────────────────────────────────────
    val tasks: StateFlow<List<TaskEntity>> = taskRepo.activeTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedTasks: StateFlow<List<TaskEntity>> = taskRepo.deletedTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedTaskIds: StateFlow<Set<String>> = _selectedTaskIds

    private val _showDeletedTasks = MutableStateFlow(false)
    val showDeletedTasks: StateFlow<Boolean> = _showDeletedTasks

    private val _showDeletedReminders = MutableStateFlow(false)
    val showDeletedReminders: StateFlow<Boolean> = _showDeletedReminders

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { taskRepo.addTask(title) }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { taskRepo.toggleTask(task) }
    }

    fun toggleTaskSelection(id: String) {
        _selectedTaskIds.update {
            if (id in it) it - id else it + id
        }
    }

    fun clearTaskSelection() {
         _selectedTaskIds.value = emptySet()
    }

    fun toggleShowDeletedTasks() {
        _showDeletedTasks.value = !_showDeletedTasks.value
    }

    fun toggleShowDeletedReminders() {
        _showDeletedReminders.value = !_showDeletedReminders.value
    }

    fun deleteSelectedTasks() {
        viewModelScope.launch {
            taskRepo.softDeleteTasks(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }

    fun restoreSelectedTasks() {
        viewModelScope.launch { 
            taskRepo.restoreTasks(_selectedTaskIds.value) 
            _selectedTaskIds.value = emptySet()
        }
    }

    fun completelyDeleteSelectedTasks() {
        viewModelScope.launch { 
            taskRepo.hardDeleteTasks(_selectedTaskIds.value)
            _selectedTaskIds.value = emptySet()
        }
    }

    // ── Reminders ────────────────────────────────────────────
    val reminders: StateFlow<List<ReminderEntity>> = reminderRepo.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextReminder: StateFlow<ReminderEntity?> = reminderRepo.allReminders
        .map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.timeInMillis > now }
                .minByOrNull { it.timeInMillis }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedReminderIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedReminderIds: StateFlow<Set<String>> = _selectedReminderIds

    val deletedReminders: StateFlow<List<ReminderEntity>> = reminderRepo.deletedReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReminder(title: String, dateTimeStr: String, timeInMillis: Long) {
        if (title.isBlank() || timeInMillis <= 0L) return
        viewModelScope.launch {
            // Generate ID first so scheduler and DB use the same one
            val id = java.util.UUID.randomUUID().toString()
            reminderRepo.addReminder(title, dateTimeStr, timeInMillis)
            scheduler.scheduleAlarm(id, title, "Time for your reminder: $title", timeInMillis)
        }
    }

    fun toggleReminderSelection(id: String) {
        _selectedReminderIds.update {
            if (id in it) it - id else it + id
        }
    }

    fun clearReminderSelection() {
        _selectedReminderIds.value = emptySet()
    }

    fun deleteSelectedReminders() {
        viewModelScope.launch {
            _selectedReminderIds.value.forEach { id ->
                scheduler.cancelAlarm(id)
            }
            reminderRepo.softDeleteReminders(_selectedReminderIds.value)
            _selectedReminderIds.value = emptySet()
        }
    }

    fun completelyDeleteSelectedReminders() {
        viewModelScope.launch {
            reminderRepo.hardDeleteReminders(_selectedReminderIds.value)
            _selectedReminderIds.value = emptySet()
        }
    }

    fun restoreSelectedReminders() {
        viewModelScope.launch {
            reminderRepo.restoreReminders(_selectedReminderIds.value)
            _selectedReminderIds.value = emptySet()
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            scheduler.cancelAlarm(id)      // ← cancel alarm first
            reminderRepo.softDeleteReminders(setOf(id))
        }
    }
}
