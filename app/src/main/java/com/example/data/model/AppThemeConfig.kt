package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val id: String, val displayName: String, val isDark: Boolean) {
    CLASSIC("classic", "Claro Clássico", false),
    NORDIC("nordic", "Claro Nórdico", false),
    MONOCHROME_LIGHT("monochrome-light", "Claro P&B", false),
    MONOCHROME_DARK("monochrome-dark", "Escuro P&B", true),
    DARK("dark", "Escuro Moderno", true),
    OLED("oled", "Escuro Total", true),
    NEON_CYAN("neon-cyan", "Escuro Futuro", true);

    companion object {
        fun fromId(id: String): AppThemeMode {
            return entries.firstOrNull { it.id == id } ?: CLASSIC
        }
    }
}

data class AccentOption(
    val id: String,
    val name: String,
    val color: Color
)

val ACCENT_OPTIONS = listOf(
    AccentOption("royal", "Azul Royal", Color(0xFF3B82F6)),
    AccentOption("indigo", "Azul Índigo", Color(0xFF4F46E5)),
    AccentOption("cobalto", "Azul Cobalto", Color(0xFF2563EB)),
    AccentOption("celeste", "Azul Celeste", Color(0xFF0EA5E9)),
    AccentOption("emerald", "Verde Esmeralda", Color(0xFF10B981)),
    AccentOption("violet", "Roxo Elétrico", Color(0xFF8B5CF6)),
    AccentOption("rose", "Rosa Moderno", Color(0xFFF43F5E))
)
