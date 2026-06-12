package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AiInsight
import com.example.data.Task
import com.example.ui.AiState
import com.example.ui.TaskViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerMainScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val aiInsight by viewModel.aiInsight.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val filterCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Enforce RTL Layout globally for Persian Planner
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "AI Planner",
                                tint = DarkSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "پلنر هوشمند هوش‌بار",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    selectedTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            },
            floatingActionButton = {
                if (currentTab == "tasks") {
                    ExtendedFloatingActionButton(
                        onClick = { showAddTaskDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .testTag("add_task_fab")
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن تسک")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "تسک جدید", fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            ) {
                Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                    when (tab) {
                        "tasks" -> TasksTab(
                            tasks = tasks,
                            filterCategory = filterCategory,
                            onCategoryFilterSelected = { viewModel.filterByCategory(it) },
                            onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                            onDeleteTask = { viewModel.deleteTask(it) }
                        )
                        "focus" -> FocusTab(
                            tasks = tasks.filter { !it.isCompleted },
                            viewModel = viewModel
                        )
                        "ai" -> AiAdvisorTab(
                            aimMessage = aiInsight,
                            aiState = aiState,
                            tasks = tasks,
                            onTriggerPrioritize = { viewModel.optimizeWithAi() },
                            onResetState = { viewModel.resetAiState() }
                        )
                        "stats" -> StatsTab(
                            stats = stats,
                            tasks = tasks
                        )
                    }
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { showAddTaskDialog = false },
                onTaskAdd = { title, desc, cat, dur, prio ->
                    viewModel.addTask(title, desc, cat, System.currentTimeMillis() + (86400000), prio, dur)
                    showAddTaskDialog = false
                }
            )
        }
    }
}

// --- Bottom Navigation Bar ---
@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.shadow(16.dp)
    ) {
        NavigationBarItem(
            selected = selectedTab == "tasks",
            onClick = { onTabSelected("tasks") },
            icon = { Icon(imageVector = Icons.Default.List, contentDescription = "تسک‌ها") },
            label = { Text("کارها", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_tasks")
        )
        NavigationBarItem(
            selected = selectedTab == "focus",
            onClick = { onTabSelected("focus") },
            icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "تمرکز") },
            label = { Text("تمرکز عمیق", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkSecondary,
                indicatorColor = DarkSecondary.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_focus")
        )
        NavigationBarItem(
            selected = selectedTab == "ai",
            onClick = { onTabSelected("ai") },
            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = "هوش مصنوعی") },
            label = { Text("مشاور هوشمند", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ColorAiScore,
                indicatorColor = ColorAiScore.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_ai")
        )
        NavigationBarItem(
            selected = selectedTab == "stats",
            onClick = { onTabSelected("stats") },
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = "عملکرد") },
            label = { Text("عملکرد من", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PriorityLow,
                indicatorColor = PriorityLow.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag("nav_stats")
        )
    }
}

// --- TAB 1: Tasks ---
@Composable
fun TasksTab(
    tasks: List<Task>,
    filterCategory: String?,
    onCategoryFilterSelected: (String?) -> Unit,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit
) {
    val categories = listOf("کار", "شخصی", "آموزش", "سلامتی", "سایر")

    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            item {
                InputChip(
                    selected = filterCategory == null,
                    onClick = { onCategoryFilterSelected(null) },
                    label = { Text("همه کارها") },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            items(categories) { cat ->
                InputChip(
                    selected = filterCategory == cat,
                    onClick = { onCategoryFilterSelected(cat) },
                    label = { Text(cat) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = getCategoryColor(cat).copy(alpha = 0.2f),
                        selectedLabelColor = getCategoryColor(cat)
                    )
                )
            }
        }

        val filteredTasks = if (filterCategory == null) {
            tasks
        } else {
            tasks.filter { it.category == filterCategory }
        }

        if (filteredTasks.isEmpty()) {
            EmptyTasksState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleCompletion = { onToggleCompletion(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedAiReasoning by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (task.aiPriorityScore > 75 && !task.isCompleted) ColorAiScore.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = task.isCompleted,
                    onClick = onToggleCompletion,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = PriorityLow,
                        unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("task_complete_btn_${task.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("task_delete_btn_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف تسک",
                        tint = PriorityHigh.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(getCategoryColor(task.category).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = task.category,
                        color = getCategoryColor(task.category),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Priority Badge
                val (priorityText, priorityColor) = getPriorityMeta(task.priority)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(priorityColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = priorityText,
                        color = priorityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Duration estimated
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "زمان",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.estimatedMinutes} دقیقه",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // AI prioritization score indicator
                if (task.aiPriorityScore > 0 && !task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ColorAiScore,
                                        ColorAiScore.copy(alpha = 0.7f)
                                    )
                                )
                            )
                            .clickable { expandedAiReasoning = !expandedAiReasoning }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "امتیاز هوشمند",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "امتیاز هوش: ${task.aiPriorityScore}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = if (expandedAiReasoning) Icons.Default.KeyboardArrowUp else Icons.Default.Info,
                                contentDescription = "توضیحات بیشتر",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Expanded AI Reasoning block
            if (expandedAiReasoning && task.aiReasoning.isNotEmpty() && !task.isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ColorAiScore.copy(alpha = 0.08f))
                        .border(1.dp, ColorAiScore.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "💡 تحلیل هوشمند اولویت‌بندی:",
                                color = ColorAiScore,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.aiReasoning,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// --- Empty Tasks Placeholder ---
@Composable
fun EmptyTasksState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "بدون تسک",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "برنامه شما کاملاً خالی است!",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "تسک‌های امروزت رو تعریف کن و دکمه «اولویت‌بندی با هوش مصنوعی» رو بزن تا بهترین برنامه‌ریزی رو تحویل بگیری.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

// --- TAB 2: Focus / Deep Work Timer ---
@Composable
fun FocusTab(
    tasks: List<Task>,
    viewModel: TaskViewModel
) {
    val activeTask by viewModel.activeFocusTask.collectAsStateWithLifecycle()
    val timeLeft by viewModel.focusTimeLeftSeconds.collectAsStateWithLifecycle()
    val isRunning by viewModel.isFocusRunning.collectAsStateWithLifecycle()
    val durationSet by viewModel.focusModeDurationSet.collectAsStateWithLifecycle()

    var showDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selection block
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚡️ اتاقک تمرکز عمیق (پومودورو)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "انتخاب تسک و مسدودسازی زمانی برای تمرکز بدون قید و بند و افزایش بهره‌وری شما.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector Button
            Box {
                Button(
                    onClick = { showDropdown = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_focus_task_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeTask?.title ?: "--- یک تسک انتخاب کنید ---",
                            color = if (activeTask != null) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "انتخاب")
                    }
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("بدون تسک (تایمر آزاد)") },
                        onClick = {
                            viewModel.selectTaskForFocus(null)
                            showDropdown = false
                        }
                    )
                    tasks.forEach { task ->
                        DropdownMenuItem(
                            text = { Text(task.title) },
                            onClick = {
                                viewModel.selectTaskForFocus(task)
                                showDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Beautiful Timer Ring Canvas
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp)
        ) {
            val progress = if (durationSet > 0) {
                timeLeft.toFloat() / (durationSet * 60).toFloat()
            } else {
                1f
            }

            val strokeColor = if (isRunning) DarkSecondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawCircle(
                    color = strokeColor.copy(alpha = 0.08f),
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
                // Active progression
                drawArc(
                    color = strokeColor,
                    startAngle = -270f,
                    sweepAngle = -360f * progress,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Digital Text display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val minStr = String.format("%02d", timeLeft / 60)
                val secStr = String.format("%02d", timeLeft % 60)
                Text(
                    text = "$minStr:$secStr",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (activeTask != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeTask!!.category,
                        color = getCategoryColor(activeTask!!.category),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Control Panel Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isRunning) {
                Button(
                    onClick = { viewModel.startFocusTimer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("start_timer_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeTask != null) DarkSecondary else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "شروع")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("شروع تمرکز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = { viewModel.pauseFocusTimer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("pause_timer_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityMedium),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "توقف")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("توقف موقت", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            IconButton(
                onClick = { viewModel.stopFocusTimer() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                    .testTag("reset_timer_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "بازنشانی",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- TAB 3: AI Insights / Strategic Advisor ---
@Composable
fun AiAdvisorTab(
    aimMessage: AiInsight?,
    aiState: AiState,
    tasks: List<Task>,
    onTriggerPrioritize: () -> Unit,
    onResetState: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AI Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ColorAiScore.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ColorAiScore.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "هوش مصنوعی",
                        tint = ColorAiScore,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "برنامه‌ریزی استراتژیک با هوش مصنوعی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "با یک کلیک، لیست کارهای ناقص شما برای مدل هوش مصنوعی Gemini ارسال شده و با تحلیل ددلاین‌ها و اهمیت کارها، نمرات اولویت‌بندی دقیق و یک برنامه عملیاتی به زبان شیرین فارسی تولید می‌شود.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val incompleteCount = tasks.count { !it.isCompleted }

                Button(
                    onClick = onTriggerPrioritize,
                    enabled = aiState !is AiState.Loading && incompleteCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_optimize_trigger"),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorAiScore),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (aiState is AiState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("درحال تحلیل تسک‌ها...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "بهینه‌سازی")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (incompleteCount > 0) "اولویت‌بندی هوشمند کارهای فعال ($incompleteCount)" else "هیچ تسک فعالی فرود نیامده",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // State displays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            when (aiState) {
                is AiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ColorAiScore, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "هوش مصنوعی در حال چیدمان اولویت‌ها بر اساس منطق است...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is AiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, PriorityHigh, RoundedCornerShape(16.dp))
                            .background(PriorityHigh.copy(alpha = 0.05f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "خطا", tint = PriorityHigh, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "اوه! خطایی رخ داد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PriorityHigh
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onResetState,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("باشه")
                        }
                    }
                }
                else -> {
                    // Success or Idle -> Show the advisor board compiled
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 50.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "📋 یادداشت راهبردی روزانه شما",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = ColorAiScore
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (aimMessage != null) {
                                            val lastDate = SimpleDateFormat("HH:mm", Locale.US).format(Date(aimMessage.lastUpdated))
                                            Text(
                                                text = "آخرین بروزرسانی: $lastDate",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = aimMessage?.overallAdvice ?: "هنوز تحلیلی تولید نشده است. برای تولید تحلیل و سازمان‌دهی استراتژیک، دکمه بالا را کلیک کنید تا هوش مصنوعی به داده‌های شما شکل اجرایی بدهد.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }

                        // Static pro tips card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🎯 چند توصیه برای مدیریت بهتر زمان:",
                                        fontWeight = FontWeight.Bold,
                                        color = DarkSecondary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    BulletPoint("تسک‌های سنگین را به بخش‌های کوچک چند دقیقه‌ای تقسیم کنید.")
                                    BulletPoint("با استفاده از تب تمرکز، از شبکه‌های اجتماعی دور بمانید.")
                                    BulletPoint("هر روز صبح تسک‌های تازه اضافه کنید و دوباره اولویت برتر را با هوش بگیرید.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "• ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkSecondary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            lineHeight = 18.sp
        )
    }
}

// --- TAB 4: Performance / Stats ---
@Composable
fun StatsTab(
    stats: TaskViewModel.Stats,
    tasks: List<Task>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📊 آمار و گزارش عملکرد شما",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val total = tasks.size
        if (total == 0) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "آمار عملکرد پس از وارد کردن اولین تسک‌ها اینجا نمایش داده می‌شود.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        // Circular completion progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "میزان پیشرفت کلی",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "شما ${stats.completedCount} کار از مجموع ${stats.totalCount} کار را با موفقیت تکمیل کرده‌اید.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    val brush = Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, DarkSecondary))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.Gray.copy(alpha = 0.15f),
                            style = Stroke(width = 8.dp.toPx())
                        )
                        drawArc(
                            brush = brush,
                            startAngle = -90f,
                            sweepAngle = 360f * stats.completionRate,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(stats.completionRate * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Stats grid numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("کارهای فعال باقی‌مانده", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${stats.totalCount - stats.completedCount}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PriorityHigh)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("تسک‌های با اولویت بالا", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${stats.highPriorityCount}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PriorityMedium)
                }
            }
        }

        // Category distribution panel
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📂 پراکندگی کارها بر اساس دسته‌بندی",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(stats.categoryDistribution.toList()) { (cat, count) ->
                        val completionPercent = stats.categoryCompletionRate[cat] ?: 0f
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(cat))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = cat, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "$count تسک (تکمیل شده: ${(completionPercent * 100).toInt()}% )",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Bar Layout progress indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                val percentageShare = count.toFloat() / stats.totalCount.toFloat()
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(percentageShare)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(getCategoryColor(cat))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ADD TASK DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdd: (title: String, desc: String, category: String, duration: Int, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("کار") }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }
    var durationEstim by remember { mutableStateOf(30) }

    val priorities = listOf("HIGH" to "بالا 🚨", "MEDIUM" to "متوسط ⚠️", "LOW" to "پایین 🟩")
    val categories = listOf("کار", "شخصی", "آموزش", "سلامتی", "سایر")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_task_dialog")
                .shadow(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📝 تعریف تسک جدید",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Title field Group
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان کار") },
                    placeholder = { Text("مثال: تکمیل گزارش طراحی") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Description field Group
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیحات بیشتر (اختیاری)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                // Category chips selector
                Column {
                    Text("دسته‌بندی:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            InputChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = getCategoryColor(cat).copy(alpha = 0.15f),
                                    selectedLabelColor = getCategoryColor(cat)
                                )
                            )
                        }
                    }
                }

                // Priority Row selectors
                Column {
                    Text("اولویت اولیه:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        priorities.forEach { (key, display) ->
                            val (_, colorCol) = getPriorityMeta(key)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedPriority == key) colorCol.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(1.dp, if (selectedPriority == key) colorCol else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .clickable { selectedPriority = key }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = display, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedPriority == key) colorCol else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // Estim Minutes duration slider
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("مدت زمان تمرکز:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text("$durationEstim دقیقه", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = durationEstim.toFloat(),
                        onValueChange = { durationEstim = it.toInt() },
                        valueRange = 10f..120f,
                        steps = 10
                    )
                }

                // Actions buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("لغو")
                    }
                    Button(
                        onClick = {
                            if (title.trim().isNotEmpty()) {
                                onTaskAdd(title, desc, selectedCategory, durationEstim, selectedPriority)
                            }
                        },
                        enabled = title.trim().isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_add_task_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت تسک")
                    }
                }
            }
        }
    }
}

// --- UTILS HELPER FUNCTIONS ---
fun getCategoryColor(category: String): Color {
    return when (category) {
        "کار" -> ColorCategoryWork
        "شخصی" -> ColorCategoryPersonal
        "آموزش" -> ColorCategoryEducation
        "سلامتی" -> ColorCategoryHealth
        else -> ColorCategoryOther
    }
}

fun getPriorityMeta(priority: String): Pair<String, Color> {
    return when (priority) {
        "HIGH" -> "بالا 🚨" to PriorityHigh
        "MEDIUM" -> "متوسط ⚠️" to PriorityMedium
        else -> "پایین 🟩" to PriorityLow
    }
}
