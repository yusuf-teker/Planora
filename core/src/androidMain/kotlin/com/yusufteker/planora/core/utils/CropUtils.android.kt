package com.yusufteker.planora.core.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

/**
 * Android implementation of [cropImageBytes].
 * Decodes input byte array into a Android [Bitmap], crops the target region,
 * and encodes the result back into a JPEG byte array.
 *
 * @param imageBytes Raw input image byte array
 * @param cropRect Normalized crop coordinates
 * @return Cropped image byte array
 */
actual fun cropImageBytes(
    imageBytes: ByteArray,
    cropRect: CropRectNormalized
): ByteArray {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return imageBytes
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height

        val left = (cropRect.left * srcWidth).toInt().coerceIn(0, (srcWidth - 1).coerceAtLeast(0))
        val top = (cropRect.top * srcHeight).toInt().coerceIn(0, (srcHeight - 1).coerceAtLeast(0))
        val width = (cropRect.width * srcWidth).toInt().coerceIn(1, (srcWidth - left).coerceAtLeast(1))
        val height = (cropRect.height * srcHeight).toInt().coerceIn(1, (srcHeight - top).coerceAtLeast(1))

        val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
        val stream = ByteArrayOutputStream()
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val result = stream.toByteArray()
        if (cropped != bitmap) {
            cropped.recycle()
        }
        bitmap.recycle()
        result
    } catch (e: Exception) {
        imageBytes
    }
}
