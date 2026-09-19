package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class ActivityCategory(
    val label: String,
    val color: Color,
    val hexColor: String
) {
    TRABALHO("Trabalho", Color(0xFF2196F3), "#2196F3"),
    ESTUDO("Estudo", Color(0xFF03A9F4), "#03A9F4"),
    DESCANSO("Descanso", Color(0xFF9C27B0), "#9C27B0"),
    LAZER("Lazer", Color(0xFFE91E63), "#E91E63"),
    ALIMENTACAO("Alimentação", Color(0xFFFF9800), "#FF9800"),
    SAUDE("Saúde", Color(0xFF009688), "#009688"),
    HIGIENE("Higiene", Color(0xFF3F51B5), "#3F51B5"),
    OUTROS("Outros", Color(0xFF607D8B), "#607D8B");

    companion object {
        fun fromString(name: String?): ActivityCategory {
            if (name == null) return OUTROS
            val clean = name.trim().lowercase()
            return entries.firstOrNull { 
                it.label.lowercase() == clean || 
                it.name.lowercase() == clean ||
                clean.contains(it.label.lowercase())
            } ?: when {
                clean.contains("trabalho") || clean.contains("reunião") || clean.contains("reuniao") || clean.contains("projeto") || clean.contains("avanço") || clean.contains("avanco") || clean.contains("office") -> TRABALHO
                clean.contains("estudo") || clean.contains("curso") || clean.contains("livro") || clean.contains("leitura") || clean.contains("aula") -> ESTUDO
                clean.contains("descanso") || clean.contains("descansar") || clean.contains("dormir") || clean.contains("sono") || clean.contains("pausa") || clean.contains("soneca") -> DESCANSO
                clean.contains("lazer") || clean.contains("filme") || clean.contains("série") || clean.contains("serie") || clean.contains("jogo") || clean.contains("jogar") || clean.contains("música") -> LAZER
                clean.contains("janta") || clean.contains("jantar") || clean.contains("almoço") || clean.contains("almoco") || clean.contains("café") || clean.contains("cafe") || clean.contains("comer") || clean.contains("refeição") || clean.contains("refeicao") || clean.contains("lanche") || clean.contains("alimenta") -> ALIMENTACAO
                clean.contains("saúde") || clean.contains("saude") || clean.contains("treino") || clean.contains("academia") || clean.contains("corrida") || clean.contains("exercício") || clean.contains("exercicio") || clean.contains("caminhada") -> SAUDE
                clean.contains("banho") || clean.contains("higiene") || clean.contains("skincare") || clean.contains("dentes") -> HIGIENE
                else -> OUTROS
            }
        }
    }
}
