package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityCustomization
import com.example.data.model.ActivityStep
import com.example.data.model.ScheduleItem

fun calculateDurationString(startTime: String, endTime: String): String {
    val sParts = startTime.split(":")
    val eParts = endTime.split(":")
    if (sParts.size != 2 || eParts.size != 2) return ""
    val sMin = (sParts[0].toIntOrNull() ?: 0) * 60 + (sParts[1].toIntOrNull() ?: 0)
    var eMin = (eParts[0].toIntOrNull() ?: 0) * 60 + (eParts[1].toIntOrNull() ?: 0)
    if (eMin <= sMin) eMin += 1440
    val diff = eMin - sMin
    val h = diff / 60
    val m = diff % 60
    return if (h > 0 && m > 0) "${h}h ${m}min" else if (h > 0) "${h}h" else "${m}min"
}

@Composable
fun ActivityDetailMainView(
    activity: ScheduleItem?,
    customization: ActivityCustomization?,
    steps: List<ActivityStep>,
    onSaveObjective: (String) -> Unit,
    onAddStep: (String, String) -> Unit,
    onToggleStep: (Int) -> Unit,
    onDeleteStep: (Int) -> Unit,
    onConnectDrive: () -> Unit,
    onLoadTestExample: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = (surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f) < 0.5f

    val cardBg = if (isDark) Color(0xFF13151D) else Color.White
    val borderCol = if (isDark) Color(0xFF262B3B) else Color(0xFFE5E7EB)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val textMuted = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    var isEditingObjective by remember { mutableStateOf(false) }
    var objectiveInput by remember(customization, activity) {
        mutableStateOf(customization?.objective ?: "")
    }

    var isAddingStep by remember { mutableStateOf(false) }
    var newStepTitle by remember { mutableStateOf("") }

    if (activity == null) {
        // Empty State: prompt to load docs
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = "Nenhuma atividade selecionada",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Text(
                    text = "Selecione uma atividade no menu à esquerda ou conecte seu arquivo Google Docs para carregar sua agenda.",
                    fontSize = 13.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                Button(
                    onClick = onConnectDrive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = CircleShape
                ) {
                    Text("Conectar Google Docs")
                }
            }
        }
        return
    }

    val durationStr = calculateDurationString(activity.startTime, activity.endTime)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // PERÍODOS SELECIONADOS tag
        Surface(
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, borderCol),
            color = Color.Transparent
        ) {
            Text(
                text = "PERÍODOS SELECIONADOS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Title: activity name (e.g. "Avanços")
        Text(
            text = activity.activity,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        // Time row: "15:20 — 16:41" and duration pill "1h 21min"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, borderCol),
                color = Color.Transparent
            ) {
                Text(
                    text = "${activity.startTime} — ${activity.endTime}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            if (durationStr.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = if (isDark) Color(0xFF1E2433) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = durationStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Section: DIRETRIZ DA ATIVIDADE (AI)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "DIRETRIZ DA ATIVIDADE (AI)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textMuted
            )

            Surface(
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, borderCol),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = activity.instructions.ifEmpty { "Nenhuma descrição adicional disponível." },
                        fontSize = 12.sp,
                        color = if (activity.instructions.isEmpty()) textMuted else textSecondary,
                        fontStyle = if (activity.instructions.isEmpty()) FontStyle.Italic else FontStyle.Normal,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Section: OBJETIVO ESTRATÉGICO
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OBJETIVO ESTRATÉGICO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = textMuted
                )

                IconButton(
                    onClick = { isEditingObjective = !isEditingObjective },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar objetivo estratégico",
                        tint = textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isEditingObjective) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = objectiveInput,
                        onValueChange = { objectiveInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escreva o objetivo estratégico...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = borderCol
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Button(
                            onClick = { isEditingObjective = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Cancelar", color = textMuted, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                onSaveObjective(objectiveInput)
                                isEditingObjective = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Salvar", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                val currentObj = customization?.objective
                Text(
                    text = if (currentObj.isNullOrBlank()) "Defina um objetivo estratégico para este período." else currentObj,
                    fontSize = 13.sp,
                    fontStyle = if (currentObj.isNullOrBlank()) FontStyle.Italic else FontStyle.Normal,
                    color = if (currentObj.isNullOrBlank()) textMuted else textPrimary,
                    modifier = Modifier.clickable { isEditingObjective = true }
                )
            }
        }

        // Section: PASSOS DA EXECUÇÃO
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PASSOS DA EXECUÇÃO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = textMuted
                )

                IconButton(
                    onClick = { isAddingStep = !isAddingStep },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar passo",
                        tint = textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isAddingStep) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newStepTitle,
                        onValueChange = { newStepTitle = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Novo passo de execução...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = borderCol
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )

                    Button(
                        onClick = {
                            if (newStepTitle.isNotBlank()) {
                                onAddStep(newStepTitle, "")
                                newStepTitle = ""
                                isAddingStep = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text("Adicionar", fontSize = 11.sp)
                    }
                }
            }

            if (steps.isEmpty()) {
                Text(
                    text = "Nenhum passo adicionado ainda.",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = textMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isDark) Color(0xFF141722) else Color(0xFFF8FAFC))
                                .border(0.5.dp, borderCol, RoundedCornerShape(2.dp))
                                .clickable { onToggleStep(index) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = step.completed,
                                    onCheckedChange = { onToggleStep(index) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF10B981),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = step.title,
                                    fontSize = 12.sp,
                                    color = if (step.completed) textMuted else textPrimary,
                                    textDecoration = if (step.completed) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }

                            IconButton(
                                onClick = { onDeleteStep(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir passo",
                                    tint = textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom pin / category badge: Pin icon + "Fixa"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = textMuted,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = activity.category.ifEmpty { "Fixa" },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textMuted
            )
        }
    }
}
