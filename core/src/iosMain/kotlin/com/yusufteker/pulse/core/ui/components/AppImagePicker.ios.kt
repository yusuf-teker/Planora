package com.yusufteker.pulse.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.preat.peekaboo.image.picker.ResizeOptions
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher

@Composable
actual fun rememberAppImagePickerLauncher(
    onResult: (ByteArray?) -> Unit
): AppImagePickerLauncher {
    val coroutineScope = rememberCoroutineScope()
    val peekabooLauncher = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = coroutineScope,
        resizeOptions = ResizeOptions(width = 500, height = 500, compressionQuality = 0.8),
        onResult = { byteArrays ->
            onResult(byteArrays.firstOrNull())
        }
    )

    return remember(peekabooLauncher) {
        object : AppImagePickerLauncher {
            override fun launch() {
                peekabooLauncher.launch()
            }
        }
    }
}
