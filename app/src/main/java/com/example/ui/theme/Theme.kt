package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeMode

@Composable
fun HorarioInteligenteTheme(
    themeMode: AppThemeMode = AppThemeMode.CLASSIC,
    accentColor: Color = Color(0xFF3B82F6),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.CLASSIC -> lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = accentColor,
            background = ClassicBg,
            onBackground = ClassicText,
            surface = ClassicSurface,
            onSurface = ClassicText,
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = ClassicTextSub,
            outline = Color(0xFFCBD5E1)
        )
        AppThemeMode.NORDIC -> lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = accentColor,
            background = NordicBg,
            onBackground = NordicText,
            surface = NordicSurface,
            onSurface = NordicText,
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = NordicTextSub,
            outline = Color(0xFFE2E8F0)
        )
        AppThemeMode.MONOCHROME_LIGHT -> lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE4E4E7),
            onPrimaryContainer = Color.Black,
            background = MonoLightBg,
            onBackground = MonoLightText,
            surface = MonoLightSurface,
            onSurface = MonoLightText,
            surfaceVariant = Color(0xFFE4E4E7),
            onSurfaceVariant = MonoLightTextSub,
            outline = Color(0xFFD4D4D8)
        )
        AppThemeMode.DARK -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.2f),
            onPrimaryContainer = Color.White,
            background = DarkBg,
            onBackground = DarkText,
            surface = DarkSurface,
            onSurface = DarkText,
            surfaceVariant = Color(0xFF282C37),
            onSurfaceVariant = DarkTextSub,
            outline = Color(0xFF374151)
        )
        AppThemeMode.OLED -> darkColorScheme(
            primary = accentColor,
            onPrimary = Color.Black,
            primaryContainer = accentColor.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            background = OledBg,
            onBackground = OledText,
            surface = OledSurface,
            onSurface = OledText,
            surfaceVariant = Color(0xFF1E1E1E),
            onSurfaceVariant = OledTextSub,
            outline = Color(0xFF27272A)
        )
        AppThemeMode.MONOCHROME_DARK -> darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF27272A),
            onPrimaryContainer = Color.White,
            background = MonoDarkBg,
            onBackground = MonoDarkText,
            surface = MonoDarkSurface,
            onSurface = MonoDarkText,
            surfaceVariant = Color(0xFF27272A),
            onSurfaceVariant = MonoDarkTextSub,
            outline = Color(0xFF3F3F46)
        )
        AppThemeMode.NEON_CYAN -> darkColorScheme(
            primary = NeonCyanPrimary,
            onPrimary = Color(0xFF003840),
            primaryContainer = NeonCyanPrimary.copy(alpha = 0.2f),
            onPrimaryContainer = NeonCyanPrimary,
            background = NeonCyanBg,
            onBackground = NeonCyanText,
            surface = NeonCyanSurface,
            onSurface = NeonCyanText,
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = NeonCyanTextSub,
            outline = Color(0xFF1E293B)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
