package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActivityCategory
import com.example.data.model.ScheduleItem

@Composable
fun AddEditActivityDialog(
    activityToEdit: ScheduleItem?,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (id: Long, start: String, end: String, title: String, instructions: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf(activityToEdit?.activity ?: "") }
    var startTime by remember { mutableStateOf(activityToEdit?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(activityToEdit?.endTime ?: "10:00") }
    var category by remember { mutableStateOf(activityToEdit?.category ?: "Trabalho") }
    var instructions by remember { mutableStateOf(activityToEdit?.instructions ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_edit_activity_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (activityToEdit != null) "Editar Atividade" else "Nova Atividade",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Defina os horários e parâmetros do protocolo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome da Atividade") },
                    placeholder = { Text("Ex: Treino de Força / Foco Profundo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Times Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Início") },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = accentColor) },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Término") },
                        placeholder = { Text("09:00") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = accentColor) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category selector
                Text(
                    text = "CATEGORIA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityCategory.entries.forEach { cat ->
                        val isSelected = category == cat.label
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) cat.color else cat.color.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, cat.color.copy(alpha = if (isSelected) 1f else 0.3f)),
                            modifier = Modifier.clickable { category = cat.label }
                        ) {
                            Text(
                                text = cat.label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Instructions field
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Diretriz / Instruções (opcional)") },
                    placeholder = { Text("Descreva pontos-chave e procedimentos desta tarefa...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank()) {
                                onSave(
                                    activityToEdit?.id ?: 0,
                                    startTime.trim(),
                                    endTime.trim(),
                                    title.trim(),
                                    instructions.trim(),
                                    category
                                )
                            }
                        },
                        enabled = title.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
