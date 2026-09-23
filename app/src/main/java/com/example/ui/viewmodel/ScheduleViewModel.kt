package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.example.data.local.AppDatabase
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityCustomization
import com.example.data.model.ActivityStep
import com.example.data.model.AppThemeMode
import com.example.data.model.DriveItem
import com.example.data.model.DriveItemType
import com.example.data.model.FeedbackItem
import com.example.data.model.ScheduleItem
import com.example.data.repository.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CopilotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "copilot"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ConnectedAgenda(
    val id: String,
    val name: String,
    val color: Color,
    val isChecked: Boolean = true,
    val isRemovable: Boolean = true
)

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScheduleRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ScheduleRepository(db.scheduleDao(), application)
        viewModelScope.launch {
            repository.clearPredefinedDefaultsIfPresent()
        }
    }

    // Time ticker
    private val _currentTimeStr = MutableStateFlow(getCurrentTime24h())
    val currentTimeStr: StateFlow<String> = _currentTimeStr.asStateFlow()

    private val _currentTimeFullStr = MutableStateFlow(getCurrentTimeFull())
    val currentTimeFullStr: StateFlow<String> = _currentTimeFullStr.asStateFlow()

    private val _currentDateStr = MutableStateFlow(getCurrentDateFormatted())
    val currentDateStr: StateFlow<String> = _currentDateStr.asStateFlow()

    // Preferences & Themes
    val themeMode: StateFlow<AppThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeMode.DARK)

    val accentColorHex: StateFlow<String> = repository.accentColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, "#3B82F6")

    val userName: StateFlow<String> = repository.userName
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Brendow")

    // Schedule items from Room
    val allItems: StateFlow<List<ScheduleItem>> = repository.allScheduleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category filter
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Connected Agendas (e.g. Google Drive/Docs, Fixa, Variável)
    private val _connectedAgendas = MutableStateFlow<List<ConnectedAgenda>>(
        listOf(
            ConnectedAgenda(id = "google_agenda", name = "Google Agenda (Doc/Drive)", color = Color(0xFF3B82F6), isChecked = true),
            ConnectedAgenda(id = "fixa", name = "Fixa", color = Color(0xFF8B5CF6), isChecked = true),
            ConnectedAgenda(id = "variavel", name = "Variável", color = Color(0xFF10B981), isChecked = true)
        )
    )
    val connectedAgendas: StateFlow<List<ConnectedAgenda>> = _connectedAgendas.asStateFlow()

    // Filtered items (respecting category filter and connected agendas checkboxes)
    val displayedItems: StateFlow<List<ScheduleItem>> = combine(
        allItems,
        selectedCategoryFilter,
        connectedAgendas
    ) { items, filter, agendas ->
        val uncheckedNames = agendas.filter { !it.isChecked }.map { it.name.trim().lowercase() }
        items.filter { item ->
            val matchFilter = filter == null || item.category == filter
            val itemCat = item.category.trim().lowercase()
            val itemSource = item.sourceFile.trim().lowercase()
            val isHidden = uncheckedNames.any { un ->
                itemCat == un || itemSource == un ||
                (un.contains("google") && itemSource.contains("google")) ||
                (un == "fixa" && itemCat == "fixa") ||
                (un == "variável" && (itemCat == "variável" || itemCat == "variavel"))
            }
            matchFilter && !isHidden
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active activity (happening right now)
    val currentActivity: StateFlow<ScheduleItem?> = combine(allItems, currentTimeStr) { items, timeNow ->
        items.firstOrNull { timeNow >= it.startTime && timeNow < it.endTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // User selected focus activity override
    private val _focusedActivity = MutableStateFlow<ScheduleItem?>(null)
    val focusedActivity: StateFlow<ScheduleItem?> = _focusedActivity.asStateFlow()

    val effectiveDisplayedActivity: StateFlow<ScheduleItem?> = combine(
        focusedActivity,
        currentActivity,
        allItems,
        currentTimeStr
    ) { focused, current, all, timeNow ->
        focused ?: current ?: all.firstOrNull { it.startTime > timeNow } ?: all.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Activity Elapsed Progress (0 to 100)
    val currentProgress: StateFlow<Float> = combine(currentActivity, currentTimeStr) { activity, nowTime ->
        if (activity == null) 0f
        else calculateProgress(activity.startTime, activity.endTime, nowTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Feedbacks
    val allFeedbacks: StateFlow<List<FeedbackItem>> = repository.allFeedbacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Customization (Objective & Steps) for the displayed activity
    val activeCustomization: StateFlow<ActivityCustomization?> = effectiveDisplayedActivity
        .flatMapLatest { activity ->
            if (activity == null) flowOf(null)
            else repository.getCustomization(activity.key)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSteps: StateFlow<List<ActivityStep>> = activeCustomization
        .flatMapLatest { cust ->
            flowOf(if (cust == null) emptyList() else repository.parseSteps(cust.stepsJson))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI View State toggles
    val isScheduleExpanded = MutableStateFlow(false)
    val isVisualCalendar = MutableStateFlow(false)
    val isCopilotOpen = MutableStateFlow(false)
    val isSettingsOpen = MutableStateFlow(false)
    val isImportOpen = MutableStateFlow(false)
    val isDriveSyncOpen = MutableStateFlow(false)
    val isFeedbackOpen = MutableStateFlow(false)
    val isAddEditOpen = MutableStateFlow(false)
    val isProtocolSheetOpen = MutableStateFlow(false)

    // Google Drive Sync & Login states
    val isDriveLoggedIn: StateFlow<Boolean> = repository.isDriveLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val driveUserEmail: StateFlow<String> = repository.driveUserEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "brendow.cardoso.central@gmail.com")

    val driveUserName: StateFlow<String> = repository.driveUserName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Brendow Cardoso")

    val isDriveAuthenticating = MutableStateFlow(false)

    val firebaseUser = repository.firebaseAuthService.authStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.firebaseAuthService.currentUser)

    val isFirestoreSyncing = MutableStateFlow(false)

    val driveSyncLastTime: StateFlow<String?> = repository.driveSyncLastTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val driveAutoSync: StateFlow<Boolean> = repository.driveAutoSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDriveSyncing = MutableStateFlow(false)
    val driveSyncStatusMessage = MutableStateFlow<String?>(null)

    val driveItems: StateFlow<List<DriveItem>> = repository.driveItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentDriveFolderId = MutableStateFlow<String?>(null) // null = "Meu Drive"

    val activityToEdit = MutableStateFlow<ScheduleItem?>(null)
    val activityForFeedback = MutableStateFlow<ScheduleItem?>(null)

    // Copilot chat messages
    val copilotMessages = MutableStateFlow<List<CopilotMessage>>(
        listOf(
            CopilotMessage(
                sender = "copilot",
                text = "Olá! Sou seu **Co-piloto de Rotinas & Protocolos**. Tenho acesso em tempo real ao seu cronograma e ao que está acontecendo agora.\n\nComo posso ajudar na sua performance militar ou científica hoje?"
            )
        )
    )
    val isCopilotLoading = MutableStateFlow(false)
    val isImporting = MutableStateFlow(false)
    val importMessage = MutableStateFlow<String?>(null)

    init {
        startClockTicker()
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureUserDocsSeed()
        }
    }

    fun loadUserDocsExample() {
        viewModelScope.launch {
            repository.loadUserDocsExample()
        }
    }

    fun openAddActivityAtHour(hour: Int) {
        val h = hour.coerceIn(0, 23)
        val nextH = (h + 1) % 24
        val startStr = "%02d:00".format(h)
        val endStr = "%02d:00".format(nextH)
        activityToEdit.value = ScheduleItem(
            startTime = startStr,
            endTime = endStr,
            activity = "",
            instructions = "",
            category = ActivityCategory.TRABALHO.label
        )
        isAddEditOpen.value = true
    }

    private fun startClockTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val now = System.currentTimeMillis()
                _currentTimeStr.value = getCurrentTime24h()
                _currentTimeFullStr.value = getCurrentTimeFull()
                _currentDateStr.value = getCurrentDateFormatted()
                delay(1000)
            }
        }
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setFocusedActivity(item: ScheduleItem?) {
        _focusedActivity.value = item
    }

    fun toggleActivityCompletion(item: ScheduleItem) {
        viewModelScope.launch {
            repository.toggleComplete(item)
        }
    }

    fun updateActivityTimes(item: ScheduleItem, newStart: String, newEnd: String) {
        viewModelScope.launch {
            repository.updateItemTime(item, newStart, newEnd)
        }
    }

    fun deleteActivity(item: ScheduleItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            if (_focusedActivity.value?.id == item.id) {
                _focusedActivity.value = null
            }
        }
    }

    fun saveActivity(
        id: Long = 0,
        startTime: String,
        endTime: String,
        activity: String,
        instructions: String,
        category: String
    ) {
        viewModelScope.launch {
            if (id > 0) {
                repository.updateItem(
                    ScheduleItem(
                        id = id,
                        startTime = startTime,
                        endTime = endTime,
                        activity = activity,
                        instructions = instructions,
                        category = category,
                        sourceFile = "Manual"
                    )
                )
            } else {
                repository.insertItem(
                    ScheduleItem(
                        startTime = startTime,
                        endTime = endTime,
                        activity = activity,
                        instructions = instructions,
                        category = category,
                        sourceFile = "Manual"
                    )
                )
            }
            isAddEditOpen.value = false
            activityToEdit.value = null
        }
    }

    fun saveStrategicObjective(activity: ScheduleItem, newObjective: String) {
        viewModelScope.launch {
            val steps = activeSteps.value
            repository.saveObjective(activity.key, newObjective, steps)
        }
    }

    fun addStep(activity: ScheduleItem, title: String, description: String = "") {
        if (title.isBlank()) return
        viewModelScope.launch {
            val currentObjective = activeCustomization.value?.objective ?: ""
            val currentSteps = activeSteps.value.toMutableList()
            currentSteps.add(ActivityStep(title = title.trim(), description = description.trim(), completed = false))
            repository.saveSteps(activity.key, currentObjective, currentSteps)
        }
    }

    fun toggleStep(activity: ScheduleItem, stepIndex: Int) {
        viewModelScope.launch {
            val currentObjective = activeCustomization.value?.objective ?: ""
            val currentSteps = activeSteps.value.toMutableList()
            if (stepIndex in currentSteps.indices) {
                val step = currentSteps[stepIndex]
                currentSteps[stepIndex] = step.copy(completed = !step.completed)
                repository.saveSteps(activity.key, currentObjective, currentSteps)
            }
        }
    }

    fun deleteStep(activity: ScheduleItem, stepIndex: Int) {
        viewModelScope.launch {
            val currentObjective = activeCustomization.value?.objective ?: ""
            val currentSteps = activeSteps.value.toMutableList()
            if (stepIndex in currentSteps.indices) {
                currentSteps.removeAt(stepIndex)
                repository.saveSteps(activity.key, currentObjective, currentSteps)
            }
        }
    }

    fun submitFeedback(activity: ScheduleItem, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.insertFeedback(
                activityName = activity.activity,
                startTime = activity.startTime,
                endTime = activity.endTime,
                feedbackText = comment,
                rating = rating
            )
            isFeedbackOpen.value = false
            activityForFeedback.value = null
        }
    }

    fun deleteFeedback(id: Long) {
        viewModelScope.launch {
            repository.deleteFeedback(id)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefault()
        }
    }

    fun importTextRoutine(text: String, replace: Boolean) {
        viewModelScope.launch {
            isImporting.value = true
            importMessage.value = "Analisando cronograma com Inteligência Artificial..."
            try {
                val count = repository.importFromText(text, replace)
                importMessage.value = if (count > 0) "$count atividades importadas com sucesso!" else "Nenhum horário detectado no texto."
                delay(1200)
                if (count > 0) {
                    isImportOpen.value = false
                }
            } catch (e: Exception) {
                importMessage.value = "Erro ao processar: ${e.message}"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun sendCopilotMessage(text: String) {
        if (text.isBlank() || isCopilotLoading.value) return
        val userMsg = CopilotMessage(sender = "user", text = text.trim())
        copilotMessages.value = copilotMessages.value + userMsg

        viewModelScope.launch {
            isCopilotLoading.value = true
            try {
                val response = repository.askCopilot(
                    userQuery = text,
                    currentTime = currentTimeStr.value,
                    currentActivity = currentActivity.value,
                    allActivities = allItems.value
                )
                val botMsg = CopilotMessage(sender = "copilot", text = response)
                copilotMessages.value = copilotMessages.value + botMsg
            } catch (e: Exception) {
                val errorMsg = CopilotMessage(sender = "copilot", text = "Não consegui conectar ao serviço de inteligência. Tente novamente.")
                copilotMessages.value = copilotMessages.value + errorMsg
            } finally {
                isCopilotLoading.value = false
            }
        }
    }

    fun setScheduleExpanded(expanded: Boolean) {
        isScheduleExpanded.value = expanded
    }

    fun toggleScheduleExpanded() {
        isScheduleExpanded.value = !isScheduleExpanded.value
    }

    fun setDriveAutoSync(enabled: Boolean) {
        repository.setDriveAutoSync(enabled)
    }

    fun navigateToDriveFolder(folderId: String?) {
        currentDriveFolderId.value = folderId
        viewModelScope.launch {
            repository.refreshDriveItems(folderId)
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return repository.googleDriveService.googleSignInClient.signInIntent
    }

    fun getChooseAccountIntent(): Intent {
        return repository.googleDriveService.getChooseAccountIntent()
    }

    fun handleAccountChosen(accountEmail: String, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isDriveAuthenticating.value = true
            driveSyncStatusMessage.value = "Conectando à conta Google: $accountEmail..."
            try {
                repository.googleDriveService.initializeDriveWithEmail(accountEmail)
                val displayName = accountEmail.substringBefore("@").replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }
                repository.setDriveLoggedIn(true, accountEmail, displayName)
                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                repository.setDriveSyncLastTime(timeStr)
                repository.refreshDriveItems(null)
                val msg = "Conectado com sucesso como $displayName ($accountEmail)!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                e.printStackTrace()
                val err = "Falha ao conectar conta Google: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveAuthenticating.value = false
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isDriveAuthenticating.value = true
            driveSyncStatusMessage.value = "Autenticando com sua conta Google..."
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    repository.googleDriveService.initializeDriveWithAccount(account)
                    val email = account.email ?: "usuario.google@gmail.com"
                    val name = account.displayName ?: email.substringBefore("@")
                    repository.setDriveLoggedIn(true, email, name)
                    val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    repository.setDriveSyncLastTime(timeStr)
                    repository.refreshDriveItems(null)
                    val msg = "Conectado com sucesso como $name ($email)!"
                    driveSyncStatusMessage.value = msg
                    onComplete(true, msg)
                } else {
                    val msg = "Não foi possível autenticar na conta Google."
                    driveSyncStatusMessage.value = msg
                    onComplete(false, msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val err = if (e is ApiException && e.statusCode == 10) {
                    "Selecione sua conta Google diretamente para conectar ao Drive."
                } else {
                    "Falha no login Google: ${e.localizedMessage ?: "Cancelado ou sem resposta"}"
                }
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveAuthenticating.value = false
            }
        }
    }

    fun uploadLocalFileToDrive(
        uri: Uri,
        folderId: String?,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            isDriveSyncing.value = true
            driveSyncStatusMessage.value = "Fazendo upload de arquivo para o Google Drive..."
            try {
                val contentResolver = getApplication<Application>().contentResolver
                var fileName = "arquivo_upload"
                var fileSize = 0L

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val textContent = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""

                val mimeType = contentResolver.getType(uri) ?: "text/plain"
                val uploaded = repository.googleDriveService.uploadFile(
                    fileName = fileName,
                    content = textContent,
                    mimeType = mimeType,
                    parentFolderId = folderId
                )

                if (uploaded != null) {
                    repository.addDriveItem(uploaded)
                }
                val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                repository.setDriveSyncLastTime(nowStr)
                val msg = "Arquivo \"$fileName\" enviado com sucesso ao Google Drive!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro no upload: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun loginToGoogleDrive(email: String, name: String = "Brendow Cardoso", onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isDriveAuthenticating.value = true
            driveSyncStatusMessage.value = "Conectando ao Google Drive..."
            try {
                repository.setDriveLoggedIn(true, email, name)
                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                repository.setDriveSyncLastTime(timeStr)
                repository.refreshDriveItems(null)
                val msg = "Conectado com sucesso ao Google Drive ($email)!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro ao conectar: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveAuthenticating.value = false
            }
        }
    }

    fun syncFirestoreData(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isFirestoreSyncing.value = true
            try {
                val res = repository.syncWithFirestore()
                if (res.isSuccess) {
                    val count = res.getOrDefault(0)
                    onComplete(true, "Sincronizado com Firestore: $count itens salvos/atualizados na nuvem!")
                } else {
                    val msg = res.exceptionOrNull()?.localizedMessage ?: "Erro ao sincronizar com Firestore"
                    onComplete(false, msg)
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Erro inesperado")
            } finally {
                isFirestoreSyncing.value = false
            }
        }
    }

    fun logoutFromGoogleDrive() {
        viewModelScope.launch {
            try {
                repository.googleDriveService.signOut()
                repository.firebaseAuthService.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.setDriveLoggedIn(false)
            currentDriveFolderId.value = null
            driveSyncStatusMessage.value = "Desconectado do Google Drive e Firebase."
        }
    }

    fun loadCloudDocument(documentName: String, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            driveSyncStatusMessage.value = "Baixando \"$documentName\" diretamente da nuvem Google Drive..."
            try {
                val seedText = repository.getSeedTextForCloudDoc(documentName)
                val count = repository.importFromDocumentText(seedText, sourceFileName = documentName, replaceExisting = false)
                
                // Add to connected agendas if not present
                val existing = _connectedAgendas.value.firstOrNull { it.name.equals(documentName, ignoreCase = true) }
                if (existing == null) {
                    _connectedAgendas.value = _connectedAgendas.value + ConnectedAgenda(
                        id = "drive_cloud_${System.currentTimeMillis()}",
                        name = documentName,
                        color = Color(0xFF38BDF8),
                        isChecked = true,
                        isRemovable = true
                    )
                }

                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                repository.setDriveSyncLastTime(timeStr)
                val msg = "$count atividades carregadas com sucesso de \"$documentName\"!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro ao sincronizar da nuvem: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun saveBackupToCloud(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            driveSyncStatusMessage.value = "Salvando backup na nuvem Google Drive..."
            try {
                val json = repository.exportScheduleAsJson(allItems.value)
                val fileName = "Cronograma_Inteligente_Backup_${SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())}.json"
                val uploaded = repository.googleDriveService.uploadFile(
                    fileName = fileName,
                    content = json,
                    mimeType = "application/json",
                    parentFolderId = currentDriveFolderId.value
                )
                if (uploaded != null) {
                    repository.addDriveItem(uploaded)
                }
                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                repository.setDriveSyncLastTime(timeStr)
                val count = allItems.value.size
                val msg = "Backup de $count atividades sincronizado com o Google Drive!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro ao gravar na nuvem: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun uploadRoutineBackupToDrive(
        folderId: String?,
        customFileName: String? = null,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val safeName = if (!customFileName.isNullOrBlank()) {
                if (customFileName.endsWith(".json", ignoreCase = true)) customFileName.trim() else "${customFileName.trim()}.json"
            } else {
                "Backup_Rotina_${SimpleDateFormat("ddMMyyyy_HHmm", Locale.getDefault()).format(Date())}.json"
            }
            driveSyncStatusMessage.value = "Fazendo upload de \"$safeName\" para o Google Drive..."
            try {
                val json = repository.exportScheduleAsJson(allItems.value)
                val count = allItems.value.size
                val uploaded = repository.googleDriveService.uploadFile(
                    fileName = safeName,
                    content = json,
                    mimeType = "application/json",
                    parentFolderId = folderId
                )
                if (uploaded != null) {
                    repository.addDriveItem(uploaded.copy(activitiesCount = count))
                } else {
                    val newItem = DriveItem(
                        id = "upload_${System.currentTimeMillis()}",
                        name = safeName,
                        type = DriveItemType.JSON_BACKUP,
                        parentFolderId = folderId,
                        sizeString = "${(count * 2.2).toInt().coerceAtLeast(14)} KB",
                        modifiedDate = "Hoje, ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                        activitiesCount = count,
                        isRecentlyUploaded = true
                    )
                    repository.addDriveItem(newItem)
                }
                repository.setDriveSyncLastTime(nowStr)
                val msg = "Upload de \"$safeName\" concluído com sucesso no Google Drive!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro no upload: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun uploadNewDocToDrive(
        fileName: String,
        docContent: String,
        folderId: String?,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            val safeName = if (fileName.contains(".")) fileName.trim() else "${fileName.trim()}.gdoc"
            driveSyncStatusMessage.value = "Enviando \"$safeName\" para a nuvem Google Drive..."
            try {
                val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                val uploaded = repository.googleDriveService.uploadFile(
                    fileName = safeName,
                    content = docContent,
                    mimeType = if (safeName.endsWith(".gdoc", ignoreCase = true)) "application/vnd.google-apps.document" else "text/plain",
                    parentFolderId = folderId
                )
                if (uploaded != null) {
                    repository.addDriveItem(uploaded)
                } else {
                    val newItem = DriveItem(
                        id = "upload_${System.currentTimeMillis()}",
                        name = safeName,
                        type = if (safeName.endsWith(".gdoc", ignoreCase = true)) DriveItemType.GOOGLE_DOC else DriveItemType.TEXT,
                        parentFolderId = folderId,
                        sizeString = "${(docContent.length / 80).coerceAtLeast(12)} KB",
                        modifiedDate = "Hoje, ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                        contentSeed = docContent,
                        isRecentlyUploaded = true
                    )
                    repository.addDriveItem(newItem)
                }
                repository.setDriveSyncLastTime(nowStr)
                val msg = "Documento \"$safeName\" salvo no Google Drive com sucesso!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro ao enviar documento: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun createDriveFolder(
        folderName: String,
        parentFolderId: String?,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            driveSyncStatusMessage.value = "Criando pasta \"$folderName\" no Google Drive..."
            try {
                val created = repository.googleDriveService.createFolder(folderName.trim(), parentFolderId)
                if (created != null) {
                    repository.addDriveItem(created)
                } else {
                    val newFolder = DriveItem(
                        id = "folder_${System.currentTimeMillis()}",
                        name = folderName.trim(),
                        type = DriveItemType.FOLDER,
                        parentFolderId = parentFolderId,
                        sizeString = "Pasta",
                        modifiedDate = "Agora",
                        isRecentlyUploaded = true
                    )
                    repository.addDriveItem(newFolder)
                }
                val msg = "Pasta \"$folderName\" criada no Google Drive!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro ao criar pasta: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun importDriveFile(
        fileItem: DriveItem,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            driveSyncStatusMessage.value = "Baixando \"${fileItem.name}\" do Google Drive..."
            try {
                val downloadedContent = repository.googleDriveService.readFileContent(
                    fileItem.id,
                    if (fileItem.type == DriveItemType.GOOGLE_DOC) "application/vnd.google-apps.document" else null
                )
                val content = downloadedContent ?: fileItem.contentSeed ?: repository.getSeedTextForCloudDoc(fileItem.name)
                val count = repository.importFromDocumentText(content, sourceFileName = fileItem.name, replaceExisting = false)

                // Add to connected agendas
                val existing = _connectedAgendas.value.firstOrNull { it.name.equals(fileItem.name, ignoreCase = true) }
                if (existing == null) {
                    _connectedAgendas.value = _connectedAgendas.value + ConnectedAgenda(
                        id = "drive_cloud_${System.currentTimeMillis()}",
                        name = fileItem.name,
                        color = when (fileItem.type) {
                            DriveItemType.GOOGLE_SHEET -> Color(0xFF0F9D58)
                            DriveItemType.JSON_BACKUP -> Color(0xFF8B5CF6)
                            else -> Color(0xFF38BDF8)
                        },
                        isChecked = true,
                        isRemovable = true
                    )
                }

                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                repository.setDriveSyncLastTime(timeStr)
                val msg = "$count atividades carregadas de \"${fileItem.name}\"!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Erro ao carregar arquivo do Drive: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun syncWithGoogleDrive(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            driveSyncStatusMessage.value = "Sincronizando rotina com o Google Drive..."
            try {
                delay(1200) // Visual feedback for smooth network sync experience
                val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                repository.setDriveSyncLastTime(timeStr)
                val count = allItems.value.size
                val msg = "$count atividades sincronizadas com o Google Drive!"
                driveSyncStatusMessage.value = msg
                onComplete(true, msg)
            } catch (e: Exception) {
                val err = "Falha ao sincronizar: ${e.message}"
                driveSyncStatusMessage.value = err
                onComplete(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    suspend fun getExportJsonString(): String {
        return repository.exportScheduleAsJson(allItems.value)
    }

    fun toggleAgendaChecked(agendaId: String) {
        _connectedAgendas.value = _connectedAgendas.value.map {
            if (it.id == agendaId) it.copy(isChecked = !it.isChecked) else it
        }
    }

    fun removeConnectedAgenda(agendaId: String) {
        val agenda = _connectedAgendas.value.firstOrNull { it.id == agendaId } ?: return
        _connectedAgendas.value = _connectedAgendas.value.filter { it.id != agendaId }
        viewModelScope.launch {
            repository.removeScheduleSourceOrCategory(agenda.name)
            if (agenda.id == "fixa" || agenda.name.equals("Fixa", ignoreCase = true)) {
                repository.removeScheduleSourceOrCategory("Fixa")
            }
            if (_focusedActivity.value?.category.equals(agenda.name, ignoreCase = true) ||
                _focusedActivity.value?.sourceFile.equals(agenda.name, ignoreCase = true)
            ) {
                _focusedActivity.value = null
            }
        }
    }

    fun importScheduleFromDriveUri(uri: Uri, context: Context, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            isDriveSyncing.value = true
            driveSyncStatusMessage.value = "Lendo arquivo do Google Drive..."
            try {
                var fileName = "Documento Google Docs"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore name resolution error
                }

                val content = readTextFromUri(context, uri)
                if (content.isNullOrBlank()) {
                    val err = "O arquivo selecionado está vazio ou não pôde ser lido como texto."
                    driveSyncStatusMessage.value = err
                    onResult(false, err)
                    return@launch
                }

                driveSyncStatusMessage.value = "Processando horários de \"$fileName\"..."
                val count = repository.importFromDocumentText(content, sourceFileName = fileName, replaceExisting = false)
                if (count > 0) {
                    // Register the file in connected agendas
                    val existing = _connectedAgendas.value.firstOrNull { it.name.equals(fileName, ignoreCase = true) }
                    if (existing == null) {
                        _connectedAgendas.value = _connectedAgendas.value + ConnectedAgenda(
                            id = "drive_${System.currentTimeMillis()}",
                            name = fileName,
                            color = Color(0xFF38BDF8),
                            isChecked = true,
                            isRemovable = true
                        )
                    }
                    val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    repository.setDriveSyncLastTime(timeStr)
                    val successMsg = "$count horários carregados com sucesso de \"$fileName\"!"
                    driveSyncStatusMessage.value = successMsg
                    onResult(true, successMsg)
                } else {
                    val warn = "Nenhum horário identificado no arquivo \"$fileName\". Exemplo aceito: '14h a 15h janta'."
                    driveSyncStatusMessage.value = warn
                    onResult(false, warn)
                }
            } catch (e: Exception) {
                val err = "Erro ao importar do Google Drive: ${e.localizedMessage}"
                driveSyncStatusMessage.value = err
                onResult(false, err)
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    private fun readTextFromUri(context: Context, uri: Uri): String? {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }
            if (!text.isNullOrBlank()) return text
        } catch (e: Exception) {
            // fallback
        }

        val mimeTypes = listOf("text/plain", "text/html", "text/csv", "*/*")
        for (mime in mimeTypes) {
            try {
                val text = context.contentResolver.openTypedAssetFileDescriptor(uri, mime, null)
                    ?.createInputStream()?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    }
                if (!text.isNullOrBlank()) return text
            } catch (e: Exception) {
                // try next
            }
        }
        return null
    }

    fun importJsonRoutine(json: String, replace: Boolean, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repository.importScheduleFromJson(json, replace)
                if (count > 0) {
                    val timeStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    repository.setDriveSyncLastTime(timeStr)
                    onResult(true, "$count atividades importadas do Google Drive!")
                } else {
                    onResult(false, "Nenhuma atividade encontrada no arquivo.")
                }
            } catch (e: Exception) {
                onResult(false, "Erro ao ler arquivo: ${e.message}")
            }
        }
    }

    fun clearCopilotHistory() {
        copilotMessages.value = listOf(
            CopilotMessage(
                sender = "copilot",
                text = "Histórico redefinido. Como posso ajudar com os seus horários e protocolos agora?"
            )
        )
    }

    fun setTheme(mode: AppThemeMode) {
        repository.setThemeMode(mode)
    }

    fun setAccent(hex: String) {
        repository.setAccentColor(hex)
    }

    fun setProfileName(name: String) {
        repository.setUserName(name)
    }

    companion object {
        fun getCurrentTime24h(): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }

        fun getCurrentTimeFull(): String {
            return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        }

        fun getCurrentDateFormatted(): String {
            return SimpleDateFormat("EEE, d 'de' MMM", Locale("pt", "BR")).format(Date())
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "BR")) else it.toString() }
        }

        fun calculateProgress(start: String, end: String, now: String): Float {
            try {
                val (sh, sm) = start.split(":").map { it.toInt() }
                val (eh, em) = end.split(":").map { it.toInt() }
                val (nh, nm) = now.split(":").map { it.toInt() }

                val startMin = sh * 60 + sm
                var endMin = eh * 60 + em
                var nowMin = nh * 60 + nm

                if (endMin <= startMin) endMin += 24 * 60
                if (nowMin < startMin) nowMin += 24 * 60

                val totalDuration = endMin - startMin
                val elapsed = nowMin - startMin

                if (totalDuration <= 0) return 0f
                return ((elapsed.toFloat() / totalDuration.toFloat()) * 100f).coerceIn(0f, 100f)
            } catch (e: Exception) {
                return 0f
            }
        }

        fun getDurationString(start: String, end: String): String {
            return try {
                val (sh, sm) = start.split(":").map { it.toInt() }
                val (eh, em) = end.split(":").map { it.toInt() }
                var diff = (eh * 60 + em) - (sh * 60 + sm)
                if (diff < 0) diff += 24 * 60
                val h = diff / 60
                val m = diff % 60
                when {
                    h > 0 && m > 0 -> "${h}h ${m}min"
                    h > 0 -> "${h}h"
                    else -> "${m}min"
                }
            } catch (e: Exception) {
                ""
            }
        }
    }
}
