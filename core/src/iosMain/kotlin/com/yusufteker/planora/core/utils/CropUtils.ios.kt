package com.yusufteker.planora.core.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGImageCreateWithImageInRect
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.getBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

/**
 * iOS implementation of [cropImageBytes].
 * Uses CoreGraphics [CGImageCreateWithImageInRect] to crop native [UIImage]
 * and returns encoded JPEG byte array.
 *
 * @param imageBytes Raw input image byte array
 * @param cropRect Normalized crop coordinates
 * @return Cropped image byte array
 */
@OptIn(ExperimentalForeignApi::class)
actual fun cropImageBytes(
    imageBytes: ByteArray,
    cropRect: CropRectNormalized
): ByteArray {
    return try {
        val nsData = imageBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = imageBytes.size.toULong())
        }
        val uiImage = UIImage.imageWithData(nsData) ?: return imageBytes
        val cgImage = uiImage.CGImage ?: return imageBytes

        val width = CGImageGetWidth(cgImage).toDouble()
        val height = CGImageGetHeight(cgImage).toDouble()

        val rect = CGRectMake(
            cropRect.left.toDouble() * width,
            cropRect.top.toDouble() * height,
            cropRect.width.toDouble() * width,
            cropRect.height.toDouble() * height
        )

        val croppedCgImage = CGImageCreateWithImageInRect(cgImage, rect) ?: return imageBytes
        val croppedUiImage = UIImage.imageWithCGImage(croppedCgImage)
        val jpegData = UIImageJPEGRepresentation(croppedUiImage, 0.9) ?: return imageBytes

        val resultSize = jpegData.length.toInt()
        val resultArray = ByteArray(resultSize)
        resultArray.usePinned { pinned ->
            jpegData.getBytes(pinned.addressOf(0), jpegData.length)
        }
        resultArray
    } catch (e: Exception) {
        imageBytes
    }
}
