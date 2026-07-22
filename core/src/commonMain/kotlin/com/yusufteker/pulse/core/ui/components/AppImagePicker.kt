package com.yusufteker.pulse.core.ui.components

import androidx.compose.runtime.Composable

interface AppImagePickerLauncher {
    fun launch()
}

/**
 * Robust cross-platform Image Picker for KMP.
 * On Android, uses PhotoPicker with automatic fallback to GetContent if ActivityNotFoundException occurs.
 * On iOS, uses PHPickerViewController.
 */
@Composable
expect fun rememberAppImagePickerLauncher(
    onResult: (ByteArray?) -> Unit
): AppImagePickerLauncher
