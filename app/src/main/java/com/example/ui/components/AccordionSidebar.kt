package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScheduleItem
import com.example.ui.viewmodel.ConnectedAgenda

@Composable
fun AccordionSidebar(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    userName: String,
    currentActivity: ScheduleItem?,
    selectedActivity: ScheduleItem?,
    allItems: List<ScheduleItem>,
    onSelectActivity: (ScheduleItem) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    connectedAgendas: List<ConnectedAgenda> = emptyList(),
    onToggleAgenda: (ConnectedAgenda) -> Unit = {},
    onRemoveAgenda: (ConnectedAgenda) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isAgoraExpanded by remember { mutableStateOf(true) }
    var isBottomContentExpanded by remember { mutableStateOf(false) } // Recolhido por padrão conforme solicitado
    var isCronogramaExpanded by remember { mutableStateOf(true) }
    var isAgendasExpanded by remember { mutableStateOf(false) } // Recolhido por padrão conforme solicitado, expande ao clicar na seta pra baixo

    val sidebarBg = Color(0xFF0F1015)
    val borderCol = Color(0xFF1E212B)
    val textPrimary = Color(0xFFF1F5F9)
    val textSecondary = Color(0xFF94A3B8)
    val textMuted = Color(0xFF64748B)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    if (!isExpanded) {
        // Slim Collapsed Rail (~48dp wide)
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .width(48.dp)
                .testTag("accordion_sidebar_slim"),
            color = sidebarBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Expand Button
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_expand_accordion")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Expandir Menu Sanfona",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Calendar / Timeline icon
                    IconButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Ver Cronograma Linear",
                            tint = textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Agora / Schedule icon
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Atividade Atual",
                            tint = textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Settings icon at bottom
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações e Temas",
                        tint = textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // Expanded Accordion Sidebar Pane (~280dp wide)
        Surface(
            modifier = modifier
                .fillMaxHeight()
                .width(280.dp)
                .testTag("accordion_sidebar_expanded"),
            color = sidebarBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Profile Avatar + Name + Subtitle + Collapse Chevron
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circle Avatar "B"
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222734))
                            .border(1.dp, Color(0xFF333B4F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(1).uppercase().ifEmpty { "B" },
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Central",
                            fontSize = 11.sp,
                            color = textMuted
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_collapse_accordion")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Recolher Menu",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = borderCol)

                // ACCORDION SECTION 1: "AGORA"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF14161E))
                        .border(1.dp, borderCol, RoundedCornerShape(3.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAgoraExpanded = !isAgoraExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsating green dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .alpha(pulseAlpha)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "AGORA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = textPrimary
                            )
                        }

                        Icon(
                            imageVector = if (isAgoraExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isAgoraExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (currentActivity != null) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = Color(0xFF1C202B),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectActivity(currentActivity) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = currentActivity.activity,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "${currentActivity.startTime} — ${currentActivity.endTime}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = textSecondary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Nenhuma atividade no momento",
                                    fontSize = 11.sp,
                                    color = textMuted,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                        }
                    }
                }

                // 3 PONTOS LOGO ABAIXO DO LAYOUT "AGORA" SEM RETÂNGULO ARREDONDADO AO REDOR
                // Padrão: recolhido. Arrastar para baixo mostra o conteúdo, arrastar para cima recolhe.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isBottomContentExpanded = !isBottomContentExpanded }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount > 6f) {
                                    // Arrastar pra baixo -> mostra o conteúdo
                                    isBottomContentExpanded = true
                                } else if (dragAmount < -6f) {
                                    // Arrastar pra cima -> recolhe o conteúdo
                                    isBottomContentExpanded = false
                                }
                            }
                        }
                        .padding(vertical = 10.dp)
                        .testTag("three_dots_accordion_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isBottomContentExpanded) Color(0xFF3B82F6)
                                        else Color(0xFF64748B)
                                    )
                            )
                        }
                    }
                }

                // CONTEÚDO EM BAIXO DOS 3 PONTOS (RECOLHIDO POR PADRÃO)
                AnimatedVisibility(
                    visible = isBottomContentExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ACCORDION SECTION 2: "CRONOGRAMA COMPLETO"
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCronogramaExpanded = !isCronogramaExpanded }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewAgenda,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "CRONOGRAMA COMPLETO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = textSecondary
                            )
                        }

                        Icon(
                            imageVector = if (isCronogramaExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isCronogramaExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (allItems.isEmpty()) {
                                Text(
                                    text = "Nenhuma atividade agendada",
                                    fontSize = 11.sp,
                                    color = textMuted,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                allItems.forEach { item ->
                                    val isSelected = selectedActivity?.id == item.id
                                    Surface(
                                        shape = RoundedCornerShape(2.dp),
                                        color = if (isSelected) Color(0xFF1F2128) else Color(0xFF13151D),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF373D4D) else borderCol
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectActivity(item) }
                                            .testTag("sidebar_item_${item.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        tint = textMuted,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "${item.startTime} — ${item.endTime}",
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = textSecondary
                                                    )
                                                }

                                                // Pill tag: "Fixa" or item.category
                                                Surface(
                                                    shape = RoundedCornerShape(2.dp),
                                                    color = Color(0xFF20232B)
                                                ) {
                                                    Text(
                                                        text = item.category.ifEmpty { "Fixa" },
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = textSecondary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = item.activity,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // ACCORDION SECTION 3: "AGENDAS CONECTADAS" (Posicionado na parte inferior do menu sanfona)
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAgendasExpanded = !isAgendasExpanded }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AGENDAS CONECTADAS (${connectedAgendas.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = textSecondary
                        )

                        Icon(
                            imageVector = if (isAgendasExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isAgendasExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (connectedAgendas.isEmpty()) {
                                Text(
                                    text = "Nenhuma agenda conectada",
                                    fontSize = 11.sp,
                                    color = textMuted,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                connectedAgendas.forEach { agenda ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF14161E).copy(alpha = 0.6f))
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onToggleAgenda(agenda) },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = agenda.isChecked,
                                                onCheckedChange = { onToggleAgenda(agenda) },
                                                modifier = Modifier.size(20.dp),
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = agenda.color,
                                                    checkmarkColor = Color.White
                                                )
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(agenda.color, CircleShape)
                                            )
                                            Text(
                                                text = agenda.name,
                                                fontSize = 11.sp,
                                                color = textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (agenda.isRemovable) {
                                            IconButton(
                                                onClick = { onRemoveAgenda(agenda) },
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .testTag("btn_remove_agenda_${agenda.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remover arquivo ${agenda.name}",
                                                    tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
