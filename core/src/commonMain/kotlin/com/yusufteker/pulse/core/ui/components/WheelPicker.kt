package com.yusufteker.pulse.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<String>,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleItemsCount: Int = 5,
    initialIndex: Int = 0,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    onItemSelected: (index: Int, item: String) -> Unit = { _, _ -> }
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val paddingItems = visibleItemsCount / 2
    
    // Add empty strings at the beginning and end to allow scrolling to the first and last item
    val paddedItems = remember(items) {
        val emptyList = List(paddingItems) { "" }
        emptyList + items + emptyList
    }

    // Trigger callback when the centered item changes
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val centerIndex = listState.firstVisibleItemIndex
        if (centerIndex in items.indices) {
            onItemSelected(centerIndex, items[centerIndex])
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        )
        
        val currentCenter by remember { derivedStateOf { listState.firstVisibleItemIndex } }
        
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(paddedItems.size) { index ->
                val centerOffset = currentCenter + paddingItems - index
                val absCenterOffset = abs(centerOffset)
                
                val scale = 1f - (absCenterOffset * 0.15f)
                val alpha = if (absCenterOffset == 0) 1f else maxOf(0.3f, 1f - (absCenterOffset * 0.3f))
                
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = paddedItems[index],
                        style = textStyle,
                        fontWeight = if (absCenterOffset == 0) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelTimePickerBottomSheet(
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 12,
    initialMinute: Int = 0
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    val hours = (0..23).map { it.toString().padStart(2, '0') }
    val minutes = (0..59).map { it.toString().padStart(2, '0') }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Saat Seç",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    items = hours,
                    initialIndex = initialHour,
                    modifier = Modifier.width(80.dp),
                    onItemSelected = { index, _ -> selectedHour = index }
                )
                
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                WheelPicker(
                    items = minutes,
                    initialIndex = initialMinute,
                    modifier = Modifier.width(80.dp),
                    onItemSelected = { index, _ -> selectedMinute = index }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { onTimeSelected(selectedHour, selectedMinute) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Tamam", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
