package com.yusufteker.planora.core.utils

/**
 * Represents normalized crop bounds relative to the image dimensions (0.0f to 1.0f).
 *
 * @property left Left normalized coordinate (0.0f to 1.0f)
 * @property top Top normalized coordinate (0.0f to 1.0f)
 * @property width Normalized width (0.0f to 1.0f)
 * @property height Normalized height (0.0f to 1.0f)
 */
data class CropRectNormalized(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * Platform-specific image cropping utility function.
 * Crops the provided raw image bytes according to the normalized crop rectangle.
 *
 * @param imageBytes Raw input image byte array
 * @param cropRect Normalized crop bounding box relative to image size
 * @return Cropped JPEG/PNG image byte array
 */
expect fun cropImageBytes(
    imageBytes: ByteArray,
    cropRect: CropRectNormalized
): ByteArray
