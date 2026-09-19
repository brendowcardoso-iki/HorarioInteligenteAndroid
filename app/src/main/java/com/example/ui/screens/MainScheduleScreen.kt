package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityCategory
import com.example.data.model.AppThemeMode
import com.example.data.model.ScheduleItem
import com.example.ui.components.AccordionSidebar
import com.example.ui.components.ActiveActivityCard
import com.example.ui.components.ActivityDetailMainView
import com.example.ui.components.AddEditActivityDialog
import com.example.ui.components.FeedbackDialog
import com.example.ui.components.GeminiCopilotSheet
import com.example.ui.components.GoogleDriveSyncDialog
import com.example.ui.components.ImportRoutineDialog
import com.example.ui.components.ProtocolDetailSheet
import com.example.ui.components.SettingsThemeDialog
import com.example.ui.components.TimelineListItem
import com.example.ui.components.VisualCalendarTimeline
import com.example.ui.viewmodel.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScheduleScreen(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val currentTimeStr by viewModel.currentTimeStr.collectAsState()
    val currentTimeFullStr by viewModel.currentTimeFullStr.collectAsState()
    val currentDateStr by viewModel.currentDateStr.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val accentColorHex by viewModel.accentColorHex.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val accentColor = remember(accentColorHex) {
        try {
            Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (e: Exception) {
            Color(0xFF3B82F6)
        }
    }

    val allItems by viewModel.allItems.collectAsState()
    val displayedItems by viewModel.displayedItems.collectAsState()
    val connectedAgendas by viewModel.connectedAgendas.collectAsState()
    val currentActivity by viewModel.currentActivity.collectAsState()
    val focusedActivity by viewModel.focusedActivity.collectAsState()
    val currentProgress by viewModel.currentProgress.collectAsState()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsState()
    val allFeedbacks by viewModel.allFeedbacks.collectAsState()

    val isVisualCalendar by viewModel.isVisualCalendar.collectAsState()
    val isCopilotOpen by viewModel.isCopilotOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isImportOpen by viewModel.isImportOpen.collectAsState()
    val isFeedbackOpen by viewModel.isFeedbackOpen.collectAsState()
    val isAddEditOpen by viewModel.isAddEditOpen.collectAsState()
    val isProtocolSheetOpen by viewModel.isProtocolSheetOpen.collectAsState()
    val isScheduleExpanded by viewModel.isScheduleExpanded.collectAsState()
    val isDriveSyncOpen by viewModel.isDriveSyncOpen.collectAsState()

    var showOnlyRemaining by remember { mutableStateOf(false) }

    val copilotMessages by viewModel.copilotMessages.collectAsState()
    val isCopilotLoading by viewModel.isCopilotLoading.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importMessage by viewModel.importMessage.collectAsState()

    val activityToEdit by viewModel.activityToEdit.collectAsState()
    val activityForFeedback by viewModel.activityForFeedback.collectAsState()
    val effectiveDisplayedActivity by viewModel.effectiveDisplayedActivity.collectAsState()

    val activeCustomization by viewModel.activeCustomization.collectAsState()
    val activeSteps by viewModel.activeSteps.collectAsState()

    val protocolSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val copilotSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = LocalContext.current

    var isAccordionExpanded by remember { mutableStateOf(true) }
    var isThemeDropdownOpen by remember { mutableStateOf(false) }

    val formattedTopDate = remember(currentDateStr) {
        try {
            val sdf = java.text.SimpleDateFormat("EEE, dd 'DE' MMM", java.util.Locale("pt", "BR"))
            val raw = sdf.format(java.util.Date()).uppercase()
            val parts = raw.split(" ")
            if (parts.size >= 4) {
                val dayOfWeek = parts[0].trimEnd(',').replace(".", "") + "."
                val day = parts[1]
                val de = parts[2]
                val month = parts[3].trimEnd('.').replace(".", "") + "."
                "$dayOfWeek, $day $de $month"
            } else {
                raw
            }
        } catch (e: Exception) {
            "SÁB., 19 DE SET."
        }
    }

    val nextActivity = remember(allItems, currentTimeStr) {
        allItems.firstOrNull { it.startTime > currentTimeStr }
    }

    val remainingItems = remember(allItems, currentTimeStr) {
        allItems.filter { it.endTime > currentTimeStr }
    }

    val scheduleItemsToDisplay = remember(showOnlyRemaining, remainingItems, allItems) {
        if (showOnlyRemaining) remainingItems else allItems
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { isAccordionExpanded = !isAccordionExpanded },
                        modifier = Modifier.testTag("btn_toggle_accordion")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Sanfona",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Text(
                        text = "Horário Inteligente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    // Date display directly without rounded rectangle
                    Text(
                        text = formattedTopDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
                    )

                    // Time display directly without rounded rectangle
                    Text(
                        text = currentTimeStr,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Refresh schedule seed / Docs
                    IconButton(
                        onClick = { viewModel.loadUserDocsExample() },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_refresh_schedule")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar Agenda",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 3-dots Dropdown Menu
                    Box {
                        IconButton(
                            onClick = { isThemeDropdownOpen = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_more_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Mais opções e temas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isThemeDropdownOpen,
                            onDismissRequest = { isThemeDropdownOpen = false }
                        ) {
                            Text(
                                text = "TEMAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            AppThemeMode.entries.forEach { mode ->
                                val isSelected = themeMode == mode
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(text = mode.displayName, fontSize = 13.sp)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setTheme(mode)
                                        isThemeDropdownOpen = false
                                    }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Lixeira", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.resetToDefaults()
                                    isThemeDropdownOpen = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Uploads de Agenda", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.FileUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.isImportOpen.value = true
                                    isThemeDropdownOpen = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Sair da conta",
                                        fontSize = 13.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    viewModel.resetToDefaults()
                                    isThemeDropdownOpen = false
                                }
                            )
                        }
                    }

                    // "Conectar" button: Blue button with Docs icon, rounded ends (CircleShape)
                    Button(
                        onClick = { viewModel.isDriveSyncOpen.value = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_conectar_drive")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Conectar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Add Activity FAB (Sem o botão de IA acima, cantos quadrados)
            FloatingActionButton(
                onClick = {
                    viewModel.activityToEdit.value = null
                    viewModel.isAddEditOpen.value = true
                },
                containerColor = accentColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("btn_add_activity")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Atividade")
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Left Side: Accordion Sidebar
            AccordionSidebar(
                isExpanded = isAccordionExpanded,
                onToggleExpand = { isAccordionExpanded = !isAccordionExpanded },
                userName = userName,
                currentActivity = currentActivity,
                selectedActivity = effectiveDisplayedActivity,
                allItems = displayedItems,
                onSelectActivity = { item ->
                    viewModel.setFocusedActivity(item)
                    if (isVisualCalendar) {
                        viewModel.isVisualCalendar.value = false
                    }
                },
                onOpenCalendar = {
                    viewModel.isVisualCalendar.value = !isVisualCalendar
                },
                onOpenSettings = {
                    isThemeDropdownOpen = true
                },
                connectedAgendas = connectedAgendas,
                onToggleAgenda = { agenda ->
                    viewModel.toggleAgendaChecked(agenda.id)
                },
                onRemoveAgenda = { agenda ->
                    viewModel.removeConnectedAgenda(agenda.id)
                    android.widget.Toast.makeText(
                        context,
                        "Arquivo/Agenda \"${agenda.name}\" removido",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            )

            // Right Side: Main Content View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (isVisualCalendar) {
                    VisualCalendarTimeline(
                        scheduleItems = allItems,
                        currentTimeStr = currentTimeStr,
                        selectedCategory = selectedCategoryFilter,
                        accentColor = accentColor,
                        onSelectCategory = { viewModel.selectCategoryFilter(it) },
                        onSelectActivity = {
                            viewModel.setFocusedActivity(it)
                            viewModel.isVisualCalendar.value = false
                        },
                        onClose = { viewModel.isVisualCalendar.value = false },
                        onPickFromDrive = { viewModel.isDriveSyncOpen.value = true },
                        onOpenImportText = { viewModel.isImportOpen.value = true },
                        onLoadTestExample = { viewModel.loadUserDocsExample() },
                        onAddActivity = {
                            viewModel.activityToEdit.value = null
                            viewModel.isAddEditOpen.value = true
                        },
                        onAddActivityAtHour = { hour ->
                            viewModel.openAddActivityAtHour(hour)
                        }
                    )
                } else {
                    ActivityDetailMainView(
                        activity = effectiveDisplayedActivity,
                        customization = activeCustomization,
                        steps = activeSteps,
                        onSaveObjective = { newObjective ->
                            effectiveDisplayedActivity?.let {
                                viewModel.saveStrategicObjective(it, newObjective)
                            }
                        },
                        onAddStep = { title, desc ->
                            effectiveDisplayedActivity?.let {
                                viewModel.addStep(it, title, desc)
                            }
                        },
                        onToggleStep = { index ->
                            effectiveDisplayedActivity?.let {
                                viewModel.toggleStep(it, index)
                            }
                        },
                        onDeleteStep = { index ->
                            effectiveDisplayedActivity?.let {
                                viewModel.deleteStep(it, index)
                            }
                        },
                        onConnectDrive = { viewModel.isDriveSyncOpen.value = true },
                        onLoadTestExample = { viewModel.loadUserDocsExample() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet: Full Protocol View
    if (isProtocolSheetOpen && effectiveDisplayedActivity != null) {
        ProtocolDetailSheet(
            activity = effectiveDisplayedActivity!!,
            customization = activeCustomization,
            steps = activeSteps,
            sheetState = protocolSheetState,
            accentColor = accentColor,
            onDismiss = { viewModel.isProtocolSheetOpen.value = false },
            onSaveObjective = { newObjective ->
                viewModel.saveStrategicObjective(effectiveDisplayedActivity!!, newObjective)
            },
            onAddStep = { title, desc ->
                viewModel.addStep(effectiveDisplayedActivity!!, title, desc)
            },
            onToggleStep = { idx ->
                viewModel.toggleStep(effectiveDisplayedActivity!!, idx)
            },
            onDeleteStep = { idx ->
                viewModel.deleteStep(effectiveDisplayedActivity!!, idx)
            },
            onTriggerFeedback = { act ->
                viewModel.activityForFeedback.value = act
                viewModel.isFeedbackOpen.value = true
            },
            onEditActivity = { act ->
                viewModel.activityToEdit.value = act
                viewModel.isAddEditOpen.value = true
            }
        )
    }

    // Modal Bottom Sheet: Gemini AI Copilot
    if (isCopilotOpen) {
        GeminiCopilotSheet(
            messages = copilotMessages,
            isLoading = isCopilotLoading,
            sheetState = copilotSheetState,
            accentColor = accentColor,
            onSendMessage = { prompt ->
                viewModel.sendCopilotMessage(prompt)
            },
            onClearHistory = {
                viewModel.clearCopilotHistory()
            },
            onDismiss = { viewModel.isCopilotOpen.value = false }
        )
    }

    // Feedback Dialog
    if (isFeedbackOpen && activityForFeedback != null) {
        FeedbackDialog(
            activity = activityForFeedback!!,
            accentColor = accentColor,
            onDismiss = { viewModel.isFeedbackOpen.value = false },
            onSubmit = { rating, comment ->
                viewModel.submitFeedback(activityForFeedback!!, rating, comment)
            }
        )
    }

    // Add / Edit Activity Dialog
    if (isAddEditOpen) {
        AddEditActivityDialog(
            activityToEdit = activityToEdit,
            accentColor = accentColor,
            onDismiss = { viewModel.isAddEditOpen.value = false },
            onSave = { id, start, end, title, instructions, category ->
                viewModel.saveActivity(id, start, end, title, instructions, category)
            }
        )
    }

    // Import Routine Dialog
    if (isImportOpen) {
        ImportRoutineDialog(
            isLoading = isImporting,
            statusMessage = importMessage,
            accentColor = accentColor,
            onDismiss = { viewModel.isImportOpen.value = false },
            onImport = { text, replace ->
                viewModel.importTextRoutine(text, replace)
            }
        )
    }

    // Settings & Theme Dialog
    if (isSettingsOpen) {
        SettingsThemeDialog(
            currentTheme = themeMode,
            currentAccentHex = accentColorHex,
            currentUserName = userName,
            feedbacks = allFeedbacks,
            onSelectTheme = { viewModel.setTheme(it) },
            onSelectAccent = { viewModel.setAccent(it) },
            onSaveUserName = { viewModel.setProfileName(it) },
            onResetRoutine = { viewModel.resetToDefaults() },
            onDeleteFeedback = { viewModel.deleteFeedback(it) },
            onOpenDriveSync = { viewModel.isDriveSyncOpen.value = true },
            onDismiss = { viewModel.isSettingsOpen.value = false }
        )
    }

    // Google Drive Sync Dialog
    if (isDriveSyncOpen) {
        GoogleDriveSyncDialog(
            viewModel = viewModel,
            accentColor = accentColor,
            onDismiss = { viewModel.isDriveSyncOpen.value = false }
        )
    }
}
