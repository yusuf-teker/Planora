package com.yusufteker.planora.core.ui.components

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberAppImagePickerLauncher(
    onResult: (ByteArray?) -> Unit
): AppImagePickerLauncher {
    val context = LocalContext.current

    val processUri: (Uri?) -> Unit = { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBytes = inputStream?.readBytes()
                inputStream?.close()

                if (originalBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
                    if (bitmap != null) {
                        val maxDim = 800
                        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                            val newW = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
                            val newH = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
                            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
                        } else {
                            bitmap
                        }
                        val baos = ByteArrayOutputStream()
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                        onResult(baos.toByteArray())
                    } else {
                        onResult(originalBytes)
                    }
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        } else {
            onResult(null)
        }
    }

    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = processUri
    )

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = processUri
    )

    return remember(pickVisualMediaLauncher, getContentLauncher) {
        object : AppImagePickerLauncher {
            override fun launch() {
                try {
                    if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                        pickVisualMediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        getContentLauncher.launch("image/*")
                    }
                } catch (e: ActivityNotFoundException) {
                    try {
                        getContentLauncher.launch("image/*")
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                        onResult(null)
                    }
                } catch (e: Exception) {
                    try {
                        getContentLauncher.launch("image/*")
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                        onResult(null)
                    }
                }
            }
        }
    }
}
