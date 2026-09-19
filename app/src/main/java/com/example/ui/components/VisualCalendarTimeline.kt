package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityCategory
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.launch

private const val HOUR_HEIGHT_DP = 96

@Composable
fun VisualCalendarTimeline(
    scheduleItems: List<ScheduleItem>,
    currentTimeStr: String,
    selectedCategory: String?,
    accentColor: Color,
    onSelectCategory: (String?) -> Unit,
    onSelectActivity: (ScheduleItem) -> Unit,
    onClose: () -> Unit = {},
    onPickFromDrive: () -> Unit = {},
    onOpenImportText: () -> Unit = {},
    onLoadTestExample: () -> Unit = {},
    onAddActivity: () -> Unit = {},
    onAddActivityAtHour: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val currentMinutes by remember(currentTimeStr) {
        derivedStateOf {
            timeToMinutes(currentTimeStr)
        }
    }

    // Auto scroll to current time on first appearance
    LaunchedEffect(Unit) {
        val targetY = ((currentMinutes / 60f) * HOUR_HEIGHT_DP * 2.5f).toInt().coerceAtLeast(0)
        scrollState.animateScrollTo(targetY)
    }

    val showScrollNowButton by remember {
        derivedStateOf {
            val nowScrollPos = ((currentMinutes / 60f) * HOUR_HEIGHT_DP * 2.5f).toInt()
            kotlin.math.abs(scrollState.value - nowScrollPos) > 500
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val bgColor = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
    val textMuted = if (isDark) Color(0xFF64748B) else Color(0xFF9CA3AF)
    val dividerColor = if (isDark) Color(0xFF2D3748) else Color(0xFFE5E7EB)
    val gridLineColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF3F4F6)
    val pillBgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6)
    val cardBg = if (isDark) Color(0xFF1E2430) else Color.White
    val cardBorder = if (isDark) Color(0xFF334155) else Color(0xFFE5E7EB)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("visual_calendar_view"),
        color = bgColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Header: "Visualização de Rotina", "CRONOGRAMA LINEAR", "TEMPO REAL", and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: 2x2 square grid icon + Titles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 4-squares grid icon
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val iconSquareColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF374151)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(7.dp).background(iconSquareColor, RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(7.dp).background(iconSquareColor, RoundedCornerShape(1.dp)))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(7.dp).background(iconSquareColor, RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(7.dp).background(iconSquareColor, RoundedCornerShape(1.dp)))
                        }
                    }

                    Column {
                        Text(
                            text = "Visualização de Rotina",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "CRONOGRAMA LINEAR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp,
                            color = textMuted
                        )
                    }
                }

                // Right: Real-time pill badge + Action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = pillBgColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Text(
                                text = "TEMPO REAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                            Text(
                                text = currentTimeStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }

                    // Google Drive Docs Pick Button
                    IconButton(
                        onClick = onPickFromDrive,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_drive_pick_visual")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Carregar arquivo Docs do Google Drive",
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Paste Docs text button
                    IconButton(
                        onClick = onOpenImportText,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_import_text_visual")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Colar texto do Google Docs",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Add new activity button
                    IconButton(
                        onClick = onAddActivity,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_add_activity_visual")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Adicionar Atividade",
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Switch / Toggle View button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("btn_close_visual_calendar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = "Alternar para Lista",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = dividerColor)

            // Category Highlight Bar: "■ DESTAQUE DE CATEGORIA:" and "CLIQUE EM UMA COR PARA DESTACAR A CATEGORIA"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Color(0xFFE91E63), RoundedCornerShape(1.dp))
                        )
                        Text(
                            text = "DESTAQUE DE CATEGORIA:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = textSecondary
                        )
                    }

                    Text(
                        text = "CLIQUE EM UMA COR PARA DESTACAR A CATEGORIA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = textMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row of 8 Square Swatches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActivityCategory.entries.forEach { cat ->
                        val isSelected = selectedCategory == cat.label
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(cat.color, RoundedCornerShape(2.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, if (isDark) Color.White else Color(0xFF111827), RoundedCornerShape(2.dp))
                                    } else {
                                        Modifier.border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    }
                                )
                                .clickable {
                                    onSelectCategory(if (isSelected) null else cat.label)
                                }
                        )
                    }

                    if (selectedCategory != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Limpar filtro",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            modifier = Modifier.clickable { onSelectCategory(null) }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = dividerColor)

            // Helper Banner if no items are currently loaded
            if (scheduleItems.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Agenda vazia. Carregue do Drive ou toque:",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onLoadTestExample,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF059669) else Color(0xFF16A34A)),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("17h às 18h Janta", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 24-Hour Timeline Canvas with clean square white cards & left color stripe (ALWAYS RENDERED)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                val totalHeightDp = (24 * HOUR_HEIGHT_DP) + 80

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalHeightDp.dp)
                ) {
                    // Hour Grid Lines
                    for (hour in 0..23) {
                        val topY = (hour * HOUR_HEIGHT_DP).dp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = topY)
                                .clickable { onAddActivityAtHour(hour) }
                                .padding(start = 12.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                    text = "%02d:00".format(hour),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = textMuted,
                                    modifier = Modifier.width(44.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(gridLineColor)
                                )
                            }
                        }

                        // Schedule Item Cards
                        scheduleItems.forEach { item ->
                            val isHighlighted = selectedCategory == null || item.category == selectedCategory
                            val startMin = timeToMinutes(item.startTime)
                            val endMin = timeToMinutes(item.endTime).let { if (it <= startMin) it + 1440 else it }
                            val durationMin = (endMin - startMin).coerceAtLeast(25)

                            val topDp = ((startMin / 60f) * HOUR_HEIGHT_DP).dp
                            val heightDp = ((durationMin / 60f) * HOUR_HEIGHT_DP).dp.coerceAtLeast(42.dp)
                            val catColor = item.categoryEnum.color

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = topDp)
                                    .padding(start = 62.dp, end = 14.dp)
                                    .height(heightDp - 3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isHighlighted) cardBg else (if (isDark) Color(0xFF151922).copy(alpha = 0.5f) else Color(0xFFFAFAFA).copy(alpha = 0.5f))
                                    )
                                    .border(
                                        border = BorderStroke(
                                            1.dp,
                                            if (isHighlighted) cardBorder else (if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6))
                                        ),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .clickable { onSelectActivity(item) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Solid thick left color line as in the uploaded image
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .fillMaxHeight()
                                            .background(if (isHighlighted) catColor else catColor.copy(alpha = 0.3f))
                                    )

                                    // Content inside card: Title bold, Category uppercase, Time range with em-dash
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = item.activity,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isHighlighted) textPrimary else textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = item.category.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            color = if (isHighlighted) catColor else catColor.copy(alpha = 0.4f)
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "${item.startTime} — ${item.endTime}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isHighlighted) textSecondary else textMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Real-Time Marker & Badge (TEMPO REAL)
                        val nowTopDp = ((currentMinutes / 60f) * HOUR_HEIGHT_DP).dp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = nowTopDp - 9.dp)
                                .padding(start = 6.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFFE91E63)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                    Text(
                                        text = currentTimeStr,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.5.dp)
                                    .background(Color(0xFFE91E63))
                            )
                        }
                    }
                }
            }

            // Floating "Vai para o Agora" button
            AnimatedVisibility(
                visible = showScrollNowButton && scheduleItems.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val targetY = ((currentMinutes / 60f) * HOUR_HEIGHT_DP * 2.5f).toInt().coerceAtLeast(0)
                            scrollState.animateScrollTo(targetY)
                        }
                    },
                    containerColor = Color(0xFFE91E63),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.testTag("btn_scroll_to_now")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier
                                .size(15.dp)
                                .rotate(45f)
                        )
                        Text(
                            text = "IR PARA O AGORA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}

private fun timeToMinutes(timeStr: String): Int {
    return try {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        h * 60 + m
    } catch (e: Exception) {
        0
    }
}
