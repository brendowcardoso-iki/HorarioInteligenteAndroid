package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ActivityCategory
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun parseScheduleFromText(text: String, sourceFileName: String = "Google Docs"): List<ScheduleItem> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "No valid Gemini API key found. Falling back to local smart parser.")
            return@withContext fallbackLocalParser(text, sourceFileName)
        }

        try {
            val systemPrompt = """
                Você é um assistente especializado em extrair cronogramas de rotinas e horários de textos em português vindos de documentos como Google Docs.
                Analise o texto fornecido pelo usuário e extraia as atividades com seus horários.
                Regras:
                1. Extraia todas as atividades que possuam horários de início e término (ex: '14h a 15h e tiver uma descrição janta' -> startTime: '14:00', endTime: '15:00', activity: 'Janta', category: 'Alimentação'; '17h até 18h com a descrição janta' -> startTime: '17:00', endTime: '18:00', activity: 'Janta', category: 'Alimentação').
                2. Suporte formatos como '14h a 15h', '14h às 15h', '14:00 - 15:00', '17h até 18h com a descrição janta'.
                3. Formate startTime e endTime estritamente como HH:mm (24 horas, ex: 08:30, 14:00, 17:00).
                4. O campo "activity" deve conter o nome/descrição da atividade com a primeira letra em maiúscula (ex: 'Janta', 'Almoço', 'Estudos').
                5. O campo "instructions" deve conter detalhes, observações ou instruções se houver.
                6. O campo "category" deve ser uma destas: Trabalho, Estudo, Descanso, Lazer, Alimentação, Saúde, Higiene, Fixa, Variável, Outros.
                7. Retorne estritamente um array JSON com objetos contendo: startTime, endTime, activity, instructions, category.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemPrompt\n\nTexto para análise:\n$text")
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error: ${response.code} ${response.message}")
                return@withContext fallbackLocalParser(text, sourceFileName)
            }

            val responseBody = response.body?.string() ?: return@withContext fallbackLocalParser(text, sourceFileName)
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext fallbackLocalParser(text, sourceFileName)
            if (candidates.length() == 0) return@withContext fallbackLocalParser(text, sourceFileName)

            val textResult = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val items = parseJsonToScheduleItems(textResult, sourceFileName)
            if (items.isEmpty()) fallbackLocalParser(text, sourceFileName) else items
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseScheduleFromText: ${e.message}", e)
            fallbackLocalParser(text, sourceFileName)
        }
    }

    private fun parseJsonToScheduleItems(rawJson: String, sourceFileName: String): List<ScheduleItem> {
        val clean = rawJson.replace("```json", "").replace("```", "").trim()
        val items = mutableListOf<ScheduleItem>()
        try {
            val jsonArray = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else {
                val obj = JSONObject(clean)
                obj.optJSONArray("schedule") ?: JSONArray()
            }

            for (i in 0 until jsonArray.length()) {
                val itemObj = jsonArray.getJSONObject(i)
                val start = itemObj.optString("startTime", "08:00")
                val end = itemObj.optString("endTime", "09:00")
                val activity = itemObj.optString("activity", "Atividade")
                val instructions = itemObj.optString("instructions", "")
                val category = itemObj.optString("category", "Outros")

                items.add(
                    ScheduleItem(
                        startTime = formatTime(start),
                        endTime = formatTime(end),
                        activity = activity.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        instructions = instructions,
                        category = category,
                        sourceFile = sourceFileName
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing json array: ${e.message}")
        }
        return items.sortedBy { it.startTime }
    }

    private fun formatTime(timeStr: String): String {
        val clean = timeStr.trim().lowercase()
        if (clean.contains("h")) {
            val parts = clean.split("h")
            val h = parts[0].toIntOrNull() ?: 0
            val mStr = parts.getOrNull(1)?.trim() ?: ""
            val m = if (mStr.isEmpty()) 0 else mStr.toIntOrNull() ?: 0
            return "%02d:%02d".format(h.coerceIn(0, 23), m.coerceIn(0, 59))
        }
        if (clean.contains(":")) {
            val parts = clean.split(":")
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return "%02d:%02d".format(h.coerceIn(0, 23), m.coerceIn(0, 59))
        }
        val h = clean.toIntOrNull() ?: 8
        return "%02d:00".format(h.coerceIn(0, 23))
    }

    suspend fun askCopilot(
        userQuery: String,
        currentTime: String,
        currentActivity: ScheduleItem?,
        allActivities: List<ScheduleItem>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineCopilotResponse(userQuery, currentTime, currentActivity, allActivities)
        }

        try {
            val scheduleSummary = allActivities.joinToString("\n") {
                "- ${it.startTime} às ${it.endTime}: ${it.activity} (${it.category})"
            }

            val currentInfo = if (currentActivity != null) {
                "${currentActivity.startTime} às ${currentActivity.endTime}: ${currentActivity.activity} (${currentActivity.category})\nInstruções: ${currentActivity.instructions}"
            } else {
                "Nenhuma atividade no momento exato."
            }

            val systemInstruction = """
                Você é o Co-piloto de Rotinas e Protocolos do app Horário Inteligente.
                Ajude o usuário com alta produtividade, tom direto, enérgico, disciplinado e de alta performance militar/científica.
                
                DADOS DO USUÁRIO EM TEMPO REAL:
                - Horário atual: $currentTime
                - Atividade que está acontecendo AGORA:
                $currentInfo
                - Cronograma completo do dia:
                $scheduleSummary
                
                REGRAS:
                1. Responda em Português do Brasil com clareza, formatação rica (tópicos com marcadores, destaques em negrito).
                2. Se o usuário perguntar o que deve fazer agora, responda com foco total na atividade do momento ou na próxima.
                3. Se pedir dicas, ofereça 3 passos de alta intensidade e foco cognitivo.
                4. Seja motivador e conciso.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemInstruction\n\nMensagem do usuário: $userQuery")
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext getOfflineCopilotResponse(userQuery, currentTime, currentActivity, allActivities)
            }

            val responseBody = response.body?.string() ?: return@withContext getOfflineCopilotResponse(userQuery, currentTime, currentActivity, allActivities)
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates") ?: return@withContext getOfflineCopilotResponse(userQuery, currentTime, currentActivity, allActivities)
            if (candidates.length() == 0) return@withContext getOfflineCopilotResponse(userQuery, currentTime, currentActivity, allActivities)

            candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "Não foi possível gerar uma resposta."
        } catch (e: Exception) {
            Log.e(TAG, "Error calling copilot: ${e.message}", e)
            getOfflineCopilotResponse(userQuery, currentTime, currentActivity, allActivities)
        }
    }

    private fun getOfflineCopilotResponse(
        userQuery: String,
        currentTime: String,
        currentActivity: ScheduleItem?,
        allActivities: List<ScheduleItem>
    ): String {
        val q = userQuery.lowercase()
        return when {
            q.contains("agora") || q.contains("fazer") -> {
                if (currentActivity != null) {
                    "🎯 **Sua Missão Agora ($currentTime):**\n\n" +
                            "**${currentActivity.activity}**\n" +
                            "⏰ Período: ${currentActivity.startTime} às ${currentActivity.endTime}\n" +
                            "📂 Categoria: ${currentActivity.category}\n\n" +
                            if (currentActivity.instructions.isNotBlank()) "📋 *Diretriz:* ${currentActivity.instructions}\n\n" else "" +
                            "💡 *Comando tático:* Elimine distrações periféricas e mantenha o foco até o término deste bloco!"
                } else {
                    val next = allActivities.firstOrNull { it.startTime > currentTime }
                    if (next != null) {
                        "⚡ Você está em um intervalo livre! O próximo compromisso é às **${next.startTime}**:\n\n" +
                                "**${next.activity}** (${next.category})\n\n" +
                                "Aproveite para se hidratar e preparar o ambiente de trabalho."
                    } else {
                        "🏁 Todas as atividades programadas para hoje foram concluídas! Prepare o descanso para amanhã."
                    }
                }
            }
            q.contains("resumo") || q.contains("rotina") -> {
                val total = allActivities.size
                val categoriesCount = allActivities.groupBy { it.category }.map { "${it.key}: ${it.value.size}" }.joinToString(", ")
                "📊 **Resumo da sua rotina:**\n\n" +
                        "• Total de blocos: **$total atividades**\n" +
                        "• Distribuição: $categoriesCount\n" +
                        "• Horário atual: **$currentTime**\n\n" +
                        "Mantenha o ritmo de execução linear e registre seus feedbacks ao final de cada bloco."
            }
            q.contains("intervalo") || q.contains("livre") -> {
                "⏳ **Análise de Intervalos Livres:**\n\n" +
                        "Seus blocos estão estruturados sequencialmente. Utilize pequenas transições de 5 a 10 minutos entre blocos intensos para oxigenação cerebral e hidratação."
            }
            else -> {
                "🚀 **Protocolo Operacional em Andamento:**\n\n" +
                        "Estamos em **$currentTime**. Para maximizar sua performance hoje:\n" +
                        "1. Mantenha hidratação constante (35ml por kg corporal).\n" +
                        "2. Execute uma única tarefa prioritária por bloco.\n" +
                        "3. Avalie seu desempenho com o botão de feedback ao finalizar cada ciclo."
            }
        }
    }

    private fun fallbackLocalParser(text: String, sourceFileName: String = "Google Docs"): List<ScheduleItem> {
        val result = mutableListOf<ScheduleItem>()
        val lines = text.lines()

        // Pattern handles: "14h a 15h e tiver uma descrição janta", "14h as 15h almoço", "14:00 - 15:00 reunião", "17h até 18h com a descrição janta"
        val rangePattern = Pattern.compile(
            """^(?:[-*•\d\.\)]\s*)?(?:das?\s+|de\s+)?(\d{1,2}(?:[:hH]\d{1,2}|[hH])?)\s*(?:-|–|—|até\s+as?|até\s+às?|até|ate\s+as?|ate\s+às?|ate|às|as|a\s+|a|ao|to)\s*(\d{1,2}(?:[:hH]\d{1,2}|[hH])?)(?:\s*(?:com\s+a\s+descri[çc][ãa]o|com\s+descri[çc][ãa]o|e\s+tiver\s+uma\s+descri[çc][ãa]o|e\s+tiver\s+a\s+descri[çc][ãa]o|e\s+tiver\s+descri[çc][ãa]o|e\s+a\s+descri[çc][ãa]o|e\s+descri[çc][ãa]o|descri[çc][ãa]o\s*:?|:|-|–|—|•)?\s*(.*))?$""",
            Pattern.CASE_INSENSITIVE
        )

        val singlePattern = Pattern.compile(
            """^(?:[-*•\d\.\)]\s*)?(?:às\s+|as\s+)?(\d{1,2}(?:[:hH]\d{1,2}|[hH])?)(?:\s*(?:com\s+a\s+descri[çc][ãa]o|com\s+descri[çc][ãa]o|e\s+tiver\s+uma\s+descri[çc][ãa]o|e\s+tiver\s+a\s+descri[çc][ãa]o|e\s+tiver\s+descri[çc][ãa]o|e\s+a\s+descri[çc][ãa]o|e\s+descri[çc][ãa]o|descri[çc][ãa]o\s*:?|:|-|–|—|•)?\s*(.*))?$""",
            Pattern.CASE_INSENSITIVE
        )

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val rangeMatcher = rangePattern.matcher(trimmed)
            if (rangeMatcher.find()) {
                val rawStart = rangeMatcher.group(1) ?: "08:00"
                val rawEnd = rangeMatcher.group(2) ?: "09:00"
                var rawTitle = rangeMatcher.group(3)?.trim() ?: ""

                rawTitle = cleanTitle(rawTitle)

                var explicitCat: String? = null
                val catMatch = Regex("""\(([^)]+)\)$""").find(rawTitle)
                if (catMatch != null) {
                    explicitCat = catMatch.groupValues[1].trim()
                    rawTitle = rawTitle.removeSuffix(catMatch.value).trim()
                }

                val title = if (rawTitle.isBlank()) "Atividade" else rawTitle
                val category = explicitCat ?: ActivityCategory.fromString(title).label

                result.add(
                    ScheduleItem(
                        startTime = formatTime(rawStart),
                        endTime = formatTime(rawEnd),
                        activity = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        instructions = "Importado de $sourceFileName",
                        category = category,
                        sourceFile = sourceFileName
                    )
                )
            }
        }

        if (result.isEmpty() && text.isNotBlank()) {
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val singleMatcher = singlePattern.matcher(trimmed)
                if (singleMatcher.find()) {
                    val rawStart = singleMatcher.group(1) ?: continue
                    var rawTitle = singleMatcher.group(2)?.trim() ?: ""
                    rawTitle = cleanTitle(rawTitle)

                    val startFmt = formatTime(rawStart)
                    val (h, m) = startFmt.split(":").map { it.toInt() }
                    val endH = (h + 1) % 24
                    val endFmt = "%02d:%02d".format(endH, m)

                    val title = if (rawTitle.isBlank()) "Atividade" else rawTitle
                    val category = ActivityCategory.fromString(title).label

                    result.add(
                        ScheduleItem(
                            startTime = startFmt,
                            endTime = endFmt,
                            activity = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            instructions = "Importado de $sourceFileName",
                            category = category,
                            sourceFile = sourceFileName
                        )
                    )
                }
            }
        }

        return result.sortedBy { it.startTime }
    }

    private fun cleanTitle(raw: String): String {
        var text = raw.trim()
            .trim('"', '\'', '“', '”', '`')
            .trim()

        val prefixes = listOf(
            "e tiver uma descrição",
            "e tiver uma descricao",
            "e tiver a descrição",
            "e tiver a descricao",
            "e tiver descrição",
            "e tiver descricao",
            "com a descrição",
            "com a descricao",
            "com descrição",
            "com descricao",
            "e a descrição",
            "e a descricao",
            "e descrição",
            "e descricao",
            "descrição:",
            "descricao:",
            "descrição",
            "descricao",
            "com o título",
            "com o titulo",
            "título:",
            "titulo:",
            "título",
            "titulo",
            "com",
            "e",
            ":",
            "-",
            "—",
            "–",
            "•"
        )
        for (p in prefixes) {
            if (text.startsWith(p, ignoreCase = true)) {
                text = text.substring(p.length).trim()
            }
        }
        text = text.trim(':', '-', '—', '–', '•', ' ', '"', '\'')
        return text
    }
}
