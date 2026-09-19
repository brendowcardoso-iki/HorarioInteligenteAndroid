package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.ScheduleDao
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityCustomization
import com.example.data.model.ActivityStep
import com.example.data.model.AppThemeMode
import com.example.data.model.DriveItem
import com.example.data.model.DriveItemType
import com.example.data.model.FeedbackItem
import com.example.data.model.ScheduleItem
import com.example.data.remote.FirebaseAuthService
import com.example.data.remote.FirestoreService
import com.example.data.remote.GeminiService
import com.example.data.remote.GoogleDriveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ScheduleRepository(
    private val dao: ScheduleDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(AppThemeMode.fromId(prefs.getString("theme_mode", AppThemeMode.DARK.id) ?: AppThemeMode.DARK.id))
    val themeMode: Flow<AppThemeMode> = _themeMode.asStateFlow()

    private val _accentColor = MutableStateFlow(prefs.getString("accent_color", "#3B82F6") ?: "#3B82F6")
    val accentColor: Flow<String> = _accentColor.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Brendow") ?: "Brendow")
    val userName: Flow<String> = _userName.asStateFlow()

    private val _driveSyncLastTime = MutableStateFlow(prefs.getString("drive_sync_last_time", null))
    val driveSyncLastTime: Flow<String?> = _driveSyncLastTime.asStateFlow()

    private val _driveAutoSync = MutableStateFlow(prefs.getBoolean("drive_auto_sync", true))
    val driveAutoSync: Flow<Boolean> = _driveAutoSync.asStateFlow()

    private val _isDriveLoggedIn = MutableStateFlow(prefs.getBoolean("drive_is_logged_in", false))
    val isDriveLoggedIn: Flow<Boolean> = _isDriveLoggedIn.asStateFlow()

    private val _driveUserEmail = MutableStateFlow(prefs.getString("drive_user_email", "") ?: "")
    val driveUserEmail: Flow<String> = _driveUserEmail.asStateFlow()

    private val _driveUserName = MutableStateFlow(prefs.getString("drive_user_name", "") ?: "")
    val driveUserName: Flow<String> = _driveUserName.asStateFlow()

    val googleDriveService = GoogleDriveService(context)
    val firebaseAuthService = FirebaseAuthService(context)
    val firestoreService = FirestoreService(context)

    private val _driveItems = MutableStateFlow<List<DriveItem>>(emptyList())
    val driveItems: Flow<List<DriveItem>> = _driveItems.asStateFlow()

    suspend fun refreshDriveItems(folderId: String? = null) {
        if (!_isDriveLoggedIn.value) {
            _driveItems.value = emptyList()
            return
        }
        val items = googleDriveService.listFolderItems(folderId)
        _driveItems.value = items
    }

    fun addDriveItem(item: DriveItem) {
        _driveItems.value = listOf(item) + _driveItems.value
    }

    val allScheduleItems: Flow<List<ScheduleItem>> = dao.getAllScheduleItems()
    val allFeedbacks: Flow<List<FeedbackItem>> = dao.getAllFeedbacks()

    init {
        val currentTheme = prefs.getString("theme_mode", null)
        if (currentTheme == null || currentTheme == AppThemeMode.CLASSIC.id) {
            setThemeMode(AppThemeMode.DARK)
        }
    }

    fun setDriveLoggedIn(loggedIn: Boolean, email: String? = null, name: String? = null) {
        prefs.edit().apply {
            putBoolean("drive_is_logged_in", loggedIn)
            if (email != null) putString("drive_user_email", email) else if (!loggedIn) remove("drive_user_email")
            if (name != null) putString("drive_user_name", name) else if (!loggedIn) remove("drive_user_name")
        }.apply()
        _isDriveLoggedIn.value = loggedIn
        if (email != null) _driveUserEmail.value = email else if (!loggedIn) _driveUserEmail.value = ""
        if (name != null) _driveUserName.value = name else if (!loggedIn) _driveUserName.value = ""
        if (!loggedIn) {
            _driveItems.value = emptyList()
        }
    }

    fun setDriveSyncLastTime(time: String) {
        prefs.edit().putString("drive_sync_last_time", time).apply()
        _driveSyncLastTime.value = time
    }

    fun setDriveAutoSync(enabled: Boolean) {
        prefs.edit().putBoolean("drive_auto_sync", enabled).apply()
        _driveAutoSync.value = enabled
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.id).apply()
        _themeMode.value = mode
    }

    fun setAccentColor(hex: String) {
        prefs.edit().putString("accent_color", hex).apply()
        _accentColor.value = hex
    }

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _userName.value = name
    }

    suspend fun insertItem(item: ScheduleItem): Long = withContext(Dispatchers.IO) {
        val newId = dao.insertScheduleItem(item)
        val savedItem = item.copy(id = newId)
        val user = firebaseAuthService.currentUser
        if (user != null) {
            try {
                firestoreService.saveScheduleItem(user.uid, savedItem)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        newId
    }

    suspend fun updateItem(item: ScheduleItem) = withContext(Dispatchers.IO) {
        dao.updateScheduleItem(item)
        val user = firebaseAuthService.currentUser
        if (user != null) {
            try {
                firestoreService.saveScheduleItem(user.uid, item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun deleteItem(item: ScheduleItem) = withContext(Dispatchers.IO) {
        dao.deleteScheduleItem(item)
        val user = firebaseAuthService.currentUser
        if (user != null) {
            try {
                firestoreService.deleteScheduleItem(user.uid, item.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun toggleComplete(item: ScheduleItem) = withContext(Dispatchers.IO) {
        val updated = item.copy(isCompleted = !item.isCompleted)
        dao.updateScheduleItem(updated)
        val user = firebaseAuthService.currentUser
        if (user != null) {
            try {
                firestoreService.saveScheduleItem(user.uid, updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateItemTime(item: ScheduleItem, newStart: String, newEnd: String) = withContext(Dispatchers.IO) {
        val updated = item.copy(startTime = newStart, endTime = newEnd)
        dao.updateScheduleItem(updated)
        val user = firebaseAuthService.currentUser
        if (user != null) {
            try {
                firestoreService.saveScheduleItem(user.uid, updated)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun syncWithFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        val user = firebaseAuthService.currentUser ?: return@withContext Result.failure(IllegalStateException("Usuário não autenticado"))
        try {
            val remoteItemsResult = firestoreService.fetchScheduleItems(user.uid)
            val remoteItems = remoteItemsResult.getOrNull() ?: emptyList()
            if (remoteItems.isNotEmpty()) {
                // Upsert remote items into local Room
                dao.insertAllScheduleItems(remoteItems)
                Result.success(remoteItems.size)
            } else {
                // If remote is empty, upload local items to Firestore
                val localItems = dao.getAllScheduleItemsSync()
                if (localItems.isNotEmpty()) {
                    firestoreService.saveScheduleItems(user.uid, localItems)
                }
                Result.success(localItems.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun getCustomization(key: String): Flow<ActivityCustomization?> = dao.getCustomization(key)

    suspend fun saveObjective(key: String, objective: String, currentSteps: List<ActivityStep>) = withContext(Dispatchers.IO) {
        val stepsJson = serializeSteps(currentSteps)
        dao.saveCustomization(ActivityCustomization(activityKey = key, objective = objective, stepsJson = stepsJson))
    }

    suspend fun saveSteps(key: String, currentObjective: String, steps: List<ActivityStep>) = withContext(Dispatchers.IO) {
        val stepsJson = serializeSteps(steps)
        dao.saveCustomization(ActivityCustomization(activityKey = key, objective = currentObjective, stepsJson = stepsJson))
    }

    suspend fun insertFeedback(activityName: String, startTime: String, endTime: String, feedbackText: String, rating: Int) = withContext(Dispatchers.IO) {
        dao.insertFeedback(
            FeedbackItem(
                activityName = activityName,
                startTime = startTime,
                endTime = endTime,
                feedbackText = feedbackText,
                rating = rating
            )
        )
    }

    suspend fun deleteFeedback(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteFeedback(id)
    }

    suspend fun clearPredefinedDefaultsIfPresent() = withContext(Dispatchers.IO) {
        val alreadyCleared = prefs.getBoolean("predefined_cleared_v2", false)
        if (!alreadyCleared) {
            dao.clearAllScheduleItems()
            prefs.edit().putBoolean("predefined_cleared_v2", true).apply()
        }
    }

    suspend fun ensureUserDocsSeed() = withContext(Dispatchers.IO) {
        val seeded = prefs.getBoolean("video_accordion_seeded_v1", false)
        if (!seeded) {
            dao.clearAllScheduleItems()
            val item1 = ScheduleItem(
                startTime = "15:20",
                endTime = "16:41",
                activity = "Avanços",
                instructions = "",
                category = "Fixa",
                sourceFile = "Google Docs",
                isCompleted = false
            )
            val item2 = ScheduleItem(
                startTime = "19:00",
                endTime = "20:00",
                activity = "Descansar",
                instructions = "",
                category = "Fixa",
                sourceFile = "Google Docs",
                isCompleted = false
            )
            dao.insertScheduleItem(item1)
            dao.insertScheduleItem(item2)
            prefs.edit().putBoolean("video_accordion_seeded_v1", true).putBoolean("user_docs_seeded_v1", true).apply()
        }
    }

    suspend fun loadUserDocsExample() = withContext(Dispatchers.IO) {
        dao.clearAllScheduleItems()
        val item1 = ScheduleItem(
            startTime = "15:20",
            endTime = "16:41",
            activity = "Avanços",
            instructions = "",
            category = "Fixa",
            sourceFile = "Google Docs",
            isCompleted = false
        )
        val item2 = ScheduleItem(
            startTime = "19:00",
            endTime = "20:00",
            activity = "Descansar",
            instructions = "",
            category = "Fixa",
            sourceFile = "Google Docs",
            isCompleted = false
        )
        dao.insertScheduleItem(item1)
        dao.insertScheduleItem(item2)
    }

    suspend fun resetToDefault() = withContext(Dispatchers.IO) {
        dao.clearAllScheduleItems()
    }

    suspend fun removeScheduleSourceOrCategory(name: String) = withContext(Dispatchers.IO) {
        dao.deleteItemsBySourceOrCategory(name)
    }

    suspend fun importFromDocumentText(text: String, sourceFileName: String, replaceExisting: Boolean = true): Int = withContext(Dispatchers.IO) {
        val parsedItems = GeminiService.parseScheduleFromText(text, sourceFileName)
        if (parsedItems.isNotEmpty()) {
            if (replaceExisting) {
                dao.clearAllScheduleItems()
            }
            dao.insertAllScheduleItems(parsedItems)
        }
        parsedItems.size
    }

    suspend fun importFromText(text: String, replaceExisting: Boolean): Int = withContext(Dispatchers.IO) {
        importFromDocumentText(text, "Documento de Rotina", replaceExisting)
    }

    suspend fun askCopilot(userQuery: String, currentTime: String, currentActivity: ScheduleItem?, allActivities: List<ScheduleItem>): String {
        return GeminiService.askCopilot(userQuery, currentTime, currentActivity, allActivities)
    }

    fun parseSteps(stepsJson: String): List<ActivityStep> {
        if (stepsJson.isBlank()) return emptyList()
        val list = mutableListOf<ActivityStep>()
        try {
            val arr = JSONArray(stepsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ActivityStep(
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        completed = obj.optBoolean("completed", false)
                    )
                )
            }
        } catch (e: Exception) {
            // handle fallback
        }
        return list
    }

    fun serializeSteps(steps: List<ActivityStep>): String {
        val arr = JSONArray()
        steps.forEach { step ->
            val obj = JSONObject().apply {
                put("title", step.title)
                put("description", step.description)
                put("completed", step.completed)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    suspend fun exportScheduleAsJson(items: List<ScheduleItem>): String = withContext(Dispatchers.Default) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "Horário Inteligente")
        root.put("exportedAt", System.currentTimeMillis())

        val itemsArr = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("startTime", item.startTime)
                put("endTime", item.endTime)
                put("activity", item.activity)
                put("instructions", item.instructions)
                put("category", item.category)
                put("isCompleted", item.isCompleted)
            }
            itemsArr.put(obj)
        }
        root.put("activities", itemsArr)
        root.toString(2)
    }

    suspend fun importScheduleFromJson(jsonString: String, replaceExisting: Boolean): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val arr = root.optJSONArray("activities") ?: JSONArray()
        val parsedList = mutableListOf<ScheduleItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            parsedList.add(
                ScheduleItem(
                    startTime = obj.optString("startTime", "08:00"),
                    endTime = obj.optString("endTime", "09:00"),
                    activity = obj.optString("activity", "Atividade"),
                    instructions = obj.optString("instructions", ""),
                    category = obj.optString("category", "Trabalho"),
                    isCompleted = obj.optBoolean("isCompleted", false)
                )
            )
        }

        if (parsedList.isNotEmpty()) {
            if (replaceExisting) {
                dao.clearAllScheduleItems()
            }
            dao.insertAllScheduleItems(parsedList)
        }
        parsedList.size
    }

    fun getSeedTextForCloudDoc(documentName: String): String {
        return when {
            documentName.contains("Militar", ignoreCase = true) -> """
                06:00 às 07:00 - Treinamento Físico Militar / Cardio & Resistência
                07:00 às 08:00 - Higiene, Café Nutritivo e Preparação Estratégica
                08:00 às 11:30 - Missões Prioritárias / Foco Tático Profundo
                11:30 às 13:00 - Almoço & Descanso Regenerativo
                13:00 às 15:20 - Engenharia de Software e Projetos de Alta Intensidade
                15:20 às 16:41 - Avanços Operacionais e Análise Científica
                16:41 às 17:15 - Debriefing Diário & Organização de Diretrizes
                17:15 às 18:30 - Estudo Científico e Leitura Técnica
                21:00 às 22:00 - Descompressão e Preparação para o Sono
            """.trimIndent()
            documentName.contains("Protocolos", ignoreCase = true) -> """
                08:30 às 10:00 - Protocolo de Inicialização de Sistemas & Verificação
                10:00 às 12:00 - Execução de Testes e Validação de Algoritmos
                14:00 às 16:00 - Auditoria de Código e Revisão por Pares
                16:00 às 17:30 - Documentação Técnica no Google Docs
                17:30 às 18:30 - Alinhamento e Fechamento Operacional
            """.trimIndent()
            else -> """
                08:00 às 09:30 - Planejamento Diário & Definição de Metas
                09:30 às 12:00 - Bloco de Produção Profunda
                13:30 às 15:30 - Execução de Demandas Críticas
                15:30 às 17:00 - Revisão de Rotinas e Sincronização
                17:00 às 18:00 - Encerramento e Planejamento de Amanhã
            """.trimIndent()
        }
    }
}
