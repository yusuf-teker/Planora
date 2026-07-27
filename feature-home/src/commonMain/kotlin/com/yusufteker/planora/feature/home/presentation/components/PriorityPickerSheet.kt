package com.yusufteker.planora.feature.home.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.shared.api.TaskPriority
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*

/**
 * Görev öncelik seviyesi seçimi için kullanılan bottom sheet bileşeni.
 * Düşük, Orta, Yüksek ve Acil seçeneklerini renkli bayrak ikonlarıyla listeler.
 *
 * @param currentPriority Mevcut seçili öncelik seviyesi.
 * @param sheetState Bottom sheet'in durumu.
 * @param onDismissRequest Sheet kapatıldığında çağrılacak callback.
 * @param onPrioritySelected Bir öncelik seçildiğinde çağrılacak callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityPickerSheet(
    currentPriority: TaskPriority,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    val options = listOf(
        TaskPriority.LOW to Triple(Res.string.priority_low, Color(0xFF9E9E9E), Icons.Default.Flag),
        TaskPriority.MEDIUM to Triple(Res.string.priority_medium, Color(0xFFFB8C00), Icons.Default.Flag),
        TaskPriority.HIGH to Triple(Res.string.priority_high, Color(0xFFE53935), Icons.Default.Flag),
        TaskPriority.URGENT to Triple(Res.string.priority_urgent, Color(0xFFD50000), Icons.Default.Flag)
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.priority_label),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            options.forEach { (priority, details) ->
                val (labelRes, color, icon) = details
                val isSelected = currentPriority == priority
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPrioritySelected(priority) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
