package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiInsight
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiState {
    object Idle : AiState
    object Loading : AiState
    data class Success(val advice: String) : AiState
    data class Error(val message: String) : AiState
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
    }

    // Task Streams
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val aiInsight: StateFlow<AiInsight?> = repository.aiInsight
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Current screen representation
    private val _currentTab = MutableStateFlow("tasks") // tasks, focus, ai, stats
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    // Task Filter state
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    fun filterByCategory(category: String?) {
        _selectedCategoryFilter.value = category
    }

    // AI prioritize trigger status
    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    fun resetAiState() {
        _aiState.value = AiState.Idle
    }

    fun optimizeWithAi() {
        viewModelScope.launch {
            _aiState.value = AiState.Loading
            val incompleteList = allTasks.value.filter { !it.isCompleted }
            if (incompleteList.isEmpty()) {
                _aiState.value = AiState.Error("هیچ کار انجام نشده‌ای برای اولویت‌بندی وجود ندارد! ابتدا تسک جدید اضافه کنید.")
                return@launch
            }
            val result = repository.performAiPrioritization(incompleteList)
            result.onSuccess { advice ->
                _aiState.value = AiState.Success(advice)
            }.onFailure { exception ->
                _aiState.value = AiState.Error(exception.message ?: "خطای ناشناخته در ارتباط با سرور.")
            }
        }
    }

    // --- Task operations ---
    fun addTask(
        title: String,
        description: String,
        category: String,
        dueDate: Long,
        priority: String,
        estimatedMinutes: Int
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                category = category,
                dueDate = dueDate,
                priority = priority,
                estimatedMinutes = estimatedMinutes
            )
            repository.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            // If the active focus task was completed, stop the focus timer
            if (activeFocusTask.value?.id == task.id && updated.isCompleted) {
                stopFocusTimer()
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            if (activeFocusTask.value?.id == task.id) {
                stopFocusTimer()
            }
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.deleteAllTasks()
            stopFocusTimer()
        }
    }

    // --- Focus Timer Operations ---
    private val _activeFocusTask = MutableStateFlow<Task?>(null)
    val activeFocusTask: StateFlow<Task?> = _activeFocusTask.asStateFlow()

    private val _focusTimeLeftSeconds = MutableStateFlow(25 * 60)
    val focusTimeLeftSeconds: StateFlow<Int> = _focusTimeLeftSeconds.asStateFlow()

    private val _isFocusRunning = MutableStateFlow(false)
    val isFocusRunning: StateFlow<Boolean> = _isFocusRunning.asStateFlow()

    private val _focusModeDurationSet = MutableStateFlow(25) // Minutes
    val focusModeDurationSet: StateFlow<Int> = _focusModeDurationSet.asStateFlow()

    private var timerJob: Job? = null

    fun selectTaskForFocus(task: Task?) {
        _activeFocusTask.value = task
        if (task != null) {
            // Set timer duration to task's estimated time (bound between 5 and 120 minutes)
            val minutes = task.estimatedMinutes.coerceIn(5, 120)
            _focusModeDurationSet.value = minutes
            _focusTimeLeftSeconds.value = minutes * 60
        } else {
            _focusModeDurationSet.value = 25
            _focusTimeLeftSeconds.value = 25 * 60
        }
        stopFocusTimer()
    }

    fun setCustomFocusDuration(minutes: Int) {
        _focusModeDurationSet.value = minutes
        _focusTimeLeftSeconds.value = minutes * 60
        stopFocusTimer()
    }

    fun startFocusTimer() {
        if (_isFocusRunning.value) return
        _isFocusRunning.value = true
        timerJob = viewModelScope.launch {
            while (_focusTimeLeftSeconds.value > 0 && _isFocusRunning.value) {
                delay(1000)
                _focusTimeLeftSeconds.value -= 1
            }
            if (_focusTimeLeftSeconds.value == 0) {
                // Timer completed! Let's update task or session completed counts
                _isFocusRunning.value = false
                val currentTask = _activeFocusTask.value
                if (currentTask != null) {
                    // Mark task complete when pomodoro completes or credit focus
                    val updatedTask = currentTask.copy(isCompleted = true)
                    repository.updateTask(updatedTask)
                    _activeFocusTask.value = null
                }
            }
        }
    }

    fun pauseFocusTimer() {
        _isFocusRunning.value = false
        timerJob?.cancel()
    }

    fun stopFocusTimer() {
        _isFocusRunning.value = false
        timerJob?.cancel()
        _focusTimeLeftSeconds.value = _focusModeDurationSet.value * 60
    }

    // --- Statistics Helper Computations ---
    // Combined stats StateFlow derived from allTasks updates reactive to UI
    data class Stats(
        val totalCount: Int = 0,
        val completedCount: Int = 0,
        val completionRate: Float = 0f,
        val highPriorityCount: Int = 0,
        val categoryDistribution: Map<String, Int> = emptyMap(),
        val categoryCompletionRate: Map<String, Float> = emptyMap()
    )

    val stats: StateFlow<Stats> = allTasks.combine(selectedCategoryFilter) { tasks, _ ->
        if (tasks.isEmpty()) return@combine Stats()

        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val completionRate = completed.toFloat() / total.toFloat()
        val highPriority = tasks.count { it.priority == "HIGH" && !it.isCompleted }

        val cats = tasks.groupBy { it.category }.mapValues { it.value.size }
        val catComp = tasks.groupBy { it.category }.mapValues { entry ->
            val totalInCat = entry.value.size
            val compInCat = entry.value.count { it.isCompleted }
            if (totalInCat > 0) compInCat.toFloat() / totalInCat.toFloat() else 0f
        }

        Stats(
            totalCount = total,
            completedCount = completed,
            completionRate = completionRate,
            highPriorityCount = highPriority,
            categoryDistribution = cats,
            categoryCompletionRate = catComp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Stats()
    )
}
