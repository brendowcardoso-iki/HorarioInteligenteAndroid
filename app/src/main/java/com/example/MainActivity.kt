package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.MainScheduleScreen
import com.example.ui.theme.HorarioInteligenteTheme
import com.example.ui.viewmodel.ScheduleViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val accentHex by viewModel.accentColorHex.collectAsState()

            val accentColor = remember(accentHex) {
                try {
                    Color(android.graphics.Color.parseColor(accentHex))
                } catch (e: Exception) {
                    Color(0xFF3B82F6)
                }
            }

            HorarioInteligenteTheme(
                themeMode = themeMode,
                accentColor = accentColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScheduleScreen(viewModel = viewModel)
                }
            }
        }
    }
}
