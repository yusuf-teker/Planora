package com.yusufteker.pulse.core.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints

fun Modifier.rotateVertically(): Modifier = this
    .
    layout { measurable, constraints ->
        // Text'i normal (yatay) haliyle ölç
        val placeable = measurable.measure(
            Constraints(
                minWidth = 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxWidth // yükseklik olarak genişliği kullan
            )
        )
        // Genişlik ve yüksekliği swap ederek layout boyutu belirle
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = -(placeable.width - placeable.height) / 2,
                y = -(placeable.height - placeable.width) / 2
            )
        }
    }
    .rotate(-90f)